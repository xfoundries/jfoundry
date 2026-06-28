# CQRS 架构包装设计

## 背景

jfoundry 已经为 Hexagonal 和 Onion 架构提供 `org.jfoundry.architecture.*` 包装注解，业务项目不需要直接依赖 jMolecules 架构注解。当前处理 Repository 查询方法膨胀问题时，需要显式表达 CQRS 读写分离边界。如果业务侧直接使用 `org.jmolecules.architecture.cqrs.*`，会破坏 jfoundry 统一抽象入口。

## 目标

新增 jfoundry CQRS 架构模块，包装 jMolecules CQRS 注解。业务侧通过 jfoundry 注解表达 Command、CommandHandler、CommandDispatcher 和 QueryModel，不直接引入 jMolecules CQRS 类型。

## 设计

新增模块：

```text
jfoundry-architecture/jfoundry-cqrs
```

提供注解：

```text
org.jfoundry.architecture.cqrs.Command
org.jfoundry.architecture.cqrs.CommandHandler
org.jfoundry.architecture.cqrs.CommandDispatcher
org.jfoundry.architecture.cqrs.QueryModel
```

这些注解只做架构语义包装，不提供 Command Bus、Query Bus 或运行时调度。`Command` 和 `CommandHandler` 保留 `namespace`、`name` 属性并提供空字符串默认值；`CommandDispatcher` 保留 `dispatches` 属性并提供空字符串默认值；`QueryModel` 无属性。

## 业务落地方向

业务项目的读侧 DTO/View 可以标注 `@QueryModel`。例如帮助文档列表查询不再通过聚合仓储，而是通过应用层查询服务读取并返回 QueryModel。

## 非目标

本次不引入 Event Sourcing，不提供通用 QueryHandler，不提供命令/查询总线，不强制所有读接口都 CQRS 化。
