package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator
}
import com.shopsphere.datagenerator.model.Session

import java.time.LocalDateTime

object SessionGenerator {

  def generate(
                sessionId: String,
                customerId: String,
                random: RandomGenerator,
                distributionEngine: DistributionEngine
              ): Session = {

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

    val sessionStart =
      LocalDateTime
        .of(2026, 1, 1, 0, 0)
        .plusSeconds(
          random
            .derive("session-start")
            .nextLong(
              0L,
              31_535_999L
            )
        )

    val durationMinutes =
      random
        .derive("session-duration")
        .nextLong(
          1L,
          120L
        )

    val sessionEnd =
      sessionStart.plusMinutes(
        durationMinutes
      )

    val deviceType =
      distributionEngine.sample[String](
        "device_type",
        random.derive("device-type")
      )

    val channel =
      distributionEngine.sample[String](
        "channel",
        random.derive("channel")
      )

    Session(
      id = sessionId,
      customerId = customerId,
      sessionStart = sessionStart,
      sessionEnd = sessionEnd,
      deviceType = deviceType,
      channel = channel
    )
  }
}