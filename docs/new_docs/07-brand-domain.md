# 07 — Brand Domain

## 1. Purpose

This document defines the business meaning, model, relationships, reference-data responsibilities, generation boundaries, configuration, validation, behavioral role, realism roadmap, and technical architecture of the **Brand** domain in the ShopSphere e-commerce data generator.

Brand is a catalog domain that identifies the commercial brand associated with a Product.

The central architectural principle is:

> **Brand owns brand identity and brand-level reference characteristics; Product references Brand.**

Brand should remain independent from transactional domains and should not own customer behavior, pricing algorithms, order generation, or output serialization.

---

# 2. Business Meaning

A Brand represents a commercial identity under which one or more ShopSphere products are sold.

A brand can influence:

- product naming,
- catalog composition,
- category participation,
- customer preference,
- product popularity,
- pricing characteristics,
- promotional behavior,
- return behavior,
- demand concentration.

The Brand domain is therefore more than a string attached to a Product.

It is a catalog dimension that can become an important behavioral dimension in the realistic data model.

---

# 3. Current Brand State

The current baseline dataset contains approximately:

```text
26 brands
```

Brands are currently part of the generated catalog.

The current implementation should be treated as the functional baseline.

The detailed Brand model should remain aligned with the existing implementation and reference/catalog data until the domain migration is performed.

---

# 4. Brand Ownership

## Brand owns

- brand identity,
- brand name,
- brand reference data,
- brand-level metadata when explicitly modeled,
- brand validation.

## Product owns

- product identity,
- product attributes,
- product price,
- product availability,
- Product → Brand relationship.

## Category owns

- product taxonomy.

## Customer owns

- brand affinity,
- purchasing behavior,
- brand preference.

## Order owns

- transactions involving Products.

Brand should not own transaction history.

---

# 5. Product Relationship

The primary relationship is:

```text
Brand
   │
   │ 1
   │
   └──────────────< Product
```

Conceptually:

```text
Brand 1 → N Product
```

A Product references a Brand.

The Brand domain does not need to hold all generated Products in memory merely because the business relationship exists.

This keeps Brand a reference/catalog domain rather than turning it into a transaction-aware domain.

---

# 6. Category and Brand Are Different Dimensions

Category and Brand answer different questions.

Category:

> "What kind of product is this?"

Brand:

> "Under which commercial brand is this product sold?"

A Product can therefore have:

```text
Category = Electronics
Brand    = Brand A
```

These are independent dimensions.

They should not be merged into a single Product classification.

---

# 7. Brand Across Categories

A realistic catalog generally allows a brand to participate in multiple categories.

Conceptually:

```text
Brand A
   ├── Electronics
   ├── Accessories
   └── Home Devices
```

while another brand may operate only in:

```text
Brand B
   └── Beauty
```

This creates a useful relationship:

```text
Brand ↔ Category
```

even though the current Product model may represent the relationship indirectly through Products.

---

# 8. Product as the Relationship Carrier

A simple implementation is:

```text
Product
  ├── categoryId
  └── brandId
```

This means the generated Product population implicitly defines the Brand-Category combination.

This is preferable to introducing a separate BrandCategory entity unless the business model actually needs explicit relationship metadata.

---

# 9. Brand-Category Affinity

A future catalog model may assign different brand probabilities within different categories.

For example:

```text
Electronics
  Brand A → 40%
  Brand B → 20%
  Brand C → 10%
  Others  → 30%

Beauty
  Brand A → 5%
  Brand D → 45%
  Brand E → 20%
  Others  → 30%
```

This is much more realistic than one global brand distribution.

The future relationship can therefore be represented as:

```text
Category
   ↓
Brand affinity within category
   ↓
Product
```

---

# 10. Brand Popularity

Brand popularity is different from:

- number of products,
- category popularity,
- product popularity,
- customer brand preference.

A brand can have:

```text
many products
but low demand
```

or:

```text
few products
but extremely high demand
```

Therefore these concepts should remain separate.

---

# 11. Brand and Product Popularity

A future demand hierarchy can be:

```text
Global demand
    ↓
Category demand
    ↓
Brand demand within category
    ↓
Product demand within brand/category
```

