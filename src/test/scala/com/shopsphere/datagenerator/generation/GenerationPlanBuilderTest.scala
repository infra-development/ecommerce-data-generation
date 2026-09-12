package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config._
import org.scalatest.funsuite.AnyFunSuite

class GenerationPlanBuilderTest extends AnyFunSuite {

  test("build should calculate counts from profile and cardinality configuration") {

    val config =
      GenerationConfig(
        generator = GeneratorSettings(
          seed = 42L,
          profile = GenerationProfile.Small,
          cardinalityProfile = CardinalityProfile.Medium,
          scenario = GenerationScenario.Baseline
        ),
        output = OutputSettings(
          directory = "data/raw"
        ),
        profileDefinition =
          GenerationProfileDefinition.small,
        cardinality =
          CardinalityDefaults.medium,
        distributions = DistributionConfig(
          distributions = Map.empty
        )
      )

    val plan =
      GenerationPlanBuilder.build(config)

    assert(plan.customerCount == 1_000L)
    assert(plan.addressCount == 1_200L)
    assert(plan.productCount == 500L)
    assert(plan.orderCount == 5_000L)
    assert(plan.orderItemCount == 12_000L)
    assert(plan.paymentCount == 5_000L)
    assert(plan.shipmentCount == 5_000L)
    assert(plan.returnCount == 400L)
    assert(plan.sessionCount == 3_000L)
    assert(plan.eventCount == 24_000L)
  }

  test("build should support high cardinality") {

    val config =
      GenerationConfig(
        generator = GeneratorSettings(
          seed = 42L,
          profile = GenerationProfile.Small,
          cardinalityProfile = CardinalityProfile.High,
          scenario = GenerationScenario.Baseline
        ),
        output = OutputSettings(
          directory = "data/raw"
        ),
        profileDefinition =
          GenerationProfileDefinition.small,
        cardinality =
          CardinalityDefaults.high,
        distributions = DistributionConfig(
          distributions = Map.empty
        )
      )

    val plan =
      GenerationPlanBuilder.build(config)

    assert(plan.addressCount == 1_500L)
    assert(plan.sessionCount == 5_000L)
    assert(plan.orderItemCount == 16_000L)
    assert(plan.paymentCount == 5_000L)
    assert(plan.shipmentCount == 5_000L)
    assert(plan.returnCount == 750L)
    assert(plan.eventCount == 60_000L)
  }
}