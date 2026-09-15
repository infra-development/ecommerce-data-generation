# 32 — Complete Current Source Architecture Inventory

## 1. Purpose

This document is the **baseline inventory of the actual implementation** before architectural migration.

It was created from the current `ecommerce-data-generation` project source archive. The inventory covers:

- production Scala sources
- test Scala sources
- configuration
- reference data
- output/data directories
- current package organization
- architectural responsibilities
- dependency direction
- A/B/C/D/E classification
- target package mapping
- migration priorities
- identified architectural risks
- missing capabilities required by the target architecture

This is an **inventory, not a redesign**.

No source-code changes are implied by this document.

---

# 2. Current Repository Shape

The current repository contains these major areas:

```text
ecommerce-data-generation/
├── build.sbt
├── README.md
├── conf/
│   └── application.conf
├── data/
│   ├── generated/
│   ├── raw/
│   └── reference/
├── docs/
│   ├── existing decision documents
│   └── architecture/
└── src/
    ├── main/
    │   └── scala/
    └── test/
        └── scala/
```

The current source tree has:

- **79 production Scala files**
- **34 test Scala files**
- configuration under `conf/`
- catalog, customer, and geography reference data under `data/reference/`
- generated/raw dataset material under `data/`

The current tree confirms that domain models are centralized in `model/` and most entity generators are centralized in `generation/`. fileciteturn100file0L159-L230

---

# 3. Classification Legend

Every component is assigned a primary architectural classification.

## A — Correctly placed / conceptually aligned

The component already has a reasonable ownership boundary and requires little or no architectural change.

A package move may still be desirable.

## B — Correct responsibility, wrong domain/package location

The implementation is useful, but the target architecture gives the responsibility to another package.

Example:

```text
generation/ProductGenerator.scala
        ↓
product/generator/ProductGenerator.scala
```

## C — Mixed responsibility

The component performs too many responsibilities or crosses architectural boundaries.

It should be decomposed or refactored.

## D — Obsolete

The component belongs to an earlier design and should eventually disappear.

Removal must happen only after references and tests are handled.

## E — Missing

The target architecture requires a capability that does not currently exist.

Missing components should be introduced only after their design responsibility is clear.

---

# 4. Current Package Architecture

Current:

```text
com.shopsphere.datagenerator
│
├── config/
├── distribution/
├── generation/
├── model/
├── output/
├── reference/
│   ├── catalog/
│   └── customer/
├── relationship/
├── statistics/
└── validation/
```

Target:

```text
com.shopsphere.datagenerator
│
├── customer/
│   ├── config/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   ├── validation/
│   └── statistics/
│
├── address/
│   ├── model/
│   ├── generator/
│   └── validation/
│
├── product/
├── category/
├── brand/
├── order/
├── orderitem/
├── payment/
├── shipment/
├── return/
├── session/
├── event/
│
├── geography/
│   ├── model/
│   ├── reference/
│   └── loader/
│
├── common/
│   ├── distribution/
│   ├── random/
│   ├── time/
│   └── util/
│
├── config/
├── generation/
├── relationship/
├── quality/
├── scenario/
├── validation/
├── output/
├── manifest/
├── statistics/
└── observability/
```

The target structure is intentionally **domain-oriented**, while cross-cutting infrastructure remains centralized.

---

# 5. Production Source Inventory

## 5.1 Application Entry Point

### `Main.scala`

**Current location**

```text
src/main/scala/com/shopsphere/datagenerator/Main.scala
```

**Current responsibility**

Application entry point and end-to-end orchestration.

It currently:

1. loads configuration
2. validates configuration
3. creates generation context
4. builds generation plan
5. invokes generation
6. writes CSV
7. validates generated CSV
8. collects statistics
9. prints results

**Classification:** C

**Reason**

The responsibility is valid, but application orchestration is currently embedded in a large entry-point implementation.

**Target**

```text
generation/
```

with a thinner:

```text
Main.scala
```

and explicit application orchestration beneath it.

**Priority:** Medium, after foundational migration.

---

# 6. Configuration Inventory

Current configuration package:

```text
config/
```

The configuration package contains:

- cardinality configuration
- generation profiles
- scenarios
- distribution configuration
- product configuration
- customer configuration
- configuration loading
- configuration validation

The package is broadly correct as a system-level configuration boundary.

However, some configuration belongs more naturally to domains once the domain architecture stabilizes.

---

## `AddressGenerationConfig.scala`

**Classification:** D

This is an obsolete address configuration model.

The current Address design is:

```text
id
customer_id
building_id
unit_number
postal_code
```

Address location is resolved through the geography hierarchy.

This file should eventually be deleted after all references are confirmed absent.

**Priority:** High cleanup, but not the first migration.

---

## `CardinalityConfig.scala`

**Classification:** A

**Responsibility:** Cardinality targets used by generation planning.

**Target:** `config/`

May later be split conceptually between global generation planning and domain-specific cardinality policies, but no immediate move is required.

---

## `CardinalityDefaults.scala`

**Classification:** A

**Responsibility:** Default cardinality values by profile.

**Target:** `config/`

---

