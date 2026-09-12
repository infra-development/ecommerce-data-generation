package com.shopsphere.datagenerator.validation

import java.nio.file.Paths

object CsvDataValidator {

  private val expectedHeaders =
    Map(
      "customers" ->
        Seq(
          "id",
          "first_name",
          "last_name",
          "email",
          "phone"
        ),

      "addresses" ->
        Seq(
          "id",
          "building_id",
          "unit_number",
          "postal_code"
        ),

      "categories" ->
        Seq(
          "id",
          "name"
        ),

      "brands" ->
        Seq(
          "id",
          "name"
        ),

      "products" ->
        Seq(
          "id",
          "name",
          "category_id",
          "brand_id",
          "price"
        ),

      "orders" ->
        Seq(
          "id",
          "customer_id",
          "order_date",
          "status",
          "total_amount"
        ),

      "order_items" ->
        Seq(
          "id",
          "order_id",
          "product_id",
          "quantity",
          "unit_price",
          "line_amount"
        ),

      "payments" ->
        Seq(
          "id",
          "order_id",
          "payment_method",
          "payment_status",
          "amount",
          "payment_date"
        ),

      "shipments" ->
        Seq(
          "id",
          "order_id",
          "shipment_status",
          "shipped_date",
          "delivered_date"
        ),

      "returns" ->
        Seq(
          "id",
          "order_id",
          "return_reason",
          "return_status",
          "return_date"
        ),

      "sessions" ->
        Seq(
          "id",
          "customer_id",
          "session_start",
          "session_end",
          "device_type",
          "channel"
        ),

      "events" ->
        Seq(
          "id",
          "session_id",
          "customer_id",
          "event_type",
          "event_timestamp",
          "product_id"
        )
    )

  def validate(
                outputDirectory: String
              ): ValidationResult = {

    val tables =
      expectedHeaders.keys.map { entity =>

        val path =
          Paths.get(
            outputDirectory,
            s"$entity.csv"
          )

        entity ->
          CsvFileReader.read(path)
      }.toMap

    var result =
      ValidationResult(
        issues = Seq.empty
      )

    result =
      validateHeaders(
        tables,
        result
      )

    result =
      validatePrimaryKeys(
        tables,
        result
      )

    result =
      validateForeignKeys(
        tables,
        result
      )

    result =
      validateBusinessRules(
        tables,
        result
      )

    result
  }

  private def validateHeaders(
                               tables: Map[String, CsvTable],
                               result: ValidationResult
                             ): ValidationResult = {

    var current =
      result

    expectedHeaders.foreach {
      case (entity, expected) =>

        val actual =
          tables(entity).header

        if (actual != expected) {

          current =
            current.add(
              category = "STRUCTURAL",
              entity = entity,
              message =
                s"Expected header ${expected.mkString("[", ", ", "]")} " +
                  s"but found ${actual.mkString("[", ", ", "]")}"
            )
        }
    }

    current
  }

  private def validatePrimaryKeys(
                                   tables: Map[String, CsvTable],
                                   result: ValidationResult
                                 ): ValidationResult = {

    var current =
      result

    expectedHeaders.keys.foreach { entity =>

      val ids =
        tables(entity).rows.map(_("id"))

      val duplicates =
        ids
          .groupBy(identity)
          .collect {
            case (id, values)
              if values.size > 1 =>
              id
          }

      if (duplicates.nonEmpty) {

        current =
          current.add(
            category = "PRIMARY_KEY",
            entity = entity,
            message =
              s"Duplicate IDs found: ${duplicates.take(10).mkString(", ")}"
          )
      }

      val emptyIds =
        ids.count(_.trim.isEmpty)

      if (emptyIds > 0) {

        current =
          current.add(
            category = "PRIMARY_KEY",
            entity = entity,
            message =
              s"$emptyIds records have empty IDs."
          )
      }
    }

    current
  }

