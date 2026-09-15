package com.shopsphere.datagenerator.distribution

import com.shopsphere.datagenerator.common.distribution.TriangularDistribution
import com.shopsphere.datagenerator.common.random.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class TriangularDistributionTest extends AnyFunSuite {

  test("sample should always remain within the configured range") {

    val distribution =
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 300.0
      )

    val random =
      new RandomGenerator(42L)

    val samples =
      (1 to 10000).map(_ => distribution.sample(random))

    assert(samples.forall(_ >= 100.0))
    assert(samples.forall(_ <= 1000.0))
  }

  test("sample should be reproducible with the same seed") {

    val distribution =
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 300.0
      )

    val samples1 =
      (1 to 100).map(
        _ => distribution.sample(new RandomGenerator(42L))
      )

    val samples2 =
      (1 to 100).map(
        _ => distribution.sample(new RandomGenerator(42L))
      )

    assert(samples1 == samples2)
  }

  test("different seeds should produce different samples") {

    val distribution =
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 300.0
      )

    val samples1 =
      (1 to 100).map(
        _ => distribution.sample(new RandomGenerator(42L))
      )

    val samples2 =
      (1 to 100).map(
        _ => distribution.sample(new RandomGenerator(99L))
      )

    assert(samples1 != samples2)
  }

  test("should support a mode equal to minimum") {

    val distribution =
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 100.0
      )

    val random =
      new RandomGenerator(42L)

    val samples =
      (1 to 1000).map(_ => distribution.sample(random))

    assert(samples.forall(_ >= 100.0))
    assert(samples.forall(_ <= 1000.0))
  }

  test("should support a mode equal to maximum") {

    val distribution =
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 1000.0
      )

    val random =
      new RandomGenerator(42L)

    val samples =
      (1 to 1000).map(_ => distribution.sample(random))

    assert(samples.forall(_ >= 100.0))
    assert(samples.forall(_ <= 1000.0))
  }

  test("should return the fixed value when minimum equals maximum") {

    val distribution =
      new TriangularDistribution(
        min = 500.0,
        max = 500.0,
        mode = 500.0
      )

    val random =
      new RandomGenerator(42L)

    val samples =
      (1 to 100).map(_ => distribution.sample(random))

    assert(samples.forall(_ == 500.0))
  }

  test("should reject mode below minimum") {

    intercept[IllegalArgumentException] {
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 50.0
      )
    }
  }

  test("should reject mode above maximum") {

    intercept[IllegalArgumentException] {
      new TriangularDistribution(
        min = 100.0,
        max = 1000.0,
        mode = 1500.0
      )
    }
  }

  test("should reject minimum greater than maximum") {

    intercept[IllegalArgumentException] {
      new TriangularDistribution(
        min = 1000.0,
        max = 100.0,
        mode = 500.0
      )
    }
  }
}