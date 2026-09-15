# 22 — Testing Architecture and Test Strategy

## 1. Purpose

This document defines the testing architecture for the ShopSphere synthetic e-commerce data generator.

The project is not a simple CRUD application. It is a probabilistic simulation system with:

- domain models;
- deterministic randomness;
- configurable distributions;
- behavioral relationships;
- reference data;
- temporal behavior;
- scenarios;
- controlled skew;
- controlled data-quality defects;
- statistics;
- validation;
- multiple output concerns.

Testing therefore cannot rely on only conventional example-based unit tests.

The test strategy must prove several different properties:

```text
Does the code behave correctly?
Does the generated data remain structurally valid?
Does the data represent the intended business behavior?
Does configuration produce the intended behavior?
Is generation reproducible?
Do scenarios produce measurable effects?
Does the output artifact remain correct?
Does the system scale?
```

The testing architecture should answer these questions at the appropriate level.

---

# 2. Core Testing Principle

The generator should be tested at multiple levels:

```text
Pure logic
    ↓
Domain behavior
    ↓
Entity generation
    ↓
Cross-domain relationships
    ↓
Scenario generation
    ↓
Full generation pipeline
    ↓
Output artifacts
    ↓
Statistical behavior
```

No single test level can provide sufficient confidence.

Unit tests are fast but cannot prove realistic end-to-end behavior.

End-to-end tests provide broad coverage but are too coarse to diagnose every defect.

Statistical tests verify distributions but cannot replace structural validation.

The test suite therefore needs deliberate layering.

---

# 3. Testing Pyramid

The preferred shape is:

```text
                 /\
                /  \
               / E2E\
              /------\
             / Scenario\
            /------------\
           / Integration  \
          /----------------\
         / Domain Behavior  \
        /--------------------\
       /     Unit Tests       \
      /________________________\
```

Most tests should remain lower-level and fast.

A smaller number of higher-level tests should validate system behavior.

---

# 4. Test Categories

The project should eventually contain:

1. Unit tests
2. Domain tests
3. Property-based tests
4. Statistical tests
5. Relationship tests
6. Configuration tests
7. Scenario tests
8. Reproducibility tests
9. Output/artifact tests
10. Integration tests
11. End-to-end tests
12. Performance tests
13. Regression tests

These categories have different purposes.

---

# 5. Unit Tests

Unit tests verify small pieces of deterministic logic.

Examples:

- ID generation;
- unit-number generation;
- weighted selection;
- probability calculations;
- date calculations;
- distribution implementations;
- configuration parsing helpers;
- CSV escaping;
- statistics calculations.

A unit test should ideally have:

```text
input
→ operation
→ expected result
```

without requiring the entire generator.

---

# 6. What Belongs in Unit Tests

Good candidates include:

```scala
WeightedCategoricalDistribution
ZipfDistribution
ParetoDistribution
RandomGenerator
UnitNumberGenerator
StatisticsCalculator
ScenarioResolver
ConfigurationValidator
```

provided these components exist as actual implementation units.

The test architecture must not require creating classes merely because this document names them as possible components.

---

# 7. Domain Tests

Domain tests verify business behavior.

Examples:

### Customer

```text
customer profile values remain within valid bounds
lifecycle state is valid
behavior profile is internally coherent
```

### Product

```text
product belongs to valid category
product belongs to valid brand
price remains valid
```

### Order

```text
order belongs to customer
status is valid
total matches order items
```

### Return

```text
returned quantity does not exceed purchased quantity
return is associated with a valid order item
```

Domain tests should use business terminology.

---

# 8. Generator Tests

Generator tests verify that a domain generator correctly converts context into domain records.

For example:

```text
AddressGenerator
CustomerGenerator
ProductGenerator
OrderGenerator
OrderItemGenerator
```

A generator test should verify:

- required inputs;
- output invariants;
- relationship references;
- deterministic behavior;
- error behavior.

---

# 9. Example Generator Test Shape

Conceptually:

```scala
val result =
  productGenerator.generate(
    context = context,
    random = random
  )

result.categoryId shouldBe expectedCategory
result.brandId shouldBe expectedBrand
result.price should be > 0.0
```

The exact APIs depend on the eventual implementation.

The important principle is that the test should express domain intent.

---

# 10. Relationship Tests

Relationships deserve explicit tests.

Examples:

```text
Order.customerId exists
OrderItem.orderId exists
OrderItem.productId exists
Payment.orderId exists
Shipment.orderId exists
Return.orderId exists
Session.customerId exists
Event.sessionId exists
```

These are not merely implementation details.

They are part of the data model contract.

---

# 11. Relationship Invariants

Useful invariants include:

```text
Every Order references one valid Customer.

Every OrderItem references one valid Order.

Every OrderItem references one valid Product.

Every Payment references one valid Order.

Every Shipment references one valid Order.

Every Session references one valid Customer.

Every Event references one valid Session.
```

The exact cardinality semantics should follow the relationship architecture.

---

# 12. Business Invariant Tests

Business invariants should be tested independently from foreign-key validity.

Examples:

```text
order total = sum of order item line totals
```

```text
returned quantity <= purchased quantity
```

```text
shipment cannot be delivered before order placement
```

```text
payment timestamp is compatible with order lifecycle
```

