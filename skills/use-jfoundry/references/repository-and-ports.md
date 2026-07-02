# Repository And Port Guidance

## Aggregate Repository

Use an aggregate repository for aggregate lifecycle and command-side aggregate loading:

- Load by aggregate ID.
- Load by stable business identity.
- Save or remove aggregates.
- Load an aggregate because the current command will immediately invoke aggregate behavior.

Name methods by domain intent, not SQL shape. Prefer `findCurrentOperation(...)` over condition-list method names.

## LookupPort

Use a `LookupPort` when an application service needs context for a workflow but will not modify the loaded object:

- Tenant, environment, account, or application key lookup.
- Existence checks for related objects.
- Lightweight context for permission checks, command enrichment, or external SDK calls.

Return lightweight records or DTOs, not MyBatis/JPA data objects.

## ReadModelPort

Use a `ReadModelPort` for query use cases, page views, dashboards, reports, list screens, projections, and read shapes that differ from write aggregates.

CQRS is useful when commands and reads have different models, performance needs, or consistency expectations. Do not introduce CQRS just because a method is read-only.

## MaintenancePort

Use a `MaintenancePort` for technical scanning and background maintenance:

- Find timed-out processing records.
- Find expired IDs to clean up.
- Select retry, repair, or cleanup candidates.

If business invariants are involved, return IDs and let the application service load aggregates and invoke domain behavior.

## Migration Order

When replacing Active Record, MyBatis-Plus `IService`, generic `Wrapper`, or specification-style queries:

1. Ask whether the query exists to modify an aggregate.
2. If yes, load by aggregate ID or stable business identity when possible.
3. If the result prepares workflow context, use `LookupPort`.
4. If the result serves UI, reporting, list, or page reads, use `ReadModelPort`.
5. If the result serves background scan, cleanup, or repair, use `MaintenancePort`.
6. If one old method serves commands and queries, split it.

## Forbidden Leaks

Aggregate repository interfaces should not expose:

- MyBatis-Plus `Wrapper`, `IPage`, `Page`, `BaseMapper`, or `IService`.
- Spring Data `Page`, `Pageable`, `Repository`, or JPA `Specification`.
- Persistence data objects or mapper types.
- Page DTOs or reporting projections that are not aggregates.

Add `JFoundryRules.aggregateRepositoryConventions()` when the project is ready to enforce these conventions.

