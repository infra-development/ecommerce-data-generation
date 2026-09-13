package com.shopsphere.datagenerator.config

case class AddressGenerationConfig(
                                    addressTypes: Map[String, Double],
                                    primaryAddressProbability: Double,
                                    addressLine2Probability: Double
                                  )