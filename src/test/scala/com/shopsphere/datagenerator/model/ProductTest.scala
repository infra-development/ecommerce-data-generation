package com.shopsphere.datagenerator.model

import org.scalatest.funsuite.AnyFunSuite

class ProductTest extends AnyFunSuite {

  test("Product should store product attributes") {

    val product =
      Product(
        id = "PRODUCT_000001",
        name = "Samsung Smartphone",
        categoryId = "CATEGORY_002",
        productTypeId = "PRODUCT_TYPE_004",
        brandId = "BRAND_001",
        productModelId = "PRODUCT_MODEL_001",
        price = BigDecimal("24999.99")
      )

    assert(product.id == "PRODUCT_000001")
    assert(product.name == "Samsung Smartphone")
    assert(product.categoryId == "CATEGORY_002")
    assert(product.productTypeId == "PRODUCT_TYPE_004")
    assert(product.brandId == "BRAND_001")
    assert(product.price == BigDecimal("24999.99"))
  }
}