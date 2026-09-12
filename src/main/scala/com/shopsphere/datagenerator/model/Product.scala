package com.shopsphere.datagenerator.model

case class Product(
                    id: String,
                    name: String,
                    categoryId: String,
                    brandId: String,
                    price: BigDecimal
                  )