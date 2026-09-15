# 06 — Category Domain

## 1. Purpose

This document defines the business meaning, hierarchy, model, reference-data responsibilities, generation boundaries, relationships, configuration, validation, technical architecture, realism roadmap, and future behavioral role of the **Category** domain in the ShopSphere e-commerce data generator.

Category is a foundational catalog domain.

It provides the taxonomy through which Products are organized and analyzed.

The central architectural principle is:

> **Category owns product taxonomy; Product references Category.**

Category should remain focused on classification.

It should not become responsible for product generation, pricing, inventory, order behavior, or customer behavior.

---

# 2. Business Meaning

A Category represents a logical product classification used by ShopSphere.

Categories allow the business to:

- organize products,
- browse the catalog,
- group products,
- analyze sales,
- model customer preferences,
- model product demand,
- create category-level scenarios,
- analyze returns and revenue.

The category domain is therefore both:

```text
catalog structure
+
analytical classification
```

---

# 3. Current Category Model

The current generated dataset contains:

```text
10 categories
```

The exact current model fields should remain aligned with the existing implementation and reference data.

The important business identity is:

```text
Category ID
Category name
```

If additional attributes are needed later, they should be introduced deliberately rather than added because they are common in commercial catalogs.

---

# 4. Category Ownership

## Category owns

- category identity,
- category taxonomy,
- category names,
- category hierarchy if introduced,
- category-level reference data,
- category validation,
- category metadata.

## Product owns

- product identity,
- product attributes,
- product price,
- product availability,
- product-to-category relationship.

## Customer owns

- category preferences,
- category affinity,
- customer behavior.

## Order owns

- purchased product/category through Order Items.

Category should not own transactions.

---

# 5. Product Relationship

The primary current relationship is:

```text
Category
   │
   │ 1
   │
   └──────────────< Product
```

Conceptually:

```text
Category 1 → N Product
```

A Product belongs to a Category.

This relationship allows the generator to create downstream behavior such as:

```text
Category
   ↓
Products
   ↓
Order Items
   ↓
Orders
```

---

# 6. Why Category Is Separate From Product

Category and Product have different business responsibilities.

A Category answers:

> "What type of product is this?"

A Product answers:

> "What specific product does ShopSphere sell?"

For example:

```text
Category
  Electronics

Product
  Wireless Noise Cancelling Headphones
```

The category is a classification.

The product is a concrete catalog item.

Keeping them separate enables category-level modeling without duplicating category information in every Product object.

---

# 7. Category as Reference Data

Category is currently closer to reference/master data than transactional data.

The category set is expected to be relatively small compared with:

```text
Customers
Orders
OrderItems
Events
```

This makes Category suitable for:

- in-memory reference data,
- stable lookup,
- deterministic selection,
- configuration-driven distributions.

---

# 8. Category Cardinality

Current baseline:

```text
10 categories
```

The number of categories should eventually be configurable.

Potential profiles:

```text
small catalog
medium catalog
large catalog
```

However, category count should not be increased merely to make a dataset larger.

The important property is that category cardinality is appropriate for the intended business scenario.

---

# 9. Category Hierarchy

The current design should not assume a multi-level category hierarchy unless the existing reference data and business model require it.

A future hierarchy could be:

```text
Electronics
   ├── Mobile
   ├── Computers
   └── Accessories
```

or:

```text
Home
   ├── Furniture
   ├── Kitchen
   └── Decor
```

But this should be an explicit design decision.

Do not add:

```text
parentCategoryId
```

just because hierarchical categories are common.

---

# 10. Flat vs Hierarchical Categories

Two valid models exist.

## Flat

```text
Category
  ├── Electronics
  ├── Clothing
  ├── Home
  └── Beauty
```

Advantages:

- simple,
- easy to generate,
- easy to analyze,
- fewer relationships.

---

## Hierarchical

```text
Electronics
   ↓
Computers
   ↓
Laptops
```

Advantages:

- richer catalog realism,
- category rollups,
- multi-level analytics,
- more realistic browsing behavior.

The current project should retain the simpler model unless a hierarchical catalog is required.

---

# 11. Category and Product Distribution

Category is important because Product distribution should generally not be uniform.

For example:

```text
Category A → 25% of products
Category B → 20%
Category C → 15%
Others     → 40%
```

The exact values are scenario-specific.

The architectural point is:

> Product generation should be able to use category-level distributions.

---

# 12. Category Popularity vs Product Popularity

These are different concepts.

Category popularity:

```text
How much demand exists for a category?
```

Product popularity:

```text
How much demand exists for a specific product?
```

A category may contain:

```text
many products
```

while only a few products account for most purchases.

Therefore:

```text
Category popularity
        ↓
Product selection within category
        ↓
Product popularity
```

should eventually be modeled as separate levels.

---

# 13. Category as a Behavioral Dimension

Category becomes especially important once CustomerBehaviorProfile is introduced.

A customer may have:

```text
category affinity
```

such as:

```text
Electronics = high
Books       = medium
Furniture   = low
```

This can influence product selection.

The resulting flow becomes:

```text
CustomerBehaviorProfile
        ↓
Category affinity
        ↓
Category selection
        ↓
Product popularity
        ↓
Product selection
```

This is a major future realism capability.

---

# 14. Category Affinity

Category affinity should not be interpreted as a permanent deterministic preference.

A customer can:

- prefer a category,
- occasionally purchase outside it,
- change preferences,
- respond to promotions,
- follow seasonal demand.

Therefore a future affinity model should produce probabilities or scores rather than hard constraints.

Conceptually:

```text
customer category preference
+
global category popularity
+
seasonality
+
promotion
+
random variation
        ↓
category selection probability
```

---

# 15. Category and Seasonality

Different categories can have different temporal demand.

For example:

```text
Category
    ↓
seasonality profile
```

Potential patterns:

```text
Winter → higher winter-product demand
Festival → higher gift demand
Back-to-school → higher stationery demand
Summer → higher cooling/travel demand
```

The exact business categories are scenario-specific.

The Category domain can eventually hold or reference a seasonality classification, but the temporal engine should own time-dependent calculations.

---

# 16. Category and Promotions

Promotions may apply to:

- a category,
- a product,
- a brand,
- a customer segment.

A future promotion model could influence category demand.

For example:

```text
promotion
   ↓
category demand multiplier
   ↓
product selection
   ↓
order
```

Promotion logic should not be embedded directly into Category.

Category provides classification.

A separate scenario/promotion model applies business effects.

---

# 17. Category and Pricing

Category often influences product pricing.

Conceptually:

```text
Category
   ↓
Product price distribution
```

For example, different categories may have different:

- price ranges,
- median prices,
- price variance,
- discount rates.

The Product domain should own actual product pricing behavior.

Category may provide reference characteristics if the business model requires them.

---

# 18. Category and Returns

Return behavior may vary significantly by category.

A future model could use:

```text
customer return propensity
+
category return tendency
+
product characteristics
+
order context
```

to determine return probability.

Again:

> Category provides classification; Return owns return behavior.

---

# 19. Category and Spark Workloads

Category is valuable for downstream Spark workloads.

Examples:

```text
Revenue by category
Orders by category
Returns by category
Products per category
Customers by preferred category
Category-level joins
Category-level aggregations
```

Category skew is also useful for performance experiments.

---

# 20. Category Skew

A realistic catalog may have uneven product counts.

Example:

```text
Category A → 30%
Category B → 20%
Category C → 15%
Long tail  → 35%
```

This creates natural group-size variation.

A scenario can make category concentration much stronger.

Example:

```text
HotCategoryScenario
```

could produce:

```text
one category → 50% of demand
```

The scenario should modify the distribution model rather than hard-code conditions inside ProductGenerator.

---

# 21. Category Reference Data

The Category domain should provide stable category reference data to Product and behavioral models.

Conceptually:

```scala
category.id
category.name
```

and potentially:

```scala
category.metadata
```

if future business rules require it.

---

# 22. Stable Ordering

Category selection must be reproducible.

If categories are stored in a Map, random selection should not rely on unspecified iteration order.

Preferred approach:

```text
stable category ordering
+
deterministic random stream
```

This follows the same reproducibility principle used by Geography.

---

# 23. Category Configuration

Category configuration should control meaningful business behavior.

Potential configuration:

```hocon
categories {
  count = 10

  distribution {
    mode = "weighted"
  }
}
```

The exact configuration should reflect actual implementation.

Avoid exposing internal data structures as configuration.

---

# 24. Category Distribution Configuration

A more useful future configuration might define category weights:

```hocon
category-distribution {
  electronics = 0.20
  clothing = 0.18
  home = 0.15
  beauty = 0.10
}
```

The exact categories and weights should come from the business/reference model.

The important design principle is:

> Business-level distribution controls belong in configuration; random-number implementation details do not.

---

# 25. Strategy Pattern Assessment

Category selection may eventually require Strategy.

Possible strategies:

```text
UniformCategorySelection
WeightedCategorySelection
ScenarioCategorySelection
CustomerAffinityCategorySelection
```

These are legitimate alternatives because they represent different business selection behavior.

