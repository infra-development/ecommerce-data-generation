# ShopSphere — Customer Domain

## 1. Purpose

The Customer domain represents the people who use ShopSphere.

A Customer is not just a row containing a name, email address, and preferences. In the long-term generator design, the customer is the primary behavioral entity from which many downstream activities emerge.

A customer can:

- register on the platform
- browse products
- create sessions
- generate events
- place orders
- purchase different products
- use different payment methods
- return products
- become more or less active over time

The Customer domain is therefore one of the most important domains for sophisticated realism.

---

## 2. Business Meaning

A ShopSphere customer represents a registered user of the e-commerce platform.

The customer has two kinds of information:

### Observable customer attributes

These are fields that belong directly to the Customer record.

Examples:

- identity
- contact information
- gender
- date of birth
- registration date
- customer status
- customer segment
- acquisition channel
- acquisition campaign
- preferred device
- preferred payment method

### Latent behavioral characteristics

These describe how the customer behaves and may not need to be stored in the final customer CSV.

Examples:

- activity level
- spending tendency
- purchase frequency
- price sensitivity
- category affinity
- brand affinity
- return propensity
- channel/device affinity

The distinction is important:

```text
Customer record
       +
Behavioral profile
       |
       +-------- Orders
       +-------- Sessions
       +-------- Events
       +-------- Returns
       +-------- Product choices
```

The behavioral profile is a future core component of the realism architecture.

---

## 3. Current Customer Model

The current Customer domain model is:

```scala
case class Customer(
  id: String,
  firstName: String,
  lastName: String,
  email: String,
  phone: String,
  gender: String,
  dateOfBirth: LocalDate,
  registrationDate: LocalDate,
  customerStatus: String,
  customerSegment: String,
  acquisitionChannel: String,
  acquisitionCampaign: String,
  preferredDevice: String,
  preferredPaymentMethod: String
)
```

These fields represent the currently implemented customer record.

The model intentionally remains a simple domain object.

It does not own:

- random-number generation
- configuration parsing
- CSV writing
- downstream order generation
- session generation
- validation of the entire dataset

Those responsibilities belong elsewhere.

---

## 4. Customer Attributes — Business Interpretation

| Attribute | Business meaning |
|---|---|
| `id` | Unique ShopSphere customer identifier |
| `firstName` | Customer's first name |
| `lastName` | Customer's last name |
| `email` | Customer contact/login identifier |
| `phone` | Customer contact number |
| `gender` | Generated demographic attribute |
| `dateOfBirth` | Customer birth date |
| `registrationDate` | Date the customer joined ShopSphere |
| `customerStatus` | Current business status of the customer |
| `customerSegment` | Business segmentation classification |
| `acquisitionChannel` | Channel through which the customer was acquired |
| `acquisitionCampaign` | Campaign associated with acquisition |
| `preferredDevice` | Device the customer most commonly prefers |
| `preferredPaymentMethod` | Payment method the customer most commonly prefers |

The exact semantics of individual status and segment values are configuration-driven.

---

## 5. Customer Lifecycle

A customer should be viewed as having a lifecycle rather than being a static record.

The target conceptual lifecycle is:

```text
Acquired
    |
    v
Registered
    |
    v
New
    |
    v
Active
    |
    +----------+
    |          |
    v          v
Loyal       Dormant
    |          |
    |          v
    |       Churned
    |          |
    |          v
    +---- Reactivated
```

This is a target behavioral model, not a claim that the current generator fully implements these transitions.

The current Customer model contains `customerStatus`, but sophisticated lifecycle transition logic remains part of the realism roadmap.

---

## 6. Customer Acquisition

A customer enters ShopSphere through an acquisition channel.

The current model supports:

```text
Acquisition Channel
        |
        v
Acquisition Campaign
```

The campaign is not selected independently of the channel.

The configuration supports campaign weights nested under acquisition channels.

Conceptually:

```text
Customer
   |
   v
Acquisition Channel
   |
   +---- Campaign A
   +---- Campaign B
   +---- Campaign C
```

