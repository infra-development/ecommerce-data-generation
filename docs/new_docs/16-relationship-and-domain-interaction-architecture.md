# 16 — Relationship and Domain-Interaction Architecture

## 1. Purpose

This document defines how ShopSphere domains interact with one another.

The project contains multiple business entities:

```text
Customer
Address
Geography
Category
Brand
Product
Order
OrderItem
Payment
Shipment
Return
Session
Event
```

The objective is not merely to make their foreign keys valid.

The objective is to define:

- who owns each business concept,
- who is allowed to depend on whom,
- where cross-domain behavior belongs,
- how relationships are generated,
- how behavioral relationships differ from structural relationships,
- how circular dependencies are avoided,
- how domain services coordinate multiple entities,
- how application orchestration assembles the complete dataset,
- how validation crosses domain boundaries,
- how realism propagates through the relationship graph.

The central principle is:

> **A relationship should have an explicit business owner, an explicit generation responsibility, and an explicit validation boundary.**

---

# 2. Why Relationship Architecture Matters

The generator is no longer a collection of independent table generators.

The entities form a business graph:

```text
                    Geography
                       │
                       ▼
                    Address
                       │
                       ▼
Customer ──────────────┼────────────── Session
   │                   │                  │
   │                   │                  ▼
   │                   │                Event
   │                   │                  │
   ▼                   │                  ▼
 Order ────────────────┘               Purchase
   │
   ├────────── OrderItem ──────── Product
   │                                │
   │                                ├── Category
   │                                └── Brand
   │
   ├── Payment
   │
   ├── Shipment
   │
   └── Return
```

If these relationships are designed poorly, the code will eventually develop:

- circular dependencies,
- giant generators,
- duplicated business logic,
- hidden random choices,
- difficult tests,
- inconsistent foreign keys,
- unclear ownership,
- fragile generation ordering.

---

# 3. Structural vs Behavioral Relationships

This distinction is fundamental.

## Structural relationship

A structural relationship answers:

> "Which record does this record belong to or reference?"

Examples:

```text
Order.customerId
OrderItem.orderId
OrderItem.productId
Payment.orderId
Shipment.orderId
Session.customerId
Event.sessionId
Address.customerId
Address.buildingId
```

## Behavioral relationship

A behavioral relationship answers:

> "Why does this record tend to reference that record?"

Examples:

```text
Customer → preferred Category
Customer → preferred Brand
Customer → preferred Product
Customer → likely Session activity
Customer → order frequency
Customer → return propensity
```

Structural relationships must be valid.

Behavioral relationships must be realistic.

---

# 4. Relationship Ownership

Every relationship should have an explicit owner.

Examples:

```text
Order → Customer
```

is structurally represented by Order.

Therefore Order generation owns assignment of a Customer to an Order.

```text
OrderItem → Product
```

is structurally represented by OrderItem.

Therefore OrderItem generation owns Product selection.

```text
Event → Session
```

is structurally represented by Event.

Therefore Event generation owns assignment of Events to a Session.

---

# 5. Relationship Ownership Rule

The owner should normally be the domain that contains the foreign-key-like reference.

For example:

```text
Order
  customerId
```

means:

```text
OrderGenerator
```

owns the structural assignment.

Do not create a separate generic:

```text
RelationshipManager
```

that performs every assignment.

That would hide business intent.

---

# 6. Why a Giant RelationshipManager Is Dangerous

A class such as:

```text
RelationshipManager
```

can easily become responsible for:

```text
customer → order
order → item
item → product
order → payment
order → shipment
order → return
customer → session
session → event
```

This creates a god object.

It also makes it difficult to answer:

> "Why was this relationship generated this way?"

The business rule becomes hidden inside infrastructure.

---

# 7. Relationship Services

Cross-domain behavior should instead be expressed through focused services where necessary.

Examples:

```text
OrderCustomerAssignment
OrderItemProductSelection
ShipmentAddressResolver
SessionOrderAttribution
ReturnEligibilityEvaluator
```

These names communicate business intent.

---

# 8. Customer → Address

Relationship:

```text
Customer
   │
   └──< Address
```

Business meaning:

> A customer can have one or more delivery addresses.

Address generation owns the structural Customer relationship.

Geography owns location reference data.

Therefore:

```text
AddressGenerator
   ↓
Customer ID
   ↓
GeographyReferenceData
```

---

# 9. Address → Geography

Relationship:

```text
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

Address does not duplicate the complete geography hierarchy.

The Address model currently stores:

```text
buildingId
unitNumber
postalCode
```

The hierarchy is resolved through Geography reference data.

---

# 10. Geography Is Reference Data

Geography is different from transactional entities.

It is primarily:

```text
reference data
```

rather than generated behavioral data.

Therefore the dependency direction is:

```text
Address
   ↓
Geography reference
```

not:

```text
Geography
   ↓
Address generation
```

---

# 11. Category → Product

Relationship:

```text
Category
   │
   └──< Product
```

Product generation owns category assignment.

Category provides valid reference data.

The Product generator should not ask a global relationship manager:

```text
give me category
```

It should use an explicit category-selection policy.

---

# 12. Brand → Product

Relationship:

```text
Brand
   │
   └──< Product