However, do not introduce all four before the system actually needs them.

A simple weighted distribution can remain a direct dependency on the existing distribution engine.

---

# 26. Product Selection Boundary

Product should not directly parse category configuration files.

Preferred:

```text
Category reference data
        ↓
Product generation
```

and:

```text
Category selection strategy
        ↓
selected Category
        ↓
Product generation
```

This keeps Category and Product responsibilities explicit.

---

# 27. Factory Pattern Assessment

A CategoryFactory is not required for simple category records.

A factory could become useful if category creation includes:

- hierarchical construction,
- metadata derivation,
- scenario-specific category creation.

Until then:

```scala
Category(...)
```

is preferable.

---

# 28. Builder Pattern Assessment

Builder is unnecessary for simple Category objects.

Use case-class construction unless category construction becomes genuinely complex.

---

# 29. Domain Service Assessment

Category-specific services are not currently necessary.

A service might become useful for:

```text
resolve category hierarchy
calculate category rollup
```

if those operations become sufficiently complex.

Simple lookup should remain simple lookup.

---

# 30. SOLID Assessment

## Single Responsibility

Category should own classification/reference information.

Product should own product generation.

Customer should own customer preference.

Order should own transactions.

---

## Open/Closed

Category selection strategies can be extended if multiple business selection mechanisms become necessary.

---

## Liskov Substitution

Relevant only if strategy interfaces are introduced.

---

## Interface Segregation

Avoid large catalog interfaces combining:

```text
category
brand
product
pricing
inventory
```

---

## Dependency Inversion

Consumers should depend on meaningful category lookup/selection boundaries rather than raw configuration files.

---

# 31. Testing Strategy

## 31.1 Category model tests

Verify:

- ID,
- name,
- equality,
- expected invariants.

---

## 31.2 Reference tests

Verify:

- category IDs are unique,
- category names are valid,
- reference data loads correctly.

---

## 31.3 Distribution tests

Verify:

- weights are valid,
- probabilities are within bounds,
- configured categories are selectable.

---

## 31.4 Product integration tests

Verify:

```text
every Product references an existing Category
```

and:

```text
configured category distribution
```

is approximately respected.

---

# 32. Statistical Validation

Category-level statistics should eventually include:

```text
products per category
orders per category
order items per category
revenue per category
returns per category
customers by category affinity
```

Useful concentration metrics include:

```text
top category share
top 10% category share
category entropy
category Gini/concentration
```

These metrics become especially useful for skew scenarios.

---

# 33. Current Simplifications

## Simplification 1 — Small category set

The baseline contains approximately ten categories.

---

## Simplification 2 — No required hierarchy

The current design does not require nested categories.

---

## Simplification 3 — No temporal category lifecycle

Categories are effectively static during a generation run.

---

## Simplification 4 — Limited category behavior

Category currently acts primarily as product classification.

---

## Simplification 5 — No explicit category affinity model

Customer category preferences are future behavior.

---

# 34. Planned Future Realism

Future Category capabilities may include:

```text
category hierarchy
category demand distributions
customer category affinity
category seasonality
category promotion response
category-specific price distributions
category-specific return rates
category-level demand skew
category lifecycle
```

These should be introduced incrementally.

---

# 35. Category Lifecycle

A future catalog may model:

```text
active
    ↓
declining
    ↓
inactive
```

or:

```text
planned
   ↓
active
   ↓
retired
```

This may matter for temporal product generation.

However, categories should not be given lifecycle state until Product lifecycle requires it.

---

# 36. Category and Product Lifecycle

A future product lifecycle might be:

```text
Category
   ↓
Product introduced
   ↓
Product grows
   ↓
Product matures
   ↓
Product declines
   ↓
Product retired
```

Category-level demand can influence each stage.

This is a future temporal model rather than a current requirement.

---

# 37. Category and Brand

Category and Brand are independent classification dimensions.

A Product may have:

```text
Category = Electronics
Brand    = Brand A
```

This enables:

```text
category affinity
+
brand affinity
```

to influence product selection.

The future selection model could therefore become:

```text
Customer
  ↓
Category affinity
  ↓
Brand affinity within category
  ↓
Product popularity
  ↓
Product selection
```

This will be important for realistic product demand.

---

# 38. Category-Brand Affinity

A future catalog may have category-specific brand distributions.

For example:

```text
Electronics
  Brand A → 30%
  Brand B → 25%
  Brand C → 10%
  Others  → 35%
```

while another category has completely different brand concentration.

This should be modeled as a relationship between Category and Brand rather than assuming one global brand distribution.

---

# 39. Category and Product Popularity

