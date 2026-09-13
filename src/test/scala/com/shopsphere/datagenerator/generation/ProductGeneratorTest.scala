package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.{
  ProductPriceDefinition,
  ProductPricingConfig
}
import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceLoader
import org.scalatest.funsuite.AnyFunSuite

class ProductGeneratorTest extends AnyFunSuite {

  private val pricingConfig =
    ProductPricingConfig(
      categories = Map(
        "CATEGORY_001" ->
          ProductPriceDefinition(
            min = 500.0,
            max = 150000.0,
            mode = 30000.0
          ),

        "CATEGORY_002" ->
          ProductPriceDefinition(
            min = 3000.0,
            max = 150000.0,
            mode = 30000.0
          ),

        "CATEGORY_003" ->
          ProductPriceDefinition(
            min = 25000.0,
            max = 250000.0,
            mode = 60000.0
          ),

        "CATEGORY_004" ->
          ProductPriceDefinition(
            min = 8000.0,
            max = 250000.0,
            mode = 40000.0
          ),

        "CATEGORY_005" ->
          ProductPriceDefinition(
            min = 500.0,
            max = 150000.0,
            mode = 20000.0
          ),

        "CATEGORY_006" ->
          ProductPriceDefinition(
            min = 200.0,
            max = 25000.0,
            mode = 1500.0
          ),

        "CATEGORY_007" ->
          ProductPriceDefinition(
            min = 300.0,
            max = 30000.0,
            mode = 2500.0
          ),

        "CATEGORY_008" ->
          ProductPriceDefinition(
            min = 100.0,
            max = 15000.0,
            mode = 1000.0
          ),

        "CATEGORY_009" ->
          ProductPriceDefinition(
            min = 100.0,
            max = 5000.0,
            mode = 500.0
          ),

        "CATEGORY_010" ->
          ProductPriceDefinition(
            min = 20.0,
            max = 5000.0,
            mode = 300.0
          )
      )
    )

  private val referenceData =
    CatalogReferenceLoader.load(
      "data/reference/catalog"
    )

  test("generate should create a product with valid references and category-specific price") {

    val product =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(42L)
      )

    assert(product.id == "PRODUCT_000001")
    assert(product.name.nonEmpty)

    assert(
      referenceData.categories.map(_.id).contains(
        product.categoryId
      )
    )

    assert(
      referenceData.brands.map(_.id).contains(
        product.brandId
      )
    )

    val priceDefinition =
      pricingConfig.categories(product.categoryId)

    assert(
      product.price >= BigDecimal(priceDefinition.min)
    )

    assert(
      product.price <= BigDecimal(priceDefinition.max)
    )
  }

  test("generate should be reproducible with the same seed") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(42L)
      )

    assert(product1 == product2)
  }

  test("generate should produce different results with different seeds") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(99L)
      )

    assert(product1 != product2)
  }

  test("generate should reject an empty product ID") {

    intercept[IllegalArgumentException] {
      ProductGenerator.generate(
        productId = "",
        referenceData = referenceData,
        pricingConfig = pricingConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test("generate should reject missing pricing configuration for the selected category") {

    val incompletePricingConfig =
      ProductPricingConfig(
        categories = Map.empty
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductGenerator.generate(
          productId = "PRODUCT_000001",
          referenceData = referenceData,
          pricingConfig = incompletePricingConfig,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage.contains(
        "Missing product pricing configuration"
      )
    )
  }

  test("generate should produce prices within the configured range for every category") {

    referenceData.categories.foreach { category =>

      val categoryPricing =
        pricingConfig.categories(category.id)

      val products =
        (1 to 1000).map { index =>

          ProductGenerator.generate(
            productId = f"PRODUCT_$index%06d",
            referenceData =
              referenceData.copy(
                categories = Seq(category)
              ),
            pricingConfig = pricingConfig,
            random = new RandomGenerator(index.toLong)
          )
        }

      assert(
        products.forall(
          _.categoryId == category.id
        )
      )

      assert(
        products.forall(
          _.price >= BigDecimal(categoryPricing.min)
        )
      )

      assert(
        products.forall(
          _.price <= BigDecimal(categoryPricing.max)
        )
      )
    }
  }
}