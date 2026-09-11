# 08 — Reproducibility

## Purpose

This document defines the reproducibility contract for the ShopSphere synthetic e-commerce data generator.

Reproducibility is a core product capability, not merely a testing convenience. A generated dataset must be traceable to the configuration, seed, generator behavior, and scenario that produced it.

The business/product specification establishes that the same configuration and seed should produce equivalent data, while changing the seed should produce different data with the same intended statistical characteristics.

---

## 1. Core Decision

The generator will use **explicit, controlled randomness**.

The fundamental reproducibility identity is:

```text
Effective Configuration
+
Seed
+
Generator Version / Generation Behavior
=
Generation Result
```

Therefore:

> **A generation must never depend on hidden or implicit sources of randomness.**

---

## 2. Reproducibility Goals

The generator should support three distinct guarantees.

### 2.1 Exact reproducibility

For a stable generator implementation:

```text
same effective config
+
same seed
+
same execution strategy
=
same generated records
```

This is the strongest and preferred guarantee for the initial implementation.

### 2.2 Statistical reproducibility

Changing the seed should produce different records while preserving the intended statistical characteristics.

Example:

```text
seed = 1001
VIP customers ≈ 5%

seed = 2002
VIP customers ≈ 5%
```

The exact rows differ, but the population behavior remains comparable.

### 2.3 Generation traceability

A generated dataset should contain enough metadata to answer:

```text
What configuration produced this?
What seed was used?
Which generator version produced it?
Which profile was selected?
Which scenarios were active?
```

---

## 3. Seed Is a First-Class Input

The seed must be explicitly represented in configuration or CLI input.

Example:

```text
seed = 123456
```

A default seed may exist for convenience, but production-quality generation should make the effective seed visible in generation metadata.

The generator must never silently derive a seed from:

```text
current timestamp
process ID
machine state
thread scheduling
```

unless explicitly requested as a separate non-reproducible mode.

---

## 4. Randomness Ownership

Randomness should be owned by the generation context.

Conceptually:

```text
GenerationContext
    |
    +-- effective configuration
    +-- seed
    +-- generator version
    +-- scenario
    |
    v
Random Source
```

Entity generators should receive controlled randomness through this context rather than creating their own unmanaged random generators.

---

## 5. No Hidden Random Sources

The following patterns are prohibited inside generation logic:

```text
new Random()
Math.random()
UUID.randomUUID()
currentTimeMillis() as random seed
thread-local randomness without controlled seeding
```

unless they are deliberately wrapped by the reproducibility layer.

Identifiers must also be deterministic if they are part of generated output.

---

## 6. Random Stream Strategy

A single global random stream is simple but can make the entire dataset sensitive to small changes.

For example:

```text
CustomerGenerator
    consumes random values
ProductGenerator
    consumes random values
OrderGenerator
    consumes random values
```

If CustomerGenerator starts consuming one additional random value, every subsequent generator may receive different random values.

Therefore the architecture should support **logical random streams**.

Conceptually:

```text
Master Seed
    |
    +-- customer stream
    +-- address stream
    +-- product stream
    +-- order stream
    +-- payment stream
    +-- session stream
    +-- event stream
```

The exact implementation is deferred, but deterministic stream separation is the preferred direction.

---

## 7. Stable Stream Derivation

Logical random streams should be derived deterministically from the master seed and a stable stream identifier.

Conceptually:

```text
customerStreamSeed = derive(masterSeed, "customer")
productStreamSeed  = derive(masterSeed, "product")
orderStreamSeed    = derive(masterSeed, "order")
```

The derivation function must itself be deterministic.

Avoid relying on runtime-dependent object hashing or unordered collection iteration.

---

## 8. Why Stream Separation Matters

Consider a future change:

```text
CustomerGenerator v1
    10 random samples/customer

CustomerGenerator v2
    11 random samples/customer
```

With one global stream, the additional sample may shift randomness for every later entity.

With independent logical streams, the impact can be more localized.

This improves:

- reproducibility
- debugging
- regression analysis
- controlled evolution
- reasoning about changes

It does **not** imply that changing a generator must preserve all historical output. It simply reduces unnecessary coupling.

---

## 9. Deterministic Identifier Strategy

Generated IDs should be deterministic.

Examples:

```text
customer-000000001
product-000000001
order-000000001
```

or another stable identifier scheme defined by the data-model implementation.

Random UUIDs should not be used by default because they make exact reproducibility unnecessarily difficult.

If identifiers are intended to simulate UUID-like production identifiers, they should still be generated from the controlled random source.

---

## 10. Ordering Is Part of Reproducibility

Deterministic values alone are insufficient if record ordering changes unpredictably.

The generator should establish stable ordering for:

- entity generation
- relationship traversal
- categorical configuration
- output file creation
- manifest entries
- statistics

Avoid relying on unspecified iteration order from unordered collections.

