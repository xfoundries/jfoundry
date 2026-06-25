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
    ArchRule[] jfoundryRules = JFoundryRules.hexagonal();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();
}
```

`JFoundryRules.hexagonal()` 启用 JFoundry 基础守护规则 + Hexagonal 主架构风格规则。
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

## 架构风格入口

如果业务项目选择明确的架构风格，可使用显式主风格入口：

```java
@ArchTest
ArchRule[] hexagonalRules = JFoundryRules.hexagonal();

@ArchTest
ArchRule[] onionRules = JFoundryRules.onionSimple();

@ArchTest
ArchRule[] onionClassicalRules = JFoundryRules.onionClassical();
```

- `JFoundryRules.hexagonal()`：基础守护规则 + Hexagonal 主风格入口。
- `JFoundryRules.onionSimple()`：基础守护规则 + Onion Simple 主风格入口。
- `JFoundryRules.onionClassical()`：基础守护规则 + Onion Classical 主风格入口。
- `JFoundryRules.noMixedHexagonalAndOnion()`：单独启用 Hexagonal/Onion 互斥规则。

若只想启用某一类底层规则，请直接使用 `PersistenceRules`、`ValueObjectRules` 或
`ArchitectureStyleRules` 等具体规则类；`JFoundryRules` 只提供主架构风格入口。

## jmolecules 官方 DDD 规则

`JFoundryRules.jmoleculesDdd()` 返回 jmolecules-archunit `0.33.0` 提供的 DDD 原生规则：

- `JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation()` —— 聚合之间只能通过 Id 或 Association 引用，避免直接对象引用导致的边界穿透
- `JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables()` —— 值对象不得引用具备身份的实体或聚合

jMolecules 原生架构规则不再通过一个混合入口暴露，而是随 `JFoundryRules.hexagonal()`、
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
```

## Maven 依赖

业务模块只需依赖 `jfoundry-test`（测试期）：

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-test</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```
