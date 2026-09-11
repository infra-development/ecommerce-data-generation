# Architecture Decisions

## Project

`ecommerce-data-generation`

## Purpose

This document is the authoritative record of the major architectural decisions for the E-Commerce Data Generation Platform.

The purpose is to capture **what we decided, why we decided it, and what consequences the decision has**.

A significant architectural decision must not be changed silently. If a decision changes, a new decision should supersede the old one and the reason for the change should be recorded.

---

# 1. Architectural Vision

The system will be a **synthetic e-commerce data-generation platform**.

Its purpose is to generate realistic, configurable, reproducible datasets that will become the controlled data foundation for the later Spark Performance Laboratory.

The generator is therefore not simply a random-data utility.

It must eventually provide:

- realistic business entities
- meaningful relationships
- configurable data volume
- configurable cardinality
- realistic statistical distributions
- realistic timestamps and behavior
- deterministic/reproducible generation
- controlled data-quality problems
- controlled data skew
- named scale profiles
- validation
- generation statistics
- generation manifests

The central architectural principle is:

> The same generation logic must be capable of producing different dataset sizes and characteristics through configuration rather than application-code changes.

---

# 2. Separation of Responsibilities

## Decision

The data generator and the Spark processing workloads are separate systems.

The generator creates the experimental input data.

The later Spark projects consume that data and investigate Spark behavior.

Conceptually:

```text
E-Commerce Data Generator
          |
          v
    Controlled Dataset
          |
          v
   Spark Learning Projects
```

## Reasoning

Mixing data generation and Spark processing would make it harder to determine whether a performance difference came from:

- the generated dataset
- the generation process
- Spark configuration
- Spark application logic

Keeping the systems separate gives us a controlled experimental environment.

## Consequence

The generator should not contain Spark-specific processing logic merely because the generated data will eventually be consumed by Spark.

---

# 3. Generator Is Initially a Normal Scala Application

## Decision

The first version of the generator will be implemented as a **normal Scala application**, not as a Spark application.

## Reasoning

The initial responsibilities are:

```text
data modeling
realistic generation
relationships
distributions
configuration
reproducibility
validation
```

These responsibilities do not require Spark.

Spark is the subject of the later experimentation, not a requirement of the generator itself.

## Consequence

The initial generator should not introduce Spark as a core dependency.

If generation itself eventually becomes a scaling problem, that should be treated as a separate engineering investigation rather than prematurely designing the entire generator around Spark.

---

# 4. Configuration-Driven Architecture

## Decision

Major characteristics of generated data must be controlled through configuration.

Configuration should eventually control:

```text
volume
cardinality
distributions
relationships
time range
behavior
data quality
skew
scale profiles
scenarios
output
seed
```

## Reasoning

The same business model must support workloads such as:

```text
100K orders
```

and:

```text
100M orders
```

without changing generation logic.

Similarly, the same workload should be able to represent different key-cardinality characteristics.

For example:

```text
100M rows + 100K customers
```

and:

```text
100M rows + 50M customers
```

should be configuration variations of the same generation system.

## Consequence

Generation code should consume configuration rather than contain hard-coded dataset sizes or experimental scenarios.

---

# 5. Deterministic Generation

## Decision

Random generation must be controlled by a deterministic seed.

The fundamental reproducibility contract is:

```text
same configuration
+
same seed
+
same generator version
=
equivalent generated dataset
```

Changing only the seed should produce a different dataset while maintaining broadly similar statistical characteristics.

## Reasoning

The platform exists partly to support controlled Spark performance experiments.

If an experiment changes Spark code or configuration, the input dataset must remain controlled.

Otherwise, we cannot confidently attribute a performance difference to the change being investigated.

## Consequence

Randomness must not be scattered through the application using uncontrolled random sources.

The random-generation architecture should eventually provide a consistent mechanism for deriving deterministic randomness.

---

# 6. Incremental Architecture

## Decision

The complete architecture will be implemented incrementally.

We will not build every subsystem before producing the first working generator.

The target architecture is conceptually:

```text
Application
    |
    +-- Configuration
    |
    +-- Reference Data
    |
    +-- Distribution Engine
    |
    +-- Entity Generators
    |
    +-- Relationship Manager
    |
    +-- Data Quality Engine
    |
    +-- Scenario Engine
    |
    +-- Validator
    |
    +-- Writer
    |
    +-- Manifest
    |
    +-- Statistics
```

## Reasoning

Implementing everything at once would introduce unnecessary complexity before the fundamental generation model is proven.

## Consequence

Each component should be introduced when the project reaches the point where that component provides real value.