This creates nested concentration.

For example:

```text
Category A
    ├── Brand X
    │     ├── Product 1  ← hot
    │     ├── Product 2
    │     └── Product 3
    │
    └── Brand Y
          ├── Product 4
          └── Product 5
```

This is a stronger realism model than assigning product popularity globally.

---

# 12. Brand and Customer Behavior

Brand becomes important when CustomerBehaviorProfile is introduced.

A customer may have:

```text
brand affinity
```

which can influence product selection.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
Category affinity
        ↓
Brand affinity
        ↓
Product popularity
        ↓
Product selection
```

This creates coherent customer purchasing patterns.

---

# 13. Brand Affinity Is Probabilistic

Brand preference should not generally be a hard constraint.

A customer who prefers Brand A can still buy Brand B.

Therefore:

```text
high affinity
```

should mean:

```text
higher probability
```

rather than:

```text
only this brand is allowed
```

This preserves realistic variation.

---

# 14. Brand Affinity Can Be Conditional

A customer's preference for a brand can vary by category.

For example:

```text
Customer
  Electronics → Brand A strongly preferred
  Clothing    → Brand B preferred
  Home        → no strong preference
```

This suggests that future behavior should not necessarily be:

```text
customer → one global brand score
```

but potentially:

```text
customer
   ↓
category
   ↓
brand affinity
```

This is a more expressive behavioral model.

---

# 15. Brand and Price

Brands can influence pricing characteristics.

A future Product pricing model may consider:

```text
category
+
brand
+
product tier
+
base product characteristics
+
promotion
```

For example:

```text
Brand tier
   ↓
price distribution
```

But actual price generation belongs to Product/Pricing behavior.

Brand should not contain a giant pricing algorithm.

---

# 16. Brand and Returns

Return rates can vary by brand indirectly through:

- product characteristics,
- quality,
- price,
- customer expectations,
- category,
- fulfillment.

A future return model could therefore use:

```text
customer return propensity
+
category return tendency
+
brand/product factors
+
order context
```

Brand supplies relevant characteristics if the model eventually requires them.

Return probability remains a Return-domain behavior.

---

# 17. Brand and Promotions

Promotions can be:

- brand-wide,
- category-wide,
- product-specific,
- customer-specific.

A future scenario might create:

```text
BrandPromotionScenario
```

which increases demand for selected brands.

The Scenario domain should apply the promotion effect.

Brand should remain a catalog/reference domain.

---

# 18. Brand and Seasonality

Some brands may have demand patterns that vary over time.

For example:

```text
Brand
   ↓
seasonal demand profile
```

However, this should only be modeled when the project has a temporal demand engine.

The temporal engine should own time calculations.

Brand can provide a classification or parameter.

---

# 19. Brand Cardinality

Current baseline:

```text
~26 brands
```

Brand count should eventually be configurable if large catalogs are required.

Possible profiles:

```text
small
medium
large
```

But cardinality should be based on the intended catalog structure.

Simply increasing Brand count does not automatically improve realism.

---

# 20. Brand Concentration

Brands often have unequal market presence.

A realistic distribution may contain:

```text
a few dominant brands
many moderate brands
a long tail of small brands
```

This can be represented using a long-tail popularity distribution.

Conceptually:

```text
Brand rank
    ↓
market share
```

where top-ranked brands account for disproportionately more demand.

---

# 21. Brand Popularity vs Product Count

Do not equate:

```text
more products
=
more demand
```

Product count and demand are separate variables.

For example:

```text
Brand A
  100 products
  15% of demand

Brand B
  20 products
  25% of demand
```

This is a perfectly valid business model.

The generator should allow these variables to diverge.

---

# 22. Brand Long-Tail Model

A future brand demand model may use:

```text
Pareto
Zipf
log-normal
categorical weighted distribution
```

depending on the desired behavior.

The exact distribution should be selected based on the statistical property being modeled.

Do not use Zipf simply because the dataset is supposed to have a long tail.

---

# 23. Brand and Category Distribution

Brand selection should ideally happen conditionally on Category.

Conceptually:

```text
select category
      ↓
select brand available in category
      ↓
