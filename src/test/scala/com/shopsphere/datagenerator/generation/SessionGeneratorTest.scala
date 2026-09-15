package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.distribution.{DistributionEngine, WeightedDistribution}
import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class SessionGeneratorTest extends AnyFunSuite {

  private val distributionEngine =
    new DistributionEngine(
      Map(
        "device_type" ->
          new WeightedDistribution(
            Seq(
              "MOBILE" -> 0.70,
              "DESKTOP" -> 0.25,
              "TABLET" -> 0.05
            )
          ),
        "channel" ->
          new WeightedDistribution(
            Seq(
              "ORGANIC" -> 0.35,
              "PAID_SEARCH" -> 0.20,
              "SOCIAL" -> 0.15,
              "EMAIL" -> 0.10,
              "DIRECT" -> 0.15,
              "REFERRAL" -> 0.05
            )
          )
      )
    )

  test("should generate session with supplied IDs") {

    val session =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      session.id == "SESSION_000001"
    )

    assert(
      session.customerId == "CUSTOMER_000001"
    )
  }

  test("session end should not be before session start") {

    val session =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      !session.sessionEnd.isBefore(
        session.sessionStart
      )
    )
  }

  test("session duration should be between one and 120 minutes") {

    val session =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val duration =
      java.time.Duration
        .between(
          session.sessionStart,
          session.sessionEnd
        )
        .toMinutes

    assert(
      duration >= 1
    )

    assert(
      duration <= 120
    )
  }

  test("device type should be valid") {

    val session =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      Set(
        "MOBILE",
        "DESKTOP",
        "TABLET"
      ).contains(
        session.deviceType
      )
    )
  }

  test("channel should be valid") {

    val session =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      Set(
        "ORGANIC",
        "PAID_SEARCH",
        "SOCIAL",
        "EMAIL",
        "DIRECT",
        "REFERRAL"
      ).contains(
        session.channel
      )
    )
  }

  test("same seed should produce same session") {

    val session1 =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val session2 =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    assert(
      session1 == session2
    )
  }

  test("different seeds should produce different sessions") {

    val session1 =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )

    val session2 =
      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(43L),
        distributionEngine = distributionEngine
      )

    assert(
      session1 != session2
    )
  }

  test("should use the configured device type distribution") {

    val deviceTypes =
      (1 to 1000).map { seed =>

        SessionGenerator
          .generate(
            sessionId = "SESSION_000001",
            customerId = "CUSTOMER_000001",
            random = new RandomGenerator(seed.toLong),
            distributionEngine = distributionEngine
          )
          .deviceType
      }

    val mobileCount =
      deviceTypes.count(
        _ == "MOBILE"
      )

    val mobileRatio =
      mobileCount.toDouble /
        deviceTypes.size

    assert(
      mobileRatio > 0.60 &&
        mobileRatio < 0.80
    )
  }

  test("should use the configured channel distribution") {

    val channels =
      (1 to 1000).map { seed =>

        SessionGenerator
          .generate(
            sessionId = "SESSION_000001",
            customerId = "CUSTOMER_000001",
            random = new RandomGenerator(seed.toLong),
            distributionEngine = distributionEngine
          )
          .channel
      }

    val organicCount =
      channels.count(
        _ == "ORGANIC"
      )

    val organicRatio =
      organicCount.toDouble /
        channels.size

    assert(
      organicRatio > 0.25 &&
        organicRatio < 0.45
    )
  }

  test("empty session ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      SessionGenerator.generate(
        sessionId = "",
        customerId = "CUSTOMER_000001",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )
    }
  }

  test("empty customer ID should be rejected") {

    assertThrows[IllegalArgumentException] {

      SessionGenerator.generate(
        sessionId = "SESSION_000001",
        customerId = "",
        random = new RandomGenerator(42L),
        distributionEngine = distributionEngine
      )
    }
  }
}