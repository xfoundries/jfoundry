# 业务项目接入指南

本文面向准备在业务系统中使用 jfoundry 的开发者和 AI Agent 使用者。README 负责介绍 jfoundry 的整体能力，本文负责回答“一个新业务项目第一步该怎么接入”。

如果你使用 AI Agent 辅助开发，建议先安装 `xfoundries/software-architecture-skills` 提供的 `domain-architecture` 插件，并让 Agent 使用其中的 `$use-jfoundry` skill。该 skill 是英文的 Agent 指令集，业务开发者通常不需要直接阅读；你只需要把本文中的提示词交给支持插件或 skills 的 Agent。

Codex 安装示例：

```bash
codex plugin marketplace add xfoundries/software-architecture-skills
codex plugin add domain-architecture@xfoundries
```

Claude Code 安装示例：

```bash
claude plugin marketplace add xfoundries/software-architecture-skills
claude plugin install domain-architecture@xfoundries
```

## 什么时候选择 jfoundry

jfoundry 适合这类业务系统：

- 有明确领域模型、聚合、值对象、领域事件或业务不变量。
- 希望用 Hexagonal 或 Onion Architecture 保护领域层和应用层边界。
- 希望用 ArchUnit 把架构约束变成自动化测试。
- 需要可靠领域事件外部化，例如 Transactional Outbox、消息重试、死信状态。
- 需要消费端幂等，例如 Inbox。
- 希望在运行时框架、持久化实现、消息中间件和自动装配之间保留清晰边界。

如果项目只是短期 CRUD 原型、没有领域复杂度、也不需要架构守护，直接使用所选运行时框架 + ORM 可能更简单。

## 推荐默认路线

新业务项目建议从最小骨架开始：

- Java 21
- Maven
- Hexagonal Architecture
- 先选择 `jfoundry-dependencies` 或 `jfoundry-spring-dependencies` BOM
- 只引入当前需要的 starter
- 先添加 ArchUnit 架构测试
- 先不启用 Outbox、Inbox、MQ adapter
- 明确运行时、持久化、消息中间件后再加入对应集成

这样可以先稳定 domain、application、adapter 和 infrastructure 的边界，再按业务需要逐步打开 Spring、Spring Boot、MyBatis-Plus、Outbox、Inbox、Kafka、RabbitMQ、RocketMQ、JobRunr 等能力。

## 新项目第一步

开始前先确定 6 个问题：

| 问题 | 推荐默认值 |
|------|------------|
| 基础包名 | 业务自己的包名，例如 `com.example.order` |
| 项目形态 | 正常 DDD 项目推荐多模块 Maven；小项目可先单应用模块 |
| 架构风格 | 默认 Hexagonal；团队明确偏好 Onion 时再选 Onion |
| 运行时框架 | 未确定时先不绑定；使用 Spring Boot 时显式选择 Spring Boot starter |
| 持久化 | 未确定时先不绑定；使用 MyBatis-Plus 时显式加入对应 starter |
| 外部消息 | 未确定时先不启用 Outbox/Inbox/MQ |

如果使用 AI Agent，可以直接给出：

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 business project.
Base package: com.example.order
Project shape: multi-module Maven
Runtime: undecided
Persistence: MyBatis-Plus
Messaging: Kafka later, not in the initial skeleton
Architecture: default
```

这里的 `Architecture: default` 会让 Agent 使用 jfoundry 推荐的新项目默认值：Hexagonal Architecture。

## 依赖选择原则

业务侧先在父 POM 选择 BOM，再按模块职责显式选择 starter，不要一次性引入所有模块。

### 1. 选择 BOM

| 项目类型 | 推荐 BOM | 使用场景 |
|----------|----------|----------|
| 框架无关核心项目 | `jfoundry-dependencies` | 只使用 DDD、架构注解、应用契约、Outbox/Inbox SPI、框架无关 adapter |
| Spring / Spring Boot 项目 | `jfoundry-spring-dependencies` | 使用 Spring Framework adapter、Spring Boot auto-configuration 或 Spring Boot starter |

框架无关核心项目：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Spring / Spring Boot 项目：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-spring-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按模块放置 starter

多模块 Maven 是正常 DDD 项目的推荐形态，因为依赖边界可以被 Maven 直接守住。依赖放置原则如下：

| 模块 / 层 | 应放依赖 | 不应放 |
|-----------|----------|--------|
| `domain` | `jfoundry-domain-starter` | Spring、MyBatis-Plus、JPA、MQ client、HTTP client、Spring Boot starter |
| `application` | `jfoundry-application-starter` | Spring Boot starter、MyBatis mapper/service、broker adapter |
| `infrastructure` | `jfoundry-infrastructure-mybatis-plus-starter`（仅使用框架无关 MyBatis-Plus adapter 时） | Controller、应用入口、Spring Boot 自动装配 starter |
| `boot` / 运行时装配模块 | `jfoundry-spring-boot-starter`，以及按需加入 `jfoundry-mybatis-plus-spring-boot-starter`、Outbox、Inbox、broker starter | 领域模型和业务规则实现 |
| 架构测试模块或测试源集 | `jfoundry-architecture-test`，`test` scope | production scope |

domain 模块：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-domain-starter</artifactId>
    </dependency>
</dependencies>
```

