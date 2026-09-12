package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator
}
import com.shopsphere.datagenerator.model.Order

import java.time.LocalDateTime

object OrderGenerator {

  def generate(
                orderId: String,
                customerId: String,
                random: RandomGenerator,
                distributionEngine: DistributionEngine
              ): Order = {

    if (orderId.isEmpty) {
      throw new IllegalArgumentException(
        "Order ID must not be empty."
      )
    }

    if (customerId.isEmpty) {
      throw new IllegalArgumentException(
        "Customer ID must not be empty."
      )
    }

    val orderDate =
      LocalDateTime
        .of(
          2026,
          1,
          1,
          0,
          0
        )
        .plusSeconds(
          random.nextLong(
            0L,
            31_535_999L
          )
        )

    val status =
      distributionEngine.sample[String](
        "order_status",
        random.derive("order-status")
      )

    Order(
      id = orderId,
      customerId = customerId,
      orderDate = orderDate,
      status = status,
      totalAmount = BigDecimal(0)
    )
  }
}