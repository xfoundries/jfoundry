# Spring Boot Runtime Assembly

Start with `jfoundry-spring-boot-starter` for Spring Boot auto-configuration and Spring
`TransactionRunner` integration. It remains deliberately small: it does not add persistence stores,
brokers, Outbox, Inbox, JobRunr, or Redisson.

Add capability starters explicitly: use `jfoundry-jpa-spring-boot-starter` or
`jfoundry-mybatis-plus-spring-boot-starter` for business aggregate persistence, then add the
matching Outbox or Inbox store starter only when [reliable messaging](../capabilities/reliable-messaging.md)
needs it. Add an event, messaging, broker, locking, or Web MVC starter only for the corresponding
capability.

For reliable messaging, `jfoundry-outbox-spring-boot-starter` adds the Outbox runtime; select a
MyBatis-Plus or JPA Outbox store starter separately. `jfoundry-inbox-spring-boot-starter` adds the
Inbox runtime; select its store separately. `jfoundry-messaging-spring-boot-starter` supplies the
default Jackson `PayloadSerializer`, which writes ISO-8601 time values and ordinary JSON values
without default-typing metadata or Java class names. A user `PayloadSerializer` takes precedence.

The Outbox dispatcher defaults to `scheduled`. `jobrunr` selects JobRunr dispatch while retaining
lightweight scheduled maintenance, and `none` registers no dispatcher, recovery, or cleanup jobs.
The dispatcher, recovery, cleanup, and the auto-configured Inbox template require a
`TransactionRunner`; the standard starter creates one when Spring Boot provides a
`PlatformTransactionManager`. Use the reference for the corresponding properties and conditions.

### Writing And Delivering Outbox Messages

First, copy the matching Outbox SQL template into the application's migration process. Starters map
and use the table but never run framework SQL.

Then choose how an application writes Outbox messages. Automatic domain-event externalization is
off by default; set `jfoundry.domain.event.dispatch.outbox.enabled=true` only when explicitly
externalized domain events are the intended integration contract. Otherwise, keep the default and
append an explicitly translated integration event through `OutboxTemplate` inside the business
transaction.

Before enabling delivery, add a broker-specific starter or provide a real `MessageSender`. Without
one, jfoundry does not register an Outbox dispatcher, so messages remain pending until an explicit
delivery capability is configured.

Auto-configuration supplies defaults only when their prerequisites are available. Application beans
override the relevant defaults, including `TransactionRunner`, `PersistenceFailureTranslator`,
`AggregatePersistenceContext`, `MessageSender`, `PayloadSerializer`, Outbox/Inbox stores, and the
JPA Inbox claim strategy. Do not place framework SQL in an auto-run migration path.

Use the [auto-configuration reference](../reference/spring-boot-autoconfiguration.md) to look up
the exact starter catalog, properties, and conditions. The capability contracts remain in
[aggregate persistence](../capabilities/aggregate-persistence.md),
[reliable messaging](../capabilities/reliable-messaging.md),
[application transactions](../capabilities/application-transactions.md), and
[distributed locks](../capabilities/distributed-locks.md).
