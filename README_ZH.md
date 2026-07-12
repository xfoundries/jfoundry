# jfoundry

中文 | [English](README.md)

---

`jfoundry` 是一个面向 Java 的可落地 DDD 开发框架，基于 jMolecules 构建，天然适配公认的 Hexagonal Architecture（六边形架构）和 Onion Architecture（洋葱架构）。

它的核心目标很明确：让业务项目真正落地领域建模、架构边界和可靠集成，同时让核心代码保持运行时框架无关。DDD、架构语义、应用层契约、领域事件、Outbox/Inbox、持久化 SPI 和消息 SPI 都不绑定 Spring、Spring Boot、Quarkus、Helidon 或 Micronaut。

Spring 是当前第一套运行时集成，位于 `jfoundry-spring`。未来扩展 Quarkus、Helidon、Micronaut 等运行时时，应作为平级集成模块接入同一套核心 SPI，而不是让核心层反向依赖某个运行时框架。

## 为什么是 jfoundry

很多 DDD 项目失败不是因为缺少概念，而是因为概念没有落到工程边界里：领域层被 Spring、ORM、MQ API 污染；应用层事务边界不清；Repository 逐渐变成通用查询接口；领域事件无法可靠外部化；架构规则停留在文档里。

`jfoundry` 解决的是这类落地问题：

- 用 jMolecules 表达 DDD、Hexagonal、Onion、CQRS 等架构语义。
- 用 Maven 模块、starter 和 SPI 维持领域、应用、基础设施、运行时集成之间的依赖方向。
- 用 ArchUnit 规则把架构约束变成可执行测试。
- 用 Outbox/Inbox、消息 SPI、持久化 SPI、事务边界契约补齐生产级应用常见基础能力。

## 核心原则

### 领域优先

领域模型是中心。聚合、值对象、领域事件、仓储接口、领域异常等核心类型不依赖 Spring、ORM、HTTP、MQ 或数据库客户端。

### 架构友好

DDD 不是六边形架构，也不是洋葱架构；它们是不同层面的设计工具。`jfoundry` 对 Hexagonal Architecture 和 Onion Architecture 提供一等支持，让项目可以显式选择架构风格，并通过规则守住依赖方向。

### 核心无运行时框架依赖

核心模块只表达业务建模、应用层编排和外部能力契约。Spring Boot 自动配置、Spring 事务、Spring 事件、Web MVC、JobRunr 等都属于外层运行时集成。

### 面向生产可靠性

框架内置事务性 Outbox、消费端 Inbox、应用层事务边界、消息发送 SPI、payload 序列化 SPI、可选 MyBatis-Plus 与 Jakarta Persistence 持久化适配器和可复用架构测试规则。

## 能力概览

| 领域 | 能力 |
|------|------|
| DDD 基础构件 | 聚合根、实体基类、值对象标记、领域事件、仓储契约、领域异常 |
| 架构风格 | 基于 jMolecules 的 Hexagonal / Onion 架构注解，以及配套 ArchUnit 规则 |
| 应用层 | `ApplicationService`、应用异常、`TransactionRunner`、CQRS 语义 |
| 领域事件 | 事件记录、作用域事件上下文、分发契约、Spring ApplicationEvent 适配器 |
| 可靠消息 | Transactional Outbox、Inbox 幂等、broker 无关 `MessageSender`、payload 序列化 SPI |
| 持久化 | 框架无关持久化契约，以及可选 MyBatis-Plus 与 Jakarta Persistence 适配器 |
| 运行时集成 | 可选 Spring Framework / Spring Boot starter、自动配置、Web MVC ProblemDetail 支持 |
| 验证 | 面向业务项目和框架内部模块边界的可复用 ArchUnit 规则 |

## 架构分层

`jfoundry` 显式区分以下边界：

```text
domain
  DDD 模型、值对象、领域事件、仓储契约

application
  应用服务、事务边界契约、CQRS、Outbox/Inbox SPI、
  领域事件编排、消息 SPI

infrastructure
  持久化适配器、消息适配器、序列化适配器、后台任务适配器

运行时集成
  Spring Framework 适配器、Spring Boot 自动配置和 starter
```

依赖方向指向内层。运行时集成模块负责装配核心契约和技术适配器，但核心模块不依赖运行时集成：

```text
运行时集成
  -> application / infrastructure adapters
       -> application contracts
            -> domain
```

同一套核心能力今天可以由 Spring 装配，未来也可以由 Quarkus、Helidon、Micronaut 或其他运行时装配。

