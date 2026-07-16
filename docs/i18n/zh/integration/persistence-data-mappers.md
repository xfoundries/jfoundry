# 持久化 DataMapper 与 MapStruct 使用指南

本文面向使用 `jfoundry-infrastructure-mybatis-plus-starter` 的业务项目，说明如何在领域聚合根与持久化 Data 对象之间实现转换。

`DataMapper<T, ID, D, K>` 是聚合仓储边界的一部分。它负责把领域聚合根映射为持久化 Data 对象，并把 Data 对象还原为领域聚合根。这个边界应保持基础设施语义，不应把 MyBatis、JPA、Spring Bean 生命周期或数据库字段泄漏到领域模型。

## 推荐原则

- 领域聚合根只表达业务状态、业务行为和领域事件。
- MyBatis-Plus 注解、TypeHandler、逻辑删除、自动填充和数据库主键类型只放在 Data 对象。
- Data 对象继承 `AggregateData<K>`，`K` 使用数据库和持久化框架天然支持的原始类型，例如 `String`、`Long` 或 `UUID`。
- 领域 ID 继续使用强类型 `Identifier`，通过 `toDataId(...)` 在仓储边界转换为 Data 主键。
- `toData(...)` 可以交给 MapStruct 生成。
- `toEntity(...)` 推荐手写，并调用聚合的 `restore(...)` 工厂方法，显式表达“持久化还原”语义。
- mapper 默认不作为 Spring Bean 注入。优先使用 MapStruct 默认 component model，并通过 `Mappers.getMapper(...)` 暴露 `INSTANCE`。

## 为什么不默认做成 Spring Bean

Data mapper 是基础设施层的纯映射逻辑，通常不需要事务、配置、生命周期回调或运行时注入。把它注册为 Spring Bean 会让仓储构造器出现无业务价值的依赖，并让一个纯映射边界变成运行时装配问题。

如果某个 mapper 确实依赖外部服务、配置或复杂组件，可以在业务项目中单独选择 Spring component model。但这应是例外，不是默认模板。

## MapStruct 推荐写法

```java
import org.jfoundry.infrastructure.persistence.DataMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface HelpDocumentDataMapper
        extends DataMapper<HelpDocument, HelpDocumentId, HelpDocumentData, String> {

    HelpDocumentDataMapper INSTANCE = Mappers.getMapper(HelpDocumentDataMapper.class);

    @Override
    @Mapping(target = "id", expression = "java(toDataId(entity.getId()))")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deleterId", ignore = true)
    @Mapping(target = "deleterName", ignore = true)
    @Mapping(target = "deletedTime", ignore = true)
    HelpDocumentData toData(HelpDocument entity);

    @Override
    default HelpDocument toEntity(HelpDocumentData data) {
        if (data == null) {
            return null;
        }
        return HelpDocument.restore(
                HelpDocumentId.of(data.getId()),
                data.getTitle(),
                data.getContent(),
                data.getStatus());
    }

    @Override
    default String toDataId(HelpDocumentId id) {
        return id == null ? null : id.value();
    }
}
```

Repository 实现中推荐使用静态 mapper：

```java
@Repository
public class HelpDocumentRepositoryImpl
        extends MybatisPlusAggregateRepository<HelpDocument, HelpDocumentId, HelpDocumentData, String>
        implements HelpDocumentRepository {

    private static final HelpDocumentDataMapper DATA_MAPPER = HelpDocumentDataMapper.INSTANCE;

    public HelpDocumentRepositoryImpl(HelpDocumentMapper mapper) {
        super(mapper, DATA_MAPPER);
    }
}
```

在 Spring Boot 应用中，`DomainEventContext` 由 jfoundry 自动配置注入到 `AbstractAggregateRepository`，业务仓储构造器不需要暴露这个框架内部参数。非 Spring 或手动装配场景可在仓储构造完成后调用 `setDomainEventContext(...)` 显式设置。

## 单 Data 与复合持久化

`MybatisPlusAggregateRepository` 为一个 `AggregateData`、一个 `DataMapper` 和一个 `BaseMapper` 可完整保存与还原的聚合提供默认实现。

对于以 MyBatis-Plus Data 表示根记录的多表聚合，同一个基类可接收根 Data 与 ID 的映射函数。业务 Adapter 覆盖完整 `do*` 操作，并使用 `loadAggregate`、`insertAggregate`、`updateAggregate` 和 `deleteAggregate` 保留根记录持久化、可选乐观锁与 context 跟踪；聚合还原和从属记录同步策略仍由业务 Adapter 决定。若根记录不是一个 MyBatis-Plus Data，或这些 helper 不适用，再直接继承 `AbstractAggregateRepository`。

不要创建一个通用的“多表 Repository”来预设从属集合的同步算法。全量替换、差异更新、追加写入和数据库级联应由业务语义、引用关系、审计要求、数据规模与并发要求决定。

默认删除路径和 `deleteAggregate` helper 会先删除根记录，确保回调执行前已经识别乐观锁冲突。不能脱离根记录存在的从属行应优先使用数据库级联；若外键要求显式先删子表，应采用项目自己的原子删除策略，而不是强行套用该 helper。

## 持久化所有的乐观并发状态

不要仅仅因为持久化技术使用乐观锁，就把数据库 version 放入领域聚合。
`AggregatePersistenceContext` 按聚合对象身份跟踪一个运行时事务内的持久化状态。
存在 `jfoundry-persistence-spring` 时，Spring Boot 会提供事务绑定实现，业务代码不需要手动开启或关闭 scope。
在活动事务之外使用跟踪状态，或者修改并非在当前事务加载的聚合，都会立即失败；当前不支持对分离聚合执行合并。

