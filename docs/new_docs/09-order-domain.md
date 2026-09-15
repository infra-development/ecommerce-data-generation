# 09 — Order Domain

## 1. Purpose

This document defines the business meaning, lifecycle, model, relationships, generation strategy, pricing boundary, customer behavior integration, configuration, validation, statistical realism, scenario behavior, technical architecture, and migration plan for the **Order** domain in the ShopSphere e-commerce data generator.

Order is the central transactional domain.

It represents a customer's commercial transaction and connects:

```text
Customer
   ↓
Order
   ↓
OrderItem
   ↓
Product
```

while also connecting downstream operational entities:

```text
Order
 ├── Payment
 ├── Shipment
 └── Return
```

The central architectural principle is:

> **Order represents the business transaction; OrderItem represents what was purchased, while Payment, Shipment, and Return represent downstream transaction processes.**

---

# 2. Business Meaning

An Order represents a customer's purchase transaction.

It answers:

> "What transaction did this customer place, when did it happen, what did they purchase, and what is the state of that transaction?"

An Order therefore provides the transactional context for:

- customer,
- order timestamp,
- order status,
- order items,
- monetary totals,
- payment,
- shipment,
- returns.

The exact fields remain governed by the current model.

---

# 3. Current Order State

The baseline currently generates approximately:

```text
5,000 orders
```

The current observed Order status distribution includes:

```text
CANCELLED  = 241
CONFIRMED  = 730
DELIVERED  = 3009
PLACED     = 242
SHIPPED    = 778
```

The exact values vary with seed/configuration.

The current status set establishes a useful baseline lifecycle:

```text
PLACED
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

The future model should make status transitions business-coherent rather than independently random.

---

# 4. Order Ownership

## Order owns

- Order identity,
- Customer relationship,
- order timestamp,
- order lifecycle/status,
- order-level transaction context,
- order-level totals where modeled.

## OrderItem owns

- Product relationship,
- quantity,
- line-level price/value,
- item-level transaction information.

## Payment owns

- payment transaction,
- payment method,
- payment status,
- payment-specific information.

## Shipment owns

- fulfillment/shipping process.

## Return owns

- post-purchase return process.

This separation prevents Order from becoming a giant transaction manager.

---

# 5. Customer Relationship

The primary relationship is:

```text
Customer
   │
   │ 1
   │
   └──────────────< Order
```

Conceptually:

```text
Customer 1 → N Order
```

A customer may place:

- no orders,
- occasional orders,
- frequent orders,
- many orders.

The distribution should eventually be driven by CustomerBehaviorProfile.

---

# 6. Current Order Generation

The current generation pipeline creates Orders after Customers and Products exist.

Conceptually:

```text
Customer population
       ↓
Order count
       ↓
Select customer
       ↓
Select order attributes
       ↓
Create Order
       ↓
Generate OrderItems
       ↓
Derive order total
```

The future architecture should preserve this business flow while making customer behavior more realistic.

---

# 7. Order Count vs Customer Count

Current baseline:

```text
1,000 customers
5,000 orders
```

giving:

```text
average orders/customer = 5
```

However, the arithmetic average hides the distribution.

A realistic system should not make every customer place exactly five orders.

Instead:

```text
inactive customers
      ↓
occasional customers
      ↓
regular customers
      ↓
high-frequency customers
```

should produce different order counts.

---

# 8. Customer Order-Frequency Model

The future CustomerBehaviorProfile should contain a purchase-frequency characteristic.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
OrderFrequencyModel
        ↓
orders/customer
```

Possible underlying distributions include:

```text
Poisson-like
negative binomial
zero-inflated
mixture distributions
```

The correct model depends on the intended customer population.

A zero-inflated or mixture model may be especially useful if inactive customers are included.

---

# 9. Heterogeneous Customers

A strong Order model requires customer heterogeneity.

For example:

```text
Customer A
  low activity
  1 order/year

Customer B
  medium activity
  5 orders/year

Customer C
  high activity
  30 orders/year
```

The exact values are not prescribed.

The important property is:

> Order frequency should emerge from customer behavior rather than a global fixed ratio.

---

# 10. Customer Lifecycle

Order generation should eventually reflect lifecycle.

Possible lifecycle states:

```text
new
active
loyal
dormant
churned
reactivated
```

A customer's order probability can depend on lifecycle.

For example:

```text
new
  ↓
initial purchase
  ↓
active
  ↓
loyal
```

or:

```text
active
  ↓
dormant
  ↓
reactivated
```

This is future behavioral realism.

---

# 11. Order Timestamp

Order timestamp is a critical field for future temporal realism.

It should eventually depend on:

```text
customer activity
+
calendar time
+
seasonality
+
campaigns
+
random variation
```

The timestamp should not simply be an independent random value.

---

# 12. Temporal Generation

A future temporal model should generate orders in a business-aware sequence.

Conceptually:

```text
Generation period
       ↓
Customer active periods
       ↓
Purchase opportunities
       ↓
Order creation
       ↓
Order timestamp
```

This is preferable to independently assigning timestamps after order generation.

---

# 13. Time Distribution

A basic baseline may use a broad timestamp distribution.

A mature model can introduce:

```text
day-of-week effects
hour-of-day effects
monthly seasonality
campaign periods
holiday effects
customer lifecycle
```

For example:

```text
weekday vs weekend
morning vs evening
normal month vs campaign month
```

The exact effects should be configurable.

---

# 14. Order Status

The current status set is:

```text
PLACED
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

These statuses should represent a lifecycle rather than independent categorical values.

A plausible lifecycle is:

```text
PLACED
   │
   ├──► CANCELLED
   │
   ▼
CONFIRMED
   │
   ▼
SHIPPED
   │
   ▼
DELIVERED
```

The exact allowed transitions should be explicitly defined.

---

# 15. Invalid Status Combinations

Avoid generating logically inconsistent states.

Examples of suspicious states:

```text
DELIVERED → no shipment
CANCELLED → successful shipment
SHIPPED → impossible timestamp ordering
```

The final rules depend on the Order, Payment, Shipment, and Return models.

Cross-domain validation should verify lifecycle consistency.

---

# 16. Status Generation

The current status distribution is useful as a baseline but should eventually be generated from lifecycle rules.

Instead of:

```text
status = randomChoice(statuses)
```

prefer:

```text
order lifecycle
    ↓
state transition
    ↓
final status
```

This creates coherent relationships with timestamps and downstream entities.

---

# 17. Order State Machine

A future explicit state model could be:

```text
                 ┌──────────────┐
                 │              │
                 ▼              │
PLACED ───────► CONFIRMED ───► SHIPPED ───► DELIVERED
   │
   │
   └──────────────► CANCELLED
```

The exact transition graph should be documented and tested.

A state-machine abstraction is justified only if lifecycle complexity actually requires it.

---

# 18. State Pattern Assessment

The State pattern may become useful if Order lifecycle behavior becomes sufficiently complex.

For example:

```text
PlacedState
ConfirmedState
ShippedState
DeliveredState
CancelledState
```

However, this should not be introduced simply because a state machine exists conceptually.

If a simple transition table is enough, use the simpler model.

The principle remains:

> Use the simplest abstraction that expresses the business rule clearly.

---

# 19. Order Items Relationship

The Order → OrderItem relationship is:

```text
Order 1
  ↓
OrderItem N
```

An Order should contain at least one OrderItem in the normal clean baseline.

The current baseline produces:

```text
mean items/order = 2.40
min = 2
max = 3
```

This is too narrow for mature realism.

---

# 20. Order Size Distribution

Future OrderItem count should be heterogeneous.

Potential distribution:

```text
1 item
2 items
3 items
4 items
5+ items
```

The exact distribution depends on the intended e-commerce scenario.

The important requirement is:

> Order size should be generated from a business distribution rather than a narrow hard-coded range.

---

# 21. Order Size and Customer Behavior

Customer behavior can influence order size.

For example:

```text
high-value customer
    → potentially larger baskets

occasional customer
    → potentially smaller baskets
```

But this should not be deterministic.

A customer can still place unusually large or small orders.

---

# 22. Order Value

Order value should emerge from OrderItems.

The correct relationship is:

```text
Order
   ↓
OrderItems
   ↓
Product prices
   ↓
Quantities
   ↓
Line totals
   ↓
