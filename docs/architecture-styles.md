# 架构风格指南

JFoundry 通过 jMolecules 表达两类主架构风格：Hexagonal 和 Onion。它们都是纯 Java 注解模块，不依赖 Spring、Helidon 或 Quarkus。

JFoundry 不再包装 Layered Architecture。Layered、Hexagonal 和 Onion 在企业应用里经常表达同一组角色的不同投影；把它们叠加使用容易让新用户误以为需要同时维护多套架构词汇。若项目确实要使用 Layered，请直接依赖 `org.jmolecules:jmolecules-layered-architecture` 和 jMolecules ArchUnit 原生规则。

## 模块选择

| 模块 | 适用场景 |
|------|----------|
| `jfoundry-hexagonal` | 使用端口和适配器隔离应用核心与外部技术 |
| `jfoundry-onion` | 使用 Onion 环形依赖保护领域模型 |

`jfoundry-architecture` 是架构风格聚合 POM，不作为业务代码的直接运行时依赖。业务项目按需依赖具体模块。

## 选择原则

Hexagonal 和 Onion 都定义“应用核心如何与外部世界隔离”的主架构风格，正常项目应选择其中一种，不要在同一个 ArchUnit 分析范围内混用。

推荐选择：

- 端口/适配器项目：`jfoundry-hexagonal`
- Onion Simple 项目：`jfoundry-onion`
- Onion Classical 项目：`jfoundry-onion`

## 启用规则

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] rules = JFoundryRules.hexagonal();
}
```

选择 Onion 时改用：

```java
@ArchTest
ArchRule[] rules = JFoundryRules.onionSimple();
```

`JFoundryRules.hexagonal()`、`JFoundryRules.onionSimple()` 和 `JFoundryRules.onionClassical()` 分别给出基础守护规则 + 单一主架构风格入口，并附带 Hexagonal/Onion 互斥规则，避免一个项目同时标注两种主风格。

## 权威参考

- jMolecules Architecture：<https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit：<https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters：<https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture：<https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
