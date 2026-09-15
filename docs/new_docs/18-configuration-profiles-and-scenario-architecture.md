# 18 — Configuration, Profiles, and Scenario Architecture

## 1. Purpose

Configuration is the control surface of the ShopSphere data generator.

The generator must allow users to control:

```text
dataset scale
cardinality
statistical behavior
entity generation
business scenarios
data quality
skew
output
reproducibility
```

without changing source code.

At the same time, configuration must not become a second programming language or one enormous object containing every possible setting.

This document defines the architecture for:

- HOCON configuration,
- typed configuration,
- configuration ownership,
- effective configuration,
- generation profiles,
- cardinality profiles,
- scenarios,
- overrides,
- strategy selection,
- validation,
- configuration fingerprints,
- reproducibility,
- configuration evolution,
- domain-specific configuration boundaries,
- application-level configuration,
- avoiding configuration-driven complexity.

The central principle is:

> **Configuration describes the desired synthetic world; domain code implements the behavior that produces it.**

---

# 2. Why Configuration Architecture Matters

ShopSphere is explicitly intended to support configurable data generation.

The same codebase should be able to produce:

```text
small clean dataset
medium realistic dataset
large skewed dataset
campaign-heavy dataset
return-heavy dataset
high-activity dataset
```

without changing domain code.

Configuration therefore controls important business and technical dimensions.

Poor configuration design can create:

```text
stringly typed behavior
hidden defaults
contradictory settings
giant config classes
duplicated configuration
unclear precedence
runtime branching everywhere
```

---

# 3. Configuration Goals

The configuration system should provide:

1. strong typing,
2. explicit defaults,
3. clear ownership,
4. validation before generation,
5. reproducibility,
6. profile composition,
7. scenario composition,
8. explicit overrides,
9. readable HOCON,
10. stable effective configuration,
11. useful error messages,
12. domain isolation,
13. extensibility.

---

# 4. Non-Goals

Configuration should not become responsible for:

```text
business algorithms
arbitrary Scala expressions
runtime object graphs
database queries
custom executable code
```

For example, this is undesirable:

```hocon
orderCountFormula = "customers * 4 + random(100)"
```

Business behavior belongs in code.

---

# 5. Current Configuration Model

The current project already has a typed top-level structure similar to:

```scala
case class GeneratorSettings(
  seed: Long,
  profile: GenerationProfile,
  cardinalityProfile: CardinalityProfile,
  scenario: GenerationScenario
)

case class OutputSettings(
  directory: String
)

case class GenerationConfig(
  generator: GeneratorSettings,
  output: OutputSettings,
  profileDefinition: GenerationProfileDefinition,
  cardinality: CardinalityConfig,
  distributions: DistributionConfig,
  productDistribution: ProductDistributionConfig,
  productPricing: ProductPricingConfig,
  productBrandAffinity: ProductBrandAffinityConfig,
  customerGeneration: CustomerGenerationConfig
)
```

This is a useful starting point.

However, the configuration architecture should evolve as domain packages become more sophisticated.

---

# 6. Typed Configuration

Configuration should be parsed once into typed Scala objects.

The flow should be:

```text
HOCON
  ↓
ConfigLoader
  ↓
typed configuration
  ↓
validation
  ↓
effective configuration
  ↓
generation
```

Generators should not repeatedly read raw HOCON.

---

# 7. Why Typed Configuration Matters

Typed configuration gives:

```text
compile-time structure
better IDE support
centralized validation
clear dependencies
testable objects
readable generator code
```

Instead of:

```scala
config.getString("customer.behavior.model")
```

throughout the application, prefer:

```scala
config.customer.behaviorModel
```

after configuration loading.

---

# 8. Configuration Ownership

Every setting should have an owner.

Examples:

```text
seed → generator/application
output directory → output
customer behavior → customer
basket size → order/orderitem
product popularity → product
payment behavior → payment
shipment timing → shipment
return behavior → return
```

Avoid putting every setting under a generic:

```text
generator.*
```

section.

---

# 9. Domain Configuration

A domain should own configuration describing its own behavior.

Conceptually:

```text
customer/
  CustomerGenerationConfig

product/
  ProductGenerationConfig

order/
  OrderGenerationConfig

payment/
  PaymentGenerationConfig
```

This keeps domain dependencies explicit.

---

# 10. Application Configuration

Some configuration is inherently application-wide.

Examples:

```text
seed
profile
cardinality profile
scenario
output directory
reference-data locations
```

These belong at the application/configuration layer.

---

# 11. Cross-Cutting Configuration

Some concepts are shared across domains:

```text
randomness
time range
distribution defaults
quality
skew
```

These can have cross-cutting configuration sections.

But domain-specific parameters should remain domain-owned.

---

# 12. Avoid One Giant Configuration Class

A class containing:

```text
100+ fields
```

is difficult to understand.

It also forces unrelated generators to depend on settings they do not use.

Prefer composition:

```scala
case class GenerationConfig(
  generator: GeneratorSettings,
  output: OutputSettings,
  customer: CustomerGenerationConfig,
  product: ProductGenerationConfig,
  order: OrderGenerationConfig,
  ...
)
```

with each domain configuration itself structured.

---

# 13. Configuration Dependency Direction

The desired dependency is:

```text
Raw HOCON
   ↓
ConfigLoader
   ↓
Typed Config
   ↓
Domain Components
```

Not:

```text
Domain Generator
   ↓
Raw HOCON
```

---

# 14. Configuration Loading

`ConfigLoader` should primarily perform:

```text
parsing
mapping
basic structural checks
```

It should not contain business generation logic.

---

# 15. Configuration Validation

Validation should happen before generation.

The application should fail early if configuration is invalid.

Examples:

```text
negative customer count
invalid probability
unknown strategy
invalid distribution parameters
empty required reference-data location
```

---

# 16. Validation Layers

Configuration validation can have several layers.

## Syntax

Can HOCON be parsed?

## Structure

Are required sections present?

## Type

Can values be converted to expected types?

## Mathematical

Are parameters valid?

## Business

Are combinations valid?

## Cross-domain

Are related settings compatible?

---

# 17. Syntax Validation

HOCON parsing should reject malformed configuration before typed mapping.

This is infrastructure-level validation.

---

# 18. Type Validation

Examples:

```text
seed → Long
count → Int/Long
probability → Double
profile → enum/value type
```

Invalid types should produce clear errors.

---

# 19. Mathematical Validation

Examples:

```text
probability ∈ [0,1]
weight >= 0
minimum <= maximum
standardDeviation > 0
shape > 0
```

---

# 20. Business Validation

Examples:

```text
minimum basket size cannot exceed maximum
return window cannot be negative
shipment delay cannot be negative
```

---

# 21. Cross-Domain Validation

Examples:

```text
OrderItems cannot be generated if Product count is zero
Events require Sessions
Shipments require Orders
Returns require eligible OrderItems
```

Some of these can be guaranteed by generation ordering, but impossible configurations should still be rejected where practical.

---

# 22. Fail-Fast Principle

Do not start generating millions of records before discovering:

```text
invalid probability
```

or:

```text
unknown distribution
```

Configuration should be validated first.

---

# 23. Configuration Defaults

Defaults should be explicit and documented.

Example:

```hocon
customer {
  behavior {
    model = "segmented"
  }
}
```

If the model has a default, that default should be visible either in:

```text
reference.conf
```

or clearly documented typed configuration.

---

# 24. Default Configuration

A standard structure can be:

```text
application.conf
reference.conf
profile/scenario overrides
```

The exact file organization should remain simple.

---

# 25. Reference Configuration

`reference.conf` can provide safe baseline defaults.

It should not hide important business assumptions.

For important settings, documentation should explain:

```text
what it means
why the default exists
```

---

# 26. Application Configuration

`application.conf` should define the normal runnable configuration.

Profiles/scenarios may override selected sections.

---

# 27. Environment Overrides

Environment-specific deployment values may eventually override technical settings such as:

```text
output path
```

But business configuration should remain reproducible.

If environment variables affect generated data, they must become part of the effective configuration fingerprint.

---

# 28. Profile Concept

A GenerationProfile represents a coherent generation preset.

Examples:

```text
small
medium
large
xlarge
```

The profile primarily controls scale and broad generation characteristics.

---

# 29. Cardinality Profile

CardinalityProfile controls dataset scale relationships.

Examples:

```text
low
medium
high
```

It may define:

```text
customers
products
orders
sessions
```

and relationship targets.

---

# 30. Profile vs Cardinality Profile

These concepts should not be conflated.

A GenerationProfile can describe:

```text
overall dataset preset
```

while CardinalityProfile describes:

```text
record-count relationships
```

The exact boundary should remain explicit.

---

