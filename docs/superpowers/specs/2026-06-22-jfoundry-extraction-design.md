---
title: jfoundry 框架抽取与重构设计
date: 2026-06-22
status: draft
author: Huang Xiao
related_to: devcloud-ci-service/ddd-framework
---

# jfoundry 框架抽取与重构设计

## 1. 执行摘要

把 `devcloud-ci-service/ddd-framework` 模块抽取为独立项目 **jfoundry**，并借此次抽取一并解决现有框架的契约缺陷、生产缺口和 DDD 概念完整性问题。

- **身份重命名**：Maven `groupId` 从 `com.mysoft.framework.ddd` 改为 `org.jfoundry`，Java 包名整体改为 `org.jfoundry.*`
- **修复 4 个契约缺陷（P1）**：框架对外契约与实际行为不一致的问题
- **补齐 5 项生产能力（P2）**：并发安全、配置硬编码、容量上限、数据增长
- **完成 DDD 概念集成（P3）**：补齐 jmolecules 生态里框架尚未用起来的部分（ValueObject、分层 stereotype、jmolecules-archunit/jackson 集成）
- **分四个阶段实施**：Phase 0 建仓改名 → Phase 1 P1 → Phase 2 P2 → Phase 3 P3，每阶段独立可验证

框架定位：**jfoundry = jmolecules 类型系统 + jmolecules 分层 stereotype + jmolecules 生态集成 + 生产级 DDD 基础设施（MyBatis-Plus 持久化、Outbox、Spring Boot 3 autoconfig）**。

---

## 2. 背景与动机

### 2.1 现状

`ddd-framework` 模块当前嵌在 `devcloud-ci-service` 仓库中，被该服务的 `ci-*` 模块直接依赖。经过两轮独立审查（Claude + GPT 5.5），识别出三类问题：

| 类别 | 数量 | 严重度 |
|---|---|---|
| 契约缺陷（文档/注解与实现不符） | 4 项 | 高 |
| 生产能力缺口 | 5 项 | 中 |
| DDD 概念缺失（jmolecules 生态未用足） | 3 项 | 中 |

### 2.2 为什么抽取为独立项目

- **跨项目复用**：jfoundry 的定位是通用 DDD 开发框架，不应绑定到 ci-service 一个业务
- **发布独立版本**：独立仓库可以独立打版本、独立 release，不受业务仓库迭代节奏约束
- **清理历史包袱**：抽取时一并解决 P1/P2/P3，避免"带病搬家"
- **范围明确**：业务模块（ci-*）的迁移是独立工作，本设计不涉及

### 2.3 为什么基于 jmolecules

jmolecules 提供 DDD vocabulary 和分层 stereotype 的纯 Java 标记，jfoundry 在其上补齐生产级基础设施。当前框架**已经**基于 jmolecules（`BaseAggregateRoot` 等均 extends/implements jmolecules 类型），本次抽取将进一步深化集成，把 jmolecules 生态的 archunit/jackson 等现成能力用起来，不自研重复轮子。

---

## 3. 目标与非目标

### 3.1 目标

1. **抽取身份**：建立 `org.jfoundry` 独立 Maven 坐标，业务代码只 import `org.jfoundry.*`，jmolecules 始终作为框架内部实现细节
2. **修复 P1 契约缺陷**：4 项（详见第 6 节）
3. **补齐 P2 生产能力**：5 项（详见第 7 节）
4. **完成 P3 DDD 概念集成**：3 项（详见第 8 节）
5. **分阶段可验证**：每个 Phase 完成有明确的验收标准，可独立停止/回退

### 3.2 非目标

| 非目标 | 原因 |
|---|---|
| ci-* 业务模块迁移到 jfoundry | 用户明确决定走独立分支，不在本设计范围 |
| jmolecules-cqrs-architecture 引入 | 当前无业务需求，jmolecules 2.x 已将其废弃 |
| jmolecules-hexagonal-architecture 引入 | 当前无业务需求 |
| 框架自研 CQRS 抽象 / ApplicationService 基类 / DTO 基类 | YAGNI，jmolecules 类型 + 业务自由组合即可 |
| jmolecules-starter-ddd / jmolecules-jpa / jmolecules-spring 引入 | 与 jfoundry 的 autoconfig 职责重叠或非 MyBatis-Plus 技术栈 |
| Git 历史保留 | 新仓库 `git init`，源仓库历史保留在 devcloud-ci-service |

---

## 4. 身份与位置

### 4.1 项目身份

| 维度 | 值 |
|---|---|
| 项目名 | jfoundry |
| 目标路径 | `/Users/huangxiao/Workspace/mine/jfoundry/` |
| Maven groupId | `org.jfoundry` |
| Maven 父 artifactId | `jfoundry-parent` |
| Java 根包 | `org.jfoundry` |
| 版本 | `1.0.0-SNAPSHOT`（保留，代码已生产验证） |
| Git 历史 | 不保留（新仓库 `git init`） |

