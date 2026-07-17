# Getting Started for Business Projects

Start with the smallest architecture that serves the current business use case. jfoundry is most
useful when a system needs explicit aggregates, invariants, domain events, architecture boundaries,
or reliable external integration. A short CRUD prototype without those needs may be simpler with a
plain runtime framework and ORM.

## Start Small

- Use Java 21 and Maven.
- Choose the architecture style from domain and project constraints; jfoundry does not select
  Hexagonal or Onion for a business project.
- Put domain, application, adapters, and runtime assembly in explicit dependency boundaries.
- Add only the starters required by the chosen capabilities. Do not enable Outbox, Inbox, brokers,
  schedulers, or distributed locks until the workflow needs them.
- Add ArchUnit tests before the implementation grows around accidental dependencies.

The default Spring Boot starter is only the base runtime entry point. Persistence, reliable
messaging, brokers, locks, and Web MVC are explicit additions. The [Spring Boot runtime guide](../implementations/spring-boot.md)
explains assembly; the [reference](../reference/spring-boot-autoconfiguration.md) is the catalog
for individual starters, properties, and conditions.

## Reading Path

1. Define project boundaries with [Architecture Styles](../framework/architecture-styles.md) and
   [ArchUnit Architecture Rules](../framework/archunit-rules.md).
2. Model aggregates and choose repository/read-side contracts with
   [Repository and Read-side Contracts](../modeling/repository-vs-read-contracts.md).
3. Add [Aggregate Persistence](../capabilities/aggregate-persistence.md), then select
   [JPA](../implementations/jpa.md) or [MyBatis-Plus](../implementations/mybatis-plus.md).
4. Add [Application Transactions](../capabilities/application-transactions.md) or
   [Distributed Locks](../capabilities/distributed-locks.md) when the use case needs them.
5. Add [Reliable Messaging: Outbox And Inbox](../capabilities/reliable-messaging.md) only for
   cross-process delivery or consumer idempotency.

See [Adoption Readiness and Validated Scope](adoption-readiness.md) before relying on a capability
in production.
