# 30 — Final Architecture Blueprint & Implementation Roadmap

## 1. Purpose

This is the final architecture blueprint for the ShopSphere e-commerce data generator before the major implementation refactoring begins.

The preceding documents established:

```text
business domain
domain boundaries
behavioral realism
relationships
randomness
time
configuration
validation
data quality
skew
output
testing
performance
readability
architecture decisions
```

This document connects those decisions into one implementation plan.

Its purpose is to answer:

```text
What are we building?
Where does each responsibility live?
How do the components interact?
In what order should implementation proceed?
How do we know each step is complete?
```

---

# 2. System Objective

ShopSphere is a configurable synthetic e-commerce data generator designed to create a statistically plausible, internally coherent e-commerce world.

It is not intended to reproduce a private company's actual dataset.

Its purpose is to provide:

```text
realistic synthetic data
+
controlled distributions
+
meaningful relationships
+
configurable scale
+
reproducibility
+
controlled skew
+
controlled data-quality defects
+
benchmark-friendly output
```

for future data engineering and Spark performance experiments.

---

# 3. Core Design Principle

The final architecture follows:

```text
business meaning
        ↓
domain behavior
        ↓
domain generation
        ↓
domain relationships
        ↓
validation/statistics
        ↓
output
        ↓
Spark workloads
```

Technical infrastructure supports this flow.

It should not obscure it.

---

# 4. Architecture Goals

The implementation should optimize for:

### Correctness

Generated relationships and business invariants must be valid unless an explicit quality scenario intentionally violates them.

### Realism

Distributions and relationships should resemble plausible e-commerce behavior.

### Configurability

Scale, cardinality, distributions, scenarios, and quality profiles should be configurable.

### Reproducibility

Equivalent effective inputs should reproduce the intended dataset semantics.

### Readability

Code should communicate business intent.

### Testability

Behavior should be independently testable.

### Scalability

The generator should eventually handle large and xlarge profiles efficiently.

### Benchmark Utility

Generated datasets should support meaningful Spark workloads.

---

# 5. Current Starting Point

The project already has a functioning foundation.

Current baseline:

```text
Scala 2.13.18
Java 17
SBT 1.13.0
```

The current test suite has:

```text
229 tests passing
```

The current end-to-end baseline generates and validates the major entities successfully.

Current validation baseline:

```text
Structural PASS
Primary-key PASS
Foreign-key PASS
Business-rule PASS
```

This baseline is protected during architecture migration.

---

# 6. Current Realism Gaps

The existing generator is structurally stronger than it is behaviorally realistic.

Known simplifications include:

```text
identical session counts per customer
identical event counts per session
narrow basket-size distribution
simplified customer behavior
simplified product popularity
limited customer/product affinity
simplified monetary behavior
limited temporal realism
simplified return behavior
limited scenario interaction
```

These are deliberate future work items, not reasons to discard the existing foundation.

---

# 7. Target Package Architecture

The target implementation is:

```text
com.shopsphere.datagenerator/
│
├── Main.scala
│
├── customer/
│   ├── config/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   ├── validation/
│   └── statistics/
│
├── address/
│   ├── model/
│   ├── generator/
│   └── validation/
│
├── geography/
│   ├── model/
│   ├── reference/
│   └── loader/
│
├── category/
│   ├── model/
│   ├── generator/
│   └── ...
│
├── brand/
│   ├── model/
│   ├── generator/
│   └── ...
│
├── product/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   ├── validation/
│   └── statistics/
│
├── order/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   ├── validation/
│   └── statistics/
│
├── orderitem/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── payment/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── shipment/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── return/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── session/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── event/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   └── validation/
│
├── common/
│   ├── distribution/
│   ├── random/
│   ├── time/
│   └── util/
│
├── config/
├── generation/
├── relationship/
├── quality/
├── scenario/
├── validation/
├── output/
├── manifest/
├── statistics/
└── observability/
```

Not every domain requires every subpackage.

Empty conceptual packages should not be created simply to satisfy the diagram.

---

# 8. Domain Ownership

| Domain | Primary Responsibility |
|---|---|
| Customer | Customer identity and behavior |
| Address | Customer address references |
| Geography | Hierarchical location reference data |
| Category | Product taxonomy |
| Brand | Product brand reference data |
| Product | Catalog products and product behavior |
| Order | Customer transaction and lifecycle |
| OrderItem | Basket/product/quantity/price |
| Payment | Payment lifecycle and method behavior |
| Shipment | Fulfillment and delivery lifecycle |
| Return | Return eligibility and lifecycle |
| Session | Customer activity sessions |
| Event | Session journey events |

---

# 9. Cross-Cutting Ownership

| Package | Responsibility |
|---|---|
| common | Small reusable technical primitives |
| config | Configuration loading and typed configuration |
| generation | Application generation orchestration |
| relationship | Focused cross-domain relationship operations |
| quality | Controlled data-quality scenarios |
| scenario | Scenario composition and runtime behavior |
| validation | Global validation orchestration |
| output | Serialization and dataset publication |
| manifest | Dataset/run metadata |
| statistics | Global statistics |
| observability | Progress and performance measurement |

