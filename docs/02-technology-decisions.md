# Technology Decisions

## Project

`ecommerce-data-generation`

## Purpose

This document records the technology choices for the E-Commerce Data Generation Platform.

The goal is to establish a stable technical foundation while avoiding unnecessary complexity in the first implementation.

Technology decisions should support the architectural principles documented in `01-architecture-decisions.md`.

---

# 1. Programming Language

## Decision

Use **Scala** as the implementation language.

## Reasoning

The generator is the first project in a broader Spark learning program.

Using Scala provides continuity between:

```text
E-Commerce Data Generator
        ↓
Spark Learning Projects
```

It also allows us to build the generator using the same language ecosystem that will be used for the later Spark work.

## Consequence

The core application, domain model, generation engine, validation logic, and supporting infrastructure will initially be implemented in Scala.

---

# 2. Build Tool

## Decision

Use **sbt**.

## Reasoning

sbt provides the standard project workflow we need for the initial Scala application:

```text
compile
test
run
package
dependency management
```

The build should remain deliberately simple during the foundation phase.

## Consequence

The first project structure will be an sbt-based Scala application.

We should avoid introducing build complexity that is not justified by an actual requirement.

---

# 3. Spark Dependency

## Decision

Do **not** use Spark as a dependency in the initial generator.

## Reasoning

The generator and Spark workloads have different responsibilities.

The generator is responsible for:

```text
data modeling
realistic generation
relationships
distributions
configuration
reproducibility
validation
```

The later Spark projects are responsible for:

```text
data processing
performance experiments
joins
aggregations
skew
memory
I/O
Spark optimization
```

Introducing Spark into the generator would unnecessarily couple these concerns.

## Consequence

The initial application should run independently of a Spark runtime.

If we later decide that distributed generation is necessary, that will be a new architectural/technology decision.

---

# 4. Configuration Technology

## Decision

Use **HOCON-style configuration** for the initial configuration system.

## Reasoning

The project requires hierarchical configuration covering many independent concerns:

```text
generator
customers
products
categories
brands
orders
order_items
payments
shipments
returns
sessions
events
geography
time
distributions
data_quality
skew
output
```

A hierarchical configuration format fits this model naturally.

It also allows profiles and scenarios to be represented without forcing application logic to contain experimental settings.

## Consequence

Configuration should be loaded into strongly modeled application configuration objects rather than passing raw configuration values throughout the application.

---

# 5. Configuration Validation

## Decision

Configuration must be validated when the application starts.

## Reasoning

Invalid configuration should fail before expensive data generation begins.

Examples of invalid conditions include:

```text
negative row count
negative cardinality
cardinality greater than possible domain
invalid probability
invalid date range
inconsistent entity counts
invalid scenario configuration
```

## Consequence

The application should have a clear distinction between:

```text
raw configuration
        ↓
configuration parsing
        ↓
configuration validation
        ↓
effective configuration
        ↓
generation
```

Generation should operate only on validated configuration.

---

# 6. Logging

## Decision

Introduce application logging during the project-foundation phase.

## Logging Goals

Logging should make the following observable:

```text
application startup
configuration loading
selected profile
selected scenario
seed
generation progress
validation progress
errors
completion statistics
```

Example:

```text
Generating customers...
Generated: 1,000,000

Generating products...
Generated: 100,000

Generating orders...
Generated: 10,000,000

Validating relationships...

Generation completed successfully.
```

## Reasoning

Large datasets can take significant time to generate.

Without useful logging, failures and performance characteristics become difficult to understand.

## Consequence

Logging should be treated as application infrastructure rather than being implemented ad hoc inside individual generators.

The exact logging library can be selected during project implementation.

---

# 7. Testing

## Decision

Automated tests are part of the foundation.

Testing should not be postponed until the entire generator is implemented.

## Testing Priorities

Tests should progressively cover:

```text
configuration parsing
configuration validation
deterministic generation
entity generation
relationships
distribution behavior
business invariants
data-quality scenarios
scenario behavior
manifest generation
```

## Reasoning

The generator is intended to produce experimental datasets.

A defect in the generator can invalidate an entire Spark experiment.

Therefore generator correctness must be established independently.

## Consequence

Each major generation capability should have tests introduced alongside its implementation.

---

# 8. Initial Output Format

## Decision

The initial raw output format will be **CSV**.

## Reasoning

