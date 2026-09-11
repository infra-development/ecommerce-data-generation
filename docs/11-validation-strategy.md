# 11 — Validation Strategy

## Purpose

This document defines how the ShopSphere generator will prove that generated datasets are **correct, internally consistent, statistically plausible, reproducible, and aligned with the requested configuration**.

Validation is a first-class part of generation.

The project specification requires:

- referential integrity validation
- business consistency validation
- statistical validation
- reporting of row counts, unique counts, null percentages, duplicate percentages, and distribution summaries
- clear failure when configured invariants are violated. fileciteturn1file2

The generator should therefore not treat successful file writing as proof of a successful generation.

---

## 1. Core Decision

The generation pipeline is considered successful only when:

```text
Generation
    +
Validation
    +
Manifest / Statistics
    =
Successful Dataset
```

Conceptually:

```text
Configuration
      |
      v
Generation
      |
      v
Validation
      |
      +---- failure ---> Generation Failed
      |
      v
Statistics
      |
      v
Manifest
      |
      v
Output Accepted
```

### Decision

> **Validation is part of the generation contract, not an optional post-processing utility.**

---

## 2. Validation Layers

Validation should operate at multiple levels.

```text
Layer 1 — Configuration validation
Layer 2 — Record validation
Layer 3 — Referential validation
Layer 4 — Business consistency validation
Layer 5 — Statistical validation
Layer 6 — Reproducibility validation
Layer 7 — Output/manifest validation
```

Each layer answers a different question.

---

## 3. Layer 1 — Configuration Validation

Configuration should be validated before expensive generation begins.

Examples:

```text
negative record count
invalid probability
unknown distribution
invalid date range
invalid profile
invalid scenario
invalid skew multiplier
invalid quality rate
```

A configuration error should fail fast.

The generator should report:

```text
configuration path
invalid value
expected constraint
```

rather than failing later with an unrelated runtime exception.

---

## 4. Layer 2 — Record Validation

Record-level validation checks whether individual records satisfy their basic schema and domain constraints.

Examples:

```text
Customer.customer_id is non-empty
Product.price >= 0
OrderItem.quantity > 0
Payment.amount >= 0
Session.end >= Session.start
```

This layer should be lightweight enough to support validation during generation where useful.

---

## 5. Schema Validation

Every entity must conform to its expected schema.

Validation should cover:

- required fields
- field types
- field names
- allowed enumerations
- nullable/non-nullable semantics
- basic formatting constraints

Example:

```text
Order.status
    ∈ configured valid statuses
```

An invalid enum value should be reported explicitly.

---

## 6. Layer 3 — Referential Integrity

Relationships are a core requirement of the data model.

The clean dataset should maintain valid references.

Examples:

```text
Order.customer_id
    -> Customer.customer_id

OrderItem.order_id
    -> Order.order_id

OrderItem.product_id
    -> Product.product_id

Payment.order_id
    -> Order.order_id
```

The project plan explicitly requires these relationship checks. fileciteturn1file2

---

## 7. Referential Integrity Metrics

The validator should report useful counts.

Example:

```text
orders:
    total = 10,000,000
    valid_customer_refs = 9,999,000
    null_customer_refs = 500
    invalid_customer_refs = 500
```

For a clean dataset:

```text
invalid references = 0
```

For a dirty scenario, configured violations may be expected.

---

## 8. Relationship Completeness

Not every relationship is necessarily mandatory in every business state.

For example:

```text
Order
    -> Payment
```

may be absent when a scenario deliberately creates missing payments.

Therefore validation should distinguish:

```text
required relationship
optional relationship
expected missing relationship
unexpected missing relationship
```

The validation policy should derive its expectations from the effective configuration and scenario.

---

## 9. Layer 4 — Business Consistency

Business consistency validates relationships between values.

Examples from the project specification include:

```text
quantity > 0
price >= 0
cost >= 0
order_total ~= sum(order items)
payment amount ~= order amount
session_end >= session_start
event_timestamp within session bounds
```

fileciteturn1file2

These checks are more meaningful than schema validation alone.

---

## 10. Derived-Value Validation

Derived fields should be validated against their source values.

Example:

```text
OrderItem:
    quantity × unit_price
        -> line_amount

Order:
    sum(line_amount)
        -> subtotal
```

Then:

```text
subtotal
    -> discounts/taxes
    -> total
```

The validator should detect mismatches between the calculated and stored values.

This prevents independently generated fields from producing logically inconsistent records.

---

## 11. Temporal Validation

Temporal relationships should be explicitly validated.

Examples:

```text
session_start <= session_end

event_timestamp >= session_start
event_timestamp <= session_end

shipment_timestamp >= order_timestamp

return_timestamp >= order_timestamp
```

The exact rules depend on entity status and business state.

Temporal validation should be scenario-aware where intentionally invalid timestamps are configured.

---

## 12. Geography Validation

Geographic hierarchy should remain coherent.

Conceptually:

```text
Country
   |
   v
State
   |
   v
City
```

A customer should not contain an arbitrary combination such as:

```text
country = A
state = B from country C
city = D from unrelated state
```

Reference-data relationships should therefore be validated.

---

## 13. Product Hierarchy Validation

Products should reference valid:

```text
category
brand
```

If category hierarchy exists:

```text
category
    -> parent category
```

must also be valid.

Product attributes conditioned on category should remain within the expected domain.

---

## 14. Customer Behavior Validation

Internal behavioral attributes may not appear in output tables, but their effects should be measurable.

Examples:

```text
VIP
    -> expected higher activity

high activity
    -> expected more sessions/orders

preferred category
    -> expected higher probability of related product interaction
```

Validation should test these as statistical relationships rather than deterministic equality.

---

## 15. Distribution Validation

The generator must compare configured distributions with observed generated distributions.

Examples:

```text
configured:
VIP = 5%

observed:
VIP = 5.02%
```

or:

```text
configured:
payment.method.UPI = 45%

observed:
payment.method.UPI = 44.97%
```

Exact equality is not expected for probabilistic generation.

The validator should use tolerances appropriate to population size.

---

## 16. Small vs Large Population Tolerance

A fixed absolute tolerance is not appropriate for every dataset.

For example:

```text
100 records
1% expected
```

may reasonably produce:

```text
0 or 1 records
```

while:

```text
100,000,000 records
1% expected
```

should be much closer to the target proportion.

Therefore statistical validation should account for sample size.

The exact statistical method is intentionally deferred.

---

## 17. Categorical Distribution Validation

For weighted categorical distributions, validate:

```text
category
expected probability
observed count
observed probability
difference
```

Example:

| Category | Expected | Observed |
|---|---:|---:|
| STANDARD | 70% | 69.98% |
| PREMIUM | 20% | 20.04% |
| VIP | 5% | 5.01% |
| BUSINESS | 5% | 4.97% |

The validator should identify categories that materially deviate from expectations.

---

## 18. Numeric Distribution Validation

For numeric distributions, useful statistics include:

```text
count
min
max
mean
median
standard deviation
p50
p90
p95
p99
```

For skewed distributions:

```text
top-N concentration
max/median
tail percentiles
```

may be more informative than mean alone.

---

## 19. Long-Tail Validation

Product popularity should be validated as a long-tail distribution.

Useful checks include:

```text
top product frequency
median product frequency
p95/p99 frequency
top 1% share
top 10% share
```

The validator should detect accidental uniformity.

A dataset in which every product receives nearly identical activity should fail or warn when a long-tail model is explicitly configured.

---

## 20. Skew Validation

For an enabled skew scenario, validation should verify:

```text
hot-key count
hot-key activity
concentration
```

Example:

```text
configured:
10 hot customers
multiplier = 100

observed:
10 selected hot customers
hot-key activity materially above baseline
```

The validator should not require an exact multiplier unless the mathematical model explicitly guarantees one.

---

## 21. Cardinality Validation

Cardinality is an explicit product requirement.

Validation should report:

```text
entity row count
distinct primary keys
distinct foreign keys
configured cardinality
observed cardinality
```

Example:

```text
orders:
    rows = 100,000,000
    unique customer IDs = 100,000
```

This distinction is important for downstream Spark aggregation and join experiments. fileciteturn1file1

---

## 22. Volume Validation