### 4.2 命名映射

| 维度 | 旧 | 新 |
|---|---|---|
| Maven groupId | `com.mysoft.framework.ddd` | `org.jfoundry` |
| Maven artifactId 前缀 | `ddd-*` | `jfoundry-*` |
| 父 POM artifactId | `ddd-framework` | `jfoundry-parent` |
| BOM artifactId | `ddd-dependencies` | `jfoundry-dependencies` |
| Java 根包 | `com.mysoft.framework.ddd` | `org.jfoundry` |
| 配置属性前缀 | `ddd.*` | `jfoundry.*` |

---

## 5. 架构与模块结构

### 5.1 目标模块布局

```
jfoundry/
├── pom.xml                                      # org.jfoundry:jfoundry-parent
├── jfoundry-dependencies/                       # BOM
├── jfoundry-domain/                             # 领域层（纯 SPI）
│   └── src/main/java/org/jfoundry/domain/
│       ├── entity/                              # AggregateRoot/Entity 包装
│       ├── event/                               # DomainEvent 包装
│       ├── repository/                          # Repository 包装
│       ├── specification/                       # Specification 模式
│       └── valueobject/                         # 【新】ValueObject 包装
│           └── ValueObject.java
├── jfoundry-architecture-layered/               # 【新】四层 stereotype（package-level）
│   └── src/main/java/org/jfoundry/architecture/layered/
│       ├── ApplicationLayer.java
│       ├── DomainLayer.java
│       ├── InterfaceLayer.java
│       └── InfrastructureLayer.java
├── jfoundry-infrastructure/                     # 基础设施层
│   ├── jfoundry-persistence-core/
│   ├── jfoundry-persistence-mybatis-plus/
│   ├── jfoundry-messaging-core/
│   ├── jfoundry-messaging-mybatis-plus/
│   ├── jfoundry-messaging-spring/
│   └── jfoundry-messaging-jobrunr/
├── jfoundry-spring/                             # Spring 集成
│   ├── jfoundry-spring-autoconfigure/
│   └── jfoundry-spring-mybatis-plus-starter/
├── jfoundry-test/                               # ArchUnit 规则集中于此
│   └── src/main/java/org/jfoundry/test/archunit/
│       ├── JFoundryRules.java
│       ├── ValueObjectRules.java
│       ├── LayeredRules.java
│       └── PersistenceRules.java
└── docs/
    ├── superpowers/specs/                       # 本设计文档所在地
    ├── release-gate.md
    ├── repository.md
    ├── events.md
    ├── valueobject.md                           # 【新】
    └── layered-architecture.md                  # 【新】
```

### 5.2 模块依赖图

```
┌─────────────────────────────────────────────────────────────┐
│ 业务 (com.mysoft.*)                                          │
└──────────────┬──────────────────────────────────────────────┘
               │ 只依赖这些
               ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│ jfoundry-domain          │   │ jfoundry-architecture-layered│
│ - AggregateRoot 包装     │   │ - @ApplicationLayer 等 4 个   │
│ - Entity 包装            │   │ - 纯 Java，零 Spring          │
│ - ValueObject 包装【新】 │   │ - 依赖 jmolecules-layered     │
│ - Repository/Event       │   └──────────────────────────────┘
│ - 依赖 jmolecules-ddd    │
└──────────────────────────┘
               ▲
               │
┌──────────────┴───────────┐   ┌──────────────────────────────┐
│ jfoundry-infrastructure  │   │ jfoundry-spring-*            │
│ - 持久化实现              │   │ - autoconfig                 │
│ - Outbox                 │   │ - starter                    │
│ - 依赖 jfoundry-domain   │   │ - 依赖 Spring                │
└──────────────────────────┘   └──────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ jfoundry-test (测试时引用)                                   │
│ - ArchUnit 规则聚合                                         │
│ - 依赖 jfoundry-* + jmolecules-archunit + archunit-junit5  │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 关键架构约束

1. **业务只 import `org.jfoundry.*`**：jmolecules 是框架的实现细节，不泄漏到业务
2. **jfoundry-architecture-layered 零 Spring 依赖**：架构层不绑定运行时
3. **jfoundry-domain 零 Spring 依赖**：领域层是纯 Java SPI
4. **Spring 集成全部在 jfoundry-spring-***：autoconfig 模块统一管理 Spring 桥接
5. **事务边界属于应用层**：持久化层零 `@Transactional`

---

## 6. P1 契约缺陷修复（4 项）

原则：**契约对齐实际**，而不是让实际迁就错误的契约。

### 6.1 P1-1 SpringDomainEventPublisher 未注册到 AutoConfig

**现状证据**：

`jfoundry-messaging-spring/src/main/java/org/jfoundry/infrastructure/messaging/spring/publisher/SpringDomainEventPublisher.java`：

```java
@Component  // 死注解：autoconfig 模块不开 @ComponentScan
public class SpringDomainEventPublisher implements DomainEventPublisher {
    @Autowired
    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher,
                                      List<DomainEventSink> sinks) { ... }
}
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 缺少对应条目。