select product within category + brand
```

This avoids impossible or implausible brand/category combinations.

---

# 24. Brand Availability

Not every brand necessarily participates in every category.

A future catalog model should therefore be able to represent:

```text
Brand A → Electronics
Brand A → Home

Brand B → Beauty only
```

The simplest representation may be derived from Product reference data.

A dedicated BrandCategory relationship should be introduced only if it carries independent business information.

---

# 25. Current Simplification

The current system may effectively treat Brand as a relatively simple catalog dimension.

This is acceptable for the baseline.

The sophisticated model should not be implemented until Product and Customer behavior models are ready to consume it.

---

# 26. Reference Data

Brand data may be represented as catalog/reference data.

The Brand domain should provide:

```text
stable IDs
stable names
```

and future metadata if needed.

Raw catalog files should be hidden behind a loader/reference boundary.

---

# 27. Brand Reference API

Potential operations include:

```scala
findBrand(id)
allBrands
brandsInCategory(categoryId)
```

The exact API should remain minimal.

Do not create lookup methods for every conceivable query before consumers require them.

---

# 28. Stable Ordering

Brand selection must support reproducibility.

If brands are stored in an unordered collection, selection should use a stable representation.

For example:

```text
sort by brand ID
```

or maintain a stable vector.

This ensures:

```text
same seed
+
same reference data
```

continues to produce deterministic selection.

---

# 29. Configuration Boundary

Brand configuration should be loaded through the common configuration subsystem.

Preferred flow:

```text
HOCON
  ↓
ConfigLoader
  ↓
typed configuration
  ↓
Brand behavior
```

The Brand domain should not directly parse HOCON.

---

# 30. Strategy Pattern Assessment

Strategy becomes justified if the project supports multiple Brand selection models.

Potential strategies:

```text
UniformBrandSelection
WeightedBrandSelection
CategoryConditionalBrandSelection
CustomerAffinityBrandSelection
ScenarioBrandSelection
```

These represent genuine alternative business behaviors.

The project should not implement all of them immediately.

The first strategy that introduces meaningful variation should be isolated cleanly.

---

# 31. Composition Over Inheritance

A Product generator should be composed from behaviors rather than subclassed for every scenario.

For example:

```text
CategorySelector
+
BrandSelector
+
ProductPopularityModel
+
PricingModel
```

is preferable to:

```text
ElectronicsProductGenerator
PremiumBrandProductGenerator
PromotionalProductGenerator
...
```

This avoids class explosion.

---

# 32. Factory Pattern Assessment

A BrandFactory is unnecessary for simple Brand records.

A factory may become appropriate for selecting a Brand selection strategy:

```text
configuration
    ↓
BrandSelectionModelFactory
    ↓
selected strategy
```

This is a better use of Factory than creating trivial factories around case classes.

---

# 33. Builder Pattern Assessment

Builder is not required for simple Brand models.

Use immutable case-class construction unless Brand metadata becomes genuinely complex.

---

# 34. Domain Service Assessment

A Brand-specific domain service is not currently required.

A future service may become appropriate for operations such as:

```text
resolve eligible brands for category
```

if eligibility rules become sufficiently complex.

Otherwise, a lookup/selection abstraction is clearer.

---

# 35. SOLID Assessment

## Single Responsibility

Brand should own brand identity/reference information.

Product should own product generation.

Customer behavior should own preference.

---

## Open/Closed

Brand selection behavior should be extensible through meaningful strategies where required.

---

## Liskov Substitution

Relevant only where multiple selection strategies implement a common contract.

---

## Interface Segregation

Avoid a giant catalog interface that combines:

```text
Brand
Category
Product
Pricing
Inventory
Promotion
```

---

## Dependency Inversion

Consumers should depend on Brand lookup/selection boundaries rather than raw files.

---

# 36. Testing Strategy

## 36.1 Model tests

Verify:

- brand ID,
- brand name,
- equality,
- required invariants.

---

## 36.2 Reference tests

Verify:

```text
brand IDs unique
brand names valid
reference data loads
```

---

## 36.3 Product integration tests

Verify:

```text
Product.brandId references existing Brand
```

and:

```text
Product.categoryId references existing Category
```

---

## 36.4 Brand-category tests

When conditional brand selection is implemented:

```text
selected brand must be valid for selected category
```

---

# 37. Statistical Validation

Brand-level statistics should eventually include:

```text
products per brand
orders per brand
order items per brand
revenue per brand
returns per brand
brand share of demand
```

Useful concentration measurements include:

```text
top 1 brand share
top 5 brand share
top 10% brand share
brand entropy
brand Gini/concentration
```

These provide measurable realism.

---

# 38. Customer-Brand Correlation Validation

Once CustomerBehaviorProfile exists, validation should test whether intended relationships are actually visible.

For example:

```text
high brand affinity customers
    ↓
