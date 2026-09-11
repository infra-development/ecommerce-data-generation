# 12 — Output Format and Storage Decisions

## 1. Purpose

This document defines how generated e-commerce data is physically written to storage.

The logical data model and generation rules determine **what data exists**. This document determines **how that data is represented as files**.

The initial output design must remain simple enough to implement early while also supporting the later Spark Performance Laboratory, where physical file layout, file counts, partitioning, and small-file behavior are important learning dimensions.

---

## 2. Core Decisions

| Area | Decision |
|---|---|
| Initial format | CSV |
| Initial storage | Local filesystem |
| Logical organization | One entity per directory |
| Physical organization | One or more part files per entity |
| File naming | Deterministic |
| Headers | Enabled by default |
| Encoding | UTF-8 |
| Null representation | Explicit, documented representation |
| File generation | Stream/chunk records rather than requiring the complete dataset in memory |
| Generation finalization | Write to temporary location, then finalize |
| Compression | Deferred |
| Parquet | Deferred |
| Avro | Deferred |
| JSON/JSONL | Deferred |
| Object storage | Future abstraction |
| Checksums | Optional/future |
| Physical-layout scenarios | Supported through configuration |

---

## 3. Separation of Logical Data and Physical Layout

The generator must distinguish between:

1. **Logical dataset**
   - entities
   - fields
   - relationships
   - cardinalities
   - distributions
   - business rules
   - data-quality behavior

2. **Physical output**
   - file format
   - directory structure
   - number of files
   - records per file
   - file sizes
   - naming
   - encoding
   - compression
   - partitioning/layout

Changing physical layout must not silently change the logical dataset.

For example, generating `customer` data into 1 file versus 100 files should represent the same logical customer population when the same generation configuration and seed are used, subject to the documented physical-output effects.

This separation is important because later Spark experiments should be able to vary file layout without confusing a storage effect with a data-generation effect.

---

## 4. Initial Output Format: CSV

CSV is the initial output format because it is:

- simple to inspect manually
- easy to generate from a normal Scala application
- widely supported
- suitable for early validation
- useful for introducing Spark readers before moving to columnar formats

The first implementation should produce conventional CSV files with:

- one record per line
- a header row
- UTF-8 encoding
- proper escaping and quoting
- a documented null representation
- deterministic column ordering

The generator must use a real CSV serialization strategy rather than constructing lines by naïvely concatenating strings. Fields may contain commas, quotes, or other characters that require escaping.

---

## 5. Directory Structure

The initial directory hierarchy should make generation identity and entity boundaries obvious.

Recommended structure:

```text
data/
└── raw/
    └── generation-000001/
        ├── manifest.json
        ├── customer/
        │   ├── part-00000.csv
        │   └── ...
        ├── address/
        │   ├── part-00000.csv
        │   └── ...
        ├── product/
        │   ├── part-00000.csv
        │   └── ...
        ├── category/
        │   └── part-00000.csv
        ├── brand/
        │   └── part-00000.csv
        ├── order/
        │   └── part-00000.csv
        ├── order_item/
        │   └── part-00000.csv
        ├── payment/
        │   └── part-00000.csv
        ├── shipment/
        │   └── part-00000.csv
        ├── return/
        │   └── part-00000.csv
        ├── session/
        │   └── part-00000.csv
        └── event/
            └── part-00000.csv
```

The exact entity list follows the project data model and may grow as the model evolves.

### Design rule

The generation directory is the boundary of one complete dataset generation.

Everything required to interpret that generation should be discoverable from that directory, especially the manifest.

---

## 6. Generation Directory Identity

Each generation should receive a unique generation identifier.

Example:

```text
generation-000001
generation-000002
generation-000003
```

The generation identifier is separate from the random seed.

- **Generation ID** identifies an output artifact.
- **Seed** identifies the deterministic random-generation state.

