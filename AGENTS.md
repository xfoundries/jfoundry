# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 multi-module Maven project for a jmolecules-based DDD framework. Top-level modules are declared in `pom.xml`: `jfoundry-dependencies`, `jfoundry-architecture`, `jfoundry-domain`, `jfoundry-application`, `jfoundry-infrastructure`, `jfoundry-starters`, `jfoundry-spring`, and `jfoundry-verification`. Production code uses standard Maven paths such as `src/main/java`; tests live under `src/test/java`; module resources and SQL migrations live under `src/main/resources` or `src/test/resources`. Documentation is in `docs/`, with the main overview in `README.md`.

## Build, Test, and Development Commands

- `mvn validate` checks the Maven reactor and module structure.
- `mvn test` compiles and runs all unit, integration, and ArchUnit tests.
- `mvn clean install` performs a full local build and installs artifacts into the local Maven repository.
- `mvn -pl jfoundry-domain test` runs tests for one module; add `-am` when dependencies must also be built.
- `mvn clean install -DskipTests` builds artifacts without executing tests; use only for local iteration.

## Coding Style & Naming Conventions

Use Java 21 features where they simplify the model, especially records for immutable value objects. Follow the existing package root `org.jfoundry.*` and standard Maven layout. Keep domain modules free of Spring and persistence dependencies; place Spring auto-configuration under `jfoundry-spring`, persistence implementations under `jfoundry-infrastructure`, reusable architecture test rules under `jfoundry-architecture/jfoundry-architecture-test`, and internal middleware verification under `jfoundry-verification`. Name tests with a `*Test` suffix. No formatter plugin is configured, so match the surrounding Java style: four-space indentation, clear method names, and concise Javadoc only where API intent is not obvious.

## Testing Guidelines

Tests use JUnit Jupiter, Spring Boot test support where needed, and ArchUnit for architecture rules. Add focused tests near the module being changed, especially for outbox state transitions, auto-configuration conditions, persistence behavior, and architecture constraints. Some modules configure Surefire with `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED`; keep that requirement in mind when moving specification or reflection-based tests.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commits, for example `fix(outbox): ...`, `test(archunit): ...`, `refactor(ddd-framework): ...`, and `docs: ...`. Keep commits scoped and use the module or concern as the scope when helpful. As an open-source framework, commit messages must be written in English: keep the Conventional Commits type and optional scope, and write the subject and body in English, for example `refactor(application): split application core module` or `fix(outbox): update retry state consistently`. Pull requests should describe the behavior change, list validation commands run, link related issues, and call out migration, configuration, or compatibility impact.

## Documentation Comments

Public API Javadoc must be written in English so generated documentation, IDE hints, and Maven Central artifacts are friendly to the wider Java ecosystem. Keep comments concise and focused on API intent; avoid restating obvious implementation details.

## Project Skills

- When modifying this repository's framework code, module boundaries, starters, BOMs, Spring Boot auto-configuration, architecture rules, runtime adapters, or release/compatibility docs, use `skills/maintain-jfoundry-framework`.
- When helping a downstream business project adopt or use jfoundry, use `skills/use-jfoundry`.
- Do not apply `maintain-jfoundry-framework` rules to downstream business projects.
- Do not apply `use-jfoundry` scaffolding rules when changing jfoundry internals.
