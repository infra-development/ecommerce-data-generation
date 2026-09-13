package com.shopsphere.datagenerator.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class ProductBrandAffinityConfigLoaderTest extends AnyFunSuite {

  test("load should load valid brand affinity configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |  PRODUCT_TYPE_001 {
          |    BRAND_001 = 0.50
          |    BRAND_002 = 0.30
          |    BRAND_003 = 0.20
          |  }
          |
          |  PRODUCT_TYPE_002 {
          |    BRAND_001 = 0.70
          |    BRAND_005 = 0.30
          |  }
          |}
          |""".stripMargin
      )

    val affinity =
      ProductBrandAffinityConfigLoader.load(config)

    assert(affinity.productTypes.size == 2)

    val electronics =
      affinity.productTypes("PRODUCT_TYPE_001")

    assert(electronics.size == 3)
    assert(electronics("BRAND_001") == 0.50)
    assert(electronics("BRAND_002") == 0.30)
    assert(electronics("BRAND_003") == 0.20)
  }

  test("load should reject missing product brand affinity configuration") {

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
        ProductBrandAffinityConfigLoader.load(config)
      }

    assert(
      exception.getMessage ==
        "Missing required configuration: product-brand-affinity"
    )
  }

  test("load should reject empty brand affinity for a product type") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |  PRODUCT_TYPE_001 {
          |  }
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductBrandAffinityConfigLoader.load(config)
    }
  }

  test("load should reject negative brand affinity weight") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |  PRODUCT_TYPE_001 {
          |    BRAND_001 = 0.50
          |    BRAND_002 = -0.50
          |  }
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductBrandAffinityConfigLoader.load(config)
      }

    assert(
      exception.getMessage.contains(
        "negative weight"
      )
    )
  }

  test("load should reject zero total brand affinity weight") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |  PRODUCT_TYPE_001 {
          |    BRAND_001 = 0.0
          |    BRAND_002 = 0.0
          |  }
          |}
          |""".stripMargin
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductBrandAffinityConfigLoader.load(config)
      }

    assert(
      exception.getMessage.contains(
        "positive total weight"
      )
    )
  }

  test("load should support weights that do not sum to one") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |  PRODUCT_TYPE_001 {
          |    BRAND_001 = 5.0
          |    BRAND_002 = 3.0
          |    BRAND_003 = 2.0
          |  }
          |}
          |""".stripMargin
      )

    val affinity =
      ProductBrandAffinityConfigLoader.load(config)

    val weights =
      affinity.productTypes("PRODUCT_TYPE_001")

    assert(weights("BRAND_001") == 5.0)
    assert(weights("BRAND_002") == 3.0)
    assert(weights("BRAND_003") == 2.0)
  }

  test("load should reject an empty product brand affinity configuration") {

    val config =
      ConfigFactory.parseString(
        """
          |product-brand-affinity {
          |}
          |""".stripMargin
      )

    intercept[IllegalArgumentException] {
      ProductBrandAffinityConfigLoader.load(config)
    }
  }
}