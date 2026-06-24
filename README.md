# jfoundry

> 基于 [jmolecules](https://jmolecules.org/) 的生产级 DDD 框架，集成 Outbox 模式、Spring Boot 自动装配、ArchUnit 架构规则。

## 名称含义

`jfoundry` 可以理解为 Java / jMolecules 生态中的 “foundry”。foundry 原意是铸造厂或工坊，本项目希望提供一组可组合、可验证、可落地的 DDD 基础构件：领域模型、架构规则、持久化适配器、消息外部化和 Spring Boot 集成。它不是业务框架模板，而是帮助团队把领域建模和工程约束稳定落地的基础设施。

## 特性

- **DDD 分层语义**：基于 jmolecules 的 `@DomainLayer` / `@ApplicationLayer` / `@InfrastructureLayer` / `@InterfaceLayer` 注解，配套 ArchUnit 规则强制依赖方向
- **聚合根 / 值对象**：提供 `ValueObject` 标记接口，强制不可变 + `equals/hashCode` 契约
- **事务性发件箱 (Outbox)**：5 状态机（`PENDING` → `DISPATCHING` → `PUBLISHED` / `FAILED` / `DEAD_LETTERED`），原子化 `claimDispatchable` 避免多实例重复派发
- **多 ORM 抽象**：核心 SPI 与具体实现解耦，当前提供 MyBatis-Plus 实现，未来可扩展 JPA / Mongo
- **多数据库支持**：MySQL（`MEDIUMTEXT`）、达梦 DM（`CLOB`），通过 Flyway 自动迁移
- **Spring Boot 自动装配**：业务侧引入 starter 即可获得完整链路，零样板代码
- **JobRunr 集成**（可选）：基于 JobRunr 的分布式 Outbox 派发
- **ArchUnit 规则库**：开箱即用的架构守护，覆盖持久化层无 `@Transactional`、自动配置零 `@Component`、值对象不可变等约束

## 模块清单

```
jfoundry-parent
├── jfoundry-dependencies                         BOM（依赖版本集中管理）
├── jfoundry-domain                               领域层（实体 / 值对象 / 事件 / 仓储接口，零 Spring 依赖）
├── jfoundry-architecture-layered                 分层语义注解（零 Spring 依赖，基于 jmolecules）
├── jfoundry-infrastructure                       基础设施层聚合
│   ├── jfoundry-persistence-core                 持久化抽象（AbstractPersistenceRepository）
│   ├── jfoundry-persistence-mybatis-plus         MyBatis-Plus 实现
│   ├── jfoundry-messaging-core                   消息发送 / 事件外部化 SPI + PayloadSerializer
│   ├── jfoundry-messaging-spring                 Spring 领域事件发布 + 默认 MessageSender
│   ├── jfoundry-outbox-core                      Transactional Outbox SPI + 状态机
│   ├── jfoundry-outbox-mybatis-plus              Outbox 的 MyBatis-Plus 存储适配器
│   ├── jfoundry-outbox-spring                    Outbox 的 Spring 外部化与 scheduled 派发器
│   └── jfoundry-outbox-jobrunr                   Outbox 的 JobRunr 派发器（可选）
├── jfoundry-spring                               Spring 整合层聚合
│   ├── jfoundry-autoconfigure                    Spring Boot AutoConfiguration
│   └── jfoundry-spring-boot-starter              面向业务侧的 Spring Boot 聚合 starter
└── jfoundry-test                                 ArchUnit 规则库（业务侧测试直接引用）
```

## 快速开始

### 1. 引入依赖

业务侧只需要引入 `jfoundry-spring-boot-starter`，它会聚合所需的默认运行时模块。MyBatis-Plus、Outbox 存储等实现细节由 starter 内部选择：

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
    ArchRule[] jfoundryRules = JFoundryRules.all();

    @ArchTest
    ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
}
```

### 4. 可选：配置领域事件外部化（Outbox）

领域事件不强制使用 Outbox。默认 `DomainEventPublisher` 会在事务提交后通过 Spring `ApplicationEventPublisher` 发布本地事件；如果业务只需要进程内监听器，可不配置 Outbox。

当事件需要可靠外部化、跨进程投递或失败重试时，再为事件标记 `@Externalized` / `@MessageRouting`，并启用 Outbox 存储与派发。业务侧只要提供 `OutboxRepository` Bean（或让 starter 自动装配 MyBatis-Plus 实现），匹配外部化规则的领域事件就会写入 Outbox 表：

```yaml
jfoundry:
  outbox:
    table-name: ddd_outbox_event       # 与 Flyway 迁移脚本一致
    dispatcher:
      enabled: true
      mode: scheduled                  # 或 jobrunr（需额外引入 jfoundry-outbox-jobrunr）
    cleanup:
      published-retention-days: 7      # PUBLISHED 状态保留 7 天后清理
      dead-lettered-retention-days: 30 # DEAD_LETTERED 保留 30 天
```

## 文档

- [值对象（Value Object）规范](docs/value-object.md)
- [分层架构注解](docs/layered-architecture.md)
- [ArchUnit 架构规则](docs/archunit-rules.md)
- [Transactional Outbox 事务性发件箱](docs/transactional-outbox.md)

## 技术栈

| 关注点 | 选型 |
|--------|------|
| JDK | 21 |
| Spring Boot | 3.2.7 |
| Spring | 6.1.10 |
| MyBatis-Plus | 3.5.12 |
| jmolecules | 2025.0.2（integrations 0.33.0） |
| ArchUnit | 1.4.2 |
| Jackson | 2.17.1 |
| Flyway | （业务侧提供，本仓库仅提供迁移脚本） |
| JobRunr | 6.3.5（可选） |

## 构建

```bash
# 完整构建（含测试）
mvn clean install

# 跳过测试
mvn clean install -DskipTests

# 仅验证 Maven 模块结构
mvn validate
```

> **JVM 参数提示**：Specification 规约模式需要通过反射从方法引用中提取字段名，构建时需要 `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED`。`jfoundry-autoconfigure` 等模块的 surefire 插件已配置；业务侧如需可参考 `.mvn/jvm.config`。

## License

[Apache License 2.0](LICENSE)
