# 19 — Validation and Data-Quality Architecture

## 1. Purpose

Validation is one of the core architectural responsibilities of ShopSphere.

A synthetic dataset can contain:

```text
valid IDs
valid foreign keys
valid CSV syntax
```

and still be a poor simulation of an e-commerce business.

Therefore validation must answer several different questions:

1. Is the generated data structurally valid?
2. Are references valid?
3. Do business invariants hold?
4. Does the data obey temporal rules?
5. Does the data have the intended statistical shape?
6. If a dirty scenario is enabled, were defects introduced deliberately and at the configured rate?
7. Are defects measurable and explainable?

This document defines the architecture for:

- configuration validation,
- structural validation,
- primary-key validation,
- foreign-key validation,
- business-rule validation,
- temporal validation,
- statistical validation,
- data-quality profiles,
- controlled corruption,
- quality-rule ownership,
- validation severity,
- validation reporting,
- failure and warning policy,
- reproducibility of corruption,
- scenario interaction,
- testing,
- Spark-quality workloads,
- migration.

The central principle is:

> **Validation is not a single final check. It is a layered contract system that protects both correctness and realism.**

---

# 2. Validation Philosophy

There are two fundamentally different quality dimensions.

## Correctness

The generated world must obey its structural and business contracts.

Examples:

```text
Order.customerId exists
OrderItem.orderId exists
OrderItem.productId exists
Order.total matches OrderItems
Return references an eligible OrderItem
```

## Realism

The generated world must exhibit plausible statistical behavior.

Examples:

```text
orders/customer is heterogeneous
products have a long tail
sessions/customer are not exactly uniform
returns are not independent of customer behavior
```

Correctness without realism produces a clean but artificial dataset.

Realism without correctness produces an unreliable dataset.

ShopSphere requires both.

---

# 3. Validation Layers

The target validation architecture contains the following layers:

```text
Configuration Validation
        ↓
Reference-Data Validation
        ↓
Structural Validation
        ↓
Primary-Key Validation
        ↓
Foreign-Key Validation
        ↓
Business-Rule Validation
        ↓
Temporal Validation
        ↓
Statistical Validation
        ↓
Data-Quality Scenario Validation
        ↓
Global Validation Report
```

Not every layer must run at exactly the same time.

---

# 4. Configuration Validation

Configuration validation occurs before generation.

Its purpose is to prevent impossible generation requests.

Examples:

```text
negative customer count
invalid probability
unknown strategy
invalid distribution parameter
invalid profile
invalid scenario
```

---

# 5. Configuration Validation Is Not Dataset Validation

These are different.

Configuration validation asks:

> "Can this generator run with these settings?"

Dataset validation asks:

> "Did the generated dataset obey its contracts?"

---

# 6. Reference-Data Validation

Reference data must be validated before domain generation.

Examples:

```text
Country IDs unique
State IDs unique
City IDs unique
Building IDs unique
Postal codes valid
Product catalog references valid
Category references valid
Brand references valid
```

---

# 7. Reference Data Is Foundational

If reference data is invalid, downstream generation can become invalid.

Therefore:

```text
reference validation
```

should happen before generating millions of transactional records.

---

# 8. Primary-Key Validation

Every generated entity with an ID should satisfy:

```text
ID exists
ID is unique
ID is non-empty
```

unless a specific data-quality scenario intentionally violates uniqueness.

---

# 9. Primary-Key Validation Examples

Customer:

```text
customer.id unique
```

Order:

```text
order.id unique
```

Product:

```text
product.id unique
```

Event:

```text
event.id unique
```

---

# 10. Primary-Key Scope

Uniqueness scope must be explicit.

Usually:

```text
Customer IDs unique within Customer entity
Order IDs unique within Order entity
```

If global IDs are required, that must be a separate contract.

---

# 11. Foreign-Key Validation

Foreign-key validation verifies:

```text
referencing ID
        ↓
referenced entity exists
```

Examples:

```text
Order.customerId → Customer.id
OrderItem.orderId → Order.id
OrderItem.productId → Product.id
Payment.orderId → Order.id
Shipment.orderId → Order.id
Session.customerId → Customer.id
Event.sessionId → Session.id
```

---

# 12. Foreign-Key Validation Is Structural

Foreign-key validation proves existence.

It does not prove business correctness.

For example:

```text
Shipment.addressId
```

