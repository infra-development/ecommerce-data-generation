package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.config.{
  ProductBrandAffinityConfig,
  ProductDistributionConfig,
  ProductPriceDefinition,
  ProductPricingConfig
}
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceLoader
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Paths

class ProductGeneratorTest extends AnyFunSuite {

  private val referenceData =
    CatalogReferenceLoader.load(
      Paths.get("data/reference/catalog")
    )

  private val productDistributionConfig =
    ProductDistributionConfig(
      categories =
        referenceData.categories
          .map(category => category.id -> 1.0)
          .toMap,
      productTypes =
        referenceData.categories
          .map { category =>
            category.id ->
              referenceData.productTypes
                .filter(_.categoryId == category.id)
                .map(productType => productType.id -> 1.0)
                .toMap
          }
          .toMap
    )

  private val pricingConfig =
    ProductPricingConfig(
      productTypes = Map(
        "PRODUCT_TYPE_001" ->
          ProductPriceDefinition(
            min = 3500.0,
            max = 45000.0,
            mode = 18000.0
          ),
        "PRODUCT_TYPE_002" ->
          ProductPriceDefinition(
            min = 1800.0,
            max = 30000.0,
            mode = 9000.0
          ),
        "PRODUCT_TYPE_003" ->
          ProductPriceDefinition(
            min = 5000.0,
            max = 50000.0,
            mode = 18000.0
          ),
        "PRODUCT_TYPE_004" ->
          ProductPriceDefinition(
            min = 7000.0,
            max = 120000.0,
            mode = 35000.0
          ),
        "PRODUCT_TYPE_005" ->
          ProductPriceDefinition(
            min = 1200.0,
            max = 8000.0,
            mode = 3000.0
          ),
        "PRODUCT_TYPE_006" ->
          ProductPriceDefinition(
            min = 60000.0,
            max = 250000.0,
            mode = 130000.0
          ),
        "PRODUCT_TYPE_007" ->
          ProductPriceDefinition(
            min = 50000.0,
            max = 180000.0,
            mode = 85000.0
          ),
        "PRODUCT_TYPE_008" ->
          ProductPriceDefinition(
            min = 50000.0,
            max = 180000.0,
            mode = 90000.0
          ),
        "PRODUCT_TYPE_009" ->
          ProductPriceDefinition(
            min = 25000.0,
            max = 180000.0,
            mode = 65000.0
          ),
        "PRODUCT_TYPE_010" ->
          ProductPriceDefinition(
            min = 70000.0,
            max = 250000.0,
            mode = 120000.0
          ),
        "PRODUCT_TYPE_011" ->
          ProductPriceDefinition(
            min = 35000.0,
            max = 220000.0,
            mode = 80000.0
          ),
        "PRODUCT_TYPE_012" ->
          ProductPriceDefinition(
            min = 20000.0,
            max = 180000.0,
            mode = 50000.0
          ),
        "PRODUCT_TYPE_013" ->
          ProductPriceDefinition(
            min = 18000.0,
            max = 90000.0,
            mode = 35000.0
          ),
        "PRODUCT_TYPE_014" ->
          ProductPriceDefinition(
            min = 25000.0,
            max = 90000.0,
            mode = 45000.0
          ),
        "PRODUCT_TYPE_015" ->
          ProductPriceDefinition(
            min = 5000.0,
            max = 30000.0,
            mode = 12000.0
          ),
        "PRODUCT_TYPE_016" ->
          ProductPriceDefinition(
            min = 500.0,
            max = 5000.0,
            mode = 1800.0
          ),
        "PRODUCT_TYPE_017" ->
          ProductPriceDefinition(
            min = 1200.0,
            max = 7000.0,
            mode = 2800.0
          ),
        "PRODUCT_TYPE_018" ->
          ProductPriceDefinition(
            min = 1800.0,
            max = 12000.0,
            mode = 5000.0
          ),
        "PRODUCT_TYPE_019" ->
          ProductPriceDefinition(
            min = 1200.0,
            max = 10000.0,
            mode = 3500.0
          ),
        "PRODUCT_TYPE_020" ->
          ProductPriceDefinition(
            min = 2500.0,
            max = 18000.0,
            mode = 6000.0
          ),
        "PRODUCT_TYPE_021" ->
          ProductPriceDefinition(
            min = 2000.0,
            max = 16000.0,
            mode = 5000.0
          ),
        "PRODUCT_TYPE_022" ->
          ProductPriceDefinition(
            min = 2500.0,
            max = 16000.0,
            mode = 6000.0
          ),
        "PRODUCT_TYPE_023" ->
          ProductPriceDefinition(
            min = 150.0,
            max = 1500.0,
            mode = 500.0
          ),
        "PRODUCT_TYPE_024" ->
          ProductPriceDefinition(
            min = 200.0,
            max = 3000.0,
            mode = 800.0
          ),
        "PRODUCT_TYPE_025" ->
          ProductPriceDefinition(
            min = 250.0,
            max = 1800.0,
            mode = 700.0
          ),
        "PRODUCT_TYPE_026" ->
          ProductPriceDefinition(
            min = 300.0,
            max = 2500.0,
            mode = 900.0
          ),
        "PRODUCT_TYPE_027" ->
          ProductPriceDefinition(
            min = 150.0,
            max = 1200.0,
            mode = 450.0
          ),
        "PRODUCT_TYPE_028" ->
          ProductPriceDefinition(
            min = 250.0,
            max = 1500.0,
            mode = 600.0
          ),
        "PRODUCT_TYPE_029" ->
          ProductPriceDefinition(
            min = 500.0,
            max = 2500.0,
            mode = 1000.0
          ),
        "PRODUCT_TYPE_030" ->
          ProductPriceDefinition(
            min = 250.0,
            max = 2000.0,
            mode = 700.0
          ),
        "PRODUCT_TYPE_031" ->
          ProductPriceDefinition(
            min = 30.0,
            max = 500.0,
            mode = 150.0
          ),
        "PRODUCT_TYPE_032" ->
          ProductPriceDefinition(
            min = 40.0,
            max = 1000.0,
            mode = 200.0
          ),
        "PRODUCT_TYPE_033" ->
          ProductPriceDefinition(
            min = 50.0,
            max = 2500.0,
            mode = 700.0
          ),
        "PRODUCT_TYPE_034" ->
          ProductPriceDefinition(
            min = 80.0,
            max = 1200.0,
            mode = 300.0
          )
      )
    )