Where ordering has no business meaning, the implementation should still use a deterministic ordering for reproducibility.

---

## 11. Configuration Resolution

The seed must be recorded after configuration resolution.

The reproducibility input is:

```text
effective configuration
```

not merely the raw configuration file.

The resolution pipeline is:

```text
defaults
   ↓
profile
   ↓
scenario
   ↓
CLI overrides
   ↓
effective configuration
   ↓
validation
   ↓
generation
```

The effective configuration should therefore be persisted or represented in generation metadata.

---

## 12. Configuration Hash

A deterministic hash of the effective configuration is useful for identifying a generation.

Conceptually:

```text
configHash = SHA-256(canonicalEffectiveConfig)
```

The exact hashing algorithm is an implementation decision, but the canonicalization principle is important.

Equivalent configurations should not produce different hashes merely because of:

- formatting
- whitespace
- property ordering

---

## 13. Generation Identity

A generation should have a traceable identity.

Conceptually:

```text
generationId
seed
configHash
generatorVersion
profile
scenario
createdAt
```

The generation ID itself does not need to be random.

A useful model is:

```text
generationId = derived identity
```

based on stable generation inputs, or a separate human-readable identifier accompanied by the deterministic metadata.

The exact format is deferred.

---

## 14. Generator Version

Generator behavior is part of reproducibility.

The same:

```text
config + seed
```

cannot automatically guarantee the same output across arbitrary generator versions.

For example, changing:

- distribution implementation
- entity generation order
- ID generation
- relationship logic
- random-stream derivation
- serialization behavior

may change output.

Therefore the generator version must be included in generation metadata.

---

## 15. Reproducibility Scope

The project should distinguish between:

### Same-version exact reproducibility

Strong target:

```text
same version
+
same effective config
+
same seed
=
same output
```

### Cross-version reproducibility

Not automatically guaranteed.

A generator behavior change may intentionally invalidate exact output equivalence.

### Statistical compatibility

Even when exact output changes, the new version should preserve intended statistical characteristics unless a distribution/model change is intentional.

---

## 16. Reproducibility Manifest

The generation manifest should record at least:

```text
generation ID
generator version
seed
configuration/profile
scenario(s)
effective configuration reference or hash
generation timestamp
entity counts
output location
```

Additional useful metadata may include:

```text
distribution-model version
schema version
application version
execution mode
```

The manifest should make a dataset independently understandable.

---

## 17. Reproducing a Previous Dataset

A future user should be able to reproduce a dataset from its generation metadata.

Conceptually:

```text
Dataset
   |
   v
Manifest
   |
   +-- generator version
   +-- seed
   +-- effective configuration
   +-- profile
   +-- scenario
   |
   v
Generator
   |
   v
Equivalent dataset
```

The project should avoid requiring undocumented CLI options or knowledge of internal code paths to reproduce a generation.

---

## 18. Seed Changes

Changing the seed should change generated records.

However, changing the seed should **not intentionally change**:

- configured entity volumes
- configured categorical probabilities
- schema
- referential integrity rules
- business invariants
- scenario semantics

unless those outcomes are inherently stochastic.

The expected relationship is:

```text
Seed A -> dataset A
Seed B -> dataset B

Dataset A ≠ Dataset B
but

Statistics(A) ≈ Statistics(B)
```

within defined statistical tolerance.

---

## 19. Reproducibility and Scaling

Scale changes are configuration changes.

Therefore:

```text
small profile + seed X
```

and:

```text
large profile + seed X
```

are different generation inputs.

They should not be expected to produce the same dataset prefix unless the generation algorithm explicitly defines such a property.

Prefix-stability across scales is **not a requirement for the initial implementation**.

This avoids constraining the architecture unnecessarily.

---

## 20. Reproducibility and Chunking

Large datasets may eventually be generated in batches/chunks.

Chunking introduces an important reproducibility concern.

A naive implementation:

```text
generate chunk 1
generate chunk 2
generate chunk 3
```

using one mutable global random stream can make output dependent on:

- chunk size
- execution order
- retries
- parallelism

Therefore the long-term design should support deterministic chunk-level random stream derivation.

Conceptually:

```text
Master Seed
   |
   +-- entity stream
          |
          +-- chunk 0
          +-- chunk 1
          +-- chunk 2
```

The exact scheme will be finalized when high-volume generation is implemented.

---

## 21. Reproducibility and Parallelism

Parallel generation should not introduce uncontrolled nondeterminism.

Potential sources include:

- concurrent random access
- nondeterministic collection ordering
- race-dependent relationship assignment
- unordered file output
- task scheduling

The initial generator may remain sequential where that simplifies correctness.

When parallelism is introduced, deterministic partitioning and random-stream ownership must be designed explicitly.

---

## 22. Reproducibility and Relationships

Relationships must remain deterministic as well as statistically realistic.

For example:

```text
Order.customer_id
```

must reference a customer generated under the same deterministic generation identity.

