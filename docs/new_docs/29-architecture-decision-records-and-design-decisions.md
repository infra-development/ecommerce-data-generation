# 29 — Architecture Decision Records & Design Decisions

## 1. Purpose

This document records the important architectural decisions that shape the ShopSphere data generator.

The purpose is not to document every implementation detail.

The purpose is to preserve **why** the architecture looks the way it does.

A future developer should be able to answer:

```text
Why are domains separated this way?
Why is Customer behavior modeled separately?
Why is randomness injected?
Why are relationships not owned by one generic manager?
Why is Spark not inside the generator?
Why are profiles and scenarios separate?
Why do we prefer composition?
```

without reconstructing the reasoning from historical code.

---

# 2. What Is an Architecture Decision Record?

An Architecture Decision Record (ADR) captures:

```text
decision
context
reasoning
consequences
```

An ADR is useful when a decision has meaningful impact on:

```text
structure
dependencies
extensibility
correctness
performance
reproducibility
testing
future development
```

---

# 3. ADR Principles

ShopSphere ADRs follow these principles:

1. Record significant decisions.
2. Explain the reasoning.
3. Record important alternatives.
4. Record consequences.
5. Prefer explicit decisions over accidental architecture.
6. Update an ADR when a decision genuinely changes.
7. Do not create an ADR for every small code change.

---

# 4. Decision Status

Each decision can conceptually have one of these statuses:

```text
Accepted
Proposed
Superseded
Deprecated
Rejected
```

The current decisions in this document are primarily:

```text
Accepted
```

unless explicitly stated otherwise.

---

# 5. ADR-001 — Domain-Oriented Architecture

## Status

Accepted

## Context

The generator contains many business entities:

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

A flat technical package structure makes it difficult to understand business ownership.

## Decision

Organize the implementation primarily around business domains.

Examples:

```text
customer/
product/
order/
shipment/
return/
session/
event/
```

Cross-cutting infrastructure remains outside the entity domains.

## Rationale

The primary question when changing business behavior is:

> Which domain owns this behavior?

A domain-oriented structure answers that question directly.

## Consequences

### Positive

- Better discoverability.
- Stronger ownership.
- Easier domain-focused testing.
- Easier future realism expansion.
- Better alignment between documentation and implementation.

### Negative

- Some cross-domain operations require explicit orchestration.
- Developers must understand domain boundaries.
- Small shared concepts may need deliberate placement.

---

# 6. ADR-002 — Cross-Cutting Infrastructure Remains Centralized

## Status

Accepted

## Context

Some capabilities apply to many domains:

```text
configuration
randomness
distributions
validation orchestration
statistics
output
observability
```

Duplicating these in every domain would create inconsistency.

## Decision

Keep genuinely cross-cutting infrastructure centralized.

Examples:

```text
config/
common/
validation/
statistics/
output/
observability/
```

## Consequences

Domain code remains focused on business behavior.

However, centralized packages must not become generic dumping grounds.

---

# 7. ADR-003 — Customer Behavior Is a First-Class Concept

## Status

Accepted

## Context

Realistic e-commerce data cannot be produced by treating every customer identically.

Customers differ in:

```text
activity
purchase frequency
spending
price sensitivity
category affinity
brand affinity
device preference
payment preference
return propensity
lifecycle state
```

These differences should influence multiple downstream entities.

## Decision

Introduce a conceptual `CustomerBehaviorProfile` as a central latent representation of customer behavior.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
 ┌──────┼────────┬──────────┬───────────┐
 ↓      ↓        ↓          ↓           ↓
Session Order   Product    Payment     Return
```

## Rationale

Without a shared behavioral source, relationships become collections of independent random choices.

With a behavioral profile, the generated world becomes internally coherent.

## Consequences

### Positive

- Correlated behavior.
- Heterogeneous customers.
- More realistic distributions.
- Better scenario modeling.
- Better statistical relationships.

### Negative

- More complex generation flow.
- Profile design becomes important.
- Behavioral calibration becomes a significant testing concern.

---

# 8. ADR-004 — Behavior Is Separate from Entity Generation

## Status

Accepted

## Context

A generator should create records, but sophisticated realism requires behavior models.

Putting all behavior directly inside generators creates large classes.

## Decision

Separate behavior modeling from record generation.

Example:

```text
OrderGenerator
    ↓
OrderFrequencyModel
```

and:

```text
ProductGenerator
    ↓
ProductPopularityModel
```

## Rationale

This follows composition and gives meaningful business behavior an explicit home.

## Consequences

Generators become smaller and more readable.

Additional abstractions must be justified by genuine variation or responsibility.

---

# 9. ADR-005 — Composition Over Inheritance

## Status

Accepted

## Context

Entity generation contains multiple independent behaviors.

Inheritance could produce large hierarchies such as:

```text
BaseGenerator
    ↓
RealisticGenerator
    ↓
ScenarioGenerator
```

## Decision

Prefer composition.

For example:

```text
OrderGenerator
 + OrderFrequencyModel
 + ProductSelector
 + TemporalModel
 + PricingModel
```

## Rationale

These behaviors are independently variable.

Composition keeps those variations explicit.

## Consequences

More objects may exist, but responsibilities remain clearer.

---

# 10. ADR-006 — Strategy Pattern for Genuine Behavioral Variation

## Status

Accepted

## Context

Different datasets may intentionally use different mathematical or business models.

Examples:

```text
uniform
Zipf
Pareto
log-normal
negative binomial
campaign-aware
```

## Decision

Use Strategy where interchangeable behavior is a real requirement.

Examples:

```text
ProductPopularityModel
OrderFrequencyModel
ReturnProbabilityModel
SessionDurationModel
```

## Consequences

### Positive

- Behavior can be substituted.
- Statistical models can evolve independently.
- Tests can target each strategy.
- Scenarios can select behavior deliberately.

### Negative

- Too many strategies can overcomplicate the code.
- Strategy should not be introduced merely because a design pattern exists.

---

# 11. ADR-007 — Factory for Configuration-Driven Construction

## Status

Accepted

## Context

Configuration may select an implementation.

Example:

```hocon
product-popularity-model = "zipf"
```

## Decision

Use factories where configuration must select an implementation.

Examples:

```text
BehaviorModelFactory
DistributionFactory
OutputWriterFactory
```

## Rationale

Configuration parsing remains near the application/configuration boundary.

The generation loop does not repeatedly interpret configuration strings.

---

# 12. ADR-008 — Constructor Injection

## Status

Accepted

## Context

Dependencies must remain visible and testable.

A global registry or service locator would hide dependencies.

## Decision

Use constructor injection by default.

Example:

```scala
final class OrderGenerator(
  frequencyModel: OrderFrequencyModel,
  productSelector: ProductSelector,
  temporalModel: TemporalModel
)
```

## Consequences

Dependencies are explicit.

Tests can supply deterministic or simplified implementations.

No dependency-injection framework is required.

---

# 13. ADR-009 — No DI Framework

## Status

Accepted

## Context

The project is a standalone Scala generator.

A dependency-injection framework would add infrastructure without currently solving a significant problem.

## Decision

Use ordinary Scala construction and constructor injection.

The application composition root creates the object graph.

## Consequences

The architecture remains lightweight.

If the system later becomes substantially more complex, this decision can be revisited.

---

# 14. ADR-010 — No Interface Explosion

## Status

Accepted

## Context

Interfaces can improve substitution, but creating an interface for every class adds unnecessary indirection.

## Decision

Create traits/interfaces only when at least one of these is meaningful:

```text
multiple implementations
testing substitution
stable architectural boundary
independent extension point
```

Otherwise use concrete classes.

## Consequences

The codebase avoids unnecessary abstraction.

---

# 15. ADR-011 — Builder Is Not the Default Construction Pattern

## Status

Accepted

## Context

Scala case classes already provide concise immutable construction.

## Decision

Use case classes for normal domain construction.

Introduce Builder only where construction is genuinely complex.

## Consequences

The code remains concise and idiomatic.

---

# 16. ADR-012 — Template Method Is Not the Default Generator Pattern

## Status

Accepted

## Context

Entity generation flows are not identical enough to justify a universal inheritance lifecycle.

## Decision

Do not impose:

```text
beforeGenerate
generate
afterGenerate
```

on every generator.

Prefer composition and explicit orchestration.

---

# 17. ADR-013 — Domain Models Remain Infrastructure Independent

## Status

Accepted

## Context

Domain models represent business data.

They should not become coupled to technical infrastructure.

## Decision

Domain models should not depend directly on:

```text
HOCON
CSV
filesystem
logging
Spark
```

## Consequences

Models remain reusable and easy to test.

---

# 18. ADR-014 — Configuration Is Resolved at the Boundary

## Status

Accepted

## Context

Scattering raw configuration access throughout generation logic makes business behavior difficult to understand and test.

## Decision

Load and validate configuration near the application boundary.

Pass typed configuration or behavior objects into domain components.

## Consequences

Domain logic does not need to know HOCON paths.

---

# 19. ADR-015 — Randomness Is an Explicit Dependency

## Status

Accepted

## Context

Synthetic generation requires randomness.

Hidden global randomness causes problems with:

```text
reproducibility
testing
parallelism
debugging
```

## Decision

Use the project's deterministic random abstraction and explicit random streams.

## Consequences

The same seed and effective inputs can reproduce a dataset.

Random streams can be isolated by semantic purpose.

---

# 20. ADR-016 — Hierarchical Random Streams

## Status

Accepted

## Context

A single global random stream couples unrelated generation decisions.

Changing one random draw can shift every subsequent result.

## Decision

Use semantically derived random streams.

Conceptually:

```text
root seed
   ↓
