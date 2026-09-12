package com.shopsphere.datagenerator.config

object CardinalityDefaults {

  val low: CardinalityConfig =
    CardinalityConfig(
      addressesPerCustomer = 1.0,
      sessionsPerCustomer = 2.0,
      eventsPerSession = 5.0,
      itemsPerOrder = 1.8,
      paymentsPerOrder = 1.0,
      shipmentsPerOrder = 1.0,
      returnsPerOrder = 0.04
    )

  val medium: CardinalityConfig =
    CardinalityConfig(
      addressesPerCustomer = 1.2,
      sessionsPerCustomer = 3.0,
      eventsPerSession = 8.0,
      itemsPerOrder = 2.4,
      paymentsPerOrder = 1.0,
      shipmentsPerOrder = 1.0,
      returnsPerOrder = 0.08
    )

  val high: CardinalityConfig =
    CardinalityConfig(
      addressesPerCustomer = 1.5,
      sessionsPerCustomer = 5.0,
      eventsPerSession = 12.0,
      itemsPerOrder = 3.2,
      paymentsPerOrder = 1.0,
      shipmentsPerOrder = 1.0,
      returnsPerOrder = 0.15
    )

  def forProfile(
                  profile: CardinalityProfile
                ): CardinalityConfig = {
    profile match {
      case CardinalityProfile.Low =>
        low
      case CardinalityProfile.Medium =>
        medium
      case CardinalityProfile.High =>
        high
    }
  }
}