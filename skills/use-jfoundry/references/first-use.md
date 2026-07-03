# First Use Guide

## Minimal Prompt

Use this prompt when starting a business project from scratch:

```text
Use $use-jfoundry to create the initial architecture for a new Java 21 business project.
Base package: PACKAGE_NAME
Project shape: multi-module Maven preferred, or single app for small projects
Runtime: none, Spring Boot, Spring Framework, or undecided
Persistence: none, MyBatis-Plus, JPA, or undecided
Messaging: none, Kafka, RabbitMQ, RocketMQ, or undecided
Architecture: default
```

## Agent Sequence

The agent should:

1. Confirm or infer the base package and project shape; prefer multi-module Maven for normal DDD projects.
2. Default architecture to Hexagonal unless the user requests Onion.
3. Copy Maven snippets by module or layer from `assets/templates/maven/`; never put Spring Boot starters in domain or application modules.
4. Copy package structure from `assets/templates/structure/`.
5. Copy `HexagonalArchitectureTest.java` or `OnionSimpleArchitectureTest.java`.
6. Replace placeholders.
7. Create package-level architecture annotations where package roles are stable.
8. Add only required optional starters.
9. Run Maven verification.

## Recommended Defaults

Use these defaults when the user has no preference:

- Java 21
- multi-module Maven for normal DDD projects
- Hexagonal Architecture
- no runtime framework binding yet
- `jfoundry-dependencies`
- `jfoundry-domain-starter` in the domain module
- `jfoundry-application-starter` in the application module
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
