# Customer Domain Design

## 1. Purpose

The Customer domain represents the registered users of ShopSphere.

A generated Customer is not merely a random demographic record. It represents a commercially meaningful customer profile that can influence downstream behavior such as:

- order frequency
- purchase value
- product affinity
- session activity
- device preference
- payment preference
- return propensity
- customer lifecycle

The Customer domain is therefore one of the primary sources of behavioral correlation in the generated dataset.

The Customer generator must produce realistic customer populations while remaining:

- deterministic
- configurable
- scalable
- statistically controllable
- referentially valid
- suitable for downstream Spark performance experiments

---

## 2. Business Meaning

A Customer represents an individual account registered on ShopSphere.

The customer has:

1. identity information
2. demographic characteristics
3. account lifecycle information
4. acquisition information
5. commercial segmentation
6. behavioral preferences

The Customer record itself must remain relatively stable.

Transactional facts such as orders, payments, shipments, returns, sessions, and events belong to their respective entities.

Customer attributes should provide the characteristics from which those downstream entities can derive realistic behavior.

---

## 3. Customer Entity

The initial Customer output schema is:

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique customer identifier |
| `first_name` | String | Synthetic first name |
| `last_name` | String | Synthetic last name |
| `email` | String | Synthetic customer email |
| `phone` | String | Synthetic customer phone number |
| `gender` | String | Customer gender category |
| `date_of_birth` | Date | Customer date of birth |
| `registration_date` | Date | Date customer registered |
| `customer_status` | String | Current account lifecycle state |
| `customer_segment` | String | Commercial customer segment |
| `acquisition_channel` | String | Channel through which customer was acquired |
| `acquisition_campaign` | String | Synthetic campaign identifier |
| `preferred_device` | String | Customer's preferred device |
| `preferred_payment_method` | String | Customer's preferred payment method |

The initial implementation should not add transactional aggregates to this schema.

---

## 4. Identity

### 4.1 Customer ID

Customer IDs must be deterministic and unique.

Example format:

```text
CUSTOMER_000000001
CUSTOMER_000000002
CUSTOMER_000000003
...
```

The exact width should follow the identifier convention used across the generator.

The ID must not depend on random generation.

The same seed and generation configuration must always produce the same customer IDs.

---

### 4.2 Names

Names must be synthetic but realistic.

The generator should use curated reference data rather than constructing arbitrary character sequences.

The reference data should eventually contain:

```text
first names
last names
```

The generator should select names using deterministic random streams.

Names do not need to be unique.

Two customers may legitimately have the same first and last name.

---

### 4.3 Email

Email addresses must be synthetic.

Example:

```text
arjun.sharma.customer000001@example.com
```

The exact naming algorithm can evolve, but the following properties are required:

- syntactically valid
- deterministic
- unique in the clean baseline
- reproducible
- derived from synthetic identity information

The generator must not use real people's email addresses.

Email uniqueness is a baseline data-quality invariant.

Controlled duplicate emails may later be introduced by the Data Quality Engine.

---

### 4.4 Phone

Phone numbers must be synthetic.

The baseline dataset should contain unique phone numbers.

Phone numbers should follow the configured geographic convention.

For the initial ShopSphere dataset, the default geography is India-oriented, so synthetic Indian-format numbers are appropriate.

No generated phone number may intentionally correspond to a real person.

Phone uniqueness is a baseline data-quality invariant.

Controlled duplicate or malformed phone numbers belong to the Data Quality Engine rather than the baseline Customer generator.

---

## 5. Gender

Gender should be configurable through a weighted distribution.

Initial baseline proposal:

| Gender | Weight |
|---|---:|
| `MALE` | 50% |
| `FEMALE` | 47% |
| `OTHER` | 3% |

These values are defaults rather than hard-coded generator behavior.

The distribution must be configurable.

The generator must not assume that gender is binary.

---

## 6. Age

Age should be generated through date of birth rather than directly storing an age.

The default customer population should represent adults.

Initial age bands:

| Age Band | Approximate Weight |
|---|---:|
| 18–24 | 10% |
| 25–34 | 30% |
| 35–44 | 28% |
| 45–54 | 18% |
| 55–64 | 10% |
| 65–80 | 4% |

