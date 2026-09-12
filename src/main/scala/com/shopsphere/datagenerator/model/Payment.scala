package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Payment(
                    id: String,
                    orderId: String,
                    paymentMethod: String,
                    paymentStatus: String,
                    amount: BigDecimal,
                    paymentDate: LocalDateTime
                  )