package com.shopsphere.datagenerator.config

case class CardinalityConfig(
                              addressesPerCustomer: Double,
                              sessionsPerCustomer: Double,
                              eventsPerSession: Double,
                              itemsPerOrder: Double,
                              paymentsPerOrder: Double,
                              shipmentsPerOrder: Double,
                              returnsPerOrder: Double
                            )