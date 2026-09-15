# 17 — Randomness, Distribution, and Reproducibility Architecture

## 1. Purpose

Randomness is foundational infrastructure for the ShopSphere synthetic e-commerce data generator.

Almost every domain requires probabilistic decisions:

```text
Customer acquisition
Customer demographics
Customer behavior
Address selection
Product/category/brand selection
Order frequency
Basket size
Product popularity
Payment method
Shipment timing
Return propensity
Session frequency
Event journeys
```

If randomness is treated as a collection of ad-hoc calls to `Random`, the generated dataset may be syntactically valid but will be difficult to reproduce, calibrate, test, parallelize, or reason about.

This document defines the architecture for:

- random-number generation,
- deterministic seed derivation,
- random-stream ownership,
- probability distributions,
- weighted selection,
- long-tail distributions,
- domain-specific behavioral models,
- configuration-driven distribution selection,
- reproducibility guarantees,
- deterministic parallel generation,
- statistical validation,
- failure handling,
- testing,
- migration from the current implementation.

The central principle is:

> **Randomness provides entropy; distributions provide statistical shape; domain behavior provides business meaning.**

These three concerns must remain distinct.

---

# 2. Why This Is a Cross-Cutting Architecture

Randomness is used by nearly every domain.

A naive implementation might contain code such as:

```scala
random.nextDouble()
random.nextInt(10)
random.nextBoolean()
```

throughout generators.

That quickly creates several problems:

```text
business meaning becomes hidden
probabilities become duplicated
tests become fragile
parallel execution changes results
small code changes alter unrelated records
configuration becomes difficult
statistical calibration becomes difficult
```

The project therefore needs a deliberate randomness architecture.

---

# 3. Three-Layer Mental Model

Every probabilistic decision should be understood through three layers.

## Layer 1 — Randomness

Produces deterministic pseudo-random values.

Examples:

```text
uniform double
uniform integer
derived random stream
```

## Layer 2 — Distribution

Transforms random values into statistical behavior.

Examples:

```text
weighted categorical
normal
log-normal
Pareto
Zipf
negative binomial
Bernoulli
```

## Layer 3 — Domain behavior

Assigns business meaning to the distribution.

Examples:

```text
CustomerActivityModel
OrderFrequencyModel
BasketSizeModel
ProductPopularityModel
ReturnProbabilityModel
```

---

# 4. Separation of Responsibilities

The following should remain separate:

```text
RandomGenerator
Distribution
Domain Strategy
```

For example:

```text
RandomGenerator
       ↓
LogNormalDistribution
       ↓
CustomerSpendingModel
```

The random generator should not know anything about customer spending.

The distribution should not know anything about customers.

The customer spending model gives business meaning to the sampled value.

---

# 5. Randomness Is Infrastructure

A random-number abstraction should provide operations such as:

```text
nextDouble
nextInt
nextLong
derive
```

It should not provide methods such as:

```text
chooseCustomer
chooseProduct
choosePaymentMethod
generateOrder
```

Those are domain responsibilities.

---

# 6. Current RandomGenerator Direction

The existing code already uses an abstraction conceptually similar to:

```scala
RandomGenerator
```

and supports derived streams such as:

```scala
random.derive("building")
random.derive("floor")
random.derive("postal-code")
```

This is a strong architectural direction and should be retained.

The migration should improve and formalize it rather than replace it with direct `scala.util.Random` usage.

---

# 7. Root Seed

Every generation run starts with a root seed.

Conceptually:

```text
rootSeed = 42
```

The root seed is part of the reproducibility contract.

For identical:

```text
seed
configuration
reference data
generator version
```

the project should aim to generate identical logical data.

---

# 8. Root Random Stream

The application creates one root deterministic random source:

```text
RootRandom(seed)
```

Domains should derive their own streams rather than consume one shared mutable sequence.

Conceptually:

```text
root
 ├── customer
 ├── address
 ├── product
 ├── session
 ├── event
 ├── order
 ├── order-item
 ├── payment
 ├── shipment
 └── return
```

---

# 9. Why Shared Mutable Random Is Dangerous

Suppose generation uses:

```scala
val random = new Random(seed)
```

and every domain consumes that object.

Originally:

```text
Customer consumes 10 random values
Order consumes next 20
Product consumes next 10
```

Later Customer generation adds one random decision.

Now every downstream random value shifts.

The same seed produces a completely different dataset.

This is known as random-stream coupling.

---

# 10. Random-Stream Isolation

Derived random streams reduce this coupling.

Conceptually:

```scala
val customerRandom = root.derive("customer")
val productRandom  = root.derive("product")
val orderRandom    = root.derive("order")
```

Changing Product generation should not alter Customer generation.

---

# 11. Entity-Level Derivation

Domain-level isolation is not sufficient for large-scale deterministic generation.

Prefer entity-level derivation.

Conceptually:

```text
root
  ↓
customer
  ↓
CUSTOMER_000001
```

Then:

```text
CUSTOMER_000001
 ├── identity
 ├── demographics
 ├── acquisition
 └── behavior
```

---

# 12. Stable Derivation Keys

Derivation keys should represent stable business identity.

Good examples:

```text
customer/CUSTOMER_000001
order/ORDER_000004221
session/SESSION_000005432
```

Less desirable:

```text
loop-17
iteration-3
temporary-index
```

unless those indices are themselves stable identifiers.

---

# 13. Hierarchical Random Streams

A useful structure is:

```text
root
  └── customer
       └── CUSTOMER_000001
            ├── identity
            ├── acquisition
            ├── demographics
            ├── preferences
            └── behavior
```

For an Order:

```text
root
  └── order
       └── ORDER_000001
            ├── timestamp
            ├── status
            ├── basket-size
            └── payment-context
```

---

# 14. Semantic Derivation Keys

Prefer semantic keys.

Good:

```scala
random.derive("basket-size")
random.derive("product-selection")
random.derive("return-probability")
```

Weak:

```scala
random.derive("a")
random.derive("r2")
random.derive("step7")
```

Semantic names improve readability and debugging.

---

# 15. Deterministic Seed Derivation

A derived random stream should be computed deterministically from:

```text
parent seed
+
derivation key
```

Conceptually:

```text
childSeed = stableHash(parentSeed, key)
```

The hashing algorithm must be stable across runs.

---

# 16. Avoid Runtime-Dependent Hashing

Do not rely blindly on hashing behavior that could vary between:

```text
JVM versions
Scala versions
processes
platforms
```

if reproducibility depends on it.

The derivation algorithm should be explicitly chosen and tested.

---

# 17. Seed Derivation Contract

The project should define:

```text
derive(seed, key) → deterministic child seed
```

Properties:

```text
same parent + same key = same child
different key = different deterministic child
independent call order
```

---

# 18. RandomGenerator Interface

A focused abstraction might conceptually expose:

```scala
trait RandomGenerator {
  def nextDouble(): Double
  def nextInt(bound: Int): Int
  def nextInt(minInclusive: Int, maxInclusive: Int): Int
  def nextLong(): Long
  def derive(key: String): RandomGenerator
}
```

This is illustrative, not a requirement to change the current interface immediately.

---

# 19. Interface Design Rule

Only expose primitive randomness required by distribution infrastructure.

Avoid domain-specific methods.

This follows the Interface Segregation Principle.

---

# 20. Range Semantics

Range semantics must be unambiguous.

For example:

```text
nextInt(minInclusive, maxInclusive)
```

must clearly document whether the maximum is:

```text
inclusive
```

or:

```text
exclusive
```

Ambiguous ranges create subtle generation defects.

---

# 21. Probability Semantics

Probability should have one project-wide convention:

```text
0.0 <= probability <= 1.0
```

Examples:

```text
0.00 = impossible
0.25 = 25%
1.00 = certain
```

---

# 22. Probability Validation

All probability configuration should validate:

```text
0.0 <= p <= 1.0
```

Fail early on:

```text
-0.1
1.2
NaN
Infinity
```

---

# 23. Bernoulli Decisions

Many business decisions are Bernoulli trials:

```text
customer returns item?
session converts?
payment succeeds?
address has optional property?
```

Conceptually:

```text
sample p
→ true / false
```

This statistical mechanism should remain separate from business naming.

---

# 24. Weighted Categorical Selection

Many domains require categorical selection.

Examples:

```text
device
payment method
traffic channel
order status
category
brand
return reason
```

A reusable weighted distribution is appropriate.

---

# 25. Weighted Distribution Contract

Given:

```text
A → 0.50
B → 0.30
C → 0.20
```

sampling should approximately converge to those proportions at large scale.

---

# 26. Weight vs Probability

The system should distinguish:

```text
normalized probability
```

from:

```text
relative weight
```

For example:

```text
A = 5
B = 3
C = 2
```

can be normalized to:

```text
0.5
0.3
0.2
```

Supporting relative weights can make configuration easier.

---

# 27. Weight Validation

Weighted distributions should reject:

```text
negative weights
NaN
Infinity
all-zero weights
empty distributions
```

Zero weight for one category may be allowed if explicitly intended.

---

# 28. Normalization

Normalization should happen in one place.

Avoid every domain independently writing:

```scala
weight / weights.sum
```

A shared distribution component should own normalization.

---

# 29. Deterministic Weighted Selection

Given:

```text
same distribution
same random stream
```

weighted selection must produce the same sequence.

This must be covered by tests.

---

# 30. Uniform Distribution

Uniform selection is appropriate when every candidate genuinely has equal probability.

Examples may include:

```text
test fixture selection
some reference-data assignments
controlled baseline scenarios
```

Uniform should not be the default simply because it is easy.

---

# 31. Real E-Commerce Is Rarely Uniform

Real behavior typically exhibits:

```text
long tails
heavy concentration
heterogeneous users
seasonality
correlation
```

Therefore many production-like generators should use non-uniform distributions.

---

# 32. Long-Tail Behavior

Long-tail distributions are important for:

```text
product popularity
customer order frequency
customer session frequency
customer spending
brand popularity
return concentration
```

---

# 33. Zipf Distribution

Zipf-like distributions are useful for ranked popularity.

Conceptually:

```text
rank 1  → very popular
rank 2  → less popular
rank 10 → much less popular
rank 1000 → rare
```

Potential use:

```text
ProductPopularityModel
```

---

# 34. Pareto Distribution

Pareto-like behavior models concentration such as:

```text
small percentage of customers
produce large percentage of orders
```

Potential uses:

```text
customer activity
customer spend
hot-key behavior
```

---

# 35. Log-Normal Distribution

Log-normal distributions are often useful for positive-valued quantities with a right tail.

Potential uses:

```text
customer spending tendency
order monetary scale
session duration
delivery duration
```

Domain calibration is still required.

---

# 36. Normal Distribution

Normal distributions may be useful for bounded or transformed variables around a central value.

However, direct normal sampling can generate invalid negative values for positive-only business quantities.

Use it deliberately.

---

# 37. Truncated Distribution

For bounded business values, truncation may be required.

Example:

```text
session duration
```

could have:

```text
minimum = 5 seconds
maximum = 4 hours
```

A truncated distribution can enforce the business range.

---

# 38. Negative Binomial

Negative-binomial-like models can be useful for overdispersed count data.

Potential examples:

```text
orders/customer
sessions/customer
events/session
```

when variance is greater than the mean.

---

# 39. Poisson Distribution

Poisson can model count processes under specific assumptions.

Potential uses:

```text
events in time
orders during a period
```

But real e-commerce activity is often more heterogeneous than simple Poisson assumptions.

---

# 40. Mixture Distributions

Some populations are better modeled as mixtures.

Example customer activity:

```text
60% low activity
30% medium activity
10% high activity
```

Each segment can then have its own distribution.

This is often easier to reason about than one complex mathematical distribution.

---

# 41. Segmentation as Distribution Architecture

Customer segments can be represented as latent classes:

```text
LOW_ACTIVITY
REGULAR
HIGH_ACTIVITY
POWER_USER
```

Then:

```text
segment
   ↓
behavioral parameter distributions
```

This produces explainable heterogeneity.

---

# 42. Distribution Selection Is a Business Decision

Do not choose:

```text
Zipf
Pareto
log-normal
```

because they sound sophisticated.

Choose them because their shape matches the business behavior being modeled.