customer stream
   ↓
customer-123
   ├── behavior
   ├── acquisition
   ├── preferences
   └── activity
```

Similarly:

```text
order-456
   ├── timing
   ├── basket
   ├── payment
   └── shipment
```

## Consequences

Randomness becomes easier to reason about and isolate.

---

# 21. ADR-017 — Time Is an Explicit Dependency

## Status

Accepted

## Context

Generation should not depend on the machine's wall clock.

## Decision

Use a simulation/dataset clock.

Generation time and business-event time are separate concepts.

## Consequences

Runs are deterministic and temporal scenarios become testable.

---

# 22. ADR-018 — Geography Is a Hierarchical Reference Domain

## Status

Accepted

## Context

ShopSphere geography is modeled as:

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

Postal codes belong to areas.

## Decision

Represent geography as reference data with explicit hierarchy and lookup capabilities.

## Consequences

Address can reference:

```text
buildingId
unitNumber
postalCode
```

without duplicating city/state/country attributes.

---

# 23. ADR-019 — Address Does Not Duplicate Geography

## Status

Accepted

## Context

The current Address design contains:

```text
id
customerId
buildingId
unitNumber
postalCode
```

Adding:

```text
city
state
country
addressLine1
addressLine2
addressType
isPrimary
```

would alter the established business model.

## Decision

Keep geography ownership in the Geography domain.

Address references the hierarchy through the building and postal code.

## Consequences

Less duplication and stronger referential consistency.

Consumers that need expanded address display can resolve the geography hierarchy.

---

# 24. ADR-020 — Category and Brand Are Catalog Reference Concepts

## Status

Accepted

## Context

Category and Brand describe catalog structure.

They are not transaction records.

## Decision

Keep catalog reference data distinct from transactional demand.

## Consequences

The catalog can remain relatively stable while:

```text
product popularity
orders
sessions
events
```

vary independently.

---

# 25. ADR-021 — Catalog Shape and Demand Shape Are Separate

## Status

Accepted

## Context

A product can exist in the catalog without receiving equal transaction volume.

## Decision

Separate:

```text
catalog cardinality
```

from:

```text
demand distribution
```

## Consequences

The generator can produce:

```text
many products
+
long-tail popularity
+
small set of hot products
```

without requiring catalog size to encode demand.

---

# 26. ADR-022 — Product Popularity Is Behavioral

## Status

Accepted

## Context

Uniform product selection creates unrealistic transaction distributions.

## Decision

Product selection should eventually incorporate:

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

## Consequences

Product demand becomes correlated and long-tailed.

---

# 27. ADR-023 — Order Total Is Derived from Order Items

## Status

Accepted

## Context

An order's financial value should be consistent with its basket.

## Decision

Order total is derived from its OrderItems and their transaction prices/quantities.

Conceptually:

```text
Order Total
=
Σ(OrderItem quantity × transaction unit price)
```

## Consequences

Financial invariants can be validated.

Order value realism can be improved through:

```text
basket composition
quantity
pricing
discounts
customer spending behavior
```

rather than arbitrary order totals.

---

# 28. ADR-024 — Product Price and Transaction Price Are Distinct Concepts

## Status

Accepted

## Context

A catalog price does not necessarily equal the price paid.

Reasons include:

```text
promotion
discount
campaign
customer segment
time
```

## Decision

Keep reference/catalog pricing conceptually separate from transaction pricing.

## Consequences

Future pricing realism can be added without redefining Product.

---

# 29. ADR-025 — Order Frequency Is a Behavioral Model

## Status

Accepted

## Context

The current generator gives customers insufficient behavioral heterogeneity.

A realistic system should contain:

```text
frequent buyers
normal buyers
occasional buyers
inactive buyers
```

## Decision

Model order frequency separately from Order record construction.

## Consequences

Customer behavior can drive order counts.

This enables negative-binomial, mixture, or other appropriate distributions.

---

# 30. ADR-026 — Order State Is a Business Lifecycle

## Status

Accepted

## Context

Orders are not merely records with unrelated status labels.

They move through a lifecycle.

## Decision

Treat Order status as a domain state model.

Current conceptual states:

```text
PLACED
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

## Consequences

Status transitions can become temporally coherent.

Payment, Shipment, and Return behavior can depend on lifecycle state.

---

# 31. ADR-027 — OrderItem Is the Basket Boundary

## Status

Accepted

## Context

OrderItem is the connection between:

```text
Order
```

and:

```text
Product
```

It also controls:

```text
quantity
price
line total
basket composition
```

## Decision

Treat OrderItem as a first-class domain concept.

## Consequences

Basket behavior can be modeled independently from order-level lifecycle.

---

# 32. ADR-028 — Basket Size Must Become Heterogeneous

## Status

Accepted

## Context

The current baseline has:

```text
mean = 2.40
minimum = 2
maximum = 3
```

This is structurally valid but too narrow for realistic e-commerce behavior.

## Decision

Future basket-size modeling should use a broader distribution.

The exact distribution remains an implementation decision to be calibrated.

## Consequences

Order values and product demand become more realistic.

---

# 33. ADR-029 — Payment Is More Than a Method Label

## Status

Accepted

## Context

The current dataset records payment method categories.

A mature model should represent:

```text
method selection
success/failure
attempts
retry
switching
timing
refund
```

## Decision

Treat Payment as a lifecycle-aware transaction domain.

## Consequences

Payment behavior can correlate with:

```text
customer preference
order value
device
channel
failure conditions
```

---

# 34. ADR-030 — Shipment Is a Lifecycle

## Status

Accepted

## Context

Shipment is not simply an `orderId` plus a delivery record.

## Decision

Model shipment lifecycle and temporal behavior.

Conceptual states may include:

```text
CREATED
DISPATCHED
IN_TRANSIT
OUT_FOR_DELIVERY
DELIVERED
FAILED
RETURN_TO_ORIGIN
```

The exact implemented state model remains subject to the shipment design.

## Consequences

Delivery timing and return behavior become more coherent.

---

# 35. ADR-031 — Return Probability Is Not Globally Constant

## Status

Accepted

## Context

Return behavior differs by:

```text
customer
product
category
order
delivery experience
```

## Decision

Model return propensity as a conditional business behavior.

Conceptually:

```text
P(return)
=
f(customer,
  product,
  category,
  order,
  shipment,
  time)
```

## Consequences

Return data can contain meaningful correlations instead of independent noise.

---

# 36. ADR-032 — Session Activity Must Be Heterogeneous

## Status

Accepted

## Context

The baseline currently generates:

```text
3 sessions/customer
8 events/session
```

This is convenient for deterministic testing but unrealistic as a broad e-commerce population.

## Decision

Future session generation should model heterogeneous customer activity.

## Consequences