## `CardinalityProfile.scala`

**Classification:** A

**Responsibility:** Cardinality profile selection and parsing.

**Target:** `config/`

---

## `ConfigLoader.scala`

**Classification:** C

**Responsibility:** Builds the complete `GenerationConfig` from HOCON configuration.

It currently coordinates several specialized loaders and parses profile/scenario/cardinality selections.

**Target:** `config/`

**Architectural concern**

It is acceptable as the system-level configuration composition root, but it should not gradually become a domain-behavior factory.

---

## `ConfigValidator.scala`

**Classification:** A/C

**Responsibility:** Top-level configuration validation.

**Target:** `config/`

Domain-specific validation should remain close to the relevant domain where appropriate.

---

## `CustomerGenerationConfig.scala`

**Classification:** B

**Responsibility:** Customer-specific generation configuration.

**Current:** `config/`

**Target:**

```text
customer/config/
```

This is a good candidate for domain ownership.

---

## `DistributionConfig.scala`

**Classification:** B

**Responsibility:** Configuration representation for distribution definitions.

**Current:** `config/`

**Target:**

```text
common/distribution/
```

or retained as system configuration if the final design keeps configuration representations centralized.

This decision should be made during common-foundation migration.

---

## `DistributionConfigLoader.scala`

**Classification:** B

**Responsibility:** Reads distribution configuration.

**Target:** likely:

```text
common/distribution/
```

The important distinction is between:

- configuration representation
- distribution runtime implementation

---

## `GenerationConfig.scala`

**Classification:** A

**Responsibility:** System-level configuration aggregate.

**Target:** `config/`

It currently contains:

- generator settings
- output settings
- profile definition
- cardinality
- distributions
- product configuration
- customer configuration

It should remain the top-level configuration object unless the architecture later demonstrates a clear need for domain configuration composition.

---

## `GenerationProfile.scala`

**Classification:** A

**Responsibility:** Generation-scale profile selection.

**Target:** `config/`

---

## `GenerationProfileDefinition.scala`

**Classification:** A

**Responsibility:** Concrete definitions associated with generation profiles.

**Target:** `config/`

---

## `GenerationScenario.scala`

**Classification:** A

**Responsibility:** Scenario selection.

**Target:** `config/` with scenario implementation remaining under `scenario/`.

---

## `ProductBrandAffinityConfig.scala`

**Classification:** B

**Responsibility:** Product/brand relationship configuration.

**Target:** likely `product/config/`.

The configuration expresses product-domain behavior and should eventually be owned by the product domain.

---

## `ProductBrandAffinityConfigLoader.scala`

**Classification:** B

**Target:**

```text
product/config/
```

---

## `ProductDistributionConfig.scala`

**Classification:** B

**Target:**

```text
product/config/
```

---

## `ProductDistributionConfigLoader.scala`

**Classification:** B

**Target:**

```text
product/config/
```

---

## `ProductPricingConfig.scala`

**Classification:** B

**Target:**

```text
product/config/
```

---

## `ProductPricingConfigLoader.scala`

**Classification:** B

**Target:**

```text
product/config/
```

---

# 7. Distribution and Randomness Inventory

Current:

```text
distribution/
├── Distribution.scala
├── DistributionEngine.scala
├── RandomGenerator.scala
├── TriangularDistribution.scala
├── UniformDistribution.scala
└── WeightedDistribution.scala
```

This is one of the strongest existing architectural boundaries.

---

## `Distribution.scala`

**Classification:** A

Runtime distribution abstraction.

**Target:**

```text
common/distribution/
```

---

## `DistributionEngine.scala`

**Classification:** A

Central distribution construction/lookup mechanism.

**Target:**

```text
common/distribution/
```

---

## `RandomGenerator.scala`

**Classification:** A

Central seeded random generator with derived random streams.

**Target:**

```text
common/random/
```

This is foundational infrastructure.

---

## `TriangularDistribution.scala`

**Classification:** A

**Target:** `common/distribution/`

---

## `UniformDistribution.scala`

**Classification:** A

**Target:** `common/distribution/`

---

## `WeightedDistribution.scala`

**Classification:** A

**Target:** `common/distribution/`

---

# 8. Generation Package Inventory

Current:

```text
generation/
├── AddressGenerator.scala
├── CardinalityAllocator.scala
├── CustomerGenerator.scala
├── EventGenerator.scala
├── GeneratedData.scala
├── GenerationContext.scala
├── GenerationContextFactory.scala
├── GenerationPipeline.scala
├── GenerationPlan.scala
├── GenerationPlanBuilder.scala
├── OrderGenerator.scala
├── OrderItemGenerator.scala
├── PaymentGenerator.scala
├── ProductGenerator.scala
├── ReturnGenerator.scala
├── SessionGenerator.scala
├── ShipmentGenerator.scala
└── UnitNumberGenerator.scala
```

This is the largest architectural migration area.

The current package is effectively a **generic entity-generation container**.

The target architecture moves entity-specific generation into domain packages.

---

## `AddressGenerator.scala`

**Classification:** B

**Target:**

```text
address/generator/
```

Depends on:

- Address model
- geography reference data
- random infrastructure

The existing implementation is already reasonably focused.

---

## `CardinalityAllocator.scala`

**Classification:** A/B

**Responsibility:** Allocates cardinalities according to generation requirements.

**Likely target:** `generation/` if it remains system-level planning infrastructure.

Do not move it into a domain simply because it currently lives beside generators.

---

## `CustomerGenerator.scala`

**Classification:** C

**Current responsibility:** Generates Customer records using customer configuration, distributions, reference names, dates, and random generation.

**Target:**

```text
customer/generator/
```

But it will likely need decomposition.

The future design should separate concerns such as:

```text
CustomerBehaviorModel
IdentityGenerator
AcquisitionGenerator
PreferenceGenerator
CustomerFactory
```

The current class/object is therefore a major migration and realism candidate.

---

## `EventGenerator.scala`

**Classification:** B/C

**Target:**

```text
event/generator/
```

It is currently an independent generator. Later it must become behaviorally connected to session/customer funnel state.

---

## `GeneratedData.scala`

**Classification:** A/C

**Responsibility:** Aggregate of all generated entity collections.

This is currently a system-level generated-dataset container.

**Target:** likely remain under `generation/`.

It should not be moved into any individual domain.

---

## `GenerationContext.scala`

**Classification:** A/C

**Responsibility:** Runtime generation dependencies/context.

**Target:** `generation/` or a carefully defined application/common context boundary.

Potential future concern: avoid turning this into a giant service locator.

Constructor-based dependency injection should remain preferred.

---

## `GenerationContextFactory.scala`

**Classification:** C

**Responsibility:** Constructs generation runtime dependencies.

**Target:** `generation/` as composition-root infrastructure.

The factory should remain responsible for composition, not domain behavior.

---

## `GenerationPipeline.scala`

**Classification:** C — High Priority

This is one of the most important architectural hotspots.

It currently coordinates generation across many entities and also contains transaction/session-related result types.

It imports the model layer broadly and calls multiple domain generators and relationship components.

**Target:** application-level orchestration under `generation/`.

It should eventually coordinate:

```text
plan
→ reference data
→ domain generation
→ relationship/domain services
→ validation
→ statistics
→ output
```

without becoming a giant business-logic class.

---

## `GenerationPlan.scala`

**Classification:** A

System-level generation plan.

**Target:** `generation/`

---

## `GenerationPlanBuilder.scala`

**Classification:** A/C

Builds the generation plan from configuration.

**Target:** `generation/`

Later it may need richer domain planning once cardinalities become behavior-driven.

---

## `OrderGenerator.scala`

**Classification:** B/C

**Target:**

```text
order/generator/
```

Currently too independent from customer behavioral context.

Future responsibility should use customer behavior, lifecycle, time, and product/order relationships.

---

## `OrderItemGenerator.scala`

**Classification:** B/C

**Target:**

```text
orderitem/generator/
```

Future responsibility should use product selection/popularity, customer preferences, and order context.

---

## `PaymentGenerator.scala`

**Classification:** B/C

**Target:**

```text
payment/generator/
```

Future behavior should incorporate payment preference and order state.

---

## `ProductGenerator.scala`

**Classification:** C — High Priority

**Target:**

```text
product/generator/
```

This generator is relatively large and contains substantial catalog/pricing/brand behavior.

It should be decomposed carefully rather than simply moved.

Potential responsibilities:

```text
Product identity
Catalog relationship
Brand selection
Pricing
Distribution
```

---

## `ReturnGenerator.scala`

**Classification:** B/C

**Target:**

```text
return/generator/
```

Future return probability should incorporate customer and product/order characteristics.

---

## `SessionGenerator.scala`

**Classification:** B/C

**Target:**

```text
session/generator/
```

Current generation is too deterministic for the final realism target.

---

## `ShipmentGenerator.scala`

**Classification:** B/C

**Target:**

```text
shipment/generator/
```

Should remain related to order lifecycle but should not be buried inside a generic relationship generator.

---

## `UnitNumberGenerator.scala`

**Classification:** B

**Target:**

```text
address/generator/
```

This is clearly address-specific generation logic.

---

# 9. Model Package Inventory

Current:

```text
model/
├── Address.scala
├── Brand.scala
├── Category.scala
├── Customer.scala
├── Event.scala
├── Order.scala
├── OrderItem.scala
├── Payment.scala
├── Product.scala
├── ProductModel.scala
├── ProductType.scala
├── Return.scala
├── Session.scala
└── Shipment.scala
```

This package is structurally simple but violates the desired domain ownership model.

Almost all entity models should move to their domain packages.

---

## Domain model mapping