---

# 10. Dependency Direction

The preferred dependency direction is:

```text
Main
 ↓
application composition
 ↓
generation orchestration
 ↓
domain components
 ↓
common abstractions
```

Infrastructure boundaries are injected rather than accessed globally.

---

# 11. Domain Dependency Rule

A domain may depend on another domain when the business relationship requires it.

However:

```text
dependency
```

should not mean:

```text
everything imports everything else
```

Use:

```text
IDs
reference interfaces
focused services
context objects
application orchestration
```

where appropriate.

---

# 12. Domain Models

Domain models should remain simple and immutable.

Examples:

```scala
case class Customer(...)
case class Product(...)
case class Order(...)
case class OrderItem(...)
case class Payment(...)
case class Shipment(...)
case class Return(...)
case class Session(...)
case class Event(...)
```

They represent business data rather than generation mechanics.

---

# 13. Address Model

The current Address model remains:

```scala
case class Address(
  id: String,
  customerId: String,
  buildingId: String,
  unitNumber: String,
  postalCode: String
)
```

This is intentional.

Geography is resolved through:

```text
buildingId
postalCode
```

The architecture must not reintroduce duplicate city/state/country fields merely for convenience.

---

# 14. Geography Model

Geography remains:

```text
Country
→ State
→ City
→ Area
→ Road
→ Society
→ Building
→ Flat/Unit
```

Postal code belongs to Area.

Geography is reference data.

---

# 15. Customer as Behavioral Anchor

Customer becomes the principal behavioral anchor of the simulation.

A future customer may carry or be associated with:

```text
activity propensity
purchase frequency
spending propensity
price sensitivity
category affinity
brand affinity
device preference
payment preference
return propensity
lifecycle state
```

These characteristics influence downstream generation.

---

# 16. CustomerBehaviorProfile

The target architecture introduces:

```text
CustomerBehaviorProfile
```

as a latent representation.

Conceptually:

```text
Customer
   ↓
CustomerBehaviorProfile
   ↓
┌───────┬────────┬──────────┬─────────┬─────────┐
Session Order    Product    Payment   Return
```

The profile should not become a dumping ground for every possible attribute.

Only behaviorally meaningful latent properties belong there.

---

# 17. Customer Behavior Strategies

Potential strategies include:

```text
CustomerActivityModel
CustomerSpendingModel
CustomerPurchaseFrequencyModel
CustomerAffinityModel
CustomerReturnPropensityModel
```

Not all need to be separate classes immediately.

Create them when their behavior becomes independently meaningful.

---

# 18. Product Behavior

Product behavior should eventually include:

```text
global popularity
customer affinity
category affinity
brand affinity
price sensitivity
seasonality
promotion
availability
```

The product catalog and product demand are distinct.

---

# 19. Category Behavior

Category defines catalog taxonomy.

Future demand can be influenced by:

```text
global category popularity
customer category affinity
seasonality
campaigns
lifecycle
```

Category cardinality is not the same as category demand.

---

# 20. Brand Behavior

Brand behavior may include:

```text
global brand popularity
category-conditioned brand affinity
customer brand affinity
price positioning
promotion
```

Brand popularity and brand catalog size remain distinct concepts.

---

# 21. Order Behavior

Order generation should eventually be driven by:

```text
customer activity
purchase frequency
purchase intent
temporal context
campaigns
session conversion
```

rather than an identical number of orders per customer.

---

# 22. Order Lifecycle

The order lifecycle remains conceptually:

```text
PLACED
→ CONFIRMED
→ SHIPPED
→ DELIVERED
```

with:

```text
CANCELLED
```

as an alternative path.

Future lifecycle modeling should preserve valid transitions.

---

# 23. OrderItem Behavior

OrderItem should model:

```text
basket size
product selection
quantity
transaction price
discount
line total
```

and eventually:

```text
basket complementarity
customer preference
product popularity
```

---

# 24. Basket Model

The baseline currently produces:

```text
2–3 items/order
mean ≈ 2.40
```

The target architecture allows a broader distribution.

Potential approaches:

```text
Poisson-like
negative binomial
zero-truncated count model
mixture
empirically calibrated distribution
```

The final choice must be based on desired business behavior.

---

# 25. Pricing Model

Separate:

```text
catalog price
```

from:

```text
transaction price
```

Transaction price can eventually depend on:

```text
base price
category
brand
customer segment
promotion
campaign
time
```

Order totals are derived from transaction line values.

---

# 26. Payment Architecture

Payment generation should eventually model:

```text
method preference
attempt
success/failure
retry
switching
timing
refund
```

rather than only selecting a method label.

---

# 27. Shipment Architecture

Shipment generation should eventually model:

```text
carrier
service level
dispatch
transit
delivery
delay
failure
RTO
```

and correlate timing with:

```text
geography
season
carrier
order characteristics
```

---

# 28. Return Architecture

Return behavior should eventually be conditional:

```text
customer propensity
product/category propensity
order context
delivery experience
return window
```

