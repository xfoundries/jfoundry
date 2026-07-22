# jfoundry

English | [中文](README_ZH.md)

---

`jfoundry` is a practical DDD framework for Java, built on jMolecules and designed for Hexagonal Architecture and Onion Architecture.

It helps business projects make domain modeling, architecture boundaries, and reliable integration executable in code. The core defines DDD concepts, architecture semantics, application contracts, domain events, persistence SPI, and messaging SPI without depending on a runtime framework. Spring and Quarkus assemble the same core through peer runtime integration modules.

## Why jfoundry

DDD projects often lose their intended boundaries in implementation: domain code imports framework or ORM APIs, transaction ownership is unclear, repositories become generic query interfaces, and external events are not delivered reliably. `jfoundry` provides:

- jMolecules-based DDD, Hexagonal, Onion, and CQRS semantics.
- Explicit dependency direction across domain, application, infrastructure, and runtime integration.
- Reusable ArchUnit rules for executable architecture constraints.
- Optional production capabilities for persistence, reliable messaging, transactions, and runtime assembly.

## Architecture

The domain model stays independent of Spring, ORM, HTTP, brokers, and database clients. Application contracts orchestrate use cases and define capability SPI; infrastructure implements technical adapters; runtime integrations assemble them.

```text
runtime integration
  -> application / infrastructure adapters
       -> application contracts
            -> domain
```

Dependencies point inward. This keeps runtime integrations outside the core rather than making a particular framework a requirement for every application.

![jfoundry module architecture](docs/i18n/assets/jfoundry-module-architecture.svg)

## Capabilities

| Area | Capability |
|------|------------|
| Domain modeling | Aggregates, value objects, domain events, repository contracts, and domain exceptions |
| Architecture | Hexagonal and Onion semantics with ArchUnit rules |
| Application | Application services, transaction boundaries, CQRS, and domain-event orchestration |
| Persistence | Aggregate persistence contracts with JPA and MyBatis-Plus implementations |
| Reliable messaging | Transactional Outbox, Inbox idempotency, messaging, and serialization SPI |
| Runtime integration | Spring Framework and Spring Boot assembly; Quarkus CDI and Jakarta Transactions integration |

## Choose Your Path

- **Architecture and modeling**: start with [Getting Started](docs/i18n/en/integration/getting-started.md), then select an [architecture style](docs/i18n/en/framework/architecture-styles.md) and review [modeling conventions](docs/i18n/en/modeling/repository-vs-read-contracts.md).
- **Aggregate persistence**: read [Aggregate Persistence](docs/i18n/en/capabilities/aggregate-persistence.md), then choose the peer implementation that fits the project: [JPA](docs/i18n/en/implementations/jpa.md) or [MyBatis-Plus](docs/i18n/en/implementations/mybatis-plus.md).
- **Reliable messaging**: read [Reliable Messaging](docs/i18n/en/capabilities/reliable-messaging.md), then choose its JPA or MyBatis-Plus store from the corresponding [JPA](docs/i18n/en/implementations/jpa.md) or [MyBatis-Plus](docs/i18n/en/implementations/mybatis-plus.md) guide.
- **Spring Boot**: use [Spring Boot Runtime Assembly](docs/i18n/en/implementations/spring-boot.md) to assemble the selected capabilities.
- **Quarkus**: use [Quarkus Runtime Integration](docs/i18n/en/implementations/quarkus.md) for CDI transactions, domain-event dispatch, and Native Image verification.
- **Starters, properties, and conditions**: use [Spring Boot Auto-configuration](docs/i18n/en/reference/spring-boot-autoconfiguration.md) as the reference.

## Minimal Setup

Import the runtime-neutral BOM, then add only the starters and capability implementations required by the application. Spring Boot and Quarkus applications can instead use their corresponding runtime BOMs described in the runtime assembly guides.

```xml
<dependencyManagement>
    <dependencies>
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

## Domain Model Example

```java
// Money.java
import org.jfoundry.domain.valueobject.ValueObject;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements ValueObject {
}
```

```java
// OrderId.java
import org.jmolecules.ddd.types.Identifier;

public record OrderId(String value) implements Identifier {
}
```

```java
// Order.java
import org.jfoundry.domain.entity.agg.BaseAggregateRoot;

public final class Order extends BaseAggregateRoot<Order, OrderId> {

    private Money total;

    public Order(OrderId id, Money total) {
        super(id);
        this.total = total;
    }

    public void changeTotal(Money total) {
        this.total = total;
    }
}
```

## Documentation

### Getting Started

- [Getting Started](docs/i18n/en/integration/getting-started.md)
- [Adoption Readiness and Validated Scope](docs/i18n/en/integration/adoption-readiness.md)

### Capabilities

- [Aggregate Persistence](docs/i18n/en/capabilities/aggregate-persistence.md)
- [Reliable Messaging: Outbox And Inbox](docs/i18n/en/capabilities/reliable-messaging.md)
- [Application Transactions](docs/i18n/en/capabilities/application-transactions.md)
- [Distributed Locks](docs/i18n/en/capabilities/distributed-locks.md)

### Implementations

- [JPA](docs/i18n/en/implementations/jpa.md)
- [MyBatis-Plus](docs/i18n/en/implementations/mybatis-plus.md)
- [Spring Boot Runtime Assembly](docs/i18n/en/implementations/spring-boot.md)
- [Quarkus Runtime Integration](docs/i18n/en/implementations/quarkus.md)

### Reference

- [Spring Boot Auto-configuration](docs/i18n/en/reference/spring-boot-autoconfiguration.md)

### Framework Semantics

- [Architecture Styles](docs/i18n/en/framework/architecture-styles.md)
- [ArchUnit Architecture Rules](docs/i18n/en/framework/archunit-rules.md)
- [Framework Boundaries](docs/i18n/en/framework/framework-boundaries.md)

### Modeling

- [Value Object Guide](docs/i18n/en/modeling/value-object.md)
- [Repository and Read-side Contracts](docs/i18n/en/modeling/repository-vs-read-contracts.md)

### Release and Compatibility

- [Compatibility Matrix](docs/release/compatibility.md)
- [Maven Central Publishing](docs/release/maven-central.md)

For the complete documentation structure, see the [Documentation Index](docs/i18n/en/index.md).

## Build

```bash
mvn validate
mvn test
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
