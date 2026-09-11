# Configuration Decisions

## Project

`ecommerce-data-generation`

## Purpose

This document defines how configuration will control the behavior of the E-Commerce Data Generation Platform.

The central requirement is:

> Dataset characteristics should be changed through configuration without changing generation logic.

This document records the initial configuration architecture. Exact field names and implementation details may evolve as the system is built.

---

# 1. Configuration Is a First-Class Concern

## Decision

Configuration is part of the core architecture, not merely application startup input.

The generator will use configuration to control major dimensions of the generated dataset.

These dimensions include:

```text
volume
cardinality
distribution
relationships
time
behavior
data quality
skew
profiles
scenarios
output
seed
```

## Reasoning

The same generator must support very different workloads while preserving the same underlying business model.

For example:

```text
100K orders
```

and:

```text
100M orders
```

should be configuration changes rather than separate implementations.

---

# 2. Configuration Processing Pipeline

## Decision

Configuration should conceptually pass through the following stages:

```text
Configuration Files / CLI
          ↓
Configuration Loading
          ↓
Parsing
          ↓
Profile Resolution
          ↓
Scenario Resolution
          ↓
Explicit Overrides
          ↓
Validation
          ↓
Effective Configuration
          ↓
Generation
```

The generator should operate only on the validated effective configuration.

## Reasoning

Separating these stages makes configuration behavior predictable and testable.

It also prevents individual generators from independently interpreting configuration values.

---

# 3. HOCON-Style Configuration

## Decision

Use a HOCON-style hierarchical configuration format.

## Reasoning

The domain has multiple related configuration areas.

Conceptually:

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

A hierarchical structure makes these concerns explicit.

---

# 4. Generator Configuration

The generator-level configuration should contain global settings.

Example:

```hocon
generator {
    seed = 12345
}
```

Potential future global settings may include:

```text
generator version
generation identifier
default profile
default scenario
parallelism
```

Only settings that have a real application requirement should be added.

---

# 5. Volume Configuration

## Decision

Record counts should be independently configurable by entity.

Example:

```hocon
customers {
    count = 1000000
}

products {
    count = 100000
}

orders {
    count = 10000000
}

events {
    count = 100000000
}
```

The same principle applies to:

```text
addresses
categories
brands
order_items
payments
shipments
returns
sessions
```

where the effective count is either explicitly configured or derived from business relationships.

## Reasoning

Different entities have different workload characteristics.

The generator must not assume that every entity scales identically.

---

# 6. Cardinality Configuration

## Decision

Cardinality must be independently controllable from row volume.

Example:

```text
100M rows
+
100K unique customers
```

must be representable separately from:

```text
100M rows
+
50M unique customers
```

## Reasoning

Cardinality has a direct impact on later Spark workloads involving:

```text
joins
aggregations
grouping
shuffle
memory
```

## Consequence

Configuration must distinguish between:

```text
number of records
```

and:

```text
number of unique values
```

where the business domain permits that distinction.

---

# 7. Distribution Configuration

## Decision

Distribution choices and important distribution parameters should eventually be configurable.

Possible distribution families include:

```text
uniform
weighted categorical
normal
log-normal
Poisson
exponential
Zipf / power-law style
```

The selected distribution must make sense for the attribute being generated.

## Example

Conceptually:

```hocon
order_items {
    average_items_per_order = 3.2
}
```

and:

```hocon
products {
    popularity_distribution = "zipf"
}
```

The exact configuration syntax will be finalized when the distribution engine is implemented.

---

# 8. Time Configuration

## Decision

The generation time range must be configurable.

Conceptually:

```hocon
time {
    start_date = "2025-01-01"
    end_date = "2025-12-31"
}
```

Time configuration should eventually support:

```text
date range
daily activity patterns
weekly patterns
seasonality
holiday periods
festival periods
marketing campaigns
sales events
```

The calendar should remain configurable rather than embedding business-calendar assumptions directly into generator logic.

---

# 9. Profile Configuration

## Decision

Named scale profiles will provide convenient standard workloads.

Initial profiles:

```text
small
medium
large
xlarge
```

Conceptual examples:

```text
small:
    10K customers
    100K orders
    500K events

medium:
    100K customers
    1M orders
    10M events

large:
    1M customers
    10M orders
    100M events

xlarge:
    10M customers
    100M orders
    1B events
```

These values are conceptual presets.

They are not hard-coded limits.

## Consequence

A profile should resolve into configuration that can still be overridden where appropriate.

---

# 10. Scenario Configuration

## Decision

Named scenarios will represent controlled experimental conditions.

Initial scenarios include:

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

Scenarios allow us to create specific Spark workloads without modifying entity-generation code.

---

# 11. Scenario vs Profile

## Decision

Profiles and scenarios represent different concepts.

### Profile

Controls primarily:

```text
how much data
```

### Scenario

Controls primarily:

```text
what characteristics the data has
```

For example:

```text
profile = large
scenario = hot-customers
```

means:

> Generate a large dataset with the controlled hot-customer behavior.

