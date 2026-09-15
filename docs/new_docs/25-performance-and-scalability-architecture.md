# 25 — Performance & Scalability Architecture

## 1. Purpose

ShopSphere is a synthetic-data generator whose eventual purpose is broader than producing a small demonstration dataset.

It must be capable of producing progressively larger datasets for downstream Spark performance experiments.

Therefore performance and scalability are architectural concerns, not late-stage optimizations.

The generator should support a progression such as:

```text
small
→ medium
→ large
→ xlarge
```

without requiring a fundamentally different application.

The primary objective is:

> **Scale the amount of generated data while preserving business semantics, reproducibility, configurability, and acceptable generation performance.**

---

# 2. Performance Is Not the Same as Maximum Speed

The fastest generator is not necessarily the best generator.

A generator that produces:

```text
1 billion unrealistic rows
```

very quickly is less useful than one that produces:

```text
100 million coherent, reproducible, configurable rows
```

at an acceptable rate.

The optimization hierarchy should therefore be:

```text
correctness
→ realism
→ reproducibility
→ scalability
→ raw throughput
```

Performance work must not silently weaken the business model.

---

# 3. Scalability Dimensions

ShopSphere has several independent scalability dimensions.

### Record scale

```text
customers
orders
order items
events
```

### Catalog scale

```text
products
categories
brands
```

### Relationship scale

```text
customer → orders
order → items
session → events
```

### Temporal scale

```text
days
months
years
```

### Scenario complexity

```text
baseline
→ seasonality
→ campaigns
→ skew
→ dirty data
```

### Output scale

```text
file count
file size
total bytes
```

A design that scales one dimension but fails another is not truly scalable.

---

# 4. Current Baseline

The current working baseline generates approximately:

```text
Customers:   1,000
Addresses:   1,200
Products:      500
Orders:      5,000
OrderItems: 12,000
Payments:    5,000
Shipments:   5,000
Returns:       239
Sessions:    3,000
Events:     24,000
```

This is useful for development and integration testing.

It is not yet representative of the scale required for Spark performance experimentation.

---

# 5. Target Scale Philosophy

The project should not hard-code one enormous target.

Instead, scale should be configuration-driven.

Conceptually:

```text
small
medium
large
xlarge
```

can define:

```text
customer count
order count
catalog size
event volume
```

while preserving the same business relationships.

---

# 6. Cardinality Is a Business Model

Scale should not simply mean:

```scala
orders = customers * 1000
```

The relationships should remain meaningful.

For example:

```text
customers
    ↓
orders/customer distribution
    ↓
orders
    ↓
items/order distribution
    ↓
order items
```

This produces scale through the business model.

---

# 7. Multiplication Factors

A profile can use scaling factors where appropriate.

For example:

```text
small  = 1x
medium = 10x
large  = 100x
xlarge = 1000x
```

These are conceptual.

Actual profile values should be chosen based on:

- execution time;
- memory;
- output size;
- Spark workload requirements;
- realistic business ratios.

---

# 8. Scale Profiles vs Behavior Profiles

Do not confuse:

```text
more records
```

with:

```text
different customer behavior
```

A large dataset should generally preserve the same baseline behavior distribution unless the profile explicitly changes behavior.

This allows fair benchmark comparisons.

---

# 9. Benchmark Dataset Families

The project should eventually support dataset families such as:

```text
baseline-small
baseline-medium
baseline-large
baseline-xlarge
```

and:

```text
skewed-large
seasonal-large
dirty-large
hot-product-large
```

This makes performance experiments reproducible.

---

# 10. Dataset Identity

A benchmark dataset should be identified by more than row count.

Useful identity components include:

```text
dataset version
configuration fingerprint
scenario
seed
reference-data fingerprint
temporal configuration
```

This prevents two datasets with identical cardinalities from being incorrectly treated as equivalent.

---

# 11. Throughput

Generation throughput should be measured.

Useful metrics:

```text
records/sec
entities/sec
MB/sec
rows/sec by entity
```

For example:

```text
customers: 500k records/sec
events:    1.2M records/sec
```

Exact performance depends heavily on hardware and output format.

---

# 12. End-to-End Throughput

The most useful operational metric is often:

```text
dataset records / total elapsed generation time
```

but this should be accompanied by phase metrics.

Otherwise a fast generator with extremely slow output may appear misleadingly efficient.

---

# 13. Phase Timing

The generator should eventually report:

```text
configuration load
reference-data load
planning
customer generation
product generation
order generation
event generation
validation
statistics
output
manifest
```

This identifies the real bottleneck.

---

# 14. Throughput by Entity

Different entities have different computational costs.

For example:

```text
Category
Brand
Product
```

may be relatively cheap.

Whereas:

```text
Event
OrderItem
```

can become very large.

Therefore performance metrics should be broken down by entity.

---

# 15. Memory Is a First-Class Constraint

The largest scalability risk in the current architecture is unnecessary materialization.

A design like:

```scala
val allEvents: Seq[Event] = ...
```

can become dangerous at xlarge scale.

If:

```text
events = 1 billion
```

then holding all events in memory is generally inappropriate.

---

# 16. Streaming Generation

The long-term architecture should favor:

```text
generate
→ consume
→ write
```

rather than:

```text
generate everything
→ store everything
→ write everything
```

This is especially important for:

```text
events
order items
orders
```

at large scale.

---

# 17. Lazy Iteration

Scala's collection abstractions can support incremental generation.

Conceptually:

```scala
Iterator
```

is preferable to eagerly materializing millions of records where the pipeline allows it.

Example:

```scala
val customers: Iterator[Customer] =
  customerGenerator.generate(...)
```

Then:

```text
customer iterator
→ writer
```

can consume records incrementally.

---

# 18. Avoid Unnecessary Collections

Avoid patterns such as:

```scala
(1 to 1000000000).map(generate).toVector
```

when the data only needs to be streamed.

Prefer:

```scala
(1 to count).iterator.map(generate)
```

when downstream APIs support incremental consumption.

---

# 19. Collection Choice

When collections are genuinely needed:

```text
Vector
Array
HashMap
```

should be chosen based on access patterns.

Do not use a collection merely because it is familiar.

Reference data may benefit from:

```text
Map[Id, Entity]
```

while output streams should generally remain iterative.

---

# 20. Reference Data vs Transaction Data

Reference data is relatively small and stable.

Examples:

```text
countries
states
cities
areas
brands
categories
products
```

Transaction/event data can become enormous.

Examples:

```text
orders
order items
sessions
events
```

The memory strategy should differ accordingly.

---

# 21. Reference Data Indexing

Reference data should be indexed for efficient lookup.

For example:

```text
productById
brandById
categoryById
buildingById
```

This avoids repeated linear searches.

A lookup:

```text
O(1) average
```

is generally preferable to:

```text
O(n)
```

for every transaction.

---

# 22. Immutable Reference Data

Reference data should preferably be immutable after loading.

Benefits:

- thread safety;
- reproducibility;
- simpler reasoning;
- safer parallelism.

---

# 23. Sorting Reference Data

Deterministic ordering is important.

Where a map is iterated for random selection, explicitly sort or use a deterministic indexed structure.

Avoid relying on unspecified map iteration order.

This is especially important for reproducibility.

---

# 24. Relationship Lookup Performance

Relationship generation can become expensive if every child performs repeated searches.

Bad:

```text
for every OrderItem:
    scan all Orders
```

Preferred:

```text
orderById
```

or a direct parent reference.

---

# 25. Customer Lookup

For large order generation:

```text
customerId → CustomerBehaviorProfile
```

should be available efficiently.

The generator should not repeatedly reconstruct customer behavior from raw customer attributes if that can be precomputed.

---

# 26. Product Selection

Product selection is likely to become one of the major computational hot spots.

A realistic popularity model may require:

```text
weighted selection
category conditioning
brand conditioning
customer affinity
temporal context
```

Naive repeated filtering can become expensive.

---

# 27. Weighted Selection

If product selection uses weights, avoid rebuilding cumulative structures for every record.

Preferred:

```text
precompute selection structure
```

then sample repeatedly.

Potential structures include:

```text
cumulative weights
alias method
indexed probability table
```

depending on scale and dynamic behavior.

---

# 28. Conditional Selection

A customer may first choose:

```text
category
```

then:

```text
brand
```

then:

```text
product
```

This hierarchical selection can reduce the size of each sampling operation.

It can also naturally represent business relationships.

---

# 29. Dynamic Popularity

If popularity changes by:

```text
time
campaign
customer segment
```

a single static selection table may no longer be sufficient.

The architecture should support cached or bucketed selection structures.

For example:

```text
time bucket
+
category
+
segment
→ selection model
```

