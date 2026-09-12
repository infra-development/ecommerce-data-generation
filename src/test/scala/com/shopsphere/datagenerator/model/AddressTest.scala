package com.shopsphere.datagenerator.model

import org.scalatest.funsuite.AnyFunSuite

class AddressTest extends AnyFunSuite {

  test("Address should store generated address attributes") {

    val address =
      Address(
        id = "ADDRESS_000001",
        buildingId = "BUILDING_002",
        unitNumber = "B402",
        postalCode = "440015"
      )

    assert(address.id == "ADDRESS_000001")
    assert(address.buildingId == "BUILDING_002")
    assert(address.unitNumber == "B402")
    assert(address.postalCode == "440015")
  }

  test("unitNumber should support non-numeric values") {

    val address =
      Address(
        id = "ADDRESS_000002",
        buildingId = "BUILDING_002",
        unitNumber = "GF-03",
        postalCode = "440015"
      )

    assert(address.unitNumber == "GF-03")
  }
}