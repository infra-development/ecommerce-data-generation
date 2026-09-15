# 12 — Shipment Domain

## 1. Purpose

This document defines the business meaning, lifecycle, relationships, generation strategy, fulfillment behavior, geography interaction, delivery timing, carrier behavior, failure modes, validation, realism, configuration, architecture, testing, and migration plan for the **Shipment** domain in the ShopSphere e-commerce data generator.

Shipment represents the operational fulfillment and delivery process associated with an Order.

The central relationship is:

```text
Order
  ↓
Shipment
  ↓
Delivery
```

with geography influencing the physical journey:

```text
Customer
  ↓
Address
  ↓
Geography
  ↓
Shipment destination
  ↓
Delivery behavior
```

The central architectural principle is:

> **Shipment represents fulfillment and delivery behavior; it should consume Order and Address context without becoming responsible for Order creation, Payment processing, Return generation, or global output.**

---

# 2. Business Meaning

A Shipment answers:

> "How is this Order physically fulfilled and delivered to the customer?"

It can represent:

- fulfillment initiation,
- carrier assignment,
- shipment status,
- dispatch,
- delivery,
- delivery timing,
- delivery failure,
- shipment tracking context where modeled.

The exact persisted fields remain governed by the current Shipment model.

Do not expand the schema merely because real logistics systems contain more attributes.

---

# 3. Current Baseline

The current generation plan produces approximately:

```text
5,000 Orders
5,000 Shipments
```

This establishes a baseline relationship close to:

```text
1 Shipment / Order
```

The current generator already treats Shipment as a downstream transactional entity.

The detailed shipment lifecycle and operational realism remain an area for improvement.

---

# 4. Shipment Ownership

## Shipment owns

- Shipment identity,
- Order relationship,
- shipment status,
- fulfillment timing,
- carrier behavior where modeled,
- delivery timing where modeled.

## Order owns

- commercial transaction,
- OrderItems,
- Order lifecycle.

## Address owns

- customer address identity and geographic placement.

## Geography owns

- geographic hierarchy and reference data.

## Payment owns

- payment processing.

## Return owns

- post-delivery return behavior.

Shipment should consume these domains rather than duplicate their responsibilities.

---

# 5. Order Relationship

The current baseline effectively models:

```text
Order 1 → 1 Shipment
```

This is a reasonable initial simplification.

However, real fulfillment can involve:

```text
one Order
   ↓
multiple Shipments
```

when items are:

- fulfilled separately,
- shipped from different locations,
- backordered,
- split by availability.

Multiple shipments are a future realism feature.

---

# 6. Shipment vs Delivery

These concepts should be distinguished.

Shipment represents:

> the fulfillment/delivery process.

Delivery represents:

> the physical completion of that process at the destination.

The current schema may not persist a separate Delivery entity.

That is acceptable.

Delivery can initially be represented through Shipment status and timestamps.

Do not introduce a separate Delivery domain until the business model requires it.

---

# 7. Shipment Lifecycle

A conceptual lifecycle is:

```text
CREATED
   ↓
PACKED
   ↓
SHIPPED
   ↓
OUT_FOR_DELIVERY
   ↓
DELIVERED
```

Potential exception states include:

```text
CANCELLED
FAILED
RETURNED
```

The exact status set must follow the current model.

---

# 8. Lifecycle vs Independent Status

Shipment status should represent a lifecycle.

Avoid:

```text
status = randomChoice(allStatuses)
```

because this can generate impossible combinations.

Prefer:

```text
shipment lifecycle
       ↓
state transition
       ↓
final state
```

This also makes timestamps easier to generate correctly.

---

# 9. State Machine

A useful conceptual state graph is:

```text
CREATED
   │
   ▼
PACKED
   │
   ▼
SHIPPED
   │
   ▼
OUT_FOR_DELIVERY
   │
   ├──────────────► FAILED
   │
   ▼
DELIVERED
```

Cancellation may be possible before shipment:

```text
CREATED → CANCELLED
PACKED  → CANCELLED
```

depending on business rules.

---

# 10. State Pattern Assessment

The State pattern is not required for the baseline.

A status enum plus transition validation is likely sufficient initially.

