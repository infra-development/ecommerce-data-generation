# 15 — Event Domain

## 1. Purpose

This document defines the business meaning, event taxonomy, session relationship, customer-journey model, event sequencing, funnel behavior, timestamp generation, conversion behavior, device/channel context, configuration, architecture, validation, statistical realism, scenarios, testing, and migration plan for the **Event** domain in the ShopSphere e-commerce data generator.

Event is the most granular behavioral domain in the current ShopSphere model.

The central relationship is:

```text
Customer
   ↓
Session
   ↓
Event
   ↓
Purchase intent
   ↓
Order
```

A Session describes a period of activity.

An Event describes an action within that period.

The central architectural principle is:

> **Events should represent a coherent customer journey inside a Session, with valid sequencing and temporal relationships, rather than being independently sampled labels attached to Sessions.**

---

# 2. Business Meaning

An Event answers:

> "What did the customer do at this point in the Session?"

Examples of conceptual customer actions include:

```text
landing
search
category view
product view
add to cart
remove from cart
cart view
checkout
payment
purchase
```

The exact event taxonomy must follow the current Event model.

Do not add event types merely because they exist in production analytics systems.

---

# 3. Current Baseline

The current generation plan produces approximately:

```text
3,000 Sessions
24,000 Events
```

Therefore the baseline has:

```text
8 Events / Session
```

The observed distribution is deterministic:

```text
min = 8
max = 8
```

This provides a structurally simple dataset.

However, it does not yet model realistic customer journeys.

---

# 4. Event Ownership

## Event owns

- Event identity,
- Session relationship,
- Customer relationship where modeled,
- event type,
- event timestamp,
- event-specific context.

## Session owns

- activity period,
- session-level context,
- start/end,
- device/channel context.

## Customer owns

- long-term identity and behavioral profile.

## Order owns

- completed commercial transaction.

Event should not own the full business logic of Session, Order, Payment, or Product.

---

# 5. Session Relationship

The fundamental relationship is:

```text
Session
   │
   │ 1
   │
   └──────────< Event
```

Conceptually:

```text
Session 1 → N Event
```

A Session should normally contain one or more Events in the clean baseline.

---

# 6. Customer Relationship

Events may carry or derive Customer context through Session.

Conceptually:

```text
Customer
   ↓
Session
   ↓
Event
```

If Event directly stores `customerId`, the value should remain consistent with the Session's Customer.

---

# 7. Event Ordering

Events within a Session should be ordered in time.

For example:

```text
10:00:01  LANDING
10:00:12  PRODUCT_VIEW
10:01:05  PRODUCT_VIEW
10:02:14  ADD_TO_CART
10:03:02  CHECKOUT
10:03:30  PAYMENT
10:03:42  PURCHASE
```

The exact taxonomy is illustrative.

The critical property is:

```text
eventTime(i) <= eventTime(i+1)
```

for the ordered event stream.

---

# 8. Event Sequence vs Random Event Types

A weak generator does:

```text
eventType = randomChoice(eventTypes)
```

for every event.

This can produce:

```text
PURCHASE
LANDING
CHECKOUT
PRODUCT_VIEW
```

which does not represent a believable customer journey.

A mature generator should model:

```text
journey state
    ↓
valid next event
    ↓
next journey state
```

---

# 9. Event Journey Model

A conceptual purchase journey is:

```text
LANDING
   ↓
SEARCH / CATEGORY_VIEW
   ↓
PRODUCT_VIEW
   ↓
ADD_TO_CART
   ↓
CART_VIEW
   ↓
CHECKOUT
   ↓
PAYMENT
   ↓
PURCHASE
```

Not every Session should follow this complete path.

---

# 10. Browse Journey

A browse-oriented Session might be:

```text
LANDING
   ↓
CATEGORY_VIEW
   ↓
PRODUCT_VIEW
   ↓
PRODUCT_VIEW
   ↓
EXIT
```

No Order is created.

---

# 11. Research Journey

A research-oriented Session may involve:

```text
LANDING
   ↓
SEARCH
   ↓
PRODUCT_VIEW
   ↓
PRODUCT_VIEW
   ↓
CATEGORY_VIEW
   ↓
EXIT
```

Again, no conversion is required.

---

# 12. Purchase Journey

A purchase-oriented Session may be:

```text
LANDING
   ↓
PRODUCT_VIEW
   ↓
ADD_TO_CART
   ↓
CART_VIEW
   ↓
CHECKOUT
   ↓
PAYMENT
   ↓
PURCHASE
```

This creates the causal bridge to Order.

---

# 13. Abandoned Journey

A customer may begin purchasing but abandon:

```text
LANDING
   ↓
PRODUCT_VIEW
   ↓
ADD_TO_CART
   ↓
CART_VIEW
   ↓
EXIT
```

This is important because most sessions need not become purchases.

---

# 14. Checkout Abandonment

Another journey:

```text
LANDING
   ↓
PRODUCT_VIEW
   ↓
ADD_TO_CART
   ↓
CHECKOUT
   ↓
EXIT
```

This can represent checkout abandonment.

---

# 15. Payment Failure Journey

A future journey may include:

```text
CHECKOUT
   ↓
PAYMENT_ATTEMPT
   ↓
PAYMENT_FAILED
   ↓
EXIT
```

or:

```text
PAYMENT_FAILED
   ↓
PAYMENT_RETRY
   ↓
PAYMENT_SUCCESS
   ↓
PURCHASE
```

This connects Event and Payment behavior.

---

# 16. Event Taxonomy

The event taxonomy should be organized around business actions.

A conceptual taxonomy:

```text
Session
 ├── LANDING
 ├── SEARCH
 ├── CATEGORY_VIEW
 ├── PRODUCT_VIEW
 ├── ADD_TO_CART
 ├── REMOVE_FROM_CART
 ├── CART_VIEW
 ├── CHECKOUT
 ├── PAYMENT_ATTEMPT
 ├── PAYMENT_FAILED
 └── PURCHASE
```

The actual current Event types should remain authoritative.

---

# 17. Event Type Ownership

Event type selection should be controlled by the journey model.

For example:

```text
JourneyState.PRODUCT_BROWSING
       ↓
allowed events
       ↓
PRODUCT_VIEW
SEARCH
CATEGORY_VIEW
ADD_TO_CART
```

This prevents impossible transitions.

---

# 18. State Machine

A conceptual journey state machine:

```text
START
  ↓
BROWSING
  ├── SEARCH
  ├── CATEGORY_VIEW
  └── PRODUCT_VIEW
          │
          ▼
      CART
          │
          ├── REMOVE_FROM_CART
          │
          ▼
       CHECKOUT
          │
          ▼
       PAYMENT
          │
          ├── FAILED
          │
          └── SUCCESS
                  │
                  ▼
               PURCHASE
```

The actual implementation may use a simpler transition table.

---

# 19. State Pattern Assessment

The State pattern is potentially useful for Event generation because event validity depends on journey state.

However, it should not be introduced automatically.

A simple model such as:

```text
JourneyState
+
allowed transitions
```

may be clearer.

Use full State objects only if event behavior becomes sufficiently complex.

---

# 20. Journey Strategy

A useful abstraction is:

```text
SessionJourneyModel
```

which answers:

> "What kind of journey does this Session represent?"

Possible strategies:

```text
BrowseJourney
ResearchJourney
PurchaseJourney
AbandonedCartJourney
CheckoutAbandonmentJourney
```

Only implement multiple concrete models when they provide meaningful behavior differences.

---

# 21. Event Sequence Strategy

A separate:

```text
EventSequenceModel
```

can determine:

> "What sequence of events does this journey produce?"

This keeps journey classification separate from event construction.

---

# 22. Event Timing Strategy

A:

```text
EventTimingModel
```

can determine:

> "When does each Event occur within the Session?"

It should consume:

```text
session start
session duration
journey
previous event
```

---

# 23. Event Count

The number of events should eventually depend on:

```text
session duration
journey type
customer engagement
random variation
```

rather than a global fixed:

```text
8
```

