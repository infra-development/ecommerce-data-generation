# 14 — Session Domain

## 1. Purpose

This document defines the business meaning, lifecycle, customer relationship, activity behavior, session frequency, device and acquisition-channel behavior, duration, conversion context, configuration, generation strategy, validation, statistical realism, scenario behavior, architecture, testing, and migration plan for the **Session** domain in the ShopSphere e-commerce data generator.

Session represents a period of customer interaction with the ShopSphere digital experience.

The central behavioral relationship is:

```text
Customer
   ↓
Session
   ↓
Events
   ↓
Purchase intent
   ↓
Order
```

This makes Session one of the most important behavioral domains in the project.

The central architectural principle is:

> **Session represents a bounded period of customer activity; it should be generated from customer behavior and temporal context, while Events represent the actions occurring inside the Session and Orders represent completed commercial transactions.**

---

# 2. Business Meaning

A Session answers:

> "What period of digital activity did this customer have on ShopSphere?"

A Session can provide context for:

- customer activity,
- visit frequency,
- device usage,
- acquisition channel,
- session duration,
- engagement,
- conversion opportunity,
- event generation.

The exact persisted attributes must follow the current Session model.

Do not add production analytics fields merely because real websites often contain them.

---

# 3. Current Baseline

The current generation plan produces approximately:

```text
1,000 Customers
3,000 Sessions
24,000 Events
```

The current baseline therefore has:

```text
3 Sessions / Customer
8 Events / Session
```

The observed statistics are exactly:

```text
Sessions/customer:
mean   = 3.00
min    = 3
max    = 3

Events/session:
mean   = 8.00
min    = 8
max    = 8
```

This is structurally convenient but behaviorally too deterministic for the project's long-term realism target.

---

# 4. Session Ownership

## Session owns

- Session identity,
- Customer relationship,
- session-level context,
- session start/end or duration where modeled,
- device/channel context where modeled.

## Event owns

- individual customer actions.

## Customer owns

- customer identity and long-term behavioral profile.

## Order owns

- completed commercial transaction.

Session should not own Event internals or Order generation.

---

# 5. Customer Relationship

The core relationship is:

```text
Customer
   │
   │ 1
   │
   └──────────< Session
```

Conceptually:

```text
Customer 1 → N Session
```

A customer may have:

```text
0 sessions
1 session
few sessions
many sessions
```

depending on lifecycle and activity.

---

# 6. Why Session Matters

Session is the bridge between:

```text
latent customer behavior
```

and:

```text
observable digital activity
```

The customer profile might contain:

```text
activity propensity
purchase frequency
device preference
channel preference
```

Session makes those behaviors observable.

---

# 7. Customer Activity Model

A future CustomerBehaviorProfile should include or imply:

```text
activity propensity
```

which determines session frequency.

Conceptually:

```text
CustomerBehaviorProfile
        ↓
SessionFrequencyModel
        ↓
sessions/customer
```

This replaces the current fixed:

```text
3 sessions/customer
```

model.

---

# 8. Heterogeneous Session Frequency

A realistic customer population should contain:

```text
inactive customers
occasional visitors
regular visitors
highly active customers
```

For example:

```text
Customer A → 1 session
Customer B → 5 sessions
Customer C → 50 sessions
```

The exact values are illustrative.

The important property is:

> Session frequency should vary substantially across customers.

---

# 9. Session Frequency Distribution

Potential models include:

```text
Poisson
negative binomial
zero-inflated
geometric
empirical
mixture distribution
```

A negative-binomial or mixture model may be useful when activity is overdispersed.

The final choice should be calibrated against intended business behavior.

---

# 10. Customer Lifecycle

Session frequency should eventually depend on customer lifecycle.

Possible states:

```text
new
active
loyal
dormant
churned
reactivated
```

Conceptually:

```text
lifecycle
    ↓
activity propensity
    ↓
session frequency
```

This allows inactive/churned customers to have fewer sessions than highly engaged customers.

---

# 11. Session Timestamp

Session timestamps should eventually be generated from customer activity rather than assigned independently.

Conceptually:

```text
Customer activity
       ↓
active time period
       ↓
session start
       ↓
session duration
       ↓
session end
```

---

# 12. Temporal Behavior

Session activity can vary with:

```text
day of week
hour of day
month
season
campaign
holiday
```

This creates realistic traffic patterns.

---

# 13. Day-of-Week Effects

A future model can distinguish:

```text
weekday
weekend
```

with configurable activity multipliers.

