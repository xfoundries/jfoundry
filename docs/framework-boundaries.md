# 框架边界设计

本文是维护者和贡献者使用的架构边界说明，用于约束 jfoundry 内部模块如何依赖框架、适配器和 starter。业务用户通常只需要阅读具体功能文档，例如 Outbox 或架构风格指南。

## 核心决策

jfoundry 的核心模块必须独立于具体应用框架，例如 Spring、Spring Boot、Helidon、Quarkus、CDI 和 Jakarta EE 运行时集成。核心模块可以依赖稳定、低侵入的库来表达领域或库级契约，例如 jMolecules 或 `slf4j-api`。

Jackson、Spring SpEL、Spring scheduling、Spring Boot auto-configuration 以及框架生命周期逻辑不能进入核心模块。

## 模块角色

核心模块只定义契约和框架无关行为：

- `jfoundry-domain`
- `jfoundry-architecture`
- `jfoundry-hexagonal`
- `jfoundry-onion`
- `jfoundry-persistence-core`

`jfoundry-application` hosts application-layer reliability contracts and framework-neutral services:

- `jfoundry-messaging-core`
- `jfoundry-inbox-core`
- `jfoundry-outbox-core`

`jfoundry-infrastructure` hosts technical adapters implementing or consuming application-layer contracts.
技术适配器实现核心契约，但不负责框架启动和自动装配：

- `jfoundry-persistence-mybatis-plus`
- `jfoundry-outbox-mybatis-plus`
- `jfoundry-inbox-mybatis-plus`
- `jfoundry-messaging-jackson`
- `jfoundry-messaging-kafka`
- `jfoundry-outbox-jobrunr`
- `jfoundry-messaging-spring`
- `jfoundry-outbox-spring`

这里的 `infrastructure` 表示 Onion simplified 中的基础设施环。它按技术关注点组织模块（Spring、MyBatis-Plus、Kafka、JobRunr 等），而不是再额外按“驱动侧 / 被驱动侧”拆成独立 Maven 层。

框架集成层负责把适配器装配进具体运行时，并提供用户入口：

- `jfoundry-spring/jfoundry-spring-boot-autoconfigure`
- `jfoundry-spring/jfoundry-spring-boot-starter`
- `jfoundry-spring/jfoundry-spring-boot-starter-messaging`
- `jfoundry-spring/jfoundry-spring-boot-starter-messaging-kafka`
- `jfoundry-spring/jfoundry-spring-boot-starter-outbox`
- `jfoundry-spring/jfoundry-spring-boot-starter-outbox-mybatis-plus`
- `jfoundry-spring/jfoundry-spring-boot-starter-outbox-jobrunr`
- `jfoundry-spring/jfoundry-spring-boot-starter-inbox`
- `jfoundry-spring/jfoundry-spring-boot-starter-inbox-mybatis-plus`
- `jfoundry-spring/jfoundry-spring-boot-starter-mybatis-plus`
- 未来的 `jfoundry-helidon`
- 未来的 `jfoundry-quarkus`

JFoundry 框架内部默认采用 Onion simplified：`jfoundry-domain` 标注 `DomainRing`，`jfoundry-application` 标注 `ApplicationRing`，`jfoundry-infrastructure` 标注 `InfrastructureRing`。`jfoundry-hexagonal` 与 `jfoundry-onion` 仍保留为业务项目使用的稳定门面，因此外部项目可以按需选择 Hexagonal 或 Onion。`jfoundry-spring-boot-autoconfigure` 负责创建和装配这些模块，但它本身不参与 Onion ring 标注。各 starter 是 POM 依赖入口，本身不承载 Java 包级架构角色。

starter 是业务项目优先依赖的聚合入口。新项目应按能力显式选择：

- `jfoundry-spring-boot-starter`：最小默认入口，只聚合 Spring Boot 自动装配和 DDD domain 基础能力。
- `jfoundry-spring-boot-starter-messaging`：聚合 messaging application contracts、Jackson payload serializer、Spring domain-event publisher 和默认 logging `MessageSender`。
- `jfoundry-spring-boot-starter-messaging-kafka`：在 messaging starter 之上选择 Kafka `MessageSender` adapter。
- `jfoundry-spring-boot-starter-outbox`：聚合 Outbox core、Spring transaction synchronization、scheduled dispatcher、recovery 和 cleanup。
- `jfoundry-spring-boot-starter-outbox-mybatis-plus`：在 Outbox starter 之上选择 MyBatis-Plus `OutboxMessageStore` adapter。
- `jfoundry-spring-boot-starter-outbox-jobrunr`：在 Outbox starter 之上选择 JobRunr dispatcher adapter。
- `jfoundry-spring-boot-starter-inbox`：聚合 Inbox core 和 `InboxTemplate` auto-configuration。
- `jfoundry-spring-boot-starter-inbox-mybatis-plus`：在 Inbox starter 之上选择 MyBatis-Plus `InboxMessageStore` adapter。
- `jfoundry-spring-boot-starter-mybatis-plus`：只聚合业务聚合根 repository 所需的 MyBatis-Plus persistence adapter，不隐式包含 Outbox/Inbox store。

