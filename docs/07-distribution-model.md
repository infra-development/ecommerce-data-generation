# 07 — Distribution Model

## Purpose

This document defines the statistical distribution strategy for the ShopSphere synthetic e-commerce data generator.

The goal is not to generate random-looking data. The goal is to generate **statistically plausible data whose distributions can be controlled, reproduced, validated, and scaled**.

This document is an implementation decision record for how distributions should be represented and applied. The business/domain specification remains in the project plan and `04-data-model-decisions.md`.

---

## 1. Core Decision

The generator will use a **distribution-driven generation model** rather than uniform random generation.

Business fields will be assigned an explicit distribution strategy where realism matters.

Examples:

- categorical fields → weighted categorical distributions
- product popularity → long-tail / heavy-tailed distributions
- order item counts → discrete probability distributions
- customer activity → heterogeneous activity distributions
- monetary values → bounded or skewed numeric distributions
- timestamps → time-of-day / day-of-week / seasonal distributions
- correlated attributes → conditional distributions or behavioral profiles

### Decision

> **Uniform randomness is an implementation primitive, not the default business distribution.**

Uniform random generation may still be appropriate for fields where every value is genuinely intended to have approximately equal probability.

---

## 2. Distribution Layer

Distribution logic should be separated from domain/entity generation.

Conceptually:

```text
Domain Generator
      |
      v
Distribution API
      |
      +-- Weighted Categorical
      +-- Uniform
      +-- Bernoulli
      +-- Discrete Probability
      +-- Normal / Log-Normal
      +-- Exponential / Gamma
      +-- Power-Law / Zipf-like
      +-- Bounded Numeric
      +-- Conditional Distribution
      +-- Mixture Distribution
```

The entity generator should ask for a business-level sample rather than implement probability calculations itself.

For example:

```text
CustomerGenerator
    -> sample(customerSegmentDistribution)

OrderGenerator
    -> sample(orderStatusDistribution)

ProductPopularityModel
    -> sample(productSelectionDistribution)
```

This keeps statistical logic reusable and prevents probability formulas from being scattered throughout the codebase.

---

## 3. Distribution Contract

Every distribution implementation should conceptually provide:

1. a way to sample a value
2. parameters describing the distribution
3. deterministic behavior from the supplied random source
4. validation of its parameters
5. a useful name/type for diagnostics
6. enough metadata to appear in effective configuration or generation statistics

Conceptually:

```text
Distribution[T]
    sample(random): T
    validate(): Result
    describe(): DistributionMetadata
```

The exact Scala API is intentionally deferred until implementation.

---

## 4. Weighted Categorical Distribution

Weighted categorical sampling will be the primary mechanism for business categories.

Example:

```text
customerSegment:
  new:       0.25
  regular:   0.50
  loyal:     0.20
  vip:       0.05
```

The generator should not assume equal probability merely because four categories exist.

### Requirements

- probabilities must be non-negative
- probabilities must sum to approximately 1.0, or be normalized explicitly
- empty category sets are invalid
- deterministic sampling must use the generation random source
- category order must be stable

### Stable ordering

Configuration parsing should preserve or explicitly establish deterministic category ordering.

The same seed and effective configuration must not produce different category assignments merely because an unordered collection was iterated in a different order.

---

## 5. Discrete Business Distributions

Some quantities should be modeled using discrete probability distributions rather than arbitrary random numbers.

Examples:

### Order items per order

Instead of:

```text
items = random(1, 10)
```

prefer a configured distribution such as:

```text
1 item  -> 0.50
2 items -> 0.25
3 items -> 0.12
4 items -> 0.06
5+      -> 0.07
```

The actual probabilities are configuration decisions and should be validated against observed expectations.

### Other candidates

- number of addresses per customer
- sessions per customer
- events per session
- returns per customer
- payment method selection
- shipment delay buckets

---

## 6. Long-Tail Product Popularity

Product popularity is a major realism requirement.

A naive uniform model:

```text
every product has equal probability
```

would make the synthetic dataset unsuitable for many realistic analytical experiments.

The generator should support a long-tail popularity model in which:

- a small number of products receive many orders
- a larger number receive moderate traffic
- a very large number receive relatively little traffic

This creates a realistic concentration of product activity and provides useful data-skew characteristics for downstream Spark experiments.

### Conceptual model

```text
Product rank
1       -> very high probability
2       -> high probability
3       -> high probability
...
100     -> moderate probability
...
10000   -> low probability
...
```

A Zipf-like or power-law model is an appropriate implementation family, but the exact formula and parameterization remain implementation decisions.