---

# 7. Entity Generators Are Separate Responsibilities

## Decision

Business entities should have separate generation responsibilities.

The planned domain contains:

```text
Customer
Address
Product
Category
Brand
Order
OrderItem
Payment
Shipment
Return
Session
Event
```

The implementation should evolve toward dedicated generators such as:

```text
CustomerGenerator
ProductGenerator
OrderGenerator
OrderItemGenerator
PaymentGenerator
SessionGenerator
EventGenerator
```

Additional generators will be added as the domain expands.

## Reasoning

Separating entity generation allows each business domain to evolve independently while preserving the overall architecture.

## Consequence

A generator for one entity should not become responsible for the entire application.

---

# 8. Relationships Are a First-Class Concern

## Decision

The platform must model meaningful relationships between entities rather than generating independent random tables.

The conceptual model is:

```text
Customer
 ├── Address
 ├── Session
 │    └── Event
 └── Order
      ├── OrderItem
      │    └── Product
      │         ├── Category
      │         └── Brand
      ├── Payment
      ├── Shipment
      └── Return
```

## Reasoning

Later Spark workloads need realistic joins and relational structures.

Randomly generated foreign keys would not provide the same experimental value as meaningful business relationships.

## Consequence

Referential integrity and relationship semantics must be considered during generation, not added only as a post-processing step.

---

# 9. Realism Over Uniform Randomness

## Decision

The generator must model realistic statistical behavior rather than treating every field as uniformly random.

Examples include:

```text
customer order frequency
product popularity
order amount
customer spending
event frequency
category popularity
payment method usage
```

Possible distribution families include:

```text
uniform
weighted categorical
normal
log-normal
Poisson
exponential
Zipf/power-law style
```

The appropriate distribution should be selected according to the business meaning of the attribute.

## Reasoning

Uniform random data does not adequately represent real e-commerce workloads.

Real systems contain:

- popular products
- repeat customers
- long-tail products
- different spending levels
- different activity levels
- temporal patterns

These characteristics are important for later Spark experiments.

## Consequence

The distribution model will become a dedicated architectural concern rather than being embedded independently inside every entity generator.

---

# 10. Behavioral Correlation

## Decision

Business behavior should contain meaningful probabilistic correlations.

For example, an internally modeled customer profile may influence:

```text
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
activity_level
```

A customer with high activity should not necessarily be guaranteed to have high purchases, but the probability of related behavior should be different.

## Reasoning

The goal is probabilistic realism, not deterministic business rules.

## Consequence

The architecture must eventually support shared behavioral attributes or latent profiles that can influence multiple generated entities.

---

# 11. Data Quality Is a Controlled Scenario

## Decision

Clean data is the default.

Data-quality problems are introduced deliberately through configuration.

Potential problems include:

```text
null values
duplicates
invalid values
missing relationships
invalid timestamps
invalid prices
invalid quantities
malformed strings
```

## Reasoning

The baseline dataset should provide a clean control case.

Dirty data should be an intentional experimental condition.

## Consequence

Data-quality behavior should be implemented as a configurable subsystem rather than mixed indiscriminately into normal entity generation.

---

# 12. Data Skew Is a Controlled Scenario

## Decision

Data skew is disabled by default and introduced through an explicit scenario.

Examples include:

```text
hot customers
hot products
highly repeated keys
```

## Reasoning

Skew is specifically useful for later Spark performance investigations.

The experiment must be able to compare:

```text
baseline
vs.
controlled skew
```

without changing the fundamental business model.

## Consequence

The architecture should contain a Scenario Engine or equivalent mechanism that can modify generation characteristics without requiring changes to entity-generation logic.

---

# 13. Profiles Are Presets, Not Limits

## Decision

The generator will eventually support named scale profiles such as:

```text
small
medium
large
xlarge
```

Profiles are convenience presets.

They are not hard-coded limits.

## Reasoning

The project needs convenient standard workloads for repeated experiments while still allowing exact custom configurations.

## Consequence

A user should be able to select a profile and then override appropriate configuration values.

---

# 14. Scenarios Must Not Require Code Changes

## Decision

Experimental scenarios should be selectable through configuration.

Examples:

```text
baseline
high-volume
high-cardinality
low-cardinality
hot-customers
hot-products
many-small-files
large-dimension
small-dimension
dirty-data
seasonal-sales
high-repeat-customers
anonymous-heavy-events
```

Conceptually:

```text
scenario = "hot-customers"
```

## Reasoning

Experimental conditions should be reproducible and independent of source-code modifications.

## Consequence

A scenario represents a controlled configuration of the generation system, not a fork of the generation implementation.