Only where complexity is justified.

---

# 30. Performance vs Realism

There is an important tradeoff:

```text
more realistic model
→ more computation
```

The architecture should not eliminate useful behavior simply because it costs CPU.

Instead:

```text
identify expensive behavior
→ measure it
→ optimize implementation
```

before simplifying the business model.

---

# 31. Avoid Premature Optimization

Do not optimize based on assumptions.

First measure:

```text
where CPU time is spent
where memory grows
where I/O dominates
```

Then optimize the actual bottleneck.

---

# 32. Profiling

The project should eventually support profiling at realistic scales.

Useful tools include JVM profilers and benchmark frameworks.

The exact tooling is an implementation decision.

The architectural requirement is simply:

> Performance decisions should be evidence-driven.

---

# 33. Allocation Pressure

Synthetic generation can create enormous numbers of short-lived JVM objects.

For example:

```text
Event
Seq[String]
CSV row
temporary timestamp
```

per record.

At large scale, object allocation and garbage collection can become a dominant cost.

---

# 34. Reduce Temporary Objects

Prefer direct record construction where possible.

Avoid unnecessary transformations:

```text
record
→ many intermediate collections
→ another object
→ CSV row
```

A clear pipeline is good, but excessive allocation should be measured and reduced when it becomes significant.

---

# 35. Strings

String construction can be surprisingly expensive.

IDs such as:

```text
ORDER_000000001
```

are necessary output values.

But avoid repeatedly constructing equivalent derived strings internally if the value can be retained or computed once.

---

# 36. Timestamp Allocation

Repeated conversion between:

```text
Instant
LocalDateTime
String
```

can be expensive.

Keep temporal values in an appropriate typed representation internally and serialize them at the output boundary.

---

# 37. Serialization

CSV serialization should be treated as a distinct performance boundary.

The generator should not know how CSV quoting or escaping works.

Conceptually:

```text
domain record
→ output adapter
→ serialized row
```

This preserves separation and makes future formats possible.

---

# 38. Output Bottleneck

At sufficiently large scale, the bottleneck may move from CPU to disk.

For example:

```text
generation = 500 MB/s
disk = 150 MB/s
```

Then optimizing customer behavior may have almost no end-to-end effect.

The phase timing architecture should make this visible.

---

# 39. Buffered Output

Large output should use buffered I/O.

Writing one filesystem operation per field or record would be inefficient.

The output layer should control buffering.

---

# 40. Compression

Future output formats may support compression.

Compression introduces a tradeoff:

```text
CPU
vs
I/O
```

For benchmark dataset creation, compression can be useful when storage is the bottleneck.

The generator should expose format/compression as output configuration rather than embedding it in domain logic.

---

# 41. File Size

Large datasets should be divided into manageable files.

A future configuration might control:

```text
target records per file
target bytes per file
```

The exact policy should be determined by downstream Spark workload requirements.

---

# 42. File Count

Too many small files create a downstream Spark problem.

Too few enormous files can reduce parallelism.

Therefore file sizing should be designed with future Spark workloads in mind.

---

# 43. Output Partitioning

The generator should eventually support logical output partitioning such as:

```text
entity
+
time bucket
```

or:

```text
entity
+
shard
```

but this should remain an output concern.

The domain generator should produce records independently of physical file layout.

---

# 44. Sharding

For very large datasets, generation can be partitioned into deterministic shards.

Example:

```text
customers-00000.csv
customers-00001.csv
...
```

A shard should have a stable identity.

---

# 45. Deterministic Shards

A shard should derive its random stream from:

```text
root seed
+
entity
+
shard id
```

This allows:

```text
shard 7
```

to be regenerated independently.

---

# 46. Parallel Generation

Parallelism is a major scalability opportunity.

Potential structure:

```text
shard 0 → worker 0
shard 1 → worker 1
shard 2 → worker 2
...
```

Each worker should have isolated deterministic random streams.

---

# 47. Parallelism and Business Relationships

Parallelism is harder for entities with dependencies.

For example:

```text
Products
→ OrderItems
```

requires product reference data to exist before order-item generation.

Likewise:

```text
Customers
→ Orders
```

requires customer behavioral context.

The generation dependency graph must remain intact.

---

# 48. Dependency-Aware Parallelism

Independent domains can potentially run in parallel.

For example:

```text
Categories ─┐
            ├→ Products
Brands ─────┘

Geography → Addresses

Customers
```

