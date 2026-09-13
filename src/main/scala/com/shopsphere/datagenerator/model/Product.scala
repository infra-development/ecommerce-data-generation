package com.shopsphere.datagenerator.model

case class Product(
                    id: String,
                    name: String,
                    categoryId: String,
                    productTypeId: String,
                    brandId: String,
                    productModelId: String,
                    price: BigDecimal
                  )