MyBatis-Plus 根 Data 可在 version 字段上使用 `@Version`，显式配置
`OptimisticLockerInnerInterceptor`，并把 Data 类型传给 `MybatisPlusAggregateRepository`。
仓储会从元数据自动识别版本字段，并在内部完成加载版本跟踪、`updateById` 前版本还原、零行冲突判断、成功后的跟踪版本推进以及 ID + version 删除。没有 `@Version` 的 Data 保持非跟踪行为；业务构造器不接收 `AggregatePersistenceContext`。

JPA 支持的默认边界是一个聚合对应一个由 JPA 管理的实体图。`EntityManager.find` 加载该实体图后，
`JpaAggregateRepository` 会跟踪其中受管理的根实体，把聚合状态应用到同一实体图，并在仓储成功返回前
`flush`；它不会调用 `merge`。加载、执行业务行为和调用 `modify(...)` 必须处于同一个事务和持久化
上下文中；不支持对分离聚合执行合并。

`JpaAggregateMapper` 负责 JPA 实体图创建、聚合还原、ID 转换，以及把当前聚合状态同步到受管理的实体图。
乐观并发控制应在实体图根实体上声明 `@Version`。JPA 会在仓储 `flush` 时检测根实体的并发更新，并在仓储
操作成功前报告为 `ConflictException`。运行时集成负责注入持久化上下文，业务构造器不接收它。如果表示方式
要求手动同步多个表或多个实体图，则完整同步逻辑仍由业务适配器负责；jfoundry 不提供通用的复合同步算法。

## 持久化异常翻译

`jfoundry-persistence-core` 定义了与运行时无关的 `PersistenceFailureTranslator` SPI 和
`AbstractPersistenceAdapter`。该基类默认使用原样透传的 translator，并在受保护的 `find`、`query`、
`add`、`modify`、`remove` 调用外围应用它，因此 core 不依赖 Spring 或其他运行时框架。
`AbstractAggregateRepository` 继承该基类，同时保留固定的聚合生命周期 `do*` 扩展点和领域事件登记。

`jfoundry-persistence-spring` 是可选的共享 Spring 运行时适配器，负责提供事务绑定的聚合持久化
上下文，而不是 JPA 专属适配器。其 `SpringDataAccessFailureTranslator` 只把已知的资源不可用、瞬时资源故障和查询超时转换为 `ExternalAccessException`，并保留 cause。重复键、完整性约束、锁冲突、SQL 或 Mapper 缺陷以及未知异常保持原样。只有当业务适配器能确定被违反的约束确实表示业务冲突时，才可将重复键转换为 `ConflictException`，例如聚合根标识已经存在。

MyBatis-Plus Spring Boot starter 会引入该运行时 Adapter，自动配置会把默认 translator 注入每个
`AbstractPersistenceAdapter` 实例。业务应用声明自己的 `PersistenceFailureTranslator` Bean 后会替代默认实现。
只读 Reader 在检索调用外围使用 `query` 或 `find`。当 CQRS 在事件或状态变化后物化派生读模型时，
Projection Store 按需要在插入或 upsert 调用外围使用 `add` 或 `modify`。Reader、Projection Store 等
非聚合持久化 Adapter 不再注入 translator，也不再手写 try/catch 翻译代码。投影物化是可选能力，
不要求 Event Sourcing。

## MyBatis-Plus Wrapper 与显式 SQL

普通单表条件、排序、更新和删除在 MyBatis-Plus Java API 能清晰表达时，优先使用 Lambda Wrapper。若正确性依赖一个原子数据库语句或数据库特有行为，例如 compare-and-set 形式的乐观锁更新，则保留显式 SQL。该选择属于业务持久化 Adapter；jfoundry 不预设集合同步算法，也不禁止显式 SQL。

## 审计字段与逻辑删除

逻辑删除字段通常是持久化技术字段，应在 `toData(...)` 中显式 ignore。典型字段包括：

- `deleted`
- `deleterId`
- `deleterName`
- `deletedTime`

审计字段需要按建模语义区分：

- 如果审计字段只是 MyBatis 自动填充技术字段，领域对象不承载这些信息，`toData(...)` 可以 ignore。
- 如果业务需要读取或展示审计信息，领域对象可以使用 `AuditableAggregateRoot` 或 `AuditableEntity` 承载审计快照，此时 mapper 应保留审计字段映射。

不要机械地把所有 `createdTime`、`lastModifiedTime` 字段都 ignore，也不要为了省映射代码把持久化审计基类带回领域对象。

## 编译配置

`jfoundry` BOM 管理 MapStruct 版本，但不会替业务项目配置 annotation processor。业务项目需要在使用 MapStruct 的模块中配置编译器插件，通常是 infrastructure 模块：

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
</dependency>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
    <executions>
        <execution>
            <id>default-compile</id>
            <configuration>
                <compilerArgs>
                    <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
                </compilerArgs>
            </configuration>
        </execution>
    </executions>
</plugin>
```

如果同一模块还使用 Lombok，应把 Lombok annotation processor 一并放入 `annotationProcessorPaths`。`unmappedTargetPolicy=ERROR` 建议只放在编译期执行上，避免测试编译阶段没有 MapStruct 处理器参与时产生无意义警告。

## 不建议框架自动化的部分

`restore(...)` 属于领域模型的还原入口，不是通用技术转换。不同聚合的还原参数、不变量、审计语义和历史兼容策略可能不同。jfoundry 不应在运行时强行推导聚合构造方式，也不应通过反射自动填充领域对象。

因此，推荐由业务项目保留一小段显式 `toEntity(...)` 代码。它的价值不是节省行数，而是让领域对象从持久化状态还原的规则可读、可测、可审查。
