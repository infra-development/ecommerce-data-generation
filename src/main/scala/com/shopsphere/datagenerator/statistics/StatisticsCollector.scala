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
      safeAverage(
        data.orderItems.size.toDouble,
        data.orders.size
      )

    val averageSessionsPerCustomer =
      safeAverage(
        data.sessions.size.toDouble,
        data.customers.size
      )

    val averageEventsPerSession =
      safeAverage(
        data.events.size.toDouble,
        data.sessions.size
      )

    val averageAddressesPerCustomer =
      safeAverage(
        data.addresses.size.toDouble,
        data.customers.size
      )

    val averageReturnsPerOrder =
      safeAverage(
        data.returns.size.toDouble,
        data.orders.size
      )

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
          averageReturnsPerOrder
      )

    val distributions =
      Map(
        "order_status" ->
          countBy(
            data.orders.map(_.status)
          ),

        "payment_method" ->
          countBy(
            data.payments.map(_.paymentMethod)
          ),

        "payment_status" ->
          countBy(
            data.payments.map(_.paymentStatus)
          ),

        "shipment_status" ->
          countBy(
            data.shipments.map(_.shipmentStatus)
          ),

        "return_status" ->
          countBy(
            data.returns.map(_.returnStatus)
          ),

        "device_type" ->
          countBy(
            data.sessions.map(_.deviceType)
          ),

        "channel" ->
          countBy(
            data.sessions.map(_.channel)
          ),

        "event_type" ->
          countBy(
            data.events.map(_.eventType)
          )
      )

    val itemsPerOrder =
      data.orders.map { order =>
        data.orderItems.count(
          _.orderId == order.id
        ).toLong
      }

    val sessionsPerCustomer =
      data.customers.map { customer =>
        data.sessions.count(
          _.customerId == customer.id
        ).toLong
      }

    val eventsPerSession =
      data.sessions.map { session =>
        data.events.count(
          _.sessionId == session.id
        ).toLong
      }

    val addressesPerCustomer =
      data.customers.map { customer =>
        data.addresses.count(
          _.customerId == customer.id
        ).toLong
      }

    val returnsPerOrder =
      data.orders.map { order =>
        data.returns.count(
          _.orderId == order.id
        ).toLong
      }

    val cardinalityStatistics =
      Map(
        "items_per_order" ->
          calculateDistributionStatistics(
            itemsPerOrder
          ),

        "sessions_per_customer" ->
          calculateDistributionStatistics(
            sessionsPerCustomer
          ),

        "events_per_session" ->
          calculateDistributionStatistics(
            eventsPerSession
          ),

        "addresses_per_customer" ->
          calculateDistributionStatistics(
            addressesPerCustomer
          ),

        "returns_per_order" ->
          calculateDistributionStatistics(
            returnsPerOrder
          )
      )

    val orderValues =
      data.orders.map(_.totalAmount)

    val financial =
      if (orderValues.nonEmpty) {

        val sortedOrderValues =
          orderValues.sorted

        FinancialStatistics(
          totalOrderValue =
            orderValues.sum,

          averageOrderValue =
            orderValues.sum /
              orderValues.size,

          minimumOrderValue =
            sortedOrderValues.head,

          p25OrderValue =
            percentile(
              sortedOrderValues,
              0.25
            ),

          medianOrderValue =
            percentile(
              sortedOrderValues,
              0.50
            ),

          p75OrderValue =
            percentile(
              sortedOrderValues,
              0.75
            ),

          p95OrderValue =
            percentile(
              sortedOrderValues,
              0.95
            ),

          p99OrderValue =
            percentile(
              sortedOrderValues,
              0.99
            ),

          maximumOrderValue =
            sortedOrderValues.last
        )

      } else {

        FinancialStatistics(
          totalOrderValue = BigDecimal(0),
          averageOrderValue = BigDecimal(0),
          minimumOrderValue = BigDecimal(0),
          p25OrderValue = BigDecimal(0),
          medianOrderValue = BigDecimal(0),
          p75OrderValue = BigDecimal(0),
          p95OrderValue = BigDecimal(0),
          p99OrderValue = BigDecimal(0),
          maximumOrderValue = BigDecimal(0)
        )
      }

    DatasetStatistics(
      recordCounts = recordCounts,
      averages = averages,
      distributions = distributions,
      cardinalityStatistics = cardinalityStatistics,
      financial = financial
    )
  }

  private def calculateDistributionStatistics(
                                               values: Seq[Long]
                                             ): DistributionStatistics = {

    if (values.isEmpty) {
      DistributionStatistics(
        minimum = 0L,
        p25 = 0L,
        median = 0L,
        p75 = 0L,
        p95 = 0L,
        p99 = 0L,
        maximum = 0L
      )
    } else {

      val sorted =
        values.sorted

      DistributionStatistics(
        minimum = sorted.head,
        p25 = percentile(
          sorted,
          0.25
        ).toLong,
        median = percentile(
          sorted,
          0.50
        ).toLong,
        p75 = percentile(
          sorted,
          0.75
        ).toLong,
        p95 = percentile(
          sorted,
          0.95
        ).toLong,
        p99 = percentile(
          sorted,
          0.99
        ).toLong,
        maximum = sorted.last
      )
    }
  }

  private def percentile(
                          sortedValues: Seq[BigDecimal],
                          percentile: Double
                        ): BigDecimal = {

    require(
      sortedValues.nonEmpty,
      "Cannot calculate percentile for an empty sequence."
    )

    require(
      percentile >= 0.0 &&
        percentile <= 1.0,
      "Percentile must be between 0.0 and 1.0."
    )

    if (sortedValues.size == 1) {
      sortedValues.head
    } else {

      val position =
        percentile *
          (sortedValues.size - 1)

      val lowerIndex =
        math.floor(position).toInt

      val upperIndex =
        math.ceil(position).toInt

      if (lowerIndex == upperIndex) {
        sortedValues(lowerIndex)
      } else {

        val fraction =
          BigDecimal(
            position - lowerIndex
          )

        sortedValues(lowerIndex) +
          (
            sortedValues(upperIndex) -
              sortedValues(lowerIndex)
            ) * fraction
      }
    }
  }

  private def percentile(
                          sortedValues: Seq[Long],
                          percentile: Double
                        ): Long = {

    require(
      sortedValues.nonEmpty,
      "Cannot calculate percentile for an empty sequence."
    )

    require(
      percentile >= 0.0 &&
        percentile <= 1.0,
      "Percentile must be between 0.0 and 1.0."
    )

    if (sortedValues.size == 1) {
      sortedValues.head
    } else {

      val position =
        percentile *
          (sortedValues.size - 1)

      val lowerIndex =
        math.floor(position).toInt

      val upperIndex =
        math.ceil(position).toInt

      if (lowerIndex == upperIndex) {
        sortedValues(lowerIndex)
      } else {

        val fraction =
          position - lowerIndex

        math.round(
          sortedValues(lowerIndex) +
            (
              sortedValues(upperIndex) -
                sortedValues(lowerIndex)
              ) * fraction
        )
      }
    }
  }

  private def safeAverage(
                           numerator: Double,
                           denominator: Int
                         ): Double = {

    if (denominator > 0) {
      numerator / denominator.toDouble
    } else {
      0.0
    }
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