A return is not simply:

```text
random.nextDouble() < globalReturnRate
```

---

# 29. Session Architecture

Session generation should model:

```text
customer activity
session start time
duration
device
channel
intent
conversion
```

Session count per customer should eventually be heterogeneous.

---

# 30. Event Architecture

Event generation should model journeys rather than independent labels.

Conceptual path:

```text
landing
→ browse
→ product_view
→ add_to_cart
→ checkout
→ payment
→ purchase
```

with alternative paths:

```text
landing
→ browse
→ product_view
→ exit
```

or:

```text
product_view
→ add_to_cart
→ checkout
→ abandonment
```

---

# 31. Temporal Architecture

Time is a first-class dependency.

The simulation should have:

```text
DatasetClock
TemporalContext
TemporalDistribution
```

or equivalent focused abstractions.

Avoid direct:

```scala
Instant.now()
```

inside generation logic.

---

# 32. Temporal Context

A temporal context can carry concepts such as:

```text
simulation date
dataset window
season
campaign
day-of-week
time-of-day
```

without becoming a global service container.

---

# 33. Randomness Architecture

Randomness follows:

```text
root seed
→ semantic stream
→ entity
→ record
→ behavior
```

Example:

```text
customer-123
  ├── behavior
  ├── acquisition
  ├── activity
  └── preferences
```

This isolates unrelated random decisions.

---

# 34. Distribution Architecture

Distributions remain technical statistical primitives.

Potential implementations:

```text
Uniform
WeightedCategorical
Bernoulli
Normal
LogNormal
Truncated
Poisson
NegativeBinomial
Zipf
Pareto
Mixture
```

Business models select and interpret these distributions.

---

# 35. Important Separation

Do not confuse:

```text
Distribution
```

with:

```text
Business Behavior Model
```

For example:

```text
ZipfDistribution
```

does not mean:

```text
ProductPopularityModel
```

The latter owns the business meaning.

---

# 36. Configuration Architecture

Configuration flow:

```text
HOCON
 ↓
raw configuration
 ↓
typed configuration
 ↓
validated effective configuration
 ↓
runtime components
```

Raw HOCON should not leak into domain generators.

---

# 37. Profiles

Profiles primarily control structural/scale dimensions.

Examples:

```text
small
medium
large
xlarge
```

They can define:

```text
customer count
product count
order count
session volume
event volume
```

---

# 38. Scenarios

Scenarios primarily control behavioral/workload dimensions.

Examples:

```text
baseline
hot-product
festival-season
high-activity
dirty
high-skew
```

---

# 39. Scenario Composition

A scenario may combine:

```text
large
+
hot-product
+
festival-season
+
dirty-foreign-key
```

The effective configuration must have deterministic precedence.

---

# 40. Generation Request

The application should eventually have a clear generation request concept containing the requested run inputs.

Conceptually:

```text
seed
profile
cardinality profile
scenario
output settings
```

The exact model may evolve during implementation.

---

# 41. Effective Configuration

The application should resolve an effective configuration before generation.

It should represent the actual values used after:

```text
defaults
profile
scenario
explicit overrides
```

are applied.

---

# 42. Generation Plan

Generation planning determines requested cardinalities and dependencies.

Conceptually:

```text
Customer
Address
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

with dependency-aware counts.

---

# 43. Dependency Graph

The conceptual dependency graph is:

```text
Reference Data
   │
   ├── Geography
   ├── Category
   ├── Brand
   └── Product
          │
          ↓
Customer ─────────────┐
   │                  │
   ├── Address        │
   ├── Session        │
   │     └── Event    │
   │                  │
   └────────→ Order ←─┘
                │
                ├── OrderItem ──→ Product
                ├── Payment
                ├── Shipment
                └── Return
