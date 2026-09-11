# 15 — Development Roadmap

## 1. Purpose

This document defines the implementation roadmap for the `ecommerce-data-generation` platform.

The roadmap translates the project architecture and design decisions into an incremental delivery sequence.

The objective is not to build every planned capability immediately. The objective is to establish a correct foundation first and then progressively add realism, relationships, configurability, reproducibility, scenarios, scale, and performance capabilities.

The roadmap follows the project's guiding priority:

```text
correctness
    ↓
realism
    ↓
relationships
    ↓
configurability
    ↓
reproducibility
    ↓
scale
    ↓
scenarios
    ↓
generation performance
```

---

## 2. Development Strategy

Development should proceed vertically rather than creating many incomplete subsystems in parallel.

Each major phase should produce something executable and testable.

The preferred pattern is:

```text
design
  ↓
minimal implementation
  ↓
tests
  ↓
validation
  ↓
integration
  ↓
refinement
  ↓
next capability
```

A feature should not be considered complete merely because its classes or configuration exist.

It should be demonstrated through generated data and automated tests.

---

## 3. Phase Overview

The project is divided into eleven major implementation phases:

| Phase | Focus |
|---|---|
| 1 | Project foundation |
| 2 | Configuration and reproducibility |
| 3 | Reference data and foundational entities |
| 4 | Core transactional entities |
| 5 | Relationships and business consistency |
| 6 | Distributions, cardinality, and realism |
| 7 | Data-quality scenarios |
| 8 | Skew and performance scenarios |
| 9 | Validation and observability |
| 10 | Output/layout maturity and scale |
| 11 | Hardening and Spark Performance Laboratory readiness |

These phases build on one another.

---

# Phase 1 — Project Foundation

## Objective

Create the executable Scala application and establish the project structure.

### Work

Set up:

- Scala project
- build configuration
- application entry point
- package structure
- test framework
- basic logging
- configuration loading foundation
- source/test directory structure
- documentation structure

Recommended conceptual package boundaries:

```text
config
model
reference
distribution
generation
relationship
quality
scenario
validation
output
manifest
statistics
observability
```

The exact package structure can evolve as implementation experience accumulates.

### Deliverable

The application starts successfully and can execute a minimal command such as:

```text
generator --help
```

### Exit Criteria

- project builds
- tests execute
- application starts
- basic configuration can be loaded
- logging works
- no generator-specific business logic is required yet

---

# Phase 2 — Configuration and Reproducibility

## Objective

Make configuration and deterministic seed handling reliable before building the full dataset.

### Work

Implement:

- configuration model
- configuration parser
- defaults
- validation
- profile resolution
- seed configuration
- generation ID
- effective configuration representation
- controlled random source

Establish the distinction between:

```text
generation_id
seed
configuration
```

### Tests

Verify:

- valid configuration loads
- invalid configuration fails
- defaults work
- same seed produces equivalent random sequences
- different seeds produce different sequences
- configuration resolution is deterministic

### Deliverable

The application can load a configuration and establish a reproducible generation context.

### Exit Criteria

No downstream generator should need to invent its own random source or independently interpret configuration.

---

# Phase 3 — Reference Data and Foundational Entities

## Objective

Build the reference-data layer and simplest entities.

### Reference data

Implement foundational data for:

- geography
- categories
- brands
- product attributes where required

### Entities

Begin with:

- Customer
- Address
- Category
- Brand
- Product

The exact implementation order can follow dependency requirements.

### Work

Establish:

- entity models
- deterministic identifiers
- reference-data lookup
- basic entity generators
- entity-level statistics

### Tests

Verify:

- schema correctness
- identifier behavior
- uniqueness where required
- configured cardinality
- valid references
- basic distributions

### Deliverable

A small configuration can generate a valid master-data dataset.

---

# Phase 4 — Core Transactional Entities

## Objective

Introduce transactional data with controlled lifecycle relationships.

### Entities

Implement:

- Order
- OrderItem
- Payment
- Shipment
- Return

Then introduce:

- Session
- Event

### Work

Implement:

- order generation
- order-item generation
- payment generation
- shipment generation
- return generation
- session generation
- event generation
- basic temporal logic

### Tests

Verify:

- row counts
- identifiers
- foreign keys
- quantity and price rules
- derived totals
- lifecycle ordering
- session/event relationships

### Deliverable

The application can generate a complete small e-commerce transaction dataset.