| Current model | Classification | Target |
|---|---|---|
| `Address.scala` | B | `address/model/Address.scala` |
| `Brand.scala` | B | `brand/model/Brand.scala` |
| `Category.scala` | B | `category/model/Category.scala` |
| `Customer.scala` | B | `customer/model/Customer.scala` |
| `Event.scala` | B | `event/model/Event.scala` |
| `Order.scala` | B | `order/model/Order.scala` |
| `OrderItem.scala` | B | `orderitem/model/OrderItem.scala` |
| `Payment.scala` | B | `payment/model/Payment.scala` |
| `Product.scala` | B | `product/model/Product.scala` |
| `ProductModel.scala` | B | `product/model/ProductModel.scala` or catalog reference ownership |
| `ProductType.scala` | B | `product/model/ProductType.scala` |
| `Return.scala` | B | `return/model/Return.scala` |
| `Session.scala` | B | `session/model/Session.scala` |
| `Shipment.scala` | B | `shipment/model/Shipment.scala` |

The models themselves are mostly simple case classes. The migration is primarily ownership/package-oriented, not a reason to introduce artificial domain hierarchies.

---

# 10. Address Domain Inventory

Address-specific implementation currently consists of:

```text
generation/AddressGenerator.scala
generation/UnitNumberGenerator.scala
model/Address.scala
```

Target:

```text
address/
├── model/
│   └── Address.scala
├── generator/
│   ├── AddressGenerator.scala
│   └── UnitNumberGenerator.scala
└── validation/
```

**Important:** preserve the existing Address model.

```text
Address(
  id,
  customerId,
  buildingId,
  unitNumber,
  postalCode
)
```

Do not reintroduce:

- address line 1
- address line 2
- city
- state
- country
- address type
- primary address flag

unless the design is explicitly changed later.

Geography resolves the location hierarchy.

---

# 11. Geography / Reference Inventory

Current geography components:

```text
reference/
├── AddressHierarchy.scala
├── GeographyLoader.scala
├── GeographyModels.scala
├── GeographyReferenceData.scala
└── GeographyValidator.scala
```

---

## `AddressHierarchy.scala`

**Classification:** B

**Target:**

```text
geography/model/
```

or a geography domain relationship/value object package.

---

## `GeographyLoader.scala`

**Classification:** B

**Target:**

```text
geography/loader/
```

---

## `GeographyModels.scala`

**Classification:** B

Contains:

```text
Country
State
City
Area
Road
Society
Building
PostalCode
```

**Target:**

```text
geography/model/
```

---

## `GeographyReferenceData.scala`

**Classification:** B

**Target:**

```text
geography/reference/
```

This is an important reference-data aggregate and currently provides hierarchy resolution operations.

---

## `GeographyValidator.scala`

**Classification:** B

**Target:** `geography/validation/` if needed, otherwise `geography/`.

---

# 12. Catalog Reference Inventory

Current:

```text
reference/catalog/
├── CatalogReferenceData.scala
└── CatalogReferenceLoader.scala
```

---

## `CatalogReferenceData.scala`

**Classification:** B

Contains catalog reference representations for:

- categories
- brands
- product types
- product models

**Target:** `product/reference/` or a broader catalog/reference boundary.

The target architecture explicitly calls for reference-data ownership, so this should not be forced into generated domain models.

---

## `CatalogReferenceLoader.scala`

**Classification:** B

**Target:** reference/catalog or product/reference loader depending on final ownership.

The loader is relatively large because it also performs reference validation.

This should be reviewed during reference-data migration.

---

# 13. Customer Reference Inventory

Current:

```text
reference/customer/
├── CustomerName.scala
├── CustomerReferenceData.scala
└── CustomerReferenceLoader.scala
```

---

## `CustomerName.scala`

**Classification:** B

**Target:** `customer/reference/` or `customer/model/` depending on final semantic role.

---

## `CustomerReferenceData.scala`

**Classification:** B

**Target:** `customer/reference/`

---

## `CustomerReferenceLoader.scala`

**Classification:** B

**Target:** `customer/reference/` or `customer/loader/`.

---

# 14. Relationship Package Inventory

Current:

```text
relationship/
├── OrderGenerationResult.scala
├── OrderLifecycle.scala
├── OrderLifecycleGenerator.scala
└── OrderRelationshipGenerator.scala
```

This package contains useful cross-entity behavior, but some responsibilities should move closer to Order.

---

## `OrderGenerationResult.scala`

**Classification:** B/C

It contains the output of order-related generation.

Likely target:

```text
order/model/
```

or `order/service/`.

Final placement should depend on whether it is considered an order aggregate result or an application-level result.

---

## `OrderLifecycle.scala`

**Classification:** B

**Target:**

```text
order/model/
```

or:

```text
order/lifecycle/
```

This represents Order lifecycle semantics and belongs with the Order domain.

---

## `OrderLifecycleGenerator.scala`

**Classification:** B

**Target:**

```text
order/lifecycle/
```

This is domain behavior, not generic relationship infrastructure.

---

## `OrderRelationshipGenerator.scala`

**Classification:** C — High Priority

This component crosses multiple entity boundaries.

It currently depends on generation classes, random/distribution infrastructure, and Product.

Some responsibilities may remain as a domain service because order creation genuinely spans:

```text
Customer
Product
Order
OrderItem
Payment
Shipment
Return
```

However, the implementation should be decomposed so that the service orchestrates domain operations rather than directly owning every generator.

Potential future role:

```text
order/service/OrderCreationService
```

plus smaller domain generators.

---

# 15. Statistics Inventory

