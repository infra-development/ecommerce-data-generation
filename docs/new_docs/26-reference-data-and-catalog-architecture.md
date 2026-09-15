# 26 — Reference Data & Catalog Architecture

## 1. Purpose

ShopSphere depends on a set of relatively stable entities and hierarchies that define the business world in which transactional records exist.

These include:

```text
Geography
Category
Brand
Product
```

and, depending on future scope:

```text
campaign definitions
business calendars
payment method reference data
carrier/service definitions
```

This reference layer is fundamentally different from high-volume transactional data.

Reference data defines **what can exist**.

Transactional generation defines **what happens**.

The architecture must keep these concerns separate.

---

# 2. Core Principle

Reference data should be:

```text
valid
stable
deterministic
indexed
versionable
reusable
```

The transactional generators should consume reference data rather than repeatedly reconstructing business definitions.

Conceptually:

```text
Reference Data
      ↓
Business World
      ↓
Customer / Product / Address / Order
      ↓
Transactions and Events
```

---

# 3. What Is Reference Data?

Reference data describes relatively stable entities or controlled vocabularies used during generation.

Examples:

```text
Country
State
City
Area
Road
Society
Building
Postal Code
Category
Brand
Product
Payment Method
Carrier
```

Not every one of these must be externally loaded.

Some can be generated or configured internally.

The architectural distinction is based on stability and reuse, not simply entity name.

---

# 4. Reference Data vs Generated Data

A useful distinction:

| Reference data | Generated data |
|---|---|
| relatively stable | high-volume |
| reused repeatedly | usually unique records |
| defines valid choices | describes business activity |
| often indexed | usually streamed |
| version-sensitive | run-specific |
| loaded before generation | produced during generation |

For example:

```text
Product catalog
```

is reference data.

```text
OrderItem
```

is transactional data that references the catalog.

---

# 5. Catalog Meaning

The catalog represents what ShopSphere sells.

It includes:

```text
Category
Brand
Product
```

The catalog should therefore have coherent internal relationships:

```text
Category
   ↑
Product
   ↓
Brand
```

A product should reference valid category and brand identities.

---

# 6. Geography Meaning

Geography defines the physical location hierarchy.

Current hierarchy:

```text
Country
  ↓
State
  ↓
City
  ↓
Area
  ↓
Road
  ↓
Society
  ↓
Building
  ↓
Flat / Unit
```

Postal code belongs to:

```text
Area
```

The current Address model intentionally stores:

```text
building_id
unit_number
postal_code
```

rather than duplicating:

```text
city
state
country
```

in every address.

---

# 7. Reference Data Ownership

Each domain owns the meaning of its reference entities.

### Geography

Owns:

```text
geographic hierarchy
postal-code relationships
geographic lookup
```

### Category

Owns:

```text
category definitions
category hierarchy/relationships where applicable
```

### Brand

Owns:

```text
brand definitions
brand-category relationships where applicable
```

### Product

Owns:

```text
product catalog
category association
brand association
pricing/catalog attributes
```

Cross-domain orchestration should consume these capabilities without taking ownership away from the domain.

---

# 8. Reference Data Lifecycle

Reference data has its own lifecycle:

```text
source
  ↓
load
  ↓
parse
  ↓
normalize
  ↓
validate
  ↓
index
  ↓
freeze
  ↓
consume
```

The transactional generator should only consume reference data after the required validation stage succeeds.

---

# 9. Source Types

Reference data may eventually come from:

```text
HOCON
CSV
JSON
database
API
generated defaults
```

The domain should not care about the physical source.

For example:

```text
ProductReferenceLoader
```

may eventually load products from CSV or JSON while exposing the same validated catalog interface.

---

# 10. Current Source

The current project primarily uses configuration/reference files and in-code/reference loading mechanisms.

The architecture should preserve the current simple setup while leaving room for richer reference-data sources.

Do not introduce external databases or APIs merely for architectural sophistication.

---

# 11. Loading Boundary

Reference-data loading should be separated from:

```text
reference-data validation
```

and:

```text
reference-data indexing
```

Conceptually:

```text
Loader
   ↓
Raw Reference Data
   ↓
Validator
   ↓
Indexed Reference Data
```

---

# 12. Loader Responsibility

A loader should answer:

> How do we obtain the reference data?

It should not decide:

> Is this reference data semantically valid?

That belongs to validation.

---

# 13. Normalization

Reference sources may contain formatting differences.

Examples:

```text
"Electronics "
"electronics"
" Electronics"
```

Normalization may produce a canonical representation.

Normalization rules must be explicit.

Do not silently alter identifiers if IDs are business keys.

---

# 14. Identifier Integrity

Reference IDs are important because transactional data will depend on them.

Examples:

```text
CATEGORY_001
BRAND_014
PRODUCT_000123
BUILDING_00045
```

IDs should be:

```text
unique
stable
non-empty
valid
```

---

# 15. Stable IDs

If reference data changes between runs, stable IDs make reproducibility and comparison easier.

Avoid generating reference IDs from nondeterministic collection order.

---

# 16. Duplicate IDs

Duplicate IDs should normally be a fatal reference-data validation error.

Example:

```text
PRODUCT_001
PRODUCT_001
```

The catalog cannot safely expose a unique lookup.

---

# 17. Missing References

Examples:

```text
product.category_id → missing category
product.brand_id    → missing brand
building.society_id → missing society
```

These should normally fail reference-data validation before transaction generation.

---

# 18. Referential Closure

A valid reference dataset should be referentially closed.

For example:

```text
Every Product category_id
→ valid Category
```

and:

```text
Every Product brand_id
→ valid Brand
```

Geography should similarly be closed across the hierarchy.

---

# 19. Geography Closure

The geography hierarchy should satisfy:

```text
every State → Country
every City → State
every Area → City
every Road → Area
every Society → Road
every Building → Society
```

and postal codes should resolve through their owning area.

---

# 20. Catalog Closure

The catalog should satisfy:

```text
every Product → Category
every Product → Brand
```

If category/brand relationships are optional in a future scenario, that optionality must be explicit.

---

# 21. Reference Validation Layers

Reference validation can be organized as:

```text
syntax
  ↓
schema
  ↓
identifier
  ↓
referential
  ↓
business
  ↓
statistical
```

Not every source requires every layer.

---

# 22. Syntax Validation

Examples:

```text
empty ID
invalid numeric value
malformed postal code
invalid date
```

---

# 23. Schema Validation

Verify required fields exist.

Example:

```text
Product requires:
id
name
category_id
brand_id
base_price
```

The exact current schema should remain the authoritative implementation contract.

---

# 24. Business Validation

Examples:

```text
price > 0
```

```text
building belongs to exactly one society
```

```text
postal code belongs to building's area
```

```text
product category exists
```

---

# 25. Statistical Validation

Reference data can also have statistical expectations.

Examples:

```text
category distribution
brand distribution
product price distribution
catalog depth
```

These become increasingly important when the catalog itself is synthetic.

---

# 26. Reference Data Immutability

Once loaded and validated, reference data should preferably become immutable.

Conceptually:

```text
Mutable source representation
        ↓
Validated immutable representation
        ↓
Generation
```

This makes concurrent reads safe.

---

# 27. Indexed Reference Representation

The generator should not repeatedly traverse raw source structures.

A useful representation may include:

```scala
Map[Id, Entity]
```

plus domain-specific indexes.

For example:

```text
productsByCategory
productsByBrand
productsByCategoryAndBrand
postalCodesByArea
buildingsBySociety
```

Only indexes that provide real performance or business value should be added.

---

# 28. Index Ownership

Indexes should belong to the reference-data component that understands them.

For example:

```text
GeographyReferenceData
```

can expose:

```text
buildings
postalCodesForArea
resolveBuilding
```

rather than forcing callers to understand internal map structures.

---

# 29. Encapsulation

Prefer:

```scala
geography.resolveBuilding(buildingId)
```

over exposing every internal map and requiring callers to perform joins manually.