These values should be configurable.

The generator should sample an age band first and then generate a date of birth within that band.

This gives us control over the population distribution without storing redundant age information.

---

## 7. Generation Reference Date

Customer age must be calculated relative to a deterministic generation reference date.

The generator should not silently depend on the machine's current date.

A future configuration option should therefore provide an explicit generation reference date.

Conceptually:

```hocon
generator {
  as-of-date = "2025-12-31"
}
```

The exact configuration field can be finalized when the Customer configuration model is implemented.

This is important for reproducibility.

Running the same configuration months later must not change customer ages or lifecycle calculations.

---

## 8. Registration Date

Registration date represents when the customer joined ShopSphere.

Registration dates should be generated inside a configurable historical window.

The default population should contain more recently registered customers than extremely old accounts.

A useful baseline model is:

| Registration Age | Approximate Weight |
|---|---:|
| 0–90 days | 12% |
| 91–180 days | 15% |
| 181–365 days | 28% |
| 366–730 days | 30% |
| 731+ days | 15% |

The exact historical window should remain configurable.

Registration date must satisfy:

```text
registration_date <= as_of_date
```

and:

```text
date_of_birth < registration_date
```

for every valid customer.

---

## 9. Customer Lifecycle

Customer status represents the current account state.

Initial baseline statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
CLOSED
```

The baseline should be strongly dominated by active customers.

Proposed starting distribution:

| Status | Weight |
|---|---:|
| `ACTIVE` | 88% |
| `INACTIVE` | 8% |
| `SUSPENDED` | 2% |
| `CLOSED` | 2% |

The distribution should be configurable.

Status should not be completely independent of registration age.

For example:

- recently registered customers are more likely to be active
- long-tenured customers have a greater probability of becoming inactive or closed
- suspended accounts should remain relatively rare

The first implementation may use conditional status distributions by registration-age band.

---

## 10. Customer Segment

Customer segment is a commercial classification.

It is a latent customer characteristic that helps drive downstream behavior.

Initial segments:

```text
BUDGET
STANDARD
PREMIUM
VIP
```

Proposed baseline distribution:

| Segment | Weight |
|---|---:|
| `BUDGET` | 20% |
| `STANDARD` | 55% |
| `PREMIUM` | 20% |
| `VIP` | 5% |

These values must be configurable.

The segment should influence downstream generation.

For example:

```text
BUDGET
  ↓
lower purchase-value propensity
higher price sensitivity

STANDARD
  ↓
normal purchase behavior

PREMIUM
  ↓
higher purchase-value propensity
greater premium-product affinity

VIP
  ↓
high order propensity
high purchase-value propensity
stronger premium-product affinity
```

The Customer generator should not directly generate lifetime spending from this segment.

---

## 11. Customer Value

Customer lifetime value must NOT be stored as a generated Customer field.

For example, the Customer entity should not contain:

```text
lifetime_value
total_orders
total_spend
average_order_value
total_returns
```

These are transactional aggregates.

They should be derived later from generated Orders, OrderItems, Payments, and Returns.

The Customer segment can act as a latent input to those downstream behaviors.

This avoids circular generation logic.

The intended flow is:

```text
Customer Segment
      ↓
purchase propensity
      ↓
orders
      ↓
order items
      ↓
spending
      ↓
observed customer value
```

---

## 12. Acquisition Channel

Acquisition channel represents how the customer originally entered ShopSphere.

The baseline channel vocabulary is:

```text
ORGANIC
PAID_SEARCH
SOCIAL
EMAIL
DIRECT
REFERRAL
```

The default distribution can initially align with the existing ShopSphere channel model:

| Channel | Weight |
|---|---:|
| `ORGANIC` | 35% |
| `PAID_SEARCH` | 20% |
| `SOCIAL` | 15% |
| `EMAIL` | 10% |
| `DIRECT` | 15% |
| `REFERRAL` | 5% |

However, Customer acquisition channel and Session/Event channel are conceptually different.

They should therefore remain separate attributes.

A customer's acquisition channel is persistent.

A session's channel is an event-time behavior and may change from session to session.

Example:

```text
Customer:
acquisition_channel = PAID_SEARCH

Session 1:
channel = ORGANIC

Session 2:
channel = DIRECT