---

# 43. Distribution Abstraction

A reusable abstraction might conceptually be:

```scala
trait Distribution[A] {
  def sample(random: RandomGenerator): A
}
```

Examples:

```text
WeightedDistribution[String]
UniformDistribution[Int]
LogNormalDistribution
```

---

# 44. Domain Strategies Consume Distributions

Example:

```text
OrderFrequencyModel
   ↓
Distribution[Int]
```

The model interprets the sample as:

```text
number of orders for a customer
```

---

# 45. Distribution Should Not Read Global Config

Prefer:

```text
ConfigLoader
   ↓
validated configuration
   ↓
DistributionFactory
   ↓
Distribution
```

rather than distributions directly reading HOCON.

This improves testability.

---

# 46. Distribution Factory

A factory is justified when configuration selects a distribution implementation.

Conceptually:

```hocon
orders-per-customer {
  distribution = "negative-binomial"
  ...
}
```

Factory:

```text
DistributionFactory
   ↓
NegativeBinomialDistribution
```

---

# 47. Avoid String Logic Throughout Domains

Bad:

```scala
if (config.distribution == "zipf") ...
else if (config.distribution == "pareto") ...
```

inside every generator.

Centralize construction.

---

# 48. Typed Distribution Configuration

Prefer typed configuration objects.

Conceptually:

```text
ZipfConfig
ParetoConfig
LogNormalConfig
WeightedConfig
```

rather than unstructured maps.

---

# 49. Distribution Parameter Validation

Every distribution should validate its mathematical constraints.

Examples:

```text
standard deviation > 0
shape > 0
scale > 0
min <= max
weights non-negative
```

Fail before generation.

---

# 50. Business Validation vs Mathematical Validation

Two validation levels exist.

Mathematical:

```text
sigma > 0
```

Business:

```text
average basket size should not be 500
```

Both matter.

---

# 51. Domain-Level Distribution Wrappers

A domain model can wrap generic distributions.

Example:

```text
BasketSizeModel
```

may internally use:

```text
NegativeBinomialDistribution
```

but expose business language:

```text
sampleBasketSize(...)
```

This keeps generators readable.

---

# 52. Readability Standard

Prefer:

```scala
val basketSize =
  basketSizeModel.sample(customerProfile, random)
```

over:

```scala
val basketSize =
  negativeBinomial.sample(random)
```

inside the Order generator.

The first communicates business intent.

---

# 53. Customer Behavior Profile

The CustomerBehaviorProfile is a key consumer of distributions.

Potential properties:

```text
activity level
purchase frequency
spending tendency
price sensitivity
category affinity
brand affinity
payment preference
return propensity
```

---

# 54. Generate Latent Behavior Once

The behavior profile should be generated once per Customer.

Then downstream domains consume it.

This avoids independently sampling contradictory behavior.

---

# 55. Example of Contradictory Sampling

Weak architecture:

```text
SessionGenerator randomly decides customer is highly active
OrderGenerator randomly decides same customer is inactive
EventGenerator randomly decides same customer is highly engaged
```

This produces incoherent synthetic customers.

---

# 56. Correlated Behavior

Better:

```text
CustomerBehaviorProfile
   activity = HIGH
   purchaseFrequency = MEDIUM_HIGH
   engagement = HIGH
```

Then:

```text
SessionGenerator
OrderGenerator
EventGenerator
```

consume the same behavioral state.

---

# 57. Correlated Variables

Not all behavioral variables should be independently sampled.

Examples:

```text
income ↔ spending
activity ↔ sessions
activity ↔ events
category affinity ↔ product selection
price sensitivity ↔ product price tier
return propensity ↔ return rate
```

---

# 58. Conditional Distributions

Correlation can be modeled using conditional distributions.

Example:

```text
customer segment
      ↓
spending distribution
```

or:

```text
category
   ↓
brand distribution
```

---

# 59. Nested Weighted Distributions

Existing configuration already supports concepts similar to nested weights.

This is useful for:

```text
Category → Brand weights
Customer segment → Device weights
Channel → Conversion weights
```

---

# 60. Conditional Selection Example

Conceptually:

```scala
val category =
  categoryAffinityModel.select(customerProfile, random)

val brand =
  brandAffinityModel.select(category, customerProfile, random)

val product =
  productPopularityModel.select(
    category,
    brand,
    customerProfile,
    random
  )
```

This creates explainable correlation.

---

# 61. Distribution Composition

Complex behavior can be composed from simpler distributions.

Example:

```text
Customer segment
   ↓
Order frequency distribution
   ↓
Basket-size distribution
   ↓
Price-tier distribution
```

No single giant distribution is required.

---

# 62. Temporal Distributions

Time requires dedicated statistical treatment.

Potential examples:

```text
hour-of-day activity
day-of-week activity
seasonality
campaign periods
inter-arrival times
delivery delays
return delays
```

---

# 63. Hour-of-Day Distribution

E-commerce traffic is not uniformly distributed across 24 hours.

A future model can use:

```text
hour → weight
```

conditioned by:

```text
customer segment
device
channel
geography
```

---

# 64. Day-of-Week Distribution

Activity may vary by:

```text
weekday
weekend
```

This can influence:

```text
sessions
orders
delivery
```

---

# 65. Seasonal Distribution

A scenario can modify baseline demand over time.

Conceptually:

```text
baseline activity
    ×
seasonality factor
    ×
campaign factor
```

---

# 66. Inter-Arrival Times

Instead of selecting independent timestamps uniformly, mature models can generate sequences using inter-arrival times.

Example:

```text
Session 1
  + 2 days
Session 2
  + 5 hours
Session 3
```

This better represents activity processes.

---

# 67. Time Randomness Must Be Causal

Random timestamps must respect domain chronology.

For example:

```text
session start
   ≤
event
   ≤
order
   ≤
payment
   ≤
shipment
   ≤
delivery
   ≤
return
```

Randomness cannot override causal constraints.

---

# 68. Bounded Sampling

When a sampled value must fit a business range, define the policy explicitly.

Possible approaches:

```text
clamp
truncate
resample
transform
```

Do not silently choose different policies in different domains.

---

# 69. Clamping

Clamping:

```text
sample = 150
max = 100
result = 100
```

is simple but can create artificial spikes at boundaries.

Use carefully.

---

# 70. Rejection Sampling

Resampling until a value falls inside the allowed range can preserve distribution shape better.

But it requires:

```text
termination safeguards
```

for impossible or extremely unlikely ranges.

---

# 71. Truncation

A mathematically truncated distribution is preferable when supported.

It avoids repeated rejection and boundary spikes.

---

# 72. Integer Conversion

Continuous distributions often produce decimal values while business counts require integers.

The conversion policy should be explicit:

```text
round
floor
ceil
```

Different choices alter distribution characteristics.

---

# 73. Minimum Cardinality

For relationships such as:

```text
Order → OrderItem
```

the business model may require:

```text
minimum 1
```

The distribution model should encode that requirement explicitly.

---

# 74. Zero-Inflated Distributions

Some counts naturally have many zeros.

Examples:

```text
returns/order
orders/customer in a period
failed payments/order
```

A zero-inflated model may be appropriate.

Conceptually:

```text
Bernoulli occurrence
       ↓
positive-count distribution
```

---

# 75. Return Example

Instead of:

```text
returns/order ~ generic count
```

use:

```text
eligible?
   ↓
returns?
   ↓
how many items?
```

This preserves business meaning.

---

# 76. Distribution Calibration

A distribution is useful only if its output resembles the intended business world.

Calibration should compare generated statistics against target ranges.

---

# 77. Calibration Metrics

Depending on domain:

```text
mean
median
variance
percentiles
concentration
conversion rate
return rate
category share
brand share
AOV
```

---

# 78. Distribution Validation Is Statistical

A test should not require exactly:

```text
UPI = 45%
```

for a finite random sample.

It should verify the observed proportion is within a reasonable tolerance.

---

# 79. Statistical Test Sample Size

Small samples produce high variance.

Distribution tests should use enough samples to make failures meaningful while keeping the test suite fast.

---

# 80. Deterministic Statistical Tests

Because the random seed is fixed, statistical tests can remain deterministic.

For example:

```text
seed = known constant
sample size = 100000
```

Then the observed statistics should remain stable.

---

# 81. Avoid Brittle Exact Distribution Tests

Do not test every generated count exactly unless exact determinism is the contract.

Prefer:

```text
expected statistical range
```

for behavioral validation.

---

# 82. Reproducibility Definition

The project should define reproducibility precisely.

Recommended definition:

> Given the same generator version, Scala/JVM compatibility assumptions, root seed, configuration, and reference data, ShopSphere should produce the same logical dataset.

---

# 83. Logical vs Byte-for-Byte Reproducibility

Two levels exist.

## Logical reproducibility

Same records and values.

## Byte-for-byte reproducibility

Exactly identical output bytes.

Byte equality additionally depends on:

```text
record ordering
line endings
CSV formatting
floating-point formatting
file partitioning
metadata
```

The project should distinguish these.

---

# 84. Recommended Baseline Guarantee

Initially guarantee:

```text
deterministic logical records
+
deterministic ordering for CSV
```

This makes byte-level equality achievable for most current outputs.

---

# 85. Reproducibility Inputs

A manifest should eventually record:

```text
generator version
Scala version
JVM version
seed
configuration fingerprint
reference-data fingerprint
scenario
profile
generation timestamp
```

The generation timestamp itself should not alter generated business data unless explicitly configured.

---

# 86. Configuration Fingerprint

A stable fingerprint of effective configuration helps identify whether two runs are comparable.

Conceptually:

```text
SHA-256(canonical effective configuration)
```

---

# 87. Reference-Data Fingerprint

Reference data affects generation.

Therefore reproducibility requires identifying:

```text
which geography/catalog reference data
```

was used.

---

# 88. Manifest Reproducibility Section

Future manifest example:

```text
reproducibility:
  seed: 42
  generatorVersion: ...
  configurationHash: ...
  referenceDataHash: ...
```

---

# 89. Deterministic Record IDs

Record identifiers should generally be deterministic.

Current patterns such as:

```text
CUSTOMER_000000001
ADDRESS_000000001
```

are useful.

Avoid random UUIDs when deterministic IDs are sufficient.

---

# 90. UUID Consideration

If UUIDs are later required, use deterministic UUID generation when reproducibility matters.

Do not call:

```scala
UUID.randomUUID()
```

inside deterministic generation.

---

# 91. Deterministic Record Ordering

Collections with unstable iteration order can break reproducibility.

Be careful with:

```text
HashMap
HashSet
```

when iteration order influences sampling or output.

---

# 92. Sorting Reference Candidates

Before indexed selection, use stable ordering where necessary.

Example already used in Address generation:

```scala
geography.buildings.values.toSeq.sortBy(_.id)
```

This is a good reproducibility practice.

---

# 93. Stable Output Ordering

CSV writers should receive records in deterministic order.

Examples:

```text
Customer by ID
Order by ID
Event by ID
```

if the generation model does not already guarantee order.

---

# 94. Parallel Generation Challenge

Parallelism can alter execution order.

If random values depend on execution order, results become nondeterministic.

Derived entity streams solve much of this problem.

---

# 95. Parallel-Safe Randomness

Conceptually:

```text
CUSTOMER_000001 → seed A
CUSTOMER_000002 → seed B
CUSTOMER_000003 → seed C
```

Then customers can be generated:

```text
serially
or
in parallel
```

without changing their values.

---

# 96. Partition Independence

For future large-scale generation, aim for:

```text
entity output independent of worker assignment
```

For example:

```text
Customer 1000
```

should receive the same derived random stream regardless of which worker generates it.

---

# 97. Scale Independence

Where practical, existing entity records should remain stable when total cardinality increases.

Example:

```text
1,000-customer run
```

and:

```text
10,000-customer run
```

could generate identical first 1,000 customers.

This is a useful property for debugging.

---

# 98. Scale-Independence Limitation

Not every model can preserve scale independence.

Rank-based global distributions may depend on:

```text
population size
```

The project should document where this property is guaranteed and where it is not.

---

# 99. Reproducibility and Reference Candidate Count

If Product selection is:

```text
random.nextInt(products.size)
```

