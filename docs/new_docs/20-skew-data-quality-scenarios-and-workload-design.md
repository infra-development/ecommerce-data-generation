# 20 — Skew, Data-Quality Scenarios, and Workload Design

## 1. Purpose

This document defines how ShopSphere should model intentional data skew, scenario-driven dataset shapes, controlled data-quality defects, and workload-oriented generation.

The purpose is not merely to make generated data look statistically interesting. The generated dataset must also be useful as a foundation for Spark performance experiments.

The design therefore has two related but distinct goals:

1. **Business realism**
   - customer activity should be heterogeneous;
   - product demand should be long-tailed;
   - some customers, products, categories, locations, campaigns, or time periods may naturally account for disproportionate activity.

2. **Workload realism**
   - the generated data should be able to create difficult but explainable Spark workloads;
   - joins should sometimes contain hot keys;
   - aggregations should sometimes concentrate records;
   - windows should operate over uneven partition sizes;
   - data-quality workloads should contain controlled defects;
   - temporal scenarios should create meaningful bursts.

These goals overlap, but they are not identical.

A dataset can be realistic without being sufficiently skewed to stress a distributed system. Conversely, artificial extreme skew can be useful for a performance experiment without representing ordinary business behavior.

The generator must therefore make the distinction explicit.

---

## 2. Core Principle

Skew is a **statistical property**, not a random corruption.

A hot product with millions of order items is not automatically bad data. It may represent a genuinely popular product.

A customer with unusually many sessions is not necessarily a data-quality defect. It may represent a highly active customer.

A category receiving a disproportionate amount of traffic may be legitimate because of a campaign.

Therefore:

> **Realistic skew belongs to the behavioral model; artificial stress skew belongs to an explicit scenario.**

This distinction is fundamental.

The system should be capable of generating:

- ordinary baseline distributions;
- naturally heterogeneous distributions;
- controlled hot-key distributions;
- temporal spikes;
- geographic concentration;
- campaign-driven concentration;
- intentionally dirty data;
- combinations of these conditions.

---

# 3. What "Skew" Means in ShopSphere

Skew occurs when records are distributed unevenly across values of a key or dimension.

Examples:

```text
customer_id
product_id
category_id
brand_id
city_id
area_id
postal_code
event_type
order_date
campaign_id
device_type
payment_method
```

A uniform distribution might look approximately like:

```text
A → 10%
B → 10%
C → 10%
D → 10%
...
```

A skewed distribution might look like:

```text
A → 42%
B → 18%
C → 10%
D → 7%
E → 5%
others → 18%
```

A long-tail distribution can be even more concentrated:

```text
top 1%       → very high activity
next 4%      → high activity
next 15%     → moderate activity
remaining 80%→ low activity
```

This shape is often more useful than simple uniform randomness.

---

# 4. Business Skew vs Workload Skew

## 4.1 Business skew

Business skew represents plausible customer or market behavior.

Examples:

- a few products sell much more than others;
- a small group of customers places many orders;
- some categories dominate demand;
- certain cities generate more transactions;
- mobile users generate more sessions than desktop users;
- some customers return products more frequently;
- campaign periods produce higher traffic.

Business skew should normally be the default direction for realism.

## 4.2 Workload skew

Workload skew intentionally exaggerates concentration to make distributed-processing behavior observable.

Examples:

```text
one product_id → 25% of all order items
```

or:

```text
one customer_id → 5% of all events
```

These values may be useful for testing Spark partition imbalance even when they are not realistic for an ordinary e-commerce population.

Workload skew must therefore be explicit and identifiable.

---

# 5. Skew Dimensions

The generator should support skew at multiple levels.

## 5.1 Customer skew

A small group of customers can generate a disproportionate amount of:

- orders;
- order items;
- sessions;
- events;
- spending;
- returns.

Example:

```text
80% customers → 20% activity
20% customers → 80% activity
```

The exact ratio should be configurable rather than hard-coded.

## 5.2 Product skew

Product popularity is one of the most important skew dimensions.

A catalog may contain:

```text
very small number of highly popular products
many moderately popular products
large number of rarely purchased products
```

This is preferable to assigning every product equal probability.

## 5.3 Category skew

Categories can have different demand levels.

For example:

```text
Electronics → high demand
Fashion     → high demand
Books       → medium demand
Specialty   → low demand
```

Category skew can also influence product popularity.

## 5.4 Brand skew

Some brands may dominate purchases while many smaller brands receive limited demand.

Brand demand should be conditioned by:

- category;
- customer preference;
- product popularity;
- campaign effects;
- price positioning.

## 5.5 Geographic skew

Traffic and orders can be concentrated in certain:

- countries;
- states;
- cities;
- areas.

Geographic skew should remain consistent with the geography hierarchy.

The system must not create impossible geographic combinations merely to produce skew.

## 5.6 Temporal skew

Activity can concentrate around:

- weekends;
- evenings;
- holidays;
- campaigns;
- product launches;
- promotional periods;
- seasonal events.

Temporal skew is particularly important for event and session workloads.

## 5.7 Event-type skew

Different event types naturally occur at different frequencies.

For example:

```text
PRODUCT_VIEW      → very common
SEARCH            → common
ADD_TO_CART       → less common
CHECKOUT_STARTED  → uncommon
PAYMENT_ATTEMPT   → uncommon
PURCHASE          → rare relative to views
```

The event distribution should therefore represent a funnel rather than uniform random categories.

---

# 6. Skew Must Propagate Through Relationships

A major design requirement is that skew should not exist in isolation.

Suppose a product becomes hot.

Then increased product demand should naturally influence:

```text
Product
  ↓
OrderItem
  ↓
Order
  ↓
Payment
  ↓
Shipment
```

The same activity may also appear in:

```text
Session
  ↓
Event
  ↓
Product interaction
```

Therefore a hot product should potentially create correlated pressure across several datasets.

Similarly, a hot customer can influence:

```text
Customer
  ↓
Sessions
  ↓
Events
  ↓
Orders
  ↓
OrderItems
  ↓
Payments
  ↓
Returns
```

This is substantially more realistic than independently applying a Zipf distribution to every table.

---

# 7. Hot-Key Modeling

A hot key is a key that receives disproportionately many records.

The generator should support an explicit hot-key model.

Conceptually:

```scala
case class HotKeyScenario(
  enabled: Boolean,
  keyType: String,
  hotKeyCount: Int,
  hotShare: Double
)
```

This is a conceptual design, not a requirement that the exact model above be implemented.

The important business semantics are:

- what dimension is hot;
- how many hot values exist;
- what proportion of records they receive;
- whether the hotness is global or time-bound;
- which related entities should inherit the effect.

---

# 8. Long-Tail Distributions

Long-tail behavior is especially valuable for product and customer activity.

Possible distributions include:

- Zipf;
- Pareto;
- log-normal;
- negative binomial;
- mixtures of distributions.

No distribution should be selected merely because it is mathematically fashionable.

The distribution must match the business quantity being modeled.

For example:

| Quantity | Candidate model |
|---|---|
| Product popularity | Zipf / Pareto / mixture |
| Customer order frequency | negative binomial / mixture |
| Customer spending | log-normal / gamma / mixture |
| Session counts | Poisson / negative binomial |
| Event counts | conditional count distribution |
| Return propensity | Bernoulli with heterogeneous probability |
| Price | bounded log-normal / category-specific model |

The actual selection belongs to the relevant domain behavior design.

---

# 9. Segmented Skew

Pure Zipf distributions are not always sufficient.

A useful alternative is a segmented population.

Example:

```text
Customers
├── VIP        2%
├── Loyal     13%
├── Regular   35%
├── Occasional30%
└── Dormant   20%
```

Each segment can have different:

- order frequency;
- spending;
- session frequency;
- product affinity;
- return propensity;
- discount sensitivity.

This approach has an important advantage:

> It gives business meaning to the skew.

The distribution is no longer just "customer 17 has a high count." It becomes "customer 17 belongs to a high-activity behavioral segment."

---

# 10. Customer Activity Skew

Customer activity should ideally be driven by a latent customer behavior profile.

Conceptually:

```text
CustomerBehaviorProfile
    |
    +-- activity propensity
    +-- purchase frequency
    +-- spending propensity
    +-- price sensitivity
    +-- category affinity
    +-- brand affinity
    +-- return propensity
    +-- device affinity
    +-- payment affinity
    +-- lifecycle state
```

These latent variables should influence downstream generation.

For example:

```text
high activity
    ↓
more sessions
    ↓
more events
    ↓
more purchase opportunities
    ↓
more orders
```

This produces correlated skew.

---

# 11. Product Popularity Skew

Product popularity should ideally be generated independently from product identity but then conditioned by business context.

A useful conceptual model is:

```text
product popularity
    × category demand
    × brand demand
    × customer affinity
    × seasonality
    × campaign effect
    × availability
```

This means the final number of purchases is not determined solely by one static product weight.

This supports more realistic changes over time.

---

# 12. Campaign-Driven Skew

A campaign can temporarily increase activity.

Example:

```text
Normal period:
  Electronics demand = 1.0x

Campaign period:
  Electronics demand = 2.5x
```

The campaign may influence:

- sessions;
- product views;
- add-to-cart events;
- orders;
- product demand;
- payment attempts.

The generator should avoid directly multiplying every downstream table independently.

Instead:

```text
campaign
    ↓
traffic / intent
    ↓
funnel behavior
    ↓
orders
    ↓
transactions
```

This creates coherent scenario propagation.

---

# 13. Geographic Skew

Geographic concentration should respect the geography hierarchy.

Example:

```text
Country
  ↓
State
  ↓
City
  ↓
Area
```

If a city becomes hot, related customer and transaction activity should remain within that city.

A geographic scenario may therefore affect:

- customer acquisition;
- customer addresses;
- sessions;
- orders;
- shipments.

The system should not produce a customer associated with one geography while arbitrarily generating behavior as though they belonged to another geography unless the business model explicitly permits it.

---

# 14. Scenario Architecture

A scenario represents an intentional world configuration.

Examples:

```text
baseline
high_activity
hot_customer
hot_product
campaign_spike
holiday_peak
geographic_concentration
return_spike
payment_failure
dirty_data
heavy_skew
```

A scenario should define business conditions rather than directly manipulate individual records.

Bad approach:

```text
if scenario == "hot-product":
    productId = "PRODUCT_000001"
```

Better approach:

```text
scenario
    ↓
behavior modifiers
    ↓
distribution parameters
    ↓
domain generation
```

This keeps scenario logic separate from entity construction.

---

# 15. Scenario Categories

## 15.1 Baseline

Purpose:

- normal generation;
- realistic but moderate heterogeneity;
- no extreme artificial skew;
- clean data by default.

This should be the reference dataset.

## 15.2 High activity

Purpose:

- increase customer and session activity;
- produce more events and orders;
- test scale and higher transaction density.

## 15.3 Hot customer

Purpose:

- concentrate activity among a small customer population;
- stress customer-key joins and aggregations.

## 15.4 Hot product

Purpose:

- concentrate order-item and event activity on selected products;
- stress product-key joins and aggregations.

## 15.5 Campaign spike

Purpose:

- create a temporary increase in traffic and conversion;
- test temporal concentration.

## 15.6 Holiday peak

Purpose:

- represent seasonal traffic and purchasing behavior;
- increase selected categories/products;
- change customer activity and order frequency.

## 15.7 Geographic concentration

Purpose:

- concentrate activity in selected geographic regions;
- stress geographically grouped workloads.

## 15.8 Return spike

Purpose:

- increase return probability for selected customers, categories, products, or periods.

## 15.9 Payment incident

Purpose:

- increase payment failures;
- increase retries;
- potentially alter payment-method distribution.

## 15.10 Dirty data

Purpose:

- introduce controlled data-quality defects;
- preserve the ability to distinguish expected scenario defects from generator bugs.

---

# 16. Scenario Composition

Multiple scenarios may sometimes be useful together.

For example:

```text
holiday_peak
+
hot_product
+
dirty_data
```

However, unrestricted composition creates ambiguity.

The system should therefore define:

- scenario precedence;
- compatible scenarios;
- conflicting scenarios;
- parameter override rules;
- effective configuration.

Example:

```text
baseline
    ↓
holiday_peak
    ↓
hot_product
    ↓
dirty_data
```

The final generated dataset should have a deterministic effective scenario configuration.

---

# 17. Scenario Ownership

Scenario logic should not become a second generator.

A scenario should provide modifiers or policies.

Conceptually:

```text
Scenario
   |
   +-- customer activity modifier
   +-- product popularity modifier
   +-- temporal modifier
   +-- quality modifier
   +-- skew modifier
```

Domain generators consume the resulting behavior.

This follows composition over a large conditional block.

---

# 18. Data-Quality Scenarios

Data quality should be intentionally controlled.

Useful defect categories include:

- missing values;
- invalid foreign keys;
- duplicate keys;
- malformed values;
- inconsistent relationships;
- invalid states;
- impossible timestamps;
- incorrect quantities;
- invalid monetary values.

The generator should distinguish:

```text
expected scenario violation
```

from:

```text
unexpected generator defect
```

This distinction is essential for testing validation systems.

---

# 19. Clean Dataset as the Reference

The clean baseline should be generated first.

Conceptually:

```text
Reference world
      ↓
valid domain records
      ↓
relationship validation
      ↓
optional scenario transformation
      ↓
quality/skew scenario
      ↓
final dataset
```

This creates a strong mental model.

The system should not generate arbitrary invalid data from the beginning unless the scenario explicitly requires it.

---

# 20. Controlled Data Corruption

Data corruption should be deterministic under the same seed.

For example:

```text
seed = 42
dirty rate = 1%
```

should produce the same corrupted records on every run.

A corruption strategy should ideally identify:

- target entity;
- target attribute;
- defect type;
- defect rate;
- deterministic selection policy.

Example conceptual configuration:

```hocon
quality {
  profile = "slightly_dirty"

  rules {
    missing-postal-code {
      rate = 0.005
    }

    invalid-order-status {
      rate = 0.001
    }
  }
}
```

The exact configuration model should evolve with the configuration architecture.

---

# 21. Workload-Oriented Dataset Design

The generator exists partly to support Spark performance experiments.

Therefore dataset design should explicitly consider workload classes.

The same logical dataset can produce very different performance depending on its shape.

Important dimensions include:

- total row count;
- number of distinct keys;
- key-frequency distribution;
- partition-size distribution;
- table-size ratios;
- temporal concentration;
- null rate;
- duplicate rate;
- relationship density.

---

# 22. Spark Workload Family: Joins

Important join patterns include:

```text
orders JOIN customers
orders JOIN order_items
order_items JOIN products
products JOIN categories
products JOIN brands
sessions JOIN events
orders JOIN payments
orders JOIN shipments
orders JOIN returns
```

Skewed join keys can create partition imbalance.

Example:

```text
product_id = PRODUCT_000001
```

appears in a very large fraction of `order_items`.

A hash partitioned join can therefore produce one or more disproportionately large partitions.

This is an intentional workload property.

---

# 23. Spark Workload Family: Aggregations

Examples:

```text
sales by product
sales by category
sales by customer
orders by city
events by session
returns by product
revenue by brand
```

These workloads are highly sensitive to key distribution.

A skewed dataset can create uneven aggregation state and partition sizes.

---

# 24. Spark Workload Family: Windows

Examples:

```text
customer order sequence
customer lifetime spend
product ranking
top products by category
session event ordering
rolling revenue
```

Windows are sensitive to:

- partition cardinality;
- partition-size imbalance;
- ordering volume;
- temporal concentration.

Hot customers can therefore create very large customer partitions.

---

# 25. Spark Workload Family: Top-K

Examples:

```text
top 100 products
top customers by spend
top brands by revenue
top cities by order volume
```

Long-tail distributions make top-K queries more realistic.

They also provide useful workloads for:

- aggregation;
- sorting;
- ranking;
- approximate algorithms.

---

# 26. Spark Workload Family: Filtering

Examples:

```text
orders during campaign
events on mobile
returns from selected category
customers from selected city
products in selected brand
```

Selective filters should have configurable selectivity.

A good generator should be able to create both:

```text
high-selectivity
```

and:

```text
low-selectivity
```

queries.

---

# 27. Spark Workload Family: Data Quality

Examples:

```text
find duplicate order IDs
find orphan order items
find invalid payment references
find null postal codes
find impossible timestamps
find invalid order states
```

Dirty scenarios should therefore be designed alongside validation workloads.

---

# 28. Workload Profiles

The system may eventually define workload profiles such as:

```text
balanced
join_skew
aggregation_skew
window_skew
temporal_spike
data_quality
mixed_stress
```

These profiles describe the intended workload shape.

They should not necessarily alter business semantics directly.

Instead:

```text
workload profile
    ↓
scenario parameters
    ↓
effective generation configuration
```

---

# 29. Skew Intensity

Skew should be configurable.

A useful conceptual scale is:

```text
NONE
LOW
MEDIUM
HIGH
EXTREME
```

Interpretation:

### NONE

Approximately balanced distribution.

### LOW

Natural heterogeneity.

### MEDIUM

Strong long-tail behavior.

### HIGH

Explicit hot-key concentration.

### EXTREME

Synthetic stress-test distribution.

The exact numerical parameters should be defined centrally rather than duplicated across domain generators.

---

# 30. Measuring Skew

The generator should eventually report skew statistics.

Useful metrics include:

- distinct key count;
- top-1 share;
- top-5 share;
- top-10 share;
- p50 frequency;
- p95 frequency;
- p99 frequency;
- max frequency;
- max/median ratio;
- coefficient of variation;
- Gini coefficient;
- partition-size estimates.

Example:

```text
Product popularity
------------------
distinct products : 500
median frequency  : 11
p95 frequency     : 420
p99 frequency     : 2,800
max frequency     : 8,700
top-10 share      : 31%
Gini              : 0.72
```

These metrics are more informative than simply saying "skew enabled."

---

# 31. Skew Validation

A scenario should have measurable acceptance criteria.

Example:

```text
Scenario: hot_product

Target:
  top_product_share >= 0.20

Observed:
  0.217

Result:
  PASS
```

This allows scenario behavior to be tested.

The same principle applies to:

- customer activity;
- category demand;
- geographic concentration;
- campaign spikes;
- return spikes.

---

# 32. Scenario Validation

Scenario validation should verify both:

1. the intended effect occurred;
2. unrelated invariants still hold.

For example, `hot_product` should verify:

```text
product popularity target reached
```

while still verifying:

```text
order_item.product_id exists
order_item.order_id exists
order totals remain valid
```

A scenario must not become an excuse to weaken structural correctness unless it is explicitly a dirty-data scenario.

---

# 33. Quality Scenario Validation

For a dirty scenario:

```text
configured defect rate
        ↓
observed defect rate
        ↓
acceptable tolerance
```

Example:

```text
configured null rate = 1.0%
observed null rate   = 0.97%
tolerance             = ±0.15%
```

The exact tolerance depends on whether the defect model is deterministic or probabilistic.

---

# 34. Exact vs Probabilistic Scenario Effects

Some scenario properties can be exact.

Example:

```text
10% of customers are hot customers
```

Other effects may be probabilistic.

Example:

```text
hot customers have 3x higher purchase propensity
```

The architecture should preserve this distinction.

Exact constraints should be validated as exact.

Probabilistic targets should be validated statistically.

---

# 35. Reproducibility

Skew and scenarios must preserve reproducibility.

Given:

```text
same seed
same configuration
same reference data
same code version
```

the expected dataset should be reproducible.

Scenario-specific random streams should be isolated.

Conceptually:

```text
root seed
   |
   +-- customer behavior
   +-- product popularity
   +-- temporal scenario
   +-- quality corruption
   +-- hot-key selection
```

A change to one scenario should not unnecessarily perturb unrelated streams.

---

# 36. Scenario Metadata

The manifest should eventually record scenario information.

Useful metadata includes:

```text
scenario name
scenario version
skew level
quality profile
seed
configuration fingerprint
reference-data fingerprint
effective parameters
target statistics
observed statistics
validation result
```

This makes datasets reproducible and explainable.

---

# 37. Scenario Versioning

Scenarios are part of the generator contract.

Changing:

```text
hot_product
```

from one distribution to another can change benchmark results substantially.

Therefore scenarios should be versioned or otherwise identifiable.

A benchmark should be able to state:

```text
dataset:
  scenario = hot_product
  scenario_version = 2
  seed = 42
```