The dataset can contain:

```text
highly active customers
casual visitors
one-time visitors
dormant customers
```

---

# 37. ADR-033 — Events Represent Customer Journeys

## Status

Accepted

## Context

Independent random event labels do not create realistic behavioral data.

## Decision

Generate Events as sequences within Session journeys.

Conceptually:

```text
landing
→ product_view
→ add_to_cart
→ checkout
→ payment
→ order
```

with alternative paths such as abandonment.

## Consequences

Event data supports realistic:

```text
funnels
drop-off
conversion
sequence analysis
session windows
```

---

# 38. ADR-034 — Event Time Must Be Ordered

## Status

Accepted

## Context

Events within a session have temporal relationships.

## Decision

Event timestamps should respect sequence and session boundaries.

For example:

```text
event_n.timestamp
≤
event_n+1.timestamp
```

within a session.

## Consequences

Windowing and event-time analytics become meaningful.

---

# 39. ADR-035 — Structural Relationships and Behavioral Relationships Are Different

## Status

Accepted

## Context

Some relationships are mandatory foreign-key relationships.

Others describe probabilistic behavior.

## Decision

Distinguish:

### Structural

```text
Order → Customer
OrderItem → Order
OrderItem → Product
Payment → Order
Shipment → Order
```

### Behavioral

```text
Customer → Product affinity
Customer → Category affinity
Customer → Brand affinity
Customer → Return propensity
```

## Consequences

Structural integrity and behavioral realism can be designed and validated independently.

---

# 40. ADR-036 — No Generic Relationship Manager

## Status

Accepted

## Context

A generic relationship manager tends to become a god object.

It could eventually contain:

```text
customer relationships
order relationships
product relationships
session attribution
return logic
geography lookup
```

## Decision

Use focused domain services and explicit orchestration.

Examples:

```text
ProductSelector
ReturnEligibilityService
ShipmentSchedulingService
SessionJourneyService
```

## Consequences

Relationship ownership becomes clearer.

---

# 41. ADR-037 — Validation Is Layered

## Status

Accepted

## Context

A valid synthetic dataset requires more than successful object creation.

## Decision

Use validation layers:

```text
configuration
reference data
structural
primary key
foreign key
relationship
business rule
temporal
statistical
data-quality scenario
```

## Consequences

Failures can be localized and understood.

---

# 42. ADR-038 — Expected Dirty Data Is Different from Generator Failure

## Status

Accepted

## Context

The generator intentionally supports data-quality scenarios.

An intentionally corrupted foreign key is not necessarily a generation defect.

## Decision

Distinguish:

```text
expected scenario violation
```

from:

```text
unexpected generator defect
```

## Consequences

Dirty-data scenarios can be validated without hiding actual bugs.

---

# 43. ADR-039 — Clean Is the Default

## Status

Accepted

## Context

Controlled dirty data is useful for testing, but baseline datasets should remain trustworthy.

## Decision

The default generation scenario is clean.

Dirty scenarios are explicit.

## Consequences

Normal benchmark datasets remain structurally valid.

---

# 44. ADR-040 — Business Skew and Artificial Workload Skew Are Distinct

## Status

Accepted

## Context

Skew can represent either:

```text
realistic business concentration
```

or:

```text
deliberate stress for a distributed workload
```

## Decision

Model these as distinct scenario intents.

## Consequences

The generator can support both:

```text
realistic hot products
```

and:

```text
extreme hot-key stress
```

without confusing their meanings.

---

# 45. ADR-041 — Scenario Composition Must Be Explicit

## Status

Accepted

## Context

A dataset may combine:

```text
large scale
+
hot products
+
festival season
+
dirty foreign keys
```

Implicit precedence becomes dangerous.

## Decision

Scenario composition and precedence must be explicit.

## Consequences

The effective configuration can be understood and reproduced.

---

# 46. ADR-042 — Profiles and Scenarios Are Different

## Status

Accepted

## Context

Scale and workload behavior are separate dimensions.

## Decision

Distinguish:

### Profile

Controls dimensions such as:

```text
small
medium
large
xlarge
```

### Scenario

Controls behavior such as:

```text
baseline
hot-product
festival-season
dirty
high-activity
```

## Consequences

The same scale can be used with multiple behavioral scenarios.

---

# 47. ADR-043 — Configuration Is Typed After Loading

## Status

Accepted

## Context

Raw HOCON values are strings and primitives.

Business code should not repeatedly interpret them.

## Decision

Resolve raw configuration into typed configuration structures before generation.

## Consequences

Configuration errors fail early.

Business logic becomes independent of configuration syntax.

---

# 48. ADR-044 — Effective Configuration Is Part of Dataset Identity

## Status

Accepted

## Context

A seed alone does not identify a dataset.

Changing configuration changes generated output.

## Decision

Dataset identity should incorporate relevant effective inputs such as:

```text
seed
effective configuration
reference-data fingerprint
scenario
profile
generator version
```

## Consequences

Benchmark datasets can be reproduced and compared more reliably.

---

# 49. ADR-045 — Reference Data Is Versioned

## Status

Accepted

## Context

Geography and catalog reference data influence generated transactions.

Changing reference data can change output even when the seed remains constant.

## Decision

Reference-data identity/fingerprint should be tracked.

## Consequences

Reproducibility becomes:

```text
same generator
+
same configuration
+
same seed
+
same reference data
```

rather than seed-only reproducibility.

---

# 50. ADR-046 — Output Is a Dataset Contract

## Status

Accepted

## Context

Generated files are consumed by future analytics and Spark workloads.

Output cannot be treated as incidental files.

## Decision

The output layer owns a stable dataset contract:

```text
entity names
schemas
serialization
metadata
manifest
publication semantics
```

## Consequences

Future storage formats can change without redefining domain models.

---

# 51. ADR-047 — CSV First, Storage Independent Architecture

## Status

Accepted

## Context

CSV is currently useful for inspection and simple interoperability.

Future Spark workloads will benefit from formats such as Parquet.

## Decision

Start with CSV while keeping the output abstraction storage-independent.

## Consequences

Parquet or other formats can be added later through output implementations.

---

# 52. ADR-048 — Generator Is Spark Independent

## Status

Accepted

## Context

The generator exists to create controlled benchmark data.

Spark is the downstream workload platform.

Coupling generation directly to Spark would make the generator harder to:

```text
test
run locally
benchmark
reuse
```

## Decision

The generator remains ordinary Scala/JVM code.

Spark consumes its output.

## Consequences

The project remains useful as an independent dataset-generation system.

---

# 53. ADR-049 — Iterator/Streaming-Friendly Generation

## Status

Accepted

## Context

Large datasets may not fit comfortably in memory.

## Decision

Generation APIs should support lazy/streaming processing where practical.

## Consequences

Large-scale profiles can reduce memory pressure.

Some operations will still require materialization when dependencies demand it.

---

# 54. ADR-050 — Deterministic Parallelism Is a Design Goal

## Status

Accepted

## Context

Large datasets may require parallel generation.

Naively sharing one random stream makes output dependent on execution order.

## Decision

Future parallel generation should use deterministic partition/shard-specific random streams.

Conceptually:

```text
root seed
→ entity
→ partition
→ record
```

## Consequences

Parallelism can improve throughput without unnecessarily sacrificing reproducibility.

---

# 55. ADR-051 — Performance Optimization Is Measurement-Driven

## Status

Accepted

## Context

Synthetic generation must eventually operate at large scale.

Premature optimization can make domain logic harder to understand.

## Decision

Use:

```text
measure
→ identify bottleneck
→ optimize
→ re-measure
```

## Consequences

Performance work remains evidence-based.

---

# 56. ADR-052 — Reference Lookups Are Indexed

## Status

Accepted

## Context

Repeated hierarchy and catalog lookups can become expensive at scale.

## Decision

Reference data should expose efficient indexed lookup operations.

Examples:

```text
buildingId → building
areaId → postal codes
categoryId → category
brandId → brand
productId → product
```

## Consequences

Generation can avoid repeated linear scans.

---

# 57. ADR-053 — Deterministic Ordering Is Important

## Status

Accepted

## Context

Iteration order can affect:

```text
random selection
output order
checksums
reproducibility
```

