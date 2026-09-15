package com.shopsphere.datagenerator.relationship

import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDateTime

class OrderLifecycleGeneratorTest extends AnyFunSuite {

  private val orderDate =
    LocalDateTime.of(
      2026,
      1,
      10,
      10,
      0
    )

  test("payment date should not be before order date") {

    val lifecycle =
      OrderLifecycleGenerator.generate(
        orderDate,
        "DELIVERED",
        new RandomGenerator(42L)
      )

    assert(
      !lifecycle.paymentDate.isBefore(orderDate)
    )
  }

  test("shipped date should not be before payment date") {

    val lifecycle =
      OrderLifecycleGenerator.generate(
        orderDate,
        "SHIPPED",
        new RandomGenerator(42L)
      )

    lifecycle.shippedDate.foreach { shippedDate =>
      assert(
        !shippedDate.isBefore(
          lifecycle.paymentDate
        )
      )
    }
  }

  test("delivered date should not be before shipped date") {

    val lifecycle =
      OrderLifecycleGenerator.generate(
        orderDate,
        "DELIVERED",
        new RandomGenerator(42L)
      )

    for {
      shippedDate <- lifecycle.shippedDate
      deliveredDate <- lifecycle.deliveredDate
    } {
      assert(
        !deliveredDate.isBefore(shippedDate)
      )
    }
  }

  test("return date should not be before delivered date") {

    /*
     * A return is probabilistic, so use seeds until we find
     * a lifecycle that actually generates one.
     */
    val lifecycleWithReturn =
      (1 to 1000)
        .map { seed =>
          OrderLifecycleGenerator.generate(
            orderDate,
            "DELIVERED",
            new RandomGenerator(seed.toLong)
          )
        }
        .find(_.returnDate.nonEmpty)

    assert(
      lifecycleWithReturn.nonEmpty
    )

    val lifecycle =
      lifecycleWithReturn.get

    for {
      deliveredDate <- lifecycle.deliveredDate
      returnDate <- lifecycle.returnDate
    } {
      assert(
        !returnDate.isBefore(deliveredDate)
      )
    }
  }

  test("return should only exist for delivered orders") {

    OrderLifecycle.statuses.foreach { orderStatus =>

      (1 to 100).foreach { seed =>

        val lifecycle =
          OrderLifecycleGenerator.generate(
            orderDate,
            orderStatus,
            new RandomGenerator(seed.toLong)
          )

        if (lifecycle.returnDate.nonEmpty) {

          assert(
            orderStatus == "DELIVERED"
          )

          assert(
            lifecycle.orderStatus == "DELIVERED"
          )

          assert(
            lifecycle.deliveredDate.nonEmpty
          )

          assert(
            lifecycle.returnStatus.nonEmpty
          )
        }
      }
    }
  }

  test("cancelled order should have failed payment") {

    (1 to 100).foreach { seed =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          "CANCELLED",
          new RandomGenerator(seed.toLong)
        )

      assert(
        lifecycle.orderStatus == "CANCELLED"
      )

      assert(
        lifecycle.paymentStatus == "FAILED"
      )

      assert(
        lifecycle.shippedDate.isEmpty
      )

      assert(
        lifecycle.deliveredDate.isEmpty
      )

      assert(
        lifecycle.returnDate.isEmpty
      )
    }
  }

  test("placed order should remain unshipped") {

    (1 to 100).foreach { seed =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          "PLACED",
          new RandomGenerator(seed.toLong)
        )

      assert(
        lifecycle.orderStatus == "PLACED"
      )

      assert(
        lifecycle.shipmentStatus == "PROCESSING"
      )

      assert(
        lifecycle.shippedDate.isEmpty
      )

      assert(
        lifecycle.deliveredDate.isEmpty
      )

      assert(
        lifecycle.returnDate.isEmpty
      )
    }
  }

  test("confirmed order should remain unshipped") {

    (1 to 100).foreach { seed =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          "CONFIRMED",
          new RandomGenerator(seed.toLong)
        )

      assert(
        lifecycle.orderStatus == "CONFIRMED"
      )

      assert(
        lifecycle.shipmentStatus == "PROCESSING"
      )

      assert(
        lifecycle.shippedDate.isEmpty
      )

      assert(
        lifecycle.deliveredDate.isEmpty
      )

      assert(
        lifecycle.returnDate.isEmpty
      )
    }
  }

  test("shipped order should have shipment date but no delivery date") {

    (1 to 100).foreach { seed =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          "SHIPPED",
          new RandomGenerator(seed.toLong)
        )

      assert(
        lifecycle.orderStatus == "SHIPPED"
      )

      assert(
        lifecycle.shipmentStatus == "IN_TRANSIT"
      )

      assert(
        lifecycle.shippedDate.nonEmpty
      )

      assert(
        lifecycle.deliveredDate.isEmpty
      )

      assert(
        lifecycle.returnDate.isEmpty
      )
    }
  }

  test("delivered order should have shipment and delivery dates") {

    (1 to 100).foreach { seed =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          "DELIVERED",
          new RandomGenerator(seed.toLong)
        )

      assert(
        lifecycle.orderStatus == "DELIVERED"
      )

      assert(
        lifecycle.shipmentStatus == "DELIVERED"
      )

      assert(
        lifecycle.shippedDate.nonEmpty
      )

      assert(
        lifecycle.deliveredDate.nonEmpty
      )

      assert(
        lifecycle.returnDate.forall { returnDate =>
          !returnDate.isBefore(
            lifecycle.deliveredDate.get
          )
        }
      )
    }
  }

  test("same seed and status should produce same lifecycle") {

    val lifecycle1 =
      OrderLifecycleGenerator.generate(
        orderDate,
        "DELIVERED",
        new RandomGenerator(42L)
      )

    val lifecycle2 =
      OrderLifecycleGenerator.generate(
        orderDate,
        "DELIVERED",
        new RandomGenerator(42L)
      )

    assert(
      lifecycle1 == lifecycle2
    )
  }

  test("order status should remain the supplied status") {

    OrderLifecycle.statuses.foreach { orderStatus =>

      val lifecycle =
        OrderLifecycleGenerator.generate(
          orderDate,
          orderStatus,
          new RandomGenerator(42L)
        )

      assert(
        lifecycle.orderStatus == orderStatus
      )
    }
  }

  test("unsupported order status should be rejected") {

    assertThrows[IllegalArgumentException] {

      OrderLifecycleGenerator.generate(
        orderDate,
        "INVALID_STATUS",
        new RandomGenerator(42L)
      )
    }
  }

  test("empty order status should be rejected") {

    assertThrows[IllegalArgumentException] {

      OrderLifecycleGenerator.generate(
        orderDate,
        "",
        new RandomGenerator(42L)
      )
    }
  }
}