Session 3:
channel = EMAIL
```

This distinction is important for realistic behavioral data.

---

## 13. Acquisition Campaign

Campaign should be synthetic and associated with the acquisition channel.

Examples:

```text
CAMPAIGN_ORGANIC_001
CAMPAIGN_PAID_SEARCH_004
CAMPAIGN_SOCIAL_002
CAMPAIGN_EMAIL_003
```

Campaign assignment should be conditional on acquisition channel.

For example:

```text
PAID_SEARCH
    → SEARCH_BRAND
    → SEARCH_CATEGORY
    → SEARCH_GENERIC

SOCIAL
    → SOCIAL_FASHION
    → SOCIAL_TECH
    → SOCIAL_FESTIVAL
```

The initial implementation can use a small deterministic campaign reference set.

The campaign should not be a completely independent random attribute.

---

## 14. Preferred Device

Preferred device represents the device category most commonly associated with the customer.

Initial device vocabulary:

```text
MOBILE
DESKTOP
TABLET
```

The existing baseline distribution is:

| Device | Weight |
|---|---:|
| `MOBILE` | 70% |
| `DESKTOP` | 25% |
| `TABLET` | 5% |

This should become the unconditional default.

However, Customer generation should eventually support conditional distributions.

For example:

```text
younger customers
    → stronger MOBILE preference

older customers
    → relatively stronger DESKTOP preference

PREMIUM/VIP
    → potentially stronger DESKTOP/tablet usage
```

The exact correlations should be configurable rather than hard-coded.

---

## 15. Preferred Payment Method

Preferred payment method represents the customer's normal payment preference.

Initial vocabulary:

```text
UPI
CREDIT_CARD
DEBIT_CARD
NET_BANKING
WALLET
```

Default distribution:

| Payment Method | Weight |
|---|---:|
| `UPI` | 45% |
| `CREDIT_CARD` | 20% |
| `DEBIT_CARD` | 15% |
| `NET_BANKING` | 10% |
| `WALLET` | 10% |

This should be configurable.

Payment preference should be correlated with customer characteristics.

For example:

```text
PREMIUM/VIP
    → higher CREDIT_CARD probability

BUDGET
    → stronger UPI/DEBIT_CARD probability

younger customers
    → stronger UPI/WALLET probability
```

These are behavioral tendencies, not deterministic rules.

A customer's preferred payment method should not force every future order to use that method.

---

## 16. Geography Relationship

Customer geography belongs primarily to the Address domain.

The Customer entity should not duplicate the complete address hierarchy.

Avoid putting fields such as:

```text
city
state
postal_code
country
```

directly into Customer if those values are already owned by Address.

The relationship should instead be:

```text
Customer
   │
   ├── Address
   │      └── Geography
   │
   └── Orders
```

A Customer may have multiple addresses.

The existing baseline cardinality target is approximately:

```text
1.2 addresses / customer
```

For 1,000 customers this results in approximately:

```text
1,200 addresses
```

The Customer domain should therefore support:

```text
Customer 1 ─── N Address
```

A primary address relationship may be established by the Relationship Manager.

---

## 17. Primary Address

If Customer contains:

```text
primary_address_id
```

the value must reference a generated Address.

The Customer generator itself should not invent an address ID that does not exist.

Therefore primary-address assignment should happen through the relationship-generation stage after the Address population exists, unless the generation pipeline establishes an explicit dependency.

This preserves referential integrity.

---

## 18. Customer → Order Relationship

A customer can have zero or many orders.

```text
Customer 1 ─── N Order
```

Order frequency should not be assigned as a simple uniform random number.

Instead, the Order generator should use customer characteristics such as:

```text
customer_status
customer_segment
registration_date
activity_propensity
```

to influence order probability.

Example:

```text
ACTIVE + VIP
    → high order propensity

ACTIVE + STANDARD
    → normal order propensity

ACTIVE + BUDGET
    → lower/moderate order propensity

INACTIVE
    → very low order propensity

CLOSED
    → no new orders
