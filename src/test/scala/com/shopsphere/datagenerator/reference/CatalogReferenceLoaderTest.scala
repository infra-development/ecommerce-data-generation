package com.shopsphere.datagenerator.reference.catalog

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Paths

class CatalogReferenceLoaderTest extends AnyFunSuite {

  test("load should load categories, brands, and product types") {

    val referenceData =
      CatalogReferenceLoader.load(
        basePath = Paths.get("data/reference/catalog")
      )

    assert(referenceData.categories.nonEmpty)
    assert(referenceData.brands.nonEmpty)
    assert(referenceData.productTypes.nonEmpty)

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

    assert(
      referenceData.productTypes.exists(
        _.name == "Smartphone"
      )
    )
  }

  test("load should preserve product type category relationships") {

    val referenceData =
      CatalogReferenceLoader.load(
        basePath = Paths.get("data/reference/catalog")
      )

    val smartphone =
      referenceData.productTypes.find(
        _.name == "Smartphone"
      )

    assert(smartphone.nonEmpty)

    assert(
      smartphone.get.categoryId == "CATEGORY_002"
    )

    assert(
      referenceData.categories.exists(
        category =>
          category.id == smartphone.get.categoryId &&
            category.name == "Mobiles"
      )
    )
  }

  test("load should reject missing catalog reference data") {

    val exception =
      intercept[IllegalArgumentException] {

        CatalogReferenceLoader.load(
          basePath =
            Paths.get("data/reference/missing_catalog")
        )
      }

    assert(
      exception.getMessage.contains(
        "Catalog reference path does not exist"
      )
    )
  }
}