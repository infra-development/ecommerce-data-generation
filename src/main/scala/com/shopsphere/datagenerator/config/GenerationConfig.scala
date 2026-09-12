package com.shopsphere.datagenerator.config

case class GeneratorSettings(
                              seed: Long,
                              profile: GenerationProfile,
                              cardinalityProfile: CardinalityProfile,
                              scenario: GenerationScenario
                            )

case class OutputSettings(
                           directory: String
                         )

case class GenerationConfig(
                             generator: GeneratorSettings,
                             output: OutputSettings,
                             profileDefinition: GenerationProfileDefinition,
                             cardinality: CardinalityConfig,
                             distributions: DistributionConfig
                           )