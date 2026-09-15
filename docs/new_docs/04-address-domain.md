# 04 — Address Domain

## 1. Purpose

This document defines the business meaning, domain model, generation strategy, validation rules, configuration boundaries, technical responsibilities, and future realism roadmap for the **Address** domain in the ShopSphere e-commerce data generator.

The Address domain is intentionally small.

Its responsibility is to represent a customer's physical delivery/location reference while relying on the Geography domain for the actual geographic hierarchy.

The central design decision is:

> **Address stores the customer's relationship to a physical building/unit and postal code; Geography owns the geographic hierarchy.**

This prevents duplicated geographic facts and keeps domain ownership clear.

---

# 2. Business Meaning

An address represents a physical location associated with a ShopSphere customer.

In the current domain model, an address identifies:

- which customer owns the address,
- which building contains the address,
- which unit within that building is associated with the customer,
- which postal code applies to the geographic area.

The address itself does **not** duplicate the complete geographic hierarchy.

The surrounding location is resolved through:

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

The Address domain therefore references the Geography domain rather than owning geographic master data.

---

# 3. Current Address Model

The current model is:

```scala
case class Address(
  id: String,
  customerId: String,
  buildingId: String,
  unitNumber: String,
  postalCode: String
)
```

The five fields are deliberate.

## 3.1 `id`

Globally identifies the generated address record.

Example:

```text
ADDRESS_000000001
```

The ID must be unique within the generated Address dataset.

---

## 3.2 `customerId`

Identifies the ShopSphere customer associated with the address.

This creates the relationship:

```text
Customer 1 ──────── N Address
```

The current generation model guarantees that every generated customer has at least one address.

---

## 3.3 `buildingId`

References a building from the Geography reference data.

This field establishes the physical location at the building level.

The building is not duplicated inside the Address record.

---

## 3.4 `unitNumber`

Identifies the apartment/flat/unit within the selected building.

The exact representation is delegated to the building's unit-numbering convention.

Examples may include:

```text
101
204
3A
5B
1204
```

The Address generator should not assume that every building uses the same numbering scheme.

---

## 3.5 `postalCode`

Identifies the postal code associated with the selected building's Area.

The postal code must be valid for the selected geographic hierarchy.

It is therefore not an arbitrary independent random value.

---

# 4. Why Address Does Not Contain City/State/Country

The current design intentionally does not store:

```text
city
state
country
area
road
society
```

inside the Address model.

Those values are properties of the geographic hierarchy.

Duplicating them would create multiple representations of the same fact.

For example, storing:

```text
building_id = BUILDING_001
city = Mumbai
state = Maharashtra
country = India
```

would create a consistency obligation:

```text
BUILDING_001 must always belong to Mumbai,
Mumbai must always belong to Maharashtra,
Maharashtra must always belong to India.
```

The Geography domain already owns those relationships.

Therefore:

```text
Address
   └── buildingId
          ↓
      Geography
          ↓
      full hierarchy
```

is preferred.

---

# 5. Domain Ownership

## Address owns

The Address domain owns:

- address record identity,
- customer-to-address association,
- building selection for an address,
- unit assignment,
- postal-code assignment,
- address-specific validation.

## Geography owns

The Geography domain owns:

- countries,
- states,
- cities,
- areas,
- roads,
- societies,
- buildings,
- building floors,
- units-per-floor,
- postal-code mappings,
- geographic hierarchy resolution.

## Customer owns

The Customer domain owns:

- customer identity,
- customer behavior,
- customer lifecycle,
- customer-level characteristics.

The Customer domain should not contain the physical address-generation algorithm.

---

# 6. Core Relationship

The primary Address relationship is:

```text
Customer
   │
   │ 1
   │
   └──────────────< Address
                       │
                       │ N
                       ▼
                   Building
                       │
                       ▼
                   Geography
```

Cardinality:

```text
Customer → Address
1 → N
```

At the current baseline:

```text
1,000 customers
1,200 addresses
```

which gives:

```text
mean addresses/customer = 1.20
```

The current implementation produces at least one address per customer and may produce a second address.

---

# 7. Address Lifecycle

An Address is not currently modeled as an independently evolving business object.

The current generator treats it as a generated customer-related reference record.

Conceptually:

```text
Customer created
      ↓
Address allocation
      ↓
Building selected
      ↓
Unit selected
      ↓
Postal code resolved
      ↓
Address created
```

A future version may introduce temporal address lifecycle behavior:

```text
created
   ↓
active
   ↓
replaced
   ↓
inactive
```

but this is not currently part of the Address model.

---

# 8. Generation Responsibility

The Address generator should have one primary responsibility:

> **Generate a valid Address for a specified customer using the available Geography reference data and deterministic randomness.**

It should not:

- generate customers,
- load geography files,
- invent geographic hierarchy,
- validate the entire dataset,
- write CSV files,
- calculate global statistics,
- manage unrelated entity relationships.

---

# 9. Current Generation Flow

The current Address generation flow is conceptually:

```text
Address ID
    +
Customer ID
    +
Geography reference data
    +
Random generator
        │
        ▼
Select building
        │
        ▼
Select floor
        │
        ▼
Select unit on floor
        │
        ▼
Generate unit number
        │
        ▼
Resolve building hierarchy
        │
        ▼
Find postal codes for building's Area
        │
        ▼
Select postal code
        │
        ▼
Create Address
```

This is a good example of a generator whose behavior is constrained by domain relationships rather than arbitrary random values.

---

# 10. Current Technical Implementation

The current implementation is equivalent to:

```scala
object AddressGenerator {

  def generate(
    addressId: String,
    customerId: String,
    geography: GeographyReferenceData,
    random: RandomGenerator
  ): Address = {

    require(
      addressId.nonEmpty,
      "Address ID must not be empty."
    )

    require(
      customerId.nonEmpty,
      "Customer ID must not be empty."
    )

    val buildings =
      geography.buildings.values.toSeq.sortBy(_.id)

    require(
      buildings.nonEmpty,
      "Cannot generate an address because no buildings are available."
    )

    val building =
      buildings(
        random
          .derive("building")
          .nextInt(buildings.size)
      )

    val floor =
      random
        .derive("floor")
        .nextInt(1, building.floors)

    val unitOnFloor =
      random
        .derive("unit-on-floor")
        .nextInt(1, building.unitsPerFloor)

    val unitNumber =
      UnitNumberGenerator.generate(
        building,
        floor,
        unitOnFloor
      )

    val hierarchy =
      geography.resolveBuilding(building.id)

    val postalCodes =
      geography
        .postalCodesForArea(hierarchy.area.id)
        .sortBy(_.id)

    require(
      postalCodes.nonEmpty,
      s"Cannot generate an address for building '${building.id}' " +
        s"because area '${hierarchy.area.id}' has no postal codes."
    )

    val postalCode =
      postalCodes(
        random
          .derive("postal-code")
          .nextInt(postalCodes.size)
      )

    Address(
      id = addressId,
      customerId = customerId,
      buildingId = building.id,
      unitNumber = unitNumber,
      postalCode = postalCode.code
    )
  }
}
```

This implementation is intentionally simple.

Its current strength is that the business flow is visible.

---

# 11. Readability Analysis

The generator currently reads approximately like the business operation:

```text
select building
→ select floor
→ select unit
→ generate unit number
→ resolve geography
→ select postal code
→ create address
```

This is desirable.

A future refactor should preserve this readability.

If the implementation becomes:

```scala
val a = resolver.process(
  selector.execute(
    factory.build(...)
  )
)
```

without adding meaningful business variation, the refactor would be a regression.

---

# 12. Dependencies

The Address domain currently depends on:

```text
Address
  ↓
GeographyReferenceData
  ↓
UnitNumberGenerator
  ↓
RandomGenerator
```

It also conceptually depends on Customer because `customerId` is the parent relationship.

However, the Address generator does not need the complete Customer object.

That is intentional.

A generator that only needs the customer identifier should receive the identifier rather than an entire Customer object.

This minimizes coupling.

---

# 13. Dependency Injection

The current method receives dependencies explicitly:

```scala
generate(
  addressId,
  customerId,
  geography,
  random
)
```

This is effectively constructor/method dependency injection without a framework.

That is appropriate for the current project.

There is no need for:

- Spring,
- Guice,
- runtime DI containers,
- service locators.

Explicit dependencies are easier to understand and test.

---

# 14. Strategy Pattern Assessment

The current Address domain does not require a Strategy abstraction merely because strategies are useful elsewhere.

There may eventually be meaningful alternative strategies for:

- building selection,
- geographic concentration,
- unit allocation,
- address distribution.

For example:

```text
AddressLocationModel
    ├── UniformLocationModel
    ├── CustomerGeographyAffinityModel
    └── ScenarioDrivenLocationModel
```

But this abstraction should only be introduced when the project actually needs configurable alternatives.

The current implementation does not justify interface proliferation.

---

# 15. Factory Pattern Assessment

An Address factory is not currently necessary.

The model is a simple case class.

A factory would become useful if Address construction acquired substantial policy:

```text
identity
+
geographic selection
+
customer geography affinity
+
address lifecycle
+
quality scenario
```

At that point a dedicated construction boundary may improve readability.

Until then:

```scala
Address(...)
```

is preferable.

---

# 16. Builder Pattern Assessment

The Builder pattern is unnecessary for the current Address model.

There are only five fields.

Scala case-class construction already provides:

- named arguments,
- immutability,
- type safety,
- readable construction.

Therefore:

```scala
Address(
  id = ...,
  customerId = ...,
  buildingId = ...,
  unitNumber = ...,
  postalCode = ...
)
```

is preferable to a mutable builder.

---

# 17. Address Cardinality

The current project models:

```text
Customer → Address = 1:N
```

The baseline uses approximately:

```text
minimum = 1
maximum = 2
mean = 1.20
```

This is acceptable as a first functional model.

However, future realism should make address cardinality configurable.

Potential business behavior:

```text
0 addresses
1 address
2 addresses
3+ addresses
```

depending on the intended customer lifecycle and scenario.

The correct distribution should be configurable rather than hard-coded.

---

# 18. Future Address Cardinality Model

A future configuration could conceptually define:

```hocon
address-cardinality {
  distribution = "categorical"

  weights {
    one = 0.80
    two = 0.17
    three = 0.03
  }
}
```

The exact values should not be treated as final business truth.

They are examples of the type of control the generator should expose.

The important design point is:

> Address cardinality should be a business distribution, not a magic number embedded inside the generation loop.

---

# 19. Customer Geography Affinity

The current implementation selects buildings independently from the customer.

This is structurally valid but can become unrealistic.

For example, if a customer repeatedly interacts with ShopSphere, their addresses should not be completely unrelated to all other geographic behavior.

Future customer behavior may contain:

```text
preferred country
preferred state
preferred city
preferred area
```

or a geographic affinity representation.

Then:

```text
CustomerBehaviorProfile
        ↓
Geography affinity
        ↓
Address building selection
```

could produce more coherent geographic behavior.

This should be implemented only after the Geography domain is well defined.

---

# 20. Primary Address

The current Address model does not contain:

```text
isPrimary
```

and there is currently no address-type field.

These should not be added merely because other e-commerce systems commonly have them.

If future business requirements require:

```text
billing address
shipping address
primary address
work address
home address
```

the model should be deliberately extended and documented.

Until then, the five-field model remains authoritative.

---

# 21. Billing and Shipping Addresses

The current model does not separately represent billing and shipping addresses.