```text
event sequence remains valid within a session
```

These tests protect business meaning.

---

# 13. Property-Based Testing

Property-based testing is particularly useful for this project because many generator behaviors are expressed as invariants rather than exact values.

Instead of testing:

```text
seed 42 produces exactly value X
```

test:

```text
generated value is always within valid bounds
```

Example:

```text
quantity >= 1
price > 0
probability ∈ [0, 1]
```

The exact framework can be selected later if needed.

---

# 14. Randomness Tests

Randomness should be tested for both:

- deterministic behavior;
- statistical behavior.

Deterministic tests:

```text
same seed + same stream → same sequence
```

Isolation tests:

```text
changing product stream does not alter customer stream
```

Boundary tests:

```text
nextInt(min, max)
```

respects the documented interval.

---

# 15. Reproducibility Test

A fundamental test is:

```text
configuration A
seed 42
    ↓
generation A

configuration A
seed 42
    ↓
generation B
```

Then compare:

```text
A == B
```

The definition of equality must be explicit.

It may mean:

- identical in-memory records;
- identical serialized files;
- identical statistics;
- identical manifest fingerprint.

Different test levels can verify different forms.

---

# 16. Reproducibility Under Refactoring

A code refactor should not accidentally alter unrelated random streams.

For example:

```text
change event generation
```

should ideally not change:

```text
customer identity generation
```

when stream isolation is designed correctly.

Tests should protect this property where it matters.

---

# 17. Distribution Tests

Distribution implementations require specialized tests.

For a bounded distribution:

```text
all generated values are inside bounds
```

For a categorical distribution:

```text
only configured categories are emitted
```

For a weighted distribution:

```text
high-weight categories occur more frequently
```

For a long-tail distribution:

```text
higher-ranked keys have higher expected frequency
```

Exact sample frequencies should not normally be asserted for small sample sizes.

---

# 18. Statistical Testing

Statistical tests should validate distributions over sufficiently large samples.

For example:

```text
10,000+ generated values
```

may be appropriate for some distribution tests.

The sample size should be chosen based on the property being tested.

Statistical tests should use tolerances rather than exact expected counts.

---

# 19. Avoiding Flaky Statistical Tests

A poor statistical test looks like:

```text
product A must appear exactly 2,137 times
```

This is fragile.

A better test might assert:

```text
product A share is between 18% and 22%
```

for a sufficiently large sample and a known configuration.

Tolerance should be justified.

---

# 20. Distribution Regression Tests

Important business distributions should have regression expectations.

Examples:

```text
items per order
orders per customer
sessions per customer
AOV
return rate
product concentration
event-type distribution
```

These tests help detect realism regressions.

---

# 21. Current Baseline Regression

The current generator has useful baseline observations.

Examples:

```text
items/order ≈ 2.40
sessions/customer = 3.00
events/session = 8.00
addresses/customer ≈ 1.20
```

These should not necessarily become permanent target values.

They are current implementation characteristics.

When realism is improved, tests should evolve toward the intended business distributions.

---

# 22. Testing the Target, Not the Accident

A critical rule:

> Do not turn every current output characteristic into a permanent test expectation.

For example:

```text
3 sessions/customer
```

is currently true.

If the target design introduces heterogeneous sessions, a test requiring exactly three sessions would become actively harmful.

Tests should protect intended contracts, not accidental implementation behavior.

---

# 23. Configuration Tests

Configuration deserves its own test layer.

Test:

- required fields;
- defaults;
- valid enum values;
- invalid enum values;
- bounds;
- nested configuration;
- profile resolution;
- scenario resolution;
- precedence;
- incompatible combinations.

Example:

```text
skew.top-share = -0.1
```

should fail configuration validation.

---

# 24. Configuration Precedence Tests

Where multiple configuration layers exist, tests should verify deterministic precedence.

Conceptually:

```text
default
< profile
< scenario
< explicit override
```

The actual precedence must match the configuration architecture.

A test should make this behavior explicit.

---

# 25. Configuration Compatibility Tests

If scenarios can be composed:

```text
holiday_peak + hot_product
```

should resolve successfully if compatible.

If two scenarios conflict:

```text
zero_failures + payment_incident
```

the system should either:

- reject the combination;
- or apply a documented precedence rule.

Tests should protect this contract.

---

# 26. Scenario Tests

A scenario test verifies:

```text
scenario configuration
→ expected statistical/business effect
```

Examples:

### hot_product

Expected:

```text
top product share increases
```

### hot_customer

Expected:

```text
customer activity concentration increases
```

### holiday_peak

Expected:

```text
activity during selected period increases
```

### dirty_data

Expected:

```text
configured defect class appears
```

---

# 27. Scenario Non-Regression Tests

A scenario should not accidentally break unrelated behavior.

For example:

```text
hot_product
```

should not cause:

```text
invalid order totals
orphan order items
invalid customer references
```

unless explicitly designed to do so.

This is where scenario tests and relationship/business validation tests intersect.

---

# 28. Data-Quality Scenario Tests

Controlled corruption requires two separate tests:

1. corruption actually occurs;
2. corruption remains within the configured target.

Example:

```text
configured null rate = 1%
observed null rate = 0.94%
```

The test should accept the configured tolerance.