## Decision

Where order affects generation or output identity, establish deterministic ordering explicitly.

## Consequences

Equivalent runs do not accidentally differ because of collection iteration order.

---

# 58. ADR-054 — Domain Statistics Are Separate from Generation

## Status

Accepted

## Context

Generators should not become responsible for global aggregation.

## Decision

Statistics are collected by dedicated statistics components.

## Consequences

Generation remains focused.

Statistics can evolve independently.

---

# 59. ADR-055 — Observability Is Separate from Business Logic

## Status

Accepted

## Context

Progress and performance measurements are required for large runs.

Embedding instrumentation everywhere makes business logic noisy.

## Decision

Observability is primarily an application/infrastructure concern.

## Consequences

Generation code remains readable.

---

# 60. ADR-056 — Error Handling Is Explicit

## Status

Accepted

## Context

Large generation runs can fail due to:

```text
configuration
reference data
generation
validation
output
resource exhaustion
```

## Decision

Use explicit error categories and clear failure semantics.

The application boundary determines run outcome and publication behavior.

## Consequences

Unexpected failures are distinguishable from expected scenario violations.

---

# 61. ADR-057 — Output Publication Should Be Atomic

## Status

Accepted

## Context

A failed run should not be mistaken for a completed dataset.

## Decision

Future output publication should use temporary/incomplete output followed by successful publication.

Conceptually:

```text
generate
→ validate
→ finalize
→ publish
```

## Consequences

Consumers are less likely to read partial datasets.

---

# 62. ADR-058 — Manifest Is the Dataset Completion Contract

## Status

Accepted

## Context

Consumers need to know whether a dataset is complete and what produced it.

## Decision

The manifest records:

```text
run identity
dataset identity
generator version
seed
effective configuration
reference fingerprint
requested counts
actual counts
validation result
statistics
output metadata
completion state
```

## Consequences

Datasets become self-describing benchmark artifacts.

---

# 63. ADR-059 — Current Validity Baseline Must Be Preserved

## Status

Accepted

## Context

The current project already has a functioning generation and validation baseline.

Current test baseline:

```text
229 tests passing
```

The baseline generation run also passes:

```text
Structural validation
Primary-key validation
Foreign-key validation
Business-rule validation
```

## Decision

Architectural refactoring must preserve this baseline unless a documented business change intentionally modifies behavior.

## Consequences

Refactoring remains controlled.

---

# 64. ADR-060 — Refactoring Is Separate from Realism Expansion

## Status

Accepted

## Context

The project still needs significant realism improvements.

Mixing architecture migration with every realism change makes failures difficult to diagnose.

## Decision

Prefer separating:

```text
architecture refactoring
```

from:

```text
new realism behavior
```

when practical.

## Consequences

Each change has a clearer purpose and smaller regression surface.

---

# 65. ADR-061 — One File at a Time During Migration

## Status

Accepted

## Context

Large simultaneous package moves and rewrites create difficult failures.

## Decision

Use controlled incremental migration:

```text
one file
→ compile
→ relevant tests
→ full tests
→ next file
```

for the implementation migration.

## Consequences

Progress is slower per individual change but substantially easier to diagnose and review.

---

# 66. ADR-062 — Documentation Is Part of Architecture

## Status

Accepted

## Context

The project contains substantial domain and architecture reasoning.

If that reasoning exists only in conversation or developer memory, it will be lost.

## Decision

Architecture and domain decisions are documented in version-controlled Markdown documents.

## Consequences

Future implementation work can be guided by explicit design intent.

---

# 67. ADR-063 — Documentation Follows Domain and Cross-Cutting Responsibilities

## Status

Accepted

## Context

A single enormous architecture document becomes difficult to navigate.

## Decision

Documentation is separated into:

```text
project/domain documents
cross-cutting architecture documents
progress/control document
ADRs
final blueprint
```

## Consequences

A developer can read only the relevant domain while retaining access to system-level decisions.

---

# 68. ADR-064 — Current Simplifications Are Explicit

## Status

Accepted

## Context

The current generator is a foundation, not the final realism engine.

Examples include:

```text
fixed session counts
fixed event counts
narrow basket sizes
simplified behavior
limited temporal realism
```

## Decision

Each domain document distinguishes:

```text
current behavior
current simplifications
future realism
```

## Consequences

Simplification is not mistaken for accidental design.

---

# 69. ADR-065 — Realism Is Statistical and Relational

## Status

Accepted

## Context

A dataset can pass structural validation while still looking unrealistic.

## Decision

Realism must be evaluated through:

```text
distributions
correlations
relationships
temporal behavior
skew
business rules
```

not only row counts and foreign keys.

## Consequences

Future validation includes statistical and behavioral checks.

---

# 70. ADR-066 — Long-Tail Behavior Is a Core Realism Requirement

## Status

Accepted

## Context

Real e-commerce populations are heterogeneous.

Uniform distributions are useful for simple testing but are insufficient as the main realism model.

## Decision

Use appropriate long-tail distributions where business behavior suggests them.

Potential models include:

```text
Zipf
Pareto
log-normal
negative binomial
mixture distributions
```

The specific model is selected per behavior rather than universally.

---

# 71. ADR-067 — Distribution Choice Is Domain-Specific

## Status

Accepted

## Context

Different phenomena have different statistical characteristics.

## Decision

Do not force one distribution onto every domain.

Examples:

```text
product popularity → long-tail
order frequency → count distribution
spending → positive heavy-tailed distribution
session duration → bounded continuous distribution
return probability → conditional probability model
```

## Consequences

The generated data can be statistically more plausible.

---

# 72. ADR-068 — Customer Behavior Drives Downstream Correlation

## Status

Accepted

## Context

Independent generators produce independent data.

Realistic e-commerce data contains dependencies.

## Decision

Customer behavior should propagate into multiple domains.

For example:

```text
high activity
→ more sessions
→ more events
→ more opportunities to purchase

high spending
→ higher order values

high return propensity
→ more returns
```

## Consequences

The dataset contains meaningful cross-entity correlations.

---

# 73. ADR-069 — Product Demand Depends on Customer and Context

## Status

Accepted

## Context

Product popularity alone does not capture personalized demand.

## Decision

Product selection may depend on:

```text
global popularity
customer affinity
category affinity
brand affinity
price sensitivity
campaign
season
availability
```

## Consequences

Product demand becomes richer than a single global distribution.

---

# 74. ADR-070 — Temporal Behavior Is a Cross-Cutting Domain Capability

## Status

Accepted

## Context

Time affects nearly every business entity.

## Decision

Temporal modeling is centralized conceptually but consumed by domain-specific behavior models.

Examples:

```text
Customer acquisition
Session activity
Event sequence
Order timing
Payment timing
Shipment delay
Return window
```

## Consequences

Time remains consistent across entities without putting all temporal logic into one god class.

---

# 75. ADR-071 — Lifecycle Is Explicit Where Business State Exists

## Status

Accepted

## Context

Several domains naturally have lifecycles.

Examples:

```text
Customer
Order
Payment
Shipment
Return
Session
```

## Decision

Model lifecycle semantics explicitly where they affect generation or validation.

## Consequences

Temporal and state-based behavior becomes easier to reason about.

---

# 76. ADR-072 — Event Generation Uses State/Journey Semantics

## Status

Accepted

## Context

Randomly selecting event types produces invalid or unrealistic sequences.

## Decision

Event generation should eventually use journey/state semantics.

Possible implementation techniques include:

```text
state machine
strategy
composition
```

depending on the final design.

## Consequences

Event sequences can represent:

```text
browse
research
cart
checkout
purchase
abandonment
```

more realistically.

---

# 77. ADR-073 — Scenario Logic Must Not Corrupt Domain Ownership

## Status

Accepted

## Context

Scenarios affect many domains.

Embedding scenario-specific branches in every generator creates coupling.

## Decision

Scenario behavior should be represented through explicit scenario/context inputs and composable strategies where practical.

## Consequences

The baseline domain model remains understandable.

---

# 78. ADR-074 — Data Quality Is Controlled Corruption

## Status

Accepted

## Context

Dirty datasets are useful for Spark data-quality workloads.

Random accidental corruption is not controllable enough.

