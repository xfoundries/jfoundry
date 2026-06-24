# 框架边界设计

本文是维护者和贡献者使用的架构边界说明，用于约束 jfoundry 内部模块如何依赖框架、适配器和 starter。业务用户通常只需要阅读具体功能文档，例如 Outbox 或架构风格指南。

## 核心决策

jfoundry 的核心模块必须独立于具体应用框架，例如 Spring、Spring Boot、Helidon、Quarkus、CDI 和 Jakarta EE 运行时集成。核心模块可以依赖稳定、低侵入的库来表达领域或库级契约，例如 jMolecules 或 `slf4j-api`。

Jackson、Spring SpEL、Spring scheduling、Spring Boot auto-configuration 以及框架生命周期逻辑不能进入核心模块。

## 模块角色

核心模块只定义契约和框架无关行为：

- `jfoundry-domain`
- `jfoundry-architecture`
- `jfoundry-layered`
- `jfoundry-hexagonal`
- `jfoundry-onion`
- `jfoundry-persistence-core`
- `jfoundry-messaging-core`
- `jfoundry-outbox-core`

技术适配器实现核心契约，但不负责框架启动和自动装配：

- `jfoundry-persistence-mybatis-plus`
- `jfoundry-outbox-mybatis-plus`
- `jfoundry-messaging-jackson`
- `jfoundry-outbox-jobrunr`
- `jfoundry-messaging-spring`
- `jfoundry-outbox-spring`

框架集成层负责把适配器装配进具体运行时，并提供用户入口：

- `jfoundry-spring/jfoundry-autoconfigure`
- `jfoundry-spring/jfoundry-spring-boot-starter`
- 未来的 `jfoundry-helidon`
- 未来的 `jfoundry-quarkus`

starter 是业务项目优先依赖的聚合入口，例如 `jfoundry-spring-boot-starter`。

## 放置规则

不要只因为类或模块与 Spring 有关，就把它移动到 `jfoundry-spring` 下。

`jfoundry-messaging-spring` 和 `jfoundry-outbox-spring` 是 Spring 技术适配器，分别服务于 messaging 和 outbox 基础设施能力，因此保留在 `jfoundry-infrastructure` 下。

`jfoundry-spring` 只保留 Spring Boot 集成层职责：auto-configuration、starter 依赖和运行时组装。这样可以把“适配器实现”和“框架启动装配”分开，也方便未来 `jfoundry-helidon` 或 `jfoundry-quarkus` 基于同一套 core SPI 和非 Spring 适配器完成自己的运行时集成。

## 当前约束

`jfoundry-messaging-core` 只保留 messaging SPI、领域事件 sink 契约、路由元数据和框架无关解析逻辑。Jackson 序列化实现属于 `jfoundry-messaging-jackson`。

`jfoundry-outbox-core` 只保留 Outbox 状态模型、repository 契约、派发服务、重试/退避抽象和框架无关派发运行时。

`jfoundry-messaging-spring` 只放 Spring-specific messaging adapter，例如 Spring 事件发布和事务感知事件 sink。

`jfoundry-outbox-spring` 只放 Spring-specific outbox adapter，例如 scheduling、transaction synchronization、properties binding 和 Spring-specific outbox wiring。

`jfoundry-outbox-jobrunr` 只放纯 JobRunr adapter。JobRunr 的 Spring Boot auto-configuration 属于 `jfoundry-spring/jfoundry-autoconfigure`。

## 兼容规则

业务项目应优先依赖框架 starter。直接依赖 adapter 模块适用于高级组合场景，但 adapter 不应自行注册 Spring Boot auto-configuration，也不应把框架启动生命周期混入核心能力。

## 验收标准

- 核心模块不能以 compile 或 provided scope 依赖 Spring、Spring Boot、Helidon、Quarkus、CDI 或 Jakarta runtime API。
- Spring Boot auto-configuration 只放在 `jfoundry-spring/jfoundry-autoconfigure`。
- adapter 模块不通过 `AutoConfiguration.imports` 自行注册 Spring Boot bean。
- Spring Boot starter 继续作为业务项目的默认集成入口。
- 未来 Helidon 或 Quarkus 模块可以复用 core SPI 和非 Spring 专属 adapter，而不依赖 Spring Boot 装配模块。
