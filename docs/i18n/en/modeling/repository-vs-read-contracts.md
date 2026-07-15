# Repository and Read-side Contracts

Aggregate repositories and read-side contracts solve different problems. Keeping them separate prevents
command-side models from turning into generic query services.

## Repository

Use an aggregate repository when the caller needs to load or save an aggregate for a command flow:

- load by aggregate ID or stable business identity;
- save or delete an aggregate;
- load an aggregate and immediately invoke behavior on it.

Do not put paging, dashboards, reports, query wrappers, or persistence service APIs into aggregate
repositories.

A repository has a DDD role independent of the selected architecture. In Hexagonal projects it may
also be a `@SecondaryPort` while remaining under `domain.repository`; in Onion projects it is an
inner-ring contract implemented by infrastructure. Do not duplicate the contract merely to satisfy
an architecture package convention.

## Lookup Contracts

Use a lookup contract for lightweight context required by an application service. It supports
command execution but does not load a full aggregate. Name it for the supplied fact or responsibility,
for example `AccountContextFinder`.

## Query and Read-model Contracts

Use a query or read-model contract for UI pages, lists, reports, exports, and statistics. It is
read-only: it returns DTOs or read models optimized for the query use case and does not materialize
or update them. A name such as `ExpenseClaimViewReader` describes the business view and read
responsibility.

## Projection Materialization Contracts

When CQRS uses an event or a state change to build or refresh a derived read model, model that
read-model update responsibility separately from the query that reads it. A
`PaymentStatusProjectionStore` may upsert the derived payment-status view from facts already
decided by a command or event, while an `ExpenseClaimViewReader` later reads that view for a page.
The store does not re-decide business rules or modify the aggregate.

`Projection`, `Projector`, and `ProjectionStore` are useful names only when that event- or
state-change-driven read-model materialization is actually present. They are not universal suffixes
or package names for queries, nor does their use require Event Sourcing. A CQRS read model can be
maintained from ordinary domain or integration events without retaining an event-sourced write
model.

## Maintenance Contracts

Use a maintenance contract for operational scanning, cleanup, retry, repair, or batch candidates.
It usually returns IDs, windows, or lightweight candidates instead of aggregates; names such as
`RetryCandidates` or `ExpiredClaimScanner` are more specific than `MaintenancePort`.

In Hexagonal Architecture lookup, query, maintenance, and projection-materialization contracts may
be secondary ports and may use a `*Port` suffix when that helps the project. Their infrastructure
implementations are secondary adapters. Onion Architecture does not define port or adapter roles:
the same contract is owned by an inner ring and implemented by infrastructure. `Reader`, `Store`,
`Finder`, `Provider`, and similar suffixes are responsibility-oriented Java project conventions, not
official DDD, Onion, or jfoundry patterns. Ubiquitous language remains the first source of the
name.

When technical grouping improves navigation, keep a read-only implementation in
`query.<feature>` and an event/state-change materializer in `projection.<feature>`. This is an
optional project convention, not an architecture rule: do not create a `projection` package merely
because a project has queries.

## ArchUnit Relationship

`JFoundryRules.aggregateRepositoryConventions()` recognizes both jMolecules `Repository` and
jfoundry `AggregateRepository` interfaces and can prevent them from
exposing common persistence and paging types. It is optional because read-side naming conventions
remain project-specific.
