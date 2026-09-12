package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.reference.customer.CustomerReferenceLoader
import org.scalatest.funsuite.AnyFunSuite

class CustomerGeneratorTest extends AnyFunSuite {

  private val referenceData =
    CustomerReferenceLoader.load(
      "data/reference/customer"
    )

  test("generate should create a customer with valid attributes") {

    val customer =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    assert(customer.id == "CUSTOMER_000001")
    assert(customer.firstName.nonEmpty)
    assert(customer.lastName.nonEmpty)
    assert(customer.email.nonEmpty)
    assert(customer.phone.nonEmpty)

    assert(
      customer.email.endsWith(
        "@shopsphere.example"
      )
    )

    assert(
      customer.email.contains(
        customer.id
      )
    )

    assert(
      customer.phone.length == 10
    )

    assert(
      customer.phone.forall(_.isDigit)
    )
  }

  test("generate should be reproducible with the same seed") {

    val customer1 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    val customer2 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    assert(customer1 == customer2)
  }

  test("generate should produce different results with different seeds") {

    val customer1 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )

    val customer2 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        random = new RandomGenerator(99L)
      )

    assert(customer1 != customer2)
  }

  test("generate should reject an empty customer ID") {

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "",
        referenceData = referenceData,
        random = new RandomGenerator(42L)
      )
    }
  }
}