---

# 24. Event Count Distribution

Future Sessions may have:

```text
1–3 events
4–10 events
10–30 events
30+ events
```

with a long tail of highly engaged Sessions.

The exact distribution must be calibrated.

---

# 25. Engagement Influence

Customer/session engagement can influence:

```text
event count
session duration
product views
cart actions
conversion probability
```

Conceptually:

```text
engagement
   ↓
event intensity
```

---

# 26. Event Rate

A future model may represent:

```text
events per minute
```

rather than generating count entirely independently.

Conceptually:

```text
session duration
×
event intensity
=
expected event count
```

Then random variation determines the actual count.

---

# 27. Event Timing

Event timestamps should be generated within the Session.

If:

```text
sessionStart = T1
sessionEnd = T2
```

then:

```text
T1 <= eventTime <= T2
```

must hold.

---

# 28. Event Time Gaps

Time gaps between events should be variable.

For example:

```text
landing → product view
    short gap

product research
    longer gap

checkout → payment
    short gap
```

The exact distributions depend on event type.

---

# 29. Event-Type-Specific Timing

A future model can define:

```text
Landing → Search
    expected short delay

Product View → Add to Cart
    moderate delay

Checkout → Payment
    short delay
```

This produces more realistic session timelines.

---

# 30. Session Duration Constraint

Event timestamps must never exceed Session duration.

If generated event gaps exceed the remaining Session time, the generator must either:

```text
extend the Session
```

or:

```text
stop generating events
```

depending on the model.

It must not silently produce invalid timestamps.

---

# 31. Event and Device

Device context comes from Session.

Conceptually:

```text
Session
  ├── device
  └── Event
       ↓
event behavior
```

Some event behavior may depend on device.

For example, mobile and desktop navigation patterns may differ.

This should be modeled only if needed.

---

# 32. Event and Channel

Channel context also originates from Session.

Conceptually:

```text
Session
  ├── channel
  └── Events
```

Campaign/traffic-source context can influence journey behavior.

---

# 33. Product Context

Product-related Events may reference Products.

Examples:

```text
PRODUCT_VIEW
ADD_TO_CART
```

If the Event model contains `productId`, it should reference a valid Product.

---

# 34. Category Context

Category-related Events may reference Categories where modeled.

For example:

```text
CATEGORY_VIEW
```

should point to a valid Category.

---

# 35. Cart Context

A realistic journey should maintain cart state.

Conceptually:

```text
cart = {}
```

then:

```text
PRODUCT_VIEW
   ↓
ADD_TO_CART(product)
   ↓
cart = {product}
```

then:

```text
REMOVE_FROM_CART(product)
   ↓
cart = {}
```

This is a future behavioral capability.

---

# 36. Cart State

Cart state is important because it prevents invalid events such as:

```text
REMOVE_FROM_CART
```

for a product never added.

It also enables:

```text
CART_VIEW
```

to represent actual basket contents.

---

# 37. Checkout Eligibility

A normal purchase journey should generally require:

```text
non-empty cart
```

before:

```text
CHECKOUT
```

This is a business invariant.

---

# 38. Payment Eligibility

Payment should generally follow:

```text
CHECKOUT
```

if that is the selected ShopSphere lifecycle.

---

# 39. Purchase Eligibility

Purchase should generally require:

```text
successful payment
```

if payment is mandatory.

This creates:

```text
checkout
   ↓
payment
   ↓
purchase
```

---

# 40. Order Creation

A future conversion flow may be:

```text
PURCHASE event
      ↓
Order creation
      ↓
OrderItems
      ↓
Payment / Shipment
```

or:

```text
Order
      ↓
PURCHASE event attribution
```

The project should explicitly choose which object is generated first.

The important requirement is that both represent the same business conversion.

---

# 41. Session-to-Order Attribution

A mature model should be able to answer:

> "Which Session produced this Order?"

This can be represented through:

```text
sessionId on Order
```

or an attribution relation.

The current Order model should not be modified during migration without explicit design.

---

# 42. Event-to-Order Attribution

Similarly:

```text
PURCHASE Event
      ↓
Order
```

may share:

```text
customerId
sessionId
```

and relevant timestamps.

This provides traceability through the funnel.

---

# 43. Customer Journey

The mature behavioral chain is:

```text
Customer
   ↓
Session
   ↓
Event sequence
   ↓
Cart behavior
   ↓
Checkout
   ↓
Payment
   ↓
Purchase
   ↓
Order
```

This is one of the most important long-term architectural goals of the generator.

---

# 44. Funnel States

Useful funnel stages:

```text
visit
product engagement
cart
checkout
payment
purchase
```

Events provide observable evidence for each stage.

---

# 45. Funnel Conversion Rates

The statistics layer can measure:

```text
session → product view
product view → cart
cart → checkout
checkout → payment
payment → purchase
```

These should be configurable/calibratable.

---

# 46. Drop-Off

A realistic funnel needs drop-off.

For example:

```text
100 Sessions
   ↓
60 Product-view Sessions
   ↓
20 Cart Sessions
   ↓
10 Checkout Sessions
   ↓
8 Payment Sessions
   ↓
7 Purchases
```

The numbers are illustrative.

The key is:

> Every stage should not have 100% conversion.

---

# 47. Customer Behavior Influence

CustomerBehaviorProfile can influence:

```text
session journey type
event count
product exploration
cart probability
checkout probability
conversion
```

This makes Events a major consumer of the behavioral architecture.

---

# 48. Product Affinity Influence

Customer product/category preferences should affect:

```text
SEARCH
CATEGORY_VIEW
PRODUCT_VIEW
ADD_TO_CART
```

This creates observable preference patterns.

---

# 49. Search Behavior

A future search model can select:

```text
category
brand
product
```

based on customer preferences.

Search events should not simply reference random Products.

---

# 50. Product View Popularity

Product views can follow a different distribution from purchases.

For example:

```text
many products viewed
fewer products purchased
```

This distinction is important.

A product can have:

```text
high view count
low purchase count
```

and vice versa.

---

# 51. View-to-Purchase Relationship

A mature model can support:

```text
Product View
     ↓
purchase propensity
     ↓
Add to Cart
     ↓
Checkout
```

This creates a meaningful behavioral funnel.

---

# 52. Product Exploration

A customer may view multiple products before purchasing one.

Therefore:

```text
views/session
```

should generally exceed:

```text
purchases/session
```

in the normal baseline.

---

# 53. Category Exploration

Similarly:

```text
categories viewed/session
```

can exceed:

```text
categories purchased/order
```

This provides realistic browsing behavior.

---

# 54. Session Intent

Session intent can determine likely Event sequences.

For example:

```text
browse
  → product exploration

research
  → many product views

purchase
  → cart/checkout/payment

support
  → support-oriented events
```

The exact taxonomy should remain focused on project requirements.

---

# 55. Event Journey Model

A useful architecture is:

```text
Session
   ↓
SessionJourneyModel
   ↓
Journey
   ↓
EventSequenceModel
   ↓
Events
```

This separates high-level behavior from event construction.

---

# 56. Event Factory Assessment

A factory may be useful for creating event records when event construction differs substantially by event type.

For example:

```text
EventFactory
```

could create:

```text
ProductViewEvent
CartEvent
CheckoutEvent
PaymentEvent
```

However, if the persisted model is a simple generic Event case class, a factory may be unnecessary.

---

# 57. Strategy Pattern

Strong strategy candidates:

```text
SessionJourneyModel
EventSequenceModel
EventTimingModel
EventCountModel
ProductExplorationModel
ConversionModel
```

These represent real behavioral variation.

---

# 58. Factory Pattern

Potential factories:

```text
JourneyModelFactory
EventSequenceModelFactory
```

Use only when configuration selects among multiple implementations.

---

# 59. State Pattern Assessment

State/transition modeling is useful conceptually.

Prefer a simple transition representation initially:

```text
state
→ allowed next states
```

Move to State objects only if event-specific state behavior becomes complex.

---

# 60. Builder Assessment

