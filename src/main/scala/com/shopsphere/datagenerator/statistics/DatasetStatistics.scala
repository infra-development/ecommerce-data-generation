package com.shopsphere.datagenerator.statistics

case class DatasetStatistics(
                              recordCounts: Map[String, Long],
                              averages: Map[String, Double],
                              distributions: Map[String, Map[String, Long]],
                              cardinalityStatistics: Map[String, DistributionStatistics],
                              financial: FinancialStatistics
                            )

case class DistributionStatistics(
                                   minimum: Long,
                                   p25: Long,
                                   median: Long,
                                   p75: Long,
                                   p95: Long,
                                   p99: Long,
                                   maximum: Long
                                 )

case class FinancialStatistics(
                                totalOrderValue: BigDecimal,
                                averageOrderValue: BigDecimal,
                                minimumOrderValue: BigDecimal,
                                p25OrderValue: BigDecimal,
                                medianOrderValue: BigDecimal,
                                p75OrderValue: BigDecimal,
                                p95OrderValue: BigDecimal,
                                p99OrderValue: BigDecimal,
                                maximumOrderValue: BigDecimal
                              )