adding Products can change selections even with the same seed.

This is expected unless stronger stability is designed.

Reproducibility should therefore be defined against the same reference data.

---

# 100. Floating-Point Determinism

Most current JVM floating-point operations are deterministic enough for this project, but avoid unnecessary dependence on unstable iteration order or platform-specific behavior.

Financial calculations should continue to use appropriate decimal semantics where required.

---

# 101. Monetary Randomness

Do not generate monetary amounts by arbitrary random decimal multiplication.

Price generation should have domain structure:

```text
category
brand
price tier
base price
promotion
customer sensitivity
```

Randomness should support the model, not replace it.

---

# 102. Price Distribution

Product base prices may use category-conditioned distributions.

Conceptually:

```text
Category
   ↓
PriceTierModel
   ↓
PriceDistribution
   ↓
Product base price
```

---

# 103. Transaction Price

OrderItem transaction price should derive from Product price and transaction context.

Conceptually:

```text
Product base price
   ↓
promotion
   ↓
customer discount
   ↓
transaction price
```

Do not independently randomize the final line price.

---

# 104. Product Popularity Distribution

A ProductPopularityModel may use:

```text
Zipf
weighted popularity
category-conditioned rank
campaign boost
```

The output should remain a Product choice, not a raw numeric rank.

---

# 105. Customer Activity Distribution

CustomerActivityModel can determine:

```text
sessions/customer
orders/customer
event engagement
```

through shared latent activity.

This is preferable to three independent distributions.

---

# 106. Basket-Size Distribution

The current generated dataset has approximately:

```text
mean = 2.40
min = 2
max = 3
```

This range is too narrow for mature realism.

A future BasketSizeModel should allow:

```text
1-item orders
typical small baskets
occasional larger baskets
```

with a right tail.

---

# 107. Session-Frequency Distribution

The current dataset generates exactly:

```text
3 Sessions/customer
```

A future model should create heterogeneous activity.

Example conceptual population:

```text
inactive/light
regular
heavy
```

---

# 108. Event-Count Distribution

The current dataset generates exactly:

```text
8 Events/session
```

A future model should make event count emerge from:

```text
journey type
session intent
engagement
conversion
```

rather than a fixed count.

---

# 109. Order-Frequency Distribution

Customer purchase frequency should be heterogeneous.

Potential model:

```text
customer activity
+
lifecycle
+
campaign exposure
+
time window
```

→ number of Orders.

---

# 110. Return Distribution

Returns should not be a simple global Bernoulli probability forever.

Future model:

```text
customer propensity
+
product propensity
+
category propensity
+
delivery experience
```

→ return probability.

---

# 111. Distribution Configuration Hierarchy

Configuration can be layered:

```text
global defaults
   ↓
profile overrides
   ↓
scenario overrides
   ↓
domain-specific settings
```

The effective configuration should be resolved before generation.

---

# 112. Effective Configuration

Generators should consume:

```text
resolved typed configuration
```

not repeatedly inspect multiple config layers.

This improves readability and consistency.

---

# 113. Configuration Override Precedence

Override precedence must be explicit.

For example:

```text
base
< profile
< scenario
< explicit run override
```

if the project chooses that order.

The exact rule should be documented and tested.

---

# 114. Scenario Distribution Modification

Scenarios should modify parameters, not bypass the architecture.

Example:

```text
normal ProductPopularityModel
```

plus:

```text
hot-product scenario
```

may change:

```text
Zipf exponent
specific product boost
```

rather than injecting unrelated random rows.

---

# 115. Distribution Observability

Generation statistics should report the realized distribution.

For example:

```text
configured payment weights
vs
observed payment proportions
```

This helps detect configuration or implementation mistakes.

---

# 116. Distribution Diagnostics

Useful diagnostics include:

```text
sample count
mean
variance
min
max
percentiles
top-k concentration
category shares
```

---

# 117. Concentration Metrics

For skewed relationships, report:

```text
top 1% share
top 5% share
top 10% share
```

Examples:

```text
Orders by Customer
OrderItems by Product
Events by Customer
```

---

# 118. Entropy Diagnostics

For categorical distributions, entropy can eventually help detect whether data is:

```text
too uniform
```

or:

```text
overly concentrated
```

This is optional advanced validation.

---

# 119. Reproducibility Tests

Core tests should verify:

```text
same seed → same result
different seed → meaningfully different result
derived key → stable stream
different derived key → different stream
```

---

# 120. Stream Isolation Test

Important test:

1. Generate Customer with current implementation.
2. Add or consume randomness in an unrelated Product stream.
3. Regenerate Customer.
4. Customer must remain unchanged.

This validates stream isolation.

---

# 121. Entity Isolation Test

For entity-derived streams:

```text
Customer 1
Customer 2
Customer 3
```

generating Customer 2 before Customer 1 should not change either record.

---

# 122. Parallel Determinism Test

Generate the same set:

```text
serially
```

and:

```text
in parallel
```

Then compare logical records.

They should match where the architecture promises parallel determinism.

---

# 123. Distribution Unit Tests

Every reusable distribution should test:

```text
parameter validation
range behavior
determinism
statistical shape
boundary cases
```

---

# 124. Weighted Distribution Tests

Test:

```text
empty weights rejected
negative weights rejected
all-zero weights rejected
single value always selected
same seed reproducible
large sample approximates expected proportions
```

---

# 125. Long-Tail Distribution Tests

For Zipf/Pareto-like models, test characteristics rather than exact formulas alone.

Example:

```text
rank 1 selected more than rank 10
top 10 share > lower-ranked share
```

plus mathematical correctness tests.

---

# 126. Domain Behavior Tests

A ProductPopularityModel test should speak in business language.

Example:

```text
popular products receive more selections than tail products
```

not merely:

```text
Zipf sampler returns expected rank
```

Both levels can exist.

---

# 127. Statistical Regression Tests

The project can maintain a small deterministic benchmark profile.

For a fixed seed/configuration, record important statistics:

```text
AOV
items/order
sessions/customer
events/session
return rate
product concentration
```

Large unexpected shifts should fail or flag regression.

---

# 128. Avoid Overfitting Regression Tests

Do not freeze every statistic forever.