Builder is generally unnecessary for Event.

A generic immutable Event record is usually straightforward.

---

# 61. Composition

Preferred architecture:

```text
EventGenerator
   ├── SessionJourneyModel
   ├── EventSequenceModel
   ├── EventTimingModel
   └── EventContextResolver
```

This is more readable than one giant generator.

---

# 62. Dependency Injection

Constructor injection is preferred.

Conceptually:

```scala
class EventGenerator(
    journeyModel: SessionJourneyModel,
    sequenceModel: EventSequenceModel,
    timingModel: EventTimingModel,
    contextResolver: EventContextResolver
)
```

No DI framework is required.

---

# 63. SOLID — Single Responsibility

EventGenerator should not own:

```text
Session generation
Order generation
Payment generation
Product catalog loading
CSV writing
global statistics
```

---

# 64. SOLID — Open/Closed

New journey types should be addable without rewriting unrelated event timing logic.

---

# 65. SOLID — Liskov

Concrete journey/sequence strategies must satisfy their contracts.

---

# 66. SOLID — Interface Segregation

Keep behavioral interfaces focused.

Avoid one massive:

```text
EventBehavior
```

interface containing every concern.

---

# 67. SOLID — Dependency Inversion

EventGenerator should depend on journey/sequence/timing abstractions where substitution is needed.

---

# 68. Readability Standard

The code should communicate a customer journey.

Preferred conceptual flow:

```scala
val journey =
  journeyModel.choose(
    sessionContext
  )

val sequence =
  sequenceModel.generate(
    journey,
    sessionContext
  )

val events =
  timingModel.place(
    sequence,
    sessionContext
  )

events.map(
  eventFactory.create
)
```

The exact implementation will depend on the final model.

The important property is that the code reads like a journey.

---

# 69. Event Context

Event generation may require a context object containing:

```text
Customer
Session
Device
Channel
Generation time
Customer behavior
Product catalog context
```

The context should remain immutable.

---

# 70. Context vs Global State

Avoid global mutable journey state.

Each Session should have its own:

```text
JourneyContext
```

This makes generation:

- deterministic,
- testable,
- parallelizable.

---

# 71. Reproducibility

Event generation must satisfy:

```text
same seed
+
same Session
+
same Customer profile
+
same Product reference data
+
same configuration
```

→ same event sequence.

---

# 72. Randomness Isolation

Use derived random streams for:

```text
journey selection
event count
product exploration
event transitions
event timing
conversion
```

This avoids unrelated random decisions influencing each other.

---

# 73. Temporal Reproducibility

The generation period should be explicit.

Avoid:

```text
LocalDateTime.now()
```

inside EventGenerator.

Use configured generation time boundaries.

---

# 74. Event ID

Event IDs should be:

```text
unique
deterministic
stable
```

The exact format follows the current implementation.

---

# 75. Event Sequence Number

A future model may use:

```text
sequenceNumber
```

to make ordering explicit.

Do not add it unless the current model or final design requires it.

Timestamp ordering remains important regardless.

---

# 76. Validation

Event-specific validation should verify:

```text
Event ID unique
Session ID valid
Customer ID valid
event type valid
timestamp valid
```

Cross-domain validation should verify:

```text
Event customer = Session customer
Event timestamp within Session
Product references valid
Category references valid
```

where those fields exist.

---

# 77. Event Transition Validation

If a journey state machine exists, validate:

```text
every event transition is allowed
```

Examples of invalid sequences:

```text
PURCHASE before CHECKOUT
CHECKOUT with empty cart
REMOVE_FROM_CART without cart item
PAYMENT before CHECKOUT
```

where those rules apply.

---

# 78. Session Boundary Validation

Every Event should satisfy:

```text
sessionStart <= eventTime <= sessionEnd
```

This is one of the most important Event invariants.

---

# 79. Customer Consistency

If both Session and Event carry Customer ID:

```text
event.customerId == session.customerId
```

must hold.

---

# 80. Product Reference Validation

If an Event contains Product ID:

```text
event.productId exists in Product catalog
```

must hold.

---

