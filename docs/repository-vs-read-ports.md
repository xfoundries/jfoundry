# Repository 与读侧端口迁移指南

本文面向正在从 Active Record、MyBatis-Plus `IService`、通用 Wrapper 或规约模式迁移到 jfoundry 的业务项目，说明什么时候应保留聚合 Repository，什么时候应拆到 `LookupPort`、`ReadModelPort` 或维护端口。

这是一份建模和迁移指南，不要求业务项目为每个查询机械创建接口。端口应表达真实的架构边界，而不是把一个 Mapper 或 Service 原样包一层。

## 核心区分

Repository 表示某类聚合的集合。它的核心职责是按标识或业务身份加载聚合、保存聚合、删除聚合，并配合聚合保护业务不变量。

读侧端口表示应用核心对外部读能力的需求。它可以读取数据库、搜索引擎、缓存、远程服务或组合数据源，但这些技术细节由基础设施适配器隐藏。

维护端口表示后台扫描、清理、批处理候选选择等技术性或运维性能力。它通常不返回完整聚合，而是返回待处理 ID、时间窗口或轻量候选项。

## Repository 适用场景

以下方法可以保留在聚合 Repository：

- 按聚合 ID 加载：`findById(OrderId id)`。
- 按稳定业务身份定位聚合：`findByOrderNo(OrderNo orderNo)`、`findByTenantCode(TenantCode code)`。
- 命令流程中定位聚合，随后立即执行业务行为：查找当前进行中的操作记录后补写结果、查找可重试任务后切换状态、查找资源池条目后申请或释放资源。
- 聚合生命周期维护：删除某个聚合的附属记录、按聚合身份移除已失效对象。

Repository 方法应优先表达领域意图，而不是复制 SQL 条件。`findCurrentOperation(...)`、`findRetryableTask(...)` 通常比 `findLatestByEnvIdAndStatusInOrderByCreatedTimeDesc(...)` 更合适。

## LookupPort 适用场景

`LookupPort` 适合应用服务为了执行业务流程而准备上下文，但读取结果不承担聚合行为。

典型场景：

- 外部 SDK 调用前读取环境编码、应用键、租户编码等上下文。
- 命令流程需要确认某个关联对象是否存在，但不会修改该对象。
- 应用服务需要按业务键读取轻量对象，用于权限裁剪、参数转换或流程分支。
- 一个流程需要跨多个聚合读取数据，但读取结果只是输入资料，不是要被当前用例修改的聚合。

命名上可以使用对象名加 `LookupPort`，例如 `EnvLookupPort`、`EnvAppLookupPort`。方法返回值应尽量是轻量 DTO、record 或专门的 lookup 结果，而不是 MyBatis Data 对象。

## ReadModelPort 适用场景

`ReadModelPort` 适合查询用例、页面展示、报表、列表、统计和读投影。

典型场景：

- 分页列表、下拉选项、详情页组合展示。
- Dashboard、报表、统计汇总。
- 最近操作时间、最新采样值、按条件聚合后的展示数据。
- 查询结果形状明显不同于写模型聚合。

如果项目使用 CQRS，`@QueryModel` 应优先标记应用入口语义上的查询返回模型。Secondary Port 的返回值可以命名为 `ReadModel`、`Summary` 或 `Projection`，但不需要默认标记为 CQRS QueryModel，避免把入口驱动语义扩散到出口端口。

## MaintenancePort 适用场景

后台维护类查询不应为了复用而塞入聚合 Repository。

典型场景：

- 查找超时处理中记录。
- 查找过期数据 ID 并批量删除。
- 按时间窗口扫描待重试、待清理、待修复对象。

维护端口优先返回聚合 ID 或轻量候选项。需要执行领域行为时，应用服务再按 ID 加载聚合并调用聚合方法。只有确实是纯技术清理且不涉及业务不变量时，适配器可以直接执行批量删除。

## 迁移判断顺序

从旧 Wrapper、规约或 `IService` 查询迁移时，按以下顺序判断：

1. 这个查询是否为了修改某个聚合？
2. 如果是，是否可以按聚合 ID 或稳定业务身份加载？
3. 查询结果是否是完整聚合，且随后会调用聚合行为？
4. 如果只是为流程准备上下文，迁到 `LookupPort`。
5. 如果服务页面、列表、报表或统计，迁到 `ReadModelPort`。
6. 如果服务后台扫描、清理、批处理候选选择，迁到 `MaintenancePort`。
7. 如果同一个旧方法同时服务命令和查询，按使用场景拆成两个端口或一个 Repository 方法加一个读侧端口。

## 反例

以下方法通常不应长期留在聚合 Repository：

- 返回分页对象或页面 DTO 的方法。
- 返回 `View`、`Summary`、`Response`、`Projection` 等明显读投影的方法。
- 为 Dashboard 或报表做 `group by`、`sum`、`count` 的方法。
- 只为了给远程 SDK 拼参数而读取若干字段的方法。
- 暴露 MyBatis-Plus `Wrapper`、`IService`、`Page` 等持久化框架类型的方法。
- 方法名直接堆叠数据库条件，并且看不出领域意图的方法。

## 例外

不是所有非 ID 查询都必须拆出 Repository。业务身份查询、命令流程中的聚合定位、生命周期维护都可以是合理的 Repository 方法。

同样，不是所有只读方法都必须引入 CQRS。查询足够简单时，一个 `LookupPort` 就可以表达应用核心对外部读能力的需求。只有当读模型形状、性能、查询复杂度或演进方向明显不同于写模型时，再引入更明确的 `ReadModelPort` 和查询模型。

## 推荐落地形态

Hexagonal 项目中推荐的依赖方向：

```text
Primary Adapter
  -> Primary Port / Application Service
      -> AggregateRepository        # 命令侧聚合加载与保存
      -> LookupPort                 # 流程上下文读取
      -> ReadModelPort              # 查询模型与展示读取
      -> MaintenancePort            # 后台扫描与清理候选
Infrastructure Adapter
  -> implements Secondary Port
```

基础设施适配器可以使用 MyBatis、JPA、SQL、远程 API 或缓存实现这些端口。应用层只依赖端口契约，不依赖具体查询技术。
