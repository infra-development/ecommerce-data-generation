# ShopSphere Data Generator — Project & Business Domain

## 1. Purpose

ShopSphere Data Generator is a configurable synthetic e-commerce data platform.

Its purpose is to generate a large, realistic, internally consistent e-commerce dataset that can later be used as the data foundation for Spark performance and data-engineering experiments.

The generator is not intended to copy a particular company's private dataset. It is intended to create a plausible e-commerce world whose business behavior, relationships, distributions, temporal patterns, skew, and data-quality characteristics can be controlled through configuration.

The project has two goals:

1. **Business realism** — generated data should behave like a plausible e-commerce business.
2. **Engineering realism** — generated data should contain the scale, cardinality, skew, correlations, and quality characteristics needed for performance and data-engineering experiments.

---

## 2. What We Are Building

At a high level, ShopSphere represents an e-commerce platform with:

- customers
- addresses
- products
- categories
- brands
- orders
- order items
- payments
- shipments
- returns
- sessions
- events

The generator creates these entities while preserving their business relationships.

The target is not independent random records.

The target is a connected system:

```text
                         ShopSphere
                             |
        +--------------------+--------------------+
        |                    |                    |
     Customer             Product              Order
        |                    |                    |
   +----+----+         +-----+-----+       +-----+-----+-----+
   |         |         |           |       |     |     |     |
Address   Session   Category     Brand   Item Payment Shipment Return
              |
            Event
```

---

## 3. Business Lifecycle

A simplified ShopSphere customer journey is:

```text
Acquisition
    |
    v
Registration
    |
    v
Browsing / Session
    |
    v
Product Discovery
    |
    v
Product Interaction
    |
    +----> Leave
    |
    v
Add to Cart
    |
    +----> Abandon
    |
    v
Checkout
    |
    +----> Abandon
    |
    v
Payment
    |
    +----> Failure
    |
    v
Order
    |
    v
Shipment
    |
    v
Delivery
    |
    +----> Return
    |
    v
Customer continues lifecycle
```

This lifecycle is a target business model. The current implementation does not yet model every stage with full behavioral sophistication.

---

## 4. Domain Entities

### 4.1 Customer

A Customer represents a person who has registered with ShopSphere and may browse, purchase, return products, and interact with the platform over time.

A customer is expected to influence multiple downstream behaviors.

Important customer concepts include:

- registration
- demographic attributes
- customer status
- customer segment
- acquisition channel
- acquisition campaign
- preferred device
- preferred payment method
- purchase activity
- spending behavior
- category affinity
- brand affinity
- return tendency
- lifecycle state

The current implementation contains the basic customer attributes and generation configuration. More sophisticated behavioral modeling is part of the realism roadmap.

### 4.2 Address

An Address represents a customer's physical location through the ShopSphere geography hierarchy.

Current attributes:

```text
id
customerId
buildingId
unitNumber
postalCode
```

The geography hierarchy is:

```text
Country
  |
State
  |
City
  |
Area
  |
Road
  |
Society
  |
Building
  |
Flat / Unit
```

Postal codes belong to an Area.

### 4.3 Product

A Product represents an item that can be purchased through ShopSphere.

Products are associated with:

```text
Category
Product Type
Brand
Product Model
```

Product generation also includes pricing and product-selection behavior.

A future realism layer will make product popularity long-tailed and introduce customer/product/category affinities.

### 4.4 Category

A Category groups products into a business product domain.

Categories can later influence:

- relative sales volume
- customer affinity
- seasonal demand
- pricing characteristics
- return behavior

Categories are primarily reference/master data in the current design.

### 4.5 Brand

A Brand represents a product manufacturer or consumer-facing brand.

Brand information participates in product construction and can later influence:

- product popularity
- customer brand affinity
- pricing
- purchase behavior

Brands are primarily reference/master data in the current design.

### 4.6 Order

An Order represents a customer's purchase transaction.

An order belongs to a customer and contains one or more order items.

Orders also participate in:

- payment
- shipment
- return
- order status
- financial calculations

Order generation will eventually be strongly influenced by customer behavior.

### 4.7 Order Item

An Order Item represents one product line within an order.

It connects:

```text
Order -> OrderItem -> Product
```

Order-item generation determines:

- product selected
- quantity
- item price
- item amount

The number of items per order should eventually follow a realistic heterogeneous distribution rather than a narrow deterministic range.

### 4.8 Payment

A Payment represents the payment associated with an order.

The current generator supports payment methods such as:

- UPI
- credit card
- debit card
- net banking
- wallet

Payment behavior can later depend on customer preference, device, order value, and other business factors.

### 4.9 Shipment

A Shipment represents fulfillment of an order.

It is related to the order and includes fulfillment-related dates and state.

Future realism can introduce differences in shipping behavior based on geography, order characteristics, and operational conditions.

### 4.10 Return

A Return represents a product/order return after purchase.

Returns should eventually depend on more than a global probability.

Potential drivers include:

- customer return tendency
- product/category return risk
- order characteristics
- price/discount
- customer behavior

### 4.11 Session

A Session represents a period of customer interaction with the ShopSphere platform.

Session behavior should eventually depend on customer activity level and lifecycle.

The current implementation uses a fixed session count per customer and is therefore intentionally simpler than the target model.

### 4.12 Event

An Event represents an interaction occurring during a session.

Conceptual stages include:

```text
session started
product viewed
add to cart
checkout
payment
order
```

The event model should eventually represent a realistic funnel rather than generating an identical number of events for every session.

---

## 5. Core Relationships

The major relationships are:

```text
Customer
   |
   +---- Address
   |
   +---- Session
   |        |
   |        +---- Event
   |
   +---- Order
            |
            +---- OrderItem ---- Product
            |
            +---- Payment
            |
            +---- Shipment
            |
            +---- Return

Product
   |
   +---- Category
   |
   +---- Brand
```

These relationships are fundamental to the generator.

A record should not merely be individually valid; relationships between records should also make business sense.

---

## 6. Cardinality Philosophy

Real e-commerce data is heterogeneous.

We do not want every customer, product, order, or session to behave identically.

Examples of desired future behavior:

```text
Customers
    |
    +-- occasional shoppers
    +-- regular shoppers
    +-- high-frequency shoppers
    +-- high-value customers

Products
    |
    +-- very popular products
    +-- moderately popular products
    +-- long-tail products

Sessions
    |
    +-- short / low-activity
    +-- normal
    +-- highly active
```

This heterogeneity is one of the central goals of the realism phase.

---

## 7. Behavioral Realism

The target architecture is based on the idea that entity attributes should not be generated independently when a business relationship exists.

For example:

```text
Customer Behavioral Profile
        |
        +-- Activity
        +-- Spending
        +-- Price Sensitivity
        +-- Category Affinity
        +-- Brand Affinity
        +-- Return Propensity
        +-- Device Affinity
        +-- Payment Preference
        |
        +-------------------+
                            |
             +--------------+--------------+
             |              |              |
          Sessions        Orders         Returns
                            |
                         Products
```

A customer's underlying behavioral characteristics can influence multiple downstream entities.

This is the foundation for sophisticated realism.

---

## 8. Temporal Behavior

The long-term target is a generator that understands that e-commerce behavior changes over time.

Potential drivers include:

- customer lifecycle
- acquisition period
- seasonality
- campaigns
- holidays
- promotions
- product demand changes
- customer activity changes

For example:

```text
Customer acquired
       |
       v
New customer
       |
       v
Active customer
       |
       +----> Loyal
       |
       +----> Dormant
       |
       +----> Churned
       |
       +----> Reactivated
```

Temporal modeling will be developed incrementally.

---

## 9. Data Realism vs. Data Validity

These are separate goals.

### Validity

The data obeys structural and business constraints.

Examples:

- primary keys are unique
- foreign keys resolve
- order totals are correct
- dates are logically ordered
- required relationships exist

### Realism

The data resembles plausible e-commerce behavior.

Examples:

- customers have heterogeneous purchase frequency
- products have long-tail popularity
- AOV follows a believable distribution
- customer preferences correlate with purchases
- sessions and events vary by activity
- returns vary by customer and product characteristics

The current project has a stronger foundation for validity than for sophisticated realism. The realism phase will address this gap.

---

## 10. Technical Architecture

The codebase is being reorganized around business domains/entities rather than primarily around technical layers.

Target structure:

```text
com.shopsphere.datagenerator/
|
+-- customer/
+-- address/
+-- product/
+-- category/
+-- brand/
+-- order/
+-- orderitem/
+-- payment/
+-- shipment/
+-- return/
+-- session/
+-- event/
|
+-- geography/
|
+-- common/
|
+-- config/
+-- generation/
+-- relationship/
+-- quality/
+-- scenario/
+-- validation/
+-- output/
+-- manifest/
+-- statistics/
+-- observability/
|
+-- Main.scala
```

An entity package may contain only the components actually required:

```text
customer/
    config/
    model/
    generator/
    behavior/
    validation/
    statistics/
```

A simpler entity may only need:

```text
address/
    model/
    generator/
    validation/
```

We will not create unnecessary abstractions or empty packages merely for symmetry.

---

## 11. Cross-Cutting Infrastructure

Not everything belongs inside an entity.

Shared infrastructure remains centralized.

Examples:

```text
common/
    distribution/
    random/
    time/
    util/
```

System-level orchestration also remains outside individual entities:

```text
generation/
relationship/
quality/
scenario/
output/
manifest/
observability/
```

The rule is:

> Domain behavior belongs to the domain. Shared mechanisms belong to shared infrastructure. System orchestration belongs to the system layer.

---

## 12. Generation Flow

The intended technical flow is:

```text
Configuration
     |
     v
Generation Plan
     |
     v
Generation Context
     |
     +---- Random / Seed
     +---- Geography Reference Data
     +---- Catalog Reference Data
     +---- Distribution Engine
     |
     v
Entity Generation
     |
     +---- Customer
     +---- Address
     +---- Product
     +---- Order
     +---- Order Item
     +---- Payment
     +---- Shipment
     +---- Return
     +---- Session
     +---- Event
     |
     v
Quality / Scenario Processing
     |
     v
Validation
     |
     v
Statistics / Manifest
     |
     v
CSV Output
```

The implementation will evolve as realism becomes more sophisticated.

---

## 13. Configuration Philosophy

Configuration should control business-scale and behavioral assumptions without requiring source-code changes.

The generator already supports concepts such as:

- seed
- generation profile
- cardinality profile
- scenario
- distributions
- product distributions
- product pricing
- brand affinity
- customer generation

The target is to make important business assumptions configurable while keeping the domain model understandable.

---

## 14. Reproducibility

A seed is a fundamental part of generation.

The same configuration and seed should produce reproducible results.

Randomness should be derived in a controlled way so that unrelated generation changes do not unnecessarily destabilize every generated entity.

This is particularly important for:

- testing
- debugging
- comparing realism changes
- performance experiments
- regression testing

Detailed reproducibility rules belong in the dedicated reproducibility concept document.

---

## 15. Validation Philosophy

Validation happens at multiple levels.

### Structural

Are the required files and columns present?

### Primary-key

Are IDs unique?

### Foreign-key

Do relationships resolve?

### Business rules

Are domain constraints satisfied?

### Statistical / realism validation

Do generated distributions remain within the expected ranges and exhibit the intended characteristics?

The final category will become increasingly important as sophisticated realism is implemented.

---

## 16. Current State

The current generator is a working foundation.

It can:

- load configuration
- load reference data
- build a generation plan
- generate the core entities
- generate 12 CSV outputs
- produce deterministic data from a seed
- calculate statistics
- validate structural and relational correctness

The current baseline is intentionally not considered the final realism implementation.

Known simplifications include:

- customer behavior is not yet modeled as a coherent latent profile
- orders per customer are not sufficiently heterogeneous
- items per order have a narrow distribution
- product popularity is not yet strongly long-tailed
- customer/product/category correlations are limited
- sessions per customer are currently fixed
- events per session are currently fixed
- monetary behavior needs better modeling
- data-quality and skew scenarios require further maturity

---

## 17. Realism Roadmap

The planned realism progression is:

```text
1. Customer Behavioral Profile
          |
2. Orders per Customer
          |
3. Items per Order
          |
4. Product Popularity / Long Tail
          |
5. Customer-Product-Category Affinity
          |
6. Spending / AOV Model
          |
7. Session and Event Behavior
          |
8. Lifecycle and Temporal Behavior
          |
9. Return Behavior
          |
10. Funnel / Conversion Behavior
          |
11. Controlled Data Quality
          |
12. Controlled Skew
          |
13. Statistical Validation and Calibration
```

The order may change as implementation reveals dependencies.

---

## 18. What "Sophisticated Realism" Means for This Project

Our target is not to reproduce a specific company's private production data.

Our target is:

> Given a business configuration, generate a statistically plausible e-commerce population whose entities, behavior, temporal patterns, relationships, distributions, skew, and data-quality characteristics remain internally coherent at arbitrary scale.

Realism is evaluated at several levels:

```text
Attribute realism
        |
Relationship realism
        |
Distribution realism
        |
Behavioral realism
        |
Temporal realism
        |
Population realism
        |
System realism
```

---

## 19. Development Principle

From this point forward, development should follow:

```text
Business Understanding
        |
        v
Expected Behavior
        |
        v
Domain / Technical Design
        |
        v
Implementation
        |
        v
Tests
        |
        v
Generated Data
        |
        v
Statistics / Validation
        |
        v
Business Review
        |
        v
Refinement
```

The objective is not simply to make tests pass.

The objective is to understand what the system is supposed to represent and then make the implementation produce that behavior.

---

## 20. Documentation Strategy

Documentation is organized around two complementary dimensions.

### Domain documents

Each entity gets a document covering:

1. Business meaning
2. Attributes
3. Lifecycle
4. Behavior
5. Relationships
6. Generation strategy
7. Configuration
8. Technical implementation
9. Validation
10. Current simplifications
11. Future realism
12. Important design decisions

### Cross-cutting concept documents

Shared technical concepts are documented separately to avoid duplication:

- distributions
- randomness and reproducibility
- relationships
- data quality
- skew
- validation
- output/storage
- observability

### Progress document

A dedicated progress document tracks:

- current phase
- completed work
- work in progress
- next work
- entity-level business/design/code/test/realism maturity
- architectural migration status
- major decisions and milestones

---

## 21. Guiding Principle

The project should always answer two questions together:

> **What does this represent in the business?**

and

> **How does our code generate and enforce that behavior?**

If a developer can read the relevant domain document and understand both answers, the documentation is doing its job.
