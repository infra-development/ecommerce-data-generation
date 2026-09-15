# 10 — OrderItem Domain

## 1. Purpose

This document defines the business meaning, data model, relationships, generation strategy, pricing boundary, quantity behavior, product-selection behavior, validation, realism, configuration, architecture, testing, and migration plan for the **OrderItem** domain in the ShopSphere e-commerce data generator.

OrderItem is one of the most important domains for realistic e-commerce data because it is the bridge between:

```text
Order
  ↓
OrderItem
  ↓
Product
```

It converts a customer transaction into concrete purchased products.

OrderItem is where several important distributions meet:

- basket size,
- product popularity,
- category affinity,
- brand affinity,
- quantity,
- price,
- customer spending behavior,
- promotions,
- order value.

The central principle is:

> **OrderItem represents a purchased product line inside an Order; it should emerge from customer behavior, product demand, basket composition, quantity, and transaction pricing rather than being a random join between Orders and Products.**

---

# 2. Business Meaning

An OrderItem represents one purchased product line within an Order.

For example:

```text
Order: ORDER_000001

OrderItem 1
  Product = PROD_123
  Quantity = 2

OrderItem 2
  Product = PROD_456
  Quantity = 1
```

The Order answers:

> "What transaction occurred?"

The OrderItem answers:

> "What product was purchased as part of that transaction?"

This distinction is fundamental.

---

# 3. Why OrderItem Is Critical

OrderItem is the transactional bridge between the catalog and the customer.

Without OrderItems, the generator can create:

```text
Customers
Orders
Products
```

but cannot realistically represent product demand.

OrderItem determines:

```text
which products sell
how often they sell
how many units are purchased
what categories are purchased
what brands are purchased
how order value is formed
```

Therefore OrderItem is central to both business realism and Spark workloads.

---

# 4. Domain Ownership

## OrderItem owns

- Order relationship,
- Product relationship,
- quantity,
- line-level transaction information,
- line-level monetary value where modeled.

## Order owns

- overall transaction,
- customer,
- order lifecycle,
- order-level context.

## Product owns

- catalog identity,
- category,
- brand,
- catalog price,
- product characteristics.

## Pricing behavior owns

- transaction price,
- discount logic,
- promotion effects,
- customer-specific pricing effects where applicable.

The boundaries should remain explicit.

---

# 5. Current Baseline

The current baseline produces approximately:

```text
12,000 OrderItems
5,000 Orders
```

Therefore:

```text
average items/order = 2.40
```

Current observed statistics are approximately:

```text
min     = 2
p25     = 2
median  = 2
p75     = 3
p95     = 3
p99     = 3
max     = 3
```

This means the current implementation effectively constrains baskets to a very narrow range.

That is useful as a deterministic baseline, but it is not sufficiently realistic for the target generator.

---

# 6. Target OrderItem Model

The target conceptual model is:

```text
OrderItem
 ├── id
 ├── orderId
 ├── productId
 ├── quantity
 └── monetary transaction fields
```

The exact fields must follow the current code model.

Do not add fields merely because a production e-commerce schema might contain them.

---

# 7. Current Model vs Future Model

The current implementation should remain the source of truth for existing attributes.

Future concepts may include:

```text
unitPrice
discount
lineTotal
```

but these should only be added after the pricing model is explicitly designed.

The architectural principle is:

> **Do not silently expand the data model while reorganizing the generator.**

Migration and domain enrichment are separate activities.

---

# 8. Relationship to Order

The fundamental relationship is:

```text
Order
  │
  │ 1
  │
  └──────────< OrderItem
```

Every valid Order should normally have one or more OrderItems.

This gives:

```text
Order 1 → N OrderItems
```

---

# 9. Relationship to Product

The second fundamental relationship is:

```text
Product
  │
  │ 1
  │
  └──────────< OrderItem
```

A product may appear in many OrderItems.

Therefore:

```text
Order
  ↓
OrderItem
  ↓
Product
```

creates the transaction-to-catalog relationship.

---

# 10. Product Popularity

Product demand should not normally be uniform.

A realistic e-commerce catalog often contains:

```text
few highly popular products
many moderately popular products
large number of low-volume products
```

Therefore Product selection should eventually follow a long-tail distribution.

Conceptually:

```text
Product popularity
        ↓
weighted selection
        ↓
OrderItem
```

---

# 11. Why Uniform Product Selection Is Weak

Suppose:

```text
10,000 products
```

and every product has equal probability.

Then approximately:

```text
each product ≈ same demand
```

This does not resemble a typical marketplace/catalog.

Instead, the desired shape is closer to:

```text
Product A  █████████████
Product B  █████████
Product C  ██████
Product D  ████
...
Product N  ▏
```

The exact mathematical distribution is configurable.

---

# 12. Product Popularity vs Catalog Cardinality

These are separate dimensions.

For example:

```text
products = 1,000,000
```

does not imply:

```text
each product receives equal orders
```

A large catalog can have extreme demand concentration.

This distinction is important for both realism and Spark skew experiments.

---

# 13. Category Conditioning

Product selection should eventually respect category behavior.

Conceptually:

```text
Customer
   ↓
Category affinity
   ↓
Category selection
   ↓
Product selection
```

This produces realistic category-level demand.

For example, a customer with strong electronics affinity should not have the same category distribution as every other customer.

---

# 14. Brand Conditioning

Brand behavior can similarly influence selection.

Conceptually:

```text
Customer
   ↓
Category preference
   ↓
Brand preference
   ↓
Product
```

The exact order of conditioning depends on the final Product/Brand model.

The important point is that product selection should preserve meaningful dependencies.

---

# 15. Customer Preference

OrderItem is where Customer preferences become observable transaction data.

A customer's latent profile can contain:

```text
category affinity
brand affinity
price sensitivity
spending level
product popularity preference
```

These should influence Product selection.

---

# 16. Customer Behavior Flow

A mature selection flow can look like:

```text
CustomerBehaviorProfile
        │
        ├── category affinity
        ├── brand affinity
        ├── price sensitivity
        └── spending level
                │
                ▼
          Category selection
                │
                ▼
            Brand selection
                │
                ▼
           Product selection
                │
                ▼
            OrderItem
```

This is one of the most important realism improvements in the project.

---

# 17. Basket Composition

OrderItem generation should produce a basket, not isolated products.

The conceptual flow is:

```text
Order
  ↓
Determine basket size
  ↓
Select product #1
  ↓
Select product #2
  ↓
Select product #3
  ↓
...
```

Each selection can depend on:

- customer preferences,
- previous basket items,
- category diversity,
- product popularity,
- promotions.

---

# 18. Basket Size

Basket size is the number of distinct OrderItems associated with an Order.

A future distribution should support:

```text
1
2
3
4
5
6+
```

rather than only:

```text
2 or 3
```

The exact distribution should be configurable.

---

# 19. Basket Size Distribution

Potential statistical models include:

```text
Poisson
geometric
negative binomial
empirical distribution
mixture distribution
```

A simple empirical distribution may be easiest to calibrate.

For example:

```text
1 item  = 30%
2 items = 30%
3 items = 20%
4 items = 10%
5 items = 6%
6+      = 4%
```

This is illustrative only, not a prescribed configuration.

---

# 20. Customer Influence on Basket Size

Basket size can be influenced by customer behavior.

Possible relationship:

```text
high activity
    → somewhat larger basket

high spending
    → potentially larger basket

low activity
    → potentially smaller basket
```

The relationship should remain probabilistic.

Do not encode:

```text
high spender = exactly 5 items
```

because this destroys variation.

---

# 21. Product Repetition

A future Order may contain:

```text
same product once with quantity > 1
```

rather than representing each physical unit as a separate OrderItem.

Therefore:

```text
OrderItem
  productId = X
  quantity = 3
```

is generally preferable to:

```text
OrderItem X
OrderItem X
OrderItem X
```

when the business representation is line-item based.

---

# 22. Quantity

Quantity is distinct from basket size.

For example:

```text
Order
  ↓
3 OrderItems

Item A quantity = 1
Item B quantity = 2
Item C quantity = 5
```

