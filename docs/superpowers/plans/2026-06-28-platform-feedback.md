# Platform Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `devcloud-ci-service` 平台升级阶段发现的通用兼容性经验反哺到 `jfoundry` 文档中，形成业务项目接入前的平台对齐指南。

**Architecture:** 第一轮只做文档反哺，不修改 Java 源码、starter POM 或自动装配行为。新增 `docs/platform-alignment.md` 承载详细指南，`README.md` 和 `docs/release/compatibility.md` 只提供入口链接，保持现有文档职责清晰。

**Tech Stack:** Markdown, Maven, Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.x, Spring Cloud Alibaba 2025.0.x, MyBatis-Plus 3.5.16, JobRunr 8.7.1.

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `docs/platform-alignment.md` | 面向业务项目的平台对齐指南，包含版本线、BOM 顺序、验证命令和常见迁移问题 | Create |
| `docs/release/compatibility.md` | 版本矩阵和 release gate 文档，增加平台对齐文档入口 | Modify |
| `README.md` | 项目入口文档，文档列表增加平台对齐指南链接 | Modify |
| `docs/superpowers/specs/2026-06-28-platform-feedback-design.md` | 已批准的设计提案 | Inspect only |
| `docs/superpowers/plans/2026-06-28-platform-feedback.md` | 本实施计划 | Create |

## Task 1: Create Platform Alignment Guide

**Files:**
- Create: `docs/platform-alignment.md`

- [ ] **Step 1: Create the guide file**

Create `docs/platform-alignment.md` with this content:

```markdown
# Platform Alignment Guide

This guide helps business applications align their platform dependencies before adopting `jfoundry` 1.x starters.

It is intentionally written as an application-side checklist. It does not prescribe private Maven repositories, Nacos namespaces, service names, database URLs, or company-specific SDKs.

## When To Use This Guide

Use this guide when an existing Spring Boot application wants to adopt `jfoundry` and already depends on some combination of:

- Spring Boot
- Spring Cloud
- Spring Cloud Alibaba / Nacos
- MyBatis-Plus
- JobRunr
- OpenFeign
- private platform BOMs or SDKs

Run this alignment before introducing runtime `jfoundry` starters. It is safe to introduce `jfoundry-dependencies` and `jfoundry-test` during the alignment step.

## Recommended 1.x Baseline

| Area | Recommended Baseline |
|------|----------------------|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Framework | 6.2.19 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| MyBatis-Plus | 3.5.16 |
| JobRunr | 8.7.1 |
| jfoundry | 1.0.0-SNAPSHOT until the first 1.x release is published |

`jfoundry` does not manage Spring Cloud Alibaba for business applications. Applications that use Nacos should import the matching Spring Cloud Alibaba BOM explicitly.

## Recommended BOM Order

Use one dependency-management block with a deliberate order:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Application or company platform BOMs, if any. -->

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Application-specific overrides, if they are intentional. -->
    </dependencies>
</dependencyManagement>
```

After importing a BOM, remove duplicate dependency-management entries for the same `groupId` and `artifactId` unless the application intentionally wants to override that BOM. A duplicate entry without a version can still hide the imported BOM result and lead Maven to report a missing dependency version.

## Minimal jfoundry Dependencies For Alignment

During platform alignment, prefer test-only `jfoundry` usage:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-test</artifactId>
    <scope>test</scope>
</dependency>
```

Do not add runtime starters until the platform build, dependency tree, and local startup behavior are understood.

## Verification Commands

Replace `<profile>` and `<start-module>` with application-specific values:

```bash
mvn -P<profile> -nsu -pl <start-module> -am test -DskipTests
```

Expected result: Maven resolves the upgraded platform and exits with `BUILD SUCCESS`.

```bash
mvn -P<profile> -nsu -pl <start-module> -am dependency:tree \
  -Dincludes=org.springframework.boot,org.springframework.cloud,com.alibaba.cloud,com.baomidou,org.jobrunr,org.jfoundry