may reference an existing Address but still belong to the wrong Customer.

That requires a business-rule check.

---

# 13. Relationship Validation

Cross-domain relationship validation checks stronger invariants.

Example:

```text
Shipment
   ↓
Address
   ↓
Customer
```

must be consistent with:

```text
Shipment
   ↓
Order
   ↓
Customer
```

if the domain model requires the shipment address to belong to the order customer.

---

# 14. Business-Rule Validation

Business validation checks domain invariants.

Examples:

```text
Order total = sum of OrderItems
OrderItem quantity > 0
Return quantity <= purchased quantity
Return only occurs for eligible orders/items
Shipment cannot precede Order
Payment cannot precede Order
```

---

# 15. Business Rules Must Have Owners

A business rule should belong to the domain that understands its meaning.

Examples:

```text
Order total
→ Order / OrderItem domain

Return eligibility
→ Return domain

Shipment timing
→ Shipment domain

Event sequence
→ Event domain
```

Global validation orchestrates these rules but should not redefine them.

---

# 16. Validation Rule Abstraction

A reusable validation abstraction may eventually look conceptually like:

```scala
trait ValidationRule[A] {
  def name: String
  def validate(value: A): ValidationResult
}
```

This is illustrative.

Do not introduce generic abstractions until multiple rules genuinely benefit from them.

---

# 17. Validation Result

A validation result should communicate:

```text
rule
severity
status
message
affected records/count
```

Conceptually:

```text
PASS
WARN
FAIL
```

---

# 18. Severity

At minimum, use:

```text
INFO
WARN
ERROR
```

or an equivalent project-wide severity model.

The important point is that severity semantics are explicit.

---

# 19. Failure vs Warning

Not every anomaly should stop generation.

Examples:

```text
invalid foreign key
```

should normally be:

```text
FAIL
```

while:

```text
slightly outside a statistical target
```

may be:

```text
WARN
```

depending on tolerance.

---

# 20. Hard Invariants

Hard invariants should fail validation.

Examples:

```text
duplicate primary key
orphan foreign key
negative quantity
invalid timestamp ordering
Order total mismatch
```

---

# 21. Statistical Targets

Statistical expectations should usually have tolerances.

Example:

```text
target return rate = 8%
acceptable range = 7%–9%
```

A small finite-sample deviation should not necessarily fail generation.

---

# 22. Statistical Validation

Statistical validation measures whether the generated population resembles the intended model.

Potential metrics:

```text
mean
median
variance
percentiles
category share
brand share
top-k concentration
conversion rate
return rate
AOV
```

---

# 23. Statistical Validation Is Not Exact Matching

Avoid:

```text
assert observedReturnRate == 0.08
```

for probabilistic generation.

Prefer:

```text
observedReturnRate within configured tolerance
```

---

# 24. Statistical Validation by Domain

Each domain should own domain-specific metrics.

Customer:

```text
orders/customer
sessions/customer
activity distribution
```

Product:

```text
sales/product
product popularity concentration
price distribution
```

Order:

```text
items/order
AOV
order status distribution
```

Session:

```text
events/session
conversion rate
session duration
```

Return:

```text
return rate
returns/customer
returns/product
```

---

# 25. Global Statistics

Global statistics can aggregate domain-level measurements.

For example:

```text
total order value
total records
overall return rate
overall conversion rate
```

Global statistics should not duplicate every domain rule.

---

# 26. Validation Ownership

A practical ownership model is:

```text
Domain package
   ↓
domain-specific validation rules

validation/
   ↓
cross-domain orchestration
```

---

# 27. Domain Validation Package

A domain may eventually contain:

```text
customer/
  validation/
    CustomerValidator.scala
```

when domain-specific validation becomes substantial.

Do not create a validator class merely to wrap one trivial check.

---

# 28. Global Validation

The system-level validator coordinates:

```text
Customer validation
Address validation
Product validation
Order validation
...
```

and:

```text
cross-domain validation
```

---

# 29. Validation Order

A sensible validation order is:

```text
configuration
→ reference data
→ entity structure
→ primary keys
→ foreign keys
→ business rules
→ temporal rules
→ statistical realism
```

This prevents noisy downstream failures when basic integrity is already broken.

---

# 30. Short-Circuit vs Aggregate

Validation may either:

```text
stop at first failure
```

or:

```text
collect multiple failures
```

For data-generation diagnostics, aggregation is generally more useful.

---

# 31. Practical Validation Strategy

A useful approach is:

```text
fatal configuration errors
        ↓
stop

dataset validation
        ↓
collect errors
        ↓
report all important failures
```

---

# 32. Validation Cost

Validation itself can become expensive at large scale.

Therefore distinguish:

```text
full validation
sampled validation
cheap validation
expensive validation
```

---

# 33. Cheap Structural Validation

Examples:

```text
ID format
record count
simple range checks
```

These can often run during generation or streaming.

---

# 34. Expensive Cross-Domain Validation

Examples:

```text
large foreign-key joins
Customer → Order consistency
Order → OrderItem totals
```

may require indexes or batch processing.

---

# 35. Streaming Validation

Some rules can be checked as records are generated.

Example:

```text
OrderItem.quantity > 0
```

No global dataset is required.

---

# 36. Batch Validation

Some rules require the complete generated population.

Example:

```text
all Customer IDs unique
```

or:

```text
product popularity distribution
```

---

# 37. Hybrid Validation

The target architecture should use:

```text
generation-time checks
+
post-generation checks
```

to balance safety and performance.

---

# 38. Generation-Time Validation

Use for:

```text
local invariants
impossible states
invalid references
```

Example:

```text
Address cannot be created if selected Building has no valid Area postal code.
```

---

# 39. Post-Generation Validation

Use for:

```text
population statistics
cross-record uniqueness
distribution shape
correlation
```

---

# 40. Data-Quality Architecture

Data-quality scenarios intentionally introduce defects.

The key principle is:

> **Dirty data must be generated deliberately, not accidentally.**

---

# 41. Clean Baseline

The default generation mode should be:

```text
clean
```

meaning intentional corruption is disabled.

---

# 42. Slightly Dirty

A slightly dirty scenario may introduce low-rate issues such as:

```text
missing optional values
minor formatting inconsistencies
small number of duplicates
limited invalid references
```

The exact defect types should be configured.

---

# 43. Dirty

A dirty scenario may introduce larger rates of:

```text
missing values
duplicate records
invalid references
invalid formats
timestamp anomalies
business-rule violations
```

---

# 44. Quality Scenario Separation

Data-quality generation should be architecturally separate from clean business generation.

Preferred flow:

```text
clean generated data
       ↓
quality transformation
       ↓
dirty dataset
```

This makes scenario behavior measurable.

---

# 45. Why Post-Generation Corruption Is Useful

It allows comparison:

```text
baseline dataset
vs
same baseline + controlled defects
```

without changing the underlying business population.

---

# 46. Quality Transformation

Conceptually:

```text
QualityTransformer
   ↓
selected records
   ↓
configured corruption
```

It should operate according to explicit rules.

---

# 47. Quality Rule Examples

Potential rules:

```text
MissingValueRule
DuplicateRecordRule
InvalidReferenceRule
InvalidFormatRule
TimestampCorruptionRule
OutOfRangeValueRule
```

These names communicate intent.

---

# 48. Quality Rule Ownership

A quality rule should know:

```text
which entity it affects
what defect it introduces
how it is parameterized
```

It should not contain unrelated business generation logic.

---

# 49. Quality Rule Configuration

Example conceptual configuration:

```hocon
quality {
  profile = "slightly_dirty"

  rules {
    customerEmailMissingRate = 0.01
    duplicateOrderRate = 0.002
  }
}
```

The exact schema should be finalized during implementation.

---

# 50. Quality Rate Semantics

Rates should have clear meanings.

For example:

```text
missingRate = 0.01
```

should explicitly mean:

> Approximately 1% of eligible records/fields are affected.

---

# 51. Eligible Population

Every quality rule should define its denominator.

For example:

```text
1% of Customers
```

is different from:

```text
1% of Customer fields
```

---

# 52. Quality Selection

Quality corruption should use deterministic random streams.

Conceptually:

```text
root/quality/customer/missing
```

This follows the reproducibility architecture.

---

# 53. Quality Reproducibility

Given:

```text
same clean dataset
same quality configuration
same seed
same generator version
```

the corrupted dataset should be reproducible.

---

# 54. Quality Scenario Isolation

Enabling a quality scenario should ideally not alter clean values before corruption.

For example:

```text
clean Customer.name
```