Similarly:

```text
OrderItem.order_id
OrderItem.product_id
Payment.order_id
Shipment.order_id
```

must remain valid when a generation is reproduced.

Relationship state must therefore be derived from deterministic IDs and/or deterministic relationship decisions.

---

## 23. Reproducibility and Conditional Distributions

Conditional distributions must use deterministic inputs.

Example:

```text
customer segment
      |
      v
activity distribution
      |
      v
number of sessions
```

For a fixed seed and configuration, the same customer should receive the same logical sampled values under the same generator version.

This allows debugging of behavioral relationships.

---

## 24. Reproducibility and Data Quality

Data-quality scenarios must also be deterministic.

Example:

```text
dirty profile
    malformed phone probability = 1%
```

With the same:

```text
config + seed + version
```

the same records should be selected for controlled corruption under the same generation strategy.

The corruption process must therefore use the controlled random source rather than hidden randomness.

---

## 25. Reproducibility and Skew

Skew scenarios must also be reproducible.

For example:

```text
product-skew
```

may change the product popularity distribution.

Given the same:

```text
scenario + seed + configuration + version
```

the same product-selection behavior should be produced.

The statistical effect of the scenario should be measurable in generation statistics.

---

## 26. Reproducibility Testing

Reproducibility should have dedicated automated tests.

### Test 1 — Same inputs

```text
config A + seed X
config A + seed X
```

Expected:

```text
equivalent output
```

### Test 2 — Different seed

```text
config A + seed X
config A + seed Y
```

Expected:

```text
different output
similar statistical characteristics
```

### Test 3 — Configuration change

```text
config A + seed X
config B + seed X
```

Expected:

```text
different output
```

when the changed configuration affects generation.

### Test 4 — Scenario change

```text
base + seed X
skew scenario + seed X
```

Expected:

```text
different intended distribution
```

### Test 5 — Manifest traceability

Every generated dataset should expose enough metadata to reconstruct its generation inputs.

---

## 27. Regression Fixtures

A small deterministic dataset should be maintained as a regression fixture.

Example:

```text
profile = small
seed = 12345
scenario = clean
```

The fixture can verify:

- schema
- IDs
- relationships
- representative values
- statistics
- manifest
- output structure

The fixture should remain intentionally small so that changes can be inspected easily.

Large datasets should not be used as the primary regression artifact.

---

## 28. What Constitutes a Breaking Change

The following should be considered reproducibility-affecting changes:

- random-stream derivation changes
- entity generation order changes
- distribution algorithm changes
- probability changes
- ID generation changes
- relationship generation changes
- schema changes
- output serialization changes
- default configuration changes
- profile changes
- scenario semantics changes

Such changes should be reflected in versioning and/or documented through the ADR process.

---

## 29. Reproducibility vs Realism

Reproducibility must not force unrealistic data.

The intended model is:

```text
deterministic sampling
        +
realistic probability models
        +
business relationships
        =
reproducible realism
```

The seed controls which realization of the population is generated.

The distribution controls what kind of population is generated.

These are separate concepts.

---

## 30. Reproducibility vs Performance

Reproducibility is a correctness requirement.

Performance optimizations must not silently remove deterministic guarantees.

However, exact byte-for-byte output should not be preserved at the cost of an impractical architecture indefinitely.

If a future optimization changes the generation behavior, the project should:

1. document the change
2. update generator/version metadata
3. preserve statistical guarantees
4. update regression fixtures where appropriate
5. record the decision in the ADR log

---

## 31. Initial Implementation Contract

For the first end-to-end generator, the following contract is sufficient:

```text
Input:
    effective configuration
    explicit seed

Generation:
    controlled seeded randomness
    deterministic entity order
    deterministic IDs
    deterministic relationships
    deterministic distributions

Output:
    deterministic dataset
    deterministic generation statistics
    manifest containing generation metadata
```

The initial implementation does not need to solve every future distributed-generation problem.

---

## 32. Deferred Decisions

The following remain intentionally open:

- exact random-number-generator implementation
- exact seed derivation algorithm
- random stream hierarchy
- deterministic parallel generation
- deterministic chunking
- cross-machine byte-for-byte guarantees
- cross-JVM-version guarantees
- serialization-level determinism
- generation identity format
- configuration hash algorithm
- long-term artifact/version compatibility policy

These should be decided when implementation requirements make them concrete.

---

## 33. Guiding Principle

The reproducibility model should make every dataset answerable by one question:

> **“What exact generation inputs and generator behavior produced this data?”**

The fundamental chain is:

```text
Seed
  +
Effective Configuration
  +
Profile
  +
Scenario
  +
Generator Version
  +
Deterministic Generation Strategy
        |
        v
Reproducible Dataset
        |
        v
Manifest + Statistics
```

The core rule is:

> **Randomness determines the realization; configuration determines the model; metadata records the identity; deterministic execution makes the result reproducible.**
