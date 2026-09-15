package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.distribution.{DistributionEngine, WeightedDistribution}
import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class OrderGeneratorTest extends AnyFunSuite {

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
          )
      )
    )

  test("should generate an order with the supplied IDs") {

    val order =
      OrderGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(order.id == "ORDER_000001")
    assert(order.customerId == "CUSTOMER_000001")
  }

  test("should generate a valid order status") {

    val validStatuses =
      Set(
        "PLACED",
        "CONFIRMED",
        "SHIPPED",
        "DELIVERED",
        "CANCELLED"
      )

    val order =
      OrderGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      validStatuses.contains(order.status)
    )
  }

  test("should initialize total amount to zero") {

    val order =
      OrderGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      order.totalAmount == BigDecimal(0)
    )
  }

  test("should produce the same order for the same seed") {

    val order1 =
      OrderGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val order2 =
      OrderGenerator.generate(
        orderId = "ORDER_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      order1 == order2
    )
  }

  test("should reject an empty order ID") {

    val exception =
      intercept[IllegalArgumentException] {

        OrderGenerator.generate(
          orderId = "",
          customerId = "CUSTOMER_000001",
          random = new RandomGenerator(42L),
          distributionEngine = distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Order ID must not be empty."
    )
  }

  test("should reject an empty customer ID") {

    val exception =
      intercept[IllegalArgumentException] {

        OrderGenerator.generate(
          orderId = "ORDER_000001",
          customerId = "",
          random = new RandomGenerator(42L),
          distributionEngine = distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Customer ID must not be empty."
    )
  }

  test("should use the configured order status distribution") {

    val statuses =
      (1 to 1000).map { seed =>

        OrderGenerator
          .generate(
            orderId = "ORDER_000001",
            customerId = "CUSTOMER_000001",
            random = new RandomGenerator(seed.toLong),
            distributionEngine = distributionEngine
          )
          .status
      }

    val deliveredCount =
      statuses.count(_ == "DELIVERED")

    val deliveredRatio =
      deliveredCount.toDouble / statuses.size

    assert(
      deliveredRatio > 0.50 &&
        deliveredRatio < 0.70
    )
  }

  test("should produce different statuses from different seeds") {

    val statuses =
      (1 to 100).map { seed =>

        OrderGenerator
          .generate(
            orderId = "ORDER_000001",
            customerId = "CUSTOMER_000001",
            random = new RandomGenerator(seed.toLong),
            distributionEngine = distributionEngine
          )
          .status
      }

    assert(
      statuses.distinct.size > 1
    )
  }
}