---

# Phase 5 — Relationships and Business Consistency

## Objective

Move from independently generated entities to a coherent relational dataset.

### Work

Implement the relationship-management strategy defined in the architecture:

```text
Customer
   ↓
Order
   ↓
OrderItem
   ↓
Product
```

and:

```text
Order
 ├── Payment
 ├── Shipment
 └── Return
```

and:

```text
Customer
   ↓
Session
   ↓
Event
```

### Business rules

Enforce:

- valid foreign keys
- order/item consistency
- payment/order consistency
- shipment/order temporal consistency
- return-window consistency
- product/category/brand consistency
- session/event temporal consistency

### Tests

Add:

- relationship integration tests
- business-rule tests
- temporal tests
- negative validation tests

### Deliverable

The generated dataset behaves like one coherent e-commerce system rather than a collection of unrelated tables.

---

# Phase 6 — Distributions, Cardinality, and Realism

## Objective

Transform structurally correct data into realistic synthetic data.

### Work

Implement the distribution engine for:

- categorical distributions
- numeric distributions
- date/time distributions
- long-tail popularity
- configurable cardinality
- segment-dependent behavior

Introduce meaningful correlations such as:

```text
customer segment
    → purchasing behavior

acquisition channel
    → customer behavior

product popularity
    → order-item frequency

category
    → product characteristics
```

### Cardinality

Support:

```text
low
medium
high
```

and explicit cardinality controls where required.

### Tests

Use:

- statistical tests
- property-based tests
- cardinality tests
- correlation/relationship tests

### Deliverable

The baseline dataset has realistic distributions rather than uniform random values.

---

# Phase 7 — Data-Quality Scenarios

## Objective

Add controlled imperfections without compromising the ability to reason about the dataset.

### Profiles

Implement:

```text
clean
slightly_dirty
dirty
```

### Quality dimensions

Support configured rates for appropriate fields, such as:

- nulls
- malformed values
- duplicates
- inconsistent values
- invalid references where intentionally modeled
- other documented data-quality defects

### Design requirement

The quality engine must be explicit.

Conceptually:

```text
clean logical record
        ↓
data-quality rules
        ↓
intentionally imperfect record
```

### Tests

Verify:

- configured violations occur approximately at the requested rate
- clean profiles remain clean
- unrelated invariants remain intact where required
- dirty scenarios are reproducible
- validation correctly identifies intentional violations

### Deliverable

The generator can create both clean training data and controlled data-quality challenge datasets.

---

# Phase 8 — Skew and Performance Scenarios

## Objective

Introduce deliberate workload characteristics for downstream Spark learning.

### Skew

Support:

- customer skew
- product skew
- configurable hot-entity concentration
- skew disabled by default

### Physical workload scenarios

Introduce configuration combinations representing:

- few large files
- moderate file counts
- many small files

### Scenario model

A scenario should be able to compose existing dimensions rather than duplicating generator logic.

Conceptually:

```text
baseline
  +
high cardinality
  +
high product skew
  +
many small files
```

### Tests

Verify:

- skew concentration
- hot-entity selection
- reproducibility
- referential integrity
- physical file counts
- scenario configuration

### Deliverable

The generator can create controlled Spark performance workloads rather than only generic datasets.

---

# Phase 9 — Validation and Observability

## Objective

Make generation self-validating and operationally transparent.

### Validation

Implement the validation layers defined in the validation strategy:

- configuration
- schema
- row counts
- cardinality
- referential integrity
- business consistency
- temporal consistency
- distributions
- data quality
- skew
- reproducibility
- output
- manifest

### Observability

Implement:

- generation lifecycle logging
- progress
- entity timing
- throughput
- output statistics
- validation summaries
- failure diagnostics
- effective configuration capture

### Manifest

Finalize the generation manifest with:

- generation ID
- seed
- profile
- scenario
- configuration
- date range
- entity statistics
- file statistics
- validation results
- distribution statistics
- data-quality statistics
- generator version

### Deliverable

Every successful generation should explain what it generated and demonstrate that it satisfies its configured contract.

---

# Phase 10 — Output/Layout Maturity and Scale

## Objective

Make the physical dataset suitable for larger experiments.

### Work

Harden:

- streaming generation
- chunked writing
- configurable file counts
- deterministic file assignment
- large output handling
- temporary-output/finalization behavior
- output validation

Expand scale profiles:

```text
small
medium
large
xlarge
```

### Performance work

Measure:

- records/second
- bytes/second
- generation duration
- validation duration
- memory usage
- output throughput

### Physical scenarios

Support controlled experiments involving:

```text
one large file
several medium files
many small files
```

### Deliverable

The generator can produce datasets large enough to become meaningful inputs to Spark performance experiments.

---

# Phase 11 — Hardening and Spark Performance Laboratory Readiness

## Objective

Turn the generator into a reliable dataset-production platform for the next stage of the project.

### Hardening

Review:

- correctness
- reproducibility
- configuration
- relationships
- distributions
- data quality
- skew
- validation
- output
- observability
- failure handling

### Test hardening

Run:

- complete unit suite
- property-based suite
- integration suite
- regression suite
- representative scenario matrix
- large-scale tests
- performance benchmarks

### Documentation

Ensure that:

- configuration examples are usable
- scenarios are documented
- output structure is documented
- manifest format is documented
- reproducibility behavior is documented
- failure behavior is documented

### Deliverable

A user should be able to:

```text
select a scenario
      ↓
select a scale
      ↓
select cardinality
      ↓
select quality/skew behavior
      ↓
select output layout
      ↓
provide a seed
      ↓
generate dataset
      ↓
validate dataset
      ↓
inspect manifest
      ↓
use dataset in Spark
```

without manually repairing or interpreting the generated data.

---

# 4. Milestone Definitions

## Milestone M1 — Executable Foundation

Completed when:

- Scala application builds
- tests run
- configuration loads
- seed is controlled

---

## Milestone M2 — Valid Master Data

Completed when:

- customer/address data works
- product/category/brand data works
- identifiers and references are valid
- basic validation exists

---

## Milestone M3 — Complete E-Commerce Dataset

Completed when:

- orders work
- order items work
- payments work
- shipments work
- returns work
- sessions work
- events work
- cross-entity relationships are valid

---

## Milestone M4 — Realistic Dataset

Completed when:

- distributions are configurable
- cardinality is configurable
- long-tail behavior exists
- important correlations exist
- statistical validation exists

---

## Milestone M5 — Scenario Dataset

Completed when:

- clean/slightly_dirty/dirty profiles work
- skew scenarios work
- scenario composition works
- scenario behavior is validated

---

## Milestone M6 — Production-Quality Generator

Completed when:

- output is deterministic
- manifest is complete
- validation is comprehensive
- observability is useful
- failures are diagnosable
- large datasets can be generated reliably

---

## Milestone M7 — Spark Performance Laboratory Ready

Completed when:

- physical file layout is configurable
- large and small-file scenarios are reproducible
- skew workloads are reproducible
- dataset characteristics are documented
- benchmark metadata is available
- generation performance is understood

---

# 5. Recommended Implementation Order Inside Each Phase

Within each phase, use the following sequence:

```text
1. define model/contract
2. write focused tests
3. implement smallest working behavior
4. validate generated output
5. integrate with existing pipeline
6. add observability
7. add configuration
8. add scenario combinations
9. benchmark where appropriate
10. document final behavior
```

This reduces the risk of building large amounts of unverified generator code.

---

# 6. Definition of Done

A feature is complete only when all applicable criteria are satisfied.

### Functional

- requested behavior is implemented
- configuration is supported
- generated data is usable

### Correctness

- business rules hold
- relationships hold
- validation passes
- edge cases are handled

### Determinism

- seed behavior is documented
- reproducibility tests pass
- nondeterministic implementation behavior is controlled

### Testing

- unit tests exist
- integration tests exist where needed
- negative tests exist where applicable
- regression coverage exists for discovered defects

### Observability

- meaningful logs exist
- relevant statistics are available
- failures are diagnosable

### Output

- physical output follows the contract
- manifest is accurate
- output validation passes

### Documentation

- behavior is documented
- configuration is documented
- important tradeoffs are recorded

---

# 7. Development Discipline

The project should avoid premature optimization.

Do not optimize generation performance before correctness is established.

Do not add advanced output formats before the CSV pipeline is reliable.

Do not introduce complex monitoring infrastructure before basic observability is sufficient.

Do not create scenario-specific generator implementations when the behavior can be expressed through existing configurable dimensions.

Prefer:

```text
one robust abstraction
+
configuration
+
composition
```

over:

```text
many duplicated scenario-specific implementations
```

---

# 8. Change Management

