# 09 — Data Quality Strategy

## Purpose

This document defines how the ShopSphere generator will create **clean, slightly dirty, and intentionally dirty datasets** while keeping data-quality behavior controlled, reproducible, measurable, and useful for downstream Spark experiments.

The project specification explicitly requires configurable support for null values, duplicates, invalid values, missing relationships, invalid timestamps, invalid prices, invalid quantities, and malformed strings, with **clean data as the default**. fileciteturn1file0

This document separates the normal data-generation model from deliberate data-quality degradation.

---

## 1. Core Decision

Data quality problems will be implemented as a **separate, explicit generation stage**.

Preferred pipeline:

```text
Reference Data
      |
      v
Clean Domain Generation
      |
      v
Relationship Construction
      |
      v
Data Quality Rules
      |
      v
Validation
      |
      v
Output
```

The generator should first produce a logically valid baseline dataset and then apply configured quality defects.

### Decision

> **Generate realistic clean data first; introduce defects through a controlled Data Quality Engine.**

This prevents corruption logic from contaminating the normal domain model.

---

## 2. Clean Data Is the Default

The default configuration must produce clean data.

Conceptually:

```hocon
data_quality {
    profile = "clean"
}
```

or equivalent explicit rates:

```hocon
data_quality {
    null_customer_id_rate = 0.0
    duplicate_order_rate = 0.0
    invalid_price_rate = 0.0
    invalid_timestamp_rate = 0.0
    missing_payment_rate = 0.0
}
```

The exact configuration syntax remains governed by `03-configuration-decisions.md`.

A user should never have to understand the Data Quality Engine merely to generate a valid baseline dataset.

---

## 3. Supported Quality Profiles

The project specification defines three intended profiles:

```text
clean
slightly_dirty
dirty
```

These are presets, not replacements for individual controls.

Conceptually:

```text
clean
    -> no intentional defects

slightly_dirty
    -> low defect rates

dirty
    -> materially higher defect rates
```

The exact percentages should remain configurable.

Profiles should resolve into effective configuration so that the actual rates used for a generation are inspectable.

---

## 4. Individual Defect Controls

The engine should eventually support independent rates for defect categories.

The project plan gives examples such as:

```text
null_customer_id_rate
duplicate_order_rate
invalid_price_rate
invalid_timestamp_rate
missing_payment_rate
```

The broader supported defect classes are:

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

Each should be independently configurable where technically meaningful. fileciteturn1file0

---

## 5. Defect Rate Semantics

A configured defect rate represents the intended proportion of eligible records affected by a defect.

For example:

```text
invalid_price_rate = 0.001
```

conceptually means:

```text
approximately 0.1%
of eligible records
```

should receive an invalid price.

The actual observed rate may differ slightly because selection is probabilistic.

For sufficiently large datasets:

```text
observed_rate ≈ configured_rate
```

within an accepted tolerance.

The exact statistical tolerance belongs in `11-validation-strategy.md`.

---

## 6. Defect Selection Must Be Deterministic

Defect selection is part of generation and therefore must use the controlled randomness defined in `08-reproducibility.md`.

For:

```text
same effective configuration
+
same seed
+
same generator version
```

the same data-quality decisions should be reproducible.

Changing only the seed should normally select different records while maintaining similar defect rates.

---

## 7. Defect Independence

Defect categories should not automatically be assumed to be independent.

For example:

```text
null customer_id
```

and:

```text
malformed email
```

could be independent in one scenario.

But a future scenario could deliberately correlate them.

The default behavior should remain simple and understandable:

```text
each configured rule
    -> selects eligible records
    -> applies its defect
```

Explicit correlation should be introduced only when a business or Spark experiment requires it.

---

## 8. Eligible Population

Every quality rule should define its eligible population.

For example:

```text
invalid_price
    -> OrderItem records with a price field

invalid_timestamp
    -> records containing timestamps

missing_payment
    -> Orders that would normally have a payment
```

A defect rate should not silently be calculated over unrelated rows.

Conceptually:

```text
eligible_count
    |
    v
defect rate
    |
    v
defect target count
```

The generator should report both where useful.

---

## 9. Null Values

Null generation should be explicit and field-specific.

Examples:

```text
customer.email
customer.city
order.customer_id
order_item.product_id
payment.method
```

Not every field should be nullable merely because a generic null generator exists.

### Important distinction

A null may be:

1. naturally valid for a nullable business field
2. intentionally introduced as a data-quality defect

These must remain distinguishable at the configuration/model level.

---

## 10. Nulls in Required Keys

Nulling a primary or foreign key intentionally creates a referential-quality defect.

Examples:

```text
order.customer_id = null
order_item.order_id = null
order_item.product_id = null
```

These should be supported because they are useful for testing data-quality handling.

However, the clean profile must never generate them.

The validation system should classify them as expected violations when a dirty scenario explicitly requested them rather than reporting them as unexplained generator failures.

---

## 11. Duplicate Records

Duplicate generation should be intentional.

Example:

```text
duplicate_order_rate = 0.0005
```

