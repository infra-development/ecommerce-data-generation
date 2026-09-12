package com.shopsphere.datagenerator.distribution

trait Distribution[T] {

  def sample(random: RandomGenerator): T
}