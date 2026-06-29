# jfoundry

> jfoundry 是基于 jMolecules 的 DDD 开发框架，当前 main 分支面向生产级 1.x 版本线。当前支持矩阵和发布门禁见 `docs/release/compatibility.md`。

## 名称含义

`jfoundry` 可以理解为 Java / jMolecules 生态中的 “foundry”。foundry 原意是铸造厂或工坊，本项目希望提供一组可组合、可验证、可落地的 DDD 基础构件：领域模型、架构规则、持久化适配器、消息外部化和 Spring Boot 集成。它不是业务框架模板，而是帮助团队把领域建模和工程约束稳定落地的基础设施。

jfoundry 基于 jMolecules 的领域建模语义，并复用 jMolecules integrations 在 Jackson、Spring、ArchUnit 等生态中的集成能力，在此基础上补充面向业务项目的 Outbox、持久化适配器、Spring Boot starter 和架构规则组合。

## 特性

- **架构风格语义**：基于 jmolecules 的 Hexagonal、Onion 注解，配套 ArchUnit 规则强制依赖方向与风格选择；JFoundry 框架内部默认采用 Onion simplified，对外仍同时支持 Hexagonal 与 Onion；Layered 不再由 JFoundry 包装，确需使用时直接引入 jMolecules 原生模块
- **聚合根 / 值对象**：提供 `ValueObject` 标记接口，强制不可变 + `equals/hashCode` 契约
- **事务性发件箱 (Outbox)**：5 状态机（`PENDING` → `DISPATCHING` → `PUBLISHED` / `FAILED` / `DEAD_LETTERED`），原子化 `claimDispatchable` 避免多实例重复派发
- **消费端幂等 (Inbox)**：提供 `InboxTemplate` 与 MyBatis-Plus 存储适配器，帮助消费者按 message/consumer 去重
- **真实 broker adapter**：提供 Kafka `MessageSender` adapter；通过 messaging-kafka starter 显式引入
- **多 ORM 抽象**：核心 SPI 与具体实现解耦，当前提供 MyBatis-Plus 实现，未来可扩展 JPA / Mongo
- **多数据库支持**：MySQL（`MEDIUMTEXT`）、达梦 DM（`CLOB`），通过 Flyway 自动迁移
- **Spring Boot 自动装配**：按能力提供 starter，业务侧只引入需要的 messaging / outbox / inbox / adapter 组合
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
│   ├── jfoundry-messaging-core                   消息发送 SPI + 领域事件外部化应用契约 / rules
│   ├── jfoundry-outbox-core                      Outbox message store 契约 + 状态机 + dispatcher service
│   └── jfoundry-inbox-core                       Inbox message store 契约 + InboxTemplate
├── jfoundry-infrastructure                       基础设施层聚合
│   ├── jfoundry-persistence-core                 持久化抽象（AbstractPersistenceRepository）
│   ├── jfoundry-persistence-mybatis-plus         MyBatis-Plus 实现
│   ├── jfoundry-messaging-jackson                Jackson PayloadSerializer adapter
│   ├── jfoundry-messaging-kafka                  Kafka MessageSender adapter（可选）
│   ├── jfoundry-inbox-mybatis-plus               Inbox MyBatis-Plus store adapter
│   ├── jfoundry-outbox-mybatis-plus              Outbox MyBatis-Plus store adapter
│   ├── jfoundry-messaging-spring                 Spring 领域事件发布适配器 + 默认 MessageSender
│   ├── jfoundry-outbox-spring                    领域事件写入 Outbox 的 Spring 适配器 + scheduled 派发器
│   └── jfoundry-outbox-jobrunr                   Outbox 的 JobRunr 派发器（可选）
├── jfoundry-spring                               Spring 整合层聚合
│   ├── jfoundry-spring-boot-autoconfigure        Spring Boot AutoConfiguration
│   ├── jfoundry-spring-boot-starter              DDD + Spring Boot 基础 starter
│   ├── jfoundry-messaging-spring-boot-starter    Messaging 能力 starter
│   ├── jfoundry-outbox-spring-boot-starter       Outbox 能力 starter
│   ├── jfoundry-inbox-spring-boot-starter        Inbox 能力 starter
│   ├── jfoundry-mybatis-plus-spring-boot-starter MyBatis-Plus persistence starter
│   └── ...
└── jfoundry-test                                 ArchUnit 规则库（业务侧测试直接引用）
```

默认 Outbox 表名是 `jfoundry_outbox_event`，可通过 `jfoundry.outbox.table-name` 覆盖物理表名；自定义表需要与默认 DDL 保持同构。Kafka 是当前第一个真实 broker adapter，业务侧如需使用需额外引入 `jfoundry-messaging-kafka-spring-boot-starter` 并提供 `KafkaTemplate<String, String>`。消费者侧可注入 `InboxTemplate` 做幂等处理：

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

## 快速开始

### 1. 引入依赖

业务侧应按能力显式选择 starter：

- 基础 DDD + Spring Boot：`jfoundry-spring-boot-starter`
- Messaging：`jfoundry-messaging-spring-boot-starter`
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
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-spring-boot-starter</artifactId>
    </dependency>
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

领域事件不强制使用 Outbox。业务侧在应用服务上标注 `@ApplicationService` 后，框架会在成功返回的应用服务边界自动 drain 聚合记录的领域事件，并通过 `DomainEventDispatcher` 分发；默认 Spring 实现会在事务提交后通过 `ApplicationEventPublisher` 发布本地事件。如果业务只需要进程内监听器，可不配置 Outbox。

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

- [平台对齐指南](docs/platform-alignment.md)
- [值对象（Value Object）规范](docs/value-object.md)
- [架构风格指南](docs/architecture-styles.md)
- [ArchUnit 架构规则](docs/archunit-rules.md)
- [Transactional Outbox 事务性发件箱](docs/transactional-outbox.md)

## 技术栈

| 关注点 | 选型 |
|--------|------|
| JDK | 21 |
| Spring Boot | 3.5.16 |
| Spring | 6.2.19 |
| MyBatis-Plus | 3.5.16 |
| jmolecules | 2025.0.2（integrations 0.33.0） |
| ArchUnit | 1.4.2 |
| Jackson | 2.19.4 |
| Flyway | （业务侧提供，本仓库仅提供迁移脚本） |
| JobRunr | 8.7.1（可选） |

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
