package com.shopsphere.datagenerator.config

import java.time.LocalDate

case class CustomerAgeBand(
                            minAge: Int,
                            maxAge: Int,
                            weight: Double
                          )

case class CustomerGenerationConfig(
                                     asOfDate: LocalDate,
                                     registrationHistoryDays: Int,
                                     ageBands: Seq[CustomerAgeBand],
                                     gender: Map[String, Double],
                                     customerStatus: Map[String, Double],
                                     customerSegments: Map[String, Double],
                                     acquisitionChannels: Map[String, Double],
                                     acquisitionCampaigns: Map[String, Map[String, Double]],
                                     preferredDevices: Map[String, Double],
                                     preferredPaymentMethods: Map[String, Double]
                                   )