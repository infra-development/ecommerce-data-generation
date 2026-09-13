package com.shopsphere.datagenerator.reference.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.nio.file.{Files, Path}

object CatalogReferenceLoader {

  def load(basePath: Path): CatalogReferenceData = {
    require(
      Files.exists(basePath),
      s"Catalog reference path does not exist: $basePath"
    )

    require(
      Files.isDirectory(basePath),
      s"Catalog reference path is not a directory: $basePath"
    )

    val mapper =
      new ObjectMapper()
        .registerModule(DefaultScalaModule)

    val categories =
      loadCategories(
        mapper,
        basePath.resolve("categories.json")
      )

    val brands =
      loadBrands(
        mapper,
        basePath.resolve("brands.json")
      )

    val productTypes =
      loadProductTypes(
        mapper,
        basePath.resolve("product_types.json")
      )

    val productModels =
      loadProductModels(
        mapper,
        basePath.resolve("product_models.json")
      )

    validate(
      categories = categories,
      brands = brands,
      productTypes = productTypes,
      productModels = productModels
    )

    CatalogReferenceData(
      categories = categories,
      brands = brands,
      productTypes = productTypes,
      productModels = productModels
    )
  }

  private def loadCategories(
                              mapper: ObjectMapper,
                              path: Path
                            ): Seq[CategoryReference] = {
    requireFile(path)

    mapper
      .readValue(
        path.toFile,
        classOf[Array[CategoryReference]]
      )
      .toSeq
  }

  private def loadBrands(
                          mapper: ObjectMapper,
                          path: Path
                        ): Seq[BrandReference] = {
    requireFile(path)

    mapper
      .readValue(
        path.toFile,
        classOf[Array[BrandReference]]
      )
      .toSeq
  }

  private def loadProductTypes(
                                mapper: ObjectMapper,
                                path: Path
                              ): Seq[ProductTypeReference] = {
    requireFile(path)

    mapper
      .readValue(
        path.toFile,
        classOf[Array[ProductTypeReference]]
      )
      .toSeq
  }

  private def loadProductModels(
                                 mapper: ObjectMapper,
                                 path: Path
                               ): Seq[ProductModelReference] = {
    requireFile(path)

    mapper
      .readValue(
        path.toFile,
        classOf[Array[ProductModelReference]]
      )
      .toSeq
  }

  private def requireFile(path: Path): Unit = {
    require(
      Files.exists(path),
      s"Catalog reference file does not exist: $path"
    )

    require(
      Files.isRegularFile(path),
      s"Catalog reference path is not a file: $path"
    )
  }

  private def validate(
                        categories: Seq[CategoryReference],
                        brands: Seq[BrandReference],
                        productTypes: Seq[ProductTypeReference],
                        productModels: Seq[ProductModelReference]
                      ): Unit = {

    require(
      categories.nonEmpty,
      "Catalog categories must not be empty."
    )

    require(
      brands.nonEmpty,
      "Catalog brands must not be empty."
    )

    require(
      productTypes.nonEmpty,
      "Catalog product types must not be empty."
    )

    require(
      productModels.nonEmpty,
      "Catalog product models must not be empty."
    )

    validateUniqueIds(
      "category",
      categories.map(_.id)
    )

    validateUniqueIds(
      "brand",
      brands.map(_.id)
    )

    validateUniqueIds(
      "product type",
      productTypes.map(_.id)
    )

    validateUniqueIds(
      "product model",
      productModels.map(_.id)
    )

    categories.foreach { category =>
      require(
        category.id.nonEmpty,
        "Catalog category ID must not be empty."
      )

      require(
        category.name.nonEmpty,
        s"Catalog category name must not be empty: ${category.id}"
      )
    }

    brands.foreach { brand =>
      require(
        brand.id.nonEmpty,
        "Catalog brand ID must not be empty."
      )

      require(
        brand.name.nonEmpty,
        s"Catalog brand name must not be empty: ${brand.id}"
      )
    }

    val categoryIds =
      categories.map(_.id).toSet

    val brandIds =
      brands.map(_.id).toSet

    val productTypeById =
      productTypes.map(productType => productType.id -> productType).toMap

    val productTypeIds =
      productTypes.map(_.id).toSet

    productTypes.foreach { productType =>
      require(
        productType.id.nonEmpty,
        "Catalog product type ID must not be empty."
      )

      require(
        productType.name.nonEmpty,
        s"Catalog product type name must not be empty: ${productType.id}"
      )

      require(
        categoryIds.contains(productType.categoryId),
        s"Catalog product type '${productType.id}' references unknown category: ${productType.categoryId}"
      )
    }

    productModels.foreach { productModel =>
      require(
        productModel.id.nonEmpty,
        "Catalog product model ID must not be empty."
      )

      require(
        productModel.name.nonEmpty,
        s"Catalog product model name must not be empty: ${productModel.id}"
      )

      require(
        categoryIds.contains(productModel.categoryId),
        s"Catalog product model '${productModel.id}' references unknown category: ${productModel.categoryId}"
      )

      require(
        productTypeIds.contains(productModel.productTypeId),
        s"Catalog product model '${productModel.id}' references unknown product type: ${productModel.productTypeId}"
      )

      require(
        brandIds.contains(productModel.brandId),
        s"Catalog product model '${productModel.id}' references unknown brand: ${productModel.brandId}"
      )

      val productType =
        productTypeById(productModel.productTypeId)

      require(
        productType.categoryId == productModel.categoryId,
        s"Catalog product model '${productModel.id}' has category '${productModel.categoryId}' but product type '${productModel.productTypeId}' belongs to category '${productType.categoryId}'"
      )
    }

    productTypes.foreach { productType =>
      val models =
        productModels.filter(
          _.productTypeId == productType.id
        )

      require(
        models.nonEmpty,
        s"No product models configured for product type: ${productType.id}"
      )
    }
  }

  private def validateUniqueIds(
                                 entityName: String,
                                 ids: Seq[String]
                               ): Unit = {

    val emptyIds =
      ids.filter(_.isEmpty)

    require(
      emptyIds.isEmpty,
      s"Catalog $entityName IDs must not be empty."
    )

    val duplicates =
      ids
        .groupBy(identity)
        .collect {
          case (id, occurrences) if occurrences.size > 1 =>
            id
        }
        .toSeq
        .sorted

    require(
      duplicates.isEmpty,
      s"Duplicate catalog $entityName IDs: ${duplicates.mkString(", ")}"
    )
  }
}