# 31. Scenario Concept

A GenerationScenario represents a deliberate behavioral condition.

Examples:

```text
baseline
high_activity
campaign_spike
hot_customer
hot_product
return_spike
```

A scenario changes behavior without changing the core domain implementation.

---

# 32. Profile vs Scenario

A profile answers:

> "How large or broadly configured is this dataset?"

A scenario answers:

> "What kind of world should this dataset represent?"

Example:

```text
large + campaign_spike
```

is meaningful.

---

# 33. Scenario Composition

Eventually multiple scenarios may need to compose.

Example:

```text
large
+
campaign spike
+
hot products
```

This should be supported only if interactions are well-defined.

---

# 34. Scenario Conflicts

Some scenarios may conflict.

Example:

```text
uniform-product-demand
```

and:

```text
extreme-hot-product-skew
```

cannot both fully control Product popularity.

The configuration system should detect or define precedence.

---

# 35. Scenario Precedence

A possible precedence model is:

```text
defaults
   ↓
profile
   ↓
cardinality profile
   ↓
scenario
   ↓
explicit run override
```

The project should adopt one explicit rule and test it.

---

# 36. Why Precedence Matters

Without defined precedence:

```text
Which value wins?
```

becomes dependent on loading order or implementation details.

That is unacceptable for reproducibility.

---

# 37. Effective Configuration

Before generation, resolve all configuration layers into one:

```text
EffectiveGenerationConfig
```

Conceptually:

```text
defaults
+ profile
+ cardinality
+ scenario
+ explicit overrides
→ effective config
```

---

# 38. Generators Consume Effective Configuration

Generators should receive:

```text
effective typed configuration
```

rather than independently resolving overrides.

This prevents duplicated precedence logic.

---

# 39. Configuration Resolution Is an Application Concern

The application/configuration layer should resolve:

```text
what settings are active
```

Domain code should implement:

```text
what those settings mean
```

---

# 40. Configuration Immutability

After resolution and validation:

```text
EffectiveGenerationConfig
```

should be immutable.

Generation should not modify it.

---

# 41. Configuration Snapshot

The effective configuration should be serializable or renderable for the manifest.

This provides a record of:

```text
what was actually generated
```

rather than only:

```text
which profile was requested
```

---

# 42. Configuration Fingerprint

A stable hash of the canonical effective configuration should eventually be recorded.

Conceptually:

```text
configurationHash
```

This supports reproducibility and dataset identification.

---

# 43. Canonicalization

The configuration must be canonicalized before hashing.

Potential sources of instability include:

```text
map ordering
whitespace
file ordering
environment expansion
```

---

# 44. Fingerprint Inputs

The reproducibility fingerprint should account for:

```text
effective configuration
seed
generator version
reference-data identity
```

The exact manifest structure is defined separately.

---

# 45. Strategy Selection

Configuration may select business strategies.

Example:

```hocon
customer.behavior.model = "segmented"
```

which maps to:

```text
SegmentedCustomerBehaviorModel
```

---

# 46. Factory Boundary

The mapping:

```text
configuration name
       ↓
implementation
```

belongs in a Factory.

Do not scatter string comparisons throughout generators.

---

# 47. Unknown Strategy

An unknown strategy should fail immediately.

Example:

```text
Unknown customer behavior model: "foo"
Supported models: segmented, simple
```

---

# 48. Strategy Parameters

A strategy's parameters should be typed.

For example:

```text
Zipf:
  exponent
  minimumRank

LogNormal:
  mean
  standardDeviation
```

Avoid passing arbitrary untyped maps deep into domain logic.

---

# 49. Typed Polymorphic Configuration

A future configuration model may use a tagged structure.

Conceptually:

```text
model = "zipf"
zipf { ... }
```

or:

```text
model {
  type = "zipf"
  exponent = ...
}
```

The exact format can be selected during implementation.

---

# 50. Avoid Overly Generic Configuration

Do not attempt to represent every future strategy with:

```text
Map[String, Any]
```

This sacrifices readability and validation.

---

# 51. Domain Configuration Example

Conceptually:

```hocon
customer {
  behavior {
    model = "segmented"

    activity {
      low = ...
      regular = ...
      high = ...
    }
  }
}
```

The corresponding Scala configuration should express the same concepts clearly.

---

# 52. Product Configuration

Product configuration may eventually include:

```text
catalog size
price model
popularity model
category affinity
brand affinity
lifecycle
```

