# 分层架构使用指南

本文档只说明 Layered Architecture。若需要在 Layered 之外选择 Hexagonal 或 Onion，请先阅读
[架构风格指南](architecture-styles.md)。

## 快速开始

模块依赖：

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-layered</artifactId>
</dependency>
```

在业务模块的 `package-info.java` 上标注 layer：

```java
// src/main/java/com/mysoft/ci/env/appservice/package-info.java
@ApplicationLayer
package com.mysoft.ci.env.appservice;

import org.jfoundry.architecture.layered.ApplicationLayer;
```

四个层标记：

| 注解 | 目标 package | 典型内容 |
|------|-------------|---------|
| `@InterfaceLayer` | `*.controller`, `*.listener`, `*.schedule` | REST 控制器、MQTT 监听器、定时任务 |
| `@ApplicationLayer` | `*.appservice` | 应用服务、DTO、编排逻辑 |
| `@DomainLayer` | `*.domain`, `*.model` | 聚合根、实体、值对象、领域事件、Repository 接口 |
| `@InfrastructureLayer` | `*.infrastructure`, `*.adapter` | Repository 实现、外部服务适配器 |

## 依赖方向

```
InterfaceLayer → ApplicationLayer → DomainLayer ← InfrastructureLayer
```

关键约束（由 `LayeredRules` 或 `JFoundryRules.layered()` 强制）：

- **应用层不能依赖接口层或基础设施层** —— 业务内核必须与适配器解耦
- **Repository 实现只能在基础设施层** —— 防止业务层直接操作持久化

## 启用 ArchUnit 规则

```java
@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {
    @ArchTest
    ArchRule[] rules = JFoundryRules.all();
}
```

包含分层规则：

- `dependencies_must_follow_layer_hierarchy`
- `only_application_may_use_repository_directly`

`JFoundryRules.all()` 默认包含 Layered 规则。若业务项目选择 Hexagonal 或 Onion 作为主架构风格，
可额外启用 `JFoundryRules.hexagonal()`、`JFoundryRules.onionSimple()` 或
`JFoundryRules.onionClassical()`。

## 为什么不混入 Spring 的 @Service / @Component

分层（layer）和 bean 身份（bean role）是**两个正交关切**：

- **Layer** 是 package-level 关切：这个 package 属于哪一层？—— 用 `@ApplicationLayer` 回答
- **Bean role** 是 class-level 关切：这个 class 在 Spring 容器里是什么角色？—— 用 `@Service` / `@Component` 回答

框架**不**提供复合注解（如 `@ApplicationService`），因为它会让两个正交概念耦合在一起——
同一个类可以是应用服务（`@ApplicationLayer` + `@Service`）、可以是领域服务（`@DomainLayer` + `@Service`）、
也可以是适配器（`@InfrastructureLayer` + `@Component`）。
