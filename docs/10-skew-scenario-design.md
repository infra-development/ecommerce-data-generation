# 10 — Skew Scenario Design

## Purpose

This document defines how the ShopSphere generator will intentionally create **controlled data skew** for downstream Spark performance experiments.

The project specification identifies data skew, hot customers, hot products, high-cardinality keys, and related workload characteristics as important future scenarios. Skew is explicitly intended to support later Spark experiments, including the Skew Lab. fileciteturn1file3

The goal is not simply to make one key appear many times. The goal is to create **measurable, configurable, reproducible, business-plausible skew**.

---

## 1. Core Decision

Skew is a **scenario**, not the default behavior of the generator.

Default:

```text
skew = disabled
```

When enabled, the generator deliberately modifies selected distributions so that a small number of keys receive disproportionately large amounts of activity.

The project plan explicitly proposes controls such as:

```hocon
skew {
    enabled = false

    hot_customer_count = 10
    hot_customer_multiplier = 100

    hot_product_count = 5
    hot_product_multiplier = 50
}
```

These values are conceptual and remain configurable. fileciteturn1file3

---

## 2. Why Skew Matters

Spark performance can be strongly affected when data is unevenly distributed across partition keys.

Conceptually:

```text
Normal distribution

Key A  ███████
Key B  ██████
Key C  ███████
Key D  ██████
Key E  ███████
```

versus:

```text
Skewed distribution

Hot A  █████████████████████████████████
Key B  ███
Key C  ██
Key D  ███
Key E  ██
```

A small number of hot keys can cause:

- uneven partition sizes
- long-running tasks
- executor imbalance
- increased shuffle pressure
- memory pressure
- spill
- straggler tasks

The generator should make these conditions reproducible so that Spark experiments can compare baseline and skewed workloads.

---

## 3. Business Keys Suitable for Skew

Initial skew candidates:

```text
customer_id
product_id
```

These align directly with the project specification's planned hot-customer and hot-product scenarios.

Potential future candidates:

```text
category_id
brand_id
city_id
acquisition_channel
session/customer relationships
```

New skew dimensions should be introduced only when there is a clear Spark experiment or business rationale.

---

## 4. Hot-Key Model

A hot-key scenario consists of:

```text
hot-key population
+
activity concentration
+
baseline population
```

Example:

```text
Customers
    |
    +-- 10 hot customers
    +-- remaining customers
```

The hot customers receive disproportionately more:

```text
orders
sessions
events
```

Similarly:

```text
Products
    |
    +-- 5 hot products
    +-- remaining products
```

The hot products receive disproportionately more:

```text
order items
events
```

---

## 5. Skew Must Preserve Referential Integrity

Skew should change **frequency**, not validity.

For example:

```text
hot_customer
    -> referenced by many valid orders
```

is valid skew.

This is preferable to:

```text
many invalid customer IDs
```

which is a data-quality problem rather than a skew problem.

Therefore:

```text
Skew Engine
    modifies selection probabilities

Data Quality Engine
    modifies data correctness
```

The two mechanisms remain separate.

---

## 6. Hot Customers

A hot-customer scenario should select a configured number of customers as high-activity customers.

Example:

```text
hot_customer_count = 10
```

The selection itself should be deterministic.

Possible strategy:

```text
customer population
       |
       v
deterministically select hot customers
       |
       v
assign high activity profile
```

Hot customers may then receive increased:

```text
order frequency
session frequency
event frequency
```

The exact multipliers should be configurable.

---

## 7. Hot Products

A hot-product scenario follows the same model.

Example:

```text
hot_product_count = 5
```

Selected products receive disproportionately high selection probability.

Conceptually:

```text
normal product distribution
          |
          v
skew scenario
          |
          v
hot-product concentration
```

The product remains a valid product with its normal category, brand, price, and other attributes.

Only its activity frequency is intentionally concentrated.

---

## 8. Multipliers

A multiplier is a convenient scenario control.

Example:

```text
hot_customer_multiplier = 100
```

means a hot customer may receive substantially more activity than a baseline customer.

However, the multiplier should not be interpreted as an unconditional guarantee that every hot key receives exactly 100 times the rows.

The final observed frequency depends on:

- baseline activity distribution
- other behavioral distributions
- number of hot keys
- total volume
- random seed
- interaction with other scenarios

Therefore the multiplier is best treated as a **distribution-shaping parameter**.

---

## 9. Absolute vs Relative Skew

The generator should support both conceptual forms eventually.

### Relative skew

```text
hot key = N × baseline activity
```

Useful when comparing across scale profiles.

### Absolute concentration

```text
top 10 keys receive approximately X% of activity
```

Useful when designing a specific Spark experiment.

### Initial implementation

Start with relative controls such as:

```text
hot_customer_multiplier
hot_product_multiplier
```

Add target-share controls later if experiments require them.

