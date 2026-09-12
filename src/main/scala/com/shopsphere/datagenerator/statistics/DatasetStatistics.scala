package com.shopsphere.datagenerator.statistics

case class DatasetStatistics(
                              recordCounts: Map[String, Long],
                              averages: Map[String, Double],
                              distributions: Map[String, Map[String, Long]],
                              financial: FinancialStatistics
                            )

case class FinancialStatistics(
                                totalOrderValue: BigDecimal,
                                averageOrderValue: BigDecimal,
                                minimumOrderValue: BigDecimal,
                                maximumOrderValue: BigDecimal
                              )