```

This is a conceptual dependency model, not necessarily the exact execution sequence.

---

# 44. Important Dependency Clarification

Order and OrderItem have a logical circularity if viewed carelessly:

```text
Order total depends on OrderItems
OrderItems depend on Order
```

The implementation should resolve this through generation orchestration.

A practical flow is:

```text
create order identity/context
→ generate items
→ calculate totals
→ finalize order
```

The domain model does not require an object graph containing recursive references.

---

# 45. Generation Orchestrator

The application-level orchestrator should coordinate:

```text
preflight
→ reference data
→ planning
→ domain generation
→ validation
→ statistics
→ output
→ manifest
→ publication
```

It should not contain all domain business rules.

---

# 46. Preflight

Preflight should validate:

```text
configuration
reference data
resource safety
scenario compatibility
output location
```

before expensive generation begins.

---

# 47. Reference Data Loading

Reference data should be loaded once per run.

Conceptually:

```text
load
→ normalize
→ validate
→ index
→ fingerprint
→ expose immutable reference snapshot
```

---

# 48. Customer Generation

Target flow:

```text
select/generate identity
→ generate behavior profile
→ generate acquisition
→ generate preferences
→ create Customer
```

The generator should not own every behavior strategy.

---

# 49. Address Generation

Target flow:

```text
customer
→ select building
→ resolve geography
→ select valid unit
→ select area postal code
→ create Address
```

The existing five-field Address contract remains intact.

---

# 50. Product Generation

Target flow:

```text
category
→ brand
→ base pricing
→ product identity
→ catalog attributes
```

Demand popularity is modeled separately from catalog creation.

---

# 51. Order Generation

Target flow:

```text
select eligible customer
→ determine order timing
→ establish order context
→ create order identity
→ generate basket
→ calculate total
→ assign lifecycle state
```

---

# 52. OrderItem Generation

Target flow:

```text
sample basket size
→ select product
→ sample quantity
→ determine transaction price
→ calculate line total
→ create OrderItem
```

---

# 53. Payment Generation

Target flow:

```text
order
→ select payment method
→ generate attempt
→ determine outcome
→ retry/switch if modeled
→ create Payment
```

---

# 54. Shipment Generation

Target flow:

```text
eligible order
→ determine fulfillment
→ select shipment context
→ calculate dispatch time
→ calculate delivery time
→ create Shipment
```

---

# 55. Return Generation

Target flow:

```text
eligible delivered item/order
→ evaluate return propensity
→ evaluate return window
→ select reason
→ determine quantity
→ create Return
```

---

# 56. Session Generation

Target flow:

```text
select active customer
→ determine session timing
→ determine duration
→ determine device/channel
→ determine intent
→ create Session
```

---

# 57. Event Generation

Target flow:

```text
session
→ select journey
→ generate ordered events
→ allocate timestamps
→ update journey state
→ create Event records
```

---

# 58. Relationship Services

Cross-domain operations should be focused.

Examples:

```text
ProductSelector
ReturnEligibilityService
ShipmentSchedulingService
SessionAttributionService
```

Avoid a universal relationship manager.

---

# 59. Validation Flow

Validation occurs after generation, with appropriate streaming/phase validation where useful.

Conceptual flow:

```text
structural
→ primary keys
→ foreign keys
→ relationships
→ business rules
→ temporal rules
→ statistical validation
→ scenario-specific quality validation
```

---

# 60. Statistics Flow

Statistics should capture:

```text
row counts
cardinality
distribution summaries
quantiles
relationship counts
correlations
skew
quality violation rates
```

and global metrics such as:

```text
generation duration
throughput
output size
```

---

# 61. Output Flow

The output boundary should:

```text
serialize
→ write
→ finalize
→ validate artifact metadata
→ publish
```

without exposing filesystem concerns to domain code.

---

# 62. Manifest Flow

The manifest should capture:

```text
dataset identity
run identity
generator version
seed
effective configuration
reference-data fingerprint
profile
scenario
requested counts
actual counts
validation result
statistics
output metadata
completion state
```

---

# 63. Publication Semantics

A dataset is not considered published until:

```text
generation completed
+
validation passed according to scenario
+
manifest finalized
+
output publication succeeded
```

Partial output should not masquerade as a completed dataset.

---

# 64. Failure Flow

Conceptually:

```text
failure
 ↓
classify
 ↓
attach context
 ↓
mark run failed
 ↓
prevent publication
 ↓
