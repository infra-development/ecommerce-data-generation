package com.shopsphere.datagenerator.reference.catalog

import org.scalatest.funsuite.AnyFunSuite

class CatalogReferenceLoaderTest extends AnyFunSuite {

  test("load should load categories and brands") {

    val referenceData =
      CatalogReferenceLoader.load(
        "data/reference/catalog"
      )

    assert(referenceData.categories.nonEmpty)
    assert(referenceData.brands.nonEmpty)

    assert(
      referenceData.categories.exists(
        _.name == "Electronics"
      )
    )

    assert(
      referenceData.brands.exists(
        _.name == "Samsung"
      )
    )
  }

  test("load should reject missing catalog reference data") {

    val exception =
      intercept[IllegalArgumentException] {
        CatalogReferenceLoader.load(
          "data/reference/missing_catalog"
        )
      }

    assert(
      exception.getMessage.contains(
        "Catalog reference data file does not exist"
      )
    )
  }
}