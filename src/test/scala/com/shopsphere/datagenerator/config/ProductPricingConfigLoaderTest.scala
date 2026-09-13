package com.shopsphere.datagenerator.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class ProductPricingConfigLoaderTest extends AnyFunSuite {

  test("load should load valid product pricing configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 500.0
          |    max = 150000.0
          |    mode = 30000.0
          |  }
          |
          |  CATEGORY_002 {
          |    min = 3000.0
          |    max = 150000.0
          |    mode = 30000.0
          |  }
          |}
          |""".stripMargin
      )

    val pricing =
      ProductPricingConfigLoader.load(config)

    assert(pricing.categories.size == 2)

    val electronics =
      pricing.categories("CATEGORY_001")

    assert(electronics.min == 500.0)
    assert(electronics.max == 150000.0)
    assert(electronics.mode == 30000.0)
  }

  test("load should reject missing product pricing configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |generator {
          |  seed = 42
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductPricingConfigLoader.load(config)
      }

    assert(
      exception.getMessage ==
        "Missing required configuration: product-pricing"
    )
  }

  test("load should reject missing minimum price") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    max = 150000.0
          |    mode = 30000.0
          |  }
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductPricingConfigLoader.load(config)
      }

    assert(
      exception.getMessage.contains(
        "Missing product pricing 'min'"
      )
    )
  }

  test("load should reject missing maximum price") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 500.0
          |    mode = 30000.0
          |  }
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductPricingConfigLoader.load(config)
      }

    assert(
      exception.getMessage.contains(
        "Missing product pricing 'max'"
      )
    )
  }

  test("load should reject missing mode") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 500.0
          |    max = 150000.0
          |  }
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductPricingConfigLoader.load(config)
      }

    assert(
      exception.getMessage.contains(
        "Missing product pricing 'mode'"
      )
    )
  }

  test("load should reject negative minimum price") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = -500.0
          |    max = 150000.0
          |    mode = 30000.0
          |  }
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductPricingConfigLoader.load(config)
    }
  }

  test("load should reject minimum greater than maximum") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 150000.0
          |    max = 500.0
          |    mode = 30000.0
          |  }
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductPricingConfigLoader.load(config)
    }
  }

  test("load should reject mode below minimum") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 500.0
          |    max = 150000.0
          |    mode = 100.0
          |  }
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductPricingConfigLoader.load(config)
    }
  }

  test("load should reject mode above maximum") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |  CATEGORY_001 {
          |    min = 500.0
          |    max = 150000.0
          |    mode = 200000.0
          |  }
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductPricingConfigLoader.load(config)
    }
  }

  test("load should reject empty product pricing configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |product-pricing {
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductPricingConfigLoader.load(config)
    }
  }
}