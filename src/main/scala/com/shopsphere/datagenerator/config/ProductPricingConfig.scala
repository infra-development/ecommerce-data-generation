package com.shopsphere.datagenerator.config

case class ProductPriceDefinition(
                                   min: Double,
                                   max: Double,
                                   mode: Double
                                 )

case class ProductPricingConfig(
                                 categories: Map[String, ProductPriceDefinition]
                               )