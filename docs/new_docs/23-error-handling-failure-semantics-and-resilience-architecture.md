# 23 — Error Handling, Failure Semantics, and Resilience Architecture

## 1. Purpose

This document defines how ShopSphere should handle errors, invalid configuration, invalid reference data, generation failures, validation failures, output failures, and other exceptional conditions.

A synthetic-data generator has an unusual correctness requirement:

> It must fail clearly when it cannot produce a dataset that satisfies its contract.

Silently producing plausible-looking but incorrect data is more dangerous than failing.

At the same time, not every abnormal condition should terminate a generation run.

Examples:

```text
invalid configuration
        → fail immediately

missing required geography
        → fail before generation

expected dirty-data violation
        → continue

optional statistics failure
        → potentially warn/continue

output write failure
        → fail the run
```

The architecture therefore needs explicit failure semantics rather than a generic "catch exception and log it" approach.

---

# 2. Core Principle

Errors should be classified by **business impact and recoverability**.

The system should distinguish:

```text
Invalid input
Configuration error
Reference-data error
Generation error
Relationship error
Validation failure
Scenario-contract failure
Statistics/observability issue
Output/artifact failure
Unexpected programming error
```

Each category should have defined handling semantics.

---

# 3. Failure Is Part of the Contract

A generator's contract includes not only successful output.

It also includes:

```text
what happens when inputs are invalid
what happens when reference data is incomplete
what happens when a scenario cannot be resolved
what happens when output fails
what happens when validation detects corruption
```

A predictable failure is a feature.

---

# 4. Failure Lifecycle

The preferred lifecycle is:

```text
Input
  ↓
Configuration validation
  ↓
Reference-data validation
  ↓
Plan validation
  ↓
Generation
  ↓
Relationship/business validation
  ↓
Statistics/scenario validation
  ↓
Output
  ↓
Artifact validation
  ↓
Manifest
  ↓
Publish
```

Failures should be detected as early as practical.

---

# 5. Fail Fast

Configuration and foundational reference-data errors should normally fail before expensive generation begins.

Example:

```text
productPopularity.exponent = -5
```

If this is invalid, the system should not:

```text
generate 10 million customers
generate 50 million events
then discover the invalid parameter
```

Instead:

```text
load configuration
→ validate
→ fail
```

This saves time and avoids misleading partial output.

---

# 6. Fail Early vs Fail Late

Not every failure can be detected early.

## Early failures

Examples:

- invalid configuration;
- unsupported scenario;
- invalid distribution parameters;
- missing required reference data;
- impossible cardinality plan.

## Generation-time failures

Examples:

- unexpected missing lookup;
- invalid generated relationship;
- impossible domain state.

## Post-generation failures

Examples:

- statistical target missed;
- output file incomplete;
- checksum mismatch.

The architecture should detect each at the earliest meaningful boundary.

---

# 7. Error Taxonomy

A useful conceptual taxonomy is:

```text
ConfigurationError
ReferenceDataError
PlanningError
GenerationError
RelationshipError
ValidationError
ScenarioError
StatisticsError
OutputError
ArtifactError
UnexpectedError
```

These names are architectural concepts.

The implementation should not create a separate exception class for every minor condition unless that improves actual handling.

---

# 8. Configuration Errors

Examples:

```text
negative record count
invalid enum value
invalid probability
unsupported distribution
invalid profile
incompatible scenario
missing required property
```

Configuration errors should normally be:

```text
fatal
fail-fast
diagnostic
```

The error should identify the relevant configuration path.

Example:

```text
Invalid configuration:
scenario.skew.product.top-share must be between 0 and 1.
Observed: 1.7
```

---

# 9. Reference-Data Errors

Reference data is foundational.

Examples:

```text
duplicate category ID
building references missing society
area has no postal code where one is required
product references missing brand
```

These should normally prevent generation.

Generating a dataset from invalid foundational reference data makes downstream failures difficult to diagnose.

---

# 10. Reference Data vs Optional Reference Data

Some reference data may eventually be optional.

The configuration should explicitly determine whether absence is:

```text
fatal
```

or:

```text
allowed
```