This is an example of a business dependency that is more realistic than globally sampling both fields independently.

---

## 7. Customer Demographics

The current generation model includes:

- age bands
- gender
- date of birth
- registration date

Age is generated through configured age bands.

Conceptually:

```text
Customer
   |
   +---- Age Band
           |
           v
       Date of Birth
```

The generator uses an `asOfDate` and configured age ranges to construct valid birth dates.

The resulting DOB must be consistent with the configured age range.

---

## 8. Registration History

Customer registration dates are generated within a configurable historical window.

Conceptually:

```text
Generation as-of date
        |
        v
Registration history window
        |
        v
Customer registration date
```

This allows datasets to represent a population acquired over time rather than assigning every customer the same registration date.

More sophisticated temporal acquisition behavior can later introduce changing acquisition rates, campaign effects, and seasonality.

---

## 9. Customer Preferences

The current customer model includes:

```text
preferredDevice
preferredPaymentMethod
```

These represent customer-level preferences rather than necessarily forcing every transaction to use the same value.

This distinction should be preserved when realism is enhanced.

For example:

```text
Customer preference
       |
       +---- likely payment method
       |
       +---- likely device
```

Future generation can allow deviations from preference while maintaining correlation.

---

## 10. Customer Relationships

Customer is a central entity.

The major relationships are:

```text
                    Customer
                       |
       +---------------+---------------+
       |               |               |
       v               v               v
    Address         Session          Order
                       |               |
                       v               +---- OrderItem
                     Event             |
                                       +---- Payment
                                       |
                                       +---- Shipment
                                       |
                                       +---- Return
```

### Customer → Address

A customer can have one or more addresses.

The current generation plan creates:

```text
1 to 2 addresses/customer
```

according to the current cardinality configuration.

### Customer → Order

A customer can place multiple orders.

This is one of the most important relationships for future realism.

The target is not a uniform number of orders per customer.

### Customer → Session

A customer can have multiple sessions.

The current implementation uses a fixed session count per customer and is therefore intentionally simplified.

### Customer → Event

Events occur within customer sessions.

The relationship is therefore:

```text
Customer → Session → Event
```

rather than treating events as independent customer records.

---

## 11. Customer as a Behavioral Source

The most important architectural decision for future realism is that the Customer domain should become a source of behavioral characteristics.

Instead of:

```text
Customer
  |
  +---- random orders
  +---- random sessions
  +---- random products
  +---- random returns
```

the target is:

```text
Customer
    |
    v
CustomerBehaviorProfile
    |
    +---- Activity
    +---- Spending
    +---- Purchase Frequency
    +---- Category Affinity
    +---- Brand Affinity
    +---- Price Sensitivity
    +---- Return Propensity
    +---- Device Affinity
    +---- Payment Preference
    |
    +----------+----------+----------+
               |          |          |
             Order     Session     Return
               |
             Product
```

This is the primary mechanism through which customer behavior becomes coherent across entities.

---

## 12. Proposed Behavioral Profile

The exact model will be designed before implementation, but a future profile may conceptually contain:

```scala
case class CustomerBehaviorProfile(
  activityLevel: Double,
  spendingLevel: Double,
  purchaseFrequency: Double,
  priceSensitivity: Double,
  returnPropensity: Double,
  categoryAffinities: Map[String, Double],
  brandAffinities: Map[String, Double],
  deviceAffinities: Map[String, Double],
  paymentAffinities: Map[String, Double]
)
```

This is a conceptual design, not yet an implementation requirement.

The profile should be treated as a latent behavioral model rather than automatically becoming another output CSV.

---

## 13. Generation Responsibilities

The Customer domain should eventually be decomposed into focused responsibilities.

Conceptually:

```text
CustomerGenerator
       |
       +---- CustomerBehaviorGenerator
       +---- CustomerIdentityGenerator
       +---- CustomerDemographicsGenerator
       +---- CustomerAcquisitionGenerator
       +---- CustomerPreferenceGenerator
       |
       v
    Customer
```

