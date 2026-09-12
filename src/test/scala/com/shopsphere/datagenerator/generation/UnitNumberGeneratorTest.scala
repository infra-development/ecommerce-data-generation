package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.reference.Building
import org.scalatest.funsuite.AnyFunSuite

class UnitNumberGeneratorTest extends AnyFunSuite {

  private val building =
    Building(
      id = "BUILDING_002",
      societyId = "SOCIETY_001",
      buildingNumber = "B",
      floors = 8,
      unitsPerFloor = 4
    )

  test("generate should create a valid unit number") {

    assert(
      UnitNumberGenerator.generate(
        building,
        floor = 1,
        unitOnFloor = 1
      ) == "B101"
    )

    assert(
      UnitNumberGenerator.generate(
        building,
        floor = 8,
        unitOnFloor = 4
      ) == "B804"
    )
  }

  test("generate should reject an invalid floor") {

    val exception =
      intercept[IllegalArgumentException] {
        UnitNumberGenerator.generate(
          building,
          floor = 9,
          unitOnFloor = 1
        )
      }

    assert(
      exception.getMessage ==
        "Floor must be between 1 and 8."
    )
  }

  test("generate should reject an invalid unit on floor") {

    val exception =
      intercept[IllegalArgumentException] {
        UnitNumberGenerator.generate(
          building,
          floor = 1,
          unitOnFloor = 5
        )
      }

    assert(
      exception.getMessage ==
        "Unit on floor must be between 1 and 4."
    )
  }

  test("generate should reject floor zero") {

    intercept[IllegalArgumentException] {
      UnitNumberGenerator.generate(
        building,
        floor = 0,
        unitOnFloor = 1
      )
    }
  }

  test("generate should reject unit zero") {

    intercept[IllegalArgumentException] {
      UnitNumberGenerator.generate(
        building,
        floor = 1,
        unitOnFloor = 0
      )
    }
  }
}