  private val brandAffinityConfig =
    ProductBrandAffinityConfig(
      productTypes = Map(
        "PRODUCT_TYPE_001" -> Map(
          "BRAND_001" -> 0.55,
          "BRAND_002" -> 0.30,
          "BRAND_005" -> 0.15
        ),
        "PRODUCT_TYPE_002" -> Map(
          "BRAND_001" -> 0.45,
          "BRAND_002" -> 0.35,
          "BRAND_005" -> 0.20
        ),
        "PRODUCT_TYPE_003" -> Map(
          "BRAND_001" -> 0.60,
          "BRAND_004" -> 0.40
        ),
        "PRODUCT_TYPE_004" -> Map(
          "BRAND_001" -> 0.35,
          "BRAND_002" -> 0.25,
          "BRAND_005" -> 0.20,
          "BRAND_003" -> 0.10,
          "BRAND_012" -> 0.10
        ),
        "PRODUCT_TYPE_005" -> Map(
          "BRAND_012" -> 0.60,
          "BRAND_003" -> 0.40
        ),
        "PRODUCT_TYPE_006" -> Map(
          "BRAND_007" -> 0.55,
          "BRAND_006" -> 0.45
        ),
        "PRODUCT_TYPE_007" -> Map(
          "BRAND_006" -> 0.40,
          "BRAND_007" -> 0.35,
          "BRAND_008" -> 0.25
        ),
        "PRODUCT_TYPE_008" -> Map(
          "BRAND_008" -> 0.55,
          "BRAND_006" -> 0.45
        ),
        "PRODUCT_TYPE_009" -> Map(
          "BRAND_004" -> 0.45,
          "BRAND_003" -> 0.30,
          "BRAND_001" -> 0.25
        ),
        "PRODUCT_TYPE_010" -> Map(
          "BRAND_003" -> 0.55,
          "BRAND_004" -> 0.45
        ),
        "PRODUCT_TYPE_011" -> Map(
          "BRAND_001" -> 0.55,
          "BRAND_004" -> 0.45
        ),
        "PRODUCT_TYPE_012" -> Map(
          "BRAND_001" -> 0.60,
          "BRAND_003" -> 0.40
        ),
        "PRODUCT_TYPE_013" -> Map(
          "BRAND_003" -> 0.55,
          "BRAND_001" -> 0.45
        ),
        "PRODUCT_TYPE_014" -> Map(
          "BRAND_001" -> 0.55,
          "BRAND_003" -> 0.45
        ),
        "PRODUCT_TYPE_015" -> Map(
          "BRAND_003" -> 0.55,
          "BRAND_004" -> 0.45
        ),
        "PRODUCT_TYPE_016" -> Map(
          "BRAND_009" -> 0.50,
          "BRAND_010" -> 0.50
        ),
        "PRODUCT_TYPE_017" -> Map(
          "BRAND_009" -> 0.50,
          "BRAND_010" -> 0.50
        ),
        "PRODUCT_TYPE_018" -> Map(
          "BRAND_009" -> 0.60,
          "BRAND_010" -> 0.40
        ),
        "PRODUCT_TYPE_019" -> Map(
          "BRAND_010" -> 0.60,
          "BRAND_009" -> 0.40
        ),
        "PRODUCT_TYPE_020" -> Map(
          "BRAND_009" -> 0.55,
          "BRAND_010" -> 0.45
        ),
        "PRODUCT_TYPE_021" -> Map(
          "BRAND_009" -> 0.55,
          "BRAND_010" -> 0.45
        ),
        "PRODUCT_TYPE_022" -> Map(
          "BRAND_009" -> 0.55,
          "BRAND_010" -> 0.45
        ),
        "PRODUCT_TYPE_023" -> Map(
          "BRAND_015" -> 0.45,
          "BRAND_016" -> 0.30,
          "BRAND_017" -> 0.25
        ),
        "PRODUCT_TYPE_024" -> Map(
          "BRAND_016" -> 0.45,
          "BRAND_015" -> 0.30,
          "BRAND_017" -> 0.25
        ),
        "PRODUCT_TYPE_025" -> Map(
          "BRAND_015" -> 0.40,
          "BRAND_016" -> 0.35,
          "BRAND_017" -> 0.25
        ),
        "PRODUCT_TYPE_026" -> Map(
          "BRAND_018" -> 0.50,
          "BRAND_017" -> 0.30,
          "BRAND_015" -> 0.20
        ),
        "PRODUCT_TYPE_027" -> Map(
          "BRAND_019" -> 0.55,
          "BRAND_020" -> 0.45
        ),
        "PRODUCT_TYPE_028" -> Map(
          "BRAND_019" -> 0.50,
          "BRAND_020" -> 0.35,
          "BRAND_021" -> 0.15
        ),
        "PRODUCT_TYPE_029" -> Map(
          "BRAND_021" -> 0.60,
          "BRAND_019" -> 0.25,
          "BRAND_020" -> 0.15
        ),
        "PRODUCT_TYPE_030" -> Map(
          "BRAND_019" -> 0.50,
          "BRAND_020" -> 0.30,
          "BRAND_021" -> 0.20
        ),
        "PRODUCT_TYPE_031" -> Map(
          "BRAND_022" -> 0.40,
          "BRAND_023" -> 0.35,
          "BRAND_026" -> 0.25
        ),
        "PRODUCT_TYPE_032" -> Map(
          "BRAND_022" -> 0.45,
          "BRAND_023" -> 0.30,
          "BRAND_025" -> 0.25
        ),
        "PRODUCT_TYPE_033" -> Map(
          "BRAND_024" -> 0.45,
          "BRAND_025" -> 0.30,
          "BRAND_026" -> 0.25
        ),
        "PRODUCT_TYPE_034" -> Map(
          "BRAND_022" -> 0.40,
          "BRAND_023" -> 0.30,
          "BRAND_026" -> 0.30
        )
      )
    )