Basket size:

```text
3
```

Total physical units:

```text
8
```

Both are useful statistics.

---

# 23. Quantity Distribution

Quantity should eventually follow a realistic distribution.

A typical shape may be:

```text
quantity = 1   → very common
quantity = 2   → less common
quantity = 3   → less common
quantity = 4+  → rare
```

The exact probabilities depend on the scenario.

A negative-binomial or empirical distribution can model heavier tails if needed.

---

# 24. Quantity and Product Type

Quantity may depend on Product characteristics.

For example:

```text
consumable product
    → higher repeat quantity

expensive durable product
    → lower quantity
```

This should eventually be represented through Product characteristics or a ProductQuantityModel.

OrderItem should consume the decision.

---

# 25. Quantity and Customer Behavior

Customer behavior can also influence quantity.

For example:

```text
bulk buyer
    → higher quantity

occasional buyer
    → lower quantity
```

Again, this should be probabilistic rather than deterministic.

---

# 26. Product Price

OrderItem is the natural location where catalog price becomes transaction price.

Conceptually:

```text
Product
  ↓
catalog price
  ↓
pricing rules
  ↓
transaction unit price
  ↓
OrderItem
```

This separates catalog information from transaction economics.

---

# 27. Unit Price

If the final OrderItem model contains a unit price, it should represent the price applicable to that transaction line.

It should not automatically be assumed to equal the Product's current catalog price.

Potential factors:

```text
base product price
+
promotion
+
discount
+
campaign
+
customer-specific pricing
```

---

# 28. Line Total

A future line total should generally satisfy:

```text
lineTotal =
  quantity × transactionUnitPrice
```

subject to explicit rounding rules.

This creates an important invariant.

---

# 29. Order Total

Order total should be derived from OrderItems:

```text
Order total =
    Σ OrderItem line totals
```

Therefore:

```text
OrderItem
   ↓
line totals
   ↓
Order
   ↓
order total
```

This makes transaction economics internally coherent.

---

# 30. Current AOV Problem

The baseline currently produces approximately:

```text
Average order value ≈ ₹226,377
Median ≈ ₹144,080
Maximum ≈ ₹1.75M
```

OrderItem is directly involved in this result because:

```text
product price
× quantity
× basket size
```

determines transaction value.

However, OrderItem should not artificially reduce values to compensate for an unrealistic Product price distribution.

The pricing model must be fixed at the appropriate boundary.

---

# 31. Price Distribution

Product pricing should eventually support:

```text
low-priced products
mid-priced products
premium products
```

with a realistic long tail.

Then OrderItem selection should produce:

```text
many low/mid-value lines
fewer premium lines
```

and consequently:

```text
many ordinary orders
fewer high-value orders
```

---

# 32. Spending Behavior

Customer spending behavior should influence which products become OrderItems.

For example:

```text
price-sensitive customer
    → greater probability of lower-priced products

premium-oriented customer
    → greater probability of premium products
```

This should emerge through weighted product selection.

---

# 33. Price Sensitivity

A latent variable can represent customer price sensitivity.

Conceptually:

```text
price sensitivity
      ↓
price-band preference
      ↓
Product selection
```

Possible categories:

```text
value
balanced
premium
```

These are conceptual labels, not necessarily final persisted fields.

---

# 34. Product Popularity and Price

Popularity should not necessarily be independent of price.

A future model may allow:

```text
popular low-price products
popular mid-price products
rare premium products
```

This produces more realistic demand.

The correlation should be configurable rather than hard-coded.

---

# 35. Category and Product Affinity

A customer may prefer:

```text
Electronics
```

and within Electronics:

```text
Apple
Samsung
Sony
```

and within a preferred brand:

```text
specific product families
```

The OrderItem generation path can therefore become hierarchical.

---

# 36. Basket Correlation

Products within the same basket need not be independent.

Potential relationships include:

```text
laptop
  +
laptop bag
  +
mouse
```

or:

```text
camera
  +
memory card
```

This is a future cross-sell model.

---

# 37. Basket Complementarity

A future Product relationship model may expose:

```text
complements
substitutes
bundles
```

Then OrderItem generation can use these relationships.

For example:

```text
primary product
    ↓
complementary-product probability
    ↓
additional OrderItem
```

This is more realistic than uniform independent selection.

---

# 38. Basket Diversity

A future basket model may control:

```text
number of categories/order
number of brands/order
repeat product probability
```

This is useful because two baskets with the same number of items can look very different.

Example:

```text
Basket A
  Electronics
  Electronics
  Electronics

Basket B
  Electronics
  Home
  Beauty
```

Both contain three items, but their business meaning differs.

---

# 39. Category Concentration

Useful OrderItem statistics include:

```text
categories/order
```

and:

```text
dominant category share
```

This helps validate whether customer category preferences are actually visible.

---

# 40. Brand Concentration

Similarly:

```text
brands/order
```

and:

```text
dominant brand share
```

can measure whether brand affinity produces meaningful behavior.

---

# 41. Product Reuse

Useful statistics:

```text
OrderItems/product
```

including:

```text
mean
median
p95
p99
max
```

This directly measures product demand concentration.

---

# 42. Product Popularity Validation

A mature dataset should be able to report:

```text
top 10 products by OrderItem count
top 1% product demand share
top 5% product demand share
```

This is a key realism metric.

---

# 43. Long-Tail Validation

A useful diagnostic is:

```text
Product rank
vs
OrderItem frequency
```

The expected graph should generally decline rather than remain flat.

The exact curve depends on the selected distribution.

---

# 44. OrderItem Skew

OrderItem is naturally suitable for Spark skew scenarios.

For example:

```text
one hot product
```

could appear in a very large number of OrderItems.

This can create:

```text
product_id
    ↓
hot partition
```

during Spark aggregation or joins.

---

# 45. Customer Skew Through OrderItems

Customer skew can also propagate:

```text
hot customer
   ↓
many Orders
   ↓
many OrderItems
```

This makes OrderItem useful for studying multi-stage skew propagation.

---

# 46. Controlled Skew

Skew should be scenario-controlled.

Examples:

```text
normal
hot-products
hot-customers
hot-categories
combined-skew
```

The clean baseline should remain reasonably natural.

---

# 47. Scenario Architecture

Avoid:

```scala
if (scenario == "hot-product") {
  ...
}
```

throughout OrderItemGenerator.

Prefer:

```text
ProductPopularityModel
        ↑
Scenario-adjusted behavior
        ↑
OrderItem generation
```

This keeps scenario logic compositional.

---

# 48. Data Quality

OrderItem quality problems should be deliberate.

Potential injected defects:

```text
duplicate ID
unknown order ID
unknown product ID
zero quantity
negative quantity
invalid monetary value
inconsistent line total
```

These should be generated only when a data-quality scenario requests them.

---

# 49. Clean Baseline

The clean baseline should guarantee:

```text
OrderItem ID unique
Order ID valid
Product ID valid
quantity > 0
line price valid
line total coherent
Order total coherent
```

The quality engine may later intentionally violate selected invariants.

---

# 50. Referential Integrity

OrderItem validation must check:

```text
orderItem.orderId exists in Orders
```

and:

```text
orderItem.productId exists in Products
```

These are primary foreign-key invariants.

---

# 51. Order Coverage

The clean baseline should normally ensure:

```text
every Order
    → at least one OrderItem
```

An Order with zero items should be allowed only if the business scenario explicitly requires abandoned/unfinalized transactions.

---

# 52. Product Coverage

Not every Product necessarily needs to appear in an OrderItem.

This is important.

A realistic catalog may contain:

```text
products with high demand
products with low demand
products with zero demand during the generation window
```

Therefore do not enforce:

```text
every Product must be purchased
```

unless a specific scenario requires it.

---

# 53. Product Lifecycle

Future Product availability should influence OrderItem selection.

For example:

```text
active product
    → selectable

discontinued product
    → selectable only before discontinuation
```

This creates temporal catalog realism.

---

# 54. Order Time and Product Availability

Eventually:

```text
Order timestamp
        ↓
Product availability at that time
        ↓
valid Product selection
```

