# 持久化 DataConverter 与 MapStruct 使用指南

本文面向使用 `jfoundry-infrastructure-mybatis-plus-starter` 的业务项目，说明如何在领域聚合根与持久化 Data 对象之间实现转换。

`DataConverter<T, ID, D, K>` 是聚合仓储边界的一部分。它负责把领域聚合根转换为持久化 Data 对象，并把持久化 Data 对象还原为领域聚合根。这个边界应保持基础设施语义，不应把 MyBatis、JPA、Spring Bean 生命周期或数据库字段泄漏到领域模型。

## 推荐原则

- 领域聚合根只表达业务状态、业务行为和领域事件。
- MyBatis-Plus 注解、TypeHandler、逻辑删除、自动填充和数据库主键类型只放在 Data 对象。
- Data 对象继承 `AggregateData<K>`，`K` 使用数据库和持久化框架天然支持的原始类型，例如 `String`、`Long` 或 `UUID`。
- 领域 ID 继续使用强类型 `Identifier`，通过 `toDataId(...)` 在仓储边界转换为 Data 主键。
- `toData(...)` 可以交给 MapStruct 生成。
- `toEntity(...)` 推荐手写，并调用聚合的 `restore(...)` 工厂方法，显式表达“持久化还原”语义。
- converter 默认不作为 Spring Bean 注入。优先使用 MapStruct 默认 component model，并通过 `Mappers.getMapper(...)` 暴露 `INSTANCE`。

## 为什么不默认做成 Spring Bean

Data converter 是基础设施层的纯对象转换逻辑，通常不需要事务、配置、生命周期回调或运行时注入。把它注册为 Spring Bean 会让仓储构造器出现无业务价值的依赖，并让一个纯转换边界变成运行时装配问题。

如果某个 converter 确实依赖外部服务、配置或复杂组件，可以在业务项目中单独选择 Spring component model。但这应是例外，不是默认模板。

## MapStruct 推荐写法

```java
import org.jfoundry.infrastructure.persistence.DataConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface HelpDocumentDataConverter
        extends DataConverter<HelpDocument, HelpDocumentId, HelpDocumentData, String> {

    HelpDocumentDataConverter INSTANCE = Mappers.getMapper(HelpDocumentDataConverter.class);

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

Repository 实现中推荐使用静态 converter：

```java
@Repository
public class HelpDocumentRepositoryImpl
        extends MybatisPlusRepository<HelpDocument, HelpDocumentId, HelpDocumentData, String>
        implements HelpDocumentRepository {

    private static final HelpDocumentDataConverter CONVERTER = HelpDocumentDataConverter.INSTANCE;

    public HelpDocumentRepositoryImpl(HelpDocumentMapper mapper) {
        super(mapper, CONVERTER);
    }
}
```

在 Spring Boot 应用中，`DomainEventContext` 由 jfoundry 自动配置注入到 `AbstractPersistenceRepository`，业务仓储构造器不需要暴露这个框架内部参数。非 Spring 或手动装配场景仍可使用兼容构造器或 `setDomainEventContext(...)` 显式设置。

## 审计字段与逻辑删除

逻辑删除字段通常是持久化技术字段，应在 `toData(...)` 中显式 ignore。典型字段包括：

- `deleted`
- `deleterId`
- `deleterName`
- `deletedTime`

审计字段需要按建模语义区分：

- 如果审计字段只是 MyBatis 自动填充技术字段，领域对象不承载这些信息，`toData(...)` 可以 ignore。
- 如果业务需要读取或展示审计信息，领域对象可以使用 `AuditableAggregateRoot` 或 `AuditableEntity` 承载审计快照，此时 converter 应保留审计字段映射。

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
