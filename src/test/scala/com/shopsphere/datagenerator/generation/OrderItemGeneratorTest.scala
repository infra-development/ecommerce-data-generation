package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Product
import org.scalatest.funsuite.AnyFunSuite

class OrderItemGeneratorTest extends AnyFunSuite {

  private val product =
    Product(
      id = "PRODUCT_000001",
      name = "Samsung Electronics Product",
      categoryId = "CATEGORY_001",
      brandId = "BRAND_001",
      price = BigDecimal("24999.99")
    )

  test("should generate an order item for the supplied order and product") {

    val item =
      OrderItemGenerator.generate(
        orderItemId = "ORDER_000001_ITEM_001",
        orderId = "ORDER_000001",
        product = product,
        random = new RandomGenerator(42L)
      )

    assert(item.id == "ORDER_000001_ITEM_001")
    assert(item.orderId == "ORDER_000001")
    assert(item.productId == "PRODUCT_000001")
  }

  test("should use the product price as unit price") {

    val item =
      OrderItemGenerator.generate(
        "ORDER_000001_ITEM_001",
        "ORDER_000001",
        product,
        new RandomGenerator(42L)
      )

    assert(item.unitPrice == product.price)
  }

  test("should calculate line amount from quantity and unit price") {

    val item =
      OrderItemGenerator.generate(
        "ORDER_000001_ITEM_001",
        "ORDER_000001",
        product,
        new RandomGenerator(42L)
      )

    val expected =
      (product.price * item.quantity)
        .setScale(
          2,
          BigDecimal.RoundingMode.HALF_UP
        )

    assert(item.lineAmount == expected)
  }

  test("should generate quantity between 1 and 5") {

    val item =
      OrderItemGenerator.generate(
        "ORDER_000001_ITEM_001",
        "ORDER_000001",
        product,
        new RandomGenerator(42L)
      )

    assert(item.quantity >= 1)
    assert(item.quantity <= 5)
  }

  test("should produce the same item for the same seed") {

    val item1 =
      OrderItemGenerator.generate(
        "ORDER_000001_ITEM_001",
        "ORDER_000001",
        product,
        new RandomGenerator(42L)
      )

    val item2 =
      OrderItemGenerator.generate(
        "ORDER_000001_ITEM_001",
        "ORDER_000001",
        product,
        new RandomGenerator(42L)
      )

    assert(item1 == item2)
  }
}