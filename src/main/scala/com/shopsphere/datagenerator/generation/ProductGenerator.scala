package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.RandomGenerator
import com.shopsphere.datagenerator.model.Product
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceData

object ProductGenerator {

  def generate(
                productId: String,
                referenceData: CatalogReferenceData,
                random: RandomGenerator
              ): Product = {

    if (productId.isEmpty) {
      throw new IllegalArgumentException(
        "Product ID must not be empty."
      )
    }

    val category =
      referenceData.categories(
        random.nextInt(referenceData.categories.size)
      )

    val brand =
      referenceData.brands(
        random.nextInt(referenceData.brands.size)
      )

    val price =
      generatePrice(random)

    Product(
      id = productId,
      name = s"${brand.name} ${category.name} Product",
      categoryId = category.id,
      brandId = brand.id,
      price = price
    )
  }

  private def generatePrice(
                             random: RandomGenerator
                           ): BigDecimal = {

    val rawPrice =
      random.nextDouble(100.0, 100000.0)

    BigDecimal(
      rawPrice
    ).setScale(
      2,
      BigDecimal.RoundingMode.HALF_UP
    )
  }
}