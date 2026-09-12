package com.shopsphere.datagenerator.model

case class OrderItem(
                      id: String,
                      orderId: String,
                      productId: String,
                      quantity: Int,
                      unitPrice: BigDecimal,
                      lineAmount: BigDecimal
                    )