The exact effect depends on the ShopSphere scenario.

---

# 14. Hour-of-Day Effects

Session starts can have an hour-of-day distribution.

For example:

```text
low overnight activity
higher daytime activity
higher evening activity
```

The generator should use a weighted distribution rather than uniform 24-hour selection if this realism is enabled.

---

# 15. Session Duration

Session duration should be variable.

A mature distribution may contain:

```text
very short sessions
normal sessions
long sessions
rare extremely long sessions
```

The exact distribution can be:

```text
log-normal
gamma
empirical
mixture
```

depending on calibration.

---

# 16. Duration vs Event Count

Session duration and Event count should eventually correlate.

For example:

```text
short session
    → fewer events

long session
    → potentially more events
```

This relationship should remain probabilistic.

---

# 17. Event Count

The current baseline has:

```text
8 Events / Session
```

for every Session.

This is too deterministic.

Future Event count should be generated from:

```text
session duration
+
customer activity
+
session intent
+
random variation
```

---

# 18. Session Intent

A useful future concept is:

```text
SessionIntent
```

Possible behavioral states:

```text
browse
research
purchase
support
re-engagement
```

These should be latent generation concepts unless the final data model explicitly requires them.

---

# 19. Browse Session

A browse-oriented Session may produce:

```text
landing
category view
product view
search
```

but not necessarily:

```text
checkout
payment
order
```

---

# 20. Purchase-Oriented Session

A purchase-oriented Session can produce:

```text
landing
product view
add to cart
checkout
payment
order
```

This is the future bridge between Session and Order.

---

# 21. Conversion Probability

A mature Session model can expose:

```text
conversion probability
```

based on:

```text
customer behavior
session intent
engagement
product/category affinity
```

Conceptually:

```text
Session
   ↓
engagement
   ↓
conversion probability
   ↓
Order
```

---

# 22. Session → Order Relationship

The baseline may generate Sessions and Orders independently.

Future realism should establish:

```text
Session
   ↓
purchase intent
   ↓
conversion
   ↓
Order
```

This creates causal structure.

---

# 23. Session Conversion

Not every Session should convert.

Expected behavior:

```text
many sessions
   ↓
few converting sessions
```

This is much more realistic than:

```text
1 Session = 1 Order
```

or independent Order generation with no relationship.

---

# 24. Orders per Session

Useful future statistics:

```text
orders/session
converting sessions / sessions
```

Most sessions may have:

```text
0 orders
```

while a smaller fraction produce:

```text
1 order
```

Multiple orders per Session should only be supported if the business scenario requires it.

---

# 25. Session and Customer Order Frequency

Customer behavior should provide the common latent driver:

```text
CustomerBehaviorProfile
       │
       ├── session activity
       │
       └── purchase propensity
```

This creates a meaningful relationship:

```text
more active customer
   → more sessions
   → more purchase opportunities
   → potentially more Orders
```

---

# 26. Device

The current baseline contains device categories:

```text
DESKTOP
MOBILE
TABLET
```

Device should eventually be a customer/session behavior rather than a completely independent random attribute.

---

# 27. Customer Device Preference

A customer may have a device affinity:

```text
mobile-heavy
desktop-heavy
mixed
```

The exact representation can remain latent.

Then:

```text
Customer profile
      ↓
DeviceSelectionModel
      ↓
Session device
```

---

# 28. Device Switching

A customer should not necessarily use only one device.

For example:

```text
Customer
  Session 1 → MOBILE
  Session 2 → MOBILE
  Session 3 → DESKTOP
```

This preserves both affinity and variation.

---

# 29. Device and Session Behavior

Device can influence:

```text
session duration
event count
conversion probability
navigation behavior
```

Only implement these correlations when they have a defined business purpose.

---

# 30. Acquisition Channel

The current baseline contains channels such as:

```text
DIRECT
EMAIL
ORGANIC
PAID_SEARCH
REFERRAL
SOCIAL
```

These should eventually represent acquisition/traffic source context rather than arbitrary independent labels.

---

# 31. Channel Ownership

Acquisition channel can originate from:

```text
Customer acquisition
```

or:

```text
Session traffic source
```

These are different concepts.

The final model should distinguish them.

A customer's original acquisition channel should not automatically be identical to every future session's traffic source.

---

# 32. Session Traffic Source

A future model may represent:

```text
session traffic source
```

with probabilities influenced by:

```text
customer history
campaigns
time
```

This is more realistic than assigning one fixed channel to a customer forever.

---

# 33. Channel and Conversion