But:

```text
Products + Customers
→ Orders/OrderItems
```

must respect dependencies.

---

# 49. DAG-Based Generation

The generation architecture can be represented as a DAG:

```text
Reference Data
   |
   +---- Categories
   |
   +---- Brands
   |
   +---- Geography
   |
   +---- Products
   |
   +---- Customers
            |
            +---- Addresses
            |
            +---- Sessions
                    |
                    +---- Events

Customers + Products
            |
            +---- Orders
                    |
                    +---- OrderItems
                    |
                    +---- Payments
                    |
                    +---- Shipments
                            |
                            +---- Returns
```

This is conceptual; the exact dependencies will evolve.

---

# 50. Application Orchestration

The application orchestration layer should own dependency sequencing.

Individual entity generators should not coordinate the entire dataset.

This prevents generators from becoming tightly coupled.

---

# 51. Batch Size

Even with streaming, some operations benefit from batching.

Examples:

```text
write 10,000 rows
flush
```

rather than:

```text
flush after every row
```

Batch size should be configurable or chosen by infrastructure defaults.

---

# 52. Bounded Memory

A scalable pipeline should have predictable memory behavior.

For a fixed configuration, memory should ideally remain approximately bounded as record count increases.

For example:

```text
1M events → X GB
10M events → 10X GB
```

would be a warning sign if events are expected to stream.

The preferred shape is closer to:

```text
1M events → stable working memory
10M events → stable working memory
```

apart from necessary reference structures.

---

# 53. Streaming Statistics

Statistics must also avoid forcing full materialization.

Instead of:

```text
allOrders.toSeq
→ calculate statistics
```

prefer streaming aggregators.

Examples:

```text
count
sum
min
max
mean
online variance
histogram
quantile sketch
```

where appropriate.

---

# 54. Quantiles at Scale

Exact percentiles may require storing large amounts of data.

For large datasets, approximate algorithms may eventually be useful.

Examples:

```text
t-digest
KLL sketch
```

The exact implementation should be selected based on accuracy and dependency constraints.

---

# 55. Statistics and Reproducibility

Approximate statistics can still be reproducible if the algorithm and processing order are controlled.

This should be considered when designing parallel statistics.

---

# 56. Validation at Scale

Validation must also scale.

Avoid:

```text
load all generated files into memory
```

for global validation.

Prefer streaming or indexed validation where possible.

---

# 57. Primary-Key Validation

For huge datasets, checking every primary key may require substantial memory.

Potential approaches:

```text
sorted IDs
hash sets
external sorting
partitioned validation
```

The correct approach depends on the output and generation architecture.

---

# 58. Foreign-Key Validation

Foreign-key validation can often use compact reference indexes.

For example:

```text
product IDs
customer IDs
order IDs
```

can be indexed once.

Then child validation becomes efficient membership testing.

---

# 59. Duplicate Detection

Duplicate detection is another potential memory bottleneck.

Where IDs are deterministic:

```text
entity + sequence
```

uniqueness may be provable structurally without retaining every ID.

This is preferable when the ID generation contract guarantees uniqueness.

---

# 60. Validation Strategy

Validation should distinguish:

```text
structural proof
```

from:

```text
expensive empirical checking
```

Use the cheapest correct method.

---

# 61. Performance of Dirty Scenarios

Data-quality corruption should not require a second full copy of the dataset.

Prefer:

```text
generate record
→ apply deterministic quality rule
→ write
```

where practical.

---

# 62. Skew Performance

Skew models can be computationally expensive if every selection requires complex probability calculations.

Precompute static components.

For dynamic skew:

```text
cache by relevant context
```

where memory remains bounded.

---

# 63. Scenario Performance

Scenario composition should ideally be resolved once before generation.

Avoid repeatedly parsing configuration for every record.

Preferred:

```text
configuration
→ effective scenario
→ executable model
→ generate records
```

---

# 64. Configuration Parsing

HOCON should be parsed once.

Domain generators should receive typed configuration or behavior objects.

Do not repeatedly query:

```scala
config.getDouble(...)
```

inside record-generation loops.

---

# 65. Object Construction

Complex behavior models should be constructed once.

Bad:

```scala
for each order:
    new ProductPopularityModel(...)
```

Preferred:

```text
create model once
→ reuse
```

provided it is immutable or safely scoped.

---

# 66. Factory Lifetime

Factories should generally construct long-lived strategies.