should remain the same when only:

```text
missing email
```

corruption is enabled.

---

# 55. Quality Corruption vs Business Behavior

Do not model:

```text
missing email
```

as a customer behavioral trait.

It is a data-quality defect.

---

# 56. Invalid Reference Rule

An invalid-reference scenario may intentionally change:

```text
Order.customerId
```

to an ID that does not exist.

The validator should detect it.

---

# 57. Duplicate Rule

A duplicate-record scenario may intentionally duplicate:

```text
Order
```

records.

The expected result is:

```text
primary-key validation failure
```

if the duplicated record preserves the same ID.

---

# 58. Missing-Value Rule

A missing-value scenario should distinguish:

```text
required field
```

from:

```text
optional field
```

The resulting validation severity may differ.

---

# 59. Timestamp Corruption

Possible defects:

```text
shipment before order
return before delivery
event outside session
```

These should be detectable by temporal validators.

---

# 60. Business-Rule Corruption

A quality scenario can intentionally violate:

```text
Order.total
```

or:

```text
Return.quantity
```

for testing downstream data-quality pipelines.

---

# 61. Controlled Corruption Must Be Measurable

The generator should report:

```text
configured corruption rate
actual affected count
actual affected rate
```

---

# 62. Quality Manifest

The manifest should eventually include:

```text
quality profile
quality rules
configured rates
actual affected counts
validation outcome
```

---

# 63. Quality and Validation Relationship

A dirty scenario should not make the validator appear broken.

Instead:

```text
expected violations
```

should be distinguishable from:

```text
unexpected violations
```

---

# 64. Expected Violations

For a configured dirty scenario:

```text
invalid-reference count = 100
```

the validator should report:

```text
known/expected quality violations
```

separately from:

```text
unexpected integrity failures
```

---

# 65. Quality Validation Modes

A useful future distinction:

```text
strict clean mode
```

expects:

```text
zero intentional violations
```

and:

```text
scenario-aware mode
```

allows configured violations while verifying they occur as intended.

---

# 66. Validation Result Categories

Conceptually:

```text
PASS
EXPECTED_VIOLATION
UNEXPECTED_FAILURE
WARNING
```

This is useful for dirty-data scenarios.

---

# 67. Quality Scenario Contract

A quality scenario should define:

```text
target entity
target field/relationship
corruption mechanism
selection rate
expected validation rule
```

---

# 68. Statistical Quality Validation

Quality scenarios may also require statistical checks.

Example:

```text
configured missing email rate = 1%
observed = 0.97%
```

should be within tolerance.

---

# 69. Quality Scenario Tolerance

Because corruption may be sampled probabilistically, observed rates should normally use tolerances.

For exact deterministic counts, exact matching may be appropriate.

---

# 70. Deterministic vs Probabilistic Corruption

Two modes may be useful.

## Probabilistic

Each eligible record has probability p of corruption.

## Exact-count

Exactly N records are selected.

Both have different statistical properties.

---

# 71. Exact Corruption Allocation

For:

```text
1,000,000 records
1% corruption
```

exact-count mode can guarantee:

```text
10,000 affected
```

This is useful for benchmark experiments.

---

# 72. Probabilistic Corruption

Probabilistic mode may produce:

```text
9,934
```

or:

```text
10,071
```

and better represents random defect occurrence.

---

# 73. Quality Configuration Should State Which Mode

Avoid ambiguity.

Example:

```text
mode = "probabilistic"
rate = 0.01
```

or:

```text
mode = "exact-count"
count = 10000
```

---

# 74. Validation Rule Registry

A central registry may eventually map:

```text
rule name
→ validator
```

But avoid an enormous registry with all logic embedded inside it.

The registry should only coordinate.

---

# 75. Domain Validator Composition

The global validator can compose:

```text
CustomerValidator
AddressValidator
ProductValidator
OrderValidator
...
```

plus:

```text
CrossDomainValidator
```

---

# 76. Cross-Domain Validator

Cross-domain validation is appropriate for rules such as:

```text
Shipment address belongs to Order customer
OrderItem product exists and is valid
Return belongs to OrderItem
Event customer/session relationship is consistent
```

---

# 77. Validation Dependency Graph

Validation can follow the same dependency structure as generation:

```text
Reference Data
      ↓
Customer / Product / Address
      ↓
Session / Order
      ↓
Event / OrderItem
      ↓
Payment / Shipment
      ↓
Return
```

