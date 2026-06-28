# Persistence Raw ID Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让持久化数据对象使用原始 ID 类型，同时领域聚合根继续使用强类型 `Identifier`。

**Architecture:** `AggregateData` 的泛型代表持久化 ID；`DataConverter` 和仓储基类拆分领域 ID 与持久化 ID。Repository 对外仍暴露领域 ID，对内通过 converter 映射成持久化 ID。

**Tech Stack:** Java 21, Maven, jMolecules, MyBatis-Plus, JUnit 5, Spring Boot Test.

---

### Task 1: 用测试表达原始持久化 ID

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-persistence-core/src/test/java/org/jfoundry/infrastructure/persistence/DataConverterTest.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-core/src/test/java/org/jfoundry/infrastructure/persistence/AggregateDataIdentityTest.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/src/test/java/org/jfoundry/infrastructure/persistence/mybatis/support/TestOrderData.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/src/test/java/org/jfoundry/infrastructure/persistence/mybatis/support/TestOrderDataConverter.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/src/test/java/org/jfoundry/infrastructure/persistence/mybatis/PersistenceTestConfig.java`

- [ ] **Step 1: Write failing tests**

把测试数据对象改成 `AggregateData<String>`，converter 增加 `toDataId(TestOrderId)`，MyBatis 测试配置删除 `TestOrderIdTypeHandler` 注册。

- [ ] **Step 2: Run focused tests to verify RED**

Run: `mvn -pl jfoundry-infrastructure/jfoundry-persistence-core,jfoundry-infrastructure/jfoundry-persistence-mybatis-plus -am test`

Expected: compilation failure because framework generics still require `DATA_ID` to be the same `Identifier` type.

### Task 2: 拆分核心持久化泛型

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-persistence-core/src/main/java/org/jfoundry/infrastructure/persistence/AggregateData.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-core/src/main/java/org/jfoundry/infrastructure/persistence/DataConverter.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-core/src/main/java/org/jfoundry/infrastructure/persistence/AbstractPersistenceRepository.java`

- [ ] **Step 1: Implement minimal API change**

`AggregateData<ID extends Serializable>`；`DataConverter<T, DOMAIN_ID, D, DATA_ID>`；`AbstractPersistenceRepository<T, DOMAIN_ID, D, DATA_ID>`。

- [ ] **Step 2: Route find/remove through converter**

`findById(DOMAIN_ID id)` and `remove(T entity)` call `converter.toDataId(id)` before invoking persistence template methods.

- [ ] **Step 3: Run core tests**

Run: `mvn -pl jfoundry-infrastructure/jfoundry-persistence-core -am test`

Expected: PASS.

### Task 3: 更新 MyBatis-Plus 适配

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/src/main/java/org/jfoundry/infrastructure/persistence/mybatis/MybatisPlusRepository.java`
- Modify: `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/src/main/java/org/jfoundry/infrastructure/persistence/mybatis/MybatisPlusAuditableData.java`

- [ ] **Step 1: Update repository generics**

`MybatisPlusRepository<T, DOMAIN_ID, D, DATA_ID>` extends the updated core repository and calls `mapper.deleteById(DATA_ID)` / `mapper.selectById(DATA_ID)`.

- [ ] **Step 2: Update auditable data bound**

`MybatisPlusAuditableData<ID extends Serializable>` extends `AggregateData<ID>`.

- [ ] **Step 3: Run MyBatis focused tests**

Run: `mvn -pl jfoundry-infrastructure/jfoundry-persistence-mybatis-plus -am test`

Expected: PASS without registering a custom ID TypeHandler.

### Task 4: 全量校验并提交

**Files:**
- All changed files from previous tasks.

- [ ] **Step 1: Search for old signatures**

Run: `rg "DataConverter<[^>]+,[^>]+,[^>]+>|AggregateData<.*Identifier|D extends AggregateData<DOMAIN_ID|D extends AggregateData<ID>"`.

Expected: no stale production signatures.

- [ ] **Step 2: Run Maven validation**

Run: `mvn validate`

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```bash
git add docs/superpowers/specs/2026-06-29-persistence-raw-id-design.md docs/superpowers/plans/2026-06-29-persistence-raw-id.md jfoundry-infrastructure/jfoundry-persistence-core jfoundry-infrastructure/jfoundry-persistence-mybatis-plus
git commit -m "refactor(persistence): split domain and data ids"
```