The generator loop should not repeatedly invoke expensive factories.

---

# 67. Thread Safety

Immutable strategies are preferred for parallel generation.

If a strategy has mutable state, the architecture must explicitly define ownership.

Avoid shared mutable state unless required.

---

# 68. Shared Randomness

Never use one shared mutable random generator across parallel workers.

Instead:

```text
root seed
→ deterministic child streams
```

This preserves isolation and reproducibility.

---

# 69. Parallel Output

Multiple workers may write separate files.

Avoid many workers writing to the same file unless the writer is specifically designed for concurrent output.

Separate shards are simpler and usually more scalable.

---

# 70. Deterministic Record Assignment

If records are partitioned into shards:

```text
record index
→ deterministic shard
```

should be stable.

For example:

```text
shard = index % shardCount
```

or another documented partitioning function.

The exact algorithm should be chosen carefully because changing it can alter reproducibility.

---

# 71. Scale and Seed Semantics

Changing scale should not unexpectedly change records that already belong to a smaller dataset if the project chooses prefix-stable generation.

This is a useful but non-mandatory property.

For example:

```text
small first 1M customers
```

could match:

```text
large first 1M customers
```

under the same seed.

Achieving this requires careful indexing and should be treated as a design decision.

---

# 72. Prefix Stability

Prefix stability is valuable for benchmarking because:

```text
small
```

can be a subset of:

```text
medium
```

rather than a completely unrelated random dataset.

However, it can conflict with global calibration.

Therefore the project should not promise prefix stability unless explicitly implemented and tested.

---

# 73. Scaling Relationships

When cardinality increases, dependent entities should scale according to configured distributions.

Example:

```text
10× customers
```

should generally lead to approximately:

```text
10× addresses
10× sessions
10× events
```

if behavior distributions remain unchanged.

But order and event counts may vary stochastically.

---

# 74. Exact vs Expected Counts

A scalable generator should distinguish:

```text
exact cardinality
```

from:

```text
expected cardinality
```

For example:

```text
customers = exactly 1,000,000
```

but:

```text
events = stochastic based on sessions
```

may produce a distribution around a target.

This should be explicit in the generation plan.

---

# 75. Cardinality Control

If exact final cardinality is required, the generator may need a constrained allocation algorithm.

For example:

```text
target 100M events
```

while preserving:

```text
variable events/session
```

can be achieved by allocating counts from a distribution and correcting the final total.

This should not collapse the distribution into a constant.

---

# 76. Scale Calibration

Large profiles should be calibrated using:

```text
row counts
record sizes
generation time
memory
disk usage
```

before being labeled:

```text
large
xlarge
```

The labels themselves are not meaningful without measurements.

---

# 77. Hardware Dependence

Performance results depend on:

```text
CPU
memory
storage
filesystem
JVM
OS
```

Therefore performance metadata should eventually capture environment information.

The dataset itself should remain environment-independent.

---

# 78. Generator Benchmarking

A benchmark suite should eventually measure:

```text
generation throughput
peak memory
output throughput
validation throughput
```

for representative scales.

---

# 79. Benchmark Isolation

Benchmark runs should distinguish:

```text
generation CPU
```

from:

```text
output I/O
```

and:

```text
validation
```

Otherwise optimization conclusions can be misleading.

---

# 80. Microbenchmarks

Microbenchmarks may be useful for:

```text
weighted sampling
product selection
ID generation
timestamp generation
CSV serialization
```

They should be used only after profiling identifies a relevant hot path.

---

# 81. Integration Benchmarks

More important than isolated microbenchmarks are end-to-end tests such as:

```text
generate 10M records
```

and measure:

```text
total time
peak memory
output size
```

These represent actual system behavior.

---

# 82. Regression Benchmarks

The project should retain baseline measurements.

Example:

```text
1M orders
generation: 18s
validation: 4s
output: 9s
peak memory: 1.2GB
```

Numbers are illustrative.

The goal is to detect regressions.

---

# 83. Performance Budgets

Eventually profiles can define budgets.

For example:

```text
medium:
  generation < 60 seconds
  peak memory < 2 GB
```

These should be environment-specific and therefore not hard-coded as universal guarantees.

---

# 84. Performance Failures

Performance regressions should not be confused with correctness failures.

A generator can be:

```text
correct but slow
```

or:

```text
fast but incorrect
```

The test strategy should keep these dimensions separate.

