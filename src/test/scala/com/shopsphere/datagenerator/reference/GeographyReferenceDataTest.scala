package com.shopsphere.datagenerator.reference

import org.scalatest.funsuite.AnyFunSuite

class GeographyReferenceDataTest extends AnyFunSuite {

  test("resolveBuilding should resolve the complete geography hierarchy") {

    val geography =
      GeographyLoader.load("data/reference/geography")

    val hierarchy =
      geography.resolveBuilding("BUILDING_002")

    assert(hierarchy.building.buildingNumber == "B")
    assert(hierarchy.society.name == "Shivaji Residency")
    assert(hierarchy.road.name == "Somalwada Road")
    assert(hierarchy.area.name == "Manish Nagar")
    assert(hierarchy.city.name == "Nagpur")
    assert(hierarchy.state.name == "Maharashtra")
    assert(hierarchy.country.name == "India")
  }

  test("resolveBuilding should reject an unknown building") {

    val geography =
      GeographyLoader.load("data/reference/geography")

    val exception =
      intercept[IllegalArgumentException] {
        geography.resolveBuilding("BUILDING_999")
      }

    assert(
      exception.getMessage ==
        "Building 'BUILDING_999' does not exist."
    )
  }
}