package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.CustomerGenerationConfig
import com.shopsphere.datagenerator.distribution.{
  RandomGenerator,
  WeightedDistribution
}
import com.shopsphere.datagenerator.model.Customer
import com.shopsphere.datagenerator.reference.customer.CustomerReferenceData

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CustomerGenerator {

  def generate(
                customerId: String,
                referenceData: CustomerReferenceData,
                generationConfig: CustomerGenerationConfig,
                random: RandomGenerator
              ): Customer = {

    require(
      customerId.nonEmpty,
      "Customer ID must not be empty."
    )

    require(
      referenceData.firstNames.nonEmpty,
      "Customer first-name reference data must not be empty."
    )

    require(
      referenceData.lastNames.nonEmpty,
      "Customer last-name reference data must not be empty."
    )

    validateConfiguration(generationConfig)

    val firstName =
      referenceData.firstNames(
        random
          .derive("first-name")
          .nextInt(referenceData.firstNames.size)
      )

    val lastName =
      referenceData.lastNames(
        random
          .derive("last-name")
          .nextInt(referenceData.lastNames.size)
      )

    val email =
      buildEmail(
        customerId = customerId,
        firstName = firstName,
        lastName = lastName
      )

    val phone =
      buildPhone(
        random = random.derive("phone")
      )

    val gender =
      sampleWeighted(
        values = generationConfig.gender,
        random = random.derive("gender"),
        fieldName = "gender"
      )

    val age =
      sampleAge(
        ageBands = generationConfig.ageBands,
        random = random.derive("age")
      )

    val dateOfBirth =
      generateDateOfBirth(
        age = age,
        asOfDate = generationConfig.asOfDate,
        random = random.derive("date-of-birth")
      )

    val registrationDate =
      generateRegistrationDate(
        dateOfBirth = dateOfBirth,
        asOfDate = generationConfig.asOfDate,
        registrationHistoryDays =
          generationConfig.registrationHistoryDays,
        random = random.derive("registration-date")
      )

    val customerStatus =
      sampleWeighted(
        values = generationConfig.customerStatus,
        random = random.derive("customer-status"),
        fieldName = "customer status"
      )

    val customerSegment =
      sampleWeighted(
        values = generationConfig.customerSegments,
        random = random.derive("customer-segment"),
        fieldName = "customer segment"
      )

    val acquisitionChannel =
      sampleWeighted(
        values = generationConfig.acquisitionChannels,
        random = random.derive("acquisition-channel"),
        fieldName = "acquisition channel"
      )

    val acquisitionCampaign =
      sampleCampaign(
        channel = acquisitionChannel,
        campaigns = generationConfig.acquisitionCampaigns,
        random = random.derive("acquisition-campaign")
      )

    val preferredDevice =
      sampleWeighted(
        values = generationConfig.preferredDevices,
        random = random.derive("preferred-device"),
        fieldName = "preferred device"
      )

    val preferredPaymentMethod =
      sampleWeighted(
        values = generationConfig.preferredPaymentMethods,
        random = random.derive("preferred-payment-method"),
        fieldName = "preferred payment method"
      )

    Customer(
      id = customerId,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      gender = gender,
      dateOfBirth = dateOfBirth,
      registrationDate = registrationDate,
      customerStatus = customerStatus,
      customerSegment = customerSegment,
      acquisitionChannel = acquisitionChannel,
      acquisitionCampaign = acquisitionCampaign,
      preferredDevice = preferredDevice,
      preferredPaymentMethod = preferredPaymentMethod
    )
  }

  private def sampleAge(
                         ageBands: Seq[com.shopsphere.datagenerator.config.CustomerAgeBand],
                         random: RandomGenerator
                       ): Int = {

    val weightedBands =
      ageBands
        .filter(_.weight > 0.0)
        .sortBy(band => (band.minAge, band.maxAge))
        .map(band => band -> band.weight)

    val selectedBand =
      new WeightedDistribution(weightedBands)
        .sample(random)

    selectedBand.minAge +
      random.nextInt(
        selectedBand.maxAge - selectedBand.minAge + 1
      )
  }

  private def generateDateOfBirth(
                                   age: Int,
                                   asOfDate: LocalDate,
                                   random: RandomGenerator
                                 ): LocalDate = {

    val earliestDate =
      asOfDate
        .minusYears(age.toLong + 1L)
        .plusDays(1)

    val latestDate =
      asOfDate.minusYears(age.toLong)

    val days =
      ChronoUnit.DAYS
        .between(earliestDate, latestDate)
        .toInt

    earliestDate.plusDays(
      random.nextInt(days + 1).toLong
    )
  }

  private def generateRegistrationDate(
                                        dateOfBirth: LocalDate,
                                        asOfDate: LocalDate,
                                        registrationHistoryDays: Int,
                                        random: RandomGenerator
                                      ): LocalDate = {

    val historyStartDate =
      asOfDate.minusDays(
        registrationHistoryDays.toLong
      )

    val legalRegistrationStart =
      dateOfBirth.plusYears(18)

    val earliestRegistrationDate =
      if (historyStartDate.isAfter(legalRegistrationStart)) {
        historyStartDate
      } else {
        legalRegistrationStart
      }

    require(
      !earliestRegistrationDate.isAfter(asOfDate),
      "Customer registration date cannot be after the as-of date."
    )

    val availableDays =
      ChronoUnit.DAYS
        .between(
          earliestRegistrationDate,
          asOfDate
        )
        .toInt

    earliestRegistrationDate.plusDays(
      random.nextInt(availableDays + 1).toLong
    )
  }

  private def sampleCampaign(
                              channel: String,
                              campaigns: Map[String, Map[String, Double]],
                              random: RandomGenerator
                            ): String = {

    val channelCampaigns =
      campaigns.getOrElse(
        channel,
        throw new IllegalArgumentException(
          s"Missing acquisition campaign configuration for channel: $channel"
        )
      )

    sampleWeighted(
      values = channelCampaigns,
      random = random,
      fieldName = s"acquisition campaign for channel '$channel'"
    )
  }

  private def sampleWeighted[T](
                                 values: Map[T, Double],
                                 random: RandomGenerator,
                                 fieldName: String
                               ): T = {

    require(
      values.nonEmpty,
      s"$fieldName configuration must not be empty."
    )

    values.foreach {
      case (value, weight) =>
        require(
          weight >= 0.0,
          s"$fieldName weight for '$value' must not be negative."
        )
    }

    val positiveValues =
      values
        .filter(_._2 > 0.0)
        .toSeq
        .sortBy(_._1.toString)

    require(
      positiveValues.nonEmpty,
      s"$fieldName configuration must contain at least one positive weight."
    )

    new WeightedDistribution(positiveValues)
      .sample(random)
  }

  private def validateConfiguration(
                                     config: CustomerGenerationConfig
                                   ): Unit = {

    require(
      config.registrationHistoryDays >= 0,
      "Customer registration history days must not be negative."
    )

    require(
      config.ageBands.nonEmpty,
      "Customer age bands must not be empty."
    )

    config.ageBands.foreach { band =>
      require(
        band.minAge >= 18,
        s"Customer minimum age must be at least 18: ${band.minAge}"
      )

      require(
        band.maxAge >= band.minAge,
        s"Customer maximum age must be >= minimum age: ${band.minAge}-${band.maxAge}"
      )

      require(
        band.weight >= 0.0,
        s"Customer age-band weight must not be negative: ${band.weight}"
      )
    }

    require(
      config.ageBands.exists(_.weight > 0.0),
      "Customer age bands must contain at least one positive weight."
    )

    validateWeights(config.gender, "gender")
    validateWeights(config.customerStatus, "customer status")
    validateWeights(config.customerSegments, "customer segment")
    validateWeights(config.acquisitionChannels, "acquisition channel")
    validateWeights(config.preferredDevices, "preferred device")
    validateWeights(
      config.preferredPaymentMethods,
      "preferred payment method"
    )
  }

  private def validateWeights[T](
                                  values: Map[T, Double],
                                  fieldName: String
                                ): Unit = {

    require(
      values.nonEmpty,
      s"$fieldName configuration must not be empty."
    )

    values.foreach {
      case (value, weight) =>
        require(
          weight >= 0.0,
          s"$fieldName weight for '$value' must not be negative."
        )
    }

    require(
      values.exists(_._2 > 0.0),
      s"$fieldName configuration must contain at least one positive weight."
    )
  }

  private def buildEmail(
                          customerId: String,
                          firstName: String,
                          lastName: String
                        ): String = {

    val normalizedFirstName =
      firstName
        .toLowerCase
        .replaceAll("[^a-z0-9]", "")

    val normalizedLastName =
      lastName
        .toLowerCase
        .replaceAll("[^a-z0-9]", "")

    s"$normalizedFirstName.$normalizedLastName.$customerId@shopsphere.example"
  }

  private def buildPhone(
                          random: RandomGenerator
                        ): String = {

    s"9${random.nextInt(100000000, 999999999)}"
  }
}