```

The exact probability model belongs to the Order generation strategy.

Customer should provide the inputs, not generate the Orders itself.

---

## 19. Customer → Session Relationship

A customer can have many sessions.

```text
Customer 1 ─── N Session
```

Session generation should be influenced by:

```text
customer_status
customer_segment
registration_date
preferred_device
acquisition_channel
```

An ACTIVE customer should generally have a higher session probability than an INACTIVE or CLOSED customer.

Preferred device should influence, but not deterministically define, the session device.

Example:

```text
preferred_device = MOBILE

Session devices:
MOBILE
MOBILE
DESKTOP
MOBILE
TABLET
```

This creates realistic behavioral variation.

---

## 20. Customer → Event Relationship

Events belong to sessions.

The relationship is therefore conceptually:

```text
Customer
   ↓
Session
   ↓
Event
```

Customer should not directly generate event counts.

Instead:

```text
Customer characteristics
        ↓
Session frequency
        ↓
Session duration / behavior
        ↓
Event count
```

This allows realistic clustering of events around active customers.

---

## 21. Latent Behavioral Traits

The generator may internally derive latent customer traits.

Examples:

```text
activity_propensity
purchase_propensity
spend_propensity
return_propensity
discount_sensitivity
premium_affinity
```

These values do not necessarily need to appear in the output CSV.

They exist to provide a consistent behavioral basis for downstream generators.

For example:

```text
customer_segment
      ↓
latent behavioral profile
      ↓
orders / sessions / returns
```

The same customer should retain consistent behavioral tendencies across generated entities.

---

## 22. Correlation Model

Customer realism depends heavily on correlation.

The initial correlation graph should be:

```text
age
 │
 ├──────────────→ preferred_device
 │
 └──────────────→ preferred_payment_method
 │
 ↓
customer_segment
 │
 ├──────────────→ purchase_propensity
 │
 ├──────────────→ spend_propensity
 │
 └──────────────→ premium_affinity
 │
 ↓
customer_status
 │
 └──────────────→ activity_propensity
 │
 ↓
registration_date
 │
 └──────────────→ tenure
```

Acquisition also participates:

```text
acquisition_channel
       │
       ├──→ acquisition_campaign
       ├──→ preferred_device
       └──→ early activity propensity
```

The important principle is that these correlations should be generated through conditional distributions rather than hard-coded if/else rules wherever practical.

---

## 23. What Customer Must NOT Control Directly

Customer generation must not directly determine:

```text
order_count
order_item_count
total_order_value
lifetime_value
payment_count
shipment_count
return_count
session_count
event_count
```

These belong to downstream entities.

Customer only provides characteristics and behavioral inputs.

This separation prevents tight coupling between entity generators.

---

## 24. Nullability

The clean baseline should contain no unexpected nulls in mandatory fields.

Mandatory fields:

```text
id
first_name
last_name
email
phone
gender
date_of_birth
registration_date
customer_status
customer_segment
acquisition_channel
preferred_device
preferred_payment_method
```

Optional fields can be introduced later if the business model requires them.

Controlled missing values belong to the Data Quality Engine.

Example:

```text
slightly_dirty
    → low percentage of missing optional values

dirty
    → higher missingness
    → malformed values
    → duplicate identity attributes
```

The baseline Customer generator should not randomly introduce data-quality defects.

---

## 25. Data Quality Scenarios

Customer-specific quality defects may eventually include:

### Duplicate email

Two or more customers share an email address.

### Duplicate phone

Two or more customers share a phone number.

### Missing email

Email is null or empty.

### Missing phone

Phone is null or empty.

### Invalid email

Examples:

```text
name@
@domain.com
name.domain.com
```

### Invalid phone

Examples:

```text
too short
too long
invalid characters
```

### Invalid date

Examples:

```text
registration_date < date_of_birth
registration_date > as_of_date
```

### Invalid status

An unsupported status value.

These defects must be controlled through the Data Quality Engine rather than appearing accidentally.

---

## 26. Reproducibility

Customer generation must be deterministic.

Given:

```text
same configuration
+
same seed
+
same reference data
```

the generated Customer dataset must be identical.

Random streams should be derived from stable customer identifiers or indexes.

Conceptually:

```text
root seed
   │
   ├── CUSTOMER_000000001
   │       ├── demographic
   │       ├── lifecycle
   │       ├── acquisition
   │       └── preferences
   │
   ├── CUSTOMER_000000002
   │       ├── demographic
   │       ├── lifecycle
   │       ├── acquisition
   │       └── preferences
   │
   └── ...