Different channels may have different conversion behavior.

Conceptually:

```text
channel
   ↓
conversion probability
```

This should be configurable.

---

# 34. Campaign Effects

A campaign can change:

```text
traffic volume
channel distribution
product interest
conversion
```

The Session domain can consume campaign context from a scenario/temporal layer.

---

# 35. Session Lifecycle

A simple Session lifecycle is:

```text
START
  ↓
ACTIVE
  ↓
END
```

There may be:

```text
timeout
abandonment
```

depending on the session model.

---

# 36. Session Timeout

A session can end because:

```text
customer leaves
```

or:

```text
inactivity timeout
```

The exact semantics depend on how Session is defined.

---

# 37. Session Start and End

If the model contains both timestamps:

```text
startTime < endTime
```

must hold.

Duration should be:

```text
endTime - startTime
```

rather than an unrelated independently generated field.

---

# 38. Session Duration and Customer Activity

Customer activity can influence duration.

For example:

```text
high engagement
   → longer expected sessions
```

but with substantial random variation.

---

# 39. Session Intent and Duration

Potential relationship:

```text
research session
    → longer

quick browse
    → shorter
```

Again, this is a future probabilistic behavior.

---

# 40. Event Generation Boundary

Events belong to the Event domain.

Session provides:

```text
session context
```

Event generation consumes it.

Avoid embedding detailed event-type generation inside SessionGenerator.

---

# 41. Session Context for Events

Useful Session context may include:

```text
customer
device
channel
start time
end time
intent
```

where these concepts exist.

EventGenerator should use this context to create a coherent event stream.

---

# 42. Event Ordering

Events within a Session should be temporally ordered.

Conceptually:

```text
Session start
    ↓
Event 1
    ↓
Event 2
    ↓
Event 3
    ↓
Session end
```

This is a cross-domain validation concern.

---

# 43. Event Count and Session Duration

A useful generation flow is:

```text
Session duration
       ↓
event count model
       ↓
event timestamps
```

rather than:

```text
random event count
+
random event timestamps
```

with no relationship.

---

# 44. Session Engagement

A future latent engagement score can influence:

```text
duration
event count
product views
cart activity
conversion
```

This could be part of CustomerBehaviorProfile or SessionContext.

---

# 45. Customer Activity Profile

The proposed behavioral architecture can contain:

```text
activity propensity
engagement tendency
purchase frequency
spending tendency
device affinity
channel affinity
```

Session consumes the activity-related dimensions.

---

# 46. Session Frequency Strategy

A meaningful strategy abstraction:

```text
SessionFrequencyModel
```

answers:

> "How many sessions does this customer generate over the generation period?"

---

# 47. Session Timing Strategy

Another:

```text
SessionTimingModel
```

answers:

> "When does this customer's session occur?"

Potential inputs:

```text
customer activity
calendar
campaign
season
```

---

# 48. Session Duration Strategy

Another:

```text
SessionDurationModel
```

answers:

> "How long does the session last?"

Potential inputs:

```text
customer activity
session intent
device
```

---

# 49. Device Selection Strategy

Another:

```text
DeviceSelectionModel
```

answers:

> "Which device does the customer use for this session?"

---

# 50. Channel Selection Strategy

Another:

```text
ChannelSelectionModel
```

answers:

> "What traffic/acquisition source produced this session?"

---

# 51. Conversion Strategy

A future:

```text
SessionConversionModel
```

can determine whether the session produces a purchase.

---

# 52. Strategy Pattern

These are legitimate Strategy-pattern candidates because they represent real behavioral alternatives.

Do not create a separate strategy class for every small rule.

---

# 53. Factory Pattern

Factories can select:

```text
SessionFrequencyModel
SessionTimingModel
SessionDurationModel
DeviceSelectionModel
ChannelSelectionModel
SessionConversionModel
```

implementations based on configuration.

Only introduce factories when multiple implementations exist.

---

# 54. Composition

Preferred structure:

```text
SessionGenerator
   ├── SessionFrequencyModel
   ├── SessionTimingModel
   ├── SessionDurationModel
   ├── DeviceSelectionModel
   ├── ChannelSelectionModel
   └── SessionConversionModel
```

rather than a monolithic SessionGenerator.

---

# 55. Dependency Injection

Constructor injection is sufficient.

Conceptually:

```scala
class SessionGenerator(
    frequencyModel: SessionFrequencyModel,
    timingModel: SessionTimingModel,
    durationModel: SessionDurationModel,
    deviceModel: DeviceSelectionModel,
    channelModel: ChannelSelectionModel
)
```