For deterministic corruption, stronger assertions may be possible.

---

# 29. Expected vs Unexpected Violations

A dirty dataset should produce:

```text
expected violations > 0
unexpected violations = 0
```

where appropriate.

This is a powerful integration test.

It proves that:

- the quality scenario works;
- the generator remains structurally coherent elsewhere.

---

# 30. Output Tests

Output tests should verify:

- expected files exist;
- expected headers exist;
- row counts are correct;
- records serialize correctly;
- special characters are escaped;
- nulls are represented consistently;
- numeric values are formatted consistently.

These tests should operate on temporary directories.

---

# 31. Output Golden Tests

A golden test compares generated output against a stored expected artifact.

This can be useful for:

- tiny deterministic fixtures;
- schema output;
- manifest structure.

However, full large datasets should not generally be committed as golden test fixtures.

They are too large and too brittle.

---

# 32. Manifest Tests

Manifest tests should verify:

```text
dataset identity
run identity
seed
generator version
configuration metadata
entity counts
validation summary
statistics summary
output metadata
```

They should also verify that optional fields are handled correctly.

---

# 33. Manifest Reproducibility

For the same generation input:

```text
manifest metadata should be stable
```

Run-specific timestamps may legitimately differ.

Therefore the test should distinguish:

```text
deterministic metadata
```

from:

```text
execution metadata
```

This is important.

---

# 34. Artifact Integrity Tests

A generated run should eventually be checked for:

```text
all expected files exist
files are readable
headers match schema
actual counts match metadata
checksums match
```

This tests the final artifact rather than only the in-memory dataset.

---

# 35. Integration Tests

Integration tests should exercise multiple components together.

Examples:

```text
ConfigLoader
→ GenerationPlanBuilder
→ GenerationPipeline
→ Validator
→ Statistics
→ CsvOutputWriter
```

These tests are slower than unit tests but catch integration defects.

---

# 36. End-to-End Test

At least one end-to-end test should generate a complete small dataset.

Example:

```text
small profile
fixed seed
temporary output directory
```

Then verify:

```text
all entity files
all relationships
validation
statistics
manifest
```

The dataset should be small enough for CI.

---

# 37. E2E Determinism

Run the full pipeline twice:

```text
run A
run B
```

with:

```text
same configuration
same seed
same reference data
```

Then compare:

```text
records
statistics
manifest deterministic fields
output files
```

This is one of the highest-value tests in the project.

---

# 38. Cross-Entity Scenario E2E Tests

Some scenarios require full-pipeline validation.

Example:

```text
hot_customer
```

should affect:

```text
sessions
events
orders
order_items
payments
returns
```

where the model defines those relationships.

A domain unit test cannot prove this propagation.

---

# 39. Test Fixtures

Fixtures should be deliberately small.

Useful fixture sets:

```text
tiny geography
tiny category set
tiny brand set
tiny product catalog
tiny customer population
```

These enable fast tests.

Fixtures should represent meaningful business cases rather than arbitrary random records.

---

# 40. Reference Data Fixtures

Geography and catalog reference data should have deterministic fixture versions.

Tests should not depend on external network calls.

For example:

```text
test geography
test categories
test brands
```

should live within the project's test resources or another deterministic test source.

---

# 41. Test Data Builders

Test builders can be useful for complex domain objects.

Example:

```scala
OrderBuilder()
  .withCustomer("CUSTOMER_001")
  .withStatus(Delivered)
  .withTotal(...)
  .build()
```

However, builders should only be introduced when constructing test objects becomes genuinely cumbersome.

Scala case classes often make simple fixtures easier:

```scala
Order(
  id = "...",
  customerId = "...",
  ...
)
```

Avoid builder proliferation.

---

# 42. Test Doubles

Dependency injection should make behavior components replaceable.

Useful test doubles include:

- deterministic random generator;
- fixed clock;
- fixed distribution;
- stub reference-data provider;
- fake output writer;
- recording metrics sink.

A test double should exist to isolate a meaningful boundary.

Do not mock every class.

---

# 43. Mocking Philosophy

Over-mocking is dangerous in a generator because behavior emerges from composition.

Prefer:

```text
real distribution
real domain logic
small deterministic fixture
```

over:

```text
mock every dependency
```

Mocks are most useful at infrastructure boundaries.

Examples:

```text
filesystem
metrics backend
external reference provider
```

if those dependencies exist.

---

# 44. Testing Time

Temporal generation requires a controllable clock or deterministic time source.

Tests should be able to establish:

```text
generation start
campaign window
order date
session time
event timestamps
```

without depending on the actual system clock.

This makes temporal tests reproducible.

---

# 45. Lifecycle Tests

Entity state machines should have explicit transition tests.

Example:

```text
PLACED → CONFIRMED → SHIPPED → DELIVERED
```

Invalid transitions should be rejected where the domain requires it.

Cancellation paths should be tested separately.

Payment, shipment, and return lifecycles should similarly have focused tests.

---

# 46. Event Funnel Tests

Event generation should eventually test valid journey sequences.

Examples:

```text
landing
→ product_view
→ add_to_cart
→ checkout
→ payment_attempt
→ purchase
```

Abandonment should also be valid:

```text
landing
→ product_view
→ add_to_cart
→ exit
```

