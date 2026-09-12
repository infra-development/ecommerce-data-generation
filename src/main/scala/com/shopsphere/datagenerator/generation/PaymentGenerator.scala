package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator
}
import com.shopsphere.datagenerator.model.Payment

import java.time.LocalDateTime

object PaymentGenerator {

  def generate(
                paymentId: String,
                orderId: String,
                amount: BigDecimal,
                random: RandomGenerator,
                distributionEngine: DistributionEngine
              ): Payment = {

    if (paymentId.isEmpty) {
      throw new IllegalArgumentException(
        "Payment ID must not be empty."
      )
    }

    if (orderId.isEmpty) {
      throw new IllegalArgumentException(
        "Order ID must not be empty."
      )
    }

    if (amount < BigDecimal(0)) {
      throw new IllegalArgumentException(
        "Payment amount must not be negative."
      )
    }

    val paymentMethod =
      distributionEngine.sample[String](
        "payment_method",
        random.derive("payment-method")
      )

    val paymentStatuses =
      Seq(
        "SUCCESS",
        "FAILED",
        "PENDING"
      )

    val paymentStatus =
      paymentStatuses(
        random
          .derive("payment-status")
          .nextInt(paymentStatuses.size)
      )

    val paymentDate =
      LocalDateTime
        .of(2026, 1, 1, 0, 0)
        .plusSeconds(
          random
            .derive("payment-date")
            .nextLong(
              0L,
              31_535_999L
            )
        )

    Payment(
      id = paymentId,
      orderId = orderId,
      paymentMethod = paymentMethod,
      paymentStatus = paymentStatus,
      amount = amount,
      paymentDate = paymentDate
    )
  }
}