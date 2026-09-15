# 21 — Output, Storage, Manifest, and Observability Architecture

## 1. Purpose

This document defines the architecture for everything that happens after ShopSphere domain records have been generated:

- output formatting;
- file layout;
- storage organization;
- output modes;
- streaming and large-scale writing;
- manifest generation;
- dataset metadata;
- generation statistics;
- observability;
- generation diagnostics;
- reproducibility metadata;
- output validation;
- benchmark dataset identification.

The purpose is to ensure that a generated dataset is not merely a directory containing CSV files.

A useful synthetic dataset must be:

- identifiable;
- reproducible;
- inspectable;
- verifiable;
- measurable;
- consumable by downstream systems;
- suitable for repeated Spark experiments.

The output layer therefore forms part of the generator's data contract.

---

# 2. Core Principle

The generator has two different responsibilities:

```text
Generate the world
        ↓
Describe and persist the world
```

Domain generators own the first responsibility.

Output, manifest, statistics, and observability components own the second.

The output layer must not decide:

- how customers behave;
- how products are selected;
- how orders are constructed;
- how returns are determined.

Instead, it receives generated domain data and writes it in a deterministic, well-defined representation.

---

# 3. Output Is a Contract

The generated files are consumed by:

- developers;
- tests;
- data-quality tooling;
- Spark jobs;
- benchmark workloads;
- analytical queries;
- future pipeline components.

Therefore output schema changes are API-like changes.

For example:

```text
customers.csv
```

must have a stable and documented meaning.

Changing:

```text
customer_id
```

to:

```text
id
```

is not merely a formatting change if downstream jobs depend on the previous column.

Output design should therefore explicitly define:

- entity name;
- schema;
- column names;
- column order;
- data types;
- nullability expectations;
- identifier semantics;
- encoding;
- delimiter;
- quoting;
- date/time representation;
- numeric representation.

---

# 4. Current Output Format

The current implementation uses CSV output.

The initial architecture intentionally keeps output simple.

Typical output is organized into entity-specific files such as:

```text
customers.csv
addresses.csv
products.csv
categories.csv
brands.csv
orders.csv
order_items.csv
payments.csv
shipments.csv
returns.csv
sessions.csv
events.csv
```

Reference entities such as:

```text
categories
brands
```

are also written as separate datasets.

The exact current schema remains owned by the corresponding domain models and CSV writer implementation.

---

# 5. Entity-Per-File Principle

The default output boundary should remain aligned with domain entities.

Example:

```text
customers.csv
orders.csv
order_items.csv
products.csv
```

This has several benefits:

- clear ownership;
- easy inspection;
- straightforward Spark ingestion;
- simple validation;
- simple file lifecycle;
- independent downstream processing.

The output layer should not combine unrelated entities merely to reduce the number of files.

---

# 6. Output Directory

A generation run should have a dedicated output directory.

Conceptually:

```text
output/
    customers.csv
    addresses.csv
    products.csv
    ...
    manifest.json
```

A run-specific directory is preferable to overwriting an unrelated previous run.

Example:

```text
datasets/
    shopsphere-medium-baseline/
        ...
```

or:

```text
runs/
    2026-09-15T18-30-00/
        ...
```

The exact naming policy can evolve, but the run boundary should remain explicit.

---

# 7. Dataset Identity

A generated dataset should have an identity independent of its physical directory.

A useful dataset identity can include:

```text
dataset name
generation profile
cardinality profile
scenario
seed
configuration fingerprint
reference-data fingerprint
generator version
```

Example:

```text
dataset:
  name: shopsphere-medium-baseline
  profile: medium
  cardinality: medium
  scenario: baseline
  seed: 42
```

This metadata allows a dataset to be identified without inspecting every record.

---

# 8. Run Identity

Dataset identity and run identity should be distinguished.

A dataset definition may be:

```text
shopsphere-medium-baseline
seed=42
```

A generation run may additionally have:

```text
run_id = 20260915-183000-a1b2
```

This matters when:

- the same dataset is generated multiple times;
- output is regenerated;
- failures occur;
- benchmarking compares multiple runs.

---

# 9. Manifest

The manifest is the central metadata artifact for a generation run.

Conceptually:

```text
manifest
├── dataset identity
├── run identity
├── generator version
├── seed
├── configuration
├── effective configuration
├── reference-data metadata
├── entity statistics
├── validation summary
├── scenario metadata
├── output metadata
└── reproducibility metadata
```

The manifest should make the dataset explainable without reading all files.

---

# 10. Manifest Responsibilities

The manifest should answer:

1. What dataset is this?
2. How was it generated?
3. Which configuration was used?
4. Which seed was used?
5. Which scenario was active?
6. What cardinalities were requested?
7. What cardinalities were actually generated?
8. What reference data was used?
9. What statistics were observed?
10. Did validation pass?
11. What files were produced?
12. What schema/version does each file represent?