The hierarchy of demand should eventually look like:

```text
Global demand
    ↓
Category demand
    ↓
Brand demand within category
    ↓
Product demand within brand/category
```

This creates a much more realistic long-tail structure than selecting Products uniformly.

---

# 40. Long-Tail Modeling

Category provides a natural boundary for long-tail modeling.

Instead of:

```text
all products → one global distribution
```

use:

```text
category
   ↓
products
   ↓
category-specific popularity distribution
```

For example:

```text
Category A
    product 1 → very popular
    product 2 → popular
    product 3 → moderate
    ...
    product N → rare
```

This creates realistic concentration.

---

# 41. Category and Customer Behavior

Customer behavior should not be encoded directly in Category.

Instead:

```text
CustomerBehaviorProfile
        ↓
category affinity
        ↓
category selection
        ↓
Product selection
```

The Category domain remains a reference dimension.

This is an important separation between:

```text
what categories exist
```

and:

```text
what a customer prefers
```

---

# 42. Category and Scenario Engine

Scenarios may modify category-level distributions.

Examples:

```text
category-demand-spike
seasonal-category
category-skew
category-quality-problem
```

The Scenario domain should own scenario composition.

Category should expose the data needed by the scenario rather than containing scenario-specific branches.

---

# 43. Data-Quality Scenarios

Possible category data-quality problems:

```text
missing category name
duplicate category ID
unknown category reference
invalid category metadata
```

As with other domains:

```text
clean generation
        ↓
optional Quality Engine
        ↓
controlled corruption
```

is preferred.

---

# 44. Output Responsibility

Category should not write CSV.

The output layer owns serialization.

Current generated output includes:

```text
categories.csv
```

The exact output schema should be derived from the Category model and documented separately by the output contract.

---

# 45. Statistics Responsibility

Category generation should not calculate global category statistics.

Statistics should consume generated Products/OrderItems/Orders and derive:

```text
product concentration
demand concentration
revenue concentration
return concentration
```

This keeps generation and measurement separate.

---

# 46. Configuration Responsibility

Category configuration belongs to the configuration subsystem.

The Category domain should consume validated configuration rather than parse HOCON directly.

Preferred:

```text
HOCON
  ↓
ConfigLoader
  ↓
typed configuration
  ↓
Category behavior
```

rather than:

```text
CategoryGenerator
  ↓
ConfigFactory.parseResources(...)
```

---

# 47. Dependency Direction

Preferred direction:

```text
Product
   ↓
Category
   ↓
common/reference infrastructure
```

Customer behavior may consume Category:

```text
CustomerBehavior
   ↓
Category
```

But Category should not depend on Customer.

This keeps the reference domain lower in the dependency graph.

---

# 48. Circular Dependency Avoidance

Avoid:

```text
Category → Product → Category
```

at the code/package dependency level.

The business relationship is naturally:

```text
Product references Category
```

Category does not need to hold all generated Products.

This distinction prevents unnecessary circular dependencies.

---

# 49. In-Memory Representation

Because Category cardinality is relatively small, an immutable collection is appropriate.

Conceptually:

```scala
Map[CategoryId, Category]
```

or equivalent.

A stable ordered sequence can support deterministic selection:

```scala
Vector[Category]
```

The exact representation should be chosen based on actual access patterns.

---

# 50. Reference Data Loading

If categories come from reference files:

```text
CategoryLoader
```

should:

1. load records,
2. parse records,
3. validate records,
4. construct the in-memory representation,
5. expose it to consumers.

The loader should not generate synthetic categories unless explicitly designed to do so.

---

# 51. Generated vs Reference Categories

There are two possible models.

## Reference categories

A fixed category catalog is loaded from data.

Advantages:

- stable business taxonomy,
- reproducibility,
- realistic names,
- easier downstream analysis.

## Generated categories

Categories themselves are synthetically generated.

Advantages:

- configurable cardinality,
- arbitrary catalog sizes.

The current project should continue using reference/catalog data where appropriate and introduce generated taxonomy only if there is a clear need.

---

# 52. Configuration-Driven Cardinality

If future scenarios require large category cardinality, the system should distinguish:

```text
reference taxonomy
```

from:

```text
synthetic expansion
```

For example:

```text
base categories = 10
synthetic subcategories = configurable
```

This should be designed explicitly rather than silently duplicating categories.

---

# 53. Reproducibility

Category behavior should be reproducible under:

```text
same seed
+
same configuration
+
same reference data
```

Changes to category ordering or reference-data version may change downstream generated records.

Therefore category reference data should eventually have a version identifier.

---

# 54. Observability

