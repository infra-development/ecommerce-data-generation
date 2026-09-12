package com.shopsphere.datagenerator.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object DistributionConfigLoader {

  def load(config: Config): DistributionConfig = {

    if (!config.hasPath("distributions")) {
      throw new IllegalArgumentException(
        "Missing required configuration: distributions"
      )
    }

    val distributionsConfig =
      config.getConfig("distributions")

    val distributions =
      distributionsConfig
        .root()
        .keySet()
        .asScala
        .map { distributionName =>

          val distribution =
            distributionsConfig.getConfig(
              distributionName
            )

          if (!distribution.hasPath("type")) {
            throw new IllegalArgumentException(
              s"Missing distribution type: $distributionName"
            )
          }

          if (!distribution.hasPath("values")) {
            throw new IllegalArgumentException(
              s"Missing distribution values: $distributionName"
            )
          }

          val distributionType =
            distribution.getString("type")

          val valuesConfig =
            distribution.getConfig("values")

          val values =
            valuesConfig
              .entrySet()
              .asScala
              .map { entry =>
                entry.getKey ->
                  valuesConfig.getDouble(entry.getKey)
              }
              .toMap

          if (values.isEmpty) {
            throw new IllegalArgumentException(
              s"Distribution values must not be empty: $distributionName"
            )
          }

          values.foreach {
            case (value, weight) =>

              if (weight < 0.0) {
                throw new IllegalArgumentException(
                  s"Distribution '$distributionName' contains negative weight for '$value': $weight"
                )
              }
          }

          if (values.values.sum <= 0.0) {
            throw new IllegalArgumentException(
              s"Distribution '$distributionName' must have a positive total weight."
            )
          }

          distributionName ->
            DistributionDefinition(
              distributionType = distributionType,
              values = values
            )
        }
        .toMap

    DistributionConfig(
      distributions = distributions
    )
  }
}