---

# 11. Manifest Is Not a Log

The manifest and logs have different responsibilities.

## Manifest

Stable, structured description of the dataset.

## Logs

Operational events emitted during generation.

For example:

```text
INFO Starting generation
INFO Generated 1000 customers
INFO Generated 5000 orders
WARN ...
```

Logs describe execution.

The manifest describes the resulting artifact.

These concerns should remain separate.

---

# 12. Manifest Is Not Statistics

Statistics are another distinct concern.

The statistics component calculates facts such as:

```text
customer count
order count
average order value
product popularity
items per order
return rate
```

The manifest may contain a summary of these statistics.

However:

> statistics calculation and manifest serialization are different responsibilities.

This separation improves testing and evolution.

---

# 13. Manifest Is Not Validation

Validation determines whether generated data satisfies defined rules.

The manifest records the result.

For example:

```text
validation:
  structural: PASS
  primary_key: PASS
  foreign_key: PASS
  business_rule: PASS
```

The validator should not know how JSON is written.

The manifest writer should not implement validation rules.

---

# 14. Proposed Manifest Structure

A future manifest can conceptually resemble:

```json
{
  "dataset": {
    "name": "shopsphere-medium-baseline",
    "version": "1"
  },
  "run": {
    "id": "..."
  },
  "generator": {
    "version": "0.1.0-SNAPSHOT",
    "scala_version": "2.13.18"
  },
  "generation": {
    "seed": 42,
    "profile": "medium",
    "cardinality_profile": "medium",
    "scenario": "baseline"
  },
  "reference_data": {
    "version": "..."
  },
  "entities": {},
  "validation": {},
  "statistics": {},
  "outputs": {}
}
```

This is an architectural target, not a statement that the current manifest has exactly this schema.

---

# 15. Configuration Fingerprint

The manifest should eventually contain a fingerprint of the effective configuration.

The fingerprint should be based on the configuration that actually influenced generation.

Conceptually:

```text
effective configuration
        ↓
canonical representation
        ↓
hash
        ↓
configuration fingerprint
```

This is stronger than recording only the configuration file name.

---

# 16. Why Effective Configuration Matters

Two runs may both claim:

```text
profile = medium
scenario = baseline
```

while having different overrides.

Therefore:

```text
profile name
```

is not sufficient for complete reproducibility.

The effective configuration should be inspectable or reconstructable.

---

# 17. Reference-Data Fingerprint

Reference data also affects generated output.

For ShopSphere this includes data such as:

- geography;
- categories;
- brands;
- any future reference catalogs.

A dataset generated using different reference data may differ even when:

```text
seed
configuration
generator version
```

are identical.

Therefore reference-data identity belongs in reproducibility metadata.

---

# 18. Generator Version

The generator version must be recorded.

A change in:

```text
ProductGenerator
```

or:

```text
OrderGenerator
```

can alter the resulting dataset.

Therefore:

```text
generator version
```

is part of dataset provenance.

For mature benchmarking, a source-control revision may also be recorded.

---

# 19. Seed

The root random seed should be recorded explicitly.

Example:

```text
seed = 42
```

The seed alone does not guarantee reproducibility if code or reference data changes.

Therefore the complete reproducibility identity is closer to:

```text
seed
+
effective configuration
+
reference data
+
generator version
```

---

# 20. Output Metadata

The manifest should describe every output artifact.

Useful fields include:

```text
entity
path
record count
schema version
file format
byte size
checksum
```

Example:

```text
orders.csv
  records: 5,000
  bytes: ...
  checksum: ...
```

Checksums are especially useful for confirming that two generated outputs are byte-identical.

---

# 21. Checksums

Checksums should be considered optional but valuable.

A checksum can identify whether a file changed.

Possible algorithms include:

- SHA-256;
- SHA-512.

The implementation should use a standard library rather than custom hashing.

For very large files, checksum calculation should be streaming.

---

# 22. Byte Size

Output size is useful operational metadata.

Example:

```text
orders.csv → 1.2 MB
events.csv → 8.4 MB
```

This helps diagnose:

- unexpected output growth;
- schema changes;
- malformed records;
- compression differences;
- benchmark dataset size.

Byte size should be measured after the file is finalized.

---

# 23. Record Counts

Every output entity should ideally have a recorded row count.

Example:

```text
customers = 1,000
orders = 5,000
order_items = 12,000
events = 24,000
```

Counts should be based on actual written records, not only requested generation-plan counts.

This distinction matters when optional or probabilistic generation is introduced.

---

# 24. Requested vs Actual Cardinality

The manifest should distinguish:

```text
requested_count
actual_count
```

Example:

```text
returns:
  requested_count: 400
  actual_count: 239
```

This is particularly important for probabilistic domains.

The current generator already demonstrates this distinction with returns.

---

# 25. Entity Statistics

Entity-specific statistics should eventually be available.

Examples:

## Customer