```

Product generation owns brand assignment.

Brand selection may depend on:

```text
category
brand popularity
product tier
configuration
```

This creates:

```text
Category
   ↓
Brand selection
   ↓
Product
```

rather than independent random selections.

---

# 13. Category × Brand Relationship

Category and Brand should not be treated as independent dimensions when realism requires affinity.

For example:

```text
Category A
   ↓
Brand A: high affinity
Brand B: medium affinity
Brand C: low affinity
```

This relationship is behavioral.

The Product domain can consume the configured affinity model.

---

# 14. Product Popularity

Product popularity is different from catalog ownership.

A Product may be:

```text
rare
moderately popular
highly popular
```

This affects transaction and event selection.

It does not necessarily affect:

```text
Product catalog existence
```

---

# 15. Customer → Product

Customer-to-Product relationship is primarily behavioral.

A customer may prefer:

```text
certain categories
certain brands
certain price tiers
certain products
```

The Customer behavior model should express those preferences.

Product selection consumes them.

---

# 16. Customer → Category

A useful behavioral hierarchy is:

```text
Customer
   ↓
Category affinity
   ↓
Brand affinity
   ↓
Product popularity
   ↓
Product choice
```

This is more realistic than:

```text
random Customer
+
random Product
```

---

# 17. Customer → Brand

Brand preference can be conditioned on category.

For example:

```text
Customer
   ↓
Electronics preference
   ↓
preferred electronics brands
```

This avoids forcing one global brand preference across unrelated categories.

---

# 18. Customer → Session

Relationship:

```text
Customer
   │
   └──< Session
```

Session generation owns structural Customer assignment.

Behavioral Session frequency comes from:

```text
CustomerBehaviorProfile
```

Thus:

```text
Customer profile
      ↓
session frequency
      ↓
Session records
```

---

# 19. Customer → Order

Relationship:

```text
Customer
   │
   └──< Order
```

Order generation owns structural Customer assignment.

Behavioral order frequency comes from Customer behavior.

Therefore:

```text
CustomerBehaviorProfile
      ↓
OrderFrequencyModel
      ↓
Order
```

---

# 20. Customer → Event

The indirect relationship is:

```text
Customer
   ↓
Session
   ↓
Event
```

If Event also stores Customer ID, it must remain consistent.

Do not independently select:

```text
event.customerId
```

from all customers.

It should derive from the Session context.

---

# 21. Session → Event

Relationship:

```text
Session
   │
   └──< Event
```

Event generation owns the event records.

Session provides:

```text
customer
start
end
device
channel
```

Event generation builds the journey within those boundaries.

---

# 22. Session → Order

This is primarily a behavioral attribution relationship.

A purchase-oriented Session may create or influence an Order.

Conceptually:

```text
Session
   ↓
Purchase Event
   ↓
Order
```

The exact persisted relationship should be decided explicitly.

---

# 23. Order → OrderItem

Relationship:

```text
Order
   │
   └──< OrderItem
```

OrderItem generation owns item creation.

Order provides:

```text
orderId
customer context
transaction context
```

The number of items should be determined by a basket-size model.

---

# 24. OrderItem → Product

Relationship:

```text
Product
   │
   └──< OrderItem
```

OrderItem owns Product selection.

Product selection may consume:

```text
customer preferences
category affinity
brand affinity
product popularity
basket context
price sensitivity
```

---

# 25. Basket Composition

Product selection should eventually consider the existing basket.

Example:

```text
Laptop
   ↓
Laptop bag
   ↓
Mouse
```

This creates:

```text
Product A
   ↓
complementary Product B
```

rather than independent item selection.

---

# 26. Order → Payment

Relationship:

```text
Order
   │
   └── Payment
```

Payment belongs to the Order transaction.

Payment generation consumes:

```text
Order
Customer behavior
payment method preferences
```

It should not generate Orders.

---

# 27. Order → Shipment

Relationship:

```text
Order
   │
   └── Shipment
```

Shipment generation consumes:

```text
Order
shipping address
payment/fulfillment eligibility
carrier behavior
```

It does not create the Order.

---

# 28. Shipment → Address

The delivery destination is an Address.

The relationship is conceptually:

```text
Shipment
   ↓
Address
   ↓
Building
   ↓
Geography
```

Shipment should resolve a valid Address rather than reconstructing geography.

---

# 29. Shipment → Geography

Shipment can derive geographic context through Address.

For example:

```text
Shipment
   ↓
Address
   ↓
Building
   ↓
Area
   ↓
City
```

This supports:

- delivery-time models,
- carrier selection,
- geographic skew,
- operational scenarios.

---

# 30. Order → Return

Return is conditional.

Not every Order produces a Return.

Conceptually:

```text
Order
   ↓
eligibility
   ↓
return probability
   ↓
Return
```

Return generation owns this decision.

---

# 31. Return → OrderItem

Returns should normally identify the item being returned.

Conceptually:

```text
Order
   ↓
OrderItem
   ↓
