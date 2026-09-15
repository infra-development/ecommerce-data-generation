# ShopSphere Data Generator — Architecture & Design Principles

## 1. Purpose

This document defines the architectural and design principles for ShopSphere Data Generator.

The objective is not simply to organize files. The objective is to build a system in which:

- business concepts are visible in the code
- responsibilities are clear
- dependencies are understandable
- changes are localized
- behavior can be tested independently
- configuration can introduce meaningful variation
- sophisticated realism can be added without turning generators into large, difficult-to-read classes

The architecture should support the project as it evolves from a basic synthetic data generator into a configurable e-commerce simulation platform.

---

## 2. Primary Architectural Goal

The primary goal is:

> **A developer should be able to understand what the generator does by reading the domain structure and the main generation flow, without having to reverse-engineer implementation details.**

The code should communicate business intent.

For example, this is preferable:

```scala
val profile = behaviorModel.generate(customerContext)
val identity = identityGenerator.generate(customerContext)
val acquisition = acquisitionGenerator.generate(profile, customerContext)
val preferences = preferenceGenerator.generate(profile, customerContext)

customerFactory.create(
  identity = identity,
  acquisition = acquisition,
  preferences = preferences
)
```

over a large method containing unrelated random sampling, business rules, and object construction.

The exact implementation will evolve, but the principle remains: **make intent visible.**

---

## 3. Architecture Style

ShopSphere will use a **domain-oriented architecture with explicit separation of domain behavior, application orchestration, and shared infrastructure**.

The architecture is not intended to be a strict implementation of a heavyweight framework.

Conceptually:

```text
                    ShopSphere Generator
                            |
              +-------------+-------------+
              |                           |
          Application                  Domains
          / System                       |
              |             +-------------+-------------+
              |             |             |             |
         Generation      Customer      Product        Order
         Pipeline        Address       Payment       Shipment
              |          Session       Return        Event
              |          Category      Brand
              |
         Cross-Cutting
         Infrastructure
```

The important boundary is between:

1. **Domain responsibilities**
2. **System/application responsibilities**
3. **Shared technical infrastructure**

---

## 4. Domain-Oriented Package Structure

The target package structure is:

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

Entity packages are allowed to have different internal structures.

For example:

```text
customer/
    config/
    model/
    generator/
    behavior/
    validation/
    statistics/
```

while:

```text
address/
    model/
    generator/
    validation/
```

The architecture should reflect actual responsibilities rather than forcing every domain into identical folders.

---

## 5. Domain Ownership

A domain owns the concepts and behavior that are primarily about that domain.

For example, Customer owns:

- Customer domain model
- customer-specific configuration
- customer generation
- customer behavior models
- customer-specific validation
- customer-specific statistics

Product owns:

- Product model
- product generation
- product pricing behavior
- product popularity behavior
- product-specific validation

Order owns:

- Order model
- order generation
- order-specific business behavior
- order-specific validation

This gives each domain a clear home.

---

## 6. Cross-Cutting Responsibilities

Not every concept belongs to an entity.

Shared mechanisms belong in common or system-level packages.

Examples:

```text
common/
    distribution/
    random/
    time/
    util/
```

System orchestration belongs in:

```text
generation/
relationship/
quality/
scenario/
output/
manifest/
observability/
```

Global validation and statistics can remain system-level while domain-specific validation/statistics live within domains.

The rule is:

> **Domain behavior belongs to the domain. Shared mechanisms belong to common infrastructure. System orchestration belongs to the application/system layer.**

---

## 7. Dependency Direction

Dependencies should generally flow toward stable domain concepts and shared abstractions.

A domain should not reach into another domain's internal implementation merely to use its behavior.

Prefer:

```text
Order
  |
  +---- Customer model
  +---- Product model
```

over:

```text
Order
  |
  +---- CustomerGenerator internals
  +---- ProductGenerator internals
```

The generator responsible for an entity should not become the public API of that entity.

This keeps domains independent and makes future refactoring safer.

---

## 8. Entity Model vs. Generation Logic

Domain models represent business data.

For example:

```scala
case class Customer(
  id: String,
  firstName: String,
  lastName: String,
  ...
)
```

The model should not know:

- how random numbers are produced
- how configuration is loaded
- how CSV files are written
- how another entity is generated
- how the complete generation pipeline is orchestrated

Generation logic belongs in generators and domain behavior components.

This keeps domain data structures simple and readable.

---

