# 14 — Observability

## 1. Purpose

This document defines the observability strategy for the `ecommerce-data-generation` platform.

The generator must make it possible to answer, during and after a generation:

- What is being generated?
- Which configuration and scenario are active?
- How much data has been generated?
- Which entity is currently being processed?
- How quickly is generation progressing?
- How much output has been written?
- How much memory is being used?
- Did validation succeed?
- What statistics were produced?
- If generation failed, where and why did it fail?

Observability is therefore part of the generator's operational contract rather than an optional debugging feature.

---

## 2. Observability Goals

The observability design has five primary goals:

1. **Progress visibility**
2. **Performance visibility**
3. **Correctness visibility**
4. **Failure diagnosis**
5. **Generation auditability**

The design should remain useful for both:

- interactive local development
- large-scale automated dataset generation

---

## 3. Three Observability Signals

The initial system should distinguish three major observability signals:

```text
Logs
Metrics
Generation metadata
```

### Logs

Explain what the generator is doing and why.

### Metrics

Quantify generation behavior.

### Generation metadata

Describes the completed dataset and its configuration.

These mechanisms complement each other and should not be treated as interchangeable.

---

## 4. Logging Strategy

Logging should be structured around meaningful generation stages.

Example lifecycle:

```text
START
  ↓
CONFIGURATION_LOADED
  ↓
REFERENCE_DATA_READY
  ↓
ENTITY_GENERATION_STARTED
  ↓
ENTITY_GENERATION_PROGRESS
  ↓
ENTITY_GENERATION_COMPLETED
  ↓
VALIDATION_STARTED
  ↓
VALIDATION_COMPLETED
  ↓
MANIFEST_WRITTEN
  ↓
GENERATION_COMPLETED
```

Failures should produce an explicit failure event containing the relevant context.

---

## 5. Log Levels

The initial logging model should support standard severity levels:

| Level | Purpose |
|---|---|
| ERROR | Generation cannot continue or completed generation is invalid |
| WARN | Unexpected but recoverable condition |
| INFO | Major generation lifecycle events |
| DEBUG | Detailed diagnostic information |
| TRACE | Very detailed development-level diagnostics, if supported |

Normal generation should not emit a log message for every record.

Per-record logging would create unnecessary overhead and make large-scale generation impractical.

---

## 6. Structured Log Context

Where possible, logs should include structured context such as:

```text
generation_id
seed
profile
scenario
entity
stage
records_generated
records_expected
elapsed_time
records_per_second
```

This allows logs to be correlated with the generation manifest and performance measurements.

Example conceptual event:

```text
generation_id=000001
entity=order
stage=generation
records_generated=500000
records_expected=1000000
rate=125000 records/s
```

The exact logging framework is governed by the technology decisions.

---

## 7. Generation Lifecycle Logging

At the beginning of generation, log:

- generation ID
- selected profile
- selected scenario
- seed
- date range
- output location
- output format
- major configuration summary

Sensitive information should not be logged unnecessarily.

At completion, log:

- total duration
- total records
- output size
- validation status
- entity-level summary
- final generation status

---

## 8. Progress Reporting

Large generations need visible progress.

Progress should be reported at meaningful intervals rather than per record.

Possible progress dimensions:

```text
entity progress
overall record progress
elapsed time
current throughput
estimated remaining time
```

Example:

```text
Generating order
  62% complete
  6.2M / 10M records
  1.4M records/s
  elapsed: 00:00:04.4
```

The initial implementation may use periodic console logging.

A richer progress UI can be introduced later if useful.

---

## 9. Progress Frequency

Progress frequency should be configurable or governed by sensible defaults.

The implementation should avoid excessive progress output for small datasets and insufficient visibility for very large datasets.

A practical strategy is:

- always log major lifecycle transitions
- report progress periodically for large entities
- report entity completion
- report final generation completion

Progress reporting must not materially distort performance measurements.

---

## 10. Entity-Level Metrics

For every generated entity, the system should track at least:

- expected row count
- generated row count
- generation duration
- generation throughput
- output file count
- output byte size
- validation status

Example:

```text
entity = customer
rows = 1,000,000
duration = 2.8s
throughput = 357,143 rows/s
files = 4
size = 420 MB
```

These metrics are useful for identifying slow entity generators and output bottlenecks.

---

## 11. Overall Generation Metrics

The generator should produce aggregate metrics such as:

```text
total_entities
total_records
total_output_bytes
total_files
generation_duration
validation_duration
records_per_second
bytes_per_second
```

Where appropriate, generation and validation time should be reported separately.

This prevents validation cost from being incorrectly attributed to record generation.

---

## 12. Stage-Level Timing

Major pipeline stages should be timed independently.

Example:

```text
configuration loading       0.02s
reference-data preparation  0.31s
customer generation        2.40s
product generation         0.18s
order generation           5.10s
validation                 1.72s
manifest generation        0.03s
```

Stage timing makes bottleneck identification much easier than a single total-duration measurement.

---

## 13. Throughput Metrics

Throughput should be measured using clear definitions.

Primary metric:

```text
records_per_second
```

Additional useful metrics:

```text
bytes_per_second
records_per_entity_per_second
output_bytes_per_second
```

The metric definition should specify whether time includes:

- generation
- serialization
- filesystem writes
- validation

These should not be mixed without being explicitly labeled.

---

## 14. Memory Observability

Memory behavior matters because the generator is intended to support large datasets.

The system should eventually expose:

- current heap usage
- peak heap usage
- available memory where meaningful
- entity-level memory behavior where measurable

The generator should not require high-cardinality per-record diagnostic state merely to produce metrics.

Memory measurements should be sampled rather than continuously tracked at excessive granularity.

---

## 15. Garbage Collection and JVM Metrics

Because the initial implementation is a Scala/JVM application, JVM-level metrics may become useful for performance analysis.

Potential metrics include:

- heap utilization
- GC count
- GC pause time
- thread count
- process CPU utilization

These should initially be treated as diagnostic/performance metrics rather than business metrics.

The project should avoid introducing a large monitoring stack before there is a concrete need.

---

## 16. Validation Observability

Validation should produce a clear summary.

Example:

```text
Validation
  schema: PASS
  row counts: PASS
  referential integrity: PASS
  business rules: PASS
  temporal rules: PASS
  distributions: PASS
  data quality: PASS
  output: PASS
```

If validation fails, the summary should identify:

- validation category
- affected entity
- affected field/rule
- expected condition
- observed condition
- severity
- representative diagnostic examples

The validator should not dump millions of individual violations.

---

## 17. Data-Quality Metrics

When data-quality scenarios are enabled, the generator should report configured versus observed behavior.

Example:

```text
email null rate
  configured: 2.0%
  observed:   2.03%
  status:      PASS
```

Useful metrics include:

- null counts/rates
- malformed-value counts
- duplicate counts
- invalid-reference counts
- out-of-range values
- other configured violation categories

The exact metrics depend on the enabled data-quality profile.

---

## 18. Distribution Metrics

The generation manifest and observability output should expose useful distribution statistics.

Examples:

- category frequencies
- customer-segment frequencies
- acquisition-channel frequencies
- order-status frequencies
- product popularity concentration
- numeric summary statistics
- configured versus observed proportions

For large datasets, statistics should be collected efficiently and should not require retaining every generated value.

---

## 19. Cardinality Metrics

Cardinality should be observable at the entity and field level where relevant.

Example:

```text
field = customer.email
rows = 1,000,000
distinct = 982,100
nulls = 17,900
```

This allows the user to verify whether the requested cardinality profile was actually achieved.

Cardinality calculations must be designed carefully for large datasets so that observability itself does not become a major memory bottleneck.

---

## 20. Skew Metrics

When skew is enabled, observability should expose the resulting concentration.

Useful metrics include:

- number of hot entities
- percentage of activity attributed to hot entities
- top-N activity share
- configured skew level
- observed skew level

Example:

```text
entity: product
skew: enabled
hot products: 100
top 100 share: 31.4%
```

This makes skew scenarios measurable rather than merely declarative.

---

## 21. Output Metrics

The output layer should report:

- output root
- entity directories
- file count
- total bytes
- records per file
- minimum file size
- maximum file size
- average file size

These measurements are especially useful for later Spark file-layout experiments.

---

## 22. Small-File Detection

The output layer should make small-file conditions visible.

The generator should not necessarily reject small files because they may be intentionally created for performance experiments.

Instead, it can report:

```text
file_count = 1000
average_file_size = 4.2 MB
```

and optionally classify the layout.

The distinction is important:

```text
unexpected small files → diagnostic warning
intentional small-file scenario → expected behavior
```

---

## 23. Generation Manifest as Persistent Observability

The manifest is the durable record of generation state.

Console logs describe what happened during execution.

The manifest records what was ultimately produced.

The manifest should therefore be sufficient to reconstruct the important characteristics of a generation without reading every output file.

At minimum, it should link:

```text
configuration
seed
scenario
logical statistics
validation
physical output
```

---

## 24. Generation ID as Correlation Identifier

The generation ID should be used consistently across:

- logs
- metrics
- manifest
- output directory
- validation results

This provides a single correlation identifier.

Example:

```text
generation-000123
```

All major logs and metadata associated with that run should be traceable to this identifier.

---

## 25. Error Observability

Errors should contain enough context to diagnose the failure.

A useful error should answer:

1. What failed?
2. During which stage?
3. For which entity?
4. Under which configuration?
5. What was expected?
6. What was observed?
7. Can the failure be reproduced using the recorded seed/configuration?

Example conceptual error:

```text
Generation failed
generation_id: generation-000123
entity: order_item
stage: relationship_generation
rule: order_id must reference an existing order
observed: missing order_id O918273
seed: 42
```

This is substantially more useful than a generic exception such as:

```text
Generation failed
```

---

## 26. Failure Classification

Failures should be classified where practical.

Suggested categories:

```text
CONFIGURATION_ERROR
GENERATION_ERROR
RELATIONSHIP_ERROR
VALIDATION_ERROR
OUTPUT_ERROR
FILESYSTEM_ERROR
INTERNAL_ERROR
```

Classification allows automation and CI systems to distinguish configuration problems from generator defects.

---

## 27. Warnings vs Errors

The generator should distinguish expected scenario behavior from unexpected defects.

Example:

### Expected

```text
dirty-data profile enabled
email null rate = 2%
```

This is not an error.

### Unexpected

```text
clean profile
email null rate = 2%
```

This should be reported as a validation failure.

Similarly, intentionally generated skew should not be treated as an anomaly merely because it produces uneven distributions.

---

## 28. Configuration Snapshot

The effective configuration should be captured after all defaults and profile expansions have been resolved.

This is important because a user may specify:

```text
profile = medium
```

while the effective configuration contains many derived values.

The manifest should preserve the effective configuration or a sufficiently complete representation of it.

This makes later reproduction and debugging possible.

---

## 29. Reproducibility and Observability

Observability must support reproduction.

At minimum, a successful generation should make it possible to identify:

```text
generator version
seed
profile
scenario
effective configuration
date range
output configuration
```

If these values are unavailable, diagnosing a future discrepancy becomes substantially harder.

---

## 30. Generator Version

The generation metadata should include the generator version or build identifier.

This is important because:

```text
same seed
+
same configuration
+
different generator version
```

does not necessarily imply identical output.

The reproducibility contract should therefore be interpreted within a defined generator-version boundary.

---

## 31. Deterministic Diagnostics

Diagnostic output should avoid introducing nondeterminism.

For example, validation errors should be ordered deterministically where practical.

Avoid relying on:

- unordered map iteration
- nondeterministic filesystem listing
- thread completion order

Deterministic diagnostics make regression failures easier to compare.

---

## 32. Observability Overhead

Observability must not significantly distort the behavior it measures.

Potential sources of overhead include:

- excessive logging
- expensive exact cardinality calculations
- retaining too much diagnostic state
- per-record metrics
- repeated filesystem statistics
- expensive distribution calculations

The implementation should prefer:

- counters
- streaming statistics
- periodic sampling
- entity-level aggregation
- bounded diagnostic examples

---

## 33. Bounded Diagnostics