Do not silently downgrade a required reference-data error to a warning.

---

# 11. Generation Errors

A generation error means the generator could not produce a valid domain record under its contract.

Examples:

```text
no valid product available
unable to select building
invalid generated unit
invalid lifecycle state
```

These are generally fatal unless the domain contract explicitly defines a recovery path.

---

# 12. Relationship Errors

Relationship errors include:

```text
missing parent
invalid foreign key
incompatible parent-child relationship
duplicate ownership
```

For clean generation these should normally fail the run.

For a dirty-data scenario, intentionally introduced relationship violations may be allowed, but they must be explicitly classified as expected violations.

---

# 13. Validation Failure

A validation failure is different from an execution exception.

For example:

```text
generation completed
validation found 7 orphan order items
```

The generator did execute successfully.

The resulting dataset is invalid according to the clean-data contract.

The run should therefore normally be:

```text
Generation: SUCCESS
Validation: FAIL
Dataset publication: BLOCKED
```

This distinction is valuable for diagnostics.

---

# 14. Expected Dirty Violations

A dirty-data scenario intentionally creates invalid records.

For example:

```text
invalid postal code
rate = 1%
```

The validator should recognize that the violation is expected.

The run may therefore report:

```text
Generation: SUCCESS
Validation:
  expected violations: 100
  unexpected violations: 0
Result: ACCEPTED DIRTY DATASET
```

The exact reporting semantics belong to the validation architecture.

---

# 15. Unexpected Violations

If a dirty scenario expects:

```text
100 invalid records
```

but the validator finds:

```text
100 expected
+
17 unexpected
```

the run should normally fail.

Unexpected defects indicate that the generator itself may be broken.

---

# 16. Scenario Failure

Scenario resolution should fail clearly when:

```text
scenario does not exist
scenario parameters are invalid
scenario combination is incompatible
scenario target cannot be satisfied
```

The system should not silently fall back to baseline.

Silent fallback makes benchmark datasets misleading.

---

# 17. Scenario Target Failure

Suppose:

```text
hot_product
target top-share >= 20%
```

but the generated dataset produces:

```text
top-share = 11%
```

The generator technically produced data, but failed the scenario contract.

The system should report:

```text
Scenario validation: FAIL
```

and should normally prevent the dataset from being published as the requested scenario.

---

# 18. Statistical Failure

Statistics may be:

```text
mandatory
optional
diagnostic
```

depending on the metric.

A failure in a mandatory statistic calculation may block the run.

A failure in optional diagnostic telemetry may only produce a warning.

This distinction should be explicit.

---

# 19. Output Errors

Output failures are normally fatal.

Examples:

```text
directory cannot be created
permission denied
disk full
write failure
stream closed unexpectedly
serialization failure
```

A dataset should not be published as complete if output writing failed.

---

# 20. Artifact Errors

Artifact validation occurs after writing.

Examples:

```text
expected file missing
row count mismatch
header mismatch
checksum mismatch
manifest references nonexistent file
```

These should normally block publication.

---

# 21. Unexpected Programming Errors

Unexpected exceptions indicate a defect or unanticipated runtime condition.

Examples:

```text
NullPointerException
IndexOutOfBoundsException
IllegalStateException
unexpected arithmetic failure
```

The system should not broadly catch these and pretend generation succeeded.

Unexpected errors should surface with diagnostic context.

---

# 22. Exception Handling Principle

Do not write:

```scala
try {
  generate()
} catch {
  case _: Throwable =>
    logger.error("Generation failed")
}
```

and continue.

This can turn a failed generation into an apparently successful dataset.

Instead:

```text
catch only where recovery or meaningful translation exists
```

Otherwise allow the failure to propagate to the application boundary.

---

# 23. Application Boundary

There should be one clear top-level boundary responsible for:

- logging the failure;
- setting process outcome;
- cleaning up temporary output;
- reporting run identity;
- preserving diagnostics.

Domain code should not decide:

```text
exit process
```

or:

```text
delete the entire output directory
```

The application boundary owns process-level behavior.

---

# 24. Domain-Level Error Handling

