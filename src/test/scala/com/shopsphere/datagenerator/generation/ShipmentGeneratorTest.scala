package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class ShipmentGeneratorTest extends AnyFunSuite {

  test("should generate shipment for the supplied order") {

    val shipment =
      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(shipment.id == "ORDER_000001_SHIPMENT")
    assert(shipment.orderId == "ORDER_000001")
  }

  test("should generate a valid shipment status") {

    val validStatuses =
      Set(
        "PROCESSING",
        "SHIPPED",
        "IN_TRANSIT",
        "DELIVERED"
      )

    val shipment =
      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(
      validStatuses.contains(
        shipment.shipmentStatus
      )
    )
  }

  test("should have no shipment dates when shipment is processing") {

    val shipment =
      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    if (shipment.shipmentStatus == "PROCESSING") {
      assert(shipment.shippedDate.isEmpty)
      assert(shipment.deliveredDate.isEmpty)
    }
  }

  test("should have a shipped date when shipment has shipped") {

    (1 to 100).foreach { seed =>

      val shipment =
        ShipmentGenerator.generate(
          "ORDER_000001_SHIPMENT",
          "ORDER_000001",
          new RandomGenerator(seed.toLong)
        )

      if (
        shipment.shipmentStatus == "SHIPPED" ||
          shipment.shipmentStatus == "IN_TRANSIT"
      ) {
        assert(shipment.shippedDate.nonEmpty)
        assert(shipment.deliveredDate.isEmpty)
      }
    }
  }

  test("should have shipped and delivered dates when shipment is delivered") {

    (1 to 100).foreach { seed =>

      val shipment =
        ShipmentGenerator.generate(
          "ORDER_000001_SHIPMENT",
          "ORDER_000001",
          new RandomGenerator(seed.toLong)
        )

      if (shipment.shipmentStatus == "DELIVERED") {

        assert(shipment.shippedDate.nonEmpty)
        assert(shipment.deliveredDate.nonEmpty)

        assert(
          shipment.deliveredDate.get.isAfter(
            shipment.shippedDate.get
          )
        )
      }
    }
  }

  test("should not have delivered date unless shipment is delivered") {

    (1 to 100).foreach { seed =>

      val shipment =
        ShipmentGenerator.generate(
          "ORDER_000001_SHIPMENT",
          "ORDER_000001",
          new RandomGenerator(seed.toLong)
        )

      if (shipment.shipmentStatus != "DELIVERED") {
        assert(shipment.deliveredDate.isEmpty)
      }
    }
  }

  test("should produce the same shipment for the same seed") {

    val shipment1 =
      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    val shipment2 =
      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(shipment1 == shipment2)
  }

  test("empty shipment ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      ShipmentGenerator.generate(
        "",
        "ORDER_000001",
        new RandomGenerator(42L)
      )
    }
  }

  test("empty order ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      ShipmentGenerator.generate(
        "ORDER_000001_SHIPMENT",
        "",
        new RandomGenerator(42L)
      )
    }
  }
}