---

# 85. Scalability Failure

A scalability test might reveal:

```text
10M rows → 30s
100M rows → 25 minutes
```

where scaling is worse than expected.

This is a performance architecture issue.

The first step should be profiling, not immediately redesigning the entire system.

---

# 86. Complexity Expectations

Important operations should have understood complexity.

Examples:

```text
reference lookup       → O(1) average
sequential generation → O(n)
streaming output       → O(n)
full sort              → O(n log n)
```

Avoid accidental:

```text
O(n²)
```

relationships.

---

# 87. Common O(n²) Risks

Examples:

```text
scan all products for every order item
scan all customers for every order
scan all events to find a session
rebuild probability table for every record
```

These patterns must be avoided at scale.

---

# 88. Temporal Generation Complexity

Time-window operations should not repeatedly scan every campaign.

Prefer:

```text
sorted campaign intervals
indexed lookup
bucketed context
```

when campaign counts become large.

---

# 89. Geography Complexity

Address generation should not repeatedly traverse:

```text
Country
→ State
→ City
→ Area
→ Road
→ Society
→ Building
```

through linear scans.

The GeographyReferenceData should expose efficient indexed resolution.

---

# 90. Product Popularity Complexity

Product selection should not repeatedly calculate all customer-product affinity scores.

Potential approach:

```text
customer profile
→ segment
→ cached category/brand preference
→ product sampler
```

rather than:

```text
customer × every product
```

for each purchase.

---

# 91. Behavioral Model Caching

Behavior models may precompute:

```text
customer segment
category preference distribution
price sensitivity parameters
activity parameters
```

This can dramatically reduce per-record computation.

The cached state must remain deterministic and immutable where possible.

---

# 92. Cache Correctness

Caches must not change business semantics.

If:

```text
cache miss
```

produces a different random stream or behavior than:

```text
cache hit
```

reproducibility can break.

Cache keys and random streams should therefore be deterministic.

---

# 93. Memory-Aware Caching

Do not cache every possible combination if cardinality explodes.

For example:

```text
customer × category × product × time bucket
```

may be enormous.

Prefer:

```text
customer segment
+
category
+
time bucket
```

or other bounded representations where acceptable.

---

# 94. Reference Data Versioning

Performance optimizations may rely on reference-data indexes.

The index should be derived from a specific reference-data version/fingerprint.

This protects reproducibility.

---

# 95. Failure Semantics at Scale

Large runs increase the cost of failure.

Therefore:

```text
preflight
+
early validation
+
bounded memory
+
atomic output
```

become increasingly important.

The error-handling architecture and scalability architecture must work together.

---

# 96. Checkpointing

Checkpointing may eventually be useful for extremely long runs.

Conceptually:

```text
shards 0–99 complete
shards 100–199 running
```

A failure could resume from completed shards.

This should only be introduced when run duration justifies the complexity.

---

# 97. Checkpoint Determinism

A resumed run should produce the same logical dataset as a fresh run.

This is another reason to make shard identity and random streams deterministic.

---

# 98. Checkpointing Is Not a Default Requirement

The current baseline is small enough that checkpointing would add complexity without meaningful benefit.

Do not implement it prematurely.

---

# 99. Streaming Is More Important Than Checkpointing

For the next scalability stage, prioritize:

```text
streaming
bounded memory
efficient lookups
efficient selection
buffered output
```

before:

```text
distributed checkpointing
```

---

# 100. Distributed Generation

The generator may eventually need to run across multiple machines.

This is not currently required.

Before considering distributed generation, the single-process architecture should first demonstrate:

```text
large-scale generation
bounded memory
deterministic sharding
parallel local generation
```

---

# 101. Spark Independence

The generator should not depend on Spark simply because its output will be used by Spark.

The generator is responsible for:

```text
business data generation
```

Spark is responsible for:

```text
downstream processing
```

This separation keeps both systems easier to reason about.

---

# 102. Spark-Friendly Output

Nevertheless, the output architecture should consider Spark-friendly properties:

```text
reasonable file sizes
stable schemas
consistent types
partitionable timestamps
deterministic IDs
manageable file counts
```

These are output-contract concerns.

---

# 103. Performance and File Format

CSV is appropriate for initial debugging and inspection.

For large benchmark datasets, a columnar format such as:

```text
Parquet
```

will likely be more appropriate.

The architecture should allow this without changing domain generation.

---

# 104. Format Strategy