A rerun with the same configuration and seed may produce an equivalent logical dataset while receiving a different generation ID if it is intentionally written as a new generation.

---

## 7. Overwrite Policy

The default behavior should be:

> Never silently overwrite an existing generation.

If the requested output directory already exists, the generator should fail unless an explicit overwrite/replace mode has been introduced and enabled.

This protects generated datasets from accidental destruction and makes experiments reproducible.

An explicit future option may support:

```text
output.overwrite = false
```

The default remains safe behavior.

---

## 8. Entity Directories

Each entity receives its own directory.

For example:

```text
generation-000001/customer/
generation-000001/order/
generation-000001/order_item/
```

This provides:

- clear entity boundaries
- simple Spark input paths
- independent file-count control
- easier validation
- easier inspection
- easier deletion/replacement of an entity during development

Entity directory names should be stable and configuration-independent.

---

## 9. Part Files

Large entities should not necessarily be written to a single file.

The writer should support multiple part files:

```text
part-00000.csv
part-00001.csv
part-00002.csv
...
```

Part numbering starts at zero and is zero-padded deterministically.

The exact padding width can be fixed initially and generalized later if needed.

### Why part files matter

The Spark Performance Laboratory will eventually need to study:

- one large file
- several medium files
- many small files
- different file counts for the same logical dataset

Therefore file count is not merely an implementation detail. It is a controllable physical-layout dimension.

---

## 10. File Count Configuration

The output layer should support an explicit file-count concept.

Conceptually:

```hocon
output {
  file_count {
    customer = 4
    order = 8
    order_item = 16
  }
}
```

The exact configuration syntax is governed by the configuration decisions document.

The generator should validate that requested file counts are sensible:

- positive
- applicable to the selected entity
- compatible with the selected scale
- not accidentally creating an unreasonable number of files

If no explicit value is supplied, the generator may use a deterministic default based on the selected scale profile.

---

## 11. Records Per File and Chunking

File count and records-per-file are related but should be conceptually separate.

Possible future controls include:

```text
file_count
records_per_file
target_file_size
```

Initially, the implementation should favor deterministic **records-per-file/file-count based chunking** rather than byte-size targeting.

Byte-size targeting is more complicated because:

- CSV row sizes vary
- escaping changes byte length
- UTF-8 characters may have variable width
- compression, when introduced, changes physical size

A future size-oriented output strategy can be added without changing the logical generation model.

---

## 12. Deterministic Record Assignment to Files

Given the same:

- logical dataset
- seed
- configuration
- entity row count
- file-count/chunking configuration

record-to-file assignment should be deterministic.

For example, if an entity contains 10,000 records and is configured for 10 files, the writer can assign deterministic contiguous record ranges:

```text
part-00000.csv → records 0–999
part-00001.csv → records 1000–1999
...
```

The exact algorithm may evolve, but it must not depend on nondeterministic iteration order.

This makes file-level comparisons and experiment reproduction substantially easier.

---

## 13. Record Ordering

The generator should produce deterministic record ordering within each entity unless a scenario explicitly requires another ordering.

Ordering must not depend on:

- hash-map iteration order
- filesystem ordering
- thread scheduling
- nondeterministic collection traversal

For relational entities, stable ordering is especially useful for debugging relationships and comparing two generations.

A later performance-oriented scenario may intentionally introduce different ordering, but that should be an explicit scenario rather than an accidental implementation property.

---

## 14. Headers and Schema

CSV files should contain a header row by default.

Example:

```csv
customer_id,email,segment,acquisition_channel,created_at
C000001,user1@example.com,regular,organic,2025-01-03T10:15:00Z
```

Column order must be deterministic and defined by the entity schema.

The output schema should not be inferred from the first generated record.

The manifest should record enough schema information to make the output self-describing at the dataset level.

---

## 15. Encoding

Initial standard:

```text
UTF-8
```

This should be consistent across all generated CSV files.