The exact taxonomy belongs to the Event domain.

---

# 47. Customer Behavior Tests

When CustomerBehaviorProfile is introduced, tests should verify that behavior dimensions are:

- bounded;
- internally coherent;
- reproducible;
- propagated downstream.

Examples:

```text
high purchase frequency
→ higher expected order count
```

```text
high activity
→ higher expected session count
```

The tests should validate statistical tendencies rather than deterministic one-to-one outcomes where the model is probabilistic.

---

# 48. Correlation Tests

Sophisticated realism requires correlation testing.

Examples:

```text
activity propensity ↔ session count
purchase propensity ↔ order count
spending propensity ↔ customer spend
product popularity ↔ order-item frequency
return propensity ↔ return rate
```

These are not simple equality checks.

They should use appropriate statistical measures.

The exact statistical method can evolve.

---

# 49. Correlation Test Caution

Correlation tests can become unstable with small datasets.

Therefore:

```text
small unit fixture
```

should not be expected to demonstrate a strong population-level correlation.

Correlation tests belong primarily in larger statistical/integration tests.

---

# 50. Skew Tests

Skew scenarios should test metrics such as:

```text
top-1 share
top-10 share
Gini
max/median
p99 frequency
```

Example:

```text
baseline Gini < hot-product Gini
```

or:

```text
hot product share >= configured threshold
```

This is more robust than asserting exact hot-key counts.

---

# 51. Statistical Acceptance Windows

Every statistical scenario should define an acceptance window.

Example:

```text
target = 20%
tolerance = ±2 percentage points
```

The test should fail when:

```text
observed = 14%
```

but pass when:

```text
observed = 19.2%
```

if the contract permits that range.

---

# 52. Test Random Seeds

Tests should use explicit seeds.

Avoid:

```scala
new Random()
```

without a controlled seed in tests.

Use:

```text
seed = 1
seed = 42
seed = 12345
```

for different deterministic test cases.

---

# 53. Multiple-Seed Testing

A generator can accidentally work for one seed and fail for another.

Important property tests should therefore use multiple seeds.

Example:

```text
1
42
123
999
Long.MaxValue
```

where valid.

The test should verify invariants across all seeds.

---

# 54. Boundary Seeds

Boundary and unusual seeds should be tested where the random infrastructure permits them.

Examples:

```text
0
1
Long.MaxValue
Long.MinValue
```

This can expose seed-derivation bugs.

---

# 55. Empty and Minimal Inputs

Generators should test minimal valid reference data.

Examples:

```text
one category
one brand
one product
one building
one customer
```

This can expose assumptions such as:

```text
at least two choices exist
```

that are not part of the actual contract.

---

# 56. Invalid Inputs

Invalid configuration and reference data should be tested.

Examples:

```text
no buildings
missing postal code
negative weight
weights do not sum to valid range
duplicate IDs
invalid hierarchy
```

The system should fail with meaningful errors.

---

# 57. Error Message Tests

Do not test every character of an error message.

Test the important diagnostic information.

For example:

```text
error mentions product ID
error mentions missing category
```

This keeps tests resilient while preserving useful diagnostics.

---

# 58. Performance Tests

Performance tests are separate from correctness tests.

Useful metrics include:

```text
records/sec
generation time
memory usage
output throughput
validation time
statistics time
```

The project should eventually maintain representative performance baselines.

---

# 59. Performance Dataset Sizes

A useful progression is:

```text
tiny
small
medium
large
xlarge
```

Tests should not run the xlarge profile on every CI build.

Instead:

```text
unit/integration → CI
small E2E        → CI
medium benchmark → scheduled
large benchmark  → performance environment
xlarge benchmark → dedicated performance environment
```

---

# 60. Performance Regression

Performance regression tests should compare relative changes rather than rigid absolute times when environments vary.

For example:

```text
new runtime <= baseline runtime × 1.20
```

may be more robust than:

```text
runtime < 10 seconds
```

unless the environment is tightly controlled.

---

# 61. Memory Regression

Memory should also be tracked for large generation.

Important questions:

```text
Does memory scale approximately with reference data?
Does generated row count cause unbounded retention?
Does statistics collection materialize too much?
Does output buffering grow unexpectedly?
```

These are architecture-level performance tests.

---

# 62. Property-Based Relationship Testing

A valuable property is:

```text
for every generated child,
its required parent exists
```

Examples:

```text
∀ orderItem:
  orderItem.orderId ∈ orders

∀ event:
  event.sessionId ∈ sessions
```

This is stronger than checking only a few sample records.

---

# 63. Referential Integrity at Scale

For larger datasets, relationship validation tests should avoid inefficient nested scans.

Testing itself should use appropriate indexes or sets.

For example:

```scala
val orderIds = orders.iterator.map(_.id).toSet
```

then:

```scala
orderItems.forall(item => orderIds.contains(item.orderId))
```

The exact implementation depends on the validation architecture.

---

# 64. Testing Statistics

Statistics functions should have exact tests where the mathematics is deterministic.

Example:

```text
values = [1, 2, 3, 4]
mean = 2.5
```

For approximate percentile algorithms, tests should verify documented tolerance.

Mathematical utilities should be tested independently from generation.

---

# 65. Testing Gini and Skew Metrics

