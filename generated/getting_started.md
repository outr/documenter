
This chapter will guide you through creating your first project with ScalaRelational. For the sake of simplicity we will use an in-memory H2 database.

#sbt dependencies
The first thing you need to do is add ScalaRelational's H2 module to your sbt project:

```scala
libraryDependencies += "org.scalarelational" %% "scalarelational-h2" % "1.1.0-SNAPSHOT"
```

If you'd prefer to use another database instead, please refer to the chapter LINK.

#Library imports
You will need the following imports:

```scala
import org.scalarelational.column.property.{ForeignKey, AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.table.Table
```
    
#Schema
The next thing you need is the database representation in Scala. The schema can map to an existing database or you can use it to create the tables in your database:

```scala
object GettingStartedDatastore extends H2Datastore(mode = H2Memory("getting_started")) {
  object suppliers extends Table("SUPPLIERS") {
    val name = column[String]("SUP_NAME", Unique)
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
  }

  object coffees extends Table("COFFEES") {
    val name = column[String]("COF_NAME", Unique)
    val supID = column[Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
    val id = column[Int]("COF_ID", PrimaryKey, AutoIncrement)
  }
}
```
     

Our `Datastore` contains `Table`s and our `Table`s contain `Column`s. As for the `Datastore` we have chosen an in-memory H2 database. Every column type must have a `DataType` associated with it. You don't see it referenced above because all standard Scala types have predefined implicit conversions available FOOTNOTE. If you need to use a type that is not supported by ScalaRelational, please refer to LINK.

#Create the database
Now that we have our schema defined in Scala, we need to create the tables in the database:

SECTION: create

All database queries must take place within a *session*. Sessions will be explained in LINK.

##Import
You'll notice we imported `ExampleDatastore._` in an effort to minimise the amount of code required here. We can explicitly write it more verbosely like this:

SECTION: create-verbose

For the sake of readability importing the datastore is generally suggested. Although if namespace collisions are a problem you can import and alias or create a shorter reference like this:

SECTION: create-aliased

#Inserting
ScalaRelational supports type-safe insertions:

SECTION: insert

If we don't call `result`, we will just create the query without ever executing it. Please note that `result` must be called within the session.

There is also a shorthand when using values in order:

SECTION: insert-shorthand

The database returns -1 as the ID is already known.

If you want to insert multiple rows at the same time, you can use a batch insertion:

SECTION: insert-batch

This is very similar to the previous insert method, except instead of calling `result` we're calling `and`. This converts the insert into a batch insert and you gain the performance of being able to insert several records with one insert statement.

You can also pass a `Seq` to `insertBatch`, which is useful if the rows are loaded from a file for example:

SECTION: insert-sequence
    
#Querying
The DSL for querying a table is similar to SQL:

SECTION: query

Although that could look a little prettier by explicitly querying what we want to see:

SECTION: query-converted

Joins are supported too. In the following example we query all coffees back filtering and joining with suppliers:

SECTION: join

#Remarks
You may have noticed the striking similarity between this code and Slick's introductory tutorial. This was done purposefully to allow better comparison of functionality between the two frameworks.

An auto-incrementing ID has been introduced into both tables to better represent the standard development scenario. Rarely do you have external IDs to supply to your database like [Slick represents](http://slick.typesafe.com/doc/3.0.0/gettingstarted.html#schema).