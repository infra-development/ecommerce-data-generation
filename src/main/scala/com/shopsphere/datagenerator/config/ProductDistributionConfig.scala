package com.shopsphere.datagenerator.config

case class ProductDistributionConfig(
                                      categories: Map[String, Double],
                                      productTypes: Map[String, Map[String, Double]]
                                    )