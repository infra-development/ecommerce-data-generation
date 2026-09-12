package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.reference.Building

object UnitNumberGenerator {

  def generate(
                building: Building,
                floor: Int,
                unitOnFloor: Int
              ): String = {

    if (floor < 1 || floor > building.floors) {
      throw new IllegalArgumentException(
        s"Floor must be between 1 and ${building.floors}."
      )
    }

    if (unitOnFloor < 1 || unitOnFloor > building.unitsPerFloor) {
      throw new IllegalArgumentException(
        s"Unit on floor must be between 1 and ${building.unitsPerFloor}."
      )
    }

    s"${building.buildingNumber}$floor${f"$unitOnFloor%02d"}"
  }
}