```text
customer count
active count
lifecycle distribution
activity distribution
```

## Product

```text
product count
price distribution
popularity distribution
category distribution
brand distribution
```

## Order

```text
order count
status distribution
AOV
items per order
orders per customer
```

## Event

```text
event count
events per session
event-type distribution
conversion rate
```

The domain owns the meaning of these statistics.

The statistics subsystem owns aggregation and reporting mechanics.

---

# 26. Global Statistics

Global statistics provide a dataset-level view.

Examples:

```text
total records
total order value
average order value
return rate
conversion rate
customer activity
product concentration
```

These metrics are useful for comparing dataset versions.

---

# 27. Distribution Summaries

The manifest or statistics output should summarize important distributions.

For example:

```text
mean
min
p25
median
p75
p95
p99
max
```

This is particularly useful for:

- order value;
- items per order;
- orders per customer;
- sessions per customer;
- events per session;
- product popularity;
- return counts.

---

# 28. Skew Statistics

The skew architecture should feed measurable statistics into the manifest.

Useful metrics include:

```text
distinct keys
top-1 share
top-5 share
top-10 share
p95 frequency
p99 frequency
max frequency
max/median
Gini coefficient
```

The output system should not calculate these itself.

It should serialize results produced by the statistics subsystem.

---

# 29. Validation Summary

The manifest should contain a concise validation summary.

Conceptually:

```text
validation:
  status: PASS

  structural:
    status: PASS

  primary_key:
    status: PASS

  foreign_key:
    status: PASS

  business_rule:
    status: PASS
```

For dirty scenarios:

```text
validation:
  status: EXPECTED_VIOLATIONS
```

may be more appropriate than simply:

```text
FAIL
```

The validation architecture defines the semantics.

---

# 30. Expected Violations

A dirty-data scenario may intentionally violate rules.

The manifest should distinguish:

```text
expected violations
```

from:

```text
unexpected violations
```

Example:

```text
configured:
  invalid postal code rate = 1%

observed:
  0.96%

expected violations:
  12

unexpected violations:
  0
```

This allows a dirty dataset to still be considered successfully generated.

---

# 31. Observability

Observability answers:

> What happened while the generator was running?

Important dimensions include:

- progress;
- throughput;
- duration;
- memory pressure;
- entity generation timing;
- validation timing;
- output timing;
- failures;
- warnings.

Observability should help diagnose both correctness and performance.

---

# 32. Logging

Logs should be structured around meaningful lifecycle events.

Example:

```text
Generation started
Configuration loaded
Reference data loaded
Generation plan created
Customers generated
Products generated
Orders generated
Validation started
Validation completed
Statistics calculated
Output written
Manifest written
Generation completed
```

Avoid logging every generated record.

Per-record logging destroys performance and creates enormous log volume.

---

# 33. Log Levels

A reasonable logging model:

```text
ERROR
WARN
INFO
DEBUG
TRACE
```

Production-scale generation should normally use:

```text
INFO
```

or a similarly concise level.

`DEBUG` may expose additional diagnostics.

`TRACE` should be used carefully and never become the default for large datasets.

---

# 34. Progress Reporting

Large datasets need progress information.

Useful metrics:

```text
entity
generated records
target records
percentage
records/sec
elapsed time
estimated remaining time
```

Example:

```text
Orders: 3,500 / 5,000
70%
throughput: 42,000 records/sec
```

Progress reporting should not require storing every generated record.

---

# 35. Throughput

Generation throughput should be measured.

Examples:

```text
customers: 120k records/sec
events: 500k records/sec
CSV output: 80 MB/sec
```

These measurements can reveal bottlenecks.

However, throughput should be treated as operational telemetry, not business correctness.

---

# 36. Phase Timing

The generator should eventually report time spent in major phases:

```text
configuration
reference-data loading
planning
customer generation
product generation
order generation
validation
statistics
output
manifest
```

This supports optimization.

For example:

```text
generation = 40s
validation = 8s
statistics = 4s
output = 30s
```

This immediately shows that optimizing random generation may not improve total runtime if output dominates.

---

# 37. Entity-Level Timing

Entity generation timing is also useful.

Example:

```text
customers     0.8s
products      0.5s
orders        3.2s
order_items   4.7s
events        7.8s
```

This can identify domains that require optimization.

---

# 38. Validation Timing

Validation may eventually become expensive at large scale.

It should therefore have separate timing.

For example:

```text
FK validation: 2.3s
business rules: 4.8s
statistical validation: 8.1s
```

This supports decisions about:

- batch validation;
- streaming validation;
- sampled validation;
- scalable indexes.

---

# 39. Output Timing

Output time should be measured separately from generation.

This distinction matters because:

```text
generation CPU time
```

and:

```text
storage I/O time
```

are different bottlenecks.

The generator should eventually make both observable.

---

# 40. Memory Observability

Memory usage becomes increasingly important as datasets grow.