CSV provides a simple and transparent first output format.

It allows us to:

- inspect generated data easily
- debug generation problems
- validate records
- consume the data from Spark
- keep the initial generator implementation straightforward

## Consequence

Initial output should follow the entity-oriented structure:

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

Each dataset may contain multiple CSV files.

---

# 9. Future Output Formats

## Decision

Other output formats are intentionally deferred.

Potential future formats may include:

```text
Parquet
JSON
Avro
```

However, none is required for the initial generator.

## Reasoning

The first objective is to establish correct business data generation.

File-format experiments belong naturally to the later Spark learning program.

## Consequence

The output layer should eventually be abstract enough that additional writers can be introduced without rewriting entity-generation logic.

The first implementation does not need to implement all possible writers.

---

# 10. Command-Line Interface

## Decision

The generator will provide a CLI as its primary execution interface.

The intended experience is conceptually:

```bash
ecommerce-data-generator   --profile large   --seed 12345   --scenario baseline
```

Another scenario:

```bash
ecommerce-data-generator   --profile large   --seed 12345   --scenario hot-customers
```

## Reasoning

The generator will be used repeatedly to create controlled datasets.

A CLI provides a clear and reproducible way to specify:

```text
profile
seed
scenario
configuration overrides
output location
```

## Consequence

The CLI should translate user input into the validated effective configuration consumed by the generation engine.

---

# 11. Randomness Infrastructure

## Decision

Random-number generation must be centralized behind application-controlled abstractions.

Individual generators should not create uncontrolled random sources independently.

## Reasoning

Deterministic reproducibility requires every random decision to ultimately derive from the configured seed.

## Consequence

The technology foundation should provide a controlled mechanism for random generation.

The precise implementation will be decided during Phase 1.

---

# 12. File-System Output

## Decision

The initial generator will write directly to a configurable local/file-system output location.

## Reasoning

The first version is intended to be a local engineering and learning project.

Introducing cloud storage or distributed storage infrastructure at this stage would add complexity without improving the core data-generation model.

## Consequence

The output path should be configurable.

The architecture should avoid hard-coding a particular machine-specific directory.

---

# 13. Dependency Management Principle

## Decision

Prefer a small dependency footprint initially.

## Reasoning

Every dependency introduces:

```text
maintenance
version compatibility
build complexity
security considerations
```

The generator should use external libraries where they provide clear value, but should not accumulate dependencies merely for convenience.

## Consequence

New dependencies should have an identifiable purpose.

A dependency that can easily be replaced by a small, well-tested internal abstraction should be evaluated carefully before introduction.

---

# 14. Versioning and Reproducibility

## Decision

The generator version must eventually be recorded as part of generation metadata.

The reproducibility model should therefore evolve from:

```text
configuration + seed
```

toward:

```text
generator version
+
configuration
+
seed
=
reproducible generation definition
```

## Reasoning

The same seed and configuration may not produce equivalent data if generation algorithms change between software versions.

Therefore the software version becomes part of the experimental record.

## Consequence

The generation manifest should eventually record the generator version.

---

# 15. Technology Selection Policy

Technology decisions should be driven by project requirements rather than by adding technologies because they are popular.

For every significant new technology, we should be able to answer:

```text
What problem does it solve?
Why is it needed now?
What complexity does it introduce?
Can the current architecture work without it?
Does it affect reproducibility?
Does it couple the generator to the later Spark experiments?
```

If the answer does not justify the technology, defer it.

---

# 16. Initial Technology Direction

The initial stack is therefore intentionally small:

```text
Language        → Scala
Build           → sbt
Configuration   → HOCON-style configuration
Execution       → CLI / normal JVM application
Output          → CSV
Logging         → application logging
Testing         → automated Scala tests
Spark           → intentionally excluded initially
```

This is sufficient to begin Phase 1 without prematurely committing to infrastructure that the project does not yet require.

---

# 17. Technology Decisions That Are Deliberately Deferred

The following should not be finalized until a concrete requirement appears:

```text
exact logging library
exact CLI library
exact testing framework
exact random/distribution library
Parquet implementation
cloud storage
database integration
distributed generation
containerization
CI/CD platform
```

These decisions should be made when their corresponding implementation phase requires them.

---

# Status

**Accepted — Initial Technology Direction**

This document records the current technology direction. Specific library choices may be introduced later through explicit decisions when implementation requirements justify them.