application 模块：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-application-starter</artifactId>
    </dependency>
</dependencies>
```

boot / 运行时装配模块：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring Boot + MyBatis-Plus runtime assembly. -->
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. 应用层需要编程式事务时使用 TransactionRunner

如果应用服务只需要整方法事务，Spring 项目可以继续使用 `@Transactional`。如果应用编排中只有某个代码块需要事务，或者 application 模块不希望依赖 Spring `TransactionTemplate`，可以通过 `jfoundry-application-starter` 使用 `TransactionRunner`；如果只需要事务契约，也可以直接依赖 `jfoundry-transaction-core`：

```java
transactionRunner.run(TransactionOptions.builder()
        .name("create-env-app")
        .timeout(Duration.ofSeconds(30))
        .build(), () -> {
    envAppRepository.save(envApp);
    operationRecordRepository.save(record);
});
```

`TransactionRunner` 只能用于 UseCase / Application Service 等应用层编排。领域对象和领域服务不应直接控制事务。Spring Boot 项目在 boot 模块引入 `jfoundry-spring-boot-starter` 后，只要存在 `PlatformTransactionManager`，框架会自动装配基于 Spring `TransactionTemplate` 的实现；没有事务管理器时不会创建该 Bean。

架构测试依赖放在执行 ArchUnit 测试的模块中，通常是 boot 模块的测试源集，或单独的 architecture-test 模块：

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-architecture-test</artifactId>
    <scope>test</scope>
</dependency>
```

按需追加：

- 领域模块：`jfoundry-domain-starter`
- 应用模块：`jfoundry-application-starter`
- MyBatis-Plus 业务持久化：`jfoundry-infrastructure-mybatis-plus-starter` 或 `jfoundry-mybatis-plus-spring-boot-starter`
- 本地 Spring 领域事件发布：`jfoundry-event-spring-boot-starter`
- Outbox：`jfoundry-outbox-spring-boot-starter`
- Outbox MyBatis-Plus 存储：`jfoundry-outbox-mybatis-plus-spring-boot-starter`
- Inbox：`jfoundry-inbox-spring-boot-starter`
- Inbox MyBatis-Plus 存储：`jfoundry-inbox-mybatis-plus-spring-boot-starter`
- Kafka adapter：`jfoundry-messaging-kafka-spring-boot-starter`
- RabbitMQ adapter：`jfoundry-messaging-rabbitmq-spring-boot-starter`
- RocketMQ adapter：`jfoundry-messaging-rocketmq-spring-boot-starter`
- Redisson 分布式锁：`jfoundry-lock-redisson-spring-boot-starter`
- 架构测试：`jfoundry-architecture-test`，`test` scope

单应用模块可以作为小项目的折中做法，但不要因此把职责混在一起：仍应保留 domain、application、adapter、infrastructure 包边界，并让 ArchUnit 测试覆盖这些包。

## 推荐包结构

新项目默认推荐 Hexagonal：

```text
com.example.order
├── boot
├── domain
│   ├── model
│   ├── event
│   └── repository
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   └── service
├── adapter
│   ├── in
│   │   ├── web
│   │   ├── messaging
│   │   └── scheduler
│   └── out
│       ├── persistence
│       ├── messaging
│       └── client
└── infrastructure
    └── config
```

核心约束：

- `domain` 不依赖 Spring、MyBatis、JPA、MQ client、HTTP client 或持久化数据对象。
- `application` 放用例编排、应用服务、命令、查询入口和端口契约。
- `adapter.in` 是 Controller、消息监听器、调度器等主适配器，只调用应用入口。
- `adapter.out` 实现应用层定义的出站端口。
- `infrastructure` 放运行时配置、技术装配和框架集成。

## 持久化 Data 与转换器

使用 `jfoundry-infrastructure-mybatis-plus-starter` 时，领域聚合根和 MyBatis-Plus Data 对象应保持分离：

