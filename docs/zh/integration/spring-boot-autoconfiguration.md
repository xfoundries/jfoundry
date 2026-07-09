# Spring Boot 自动配置总览

本文汇总 jfoundry 提供的 Spring Boot 入口、配置项和 Bean 装配条件，用于选择 starter，也用于排查某个 Bean 为什么注册或没有注册。

## Starter 入口

| Starter | 引入能力 | 不会引入 |
|---------|----------|----------|
| `jfoundry-spring-boot-starter` | Spring Boot 自动配置、Spring `TransactionRunner` 集成 | Outbox、Inbox、MyBatis-Plus store、broker client、JobRunr |
| `jfoundry-event-spring-boot-starter` | 领域事件派发、Spring application event 发布 | Outbox 持久化或 broker 投递 |
| `jfoundry-messaging-spring-boot-starter` | Messaging SPI、Jackson payload serializer、Spring messaging runtime、默认 logging `MessageSender` | Kafka、RabbitMQ、RocketMQ client |
| `jfoundry-messaging-kafka-spring-boot-starter` | Kafka `MessageSender` adapter | Outbox store |
| `jfoundry-messaging-rabbitmq-spring-boot-starter` | RabbitMQ `MessageSender` adapter | Outbox store |
| `jfoundry-messaging-rocketmq-spring-boot-starter` | RocketMQ `MessageSender` adapter | Outbox store |
| `jfoundry-outbox-spring-boot-starter` | Outbox core、领域事件外部化、scheduled 派发集成 | Outbox 表存储、JobRunr |
| `jfoundry-outbox-mybatis-plus-spring-boot-starter` | MyBatis-Plus `OutboxMessageStore` adapter | 数据库 migration 执行 |
| `jfoundry-outbox-jobrunr-spring-boot-starter` | JobRunr Outbox dispatcher | Outbox 表存储 |
| `jfoundry-inbox-spring-boot-starter` | Inbox core、`InboxTemplate` | Inbox 表存储 |
| `jfoundry-inbox-mybatis-plus-spring-boot-starter` | MyBatis-Plus `InboxMessageStore` adapter | 数据库 migration 执行 |
| `jfoundry-mybatis-plus-spring-boot-starter` | Spring Boot MyBatis-Plus 运行时装配 | 业务持久化 starter、Outbox/Inbox store |
| `jfoundry-webmvc-spring-boot-starter` | Web MVC `ProblemDetail` 异常响应 | Messaging、Outbox、Inbox |

## 配置项

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `jfoundry.domain.event.dispatch.enabled` | `true` | 开启应用服务边界上的领域事件自动派发。 |
| `jfoundry.domain.event.dispatch.spring.enabled` | `true` | 当 Spring 事件 adapter 存在时，开启 Spring `ApplicationEventPublisher` 派发。 |
| `jfoundry.domain.event.dispatch.outbox.enabled` | `false` | 当存在 `DomainEventOutboxRecorder` Bean 时，开启 Outbox 领域事件派发。 |
| `jfoundry.outbox.table-name` | `jfoundry_outbox_event` | 改写 MyBatis-Plus Outbox 物理表名。业务应用必须自行建表。 |
| `jfoundry.outbox.dispatcher.mode` | `scheduled` | 选择 `scheduled`、`jobrunr` 或 `none`。 |
| `jfoundry.outbox.dispatcher.interval-ms` | `5000` | scheduled 派发固定延迟间隔。 |
| `jfoundry.outbox.dispatcher.cron` | `*/10 * * * * *` | JobRunr recurring dispatch cron 表达式。 |
| `jfoundry.outbox.dispatcher.batch-size` | `50` | 每次派发最多 claim 的记录数。 |
| `jfoundry.outbox.dispatcher.max-retries` | `5` | dead-letter 前最大派发尝试次数。 |
| `jfoundry.outbox.dispatcher.backoff-base-ms` | `1000` | 重试退避基础值。 |
| `jfoundry.outbox.dispatcher.backoff-max-ms` | `300000` | 最大重试退避值。 |
| `jfoundry.outbox.recovery.enabled` | 跟随 dispatcher mode | 在 `scheduled` 和 `jobrunr` 下开启卡住的 `DISPATCHING` 恢复；`none` 下始终关闭。 |
| `jfoundry.outbox.recovery.interval` | `60s` | recovery job 间隔。 |
| `jfoundry.outbox.recovery.stuck-timeout` | `5m` | `DISPATCHING` 记录超过该时间后视为卡住。 |
| `jfoundry.outbox.cleanup.enabled` | 跟随 dispatcher mode | 在 `scheduled` 和 `jobrunr` 下开启终态记录清理；`none` 下始终关闭。 |
| `jfoundry.outbox.cleanup.interval` | `24h` | cleanup job 间隔。 |
| `jfoundry.outbox.cleanup.published-retention-days` | `7` | `PUBLISHED` 记录保留天数。 |
| `jfoundry.outbox.cleanup.dead-lettered-retention-days` | `30` | `DEAD_LETTERED` 记录保留天数。 |
| `jfoundry.outbox.cleanup.batch-size` | `1000` | 每批最多删除记录数。 |

