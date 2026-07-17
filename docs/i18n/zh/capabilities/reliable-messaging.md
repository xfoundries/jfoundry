# 可靠消息：Outbox 与 Inbox

只有领域事件必须可靠投递到其他进程或外部系统时，才使用 Transactional Outbox。进程内事件处理不需要它。Inbox 为一条消息与一个消费者的组合提供消费端幂等。

![transactional-outbox.png](../../assets/outbox/transactional-outbox.png)

## 事件流

```text
聚合记录领域事件
  -> 应用服务边界提取事件
  -> 外部化选择 topic、key 和 payload
  -> 与业务变更在同一数据库事务写入 Outbox 行
  -> dispatcher claim 后经由 MessageSender 发送
  -> 消费端使用 InboxTemplate 实现幂等
```

`DefaultDomainEventOutboxRecorder` 只记录带有 `@Externalized` 的事件；`@MessageRouting` 提供路由信息，但本身不会使事件外部化。只有事件本身是稳定公共契约时才使用自动外部化；否则在应用边界翻译为版本化集成契约，并使用 `OutboxTemplate` 显式追加。该模板加入调用方事务，不会自行开启事务或同步发送。

## Payload 契约

将 `payloadType` 视为稳定的契约名称，而不是 Java 类名。消费者应将 envelope 反序列化为各自的版本化契约。默认 Jackson serializer 输出可移植 JSON：时间为 ISO-8601，数值保持普通 JSON 数字，不使用 default typing 或暴露 Java 类名。需要其他 wire format 时提供 `PayloadSerializer`。

## Outbox 状态机

- `PENDING`：已写入，等待派发。
- `DISPATCHING`：已被 dispatcher claim。
- `PUBLISHED`：发送成功。
- `FAILED`：本次发送失败，等待重试。
- `DEAD_LETTERED`：超过最大重试次数。

Recovery 将卡住的 `DISPATCHING` 消息恢复为 `PENDING`。Cleanup 只删除过期终态记录。派发触发方式为 `scheduled`、`jobrunr` 或 `none`；`none` 不注册 dispatcher、recovery job 或 cleanup job。

## SQL 模板

SQL 仅作为可复制模板提供，jfoundry 从不自动执行。`jfoundry-outbox-core` 拥有规范 Outbox 路径，`jfoundry-inbox-core` 拥有规范 Inbox 路径。将需要的模板复制进业务应用的迁移流程：

```text
jfoundry/sql/outbox/mysql/create_outbox_event.sql
jfoundry/sql/outbox/postgresql/create_outbox_event.sql
jfoundry/sql/inbox/common/create_inbox_message.sql
```

## 选择 Store

| 需求 | 指南 |
|------|------|
| MyBatis-Plus Outbox 和 Inbox store | [MyBatis-Plus](../implementations/mybatis-plus.md) |
| JPA Outbox 和 Inbox store，包括数据库相关的 Inbox claim | [JPA](../implementations/jpa.md) |
| Spring Boot 能力装配和 dispatcher 配置 | [Spring Boot](../implementations/spring-boot.md) |

Starter、配置项和注册条件查询请使用 [Spring Boot 自动配置](../reference/spring-boot-autoconfiguration.md)。
