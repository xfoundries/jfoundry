# Spring Boot 运行时装配

从 `jfoundry-spring-boot-starter` 开始，以获得 Spring Boot 自动配置和 Spring `TransactionRunner` 集成。它刻意保持轻量：不引入持久化 store、broker、Outbox、Inbox、JobRunr 或 Redisson。

按能力显式添加 starter：业务聚合持久化使用 `jfoundry-jpa-spring-boot-starter` 或 `jfoundry-mybatis-plus-spring-boot-starter`；只有[可靠消息](../capabilities/reliable-messaging.md)需要时，才添加匹配的 Outbox 或 Inbox store starter。领域事件、messaging、broker、锁和 Web MVC 也只添加对应能力的 starter。

自动配置仅在前置条件满足时提供默认值。应用 Bean 覆盖相应默认实现，包括 `TransactionRunner`、`PersistenceFailureTranslator`、`AggregatePersistenceContext`、`MessageSender`、`PayloadSerializer`、Outbox/Inbox store 和 JPA Inbox claim strategy。不要把框架 SQL 放入自动执行的 migration 路径。

精确的 starter 目录、配置项和条件见[自动配置参考](../reference/spring-boot-autoconfiguration.md)。能力契约仍位于[聚合持久化](../capabilities/aggregate-persistence.md)、[可靠消息](../capabilities/reliable-messaging.md)、[应用事务](../capabilities/application-transactions.md)和[分布式锁](../capabilities/distributed-locks.md)。
