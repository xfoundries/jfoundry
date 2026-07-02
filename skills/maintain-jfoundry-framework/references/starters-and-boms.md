# Starters And BOMs

## BOM Layers

- `jfoundry-dependencies`: aggregate framework-neutral BOM exposed to users.
- `jfoundry-modules-dependencies`: jfoundry module versions.
- `jfoundry-foundation-dependencies`: low-level common dependencies.
- `jfoundry-spring-official-dependencies`: Spring official ecosystem versions.
- `jfoundry-spring-integration-dependencies`: Spring integration dependencies and optional technology versions.
- `jfoundry-spring-dependencies`: aggregate Spring business application BOM.

When adding a module or third-party dependency, update the narrowest relevant BOM and any aggregate BOM that imports it.

## Starter Rules

Starters are user-facing dependency entry points. They should:

- contain POM dependencies only;
- avoid Java runtime logic;
- remain capability-specific;
- avoid surprising transitive dependencies;
- make heavy capabilities explicit.

Default Spring Boot starter:

- keep `jfoundry-spring-boot-starter` minimal;
- do not implicitly include MyBatis-Plus stores, Outbox, Inbox, broker adapters, JobRunr, or middleware clients.

Capability starters:

- `jfoundry-event-spring-boot-starter`: local domain event publication.
- `jfoundry-messaging-spring-boot-starter`: messaging contracts, serializer, and default logging sender.
- `jfoundry-messaging-<broker>-spring-boot-starter`: concrete broker sender adapter.
- `jfoundry-outbox-spring-boot-starter`: Outbox core with Spring transaction/scheduling integration.
- `jfoundry-outbox-mybatis-plus-spring-boot-starter`: MyBatis-Plus Outbox store.
- `jfoundry-outbox-jobrunr-spring-boot-starter`: JobRunr dispatcher.
- `jfoundry-inbox-spring-boot-starter`: Inbox core and `InboxTemplate`.
- `jfoundry-inbox-mybatis-plus-spring-boot-starter`: MyBatis-Plus Inbox store.
- `jfoundry-mybatis-plus-spring-boot-starter`: Spring Boot runtime assembly for business MyBatis-Plus persistence, not Outbox/Inbox stores.

## Before Changing A Starter

Check:

1. Is this dependency needed by every user of the starter?
2. Does it make an optional capability implicit?
3. Does it pull a broker, ORM, migration tool, or scheduler into the default path?
4. Does auto-configuration still have matching conditions?
5. Does README or `docs/getting-started-for-business-projects.md` need an update?
6. Does the compatibility matrix need a version entry?

## Release Compatibility

For stable 1.x:

- Java compile target: 21
- Runtime Java baseline: 21
- Spring Boot: 3.5.x
- Spring Framework: 6.2.x
- Maven release tool: 3.9.x

Do not silently move 1.x to Spring Boot 4, Spring Framework 7, Java 25 compile target, or Jakarta EE 11. Treat that as a 2.x compatibility line unless the project explicitly changes release policy.