## 9. Generators

A generator is responsible for creating instances of a domain object according to the configured business model.

A generator should coordinate behavior rather than contain every detail itself.

Conceptually:

```text
CustomerGenerator
       |
       +---- Identity
       +---- Demographics
       +---- Acquisition
       +---- Preferences
       +---- Behavior Profile
       |
       v
    Customer
```

For a more complex domain:

```text
OrderGenerator
       |
       +---- Order Frequency
       +---- Product Selection
       +---- Item Quantity
       +---- Pricing
       +---- Discounts
       |
       v
     Order
```

A generator should remain readable enough that its main method communicates the generation process.

---

## 10. Behavioral Models

Sophisticated realism requires business behavior to be represented explicitly.

A behavioral model represents a business assumption or a probabilistic rule.

Examples:

```text
CustomerActivityModel
CustomerSpendingModel
CustomerLifecycleModel
CategoryAffinityModel
BrandAffinityModel
OrderFrequencyModel
ProductPopularityModel
ReturnProbabilityModel
```

These models are distinct from the generated entity itself.

For example:

```text
Customer
   +
CustomerBehaviorProfile
   |
   +---- Orders
   +---- Sessions
   +---- Events
   +---- Returns
```

This allows one underlying customer behavior to influence multiple downstream entities.

---

## 11. Composition Over Large Generators

We prefer composition over creating large generator classes.

Instead of:

```text
CustomerGenerator
    500 lines
```

with every rule embedded inside it, prefer:

```text
CustomerGenerator
    |
    +-- CustomerBehaviorGenerator
    +-- CustomerIdentityGenerator
    +-- CustomerAcquisitionGenerator
    +-- CustomerPreferenceGenerator
```

Only introduce a separate component when it owns a meaningful responsibility.

The goal is not to maximize the number of classes.

The goal is to keep each responsibility understandable.

---

## 12. Strategy Pattern

The Strategy pattern is appropriate where the business model has meaningful interchangeable behavior.

Examples:

```scala
trait OrderFrequencyModel {
  def ordersFor(
    customer: CustomerBehaviorProfile,
    context: GenerationContext
  ): Int
}
```

Possible implementations:

```text
FixedOrderFrequency
PoissonOrderFrequency
NegativeBinomialOrderFrequency
LongTailOrderFrequency
```

This allows configuration to select different business assumptions without rewriting the order generation process.

The same principle may apply to:

- product popularity
- item count
- customer activity
- pricing
- return probability
- lifecycle transitions

Strategy should be used when there is genuine variation—not merely because the pattern exists.

---

## 13. Factory Pattern

Factories are useful when configuration determines which implementation should be created.

Conceptually:

```text
Configuration
      |
      v
Behavior Model Factory
      |
      +---- Strategy A
      +---- Strategy B
      +---- Strategy C
```

For example:

```scala
OrderFrequencyModelFactory.create(config)
```

The factory should remain small and focused.

Avoid spreading configuration-specific `if/else` or `match` logic throughout generators.

---

## 14. Dependency Injection

Dependencies should be explicit wherever that improves clarity, testing, or substitution.

Prefer:

```scala
class CustomerGenerator(
  behaviorGenerator: CustomerBehaviorGenerator,
  identityGenerator: CustomerIdentityGenerator
)
```

over constructing all dependencies internally.

This makes the class's requirements visible.

It also allows tests to provide controlled implementations or deterministic components.

A dependency-injection framework is not required. Constructor injection is sufficient for the current project.

---

## 15. Interfaces and Traits

Traits and interfaces should be introduced only when they provide a real benefit.

Good reasons include:

- interchangeable strategies
- meaningful testing boundaries
- multiple implementations
- stable dependency boundaries
- extension points

Avoid creating an interface for every concrete class.

For example, this is unnecessary unless there is a reason:

```text
ICustomerGenerator
IEmailGenerator
IPhoneGenerator
INameGenerator
```

If there is only one implementation and no meaningful boundary, a concrete class is usually clearer.

---

## 16. SOLID Principles

SOLID is a design guide, not a requirement to create abstractions everywhere.

### 16.1 Single Responsibility Principle

A class should have one coherent reason to change.

Bad:

```text
CustomerGenerator
    |
    +-- customer creation
    +-- CSV writing
    +-- configuration parsing
    +-- statistics
    +-- validation
```

Better:

```text
CustomerGenerator
CustomerValidator
CustomerStatistics
ConfigLoader
CsvOutputWriter
```

