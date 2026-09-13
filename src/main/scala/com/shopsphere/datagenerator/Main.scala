package com.shopsphere.datagenerator

import com.shopsphere.datagenerator.config.{
  ConfigLoader,
  ConfigValidator
}

import com.shopsphere.datagenerator.generation.{
  GenerationContextFactory,
  GenerationPipeline,
  GenerationPlanBuilder
}

import com.shopsphere.datagenerator.output.CsvOutputWriter

import com.shopsphere.datagenerator.validation.CsvDataValidator
import com.shopsphere.datagenerator.statistics.StatisticsCollector

object Main {

  def main(args: Array[String]): Unit = {

    val startTime =
      System.nanoTime()

    println("==========================================")
    println("ShopSphere E-Commerce Data Generator")
    println("==========================================")

    // --------------------------------------------------
    // 1. Load and validate configuration
    // --------------------------------------------------

    println()
    println("Loading configuration...")

    val config =
      ConfigLoader.load()

    ConfigValidator.validate(config)

    println(
      "Configuration validated successfully."
    )

    // --------------------------------------------------
    // 2. Create generation context
    // --------------------------------------------------

    val context =
      GenerationContextFactory.create(config)

    println(
      "Generation context created."
    )

    // --------------------------------------------------
    // 3. Build generation plan
    // --------------------------------------------------

    val plan =
      GenerationPlanBuilder.build(config)

    println()
    println("Generation plan:")
    println(
      s"  Customers:  ${plan.customerCount}"
    )
    println(
      s"  Addresses:  ${plan.addressCount}"
    )
    println(
      s"  Products:   ${plan.productCount}"
    )
    println(
      s"  Orders:     ${plan.orderCount}"
    )
    println(
      s"  OrderItems: ${plan.orderItemCount}"
    )
    println(
      s"  Payments:   ${plan.paymentCount}"
    )
    println(
      s"  Shipments:  ${plan.shipmentCount}"
    )
    println(
      s"  Returns:    ${plan.returnCount}"
    )
    println(
      s"  Sessions:   ${plan.sessionCount}"
    )
    println(
      s"  Events:     ${plan.eventCount}"
    )

    // --------------------------------------------------
    // 4. Generate data
    // --------------------------------------------------

    println()
    println("Starting generation...")

    val generatedData =
      GenerationPipeline.generate(
        context = context,
        plan = plan
      )

    println()
    println("Data generation completed.")

    // --------------------------------------------------
    // 5. Write CSV output
    // --------------------------------------------------

    println()
    println("Writing CSV output...")

    val outputCounts =
      CsvOutputWriter.write(
        data = generatedData,
        outputDirectory =
          config.output.directory
      )

    println(
      "CSV output completed."
    )

    println()
    println("Written records:")

    outputCounts.toSeq
      .sortBy(_._1)
      .foreach {
        case (entity, count) =>
          println(
            f"  $entity%-12s $count%,d"
          )
      }

    // --------------------------------------------------
    // 6. Validate generated CSV data
    // --------------------------------------------------

    println()
    println("Validating generated CSV data...")

    val validationResult =
      CsvDataValidator.validate(
        config.output.directory
      )

    println()
    println("==========================================")
    println("ShopSphere Data Validation")
    println("==========================================")
    println()

    if (validationResult.isValid) {

      println(
        "Structural validation       PASS"
      )

      println(
        "Primary-key validation      PASS"
      )

      println(
        "Foreign-key validation      PASS"
      )

      println(
        "Business-rule validation    PASS"
      )

      println()
      println(
        "Validation completed successfully."
      )

    } else {
      println(
        s"Validation failed with " +
          s"${validationResult.issues.size} issue(s)."
      )
      println()

      validationResult.issues.foreach {
        issue =>
          println(
            s"[${issue.category}] " +
              s"${issue.entity}: " +
              s"${issue.message}"
          )
      }

      throw new IllegalStateException(
        "Generated data failed validation."
      )
    }

    // --------------------------------------------------
    // 7. Collect and display dataset statistics
    // --------------------------------------------------

    println()
    println("Collecting dataset statistics...")

    val statistics =
      StatisticsCollector.collect(
        generatedData
      )

    println("Dataset statistics collected.")

    println()
    println("==========================================")
    println("Dataset Statistics")
    println("==========================================")

    println()
    println("Cardinality:")

    println(
      f"  Items per order:        ${statistics.averages("items_per_order")}%.2f"
    )

    println(
      f"    Distribution:         ${formatDistribution("items_per_order", statistics)}"
    )

    println(
      f"  Sessions per customer:  ${statistics.averages("sessions_per_customer")}%.2f"
    )

    println(
      f"    Distribution:         ${formatDistribution("sessions_per_customer", statistics)}"
    )

    println(
      f"  Events per session:     ${statistics.averages("events_per_session")}%.2f"
    )

    println(
      f"    Distribution:         ${formatDistribution("events_per_session", statistics)}"
    )

    println(
      f"  Addresses per customer: ${statistics.averages("addresses_per_customer")}%.2f"
    )

    println(
      f"    Distribution:         ${formatDistribution("addresses_per_customer", statistics)}"
    )

    println(
      f"  Returns per order:      ${statistics.averages("returns_per_order")}%.4f"
    )

    println(
      f"    Distribution:         ${formatDistribution("returns_per_order", statistics)}"
    )

    println()
    println("Financial:")

    println(
      s"  Total order value:   ${statistics.financial.totalOrderValue}"
    )

    println(
      s"  Average order value: ${statistics.financial.averageOrderValue}"
    )

    println(
      s"  Minimum order value: ${statistics.financial.minimumOrderValue}"
    )

    println(
      s"  P25 order value:     ${statistics.financial.p25OrderValue}"
    )

    println(
      s"  Median order value:  ${statistics.financial.medianOrderValue}"
    )

    println(
      s"  P75 order value:     ${statistics.financial.p75OrderValue}"
    )

    println(
      s"  P95 order value:     ${statistics.financial.p95OrderValue}"
    )

    println(
      s"  P99 order value:     ${statistics.financial.p99OrderValue}"
    )

    println(
      s"  Maximum order value: ${statistics.financial.maximumOrderValue}"
    )

    println()
    println("Order status distribution:")

    statistics.distributions("order_status")
      .toSeq
      .sortBy(_._1)
      .foreach {
        case (status, count) =>
          println(
            f"  $status%-12s $count%,d"
          )
      }

    println()
    println("Payment method distribution:")

    statistics.distributions("payment_method")
      .toSeq
      .sortBy(_._1)
      .foreach {
        case (method, count) =>
          println(
            f"  $method%-15s $count%,d"
          )
      }

    println()
    println("Device distribution:")

    statistics.distributions("device_type")
      .toSeq
      .sortBy(_._1)
      .foreach {
        case (device, count) =>
          println(
            f"  $device%-10s $count%,d"
          )
      }

    println()
    println("Channel distribution:")

    statistics.distributions("channel")
      .toSeq
      .sortBy(_._1)
      .foreach {
        case (channel, count) =>
          println(
            f"  $channel%-15s $count%,d"
          )
      }

    // --------------------------------------------------
    // 8. Generation summary
    // --------------------------------------------------

    val elapsedSeconds =
      (System.nanoTime() - startTime) /
        1_000_000_000.0

    println()
    println("==========================================")
    println("Generation Summary")
    println("==========================================")
    println()

    println(
      s"Seed:              ${config.generator.seed}"
    )

    println(
      s"Profile:           ${config.generator.profile.name}"
    )

    println(
      s"Cardinality:       " +
        s"${config.generator.cardinalityProfile.name}"
    )

    println(
      s"Scenario:          ${config.generator.scenario.name}"
    )

    println(
      s"Output directory:  ${config.output.directory}"
    )

    println()

    println(
      s"Customers:          ${generatedData.customers.size}"
    )

    println(
      s"Addresses:          ${generatedData.addresses.size}"
    )

    println(
      s"Categories:         ${generatedData.categories.size}"
    )

    println(
      s"Brands:             ${generatedData.brands.size}"
    )

    println(
      s"Products:           ${generatedData.products.size}"
    )

    println(
      s"Orders:             ${generatedData.orders.size}"
    )

    println(
      s"OrderItems:         ${generatedData.orderItems.size}"
    )

    println(
      s"Payments:            ${generatedData.payments.size}"
    )

    println(
      s"Shipments:           ${generatedData.shipments.size}"
    )

    println(
      s"Returns:             ${generatedData.returns.size}"
    )

    println(
      s"Sessions:            ${generatedData.sessions.size}"
    )

    println(
      s"Events:              ${generatedData.events.size}"
    )

    println()

    println(
      f"Elapsed time:       $elapsedSeconds%.3f seconds"
    )

    println()
    println(
      "Generation finished successfully."
    )
  }

  private def formatDistribution(
                                  name: String,
                                  statistics: com.shopsphere.datagenerator.statistics.DatasetStatistics
                                ): String = {

    val distribution =
      statistics.cardinalityStatistics(name)

    s"min=${distribution.minimum}, " +
      s"p25=${distribution.p25}, " +
      s"median=${distribution.median}, " +
      s"p75=${distribution.p75}, " +
      s"p95=${distribution.p95}, " +
      s"p99=${distribution.p99}, " +
      s"max=${distribution.maximum}"
  }
}