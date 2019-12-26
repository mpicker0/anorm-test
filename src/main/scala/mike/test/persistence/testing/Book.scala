package mike.test.persistence.testing

/** The case class that represents books; a plain old Scala case class */
case class Book(id: Option[Long], title: String, author: String, isbn: Option[String] = None, pages: Option[Int] = None)