```

Expected result: the tree shows the intended Spring Boot, Spring Cloud, Spring Cloud Alibaba, MyBatis-Plus, JobRunr, and `jfoundry` versions.

```bash
mvn -P<profile> -nsu -pl <start-module> -am install -DskipTests
```

Expected result: all reactor artifacts needed by single-module startup are installed into the local Maven repository.

If your Maven mirror is slow or has stale SNAPSHOT metadata, `-nsu` can reduce noise while you isolate the actual compatibility problem. Remove `-nsu` when you need to refresh SNAPSHOT metadata.

## Local Startup Verification

For multi-module applications, install the reactor first and then start from the boot module:

```bash
mvn -P<profile> -nsu -pl <start-module> -am install -DskipTests
mvn -P<profile> -nsu -f <start-module>/pom.xml org.springframework.boot:spring-boot-maven-plugin:${spring-boot.version}:run
```

Expected result: the application reaches the point where its normal infrastructure initializes. For Nacos applications, verify both config loading and service registration.

## Spring Cloud Alibaba 2025 And Nacos Config

For Spring Cloud Alibaba `2025.0.x`, prefer explicit Config Data imports for Nacos configuration:

```yaml
spring:
  config:
    import:
      - nacos:service-common.yml?group=JAVA&refreshEnabled=true
      - nacos:${spring.application.name}.yml?group=JAVA&refreshEnabled=true
```

Adjust the data IDs and groups to match the application. Do not copy private Nacos addresses, namespaces, accounts, or passwords into reusable documentation.

Keep the normal Nacos connection properties in application-owned configuration:

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_ADDRESS:127.0.0.1:8848}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        namespace: ${NACOS_NAMESPACE:public}
      config:
        namespace: ${NACOS_NAMESPACE:public}
        group: DEFAULT_GROUP
        file-extension: yml
```

## OpenFeign 4.3 Attribute Resolution

Spring Cloud OpenFeign `4.3.x` resolves `@FeignClient` attributes eagerly by default. Existing applications often keep Feign URLs in remote configuration. If those placeholders are not available during early startup, the application can fail while validating a URL such as `http://${feign.client.config.example.url}`.

Use this compatibility switch while migrating:

```yaml
spring:
  cloud:
    openfeign:
      lazy-attributes-resolution: true
```

Treat this as a migration compatibility setting. Over time, prefer configuration that is available before Feign clients are registered.

## MyBatis-Plus 3.5.16 Pagination Dependency

MyBatis-Plus `3.5.16` keeps `PaginationInnerInterceptor` in `mybatis-plus-jsqlparser`. Applications or platform libraries that create a `PaginationInnerInterceptor` need this dependency:

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>
```

Use the same MyBatis-Plus version as the rest of the platform. If the application imports `jfoundry-dependencies`, the version is managed by the BOM.

## JobRunr 8 Annotation Package

JobRunr `8.x` moved `@Recurring` to:

```java
import org.jobrunr.jobs.annotations.Recurring;
```

Replace the old import:

```java
import org.jobrunr.spring.annotations.Recurring;
```

The annotation usage can usually stay unchanged.

## Nacos Client Shutdown Observation

Nacos Client `3.0.3` can emit shutdown-order warnings or exceptions during local `Ctrl-C`, even after service registration and deregistration have completed. Treat this as a compatibility observation unless it prevents normal graceful shutdown in the target environment.

## What Must Stay In The Application

Do not move these details into `jfoundry`:

- private Maven profile names and repository URLs
- Nacos server addresses, usernames, passwords, namespaces, and group conventions
- business service names
- database URLs and credentials
- message broker addresses
- company-specific SDKs
- migration strategy for a legacy in-house framework

`jfoundry` should document the integration shape and compatibility traps. Applications own their environment and business context.

## When To Add Runtime Starters

Add runtime starters only after the platform baseline is stable:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-spring-boot-starter</artifactId>
</dependency>
```

For MyBatis-Plus business persistence pilots, add:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-spring-boot-starter-mybatis-plus</artifactId>
</dependency>
```

Outbox, Inbox, Messaging, broker adapters, and JobRunr dispatchers should be added only when a concrete business scenario needs them.
```

