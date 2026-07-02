# 业务项目接入指南

本文面向准备在业务系统中使用 jfoundry 的开发者和 AI Agent 使用者。README 负责介绍 jfoundry 的整体能力，本文负责回答“一个新业务项目第一步该怎么接入”。

如果你使用 AI Agent 辅助开发，请优先让 Agent 使用仓库内的 `skills/use-jfoundry` skill。该 skill 是英文的 Agent 指令集，业务开发者通常不需要直接阅读；你只需要把本文中的提示词交给支持 Codex skills 的 Agent。

## 什么时候选择 jfoundry

jfoundry 适合这类业务系统：

- 有明确领域模型、聚合、值对象、领域事件或业务不变量。
- 希望用 Hexagonal 或 Onion Architecture 保护领域层和应用层边界。
- 希望用 ArchUnit 把架构约束变成自动化测试。
- 需要可靠领域事件外部化，例如 Transactional Outbox、消息重试、死信状态。
- 需要消费端幂等，例如 Inbox。
- 希望在 MyBatis-Plus、消息中间件、Spring Boot 自动装配之间保留清晰边界。

如果项目只是短期 CRUD 原型、没有领域复杂度、也不需要架构守护，直接使用 Spring Boot + ORM 可能更简单。

## 推荐默认路线

新业务项目建议从最小骨架开始：

- Java 21
- Maven
- Spring Boot
- Hexagonal Architecture
- `jfoundry-spring-dependencies` BOM
- `jfoundry-spring-boot-starter`
- 先添加 ArchUnit 架构测试
- 先不启用 Outbox、Inbox、MQ adapter
- 明确需要持久化时再加入 MyBatis-Plus 或其他持久化实现

这样可以先稳定 domain、application、adapter 和 infrastructure 的边界，再按业务需要逐步打开 Outbox、Inbox、Kafka、RabbitMQ、RocketMQ、JobRunr 等能力。

## 新项目第一步

开始前先确定 5 个问题：

| 问题 | 推荐默认值 |
|------|------------|
| 基础包名 | 业务自己的包名，例如 `com.example.order` |
| 项目形态 | 复杂项目用多模块 Maven；小项目可先单 Spring Boot app |
| 架构风格 | 默认 Hexagonal；团队明确偏好 Onion 时再选 Onion |
| 持久化 | 未确定时先不绑定；使用 MyBatis-Plus 时显式加入对应 starter |
| 外部消息 | 未确定时先不启用 Outbox/Inbox/MQ |

如果使用 AI Agent，可以直接给出：

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 Spring Boot business project.
Base package: com.example.order
Project shape: multi-module Maven
Persistence: MyBatis-Plus
Messaging: Kafka later, not in the initial skeleton
Architecture: default
```

这里的 `Architecture: default` 会让 Agent 使用 jfoundry 推荐的新项目默认值：Hexagonal Architecture。

## 依赖选择原则

业务侧应按能力显式选择 starter，不要一次性引入所有模块。

最小 Spring Boot 入口：

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

<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
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
- 架构测试：`jfoundry-architecture-test`，`test` scope

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

读侧能力按用途拆分：

- `LookupPort`：应用服务为了执行业务流程读取轻量上下文。
- `ReadModelPort`：页面、列表、报表、统计和投影查询。
- `MaintenancePort`：后台扫描、清理、重试、修复候选选择。

详细判断规则见 [Repository 与读侧端口迁移指南](repository-vs-read-ports.md)。

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

`skills/use-jfoundry` 是给 Agent 使用的英文 skill，包含：

- 新项目接入流程
- Maven 依赖模板
- Hexagonal / Onion 包结构模板
- ArchUnit 测试模板
- Repository / Port 判断规则
- Outbox / Inbox 接入判断

业务侧推荐流程：

1. 先让 Agent 使用 `$use-jfoundry` 生成项目骨架和架构测试。
2. 再围绕第一个 bounded context 建模聚合和值对象。
3. 每新增一个外部系统、数据库查询或消息链路，都让 Agent 先判断它是 Repository、LookupPort、ReadModelPort、MaintenancePort、Outbox 还是 Inbox。
4. 每次变更后运行 Maven 测试和 ArchUnit 测试。

推荐提示词：

```text
Use $use-jfoundry to add the first bounded context for order management.
Keep the domain model free of Spring and MyBatis.
Use Hexagonal Architecture.
Add architecture tests before implementation.
```

## 下一步阅读

- [架构风格指南](architecture-styles.md)
- [ArchUnit 架构规则](archunit-rules.md)
- [Repository 与读侧端口迁移指南](repository-vs-read-ports.md)
- [Transactional Outbox](transactional-outbox.md)
- [值对象规范](value-object.md)

