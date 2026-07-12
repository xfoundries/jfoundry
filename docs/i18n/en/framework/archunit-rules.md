# ArchUnit Architecture Rules

jfoundry turns architecture decisions into reusable ArchUnit rules. Business projects should enable
rules early, then tighten optional conventions as the model stabilizes.

## Recommended Start

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyAppArchitectureTest {

    @ArchTest
    ArchTests architecture = JFoundryRules.hexagonalStrict();

    @ArchTest
    ArchTests ddd = JFoundryRules.jmoleculesDdd();
}
```

`hexagonalStrict()` combines jfoundry baseline guards, native jMolecules Hexagonal rules, and
jfoundry implementation conventions. `jmoleculesDdd()` exposes selected official jMolecules DDD
rules.

All grouped entrypoints return ArchUnit `ArchTests`, so the JUnit 5 engine discovers every nested
rule without treating an `ArchRule[]` array as a single rule.

## Rule Groups

| Group | Purpose |
|-------|---------|
| `PersistenceRules` | Keep persistence implementations free of transaction boundary leakage and component scanning shortcuts. |
| `ValueObjectRules` | Require value objects to be immutable and to provide value semantics. |
| `ArchitectureStyleRules` | Prevent Hexagonal and Onion primary styles from being mixed in the same analysis scope. |
| `FrameworkModuleRules` | Guard jfoundry's own module ring annotations and wrapper annotation usage. |
| `AggregateRepositoryConventionRules` | Optional guardrails for jMolecules `Repository` and jfoundry `AggregateRepository` interfaces that expose query wrappers, paging APIs, or persistence service/mapper APIs. |

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