Not every component must become a separate class immediately.

The decomposition should happen when the responsibility becomes sufficiently meaningful or independently variable.

The central generator should coordinate rather than contain every low-level rule.

---

## 14. Proposed Customer Technical Structure

Target package:

```text
customer/
|
+-- model/
|    +-- Customer.scala
|    +-- CustomerBehaviorProfile.scala       (future)
|
+-- config/
|    +-- CustomerGenerationConfig.scala
|
+-- generator/
|    +-- CustomerGenerator.scala
|    +-- CustomerBehaviorGenerator.scala     (future)
|    +-- CustomerIdentityGenerator.scala     (if justified)
|    +-- CustomerAcquisitionGenerator.scala  (if justified)
|
+-- behavior/
|    +-- CustomerActivityModel.scala          (future)
|    +-- CustomerSpendingModel.scala         (future)
|    +-- CustomerLifecycleModel.scala        (future)
|    +-- CategoryAffinityModel.scala         (future)
|    +-- BrandAffinityModel.scala            (future)
|
+-- validation/
|    +-- CustomerValidator.scala              (future/domain-specific)
|
+-- statistics/
     +-- CustomerStatistics.scala             (future/domain-specific)
```

The `(future)` components are deliberately not being created during the initial architectural migration.

---

## 15. Current Configuration

The current customer configuration is:

```scala
case class CustomerAgeBand(
  minAge: Int,
  maxAge: Int,
  weight: Double
)

case class CustomerGenerationConfig(
  asOfDate: LocalDate,
  registrationHistoryDays: Int,
  ageBands: Seq[CustomerAgeBand],
  gender: Map[String, Double],
  customerStatus: Map[String, Double],
  customerSegments: Map[String, Double],
  acquisitionChannels: Map[String, Double],
  acquisitionCampaigns: Map[String, Map[String, Double]],
  preferredDevices: Map[String, Double],
  preferredPaymentMethods: Map[String, Double]
)
```

The configuration expresses generation assumptions without putting those assumptions directly into the Customer model.

---

## 16. Current Generation Flow

The current generation flow is conceptually:

```text
CustomerGenerationConfig
          |
          v
     CustomerGenerator
          |
          +---- Generate identity
          |
          +---- Generate gender
          |
          +---- Generate age / DOB
          |
          +---- Generate registration date
          |
          +---- Generate status
          |
          +---- Generate segment
          |
          +---- Generate acquisition channel
          |
          +---- Generate campaign
          |
          +---- Generate device preference
          |
          +---- Generate payment preference
          |
          v
       Customer
```

The current generator already supports weighted and constrained generation for these attributes.

---

## 17. Randomness and Reproducibility

Customer generation uses the project's deterministic random infrastructure.

Customer-specific derived random streams are used so that generation can remain reproducible while separating unrelated random decisions.

The principle is:

```text
Global Seed
    |
    +---- Customer-specific stream
              |
              +---- attribute decisions
```

The exact random-stream implementation belongs to the cross-cutting reproducibility documentation.

Customer documentation only describes how Customer generation uses that mechanism.

---

## 18. Generation Rules

The Customer generator should enforce basic input validity.

Examples include:

- customer ID must not be empty
- configured age ranges must be meaningful
- distribution weights must be valid
- acquisition campaign configuration must be consistent with acquisition channels
- generated DOB must fall within the configured age band
- generated registration date must respect the configured history window

The generator should fail clearly when required configuration or business assumptions are invalid.

---

## 19. Customer Validation

Customer-specific validation should eventually cover:

### Structural

- ID exists
- required attributes exist

### Attribute validity

- valid date of birth
- valid registration date
- valid configured values

### Business validity

- registration date is not after the generation as-of date
- age corresponds to the configured generation rules
- acquisition campaign belongs to the selected acquisition channel

### Statistical validation

At population level, we should verify intended characteristics such as:

- age distribution
- gender distribution
- segment distribution
- acquisition-channel distribution
- campaign distribution conditional on channel
- device preference distribution
- payment preference distribution

---

## 20. Realism Gaps in the Current Implementation

The current Customer implementation is functional but not yet behaviorally sophisticated.

Important gaps are:

### Independent attributes

Many customer attributes are generated from configured distributions without a deeper latent behavioral model.

### No spending profile

There is no explicit customer spending tendency driving order value.

### No purchase-frequency profile

Customers do not yet have a behavioral order-frequency model.

### No category affinity

Customer preferences do not yet strongly influence product/category selection.

### No brand affinity

Brand preference is not yet modeled as a customer-level behavioral characteristic.

### No return propensity

Return behavior is not yet strongly linked to customer behavior.

### Limited lifecycle

Customer status exists, but a full lifecycle transition model is not yet implemented.

### Limited temporal behavior

Customer behavior does not yet evolve significantly over time.

These gaps are intentional candidates for the realism phase.

---

## 21. Future Realism Design

The Customer realism roadmap is:

```text
Customer
    |
    v
Behavioral Profile
    |
    +---- Activity Model
    |
    +---- Spending Model
    |
    +---- Purchase Frequency Model
    |
    +---- Category Affinity Model
    |
    +---- Brand Affinity Model
    |
    +---- Price Sensitivity Model
    |
    +---- Return Propensity Model
    |
    +---- Lifecycle Model
    |
    +---- Temporal Behavior Model
```

Then downstream domains consume the relevant behavior:

```text
Customer Behavior
       |
       +---- Order Frequency
       +---- Product Selection
       +---- Order Value
       +---- Session Frequency
       +---- Event Activity
       +---- Return Probability
```

---

## 22. Design Patterns Applicable to Customer

Patterns should be introduced only where they solve a real problem.

### Strategy

Likely appropriate for behavior models where multiple configurable implementations are possible.

Examples:

```text
OrderFrequencyModel
CustomerActivityModel
CustomerSpendingModel
```

### Factory

Appropriate where configuration determines which behavior model implementation is constructed.

Example:

```text
CustomerBehaviorModelFactory
```

### Composition

The primary mechanism for assembling customer generation behavior.

The CustomerGenerator should compose focused components rather than inherit from a hierarchy of generators.

### Constructor Dependency Injection

Dependencies should be passed explicitly to generators and behavior components.

A framework is not required.

---

## 23. SOLID Application

### Single Responsibility

Customer generation should not also own:

- CSV output
- global validation
- configuration parsing
- order generation

Customer-specific behavior should be separated when it becomes a meaningful responsibility.

### Open/Closed

Behavior models should allow new generation strategies without rewriting stable customer orchestration.

### Liskov Substitution

Where strategy traits are introduced, each implementation must honor the same behavioral contract.

### Interface Segregation

Only create focused interfaces for actual variation points.

### Dependency Inversion

Customer generation should depend on behavior contracts where behavior is intentionally configurable or replaceable.

---

## 24. Readability Standard

The main Customer generation flow should eventually be understandable without reading every implementation detail.

Preferred conceptual style:

```scala
val profile = behaviorModel.generate(context)

val identity = identityGenerator.generate(context)
val demographics = demographicsGenerator.generate(profile, context)
val acquisition = acquisitionGenerator.generate(profile, context)
val preferences = preferenceGenerator.generate(profile, context)

customerFactory.create(
  identity = identity,
  demographics = demographics,
  acquisition = acquisition,
  preferences = preferences
)
```

The exact classes may differ after implementation design.

The important requirement is that the code communicates the business process.

---

## 25. Testing Strategy

Customer testing should exist at several levels.

### Model tests

Verify:

- equality
- field preservation
- basic domain construction

### Generator tests

Verify:

- valid generation
- deterministic generation
- different seeds produce meaningful variation
- invalid configuration fails clearly
- age-band constraints
- acquisition campaign/channel relationship
- configured weighted distributions

