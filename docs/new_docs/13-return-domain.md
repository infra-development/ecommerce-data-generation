# 13 — Return Domain

## 1. Purpose

This document defines the business meaning, lifecycle, relationships, generation strategy, return propensity, eligibility, timing, reasons, partial-return behavior, refund relationship, validation, statistical realism, configuration, architecture, testing, and migration plan for the **Return** domain in the ShopSphere e-commerce data generator.

Return represents post-purchase behavior in which a customer sends purchased merchandise back through the fulfillment process.

The central relationship is:

```text
Customer
   ↓
Order
   ↓
OrderItem
   ↓
Return
```

with fulfillment context:

```text
Order
   ↓
Shipment
   ↓
Delivery
   ↓
Return
```

and financial consequence:

```text
Return
   ↓
Refund
   ↓
Payment
```

The central architectural principle is:

> **Return represents a post-purchase business event; it should be generated from transaction, customer, product, fulfillment, and timing context rather than as an independent random flag on an Order.**

---

# 2. Business Meaning

A Return answers:

> "What purchased merchandise did the customer decide to return, and how did that return progress?"

A return may involve:

- an Order,
- one or more OrderItems,
- a customer,
- a reason,
- a return request,
- physical return processing,
- refund behavior.

The exact persisted fields must follow the current Return model.

Do not add production-style return attributes merely because they are common in real commerce systems.

---

# 3. Current Baseline

The current generation plan targets approximately:

```text
5,000 Orders
400 planned Returns
```

The observed baseline generated:

```text
239 Returns
```

giving approximately:

```text
239 / 5,000 ≈ 4.78%
```

The current relationship is therefore approximately:

```text
4.8 returns / 100 Orders
```

The exact result depends on seed and generation logic.

This provides a useful starting point for return-rate realism.

---

# 4. Return Ownership

## Return owns

- Return identity,
- Order relationship,
- returned item relationship where modeled,
- return reason,
- return lifecycle/status,
- return timing,
- refund-related return context where modeled.

## Order owns

- original commercial transaction.

## OrderItem owns

- purchased product and original line-level transaction context.

## Product owns

- catalog/product characteristics that may influence return probability.

## Shipment owns

- delivery and fulfillment context.

## Payment owns

- financial transaction processing.

Return should coordinate with these domains without absorbing their responsibilities.

---

# 5. Return vs Refund

Return and refund are related but not identical.

A Return answers:

> "What merchandise came back?"

A Refund answers:

> "What money was returned to the customer?"

The conceptual flow is:

```text
Customer Return
      ↓
Return processing
      ↓
Refund eligibility
      ↓
Refund
      ↓
Payment
```

The current project may not have a separate Refund entity.

That is acceptable.

Refund can remain a future Payment/financial capability until a separate domain is justified.

---

# 6. Return Relationship to Order

The baseline can represent:

```text
Order 0 → 1 Return
```

because the current observed data has at most one Return per Order.

However, the business model should not assume that every returned transaction requires exactly one Return record forever.

Future realism may allow:

```text
Order
 ├── Return A
 └── Return B
```

if separate return requests are supported.

---

# 7. Return Relationship to OrderItem

This is one of the most important future design decisions.

A return is usually associated with specific purchased items.

Conceptually:

```text
Order
  ├── OrderItem A
  ├── OrderItem B
  └── OrderItem C
          ↓
       Return
          ↓
     returned item
```

A future model should support partial returns.

For example:

```text
Order:
  Item A × 2
  Item B × 1

Return:
  Item A × 1
```

The complete Order was not returned.

Only part of it was.

---

# 8. Current Simplification

The current Return implementation should remain aligned with the existing model.

If the current Return model only references Order, do not silently add an OrderItem foreign key during architectural migration.

Instead:

```text
Current model
    ↓
preserve during migration
    ↓
future partial-return design
```

This separates architectural refactoring from business-model expansion.

---

# 9. Return Eligibility

Not every Order should be eligible for return.

A basic rule is:

```text
Order delivered
      ↓
eligible return window
      ↓
return may occur
```

Orders that are:

```text
PLACED
CONFIRMED
SHIPPED
```

would normally not be customer merchandise returns unless the business scenario explicitly supports pre-delivery cancellation/return behavior.