A State implementation becomes justified if Shipment eventually has substantial state-specific behavior such as:

```text
retry delivery
reschedule
partial fulfillment
carrier exception
return-to-origin
```

The architecture should remain simple until that complexity exists.

---

# 11. Order Lifecycle Relationship

Shipment should be coherent with Order status.

A simplified relationship may be:

```text
Order CONFIRMED
       ↓
Shipment CREATED
       ↓
Shipment SHIPPED
       ↓
Order SHIPPED
       ↓
Shipment DELIVERED
       ↓
Order DELIVERED
```

The exact transition mapping should be explicitly defined.

---

# 12. Payment Relationship

If ShopSphere requires successful payment before fulfillment:

```text
Order
  ↓
Payment SUCCESS
  ↓
Shipment
```

This is an orchestration rule.

Shipment should not independently inspect or generate Payment internals.

---

# 13. Shipment Timestamp Model

A realistic Shipment requires temporal relationships.

Potential timestamps include:

```text
created
packed
shipped
out-for-delivery
delivered
```

Only fields supported by the current model should be persisted.

The important future behavior is the ordering:

```text
created <= packed <= shipped <= outForDelivery <= delivered
```

where those stages exist.

---

# 14. Delivery Time

Delivery time should not be an independent random timestamp.

It should emerge from:

```text
shipment time
+
delivery duration
```

Conceptually:

```text
shipTime
   ↓
DeliveryTimeModel
   ↓
delivery time
```

---

# 15. Delivery Duration

Delivery duration can depend on:

```text
origin
destination
distance
geography
carrier
service level
random variation
```

The initial implementation can use a simpler model.

The future architecture should leave room for these factors.

---

# 16. Geography Relationship

Shipment is the first major domain where Geography becomes operationally meaningful.

The flow is:

```text
Customer
   ↓
Address
   ↓
Building
   ↓
Society
   ↓
Road
   ↓
Area
   ↓
City
   ↓
State
   ↓
Country
```

Shipment can use this hierarchy to determine delivery behavior.

---

# 17. Address Relationship

Shipment should eventually reference the destination Address.

Conceptually:

```text
Shipment
   ↓
Address
   ↓
Geography
```

This prevents Shipment from duplicating:

```text
city
state
country
postal code
```

when those are already represented by the Address/Geography model.

---

# 18. Current Address Model

The current Address model is:

```text
id
customerId
buildingId
unitNumber
postalCode
```

Shipment should respect this existing design.

It should not introduce duplicate location fields simply to make Shipment self-contained.

---

# 19. Postal Code and Delivery

Postal code belongs to the geographic Area.

Therefore delivery behavior can eventually use:

```text
postalCode
  ↓
Area
  ↓
City
  ↓
State
```

to derive delivery characteristics.

---

# 20. Geographic Delivery Difficulty

Different geographic areas may have different delivery characteristics.

A future model can represent:

```text
delivery difficulty
```

through reference data or behavioral models.

For example:

```text
urban area
    → shorter typical delivery time

remote area
    → longer typical delivery time
```

This should remain probabilistic.

---

# 21. Carrier

A mature Shipment model may contain or derive:

```text
carrier
```

The current model should not be expanded until carrier behavior is explicitly designed.

Carrier selection can eventually depend on:

```text
destination
service level
order characteristics
scenario
```

---

# 22. Carrier Selection

Potential strategy:

```text
CarrierSelectionModel
```

Conceptually:

```text
Shipment context
      ↓
CarrierSelectionModel
      ↓
Carrier
```

The model can use geographic compatibility.

---

# 23. Carrier Performance

A future carrier model can contain behavioral parameters such as:

```text
delivery speed
failure probability
delay distribution
coverage
```

This enables realistic differences between carriers.

---

# 24. Carrier and Geography

Carrier coverage can be modeled as:

```text
Carrier
   ↓
supported geography
```

A carrier should not necessarily be equally likely everywhere.

For example:

```text
Carrier A
  strong urban coverage

Carrier B
  strong remote coverage
```

This is future realism.

---

# 25. Service Level

A future shipment may support:

```text
standard
express
same-day
```

