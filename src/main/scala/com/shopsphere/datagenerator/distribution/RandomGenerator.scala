package com.shopsphere.datagenerator.distribution

import scala.util.Random

class RandomGenerator(seed: Long) {

  private val random = new Random(seed)

  def nextInt(bound: Int): Int = {
    require(
      bound > 0,
      "Bound must be greater than zero."
    )

    random.nextInt(bound)
  }

  def nextLong(): Long = {
    random.nextLong()
  }

  def nextDouble(): Double = {
    random.nextDouble()
  }

  def nextBoolean(): Boolean = {
    random.nextBoolean()
  }

  def nextInt(
               minInclusive: Int,
               maxInclusive: Int
             ): Int = {

    require(
      minInclusive <= maxInclusive,
      "Minimum value must not be greater than maximum value."
    )

    if (minInclusive == maxInclusive) {
      minInclusive
    } else {
      minInclusive +
        random.nextInt(
          maxInclusive - minInclusive + 1
        )
    }
  }

  def nextDouble(
                  minInclusive: Double,
                  maxExclusive: Double
                ): Double = {

    require(
      minInclusive <= maxExclusive,
      "Minimum value must not be greater than maximum value."
    )

    if (minInclusive == maxExclusive) {
      minInclusive
    } else {
      minInclusive +
        random.nextDouble() *
          (maxExclusive - minInclusive)
    }
  }

  def nextLong(
                minInclusive: Long,
                maxInclusive: Long
              ): Long = {

    require(
      minInclusive <= maxInclusive,
      "Minimum value must not be greater than maximum value."
    )

    if (minInclusive == maxInclusive) {
      minInclusive
    } else {
      minInclusive +
        Math.floorMod(
          random.nextLong(),
          maxInclusive - minInclusive + 1
        )
    }
  }

  def derive(streamName: String): RandomGenerator = {

    require(
      streamName.nonEmpty,
      "Stream name must not be empty."
    )

    val streamHash =
      streamName.hashCode.toLong

    val combinedSeed =
      seed ^
        (streamHash * 0x9E3779B97F4A7C15L)

    val derivedSeed =
      mix64(combinedSeed)

    new RandomGenerator(derivedSeed)
  }

  private def mix64(value: Long): Long = {

    var z =
      value + 0x9E3779B97F4A7C15L

    z =
      (z ^ (z >>> 30)) *
        0xBF58476D1CE4E5B9L

    z =
      (z ^ (z >>> 27)) *
        0x94D049BB133111EBL

    z ^ (z >>> 31)
  }
}