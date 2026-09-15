# 08 — Product Domain

## 1. Purpose

This document defines the business meaning, model, catalog relationships, generation strategy, pricing responsibilities, popularity model, configuration boundaries, validation, behavioral role, realism roadmap, technical architecture, and migration plan for the **Product** domain in the ShopSphere e-commerce data generator.

Product is the central catalog domain.

It is where several foundational dimensions converge:

```text
Category
   +
Brand
   +
Product attributes
   +
Pricing
   +
Popularity
   +
Availability
```

As realism matures, Product also becomes a major consumer of:

```text
Customer behavior
+
time
+
promotion
+
scenario
```

The central architectural principle is:

> **Product represents a concrete sellable catalog item; Category and Brand classify it, while demand and purchasing behavior determine how frequently it appears in transactions.**

---

# 2. Business Meaning

A Product is a concrete item that ShopSphere offers for purchase.

A Product answers:

> "What specific thing can a customer purchase?"

This is different from:

```text
Category → what kind of product?
Brand    → under which commercial identity?
Product  → which concrete catalog item?
```

For example:

```text
Category
  Electronics

Brand
  Brand A

Product
  Brand A Wireless Noise Cancelling Headphones
```

The Product is the transactional catalog object that eventually appears in Order Items.

---

# 3. Why Product Is a Central Domain

Product connects the catalog side of ShopSphere with transaction generation.

The core relationship is:

```text
Category
    ↓
Brand
    ↓
Product
    ↓
OrderItem
    ↓
Order
```

But realistic product selection eventually becomes:

```text
Customer Behavior
        ↓
Category affinity
        ↓
Brand affinity
        ↓
Product popularity
        ↓
Product selection
        ↓
OrderItem
```

Product is therefore one of the most important domains for sophisticated realism.

---

# 4. Current Product State

The baseline currently generates approximately:

```text
500 products
```

The current system also has approximately:

```text
10 categories
26 brands
```

The exact current Product fields should remain aligned with the existing implementation until the domain migration is performed.

The Product design should therefore distinguish:

```text
current model
```

from:

```text
future richer product model
```

and avoid silently introducing fields that do not exist today.

---

# 5. Product Ownership

## Product owns

- product identity,
- product catalog attributes,
- Category relationship,
- Brand relationship,
- product-level pricing information where modeled,
- product-level availability where modeled,
- product-level catalog metadata,
- product validation.

## Category owns

- taxonomy.

## Brand owns

- commercial brand identity.

## OrderItem owns

- purchased quantity,
- transactional price snapshot,
- product reference,
- line-level transaction information.

This distinction is important.

A Product's current price is not necessarily identical to the price paid in every historical Order Item.

---

# 6. Product Identity

Every Product requires a stable unique identifier.

Conceptually:

```text
PRODUCT_000000001
```

The Product ID should:

- be unique,
- be deterministic under the same generation context,
- remain stable during the generated dataset,
- be usable as an OrderItem foreign key.

---

# 7. Product ↔ Category

Each Product currently belongs to a Category.

Relationship:

```text
Category 1
   ↓
Product N
```

A Product should not contain an arbitrary category name independently generated from the Category reference data.

It should reference the authoritative Category.

---

# 8. Product ↔ Brand

Each Product currently belongs to a Brand.

Relationship:

```text
Brand 1
   ↓
Product N
```

The Brand reference should be authoritative.

The Product generator should not generate arbitrary brand names.

---

# 9. Product ↔ Category ↔ Brand

The core catalog relationship is:

```text
Category
    │
    ├───────── Product ───────── Brand
    │
    └── defines product taxonomy
```

The combination:

```text
Category + Brand
```

can eventually constrain which Products are valid.

Not every brand must necessarily participate in every category.

---

# 10. Product as a Concrete Catalog Item

A Product should be more specific than a Category or Brand.

For example:

```text
Category:
  Electronics

Brand:
  Brand A

Product:
  Brand A 55-inch 4K Smart Television
```

This distinction is important because downstream transactions reference Products rather than broad catalog dimensions.

---

# 11. Product Cardinality

Current baseline:

```text
500 Products
```

The product count should be configurable through the existing generation-plan/configuration architecture.

Possible profiles:

```text
small
medium
large
xlarge
```

The important requirement is:

> Product cardinality must scale independently from transaction cardinality.