No DI framework is required.

---

# 56. Builder Assessment

Builder is generally unnecessary for Session.

A simple immutable case class remains preferable.

---

# 57. State Pattern Assessment

A State pattern is not necessary for the baseline.

Session usually has a simple lifecycle.

Use a richer state abstraction only if session behavior becomes operationally complex.

---

# 58. Domain Service Assessment

Session may eventually participate in a behavioral orchestration service:

```text
Customer
   ↓
Session
   ↓
Events
   ↓
Conversion
```

The service should coordinate the workflow.

It should not absorb every domain's generation rules.

---

# 59. SOLID — Single Responsibility

SessionGenerator should not own:

```text
Event generation
Order generation
Customer creation
CSV output
statistics
```

---

# 60. SOLID — Open/Closed

New session timing or duration models should be addable without rewriting unrelated session logic.

---

# 61. SOLID — Liskov

Concrete session strategies should satisfy their contracts.

---

# 62. SOLID — Interface Segregation

Keep strategy interfaces narrow.

Avoid one:

```text
SessionBehavior
```

interface containing every possible behavior unless the domain actually needs it.

---

# 63. SOLID — Dependency Inversion

SessionGenerator should depend on meaningful behavior abstractions where variation exists.

---

# 64. Readability Standard

The code should communicate customer activity.

Preferred conceptual flow:

```scala
val sessionCount =
  frequencyModel.determine(
    customerProfile,
    generationPeriod
  )

val sessions =
  timingModel.generate(
    customerProfile,
    sessionCount
  )

sessions.map { startTime =>
  val duration =
    durationModel.determine(
      customerProfile,
      startTime
    )

  val device =
    deviceModel.select(
      customerProfile
    )

  val channel =
    channelModel.select(
      customerProfile,
      startTime
    )

  createSession(
    customer,
    startTime,
    duration,
    device,
    channel
  )
}
```

The exact implementation will depend on the final model.

The important property is that the code reads like session activity.

---

# 65. Reproducibility

Session generation must satisfy:

```text
same seed
+
same configuration
+
same customer population
+
same behavioral models
```

→ same sessions.

Derived random streams should isolate:

```text
session count
timing
duration
device
channel
conversion
```

---

# 66. Randomness Isolation

Prefer:

```text
random.derive(customerId)
```

then:

```text
random.derive("session-frequency")
random.derive("session-timing")
random.derive("session-duration")
random.derive("device")
random.derive("channel")
```

This reduces accidental coupling between independent decisions.

---

# 67. Temporal Reproducibility

Time generation must use an explicit generation period.

Avoid dependence on:

```text
system clock
```

unless the generator explicitly supports a real-time mode.

A fixed generation window makes datasets reproducible.

---

# 68. Session Frequency Validation

Measure:

```text
sessions/customer
```

including:

```text
mean
median
p75
p90
p95
p99
max
```

The target is heterogeneous behavior.

---

# 69. Session Duration Validation

Measure:

```text
duration distribution
```

including:

```text
mean
median
p95
p99
max
```

This identifies whether sessions are too uniform.

---

# 70. Event Count Validation

Measure:

```text
events/session
```

and ensure it is no longer exactly constant when variable behavior is enabled.

---

# 71. Device Distribution Validation

Compare:

```text
configured device weights
```

with:

```text
observed device distribution
```

and also examine:

```text
device distribution by customer
```

when customer affinity exists.

---

# 72. Channel Distribution Validation

Measure:

```text
channel share
channel by time
channel by customer
channel conversion
```

where those concepts are implemented.

---

# 73. Session Conversion Validation

If conversion modeling exists:

```text
converting sessions / sessions
```

should be measurable.

Also:

```text
conversion rate by device
conversion rate by channel
conversion rate by customer segment
```

can validate intended behavior.

---

# 74. Customer Activity Validation

A high-activity customer segment should produce statistically more sessions than a low-activity segment.

This validates the CustomerBehaviorProfile → Session relationship.

---

# 75. Temporal Validation

Useful metrics:

```text
sessions/hour
sessions/day
sessions/day-of-week
sessions/month
```

The expected shape should match configured temporal effects.

---

# 76. Session/Event Temporal Validation

Verify:

```text
sessionStart <= eventTime <= sessionEnd
```

for all Events associated with a Session.

This is a cross-domain invariant.

---

# 77. Session/Order Validation

When session-to-order attribution exists:

```text
orderTime >= sessionStart
```

