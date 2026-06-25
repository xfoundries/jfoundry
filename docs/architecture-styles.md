# 架构风格指南

JFoundry 通过 jMolecules 表达三类架构风格：Layered、Hexagonal 和 Onion。它们都是纯 Java 注解模块，不依赖 Spring、Helidon 或 Quarkus。

## 模块选择

| 模块 | 适用场景 |
|------|----------|
| `jfoundry-layered` | 表达接口层、应用层、领域层、基础设施层的责任边界 |
| `jfoundry-hexagonal` | 使用端口和适配器隔离应用核心与外部技术 |
| `jfoundry-onion` | 使用 Onion 环形依赖保护领域模型 |

`jfoundry-architecture` 是架构风格聚合 POM，不作为业务代码的直接运行时依赖。业务项目按需依赖具体模块。

## 组合原则

Layered 只表达职责分层，本身不作为最终对外的完整架构风格。业务项目通常应选择：

- Layered + Hexagonal
- Layered + Onion Simple
- Layered + Onion Classical

Hexagonal 和 Onion 都定义“应用核心如何与外部世界隔离”的主架构风格，正常项目应选择其中一种，不要在同一个 ArchUnit 分析范围内混用。

推荐组合：

- 端口/适配器项目：`jfoundry-layered` + `jfoundry-hexagonal`
- Onion Simple 项目：`jfoundry-layered` + `jfoundry-onion`
- Onion Classical 项目：`jfoundry-layered` + `jfoundry-onion`

## 启用规则

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] defaultRules = JFoundryRules.layeredHexagonal();
}
```

选择 Onion 时改用：

```java
@ArchTest
ArchRule[] rules = JFoundryRules.layeredOnionSimple();
```

`JFoundryRules.layeredHexagonal()`、`JFoundryRules.layeredOnionSimple()` 和 `JFoundryRules.layeredOnionClassical()` 分别给出基础守护规则 + Layered + 主风格的组合入口，并附带 Hexagonal/Onion 互斥规则，避免一个项目同时标注两种主风格。

## 权威参考

- jMolecules Architecture：<https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit：<https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters：<https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture：<https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
