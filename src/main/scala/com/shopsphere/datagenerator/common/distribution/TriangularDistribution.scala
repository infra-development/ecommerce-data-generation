package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagenerator.common.random.RandomGenerator

class TriangularDistribution(
                              min: Double,
                              max: Double,
                              mode: Double
                            ) extends Distribution[Double] {

  require(
    !min.isNaN && !min.isInfinity,
    "Minimum must be finite."
  )

  require(
    !max.isNaN && !max.isInfinity,
    "Maximum must be finite."
  )

  require(
    !mode.isNaN && !mode.isInfinity,
    "Mode must be finite."
  )

  require(
    min <= mode,
    "Minimum must not be greater than mode."
  )

  require(
    mode <= max,
    "Mode must not be greater than maximum."
  )

  override def sample(
                       random: RandomGenerator
                     ): Double = {

    if (min == max) {
      min
    } else {
      val u = random.nextDouble()
      val range = max - min
      val modePosition = mode - min

      if (u < modePosition / range) {
        min + math.sqrt(
          u * range * modePosition
        )
      } else {
        max - math.sqrt(
          (1.0 - u) * range * (max - mode)
        )
      }
    }
  }
}