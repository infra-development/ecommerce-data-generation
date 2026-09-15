# 28 — Code Organization, Naming & Readability Standards

## 1. Purpose

The ShopSphere architecture is intentionally domain-oriented.

That architecture will only remain useful if the code communicates the same business model clearly.

A technically correct codebase can still become difficult to maintain when:

- names are vague;
- responsibilities are hidden;
- packages do not match domain ownership;
- generators become too large;
- configuration details leak everywhere;
- business rules are buried inside utility methods;
- abstractions exist without a real purpose.

This document defines coding standards for ShopSphere so that the implementation remains readable as realism, scale, scenarios, and architectural sophistication increase.

The central principle is:

> **Code should make the business model easier to understand, not force the reader to reconstruct it from implementation details.**

---

# 2. Readability Is an Architectural Requirement

Readability is not cosmetic.

For this project, readable code directly affects:

```text
correctness
testability
refactoring safety
onboarding
performance optimization
business realism
```

A developer should be able to inspect a generator and understand:

```text
what business decision is being made
what data it depends on
what behavior model controls it
what relationships it creates
```

without tracing through a large collection of generic helpers.

---

# 3. Business Language First

Use the language of ShopSphere in code.

Prefer:

```text
Customer
Order
OrderItem
Product
Shipment
Return
Session
Event
```

over generic terms such as:

```text
Record
EntityData
ItemData
TransactionObject
```

when the business meaning is known.

---

# 4. Package Organization

The target architecture is domain-oriented.

Conceptually:

```text
com.shopsphere.datagenerator/
│
├── customer/
│   ├── config/
│   ├── model/
│   ├── generator/
│   ├── behavior/
│   ├── validation/
│   └── statistics/
│
├── address/
│   ├── model/
│   ├── generator/
│   └── validation/
│
├── product/
├── category/
├── brand/
├── order/
├── orderitem/
├── payment/
├── shipment/
├── return/
├── session/
├── event/
│
├── geography/
│   ├── model/
│   ├── reference/
│   └── loader/
│
├── common/
├── config/
├── generation/
├── relationship/
├── quality/
├── scenario/
├── validation/
├── output/
├── manifest/
├── statistics/
└── observability/
```

Only create subpackages that represent actual responsibilities.

---

# 5. Domain Ownership

Each domain package should answer:

> What business concept does this package own?

For example:

```text
customer/
```

owns customer meaning and customer-specific behavior.

It should not become the home for:

```text
generic random utilities
CSV writing
global configuration
```

---

# 6. Cross-Cutting Packages

Cross-cutting packages should contain capabilities used across multiple domains.

Examples:

```text
common/
config/
validation/
output/
statistics/
observability/
```

These packages should not become dumping grounds.

---

# 7. The `common` Package

`common` should remain small.

Good candidates:

```text
generic deterministic random primitives
small shared value utilities
generic collection helpers
```

Bad candidates:

```text
CustomerHelper
OrderHelper
ProductManager
BusinessUtils
```

Domain-specific logic belongs to the domain.

---

# 8. Avoid the `util` Dump

A package named:

```text
util/
```

often becomes a place where unclear responsibilities accumulate.

Before creating a utility, ask:

> Which domain or cross-cutting responsibility actually owns this behavior?

If the answer is:

```text
customer
```

put it in:

```text
customer/
```

---

# 9. File Naming

File names should describe the primary type or responsibility.

Good:

```text
Customer.scala
CustomerGenerator.scala
CustomerBehaviorProfile.scala
CustomerBehaviorModel.scala
OrderGenerator.scala
ReturnEligibilityService.scala
```

Avoid:

```text
CustomerStuff.scala
Helpers.scala
Misc.scala
Manager.scala
Processor.scala
Logic.scala
```

---

# 10. One Primary Responsibility Per File

A file may contain closely related types where Scala style makes that reasonable.

But avoid large files containing unrelated domain concepts.

For example, this is undesirable:

```text
EcommerceModels.scala
```

containing:

```text
Customer
Product
Order
Payment
Shipment
Return
```

Domain files should remain discoverable.

---

# 11. Model Naming

Domain models should use nouns.

Examples:

```scala
case class Customer(...)
case class Product(...)
case class Order(...)
case class Address(...)
```

Avoid:

```scala
CustomerData
CustomerRecord
CustomerObject
```

unless the distinction is genuinely meaningful.

---

# 12. Behavior Model Naming

Behavior models should communicate the behavior they own.

Good:

```text
CustomerActivityModel
CustomerSpendingModel
OrderFrequencyModel
ProductPopularityModel
ReturnProbabilityModel
SessionDurationModel
```

Bad:

```text
CustomerStrategy
BusinessStrategy
GenericModel
BehaviorHelper
```

unless those names genuinely represent a defined abstraction.

---

# 13. Generator Naming

Use:

```text
<Domain>Generator
```

when the component creates domain records.

Examples:

```text
CustomerGenerator
ProductGenerator
OrderGenerator
OrderItemGenerator
PaymentGenerator
ShipmentGenerator
ReturnGenerator
SessionGenerator
EventGenerator
```

---

# 14. Service Naming

Use domain-specific business language.

Good:

```text
ReturnEligibilityService
ShipmentSchedulingService
OrderItemAllocationService
SessionJourneyService
```

Bad:

```text
RelationshipManager
DataManager
ProcessingService
BusinessService
```

The name should tell the reader what business operation occurs.

---

# 15. Resolver Naming

Resolvers are appropriate when the responsibility is actually resolution.

Examples:

```text
CatalogResolver
GeographyResolver
ProductResolver
```

Avoid calling every lookup component a resolver.

---

# 16. Selector Naming

Use `Selector` when the component chooses among alternatives.

Examples:

```text
ProductSelector
PaymentMethodSelector
CategorySelector
BrandSelector
```

This communicates a different responsibility from:

```text
Generator
```

---

# 17. Sampler Naming

Use `Sampler` when the component samples from a distribution.

Examples:

```text
ProductPopularitySampler
SessionDurationSampler
OrderFrequencySampler
```

If the object contains substantial business behavior, a domain-oriented `Model` may be clearer.

---

# 18. Strategy Naming

A Strategy should represent a meaningful alternative behavior.

Good:

```text
UniformProductPopularityModel
ZipfProductPopularityModel
CampaignAwareProductPopularityModel
```

if those are genuine alternatives.

Avoid:

```text
DefaultStrategy
StrategyImpl
StrategyV2
```

without semantic meaning.

---

# 19. Factory Naming

Factories should communicate what they construct.

Good:

```text
DistributionFactory
OutputWriterFactory
BehaviorModelFactory
ReferenceDataLoaderFactory
```

Avoid:

```text
FactoryManager
UniversalFactory
ObjectFactory
```

unless the scope is genuinely that broad.

---

# 20. Configuration Naming

Configuration names should correspond to business concepts.

Good:

```hocon
customer-generation
product-distribution
product-pricing
scenario
cardinality
```

Avoid vague names:

```hocon
settings
misc
options
params
```

for domain-specific behavior.

---

# 21. Units Must Be Explicit

Numeric configuration should make units obvious.

Prefer:

```hocon
session-duration-minutes = 5
shipment-delay-days = 3
```

over:

```hocon
session-duration = 5
shipment-delay = 3
```

where the unit is not obvious.

---

# 22. Probability Naming

Probabilities should be clearly recognizable.

Examples:

```hocon
return-probability = 0.08
address-generation-probability = 0.20
```

Avoid ambiguous:

```hocon
return-rate = 0.08
```

unless the term `rate` has an intentionally defined statistical meaning.

---

# 23. Counts vs Rates

Distinguish:

```text
count
probability
rate
ratio
multiplier
```

These are not interchangeable.

Example:

```text
customer-count
return-probability
orders-per-day-rate
campaign-traffic-multiplier
```

---

# 24. Method Naming

Methods should describe actions.

Good:

```scala
generateCustomer()
selectProduct()
calculateOrderTotal()
resolveBuilding()
validateReferenceData()
writeOrders()
```

Avoid:

```scala
doIt()
process()
handle()
run()
execute()
```

unless the context makes the meaning unambiguous.

---

# 25. Boolean Naming

Boolean values should read naturally.

Good:

```scala
isValid
isActive
hasReturned
isWithinWindow
shouldGenerate
```

Avoid:

```scala
validFlag
activeValue
returnBoolean
```

---

# 26. Option Naming

If an `Option` represents presence/absence, name it accordingly.

Good:

```scala
product: Option[Product]
preferredBrand: Option[Brand]
deliveryTimestamp: Option[Instant]
```

Avoid encoding optionality in names:

```scala
maybeProductValue
nullableProduct
```

unless that meaning is particularly useful.

---

# 27. ID Naming

Use the domain name.

Good:

```scala
customerId
orderId
productId
buildingId
sessionId
eventId
```

Avoid:

```scala
id1
id2
parentId
foreignId
```

unless the relationship is genuinely generic.

---

# 28. Avoid Ambiguous `id`

Inside a domain model:

```scala
id
```

is often fine.

Across relationships:

```scala
customerId
productId
orderId
```

is much clearer.

---

# 29. Relationship Naming

Use explicit relationship semantics.

Good:

```scala
customerId
categoryId
brandId
buildingId
postalCode
```

Avoid:

```scala
parent
reference
owner
source
target
```

unless the relationship is intentionally generic.

---

# 30. Collection Naming

Plural names should represent collections.

Good:

```scala
customers
orders
products
events
buildings
postalCodes
```

Avoid:

```scala
customerList
orderData
productCollection
```

unless the concrete collection type matters to the API.

---

# 31. Iterator Naming

If a collection is intentionally lazy, the name can communicate it where useful.

For example:

```scala
customerIterator
```

may be appropriate at an application boundary.

Within a focused method, simply:

```scala
customers
```

may be clearer if the type already communicates laziness.

---

# 32. Avoid Hungarian Notation

Do not encode types into variable names.

Avoid:

```text
customerSeq
orderMap
productOpt
```

unless the distinction materially improves clarity.

The Scala type system already communicates this.

---

# 33. Type Inference

Scala's type inference can improve readability.

Prefer:

```scala
val customer = customerGenerator.generate(...)
```

when the type is obvious.

Use explicit types when they improve:

```text
API clarity
documentation
complex expressions
public contracts
```

---

# 34. Do Not Overuse Type Inference

This:

```scala
val x = f(a, b, c)
```

may be less readable than:

```scala
val customerProfile: CustomerBehaviorProfile =
  behaviorModel.generate(...)
```

when the type carries important business meaning.

Use judgment.

---

# 35. Case Classes for Domain Data

Scala case classes are the preferred default for simple immutable domain data.

Example:

```scala
case class Address(
  id: String,
  customerId: String,
  buildingId: String,
  unitNumber: String,
  postalCode: String
)
```

This matches the current Address design.

---

# 36. Do Not Add Fields for Convenience

Do not add fields to a domain model merely because another component would find them convenient.

For example, the current Address model intentionally does not duplicate:

```text
city
state
country
addressLine1
addressLine2
addressType
isPrimary
```

The geography hierarchy resolves location semantics.

Model changes should be business decisions.

---

# 37. Value Objects

Value objects may be introduced when a concept has meaningful invariants.

Potential future examples:

```text
Money
PostalCode
CustomerId
OrderId
TimeWindow
```

Do not wrap every `String` in a separate type without a business reason.

---

# 38. Domain Invariants

Invariants should live close to the concept they protect where practical.

For example:

```text
OrderItem quantity > 0
```

