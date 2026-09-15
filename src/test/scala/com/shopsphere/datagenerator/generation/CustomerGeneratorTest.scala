package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.config.{
  CustomerAgeBand,
  CustomerGenerationConfig
}
import com.shopsphere.datagenerator.reference.customer.CustomerReferenceLoader
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CustomerGeneratorTest extends AnyFunSuite {

  private val referenceData =
    CustomerReferenceLoader.load(
      "data/reference/customer"
    )

  private val generationConfig =
    CustomerGenerationConfig(
      asOfDate = LocalDate.of(2026, 1, 1),
      registrationHistoryDays = 3650,
      ageBands = Seq(
        CustomerAgeBand(18, 24, 0.10),
        CustomerAgeBand(25, 34, 0.30),
        CustomerAgeBand(35, 44, 0.28),
        CustomerAgeBand(45, 54, 0.18),
        CustomerAgeBand(55, 64, 0.10),
        CustomerAgeBand(65, 80, 0.04)
      ),
      gender = Map(
        "MALE" -> 0.50,
        "FEMALE" -> 0.47,
        "OTHER" -> 0.03
      ),
      customerStatus = Map(
        "ACTIVE" -> 0.92,
        "INACTIVE" -> 0.05,
        "SUSPENDED" -> 0.02,
        "CLOSED" -> 0.01
      ),
      customerSegments = Map(
        "STANDARD" -> 0.70,
        "PREMIUM" -> 0.20,
        "VIP" -> 0.08,
        "BUSINESS" -> 0.02
      ),
      acquisitionChannels = Map(
        "ORGANIC" -> 0.35,
        "PAID_SEARCH" -> 0.20,
        "SOCIAL" -> 0.15,
        "EMAIL" -> 0.10,
        "DIRECT" -> 0.15,
        "REFERRAL" -> 0.05
      ),
      acquisitionCampaigns = Map(
        "ORGANIC" -> Map(
          "SEO" -> 0.70,
          "CONTENT" -> 0.30
        ),
        "PAID_SEARCH" -> Map(
          "GOOGLE_ADS" -> 0.70,
          "BING_ADS" -> 0.30
        ),
        "SOCIAL" -> Map(
          "INSTAGRAM" -> 0.50,
          "FACEBOOK" -> 0.30,
          "YOUTUBE" -> 0.20
        ),
        "EMAIL" -> Map(
          "WELCOME" -> 0.60,
          "PROMOTION" -> 0.40
        ),
        "DIRECT" -> Map(
          "DIRECT_VISIT" -> 1.00
        ),
        "REFERRAL" -> Map(
          "CUSTOMER_REFERRAL" -> 0.70,
          "PARTNER_REFERRAL" -> 0.30
        )
      ),
      preferredDevices = Map(
        "MOBILE" -> 0.70,
        "DESKTOP" -> 0.25,
        "TABLET" -> 0.05
      ),
      preferredPaymentMethods = Map(
        "UPI" -> 0.45,
        "CREDIT_CARD" -> 0.20,
        "DEBIT_CARD" -> 0.15,
        "NET_BANKING" -> 0.10,
        "WALLET" -> 0.10
      )
    )

  test("generate should create a customer with valid attributes") {

    val customer =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(42L)
      )

    assert(customer.id == "CUSTOMER_000001")

    assert(customer.firstName.nonEmpty)
    assert(customer.lastName.nonEmpty)

    assert(customer.email.nonEmpty)
    assert(customer.email.endsWith("@shopsphere.example"))
    assert(customer.email.contains(customer.id))

    assert(customer.phone.nonEmpty)
    assert(customer.phone.length == 10)
    assert(customer.phone.forall(_.isDigit))

    assert(Set("MALE", "FEMALE", "OTHER").contains(customer.gender))

    assert(
      !customer.dateOfBirth.isAfter(generationConfig.asOfDate)
    )

    val age =
      java.time.Period
        .between(
          customer.dateOfBirth,
          generationConfig.asOfDate
        )
        .getYears

    assert(age >= 18)
    assert(age <= 80)

    assert(
      !customer.registrationDate.isAfter(
        generationConfig.asOfDate
      )
    )

    assert(
      !customer.registrationDate.isBefore(
        customer.dateOfBirth.plusYears(18)
      )
    )

    assert(
      !customer.registrationDate.isBefore(
        generationConfig.asOfDate
          .minusDays(generationConfig.registrationHistoryDays.toLong)
      )
    )

    assert(
      Set(
        "ACTIVE",
        "INACTIVE",
        "SUSPENDED",
        "CLOSED"
      ).contains(customer.customerStatus)
    )

    assert(
      Set(
        "STANDARD",
        "PREMIUM",
        "VIP",
        "BUSINESS"
      ).contains(customer.customerSegment)
    )

    assert(
      generationConfig.acquisitionChannels.contains(
        customer.acquisitionChannel
      )
    )

    assert(
      generationConfig.acquisitionCampaigns(
        customer.acquisitionChannel
      ).contains(
        customer.acquisitionCampaign
      )
    )

    assert(
      Set(
        "MOBILE",
        "DESKTOP",
        "TABLET"
      ).contains(customer.preferredDevice)
    )

    assert(
      Set(
        "UPI",
        "CREDIT_CARD",
        "DEBIT_CARD",
        "NET_BANKING",
        "WALLET"
      ).contains(customer.preferredPaymentMethod)
    )
  }

  test("generate should be reproducible with the same seed") {

    val customer1 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(42L)
      )

    val customer2 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(42L)
      )

    assert(customer1 == customer2)
  }

  test("generate should produce different results with different seeds") {

    val customer1 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(42L)
      )

    val customer2 =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(99L)
      )

    assert(customer1 != customer2)
  }

  test("generate should respect single-positive-weight configuration") {

    val deterministicConfig =
      generationConfig.copy(
        ageBands = Seq(
          CustomerAgeBand(
            minAge = 30,
            maxAge = 30,
            weight = 1.0
          )
        ),
        gender = Map(
          "FEMALE" -> 1.0
        ),
        customerStatus = Map(
          "ACTIVE" -> 1.0
        ),
        customerSegments = Map(
          "PREMIUM" -> 1.0
        ),
        acquisitionChannels = Map(
          "ORGANIC" -> 1.0
        ),
        acquisitionCampaigns = Map(
          "ORGANIC" -> Map(
            "SEO" -> 1.0
          )
        ),
        preferredDevices = Map(
          "MOBILE" -> 1.0
        ),
        preferredPaymentMethods = Map(
          "UPI" -> 1.0
        )
      )

    val customer =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = deterministicConfig,
        random = new RandomGenerator(42L)
      )

    assert(customer.gender == "FEMALE")
    assert(customer.customerStatus == "ACTIVE")
    assert(customer.customerSegment == "PREMIUM")
    assert(customer.acquisitionChannel == "ORGANIC")
    assert(customer.acquisitionCampaign == "SEO")
    assert(customer.preferredDevice == "MOBILE")
    assert(customer.preferredPaymentMethod == "UPI")

    assert(
      java.time.Period
        .between(
          customer.dateOfBirth,
          deterministicConfig.asOfDate
        )
        .getYears == 30
    )
  }

  test("generate should reject an empty customer ID") {

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "",
        referenceData = referenceData,
        generationConfig = generationConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject negative registration history") {

    val invalidConfig =
      generationConfig.copy(
        registrationHistoryDays = -1
      )

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = invalidConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject customer age below 18") {

    val invalidConfig =
      generationConfig.copy(
        ageBands = Seq(
          CustomerAgeBand(
            minAge = 17,
            maxAge = 25,
            weight = 1.0
          )
        )
      )

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = invalidConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject negative gender weight") {

    val invalidConfig =
      generationConfig.copy(
        gender = Map(
          "MALE" -> 0.5,
          "FEMALE" -> -0.5
        )
      )

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = invalidConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject a weight configuration with no positive values") {

    val invalidConfig =
      generationConfig.copy(
        preferredDevices = Map(
          "MOBILE" -> 0.0,
          "DESKTOP" -> 0.0
        )
      )

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = invalidConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject missing acquisition campaign configuration") {

    val invalidConfig =
      generationConfig.copy(
        acquisitionChannels = Map(
          "ORGANIC" -> 1.0
        ),
        acquisitionCampaigns = Map.empty
      )

    intercept[IllegalArgumentException] {
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = invalidConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should keep registration date within configured history") {

    val config =
      generationConfig.copy(
        registrationHistoryDays = 365
      )

    val customer =
      CustomerGenerator.generate(
        customerId = "CUSTOMER_000001",
        referenceData = referenceData,
        generationConfig = config,
        random = new RandomGenerator(42L)
      )

    val historyStart =
      config.asOfDate.minusDays(
        config.registrationHistoryDays.toLong
      )

    assert(
      !customer.registrationDate.isBefore(historyStart)
    )

    assert(
      !customer.registrationDate.isAfter(config.asOfDate)
    )

    assert(
      !customer.registrationDate.isBefore(
        customer.dateOfBirth.plusYears(18)
      )
    )
  }
}