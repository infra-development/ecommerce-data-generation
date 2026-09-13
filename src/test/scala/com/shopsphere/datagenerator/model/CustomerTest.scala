package com.shopsphere.datagenerator.model

import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class CustomerTest extends AnyFunSuite {

  test("Customer should store customer attributes") {

    val customer =
      Customer(
        id = "CUSTOMER_000001",
        firstName = "Aarav",
        lastName = "Sharma",
        email = "aarav.sharma@example.com",
        phone = "9876543210",
        gender = "MALE",
        dateOfBirth = LocalDate.of(1995, 5, 10),
        registrationDate = LocalDate.of(2020, 1, 15),
        customerStatus = "ACTIVE",
        customerSegment = "STANDARD",
        acquisitionChannel = "ORGANIC",
        acquisitionCampaign = "SEO",
        preferredDevice = "MOBILE",
        preferredPaymentMethod = "UPI"
      )

    assert(customer.id == "CUSTOMER_000001")
    assert(customer.firstName == "Aarav")
    assert(customer.lastName == "Sharma")
    assert(customer.email == "aarav.sharma@example.com")
    assert(customer.phone == "9876543210")
    assert(customer.gender == "MALE")
    assert(customer.dateOfBirth == LocalDate.of(1995, 5, 10))
    assert(customer.registrationDate == LocalDate.of(2020, 1, 15))
    assert(customer.customerStatus == "ACTIVE")
    assert(customer.customerSegment == "STANDARD")
    assert(customer.acquisitionChannel == "ORGANIC")
    assert(customer.acquisitionCampaign == "SEO")
    assert(customer.preferredDevice == "MOBILE")
    assert(customer.preferredPaymentMethod == "UPI")
  }

  test("Customer should support names containing spaces") {

    val customer =
      Customer(
        id = "CUSTOMER_000002",
        firstName = "Mary Jane",
        lastName = "Watson",
        email = "mary.watson@example.com",
        phone = "9876543211",
        gender = "FEMALE",
        dateOfBirth = LocalDate.of(1990, 8, 20),
        registrationDate = LocalDate.of(2021, 3, 10),
        customerStatus = "ACTIVE",
        customerSegment = "PREMIUM",
        acquisitionChannel = "SOCIAL",
        acquisitionCampaign = "INSTAGRAM",
        preferredDevice = "MOBILE",
        preferredPaymentMethod = "CREDIT_CARD"
      )

    assert(customer.firstName == "Mary Jane")
    assert(customer.lastName == "Watson")
  }
}