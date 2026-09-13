package com.shopsphere.datagenerator.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object ProductPricingConfigLoader {

  def load(config: Config): ProductPricingConfig = {

    if (!config.hasPath("product-pricing")) {
      throw new IllegalArgumentException(
        "Missing required configuration: product-pricing"
      )
    }

    val pricingConfig =
      config.getConfig("product-pricing")

    val productTypes =
      pricingConfig
        .root()
        .keySet()
        .asScala
        .map { productTypeId =>

          val productTypeConfig =
            pricingConfig.getConfig(productTypeId)

          requirePath(
            productTypeConfig,
            productTypeId,
            "min"
          )

          requirePath(
            productTypeConfig,
            productTypeId,
            "max"
          )

          requirePath(
            productTypeConfig,
            productTypeId,
            "mode"
          )

          val min =
            productTypeConfig.getDouble("min")

          val max =
            productTypeConfig.getDouble("max")

          val mode =
            productTypeConfig.getDouble("mode")

          validatePriceDefinition(
            productTypeId = productTypeId,
            min = min,
            max = max,
            mode = mode
          )

          productTypeId ->
            ProductPriceDefinition(
              min = min,
              max = max,
              mode = mode
            )
        }
        .toMap

    if (productTypes.isEmpty) {
      throw new IllegalArgumentException(
        "Product pricing configuration must not be empty."
      )
    }

    ProductPricingConfig(
      productTypes = productTypes
    )
  }

  private def requirePath(
                           config: Config,
                           productTypeId: String,
                           path: String
                         ): Unit = {

    if (!config.hasPath(path)) {
      throw new IllegalArgumentException(
        s"Missing product pricing '$path' for product type: $productTypeId"
      )
    }
  }

  private def validatePriceDefinition(
                                       productTypeId: String,
                                       min: Double,
                                       max: Double,
                                       mode: Double
                                     ): Unit = {

    if (min.isNaN || min.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must be finite for product type: $productTypeId"
      )
    }

    if (max.isNaN || max.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing maximum must be finite for product type: $productTypeId"
      )
    }

    if (mode.isNaN || mode.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing mode must be finite for product type: $productTypeId"
      )
    }

    if (min < 0.0) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must not be negative for product type: $productTypeId"
      )
    }

    if (min > max) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must not be greater than maximum for product type: $productTypeId"
      )
    }

    if (mode < min || mode > max) {
      throw new IllegalArgumentException(
        s"Product pricing mode must be between minimum and maximum for product type: $productTypeId"
      )
    }
  }
}