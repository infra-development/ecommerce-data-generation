package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Return

import java.time.LocalDateTime

object ReturnGenerator {

  private val returnReasons =
    Seq(
      "DAMAGED",
      "WRONG_ITEM",
      "NOT_AS_EXPECTED",
      "SIZE_ISSUE",
      "CHANGED_MIND"
    )

  private val returnStatuses =
    Seq(
      "REQUESTED",
      "APPROVED",
      "RECEIVED",
      "REFUNDED",
      "REJECTED"
    )

  def generate(
                returnId: String,
                orderId: String,
                random: RandomGenerator
              ): Return = {

    if (returnId.isEmpty) {
      throw new IllegalArgumentException(
        "Return ID must not be empty."
      )
    }

    if (orderId.isEmpty) {
      throw new IllegalArgumentException(
        "Order ID must not be empty."
      )
    }

    val returnReason =
      returnReasons(
        random.nextInt(returnReasons.size)
      )

    val returnStatus =
      returnStatuses(
        random.nextInt(returnStatuses.size)
      )

    val returnDate =
      LocalDateTime.of(
        2026,
        1,
        1,
        0,
        0
      ).plusSeconds(
        random.nextLong(0L, 31_535_999L)
      )

    Return(
      id = returnId,
      orderId = orderId,
      returnReason = returnReason,
      returnStatus = returnStatus,
      returnDate = returnDate
    )
  }
}