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

在 Hexagonal 口径下，`@Application` 表示六边形内部的应用核心，不只表示 application service 包；领域模型和应用服务都属于这个核心。`@PrimaryPort` 表示应用核心暴露给外部驱动方的入口，`@PrimaryAdapter` 表示调度器、HTTP、CLI、消息监听器等驱动应用的技术入口。`@SecondaryPort` 表示应用核心对外部能力的出站需求，`@SecondaryAdapter` 表示数据库、消息中间件、序列化器等对这些端口的具体实现。JFoundry 不提供裸 `@Port` / `@Adapter` 包装注解；如果方向明确，就使用 Primary 或 Secondary 特化注解。如果项目确实需要模糊角色，请直接使用 jMolecules 原生 `@Port` / `@Adapter`。Spring Boot auto-configuration 只负责装配 adapter，不标注为 adapter。

JFoundry 框架内部直接使用 jMolecules 原生架构注解，`jfoundry-hexagonal` 与 `jfoundry-onion` 保留为业务项目使用的稳定门面。`JFoundryRules` 会同时识别 JFoundry 包装注解和 jMolecules 原生注解。

推荐选择：

- 框架内部实现：`jfoundry-onion`（simplified）
- 端口/适配器项目：`jfoundry-hexagonal`
- Onion Simple 项目：`jfoundry-onion`
- Onion Classical 项目：`jfoundry-onion`

## 启用规则

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] rules = JFoundryRules.onionSimple();
}
```

如果业务项目选择 Hexagonal，则改用：

```java
@ArchTest
ArchRule[] rules = JFoundryRules.hexagonal();
```

`JFoundryRules.onionSimple()`、`JFoundryRules.onionClassical()` 和 `JFoundryRules.hexagonal()` 分别给出基础守护规则 + 单一主架构风格入口，并附带 Hexagonal/Onion 互斥规则，避免一个项目同时标注两种主风格。

## 权威参考

- jMolecules Architecture：<https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit：<https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters：<https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture：<https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
