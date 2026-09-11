# Data Model Decisions

## Project

`ecommerce-data-generation`

## Purpose

This document defines the initial business-domain model for the E-Commerce Data Generation Platform.

The model represents a synthetic online retailer, **ShopSphere**, and is designed to provide realistic relational data for later Spark experiments.

The model should evolve deliberately. New entities, fields, or relationships should be introduced through explicit decisions rather than accidental expansion.

---

# 1. Business Context

ShopSphere is an online retailer whose customers can:

```text
create accounts
browse products
search for products
add products to carts
place orders
make payments
receive shipments
return products
purchase repeatedly
```

The business operates across:

```text
countries
states / provinces
cities
product categories
brands
```

The generated data should support analytics around:

```text
sales
customers
products
categories
payments
customer activity
conversion
retention
revenue
geography
order behavior
product popularity
```

---

# 2. Initial Domain Entities

The initial domain contains:

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

These entities will be implemented incrementally.

The initial implementation does not need to generate all entities at once.

---

# 3. Conceptual Relationship Model

The intended relationship structure is:

```text
Customer
 ├── Address
 ├── Session
 │    └── Event
 └── Order
      ├── OrderItem
      │    └── Product
      │         ├── Category
      │         └── Brand
      ├── Payment
      ├── Shipment
      └── Return
```

This is the conceptual business model.

The physical implementation may evolve as the project develops.

---

# 4. Customer Domain

## Purpose

Represents customers registered with ShopSphere.

## Initial Attributes

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

## Customer Segments

Initial segments:

```text
STANDARD
PREMIUM
VIP
BUSINESS
```

The segment distribution should not necessarily be uniform.

## Acquisition Channels

Initial channels:

```text
ORGANIC
SEARCH
SOCIAL
ADVERTISEMENT
REFERRAL
PARTNER
```

## Internal Behavioral Profile

The generator should internally model behavioral characteristics such as:

```text
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
activity_level
```

These values are behavioral inputs to generation.

They do not necessarily need to appear as columns in the raw customer dataset.

## Design Principle

Customer behavior should influence related generated activity probabilistically.

For example:

```text
customer profile
      |
      +--> order frequency
      +--> order value
      +--> category preference
      +--> return behavior
      +--> session activity
```

---

# 5. Address Domain

## Purpose

Represents customer addresses used by the business.

The initial relationship is:

```text
Customer
    |
    v
Address
```

Addresses must use coherent geographic information.

The generator should not create arbitrary combinations of country, state, and city.

## Initial Direction

The exact address schema will be finalized when the Address generator is implemented.

At minimum, the model must support an address identifier that can be referenced by transactional entities such as orders.

---

# 6. Geography Model

## Decision

Geography is hierarchical.

The intended hierarchy is:

```text
Country
    ↓
State / Province
    ↓
City
    ↓
Customer / Address
```

## Reasoning

Geography is part of the business model and must remain internally consistent.

Invalid combinations such as:

```text
country = India
city = New York
```

must not be generated in clean data.

## Design Principle

Geographic information should come from controlled reference data rather than independent random strings.

---

# 7. Category Domain

## Purpose

Represents product categories.

Categories should support hierarchy.

Conceptual examples:

```text
Electronics
 ├── Mobile
 ├── Laptop
 ├── Television
 └── Accessories

Home
 ├── Furniture
 ├── Kitchen
 └── Decor

Fashion
 ├── Men
 ├── Women
 └── Kids
```

The exact category taxonomy is a generation-data decision and can expand later.

---

# 8. Brand Domain

## Purpose

Represents brands associated with products.

The primary relationship is:

```text
Brand
    ↑
Product
```

The number of brands must be configurable.

Brand popularity may eventually be modeled statistically if required by a later experiment.

---

# 9. Product Domain

## Purpose

Represents products sold by ShopSphere.

## Initial Attributes

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

## Relationships

Each product should reference:

```text
Category
Brand
```

## Product Characteristics

Product characteristics should correlate with category.

For example:

```text
electronics
    → generally different price/weight characteristics

accessories
    → generally different price/weight characteristics
```

The objective is realistic probabilistic behavior rather than a single global distribution applied to every product.

## Product Popularity

Product sales/activity should eventually follow a long-tail pattern:

```text
most products
    → relatively few sales

some products
    → moderate sales

very few products
    → extremely high sales
```

This is important for later controlled data-skew experiments.

---

# 10. Order Domain

## Purpose

Represents customer purchases.

## Initial Attributes

```text
order_id
customer_id
order_timestamp
order_status
shipping_address_id
payment_id
order_total
```

## Relationships

An order belongs to:

```text
Customer
```

and references:

```text
Address
Payment
```

An order contains one or more:

```text
OrderItem
```

## Order Statuses

Initial statuses:

```text
CREATED
PAID
SHIPPED
DELIVERED
CANCELLED
RETURNED
```

Status probabilities should eventually be configurable.

## Temporal Behavior

Order timestamps should follow realistic temporal patterns rather than uniform random timestamps.

The time model may incorporate:

```text
daily activity
weekly activity
seasonality
sales events
```

---

# 11. Order Item Domain

## Purpose

Represents the individual products contained in an order.

## Initial Attributes

```text
order_item_id
order_id
product_id
quantity
unit_price
discount
line_total
```

## Relationships

Each order item references:

```text
Order
Product
```

## Quantity / Item Count Behavior

Orders should contain variable numbers of items.

The intended distribution is conceptually:

```text
Most orders
    → 1–3 items

Some orders
    → 4–10 items

Small percentage
    → 10+ items
```

Every order should not have the same item count.

## Business Consistency

At minimum:

```text
quantity > 0
unit_price >= 0
discount >= 0
line_total is derived consistently
```

The exact monetary calculation rules will be finalized during implementation.

---

# 12. Payment Domain

## Purpose

Represents payment activity associated with orders.

## Initial Attributes

```text
payment_id
order_id
payment_method
payment_status
amount
payment_timestamp
```

## Payment Methods

Initial methods:

```text
CREDIT_CARD
DEBIT_CARD
UPI
NET_BANKING
WALLET
COD
```

## Relationships

Each payment references:

```text
Order
```

## Business Consistency

Payment amount should be logically related to the corresponding order amount.

Payment behavior should also be statistically configurable where useful.

---

# 13. Shipment Domain

## Purpose

Represents fulfillment of orders.

The initial relationship is:

```text
Order
    |
    v
Shipment
```

The detailed shipment schema is intentionally deferred until the shipment generator is implemented.

The domain should eventually support realistic fulfillment timing and status behavior.

---

# 14. Return Domain

## Purpose

Represents returned products/orders.

The initial relationship is:

```text
Order
    |
    v
Return
```

Return behavior should eventually be influenced by the internally modeled customer/product behavior where appropriate.

For example:

```text
customer return probability
        +
product characteristics
        ↓
probabilistic return behavior
```

The detailed return schema is intentionally deferred.

---

# 15. Session Domain

## Purpose

Represents customer application/web sessions.

## Initial Attributes

```text
session_id
customer_id
session_start
session_end
device_type
platform
```

## Relationships

A session belongs to:

```text
Customer
```

A session contains:

```text
Event
```

## Business Behavior

Session duration and activity should eventually vary according to customer activity level.

---

# 16. Event Domain

## Purpose

Represents user activity within a session.

## Initial Attributes

```text
event_id
session_id
customer_id
event_timestamp
event_type
product_id
```

## Initial Event Types

```text
SEARCH
VIEW_PRODUCT
ADD_TO_CART
REMOVE_FROM_CART
BEGIN_CHECKOUT
PURCHASE
```

## Event Relationships

Events reference:

```text
Session
Customer
Product
```

where applicable.

Not every event requires a product.

For example:

```text
SEARCH
```

may occur without a specific product reference.

## Event Sequences

The generator should support realistic probabilistic sequences such as:

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

However:

> Not every session should end in a purchase.

