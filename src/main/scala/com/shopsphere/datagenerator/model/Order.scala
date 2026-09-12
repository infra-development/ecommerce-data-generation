package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Order(
                  id: String,
                  customerId: String,
                  orderDate: LocalDateTime,
                  status: String,
                  totalAmount: BigDecimal
                )