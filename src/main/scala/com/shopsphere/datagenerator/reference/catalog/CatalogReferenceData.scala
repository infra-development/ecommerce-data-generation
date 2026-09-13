package com.shopsphere.datagenerator.reference.catalog

import com.fasterxml.jackson.annotation.JsonProperty

case class CatalogReferenceData(
                                 categories: Seq[CategoryReference],
                                 brands: Seq[BrandReference],
                                 productTypes: Seq[ProductTypeReference],
                                 productModels: Seq[ProductModelReference]
                               )

case class CategoryReference(
                              id: String,
                              name: String
                            )

case class BrandReference(
                           id: String,
                           name: String
                         )

case class ProductTypeReference(
                                 id: String,
                                 @JsonProperty("category_id")
                                 categoryId: String,
                                 name: String
                               )

case class ProductModelReference(
                                  id: String,
                                  @JsonProperty("category_id")
                                  categoryId: String,
                                  @JsonProperty("product_type_id")
                                  productTypeId: String,
                                  @JsonProperty("brand_id")
                                  brandId: String,
                                  name: String
                                )