Intentional realism improvements will change distributions.

Regression baselines should be versioned or updated deliberately.

---

# 129. Randomness Logging

Do not log every random number.

That would be enormous and unhelpful.

Instead log:

```text
root seed
derived domain seeds if debugging requires them
configuration
scenario
distribution parameters
```

---

# 130. Debugging One Entity

A useful debugging capability is:

```text
reproduce CUSTOMER_000123
```

using:

```text
root seed
entity ID
configuration
```

without regenerating the entire dataset.

Hierarchical derivation makes this feasible.

---

# 131. Debugging One Relationship

Likewise:

```text
ORDER_000456/product-selection
```

could be reproduced from its semantic random stream.

This is a major operational advantage.

---

# 132. Randomness Versioning

Changing the seed-derivation algorithm can change every generated record.

Therefore the algorithm is effectively part of the generator's reproducibility version.

---

# 133. Distribution Versioning

Changing a distribution implementation may also alter generated records.

The manifest should identify the generator version sufficiently to explain these differences.

---

# 134. Compatibility Policy

The project does not need indefinite backward reproducibility for every historical implementation.

But changes that intentionally break reproducibility should be explicit.

---

# 135. Scala Version

The project standard remains:

```text
Scala 2.13
```

The randomness architecture must remain compatible with Scala 2.13.

Do not introduce Scala 3-only APIs or syntax.

---

# 136. Java Random APIs

The implementation may use JVM random primitives internally, but they should remain behind the project's RandomGenerator abstraction.

This prevents domain code from depending on a specific random library.

---

# 137. Dependency Rule

Domain packages should depend on abstractions such as:

```text
RandomGenerator
Distribution
```

rather than:

```text
scala.util.Random
java.util.Random
```

directly.

---

# 138. Package Architecture

A suitable cross-cutting structure is:

```text
common/
  random/
    RandomGenerator.scala
    SeedDeriver.scala

  distribution/
    Distribution.scala
    WeightedDistribution.scala
    UniformDistribution.scala
    ...

  time/
    ...
```

Exact files should be introduced only when required.

Do not create empty packages for planned concepts.

---

# 139. Distribution vs Domain Package

Generic mathematical distributions belong in:

```text
common/distribution
```

Business models belong in domain packages.

Example:

```text
common/distribution/ZipfDistribution
```

versus:

```text
product/behavior/ProductPopularityModel
```

---

# 140. Factory Location

Generic distribution factories can live with distribution infrastructure.

Domain strategy factories should live with their domain.

Example:

```text
product/behavior/ProductPopularityModelFactory
```

if configuration selects multiple implementations.

---

# 141. SOLID Analysis — SRP

Single Responsibility Principle:

```text
RandomGenerator → random primitives
Distribution → statistical sampling
BehaviorModel → business interpretation
Generator → entity construction
ConfigLoader → configuration parsing
```

This is the intended separation.

---

# 142. SOLID Analysis — OCP

Open/Closed Principle:

New distributions should be addable without rewriting all domain generators.

New behavior strategies should be addable without changing infrastructure.

---

# 143. SOLID Analysis — LSP

Any implementation of:

```text
Distribution[A]
```

should obey its sampling contract.

Any implementation of:

```text
RandomGenerator
```

should obey deterministic derivation semantics if the interface promises them.

---

# 144. SOLID Analysis — ISP

Do not force every domain to depend on a huge random API.

Expose only required operations.

---

# 145. SOLID Analysis — DIP

Business behavior depends on abstractions:

```text
RandomGenerator
Distribution
```

rather than concrete random libraries.

---

# 146. Design Pattern — Strategy

Strategy is highly appropriate for:

```text
CustomerActivityModel
OrderFrequencyModel
BasketSizeModel
ProductPopularityModel
ReturnProbabilityModel
```

when multiple behaviors are configurable.

---

# 147. Design Pattern — Factory

Factory is appropriate where configuration selects strategies or distributions.

Example:

```text
"zipf"
   ↓
ProductPopularityModelFactory
   ↓
ZipfProductPopularityModel
```

---

# 148. Design Pattern — Composition

Composition should be the default.

Example:

```text
OrderGenerator
   ├── OrderFrequencyModel
   ├── OrderTimeModel
   ├── OrderStatusModel
   └── BasketSizeModel
```

rather than one giant inheritance hierarchy.

---

# 149. Avoid Template-Method Overuse

Do not build a complex base class such as:

```text
AbstractEntityGenerator
```

unless multiple domains genuinely share a stable algorithm.

Customer, Product, Event, and Shipment generation have very different business flows.

Composition is safer.

---

# 150. Avoid Generic Distribution DSL Too Early

A powerful generic distribution DSL may eventually be useful.

But implementing one before actual domain needs are clear would create unnecessary abstraction.

Start with the distributions required by current realism work.

---

# 151. Failure Philosophy

Invalid statistical configuration should fail:

```text
early
clearly
with business context
```

Example:

```text
Invalid order-frequency configuration:
minimum orders cannot exceed maximum orders.
```

Better than:

```text
IllegalArgumentException
```

without context.

---

# 152. Empty Candidate Selection

A distribution cannot select from an empty candidate set.

Fail explicitly.

Example:

```text
Cannot select Product because no eligible products exist for category X.
```

---

# 153. Impossible Conditional Distribution

If:

```text
Customer prefers Category X
```

but Category X has no Products, define the fallback policy explicitly.

Possible policies:

```text
fail
fallback to another category
fallback to global catalog
```

Do not silently invent one.

---

# 154. Fallbacks Affect Realism

Fallback behavior can distort distributions.

Therefore fallback counts should eventually be observable.

Example:

```text
product selection fallback count = 127
```

---

# 155. Randomness and Data Quality

Data-quality corruption should have its own derived streams.

Conceptually:

```text
root
  └── quality
       ├── missing-values
       ├── invalid-reference
       └── malformed-record
```

This prevents enabling a quality scenario from changing unrelated clean business generation.

---

# 156. Randomness and Skew

Skew scenarios should likewise use isolated streams.

```text
root
  └── scenario
       └── hot-product
```

---

# 157. Scenario Isolation

Ideally:

```text
baseline generation
```

and:

```text
baseline + quality corruption
```