Encoding should be documented in the manifest or output metadata.

The generator should not depend on the host machine's default character encoding.

---

## 16. CSV Escaping and Quoting

CSV serialization must correctly handle:

- commas
- double quotes
- line breaks where permitted
- empty strings
- Unicode characters

For example, a value containing a comma must be represented according to the selected CSV dialect.

The serializer must ensure that generated output can be read back without ambiguity.

Round-trip tests should verify:

```text
generated record
    ↓
CSV writer
    ↓
CSV parser
    ↓
equivalent record
```

---

## 17. Null Representation

The initial implementation must choose one explicit null representation and apply it consistently.

Recommended initial representation:

```text
<NULL>
```

This must be distinguished from:

- empty string
- literal text such as `"null"`
- whitespace

The chosen representation should be documented in the output contract and manifest.

Data-quality scenarios that introduce missing values must use the same representation unless the scenario explicitly models malformed/mixed null encoding.

---

## 18. Line Endings

The generator should use a consistent line-ending strategy rather than relying on the operating system.

The initial implementation should standardize on a single line-ending convention and test it consistently across development environments.

This avoids unnecessary differences between generated datasets on Windows, Linux, and CI environments.

---

## 19. Temporary Output and Atomic Finalization

Generation should not directly expose a partially generated dataset as the final generation.

Recommended flow:

```text
create temporary generation directory
        ↓
generate entity files
        ↓
validate generated output
        ↓
write/finalize manifest
        ↓
atomically or safely rename/finalize directory
        ↓
generation becomes visible as complete
```

The exact filesystem atomicity guarantees depend on the target filesystem.

The important invariant is:

> A generation directory should not be treated as complete merely because generation started.

A completion marker may be introduced if required.

---

## 20. Failure Handling

If generation fails:

- the failure must be reported clearly
- the incomplete generation must not be mistaken for a valid dataset
- temporary files should be cleaned up where safe
- the original existing datasets must remain untouched
- diagnostic information should identify the failing stage/entity

A future implementation may preserve failed generations for debugging when explicitly configured.

The default behavior should favor clean output directories.

---

## 21. Manifest Placement

The manifest belongs at the generation root:

```text
generation-000001/
├── manifest.json
├── customer/
├── order/
└── ...
```

The manifest is the authoritative metadata record for that generated dataset.

It should contain, at minimum:

- generation ID
- seed
- profile
- scenario
- effective configuration
- generation timestamp
- logical date range
- entity row counts
- unique/cardinality statistics
- file counts
- file sizes
- data-quality statistics
- distribution statistics
- output format
- output encoding
- output layout information

This follows the project requirement that every generation be accompanied by useful generation metadata.

---

## 22. Manifest and Validation

Manifest creation should happen only after the generator has enough information to report the final state accurately.

The manifest should distinguish between:

- requested values
- generated values
- validated values

For example:

```text
requested customer rows = 1,000,000
generated customer rows = 1,000,000
validated customer rows = 1,000,000
```

Where validation fails, the generation should not be presented as a successful completed artifact.

---

## 23. File Statistics

For every output file, the system should be able to determine at least:

- file path/name
- entity
- record count
- byte size

Example conceptual manifest entry:

```json
{
  "entity": "customer",
  "file": "customer/part-00000.csv",
  "record_count": 250000,
  "size_bytes": 42133789
}
```

Checksums are useful for stronger reproducibility and artifact verification, but are initially optional/deferred.

---

## 24. Compression

Compression is intentionally deferred from the first CSV implementation.

Potential future options include:

```text
gzip
bzip2
zstd
```

Compression introduces additional physical dimensions that are useful for Spark experiments but should not complicate the first implementation.

When compression is introduced, it should be modeled as an output-layer concern rather than a data-generation concern.

---

## 25. Future Columnar Formats

The architecture should leave room for:

- Parquet
- Avro
- JSON/JSONL
- other Spark-compatible formats