Domain functions should prefer explicit preconditions and meaningful failures.

For example:

```scala
require(
  buildings.nonEmpty,
  "Cannot generate an address because no buildings are available."
)
```

This style is already used in the project.

As architecture matures, domain-specific validation may use more explicit error representations where needed.

---

# 25. `require` vs Domain Errors

`require` is useful for programmer-facing preconditions and simple invalid inputs.

For complex domain workflows, a structured error model may eventually be clearer.

Example:

```text
AddressGenerationFailure(
  addressId,
  reason
)
```

The project should not convert every `require` immediately.

Introduce richer error types where error handling genuinely benefits from them.

---

# 26. Avoiding Exception Explosion

Do not create:

```text
CustomerGenerationException
CustomerIdentityException
CustomerPreferenceException
CustomerBehaviorException
...
```

unless callers need to distinguish those failures.

The number of exception types should be driven by handling requirements.

---

# 27. Error Context

Every important failure should provide useful context.

Useful context includes:

```text
entity
entity ID
scenario
seed
configuration path
phase
reference ID
```

Example:

```text
Failed to generate OrderItem
orderId=ORDER_00001234
reason=selected product does not exist
scenario=baseline
seed=42
```

---

# 28. Avoid Sensitive Output

Diagnostics should not accidentally expose:

- secrets;
- credentials;
- environment variables;
- private filesystem data unrelated to the run.

The generator does not need sensitive information in normal errors.

---

# 29. Error Message Quality

A good error message answers:

```text
What failed?
Where?
Why?
What input caused it?
What should be checked?
```

Example:

```text
Invalid scenario configuration at
scenario.skew.product.top-share:
expected a value in [0,1], observed 1.20.
```

This is much better than:

```text
IllegalArgumentException
```

alone.

---

# 30. Validation Error Aggregation

Configuration validation should usually aggregate independent errors.

Example:

```text
Invalid configuration:
- generator.profile is unsupported
- customer.activity.mean must be > 0
- scenario.skew.top-share must be <= 1
```

This lets users fix multiple configuration problems in one iteration.

---

# 31. Generation Error Aggregation

Generation-time errors are different.

If generation encounters a fatal condition that makes continuation unsafe, fail immediately.

Do not attempt to collect millions of identical errors.

For repetitive recoverable anomalies, bounded aggregation may be useful.

---

# 32. Error Rate Thresholds

Some future generation conditions may be probabilistic or data-quality related.

For example:

```text
lookup failure rate <= 0.01%
```

A small number of recoverable anomalies may be acceptable.

If the rate exceeds a configured threshold:

```text
generation fails
```

This should be explicit rather than hidden.

---

# 33. Retry Semantics

Retries should only be used for genuinely transient failures.

For the current local deterministic generator, most failures are not transient.

Examples where retry may eventually make sense:

```text
object-storage upload
external reference-data retrieval
```

if such capabilities are added.

Do not retry deterministic programming errors.

---

# 34. Idempotency

Generation should be safe to rerun with:

```text
same seed
same configuration
same reference data
```

This is naturally supported by deterministic generation.

Output handling must also ensure reruns do not accidentally merge old and new artifacts.

---

# 35. Partial Output

A failed run may have already written:

```text
customers.csv
products.csv
orders.csv
```

but not:

```text
events.csv
manifest.json
```

That directory must not be treated as a complete dataset.

The preferred approach is temporary output followed by publication.

---

# 36. Temporary Output

Conceptually:

```text
/tmp/run-123/
```

contains intermediate artifacts.

Only after successful completion:

```text
/tmp/run-123/
        ↓
datasets/run-123/
```

The exact filesystem mechanism may evolve.

---

# 37. Atomic Publication

The publication boundary should be as atomic as the storage system permits.

The conceptual contract is:

```text
incomplete
```

versus:

```text
complete
```

A consumer should not have to guess which state it is observing.

---

# 38. Manifest as Completion Marker

A useful convention is:

```text
manifest.json
```

is written only after all required generation, validation, statistics, and output steps succeed.

Therefore:

```text
manifest exists
```

can mean:

```text
dataset publication completed
```

provided the project explicitly adopts this convention.

