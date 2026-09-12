package com.shopsphere.datagenerator.distribution

import org.scalatest.funsuite.AnyFunSuite

class DistributionEngineTest extends AnyFunSuite {

  test("should sample registered distribution") {

    val engine =
      new DistributionEngine(
        Map(
          "status" ->
            new WeightedDistribution(
              Seq(
                "ACTIVE" -> 80.0,
                "INACTIVE" -> 20.0
              )
            )
        )
      )

    val value =
      engine.sample[String](
        "status",
        new RandomGenerator(42L)
      )

    assert(
      Set(
        "ACTIVE",
        "INACTIVE"
      ).contains(value)
    )
  }

  test("should report whether distribution exists") {

    val engine =
      new DistributionEngine(
        Map(
          "status" ->
            new UniformDistribution(
              Seq("A", "B")
            )
        )
      )

    assert(engine.contains("status"))
    assert(!engine.contains("unknown"))
  }

  test("unknown distribution should be rejected") {

    val engine =
      new DistributionEngine(
        Map.empty
      )

    assertThrows[IllegalArgumentException] {

      engine.sample[String](
        "unknown",
        new RandomGenerator(42L)
      )
    }
  }

  test("empty distribution name should be rejected") {

    val engine =
      new DistributionEngine(
        Map.empty
      )

    assertThrows[IllegalArgumentException] {

      engine.sample[String](
        "",
        new RandomGenerator(42L)
      )
    }
  }

  test("same seed should produce same result") {

    val engine =
      new DistributionEngine(
        Map(
          "status" ->
            new WeightedDistribution(
              Seq(
                "A" -> 60.0,
                "B" -> 30.0,
                "C" -> 10.0
              )
            )
        )
      )

    val random1 =
      new RandomGenerator(42L)

    val random2 =
      new RandomGenerator(42L)

    val values1 =
      (1 to 100).map { _ =>
        engine.sample[String](
          "status",
          random1
        )
      }

    val values2 =
      (1 to 100).map { _ =>
        engine.sample[String](
          "status",
          random2
        )
      }

    assert(values1 == values2)
  }
}