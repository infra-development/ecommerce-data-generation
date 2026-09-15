# 03 — Project Progress and Development Control

## 1. Purpose

This document is the living control document for the `ecommerce-data-generation` project.

Its purpose is to answer, at any point in development:

- Where is the project now?
- What has already been designed?
- What has already been implemented?
- What has been validated?
- What is currently being redesigned or migrated?
- What remains to be built?
- Which parts are structurally complete but still lack realism?
- Which architectural decisions are already settled?
- What is the next highest-value piece of work?

This is not a historical changelog.

It is a **development ledger and decision-control document**.

The project should use this document to prevent three common problems:

1. rebuilding functionality that already exists,
2. confusing implementation completeness with realism completeness,
3. changing architecture without understanding its effect on the rest of the system.

---

# 2. Current Project Position

## 2.1 Overall assessment

The project currently has a strong functional foundation and a working end-to-end generation pipeline.

The current implementation should be considered approximately:

> **70% complete as a project foundation, but substantially less mature in sophisticated data realism.**

The 70% assessment should not be interpreted as:

> "70% of the final sophisticated generator is already finished."

Instead, it means that a substantial portion of the foundational engineering work is already present:

- Scala/SBT project setup,
- configuration loading,
- reference data,
- entity models,
- deterministic random generation,
- generation pipeline,
- relationships,
- CSV output,
- validation,
- statistics,
- observability,
- baseline tests,
- reproducible generation,
- baseline cardinality control.

The largest remaining gap is not simply adding more entities.

The largest remaining gap is making the generated world behave like a plausible e-commerce business.

---

# 3. What Is Already Working

## 3.1 Build and runtime foundation

Current technology decisions:

- Scala 2.13
- Java 17 LTS
- SBT
- Typesafe Config / HOCON
- Jackson Scala module where required
- ScalaTest
- CSV output initially
- deterministic seeded generation

The application can run as a normal Scala application.

Spark is intentionally not part of the generator runtime.

The generator is intended to produce datasets that can later be consumed by Spark performance experiments.

---

# 4. Current End-to-End Generation

The baseline pipeline can currently generate:

- Customers
- Addresses
- Products
- Categories
- Brands
- Orders
- Order Items
- Payments
- Shipments
- Returns
- Sessions
- Events

The baseline dataset has successfully produced approximately:

```text
Customers:  1000
Addresses:  1200
Products:   500
Orders:     5000
OrderItems: 12000
Payments:   5000
Shipments:  5000
Returns:    400
Sessions:   3000
Events:     24000
```

The observed output can vary where generation rules intentionally involve randomness.

---

# 5. Current Validation State

The baseline end-to-end generation has successfully passed:

```text
Structural PASS
Primary-key PASS
Foreign-key PASS
Business-rule PASS
```

The test suite has also reached:

> **229 passing tests**

This establishes that the project is currently much stronger in structural correctness than in behavioral realism.

That distinction must remain explicit throughout development.

---

# 6. Current Data Characteristics

The current baseline statistics include:

## 6.1 Order items

```text
mean = 2.40
min = 2
p25 = 2
median = 2
p75 = 3
p95 = 3
p99 = 3
max = 3
```

This is structurally valid but too narrow for a mature e-commerce simulation.

A future model should allow substantially more heterogeneous order sizes.

---

## 6.2 Sessions

Current baseline behavior is effectively deterministic:

```text
sessions per customer = exactly 3
```

This is a major realism gap.

Real customers should have heterogeneous activity levels.

Examples of plausible behavior include:

- inactive customers,
- occasional visitors,
- regular customers,
- highly active customers,
- customers who return during campaigns,
- customers who become dormant,
- customers who reactivate.

---

## 6.3 Events

Current baseline behavior is also effectively deterministic:

```text
events per session = exactly 8
```

This should eventually become a distribution driven by session behavior and funnel progression.

A session should not automatically contain the same number of events.

---

## 6.4 Addresses

Current baseline behavior:

```text
mean = 1.20
min = 1
p25 = 1
median = 1
p75 = 1
p95 = 2
p99 = 2
max = 2
```

This is already more heterogeneous than sessions and events.

The current address model intentionally contains:

- address ID,
- customer ID,
- building ID,
- unit number,
- postal code.

The geography hierarchy resolves location rather than duplicating city/state/country fields inside the address record.

---

## 6.5 Returns

Current baseline behavior:

```text
average returns per order ≈ 0.0478
```

This is useful as a starting point, but future return probability should depend on business factors rather than only an approximately global probability.

Potential factors include:

- customer return tendency,
- product/category return tendency,
- order characteristics,
- delivery behavior,
- price/value,
- customer segment.

---

## 6.6 Monetary values

The current baseline produces:

```text
Average order value ≈ ₹226,377
Median order value ≈ ₹144,080
Maximum ≈ ₹1,753,065
```

This is too expensive for a broad generic e-commerce baseline.

The pricing and order-value model therefore requires redesign before the project can be considered highly realistic.

The target is not to match a particular company's private distribution.

The target is to create a statistically plausible and internally coherent e-commerce economy.

---

# 7. Current Architectural Direction

The project is now moving toward a domain-oriented architecture.

The guiding structure is:

```text
com/shopsphere/datagenerator/
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
├── product/
├── category/
├── brand/
├── order/
├── orderitem/
├── payment/
├── shipment/
├── return/
├── session/
├── event/
│
├── geography/
│   ├── model/
│   ├── reference/
│   └── loader/
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
├── observability/
│
└── Main.scala
```

This is the target direction, not a claim that the repository has already been completely migrated to this structure.

---

# 8. Architectural Migration Status

The architecture migration should be performed incrementally.

Do not move every file at once.

The preferred sequence is:

1. Establish common infrastructure.
2. Establish geography as a coherent domain.
3. Establish category.
4. Establish brand.
5. Establish product.
6. Establish customer.
7. Establish address.
8. Establish order.
9. Establish order item.
10. Establish payment.
11. Establish shipment.
12. Establish return.
13. Establish session.
14. Establish event.
15. Clean up relationship orchestration.
16. Clean up global validation.
17. Clean up global statistics.
18. Perform final architecture verification.

Each migration step should follow:

```text
Understand
→ Design
→ Move
→ Refactor
→ Test
→ Review
→ Record
```

A package move without redesign is not considered architectural progress.

---

# 9. Documentation Status

The project documentation is being rebuilt around the domain model.

Current primary documents:

```text
00-project-overview-and-business-domain.md
01-architecture-and-design-principles.md
02-customer-domain.md
03-project-progress-and-development-control.md
```

These documents establish:

- business context,
- system architecture,
- design principles,
- Customer domain understanding,
- current project state,
- development control.

Additional domain documents should be created one entity at a time.

---

# 10. Domain Documentation Roadmap

The expected domain-document sequence is:

```text
04-address-domain.md
05-geography-domain.md
06-category-domain.md
07-brand-domain.md
08-product-domain.md
09-order-domain.md
10-orderitem-domain.md
11-payment-domain.md
12-shipment-domain.md
13-return-domain.md
14-session-domain.md
15-event-domain.md
```

The exact order can change if implementation dependencies make another sequence more appropriate.

Each domain document should explain both:

```text
Business meaning
+
Technical design
```

Each should cover, where applicable:

1. business meaning,
2. attributes,
3. lifecycle,
4. behavior,
5. relationships,
6. generation strategy,
7. configuration,
8. technical implementation,
9. validation,
10. current simplifications,
11. future realism,
12. design decisions,
13. testing strategy.

---

# 11. Cross-Cutting Documentation Roadmap

After the main domains are understood, cross-cutting design should be documented explicitly.

Planned topics include:

```text
Distribution models
Randomness and reproducibility
Relationship generation
Data-quality injection
Skew scenarios
Temporal modeling
Validation architecture
Output and storage
Statistics and observability
Scenario architecture
Configuration architecture
```

The existing decision documents should be mined for useful content rather than blindly discarded.

Useful historical decisions should become:

- concept documentation,
- architecture documentation,
- ADR history,
- domain documentation,
- or implementation guidance.

---

# 12. Realism Maturity

Realism is currently the primary development focus.

The project should distinguish the following maturity levels.

## Level 1 — Structural correctness

The record:

- has the correct fields,
- satisfies required constraints,
- has valid identifiers,
- has valid foreign keys,
- obeys basic business rules.

Current status:

> Strong.

---

## Level 2 — Cardinality realism

Relationships have plausible distributions.

Examples:

- customers → addresses,
- customers → orders,
- orders → items,
- customers → sessions,
- sessions → events.

Current status:

> Partially implemented.

---

## Level 3 — Distribution realism

Important numerical and categorical fields follow plausible distributions.

Examples:

- order value,
- product popularity,
- order frequency,
- session activity,
- item counts,
- returns,
- customer spending.

Current status:

> Early / partially implemented.

---

## Level 4 — Behavioral realism

Entities behave differently because of latent behavioral characteristics.

Examples:

- high-value customers spend more,
- active customers generate more sessions,
- some customers prefer specific categories,
- some customers have stronger discount sensitivity,
- some customers return products more frequently.

Current status:

> Not yet mature.

---

## Level 5 — Correlated realism

Behavior is correlated across entities.

For example:

```text
Customer behavior
    ↓
Product preference
    ↓
Product selection
    ↓
Order value
    ↓
Return probability
```

Another example:

```text
Customer activity
    ↓
Session frequency
    ↓
Event volume
    ↓
Conversion probability
    ↓
Order frequency
```

Current status:

> Planned.

---

## Level 6 — Temporal realism

Behavior changes over time.

Examples:

- customer lifecycle,
- acquisition cohorts,
- campaigns,
- seasonality,
- promotions,
- product lifecycle,
- changing activity,
- reactivation.

Current status:

> Planned.

---

## Level 7 — Scenario realism

The generator can intentionally create recognizable workloads.

Examples:

- clean baseline,
- hot products,
- hot customers,
- promotional spike,
- seasonal demand,
- high-return category,
- inactive customer population,
- skew-heavy workload,
- data-quality problems.

Current status:

> Architecture planned; implementation maturity is limited.

---

# 13. Core Realism Architecture

The key planned architectural improvement is a latent customer behavior model.

Conceptually:

```text
Customer
   │
   ▼
CustomerBehaviorProfile
   │
   ├── activity
   ├── spending
   ├── purchase frequency
   ├── price sensitivity
   ├── category affinity
   ├── brand affinity
   ├── device affinity
   ├── payment affinity
   ├── return propensity
   └── lifecycle state
```

This profile should influence downstream generators.

For example:

```text
CustomerBehaviorProfile
        │
        ├──────────────► Sessions
        │
        ├──────────────► Events
        │
        ├──────────────► Orders
        │
        ├──────────────► Product Selection
        │
        ├──────────────► Payment Method
        │
        └──────────────► Returns
```

This avoids generating every entity independently.

Independent random generation is one of the main reasons synthetic data becomes statistically incoherent.

---

# 14. Planned Strategy-Based Behavioral Models

Where genuine behavioral alternatives exist, use Strategy.

Examples:

```text
OrderFrequencyModel
CustomerActivityModel
CustomerSpendingModel
ProductPopularityModel
ReturnProbabilityModel
SessionLengthModel
EventCountModel
```

The purpose is not to introduce patterns for their own sake.

The purpose is to make meaningful business behavior replaceable and testable.

For example:

```text
ProductPopularityModel
    ├── UniformPopularity
    ├── LongTailPopularity
    └── ScenarioDrivenPopularity
```

The concrete strategy should be selected through configuration/factory logic where appropriate.

---

# 15. SOLID and Readability Quality Gate

Every major redesign should be evaluated against:

## Single Responsibility

A class should have one meaningful reason to change.

---

## Open/Closed

New business behavior should be addable without repeatedly rewriting stable orchestration.

---

## Liskov Substitution

A strategy implementation must honor the contract expected by its consumer.

---

## Interface Segregation

Do not create large interfaces containing unrelated operations.

---

## Dependency Inversion

Domain behavior should not be unnecessarily coupled to concrete infrastructure.

Constructor injection is preferred.

No dependency-injection framework is currently required.

---

# 16. Readability Standard

Code should communicate business intent.

Prefer:

```scala
val profile = behaviorModel.generate(context)
val preferences = preferenceGenerator.generate(profile, context)
val acquisition = acquisitionGenerator.generate(profile, context)
```

over:

```scala
val x = helper.process(a, b, c)
val y = manager.createThing(x)
```

Names should express business concepts.

Avoid vague names such as:

- Helper
- Manager
- Processor
- Util

unless the name has a genuinely precise meaning.

---

# 17. Testing Progress

Testing should continue at multiple levels.

## Unit tests

Test:

- distributions,
- behavior models,
- generators,
- factories,
- validation rules,
- configuration interpretation.

---

## Relationship tests

Test:

- foreign-key correctness,
- relationship cardinality,
- parent-child consistency,
- uniqueness.

---

## Property-style tests

Useful properties include:

```text
same seed → same output
different seed → different output
configured cardinality → expected record count
probabilities → valid bounds
IDs → unique
foreign keys → resolvable
```

---

## Statistical tests

As realism increases, tests should also verify distributional properties.

Examples:

- order-size distribution,
- product popularity skew,
- customer activity skew,
- return rate,
- AOV,
- category concentration.

These tests should use tolerances rather than brittle exact-value assertions.

---

## End-to-end tests

The full pipeline must continue to:

1. load configuration,
2. load reference data,
3. build a generation plan,
4. generate all entities,
5. write output,
6. validate output,
7. produce statistics/manifest,
8. complete reproducibly.

---

# 18. Current Architectural Risk Register

## Risk 1 — Over-refactoring

Moving too many files at once can break the system and make failures difficult to isolate.

Response:

> One domain at a time.

---

## Risk 2 — Pattern overuse

Adding Strategy/Factory/Service abstractions without real variation creates unnecessary complexity.

Response:

> Introduce a pattern only when it solves a concrete design problem.

---

## Risk 3 — Independent randomness

Independent random choices can create statistically inconsistent data.

Response:

> Introduce shared behavioral profiles and correlated generation.

---

## Risk 4 — Confusing correctness with realism

A dataset can pass every foreign-key test while still looking unrealistic.

Response:

> Track structural correctness and realism separately.

---

## Risk 5 — Premature optimization

Optimizing code before the statistical model is correct can preserve the wrong behavior at higher speed.

Response:

> Correctness → realism → architecture → performance tuning.

---

## Risk 6 — Configuration explosion

Every new behavior can become another configuration parameter.

Response:

> Expose meaningful business controls, not every internal implementation detail.

---

## Risk 7 — Hidden coupling

Entity generators may become tightly coupled through shared mutable state.

Response:

> Prefer explicit immutable inputs and constructor dependencies.

---

# 19. Current Technical Cleanup

Known cleanup items include obsolete address-generation configuration and code.

Current address design intentionally contains only:

```text
id
customerId
buildingId
unitNumber
postalCode
```

The geography hierarchy is responsible for resolving the surrounding location.

The obsolete `AddressGenerationConfig` and corresponding configuration section should eventually be removed.

This cleanup should be performed separately and tested independently.

---

# 20. Development Sequence From Here

The immediate sequence should be:

### Phase A — Architecture documentation

Complete:

```text
00 Project Overview
01 Architecture and Design Principles
02 Customer Domain
03 Project Progress
```

Status:

> Current phase.

---

### Phase B — Domain understanding

Create the remaining domain documents.

Start with domains that establish reference dependencies.

Recommended order:

```text
Address
Geography
Category
Brand
Product
Customer
Order
OrderItem
Payment
Shipment
Return
Session
Event
```

Customer is already documented and should serve as the reference architecture for later domains.

---

### Phase C — Architecture migration

Migrate one domain at a time.

For each domain:

```text
1. Inspect current code
2. Understand business responsibility
3. Design target package
4. Identify behavior abstractions
5. Identify cross-domain dependencies
6. Move code
7. Refactor names/responsibilities
8. Add/fix tests
9. Run full suite
10. Review generated behavior
11. Update progress
```

---

### Phase D — Behavioral realism

After the architecture is stable enough:

```text
CustomerBehaviorProfile
        ↓
Customer activity
        ↓
Session behavior
        ↓
Event funnel
        ↓
Order frequency
        ↓
Product selection
        ↓
Spending behavior
        ↓
Returns
```

This should be implemented incrementally.

---

### Phase E — Distribution realism

Improve:

- product popularity,
- customer activity,
- order frequency,
- order size,
- spending,
- returns,
- session length,
- event counts.

Use distributions appropriate to each business quantity.

Do not use the same generic random distribution everywhere.

---

### Phase F — Correlation

Introduce relationships between behavioral variables.

Examples:

```text
high activity ↔ more sessions
high activity ↔ more orders
high spending ↔ higher AOV
category affinity ↔ product selection
return propensity ↔ return events
discount sensitivity ↔ promotion response
```

---

### Phase G — Temporal modeling

Introduce:

- customer lifecycle,
- acquisition cohorts,
- seasonality,
- promotions,
- product lifecycle,
- temporal demand variation.

---

### Phase H — Scenario engine

Implement controlled scenarios such as:

```text
baseline
hot-products
hot-customers
promotion-spike
seasonal-peak
high-return
data-quality
```

Scenarios should modify behavior through explicit mechanisms rather than scattered special cases.

---

### Phase I — Statistical validation

Build validation that can answer:

> Does the generated dataset statistically resemble the intended business model?

This includes:

- distribution summaries,
- percentiles,
- concentration measures,
- skew measures,
- relationship checks,
- scenario assertions.

---

# 21. Definition of Done

The project should not be considered complete merely because:

- every entity exists,
- every CSV is written,
- all foreign keys pass,
- tests pass.

The final generator should satisfy all of the following.

## Business