Useful Category metrics include:

```text
category count
products/category
order items/category
revenue/category
returns/category
```

Scenario diagnostics may include:

```text
active category skew
top category share
distribution mode
```

These metrics should be emitted through the system's statistics/observability layer.

---

# 55. Spark Performance Considerations

Category provides natural grouping keys for Spark experiments.

Examples:

```scala
groupBy("category_id")
```

and joins:

```text
Product
JOIN Category
```

Potential performance scenarios:

```text
uniform category distribution
moderate category skew
extreme category skew
```

These can demonstrate:

- aggregation imbalance,
- partition skew,
- shuffle behavior,
- join performance.

---

# 56. What Category Should Not Solve

Category should not own:

```text
product generation
product pricing
customer preferences
order creation
returns
payments
shipments
CSV output
global validation
global statistics
scenario orchestration
```

Its role is classification and reference taxonomy.

---

# 57. Migration Plan

When Category is migrated into the target domain architecture:

## Step 1

Inspect current Category model/reference implementation.

## Step 2

Identify current loaders and configuration.

## Step 3

Define target Category responsibilities.

## Step 4

Move the model into:

```text
category/model
```

## Step 5

Move reference/loader code into:

```text
category/reference
```

and/or:

```text
category/loader
```

only where those boundaries are actually needed.

## Step 6

Move Category-specific validation.

## Step 7

Update Product dependencies.

## Step 8

Update generation orchestration.

## Step 9

Run Category tests.

## Step 10

Run Product tests.

## Step 11

Run the complete test suite.

## Step 12

Run end-to-end generation.

## Step 13

Compare category/product statistics against the pre-migration baseline.

---

# 58. Migration Quality Gate

Category migration is complete when:

### Business

- category responsibility is explicit,
- Product relationship is explicit.

### Reference

- categories load correctly,
- IDs are unique,
- references are stable.

### Architecture

- Product consumes Category through a clear boundary,
- Category does not depend on transactional domains.

### Reproducibility

- category ordering is stable,
- same seed/reference data produces stable behavior.

### Testing

- Category tests pass,
- Product integration tests pass,
- full suite passes.

### Data

- category counts remain correct,
- product/category relationships remain valid,
- category distribution remains within expected tolerance.

---

# 59. Design Decisions

## Decision A — Category owns taxonomy

Category defines the classification system.

---

## Decision B — Product references Category

Product is the concrete catalog item.

---

## Decision C — Do not assume hierarchy

Nested categories are a future capability, not an automatic requirement.

---

## Decision D — Separate category popularity from product popularity

They represent different levels of business behavior.

---

## Decision E — Customer affinity is external behavior

Customers may prefer categories, but Category does not own that preference.

---

## Decision F — Scenario behavior stays outside Category

Scenarios modify selection/distribution behavior through explicit mechanisms.

---

## Decision G — Avoid premature abstractions

Use Strategy only when alternative category-selection behavior is genuinely required.

---

# 60. Final Mental Model

Category should be understood as:

```text
                    CATEGORY
                       │
          ┌────────────┼────────────┐
          │            │            │
       Products     Demand       Analytics
          │         grouping        │
          ▼            ▼            ▼
      Catalog      Behavior      Aggregation
```

The most important future behavioral flow is:

```text
Customer Behavior
       ↓
Category Affinity
       ↓
Category Selection
       ↓
Brand Selection
       ↓
Product Popularity
       ↓
Product Selection
       ↓
Order Item
```

Category provides the taxonomy through which this behavior becomes coherent.

---

# 61. Summary

The Category domain is a small but important foundational catalog domain.

Its primary responsibility is:

> **Define and expose the product taxonomy used by the rest of the e-commerce system.**

The current baseline contains approximately ten categories.

The immediate architecture should remain simple:

```text
category/
├── model/
├── reference/
└── validation/
```

with additional layers introduced only when justified.

The most important future role of Category is not merely classification.

It becomes an intermediate layer in realistic demand generation:

```text
Customer behavior
       ↓
Category affinity
       ↓
Category demand
       ↓
Brand demand
       ↓
Product popularity
       ↓
Order behavior
```

This makes Category a key bridge between the catalog/reference side of ShopSphere and the behavioral realism engine.

The domain should therefore remain:

- authoritative,
- stable,
- deterministic,
- easy to query,
- independent of transactions,
- independent of customer behavior,
- free of output concerns,
- free of scenario-specific branching.

The next catalog domain is **Brand**, which will build on Category and introduce an important many-to-many-style business relationship at the Product-selection level: brands can participate across categories, while their popularity and customer affinity can vary by category.