This protects invariants and improves readability.

---

# 30. Reference Data API

A domain-facing reference component should expose business-oriented operations.

Examples:

```text
findProduct(id)
productsForCategory(categoryId)
productsForBrand(brandId)
postalCodesForArea(areaId)
resolveBuilding(buildingId)
```

Avoid exposing low-level implementation details unnecessarily.

---

# 31. Optional vs Required Lookup

Lookup APIs should make absence meaningful.

For example:

```scala
Option[Product]
```

may be appropriate for:

```text
findProduct
```

while:

```text
requireProduct
```

may be appropriate where absence indicates a broken invariant.

Do not force callers to repeatedly handle impossible states.

---

# 32. Reference Data Snapshots

A generation run should conceptually consume a reference-data snapshot.

This means:

```text
run
→ reference snapshot A
```

rather than:

```text
run
→ reference data changes halfway through
```

A snapshot can be identified by a fingerprint.

---

# 33. Reference Fingerprint

The manifest should eventually record something like:

```text
referenceDataFingerprint
```

derived from the actual reference inputs.

This allows:

```text
same seed
+
different catalog
```

to be correctly recognized as different dataset inputs.

---

# 34. Reproducibility

Reproducibility depends on more than the random seed.

A stronger identity is:

```text
seed
+
configuration
+
scenario
+
reference data
+
implementation version
```

This should be reflected in the manifest architecture.

---

# 35. Reference Data Versioning

Future reference datasets may have explicit versions:

```text
catalog-v1
catalog-v2
geography-v3
```

Versioning is especially useful when benchmark datasets must remain reproducible over time.

---

# 36. Version Semantics

A version should identify the reference content, not merely the filename.

Changing:

```text
products.csv
```

without changing a nominal version number should still be detectable through content fingerprinting.

---

# 37. Reference Data and Profiles

Scale profiles may select different reference-data sizes.

Example:

```text
small catalog
large catalog
```

However, profile semantics should remain explicit.

Do not silently replace the catalog when changing:

```text
small → large
```

unless the profile contract says so.

---

# 38. Catalog Cardinality

Catalog size is distinct from transaction volume.

A dataset can have:

```text
500 products
10M order items
```

or:

```text
1M products
10M order items
```

These create very different workload characteristics.

---

# 39. Catalog Scale

Catalog scale should therefore be independently configurable.

Useful dimensions:

```text
category count
brand count
product count
category depth
brand/category coverage
```

---

# 40. Product Catalog Realism

A realistic catalog should eventually include:

```text
long-tail product popularity
category-conditioned popularity
brand-conditioned popularity
price tiers
product lifecycle
seasonality
```

The Product domain document defines the business behavior.

Reference architecture defines how the catalog is represented and accessed efficiently.

---

# 41. Category Hierarchy

The current category model may be relatively compact.

Future catalog realism may introduce:

```text
Department
  ↓
Category
  ↓
Subcategory
  ↓
Product
```

This should only be introduced if it improves business realism or workload design.

---

# 42. Category Hierarchy vs Flat Category

A flat category structure is easier to generate and query.

A hierarchy provides richer workloads:

```text
roll-up
drill-down
hierarchical aggregation
```

The architecture should support evolution without requiring transactional generators to know the physical hierarchy representation.

---

# 43. Brand-Category Affinity

Brands may have different presence across categories.

For example:

```text
Brand A → strong electronics presence
Brand B → strong home presence
```

This can be represented as reference-data affinity.

The Product generator can consume it.

---

# 44. Product-Category Ownership

Every product should have a valid category relationship.

The catalog should prevent orphan products unless an explicit dirty-data scenario intentionally creates them after clean generation.

---

# 45. Product-Brand Ownership

Similarly:

```text
product.brand_id
```

should resolve to a valid Brand in the clean catalog.

---

# 46. Pricing Reference Data

Product base price belongs conceptually to the Product domain.

Reference representation may store:

```text
base price
price tier
currency
```

where the schema supports these attributes.

Transaction-time pricing can then apply:

```text
promotion
discount
customer effects
```

