package com.shopsphere.datagenerator.reference

case class GeographyReferenceData(
                                   countries: Map[String, Country],
                                   states: Map[String, State],
                                   cities: Map[String, City],
                                   areas: Map[String, Area],
                                   roads: Map[String, Road],
                                   societies: Map[String, Society],
                                   buildings: Map[String, Building],
                                   postalCodes: Map[String, PostalCode]
                                 ) {

  private val statesByCountry: Map[String, Seq[State]] =
    states.values.toSeq.groupBy(_.countryId)

  private val citiesByState: Map[String, Seq[City]] =
    cities.values.toSeq.groupBy(_.stateId)

  private val areasByCity: Map[String, Seq[Area]] =
    areas.values.toSeq.groupBy(_.cityId)

  private val roadsByArea: Map[String, Seq[Road]] =
    roads.values.toSeq.groupBy(_.areaId)

  private val societiesByRoad: Map[String, Seq[Society]] =
    societies.values.toSeq.groupBy(_.roadId)

  private val buildingsBySociety: Map[String, Seq[Building]] =
    buildings.values.toSeq.groupBy(_.societyId)

  private val postalCodesByArea: Map[String, Seq[PostalCode]] =
    postalCodes.values.toSeq.groupBy(_.areaId)

  def statesForCountry(countryId: String): Seq[State] = {
    statesByCountry.getOrElse(countryId, Seq.empty)
  }

  def citiesForState(stateId: String): Seq[City] = {
    citiesByState.getOrElse(stateId, Seq.empty)
  }

  def areasForCity(cityId: String): Seq[Area] = {
    areasByCity.getOrElse(cityId, Seq.empty)
  }

  def roadsForArea(areaId: String): Seq[Road] = {
    roadsByArea.getOrElse(areaId, Seq.empty)
  }

  def societiesForRoad(roadId: String): Seq[Society] = {
    societiesByRoad.getOrElse(roadId, Seq.empty)
  }

  def buildingsForSociety(societyId: String): Seq[Building] = {
    buildingsBySociety.getOrElse(societyId, Seq.empty)
  }

  def postalCodesForArea(areaId: String): Seq[PostalCode] = {
    postalCodesByArea.getOrElse(areaId, Seq.empty)
  }

  def resolveBuilding(buildingId: String): AddressHierarchy = {
    val building =
      buildings.getOrElse(
        buildingId,
        throw new IllegalArgumentException(
          s"Building '$buildingId' does not exist."
        )
      )

    val society =
      societies.getOrElse(
        building.societyId,
        throw new IllegalStateException(
          s"Society '${building.societyId}' referenced by building '$buildingId' does not exist."
        )
      )

    val road =
      roads.getOrElse(
        society.roadId,
        throw new IllegalStateException(
          s"Road '${society.roadId}' referenced by society '${society.id}' does not exist."
        )
      )

    val area =
      areas.getOrElse(
        road.areaId,
        throw new IllegalStateException(
          s"Area '${road.areaId}' referenced by road '${road.id}' does not exist."
        )
      )

    val city =
      cities.getOrElse(
        area.cityId,
        throw new IllegalStateException(
          s"City '${area.cityId}' referenced by area '${area.id}' does not exist."
        )
      )

    val state =
      states.getOrElse(
        city.stateId,
        throw new IllegalStateException(
          s"State '${city.stateId}' referenced by city '${city.id}' does not exist."
        )
      )

    val country =
      countries.getOrElse(
        state.countryId,
        throw new IllegalStateException(
          s"Country '${state.countryId}' referenced by state '${state.id}' does not exist."
        )
      )

    AddressHierarchy(
      country = country,
      state = state,
      city = city,
      area = area,
      road = road,
      society = society,
      building = building
    )
  }
}