```

Changing the generation of one customer should not unnecessarily shift the random sequence of all subsequent customers.

---

## 27. Scaling

Customer generation must support the existing cardinality model.

For example:

```text
small
    1,000 customers

medium
    configurable larger population

large
    significantly larger population

xlarge
    very large population
```

The Customer generator must operate in a streaming-friendly manner.

It must not require the complete customer population to be held in memory.

This is important for later large-scale generation.

---

## 28. Population Distribution Validation

After generation, the Customer population should be validated statistically.

Required checks include:

### Uniqueness

```text
customer_id uniqueness = 100%
```

For the clean baseline:

```text
email uniqueness = 100%
phone uniqueness = 100%
```

### Validity

Every record must satisfy:

```text
18 <= age <= 80
date_of_birth < registration_date
registration_date <= as_of_date
```

### Category coverage

All configured values should appear when population size is sufficiently large.

Examples:

```text
all genders
all customer statuses
all customer segments
all acquisition channels
all device types
all payment preferences
```

### Distribution checks

Observed distributions should be compared with configured distributions using tolerance bands.

### Correlation checks

Examples:

```text
segment × device
segment × payment
age × device
status × registration tenure
acquisition channel × campaign
```

The validator should detect obviously uniform or randomized behavior where meaningful correlation was expected.

---

## 29. Small-Population Behavior

Statistical exactness must not be expected for very small datasets.

For example, a 1,000-customer dataset should approximately follow configured distributions but should not be expected to contain exactly:

```text
500 MALE
470 FEMALE
30 OTHER
```

The generator is sampling from distributions.

Validation should therefore use tolerances.

Larger populations should converge toward configured distributions.

---

## 30. Customer Generator Responsibilities

`CustomerGenerator` should be responsible for generating:

```text
Customer identity
Customer demographics
Customer registration
Customer status
Customer segment
Customer acquisition attributes
Customer preferences
```

It should use:

```text
GenerationContext
reference data
distribution engine
customer-specific random stream
```

---

## 31. Customer Generator Non-Responsibilities

`CustomerGenerator` should NOT:

- generate Orders
- generate OrderItems
- generate Payments
- generate Shipments
- generate Returns
- generate Sessions
- generate Events
- assign arbitrary geography
- calculate lifetime value
- calculate order aggregates
- introduce uncontrolled data-quality problems
- write CSV files directly

Those responsibilities belong to other components.

---

## 32. Configuration Requirements

Customer generation should eventually have explicit configuration for:

```text
age distribution
gender distribution
registration-date distribution
customer-status distribution
customer-segment distribution
acquisition-channel distribution
acquisition-campaign distribution
device preference distribution
payment preference distribution
```

Where useful, configuration should support conditional distributions.

Example conceptual structure:

```hocon
customer {
  gender {
    MALE = 0.50
    FEMALE = 0.47
    OTHER = 0.03
  }

  segments {
    BUDGET = 0.20
    STANDARD = 0.55
    PREMIUM = 0.20
    VIP = 0.05
  }

  preferences {
    device {
      MOBILE = 0.70
      DESKTOP = 0.25
      TABLET = 0.05
    }

    payment-method {
      UPI = 0.45
      CREDIT_CARD = 0.20
      DEBIT_CARD = 0.15
      NET_BANKING = 0.10
      WALLET = 0.10
    }
  }
}
```

The final HOCON structure will be defined in the Customer configuration implementation.

---

## 33. Reference Data Requirements

Customer generation will require curated synthetic reference data for:

```text
first names
last names
```

Reference data should be external to the generator implementation.

This allows the population to be expanded without changing Scala code.

The reference data loader should validate:

- files exist
- records are non-empty
- values are non-empty
- duplicate handling is explicit

---

## 34. Testing Strategy

Customer generation requires unit tests for:

### Identity

- IDs are unique
- IDs are deterministic

### Demographics

- gender values are valid
- DOB values are valid
- age remains inside configured range

### Lifecycle

- status values are valid
- registration dates are valid
- lifecycle rules are respected

### Segmentation

- segment values are valid
- configured distributions are honored

### Acquisition

- channel values are valid
- campaigns are compatible with channels

### Preferences

- device values are valid
- payment-method values are valid

### Configuration

- invalid configuration fails
- unknown distribution values fail
- negative weights fail
- zero total weight fails

### Reproducibility

The same:

```text
seed + configuration + reference data
```

must produce identical customers.

### Independence

Changing one customer's generated values must not unnecessarily alter unrelated customers.

### Distribution

Large populations should approximately match configured distributions.

---

## 35. Integration Tests

Customer generation must eventually be tested with:

```text
Customer
   ↓