Configured entity volumes should be checked exactly where the generation model specifies exact counts.

Example:

```text
configured:
customers = 1,000,000

observed:
customers = 1,000,000
```

For derived entities such as OrderItems, the validator should instead use the applicable distribution or relationship expectation when the count is intentionally stochastic.

---

## 23. Data Quality Validation

The Data Quality Engine and Validator must work together.

For:

```text
clean
```

expected defects should be zero.

For:

```text
slightly_dirty
dirty
```

the validator should check that configured defect rates are approximately realized.

Examples:

```text
null rate
duplicate rate
invalid price rate
invalid timestamp rate
missing relationship rate
```

The quality strategy is defined separately in `09-data-quality-strategy.md`.

---

## 24. Expected Violations

A dirty dataset may intentionally violate clean-data invariants.

Therefore validation must distinguish:

```text
expected violation
```

from:

```text
unexpected violation
```

Example:

```text
configured:
invalid_price_rate = 0.001

observed:
invalid prices = 0.098%
```

This can be acceptable.

But:

```text
invalid customer references = 12%
```

when no such defect was configured should be treated as an unexpected generator problem.

---

## 25. Validation Severity

Validation results should eventually support severity levels.

Suggested levels:

```text
INFO
WARN
ERROR
```

### INFO

Expected informational statistic.

### WARN

Deviation worth reporting but not necessarily generation-fatal.

### ERROR

A contract or invariant has been violated.

The exact severity policy should be defined during implementation.

---

## 26. Hard vs Soft Validation

Not every statistical deviation should fail generation.

### Hard validation

Use for deterministic invariants:

```text
negative configured count
duplicate primary key in clean mode
invalid foreign key in clean mode
order total calculation mismatch
invalid schema
```

### Soft/statistical validation

Use for probabilistic characteristics:

```text
distribution proportion
mean
percentiles
skew concentration
```

Statistical checks should normally use tolerances.

---

## 27. Validation Modes

The generator may eventually support modes such as:

```text
strict
standard
off
```

### Strict

Fail on configured validation errors and tighter statistical tolerances.

### Standard

Normal production mode.

### Off

Skip expensive validation when explicitly requested.

### Initial decision

Validation should be enabled by default.

An explicit mechanism may eventually allow expensive checks to be disabled for very large generations, but this should never silently disable essential correctness checks.

---

## 28. Validation Timing

Validation can occur at several points.

### During generation

Useful for:

```text
local record invariants
configuration constraints
```

### After entity generation

Useful for:

```text
referential integrity
distribution statistics
```

### Before output acceptance

Useful for:

```text
final dataset contract
manifest consistency
```

The implementation should avoid repeatedly scanning huge datasets unnecessarily.

---

## 29. Streaming-Friendly Validation

Because the generator may eventually create very large datasets, validation should support streaming or incremental statistics where practical.

Avoid requiring the entire dataset to be loaded into memory.

Examples:

```text
row count
null count
sum
mean
min/max
frequency counters
hash-based distinct estimates
```

The exact algorithms will be selected based on scale requirements.

---

## 30. Validation and Memory

Validation must not undermine the generator's ability to produce large datasets.

Avoid designs such as:

```text
generate 1B rows
load all rows into memory
validate
```

Instead prefer:

```text
generate
    |
    +--> write
    |
    +--> accumulate validation statistics
```

or:

```text
generate chunks
    |
    +--> validate chunk
    +--> update aggregate statistics
```

The exact architecture will evolve with chunking.

---

## 31. Validation and Reproducibility

Validation results should also be deterministic for deterministic generation inputs.

For:

```text
same config
+
same seed
+
same generator version
```

the expected validation statistics should be the same.

This makes validation output useful for regression testing.

---

## 32. Validation and Manifest

The manifest should summarize validation status.

Conceptually:

```json
{
  "validation": {
    "status": "PASSED",
    "referentialIntegrity": "PASSED",
    "businessConsistency": "PASSED",
    "statisticalValidation": "PASSED"
  }
}
```

The project specification requires the generation manifest to serve as an audit record for generated datasets. fileciteturn1file2

---

## 33. Validation Report

A human-readable summary should eventually look conceptually like:

```text
Validation
----------

Schema:
  PASS

Referential integrity:
  PASS
  invalid references: 0

Business consistency:
  PASS
  order-total mismatches: 0

Statistics:
  PASS
  customer segments within tolerance
  payment methods within tolerance

Data quality:
  PASS
  configured defects within tolerance

Overall:
  SUCCESS
```

For failures, the report should identify the exact failing area.

---

## 34. Failure Diagnostics

A validation failure should answer:

```text
What failed?
Where did it fail?
How many records were affected?
What was expected?
What was observed?
Which configuration caused the expectation?
```

Example:

```text
VALIDATION ERROR

Entity: order_items
Field: quantity
Rule: quantity > 0
Expected invalid count: 0
Observed invalid count: 127
```

This is substantially more useful than:

```text
Validation failed.
```

---

## 35. Validation Statistics

The project specification explicitly calls for reporting:

```text
row counts
unique counts
null percentages
duplicate percentages
distribution summaries
```

These should become standard generation statistics rather than ad hoc debug output. fileciteturn1file2

---

## 36. Cross-Entity Validation

Some rules require multiple entities.

Examples:

```text
Order.customer_id
    exists in Customer

OrderItem.order_id
    exists in Order

Payment.order_id
    exists in Order

Event.session_id
    exists in Session
```

These checks should be performed through the relationship model or appropriate indexed/reference structures.

The validator should not duplicate business relationship logic unnecessarily.

---

## 37. Validation Ownership

A clean separation should exist:

```text
Entity Generator
    -> creates records

Relationship Manager
    -> establishes relationships

Data Quality Engine
    -> intentionally creates defects

Validator
    -> verifies resulting behavior
```

The validator should primarily observe and verify rather than silently repair data.

---

## 38. Validator Must Not Repair

Validation and remediation are separate responsibilities.

The validator should not silently:

```text
fix nulls
replace invalid values
repair foreign keys
deduplicate records
```

If repair is ever introduced, it should be an explicit transformation stage.

Otherwise a generation defect could disappear before being reported.

---

## 39. Regression Validation

A small deterministic generation should be used as a regression test.

Example:

```text
profile = small
scenario = clean
seed = 12345
```

Expected checks may include:

```text
exact row counts
exact schema
exact relationships
known representative values
known statistics
manifest fields
```

For probabilistic behavior, assertions should focus on stable characteristics unless exact output is deliberately part of the contract.

---

## 40. Validation Test Categories

The implementation should include:

### Unit tests

```text
individual validation rules
```

### Integration tests

```text
complete entity relationships
```

### Statistical tests

```text
distribution behavior
```

### Scenario tests

```text
skew
data quality
high/low cardinality
```

### Reproducibility tests

```text
same seed -> same result
different seed -> different realization
```

### End-to-end tests

```text
configuration
    -> generation
    -> validation
    -> manifest
```

---

## 41. Initial Validation Scope

The first implementation should validate:

1. configuration correctness
2. schema correctness
3. exact configured entity counts
4. primary-key uniqueness in clean mode
5. foreign-key integrity
6. basic business invariants
7. derived monetary values
8. temporal consistency
9. configured data-quality rates
10. basic distribution statistics
11. cardinality
12. generation manifest consistency

This is enough to establish a strong correctness foundation.

---

## 42. Deferred Decisions

The following remain intentionally open:

- exact statistical goodness-of-fit tests
- confidence interval method
- exact tolerance calculation
- approximate distinct-count algorithms
- streaming histogram implementation
- validation parallelism
- strict/standard/off mode semantics
- validation report file format
- validation API design
- cross-file validation architecture
- maximum validation cost allowed for XLarge datasets

These should be resolved when implementation and scale requirements make them concrete.

---

## 43. Guiding Principle

The generator should never answer:

```text
"Did the files get created?"
```

and call that success.

It should answer:

```text
Are the records valid?
Are relationships valid?
Are business rules valid?
Are requested distributions present?
Are configured cardinalities present?
Are intentional defects within tolerance?
Is the result reproducible?
Does the manifest accurately describe it?
```

The core rule is:

> **Generation creates the dataset; validation proves that the dataset matches the requested contract.**