A dataset may have:

```text
500 products
5,000 orders
```

or:

```text
100,000 products
5,000,000 orders
```

depending on the scenario.

---

# 12. Product Catalog Size vs Transaction Volume

These are separate business dimensions.

For example:

```text
Catalog size = 100,000 products
Transaction volume = 10,000,000 order items
```

does not imply every Product is equally popular.

In a realistic catalog:

```text
few products → very high demand
many products → low demand
```

This is one of the most important Product-domain realism requirements.

---

# 13. Product Popularity

Product popularity determines how frequently a Product appears in purchase-related activity.

A uniform model:

```text
P(Product A) = P(Product B)
```

is usually a weak generic e-commerce model.

A better model is:

```text
Product popularity
   ↓
long-tail demand
```

where a small number of Products account for a large share of demand.

---

# 14. Long-Tail Demand

A target Product demand hierarchy is:

```text
Global demand
    ↓
Category demand
    ↓
Brand demand
    ↓
Product demand
```

This allows realistic concentration.

Conceptually:

```text
Category A
  ├── Brand X
  │     ├── Product 1  ← very hot
  │     ├── Product 2
  │     ├── Product 3
  │     └── many low-volume products
  │
  └── Brand Y
        └── ...
```

The long tail should be measurable rather than merely claimed.

---

# 15. Product Popularity Is Not Product Quality

Popularity should not automatically mean:

```text
higher quality
```

A Product can be popular because of:

- price,
- promotion,
- brand,
- customer preference,
- seasonality,
- availability,
- exposure.

Therefore Product popularity should be treated as a demand property rather than a universal quality score.

---

# 16. Product Demand and Customer Behavior

Future product selection should depend on CustomerBehaviorProfile.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
category affinity
        ↓
brand affinity
        ↓
product affinity
        ↓
product popularity
        ↓
selected Product
```

The generator should balance:

```text
global popularity
+
customer preference
```

so that individual customers differ while the global dataset still exhibits the intended long tail.

---

# 17. Product Affinity

A future CustomerBehaviorProfile may include product-level preference, but storing a huge explicit preference vector for every customer may be unnecessarily expensive.

A more scalable approach may be:

```text
customer category affinity
+
customer brand affinity
+
global product popularity
+
random variation
```

with occasional product-level preference where justified.

This reduces state while preserving meaningful correlation.

---

# 18. Product Selection Formula

A conceptual future selection model could be:

```text
product score
=
global popularity
×
category affinity
×
brand affinity
×
price sensitivity
×
availability
×
temporal factor
×
promotion factor
×
scenario factor
```

This is a conceptual model.

The implementation should compose separate strategies rather than place every factor into one giant method.

---

# 19. Product Availability

A Product may be:

```text
available
unavailable
discontinued
```

A future lifecycle could be:

```text
introduced
    ↓
growing
    ↓
mature
    ↓
declining
    ↓
discontinued
```

The current generator does not need the full lifecycle.

Availability becomes important when temporal realism is introduced.

---

# 20. Product Lifecycle

Product lifecycle can influence demand.

For example:

```text
New Product
    ↓
low initial demand
    ↓
growth
    ↓
peak popularity
    ↓
decline
```

A discontinued Product should eventually stop receiving new orders.

This is a future requirement.

---

# 21. Product Pricing

Pricing is one of the largest current realism gaps.

The current baseline produces an order-value distribution that is too high for a broad generic e-commerce dataset.

Observed baseline:

```text
Average order value ≈ ₹226,377
Median order value ≈ ₹144,080
Maximum ≈ ₹1,753,065
```

This indicates that Product pricing and/or order construction needs calibration.

---

# 22. Pricing Ownership

Product should own the concept of a Product's catalog price.

However, the final transaction price may depend on:

```text
base price
+
promotion
+
discount
+
customer-specific effects
+
quantity
+
time
```

Therefore:

```text
Product price
```

and:

```text
OrderItem transaction price
```

should be distinct concepts.

---

# 23. Recommended Pricing Architecture

A future Product pricing model can be composed from:

```text
Base Price Model
        ↓
Category Modifier
        ↓
Brand Modifier
        ↓
Product Tier Modifier
        ↓
Temporal Modifier
        ↓
Promotion Modifier
        ↓