The output architecture can eventually use:

```text
OutputWriter
```

with implementations such as:

```text
CsvOutputWriter
ParquetOutputWriter
```

if the project needs them.

The domain layer should not know which format is used.

---

# 105. Observability

Performance metrics belong in the observability architecture.

At minimum:

```text
phase duration
records generated
records written
throughput
```

At larger scale:

```text
peak memory
GC behavior
output throughput
```

may be useful.

---

# 106. Progress Reporting

Long-running generation should expose progress.

For example:

```text
Customers  [████████████████] 100%
Orders     [██████████------] 62%
Events     [████------------] 41%
```

The implementation can be simpler initially.

The important requirement is visibility into long-running phases.

---

# 107. Progress Must Not Become a Bottleneck

Do not log every record.

Prefer:

```text
every N records
```

or:

```text
every N seconds
```

Progress reporting itself must remain cheap.

---

# 108. Logging Volume

At xlarge scale, excessive logging can dominate runtime.

Record-level logging should normally be disabled.

Useful logs are:

```text
phase started
phase completed
periodic progress
warnings
errors
summary
```

---

# 109. Performance-Sensitive Logging

Avoid constructing expensive log messages when the log level is disabled.

This is especially relevant inside tight generation loops.

---

# 110. SOLID

## Single Responsibility

Generators generate.

Writers write.

Statistics calculate.

Validators validate.

Profilers/observability collect metrics.

## Open/Closed

New output formats or sampling strategies can be added without rewriting entity models.

## Liskov Substitution

Alternative distribution or output implementations must preserve their contracts.

## Interface Segregation

Avoid one giant `PerformanceAwareGenerator` interface.

## Dependency Inversion

Application orchestration can depend on focused output/statistics abstractions.

---

# 111. Strategy Pattern

Good Strategy candidates include:

```text
ProductPopularityModel
CustomerActivityModel
OrderFrequencyModel
OutputWriter
FilePartitioningStrategy
```

when there are real alternative behaviors.

Do not create strategies merely for simple one-line algorithms.

---

# 112. Factory Pattern

Factories can construct:

```text
configured distribution
behavior model
output writer
scenario
```

once during application setup.

They should not be invoked repeatedly in the hot loop.

---

# 113. Dependency Injection

Constructor injection is sufficient.

Example:

```scala
final class OrderGenerator(
  frequencyModel: OrderFrequencyModel,
  productSelector: ProductSelector,
  random: RandomGenerator
)
```

This makes dependencies visible and testable.

---

# 114. Composition

The most important performance architecture principle is composition.

A large generator should be composed from:

```text
behavior model
time model
relationship resolver
distribution model
entity factory
```

rather than one giant method containing all business and technical logic.

---

# 115. Readability and Performance

Performance optimizations must remain readable.

Avoid replacing clear code with obscure micro-optimizations unless measurements justify them.

Example:

```scala
val product = productSelector.select(...)
```

is preferable to an opaque hand-optimized implementation until profiling demonstrates that selection is a bottleneck.

---

# 116. Abstraction Cost

Abstractions have runtime costs too.

Do not create deep chains such as:

```text
Generator
→ Service
→ Manager
→ Processor
→ Handler
→ Strategy
→ Factory
→ Provider
```

for a simple operation.

The architecture should optimize for:

```text
business clarity
+
measured performance
```

---

# 117. Current Implementation vs Target

The current project is a working single-process Scala generator with CSV output and in-memory generated data structures.

The target architecture moves toward:

```text
streaming generation
bounded memory
efficient indexes
deterministic sharding
parallel generation
streaming statistics
scalable validation
atomic output
performance observability
```

These target capabilities should be introduced incrementally.

---

# 118. Recommended Migration Sequence

Performance work should follow the architectural refactoring rather than precede it.

### Phase 1

Measure current baseline.

### Phase 2

Remove unnecessary eager collections.

### Phase 3

Introduce iterator/streaming generation where useful.

### Phase 4

Separate reference data from transaction data.

### Phase 5

Optimize lookup structures.

### Phase 6

Optimize product/customer selection.

### Phase 7

Stream output.

### Phase 8

Stream statistics.

### Phase 9

Scale validation.

### Phase 10

Introduce deterministic sharding.

### Phase 11

Introduce local parallel generation.

### Phase 12

Add large-scale benchmark profiles.

### Phase 13

Only then consider distributed execution.

---