---

# 10. Return Eligibility Model

A future strategy can represent:

```text
ReturnEligibilityModel
```

which determines:

> "Is this Order or OrderItem eligible to be returned?"

Potential inputs:

```text
order status
delivery timestamp
product category
product characteristics
return window
scenario
```

---

# 11. Return Window

A return window can be represented conceptually as:

```text
delivery
   ↓
return window
   ↓
eligible
   ↓
return request
```

For example, a product may have a configurable number of eligible days.

The exact default should be chosen through business configuration rather than hard-coded in ReturnGenerator.

---

# 12. Return Timing

Return timing should be derived from delivery.

Conceptually:

```text
deliveredAt
    ↓
ReturnRequestTimeModel
    ↓
returnRequestedAt
```

A return should not randomly occur before delivery.

---

# 13. Return Delay Distribution

The time between delivery and return request should be variable.

Potential shape:

```text
many returns
    → relatively soon after delivery

fewer returns
    → near the end of return window
```

The exact distribution can be modeled with:

```text
empirical distribution
geometric-like distribution
truncated distribution
```

depending on calibration needs.

---

# 14. Customer Return Propensity

Return behavior should eventually be customer-specific.

A future CustomerBehaviorProfile may contain:

```text
return propensity
```

or an equivalent latent behavior.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
ReturnProbabilityModel
        ↓
Return
```

This is significantly more realistic than assigning the same return probability to every customer.

---

# 15. Customer Heterogeneity

Customers can behave differently.

For example:

```text
Customer A
  low return tendency

Customer B
  average return tendency

Customer C
  high return tendency
```

This does not mean every purchase by Customer C must be returned.

It means:

```text
P(return | Customer C)
>
P(return | Customer A)
```

on average.

---

# 16. Product Return Propensity

Product characteristics can also influence return probability.

Potential factors:

```text
category
price band
product type
brand
```

The exact factors depend on the Product model.

The important architectural principle is:

> Return probability should be contextual.

---

# 17. Category Return Rate

Different categories can plausibly have different return behavior.

For example:

```text
Category
    ↓
category-specific return multiplier
```

This creates useful conditional statistics:

```text
return rate by category
```

The exact multipliers should be configurable.

---

# 18. Product Return Rate

Products can also have different return tendencies.

For example:

```text
Product A → low return propensity
Product B → high return propensity
```

A product-level return multiplier can create this variation.

Again, it should remain probabilistic.

---

# 19. Customer × Product Interaction

A mature model can combine:

```text
customer return propensity
+
product return propensity
```

to determine:

```text
P(return)
```

Conceptually:

```text
CustomerProfile
       +
ProductProfile
       +
OrderContext
       ↓
ReturnProbabilityModel
       ↓
return decision
```

This is an important target for sophisticated realism.

---

# 20. Shipment and Delivery Effect

Return probability can depend on fulfillment outcomes.

For example:

```text
late delivery
    ↓
customer dissatisfaction
    ↓
potentially higher return probability
```

This should not be deterministic.

It is a potential future correlation.

---

# 21. Delivery Condition

A future model may include delivery-related factors such as:

```text
late
damaged
failed attempt
```

which could affect return behavior.

The Shipment domain owns those delivery facts.

Return consumes them.

---

# 22. Return Reason

A Return commonly has a reason.

Potential conceptual reasons include:

```text
DAMAGED
WRONG_ITEM
SIZE_OR_FIT
NOT_AS_EXPECTED
CHANGED_MIND
DEFECTIVE
OTHER
```

The final enum must follow the actual model and business requirements.

Do not add reasons simply to increase the number of categories.

---

# 23. Return Reason Distribution

Return reasons should have configurable weights.

Conceptually:

```text
ReturnReasonModel
       ↓
reason
```

A uniform distribution is usually not realistic.

One or two reasons may account for a large share of returns.

---

# 24. Reason and Product Category

Return reasons can depend on category.

For example:

```text
fashion
  → fit-related returns

electronics
  → defective / not-as-expected returns
```

These are illustrative examples.

The generator should only encode relationships that the ShopSphere scenario explicitly adopts.

---

# 25. Reason and Customer Behavior

Customer behavior may also affect reasons.

For example:

```text
high return propensity
   → more changed-mind behavior
