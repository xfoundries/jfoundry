# 业务项目接入指南

从满足当前业务用例的最小架构开始。jfoundry 最适合需要明确聚合、不变量、领域事件、架构边界或可靠外部集成的系统。短期 CRUD 原型如果没有这些需求，直接使用运行时框架和 ORM 可能更简单。

## 从最小结构开始

- 使用 Java 21 和 Maven。
- 依据领域和项目约束选择架构风格；jfoundry 不会为业务项目决定 Hexagonal 或 Onion。
- 将 domain、application、adapter 和运行时装配放在明确的依赖边界中。
- 只添加已选择能力需要的 starter；在用例需要前，不启用 Outbox、Inbox、broker、scheduler 或分布式锁。
- 在实现因偶然依赖而增长前，先添加 ArchUnit 测试。

默认 Spring Boot starter 只是基础运行时入口。持久化、可靠消息、broker、锁和 Web MVC 都是显式添加项。[Spring Boot 运行时指南](../implementations/spring-boot.md)说明装配方式；单个 starter、配置项和条件请查询[参考页](../reference/spring-boot-autoconfiguration.md)。

## 阅读路径

1. 先通过[架构风格指南](../framework/architecture-styles.md)和 [ArchUnit 架构规则](../framework/archunit-rules.md)确定项目边界。
2. 通过 [Repository 与读侧契约迁移指南](../modeling/repository-vs-read-contracts.md)建模聚合，并选择 Repository/读侧契约。
3. 加入[聚合持久化](../capabilities/aggregate-persistence.md)，再选择 [JPA](../implementations/jpa.md)或 [MyBatis-Plus](../implementations/mybatis-plus.md)。
4. 用例需要时加入[应用事务](../capabilities/application-transactions.md)或[分布式锁](../capabilities/distributed-locks.md)。
5. 只有需要跨进程投递或消费端幂等时，才加入[可靠消息：Outbox 与 Inbox](../capabilities/reliable-messaging.md)。

在生产环境依赖某项能力前，请查看[采用就绪度与已验证范围](adoption-readiness.md)。
