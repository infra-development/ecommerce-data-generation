package com.shopsphere.datagenerator.relationship

import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator,
  WeightedDistribution
}
import com.shopsphere.datagenerator.model.Product
import org.scalatest.funsuite.AnyFunSuite

class OrderRelationshipGeneratorTest extends AnyFunSuite {

  private val products =
    Seq(
      Product(
        id = "PRODUCT_000001",
        name = "Samsung Electronics Product",
        categoryId = "CATEGORY_001",
        productTypeId = "PRODUCT_TYPE_001",
        brandId = "BRAND_001",
        productModelId = "PRODUCT_MODEL_001",
        price = BigDecimal("1000.00")
      ),
      Product(
        id = "PRODUCT_000002",
        name = "Apple Mobiles Product",
        categoryId = "CATEGORY_002",
        productTypeId = "PRODUCT_TYPE_004",
        brandId = "BRAND_002",
        productModelId = "PRODUCT_MODEL_002",
        price = BigDecimal("2500.00")
      ),
      Product(
        id = "PRODUCT_000003",
        name = "LG Television Product",
        categoryId = "CATEGORY_003",
        productTypeId = "PRODUCT_TYPE_006",
        brandId = "BRAND_003",
        productModelId = "PRODUCT_MODEL_003",
        price = BigDecimal("5000.00")
      )
    )

  private val distributionEngine =
    new DistributionEngine(
      Map(
        "order_status" ->
          new WeightedDistribution(
            Seq(
              "PLACED" -> 0.05,
              "CONFIRMED" -> 0.15,
              "SHIPPED" -> 0.15,
              "DELIVERED" -> 0.60,
              "CANCELLED" -> 0.05
            )
          ),
        "payment_method" ->
          new WeightedDistribution(
            Seq(
              "UPI" -> 0.45,
              "CREDIT_CARD" -> 0.20,
              "DEBIT_CARD" -> 0.15,
              "NET_BANKING" -> 0.10,
              "WALLET" -> 0.10
            )
          )
      )
    )

  test("should generate all required order relationships") {

    val result =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(result.order.id == "ORDER_000001")
    assert(result.order.customerId == "CUSTOMER_000001")

    assert(result.orderItems.size == 3)

    assert(
      result.payment.orderId ==
        result.order.id
    )

    assert(
      result.shipment.orderId ==
        result.order.id
    )
  }

  test("should calculate order total from order items") {

    val result =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val expectedTotal =
      result.orderItems
        .map(_.lineAmount)
        .sum
        .setScale(
          2,
          BigDecimal.RoundingMode.HALF_UP
        )

    assert(
      result.order.totalAmount ==
        expectedTotal
    )
  }

  test("should use valid products for all order items") {

    val productIds =
      products.map(_.id).toSet

    val result =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 5,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    result.orderItems.foreach { item =>
      assert(
        productIds.contains(item.productId)
      )
    }
  }

  test("should maintain order ID across all child entities") {

    val result =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      result.orderItems.forall(
        _.orderId == result.order.id
      )
    )

    assert(
      result.payment.orderId ==
        result.order.id
    )

    assert(
      result.shipment.orderId ==
        result.order.id
    )