Return
```

Return generation should not independently select a Product unrelated to the Order.

---

# 32. Return Behavioral Dependencies

Return probability can depend on:

```text
customer return propensity
product return propensity
category return propensity
delivery outcome
order characteristics
```

This is a cross-domain behavioral decision.

---

# 33. Relationship Graph

The complete structural graph can be represented as:

```text
Geography
    ↑
Address
    ↑
Customer
    ├──────────────→ Session ─────→ Event
    │                    │
    │                    └────────→ Purchase
    │
    └──────────────→ Order
                       ├────────→ OrderItem ─────→ Product
                       │                           ├──→ Category
                       │                           └──→ Brand
                       ├────────→ Payment
                       ├────────→ Shipment ─────→ Address
                       └────────→ Return ───────→ OrderItem
```

The arrows represent dependency/reference direction, not necessarily ownership of the business concept.

---

# 34. Generation Dependency Graph

Generation should follow dependency order.

A practical order is:

```text
Reference Data
      ↓
Category / Brand
      ↓
Product
      ↓
Customer
      ↓
Address
      ↓
Session
      ↓
Event
      ↓
Order
      ↓
OrderItem
      ↓
Payment
      ↓
Shipment
      ↓
Return
```

However, mature behavioral generation may require orchestration changes.

For example:

```text
Session
   ↔
Order attribution
```

must not become a circular generator dependency.

---

# 35. Circular Dependency Problem

A dangerous design is:

```text
SessionGenerator
    → OrderGenerator
        → SessionGenerator
```

This creates circular runtime dependencies.

Avoid it.

---

# 36. Application-Level Coordination

Cross-domain workflows should be coordinated at the application layer.

For example:

```text
Session generation
       ↓
Event journey
       ↓
conversion decision
       ↓
Order generation
```

The application orchestration layer coordinates the process.

Individual generators remain focused.

---

# 37. Domain Service Example

A focused domain service could represent:

```text
PurchaseConversionService
```

Responsibility:

> Determine whether a Session journey produces a commercial conversion.

It should not write CSV files or generate every downstream entity.

---

# 38. Domain Service Boundary

A domain service is justified when:

```text
business decision
```

requires multiple domain concepts.

Examples:

```text
PurchaseConversionService
ReturnEligibilityService
ShipmentEligibilityService
```

---

# 39. Avoid Service Explosion

Do not create services for every method.

Bad:

```text
CustomerIdService
ProductIdService
OrderIdService
EventIdService
```

if simple generators can handle those responsibilities.

Services should exist for business operations that genuinely cross boundaries.

---

# 40. Application Orchestrator

The application layer should express the overall workflow.

Conceptually:

```scala
val referenceData = referenceDataLoader.load()

val catalog = catalogGenerator.generate(referenceData)

val customers =
  customerGenerator.generate(catalog, context)

val addresses =
  addressGenerator.generate(customers, referenceData.geography)

val sessions =
  sessionGenerator.generate(customers)

val events =
  eventGenerator.generate(sessions, customers, catalog)

val orders =
  orderGenerator.generate(customers, events, catalog)

val orderItems =
  orderItemGenerator.generate(orders, customers, catalog)

val payments =
  paymentGenerator.generate(orders, customers)

val shipments =
  shipmentGenerator.generate(orders, addresses, referenceData.geography)

val returns =
  returnGenerator.generate(orders, orderItems, customers)
```

This is conceptual.

The final implementation should use domain-specific context objects rather than enormous method signatures.

---

# 41. Dependency Injection

The orchestrator should receive generators through constructor injection.

Conceptually:

```text
GenerationApplication
   ├── CustomerGenerator
   ├── AddressGenerator
   ├── SessionGenerator
   ├── EventGenerator
   ├── OrderGenerator
   ├── OrderItemGenerator
   ├── PaymentGenerator
   ├── ShipmentGenerator
   └── ReturnGenerator
```

This makes orchestration explicit.

---

# 42. Generator Independence

A generator should depend only on:

```text
its required domain inputs
+
behavior models
+
reference data
+
randomness
+
configuration
```

It should not depend on the entire application.

---

# 43. Context Objects

When cross-domain input becomes large, use explicit context objects.

Examples:

```text
CustomerGenerationContext
OrderGenerationContext
SessionGenerationContext
ShipmentGenerationContext
```

A context should contain only what that domain needs.

---

# 44. Avoid Universal GenerationContext

A giant:

```text
GenerationContext
```

containing every entity and configuration object can become another god object.

Prefer smaller contexts.

---

# 45. Structural Relationship Selection

Structural assignment should be explicit.

For example:

```scala
order.customerId
```

should communicate:

> This Order belongs to this Customer.

Avoid hidden relationship assignment inside unrelated utility methods.

---

# 46. Behavioral Selection

Behavioral selection should be explicit.

For example:

```scala
productPopularityModel.select(...)
```

communicates:

> Product is being selected according to popularity.

This is more readable than:

```scala
random.pick(...)
```

with no indication of business meaning.

---

# 47. Randomness Is Not Business Logic

A random generator is infrastructure.

For example:

```text
RandomGenerator
```

should provide randomness.

It should not decide:

```text
which customer buys electronics
```

That decision belongs to a business behavior model.

---

# 48. Distribution Is Not Relationship Logic

Similarly:

```text
Distribution
```

provides statistical sampling.

It should not know:

```text
which product belongs to an order
```

Product selection combines business rules with distributions.

---

# 49. Relationship Resolver

A small resolver can be justified for read-only reference resolution.

Examples:

```text
GeographyResolver
ProductResolver
CustomerResolver
```

But these should not mutate unrelated domains.

---

# 50. Relationship Indexes

Large datasets require efficient lookup.

Examples:

```text
customerId → Customer
orderId → Order
productId → Product
sessionId → Session
buildingId → GeographyHierarchy
```

Indexes should be owned by appropriate reference/context components.

---

# 51. Avoid Repeated Linear Scans

Do not repeatedly do:

```text
customers.find(_.id == customerId)
```

for millions of relationships.

Use indexed structures where scale requires them.

---

# 52. Deterministic Ordering

Reference collections should have deterministic ordering.

For example:

```text
sortBy(_.id)
```

before indexed random selection when order affects reproducibility.

---

# 53. Relationship Cardinality

Every relationship should have a documented cardinality.

Examples:

```text
Customer → Address
1 → N