# 81. Category Reference Validation

If an Event contains Category ID:

```text
event.categoryId exists
```

and should be consistent with the selected Product where applicable.

---

# 82. Cart Validation

If cart state is modeled:

```text
ADD_TO_CART
    → product becomes present

REMOVE_FROM_CART
    → product was previously present

CHECKOUT
    → cart non-empty
```

These are behavioral invariants.

---

# 83. Purchase Validation

If Purchase is an Event:

```text
PURCHASE
    → valid checkout path
```

and:

```text
PURCHASE
    → corresponding Order
```

when the model establishes that relationship.

---

# 84. Payment Validation

If Payment events are generated:

```text
PAYMENT_ATTEMPT
```

should occur after the appropriate checkout state.

A:

```text
PAYMENT_FAILED
```

should not be treated as a successful conversion.

---

# 85. Statistical Validation

Event statistics should include:

```text
events/session
events/customer
event type distribution
events/minute
journey-type distribution
conversion rate
product views/session
cart rate
checkout rate
payment success rate
```

---

# 86. Event-Type Distribution

Observed event-type shares should be compared against configured behavior.

Do not expect exact percentages for random models.

Use statistical tolerance.

---

# 87. Event Count Distribution

The mature generator should report:

```text
mean
median
p75
p90
p95
p99
max
```

for:

```text
events/session
```

This is necessary to detect excessive determinism.

---

# 88. Journey Distribution

Measure:

```text
browse sessions
research sessions
purchase sessions
abandoned sessions
```

and compare against configuration.

---

# 89. Funnel Metrics

Measure:

```text
Sessions
Product-view Sessions
Cart Sessions
Checkout Sessions
Payment Sessions
Purchase Sessions
Orders
```

Then calculate stage conversion rates.

---

# 90. Temporal Event Distribution

Measure:

```text
events/hour
events/day
events/day-of-week
events/month
```

This verifies temporal behavior.

---

# 91. Product View Concentration

Measure:

```text
views/product
```

and compare with:

```text
purchases/product
```

This can reveal whether product popularity is represented consistently across browsing and purchasing.

---

# 92. Customer Engagement Validation

High-engagement customers should produce:

```text
more sessions
more events
potentially more conversions
```

than low-engagement customers.

This validates the behavioral chain.

---

# 93. Conversion Validation

If a purchase-oriented Session has a higher conversion probability, observed conversion should reflect that.

The relationship should be statistical, not deterministic.

---

# 94. Data Quality

Potential Event defects:

```text
duplicate event ID
unknown session ID
unknown customer ID
invalid event type
event outside session
invalid product ID
invalid category ID
invalid sequence
impossible journey transition
```

These should be deliberately injected.

---

# 95. Clean Baseline

The clean Event generator should guarantee:

```text
unique IDs
valid Session references
valid event types
valid timestamps
valid customer consistency
valid product/category references
valid journey transitions
```

where applicable.

---

# 96. Controlled Skew

Event is an excellent domain for skew experiments.

A hot customer can create:

```text
Customer
   ↓
many Sessions
   ↓
many Events
```

A hot Session can create:

```text
Session
   ↓
many Events
```

A hot Product can create:

```text
Product
   ↓
many ProductView Events
```

---

# 97. Hot Session Scenario

A scenario can deliberately create unusually event-heavy Sessions.

This is useful for:

```text
groupBy(session_id)
```

and join/aggregation skew experiments.

---

# 98. Hot Customer Scenario

A customer activity scenario can create:

```text
few customers
   ↓
many Sessions
   ↓
many Events
```

This propagates skew through multiple datasets.

---

# 99. Campaign Traffic Spike

A campaign can cause:

```text
traffic spike
   ↓
Session spike
   ↓
Event spike
   ↓
potential conversion spike
```

This creates a realistic time-series workload.

---

# 100. Bot-Like Traffic Scenario

A future scenario can model abnormal traffic such as:

```text
high event volume
very short intervals
low conversion
repeated product views
```

This can be useful for anomaly-detection datasets.

It should remain a controlled scenario rather than polluting the clean baseline.

