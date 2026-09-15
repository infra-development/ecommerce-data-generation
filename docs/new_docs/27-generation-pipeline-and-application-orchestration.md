# 27 — Generation Pipeline & Application Orchestration

## 1. Purpose

The ShopSphere generator is not simply a collection of entity generators.

It is an application that must coordinate:

```text
configuration
reference data
generation planning
behavior models
entity generation
relationships
validation
statistics
output
manifest
observability
```

The purpose of this document is to define how those responsibilities should be composed into one coherent generation pipeline.

The central architectural rule is:

> **Entity generators own entity behavior; application orchestration owns the sequence and coordination of the generation run.**

This prevents individual generators from becoming responsible for the entire synthetic world.

---

# 2. Why Orchestration Needs Its Own Architecture

Without an explicit orchestration boundary, generation logic tends to grow into a large method such as:

```text
load config
load geography
generate customers
generate addresses
generate categories
generate brands
generate products
generate orders
generate items
generate payments
generate shipments
generate returns
generate sessions
generate events
validate
calculate statistics
write files
```

This may work initially.

As realism and scale increase, it becomes difficult to:

- understand dependencies;
- test phases independently;
- change execution order;
- parallelize independent phases;
- introduce streaming;
- handle failures;
- report progress;
- support scenarios.

Orchestration should therefore be explicit.

---

# 3. Core Separation

The target architecture separates four levels:

```text
Domain behavior
      ↓
Entity generation
      ↓
Generation workflow
      ↓
Application lifecycle
```

### Domain behavior

Answers:

> How does this business concept behave?

### Entity generation

Answers:

> How do we construct records for this entity?

### Generation workflow

Answers:

> In what order are entities generated and connected?

### Application lifecycle

Answers:

> How does a complete generation run start, succeed, fail, and publish?

---

# 4. Application Entry Point

The application entry point should be thin.

Conceptually:

```scala
object Main extends App {
  val application = Application.bootstrap(...)
  application.run()
}
```

The entry point should not contain business-generation logic.

It should primarily:

- parse runtime arguments;
- initialize the application;
- invoke the generation use case;
- report the final outcome.

---

# 5. Bootstrap

Application bootstrap should construct the major dependencies.

Conceptually:

```text
arguments
   ↓
configuration
   ↓
reference data
   ↓
effective generation plan
   ↓
behavior models
   ↓
generators
   ↓
validators
   ↓
writer
   ↓
orchestrator
```

This is where dependency injection happens.

---

# 6. Composition Root

The bootstrap layer is the natural **composition root**.

It should decide:

```text
which implementation
which configuration
which strategy
which output writer
which scenario
```

Domain classes should not construct their own dependencies from global configuration.

---

# 7. Constructor Injection

The project does not need a dependency-injection framework.

Constructor injection is sufficient.

Example:

```scala
final class GenerationApplication(
  orchestrator: GenerationOrchestrator
)
```

and:

```scala
final class GenerationOrchestrator(
  plan: GenerationPlan,
  customerGenerator: CustomerGenerator,
  productGenerator: ProductGenerator,
  orderGenerator: OrderGenerator,
  validator: DatasetValidator,
  writer: OutputWriter
)
```

The exact constructor should remain focused rather than becoming a dependency container.

---

# 8. Avoid a God Orchestrator

A single orchestration component is useful.

A single component that knows every implementation detail is not.

Avoid:

```text
GenerationOrchestrator
    ├── parses HOCON
    ├── creates random generators
    ├── loads CSV
    ├── validates geography
    ├── generates customers
    ├── selects products
    ├── writes CSV
    └── calculates every statistic
```

Instead:

```text
Application
    ↓
Orchestrator
    ↓
focused services/components
```

---

# 9. Generation Run

A generation run should be treated as a first-class application concept.

Conceptually:

```text
GenerationRun
  runId
  seed
  profile
  scenario
  effective configuration
  generation plan
  status
```

The run identity is different from the dataset itself.

---

# 10. Run Lifecycle

The target lifecycle is:

```text
CREATED
   ↓
PREFLIGHT
   ↓
GENERATING
   ↓
VALIDATING
   ↓
WRITING
   ↓
FINALIZING
   ↓
COMPLETED
```