Use small hand-computable fixtures.

For example:

```text
[1, 1, 1, 1]
```

should represent no concentration.

A highly concentrated fixture:

```text
[100, 1, 1, 1]
```

should produce substantially greater inequality.

This helps verify the metric implementation before using it in large scenario tests.

---

# 66. Golden Statistics

For fixed small fixtures, statistics can have exact expected values.

For generated populations, prefer tolerance-based regression ranges.

This distinction prevents statistical tests from becoming flaky.

---

# 67. Test Naming

Test names should communicate business intent.

Prefer:

```text
should preserve order total when item prices change
```

over:

```text
testOrder7
```

Prefer:

```text
should generate postal code belonging to selected building area
```

over:

```text
addressTest2
```

Readable test names are part of the project's documentation.

---

# 68. Test Organization

The test package should eventually mirror domain ownership.

Conceptually:

```text
src/test/scala/com/shopsphere/datagenerator/
├── customer/
├── address/
├── geography/
├── category/
├── brand/
├── product/
├── order/
├── orderitem/
├── payment/
├── shipment/
├── return/
├── session/
├── event/
├── config/
├── distribution/
├── scenario/
├── validation/
├── output/
├── manifest/
├── statistics/
└── integration/
```

Do not create empty packages just for symmetry.

---

# 69. Domain Test Ownership

Customer tests should primarily live with Customer.

Product tests should primarily live with Product.

Relationship tests involving multiple domains can live in:

```text
relationship
```

or:

```text
integration
```

depending on their scope.

This mirrors the architecture.

---

# 70. Test Helpers

Shared test utilities should remain small and explicit.

Good examples:

```text
TestRandom
TestClock
TestReferenceData
TestOutputDirectory
```

Avoid a giant:

```text
TestUtils
```

containing unrelated behavior.

---

# 71. Test Isolation

Tests should not depend on:

- execution order;
- mutable global state;
- previous test output;
- current date;
- current working directory where avoidable.

Each test should establish its own required state.

---

# 72. Temporary Output

Output tests should use isolated temporary directories.

Do not write test output into:

```text
src/main/resources
```

or the normal dataset directory.

This prevents tests from contaminating development artifacts.

---

# 73. Parallel Test Execution

Tests should be safe to run in parallel where possible.

This requires:

- isolated temporary paths;
- no shared mutable random generators;
- no global configuration mutation;
- deterministic fixtures.

Parallel-safe tests improve CI speed.

---

# 74. Current ScalaTest Direction

The project already uses ScalaTest 3.2.19.

Existing tests should continue using the chosen ScalaTest style consistently.

The test framework should remain compatible with:

```text
Scala 2.13.18
SBT 1.13.0
```

Scala 3 should not be introduced.

---

# 75. CI Test Layers

A future CI pipeline can use stages:

```text
Stage 1
compile

Stage 2
unit tests

Stage 3
domain/integration tests

Stage 4
small E2E

Stage 5
statistical regression

Stage 6
performance tests
```

Performance tests may run separately from normal pull-request validation.

---

# 76. Fast Feedback

The normal developer loop should remain fast.

Typical command:

```powershell
sbt test
```

should run the normal correctness suite.

Heavy benchmarks should not be hidden inside ordinary unit tests.

---

# 77. Test Tags

If the suite grows substantially, tests may be categorized.

Possible categories:

```text
Unit
Integration
EndToEnd
Statistical
Performance
Slow
```

The exact tagging mechanism can be introduced only when needed.

---

# 78. Test Data Volume

The test suite should use the smallest dataset capable of proving the property.

Examples:

```text
5 customers
```

may be enough for a relationship test.

```text
100,000 products
```

may be required for a meaningful popularity distribution test.

Do not use large data by default.

---

# 79. Statistical Test Isolation

Statistical tests should have explicit:

```text
sample size
seed
distribution parameters
acceptance tolerance
```

This makes failures diagnosable.

Example:

```text
ProductPopularityZipfTest
sampleSize = 100000
seed = 42
exponent = 1.2
topShareTolerance = ...
```

---

# 80. Scenario Matrix Tests

A compact matrix can validate combinations.

Example:

| Scenario | Structural | Relationships | Statistics | Quality |
|---|---:|---:|---:|---:|
| baseline | ✓ | ✓ | ✓ | ✓ |
| hot_product | ✓ | ✓ | ✓ | ✓ |
| hot_customer | ✓ | ✓ | ✓ | ✓ |
| holiday_peak | ✓ | ✓ | ✓ | ✓ |
| dirty_data | ✓* | ✓* | ✓ | ✓✓ |

`*` depends on which violations the scenario intentionally introduces.

---

# 81. Contract Tests

Each major abstraction should have a contract.

For example, a `ProductPopularityModel` contract might require:

```text
returns only valid product IDs
probabilities are non-negative
distribution is deterministic under controlled random input
```

Every implementation should satisfy the same contract.

This is especially useful when Strategy implementations multiply.

---

# 82. Testing Factories

Factories should test:

```text
valid config → correct implementation
invalid config → meaningful failure
unsupported implementation → meaningful failure
```

Do not test private construction details unnecessarily.

---

# 83. Testing Composition

Composition should be tested for predictable interactions.

Example:

```text
base demand
+
seasonality
```

