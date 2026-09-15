package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagenerator.common.random.RandomGenerator

class DistributionEngine(
                          distributions: Map[String, Distribution[_]]
                        ) {

  require(
    distributions.keys.forall(_.nonEmpty),
    "Distribution names must not be empty."
  )

  def sample[T](
                 name: String,
                 random: RandomGenerator
               ): T = {

    if (name.isEmpty) {
      throw new IllegalArgumentException(
        "Distribution name must not be empty."
      )
    }

    val distribution =
      distributions.getOrElse(
        name,
        throw new IllegalArgumentException(
          s"Unknown distribution: $name"
        )
      )

    distribution
      .asInstanceOf[Distribution[T]]
      .sample(random)
  }

  def contains(name: String): Boolean =
    distributions.contains(name)
}