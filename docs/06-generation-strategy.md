# Generation Strategy

## Project

`ecommerce-data-generation`

## Purpose

This document defines how the E-Commerce Data Generation Platform should generate data.

The objective is not simply to create a large number of records.

The generator must create data that is:

```text
correct
realistic
related
configurable
reproducible
measurable
```

The strategy described here is the initial direction. Implementation details may evolve as we learn more about the domain and generation requirements.

---

# 1. Core Generation Principle

The generator should follow this fundamental model:

```text
Configuration
      +
Deterministic Seed
      +
Reference Data
      +
Domain Rules
      +
Statistical Distributions
      ↓
Generated Dataset
      ↓
Validation
      ↓
Manifest + Statistics
```

The generation process should not depend on uncontrolled external state.

---

# 2. Generation Is a Pipeline

## Decision

Generation should be treated as a pipeline of explicit stages.

The conceptual stages are:

```text
1. Load configuration
2. Validate configuration
3. Initialize deterministic randomness
4. Load/build reference data
5. Generate domain entities
6. Establish relationships
7. Generate dependent transactional/activity data
8. Apply configured scenarios
9. Apply configured data-quality conditions
10. Validate generated data
11. Write datasets
12. Generate statistics
13. Generate manifest
```

The exact order of individual stages may change when implementation details require it.

---

# 3. Configuration Must Be Resolved Before Generation

Before any large dataset is generated, the application must establish the effective configuration.

Conceptually:

```text
default configuration
        ↓
profile
        ↓
scenario
        ↓
explicit overrides
        ↓
validation
        ↓
effective configuration
```

The generation engine should receive the resulting validated configuration.

Generators should not independently interpret profiles or scenarios.

---

# 4. Deterministic Randomness

Every random decision must ultimately derive from the configured seed.

Example:

```text
seed = 12345
```

The same:

```text
configuration + seed + generator version
```

should produce an equivalent generation result.

Changing the seed should produce a different dataset while preserving the intended statistical characteristics.

## Important Principle

Avoid uncontrolled calls to global/random system state.

Randomness should be available through controlled generation abstractions.

---

# 5. Reference Data First

Reference data should be generated or loaded before entities that depend on it.

Initial reference domains include:

```text
countries
states / provinces
cities
categories
brands
```

Potentially:

```text
product taxonomy
device types
platforms
payment methods
order statuses
event types
```

Reference data establishes valid domains from which transactional data can draw.

---

# 6. Dependency-Aware Generation

Entities should be generated according to their dependencies.

A conceptual dependency graph is:

```text
Geography
    ↓
Customer
    ↓
Address

Category ──┐
           ├──> Product
Brand ─────┘

Customer ──────> Order
Product ───────> OrderItem
Order ──────────> OrderItem
Order ──────────> Payment
Order ──────────> Shipment
Order ──────────> Return

Customer ──────> Session
Session ────────> Event
Product ────────> Event
```

This is a logical dependency model rather than a requirement that every entity be generated in exactly one pass.

---

# 7. Generate Stable Identifiers

Each major entity requires an identifier.

Examples:

```text
customer_id
address_id
category_id
brand_id
product_id
order_id
order_item_id
payment_id
shipment_id
return_id
session_id
event_id
```

Identifiers should be:

```text
unique within the intended domain
stable for the generation run
joinable
validatable
reproducible
```

The exact identifier-generation implementation will be decided during implementation.

---

# 8. Generate Reference Entities Before Dependent Entities

Reference entities should generally be generated before transactional entities.

For example:

```text
Category
Brand
    ↓
Product
    ↓
Order
    ↓
OrderItem
```

Likewise:

```text
Customer
    ↓
Session
    ↓
Event
```

This allows dependent records to reference valid existing entities.

---

# 9. Customer Generation Strategy

Customer generation should occur in stages.

Conceptually:

```text
Generate customer identity
        ↓
Assign geography
        ↓
Assign segment
        ↓
Assign acquisition channel
        ↓
Generate internal behavioral profile
```

The behavioral profile may include:

```text
activity_level
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
```

These attributes should subsequently influence related generated behavior.

---

# 10. Product Generation Strategy

Product generation should follow:

```text
Generate product identity
        ↓
Assign category
        ↓
Assign brand
        ↓
Generate category-appropriate attributes
        ↓
Assign popularity characteristics
```

Product attributes should be correlated with category where appropriate.

For example, price distributions should not necessarily be identical for every category.

---

# 11. Order Generation Strategy

Orders depend primarily on customers.

Conceptually:

```text
Customer behavioral profile
          ↓
Order frequency
          ↓
Order timestamp
          ↓
Order
```

Order generation should consider:

```text
customer activity
purchase frequency
time patterns
order status
average order value
category preference
```

The relationship should remain probabilistic.

A high-frequency customer should be more likely to generate many orders, but not every customer characteristic should become a deterministic rule.