higher probability of purchasing preferred brand
```

But the relationship should not be so deterministic that it eliminates natural variation.

Statistical tests should validate approximate relationships rather than exact customer-level outcomes.

---

# 39. Brand Skew Scenarios

Brand is an excellent target for controlled skew.

Examples:

## Hot brand

One or a few brands dominate demand.

## Long-tail brand

A small set dominates while many brands receive little demand.

## Category-specific brand dominance

One brand dominates Electronics while another dominates Beauty.

## Promotional brand spike

A selected brand experiences temporary demand amplification.

These scenarios are useful for Spark aggregation and join-skew experiments.

---

# 40. Data-Quality Scenarios

Potential Brand data-quality defects:

```text
missing brand name
duplicate brand ID
invalid brand reference
malformed metadata
```

As with other domains:

```text
valid Brand
    ↓
Quality Engine
    ↓
controlled corruption
```

is preferred.

The baseline Brand generator should not randomly create invalid brands.

---

# 41. Output Responsibility

Brand should not write CSV.

The output layer serializes Brand records.

Current baseline output includes:

```text
brands.csv
```

The output schema should remain derived from the actual Brand model.

---

# 42. Statistics Responsibility

Brand generation should not own global demand statistics.

Statistics should calculate Brand-level distributions after Products and transaction data are generated.

This allows the project to compare:

```text
configured brand behavior
```

against:

```text
observed brand behavior
```

---

# 43. Manifest Responsibility

The manifest may eventually record:

```text
brand reference-data version
brand count
active brand distribution model
scenario
```

The Brand domain itself should not create the manifest.

---

# 44. Dependency Direction

Preferred dependency structure:

```text
Product
   ↓
Brand
Category
   ↓
common/reference infrastructure
```

Customer behavior may consume Brand:

```text
CustomerBehavior
   ↓
Brand
```

Brand should not depend on Customer.

---

# 45. Circular Dependency Avoidance

Avoid:

```text
Brand → Product → Brand
```

at the code level.

The business relationship is represented by:

```text
Product.brandId
```

rather than Brand maintaining generated Product collections.

---

# 46. Brand and Product Generator

The Product generator will eventually need to coordinate:

```text
Category selection
Brand selection
Product identity
Product attributes
Pricing
Popularity
```

A readable business flow could become:

```scala
val category = categorySelector.select(context)
val brand = brandSelector.select(category, context)
val product = productFactory.create(category, brand, context)
```

The exact implementation will depend on the Product domain design.

---

# 47. Brand as a Conditional Distribution

A particularly important future capability is:

```text
P(Brand | Category)
```

rather than simply:

```text
P(Brand)
```

This allows the catalog to represent realistic specialization.

Later, customer behavior can further condition selection:

```text
P(Brand | Customer, Category, Time, Scenario)
```

The generator does not need to implement the full formula immediately.

The architecture should leave room for this evolution.

---

# 48. Brand Demand Model

The eventual demand model may be expressed conceptually as:

```text
brand demand
=
global brand popularity
×
category compatibility
×
customer affinity
×
temporal effects
×
promotion effects
×
scenario effects
×
random variation
```

This is a conceptual model, not a required literal implementation.

It illustrates how multiple independent concerns can compose into realistic behavior.

---

# 49. Avoid Giant Brand Logic

Do not allow Brand selection to become:

```scala
if (category == ...)
else if (customerSegment == ...)
else if (month == ...)
else if (scenario == ...)
...
```

Such logic becomes difficult to test and maintain.

Prefer composition:

```text
base brand distribution
+
category conditioning
+
customer affinity
+
temporal modifier
+
scenario modifier
```

with clear boundaries.

---

# 50. Brand and Customer Lifecycle

Brand affinity may evolve with customer lifecycle.

For example:

```text
new customer
    ↓