Failure can occur from active states:

```text
FAILED
```

Cancellation may eventually be:

```text
CANCELLED
```

---

# 11. Preflight Phase

Preflight should validate everything that can be validated cheaply before high-volume generation.

Potential checks:

```text
configuration
reference data
generation plan
scenario
output location
safety limits
```

The goal is to avoid expensive failures.

---

# 12. Effective Configuration

Raw configuration should be transformed into effective configuration before generation.

Conceptually:

```text
reference.conf
+
application.conf
+
profile
+
cardinality profile
+
scenario
+
explicit overrides
        ↓
effective configuration
```

Generators should consume typed effective configuration rather than repeatedly interpreting raw HOCON.

---

# 13. Generation Plan

The generation plan should describe the requested workload.

Current planning already exposes counts such as:

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

The target plan can become richer.

---

# 14. Plan Responsibilities

A generation plan should answer:

```text
how many?
which profile?
which scenario?
which time window?
which entities?
which scale?
```

It should not generate records.

---

# 15. Plan Validation

The plan should verify:

```text
counts are valid
relationships are feasible
scenario is compatible
scale is within safety limits
required reference data exists
```

before generation begins.

---

# 16. Generation Context

A generation context may contain stable dependencies needed across domain generation.

Conceptually:

```text
GenerationContext
  configuration
  reference data
  temporal model
  scenario
  random seed/streams
```

It should remain small.

---

# 17. What Does Not Belong in Context

Avoid putting every application service into the context.

Do not automatically include:

```text
writer
validator
logger
statistics collector
filesystem
```

unless a specific generation component genuinely needs it.

Otherwise the context becomes a hidden service locator.

---

# 18. Generation DAG

The generation process is naturally a dependency graph.

Conceptually:

```text
Reference Data
      |
      +---- Category
      |
      +---- Brand
      |
      +---- Geography
      |
      +---- Product
      |
      +---- Customer
               |
               +---- Address
               |
               +---- Session
                       |
                       +---- Event

Customer + Product
      |
      +---- Order
               |
               +---- OrderItem
               |
               +---- Payment
               |
               +---- Shipment
                       |
                       +---- Return
```

The exact dependency graph should follow the domain interaction architecture.

---

# 19. Dependency Direction

The application should execute dependencies in the required order.

For example:

```text
Product
```

must exist before:

```text
OrderItem
```

can select products.

Likewise:

```text
CustomerBehaviorProfile
```

must exist before customer-driven order frequency is sampled.

---

# 20. Independent Phases

Some reference phases may be independent.

For example:

```text
Category
Brand
Geography
```

can potentially load in parallel.

However, parallelism should only be introduced where it reduces meaningful runtime.

---

# 21. Dependent Phases

Some phases must wait.

Example:

```text
Category + Brand
      ↓
Product
```

and:

```text
Customer + Product
      ↓
Order
      ↓
OrderItem
```

The orchestration layer should express these dependencies explicitly.

---

# 22. Customer Generation

The customer phase should conceptually execute:

```text
customer behavior profile
        ↓
identity
        ↓
acquisition/lifecycle attributes
        ↓
preferences
        ↓
Customer
```

The generator should compose focused components rather than become a monolith.

---

# 23. Address Generation

Address generation depends on:

```text
Customer
Geography
```

The current address model remains:

```text
id
customerId
buildingId
unitNumber
postalCode
```

The orchestrator should pass the required dependencies without expanding the model.

---

# 24. Product Generation

Product generation depends on:

```text
Category
Brand
Product configuration
```

Product behavior should remain responsible for:

```text
catalog attributes
price
popularity metadata where appropriate
```

The orchestrator only sequences the phase.

---

# 25. Order Generation

Order generation requires:

```text
Customer
Product/catalog
Customer behavior
Temporal context
```

It may produce order-level records and information needed for downstream item generation.

---

# 26. OrderItem Generation

OrderItem generation requires:

```text
Order
Product
Customer behavior/preferences
basket model
pricing model
```

It should not be responsible for generating the Order itself.

