# Module Boundaries

## Core Principle

jfoundry's core modules must stay independent of concrete application runtimes. Spring, Spring Boot, Helidon, Quarkus, CDI, Jakarta runtime integration, scheduling, transaction synchronization, property binding, and auto-configuration belong outside the core.

Stable, low-intrusion libraries such as jMolecules and `slf4j-api` may appear in core modules when they express framework contracts or architecture semantics.

## Module Roles

### Framework-Neutral Core

- `jfoundry-domain`: domain building blocks, entity/value object/event/repository abstractions.
- `jfoundry-architecture`: architecture style facade modules and aggregation.
- `jfoundry-architecture/jfoundry-architecture-test`: reusable ArchUnit and test helpers for framework users.
- `jfoundry-application`: application-layer contracts, CQRS annotations, event dispatch contracts, messaging SPI, Outbox/Inbox core contracts.
- `jfoundry-infrastructure`: framework-neutral technical adapters for persistence, messaging, payload serialization, JobRunr dispatching, and similar technologies.

### Runtime Integration

- `jfoundry-spring/jfoundry-spring-runtime`: Spring Framework adapters such as local event publishing, default messaging sender, and outbox transaction/scheduling integration.
- `jfoundry-spring/jfoundry-spring-boot-autoconfigure`: Spring Boot auto-configuration, conditions, properties, and runtime wiring.
- `jfoundry-spring/jfoundry-spring-boot-starters`: dependency entry points only.

### Verification

- `jfoundry-verification`: internal middleware integration tests, Testcontainers, database/broker compatibility checks, and profile-driven integration verification.

## Dependency Direction

Allowed direction:

```text
domain / architecture
  <- application
  <- infrastructure adapters
  <- spring runtime adapters
  <- boot auto-configuration
  <- starters
```

Practical rules:

- Domain must not depend on Spring, MyBatis, JPA, broker clients, Jackson object mapping details, or runtime integration.
- Application modules define contracts and framework-neutral services. They may depend on domain abstractions and jMolecules semantics.
- Infrastructure modules implement or consume application/domain contracts without registering Spring Boot auto-configuration.
- Spring runtime modules may depend on application contracts and framework-neutral adapters.
- Boot auto-configuration wires beans, conditions, properties, and integration defaults.
- Starters depend on modules; they do not contain Java runtime behavior.

## Internal Architecture Style

jfoundry framework internals use Onion simplified:

- `org.jfoundry.domain..` is the domain ring.
- `org.jfoundry.application..` is the application ring.
- `org.jfoundry.infrastructure..` is the infrastructure ring.

Use jMolecules architecture annotations internally. The JFoundry wrapper annotations remain public facades for business projects.

## Red Flags

- A core module starts depending on `spring-*`, `spring-boot-*`, servlet APIs, scheduling APIs, or runtime lifecycle APIs.
- An adapter module adds `AutoConfiguration.imports`.
- A starter module gains Java source with runtime behavior.
- A default starter starts pulling broker clients, Outbox store adapters, Inbox store adapters, JobRunr, or MyBatis-Plus stores implicitly.
- Outbox/Inbox persistence data starts extending aggregate persistence abstractions.
- Boot auto-configuration is marked as an Onion ring package.

