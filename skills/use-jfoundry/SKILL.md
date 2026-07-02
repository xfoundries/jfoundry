---
name: use-jfoundry
description: Guide AI agents and developers when starting or modifying Java business projects that use the jfoundry framework. Use for new project setup, Maven dependency selection, Hexagonal or Onion package layout, jMolecules/JFoundry architecture annotations, aggregate repository versus read port decisions, Outbox/Inbox integration, and ArchUnit test setup in business applications. Do not use for maintaining the jfoundry framework repository itself.
---

# Use JFoundry

## Purpose

Use this skill to build business applications on jfoundry without drifting from the framework's intended architecture. It is for application projects, not for changing the jfoundry framework internals.

Default to a new Java 21 Maven project using Hexagonal Architecture unless the user explicitly chooses Onion. Prefer copying the bundled templates first, then adapting names and packages.

## First-Time Workflow

1. Identify the project shape: single Spring Boot app, multi-module Maven app, or separate domain/application/infrastructure modules.
2. Choose one primary architecture style. Prefer Hexagonal for new business projects; choose Onion only when the user asks for it or the codebase already uses it. Do not mix Hexagonal and Onion in the same ArchUnit analysis scope.
3. Read `references/dependencies.md` and copy the matching Maven template snippets from `assets/templates/maven/`.
4. Read `references/architecture.md` and copy the matching package structure from `assets/templates/structure/`.
5. Copy one architecture test template from `assets/templates/java/`, replace `PACKAGE_NAME`, and add it under the business project's test source set.
6. Read `references/repository-and-ports.md` before creating repositories, query ports, lookup ports, read models, or maintenance ports.
7. Read `references/outbox-inbox.md` only when the project needs reliable external event publication or idempotent message consumption.
8. Run the smallest relevant Maven verification command, usually `mvn test`, or a module-scoped `mvn -pl <module> test`.

## Core Rules

- Keep domain code free of Spring, MyBatis, persistence models, message broker clients, and framework lifecycle APIs.
- Put use case orchestration and transaction-facing workflow in the application layer.
- Express outbound needs as secondary ports. Put MyBatis, JPA, Redis, HTTP clients, MQ clients, and other technology details in infrastructure adapters.
- Primary adapters such as controllers, message listeners, CLI commands, and schedulers must call primary ports or application services, not secondary adapters directly.
- Use aggregate repositories for aggregate lifecycle and command-side aggregate loading. Use lookup/read/maintenance ports for non-aggregate reads.
- Enable Outbox only for reliable publication to an external process or broker. Use local domain event dispatch when events stay in-process.
- Enable Inbox only when a consumer must handle duplicate delivery safely.
- Add ArchUnit tests early. They are part of the project skeleton, not a late cleanup.

## Bundled Templates

Copy templates instead of rewriting them from memory:

- `assets/templates/java/HexagonalArchitectureTest.java`
- `assets/templates/java/OnionSimpleArchitectureTest.java`
- `assets/templates/maven/dependency-management.xml`
- `assets/templates/maven/domain-module-dependencies.xml`
- `assets/templates/maven/application-module-dependencies.xml`
- `assets/templates/maven/infrastructure-mybatis-plus-dependencies.xml`
- `assets/templates/maven/spring-boot-app-dependencies.xml`
- `assets/templates/maven/spring-boot-mybatis-plus-dependencies.xml`
- `assets/templates/maven/outbox-inbox-dependencies.xml`
- `assets/templates/maven/broker-dependencies.xml`
- `assets/templates/structure/hexagonal-package-structure.txt`
- `assets/templates/structure/onion-simple-package-structure.txt`

Replace placeholders such as `PACKAGE_NAME` and `JFOUNDRY_VERSION`. Keep optional snippets optional; do not add Outbox, Inbox, MyBatis-Plus, or broker starters unless the use case needs them.

## Reference Routing

- Read `references/first-use.md` when the user is starting a new project or asks how to invoke this skill.
- Read `references/architecture.md` for package roles, annotations, dependency direction, and architecture style selection.
- Read `references/dependencies.md` for starter selection and Maven snippets.
- Read `references/repository-and-ports.md` before modeling persistence, aggregate repositories, read models, or query ports.
- Read `references/outbox-inbox.md` before adding event externalization, broker adapters, Outbox tables, dispatchers, or consumer idempotency.
- Read `references/testing.md` before adding or changing architecture tests.

## Common First Prompt

When guiding a new project, start by asking for the base package, project/module shape, runtime stack, persistence choice, and whether external messaging is required. If the user has no preference, proceed with:

- Java 21
- Maven
- Spring Boot runtime
- Hexagonal Architecture
- `jfoundry-spring-dependencies` BOM
- `jfoundry-spring-boot-starter`
- `JFoundryRules.hexagonalStrict()` and `JFoundryRules.jmoleculesDdd()`

Suggested user prompt for a new business project:

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 Spring Boot business project.
Base package: com.example.order
Project shape: multi-module Maven
Persistence: MyBatis-Plus
Messaging: Kafka later, not in the initial skeleton
Architecture: default unless you need to choose
```

If details are missing, ask one concise question or proceed with the defaults above when the choice is low risk.