```

or:

```text
high price sensitivity
   → potentially more expectation-related returns
```

These should be probabilistic relationships.

---

# 26. Return Status

A future lifecycle may contain:

```text
REQUESTED
APPROVED
IN_TRANSIT
RECEIVED
INSPECTED
REFUNDED
REJECTED
```

The exact status set must follow the final Return model.

The current implementation should not be expanded without explicit design.

---

# 27. Return Lifecycle

A conceptual lifecycle:

```text
REQUESTED
    ↓
APPROVED
    ↓
IN_TRANSIT
    ↓
RECEIVED
    ↓
INSPECTED
    ↓
REFUNDED
```

Alternative path:

```text
REQUESTED
    ↓
REJECTED
```

Not every return must follow every state if the business process simplifies them.

---

# 28. State Machine Assessment

A simple status model plus transition validation may be sufficient initially.

The State pattern becomes useful only if state-specific behavior becomes complex.

For example:

```text
approve
reject
receive
inspect
refund
```

may eventually require richer lifecycle behavior.

Until then, keep the design simple.

---

# 29. Return Approval

A future model may distinguish:

```text
customer requested return
```

from:

```text
business approved return
```

This is useful because not every request necessarily becomes a completed return.

The baseline may treat Return as already-created/completed behavior.

---

# 30. Return Rejection

A rejected return may occur because:

```text
outside return window
product not eligible
condition invalid
policy violation
```

The exact reasons should only be introduced when eligibility/approval behavior is implemented.

---

# 31. Partial Return

Partial returns are a major future realism feature.

Example:

```text
Order
  ├── Laptop × 1
  ├── Mouse × 2
  └── Bag × 1

Return
  └── Mouse × 1
```

This requires Return to identify the returned OrderItem and returned quantity.

---

# 32. Returned Quantity

If an OrderItem contains:

```text
quantity = 5
```

then a partial return could contain:

```text
returnedQuantity = 2
```

with invariant:

```text
0 < returnedQuantity <= purchasedQuantity
```

This should only be introduced after the OrderItem/Return relationship is designed.

---

# 33. Multiple Return Requests

A future order could have:

```text
Order
   ↓
Return Request A
   ↓
partial return

Return Request B
   ↓
different item
```

This is a more sophisticated workflow.

The baseline does not require it.

---

# 34. Refund Amount

For a simple full return:

```text
Refund Amount
=
returned line value
```

subject to discount, tax, shipping, and refund policy if those are eventually modeled.

The current project should not invent a tax/refund accounting model prematurely.

---

# 35. Refund and Payment

The conceptual relationship is:

```text
Return
   ↓
Refund
   ↓
Payment
```

A successful refund should correspond to a previous successful payment.

This becomes a cross-domain invariant if refund behavior is implemented.

---

# 36. Refund Method

A future refund may return funds through:

```text
original payment method
```

or another supported method.

This should be modeled only if Payment/Refund requirements justify it.

---

# 37. Return and Order Value

Return behavior should eventually affect:

```text
gross order value
returned value
net order value
```

For example:

```text
Gross = ₹10,000
Returned = ₹2,000
Net = ₹8,000
```

This creates useful analytical measures.

---

# 38. Return Rate

Return rate should have multiple definitions.

For example:

```text
returned orders / delivered orders
```

and:

```text
returned items / purchased items
```

and:

```text
returned value / purchased value
```

These are different metrics.

The statistics layer should name them explicitly.

---

# 39. Why Return Rate Alone Is Insufficient

Suppose:

```text
return rate = 5%
```

This could mean:

```text
5% of Orders returned
```

or:

```text
5% of physical units returned
```

or:

```text
5% of merchandise value returned
```

These can produce very different business interpretations.

---

# 40. Return Concentration

Useful metrics include:

```text
returns/customer
returns/product
returns/category
returns/brand
```

and:

```text
top 1% customers' return share
```

This reveals whether return behavior is heterogeneous.

---

# 41. High-Return Customer Skew

A small customer population may account for a disproportionate share of returns.

This can be modeled through:

```text
CustomerBehaviorProfile.returnPropensity
```

and later used as a controlled realism scenario.

---

# 42. High-Return Product Skew

Similarly:

```text
few products
   ↓
