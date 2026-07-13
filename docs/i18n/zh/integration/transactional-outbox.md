# Transactional Outbox

Transactional Outbox（事务性发件箱）是一种可靠发布消息的通用架构模式。它解决的是“双写”问题：业务事务需要同时更新数据库并发送消息，如果两步之间任一步失败，就可能出现业务数据和消息系统不一致。

权威参考：

- [microservices.io - Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [AWS Prescriptive Guidance - Transactional outbox pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
- [Microsoft Learn - Transactional Outbox Pattern](https://learn.microsoft.com/en-us/azure/architecture/databases/guide/transactional-out-box-cosmos)
- [Debezium - Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html)

## 什么时候使用

只在需要把领域事件可靠投递到进程外系统时使用 Outbox，例如 Kafka、RabbitMQ、异步集成、跨服务通知、失败重试和最终一致性链路。如果事件只需要进程内 Spring 监听器处理，不需要配置 Outbox。

![transactional-outbox.png](../../assets/outbox/transactional-outbox.png)

## jfoundry 事件链路

业务侧在应用服务上标注 `@ApplicationService` 后，框架会在成功返回的应用服务边界自动 drain 聚合记录的领域事件，并通过 `DomainEventDispatcher` 分发。默认启用的是组合分发：`jfoundry-event-spring` 负责通过 `ApplicationEventPublisher` 安排 Spring 进程内事件发布；启用 Outbox 后，`jfoundry-outbox-spring` 负责调用 `DomainEventOutboxRecorder` 在事务内写 Outbox。这里有一个边界要点：

- `jfoundry-domain` 定义领域事件抽象与聚合事件记录能力
- `jfoundry-event-core` 定义应用层事件登记与分发契约
- `jfoundry-event-externalization-core` 定义领域事件外部化规则与路由元数据
- `jfoundry-messaging-core` 定义底层消息发送与 payload 序列化 SPI
- `jfoundry-event-spring` 提供 Spring `ApplicationEventPublisher` 领域事件发布适配器
- `jfoundry-outbox-spring` 提供默认 `DomainEventOutboxRecorder` 实现，把匹配规则的领域事件写入 Outbox

jfoundry 内置的 `DefaultDomainEventOutboxRecorder` 只处理标记了 `@Externalized` 的事件，并把匹配的事件序列化写入 Outbox 表。

jfoundry 当前实现的是 Transactional Outbox 的 polling publisher 变体：业务事务写入 `jfoundry_outbox_event`，后台 dispatcher 轮询并投递。transaction-log tailing / Debezium 不属于默认运行时；如果业务需要基于数据库日志的发布链路，应在应用外部组合 Debezium Outbox Event Router。

典型链路：

```text
@ApplicationService
  -> DomainEventContext
  -> DomainEventDispatcher
  -> DomainEventOutboxRecorder
  -> OutboxMessageStore
  -> jfoundry_outbox_event
  -> OutboxDispatcher
  -> MessageSender
  -> MQ / external system
```

Outbox 记录会携带 broker-neutral 的 aggregate metadata（`aggregate_type`、`aggregate_id`、`aggregate_version`），用于路由、观测或下游顺序控制。它不绑定 Kafka、RabbitMQ 或其他 MQ。

## 标记外部化事件

`@Externalized` 决定事件是否参与外部化；`@MessageRouting` 可提供更明确的 topic 和 routing key。

```java
@Externalized("order.created")
@MessageRouting(topic = "order.created", key = "orderId")
public final class OrderCreatedEvent extends AbstractDomainEvent {
    private final String orderId;

    public OrderCreatedEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
```

如果只标记 `@MessageRouting` 而没有 `@Externalized`，事件不会写入 Outbox。

## 直接外部化与契约隔离

当领域事件本身就是经过明确设计、能够稳定演进的公共契约时，可以使用自动外部化：

```text
@Externalized 领域事件 -> DomainEventOutboxRecorder -> Outbox
```

不要仅为了少写一层转换，就把内部领域事件暴露给外部消费者。当消费者需要独立、版本化的集成事件契约时，应在应用边界完成转换，再显式记录：

```java
ExpenseClaimApprovedV1 integrationEvent = translator.translate(domainEvent);
outboxTemplate.append(new OutboxAppendRequest(
        eventId,
        "expense-approval.events.v1",
        claimId,
        "ExpenseClaimApprovedV1",
        integrationEvent,
        occurredAt,
        "ExpenseClaim",
        claimId,
        aggregateVersion));
```

`OutboxTemplate` 使用已有的 `PayloadSerializer` 序列化 payload，并通过 `OutboxMessageStore` 追加 `PENDING` 消息。它参与调用方已经开启的事务；它不负责把领域事件转换成集成事件、不主动开启事务，也不做同步消息投递。原有的自动领域事件外部化路径保持不变。

## 配置

Outbox 是可选能力。业务侧需要可靠外部化时引入 `jfoundry-outbox-spring-boot-starter`；存在 `OutboxMessageStore` 和 `PayloadSerializer` 时，该 starter 会自动装配 `OutboxTemplate`。如果需要 MyBatis-Plus 的 Outbox 存储，再引入 `jfoundry-outbox-mybatis-plus-spring-boot-starter`。后者会通过 MyBatis-Plus 适配器提供 `OutboxMessageStore`，表名默认为 `jfoundry_outbox_event`。如需自定义表名，设置 `jfoundry.outbox.table-name`，并由业务侧创建同结构表。

Outbox 间接使用的 messaging starter 已包含 Spring Boot 官方 JSON starter。因此批处理消费者等
非 Web 应用也会获得默认 Jackson `ObjectMapper` 和 `PayloadSerializer`，无需为了 Outbox 额外
引入 WebMVC 或 WebFlux starter。业务侧自定义的 `ObjectMapper` 或 `PayloadSerializer` 仍然优先。

```yaml
jfoundry:
  outbox:
    table-name: jfoundry_outbox_event
    dispatcher:
      mode: scheduled
      interval-ms: 5000
      batch-size: 50
      max-retries: 5
      backoff-base-ms: 1000
      backoff-max-ms: 300000
    recovery:
      enabled: true
      interval: 60s
      stuck-timeout: 5m
    cleanup:
      enabled: true
      interval: 24h
      published-retention-days: 7
      dead-lettered-retention-days: 30
      batch-size: 1000
```

`mode: jobrunr` 可将消息派发触发器切换为 JobRunr，需要额外引入
`jfoundry-outbox-jobrunr-spring-boot-starter`。

`jfoundry.outbox.dispatcher.mode` 只控制 Outbox 消息派发触发方式：

- `scheduled`：使用 Spring `@Scheduled` 轮询派发，并默认启用 recovery 与 cleanup。
- `jobrunr`：使用 JobRunr 触发派发，并默认启用 recovery 与 cleanup；recovery / cleanup
  仍是轻量 Spring `@Scheduled` 维护任务，不依赖 JobRunr。
- `none`：不注册自动派发器，也不注册 recovery / cleanup 后台任务。

`jfoundry.outbox.recovery.enabled` 与 `jfoundry.outbox.cleanup.enabled` 只用于在 `scheduled` / `jobrunr`
模式下关闭对应维护任务；`mode: none` 表示关闭自动派发及框架内置维护任务。

## Broker adapter

Outbox starter 不携带具体 broker 客户端。未提供 `MessageSender` 时，jfoundry 使用 logging sender
记录消息内容，但它会返回失败结果，不会让 dispatcher 把消息标记为 `PUBLISHED`。生产环境如启用
Outbox 外部化，必须提供真实 `MessageSender`；没有外部投递需求时，应关闭 dispatcher 或不要标记事件为外部化。

jfoundry 当前提供 Kafka、RabbitMQ、RocketMQ 等 broker adapter。Kafka/RabbitMQ 的基础设施 adapter 直接面向原生客户端；Spring Boot 业务侧显式引入对应 starter 后，自动配置会把 Spring 侧客户端（例如 `KafkaTemplate<String, String>` / `RabbitTemplate`）包装为 `MessageSender`。如果不使用 Spring Boot，也可以直接依赖原生 broker adapter 或自行实现同一个 `MessageSender` SPI，不需要改 Outbox 本体。

## 表结构与迁移

这些 SQL 以模板形式随 jar 发布，路径避开 Flyway 默认扫描目录，不会被业务项目自动执行。
业务项目应按数据库类型复制对应模板到自己的 Flyway/Liquibase migration 目录，或由 DBA 手工执行。

Outbox 模板的 classpath resource 路径：

```text
jfoundry/sql/outbox/mysql/create_outbox_event.sql
jfoundry/sql/outbox/postgresql/create_outbox_event.sql
```

可以从依赖 jar 中查看或解出模板：

```bash
jar tf jfoundry-outbox-mybatis-plus-*.jar | grep 'jfoundry/sql/outbox'
jar xf jfoundry-outbox-mybatis-plus-*.jar jfoundry/sql/outbox/mysql/create_outbox_event.sql
```

解出后复制到业务项目自己的迁移目录，并按项目命名规范改成业务侧 migration 文件名。

核心字段包括 `event_id`、`topic`、`payload_key`、`payload_type`、`payload_json`、`aggregate_type`、`aggregate_id`、`aggregate_version`、`status`、重试字段和 claim 字段。

## 状态语义

- `PENDING`：已写入 Outbox，等待派发。
- `DISPATCHING`：已被某个派发器实例 claim，正在投递。
- `PUBLISHED`：投递成功。
- `FAILED`：本次投递失败，等待下次重试。
- `DEAD_LETTERED`：超过最大重试次数，进入死信状态。

派发器通过原子 claim 避免多实例重复取同一批记录。恢复任务会把长时间停留在 `DISPATCHING` 的记录回滚为 `PENDING`，清理任务只删除过期的 `PUBLISHED` 和 `DEAD_LETTERED` 终态记录。

## 使用建议

消费者应按 `event_id` 或业务消息 id 做幂等处理。Outbox 能保证业务数据和待投递消息在同一数据库事务内落库，但消息系统仍可能出现重复投递、消费端重试或下游局部失败。业务侧的 `MessageSender` 实现应只负责向具体 MQ 发送消息，并把失败结果返回给 dispatcher。

`jfoundry-inbox-spring-boot-starter` 会在业务侧存在 `InboxMessageStore` Bean 时装配 `InboxTemplate`。MyBatis-Plus 项目可引入 `jfoundry-inbox-mybatis-plus-spring-boot-starter` 提供 MyBatis-Plus `InboxMessageStore`。消费者可以用 `executeOnce(...)` 包住处理逻辑；MyBatis-Plus 适配器会先按 `messageId + consumerName` 抢占 `PROCESSING` 记录，成功后标记 `PROCESSED`，失败后标记 `FAILED`，并发重复投递时只有抢占成功的一方会执行 handler：

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

默认 Inbox 表名是 `jfoundry_inbox_message`，SQL 模板位于：

```text
jfoundry/sql/inbox/common/create_inbox_message.sql
```

Inbox DDL 当前只使用 `VARCHAR`、`TIMESTAMP`、主键、唯一约束和普通索引，未使用自增、JSON、TEXT/CLOB、函数默认值或方言索引语法，因此 MySQL、PostgreSQL 可复用同一份模板。后续如果某个数据库需要专门字段类型、索引语法或存储参数，再按 `jfoundry/sql/inbox/{database}/...` 增加方言模板。

国产数据库或其他专有数据库的 DDL 模板不作为 jfoundry 官方内置模板发布，建议由数据库厂商、三方集成包或业务侧按自身版本和兼容模式维护。
