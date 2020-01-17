package mike.test.persistence.testing

// Note: anorm.~ is different from the implicit ~ inside RowParser.  Make
// sure to import it here, or there will be difficult-to-troubleshoot errors
// in the case matches below!
import anorm.{~, ResultSetParser}
import anorm.SqlParser.str

case class User(name: String, readBooks: Seq[String])

object User {
  val readBooksSql = """select u.name as username, b.title as booktitle
                            from user u left join read_book rb on u.id = rb.user_id
                              left join book b on rb.book_id = b.id
                            where u.name = {username}"""
  val readBooksAllUsersSql = """select u.name as username, b.title as booktitle
                            from user u left join read_book rb on u.id = rb.user_id
                              left join book b on rb.book_id = b.id"""

  val withBooksParser: ResultSetParser[Option[User]] = {
    val userAndTitlesParser = (str("username") ~ str("booktitle").?).*
    userAndTitlesParser map { userAndTitles =>
      userAndTitles.headOption map { case username ~ _ =>
        User(username, userAndTitles flatMap { case _ ~ title => title })
      }
    }
  }

  val withBooksParserAllUsers: ResultSetParser[Seq[User]] = {
    val userAndTitlesParser = (str("username") ~ str("booktitle").?).*
    userAndTitlesParser map { allUsersAndTitles =>
      allUsersAndTitles.groupBy(_._1).flatMap { case (_, userAndTitles) =>
        userAndTitles.headOption map { case username ~ _ =>
          User(username, userAndTitles flatMap { case _ ~ title => title })
        }
      }.toSeq
    }
  }
}