Customer/Order transaction effects
```

Not all layers need to exist in the first implementation.

The architecture should evolve incrementally.

---

# 24. Base Price Distribution

Product prices should not all be generated from a narrow uniform range.

Potential models include:

```text
log-normal
gamma
mixture distributions
category-specific distributions
```

The correct choice depends on the intended business model.

The key requirement is:

> Product prices should form a plausible distribution with meaningful heterogeneity.

---

# 25. Category-Specific Pricing

Different categories should have different price scales.

Conceptually:

```text
Category
   ↓
price distribution
```

For example:

```text
Accessories → lower median
Electronics → higher median
Luxury      → high median
```

The exact categories and values are configuration/reference-data decisions.

---

# 26. Brand Pricing Effects

Brand can influence price positioning.

Conceptually:

```text
Category
    ↓
Brand tier
    ↓
Product price distribution
```

A premium brand may have a higher price distribution than a budget brand in the same category.

This should be probabilistic, not deterministic.

---

# 27. Product Tier

A future Product model may include a pricing tier:

```text
budget
mid
premium
luxury
```

The tier can influence:

- price,
- popularity,
- customer segment compatibility,
- return behavior.

This should only be added when it improves the business model enough to justify another attribute.

---

# 28. Discounts

Discounts should not be permanently embedded into Product's base price.

A clean model is:

```text
Product
  basePrice

OrderItem
  transactionPrice
```

with a pricing/promotion engine deciding the final paid price.

This enables historical transactions to differ from current catalog pricing.

---

# 29. Product and Order Value

Order value should emerge from:

```text
selected products
+
quantities
+
transaction prices
```

rather than being independently generated.

This is a crucial realism rule.

The generator should not generate:

```text
Order total = random ₹150,000
```

and separately generate cheap Products.

Instead:

```text
Products
   ↓
quantities
   ↓
prices
   ↓
OrderItem values
   ↓
Order total
```

must be internally coherent.

---

# 30. Product Quantity Interaction

Product selection and quantity are related.

A future model may allow:

```text
cheap/high-frequency products
    → larger quantities

expensive products
    → lower quantities
```

This is not a universal rule, but it is a useful possible behavioral correlation.

---

# 31. Product and Category Affinity

Customer category affinity should influence the eligible Product set.

Conceptually:

```text
Customer
   ↓
Category preference
   ↓
Category
   ↓
Product candidates
```

The Product domain should provide valid catalog choices.

Customer behavior decides preference.

---

# 32. Product and Brand Affinity

Within the selected category:

```text
Customer brand affinity
        ↓
Brand
        ↓
Product
```

This allows a customer to behave consistently.

For example:

```text
Customer prefers Electronics
Customer strongly prefers Brand A
Customer buys several Brand A electronics
```

without making every purchase deterministic.

---

# 33. Product and Promotions

Promotion can alter Product selection.

Conceptually:

```text
Product
   ↓
promotion
   ↓
temporary popularity increase
```

This creates realistic campaign behavior.

A promotional Product may experience:

```text
more views
more carts
more purchases
```

and possibly different order quantities.

Promotion logic belongs to a scenario/marketing layer rather than being hard-coded inside Product.

---

# 34. Product and Seasonality

Product demand may vary with time.

Conceptually:

```text
Product
   ↓
seasonality profile
   ↓
time-dependent demand
```

For example:

```text
seasonal product
    ↓
demand peak
    ↓
normal demand
    ↓
off-season decline
```

This is future temporal behavior.

---

# 35. Product and Events

Events should eventually reflect Product interest.

Example funnel:

```text
session
   ↓
product_view
   ↓
add_to_cart
   ↓
checkout
   ↓
purchase
```

Products with higher popularity may naturally generate more views and purchases.

The Event generator should not independently invent product demand.

---

# 36. Product and Returns

Product characteristics can influence return probability.

Potential factors:

```text
product category
product price
product type
brand
customer propensity
```

The Return domain should own the return decision.

Product can provide relevant attributes.

---

# 37. Product and Customer Spending

Customer spending behavior should interact with Product pricing.

For example:

```text
high-spending customer
    ↓
greater probability of premium products

price-sensitive customer
    ↓
greater probability of lower-priced products
```

This is a Customer behavior concern consuming Product price information.

---

# 38. Product Selection Architecture

A future readable generation flow could be:

```scala
val category =
  categorySelector.select(customerProfile, context)

