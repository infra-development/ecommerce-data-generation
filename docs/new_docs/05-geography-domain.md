# 05 — Geography Domain

## 1. Purpose

This document defines the business meaning, domain model, hierarchy, reference-data responsibilities, lookup behavior, generation boundaries, validation rules, technical architecture, and future realism roadmap for the **Geography** domain in the ShopSphere e-commerce data generator.

Geography is a foundational reference domain.

It provides the physical hierarchy required by other domains, especially:

- Address,
- Customer,
- Order,
- Shipment,
- future geographic behavior models,
- future geographic skew scenarios.

The central architectural principle is:

> **Geography owns the geographic world; other domains reference it.**

---

# 2. Business Meaning

Geography represents the physical location structure in which ShopSphere customers, addresses, and downstream commerce activity exist.

The current hierarchy is:

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

Postal codes are associated with Areas.

The hierarchy provides a consistent foundation for generating physically plausible addresses.

---

# 3. Why Geography Is a Separate Domain

Geography is reference/master data rather than transactional data.

It answers questions such as:

```text
Which state contains this city?
Which area contains this road?
Which society contains this building?
Which area owns this postal code?
Which building belongs to this city?
```

These relationships should not be reconstructed independently by every generator.

Without a centralized Geography domain, generators could independently produce combinations such as:

```text
Building from City A
Postal Code from City B
```

or:

```text
Society from Area X
Building from Area Y
```

The Geography domain prevents this class of inconsistency.

---

# 4. Domain Ownership

## Geography owns

- geographic entities,
- hierarchy relationships,
- geographic reference-data loading,
- hierarchy resolution,
- postal-code mappings,
- building metadata,
- unit/floor metadata,
- stable lookup operations,
- geographic reference validation.

## Address owns

- customer-to-address association,
- selected building reference,
- selected unit,
- address record identity,
- address-specific generation.

## Customer owns

- customer behavior,
- customer lifecycle,
- customer-level geographic preference,
- customer identity.

Geography should not contain customer behavior.

---

# 5. Hierarchy

The canonical hierarchy is:

```text
Country
   │
   └── State
         │
         └── City
               │
               └── Area
                     │
                     └── Road
                           │
                           └── Society
                                 │
                                 └── Building
                                       │
                                       └── Flat / Unit
```

Postal code:

```text
Area
  └── Postal Code(s)
```

The hierarchy should be treated as a graph with well-defined parent relationships rather than as a collection of unrelated lookup tables.

---

# 6. Country

A Country is the top-level geographic container.

Conceptually it may contain:

```text
id
name
```

The current project does not require a rich country model.

Country mainly provides hierarchy context.

Example:

```text
COUNTRY_IN
India
```

The exact reference-data schema remains an implementation concern of the Geography domain.

---

# 7. State

A State belongs to exactly one Country.

Conceptual relationship:

```text
Country 1
   ↓
State N
```

Example:

```text
India
  └── Maharashtra
```

The State entity should reference its parent Country through an identifier.

---

# 8. City

A City belongs to a State.

Conceptual relationship:

```text
State 1
   ↓
City N
```

Example:

```text
Maharashtra
  ├── Mumbai
  ├── Pune
  └── Nagpur
```

A City should not independently claim a state through duplicated text.

Its parent relationship is the authoritative source.

---

# 9. Area

An Area belongs to a City.

The Area is particularly important because postal codes are associated with Areas.

Conceptually:

```text
City
  ↓
Area
  ├── postal code(s)
  ├── road(s)
  └── ...
```

---

# 10. Road

A Road belongs to an Area.

Conceptually:

```text
Area
  ↓
Road
```

Roads provide additional geographic realism without requiring Address to duplicate them.

---

# 11. Society

A Society belongs to a Road or the appropriate lower-level geographic parent represented by the reference data.

It provides a grouping for buildings.

Conceptually:

```text
Road
  ↓
Society
  ↓
Building
```

The exact parent mapping should follow the loaded reference-data schema.

The Geography domain must not infer relationships from names.

---

# 12. Building

Building is the most important Geography entity for the current Address generator.

A building contains metadata needed to generate a valid unit.

Current Address generation uses:

```text
building.id
building.floors
building.unitsPerFloor
```

The building therefore provides the structural constraints for unit generation.

---

# 13. Flat / Unit

The current project does not maintain a separate persistent Unit entity.

Instead, an Address derives a unit number from:

```text
building
+
floor
+
unit position
```

For example:

```text
Building
  floors = 10
  unitsPerFloor = 4

floor = 3
unitOnFloor = 2

→ unit number generated according to building convention
```

This is an intentional simplification.

A future design may introduce explicit units if the business model requires them.

---

# 14. Postal Code

Postal codes are associated with Areas.

The current Address generator resolves:

```text
building
    ↓
building hierarchy
    ↓
area
    ↓
postal codes
```

and chooses a postal code belonging to that Area.

Therefore:

> Postal-code validity is a geographic relationship, not an independent random choice.

---

# 15. Current Reference Data

The project contains geographic reference data used by the generator.

The reference data supports the hierarchy needed to resolve:

```text
building
→ society
→ road
→ area
→ city
→ state
→ country
```

and:

```text
area
→ postal code(s)
```

The exact file layout should remain an implementation detail behind the Geography API.

Other domains should not parse raw Geography files directly.

---

# 16. Reference Data as a Domain Boundary

A generator should not do this:

```scala
parseCsv("buildings.csv")
```

inside Address generation.

Instead:

```scala
geography.buildings
geography.resolveBuilding(buildingId)
geography.postalCodesForArea(areaId)
```

should be the conceptual boundary.

This provides:

- centralized loading,
- validation,
- stable lookup behavior,
- testability,
- protection from raw file-format changes.

---

# 17. Geography Reference API

The current implementation already exposes operations conceptually equivalent to:

```scala
geography.buildings
geography.resolveBuilding(buildingId)
geography.postalCodesForArea(areaId)
```

These operations form an important domain boundary.

Potential future API:

```scala
findCountry(id)
findState(id)
findCity(id)
findArea(id)
findRoad(id)
findSociety(id)
findBuilding(id)
postalCodesForArea(areaId)
resolveBuilding(buildingId)
```

The API should grow only when consumers genuinely require the operation.

---

# 18. Lookup vs Generation

Geography is primarily a **reference domain**, not a transactional generator.

There is an important distinction:

```text
Reference loading
    ≠
Address generation
```

Geography loads and validates the physical world.

Address chooses from that world.

For example:

```text
Geography:
"What buildings exist?"

Address:
"Which building should this customer receive?"
```

This separation is fundamental.

---

# 19. Geography Does Not Choose Customer Locations

The Geography domain should not decide:

```text
Which city this customer prefers
```

That is customer behavior.

Instead:

```text
CustomerBehaviorProfile
        ↓
geographic preference
        ↓
Address / customer-location selection
        ↓
Geography lookup
```

Geography supplies valid choices.

Behavior models decide preference.

---

# 20. Current Address Integration

The current Address generator effectively performs:

```text
1. obtain all buildings
2. choose a building deterministically
3. choose floor
4. choose unit position
5. resolve building hierarchy
6. obtain postal codes for the Area
7. choose postal code
8. construct Address
```

Geography supplies steps:

```text
1
5
6
```

Address owns the business selection and construction logic.

---

# 21. Stable Ordering

Reference collections should have deterministic ordering.

The current Address generator uses:

```scala
.sortBy(_.id)
```

before selecting randomly.

This matters because reproducibility depends not only on the seed but also on stable iteration order.

The Geography domain should therefore provide stable collections or clearly document ordering guarantees.

---

# 22. Deterministic Reference Data

Given identical reference data:

```text
same reference data
+
same seed
+
same configuration
```

the generator should produce the same logical output.

If reference data changes, reproducibility should not silently be expected across versions.

The generated manifest should eventually record enough information to identify the reference-data version.

---

# 23. Reference Data Validation

Geography should validate its own reference data before generation begins.

Important checks include:

## ID uniqueness

Every geographic entity ID must be unique within its entity type.

---

## Parent existence

Every child should reference an existing parent.

Examples:

```text
state.countryId exists
city.stateId exists
area.cityId exists
road.areaId exists
society.roadId exists
building.societyId exists
```

The exact fields depend on the reference-data model.

---

## No broken hierarchy

A building should be resolvable all the way to:

```text
Country
State
City
Area
Road
Society
```

where those hierarchy levels are required.

---

## Postal-code validity

Every postal code should belong to a valid Area.

---

## Building constraints

Building metadata should satisfy:

```text
floors > 0
unitsPerFloor > 0
```

or whatever constraints the chosen reference schema defines.

---

# 24. Failure Philosophy