Current:

```text
statistics/
├── DatasetStatistics.scala
└── StatisticsCollector.scala
```

---

## `DatasetStatistics.scala`

**Classification:** A/C

System-level dataset statistics model.

**Target:** `statistics/`.

Domain-specific statistics may later be introduced under domains.

---

## `StatisticsCollector.scala`

**Classification:** C — High Priority

The collector currently operates across the generated dataset and contains substantial entity-specific statistical logic.

**Target:** keep a global collector/orchestrator under:

```text
statistics/
```

but consider extracting domain-specific metric calculation into:

```text
customer/statistics/
product/statistics/
order/statistics/
session/statistics/
...
```

The global collector should aggregate, not become another giant domain-aware class.

---

# 16. Validation Inventory

Current:

```text
validation/
├── CsvDataValidator.scala
├── CsvFileReader.scala
└── CsvValidationResult.scala
```

---

## `CsvDataValidator.scala`

**Classification:** C — High Priority

This is one of the largest current components.

It performs CSV-level validation across the generated dataset, including structural, key, relationship, and business checks.

The responsibility is valuable, but the implementation should eventually separate:

```text
CSV reading
global validation orchestration
domain validation
relationship validation
business rules
```

Target:

```text
validation/
```

for global orchestration, plus domain validation where justified.

---

## `CsvFileReader.scala`

**Classification:** A

This is infrastructure for reading generated CSV files.

**Target:** `validation/` or `output/reader/`.

No urgent redesign required.

---

## `CsvValidationResult.scala`

**Classification:** A

Global validation result types.

**Target:** `validation/`.

---

# 17. Output Inventory

Current:

```text
output/
├── CsvOutputWriter.scala
└── CsvWriter.scala
```

---

## `CsvWriter.scala`

**Classification:** A

Low-level CSV writing utility.

**Target:** `output/`.

---

## `CsvOutputWriter.scala`

**Classification:** A/C

High-level writer for the generated dataset.

It knows about the complete `GeneratedData` aggregate and writes entity-specific CSVs.

This is appropriate as system-level output orchestration, but it should not contain business rules.

**Target:** `output/`.

Potential future extension:

```text
output/
├── csv/
├── manifest/
└── ...
```

---

# 18. Manifest Package

Current:

```text
manifest/
```

but there are currently no production source files in it.

**Classification:** E

The target architecture requires manifest generation.

Expected future responsibility:

```text
manifest/
├── Manifest.scala
├── ManifestBuilder.scala
└── ...
```

It should capture information such as:

- generation seed
- profile
- scenario
- cardinality profile
- record counts
- output locations
- generation metadata
- validation status
- important statistics

This should be implemented later, after output and orchestration are stabilized.

---

# 19. Observability Package

Current:

```text
observability/
```

but no production source files currently exist.

**Classification:** E

Future responsibility:

- generation timing
- per-domain generation timing
- record throughput
- validation timing
- output timing
- memory/scale observations where appropriate
- scenario execution metrics

Keep this separate from business generation logic.

---

# 20. Quality Package

Current:

```text
quality/
```

but no production source files currently exist.

**Classification:** E

The target architecture requires controlled data-quality problems.

Future capabilities may include:

```text
missing values
duplicate records
invalid references
format violations
inconsistent relationships
outlier injection
```

These should be deterministic and scenario-controlled.

---

# 21. Scenario Package

Current:

```text
scenario/
```

but no production source files currently exist.

**Classification:** E

Scenario selection currently exists in configuration, but the runtime scenario implementation is not yet present as a dedicated subsystem.

Future responsibility:

```text
clean
slightly_dirty
dirty
skewed
high_cardinality
stress
```

or equivalent scenario combinations.

Scenario should compose existing generation policies rather than duplicate generators.

---

# 22. Model Ownership Summary

The current architecture centralizes all generated business entities:

```text
model/
```

This should change to:

```text
customer/model/
address/model/
category/model/
brand/model/
product/model/
order/model/
orderitem/model/
payment/model/
shipment/model/
return/model/
session/model/
event/model/
```

This is primarily a **package ownership migration**, not an excuse to redesign every case class.

---

# 23. Cross-Cutting Ownership Summary

| Current | Target | Classification |
|---|---|---|
| `distribution/` | `common/distribution/` | A |
| `RandomGenerator` | `common/random/` | A |
| `generation/GenerationPlan*` | `generation/` | A |
| `generation/GenerationContext*` | `generation/` | A/C |
| `generation/GenerationPipeline` | `generation/` | C |
| `output/` | `output/` | A |
| `validation/` | `validation/` | A/C |
| `statistics/` | `statistics/` | A/C |
| `relationship/` | split between domains + services | C |
| `quality/` | `quality/` | E |
| `scenario/` | `scenario/` | E |
| `manifest/` | `manifest/` | E |
| `observability/` | `observability/` | E |

---

# 24. Test Inventory

The project currently contains **34 test Scala files**.

Current test packages mirror the current source structure:

```text
test/
└── com/shopsphere/datagenerator/
    ├── config/
    ├── distribution/
    ├── generation/
    ├── model/
    ├── output/
    ├── reference/
    ├── relationship/
    ├── statistics/
    └── validation/
```

