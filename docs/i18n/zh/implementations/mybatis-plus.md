# MyBatis-Plus 实现

该实现使用 MyBatis-Plus 满足[聚合持久化](../capabilities/aggregate-persistence.md)和[可靠消息](../capabilities/reliable-messaging.md)契约。

## 聚合持久化

持久化 data object 继承 `AggregateData<K>` 并使用数据库友好的 ID。`DataMapper` 在 repository 边界转换 ID、data 和 aggregate。`MybatisPlusAggregateRepository` 为一个 `AggregateData`、一个 `DataMapper` 和一个 `BaseMapper` 提供完整默认实现。

多表聚合存在 MyBatis-Plus 根记录时，可覆盖完整的 `do*` 操作，并在适用时使用 `loadAggregate`、`insertAggregate`、`updateAggregate` 和 `deleteAggregate` 保留根记录持久化与 context 行为。否则直接继承 `AbstractAggregateRepository`。业务适配器仍负责还原和从属记录同步。

若需要持久化所有的乐观锁，在根 data 的版本字段上标注 `@Version`，配置 `OptimisticLockerInnerInterceptor`，并将 data class 传给 `MybatisPlusAggregateRepository`。repository 会处理已加载版本追踪、`updateById` 版本还原、零行冲突、已追踪版本推进以及 ID 加版本的删除。未标注 `@Version` 的 data 保持不追踪行为。

普通单表条件、排序、更新和删除优先使用 Lambda Wrapper。一个原子语句或数据库特定行为不可或缺时，例如 compare-and-set 更新，保留显式 SQL。

## Outbox 与 Inbox Store

内置 `OutboxMessageStore` 选择 `jfoundry-outbox-mybatis-plus-spring-boot-starter`，内置 `InboxMessageStore` 选择 `jfoundry-inbox-mybatis-plus-spring-boot-starter`。两者均为显式选择；`jfoundry-mybatis-plus-spring-boot-starter` 只装配业务持久化，不会引入任一 store。SQL 模板仍由业务应用迁移流程拥有。

在 Spring Boot 下，默认 Outbox dispatcher 和 Inbox template 使用 `TransactionRunner` 包裹各自的数据库阶段。即使单次 MyBatis mapper 调用可以自行提交，这仍不可省略：handler 与 Inbox 的 `PROCESSED` 状态迁移必须保持原子性。

运行时装配和用户替换行为见 [Spring Boot](spring-boot.md)，条件和配置项见[参考](../reference/spring-boot-autoconfiguration.md)。
