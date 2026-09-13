package com.shopsphere.datagenerator.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object ProductDistributionConfigLoader {

  def load(config: Config): ProductDistributionConfig = {

    if (!config.hasPath("product-distribution")) {
      throw new IllegalArgumentException(
        "Missing required configuration: product-distribution"
      )
    }

    val productDistributionConfig =
      config.getConfig("product-distribution")

    val categories =
      loadWeights(
        config = productDistributionConfig,
        path = "categories",
        context = "product category distribution"
      )

    if (!productDistributionConfig.hasPath("product-types")) {
      throw new IllegalArgumentException(
        "Missing required configuration: product-types"
      )
    }

    val productTypesConfig =
      productDistributionConfig.getConfig("product-types")

    val productTypes =
      productTypesConfig
        .root()
        .keySet()
        .asScala
        .map { categoryId =>

          val categoryConfig =
            productTypesConfig.getConfig(categoryId)

          val weights =
            categoryConfig
              .entrySet()
              .asScala
              .map { entry =>
                entry.getKey ->
                  categoryConfig.getDouble(entry.getKey)
              }
              .toMap

          validateWeights(
            weights = weights,
            context =
              s"product type distribution for category: $categoryId"
          )

          categoryId -> weights
        }
        .toMap

    if (productTypes.isEmpty) {
      throw new IllegalArgumentException(
        "Product type distributions must not be empty."
      )
    }

    ProductDistributionConfig(
      categories = categories,
      productTypes = productTypes
    )
  }

  private def loadWeights(
                           config: Config,
                           path: String,
                           context: String
                         ): Map[String, Double] = {

    if (!config.hasPath(path)) {
      throw new IllegalArgumentException(
        s"Missing required configuration: $path"
      )
    }

    val valuesConfig =
      config.getConfig(path)

    val values =
      valuesConfig
        .entrySet()
        .asScala
        .map { entry =>
          entry.getKey ->
            valuesConfig.getDouble(entry.getKey)
        }
        .toMap

    validateWeights(
      weights = values,
      context = context
    )

    values
  }

  private def validateWeights(
                               weights: Map[String, Double],
                               context: String
                             ): Unit = {

    if (weights.isEmpty) {
      throw new IllegalArgumentException(
        s"Distribution values must not be empty: $context"
      )
    }

    weights.foreach {
      case (value, weight) =>

        if (weight.isNaN || weight.isInfinity) {
          throw new IllegalArgumentException(
            s"Distribution '$context' contains non-finite weight for '$value': $weight"
          )
        }

        if (weight < 0.0) {
          throw new IllegalArgumentException(
            s"Distribution '$context' contains negative weight for '$value': $weight"
          )
        }
    }

    if (weights.values.sum <= 0.0) {
      throw new IllegalArgumentException(
        s"Distribution '$context' must have a positive total weight."
      )
    }
  }
}