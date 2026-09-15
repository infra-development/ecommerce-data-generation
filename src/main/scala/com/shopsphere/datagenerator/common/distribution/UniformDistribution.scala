package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagenerator.common.random.RandomGenerator

class UniformDistribution[T](
                              values: Seq[T]
                            ) extends Distribution[T] {

  require(
    values.nonEmpty,
    "Uniform distribution values must not be empty."
  )

  override def sample(
                       random: RandomGenerator
                     ): T = {

    values(
      random.nextInt(values.size)
    )
  }
}