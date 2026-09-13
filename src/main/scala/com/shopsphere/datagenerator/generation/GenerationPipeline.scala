package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.model._
import com.shopsphere.datagenerator.reference.catalog.CatalogReferenceLoader
import com.shopsphere.datagenerator.reference.customer.CustomerReferenceLoader
import com.shopsphere.datagenerator.relationship.OrderRelationshipGenerator

import java.nio.file.Paths

object GenerationPipeline {

  def generate(
                context: GenerationContext,
                plan: GenerationPlan
              ): GeneratedData = {

    val customerReferenceData =
      CustomerReferenceLoader.load("data/reference/customer")

    val catalogReferenceData =
      CatalogReferenceLoader.load(Paths.get("data/reference/catalog"))

    println("Generating customers...")

    val customers =
      generateCustomers(
        context,
        plan,
        customerReferenceData
      )

    println("Generating addresses...")

    val addresses =
      generateAddresses(
        context,
        plan,
        customers
      )

    require(
      addresses.size == plan.addressCount,
      s"Generated ${addresses.size} addresses but expected ${plan.addressCount}."
    )

    println("Generating categories and brands...")

    val categories =
      catalogReferenceData.categories.map { category =>
        Category(
          id = category.id,
          name = category.name
        )
      }

    val brands =
      catalogReferenceData.brands.map { brand =>
        Brand(
          id = brand.id,
          name = brand.name
        )
      }

    println("Generating products...")

    val products =
      generateProducts(
        context,
        plan,
        catalogReferenceData
      )

    println("Generating orders...")

    val transactionData =
      generateOrders(
        context,
        plan,
        customers,
        products
      )

    println("Generating sessions and events...")

    val sessionData =
      generateSessions(
        context,
        plan,
        customers,
        products
      )

    GeneratedData(
      customers = customers,
      addresses = addresses,
      categories = categories,
      brands = brands,
      products = products,
      orders = transactionData.orders,
      orderItems = transactionData.orderItems,
      payments = transactionData.payments,
      shipments = transactionData.shipments,
      returns = transactionData.returns,
      sessions = sessionData.sessions,
      events = sessionData.events
    )
  }

  private def generateCustomers(
                                 context: GenerationContext,
                                 plan: GenerationPlan,
                                 referenceData:
                                 com.shopsphere.datagenerator.reference.customer.CustomerReferenceData
                               ): Seq[Customer] = {

    val random =
      context.random.derive("customers")

    (1L to plan.customerCount).map { index =>

      CustomerGenerator.generate(
        customerId =
          f"CUSTOMER_$index%09d",
        referenceData = referenceData,
        generationConfig = context.config.customerGeneration,
        random = random.derive(index.toString)
      )
    }
  }

  private def generateAddresses(
                                 context: GenerationContext,
                                 plan: GenerationPlan,
                                 customers: Seq[Customer]
                               ): Seq[Address] = {

    require(
      customers.nonEmpty,
      "Customers must not be empty when generating addresses."
    )

    val random =
      context.random.derive("addresses")

    val addressCounts =
      CardinalityAllocator.allocate(
        baseCount = customers.size,
        averagePerBase =
          context.config.cardinality.addressesPerCustomer
      )

    var addressIndex = 1L

    customers.zip(addressCounts).flatMap {
      case (customer, addressCount) =>

        (1 to addressCount).map { sequence =>

          val address =
            AddressGenerator.generate(
              addressId =
                f"ADDRESS_$addressIndex%09d",
              customerId = customer.id,
              geography = context.geography,
              random =
                random.derive(
                  s"${customer.id}-$sequence"
                )
            )

          addressIndex += 1

          address
        }
    }
  }

  private def generateProducts(
                                context: GenerationContext,
                                plan: GenerationPlan,
                                referenceData:
                                com.shopsphere.datagenerator.reference.catalog.CatalogReferenceData
                              ): Seq[Product] = {

    val random =
      context.random.derive("products")

    (1L to plan.productCount).map { index =>

      ProductGenerator.generate(
        productId = f"PRODUCT_$index%09d",
        referenceData = referenceData,
        productDistributionConfig =
          context.config.productDistribution,
        pricingConfig = context.config.productPricing,
        brandAffinityConfig = context.config.productBrandAffinity,
        random = random.derive(index.toString)
      )
    }
  }