Order total
```

Do not independently generate an Order total and then generate OrderItems.

That produces internally inconsistent synthetic data.

---

# 23. Current Monetary Problem

The baseline currently produces:

```text
Average order value ≈ ₹226,377
Median order value ≈ ₹144,080
Maximum ≈ ₹1,753,065
```

This is too high for a broad generic e-commerce baseline.

The problem is primarily upstream in Product pricing and/or Product selection.

Order should therefore consume a better Product pricing model rather than compensate with arbitrary Order totals.

---

# 24. Order Total Responsibility

Order may expose or store an order total, but the total should be derived from OrderItems.

Conceptually:

```scala
val total =
  items.map(_.lineTotal).sum
```

The exact implementation depends on the current model.

The important invariant is:

```text
order.total == sum(orderItems)
```

within the chosen monetary precision rules.

---

# 25. Monetary Precision

Money should not be represented with binary floating-point arithmetic where exact currency semantics matter.

A future implementation should consider:

```text
BigDecimal
```

with explicit rounding rules.

The current implementation should be inspected before changing the type globally.

Any monetary refactor should be isolated and tested carefully.

---

# 26. Discounts and Transaction Pricing

The final paid price may differ from Product catalog price.

Potential flow:

```text
Product base price
       ↓
Promotion
       ↓
Discount
       ↓
OrderItem transaction price
       ↓
Line total
```

Order should consume the resulting transaction values.

It should not own the entire pricing engine.

---

# 27. Customer Spending Behavior

CustomerBehaviorProfile should eventually influence the distribution of selected Product prices.

Conceptually:

```text
Customer spending profile
       ↓
price sensitivity
       ↓
product selection
       ↓
basket value
       ↓
order value
```

This creates a meaningful relationship between customer behavior and order value.

---

# 28. Customer AOV

A future customer profile may have a latent spending tendency.

For example:

```text
low spender
medium spender
high spender
```

But this should produce distributions rather than exact fixed AOVs.

The final observed AOV should still vary from order to order.

---

# 29. Order Frequency and Order Value

A sophisticated customer model should allow separate but potentially correlated dimensions:

```text
purchase frequency
spending level
```

A customer may be:

```text
high frequency + low value
```

or:

```text
low frequency + high value
```

or:

```text
high frequency + high value
```

This creates richer customer heterogeneity.

---

# 30. Order and Product Popularity

Product popularity should influence what appears in OrderItems.

Therefore:

```text
ProductPopularityModel
        ↓
OrderItem Product selection
```

rather than:

```text
Order
  ↓
uniform Product choice
```

The final order dataset should exhibit the intended long-tail demand.

---

# 31. Category and Brand Influence

The Product selection flow should eventually be:

```text
Customer behavior
        ↓
Category selection
        ↓
Brand selection
        ↓
Product selection
        ↓
OrderItem
```

This means Order should not itself select categories or brands.

Order coordinates the transaction.

Product selection belongs to the catalog/behavior boundary.

---

# 32. Order and Address

An Order may eventually reference the Address used for fulfillment.

Potential future model:

```text
Order
  ↓
shippingAddressId
  ↓
Address
```

and potentially:

```text
billingAddressId
```

if the business model requires it.

The current Order model should not be expanded until this relationship is explicitly designed.

---

# 33. Address Snapshot vs Reference

A future decision is whether an Order should:

```text
reference saved Address
```

or:

```text
store an address snapshot
```

A reference reflects current address identity.

A snapshot preserves historical transaction context even if the customer later changes the address.

For realistic transactional history, snapshots may eventually be important.

This should be decided during detailed Order/Shipment design.

---

# 34. Order and Payment

Relationship:

```text
Order
  ↓
Payment
```

The common baseline may be approximately:

```text
1 Payment / Order
```

but payment behavior may eventually include:

- failed attempts,
- retries,
- multiple payment attempts,
- refunds.

These belong to Payment.

Order should not implement payment state machines.

---

# 35. Order and Shipment

Relationship:

```text
Order
  ↓
Shipment
```

A baseline may use approximately:

```text
1 Shipment / Order
```

Future realism may include:

```text
multiple shipments
partial fulfillment
delivery delay
cancellation before shipment
```

These belong primarily to Shipment and Order lifecycle coordination.

---

# 36. Order and Return

Returns occur after or around fulfillment.

Conceptually:

```text
Order
  ↓