## 自动配置条件

| 自动配置 | 注册 Bean | 主要条件 |
|----------|-----------|----------|
| `TransactionRunnerAutoConfiguration` | `SpringTransactionRunner` | 存在 `TransactionRunner`、`TransactionTemplate` 和 `PlatformTransactionManager`；没有已有 `TransactionRunner`。 |
| `DomainEventPersistenceAutoConfiguration` | Repository `DomainEventContext` 注入器 | classpath 中存在 `DomainEventContext` 和 `AbstractPersistenceRepository`。 |
| `DomainEventDispatchAutoConfiguration` | `DomainEventScope`、`DomainEventContext`、派发拦截器、Spring event dispatcher、可选 Outbox dispatcher | 应用服务和 dispatcher 类型存在；配置项允许对应路径。 |
| `DomainEventOutboxRecorderAutoConfiguration` | `PayloadSerializer`、外部化 resolver、`DomainEventOutboxRecorder` | Outbox store 和 serializer 依赖可用；没有用户自定义替代 Bean。 |
| `MessageSenderAutoConfiguration` | `LoggingMessageSender` fallback | 没有用户自定义或 broker-specific `MessageSender`。fallback 返回发送失败结果。 |
| `KafkaMessageSenderAutoConfiguration` | `KafkaMessageSender` | 存在 `KafkaOperations` class 和 Bean；没有已有 `MessageSender`。 |
| `RabbitMqMessageSenderAutoConfiguration` | `RabbitMqMessageSender` | 存在 `RabbitTemplate` class 和 `RabbitOperations` Bean；没有已有 `MessageSender`。 |
| `RocketMqMessageSenderAutoConfiguration` | `RocketMqMessageSender` | 存在 RocketMQ producer class 和 `MQProducer` Bean；没有已有 `MessageSender`。 |
| `OutboxMybatisPlusAutoConfiguration` | Outbox 表名 customizer、`MybatisPlusInterceptor`、`OutboxMessageStore` | MyBatis-Plus 和 Outbox store adapter class 存在。SQL 模板不会自动执行。 |
| `OutboxDispatcherAutoConfiguration` | `BackoffStrategy`、scheduled dispatcher、recovery job、cleanup job | 存在 Outbox store、message sender、scheduled dispatcher class；mode 为 `scheduled` 或维护任务由托管 mode 启用。 |
| `JobRunrDispatcherAutoConfiguration` | `JobRunrOutboxDispatcher` | 存在 JobRunr 和 jfoundry JobRunr adapter class；`mode=jobrunr`；存在 store、sender、backoff Bean。 |
| `InboxMybatisPlusAutoConfiguration` | MyBatis-Plus `InboxMessageStore` | 存在 `SqlSessionFactory`、mapper scanning 和 Inbox store adapter；没有已有 store。 |
| `InboxAutoConfiguration` | `InboxTemplate` | classpath 中存在 `InboxTemplate`，且存在 `InboxMessageStore` Bean。 |
| `WebMvcProblemDetailAutoConfiguration` | `ProblemDetailExceptionHandler` | Servlet Web MVC 应用且 handler class 存在；没有已有 handler。 |

## 说明

- SQL 文件只是可复制模板。jfoundry jar 不会自动创建 Outbox 或 Inbox 表。
- broker-specific `MessageSender` 的自动配置先于 logging fallback，因此会优先生效。
- `mode=none` 表示不注册 dispatcher、recovery job 或 cleanup job，即使显式开启 recovery 或 cleanup 也不会注册。