### Important constraint

The model must avoid producing an extreme concentration accidentally.

The configuration should therefore allow control over the degree of skew.

---

## 7. Customer Activity Distribution

Customers should not all behave identically.

A useful model is a heterogeneous population:

```text
Low activity customers
        |
        +--> occasional sessions/orders

Medium activity customers
        |
        +--> regular activity

High activity customers
        |
        +--> frequent sessions/orders

VIP / highly active customers
        |
        +--> disproportionately high activity
```

Customer activity can influence:

- session frequency
- event count
- order frequency
- order value
- product category affinity
- return behavior

This supports the behavioral-correlation requirement from the project specification.

The activity model should be separated from the individual entity generators.

---

## 8. Mixture Distributions

Real populations are often better represented as a combination of several sub-populations.

Example:

```text
Customer population
       |
       +-- Low activity     70%
       +-- Medium activity  25%
       +-- High activity     4%
       +-- VIP               1%
```

Each segment may have different distributions.

For example:

```text
Low activity
    sessions ~ low-count distribution
    orders   ~ low-count distribution

High activity
    sessions ~ high-count distribution
    orders   ~ high-count distribution
```

### Decision

Use mixture distributions or behavioral profiles when a single distribution cannot adequately represent population heterogeneity.

This is preferable to adding arbitrary random noise to otherwise uniform data.

---

## 9. Conditional Distributions

Some values depend on another attribute.

Examples:

```text
product category
      |
      +--> price distribution
      +--> discount distribution
      +--> popularity distribution
```

and:

```text
customer segment
      |
      +--> order frequency
      +--> average order value
      +--> return probability
```

and:

```text
order value
      |
      +--> payment method probability
```

These should be represented as **conditional distributions**, not independent random fields.

Conceptually:

```text
P(price | category)
P(orderFrequency | customerSegment)
P(paymentMethod | orderValue)
```

This is a key mechanism for preserving business realism.

---

## 10. Correlation Strategy

The generator should deliberately model important correlations.

### Examples

| Driver | Dependent attribute |
|---|---|
| Customer segment | activity |
| Customer activity | order count |
| Product category | price |
| Product popularity | order frequency |
| Order value | payment method |
| Order status | shipment/return behavior |
| Time period | order/event volume |
| Acquisition channel | customer behavior |

Not every field needs a correlation.

The objective is to model **business-significant dependencies**, not to create artificial correlation everywhere.

---

## 11. Time Distributions

Time generation should also be distribution-driven.

The project specification calls for:

- daily patterns
- weekly patterns
- seasonal patterns
- realistic event timing

Therefore timestamps should not simply be:

```text
timestamp = random timestamp across entire period
```

Instead, the model should consider:

```text
date
  -> day-of-week weight
  -> seasonal weight
  -> daily volume

time-of-day
  -> hour/minute distribution
```

For behavioral events, time may additionally depend on event/session state.

Example:

```text
session start
    |
    +--> product_view
    +--> add_to_cart
    +--> checkout
    +--> purchase
```

The event sequence and timestamps should remain logically consistent.

---

## 12. Monetary Distributions

Monetary values should generally not be uniformly distributed across a large range.

For example:

```text
orderTotal = Uniform(100, 100000)
```

would typically produce an unrealistic population.

Potential models include:

- log-normal
- gamma
- bounded mixtures
- category-conditioned distributions
- segment-conditioned distributions

The selected model should support:

- positive values
- realistic concentration
- configurable lower/upper bounds where required
- reproducibility
- validation

Order totals should also remain consistent with order-item values.

Therefore, distribution sampling must not override deterministic business calculations.

A preferred approach is:

```text
product price
    +
quantity
    -
discount
    =
line amount

sum(line amounts)
    =
order subtotal
```

The distribution controls the underlying business variables; derived totals are calculated from those variables.

---

## 13. Distribution vs Derived Value

A fundamental rule:

> **Do not independently sample values that should be mathematically derived.**

For example, do not independently generate:

```text
orderTotal
orderItemTotal
paymentAmount
```

Instead:

```text
product price
      +
quantity
      -
discount
      |
      v
order item amount
      |
      v
order subtotal
      |
      v
order total
      |
      v
payment amount
```

This prevents statistically plausible individual fields from producing an internally inconsistent record.

---

## 14. Distribution Parameters

Distribution parameters must be configurable.

Examples:

```text
distribution {
  type = "weighted-categorical"
  values = {
    regular = 0.50
    loyal = 0.20
    vip = 0.05
    new = 0.25
  }
}
```