  private def generateOrders(
                              context: GenerationContext,
                              plan: GenerationPlan,
                              customers: Seq[Customer],
                              products: Seq[Product]
                            ): TransactionGenerationResult = {

    require(
      customers.nonEmpty,
      "Customers must not be empty when generating orders."
    )

    require(
      products.nonEmpty,
      "Products must not be empty when generating orders."
    )

    val random =
      context.random.derive("orders")

    val itemCounts =
      CardinalityAllocator.allocate(
        baseCount = plan.orderCount,
        averagePerBase =
          context.config.cardinality.itemsPerOrder
      )

    val results =
      (1L to plan.orderCount).map { index =>

        val customer =
          customers(
            ((index - 1) % customers.size).toInt
          )

        OrderRelationshipGenerator.generate(
          orderId =
            f"ORDER_$index%09d",
          customerId = customer.id,
          products = products,
          itemCount = itemCounts((index - 1).toInt),
          random = random.derive(index.toString),
          distributionEngine =
            context.distributionEngine
        )
      }

    TransactionGenerationResult(
      orders =
        results.map(_.order),

      orderItems =
        results.flatMap(_.orderItems),

      payments =
        results.map(_.payment),

      shipments =
        results.map(_.shipment),

      returns =
        results.flatMap(_.returnRecord)
    )
  }

  private def generateSessions(
                                context: GenerationContext,
                                plan: GenerationPlan,
                                customers: Seq[Customer],
                                products: Seq[Product]
                              ): SessionGenerationResult = {

    require(
      customers.nonEmpty,
      "Customers must not be empty when generating sessions."
    )

    require(
      products.nonEmpty,
      "Products must not be empty when generating events."
    )

    val random =
      context.random.derive("sessions")

    val sessions =
      (1L to plan.sessionCount).map { index =>

        val customer =
          customers(
            ((index - 1) % customers.size).toInt
          )

        SessionGenerator.generate(
          sessionId =
            f"SESSION_$index%09d",
          customerId = customer.id,
          random = random.derive(index.toString),
          distributionEngine =
            context.distributionEngine
        )
      }

    val events =
      generateEvents(
        context,
        sessions,
        products
      )

    SessionGenerationResult(
      sessions = sessions,
      events = events
    )
  }

  private def generateEvents(
                              context: GenerationContext,
                              sessions: Seq[Session],
                              products: Seq[Product]
                            ): Seq[Event] = {

    val random =
      context.random.derive("events")

    val eventCounts =
      CardinalityAllocator.allocate(
        baseCount = sessions.size,
        averagePerBase =
          context.config.cardinality.eventsPerSession
      )

    var eventIndex = 1L

    sessions.zip(eventCounts).flatMap {
      case (session, eventCount) =>

        (1 to eventCount).map { sequence =>

          val eventRandom =
            random.derive(
              s"${session.id}-$sequence"
            )

          val eventType =
            EventGenerator.eventTypes(
              eventRandom
                .derive("type")
                .nextInt(
                  EventGenerator.eventTypes.size
                )
            )

          val productId =
            if (
              eventType == "PRODUCT_VIEW" ||
                eventType == "ADD_TO_CART" ||
                eventType == "PURCHASE"
            ) {
              Some(
                products(
                  eventRandom
                    .derive("product")
                    .nextInt(products.size)
                ).id
              )
            } else {
              None
            }

          val event =
            EventGenerator.generate(
              eventId =
                f"EVENT_$eventIndex%09d",
              sessionId = session.id,
              customerId = session.customerId,
              sessionStart = session.sessionStart,
              sessionEnd = session.sessionEnd,
              eventType = eventType,
              productId = productId,
              random = eventRandom
            )

          eventIndex += 1

          event
        }
    }
  }
}

case class TransactionGenerationResult(
                                        orders: Seq[Order],
                                        orderItems: Seq[OrderItem],
                                        payments: Seq[Payment],
                                        shipments: Seq[Shipment],
                                        returns: Seq[Return]
                                      )

case class SessionGenerationResult(
                                    sessions: Seq[Session],
                                    events: Seq[Event]
                                  )