high return rate
```

can be used for product-quality scenarios.

This should be distinct from:

```text
high product sales volume
```

A product can be popular and still have a low return rate.

---

# 43. Return Scenario

A useful scenario may be:

```text
high-return-category
```

where selected categories receive elevated return probability.

This is preferable to increasing the return probability globally.

---

# 44. Customer Return Scenario

Another scenario:

```text
high-return-customers
```

can assign higher latent return propensity to a small customer segment.

This produces controlled behavioral heterogeneity.

---

# 45. Quality Scenario

Return-specific data-quality defects can include:

```text
unknown Order ID
unknown OrderItem ID
invalid return status
invalid return reason
return before delivery
return after policy window
returned quantity > purchased quantity
negative refund amount
duplicate Return ID
```

These should be injected deliberately.

---

# 46. Clean Baseline

The clean baseline should guarantee:

```text
valid Return ID
valid Order reference
valid OrderItem reference where modeled
valid reason
valid lifecycle
valid timestamps
valid quantities
```

and:

```text
return timing follows delivery
```

when delivery information is available.

---

# 47. Eligibility Validation

If a return window is implemented:

```text
returnRequestedAt
```

should satisfy:

```text
deliveredAt <= returnRequestedAt
```

and:

```text
returnRequestedAt <= deliveredAt + returnWindow
```

for eligible returns.

---

# 48. Partial Return Validation

If partial returns exist:

```text
returnedQuantity <= purchasedQuantity
```

and:

```text
returnedQuantity > 0
```

must hold.

Multiple returns against the same OrderItem must also respect the total purchased quantity.

---

# 49. Refund Validation

If refund is modeled:

```text
refundAmount >= 0
```

and:

```text
refundAmount <= eligibleAmount
```

where the policy defines such a constraint.

---

# 50. Temporal Validation

A possible clean lifecycle requires:

```text
deliveredAt
    <= returnRequestedAt
    <= returnReceivedAt
    <= refundAt
```

for states that contain those timestamps.

---

# 51. Return Statistics

The statistics layer should report:

```text
return rate by order
return rate by item
return rate by value
returns/customer
returns/product
returns/category
returns/brand
return reasons
return timing
refund amount
```

---

# 52. Return Timing Statistics

Useful metrics:

```text
days-to-return
```

with:

```text
mean
median
p75
p90
p95
p99
max
```

This helps calibrate the return-window behavior.

---

# 53. Reason Distribution Statistics

Measure:

```text
return reason share
```

and conditional distributions:

```text
reason by category
reason by product type
reason by customer segment
```

when those behaviors are implemented.

---

# 54. Customer Return Propensity Validation

A high-propensity customer population should statistically return more merchandise than a low-propensity population.

This verifies that latent behavior actually reaches the generated data.

---

# 55. Product Return Propensity Validation

Similarly:

```text
high-return products
```

should exhibit higher observed return rates than:

```text
low-return products
```

within statistical tolerance.

---

# 56. Strategy Pattern

Return has legitimate strategy candidates:

```text
ReturnProbabilityModel
ReturnEligibilityModel
ReturnReasonModel
ReturnTimingModel
ReturnQuantityModel
RefundModel
```

Not all need to be implemented immediately.

---

# 57. ReturnProbabilityModel

This answers:

> "How likely is this purchase to result in a return?"

Potential inputs:

```text
customer profile
product profile
category
order value
delivery context
scenario
```

---

# 58. ReturnEligibilityModel

This answers:

> "Is this transaction eligible for a return?"

Potential inputs:

```text
delivery status
delivery timestamp
product policy
return window
scenario
```

---

# 59. ReturnReasonModel

This answers:

> "Why is the customer returning the merchandise?"

It may depend on:

```text
category
product
customer behavior
```

---

# 60. ReturnTimingModel

This answers:

> "When does the customer request the return?"

Input:

```text
delivery context
customer behavior
return window
```

---

# 61. ReturnQuantityModel

This becomes necessary when partial returns are supported.

It answers:

> "How many purchased units are returned?"

---

# 62. RefundModel

A future RefundModel can determine:

```text
refund amount
refund timing
refund eligibility
```

It should not be introduced until the financial model is ready.

---

# 63. Factory Pattern

Factories may select:

```text
ReturnProbabilityModelFactory
ReturnEligibilityModelFactory
ReturnReasonModelFactory
ReturnTimingModelFactory
```

only when multiple implementations genuinely exist.

---

# 64. Builder Assessment

Builder is generally unnecessary for Return.

Immutable case classes remain the preferred representation for simple records.

---

# 65. Composition

Prefer:

```text
ReturnGenerator
   ├── ReturnProbabilityModel
   ├── ReturnEligibilityModel
   ├── ReturnReasonModel
   ├── ReturnTimingModel
   └── ReturnQuantityModel
