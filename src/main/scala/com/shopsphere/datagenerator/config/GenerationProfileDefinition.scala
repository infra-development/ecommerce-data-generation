package com.shopsphere.datagenerator.config

case class GenerationProfileDefinition(
                                        profile: GenerationProfile,
                                        customerCount: Long,
                                        productCount: Long,
                                        orderCount: Long
                                      )

object GenerationProfileDefinition {

  val small: GenerationProfileDefinition =
    GenerationProfileDefinition(
      profile = GenerationProfile.Small,
      customerCount = 1_000L,
      productCount = 500L,
      orderCount = 5_000L
    )

  val medium: GenerationProfileDefinition =
    GenerationProfileDefinition(
      profile = GenerationProfile.Medium,
      customerCount = 100_000L,
      productCount = 10_000L,
      orderCount = 1_000_000L
    )

  val large: GenerationProfileDefinition =
    GenerationProfileDefinition(
      profile = GenerationProfile.Large,
      customerCount = 10_000_000L,
      productCount = 100_000L,
      orderCount = 100_000_000L
    )

  val xlarge: GenerationProfileDefinition =
    GenerationProfileDefinition(
      profile = GenerationProfile.XLarge,
      customerCount = 100_000_000L,
      productCount = 1_000_000L,
      orderCount = 1_000_000_000L
    )

  val all: Seq[GenerationProfileDefinition] = Seq(
    small,
    medium,
    large,
    xlarge
  )

  def forProfile(
                  profile: GenerationProfile
                ): GenerationProfileDefinition = {
    all.find(_.profile == profile).get
  }
}