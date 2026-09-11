# Architecture Decision Record (ADR) Log

## Project

`ecommerce-data-generation`

## Purpose

This document is the chronological index of significant architectural and engineering decisions made for the E-Commerce Data Generation Platform.

The detailed reasoning for major decisions is maintained in the corresponding decision documents.

This log exists so that we can answer, in the future:

```text
What did we decide?
Why did we decide it?
When did we decide it?
Has that decision changed?
What replaced it?
```

---

# ADR-001 — Scala as the Implementation Language

**Status:** Accepted

**Decision:** Build the generator in Scala.

**Reasoning:** The generator is the foundation of a broader Spark learning program, and Scala provides continuity with the later Spark projects.

**Related document:** `02-technology-decisions.md`

---

# ADR-002 — sbt as the Build Tool

**Status:** Accepted

**Decision:** Use sbt for project build and dependency management.

**Reasoning:** It provides the required Scala project workflow without introducing unnecessary build infrastructure.

**Related document:** `02-technology-decisions.md`

---

# ADR-003 — Do Not Use Spark for the Initial Generator

**Status:** Accepted

**Decision:** The initial generator is a normal Scala/JVM application and does not use Spark as its core execution engine.

**Reasoning:** Data generation and Spark processing experiments are separate responsibilities. Keeping them separate creates a controlled experimental environment.

**Related document:** `01-architecture-decisions.md`

---

# ADR-004 — Configuration-Driven Generation

**Status:** Accepted

**Decision:** Major dataset characteristics must be controlled through configuration rather than generation-code changes.

**Reasoning:** The same business model must support different volumes, cardinalities, distributions, data-quality conditions, and experimental scenarios.

**Related document:** `03-configuration-decisions.md`

---

# ADR-005 — Deterministic Seed

**Status:** Accepted

**Decision:** Random generation must ultimately be controlled by a deterministic seed.

**Reasoning:** Reproducible input datasets are required for meaningful Spark performance experiments.

**Related document:** `01-architecture-decisions.md`

---

# ADR-006 — Incremental Architecture

**Status:** Accepted

**Decision:** Implement the platform incrementally rather than implementing every subsystem at once.

**Reasoning:** This keeps early development understandable while allowing the architecture to evolve toward the intended component boundaries.

**Related document:** `01-architecture-decisions.md`

---

# ADR-007 — Separate Entity Generation Responsibilities

**Status:** Accepted

**Decision:** Major business entities should have separate generation responsibilities.

**Reasoning:** Independent entity generators make the domain easier to evolve while preserving relationships through shared generation infrastructure.

**Related document:** `04-data-model-decisions.md`

---

# ADR-008 — Relationships Are First-Class

**Status:** Accepted

**Decision:** Generated entities must have meaningful business relationships rather than being independently random tables.

**Reasoning:** Later Spark experiments require realistic relational joins and business-consistent data.

**Related document:** `04-data-model-decisions.md`

---

# ADR-009 — Realistic Distributions

**Status:** Accepted

**Decision:** Important business attributes should use appropriate non-uniform statistical distributions instead of default uniform randomness.

**Reasoning:** Real e-commerce systems contain long-tail product popularity, different customer activity levels, varied order values, and other non-uniform behavior.

**Related document:** `01-architecture-decisions.md`

**Note:** The exact distribution models are not yet finalized.

---

# ADR-010 — Behavioral Correlation

**Status:** Accepted

**Decision:** Related business behavior should be probabilistically correlated.

**Reasoning:** Customer activity, purchasing behavior, spending, category preference, and returns should exhibit realistic relationships without becoming deterministic rules.

**Related document:** `04-data-model-decisions.md`

---

# ADR-011 — Clean Data by Default

**Status:** Accepted

**Decision:** The default dataset is clean. Data-quality defects are introduced deliberately through configuration/scenarios.

**Reasoning:** Clean baseline datasets are necessary for controlled experiments, while dirty data is itself an experimental condition.

**Related document:** `01-architecture-decisions.md`

---

# ADR-012 — Skew Is Explicitly Controlled

**Status:** Accepted

**Decision:** Data skew is disabled by default and enabled through explicit scenarios.

**Reasoning:** Later Spark skew experiments need a controlled comparison between baseline and skewed workloads.

**Related document:** `01-architecture-decisions.md`

---

# ADR-013 — Profiles Are Presets

**Status:** Accepted

**Decision:** `small`, `medium`, `large`, and `xlarge` are convenience presets rather than fixed system limits.

**Reasoning:** Standard workloads are useful for repeated experiments, but users must retain the ability to customize exact dataset characteristics.

**Related document:** `03-configuration-decisions.md`

---

# ADR-014 — Scenarios Must Not Require Source-Code Changes

**Status:** Accepted

**Decision:** Experimental scenarios are selected through configuration rather than code modifications.

**Reasoning:** Experimental conditions must be reproducible and independent of changes to entity-generation logic.

**Related document:** `03-configuration-decisions.md`

---

# ADR-015 — Validation Is a First-Class Capability

**Status:** Accepted

**Decision:** Generated datasets must be validated for referential integrity, business consistency, and statistical characteristics.