exploration
    ↓
preference formation
    ↓
loyal behavior
```

A mature CustomerBehaviorProfile could therefore contain evolving brand preferences.

This belongs to Customer behavior rather than Brand.

Brand simply provides the set of possible commercial identities.

---

# 51. Brand and Acquisition

Marketing acquisition channels may influence brand exposure.

For example:

```text
Paid Search
    ↓
specific brand exposure
    ↓
brand preference
    ↓
purchase
```

This is a future correlation.

It should be modeled through Customer/Marketing behavior rather than embedding acquisition logic into Brand.

---

# 52. Brand and Events

Event data may reveal brand interest:

```text
product_view
add_to_cart
purchase
```

for products belonging to a particular brand.

The Event domain can therefore indirectly reflect Brand behavior.

Brand should not generate Events.

---

# 53. Brand and Return Behavior

A mature Return model could measure:

```text
return rate by brand
```

and determine return probability from:

```text
customer
+
category
+
brand
+
product
+
order
```

This creates cross-domain realism.

---

# 54. Current Simplifications

The current Brand model intentionally simplifies:

- brand hierarchy,
- brand-category metadata,
- brand market share,
- customer brand affinity,
- temporal brand behavior,
- promotional effects,
- brand lifecycle.

These are future capabilities.

---

# 55. Planned Future Realism

The Brand domain can evolve through:

```text
stable brand reference
        ↓
category-conditioned brand selection
        ↓
brand popularity distribution
        ↓
customer brand affinity
        ↓
temporal brand demand
        ↓
promotion effects
        ↓
brand-specific skew scenarios
```

This should happen after Product and Customer behavior architecture is ready.

---

# 56. Geographic Interaction

Brand behavior may also vary by geography.

For example:

```text
City A
  Brand X strong

City B
  Brand Y strong