---

# 27. Payment Generation

Payment generation consumes:

```text
Order
customer behavior
payment method reference
temporal model
```

and applies payment lifecycle behavior.

---

# 28. Shipment Generation

Shipment generation consumes:

```text
Order
Address
Geography
Payment outcome where relevant
temporal model
carrier/service configuration
```

and generates shipment behavior.

---

# 29. Return Generation

Return generation consumes:

```text
Order
OrderItem
Shipment/delivery context
Customer return propensity
Product/category context
temporal model
```

and determines eligible returns.

---

# 30. Session Generation

Session generation consumes:

```text
Customer
CustomerBehaviorProfile
TemporalModel
```

and generates variable activity according to the configured behavior.

---

# 31. Event Generation

Event generation consumes:

```text
Session
Customer
Product context
TemporalModel
journey model
```

and produces an ordered behavioral sequence.

---

# 32. Entity Generators Should Be Narrow

A generator should answer one primary question.

For example:

```text
CustomerGenerator
→ how is a Customer generated?

OrderGenerator
→ how is an Order generated?
```

It should not answer:

```text
how is a Shipment generated?
```

---

# 33. Domain Services

When an operation genuinely spans entities, use a focused domain service.

Examples:

```text
OrderItemAllocationService
ReturnEligibilityService
ShipmentSchedulingService
SessionJourneyService
```

These should have specific business names.

Avoid:

```text
RelationshipManager
DataManager
BusinessManager
```

as generic containers.

---

# 34. Orchestration vs Domain Service

The distinction is:

### Orchestrator

```text
what happens next?
```

### Domain service

```text
how does this business operation work?
```

For example:

```text
Orchestrator:
generate Order
then generate OrderItems
then generate Payment
```

while:

```text
ReturnEligibilityService:
determine whether an item can be returned
```

---

# 35. Application Services

An application service can represent a user-facing operation:

```text
GenerateDataset
```

It coordinates the workflow.

It should not contain detailed business rules.

---

# 36. Suggested Application Structure

Conceptually:

```text
application/
  GenerateDataset.scala
  GenerationApplication.scala
  GenerationRun.scala
  GenerationOrchestrator.scala
  PreflightService.scala
```

The exact package can be refined during implementation.

---

# 37. Orchestration API

A clean application-level API might eventually resemble:

```scala
trait DatasetGenerator {
  def generate(request: GenerationRequest): GenerationResult
}
```

This is a potential boundary, not a requirement to introduce an interface immediately.

---

# 38. Generation Request

A request can conceptually contain:

```text
profile
cardinality profile
scenario
seed
output settings
```

The application resolves it into effective configuration and a plan.

---

# 39. Generation Result

A result may eventually contain:

```text
run metadata
dataset metadata
statistics
validation result
output artifacts
```

A failed generation should return or propagate a meaningful failure.

---

# 40. Avoid Returning Huge Data Structures

At scale, a `GenerationResult` should not necessarily contain:

```text
all customers
all orders
all events
```

It should contain metadata/references to produced artifacts.

This aligns with the scalability architecture.

---

# 41. Streaming Generation Contract

The long-term flow should resemble:

```text
entity generator
→ Iterator[Entity]
→ output writer
→ statistics
→ validation
```

rather than:

```text
entity generator
→ Seq[Entity]
→ aggregate everything
```

---

# 42. Multiple Consumers

A generated stream may need to feed:

```text
writer
statistics
validation
```

A naive design may try to reuse one iterator multiple times.

Remember:

```text
Iterator is consumable
```

The architecture therefore needs an explicit strategy.

Options include:

```text
generate once and fan out
stream directly to writer with inline statistics
write then validate
```

The choice depends on scale and validation requirements.

---

# 43. Generation vs Validation Order

Validation can occur:

```text
during generation
```

for cheap local invariants, and:

```text
after generation
```

for global relationships/statistics.

A hybrid model is preferred.

---

# 44. Local Validation

Examples:

```text
Order.id non-empty
Order.customerId valid
Order total non-negative
```

These can be checked immediately.

---

# 45. Global Validation

Examples:

```text
all OrderItems reference existing Orders
distribution meets target
global duplicate detection
```

These may require output or indexed state.

---

# 46. Output Strategy

The orchestrator should depend on an output abstraction rather than a specific CSV implementation.

Conceptually:

```scala
outputWriter.write(...)
```

The domain remains independent of physical format.

---

# 47. Output Lifecycle

The application should coordinate:

```text
open output
→ generate
→ finalize output
→ validate artifacts
→ publish
```

A failed write must prevent successful completion.

---

# 48. Manifest Lifecycle

The manifest should be finalized only after the completion criteria are satisfied.

Conceptually:

```text
run starts
    ↓
working metadata
    ↓
generation
    ↓
validation
    ↓
output
    ↓
manifest finalization
    ↓
publish
```

---

# 49. Statistics Lifecycle

Statistics can be collected during generation where possible.

Examples:

```text
record count
sum
min
max
category counts
status counts
```

More expensive statistics may be computed afterward.

---

# 50. Observability Lifecycle

Observability should surround major phases:

```text
phase started
phase progress
phase completed
phase failed
```

It should not force business generators to know logging implementation details.

---

# 51. Failure Boundary

The application boundary should catch only errors for which it can add useful handling.

For example:

```text
log run failure
mark run failed
cleanup temporary output
return non-success outcome
```

It should not swallow unexpected failures.

---

# 52. Partial Output

The orchestration layer should coordinate temporary output.

Conceptually:

```text
run workspace
    ↓
entity files
    ↓
validation
    ↓
artifact finalization
    ↓
publish
```

A failed run should not appear complete.

---

# 53. Phase-Level Transactions

Generation itself is not a database transaction.

Do not pretend that:

```text
customer generation
```

can be rolled back like a SQL transaction.

Instead, use artifact lifecycle management:

```text
temporary
→ validated
→ published
```

---

# 54. Idempotency

Running:

```text
same request
same seed
same reference data
```

should produce equivalent logical output.

This requires:

```text
deterministic random streams
stable ordering
stable reference data
stable configuration
```

---

# 55. Re-running a Failed Phase

Future sharding may allow:

```text
failed shard
→ regenerate shard
```

without regenerating the entire dataset.

The orchestration architecture should not prevent this.

---

# 56. Configuration Immutability

Once the run starts:

```text
effective configuration
```

should be immutable.

Do not allow generators to mutate global configuration.

---

# 57. Scenario Immutability

The effective scenario should also be fixed for the run.

A campaign should not randomly appear/disappear because a shared mutable scenario object changed.

---

# 58. Randomness Boundary

The orchestrator owns creation of the root deterministic random context.

Domain components receive derived streams.

This prevents hidden random-state coupling.

---

# 59. Generation Context Boundary

A useful rule:

```text
Application constructs context.
Domain generators consume context.
Generators do not mutate global context.
```

---

# 60. Progress

The orchestrator is the correct layer to report:

```text
customers complete
products complete
orders complete
events complete
```

because it knows phase boundaries.

Individual domain generators can optionally report internal progress through an observability abstraction.

---

# 61. Concurrency

The orchestrator should eventually understand which phases can execute concurrently.

For example:

```text
load Category ─┐
load Brand ────┼→ Product
load Geography ┘
```

But it should not parallelize a phase whose business dependencies are not ready.

---

# 62. Concurrency Should Be Explicit

Avoid hidden concurrency inside:

```text
CustomerGenerator
```

or:

```text
ProductGenerator
```

unless that component's contract explicitly requires it.

Application-level parallelism is easier to observe and control.

---

# 63. Thread Pool Ownership

If parallel generation is introduced, application infrastructure should own the execution context/thread pool.

Domain generators should not create their own thread pools.

---

# 64. Bounded Concurrency

Parallelism should be bounded.

Do not create:

```text
one thread per entity
```

or:

```text
one thread per file
```

without limits.

---

# 65. Backpressure

When output becomes the bottleneck:

```text
generation
→ bounded queue
→ writer
```

may be required.

The orchestration layer can coordinate this while preserving domain independence.

---

# 66. Application vs Infrastructure

Application orchestration may depend on:

```text
OutputWriter
Clock
Observability
```

but domain models should not depend on:

```text
filesystem
console
thread pools
```

---

# 67. Testing Orchestration

Application orchestration should have tests for:

```text
correct phase order
dependency enforcement
failure propagation
publication rules
scenario selection
plan usage
```

---

# 68. Unit Testing the Orchestrator

Use focused test doubles for:

```text
CustomerGenerator
ProductGenerator
Validator
Writer
```

to test sequencing.

The test should not regenerate millions of records.

---

# 69. Integration Testing

Integration tests should verify:

```text
real configuration
real reference data
real generators
real writer
```

for representative small datasets.

---

# 70. End-to-End Testing

The existing E2E test/run should remain important.

It verifies that:

```text
configuration
→ generation
→ validation
→ output
```

works as one system.

---

# 71. Failure Tests

Test:

```text
invalid configuration
missing reference data
generation failure
validation failure
output failure
```

and verify correct final run status.

---

# 72. Deterministic Test

A strong orchestration test:

```text
run(seed=42)
run(seed=42)
```

should produce equivalent logical output.

---

# 73. Scenario Test

A scenario test should verify that:

```text
scenario selection
```

actually reaches the relevant behavior strategies.

It should not merely verify that a string was accepted.

---

# 74. Dependency Test

For example:

```text
OrderItem generation
```

must not begin before:

```text
Order generation
```

or required product/customer dependencies are ready.

---

# 75. Performance Test

The orchestrator should eventually support benchmark runs that measure:

```text
phase timing
throughput
memory
output
```

without changing business behavior.

---

# 76. SOLID

## Single Responsibility

The orchestrator coordinates.

The generator generates.

The writer writes.

The validator validates.

## Open/Closed

New entity phases can be integrated without rewriting every existing domain generator.

## Liskov Substitution

Alternative generator strategies must preserve their contracts.

## Interface Segregation

Avoid one universal:

```text
GeneratorComponent
```

interface requiring unrelated methods.

## Dependency Inversion

Application orchestration should depend on focused abstractions where implementation substitution is genuinely useful.

---

# 77. Strategy Pattern

Good candidates:

```text
GenerationPlanBuilder
OutputWriter
ProductSelectionModel
CustomerBehaviorModel
TemporalModel
```

when alternative implementations are real.

The orchestrator should not become a collection of Strategy-specific business rules.

---

# 78. Factory Pattern

Factories should be used during bootstrap to construct:

```text
behavior strategies
distribution models
scenario
output writer
```

They should not be invoked inside hot loops.

---

# 79. Template Method

A generic:

```text
AbstractEntityGenerator
```

with a fixed inheritance lifecycle should not be introduced automatically.

Entity generation differs significantly across domains.

Composition is generally clearer.

---

# 80. Builder

The generation pipeline may eventually have a complex configuration object.

However, typed configuration and case classes are sufficient until construction becomes genuinely complex.

Do not create a builder solely because the pattern exists.

---

# 81. Readability

The orchestration code should read like the business workflow.

Preferred:

```scala
val referenceData = referenceLoader.load()
referenceValidator.validate(referenceData)

val plan = planBuilder.build(request, referenceData)
planValidator.validate(plan)

val customers = customerGenerator.generate(plan, context)
val products = productGenerator.generate(plan, context)

val orders = orderGenerator.generate(
  customers,
  products,
  context
)
```

This communicates the generation story.

---

# 82. Avoid Technical Noise

Avoid orchestration like:

```scala
val a = manager.handle(...)
val b = service.process(...)
val c = helper.execute(...)
```

without business meaning.

The names should communicate:

```text
load
validate
build
generate
write
publish
```

---

# 83. Explicit Dependencies

Prefer:

```scala
orderGenerator.generate(
  customers = customers,
  products = products,
  context = context
)
```

over hiding dependencies inside:

```scala
generationContext.get(...)
```

where possible.

Explicit dependencies improve readability.

---

# 84. Generation Pipeline Stages

A mature pipeline can be organized into:

```text
1. Bootstrap
2. Preflight
3. Reference preparation
4. Plan creation
5. Behavior-model construction
6. Domain generation
7. Relationship completion
8. Validation
9. Statistics
10. Output finalization
11. Manifest
12. Publication
```

Not every entity needs a separate global stage.

---

# 85. Relationship Completion

Some relationships can be established during generation.

Others may need a dedicated domain service.

The orchestrator should know only that the relationship contract must be satisfied before relevant validation.

---

# 86. CustomerBehaviorProfile

Customer behavior is a key shared dependency.

The orchestration architecture should allow:

```text
CustomerGenerator
→ creates CustomerBehaviorProfile

downstream generators
→ consume stable behavior profile
```

without reconstructing customer behavior independently.

---

# 87. Behavior Profile Ownership

The Customer domain owns the meaning of the profile.

The application orchestrator owns its lifecycle:

```text
create
→ retain/retrieve
→ provide to downstream generation
```

---

# 88. Avoid Recomputing Behavior

Do not independently sample:

```text
customer purchase frequency
```

in Order generation and:

```text
customer activity
```

in Session generation if these are intended to be correlated.

The shared profile should provide common latent behavior.

---

# 89. Data Flow

The target business data flow is:

```text
CustomerBehaviorProfile
        |
        +---- Customer
        |
        +---- Session
        |
        +---- Order
        |
        +---- Payment
        |
        +---- Return
```

Additional catalog/time context joins this behavior.

---

# 90. Temporal Context

The orchestration layer should provide a temporal model/context to relevant generators.

It should not calculate business timestamps itself.

---

# 91. Scenario Context

Similarly:

```text
scenario
```

should resolve into executable behavior before generation.

Generators should not repeatedly inspect raw scenario strings.

---

# 92. Effective Runtime Model

A useful conceptual bootstrap result:

```text
GenerationRuntime
  effectiveConfig
  generationPlan
  referenceData
  behaviorModels
  temporalModel
  scenarioModel
  randomContext
```

This is an internal application composition object, not necessarily a public API.

---

# 93. Runtime Object Lifetime

Long-lived runtime components should be created once per generation run.

Examples:

```text
reference data
behavior models
temporal model
selection indexes
```

Short-lived records should be created during generation.

---

# 94. Avoid Global Singletons

Avoid:

```scala
object GlobalConfig
object GlobalRandom
object GlobalCatalog
```

because they make:

```text
tests
parallelism
reproducibility
```

harder.

---

# 95. Run Isolation

Two generation runs in the same JVM should ideally have isolated:

```text
configuration
randomness
scenario
reference snapshot
output
statistics
```

This is useful for tests and future batch orchestration.

---

# 96. Multiple Runs

A future application may execute:

```text
baseline
skew
dirty
```

runs sequentially or concurrently.

Run-specific state must not leak between them.

---

# 97. Failure Isolation

If one run fails:

```text
run A → FAILED
run B → SUCCESS
```

the failure of A should not corrupt B's state.

This strongly favors immutable/run-scoped dependencies.

---

# 98. Output Isolation

Each run should have a unique output workspace.

For example:

```text
output/
  run-001/
  run-002/
```

or dataset-specific identities.

The exact path convention belongs to the output architecture.

---

# 99. Application Logging

The application boundary should log:

```text
run started
configuration/profile/scenario
planned counts
phase progress
run completed
run failed
```

Detailed domain diagnostics remain in their appropriate components.

---

# 100. Observability Events

A future structured event stream could expose:

```text
GenerationStarted
PhaseStarted
PhaseCompleted
ValidationCompleted
OutputCompleted
GenerationFailed
GenerationCompleted
```

This is optional until multiple observability consumers exist.

---

# 101. Metrics

Useful application metrics:

```text
records generated
records written
phase duration
validation duration
output duration
throughput
```

The orchestrator can coordinate these measurements.

---

# 102. Progress Accuracy

Progress should represent actual work.

Avoid:

```text
fake progress percentages
```

based only on elapsed time.

Where possible use:

```text
generated / planned
```

or phase-specific work units.

---

# 103. Large-Scale Orchestration

At xlarge scale, orchestration should avoid:

```text
collect all entities in memory
```

and instead coordinate streaming phases.