  test(
    "generate should create a product with valid category, type, brand, model and product-type-specific price"
  ) {

    val product =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )

    assert(product.id == "PRODUCT_000001")
    assert(product.name.nonEmpty)

    val category =
      referenceData.categories
        .find(_.id == product.categoryId)
        .get

    val productType =
      referenceData.productTypes
        .find(_.id == product.productTypeId)
        .get

    val brand =
      referenceData.brands
        .find(_.id == product.brandId)
        .get

    val productModel =
      referenceData.productModels
        .find(_.id == product.productModelId)
        .get

    assert(productType.categoryId == category.id)
    assert(productModel.categoryId == category.id)
    assert(productModel.productTypeId == productType.id)
    assert(productModel.brandId == brand.id)

    val priceDefinition =
      pricingConfig.productTypes(product.productTypeId)

    assert(
      product.price >= BigDecimal(priceDefinition.min)
    )

    assert(
      product.price <= BigDecimal(priceDefinition.max)
    )

    assert(
      brandAffinityConfig.productTypes(product.productTypeId)
        .contains(product.brandId)
    )
  }

  test("generate should be reproducible with the same seed") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )

    assert(product1 == product2)
  }

  test("generate should produce different results with different seeds") {

    val product1 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )

    val product2 =
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(99L)
      )

    assert(product1 != product2)
  }

  test("generate should reject an empty product ID") {

    intercept[IllegalArgumentException] {
      ProductGenerator.generate(
        productId = "",
        referenceData = referenceData,
        productDistributionConfig = productDistributionConfig,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )
    }
  }

  test(
    "generate should reject missing pricing configuration for the selected product type"
  ) {

    val incompletePricingConfig =
      ProductPricingConfig(
        productTypes = Map.empty
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductGenerator.generate(
          productId = "PRODUCT_000001",
          referenceData = referenceData,
          productDistributionConfig = productDistributionConfig,
          pricingConfig = incompletePricingConfig,
          brandAffinityConfig = brandAffinityConfig,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage.contains(
        "Missing product pricing configuration"
      )
    )
  }

  test(
    "generate should reject missing brand affinity configuration for the selected product type"
  ) {

    val incompleteBrandAffinityConfig =
      ProductBrandAffinityConfig(
        productTypes = Map.empty
      )

    val exception =
      intercept[IllegalArgumentException] {
        ProductGenerator.generate(
          productId = "PRODUCT_000001",
          referenceData = referenceData,
          productDistributionConfig = productDistributionConfig,
          pricingConfig = pricingConfig,
          brandAffinityConfig = incompleteBrandAffinityConfig,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage.contains(
        "Missing product brand affinity configuration"
      )
    )
  }

  test(
    "generate should select only brands configured for the selected product type"
  ) {

    val productType =
      referenceData.productTypes
        .find(_.id == "PRODUCT_TYPE_004")
        .get

    val category =
      referenceData.categories
        .find(_.id == productType.categoryId)
        .get

    val productTypeReferenceData =
      referenceData.copy(
        categories = Seq(category),
        productTypes = Seq(productType),
        productModels =
          referenceData.productModels
            .filter(_.productTypeId == productType.id)
      )

    val allowedBrands =
      brandAffinityConfig.productTypes(productType.id).keySet

    val products =
      (1 to 1000).map { index =>

        ProductGenerator.generate(
          productId = f"PRODUCT_$index%06d",
          referenceData = productTypeReferenceData,
          productDistributionConfig = productDistributionConfig.copy(
            categories = Map(category.id -> 1.0),
            productTypes =
              Map(
                category.id ->
                  Map(productType.id -> 1.0)
              )
          ),
          pricingConfig = pricingConfig,
          brandAffinityConfig = brandAffinityConfig,
          random = new RandomGenerator(index.toLong)
        )
      }

    assert(
      products.forall(
        product => product.categoryId == category.id
      )
    )

    assert(
      products.forall(
        product => product.productTypeId == productType.id
      )
    )

    assert(
      products.forall(
        product => allowedBrands.contains(product.brandId)
      )
    )
  }

  test(
    "generate should reject unknown brand IDs in affinity configuration"
  ) {

    val invalidAffinityConfig =
      ProductBrandAffinityConfig(
        productTypes = Map(
          "PRODUCT_TYPE_001" -> Map(
            "BRAND_999" -> 1.0
          )
        )
      )

    val productType =
      referenceData.productTypes
        .find(_.id == "PRODUCT_TYPE_001")
        .get

    val category =
      referenceData.categories
        .find(_.id == productType.categoryId)
        .get

    val productTypeReferenceData =
      referenceData.copy(
        categories = Seq(category),
        productTypes = Seq(productType),
        productModels =
          referenceData.productModels
            .filter(_.productTypeId == productType.id)
      )

    val exception =
      intercept[IllegalArgumentException] {

        ProductGenerator.generate(
          productId = "PRODUCT_000001",
          referenceData = productTypeReferenceData,
          productDistributionConfig = productDistributionConfig.copy(
            categories = Map(category.id -> 1.0),
            productTypes =
              Map(
                category.id ->
                  Map(productType.id -> 1.0)
              )
          ),
          pricingConfig = pricingConfig,
          brandAffinityConfig = invalidAffinityConfig,
          random = new RandomGenerator(42L)
        )
      }

    assert(
      exception.getMessage.contains(
        "unknown brand IDs"
      )
    )
  }

  test(
    "generate should produce prices within the configured range for every product type"
  ) {

    referenceData.productTypes.foreach { productType =>

      val category =
        referenceData.categories
          .find(_.id == productType.categoryId)
          .get

      val productTypePricing =
        pricingConfig.productTypes(productType.id)

      val productTypeReferenceData =
        referenceData.copy(
          categories = Seq(category),
          productTypes = Seq(productType),
          productModels =
            referenceData.productModels
              .filter(_.productTypeId == productType.id)
        )

      val products =
        (1 to 1000).map { index =>

          ProductGenerator.generate(
            productId = f"${productType.id}_$index%06d",
            referenceData = productTypeReferenceData,
            productDistributionConfig = productDistributionConfig.copy(
              categories = Map(category.id -> 1.0),
              productTypes =
                Map(
                  category.id ->
                    Map(productType.id -> 1.0)
                )
            ),
            pricingConfig = pricingConfig,
            brandAffinityConfig = brandAffinityConfig,
            random =
              new RandomGenerator(
                productType.id.hashCode.toLong * 10000L + index
              )
          )
        }

      assert(
        products.forall(
          _.productTypeId == productType.id
        )
      )

      assert(
        products.forall(
          _.price >= BigDecimal(productTypePricing.min)
        )
      )

      assert(
        products.forall(
          _.price <= BigDecimal(productTypePricing.max)
        )
      )

      val allowedBrands =
        brandAffinityConfig.productTypes(
          productType.id
        ).keySet

      assert(
        products.forall(
          product => allowedBrands.contains(product.brandId)
        )
      )
    }
  }

  test("generate should honor a single positive category weight") {
    val category = referenceData.categories.find(_.id == "CATEGORY_002").get
    val types = referenceData.productTypes.filter(_.categoryId == category.id)

    val restrictedReferenceData = referenceData.copy(
      categories = Seq(category),
      productTypes = types,
      productModels = referenceData.productModels.filter(_.categoryId == category.id)
    )

    val distribution = ProductDistributionConfig(
      categories = Map(category.id -> 1.0),
      productTypes = Map(
        category.id -> types.map(t => t.id -> 1.0).toMap
      )
    )

    val products = (1 to 1000).map { index =>
      ProductGenerator.generate(
        productId = f"PRODUCT_$index%06d",
        referenceData = restrictedReferenceData,
        productDistributionConfig = distribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(index.toLong)
      )
    }

    assert(products.forall(_.categoryId == category.id))
  }

  test("generate should honor a single positive product type weight") {
    val productType = referenceData.productTypes.find(_.id == "PRODUCT_TYPE_004").get
    val category = referenceData.categories.find(_.id == productType.categoryId).get

    val restrictedReferenceData = referenceData.copy(
      categories = Seq(category),
      productTypes = Seq(productType),
      productModels = referenceData.productModels.filter(_.productTypeId == productType.id)
    )

    val distribution = ProductDistributionConfig(
      categories = Map(category.id -> 1.0),
      productTypes = Map(category.id -> Map(productType.id -> 1.0))
    )

    val products = (1 to 1000).map { index =>
      ProductGenerator.generate(
        productId = f"PRODUCT_$index%06d",
        referenceData = restrictedReferenceData,
        productDistributionConfig = distribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(index.toLong)
      )
    }

    assert(products.forall(_.productTypeId == productType.id))
  }

  test("generate should never select a zero-weight category") {
    val positiveCategory = referenceData.categories.find(_.id == "CATEGORY_002").get
    val zeroCategory = referenceData.categories.find(_.id == "CATEGORY_003").get

    val distribution = ProductDistributionConfig(
      categories = Map(
        positiveCategory.id -> 1.0,
        zeroCategory.id -> 0.0
      ),
      productTypes = Map(
        positiveCategory.id ->
          referenceData.productTypes.filter(_.categoryId == positiveCategory.id)
            .map(t => t.id -> 1.0).toMap,
        zeroCategory.id ->
          referenceData.productTypes.filter(_.categoryId == zeroCategory.id)
            .map(t => t.id -> 1.0).toMap
      )
    )

    val products = (1 to 1000).map { index =>
      ProductGenerator.generate(
        productId = f"PRODUCT_$index%06d",
        referenceData = referenceData,
        productDistributionConfig = distribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(index.toLong)
      )
    }

    assert(products.forall(_.categoryId == positiveCategory.id))
  }

  test("generate should never select a zero-weight product type") {
    val category = referenceData.categories.find(_.id == "CATEGORY_002").get
    val positiveType = referenceData.productTypes.find(_.id == "PRODUCT_TYPE_004").get
    val zeroType = referenceData.productTypes.find(_.id == "PRODUCT_TYPE_005").get

    val distribution = ProductDistributionConfig(
      categories = Map(category.id -> 1.0),
      productTypes = Map(
        category.id -> Map(
          positiveType.id -> 1.0,
          zeroType.id -> 0.0
        )
      )
    )

    val products = (1 to 1000).map { index =>
      ProductGenerator.generate(
        productId = f"PRODUCT_$index%06d",
        referenceData = referenceData,
        productDistributionConfig = distribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(index.toLong)
      )
    }

    assert(products.forall(_.productTypeId == positiveType.id))
  }

  test("generate should reject unknown category IDs in product distribution configuration") {
    val invalidDistribution = productDistributionConfig.copy(
      categories = productDistributionConfig.categories + ("CATEGORY_999" -> 1.0)
    )

    val exception = intercept[IllegalArgumentException] {
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = invalidDistribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )
    }

    assert(exception.getMessage.contains("unknown category IDs"))
  }

  test("generate should reject unknown product type IDs in product distribution configuration") {
    val category = referenceData.categories.find(_.id == "CATEGORY_002").get

    val invalidDistribution = ProductDistributionConfig(
      categories = Map(category.id -> 1.0),
      productTypes = Map(category.id -> Map("PRODUCT_TYPE_999" -> 1.0))
    )

    val exception = intercept[IllegalArgumentException] {
      ProductGenerator.generate(
        productId = "PRODUCT_000001",
        referenceData = referenceData,
        productDistributionConfig = invalidDistribution,
        pricingConfig = pricingConfig,
        brandAffinityConfig = brandAffinityConfig,
        random = new RandomGenerator(42L)
      )
    }

    assert(exception.getMessage.contains("unknown product type IDs"))
  }

  test("generate should remain reproducible with product distribution configuration") {
    val product1 = ProductGenerator.generate(
      productId = "PRODUCT_000001",
      referenceData = referenceData,
      productDistributionConfig = productDistributionConfig,
      pricingConfig = pricingConfig,
      brandAffinityConfig = brandAffinityConfig,
      random = new RandomGenerator(12345L)
    )

    val product2 = ProductGenerator.generate(
      productId = "PRODUCT_000001",
      referenceData = referenceData,
      productDistributionConfig = productDistributionConfig,
      pricingConfig = pricingConfig,
      brandAffinityConfig = brandAffinityConfig,
      random = new RandomGenerator(12345L)
    )

    assert(product1 == product2)
  }


  test(
    "generate should approximately follow configured category weights at population level"
  ) {

    val category1 =
      referenceData.categories
        .find(_.id == "CATEGORY_001")
        .get

    val category2 =
      referenceData.categories
        .find(_.id == "CATEGORY_002")
        .get

    val productTypesByCategory =
      referenceData.productTypes
        .groupBy(_.categoryId)
        .map {
          case (categoryId, productTypes) =>
            categoryId ->
              productTypes
                .map(productType => productType.id -> 1.0)
                .toMap
        }

    val distribution =
      ProductDistributionConfig(
        categories =
          Map(
            category1.id -> 0.70,
            category2.id -> 0.30
          ),
        productTypes =
          Map(
            category1.id ->
              Map(
                "PRODUCT_TYPE_001" -> 0.80,
                "PRODUCT_TYPE_002" -> 0.20
              ),
            category2.id ->
              productTypesByCategory(category2.id)
          )
      )

    val products =
      (1 to 10000).map { index =>
        ProductGenerator.generate(
          productId = f"PRODUCT_$index%06d",
          referenceData = referenceData,
          productDistributionConfig = distribution,
          pricingConfig = pricingConfig,
          brandAffinityConfig = brandAffinityConfig,
          random = new RandomGenerator(index.toLong)
        )
      }

    val category1Frequency =
      products.count(_.categoryId == category1.id).toDouble / products.size

    val category2Frequency =
      products.count(_.categoryId == category2.id).toDouble / products.size

    assert(math.abs(category1Frequency - 0.70) <= 0.03)
    assert(math.abs(category2Frequency - 0.30) <= 0.03)
  }

  test(
    "generate should approximately follow configured product type weights within a category"
  ) {

    val category =
      referenceData.categories
        .find(_.id == "CATEGORY_001")
        .get

    val productType1 =
      referenceData.productTypes
        .find(_.id == "PRODUCT_TYPE_001")
        .get

    val productType2 =
      referenceData.productTypes
        .find(_.id == "PRODUCT_TYPE_002")
        .get

    val restrictedReferenceData =
      referenceData.copy(
        categories = Seq(category),
        productTypes = Seq(productType1, productType2),
        productModels =
          referenceData.productModels
            .filter(model =>
              model.productTypeId == productType1.id ||
                model.productTypeId == productType2.id
            )
      )

    val distribution =
      ProductDistributionConfig(
        categories =
          Map(
            category.id -> 1.0
          ),
        productTypes =
          Map(
            category.id ->
              Map(
                productType1.id -> 0.80,
                productType2.id -> 0.20
              )
          )
      )

    val products =
      (1 to 10000).map { index =>
        ProductGenerator.generate(
          productId = f"PRODUCT_$index%06d",
          referenceData = restrictedReferenceData,
          productDistributionConfig = distribution,
          pricingConfig = pricingConfig,
          brandAffinityConfig = brandAffinityConfig,
          random = new RandomGenerator(index.toLong)
        )
      }

    val productType1Frequency =
      products.count(_.productTypeId == productType1.id).toDouble / products.size

    val productType2Frequency =
      products.count(_.productTypeId == productType2.id).toDouble / products.size

    assert(math.abs(productType1Frequency - 0.80) <= 0.03)
    assert(math.abs(productType2Frequency - 0.20) <= 0.03)
  }

}