produce the same clean values before corruption is applied.

This makes scenario comparison easier.

---

# 158. Randomness and Validation

Validation should never consume the same mutable random stream used for generation.

Validation should normally be deterministic and non-random.

If statistical sampling is used for very large validation, it should have an independent validation seed.

---

# 159. Randomness and Statistics

Statistics must observe generated data.

They must not regenerate expected values using generation randomness.

---

# 160. Performance Considerations

Distribution sampling may execute billions of times at large scale.

Therefore reusable distributions should avoid unnecessary allocations in hot loops.

But performance optimization should follow correctness and readability.

---

# 161. Precomputed Weighted CDF

For stable weighted distributions, cumulative weights can be precomputed.

Conceptually:

```text
A → 0.50
B → 0.80
C → 1.00
```

Then one random value selects the bucket.

---

# 162. Alias Method

For very large repeated categorical sampling, an alias table may improve performance.

Do not implement it until profiling shows weighted selection is a bottleneck.

---

# 163. Distribution Object Reuse

Immutable validated distribution objects should generally be reusable.

Avoid rebuilding:

```text
payment-method distribution
```

for every Payment.

---

# 164. Immutable Configuration

Distribution configuration should be immutable after loading.

This improves:

```text
thread safety
reproducibility
reasoning
```

---

# 165. Thread Safety

A mutable RNG instance is typically not safe for uncontrolled shared parallel access.

Entity-derived random instances should remain local to generation tasks.

---

# 166. Deterministic Partitioning

Future large-scale generation can assign stable ID ranges to partitions.

Example:

```text
partition 0 → Customer 1–100000
partition 1 → Customer 100001–200000
```

Each entity derives its own seed.

---

# 167. Reproducible Sharding

Output sharding should not determine random values.

Random values should be determined from:

```text
entity identity
```

not:

```text
output file number
```

---

# 168. Generation Plan Interaction

The GenerationPlan determines cardinalities.

Randomness determines individual outcomes within those planned constraints.

These responsibilities should remain separate.

---

# 169. Exact Counts vs Random Counts

Some cardinalities may be exact:

```text
total Customers = 1,000,000
```

while relationships are distributed:

```text
Orders/customer varies
```

The planner may need reconciliation so totals remain near or exactly at configured targets.

---

# 170. Reconciliation Strategies

If the configuration requires an exact total number of Orders while per-customer frequency is random, possible approaches include:

```text
sample then normalize
allocate weighted counts
generate until target
```

The chosen strategy should be explicit because it affects distributions.

---

# 171. Weighted Allocation

A useful approach for exact total allocation:

```text
Customer activity weights
        ↓
allocate N Orders across Customers
```

This can preserve:

```text
exact total Orders
+
heterogeneous customer activity
```

---

# 172. Largest-Remainder Allocation

For exact deterministic allocation, fractional expected counts can be converted to integer counts using a deterministic allocation method such as largest remainder.

This may be preferable to unconstrained random counts in some profiles.

---

# 173. Stochastic vs Exact Planning

The project should support both concepts:

```text
exact planned cardinality
```

and:

```text
stochastic relationship cardinality
```

depending on configuration.

---

# 174. Cardinality Profile Interaction

Existing:

```text
CardinalityProfile
```

controls scale.

Behavioral distributions should shape allocation within that scale rather than unintentionally overriding the profile.

---

# 175. Distribution Profile Interaction

A future profile might define:

```text
small scale
+
realistic behavior
```

or:

```text
large scale
+
heavy skew
```

Scale and behavior should remain orthogonal concepts where possible.

---

# 176. Randomness Naming Convention

Use derivation keys based on business concepts.

Examples:

```text
customer-profile
acquisition-channel
category-affinity
brand-affinity
order-frequency
basket-size
product-selection
payment-method
shipment-delay
return-decision
```

---

# 177. Avoid Reusing a Stream for Unrelated Decisions

Weak:

```scala
val r = random.derive("order")
```

then consume it sequentially for everything.

Better:

```scala
val timestampRandom = random.derive("timestamp")
val statusRandom = random.derive("status")
val basketRandom = random.derive("basket-size")
```

when independence and stability matter.

---

# 178. Do Not Over-Derive

Creating a separate derived stream for every trivial arithmetic operation would add noise.

Derive at meaningful semantic boundaries.

---

# 179. Reproducibility Granularity

A useful target is:

```text
domain
→ entity
→ major behavior
```

Example:

```text
order
→ ORDER_000001
→ basket-size
```

This is sufficiently fine-grained for debugging without excessive complexity.

---

# 180. Documentation Requirement

Every new probabilistic domain model should document:

```text
business meaning
distribution choice
parameters
bounds
dependencies
correlations
reproducibility key
validation metrics
```

---

# 181. Code Review Questions

For probabilistic code, reviewers should ask:

1. Why is this random?
2. What business behavior does it represent?
3. Why was this distribution selected?
4. Are parameters configurable?
5. Are bounds valid?
6. Is the stream deterministic?
7. Is it isolated from unrelated behavior?
8. Is correlation required?
9. How will realism be validated?
10. What happens under parallel generation?

---

# 182. Migration Strategy

Do not rewrite all randomness infrastructure at once.

The project currently passes the full test suite and generates valid data.

Migration should be incremental.

---

# 183. Migration Phase 1 — Inventory

Identify:

```text
all direct Random usage
all RandomGenerator usage
all derive keys
all weighted-selection implementations
all duplicated probability logic
```

No behavior change.

---

# 184. Migration Phase 2 — Formalize Random Core

Stabilize:

```text
RandomGenerator
SeedDeriver
derivation semantics
range semantics
```

Add dedicated tests.

---

# 185. Migration Phase 3 — Generic Distribution Core

Introduce only immediately needed reusable distributions.

Likely initial set:

```text
Bernoulli
Weighted categorical
Uniform
bounded count model support
```

Then add long-tail distributions as realism work requires them.

---

# 186. Migration Phase 4 — Customer Behavior

Customer should become the first major consumer of the new architecture.

Introduce:

```text
CustomerBehaviorProfile
```

with explainable latent behavior.

---

# 187. Migration Phase 5 — Session and Event