should be enforced.

This prevents historical impossibilities such as a product being purchased before it existed.

---

# 55. Promotion Effects

Promotions can modify OrderItem selection.

Conceptually:

```text
promotion period
      ↓
product demand multiplier
      ↓
selection probability
      ↓
OrderItem
```

Promotions can also modify:

```text
transaction price
```

The promotion engine should remain a separate concern.

---

# 56. Seasonality

Product demand can vary over time.

Examples:

```text
season
campaign
holiday
weekday
month
```

This can affect:

```text
category selection
product selection
basket size
quantity
price
```

The temporal model should coordinate these effects.

---

# 57. Reproducibility

OrderItem generation must remain deterministic under the same:

```text
seed
configuration
reference data
behavior models
```

Derived random streams are recommended.

For example:

```text
Order
  └── OrderItem random stream
       ├── basket size
       ├── product selection
       ├── quantity
       └── pricing
```

This helps isolate changes.

---

# 58. Randomness Isolation

Avoid using one global mutable random generator for all decisions.

Prefer deterministic derivation such as:

```text
random.derive(orderId)
```

and further:

```text
random.derive("basket-size")
random.derive("product-selection")
random.derive("quantity")
```

The exact API follows the existing RandomGenerator design.

---

# 59. Strategy Pattern

OrderItem has genuine strategy candidates.

Potential strategies:

```text
BasketSizeModel
ProductSelectionModel
QuantityModel
PricingModel
BasketCompositionModel
```

These are legitimate behavioral variation points.

---

# 60. BasketSizeModel

Example abstraction:

```text
BasketSizeModel
    ↓
number of OrderItems
```

Possible implementations:

```text
FixedBasketSizeModel
EmpiricalBasketSizeModel
DistributionBasketSizeModel
BehaviorDrivenBasketSizeModel
```

The exact set should be kept small.

---

# 61. ProductSelectionModel

Possible strategies:

```text
UniformProductSelection
WeightedProductSelection
CustomerAffinityProductSelection
LongTailProductSelection
```

A sophisticated system may compose these rather than create many subclasses.

---

# 62. QuantityModel

Possible strategies:

```text
FixedQuantityModel
WeightedQuantityModel
ProductAwareQuantityModel
CustomerAwareQuantityModel
```

Again, use only strategies that represent real variation.

---

# 63. PricingModel

Potential strategies:

```text
CatalogPriceModel
DiscountedPriceModel
PromotionAwarePriceModel
CustomerAwarePriceModel
```

Pricing should remain independent from Product identity.

---

# 64. Factory Assessment

Factories may select configured implementations.

For example:

```text
ProductSelectionModelFactory
BasketSizeModelFactory
```

Factory use is justified when configuration chooses among multiple implementations.

Do not introduce factories for simple object construction.

---

# 65. Builder Assessment

Builder is generally unnecessary for OrderItem.

A Scala case class is sufficient for simple immutable records.

Use a Builder only if construction becomes genuinely complex and staged.

---

# 66. Template Method Assessment

Template Method is not a preferred default.

Composition is generally clearer:

```text
OrderItemGenerator
  + BasketSizeModel
  + ProductSelectionModel
  + QuantityModel
  + PricingModel
```

rather than inheritance-heavy generators.

---

# 67. Dependency Injection

Prefer constructor injection.

Conceptually:

```scala
class OrderItemGenerator(
    basketSizeModel: BasketSizeModel,
    productSelectionModel: ProductSelectionModel,
    quantityModel: QuantityModel,
    pricingModel: PricingModel
)
```

No dependency-injection framework is required.

---

# 68. Interfaces and Traits

Traits should exist where there is meaningful substitution.

Good:

```text
trait ProductSelectionModel
trait BasketSizeModel
```

Potentially unnecessary:

```text
trait OrderItemRepository
trait OrderItemManager
trait OrderItemHelper
```

when the generator has no such abstraction need.

---

# 69. Composition Over Giant Generator

Avoid:

```text
OrderItemGenerator
  ├── customer behavior
  ├── category logic
  ├── brand logic
  ├── product popularity
  ├── quantity
  ├── pricing
  ├── promotions
  ├── statistics
  ├── validation
  └── CSV writing
```

Prefer:

```text
OrderItemGenerator
  ├── BasketSizeModel
  ├── ProductSelectionModel
  ├── QuantityModel
  └── PricingModel
```

with supporting domain/reference services.

---

# 70. SOLID — Single Responsibility

OrderItem generation should not own:

```text
Product catalog loading
Customer persistence
CSV output
global statistics
quality injection
```

Each belongs elsewhere.

---

# 71. SOLID — Open/Closed

New product-selection behavior should be addable without rewriting the entire generator.

---

# 72. SOLID — Liskov

Any concrete ProductSelectionModel should satisfy the same contract.

---

# 73. SOLID — Interface Segregation

Keep behavioral interfaces narrow.

---

# 74. SOLID — Dependency Inversion

The generator should depend on behavior abstractions rather than hard-coded selection algorithms where variation is required.

---

# 75. Readability

The code should communicate business intent.

Preferred conceptual flow:

```scala
val basketSize =
  basketSizeModel.determine(customerProfile, orderContext)

val products =
  productSelectionModel.select(
    basketSize,
    customerProfile,
    orderContext
  )

val items =
  products.map { product =>
    val quantity =
      quantityModel.determine(product, customerProfile)

    val price =
      pricingModel.determine(product, customerProfile, orderContext)

    createOrderItem(
      orderContext,
      product,
      quantity,
      price
    )
  }
```

The exact implementation may differ.

The important property is readability.

---

# 76. Business Language in Code

Prefer names such as:

```text
basketSize
productSelection
quantity
transactionPrice
lineTotal
customerAffinity
```

Avoid vague names such as:

```text
data
result
value
helper
processor
manager
```

when a domain-specific name is possible.

---

# 77. Validation Boundary

OrderItem-specific validation belongs close to the OrderItem domain.

Examples:

```text
quantity > 0
valid order ID
valid product ID
valid line amount
```

Cross-entity validation belongs in the global validation layer.

---

# 78. Statistical Validation

The statistics layer should measure:

```text
items/order
units/order
quantity distribution
OrderItems/product
OrderItems/category
OrderItems/brand
line value
product concentration
basket category diversity
basket brand diversity
```

These metrics reveal whether the generator behaves realistically.

---

# 79. Important Distribution Metrics

For basket size:

```text
mean
median
p75
p90
p95
p99
max
```

For quantity:

```text
mean
median
p95
max
```

For product demand:

```text
top 1%
top 5%
top 10%
```

share of total OrderItems.

---

# 80. Product Demand Concentration

A key realism question:

> What percentage of all OrderItems is generated by the top 1% of products?

This is more informative than simply checking that a weighted distribution exists.

---

# 81. Customer-to-Product Correlation

A sophisticated validation layer can test whether:

```text
customer preference
```

actually changes:

```text
product/category selection
```

For example:

```text
high-electronics-affinity customers
    → higher electronics purchase share
```

This should be measurable.

---

# 82. Basket-Level Correlation

A further validation can test:

```text
category count/order
brand count/order
repeat-category rate
repeat-brand rate
```

These reveal whether baskets are too independent or too homogeneous.

---

# 83. Monetary Validation

Measure:

```text
line value
order value
quantity
basket size
```

and verify that:

```text
order value = sum(line values)
```

within the defined precision.

---

# 84. Data Quality Scenarios

Potential OrderItem quality profiles:

```text
clean
slightly_dirty
dirty
```

A dirty profile may introduce:

```text
invalid references
invalid quantity
duplicate IDs
monetary inconsistencies
```

The severity must remain configurable.

---

# 85. Quality Injection Boundary

Do not embed quality defects inside normal generation logic.

Prefer:

```text
Clean OrderItem
      ↓
Quality Engine
      ↓
Scenario-specific corruption
```

This keeps normal business generation clean.

---

# 86. Output Boundary

OrderItem should not know about CSV.

The output layer handles:

```text
order_items.csv
```

and serialization.

This keeps domain logic independent of storage.