Customer → Session
1 → N

Session → Event
1 → N

Customer → Order
1 → N

Order → OrderItem
1 → N

Order → Payment
1 → N or 1 → 1 depending on model

Order → Shipment
1 → N

Order → Return
1 → N
```

The exact cardinality must follow the current domain model.

---

# 54. Cardinality Is a Business Rule

Do not encode relationship counts only as loops.

For example:

```scala
for (i <- 1 to 3)
```

does not communicate:

> Customer typically has three Sessions.

A behavior model should eventually determine cardinality.

---

# 55. Relationship Cardinality Distribution

Future realistic models should support:

```text
Customer
  1 Session
  2 Sessions
  5 Sessions
  20 Sessions
```

with a configured distribution.

The same principle applies to:

```text
Orders/customer
Items/order
Events/session
Returns/order
```

---

# 56. Relationship Skew

Relationship cardinality is a major source of data skew.

For example:

```text
Customer A
   ↓
1000 Orders

Customer B
   ↓
2 Orders
```

This creates a realistic long tail.

---

# 57. Hot-Key Modeling

A scenario can explicitly define:

```text
hot customer fraction
hot customer multiplier
```

which changes relationship cardinality.

This is preferable to arbitrary duplication.

---

# 58. Relationship Realism

Realistic data requires correlation.

Weak model:

```text
Customer
   +
random Product
```

Better:

```text
CustomerBehaviorProfile
        ↓
Category affinity
        ↓
Brand affinity
        ↓
Product popularity
        ↓
OrderItem
```

---

# 59. Correlation Propagation

Customer behavior should propagate through domains:

```text
Customer profile
    ↓
Session frequency
    ↓
Event engagement
    ↓
Product exploration
    ↓
Order frequency
    ↓
Basket composition
    ↓
Return behavior
```

This creates a coherent synthetic world.

---

# 60. Domain Interaction Matrix

| Domain | Depends structurally on | Behavioral inputs |
|---|---|---|
| Customer | Reference data | acquisition, demographics, behavior |
| Address | Customer, Geography | geographic affinity |
| Product | Category, Brand | popularity, category/brand rules |
| Session | Customer | activity, lifecycle |
| Event | Session | journey, engagement, product affinity |
| Order | Customer | frequency, spending, conversion |
| OrderItem | Order, Product | affinity, basket behavior |
| Payment | Order | payment preference, success |
| Shipment | Order, Address | carrier, geography, SLA |
| Return | OrderItem / Order | return propensity, product behavior |

This matrix is an architectural guide, not an implementation contract.

---

# 61. Dependency Direction

A useful dependency direction is:

```text
Common Infrastructure
        ↓
Reference Data
        ↓
Domain Models
        ↓
Behavior Models
        ↓
Domain Generators
        ↓
Application Orchestration
        ↓
Output / Manifest / Global Validation
```

Avoid dependencies flowing backward.

---

# 62. Domain Models Should Stay Lightweight

A Product model should not know how to generate:

```text
Orders
Sessions
Events
```

Likewise:

```text
Customer
```

should not generate Orders.

Generation behavior belongs in generators/strategies.

---

# 63. Domain Models vs Behavior

Keep:

```text
Product
```

as data.

Keep:

```text
ProductPopularityModel
```

as behavior.

Keep:

```text
ProductGenerator
```

as orchestration of Product creation.

This separation improves readability.

---

# 64. Cross-Domain Behavior

Some behaviors naturally span domains.

Examples:

```text
PurchaseConversionService
ReturnEligibilityService
ShipmentDeliveryModel
```

These should live in appropriate domain/application areas rather than being hidden inside generic utilities.

---

# 65. Transaction Boundary

Order is the primary commercial transaction boundary.

The relationship chain is:

```text
Order
 ├── OrderItems
 ├── Payment
 ├── Shipment
 └── Return
```

This gives the project a natural transaction-centric structure.

---

# 66. Behavioral Boundary

Session is the primary digital-interaction boundary.

The relationship chain is:

```text
Session
 └── Events
```

Customer is the longer-lived behavioral boundary.

Thus:

```text
Customer
   ↓
