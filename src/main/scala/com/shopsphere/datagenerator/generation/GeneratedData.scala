package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.model.{
  Address,
  Brand,
  Category,
  Customer,
  Event,
  Order,
  OrderItem,
  Payment,
  Product,
  Return,
  Session,
  Shipment
}

case class GeneratedData(
                          customers: Seq[Customer],
                          addresses: Seq[Address],
                          categories: Seq[Category],
                          brands: Seq[Brand],
                          products: Seq[Product],
                          orders: Seq[Order],
                          orderItems: Seq[OrderItem],
                          payments: Seq[Payment],
                          shipments: Seq[Shipment],
                          returns: Seq[Return],
                          sessions: Seq[Session],
                          events: Seq[Event]
                        )