clean temporary output
```

unless a deliberately configured recovery behavior applies.

---

# 65. Error Categories

The implementation should distinguish:

```text
configuration error
reference-data error
generation error
relationship error
validation error
scenario error
output error
resource error
```

The exact exception hierarchy should remain small and meaningful.

---

# 66. Quality Scenario Flow

A dirty scenario should be:

```text
explicitly selected
→ configured
→ deterministically applied
→ measured
→ validated against expected violation rates
```

Unexpected violations remain failures.

---

# 67. Skew Scenario Flow

Skew should be intentionally modeled.

Examples:

```text
hot customers
hot products
hot categories
geographic concentration
campaign spikes
partition-oriented hot keys
```

The scenario should record measurable concentration metrics.

---

# 68. Workload-Oriented Dataset Families

Eventually provide dataset families for:

```text
baseline
large
xlarge
high-cardinality
high-skew
dirty
temporal
high-activity
```

Each should have documented characteristics.

---

# 69. Spark Workload Mapping

The generated data should support future labs such as:

```text
large joins
skewed joins
groupBy aggregation
window functions
Top-K
filter selectivity
funnel analysis
customer segmentation
data-quality detection
temporal analysis
```

The generator does not implement those Spark jobs.

---

# 70. Testing Architecture

Testing remains layered.

### Unit

```text
models
distributions
behavior models
selectors
services
generators
```

### Integration

```text
configuration
reference data
multi-domain relationships
output
```

### End-to-End

```text
configuration
→ generation
→ validation
→ output
→ manifest
```

### Statistical

```text
distribution shape
correlation
skew
behavior rates
```

### Reproducibility

```text
same inputs
→ expected same output
```

---

# 71. Architecture Refactoring Rule

Do not simultaneously:

```text
move packages
+
rewrite business behavior
+
change distributions
+
change schemas
+
optimize performance
```

unless necessary.

Prefer controlled changes.

---

# 72. One-File-at-a-Time Implementation Workflow

The implementation migration should use:

```text
1. Select one file.
2. Inspect dependencies.
3. Determine target package/responsibility.
4. Move or refactor the file.
5. Fix direct references.
6. Compile.
7. Run relevant tests.
8. Run full tests.
9. Review diff.
10. Continue.
```

This is the default migration discipline.

---

# 73. First Implementation Phase

Before moving entity generators, establish or clean up the common infrastructure.

Focus on:

```text
randomness
distribution
time
small shared utilities
```

Do not build speculative abstractions.

---

# 74. Second Implementation Phase

Migrate reference domains:

```text
geography
category
brand
product
```

This establishes clean catalog/reference ownership.

---

# 75. Third Implementation Phase

Migrate:

```text
customer
address
```

Customer is especially important because it becomes the behavioral anchor.

Address follows geography and customer boundaries.

---

# 76. Fourth Implementation Phase

Migrate transactional domains:

```text
order
orderitem
payment
shipment
return
```

Maintain existing structural correctness before adding sophisticated realism.

---

# 77. Fifth Implementation Phase

Migrate behavioral activity domains:

```text
session
event
```

These will later become important inputs to conversion and journey realism.

---

# 78. Sixth Implementation Phase

Clean up cross-domain infrastructure:

```text
relationship
validation
statistics
quality
scenario
```

After domain ownership becomes clear, remove obsolete or generic responsibilities.

---

# 79. Seventh Implementation Phase

Refactor application orchestration:

```text
Main
GenerationPipeline
composition root
generation plan
run lifecycle
```

The final orchestrator should read like an application workflow.

---

# 80. Eighth Implementation Phase

Finalize:

```text
output
manifest
observability
failure semantics
```

and verify the full run lifecycle.

---

# 81. Ninth Implementation Phase

Only after structural migration is stable, begin realism implementation.

Recommended order:

```text
CustomerBehaviorProfile
→ customer activity
→ order frequency
→ basket size
→ product popularity
→ affinity
→ pricing/spending
→ temporal realism
→ session behavior
→ event journeys
→ return propensity
→ advanced scenarios
```

---

# 82. Tenth Implementation Phase

After realism stabilizes:

```text
performance profiling
→ indexing
→ allocation optimization
→ streaming
→ parallelism
→ large/xlarge tuning
```

Optimization should be driven by measurements.

---

# 83. Milestone M0 — Architecture Baseline

Complete when:

```text
architecture documents complete
ADR decisions recorded
current tests green
current E2E run green
```

Status:

```text
Ready
```

---

# 84. Milestone M1 — Common Infrastructure

Complete when:

```text
randomness boundary is clean
distribution boundary is clean
time boundary is clean
tests remain green
```

---

# 85. Milestone M2 — Reference Data

Complete when:

```text
geography is isolated
category is isolated
brand is isolated
product is isolated
lookup/indexing is explicit
reference validation is explicit
```

---

# 86. Milestone M3 — Customer and Address

Complete when:

```text
Customer domain is isolated
Customer behavior boundary exists
Address domain is isolated
Geography relationship is clean
```

Do not require the complete behavior profile implementation before the package migration.

---

# 87. Milestone M4 — Transaction Domains

Complete when:

```text
Order
OrderItem
Payment
Shipment
Return
```

have clear ownership and tests.

---

# 88. Milestone M5 — Activity Domains

Complete when:

```text
Session
Event
```

have clear ownership and relationship boundaries.

---

# 89. Milestone M6 — Cross-Cutting Cleanup

Complete when:

```text
generic relationship logic is removed/reduced
validation ownership is clear
statistics ownership is clear
scenario/quality boundaries are clear
```

---

# 90. Milestone M7 — Application Architecture

Complete when:

```text
composition root is clear
generation lifecycle is explicit
failure semantics are explicit
manifest/output lifecycle is explicit
```

---

# 91. Milestone M8 — Realism V1

Target capabilities:

```text
heterogeneous customer behavior
variable order frequency
broader basket distribution
long-tail product demand
customer/category/product correlation
better monetary realism
```

---

# 92. Milestone M9 — Temporal and Journey Realism

Target capabilities:

```text
temporal lifecycle
session heterogeneity
event journeys
conversion funnels
campaign/seasonality
```

---

# 93. Milestone M10 — Advanced Scenarios

Target capabilities:

```text
controlled skew
controlled dirty data
high-activity scenarios
campaign spikes
workload-specific distributions
```

---

# 94. Milestone M11 — Scale and Performance

Target capabilities:

```text
large
xlarge
streaming
allocation control
reference indexing
parallel generation
performance budgets
```

---

# 95. Milestone M12 — Spark Benchmark Readiness

Complete when:

```text
dataset families are documented
dataset identity is stable
manifests are complete
skew is measurable
quality scenarios are measurable
Spark workload properties are documented
```

---

# 96. Definition of Done — Domain

A domain is considered migrated when:

```text
[ ] model ownership is clear
[ ] generator ownership is clear
[ ] dependencies are explicit
[ ] configuration ownership is clear
[ ] behavior abstractions are justified
[ ] validation ownership is clear
[ ] statistics ownership is clear
[ ] tests are colocated logically
[ ] package naming is clear
[ ] existing behavior is preserved
[ ] documentation matches implementation
```

---

# 97. Definition of Done — Behavior Model

A behavior model is complete when:

```text
[ ] business meaning is documented
[ ] inputs are explicit
[ ] randomness is deterministic
[ ] distribution is appropriate
[ ] bounds are defined
[ ] edge cases are tested
[ ] statistical behavior is tested where appropriate
[ ] alternative strategies are justified
```

---

# 98. Definition of Done — Scenario

A scenario is complete when:

```text
[ ] intent is documented
[ ] affected domains are explicit
[ ] configuration is validated
[ ] precedence is deterministic
[ ] output impact is measurable
[ ] reproducibility is preserved
[ ] expected quality violations are defined
```

---

# 99. Definition of Done — Dataset

A generated dataset is complete when:

```text
[ ] requested profile resolved
[ ] effective configuration recorded
[ ] reference data validated
[ ] generation completed
[ ] validation completed
[ ] statistics generated
[ ] manifest finalized
[ ] output published successfully
```

---

# 100. Definition of Done — Architecture

The architecture is mature when:

```text
[ ] domains have clear ownership
[ ] generators remain readable
[ ] behavior models are composable
[ ] dependencies are visible
[ ] randomness is explicit
[ ] time is explicit
[ ] relationships are focused
[ ] validation is layered
[ ] scenarios are composable
[ ] output is decoupled
[ ] Spark is downstream
[ ] large-scale generation is feasible
```

---

# 101. Architecture Quality Gates

Every major refactoring step must satisfy four gates.

## Gate 1 — Correctness

```text
compile
+
tests
+
relevant E2E behavior
```

## Gate 2 — Architecture

```text
ownership
+
dependencies
+
responsibilities
```

## Gate 3 — Readability

```text
naming
+
business flow
+
minimal indirection
```

## Gate 4 — Future Extensibility

```text
can future realism be added cleanly?
```

---

# 102. Regression Strategy

After each coherent migration step:

```powershell
sbt compile
sbt test
```

For pipeline-level changes:

```powershell
sbt run
```

The baseline must remain available for comparison.

---

# 103. Git Strategy

Prefer commits such as:

```text
refactor: move geography model into geography domain
refactor: isolate product generation
refactor: introduce customer behavior boundary
refactor: simplify relationship ownership
```

Avoid commits combining unrelated:

```text
architecture
realism
performance
schema
```

changes.

---

# 104. Realism Regression

Once realism work begins, preserve reference datasets/statistics for comparison.

For example:

```text
baseline seed
baseline profile
baseline scenario
```

can be used to detect accidental distribution changes.

---

# 105. Statistical Regression

Track important metrics such as:

```text
items/order
orders/customer
sessions/customer
events/session
return rate
average order value
median order value
product concentration
category concentration
conversion rate
```

Thresholds should be calibrated rather than arbitrary.

---

# 106. Reproducibility Regression

Test:

```text
same seed
same configuration
same reference data
same generator version
```

and verify the expected reproducibility level.

---

# 107. Performance Regression

Track:

```text
records/second
phase duration
memory
output throughput
```

at representative dataset sizes.

Do not establish performance budgets before measuring a credible baseline.

---

# 108. Migration Risk — Hidden Dependencies

Risk:

```text
a class is imported by many packages
```

Mitigation:

```text
inspect references
move incrementally
compile immediately
```

---

# 109. Migration Risk — Behavioral Drift

Risk:

```text
package migration accidentally changes behavior
```

Mitigation:

```text
tests
E2E comparison
reference statistics
```

---

# 110. Migration Risk — Over-Abstraction

Risk:

```text
every moved class receives an interface/factory/strategy
```

Mitigation:

```text
apply ADR rules
```

---

# 111. Migration Risk — Customer Behavior Becomes a God Object

Risk:

```text
CustomerBehaviorProfile
```

contains every attribute in the system.

Mitigation:

Keep it focused on latent behavioral properties.

Domain-specific state remains in its owning domain.

---

# 112. Migration Risk — Orchestrator Becomes a God Object

Risk:

```text
GenerationOrchestrator
```

contains all business rules.

Mitigation:

Orchestrator coordinates sequence.

Domain services and behavior models own business decisions.

---

# 113. Migration Risk — Common Package Growth

Risk:

```text
common/
```

becomes the new `util/` dump.

Mitigation:

Every addition requires a clear cross-cutting justification.

---

# 114. Migration Risk — Generic Relationship Layer

Risk:

A generic relationship framework becomes more complex than the relationships themselves.

Mitigation:

Use explicit domain services.

---

# 115. Migration Risk — Performance Premature Optimization

Risk:

Code becomes difficult to understand before the real bottlenecks are known.

Mitigation:

Profile first.

---

# 116. Migration Risk — Realism Before Architecture

Risk:

Sophisticated probability logic becomes embedded in existing generators.

Mitigation:

Establish behavior boundaries first.

---

# 117. Migration Risk — Documentation Drift

Risk:

Architecture documents describe a system that no longer exists.

Mitigation:

Update documentation whenever a significant decision changes.

---

# 118. Final Runtime Architecture

The mature runtime should conceptually look like:

```text
Main
 │
 ▼
