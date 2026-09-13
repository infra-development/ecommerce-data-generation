package com.shopsphere.datagenerator.output

import com.shopsphere.datagenerator.generation.GeneratedData

import java.nio.file.Paths

object CsvOutputWriter {

  def write(
             data: GeneratedData,
             outputDirectory: String
           ): Map[String, Long] = {

    val outputPath =
      Paths.get(outputDirectory)

    Map(
      "customers" ->
        writeCustomers(data, outputPath),

      "addresses" ->
        writeAddresses(data, outputPath),

      "categories" ->
        writeCategories(data, outputPath),

      "brands" ->
        writeBrands(data, outputPath),

      "products" ->
        writeProducts(data, outputPath),

      "orders" ->
        writeOrders(data, outputPath),

      "order_items" ->
        writeOrderItems(data, outputPath),

      "payments" ->
        writePayments(data, outputPath),

      "shipments" ->
        writeShipments(data, outputPath),

      "returns" ->
        writeReturns(data, outputPath),

      "sessions" ->
        writeSessions(data, outputPath),

      "events" ->
        writeEvents(data, outputPath)
    )
  }

  private def writeCustomers(
                              data: GeneratedData,
                              outputPath: java.nio.file.Path
                            ): Long = {

    CsvWriter.write(
      outputPath.resolve("customers.csv"),
      Seq(
        "id",
        "first_name",
        "last_name",
        "email",
        "phone"
      ),
      data.customers.iterator.map { customer =>
        Seq(
          customer.id,
          customer.firstName,
          customer.lastName,
          customer.email,
          customer.phone
        )
      }
    )
  }

  private def writeAddresses(
                              data: GeneratedData,
                              outputPath: java.nio.file.Path
                            ): Long = {

    CsvWriter.write(
      outputPath.resolve("addresses.csv"),
      Seq(
        "id",
        "customer_id",
        "building_id",
        "unit_number",
        "postal_code"
      ),
      data.addresses.iterator.map { address =>
        Seq(
          address.id,
          address.customerId,
          address.buildingId,
          address.unitNumber,
          address.postalCode
        )
      }
    )
  }

  private def writeCategories(
                               data: GeneratedData,
                               outputPath: java.nio.file.Path
                             ): Long = {

    CsvWriter.write(
      outputPath.resolve("categories.csv"),
      Seq(
        "id",
        "name"
      ),
      data.categories.iterator.map { category =>
        Seq(
          category.id,
          category.name
        )
      }
    )
  }

  private def writeBrands(
                           data: GeneratedData,
                           outputPath: java.nio.file.Path
                         ): Long = {

    CsvWriter.write(
      outputPath.resolve("brands.csv"),
      Seq(
        "id",
        "name"
      ),
      data.brands.iterator.map { brand =>
        Seq(
          brand.id,
          brand.name
        )
      }
    )
  }

  private def writeProducts(
                             data: GeneratedData,
                             outputPath: java.nio.file.Path
                           ): Long = {

    CsvWriter.write(
      outputPath.resolve("products.csv"),
      Seq(
        "id",
        "name",
        "category_id",
        "brand_id",
        "price"
      ),
      data.products.iterator.map { product =>
        Seq(
          product.id,
          product.name,
          product.categoryId,
          product.brandId,
          product.price.toString()
        )
      }
    )
  }

  private def writeOrders(
                           data: GeneratedData,
                           outputPath: java.nio.file.Path
                         ): Long = {

    CsvWriter.write(
      outputPath.resolve("orders.csv"),
      Seq(
        "id",
        "customer_id",
        "order_date",
        "status",
        "total_amount"
      ),
      data.orders.iterator.map { order =>
        Seq(
          order.id,
          order.customerId,
          order.orderDate.toString,
          order.status,
          order.totalAmount.toString()
        )
      }
    )
  }

  private def writeOrderItems(
                               data: GeneratedData,
                               outputPath: java.nio.file.Path
                             ): Long = {

    CsvWriter.write(
      outputPath.resolve("order_items.csv"),
      Seq(
        "id",
        "order_id",
        "product_id",
        "quantity",
        "unit_price",
        "line_amount"
      ),
      data.orderItems.iterator.map { item =>
        Seq(
          item.id,
          item.orderId,
          item.productId,
          item.quantity.toString,
          item.unitPrice.toString(),
          item.lineAmount.toString()
        )
      }
    )
  }

  private def writePayments(
                             data: GeneratedData,
                             outputPath: java.nio.file.Path
                           ): Long = {

    CsvWriter.write(
      outputPath.resolve("payments.csv"),
      Seq(
        "id",
        "order_id",
        "payment_method",
        "payment_status",
        "amount",
        "payment_date"
      ),
      data.payments.iterator.map { payment =>
        Seq(
          payment.id,
          payment.orderId,
          payment.paymentMethod,
          payment.paymentStatus,
          payment.amount.toString(),
          payment.paymentDate.toString
        )
      }
    )
  }

  private def writeShipments(
                              data: GeneratedData,
                              outputPath: java.nio.file.Path
                            ): Long = {

    CsvWriter.write(
      outputPath.resolve("shipments.csv"),
      Seq(
        "id",
        "order_id",
        "shipment_status",
        "shipped_date",
        "delivered_date"
      ),
      data.shipments.iterator.map { shipment =>
        Seq(
          shipment.id,
          shipment.orderId,
          shipment.shipmentStatus,
          shipment.shippedDate.map(_.toString).getOrElse(""),
          shipment.deliveredDate.map(_.toString).getOrElse("")
        )
      }
    )
  }

  private def writeReturns(
                            data: GeneratedData,
                            outputPath: java.nio.file.Path
                          ): Long = {

    CsvWriter.write(
      outputPath.resolve("returns.csv"),
      Seq(
        "id",
        "order_id",
        "return_reason",
        "return_status",
        "return_date"
      ),
      data.returns.iterator.map { record =>
        Seq(
          record.id,
          record.orderId,
          record.returnReason,
          record.returnStatus,
          record.returnDate.toString
        )
      }
    )
  }

  private def writeSessions(
                             data: GeneratedData,
                             outputPath: java.nio.file.Path
                           ): Long = {

    CsvWriter.write(
      outputPath.resolve("sessions.csv"),
      Seq(
        "id",
        "customer_id",
        "session_start",
        "session_end",
        "device_type",
        "channel"
      ),
      data.sessions.iterator.map { session =>
        Seq(
          session.id,
          session.customerId,
          session.sessionStart.toString,
          session.sessionEnd.toString,
          session.deviceType,
          session.channel
        )
      }
    )
  }

  private def writeEvents(
                           data: GeneratedData,
                           outputPath: java.nio.file.Path
                         ): Long = {

    CsvWriter.write(
      outputPath.resolve("events.csv"),
      Seq(
        "id",
        "session_id",
        "customer_id",
        "event_type",
        "event_timestamp",
        "product_id"
      ),
      data.events.iterator.map { event =>
        Seq(
          event.id,
          event.sessionId,
          event.customerId,
          event.eventType,
          event.eventTimestamp.toString,
          event.productId.getOrElse("")
        )
      }
    )
  }
}