    result.returnRecord.foreach { returnRecord =>
      assert(
        returnRecord.orderId ==
          result.order.id
      )
    }
  }

  test("should keep payment amount equal to order total") {

    val result =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      result.payment.amount ==
        result.order.totalAmount
    )
  }

  test("should generate deterministic relationships for the same seed") {

    val result1 =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val result2 =
      OrderRelationshipGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        products = products,
        itemCount = 3,
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(result1 == result2)
  }

  test("should reject an empty product collection") {

    val exception =
      intercept[IllegalArgumentException] {

        OrderRelationshipGenerator.generate(
          orderId = "ORDER_000001",
          customerId = "CUSTOMER_000001",
          products = Seq.empty,
          itemCount = 3,
          random = new RandomGenerator(42L),
          distributionEngine = distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Products must not be empty."
    )
  }

  test("should reject a non-positive item count") {

    val exception =
      intercept[IllegalArgumentException] {

        OrderRelationshipGenerator.generate(
          orderId = "ORDER_000001",
          customerId = "CUSTOMER_000001",
          products = products,
          itemCount = 0,
          random = new RandomGenerator(42L),
          distributionEngine = distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Item count must be greater than zero."
    )
  }

  test("should maintain temporal consistency across the order lifecycle") {

    val lifecycleProducts =
      Seq(
        Product(
          id = "PRODUCT_001",
          name = "Samsung Mobile",
          categoryId = "CATEGORY_002",
          productTypeId = "PRODUCT_TYPE_004",
          brandId = "BRAND_001",
          productModelId = "PRODUCT_MODEL_001",
          price = BigDecimal("10000.00")
        ),
        Product(
          id = "PRODUCT_002",
          name = "Nike Shoes",
          categoryId = "CATEGORY_007",
          productTypeId = "PRODUCT_TYPE_020",
          brandId = "BRAND_009",
          productModelId = "PRODUCT_MODEL_002",
          price = BigDecimal("5000.00")
        )
      )

    (1 to 100).foreach { seed =>

      val result =
        OrderRelationshipGenerator.generate(
          orderId = "ORDER_000001",
          customerId = "CUSTOMER_000001",
          products = lifecycleProducts,
          itemCount = 2,
          random = new RandomGenerator(seed.toLong),
          distributionEngine = distributionEngine
        )

      val order = result.order
      val payment = result.payment
      val shipment = result.shipment

      assert(
        !payment.paymentDate.isBefore(
          order.orderDate
        )
      )

      shipment.shippedDate.foreach { shippedDate =>
        assert(
          !shippedDate.isBefore(
            payment.paymentDate
          )
        )
      }

      shipment.deliveredDate.foreach { deliveredDate =>
        assert(
          shipment.shippedDate.exists { shippedDate =>
            !deliveredDate.isBefore(
              shippedDate
            )
          }
        )
      }

      result.returnRecord.foreach { returnRecord =>

        assert(
          order.status == "DELIVERED"
        )

        assert(
          shipment.deliveredDate.exists { deliveredDate =>
            !returnRecord.returnDate.isBefore(
              deliveredDate
            )
          }
        )
      }
    }
  }

  test("should not generate a return for a non-delivered order") {

    val lifecycleProducts =
      Seq(
        Product(
          id = "PRODUCT_001",
          name = "Samsung Mobile",
          categoryId = "CATEGORY_002",
          productTypeId = "PRODUCT_TYPE_004",
          brandId = "BRAND_001",
          productModelId = "PRODUCT_MODEL_001",
          price = BigDecimal("10000.00")
        )
      )

    (1 to 100).foreach { seed =>

      val result =
        OrderRelationshipGenerator.generate(
          orderId = "ORDER_000001",
          customerId = "CUSTOMER_000001",
          products = lifecycleProducts,
          itemCount = 1,
          random = new RandomGenerator(seed.toLong),
          distributionEngine = distributionEngine
        )

      if (result.order.status != "DELIVERED") {
        assert(
          result.returnRecord.isEmpty
        )
      }
    }
  }

  test("should use the configured order status distribution") {

    val statuses =
      (1 to 1000).map { seed =>

        OrderRelationshipGenerator
          .generate(
            orderId = "ORDER_000001",
            customerId = "CUSTOMER_000001",
            products = products,
            itemCount = 1,
            random = new RandomGenerator(seed.toLong),
            distributionEngine = distributionEngine
          )
          .order
          .status
      }

    val deliveredCount =
      statuses.count(_ == "DELIVERED")

    val deliveredRatio =
      deliveredCount.toDouble / statuses.size

    /*
     * Configured probability is 60%.
     * Allow a reasonable statistical tolerance.
     */
    assert(
      deliveredRatio > 0.50 &&
        deliveredRatio < 0.70
    )
  }
}