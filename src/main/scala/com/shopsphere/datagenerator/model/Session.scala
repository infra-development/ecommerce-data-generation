package com.shopsphere.datagenerator.model

import java.time.LocalDateTime

case class Session(
                    id: String,
                    customerId: String,
                    sessionStart: LocalDateTime,
                    sessionEnd: LocalDateTime,
                    deviceType: String,
                    channel: String
                  )