Useful metrics may include:

```text
heap used
heap committed
peak heap
reference-data size
in-memory entity counts
```

The first implementation does not need a complex monitoring subsystem.

Simple JVM/process metrics can be sufficient.

---

# 41. Error Reporting

Failures should contain enough context to diagnose them.

Bad:

```text
Generation failed.
```

Better:

```text
Generation failed while generating order items:
order_id=ORDER_00001234
reason=product distribution returned an invalid product reference
```

Error messages should include business identifiers where useful.

---

# 42. Fail-Fast vs Partial Output

The generator should define what happens when a critical error occurs.

For correctness-sensitive generation:

```text
critical failure
    ↓
generation fails
```

Partial output should not automatically be presented as a valid dataset.

If partial output is retained for debugging, it should be clearly marked as incomplete.

---

# 43. Atomic Output

A useful future design is:

```text
temporary run directory
        ↓
generation
        ↓
validation
        ↓
manifest
        ↓
successful completion
        ↓
publish/finalize
```

This avoids exposing an incomplete dataset as though it were complete.

Conceptually:

```text
runs/.tmp/<run-id>
```

becomes:

```text
runs/<run-id>
```

only after successful completion.

The exact filesystem implementation can evolve later.

---

# 44. Output Writer Responsibility

An output writer should own:

- serialization;
- file creation;
- escaping;
- delimiters;
- encoding;
- row writing;
- output finalization.

It should not own:

- business validation;
- scenario selection;
- customer behavior;
- distribution decisions.

---

# 45. Writer Abstraction

A small output abstraction may eventually be useful.

Conceptually:

```scala
trait DatasetWriter {
  def write(data: GeneratedData, destination: Path): OutputSummary
}
```

Potential implementations:

```text
CsvDatasetWriter
ParquetDatasetWriter
JsonDatasetWriter
```

This is a legitimate Strategy boundary because file formats are genuinely interchangeable.

However, the abstraction should remain small.

---

# 46. Avoiding Format Leakage

Domain models should not contain CSV-specific logic.

Bad:

```scala
case class Customer(...) {
  def toCsvRow: Seq[String] = ...
}
```

This couples business models to one output format.

Prefer:

```text
Customer
   ↓
CustomerCsvEncoder
```

or an equivalent writer-owned mapping.

This keeps domain models storage-independent.

---

# 47. Schema Ownership

Schema meaning belongs to the domain.

Serialization belongs to output.

For example:

```text
Customer domain
  → defines customer fields and semantics

CSV writer
  → decides how those fields are serialized
```

This distinction becomes important when Parquet is introduced.

---

# 48. CSV Concerns

The CSV writer must correctly handle:

- commas;
- quotes;
- line breaks;
- empty strings;
- nulls;
- encoding;
- numeric formatting.

It should use one well-tested CSV-writing mechanism rather than repeatedly implementing escaping manually.

---

# 49. Numeric Formatting

Numeric output must be deterministic.

Financial values should use a defined representation.

For example:

```text
805.92
```

rather than:

```text
8.0592E2
```

The exact precision and rounding policy should be documented by the relevant domain.

The output layer should serialize according to that contract.

---

# 50. Date and Time Formatting

Dates and timestamps should use a consistent representation.

The target design should prefer machine-readable forms such as:

```text
2026-09-15T18:30:00
```

or a documented UTC representation where appropriate.

The important requirement is consistency.

Time semantics belong to the domain/time architecture, not the writer.

---

# 51. Null Representation

The output contract should explicitly define how null values are represented.

This is particularly important for dirty-data scenarios.

Possible representations include:

```text
empty field
```

or:

```text
NULL
```

The generator should choose one documented representation rather than allowing writers to vary.

---

# 52. File Naming

File names should be stable and predictable.

Current examples:

```text
customers.csv
order_items.csv
```

Naming should follow a consistent convention.

Avoid names that depend on implementation class names.

---

# 53. Partitioned Output

Future large-scale generation may support partitioned output.

Example:

```text
events/
    date=2026-09-14/
    date=2026-09-15/
```

or:

```text
orders/
    year=2026/
    month=09/
```

Partitioning should be an output/storage concern.

It must not change domain semantics.

---

# 54. File Count as a Workload Dimension

For Spark experiments, file count can matter.

A workload may intentionally use:

```text
1 large file
```

versus:

```text
10,000 small files
```

This affects:

- file listing;
- task scheduling;
- metadata overhead;
- input parallelism.

Therefore file-layout scenarios may eventually become part of workload configuration.

---

# 55. Small Files Scenario

A future workload profile may intentionally generate many output files.

This is a storage/workload scenario, not a domain behavior.

For example:

```text
events/
    part-00000.csv
    part-00001.csv
    ...
```

The domain data remains unchanged.

Only physical representation changes.

---

# 56. Compression

Future formats may support compression.

Examples:

```text
gzip
snappy
zstd
```

