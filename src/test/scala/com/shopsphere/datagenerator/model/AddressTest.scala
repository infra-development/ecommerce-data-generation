package com.shopsphere.datagenerator.model

import org.scalatest.funsuite.AnyFunSuite

class AddressTest extends AnyFunSuite {

  test("Address should store address attributes") {

    val address =
      Address(
        id = "ADDRESS_000001",
        customerId = "CUSTOMER_000001",
        buildingId = "BUILDING_001",
        unitNumber = "101",
        postalCode = "400001"
      )

    assert(address.id == "ADDRESS_000001")
    assert(address.customerId == "CUSTOMER_000001")
    assert(address.buildingId == "BUILDING_001")
    assert(address.unitNumber == "101")
    assert(address.postalCode == "400001")
  }

  test("Address should support different unit number formats") {

    val address =
      Address(
        id = "ADDRESS_000002",
        customerId = "CUSTOMER_000002",
        buildingId = "BUILDING_002",
        unitNumber = "A-202",
        postalCode = "411001"
      )

    assert(address.id == "ADDRESS_000002")
    assert(address.customerId == "CUSTOMER_000002")
    assert(address.buildingId == "BUILDING_002")
    assert(address.unitNumber == "A-202")
    assert(address.postalCode == "411001")
  }

  test("Address should support equality") {

    val address1 =
      Address(
        id = "ADDRESS_000001",
        customerId = "CUSTOMER_000001",
        buildingId = "BUILDING_001",
        unitNumber = "101",
        postalCode = "400001"
      )

    val address2 =
      Address(
        id = "ADDRESS_000001",
        customerId = "CUSTOMER_000001",
        buildingId = "BUILDING_001",
        unitNumber = "101",
        postalCode = "400001"
      )

    assert(address1 == address2)
  }

  test("Address should distinguish different addresses") {

    val address1 =
      Address(
        id = "ADDRESS_000001",
        customerId = "CUSTOMER_000001",
        buildingId = "BUILDING_001",
        unitNumber = "101",
        postalCode = "400001"
      )

    val address2 =
      Address(
        id = "ADDRESS_000002",
        customerId = "CUSTOMER_000001",
        buildingId = "BUILDING_001",
        unitNumber = "102",
        postalCode = "400001"
      )

    assert(address1 != address2)
  }
}