The current test tree includes dedicated tests for configuration loaders, distributions, generators, models, output, reference data, relationships, statistics, and CSV validation. fileciteturn100file0L234-L291

---

# 25. Test Migration Mapping

## Configuration

Current:

```text
config/
├── DistributionConfigLoaderTest
├── ProductBrandAffinityConfigLoaderTest
├── ProductDistributionConfigLoaderTest
└── ProductPricingConfigLoaderTest
```

These should follow the configuration ownership migration.

Potential target:

```text
product/config/
```

for product-specific tests.

---

## Distribution

Current:

```text
distribution/
├── DistributionEngineTest
├── TriangularDistributionTest
├── UniformDistributionTest
└── WeightedDistributionTest
```

Target:

```text
common/distribution/
```

---

## Generation

Current generator tests should move with the corresponding domain generator.

Examples:

```text
AddressGeneratorTest
→ address/generator/

CustomerGeneratorTest
→ customer/generator/

ProductGeneratorTest
→ product/generator/

OrderGeneratorTest
→ order/generator/

OrderItemGeneratorTest
→ orderitem/generator/

PaymentGeneratorTest
→ payment/generator/

ShipmentGeneratorTest
→ shipment/generator/

ReturnGeneratorTest
→ return/generator/

SessionGeneratorTest
→ session/generator/

EventGeneratorTest
→ event/generator/
```

`CardinalityAllocatorTest`, `GenerationPlanBuilderTest`, and context/pipeline tests should remain system-level where appropriate.

---

## Model Tests

Current:

```text
model/
├── AddressTest
├── CustomerTest
└── ProductTest
```

These should move with their domain models.

The lack of model tests for several entities should be reviewed later, but we should not automatically add tests merely to increase test count.

---

## Output Tests

Remain under:

```text
output/
```

---

## Reference Tests

Move with the corresponding reference-data ownership.

---

## Relationship Tests

These should be reviewed carefully during the Order/domain-service migration.

---

## Statistics Tests

Remain system-level unless metrics are extracted into domain-specific statistics.

---

## Validation Tests

Remain system-level for CSV infrastructure and global validation.

---

# 26. Configuration and Runtime Resource Inventory

Current configuration/resource areas include:

```text
conf/application.conf
```

and reference datasets:

```text
data/reference/
├── catalog/
│   ├── brands.json
│   ├── categories.json
│   ├── product_models.json
│   ├── product_pricing.json
│   └── product_types.json
│
├── customer/
│   ├── first_names.json
│   └── last_names.json
│
└── geography/
    ├── areas.json
    ├── buildings.json
    ├── cities.json
    ├── countries.json
    ├── postal_codes.json
    ├── roads.json
    ├── societies.json
    └── states.json
```

The repository tree confirms these reference-data groups and the current raw/generated data areas. fileciteturn100file0L62-L101

---

# 27. Reference Data Ownership

Target ownership:

```text
geography/
└── reference/

customer/
└── reference/

product/
└── reference/
```

The physical `data/reference` directory can remain centralized.

**Important distinction:**

Physical file storage does not need to mirror Scala package ownership.

The code should own the semantics; the reference data directory can remain organized by reference-data category.

---

# 28. Current Dependency Direction

The current implementation broadly follows this pattern:

```text
Main
  ↓
Config
  ↓
GenerationContext / GenerationPlan
  ↓
GenerationPipeline
  ├── CustomerGenerator
  ├── AddressGenerator
  ├── ProductGenerator
  ├── Order/Relationship logic
  ├── SessionGenerator
  └── EventGenerator
  ↓
GeneratedData
  ├── Output
  ├── Validation
  └── Statistics
```

Reference data feeds generators:

```text
Reference Data
      ↓
Domain Generators
```

Random/distribution infrastructure feeds generators:

```text
Common Randomness
      ↓
Distribution / Sampling
      ↓
Domain Generators
```

This is a workable foundation.

The main problem is not that the dependency graph is fundamentally broken.

The main problem is **ownership concentration**.

---

# 29. Major Architectural Hotspots

The following components deserve special attention during migration.

## Hotspot 1 — `GenerationPipeline.scala`

Problem:

- too many domains coordinated directly
- transaction/activity results defined locally
- broad model dependency

Action:

- retain system-level orchestration
- extract domain responsibilities
- make pipeline progressively thinner

---

## Hotspot 2 — `CustomerGenerator.scala`

Problem:

- customer generation is already large
- final architecture requires latent behavioral modeling

Action:

- move first
- then decompose behavior from record construction
- introduce behavior strategies only where justified

---

## Hotspot 3 — `ProductGenerator.scala`

Problem:

- relatively large
- combines product identity, catalog relationship, pricing, distribution, and affinity concerns

Action:

- move to Product domain
- separate responsibilities carefully

---

## Hotspot 4 — `OrderRelationshipGenerator.scala`

Problem:

- cross-domain responsibility
- currently depends on generation package

Action:

- evolve into explicit domain service/orchestration where justified

---