should not depend entirely on a distant global validator if the domain object can safely enforce it.

However, global cross-record invariants belong in validation.

---

# 39. Local vs Global Rules

### Local rule

```text
price > 0
```

### Relationship rule

```text
orderItem.orderId exists
```

### Global rule

```text
every generated order has at least one item
```

Keep each rule at the appropriate level.

---

# 40. Method Size

Methods should be small enough that the reader can understand their purpose without excessive scrolling.

Do not use arbitrary line limits.

Instead ask:

> Can the method be described by one clear business responsibility?

---

# 41. Large Generators

A generator becoming 300–500 lines is a warning sign.

It often means the generator is doing too many things:

```text
behavior
identity
relationships
pricing
timing
validation
statistics
```

Extract focused components based on business responsibility.

---

# 42. Customer Generator

The desired customer generation flow should eventually resemble:

```scala
val profile = behaviorModel.generate(context)
val identity = identityGenerator.generate(context)
val acquisition = acquisitionGenerator.generate(profile, context)
val preferences = preferenceGenerator.generate(profile, context)

customerFactory.create(
  identity = identity,
  acquisition = acquisition,
  preferences = preferences
)
```

The exact implementation may differ.

The important point is compositional business flow.

---

# 43. Order Generator

A readable conceptual flow:

```scala
val customer = customerSelector.select(context)
val timing = orderTimingModel.sample(customer, context)
val frequency = orderFrequencyModel.sample(customer, context)

orderFactory.create(
  customer = customer,
  timing = timing,
  ...
)
```

This is easier to understand than a single method containing every probability and timestamp calculation.

---

# 44. Avoid Boolean Parameter Explosion

Avoid APIs such as:

```scala
generate(
  customer,
  true,
  false,
  true,
  false,
  true
)
```

The meaning is invisible.

Prefer:

```scala
generate(
  customer = customer,
  scenario = scenario,
  temporalContext = context
)
```

or a meaningful typed configuration object.

---

# 45. Avoid Parameter Explosion

The opposite problem is:

```scala
generate(
  a,
  b,
  c,
  d,
  e,
  f,
  g,
  h,
  i,
  j,
  k
)
```

When many parameters are genuinely related, use a meaningful domain object.

Example:

```scala
OrderGenerationContext
```

if it represents a real concept.

Do not create a parameter object merely to hide poor design.

---

# 46. Parameter Objects

Good parameter object:

```text
TemporalContext
CustomerBehaviorProfile
GenerationRequest
GenerationPlan
```

These represent actual domain/application concepts.

Bad parameter object:

```text
EverythingNeededForOrderGenerator
```

with unrelated services and infrastructure.

---

# 47. Dependency Visibility

A class should make its important dependencies visible.

Prefer:

```scala
final class OrderGenerator(
  frequencyModel: OrderFrequencyModel,
  productSelector: ProductSelector,
  temporalModel: TemporalModel
)
```

rather than retrieving dependencies from a global registry.

---

# 48. Avoid Service Locators

Avoid patterns such as:

```scala
context.get[ProductSelector]
context.get[OrderFrequencyModel]
context.get[RandomGenerator]
```

throughout business code.

This hides dependencies and weakens compile-time guidance.

---

# 49. Dependency Injection

Use:

```text
constructor injection
method parameters
```

as the default.

No dependency-injection framework is required.

---

# 50. Interfaces and Traits

Introduce an interface when there is a real abstraction boundary.

Examples:

```scala
trait ProductPopularityModel
trait OutputWriter
trait ReferenceDataLoader
```

when multiple implementations or testing substitutions are useful.

---

# 51. Avoid Interface Explosion

Do not create:

```text
CustomerGeneratorInterface
CustomerValidatorInterface
CustomerFactoryInterface
CustomerStatisticsInterface
```

unless callers actually need substitution.

Concrete classes are perfectly acceptable.

---

# 52. Abstract Classes

Abstract classes should be uncommon.

Prefer:

```text
composition
traits for focused contracts
concrete implementations
```

rather than inheritance-heavy hierarchies.

---

# 53. Inheritance

Inheritance should represent a true:

```text
is-a
```

relationship.

Do not use inheritance merely to share helper methods.

Composition is usually clearer.

---

# 54. Composition

Prefer:

```text
OrderGenerator
  + OrderFrequencyModel
  + ProductSelector
  + TemporalModel
  + PricingModel
```

over:

```text
BaseGenerator
  ↓
AdvancedGenerator
  ↓
RealisticGenerator
  ↓
ScenarioGenerator
```

---

# 55. Strategy Pattern

Strategy is useful when behavior is genuinely interchangeable.

Example:

```text
OrderFrequencyModel
```

may have:

```text
FixedOrderFrequencyModel
NegativeBinomialOrderFrequencyModel
BehaviorDrivenOrderFrequencyModel
```

The domain generator should depend on the meaningful behavior contract.

---

# 56. Factory Pattern

Factory is useful when construction depends on configuration.

Example:

```text
product-popularity-model = zipf
```

can cause:

```text
ProductPopularityModelFactory
```

to create the selected implementation.

The factory should remain outside the hot generation loop.

---

# 57. Builder Pattern

Builders should be rare.

Scala case classes already make simple immutable construction concise.

Use a Builder only when:

```text
construction is genuinely complex
validation spans many steps
optional combinations are numerous
```

---

# 58. Template Method

Do not force entity generators into a shared inheritance lifecycle.

For example:

```text
beforeGenerate
generate
afterGenerate
```

may look elegant but can hide meaningful domain differences.

Composition is generally preferred.

---

# 59. Domain Services

Use domain services when a business operation:

```text
does not naturally belong to one entity
```

Examples:

```text
ReturnEligibilityService
ShipmentSchedulingService
SessionJourneyService
```

The service name should express the business operation.

---

# 60. Pure Functions