This distinction should remain clear in the configuration architecture.

---

# 12. Data Quality Configuration

## Decision

Clean data is the default.

Data-quality problems are controlled through configuration.

Example:

```hocon
data_quality {
    null_customer_id_rate = 0.0
    duplicate_order_rate = 0.0
    invalid_price_rate = 0.0
    invalid_timestamp_rate = 0.0
    missing_payment_rate = 0.0
}
```

Potential quality profiles:

```text
clean
slightly_dirty
dirty
```

Individual rates should remain configurable where appropriate.

---

# 13. Skew Configuration

## Decision

Skew is disabled by default.

Example:

```hocon
skew {
    enabled = false

    hot_customer_count = 10
    hot_customer_multiplier = 100

    hot_product_count = 5
    hot_product_multiplier = 50
}
```

When enabled, a small number of keys can receive disproportionately large amounts of activity.

## Reasoning

Skew is a deliberate experimental characteristic.

It should never appear accidentally because of an implementation detail.

---

# 14. Output Configuration

The output location and relevant output behavior should be configurable.

Conceptually:

```hocon
output {
    base_path = "data/raw"
}
```

Future output settings may include:

```text
file count
file size
partitioning
compression
output format
```

These should be introduced only when the corresponding functionality exists.

---

# 15. CLI Overrides

## Decision

The CLI should be able to override selected configuration values.

Conceptually:

```bash
ecommerce-data-generator     --profile large     --seed 12345     --scenario hot-customers
```

Potential future overrides may include:

```bash
--output <path>
--customers <count>
--orders <count>
```

CLI options should not create a second configuration system.

They should modify or override the loaded configuration and then pass the resulting configuration through normal validation.

---

# 16. Configuration Precedence

The intended precedence model is:

```text
Default Configuration
        ↓
Profile
        ↓
Scenario
        ↓
CLI / Explicit Overrides
```

The resulting configuration is then validated.

Conceptually:

```text
defaults
    +
profile
    +
scenario
    +
explicit overrides
        ↓
effective configuration
```

The exact merge semantics will be finalized during implementation.

---

# 17. Configuration Validation

## Decision

Invalid configuration must fail before generation begins.

Validation should detect problems such as:

```text
negative counts
zero where a positive value is required
negative probabilities
probability > 1
invalid dates
end date before start date
cardinality greater than row count where applicable
invalid distribution parameters
invalid scenario parameters
inconsistent relationships
```

The application should produce a clear error explaining what is invalid.

---

# 18. Strong Configuration Model

## Decision

Generators should consume strongly modeled configuration objects rather than raw configuration lookups throughout the codebase.

Conceptually:

```text
Raw Configuration
       ↓
Parsed Configuration
       ↓
Validated Configuration
       ↓
Typed Domain Configuration
       ↓
Generators
```

## Reasoning

This reduces coupling between the configuration format and business logic.

It also makes configuration behavior easier to test.

---

# 19. Configuration Must Be Reproducible

A generation run must be reconstructable from its effective configuration and seed.

The eventual generation manifest should therefore preserve the effective configuration rather than only recording:

```text
profile = large
scenario = baseline
```

It should preserve enough information to understand the actual settings used for that generation.

The reproducibility model will eventually be:

```text
generator version
+
effective configuration
+
seed
=
generation definition
```

---

# 20. No Business Logic in Configuration

## Decision

Configuration describes generation behavior; it should not become a programming language.

Avoid putting complex business algorithms directly into configuration.

Configuration should specify things such as:

```text
counts
rates
weights
thresholds
ranges
distribution names
profile names
scenario names
dates
paths
```

Generation logic should remain in Scala.

## Reasoning

This keeps the system understandable and testable.

---

# 21. No Hard-Coded Experimental Scenarios

A scenario should not require source-code modification.

Bad pattern:

```text
if scenario == "hot-customers":
    use completely different generator
```

Preferred direction:

```text
scenario
   ↓
effective configuration
   ↓
normal generation architecture
```

The scenario changes controlled parameters and behavior through established abstractions.

---

# 22. Configuration Documentation

Every important configuration property should eventually document:

```text
name
type
default
valid range
meaning
dependencies
example
```

This documentation should be maintained alongside the implementation.

---

# 23. Initial Configuration Structure

The intended top-level structure is:

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

The exact fields will be added incrementally as each generation capability is implemented.

We should not attempt to define every possible configuration property before its corresponding functionality exists.

---

# 24. Guiding Principle

The configuration system should allow us to answer:

> Can we create a meaningfully different experimental dataset by changing configuration rather than changing application code?

If yes, the configuration architecture is serving its purpose.

If no, we should first investigate whether the missing capability belongs in:

```text
configuration
distribution model
scenario model
domain model
generation abstraction
```

before introducing experiment-specific code.

---

# Status

**Accepted — Initial Configuration Direction**

This document defines the current configuration architecture. Exact property names, types, defaults, and merge semantics will be finalized during implementation and recorded through explicit decisions where necessary.