**后果**：业务依赖 jfoundry 后，`DomainEventPublisher` 注入失败，除非业务自己 `@ComponentScan` 扫到框架包（反模式）。

**修复设计**：

1. 删掉 `SpringDomainEventPublisher` 上的 `@Component`
2. 新增 `DomainEventPublisherAutoConfiguration`：

```java
@AutoConfiguration
@ConditionalOnClass(DomainEventPublisher.class)
@ConditionalOnProperty(prefix = "jfoundry.domain.event", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class DomainEventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public SpringDomainEventPublisher springDomainEventPublisher(
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<List<DomainEventSink>> sinksProvider) {
        return new SpringDomainEventPublisher(
            eventPublisher, sinksProvider.getIfAvailable(List::of));
    }
}
```

3. 加入 `AutoConfiguration.imports`

**验收**：
- 集成测试：空 Spring Boot app + jfoundry classpath → `DomainEventPublisher` bean 存在
- ArchUnit：`jfoundry-spring-autoconfigure` 模块禁止 `@Component`

### 6.2 P1-2 DomainEventExternalizer 条件判断类型错误

**现状证据**：

`DomainEventExternalizerAutoConfiguration.java`：

```java
@Bean
@ConditionalOnBean(OutboxRepository.class)
@ConditionalOnMissingBean(DomainEventSink.class)  // ← 检查错了类型
public DomainEventExternalizer domainEventExternalizer(...) { ... }
```

**后果**：业务自定义任何 `DomainEventSink`（如日志、metrics）时，外部化器被条件挡住不创建，集成事件链路静默失效。

**修复设计**：

```java
@Bean
@ConditionalOnBean(OutboxRepository.class)
@ConditionalOnMissingBean(DomainEventExternalizer.class)  // ← 检查自己
@Order(Ordered.LOWEST_PRECEDENCE)  // Externalizer 在 Sink 链末端
public DomainEventExternalizer domainEventExternalizer(...) { ... }
```

并在 `DomainEventSink` 接口 javadoc 明确：sink 链按 `@Order` 升序执行，Externalizer 默认末端。

**验收**：
- 集成测试：业务定义自定义 `LoggingDomainEventSink` + `OutboxRepository` 存在 → `DomainEventExternalizer` 仍被创建
- 集成测试：业务定义自定义 `DomainEventExternalizer` → 框架默认实现退让

### 6.3 P1-3 AggregateRepository 事务保证的 javadoc 谎言

**现状证据**：

`AggregateRepository.java` 行 54-66：

```java
/// 批量加入聚合集合(新建)。
/// <p>
/// 事务性保证:整个批量操作在单个事务中执行。如果部分失败,所有操作都会回滚。
void addAll(Collection<T> entities);

/// 批量修改聚合集合中已存在的元素。
/// <p>
/// 事务性保证:整个批量操作在单个事务中执行。
void modifyAll(Collection<T> entities);
```

`AbstractPersistenceRepository` 实现里零 `@Transactional`。

**后果**：业务读文档以为批量是原子，实际"best effort"——部分失败时已执行的不回滚，静默数据不一致。

**修复方向**：选择 B（让契约迁就实际，删除"事务性保证"措辞），理由：

1. DDD 原则："一个事务修改一个聚合根"。批量跨聚合事务是反模式
2. 事务归属：事务是应用层关切，不该下沉到 Repository
3. 避免 surprises：框架级 `@Transactional` 会引发传播行为冲突
4. 应用层用法清晰：

```java
@ApplicationLayer  // 在 package-info.java
package com.mysoft.ci.env.appservice;

@Service
public class EnvAppService {
    @Transactional  // ← 事务边界在这里显式声明
    public void createEnvs(List<EnvCreateCmd> cmds) {
        cmds.forEach(cmd -> envRepository.add(...));
    }
}
```

**修复内容**：

1. 修改 javadoc（删除"事务性保证"，改为批量语义说明）
2. ArchUnit 规则：`org.jfoundry.infrastructure.persistence` 包下零 `@Transactional`

**验收**：
- ArchUnit：persistence 包下零 `@Transactional`
- 文档测试：grep "事务性保证" 在源码零命中

### 6.4 P1-4 OutboxDispatcher 不尊重 enabled=false

**现状证据**：

`OutboxDispatcherAutoConfiguration.java`：

```java
@AutoConfiguration
@EnableScheduling  // ← 强制开启调度
// 缺：@ConditionalOnProperty
public class OutboxDispatcherAutoConfiguration { ... }
```

**后果**：业务配 `jfoundry.outbox.dispatcher.enabled=false` 不生效，dispatcher 照跑。测试环境、多实例重复投递场景都会被这个 bug 放大。