Compression is a storage concern.

The generator architecture should allow it without modifying domain generators.

---

# 57. CSV vs Parquet

CSV is useful initially because it is:

- human-readable;
- easy to inspect;
- easy to generate;
- universally supported.

Parquet will eventually be important for Spark workloads because it provides:

- columnar storage;
- compression;
- predicate pushdown;
- schema information;
- efficient analytical scans.

The architecture should therefore avoid coupling the entire system to CSV.

---

# 58. Output Strategy

The target architecture can treat output format as a strategy.

Conceptually:

```scala
val writer =
  DatasetWriterFactory.create(outputConfig.format)

writer.write(dataset, destination)
```

The factory chooses the format.

The domain does not know which format was selected.

---

# 59. Manifest Format

The manifest should preferably be machine-readable.

JSON is a natural initial choice.

Possible future formats:

- JSON;
- YAML;
- Parquet metadata;
- compact text summary.

The canonical machine-readable manifest should remain stable even if a human-readable report is added.

---

# 60. Human-Readable Summary

In addition to the machine-readable manifest, a concise human-readable summary can be useful.

Example:

```text
ShopSphere Dataset
------------------
Profile       : medium
Scenario      : baseline
Seed          : 42

Records
-------
Customers     : 1,000
Orders        : 5,000
Order Items   : 12,000
Events        : 24,000

Validation
----------
Structural    : PASS
Primary Key   : PASS
Foreign Key   : PASS
Business Rule : PASS
```

This is especially useful for local development.

---

# 61. Manifest Serialization Boundary

The manifest should have a domain-neutral metadata model.

Conceptually:

```text
GenerationManifest
    |
    +-- DatasetMetadata
    +-- RunMetadata
    +-- GenerationMetadata
    +-- ReferenceDataMetadata
    +-- EntityOutputMetadata
    +-- StatisticsSummary
    +-- ValidationSummary
```

Serialization should be separate.

This prevents the manifest model from becoming tied to one serialization library.

---

# 62. Jackson

Jackson is already part of the project's dependency set.

It is a reasonable technology for serializing structured manifest models.

However:

> Jackson should be an infrastructure dependency, not a domain dependency.

Domain models should not require Jackson annotations merely to function.

---

# 63. Manifest Evolution

Manifest schemas will evolve.

For example:

```text
manifest version 1
manifest version 2
```

A schema version should therefore be recorded.

This is particularly important when benchmark datasets remain archived for long periods.

---

# 64. Backward Compatibility

The project should avoid unnecessary breaking changes to the manifest.

If a field is renamed or removed:

- increment schema version where appropriate;
- document the change;
- provide migration/compatibility handling if consumers require it.

---

# 65. Observability vs Statistics

The distinction is:

```text
Observability
  = what happened during execution

Statistics
  = what the generated dataset looks like
```

Examples:

```text
throughput → observability
generation duration → observability

AOV → statistics
Gini coefficient → statistics
items/order → statistics
```

Some metrics may appear in both execution logs and final manifest, but ownership remains distinct.

---

# 66. Statistics Collection Architecture

Statistics should preferably be calculated through focused components.

Conceptually:

```text
CustomerStatistics
ProductStatistics
OrderStatistics
SessionStatistics
EventStatistics
GlobalStatistics
```

A statistics coordinator can aggregate results.

Avoid one enormous statistics class containing every calculation.

---

# 67. Streaming Statistics

At large scale, statistics should avoid requiring all records to remain in memory.

Useful algorithms can calculate:

- counts;
- sums;
- means;
- approximate percentiles;
- frequency summaries.

Some exact metrics may require indexes or multiple passes.

The architecture should allow both exact and approximate statistics where appropriate.

---

# 68. Statistics Accuracy

Every statistic should define its accuracy semantics.

For example:

```text
record count = exact
total revenue = exact
mean = exact
p99 = exact or approximate
Gini = exact or approximate
```

A benchmark report should not silently treat an approximation as exact.

---

# 69. Sampling

For very large datasets, observability or exploratory statistics may use sampling.

However, sample-based statistics must be labeled as such.

For example:

```text
product popularity statistics
sample size = 1,000,000
```

This avoids misleading conclusions.

---

# 70. Scenario Statistics

Scenario statistics should focus on scenario-relevant measures.

For `hot_product`:

```text
top-product share
Gini
max/median
```

For `holiday_peak`:

```text
peak-day traffic
baseline-day traffic
peak multiplier
```

For `dirty_data`:

```text
configured defect rate
observed defect rate
unexpected violations
```

This makes scenarios testable.

---

# 71. Benchmark Metadata

A future Spark benchmark should be able to record:

```text
dataset_id
scenario
dataset_size
row_counts
file_size
file_count
skew metrics
quality profile
seed
generator version
```

This makes performance results interpretable.

For example:

```text
Benchmark A
Dataset: shopsphere-large-hot-product
Top-product share: 22%
Events: 240M
Files: 1,024
```