without mutating the reference catalog.

---

# 47. Catalog Price vs Transaction Price

This distinction is important.

```text
catalog/base price
```

is reference data.

```text
transaction price
```

is generated business activity.

A promotion should not rewrite the product's base price.

---

# 48. Currency

If multi-currency support is introduced:

```text
currency
exchange rate
pricing region
```

become reference/business context.

The current project can remain single-currency until multi-currency requirements exist.

---

# 49. Payment Method Reference Data

Payment methods can be represented as controlled reference values:

```text
UPI
CREDIT_CARD
DEBIT_CARD
NET_BANKING
WALLET
```

The Payment domain controls behavior such as:

```text
selection
success rate
retry
switching
```

Reference data defines the available methods.

---

# 50. Shipment Reference Data

Future shipment realism may require:

```text
carrier
service level
delivery zone
```

These are suitable reference entities.

The Shipment domain uses them to model operational behavior.

---

# 51. Business Calendar Reference Data

Temporal modeling may require:

```text
holidays
festival periods
campaign calendars
```

These are reference/context data rather than transactional records.

---

# 52. Campaign Reference Data

A campaign definition might contain:

```text
campaign_id
start
end
affected categories
affected products
traffic effect
conversion effect
discount effect
```

The campaign model then applies these definitions to temporal behavior.

---

# 53. Reference Data Loading Strategies

Possible strategies:

### Static configuration

Best for:

```text
small development datasets
```

### File-based catalog

Best for:

```text
large configurable reference datasets
```

### Generated reference catalog

Best when:

```text
catalog itself must scale
```

### External source

Future only, if business requirements justify it.

---

# 54. Strategy Pattern

Reference loading is a legitimate Strategy boundary if multiple source types are required.

Conceptually:

```text
ReferenceDataLoader
  ├── ConfigReferenceDataLoader
  ├── CsvReferenceDataLoader
  └── JsonReferenceDataLoader
```

Only create these implementations when multiple sources actually exist.

---

# 55. Factory Pattern

A Factory can select the loader based on configuration.

For example:

```text
reference.source.type = csv
```

selects the CSV loader.

The application layer owns configuration interpretation.

---

# 56. Do Not Build a Generic Loader Framework

Avoid an abstract framework like:

```text
UniversalReferenceDataManager
```

with dozens of type parameters and callbacks.

Reference domains have different validation and indexing needs.

Prefer domain-specific loaders composed over common low-level utilities.

---

# 57. Common Parsing Utilities

Common utilities may handle:

```text
CSV parsing
JSON parsing
identifier parsing
numeric conversion
```

but they should remain infrastructure utilities.

They should not own domain meaning.

---

# 58. Loader Error Handling

Reference loading should distinguish:

```text
file not found
malformed source
invalid record
duplicate ID
missing relationship
```

The error-handling architecture defines the broader failure semantics.

---

# 59. Partial Reference Loading

By default, a reference loader should not silently skip invalid records.

Example:

```text
1,000 products
7 malformed
```

should not silently create a catalog of:

```text
993 products
```

unless partial loading is an explicit supported mode.

---

# 60. Strict vs Lenient Reference Loading

If lenient loading is ever supported, it must report:

```text
records read
records accepted
records rejected
rejection reasons
```

and should be explicit in configuration.

---

# 61. Reference Data Quality Scenarios

Dirty-data scenarios may intentionally corrupt reference data.

Examples:

```text
duplicate product
missing brand
invalid category
orphan geography
```

However, this is different from corrupting transactional output.

The scenario should identify which layer is being corrupted.

---

# 62. Reference Corruption Timing

The preferred conceptual sequence is:

```text
load clean reference
→ validate clean reference
→ apply intentional scenario corruption
→ validate expected dirty reference
→ generate transactions
```

This preserves a clean baseline and makes corruption explicit.

---

# 63. Reference Data and Clean Generation

Clean transactional generation should never depend on an already-invalid reference catalog unless the scenario explicitly requires it.

Otherwise one invalid reference can cascade into millions of unintended violations.