A future Order model may need to represent whether an order uses:

- the customer's standard address,
- a different saved address,
- a newly supplied delivery location.

This is an Order-level business decision and should not automatically expand the Address entity.

Potential future relationship:

```text
Order
 ├── shippingAddressId → Address
 └── billingAddressId  → Address
```

if the business model eventually requires it.

---

# 22. Geographic Consistency

One of the most important Address validation rules is:

> **The postal code must belong to the Area containing the selected building.**

The generation flow therefore resolves:

```text
building
   ↓
building hierarchy
   ↓
area
   ↓
postal codes
```

and selects the postal code from that Area.

This prevents impossible combinations such as:

```text
Building in Area A
Postal Code belonging to Area B
```

---

# 23. Current Address Validation

Address-specific validation should include:

## Required fields

```text
id != empty
customerId != empty
buildingId != empty
unitNumber != empty
postalCode != empty
```

## Identity

```text
address IDs are unique
```

## Customer relationship

```text
customerId exists
```

## Geography relationship

```text
buildingId exists
```

## Unit validity

The generated unit should be valid for the selected building.

## Postal validity

The postal code must belong to the Area containing the selected building.

---

# 24. Generator Failure Conditions

The generator should fail clearly when required reference data is unavailable.

Examples:

### No buildings

```text
Cannot generate an address because no buildings are available.
```

### Building's Area has no postal codes

```text
Cannot generate an address for building '...'
because area '...' has no postal codes.
```

These failures are preferable to silently producing invalid data.

---

# 25. Deterministic Randomness

Address generation uses the project's deterministic random abstraction.

The current pipeline derives an Address-specific random stream from:

```text
customer ID
+
address sequence
```

Conceptually:

```scala
random.derive(s"${customer.id}-$sequence")
```

This is important because it gives address generation an explicit deterministic context.

The same seed and same logical inputs should reproduce the same generated Address.

---

# 26. Stable Reference Ordering

The generator sorts reference collections by stable IDs before random selection:

```scala
.sortBy(_.id)
```

This is important for reproducibility.

Randomly selecting from an unordered collection can produce different results if iteration order changes.

Therefore:

> **Stable input ordering is part of reproducibility.**

This principle should also be applied consistently across other domains.

---

# 27. Testing Strategy

The Address domain should have tests at several levels.

## 27.1 Model tests

Verify:

- fields are preserved,
- equality works,
- inequality works,
- alternative unit-number formats are accepted.

---

## 27.2 Generator tests

Verify:

- valid Address generation,
- non-empty ID validation,
- non-empty customer ID validation,
- deterministic generation,
- different seeds can produce different values,
- valid unit numbers,
- valid building references,
- valid postal-code relationships.

---

## 27.3 Failure tests

Verify:

```text
no buildings → clear failure
building with no postal code → clear failure
invalid required input → clear failure
```

---

## 27.4 Relationship tests

Verify:

```text
address.customerId exists in customers
address.buildingId exists in geography
postalCode belongs to building's Area
```

---

# 28. Future Statistical Validation

As the generator becomes more realistic, Address should have domain-level statistics such as:

```text
addresses per customer
addresses per city
addresses per state
addresses per area
addresses per building
```

Potential concentration measures:

```text
top 1% buildings by address count
top 10% areas by address count
geographic concentration
```

These become especially important when geographic affinity and skew scenarios are introduced.

---

# 29. Address Skew Scenarios

Address can participate in controlled geographic skew.

Examples:

### Uniform geography

Addresses are distributed broadly across available buildings.

### City concentration

A large percentage of addresses are concentrated in selected cities.

### Area concentration

Specific areas receive disproportionately many addresses.

### Building concentration

Selected buildings become address hotspots.

This can later be useful for Spark workloads involving:

- joins,
- aggregations,
- grouping,
- partitioning,
- skew handling.

---

# 30. Data-Quality Scenarios