or:

```text
distribution {
  type = "zipf"
  exponent = 1.1
}
```

or:

```text
distribution {
  type = "log-normal"
  mean = ...
  standardDeviation = ...
}
```

The exact syntax is governed by `03-configuration-decisions.md`.

Distribution configuration should be declarative rather than encoded as custom logic inside generators.

---

## 15. Parameter Validation

Invalid distributions must fail during configuration validation rather than halfway through generation.

Examples of invalid configuration:

```text
negative probability
probabilities that cannot be normalized
negative standard deviation
invalid upper/lower bounds
empty categorical distribution
invalid Zipf exponent
unknown distribution type
```

The configuration validation layer should report:

- field/path
- distribution type
- invalid parameter
- expected constraint

Example:

```text
Invalid configuration:
distributions.customerSegment.values.vip

Probability must be >= 0 and part of a valid categorical distribution.
```

---

## 16. Deterministic Sampling

All distribution sampling must use the generator's controlled randomness model.

There must be no hidden use of:

```text
java.util.Random()
Math.random()
current time as an implicit seed
```

inside distribution implementations.

Instead:

```text
GenerationContext
      |
      v
Seeded Random Source
      |
      v
Distribution.sample(...)
```

This preserves the project-wide reproducibility decision.

Distribution implementations should therefore be pure with respect to their explicit random input as far as practical.

---

## 17. Stable Sampling Across Runs

The same:

```text
effective configuration
+
generator version
+
seed
```

should produce the same logical distribution sampling behavior for the same generation strategy.

Changing:

- seed
- distribution parameters
- distribution implementation/version

may change generated values.

Such changes should be visible in generation metadata where practical.

---

## 18. Distribution Versioning

Statistical models are part of the generator's behavior.

Changing a distribution formula can materially change generated datasets even when configuration appears unchanged.

Therefore distribution changes should be treated as generator behavior changes.

Examples:

```text
productPopularityModel = zipf-v1
```

or equivalent version information in generator metadata.

The exact representation is deferred, but the principle is accepted:

> **A statistical model change is a reproducibility-relevant change.**

---

## 19. Calibration

Configured probabilities are hypotheses about the generated population.

The generator should eventually support comparing:

```text
configured distribution
        vs
observed generated distribution
```

For example:

```text
Expected:
VIP = 5%

Observed:
VIP = 4.98%
```

For sufficiently large populations, observed proportions should converge toward configured expectations within defined tolerances.

Calibration should be used to detect:

- implementation errors
- incorrect weighting
- accidental bias
- boundary errors
- seed-related anomalies
- incorrect conditional sampling

---

## 20. Distribution Validation

Distribution validation should operate at multiple levels.

### Level 1 — Parameter validation

Checks that configuration is mathematically valid.

### Level 2 — Sampling validation

Checks that a distribution implementation produces expected behavior over many samples.

### Level 3 — Generation validation

Checks observed distributions in generated datasets.

### Level 4 — Relationship validation

Checks that conditional distributions preserve intended relationships.

Example:

```text
VIP customers
    should have higher average activity
```

The exact statistical tests and tolerance framework will be defined later in `11-validation-strategy.md`.

---

## 21. Avoiding Overfitting

The generator is intended to create realistic analytical datasets, not reproduce one exact production dataset.

Therefore distributions should capture:

- realistic shapes
- useful correlations
- meaningful variability
- controllable skew

without attempting to encode every possible production-specific detail.

The model should remain understandable and configurable.

---

## 22. Distribution Presets

Reusable distribution presets may be introduced for common patterns.

Examples:

```text
mostly-common-with-small-tail
long-tail
heavy-tail
rare-event
balanced-categorical
weekday-heavy
weekend-heavy
business-hours
```

Presets should be convenience abstractions.

They must resolve into explicit effective distribution parameters so that the final generation remains inspectable and reproducible.

---

## 23. Relationship to Profiles and Scenarios

Distribution models and scale profiles serve different purposes.

### Profile

Defines a reusable baseline.

Example:

```text
large
```

may select:

- large entity volumes
- higher cardinalities
- standard realistic distributions

### Scenario

Introduces a deliberate analytical condition.

Example:

```text
product-skew
```

may increase concentration in product popularity.

Therefore:

```text
Profile
   -> baseline distribution parameters

Scenario
   -> controlled modifications to distribution behavior
```

This separation follows the configuration decisions in `03-configuration-decisions.md`.

---

## 24. Distribution and Data Quality

Data quality problems should not normally be implemented by corrupting the distribution layer itself.

