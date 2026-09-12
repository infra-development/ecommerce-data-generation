package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import org.scalatest.funsuite.AnyFunSuite

class ReturnGeneratorTest extends AnyFunSuite {

  test("should generate return for the supplied order") {

    val returnRecord =
      ReturnGenerator.generate(
        "ORDER_000001_RETURN",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(returnRecord.id == "ORDER_000001_RETURN")
    assert(returnRecord.orderId == "ORDER_000001")
  }

  test("should generate a valid return reason") {

    val validReasons =
      Set(
        "DAMAGED",
        "WRONG_ITEM",
        "NOT_AS_EXPECTED",
        "SIZE_ISSUE",
        "CHANGED_MIND"
      )

    val returnRecord =
      ReturnGenerator.generate(
        "ORDER_000001_RETURN",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(validReasons.contains(returnRecord.returnReason))
  }

  test("should generate a valid return status") {

    val validStatuses =
      Set(
        "REQUESTED",
        "APPROVED",
        "RECEIVED",
        "REFUNDED",
        "REJECTED"
      )

    val returnRecord =
      ReturnGenerator.generate(
        "ORDER_000001_RETURN",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(validStatuses.contains(returnRecord.returnStatus))
  }

  test("should produce the same return for the same seed") {

    val return1 =
      ReturnGenerator.generate(
        "ORDER_000001_RETURN",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    val return2 =
      ReturnGenerator.generate(
        "ORDER_000001_RETURN",
        "ORDER_000001",
        new RandomGenerator(42L)
      )

    assert(return1 == return2)
  }
}