Application Bootstrap
 │
 ├── Config Loader
 ├── Reference Data Loader
 ├── Effective Configuration
 ├── Scenario Factory
 ├── Randomness Factory
 ├── Temporal Context
 ├── Behavior Model Factories
 ├── Domain Generators
 ├── Relationship Services
 ├── Validators
 ├── Statistics
 ├── Output Writer
 ├── Manifest Writer
 └── Observability
 │
 ▼
Generation Orchestrator
 │
 ▼
Generated Dataset
 │
 ├── Validation
 ├── Statistics
 ├── Manifest
 └── Publication
```

---

# 119. Mature Customer Flow

```text
CustomerRequest
      ↓
CustomerBehaviorModel
      ↓
CustomerBehaviorProfile
      ↓
CustomerGenerator
      ↓
Customer
      │
      ├────────→ Address
      │
      ├────────→ Session
      │               ↓
      │              Event
      │
      └────────→ Order
                    ↓
                 OrderItem
                    ↓
               Product
                    │
              ┌─────┴─────┐
              ↓           ↓
           Payment      Shipment
                            ↓
                          Return
```

This is a conceptual business flow rather than a literal object ownership hierarchy.

---

# 120. Mature Generation Flow

The application should eventually read conceptually as:

```scala
val effectiveConfig =
  configurationResolver.resolve(request)