val brand =
  brandSelector.select(
    category,
    customerProfile,
    context
  )

val product =
  productSelector.select(
    category,
    brand,
    customerProfile,
    context
  )
```

This communicates business intent.

---

# 39. Strategy Pattern

Product is one of the strongest candidates for Strategy because it contains multiple legitimate behavioral alternatives.

Potential strategies:

```text
ProductPopularityModel
PriceModel
ProductSelectionModel
AvailabilityModel
```

For example:

```text
ProductPopularityModel
    ├── UniformPopularity
    ├── LongTailPopularity
    └── ScenarioDrivenPopularity
```

This is justified because the behavior is expected to vary across generation profiles and scenarios.

---

# 40. Factory Pattern

Factories can be useful for selecting configured strategies.

Conceptually:

```text
Product configuration
        ↓
ProductStrategyFactory
        ↓
PopularityModel
PriceModel
SelectionModel
```

The factory should select behavior.

It should not exist merely to wrap:

```scala
Product(...)
```

---

# 41. Builder Pattern

Builder is not currently required for Product.

If the Product model remains a normal immutable case class, named constructor arguments are clearer.

Builder becomes worth considering only if Product construction eventually contains substantial optional configuration and validation.

---

# 42. Composition Over Inheritance

Avoid:

```text
ElectronicsProduct
PremiumProduct
DiscountedProduct
SeasonalProduct
HotProduct
```

as a class hierarchy.

Prefer:

```text
Product
+
PricingModel
+
PopularityModel
+
AvailabilityModel
```

This keeps business behavior composable.

---

# 43. Product Model vs Product Generator

Keep the Product model data-oriented.

The generator owns:

```text
how a Product is generated
```

The model owns:

```text
what a Product is
```

Do not put generation randomness inside the Product case class.

---

# 44. Product Reference Data

If products are generated synthetically, Category and Brand should still be reference inputs.

If some products are loaded from catalog reference data, the loader should hide the source format.

Possible architecture:

```text
Category Reference
        ↓
Brand Reference
        ↓
Product Generator
        ↓
Generated Product Catalog
```

---

# 45. Product Naming

Product names should be generated or selected coherently from:

```text
Brand
+
Category
+
product characteristics
```

A future product-name strategy might produce:

```text
Brand A 55-inch 4K Smart TV
```

rather than random unrelated text.

The current model should only be expanded if names are part of the existing Product schema.

---

# 46. Product Attributes

Potential future attributes include:

```text
model
variant
size
color
weight
rating
price tier
```

These should be added only when downstream business behavior uses them.

A field without behavioral or analytical purpose increases complexity without improving the generator.

---

# 47. Product Variants

A future catalog may distinguish:

```text
Product
   ↓
Variant
```

For example:

```text
T-shirt
  ├── Small / Black
  ├── Medium / Black
  ├── Large / Black
  └── Medium / Blue
```

This is not currently required.

If introduced, the relationship should be explicitly designed rather than hidden inside Product attributes.

---

# 48. Product Catalog Integrity

Every generated Product should satisfy:

```text
unique Product ID
valid Category ID
valid Brand ID
valid price
valid required attributes
```

If Brand-Category compatibility is modeled:

```text
Product.brandId must be valid for Product.categoryId
```

---

# 49. Product Validation

Product-specific validation should include:

## Identity

```text
ID non-empty
ID unique
```

## Category

```text
category exists
```

## Brand

```text
brand exists
```

## Price

```text
price >= 0
```

or a stricter positive constraint if free Products are not supported.

## Business compatibility

```text
brand/category combination valid
```

when such a rule is active.

---

# 50. Statistical Validation

Product statistics should include:

```text
products/category
products/brand
products/category/brand
price distribution
price percentiles
product popularity
product demand concentration
```

Future transaction statistics should include:

```text
order items/product
orders/product
revenue/product
```

---

# 51. Product Popularity Validation

If a long-tail model is active, validate:

```text
top 1% product share
top 5% product share
top 10% product share
median demand
p95 demand
p99 demand
```

Potential concentration metrics:

```text
Gini coefficient
Herfindahl-style concentration
entropy
```

The exact metrics should be chosen according to the Spark experiments they support.

---

# 52. Product Price Validation

The pricing model should be validated using:

```text
min
p25
median
p75
p95
p99
max
mean
```

and possibly:

```text
standard deviation
coefficient of variation
```

The objective is to prevent accidental distributions such as the current excessively high broad-market AOV.

---

# 53. Product Skew Scenarios

Product is one of the strongest domains for controlled skew.

Examples:

## Hot products

A small number of products receive most demand.

## Category skew

A few categories dominate Product and transaction volume.

## Brand skew

A few brands dominate demand.

## Product catalog imbalance

A few categories contain most Products.

## Campaign spike

Selected Products receive temporary demand amplification.

---

# 54. Product Data-Quality Scenarios

Possible defects include:

```text
duplicate Product ID
unknown Category ID
unknown Brand ID
invalid price
missing required attribute
invalid Brand/Category combination
```

The clean Product generator should not intentionally produce these.

The Quality Engine should inject them explicitly.

---

# 55. Output Responsibility

Product should not write files.

The output layer serializes Product records.

Current baseline output includes:

```text
products.csv
```

The output contract should remain independent of Product generation logic.

---

# 56. Statistics Responsibility

Product generation should not own global statistics.

Statistics should consume Products and transactions.

This separation enables:

```text
generation
    ↓
