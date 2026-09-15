# 31 — Implementation Execution Plan and Source Inventory Phase

## 1. Purpose

The architecture and domain documentation phase is now complete.

The next phase is **implementation execution**. We will not immediately rewrite the generator or jump into sophisticated realism. We will first establish a precise understanding of the current source tree and use that as the baseline for a controlled migration toward the documented target architecture.

The governing principle for this phase is:

> **Understand the current implementation first, then migrate it incrementally, one file at a time, while preserving correctness.**

This document records the execution plan so that development remains aligned with the architecture already documented.

---

## 2. Current Position

The project currently has a functioning generator foundation.

Known baseline:

- Scala 2.13
- SBT build is working
- 229 automated tests passed
- End-to-end generation completed successfully
- CSV output is being produced
- Structural validation passes
- Primary-key validation passes
- Foreign-key validation passes
- Business-rule validation passes
- The current generator can produce a coherent multi-entity dataset

The important distinction is:

> **The generator is functionally working, but the generated data is not yet sophisticated enough to represent a highly realistic e-commerce world.**

Therefore, the next phase is not a rewrite from scratch.

It is a **controlled architectural migration followed by realism improvements**.

---

# 3. Immediate Next Step — Current Source Architecture Inventory

Before changing source code, inspect the current implementation completely.

The inventory must cover:

1. Every Scala production source file
2. Every Scala test file
3. Configuration files
4. Build configuration
5. Runtime resources
6. Existing package structure
7. Major dependency relationships
8. Existing generators
9. Existing models
10. Existing validation
11. Existing statistics
12. Existing output code
13. Existing relationship logic
14. Existing scenario/quality logic
15. Existing random/distribution infrastructure
16. Existing orchestration/pipeline logic

The objective is to understand **what actually exists**, rather than assuming that the implementation matches the documentation.

---

# 4. Source Classification

Every existing source file should be classified into one of five categories.

## A — Correctly placed

The file already belongs naturally in the target architecture.

Example:

```text
distribution/RandomGenerator.scala
```

may eventually belong under:

```text
common/random/
```

If the responsibility is already appropriate and only the package location needs adjustment, it is a straightforward migration.

---

## B — Wrong package, correct responsibility

The implementation is conceptually sound but is located in a package that does not match the target domain ownership.

Example:

```text
generation/AddressGenerator.scala
```

could move to:

```text
address/generator/AddressGenerator.scala
```

without fundamentally changing its business responsibility.

---

## C — Mixed responsibility

A source file performs several responsibilities that should be separated.

Examples:

- generation + business rules
- generation + statistics
- entity construction + relationship management
- configuration loading + domain behavior
- validation + data repair

These files require careful decomposition rather than a simple move.

---

## D — Obsolete

The file or configuration exists because of an earlier design but is no longer part of the intended architecture.

Example:

```text
AddressGenerationConfig.scala
```

is currently obsolete because the Address domain no longer uses the previously proposed address-generation configuration model.

Obsolete components should be removed only after all references have been eliminated and tests remain green.

---

## E — Missing

The target architecture requires a capability that does not currently exist.

Examples may include:

- CustomerBehaviorProfile
- customer behavior strategies
- product popularity model
- realistic return probability model
- temporal behavior model
- lifecycle model
- richer scenario implementation

Missing components should not be created speculatively before their responsibility and dependencies are understood.

---

# 5. Gap Analysis

After classification, create a gap analysis between:

```text
CURRENT IMPLEMENTATION
        ↓
TARGET ARCHITECTURE
```

For each area, record:

| Area | Current State | Target State | Gap | Action |
|---|---|---|---|---|
| Customer | Existing generator/model | Domain-oriented customer module | To inspect | Migrate/refactor |
| Address | Existing generator/model | Address domain | Package/ownership review | Migrate |
| Product | Existing implementation | Product domain | To inspect | Migrate |
| Order | Existing implementation | Order domain | To inspect | Migrate |
| Randomness | Existing infrastructure | Common random module | To inspect | Migrate |
| Validation | Existing global validation | Global + domain validation | To inspect | Split where justified |
| Statistics | Existing global statistics | Global + domain statistics | To inspect | Split where justified |
| Realism | Basic distributions | Behavioral simulation | Significant gap | Implement later |

The table should be based on actual source inspection, not assumptions.

---

# 6. The Migration Rule

We will follow this exact development loop:

```text
SOURCE INVENTORY
       ↓
GAP ANALYSIS
       ↓
SELECT ONE FILE
       ↓
MAKE ONE CONTROLLED CHANGE
       ↓
sbt compile
       ↓
sbt test
       ↓
REVIEW RESULT
       ↓
SELECT NEXT FILE
```

