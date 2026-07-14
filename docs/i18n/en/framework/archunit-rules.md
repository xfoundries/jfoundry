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
| `ArchitectureStyleRules` | Require the selected primary style to be declared and prevent Hexagonal and Onion from being mixed in the same analysis scope. |
| `FrameworkModuleRules` | Guard jfoundry's own module ring annotations and wrapper annotation usage. |
| `AggregateRepositoryConventionRules` | Optional guardrails for jMolecules `Repository` and jfoundry `AggregateRepository` interfaces that expose query wrappers, paging APIs, or persistence service/mapper APIs. |
| `CqrsRules` | Optional architecture-neutral placement and dependency rules for CQRS commands, query models, handlers, and dispatchers. |
| `HexagonalConventionRules` | Hexagonal-only port, adapter, package, persistence-isolation, and secondary-side CQRS boundary conventions. |

`CqrsRules` does not import Hexagonal Port or Adapter roles. The rule that prevents secondary ports
and adapters from exposing CQRS entry models belongs to `HexagonalConventionRules`; Onion projects
use their ring rules plus the architecture-neutral CQRS rules without inventing Hexagonal roles.

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

The Hexagonal, Onion Simple, and Onion Classical primary entrypoints fail when the analyzed scope
contains no matching architecture annotation. This is a fail-fast guard against a vacuous pass; it
requires at least one selected-style marker, not every possible role or ring in a partial scope.

## Maven Dependency

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-architecture-test</artifactId>
    <scope>test</scope>
</dependency>
```
