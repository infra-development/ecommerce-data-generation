package com.shopsphere.datagenerator.relationship

import com.shopsphere.datagenerator.distribution.RandomGenerator

import java.time.LocalDateTime

object OrderLifecycleGenerator {

  def generate(
                orderDate: LocalDateTime,
                orderStatus: String,
                random: RandomGenerator
              ): OrderLifecycle = {

    require(
      orderDate != null,
      "Order date must not be null."
    )

    require(
      orderStatus.nonEmpty,
      "Order status must not be empty."
    )

    require(
      OrderLifecycle.statuses.contains(orderStatus),
      s"Unsupported order status: $orderStatus"
    )

    /*
     * Payment occurs after order creation.
     */
    val paymentDate =
      orderDate.plusHours(
        random.nextLong(0L, 48L)
      )

    /*
     * A cancelled order currently gets FAILED payment.
     *
     * This is intentionally conservative because our current
     * Order model stores only the final status and does not yet
     * model a cancellation timestamp/history.
     */
    val paymentStatus =
      if (orderStatus == "CANCELLED") {
        "FAILED"
      } else {
        "SUCCESS"
      }

    /*
     * Shipment lifecycle depends on the final order status.
     */
    val shipmentStatus =
      orderStatus match {

        case "PLACED" =>
          "PROCESSING"

        case "CONFIRMED" =>
          "PROCESSING"

        case "SHIPPED" =>
          "IN_TRANSIT"

        case "DELIVERED" =>
          "DELIVERED"

        case "CANCELLED" =>
          "PROCESSING"
      }

    val shippedDate =
      orderStatus match {

        case "SHIPPED" | "DELIVERED" =>
          Some(
            paymentDate.plusHours(
              random.nextLong(1L, 72L)
            )
          )

        case _ =>
          None
      }

    val deliveredDate =
      if (orderStatus == "DELIVERED") {
        Some(
          shippedDate.get.plusHours(
            random.nextLong(12L, 168L)
          )
        )
      } else {
        None
      }

    /*
     * Returns are only possible after delivery.
     */
    val shouldReturn =
      orderStatus == "DELIVERED" &&
        random
          .derive("return-decision")
          .nextDouble() < 0.08

    val returnStatus =
      if (shouldReturn) {
        Some(
          OrderLifecycle.returnStatuses(
            random
              .derive("return-status")
              .nextInt(
                OrderLifecycle.returnStatuses.size
              )
          )
        )
      } else {
        None
      }

    val returnDate =
      if (shouldReturn) {
        Some(
          deliveredDate.get.plusHours(
            random
              .derive("return-date")
              .nextLong(24L, 720L)
          )
        )
      } else {
        None
      }

    OrderLifecycle(
      orderStatus = orderStatus,
      paymentStatus = paymentStatus,
      paymentDate = paymentDate,
      shipmentStatus = shipmentStatus,
      shippedDate = shippedDate,
      deliveredDate = deliveredDate,
      returnStatus = returnStatus,
      returnDate = returnDate
    )
  }
}