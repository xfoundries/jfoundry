# jfoundry

> `jfoundry` 可以理解为 Java / jMolecules 生态中的 “foundry”。foundry 原意是铸造厂或工坊，本项目希望提供一组可组合、可验证、可落地的 DDD 基础构件，帮助团队把领域建模和工程约束稳定落地。

jfoundry 的核心模块保持运行时框架无关：DDD、架构风格、CQRS、领域事件、Outbox/Inbox 契约、持久化 SPI 和消息 SPI 不绑定 Spring。Spring 是当前第一套运行时集成，集中放在 `jfoundry-spring` 下；未来如果需要支持 Helidon、Micronaut、Quarkus 等运行时，应以平级集成模块扩展，而不是让核心层反向耦合某个框架。

jfoundry 基于 jMolecules 的领域建模语义，并复用 jMolecules integrations 在 Jackson、Spring、ArchUnit 等生态中的集成能力，在此基础上补充面向业务项目的 Outbox、持久化适配器、Spring Boot starter 和架构规则组合。

## 特性

- **架构风格语义**：基于 jmolecules 的 Hexagonal、Onion 注解，配套 ArchUnit 规则强制依赖方向与风格选择；JFoundry 框架内部默认采用 Onion simplified，对外仍同时支持 Hexagonal 与 Onion；Layered 不再由 JFoundry 包装，确需使用时直接引入 jMolecules 原生模块
- **聚合根 / 值对象**：提供 `ValueObject` 标记接口，强制不可变 + `equals/hashCode` 契约
- **事务性发件箱 (Outbox)**：5 状态机（`PENDING` → `DISPATCHING` → `PUBLISHED` / `FAILED` / `DEAD_LETTERED`），原子化 `claimDispatchable` 避免多实例重复派发
- **消费端幂等 (Inbox)**：提供 `InboxTemplate` 与 MyBatis-Plus 存储适配器，帮助消费者按 message/consumer 去重
- **真实 broker adapter**：提供 Kafka `MessageSender` adapter；通过 messaging-kafka starter 显式引入
- **多 ORM 抽象**：核心 SPI 与具体实现解耦，当前提供 MyBatis-Plus 实现，未来可扩展 JPA / Mongo
- **多数据库支持**：MySQL（`MEDIUMTEXT`）、达梦 DM（`CLOB`），通过 Flyway 自动迁移
- **运行时框架集成**：核心能力不绑定 Spring；当前提供 Spring Framework adapter 与 Spring Boot starter，未来可扩展 Helidon / Micronaut / Quarkus 等平级运行时集成
- **JobRunr 集成**（可选）：基于 JobRunr 的分布式 Outbox 派发
- **ArchUnit 规则库**：开箱即用的架构守护，覆盖持久化层无 `@Transactional`、自动配置零 `@Component`、值对象不可变等约束

## 模块清单

