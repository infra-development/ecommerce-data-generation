package com.shopsphere.datagenerator.config

import com.typesafe.config.{Config, ConfigFactory}

import java.time.LocalDate
import scala.jdk.CollectionConverters._

object ConfigLoader {

  def load(): GenerationConfig = {
    val config = ConfigFactory.load()

    val distributionConfig =
      DistributionConfigLoader.load(config)

    val productDistributionConfig =
      ProductDistributionConfigLoader.load(config)

    val productPricingConfig =
      ProductPricingConfigLoader.load(config)

    val productBrandAffinityConfig =
      ProductBrandAffinityConfigLoader.load(config)

    val customerGenerationConfig =
      loadCustomerGenerationConfig(config)

    val profileName =
      config.getString("generator.profile")

    val profile =
      GenerationProfile.fromString(profileName) match {
        case Right(value) => value
        case Left(error) =>
          throw new IllegalArgumentException(error)
      }

    val scenarioName =
      config.getString("generator.scenario")

    val scenario =
      GenerationScenario.fromString(scenarioName) match {
        case Right(value) => value
        case Left(error) =>
          throw new IllegalArgumentException(error)
      }

    val cardinalityProfileName =
      config.getString("generator.cardinality-profile")

    val cardinalityProfile =
      CardinalityProfile.fromString(cardinalityProfileName) match {
        case Right(value) => value
        case Left(error) =>
          throw new IllegalArgumentException(error)
      }

    val cardinality =
      CardinalityDefaults.forProfile(cardinalityProfile)

    val generatorSettings =
      GeneratorSettings(
        seed = config.getLong("generator.seed"),
        profile = profile,
        cardinalityProfile = cardinalityProfile,
        scenario = scenario
      )

    val outputSettings =
      OutputSettings(
        directory = config.getString("output.directory")
      )

    val profileDefinition =
      GenerationProfileDefinition.forProfile(profile)

    GenerationConfig(
      generator = generatorSettings,
      output = outputSettings,
      profileDefinition = profileDefinition,
      cardinality = cardinality,
      distributions = distributionConfig,
      productDistribution = productDistributionConfig,
      productPricing = productPricingConfig,
      productBrandAffinity = productBrandAffinityConfig,
      customerGeneration = customerGenerationConfig
    )
  }

  private def loadCustomerGenerationConfig(
                                            config: Config
                                          ): CustomerGenerationConfig = {

    val customerConfig =
      config.getConfig("customer-generation")

    val ageBands =
      customerConfig
        .getConfigList("age-bands")
        .asScala
        .map { ageBand =>
          CustomerAgeBand(
            minAge = ageBand.getInt("min-age"),
            maxAge = ageBand.getInt("max-age"),
            weight = ageBand.getDouble("weight")
          )
        }
        .toSeq

    CustomerGenerationConfig(
      asOfDate =
        LocalDate.parse(
          customerConfig.getString("as-of-date")
        ),

      registrationHistoryDays =
        customerConfig.getInt(
          "registration-history-days"
        ),

      ageBands = ageBands,

      gender =
        loadWeights(
          customerConfig,
          "gender"
        ),

      customerStatus =
        loadWeights(
          customerConfig,
          "customer-status"
        ),

      customerSegments =
        loadWeights(
          customerConfig,
          "customer-segments"
        ),

      acquisitionChannels =
        loadWeights(
          customerConfig,
          "acquisition-channels"
        ),

      acquisitionCampaigns =
        loadNestedWeights(
          customerConfig,
          "acquisition-campaigns"
        ),

      preferredDevices =
        loadWeights(
          customerConfig,
          "preferred-devices"
        ),

      preferredPaymentMethods =
        loadWeights(
          customerConfig,
          "preferred-payment-methods"
        )
    )
  }

  private def loadWeights(
                           config: Config,
                           path: String
                         ): Map[String, Double] = {

    val section =
      config.getConfig(path)

    section
      .entrySet()
      .asScala
      .map { entry =>
        entry.getKey ->
          section.getDouble(entry.getKey)
      }
      .toMap
  }

  private def loadNestedWeights(
                                 config: Config,
                                 path: String
                               ): Map[String, Map[String, Double]] = {

    val section =
      config.getConfig(path)

    section
      .root()
      .keySet()
      .asScala
      .toSeq
      .sorted
      .map { key =>
        key ->
          loadWeights(
            section,
            key
          )
      }
      .toMap
  }
}