should produce:

```text
seasonal demand multiplier
```

without losing:

```text
product identity
```

More complex composition should be tested incrementally.

---

# 84. Testing Dependency Injection

DI itself does not need extensive tests.

Instead, test that:

```text
generator accepts supplied behavior
```

and:

```text
test can replace behavior with deterministic implementation
```

This proves the architectural boundary is useful.

---

# 85. Mutation Testing

Mutation testing may eventually be useful for core business rules.

It can reveal whether tests detect changes such as:

```text
>= becomes >
order total calculation changes
FK condition removed
```

This should be considered after the core test suite is mature.

It is not an immediate requirement.

---

# 86. Fuzz Testing

Fuzzing may be useful for:

- configuration;
- CSV serialization;
- malformed reference data;
- distribution parameters.

Potential failures include:

```text
unexpected exception
infinite loop
stack overflow
invalid output
```

This is a future hardening layer.

---

# 87. Security Testing

This project is not primarily a security-sensitive application, but output handling should still avoid:

- path traversal through configuration;
- uncontrolled file paths;
- accidental credential output;
- unsafe deserialization.

Security testing should remain proportionate to the actual threat model.

---

# 88. Test Observability

When a statistical or E2E test fails, the failure should expose useful diagnostics.

For example:

```text
Expected top-product share >= 20%
Observed: 12.7%

Seed: 42
Scenario: hot_product
Sample size: 100000
```

This is much more useful than:

```text
Assertion failed
```

---

# 89. Failure Artifacts

For difficult integration/statistical failures, it can be useful to retain:

```text
manifest
statistics
configuration
seed
small sample output
```

This should be optional so ordinary tests do not leave large artifacts.

---

# 90. Testing Current Realism Gaps

The test suite should explicitly recognize the current realism limitations.

Examples:

```text
sessions/customer currently deterministic
events/session currently deterministic
items/order currently narrow
AOV currently too high for target baseline
```

These should be represented as:

```text
known realism gaps
```

rather than incorrectly encoded as desired long-term behavior.

---

# 91. Realism Evolution

When a realism feature is implemented:

```text
old implementation characteristic
```

should be replaced by:

```text
new business contract
```

Example:

Current:

```text
exactly 3 sessions/customer
```

Future:

```text
sessions/customer follows configured heterogeneous distribution
```

The test should evolve accordingly.

---

# 92. Testing Documentation Contracts

The architecture documents are intended to guide implementation.

When a design decision becomes an actual contract, corresponding tests should be added.

For example:

```text
Address has five fields
```

if this remains the domain contract.

The test suite should prevent accidental reintroduction of obsolete address fields or behavior.

---

# 93. Migration Testing

During architectural migration, tests are safety rails.

Recommended loop:

```text
move one file
    ↓
compile
    ↓
run targeted tests
    ↓
run full tests
    ↓
inspect behavior
```

Do not perform a large package migration and only test at the end.

This is especially important because the user has intentionally chosen an incremental one-file-at-a-time migration approach.

---

# 94. Refactoring Safety

A refactor should ideally change:

```text
structure
```

without changing:

```text
behavior
```

unless behavior change is intentional.

Tests should therefore be run before and after significant structural changes.

---

# 95. Architecture Tests

Some architectural rules can eventually be tested mechanically.

Examples:

```text
domain package does not depend on output package
domain model does not depend on CSV writer
Spark is not a dependency of generation domain
```

The exact mechanism can be introduced later.

The first defense should remain clear package ownership and code review.

---

# 96. Dependency Direction Test

The intended dependency direction is approximately:

```text
domain
  ↑
behavior
  ↑
application orchestration
  ↓
infrastructure
```

More precisely, domain code should not depend upward on infrastructure concerns.

Tests can detect accidental imports when the project becomes large enough to justify architecture checks.

---

# 97. Test Coverage

Line coverage is useful but insufficient.

A project can have:

```text
90% line coverage
```

while having poor statistical and business coverage.

Important coverage dimensions are:

```text
domain behavior
relationship invariants
configuration paths
scenario combinations
distribution behavior
reproducibility
output artifacts
```

Coverage should therefore be considered multidimensional.

---

# 98. Definition of Test Completeness

A domain is not adequately tested merely because its generator has unit tests.

A mature domain should have:

```text
model tests
generator tests
business invariant tests
relationship tests
distribution/behavior tests
reproducibility tests
integration coverage
```

as appropriate to its complexity.

---

# 99. Testing the Generation Plan

The generation plan should have focused tests for:

- profile selection;
- cardinality selection;
- derived counts;
- scenario effects where they affect counts;
- impossible plans;
- consistency between parent and child counts.

Example:

```text
orders > 0
order_items target >= orders where required
sessions >= customers where profile requires activity
```

Exact relationships depend on the configuration contract.

---

# 100. Testing Cardinality Scaling

Profiles should be tested across:

```text
small
medium
large
xlarge
```

without requiring full generation for every test.

The plan builder can often be tested much more cheaply.

Then a smaller number of tests should verify actual generation.

---

# 101. Testing Cardinality Relationships

Important properties include:

```text
address count >= customer count
```

if every customer requires at least one address.

```text
payment count compatible with order count
```

```text
shipment count compatible with order lifecycle
```