**修复设计**：

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxDispatcherAutoConfiguration { ... }
```

`matchIfMissing = true` 保持默认开启行为（向后兼容）。

**验收**：
- 集成测试 A：空配置 → dispatcher bean 存在
- 集成测试 B：`jfoundry.outbox.dispatcher.enabled=false` → dispatcher bean 不存在

---

## 7. P2 生产能力补齐（5 项）

原则：**不破坏现有 API，通过增量能力补齐生产缺口**。

### 7.1 P2-1 Outbox 多实例并发派发的竞态

**现状证据**：

`MybatisPlusOutboxRepository.java` 的 `findDispatchable` + `markAsPublished` 是非原子的 read-then-update。

**后果**：多实例部署时，两个 pod 同时 `findDispatchable(100)` 拿到相同记录，各自派发一次，重复投递。文档 `events.md` 自己也承认"多实例会重复投递，依赖消费端幂等"。

**修复设计**：DISPATCHING 中间态 + SQL 原子 claim + 超时恢复任务

**状态机**：

```
            claim (atomic UPDATE)
PENDING ─────────────────────────► DISPATCHING ───success───► PUBLISHED
  ▲                                    │
  │                                    ├──failure──► FAILED ──maxRetries──► DEAD_LETTERED
  │                                    │                │
  │                                    │                └──reactivate──► PENDING
  │                                    │
  └────stuck > timeout (recovery)──────┘
```

**Schema 改动**（V1 迁移脚本直接包含，不是增量 ALTER）：

```sql
status VARCHAR(20) NOT NULL,                  -- PENDING/DISPATCHING/PUBLISHED/FAILED/DEAD_LETTERED
claimed_at DATETIME(3) NULL,
claimed_by VARCHAR(100) NULL,                 -- pod 标识 (hostname + 短 UUID)
INDEX idx_outbox_claim (status, claimed_at)
```

**Repository API**：

```java
public interface OutboxRepository {
    /// 原子声明一批待派发事件。
    List<OutboxEntry> claimDispatchable(int limit, String claimerId);
    
    void markAsPublished(Long id);
    void markAsFailed(Long id, String reason);
    
    /// 恢复卡住的 DISPATCHING 记录。
    int recoverStuckDispatching(Duration timeout);
}
```

**claim SQL（MySQL 版）**：

```sql
UPDATE jfoundry_outbox_event
SET status = 'DISPATCHING',
    claimed_at = NOW(3),
    claimed_by = #{claimerId}
WHERE id IN (
    SELECT id FROM (
        SELECT id FROM jfoundry_outbox_event
        WHERE status = 'PENDING'
        ORDER BY id
        LIMIT #{limit}
    ) AS t
);

SELECT * FROM jfoundry_outbox_event
WHERE claimed_by = #{claimerId} AND status = 'DISPATCHING';
```

**方言处理**：达梦不支持 `UPDATE ... LIMIT subquery`，需用 ROWNUM 或 top-N 子查询改写。按 P2-3 的 dialect 分派。

**恢复任务**：

```java
@Scheduled(fixedDelayString = "${jfoundry.outbox.recovery.interval:60s}")
public void recoverStuckDispatching() {
    Duration timeout = properties.getRecovery().getStuckTimeout();  // 默认 5min
    int recovered = outboxRepository.recoverStuckDispatching(timeout);
    if (recovered > 0) {
        log.warn("Recovered {} stuck DISPATCHING outbox records", recovered);
    }
}
```

**验收**：
- 并发集成测试：2 线程并发 `claimDispatchable(10)`，两批记录 id 不相交，总量 = 20
- 超时恢复测试：手动插入 DISPATCHING + `claimed_at=10min 前`，跑 `recoverStuckDispatching`，断言改回 PENDING

### 7.2 P2-2 OutboxData 表名硬编码

**现状证据**：`OutboxData.java` 硬编码 `@TableName("ddd_outbox_event")`。

**后果**：业务想自定义表名（已有 Outbox 表、或命名规范不同）时，必须改框架源码。

**修复设计**：

1. 删除 `OutboxData` 上的 `@TableName`
2. 配置属性 `jfoundry.outbox.table-name`（默认 `ddd_outbox_event`，向后兼容）
3. 在 `OutboxMybatisPlusAutoConfiguration` 注册 `TableNameHandler`：

```java
@Bean
static TableNameHandler outboxTableNameHandler(JfoundryOutboxProperties props) {
    String configuredName = props.getTableName();
    return (tableName, sql) -> 
        "ddd_outbox_event".equals(tableName) ? configuredName : tableName;
}
```

**验收**：
- 集成测试：设 `table-name=custom_outbox`，操作后 `custom_outbox` 表有数据
- 集成测试：不设 `table-name`，默认 `ddd_outbox_event` 仍工作

### 7.3 P2-3 PaginationInnerInterceptor 方言缺失

**现状证据**：

```java
interceptor.addInnerInterceptor(new PaginationInnerInterceptor());  // 无 DbType
```

**后果**：无参构造依赖运行时自动识别，在 DataSource 代理（HikariCP wrapper）下识别失败；达梦数据库识别不准，分页 SQL 翻译错误。

**修复设计**：

1. 配置属性 `jfoundry.persistence.db-type`（可选）
2. 自动识别兜底：从 `DataSource` connection metadata 推断 `DbType`
3. 显式优先：属性覆盖自动识别

```java
private DbType resolveDbType(JfoundryPersistenceProperties props, DataSource dataSource) {
    if (props.getDbType() != null) {
        return props.getDbType();
    }
    return DbTypeResolver.autoDetect(dataSource);
}
```

**验收**：
- 集成测试：H2 内存库自动识别为 H2
- 集成测试：显式配 `db-type=DM`，拦截器使用 DM 方言

### 7.4 P2-4 payload_json 容量上限

**现状证据**：V1 迁移脚本用 `payload_json TEXT NOT NULL`。

**后果**：MySQL `TEXT` 上限 64KB，集成事件 payload 超 64KB 时写入失败。

**修复设计**：jfoundry 是新仓库，V1 迁移脚本直接用合适类型，按方言分派：

```sql
-- V20260617__create_outbox_event_mysql.sql
payload_json MEDIUMTEXT NOT NULL,    -- 16MB

