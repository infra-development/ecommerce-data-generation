package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.config.ProductPricingConfig
import com.shopsphere.datagenerator.distribution.{
  RandomGenerator,
  TriangularDistribution
}
import com.shopsphere.datagenerator.model.Product
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceData

object ProductGenerator {

  def generate(
                productId: String,
                referenceData: CatalogReferenceData,
                pricingConfig: ProductPricingConfig,
                random: RandomGenerator
              ): Product = {

    if (productId.isEmpty) {
      throw new IllegalArgumentException(
        "Product ID must not be empty."
      )
    }

    if (referenceData.categories.isEmpty) {
      throw new IllegalArgumentException(
        "Catalog categories must not be empty."
      )
    }

    if (referenceData.brands.isEmpty) {
      throw new IllegalArgumentException(
        "Catalog brands must not be empty."
      )
    }

    val category =
      referenceData.categories(
        random.derive("category").nextInt(
          referenceData.categories.size
        )
      )

    val brand =
      referenceData.brands(
        random.derive("brand").nextInt(
          referenceData.brands.size
        )
      )

    val priceDefinition =
      pricingConfig.categories.getOrElse(
        category.id,
        throw new IllegalArgumentException(
          s"Missing product pricing configuration for category: ${category.id}"
        )
      )

    val price =
      generatePrice(
        priceDefinition.min,
        priceDefinition.max,
        priceDefinition.mode,
        random.derive("price")
      )

    Product(
      id = productId,
      name = s"${brand.name} ${category.name} Product",
      categoryId = category.id,
      brandId = brand.id,
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