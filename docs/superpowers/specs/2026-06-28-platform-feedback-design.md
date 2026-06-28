# jfoundry 平台对齐反哺设计（2026-06-28）

## 背景

`devcloud-ci-service` 在接入新版 `jfoundry` 前，先完成了平台版本对齐：

- Spring Boot `3.5.16`
- Spring Cloud `2025.0.3`
- Spring Cloud Alibaba `2025.0.0.0`
- MyBatis-Plus `3.5.16`
- JobRunr `8.7.1`
- `org.jfoundry:jfoundry-dependencies:1.0.0-SNAPSHOT`
- `org.jfoundry:jfoundry-test:1.0.0-SNAPSHOT`

这次升级发现的问题大多不是 `devcloud-ci-service` 的业务问题，而是业务项目接入 `jfoundry` 前普遍会遇到的平台兼容性、依赖诊断和 starter 选择问题。因此需要将这些经验反哺到 `jfoundry`，让后续业务项目能按清单完成平台对齐，减少重复踩坑。

## 目标

本次反哺的目标是先补齐文档和验证模板，再谨慎评估是否需要调整 starter 依赖。

具体目标：

- 为业务项目提供一份平台对齐检查清单。
- 说明 `jfoundry` 1.x 推荐的平台版本和 BOM 导入顺序。
- 记录 Spring Cloud Alibaba 2025、OpenFeign 4.3、MyBatis-Plus 3.5.16、JobRunr 8 的迁移注意点。
- 给出可复制的 Maven 验证命令模板。
- 明确哪些信息属于业务项目环境，不进入 `jfoundry`。
- 评估 `jfoundry-spring-boot-starter-mybatis-plus` 是否需要显式带上 `mybatis-plus-jsqlparser`，但不在第一轮直接改代码。

## 非目标

本次不做以下事情：

- 不把 `devcloud-ci-service` 的 Nacos 地址、namespace、账号、profile 或公司 Maven 仓库写入 `jfoundry`。
- 不修改 `jfoundry` 的核心领域模型、Outbox、Inbox、Messaging 或架构规则。
- 不引入新的命令行工具，例如 `jfoundry doctor`。
- 不改变 `jfoundry` runtime starter 的自动装配行为。
- 不为所有旧业务项目提供完整升级自动化脚本。

## 推荐方案

采用“两步走”：

1. 第一轮只更新文档。
2. 第二轮基于更多业务项目反馈，再决定是否调整 starter 依赖。

这样做的原因：

- 当前反哺点大多是迁移知识和版本兼容性，不需要框架运行时代码才能解决。
- `jfoundry` 的设计原则是按能力显式选择 starter，不能因为一个业务项目踩坑就扩大 starter 依赖面。
- `mybatis-plus-jsqlparser` 是否应进入 `jfoundry-spring-boot-starter-mybatis-plus` 需要单独验证：如果 starter 面向完整 MyBatis-Plus 业务仓储体验，显式带上它有价值；如果 starter 坚持极小依赖，则应只在文档中提示业务侧按需引入。

## 文档落点

新增文档：

```text
docs/platform-alignment.md
```

用途：

- 面向业务项目迁移负责人。
- 说明如何把业务项目平台线对齐到 `jfoundry` 1.x。
- 提供检查清单、依赖树命令和常见失败原因。

同时更新现有文档：

```text
docs/release/compatibility.md
README.md
```

更新策略：

- `docs/release/compatibility.md` 保持版本矩阵和 release gate 视角，只补充指向 `docs/platform-alignment.md` 的链接。
- `README.md` 只在快速开始或文档列表中加入平台对齐文档入口，不塞入过长迁移细节。

## 平台对齐文档结构

`docs/platform-alignment.md` 建议包含以下章节：

### 1. 适用范围

说明本文适用于已经有 Spring Boot / Spring Cloud / MyBatis-Plus / JobRunr 基础设施的业务项目，在引入 `jfoundry` starter 前完成平台线对齐。

### 2. 推荐版本线

列出 `jfoundry` 1.x 推荐基线：

| 组件 | 推荐版本 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| MyBatis-Plus | 3.5.16 |
| JobRunr | 8.7.1 |

### 3. BOM 导入顺序

建议顺序：

1. 业务内部 BOM。
2. Spring Boot BOM。
3. Spring Cloud BOM。
4. Spring Cloud Alibaba BOM。
5. `jfoundry-dependencies` BOM。
6. 业务项目补充的显式 dependency management。

同时说明：业务 POM 中如果保留同 groupId/artifactId 的 dependencyManagement 条目，可能覆盖 BOM 管理结果，导致 Maven 报缺版本或解析到旧版本。

