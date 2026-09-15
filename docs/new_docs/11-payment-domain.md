# 11 — Payment Domain

## 1. Purpose

This document defines the business meaning, transaction role, model boundaries, payment-method behavior, payment lifecycle, failure behavior, retry behavior, customer and order relationships, configuration, generation strategy, validation, statistical realism, data-quality scenarios, architecture, testing, and migration plan for the **Payment** domain in the ShopSphere e-commerce data generator.

Payment represents the financial processing associated with an Order.

The central business relationship is:

```text
Customer
   ↓
Order
   ↓
Payment
```

Payment is not the Order itself.

An Order represents the purchase transaction.

A Payment represents an attempt or successful mechanism through which that transaction is financially processed.

The central architectural principle is:

> **Payment models the financial transaction associated with an Order and its lifecycle; it should not absorb Order, Shipment, Return, or global transaction orchestration responsibilities.**

---

# 2. Business Meaning

Payment answers:

> "How was this Order financially attempted and/or completed?"

Depending on the final model, Payment may represent:

- a successful payment,
- a failed payment,
- a payment attempt,
- a captured transaction,
- a refunded amount,
- or another payment lifecycle state.

The exact persisted semantics must follow the current Payment model.

Do not silently redefine Payment as a complete payment gateway simulation.

The generator should model only the level of payment behavior required by the ShopSphere business scenario.

---

# 3. Current Baseline

The current generation plan produces approximately:

```text
5,000 Orders
5,000 Payments
```

This establishes an initial relationship close to:

```text
1 Payment / Order
```

The current payment-method distribution is approximately:

```text
CREDIT_CARD  =   965
DEBIT_CARD   =   724
NET_BANKING  =   513
UPI          = 2,273
WALLET       =   525
```

The exact values depend on the seed and configuration.

This is a useful baseline, but it is simpler than a realistic payment lifecycle.

---

# 4. Payment Ownership

## Payment owns

- Payment identity,
- Order relationship,
- payment method,
- payment status where modeled,
- payment-specific transaction information,
- payment timing where modeled.

## Order owns

- commercial transaction,
- Order lifecycle,
- OrderItems,
- order-level business context.

## Shipment owns

- fulfillment and delivery.

## Return owns

- return process.

## Quality Engine owns

- intentional payment data corruption.

This separation prevents Payment from becoming a generic transaction manager.

---

# 5. Order Relationship

The current baseline effectively models:

```text
Order 1 → 1 Payment
```

However, the business model should distinguish:

```text
Payment
```

from:

```text
PaymentAttempt
```

A single Order may eventually have:

```text
failed attempt
     ↓
retry
     ↓
successful payment
```

Whether these become separate records depends on the final schema.

---

# 6. Payment Attempt vs Payment

This distinction is important.

A payment attempt represents:

> "An attempt was made to process money."

A successful Payment represents:

> "The transaction was successfully processed."

A realistic system may therefore have:

```text
Order
  ↓
Payment Attempts
  ├── FAILED
  ├── FAILED
  └── SUCCESSFUL
```

The current baseline does not need to implement this complexity immediately.

---

# 7. Current Simplification

The current generator appears to use approximately one Payment per Order.

This is acceptable for the structural baseline.

However, it does not naturally represent:

```text
payment failure
retry
alternate payment method
```

These are future realism capabilities.

---

# 8. Payment Methods

The current baseline includes:

```text
CREDIT_CARD
DEBIT_CARD
NET_BANKING
UPI
WALLET
```

Payment method should be modeled as a business category rather than an arbitrary string.

The exact representation should follow the current model.

---

# 9. Payment Method Distribution

The current distribution is heavily weighted toward UPI.

This may be useful for an India-focused ShopSphere scenario.

However, the configuration should remain capable of changing payment-method probabilities.

The generator should not hard-code:

```text
UPI = fixed percentage
```

inside PaymentGenerator.

---

# 10. Configurable Payment Behavior

Payment configuration should eventually control:

```text
payment method weights
success probability
failure probability
retry behavior
method-specific failure rates
processing delay
```

Only expose parameters that have a meaningful business effect.

---

# 11. Payment Method Preference

Payment method selection can eventually be influenced by customer behavior.

For example:

```text
Customer
   ↓
payment preference
   ↓
Payment method
```

A customer may have a stable preference such as:

```text
UPI-oriented
card-oriented
wallet-oriented
```

while still occasionally using another method.