Sessions
   ↓
Events
```

---

# 67. Customer as Latent Variable

CustomerBehaviorProfile acts as a latent variable connecting domains.

Conceptually:

```text
CustomerBehaviorProfile
       │
       ├── activity
       ├── spending
       ├── category affinity
       ├── brand affinity
       ├── price sensitivity
       ├── payment preference
       └── return propensity
```

These properties influence multiple downstream generators.

---

# 68. Avoid Copying Behavior Everywhere

Do not independently regenerate:

```text
customer activity
```

inside Session and:

```text
customer activity
```

inside Event.

Generate the underlying behavior once and consume it.

---

# 69. Behavior Profile Lifetime

The profile should normally be created once per Customer.

Then:

```text
Customer
   ↓
CustomerBehaviorProfile
```

is reused during generation.

This produces consistent behavior.

---

# 70. Temporal Behavior

Behavior can evolve over time.

Future models may support:

```text
new customer
   ↓
active
   ↓
loyal
   ↓
dormant
   ↓
reactivated
```

This affects Sessions, Events, Orders, and Returns.

---

# 71. Domain Interaction and Time

Cross-domain timestamps should be coherent.

Example:

```text
Event
   ↓
Purchase
   ↓
Order
   ↓
Payment
   ↓
Shipment
   ↓
Delivery
   ↓
Return
```

Each timestamp should respect business chronology.

---

# 72. Chronological Constraints

Examples:

```text
eventTime <= orderTime
orderTime <= paymentTime
paymentTime <= shipmentTime
shipmentTime <= deliveryTime
deliveryTime <= returnTime
```

The exact constraints depend on the final state models.

---

# 73. Relationship Validation Architecture

Validation should operate at multiple levels.

## Domain validation

Checks one domain.

## Relationship validation

Checks cross-domain references.

## Business validation

Checks business invariants.

## Statistical validation

Checks realism.

---

# 74. Domain Validation Example

Product validation:

```text
Product ID unique
Category ID valid
Brand ID valid
price valid
```

---

# 75. Relationship Validation Example

OrderItem:

```text
OrderItem.orderId exists
OrderItem.productId exists
```

---

# 76. Business Validation Example

Order:

```text
total = sum(OrderItems)
```

Payment:

```text
payment belongs to Order
```

Shipment:

```text
shipment references valid delivery Address
```

---

# 77. Statistical Validation Example

Customer:

```text
orders/customer distribution
```

Session:

```text
sessions/customer distribution
```

Event:

```text
events/session distribution
```

Product:

```text
orders/product concentration
```

Return:

```text
returns/customer concentration
```

---

# 78. Relationship Statistics

The project should eventually report:

```text
addresses/customer
sessions/customer
events/session
orders/customer
items/order
payments/order
shipments/order
returns/order
```

for each generated dataset.

---

# 79. Relationship Distribution Percentiles

For high-cardinality relationships, report:

```text
mean
median
p75
p90
p95
p99
max
```

This is essential for detecting unrealistic uniformity.

---

# 80. Referential Integrity

The baseline must guarantee:

```text
no orphan foreign keys
```

unless a data-quality scenario explicitly injects them.

---

# 81. Controlled Corruption

Quality scenarios may intentionally break:

```text
references
timestamps
relationships
```

The corruption should be:

```text
configured
measurable
reproducible
```

---

# 82. Reproducibility Across Domains

A relationship must remain deterministic for the same:

```text
seed
configuration
reference data
generation version
```

---

# 83. Random Stream Ownership

Prefer:

```text
customer random stream
session random stream
event random stream
order random stream
```

rather than one global mutable RNG sequence.

This makes domain changes less likely to alter unrelated domains.

---

# 84. Parallel Generation

Independent generation stages should be parallelizable where possible.

For example:

```text
Customer generation
Product generation
```

may be independently generated after reference data is available.

Downstream stages depend on generated references.

---

# 85. Parallelism Constraint

Do not sacrifice reproducibility merely to parallelize.

The design goal is:

```text
parallel generation
+
stable derived randomness
```

rather than:

```text
shared mutable Random
```

---

# 86. Output Independence

Relationship architecture must not depend on CSV.

The same domain objects should eventually support:

```text
CSV
Parquet
JSON
```

without changing business relationships.

---

# 87. Manifest Independence

Manifest generation should consume final outputs/statistics.

It should not alter relationships.

---

# 88. Scenario Independence

Scenario logic should modify behavior through explicit configuration.

Examples:

```text
high_activity
campaign_spike
hot_customer
hot_product
return_spike
```

The scenario engine should not directly manipulate arbitrary entity rows.

---

# 89. Scenario Composition

A scenario can conceptually modify:

```text
CustomerBehaviorProfile
```

which then propagates:

```text
Customer
   ↓
Session
   ↓
Event
   ↓
Order
   ↓
Return
```

This is more coherent than independent row-level hacks.

---

# 90. Example: Hot Customer Scenario

Weak implementation:

```text
duplicate random Orders for customer X
```

Better:

```text
CustomerBehaviorProfile
   activity = high
   purchaseFrequency = high
   spending = high
