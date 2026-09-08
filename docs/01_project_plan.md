# E-Commerce Data Generation Platform
## Business Understanding, Product Vision & Complete Project Plan

**Project:** `ecommerce-data-generation`  
**Document:** Business Understanding & Project Plan  
**Status:** Project Definition

---

# 1. Executive Summary

We are building a **synthetic e-commerce data-generation platform**.

The platform will generate realistic data for an online e-commerce company. The generated data will become the data foundation for the entire Spark Performance Laboratory.

The generator must support:

- configurable data volume
- configurable cardinality
- realistic distributions
- relationships between entities
- realistic timestamps and behavior
- deterministic/reproducible generation
- controlled data-quality problems
- controlled data skew
- scale profiles
- validation
- generation statistics and manifests

The key requirement is:

> **The same generation code should produce small, medium, large, and extreme datasets by changing configuration rather than changing application logic.**

---

# 2. Business Scenario

Imagine a large online retailer called **ShopSphere**.

Customers use its web and mobile applications to:

1. create accounts
2. browse products
3. search for products
4. add products to carts
5. place orders
6. make payments
7. receive shipments
8. return products
9. purchase repeatedly

The business operates across multiple countries, states/provinces, cities, product categories and brands.

The company wants analytics around:

- sales
- customers
- products
- categories
- payments
- customer activity
- conversion
- retention
- revenue
- geography
- order behavior
- product popularity

Our generator simulates the source systems producing this data.

---

# 3. Why This Project Exists

Tiny manually-created datasets are not sufficient for serious Spark learning.

We need data containing:

- millions or billions of rows
- many unique keys
- repeated keys
- relational joins
- realistic distributions
- long-tail behavior
- realistic timestamps
- correlations
- controlled skew
- controllable data-quality problems

We also need experiments to be reproducible.

If a Spark optimization changes runtime from 20 minutes to 8 minutes, we need confidence that the underlying dataset did not change.

Therefore:

```text
Configuration
     +
Deterministic seed
     +
Controlled distributions
     +
Referential integrity
     +
Validation
     =
Reproducible experimental dataset
```

---

# 4. Project Vision

The generator should eventually look like:

```text
                    Configuration
                         |
                         v
                Data Generation Engine
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
      Reference      Transaction       Event
        Data             Data          Data
          |              |              |
          +--------------+--------------+
                         |
                         v
                   Validation
                         |
                         v
                    Raw Dataset
                         |
              +----------+----------+
              |                     |
              v                     v
          Manifest              Statistics
```

The output becomes the controlled data source for the Spark learning projects.

---

# 5. Core Product Requirements

The generator must control six major dimensions.

## 5.1 Volume

Configure record counts independently:

```text
customers
products
orders
order_items
payments
sessions
events
```

Example:

```hocon
customers = 1000000
products = 100000
orders = 10000000
events = 100000000
```

Changing these values must not require changes to generation logic.

---

## 5.2 Cardinality

Control unique-value counts.

Examples:

```text
unique customers
unique products
unique categories
unique brands
unique sessions
unique cities
```

This matters because:

```text
100M rows + 100K customer IDs
```

is a very different workload from:

```text
100M rows + 50M customer IDs
```

This distinction will be important for Spark aggregations, joins and memory experiments.

---

## 5.3 Distribution

Values must not simply be uniform random values.

Examples:

```text
customer order frequency
product popularity
order amount
customer spending
event frequency
category popularity
payment method usage
```

Use appropriate distributions such as:

```text
uniform
weighted categorical
normal
log-normal
Poisson
exponential
Zipf/power-law style
```

The exact distribution should be selected according to the business meaning.

---

## 5.4 Correlation

Business fields should have meaningful relationships.

Examples:

```text
country -> state -> city
customer -> orders
order -> order_items
order_item -> product
order -> payment
customer -> session
session -> events
product -> category
product -> brand
```

Behavior should also correlate where appropriate.

For example:

```text
customer segment
        |
        +--> purchase frequency
        +--> average order value
        +--> discount sensitivity
        +--> preferred categories
```