and ideally:

```text
orderTime <= sessionEnd
```

if conversion occurs within the Session.

The exact rule depends on the attribution model.

---

# 78. Data Quality

Potential Session defects:

```text
duplicate session ID
unknown customer ID
invalid device
invalid channel
end before start
negative duration
events outside session
```

These should be injected by the Quality Engine.

---

# 79. Clean Baseline

The clean Session generator should guarantee:

```text
unique Session ID
valid Customer reference
valid start/end
valid device
valid channel
```

and:

```text
duration is coherent with timestamps
```

where duration is stored.

---

# 80. Skew

Session naturally supports customer activity skew.

A small number of highly active customers may generate:

```text
many sessions
```

while most customers generate relatively few.

This is useful for Spark workloads.

---

# 81. Hot Customer Session Scenario

A scenario can deliberately create:

```text
hot customers
    ↓
many Sessions
    ↓
many Events
```

This produces cascading skew.

---

# 82. Hot Device/Channel Scenario

A scenario can also concentrate sessions around:

```text
one device
```

or:

```text
one channel
```

This can support aggregation-skew experiments.

---

# 83. Campaign Traffic Spike

A campaign scenario can create:

```text
campaign
   ↓
session volume spike
   ↓
event volume spike
   ↓
potential conversion spike
```

This is a valuable temporal workload.

---

# 84. Session Data Quality Scenarios

Possible scenarios:

```text
timestamp corruption
invalid device values
invalid channel values
orphan sessions
duplicate sessions
```

The Quality Engine should apply these intentionally.

---

# 85. Output Boundary

Session should not write:

```text
sessions.csv
```

directly.

The output layer owns serialization.

---

# 86. Statistics Boundary

Session generation should not calculate global:

```text
sessions/customer
average duration
conversion rate
```

The statistics layer owns those measurements.

---

# 87. Manifest Boundary

The manifest records:

```text
session row count
schema metadata
generation metadata
```

Session generation does not own manifest construction.

---

# 88. Spark Performance Laboratory

Session is highly valuable for Spark experiments.

Potential workloads include:

```text
Session ↔ Customer
Session ↔ Event
Session ↔ Order
```

and:

```text
groupBy(customer_id)
groupBy(device)
groupBy(channel)
```

---

# 89. Event Join Workloads

A common workload:

```text
Session
  JOIN Event
```

followed by:

```text
groupBy(session_id)
```

or:

```text
groupBy(customer_id)
```

This can create large one-to-many joins.

---

# 90. Customer Activity Workloads

Potential analyses:

```text
sessions/customer
events/customer
session duration/customer
conversion/customer
```

These become useful customer-behavior Spark exercises.

---

# 91. Funnel Analytics

Once Events and Orders are behaviorally connected, Spark can analyze:

```text
Sessions
   ↓
Product Views
   ↓
Cart
   ↓
Checkout
   ↓
Payment
   ↓
Order
```

This creates realistic funnel analytics.

---

# 92. Temporal Spark Workloads

Potential workloads:

```text
sessions/hour
sessions/day
sessions by campaign
conversion by day
event volume by hour
```

This supports time-based aggregation and windowing.

---

# 93. Session Skew Workloads

Hot customers create:

```text
customer_id
    ↓
large number of Sessions
```

and then:

```text
customer_id
    ↓
even larger number of Events
```

This allows multi-stage skew analysis.

---

# 94. Current Simplifications

The current Session implementation simplifies:

- session frequency,
- activity heterogeneity,
- session duration,
- session timestamps,
- device affinity,
- channel behavior,
- session intent,
- event count,
- event/session correlation,
- conversion,
- customer lifecycle,
- temporal seasonality,
- campaign traffic,
- customer/session attribution.

These are planned realism improvements.

---

# 95. Planned Realism Sequence

Recommended progression:

```text
1. Preserve current Session model
2. Introduce variable sessions/customer
3. Add customer activity propensity
4. Add realistic session timing
5. Add session duration distribution
6. Add device affinity
7. Add channel behavior
8. Make event count depend on session behavior
9. Introduce session intent
10. Connect Session → Events
11. Connect Session → Order conversion
12. Add campaign/seasonality
13. Add customer lifecycle
```

---

# 96. Migration Strategy

## Step 1

Inspect the current Session model.

## Step 2

Inspect current SessionGenerator.

## Step 3

Inspect Session configuration.

## Step 4

Inspect Customer relationship.

## Step 5

Inspect Device and Channel generation.

