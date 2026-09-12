package com.shopsphere.datagenerator.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class DistributionConfigLoaderTest extends AnyFunSuite {

  test("should load configured distributions") {

    val config =
      ConfigFactory.load()

    val distributionConfig =
      DistributionConfigLoader.load(config)

    assert(
      distributionConfig.distributions.contains(
        "order_status"
      )
    )

    assert(
      distributionConfig.distributions.contains(
        "payment_method"
      )
    )

    assert(
      distributionConfig.distributions.contains(
        "device_type"
      )
    )

    assert(
      distributionConfig.distributions.contains(
        "channel"
      )
    )
  }

  test("should load weighted values") {

    val config =
      ConfigFactory.load()

    val distributionConfig =
      DistributionConfigLoader.load(config)

    val orderStatus =
      distributionConfig
        .distributions("order_status")

    assert(
      orderStatus.distributionType == "weighted"
    )

    assert(
      orderStatus.values("DELIVERED") == 0.60
    )

    assert(
      orderStatus.values("CANCELLED") == 0.05
    )
  }

  test("should reject missing distributions configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |generator {
          |  seed = 42
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      DistributionConfigLoader.load(config)
    }
  }

  test("should reject missing distribution type") {

    val config =
      ConfigFactory.parseString(
        """
          |distributions {
          |  test {
          |    values {
          |      A = 1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      DistributionConfigLoader.load(config)
    }
  }

  test("should reject missing distribution values") {

    val config =
      ConfigFactory.parseString(
        """
          |distributions {
          |  test {
          |    type = "weighted"
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      DistributionConfigLoader.load(config)
    }
  }

  test("should reject negative weights") {

    val config =
      ConfigFactory.parseString(
        """
          |distributions {
          |  test {
          |    type = "weighted"
          |    values {
          |      A = 1.0
          |      B = -1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      DistributionConfigLoader.load(config)
    }
  }

  test("should reject zero total weight") {

    val config =
      ConfigFactory.parseString(
        """
          |distributions {
          |  test {
          |    type = "weighted"
          |    values {
          |      A = 0.0
          |      B = 0.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      DistributionConfigLoader.load(config)
    }
  }
}