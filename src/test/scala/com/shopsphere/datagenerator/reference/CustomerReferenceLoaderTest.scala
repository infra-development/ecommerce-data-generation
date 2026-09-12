package com.shopsphere.datagenerator.reference.customer

import org.scalatest.funsuite.AnyFunSuite

class CustomerReferenceLoaderTest extends AnyFunSuite {

  test("load should load customer name reference data") {

    val referenceData =
      CustomerReferenceLoader.load(
        "data/reference/customer"
      )

    assert(referenceData.firstNames.nonEmpty)
    assert(referenceData.lastNames.nonEmpty)

    assert(
      referenceData.firstNames.contains("Aarav")
    )

    assert(
      referenceData.lastNames.contains("Sharma")
    )
  }

  test("load should reject missing reference data") {

    val exception =
      intercept[IllegalArgumentException] {
        CustomerReferenceLoader.load(
          "data/reference/missing_customer"
        )
      }

    assert(
      exception.getMessage.contains(
        "Customer reference data file does not exist"
      )
    )
  }
}