Preferred flow:

```text
Generate realistic clean value
          |
          v
Apply explicit data-quality policy
          |
          v
Controlled dirty value
```

For example:

```text
valid phone number distribution
          |
          v
data-quality scenario
          |
          v
small percentage of malformed phones
```

This preserves a clean statistical baseline while allowing controlled imperfections.

---

## 25. Distribution and Skew Scenarios

Skew is a deliberate analytical characteristic.

A skew scenario may modify:

- product popularity exponent
- hot-product probability mass
- customer activity concentration
- partition-key concentration

The underlying distribution mechanism remains reusable.

Conceptually:

```text
Base distribution
       |
       +--> Normal scenario
       |
       +--> Moderate skew
       |
       +--> Extreme skew
```

The dedicated skew design will be documented in `10-skew-scenario-design.md`.

---

## 26. Distribution Statistics

Generation statistics should capture useful observed distribution information.

Potential metrics:

```text
category counts
category percentages
min / max
mean
median
p95 / p99
standard deviation
distinct count
top-N frequencies
```

For heavy-tailed fields:

```text
top 1%
top 10%
share of total activity
```

may be more informative than mean alone.

These statistics support both debugging and downstream Spark experiments.

---

## 27. Distribution Metadata

The generation manifest should eventually make it possible to understand which statistical configuration produced a dataset.

Useful metadata includes:

```text
distribution model/type
parameters or effective configuration reference
scenario
profile
seed
generator version
```

Avoid embedding excessive implementation detail into individual data files.

The manifest and effective configuration are the appropriate sources of generation metadata.

---

## 28. Testing Strategy for Distributions

Distribution tests should be deterministic where possible.

Tests should cover:

### Unit tests

- parameter validation
- boundary behavior
- weighted sampling
- deterministic seed behavior
- empty/invalid configuration

### Statistical tests

- observed proportions
- expected ranges
- long-tail shape
- conditional relationships
- mixture behavior

### Regression tests

Known configuration + seed should preserve expected generation characteristics unless an intentional model change is made.

Statistical tests should use tolerances rather than exact frequency equality.

---

## 29. Scale Considerations

Distribution sampling must remain efficient at large volumes.

Avoid designs that require:

```text
large in-memory probability tables
```

when a compact mathematical model can provide equivalent behavior.

For categorical distributions, cumulative weights or an efficient alias-style sampler may be appropriate depending on cardinality and sampling frequency.

For long-tail distributions, avoid materializing unnecessarily large structures when direct or compact sampling is practical.

The implementation choice will be made after profiling rather than prematurely optimizing.

---

## 30. Initial Distribution Set

The first implementation should support only the distributions needed for the initial vertical slice.

Recommended initial set:

1. Uniform
2. Weighted categorical
3. Bernoulli
4. Discrete weighted values
5. Bounded numeric
6. One long-tail / Zipf-like model

Additional distributions should be introduced when a domain requirement justifies them.

This avoids building a generic statistical framework before the generator has proven its domain model.

---

## 31. Initial Vertical Slice

The first end-to-end implementation should exercise the distribution system through realistic business fields.

Suggested examples:

```text
Customer
  -> segment: weighted categorical
  -> acquisition channel: weighted categorical

Product
  -> category: weighted categorical
  -> popularity: long-tail model

Order
  -> item count: discrete distribution
  -> status: weighted categorical

Payment
  -> method: weighted categorical
```

Then validate:

```text
configured probabilities
        vs
observed generated statistics
```

This provides an early proof that configuration, randomness, distributions, generation, and validation work together.

---

## 32. Deferred Decisions

The following are intentionally not finalized in this document:

- exact Scala distribution interfaces
- exact random-number-generator implementation
- exact Zipf/power-law algorithm
- exact statistical goodness-of-fit tests
- alias-method implementation
- numerical precision policy
- performance benchmarks for each distribution
- final configuration syntax for every distribution
- advanced copula/multivariate statistical modeling

These decisions should be made when the corresponding implementation need appears.

---

## 33. Guiding Principle

The distribution system exists to make generated data **predictable statistically, not predictable row-by-row**.

The generator should provide:

```text
realistic shape
+
controlled parameters
+
business correlations
+
deterministic randomness
+
validation
+
scalability
```

while avoiding:

```text
uniform randomness everywhere
+
independent fields everywhere
+
hidden probability logic
+
unvalidated parameters
+
non-deterministic sampling
```

The core rule is:

> **Model the probability of business behavior explicitly, then let deterministic seeded randomness realize that model.**
