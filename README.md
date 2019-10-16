Overview
========

This is a test of the persistence using
[Anorm](https://playframework.github.io/anorm/).
It was created to be similar to my other persistence projects in Java and
Scala.  This project uses H2 as the database.


1. Run the application the first time with `sbt "run --create"` This creates
   the datastore in the `target` directory and inserts the initial data.
2. Run the application to query the data: `sbt run`

H2 console
==========

To connect to the database directly:

1. Locate the H2 .jar file.
    
       find ~/ivy2 -name "h2*jar"

2. Start the console (make sure you use the same version of the .jar that
   you're referencing in `build.sbt`)

       java -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.197.jar

3. Use the _Generic H2 (Embedded)_ setting with a JDBC URL pointed at the
   `target` directory of this project, for example:
   
       jdbc:h2:~/code/personal/anorm-test/target/anorm-test-db
   
   Username and password should be blank
