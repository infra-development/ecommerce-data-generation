package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Address
import com.shopsphere.datagenerator.reference.GeographyReferenceData

object AddressGenerator {

  def generate(
                addressId: String,
                customerId: String,
                geography: GeographyReferenceData,
                random: RandomGenerator
              ): Address = {

    // An address must always have its own identifier.
    require(
      addressId.nonEmpty,
      "Address ID must not be empty."
    )

    // Customer ownership is mandatory because Customer -> Address
    // is an explicit relationship in the ShopSphere data model.
    require(
      customerId.nonEmpty,
      "Customer ID must not be empty."
    )

    // Sort the buildings before random selection.
    // Geography data is stored in maps, whose iteration order should
    // not be relied upon for deterministic generation.
    val buildings =
      geography.buildings.values.toSeq.sortBy(_.id)

    require(
      buildings.nonEmpty,
      "Cannot generate an address because no buildings are available."
    )

    // Select a deterministic random building from the available
    // geography reference data.
    val building =
      buildings(random.nextInt(buildings.size))

    // Generate a valid floor and unit within the selected building.
    val floor =
      random.nextInt(1, building.floors)

    val unitOnFloor =
      random.nextInt(1, building.unitsPerFloor)

    val unitNumber =
      UnitNumberGenerator.generate(
        building,
        floor,
        unitOnFloor
      )

    // Resolve the building hierarchy so that the postal code comes
    // from the same geographic area as the selected building.
    val hierarchy =
      geography.resolveBuilding(building.id)

    val postalCodes =
      geography
        .postalCodesForArea(hierarchy.area.id)
        .sortBy(_.id)

    require(
      postalCodes.nonEmpty,
      s"Cannot generate an address for building '${building.id}' " +
        s"because area '${hierarchy.area.id}' has no postal codes."
    )

    val postalCode =
      postalCodes(
        random.nextInt(postalCodes.size)
      )

    // customerId establishes the Customer -> Address relationship.
    Address(
      id = addressId,
      customerId = customerId,
      buildingId = building.id,
      unitNumber = unitNumber,
      postalCode = postalCode.code
    )
  }
}