## Decision

Data-quality scenarios intentionally inject defined defects at configured rates.

Examples:

```text
missing foreign key
duplicate key
invalid value
invalid relationship
temporal inconsistency
```

## Consequences

Expected defects can be measured and validated.

---

# 79. ADR-075 — Quality Scenarios Are Explicit

## Status

Accepted

## Context

A clean dataset and a dirty dataset serve different purposes.

## Decision

Data-quality behavior is activated by explicit scenario/configuration.

## Consequences

The default dataset remains clean.

---

# 80. ADR-076 — Skew Is Measurable

## Status

Accepted

## Context

Calling a dataset "skewed" without measuring concentration is insufficient.

## Decision

Skew scenarios should expose measurable metrics.

Examples:

```text
top-1 share
top-10 share
top-1% share
Gini-like concentration
frequency ratio
partition-size distribution
```

The exact metrics may vary by workload.

## Consequences

Skew scenarios become reproducible benchmark configurations rather than subjective labels.

---

# 81. ADR-077 — Workload Design Is Part of Dataset Design

## Status

Accepted

## Context

The generator ultimately supports a Spark Performance Laboratory.

A dataset should therefore support meaningful workloads.

## Decision

Design dataset families intentionally for:

```text
joins
aggregations
groupBy
window functions
Top-K
filters
data-quality analysis
skew stress
temporal analysis
```

## Consequences

Generated data becomes a benchmark asset rather than merely synthetic sample data.

---

# 82. ADR-078 — Generator and Spark Workload Are Separate Layers

## Status

Accepted

## Context

The same dataset may support many Spark experiments.

## Decision

Keep:

```text
data generation
```

separate from:

```text
Spark workload implementation
```

## Consequences

One dataset can be reused across multiple labs and execution strategies.

---

# 83. ADR-079 — Dataset Families Are More Useful Than One "Perfect" Dataset

## Status

Accepted

## Context

Different experiments require different data characteristics.

## Decision

Eventually provide dataset families such as:

```text
baseline
large
xlarge
high-skew
high-cardinality
dirty
temporal
high-activity
```

## Consequences

Benchmark experiments can select a dataset designed for the workload.

---

# 84. ADR-080 — Dataset Identity Must Be Stable

## Status

Accepted

## Context

Performance comparisons require knowing exactly which dataset was used.

## Decision

A benchmark dataset identity should be derived from meaningful generation inputs.

## Consequences

Performance results can reference a stable dataset definition.

---

# 85. ADR-081 — Schema Stability Matters

## Status

Accepted

## Context

Downstream Spark jobs depend on predictable schemas.

## Decision

Entity schemas should change deliberately and be documented.

## Consequences

Dataset evolution is manageable.

---

# 86. ADR-082 — Output Format Must Not Define Domain Models

## Status

Accepted

## Context

CSV is only one representation.

## Decision

Domain models are independent of serialization format.

## Consequences

Future Parquet or other writers can consume the same domain records.

---

# 87. ADR-083 — Manifest and Validation Are Different

## Status

Accepted

## Context

Validation answers:

```text
Is the dataset correct?
```

The manifest answers:

```text
What dataset is this and how was it produced?
```

## Decision

Keep these responsibilities distinct while allowing the manifest to record validation results.

## Consequences

Both concerns remain independently evolvable.

---

# 88. ADR-084 — Current Test Baseline Is an Architectural Constraint

## Status

Accepted

## Context

A green test suite provides confidence during migration.

## Decision

The current:

```text
229 tests passing
```

baseline is preserved unless a documented behavior change intentionally alters tests.

## Consequences

Refactoring can proceed incrementally.

---

# 89. ADR-085 — Readability Has Equal Architectural Importance

## Status

Accepted

## Context

The project will eventually contain sophisticated behavior models.

If the architecture is technically correct but difficult to read, future development becomes expensive.

## Decision

Readability is an explicit architecture quality criterion.

Code should communicate:

```text
business intent
responsibility
dependencies
```

clearly.

## Consequences

We prefer:

```text
clear composition
named business decisions
focused classes
```

over clever generic abstractions.

---

# 90. ADR-086 — Avoid Generic Managers and Helpers

## Status

Accepted

## Context

Names such as:

```text
Manager
Helper
Processor
Handler
```

often hide responsibility.

## Decision

Use specific domain names where possible.

Examples:

```text
ReturnEligibilityService
ProductSelector
ShipmentSchedulingModel
```

## Consequences

Code becomes easier to navigate and review.

---

# 91. ADR-087 — Avoid Premature Generic Frameworks

## Status

Accepted

## Context

It is tempting to build a generic framework for all entities.

## Decision

ShopSphere should remain domain-specific.

Do not generalize behavior merely because multiple entities currently look similar.

## Consequences

The business model remains visible.

---

# 92. ADR-088 — Domain-Specific Similarity Does Not Require Shared Abstraction

## Status

Accepted

## Context

Two generators may contain similar-looking code while having different business semantics.

## Decision

Do not abstract duplicated code until the shared concept is genuinely understood.

## Consequences

Some local duplication may be preferable to a misleading abstraction.

---

# 93. ADR-089 — Business Meaning Before Design Pattern

## Status

Accepted

## Context

Patterns are tools, not goals.

## Decision

The design sequence is:

```text
business problem
→ responsibility
→ variation
→ dependency boundary
→ pattern if useful
```

not:

```text
pattern
→ find somewhere to use it
```

## Consequences

The architecture remains pragmatic.

---

# 94. ADR-090 — Architecture Must Support Future Realism

## Status

Accepted

## Context

The current dataset is structurally sound but not yet fully realistic.

## Decision

Architecture must make the following additions possible without major rewrites:

```text
CustomerBehaviorProfile
heterogeneous activity
long-tail demand
basket distributions
pricing behavior
temporal modeling
customer/product correlations
return propensity
campaigns
seasonality
skew
quality scenarios
```

## Consequences

The current implementation should be evaluated partly by whether it provides clean extension points.

---

# 95. ADR-091 — Realism Is Added Incrementally

## Status

Accepted

## Context

Trying to implement every realism dimension simultaneously increases complexity and makes calibration difficult.

## Decision

Increase realism in controlled layers.

Suggested order:

```text
1. customer behavior
2. heterogeneous order frequency
3. basket-size distribution
4. product popularity
5. customer/product affinity
6. monetary realism
7. temporal realism
8. session/event journey realism
9. return behavior
10. advanced scenarios
```

## Consequences

Each layer can be validated before the next is added.

---

# 96. ADR-092 — Statistical Validation Is Required for Mature Realism

## Status

Accepted

## Context

Structural correctness does not prove distributional correctness.

## Decision

Mature realism requires statistical validation.

Examples:

```text
distribution shape
mean/median
quantiles
tail concentration
correlation
conversion rate
return rate
items/order
sessions/customer
events/session
```

## Consequences

Realism becomes measurable.

---

# 97. ADR-093 — Exact Counts and Statistical Counts Are Different

## Status

Accepted

## Context

Some quantities should be exact.

Others naturally arise from stochastic behavior.

## Decision

Distinguish:

```text
requested cardinality
```

from:

```text
behaviorally generated observed count
```

## Consequences

A profile can request a population size while behavioral distributions determine downstream variation.

---

# 98. ADR-094 — Configuration Must Have Safety Limits

## Status

Accepted

## Context

A configuration can request enormous output.

Unbounded generation can exhaust:

```text
memory
disk
CPU
```

## Decision

Configuration validation should enforce explicit safety limits where appropriate.

## Consequences

Operational failures become more predictable.

---

# 99. ADR-095 — Reproducibility Includes Effective Inputs

## Status

Accepted

## Context

A seed is not enough if:

```text
configuration changes
reference data changes
generator version changes
```

## Decision

Reproducibility is defined in terms of effective generation inputs.

Conceptually:

```text
same seed
+
same effective configuration
+
same reference data
+
same generator version
```

should reproduce the same intended dataset semantics.

## Consequences

Manifest/fingerprint design becomes important.

---

# 100. ADR-096 — Reproducibility and Statistical Equivalence Are Different

## Status

Accepted

## Context

Parallel or optimized implementations may preserve statistical behavior without preserving byte-identical output.