  private def validateForeignKeys(
                                   tables: Map[String, CsvTable],
                                   result: ValidationResult
                                 ): ValidationResult = {

    var current =
      result

    current =
      validateForeignKey(
        tables,
        current,
        "orders",
        "customer_id",
        "customers"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "order_items",
        "order_id",
        "orders"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "order_items",
        "product_id",
        "products"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "payments",
        "order_id",
        "orders"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "shipments",
        "order_id",
        "orders"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "returns",
        "order_id",
        "orders"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "sessions",
        "customer_id",
        "customers"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "events",
        "session_id",
        "sessions"
      )

    current =
      validateForeignKey(
        tables,
        current,
        "events",
        "customer_id",
        "customers"
      )

    current =
      validateOptionalForeignKey(
        tables,
        current,
        "events",
        "product_id",
        "products"
      )

    /*
     * Customer → Address is intentionally not validated yet.
     *
     * addresses.csv currently has no customer_id column.
     *
     * This is a known data-model gap that we will address separately.
     */

    current
  }

  private def validateForeignKey(
                                  tables: Map[String, CsvTable],
                                  result: ValidationResult,
                                  childEntity: String,
                                  foreignKey: String,
                                  parentEntity: String
                                ): ValidationResult = {

    val parentIds =
      tables(parentEntity)
        .rows
        .map(_("id"))
        .toSet

    val invalidValues =
      tables(childEntity)
        .rows
        .map(_(foreignKey))
        .filterNot(parentIds.contains)
        .distinct

    if (invalidValues.nonEmpty) {

      result.add(
        category = "FOREIGN_KEY",
        entity = childEntity,
        message =
          s"$foreignKey contains ${invalidValues.size} invalid references " +
            s"to $parentEntity."
      )

    } else {
      result
    }
  }

  private def validateOptionalForeignKey(
                                          tables: Map[String, CsvTable],
                                          result: ValidationResult,
                                          childEntity: String,
                                          foreignKey: String,
                                          parentEntity: String
                                        ): ValidationResult = {

    val parentIds =
      tables(parentEntity)
        .rows
        .map(_("id"))
        .toSet

    val invalidValues =
      tables(childEntity)
        .rows
        .map(_(foreignKey))
        .filter { value =>
          value.nonEmpty &&
            !parentIds.contains(value)
        }
        .distinct

    if (invalidValues.nonEmpty) {

      result.add(
        category = "FOREIGN_KEY",
        entity = childEntity,
        message =
          s"$foreignKey contains ${invalidValues.size} invalid references " +
            s"to $parentEntity."
      )

    } else {
      result
    }
  }

  private def validateBusinessRules(
                                     tables: Map[String, CsvTable],
                                     result: ValidationResult
                                   ): ValidationResult = {

    var current =
      result

    current =
      validateOrderTotals(
        tables,
        current
      )

    current =
      validateOrderItemAmounts(
        tables,
        current
      )

    current =
      validateShipmentDates(
        tables,
        current
      )

    current =
      validateEventTimestamps(
        tables,
        current
      )

    current =
      validateReturnDates(
        tables,
        current
      )

    current
  }

  private def validateOrderTotals(
                                   tables: Map[String, CsvTable],
                                   result: ValidationResult
                                 ): ValidationResult = {

    val itemTotals =
      tables("order_items")
        .rows
        .groupBy(_("order_id"))
        .view
        .mapValues { items =>
          items
            .map(_("line_amount"))
            .map(BigDecimal(_))
            .sum
        }
        .toMap

    val invalidOrders =
      tables("orders")
        .rows
        .filter { order =>

          val expected =
            itemTotals.getOrElse(
              order("id"),
              BigDecimal(0)
            )

          val actual =
            BigDecimal(
              order("total_amount")
            )

          expected != actual
        }

    if (invalidOrders.nonEmpty) {

      result.add(
        category = "BUSINESS_RULE",
        entity = "orders",
        message =
          s"${invalidOrders.size} orders have totals that do not match " +
            "the sum of their order-item line amounts."
      )

    } else {
      result
    }
  }

