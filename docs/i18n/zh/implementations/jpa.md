# JPA 实现

该实现使用 Jakarta Persistence 满足[聚合持久化](../capabilities/aggregate-persistence.md)和[可靠消息](../capabilities/reliable-messaging.md)契约。

## 聚合持久化

受支持的默认形态是每个聚合一个由 JPA 管理的实体图。`JpaAggregateRepository` 通过 `EntityManager.find` 加载实体图，追踪受管理的根实体，把聚合变更应用到同一个图，并在报告成功前 flush。它从不调用 `merge`；加载、领域行为和 `modify(...)` 必须位于同一个事务和持久化上下文。

`JpaAggregateMapper` 创建和还原实体图、转换 ID，并把当前聚合状态同步到受管理的实体图。它不提供跨多个表或实体图的通用手工同步。

乐观并发控制时，在实体图根实体上标注 `@Version`，并确保每次实体图变更都会改变根实体的持久化属性。子实体变更不会仅因根实体存在 `@Version` 就参与并发控制；mapper 可在同步子实体变更时显式 touch 根实体。并发根更新会在 repository flush 时以 `ConflictException` 报告。

JPA 业务运行时装配使用 `jfoundry-jpa-spring-boot-starter`。它不会引入 Outbox 或 Inbox store。

Quarkus 环境中，加入 `jfoundry-quarkus-runtime`、`jfoundry-persistence-jpa`、Quarkus Hibernate ORM
和所选数据源扩展即可。由 CDI 管理的 `JpaAggregateRepository` 会从 jfoundry Quarkus 扩展获得事务作用域内的
`AggregatePersistenceContext`。应使用 `TransactionRunner` 作为事务边界，业务代码无需创建或设置该上下文。
运行时装配要求见 [Quarkus](quarkus.md)。

### 直接装配 JPA 或 Hibernate

JPA adapter 是运行时无关的，但不是开箱即用的 raw-Hibernate bootstrap。Spring Boot 和受支持的 Quarkus
装配之外，应用负责创建 `EntityManagerFactory`、开启和完成每个事务、向 persistence unit 注册聚合与框架实体，并向
`JpaAggregateRepository` 提供事务作用域内的 `AggregatePersistenceContext`。repository 只能在该持久化上下文和事务内使用。

JPQL 是面向实体及其字段的可移植查询语言。Hibernate 会把普通 JPQL 查询和更新转换为所选数据库方言。Inbox 的首次 claim 刻意不同：它需要原子的 insert-or-ignore 等价操作，因此必须显式保留数据库相关策略，而不能假装 JPQL 能可移植地表达该操作。

## Outbox 与 Inbox Store

显式选择 `jfoundry-outbox-jpa-spring-boot-starter` 和/或 `jfoundry-inbox-jpa-spring-boot-starter`。JPA adapter 是运行时无关的；在 Spring Boot 下，框架实体会自动映射，因此应用无需把它们加入自己的 `@EntityScan`。

实体注册不等于 schema 管理。将匹配的 Outbox 或 Inbox SQL 模板复制到应用自己的迁移流程并在那里维护；不要依赖 Hibernate schema 自动生成功能创建或演进 jfoundry 表。

Outbox store 使用 JPQL 读取一页可派发候选记录，并通过 compare-and-set 更新逐条 claim。dispatcher 的 claim token 建立所有权；已 claim 记录的发布和失败更新都使用该 token。

JPA Inbox 内置原子 claim 策略只支持 PostgreSQL 和 MySQL。其他数据库产品需要提供 `JpaInboxClaimStrategy`。当数据库产品未知且未提供策略时，Boot 会快速失败，而不是选择通用方言行为。用户提供的 `InboxMessageStore`、`OutboxMessageStore` 或 `JpaInboxClaimStrategy` 优先。

自定义 claim strategy 实现 `boolean tryClaim(EntityManager entityManager, String messageId, String consumerName, Instant now)`。它必须原子创建 `PROCESSING` 行，并在消息已存在时返回 `false`；目标数据库必须具有并发重复投递测试。raw JPA/Hibernate 的 Outbox 和 Inbox 装配必须应用[可靠消息](../capabilities/reliable-messaging.md)描述的事务边界。

运行时装配和配置见 [Spring Boot](spring-boot.md) 和[自动配置参考](../reference/spring-boot-autoconfiguration.md)。