---

# 87. Manifest Boundary

The manifest should record:

```text
OrderItem row count
schema information
generation metadata
```

but OrderItem generation should not construct the manifest.

---

# 88. Spark Performance Laboratory

OrderItem is likely to be one of the most useful datasets for future Spark experiments.

Potential workloads include:

```text
OrderItem ↔ Product join
OrderItem ↔ Order join
groupBy(product_id)
groupBy(order_id)
groupBy(category_id)
groupBy(customer_id)
top-N products
product revenue aggregation
basket analysis
```

---

# 89. Spark Skew Experiments

A hot product scenario can produce:

```text
product_id = HOT_PRODUCT
```

with disproportionate OrderItems.

Then:

```text
groupBy(product_id)
```

can create a hot partition.

This provides a realistic basis for:

- salting,
- repartitioning,
- adaptive query execution,
- skew join experiments.

---

# 90. Large-Scale OrderItems

At scale, OrderItems can greatly exceed Orders.

For example:

```text
Orders      = 100M
OrderItems  = 300M+
```

depending on basket-size configuration.

Therefore OrderItem generation must eventually be designed for high throughput.

---

# 91. Cardinality Scaling

OrderItem count should preferably derive from:

```text
number of Orders
×
basket-size distribution
```

rather than being configured as an unrelated absolute number.

This preserves semantic consistency.

---

# 92. Cardinality Formula

Conceptually:

```text
OrderItem count
≈
Σ basketSize(order)
```

for all generated Orders.

This means changing Order count naturally changes OrderItem count.

---

# 93. Cardinality Configuration

The user should be able to influence:

```text
orders
basket-size distribution
quantity distribution
```

and observe the resulting:

```text
OrderItem count
```

without manually configuring contradictory totals.

---

# 94. Reproducible Scaling

Given:

```text
seed = X
orders = N
basket model = Y
```

the resulting OrderItems should be reproducible.

Changing only:

```text
orders = N + Δ
```

may change the generated population naturally, but the random model should remain deterministic for a given complete configuration.

---

# 95. Parallelization

OrderItem generation is naturally partitionable by Order.

Conceptually:

```text
Order partition
      ↓
generate items
      ↓
emit OrderItems
```

This makes it suitable for future parallel generation.

---

# 96. Immutable Reference Data

Product and customer behavior inputs should ideally be immutable during generation.

This improves:

- reproducibility,
- thread safety,
- reasoning,
- parallel generation.

---

# 97. Deterministic Ordering

When generating or selecting candidates from collections, stable ordering should be used.

For example:

```text
sortBy(product.id)
```

before deterministic indexed selection where appropriate.

This prevents collection-order changes from silently changing generated data.

---

# 98. Current Simplifications

The current implementation simplifies:

- basket size,
- quantity,
- product popularity,
- customer-product affinity,
- category affinity,
- brand affinity,
- cross-sell behavior,
- product lifecycle,
- promotion,
- temporal demand,
- customer price sensitivity,
- transaction pricing.

These should be improved incrementally.

---

# 99. Planned Realism

Recommended progression:

```text
1. Preserve current OrderItem model
2. Improve basket-size distribution
3. Improve quantity distribution
4. Implement product popularity
5. Add customer/category affinity
6. Add brand/product conditioning
7. Improve product price distribution
8. Derive transaction prices
9. Add basket complementarity
10. Add promotion/seasonality
11. Add product lifecycle
12. Add advanced customer-product correlations
```

---

# 100. Migration Strategy

OrderItem migration should happen after Order architecture is stabilized enough to provide a clear transaction context.

## Step 1

Inspect the current OrderItem model.

## Step 2

Inspect current OrderItem generation.

## Step 3

Inspect Product dependencies.

## Step 4

Inspect quantity logic.

## Step 5

Inspect pricing/line-total logic.

## Step 6

Identify responsibilities.

## Step 7

Move model into:

```text
orderitem/model
```

## Step 8

Move generator into:

```text
orderitem/generator
```

## Step 9

Introduce behavioral abstractions only where needed.

## Step 10

Update Order generation orchestration.

