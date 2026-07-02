# First Use Guide

## Minimal Prompt

Use this prompt when starting a business project from scratch:

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 Spring Boot business project.
Base package: PACKAGE_NAME
Project shape: single app or multi-module Maven
Persistence: none, MyBatis-Plus, JPA, or undecided
Messaging: none, Kafka, RabbitMQ, RocketMQ, or undecided
Architecture: default
```

## Agent Sequence

The agent should:

1. Confirm or infer the base package and project shape.
2. Default architecture to Hexagonal unless the user requests Onion.
3. Copy Maven snippets from `assets/templates/maven/`.
4. Copy package structure from `assets/templates/structure/`.
5. Copy `HexagonalArchitectureTest.java` or `OnionSimpleArchitectureTest.java`.
6. Replace placeholders.
7. Create package-level architecture annotations where package roles are stable.
8. Add only required optional starters.
9. Run Maven verification.

## Recommended Defaults

Use these defaults when the user has no preference:

- Java 21
- Maven
- Spring Boot
- Hexagonal Architecture
- `jfoundry-spring-dependencies`
- `jfoundry-spring-boot-starter`
- no Outbox
- no Inbox
- no broker starter
- no MyBatis-Plus unless persistence is explicitly requested

## When To Ask Before Proceeding

Ask before continuing when:

- The base package is unknown.
- The project shape affects file creation substantially.
- The persistence choice is unknown and code generation depends on it.
- The user asks for external messaging but does not identify a broker.
- The project already has an architecture style and it conflicts with the defaults.