-- V20260617__create_outbox_event_dm.sql
payload_json CLOB NOT NULL,          -- 2GB
```

**验收**：
- 集成测试：INSERT 1MB payload 成功
- 集成测试：INSERT 20MB（超过 MEDIUMTEXT）失败并抛出明确异常

### 7.5 P2-5 Outbox 清理任务

**现状证据**：框架只负责写 Outbox，不负责清，表无限增长。

**修复设计**：新增 `OutboxCleanupJob`，定期删除终态记录。

**配置属性**：

```yaml
jfoundry:
  outbox:
    cleanup:
      enabled: true                              # 默认开启
      cron: "0 0 2 * * ?"                        # 默认每天凌晨 2 点
      published-retention-days: 7                # PUBLISHED 保留 7 天
      dead-lettered-retention-days: 30           # DEAD_LETTERED 保留 30 天
      batch-size: 1000                           # 单批删除上限
```

**实现**：Spring `@Scheduled`（不用 JobRunr，清理是幂等的，不需要事务性重试）

**分批删除**：

```java
default int deleteByStatusAndCreatedAtBefore(Status status, Instant cutoff, int batchSize) {
    int total = 0;
    int deleted;
    do {
        deleted = mapper.deleteBatchByStatusAndCreatedBefore(status, cutoff, batchSize);
        total += deleted;
    } while (deleted == batchSize);
    return total;
}
```

**验收**：
- 集成测试：INSERT 10 条 PUBLISHED（8 天前）+ 10 条（1 天前），跑 cleanup，断言只删 10 条
- 集成测试：`enabled=false` → job 不被注册

---

## 8. P3 DDD 概念补齐（3 项）

jmolecules 已提供基础类型和 stereotype，但框架尚未完全用起来。P3 的目标是补齐这些集成，不重复发明概念。

### 8.1 P3-1 ValueObject 包装接口

**现状**：jmolecules `@ValueObject` / `ValueObject` 类型已在 `jmolecules-ddd` 依赖里可用，但业务直接 import 会泄漏 jmolecules 依赖。框架零 ArchUnit 规则保护 ValueObject 语义。

**修复设计**：

新增框架包装接口，对齐其他概念的包装模式：

```java
package org.jfoundry.domain.valueobject;

/// 领域层值对象标记接口。
/// <p>
/// 业务值对象实现此接口即获得：
/// - jmolecules ValueObject 类型语义（可被 jmolecules 生态工具识别）
/// - 框架 ArchUnit 规则保护（强制不可变、equals/hashCode）
/// <p>
/// 推荐使用 Java 21 record 作为实现载体。
public interface ValueObject extends org.jmolecules.ddd.types.ValueObject {
}
```

业务侧用法：

```java
package com.mysoft.ci.env.valueobject;

import org.jfoundry.domain.valueobject.ValueObject;

public record Money(BigDecimal amount, Currency currency) implements ValueObject {
    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }
}
```

**ArchUnit 规则**（`ValueObjectRules.java`）：

```java
public static final ArchRule value_objects_must_be_final =
    classes().that().implement(ValueObject.class)
             .should().beRecords()
             .orShould().haveModifier(JavaModifier.FINAL);

public static final ArchRule value_object_fields_must_be_final =
    classes().that().implement(ValueObject.class)
             .should().haveOnlyFinalFields();

public static final ArchRule value_objects_must_implement_equals_and_hashCode =
    classes().that().implement(ValueObject.class)
             .should().implementEqualsAndHashCode();
