# 13 — Testing Strategy

## 1. Purpose

This document defines the testing strategy for the `ecommerce-data-generation` project.

The generator is not simply a random-data utility. It is responsible for producing a dataset that satisfies:

- a declared schema
- configured cardinalities
- realistic distributions
- cross-entity relationships
- business invariants
- temporal rules
- controlled data-quality behavior
- controlled skew
- reproducibility requirements
- output-format and storage contracts

Testing must therefore verify both **individual generation components** and the **properties of the generated dataset as a whole**.

---

## 2. Testing Philosophy

The testing strategy follows the project's implementation priorities:

```text
correctness
    ↓
realism
    ↓
relationships
    ↓
configurability
    ↓
reproducibility
    ↓
scale
    ↓
scenarios
    ↓
generation performance
```

A faster generator that produces invalid data is not a successful implementation.

The test suite should make incorrect behavior fail close to its source and should also contain end-to-end tests capable of detecting interactions between components.

---

## 3. Testing Layers

Testing is divided into the following layers:

1. unit tests
2. property-based tests
3. component/integration tests
4. dataset validation tests
5. reproducibility tests
6. scenario tests
7. output-format tests
8. regression tests
9. performance tests
10. end-to-end tests

Not every test needs to exercise the entire generator.

The preferred approach is to test the smallest meaningful unit first and reserve expensive full-generation tests for integration and end-to-end coverage.

---

## 4. Unit Testing

Unit tests verify deterministic, isolated behavior of individual components.

Primary unit-test targets include:

- configuration parsing
- configuration validation
- seed handling
- random/distribution utilities
- categorical distributions
- numeric distributions
- long-tail distributions
- date/time generation
- identifier generation
- geography generation
- product/category/brand generation
- customer generation
- order generation
- order-item generation
- payment generation
- shipment generation
- return generation
- session generation
- event generation
- data-quality transformations
- skew selection
- validation rules
- CSV serialization
- file naming
- manifest construction

A unit test should avoid depending on the filesystem or the complete dataset unless the behavior under test specifically belongs to that layer.

---

## 5. Configuration Tests

Configuration is part of the public contract of the generator.

Tests should verify:

- valid configuration is accepted
- missing required configuration is rejected
- invalid values are rejected
- unsupported enum/profile values are rejected
- conflicting settings are rejected
- default values are applied correctly
- configuration overrides behave predictably
- scale profiles resolve correctly
- cardinality profiles resolve correctly
- data-quality profiles resolve correctly
- skew configuration resolves correctly
- scenario configuration resolves correctly

Examples of invalid configurations include:

```text
negative row count
zero file count
invalid date range
unknown profile
invalid probability
invalid percentage
invalid relationship reference
```

Configuration validation should fail before expensive generation begins.

---

## 6. Randomness and Seed Tests

Randomness is central to generation, but tests must not depend on accidental random outcomes.

The generator should expose deterministic behavior through an explicit seed.

Tests should verify:

### Same seed

```text
same configuration
+
same seed
+
same generator version
    ↓
equivalent generated dataset
```

### Different seed

Different seeds should normally produce different datasets while still satisfying the same statistical and business contracts.

The test should not require every record to differ. It should verify that the generated artifacts are not incorrectly identical because the seed is ignored or mishandled.

---

## 7. Distribution Tests

Distribution logic should be tested separately from entity generation.

For a configured categorical distribution:

```text
A = 70%
B = 20%
C = 10%
```

tests should verify that sufficiently large samples fall within an accepted tolerance around the configured proportions.

Tests should account for sampling variation.

They should not expect:

```text
A == exactly 70.000%
```

for every finite random sample.

The testing framework should therefore support statistical tolerances appropriate to sample size.

---

## 8. Long-Tail Distribution Tests

The project requires realistic popularity behavior, especially for products.

Tests should verify properties such as:

- a small number of products receive disproportionately high activity
- many products receive lower activity
- the distribution is not accidentally uniform
- configured skew changes the concentration as intended
- deterministic seeds reproduce the same distribution

The exact statistical test can evolve as the distribution model becomes more sophisticated.

The important requirement is that tests validate the **shape and intent** of the distribution rather than a single hard-coded sequence of values.

---

## 9. Property-Based Testing

Property-based testing is particularly valuable because the generator has many combinations of:

- scales
- cardinalities
- seeds
- scenarios
- data-quality profiles
- skew settings
- date ranges
- file counts

Instead of testing only individual examples, property-based tests can generate many valid configurations and verify invariant properties.

Examples:

### Identifier uniqueness

For an entity configured to have unique IDs:

```text
count(rows) == count(distinct(id))
```

### Valid references

For every foreign key:

```text
foreign_key ∈ referenced_entity_ids
```

### Quantity

```text
quantity > 0
```

when the clean business model requires positive quantity.

### Temporal ordering

```text
event_time >= session_start
```

when required by the model.

### Derived totals

```text
line_total = quantity × unit_price
```

within the defined numeric precision rules.

---

## 10. Relationship Tests

Relationships are a major source of generator defects and must have dedicated tests.

Tests should verify:

- every order references an existing customer
- every order item references an existing order
- every order item references an existing product
- every payment references an existing order
- every shipment references an existing order
- every return references a valid order/order-item relationship where applicable
- sessions reference valid customers
- events reference valid sessions/customers where configured
- product category references are valid
- product brand references are valid
- address customer references are valid

Tests should also verify relationship cardinality where the model defines it.

---

## 11. Business-Rule Tests

The generator must preserve business consistency in clean datasets.

Examples include:

### Order totals

```text
order_total ≈ Σ(order_item.line_total)
```

subject to the project's rounding/precision rules.

### Payment amount

```text
payment amount
```

must be consistent with the associated order according to the payment model.

### Shipment timing

A shipment should not occur before the associated order when the business scenario forbids it.

### Return timing

A return should satisfy the configured return-window rules.

### Product hierarchy

A product must reference a valid category and brand.

These rules should be tested independently where possible and again through end-to-end generation.

---

## 12. Temporal Tests

Time is a first-class dimension of the synthetic dataset.

Tests should verify:

- generated timestamps fall within the configured range
- created timestamps precede relevant lifecycle events
- order timestamps precede shipment timestamps where required
- session events fall within their session windows
- event sequences respect required ordering
- date ranges are interpreted consistently
- timezone handling is deterministic

Boundary conditions should receive explicit tests:

```text
start of range
end of range
same-day events
minimum lifecycle duration
maximum configured duration
```

---

## 13. Data-Quality Tests

Data-quality behavior must be tested separately from clean-data generation.

For example, if a scenario specifies:

```text
email_null_rate = 2%
```

tests should verify that the resulting rate is approximately the configured rate within an appropriate tolerance.

Tests should also verify that quality problems are injected into the intended fields and do not accidentally alter unrelated fields.

Important distinction:

```text
clean generator
    → business invariants should hold

dirty generator
    → configured violations should appear
    → unrelated invariants should remain intact where possible
```

The exact expected violations belong to the data-quality configuration.

---

## 14. Skew Tests

Skew is an explicit scenario dimension.

Tests should verify:

- skew disabled produces the baseline distribution
- customer skew concentrates activity on configured hot customers
- product skew concentrates activity on configured hot products
- configured skew intensity changes concentration predictably
- hot-entity selection is reproducible
- skew does not unintentionally violate referential integrity

Skew tests should inspect aggregate behavior rather than expecting a specific random record sequence.

---

## 15. Cardinality Tests

Cardinality is a core generator feature.

For every supported cardinality mode, tests should verify:

- expected row volume
- expected unique counts
- relationship-compatible cardinalities
- configured reuse behavior

For example:

```text
customer_count = 100,000
```

should result in the configured number of customer records.

If a field is configured with a cardinality constraint:

```text
distinct(field) <= configured_cardinality
```

or the stronger equality requirement where explicitly specified.

Tests must distinguish:

- row count
- distinct count
- null count
- effective cardinality

These are different metrics.

---

## 16. Scale Profile Tests

The project defines scale profiles such as:

```text
small
medium
large
xlarge
```

Tests should verify that each profile resolves into valid generation parameters.

The automated test suite should not necessarily execute the full `xlarge` dataset during every build.

Instead:

- small profile → routine CI coverage
- medium profile → integration coverage
- large profile → selected performance/integration runs
- xlarge profile → dedicated performance runs

This prevents normal development feedback from becoming prohibitively slow.

---

## 17. Output Writer Tests

The CSV writer requires dedicated tests.

Tests should verify:

- correct header
- correct column ordering
- correct delimiter handling
- correct quote escaping
- correct null representation
- UTF-8 output
- correct line endings
- correct record count
- deterministic file naming
- deterministic file ordering
- multiple-part output
- empty/small datasets
- large file/chunk behavior

A round-trip test should be included:

```text
generated records
      ↓
CSV writer
      ↓
CSV reader/parser
      ↓
parsed records
```

The parsed representation should be equivalent to the original records according to the output contract.

---

## 18. File Layout Tests

Physical output is part of the project contract.

Tests should verify:

- expected generation directory exists
- entity directories exist
- expected number of part files exists
- part numbers are sequential
- no unexpected files exist
- file counts match configuration
- records are distributed according to the configured chunking strategy
- manifest file references are valid

