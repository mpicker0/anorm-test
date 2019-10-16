name          := "anorm-persistence-test"

organization  := "mike.test"

version       := "0.1"

scalaVersion  := "2.12.10"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
  "org.playframework.anorm" %% "anorm" % "2.6.4",
  "com.h2database" % "h2" % "1.4.197",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

