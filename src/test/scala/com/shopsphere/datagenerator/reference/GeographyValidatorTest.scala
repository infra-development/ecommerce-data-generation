package com.shopsphere.datagenerator.reference

import org.scalatest.funsuite.AnyFunSuite

class GeographyValidatorTest extends AnyFunSuite {

  test("validate should reject duplicate country IDs") {

    val countries = Seq(
      Country(
        id = "COUNTRY_001",
        name = "India",
        isoCode = "IN"
      ),
      Country(
        id = "COUNTRY_001",
        name = "United States",
        isoCode = "US"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = countries,
          states = Seq.empty,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "Duplicate countries IDs: COUNTRY_001"
    )
  }

  test("validate should reject a state referencing an unknown country") {

    val states = Seq(
      State(
        id = "STATE_001",
        countryId = "COUNTRY_999",
        name = "Maharashtra",
        code = "MH"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = states,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "State 'STATE_001' references country 'COUNTRY_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("validate should reject a city referencing an unknown state") {

    val cities = Seq(
      City(
        id = "CITY_001",
        stateId = "STATE_999",
        name = "Nagpur"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = Seq.empty,
          cities = cities,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "City 'CITY_001' references state 'STATE_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("validate should reject an area referencing an unknown city") {

    val areas = Seq(
      Area(
        id = "AREA_001",
        cityId = "CITY_999",
        name = "Manish Nagar"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = Seq.empty,
          cities = Seq.empty,
          areas = areas,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "Area 'AREA_001' references city 'CITY_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("validate should reject a road referencing an unknown area") {

    val roads = Seq(
      Road(
        id = "ROAD_001",
        areaId = "AREA_999",
        name = "Somalwada Road"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = Seq.empty,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = roads,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "Road 'ROAD_001' references area 'AREA_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("validate should reject a society referencing an unknown road") {

    val societies = Seq(
      Society(
        id = "SOCIETY_001",
        roadId = "ROAD_999",
        name = "Shivaji Residency"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = Seq.empty,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = societies,
          buildings = Seq.empty,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "Society 'SOCIETY_001' references road 'ROAD_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("validate should reject a building referencing an unknown society") {

    val buildings = Seq(
      Building(
        id = "BUILDING_001",
        societyId = "SOCIETY_999",
        buildingNumber = "B",
        floors = 8,
        unitsPerFloor = 4
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = Seq.empty,
          states = Seq.empty,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = buildings,
          postalCodes = Seq.empty
        )
      }

    assert(
      exception.getMessage ==
        "Building 'BUILDING_001' references society 'SOCIETY_999', " +
          "but the referenced ID does not exist."
    )
  }

  test("reject postal code referencing unknown area") {

    val countries = Seq(
      Country(
        id = "COUNTRY_001",
        name = "India",
        isoCode = "IN"
      )
    )

    val postalCodes = Seq(
      PostalCode(
        id = "POSTAL_CODE_001",
        areaId = "AREA_999",
        code = "440015"
      )
    )

    val exception =
      intercept[IllegalArgumentException] {
        GeographyValidator.validate(
          countries = countries,
          states = Seq.empty,
          cities = Seq.empty,
          areas = Seq.empty,
          roads = Seq.empty,
          societies = Seq.empty,
          buildings = Seq.empty,
          postalCodes = postalCodes
        )
      }

    assert(
      exception.getMessage ==
        "Postal code 'POSTAL_CODE_001' references area 'AREA_999', but the referenced ID does not exist."
    )
  }
}