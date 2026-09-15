package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.model.{OrderItem, Product}

object OrderItemGenerator {

  def generate(
                orderItemId: String,
                orderId: String,
                product: Product,
                random: RandomGenerator
              ): OrderItem = {

    if (orderItemId.isEmpty) {
      throw new IllegalArgumentException(
        "Order item ID must not be empty."
      )
    }

    if (orderId.isEmpty) {
      throw new IllegalArgumentException(
        "Order ID must not be empty."
      )
    }

    val quantity =
      random.nextInt(1, 5)

    val unitPrice =
      product.price

    val lineAmount =
      (unitPrice * quantity)
        .setScale(
          2,
          BigDecimal.RoundingMode.HALF_UP
        )

    OrderItem(
      id = orderItemId,
      orderId = orderId,
      productId = product.id,
      quantity = quantity,
      unitPrice = unitPrice,
      lineAmount = lineAmount
    )
  }
}