OrderItem
  ↓
Return
```

A future model should allow:

```text
no return
partial return
multiple returned items
```

rather than treating Return as a simple random property of the Order.

---

# 37. Return Probability

Return probability should eventually depend on:

```text
customer return propensity
+
product/category characteristics
+
order context
+
delivery context
```

The Return domain owns the actual return decision.

Order provides transaction context.

---

# 38. Order and Session

The customer journey should eventually connect:

```text
Session
   ↓
Events
   ↓
Purchase intent
   ↓
Order
```

A mature model may associate an Order with the session or event path that produced it.

This is an important future behavioral correlation.

---

# 39. Conversion Modeling

Future Order creation can use conversion probability.

Conceptually:

```text
Session activity
    ↓
engagement
    ↓
cart behavior
    ↓
checkout
    ↓
conversion probability
    ↓
Order
```

This would connect Sessions/Events with Orders.

The current generator does not yet have this level of funnel realism.

---

# 40. Order Acquisition Channel

The current baseline has channel categories such as:

```text
DIRECT
EMAIL
ORGANIC
PAID_SEARCH
REFERRAL
SOCIAL
```

These should eventually influence behavior rather than being independent labels.

For example:

```text
PAID_SEARCH
    ↓
higher campaign exposure
    ↓
certain category/product mix
```

This belongs to acquisition/marketing behavior, not the basic Order model.

---

# 41. Order Device

Current baseline includes device categories:

```text
DESKTOP
MOBILE
TABLET
```

These can eventually correlate with:

```text
customer behavior
session type
event behavior
conversion
order value
```

Order should store the relevant transactional attribute if it is part of the current model, but device behavior should be generated by the appropriate behavioral/session layer.

---

# 42. Order Channel and Customer Behavior

Acquisition channel can influence customer behavior.

For example:

```text
channel
  ↓
customer profile
  ↓
product/category affinity
  ↓
orders
```

This creates a more realistic causal chain.

Avoid generating channel independently for every Order if the business model intends channel to represent customer acquisition.

---

# 43. Order Timestamp and Status Consistency

A future Order lifecycle should satisfy temporal rules.

For example:

```text
placedAt <= confirmedAt <= shippedAt <= deliveredAt
```

when those timestamps exist.

For cancelled Orders:

```text
cancelledAt
```

should occur before shipment/delivery if cancellation is pre-fulfillment.

The exact rules depend on the final Order/Shipment model.

---

# 44. State Transition Validation

Validation should detect impossible sequences.

Examples:

```text
DELIVERED before SHIPPED
SHIPPED before CONFIRMED
CANCELLED after DELIVERED
```

unless a business scenario explicitly supports such a state.

---

# 45. Order ID Generation

Order IDs should be:

```text
unique
deterministic
stable
```

Example:

```text
ORDER_000000001
```

The exact format follows the current implementation.

---

# 46. Customer Assignment

Customer selection should eventually use a customer-specific order propensity.

Avoid:

```text
random customer for every order
```

with no relationship to activity.

Prefer:

```text
CustomerBehaviorProfile
        ↓
order propensity
        ↓
customer order count
```

This creates natural concentration.

---

# 47. Order Frequency Distribution

A realistic dataset should eventually exhibit:

```text
many customers with few orders
fewer customers with moderate orders
small number with many orders
```

This long-tail customer activity is useful for both realism and Spark skew experiments.

---

# 48. Customer Order Concentration

Useful statistics:

```text
orders/customer
p50
p75
p90
p95
p99
max
```

and:

```text
top 1% customers' order share
top 5% customers' order share
```

This measures whether customer activity is truly heterogeneous.

---

# 49. Order Skew

Order is a natural domain for controlled customer-key skew.

A scenario can create:

```text
hot customers
```

where a small number of customers generate a large percentage of Orders.

This is highly valuable for Spark join/aggregation experiments.

---

# 50. Hot Customer Scenario

Conceptually:

```text
HotCustomerScenario
        ↓
CustomerActivityModel
        ↓
order frequency
        ↓