## 快速开始

先按运行时选择 BOM，再按模块职责显式引入 starter。

```xml
<dependencyManagement>
    <dependencies>
        <!-- 运行时框架无关核心：DDD、架构语义、应用层契约、SPI。 -->
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

如果项目使用 Spring Boot，改用 Spring BOM：

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-spring-dependencies</artifactId>
    <version>${jfoundry.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

典型模块依赖：

```xml
<!-- domain 模块 -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-domain-starter</artifactId>
</dependency>

<!-- application 模块 -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-application-starter</artifactId>
</dependency>

<!-- Spring Boot 运行时装配模块，可选 -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-spring-boot-starter</artifactId>
</dependency>
```

持久化、Outbox、Inbox、broker starter 应按业务需要显式引入，不建议一次性加入所有 starter。按模块选择依赖的详细说明见 [业务项目接入指南](docs/i18n/zh/integration/getting-started-for-business-projects.md)。

## 领域模型示例

```java
import org.jfoundry.domain.valueobject.ValueObject;
import org.jmolecules.ddd.annotation.AggregateRoot;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements ValueObject {
}

@AggregateRoot
public class Order {

    private OrderId id;
    private Money total;

    public void changeTotal(Money total) {
        this.total = total;
    }
}
```

## 架构规则验证

架构规则应该运行在业务项目测试里，而不是只停留在框架文档中：

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyAppArchitectureTest {

    @ArchTest
    static final ArchTests onion = JFoundryRules.onionSimple();

    @ArchTest
    static final ArchTests ddd = JFoundryRules.jmoleculesDdd();
}
```

项目选择六边形架构时使用 `JFoundryRules.hexagonalStrict()`；选择洋葱架构时使用 `JFoundryRules.onionSimple()` 或 `JFoundryRules.onionClassical()`。

聚合 Repository 在不同架构风格下都保持独立的 DDD 身份。Hexagonal 项目可以同时将它标记为 `@SecondaryPort`，而无需移出 `domain.repository`；Onion 项目则把契约放在内环、实现放在基础设施环。

## 可靠事件外部化

领域事件可以只在进程内发布，也可以通过 Outbox 可靠外部化：

```text
聚合记录领域事件
  -> 应用服务边界收集事件
  -> 外部化规则选择 topic/key/payload
  -> 同一数据库事务内写入 Outbox
  -> 派发器 claim 并通过 MessageSender 发送
  -> 消费侧通过 InboxTemplate 按 message/consumer 幂等处理
```

Outbox 是可选能力。只有当事件需要跨进程投递、失败重试或可靠外部化时才需要启用。详细说明见 [Transactional Outbox](docs/i18n/zh/integration/transactional-outbox.md)。

## 模块说明

| 模块 | 用途 |
|------|------|
| `jfoundry-domain` | 领域基础类型、值对象契约、领域事件、仓储契约 |
| `jfoundry-architecture` | Hexagonal、Onion、CQRS 架构语义和架构测试规则 |
| `jfoundry-application` | 应用服务契约、事务边界、领域事件编排、消息、Outbox/Inbox SPI |
| `jfoundry-infrastructure` | 持久化、序列化、消息发送、后台派发等技术适配器 |
| `jfoundry-starters` | 运行时框架无关的依赖入口 |
| `jfoundry-spring` | Spring Framework 适配器、Spring Boot 自动配置和 starter |
| `jfoundry-verification` | 框架内部中间件和运行时行为验证 |

## 文档

- [中文文档索引](docs/i18n/zh/index.md)
- 框架语义：[架构风格指南](docs/i18n/zh/framework/architecture-styles.md)、[ArchUnit 架构规则](docs/i18n/zh/framework/archunit-rules.md)、[框架边界设计](docs/i18n/zh/framework/framework-boundaries.md)
- 建模约定：[值对象规范](docs/i18n/zh/modeling/value-object.md)、[Repository 与读侧端口迁移指南](docs/i18n/zh/modeling/repository-vs-read-ports.md)
- 技术集成：[业务项目接入指南](docs/i18n/zh/integration/getting-started-for-business-projects.md)、[持久化 DataConverter 与 MapStruct 使用指南](docs/i18n/zh/integration/persistence-data-converters.md)、[Transactional Outbox 事务性发件箱](docs/i18n/zh/integration/transactional-outbox.md)

## 构建

```bash
mvn validate
mvn test
mvn clean install
```

## 许可证

[Apache License 2.0](LICENSE)
