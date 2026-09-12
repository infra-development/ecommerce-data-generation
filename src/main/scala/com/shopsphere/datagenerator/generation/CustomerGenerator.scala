package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Customer
import com.shopsphere.datagenerator.reference.customer.CustomerReferenceData

object CustomerGenerator {

  def generate(
                customerId: String,
                referenceData: CustomerReferenceData,
                random: RandomGenerator
              ): Customer = {

    require(
      customerId.nonEmpty,
      "Customer ID must not be empty."
    )

    val firstName =
      referenceData.firstNames(
        random.nextInt(referenceData.firstNames.size)
      )

    val lastName =
      referenceData.lastNames(
        random.nextInt(referenceData.lastNames.size)
      )

    val email =
      buildEmail(
        customerId,
        firstName,
        lastName
      )

    val phone =
      buildPhone(random)

    Customer(
      id = customerId,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone
    )
  }

  private def buildEmail(
                          customerId: String,
                          firstName: String,
                          lastName: String
                        ): String = {

    val normalizedFirstName =
      firstName.toLowerCase.replaceAll("[^a-z0-9]", "")

    val normalizedLastName =
      lastName.toLowerCase.replaceAll("[^a-z0-9]", "")

    s"$normalizedFirstName.$normalizedLastName.$customerId@shopsphere.example"
  }

  private def buildPhone(
                          random: RandomGenerator
                        ): String = {

    s"9${random.nextInt(100000000, 999999999)}"
  }
}