Geography should fail fast when its reference world is invalid.

A broken reference dataset should not be allowed to produce thousands or millions of invalid Addresses.

The preferred sequence is:

```text
Load reference data
      ↓
Validate reference data
      ↓
Fail if invalid
      ↓
Start generation
```

rather than:

```text
Start generation
      ↓
Discover broken reference data
      ↓
Partially generate dataset
      ↓
Fail late
```

---

# 25. Current Technical Architecture

The target package is:

```text
com.shopsphere.datagenerator.geography/
├── model/
├── reference/
└── loader/
```

Potential responsibilities:

### `geography.model`

Geographic entities and value structures.

### `geography.reference`

In-memory reference representation and lookup APIs.

### `geography.loader`

Parsing/loading external reference data.

This structure may evolve if the implementation reveals a better boundary.

---

# 26. Why Loader Is Separate

The Geography domain should distinguish:

```text
external representation
        ↓
loader
        ↓
domain/reference model
        ↓
lookup
```

For example, if source data changes from CSV to JSON, Address should not need to change.

The loader absorbs the external representation.

---

# 27. Model vs Reference Store

A useful conceptual separation is:

```text
Geographic entity
    =
business/reference model

GeographyReferenceData
    =
in-memory reference world
```

For example:

```scala
case class Building(...)
```

represents a building.

Whereas:

```scala
GeographyReferenceData(...)
```

represents the collection and indexes needed to resolve geographic relationships efficiently.

---

# 28. Indexing

Because Geography is read-heavy during generation, reference data should provide efficient lookup.

Potential indexes:

```text
statesByCountry
citiesByState
areasByCity
roadsByArea
societiesByRoad
buildingsBySociety
postalCodesByArea
```

The exact implementation may use immutable Maps.

The important design goal is:

> Consumers should not repeatedly scan the complete reference dataset for every Address.

---

# 29. Current Performance Consideration

The current Address generator obtains:

```scala
geography.buildings.values.toSeq.sortBy(_.id)
```

This is readable and acceptable for the current small baseline.

At large scale, repeatedly converting and sorting the same reference collection would be unnecessary work.

A future Geography reference API should expose a stable pre-indexed sequence:

```scala
geography.buildingsInStableOrder
```

or equivalent.

The optimization should happen behind the Geography boundary.

The Address generator should remain readable.

---

# 30. Avoid Premature Optimization

Do not optimize Geography solely because a Map or cache sounds more sophisticated.

First establish:

```text
correct hierarchy
+
correct lookups
+
clear API
+
stable behavior
```

Then profile generation.

Optimization should be driven by actual generation cost.

---

# 31. Geography as Immutable Reference Data

The preferred design is immutable in-memory reference data.

Conceptually:

```text
load once
   ↓
validate once
   ↓
share read-only
   ↓
generate many records
```

This is appropriate because geography is reference/master data during a generation run.

There should be no need for mutable global geographic state.

---

# 32. Dependency Injection

Application startup should construct Geography once.

Conceptually:

```scala
val geography = GeographyLoader.load(...)
```

then inject it into the generation context.

Generators receive the reference data they require.

This avoids:

```scala
GlobalGeography.current
```

or other hidden global state.

---

# 33. Strategy Pattern Assessment

Geography itself does not currently require a Strategy abstraction.

There may eventually be alternative geographic selection strategies, but those belong primarily to consumers such as Address or Customer behavior.

For example:

```text
UniformBuildingSelection
CustomerGeographyAffinity
ScenarioGeographySkew
```

These are **selection behaviors**, not alternative definitions of Geography.

That distinction should be preserved.

---

# 34. Factory Pattern Assessment

A Geography factory is not necessary for simple immutable reference models.

A loader may act as the construction boundary:

```text
GeographyLoader
```

If multiple reference-data sources eventually exist, a source-selection factory could become useful.

For example:

```text
CsvGeographySource
JsonGeographySource
DatabaseGeographySource
```

But this should be introduced only if multiple sources are actually supported.

---

# 35. Builder Pattern Assessment

A builder is not necessary for individual geography records if they remain simple case classes.

Loading an entire reference graph is more complex, but that complexity is better handled through:

```text
loader
+
validation
+
index construction
```

rather than a large mutable builder.

---

# 36. Domain Services

A Geography domain service may be appropriate for operations that genuinely span multiple geographic entities.

Examples:

```text
resolveBuildingHierarchy
findPostalCodesForArea
validateHierarchy
```

