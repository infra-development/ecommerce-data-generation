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

    require(
      addressId.nonEmpty,
      "Address ID must not be empty."
    )

    require(
      customerId.nonEmpty,
      "Customer ID must not be empty."
    )

    val buildings =
      geography.buildings.values.toSeq.sortBy(_.id)

    require(
      buildings.nonEmpty,
      "Cannot generate an address because no buildings are available."
    )

    val building =
      buildings(
        random
          .derive("building")
          .nextInt(buildings.size)
      )

    val floor =
      random
        .derive("floor")
        .nextInt(1, building.floors)

    val unitOnFloor =
      random
        .derive("unit-on-floor")
        .nextInt(1, building.unitsPerFloor)

    val unitNumber =
      UnitNumberGenerator.generate(
        building,
        floor,
        unitOnFloor
      )

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
        random
          .derive("postal-code")
          .nextInt(postalCodes.size)
      )

    Address(
      id = addressId,
      customerId = customerId,
      buildingId = building.id,
      unitNumber = unitNumber,
      postalCode = postalCode.code
    )
  }
}