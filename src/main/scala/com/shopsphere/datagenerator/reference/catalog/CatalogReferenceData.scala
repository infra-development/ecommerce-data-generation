package com.shopsphere.datagenerator.reference.catalog

case class CatalogReferenceData(
                                 categories: Seq[CategoryReference],
                                 brands: Seq[BrandReference]
                               )

case class CategoryReference(
                              id: String,
                              name: String
                            )

case class BrandReference(
                           id: String,
                           name: String
                         )