This is important for performance comparisons.

---

# 38. Avoiding Hidden Scenario Behavior

A major anti-pattern is hidden behavior.

Bad:

```scala
if (config.seed == 42) {
  // special distribution
}
```

Bad:

```scala
if (recordCount > 1000000) {
  // silently introduce skew
}
```

Bad:

```scala
if (profile == "large") {
  // secretly alter customer behavior
}
```

These behaviors make benchmarks difficult to reproduce and explain.

All intentional shape changes should be explicit in configuration or scenario definitions.

---

# 39. Strategy Pattern

Strategy is appropriate when the same business concept has meaningful interchangeable implementations.

Examples:

```scala
trait ProductPopularityModel
trait CustomerActivityModel
trait ReturnProbabilityModel
trait TemporalDemandModel
trait SkewModel
```

Potential implementations:

```text
UniformProductPopularity
ZipfProductPopularity
SegmentedProductPopularity
HotKeyProductPopularity
```

The generator should select strategies through configuration/factory logic.

Do not create a strategy interface merely to wrap one implementation.

---

# 40. Composition Over Conditional Logic

A scenario should be assembled from smaller behavior components.

Conceptually:

```scala
val demandModel =
  baseDemand
    .withSeasonality(seasonality)
    .withCampaign(campaign)
    .withSkew(skew)
```

The exact API may differ.

The architectural principle is:

> behavior should be composed from understandable parts rather than implemented as a large scenario switch.

---

# 41. Factory Pattern

A factory can translate configuration into a concrete strategy.

Conceptually:

```scala
val skewModel =
  SkewModelFactory.create(config.skew)
```

The factory should:

- validate supported types;
- construct the correct implementation;
- keep configuration parsing out of domain behavior;
- fail clearly for unsupported choices.

It should not become a giant global factory for every entity in the system.

---

# 42. Dependency Injection

Dependencies should be passed explicitly.

Example:

```scala
class ProductDemandGenerator(
    popularityModel: ProductPopularityModel,
    random: RandomGenerator
)
```

This makes behavior:

- visible;
- testable;
- replaceable;
- deterministic.

No dependency injection framework is required.

---

# 43. SOLID Application

## Single Responsibility

A skew model calculates skew behavior.

A scenario resolver resolves scenarios.

A quality strategy introduces quality defects.

A generator constructs domain records.

These responsibilities should not collapse into one class.

## Open/Closed

Adding:

```text
ParetoSkewModel
```

should not require rewriting unrelated product generation logic.

## Liskov Substitution

Alternative strategies must preserve the semantic contract of the behavior they replace.

## Interface Segregation

Avoid one giant:

```scala
trait ScenarioManager
```

with dozens of unrelated methods.

Prefer small, meaningful boundaries.

## Dependency Inversion

Domain generation should depend on behavior abstractions where genuine variation exists, not directly on concrete random/distribution implementations.

---

# 44. Readability Rules

Scenario code should communicate business intent.

Prefer:

```scala
val popularity =
  productPopularityModel.generate(context)
```

over:

```scala
val x =
  strategy.apply(a, b, c, d)
```

Prefer:

```scala
val activity =
  customerActivityModel.generate(customerProfile)
```

over:

```scala
val value = helper.calculate(profile)
```

Names should describe business concepts.

Avoid vague names such as:

```text
Manager
Helper
Processor
Util
Handler
Engine
```

unless the responsibility is genuinely broad and the name communicates it accurately.

---

# 45. Configuration Ownership

Skew configuration should belong to the behavior/scenario configuration architecture.

It should not be duplicated across:

```text
customer.conf
product.conf
order.conf
event.conf
```

when the same cross-cutting skew concept is being configured.

Domain-specific parameters may remain in domain configuration.

Cross-cutting scenario parameters should have a single ownership boundary.

---

# 46. Example Configuration Shape

Conceptually:

```hocon
scenario {
  name = "hot_product"

  skew {
    level = "high"

    product {
      enabled = true
      hot-key-count = 5
      top-share = 0.20
    }
  }
}
```

A richer scenario might be:

```hocon
scenario {
  name = "holiday_peak"

  temporal {
    enabled = true
    multiplier = 2.5
  }

  product-demand {
    categories = ["electronics", "fashion"]
  }

  skew {
    level = "medium"
  }
}
```

These are architectural examples, not a statement of the current configuration schema.

---

# 47. Scenario Precedence

The effective configuration must have deterministic precedence.

A conceptual order is:

```text
base defaults
    ↓
generation profile
    ↓
cardinality profile
    ↓
scenario
    ↓
explicit overrides
```

The exact precedence should remain consistent with the configuration architecture defined in the configuration document.

The critical requirement is that the final effective values are inspectable.

---

# 48. Conflicting Scenarios

Some scenarios may conflict.

Examples:

```text
balanced
+
extreme_skew
```

or:

```text
payment_incident
+
zero_failures
```

The system should not silently choose one.

Possible policies:

1. reject incompatible combinations;
2. define explicit precedence;
3. merge only compatible properties.

Rejecting ambiguous combinations is generally preferable to silently producing unexpected data.

---

# 49. Workload Matrix

A future benchmark suite should map scenarios to workload types.

