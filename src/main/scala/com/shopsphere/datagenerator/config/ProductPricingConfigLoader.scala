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

    val categories =
      pricingConfig
        .root()
        .keySet()
        .asScala
        .map { categoryId =>

          val categoryConfig =
            pricingConfig.getConfig(categoryId)

          requirePath(
            categoryConfig,
            categoryId,
            "min"
          )

          requirePath(
            categoryConfig,
            categoryId,
            "max"
          )

          requirePath(
            categoryConfig,
            categoryId,
            "mode"
          )

          val min =
            categoryConfig.getDouble("min")

          val max =
            categoryConfig.getDouble("max")

          val mode =
            categoryConfig.getDouble("mode")

          validatePriceDefinition(
            categoryId = categoryId,
            min = min,
            max = max,
            mode = mode
          )

          categoryId ->
            ProductPriceDefinition(
              min = min,
              max = max,
              mode = mode
            )
        }
        .toMap

    if (categories.isEmpty) {
      throw new IllegalArgumentException(
        "Product pricing configuration must not be empty."
      )
    }

    ProductPricingConfig(
      categories = categories
    )
  }

  private def requirePath(
                           config: Config,
                           categoryId: String,
                           path: String
                         ): Unit = {

    if (!config.hasPath(path)) {
      throw new IllegalArgumentException(
        s"Missing product pricing '$path' for category: $categoryId"
      )
    }
  }

  private def validatePriceDefinition(
                                       categoryId: String,
                                       min: Double,
                                       max: Double,
                                       mode: Double
                                     ): Unit = {

    if (min.isNaN || min.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must be finite for category: $categoryId"
      )
    }

    if (max.isNaN || max.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing maximum must be finite for category: $categoryId"
      )
    }

    if (mode.isNaN || mode.isInfinity) {
      throw new IllegalArgumentException(
        s"Product pricing mode must be finite for category: $categoryId"
      )
    }

    if (min < 0.0) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must not be negative for category: $categoryId"
      )
    }

    if (min > max) {
      throw new IllegalArgumentException(
        s"Product pricing minimum must not be greater than maximum for category: $categoryId"
      )
    }

    if (mode < min || mode > max) {
      throw new IllegalArgumentException(
        s"Product pricing mode must be between minimum and maximum for category: $categoryId"
      )
    }
  }
}