## Hotspot 5 — `CsvDataValidator.scala`

Problem:

- very large cross-entity validator

Action:

- retain global validation orchestration
- extract domain-specific validation rules where justified

---

## Hotspot 6 — `StatisticsCollector.scala`

Problem:

- large global collector

Action:

- retain aggregate statistics orchestration
- extract domain-specific metric calculations if complexity warrants it

---

# 30. Missing Capabilities

The inventory confirms several target-architecture capabilities do not currently have implementation packages.

## Customer behavior

Missing:

```text
CustomerBehaviorProfile
CustomerActivityModel
CustomerSpendingModel
CustomerPurchaseFrequencyModel
CustomerPreferenceModel
CustomerReturnPropensity
CustomerLifecycleModel
```

These should not all be implemented independently.

The behavioral anchor should be designed first.

---

## Product popularity

Missing a true long-tail product-demand model.

Future examples:

```text
Zipf
Pareto
log-normal
negative-binomial
```

The exact distribution should be selected based on the business behavior being modeled, not simply because it is mathematically interesting.

---

## Temporal model

Missing a coherent system-wide temporal behavior model.

Future concepts:

```text
seasonality
campaign periods
customer lifecycle
order recency
session timing
event timing
```

---

## Funnel model

Missing a realistic session/event funnel.

Future example:

```text
landing
  ↓
product view
  ↓
product interaction
  ↓
add to cart
  ↓
checkout
  ↓
payment
  ↓
order
```

Not every session should follow the same path.

---

## Data-quality injection

Missing dedicated quality execution.

---

## Skew scenarios

Missing dedicated runtime skew implementation.

---

## Manifest

Missing implementation.

---

## Observability

Missing dedicated implementation.

---

# 31. Architecture Classification Summary

## A — Mostly aligned

```text
CardinalityConfig
CardinalityDefaults
CardinalityProfile
GenerationConfig
GenerationProfile
GenerationProfileDefinition
GenerationScenario

Distribution
DistributionEngine
RandomGenerator
TriangularDistribution
UniformDistribution
WeightedDistribution

GeneratedData
GenerationPlan
GenerationPlanBuilder

CsvWriter
CsvFileReader
CsvValidationResult
DatasetStatistics
```

Some A components may still move package location without changing responsibility.

---

## B — Correct concept, wrong package

Major examples:

```text
AddressGenerator
UnitNumberGenerator

CustomerGenerationConfig

ProductBrandAffinityConfig
ProductBrandAffinityConfigLoader
ProductDistributionConfig
ProductDistributionConfigLoader
ProductPricingConfig
ProductPricingConfigLoader

All entity models currently under model/

Geography models/loaders/reference data
Customer reference data/loaders
Catalog reference data/loaders

OrderLifecycle
OrderLifecycleGenerator
```

These are mostly ownership/package migrations.

---

## C — Mixed responsibility / architectural hotspot

```text
Main
ConfigLoader
ConfigValidator

CustomerGenerator
ProductGenerator
GenerationContext
GenerationContextFactory
GenerationPipeline

OrderGenerator
OrderItemGenerator
PaymentGenerator
ReturnGenerator
SessionGenerator
ShipmentGenerator
EventGenerator

OrderGenerationResult
OrderRelationshipGenerator

StatisticsCollector
CsvDataValidator
```

These require more than mechanical package moves.

---

## D — Obsolete

Confirmed current candidate:

```text
AddressGenerationConfig.scala
```

Potential additional obsolete pieces should be identified during migration rather than guessed now.

---

## E — Missing

```text
manifest implementation
observability implementation
quality implementation
scenario implementation

CustomerBehaviorProfile
customer behavior models
lifecycle model
product popularity model
temporal behavior model
session funnel model
advanced return probability model
statistical realism/calibration layer
```

---

# 32. Recommended Migration Order Based on the Actual Source

The source inventory changes the implementation order slightly from a simple alphabetical migration.

## Stage 1 — Common foundation

First migrate:

```text
RandomGenerator
Distribution
DistributionEngine
TriangularDistribution
UniformDistribution
WeightedDistribution
```

Reason:

These are low-level dependencies used by many domains.

---

## Stage 2 — Reference domains

Then:

```text
geography
customer/reference
product/reference
```

Reason:

Generators depend on reference data.

---

## Stage 3 — Models

Move entity models to their domain packages.

This should happen before moving generators so the domain ownership boundary becomes explicit.

---

## Stage 4 — Simple generators

Move focused generators first:

```text
AddressGenerator
UnitNumberGenerator
OrderGenerator
PaymentGenerator
ShipmentGenerator
ReturnGenerator
SessionGenerator
EventGenerator
```

Do not immediately redesign their business behavior.

---

## Stage 5 — Complex generators

Then tackle:

```text
CustomerGenerator
ProductGenerator
OrderRelationshipGenerator
GenerationPipeline
```

These require decomposition.

---

## Stage 6 — Cross-cutting cleanup

Then:

```text
StatisticsCollector
CsvDataValidator
ConfigLoader
```

and remaining relationship infrastructure.

---

## Stage 7 — Remove obsolete pieces