Orders
```

The scenario should modify the behavior model rather than contain special Order-generation branches.

---

# 51. Promotion Spike

Another useful scenario:

```text
Promotion
   ↓
product/category demand multiplier
   ↓
customer conversion
   ↓
Order volume spike
```

This should produce a temporal concentration of Orders.

---

# 52. Seasonal Orders

Seasonality can modify:

```text
order arrival rate
product selection
order value
```

For example:

```text
campaign period
   ↓
higher order rate
```

The temporal engine should own the time-dependent effect.

---

# 53. Order Data-Quality Scenarios

Potential controlled defects:

```text
duplicate Order ID
unknown customer ID
invalid status
invalid timestamp
negative total
inconsistent total
```

The clean Order generator should not produce these accidentally.

The Quality Engine should inject them intentionally.

---

# 54. Output Responsibility

Order should not write CSV.

The output layer serializes Order records.

Current output includes:

```text
orders.csv
```

The schema should remain independent from Order generation implementation.

---

# 55. Statistics Responsibility

Order generation should not calculate global statistics.

The statistics layer should measure:

```text
orders/customer
order value
order status
orders over time
orders by category
orders by product
orders by channel
orders by device
```

This separation is important for validation and calibration.

---

# 56. Configuration Responsibility

Order configuration should be loaded centrally.

Preferred:

```text
HOCON
  ↓
ConfigLoader
  ↓
typed Order configuration
  ↓
Order generation behavior
```

The Order generator should not parse HOCON directly.

---

# 57. Potential Order Configuration

A future configuration may expose:

```hocon
order {
  count = 5000

  frequency {
    model = "customer-driven"
  }

  items {
    distribution = "variable"
  }

  lifecycle {
    model = "state-based"
  }

  timestamps {
    model = "temporal"
  }
}
```

The exact configuration should be finalized only after the current implementation is inspected and the behavioral models are designed.

---

# 58. Strategy Pattern

Order has several genuine behavioral strategy candidates:

```text
OrderFrequencyModel
OrderSizeModel
OrderTimestampModel
OrderLifecycleModel
ConversionModel
```

Not all belong physically inside `order/`.

For example:

```text
CustomerBehavior
    → OrderFrequencyModel

Temporal
    → OrderTimestampModel

Order
    → lifecycle
```

The domain boundary should follow business ownership.

---

# 59. Factory Pattern

Factories can be useful for selecting configured behavioral models.

For example:

```text
OrderLifecycleModelFactory
```

or:

```text
OrderSizeModelFactory
```

Only introduce them when multiple concrete strategies exist.

Do not create factories around simple case-class construction.

---

# 60. Composition

Order generation should be composed from focused responsibilities.

Conceptually:

```text
customer order propensity
+
timestamp model
+
order-size model
+
product selector
+
pricing
+
lifecycle
        ↓
Order + OrderItems
```

This is preferable to one giant:

```text
OrderGenerator
```

containing every rule.

---

# 61. Domain Service Assessment

A domain/application service may become appropriate for:

```text
create complete transaction
```

because this operation spans:

```text
Customer
Order
OrderItems
Payment
Shipment
```

However, the service should orchestrate.

It should not absorb every entity's business logic.

---

# 62. Transaction Orchestration

A future application-level transaction workflow may be:

```text
Customer selected
       ↓
Order context created
       ↓
Order items selected
       ↓
Transaction prices calculated
       ↓
Order total derived
       ↓
Order lifecycle determined
       ↓
Payment generated
       ↓
Shipment generated
       ↓
Potential return generated
```

This is application orchestration.

Each domain remains responsible for its own behavior.

---

# 63. SOLID Assessment

## Single Responsibility

Avoid making OrderGenerator responsible for:

```text
payment
shipment
return
CSV
statistics
```

---

## Open/Closed

Order behavior should be extensible through meaningful models.

---

## Liskov Substitution

Relevant when multiple Order strategies share contracts.

---

## Interface Segregation

Avoid one enormous transaction interface.

---

## Dependency Inversion

Order generation should depend on narrow behavior/reference boundaries.

---

# 64. Readability Standard

Preferred:

```scala
val customer =
  customerSelector.selectForOrder(context)

val items =
  orderItemGenerator.generate(
    customerProfile,
    context
  )