if the business scenario requires it.

Service level can affect:

```text
delivery duration
price
carrier selection
failure probability
```

Do not add service-level complexity until the model needs it.

---

# 26. Shipment and Order Value

Order value can potentially influence:

```text
service level
carrier
delivery priority
```

but this should be optional.

Synthetic correlations should be driven by business assumptions, not added simply to make data look correlated.

---

# 27. Shipment Weight

A future Shipment model may derive package characteristics from OrderItems.

For example:

```text
OrderItems
   ↓
product quantities
   ↓
package weight/volume
   ↓
shipment behavior
```

This is not required for the current baseline.

---

# 28. Shipment Size

Future fulfillment realism can include:

```text
item count
quantity
product dimensions
package count
```

These can influence:

```text
shipment count
carrier
delivery time
```

This becomes particularly useful when multiple shipments are introduced.

---

# 29. Split Shipment

A sophisticated Order may be fulfilled through:

```text
Order
 ├── Shipment A
 │     └── items 1–2
 │
 └── Shipment B
       └── items 3–4
```

The current one-shipment-per-order baseline intentionally avoids this complexity.

---

# 30. Shipment Cardinality

Current baseline:

```text
1 Shipment / Order
```

Future distribution could allow:

```text
most Orders → 1 shipment
some Orders → 2 shipments
rare Orders → 3+ shipments
```

The exact distribution should be scenario-configurable.

---

# 31. Shipment and OrderItem

If split shipments are introduced, Shipment should reference or associate with the relevant OrderItems.

Conceptually:

```text
Order
  ↓
OrderItems
  ↓
Shipment assignment
```

This is a future relationship design.

---

# 32. Delivery Delay

Delivery should have natural variation.

A simple future model:

```text
expected delivery duration
+
random variation
```

A richer model:

```text
geography
+
carrier
+
service level
+
season
+
incident
+
random variation
```

---

# 33. Delivery Distribution

Delivery durations should not all be identical.

For example:

```text
most deliveries
    → near expected time

some
    → early

some
    → delayed

rare
    → severely delayed
```

This is a better statistical representation than fixed duration.

---

# 34. Delivery SLA

A future model can define:

```text
promised delivery duration
```

and compare actual:

```text
actual duration
```

to derive:

```text
on-time
late
early
```

This is useful for operational analytics.

---

# 35. SLA Breach

Potential statistic:

```text
late delivery rate
```

can be generated from:

```text
actualDeliveryTime > promisedDeliveryTime
```

This provides an important operational metric.

---

# 36. Delivery Failure

Not every shipment needs to succeed immediately.

Future failure modes can include:

```text
customer unavailable
address issue
carrier exception
weather
operational delay
```

These should be scenario-driven.

---

# 37. Failed Delivery

A failed delivery can result in:

```text
retry
```

or:

```text
return to origin
```

The exact lifecycle should be defined before implementing it.

---

# 38. Return-to-Origin

A possible future lifecycle:

```text
OUT_FOR_DELIVERY
      ↓
FAILED
      ↓
RETRY
      ↓
FAILED
      ↓
RETURNED_TO_ORIGIN
```

This creates a useful relationship between Shipment and Return.

---

# 39. Shipment and Return

Return can happen after:

```text
DELIVERED
```

or after:

```text
failed delivery / return-to-origin
```

depending on business rules.

The Return domain owns the return event.

Shipment provides fulfillment context.

---

# 40. Return Timing

A return should generally occur after delivery if it is a customer-initiated product return.

Conceptually:

```text
deliveredAt
    ↓
returnRequestAt
    ↓
returnProcessedAt
```

The exact fields depend on the Return model.

---

# 41. Customer Behavior

Customer behavior can influence delivery-related behavior indirectly.

Examples:

```text
preferred address
delivery preference
service level preference
```

Only introduce these if they represent actual business requirements.

Shipment should not become a second CustomerBehaviorProfile.

---

# 42. Geography Affinity

A customer's address determines destination geography.

That geography can influence:

```text
carrier
delivery time
delivery success
```

This is a useful natural correlation because it follows the business process.

---

# 43. Temporal Seasonality

Shipment performance can vary by time.

