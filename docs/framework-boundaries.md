# Framework Boundary Design

## Decision

jfoundry core modules must be independent from application frameworks such as Spring, Spring Boot, Helidon, Quarkus, CDI, and Jakarta EE runtime integration. Core modules may depend on stable, low-intrusion libraries when they express domain or library-level contracts, such as jMolecules or `slf4j-api`.

Jackson, Spring SpEL, Spring scheduling, Spring Boot auto-configuration, and framework lifecycle concerns must not live in core modules.

## Module Roles

Core modules define contracts and framework-neutral behavior:

- `jfoundry-domain`
- `jfoundry-architecture-layered`
- `jfoundry-persistence-core`
- `jfoundry-messaging-core`
- `jfoundry-outbox-core`

Technology adapters implement contracts without owning framework lifecycle:

- `jfoundry-persistence-mybatis-plus`
- `jfoundry-outbox-mybatis-plus`
- `jfoundry-messaging-jackson`
- `jfoundry-outbox-jobrunr`

Framework integrations wire adapters into a specific runtime:

- `jfoundry-spring`
- future `jfoundry-helidon`
- future `jfoundry-quarkus`

Starters are user-facing aggregate entry points for a framework, for example `jfoundry-spring-boot-starter`.

## Required Refactoring

`jfoundry-messaging-core` should keep only messaging SPI, domain event sink contracts, routing metadata, and resolver interfaces. Move `JacksonPayloadSerializer` into `jfoundry-messaging-jackson`. Move Spring SpEL-based externalization resolution into Spring integration or a clearly named Spring/SpEL adapter.

`jfoundry-outbox-core` should expose state machine, repository contracts, dispatch service contracts, retry/backoff abstractions, and a framework-neutral `dispatchOnce` style runtime service.

`jfoundry-outbox-spring` should contain Spring scheduling, transaction synchronization, properties binding, and Spring-specific event publishing or sink wiring.

`jfoundry-outbox-jobrunr` should contain pure JobRunr integration only. Spring Boot auto-configuration for JobRunr belongs in Spring integration.

## Compatibility Rule

Business applications should depend on a framework starter whenever possible. Direct dependency on adapter modules is supported for advanced composition, but adapters must remain usable by non-Spring integrations when their underlying technology allows it.

## Success Criteria

- No core module has compile or provided dependencies on Spring, Spring Boot, Helidon, Quarkus, CDI, or Jakarta runtime APIs.
- Spring-specific classes live only under Spring integration modules.
- Adapter modules do not register framework beans by themselves.
- Spring Boot tests continue to pass through `jfoundry-spring-boot-starter`.
- Future Helidon or Quarkus modules can reuse core SPI and technology adapters without depending on Spring artifacts.