```
jfoundry-parent
├── jfoundry-dependencies                         BOM（依赖版本集中管理）
├── jfoundry-domain                               领域层（实体 / 值对象 / 事件 / 仓储接口，零 Spring 依赖）
├── jfoundry-architecture                         架构风格聚合 POM（零 Spring 依赖，基于 jmolecules）
│   ├── jfoundry-hexagonal                        Hexagonal Architecture 端口/适配器注解
│   └── jfoundry-onion                            Onion Architecture 环形注解
├── jfoundry-application                          应用层聚合
│   ├── jfoundry-event-core                       领域事件登记 / 分发应用契约
│   ├── jfoundry-event-externalization-core       领域事件外部化规则与路由元数据
│   ├── jfoundry-messaging-core                   消息发送与 payload 序列化 SPI
│   ├── jfoundry-outbox-core                      Outbox message store 契约 + 状态机 + dispatcher service
│   └── jfoundry-inbox-core                       Inbox message store 契约 + InboxTemplate
├── jfoundry-infrastructure                       基础设施层聚合
│   ├── jfoundry-persistence-core                 持久化抽象（AbstractPersistenceRepository）
│   ├── jfoundry-persistence-mybatis-plus         MyBatis-Plus 实现
│   ├── jfoundry-messaging-jackson                Jackson PayloadSerializer adapter
│   ├── jfoundry-messaging-kafka                  Kafka MessageSender adapter（可选）
│   ├── jfoundry-inbox-mybatis-plus               Inbox MyBatis-Plus store adapter
│   ├── jfoundry-outbox-mybatis-plus              Outbox MyBatis-Plus store adapter
│   └── jfoundry-outbox-jobrunr                   Outbox 的 JobRunr 派发器（可选）
├── jfoundry-starters                             非 Spring 能力聚合入口
│   ├── jfoundry-domain-starter                   领域建模 + 架构边界语义
│   ├── jfoundry-application-starter              应用层契约 + CQRS + 领域 starter
│   └── jfoundry-infrastructure-mybatis-plus-starter MyBatis-Plus 持久化能力
├── jfoundry-spring                               Spring 生态集成聚合
│   ├── jfoundry-spring-runtime                   Spring Framework 运行时适配器
│   │   ├── jfoundry-event-spring                 Spring ApplicationEvent 领域事件发布适配器
│   │   ├── jfoundry-messaging-spring             Spring 默认 LoggingMessageSender 适配器
│   │   └── jfoundry-outbox-spring                领域事件写入 Outbox 的 Spring 适配器 + scheduled 派发器
│   ├── jfoundry-spring-boot-autoconfigure        Spring Boot AutoConfiguration
│   └── jfoundry-spring-boot-starters             Spring Boot starter 依赖入口
│       ├── jfoundry-spring-boot-starter          DDD + Spring Boot 基础 starter
│       ├── jfoundry-event-spring-boot-starter    领域事件 Spring 发布 starter
│       ├── jfoundry-messaging-spring-boot-starter Messaging transport 能力 starter
│       ├── jfoundry-outbox-spring-boot-starter   Outbox 能力 starter
│       ├── jfoundry-inbox-spring-boot-starter    Inbox 能力 starter
│       ├── jfoundry-mybatis-plus-spring-boot-starter MyBatis-Plus persistence starter
│       └── ...
├── jfoundry-architecture
│   └── jfoundry-architecture-test                架构测试规则库（业务侧测试直接引用）
└── jfoundry-verification
    └── jfoundry-middleware-integration-tests     中间件集成验证（框架内部）
```

默认 Outbox 表名是 `jfoundry_outbox_event`，可通过 `jfoundry.outbox.table-name` 覆盖物理表名；自定义表需要与默认 DDL 保持同构。Kafka 是当前第一个真实 broker adapter，业务侧如需使用需额外引入 `jfoundry-messaging-kafka-spring-boot-starter` 并提供 `KafkaTemplate<String, String>`。消费者侧可注入 `InboxTemplate` 做幂等处理：

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

## 业务项目接入入口

如果你是在业务项目中首次接入 jfoundry，请先阅读 [业务项目接入指南](docs/getting-started-for-business-projects.md)。它会先帮你确定项目形态、架构风格、依赖选择、包结构、ArchUnit 架构测试，以及 Outbox / Inbox 是否应该启用。

如果你使用 AI Agent 辅助开发，本仓库提供了 [use-jfoundry skill](skills/use-jfoundry/SKILL.md)。该 skill 是给 Agent 使用的英文指令集，业务开发者通常不需要直接阅读；你可以在支持 Codex skills 的环境中让 Agent 使用 `$use-jfoundry`，例如：

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 Spring Boot business project.
Base package: com.example.order
Project shape: multi-module Maven
Persistence: MyBatis-Plus
Messaging: Kafka later, not in the initial skeleton
Architecture: default
```

## 快速开始

### 1. 引入依赖

业务侧应按能力显式选择 starter：

- 领域层：`jfoundry-domain-starter`
- 应用层：`jfoundry-application-starter`
- MyBatis-Plus 基础设施层：`jfoundry-infrastructure-mybatis-plus-starter`
- 基础 DDD + Spring Boot：`jfoundry-spring-boot-starter`
- Event Spring bridge：`jfoundry-event-spring-boot-starter`
- Messaging transport：`jfoundry-messaging-spring-boot-starter`
- Kafka adapter：`jfoundry-messaging-kafka-spring-boot-starter`
- Outbox：`jfoundry-outbox-spring-boot-starter`
- Outbox MyBatis-Plus store：`jfoundry-outbox-mybatis-plus-spring-boot-starter`
- Outbox JobRunr dispatcher：`jfoundry-outbox-jobrunr-spring-boot-starter`
- Inbox：`jfoundry-inbox-spring-boot-starter`
- Inbox MyBatis-Plus store：`jfoundry-inbox-mybatis-plus-spring-boot-starter`
- MyBatis-Plus business persistence：`jfoundry-mybatis-plus-spring-boot-starter`

MyBatis-Plus 项目示例：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- domain module -->
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-domain-starter</artifactId>
    </dependency>

    <!-- application module -->
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-application-starter</artifactId>
    </dependency>

    <!-- infrastructure module -->
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
    </dependency>

    <!-- Spring Boot assembly module -->
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

MyBatis-Plus + Outbox + Inbox 项目示例：

```xml
<dependencies>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-outbox-mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-inbox-mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Spring Data / JPA 项目示例：