Examples:

```text
festival season
sale period
holiday period
weather event
```

These can produce:

```text
higher shipment volume
longer delivery time
higher delay rate
```

This is a strong future scenario for operational realism.

---

# 44. Promotion Surge

A major promotion can cause:

```text
Order volume ↑
      ↓
Shipment volume ↑
      ↓
Operational load ↑
      ↓
Delivery delay ↑
```

This creates a meaningful causal chain.

---

# 45. Operational Incident Scenario

A future scenario can model:

```text
carrier incident
```

with:

```text
specific carrier
+
specific period
+
specific geography
```

producing elevated delays/failures.

This is much more realistic than uniformly increasing every shipment's delay.

---

# 46. Skew Scenario

Shipment can also support controlled skew.

Examples:

```text
hot geography
hot carrier
hot postal code
```

This can produce uneven distribution for Spark workloads.

---

# 47. Hot Geography

A scenario may generate disproportionate shipment volume to one:

```text
city
area
postal code
```

This can be useful for:

```text
groupBy(postal_code)
```

and geography-based joins.

---

# 48. Hot Carrier

A hot-carrier scenario can create:

```text
carrier A
  → large majority of shipments
```

which can be used to study aggregation skew.

---

# 49. Data Quality

Potential Shipment defects:

```text
duplicate shipment ID
unknown order ID
unknown address ID
invalid status
invalid timestamp
delivery before shipment
invalid carrier
```

These should be intentional scenario defects.

---

# 50. Clean Baseline

The clean Shipment generator should guarantee:

```text
unique shipment ID
valid Order reference
valid Address reference when modeled
valid lifecycle
valid timestamps
```

---

# 51. Referential Integrity

Validate:

```text
Shipment.orderId exists
```

and if Address is referenced:

```text
Shipment.addressId exists
```

Cross-domain validation should verify the destination belongs to the expected customer/order context.

---

# 52. Customer Address Consistency

If an Order uses a particular customer address:

```text
Order
  ↓
Address
  ↓
Customer
```

Shipment should not point to an unrelated customer's Address.

This is a cross-domain business invariant.

---

# 53. Timestamp Validation

Examples:

```text
shipmentCreatedAt <= shippedAt
shippedAt <= deliveredAt
```

where those timestamps exist.

Invalid temporal sequences should fail validation.

---

# 54. Order/Shipment Validation

Examples:

```text
DELIVERED Order
    → corresponding Shipment delivered
```

and:

```text
Order SHIPPED
    → Shipment should be shipped or later
```

The exact mapping should follow the final lifecycle design.

---

# 55. Statistical Validation

Shipment statistics should include:

```text
shipments/order
shipments/day
delivery duration
median delivery duration
p95 delivery duration
late delivery rate
failure rate
carrier share
geography share
```

---

# 56. Delivery Distribution Validation

A realistic distribution should avoid:

```text
every shipment = exactly 2 days
```

unless that is an intentional scenario.

Instead measure:

```text
p50
p75
p90
p95
p99
max
```

of delivery duration.

---

# 57. Carrier Validation

If carrier weights are configured:

```text
configured carrier share
≈
observed carrier share
```

within an acceptable tolerance.

If carrier choice is geography-conditioned, validate the conditional distribution as well.

---

# 58. Geography Validation

Useful metrics:

```text
shipments/city
shipments/area
shipments/postal code
```

and:

```text
delivery duration by geography
```

These help verify that geography actually affects operational behavior.

---

# 59. SLA Validation

If SLA modeling exists:

```text
late delivery rate
```

should remain close to configured expectations.

Also measure:

```text
late rate by carrier
late rate by geography
```

when those factors are modeled.

---

# 60. Strategy Pattern

Shipment has legitimate strategy candidates:

```text
CarrierSelectionModel
DeliveryDurationModel
ShipmentSplitModel
DeliveryOutcomeModel
```

These represent real behavior variation.

---

# 61. CarrierSelectionModel

This answers:

> "Which carrier fulfills this shipment?"

Potential inputs:

```text
destination geography
service level
order characteristics
scenario
```

---

# 62. DeliveryDurationModel

This answers:

> "How long does delivery take?"

Potential inputs:

```text
origin
destination
carrier
service level
calendar time
scenario
```

---

# 63. ShipmentSplitModel

This answers:

> "Does an Order become one or multiple Shipments?"

The baseline can simply return:

```text
one shipment
```

Future implementations can introduce split fulfillment.

---

# 64. DeliveryOutcomeModel

This answers:

> "Does the shipment deliver successfully and on time?"

Potential outcomes:

```text
DELIVERED
DELAYED
FAILED
RETURNED
```

depending on the lifecycle.

---

# 65. Factory Pattern

Factories can select configured strategies:

```text
CarrierSelectionModelFactory
DeliveryDurationModelFactory
ShipmentSplitModelFactory
```

Use them only when there are meaningful alternatives.

---

# 66. Builder Assessment

Builder is generally unnecessary for Shipment.

Use immutable case classes for straightforward record construction.

---

# 67. Composition

Prefer:

```text
ShipmentGenerator
   ├── CarrierSelectionModel
   ├── DeliveryDurationModel
   ├── ShipmentSplitModel
   └── DeliveryOutcomeModel
```

rather than a large ShipmentGenerator containing every logistics rule.

---

# 68. Dependency Injection

Constructor injection is preferred.

Conceptually:

```scala
class ShipmentGenerator(
    carrierSelectionModel: CarrierSelectionModel,
    deliveryDurationModel: DeliveryDurationModel,
    shipmentSplitModel: ShipmentSplitModel,
    deliveryOutcomeModel: DeliveryOutcomeModel
)
```

No DI framework is necessary.

---

# 69. SOLID — Single Responsibility

ShipmentGenerator should not own:

```text
Order creation
Payment processing
Return generation
CSV output
global statistics
```

---

# 70. SOLID — Open/Closed

New carrier or delivery behavior should be introduced through strategy boundaries where genuine variation exists.

---

# 71. SOLID — Liskov

Concrete shipment strategies should preserve their contracts.

---

# 72. SOLID — Interface Segregation

Avoid a giant logistics interface.

Keep:

```text
carrier selection
delivery timing
shipment splitting
delivery outcome
```

separate where substitution is needed.

---

# 73. SOLID — Dependency Inversion

Shipment generation should depend on behavior abstractions rather than hard-coded logistics algorithms when the behavior is configurable.

---

# 74. Readability Standard

The code should communicate fulfillment.

Preferred conceptual flow:

```scala
val destination =
  addressResolver.resolve(order.addressId)

val carrier =
  carrierSelectionModel.select(
    order,
    destination
  )

val duration =
  deliveryDurationModel.determine(
    order,
    destination,
    carrier
  )

val outcome =
  deliveryOutcomeModel.determine(
    order,
    destination,
    carrier
  )

shipmentFactory.create(
  order,
  destination,
  carrier,
  duration,
  outcome
)
```

The exact implementation depends on the final model.

---

# 75. Geography Dependency Direction

Shipment may depend on Geography reference services.

Geography should not depend on Shipment.

Preferred direction:

```text
Geography
    ↑
Address
    ↑
Shipment
```

or more precisely:

```text
Shipment
   → Address/Geography lookup
```

without circular dependencies.

---

# 76. Reference Data

Shipment should consume immutable geography/reference data.

Do not duplicate the geography hierarchy inside Shipment.

---

# 77. Testing Strategy

## Model tests

Verify:

- required fields,
- equality,
- valid status representation.

## Generator tests

Verify:

- unique IDs,
- valid Order references,
- valid Address references,
- deterministic generation.

## Lifecycle tests

Verify valid state transitions.

## Temporal tests

Verify timestamp ordering.

## Geography tests

Verify destination relationships.

---

# 78. Carrier Tests

If carrier selection is configurable:

```text
same seed
+
same inputs
→
same carrier
```

and:

```text
configured weights
→
observed distribution within tolerance
```

---

# 79. Delivery Duration Tests

Test:

```text
duration > 0
```

and where modeled:

```text
duration responds to geography
```

For example, remote areas should have a statistically different expected distribution only if that rule is explicitly configured.

---

# 80. Integration Tests