---

# 64. Reference Data and Scenario Cascades

A single dirty reference record can create many transactional defects.

For example:

```text
invalid Product
        ↓
OrderItem references Product
        ↓
many invalid relationships
```

This can be useful for a workload.

It can also make scenario interpretation difficult.

Therefore cascade behavior should be explicitly documented.

---

# 65. Reference Data and Skew

Catalog skew can be modeled at reference level.

Examples:

```text
few categories contain most products
few brands dominate catalog
```

This is different from demand skew:

```text
few products receive most order items
```

Both should be independently controllable.

---

# 66. Catalog Shape vs Demand Shape

A critical distinction:

```text
catalog shape
```

describes what exists.

```text
demand shape
```

describes what gets purchased.

A catalog may be evenly distributed while demand is highly skewed.

Do not conflate these.

---

# 67. Product Popularity

Product popularity is primarily transactional behavior.

It may consume:

```text
catalog structure
```

but should not mutate the reference catalog merely because a product becomes popular.

---

# 68. Reference Data and Customer Affinity

Customer preferences may reference catalog identities:

```text
preferred categories
preferred brands
preferred products
```

The CustomerBehaviorProfile should consume valid catalog IDs.

---

# 69. Catalog Changes During a Run

The preferred architecture is:

```text
catalog loaded once
catalog frozen
generation runs
```

Do not dynamically mutate catalog contents during a normal batch run.

Temporal product lifecycle can change **availability or demand**, not necessarily the underlying catalog definition.

---

# 70. Catalog Snapshot for Reproducibility

A generation run should record:

```text
catalog fingerprint
geography fingerprint
other reference fingerprints
```

This makes benchmark reproduction much stronger.

---

# 71. Reference Data Memory

Because reference data is reused heavily, it is appropriate to keep validated/indexed reference structures in memory.

However:

```text
catalog = 100M products
```

changes the design.

At very large catalog scale, the architecture may need:

```text
memory-mapped structures
external indexes
partitioned catalogs
```

This is future work.

---

# 72. Catalog vs Transaction Scaling

A useful benchmark matrix may vary both dimensions:

| Catalog | Transactions | Workload |
|---|---|---|
| small | small | development |
| small | huge | high reuse / hot keys |
| large | huge | broad joins |
| huge | huge | high-cardinality joins |

This is valuable for Spark experiments.

---

# 73. Small Catalog / Huge Transactions

This creates strong reuse:

```text
many OrderItems
→ relatively few Products
```

Useful for:

```text
joins
groupBy
Top-K
broadcast experiments
```

---

# 74. Large Catalog / Huge Transactions

This stresses:

```text
join size
lookup
shuffle
dimension-table handling
```

and avoids trivial broadcast assumptions.

---

# 75. Reference Data for Spark

Reference data can later become dimension tables:

```text
dim_customer
dim_product
dim_category
dim_brand
dim_geography
```

while:

```text
orders
order_items
events
```

act as fact/event tables.

The generator should naturally support this analytical shape.

---

# 76. Star-Schema Implication

The domain model is not required to become a warehouse schema.

However, its relationships naturally enable analytical models:

```text
Customer
Product
Category
Brand
Geography
        ↓
Order / OrderItem / Event
```

This is valuable for Spark workloads.

---

# 77. Slowly Changing Reference Data

Future scenarios may simulate catalog changes over time.

For example:

```text
product price changed
brand ownership changed
category hierarchy changed
```

This requires temporal reference-data versioning.

It is not required for the current baseline.

---

# 78. Reference Snapshots Over Time

A future temporal catalog could support:

```text
catalog snapshot at T1
catalog snapshot at T2
```

Then transactions resolve against the correct historical state.

This is an advanced capability and should be implemented only when needed.

---

# 79. Reference Data and Temporal Modeling

The time architecture and reference architecture meet at:

```text
effective_from
effective_to
```

where temporal reference data is introduced.

The reference domain owns the definition.

The temporal architecture owns interpretation of time.

---

# 80. Performance

Reference data should be optimized for repeated reads.