val total =
  orderTotalCalculator.calculate(items)

val status =
  orderLifecycle.determine(context)
```

The exact implementation may differ.

The principle is that the code should read like a transaction.

---

# 65. Testing Strategy

## Model tests

Verify:

- Order fields,
- equality,
- required invariants.

---

## Generator tests

Verify:

- unique IDs,
- valid customer references,
- valid timestamps,
- valid statuses,
- deterministic generation.

---

## Order-item integration

Verify:

```text
every Order has valid OrderItems
```

and:

```text
order total = sum of line totals
```

---

## Lifecycle tests

Verify valid state transitions.

---

## Temporal tests

Verify timestamp ordering.

---

## Customer behavior tests

Verify:

```text
high-activity customers
→ higher expected order counts
```

once the behavior model is implemented.

---

# 66. Statistical Testing

Important Order metrics:

```text
orders/customer
items/order
order value
orders/day
orders/hour
status distribution
channel distribution
device distribution
```

Distribution tests should use tolerance ranges.

Do not assert exact counts for probabilistic models unless the behavior is intentionally deterministic.

---

# 67. Monetary Regression Testing

Because Order value is currently unrealistic, Product/Order changes should track:

```text
mean
median
p95
p99
maximum
```

A future regression test should prevent accidental return to extreme values.

---

# 68. Reproducibility

Order generation should satisfy:

```text
same seed
+
same configuration
+
same reference data
+
same behavior models
+
same time configuration
```

→ same logical Orders.

Derived random streams should isolate major business decisions.

---

# 69. Parallel Generation

Future high-volume generation may parallelize Orders.

The design should therefore avoid shared mutable state.

Preferred:

```text
immutable customer/reference data
+
independent customer/order random streams
+
immutable generated records
```

This makes parallel generation easier to reason about.

---

# 70. Performance Considerations

At large scale, Order generation may become one of the dominant workloads.

Potential optimizations include:

```text
precomputed customer order weights
efficient Product selection
precomputed distributions
batch generation
```

Optimization should occur after behavior is statistically correct.

---

# 71. Product Selection Performance

OrderItem generation should not repeatedly scan every Product for every item.

A future Product selection model may provide:

```text
precomputed category candidates
precomputed brand candidates
weighted product indexes
```

The Order domain should consume that abstraction rather than implement catalog indexing itself.

---

# 72. Order Generation and Memory

For very large datasets, the generator may eventually need streaming or partitioned output.

The Order domain should not assume that the entire generated dataset must be held in memory.

This is an output/generation architecture concern.

---

# 73. Current Simplifications

The current Order implementation intentionally simplifies:

- customer order-frequency heterogeneity,
- temporal behavior,
- lifecycle state transitions,
- order-size distribution,
- session-to-order conversion,
- customer-specific AOV,
- promotion effects,
- seasonality,
- multi-shipment orders,
- payment retries,
- partial returns.

These are future capabilities.

---

# 74. Planned Realism Sequence

Recommended Order realism sequence:

```text
1. Stabilize Order model
2. Improve customer order frequency
3. Improve order-size distribution
4. Calibrate Product pricing
5. Derive order totals from items
6. Improve status lifecycle
7. Introduce temporal order generation
8. Connect session conversion
9. Introduce customer spending correlation
10. Introduce promotion/seasonality
11. Introduce richer payment/shipment/return relationships
```

This order minimizes cross-domain confusion.

---

# 75. Order Migration

When migrating Order into the domain architecture:

## Step 1

Inspect current Order model.

## Step 2

Inspect current Order generator.

## Step 3

Inspect OrderItem generation dependencies.

## Step 4

Inspect current status logic.

## Step 5

Inspect pricing and total calculation.

## Step 6

Inspect Customer selection.

## Step 7

Define target responsibilities.

## Step 8

Move Order model into:

```text
order/model
```

## Step 9

Move Order-specific generation into:

```text
order/generator
```

or more precise packages if justified.

## Step 10

Move lifecycle logic into an explicit boundary if complexity justifies it.

## Step 11

Move Order validation.

## Step 12

Update OrderItem dependencies.

## Step 13

Update Payment/Shipment/Return orchestration.

## Step 14

Run Order tests.

## Step 15

Run dependent-domain tests.

## Step 16

Run complete suite.

## Step 17

Run end-to-end generation.

## Step 18

Compare transaction statistics against the baseline.

---

# 76. Order Migration Quality Gate

Order migration is complete when:

### Business

- Order meaning is explicit.
- Customer relationship is explicit.
- OrderItem relationship is explicit.
- lifecycle is defined.

### Architecture

- Order does not own Payment implementation.
- Order does not own Shipment implementation.
- Order does not own Return implementation.
- Order does not write output.
- Order does not own global statistics.

### Data

- IDs unique,
- customer references valid,
- timestamps valid,
- statuses valid,
- totals coherent.

### Reproducibility

- same inputs reproduce the same transaction population.

### Testing

- Order tests pass,
- OrderItem tests pass,
- downstream integration tests pass,
- complete suite passes.

### Statistics

- orders/customer measured,
- items/order measured,
- order value measured,
- status distribution measured,
- temporal distribution measured.

---

# 77. Design Decisions

## Decision A — Order is the transaction boundary

It represents the commercial transaction.

---

## Decision B — OrderItem represents purchased lines

Product selection and quantity belong at OrderItem level.

---

## Decision C — Order totals emerge from OrderItems

Do not independently generate totals.

---

## Decision D — Customer behavior drives order frequency

The long-term model should not use a fixed orders/customer ratio.

---

## Decision E — Order lifecycle should be state-aware

Statuses should represent valid transitions.

---

## Decision F — Time is a first-class concern

Order timestamps should eventually reflect customer activity and temporal demand.

---

## Decision G — Payment, Shipment, Return remain separate domains

Order coordinates the transaction without absorbing their business logic.

---

## Decision H — Scenario behavior stays compositional

Promotion, seasonality, and skew should modify behavior models rather than create scattered Order-specific branches.

---

## Decision I — Avoid premature state-pattern complexity

Use a state machine or State pattern only if lifecycle complexity justifies it.

---

# 78. Final Mental Model

The simplest business view is:

```text
Customer
   │
   ▼