Verify:

```text
Order
  ↓
Shipment
  ↓
Address
```

and:

```text
Order
  ↓
Payment
  ↓
Shipment
```

where the business lifecycle requires payment before fulfillment.

---

# 81. Reproducibility

Shipment generation should satisfy:

```text
same seed
same configuration
same Orders
same Address/reference data
```

→ same Shipments.

Derived random streams should isolate:

```text
carrier
timing
outcome
```

---

# 82. Parallel Generation

Shipment generation can naturally be partitioned by Order.

Conceptually:

```text
Order partition
      ↓
Shipment generation
      ↓
Shipment records
```

Avoid global mutable counters where possible.

---

# 83. Shipment ID Generation

Shipment IDs should be:

```text
unique
deterministic
stable
```

The exact format should follow the existing model.

---

# 84. Performance

At high scale, useful optimizations include:

```text
precomputed carrier weights
indexed geography lookup
efficient duration sampling
local random streams
batch generation
```

The generator should avoid repeatedly traversing the entire geography hierarchy for every Shipment if an indexed lookup can provide the same result.

---

# 85. Geography Lookup Performance

The Geography domain should expose efficient lookup operations such as:

```text
building → hierarchy
area → postal codes
postal code → area
```

Shipment should consume these operations.

It should not recreate lookup indexes.

---

# 86. Output Boundary

Shipment should not write:

```text
shipments.csv
```

directly.

The output layer owns serialization.

---

# 87. Statistics Boundary

Shipment generation should not calculate:

```text
average delivery time
late delivery rate
carrier share
```

The statistics layer owns those measurements.

---

# 88. Manifest Boundary

The manifest records:

```text
shipment row count
schema metadata
generation metadata
```

Shipment generation does not own manifest construction.

---

# 89. Spark Performance Laboratory

Shipment supports useful Spark workloads:

```text
Shipment ↔ Order
Shipment ↔ Address
groupBy(carrier)
groupBy(city)
groupBy(postal_code)
delivery-duration analysis
late-delivery analysis
```

---

# 90. Geography Join Workload

A useful Spark exercise is:

```text
Shipment
   JOIN Address
   JOIN Geography
```

followed by:

```text
groupBy(city)
```

or:

```text
groupBy(postal_code)
```

This creates realistic multi-table joins.

---

# 91. Carrier Analytics

Potential Spark workloads:

```text
delivery time by carrier
failure rate by carrier
shipment volume by carrier
late rate by carrier
```

These become more interesting once carrier behavior is heterogeneous.

---

# 92. Temporal Analytics

Potential workloads:

```text
shipments/day
shipments/hour
late deliveries by day
delivery duration by month
incident-period comparison
```

This supports window and time-series Spark exercises.

---

# 93. Skew Workloads

Potential scenarios:

```text
hot carrier
hot geography
hot postal code
```

can create aggregation or join skew.

This should remain controlled and reproducible.

---

# 94. Current Simplifications

The current Shipment implementation simplifies:

- one shipment per Order,
- carrier behavior,
- delivery duration,
- geography-conditioned delivery,
- service levels,
- split shipments,
- delivery failures,
- retries,
- SLA,
- operational incidents,
- package characteristics,
- carrier-specific performance.

These are future realism capabilities.

---

# 95. Planned Realism Sequence

Recommended progression:

```text
1. Preserve current Shipment model
2. Stabilize Order relationship
3. Add coherent shipment lifecycle
4. Add delivery timing
5. Connect Address/Geography
6. Add carrier behavior
7. Add delivery variability
8. Add SLA modeling
9. Add failure/retry behavior
10. Add split shipments
11. Add seasonal/incident effects
12. Add package characteristics
```

---

# 96. Migration Strategy

## Step 1

Inspect current Shipment model.

## Step 2

Inspect current ShipmentGenerator.

## Step 3

Inspect current Order relationship.

## Step 4

Inspect current Address relationship.

## Step 5

Inspect current status logic.

## Step 6

Inspect current timestamp generation.

## Step 7

Define target responsibilities.

## Step 8

Move model into:

```text
shipment/model
```

## Step 9

Move generation into:

```text
shipment/generator
```

