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

Use a query or read-model contract for UI pages, lists, projections, reports, exports, and statistics.
It can return DTOs or read models optimized for the query use case. A name such as
`ExpenseClaimViewReader` describes the business view and read responsibility.

## Maintenance Contracts

Use a maintenance contract for operational scanning, cleanup, retry, repair, or batch candidates.
It usually returns IDs, windows, or lightweight candidates instead of aggregates; names such as
`RetryCandidates` or `ExpiredClaimScanner` are more specific than `MaintenancePort`.

In Hexagonal Architecture these contracts may be secondary ports and may use a `*Port` suffix when
that helps the project. Onion Architecture does not define port roles. `Reader`, `Store`, `Finder`,
`Provider`, and similar suffixes are responsibility-oriented Java project conventions, not official
DDD, Onion, or jfoundry patterns. Ubiquitous language remains the first source of the name.

## ArchUnit Relationship

`JFoundryRules.aggregateRepositoryConventions()` recognizes both jMolecules `Repository` and
jfoundry `AggregateRepository` interfaces and can prevent them from
exposing common persistence and paging types. It is optional because read-side naming conventions
remain project-specific.