# 119. Performance Quality Gate

Before declaring scalability architecture mature:

```text
□ no unnecessary full-dataset materialization
□ transaction generation can stream
□ reference lookups are indexed
□ no accidental O(n²) operations
□ weighted sampling structures are reusable
□ configuration is parsed once
□ behavior models are reused
□ random streams are deterministic and isolated
□ output is buffered
□ statistics can operate incrementally
□ validation has scalable paths
□ long-running phases expose progress
□ performance metrics are recorded
□ scale profiles are measurable
□ large datasets do not silently truncate
□ output integrity survives failures
```

---

# 120. Definition of Done

Performance and scalability architecture is mature when:

### Scale

The same application architecture can generate progressively larger datasets.

### Memory

Memory usage is bounded by design for streaming-heavy entities.

### Throughput

Generation and output throughput are measurable.

### Correctness

Optimization does not change business semantics unintentionally.

### Reproducibility

Parallel or sharded execution remains deterministic under the defined reproducibility contract.

### Maintainability

Performance-critical code remains understandable.

### Benchmark Utility

The generated datasets are large enough and structurally realistic enough to support meaningful Spark experiments.

---

# 121. Explicit Architecture Decisions

### Decision 1

Scalability is an architectural requirement, not a final optimization pass.

### Decision 2

Correctness and realism must not be sacrificed for raw generation speed.

### Decision 3

Scale should be configuration-driven.

### Decision 4

Scale profiles should be distinguished from behavior profiles.

### Decision 5

Transaction/event data should move toward streaming generation.

### Decision 6

Reference data may be materialized and indexed because it is comparatively small and stable.

### Decision 7

Large per-record lookup operations must avoid accidental O(n²) behavior.

### Decision 8

Configuration should be parsed once and transformed into executable domain behavior.

### Decision 9

Expensive selection structures should be reused rather than rebuilt per record.

### Decision 10

Statistics and validation must eventually support incremental processing.

### Decision 11

Parallel generation must use isolated deterministic random streams.

### Decision 12

Deterministic sharding is the preferred first step toward large-scale parallelism.

### Decision 13

Checkpointing and distributed generation are future capabilities, not current requirements.

### Decision 14

Spark should consume the generated data but should not become a dependency of the generator's domain architecture.

### Decision 15

Performance optimization must be evidence-driven through profiling and benchmarks.

### Decision 16

Readable business-oriented code takes priority over premature micro-optimization.

### Decision 17

Output format and physical file partitioning remain infrastructure concerns.

---

# 122. Final Design Summary

The target performance architecture is:

```text
                 Configuration
                      |
                      v
                Effective Plan
                      |
              +-------+-------+
              |               |
       Reference Data      Behavior Models
              |               |
              +-------+-------+
                      |
               Generation DAG
                      |
          +-----------+-----------+
          |           |           |
       Customer    Product    Geography
          |           |           |
          +-----------+-----------+
                      |
                Transaction Flow
                      |
          +-----------+-----------+
          |                       |
      Streaming              Deterministic
      Generation               Sharding
          |                       |
          +-----------+-----------+
                      |
                Buffered Output
                      |
          +-----------+-----------+
          |                       |
      Statistics               Validation
          |                       |
          +-----------+-----------+
                      |
                  Manifest
                      |
                  Published
                  Dataset
```

The key scalability transition is:

```text
CURRENT

generate everything
        ↓
hold data
        ↓
validate
        ↓
write


TARGET

configure
   ↓
preflight
   ↓
stream generation
   ↓
buffered output
   ├──→ streaming statistics
   └──→ scalable validation
   ↓
finalize
   ↓
publish
```

This architecture allows ShopSphere to grow from a development-scale generator into a serious benchmark-data platform without rewriting its domain model.

The most important performance principle is:

> **Do not make the business model simpler to make the generator faster until measurement proves that the business model itself is the bottleneck.**

First optimize:

```text
allocation
lookup
sampling
I/O
streaming
parallelism
```

and only then reconsider expensive realism features.

For the future Spark Performance Laboratory, this matters because the generator is itself the first performance-sensitive system in the project. If its scalability architecture is weak, producing sufficiently large and realistic benchmark datasets becomes difficult. If it is designed correctly, the same business model can generate small test fixtures, medium development datasets, large analytical datasets, and xlarge Spark benchmark workloads with controlled differences in scale, skew, temporal behavior, and data quality.
