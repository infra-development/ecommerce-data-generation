package com.shopsphere.datagenerator.config

case class DistributionDefinition(
                                   distributionType: String,
                                   values: Map[String, Double]
                                 )

case class DistributionConfig(
                               distributions: Map[String, DistributionDefinition]
                             )