---

# 78. Validation Does Not Mutate Data

Validators should be read-only.

They should not repair invalid records.

---

# 79. Repair Is a Separate Concern

If the project later needs repair workflows:

```text
validation
   ↓
repair proposal
   ↓
repair transformation
```

should remain separate.

The generator itself should not silently repair defects.

---

# 80. Validation and Output

Validation should work on domain data before output when possible.

It should not depend on CSV parsing to validate business objects.

Output-level validation can be added separately to verify serialization.

---

# 81. Serialization Validation

CSV-level checks can verify:

```text
headers
column count
escaping
line format
record count
```

These are output concerns.

---

# 82. Business Validation vs Serialization Validation

Keep them separate.

Business:

```text
Order.total
```

Serialization:

```text
CSV has correct number of columns
```

---

# 83. Validation Reports

A useful report should summarize:

```text
overall status
rule count
passed
warnings
expected violations
unexpected failures
```

---

# 84. Rule-Level Report

For each rule:

```text
rule
entity
severity
expected
actual
status
message
```

---

# 85. Example Validation Summary

Conceptually:

```text
Validation:
  Structural: PASS
  Primary-key: PASS
  Foreign-key: PASS
  Business-rule: PASS
  Temporal: PASS
  Statistical: WARN
  Quality scenario: PASS
```

---

# 86. Statistical Warning Example

```text
Product top-1 concentration:
expected >= 2.0%
observed = 1.7%
status = WARN
```

This is more useful than simply reporting:

```text
FAIL
```

---

# 87. Failure Thresholds

Thresholds should be configuration-driven where appropriate.

For example:

```text
AOV tolerance
return-rate tolerance
payment-share tolerance
```

---

# 88. Avoid Over-Validation

Not every property requires a validation rule.

Focus on:

```text
business invariants
high-risk relationships
important statistical characteristics
```

---

# 89. Validation Cost Budget

At very large scale, validation can become a substantial part of runtime.

Therefore validation profiles may eventually support:

```text
minimal
standard
full
```

---

# 90. Minimal Validation

Useful during development:

```text
basic structural checks
primary keys
foreign keys
```

---

# 91. Standard Validation

Adds:

```text
business rules
temporal rules
core statistics
```

---

# 92. Full Validation

Adds:

```text
advanced correlations
distribution diagnostics
quality-rate verification
expensive cross-domain checks
```

---

# 93. Validation Profile vs Generation Profile

Do not confuse:

```text
dataset size profile
```

with:

```text
validation depth profile
```

They are separate concerns.

---

# 94. Validation and Spark

Validation architecture also prepares the project for future Spark workloads.

Potential future validation workloads include:

```text
orphan detection
duplicate detection
referential joins
distribution aggregation
top-k concentration
temporal anomaly detection
```

---

# 95. Data-Quality Spark Workloads

Dirty datasets are particularly useful for:

```text
duplicate detection
null analysis
referential integrity checks
bad timestamp detection
data-quality aggregation
```

---

# 96. Validation and Skew

Validation itself can be affected by skew.

For example:

```text
one Customer with millions of Orders
```

can create a hot aggregation key.

This is useful for future Spark performance experiments.

---

# 97. Statistical Validation of Skew

Skew validation should report:

```text
top 1% share
top 5% share
max/median ratio
Gini-like concentration metrics
```

where appropriate.

---

# 98. Correlation Validation

As realism improves, validation should test relationships such as:

```text
customer activity ↔ sessions
customer activity ↔ orders
price sensitivity ↔ product price tier
category affinity ↔ product purchases
return propensity ↔ actual returns
```

---

# 99. Correlation Need Not Be Exact

The goal is not to force a particular correlation coefficient unless explicitly configured.

The goal is to ensure the intended relationship exists and is statistically plausible.

---

# 100. Correlation Regression

A future realism regression test can assert:

```text
expected positive correlation
```

rather than a fragile exact coefficient.

---

# 101. Temporal Validation

Temporal validation should verify causal ordering.

Examples:

```text
event within session
order after purchase interaction where modeled
payment not before order
shipment not before order
delivery not before shipment
return not before delivery
```

---

# 102. Session Event Boundary

For every Event:

```text
session.start <= event.time <= session.end
```

where the model defines bounded sessions.

