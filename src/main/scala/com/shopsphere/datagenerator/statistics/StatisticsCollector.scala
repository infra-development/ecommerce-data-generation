package com.shopsphere.datagenerator.statistics

import com.shopsphere.datagenerator.generation.GeneratedData

object StatisticsCollector {

  def collect(
               data: GeneratedData
             ): DatasetStatistics = {

    val recordCounts =
      Map(
        "customers" -> data.customers.size.toLong,
        "addresses" -> data.addresses.size.toLong,
        "categories" -> data.categories.size.toLong,
        "brands" -> data.brands.size.toLong,
        "products" -> data.products.size.toLong,
        "orders" -> data.orders.size.toLong,
        "order_items" -> data.orderItems.size.toLong,
        "payments" -> data.payments.size.toLong,
        "shipments" -> data.shipments.size.toLong,
        "returns" -> data.returns.size.toLong,
        "sessions" -> data.sessions.size.toLong,
        "events" -> data.events.size.toLong
      )

    val averageItemsPerOrder =
      if (data.orders.nonEmpty) {
        data.orderItems.size.toDouble /
          data.orders.size.toDouble
      } else {
        0.0
      }

    val averageSessionsPerCustomer =
      if (data.customers.nonEmpty) {
        data.sessions.size.toDouble /
          data.customers.size.toDouble
      } else {
        0.0
      }

    val averageEventsPerSession =
      if (data.sessions.nonEmpty) {
        data.events.size.toDouble /
          data.sessions.size.toDouble
      } else {
        0.0
      }

    val averageAddressesPerCustomer =
      if (data.customers.nonEmpty) {
        data.addresses.size.toDouble /
          data.customers.size.toDouble
      } else {
        0.0
      }

    val averages =
      Map(
        "items_per_order" ->
          averageItemsPerOrder,

        "sessions_per_customer" ->
          averageSessionsPerCustomer,

        "events_per_session" ->
          averageEventsPerSession,

        "addresses_per_customer" ->
          averageAddressesPerCustomer,

        "returns_per_order" ->
          (
            if (data.orders.nonEmpty) {
              data.returns.size.toDouble /
                data.orders.size.toDouble
            } else {
              0.0
            }
            )
      )

    val orderStatusDistribution =
      countBy(
        data.orders.map(_.status)
      )

    val paymentMethodDistribution =
      countBy(
        data.payments.map(_.paymentMethod)
      )

    val paymentStatusDistribution =
      countBy(
        data.payments.map(_.paymentStatus)
      )

    val shipmentStatusDistribution =
      countBy(
        data.shipments.map(_.shipmentStatus)
      )

    val returnStatusDistribution =
      countBy(
        data.returns.map(_.returnStatus)
      )

    val deviceTypeDistribution =
      countBy(
        data.sessions.map(_.deviceType)
      )

    val channelDistribution =
      countBy(
        data.sessions.map(_.channel)
      )

    val eventTypeDistribution =
      countBy(
        data.events.map(_.eventType)
      )

    val distributions =
      Map(
        "order_status" ->
          orderStatusDistribution,

        "payment_method" ->
          paymentMethodDistribution,

        "payment_status" ->
          paymentStatusDistribution,

        "shipment_status" ->
          shipmentStatusDistribution,

        "return_status" ->
          returnStatusDistribution,

        "device_type" ->
          deviceTypeDistribution,

        "channel" ->
          channelDistribution,

        "event_type" ->
          eventTypeDistribution
      )

    val orderValues =
      data.orders.map(_.totalAmount)

    val financial =
      if (orderValues.nonEmpty) {

        FinancialStatistics(
          totalOrderValue =
            orderValues.sum,

          averageOrderValue =
            orderValues.sum /
              orderValues.size,

          minimumOrderValue =
            orderValues.min,

          maximumOrderValue =
            orderValues.max
        )

      } else {

        FinancialStatistics(
          totalOrderValue = BigDecimal(0),
          averageOrderValue = BigDecimal(0),
          minimumOrderValue = BigDecimal(0),
          maximumOrderValue = BigDecimal(0)
        )
      }

    DatasetStatistics(
      recordCounts = recordCounts,
      averages = averages,
      distributions = distributions,
      financial = financial
    )
  }

  private def countBy(
                       values: Seq[String]
                     ): Map[String, Long] = {

    values
      .groupBy(identity)
      .view
      .mapValues(_.size.toLong)
      .toMap
  }
}