However, these should not be implemented merely because they are useful eventually.

The first milestone should establish a clean output abstraction so that additional writers can be introduced without rewriting the generation logic.

Conceptually:

```text
Logical Records
      ↓
Output Writer Interface
      ├── CSV Writer
      ├── Parquet Writer       (future)
      ├── Avro Writer          (future)
      └── JSON Writer          (future)
```

---

## 26. Local Filesystem

The first implementation targets the local filesystem.

The writer should avoid embedding assumptions throughout the generation code that make future storage backends impossible.

A storage abstraction can eventually support:

```text
Local filesystem
HDFS
S3-compatible object storage
Azure Blob
GCS
```

The current project does not require implementing these backends.

---

## 27. Physical Layout Scenarios for Spark

The output layer is deliberately designed to support later Spark performance experiments.

Examples include:

### Scenario A — Few large files

```text
customer/
  part-00000.csv
```

Useful for studying larger sequential reads and reduced file-opening overhead.

### Scenario B — Moderate file count

```text
customer/
  part-00000.csv
  ...
  part-00007.csv
```

Useful as a more typical distributed-data layout.

### Scenario C — Many small files

```text
customer/
  part-00000.csv
  ...
  part-00999.csv
```

Useful for studying small-file overhead and metadata/listing costs.

These scenarios must be produced through explicit configuration rather than accidental behavior.

---

## 28. Logical vs Physical Experiment Isolation

When comparing Spark performance, the experiment should record both:

### Logical dimensions

- row counts
- cardinalities
- distributions
- skew
- data-quality profile

### Physical dimensions

- format
- compression
- file count
- records per file
- file size
- partitioning/layout

This prevents incorrect conclusions such as attributing a performance difference to skew when the real difference is simply file count.

---

## 29. Output Validation

Before a generation is finalized, the output layer should verify:

1. expected entity directories exist
2. expected files exist
3. file names follow the naming contract
4. headers are correct
5. record counts match generation statistics
6. files are readable
7. encoding is correct
8. no unexpected temporary files remain
9. manifest references existing files
10. manifest totals agree with actual output

Output validation complements, but does not replace, logical validation.

---

## 30. Initial Implementation Scope

The first implementation should include:

- CSV writer
- local filesystem writer
- generation-root directory
- one directory per entity
- deterministic part-file naming
- configurable file count
- deterministic record ordering
- deterministic record-to-file assignment
- UTF-8
- headers
- correct CSV escaping
- explicit null representation
- temporary output/finalization behavior
- generation manifest
- file and row statistics
- output validation

This is sufficient to establish a reliable physical dataset artifact.

---

## 31. Deferred Decisions

The following are intentionally deferred:

- Parquet writer
- Avro writer
- JSON writer
- compression
- target byte-size based file splitting
- object-storage writers
- checksums as a mandatory feature
- advanced partition-directory schemes
- filesystem-specific atomicity guarantees
- encryption
- schema registry integration
- transactional table formats

These can be introduced when they directly support a later project milestone or Spark experiment.

---

## 32. Design Principles

The output subsystem follows these principles:

1. **Logical data is independent of physical layout.**
2. **Output must be deterministic.**
3. **Partial output must not masquerade as complete output.**
4. **File layout is an explicit experiment dimension.**
5. **CSV serialization must be correct, not merely convenient.**
6. **Metadata is part of the generated artifact.**
7. **The writer must scale without requiring the complete dataset in memory.**
8. **Storage-specific concerns belong in the output layer.**
9. **Future formats should be addable without changing entity-generation logic.**
10. **Safe defaults are preferred over destructive behavior.**

---

## 33. Guiding Principle

> **The generator defines the dataset; the output layer defines its physical representation.**

The first output implementation should therefore be intentionally boring, deterministic, inspectable, and reliable. More sophisticated physical layouts should be added only when they serve a concrete learning or performance-testing objective.
