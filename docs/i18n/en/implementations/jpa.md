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

### Direct JPA Or Hibernate Assembly

The JPA adapter is runtime-neutral, not a turnkey raw-Hibernate bootstrap. Outside Spring Boot,
the application creates the `EntityManagerFactory`, starts and completes each transaction, registers
the aggregate and framework entities with its persistence unit, and supplies a transaction-scoped
`AggregatePersistenceContext` to `JpaAggregateRepository`. The repository must be used only inside
that persistence context and transaction.

JPQL is portable query language over entities and fields. Hibernate translates JPQL to the selected
database dialect for ordinary queries and updates. The Inbox first-claim operation is intentionally
different: it needs an atomic insert-or-ignore equivalent, so its database-specific strategy remains
explicit instead of pretending that JPQL can express it portably.

## Outbox And Inbox Stores

Select `jfoundry-outbox-jpa-spring-boot-starter` and/or
`jfoundry-inbox-jpa-spring-boot-starter` explicitly. The JPA adapters are framework-neutral; under
Spring Boot their framework entities are mapped automatically, so applications do not add them to
their own `@EntityScan`.

Entity registration is not schema management. Copy the matching Outbox or Inbox SQL template into
the application's migration process and maintain it there. Do not rely on Hibernate schema
generation to create or evolve jfoundry tables.

The Outbox store reads a page of dispatchable candidates with JPQL and claims each record through a
compare-and-set update. A dispatcher claim token establishes ownership; publication and failure
updates for a claimed record use that token.

The JPA Inbox built-in atomic claim strategy supports PostgreSQL and MySQL only. Provide
`JpaInboxClaimStrategy` for another database product. When the product is unknown and no strategy
is supplied, Boot fails fast rather than choosing a generic dialect behavior. A user-provided
`InboxMessageStore`, `OutboxMessageStore`, or `JpaInboxClaimStrategy` takes precedence.

A custom claim strategy implements
`boolean tryClaim(EntityManager entityManager, String messageId, String consumerName, Instant now)`.
It must atomically create the `PROCESSING` row and return `false` for an existing delivery; include
a concurrent duplicate-delivery test for the target database. Raw JPA/Hibernate Outbox and Inbox
assembly must apply the transaction boundaries described by [reliable messaging](../capabilities/reliable-messaging.md).

For runtime assembly and configuration, see [Spring Boot](spring-boot.md) and the
[auto-configuration reference](../reference/spring-boot-autoconfiguration.md).
