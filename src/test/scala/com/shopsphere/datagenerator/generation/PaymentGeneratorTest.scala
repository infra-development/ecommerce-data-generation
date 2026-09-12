package com.shopsphere.datagenerator.generation

import com.shopsphere.datagenerator.distribution.{
  DistributionEngine,
  RandomGenerator,
  WeightedDistribution
}
import org.scalatest.funsuite.AnyFunSuite

class PaymentGeneratorTest extends AnyFunSuite {

  private val amount =
    BigDecimal("24999.99")

  private val distributionEngine =
    new DistributionEngine(
      Map(
        "payment_method" ->
          new WeightedDistribution(
            Seq(
              "UPI" -> 0.45,
              "CREDIT_CARD" -> 0.20,
              "DEBIT_CARD" -> 0.15,
              "NET_BANKING" -> 0.10,
              "WALLET" -> 0.10
            )
          )
      )
    )

  test("should generate payment for the supplied order") {

    val payment =
      PaymentGenerator.generate(
        "ORDER_000001_PAYMENT",
        "ORDER_000001",
        amount,
        new RandomGenerator(42L),
        distributionEngine
      )

    assert(payment.id == "ORDER_000001_PAYMENT")
    assert(payment.orderId == "ORDER_000001")
    assert(payment.amount == amount)
  }

  test("should generate a valid payment method") {

    val validMethods =
      Set(
        "UPI",
        "CREDIT_CARD",
        "DEBIT_CARD",
        "NET_BANKING",
        "WALLET"
      )

    val payment =
      PaymentGenerator.generate(
        "ORDER_000001_PAYMENT",
        "ORDER_000001",
        amount,
        new RandomGenerator(42L),
        distributionEngine
      )

    assert(
      validMethods.contains(payment.paymentMethod)
    )
  }

  test("should generate a valid payment status") {

    val validStatuses =
      Set(
        "SUCCESS",
        "FAILED",
        "PENDING"
      )

    val payment =
      PaymentGenerator.generate(
        "ORDER_000001_PAYMENT",
        "ORDER_000001",
        amount,
        new RandomGenerator(42L),
        distributionEngine
      )

    assert(
      validStatuses.contains(payment.paymentStatus)
    )
  }

  test("should produce the same payment for the same seed") {

    val payment1 =
      PaymentGenerator.generate(
        "ORDER_000001_PAYMENT",
        "ORDER_000001",
        amount,
        new RandomGenerator(42L),
        distributionEngine
      )

    val payment2 =
      PaymentGenerator.generate(
        "ORDER_000001_PAYMENT",
        "ORDER_000001",
        amount,
        new RandomGenerator(42L),
        distributionEngine
      )

    assert(payment1 == payment2)
  }

  test("should reject an empty payment ID") {

    val exception =
      intercept[IllegalArgumentException] {

        PaymentGenerator.generate(
          "",
          "ORDER_000001",
          amount,
          new RandomGenerator(42L),
          distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Payment ID must not be empty."
    )
  }

  test("should reject an empty order ID") {

    val exception =
      intercept[IllegalArgumentException] {

        PaymentGenerator.generate(
          "PAYMENT_000001",
          "",
          amount,
          new RandomGenerator(42L),
          distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Order ID must not be empty."
    )
  }

  test("should reject a negative payment amount") {

    val exception =
      intercept[IllegalArgumentException] {

        PaymentGenerator.generate(
          "PAYMENT_000001",
          "ORDER_000001",
          BigDecimal("-1.00"),
          new RandomGenerator(42L),
          distributionEngine
        )
      }

    assert(
      exception.getMessage ==
        "Payment amount must not be negative."
    )
  }

  test("should use the configured payment method distribution") {

    val paymentMethods =
      (1 to 1000).map { seed =>

        PaymentGenerator
          .generate(
            paymentId =
              "PAYMENT_000001",
            orderId =
              "ORDER_000001",
            amount =
              amount,
            random =
              new RandomGenerator(seed.toLong),
            distributionEngine =
              distributionEngine
          )
          .paymentMethod
      }

    val upiCount =
      paymentMethods.count(
        _ == "UPI"
      )

    val upiRatio =
      upiCount.toDouble /
        paymentMethods.size

    assert(
      upiRatio > 0.35 &&
        upiRatio < 0.55
    )
  }

  test("should produce different payment methods from different seeds") {

    val paymentMethods =
      (1 to 100).map { seed =>

        PaymentGenerator
          .generate(
            paymentId =
              "PAYMENT_000001",
            orderId =
              "ORDER_000001",
            amount =
              amount,
            random =
              new RandomGenerator(seed.toLong),
            distributionEngine =
              distributionEngine
          )
          .paymentMethod
      }

    assert(
      paymentMethods.distinct.size > 1
    )
  }
}