val references =
  referenceDataLoader.load(effectiveConfig)

referenceValidator.validate(references)

val plan =
  generationPlanner.create(effectiveConfig, references)

val runtime =
  runtimeFactory.create(
    effectiveConfig,
    references,
    plan
  )

val dataset =
  generationOrchestrator.generate(runtime)

validationService.validate(dataset)

statisticsService.calculate(dataset)

outputService.publish(dataset)
```

Exact APIs will be determined during implementation.

The intended property is the readable business workflow.

---

# 121. Final Architecture Principles

The implementation should consistently preserve:

```text
1. Domain ownership.
2. Explicit behavior.
3. Composition.
4. Visible dependencies.
5. Deterministic randomness.
6. Explicit simulation time.
7. Immutable reference data.
8. Focused relationship services.
9. Layered validation.
10. Controlled scenarios.
11. Measurable skew.
12. Dataset identity.
13. Storage independence.
14. Spark independence.
15. Measurement-driven performance.
16. Incremental refactoring.
17. Readable code.
18. Documentation/code alignment.
```

---

# 122. What We Will Not Build

To prevent scope drift, the first mature implementation will not attempt to build:

```text
a generic entity-generation framework
a universal relationship engine
a full dependency-injection framework
a generic workflow engine
a generic rules engine for every business rule
a Spark-native generator
a distributed cluster scheduler
a perfect replica of a real company's private data
```

These would increase complexity without being required for the core objective.

---

# 123. What We Will Build

The project will build:

```text
a domain-oriented e-commerce simulation
+
behavior-driven generation
+
deterministic randomness
+
temporal coherence
+
configurable distributions
+
realistic relationships
+
controlled skew
+
controlled data quality
+
validation/statistics
+
reproducible output
+
benchmark-oriented dataset families
```

---

# 124. Implementation Priority

The priority hierarchy is:

```text
P0 — Correctness
P1 — Architecture
P2 — Realism
P3 — Reproducibility
P4 — Configurability
P5 — Scale
P6 — Scenarios
P7 — Performance optimization
```

These priorities can overlap, but correctness cannot be traded away for realism or speed.

---

# 125. First Implementation Task

After this document, implementation begins.

The first task is **not** to rewrite CustomerGenerator.

The first task is to establish the target common architecture safely.

The first concrete implementation sequence should begin with:

```text
common/distribution
common/random
common/time
```

while preserving the existing behavior.

Then migrate reference domains.

---

# 126. First File Discipline

Only one source file should be changed/moved at a time unless two files are inseparable for compilation.

For each file:

```text
inspect
→ decide target
→ modify
→ compile
→ test
→ review
```

This is deliberate.

---

# 127. Implementation Stop Conditions

Stop and reassess if a migration causes:

```text
test failures that cannot be localized
unexpected business behavior changes
circular dependencies
large generic abstractions
significant API confusion
```

Do not continue moving files merely to maintain a migration checklist.

---

# 128. Architecture Evolution

The architecture is not frozen forever.

Future evidence may justify changes.

A change should be driven by:

```text
business requirement
measured performance
testing evidence
maintainability evidence
new workload requirement
```

not by pattern fashion.

---

# 129. Final Readability Test

A developer should be able to read:

```text
CustomerGenerator
```

and understand:

```text
what a customer is
what behavior influences the customer
what dependencies are required
what it generates
```

They should then be able to move to:

```text
OrderGenerator
```

and understand the relationship to customer behavior.

The same should hold for every domain.

---

# 130. Final Realism Test

A generated dataset should eventually answer "yes" to questions such as:

```text
Do customers behave differently?
Do active customers generate more activity?
Do customers purchase at different frequencies?
Do products have long-tail demand?
Do customer preferences affect products?
Do basket sizes vary realistically?
Does spending behavior affect order value?
Are sessions temporally plausible?
Are events sequentially coherent?
Are returns behaviorally correlated?
Does seasonality affect activity?
Can skew be intentionally controlled?
Can dirty data be intentionally controlled?
```

These are realism goals, not all current capabilities.

---

# 131. Final Reproducibility Test

A mature run should be identifiable through:

```text
generator version
+
seed
+
effective configuration
+
reference-data fingerprint
+
scenario
+
profile
```

and should produce the documented reproducibility guarantee.

---

# 132. Final Benchmark Test

A mature dataset should provide enough metadata to understand:

```text
size
shape
cardinality
skew
distribution
quality
relationships
generation configuration
```

so that a Spark benchmark result can identify exactly what dataset was used.

---

# 133. Final Architecture Checklist

```text
[ ] Domain packages established.
[ ] Cross-cutting packages controlled.
[ ] Customer behavior boundary established.
[ ] Geography/reference data isolated.
[ ] Product/catalog ownership established.
[ ] Transaction domains isolated.
[ ] Activity domains isolated.
[ ] Relationship services focused.
[ ] Randomness explicit.
[ ] Time explicit.
[ ] Configuration typed.
[ ] Profiles separated from scenarios.
[ ] Validation layered.
[ ] Quality scenarios explicit.
[ ] Skew scenarios measurable.
[ ] Statistics separated.
[ ] Output decoupled.
[ ] Manifest defined.
[ ] Failure semantics defined.
[ ] Observability defined.
[ ] Tests preserved.
[ ] Reproducibility preserved.
[ ] Performance measured.
[ ] Spark remains downstream.
```

---

# 134. Documentation Completion

The architecture documentation set is now complete.

The sequence has covered:

```text
00 Project / Business Overview
01 Architecture / Design Principles
02 Customer
03 Progress / Development Control
04 Address
05 Geography
06 Category
07 Brand
08 Product
09 Order
10 OrderItem
11 Payment
12 Shipment
13 Return
14 Session
15 Event
16 Relationship Architecture
17 Randomness / Distribution / Reproducibility
18 Configuration / Profiles / Scenarios
19 Validation / Data Quality
20 Skew / Quality Scenarios / Workloads
21 Output / Storage / Manifest / Observability
22 Testing Architecture
23 Error Handling / Resilience
24 Time / Lifecycle / Temporal Modeling
25 Performance / Scalability
26 Reference Data / Catalog
27 Generation Pipeline / Application Orchestration
28 Code Organization / Naming / Readability
29 Architecture Decision Records
30 Final Architecture Blueprint / Implementation Roadmap
```

This completes the planned architecture documentation phase.

---

# 135. Transition to Implementation

From this point onward, the project should stop expanding the documentation set unless implementation reveals a genuinely new architectural decision.

The next work is implementation.

The working sequence is:

```text
architecture
     ↓