- 领域聚合根不放 MyBatis-Plus 注解、TypeHandler、逻辑删除字段或自动填充字段。
- Data 对象继承 `AggregateData<K>`，`K` 使用数据库天然支持的主键类型。
- `DataConverter` 负责领域强类型 ID 与持久化主键类型之间的转换。
- converter 默认不作为 Spring Bean 注入；MapStruct converter 推荐使用 `@Mapper` + `Mappers.getMapper(...)` 暴露 `INSTANCE`。
- `toData(...)` 可以由 MapStruct 生成，`toEntity(...)` 推荐手写并调用聚合 `restore(...)`，保留清晰的持久化还原语义。

详细规则和示例见 [持久化 DataConverter 与 MapStruct 使用指南](persistence-data-converters.md)。

## 架构测试先行

新项目应尽早添加 ArchUnit 测试。Hexagonal 项目推荐：

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.example.order")
class OrderArchitectureTest {

    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.hexagonalStrict();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();

    @ArchTest
    ArchRule[] aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();
}
```

`hexagonalStrict()` 会启用 jfoundry 基础守护规则、jMolecules Hexagonal 规则和 jfoundry 推荐落地约定。`jmoleculesDdd()` 会启用聚合引用和值对象相关的 jMolecules DDD 规则。`aggregateRepositoryConventions()` 用于防止聚合仓储接口泄漏分页、条件对象和持久化 service/mapper 类型。

如果项目选择 Onion Simple，将主规则替换为：

```java
@ArchTest
ArchRule[] jfoundryRules = JFoundryRules.onionSimple();
```

## Repository 与读侧端口

聚合 Repository 表示某类聚合的集合，适合：

- 按聚合 ID 或稳定业务身份加载聚合。
- 保存、删除聚合。
- 命令流程中加载聚合并立即调用聚合行为。

不要把分页列表、报表、页面 DTO、Dashboard 统计、MyBatis-Plus `Wrapper`、Spring Data `Pageable` 等查询能力塞进聚合 Repository。

读侧能力可以按用途渐进拆分。`LookupPort`、`ReadModelPort`、`MaintenancePort` 是 jfoundry 推荐的分类和命名后缀，不是强制规范；业务项目也可以使用已有的 `QueryPort`、`Finder`、`Gateway`、`Scanner` 等命名。

- `LookupPort`：应用服务为了执行业务流程读取轻量上下文。
- `ReadModelPort`：页面、列表、报表、统计和投影查询。
- `MaintenancePort`：后台扫描、清理、重试、修复候选选择。

详细判断规则见 [Repository 与读侧端口迁移指南](../modeling/repository-vs-read-ports.md)。

## Outbox 与 Inbox 何时启用

Outbox 只在领域事件需要可靠投递到进程外系统时启用，例如 Kafka、RabbitMQ、RocketMQ、跨服务通知、失败重试和最终一致性链路。如果事件只需要进程内 Spring 监听器处理，不需要配置 Outbox。

Inbox 用于消费端幂等。当消费者可能收到重复消息或重试消息时，用 `InboxTemplate` 包住处理逻辑：

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

详细说明见 [Transactional Outbox](transactional-outbox.md)。

## AI Agent 使用方式

`$use-jfoundry` 是 `xfoundries/software-architecture-skills` 的 `domain-architecture` 插件提供给 Agent 使用的英文 skill，包含：

- 新项目接入流程
- Maven 依赖模板
- Hexagonal / Onion 包结构模板
- ArchUnit 测试模板
- Repository / Port 判断规则
- Outbox / Inbox 接入判断

业务侧推荐流程：

1. 先让 Agent 使用 `$use-jfoundry` 生成项目骨架和架构测试。
2. 再围绕第一个 bounded context 建模聚合和值对象。
3. 每新增一个外部系统、数据库查询或消息链路，都让 Agent 先判断它是聚合 Repository、读侧端口、维护端口、Outbox 还是 Inbox；读侧端口可按复杂度再细分为 Lookup、ReadModel 或 Maintenance。
4. 每次变更后运行 Maven 测试和 ArchUnit 测试。

推荐提示词：

```text
Use $use-jfoundry to add the first bounded context for order management.
Keep the domain model free of Spring and MyBatis.
Use Hexagonal Architecture.
Add architecture tests before implementation.
```

## 下一步阅读

- [架构风格指南](../framework/architecture-styles.md)
- [ArchUnit 架构规则](../framework/archunit-rules.md)
- [Repository 与读侧端口迁移指南](../modeling/repository-vs-read-ports.md)
- [应用事务](application-transactions.md)
- [分布式锁](distributed-locks.md)
- [Transactional Outbox](transactional-outbox.md)
- [值对象规范](../modeling/value-object.md)