However, if these are simple lookups, methods on the reference representation are clearer.

Do not create a service layer simply to wrap Maps.

---

# 37. SOLID Assessment

## Single Responsibility

Preferred separation:

```text
Loader
    → loads reference data

Validator
    → validates reference data

ReferenceData
    → provides lookup

Consumer generator
    → selects business-specific locations
```

---

## Open/Closed

New reference-data sources should be supportable without changing every consumer.

---

## Liskov Substitution

Relevant only if multiple reference-source implementations are introduced.

---

## Interface Segregation

Do not expose one enormous:

```scala
trait GeographyEverything
```

interface.

Consumers should depend on the smallest meaningful boundary.

---

## Dependency Inversion

Consumers should depend on the Geography reference abstraction rather than raw source files.

---

# 38. Address Relationship

The Address domain depends on Geography.

Conceptually:

```text
AddressGenerator
       ↓
GeographyReferenceData
       ↓
Building
       ↓
Area
       ↓
Postal Code
```

The Address generator should never need to understand how the reference data was loaded.

---

# 39. Customer Relationship

Future customer behavior may include geography affinity.

For example:

```text
CustomerBehaviorProfile
    preferredCity
    preferredArea
```

The behavior model should then ask Geography for valid candidates.

Geography remains authoritative for whether those candidates actually exist.

---

# 40. Order Relationship

Orders may eventually contain geographic behavior such as:

- delivery location,
- shipping region,
- fulfillment region.

These should reference Geography through appropriate domain relationships.

Geography should not know about Orders.

---

# 41. Shipment Relationship

Shipment may eventually use geographic attributes to model:

- delivery distance,
- estimated delivery time,
- regional fulfillment,
- shipping cost.

These are Shipment/business behaviors.

Geography should provide the physical facts needed by those models.

---

# 42. Geographic Affinity

A future behavioral model may represent:

```text
country affinity
state affinity
city affinity
area affinity
```

The model should not contain duplicated geographic records.

Instead:

```text
affinity
   ↓
Geography lookup
   ↓
valid geographic entity
```

This preserves domain ownership.

---

# 43. Geographic Skew

Geography is a natural foundation for controlled skew scenarios.

Examples:

## City skew

A small number of cities contain most customers.

```text
City A → 40%
City B → 25%
City C → 15%
Others → 20%
```

---

## Area skew

A few Areas contain most addresses.

---

## Building skew

A small number of buildings contain disproportionately many addresses.

---

## Postal-code skew

A few postal codes dominate the generated dataset.

These are valuable for Spark experiments involving:

- groupBy,
- joins,
- repartitioning,
- partition skew,
- aggregation hotspots.

---

# 44. Uniform vs Skewed Geography

The clean baseline should have a clearly defined geographic distribution.

A scenario should explicitly modify it.

For example:

```text
Baseline
    → broadly distributed

HotCities
    → concentrated city distribution

HotAreas
    → concentrated area distribution
```

Avoid scattering conditions throughout AddressGenerator.

Scenario behavior should be composed into the selection model.

---

# 45. Geographic Realism

A realistic geographic model does not require a perfect map of the world.

It requires:

```text
hierarchical consistency
+
plausible concentration
+
stable relationships
+
usable reference metadata
```

The project is a synthetic generator, not a GIS product.

The objective is to create a useful e-commerce geographic world.

---

# 46. Temporal Geography

Geography itself is relatively stable during a generation run.

However, future scenarios may model:

- new buildings,
- new areas,
- changing postal mappings,
- expansion into cities.

These are likely better represented as versioned reference-data scenarios rather than random mutations inside each Address generation.

---

# 47. Versioning

Future generation manifests should ideally record:

```text
geography reference-data version
```

This makes reproducibility more robust.

A dataset should be reproducible from:

```text
generator version
+
configuration
+
seed
+
reference-data version
+
scenario
```

rather than only:

```text
seed
```

---

# 48. Testing Strategy

## 48.1 Model tests

Verify:

- geographic entity fields,
- equality,
- valid parent identifiers.

---

## 48.2 Loader tests

Verify:

- valid reference data loads,
- malformed records fail,
- missing required fields fail,
- duplicate IDs fail.

---

## 48.3 Hierarchy tests

Verify:

```text
Country → State
State → City
City → Area
Area → Road
Road → Society
Society → Building
```

and all required reverse/indexed lookups.