## Step 6

Inspect timestamp/duration generation.

## Step 7

Inspect Event dependency.

## Step 8

Define target responsibilities.

## Step 9

Move model into:

```text
session/model
```

## Step 10

Move generation into:

```text
session/generator
```

## Step 11

Introduce behavior strategies where justified.

## Step 12

Update Event orchestration.

## Step 13

Update future Order conversion orchestration.

## Step 14

Update validation.

## Step 15

Update statistics.

## Step 16

Run focused Session tests.

## Step 17

Run Event integration tests.

## Step 18

Run complete suite.

## Step 19

Run end-to-end generation.

## Step 20

Compare Session statistics with baseline.

---

# 97. Migration Quality Gate

Session migration is complete when:

### Business

- Session meaning is explicit.
- Customer relationship is explicit.
- activity behavior is defined.
- temporal semantics are defined.

### Architecture

- Session does not generate Events internally.
- Session does not generate Orders internally.
- Session does not own Customer creation.
- Session does not write output.
- Session does not own global statistics.

### Data

- IDs unique,
- Customer references valid,
- timestamps coherent,
- device valid,
- channel valid.

### Behavior

- session frequency has a clear owner,
- timing has a clear owner,
- duration has a clear owner,
- device has a clear owner,
- channel has a clear owner.

### Integration

- Event generation receives valid Session context,
- future conversion logic has a clear boundary.

### Testing

- focused tests pass,
- Event integration tests pass,
- complete suite passes.

### Reproducibility

- same inputs produce the same Sessions.

---

# 98. Design Decisions

## Decision A — Session is a behavioral domain

It represents customer activity rather than a completed business transaction.

---

## Decision B — Session frequency should eventually be customer-driven

The current fixed three sessions/customer is a baseline simplification.

---

## Decision C — Session duration is variable

Realistic sessions should not all have identical durations.

---

## Decision D — Device is behavioral context

Device selection should eventually reflect customer affinity while preserving variation.

---

## Decision E — Channel represents traffic context

Acquisition source should not automatically be treated as a permanent customer attribute.

---

## Decision F — Event generation belongs to Event

Session provides context; Event owns actions.

---

## Decision G — Session-to-Order conversion is a future causal relationship

Orders should eventually emerge from purchase-oriented sessions rather than being completely independent.

---

## Decision H — Time is first-class

Session timing should reflect customer activity and temporal effects.

---

## Decision I — Skew is scenario-controlled

Hot customers and traffic spikes should be deliberate scenarios.

---

# 99. Final Mental Model

The simple baseline:

```text
Customer
   ↓
Session
   ↓
Events
```

The behavioral model:

```text
CustomerBehaviorProfile
        │
        ├── activity propensity
        ├── device affinity
        ├── channel affinity
        └── purchase propensity
                │
                ▼
          SessionFrequency
                │
                ▼
          SessionTiming
                │
                ▼
         SessionDuration
                │
        ┌───────┴────────┐
        ▼                ▼
     Device           Channel
        │                │
        └───────┬────────┘
                ▼
             Session
                │
                ▼
              Events
                │
                ▼
          Purchase Intent
                │
                ▼
              Order
```

---

# 100. Summary

Session represents a bounded period of digital customer activity and is the key behavioral bridge between Customers, Events, and eventually Orders.

The current baseline has:

```text
1,000 Customers
3,000 Sessions
24,000 Events
```

with deterministic:

```text
3 Sessions / Customer
8 Events / Session
```

This is structurally useful but not sufficiently realistic.

The major realism improvements are:

```text
customer activity propensity
        ↓
heterogeneous session frequency
        ↓
realistic session timing
        ↓
variable duration
        ↓
device/channel affinity
        ↓
session intent
        ↓
variable event count
        ↓
session-to-order conversion
        ↓
campaign/seasonality
        ↓
customer lifecycle
```

The target architecture is:

```text
SessionGenerator
   ├── SessionFrequencyModel
   ├── SessionTimingModel
   ├── SessionDurationModel
   ├── DeviceSelectionModel
   ├── ChannelSelectionModel
   └── SessionConversionModel
```

with Event generation remaining a separate domain.

The most important principle is:

> **A Session should look like a realistic period of customer activity generated by a person's behavior and the surrounding temporal environment, not like a fixed record count mechanically attached to every Customer.**

The next domain is **Event**, which is the deepest behavioral domain: event taxonomy, event sequencing, funnel state, timestamps, session context, conversion paths, device/channel effects, and realistic customer journeys.