---

# 12. Order Item Generation Strategy

For each order:

```text
Order
  ↓
Determine item count
  ↓
Select products
  ↓
Determine quantity
  ↓
Determine unit price
  ↓
Determine discount
  ↓
Calculate line total
```

The number of items should follow a distribution.

Conceptually:

```text
Most orders → 1–3 items
Some orders → 4–10 items
Small percentage → 10+ items
```

Product selection should eventually reflect:

```text
product popularity
customer preferred category
category availability
```

rather than selecting every product uniformly.

---

# 13. Derived Values Should Be Calculated

When a value logically follows from generated records, it should be derived.

Example:

```text
OrderItem
    quantity
    unit_price
    discount
        ↓
    line_total
```

Then:

```text
OrderItems
    ↓
order_total
```

Then:

```text
Order
    ↓
Payment amount
```

This prevents independent random values from creating inconsistent business records.

---

# 14. Payment Generation

Payment generation should depend on the order.

Conceptually:

```text
Order
  ↓
Payment method
  ↓
Payment status
  ↓
Payment timestamp
  ↓
Payment amount
```

Payment amount should be logically related to the order amount.

Payment method distribution should eventually be configurable.

---

# 15. Shipment and Return Generation

Shipment and return data should be generated from the relevant order state and business behavior.

Conceptually:

```text
Order
  ├──> Shipment
  └──> Return
```

Generation should eventually consider:

```text
order status
fulfillment timing
customer behavior
return probability
```

The detailed algorithms are intentionally deferred until these domains are implemented.

---

# 16. Session Generation

Sessions depend on customers.

Conceptually:

```text
Customer behavioral profile
          ↓
Session frequency
          ↓
Session start
          ↓
Session duration
          ↓
Session
```

Session characteristics should vary across customers.

For example:

```text
low activity customer
    → fewer sessions

high activity customer
    → more sessions
```

This relationship should be probabilistic rather than absolute.

---

# 17. Event Generation

Events depend on sessions.

Conceptually:

```text
Session
   ↓
Determine event sequence
   ↓
Generate event timestamps
   ↓
Generate event records
```

Possible sequence:

```text
SEARCH
   ↓
VIEW_PRODUCT
   ↓
ADD_TO_CART
   ↓
BEGIN_CHECKOUT
   ↓
PURCHASE
```

But the generator must also produce incomplete sequences.

For example:

```text
SEARCH
   ↓
VIEW_PRODUCT
```

or:

```text
VIEW_PRODUCT
   ↓
ADD_TO_CART
```

Not every session should result in a purchase.

---

# 18. Temporal Generation

Timestamps should be generated using the configured time model.

The generator should eventually account for:

```text
daily patterns
weekly patterns
seasonality
holiday periods
festival periods
marketing campaigns
sales events
```

The temporal model should influence event/order/session activity rather than assigning timestamps independently and uniformly.

---

# 19. Probabilistic Relationships

The generator should prefer probability-based relationships.

Example:

```text
Customer Segment
       ↓
higher probability of:
       ├── more activity
       ├── higher spending
       └── repeat purchases
```

This does not mean:

```text
VIP → exactly 10 orders
```

Instead:

```text
VIP → higher probability of frequent/high-value behavior
```

This distinction is important for realistic data.

---

# 20. Product Popularity

Product selection should eventually use a long-tail distribution.

Conceptually:

```text
                 Sales
                   ^
                   |
                   |                   |                    |                     |   \________
                   |
                   +-----------------> Products
```

Most products should receive relatively little activity.

A small number should receive substantially more activity.

This behavior will later provide a natural basis for controlled skew scenarios.

---

# 21. Customer Activity

Customer activity should also be heterogeneous.

Conceptually:

```text
Low activity
    ↓
Medium activity
    ↓
High activity
```

This affects:

```text
sessions
events
orders
```

The generator should avoid making every customer equally active.

---

# 22. Generation Granularity

The generator should avoid unnecessarily materializing the entire dataset in memory.

The exact streaming/chunking strategy will be determined during implementation.

The intended direction is:

```text
generate manageable batch
        ↓
write batch
        ↓
release temporary state
        ↓
generate next batch
```

This becomes increasingly important as dataset volume grows.

## Important Constraint

The generation algorithm must still preserve required relationships and reproducibility.

---

# 23. Relationship State

Some generation stages will require access to previously generated entities.

For example:

```text
OrderItem generation
    requires:
        order_id
        product_id
```

and:

```text
Event generation
    requires:
        session_id
        customer_id
        product_id where applicable
```

The implementation must therefore distinguish between:

```text
full entity data required for generation
```

and:

```text
minimal reference state required for relationships
```

We should avoid retaining large unnecessary objects in memory.

---

# 24. Scenario Application

Scenarios should modify generation characteristics through established configuration and generation abstractions.