```

rather than a large conditional ReturnGenerator.

---

# 66. Dependency Injection

Constructor injection is preferred.

Conceptually:

```scala
class ReturnGenerator(
    probabilityModel: ReturnProbabilityModel,
    eligibilityModel: ReturnEligibilityModel,
    reasonModel: ReturnReasonModel,
    timingModel: ReturnTimingModel
)
```

No DI framework is required.

---

# 67. Domain Service Assessment

Return generation spans multiple domains:

```text
Order
OrderItem
Customer
Product
Shipment
Payment
```

A coordinating service may eventually orchestrate:

```text
return creation
+
refund processing
```

but individual models should retain their own responsibilities.

---

# 68. SOLID — Single Responsibility

ReturnGenerator should not own:

```text
Order generation
Shipment generation
Payment generation
CSV writing
global statistics
```

---

# 69. SOLID — Open/Closed

Return behavior should be extensible through strategies where real behavioral variation exists.

---

# 70. SOLID — Liskov

Concrete return strategies should satisfy their declared contracts.

---

# 71. SOLID — Interface Segregation

Keep return interfaces narrow.

Avoid a single interface that mixes:

```text
eligibility
reason
timing
refund
```

unless the business model genuinely requires it.

---

# 72. SOLID — Dependency Inversion

Return generation should depend on meaningful abstractions for customer/product/fulfillment context.

---

# 73. Readability Standard

The code should communicate post-purchase behavior.

Preferred conceptual flow:

```scala
val eligible =
  eligibilityModel.isEligible(
    order,
    deliveryContext
  )

if (eligible) {
  val probability =
    probabilityModel.calculate(
      customerProfile,
      order,
      productContext
    )

  val shouldReturn =
    random.nextBoolean(probability)

  if (shouldReturn) {
    val reason =
      reasonModel.select(
        customerProfile,
        productContext
      )

    val requestedAt =
      timingModel.determine(
        deliveryContext
      )

    createReturn(
      order,
      reason,
      requestedAt
    )
  }
}
```

The exact implementation will depend on the final model.

The important property is that the code reads like a return decision.

---

# 74. Cross-Domain Dependency Direction

A reasonable dependency direction is:

```text
Customer Behavior ─┐
                   │
Product Context ───┼──► Return Behavior
                   │
Shipment Context ──┘
```

Return consumes these contexts.

They should not depend on Return-specific implementation details.

---

# 75. Customer Behavior Integration

Return is one of the strongest consumers of CustomerBehaviorProfile.

The profile should eventually influence:

```text
return propensity
return timing
return reason distribution
```

This makes Return an important validation point for the behavioral architecture.

---

# 76. Product Behavior Integration

Product characteristics can influence:

```text
return probability
return reason
return quantity
```

The Product domain supplies context.

Return owns the return decision.

---

# 77. Shipment Integration

Shipment supplies:

```text
delivery status
delivery timestamp
delay information
```

Return uses this to determine:

```text
eligibility
timing
potential return propensity
```

---

# 78. Payment Integration

Payment supplies financial context.

A future refund process can use:

```text
successful payment
payment amount
payment method
```

to determine refund handling.

Return should not mutate Payment directly.

---

# 79. Output Boundary

Return should not write:

```text
returns.csv
```

The output layer owns serialization.

---

# 80. Statistics Boundary

Return generation should not calculate:

```text
return rate
refund rate
average days to return
```

The statistics layer owns these metrics.

---

# 81. Manifest Boundary

The manifest can record:

```text
return row count
schema
generation metadata
```

Return generation does not own manifest construction.

---

# 82. Reproducibility

Return generation must remain deterministic under:

```text
same seed
same configuration
same customer profiles
same orders
same products
same shipment context
```

Derived random streams should isolate:

```text
eligibility
return decision
reason
timing
quantity
```

---

# 83. Parallel Generation

Return generation can eventually be partitioned by eligible Order or OrderItem.

Conceptually:

```text
Orders / OrderItems
       ↓
