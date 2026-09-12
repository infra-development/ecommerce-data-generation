package com.shopsphere.datagenerator.generation

import org.scalatest.funsuite.AnyFunSuite

class CardinalityAllocatorTest
  extends AnyFunSuite {

  test(
    "allocate should preserve exact total for fractional cardinality"
  ) {

    val allocation =
      CardinalityAllocator.allocate(
        baseCount = 5000,
        averagePerBase = 2.4
      )

    assert(allocation.size == 5000)
    assert(allocation.sum == 12000)
    assert(allocation.count(_ == 2) == 3000)
    assert(allocation.count(_ == 3) == 2000)
  }

  test(
    "allocate should handle whole-number cardinality"
  ) {

    val allocation =
      CardinalityAllocator.allocate(
        baseCount = 100,
        averagePerBase = 3.0
      )

    assert(allocation.size == 100)
    assert(allocation.sum == 300)
    assert(allocation.forall(_ == 3))
  }

  test(
    "allocate should handle cardinality below one"
  ) {

    val allocation =
      CardinalityAllocator.allocate(
        baseCount = 10,
        averagePerBase = 0.4
      )

    assert(allocation.size == 10)
    assert(allocation.sum == 4)
    assert(allocation.count(_ == 0) == 6)
    assert(allocation.count(_ == 1) == 4)
  }

  test(
    "allocate should return zero counts for zero cardinality"
  ) {

    val allocation =
      CardinalityAllocator.allocate(
        baseCount = 10,
        averagePerBase = 0.0
      )

    assert(allocation.size == 10)
    assert(allocation.sum == 0)
    assert(allocation.forall(_ == 0))
  }

  test(
    "allocate should return empty sequence for zero base count"
  ) {

    val allocation =
      CardinalityAllocator.allocate(
        baseCount = 0,
        averagePerBase = 2.4
      )

    assert(allocation.isEmpty)
  }

  test(
    "allocate should reject negative cardinality"
  ) {

    assertThrows[IllegalArgumentException] {
      CardinalityAllocator.allocate(
        baseCount = 10,
        averagePerBase = -1.0
      )
    }
  }
}