These should be grouped according to Product ownership.

---

# 53. Order Configuration

Order configuration may include:

```text
order frequency
basket size
order timing
status
conversion
```

Some conversion settings may be owned by Session/Event if they describe the digital journey.

---

# 54. Event Configuration

Event configuration may include:

```text
journey model
event types
event timing
funnel probabilities
abandonment
conversion
```

---

# 55. Shipment Configuration

Shipment configuration may include:

```text
carrier selection
service level
delivery delay
failure probability
split-shipment behavior
```

---

# 56. Return Configuration

Return configuration may include:

```text
return window
reason distribution
return probability model
partial-return behavior
refund timing
```

---

# 57. Cross-Cutting Quality Configuration

Data quality should have explicit configuration.

Conceptually:

```hocon
quality {
  profile = "clean"
}
```

or:

```text
quality scenarios
```

depending on the final architecture.

---

# 58. Clean Baseline

The default scenario should produce clean data.

That means:

```text
no intentional orphan references
no intentional malformed values
no intentional timestamp corruption
```

---

# 59. Dirty Scenario

A dirty scenario can introduce:

```text
missing values
invalid references
duplicate records
malformed values
```

with explicit rates.

---

# 60. Quality Scenario Isolation

Quality configuration should not silently alter business distributions.

Ideally:

```text
clean business generation
      ↓
quality transformation
```

This allows controlled comparison.

---

# 61. Skew Configuration

Skew should also be explicit.

Examples:

```text
hot_customer
hot_product
hot_category
hot_geography
```

---

# 62. Skew vs Scenario

Skew is a property of generated data shape.

Scenario is a broader business condition.

A scenario may activate skew.

The architecture should allow both concepts without duplicating configuration.

---

# 63. Example: Hot Customer

Configuration might eventually specify:

```hocon
scenario {
  type = "hot-customer"

  hotCustomer {
    fraction = 0.01
    activityMultiplier = 10.0
  }
}
```

The Customer behavior system interprets this.

---

# 64. Example: Campaign Spike

A campaign scenario may define:

```text
start
end
traffic multiplier
conversion multiplier
target categories
```

The relevant domain behavior models consume these parameters.

---

# 65. Avoid Row-Level Scenario Logic

Do not let configuration say:

```text
customerId=1234 should receive 100 orders
```

unless the scenario is explicitly designed for deterministic fixture behavior.

Scenarios should normally modify population-level behavior.

---

# 66. Scenario and Reproducibility

Scenario configuration must be included in the effective configuration fingerprint.

Otherwise two runs may appear equivalent when they are not.

---

# 67. Scenario and Random Streams

Each scenario should have deterministic random-stream boundaries.

For example:

```text
root/scenario/hot-customer
```

This follows the randomness architecture.

---

# 68. Profile Composition

A profile should ideally be declarative.

Example:

```text
medium
```

might resolve:

```text
customers = 1000
products = 500
orders = 5000
```

The profile does not implement generation logic.

---

# 69. Scale Profiles

Potential profiles:

```text
small
medium
large
xlarge
```

Their purpose is operational scale.

---

# 70. Cardinality Profiles

Potential cardinality profiles:

```text
low
medium
high
```

Their purpose is relative entity counts and relationship density.

---

# 71. Scale and Cardinality

These can be combined:

```text
large + high
```

to produce a large, relationship-dense dataset.

The combination should be validated against resource expectations.

---

# 72. Output Volume Estimation

Generation planning should estimate:

```text
Customers
Addresses
Products
Orders
OrderItems
Payments
Shipments
Returns
Sessions
Events
```

before generation.

---

# 73. Unsafe Configuration

A configuration that implies:

```text
10 million customers
+
100 sessions/customer
+
100 events/session
```

could create billions of records.

The generator should warn or reject based on configured safety limits.

---

# 74. Safety Limits

Potential limits:

```text
maximum estimated records
maximum estimated output bytes
maximum relationship expansion
```

These should be configurable.

---

# 75. Safety Limits vs User Intent

Safety limits should prevent accidental explosions, not silently reduce requested scale.

If a limit is exceeded:

```text
fail clearly
```

unless an explicit override is provided.

---

# 76. Configuration Error Messages

Errors should communicate:

```text
what is invalid
where it is located
why it is invalid
what is allowed
```

Example:

```text
Invalid configuration at order.basketSize.minimum:
minimum must be >= 1 and <= order.basketSize.maximum.
```

