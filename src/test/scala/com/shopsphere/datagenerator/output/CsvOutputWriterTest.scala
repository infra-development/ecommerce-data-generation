package com.shopsphere.datagenerator.output

import com.shopsphere.datagenerator.generation.GeneratedData
import com.shopsphere.datagenerator.model.{Address, Brand, Category, Customer, Event, Order, OrderItem, Payment, Product, Return, Session, Shipment}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files
import java.time.LocalDateTime

class CsvOutputWriterTest extends AnyFunSuite {

  test("write should create products.csv with product type and product model columns") {

    val outputDirectory =
      Files.createTempDirectory("csv-output-writer-test")

    val product =
      Product(
        id = "PRODUCT_000000001",
        name = "Samsung Galaxy S25",
        categoryId = "CATEGORY_002",
        productTypeId = "PRODUCT_TYPE_004",
        brandId = "BRAND_001",
        productModelId = "PRODUCT_MODEL_010",
        price = BigDecimal("74999.00")
      )

    val data =
      GeneratedData(
        customers = Seq.empty[Customer],
        addresses = Seq.empty[Address],
        categories = Seq(
          Category(
            id = "CATEGORY_002",
            name = "Mobiles"
          )
        ),
        brands = Seq(
          Brand(
            id = "BRAND_001",
            name = "Samsung"
          )
        ),
        products = Seq(product),
        orders = Seq.empty[Order],
        orderItems = Seq.empty[OrderItem],
        payments = Seq.empty[Payment],
        shipments = Seq.empty[Shipment],
        returns = Seq.empty[Return],
        sessions = Seq.empty[Session],
        events = Seq.empty[Event]
      )

    val result =
      CsvOutputWriter.write(
        data = data,
        outputDirectory = outputDirectory.toString
      )

    assert(result("products") == 1L)

    val productFile =
      outputDirectory.resolve("products.csv")

    assert(Files.exists(productFile))

    val lines =
      Files.readAllLines(productFile)

    assert(lines.size() == 2)

    assert(
      lines.get(0) ==
        "id,name,category_id,product_type_id,brand_id,product_model_id,price"
    )

    assert(
      lines.get(1) ==
        "PRODUCT_000000001,Samsung Galaxy S25,CATEGORY_002,PRODUCT_TYPE_004,BRAND_001,PRODUCT_MODEL_010,74999.00"
    )
  }

  test("write should preserve product type and product model IDs for multiple products") {

    val outputDirectory =
      Files.createTempDirectory("csv-output-writer-products-test")

    val products =
      Seq(
        Product(
          id = "PRODUCT_000000001",
          name = "Samsung Galaxy S25",
          categoryId = "CATEGORY_002",
          productTypeId = "PRODUCT_TYPE_004",
          brandId = "BRAND_001",
          productModelId = "PRODUCT_MODEL_010",
          price = BigDecimal("74999.00")
        ),
        Product(
          id = "PRODUCT_000000002",
          name = "Apple iPhone 16",
          categoryId = "CATEGORY_002",
          productTypeId = "PRODUCT_TYPE_004",
          brandId = "BRAND_002",
          productModelId = "PRODUCT_MODEL_011",
          price = BigDecimal("79999.00")
        )
      )

    val data =
      GeneratedData(
        customers = Seq.empty[Customer],
        addresses = Seq.empty[Address],
        categories = Seq.empty[Category],
        brands = Seq.empty[Brand],
        products = products,
        orders = Seq.empty[Order],
        orderItems = Seq.empty[OrderItem],
        payments = Seq.empty[Payment],
        shipments = Seq.empty[Shipment],
        returns = Seq.empty[Return],
        sessions = Seq.empty[Session],
        events = Seq.empty[Event]
      )

    val result =
      CsvOutputWriter.write(
        data = data,
        outputDirectory = outputDirectory.toString
      )

    assert(result("products") == 2L)

    val productFile =
      outputDirectory.resolve("products.csv")

    val lines =
      Files.readAllLines(productFile)

    assert(lines.size() == 3)

    assert(
      lines.get(1) ==
        "PRODUCT_000000001,Samsung Galaxy S25,CATEGORY_002,PRODUCT_TYPE_004,BRAND_001,PRODUCT_MODEL_010,74999.00"
    )

    assert(
      lines.get(2) ==
        "PRODUCT_000000002,Apple iPhone 16,CATEGORY_002,PRODUCT_TYPE_004,BRAND_002,PRODUCT_MODEL_011,79999.00"
    )
  }
}