# ArchUnit Architecture Rules

jfoundry turns architecture decisions into reusable ArchUnit rules. Business projects should enable
rules early, then tighten optional conventions as the model stabilizes.

## Recommended Start

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyAppArchitectureTest {

    @ArchTest
    ArchRule[] architecture = JFoundryRules.hexagonalStrict();

    @ArchTest
    ArchRule[] ddd = JFoundryRules.jmoleculesDdd();
}
```

`hexagonalStrict()` combines jfoundry baseline guards, native jMolecules Hexagonal rules, and
jfoundry implementation conventions. `jmoleculesDdd()` exposes selected official jMolecules DDD
rules.

## Rule Groups

| Group | Purpose |
|-------|---------|
| `PersistenceRules` | Keep persistence implementations free of transaction boundary leakage and component scanning shortcuts. |
| `ValueObjectRules` | Require value objects to be immutable and to provide value semantics. |
| `ArchitectureStyleRules` | Prevent Hexagonal and Onion primary styles from being mixed in the same analysis scope. |
| `FrameworkModuleRules` | Guard jfoundry's own module ring annotations and wrapper annotation usage. |
| `AggregateRepositoryConventionRules` | Optional guardrails that keep aggregate repositories from exposing query wrappers, paging APIs, and persistence service/mapper APIs. |

## Entry Points

| Entry | Use when |
|-------|----------|
| `JFoundryRules.hexagonalStrict()` | Recommended Hexagonal project baseline. |
| `JFoundryRules.hexagonal()` | Native jMolecules Hexagonal rules plus jfoundry baseline guards. |
| `JFoundryRules.hexagonalConventions()` | Only jfoundry Hexagonal implementation conventions. |
| `JFoundryRules.onionSimple()` | Onion Simple project baseline. |
| `JFoundryRules.onionClassical()` | Onion Classical project baseline. |
| `JFoundryRules.aggregateRepositoryConventions()` | Optional repository API hardening. |
| `JFoundryRules.jmoleculesDdd()` | Selected official jMolecules DDD rules. |

## Maven Dependency

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-architecture-test</artifactId>
    <scope>test</scope>
</dependency>
```