measurement
    ↓
comparison against expected distribution
```

without coupling generation to analytics.

---

# 57. Manifest Responsibility

The manifest should eventually include:

```text
product count
catalog version
category reference version
brand reference version
pricing model
popularity model
scenario
```

The exact fields depend on the final manifest contract.

---

# 58. Configuration Responsibility

Product behavior should consume typed configuration.

Preferred:

```text
HOCON
  ↓
ConfigLoader
  ↓
Product configuration
  ↓
Product strategies
```

Do not make Product strategies parse HOCON directly.

---

# 59. Product Configuration

Future configuration may contain:

```hocon
product {
  count = 500

  popularity {
    model = "long-tail"
  }

  pricing {
    model = "category-aware"
  }

  availability {
    model = "active"
  }
}
```

The exact schema should be finalized only after the Product architecture is designed against current configuration.

---

# 60. Dependency Direction

Preferred:

```text
Product
   ↓
Category
Brand
common distribution/random infrastructure
```

Behavioral models may consume:

```text
CustomerBehaviorProfile
```

but Product should not depend on Customer as a complete domain object unless necessary.

---

# 61. Avoid Circular Dependencies

Avoid:

```text
Product → Customer → Product
```

at the code level.

Customer behavior can depend on Product metadata or reference data through a narrow boundary.

Product should not contain customer collections.

---

# 62. Customer Behavior Boundary

A good future boundary is:

```text
CustomerBehaviorProfile
```

rather than:

```scala
Customer
```

when Product selection only needs behavioral information.

For example:

```scala
productSelector.select(
  profile = customerProfile,
  category = category,
  brand = brand,
  context = context
)
```

This reduces coupling.

---

# 63. Product and Randomness

Product generation should use deterministic random streams.

Different decisions should have explicit random scopes.

Conceptually:

```text
product identity
product category
product brand
product price
product attributes
```

should not all consume one opaque sequence if independent reproducibility matters.

Derived random streams can make behavior easier to reason about.

---

# 64. Stable Catalog Ordering

Product selection must use stable Product ordering.

For example:

```text
sort by Product ID
```

before applying a deterministic popularity model.

This prevents changes in collection iteration order from silently changing generated output.

---

# 65. Product Generation Context

A future Product generation context may contain:

```text
seed/random
configuration
categories
brands
time model
scenario
distribution models
```

Only dependencies required by Product behavior should be exposed.

Avoid passing a giant mutable global context into every method.

---

# 66. Large Catalog Considerations

For very large catalogs:

```text
100K
1M
10M products
```

the generator should avoid repeatedly scanning the entire Product set.

Popularity selection may require:

```text
precomputed weights
alias tables
cumulative distributions
indexed candidate groups
```

depending on the chosen algorithm.

This is an optimization concern after the statistical model is correct.

---

# 67. Product Selection Performance

Potential selection pipeline:

```text
Customer
   ↓
Category candidates
   ↓
Brand candidates
   ↓
Product candidates
   ↓