eligible population
       ↓
Return generation
```

Avoid shared mutable state.

---

# 84. Performance

At large scale, Return generation may require efficient:

```text
Order lookup
OrderItem lookup
Customer profile lookup
Product lookup
Shipment lookup
```

Indexed reference structures should be prepared outside ReturnGenerator.

---

# 85. Lookup Responsibility

ReturnGenerator should not repeatedly scan:

```text
all Orders
all OrderItems
all Products
all Shipments
```

for every candidate.

Prefer indexed context services.

---

# 86. Spark Performance Laboratory

Return supports useful Spark workloads:

```text
Return ↔ Order
Return ↔ OrderItem
Return ↔ Product
Return ↔ Customer
Return ↔ Shipment
```

and:

```text
groupBy(return_reason)
groupBy(category)
groupBy(product)
groupBy(customer)
```

---

# 87. Return Analytics

Potential Spark exercises include:

```text
return rate by category
return rate by product
return rate by customer segment
return rate by order value
return rate by carrier
days-to-return distribution
returned value by month
```

---

# 88. Join Workloads

A useful multi-table workload is:

```text
Return
  JOIN OrderItem
  JOIN Product
  JOIN Customer
```

followed by:

```text
groupBy(category)
```

or:

```text
groupBy(customer_segment)
```

This produces realistic analytical joins.

---

# 89. Skew Workloads

High-return customers can produce:

```text
customer_id
    ↓
many Return records
```

while high-return products can produce:

```text
product_id
    ↓
many Return records
```

These can be used for controlled Spark skew experiments.

---

# 90. Temporal Workloads

Return timing supports:

```text
returns/day
returns/month
days-to-return
return rate by cohort
```

This is useful for time-based Spark exercises.

---

# 91. Cohort Analysis

A future dataset can support:

```text
customer acquisition cohort
      ↓
orders
      ↓
returns
```

This can measure:

```text
return rate by customer cohort
```

and is useful for business analytics.

---

# 92. Return Rate by Order Value

A useful metric is:

```text
return probability by order-value band
```

For example:

```text
₹0–₹1K
₹1K–₹5K
₹5K–₹10K
₹10K+
```

The exact bands should be configurable.

This can reveal unintended monetary correlations.

---

# 93. Return Rate by Customer Behavior

Once CustomerBehaviorProfile exists:

```text
return propensity segment
    ↓
