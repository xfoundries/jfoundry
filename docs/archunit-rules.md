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
    ArchRule[] jfoundryRules = JFoundryRules.all();

    @ArchTest
    ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
}
```

一行引入 jfoundry 自有的全部规则 + jmolecules 官方规则。

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

### LayeredRules

| 规则 | 作用 |
|------|------|
| `dependencies_must_follow_layer_hierarchy` | 应用层 package 不能依赖接口层或基础设施层 package |
| `only_application_may_use_repository_directly` | Repository 实现只能在基础设施层 |

## jmolecules 官方规则

`JFoundryRules.jmoleculesNative()` 返回 jmolecules-archunit `0.33.0` 提供的三条原生规则：

- `JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation()` —— 聚合之间只能通过 Id 或 Association 引用，避免直接对象引用导致的边界穿透
- `JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables()` —— 值对象不得引用具备身份的实体或聚合
- `JMoleculesArchitectureRules.ensureLayering()` —— 基于 jmolecules 分层注解的 LayeredArchitecture 约束

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
