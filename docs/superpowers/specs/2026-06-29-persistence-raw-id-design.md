# 持久化原始 ID 设计

## 背景

`AggregateData` 在 jfoundry 中定位为持久层映射对象，是领域聚合根的数据库数据形态。现有设计要求 `AggregateData<ID extends Identifier & Serializable>`，导致持久层数据对象必须直接持有领域强类型 ID。

在 MyBatis-Plus 场景下，这会让数据库主键字段变成 `HelpDocumentId`、`TestOrderId` 这类领域值对象，进而需要每个 ID 类型编写 TypeHandler 和配置类。这个成本来自框架边界建模，而不是业务复杂度。

## 目标

领域模型继续使用强类型 ID，例如 `HelpDocumentId implements Identifier`。持久层 `AggregateData` 使用数据库天然支持的原始 ID 类型，例如 `String`、`Long`、`UUID`。领域 ID 与持久化 ID 的转换发生在 Repository Adapter 的 `DataConverter` 边界。

## 设计

`AggregateData` 的 ID 泛型代表持久化 ID，不再继承 `Identifier`：

```java
public abstract class AggregateData<ID extends Serializable>
```

仓储和转换器拆分领域 ID 与持久化主键泛型：

```text
ID：领域 ID，继承 Identifier
K：持久化主键，继承 Serializable
```

`DataConverter` 负责在聚合根和数据对象之间转换：

```text
TestOrderId <-> String
HelpDocumentId <-> String
```

`AbstractPersistenceRepository` 对外仍实现 `AggregateRepository<T, ID>`，但模板方法使用 `K` 与持久化框架交互。删除和查询时通过新的 `toDataId(ID id)` 方法把领域 ID 转换成持久化主键。

## 非目标

不提供旧泛型签名兼容层。当前 jfoundry 正处于框架设计收敛期，本次直接采用更清晰的 API。

本次不解决复杂读模型或查询条件扩展问题；读模型/CQRS 方案另行处理。

## 测试策略

核心测试覆盖 `AggregateData<String>`、`DataConverter<TestOrder, TestOrderId, TestOrderData, String>` 和 MyBatis-Plus 仓储集成测试。集成测试必须不再注册 `TestOrderIdTypeHandler`，以证明持久化链路使用原始 `String` 主键即可工作。