For example:

```text
configured file_count = 4

expected:
part-00000.csv
part-00001.csv
part-00002.csv
part-00003.csv
```

---

## 19. Manifest Tests

The manifest should be tested as a first-class artifact.

Tests should verify that it contains:

- generation ID
- seed
- profile
- scenario
- effective configuration
- date range
- entity row counts
- unique counts
- file counts
- file sizes
- validation status
- distribution statistics
- data-quality statistics
- output-format metadata

Manifest totals should agree with the actual generated files.

The manifest should never report successful completion when validation has failed.

---

## 20. Validation Tests

The validator itself requires negative tests.

The test suite should deliberately construct invalid datasets and verify that the correct validation rule detects them.

Examples:

```text
invalid foreign key
duplicate required ID
negative quantity
invalid price
invalid temporal ordering
invalid category reference
incorrect derived total
unexpected null
distribution outside tolerance
incorrect output file count
```

Tests should verify both:

1. that invalid data is detected
2. that valid data is accepted

The validator must not silently repair the invalid dataset during validation.

---

## 21. Reproducibility Tests

Reproducibility should be tested at multiple levels.

### Record level

Same seed/configuration should produce equivalent generated records.

### Dataset level

Entity row counts, distributions, relationships, and relevant values should match.

### Output level

Where deterministic physical output is promised:

- file names should match
- record assignment should match
- file contents should match

### Manifest level

Expected deterministic metadata should match.

Generation timestamps and other intentionally nondeterministic metadata must be treated separately.

---

## 22. Scenario Matrix Testing

Scenarios should be tested as combinations of independent dimensions.

A useful matrix includes:

| Dimension | Examples |
|---|---|
| Scale | small, medium |
| Cardinality | low, medium, high |
| Data quality | clean, slightly_dirty, dirty |
| Skew | disabled, customer, product |
| Seed | fixed test seeds |
| Output files | 1, several, many |
| Date range | short, normal |
| Scenario | baseline, targeted performance scenario |

The complete Cartesian product should not necessarily run in every build.

Instead, maintain:

- a representative CI matrix
- targeted scenario tests
- periodic comprehensive runs

---

## 23. Regression Tests

Every discovered generator defect should ideally produce a regression test.

The workflow is:

```text
bug discovered
      ↓
minimal reproducing configuration
      ↓
regression test
      ↓
fix
      ↓
test becomes permanent
```

Regression tests should preserve the exact conditions required to reproduce important defects.

Examples:

- seed-specific relationship bug
- boundary date bug
- incorrect rounding
- incorrect cardinality
- CSV escaping bug
- incorrect file splitting
- skew unexpectedly affecting unrelated entities

---

## 24. Golden Dataset Tests

Small deterministic datasets can be maintained as golden fixtures for stable behaviors.

A golden fixture may contain:

- configuration
- seed
- generated output
- expected manifest
- expected validation result

Golden tests are useful when exact output is intentionally part of the contract.

However, they should not be overused for statistical behavior where implementation improvements may legitimately change the random sequence while preserving the required distributional properties.

---

## 25. Test Fixtures

Fixtures should be intentionally small and readable.

Examples:

```text
1 customer
2 customers
1 product
2 products
1 order
order with multiple items
order with payment
order with shipment
order with return
session with event sequence
```

Small fixtures make business-rule failures easy to diagnose.

Reusable fixture builders should be preferred over duplicated setup code.

---

## 26. Test Data Independence

Tests must avoid unintended dependence on:

- system clock
- host timezone
- locale
- operating-system path conventions
- default character encoding
- hash-map ordering
- filesystem ordering
- machine-specific randomness

Where time is required, tests should use a controlled clock/reference time.

Where randomness is required, tests should use explicit seeds.

---

## 27. Test Parallelism

Tests should be safe to run in parallel where practical.

Avoid shared mutable state such as:

```text
global random generator
shared output directory
shared mutable configuration
```

Each test should have isolated state and output locations.

This is especially important because the project is intended to scale into larger generation and performance test suites.

---

## 28. Performance Testing

Performance testing is separate from correctness testing.

The project should eventually measure:

- records generated per second
- records written per second
- total generation time
- CPU utilization
- memory usage
- output throughput
- validation overhead
- manifest/statistics overhead

Performance tests should use stable configurations and seeds.

The objective is to identify bottlenecks without allowing performance optimization to weaken correctness.

---

## 29. Memory and Streaming Tests

The generator is intended to support large datasets.

Tests should therefore verify that entity generation and output do not require loading the complete dataset into memory.

Where practical, tests should exercise:

```text
generate
  → transform
  → validate
  → write
```

as a streaming/chunked process.

