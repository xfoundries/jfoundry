# jfoundry

English | [中文](README_ZH.md)

---

`jfoundry` is a practical DDD framework for Java, built on jMolecules and designed to fit well-known Hexagonal Architecture and Onion Architecture styles.

Its goal is straightforward: help business projects make domain modeling, architecture boundaries, and reliable integration executable in code, while keeping the core independent of runtime frameworks. DDD concepts, architecture semantics, application contracts, domain events, Outbox/Inbox, persistence SPI, and messaging SPI do not depend on Spring, Spring Boot, Quarkus, Helidon, or Micronaut.

Spring is the first runtime integration and lives under `jfoundry-spring`. Future Quarkus, Helidon, Micronaut, or other runtime integrations should be added as peer integration modules on top of the same core SPI, instead of pushing runtime dependencies into the core.

## Why jfoundry

Many DDD projects fail not because they lack concepts, but because those concepts never become engineering boundaries. The domain layer gets polluted by Spring, ORM, or MQ APIs; application transaction boundaries become unclear; repositories gradually become generic query interfaces; domain events cannot be externalized reliably; architecture rules remain only in documents.

`jfoundry` addresses these practical problems:

- Express DDD, Hexagonal, Onion, and CQRS semantics with jMolecules.
- Use Maven modules, starters, and SPI to preserve dependency direction across domain, application, infrastructure, and runtime integration boundaries.
- Turn architecture constraints into executable tests with ArchUnit.
- Provide production-oriented building blocks such as Outbox/Inbox, messaging SPI, persistence SPI, and application transaction boundary contracts.

## Core Principles

### Domain First

The domain model is the center. Aggregates, value objects, domain events, repository contracts, and domain exceptions do not depend on Spring, ORM, HTTP, MQ, or database clients.

### Architecture Friendly

DDD is not Hexagonal Architecture, and it is not Onion Architecture. They are design tools at different levels. `jfoundry` provides first-class support for Hexagonal Architecture and Onion Architecture so projects can explicitly choose an architecture style and enforce dependency direction with rules.

### Runtime-neutral Core

Core modules only express business modeling, application orchestration, and external capability contracts. Spring Boot auto-configuration, Spring transactions, Spring events, Web MVC, JobRunr, and similar concerns belong to outer runtime integration modules.

### Production Reliability

The framework includes Transactional Outbox, consumer-side Inbox idempotency, application transaction boundaries, messaging SPI, payload serialization SPI, MyBatis-Plus persistence adapters, and reusable architecture test rules.

## Capabilities

| Area | Capability |
|------|------------|
| DDD building blocks | Aggregate roots, entity base types, value object marker, domain events, repository contracts, domain exceptions |
| Architecture styles | jMolecules-based Hexagonal / Onion annotations and ArchUnit rules |
| Application layer | `ApplicationService`, application exceptions, `TransactionRunner`, CQRS semantics |
| Domain events | Event recording, scoped event context, dispatch contracts, Spring ApplicationEvent adapter |
| Reliable messaging | Transactional Outbox, Inbox idempotency, broker-neutral `MessageSender`, payload serialization SPI |
| Persistence | Runtime-neutral persistence contracts and MyBatis-Plus adapters |
| Runtime integration | Optional Spring Framework / Spring Boot starters, auto-configuration, Web MVC ProblemDetail support |
| Verification | Reusable ArchUnit rules for business projects and internal framework module boundaries |

## Architecture Layers

`jfoundry` makes the following boundaries explicit:

```text
domain
  DDD model, value objects, domain events, repository contracts

application
  application services, transaction boundary contracts, CQRS, Outbox/Inbox SPI,
  domain event orchestration, messaging SPI

infrastructure
  persistence adapters, messaging adapters, serialization adapters,
  background dispatch adapters

runtime integration
  Spring Framework adapters, Spring Boot auto-configuration, and starters
```

Dependencies point inward:

```text
runtime integration -> infrastructure -> application -> domain
```

The same core can be assembled by Spring today, and by Quarkus, Helidon, Micronaut, or another runtime later.

## Quick Start

Choose the BOM for your runtime first, then add starters explicitly by module responsibility.

```xml
<dependencyManagement>
    <dependencies>
        <!-- Runtime-neutral core: DDD, architecture semantics, application contracts, SPI. -->
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

For Spring Boot applications, use the Spring BOM instead:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-spring-dependencies</artifactId>
    <version>${jfoundry.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Typical module dependencies:

```xml
<!-- domain module -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-domain-starter</artifactId>
</dependency>

<!-- application module -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-application-starter</artifactId>
</dependency>

<!-- Spring Boot runtime assembly module, optional -->
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-spring-boot-starter</artifactId>
</dependency>
```

Persistence, Outbox, Inbox, and broker starters should be added explicitly based on business needs. Avoid adding every starter by default. See [Getting Started for Business Projects](docs/getting-started-for-business-projects.md) for module-level dependency guidance.

## Domain Model Example

```java
import org.jfoundry.domain.valueobject.ValueObject;
import org.jmolecules.ddd.annotation.AggregateRoot;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements ValueObject {
}

@AggregateRoot
public class Order {

    private OrderId id;
    private Money total;

    public void changeTotal(Money total) {
        this.total = total;
    }
}
```

## Architecture Verification

Architecture rules should run in business project tests, not remain only in framework documentation:

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyAppArchitectureTest {

    @ArchTest
    static final ArchRule[] onion = JFoundryRules.onionSimple();

    @ArchTest
    static final ArchRule[] ddd = JFoundryRules.jmoleculesDdd();
}
```

Use `JFoundryRules.hexagonalStrict()` when the project chooses Hexagonal Architecture. Use `JFoundryRules.onionSimple()` or `JFoundryRules.onionClassical()` when it chooses Onion Architecture.

## Reliable Event Externalization

Domain events can stay in-process, or they can be externalized reliably through Outbox:

```text
aggregate records domain event
  -> application service boundary drains events
  -> externalization rule selects topic/key/payload
  -> Outbox row is written in the same database transaction
  -> dispatcher claims and sends through MessageSender
  -> consumer side uses InboxTemplate for message/consumer idempotency
```

Outbox is optional. Enable it only when events need cross-process delivery, retry, or reliable externalization. See [Transactional Outbox](docs/transactional-outbox.md) for details.

## Modules

| Module | Purpose |
|--------|---------|
| `jfoundry-domain` | Domain base types, value object contract, domain events, repository contracts |
| `jfoundry-architecture` | Hexagonal, Onion, CQRS architecture semantics and architecture test rules |
| `jfoundry-application` | Application service contracts, transaction boundary, domain event orchestration, messaging, Outbox/Inbox SPI |
| `jfoundry-infrastructure` | Technical adapters for persistence, serialization, messaging, and background dispatch |
| `jfoundry-starters` | Runtime-neutral dependency entry points |
| `jfoundry-spring` | Spring Framework adapters, Spring Boot auto-configuration, and starters |
| `jfoundry-verification` | Internal middleware and runtime behavior verification |

## Documentation

- [Getting Started for Business Projects](docs/getting-started-for-business-projects.md)
- [Architecture Styles](docs/architecture-styles.md)
- [ArchUnit Architecture Rules](docs/archunit-rules.md)
- [Repository and Read-side Ports Migration Guide](docs/repository-vs-read-ports.md)
- [Transactional Outbox](docs/transactional-outbox.md)
- [Persistence DataConverter and MapStruct Guide](docs/persistence-data-converters.md)
- [Value Object Guide](docs/value-object.md)
- [Framework Boundaries](docs/framework-boundaries.md)

## Build

```bash
mvn validate
mvn test
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
