package org.scalarelational.manual.gettingstarted

import GettingStartedDatastore
import GettingStartedDatastore._

/**
 * @author Matt Hicks <matt@outr.com>
 */
object GettingStarted extends SectionSupport {
  section("create") {
    import GettingStartedDatastore._

    session {
      create(suppliers, coffees)
    }
  }
  
  section("createVerbose", invoke = false) {
    GettingStartedDatastore.session {
      GettingStartedDatastore.create(
        GettingStartedDatastore.suppliers,
        GettingStartedDatastore.coffees
      )
    }
  }

  section("createAliased", invoke = false) {
    def ds = GettingStartedDatastore

    ds.session {
      ds.create(ds.suppliers, ds.coffees)
    }
  }

  section("insert") {
    import GettingStartedDatastore._
    import suppliers._

    session {
      insert(id(101), name("Acme, Inc."), street("99 Market Street"),
        city("Groundsville"), state("CA"), zip("95199")).result
      insert(id(49), name("Superior Coffee"), street("1 Party Place"),
        city("Mendocino"), state("CA"), zip("95460")).result
    }
  }

  section("insertShorthand") {
    import GettingStartedDatastore._

    session {
      insertInto(suppliers, 150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").result
    }
  }

  section("insertBatch") {
    import GettingStartedDatastore._
    import coffees._

    session {
      insert(name("Colombian"), supID(101), price(7.99), sales(0), total(0)).
        and(name("French Roast"), supID(49), price(8.99), sales(0), total(0)).
        and(name("Espresso"), supID(150), price(9.99), sales(0), total(0)).
        and(name("Colombian Decaf"), supID(101), price(8.99), sales(0), total(0)).
        and(name("French Roast Decaf"), supID(49), price(9.99), sales(0), total(0)).result
    }
  }

  section("insertSequence") {
    import GettingStartedDatastore._
    import coffees._

    session {
      val rows = (0 to 10).map { index =>
        List(name(s"Generic Coffee ${index + 1}"), supID(49), price(6.99), sales(0), total(0))
      }
      insertBatch(rows).result
    }
  }

  section("query") {
    import GettingStartedDatastore._
    import coffees._

    session {
      val query = select (*) from coffees

      query.result.map { r =>
        s"${r(name)}\t${r(supID)}\t${r(price)}\t${r(sales)}\t${r(total)}"
      }.mkString("\n")
    }
  }

  section("queryConverted") {
    import GettingStartedDatastore.{coffees => c, _}

    session {
      val query = select (c.name, c.supID, c.price, c.sales, c.total) from coffees

      query.result.converted.map {
        case (name, supID, price, sales, total) => s"$name  $supID  $price  $sales  $total"
      }.mkString("\n")
    }
  }

  section("join") {
    import GettingStartedDatastore._

    session {
      val query = (select(coffees.name, suppliers.name)
        from coffees
        innerJoin suppliers on coffees.supID === suppliers.id
        where coffees.price < 9.0)

      query.result.map { r =>
        s"Coffee: ${r(coffees.name)}, Supplier: ${r(suppliers.name)}"
      }.mkString("\n")
    }
  }
}