---

## 48.4 Postal tests

Verify:

```text
postal code → Area
Area → postal codes
```

and that all returned postal codes belong to the requested Area.

---

## 48.5 Building tests

Verify:

```text
building → full hierarchy
building → valid floor metadata
building → valid unit metadata
```

---

# 49. Property-Style Tests

Useful properties include:

```text
every State references an existing Country
every City references an existing State
every Area references an existing City
every Road references an existing Area
every Society references an existing Road
every Building references an existing Society
every postal code references an existing Area
```

Additionally:

```text
resolveBuilding(buildingId)
```

should return a hierarchy whose IDs match the building's actual ancestry.

---

# 50. Reference-Data Quality vs Generated-Data Quality

These are different concerns.

## Reference-data quality

Checks the correctness of the geographic master/reference world.

## Generated-data quality

Checks whether generated Addresses or other entities obey that world.

Pipeline:

```text
Reference Data
      ↓
Reference Validation
      ↓
Valid Geography World
      ↓
Entity Generation
      ↓
Generated Data Validation
```

This distinction should remain explicit.

---

# 51. Current Simplifications

The current Geography domain is intentionally simplified.

## Simplification 1

The project does not attempt to model every geographic feature in the real world.

---

## Simplification 2

The current model does not require coordinates.

No latitude/longitude fields are necessary unless future use cases require them.

---

## Simplification 3

The current model does not implement GIS geometry.

---

## Simplification 4

The current model does not simulate geographic movement.

---

## Simplification 5

The current model does not model real postal-system history.

---

# 52. Planned Future Capabilities

Potential future extensions:

```text
latitude / longitude
distance calculations
geographic zones
delivery regions
regional fulfillment centers
customer geographic affinity
geographic popularity
city-level demand
area-level demand
building-level hotspots
temporal geographic expansion
```

These should only be added when downstream business behavior requires them.

---

# 53. Geographic Value Objects

Future implementation may benefit from value objects such as:

```text
CountryId
StateId
CityId
AreaId
RoadId
SocietyId
BuildingId
PostalCode
```

However, these should not be introduced merely to create more types.

The benefit must justify the added code.

The current project can continue with typed identifiers as simple strings where appropriate.

---

# 54. Raw String vs Strong Types

There is a tradeoff.

Current approach:

```scala
buildingId: String
```

is simple and readable.

A future strongly typed model might use:

```scala
BuildingId
CustomerId
PostalCode
```

This could prevent accidental mixing of IDs.

However, introducing wrappers across every entity can create significant migration cost.

This decision should be revisited when the domain architecture stabilizes.

---

# 55. Performance Considerations for Large Datasets

Geography itself may be relatively small compared with generated transaction data.

Therefore the main concern is not storing geography but using it efficiently.

The preferred model is:

```text
load once
index once
reuse many times
```

For millions or billions of generated records, repeated raw parsing is unacceptable.

---

# 56. Thread Safety

If generation becomes parallelized in the future, Geography reference data should remain safely shareable.

Immutable reference data naturally supports this.

Randomness should remain separate and deterministic per generation context.

Do not introduce mutable geographic state to coordinate generators.

---

# 57. Concurrency

A future high-throughput generator may generate Addresses concurrently.

The Geography design should support concurrent read access without synchronization-heavy mutable structures.

This reinforces the preference for immutable Maps and sequences.

---

# 58. Observability

Geography-related generation metrics may eventually include:

```text
addresses by country
addresses by state
addresses by city
addresses by area
addresses by building
```

Reference-data diagnostics may include:

```text
countries loaded
states loaded
cities loaded
areas loaded
roads loaded
societies loaded
buildings loaded
postal codes loaded
```

These metrics should be exposed by observability/statistics infrastructure rather than embedded in Geography lookup methods.

---

# 59. Manifest Information

The generation manifest should eventually record:

```text
geography reference-data version
geography record counts
active geographic scenario
```

This helps reproduce and explain a generated dataset.

---

# 60. Geography and Spark Workloads

Geography should eventually enable realistic Spark datasets for:

## Geographic aggregation

```text
orders by city
customers by state
revenue by postal code
```

## Join workloads

```text
Customer
  JOIN Address
  JOIN Geography
```

## Skew workloads

```text
hot cities
hot areas
hot buildings
```

## Partitioning workloads

```text
partition by city
partition by postal code
partition by state
```

## Filtering workloads

