# JPA Implementation

This implementation satisfies the [aggregate persistence](../capabilities/aggregate-persistence.md)
and [reliable messaging](../capabilities/reliable-messaging.md) contracts using Jakarta Persistence.

## Aggregate Persistence

The supported default is one JPA-managed entity graph for one aggregate. `JpaAggregateRepository`
loads that graph with `EntityManager.find`, tracks the managed root, applies aggregate changes to
the same graph, and flushes before reporting success. It never calls `merge`; load, domain behavior,
and `modify(...)` must remain in one transaction and persistence context.

`JpaAggregateMapper` creates and restores the entity graph, converts IDs, and synchronizes current
aggregate state to the managed graph. It does not provide generic manual synchronization across
multiple tables or entity graphs.

For optimistic concurrency, put `@Version` on the entity-graph root and ensure every graph mutation
changes a persistent root attribute. Child-only changes do not participate merely because the root
has `@Version`; the mapper may explicitly touch the root while applying a child change. A concurrent
root update is reported as `ConflictException` at repository flush.

Use `jfoundry-jpa-spring-boot-starter` for JPA business runtime assembly. It does not add Outbox or
Inbox stores.

## Outbox And Inbox Stores

Select `jfoundry-outbox-jpa-spring-boot-starter` and/or
`jfoundry-inbox-jpa-spring-boot-starter` explicitly. The JPA adapters are framework-neutral; under
Spring Boot their framework entities are mapped automatically, so applications do not add them to
their own `@EntityScan`.

The Outbox store reads a page of dispatchable candidates with JPQL and claims each record through a
compare-and-set update. A dispatcher claim token establishes ownership; publication and failure
updates for a claimed record use that token.

The JPA Inbox built-in atomic claim strategy supports PostgreSQL and MySQL only. Provide
`JpaInboxClaimStrategy` for another database product. When the product is unknown and no strategy
is supplied, Boot fails fast rather than choosing a generic dialect behavior. A user-provided
`InboxMessageStore`, `OutboxMessageStore`, or `JpaInboxClaimStrategy` takes precedence.

For runtime assembly and configuration, see [Spring Boot](spring-boot.md) and the
[auto-configuration reference](../reference/spring-boot-autoconfiguration.md).