---

# 12. Customer Payment Affinity

A future CustomerBehaviorProfile can contain a latent:

```text
payment method affinity
```

or an equivalent behavioral representation.

This allows:

```text
Customer A → mostly UPI
Customer B → mostly cards
Customer C → mixed methods
```

without making payment method deterministic.

---

# 13. Payment Method and Customer Geography

Payment method distribution can potentially vary by geography.

For example:

```text
geography
   ↓
payment adoption
   ↓
method probability
```

This is a future realism extension.

It should be introduced only if the project needs geography/payment correlation.

---

# 14. Payment Method and Device

Payment method may correlate with device.

Conceptually:

```text
device
   ↓
payment preference
```

For example, a mobile-heavy customer population may have different payment-method behavior from a desktop-heavy population.

The exact relationship should be modeled probabilistically.

---

# 15. Payment Method and Order Value

Payment method may also correlate with transaction value.

For example:

```text
large order
   ↓
different method probability
```

This should not be assumed universally.

If implemented, it should be a configurable behavioral relationship.

---

# 16. Payment Success

A realistic Payment model should distinguish:

```text
payment method
```

from:

```text
payment outcome
```

These are separate dimensions.

Conceptually:

```text
method
  +
transaction context
  ↓
payment outcome
```

---

# 17. Success and Failure

Potential payment outcomes:

```text
SUCCESS
FAILED
```

A richer lifecycle may include:

```text
INITIATED
PROCESSING
SUCCESS
FAILED
CANCELLED
REFUNDED
```

The exact status set must follow the final model.

Do not add all possible gateway states merely because they exist in real systems.

---

# 18. Payment State Machine

A conceptual lifecycle is:

```text
INITIATED
    │
    ▼
PROCESSING
    │
    ├────────► FAILED
    │
    ▼
SUCCESS
```

Refund is a separate financial lifecycle after success:

```text
SUCCESS
   ↓
REFUND
```

Whether Refund is a Payment status or a separate Return/refund concept must be explicitly decided later.

---

# 19. State Pattern Assessment

A State pattern is not required for the current baseline.

A simple:

```text
PaymentStatus
```

plus transition validation may be sufficient.

If future payment behavior becomes complex enough to support:

```text
multiple attempts
partial capture
authorization
capture
refund
chargeback
```

then a dedicated lifecycle abstraction may become justified.

---

# 20. Failure Modeling

Payment failure should be intentional and probabilistic.

A simplistic model:

```text
random boolean
```

is acceptable only for a very basic baseline.

A stronger model can depend on:

```text
payment method
customer behavior
order value
scenario
```

---

# 21. Method-Specific Failure

Different payment methods may have different failure probabilities.

Conceptually:

```text
PaymentMethod
      ↓
FailureProbabilityModel
      ↓
Outcome
```

The exact probabilities should be configuration-driven.

---

# 22. Order Value and Failure

Payment failure may depend weakly on transaction value.

For example:

```text
very large transaction
    → potentially different risk/failure behavior
```

This should be used carefully.

Synthetic correlations should remain plausible rather than creating artificial rules.

---

# 23. Retry Behavior

A failed payment can lead to:

```text
retry
```

A future model may allow:

```text
Attempt 1 → FAILED
Attempt 2 → FAILED
Attempt 3 → SUCCESS
```

The retry mechanism can change:

```text
payment method
```

or keep the same method.

---

# 24. Retry Strategy

Potential strategy:

```text
PaymentRetryModel
```

with behaviors such as:

```text
NoRetry
SingleRetry
BoundedRetry
BehaviorDrivenRetry
```

Only introduce multiple implementations when the project needs them.

---

# 25. Payment Method Switching

A realistic retry may change method.

Example:

```text
UPI
  ↓ failed
CREDIT_CARD
  ↓
SUCCESS
```

This creates richer transaction behavior.

The probability should be configurable.

---

# 26. Payment and Order Lifecycle

Payment and Order status should be coherent.

For example:

```text
Order PLACED
    ↓
Payment initiated
```

and:

```text
Order CONFIRMED
    ↓
successful payment
```

if the business scenario assumes payment is required before confirmation.

The exact rule depends on the final Order lifecycle.

---

# 27. Avoid Circular Responsibility

Do not implement:

```text
Payment decides Order status
Order decides Payment status
```

inside both generators.

Prefer a coordinating application/domain service:

```text
Order + Payment lifecycle coordination
```

while each domain owns its own state.

---

# 28. Payment and Shipment

Shipment should generally depend on successful order/payment conditions when the business scenario requires payment before fulfillment.

Conceptually:

```text
Order
  ↓
Payment success
  ↓
Shipment
```

The exact dependency may vary for:

```text
cash on delivery
pay-later
failed payment
```

if those methods are introduced.

---

# 29. Current Payment Method Scope

The current baseline does not appear to model:

```text
cash on delivery
BNPL
bank transfer
gift card
```

These should not be added automatically.

The current method set is sufficient until business requirements expand.

---

# 30. Payment and Returns

A return may eventually produce:

```text
refund
```

The relationship is conceptually:

```text
Order
  ↓
Return
  ↓
Refund
  ↓
Payment
```

The project should decide whether refund information belongs:

```text
inside Payment
```

or:

```text
as a separate financial/refund model
```

before implementing detailed refund behavior.

---

# 31. Payment and Revenue

Payment amount should correspond to the transaction amount when the payment represents the full successful purchase.

Conceptually:

```text
OrderItems
   ↓
Order Total
   ↓
Payment Amount
```

A key invariant can be:

```text
successful payment amount = order amount
```

for the simple full-payment baseline.

---

# 32. Partial Payments

Partial payment is a possible future feature.

For example:

```text
Order = ₹10,000

Payment 1 = ₹4,000
Payment 2 = ₹6,000
```

This is not required for the initial model.

If introduced, the invariant changes from:

```text
payment amount == order total
```

to:

```text
sum(successful payments) == order total
```

when the order is fully paid.

---

# 33. Currency

The current project is oriented toward an India-focused e-commerce scenario.

The current monetary model should remain consistent with the existing project assumptions.

Multi-currency support should be considered only if the business domain expands internationally.

Do not introduce currency conversion complexity prematurely.

---

# 34. Payment Timestamp

Payment timing should eventually be related to Order timing.

Conceptually:

```text
order placed
      ↓
payment initiated
      ↓
payment completed
```

Therefore payment timestamps should not be independently random.

---

# 35. Processing Delay

A future model can introduce:

```text
paymentProcessingDelay
```

based on:

```text
payment method
system behavior
random variation
```

This can produce realistic temporal relationships.

---

# 36. Payment and Session

A mature behavioral model can connect:

```text
Session
  ↓
checkout
  ↓
payment attempt
  ↓
successful payment
  ↓
Order
```

This is especially useful for funnel analysis.

The current generator does not yet need this level of coupling.

---

# 37. Conversion and Payment

Payment should be downstream of conversion.

Conceptually:

```text
Session engagement
       ↓
Checkout
       ↓
Payment attempt
       ↓
Success
       ↓
Order
```

or, depending on the business process:

```text
Order created
       ↓
Payment
       ↓
Order confirmation
```

The chosen lifecycle should be documented explicitly.

---

# 38. Payment Failure and Conversion

A failed payment should not necessarily imply that the customer never intended to purchase.

This distinction matters:

```text
checkout intent
    ≠
successful payment
```

A mature funnel can therefore measure:

```text
checkout attempts
payment attempts
successful payments
orders
```

separately.

---

# 39. Payment Configuration Architecture

Preferred flow:

```text
application.conf
       ↓
ConfigLoader
       ↓
typed Payment configuration
       ↓
Payment models
       ↓
PaymentGenerator
```

PaymentGenerator should not read configuration files directly.

---

# 40. Potential Configuration

A future HOCON structure could look conceptually like:

```hocon
payment {
  methods {
    weights {
      UPI = 0.45
      CREDIT_CARD = 0.20
      DEBIT_CARD = 0.15
      NET_BANKING = 0.10
      WALLET = 0.10
    }
  }

  outcome {
    successProbability = 0.98
  }

  retry {
    enabled = true
    maxAttempts = 2
  }
}
```

These values are illustrative only.

The actual defaults should be calibrated against the intended scenario.

---

# 41. Strategy Pattern

Payment has several legitimate strategy candidates:

```text
PaymentMethodSelectionModel
PaymentOutcomeModel
PaymentRetryModel
PaymentTimingModel
```

These represent genuine business variability.

---

# 42. PaymentMethodSelectionModel

This strategy answers:

> "Which payment method does this customer use for this transaction?"

Possible implementations:

```text
WeightedPaymentMethodSelection
CustomerAffinityPaymentSelection
ContextAwarePaymentSelection
```

---

# 43. PaymentOutcomeModel

This strategy answers:

> "Does this payment attempt succeed?"

Potential inputs:

```text
payment method
order value
customer profile
scenario
```

Output:

```text
payment outcome
```

---

# 44. PaymentRetryModel

This strategy answers:

> "What happens after a failed payment?"

Potential behaviors:

```text
no retry
one retry
bounded retries
method-switch retry
```

---

# 45. PaymentTimingModel

This strategy answers:

> "When does payment processing occur relative to the Order?"

Potential inputs:

```text
order timestamp
payment method
processing behavior
```

---

# 46. Factory Pattern

Factories may select configured implementations.

Examples:

```text
PaymentMethodSelectionModelFactory
PaymentOutcomeModelFactory
PaymentRetryModelFactory
```

Do not introduce factories until there are meaningful alternative implementations.

---

# 47. Builder Assessment

Builder is generally unnecessary for Payment.

A simple immutable case class should remain the default.

A Builder becomes justified only if Payment construction becomes complex enough to require staged validation.

---

# 48. Composition

Prefer:

```text
PaymentGenerator
  ├── PaymentMethodSelectionModel
  ├── PaymentOutcomeModel
  ├── PaymentRetryModel
  └── PaymentTimingModel
```

rather than a large conditional PaymentGenerator.

---

# 49. Dependency Injection

Constructor injection is sufficient.

Conceptually:

```scala
class PaymentGenerator(
    methodModel: PaymentMethodSelectionModel,
    outcomeModel: PaymentOutcomeModel,
    retryModel: PaymentRetryModel,
    timingModel: PaymentTimingModel
)
```

No dependency-injection framework is necessary.

---

# 50. SOLID — Single Responsibility

PaymentGenerator should not own:

```text
Order generation
Shipment generation
Return generation
CSV writing
global statistics
```

---

# 51. SOLID — Open/Closed

New payment outcome behavior should be addable through strategies rather than widespread conditional modifications.

---

# 52. SOLID — Liskov

Concrete payment strategies must preserve their declared behavioral contracts.

---

# 53. SOLID — Interface Segregation

Keep payment behavior interfaces narrow.

Avoid one giant:

```text
PaymentService
```

interface containing unrelated concerns.

---

# 54. SOLID — Dependency Inversion

PaymentGenerator should depend on meaningful behavior abstractions where variation exists.

---

# 55. Readability Standard

The code should communicate payment business flow.

Conceptually:

```scala
val method =
  paymentMethodModel.select(
    customerProfile,
    orderContext
  )

val outcome =
  paymentOutcomeModel.determine(
    method,
    orderContext
  )

val attempts =
  paymentRetryModel.generate(
    method,
    outcome,
    orderContext
  )

paymentFactory.create(
  orderContext,
  attempts
)
```

The exact implementation will depend on the final model.

The important property is that the code reads like payment processing.

---

# 56. Validation

Payment-specific validation should verify:

```text
payment ID unique
order ID valid
payment method valid
payment status valid
amount valid
timestamps valid
```

Cross-domain validation should verify:

```text
payment ↔ order consistency
payment amount ↔ order total
payment lifecycle ↔ order lifecycle
```

where those rules apply.

---

# 57. Payment Amount Validation

For the simple baseline:

```text
successful payment amount
=
Order total
```

For future multiple-payment support:

```text
Σ successful payment amounts
=
Order total
```

when fully paid.

---

# 58. Payment Method Validation

Only configured methods should appear.

If configuration defines:

```text
UPI
CREDIT_CARD
DEBIT_CARD
NET_BANKING
WALLET
```

then unexpected values should fail validation.

---

# 59. Retry Validation

If retries are implemented:

```text
attemptNumber
```

should be coherent.

For example:

```text
1
2
3
```

rather than:

```text
1
4
2
```

if sequential attempts are required.

---

# 60. Temporal Validation

Payment events should follow valid time ordering.

For example:

```text
paymentInitiatedAt <= paymentCompletedAt
```

where both exist.

Order/payment ordering should also follow the defined lifecycle.

---

# 61. Statistical Validation

Payment statistics should include:

```text
payments/order
payment methods
payment outcomes
failure rate
retry count
successful payment amount
payment processing delay
```

---

# 62. Method Distribution Validation

Compare observed distribution with configured weights.

For example:

```text
configured UPI = 45%
observed UPI ≈ 45%
```

within an acceptable statistical tolerance.

Do not require exact percentages for random generation.

---

# 63. Failure Rate Validation

If the configured success probability is:

```text
98%
```

then large datasets should produce a failure rate close to:

```text
2%
```

within a reasonable confidence/tolerance range.

---

# 64. Customer Affinity Validation

If Customer A is modeled as UPI-heavy, the generator should produce a higher UPI share for that customer than for a customer without that affinity.

This validates that the behavioral model is actually connected to Payment.

---

# 65. Payment Method Concentration

Useful metrics:

```text
payment method share
top method share
customer-level method concentration
```

This can identify whether the model is too uniform.

---

# 66. Data Quality Scenarios

Potential Payment defects:

```text
duplicate payment ID
unknown order ID
invalid payment method
invalid status
negative amount
zero amount
amount mismatch
invalid timestamp
duplicate payment attempt
```

These should be introduced deliberately by the Quality Engine.

---

# 67. Clean Baseline

The clean baseline should produce:

```text
valid payment IDs
valid Order references
valid methods
valid amounts
valid status
valid time relationships
```

No accidental corruption should be required to test validation.

---

# 68. Scenario-Based Payment Failures

A scenario may intentionally increase payment failures.

For example:

```text
payment-degradation
```

could modify:

```text
PaymentOutcomeModel
```

rather than hard-coding scenario checks into PaymentGenerator.

---

# 69. Payment Failure Burst

A temporal scenario could produce:

```text
normal failure rate
       ↓
incident period
       ↓
high failure rate
       ↓
normal recovery
```

This is useful for time-series and operational Spark analysis.

---

# 70. Payment Skew

Payment itself is less naturally skewed than OrderItem, but skew can still be modeled through:

```text
payment method
customer
order value
failure scenario
```

For example, a temporary payment-system incident may concentrate failures around a specific period or method.

---

# 71. Reproducibility

Payment generation must satisfy:

```text
same seed
same configuration
same customer behavior
same order population
same reference data
```

→ same Payment records.

Derived random streams should be used for method selection, outcome, retry, and timing.

---

# 72. Parallel Generation

Payment generation can be partitioned by Order.

Conceptually:

```text
Order partition
    ↓
Payment generation
    ↓
Payment records
```

This avoids global mutable state.

---

# 73. Payment Generation Performance

At very large scale, useful optimizations include:

```text
precomputed method weights
efficient categorical sampling
precomputed outcome distributions
local random streams
batch output
```

Correctness and statistical behavior come before micro-optimization.

---

# 74. Output Boundary

Payment generation should not write:

```text
payments.csv
```

directly.

The output layer owns serialization.

---

# 75. Statistics Boundary

PaymentGenerator should not calculate global:

```text
failure rate
method distribution
retry statistics
```

The statistics layer should do this.

---

# 76. Manifest Boundary

The manifest should record:

```text
Payment row count
generation metadata
schema information
```

but Payment should not construct the manifest.

---

# 77. Spark Performance Laboratory

Payment can support useful Spark workloads:

```text
Payment ↔ Order join
groupBy(payment_method)
groupBy(payment_status)
time-series failure analysis
payment-method conversion analysis
high-value transaction analysis
```

---

# 78. Operational Incident Workload

A payment-failure scenario can create realistic analytics such as:

```text
failure rate by hour
failure rate by method
failure rate by customer segment
failure rate by order-value band
```

This can later become a Spark aggregation exercise.

---

# 79. Current Simplifications

The current Payment implementation simplifies:

- one payment per Order,
- payment attempts,
- retries,
- payment failures,
- method switching,
- processing delay,
- customer payment affinity,
- geography/payment correlation,
- device/payment correlation,
- refunds,
- partial payments,
- payment incidents.

These should be treated as planned realism extensions rather than defects in the baseline.

---

# 80. Planned Realism Sequence

Recommended progression:

```text
1. Preserve current Payment model
2. Stabilize method configuration
3. Introduce payment outcome
4. Add customer payment affinity
5. Add payment timing
6. Add retry behavior
7. Add method switching
8. Connect payment to Order lifecycle
9. Add refund/return relationship
10. Add temporal payment incidents
```

This sequence avoids introducing too many cross-domain dependencies at once.

---

# 81. Migration Strategy

## Step 1

Inspect the current Payment model.

## Step 2