---

# 103. Order Item Boundary

OrderItems should be associated with the Order's transaction context.

If item-level timestamps are introduced, they must be coherent with Order time.

---

# 104. Payment Timing

If payment timing is modeled:

```text
order.time <= payment.time
```

unless the business model explicitly permits preauthorization before order creation.

---

# 105. Shipment Timing

Normally:

```text
order.time <= shipment.time
```

and:

```text
shipment.time <= delivery.time
```

if delivery time is represented.

---

# 106. Return Timing

Normally:

```text
delivery.time <= return.time
```

when returns require delivered goods.

Exceptions must be explicit.

---

# 107. Validation of Current Simplifications

The current baseline intentionally has simpler behavior.

Validation should reflect the current contract rather than imposing future realism prematurely.

For example:

```text
exact 3 sessions/customer
exact 8 events/session
```

are currently valid if those are current generation contracts.

---

# 108. Do Not Validate Future Behavior Too Early

A validator should not fail the current implementation because:

```text
sessions/customer are too uniform
```

if heterogeneity has not yet been implemented.

Instead, track this as a realism gap.

---

# 109. Validation Evolution

As realism improves:

```text
current invariant
   ↓
new behavior model
   ↓
new statistical validator
```

The validator should evolve with the domain contract.

---

# 110. Test Architecture

Validation tests should exist at multiple levels.

```text
unit
domain
cross-domain
integration
end-to-end
statistical
quality-scenario
```

---

# 111. Unit Validation Tests

Test individual rules.

Example:

```text
OrderTotalValidator
```

given:

```text
items total = 100
order total = 110
```

should fail.

---

# 112. Domain Validation Tests

Test a generated domain population.

Example:

```text
all generated Products reference valid Category and Brand
```

---

# 113. Cross-Domain Tests

Test:

```text
Orders + Customers
Orders + OrderItems + Products
Orders + Shipments + Addresses
```

---

# 114. Quality Scenario Tests

Given:

```text
dirty profile
```

verify:

```text
expected defects appear
unexpected defects do not appear
validation detects expected defects
```

---

# 115. Statistical Tests

Use deterministic seeds and sufficient sample sizes.

Verify:

```text
distribution shape
concentration
target ranges
```

---

# 116. Full End-to-End Test

A small dataset should be generated and validated through:

```text
configuration
generation
validation
statistics
output
```

This protects integration.

---

# 117. Validation Regression

The project should preserve a baseline of important validation metrics.

Intentional realism changes should update the baseline deliberately.

---

# 118. Validation Failure Messages

Messages should contain enough context to debug.

Bad:

```text
validation failed
```

Better:

```text
OrderTotalMismatch:
Order ORDER_000123 total=1250.00,
sum(OrderItems)=1190.00
```

---

# 119. Large-Scale Error Reporting

Do not emit millions of duplicate errors.

Use:

```text
sample violations
+
aggregate count
+
representative examples
```

---

# 120. Error Sampling

For a rule with:

```text
10,000 violations
```

the report can provide:

```text
count = 10,000
examples = first/selected 10
```

while retaining the aggregate count.

---

# 121. Validation Observability

Metrics should include:

```text
records checked
rules executed
duration
failures
warnings
expected violations
```

---

# 122. Validation Performance

Validation duration should eventually be observable separately from:

```text
generation
writing
statistics
```

This helps identify bottlenecks.

---

# 123. Validation and Reproducibility

A validation failure should be reproducible using:

```text
seed
configuration
reference-data version
generator version
```

---

# 124. Quality Scenario Reproduction

A particular corrupted record should ideally be traceable to:

```text
quality rule
seed stream
record identity
```

This makes data-quality debugging much easier.

---

# 125. Validation and Randomness

Validation should not consume generation RNG streams.

If statistical sampling is necessary:

```text
validationRandom
```

should be separate.

---

# 126. Validation and Configuration

Validation tolerances should be part of effective configuration when they affect pass/fail decisions.

This makes validation results reproducible.

---

# 127. Validation and Manifest

The final manifest should eventually record:

```text
validation status
rule counts
warnings
expected violations
unexpected failures
```

---

# 128. Validation and Scenarios

Scenario-specific expectations must be explicit.

Example:

```text
hot-product
```

should change:

```text
product concentration target
```

rather than causing the normal concentration validator to fail.

---

