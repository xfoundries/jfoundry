# 架构风格指南

JFoundry 通过 jMolecules 表达两类主架构风格：Hexagonal 和 Onion。它们都是纯 Java 注解模块，不依赖 Spring、Helidon 或 Quarkus。JFoundry 框架内部默认采用 Onion simplified；对外仍同时提供 Hexagonal 与 Onion 门面，业务项目按自己的架构偏好选择。

JFoundry 不再包装 Layered Architecture。Layered、Hexagonal 和 Onion 在企业应用里经常表达同一组角色的不同投影；把它们叠加使用容易让新用户误以为需要同时维护多套架构词汇。若项目确实要使用 Layered，请直接依赖 `org.jmolecules:jmolecules-layered-architecture` 和 jMolecules ArchUnit 原生规则。

## 模块选择

| 模块 | 适用场景 |
|------|----------|
| `jfoundry-hexagonal` | 使用端口和适配器隔离应用核心与外部技术 |
| `jfoundry-onion` | 使用 Onion 环形依赖保护领域模型 |

`jfoundry-architecture` 是架构风格聚合 POM，不作为业务代码的直接运行时依赖。业务项目按需依赖具体模块。

## 选择原则

Hexagonal 和 Onion 都定义“应用核心如何与外部世界隔离”的主架构风格，正常项目应选择其中一种，不要在同一个 ArchUnit 分析范围内混用。JFoundry 自身为了降低内部模块语义混乱，内部实现默认采用 Onion simplified；但这不改变业务项目可以选择 Hexagonal 或 Onion 的事实。

Hexagonal 角色语义：

| 注解 | 含义 | 常见位置 | 示例 |
|------|------|----------|------|
| `@Application` | 六边形内部的应用核心，不只表示 application service 包；领域模型和应用服务都属于这个核心 | `domain`、`application`，或更细的应用核心包 | 聚合、领域服务、应用服务、用例编排 |
| `@PrimaryPort` | 应用核心暴露给外部驱动方的入口 | `application.port.in`、`application.usecase` | `CreateOrderUseCase`、`CancelOrderCommandHandler` |
| `@PrimaryAdapter` | 驱动应用的技术入口 | `adapter.in.web`、`adapter.in.messaging`、`adapter.in.scheduler` | REST Controller、消息监听器、定时任务、CLI |
| `@SecondaryPort` | 应用核心对外部能力的出站需求 | `application.port.out` | `OrderRepository`、`PaymentGatewayPort`、`OrderEventPublisherPort` |
| `@SecondaryAdapter` | 外部能力的具体实现 | `adapter.out.persistence`、`adapter.out.messaging`、`adapter.out.client` | MyBatis adapter、Kafka sender、支付 SDK adapter |

JFoundry 不提供裸 `@Port` / `@Adapter` 包装注解；如果方向明确，就使用 Primary 或 Secondary 特化注解。如果项目确实需要模糊角色，请直接使用 jMolecules 原生 `@Port` / `@Adapter`。Spring Boot auto-configuration 只负责装配 adapter，不标注为 adapter。

Onion 角色语义：

| 风格 | 注解 | 含义 | 常见位置 | 示例 |
|------|------|------|----------|------|
| Onion Simple | `@DomainRing` | 领域核心，承载业务概念和不变量 | `domain` | 聚合、实体、值对象、领域事件、领域服务、仓储契约 |
| Onion Simple | `@ApplicationRing` | 应用层，编排用例并协调领域模型和出站端口 | `application` | 应用服务、命令处理、查询入口、事务边界附近的流程编排 |
| Onion Simple | `@InfrastructureRing` | 外圈基础设施，依赖内圈并实现技术细节 | `infrastructure`、`adapter` | 持久化实现、消息实现、外部 API client、运行时配置 |
| Onion Classical | `@DomainModelRing` | 更细分的领域模型核心 | `domain.model` | 聚合、实体、值对象、领域事件 |
| Onion Classical | `@DomainServiceRing` | 领域服务环 | `domain.service` | 跨聚合且属于领域语义的服务 |
| Onion Classical | `@ApplicationServiceRing` | 应用服务环 | `application` | 用例服务、命令处理、应用流程编排 |
| Onion Classical | `@InfrastructureRing` | 基础设施环 | `infrastructure` | 数据库、消息、外部系统、框架配置 |