Good practices:

```text
load once
validate once
index once
reuse
```

Avoid:

```text
parse source
for every transaction
```

---

# 81. Reference Lookup Complexity

Desired behavior:

```text
Product by ID                 → O(1) average
Category by ID               → O(1) average
Brand by ID                  → O(1) average
Building by ID               → O(1) average
Postal codes by area         → O(1) average lookup + selection
Products by category         → indexed lookup
```

Exact structures depend on implementation.

---

# 82. Precomputed Probability Structures

If product/category/brand selection uses static reference data, precompute:

```text
weights
cumulative distributions
alias tables
```

where beneficial.

This belongs to the distribution/selection layer, not the raw reference loader.

---

# 83. Immutability and Concurrency

Immutable reference data makes:

```text
parallel customer generation
parallel order generation
parallel address generation
```

much safer.

This is a major reason to freeze reference data before transaction generation.

---

# 84. Reference Data Testing

Reference components require tests for:

```text
loading
normalization
IDs
duplicates
missing references
hierarchy closure
lookup
index correctness
deterministic ordering
fingerprinting
```

---

# 85. Geography Testing

Examples:

```text
building resolves to society
society resolves to road
road resolves to area
area resolves to city
city resolves to state
state resolves to country
postal code resolves to area
```

---

# 86. Catalog Testing

Examples:

```text
product resolves to category
product resolves to brand
productsByCategory matches product.categoryId
productsByBrand matches product.brandId
```

Indexes must not disagree with primary maps.

---

# 87. Property Testing

Useful properties:

```text
every indexed product exists in productById
```

```text
every product in productsByCategory has that category ID
```

```text
every geography index result resolves through the hierarchy
```

---

# 88. Deterministic Ordering

Where reference collections are sampled by index, define deterministic ordering explicitly.

For example:

```scala
products.sortBy(_.id)
```

before creating a sampler.

This prevents map-order changes from changing generated datasets.

---

# 89. Reference Data and Randomness

Reference data itself should not consume random numbers merely to determine deterministic ordering.

Random selection should occur in the sampling layer.

This separation improves reproducibility.

---

# 90. Domain Boundaries

A clean dependency direction is:

```text
Source
  ↓
Loader
  ↓
Validated Reference Model
  ↓
Domain Selection/Behavior
  ↓
Transaction Generator
```

Not:

```text
Transaction Generator
→ parses CSV
→ validates CSV
→ builds indexes
→ chooses products
```

---

# 91. Application Orchestration

The application should construct the reference world once:

```text
load
→ validate
→ index
→ freeze
```

then pass it into generation context.

---

# 92. Generation Context

A future `GenerationContext` may contain:

```text
reference data
time model
scenario
random root
effective configuration
```

It should provide access to immutable dependencies without becoming a god object.

---

# 93. Avoid Context Explosion

Do not put every possible service into:

```scala
GenerationContext
```

For example:

```text
logger
writer
validator
statistics
metrics
configuration parser
```

should not automatically belong there.

Only shared generation dependencies belong in the context.

---

# 94. SOLID

## Single Responsibility

Loaders load.

Validators validate.

Indexes provide efficient access.

Catalog models describe reference entities.

## Open/Closed

New source formats can be introduced without changing catalog semantics.

## Liskov Substitution

Alternative loaders should produce equivalent validated reference contracts.

## Interface Segregation

Avoid giant reference-data interfaces.

## Dependency Inversion

Generators depend on domain-facing reference contracts rather than raw files.

---

# 95. Strategy Pattern

Appropriate Strategy examples:

```text
ReferenceDataSource
ProductSelectionModel
CatalogScalingModel
```

when multiple implementations genuinely exist.

---

# 96. Factory Pattern

Factories are appropriate for:

```text
selecting a reference-data loader
selecting a product sampler
building configured catalog components
```

---

# 97. Builder Pattern

A Builder is generally unnecessary for simple immutable reference models.

Scala case classes already provide concise construction.

A Builder may become appropriate if constructing a large immutable catalog requires many validated optional components.

