package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.model.Shipment

import java.time.LocalDateTime

object ShipmentGenerator {

  private val statuses =
    Seq(
      "PROCESSING",
      "SHIPPED",
      "IN_TRANSIT",
      "DELIVERED"
    )

  def generate(
                shipmentId: String,
                orderId: String,
                random: RandomGenerator
              ): Shipment = {

    if (shipmentId.isEmpty) {
      throw new IllegalArgumentException(
        "Shipment ID must not be empty."
      )
    }

    if (orderId.isEmpty) {
      throw new IllegalArgumentException(
        "Order ID must not be empty."
      )
    }

    val status =
      statuses(
        random.nextInt(statuses.size)
      )

    val shippedDate =
      status match {

        case "SHIPPED" | "IN_TRANSIT" | "DELIVERED" =>
          Some(
            LocalDateTime
              .of(2026, 1, 1, 0, 0)
              .plusSeconds(
                random.nextLong(
                  0L,
                  31_535_999L
                )
              )
          )

        case _ =>
          None
      }

    val deliveredDate =
      if (status == "DELIVERED") {
        Some(
          shippedDate.get.plusHours(
            random.nextLong(12L, 168L)
          )
        )
      } else {
        None
      }

    Shipment(
      id = shipmentId,
      orderId = orderId,
      shipmentStatus = status,
      shippedDate = shippedDate,
      deliveredDate = deliveredDate
    )
  }
}