The exact assertions must follow domain semantics.

---

# 102. Testing Reference Data

Reference data tests should verify:

- IDs are unique;
- required hierarchy exists;
- relationships are valid;
- lookup indexes are consistent;
- deterministic ordering is preserved;
- required postal-code relationships exist.

These tests protect the foundation on which generation depends.

---

# 103. Testing Geography

Geography tests should verify:

```text
Country → State → City → Area → Road → Society → Building
```

relationships.

For Address:

```text
building
→ area
→ postal code
```

must remain resolvable.

The test should reflect the current five-field Address model and must not introduce duplicate city/state/country attributes into Address.

---

# 104. Testing Product Relationships

Product tests should verify:

```text
product → category
product → brand
```

and any future:

```text
category → brand affinity
product popularity
pricing
availability
```

behavior.

---

# 105. Testing Order and OrderItem

These domains require particularly strong integration testing.

Core properties:

```text
Order
  ↓
OrderItems
  ↓
Products
```

and:

```text
order.total =
  sum(orderItem.lineTotal)
```

where the model defines this invariant.

---

# 106. Testing Payment

Payment tests should eventually cover:

```text
successful payment
failed payment
retry
method switching
refund relationship
```

The exact lifecycle should follow the Payment domain contract.

---

# 107. Testing Shipment

Shipment tests should cover:

```text
order relationship
address/geography compatibility
lifecycle ordering
delivery timing
failure/retry
```

where implemented.

---

# 108. Testing Return

Return tests should cover:

```text
eligibility
returned quantity
order-item relationship
reason
refund relationship
customer/product propensity
```

as realism matures.

---

# 109. Testing Session

Session tests should eventually cover:

```text
customer relationship
activity frequency
device/channel
duration
start/end time
conversion
```

Current deterministic counts should not become permanent target contracts.

---

# 110. Testing Event

Event tests should eventually cover:

```text
session relationship
sequence
event taxonomy
funnel transitions
timestamps
product context
conversion
abandonment
```

This is likely to become one of the richer test domains.

---

# 111. Testing End-to-End Business Coherence

A high-value test should eventually trace a complete customer journey:

```text
Customer
   ↓
Session
   ↓
Event: PRODUCT_VIEW
   ↓
Event: ADD_TO_CART
   ↓
Order
   ↓
OrderItem
   ↓
Payment
   ↓
Shipment
   ↓
Return
```

Not every generated session must follow this path.

The test only needs to verify that when the model generates such a journey, the relationships and lifecycle semantics remain coherent.

---

# 112. Testing Scenario Propagation

Scenario tests should verify propagation rather than isolated parameter changes.

Example:

```text
holiday_peak
```

may increase:

```text
sessions
events
orders
product demand
```

The test should measure the intended changes at the population level.

---

# 113. Test Design for Probabilistic Systems

The central testing mindset is:

```text
deterministic infrastructure
+
probabilistic business behavior
```

Deterministic infrastructure should have exact tests.

Probabilistic behavior should have:

- statistical expectations;
- tolerances;
- sufficient samples;
- fixed seeds;
- repeatable experiments.

This distinction prevents both flaky tests and under-tested behavior.

---

# 114. Test Runtime Budget

A practical target is:

```text
unit tests            → seconds
normal test suite     → seconds/tens of seconds
small E2E             → tens of seconds
statistical suite     → controlled
performance suite     → separate
```

The exact timings depend on the environment.

The important principle is that developers should be able to run the normal suite frequently.

---

# 115. Test Execution Commands

The standard baseline remains:

```powershell
sbt test
```

Targeted tests can be run through the normal ScalaTest/SBT mechanisms as needed.

A future project may add convenient commands or aliases for:

```text
unit
integration
e2e
statistics
performance
```

only when the suite becomes large enough to justify them.

---

# 116. Test Reporting

A failed test should make clear:

```text
what failed
why it failed
seed
scenario
configuration
observed value
expected range
```

This is particularly important for statistical tests.

---

# 117. Regression Baseline Files

As realism matures, the project may maintain small benchmark/reference summaries.

Example:

```text
baseline statistics:
  AOV range
  return rate range
  product Gini range
  session count distribution
```

These should be versioned alongside the generator.

They are part of the realism contract.

---

# 118. Avoiding Over-Testing Implementation Details

Tests should not care whether the generator internally uses:

```text
class A
```

or:

```text
class B
```

unless that is an architectural contract.

Prefer testing:

```text
business behavior
public contracts
observable output
```

This makes refactoring safer.

---

# 119. Architecture Quality Gate

Before accepting a new domain implementation, verify:

```text
□ model tests
□ generator tests
□ relationship tests
□ business invariant tests
□ deterministic tests
□ distribution tests where applicable
□ scenario tests where applicable
□ integration coverage
□ output coverage where applicable
```

Not every domain needs every category.

The checklist should be applied according to actual complexity.

---

# 120. Definition of Done

The testing architecture is mature when:

### Correctness

- domain invariants are covered;
- relationships are covered;
- configuration validation is covered;
- output contracts are covered.

### Realism

- important distributions have statistical tests;
- behavioral correlations have appropriate tests;
- scenario effects are measurable;
- realism regressions can be detected.

### Reproducibility