We want **probabilistic realism**, not deterministic rules.

---

## 5.5 Data Quality

The generator should optionally introduce:

```text
null values
duplicates
invalid values
missing relationships
invalid timestamps
invalid prices
invalid quantities
malformed strings
```

All should be configurable.

Default behavior should produce clean data.

---

## 5.6 Special Scenarios

The generator must eventually support controlled Spark scenarios:

```text
data skew
hot customers
hot products
high-cardinality keys
low-cardinality keys
small dimensions
large fact tables
many small files
large files
```

These scenarios will feed later Spark projects.

---

# 6. Business Entities

Initial domain:

```text
Customer
Address
Product
Category
Brand
Order
OrderItem
Payment
Shipment
Return
Session
Event
```

We should implement these incrementally.

---

# 7. Entity Relationships

Conceptually:

```text
Customer
   |
   +------------------+
   |                  |
   v                  v
Address            Session
                      |
                      v
                    Event
                      |
                      +--> Product
                      +--> Customer

Customer
   |
   v
Order
   |
   +------------------+
   |                  |
   v                  v
OrderItem          Payment
   |
   v
Product
   |
   +--> Category
   +--> Brand

Order
   |
   +--> Shipment
   |
   +--> Return
```

The physical implementation can evolve, but relationships must remain meaningful.

---

# 8. Customer Domain

Customer attributes:

```text
customer_id
first_name
last_name
email
gender
date_of_birth
signup_date
country
state
city
customer_segment
acquisition_channel
```

Segments:

```text
STANDARD
PREMIUM
VIP
BUSINESS
```

Acquisition channels:

```text
ORGANIC
SEARCH
SOCIAL
ADVERTISEMENT
REFERRAL
PARTNER
```

The generator should internally model behavioral attributes such as:

```text
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
activity_level
```

These can influence generated behavior without necessarily appearing in the raw customer table.

---

# 9. Product Domain

Products:

```text
product_id
product_name
category_id
brand_id
price
cost
weight
rating
release_date
```

Categories should support hierarchy.

Example:

```text
Electronics
  -> Mobile
  -> Laptop
  -> Television
  -> Accessories

Home
  -> Furniture
  -> Kitchen
  -> Decor

Fashion
  -> Men
  -> Women
  -> Kids
```

Product characteristics should be correlated with category.

For example, electronics should have a different price distribution from accessories.

---

# 10. Order Domain

Orders:

```text
order_id
customer_id
order_timestamp
order_status
shipping_address_id
payment_id
order_total
```

Statuses:

```text
CREATED
PAID
SHIPPED
DELIVERED
CANCELLED
RETURNED
```

Status probabilities should be configurable.

Order timestamps should follow realistic temporal patterns.

---

# 11. Order Item Domain

Each order contains one or more products.

```text
order_item_id
order_id
product_id
quantity
unit_price
discount
line_total
```

Items per order should follow a distribution.

For example:

```text
Most orders -> 1-3 items
Some orders -> 4-10 items
Small percentage -> 10+ items
```

Do not give every order exactly the same number of items.

---

# 12. Payment Domain

Payments:

```text
payment_id
order_id
payment_method
payment_status
amount
payment_timestamp
```

Payment methods:

```text
CREDIT_CARD
DEBIT_CARD
UPI
NET_BANKING
WALLET
COD
```

Payment amount should be logically related to the order amount.

---

# 13. Session and Event Domain

Sessions:

```text
session_id
customer_id
session_start
session_end
device_type
platform
```

Events:

```text
event_id
session_id
customer_id
event_timestamp
event_type
product_id
```

Event types:

```text
SEARCH
VIEW_PRODUCT
ADD_TO_CART
REMOVE_FROM_CART
BEGIN_CHECKOUT
PURCHASE
```

Sessions should support realistic sequences such as:

```text
SEARCH
  |
  v
VIEW_PRODUCT
  |
  v
ADD_TO_CART
  |
  v
BEGIN_CHECKOUT
  |
  v
PURCHASE
```