weighted selection
```

The implementation should avoid rebuilding candidate distributions for every OrderItem.

Precompute stable structures where possible.

---

# 68. Product and Memory

The Product catalog may be large relative to reference data.

The generator should distinguish:

```text
catalog metadata
```

from:

```text
transactional product-selection state
```

Avoid storing unnecessary per-customer product preference vectors if a compact affinity model provides equivalent realism.

---

# 69. Product and Parallel Generation

Future parallel generation should be possible without shared mutable Product state.

Preferred model:

```text
immutable catalog
+
independent random streams
+
independent generated records
```

This supports deterministic parallel generation more cleanly.

---

# 70. Product and Reproducibility

A reproducible Product dataset requires:

```text
same generator version
+
same configuration
+
same seed
+
same Category reference
+
same Brand reference
+
same scenario
```

If pricing/popularity models change, the generator version or model version should be reflected in the manifest.

---

# 71. Product and Temporal Reproducibility

When time-dependent models are introduced, the generation must also have a deterministic time reference.

For example:

```text
generation start date
+
time model version
```

should become part of the generation context.

---

# 72. Product and Business Scenarios

Product should respond to scenarios through behavior models.

Examples:

```text
baseline
hot-products
category-spike
brand-promotion
seasonal-peak
premium-customer
```

The scenario engine should select or modify strategies.

Avoid:

```scala
if (scenario == "hot-products") ...
```

spread throughout Product code.

---

# 73. Product and Data Quality

The Product domain should always have a valid baseline representation.

Quality scenarios should operate after generation:

```text
Product generation
      ↓
valid catalog
      ↓
Quality injection
      ↓
controlled dirty catalog
```

This makes clean and dirty scenarios reproducible from the same source model.

---

# 74. Product and Spark Performance Laboratory

Product is central to downstream Spark experiments.

Possible workloads include:

## Product aggregations

```text
sales by product
revenue by product
returns by product
```

## Product joins

```text
OrderItem
JOIN Product
JOIN Category
JOIN Brand
```

## Hot-key skew

```text
few Products
→ most OrderItems
```

## Catalog-size experiments

```text
small catalog
large catalog
very large catalog
```

## Join strategy experiments

Product dimensions can be small enough for broadcast-style experiments or large enough to force different strategies.

---

# 75. Product as a Performance Control Knob

The generator should eventually allow independent controls for:

```text
product count
order count
order item count
product popularity skew
category skew
brand skew
```

This allows Spark experiments to vary:

```text
data volume
+
cardinality
+
skew
```

independently.

That is a major reason Product architecture matters to the overall project.

---

# 76. Current Simplifications

The current Product implementation intentionally simplifies:

- product behavior,
- product popularity,
- customer-specific preference,
- lifecycle,
- seasonality,
- promotion,
- advanced pricing,
- product variants,
- geographic demand,
- temporal availability.

These are future capabilities.

---

# 77. Current Major Realism Gap

The current Product/pricing behavior contributes to an unrealistically high order-value distribution.

Therefore Product should be treated as a priority domain for realism improvement.

The immediate target is not maximum complexity.

The immediate target is:

```text
plausible product prices
+
plausible product popularity
+
coherent order-item pricing
```

before adding advanced scenarios.

---

# 78. Recommended Product Realism Sequence

Implement in this order:

```text
1. Validate current Product model
2. Calibrate base price distribution
3. Introduce category-aware pricing
4. Introduce long-tail product popularity
5. Introduce category-conditioned product demand
6. Introduce brand-conditioned demand
7. Integrate CustomerBehaviorProfile
8. Introduce promotion effects
9. Introduce temporal demand
10. Introduce lifecycle
11. Introduce advanced scenarios
```

This sequence reduces complexity while improving realism progressively.

---

# 79. Product Domain Migration

When the current Product implementation is migrated:

## Step 1

Inspect current Product model.

## Step 2

Inspect current Product generator.

## Step 3

Inspect current pricing logic.

## Step 4

Inspect current Product distribution logic.

## Step 5

Inspect Category and Brand dependencies.

## Step 6

Define target responsibilities.

## Step 7

Move Product model into:

```text
product/model
```

## Step 8

Move generation logic into:

```text
product/generator
```

or a more precise selection/pricing structure if justified.

## Step 9

Introduce behavior strategies only where current requirements justify them.

## Step 10

Move Product-specific validation.

## Step 11

Update OrderItem dependencies.

## Step 12

Run Product tests.

## Step 13

Run OrderItem tests.

## Step 14

Run complete suite.

## Step 15

Run end-to-end generation.

## Step 16

Compare Product and order-value statistics with baseline.

---

# 80. Product Migration Quality Gate

Product migration is complete when:

### Business

- Product meaning is explicit.
- Category and Brand relationships are explicit.
- pricing ownership is explicit.
- demand behavior is explicit.

### Architecture

- Product is not coupled to output.
- Product is not coupled to global statistics.
- Product does not own Customer state.
- behavior abstractions are justified.

### Data

- IDs remain valid,
- category references remain valid,
- brand references remain valid,
- prices remain valid.

### Reproducibility

- same seed/config/reference data produces stable catalog output.

### Testing

- Product tests pass,
- Category integration passes,
- Brand integration passes,
- OrderItem integration passes,
- full suite passes.

### Statistics

- product count remains correct,
- category/brand distribution remains within expected tolerance,
- price distribution is measured,
- popularity distribution is measured.

---

# 81. Design Decisions

## Decision A — Product is the concrete catalog item

Category and Brand classify Product.

---

## Decision B — Product demand is separate from Product identity

A Product exists independently of how often it sells.

---

## Decision C — Product popularity should eventually be long-tail

Uniform demand is a weak default for sophisticated realism.

---

## Decision D — Category and Brand condition Product behavior

Future demand should support:

```text
P(Product | Category, Brand)
```

rather than only a global Product distribution.

---

## Decision E — Customer behavior influences Product selection

The CustomerBehaviorProfile should influence selection without coupling Product to the entire Customer object.

---

## Decision F — Product price and transaction price are distinct

The catalog price is not necessarily the historical paid price.

---

## Decision G — Order totals emerge from Order Items

Do not generate independent random order totals.

---

## Decision H — Pricing is composable

Future pricing should combine meaningful factors rather than become one giant pricing method.

---

## Decision I — Product scenarios stay outside core Product identity

Scenario effects belong in behavioral/scenario infrastructure.

---

## Decision J — Avoid premature Product variants

Variant modeling should be introduced only when downstream requirements justify it.

---

# 82. Final Mental Model

Product sits at the center of the catalog:

```text
             Category
                 │
                 ▼
              Product
                 ▲
                 │
               Brand
