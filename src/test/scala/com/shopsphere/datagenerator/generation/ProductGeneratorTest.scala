package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceLoader
import org.scalatest.funsuite.AnyFunSuite

class ProductGeneratorTest extends AnyFunSuite {

  private val referenceData =
    CatalogReferenceLoader.load(
      "data/reference/catalog"
    )

  test("generate should create a product with valid references") {

    val product =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    assert(product.id == "PRODUCT_000001")
    assert(product.name.nonEmpty)
    assert(product.price >= BigDecimal("100.00"))
    assert(product.price < BigDecimal("100000.00"))

    assert(
      referenceData.categories.map(_.id).contains(
        product.categoryId
      )
    )

    assert(
      referenceData.brands.map(_.id).contains(
        product.brandId
      )
    )
  }

  test("generate should be reproducible with the same seed") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    assert(product1 == product2)
  }

  test("generate should produce different results with different seeds") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        random = new RandomGenerator(99L)
      )

    assert(product1 != product2)
  }

  test("generate should reject an empty product ID") {

    intercept[IllegalArgumentException] {
      ProductGenerator.generate(
        productId = "",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )
    }
  }
}