---

# 101. Data Quality Scenario

Potential Event corruption scenarios:

```text
timestamp corruption
invalid transitions
orphan events
duplicate events
invalid product references
```

The Quality Engine owns injection.

---

# 102. Output Boundary

Event should not write:

```text
events.csv
```

directly.

The output layer owns serialization.

---

# 103. Statistics Boundary

Event generation should not calculate global:

```text
event distribution
funnel conversion
events/session
```

The statistics layer owns these metrics.

---

# 104. Manifest Boundary

The manifest records:

```text
event row count
schema metadata
generation metadata
```

Event generation does not construct the manifest.

---

# 105. Spark Performance Laboratory

Event is likely to become the largest dataset in the project.

Potential workloads include:

```text
Event ↔ Session
Event ↔ Customer
Event ↔ Product
Event ↔ Order
```

and:

```text
groupBy(event_type)
groupBy(session_id)
groupBy(customer_id)
groupBy(product_id)
```

---

# 106. Large Event Volume

Because:

```text
Events / Session
```

can be much larger than:

```text
Sessions / Customer
```

Event cardinality can grow very quickly.

For example:

```text
10M Sessions
×
15 Events/session
=
150M Events
```

This makes Event useful for large-scale Spark processing.

---

# 107. Event Skew Workloads

Hot customers and hot sessions can create:

```text
uneven partition sizes
```

during:

```text
groupBy(customer_id)
groupBy(session_id)
```

This is useful for Spark skew analysis.

---

# 108. Funnel Workloads

Spark can analyze:

```text
Session
  → Events
  → conversion
```

with:

```text
window functions
groupBy
joins
conditional aggregation
```

This makes Event an excellent analytical workload.

---

# 109. Sessionization Workload

If raw Event data is later used without Session identifiers, Spark can reconstruct Sessions using:

```text
customer_id
+
timestamp gaps
```

This can become a future Spark lab.

---

# 110. Event Ordering Workload

Spark window functions can calculate:

```text
previous event
next event
time since previous event
```

This supports journey analysis.

---

# 111. Journey Analytics

Potential Spark exercises:

```text
most common event paths
drop-off after product view
checkout abandonment
conversion by device
conversion by channel
```

These are directly supported by realistic Event sequences.

---

# 112. Current Simplifications

The current Event implementation simplifies:

- fixed events/session,
- independent event types,
- journey sequencing,
- session intent,
- cart state,
- event timing,
- event duration,
- product exploration,
- funnel conversion,
- payment event behavior,
- session-to-order attribution,
- customer engagement,
- temporal seasonality,
- campaign effects,
- abnormal traffic scenarios.

These are planned realism improvements.

---

# 113. Planned Realism Sequence

Recommended progression:

```text
1. Preserve current Event model
2. Stabilize Session relationship
3. Define authoritative event taxonomy
4. Introduce journey model
5. Introduce valid event transitions
6. Make event count variable
7. Introduce event timing
8. Introduce customer/product context
9. Introduce cart state
10. Introduce funnel conversion
11. Connect purchase events to Orders
12. Connect payment events
13. Add campaign/seasonality
14. Add abnormal traffic scenarios
```

---

# 114. Migration Strategy

## Step 1

Inspect current Event model.

## Step 2

Inspect current EventGenerator.

## Step 3

Inspect current Event types.

## Step 4

Inspect Session relationship.

## Step 5

Inspect current event-count logic.

## Step 6

Inspect timestamp generation.

## Step 7

Inspect Product/Category references.

## Step 8

Define target responsibilities.

## Step 9

Move model into:

```text
event/model
```

## Step 10

Move generation into:

```text
event/generator
```

## Step 11

Introduce journey/sequence strategies.

## Step 12

Update Session orchestration.

## Step 13

Update future Order conversion orchestration.

## Step 14

Update validation.

## Step 15

Update statistics.

## Step 16

Run focused Event tests.

## Step 17

Run Session integration tests.

## Step 18

Run Order integration tests.

## Step 19

Run complete suite.

## Step 20