A runtime without this context is difficult to interpret.

---

# 72. Observability Events

The implementation may eventually model internal lifecycle events such as:

```text
GenerationStarted
EntityGenerationStarted
EntityGenerationCompleted
ValidationStarted
ValidationCompleted
OutputStarted
OutputCompleted
GenerationCompleted
GenerationFailed
```

These events can support logging and metrics without coupling business logic to a logging framework.

This is optional infrastructure, not a requirement for the first implementation.

---

# 73. Avoiding Logging Coupling

Domain code should not be filled with logging statements.

Prefer observability at orchestration boundaries.

For example:

```scala
logger.info("Generating customers")
customerGenerator.generate(...)
logger.info("Customers generated")
```

rather than logging every internal operation.

This keeps domain code readable.

---

# 74. Metrics Abstraction

If metrics become necessary, a small abstraction can isolate the generator from a specific metrics system.

Conceptually:

```scala
trait MetricsRecorder {
  def increment(name: String, value: Long): Unit
  def observe(name: String, value: Double): Unit
}
```

But this should only be introduced if multiple metric backends or testing boundaries justify it.

Do not create abstractions prematurely.

---

# 75. Timing Utility

Timing is sufficiently cross-cutting that a small timing utility may be appropriate.

It should produce:

```text
operation
duration
```

and optionally emit metrics/logging.

It should not know anything about customers, products, or orders.

---

# 76. SOLID Application

## Single Responsibility

Output writer:

```text
write data
```

Manifest builder:

```text
assemble metadata
```

Manifest serializer:

```text
serialize manifest
```

Statistics calculator:

```text
calculate statistics
```

Observability:

```text
report execution telemetry
```

These responsibilities should remain distinct.

## Open/Closed

Adding:

```text
ParquetDatasetWriter
```

should not require modifying customer generation.

## Liskov Substitution

Alternative writers should satisfy the output contract.

## Interface Segregation

Do not create one giant infrastructure interface containing:

```text
write
validate
measure
log
serialize
```

## Dependency Inversion

Generation orchestration may depend on small abstractions for:

- output;
- metrics;
- clock;
- manifest serialization,

where this provides real value.

---

# 77. Strategy Pattern

Output format is a strong Strategy use case.

Example:

```text
CSV writer
Parquet writer
JSON writer
```

Statistics algorithms may also be strategies where exact and approximate implementations genuinely need to be interchangeable.

Do not create a strategy interface for every writer helper.

---

# 78. Factory Pattern

A factory can map:

```text
format = csv
```

to:

```text
CsvDatasetWriter
```

and:

```text
format = parquet
```

to:

```text
ParquetDatasetWriter
```

Similarly, a manifest serializer factory could later support multiple serialization formats if needed.

---

# 79. Dependency Injection

The application layer should construct infrastructure dependencies and pass them explicitly.

Conceptually:

```scala
val writer = DatasetWriterFactory.create(outputConfig)
val statistics = StatisticsCoordinator(...)
val manifest = ManifestBuilder(...)

generationService.run(
  writer,
  statistics,
  manifest
)
```

The exact implementation may differ.

The important principle is explicit dependencies.

---

# 80. Domain Independence

The following should remain independent of output format:

```text
Customer
Product
Order
OrderItem
Payment
Shipment
Return
Session
Event
```

They should not contain:

```text
toCsv
writeCsv
writeJson
writeParquet
```

methods.

---

# 81. Output Validation

After writing, the system should eventually be able to verify:

- expected files exist;
- record counts match;
- file sizes are non-zero where expected;
- schema headers are correct;
- checksums are calculated if enabled;
- manifest references valid files.

This is different from domain validation.

It validates the generated artifact.

---

# 82. Artifact Validation

There are therefore two distinct validation layers:

```text
Data validation
    ↓
Are the records correct?

Artifact validation
    ↓
Was the dataset written correctly?
```

Both are important.

A perfectly valid in-memory dataset can still result in:

- truncated output;
- missing files;
- incorrect headers;
- partial writes.

---

# 83. Output Transaction Boundary

The finalization process should ideally be:

```text
prepare output
    ↓
write all entity files
    ↓
calculate statistics
    ↓
validate data
    ↓
validate artifacts
    ↓
write manifest
    ↓
publish completed dataset
```

The precise ordering may evolve.

The important point is that the final dataset should not be advertised as complete before completion criteria are satisfied.

---

# 84. Failed Run Metadata

If a generation run fails, useful metadata may still be retained.

For example:

```text
run_id
failure phase
error message
completed entities
elapsed time
configuration fingerprint
seed
```

This is useful for diagnosing large-scale generation failures.

Failed artifacts should be clearly marked and never confused with successful datasets.

---

# 85. Reproducibility Report

A future command may provide:

```text
Dataset reproducibility
-----------------------
Seed                  : 42
Generator version     : 0.1.0
Config fingerprint    : ...
Reference fingerprint : ...
Scenario              : baseline
Manifest fingerprint  : ...
```

This can make reproducing benchmark data straightforward.

---

# 86. Dataset Comparison

The architecture should eventually support comparing two generated datasets.

Useful comparison dimensions:

```text
record counts
distribution summaries
skew statistics
validation results
file sizes
generation time
scenario metadata
```

Example:

```text
Dataset A → baseline
Dataset B → hot_product

Difference:
  top-product share: 3% → 21%
  order count: unchanged
  validation: PASS
```

This is valuable for scenario development.

---

# 87. Regression Detection

Statistics can also detect unintended changes.

Suppose a code change causes:

```text
median AOV:
144K → 260K
```

without an intentional pricing-model change.

A statistical regression test should be able to flag this.

This is one reason the manifest and statistics architecture matters.

---

# 88. Realism Regression

The same mechanism can detect realism regressions.

Examples:

```text
items/order becomes almost constant
product popularity becomes uniform
customer activity loses heterogeneity
return rate collapses
```

These may not violate structural validation.

They are nevertheless important generator regressions.

---

# 89. Benchmark Stability

Spark benchmark datasets should ideally remain stable enough to compare changes.

This does not mean every generator change must preserve byte-identical output forever.

Instead, benchmark datasets should be versioned.

For example:

```text
shopsphere-large-hot-product-v1
shopsphere-large-hot-product-v2
```

Changes should be intentional and documented.

---

# 90. Current Implementation vs Target

The current project already has:

- CSV output;
- entity-specific output files;
- output configuration;
- generation statistics;
- validation;
- deterministic generation;
- manifest-related architecture.

The target design expands this into:

- formal dataset identity;
- run identity;
- effective configuration fingerprint;
- reference-data fingerprint;
- schema/version metadata;
- output checksums;
- artifact validation;
- richer statistics;
- execution timing;
- workload metadata;
- scenario metadata;
- benchmark reproducibility.

These are architecture targets, not claims about current implementation.

---

# 91. Current Baseline

The current baseline successfully produces entity files and reports generation/validation information.

A typical run records counts such as:

```text
customers    1,000
addresses    1,200
products       500
orders       5,000
order_items 12,000
payments     5,000
shipments    5,000
returns        239
sessions     3,000
events      24,000
```

The current validation reports:

```text
Structural PASS
Primary-key PASS
Foreign-key PASS
Business-rule PASS
```

These are useful foundation capabilities.

---

# 92. Current Limitations

The output/observability layer is not yet a complete benchmark artifact system.

Missing or immature areas include:

- richer manifest schema;
- output checksums;
- formal dataset fingerprinting;
- reference-data fingerprinting;
- artifact validation;
- detailed phase timing;
- workload metadata;
- scenario target reporting;
- statistical regression baselines;
- multiple output formats;
- partitioned output;
- large-scale streaming output.

These should be introduced incrementally.

---

# 93. Recommended Implementation Sequence

Do not implement everything in this document at once.

Recommended order:

```text
1. stabilize current CSV writer
        ↓
2. formalize output schema ownership
        ↓
3. strengthen manifest model
        ↓
4. add actual-vs-requested counts
        ↓
5. add output metadata
        ↓
6. add configuration/reference fingerprints
        ↓
7. add artifact validation
        ↓
8. improve statistics summaries
        ↓
9. add generation phase timing
        ↓
10. add scenario/skew metadata
        ↓
11. add benchmark dataset identity
        ↓
12. introduce alternative formats when needed
```

This avoids premature infrastructure complexity.

---

# 94. Output Architecture

The target package structure may resemble:

```text
output/
├── DatasetWriter.scala
├── CsvDatasetWriter.scala
├── DatasetWriterFactory.scala
├── OutputSummary.scala
├── OutputArtifact.scala
└── CsvWriter.scala

manifest/
├── GenerationManifest.scala
├── ManifestBuilder.scala
├── ManifestSerializer.scala
└── JsonManifestSerializer.scala

statistics/
├── StatisticsCoordinator.scala
├── DatasetStatistics.scala
├── EntityStatistics.scala
└── distribution/

observability/
├── GenerationMetrics.scala
├── MetricsRecorder.scala
├── GenerationTimer.scala
└── Logging...
```

The exact names can evolve.

Do not create empty classes merely to match the diagram.

---

# 95. Application Orchestration

The generation application should eventually read approximately like:

```scala
val configuration = configLoader.load()
val context = contextBuilder.build(configuration)

val dataset = generationService.generate(context)

val statistics = statisticsService.calculate(dataset)

validationService.validate(dataset, context)

val output = datasetWriter.write(dataset, outputPath)

val manifest =
  manifestBuilder.build(
    context,
    output,
    statistics,
    validation
  )

manifestWriter.write(manifest, outputPath)
```