---

# 39. Failed Manifest

If failed-run metadata is retained, it should be clearly distinguished.

For example:

```text
run-status = FAILED
```

rather than allowing a partial manifest to look like a successful dataset.

---

# 40. Cleanup Policy

Temporary artifacts should normally be cleaned after failure.

However, cleanup may be optional when diagnostics are more valuable.

Possible policy:

```text
normal failure → clean temporary output
debug mode     → retain temporary output
```

The exact policy should be configurable if needed.

---

# 41. Cleanup Failure

If cleanup itself fails:

```text
generation already failed
cleanup also failed
```

The primary error should remain visible.

The cleanup error should be reported as secondary diagnostic information.

Do not replace the root cause with:

```text
failed to delete directory
```

---

# 42. Resource Management

File handles, streams, and temporary resources must be safely closed.

Use structured resource management.

Avoid:

```scala
val writer = Files.newBufferedWriter(...)
...
writer.close()
```

without protection against exceptions.

The implementation should use appropriate Scala/JVM resource-management techniques.

---

# 43. Large File Failure

For large output files, a failure halfway through writing can leave a large partial artifact.

The writer should therefore:

- write to temporary location;
- close resources correctly;
- remove or quarantine failed files;
- report the entity/file that failed.

This is especially important for large event datasets.

---

# 44. Backpressure

The initial local generator does not require a complex streaming backpressure architecture.

However, future streaming output should avoid unbounded producer buffering.

If generation becomes faster than output:

```text
generator
    ↓
bounded buffer
    ↓
writer
```

should be considered.

Do not introduce this complexity before streaming output requires it.

---

# 45. Memory Failure

If the generator runs out of memory, normal application-level exception handling may not provide reliable recovery.

The architecture should instead reduce memory pressure through:

- streaming;
- bounded collections;
- reference-data indexing;
- incremental statistics.

Memory management is primarily a design problem, not an exception-catching problem.

---

# 46. Timeouts

The local deterministic generator generally does not need arbitrary timeouts.

Future external operations may.

For example:

```text
reference-data service
object-storage upload
```

if added later.

Timeouts should belong to those infrastructure boundaries.

---

# 47. Cancellation

A future long-running generation process may need cancellation.

Cancellation should leave the dataset in an explicitly incomplete state.

Conceptually:

```text
RUNNING
  ↓ cancel
CANCELLED
```

and not:

```text
SUCCESS
```

The exact implementation can be added when long-running generation requires it.

---

# 48. Process Exit Semantics

The command-line application should eventually expose meaningful process outcomes.

Conceptually:

```text
0 → successful dataset
non-zero → generation failed
```

The exact exit-code taxonomy can be introduced if downstream automation needs it.

The important rule is that failed generation must not return success.

---

# 49. Logging Failure Context

At the application boundary, errors should include:

```text
run ID
scenario
profile
seed
phase
```

where available.

Example:

```text
Generation failed
run=20260915-183000-a1b2
scenario=hot_product
profile=large
seed=42
phase=output
```

---

# 50. Structured Error Reporting

As observability matures, errors may be represented structurally:

```text
ErrorEvent
  category
  phase
  message
  entity
  entityId
  recoverable
  expected
```

This can support both logging and final run metadata.

It should be introduced only if multiple consumers justify it.

---

# 51. Resilience Does Not Mean Hiding Errors

The purpose of resilience is:

```text
recover safely where recovery is valid
```

not:

```text
never fail
```

For a deterministic data generator, correctness is more important than maximum continuation.

---

# 52. Recoverable vs Fatal Matrix

A conceptual policy:

| Failure | Recoverable? | Default |
|---|---:|---|
| Invalid configuration | No | Fail |
| Missing required reference data | No | Fail |
| Invalid generation plan | No | Fail |
| Missing optional reference data | Maybe | Warn/continue |
| Expected dirty violation | Yes | Continue |
| Unexpected relationship violation | No | Fail |
| Scenario target miss | No | Fail |
| Optional metric failure | Yes | Warn |
| Output write failure | No | Fail |
| Artifact validation failure | No | Fail |
| Unexpected programming error | No | Fail |

