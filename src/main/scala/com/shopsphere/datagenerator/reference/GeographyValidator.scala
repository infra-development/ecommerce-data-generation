package com.shopsphere.datagenerator.reference

object GeographyValidator {

  def validate(
                countries: Seq[Country],
                states: Seq[State],
                cities: Seq[City],
                areas: Seq[Area],
                roads: Seq[Road],
                societies: Seq[Society],
                buildings: Seq[Building],
                postalCodes: Seq[PostalCode]
              ): Unit = {

    validateUniqueIds("countries", countries.map(_.id))
    validateUniqueIds("states", states.map(_.id))
    validateUniqueIds("cities", cities.map(_.id))
    validateUniqueIds("areas", areas.map(_.id))
    validateUniqueIds("roads", roads.map(_.id))
    validateUniqueIds("societies", societies.map(_.id))
    validateUniqueIds("buildings", buildings.map(_.id))
    validateUniqueIds("postal_codes", postalCodes.map(_.id))

    validateRelationships(
      countries,
      states,
      cities,
      areas,
      roads,
      societies,
      buildings,
      postalCodes
    )
  }

  private def validateUniqueIds(
                                 entityName: String,
                                 ids: Seq[String]
                               ): Unit = {

    val duplicates =
      ids
        .groupBy(identity)
        .collect {
          case (id, values) if values.size > 1 =>
            id
        }

    if (duplicates.nonEmpty) {
      throw new IllegalArgumentException(
        s"Duplicate $entityName IDs: " +
          duplicates.toSeq.sorted.mkString(", ")
      )
    }
  }

  private def validateRelationships(
                                     countries: Seq[Country],
                                     states: Seq[State],
                                     cities: Seq[City],
                                     areas: Seq[Area],
                                     roads: Seq[Road],
                                     societies: Seq[Society],
                                     buildings: Seq[Building],
                                     postalCodes: Seq[PostalCode]
                                   ): Unit = {

    val countryIds = countries.map(_.id).toSet
    val stateIds = states.map(_.id).toSet
    val cityIds = cities.map(_.id).toSet
    val areaIds = areas.map(_.id).toSet
    val roadIds = roads.map(_.id).toSet
    val societyIds = societies.map(_.id).toSet

    states.foreach { state =>
      requireReference(
        state.countryId,
        countryIds,
        s"State '${state.id}' references country"
      )
    }

    cities.foreach { city =>
      requireReference(
        city.stateId,
        stateIds,
        s"City '${city.id}' references state"
      )
    }

    areas.foreach { area =>
      requireReference(
        area.cityId,
        cityIds,
        s"Area '${area.id}' references city"
      )
    }

    roads.foreach { road =>
      requireReference(
        road.areaId,
        areaIds,
        s"Road '${road.id}' references area"
      )
    }

    societies.foreach { society =>
      requireReference(
        society.roadId,
        roadIds,
        s"Society '${society.id}' references road"
      )
    }

    buildings.foreach { building =>
      requireReference(
        building.societyId,
        societyIds,
        s"Building '${building.id}' references society"
      )
    }

    postalCodes.foreach { postalCode =>
      requireReference(
        postalCode.areaId,
        areaIds,
        s"Postal code '${postalCode.id}' references area"
      )
    }
  }

  private def requireReference(
                                referencedId: String,
                                validIds: Set[String],
                                description: String
                              ): Unit = {

    if (!validIds.contains(referencedId)) {
      throw new IllegalArgumentException(
        s"$description '$referencedId', " +
          "but the referenced ID does not exist."
      )
    }
  }
}