When a new requirement is discovered, determine first which design layer it belongs to:

```text
logical model
configuration
generation strategy
distribution
relationship
data quality
skew/scenario
validation
output
observability
```

Then update the corresponding decision document before or alongside implementation when the change affects an architectural contract.

Important architectural changes should also be recorded in the ADR log.

---

# 9. Risk-Based Prioritization

If implementation time becomes constrained, prioritize in this order:

### Critical

- schema correctness
- referential integrity
- business consistency
- deterministic seeds
- configuration validation
- output correctness
- validation

### High

- realistic distributions
- cardinality
- temporal behavior
- data-quality scenarios
- skew
- observability

### Medium

- advanced physical layouts
- performance optimization
- expanded statistics
- additional scenario combinations

### Deferred

- additional output formats
- cloud/object-storage backends
- external monitoring infrastructure
- advanced compression
- sophisticated orchestration

This protects the project's core educational and engineering value.

---

# 10. Suggested First Vertical Slice

Before implementing the entire model, establish one complete vertical slice.

Recommended slice:

```text
configuration
    ↓
seed
    ↓
Customer
    ↓
Product
    ↓
Order
    ↓
OrderItem
    ↓
CSV output
    ↓
validation
    ↓
manifest
```

The slice should support:

- deterministic generation
- configurable small row counts
- valid foreign keys
- basic distributions
- CSV output
- validation
- manifest generation

Once this path is reliable, additional entities can be added without changing the fundamental architecture.

---

# 11. Expansion Strategy

Expand the dataset in dependency order.

Recommended progression:

```text
Category / Brand
       ↓
Product
       ↓
Customer / Address
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
       ↓
Session
       ↓
Event
```

This ordering is not mandatory if implementation dependencies suggest a different sequence, but the underlying principle is:

> Build referenced entities before entities that depend on them.

---

# 12. Architecture Stability Checkpoints

After major phases, pause and reassess the architecture.

### After Phase 3

Check:

- entity model
- reference-data approach
- identifier strategy
- configuration boundaries

### After Phase 5

Check:

- relationship strategy
- business-rule placement
- generator dependencies

### After Phase 6

Check:

- distribution abstraction
- cardinality model
- correlation strategy

### After Phase 8

Check:

- scenario composition
- skew abstraction
- physical-layout configuration

### After Phase 11

Check:

- performance bottlenecks
- extension points
- future Spark integration
- maintainability

Architectural changes discovered at these checkpoints should be documented rather than silently introduced.

---

# 13. Expected Evolution

The generator is intentionally expected to evolve through stages.

### Stage 1

```text
correct synthetic data
```

### Stage 2

```text
realistic synthetic data
```

### Stage 3

```text
configurable synthetic data
```

### Stage 4

```text
scenario-driven synthetic data
```

### Stage 5

```text
performance-oriented dataset generator
```

### Stage 6

```text
reproducible dataset laboratory
```

The final stage should support repeatable experiments where changes in Spark workload behavior can be attributed to explicit dataset dimensions.

---

# 14. Long-Term Direction

The final generator should become more than a one-off data-generation program.

It should function as a controlled dataset laboratory with independent dimensions such as:

```text
Scale
Cardinality
Distribution
Correlation
Data Quality
Skew
Temporal Range
File Count
File Size
Output Format
```

These dimensions should be composable.

For example:

```text
Scenario:
  scale = large
  cardinality = high
  product_skew = high
  data_quality = clean
  file_count = 500
  output_format = csv
  seed = 42
```

This should produce a dataset whose characteristics can be measured, reproduced, and used as a controlled input to downstream Spark experiments.

---

# 15. Final Definition of the Project

The project should ultimately provide this contract:

```text
Input
  = configuration + scenario + seed

Process
  = deterministic synthetic-data generation
    + relationships
    + realism
    + controlled imperfections
    + controlled skew
    + validation
    + observability

Output
  = dataset files
    + manifest
    + statistics
    + validation result
```

A generated dataset should therefore be treated as an experiment artifact, not merely as a collection of random CSV files.

---

# 16. Final Guiding Principle

> **Build the simplest correct generator first, then progressively add realism, configurability, scale, scenarios, and performance without weakening reproducibility or correctness.**

The roadmap is complete when the project can reliably transform an explicit configuration and seed into a validated, observable, reproducible e-commerce dataset that is suitable for systematic Spark performance experimentation.
