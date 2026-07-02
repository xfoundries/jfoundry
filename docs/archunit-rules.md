# ArchUnit 规则清单

## 快速启用（推荐）

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {

    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.hexagonalStrict();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();
}
```

`JFoundryRules.hexagonalStrict()` 启用 JFoundry 基础守护规则 + jMolecules Hexagonal 主架构规则 + JFoundry Hexagonal 推荐落地约定。
`JFoundryRules.jmoleculesDdd()` 引入 jmolecules 官方 DDD 规则。

## jfoundry 自有规则

### PersistenceRules

| 规则 | 作用 |
|------|------|
| `persistence_repository_must_not_use_transactional` | 持久化实现包下零 `@Transactional` |
| `autoconfig_must_not_use_component` | autoconfig 模块禁止 `@Component`（用 `@AutoConfiguration` + `@Bean`） |

### ValueObjectRules

| 规则 | 作用 |
|------|------|
| `value_objects_must_be_final` | ValueObject 实现类必须 final 或 record |
| `value_object_fields_must_be_final` | ValueObject 字段必须全部 final |
| `value_objects_must_implement_equals_and_hashCode` | ValueObject 必须实现 equals/hashCode |

### ArchitectureStyleRules

| 规则 | 作用 |
|------|------|
| `hexagonal_and_onion_must_not_be_mixed` | 同一个 ArchUnit 分析范围内禁止同时使用 Hexagonal 与 Onion 主架构风格 |

### FrameworkModuleRules

| 规则 | 作用 |
|------|------|
| `framework_should_use_jmolecules_architecture_annotations_internally` | JFoundry 框架内部直接使用 jMolecules 架构注解，不依赖 JFoundry 包装注解；包装注解作为业务项目门面保留 |
| `domain_packages_should_be_onion_domain_ring` | `org.jfoundry.domain..` 必须标注 Onion simplified `DomainRing` |
| `application_packages_should_be_onion_application_ring` | `org.jfoundry.application..` 必须标注 Onion simplified `ApplicationRing` |
| `infrastructure_packages_should_be_onion_infrastructure_ring` | `org.jfoundry.infrastructure..` 必须标注 Onion simplified `InfrastructureRing` |

### AggregateRepositoryConventionRules

这组规则是可选约定，不会进入 `JFoundryRules.hexagonal()`、`onionSimple()` 等主架构入口。它只守护明确的技术类型泄漏，不通过类名猜测某个返回值是不是读模型。

| 规则 | 作用 |
|------|------|
| `aggregate_repositories_must_not_expose_query_condition_types` | `AggregateRepository` 子接口方法签名和继承关系中禁止暴露 MyBatis-Plus `Wrapper`、Spring Data JPA `Specification` 等通用条件 API |
| `aggregate_repositories_must_not_expose_paging_types` | `AggregateRepository` 子接口禁止暴露 `Page`、`IPage`、`Pageable` 等分页 API |
| `aggregate_repositories_must_not_expose_persistence_service_types` | `AggregateRepository` 子接口禁止暴露 `BaseMapper`、`IService`、Spring Data Repository 等持久化 service/mapper API |

启用方式：

```java
@ArchTest
ArchRule[] aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();
```

说明：

- 该规则不禁止业务具名方法，例如 `findCurrentOperation` 或 `findByBusinessKey`。这类方法是否属于命令侧聚合定位，需要项目按业务语义评审。
- 该规则不根据 `*View`、`*Summary`、`*Record`、`*Response`、`*DTO` 等后缀判断读模型，因为这些名称也可能是合法聚合或领域概念。
- 如果项目希望禁止应用层或领域层直接依赖 MyBatis-Plus `Wrapper`、本地历史框架 `Specification` 或其它条件对象，应在业务侧按自己的模块/包名补充本地 ArchUnit 规则，避免 JFoundry 对开源用户做过度假设。

## 架构风格入口

如果业务项目选择明确的架构风格，可使用显式主风格入口：

```java
@ArchTest
ArchRule[] onionRules = JFoundryRules.onionSimple();

@ArchTest
ArchRule[] onionClassicalRules = JFoundryRules.onionClassical();

@ArchTest
ArchRule[] hexagonalRules = JFoundryRules.hexagonalStrict();
```

- `JFoundryRules.onionSimple()`：基础守护规则 + Onion Simple 主风格入口。
- `JFoundryRules.onionClassical()`：基础守护规则 + Onion Classical 主风格入口。
- `JFoundryRules.hexagonalStrict()`：基础守护规则 + Hexagonal 主风格入口 + JFoundry Hexagonal 推荐落地约定；推荐业务项目使用。
- `JFoundryRules.hexagonal()`：基础守护规则 + jMolecules Hexagonal 主风格入口；适合只需要原生 Hexagonal 依赖规则、不需要 JFoundry 包名和类型形态约定的场景。
- `JFoundryRules.hexagonalConventions()`：只启用 JFoundry Hexagonal 推荐落地约定，可与自定义主架构规则组合使用。
- `JFoundryRules.noMixedHexagonalAndOnion()`：单独启用 Hexagonal/Onion 互斥规则。

若只想启用某一类底层规则，请直接使用 `PersistenceRules`、`ValueObjectRules` 或
`ArchitectureStyleRules` 等具体规则类；`JFoundryRules` 只提供主架构风格入口。

## jmolecules 官方 DDD 规则

`JFoundryRules.jmoleculesDdd()` 返回 jmolecules-archunit `0.33.0` 提供的 DDD 原生规则：

- `JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation()` —— 聚合之间只能通过 Id 或 Association 引用，避免直接对象引用导致的边界穿透
- `JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables()` —— 值对象不得引用具备身份的实体或聚合

jMolecules 原生架构规则不再通过一个混合入口暴露，而是随 `JFoundryRules.hexagonal()`、`JFoundryRules.hexagonalStrict()`、
`JFoundryRules.onionSimple()` 或 `JFoundryRules.onionClassical()` 进入对应主风格入口。

> 说明：jmolecules-integrations 的版本由 `jmolecules-bom` `2025.0.2` 统一锁定为 `0.33.0`（`0.34.0` 并不存在于 Maven Central，jmolecules-integrations 的版本号从 `0.33.0` 直接跳到 `1.6.0`）。业务侧无需在 pom 中单独声明此版本。

## 精细控制

如果只想启用某一组规则：

```java
@ArchTest
ArchRule noTransactionalInPersistence = PersistenceRules.persistence_repository_must_not_use_transactional;

@ArchTest
ArchRule[] valueObjectRules = {
    ValueObjectRules.value_objects_must_be_final,
    ValueObjectRules.value_object_fields_must_be_final,
    ValueObjectRules.value_objects_must_implement_equals_and_hashCode
};

@ArchTest
ArchRule[] aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();
```

## Maven 依赖

业务模块只需依赖 `jfoundry-architecture-test`（测试期）：

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-architecture-test</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```