---

## 10. Skew Intensity Profiles

Named skew profiles can simplify experiment setup.

Possible presets:

```text
none
light
moderate
heavy
extreme
```

Conceptually:

```text
light
    -> small concentration

moderate
    -> clearly visible hot keys

heavy
    -> severe partition imbalance

extreme
    -> intentionally pathological workload
```

The exact numerical parameters should remain configurable.

Profiles must resolve into explicit effective configuration.

---

## 11. Product Popularity and Hot Products

The project already requires a long-tail product popularity model.

This creates an important distinction:

```text
Long-tail popularity
    = realistic baseline behavior

Hot-product scenario
    = deliberately amplified concentration
```

Therefore skew should not replace the normal product popularity distribution.

Preferred model:

```text
Base product popularity
        +
Skew scenario
        |
        v
Controlled hot-product distribution
```

This allows experiments to distinguish naturally occurring heavy tails from deliberately amplified skew.

---

## 12. Customer Activity and Hot Customers

Similarly, customer activity already has heterogeneous behavior.

The project specification calls for internal behavioral attributes such as:

```text
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
activity_level
```

and describes high-activity customers with frequent purchases and higher average order values. fileciteturn1file0

Therefore hot customers should be implemented by modifying the activity model rather than bypassing it.

Preferred conceptual model:

```text
Customer Behavioral Profile
          |
          v
Normal activity
          |
          +---- skew scenario ----> hot activity
```

---

## 13. Skew Across Related Tables

A useful skew scenario should propagate naturally through relationships.

### Hot customer

```text
Customer
   |
   +--> many Orders
            |
            +--> many OrderItems
            +--> Payments
            +--> Shipments
            +--> Returns
```

### Hot product

```text
Product
   |
   +--> many OrderItems
   |
   +--> many Events
```

This produces realistic concentration across multiple fact-like tables.

---

## 14. Event Skew

Events can amplify customer/product skew.

For example:

```text
hot customer
    -> more sessions
    -> more events

hot product
    -> more product_view events
    -> more add_to_cart events
    -> more purchases
```

This is useful because later Spark experiments may aggregate or join events against dimensions.

The event generator should inherit the skew model rather than independently creating unrelated hot keys.

---

## 15. Join-Key Skew

The most valuable skew scenarios are those that affect keys used in joins.

Examples:

```text
orders.customer_id
order_items.product_id
events.customer_id
events.product_id
```

The scenario library should explicitly identify which join keys are affected.

This allows downstream Spark experiments to ask:

```text
How does this join behave with normal key distribution?
How does the same join behave with hot keys?
```

---

## 16. Aggregation-Key Skew

Skew should also support aggregation workloads.

Example:

```text
GROUP BY customer_id
```

or:

```text
GROUP BY product_id
```

A hot-key scenario should produce measurable concentration in the grouped population.

Useful statistics include:

```text
top 1 key share
top 10 key share
top 100 key share
max frequency
median frequency
p95 frequency
p99 frequency
max / median ratio
```

---

## 17. Measuring Skew

Skew must be measurable rather than judged visually.

For a key distribution:

```text
frequency(key)
```

the generator should eventually report statistics such as:

```text
distinct keys
total rows
minimum frequency
maximum frequency
mean frequency
median frequency
p95
p99
top-N concentration
max/median ratio
```

These metrics should be included in generation statistics where relevant.

---

## 18. Skew Ratio

A simple diagnostic metric is:

```text
skew_ratio = max_frequency / median_frequency
```

Example:

```text
median = 100
maximum = 10,000

skew_ratio = 100
```

This is not a complete statistical measure, but it is an intuitive diagnostic.

Other concentration metrics should be added where useful.

---

## 19. Top-N Concentration

For Spark experiments, top-N concentration can be more useful than max frequency.

Example:

```text
Top 10 customers
    -> 12% of all orders

Top 100 customers
    -> 25% of all orders
```

This makes the workload's concentration explicit.

The exact statistics to expose will be finalized in `11-validation-strategy.md` and `14-observability.md`.

---

## 20. Skew and Partition Simulation

The generator should **not** attempt to reproduce Spark's physical partitioning behavior inside the business data generator.

It should generate the logical key-frequency distribution.

Spark should determine:

```text
partitioning
shuffle
task assignment
memory
spill
execution behavior
```

This keeps the generator independent of Spark, consistent with the architectural decision that the initial generator is a normal Scala application rather than a Spark application. fileciteturn1file4

---

## 21. Skew and Output Files

File-level skew is a separate concern.

These are different scenarios:

```text
key skew
    -> uneven logical key frequency

file-size skew
    -> uneven physical file sizes
```

The initial skew scenario should focus on **logical key skew**.

File-count and file-size scenarios belong to the broader scenario library and output design.

---

## 22. Skew and Cardinality

