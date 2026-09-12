package com.shopsphere.datagenerator.distribution

import org.scalatest.funsuite.AnyFunSuite

class UniformDistributionTest extends AnyFunSuite {

  test("should sample only from supplied values") {

    val distribution =
      new UniformDistribution(
        Seq("A", "B", "C")
      )

    val random =
      new RandomGenerator(42L)

    (1 to 100).foreach { _ =>
      val value =
        distribution.sample(random)

      assert(
        Set("A", "B", "C").contains(value)
      )
    }
  }

  test("same seed should produce same sequence") {

    val distribution =
      new UniformDistribution(
        Seq("A", "B", "C")
      )

    val random1 =
      new RandomGenerator(42L)

    val random2 =
      new RandomGenerator(42L)

    val values1 =
      (1 to 100).map { _ =>
        distribution.sample(random1)
      }

    val values2 =
      (1 to 100).map { _ =>
        distribution.sample(random2)
      }

    assert(values1 == values2)
  }

  test("empty values should be rejected") {

    assertThrows[IllegalArgumentException] {

      new UniformDistribution(
        Seq.empty[String]
      )
    }
  }
}