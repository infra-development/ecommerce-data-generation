package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagenerator.common.random.RandomGenerator

trait Distribution[T] {

  def sample(random: RandomGenerator): T
}