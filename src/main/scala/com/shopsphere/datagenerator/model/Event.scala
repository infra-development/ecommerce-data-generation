package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Event(
                  id: String,
                  sessionId: String,
                  customerId: String,
                  eventType: String,
                  eventTimestamp: LocalDateTime,
                  productId: Option[String]
                )