## Decision

Distinguish:

```text
bitwise/record-level reproducibility
```

from:

```text
statistical reproducibility
```

and document which guarantee applies to each execution mode.

## Consequences

Parallel optimization does not have to be incorrectly described as identical output.

---

# 101. ADR-097 — Domain Boundaries Are Not Strict Aggregate Boundaries Yet

## Status

Accepted

## Context

The generator is a simulation system rather than a transactional application.

Applying strict DDD aggregate rules everywhere could create unnecessary complexity.

## Decision

Use domain ownership and explicit invariants without forcing every entity into a formal aggregate hierarchy.

## Consequences

The architecture remains pragmatic.

---

# 102. ADR-098 — Application Orchestration Owns Generation Order

## Status

Accepted

## Context

Entity generation has dependencies.

Example:

```text
Product
→ OrderItem
→ Order
```

or more precisely:

```text
Customer + Product
→ Order
→ OrderItem
→ Payment/Shipment
```

## Decision

Application orchestration owns the generation dependency graph.

Domain generators remain focused on their own responsibilities.

## Consequences

Generation order remains visible and testable.

---

# 103. ADR-099 — Domain Services Own Cross-Entity Business Operations

## Status

Accepted

## Context

Some operations span multiple entities.

Examples:

```text
return eligibility
shipment scheduling
session attribution
product selection
```

## Decision

Use focused domain services for these operations rather than putting everything into the application orchestrator.

## Consequences

The orchestrator remains an application workflow rather than a business god object.

---

# 104. ADR-100 — Application Orchestrator Does Not Own Domain Rules

## Status

Accepted

## Context

A large orchestrator can gradually accumulate business decisions.

## Decision

The orchestrator coordinates:

```text
when
```

not:

```text
all business rules
```

## Consequences

Business behavior remains testable at domain level.

---

# 105. ADR-101 — Application Composition Root Owns Object Construction

## Status

Accepted

## Context

Dependencies need a clear construction location.

## Decision

The application bootstrap/composition root constructs:

```text
configuration
reference data
behavior models
generators
validators
writers
statistics
observability
```

## Consequences

Runtime dependencies remain visible in one place.

---

# 106. ADR-102 — Generation Context Is Typed

## Status

Accepted

## Context

Generators need contextual information such as:

```text
randomness
time
scenario
reference data
```

Passing unrelated values individually can become cumbersome.

## Decision

Use meaningful context objects where they represent real concepts.

Avoid one giant global context containing every application service.

## Consequences

Context remains useful without becoming a service locator.

---

# 107. ADR-103 — Reference Data Is Immutable During a Run

## Status

Accepted

## Context

Generation depends on stable reference data.

Mutation during generation would threaten reproducibility and referential integrity.

## Decision

Reference data is treated as immutable for the duration of a generation run.

## Consequences

Concurrent generation becomes safer.

---

# 108. ADR-104 — Reference Data Validation Happens Before Transaction Generation

## Status

Accepted

## Context

Broken geography or catalog references can invalidate large generated datasets.

## Decision

Validate reference data during preflight.

## Consequences

Failures happen early rather than after expensive generation.

---

# 109. ADR-105 — Validation Can Be Fail-Fast or Aggregate by Phase

## Status

Accepted

## Context

Some errors should stop generation immediately.

Others are more useful when reported together.

## Decision

Use phase-appropriate validation semantics.

Examples:

```text
configuration → fail fast
reference data → fail fast
dataset validation → aggregate useful violations
```

## Consequences

Error reporting remains practical.

---

# 110. ADR-106 — Dirty Scenario Violations Are Measured

## Status

Accepted

## Context

A dirty dataset is only useful if the intended defect rate is known.

## Decision

Quality scenarios should record expected and observed corruption statistics.

## Consequences

Data-quality experiments become reproducible and measurable.

---

# 111. ADR-107 — Output Publication Requires Successful Completion

## Status

Accepted

## Context

Partial output can be mistaken for a valid benchmark dataset.

## Decision

A run is considered published only after:

```text
generation
+
validation
+
manifest finalization
```

successfully complete.

## Consequences

Consumers have a clear completion contract.

---

# 112. ADR-108 — Performance Metrics Are Part of the Run Record

## Status

Accepted

## Context

The generator itself will eventually be benchmarked.

## Decision

Record metrics such as:

```text
phase duration
records generated
throughput
memory observations where available
output size
```

## Consequences

Generator performance regressions can be detected.

---

# 113. ADR-109 — Benchmark Dataset and Generator Benchmark Are Separate

## Status

Accepted

## Context

Two different questions exist:

```text
How realistic/useful is the generated dataset?
```

and:

```text
How quickly can the generator produce it?
```

## Decision

Track dataset characteristics separately from generator performance.

## Consequences

A faster generator is not automatically considered a better generator if it damages data quality or realism.

---

# 114. ADR-110 — Spark Workloads Should Exploit Dataset Properties

## Status

Accepted

## Context

The generator is intended to feed a Spark Performance Laboratory.

## Decision

Dataset design should intentionally support workload dimensions such as:

```text
join cardinality
join skew
aggregation cardinality
window partition size
filter selectivity
Top-K concentration
data-quality violation density
```

## Consequences

The generator and future Spark labs form a coherent benchmark ecosystem.

---

# 115. ADR-111 — Do Not Optimize for One Spark Query

## Status

Accepted

## Context

A dataset optimized for one query can become unrepresentative for others.

## Decision

Build reusable dataset families with documented properties rather than one dataset tailored to one benchmark.

## Consequences

Experiments remain comparable and reusable.

---

# 116. ADR-112 — Code Readability Is a Review Gate

## Status

Accepted

## Context

The architecture will become increasingly sophisticated.

## Decision

A change is not complete merely because tests pass.

It should also satisfy:

```text
clear naming
clear ownership
visible dependencies
reasonable method size
business-readable flow
```

## Consequences

Maintainability is treated as part of correctness.

---

# 117. ADR-113 — The Codebase Uses Scala 2.13

## Status

Accepted

## Context

The current project is explicitly based on Scala 2.13.

## Decision

Use:

```text
Scala 2.13.18
```

and do not introduce Scala 3-specific syntax or dependencies.

## Consequences

Implementation remains aligned with the established build.

---

# 118. ADR-114 — Java 17 Is the Current Runtime Baseline

## Status

Accepted

## Context

The current development environment uses Java 17.

## Decision

Maintain compatibility with the Java 17 runtime baseline.

---

# 119. ADR-115 — Documentation and Code Should Evolve Together

## Status

Accepted

## Context

Architecture decisions become stale if implementation changes without updating design documentation.

## Decision

When a meaningful architectural decision changes:

```text
ADR
+
architecture/domain document
+
code
+
tests
```

should be reconciled.

## Consequences

Documentation remains trustworthy.

---

# 120. ADR-116 — Superseding Decisions Are Preserved

## Status

Accepted

## Context

Historical decisions explain why the architecture changed.

Deleting old decisions loses useful context.

## Decision

When an ADR is superseded, preserve the old decision and explicitly identify the replacement.

## Consequences

Architectural history remains understandable.

---

# 121. ADR-117 — No Silent Architecture Changes

## Status

Accepted

## Context

Large structural changes can alter assumptions across the project.

## Decision

Significant changes to:

```text
domain ownership
dependency direction
randomness
reproducibility
schema
generation semantics
```

should be explicitly documented.

## Consequences

The project avoids architectural drift.

---

# 122. ADR-118 — Current Address Model Is Preserved During Migration

## Status

Accepted

## Context

The Address model was intentionally simplified around geography references.

Current fields:

```text
id
customerId
buildingId
unitNumber
postalCode
```

## Decision

Refactoring must preserve this model unless a new business decision explicitly changes it.

## Consequences

Package migration does not accidentally reintroduce obsolete address fields.

---

# 123. ADR-119 — Obsolete Configuration Is Removed Deliberately

## Status

Accepted

## Context

The old Address configuration contained fields that no longer correspond to the current model.

## Decision

Remove obsolete configuration only after references are identified and tests are green.

## Consequences

Configuration migration remains controlled rather than being mixed into unrelated refactoring.

---