Remove:

```text
AddressGenerationConfig.scala
```

only after confirming there are no references.

---

## Stage 8 — Implement missing infrastructure

Then:

```text
manifest/
observability/
quality/
scenario/
```

---

## Stage 9 — Realism

Only after architectural stabilization:

```text
CustomerBehaviorProfile
      ↓
Customer
      ↓
Sessions / Events / Orders
      ↓
Products / OrderItems
      ↓
Payments / Shipments / Returns
```

---

# 33. First File Recommendation

The inventory indicates that the first implementation change should be:

```text
RandomGenerator.scala
```

Target:

```text
src/main/scala/com/shopsphere/datagenerator/common/random/RandomGenerator.scala
```

Why start here?

1. It is low-level infrastructure.
2. It has a clear responsibility.
3. It has few dependencies.
4. Many generators depend on it.
5. Moving it establishes the first target architecture boundary.
6. It does not require us to redesign business behavior.
7. Its behavior should remain unchanged.
8. Its tests provide a clean regression point.

However, before modifying it, we should inspect its complete implementation and its exact test usage.

The first change should be a **package/ownership migration**, not a redesign.

---

# 34. First Migration Example

Conceptually:

```text
BEFORE

distribution/
└── RandomGenerator.scala


AFTER

common/
└── random/
    └── RandomGenerator.scala
```

Then update imports.

Run:

```powershell
sbt compile
sbt test
```

If successful, review the change before proceeding.

No other architecture migration should be bundled into that change.

---

# 35. Important Architectural Conclusions

The source inventory gives us several useful conclusions.

### Conclusion 1

The project does **not** need a rewrite.

The current implementation has a viable foundation.

### Conclusion 2

The primary architectural issue is **domain ownership**, not fundamentally incorrect technology.

### Conclusion 3

The `generation/` package is overloaded.

It currently means:

```text
"anything that generates something"
```

The target architecture should make generation behavior belong to the domain that owns the business meaning.

### Conclusion 4

The `model/` package is similarly overloaded.

Simple entity models should move to domain ownership.

### Conclusion 5

Cross-cutting infrastructure is already partly separated.

We should preserve that strength rather than moving everything into domains.

### Conclusion 6

Customer and Product are the two most important complex domains.

Customer becomes the behavioral anchor.

Product becomes the demand/pricing/catalog anchor.

### Conclusion 7

Order is the primary transactional orchestration boundary.

Relationship logic should evolve into explicit domain services where cross-entity behavior genuinely exists.

### Conclusion 8

Validation and statistics should remain globally orchestrated but should not become giant classes containing every domain rule.

### Conclusion 9

The missing `quality`, `scenario`, `manifest`, and `observability` packages are intentional architectural gaps, not failures.

They are planned future capabilities.

### Conclusion 10

The first migration should be technically small and architecturally meaningful.

That is why `RandomGenerator.scala` is the recommended first file.

---

# 36. Current → Target Architecture at a Glance

```text
CURRENT
──────────────────────────────────────

config
distribution
generation
model
output
reference
relationship
statistics
validation


                    ↓
              controlled migration
                    ↓


TARGET
──────────────────────────────────────

common
  ├── random
  ├── distribution
  ├── time
  └── util

geography
customer
address
category
brand
product
order
orderitem
payment
shipment
return
session
event

generation
relationship
quality
scenario
validation
statistics
output
manifest
observability
```

The key transition is:

```text
technical grouping
       ↓
domain ownership
```

---

# 37. Execution Rule From This Point

We now have enough information to stop discussing architecture abstractly and start implementing it.

The working rule is:

> **One file. One responsibility change. Compile. Test. Review. Repeat.**

The next implementation action is therefore:

```text
Move/refactor RandomGenerator.scala
        ↓
update affected imports
        ↓
update RandomGeneratorTest.scala package/location if necessary
        ↓
sbt compile
        ↓
sbt test
```

Nothing else should be changed in that first migration step.

---

# 38. Baseline

The previously established functional baseline remains the reference point:

```text
229 tests passing
End-to-end generation successful
Structural validation PASS
Primary-key validation PASS
Foreign-key validation PASS
Business-rule validation PASS
```

Architectural migration must preserve this baseline unless a deliberate behavior change is being introduced.

The realism baseline is separate: current data is functionally coherent but still requires substantial behavioral improvement later.

---

# 39. Final Inventory Status

```text
SOURCE INVENTORY              COMPLETE
PACKAGE INVENTORY             COMPLETE
TEST INVENTORY                COMPLETE
REFERENCE DATA INVENTORY      COMPLETE
ARCHITECTURAL CLASSIFICATION  COMPLETE
MAJOR HOTSPOTS IDENTIFIED     COMPLETE
MISSING CAPABILITIES          IDENTIFIED
MIGRATION ORDER               DEFINED
FIRST FILE                    IDENTIFIED
```

## Next Step

**Begin the first controlled migration with `RandomGenerator.scala`.**

Before changing its contents, inspect the complete file and its test, then make the smallest possible package/ownership change.

This keeps the implementation exactly aligned with the documented execution plan.