source inspection
     ↓
one-file refactor
     ↓
compile
     ↓
tests
     ↓
E2E verification
     ↓
next file
```

Only after the architecture migration is stable should the project move aggressively into:

```text
behavioral realism
temporal realism
advanced scenarios
performance
Spark benchmark preparation
```

---

# 136. Final Project Direction

The finished ShopSphere system should not merely generate many rows.

It should generate a coherent synthetic world.

That world should have:

```text
customers with different behaviors
products with different demand
orders with different baskets
payments with realistic outcomes
shipments with realistic timing
returns with meaningful propensity
sessions with different activity
events with coherent journeys
geography with hierarchical structure
catalogs with realistic composition
temporal patterns
business skew
controlled workload skew
controlled data-quality defects
```

while remaining:

```text
configurable
deterministic
testable
observable
scalable
```

---

# 137. Final Architectural Statement

The final architecture is successful if a developer can trace:

```text
Why?
 ↓
business requirement

What?
 ↓
domain concept

How?
 ↓
behavior model

When?
 ↓
temporal model

Which?
 ↓
selector/distribution

How many?
 ↓
cardinality/profile

How related?
 ↓
domain relationship

Is it valid?
 ↓
validation

How realistic?
 ↓
statistics

How generated?
 ↓
manifest/observability

How processed?
 ↓
Spark workload
```

without encountering a maze of generic managers, hidden state, accidental dependencies, or unexplained randomness.

That is the architecture we will implement.

---

# 138. Immediate Next Step

The documentation phase is complete.

The implementation phase begins with **source inspection and the first one-file refactor**.

Before modifying code, establish the exact current file structure and dependency references.

Then proceed incrementally.

No broad rewrite.

No speculative framework.

No pattern for pattern's sake.

One file.

One responsibility.

One verified step at a time.
