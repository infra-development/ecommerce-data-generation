package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Event

import java.time.LocalDateTime

object EventGenerator {

  val eventTypes =
    Seq(
      "SESSION_START",
      "PAGE_VIEW",
      "PRODUCT_VIEW",
      "ADD_TO_CART",
      "CHECKOUT",
      "PURCHASE",
      "SESSION_END"
    )

  def generate(
                eventId: String,
                sessionId: String,
                customerId: String,
                sessionStart: LocalDateTime,
                sessionEnd: LocalDateTime,
                eventType: String,
                productId: Option[String],
                random: RandomGenerator
              ): Event = {

    if (eventId.isEmpty) {
      throw new IllegalArgumentException(
        "Event ID must not be empty."
      )
    }

    if (sessionId.isEmpty) {
      throw new IllegalArgumentException(
        "Session ID must not be empty."
      )
    }

    if (customerId.isEmpty) {
      throw new IllegalArgumentException(
        "Customer ID must not be empty."
      )
    }

    if (sessionStart.isAfter(sessionEnd)) {
      throw new IllegalArgumentException(
        "Session start must not be after session end."
      )
    }

    if (!eventTypes.contains(eventType)) {
      throw new IllegalArgumentException(
        s"Unsupported event type: $eventType"
      )
    }

    val startSecond =
      sessionStart.toEpochSecond(
        java.time.ZoneOffset.UTC
      )

    val endSecond =
      sessionEnd.toEpochSecond(
        java.time.ZoneOffset.UTC
      )

    val eventTimestamp =
      if (startSecond == endSecond) {
        sessionStart
      } else {
        LocalDateTime.ofEpochSecond(
          random.nextLong(
            startSecond,
            endSecond
          ),
          0,
          java.time.ZoneOffset.UTC
        )
      }

    Event(
      id = eventId,
      sessionId = sessionId,
      customerId = customerId,
      eventType = eventType,
      eventTimestamp = eventTimestamp,
      productId = productId
    )
  }
}