Use pure functions where they improve clarity.

Good candidates:

```text
calculateLineTotal
calculateOrderTotal
deriveUnitNumber
validateRange
```

Pure functions are easy to test.

---

# 61. Randomness

Randomness is a dependency, not hidden global state.

Prefer:

```scala
model.sample(random)
```

or constructor injection of an appropriately scoped random stream.

---

# 62. Randomness Naming

Use business-oriented stream names.

Examples:

```text
customer-behavior
product-selection
order-frequency
session-duration
event-inter-arrival
```

This makes deterministic isolation understandable.

---

# 63. Distribution Naming

Distribution abstractions should communicate statistical meaning.

Good:

```text
WeightedCategoricalDistribution
ZipfDistribution
LogNormalDistribution
NegativeBinomialDistribution
```

Avoid:

```text
GenericDistribution2
AdvancedDistribution
RandomHelper
```

---

# 64. Distribution Parameters

Parameter names should communicate mathematical meaning.

For example:

```text
mean
standardDeviation
shape
scale
exponent
probability
```

Do not use:

```text
a
b
x
factor
```

unless they are standard in a narrow mathematical context.

---

# 65. Business Rules Should Be Named

Instead of:

```scala
if (x > y && z < 0.2 && p) {
  ...
}
```

prefer extracting a named decision:

```scala
if (isEligibleForReturn(orderItem, delivery, customerProfile)) {
  ...
}
```

The rule becomes discoverable.

---

# 66. Avoid Magic Numbers

Bad:

```scala
if (random.nextDouble() < 0.08)
```

inside business logic.

Better:

```scala
if (random.nextDouble() < returnProbability)
```

with the probability coming from configuration or a behavior model.

---

# 67. Constants

Constants should have business meaning.

Good:

```scala
private val MaximumItemsPerOrder = 20
```

if that is a real business/configuration constraint.

Avoid:

```scala
private val X = 20
```

---

# 68. Comments

Comments should explain:

```text
why
```

not simply:

```text
what
```

Bad:

```scala
// Generate customer
val customer = ...
```

Good:

```scala
// Keep customer acquisition time separate from generation wall-clock time
// so identical runs remain reproducible.
```

---

# 69. Avoid Comment-Driven Code

If a comment is required to explain what a function does, consider whether:

```text
the method name
```

or:

```text
the structure
```

could communicate it more clearly.

---

# 70. Documentation Comments

Public APIs and non-obvious domain abstractions should have useful documentation.

Do not document every trivial case-class field with repetitive comments.

---

# 71. Code Ordering

Within a class, a readable order is generally:

```text
constructor/dependencies
public API
business operations
supporting private operations
```

For objects:

```text
public factory/generation method
supporting private methods/constants
```

The exact order can vary when readability improves.

---

# 72. Import Organization

Keep imports minimal and organized.

Avoid wildcard imports when they obscure where important types originate.

---

# 73. Pattern Matching

Use pattern matching when the domain is naturally state-based.

Example:

```scala
order.status match {
  case PLACED    => ...
  case CONFIRMED => ...
  case SHIPPED   => ...
  case DELIVERED => ...
  case CANCELLED => ...
}
```

This communicates lifecycle semantics.

---

# 74. Exhaustiveness

Prefer exhaustive pattern matching for closed domain states.

Avoid:

```scala
case _ => ...
```

when silently accepting an unknown state could hide a bug.

---

# 75. Enumerations

For finite business categories, use a type-safe representation.

Examples:

```text
OrderStatus
PaymentMethod
DeviceType
Channel
```

The exact Scala representation can vary.

The important point is avoiding arbitrary strings throughout business logic.

---

# 76. Stringly-Typed Business Logic

Avoid repeated comparisons such as:

```scala
if (status == "DELIVERED")
```

throughout the codebase.

Prefer domain types.

---

# 77. Configuration Strings

Configuration may necessarily contain strings:

```hocon
profile = "large"
scenario = "hot-product"
```

Convert them into typed domain/configuration values at the configuration boundary.

---

# 78. Error Messages

Error messages should use domain vocabulary.

Good:

```text
Product PRODUCT_001 references missing Brand BRAND_004.
```

Bad:

```text
Lookup failed.
```

---

# 79. Logging

Logs should describe application phases and important outcomes.

Avoid logging every generated record.

Useful:

```text
Generation started
Generation plan created
Products generated
Validation completed
Dataset published
```

---

# 80. Logging Context

Where appropriate include:

```text
run ID
seed
profile
scenario
phase
```

This is especially important for large benchmark runs.

---

# 81. Avoid Logging in Domain Models

A simple case class should not log.

A pure business function should generally not log.

Observability belongs at application/infrastructure boundaries.

---

# 82. Validation Naming

Validation methods should clearly communicate what they validate.

Good:

```scala
validatePrimaryKeys()
validateForeignKeys()
validateOrderItems()
validateTemporalRelationships()
```

Avoid:

```scala
check()
verify()
processValidation()
```

when the specific responsibility can be named.

---

# 83. Statistics Naming

Statistics should use domain terms.

Good:

```text
averageOrderValue
itemsPerOrder
sessionsPerCustomer
eventsPerSession
returnRate
```

Avoid:

```text
metric1
statA
value
```

---

# 84. Output Naming

Output methods should clearly communicate the artifact.

Good:

```scala
writeCustomers()
writeOrders()
writeOrderItems()
writeManifest()
```

Avoid:

```scala
writeData()
save()
persistAll()
```

for broad responsibilities.

---

# 85. File Names and Business Entities

Output files should use stable entity names.

Current examples:

```text
customers.csv
products.csv
orders.csv
order_items.csv
events.csv
```

The output naming contract should be documented and stable.

---

# 86. Test Naming

Tests should describe behavior.

Good:

```text
should generate an address with a valid building reference
should preserve reproducibility for the same seed
should reject a building with no postal code
```