- entities have coherent business meaning,
- lifecycle rules are explicit,
- relationships are meaningful.

## Architecture

- domain responsibilities are clear,
- cross-cutting infrastructure is centralized,
- dependencies are explicit,
- abstractions are justified,
- code is readable.

## Configuration

- volume is configurable,
- cardinality is configurable,
- behavior is configurable,
- scenarios are configurable,
- reproducibility is configurable.

## Realism

- important quantities have plausible distributions,
- entities are heterogeneous,
- relationships are correlated where appropriate,
- temporal behavior is represented,
- long-tail behavior exists where appropriate.

## Correctness

- primary keys are valid,
- foreign keys are valid,
- business rules pass,
- generated records are internally coherent.

## Reproducibility

- the same configuration and seed reproduce the same logical dataset.

## Quality scenarios

- clean data can be generated,
- controlled dirty data can be generated,
- controlled skew can be generated,
- scenario behavior is measurable.

## Observability

The generator should report enough information to understand:

- what was generated,
- how much was generated,
- which configuration was used,
- which seed was used,
- which distributions were selected,
- which scenarios were active,
- what the resulting statistics look like,
- whether validation passed.

---

# 22. Project Milestones

## M0 — Functional foundation

Status:

> Complete / baseline established.

Includes:

- build,
- configuration,
- reference data,
- entity models,
- generation pipeline,
- output,
- validation,
- baseline tests.

---

## M1 — Architecture and domain design

Status:

> In progress.

Target:

- domain-oriented package architecture,
- domain documentation,
- explicit design responsibilities,
- justified behavioral abstractions,
- Customer as reference architecture.

---

## M2 — Domain architecture migration

Status:

> Planned.

Target:

- migrate entities into domain packages,
- clean dependencies,
- remove obsolete code,
- preserve behavior during migration.

---

## M3 — Behavioral realism

Status:

> Planned.

Target:

- customer behavior profiles,
- heterogeneous customer activity,
- heterogeneous purchasing,
- meaningful downstream influence.

---

## M4 — Distribution realism

Status:

> Planned.

Target:

- long-tail product demand,
- realistic customer activity,
- realistic order sizes,
- realistic monetary distributions,
- realistic return behavior.

---

## M5 — Correlated realism

Status:

> Planned.

Target:

- cross-entity behavioral correlations,
- coherent customer journeys,
- meaningful preferences.

---

## M6 — Temporal realism

Status:

> Planned.

Target:

- lifecycle,
- seasonality,
- campaigns,
- temporal demand,
- product lifecycle.

---

## M7 — Scenario engine

Status:

> Planned.

Target:

- controlled skew,
- campaign spikes,
- high-return populations,
- data-quality scenarios,
- workload-specific distributions.

---

## M8 — Statistical validation and calibration

Status:

> Planned.

Target:

- distribution validation,
- relationship validation,
- scenario validation,
- calibration reports,
- regression detection.

---

## M9 — Final generator release

Status:

> Future.

Target:

- stable architecture,
- mature realism,
- reproducibility,
- configurable scenarios,
- complete validation,
- documented behavior,
- production-quality generator.

---

# 23. Work Tracking Rules

Every meaningful implementation task should update this document.

The update should identify:

```text
Task
Domain
Reason
Design decision
Files changed
Tests changed
Current status
Known limitations
Next step
```

Do not mark a task complete merely because the code compiles.

A domain migration is complete only when:

```text
architecture
+
tests
+
behavior
+
documentation
```

are all reviewed.

---

# 24. Current Next Step

The immediate next task is **not** to start moving files blindly.

The next step is to continue domain documentation and architecture design.

Recommended sequence:

```text
03 Project Progress
        ↓
04 Address Domain
        ↓
05 Geography Domain
        ↓
06 Category Domain
        ↓
07 Brand Domain
        ↓
08 Product Domain
        ↓
...
```

Once the domain model is documented sufficiently, begin code migration one domain at a time.

---

# 25. Operating Principle

The project should continuously move through:

```text
Business understanding
        ↓
Domain model
        ↓
Behavior model
        ↓
Technical design
        ↓
Implementation
        ↓
Tests
        ↓
Generated data
        ↓
Statistical validation
        ↓
Review
```

A technically elegant implementation that produces unrealistic data is not a successful result.

A realistic dataset produced by unmaintainable code is also not a successful result.

The objective is:

> **A readable, configurable, reproducible, statistically plausible e-commerce world implemented through a disciplined domain-oriented architecture.**

This document should be updated whenever the project materially changes direction, reaches a milestone, discovers a major limitation, or completes a domain migration.