```xml
<dependencies>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 2. 创建领域模型

```java
import org.jfoundry.domain.valueobject.ValueObject;

// 值对象（推荐用 record，天生满足不可变 + equals/hashCode）
public record Money(BigDecimal amount, String currency) implements ValueObject {}

// 聚合根
@AggregateRoot
public class Order {
    private OrderId id;
    private Money total;
    // ...
}
```

### 3. 启用架构守护（推荐）

在业务模块的测试目录下添加一个 ArchUnit 测试，引用框架自带的规则：

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyAppArchitectureTest {
    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.onionSimple();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();
}
```

### 4. 可选：配置领域事件外部化（Outbox）

领域事件不强制使用 Outbox。业务侧在应用服务上标注 `@ApplicationService` 后，框架会在成功返回的应用服务边界自动 drain 聚合记录的领域事件，并通过 `DomainEventDispatcher` 分发；`jfoundry-event-spring-boot-starter` 提供的 Spring 实现会在事务提交后通过 `ApplicationEventPublisher` 发布本地事件。如果业务只需要进程内监听器，可不配置 Outbox。

当事件需要可靠外部化、跨进程投递或失败重试时，再为事件标记 `@Externalized` / `@MessageRouting`，并启用 Outbox 存储与派发。业务侧只要提供 `OutboxMessageStore` Bean（或引入 `jfoundry-outbox-mybatis-plus-spring-boot-starter`），匹配外部化规则的领域事件就会写入 Outbox 表：

```yaml
jfoundry:
  outbox:
    table-name: jfoundry_outbox_event       # 与 Flyway 迁移脚本一致
    dispatcher:
      enabled: true
      mode: scheduled                  # 或 jobrunr（需额外引入 jfoundry-outbox-jobrunr-spring-boot-starter）
    cleanup:
      published-retention-days: 7      # PUBLISHED 状态保留 7 天后清理
      dead-lettered-retention-days: 30 # DEAD_LETTERED 保留 30 天
```

## 文档

- [业务项目接入指南](docs/getting-started-for-business-projects.md)
- [值对象（Value Object）规范](docs/value-object.md)
- [架构风格指南](docs/architecture-styles.md)
- [ArchUnit 架构规则](docs/archunit-rules.md)
- [Repository 与读侧端口迁移指南](docs/repository-vs-read-ports.md)
- [Transactional Outbox 事务性发件箱](docs/transactional-outbox.md)

## 技术栈

| 关注点       | 选型                                 |
|--------------|--------------------------------------|
| JDK          | 21（推荐25）                         |
| Spring Boot  | 3.5.16                               |
| Spring       | 6.2.19                               |
| MyBatis-Plus | 3.5.16                               |
| jmolecules   | 2025.0.2（integrations 0.33.0）      |
| ArchUnit     | 1.4.2                                |
| Jackson      | 2.19.4                               |
| Flyway       | （业务侧提供，本仓库仅提供迁移脚本） |
| JobRunr      | 8.7.1（可选）                        |

## 构建

```bash
# 完整构建（含测试）
mvn clean install

# 跳过测试
mvn clean install -DskipTests

# 仅验证 Maven 模块结构
mvn validate
```

> **JVM 参数提示**：Specification 规约模式需要通过反射从方法引用中提取字段名，构建时需要 `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED`。`jfoundry-spring-boot-autoconfigure` 等模块的 surefire 插件已配置；业务侧如需可参考 `.mvn/jvm.config`。

## License

[Apache License 2.0](LICENSE)
