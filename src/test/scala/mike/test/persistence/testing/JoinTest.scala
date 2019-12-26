package mike.test.persistence.testing

import java.sql.DriverManager
import anorm.SQL
import org.scalatest.{FunSpecLike, Matchers}

class JoinTest extends FunSpecLike with Matchers {
  implicit val connection = DriverManager.getConnection("jdbc:h2:file:./target/anorm-test-db")

  describe("join") {
    it("joins in a one-to-many relationship") {
      // This test assumes that the data was inserted in Main.scala
      val expectedBob = User("Bob", Seq("UNIX in a Nutshell", "Programming Windows Identity Foundation", "Python Cookbook"))
      val expectedAlice = User("Alice", Nil)
      val expectedJoe = User("Joe", Seq("Programming Windows Identity Foundation"))

      val bob = SQL(User.readBooksSql).on("username" -> "Bob").as(User.withBooksParser)
      bob shouldBe Some(expectedBob)

      val alice = SQL(User.readBooksSql).on("username" -> "Alice").as(User.withBooksParser)
      alice shouldBe Some(expectedAlice)

      val joe = SQL(User.readBooksSql).on("username" -> "Joe").as(User.withBooksParser)
      joe shouldBe Some(expectedJoe)

      // this user does not exist in the data
      val nobody = SQL(User.readBooksSql).on("username" -> "no-such-person").as(User.withBooksParser)
      nobody shouldBe None

      // Similar to the above, but do it in a single query
      val users: Seq[User] = SQL(User.readBooksAllUsersSql).as(User.withBooksParserAllUsers)
      // don't care about ordering in this test
      users should contain theSameElementsAs Seq(expectedBob, expectedAlice, expectedJoe)
    }
  }
}