The exact policies can evolve as requirements become concrete.

---

# 53. Error Severity

A useful conceptual severity model is:

```text
INFO
WARN
ERROR
FATAL
```

However, logging severity and execution semantics should not be conflated.

A `WARN` may still be execution-fatal.

A `FATAL` error should always prevent successful publication.

---

# 54. Error Classification vs Logging Level

For example:

```text
scenario target missed
```

could be logged as:

```text
ERROR
```

while its semantic classification is:

```text
ScenarioValidationFailure
```

The error category tells the system what happened.

The log level tells operators how prominently to display it.

---

# 55. Domain Boundary

Domain code should not know about:

```text
SLF4J
exit codes
temporary directories
manifest paths
```

Those belong to application/infrastructure layers.

Domain code should communicate failure through its normal return/exception mechanisms.

---

# 56. Application Boundary

The application layer coordinates:

```text
configuration
generation
validation
statistics
output
manifest
failure reporting
```

It is the right place to decide:

```text
publish
or
do not publish
```

---

# 57. Infrastructure Boundary

Infrastructure components handle failures related to:

```text
filesystem
serialization
logging
metrics
external storage
```

They should translate low-level failures into meaningful infrastructure errors where callers benefit.

---

# 58. SOLID Application

## Single Responsibility

Configuration validation should not write output.

Output writing should not classify domain violations.

Manifest generation should not recover from generation failures.

## Open/Closed

New output backends should introduce their own failure semantics without rewriting domain generators.

## Liskov Substitution

Alternative writers must preserve the output contract, including failure behavior.

## Interface Segregation

Avoid one giant error-handling interface.

## Dependency Inversion

Application orchestration can depend on focused abstractions for output and observability.

---

# 59. Strategy Pattern

Error-handling strategies should not be abstracted unless there is genuine variation.

Potential legitimate examples:

```text
CleanupPolicy
RetryPolicy
OutputPublicationStrategy
```

if multiple implementations become necessary.

Do not introduce:

```text
ErrorStrategy
```

merely to demonstrate a design pattern.

---

# 60. Result Types

Some operations may eventually benefit from explicit result types.

Conceptually:

```scala
Either[GenerationError, Customer]
```

or:

```scala
Either[ValidationError, ValidationResult]
```

This is useful when the caller must explicitly handle expected failure.

However, not every internal operation needs `Either`.

Use explicit result types where failure is a normal part of the operation rather than an exceptional programming condition.

---

# 61. Exceptions

Exceptions remain appropriate for:

- unrecoverable infrastructure failures;
- violated programmer assumptions;
- unexpected runtime failures.

The project should avoid forcing every failure into a functional result abstraction.

---

# 62. Validation Results

Validation is naturally represented as structured results because multiple violations may be expected.

Conceptually:

```text
ValidationResult
  status
  violations
  expectedViolations
  unexpectedViolations
```

This fits the validation architecture better than throwing immediately on every violation.

---

# 63. Generation Results

A future generation service may return something like:

```text
GeneratedDataset
  data
  plan
  generationSummary
```

or a structured success/failure result.

The exact type should emerge from implementation needs.

Do not build a large hierarchy of result objects prematurely.

---

# 64. Error Propagation

The default rule should be:

```text
lower layer detects
    ↓
adds meaningful context
    ↓
propagates
    ↓
application boundary decides outcome
```

Do not swallow errors.

Do not duplicate the same error message at every layer.

---

# 65. Error Wrapping

When wrapping an error, preserve the root cause.

For example:

```text
OutputError:
  failed to write orders.csv
    caused by:
      permission denied
```

This is preferable to replacing the original cause.

---

# 66. Root Cause Preservation

Diagnostics should make it possible to identify:

```text
root cause
```

even when multiple layers add context.

This matters particularly for:

```text
serialization
filesystem
reference-data
```

failures.

---

# 67. Retry Policy

If retries are introduced, they should specify:

```text
which failures are retryable
maximum attempts
backoff
whether operation is idempotent
```

Never retry:

```text
invalid configuration
invalid data
programming errors
```

by default.

---