### Behavior-model tests

Future tests will verify:

- activity distribution
- spending distribution
- purchase frequency
- affinity generation
- lifecycle transitions

### Statistical tests

Future tests will validate population-level characteristics rather than exact individual records.

---

## 26. Example of Intended Behavioral Coherence

A future high-activity, high-spending customer might have:

```text
Activity Level       = High
Spending Level       = High
Purchase Frequency   = High
Price Sensitivity    = Low
Electronics Affinity = High
Return Propensity    = Medium
```

The downstream data should then plausibly show:

```text
many sessions
many events
many orders
higher order values
more electronics purchases
lower sensitivity to discounts
some returns
```

The point is not that every customer with these attributes must behave identically.

The point is that the attributes create **probabilistic tendencies** that remain coherent across the customer's lifecycle.

---

## 27. Current vs Target Architecture

### Current

```text
CustomerGenerator
      |
      +---- weighted distributions
      +---- customer configuration
      |
      v
Customer
```

### Target

```text
Customer Context
      |
      v
Behavior Profile
      |
      +---- Activity
      +---- Spending
      +---- Frequency
      +---- Affinity
      +---- Lifecycle
      |
      v
Customer Generation Components
      |
      v
Customer
      |
      +---- downstream behavioral influence
               |
               +---- Order
               +---- Session
               +---- Event
               +---- Product selection
               +---- Return
```

The target architecture should be introduced incrementally.

---

## 28. Migration Approach

Customer will be the first domain used to validate the new architecture.

We will not immediately create every proposed future class.

The migration sequence is:

```text
1. Review current Customer code
2. Confirm business responsibilities
3. Move Customer model into customer/model
4. Move Customer configuration into customer/config
5. Move Customer generator into customer/generator
6. Move/refactor Customer tests
7. Verify imports and dependencies
8. Run full test suite
9. Review code readability
10. Document final Customer architecture
11. Only then design the behavioral realism layer
```

Each meaningful step should leave the project in a buildable and testable state.

---

## 29. Customer Domain Quality Gate

Customer architecture is ready for the realism phase when:

- the Customer package has a clear boundary
- the Customer model is simple
- generation responsibilities are clear
- configuration is separated from domain data
- dependencies are explicit
- unnecessary abstractions are absent
- tests pass
- the generation flow is readable
- documentation matches implementation
- current limitations are explicitly known

Only after this quality gate should sophisticated Customer behavior be implemented.

---

## 30. Key Decisions

### Decision 1 — Customer is a domain, not merely a model

Customer owns its domain-specific generation and behavior.

### Decision 2 — Customer behavior is distinct from Customer data

The behavioral profile is a separate conceptual model.

### Decision 3 — Customer behavior can influence downstream domains

Orders, sessions, events, products, and returns may consume customer behavior.

### Decision 4 — CustomerGenerator coordinates rather than owns everything

Focused components can be introduced as complexity grows.

### Decision 5 — Abstractions are justified by variation

We will not create interfaces simply to satisfy SOLID mechanically.

### Decision 6 — Readability is a design requirement

The main generation flow should communicate business intent.

### Decision 7 — Sophisticated realism is incremental

The current generator is a valid baseline. The behavioral model will be layered on without destabilizing the foundation.

---

## 31. Summary

The Customer domain is the foundation for the project's future behavioral realism.

Today:

```text
Customer
  |
  +---- configured attributes
  +---- deterministic generation
  +---- basic validation
```

Target:

```text
Customer
  |
  v
Behavioral Profile
  |
  +---- Activity
  +---- Spending
  +---- Frequency
  +---- Affinity
  +---- Lifecycle
  +---- Return Propensity
  |
  +----------+----------+----------+
             |          |          |
           Orders    Sessions    Returns
             |
          Products
```

The immediate objective is **not** to implement the target model yet.

The immediate objective is to establish a clean Customer domain boundary and a readable technical structure. That becomes the reference architecture for the remaining entity migrations and the subsequent realism phase.