普通新项目如果选择 Onion，优先使用 Onion Simple；只有团队明确需要区分 domain model、domain service、application service 等更细 ring 语义时，再使用 Onion Classical。无论 Simple 还是 Classical，依赖方向都应指向领域核心，Spring Boot auto-configuration 仍只负责装配，不作为 Onion ring 参与建模。

JFoundry 框架内部直接使用 jMolecules 原生架构注解，`jfoundry-hexagonal` 与 `jfoundry-onion` 保留为业务项目使用的稳定门面。`JFoundryRules` 会同时识别 JFoundry 包装注解和 jMolecules 原生注解。

### 如何选择？

| 视角 | 更适合的场景 | 简单例子 |
|------|----------------|----------|
| Hexagonal | 外部输入/输出边界清晰，需要明确区分“谁驱动应用”和“应用依赖哪些外部能力” | 订单系统通过 REST 接收命令，通过数据库保存聚合，通过 Kafka 投递事件，通过支付 SDK 调外部服务 |
| Hexagonal | 正在从 Controller → Service → Mapper 迁移，希望逐步拆出 primary port、secondary port 和 adapter | Controller 调 primary port，应用服务调 secondary port，MyBatis adapter 实现 secondary port |
| Hexagonal | 领域事件需要可靠外部化，Outbox、MessageSender、broker sender 都要作为外部能力处理 | 订单创建后写 Outbox，再由 Kafka `MessageSender` 投递 |
| Onion | 更关心依赖向领域核心收敛，不需要显式区分 primary / secondary 端口 | 定价、计费、审批规则库主要维护聚合、值对象和领域服务，外部集成较少 |
| Onion | 既有项目已经按 domain / application / infrastructure 分包，并且依赖方向基本向内 | 老系统保留 Onion 包结构，只补充 jfoundry 注解和 ArchUnit 规则 |
| Onion Classical | 团队明确需要区分 domain model、domain service、application service、infrastructure 等更细 ring | 复杂领域平台已有成熟的 Onion Classical 术语和包结构 |
| 不强制选择完整架构风格 | 简单 CRUD 后台、短期原型、没有明显业务不变量或外部端口 | 管理台只维护少量表单和列表，可先使用更简单的 Spring Boot 分层 |

快速判断：

```text
需要清楚表达外部输入/输出边界 -> Hexagonal
主要想保护领域核心、团队习惯环形依赖语言 -> Onion
只是简单 CRUD 或短期原型 -> 先不要引入完整架构风格
```

## 启用规则

新业务项目选择 Hexagonal 时，推荐启用严格入口：

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] rules = JFoundryRules.hexagonalStrict();
}
```

如果业务项目选择 Onion Simple，则改用：

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] rules = JFoundryRules.onionSimple();
}
```

`JFoundryRules.onionSimple()` 和 `JFoundryRules.onionClassical()` 分别给出基础守护规则 + 单一主架构风格入口，并附带 Hexagonal/Onion 互斥规则。`JFoundryRules.hexagonalStrict()` 是 Hexagonal 项目的推荐入口：它包含 `JFoundryRules.hexagonal()` 的基础 Hexagonal 依赖规则，也包含 JFoundry 对端口、适配器、包名和持久化细节隔离的推荐落地约定。若只需要 jMolecules 原生 Hexagonal 规则，可单独使用 `JFoundryRules.hexagonal()`。

## 权威参考

- jMolecules Architecture：<https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit：<https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters：<https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture：<https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