---

# 15. Validation Is Part of Generation

## Decision

Generated data must be validated.

Validation should eventually cover:

### Referential integrity

```text
Order → Customer
OrderItem → Order
OrderItem → Product
Payment → Order
Event → Session
```

### Business consistency

```text
quantity > 0
price >= 0
cost >= 0
order_total ≈ sum(order items)
payment amount ≈ order amount
session_end >= session_start
event_timestamp within session bounds
```

### Statistical properties

```text
row counts
unique counts
null percentages
duplicate percentages
distribution summaries
```

## Reasoning

A large dataset is not useful if its correctness cannot be established.

## Consequence

Validation must be treated as a first-class subsystem and not as an optional debugging utility.

---

# 16. Generation Manifest Is Part of the Dataset

## Decision

Every generation run should produce a generation manifest.

The manifest should eventually contain:

```text
generation_id
seed
profile
scenario
configuration
generation timestamp
date range
row counts
unique key counts
file counts
file sizes
data-quality statistics
distribution statistics
```

## Reasoning

The manifest provides the audit record needed to understand and reproduce an experimental dataset.

## Consequence

A generated dataset should be considered incomplete if its associated generation metadata is missing.

---

# 17. Observability

## Decision

The generator should expose generation progress and final statistics.

Progress should communicate operations such as:

```text
Generating customers...
Generating products...
Generating orders...
Generating order items...
Generating events...
Validating relationships...
Generation completed successfully.
```

Final statistics should include at least:

```text
total generation time
rows generated
files generated
output size
validation status
```

## Reasoning

Large generation jobs need operational visibility.

## Consequence

Observability will be designed into the application rather than added only after performance or operational problems appear.

---

# 18. Output Structure

## Decision

The initial raw output will be organized by entity and generation run.

Conceptually:

```text
data/
└── raw/
    └── generation-<id>/
        ├── customers/
        ├── addresses/
        ├── products/
        ├── categories/
        ├── brands/
        ├── orders/
        ├── order_items/
        ├── payments/
        ├── shipments/
        ├── returns/
        ├── sessions/
        ├── events/
        └── generation-manifest.json
```

Each entity may contain multiple files.

## Reasoning

Generation runs must remain isolated so that datasets from different experiments do not overwrite one another.

## Consequence

Output management must understand generation identity and dataset boundaries.

---

# 19. Correctness Before Generation Performance

## Decision

The generator will be developed in this order:

```text
Correctness
    ↓
Business realism
    ↓
Relationships
    ↓
Configurability
    ↓
Reproducibility
    ↓
Scale
    ↓
Special scenarios
    ↓
Generation performance
```

## Reasoning

Premature optimization could make the generator harder to understand and compromise the quality of the experimental data.

The most valuable output is a controlled and realistic experimental environment, not simply the maximum number of rows generated per second.

## Consequence

Generation performance will be measured and improved after correctness and core functionality are established.

---

# 20. Decision Change Policy

A significant architectural decision must not be silently changed.

When an existing decision needs to change:

1. Create a new ADR.
2. Identify the previous decision.
3. Mark the previous decision as `Superseded`.
4. Record the reason for the change.
5. Record the consequences.
6. Reference the new decision.

Example:

```text
ADR-003
Status: Superseded by ADR-020
```

This preserves the project's engineering history.

---

# 21. Current Architecture Direction

At the end of the foundation phase, the application should conceptually evolve toward:

```text
                         Configuration
                              |
                              v
                         Application
                              |
              +---------------+---------------+
              |               |               |
              v               v               v
        Reference Data   Distribution     Scenario
                           Engine           Engine
              |               |               |
              +---------------+---------------+
                              |
                              v
                      Entity Generators
                              |
                              v
                    Relationship Management
                              |
                              v
                     Data Quality Engine
                              |
                              v
                          Validator
                              |
                    +---------+---------+
                    |                   |
                    v                   v
                  Writer          Statistics
                    |                   |
                    +---------+---------+
                              |
                              v
                           Manifest
```

This is the **target architectural direction**, not a requirement to implement every component immediately.

---

# 22. Guiding Principle

The platform should always be evaluated against this question:

> Can we create a realistic, controlled, measurable dataset for a specific Spark experiment without changing the generator's business logic?

If the answer is yes, the architecture is moving in the right direction.

If the answer is no, the problem should generally be solved through better configuration, modeling, distribution, scenario, or generation abstractions before adding experiment-specific code.

---

## Status

**Accepted — Initial Architecture**

This document is expected to evolve through explicit architectural decisions recorded in the ADR log.