Do not introduce one preemptively.

---

# 98. Domain Services

A domain service can be useful for operations spanning reference entities.

Example:

```text
CatalogResolver
```

could resolve:

```text
product
→ category
→ brand
```

if that operation is repeatedly needed.

However, avoid creating a generic service simply to wrap maps.

---

# 99. Readability

Prefer:

```scala
catalog.productsForCategory(category.id)
```

over:

```scala
catalog.indexes
  .get("productsByCategory")
  .get(category.id)
  .toSeq
```

The first communicates business meaning.

The second exposes implementation.

---

# 100. Error Handling

Reference failures should contain domain context.

Example:

```text
Product PRODUCT_123 references missing Brand BRAND_99
```

is better than:

```text
Key not found
```

---

# 101. Reference Data and Output

Reference data may itself be written as dataset entities:

```text
brands.csv
categories.csv
products.csv
```

The output layer should serialize the validated reference model.

It should not reread the source file.

---

# 102. Reference Data and Manifest

The manifest should eventually report:

```text
reference sources
reference versions
reference fingerprints
reference record counts
```

This makes the generated dataset auditable.

---

# 103. Catalog Statistics

Useful catalog statistics:

```text
category count
brand count
product count
products/category
products/brand
price distribution
brand-category coverage
```

These should be generated before or alongside transactional statistics.

---

# 104. Reference Statistics vs Transaction Statistics

Do not mix:

```text
catalog distribution
```

with:

```text
transaction demand distribution
```

For example:

```text
Brand A owns 20% of products
```

does not mean:

```text
Brand A receives 20% of sales
```

Demand can be intentionally skewed.

---

# 105. Catalog Skew

Catalog skew might mean:

```text
few categories contain many products
```

or:

```text
few brands have broad catalogs
```

This is structural skew.

It should be measurable separately from transaction hot-key skew.

---

# 106. Demand Skew

Demand skew means:

```text
few products receive many purchases
```

This is behavioral/transactional skew.

Both can be combined:

```text
catalog skew
+
demand skew
```

for difficult Spark workloads.

---

# 107. Geographic Reference Skew

Geography can also be structurally skewed:

```text
few cities
→ many areas
→ many buildings
```

This can influence address generation and join behavior.

---

# 108. Reference Data and Workload Design

Reference architecture should make it possible to create workload families such as:

```text
high product reuse
large product dimension
high category skew
high brand skew
deep geography
wide geography
```

This makes the dataset generator a controlled workload-design tool rather than only a row generator.

---

# 109. Current Simplifications

The current project intentionally keeps reference architecture relatively simple.

Examples:

- geography is represented through a reference hierarchy;
- Address does not duplicate city/state/country fields;
- catalog size is currently modest;
- reference data is primarily local/static;
- temporal reference versioning is limited;
- external reference sources are not required;
- catalog lifecycle is not fully modeled.

These are conscious scope decisions.

---

# 110. Future Realism

Future reference-data realism may add:

```text
category hierarchy
product lifecycle
catalog versioning
historical pricing
regional availability
carrier reference data
campaign reference data
business calendars
larger catalogs
reference-data snapshots
```

Each addition should have a clear business or workload reason.

---

# 111. Recommended Implementation Sequence

Reference architecture should be strengthened in this order:

```text
1. identify all current reference data
2. separate loaders from validated models
3. centralize reference validation
4. build immutable indexed representations
5. remove repeated linear lookups
6. expose business-oriented lookup APIs
7. add reference fingerprints
8. connect catalog selection models
9. support configurable catalog scale
10. add catalog statistics
11. add structural skew controls
12. add richer reference sources only when needed
13. add temporal reference snapshots if required
```

---

# 112. Quality Gate

Before reference architecture is considered mature:

```text
□ reference sources are explicit
□ loaders are separated from validation
□ identifiers are unique and stable
□ referential closure is validated
□ validated reference data is immutable
□ important lookups are indexed
□ indexes preserve domain invariants
□ deterministic ordering is explicit
□ reference fingerprints are available
□ catalog and demand distributions are distinct
□ reference statistics are measurable
□ reference loading failures are explicit
□ partial loading is never silent
□ reference data can be reused safely by generators
□ architecture does not depend on Spark
```