---

# 77. Configuration Testing

Configuration tests should verify:

```text
valid config loads
defaults resolve
profile resolves
scenario resolves
override precedence works
invalid values fail
unknown strategy fails
unknown profile fails
```

---

# 78. Configuration Property Tests

Useful invariants:

```text
probabilities always remain [0,1]
effective counts remain positive
profile resolution is deterministic
same configuration produces same fingerprint
```

---

# 79. Configuration Snapshot Tests

For important profiles, maintain expected effective configuration snapshots.

Example:

```text
small + baseline
medium + baseline
large + hot-product
```

This makes accidental configuration drift visible.

---

# 80. Configuration Evolution

Configuration will evolve as realism improves.

For example:

```text
old:
product-pricing

new:
product {
  pricing { ... }
}
```

Migration should be deliberate.

---

# 81. Backward Compatibility

The project does not need to support every historical configuration forever.

But breaking changes should be explicit.

When practical, provide:

```text
migration notes
```

or:

```text
clear startup errors
```

---

# 82. Deprecated Settings

If a setting is no longer used, remove it rather than keeping dead configuration indefinitely.

The project currently has an obsolete Address configuration concept that should eventually be cleaned up.

That cleanup should be handled separately from introducing new configuration architecture.

---

# 83. Configuration Documentation

Every meaningful setting should document:

```text
name
type
default
meaning
valid range
example
owner
```

---

# 84. Configuration Naming

Names should communicate business meaning.

Good:

```text
ordersPerCustomer
returnProbability
productPopularity
sessionActivity
```

Weak:

```text
factor1
mode2
x
multiplier
```

unless the context makes the meaning explicit.

---

# 85. Units Must Be Explicit

For time values, distinguish:

```text
seconds
minutes
hours
days
```

Do not rely on ambiguous numeric fields.

Example:

```text
deliveryDelayDays
```

is clearer than:

```text
deliveryDelay
```

---

# 86. Percentages vs Probabilities

Use one consistent convention.

Recommended:

```text
probability = 0.15
```

rather than:

```text
percentage = 15
```

unless the configuration intentionally uses human-readable percentages.

---

# 87. Currency Configuration

Monetary configuration should specify:

```text
currency
```

where relevant.

The current business context assumes Indian e-commerce and INR-oriented monetary realism.

---

# 88. Configuration and Localization

Geographic reference data should not be hard-coded into domain configuration.

For example:

```text
cities
states
postal codes
```

belong in Geography reference data.

---

# 89. Reference Data Configuration

Configuration should specify:

```text
where reference data is loaded from
```

rather than embedding thousands of reference records in HOCON.

---

# 90. Reference Data Version

The manifest should identify the reference-data version or fingerprint.

Reference data is part of effective generation input.

---

# 91. Configuration and Code Defaults

Avoid having:

```text
default in HOCON
+
different default in Scala
+
different default in tests
```

There should be one authoritative default.

---

# 92. Avoid Hidden Constants

A domain generator should not contain:

```scala
val defaultProbability = 0.15
```

if the value is supposed to be configurable.

Constants are appropriate only for genuine invariant domain rules.

---

# 93. Configuration vs Invariant

Example invariant:

```text
probability cannot be negative
```

belongs in code validation.

Example configurable behavior:

```text
return probability = 0.15
```

belongs in configuration.

---

# 94. Configuration vs Derived Values

Not every value needs configuration.

For example:

```text
Order.total
```

should be derived from OrderItems.

Do not configure values that should be calculated from business relationships.

---

# 95. Configuration vs Randomness

Do not configure raw random seeds for every field.

The root seed should be user-controlled.

Derived streams should be deterministic.

---

# 96. Configuration vs Distribution

Configuration specifies:

```text
which distribution
what parameters
```

The distribution implementation owns:

```text
sampling algorithm
```

---

# 97. Configuration vs Strategy

Configuration specifies:

```text
which business model
```

The strategy owns:

```text
business interpretation
```

---

# 98. Configuration vs Scenario

Configuration activates:

```text
scenario
```

Scenario logic determines:

```text
how behavior changes
```

---

# 99. Configuration and SOLID — SRP

Configuration loading should have one responsibility:

```text
turn configuration source into validated typed configuration
```

It should not generate data.

---

# 100. Configuration and SOLID — OCP

Adding a new behavior strategy should ideally require:

```text
new strategy
+
factory registration
+
configuration
+
tests
```

rather than modifying every generator.

---

# 101. Configuration and SOLID — DIP

Generators depend on typed configuration abstractions/objects rather than raw configuration parsing.

---

# 102. Readability Standard

A generator should read like:

```scala
val basketSize =
  basketSizeModel.sample(customerProfile, random)
```

not:

```scala
val n =
  config.getInt("order.basket.min") +
    random.nextInt(...)
```

Configuration details should be resolved before business logic.

---

# 103. Avoid Configuration Leakage

Do not pass the entire:

```text
GenerationConfig
```

into every domain component.

Pass only the relevant typed configuration.

---

# 104. Example Dependency

Prefer:

```scala
class OrderGenerator(
  config: OrderGenerationConfig,
  basketSizeModel: BasketSizeModel
)
```

over:

```scala
class OrderGenerator(
  config: GenerationConfig
)
```

This makes dependencies obvious.

---

# 105. Configuration Contexts

If a domain requires several related configuration objects, define a focused domain configuration.

Example:

```text
OrderGenerationConfig
  ├── frequency
  ├── timing
  ├── status
  └── basket
```

---

# 106. Avoid Nested Configuration Explosion

Do not create one case class per trivial scalar.

Bad:

```text
ProbabilityConfig
ValueConfig
MinimumConfig
MaximumConfig
```

when simple fields are sufficient.

Use nested types where they communicate a meaningful concept.

---

# 107. Configuration as Domain Language

Good configuration should allow a domain expert to understand the synthetic world without reading Scala.

For example:

```hocon
customer {
  behavior {
    activityModel = "segmented"
  }
}
```

communicates intent.

---

# 108. Configuration as Contract

Configuration is effectively an API for the generator.

Therefore:

```text
names
types
defaults
semantics
compatibility
```

matter.

---

# 109. Versioned Configuration

As the project matures, the manifest may record:

```text
configurationSchemaVersion
```

This helps interpret historical datasets.

---

# 110. Schema Version vs Generator Version

These are different.

```text
generatorVersion
```

identifies implementation.

```text
configurationSchemaVersion
```

identifies configuration structure.

Both may be useful.

---

# 111. Effective Configuration Export

A generation run should eventually be able to write:

```text
effective-config.conf
```

or equivalent manifest content.

This improves reproducibility.

---

# 112. Security

Do not place secrets in generation configuration.

This generator should not need:

```text
passwords
API keys
database credentials
```

for its core function.

---

# 113. Environment-Specific Paths

Output paths may legitimately differ by environment.

If the output path does not affect generated records, it need not affect the logical-data reproducibility fingerprint.

This distinction should be documented.

---

# 114. Configuration and Output

Output format should be configurable:

```text
CSV
future Parquet
future JSON
```

but domain generation should not depend on output format.

---

# 115. Configuration and Observability

Logging level and diagnostic options can be configurable.

But observability settings should not alter business data.

---

# 116. Configuration and Performance

Performance controls may eventually include:

```text
parallelism
batch size
buffer size
```

These should not affect logical generated data where deterministic parallelism is promised.

---

# 117. Performance Configuration Boundary

Technical tuning belongs in:

```text
runtime/performance
```

not inside business domain configuration.

---

# 118. Configuration and Determinism

If:

```text
parallelism = 1
```

versus:

```text
parallelism = 8
```

changes generated records, reproducibility becomes difficult.

The architecture should strive to isolate performance settings from business randomness.

---

# 119. Profile Validation

Each profile should validate:

```text
all required domain settings exist
counts are feasible
combinations are coherent
```

---

# 120. Scenario Validation

Each scenario should validate:

```text
parameters
supported domains
conflicts
required reference data
```

---

# 121. Scenario Capability

A scenario should declare what it affects.

Conceptually:

```text
campaign_spike
  affects:
    session
    event
    order
```

This can prevent accidental changes to unrelated domains.

---

# 122. Scenario Isolation

A scenario should not silently modify:

```text
Product pricing
```

if it is documented only as a traffic scenario.

Cross-domain effects should be explicit.

---

# 123. Scenario Propagation

For meaningful scenarios:

```text
scenario
   ↓
behavior model parameters
   ↓
domain behavior
   ↓
generated relationships
```

This is preferable to direct row mutation.

---

# 124. Example: High Activity

Scenario:

```text
high_activity
```

may modify:

```text
customer activity distribution
```