This is a key part of behavioral realism.

---

# 17. Referential Integrity

## Decision

Clean generated data must preserve referential integrity.

Initial expectations:

```text
Order.customer_id
    → valid Customer

Order.shipping_address_id
    → valid Address

OrderItem.order_id
    → valid Order

OrderItem.product_id
    → valid Product

Payment.order_id
    → valid Order

Session.customer_id
    → valid Customer

Event.session_id
    → valid Session

Event.customer_id
    → valid Customer

Event.product_id
    → valid Product where applicable
```

## Exception

Intentional relationship violations may be introduced only when a configured data-quality scenario explicitly requests them.

---

# 18. Business Consistency

The clean/default dataset should satisfy important business invariants.

Initial invariants include:

```text
quantity > 0
price >= 0
cost >= 0
order_total ≈ sum(order items)
payment amount ≈ order amount
session_end >= session_start
event_timestamp within session bounds
```

Additional invariants will be added as the domain becomes more complete.

---

# 19. Identity Strategy

Each major entity requires a stable identifier.

Initial identifiers:

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

Identifiers must be suitable for:

```text
joins
referential integrity
validation
statistics
reproducibility
```

The exact identifier-generation strategy is intentionally deferred to the generation-strategy design.

---

# 20. Derived Values

Where a value can be logically derived from other generated data, the generator should prefer deriving it rather than generating an unrelated random value.

Examples:

```text
OrderItem.line_total
    ← quantity
    + unit_price
    - discount

Order.order_total
    ← OrderItems

Payment.amount
    ← Order

Session.duration
    ← session_start + session_end
```

## Reasoning

Derived values preserve business consistency and make the generated dataset more realistic.

---

# 21. Domain Dependency Direction

The generation dependencies should generally follow the business relationships.

Conceptually:

```text
Reference Data
      ↓
Customer / Address
      ↓
Product
      ↓
Order
      ↓
OrderItem
      ↓
Payment / Shipment / Return

Customer
      ↓
Session
      ↓
Event
```

The exact execution order may evolve once the generation strategy is designed.

---

# 22. Model vs. Raw Tables

The internal generation model does not need to be identical to the final raw output schema.

For example, a customer may internally have:

```text
activity_level
purchase_frequency
average_order_value
preferred_category
return_probability
discount_sensitivity
```

while the raw `customers` dataset contains only the externally defined customer attributes.

## Decision

Internal behavioral state may exist solely to drive realistic generation.

It should not automatically become part of the raw dataset.

---

# 23. Domain Expansion Policy

The initial domain should remain focused.

Potential future entities may be added if they provide meaningful business or Spark-experiment value.

A new entity should be introduced only after answering:

```text
What business concept does it represent?
What relationship does it have?
What experiment or analytics use case needs it?
What new generation behavior does it require?
How will it be validated?
```

This prevents unnecessary domain expansion.

---

# 24. Data Model and Spark Experiments

The model is deliberately relational because later projects will investigate:

```text
joins
aggregations
high-cardinality keys
low-cardinality keys
data skew
large fact tables
small dimensions
file behavior
memory pressure
shuffle behavior
```

The data model should therefore provide both:

```text
realistic business relationships
```

and:

```text
controllable workload characteristics
```

without turning the business model into an artificial Spark benchmark.

---

# 25. Guiding Principle

The data model should satisfy this principle:

> Generated records should look like they came from a real e-commerce business, while their relationships and statistical characteristics remain controlled enough to support reproducible experiments.

The model should favor:

```text
meaningful relationships
+
probabilistic realism
+
referential integrity
+
derived business values
+
configurability
```

over:

```text
independent random records
+
uniform distributions
+
hard-coded relationships
```

---

# Status

**Accepted — Initial Data Model**

The entity list, core relationships, initial attributes, and business invariants defined here are the current baseline.

Future changes should be recorded explicitly, especially when they affect:

- entity relationships
- identifiers
- cardinality assumptions
- business invariants
- raw output schemas
- generation dependencies
