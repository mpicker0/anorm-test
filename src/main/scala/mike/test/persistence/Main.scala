package mike.test.persistence

import anorm._
import java.sql.{Connection, DriverManager}

object Main {
  def initDb(implicit connection: Connection) = {
    SQL(
      """
        create table book (id integer not null generated always as identity (start with 1, increment by 1),
                           title varchar(256) not null,
                           author varchar(256) not null,
                           isbn varchar(256),
                           pages integer)
      """).execute()

    // TODO see the section on "Generated parameter conversions" to see if it's
    // easier to insert directly from case classes
    SQL("insert into book(title, author) values ({title}, {author})")
      .on("title" -> "UNIX in a Nutshell", "author" -> "Gilly, Daniel")
      .executeInsert()
    SQL("insert into book(title, author) values ({title}, {author})")
      .on("title" -> "Programming Windows Identity Foundation", "author" -> "Bertocci, Vittorio", "isbn" -> "978-0-7356-2718-5", "pages" -> 250)
      .executeInsert()
    SQL("insert into book(title, author) values ({title}, {author})")
      .on("title" -> "Python Cookbook", "author" -> "Martelli, Alex", "isbn" -> "0-596-00797-3", "pages" -> 810)
      .executeInsert()

    SQL(
      """
        create table user (id integer not null generated always as identity (start with 1, increment by 1),
                           name varchar(256) not null)
        """.stripMargin
    ).execute()
    // Anorm multi-insert
    BatchSql("insert into user(name) values({name})",
      Seq[NamedParameter]("name" -> "Bob"),
      Seq[NamedParameter]("name" -> "Alice"),
      Seq[NamedParameter]("name" -> "Joe")
    ).execute()

    SQL(
      """
        create table read_book (user_id integer not null,
                                book_id integer not null)
        """.stripMargin
    ).execute()
    BatchSql("insert into read_book(user_id, book_id) values({user_id}, {book_id})",
      Seq[NamedParameter]("user_id" -> 1, "book_id" -> 1),
      Seq[NamedParameter]("user_id" -> 1, "book_id" -> 2),
      Seq[NamedParameter]("user_id" -> 1, "book_id" -> 3),
      Seq[NamedParameter]("user_id" -> 3, "book_id" -> 2)
    ).execute()
  }

  def basicReadExample(implicit connection: Connection) = {
    println("-- basicReadExample -----")
    // count will return a single int value, so we use int(...).single
    val count = SQL("select count(*) as count from book").as(SqlParser.int("count").single)
    println(s"row count: $count")

    // namedParser maps column names to case class field names
    val bookParser: RowParser[Book] = Macro.namedParser[Book]
    // select * will return many values, so we use parser.* to get all the values
    val result: List[Book] = SQL"select * from book".as(bookParser.*)
    result.foreach(println)
  }

  def basicParseExample(implicit connection: Connection) = {
    println("-- basicParseExample -----")
    import anorm.SqlParser.{int, flatten}
    // a RowParser parses one row to a Scala value; flatten converts it to a tuple
    val readBooksRowParser = int("user_id") ~ int("book_id") map flatten
    // a ResultSetParser handles all the results of the query.  * parses as many rows as possible
    val readBooksResult = SQL("select user_id, book_id from read_book").as(readBooksRowParser.*)
    readBooksResult.foreach { t =>
      println(s"user ${t._1}, book ${t._2}")
    }

    // The RowParser and ResultSetParser can be combined in one statement, which might make it easier to read for
    // simple parsers
    SQL("select user_id, book_id from read_book")
      .as((int("user_id") ~ int("book_id") map flatten).*)
      .foreach { t => println(s"user ${t._1}, book ${t._2}") }
  }