## 放置规则

不要只因为类或模块与 Spring 有关，就把它移动到 `jfoundry-spring` 下。

`jfoundry-messaging-spring` 和 `jfoundry-outbox-spring` 是 Spring 技术适配器，分别服务于 messaging 和 outbox 基础设施能力，因此保留在 `jfoundry-infrastructure` 下。

`jfoundry-spring` 只保留 Spring Boot 集成层职责：auto-configuration、starter 依赖和运行时组装。这样可以把“适配器实现”和“框架启动装配”分开，也方便未来 `jfoundry-helidon` 或 `jfoundry-quarkus` 基于同一套 core SPI 和非 Spring 适配器完成自己的运行时集成。

## 当前约束

`jfoundry-messaging-core` 属于 `jfoundry-application`。它不是“与领域无关的通用 MQ core”，而是两部分 application-layer 能力的组合：

- 消息发送与 payload 序列化 SPI，例如 `MessageSender`、`PayloadSerializer`
- 领域事件外部化规则与 sink 契约，例如 `DomainEventSink`、`ExternalizationRuleResolver`、`AggregateRoutingResolver`

因此它依赖 `jfoundry-domain` 中的 `DomainEvent` 相关抽象是设计内的依赖方向：application 层围绕领域事件定义外部化编排规则，infrastructure 层再实现具体 broker / Spring / Outbox 适配器。Jackson 序列化实现属于 `jfoundry-messaging-jackson`。

`jfoundry-outbox-core` 属于 `jfoundry-application`，只保留 Outbox 状态模型、message store 契约、派发服务、重试/退避抽象和框架无关派发运行时。

`jfoundry-inbox-core` 属于 `jfoundry-application`，提供消费端幂等的 `InboxMessageStore` message store 契约与 `InboxTemplate`。

Outbox/Inbox data objects do not extend `AggregateData`; their MyBatis stores use `BaseMapper` directly instead of `MybatisPlusRepository`.

`jfoundry-messaging-spring` 只放 Spring-specific messaging adapter，例如 `DomainEventPublisher` 的 Spring 实现、本地 `ApplicationEventPublisher` 集成，以及默认 logging `MessageSender`。它负责“如何在 Spring 运行时发布领域事件”，不负责事件外部化规则本身。

`jfoundry-outbox-spring` 只放 Spring-specific outbox adapter，例如 `DomainEventExternalizer`、scheduling、transaction synchronization、properties binding 和 Spring-specific outbox wiring。它负责“如何把符合规则的领域事件写入 Outbox 并调度派发”，不定义领域事件本身，也不定义 broker 抽象。

`jfoundry-outbox-jobrunr` 只放纯 JobRunr adapter。JobRunr 的 Spring Boot auto-configuration 属于 `jfoundry-spring/jfoundry-spring-boot-autoconfigure`。

`jfoundry-messaging-kafka` 只放 Kafka `MessageSender` adapter。业务侧显式选择 Kafka 时引入 `jfoundry-spring-boot-starter-messaging-kafka`，未来切换 MQ 通过替换 `MessageSender` adapter starter 完成，不影响 Outbox core。

MyBatis-Plus adapter 不进入 `jfoundry-spring-boot-starter`。如果业务使用 Spring Data / JPA，应依赖默认 starter，并通过业务侧或后续扩展模块提供 repository、`OutboxMessageStore`、`InboxMessageStore` 实现。

## 兼容规则

业务项目应优先依赖框架 starter。直接依赖 adapter 模块适用于高级组合场景，但 adapter 不应自行注册 Spring Boot auto-configuration，也不应把框架启动生命周期混入核心能力。

## 验收标准

- 核心模块不能以 compile 或 provided scope 依赖 Spring、Spring Boot、Helidon、Quarkus、CDI 或 Jakarta runtime API。
- Spring Boot auto-configuration 只放在 `jfoundry-spring/jfoundry-spring-boot-autoconfigure`。
- adapter 模块不通过 `AutoConfiguration.imports` 自行注册 Spring Boot bean。
- Spring Boot starter 继续作为业务项目的集成入口；默认 starter 保持轻量，能力 starter 使用 `starter-{能力}` / `starter-{能力}-{adapter}` 命名。
- 未来 Helidon 或 Quarkus 模块可以复用 core SPI 和非 Spring 专属 adapter，而不依赖 Spring Boot 装配模块。
