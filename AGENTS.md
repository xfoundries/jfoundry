# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 multi-module Maven project for a jMolecules-based, runtime-neutral DDD framework. Top-level modules are declared in `pom.xml`: `jfoundry-dependencies`, `jfoundry-architecture`, `jfoundry-domain`, `jfoundry-application`, `jfoundry-infrastructure`, `jfoundry-starters`, `jfoundry-spring`, and `jfoundry-verification`. Production code uses standard Maven paths such as `src/main/java`; tests live under `src/test/java`; module resources live under `src/main/resources` or `src/test/resources`. SQL files shipped by jfoundry are templates, not auto-run migrations. Documentation is in `docs/`, with the default English overview in `README.md` and the Chinese overview in `README_ZH.md`.

## Build, Test, and Development Commands

- `mvn validate` checks the Maven reactor and module structure.
- `mvn test` compiles and runs all unit, integration, and ArchUnit tests.
- `mvn clean install` performs a full local build and installs artifacts into the local Maven repository.
- `mvn -pl jfoundry-domain test` runs tests for one module; add `-am` when dependencies must also be built.
- `mvn clean install -DskipTests` builds artifacts without executing tests; use only for local iteration.

## Coding Style & Naming Conventions

Use Java 21 features where they simplify the model, especially records for immutable value objects. Follow the existing package root `org.jfoundry.*` and standard Maven layout. Keep domain modules free of Spring and persistence dependencies; place Spring auto-configuration under `jfoundry-spring`, persistence implementations under `jfoundry-infrastructure`, reusable architecture test rules under `jfoundry-architecture/jfoundry-architecture-test`, and internal middleware verification under `jfoundry-verification`. Name tests with a `*Test` suffix. No formatter plugin is configured, so match the surrounding Java style: four-space indentation, clear method names, and concise English Javadocs/comments only where API intent or non-obvious behavior needs explanation.

## Architecture Boundaries

The core framework must remain independent of runtime frameworks such as Spring, Spring Boot, Quarkus, Helidon, and Micronaut. Keep these boundaries explicit:

- `jfoundry-domain` contains domain modeling primitives and must not depend on application, infrastructure, persistence, messaging, or runtime integration modules.
- `jfoundry-application` contains application-layer contracts, transaction abstractions, domain event orchestration, Outbox/Inbox SPI, messaging SPI, and serialization SPI. It must not depend on Spring, MyBatis-Plus, broker clients, or concrete databases.
- `jfoundry-infrastructure` contains concrete adapters for persistence, messaging, serialization, and job execution. Infrastructure adapters may depend on native clients such as MyBatis-Plus, Kafka clients, RabbitMQ Java client, RocketMQ client, Jackson, or JobRunr, but they must not depend on Spring Framework or Spring Boot unless they are deliberately placed under `jfoundry-spring`.
- `jfoundry-spring` is the Spring runtime integration layer. Put Spring Framework adapters, Spring Boot auto-configuration, and Spring Boot starters here. Spring-side wrappers may adapt Spring clients such as `KafkaTemplate` or `RabbitTemplate` to core SPI interfaces, but core and infrastructure modules must not require those Spring clients.
- `jfoundry-starters` and `jfoundry-spring/jfoundry-spring-boot-starters` should assemble existing capabilities; avoid placing domain logic, persistence logic, or broker-specific behavior directly in starters.
- `jfoundry-verification` is for framework-internal smoke, integration, and architecture verification. Do not publish business-facing APIs from verification modules.

## Language Policy

As an open-source framework, source-level artifacts must be friendly to the wider Java ecosystem:

- Java source comments must be written in English. This includes Javadocs, `package-info.java`, inline comments, test documentation comments, configuration property comments, architecture rule explanations, and SQL/resource comments shipped in jars.
- Public documentation may be localized, but languages must not be mixed in the same document. `README_ZH.md` is the default Chinese overview; `README.md` is the English overview. Add or update separate localized files instead of mixing Chinese and English sections in one file.
- Commit messages, release notes intended for repository history, Maven metadata, generated documentation text, and PR descriptions should be written in English.
- When editing existing Chinese comments in source files, translate them to English instead of adding new Chinese comments nearby. Do not translate user-facing Chinese documentation unless the file is meant to be English.

## Testing Guidelines

Tests use JUnit Jupiter, Spring Boot test support where needed, and ArchUnit for architecture rules. Add focused tests near the module being changed, especially for outbox state transitions, auto-configuration conditions, persistence behavior, and architecture constraints. Some modules configure Surefire with `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED`; keep that requirement in mind when moving specification or reflection-based tests.

## SQL Templates

jfoundry ships SQL only as copyable templates. Do not place framework SQL templates under Flyway's default `db/migration` path, and do not make framework jars auto-create business tables. Business applications should copy templates into their own Flyway/Liquibase migrations or execute DDL manually through their operational process.

- Outbox templates live under `jfoundry/sql/outbox/{database}/create_outbox_event.sql`. Official templates are maintained only for databases this project chooses to support directly, currently MySQL and PostgreSQL.
- Inbox currently uses a portable template under `jfoundry/sql/inbox/common/create_inbox_message.sql`. Add database-specific Inbox templates only when a supported database requires dialect-specific DDL.
- Proprietary or domestic database templates are not maintained as official built-in templates in this repository. They should be supplied by vendors, third-party integration packages, or downstream applications.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commits, for example `fix(outbox): ...`, `test(archunit): ...`, `refactor(ddd-framework): ...`, and `docs: ...`. Keep commits scoped and use the module or concern as the scope when helpful. Follow the Language Policy for commit and PR text: keep the Conventional Commits type and optional scope, and write the subject and body in English, for example `refactor(application): split application core module` or `fix(outbox): update retry state consistently`. Pull requests should describe the behavior change, list validation commands run, link related issues, and call out migration, configuration, or compatibility impact.

## Documentation Comments

Javadocs and documentation comments in source code must follow the Language Policy. There is no Javadoc i18n mechanism for comment bodies; generated documentation uses the text from source comments. Keep comments concise and focused on API intent; avoid restating obvious implementation details.

## Project Skills

- Framework maintenance and downstream adoption skills now live in the standalone `xfoundries/software-architecture-skills` project.
- When modifying this repository's framework code, module boundaries, starters, BOMs, Spring Boot auto-configuration, architecture rules, runtime adapters, or release/compatibility docs, use the framework-maintenance skill from that project when available.
- When helping a downstream business project adopt or use jfoundry, recommend the `domain-architecture` plugin from `xfoundries/software-architecture-skills` and use its `use-jfoundry` skill.
- Do not apply `maintain-jfoundry-framework` rules to downstream business projects.
- Do not apply downstream `use-jfoundry` scaffolding rules when changing jfoundry internals.
