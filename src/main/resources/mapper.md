[package value="org.scalarelational.manual.mapper"]
The mapper module provides functionality to map table rows when persisting or selecting rows.

#sbt dependency
We must first add another dependency to our build file:

```scala
libraryDependencies += "org.scalarelational" %% "scalarelational-mapper" % "1.1.0-SNAPSHOT"

libraryDependencies += "org.scalarelational" %% "scalarelational-h2" % "1.1.0-SNAPSHOT"
```

#Library imports
For the mapper you need the following additional import:

[scala type="imports" filename="MapperDatastore"]

#Table definition
When defining a table definition with the mapper, the key difference is that you need to use `MappedTable`  and supply the `case class` you want to map it to. We change the example from the previous chapter to:

[scala type="object"]

You may have noticed that the supplier ID in `coffees` now has a type-safe reference. The second type argument of `column` denotes the underlying SQL type, which in case of foreign keys is an integer.

##Creating table
As previously, create the tables using `create`:

[scala type="section" section="create" filename="Mapper"]

#Entities
Along with the table definition, you have to declare an accompanying `case class`, which is called *entity*. An entity needs to contain exactly the same columns as the table and the columns must have the same types.

A `case class` needs to extend from `Entity`. Furthermore, it needs to define the table that the columns map to.

[scala type="class" filename="Supplier"]

[scala type="class" filename="Coffee"]

Though all of these fields are in the same order as the table, this is not required to be the case. Mapping takes place based on the field name to the column name in the table, so order doesn't matter.

#Insert
We've create a `Supplier` case class, but now we need to create an instance and insert it into the database:

[scala type="section" section="insertSupplier" filename="Mapper"]

It is worth noting here that the result is the database-generated primary key.

Now define some global IDs first that we will use throughout this chapter:

[scala type="object" filename="Ids"]

And insert some additional suppliers and capture their ids:

[scala type="section" section="insertSuppliers" filename="Mapper"]

#Batch inserting
Now that we have some suppliers, we need to add some coffees as well:

[scala type="section" section="insertBatch"]

Note that we need to use type-safe references for the suppliers.

#Query
We've successfully inserted our `Supplier` instance. The syntax for querying it back out is similar to SQL:

[scala type="section" section="queryBasic"]

The mapper will automatically match column names in the results to fields in the `case class` provided. Every query can have its own class for convenience mapping.

##Using references
Use `ref` on a table definition to obtain its reference. It can then be used in queries and compared to foreign key columns like `supID`.

[scala type="section" section="queryRefs"]

##Using joins
Joins are one of the major points where ScalaRelational diverges from other frameworks that have a concept of an ORM:

[scala type="section" section="queryJoins"]

This is an efficient SQL query to join the `coffees` table with the `suppliers` table and get back a single result set. Using the mapper we are able to separate the columns relating to `coffees` from `suppliers` and map them directly to our `case class`es.

#Polymorphic tables
It may be desired to represent a type hierarchy in a single table for better performance:

[scala type="trait" filename="User"]

[scala type="case class" filename="UserGuest"]

[scala type="case class" filename="UserAdmin"]

[scala type="object" filename="UsersDatastore"]

Create the tables:

[scala type="section" section="userCreate" filename="Mapper"]

Now you can insert a heterogeneous list of entities:

[scala type="section" section="usersInsert"]

To query the table, you will need to evaluate the column which encodes the original type of the object, namely `isGuest` in this case. For more complex type hierarchies you may want to use an enumeration instead of a boolean flag.

[scala type="section" section="usersQuery"]