  private def validateOrderItemAmounts(
                                        tables: Map[String, CsvTable],
                                        result: ValidationResult
                                      ): ValidationResult = {

    val invalidItems =
      tables("order_items")
        .rows
        .filter { item =>

          val quantity =
            item("quantity").toInt

          val unitPrice =
            BigDecimal(
              item("unit_price")
            )

          val expected =
            (
              unitPrice *
                quantity
              ).setScale(
              2,
              BigDecimal.RoundingMode.HALF_UP
            )

          val actual =
            BigDecimal(
              item("line_amount")
            )

          expected != actual
        }

    if (invalidItems.nonEmpty) {

      result.add(
        category = "BUSINESS_RULE",
        entity = "order_items",
        message =
          s"${invalidItems.size} order items have invalid line amounts."
      )

    } else {
      result
    }
  }

  private def validateShipmentDates(
                                     tables: Map[String, CsvTable],
                                     result: ValidationResult
                                   ): ValidationResult = {

    val invalidShipments =
      tables("shipments")
        .rows
        .filter { shipment =>

          val shipped =
            shipment("shipped_date")

          val delivered =
            shipment("delivered_date")

          val deliveredBeforeShipped =
            shipped.nonEmpty &&
              delivered.nonEmpty &&
              java.time.LocalDateTime
                .parse(delivered)
                .isBefore(
                  java.time.LocalDateTime.parse(shipped)
                )

          deliveredBeforeShipped
        }

    if (invalidShipments.nonEmpty) {

      result.add(
        category = "BUSINESS_RULE",
        entity = "shipments",
        message =
          s"${invalidShipments.size} shipments have a delivered date " +
            "before their shipped date."
      )

    } else {
      result
    }
  }

  private def validateEventTimestamps(
                                       tables: Map[String, CsvTable],
                                       result: ValidationResult
                                     ): ValidationResult = {

    val sessions =
      tables("sessions")
        .rows
        .map { session =>
          session("id") ->
            (
              java.time.LocalDateTime.parse(
                session("session_start")
              ),
              java.time.LocalDateTime.parse(
                session("session_end")
              )
            )
        }
        .toMap

    val invalidEvents =
      tables("events")
        .rows
        .filter { event =>

          val timestamp =
            java.time.LocalDateTime.parse(
              event("event_timestamp")
            )

          sessions
            .get(event("session_id"))
            .exists {
              case (start, end) =>
                timestamp.isBefore(start) ||
                  timestamp.isAfter(end)
            }
        }

    if (invalidEvents.nonEmpty) {

      result.add(
        category = "BUSINESS_RULE",
        entity = "events",
        message =
          s"${invalidEvents.size} events have timestamps outside " +
            "their session boundaries."
      )

    } else {
      result
    }
  }

  private def validateReturnDates(
                                   tables: Map[String, CsvTable],
                                   result: ValidationResult
                                 ): ValidationResult = {

    val deliveredDates =
      tables("shipments")
        .rows
        .flatMap { shipment =>

          shipment("delivered_date") match {
            case value if value.nonEmpty =>
              Some(
                shipment("order_id") ->
                  java.time.LocalDateTime.parse(value)
              )

            case _ =>
              None
          }
        }
        .toMap

    val invalidReturns =
      tables("returns")
        .rows
        .filter { record =>

          deliveredDates
            .get(record("order_id"))
            .exists { deliveredDate =>

              val returnDate =
                java.time.LocalDateTime.parse(
                  record("return_date")
                )

              returnDate.isBefore(
                deliveredDate
              )
            }
        }

    if (invalidReturns.nonEmpty) {

      result.add(
        category = "BUSINESS_RULE",
        entity = "returns",
        message =
          s"${invalidReturns.size} returns have a return date " +
            "before delivery."
      )

    } else {
      result
    }
  }
}