- [ ] **Step 2: Verify the guide does not contain private environment values**

Run:

```bash
rg -n "10\\.20\\.|devcloud-ci|enterprise-dev|my-nexus|cxxu|huangxiao|LeiStd|leistd" docs/platform-alignment.md
```

Expected: no matches.

- [ ] **Step 3: Commit the new guide**

Run:

```bash
git add docs/platform-alignment.md
git commit -m "docs: add platform alignment guide"
```

Expected: commit succeeds.

## Task 2: Link The Guide From Existing Docs

**Files:**
- Modify: `README.md`
- Modify: `docs/release/compatibility.md`

- [ ] **Step 1: Update README document list**

Modify the `## 文档` section in `README.md` so it becomes:

```markdown
## 文档

- [平台对齐指南](docs/platform-alignment.md)
- [值对象（Value Object）规范](docs/value-object.md)
- [架构风格指南](docs/architecture-styles.md)
- [ArchUnit 架构规则](docs/archunit-rules.md)
- [Transactional Outbox 事务性发件箱](docs/transactional-outbox.md)
```

- [ ] **Step 2: Add compatibility link**

In `docs/release/compatibility.md`, after the `Stable 1.x Dependency Baseline` table and before the paragraph that starts with `` `org.javassist:javassist` ``, add:

```markdown
Business applications aligning to this matrix should also read
[Platform Alignment Guide](../platform-alignment.md) before adding runtime starters.
```

- [ ] **Step 3: Verify links are present**

Run:

```bash
rg -n "platform-alignment|Platform Alignment Guide|平台对齐指南" README.md docs/release/compatibility.md
```

Expected output includes:

```text
README.md:<line>:- [平台对齐指南](docs/platform-alignment.md)
docs/release/compatibility.md:<line>:[Platform Alignment Guide](../platform-alignment.md)
```

- [ ] **Step 4: Commit link updates**

Run:

```bash
git add README.md docs/release/compatibility.md
git commit -m "docs: link platform alignment guide"
```

Expected: commit succeeds.

## Task 3: Verify Documentation Change

**Files:**
- Inspect: `docs/platform-alignment.md`
- Inspect: `README.md`
- Inspect: `docs/release/compatibility.md`

- [ ] **Step 1: Run Markdown content checks**

Run:

```bash
rg -n "TBD|TODO|待定|待填写|devcloud-ci|enterprise-dev|my-nexus|10\\.20\\.|cxxu|huangxiao" \
  docs/platform-alignment.md README.md docs/release/compatibility.md
```

Expected: no matches.

- [ ] **Step 2: Verify Maven reactor still validates**

Run:

```bash
mvn validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Inspect final diff**

Run:

```bash
git status --short
git log --oneline -5
```

Expected:

- No modified tracked files remain.
- Only unrelated pre-existing untracked files, such as `graphify-out/`, may remain.
- Latest commits include:
  - `docs: link platform alignment guide`
  - `docs: add platform alignment guide`
  - `docs: propose platform feedback plan`

## Task 4: Record Code Follow-Up Decision

**Files:**
- Modify: `docs/superpowers/specs/2026-06-28-platform-feedback-design.md`

- [ ] **Step 1: Add post-implementation note**

Append this section to `docs/superpowers/specs/2026-06-28-platform-feedback-design.md`:

```markdown
## 第一轮执行结果

第一轮只反哺文档：

- 新增 `docs/platform-alignment.md`。
- `README.md` 增加平台对齐指南入口。
- `docs/release/compatibility.md` 增加平台对齐指南入口。
- 暂不修改 `jfoundry-spring-boot-starter-mybatis-plus` 依赖。

`mybatis-plus-jsqlparser` 是否进入 starter 保留为后续观察项。至少再从一个业务项目获得相同反馈后，再决定是加入现有 MyBatis-Plus starter，还是保持文档提示。
```

- [ ] **Step 2: Commit execution note**

Run:

```bash
git add docs/superpowers/specs/2026-06-28-platform-feedback-design.md
git commit -m "docs: record platform feedback decision"
```

Expected: commit succeeds.