### 4. 验证命令模板

提供可替换 profile 的命令：

```bash
mvn -P<profile> -nsu -pl <start-module> -am test -DskipTests
mvn -P<profile> -nsu -pl <start-module> -am dependency:tree \
  -Dincludes=org.springframework.boot,org.springframework.cloud,com.alibaba.cloud,com.baomidou,org.jobrunr,org.jfoundry
mvn -P<profile> -nsu -pl <start-module> -am install -DskipTests
```

说明：

- `<profile>` 由业务项目替换，例如私有 Nexus profile。
- `<start-module>` 由业务项目替换，例如 `ci-start`。
- `-nsu` 可用于减少 SNAPSHOT metadata 更新对首次定位问题的干扰。

### 5. 常见迁移问题

记录以下问题和推荐处理：

- Spring Cloud Alibaba 2025：Nacos Config 推荐显式配置 `spring.config.import=nacos:...`。
- OpenFeign 4.3：旧项目如果 Feign URL 依赖远程配置，可临时启用 `spring.cloud.openfeign.lazy-attributes-resolution=true`。
- MyBatis-Plus 3.5.16：使用 `PaginationInnerInterceptor` 时需要 `com.baomidou:mybatis-plus-jsqlparser`。
- JobRunr 8：`@Recurring` 从 `org.jobrunr.spring.annotations.Recurring` 迁移到 `org.jobrunr.jobs.annotations.Recurring`。
- Nacos Client 3.0.3：本地 Ctrl-C 停止时可能出现 shutdown 顺序相关警告，应作为兼容性观察项，不影响正常启动成功判定。

### 6. 不应进入 jfoundry 的业务信息

明确这些内容不应写入 `jfoundry`：

- 公司 Maven profile 名称和仓库地址。
- Nacos 服务器地址、账号、namespace、group 约定。
- 业务项目私有 SDK。
- 业务服务名、数据库地址、MQTT 地址。
- 业务项目的历史框架，例如 LeiStd 的具体迁移策略。

## 可选代码调整候选

### 候选：MyBatis-Plus starter 显式依赖 jsqlparser

候选改动：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>
```

可能落点：

```text
jfoundry-spring/jfoundry-spring-boot-starter-mybatis-plus/pom.xml
```

支持理由：

- `PaginationInnerInterceptor` 是业务 MyBatis-Plus 项目常用能力。
- MyBatis-Plus 3.5.16 已将该能力拆分到单独 artifact。
- starter 如果定位为“业务 MyBatis-Plus 仓储开箱即用”，带上该依赖可降低接入成本。

保留理由：

- `jfoundry` 当前强调按能力显式选择，starter 依赖不宜过宽。
- 并非所有业务项目都需要分页插件。
- 第一轮只有一个业务项目反馈，样本不足。

决策建议：

- 第一轮只在文档中记录。
- 如果后续第二个业务项目也遇到相同问题，再评估将其加入 starter 或提供 `jfoundry-spring-boot-starter-mybatis-plus-pagination` 之类更细能力入口。

## 验收标准

第一轮实施完成后应满足：

- 新增 `docs/platform-alignment.md`。
- `docs/release/compatibility.md` 链接到平台对齐文档。
- `README.md` 文档列表包含平台对齐文档入口。
- 文档使用通用占位符，不包含 DevCloud CI 的私有地址、账号或数据库信息。
- 文档包含 BOM 顺序、验证命令、常见迁移问题和非目标说明。
- 不修改 Java 源码和 starter POM。

## 后续观察项

- 是否需要 `jfoundry doctor` 或 Maven profile 来自动检查业务项目依赖对齐。
- 是否需要在 `jfoundry-test` 中增加面向业务项目的架构债务 baseline/report-only 能力。
- 是否需要在 MyBatis-Plus starter 中显式加入 `mybatis-plus-jsqlparser`。
- 是否需要为 Spring Cloud Alibaba / OpenFeign / JobRunr 写独立迁移页。

## 第一轮执行结果

第一轮只反哺文档：

- 新增 `docs/platform-alignment.md`。
- `README.md` 增加平台对齐指南入口。
- `docs/release/compatibility.md` 增加平台对齐指南入口。
- 暂不修改 `jfoundry-spring-boot-starter-mybatis-plus` 依赖。

`mybatis-plus-jsqlparser` 是否进入 starter 保留为后续观察项。至少再从一个业务项目获得相同反馈后，再决定是加入现有 MyBatis-Plus starter，还是保持文档提示。