Run end-to-end generation.

## Step 21

Compare Event statistics with baseline.

---

# 115. Migration Quality Gate

Event migration is complete when:

### Business

- Event meaning is explicit.
- taxonomy is defined.
- journey semantics are defined.
- Session relationship is explicit.

### Architecture

- Event does not generate Sessions.
- Event does not generate Orders.
- Event does not own Product catalog loading.
- Event does not write output.
- Event does not own global statistics.

### Data

- IDs unique,
- Session references valid,
- timestamps within Session,
- event types valid,
- references valid.

### Behavior

- journey has a clear owner,
- sequence has a clear owner,
- timing has a clear owner,
- context resolution has a clear owner.

### Testing

- Event tests pass,
- Session integration tests pass,
- Order integration tests pass,
- complete suite passes.

### Reproducibility

- same inputs produce the same event sequences.

---

# 116. Design Decisions

## Decision A — Event represents an action

It is not merely a categorical record attached randomly to a Session.

---

## Decision B — Session owns the activity period

Event belongs inside Session context.

---

## Decision C — Event sequence should be journey-aware

Valid transitions matter.

---

## Decision D — Not every Session converts

Funnel drop-off is essential for realism.

---

## Decision E — Event count should eventually be variable

The current eight-events/session rule is a baseline simplification.

---

## Decision F — Event timestamps must live inside Session

Temporal validity is mandatory.

---

## Decision G — Cart state may become a first-class generation context

This enables realistic add/remove/checkout behavior.

---

## Decision H — Purchase Event and Order must represent the same conversion

The attribution relationship should be explicitly designed.

---

## Decision I — Event scenarios remain compositional

Campaign spikes, hot sessions, hot customers, and abnormal traffic should modify behavior models rather than create scattered conditional logic.

---

# 117. Final Mental Model

The simple baseline:

```text
Customer
   ↓
Session
   ↓
8 Events
```

The mature behavioral model:

```text
CustomerBehaviorProfile
        │
        ▼
     Session
        │
        ▼
 SessionJourneyModel
        │
        ▼
      Journey
        │
        ▼
 EventSequenceModel
        │
        ├── LANDING
        ├── SEARCH
        ├── PRODUCT_VIEW
        ├── ADD_TO_CART
        ├── CART_VIEW
        ├── CHECKOUT
        ├── PAYMENT
        └── PURCHASE
                │
                ▼
              Order
```

The temporal model:

```text
Session Start
     ↓
Event 1
     ↓
Δt
     ↓
Event 2
     ↓
Δt
     ↓
Event N
     ↓
Session End
```

The funnel model:

```text
Sessions
   ↓
Product Engagement
   ↓
Cart
   ↓
Checkout
   ↓
Payment
   ↓
Purchase
   ↓
Order
```

---

# 118. Summary

Event is the deepest behavioral domain in the ShopSphere generator.

The current baseline has:

```text
3,000 Sessions
24,000 Events
8 Events / Session
```

This is structurally useful but highly deterministic.

The major realism improvements are:

```text
customer engagement
        ↓
session intent
        ↓
journey selection
        ↓
valid event sequencing
        ↓
variable event count
        ↓
realistic event timing
        ↓
product/category exploration
        ↓
cart state
        ↓
checkout behavior
        ↓
payment behavior
        ↓
purchase conversion
        ↓
Order
```

The target architecture is:

```text
EventGenerator
   ├── SessionJourneyModel
   ├── EventSequenceModel
   ├── EventTimingModel
   └── EventContextResolver
```

with Session supplying the bounded activity context and Order representing the resulting commercial transaction.

The most important principle is:

> **Events should tell a believable story of what the customer did during a Session. A realistic Event dataset is therefore a collection of journeys and state transitions, not a random collection of event labels.**

With Event completed, the next documentation stage moves from individual domains toward **cross-cutting architecture**. The next document should define the **Relationship and Domain-Interaction Architecture**, covering how Customer, Address, Product, Order, OrderItem, Payment, Shipment, Return, Session, and Event interact without creating circular dependencies or giant generators.
