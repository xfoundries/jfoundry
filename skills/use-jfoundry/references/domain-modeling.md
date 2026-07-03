# Domain Modeling Guidance

Use this reference for jfoundry business projects when creating or changing aggregates, entities, value objects, domain services, domain events, and command behavior. Keep it implementation-oriented; use a broader DDD architecture skill when the question is about architecture theory, source authority, or non-jfoundry ecosystems.

## Modeling Workflow

Start from use cases, not tables. For each workflow, identify:

- The business command, named as a task or intent.
- The actor and preconditions.
- The aggregate or concept that owns the decision.
- The invariant that must stay true.
- The state transition or fact produced by the decision.
- The event worth publishing after the decision, if any.

Prefer domain terms from the business language. Do not rename everything into technical vocabulary such as manager, handler, data, record, wrapper, or config unless those are real domain terms.

## Aggregate Roots

Create an aggregate root when a group of state and behavior must be changed consistently to protect business invariants.

Good aggregate-root signals:

- A command targets one clear consistency boundary.
- Rules require checking and changing several fields or child entities together.
- Other objects should refer to this concept by identity.
- Repository loading and saving the whole boundary makes the use case clearer.

Avoid creating aggregates that only mirror database tables. If the use case is simple CRUD with no meaningful invariant, keep the model simpler and do not invent behavior.

Aggregate methods should be named as business actions, for example `submit()`, `approve(...)`, `reserve(...)`, `markRetryable(...)`, or `expire(...)`. Avoid setter-driven workflows when a named behavior can express the rule.

## Entities And Value Objects

Use an entity when identity matters across state changes.

Use a value object when equality is based on values and the object expresses a concept such as money, range, quantity, code, address, or policy snapshot. Prefer Java records for immutable value objects when they fit.

Put validation close to the value object when the rule defines the value itself. Put validation in the aggregate when the rule depends on aggregate state or lifecycle.

## Domain Services

Use a domain service only for domain decisions that do not naturally belong to one aggregate or value object.

Do not move orchestration, transactions, security checks, logging, framework calls, or persistence access into a domain service. Those belong in application services or adapters.

If a service needs MyBatis, JPA, HTTP clients, message brokers, Spring components, or cache APIs, it is not a pure domain service.

## Application Services

Application services coordinate use cases:

- Load aggregates through aggregate repositories.
- Load workflow context through read-side ports.
- Call aggregate or domain-service behavior.
- Save changed aggregates.
- Trigger external publication through Outbox or message ports when needed.

Application services should not accumulate business rules that are better expressed by aggregate methods or value objects.

## Domain Events

Raise a domain event when a meaningful business fact has happened and other parts of the system may react to it.

Name events in past tense, for example `OrderSubmitted`, `PaymentAuthorized`, or `TaskExpired`. Keep payloads focused on identity and facts consumers need; do not expose persistence data objects.

Use local domain events for in-process reactions. Use Outbox only when the event must be reliably delivered outside the process or to a broker.

## Repository And Ports

Use aggregate repositories for aggregate lifecycle and command-side aggregate loading. Keep repository methods aligned with domain intent, not SQL condition shape.

Use read-side ports for workflow context, page/query models, reporting, dashboards, and maintenance scans. Split lookup, read-model, and maintenance ports only when the distinction clarifies responsibility.

## Implementation Checklist

Before writing code, produce a short modeling note:

- Aggregate roots and their identities.
- Entities and value objects.
- Commands handled by each aggregate.
- Invariants protected by each aggregate method.
- Domain events emitted.
- Repositories and read-side ports needed by application services.

When implementing, keep domain classes free of Spring, MyBatis, JPA annotations, persistence data objects, and message broker APIs. Use jMolecules or JFoundry annotations only when they improve clarity, validation, or documentation.
