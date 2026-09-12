package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.reference.GeographyLoader
import org.scalatest.funsuite.AnyFunSuite

class AddressGeneratorTest extends AnyFunSuite {

  private val geography =
    GeographyLoader.load("data/reference/geography")

  test("generate should create an address with valid references") {

    val random =
      new RandomGenerator(42L)

    val address =
      AddressGenerator.generate(
        addressId = "ADDRESS_000001",
        geography = geography,
        random = random
      )

    assert(address.id == "ADDRESS_000001")

    assert(
      geography.buildings.contains(address.buildingId)
    )

    val building =
      geography.buildings(address.buildingId)

    assert(
      address.unitNumber.startsWith(building.buildingNumber)
    )

    val hierarchy =
      geography.resolveBuilding(address.buildingId)

    val postalCodes =
      geography.postalCodesForArea(hierarchy.area.id)

    assert(
      postalCodes.map(_.code).contains(address.postalCode)
    )
  }

  test("generate should be reproducible with the same seed") {

    val address1 =
      AddressGenerator.generate(
        addressId = "ADDRESS_000001",
        geography = geography,
        random = new RandomGenerator(42L)
      )

    val address2 =
      AddressGenerator.generate(
        addressId = "ADDRESS_000001",
        geography = geography,
        random = new RandomGenerator(42L)
      )

    assert(address1 == address2)
  }

  test("generate should produce different results with different seeds") {

    val address1 =
      AddressGenerator.generate(
        addressId = "ADDRESS_000001",
        geography = geography,
        random = new RandomGenerator(42L)
      )

    val address2 =
      AddressGenerator.generate(
        addressId = "ADDRESS_000001",
        geography = geography,
        random = new RandomGenerator(99L)
      )

    assert(address1 != address2)
  }
}