This is deliberately conservative.

The objective is to keep the project continuously understandable and recoverable.

---

# 7. One-File-at-a-Time Discipline

We will not perform a broad package migration in one operation.

Instead:

1. Select one source file.
2. Determine its target location.
3. Determine whether its responsibility should change.
4. Identify all references to it.
5. Make the smallest necessary change.
6. Compile.
7. Run the complete test suite.
8. Review the result.
9. Only then proceed to the next file.

This prevents large batches of architectural changes from hiding unrelated failures.

It also gives us a clean history of why each change was made.

---

# 8. Migration Order

The implementation migration follows the dependency structure documented in the architecture blueprint.

## Phase 0 — Baseline and Inventory

First:

- capture the current build state
- inspect source tree
- inspect tests
- inspect configuration
- inspect dependencies
- identify package ownership
- identify obsolete components
- identify missing capabilities
- build the current-source architecture map

No architectural rewrite should begin before this inventory is complete.

---

## Phase 1 — Common Foundation

Establish stable cross-cutting primitives:

```text
common/
├── random/
├── distribution/
├── time/
└── util/
```

The purpose is to ensure that domain generators depend on stable common abstractions rather than duplicating infrastructure.

---

## Phase 2 — Reference and Catalog Domains

Migrate:

```text
geography/
category/
brand/
product/
```

These provide reference data and product/catalog context required by downstream domains.

---

## Phase 3 — Customer and Address

Migrate:

```text
customer/
address/
```

Customer is especially important because it becomes the primary behavioral anchor for realistic generation.

Address remains intentionally simple and must preserve the current documented model:

```text
id
customer_id
building_id
unit_number
postal_code
```

The geography hierarchy remains responsible for resolving location context.

---

## Phase 4 — Transaction Domains

Migrate:

```text
order/
orderitem/
payment/
shipment/
return/
```

These domains should eventually express business behavior rather than simply generating independent random records.

---

## Phase 5 — Activity Domains

Migrate:

```text
session/
event/
```

These will later become part of the customer activity/funnel model.

---

## Phase 6 — Cross-Cutting Cleanup

Review and migrate:

```text
relationship/
validation/
statistics/
quality/
scenario/
```

The goal is to distinguish:

- domain-owned behavior
- cross-domain business services
- global validation
- scenario orchestration
- quality injection

without creating unnecessary abstractions.

---

## Phase 7 — Application Orchestration

Refine the application-level flow so that orchestration is explicit.

The intended high-level sequence is:

```text
Preflight
   ↓
Load Reference Data
   ↓
Build Generation Plan
   ↓
Generate Domains
   ↓
Validate
   ↓
Calculate Statistics
   ↓
Write Output
   ↓
Generate Manifest
   ↓
Report Observability Metrics
```

The application orchestrator coordinates the process.

It should not contain the business logic of every domain.

---

# 9. Realism Comes After Architectural Stabilization

Once the architecture is clean and stable, we will improve generated-data realism.

This is intentionally later in the sequence.

The realism work will include:

### Customer behavior

Introduce a latent behavioral profile containing concepts such as:

- activity level
- spending level
- purchase frequency
- price sensitivity
- category affinity
- brand affinity
- device preference
- payment preference
- return propensity
- lifecycle state

### Product popularity

Introduce long-tail behavior so that:

- a small number of products are highly popular
- a larger number have moderate demand
- many products have low demand

### Order behavior

Move away from narrow deterministic distributions toward heterogeneous customer behavior.

### Session behavior

Move from:

```text
exactly 3 sessions/customer
```

toward a realistic distribution influenced by customer activity and lifecycle.

### Event behavior

Move from:

```text
exactly 8 events/session
```

toward session-type and funnel-dependent event sequences.

### Return behavior

Make return probability depend on multiple factors:

```text
customer propensity
+
product/category behavior
+
order characteristics
+
possibly temporal/contextual effects
```

### Monetary behavior

Calibrate prices, quantities, discounts, and customer behavior so that order values resemble plausible e-commerce distributions rather than simply satisfying mathematical constraints.

---

# 10. Realism Architecture Principle

The most important realism principle is:

> **Behavior should be generated once and reused across related domains.**

For example:

```text
CustomerBehaviorProfile
        │
        ├── Sessions
        ├── Events
        ├── Orders
        ├── Product Selection
        ├── Spending
        ├── Payment Preference
        └── Returns
```