```

Then:

```text
more Sessions
more Events
more Orders
larger expected spend
```

emerge naturally.

---

# 91. Example: Campaign Scenario

Campaign parameters:

```text
campaign start
campaign end
traffic multiplier
conversion multiplier
category affinity
```

can influence:

```text
Sessions
Events
Orders
```

without directly editing generated rows.

---

# 92. Example: Return Spike

A return scenario can modify:

```text
category return propensity
```

or:

```text
customer return propensity
```

rather than creating arbitrary Return records.

---

# 93. Testing Relationship Architecture

Tests should exist at several levels.

## Unit tests

Test individual relationship policies.

## Domain tests

Test generator behavior.

## Integration tests

Test multiple domains together.

## End-to-end tests

Generate the complete dataset.

---

# 94. Relationship Unit Test

Example:

```text
ProductSelector
```

should select a valid Product according to its strategy.

---

# 95. Relationship Integration Test

Example:

```text
Order
   ↓
OrderItems
   ↓
Products
```

should maintain:

```text
valid orderId
valid productId
```

and expected business invariants.

---

# 96. Cross-Domain Contract Tests

Useful contracts:

```text
Session → Event
Order → OrderItem
OrderItem → Product
Order → Payment
Order → Shipment
OrderItem → Return
```

These protect architectural boundaries during refactoring.

---

# 97. Mutation Testing Mindset

A useful quality question is:

> If a relationship assignment is accidentally changed to random selection, will tests detect it?

If not, the tests are too structural and not behavioral enough.

---

# 98. Readability Test

A reviewer should be able to answer:

```text
Who creates this relationship?
Why is this entity selected?
Which behavior determines it?
Where is it validated?
```

by reading a small amount of code.

---

# 99. Anti-Pattern: Generic Helper

Avoid:

```text
RelationshipHelper
DataHelper
GenerationHelper
UtilityManager
```

when the name hides business behavior.

Prefer:

```text
ProductPopularityModel
ShipmentAddressResolver
ReturnEligibilityService
```

---

# 100. Anti-Pattern: Hidden Lookup

Avoid:

```scala
randomCustomer()
```

inside OrderItem or Event code.

The required relationship should be visible in the method contract.

---

# 101. Anti-Pattern: Hidden Global State

Avoid:

```text
global currentCustomer
global currentOrder
global currentSession
```

This creates non-local behavior and makes parallel generation difficult.

---

# 102. Anti-Pattern: Circular Domain Imports

Avoid:

```text
event → order → event
```

as direct implementation dependencies.

Use application-level coordination or explicit domain services.

---

# 103. Anti-Pattern: Entity Knows Its Generator

Avoid making:

```text
Customer
```

responsible for:

```text
CustomerGenerator
```

Models should remain independent data structures.

---

# 104. Anti-Pattern: Generator Knows Everything

Avoid:

```text
MasterGenerator
```

with hundreds of lines containing every relationship rule.

Application orchestration should compose focused generators.

---

# 105. Recommended Package Structure

Relationship architecture does not require its own giant package.

Recommended structure:

```text
com/shopsphere/datagenerator/

  customer/
  address/
  geography/
  category/
  brand/
  product/
  session/
  event/
  order/
  orderitem/
  payment/
  shipment/
  return/

  common/
  config/
  generation/
  scenario/
  quality/
  validation/
  statistics/
  output/
  manifest/
  observability/
```

Cross-domain services should live where their business ownership is clearest.

---

# 106. Relationship Package Assessment

Do not automatically create:

```text
relationship/
```

just because relationships exist.

A generic relationship package tends to become a dumping ground.

Use the package only for genuinely cross-domain infrastructure that has no better domain owner.

---

# 107. Application Layer

The:

```text
generation/
```

package should contain application orchestration.

Its responsibility is:

```text
what happens first
what happens next
what inputs flow between stages
```

not detailed business rules.

---

# 108. Domain Layer

Domain packages should contain:

```text
models
generators
behavior
domain-specific validation
```

as needed.

---

# 109. Infrastructure Layer

Infrastructure should contain:

```text
randomness
distributions
configuration loading
output
manifest
observability
```

and other technical mechanisms.

---

# 110. Dependency Rule

A useful mental model:

```text
Infrastructure
     ↑
Application
     ↑
Domain
```

More precisely, dependencies should be arranged so that domain business behavior does not become coupled to output or infrastructure implementation details.

---

# 111. Relationship Contracts

Each important cross-domain relationship should have a documented contract.

Example:

```text
OrderItem → Product

Input:
  Order context
  Customer behavior
  Product catalog

Guarantees:
  product exists
  product is selectable
  product belongs to valid category/brand
