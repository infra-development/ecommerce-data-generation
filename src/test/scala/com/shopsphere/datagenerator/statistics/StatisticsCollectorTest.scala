package com.shopsphere.datagenerator.statistics

import com.shopsphere.datagenerator.generation.GeneratedData
import com.shopsphere.datagenerator.model._
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDateTime

class StatisticsCollectorTest
  extends AnyFunSuite {

  private val date =
    LocalDateTime.of(
      2026,
      1,
      1,
      12,
      0
    )

  private val data =
    GeneratedData(
      customers =
        Seq(
          Customer(
            "C1",
            "John",
            "Smith",
            "john@example.com",
            "900000001"
          ),
          Customer(
            "C2",
            "Jane",
            "Doe",
            "jane@example.com",
            "900000002"
          )
        ),

      addresses = Seq.empty,

      categories =
        Seq(
          Category("CAT1", "Electronics")
        ),

      brands =
        Seq(
          Brand("BR1", "Brand A")
        ),

      products =
        Seq(
          Product(
            "P1",
            "Product 1",
            "CAT1",
            "BR1",
            BigDecimal("100.00")
          )
        ),

      orders =
        Seq(
          Order(
            "O1",
            "C1",
            date,
            "DELIVERED",
            BigDecimal("200.00")
          ),
          Order(
            "O2",
            "C2",
            date,
            "PLACED",
            BigDecimal("100.00")
          )
        ),

      orderItems =
        Seq(
          OrderItem(
            "OI1",
            "O1",
            "P1",
            1,
            BigDecimal("100.00"),
            BigDecimal("100.00")
          ),
          OrderItem(
            "OI2",
            "O1",
            "P1",
            1,
            BigDecimal("100.00"),
            BigDecimal("100.00")
          ),
          OrderItem(
            "OI3",
            "O2",
            "P1",
            1,
            BigDecimal("100.00"),
            BigDecimal("100.00")
          )
        ),

      payments =
        Seq(
          Payment(
            "PAY1",
            "O1",
            "UPI",
            "SUCCESS",
            BigDecimal("200.00"),
            date
          ),
          Payment(
            "PAY2",
            "O2",
            "CREDIT_CARD",
            "SUCCESS",
            BigDecimal("100.00"),
            date
          )
        ),

      shipments =
        Seq(
          Shipment(
            "S1",
            "O1",
            "DELIVERED",
            Some(date),
            Some(date.plusDays(1))
          ),
          Shipment(
            "S2",
            "O2",
            "PROCESSING",
            None,
            None
          )
        ),

      returns =
        Seq(
          Return(
            "R1",
            "O1",
            "DAMAGED",
            "REFUNDED",
            date.plusDays(2)
          )
        ),

      sessions =
        Seq(
          Session(
            "S01",
            "C1",
            date,
            date.plusHours(1),
            "MOBILE",
            "ORGANIC"
          ),
          Session(
            "S02",
            "C2",
            date,
            date.plusHours(1),
            "DESKTOP",
            "PAID_SEARCH"
          )
        ),

      events =
        Seq(
          Event(
            "E1",
            "S01",
            "C1",
            "PRODUCT_VIEW",
            date,
            Some("P1")
          ),
          Event(
            "E2",
            "S01",
            "C1",
            "ADD_TO_CART",
            date.plusMinutes(1),
            Some("P1")
          ),
          Event(
            "E3",
            "S02",
            "C2",
            "PAGE_VIEW",
            date,
            None
          )
        )
    )

  test("should calculate record counts") {

    val statistics =
      StatisticsCollector.collect(data)

    assert(
      statistics.recordCounts("customers") == 2
    )

    assert(
      statistics.recordCounts("orders") == 2
    )

    assert(
      statistics.recordCounts("order_items") == 3
    )

    assert(
      statistics.recordCounts("events") == 3
    )
  }

  test("should calculate cardinality averages") {

    val statistics =
      StatisticsCollector.collect(data)

    assert(
      statistics.averages("items_per_order") ==
        1.5
    )

    assert(
      statistics.averages("sessions_per_customer") ==
        1.0
    )

    assert(
      statistics.averages("events_per_session") ==
        1.5
    )
  }

  test("should calculate order status distribution") {

    val statistics =
      StatisticsCollector.collect(data)

    assert(
      statistics.distributions("order_status") ==
        Map(
          "DELIVERED" -> 1L,
          "PLACED" -> 1L
        )
    )
  }

  test("should calculate payment method distribution") {

    val statistics =
      StatisticsCollector.collect(data)

    assert(
      statistics.distributions("payment_method") ==
        Map(
          "UPI" -> 1L,
          "CREDIT_CARD" -> 1L
        )
    )
  }

  test("should calculate financial statistics") {

    val statistics =
      StatisticsCollector.collect(data)

    assert(
      statistics.financial.totalOrderValue ==
        BigDecimal("300.00")
    )

    assert(
      statistics.financial.averageOrderValue ==
        BigDecimal("150.00")
    )

    assert(
      statistics.financial.minimumOrderValue ==
        BigDecimal("100.00")
    )

    assert(
      statistics.financial.maximumOrderValue ==
        BigDecimal("200.00")
    )
  }
}