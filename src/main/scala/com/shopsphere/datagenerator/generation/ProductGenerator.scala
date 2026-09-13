package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.{
  ProductBrandAffinityConfig,
  ProductDistributionConfig,
  ProductPricingConfig
}
import com.shopsphere.datagenerator.distribution.{
  RandomGenerator,
  TriangularDistribution,
  WeightedDistribution
}
import com.shopsphere.datagenerator.model.Product
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceData

object ProductGenerator {

  def generate(
                productId: String,
                referenceData: CatalogReferenceData,
                productDistributionConfig: ProductDistributionConfig,
                pricingConfig: ProductPricingConfig,
                brandAffinityConfig: ProductBrandAffinityConfig,
                random: RandomGenerator
              ): Product = {

    require(
      productId.nonEmpty,
      "Product ID must not be empty."
    )

    require(
      referenceData.categories.nonEmpty,
      "Catalog categories must not be empty."
    )

    require(
      referenceData.brands.nonEmpty,
      "Catalog brands must not be empty."
    )

    require(
      referenceData.productTypes.nonEmpty,
      "Catalog product types must not be empty."
    )

    require(
      referenceData.productModels.nonEmpty,
      "Catalog product models must not be empty."
    )

    // -----------------------------------------------------------------------
    // 1. Select category using configured category popularity
    // -----------------------------------------------------------------------

    val categoryWeights =
      productDistributionConfig.categories

    val unknownCategoryIds =
      categoryWeights.keySet.diff(
        referenceData.categories.map(_.id).toSet
      )

    require(
      unknownCategoryIds.isEmpty,
      s"Product category distribution contains unknown category IDs: ${unknownCategoryIds.toSeq.sorted.mkString(", ")}"
    )

    val availableCategoryWeights =
      categoryWeights.filter {
        case (categoryId, weight) =>
          referenceData.categories.exists(_.id == categoryId) &&
            weight > 0.0
      }

    require(
      availableCategoryWeights.nonEmpty,
      "No configured categories with positive weights are available in the catalog."
    )

    val categoriesById =
      referenceData.categories
        .map(category => category.id -> category)
        .toMap

    val weightedCategories =
      availableCategoryWeights.toSeq
        .sortBy(_._1)
        .map {
          case (categoryId, weight) =>
            categoriesById(categoryId) -> weight
        }

    val category =
      new WeightedDistribution(weightedCategories)
        .sample(
          random.derive("category")
        )

    // -----------------------------------------------------------------------
    // 2. Select product type within category using configured popularity
    // -----------------------------------------------------------------------

    val productTypesForCategory =
      referenceData.productTypes
        .filter(_.categoryId == category.id)

    require(
      productTypesForCategory.nonEmpty,
      s"No product types configured for category: ${category.id}"
    )

    val productTypeWeights =
      productDistributionConfig.productTypes.getOrElse(
        category.id,
        throw new IllegalArgumentException(
          s"Missing product type distribution for category: ${category.id}"
        )
      )

    val productTypesById =
      productTypesForCategory
        .map(productType => productType.id -> productType)
        .toMap

    val unknownProductTypeIds =
      productTypeWeights.keySet.diff(
        productTypesById.keySet
      )

    require(
      unknownProductTypeIds.isEmpty,
      s"Product type distribution contains unknown product type IDs for category '${category.id}': ${unknownProductTypeIds.toSeq.sorted.mkString(", ")}"
    )

    val availableProductTypeWeights =
      productTypeWeights.filter {
        case (productTypeId, weight) =>
          productTypesById.contains(productTypeId) &&
            weight > 0.0
      }

    require(
      availableProductTypeWeights.nonEmpty,
      s"No configured product types with positive weights are available for category: ${category.id}"
    )

    val weightedProductTypes =
      availableProductTypeWeights.toSeq
        .sortBy(_._1)
        .map {
          case (productTypeId, weight) =>
            productTypesById(productTypeId) -> weight
        }

    val productType =
      new WeightedDistribution(weightedProductTypes)
        .sample(
          random.derive("product-type")
        )

    // -----------------------------------------------------------------------
    // 3. Validate pricing configuration
    // -----------------------------------------------------------------------

    val priceDefinition =
      pricingConfig.productTypes.getOrElse(
        productType.id,
        throw new IllegalArgumentException(
          s"Missing product pricing configuration for product type: ${productType.id}"
        )
      )

    // -----------------------------------------------------------------------
    // 4. Get brand affinity configuration
    // -----------------------------------------------------------------------

    val brandWeights =
      brandAffinityConfig.productTypes.getOrElse(
        productType.id,
        throw new IllegalArgumentException(
          s"Missing product brand affinity configuration for product type: ${productType.id}"
        )
      )

    val brandsById =
      referenceData.brands
        .map(brand => brand.id -> brand)
        .toMap

    val unknownBrandIds =
      brandWeights.keySet.diff(brandsById.keySet)

    require(
      unknownBrandIds.isEmpty,
      s"Product brand affinity contains unknown brand IDs for category '${category.id}': ${unknownBrandIds.toSeq.sorted.mkString(", ")}"
    )

    // -----------------------------------------------------------------------
    // 5. Find models available for this category + product type
    //
    //    Category
    //        ↓
    //    Product Type
    //        ↓
    //    Available Product Models
    //
    //    We must not select a brand that has no model for this
    //    product type.
    // -----------------------------------------------------------------------

    val productModelsForType =
      referenceData.productModels.filter { model =>
        model.categoryId == category.id &&
          model.productTypeId == productType.id
      }

    require(
      productModelsForType.nonEmpty,
      s"No product models configured for category '${category.id}', product type '${productType.id}'"
    )

    // -----------------------------------------------------------------------
    // 6. Restrict brand affinity to brands that actually have models
    // -----------------------------------------------------------------------

    val availableBrandIds =
      productModelsForType
        .map(_.brandId)
        .toSet

    val availableBrandWeights =
      brandWeights.filter {
        case (brandId, _) =>
          availableBrandIds.contains(brandId)
      }

    require(
      availableBrandWeights.nonEmpty,
      s"No configured brands with product models for category '${category.id}', product type '${productType.id}'"
    )

    // -----------------------------------------------------------------------
    // 7. Select brand using configured affinity
    //
    //    We preserve the relative affinity weights among feasible brands.
    // -----------------------------------------------------------------------

    val weightedBrands =
      availableBrandWeights.toSeq
        .sortBy(_._1)
        .map {
          case (brandId, weight) =>
            brandsById(brandId) -> weight
        }

    val brandDistribution =
      new WeightedDistribution(weightedBrands)

    val brand =
      brandDistribution.sample(
        random.derive("brand")
      )

    // -----------------------------------------------------------------------
    // 8. Select product model for the selected brand
    // -----------------------------------------------------------------------

    val productModels =
      productModelsForType.filter(
        _.brandId == brand.id
      )

    require(
      productModels.nonEmpty,
      s"No product models configured for category '${category.id}', product type '${productType.id}', brand '${brand.id}'"
    )

    val productModel =
      productModels(
        random
          .derive("product-model")
          .nextInt(productModels.size)
      )

    // -----------------------------------------------------------------------
    // 9. Generate price
    // -----------------------------------------------------------------------

    val price =
      generatePrice(
        min = priceDefinition.min,
        max = priceDefinition.max,
        mode = priceDefinition.mode,
        random = random.derive("price")
      )

    // -----------------------------------------------------------------------
    // 10. Build Product / SKU
    // -----------------------------------------------------------------------

    Product(
      id = productId,
      name = s"${brand.name} ${productModel.name}",
      categoryId = category.id,
      productTypeId = productType.id,
      brandId = brand.id,
      productModelId = productModel.id,
      price = price
    )
  }

  private def generatePrice(
                             min: Double,
                             max: Double,
                             mode: Double,
                             random: RandomGenerator
                           ): BigDecimal = {

    val distribution =
      new TriangularDistribution(
        min = min,
        max = max,
        mode = mode
      )

    BigDecimal(
      distribution.sample(random)
    ).setScale(
      2,
      BigDecimal.RoundingMode.HALF_UP
    )
  }
}