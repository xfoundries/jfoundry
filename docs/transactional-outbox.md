# Transactional Outbox

Transactional Outbox（事务性发件箱）是一种可靠发布消息的通用架构模式。它解决的是“双写”问题：业务事务需要同时更新数据库并发送消息，如果两步之间任一步失败，就可能出现业务数据和消息系统不一致。

权威参考：

- [microservices.io - Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [AWS Prescriptive Guidance - Transactional outbox pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
- [Microsoft Learn - Transactional Outbox Pattern](https://learn.microsoft.com/en-us/azure/architecture/databases/guide/transactional-out-box-cosmos)
- [Debezium - Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html)

## 什么时候使用

只在需要把领域事件可靠投递到进程外系统时使用 Outbox，例如 Kafka、RabbitMQ、异步集成、跨服务通知、失败重试和最终一致性链路。如果事件只需要进程内 Spring 监听器处理，不需要配置 Outbox。

![transactional-outbox.png](outbox/transactional-outbox.png)

## jfoundry 事件链路

`DomainEventPublisher` 默认由 Spring 实现：事务提交后通过 `ApplicationEventPublisher` 发布本地事件。同时，它会在事务内同步调用 `DomainEventSink`。jfoundry 内置的 `DomainEventExternalizer` 是一个 Sink，它只处理标记了 `@Externalized` 的事件，并把匹配的事件序列化写入 Outbox 表。

典型链路：

```text
Aggregate/Application Service
  -> DomainEventPublisher
  -> DomainEventExternalizer
  -> OutboxRepository
  -> ddd_outbox_event
  -> OutboxDispatcher
  -> MessageSender
  -> MQ / external system
```

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

## 配置

默认 MyBatis-Plus starter 会提供 `OutboxRepository`，表名默认为 `ddd_outbox_event`。如需自定义表名，业务侧必须创建同结构表。

```yaml
jfoundry:
  outbox:
    table-name: ddd_outbox_event
    dispatcher:
      enabled: true
      mode: scheduled
      interval-ms: 5000
      batch-size: 50
      max-retries: 5
      backoff-base-ms: 1000
      backoff-max-ms: 300000
    recovery:
      interval: 60s
      stuck-timeout: 5m
    cleanup:
      enabled: true
      interval: 24h
      published-retention-days: 7
      dead-lettered-retention-days: 30
      batch-size: 1000
```

`mode: jobrunr` 可切换到 JobRunr 派发器，需要额外引入 `jfoundry-outbox-jobrunr`。

## 表结构与迁移

MySQL 脚本位于：

```text
jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql
```

达梦 DM 脚本位于：

```text
jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event_dm.sql
```

业务项目可通过 Flyway、Liquibase 或手工 DDL 创建表。核心字段包括 `event_id`、`topic`、`payload_key`、`payload_type`、`payload_json`、`status`、重试字段和 claim 字段。

## 状态语义

- `PENDING`：已写入 Outbox，等待派发。
- `DISPATCHING`：已被某个派发器实例 claim，正在投递。
- `PUBLISHED`：投递成功。
- `FAILED`：本次投递失败，等待下次重试。
- `DEAD_LETTERED`：超过最大重试次数，进入死信状态。

派发器通过原子 claim 避免多实例重复取同一批记录。恢复任务会把长时间停留在 `DISPATCHING` 的记录回滚为 `PENDING`，清理任务只删除过期的 `PUBLISHED` 和 `DEAD_LETTERED` 终态记录。

## 使用建议

消费者应按 `event_id` 做幂等处理。Outbox 能保证业务数据和待投递消息在同一数据库事务内落库，但消息系统仍可能出现重复投递、消费端重试或下游局部失败。业务侧的 `MessageSender` 实现应只负责向具体 MQ 发送消息，并把失败结果返回给 dispatcher。