Order
   │
   ├──────────────► Payment
   │
   ├──────────────► Shipment
   │
   ├──────────────► Return
   │
   ▼
OrderItem
   │
   ▼
Product
```

The realistic generation flow becomes:

```text
CustomerBehaviorProfile
        │
        ├── order frequency
        ├── spending level
        ├── activity
        └── preferences
                │
                ▼
            Order creation
                │
                ├── timestamp
                ├── lifecycle
                └── order size
                        │
                        ▼
                    OrderItems
                        │
                        ├── Product
                        ├── Quantity
                        └── Transaction Price
                                │
                                ▼
                           Order Total
```

Then:

```text
Order
 ├── Payment
 ├── Shipment
 └── Return
```

---

# 79. Summary

Order is the central transactional domain of ShopSphere.

It connects customer behavior to concrete commercial transactions and then to operational processes.

The current baseline contains approximately:

```text
1,000 Customers
5,000 Orders
12,000 OrderItems
```

with:

```text
mean items/order = 2.40
```

and an unrealistically high monetary distribution:

```text
mean AOV ≈ ₹226K
median AOV ≈ ₹144K
```

The solution is not to invent arbitrary Order totals.

The correct architecture is:

```text
Customer behavior
        ↓
Order frequency
        ↓
Order
        ↓
OrderItems
        ↓
Product pricing
        ↓
Order total
```

The major future improvements are:

```text
heterogeneous customer order frequency
        ↓
variable basket sizes
        ↓
realistic Product prices
        ↓
coherent order totals
        ↓
state-aware lifecycle
        ↓
temporal order generation
        ↓
session-to-order conversion
        ↓
customer spending correlation
        ↓
promotion/seasonality
```

Architecturally, Order should remain a transaction orchestrator rather than becoming a container for every downstream business process.

The target is:

> **A transaction model where Orders emerge naturally from customer behavior, product selection, pricing, time, and lifecycle rules, while Payment, Shipment, Return, and OrderItem remain independently understandable domains.**

The next domain is **OrderItem**, which is especially important because it is the physical bridge between the Product catalog and Order economics: quantity, product selection, line pricing, basket size, and product popularity all meet there.