# 129. Scenario-Aware Statistical Targets

Each scenario may have its own expected ranges.

Conceptually:

```text
baseline:
  topProductShare = ...

hot-product:
  topProductShare = ...
```

---

# 130. Scenario-Aware Quality Expectations

Similarly:

```text
clean:
  invalidReferenceCount = 0

dirty:
  invalidReferenceCount ≈ configured rate
```

---

# 131. Validation Contract

The complete contract is:

```text
Configuration
    → valid before generation

Generated data
    → structurally valid

Relationships
    → referentially valid

Business rules
    → coherent

Temporal behavior
    → causally valid

Statistics
    → within intended ranges

Quality scenarios
    → defects intentional and measurable
```

---

# 132. Validation Architecture Diagram

```text
                    Generation
                        │
                        ▼
                Generated Dataset
                        │
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
 Structural          Business        Statistical
 Validation          Validation      Validation
       │                │                │
       └────────────────┼────────────────┘
                        ▼
                 Quality Scenario
                     Validation
                        │
                        ▼
                Global Validation
                      Report
```

Configuration validation occurs before the generation boundary.

---

# 133. Explicit Architecture Decisions

## Decision 1

Validation is layered rather than implemented as one monolithic validator.

## Decision 2

Configuration validation happens before generation.

## Decision 3

Reference data is validated before dependent generation.

## Decision 4

Primary-key and foreign-key validation are distinct.

## Decision 5

Business-rule validation is distinct from structural integrity.

## Decision 6

Temporal validation is a first-class validation category.

## Decision 7

Statistical validation is required for realism.

## Decision 8

Domain-specific validation belongs with the domain when substantial.

## Decision 9

Global validation orchestrates domain and cross-domain rules.

## Decision 10

Validators are read-only and do not silently repair records.

## Decision 11

Clean generation is the default.

## Decision 12

Dirty data is introduced through explicit, deterministic quality transformations.

## Decision 13

Quality scenarios must be measurable and reproducible.

## Decision 14

Expected quality violations are reported separately from unexpected failures.

## Decision 15

Statistical targets use tolerances unless exact counts are explicitly required.

## Decision 16

Validation depth should eventually be configurable independently from generation scale.

## Decision 17

Validation should support both generation-time and post-generation checks.

## Decision 18

Large-scale validation reports aggregate failures and provide representative examples rather than logging every defect.

## Decision 19

Scenario-specific statistical expectations must be supported.

## Decision 20

Validation metadata belongs in the final generation manifest.

---

# 134. Current Baseline

The existing project already demonstrates a useful baseline:

```text
Structural PASS
Primary-key PASS
Foreign-key PASS
Business-rule PASS
```

This is a strong foundation.

The next maturity step is to formalize these checks under the layered architecture and add:

```text
temporal validation
statistical realism validation
scenario-aware quality validation
```

as those behaviors are implemented.

---

# 135. Current Simplifications

The current baseline intentionally has:

```text
fixed Sessions/customer
fixed Events/session
narrow basket-size distribution
limited customer behavioral correlation
limited temporal behavior
simple return behavior
```

These should not be treated as validation failures today.

They are documented realism gaps.

---

# 136. Future Validation Maturity

The target is:

```text
Level 1 — structural integrity
Level 2 — relationship integrity
Level 3 — business invariants
Level 4 — temporal coherence
Level 5 — distribution validation
Level 6 — correlation validation
Level 7 — scenario-aware validation
```

---

# 137. Final Principle

The purpose of validation is not merely to prove:

> "The generator did not crash."

It must prove:

> **"The generated synthetic world obeys its structural contracts, business rules, temporal constraints, and intended statistical behavior—and any intentional defects are controlled, reproducible, and measurable."**

This makes validation part of the generator's architecture rather than an afterthought.

---

# 138. Next Documentation Stage

The next document should define:

```text
20 — Skew, Data-Quality Scenarios, and Workload Design
```

It should consolidate:

- hot customers,
- hot products,
- hot categories,
- geographic skew,
- relationship skew,
- quality scenarios,
- scenario composition,
- controlled corruption,
- workload-oriented dataset shapes,
- Spark join skew,
- partitioning implications,
- benchmark scenarios,
- reproducibility,
- scenario metadata,
- performance-lab use cases.

The goal is to make scenarios first-class dataset designs rather than collections of independent switches.