Address
   ↓
Order
   ↓
OrderItem
   ↓
Payment
   ↓
Shipment
   ↓
Return
```

and:

```text
Customer
   ↓
Session
   ↓
Event
```

The integration tests must verify referential integrity.

Examples:

```text
every Order.customer_id exists
every Session.customer_id exists
every Address.customer_id exists
```

---

## 36. Population Validation Targets

For the initial baseline population, the validator should report:

```text
record count
unique customer count
age statistics
gender distribution
status distribution
segment distribution
acquisition distribution
device distribution
payment preference distribution
registration-date distribution
```

It should also report important cross-tabs such as:

```text
segment × device
segment × payment
age-band × device
status × tenure
acquisition-channel × campaign
```

This information will later become part of the dataset manifest/statistics output.

---

## 37. Performance Requirements

The Customer generator should be simple enough to scale to millions of records.

Avoid:

```text
O(N²) customer processing
```

Avoid repeatedly scanning the entire customer population.

Prefer:

```text
O(N)
```

generation.

Reference data should be loaded once.

Configuration should be loaded once.

Random generators should be created efficiently.

Generated Customer records should be emitted incrementally.

---

## 38. Design Principle

The Customer domain should establish a realistic customer population without embedding downstream transactional facts.

The intended architecture is:

```text
                 ┌──────────────────────┐
                 │ Customer Reference   │
                 │ Data                 │
                 └──────────┬───────────┘
                            │
                            ▼
                 ┌──────────────────────┐
                 │ Customer Generator   │
                 └──────────┬───────────┘
                            │
                            ▼
                 ┌──────────────────────┐
                 │ Customer Population  │
                 └──────────┬───────────┘
                            │
            ┌───────────────┼────────────────┐
            ▼               ▼                ▼
        Addresses        Orders           Sessions
                            │                │
                            ▼                ▼
                     OrderItems           Events
                            │
                    ┌───────┼────────┐
                    ▼       ▼        ▼
                 Payment Shipment  Return
```

Customer characteristics provide behavioral inputs to downstream generators.

They do not own downstream facts.

---

## 39. Implementation Order

Customer implementation should proceed in this order:

```text
1. Customer domain design
2. Customer reference data
3. Customer configuration model
4. Customer configuration loader
5. Customer model
6. Customer generator
7. Customer generator unit tests
8. Pipeline integration
9. Population validation
10. Documentation update
```

Only one implementation file should be changed at a time.

Each step must pass the existing test suite before proceeding.

---

## 40. Definition of Done

The Customer domain is considered complete when:

- Customer records are realistic
- IDs are deterministic and unique
- synthetic identity data is valid
- demographics are configurable
- registration dates are reproducible
- lifecycle states are realistic
- customer segments are configurable
- acquisition is modeled
- device preferences are modeled
- payment preferences are modeled
- meaningful correlations exist
- no transactional aggregates are incorrectly embedded in Customer
- clean baseline data has no uncontrolled quality defects
- quality defects can later be introduced intentionally
- generation is reproducible
- generation scales linearly
- unit tests pass
- integration tests pass
- population statistics are available
- referential integrity is preserved

---

## 41. Final Domain Boundary

The Customer domain owns:

```text
WHO the customer is
WHEN they joined
WHAT commercial segment they belong to
HOW they were acquired
WHAT they generally prefer
WHAT lifecycle state their account is in
```

The Customer domain does not own:

```text
WHAT they ordered
HOW MUCH they spent
HOW they paid for a specific order
WHETHER an order shipped
WHETHER an order was returned
HOW many sessions they generated
WHAT events occurred during those sessions
```

Those facts belong to downstream domains.