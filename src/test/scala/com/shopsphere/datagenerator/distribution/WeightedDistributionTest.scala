package com.shopsphere.datagenerator.distribution

import com.shopsphere.datagenerator.common.distribution.WeightedDistribution
import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class WeightedDistributionTest extends AnyFunSuite {

  test("should sample only from supplied values") {

    val distribution =
      new WeightedDistribution(
        Seq(
          "A" -> 70.0,
          "B" -> 20.0,
          "C" -> 10.0
        )
      )

    val random =
      new RandomGenerator(42L)

    (1 to 1000).foreach { _ =>

      val value =
        distribution.sample(random)

      assert(
        Set("A", "B", "C").contains(value)
      )
    }
  }

  test("zero weight value should never be selected") {

    val distribution =
      new WeightedDistribution(
        Seq(
          "A" -> 1.0,
          "B" -> 0.0
        )
      )

    val random =
      new RandomGenerator(42L)

    (1 to 1000).foreach { _ =>
      assert(
        distribution.sample(random) == "A"
      )
    }
  }

  test("weights do not need to sum to one") {

    val distribution =
      new WeightedDistribution(
        Seq(
          "A" -> 70.0,
          "B" -> 30.0
        )
      )

    val random =
      new RandomGenerator(42L)

    (1 to 1000).foreach { _ =>

      assert(
        Set("A", "B").contains(
          distribution.sample(random)
        )
      )
    }
  }

  test("negative weight should be rejected") {

    assertThrows[IllegalArgumentException] {

      new WeightedDistribution(
        Seq(
          "A" -> 1.0,
          "B" -> -1.0
        )
      )
    }
  }

  test("all zero weights should be rejected") {

    assertThrows[IllegalArgumentException] {

      new WeightedDistribution(
        Seq(
          "A" -> 0.0,
          "B" -> 0.0
        )
      )
    }
  }

  test("empty values should be rejected") {

    assertThrows[IllegalArgumentException] {

      new WeightedDistribution(
        Seq.empty[(String, Double)]
      )
    }
  }

  test("same seed should produce same sequence") {

    val distribution =
      new WeightedDistribution(
        Seq(
          "A" -> 70.0,
          "B" -> 20.0,
          "C" -> 10.0
        )
      )

    val random1 =
      new RandomGenerator(42L)

    val random2 =
      new RandomGenerator(42L)

    val values1 =
      (1 to 1000).map { _ =>
        distribution.sample(random1)
      }

    val values2 =
      (1 to 1000).map { _ =>
        distribution.sample(random2)
      }

    assert(values1 == values2)
  }
}