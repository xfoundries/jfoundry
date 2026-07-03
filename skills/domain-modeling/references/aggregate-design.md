# Aggregate Design

Use this when choosing aggregate roots, entities, value objects, repositories, domain services, or consistency boundaries.

## Aggregate Roots

Create an aggregate root when a group of state and behavior must change consistently to protect business invariants.

Good signals:

- A command targets one clear consistency boundary.
- Rules require checking and changing several fields or child entities together.
- Other objects should refer to this concept by identity.
- Repository loading and saving the boundary makes the use case clearer.

Avoid aggregates that only mirror database tables. If the use case is simple CRUD with no meaningful invariant, keep the model simpler.

Aggregate methods should be named as business actions, such as `submit`, `approve`, `reserve`, `expire`, `assign`, or `cancel`. Avoid setter-driven workflows when a named behavior can express the rule.

## Entities And Value Objects

Use an entity when identity matters across state changes.

Use a value object when equality is based on values and the object expresses a concept such as money, quantity, range, code, address, schedule, policy snapshot, or limit.

Put validation close to the value object when the rule defines the value itself. Put validation in the aggregate when the rule depends on lifecycle or aggregate state.

## Domain Services

Use a domain service only for domain decisions that do not naturally belong to one aggregate or value object.

Do not move orchestration, transactions, security checks, logging, framework calls, or persistence access into a domain service. Those belong in application services or adapters.

## Repositories

Use repositories for aggregate lifecycle and command-side aggregate loading. Repository methods should express domain intent, not SQL condition shape.

Queries for pages, reports, dashboards, projections, lookup context, or maintenance scans are usually read-side concerns, not aggregate repository responsibilities.

## Aggregate Boundary Checks

- What invariant does this aggregate protect?
- Can the command complete by loading one aggregate?
- Is cross-aggregate consistency truly immediate, or can it be eventual?
- Is the aggregate too large because it is copying a screen or table shape?
- Does any child object need identity outside this aggregate?
- Are references to other aggregates by identity rather than object graph?