## Step 11

Update validation.

## Step 12

Update statistics.

## Step 13

Run focused tests.

## Step 14

Run complete suite.

## Step 15

Run end-to-end generation.

## Step 16

Compare baseline statistics.

---

# 101. Migration Quality Gate

OrderItem migration is complete when:

### Model

- fields remain intentional,
- no accidental schema expansion.

### Relationships

- valid Order references,
- valid Product references.

### Business rules

- every clean Order has valid items,
- quantity is valid,
- monetary values are coherent.

### Architecture

- Product loading is not duplicated,
- output is separate,
- statistics are separate,
- quality injection is separate.

### Behavior

- product selection has a clear owner,
- basket size has a clear owner,
- quantity has a clear owner.

### Testing

- unit tests pass,
- generator tests pass,
- integration tests pass,
- full suite passes.

### Reproducibility

- same inputs produce the same logical result.

---

# 102. Design Decisions

## Decision A — OrderItem is the transaction-to-product bridge

It connects Orders to Products.

---

## Decision B — Basket size is an Order-level concept

The number of OrderItems is determined for an Order, not independently for each item.

---

## Decision C — Quantity is an item-level concept

Quantity belongs to the individual purchased product line.

---

## Decision D — Product selection is behavior-driven

Long-term product selection should use popularity and customer affinity rather than uniform random selection.

---

## Decision E — Order totals derive from OrderItems

The transactional total should be mathematically consistent with its lines.

---

## Decision F — Product catalog price and transaction price are distinct concepts

Discounts and promotions should be represented at the correct pricing boundary.

---

## Decision G — Data-quality corruption is separate from clean generation

The Quality Engine deliberately introduces defects.

---

## Decision H — OrderItem generation should be compositional

Basket size, product selection, quantity, and pricing are separate behavior boundaries.

---

## Decision I — Strategy is justified for genuine behavior variation

Do not introduce abstractions merely for architectural appearance.

---

# 103. Final Mental Model

The simplest business representation is:

```text
Order
  │
  ├── OrderItem
  │      ├── Product
  │      ├── Quantity
  │      └── Transaction Price
  │
  ├── OrderItem
  │      ├── Product
  │      ├── Quantity
  │      └── Transaction Price
  │
  └── ...
```

The realistic generation flow is:

```text
CustomerBehaviorProfile
        │
        ├── category affinity
        ├── brand affinity
        ├── price sensitivity
        └── spending level
                │
                ▼
          BasketSizeModel
                │
                ▼
       ProductSelectionModel
                │
                ├── popularity
                ├── category
                ├── brand
                └── customer affinity
                        │
                        ▼
                  QuantityModel
                        │
                        ▼
                   PricingModel
                        │
                        ▼
                    OrderItem
```

Then:

```text
OrderItems
    ↓
line totals
    ↓
Order total
```

---

# 104. Summary

OrderItem is one of the most important domains in the ShopSphere generator because it transforms abstract Orders and catalog Products into observable customer demand.

The current baseline:

```text
5,000 Orders
12,000 OrderItems
2.40 items/order
```

works structurally but is too deterministic for the project's long-term realism goals.

The major improvements are:

```text
variable basket size
        ↓
realistic quantity
        ↓
long-tail product popularity
        ↓
customer/category/brand affinity
        ↓
price sensitivity
        ↓
realistic transaction pricing
        ↓
basket-level correlations
        ↓
promotion/seasonality
        ↓
product lifecycle
```

The architectural target is:

```text
Order
   ↓
BasketSizeModel
   ↓
ProductSelectionModel
   ↓
QuantityModel
   ↓
PricingModel
   ↓
OrderItem
```

with cross-cutting systems handling:

```text
configuration
randomness
quality
validation
statistics
output
observability
```

The most important principle is:

> **OrderItems should look like the purchases produced by a population of customers with different preferences, behaviors, budgets, and activity levels—not like uniformly sampled Products attached to Orders.**

The next domain is **Payment**, where the transaction moves from purchase intent and OrderItem economics into payment-method selection, payment outcomes, failures, retries, and payment lifecycle behavior.