observed return rate
```

should show the intended ordering.

This becomes a key behavioral realism test.

---

# 94. Current Simplifications

The current Return implementation simplifies:

- return eligibility,
- customer return propensity,
- product return propensity,
- category-specific return rates,
- return reasons,
- return timing,
- partial returns,
- multiple return requests,
- return lifecycle,
- refund behavior,
- shipment-condition effects,
- return-window rules.

These are planned realism capabilities.

---

# 95. Planned Realism Sequence

Recommended progression:

```text
1. Preserve current Return model
2. Stabilize Order relationship
3. Introduce delivered-order eligibility
4. Introduce customer return propensity
5. Introduce product/category return propensity
6. Introduce return reasons
7. Introduce return timing
8. Connect Shipment delivery context
9. Introduce partial returns
10. Introduce return lifecycle
11. Introduce refund behavior
12. Introduce advanced customer/product interactions
```

---

# 96. Migration Strategy

## Step 1

Inspect current Return model.

## Step 2

Inspect current ReturnGenerator.

## Step 3

Inspect how Orders are selected.

## Step 4

Inspect current return probability.

## Step 5

Inspect current relationship to OrderItems.

## Step 6

Inspect current timestamps/statuses.

## Step 7

Define target responsibilities.

## Step 8

Move model into:

```text
return/model
```

## Step 9

Move generation into:

```text
return/generator
```

## Step 10

Introduce behavior strategies only where justified.

## Step 11

Update transaction orchestration.

## Step 12

Update validation.

## Step 13

Update statistics.

## Step 14

Run focused Return tests.

## Step 15

Run Order/OrderItem/Shipment integration tests.

## Step 16

Run the complete suite.

## Step 17

Run end-to-end generation.

## Step 18

Compare return statistics with the baseline.

---

# 97. Migration Quality Gate

Return migration is complete when:

### Business

- Return meaning is explicit.
- eligibility is defined.
- Order relationship is defined.
- return timing is defined.

### Architecture

- Return does not generate Orders.
- Return does not own Shipment behavior.
- Return does not directly mutate Payment.
- Return does not write output.
- Return does not own global statistics.

### Data

- IDs unique,
- references valid,
- reasons valid,
- statuses valid,
- timestamps coherent.

### Behavior

- return probability has a clear owner,
- eligibility has a clear owner,
- reason has a clear owner,
- timing has a clear owner.

### Testing

- Return tests pass,
- dependent integration tests pass,
- full suite passes.

### Reproducibility

- same inputs produce the same returns.

---

# 98. Design Decisions

## Decision A — Return is post-purchase behavior

It should occur in the context of an existing transaction.

---

## Decision B — Delivery precedes normal merchandise return

Customer return eligibility should normally begin after delivery.

---

## Decision C — Return probability is contextual

Customer, Product, Order, and Shipment context can influence it.

---

## Decision D — Customer return propensity belongs to behavioral modeling

It should not be embedded as a hard-coded ReturnGenerator constant.

---

## Decision E — Partial returns are a future capability

They require a clear OrderItem relationship and returned-quantity model.

---

## Decision F — Return and Refund are separate concepts

Return describes merchandise; Refund describes money.

---

## Decision G — Payment remains the financial domain

Return should request/orchestrate refund behavior rather than own Payment internals.

---

## Decision H — Return lifecycle should remain simple until complexity requires more

A status enum plus validation is preferred before introducing a State implementation.

---

## Decision I — Return quality defects are scenario-driven

The clean business generator should not produce accidental invalid returns.

---

# 99. Final Mental Model

The simple baseline:

```text
Order
  ↓
Shipment
  ↓
Delivered
  ↓
Return
```

The behavioral model:

```text
CustomerBehaviorProfile
        │
        ├── return propensity
        └── return behavior
                │
                ▼
             Return
                ▲
                │
ProductProfile ─┤
                │
ShipmentContext ┘
```

The mature return flow:

```text
Order
  ↓
OrderItem
  ↓
Delivered Shipment
  ↓
ReturnEligibilityModel
  ↓
ReturnProbabilityModel
  ↓
Return decision
  ↓
ReturnReasonModel
  ↓
ReturnTimingModel
  ↓
Return
  ↓
Refund
  ↓
Payment
```

For partial returns:

```text
Order
 ├── Item A × 2
 ├── Item B × 1
 └── Item C × 1
          │
          ▼
       Return
          └── Item A × 1
```

---

# 100. Summary

Return represents post-purchase merchandise-return behavior.

The current baseline generates approximately:

```text
239 Returns
5,000 Orders
```

or roughly:

```text
4.8% of Orders
```

The baseline is structurally useful but currently does not model the full behavioral richness of returns.

The major realism improvements are:

```text
delivered-order eligibility
        ↓
customer return propensity
        ↓
product/category return propensity
        ↓
return reasons
        ↓
return timing
        ↓
shipment/delivery effects
        ↓
partial returns
        ↓
return lifecycle
        ↓
refund behavior
```

The target architecture is:

```text
ReturnGenerator
   ├── ReturnEligibilityModel
   ├── ReturnProbabilityModel
   ├── ReturnReasonModel
   ├── ReturnTimingModel
   └── ReturnQuantityModel
```

with financial refund processing remaining separate.

The most important principle is:

> **A Return should look like a believable post-purchase decision produced by customer behavior, product characteristics, fulfillment context, and timing—not like a random Boolean attached to an Order.**

The next domain is **Session**, which introduces customer activity before and around transactions: session frequency, device/channel behavior, session duration, conversion opportunities, and the behavioral bridge between Customers, Events, and Orders.