Not every session should end in a purchase.

---

# 14. Geography Model

Geography must be hierarchical.

```text
Country
   |
   v
State / Province
   |
   v
City
   |
   v
Customer
```

The generator must not produce nonsensical combinations such as:

```text
country = India
city = New York
```

Geography should come from a controlled reference dataset.

---

# 15. Time Model

Support:

```text
start_date
end_date
```

and generate realistic timestamps.

Include configurable patterns for:

### Daily behavior

```text
night -> low activity
day -> medium activity
evening -> high activity
```

### Weekly behavior

```text
weekday/weekend differences
```

### Seasonal behavior

Future capability:

```text
festival periods
holiday periods
marketing campaigns
sales events
```

The calendar should be configurable rather than hard-coded into business logic.

---

# 16. Behavioral Realism

The generator should model business behavior statistically.

Example:

```text
Customer A:
  low activity
  low purchase frequency
  low average order value

Customer B:
  medium activity
  medium purchase frequency

Customer C:
  high activity
  frequent purchases
  high average order value
```

Product popularity should also be long-tailed:

```text
Most products -> few sales
Some products -> moderate sales
Very few products -> extremely high sales
```

This behavior is especially valuable for later Spark skew experiments.

---

# 17. Configuration System

All major behavior must be configuration-driven.

Recommended configuration structure:

```text
generator
customers
products
categories
brands
orders
order_items
payments
shipments
returns
sessions
events
geography
time
distributions
data_quality
skew
output
```

Example:

```hocon
generator {
    seed = 12345
}

customers {
    count = 1000000
}

products {
    count = 100000
    categories = 100
    brands = 2000
}

orders {
    count = 10000000
}

order_items {
    average_items_per_order = 3.2
}

events {
    count = 100000000
}

data_quality {
    null_customer_id_rate = 0.0
    duplicate_order_rate = 0.0
}

skew {
    enabled = false
}
```

The exact configuration model will be finalized during implementation.

---

# 18. Reproducibility

Every random process must ultimately be controlled by a deterministic seed.

For example:

```text
seed = 12345
```

Same:

```text
configuration + seed
```

must produce equivalent data.

Changing only the seed should produce different data while maintaining similar statistical characteristics.

This is mandatory for performance experiments.

---

# 19. Scale Profiles

Support convenient named profiles.

```text
small
medium
large
xlarge
```

Example conceptual values:

### Small

```text
10K customers
100K orders
500K events
```

### Medium

```text
100K customers
1M orders
10M events
```

### Large

```text
1M customers
10M orders
100M events
```

### XLarge

```text
10M customers
100M orders
1B events
```

Exact values should remain configurable.

Profiles are presets, not hard limits.

---

# 20. Cardinality Profiles

Cardinality should be independently controllable.

Example:

```text
LOW
MEDIUM
HIGH
```

Example workload:

```text
100M orders

LOW:
100K unique customers

HIGH:
50M unique customers
```

This allows us to study Spark behavior under different key-cardinality scenarios without changing the basic business model.

---

# 21. Data Skew

Skew is a special scenario and should be disabled by default.

Example:

```hocon
skew {
    enabled = false

    hot_customer_count = 10
    hot_customer_multiplier = 100

    hot_product_count = 5
    hot_product_multiplier = 50
}
```

When enabled, a small number of customers/products can generate disproportionately large amounts of activity.

This will provide data for:

**Project 2 — The Skew Lab.**

---

# 22. Data Quality Scenarios

Example:

```hocon
data_quality {
    null_customer_id_rate = 0.001
    duplicate_order_rate = 0.0005
    invalid_price_rate = 0.0002
    invalid_timestamp_rate = 0.0001
    missing_payment_rate = 0.001
}
```

The generator should support:

```text
clean
slightly_dirty
dirty
```

profiles in addition to individual configuration values.

---

# 23. Output Structure

Initially generate raw data as CSV.

Example:

```text
data/
└── raw/
    └── generation-001/
        ├── customers/
        ├── addresses/
        ├── products/
        ├── categories/
        ├── brands/
        ├── orders/
        ├── order_items/
        ├── payments/
        ├── shipments/
        ├── returns/
        ├── sessions/
        └── events/
```

Each dataset should be capable of containing multiple files:

```text
orders/
    part-00000.csv
    part-00001.csv
    part-00002.csv
    ...
```

File count and output layout should eventually be configurable.

---

# 24. Generation Manifest

Every generation run must produce a manifest.

Example:

```text
generation-manifest.json
```

It should contain:

```text
generation_id
seed
profile
scenario
configuration
generation timestamp
date range
row counts
unique key counts
file counts
file sizes
data-quality statistics
distribution statistics
```

Example:

```json
{
  "generation_id": "generation-001",
  "seed": 12345,
  "customers": 1000000,
  "products": 100000,
  "orders": 10000000,
  "order_items": 32000000,
  "events": 100000000
}
```

This becomes the audit record for every future Spark experiment.

---

# 25. Validation

The generator must validate the resulting dataset.

## Referential integrity

Every valid order should reference an existing customer.

Every order item should reference:

```text
valid order
valid product
```

Every payment should reference its order.

Every event should reference valid session/customer/product relationships where applicable.

## Business consistency

Examples:

```text
quantity > 0
price >= 0
cost >= 0
order_total ~= sum(order items)
payment amount ~= order amount
session_end >= session_start
event_timestamp within session bounds
```

## Statistical validation

Report:

```text
row counts
unique counts
null percentages
duplicate percentages
distribution summaries
```

The generator should fail clearly when configured invariants are violated.

---

# 26. Generator Observability

The generator should show progress.

Example:

```text
Generating customers...
Generated: 1,000,000

Generating products...
Generated: 100,000

Generating orders...
Generated: 10,000,000

Generating order items...
Generated: 31,982,341

Generating events...
Generated: 100,000,000

Validating relationships...

Generation completed successfully.
```

At the end report:

```text
total generation time
rows generated
files generated
output size
validation status
```

---

# 27. Application Architecture

Suggested logical components:

```text
Application
    |
    +-- Configuration
    |
    +-- Reference Data
    |
    +-- Distribution Engine
    |
    +-- Entity Generators
    |      +-- CustomerGenerator
    |      +-- ProductGenerator
    |      +-- OrderGenerator
    |      +-- OrderItemGenerator
    |      +-- PaymentGenerator
    |      +-- SessionGenerator
    |      +-- EventGenerator
    |
    +-- Relationship Manager
    |
    +-- Data Quality Engine
    |
    +-- Scenario Engine
    |
    +-- Validator
    |
    +-- Writer
    |
    +-- Manifest
    |
    +-- Statistics
```

Do not implement the entire architecture at once.

Build it incrementally.

---

# 28. Important Technology Decision

The first version of the generator should be a **normal Scala application**, not a Spark application.

The generator's initial job is:

```text
data modeling
realistic generation
relationships
distributions
configuration
reproducibility
validation
```

Using Spark for generation would mix two separate concerns:

```text
Generate data
```

and:

```text
Learn Spark processing
```

Keeping them separate gives us a cleaner experimental environment.

If generation itself becomes a scaling problem later, that can become a separate engineering investigation.

---

# 29. Development Phases

## Phase 1 — Project Foundation

Build:

```text
sbt project
configuration loading
logging
seed management
output directory management
CLI
```

Deliverable:

> Application starts and correctly loads configuration.

---

## Phase 2 — Reference Data

Build:

```text
countries
states
cities
categories
brands
products
```

Deliverable:

> Generate coherent reference data.

---

## Phase 3 — Customers

Build:

```text
customer IDs
customer information
geography
segments
acquisition channels
behavioral profiles
```

Deliverable:

> Generate configurable customer cardinality.

---

## Phase 4 — Orders

Build:

```text
orders
order items
order status
timestamps
customer relationships
product relationships
```

Deliverable:

> Generate coherent transactional data.

---

## Phase 5 — Payments and Operations

Build:

```text
payments
shipments
returns
```