# 124. ADR-120 — Architecture Migration Follows Dependency Order

## Status

Accepted

## Context

Moving domains arbitrarily can create temporary circular or broken dependencies.

## Decision

Migrate according to dependency structure.

Current intended sequence:

```text
common/random/distribution
→ geography
→ category
→ brand
→ product
→ customer
→ address
→ order
→ orderitem
→ payment
→ shipment
→ return
→ session
→ event
→ relationship cleanup
→ validation/statistics/output
→ application orchestration
```

## Consequences

Migration risk is reduced.

---

# 125. ADR-121 — Do Not Change Multiple Dimensions Without Need

## Status

Accepted

## Context

A change can simultaneously alter:

```text
package structure
business behavior
randomness
performance
output
```

making diagnosis difficult.

## Decision

Prefer one coherent dimension of change at a time.

## Consequences

Tests provide stronger localization of regressions.

---

# 126. ADR-122 — The Existing Working Baseline Is Valuable

## Status

Accepted

## Context

The project currently generates:

```text
customers
addresses
products
orders
order items
payments
shipments
returns
sessions
events
```

and validates the output successfully.

## Decision

Treat this working state as a protected baseline while architecture is improved.

## Consequences

New sophistication is built on known correctness rather than on an unstable foundation.

---

# 127. ADR-123 — Correctness Before Realism

## Status

Accepted

## Context

Realistic-looking data with broken relationships is not useful.

## Decision

Development priority is:

```text
correctness
→ realism
→ relationships
→ configurability
→ reproducibility
→ scale
→ scenarios
→ performance
```

This is a guiding priority rather than an absolute sequence for every change.

## Consequences

Structural failures are not hidden by realism features.

---

# 128. ADR-124 — Realism Before Extreme Scale

## Status

Accepted

## Context

Scaling an unrealistic model only produces more unrealistic data.

## Decision

Establish credible distributions and relationships before optimizing for the largest volumes.

## Consequences

Performance work targets the intended model rather than preserving accidental behavior.

---

# 129. ADR-125 — One "Perfect" Distribution Is Not Required

## Status

Accepted

## Context

Different customer and transaction phenomena have different shapes.

## Decision

Use distributions appropriate to the modeled phenomenon.

No universal distribution is prescribed.

## Consequences

Statistical calibration becomes part of domain design.

---

# 130. ADR-126 — Statistical Calibration Is Iterative

## Status

Accepted

## Context

Synthetic distributions cannot always be selected correctly from theory alone.

## Decision

Use an iterative process:

```text
hypothesis
→ generate
→ measure
→ compare
→ adjust
→ regenerate
```

## Consequences

Realism is treated as an empirical engineering problem.

---

# 131. ADR-127 — Correlation Is a First-Class Realism Target

## Status

Accepted

## Context

Independent marginal distributions can still produce unrealistic joint behavior.

## Decision

Validate important cross-variable relationships.

Examples:

```text
customer activity ↔ sessions
sessions ↔ events
activity ↔ orders
spending ↔ order value
return propensity ↔ returns
category affinity ↔ purchased category
```

## Consequences

The generator is evaluated as a coherent system rather than a collection of isolated distributions.

---

# 132. ADR-128 — Avoid Artificial Correlation Everywhere

## Status

Accepted

## Context

Adding correlations merely to make data "interesting" can make it unrealistic.

## Decision

Correlations should represent plausible business mechanisms.

## Consequences

Behavioral realism remains grounded in domain reasoning.

---

# 133. ADR-129 — Business Rules Must Be Explainable

## Status

Accepted

## Context

Complex probabilistic rules can become impossible to maintain.

## Decision

Important rules should have a clear business explanation.

For example:

```text
Why is this customer more likely to return?
Why does this product receive more demand?
Why does this shipment take longer?
```

## Consequences

Model calibration and debugging become easier.

---

# 134. ADR-130 — Randomness Does Not Replace Business Modeling

## Status

Accepted

## Context

A large number of random draws can create superficially varied data.

Variation alone is not realism.

## Decision

Randomness is used to sample from business models.

It is not used as a substitute for those models.

## Consequences

The architecture distinguishes:

```text
random mechanism
```

from:

```text
business behavior
```

---

# 135. ADR-131 — Data Quality Scenarios Must Not Hide Generator Bugs

## Status

Accepted

## Context

Intentional corruption creates invalid records by design.

An overly permissive validator could accidentally classify unrelated defects as scenario behavior.

## Decision

Every dirty scenario defines expected violation classes and measurable rates.

Unexpected violations remain failures.

## Consequences

Quality testing remains trustworthy.

---

# 136. ADR-132 — Scenario Fingerprints Matter

## Status

Accepted

## Context

Two runs with the same seed but different scenario configuration are different datasets.

## Decision

Scenario configuration contributes to dataset identity/fingerprinting.

---

# 137. ADR-133 — Configuration Precedence Must Be Explicit

## Status

Accepted

## Context

Profiles, scenarios, defaults, and explicit overrides can all affect effective behavior.

## Decision

Configuration precedence must be deterministic and documented.

Conceptually:

```text
defaults
→ profile
→ scenario
→ explicit override
```

The exact precedence is subject to the final configuration implementation, but it must never be implicit.

---

# 138. ADR-134 — Conflicting Configuration Should Fail Clearly

## Status

Accepted

## Context

Some combinations are invalid or contradictory.

## Decision

Configuration validation should reject incompatible combinations before generation.

## Consequences

Invalid runs fail cheaply.

---

# 139. ADR-135 — Safety Limits Are Part of Configuration Validation

## Status

Accepted

## Context

Large cardinalities can cause operational failure.

## Decision

Configuration validation includes resource-aware safety checks where practical.

---

# 140. ADR-136 — Domain Statistics Belong Near Their Domain

## Status

Accepted

## Context

Some statistics are inherently domain-specific.

Examples:

```text
items per order
return rate
events per session
average order value
```

## Decision

Domain-specific statistic definitions can live with the domain, while global aggregation remains centralized.

## Consequences

Statistics remain semantically meaningful.

---

# 141. ADR-137 — Global Statistics Remain Cross-Cutting

## Status

Accepted

## Context

Some statistics combine multiple entities.

Examples:

```text
overall dataset size
global validation status
dataset fingerprint
generation throughput
```

## Decision

Global statistics and orchestration remain outside individual domains.

---

# 142. ADR-138 — Validation Ownership Follows Rule Scope

## Status

Accepted

## Context

A validation rule can be:

```text
domain-local
cross-entity
global
```

## Decision

Place validation according to the scope of the invariant.

## Consequences

Validation responsibilities remain understandable.

---

# 143. ADR-139 — Performance Optimizations Must Preserve Semantics

## Status

Accepted

## Context

Optimizations can accidentally change:

```text
random sequence
ordering
sampling distribution
```

## Decision

Every performance optimization must verify that intended semantic guarantees remain intact.

---

# 144. ADR-140 — Large-Scale Generation Should Minimize Allocation Pressure

## Status

Accepted

## Context

At large scale, object allocation can become a major performance cost.

## Decision

Prefer allocation-conscious techniques where profiling shows they matter.

Potential techniques:

```text
Iterator
streaming
bounded buffers
indexed reference data
primitive-friendly counters
```

Do not sacrifice domain readability without measured benefit.

---

# 145. ADR-141 — Parallelism Is an Optimization, Not the Domain Model

## Status

Accepted

## Context

Parallel generation changes execution mechanics.

## Decision

The domain model should not depend on thread topology.

Parallel execution is an application/performance concern.

---

# 146. ADR-142 — Spark Is a Consumer, Not a Domain Dependency

## Status

Accepted

## Context

Spark may eventually process billions of generated records.

The generator should not import Spark simply because its output is intended for Spark.

## Decision

Keep Spark outside the generator domain.

---

# 147. ADR-143 — Dataset Design Includes Workload Characteristics

## Status

Accepted

## Context

Benchmark datasets need measurable workload properties.

## Decision

Dataset manifests should eventually describe characteristics such as:

```text
row counts
cardinality
skew
selectivity
distribution summaries
relationship counts
quality violation rates
```

---

# 148. ADR-144 — Output Should Be Self-Describing

## Status

Accepted