- same seed produces same results;
- random streams are isolated;
- manifests contain sufficient provenance.

### Quality

- dirty scenarios are tested;
- expected and unexpected violations are distinguishable.

### Performance

- representative benchmarks exist;
- generator regressions can be detected;
- large-scale tests are separated from normal CI.

### Maintainability

- tests follow domain ownership;
- shared helpers remain small;
- tests protect contracts rather than implementation accidents.

---

# 121. Recommended Implementation Sequence

Testing architecture should evolve alongside implementation.

Recommended sequence:

```text
1. preserve existing passing tests
        ↓
2. migrate tests with domain code
        ↓
3. add contract tests for common infrastructure
        ↓
4. strengthen relationship tests
        ↓
5. introduce reproducibility tests
        ↓
6. introduce statistical tests as realism improves
        ↓
7. add scenario tests
        ↓
8. add output/manifest tests
        ↓
9. add full-pipeline E2E tests
        ↓
10. establish performance baselines
        ↓
11. add realism regression suite
```

Do not build the complete testing framework before the corresponding production architecture exists.

---

# 122. Migration Rule

The project is intentionally being migrated one file at a time.

The safe migration loop is:

```text
one production file
        ↓
one or more corresponding test files
        ↓
compile
        ↓
targeted tests
        ↓
full sbt test
        ↓
review behavior
```

A passing test suite is a migration safety mechanism.

---

# 123. Current Test Baseline

The project currently has a strong foundation of automated tests.

The latest known baseline is:

```text
229 tests passed
```

This should be treated as the current safety baseline.

Future architectural changes should preserve or intentionally replace this coverage.

---

# 124. What the Current Test Suite Proves

The current tests provide meaningful confidence around:

- configuration;
- domain models;
- generators;
- relationships;
- validation;
- output;
- deterministic behavior;
- generation planning.

However, they do not yet prove the full target realism architecture.

In particular, the following require stronger future coverage:

```text
heterogeneous customer behavior
long-tail product popularity
customer/product/category affinity
variable sessions
variable events
realistic spending
temporal behavior
controlled skew
controlled data-quality scenarios
statistical calibration
```

---

# 125. Testing Strategy for the Realism Roadmap

As each realism feature is implemented, tests should be introduced at the same time.

Example:

```text
CustomerBehaviorProfile
    ↓
unit tests
    ↓
customer behavior tests
    ↓
correlation tests
    ↓
order/session integration tests
```

Do not implement realism first and postpone its tests.

That makes statistical regressions difficult to diagnose.

---

# 126. Explicit Architecture Decisions

### Decision 1

Testing is layered; no single test category is sufficient.

### Decision 2

Tests protect intended contracts, not accidental current output characteristics.

### Decision 3

Deterministic logic should use exact assertions.

### Decision 4

Probabilistic behavior should use statistical expectations and tolerances.

### Decision 5

Same seed plus same generation inputs must be reproducible.

### Decision 6

Random streams should be isolated enough to prevent unrelated changes from causing unnecessary output changes.

### Decision 7

Relationships and business invariants are first-class test concerns.

### Decision 8

Dirty-data scenarios must distinguish intentional violations from unexpected defects.

### Decision 9

Scenario tests must verify both intended effects and preservation of unrelated invariants.

### Decision 10

Output and manifest are tested as artifacts, not merely as side effects.

### Decision 11

Performance testing is separate from normal correctness testing.

### Decision 12

Statistical tests must expose seed, sample size, configuration, and tolerance when failures occur.

### Decision 13

Tests should follow domain ownership and avoid a giant shared test utility layer.

### Decision 14

The current 229-test suite is a migration safety baseline, not the final realism test suite.

### Decision 15

Testing complexity should grow with actual domain complexity rather than being introduced as infrastructure for its own sake.

---

# 127. Final Design Summary

The ShopSphere testing architecture is:

```text
                    Test Strategy
                         |
        +----------------+----------------+
        |                |                |
        v                v                v
   Deterministic     Business         Statistical
      Tests          Tests             Tests
        |                |                |
        +--------+-------+-------+--------+
                 |               |
                 v               v
             Scenario        Relationship
               Tests            Tests
                 |               |
                 +-------+-------+
                         |
                         v
                  Integration Tests
                         |
                         v
                    E2E Tests
                         |
              +----------+----------+
              |                     |
              v                     v
        Artifact Tests        Performance Tests
```

The essential philosophy is:

```text
Unit tests prove local logic.

Domain tests prove business rules.

Relationship tests prove the data graph.

Property tests prove invariants across many inputs.

Statistical tests prove distributions and realism.

Scenario tests prove intentional dataset shapes.

Reproducibility tests prove deterministic generation.

Output tests prove artifact correctness.

E2E tests prove the complete pipeline.

Performance tests prove scalability characteristics.
```

The test suite should therefore evolve with the generator.

The current objective is not to create thousands of tests. It is to create the **right tests at the right architectural boundary**.

For ShopSphere, the highest-value long-term test is not:

```text
"did this method return this object?"
```

It is:

```text
"does this configuration and seed produce a
structurally valid, behaviorally coherent,
statistically plausible, reproducible dataset
with the intended scenario characteristics?"
```

That is the testing contract the mature generator must ultimately satisfy.