```

---

# 112. Contract Example: Event → Session

Input:

```text
Session
Customer behavior
Journey model
```

Guarantees:

```text
session exists
event timestamp is inside session
event customer is consistent
```

---

# 113. Contract Example: Shipment → Address

Input:

```text
Order
Customer addresses
Geography
```

Guarantees:

```text
address exists
address belongs to order customer
geography reference is valid
```

---

# 114. Contract Example: Return → OrderItem

Input:

```text
Order
OrderItems
Customer behavior
```

Guarantees:

```text
returned item belongs to order
returned quantity is valid
return is eligible
```

---

# 115. Relationship Error Handling

Relationship failures should be explicit.

Examples:

```text
No customers available
No products available
No addresses available
Invalid geography reference
Order has no eligible items
```

Do not silently create invalid relationships.

---

# 116. Configuration Boundaries

Configuration should define:

```text
cardinality
distributions
behavior
scenario parameters
```

It should not contain arbitrary runtime object references.

---

# 117. Relationship Configuration

Useful future configuration:

```hocon
relationships {
  customer {
    addresses = ...
    sessions = ...
    orders = ...
  }

  order {
    items = ...
    payments = ...
    shipments = ...
    returns = ...
  }

  session {
    events = ...
  }
}
```

The exact configuration structure should be designed before implementation.

---

# 118. Configuration vs Behavior

Configuration should express:

```text
what behavior is desired
```

Strategy implementations express:

```text
how behavior is generated
```

This separation supports multiple scenarios.

---

# 119. Factory Selection

If configuration says:

```text
product-popularity-model = "zipf"
```

a Factory may create:

```text
ZipfProductPopularityModel
```

Likewise:

```text
event-journey-model = "funnel"
```

may select:

```text
FunnelJourneyModel
```

---

# 120. Avoid Factory Everywhere

Factories are useful when runtime configuration selects among alternatives.

Do not create factories for:

```text
CustomerFactory
AddressFactory
ProductFactory
```

unless construction genuinely requires polymorphic selection.

---

# 121. Relationship Realism Maturity

Relationship realism can be viewed in levels.

## Level 1

Valid foreign keys.

## Level 2

Correct cardinality.

## Level 3

Variable cardinality distributions.

## Level 4

Behavioral affinity.

## Level 5

Cross-domain correlation.

## Level 6

Temporal coherence.

## Level 7

Scenario-driven propagation.

The project currently has a strong foundation at Levels 1–2, with portions of Level 3.

---

# 122. Current Relationship Strength

Current generation successfully establishes:

```text
Customer → Address
Customer → Session
Session → Event
Customer → Order
Order → OrderItem
OrderItem → Product
Order → Payment
Order → Shipment
Order → Return
```

with structural validation.

The major remaining work is behavioral realism.

---

# 123. Current Weaknesses

The main relationship weaknesses are:

```text
fixed cardinalities
weak behavioral affinity
independent selection in several areas
limited temporal coupling
limited Session → Order conversion linkage
limited basket behavior
limited cross-domain customer behavior propagation
```

---

# 124. Target Relationship Model

The target is:

```text
CustomerBehaviorProfile
       │
       ├────────→ Session frequency
       │                ↓
       │             Journey
       │                ↓
       │             Events
       │                ↓
       │             Conversion
       │                ↓
       ├────────→ Order frequency
       │                ↓
       │             Basket
       │                ↓
       │             Products
       │
       └────────→ Return propensity
```

This is the core behavioral graph.

---

# 125. Generation Ordering After Behavioral Maturity

The eventual workflow may be:

```text
Reference Data
      ↓
Catalog
      ↓
Customers + Behavior Profiles
      ↓
Sessions
      ↓
Event Journeys
      ↓
Conversion Decisions
      ↓
Orders
      ↓
OrderItems
      ↓
Payments
      ↓
Shipments
      ↓
Returns
```

This order preserves causal meaning.

---

# 126. Why Events Come Before Orders

If the project wants realistic digital conversion:

```text
Session
  ↓
Events
  ↓
Purchase
  ↓
Order
```

This is more meaningful than independently generating:

```text
Sessions
Orders
```

and later pretending they are related.

---

# 127. Why Orders Come Before Returns

Returns are downstream of:

```text
Order
OrderItem
Shipment/Delivery
```

Therefore Return generation should consume those records.

---

# 128. Why Product Exists Before Transactions

Products are catalog entities.

Transactions reference Products.

Therefore:

```text
Product
   ↓
OrderItem
```

and:

```text
Product
   ↓
Event
```

are natural dependencies.

---

# 129. Relationship Graph as a Business Graph

The generator should be understood as producing a synthetic business world:

```text
People
  ↓
Locations
  ↓
Catalog
  ↓
Digital interactions
  ↓
Commercial transactions
  ↓
Fulfillment
  ↓
Post-purchase behavior
```

This is more useful than thinking of the project as independent CSV generators.

---

# 130. Spark Implications

The relationship graph intentionally creates realistic join paths:

```text
Customer ↔ Order
Order ↔ OrderItem
OrderItem ↔ Product
Order ↔ Payment
Order ↔ Shipment
OrderItem ↔ Return
Customer ↔ Session
Session ↔ Event
```

These become future Spark workloads.

---

# 131. Join Cardinality

Different joins should have different cardinalities.

Examples:

```text
Customer → Order
1:N

Order → OrderItem
1:N

