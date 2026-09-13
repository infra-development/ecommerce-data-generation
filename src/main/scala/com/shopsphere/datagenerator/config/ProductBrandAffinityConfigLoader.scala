package com.shopsphere.datagenerator.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object ProductBrandAffinityConfigLoader {

  def load(config: Config): ProductBrandAffinityConfig = {

    if (!config.hasPath("product-brand-affinity")) {
      throw new IllegalArgumentException(
        "Missing required configuration: product-brand-affinity"
      )
    }

    val affinityConfig =
      config.getConfig("product-brand-affinity")

    val productTypes =
      affinityConfig
        .root()
        .keySet()
        .asScala
        .map { productTypeId =>

          val productTypeConfig =
            affinityConfig.getConfig(productTypeId)

          val brands =
            productTypeConfig
              .entrySet()
              .asScala
              .map { entry =>
                entry.getKey ->
                  productTypeConfig.getDouble(entry.getKey)
              }
              .toMap

          if (brands.isEmpty) {
            throw new IllegalArgumentException(
              s"Brand affinity must not be empty for product type: $productTypeId"
            )
          }

          brands.foreach {
            case (brandId, weight) =>
              if (weight < 0.0) {
                throw new IllegalArgumentException(
                  s"Brand affinity contains negative weight for product type '$productTypeId', brand '$brandId': $weight"
                )
              }
          }

          if (brands.values.sum <= 0.0) {
            throw new IllegalArgumentException(
              s"Brand affinity must have a positive total weight for product type: $productTypeId"
            )
          }

          productTypeId -> brands
        }
        .toMap

    if (productTypes.isEmpty) {
      throw new IllegalArgumentException(
        "Product brand affinity configuration must not be empty."
      )
    }

    ProductBrandAffinityConfig(
      productTypes = productTypes
    )
  }
}