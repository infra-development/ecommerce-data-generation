package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.GenerationConfig
import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator
}
import com.shopsphere.datagenerator.reference.GeographyReferenceData

case class GenerationContext(
                              config: GenerationConfig,
                              random: RandomGenerator,
                              geography: GeographyReferenceData,
                              distributionEngine: DistributionEngine
                            )