```

Demand then flows through:

```text
Customer Behavior
        ↓
Category affinity
        ↓
Brand affinity
        ↓
Product popularity
        ↓
Product selection
        ↓
Order Item
        ↓
Order
```

Pricing flows through:

```text
Product
  ↓
Base Price
  ↓
Category/Brand/Tier
  ↓
Promotion/Temporal effects
  ↓
Transaction Price
  ↓
OrderItem
```

The complete future model becomes:

```text
Customer
   │
   ▼
Behavior Profile
   │
   ├── category preference
   ├── brand preference
   ├── spending behavior
   └── price sensitivity
            │
            ▼
        Product Selection
            │
            ├── Category
            ├── Brand
            ├── Popularity
            ├── Price
            └── Availability
                    │
                    ▼
                OrderItem
                    │
                    ▼
                  Order
```

---

# 83. Summary

Product is the most important catalog domain because it connects:

```text
Category
Brand
Pricing
Popularity
Customer behavior
Transactions
```

The current baseline contains approximately 500 Products, 10 Categories, and 26 Brands.

The most important current Product problem is not architectural complexity.

It is realism.

The current monetary distribution produces an excessively high broad-market order value, so Product pricing requires calibration.

The next major realism improvements should be:

```text
plausible price distribution
        ↓
long-tail product popularity
        ↓
category-conditioned demand
        ↓
brand-conditioned demand
        ↓
customer-specific preference
        ↓
temporal/promotion effects
        ↓
product lifecycle
```

Architecturally, Product is an excellent place to apply Strategy and composition because pricing, popularity, selection, and availability are genuine areas of behavioral variation.

However, the Product model itself should remain simple and data-oriented.

The target is:

> **A concrete, coherent, configurable product catalog whose prices and demand emerge from business-level rules rather than arbitrary independent randomness.**

The next domain is **Order**, where selected Products become actual customer transactions and where customer behavior, product selection, order-size distributions, pricing, status lifecycle, and temporal behavior begin to converge.
