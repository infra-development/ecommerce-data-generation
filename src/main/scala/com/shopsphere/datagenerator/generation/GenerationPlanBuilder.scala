package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.GenerationConfig

object GenerationPlanBuilder {

  def build(config: GenerationConfig): GenerationPlan = {

    val customerCount =
      config.profileDefinition.customerCount

    val productCount =
      config.profileDefinition.productCount

    val orderCount =
      config.profileDefinition.orderCount

    val addressCount =
      calculateCount(
        customerCount,
        config.cardinality.addressesPerCustomer
      )

    val sessionCount =
      calculateCount(
        customerCount,
        config.cardinality.sessionsPerCustomer
      )

    val orderItemCount =
      calculateCount(
        orderCount,
        config.cardinality.itemsPerOrder
      )

    val paymentCount =
      calculateCount(
        orderCount,
        config.cardinality.paymentsPerOrder
      )

    val shipmentCount =
      calculateCount(
        orderCount,
        config.cardinality.shipmentsPerOrder
      )

    val returnCount =
      calculateCount(
        orderCount,
        config.cardinality.returnsPerOrder
      )

    val eventCount =
      calculateCount(
        sessionCount,
        config.cardinality.eventsPerSession
      )

    GenerationPlan(
      customerCount = customerCount,
      addressCount = addressCount,
      productCount = productCount,
      orderCount = orderCount,
      orderItemCount = orderItemCount,
      paymentCount = paymentCount,
      shipmentCount = shipmentCount,
      returnCount = returnCount,
      sessionCount = sessionCount,
      eventCount = eventCount
    )
  }

  private def calculateCount(
                              baseCount: Long,
                              averagePerBase: Double
                            ): Long = {
    Math.round(
      baseCount.toDouble * averagePerBase
    )
  }
}