This is where the performance architecture meets the pipeline architecture.

---

# 104. Streaming Pipeline

A future high-scale flow:

```text
Customer generation
      ↓
Customer output/index
      ↓
Order generation
      ↓
OrderItem generation
      ↓
Payment/Shipment
      ↓
Validation/statistics
      ↓
finalization
```

Some dependencies may require durable intermediate artifacts or compact indexes.

---

# 105. Intermediate State

Intermediate state should be introduced only when required.

Examples:

```text
customer behavior profiles
product indexes
order IDs
```

The architecture should favor compact representations over retaining complete records.

---

# 106. Intermediate Artifacts

If intermediate artifacts are persisted:

```text
they are implementation artifacts
```

not automatically part of the final dataset contract.

The manifest should distinguish them from published entities.

---

# 107. Orchestration and Reference Snapshots

Reference data should be frozen before generation.

The orchestrator should pass the same snapshot to all dependent phases.

This prevents:

```text
Product phase sees catalog A
OrderItem phase sees catalog B
```

within one run.

---

# 108. Orchestration and Validation

Validation should receive the same run metadata and effective configuration used for generation.

Otherwise validation may incorrectly assume different scenario semantics.

---

# 109. Orchestration and Statistics

Statistics should describe the actual generated dataset, not only requested targets.

For example:

```text
planned orders = 5,000
actual orders = 5,000
mean items/order = 2.41
```

This should feed the final manifest.

---

# 110. Requested vs Actual

The run summary should distinguish:

```text
requested
planned
generated
written
validated
published
```

These are not always identical.

---

# 111. Cancellation

Future cancellation should be coordinated by the application layer.

Generators should periodically honor cancellation without owning process termination.

---

# 112. Resource Cleanup

The application boundary should coordinate:

```text
writer close
temporary output cleanup
thread-pool shutdown
resource release
```

where relevant.

---

# 113. Error Semantics

The orchestration layer should preserve the error-handling architecture:

```text
configuration failure
→ fail preflight

generation failure
→ fail run

validation failure
→ block publication

output failure
→ block completion
```

---

# 114. No Silent Recovery

The orchestrator must not do:

```text
Product generation failed
→ generate fewer products
→ continue
```

unless adaptive behavior is explicitly part of the plan.

---

# 115. Application Outcome

The final application outcome should clearly distinguish:

```text
SUCCESS
FAILED
CANCELLED
```

The exact API can evolve.

The semantics must remain explicit.

---

# 116. Current Implementation vs Target

The current project already has:

- `Main`;
- `ConfigLoader`;
- generation planning;
- `GenerationPipeline`;
- entity generators;
- validation;
- output;
- statistics;
- observability-related structure.

The target architecture refines these into explicit application boundaries and dependency-aware phases.

The current implementation should be migrated incrementally rather than rewritten blindly.

---

# 117. Migration Strategy

Recommended sequence:

```text
1. document current GenerationPipeline
2. separate bootstrap from generation logic
3. make application composition explicit
4. formalize GenerationRequest/Run concepts where useful
5. centralize preflight
6. formalize dependency order
7. isolate domain generators
8. introduce behavior-model construction
9. separate validation/statistics/output orchestration
10. introduce temporary publication lifecycle
11. add phase-level observability
12. introduce streaming
13. introduce deterministic parallelism
```

---

# 118. Do Not Rewrite Everything at Once

The current system works.

Therefore migration should preserve the passing baseline:

```text
229 tests
```

and the existing successful end-to-end generation.

Each architectural refactor should leave the system buildable and testable.

---

# 119. One-File-at-a-Time Migration

The implementation should be migrated incrementally.

For each file:

```text
inspect
→ modify
→ compile
→ test
→ run if needed
→ proceed
```

This minimizes the blast radius of architectural changes.

---

# 120. Orchestration Quality Gate

Before considering the application pipeline mature:

```text
□ Main is thin
□ bootstrap is explicit
□ dependencies are constructor-injected
□ configuration is resolved before generation
□ preflight occurs before expensive work
□ generation dependencies are explicit
□ domain generators remain narrow
□ cross-domain business operations use focused services
□ orchestrator coordinates rather than implements business rules
□ validation has a clear place in the lifecycle
□ output publication is explicit
□ manifest represents completion
□ failures cannot silently become success
□ run state is observable
□ randomness is run-scoped and deterministic
□ independent phases can eventually be parallelized
□ streaming can be introduced without redesigning domains
```

---

# 121. Definition of Done

The generation pipeline architecture is complete when:

### Business clarity

The orchestration reads like the ShopSphere generation workflow.

### Dependency correctness

Entities are generated only after their required dependencies exist.

### Isolation

Domain generators do not own application lifecycle concerns.

### Testability

Phases can be tested independently.

### Failure safety

A failed run cannot be published as successful.

### Reproducibility

A run has isolated deterministic configuration and randomness.

### Scalability

The orchestration can evolve from in-memory generation toward streaming and sharding.

### Extensibility

New entities and scenarios can be integrated without turning the orchestrator into a god object.

---

# 122. Explicit Architecture Decisions

### Decision 1

The application entry point remains thin.

### Decision 2

Application bootstrap is the composition root.

### Decision 3

Dependency injection will use constructor/method injection rather than a DI framework.

### Decision 4

Entity generators own entity behavior; orchestration owns workflow sequencing.

### Decision 5

Generation dependencies should be explicit.

### Decision 6

The generation process should be represented conceptually as a dependency DAG.

### Decision 7

Preflight validation occurs before expensive generation.

### Decision 8

Domain services are used for focused cross-entity business operations.

### Decision 9

A generic RelationshipManager or BusinessManager should not become the orchestration mechanism.

### Decision 10

The orchestrator should not parse raw configuration or implement detailed business rules.

### Decision 11

Generation context should remain small and should not become a service locator.

### Decision 12

Run-specific configuration, randomness, scenario, and reference data should be isolated.

### Decision 13

Validation, statistics, output, and observability remain distinct concerns coordinated by the application layer.

### Decision 14

Successful publication requires successful completion of the required lifecycle phases.

### Decision 15

The pipeline must be able to evolve toward streaming and deterministic parallel generation.

### Decision 16

The current passing baseline should be preserved throughout migration.

### Decision 17

Architectural migration should proceed incrementally, with one-file-at-a-time changes and verification after each meaningful change.

---

# 123. Final Design Summary

The target application architecture is:

```text
                         Main
                          |
                          v
                    Application Bootstrap
                          |
            +-------------+-------------+
            |             |             |
      Configuration   Reference Data   Runtime
            |             |             |
            +-------------+-------------+
                          |
                          v
                    Preflight
                          |
                          v
                  Effective Generation
                        Plan
                          |
                          v
                 Generation Runtime
                          |
        +-----------------+------------------+
        |                 |                  |
  Behavior Models   Temporal Model     Random Context
        |                 |                  |
        +-----------------+------------------+
                          |
                          v
                  Generation Orchestrator
                          |
          +---------------+----------------+
          |               |                |
      Customer         Product         Geography
          |               |                |
          +---------------+----------------+
                          |
                       Orders
                          |
             +------------+------------+
             |            |            |
         OrderItems     Payment     Shipment
                                      |
                                    Return
                          |
                    Sessions / Events
                          |
                          v
                    Validation
                          |
                          v
                    Statistics
                          |
                          v
                    Output Finalize
                          |
                          v
                       Manifest
                          |
                          v
                      Publish
```

The most important distinction is:

```text
GENERATOR
    knows how to generate its domain

ORCHESTRATOR
    knows when and why generation phases run

APPLICATION
    knows how the complete run starts, succeeds, fails, and publishes
```

That separation gives ShopSphere a clean path from the current working implementation toward the sophisticated generator described by the preceding architecture documents.

The orchestration layer should remain intentionally boring.

That is a positive design outcome.

When the orchestration code reads approximately like:

```text
load
validate
plan
generate
validate
write
finalize
publish
```

while the detailed business behavior lives inside focused domain components, the architecture is doing its job.

The next implementation stages can then improve realism, streaming, performance, and parallelism without turning the entire application into one interconnected generator.