```

This is a potential future model:

```text
P(Brand | Category, Geography)
```

It should not be implemented until geographic affinity is mature enough to support it.

---

# 57. Reference Data Versioning

Future manifests should record Brand reference-data version.

A reproducible dataset should therefore depend on:

```text
generator version
+
configuration
+
seed
+
Brand reference version
+
Category reference version
+
Geography reference version
+
scenario
```

where applicable.

---

# 58. Performance Considerations

Brand cardinality is small compared with transaction volume.

Therefore the preferred implementation is:

```text
load once
index once
reuse many times
```

Avoid repeated file parsing or expensive filtering during Product generation.

Category-conditioned Brand lookup may eventually benefit from:

```text
Map[CategoryId, Vector[Brand]]
```

or an equivalent immutable index.

---

# 59. Concurrency

Brand reference data should be immutable and safely shareable across parallel generation workers.

Selection randomness should remain isolated per generation context.

Do not use mutable global counters inside Brand selection.

---

# 60. Observability

Useful Brand metrics include:

```text
brand count
products per brand
demand share by brand
revenue share by brand
return rate by brand
top-brand concentration
```

Scenario metrics can include:

```text
hot-brand share
brand distribution mode
category-conditioned brand concentration
```

---

# 61. Spark Performance Applications

Brand provides useful keys for:

```text
groupBy brand
join Product ↔ Brand
join Product ↔ Category
brand-level revenue aggregation
brand-level return aggregation
```

Skew scenarios can demonstrate:

```text
hot-key aggregation
partition imbalance
join skew
shuffle amplification
```

This makes Brand useful beyond catalog semantics.

---

# 62. What Brand Should Not Solve

Brand should not own:

```text
Product generation
Product pricing algorithm
Customer preferences
Order generation
Payment
Shipment
Return decisions
Event generation
CSV output
global statistics
scenario orchestration
```

It defines commercial identity and reference characteristics.

---

# 63. Proposed Target Package

The target package can remain intentionally small:

```text
com.shopsphere.datagenerator.brand/
├── model/
├── reference/
└── validation/
```

If Brand selection strategies become substantial, a generator/selection package can be introduced:

```text
brand/
├── model/
├── reference/
├── selection/
└── validation/
```

Do not create `selection/` until selection behavior actually belongs here.

---

# 64. Migration Plan

When Brand is migrated:

## Step 1

Inspect current Brand model and catalog/reference data.

## Step 2

Identify Brand loading logic.

## Step 3

Identify Brand configuration.

## Step 4

Move the model into:

```text
brand/model
```

## Step 5

Move reference loading into the appropriate Brand boundary.

## Step 6

Move Brand validation.

## Step 7

Update Product dependencies.

## Step 8

Update category/brand selection code where necessary.

## Step 9

Run Brand tests.

## Step 10

Run Product integration tests.

## Step 11

Run the complete suite.

## Step 12

Run end-to-end generation.

## Step 13

Compare product/brand distributions with the baseline.

---

# 65. Migration Quality Gate

Brand migration is complete when:

### Business

- Brand meaning is explicit.
- Brand/Product relationship is explicit.
- Category interaction is understood.

### Reference

- Brand data loads correctly.
- IDs are unique.
- ordering is stable.

### Architecture

- Product consumes Brand through a clear boundary.
- Brand does not depend on transactions.

### Reproducibility

- same seed and reference data produce stable selection.

### Testing

- Brand tests pass.
- Product tests pass.
- full suite passes.

### Statistics

- brand counts remain correct,
- product-brand relationships remain valid,
- baseline distribution is preserved.

---

# 66. Design Decisions

## Decision A — Brand is a catalog dimension

Brand represents commercial identity.

---

## Decision B — Product references Brand

The Product model carries the relationship.

---

## Decision C — Brand and Category remain separate

They represent different business dimensions.

---

## Decision D — Brand-category behavior may be conditional

Future selection should support:

```text
P(Brand | Category)
```

when required.

---

## Decision E — Customer brand affinity belongs to Customer behavior

Brand does not own customer preferences.

---

## Decision F — Demand popularity is separate from catalog size

Number of products does not determine demand automatically.

---

## Decision G — Scenarios stay outside Brand

Hot-brand and promotional scenarios should modify selection behavior through the scenario architecture.

---

## Decision H — Avoid premature abstraction

Do not introduce Brand strategies, factories, services, or additional entities until there is a concrete business requirement.

---

# 67. Final Mental Model

The simplest useful model is:

```text
Category
    │
    │ determines eligible catalog
    ▼
Brand
    │
    │ commercial identity
    ▼
Product
    │
    │ demand
    ▼
Order Item
    │
    ▼
Order
```

With future behavioral realism:

```text
CustomerBehaviorProfile
        │
        ├── Category affinity
        │
        └── Brand affinity
                │
                ▼
          Product selection
                │
                ▼
             Purchase
```

And the catalog demand hierarchy becomes:

```text
Global demand
    ↓
Category demand
    ↓
Brand demand
    ↓
Product demand
```

---

# 68. Summary

Brand is a relatively small catalog domain but becomes an important behavioral dimension as the generator becomes more sophisticated.

Its primary responsibility is:

> **Define and expose commercial brand identities that Products can reference.**

The current baseline contains approximately 26 brands.

The immediate architecture should remain simple and focused:

```text
brand/
├── model/
├── reference/
└── validation/
```

The most important future capability is conditional brand behavior:

```text
P(Brand | Category)
```

followed by:

```text
P(Brand | Customer, Category)
```

and eventually potentially:

```text
P(Brand | Customer, Category, Geography, Time, Scenario)
```

The implementation should achieve this through composition and justified behavioral strategies rather than a giant conditional Brand generator.

Brand therefore acts as a key intermediate layer between:

```text
catalog taxonomy
        ↓
commercial identity
        ↓
product popularity
        ↓
customer preference
        ↓
transaction behavior
```

The next domain is **Product**, which is the most important catalog domain because it combines Category, Brand, pricing, popularity, and eventually Customer behavior into concrete purchasable items.