Avoid:

```text
test1
test2
works
basicTest
```

---

# 87. Test Organization

Tests should generally mirror domain ownership.

For example:

```text
customer/
  CustomerGeneratorTest.scala
  CustomerBehaviorModelTest.scala

address/
  AddressGeneratorTest.scala
```

This makes tests discoverable.

---

# 88. Fixtures

Fixtures should be named by purpose.

Good:

```text
minimalGeography
smallCatalog
customerWithHighReturnPropensity
```

Avoid:

```text
fixture1
data
testStuff
```

---

# 89. Test Data Readability

A test should make the relevant business situation obvious.

Prefer:

```scala
val customer =
  customerWithHighReturnPropensity
```

over constructing a large anonymous object whose important property is hidden.

---

# 90. Deterministic Tests

Tests involving randomness should use explicit seeds.

Avoid tests that depend on:

```text
current time
global random state
machine ordering
```

---

# 91. Statistical Test Readability

Statistical tests should explain:

```text
what distribution is expected
what tolerance is acceptable
why the threshold exists
```

Do not use unexplained:

```scala
assert(rate > 0.07 && rate < 0.09)
```

without context.

---

# 92. Configuration Test Naming

Configuration tests should communicate the business property.

Example:

```text
should reject a probability greater than one
should resolve large profile cardinalities
should reject incompatible skew scenarios
```

---

# 93. Avoid Deep Test Helpers

Test utilities can become another abstraction maze.

Keep fixtures close to the tests that use them unless they are genuinely shared.

---

# 94. API Surface

Keep classes and methods as private as practical.

If a method is only used internally:

```scala
private
```

should be preferred.

A smaller public API is easier to maintain.

---

# 95. Package-Private Scope

Scala package-private visibility can be useful where multiple components in one domain need access.

Use it carefully.

Do not expose internals globally simply to avoid designing a proper API.

---

# 96. Encapsulation

Reference-data indexes should remain encapsulated.

For example:

```scala
geography.resolveBuilding(id)
```

is preferable to:

```scala
geography.buildings.get(id)
```

when callers should not depend on internal storage.

---

# 97. Immutability

Prefer:

```text
val
immutable case classes
immutable collections
```

throughout the domain.

Mutable state should have a clear performance or lifecycle reason.

---

# 98. Mutable State

Potential legitimate uses include:

```text
streaming statistics accumulator
progress counter
output writer state
```

These should remain infrastructure/application concerns where possible.

---

# 99. Avoid Global Mutable State

Never make core business behavior depend on:

```scala
var globalRandom
var globalConfig
var currentScenario
```

This undermines:

```text
reproducibility
parallelism
test isolation
```

---

# 100. Functional Style

Use functional constructs where they make the business flow clearer.

Good:

```scala
orders.map(...)
orders.filter(...)
```

when operating on appropriately sized collections.

For large data, use:

```scala
Iterator
```

or streaming abstractions.

Do not use functional transformations mechanically when they create unnecessary allocations.

---

# 101. Performance and Readability

Readable code is not automatically slow.

The preferred sequence is:

```text
write clear code
→ measure
→ identify bottleneck
→ optimize bottleneck
→ preserve semantics
```

---

# 102. Avoid Clever Code

Do not compress complex business behavior into one expression merely because Scala allows it.

Bad:

```scala
val x = a.flatMap(...).groupBy(...).view.mapValues(...).toSeq...
```

when the business flow becomes difficult to understand.

Break it into named steps.

---

# 103. Intermediate Variables

Intermediate variables are useful when they communicate business concepts.

Good:

```scala
val customerProfile = ...
val purchaseIntent = ...
val orderTime = ...
```

Avoid removing these solely to reduce line count.

---

# 104. Business Vocabulary in Variables

Prefer:

```text
purchaseIntent
returnProbability
deliveryDelay
categoryAffinity
```

over:

```text
p
x
factor
value
tmp
```

---

# 105. Avoid Generic `data`

Names such as:

```text
data
result
value
info
context
```

are sometimes unavoidable.

But when the type has business meaning, use it.

Prefer:

```text
customer
orders
catalog
geography
validationResult
```

---

# 106. Context Naming

`Context` should be reserved for a real contextual concept.

Examples:

```text
TemporalContext
GenerationContext
ScenarioContext
```

Avoid:

```text
EverythingContext
ApplicationContext
GlobalContext
```

that silently contains the entire application.

---

# 107. Model vs Service vs Generator

These names should remain semantically distinct.

### Model

Represents behavior or a domain abstraction.

```text
CustomerBehaviorModel
ProductPopularityModel
```

### Generator

Creates records.

```text
CustomerGenerator
OrderGenerator
```

### Service

Performs a domain operation.

```text
ReturnEligibilityService
```

This vocabulary helps readers understand responsibilities.

---

# 108. Factory vs Builder

### Factory

Chooses or constructs an implementation.

### Builder

Constructs a complex object progressively.

Do not use these names interchangeably.

---

# 109. Resolver vs Selector

### Resolver

Finds a known object from a relationship/key.

```text
resolveBuilding(buildingId)
```

### Selector

Chooses among alternatives.

```text
selectProduct(customerProfile, context)
```

The distinction is useful.

---

# 110. Validator vs Rule

### Validator

Coordinates validation.

### Rule

Defines a specific validation condition.

For example:

```text
OrderTemporalRule
```

may implement one rule.

`OrderValidator` can coordinate multiple rules.

Do not create a rule abstraction unless multiple rules justify it.

---

# 111. Quality Rule Naming

Use names that describe the defect or business condition.

Good:

```text
MissingForeignKeyRule
InvalidPostalCodeRule
ReturnOutsideWindowRule
DuplicatePrimaryKeyRule
```

---

# 112. Scenario Naming

Scenario names should communicate business/workload intent.

Good:

```text
baseline
hot-product
festival-season
dirty-foreign-key
high-activity
```

Avoid:

```text
scenario1
scenario2
test-mode
special
```

---

# 113. Profile Naming

Profiles should communicate scale or behavior.

Good:

```text
small
medium
large
xlarge
```

and where behavior profiles exist:

```text
high-activity
high-return
price-sensitive
```

Do not overload one profile name with unrelated meanings.

---

# 114. Package Dependency Direction

Domain packages should not depend on:

```text
output
observability
application bootstrap
```

for core business logic.

A preferred direction is:

```text
application
   ↓
domain
   ↓
common abstractions
```

with infrastructure plugged in at boundaries.

---

# 115. Avoid Circular Dependencies

A common warning sign:

```text
Customer → Order
Order → Customer
```

through implementation-level dependencies.

Use:

```text
IDs
behavior profiles
focused services
application orchestration
```

where appropriate.

---

# 116. Dependency Rules

A practical target:

```text
model
  → minimal dependencies

behavior
  → model + common abstractions

generator
  → model + behavior + reference/context

application
  → generators + validators + output + observability
```

Exact dependencies will vary.

---

# 117. Domain Model Independence

Domain models should not depend on:

```text
HOCON
CSV
JVM filesystem
Spark
logging
```

They represent business data.

---

# 118. Configuration Boundary

Raw HOCON should be translated into typed configuration near the configuration boundary.

Do not scatter:

```scala
config.getString(...)
config.getInt(...)
```

through domain generation loops.

---

# 119. Output Boundary

Domain records should not know:

```text
CSV header
file path
buffer size
```

The output layer owns serialization.

---

# 120. Statistics Boundary

Domain generators should not own global statistics.

They can expose generated values.

Statistics components aggregate them.

---

# 121. Observability Boundary

Business code should not depend directly on:

```text
console printing
logging framework
metrics backend
```

unless there is a clear application-level requirement.

---

# 122. Error Boundary

Domain components should provide meaningful errors.

The application layer decides:

```text
run failed
publication blocked
exit outcome
```

---

# 123. Documentation and Code Alignment

Every major domain document should map cleanly to code.

For each domain, a reader should be able to find:

```text
business meaning
model
generator
behavior
validation
statistics
tests
```

where those responsibilities exist.

---

# 124. Current Address Example

The Address domain demonstrates this principle.

Current model:

```scala
case class Address(
  id: String,
  customerId: String,
  buildingId: String,
  unitNumber: String,
  postalCode: String
)
```

The code should preserve the business decision that:

```text
buildingId
```

anchors the geography relationship.

It should not introduce duplicate location attributes simply because they are convenient for output.

---

# 125. Current Geography Example

The Geography component should expose business-oriented operations such as:

```scala
geography.resolveBuilding(buildingId)
geography.postalCodesForArea(areaId)
```

rather than making every caller understand the internal hierarchy maps.

---

# 126. Current Generation Pipeline Example

The existing `GenerationPipeline` is a useful foundation.

The target refactoring should make its business flow clearer rather than simply moving the same complexity into more classes.

---

# 127. Refactoring Rule

Moving code between packages is not architecture.

Architecture improves when:

```text
ownership
responsibility
dependency direction
business meaning
```

become clearer.

---

# 128. One Responsibility at a Time

When refactoring a file, ask:

```text
What is this file responsible for?
What dependencies does it truly need?
Which code belongs elsewhere?
What public API should remain?
```

Then make the smallest coherent change.

---

# 129. Avoid Mechanical Extraction

Do not extract every block into a class.

For example:

```text
TimestampHelper
StringHelper
CustomerNameHelper
CustomerAgeHelper
```

may create more indirection than value.

Extract behavior when it has:

```text
business meaning
reuse
independent testing value
alternative implementations
```

---

# 130. Refactoring Safety

Each meaningful refactor should be followed by:

```text
compile
unit tests
integration tests where relevant
E2E run where relevant
```

The current baseline is:

```text
229 tests passing
```

and should be preserved during migration.

---

# 131. One-File-at-a-Time Discipline

The preferred implementation workflow is:

```text
select one file
    ↓
inspect dependencies
    ↓
modify
    ↓
compile
    ↓
run relevant tests
    ↓
verify
    ↓
move to next file
```

This keeps architectural migration controlled.

---

# 132. Git Discipline

Each coherent architectural step should be easy to review and, ideally, easy to revert.

Avoid giant commits that combine:

```text
package moves
+
business logic changes
+
realism changes
+
performance changes
```

unless there is a strong reason.

---

# 133. Refactoring vs Feature Work

Separate:

```text
architecture refactoring
```

from:

```text
new realism behavior
```

where practical.

Otherwise a failing test becomes difficult to diagnose.

---

# 134. Migration Order

The code migration should follow the architecture documents and dependency graph.

A practical sequence is:

```text
common/random/distribution
→ geography
→ category
→ brand
→ product
→ customer
→ address
→ order
→ orderitem
→ payment
→ shipment
→ return
→ session
→ event
→ relationships
→ validation/statistics/output
→ application orchestration
```

The exact order may change when implementation dependencies require it.

---

# 135. Architecture Before Optimization

Do not optimize a poorly understood design.

First establish:

```text
domain ownership
behavior boundaries
generation responsibilities
dependency direction
```

Then optimize hot paths.

---

# 136. Architecture Before Realism

Likewise, do not add sophisticated behavior into a tangled generator.

First establish:

```text
CustomerBehaviorModel
OrderFrequencyModel
ProductPopularityModel
TemporalModel
```

or equivalent focused abstractions.

Then implement richer realism.

---

# 137. Code Review Questions

Every architectural code review should ask:

### Business

```text
Does this code represent a clear business concept?
```

### Responsibility

```text
Does this class have one coherent responsibility?
```

### Dependency