| Scenario | Join | Aggregation | Window | Temporal | Quality |
|---|---:|---:|---:|---:|---:|
| baseline | ✓ | ✓ | ✓ | ✓ | ✓ |
| hot_customer | ✓✓ | ✓✓ | ✓✓ |  |  |
| hot_product | ✓✓ | ✓✓ | ✓ |  |  |
| holiday_peak | ✓ | ✓ | ✓ | ✓✓ |  |
| geographic_skew | ✓ | ✓✓ | ✓ | ✓ |  |
| dirty_data | ✓ | ✓ |  |  | ✓✓ |
| mixed_stress | ✓✓ | ✓✓ | ✓✓ | ✓✓ | ✓✓ |

The notation is conceptual and indicates relative stress, not a measured benchmark result.

---

# 50. Dataset Families

The generator should eventually support named dataset families.

Example:

```text
shopsphere-small-baseline
shopsphere-medium-baseline
shopsphere-large-hot-product
shopsphere-large-hot-customer
shopsphere-xlarge-mixed-stress
shopsphere-medium-dirty-data
```

A dataset name should communicate its intended shape.

This is useful when datasets are reused in Spark experiments.

---

# 51. Baseline vs Stress Datasets

Two categories should remain conceptually separate.

## Baseline datasets

Used for:

- correctness;
- realistic analytics;
- regression tests;
- ordinary performance measurements.

## Stress datasets

Used for:

- skew handling;
- partition imbalance;
- shuffle pressure;
- large windows;
- data-quality processing;
- extreme cardinality.

This avoids confusing benchmark results from intentionally pathological data with normal workload behavior.

---

# 52. Relationship Density

Workload design is not only about skew.

Relationship density also matters.

Examples:

```text
orders per customer
items per order
events per session
sessions per customer
returns per order
```

A dense relationship can increase join multiplicity.

For example:

```text
customer
  → many sessions
      → many events
```

can create a large intermediate dataset.

This should be configurable and statistically measurable.

---

# 53. Join Explosion Scenarios

The generator should eventually support datasets that intentionally expose join multiplication.

For example:

```text
Customer
  × Session
  × Event
```

or:

```text
Order
  × OrderItem
  × Payment
  × Shipment
```

The generated data should remain semantically valid.

The workload should expose the cost of the join shape rather than relying on malformed data.

---

# 54. Temporal Workload Design

Temporal scenarios should include:

- normal days;
- weekends;
- seasonal periods;
- campaign periods;
- bursts;
- quiet periods.

The event stream should preserve event ordering within sessions.

Orders should remain temporally compatible with their associated sessions and events where attribution is modeled.

This allows realistic:

```text
time-window aggregations
rolling metrics
sessionization
```

workloads.

---

# 55. Partition-Oriented Thinking

Spark sees partitions, not just business entities.

Therefore workload design should consider:

```text
records per key
records per partition
distinct keys per partition
largest partition / median partition
```

The generator itself does not need to create Spark partitions.

However, its key distributions should make partition behavior predictable enough to test.

---

# 56. File Layout Considerations

The generator initially writes separate CSV files per entity.

For workload testing, file layout can later become another dimension:

```text
single large file
many small files
balanced files
imbalanced files
```

This should remain an output/storage concern, not be mixed into entity business logic.

Future formats may include:

- Parquet;
- JSON;
- CSV;
- partitioned output.

---

# 57. Scenario Fingerprint

A future dataset manifest should be able to identify the complete scenario.

Conceptually:

```text
scenario fingerprint =
    hash(
      scenario configuration
      + effective generation configuration
      + seed
      + reference-data version
      + generator version
    )
```

This provides a compact identity for reproducibility.

---

# 58. Statistical Calibration

Skew parameters should eventually be calibrated from generated statistics.

Process:

```text
configure
    ↓
generate
    ↓
measure
    ↓
compare with target
    ↓
adjust distribution
    ↓
repeat
```

This is especially important for:

- Gini coefficient;
- top-key share;
- customer activity;
- product popularity;
- return rate;
- AOV;
- session frequency.

The first implementation does not need automatic calibration, but the architecture should not prevent it.

---

# 59. Testing Strategy

Testing should exist at multiple levels.

## Unit tests

Test:

- distribution strategies;
- skew strategies;
- scenario modifiers;
- quality rules;
- statistical calculations.

## Domain tests

Test:

- customer behavior propagation;
- product popularity;
- return behavior;
- temporal effects.

## Scenario tests

Test:

```text
scenario → expected shape
```

## Integration tests

Test:

```text
config
→ effective scenario
→ generation
→ validation
→ statistics
→ output
```

## Reproducibility tests

Run the same configuration twice and compare:

- records;
- statistics;
- manifest;
- fingerprints.

---

# 60. Property-Based Thinking

Some behavior is better described as properties.

Examples:

```text
hot-product scenario:
  top product share should exceed target
```

```text
dirty-data scenario:
  configured defect class should be observable
```

```text
baseline:
  no unexpected foreign-key violations
```

```text
same seed:
  same scenario produces same dataset
```

This style is valuable for distribution-heavy code.

---

# 61. Performance of the Generator

Scenario complexity must not make generation unnecessarily expensive.

Avoid:

- scanning every record to select hot keys;
- repeatedly sorting large collections;
- rebuilding distributions per record;
- creating excessive intermediate objects;
- global mutable state.

Prefer:

- precomputed distributions;
- indexed reference data;
- immutable configuration;
- deterministic random streams;
- reusable strategies;
- streaming output where appropriate.

---

# 62. Large-Scale Considerations

At large cardinalities, skew can amplify memory and processing costs inside the generator itself.

Examples:

```text
millions of events
millions of order items
large product catalog
large customer population
```

The generator should therefore avoid materializing unnecessary global structures.

Future generation should support streaming or bounded-memory approaches where required.

---

# 63. Parallel Generation

Parallel generation must preserve reproducibility.

A naive implementation:

```scala
sharedRandom.nextInt()
```

is unsuitable because execution order can change.

Instead, generation should derive deterministic streams from stable semantic identities.

Conceptually:

```text
root seed
+
customer id
+
domain
+
scenario
```

produces an isolated stream.

This allows parallelism without sacrificing deterministic behavior.

---

# 64. Failure Isolation

Scenario behavior should fail clearly.

Examples:

```text
unsupported skew model
invalid top-share
negative defect rate
incompatible scenario
missing required hot key
invalid scenario parameter
```

These should fail during configuration validation rather than producing a partially incorrect dataset.

---

# 65. Current Implementation vs Target Design

The current generator already has:

- deterministic random generation;
- distribution infrastructure;
- HOCON configuration;
- generation profiles;
- cardinality profiles;
- scenarios;
- entity generators;
- structural validation;
- business-rule validation;
- statistics;
- CSV output;
- manifest-related architecture.

However, the current generated data does **not yet** implement the full sophistication described here.

Current baseline has important deterministic relationships such as:

```text
3 sessions/customer
8 events/session
2–3 items/order
```

These are useful for the foundation but are too uniform for the target realism level.

The current product/order model also produces an order-value distribution that is too expensive for a broad e-commerce baseline.

These are realism gaps, not architectural failures.

---

# 66. Planned Realism Sequence

The recommended implementation order is:

```text
1. CustomerBehaviorProfile
        ↓
2. Customer activity distribution
        ↓
3. Product popularity distribution
        ↓
4. Variable basket size
        ↓
5. Customer/category/product affinity
        ↓
6. Spending and price sensitivity
        ↓
7. Session heterogeneity
        ↓
8. Event funnel and variable event counts
        ↓
9. Return propensity
        ↓
10. Temporal/campaign effects
        ↓
11. Controlled skew scenarios
        ↓
12. Controlled data-quality scenarios
        ↓
13. Statistical calibration
```

This sequence reduces the risk of implementing artificial skew before the underlying business behavior exists.

---

# 67. Architecture Boundary

The skew/scenario architecture should not own:

- customer identity;
- product identity;
- order construction;
- CSV writing;
- geography reference loading.

It owns:

- scenario definitions;
- scenario resolution;
- skew behavior;
- scenario modifiers;
- quality scenario coordination;
- workload-oriented shape definitions.

Domain generators remain responsible for constructing domain records.

---

# 68. Domain Interaction

The eventual flow should resemble:

```text
EffectiveConfiguration
        |
        v
ScenarioContext
        |
        +--------------------+
        |                    |
        v                    v
CustomerBehavior       ProductDemand
        |                    |
        +---------+----------+
                  |
                  v
           Session / Order
                  |
                  v
          OrderItem / Event
                  |
          +-------+-------+
          |               |
          v               v
       Payment         Shipment
          |
          v
        Return
```

The exact dependency graph may evolve, but scenario information should influence behavior rather than directly creating unrelated records.

---

# 69. Scenario Context

A scenario context may eventually contain:

```text
scenario identity
effective parameters
temporal state
skew configuration
quality configuration
campaign state
random stream roots
```

It should be immutable.

Generators should receive the relevant context rather than reaching into global configuration.

---

# 70. Avoiding a Global Scenario Object

Although a scenario context is useful, it should not become a god object.

Do not pass a giant context containing every configuration field to every function.

Prefer focused inputs.

For example:

```scala
productPopularityModel.generate(
  popularityContext
)
```

rather than:

```scala
generator.generate(
  entireApplicationContext
)
```

This improves readability and dependency clarity.

---

# 71. Domain-Specific Skew

Not every domain needs a generic skew abstraction.

For example:

- Product can own product popularity behavior.
- Customer can own customer activity behavior.
- Event can own event funnel frequency.
- Geography can own geographic demand weights.

A common `SkewModel` should exist only if there is a genuine shared abstraction.

Otherwise, domain-specific behavior is clearer.

---

# 72. Global Scenario Orchestration

The application layer should coordinate scenario composition.

Conceptually:

```scala
val effectiveScenario =
  scenarioResolver.resolve(config)

val customerBehavior =
  customerBehaviorFactory.create(effectiveScenario)

val productDemand =
  productDemandFactory.create(effectiveScenario)

val qualityPlan =
  qualityPlanFactory.create(effectiveScenario)
```

Then domain generators receive these components.

This keeps orchestration separate from domain construction.

---

# 73. Scenario and Data Quality Interaction

A dirty scenario should normally be applied after a valid baseline has been produced.

Example:

```text
valid baseline
    ↓
holiday peak
    ↓
hot product
    ↓
quality corruption
```

This allows validation to determine whether observed defects match the configured corruption plan.

It also makes debugging easier.

---

# 74. What Should Never Be Hidden

The following should always be explicit:

- seed;
- scenario;
- skew level;
- quality profile;
- cardinality profile;
- generation profile;
- reference-data version;
- configuration fingerprint.

If a benchmark cannot explain why its data has a particular shape, the dataset contract is incomplete.

---

# 75. Definition of Done

The skew/scenario/workload architecture is complete when:

### Architecture

- scenario ownership is explicit;
- domain behavior remains domain-owned;
- cross-cutting concerns have clear boundaries;
- no god-object scenario manager exists.

### Behavior

- business skew can be modeled;
- artificial stress skew can be enabled explicitly;
- scenario effects propagate coherently;
- quality scenarios are controlled.

### Configuration

- scenarios are configurable;
- incompatible combinations are rejected or explicitly resolved;
- effective configuration is inspectable.

### Reproducibility

- same seed/configuration reproduces the same scenario;
- scenario-specific random streams are deterministic.

### Validation

- scenario targets are measurable;
- structural invariants remain valid unless intentionally corrupted;
- quality defects are measurable.

### Statistics

- skew metrics are reported;
- scenario effects are reported;
- workload-relevant shape statistics are available.

### Performance

- large-scale generation remains practical;
- scenario logic does not require expensive per-record global computation.

---

# 76. Migration Plan

Migration should be incremental.

## Step 1 — Documentation

Document the intended scenario and skew architecture.

## Step 2 — Common random/distribution infrastructure

Ensure deterministic streams and reusable distributions support the required models.

## Step 3 — Customer behavior

Introduce the latent behavior profile and activity model.

## Step 4 — Product demand

Introduce long-tail product popularity.

## Step 5 — Order/order-item behavior

Use customer behavior and product demand to create variable transaction behavior.

## Step 6 — Session/event behavior

Introduce heterogeneous sessions and event funnel behavior.

## Step 7 — Return behavior

Introduce heterogeneous return propensity.

## Step 8 — Temporal scenarios

Add campaign, holiday, and seasonal effects.

## Step 9 — Explicit skew scenarios

Add hot-customer, hot-product, geographic, and workload-oriented skew.

## Step 10 — Data-quality scenarios

Add controlled corruption strategies.

## Step 11 — Scenario statistics

Add measurable target/observed comparisons.

## Step 12 — Benchmark dataset profiles

Create named datasets for Spark workload families.

---

# 77. Quality Gate Before Implementation

Before writing code for this architecture, answer:

1. What exact business behavior is being modeled?
2. Is the skew naturally produced by that behavior or artificially injected?
3. Which domain owns the behavior?
4. What strategy, if any, is genuinely interchangeable?
5. What configuration controls it?
6. What random stream does it consume?
7. What downstream entities should inherit the effect?
8. What invariants must remain true?
9. What statistics prove the scenario worked?
10. What Spark workload does the scenario enable?
11. How will the scenario be reproduced?
12. What is intentionally simplified in the first implementation?

If these answers are unclear, implementation should wait.

---

# 78. Explicit Architecture Decisions

### Decision 1

Skew is a statistical property and should not be treated as corruption by default.

### Decision 2

Business realism and workload stress are separate concepts.

### Decision 3

Natural behavioral skew should be implemented before artificial extreme skew.

### Decision 4

Scenario logic should modify behavior rather than directly construct domain records.

### Decision 5

Scenario composition must be deterministic and explicit.

### Decision 6

Dirty-data scenarios should operate on an otherwise valid baseline where practical.

### Decision 7

Scenario-specific random streams must be deterministic and isolated.

### Decision 8

Scenario targets must be measurable through statistics and validation.

### Decision 9

The generator should expose enough shape metadata to make Spark benchmark datasets explainable.

### Decision 10

The architecture should support workload-oriented dataset families without coupling domain generation to Spark.

---

# 79. Final Design Summary

The intended ShopSphere architecture is:

```text
Business Domain
      ↓
Behavior Models
      ↓
Distributions
      ↓
Scenario Modifiers
      ↓
Domain Generators
      ↓
Valid Baseline Dataset
      ↓
Optional Scenario Effects
      ↓
Quality / Skew Scenario
      ↓
Validation + Statistics
      ↓
Manifest
      ↓
Spark Workload
```

The critical design principle is that the generator should produce a **coherent world**, not a collection of independently randomized tables.

A realistic hot product should affect product demand, order items, orders, sessions/events where appropriate, and downstream transactions.

A realistic high-activity customer should affect sessions, events, purchases, spending, and potentially returns.

A campaign should affect traffic and purchasing through behavioral mechanisms.

A dirty-data scenario should introduce known defects while preserving the distinction between intentional defects and generator bugs.

A workload-stress scenario may intentionally exaggerate skew, but that exaggeration must be explicit and measurable.

The final dataset should therefore answer three questions:

```text
What business world does this dataset represent?
What statistical shape does it have?
What workload is it intended to exercise?
```

If those three questions can be answered from the configuration, manifest, statistics, and documentation, the scenario architecture is doing its job.