```

### 8.2 P3-2 四层 Stereotype（jfoundry-architecture-layered 模块）

**现状**：jmolecules-layered-architecture 提供 `@ApplicationLayer / @DomainLayer / @InterfaceLayer / @InfrastructureLayer`，框架完全没用，靠 Maven 模块隐式分层，无 ArchUnit 约束。

**修复设计**：

新增 `jfoundry-architecture-layered` 模块（纯 Java，依赖 jmolecules-layered-architecture，零 Spring 依赖）：

```java
package org.jfoundry.architecture.layered;

import java.lang.annotation.*;
import org.jmolecules.architecture.layered.ApplicationLayer;

/// 应用层模块标记。
/// <p>
/// 标注在 package-info.java 上，声明整个 package 属于应用层。
@ApplicationLayer  // jmolecules 内部 meta
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLayer {
}
```

其他三个（`DomainLayer / InterfaceLayer / InfrastructureLayer`）结构相同。

**业务用法**：

```java
// src/main/java/com/mysoft/ci/env/appservice/package-info.java
@ApplicationLayer
package com.mysoft.ci.env.appservice;

import org.jfoundry.architecture.layered.ApplicationLayer;
```

```java
// src/main/java/com/mysoft/ci/env/appservice/EnvAppService.java
package com.mysoft.ci.env.appservice;

@Service  // Spring 运行时关切，正交于 layer
public class EnvAppService { ... }
```

**关键设计决策**：
- 分层是 package-level 关切，不是 class-level（jmolecules 哲学）
- Bean 身份是 class-level 关切，由 Spring `@Service` 表达，正交于 layer
- 框架不提供 `@ApplicationService` 复合注解（混了两个正交关切）

**ArchUnit 规则**（`LayeredRules.java`）：

```java
public static final ArchRule application_must_not_depend_on_infrastructure =
    classes().that().resideInAPackageAnnotatedWith(ApplicationLayer.class)
             .should().onlyDependOnClassesThat().resideInAnyPackageAnnotatedWith(
                 ApplicationLayer.class, DomainLayer.class);

public static final ArchRule only_application_can_use_repository_directly =
    classes().that().resideInAPackageAnnotatedWith(ApplicationLayer.class)
             .should().dependOnClassesThat().areAssignableTo(Repository.class);
```

### 8.3 P3-3 jmolecules 生态集成

**引入依赖**：

- `org.jmolecules.integrations:jmolecules-archunit` —— 拿现成的 DDD + 架构规则
- `org.jmolecules.integrations:jmolecules-jackson` —— ValueObject / Identifier 序列化

**不引入**：
- `jmolecules-spring`（功能是 Jackson 转换 + JPA AttributeConverter，与 jmolecules-jackson 重叠，且我们用 MyBatis-Plus）
- `jmolecules-starter-ddd`（自带 autoconfig 与 jfoundry 冲突）
- `jmolecules-jpa`（技术栈不符）

**集成方式**：

`jfoundry-test/JFoundryRules.java` 聚合入口：

```java
public final class JFoundryRules {

    /// 框架自有的所有 ArchUnit 规则聚合
    public static ArchRule[] all() {
        return Stream.of(
            ValueObjectRules.class.getFields(),
            LayeredRules.class.getFields(),
            PersistenceRules.class.getFields()
        ).flatMap(Arrays::stream)
         .map(f -> ((ArchRule) f.get(null)))
         .toArray(ArchRule[]::new);
    }