Address is a useful candidate for controlled data-quality problems.

Potential scenarios include:

```text
missing postal code
invalid postal code
unknown building ID
duplicate address ID
missing customer ID
malformed unit number
```

These should never appear accidentally in the clean baseline.

The Quality domain should inject such defects explicitly.

The Address generator should remain responsible for producing valid baseline records.

---

# 31. Important Separation: Generation vs Quality Injection

Do not make the Address generator randomly generate invalid records.

Instead:

```text
Address Generator
       ↓
valid Address
       ↓
Quality Engine
       ↓
optional controlled corruption
```

This keeps the clean generation model understandable.

It also allows the same valid dataset to be transformed into multiple quality scenarios.

---

# 32. Output Responsibility

The Address domain should not write CSV files.

The generator produces:

```scala
Seq[Address]
```

or an appropriate iterable representation.

The output layer decides how to serialize it.

Current output representation:

```text
addresses.csv
```

with columns:

```text
id
customer_id
building_id
unit_number
postal_code
```

This separation keeps domain generation independent from storage format.

---

# 33. Statistics Responsibility

Address generation should not calculate global statistics.

The statistics layer should consume generated addresses and calculate:

```text
count
addresses/customer
geographic distribution
building concentration
```

This preserves the separation:

```text
Generation
    ≠
Statistics
```

---

# 34. Manifest Responsibility

The manifest should describe the generated Address output, including information such as:

```text
entity = addresses
record count
output file
schema information
generation context
```

The Address domain itself should not own manifest generation.

---

# 35. Relationship Orchestration

The application-level generation pipeline currently decides when addresses are generated.

Conceptually:

```text
Generate Customer
       ↓
Determine address count
       ↓
Generate Address for Customer
```

The Address generator should not loop over all Customers.

Likewise, the Customer generator should not implement building selection.

This keeps orchestration and domain generation separate.

---

# 36. Current Simplifications

The current Address implementation intentionally simplifies several aspects.

## Simplification 1 — Limited address cardinality

Customers currently receive approximately one or two addresses.

---

## Simplification 2 — Uniform building selection

Building selection is currently broadly random from available buildings.

There is no sophisticated customer geographic affinity model yet.

---

## Simplification 3 — No temporal lifecycle

Address creation and replacement are not modeled over time.

---

## Simplification 4 — No address types

The current model does not distinguish:

- home,
- office,
- billing,
- shipping,
- primary.

---

## Simplification 5 — No historical address state

The system does not currently preserve address changes over customer history.

---

# 37. Planned Future Realism

The Address domain may eventually support:

```text
customer geography affinity
address cardinality distributions
address lifecycle
historical addresses
shipping/billing selection
geographic concentration
scenario-driven geography
```

The order of implementation should depend on the maturity of Customer, Geography, and Order domains.

---

# 38. Proposed Target Package

The intended package is:

```text
com.shopsphere.datagenerator.address/
├── model/
├── generator/
└── validation/
```

Additional subpackages should only be introduced when justified.

For example, a `behavior/` package is unnecessary until Address actually owns meaningful behavior strategies.

---

# 39. Proposed Target Responsibilities

## `address.model`

Contains:

```text
Address
```

and potentially future Address-specific value objects if they become necessary.

---

## `address.generator`

Contains:

```text
AddressGenerator
```

and future generation policies that genuinely belong to Address.

---

## `address.validation`

Contains Address-specific validation rules.

Global orchestration remains in the system-level validation layer.

---

# 40. Dependency Direction

Preferred direction:

```text
address
   ↓
geography
common infrastructure
```

Address should not depend on:

```text
output
manifest
statistics
Main
```

as implementation dependencies.

Application orchestration can depend on Address.

This keeps the dependency direction understandable.

---

# 41. Domain Service Assessment

A dedicated Address domain service is not currently necessary.

A service becomes useful if an operation crosses multiple concepts and cannot naturally belong to one object.

For example:

```text
resolve customer's preferred delivery address
```

could eventually involve:

```text
Customer
+
Address
+
Order
+
Geography
```

That may belong in a higher-level domain/application service rather than inside `Address`.

Do not create a service merely to move a few lines of code.

---

# 42. SOLID Assessment

## Single Responsibility

Good target:

```text
AddressGenerator → generates Address
Address validation → validates Address rules
Geography → owns geographic reference data
Output writer → serializes Address
```

---

## Open/Closed

Future address-selection strategies should be addable without turning `AddressGenerator` into a giant conditional block.

---

## Liskov Substitution

Relevant only if meaningful strategy interfaces are introduced.

Do not create inheritance hierarchies simply to satisfy the principle formally.

---

## Interface Segregation

Do not create:

```scala
trait AddressManager
```

containing unrelated operations.

---

## Dependency Inversion

The generator should depend on meaningful abstractions where variation exists.

The current explicit `RandomGenerator` boundary is appropriate.

---

# 43. Anti-Patterns to Avoid

Avoid:

```text
AddressHelper
AddressManager
AddressProcessor
AddressUtil
```

unless the responsibility is precisely defined.

Avoid:

```text
AddressGenerator
    ├── CSV writing
    ├── statistics
    ├── validation
    ├── geography loading
    └── quality corruption
```

This would violate separation of responsibilities.

Avoid duplicating geography:

```text
Address.city
Address.state
Address.country
```

unless the business model explicitly requires denormalized address snapshots.

---

# 44. Migration Plan

When the Address domain is migrated into the target architecture:

## Step 1

Move the Address model to:

```text
address/model/Address.scala
```

---

## Step 2

Move the generator to:

```text
address/generator/AddressGenerator.scala
```

---

## Step 3

Move or create Address-specific tests alongside the domain.

---

## Step 4

Move Address-specific validation logic where appropriate.

---

## Step 5

Update application orchestration imports.

---

## Step 6

Update output integration without moving output responsibilities into Address.

---

## Step 7

Run the complete test suite.

---

## Step 8

Run the end-to-end generator.

---

## Step 9

Compare generated statistics with the pre-migration baseline.

---

## Step 10

Update the project progress document.

The migration should preserve behavior unless the migration explicitly includes a business-model change.

---

# 45. Address Domain Quality Gate

The Address domain migration/design is considered successful when:

### Business

- Address meaning is explicit.
- Geography ownership is explicit.
- Customer relationship is explicit.

### Model

- The five-field model remains intentional.
- No accidental geographic duplication exists.

### Generation

- building selection is valid,
- unit generation is valid,
- postal-code selection is geographically valid.

### Reproducibility

- deterministic behavior is preserved.

### Validation

- invalid references are detected,
- geographic inconsistencies are detected.

### Architecture

- Address does not own CSV writing,
- Address does not own global statistics,
- Address does not own global orchestration,
- unnecessary abstractions are avoided.

### Testing

- unit tests pass,
- generator tests pass,
- relationship tests pass,
- end-to-end tests pass.

---

# 46. Design Decisions

## Decision A — Keep Address small

The Address model should represent the address relationship without becoming a copy of the Geography model.

---

## Decision B — Geography owns location hierarchy

Country/state/city/area/road/society/building relationships belong to Geography.

---

## Decision C — Postal code remains on Address

The generated address contains the postal code used for the customer's physical location.

The generator obtains it from the building's Area.

---

## Decision D — No address-type fields yet

Fields such as `isPrimary` or `addressType` are not part of the current model.

---

## Decision E — Explicit dependencies

The generator receives Geography and deterministic randomness explicitly.

---

## Decision F — Valid generation first

The Address generator produces valid baseline data.

Quality corruption is a separate concern.

---

## Decision G — Avoid premature patterns

Strategy, Factory, Builder, and domain services should only be introduced when concrete variation or complexity justifies them.

---

# 47. Address as a Reference Domain

