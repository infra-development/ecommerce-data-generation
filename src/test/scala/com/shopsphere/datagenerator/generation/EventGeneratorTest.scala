package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDateTime

class EventGeneratorTest extends AnyFunSuite {

  private val sessionStart =
    LocalDateTime.of(
      2026,
      1,
      10,
      10,
      0
    )

  private val sessionEnd =
    LocalDateTime.of(
      2026,
      1,
      10,
      11,
      0
    )

  test("should generate event with supplied relationships") {

    val event =
      EventGenerator.generate(
        eventId = "EVENT_000001",
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        eventType = "PRODUCT_VIEW",
        productId = Some("PRODUCT_000001"),
        random = new RandomGenerator(42)
      )

    assert(event.id == "EVENT_000001")
    assert(event.sessionId == "SESSION_000001")
    assert(event.customerId == "CUSTOMER_000001")
    assert(event.productId.contains("PRODUCT_000001"))
  }

  test("event timestamp should be inside session") {

    val event =
      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "PAGE_VIEW",
        None,
        new RandomGenerator(42)
      )

    assert(
      !event.eventTimestamp.isBefore(sessionStart)
    )

    assert(
      !event.eventTimestamp.isAfter(sessionEnd)
    )
  }

  test("event type should be valid") {

    val event =
      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "ADD_TO_CART",
        Some("PRODUCT_000001"),
        new RandomGenerator(42)
      )

    assert(
      EventGenerator.eventTypes.contains(
        event.eventType
      )
    )
  }

  test("event without product should be supported") {

    val event =
      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "SESSION_START",
        None,
        new RandomGenerator(42)
      )

    assert(event.productId.isEmpty)
  }

  test("invalid event type should be rejected") {

    assertThrows[IllegalArgumentException] {

      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "INVALID_EVENT",
        None,
        new RandomGenerator(42)
      )
    }
  }

  test("invalid session range should be rejected") {

    assertThrows[IllegalArgumentException] {

      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionEnd,
        sessionStart,
        "PAGE_VIEW",
        None,
        new RandomGenerator(42)
      )
    }
  }

  test("same seed should produce same event") {

    val event1 =
      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "PRODUCT_VIEW",
        Some("PRODUCT_000001"),
        new RandomGenerator(42)
      )

    val event2 =
      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "PRODUCT_VIEW",
        Some("PRODUCT_000001"),
        new RandomGenerator(42)
      )

    assert(event1 == event2)
  }

  test("empty event ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      EventGenerator.generate(
        "",
        "SESSION_000001",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "PAGE_VIEW",
        None,
        new RandomGenerator(42)
      )
    }
  }

  test("empty session ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      EventGenerator.generate(
        "EVENT_000001",
        "",
        "CUSTOMER_000001",
        sessionStart,
        sessionEnd,
        "PAGE_VIEW",
        None,
        new RandomGenerator(42)
      )
    }
  }

  test("empty customer ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      EventGenerator.generate(
        "EVENT_000001",
        "SESSION_000001",
        "",
        sessionStart,
        sessionEnd,
        "PAGE_VIEW",
        None,
        new RandomGenerator(42)
      )
    }
  }
}