# Repository and Read-side Ports

Aggregate repositories and read-side ports solve different problems. Keeping them separate prevents
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

## LookupPort

Use a `LookupPort` for lightweight context required by an application service. It supports command
execution but does not load a full aggregate.

## ReadModelPort

Use a `ReadModelPort` for UI pages, lists, projections, reports, exports, and statistics. It can
return DTOs or read models optimized for the query use case.

## MaintenancePort

Use a `MaintenancePort` for operational scanning, cleanup, retry, repair, or batch candidates. It
usually returns IDs, windows, or lightweight candidates instead of aggregates.

## ArchUnit Relationship

`JFoundryRules.aggregateRepositoryConventions()` recognizes both jMolecules `Repository` and
jfoundry `AggregateRepository` interfaces and can prevent them from
exposing common persistence and paging types. It is optional because read-side naming conventions
remain project-specific.