Address is deliberately a simpler domain than Customer or Order.

It is useful as an architectural test because it demonstrates that the target architecture does not require every domain to contain the same number of layers.

For Address, the appropriate design may simply be:

```text
model
generator
validation
```

That is enough.

The architecture should be consistent in principles, not artificially identical in package count.

---

# 48. Relationship With Customer Behavior

The Address domain itself should not own customer behavior.

However, it may consume customer-derived geographic preferences in the future.

Potential flow:

```text
CustomerBehaviorProfile
        ↓
GeographyPreference
        ↓
AddressGenerator
        ↓
Address
```

This is an example of composition across domains.

The Address generator should receive only the information it actually needs.

---

# 49. Relationship With Order

The Address domain may later become part of Order fulfillment behavior.

Potential flow:

```text
Customer
   ↓
Saved Addresses
   ↓
Order
   ↓
Selected Shipping Address
   ↓
Shipment
```

The current model does not yet require this level of historical order-address modeling.

It should be introduced when Order and Shipment design reaches that requirement.

---

# 50. Relationship With Shipment

Shipment is expected to depend indirectly on Address through Order.

Conceptually:

```text
Shipment
   ↓
Order
   ↓
Shipping Address
   ↓
Building
   ↓
Geography
```

The exact snapshot/reference design should be decided during the Shipment domain design.

The Address domain should not anticipate every future relationship by adding fields prematurely.

---

# 51. Relationship With Data Quality

Address is particularly useful for demonstrating controlled data-quality injection.

A clean baseline:

```text
100% valid references
100% valid postal relationships
```

A dirty scenario might intentionally introduce:

```text
1% invalid postal codes
0.5% missing customer IDs
0.1% malformed unit numbers
```

The exact rates belong to scenario configuration.

The Address domain should remain unaware of scenario-specific corruption policies.

---

# 52. Relationship With Spark Performance Labs

Address data can eventually support Spark exercises involving:

- Customer ↔ Address joins,
- geographic aggregation,
- postal-code grouping,
- skewed building distributions,
- geographic partitioning,
- join strategy comparison,
- data-quality filtering.

This is one reason the domain must remain internally coherent.

A synthetic dataset should create realistic relationships that make downstream Spark workloads meaningful.

---

# 53. What Address Should Not Solve

The Address domain should not solve:

```text
customer behavior
product selection
order creation
shipment scheduling
CSV serialization
global validation
global statistics
scenario orchestration
reference-data loading
```

Those belong elsewhere.

---

# 54. Final Target Mental Model

The simplest useful mental model is:

```text
Customer
   │
   │ owns
   ▼
Address
   │
   │ references
   ▼
Building
   │
   │ belongs to
   ▼
Geography hierarchy
```

And generation is:

```text
Customer
   ↓
Address count
   ↓
Building
   ↓
Floor
   ↓
Unit
   ↓
Area
   ↓
Postal Code
   ↓
Address
```

This is the business flow that the implementation should make obvious.

---

# 55. Summary

The Address domain is intentionally small and relational.

Its core responsibility is to connect a Customer to a physical building/unit and valid postal code without duplicating the Geography hierarchy.

The current five-field model is:

```text
id
customerId
buildingId
unitNumber
postalCode
```

The current generator already demonstrates several important architectural principles:

- explicit dependencies,
- deterministic randomness,
- stable reference ordering,
- valid reference selection,
- separation from output,
- separation from global validation,
- separation from statistics,
- readable business flow.

The main future improvements are:

```text
address cardinality realism
        ↓
customer geography affinity
        ↓
temporal address lifecycle
        ↓
shipping/billing selection
        ↓
geographic skew scenarios
        ↓
statistical validation
```

The immediate architectural goal is not to make Address complex.

It is to make Address **correctly scoped, readable, testable, and properly connected to Customer and Geography**.

That becomes the standard against which later domain designs can be evaluated.