A duplicate should have clearly defined semantics.

Possible forms include:

### Exact duplicate

Same record values.

```text
row A
row A
```

### Business duplicate

Two physical records representing the same business event but differing in a non-key field.

```text
same order_id
different ingestion metadata
```

The initial implementation should prefer **exact duplicate records** because they are easy to reason about and validate.

More sophisticated duplicate semantics can be introduced later.

---

## 12. Duplicate Strategy

Duplicates should not accidentally create impossible primary-key semantics unless that is the scenario being tested.

For example, if `order_id` is defined as a unique business identifier, an exact duplicate order row intentionally violates uniqueness.

That is useful for dirty-data experiments, but it must be explicitly configured.

The generator should report:

```text
duplicate rows introduced
duplicate rate
affected entity
```

---

## 13. Invalid Values

Invalid values should be domain-aware.

Examples:

```text
quantity <= 0
price < 0
invalid status
unknown category
unsupported payment method
```

The invalid value should violate a known business invariant rather than simply contain arbitrary garbage.

This makes the resulting dataset useful for validation and data-cleaning exercises.

---

## 14. Invalid Prices

A valid price has domain constraints defined by the data model.

The project specification explicitly identifies invalid prices as a supported quality problem. fileciteturn1file0

Possible defects include:

```text
negative price
zero price where prohibited
unexpected precision
unrealistically malformed numeric representation
```

The initial implementation should choose a small, clearly defined set of invalid forms rather than generating arbitrary numeric corruption.

---

## 15. Invalid Quantities

Order quantities should normally satisfy the domain invariant:

```text
quantity > 0
```

The project validation rules explicitly identify `quantity > 0` as a business consistency condition. fileciteturn1file2

A dirty scenario may introduce:

```text
quantity = 0
quantity < 0
```

The selected invalid form should be configurable or documented.

---

## 16. Invalid Timestamps

Timestamp defects should remain recognizable.

Examples:

```text
event before session_start
event after session_end
order timestamp outside configured generation range
shipment before order
return before order
```

These defects are especially valuable because they exercise temporal validation rather than merely null checking.

The clean generator must maintain:

```text
session_end >= session_start
event_timestamp within session bounds
```

as specified by the project plan. fileciteturn1file2

---

## 17. Missing Relationships

A missing relationship deliberately removes an expected association.

Examples:

```text
Order
    customer_id -> missing/invalid

OrderItem
    product_id -> missing/invalid

Payment
    expected for order -> absent

Shipment
    expected relationship -> absent
```

The project specification explicitly includes missing relationships as a supported quality problem. fileciteturn1file0

The distinction between:

```text
missing relationship
```

and:

```text
invalid foreign-key value
```

should be preserved because they test different downstream behaviors.

---

## 18. Referential Integrity Defects

The clean dataset should satisfy referential integrity.

Examples:

```text
Order.customer_id -> existing Customer
OrderItem.order_id -> existing Order
OrderItem.product_id -> existing Product
Payment.order_id -> existing Order
```

A dirty scenario may intentionally violate these relationships.

The validation layer should report:

```text
valid references
invalid references
missing references
null references
```

rather than collapsing everything into a single error count.

---

## 19. Malformed Strings

Malformed strings should target realistic data-cleaning cases.

Examples:

```text
email
phone
postal_code
name
address fields
```

Possible defects include:

```text
invalid email structure
unexpected whitespace
illegal characters
truncated values
incorrect formatting
empty strings
```

The initial implementation should use a small controlled library of malformed patterns.

Avoid generating arbitrary unreadable strings that have no analytical value.

---

## 20. Empty String vs Null

These are separate quality conditions.

```text
null
```

means no value.

```text
""
```

means a present but empty string.

The generator should treat them separately in configuration and statistics where applicable.

This distinction is useful for testing ingestion, filtering, validation, and data-cleaning logic.

---

## 21. Defect Application Order

Defect application order must be deterministic.

A preferred conceptual order is:

```text
1. Generate clean records
2. Apply value defects
3. Apply relationship defects
4. Apply duplicate defects
5. Validate according to expected quality profile
6. Write output
7. Record quality statistics
```

The exact ordering may change during implementation, but it must be explicitly defined because multiple defects can interact.

---

## 22. Defect Interaction

Example:

```text
Order
    |
    +--> invalid customer_id
    |
    +--> duplicate row
```

A duplicate of an already-corrupted row may count differently from a duplicate of the clean row.

Therefore the engine should eventually define whether rates are applied:

```text
independently to the original clean population
```

or:

```text
sequentially to the current population
```

### Initial decision

Prefer applying defect-selection policies against a clearly defined **eligible population snapshot** so that configured rates remain understandable.

Complex interactions should be introduced only when required.

---

## 23. Quality Profiles Must Be Composable

A profile should be a convenience preset.

For example:

```text
slightly_dirty
    -> low null rates
    -> low duplicate rate
    -> low invalid-value rate
```

A user should still be able to override one specific rate.

Conceptually:

```text
profile = slightly_dirty

override:
    invalid_price_rate = 0.005
```

The final effective configuration should expose the resulting values.

This follows the general configuration precedence model established in `03-configuration-decisions.md`.

---

## 24. Clean Baseline Must Remain Available

Every dirty dataset should conceptually have a clean baseline.

This allows comparisons such as:

```text
baseline
    vs
slightly_dirty
    vs
dirty
```

This is important for downstream experiments because the user should be able to isolate the effect of data-quality problems.

---

## 25. Quality Scenarios

Data quality should eventually be available through named scenarios.

Examples:

```text
clean
slightly-dirty
dirty
missing-keys
duplicate-orders
invalid-prices
invalid-timestamps
```

A scenario should modify configuration rather than require changes to entity-generation code.

The project specification explicitly requires named profiles such as `clean`, `slightly_dirty`, and `dirty`. fileciteturn1file3

---

## 26. Data Quality Statistics

Every generation should report quality statistics.

The project manifest is expected to contain data-quality statistics, and statistical validation should report null and duplicate percentages. fileciteturn1file2

Useful metrics include:

```text
null count
null percentage
duplicate row count
duplicate percentage
invalid value count
invalid timestamp count
invalid relationship count
malformed string count
```

Where practical, statistics should be reported per entity and field.

Example:

```text
orders.customer_id
    null_count = 1000
    null_rate = 0.001
```

---

## 27. Expected vs Unexpected Violations

A dirty dataset intentionally contains invalid records.

Therefore validation must distinguish:

```text
expected violation
```

from:

```text
unexpected generator bug
```

Example:

```text
configured:
null_customer_id_rate = 0.001

observed:
null_customer_id_rate = 0.00102
```

This should not fail merely because nulls exist.

Instead, validation should verify that the observed behavior is consistent with the configured dirty profile.

---

## 28. Quality Validation

Validation should answer at least four questions:

### 1. Did the requested defect occur?

```text
configured rate > 0
observed defect count > 0
```

### 2. Did it occur at the expected rate?

```text
observed rate ≈ configured rate
```

### 3. Did clean data remain clean where it should?

For example:

```text
clean profile
    -> invalid_price_count = 0
```

### 4. Did the defect affect only its intended population?

For example:

```text
invalid_price
    -> OrderItem price
```

should not accidentally corrupt:

```text
Product base_price
```

unless explicitly configured.

---

## 29. Quality and Reproducibility

The following must reproduce:

```text
which records were corrupted
which defects were applied
which duplicate rows were introduced
which relationships were removed
```

for the same generation inputs.

This means the Data Quality Engine must participate in the same deterministic random-stream strategy as the rest of the generator.

---

## 30. Quality and Distribution

Data quality must be applied without accidentally destroying the underlying clean distribution unless that is intentional.

Example:

```text
clean price distribution
        |
        v
1% invalid prices
        |
        v
dirty dataset
```

The clean population remains the reference model.

This makes statistical comparisons between clean and dirty datasets meaningful.

---

## 31. Quality and Spark Experiments

Dirty datasets are intended to support future experiments such as:

- null handling
- filtering
- deduplication
- data-quality validation
- join behavior with missing keys
- malformed-record handling
- timestamp validation
- data-cleaning pipelines

The generator should therefore make defects **controlled and measurable**, rather than merely making the dataset messy.

---

## 32. Initial Implementation Scope

The first Data Quality Engine should support:

1. configurable null rates
2. exact duplicate rows
3. invalid prices
4. invalid quantities
5. invalid timestamps
6. missing relationships
7. malformed strings
8. clean / slightly_dirty / dirty presets
9. deterministic defect selection
10. quality statistics

More advanced corruption models can follow.

---

## 33. Failure Behavior

Configuration errors should fail before generation.

Examples:

```text
negative defect rate
rate > 1
unknown quality profile
invalid field name
defect incompatible with field type
invalid relationship target
```

Generation-time failures should clearly identify:

```text
entity
field
defect type
record/chunk context where available
```

The generator should not silently ignore an invalid data-quality configuration.

---

## 34. Deferred Decisions

The following are intentionally left open:

- exact Scala API for quality rules
- exact defect-selection algorithm
- whether duplicate records preserve identical physical bytes
- advanced correlated defects
- configurable defect ordering
- exact malformed-string libraries
- statistical confidence intervals
- whether quality metadata is embedded per row
- parallel corruption strategy
- large-scale duplicate-generation optimization

These should be resolved when implementation requirements become concrete.

---

## 35. Guiding Principle

The Data Quality Engine should produce **controlled imperfection**.

The desired model is:

```text
Realistic clean dataset
        +
Explicit quality profile
        +
Deterministic defect selection
        +
Measured defect statistics
        =
Useful dirty dataset
```

Not:

```text
random corruption
+
unexplained invalid rows
+
unmeasurable defect rates
```

The core rule is:

> **Data quality problems are scenarios, not accidents: they must be configurable, reproducible, measurable, and isolated from the clean generation model.**