**Reasoning:** Dataset size alone is insufficient. We must know whether the generated dataset is valid and whether its intended characteristics were achieved.

**Related document:** `04-data-model-decisions.md`

---

# ADR-016 — Generation Manifest

**Status:** Accepted

**Decision:** Every generation run should produce a generation manifest containing enough metadata to understand and reproduce the dataset.

**Reasoning:** The manifest becomes the audit record for later Spark experiments.

**Related document:** `01-architecture-decisions.md`

---

# ADR-017 — CSV as the Initial Raw Output

**Status:** Accepted

**Decision:** The first output format is CSV.

**Reasoning:** CSV is easy to inspect, validate, debug, and consume from later Spark projects.

**Related document:** `02-technology-decisions.md`

---

# ADR-018 — Defer Alternative Output Formats

**Status:** Accepted

**Decision:** Parquet, JSON, Avro, and other formats are deferred until there is a concrete requirement.

**Reasoning:** The first objective is correct and realistic data generation. File-format experimentation belongs primarily to later stages of the learning program.

**Related document:** `02-technology-decisions.md`

---

# ADR-019 — Correctness Before Generation Performance

**Status:** Accepted

**Decision:** Optimize the generator only after correctness, realism, relationships, configurability, reproducibility, and scale behavior are established.

**Reasoning:** Premature optimization could increase complexity while reducing confidence in the generated data.

**Related document:** `01-architecture-decisions.md`

---

# ADR-020 — Small Initial Technology Footprint

**Status:** Accepted

**Decision:** Keep the initial technology stack intentionally small.

**Initial direction:**

```text
Scala
sbt
HOCON-style configuration
JVM application
CLI
CSV
application logging
automated tests
```

**Reasoning:** Additional technologies should solve concrete problems rather than being introduced speculatively.

**Related document:** `02-technology-decisions.md`

---

# ADR-021 — Strongly Modeled Effective Configuration

**Status:** Accepted

**Decision:** Generation components should consume validated, strongly modeled configuration rather than repeatedly reading raw configuration values.

**Reasoning:** This separates configuration syntax from business logic and improves validation and testability.

**Related document:** `03-configuration-decisions.md`

---

# ADR-022 — Derived Business Values Should Be Derived

**Status:** Accepted

**Decision:** Values that logically depend on other generated values should generally be calculated from those values rather than generated independently.

Examples:

```text
OrderItem.line_total
    ← quantity + unit_price + discount

Order.order_total
    ← OrderItems

Payment.amount
    ← Order
```

**Reasoning:** Derived values preserve business consistency.

**Related document:** `04-data-model-decisions.md`

---

# ADR-023 — Internal Behavioral State Need Not Be Raw Data

**Status:** Accepted

**Decision:** Internal behavioral attributes may exist solely to drive realistic generation and do not automatically become columns in raw datasets.

Examples:

```text
activity_level
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
```

**Reasoning:** The internal generation model and external source-table schema serve different purposes.

**Related document:** `04-data-model-decisions.md`

---

# ADR-024 — Explicit Decision Change Policy

**Status:** Accepted

**Decision:** Existing architectural decisions must not be silently changed.

When a significant decision changes:

1. Create a new ADR.
2. Identify the old decision.
3. Mark the old decision as `Superseded`.
4. Explain why it changed.
5. Record the consequences.
6. Reference the replacement decision.

Example:

```text
ADR-003
Status: Superseded by ADR-025
```

---

# Decision Status Vocabulary

Use the following statuses consistently:

```text
Proposed
Accepted
Superseded
Rejected
Deprecated
```

## Proposed

A decision is under discussion and has not yet been accepted.

## Accepted

The decision is currently active.

## Superseded

The decision was previously accepted but has been replaced by a newer decision.

## Rejected

The option was considered but explicitly not selected.

## Deprecated

The decision is still historically relevant but should no longer be used for new implementation.

---

# Decision Numbering

ADR numbers are sequential.

Do not reuse an ADR number.

If a decision is replaced:

```text
ADR-010 → Superseded by ADR-027
```

Do not edit ADR-010 into ADR-027.

The historical record must remain understandable.

---

# Decision Change Example

Suppose we initially decide:

```text
ADR-003
Use a normal Scala application.
```

Later we discover that generation of extreme datasets requires distributed generation.

We should not silently modify ADR-003.

Instead:

```text
ADR-003
Status: Superseded by ADR-030
```

and create:

```text
ADR-030
Distributed generation for extreme-scale datasets
```

with a clear explanation of:

```text
problem discovered
alternatives considered
decision
reasoning
trade-offs
impact on previous architecture
```

---

# Current Decision Baseline

At the current stage, the major accepted direction is:

```text
Scala
  ↓
sbt
  ↓
Normal JVM Application
  ↓
Configuration-Driven Generation
  ↓
Deterministic Randomness
  ↓
Realistic Business Model
  ↓
Meaningful Relationships
  ↓
Validation
  ↓
CSV Dataset + Manifest
  ↓
Later Spark Learning Projects
```

The architecture remains intentionally incremental.

---

# Status

**Active**

This log is a historical record. New significant decisions should be appended rather than rewriting history.