    /// jmolecules 官方规则（DDD + 架构）
    public static final ArchRule[] jmoleculesNative() {
        return new ArchRule[] {
            JMoleculesRules.ddd().domainEventsMustBeImplementedInAggregate(),
            JMoleculesRules.ddd().aggregatesMustOnlyReferenceOtherAggregatesById(),
            JMoleculesRules.layeredArchitecture(),
        };
    }
}
```

业务侧用法：

```java
@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {
    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.all();
    
    @ArchTest
    ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
}
```

`jfoundry-spring-autoconfigure/JfoundryJacksonAutoConfiguration`：

```java
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnBean(ObjectMapper.class)
public class JfoundryJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Module.class)
    public Module jmoleculesJacksonModule() {
        return new JMoleculesModule();
    }
}
```

---

## 9. 完整 ArchUnit 规则清单

jfoundry-test 提供的所有规则，业务一行 `@ArchTest` 全启用：

| 规则 | 来源 | 作用 |
|---|---|---|
| `value_objects_must_be_final` | 框架自有 | ValueObject 实现类必须 final 或 record |
| `value_object_fields_must_be_final` | 框架自有 | ValueObject 字段全部 final |
| `value_objects_must_implement_equals_and_hashCode` | 框架自有 | ValueObject 必须重写 equals/hashCode |
| `interface_must_not_depend_on_infrastructure` | 框架自有 | Controller 不能直接依赖基础设施 |
| `application_must_not_depend_on_infrastructure` | 框架自有 | Application 必须通过端口访问基础设施 |
| `persistence_repository_must_not_use_transactional` | 框架自有（P1-3） | 持久化层零 `@Transactional` |
| `autoconfig_must_not_use_component` | 框架自有（P1-1） | autoconfig 模块零 `@Component` |
| `jmolecules.ddd().*` | jmolecules-archunit | jmolecules 现成的 DDD 规则集 |
| `jmolecules.layeredArchitecture()` | jmolecules-archunit | jmolecules 现成的分层规则 |

---

## 10. 实施阶段拆分

### 10.1 Phase 0 — 建仓 + rename（预计 1-2 天）

| # | commit | 内容 |
|---|---|---|
| 0.1 | `chore: init jfoundry project skeleton` | 创建目录 + git init + 空 Maven 结构 |
| 0.2 | `chore: copy source from devcloud-ci-service/ddd-framework` | 拷贝所有源码（保留原包名） |
| 0.3 | `refactor: rename Maven coordinates to org.jfoundry` | 父 POM + 所有子模块 pom.xml |
| 0.4 | `refactor: rename Java packages com.mysoft.framework.ddd → org.jfoundry` | 所有 `.java` 文件的 package + import 批量替换 |
| 0.5 | `refactor: rename module directories ddd-* → jfoundry-*` | 目录重命名 + 父 pom `<modules>` 更新 |
| 0.6 | `refactor: rename config property prefix ddd.* → jfoundry.*` | `@ConfigurationProperties` + `@ConditionalOnProperty` 前缀 |
| 0.7 | `refactor: update AutoConfiguration.imports class names` | `META-INF/spring/...AutoConfiguration.imports` FQN 同步 |
| 0.8 | `refactor: update Flyway migration scripts` | 迁移脚本命名 + 表名前缀 |
| 0.9 | `chore: verify build and tests green` | `mvn clean install` 全绿 |

### 10.2 Phase 1 — P1 契约修复（预计 1 天）

| # | commit | 内容 |
|---|---|---|
| 1.1 | `fix(autoconfig): register DomainEventPublisher via autoconfig` | 删 `@Component`，新增 `DomainEventPublisherAutoConfiguration` |
| 1.2 | `fix(autoconfig): correct DomainEventExternalizer condition type` | 条件类型修正 + `@Order` |
| 1.3 | `docs(repository): correct addAll/modifyAll javadoc on transaction semantics` | 删除"事务性保证"措辞 |
| 1.4 | `test(archunit): forbid @Transactional in persistence layer` | 新增 ArchUnit 规则 |
| 1.5 | `fix(autoconfig): honor jfoundry.outbox.dispatcher.enabled=false` | 加 `@ConditionalOnProperty` |

### 10.3 Phase 2 — P2 生产增强（预计 2-3 天）

| # | commit | 内容 |
|---|---|---|
| 2.1 | `feat(outbox): add DISPATCHING state for atomic claim` | 状态枚举 + schema 字段 |
| 2.2 | `feat(outbox): implement claimDispatchable with conditional UPDATE` | Repository 实现 |
| 2.3 | `feat(outbox): add stuck-dispatching recovery job` | `@Scheduled` 恢复任务 |
| 2.4 | `feat(outbox): support dynamic table name via TableNameHandler` | 删硬编码 + handler |
| 2.5 | `feat(persistence): explicit DbType for PaginationInnerInterceptor` | 自动识别 + 显式覆盖 |
| 2.6 | `feat(migration): use MEDIUMTEXT/CLOB for payload_json` | V1 脚本按方言分派 |
| 2.7 | `feat(outbox): add cleanup job for terminal-state records` | `OutboxCleanupJob` |

### 10.4 Phase 3 — P3 DDD 补齐（预计 1-2 天）

| # | commit | 内容 |
|---|---|---|
| 3.1 | `feat(domain): add ValueObject wrapper interface` | `org.jfoundry.domain.valueobject.ValueObject` |
| 3.2 | `feat(archunit): add ValueObject immutability rules` | `ValueObjectRules` |
| 3.3 | `feat(architecture): add jfoundry-architecture-layered module` | 新模块 + 4 个 stereotype |
| 3.4 | `feat(archunit): add layered dependency direction rules` | `LayeredRules` |
| 3.5 | `feat(test): integrate jmolecules-archunit native rules` | 引入依赖 + `JFoundryRules.jmoleculesNative()` |
| 3.6 | `feat(spring): integrate jmolecules-jackson module` | `JfoundryJacksonAutoConfiguration` |
| 3.7 | `docs: add valueobject.md and layered-architecture.md` | 业务使用指南 |

**总计**：约 28 个 commit，5-8 天工作量。

---

## 11. 风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| 大批量 rename 导致编译失败 | 高 | 中 | IDE 重构 + 每 commit 后 `mvn compile`；0.4 拆成多个小 commit（按模块） |
| jmolecules 版本不兼容 | 中 | 高 | Phase 0 验证 2.0.1 与现有代码无冲突；锁定 BOM 版本到 `jfoundry-dependencies` |
| MySQL/达梦 claim SQL 方言差异 | 中 | 高 | 用 `databaseId` 分派 SQL；测试覆盖两种方言（H2 模拟 MySQL 行为） |
| ArchUnit 规则误伤框架自身测试 | 中 | 低 | 规则带豁免列表；先跑现有代码确认全绿 |
| jmolecules-jackson 与业务 Jackson 配置冲突 | 低 | 中 | `@ConditionalOnMissingBean` 保护；提供 `jfoundry.jackson.enabled=false` 关闭 |
| 多实例 DISPATCHING 超时阈值难定 | 中 | 中 | 默认 5min 保守值，`jfoundry.outbox.recovery.stuck-timeout` 可配 |

---

## 12. 验收标准

### 12.1 Phase 0

- `mvn clean install -DskipTests` 全模块成功
- `mvn test` 全绿
- `grep -r "com.mysoft.framework.ddd" src/` 零命中（Java 包名）
- `grep -r "ddd\." src/main/resources/` 零命中（配置前缀）
- 父 pom `<modules>` 子项全部 `jfoundry-*`，无残留 `ddd-*` 目录

### 12.2 Phase 1

- 每个 P1 修复对应至少一个集成测试，全绿
- `JFoundryRules.all()` 全部通过（在 jfoundry 自己的测试代码上跑）

### 12.3 Phase 2

- P2-1：2 线程并发 `claimDispatchable` 集成测试通过（零交集）
- P2-2/P2-3：自定义表名 + db-type 配置的集成测试通过
- P2-4：1MB payload 写入测试通过
- P2-5：cleanup job 触发测试通过

### 12.4 Phase 3

- 新模块 `jfoundry-architecture-layered` 编译零 Spring 依赖（`mvn dependency:tree` 无 spring-context）
- 业务示例：在 `package-info.java` 标 `@ApplicationLayer` + 写一个 ValueObject，ArchUnit 规则全通过

---

## 13. 属性前缀迁移汇总

所有配置属性统一从 `ddd.*` 改为 `jfoundry.*`：

```
jfoundry.domain.event.enabled                              # P1-1
jfoundry.outbox.table-name                                 # P2-2
jfoundry.persistence.db-type                               # P2-3
jfoundry.outbox.dispatcher.enabled                         # P1-4
jfoundry.outbox.recovery.interval                          # P2-1
jfoundry.outbox.recovery.stuck-timeout                     # P2-1
jfoundry.outbox.cleanup.enabled                            # P2-5
jfoundry.outbox.cleanup.cron                               # P2-5
jfoundry.outbox.cleanup.published-retention-days           # P2-5
jfoundry.outbox.cleanup.dead-lettered-retention-days       # P2-5
jfoundry.outbox.cleanup.batch-size                         # P2-5
```

---

## 附录 A：与 jmolecules 的关系总览

| jmolecules 提供能力 | jfoundry 现状 | 本设计后 |
|---|---|---|
| `ddd.types.AggregateRoot / Entity / Repository / Identifier` | ✅ 已 extends/implements | ✅ 保持 |
| `event.types.DomainEvent` | ✅ 已 implements | ✅ 保持 |
| `ddd.types.ValueObject` + `@ValueObject` | ❌ 未用 | ✅ P3-1 包装为 `org.jfoundry.domain.valueobject.ValueObject` |
| `layered.@ApplicationLayer / @DomainLayer / @InterfaceLayer / @InfrastructureLayer` | ❌ 未用 | ✅ P3-2 包装为 `org.jfoundry.architecture.layered.*` |
| `integrations/jmolecules-archunit` | ❌ 未引 | ✅ P3-3 引入并聚合到 `JFoundryRules` |
| `integrations/jmolecules-jackson` | ❌ 未引 | ✅ P3-3 引入并配置 autoconfig |
| `integrations/jmolecules-spring` | ❌ 未引 | ❌ 不引（职责重叠） |
| `integrations/jmolecules-starter-ddd` | ❌ 未引 | ❌ 不引（与 jfoundry autoconfig 冲突） |

---

## 附录 B：术语对照表

| 中文 | 英文 | 含义 |
|---|---|---|
| 聚合根 | AggregateRoot | DDD 中具有全局标识的根实体，事务边界 |
| 实体 | Entity | 有标识的对象，生命周期内标识不变 |
| 值对象 | ValueObject | 无标识的不可变对象，按值相等 |
| 仓储 | Repository | 聚合根的集合抽象，提供持久化访问 |
| 领域事件 | DomainEvent | 领域中已发生的事实，不可变 |
| 集成事件 | Integration Event | 跨限界上下文的事件，通过 Outbox 派发 |
| 端口 | Port | 六边形架构中与外部交互的接口 |
| 适配器 | Adapter | 端口的实现 |
| 出队 | Outbox | 存储待派发集成事件的数据库表模式 |
