package com.shopsphere.datagenerator.model

import java.time.LocalDate

case class Customer(
                     id: String,
                     firstName: String,
                     lastName: String,
                     email: String,
                     phone: String,
                     gender: String,
                     dateOfBirth: LocalDate,
                     registrationDate: LocalDate,
                     customerStatus: String,
                     customerSegment: String,
                     acquisitionChannel: String,
                     acquisitionCampaign: String,
                     preferredDevice: String,
                     preferredPaymentMethod: String
                   )