Use Customer behavior to replace:

```text
exact 3 Sessions/customer
exact 8 Events/session
```

with realistic heterogeneous behavior.

---

# 188. Migration Phase 6 — Orders and OrderItems

Introduce:

```text
OrderFrequencyModel
BasketSizeModel
ProductPopularityModel
```

and connect them to Customer behavior.

---

# 189. Migration Phase 7 — Monetary Calibration

Refine:

```text
Product pricing
Customer spending
OrderItem transaction pricing
AOV
```

until generated monetary statistics are plausible.

---

# 190. Migration Phase 8 — Returns

Introduce correlated:

```text
ReturnProbabilityModel
```

instead of a mostly global return probability.

---

# 191. Migration Phase 9 — Temporal Models

Introduce:

```text
seasonality
campaign effects
inter-arrival behavior
journey timing
shipment delays
```

incrementally.

---

# 192. Migration Phase 10 — Statistical Validation

Add automated checks for:

```text
distribution shape
concentration
correlation
percentiles
```

to protect realism.

---

# 193. Quality Gate — Random Core

Before migrating domains, the random core should guarantee:

- same seed reproducibility,
- stable derivation,
- documented range semantics,
- deterministic semantic streams,
- no domain business logic.

---

# 194. Quality Gate — Distribution Core

Each reusable distribution should have:

- parameter validation,
- deterministic sampling,
- range/boundary tests,
- statistical tests,
- immutable configuration.

---

# 195. Quality Gate — Domain Behavior

Each behavior model should have:

- business name,
- documented purpose,
- typed configuration,
- explicit dependencies,
- deterministic random stream,
- statistical validation,
- focused tests.

---

# 196. Quality Gate — Reproducibility

A complete run should record enough metadata to answer:

```text
Can this dataset be reproduced?
```

At minimum:

```text
seed
effective configuration
generator version
reference-data identity
```

---

# 197. Current Simplifications

The current project still contains several intentionally simple distributions.

Examples observed in the baseline include:

```text
3 Sessions/customer exactly
8 Events/session exactly
2–3 Items/order
limited behavioral correlation
high monetary values
```

These are acceptable as a functioning baseline.

They should now be replaced incrementally with explicit behavior models.

---

# 198. Future Realism

The target statistical architecture should eventually support:

```text
heterogeneous customer populations
long-tail product demand
correlated category/brand/product affinity
customer spending distributions
session and order frequency distributions
journey-conditioned event counts
temporal seasonality
campaign effects
return propensity
controlled skew
controlled data-quality corruption
```

---

# 199. Explicit Architecture Decisions

## Decision 1

Use a project-owned `RandomGenerator` abstraction.

Do not expose concrete random libraries throughout domain code.

## Decision 2

Use deterministic hierarchical random-stream derivation.

## Decision 3

Prefer semantic derivation keys.

## Decision 4

Separate randomness, mathematical distributions, and domain behavior.

## Decision 5

Generic distributions belong in cross-cutting infrastructure.

## Decision 6

Business strategies belong in domain packages.

## Decision 7

CustomerBehaviorProfile is the primary mechanism for propagating correlated customer behavior.

## Decision 8

Configuration selects behavior/distribution implementations through typed configuration and factories where variation is genuine.

## Decision 9

Do not use one shared mutable global RNG.

## Decision 10

Parallel generation must not depend on execution order where deterministic entity streams are feasible.

## Decision 11

Statistical validation is required for realism; referential validation alone is insufficient.

## Decision 12

Distribution choices must be justified by business behavior, not mathematical sophistication.

## Decision 13

Quality and skew scenarios receive isolated random streams.

## Decision 14

Reference-data ordering must be deterministic whenever it affects random selection.

## Decision 15

The architecture remains Scala 2.13 compatible.

---

# 200. Target Architecture

The target architecture can be summarized as:

```text
                        Root Seed
                            │
                            ▼
                    ┌───────────────┐
                    │ Seed Deriver  │
                    └───────┬───────┘
                            │
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
     Customer             Product            Order
       Stream              Stream             Stream
         │                  │                  │
         ▼                  ▼                  ▼
   Entity Streams      Entity Streams      Entity Streams
         │                  │                  │
         ▼                  ▼                  ▼
  RandomGenerator      RandomGenerator     RandomGenerator
         │                  │                  │
         └──────────────┬───┴──────────────────┘
                        ▼
                  Distributions
                        │
                        ▼
                 Behavior Models
                        │
                        ▼
                 Domain Generators
                        │
                        ▼
                  Generated Data
                        │
             ┌──────────┴──────────┐
             ▼                     ▼
        Validation             Statistics
```

---

# 201. Final Mental Model

When reading probabilistic generation code, the architecture should make it possible to answer three questions immediately.

### Where does uncertainty come from?

```text
RandomGenerator
```

### What statistical shape does it have?

```text
Distribution
```

### What business concept does it represent?

```text
Domain Behavior Model
```

For example:

```text
RandomGenerator
       ↓
Weighted Distribution
       ↓
PaymentMethodPreferenceModel
       ↓
Payment
```

or:

```text
RandomGenerator
       ↓
Long-tail Distribution
       ↓
ProductPopularityModel
       ↓
OrderItem
```

---

# 202. Final Principle

The most important principle is:

> **Never let a random-number call become a substitute for a business model.**

Randomness should make ShopSphere varied.

Distributions should make ShopSphere statistically plausible.

Behavior models should make ShopSphere understandable.

Deterministic seed derivation should make ShopSphere reproducible.

Together these form the statistical foundation required for the next stage of sophisticated realism.

---

# 203. Next Documentation Stage

The next cross-cutting document should define:

```text
18 — Configuration, Profiles, and Scenario Architecture
```

It should consolidate:

- HOCON ownership,
- typed configuration,
- configuration hierarchy,
- generation profiles,
- cardinality profiles,
- scenarios,
- overrides,
- validation,
- strategy selection,
- effective configuration,
- configuration fingerprinting,
- compatibility and evolution,
- domain-specific configuration boundaries,
- avoiding one giant configuration object.

That document will complete the configuration side of the architecture before the remaining cross-cutting documents on validation, quality/skew, output/storage, statistics, and observability.
