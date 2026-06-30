# 领域事件分发重构设计（2026-06-29）

## 背景

在 `devcloud-ci-service` 接入 `jfoundry` 的过程中，领域事件相关自动装配暴露出两个框架层面的别扭点：

- `DomainEventContext` 与事件分发开关绑在一起，导致仓储实现注入上下文时容易因为关闭分发而缺 Bean。
- `SpringDomainEventDispatcher` 同时承担 Spring 应用事件发布和 Outbox 记录，职责不单一。

本轮重构面向 `jfoundry` 新版本，允许破坏性调整，不保留旧配置和旧类兼容层。

## 目标

- `DomainEventContext` 和 `DomainEventScope` 始终自动装配，Repository 可以稳定注入领域事件上下文。
- 领域事件分发能力按子能力显式开启或关闭：

```yaml
jfoundry:
  domain:
    event:
      dispatch:
        enabled: true
        spring:
          enabled: true
        outbox:
          enabled: false
```

- Spring 应用事件发布和 Outbox 记录拆成两个独立 `DomainEventDispatcher` 实现。
- 使用组合分发器把多个具体分发器组合成应用层唯一的 `DomainEventDispatcher`。
- 删除旧的 `SpringDomainEventDispatcher`，不提供 deprecated 兼容类。

## 非目标

- 不引入事件总线、消息中间件或 Event Sourcing。
- 不改变聚合根记录事件和 `DomainEventContext` 收集事件的语义。
- 不要求业务项目必须开启 Outbox。
- 不为旧配置 `jfoundry.domain.event.enabled` 提供迁移兼容。

## 设计

### 应用层

新增：

```text
org.jfoundry.application.event.CompositeDomainEventDispatcher
```

职责：

- 实现 `DomainEventDispatcher`。
- 按构造参数顺序委派给多个 `DomainEventDispatcher`。
- 拒绝空列表以外的非法输入，例如 null 事件列表和 null 事件元素。
- 不依赖 Spring 或 Outbox。

### Spring Messaging 适配

新增：

```text
org.jfoundry.infrastructure.event.spring.dispatcher.SpringApplicationEventDispatcher
```

职责：

- 只通过 `ApplicationEventPublisher` 发布 Spring 应用事件。
- 保留事务同步语义：存在 Spring 事务同步时在 `afterCommit` 后发布，否则立即发布。
- 不感知 Outbox。

删除：

```text
org.jfoundry.infrastructure.messaging.spring.dispatcher.SpringDomainEventDispatcher
```

### Outbox 适配

新增：

```text
org.jfoundry.infrastructure.outbox.spring.externalization.OutboxDomainEventDispatcher
```

职责：

- 只调用 `DomainEventOutboxRecorder.record(events)`。
- 不发布 Spring 应用事件。

### 自动装配

`DomainEventDispatchAutoConfiguration` 调整为：

- 不再受 `jfoundry.domain.event.enabled` 控制。
- 始终创建 `DomainEventScope`。
- 始终创建 `DomainEventContext`。
- `jfoundry.domain.event.dispatch.enabled=false` 时，不创建组合分发器、拦截器和 advisor。
- `jfoundry.domain.event.dispatch.spring.enabled=true` 默认创建 `SpringApplicationEventDispatcher`。
- `jfoundry.domain.event.dispatch.outbox.enabled=true` 且存在 `DomainEventOutboxRecorder` 时创建 `OutboxDomainEventDispatcher`。
- 当至少存在一个具体 `DomainEventDispatcher` 时，才创建 `CompositeDomainEventDispatcher`、`DomainEventDispatchInterceptor` 和 advisor。

## 配置语义

| 配置 | 默认值 | 含义 |
|---|---:|---|
| `jfoundry.domain.event.dispatch.enabled` | `true` | 是否在应用服务边界自动分发已登记事件 |
| `jfoundry.domain.event.dispatch.spring.enabled` | `true` | 是否发布 Spring 应用事件 |
| `jfoundry.domain.event.dispatch.outbox.enabled` | `false` | 是否把事件记录到 Outbox |

没有全局 `jfoundry.domain.event.enabled`。领域事件上下文是框架基础设施，默认存在；分发才是可选行为。

## 验收标准

- 默认配置下存在 `DomainEventContext`、`DomainEventScope`、`SpringApplicationEventDispatcher`、`CompositeDomainEventDispatcher`、拦截器和 advisor。
- `dispatch.enabled=false` 时仍存在 `DomainEventContext` 和 `DomainEventScope`，但不存在分发器、拦截器和 advisor。
- `dispatch.spring.enabled=false` 且未开启 Outbox 时，不存在分发器、拦截器和 advisor。
- `dispatch.outbox.enabled=true` 且存在 recorder 时，组合分发器同时包含 Spring 和 Outbox 能力。
- 代码中不再存在 `SpringDomainEventDispatcher`。
- 代码中不再使用 `jfoundry.domain.event.enabled`。
