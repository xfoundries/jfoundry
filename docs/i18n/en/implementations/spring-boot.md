# Spring Boot Runtime Assembly

Start with `jfoundry-spring-boot-starter` for Spring Boot auto-configuration and Spring
`TransactionRunner` integration. It remains deliberately small: it does not add persistence stores,
brokers, Outbox, Inbox, JobRunr, or Redisson.

Add capability starters explicitly: use `jfoundry-jpa-spring-boot-starter` or
`jfoundry-mybatis-plus-spring-boot-starter` for business aggregate persistence, then add the
matching Outbox or Inbox store starter only when [reliable messaging](../capabilities/reliable-messaging.md)
needs it. Add an event, messaging, broker, locking, or Web MVC starter only for the corresponding
capability.

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
