package com.shopsphere.datagenerator.relationship

import java.time.LocalDateTime

case class OrderLifecycle(
                           orderStatus: String,
                           paymentStatus: String,
                           paymentDate: LocalDateTime,
                           shipmentStatus: String,
                           shippedDate: Option[LocalDateTime],
                           deliveredDate: Option[LocalDateTime],
                           returnStatus: Option[String],
                           returnDate: Option[LocalDateTime]
                         )

object OrderLifecycle {

  val statuses =
    Seq(
      "PLACED",
      "CONFIRMED",
      "SHIPPED",
      "DELIVERED",
      "CANCELLED"
    )

  val shipmentStatuses =
    Seq(
      "PROCESSING",
      "SHIPPED",
      "IN_TRANSIT",
      "DELIVERED"
    )

  val paymentStatuses =
    Seq(
      "SUCCESS",
      "FAILED",
      "PENDING"
    )

  val returnStatuses =
    Seq(
      "REQUESTED",
      "APPROVED",
      "RECEIVED",
      "REFUNDED",
      "REJECTED"
    )
}