When millions of records violate a rule, the system should not retain or print millions of examples.

Instead, capture:

- total violation count
- first N examples
- optionally deterministic representative samples
- affected entity/field
- expected and observed rates

Example:

```text
invalid_reference_count = 12,483
examples_shown = 10
```

This keeps memory and log volume bounded.

---

## 34. Console Output

The initial user experience can be console-oriented.

A typical successful generation might look conceptually like:

```text
Generation started
  id: generation-000001
  profile: medium
  scenario: baseline
  seed: 42

Generating customer ... done
Generating product  ... done
Generating order    ... done
Generating payment  ... done

Validation ... PASS

Generation completed
  records: 12,450,000
  files: 48
  output: 3.8 GB
  duration: 31.4s
  throughput: 396,497 records/s
```

This is sufficient for the first implementation.

A machine-readable logging mode can be introduced later.

---

## 35. Machine-Readable Observability

Future versions may support structured JSON logs or metrics export.

Potential consumers include:

- CI pipelines
- benchmark runners
- dashboards
- experiment orchestration
- automated regression analysis

This should be introduced through a clean logging/metrics abstraction rather than embedding output-specific code throughout generators.

---

## 36. Benchmark Integration

Performance benchmarks should consume the same observability metrics as normal generation where possible.

For example:

```text
records_per_second
bytes_per_second
generation_duration
validation_duration
peak_memory
```

This avoids maintaining two independent measurement implementations.

Benchmark results should record:

- configuration
- seed
- generator version
- hardware/environment where relevant
- measured metrics

---

## 37. Spark Laboratory Integration

Observability should eventually help bridge dataset generation and Spark experiments.

A generated dataset should make it possible to understand:

```text
logical characteristics
    +
physical characteristics
    +
generation characteristics
```

before running a Spark workload.

For example:

```text
10M orders
5M customers
product skew = high
order files = 200
CSV
average file size = 18 MB
```

This provides the necessary context for interpreting Spark performance results.

---

## 38. Initial Implementation Scope

The first implementation should include:

- lifecycle logging
- generation ID
- seed/configuration logging
- entity-level progress
- entity-level timing
- total generation timing
- generation throughput
- output file count and size
- validation summary
- data-quality summary
- distribution summary
- cardinality summary where practical
- skew summary where enabled
- effective configuration in manifest
- generator version in manifest
- clear categorized failures
- bounded diagnostic examples

A sophisticated metrics backend is not required initially.

---

## 39. Deferred Decisions

The following are intentionally deferred:

- Prometheus/OpenTelemetry integration
- external monitoring dashboards
- distributed tracing
- centralized log aggregation
- advanced JVM metrics export
- real-time web UI
- alerting infrastructure
- persistent benchmark database
- sophisticated anomaly detection

These should be introduced only when the project has a concrete operational or performance-analysis need.

---

## 40. Testing Observability

Observability itself should be tested.

Tests should verify:

- generation start is reported
- generation completion is reported
- failures contain useful context
- generation IDs remain consistent
- entity metrics match generated output
- row counts match manifest values
- output sizes are reported correctly
- validation summaries match validator results
- configured versus observed quality rates are reported correctly
- deterministic diagnostics remain stable where promised

Tests should avoid asserting fragile formatting details unless formatting is itself part of the contract.

---

## 41. Observability Principles

The observability design follows these principles:

1. **Every generation should be identifiable.**
2. **Major lifecycle stages should be visible.**
3. **Metrics should have precise definitions.**
4. **Observability must remain bounded in memory and output volume.**
5. **Expected scenario behavior must not be confused with errors.**
6. **Failures should contain enough context for reproduction.**
7. **The manifest is the durable generation record.**
8. **Logs and metrics should correlate through the generation ID.**
9. **Observability must not materially distort performance.**
10. **Operational detail should increase as scale increases, without becoming per-record noise.**

---

## 42. Guiding Principle

> **A generation that cannot explain what it produced, how it performed, and why it failed is not sufficiently observable.**

The initial implementation should therefore provide strong lifecycle, progress, performance, validation, and artifact visibility without introducing an unnecessarily complex monitoring infrastructure.
