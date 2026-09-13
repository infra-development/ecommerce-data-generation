package com.shopsphere.datagenerator.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class ProductDistributionConfigLoaderTest extends AnyFunSuite {

  test("should load category and product type distributions") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |
          |  categories {
          |    CATEGORY_001 = 0.60
          |    CATEGORY_002 = 0.40
          |  }
          |
          |  product-types {
          |
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 0.70
          |      PRODUCT_TYPE_002 = 0.30
          |    }
          |
          |    CATEGORY_002 {
          |      PRODUCT_TYPE_003 = 0.80
          |      PRODUCT_TYPE_004 = 0.20
          |    }
          |  }
          |}
          |""".stripMargin
      )

    val result =
      ProductDistributionConfigLoader.load(config)

    assert(
      result.categories ==
        Map(
          "CATEGORY_001" -> 0.60,
          "CATEGORY_002" -> 0.40
        )
    )

    assert(
      result.productTypes ==
        Map(
          "CATEGORY_001" ->
            Map(
              "PRODUCT_TYPE_001" -> 0.70,
              "PRODUCT_TYPE_002" -> 0.30
            ),
          "CATEGORY_002" ->
            Map(
              "PRODUCT_TYPE_003" -> 0.80,
              "PRODUCT_TYPE_004" -> 0.20
            )
        )
    )
  }

  test("missing product-distribution should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |generator {
          |  seed = 42
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("missing categories should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("missing product-types should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 1.0
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("empty category distribution should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {}
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("empty product type distribution should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 1.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {}
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("negative category weight should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 0.5
          |    CATEGORY_002 = -0.5
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("negative product type weight should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 1.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 0.5
          |      PRODUCT_TYPE_002 = -0.5
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("all zero category weights should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 0.0
          |    CATEGORY_002 = 0.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 1.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("all zero product type weights should be rejected") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 1.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 0.0
          |      PRODUCT_TYPE_002 = 0.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    assertThrows[IllegalArgumentException] {
      ProductDistributionConfigLoader.load(config)
    }
  }

  test("weights do not need to sum to one") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 60.0
          |    CATEGORY_002 = 40.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 70.0
          |      PRODUCT_TYPE_002 = 30.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    val result =
      ProductDistributionConfigLoader.load(config)

    assert(
      result.categories.values.sum == 100.0
    )

    assert(
      result.productTypes("CATEGORY_001").values.sum == 100.0
    )
  }

  test("zero weight product type should be allowed") {

    val config =
      ConfigFactory.parseString(
        """
          |product-distribution {
          |  categories {
          |    CATEGORY_001 = 1.0
          |  }
          |
          |  product-types {
          |    CATEGORY_001 {
          |      PRODUCT_TYPE_001 = 1.0
          |      PRODUCT_TYPE_002 = 0.0
          |    }
          |  }
          |}
          |""".stripMargin
      )

    val result =
      ProductDistributionConfigLoader.load(config)

    assert(
      result.productTypes("CATEGORY_001")("PRODUCT_TYPE_002") == 0.0
    )
  }
}