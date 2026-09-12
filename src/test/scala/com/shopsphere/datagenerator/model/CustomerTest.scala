package com.shopsphere.datagenerator.model

import org.scalatest.funsuite.AnyFunSuite

class CustomerTest extends AnyFunSuite {

  test("Customer should store customer attributes") {

    val customer =
      Customer(
        id = "CUSTOMER_000001",
        firstName = "Aarav",
        lastName = "Sharma",
        email = "aarav.sharma@example.com",
        phone = "9876543210"
      )

    assert(customer.id == "CUSTOMER_000001")
    assert(customer.firstName == "Aarav")
    assert(customer.lastName == "Sharma")
    assert(customer.email == "aarav.sharma@example.com")
    assert(customer.phone == "9876543210")
  }

  test("Customer should support names containing spaces") {

    val customer =
      Customer(
        id = "CUSTOMER_000002",
        firstName = "Mary Jane",
        lastName = "Watson",
        email = "mary.watson@example.com",
        phone = "9876543211"
      )

    assert(customer.firstName == "Mary Jane")
    assert(customer.lastName == "Watson")
  }
}