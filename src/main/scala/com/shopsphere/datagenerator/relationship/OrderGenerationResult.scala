package com.shopsphere.datagenerator.relationship

import com.shopsphere.datagenerator.model.{
  Order,
  OrderItem,
  Payment,
  Shipment,
  Return
}

case class OrderGenerationResult(
                                  order: Order,
                                  orderItems: Seq[OrderItem],
                                  payment: Payment,
                                  shipment: Shipment,
                                  returnRecord: Option[Return]
                                )