Memory-sensitive tests can later enforce approximate upper bounds for representative large configurations.

---

## 30. End-to-End Tests

End-to-end tests should execute the complete pipeline:

```text
configuration
    ↓
reference data
    ↓
entity generation
    ↓
relationship generation
    ↓
data-quality injection
    ↓
skew/scenario logic
    ↓
validation
    ↓
CSV output
    ↓
manifest
```

A successful end-to-end test should prove that the generated artifact is usable as a complete dataset rather than merely proving that individual components work independently.

---

## 31. CI Test Strategy

A practical CI structure is:

### Fast suite

Runs on normal development changes:

- unit tests
- configuration tests
- distribution tests with small samples
- relationship tests
- validation tests
- CSV serialization tests
- reproducibility tests
- small end-to-end tests

### Integration suite

Runs in CI or a higher-cost pipeline:

- multi-entity generation
- scenario combinations
- file layout tests
- manifest validation
- larger samples
- round-trip tests

### Performance suite

Runs separately:

- large profile
- xlarge profile
- throughput benchmarks
- memory tests
- output performance
- validation performance

This keeps developer feedback fast while still maintaining confidence in the complete system.

---

## 32. Test Naming

Test names should describe the behavior being verified.

Prefer:

```text
shouldRejectNegativeCustomerCount
shouldPreserveForeignKeyIntegrity
shouldReproduceSameDatasetForSameSeed
shouldRespectConfiguredProductCardinality
shouldWriteConfiguredNumberOfPartFiles
shouldDetectInvalidOrderTotal
```

Avoid names that only describe implementation details.

The test suite should read as executable documentation of the generator contract.

---

## 33. Initial Test Coverage Priorities

The first implementation should prioritize tests in this order:

### Priority 1 — correctness

- configuration validation
- identifiers
- entity schemas
- foreign keys
- business invariants
- temporal rules

### Priority 2 — determinism

- explicit seed behavior
- same-seed reproducibility
- deterministic ordering
- deterministic file assignment

### Priority 3 — configurability

- scale
- cardinality
- distributions
- data-quality rates
- skew

### Priority 4 — output

- CSV correctness
- file layout
- manifest
- output validation

### Priority 5 — performance

- throughput
- memory behavior
- large-scale generation

---

## 34. What Should Not Be Tested as Exact Values

The generator uses stochastic processes, so tests should not incorrectly enforce exact random outcomes.

Avoid assertions such as:

```text
first generated customer must have value X
exactly 70% of 100 records must belong to category A
```

unless the behavior is explicitly deterministic by contract.

Prefer:

```text
distribution is within tolerance
invariant always holds
same seed produces equivalent output
configured cardinality is satisfied
configured violation rate is approximately satisfied
```

This distinction keeps tests robust while preserving correctness.

---

## 35. Testing Invariants Across Scenarios

A scenario may intentionally introduce data-quality violations or skew, but it should not accidentally break unrelated contracts.

For example:

```text
dirty-data scenario
    → expected email nulls
    → expected malformed values
    → expected configured violations

still required:
    → valid entity identity
    → valid unrelated relationships
    → valid output serialization
    → valid manifest
```

Scenario tests should explicitly document which invariants are intentionally relaxed.

---

## 36. Testing Principles

The test strategy follows these principles:

1. **Test contracts, not implementation accidents.**
2. **Test deterministic behavior with explicit seeds.**
3. **Use statistical tolerance for stochastic behavior.**
4. **Test relationships independently and end-to-end.**
5. **Test both valid and deliberately invalid datasets.**
6. **Keep small tests fast and diagnostic.**
7. **Use property-based testing for broad configuration spaces.**
8. **Treat output files and manifests as testable artifacts.**
9. **Preserve discovered defects as regression tests.**
10. **Separate correctness testing from performance benchmarking.**
11. **Keep tests independent of host environment.**
12. **Do not allow performance optimization to weaken data correctness.**

---

## 37. Initial Implementation Scope

The initial testing framework should establish:

- unit-test structure
- configuration tests
- seed/reproducibility tests
- distribution tests
- entity tests
- relationship tests
- business-rule tests
- temporal tests
- data-quality tests
- skew tests
- CSV writer tests
- file-layout tests
- manifest tests
- validator negative tests
- small end-to-end tests
- representative scenario matrix
- regression-test structure

Property-based, performance, and very large-scale tests can be expanded progressively as the generator implementation matures.

---

## 38. Guiding Principle

> **Every important generator contract should be executable as a test.**

The generator should not rely on visual inspection or manual confidence to establish correctness. A generated dataset is successful only when its configured contracts, relationships, invariants, reproducibility expectations, and physical output requirements can be demonstrated by automated tests.