This is substantially better than generating each entity independently and attempting to correlate them afterward.

The result should be a coherent synthetic population rather than a collection of independently randomized tables.

---

# 11. Strategy and Composition

Where genuine behavioral variation exists, use small interchangeable strategies.

Examples:

```text
OrderFrequencyModel
CustomerActivityModel
CustomerSpendingModel
ProductPopularityModel
ReturnProbabilityModel
```

A configuration or scenario may select a strategy.

The preferred design is composition:

```text
CustomerGenerator
    ↓
BehaviorModel
    ↓
IdentityGenerator
    ↓
PreferenceGenerator
    ↓
CustomerFactory
```

We should avoid creating interfaces simply because an interface is theoretically possible.

---

# 12. Design Pattern Rule

Patterns are tools, not objectives.

Use:

- Strategy when behavior genuinely varies
- Factory when object creation has meaningful policy/configuration
- Builder only when construction is genuinely complex
- Domain services when an operation spans domain entities
- Dependency injection through constructors

Do not introduce:

- unnecessary frameworks
- generic `Manager` classes
- generic `Helper` classes
- artificial interfaces
- enterprise-style abstractions without a real need

---

# 13. Validation Rule

Every migration must preserve correctness.

At minimum, after meaningful changes:

```powershell
sbt compile
sbt test
```

For changes affecting generation behavior, also run:

```powershell
sbt run
```

and verify:

- record counts
- relationships
- validation status
- output files
- important distribution statistics
- reproducibility where relevant

A refactoring is not complete merely because it compiles.

---

# 14. Definition of Done for an Architectural Migration

A migrated component is considered complete when:

- it is in the correct package
- responsibility is clearly defined
- dependencies point in the intended direction
- obsolete dependencies are removed
- tests are updated
- compilation succeeds
- the full test suite succeeds
- behavior is preserved unless an intentional behavior change was planned
- documentation remains consistent with the implementation

---

# 15. What We Will Not Do

We will explicitly avoid:

### No big-bang rewrite

Do not replace the whole project with a new architecture in one step.

### No realism-first rewrite

Do not introduce sophisticated behavioral simulation before understanding and stabilizing the existing architecture.

### No speculative framework

Do not introduce dependency-injection frameworks, actor frameworks, streaming frameworks, or other infrastructure unless a concrete requirement appears.

### No pattern-for-pattern's-sake design

A pattern must solve an actual design problem.

### No unnecessary entity duplication

Keep business ownership clear.

### No accidental Address redesign

The current five-field Address model remains the design unless explicitly changed.

---

# 16. Immediate Work Queue

The next work items are therefore:

```text
1. Inspect current source tree
2. Inspect current tests
3. Inspect configuration
4. Inspect build/dependencies
5. Build current-source architecture inventory
6. Classify every relevant file A/B/C/D/E
7. Build current → target gap analysis
8. Identify the first migration file
9. Change ONE FILE
10. Compile
11. Test
12. Review
13. Repeat
```

Only after this migration is sufficiently stable should we move into the major realism implementation.

---

# 17. Overall Roadmap

The complete project roadmap is:

```text
DOCUMENTATION
     ✓
     │
     ▼
SOURCE INVENTORY
     │
     ▼
GAP ANALYSIS
     │
     ▼
ARCHITECTURAL MIGRATION
     │
     ├── Common
     ├── Geography / Catalog
     ├── Customer / Address
     ├── Transactions
     ├── Activity
     └── Cross-cutting
     │
     ▼
APPLICATION ORCHESTRATION
     │
     ▼
REALISM ENGINE
     │
     ├── Customer behavior
     ├── Long-tail demand
     ├── Correlations
     ├── Lifecycle
     ├── Temporal behavior
     ├── Funnel behavior
     └── Monetary calibration
     │
     ▼
SCENARIOS / QUALITY / SKEW
     │
     ▼
STATISTICAL VALIDATION
     │
     ▼
PERFORMANCE / SCALE
     │
     ▼
STABLE E-COMMERCE DATA PLATFORM
     │
     ▼
SPARK PERFORMANCE LABORATORY
```

---

# 18. Guiding Principle

The project should evolve in this order:

> **Correctness → Architecture → Realism → Validation → Scale → Performance**

Not:

> **More code → more features → hope the dataset looks realistic.**

The architecture exists to make realistic behavior understandable, configurable, testable, reproducible, and scalable.

The next concrete action is therefore **source inspection and architecture inventory**.

No implementation changes should be made until that inventory gives us a reliable map of the current codebase.