Conceptually:

```text
Baseline Configuration
        ↓
Scenario
        ↓
Effective Configuration
        ↓
Normal Generation Pipeline
```

The normal entity-generation architecture should remain intact.

---

# 25. Data Quality Application

Data-quality scenarios should be applied deliberately.

Conceptually:

```text
Valid generated data
        ↓
Configured quality transformations
        ↓
Scenario dataset
        ↓
Validation
```

However, the exact stage at which each type of defect is introduced may vary.

For example:

```text
null value
```

may be applied at record generation time, while:

```text
duplicate record
```

may be more naturally introduced at a later stage.

The implementation should choose the stage that provides the clearest and most controllable behavior.

---

# 26. Validation Strategy

Validation should occur after generation and before a run is considered successful.

At minimum:

```text
row counts
unique identifiers
referential integrity
business invariants
null rates
duplicate rates
distribution statistics
```

Validation should produce actionable failure information.

---

# 27. Writing Strategy

Generated datasets should be written incrementally rather than requiring the entire dataset to exist in memory.

Conceptually:

```text
Generator
   ↓
Batch / partition
   ↓
Writer
   ↓
CSV file
```

The writer should eventually be abstract enough to support additional formats.

The initial implementation will use CSV.

---

# 28. Generation Statistics

During generation, the application should collect statistics needed for the final manifest.

Potential statistics include:

```text
rows generated
unique key counts
null counts
duplicate counts
min/max values
distribution summaries
generation duration
file counts
output size
```

The exact statistics should be introduced progressively.

---

# 29. Generation Manifest

At the end of a successful run, the generator should create:

```text
generation-manifest.json
```

The manifest should record enough information to understand the dataset.

Conceptually:

```text
generation_id
generator_version
seed
profile
scenario
effective_configuration
date range
row counts
unique counts
file counts
file sizes
validation status
generation statistics
```

---

# 30. Failure Behavior

A generation run should fail clearly when a required invariant cannot be satisfied.

Examples:

```text
invalid configuration
reference-data inconsistency
duplicate identifier
broken relationship
invalid business value
validation failure
unrecoverable output failure
```

The application should not silently produce a dataset that violates its clean-data contract.

---

# 31. Generation Run Identity

Every generation run should have a unique generation identifier.

Conceptually:

```text
generation-2026-09-11-<unique-suffix>
```

The exact identifier format will be finalized during implementation.

The identifier should be used to isolate output and connect:

```text
dataset
+
manifest
+
statistics
+
logs
```

---

# 32. Initial End-to-End Flow

The first complete implementation should eventually resemble:

```text
CLI
 ↓
Load Configuration
 ↓
Validate Configuration
 ↓
Initialize Seed
 ↓
Load Reference Data
 ↓
Generate Customers
 ↓
Generate Addresses
 ↓
Generate Products
 ↓
Generate Orders
 ↓
Generate Order Items
 ↓
Generate Payments
 ↓
Generate Shipments
 ↓
Generate Returns
 ↓
Generate Sessions
 ↓
Generate Events
 ↓
Apply Configured Scenario Behavior
 ↓
Validate
 ↓
Write / Finalize Outputs
 ↓
Generate Statistics
 ↓
Generate Manifest
```

The actual implementation order may be simplified during early development.

---

# 33. First Vertical Slice

We should not implement the entire pipeline before testing the architecture.

The first meaningful vertical slice should be:

```text
Configuration
    ↓
Seed
    ↓
Reference Data
    ↓
Customers
    ↓
Products
    ↓
Orders
    ↓
Order Items
    ↓
Validation
    ↓
CSV Output
    ↓
Manifest
```

This gives us enough functionality to validate:

```text
configuration
determinism
entity generation
relationships
derived values
writing
validation
metadata
```

before adding the remaining domains.

---

# 34. Generation Strategy and Future Scale

The generator must eventually support:

```text
small
medium
large
xlarge
```

without changing the business-generation architecture.

As scale increases, we may need to change:

```text
batch size
I/O strategy
reference-state representation
parallel generation strategy
memory management
```

Those are implementation optimizations.

They should not change the underlying business semantics.

---

# 35. Guiding Principle

The generation strategy should always preserve this hierarchy:

```text
Business correctness
        ↓
Relationship correctness
        ↓
Statistical realism
        ↓
Configurability
        ↓
Reproducibility
        ↓
Scalability
```

The generator is successful when it produces data that is not merely large, but behaves like a controlled synthetic e-commerce source system.

---

# Status

**Accepted — Initial Generation Strategy**

This document defines the current generation approach.

The following areas are intentionally left for dedicated future decisions:

```text
exact distribution algorithms
exact random-number implementation
parallel generation
batch sizing
identifier implementation
writer implementation
detailed shipment generation
detailed return generation
```

Those decisions should be recorded when their implementation phases require them.