This is conceptual.

The important property is readability of the business workflow.

---

# 96. Avoiding a Giant Generation Service

The orchestration layer should coordinate.

It should not contain:

```text
customer generation logic
product selection logic
CSV escaping
SHA-256 implementation
Gini calculation
validation rules
```

Those belong elsewhere.

The orchestration layer should read like a workflow.

---

# 97. Storage Independence

The business model should remain independent of physical storage.

Future storage targets may include:

```text
local filesystem
object storage
distributed filesystem
```

The first implementation only needs local filesystem support.

Storage abstraction should be introduced when there is a real requirement.

---

# 98. Spark Independence

The generator should not depend on Spark merely because its output will later be consumed by Spark.

This is an important boundary.

The generator should produce well-defined datasets.

Spark jobs should own:

- ingestion;
- transformation;
- partitioning;
- caching;
- joins;
- aggregations;
- performance measurements.

This keeps the data-generation project focused.

---

# 99. Workload Metadata Without Spark Coupling

The generator can still record:

```text
intended workload = join_skew
```

without importing Spark classes.

This metadata belongs to the dataset contract, not the execution engine.

---

# 100. Manifest and Spark Lab Integration

A future Spark lab can read the manifest to determine:

```text
dataset path
row counts
scenario
seed
skew metrics
quality profile
```

This avoids hard-coding assumptions in every benchmark.

---

# 101. Definition of Done

The output/storage/manifest/observability architecture is complete when:

### Output

- entity files have stable schemas;
- serialization is independent of domain models;
- output format can evolve independently;
- large outputs can be written safely.

### Manifest

- dataset identity is recorded;
- run identity is recorded;
- configuration identity is recorded;
- reference-data identity is recorded;
- output artifacts are recorded;
- validation summary is recorded;
- statistics summary is recorded.

### Statistics

- actual row counts are recorded;
- important distributions are summarized;
- skew metrics are available;
- scenario metrics are available.

### Observability

- phase durations are measurable;
- major failures are diagnosable;
- progress is visible for large generation.

### Reproducibility

- seed is recorded;
- generator version is recorded;
- configuration fingerprint is recorded;
- reference-data fingerprint is recorded.

### Artifact integrity

- expected files are verified;
- incomplete runs are distinguishable;
- checksums can be enabled.

---

# 102. Quality Gate Before Implementation

Before changing the output architecture, answer:

1. What is the exact output contract?
2. Which fields belong to the domain schema?
3. Which concerns belong only to serialization?
4. What identifies a dataset?
5. What identifies a run?
6. What metadata is required to reproduce it?
7. Which statistics are exact?
8. Which statistics may be approximate?
9. What makes a generated artifact complete?
10. How are failed runs represented?
11. Which output formats are actually needed now?
12. What information must Spark benchmarks consume?

If these questions are unanswered, implementation should wait.

---

# 103. Explicit Architecture Decisions

### Decision 1

Output is a first-class dataset contract, not an incidental side effect.

### Decision 2

Domain models remain independent of storage formats.

### Decision 3

Entity-specific files are the initial default.

### Decision 4

CSV remains the initial format because it is simple and inspectable.

### Decision 5

Alternative formats should be added through output strategies when there is a real requirement.

### Decision 6

Manifest generation is separate from validation and statistics calculation.

### Decision 7

The manifest records actual output facts rather than merely requested generation parameters.

### Decision 8

Effective configuration, seed, generator version, and reference-data identity form the core reproducibility metadata.

### Decision 9

Artifact validation is distinct from domain-data validation.

### Decision 10

Observability measures execution; statistics describe the resulting dataset.

### Decision 11

Benchmark datasets should be explicitly identifiable by scenario and version.

### Decision 12

The generator remains independent of Spark even though Spark is a primary downstream consumer.

---

# 104. Final Design Summary

The target output architecture is:

```text
                 Generated Domain Data
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          Writer     Statistics   Validation
             |           |           |
             v           |           |
       Output Artifacts  |           |
             |           +-----+-----+
             |                 |
             +--------+--------+
                      |
                      v
                   Manifest
                      |
                      v
              Completed Dataset
                      |
                      v
                Spark Workloads
```

The key separation is:

```text
Domain generation
    = what data means

Output
    = how data is stored

Statistics
    = what the generated data looks like

Validation
    = whether it satisfies its rules

Manifest
    = what dataset was produced and how

Observability
    = what happened during generation
```

A mature ShopSphere dataset should therefore be more than a set of CSV files.

It should be a reproducible, measurable artifact with:

```text
identity
+ provenance
+ schema
+ records
+ statistics
+ validation
+ scenario metadata
+ output metadata
```

That artifact can then serve as a reliable input to the future Spark Performance Laboratory.

The design goal is not to build an elaborate metadata platform. The goal is to make every generated dataset **understandable, reproducible, testable, and benchmarkable** without coupling the business domain to infrastructure.
