package com.shopsphere.datagenerator.generation

case class GenerationPlan(
                           customerCount: Long,
                           addressCount: Long,
                           productCount: Long,
                           orderCount: Long,
                           orderItemCount: Long,
                           paymentCount: Long,
                           shipmentCount: Long,
                           returnCount: Long,
                           sessionCount: Long,
                           eventCount: Long
                         )