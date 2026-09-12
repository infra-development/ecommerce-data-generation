package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.GenerationConfig
import com.shopsphere.datagenerator.distribution.{DistributionEngine, RandomGenerator, UniformDistribution, WeightedDistribution}
import com.shopsphere.datagenerator.reference.GeographyLoader

object GenerationContextFactory {

  def create(config: GenerationConfig): GenerationContext = {
    val random =
      new RandomGenerator(config.generator.seed)

    val geography =
      GeographyLoader.load("data/reference/geography")

    val distributionEngine =
      new DistributionEngine(
        config.distributions.distributions.map {
          case (name, definition) =>

            val distribution =
              definition.distributionType match {

                case "uniform" =>
                  new UniformDistribution(
                    definition.values.keys.toSeq
                  )

                case "weighted" =>
                  new WeightedDistribution(
                    definition.values.toSeq
                  )

                case other =>
                  throw new IllegalArgumentException(
                    s"Unsupported distribution type '$other' for '$name'"
                  )
              }

            name -> distribution
        }
      )

    GenerationContext(
      config = config,
      random = random,
      geography = geography,
      distributionEngine = distributionEngine
    )
  }
}