  def joinExample(implicit connection: Connection) = {
    println("-- joinExample -----")
    import anorm.SqlParser.{int, str, flatten}
    case class UserWithBook(name: String, bookTitle: String)
    object UserWithBook {
      // The parser doesn't have to be in a companion object like this, but it often is and makes it more portable
      val parser: RowParser[UserWithBook] =
        str("username") ~ str("booktitle") map { case u ~ b => UserWithBook(u, b) }
      // As with the parser, it's helpful to keep the SQL in the companion object
      // Do a join to find out each user along with the books they've read
      val bookSql = """select u.name as username, b.title as booktitle
                       from user u join read_book rb on u.id = rb.user_id
                         join book b on rb.book_id = b.id"""
    }
    import UserWithBook._
    SQL(bookSql).as(parser.*).foreach { ub =>
      println(s"user: ${ub.name}, book: ${ub.bookTitle}")
    }

    // Step up a level; just select the book IDs
    // Do a join to find out each user along with the books they've read
    case class UserWithBookIds(name: String, bookIds: Seq[Int])
    object UserWithBookIds {
      val readBookIdSql = """select u.name as username, rb.book_id as bookid
                             from user u left join read_book rb on u.id = rb.user_id
                             where u.id = {userId}"""
      // ? turns the Int into an Option[Int]; since we're using a left join, it could be null
      val userWithReadBookIdParser = str("username") ~ int("bookid").? map flatten
    }
    import UserWithBookIds._
    for (userId <- 1 to 3) {
      val userWithReadBookIdResult = SQL(readBookIdSql)
        .on("userId" -> userId)
        .as(userWithReadBookIdParser.*)
      val userWithBookIds = userWithReadBookIdResult.headOption.map { f =>
        UserWithBookIds(f._1, userWithReadBookIdResult.flatMap(_._2))
      }
      println(s"userWithBookIds: $userWithBookIds")
    }
  }

  def joinExample2(implicit connection: Connection) = {
    println("-- joinExample2 -----")
    import anorm.SqlParser.str

    // This is the same as mike.test.persistence.User; putting the case class and companion object together in this
    // file so it's more obvious
    case class User2(name: String, readBooks: Seq[String])
    object User2 {
      val readBooksSql = """select u.name as username, b.title as booktitle
                            from user u left join read_book rb on u.id = rb.user_id
                              left join book b on rb.book_id = b.id
                            where u.name = {username}"""
      val readBooksAllUsersSql = """select u.name as username, b.title as booktitle
                            from user u left join read_book rb on u.id = rb.user_id
                              left join book b on rb.book_id = b.id"""

      val withBooksParser: ResultSetParser[Option[User2]] = {
        val userAndTitlesParser = (str("username") ~ str("booktitle").?).*
        userAndTitlesParser map { userAndTitles =>
          userAndTitles.headOption map { case username ~ _ =>
            User2(username, userAndTitles flatMap { case _ ~ title => title })
          }
        }
      }

      val withBooksParserAllUsers: ResultSetParser[Seq[User2]] = {
        val userAndTitlesParser = (str("username") ~ str("booktitle").?).*
        userAndTitlesParser map { allUsersAndTitles =>
          allUsersAndTitles.groupBy(_._1).flatMap { case (_, userAndTitles) =>
            userAndTitles.headOption map { case username ~ _ =>
              User2(username, userAndTitles flatMap { case _ ~ title => title })
            }
          }.toSeq
        }
      }
    }

    Seq("Bob", "Alice", "Joe") foreach { username =>
      SQL(User2.readBooksSql)
        .on("username" -> username)
        .as(User2.withBooksParser).foreach { ub =>
        println(s"user: ${ub.name}, books: ${ub.readBooks}")
      }
    }

    // Similar to the above, but do it in a single query
    val user2s: Seq[User2] =
      SQL(User2.readBooksAllUsersSql)
        .as(User2.withBooksParserAllUsers)
    println(s"All users: ${user2s}")
  }

  def main(args: Array[String]) {
    // Anorm's `SQL` just needs an implicit Connection in order to work
    implicit val connection = DriverManager.getConnection("jdbc:h2:file:./target/anorm-test-db")

    if(args.contains("--create")) initDb else {
      basicReadExample
      basicParseExample
      joinExample
      joinExample2
    }
  }
}