Skew and cardinality are independent dimensions.

For example:

```text
100M orders
100K customers
high concentration
```

is different from:

```text
100M orders
50M customers
high concentration
```

The generator should allow:

```text
volume
+
cardinality
+
skew
```

to be controlled independently where possible.

This follows the project's requirement that cardinality be independently controllable. fileciteturn1file1

---

## 23. Skew and Volume

Skew intensity must be interpreted relative to total volume.

A scenario that is highly visible at:

```text
100M orders
```

may be statistically unstable at:

```text
10K orders
```

Therefore validation should distinguish:

```text
small dataset
```

from:

```text
large dataset
```

and avoid overly strict statistical expectations for tiny populations.

---

## 24. Skew and Reproducibility

Hot-key selection and activity assignment must be deterministic.

For:

```text
same seed
+
same effective configuration
+
same generator version
```

the same logical hot-key population should be selected.

Changing the seed may change which keys become hot if the scenario defines hot-key selection probabilistically.

Alternatively, a scenario may explicitly select deterministic top-ranked keys.

The initial implementation should document whichever strategy is chosen rather than leaving the behavior implicit.

---

## 25. Recommended Initial Hot-Key Selection

For the initial implementation, prefer a deterministic selection process.

Conceptually:

```text
eligible keys
    |
    v
controlled seeded selection
    |
    v
hot-key set
```

The selected hot keys should be recorded in generation statistics or manifest metadata when the population is small enough to make that practical.

For very large hot-key populations, summary statistics are preferable to storing the complete list in the manifest.

---

## 26. Multiple Skew Dimensions

The generator may eventually support:

```text
hot customers
+
hot products
```

simultaneously.

This can create compounded effects.

Example:

```text
Hot customer
    buys
Hot product
```

This is a potentially extreme condition.

Therefore combinations of skew scenarios should be explicitly supported and measured rather than assumed to behave like the individual scenarios.

---

## 27. Skew Interaction With Behavioral Correlation

Skew should respect business behavior.

For example, a hot customer may have:

```text
higher activity
```

but should not automatically have:

```text
invalid data
```

unless a data-quality scenario also requests it.

Likewise, a hot product should retain its:

```text
category
brand
price
```

and only become more frequently selected.

---

## 28. Skew Scenario Examples

The future scenario library can include:

```text
baseline
hot-customers
hot-products
hot-customers-and-products
heavy-customer-skew
heavy-product-skew
extreme-join-skew
```

These should be configuration-driven.

The project plan explicitly expects named scenarios such as `hot-customers` and `hot-products`. fileciteturn1file3

---

## 29. Experimental Dataset Pairing

Every skew dataset should ideally have a comparable baseline dataset.

Example:

```text
Dataset A
    profile = large
    scenario = baseline
    seed = 12345

Dataset B
    profile = large
    scenario = hot-customers
    seed = 12345
```

The experiment can then compare:

```text
same volume
same schema
same general business model
same seed
different skew scenario
```

The seed does not guarantee identical non-skewed rows if the scenario changes generation decisions, but using the same seed helps keep the experiment controlled.

---

## 30. Validation of Skew Scenarios

Validation should confirm:

### Configuration

```text
skew enabled?
hot-key count valid?
multipliers valid?
```

### Structural correctness

```text
all hot keys exist
all references remain valid
```

### Distribution

```text
hot keys actually have higher activity
```

### Magnitude

```text
observed concentration is within expected tolerance
```

### Isolation

```text
non-skewed dimensions remain within expected behavior
```

The detailed validation framework belongs in `11-validation-strategy.md`.

---

## 31. Initial Implementation Scope

The first skew implementation should support:

1. enable/disable skew
2. hot customers
3. hot products
4. configurable hot-key counts
5. configurable activity multipliers
6. deterministic hot-key selection
7. propagation through related entities
8. skew statistics
9. baseline-vs-skew scenario comparison

This is sufficient to support the first useful Spark skew experiments.

---

## 32. Deferred Decisions

The following remain intentionally open:

- exact mathematical skew transformation
- exact interpretation of multipliers
- target-share configuration
- adaptive skew models
- multiple simultaneous hot-key populations
- correlated customer/product hotness
- partition-aware workload generation
- synthetic file-size skew
- extreme pathological distributions
- exact skew metrics
- automated calibration against Spark partition statistics

These should be decided when implementation and Spark experiments require them.

---

## 33. Guiding Principle

The purpose of skew generation is not to create random chaos.

It is to create a **known, measurable concentration of valid business activity**.

The desired model is:

```text
Realistic baseline
       +
Controlled hot-key scenario
       +
Deterministic generation
       +
Measured concentration
       =
Reproducible Spark skew workload
```

The core rule is:

> **Skew should change frequency, not correctness: make a small number of valid business keys disproportionately important, and make that concentration measurable and reproducible.**