Session → Event
1:N
```

This matters for:

- join size,
- aggregation,
- partitioning,
- skew,
- shuffle volume.

---

# 132. Join Skew

The same behavioral models that improve realism can intentionally create:

```text
hot customers
hot products
hot sessions
```

This makes Spark skew experiments meaningful.

---

# 133. Relationship Selectivity

Future workloads should have different selectivities.

For example:

```text
Orders for one customer
Events for one session
Orders containing one product
Returns for one category
```

This helps exercise Spark filtering and joins.

---

# 134. Data Volume Propagation

Small changes in relationship cardinality can have large volume effects.

For example:

```text
Customers
   × orders/customer
      × items/order
         = OrderItems
```

Likewise:

```text
Customers
   × sessions/customer
      × events/session
         = Events
```

Therefore cardinality configuration is a major scale-control mechanism.

---

# 135. Relationship Configuration Must Be Safe

A configuration such as:

```text
100 sessions/customer
50 events/session
```

can produce enormous data.

The generator should estimate output volume before generation.

This belongs in GenerationPlan.

---

# 136. Relationship Planning

Generation planning should eventually calculate:

```text
customers
addresses
sessions
events
orders
orderItems
payments
shipments
returns
```

from cardinality and distribution settings.

---

# 137. Relationship Statistics as Feedback

After generation:

```text
planned cardinality
        ↓
actual cardinality
        ↓
statistics
        ↓
validation
```

This provides a feedback loop for calibration.

---

# 138. Design Review Checklist

For every relationship, ask:

1. What is the business meaning?
2. Who owns the relationship?
3. What is the cardinality?
4. Is the relationship structural or behavioral?
5. Which strategy determines it?
6. What inputs does the strategy need?
7. What invariants must hold?
8. How is it validated?
9. How is it made reproducible?
10. How can skew be introduced?
11. What happens at large scale?

---

# 139. Relationship Quality Gate

A relationship is architecturally complete when:

### Ownership

- one domain clearly owns generation.

### Meaning

- relationship has documented business meaning.

### Cardinality

- expected cardinality is explicit.

### Behavior

- behavioral selection has an explicit model where required.

### Validation

- structural and business invariants are tested.

### Reproducibility

- selection is deterministic for the same inputs.

### Scale

- lookup is efficient enough for intended volumes.

### Scenarios

- skew/corruption can be introduced deliberately.

---

# 140. Refactoring Rule

When moving an entity package:

> Move the entity's relationship behavior with the entity unless that behavior genuinely belongs to a cross-domain service or application workflow.

Do not mechanically move all relationship code into a central package.

---

# 141. Refactoring Rule: Preserve Behavior First

During architectural migration:

```text
existing valid behavior
        ↓
move
        ↓
tests
        ↓
refactor
        ↓
improve realism
```

Do not combine a package migration with an uncontrolled behavioral rewrite.

---

# 142. Refactoring Rule: One Concern at a Time

A safe sequence is:

```text
model
→ generator
→ focused tests
→ orchestration
→ validation
→ statistics
→ full suite
```

This keeps failures diagnosable.

---

# 143. Final Architecture

The target relationship architecture is:

```text
                    ┌───────────────┐
                    │   Geography   │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Address    │
                    └───────┬───────┘
                            │
                            ▼
┌───────────────┐     ┌───────────────┐
│    Product    │◄────│    Customer   │
│ ┌───────────┐ │     │  Behavior     │
│ │ Category  │ │     │   Profile     │
│ │ Brand     │ │     └───────┬───────┘
│ └───────────┘ │             │
└───────┬───────┘             │
        │                      ├─────────────┐
        │                      ▼             ▼
        │                 ┌─────────┐   ┌─────────┐
        │                 │ Session │   │  Order  │
        │                 └────┬────┘   └────┬────┘
        │                      │             │
        │                      ▼             ├── OrderItem ──► Product
        │                   Event            ├── Payment
        │                                    ├── Shipment ──► Address
        │                                    └── Return ────► OrderItem
        │
        └───────────────────────────────────────────────┘
```

---

# 144. Final Mental Model

There are three major relationship layers.

## Layer 1 — Structural

```text
foreign keys
references
cardinality
```

## Layer 2 — Behavioral

```text
affinity
propensity
frequency
popularity
```

## Layer 3 — Temporal

```text
journey
sequence
lifecycle
causal ordering
```

A sophisticated generator needs all three.

---

# 145. Summary

ShopSphere should not be implemented as a set of independent entity generators connected by a generic RelationshipManager.

Instead:

```text
Each domain owns its structural relationships.
Focused domain services own genuine cross-domain business decisions.
Application orchestration owns workflow.
Behavior models own statistical/behavioral selection.
Validation owns integrity checking.
Statistics own measurement.
Infrastructure owns randomness, configuration, output, and technical mechanisms.
```

The resulting architecture is:

```text
CustomerBehaviorProfile
          ↓
Domain Behavior Models
          ↓
Focused Domain Generators
          ↓
Application Orchestration
          ↓
Relationship Graph
          ↓
Validation
          ↓
Statistics
          ↓
Output
```

The most important architectural principle is:

> **Relationships are business behavior, not plumbing. Make their ownership, meaning, selection policy, and validation explicit.**

The next documentation stage should define the **cross-cutting randomness, distributions, and reproducibility architecture**, because these mechanisms are used by nearly every domain and directly determine whether the behavioral relationships remain realistic, configurable, and deterministic.