Deliverable:

> Complete the transactional model.

---

## Phase 6 — Sessions and Events

Build:

```text
sessions
events
event sequences
```

Deliverable:

> Generate realistic customer activity.

---

## Phase 7 — Distribution Engine

Replace simplistic randomness with configurable distributions.

Deliverable:

> Business behavior becomes statistically realistic.

---

## Phase 8 — Data Quality

Add configurable:

```text
nulls
duplicates
invalid records
missing relationships
```

Deliverable:

> Generate clean or intentionally dirty datasets.

---

## Phase 9 — Scale Profiles

Add:

```text
small
medium
large
xlarge
```

Deliverable:

> Change data volume through configuration.

---

## Phase 10 — Scenario Engine

Add:

```text
low cardinality
high cardinality
skew
hot keys
small dimensions
large facts
many small files
```

Deliverable:

> Produce datasets specifically designed for Spark experiments.

---

## Phase 11 — Validation and Manifest

Add:

```text
referential validation
business validation
statistical validation
generation manifest
```

Deliverable:

> Every dataset is measurable and reproducible.

---

# 30. Future Scenario Library

Named scenarios should eventually include:

```text
baseline
high-volume
high-cardinality
low-cardinality
hot-customers
hot-products
many-small-files
large-dimension
small-dimension
dirty-data
seasonal-sales
high-repeat-customers
anonymous-heavy-events
```

A scenario should be selectable through configuration:

```text
scenario = "hot-customers"
```

without modifying generation code.

---

# 31. Definition of Done

The platform is complete when these are possible.

### Volume

Generate:

```text
100K orders
```

then change only configuration and generate:

```text
100M orders
```

---

### Cardinality

Generate:

```text
100M orders
10M customers
```

then generate:

```text
100M orders
100K customers
```

using the same business model.

---

### Distribution

Product popularity and customer behavior should show realistic non-uniform distributions.

---

### Relationships

Generated data should pass referential and business validation.

---

### Reproducibility

Same configuration + same seed should reproduce equivalent results.

---

### Data Quality

Turn data-quality scenarios on/off without changing generation code.

---

### Skew

Enable the skew scenario without modifying entity-generation logic.

---

### Manifest

Every run must produce enough metadata to reproduce and understand the dataset.

---

# 32. Relationship to the Spark Learning Program

This project is the **data foundation**.

The long-term learning path becomes:

```text
PROJECT 0
E-Commerce Data Generation Platform
          |
          v
PROJECT 1
E-Commerce Analytics Pipeline
          |
          v
PROJECT 2
The Skew Lab
          |
          v
PROJECT 3
The Join Lab
          |
          v
PROJECT 4
Memory / GC / Spill Lab
          |
          v
PROJECT 5
File Format & I/O Lab
          |
          v
PROJECT 6
Spark Production Investigation
```

The same underlying business data should be reused throughout.

---

# 33. Final Product Vision

Eventually we should be able to run something conceptually like:

```text
ecommerce-data-generator     --profile large     --seed 12345     --scenario baseline
```

or:

```text
ecommerce-data-generator     --profile large     --seed 12345     --scenario hot-customers
```

and receive:

```text
data/
└── raw/
    └── generation-2026-...
        ├── customers/
        ├── products/
        ├── categories/
        ├── brands/
        ├── orders/
        ├── order_items/
        ├── payments/
        ├── shipments/
        ├── returns/
        ├── sessions/
        ├── events/
        └── generation-manifest.json
```

The generated data should **look and behave like data from a real e-commerce business**, while remaining completely controlled by configuration.

---

# 34. Guiding Principle

The generator should be built in this order:

```text
Correctness
    ↓
Business realism
    ↓
Relationships
    ↓
Configurability
    ↓
Reproducibility
    ↓
Scale
    ↓
Special scenarios
    ↓
Generation performance
```

Do not optimize the generator prematurely.

The most valuable output is not merely millions of rows.

It is a **controlled, realistic experimental environment** that lets us ask meaningful Spark questions and know exactly what changed between experiments.
