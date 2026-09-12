package com.shopsphere.datagenerator.config

import com.typesafe.config.ConfigFactory

object ConfigLoader {

  def load(): GenerationConfig = {
    val config = ConfigFactory.load()
    val distributionConfig = DistributionConfigLoader.load(config)

    val profileName = config.getString("generator.profile")
    val profile = GenerationProfile.fromString(profileName) match {
      case Right(value) => value
      case Left(error) =>
        throw new IllegalArgumentException(error)
    }

    val scenarioName = config.getString("generator.scenario")
    val scenario = GenerationScenario.fromString(scenarioName) match {
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

    val generatorSettings = GeneratorSettings(
      seed = config.getLong("generator.seed"),
      profile = profile,
      cardinalityProfile = cardinalityProfile,
      scenario = scenario
    )

    val outputSettings = OutputSettings(
      directory = config.getString("output.directory")
    )

    val profileDefinition =
      GenerationProfileDefinition.forProfile(profile)

    GenerationConfig(
      generator = generatorSettings,
      output = outputSettings,
      profileDefinition = profileDefinition,
      cardinality = cardinality,
      distributions = distributionConfig
    )
  }
}