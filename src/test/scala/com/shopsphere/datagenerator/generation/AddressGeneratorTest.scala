package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.reference.GeographyLoader
import org.scalatest.funsuite.AnyFunSuite

class AddressGeneratorTest extends AnyFunSuite {

  private val geography =
    GeographyLoader.load("data/reference/geography")

  private def generateAddress(
                               seed: Long,
                               addressId: String = "ADDRESS_000001",
                               customerId: String = "CUSTOMER_000001"
                             ): com.shopsphere.datagenerator.model.Address = {

    AddressGenerator.generate(
      addressId = addressId,
      customerId = customerId,
      geography = geography,
      random = new RandomGenerator(seed)
    )
  }

  test("generate should create an address with valid references") {

    val address =
      generateAddress(42L)

    assert(address.id == "ADDRESS_000001")
    assert(address.customerId == "CUSTOMER_000001")

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

  test("generate should preserve the customer relationship") {

    val address =
      generateAddress(
        seed = 42L,
        customerId = "CUSTOMER_000123"
      )

    assert(
      address.customerId == "CUSTOMER_000123"
    )
  }

  test("generate should preserve the address ID") {

    val address =
      generateAddress(
        seed = 42L,
        addressId = "ADDRESS_000987"
      )

    assert(
      address.id == "ADDRESS_000987"
    )
  }

  test("generate should reject an empty address ID") {

    val exception =
      intercept[IllegalArgumentException] {

        generateAddress(
          seed = 42L,
          addressId = ""
        )
      }

    assert(
      exception.getMessage ==
        "requirement failed: Address ID must not be empty."
    )
  }

  test("generate should reject an empty customer ID") {

    val exception =
      intercept[IllegalArgumentException] {

        generateAddress(
          seed = 42L,
          customerId = ""
        )
      }

    assert(
      exception.getMessage ==
        "requirement failed: Customer ID must not be empty."
    )
  }

  test("generate should be reproducible with the same seed") {

    val address1 =
      generateAddress(42L)

    val address2 =
      generateAddress(42L)

    assert(address1 == address2)
  }

  test("generate should produce different results with different seeds") {

    val address1 =
      generateAddress(42L)

    val address2 =
      generateAddress(99L)

    assert(address1 != address2)
  }

  test("generate should produce a valid unit number for the selected building") {

    val addresses =
      (1L to 100L).map { seed =>
        generateAddress(seed)
      }

    addresses.foreach { address =>

      val building =
        geography.buildings(address.buildingId)

      assert(
        address.unitNumber.startsWith(building.buildingNumber)
      )

      assert(address.unitNumber.nonEmpty)
    }
  }

  test("generate should produce a postal code belonging to the building area") {

    val addresses =
      (1L to 100L).map { seed =>
        generateAddress(seed)
      }

    addresses.foreach { address =>

      val hierarchy =
        geography.resolveBuilding(address.buildingId)

      val postalCodes =
        geography
          .postalCodesForArea(hierarchy.area.id)
          .map(_.code)

      assert(
        postalCodes.contains(address.postalCode)
      )
    }
  }

  test("generate should fail when no buildings are available") {

    val emptyGeography =
      geography.copy(
        buildings = Map.empty
      )

    val exception =
      intercept[IllegalArgumentException] {

        AddressGenerator.generate(
          addressId = "ADDRESS_000001",
          customerId = "CUSTOMER_000001",
          geography = emptyGeography,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage ==
        "requirement failed: Cannot generate an address because no buildings are available."
    )
  }

  test("generate should fail when the selected building area has no postal codes") {

    val building =
      geography.buildings.values.toSeq.sortBy(_.id).head

    val hierarchy =
      geography.resolveBuilding(building.id)

    val postalCodesWithoutArea =
      geography.postalCodes.values
        .filterNot(_.areaId == hierarchy.area.id)
        .map(postalCode => postalCode.id -> postalCode)
        .toMap

    val modifiedGeography =
      geography.copy(
        postalCodes = postalCodesWithoutArea
      )

    val exception =
      intercept[IllegalArgumentException] {

        AddressGenerator.generate(
          addressId = "ADDRESS_000001",
          customerId = "CUSTOMER_000001",
          geography = modifiedGeography,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage.contains(
        s"area '${hierarchy.area.id}' has no postal codes"
      )
    )
  }
}