```text
customers in selected cities
orders in selected regions
```

This downstream purpose should influence design, but should not turn Geography into a Spark-specific implementation.

---

# 61. What Geography Should Not Solve

Geography should not own:

```text
customer location preference
order frequency
product demand
delivery probability
shipment status
CSV writing
global generation orchestration
customer generation
scenario orchestration
data corruption
```

It provides the physical reference world.

Other domains decide how that world is used.

---

# 62. Migration Plan

When Geography is migrated into the target package structure:

## Step 1

Identify current geography model classes.

---

## Step 2

Identify current reference-data loaders.

---

## Step 3

Identify current hierarchy-resolution logic.

---

## Step 4

Move geographic models into:

```text
geography/model
```

---

## Step 5

Move reference representation into:

```text
geography/reference
```

---

## Step 6

Move loaders into:

```text
geography/loader
```

---

## Step 7

Create explicit lookup boundaries.

---

## Step 8

Move reference-data validation into the Geography boundary where appropriate.

---

## Step 9

Update Address imports.

---

## Step 10

Run Geography tests.

---

## Step 11

Run Address tests.

---

## Step 12

Run the complete test suite.

---

## Step 13

Run the end-to-end generator.

---

## Step 14

Compare geographic statistics with the pre-migration baseline.

---

# 63. Migration Quality Gate

Geography migration is complete only when:

### Business

- hierarchy is clearly defined,
- ownership is clear.

### Reference data

- loading works,
- validation works,
- indexes work.

### API

- consumers use Geography boundaries,
- raw file parsing is not leaked.

### Reproducibility

- ordering is stable,
- generation behavior is preserved.

### Address integration

- building lookup works,
- postal lookup works,
- Address generation remains valid.

### Testing

- Geography tests pass,
- Address tests pass,
- full suite passes.

---

# 64. Design Decisions

## Decision A — Geography is reference/master data

It represents the physical world used by transactional domains.

---

## Decision B — Geography owns hierarchy

Country through Building relationships belong to Geography.

---

## Decision C — Postal codes belong to Areas

Address obtains postal codes through the building's Area.

---

## Decision D — Address references Geography

Address does not duplicate the complete geographic hierarchy.

---

## Decision E — Reference data is loaded once

The preferred runtime model is immutable, validated, reusable reference data.

---

## Decision F — Stable ordering supports reproducibility

Reference collections must have deterministic ordering where random selection depends on them.

---

## Decision G — Geographic preference belongs to behavior

Customer-specific location preferences are not Geography concerns.

---

## Decision H — Avoid premature GIS complexity

Coordinates, geometry, routing, and distance calculations are future capabilities, not current requirements.

---

# 65. Final Mental Model

The easiest way to understand the Geography domain is:

```text
                    GEOGRAPHY
                        │
        ┌───────────────┴───────────────┐
        │                               │
   Hierarchy                        Postal Data
        │                               │
        ▼                               ▼
Country → State → City → Area      Area → Postal Code
                        │
                        ▼
                      Road
                        │
                        ▼
                     Society
                        │
                        ▼
                    Building
                        │
                        ▼
                  Unit structure
```

Consumers then use this world:

```text
Customer Behavior
       │
       ▼
Location Preference
       │
       ▼
Address Generator
       │
       ▼
Geography Lookup
       │
       ▼
Valid Address
```

---

# 66. Summary

Geography is one of the most important foundational domains in ShopSphere because it defines the physical world in which customer addresses and future location-aware behavior exist.

Its responsibility is deliberately narrow:

> **Load, validate, represent, and resolve the geographic hierarchy.**

It should not decide how customers behave within that geography.

The current hierarchy is:

```text
Country
→ State
→ City
→ Area
→ Road
→ Society
→ Building
→ Flat / Unit
```

with postal codes associated with Areas.

The key architectural boundary is:

```text
External reference files
        ↓
Geography Loader
        ↓
Validated Geography Reference Data
        ↓
Geography Lookup API
        ↓
Address / Customer / Order / Shipment
```

The future evolution should be:

```text
correct hierarchy
        ↓
efficient reference lookup
        ↓
customer geography affinity
        ↓
geographic concentration/skew
        ↓
temporal geographic behavior
        ↓
delivery/fulfillment realism
```

The Geography domain should remain simpler than the behavioral domains.

Its quality comes from being **authoritative, immutable, validated, deterministic, efficiently queryable, and easy for other domains to consume**.
