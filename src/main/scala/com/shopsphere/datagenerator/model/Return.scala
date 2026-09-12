package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Return(
                   id: String,
                   orderId: String,
                   returnReason: String,
                   returnStatus: String,
                   returnDate: LocalDateTime
                 )