## Step 10

Introduce strategy boundaries only where justified.

## Step 11

Update transaction orchestration.

## Step 12

Update validation.

## Step 13

Update statistics.

## Step 14

Run focused tests.

## Step 15

Run dependent-domain tests.

## Step 16

Run the full suite.

## Step 17

Run end-to-end generation.

## Step 18

Compare shipment statistics with baseline.

---

# 97. Migration Quality Gate

Shipment migration is complete when:

### Business

- Shipment meaning is explicit.
- Order relationship is explicit.
- delivery lifecycle is defined.

### Architecture

- Shipment does not generate Orders.
- Shipment does not process Payments.
- Shipment does not generate Returns.
- Shipment does not own Geography.
- Shipment does not write output.
- Shipment does not own global statistics.

### Data

- IDs unique,
- Order references valid,
- Address references valid where modeled,
- statuses valid,
- timestamps coherent.

### Geography

- destination resolves correctly,
- no duplicated geography hierarchy.

### Testing

- focused tests pass,
- integration tests pass,
- complete suite passes.

### Reproducibility

- same inputs produce the same Shipments.

---

# 98. Design Decisions

## Decision A — Shipment is the fulfillment domain

It owns the physical delivery process.

---

## Decision B — One Shipment per Order is the baseline

The architecture remains open to split shipments.

---

## Decision C — Delivery is initially represented by Shipment

A separate Delivery entity is unnecessary until the business model requires it.

---

## Decision D — Geography is referenced, not duplicated

Shipment consumes Address/Geography information.

---

## Decision E — Delivery duration is behavior

It should eventually depend on geography, carrier, service level, and temporal context.

---

## Decision F — Carrier behavior is configurable

Carrier selection should be a behavior model where multiple policies exist.

---

## Decision G — Lifecycle is state-aware

Shipment status should represent valid operational transitions.

---

## Decision H — Operational incidents are scenarios

Incident behavior should modify shipment strategies rather than scatter conditionals through the generator.

---

## Decision I — Statistics and output remain cross-cutting

Shipment generates domain records; infrastructure measures and persists them.

---

# 99. Final Mental Model

The baseline:

```text
Order
  ↓
Shipment
  ↓
Delivery
```

The geography-aware model:

```text
Customer
   ↓
Address
   ↓
Geography
   ↓
Shipment
   ├── Carrier
   ├── Service Level
   ├── Ship Time
   └── Delivery Time
```

The mature generation flow:

```text
Order
  ↓
Destination Address
  ↓
Geographic Context
  ↓
CarrierSelectionModel
  ↓
ShipmentSplitModel
  ↓
DeliveryDurationModel
  ↓
DeliveryOutcomeModel
  ↓
Shipment
```

The operational feedback loop becomes:

```text
Promotion / Season / Incident
          ↓
Order Volume
          ↓
Shipment Volume
          ↓
Carrier Load
          ↓
Delivery Duration
          ↓
Late / Failed Delivery
```

---

# 100. Summary

Shipment represents the physical fulfillment and delivery process associated with an Order.

The current baseline has:

```text
5,000 Orders
5,000 Shipments
```

providing a simple one-to-one transaction-to-fulfillment relationship.

The major realism improvements are:

```text
coherent lifecycle
        ↓
delivery timing
        ↓
geographic influence
        ↓
carrier selection
        ↓
delivery variability
        ↓
SLA behavior
        ↓
failure/retry
        ↓
split shipments
        ↓
seasonal and operational incidents
```

The target architecture is:

```text
ShipmentGenerator
   ├── CarrierSelectionModel
   ├── ShipmentSplitModel
   ├── DeliveryDurationModel
   └── DeliveryOutcomeModel
```

with Address and Geography supplying destination context and cross-cutting systems handling configuration, validation, statistics, quality, output, and observability.

The most important principle is:

> **Shipment should look like a realistic fulfillment process operating against a real geographic destination, not like an independent status record attached randomly to an Order.**

The next domain is **Return**, where post-purchase behavior connects Orders, OrderItems, Customers, Products, and Shipments through return propensity, eligibility, timing, reasons, partial returns, and refund behavior.
