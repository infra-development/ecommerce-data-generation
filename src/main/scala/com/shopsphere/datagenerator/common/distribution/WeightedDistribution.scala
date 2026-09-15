package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagenerator.common.random.RandomGenerator

class WeightedDistribution[T](
                               values: Seq[(T, Double)]
                             ) extends Distribution[T] {

  require(
    values.nonEmpty,
    "Weighted distribution values must not be empty."
  )

  values.foreach {
    case (_, weight) =>
      require(
        weight >= 0.0,
        "Distribution weights must not be negative."
      )
  }

  private val totalWeight =
    values.map(_._2).sum

  require(
    totalWeight > 0.0,
    "Total distribution weight must be greater than zero."
  )

  private val cumulativeWeights =
    values
      .scanLeft(0.0) {
        case (cumulative, (_, weight)) =>
          cumulative + weight
      }
      .tail

  override def sample(
                       random: RandomGenerator
                     ): T = {

    val target =
      random.nextDouble(
        0.0,
        totalWeight
      )

    val index =
      cumulativeWeights.indexWhere(
        target < _
      )

    if (index >= 0) {
      values(index)._1
    } else {
      values.last._1
    }
  }
}