which then changes:

```text
sessions
events
orders
```

---

# 125. Example: Return Spike

Scenario:

```text
return_spike
```

may modify:

```text
return propensity
```

which changes:

```text
Return records
```

while preserving normal Order generation.

---

# 126. Example: Hot Product

Scenario:

```text
hot_product
```

may modify:

```text
ProductPopularityModel
```

and therefore:

```text
OrderItem concentration
Event product-view concentration
```

---

# 127. Scenario Interaction With Customer Profile

Customer-level scenarios should ideally influence the latent profile.

Example:

```text
hot_customer
   ↓
activity multiplier
   ↓
CustomerBehaviorProfile
```

This is more coherent than separately changing:

```text
orders
sessions
events
```

with unrelated multipliers.

---

# 128. Configuration and Correlation

Configuration should expose parameters that define correlations.

Examples:

```text
activity ↔ order frequency
price sensitivity ↔ price tier
category affinity ↔ product selection
return propensity ↔ returns
```

The actual correlation algorithm belongs in behavior models.

---

# 129. Configuration and Statistical Targets

Future profiles may specify target statistical ranges.

For example:

```text
target AOV
target return rate
target conversion rate
```

These should be treated as calibration targets, not blindly forced values.

---

# 130. Calibration vs Hard Constraints

A target:

```text
return rate ≈ 8%
```

is different from:

```text
exactly 8% returns
```

The architecture should distinguish:

```text
statistical target
```

from:

```text
hard cardinality constraint
```

---

# 131. Exact Count Configuration

Some settings are naturally exact:

```text
customer count = 1,000,000
product count = 100,000
```

These should remain exact where required by GenerationPlan.

---

# 132. Statistical Configuration

Other settings should be probabilistic:

```text
return rate
conversion rate
payment method share
```

The generator should document expected sampling variance.

---

# 133. Configuration Review

Before implementation, every configuration field should be reviewed:

```text
Is it truly configurable?
Who owns it?
Is it an invariant?
Does it need typing?
Does it need validation?
Does it affect reproducibility?
```

---

# 134. Configuration Anti-Pattern: Boolean Explosion

Avoid dozens of flags:

```text
enableX
enableY
enableZ
useNewModel
useAdvancedModel
useLegacyMode
```

Prefer explicit strategies/scenarios when behavior is genuinely different.

---

# 135. Configuration Anti-Pattern: Magic Strings

Avoid arbitrary strings where a typed domain concept is possible.

For example:

```text
"large"
"medium"
```

may map to a sealed domain representation or validated enum-like model.

---

# 136. Scala 2.13 Compatibility

The configuration design must remain Scala 2.13 compatible.

Use ordinary Scala 2.13 case classes, sealed traits, objects, and validated constructors as appropriate.

Do not design around Scala 3-only language features.

---

# 137. Configuration Test Matrix

The test suite should eventually cover:

```text
baseline
small
medium
large
high-cardinality
clean
dirty
hot-customer
hot-product
campaign
return-spike
combined scenarios
```

where those profiles/scenarios exist.

---

# 138. Configuration Regression

When a configuration field changes meaning:

```text
update documentation
update tests
update migration notes
update manifest/schema version if necessary
```

Do not silently repurpose fields.

---

# 139. Current Configuration Cleanup

Before introducing extensive new configuration, remove obsolete concepts deliberately.

One known cleanup item is the old Address-generation configuration that no longer matches the five-field Address model.

This should be removed in a focused cleanup change rather than mixed into a large architecture migration.

---

# 140. Configuration Migration Strategy

Configuration migration should proceed in stages.

## Stage 1

Preserve current behavior.

## Stage 2

Group settings by domain.

## Stage 3

Introduce typed domain configurations.

## Stage 4

Introduce behavior strategy selection.

## Stage 5

Introduce scenario overrides.

## Stage 6

Introduce effective configuration and fingerprinting.

---

# 141. Do Not Combine Everything

Do not simultaneously change:

```text
configuration structure
randomness
customer behavior
output
validation
```

in one uncontrolled commit.

The project has enough complexity that each architectural change should remain diagnosable.

---

# 142. Migration Quality Gate

After each configuration refactor:

```text
sbt test
sbt run
```

should succeed.

Generated counts and validation results should be compared against the previous baseline unless the change intentionally alters behavior.

---

# 143. Configuration Quality Gate

The architecture is acceptable when:

- settings have clear owners,
- raw HOCON is loaded centrally,
- typed configuration reaches domains,
- invalid settings fail early,
- precedence is deterministic,
- profiles are distinct from scenarios,
- effective configuration is inspectable,
- strategy selection is centralized,
- domain dependencies are narrow,
- configuration does not contain business algorithms.

---

# 144. Final Configuration Flow

The intended flow is:

```text
                    HOCON
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
      Defaults                 Run Config
          │                       │
          └───────────┬───────────┘
                      ▼
                  Profiles
                      │
                      ▼
               Cardinality
                      │
                      ▼
                  Scenarios
                      │
                      ▼
              Explicit Overrides
                      │
                      ▼
             Effective Configuration
                      │
              ┌───────┴────────┐
              ▼                ▼
          Validation       Fingerprint
              │
              ▼
       Domain Configurations
              │
              ▼
       Strategy / Factory
              │
              ▼
       Domain Generators
```

---

# 145. Final Mental Model

Configuration answers:

> **What synthetic world do we want?**

Profiles answer:

> **How large and broadly configured is it?**

Scenarios answer:

> **What special business condition should exist?**

Distributions answer:

> **What statistical shape should the behavior have?**

Strategies answer:

> **How is that business behavior implemented?**

Generators answer:

> **How are domain records constructed?**

Validation answers:

> **Did the resulting world obey its contracts?**

---

# 146. Explicit Architecture Decisions

## Decision 1

Use HOCON as the human-facing configuration format.

## Decision 2

Load configuration centrally and convert it to typed Scala objects.

## Decision 3

Generators must not repeatedly read raw HOCON.

## Decision 4

Configuration should be owned by the domain or cross-cutting subsystem that gives it meaning.

## Decision 5

Avoid one giant configuration object as the dependency of every generator.

## Decision 6

Profiles and scenarios are distinct concepts.

## Decision 7

Configuration precedence must be explicit and deterministic.

## Decision 8

Resolve configuration into an immutable effective configuration before generation.

## Decision 9

Runtime strategy selection should be centralized through factories where polymorphism is real.

## Decision 10

Business algorithms do not belong in HOCON.

## Decision 11

Statistical behavior is configured through typed parameters and implemented by behavior models/distributions.

## Decision 12

Scenario effects should propagate through behavior models rather than arbitrary row-level mutation.

## Decision 13

Quality and skew scenarios must remain explicit and measurable.

## Decision 14

The effective configuration should eventually be fingerprinted and recorded in the manifest.

## Decision 15

Configuration evolution must be deliberate and documented.

## Decision 16

Technical runtime tuning should be separated from business configuration.

## Decision 17

Scala 2.13 remains the project standard.

---

# 147. Target Package Relationship

A domain-oriented implementation may eventually resemble:

```text
customer/
  config/
    CustomerGenerationConfig.scala

  behavior/
    CustomerBehaviorProfile.scala
    CustomerBehaviorModel.scala
    ...

  generator/
    CustomerGenerator.scala

product/
  config/
  behavior/
  generator/

order/
  config/
  behavior/
  generator/

...

config/
  ConfigLoader.scala
  EffectiveGenerationConfig.scala
  ProfileResolver.scala
  ScenarioResolver.scala
  ConfigurationValidator.scala
```

The exact number of files should follow actual complexity.

Do not create empty abstractions merely to satisfy the package diagram.

---

# 148. Relationship to Previous Architecture Documents

This document connects directly to:

```text
16 — Relationship and Domain Interaction Architecture
17 — Randomness, Distribution, and Reproducibility Architecture
```

The dependency chain is:

```text
Configuration
      ↓
Randomness / Distribution
      ↓
Behavior Models
      ↓
Relationships
      ↓
Domain Generators
```

Configuration therefore defines the inputs to the architecture without owning its business algorithms.

---

# 149. Next Documentation Stage

The next document should define:

```text
19 — Validation and Data-Quality Architecture
```

It should consolidate:

- structural validation,
- primary-key validation,
- foreign-key validation,
- business-rule validation,
- statistical validation,
- configuration validation,
- controlled corruption,
- data-quality profiles,
- quality-rule ownership,
- validation severity,
- validation reporting,
- failure vs warning policy,
- reproducibility of corruption,
- scenario interaction,
- domain-level vs global validation,
- testing strategy,
- Spark-quality workloads,
- migration and quality gates.

This completes the core architecture needed before implementation migration begins.
