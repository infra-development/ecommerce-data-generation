package com.shopsphere.datagenerator.config

case class ProductPriceDefinition(
                                   min: Double,
                                   max: Double,
                                   mode: Double
                                 )

case class ProductPricingConfig(
                                 productTypes: Map[String, ProductPriceDefinition]
                               )