# Framework Boundaries

This document is for maintainers and contributors. It defines where framework code belongs and how
jfoundry keeps its core independent of runtime frameworks.

## Core Decision

jfoundry core modules must not depend on application runtimes such as Spring, Spring Boot, Helidon,
Quarkus, Micronaut, CDI, or Jakarta EE runtime integration APIs. Stable low-intrusion libraries
such as jMolecules and `slf4j-api` may appear in core modules when they express contracts.

## Module Roles

| Area | Modules |
|------|---------|
| Domain and architecture | `jfoundry-domain`, `jfoundry-architecture`, `jfoundry-hexagonal`, `jfoundry-onion`, `jfoundry-cqrs` |
| Application contracts | `jfoundry-application-core`, `jfoundry-transaction-core`, `jfoundry-event-core`, `jfoundry-event-externalization-core`, `jfoundry-messaging-core`, `jfoundry-outbox-core`, `jfoundry-inbox-core` |
| Framework-neutral adapters | `jfoundry-persistence-core`, `jfoundry-persistence-mybatis-plus`, `jfoundry-messaging-jackson`, broker adapters, Outbox/Inbox MyBatis-Plus stores, JobRunr dispatch adapter |
| Spring runtime integration | `jfoundry-spring-runtime/*` |
| Spring Boot integration | `jfoundry-spring-boot-autoconfigure`, `jfoundry-spring-boot-starters/*` |
| Verification | `jfoundry-verification/*` |

## Placement Rules

- Spring Framework lifecycle, transaction synchronization, scheduling, event publishing, MVC APIs,
  and Spring-side client wrappers belong under `jfoundry-spring/jfoundry-spring-runtime`.
- Spring Boot conditions, `@ConfigurationProperties`, bean wiring, metadata, and
  `AutoConfiguration.imports` belong under `jfoundry-spring/jfoundry-spring-boot-autoconfigure`.
- Starters are dependency entry points only; they must not contain runtime behavior.
- Framework-neutral database, broker, serializer, and scheduler adapters belong under
  `jfoundry-infrastructure`.
- Middleware integration tests and Testcontainers compatibility checks belong under
  `jfoundry-verification`.

## Outbox Boundary

`jfoundry-outbox-core` owns the message model, store contract, dispatch service, retry/backoff
contract, and state machine.

`jfoundry-outbox-spring` owns Spring runtime integration such as transaction synchronization,
scheduled dispatching, and domain-event recording in a Spring runtime.

`jfoundry-spring-boot-autoconfigure` owns Outbox configuration properties, conditions, and bean
wiring. `OutboxDispatcherProperties` and related properties live there because property binding is
a Boot concern.

`jfoundry-outbox-jobrunr` is a pure JobRunr dispatch adapter. Its Spring Boot auto-configuration
also belongs under `jfoundry-spring-boot-autoconfigure`.

## Acceptance Criteria

- Core modules have no compile/provided dependency on Spring, Spring Boot, Helidon, Quarkus,
  Micronaut, CDI, Jakarta runtime APIs, broker clients, or persistence framework details.
- Adapter modules do not register Spring Boot auto-configuration directly.
- Starters remain lightweight dependency choices.
- Future runtime integrations can reuse core SPI and framework-neutral adapters without depending
  on Spring Boot.