Inspect current PaymentGenerator.

## Step 3

Inspect Payment configuration.

## Step 4

Inspect how Orders are selected.

## Step 5

Inspect how payment method weights are currently applied.

## Step 6

Identify lifecycle/status logic.

## Step 7

Move the Payment model into:

```text
payment/model
```

## Step 8

Move generation logic into:

```text
payment/generator
```

## Step 9

Introduce behavior strategies only where required.

## Step 10

Update transaction orchestration.

## Step 11

Update validation.

## Step 12

Update statistics.

## Step 13

Run focused Payment tests.

## Step 14

Run dependent Order tests.

## Step 15

Run the complete suite.

## Step 16

Run end-to-end generation.

## Step 17

Compare payment statistics with the baseline.

---

# 82. Migration Quality Gate

Payment migration is complete when:

### Business

- Payment meaning is explicit.
- Order relationship is explicit.
- payment methods are explicit.
- lifecycle is defined.

### Architecture

- Payment does not own Order generation.
- Payment does not own Shipment generation.
- Payment does not own Return generation.
- Payment does not write output.
- Payment does not own global statistics.

### Data

- IDs unique,
- Order references valid,
- methods valid,
- amounts coherent,
- timestamps coherent.

### Behavior

- method selection has a clear owner,
- outcome has a clear owner,
- retry has a clear owner if implemented.

### Testing

- Payment tests pass,
- Order integration tests pass,
- complete suite passes.

### Reproducibility

- same inputs produce the same logical payments.

---

# 83. Design Decisions

## Decision A — Payment is separate from Order

Order represents the commercial transaction; Payment represents financial processing.

---

## Decision B — Payment method is configurable

Payment method distribution belongs in configuration/behavior models.

---

## Decision C — Payment outcome is distinct from payment method

Method selection and success/failure are separate decisions.

---

## Decision D — One Payment per Order is a baseline simplification

The architecture should remain extensible toward multiple attempts.

---

## Decision E — Payment retries are future behavior

They should not be simulated through arbitrary duplicate Payment records.

---

## Decision F — Payment lifecycle is state-aware

Statuses should follow defined transitions.

---

## Decision G — Order and Payment lifecycle coordination belongs outside both generators

A coordinating service should orchestrate cross-domain behavior.

---

## Decision H — Payment failures can become scenario-driven

Incidents and degradation should modify the outcome model.

---

## Decision I — Payment statistics remain cross-cutting

The Payment domain generates records; the statistics layer evaluates the population.

---

# 84. Final Mental Model

The baseline is:

```text
Order
  │
  ▼
Payment
  ├── method
  ├── outcome
  ├── amount
  └── timing
```

The mature model becomes:

```text
CustomerBehaviorProfile
        │
        ▼
PaymentMethodSelectionModel
        │
        ▼
Payment Attempt
        │
        ▼
PaymentOutcomeModel
        │
        ├────────► FAILED
        │             │
        │             ▼
        │       PaymentRetryModel
        │             │
        │             └──► retry
        │
        ▼
     SUCCESS
        │
        ▼
     Order confirmation
```

with:

```text
Order
  ↓
Payment
  ↓
Shipment
```

and eventually:

```text
Return
  ↓
Refund
  ↓
Payment
```

where the final refund architecture is explicitly designed.

---

# 85. Summary

Payment is the financial-processing domain associated with an Order.

The current baseline has:

```text
5,000 Orders
5,000 Payments
```

with payment methods:

```text
CREDIT_CARD
DEBIT_CARD
NET_BANKING
UPI
WALLET
```

This provides a useful structural foundation but is currently much simpler than a realistic payment system.

The major future improvements are:

```text
payment outcomes
        ↓
customer payment affinity
        ↓
payment timing
        ↓
retry behavior
        ↓
method switching
        ↓
Order lifecycle integration
        ↓
refund relationships
        ↓
temporal payment incidents
```

The target architecture is:

```text
PaymentGenerator
   ├── PaymentMethodSelectionModel
   ├── PaymentOutcomeModel
   ├── PaymentRetryModel
   └── PaymentTimingModel
```

with cross-domain coordination handled separately.

The most important principle is:

> **Payment should represent believable financial processing of an Order without becoming a second Order generator or a generic transaction-management layer.**

The next domain is **Shipment**, where the transaction moves from financial completion into fulfillment, delivery timing, carrier behavior, geography, and operational lifecycle.