## Context

A benchmark dataset may be moved or archived.

## Decision

Metadata sufficient to understand the dataset should accompany the output.

The manifest is the primary mechanism.

---

# 149. ADR-145 — Schema and Business Semantics Must Stay Aligned

## Status

Accepted

## Context

A schema field can exist technically while its business meaning changes.

## Decision

Schema changes require checking:

```text
domain documentation
generation semantics
validation
statistics
output
downstream expectations
```

---

# 150. ADR-146 — Current Baseline Monetary Distribution Is a Known Simplification

## Status

Accepted

## Context

The current baseline has approximately:

```text
mean order value ≈ ₹226K
median order value ≈ ₹144K
```

This is too high and too broad for the intended generic e-commerce baseline.

## Decision

Do not treat the current monetary distribution as the final realism target.

Future pricing and spending models will recalibrate it.

## Consequences

The existing output remains a correctness baseline rather than a final business benchmark.

---

# 151. ADR-147 — Exact Session/Event Counts Are Known Simplifications

## Status

Accepted

## Context

The baseline has:

```text
3 sessions/customer
8 events/session
```

This is useful for initial deterministic testing but does not represent heterogeneous activity.

## Decision

Preserve the baseline during architecture migration while treating variable activity as future realism work.

---

# 152. ADR-148 — Narrow Basket Size Is a Known Simplification

## Status

Accepted

## Context

The baseline currently produces:

```text
2–3 items/order
```

with mean:

```text
2.40
```

## Decision

Preserve this behavior during structural migration but replace it later with a calibrated basket-size model.

---

# 153. ADR-149 — Documentation Ends Before Implementation Refactoring

## Status

Accepted

## Context

The project has been documenting its architecture before performing the major package migration.

## Decision

Complete the planned architecture documentation set first, then move to implementation/refactoring.

Current remaining planned documentation after this ADR document:

```text
30 — Final Architecture Blueprint & Implementation Roadmap
```

After that, implementation work begins.

---

# 154. ADR-150 — Final Blueprint Is the Implementation Reference

## Status

Accepted

## Context

Individual documents explain domains and cross-cutting concerns.

A final implementation blueprint is needed to connect them.

## Decision

The final architecture blueprint will consolidate:

```text
package structure
dependency direction
generation flow
behavior models
patterns
configuration
validation
output
testing
migration order
milestones
```

without replacing the detailed domain documents.

---

# 155. Decision Review Rules

An existing ADR should be reconsidered when:

```text
a major business requirement changes
a major scalability constraint appears
a design abstraction repeatedly causes friction
a new requirement contradicts an existing decision
a previously deferred problem becomes immediate
```

Do not revisit decisions merely because another design is fashionable.

---

# 156. When to Create a New ADR

Create a new ADR when a change materially affects:

```text
architecture
domain boundaries
data contracts
reproducibility
storage
performance model
generation semantics
testing strategy
```

---

# 157. When Not to Create an ADR

Do not create an ADR for:

```text
renaming a local variable
fixing a typo
ordinary unit-test additions
small refactors with no architectural consequence
formatting
```

---

# 158. ADR Update Procedure

When a major decision changes:

```text
1. Identify the existing ADR.
2. Explain why it no longer applies.
3. Mark it superseded/deprecated if appropriate.
4. Record the new decision.
5. Update affected architecture documents.
6. Update code and tests.
7. Verify the build.
```

---

# 159. Decision Matrix

| Area | Decision |
|---|---|
| Architecture | Domain-oriented |
| Cross-cutting | Centralized where genuinely shared |
| Customer realism | CustomerBehaviorProfile |
| Behavior | Separate from generators |
| Composition | Preferred |
| Strategy | Genuine behavioral variation |
| Factory | Configuration-driven construction |
| DI | Constructor injection |
| DI framework | Not required |
| Builder | Exceptional |
| Template Method | Not default |
| Randomness | Explicit |
| Time | Explicit |
| Geography | Hierarchical reference data |
| Address | References geography |
| Product demand | Long-tail/behavior-driven |
| Order total | Derived from items |
| Pricing | Catalog vs transaction price |
| Payment | Lifecycle-aware |
| Shipment | Lifecycle-aware |
| Return | Conditional propensity |
| Session | Heterogeneous activity |
| Event | Journey/state semantics |
| Relationships | Focused services/orchestration |
| Validation | Layered |
| Dirty data | Explicit controlled scenarios |
| Skew | Business vs workload distinction |
| Profiles | Scale/structural dimension |
| Scenarios | Behavioral/workload dimension |
| Output | Dataset contract |
| Manifest | Completion and identity metadata |
| Storage | CSV first, abstraction for future formats |
| Spark | Downstream consumer |
| Performance | Measurement-driven |
| Parallelism | Application concern |
| Documentation | Versioned architecture artifact |
| Refactoring | Incremental |
| Scala | 2.13 |
| Java | 17 |

---

# 160. Master Architecture Principle

The accumulated decisions can be reduced to one principle:

> **Build a domain-readable, behavior-driven, deterministic synthetic e-commerce world whose technical architecture is strong enough to scale into a Spark benchmarking platform without sacrificing business meaning.**

That principle implies:

```text
domain ownership
+
explicit behavior
+
controlled randomness
+
temporal coherence
+
strong relationships
+
measurable distributions
+
controlled skew
+
controlled data quality
+
reproducibility
+
scalable output
```

---

# 161. What These ADRs Protect

These decisions protect the project against several predictable failure modes.

### Failure mode 1

A giant generator accumulates every business rule.

Protected by:

```text
behavior separation
composition
domain services
```

### Failure mode 2

Every class gets an interface and factory.

Protected by:

```text
pragmatic abstraction rules
```

### Failure mode 3

Randomness becomes hidden global state.

Protected by:

```text
explicit deterministic random streams
```

### Failure mode 4

Time comes from the machine clock.

Protected by:

```text
explicit simulation time
```

### Failure mode 5

Dirty data hides real bugs.

Protected by:

```text
expected vs unexpected violations
```

### Failure mode 6

A dataset looks valid but unrealistic.

Protected by:

```text
statistical validation
correlation validation
behavioral modeling
```

### Failure mode 7

Spark concerns leak into the generator.

Protected by:

```text
generator/Spark separation
```

### Failure mode 8

Architecture migration breaks a working system.

Protected by:

```text
incremental migration
229-test baseline
```

---

# 162. Final ADR Quality Gate

Before declaring the architecture ready for implementation, verify:

```text
[ ] Major domain boundaries are explicit.
[ ] Customer behavior has a defined architectural home.
[ ] Randomness is explicit and reproducible.
[ ] Time is explicit and deterministic.
[ ] Reference data is separated from transactions.
[ ] Structural and behavioral relationships are distinguished.
[ ] Generators are not responsible for everything.
[ ] Strategy is used only for meaningful variation.
[ ] Factories are used only where construction selection matters.
[ ] Dependencies are visible.
[ ] Configuration is resolved at boundaries.
[ ] Validation has clear ownership.
[ ] Data-quality scenarios are explicit.
[ ] Skew has explicit semantics.
[ ] Output is treated as a dataset contract.
[ ] Manifest semantics are defined.
[ ] Spark remains downstream.
[ ] Performance is measurement-driven.
[ ] Documentation and code can evolve together.
[ ] The existing correctness baseline is protected.
[ ] Known realism gaps are explicitly acknowledged.
[ ] Implementation migration can proceed incrementally.
```

---

# 163. Closing Decision

The architecture is intentionally **pragmatic rather than pattern-maximal**.

The project should not attempt to demonstrate every software-design pattern.

It should demonstrate that good architecture can make a sophisticated data-generation system:

```text
understandable
testable
reproducible
configurable
realistic
scalable
maintainable
```

The most important architectural decisions are therefore not the names of the patterns.

They are the boundaries:

```text
business domain
        ↓
behavior model
        ↓
generation
        ↓
relationships
        ↓
validation/statistics
        ↓
output
        ↓
Spark workloads
```

Each layer should have a clear purpose.

Each dependency should be intentional.

Each major decision should have a reason.

And when a future developer asks:

> "Why did we build it this way?"

the answer should be available in the architecture rather than hidden in the code.