---

# 113. Definition of Done

The reference-data architecture is complete when:

### Correctness

All required reference relationships are valid before transaction generation.

### Performance

Repeated transactional lookups do not require repeated source parsing or linear scans.

### Reproducibility

Reference content and ordering contribute deterministically to generation.

### Maintainability

Each domain owns its reference semantics.

### Extensibility

New reference sources can be added without changing transactional business logic.

### Workload Design

Catalog size and shape can be controlled independently from transaction volume.

### Observability

Reference versions, fingerprints, counts, and important statistics can be recorded.

---

# 114. Explicit Architecture Decisions

### Decision 1

Reference data defines the business world; transactional data describes activity within that world.

### Decision 2

Reference data should be loaded, validated, indexed, and frozen before high-volume generation.

### Decision 3

Reference data should be immutable during a normal generation run.

### Decision 4

Reference IDs must be stable, unique, and deterministic.

### Decision 5

Referential closure is a precondition for clean transaction generation.

### Decision 6

Domain-specific reference components should expose business-oriented lookup operations.

### Decision 7

Internal indexes should remain encapsulated.

### Decision 8

Catalog shape and transaction demand shape are separate dimensions.

### Decision 9

Reference fingerprints should contribute to dataset reproducibility.

### Decision 10

Reference-data source selection may use Strategy and Factory when multiple sources genuinely exist.

### Decision 11

A generic UniversalReferenceDataManager should not be introduced.

### Decision 12

Reference loading should not silently discard invalid records.

### Decision 13

Reference data may be materialized in memory when scale permits; very large catalogs may require different storage/indexing later.

### Decision 14

The generator remains independent of Spark.

### Decision 15

Reference architecture should support both realistic business data and deliberate Spark workload construction.

---

# 115. Final Design Summary

The target architecture is:

```text
                Reference Sources
                 /      |      \
              HOCON    CSV     JSON
                 \      |      /
                      Loader
                        |
                        v
                 Raw Reference Data
                        |
                        v
                 Reference Validation
                        |
              +---------+---------+
              |                   |
           invalid              valid
              |                   |
             FAIL                 v
                         Normalized Reference
                                  |
                                  v
                         Immutable Indexes
                                  |
                 +----------------+----------------+
                 |                |                |
             Geography          Catalog       Business Context
                 |                |                |
             Buildings      Category/Brand    Calendar/Campaign
             Postal Codes       Product              |
                 |                |                |
                 +----------------+----------------+
                                  |
                           Generation Context
                                  |
                 +----------------+----------------+
                 |                |                |
             Customers         Addresses       Transactions
                                                  |
                                    +-------------+-------------+
                                    |             |             |
                                  Orders       Sessions       Events
                                    |
                         +----------+----------+
                         |          |          |
                     OrderItems   Payment   Shipment
                                                   |
                                                Return
```

The essential separation is:

```text
REFERENCE WORLD
    defines what exists

GENERATION LOGIC
    defines what happens

OUTPUT
    defines how it is stored
```

A strong reference architecture gives the rest of ShopSphere a stable foundation.

For example:

```text
ProductGenerator
```

should not need to know whether the product catalog came from:

```text
HOCON
CSV
JSON
```

It should receive a validated catalog.

Likewise:

```text
AddressGenerator
```

should consume the validated geography hierarchy rather than parsing geography files itself.

This separation improves:

```text
correctness
performance
reproducibility
testability
parallelism
```

and makes future catalog scaling possible.

The most important architectural principle is:

> **Load the business world once, validate it completely, index it for the access patterns the business requires, freeze it, and let high-volume generation operate against that stable world.**

That principle becomes increasingly important as ShopSphere moves from a 1,000-customer development dataset toward large and xlarge benchmark datasets.

The next architecture layer can therefore build on a stable reference world rather than repeatedly reconstructing business definitions inside transaction generators.
