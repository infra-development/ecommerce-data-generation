package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Shipment(
                     id: String,
                     orderId: String,
                     shipmentStatus: String,
                     shippedDate: Option[LocalDateTime],
                     deliveredDate: Option[LocalDateTime]
                   )