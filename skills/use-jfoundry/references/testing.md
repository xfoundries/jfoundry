# Testing Guidance

## Architecture Tests

Add architecture tests when creating the project skeleton. Do not wait until after application code has grown.

For new Hexagonal projects, copy `assets/templates/java/HexagonalArchitectureTest.java` and replace `PACKAGE_NAME`.

For Onion Simple projects, copy `assets/templates/java/OnionSimpleArchitectureTest.java` and replace `PACKAGE_NAME`.

Use:

```java
@ArchTest
ArchRule[] jfoundryRules = JFoundryRules.hexagonalStrict();

@ArchTest
ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();
```

For Onion:

```java
@ArchTest
ArchRule[] jfoundryRules = JFoundryRules.onionSimple();
```

## Optional Rules

Add these only when the project uses the corresponding style:

- `JFoundryRules.aggregateRepositoryConventions()` for repository boundary enforcement.
- `JFoundryRules.cqrs()` when using jfoundry CQRS annotations.

## Test Scope

Use `jfoundry-architecture-test` with `test` scope. Keep architecture tests in the module or test aggregation that can see the packages being checked.

## Verification

For a single-module project, run:

```bash
mvn test
```

For a multi-module Maven project, run the narrow module first:

```bash
mvn -pl <module> test
```

Use `-am` when dependencies in the same reactor must be built:

```bash
mvn -pl <module> -am test
```