The responsibilities remain separate.

---

### 16.2 Open/Closed Principle

The system should be open to new behavior without requiring modification of stable orchestration code.

For example:

```text
OrderFrequencyModel
       |
       +-- Poisson
       +-- Negative Binomial
       +-- Long Tail
```

Adding a new strategy should not require rewriting `OrderGenerator`.

---

### 16.3 Liskov Substitution Principle

When multiple implementations exist for a trait, each implementation must preserve the contract expected by the caller.

For example, every `OrderFrequencyModel` must return a valid order count according to the model's contract.

We will not use inheritance hierarchies where substitution does not make business sense.

---

### 16.4 Interface Segregation Principle

Interfaces should expose only the behavior clients actually need.

Prefer small, focused contracts such as:

```scala
trait ProductSelectionModel {
  def selectProduct(...): Product
}
```

over a large interface containing unrelated generation operations.

---

### 16.5 Dependency Inversion Principle

High-level generation logic should depend on stable contracts where variation is expected.

For example:

```text
OrderGenerator
      |
      v
OrderFrequencyModel
```

rather than hard-coding one particular mathematical distribution inside the generator.

Concrete implementations are selected at the composition/configuration boundary.

---

## 17. Readability as an Architectural Requirement

Readability is a first-class requirement.

A developer should be able to read:

```scala
val profile = behaviorModel.generate(context)
val items = itemGenerator.generate(orderContext, profile)
val payment = paymentGenerator.generate(order, profile)
```

and understand the business flow.

Avoid code where the main business behavior is hidden inside:

- deeply nested expressions
- generic helper methods
- meaningless utility classes
- excessive abstraction
- long methods
- cryptic variable names
- repeated low-level random operations

The code should favor **named concepts over clever expressions**.

---

## 18. Naming Principles

Names should describe either:

1. a business concept, or
2. a clear technical responsibility.

Prefer:

```text
CustomerBehaviorProfile
CustomerActivityModel
OrderFrequencyModel
ProductPopularityModel
ReturnProbabilityModel
```

over:

```text
Helper
Manager
Processor
Util
CommonGenerator
DataHelper
```

A class name should make its purpose understandable without opening the implementation.

---

## 19. Business Logic vs. Infrastructure

Business logic should not depend directly on file-format details.

For example:

```text
CustomerGenerator
       |
       v
Customer
```

not:

```text
CustomerGenerator
       |
       v
CSV row
```

Output should be a separate concern:

```text
Domain Objects
      |
      v
Output Writer
      |
      v
CSV
```

This allows future output formats without rewriting domain generation.

---

## 20. Randomness as Infrastructure

Randomness is a shared technical mechanism.

Business components should express intent:

```text
sample customer activity
sample order frequency
select product
```

rather than repeatedly implementing low-level random-number handling.

The random infrastructure provides:

- seed control
- derived random streams
- reproducibility
- deterministic testing

Business behavior determines how randomness is used.

---

## 21. Distribution Models as Infrastructure + Business Configuration

The mathematical mechanisms are reusable infrastructure.

Examples:

- uniform
- weighted
- normal/log-normal
- Poisson
- negative binomial
- Pareto/Zipf-like
- bounded/truncated distributions

The business domain decides where and why they are used.

For example:

```text
ProductPopularityModel
        |
        v
Zipf-like distribution
```

The distribution mechanism is generic; product popularity is business behavior.

---

## 22. Relationship Modeling

Relationships are not merely foreign keys.

There are two separate concerns.

### Structural relationship

```text
Order.customerId
OrderItem.orderId
OrderItem.productId
```

### Behavioral relationship

```text
How many orders does this customer make?
Which products does this customer prefer?
How likely is this order to be returned?
```

Structural relationships belong to the domain model.

Behavioral relationships belong to domain behavior and generation logic.

This distinction is central to sophisticated realism.

---

## 23. Domain Services

Some behavior will not naturally belong to a single entity.

A domain service can be introduced when a business operation spans multiple domain concepts.

Potential examples:

```text
PurchaseBehaviorService
ProductSelectionService
OrderPricingService
ReturnDecisionService
CustomerLifecycleService
```

A service should represent a meaningful business responsibility.

Avoid using `Service` as a generic name for any class containing logic.

---

## 24. Application/System Orchestration

The generation pipeline is responsible for coordinating the overall process.

Conceptually:

```text
Configuration
    |
    v
Generation Plan
    |
    v
Generation Context
    |
    v
Reference Data
    |
    v
Domain Generators
    |
    v
Quality / Scenarios
    |
    v
Validation
    |
    v
Statistics / Manifest
    |
    v
Output
```

The pipeline should coordinate domains, not duplicate their business logic.

---

## 25. Configuration Boundary

Configuration is an input to the system.

It should define business assumptions such as:

- scale
- cardinality
- distributions
- pricing
- customer generation
- scenarios
- quality profiles

Configuration parsing belongs to the configuration layer.

Domain classes should receive already-understood configuration rather than parsing HOCON themselves.

---

## 26. Validation Boundary

Validation exists at two levels.

### Domain validation

Examples:

```text
CustomerValidator
ProductValidator
OrderValidator
AddressValidator
```

These understand domain rules.

### System validation

Examples:

```text
Primary-key validation
Foreign-key validation
Output schema validation
Global business-rule validation
```

These understand the generated dataset as a whole.

The distinction prevents domain classes from becoming responsible for the entire dataset.

---

## 27. Testing Architecture

Tests should exist at multiple levels.

### Unit tests

Test individual domain behavior:

```text
CustomerBehaviorProfileTest
OrderFrequencyModelTest
ProductPopularityModelTest
```

### Generator tests

Test domain generation:

```text
CustomerGeneratorTest
ProductGeneratorTest
OrderGeneratorTest
```

### Integration tests

Test relationships:

```text
CustomerOrderIntegrationTest
OrderPaymentIntegrationTest
SessionEventIntegrationTest
```

### Statistical tests

Test distribution characteristics:

```text
CustomerBehaviorDistributionTest
ProductPopularityDistributionTest
```

### End-to-end tests

Test:

```text
configuration
    -> generation
    -> output
    -> validation
```

Tests should validate both correctness and intended business behavior.

---

## 28. Avoiding Over-Engineering

The architecture must remain proportional to the problem.

We explicitly avoid:

- interfaces with only one trivial implementation
- unnecessary inheritance
- deep class hierarchies
- design patterns used only for appearance
- dependency-injection frameworks without a need
- generic `Manager`/`Processor`/`Helper` classes
- excessive indirection
- abstractions that hide simple business logic

The preferred rule is:

> **Use the simplest design that clearly expresses the business responsibility and leaves the necessary variation points open.**

---

## 29. Domain as the Reference Point

Customer will be the first domain we design using these principles.

Customer is particularly important because sophisticated realism will eventually make Customer behavior influence:

```text
Customer
   |
   +---- Orders
   |
   +---- Sessions
   |
   +---- Events
   |
   +---- Returns
   |
   +---- Product/category affinity
```

The Customer domain will therefore become our reference example for:

- behavioral modeling
- strategy selection
- dependency boundaries
- domain composition
- testing
- documentation
- readability

We should not blindly copy the Customer structure to every other domain. We should reuse principles where they make sense.

---

## 30. Refactoring Rule

Package migration is not merely a file-moving exercise.

For each domain we will ask:

1. What does this domain mean to the business?
2. What responsibilities belong to it?
3. What are its dependencies?
4. Which code is actually domain-specific?
5. Which code is cross-cutting infrastructure?
6. Where is behavior expected to vary?
7. Which abstractions are justified?
8. Can the main generation flow be understood by reading it?
9. Can the behavior be tested independently?
10. Does the implementation reflect the documented business model?

Only after answering these questions should code be moved or redesigned.

---

## 31. Architectural Quality Gate

A domain migration is considered complete only when:

- the package boundary is clear
- responsibilities are separated
- dependencies are explicit
- unnecessary abstractions have been avoided
- tests pass
- generated output remains valid
- the main generation flow is readable
- documentation explains both business and technical behavior

The migration is therefore complete only when **architecture, code, tests, and documentation agree**.

---

## 32. Final Design Principle

The architecture should make the following chain visible:

```text
Business Concept
      |
      v
Business Behavior
      |
      v
Domain Model
      |
      v
Behavior Model / Strategy
      |
      v
Generator
      |
      v
Generated Entity
      |
      v
Relationships
      |
      v
Validation / Statistics
      |
      v
Output
```

The ultimate standard for the project is:

> **A person should be able to understand the business behavior from the documentation, understand the technical design from the package structure, and understand the implementation by reading the code.**

That is the architecture we will build toward.