```text
Are dependencies visible?
```

### Readability

```text
Can a reader understand the business flow?
```

### Testability

```text
Can the behavior be tested independently?
```

### Performance

```text
Does this introduce unnecessary allocations or O(n²) work?
```

### Reproducibility

```text
Does this change random-stream behavior?
```

---

# 138. Anti-Pattern: God Generator

Symptoms:

```text
500+ lines
many unrelated private methods
dozens of dependencies
configuration parsing
randomness
validation
output
```

Remedy:

```text
extract business responsibilities
```

not merely:

```text
split the file into arbitrary chunks
```

---

# 139. Anti-Pattern: God Manager

Names such as:

```text
RelationshipManager
DataManager
GenerationManager
BusinessManager
```

often indicate unclear ownership.

Replace with focused responsibilities.

---

# 140. Anti-Pattern: Generic Helper

A helper with methods such as:

```text
format
calculate
generate
validate
select
resolve
```

for unrelated domains is a design smell.

---

# 141. Anti-Pattern: Primitive Obsession

Passing:

```text
String
Double
Int
```

everywhere can hide business semantics.

Introduce domain types where invariants or meaning justify them.

Do not wrap primitives indiscriminately.

---

# 142. Anti-Pattern: Stringly Typed State

Avoid:

```scala
status: String
```

when the state is a closed business vocabulary.

Prefer a typed representation.

---

# 143. Anti-Pattern: Hidden Randomness

Avoid:

```scala
scala.util.Random.nextInt(...)
```

inside arbitrary domain methods.

Use the project's deterministic random abstraction.

---

# 144. Anti-Pattern: Hidden Time

Avoid:

```scala
Instant.now()
```

inside generation logic.

Use the simulation clock/time model.

---

# 145. Anti-Pattern: Configuration Leakage

Avoid:

```scala
config.getDouble("customer.return-probability")
```

inside `ReturnGenerator`.

Configuration should be resolved before domain execution.

---

# 146. Anti-Pattern: Infrastructure Leakage

Avoid domain code such as:

```scala
Files.write(...)
println(...)
logger.info(...)
```

inside business generators.

---

# 147. Anti-Pattern: Over-Abstraction

Avoid creating:

```text
Factory
Strategy
Builder
Provider
Manager
Service
Adapter
Facade
```

for every class.

Use a pattern only when it solves a real design problem.

---

# 148. Anti-Pattern: Under-Abstraction

The opposite problem is putting all behavior into:

```text
CustomerGenerator
OrderGenerator
```

even when those classes clearly contain independent business models.

Use composition where behavior has meaningful identity.

---

# 149. Anti-Pattern: Clever Generic Framework

Do not build a universal framework that attempts to generate every entity through:

```text
GenericEntityGenerator[T]
GenericRelationship[T,U]
GenericBusinessRule[T]
```

if it makes the business model harder to understand.

ShopSphere is a business-domain simulation, not a generic entity framework.

---

# 150. Naming Quality Gate

Before accepting a class or method name, ask:

```text
Does the name communicate business meaning?
Would another developer understand its responsibility without opening the file?
Is it more specific than Manager/Helper/Processor?
Does it distinguish model/generator/service/selector/resolver?
```

---

# 151. Readability Quality Gate

Before accepting a method, ask:

```text
Can I describe what this method does in one sentence?
Are the dependencies obvious?
Are important business decisions named?
Are magic numbers eliminated?
Is the control flow understandable?
```

---

# 152. Architecture Quality Gate

Before accepting a new abstraction, ask:

```text
Why does this abstraction exist?
What variation does it isolate?
Does it improve testing?
Does it improve dependency direction?
Does it improve readability?
Would the code be clearer without it?
```

If the last answer is yes, do not add it.

---

# 153. Performance Quality Gate

Before optimizing code, ask:

```text
Have we measured the bottleneck?
What is the current complexity?
What allocations occur?
Will the optimization change reproducibility?
Will it make the business model harder to understand?
```

---

# 154. Reproducibility Quality Gate

Any change involving:

```text
randomness
ordering
parallelism
sampling
```

should explicitly consider:

```text
same seed
same configuration
same reference data
→ expected output
```

---

# 155. Testing Quality Gate

A new domain component should have appropriate tests for:

```text
normal behavior
boundary behavior
invalid input
determinism
relationships
statistical behavior
```

depending on its responsibility.

---

# 156. Documentation Quality Gate

When a meaningful business rule changes:

```text
code
tests
documentation
```

should remain aligned.

The domain documents are part of the architecture, not merely project notes.

---

# 157. Scala Version

The project standard is:

```text
Scala 2.13
```

Specifically the current build uses:

```text
Scala 2.13.18
```

Do not introduce Scala 3 syntax or Scala-3-specific libraries.

---

# 158. Java Compatibility

The current environment uses:

```text
Java 17
```

Code should remain compatible with the project's supported Java runtime.

---

# 159. Dependency Discipline

Before adding a library, ask:

```text
Does the project genuinely need it?
Can the requirement be implemented clearly with the existing stack?
Does the dependency improve or reduce maintainability?
Does it affect reproducibility?
```

Avoid dependency growth for trivial utilities.

---

# 160. Build Discipline

After code changes:

```powershell
sbt test
```

is the default verification step.

For changes affecting the complete pipeline:

```powershell
sbt run
```

should also be used.

---

# 161. Compile Fast During Refactoring

During one-file-at-a-time migration, use:

```powershell
sbt compile
```

for rapid feedback before running the full suite when appropriate.

Then:

```powershell
sbt test
```

before moving to the next coherent change.

---

# 162. Current Baseline

The architecture migration begins from a functioning baseline:

```text
229 tests passing
```

and an end-to-end run that successfully generates and validates the baseline dataset.

This baseline is an important regression guard.

---

# 163. Refactoring Success

A refactor is successful when:

```text
tests remain green
+
business behavior remains correct
+
ownership becomes clearer
+
dependencies become clearer
+
future realism becomes easier to add
```

Moving files without these improvements is not sufficient.

---

# 164. Realism Expansion

Once architecture is stable, readability standards should make sophisticated realism straightforward to add.

For example:

```text
CustomerBehaviorProfile
    ↓
CustomerActivityModel
    ↓
SessionGenerator

CustomerBehaviorProfile
    ↓
OrderFrequencyModel
    ↓
OrderGenerator

CustomerBehaviorProfile
    ↓
ReturnProbabilityModel
    ↓
ReturnGenerator
```

This is more maintainable than embedding all behavior in downstream generators.

---

# 165. Business Flow as Code

A mature generator should read approximately like:

```text
load the business world
→ establish customer behavior
→ generate customers
→ establish product catalog
→ generate activity
→ generate orders
→ generate order items
→ generate payments
→ generate shipments
→ generate returns
→ validate
→ measure
→ publish
```

The implementation details should support this narrative.

---

# 166. The Reader Test

A useful final test for code quality:

> If a developer understands the ShopSphere business documents but has not seen the implementation, can they read the generator code and recognize the same business model?

If yes, the architecture is succeeding.

If not, the code is hiding too much of the domain.

---

# 167. Definition of Done

Code organization, naming, and readability standards are mature when:

### Discoverability

A developer can locate domain code quickly.

### Naming

Classes, methods, variables, and configuration use meaningful business terminology.

### Ownership

Each important behavior has an obvious owner.

### Dependencies

Important dependencies are visible and directed correctly.

### Composition

Complex generators are composed from focused responsibilities.

### Abstraction

Patterns are used only where they solve real problems.

### Testability

Business behavior can be tested independently.

### Performance

Readable code can still scale through measured optimization.

### Reproducibility

Randomness and time remain explicit dependencies.

### Maintainability

Future realism can be added without turning generators into god objects.

---

# 168. Explicit Architecture Decisions

### Decision 1

Domain terminology is the primary vocabulary of the codebase.

### Decision 2

Package structure follows business ownership.

### Decision 3

`common` and cross-cutting packages remain intentionally small and focused.

### Decision 4

Generic `Helper`, `Manager`, and `Processor` classes should be avoided when a business-specific name exists.

### Decision 5

Entity generators create domain records; they do not own unrelated business behavior.

### Decision 6

Behavior models represent meaningful business behavior and are composed into generators.

### Decision 7

Constructor injection is the default dependency-injection mechanism.

### Decision 8

Interfaces and traits are introduced only when a genuine substitution or boundary exists.

### Decision 9

Composition is preferred over inheritance.

### Decision 10

Strategy is used for real interchangeable behavior.

### Decision 11

Factory is used when configuration-driven implementation construction is meaningful.

### Decision 12

Builder and Template Method should not be introduced by default.

### Decision 13

Business rules should be named rather than hidden inside complex conditional expressions.

### Decision 14

Magic numbers and unexplained constants should not be embedded in business logic.

### Decision 15

Raw configuration should not leak into domain generation.

### Decision 16

Wall-clock time and global randomness should not leak into business generation.

### Decision 17

Domain models remain independent of output, filesystem, logging, and Spark.

### Decision 18

Readable code is preferred over clever or prematurely optimized code.

### Decision 19

Performance optimization must be measurement-driven.

### Decision 20

Architecture refactoring should proceed incrementally, with one-file-at-a-time verification.

### Decision 21

The current passing test baseline should be preserved during migration.

### Decision 22

Documentation, code, and tests should remain aligned as business rules evolve.

---

# 169. Final Design Summary

The target codebase should make this structure immediately visible:

```text
com.shopsphere.datagenerator

├── customer
│   ├── model
│   │   └── Customer
│   ├── behavior
│   │   ├── CustomerBehaviorProfile
│   │   ├── CustomerActivityModel
│   │   └── CustomerSpendingModel
│   ├── generator
│   │   └── CustomerGenerator
│   └── validation
│
├── product
│   ├── model
│   │   └── Product
│   ├── behavior
│   │   └── ProductPopularityModel
│   └── generator
│       └── ProductGenerator
│
├── order
│   ├── model
│   │   └── Order
│   ├── behavior
│   │   └── OrderFrequencyModel
│   └── generator
│       └── OrderGenerator
│
├── orderitem
│   ├── model
│   │   └── OrderItem
│   ├── behavior
│   │   ├── BasketSizeModel
│   │   └── ProductSelector
│   └── generator
│
├── shipment
│   ├── model
│   ├── behavior
│   │   └── ShipmentSchedulingModel
│   └── generator
│
├── return
│   ├── model
│   ├── behavior
│   │   └── ReturnProbabilityModel
│   └── generator
│
├── geography
│   ├── model
│   ├── reference
│   └── loader
│
├── generation
│   ├── GenerationPlan
│   ├── GenerationContext
│   └── GenerationOrchestrator
│
├── config
├── validation
├── quality
├── scenario
├── output
├── manifest
├── statistics
├── observability
└── common
```

The important property is not the exact directory tree.

It is that the structure answers:

```text
Who owns this behavior?
What does this class do?
What does it depend on?
Why does this abstraction exist?
```

without requiring a long investigation.

A well-designed ShopSphere codebase should allow a developer to follow a business decision from:

```text
architecture document
        ↓
domain concept
        ↓
configuration
        ↓
behavior model
        ↓
generator
        ↓
validation
        ↓
statistics
        ↓
output
```

with minimal ambiguity.

The strongest implementation will therefore not be the one with the largest number of classes, patterns, abstractions, or frameworks.

It will be the one where:

```text
business meaning
+
code structure
+
dependency structure
+
tests
+
documentation
```

all tell the same story.

That is the standard to use throughout the remaining refactoring work.