# 68. Partial Retry

The architecture should eventually support retrying an individual infrastructure operation without regenerating the entire dataset where practical.

Example:

```text
upload orders file failed
```

might be retried.

But:

```text
order generation failed
```

may require rerunning the affected generation stage.

This is future architecture, not a current requirement.

---

# 69. Deterministic Recovery

If a generation stage is retried, it should produce the same records under the same deterministic inputs.

This is another benefit of isolated random streams.

---

# 70. Failure and Randomness

A subtle issue arises if generation uses shared mutable random state.

Suppose:

```text
customer generation fails
```

and the system retries.

If random state has advanced unpredictably, retrying may produce different data.

Deterministic semantic random streams reduce this problem.

---

# 71. Failure and Scenario State

Scenario state should also be deterministic.

A retry should not accidentally create:

```text
different campaign period
different hot keys
different quality corruption
```

under identical inputs.

Scenario selection should therefore be derived from stable configuration and random streams.

---

# 72. Failure Diagnostics for Statistical Tests

When a scenario fails statistically, report:

```text
scenario
metric
target
observed
tolerance
sample size
seed
```

Example:

```text
Scenario validation failed:
hot_product

Metric:
top_product_share

Target:
>= 0.20

Observed:
0.137

Seed:
42

Sample:
100000 order items
```

This is highly actionable.

---

# 73. Failure Diagnostics for Relationships

For a relationship violation, report:

```text
entity
record ID
foreign key
expected parent
relationship
```

Example:

```text
OrderItem ORDER_ITEM_00012
references missing Order ORDER_00999
relationship: OrderItem.orderId → Order.id
```

---

# 74. Failure Diagnostics for Output

For output failures, report:

```text
file
entity
operation
path
phase
```

Example:

```text
Failed writing events.csv
operation=flush
phase=output
```

Avoid exposing internal stack traces as the only diagnostic.

---

# 75. Resilience and Large Datasets

At xlarge scale, failure cost is high.

A failure after:

```text
90 minutes
```

is expensive.

Therefore preflight validation becomes increasingly valuable.

Before generation:

```text
configuration
reference data
generation plan
scenario
output location
disk capacity where feasible
```

should be checked.

---

# 76. Preflight Validation

A future preflight phase may verify:

```text
configuration valid
reference data valid
scenario valid
plan valid
output directory usable
```

before expensive generation.

This is one of the highest-value resilience improvements.

---

# 77. Disk-Space Checks

For local filesystem output, the generator may eventually estimate required space.

Inputs:

```text
expected record counts
estimated row size
file count
format
compression
```

Then compare with available capacity.

This is optional but valuable for very large runs.

---

# 78. Resource Limits

The generator should have safety limits where configuration could accidentally request unreasonable work.

Examples:

```text
maximum customers
maximum events
maximum output size estimate
maximum scenario skew
```

The limits should fail clearly rather than allowing an accidental runaway job.

---

# 79. Configuration Safety

Example:

```text
events per session = 10^12
```

should not be accepted merely because the number is syntactically valid.

Semantic safety validation should protect the environment.

---

# 80. Avoiding Silent Truncation

The generator must never silently reduce:

```text
requested count
```

because it cannot handle the requested scale.

If a safety limit is exceeded:

```text
fail with clear error
```

rather than:

```text
generate fewer rows
```

unless the configuration explicitly requests adaptive behavior.

---

# 81. Adaptive Behavior

Adaptive generation can be useful in future systems, but it must be explicit.

Example:

```text
memory-aware generation
```

might alter batch size.

That is acceptable because:

```text
batch size
```

is an execution concern.

It should not silently alter:

```text
business cardinality
```

or:

```text
scenario semantics
```

---

# 82. Observability During Failure

The system should report the last successful phase.

Example:

```text
Generation failed.

Completed:
  customers
  products
  orders

Failed:
  order_items

Run:
  ...

Scenario:
  ...

Seed:
  42
```

This makes large-run failures diagnosable.

---

# 83. Failure State Machine

A run can conceptually move through:

```text
CREATED
   ↓
PREFLIGHT
   ↓
GENERATING
   ↓
VALIDATING
   ↓
WRITING
   ↓
FINALIZING
   ↓
COMPLETED
```

Failure states may be:

```text
FAILED
CANCELLED
```

A run should never move backward into a successful state after failure.

---

# 84. Run Status

The manifest/run metadata may eventually contain:

```text
status =
  RUNNING
  COMPLETED
  FAILED
  CANCELLED
```

Only:

```text
COMPLETED
```

should represent a published dataset.

---

# 85. State Transition Validation

Run-state transitions should be valid.

For example:

```text
CREATED → PREFLIGHT
PREFLIGHT → GENERATING
GENERATING → VALIDATING
VALIDATING → WRITING
WRITING → FINALIZING
FINALIZING → COMPLETED
```

Failure may occur from most active states.

Invalid transitions should not be silently accepted.

---

# 86. Testing Error Handling

Error semantics require dedicated tests.

Examples:

```text
invalid configuration fails before generation
missing geography fails before output
invalid scenario fails before generation
unexpected FK violation blocks publication
dirty scenario allows expected violations
output failure prevents completion
partial output is not published
same failure reports useful context
```

---

# 87. Testing Cleanup

Tests should verify that:

```text
failed run
→ temporary output cleaned
```

where cleanup is the configured policy.

If debug retention is enabled:

```text
failed run
→ temporary output retained and marked incomplete
```

---

# 88. Testing Publication

A valuable test:

```text
successful run
→ manifest exists
```

and:

```text
failed run
→ successful manifest does not exist
```

This establishes the publication contract.

---

# 89. Testing Retry

If retry functionality is introduced later, test:

```text
transient failure
→ retry
→ success
```

and:

```text
permanent failure
→ no endless retries
```

Retry behavior should have strict bounds.

---

# 90. Testing Error Aggregation

Configuration tests should verify multiple independent errors are reported together.

Example:

```text
three invalid fields
→ three diagnostics
```

rather than stopping after the first unrelated field.

---

# 91. Testing Unexpected Exceptions

The E2E boundary should verify that unexpected exceptions:

```text
fail the process
```

rather than being swallowed.

The test should not depend on exact stack-trace formatting.

---

# 92. Testing Scenario Failure

A statistical scenario test should deliberately use an impossible or overly strict target and verify:

```text
scenario validation fails
publication blocked
```

This protects the benchmark dataset contract.

---

# 93. Testing Output Failure

Output infrastructure can use a test double or controlled filesystem condition to simulate:

```text
write failure
```

Then verify:

```text
run does not complete
manifest is not published
error contains file/phase context
```

---

# 94. Testing Reference-Data Failure

Use a deliberately invalid fixture:

```text
building references nonexistent society
```

Then verify:

```text
preflight fails
no expensive generation begins
```

This protects fail-fast behavior.

---

# 95. Error Handling Anti-Patterns

Avoid:

### Catch-all recovery

```scala
catch {
  case _: Throwable => continue
}
```

### Silent fallback

```text
unknown scenario → baseline
```

### Silent truncation

```text
requested 1B records → generated 100M
```

without explicit configuration.

### Partial success reporting

```text
some files written → "generation complete"
```

### Generic errors

```text
Something went wrong
```

### Infinite retries

```text
retry forever
```

### Business logic in infrastructure

```text
writer decides whether order is valid
```

---

# 96. Current Implementation vs Target

The current project already uses explicit preconditions in places such as address generation.

It also has layered validation that distinguishes structural, primary-key, foreign-key, and business-rule checks.

The target architecture extends this into:

- formal failure categories;
- preflight validation;
- expected vs unexpected scenario violations;
- atomic publication;
- richer run status;
- output/artifact failure handling;
- structured diagnostics;
- reproducible retry behavior;
- stronger large-scale safeguards.

These are target capabilities, not claims that they all exist today.

---

# 97. Recommended Implementation Sequence

Implement resilience incrementally:

```text
1. preserve current meaningful failures
        ↓
2. improve configuration diagnostics
        ↓
3. strengthen reference-data preflight
        ↓
4. define application-level failure boundary
        ↓
5. prevent successful publication after validation failure
        ↓
6. introduce temporary output/publish boundary
        ↓
7. strengthen output failure handling
        ↓
8. add structured run status
        ↓
9. add richer error diagnostics
        ↓
10. add scenario/statistical failure semantics
        ↓
11. add resource/safety checks for large scale
        ↓
12. add retry only where genuine transient operations exist
```

---

# 98. Quality Gate

Before considering error handling mature, verify:

```text
□ invalid configuration fails early
□ invalid reference data fails early
□ impossible plans fail early
□ unexpected generation errors do not disappear
□ validation failures block clean publication
□ expected dirty violations remain distinguishable
□ scenario target failures are visible
□ output failures block completion
□ partial datasets are never presented as complete
□ manifest completion semantics are explicit
□ diagnostics contain useful context
□ root causes are preserved
□ retries are bounded and selective
□ deterministic generation remains reproducible
```

---

# 99. Definition of Done

The error-handling architecture is complete when:

### Correctness

The system cannot silently publish an invalid or incomplete dataset.

### Diagnostics

Failures identify the relevant:

```text
phase
entity
record
configuration
scenario
seed
```

where applicable.

### Resilience

Recoverable failures have explicit recovery semantics.

Fatal failures terminate the run cleanly.

### Reproducibility

Retries and reruns do not introduce unexplained randomness.

### Operations

Large generation runs provide enough information to determine:

```text
where they failed
why they failed
what was completed
```

### Output Integrity

A dataset is published only after its completion criteria are satisfied.

---

# 100. Explicit Architecture Decisions

### Decision 1

Failure semantics are part of the generator contract.

### Decision 2

Configuration and foundational reference-data failures should fail fast.

### Decision 3

Unexpected programming errors must not be swallowed.

### Decision 4

Validation failure is distinct from execution failure.

### Decision 5

Expected dirty-data violations are distinct from unexpected generator defects.

### Decision 6

Scenario target failure should prevent publication as the requested scenario.

### Decision 7

Output and artifact failures are normally fatal.

### Decision 8

The application boundary owns process-level failure handling and publication decisions.

### Decision 9

Domain code should not manage process exits or output cleanup.

### Decision 10

Temporary output followed by final publication is the preferred long-term artifact model.

### Decision 11

A successful manifest should represent a completed dataset.

### Decision 12

Retries should exist only for genuinely transient and safely retryable operations.

### Decision 13

Error messages should preserve root causes and provide actionable context.

### Decision 14

Safety limits should prevent accidental runaway generation.

### Decision 15

Resilience means safe recovery where appropriate, not suppression of failures.

---

# 101. Final Design Summary

The target failure architecture is:

```text
                   Input
                     |
                     v
               Preflight Check
                     |
          +----------+----------+
          |                     |
       invalid                valid
          |                     |
          v                     v
        FAIL                Generation
                                |
                       +--------+--------+
                       |                 |
                    failure            success
                       |                 |
                       v                 v
                      FAIL            Validation
                                         |
                              +----------+----------+
                              |                     |
                           invalid                valid
                              |                     |
                              v                     v
                             FAIL                Statistics
                                                   |
                                                   v
                                                 Output
                                                   |
                                         +---------+---------+
                                         |                   |
                                      failure             success
                                         |                   |
                                         v                   v
                                        FAIL             Artifact
                                                           |
                                                   +-------+-------+
                                                   |               |
                                                invalid          valid
                                                   |               |
                                                   v               v
                                                  FAIL          Manifest
                                                                   |
                                                                   v
                                                               PUBLISH
```

The most important rule is simple:

> **A dataset is successful only when the generator can prove that the requested artifact was produced according to its contract.**

That contract includes not only records and relationships, but also:

```text
configuration
scenario
statistics
validation
output integrity
reproducibility
```

The system should prefer a clear failure over a silent approximation.

For a synthetic-data generator that will later drive Spark performance experiments, this is particularly important: an invalid or partially generated dataset can produce performance conclusions that appear technical but are actually caused by a hidden data-generation defect.

The error architecture therefore protects the integrity of everything built on top of ShopSphere.
