package com.shopsphere.datagenerator.relationship

import com.shopsphere.datagenerator.common.distribution.DistributionEngine
import com.shopsphere.datagenerator.common.random.RandomGenerator
import com.shopsphere.datagenerator.generation.{
  OrderGenerator,
  OrderItemGenerator,
  PaymentGenerator,
  ReturnGenerator,
  ShipmentGenerator
}
import com.shopsphere.datagenerator.model.Product

object OrderRelationshipGenerator {

  def generate(
                orderId: String,
                customerId: String,
                products: Seq[Product],
                itemCount: Int,
                random: RandomGenerator,
                distributionEngine: DistributionEngine
              ): OrderGenerationResult = {

    if (products.isEmpty) {
      throw new IllegalArgumentException(
        "Products must not be empty."
      )
    }

    if (itemCount <= 0) {
      throw new IllegalArgumentException(
        "Item count must be greater than zero."
      )
    }

    /*
     * Generate the base order.
     *
     * OrderGenerator is responsible for selecting the
     * order status through the DistributionEngine.
     */
    val baseOrder =
      OrderGenerator.generate(
        orderId = orderId,
        customerId = customerId,
        random = random.derive("order"),
        distributionEngine = distributionEngine
      )

    /*
     * Lifecycle generation receives the already-selected
     * order status. It must not independently select another
     * status.
     */
    val lifecycle =
      OrderLifecycleGenerator.generate(
        orderDate = baseOrder.orderDate,
        orderStatus = baseOrder.status,
        random = random.derive("lifecycle")
      )

    val orderItems =
      (1 to itemCount).map { itemNumber =>

        val product =
          products(
            random
              .derive(
                s"product-$itemNumber"
              )
              .nextInt(products.size)
          )

        OrderItemGenerator.generate(
          orderItemId =
            f"${baseOrder.id}_ITEM_$itemNumber%03d",
          orderId = baseOrder.id,
          product = product,
          random =
            random.derive(
              s"order-item-$itemNumber"
            )
        )
      }

    val totalAmount =
      orderItems
        .map(_.lineAmount)
        .sum
        .setScale(
          2,
          BigDecimal.RoundingMode.HALF_UP
        )

    /*
     * Lifecycle status should be the same status selected
     * by OrderGenerator.
     *
     * The lifecycle generator returns the same value, so this
     * copy currently mainly establishes the completed order
     * with its calculated total.
     */
    val completedOrder =
      baseOrder.copy(
        status = lifecycle.orderStatus,
        totalAmount = totalAmount
      )

    val generatedPayment =
      PaymentGenerator.generate(
        paymentId =
          s"${completedOrder.id}_PAYMENT",
        orderId = completedOrder.id,
        amount = completedOrder.totalAmount,
        random =
          random.derive("payment"),
        distributionEngine =
          distributionEngine
      )

    val payment =
      generatedPayment.copy(
        paymentStatus =
          lifecycle.paymentStatus,
        paymentDate =
          lifecycle.paymentDate
      )

    val generatedShipment =
      ShipmentGenerator.generate(
        shipmentId =
          s"${completedOrder.id}_SHIPMENT",
        orderId = completedOrder.id,
        random =
          random.derive("shipment")
      )

    val shipment =
      generatedShipment.copy(
        shipmentStatus =
          lifecycle.shipmentStatus,
        shippedDate =
          lifecycle.shippedDate,
        deliveredDate =
          lifecycle.deliveredDate
      )

    val returnRecord =
      lifecycle.returnStatus.map { status =>

        val generatedReturn =
          ReturnGenerator.generate(
            returnId =
              s"${completedOrder.id}_RETURN",
            orderId = completedOrder.id,
            random =
              random.derive("return")
          )

        generatedReturn.copy(
          returnStatus = status,
          returnDate =
            lifecycle.returnDate.get
        )
      }

    OrderGenerationResult(
      order = completedOrder,
      orderItems = orderItems,
      payment = payment,
      shipment = shipment,
      returnRecord = returnRecord
    )
  }
}