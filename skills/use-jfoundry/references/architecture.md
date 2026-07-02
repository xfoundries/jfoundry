# Architecture Guidance

## Style Selection

Prefer Hexagonal Architecture for new business projects because it gives AI agents clear package and dependency boundaries:

```text
primary adapter -> primary port / application service -> domain
application service -> secondary port
secondary adapter -> secondary port
```

Use Onion Simple when the team explicitly wants ring terminology:

```text
infrastructure -> application -> domain
```

Do not mix Hexagonal and Onion annotations in the same ArchUnit analysis scope. If a project already chose a style, preserve it.

## Hexagonal Roles

- `@Application`: application core. It can include use cases, application services, and domain-facing orchestration.
- `@PrimaryPort`: inbound API contract exposed by the application core. It should be an interface and live under `port.in`, `ports.in`, `port.inbound`, `ports.inbound`, `usecase`, or `usecases`.
- `@PrimaryAdapter`: inbound driver such as REST controller, message listener, CLI command, scheduler, or batch trigger.
- `@SecondaryPort`: outbound need expressed by the application core. It should be an interface and live under `port.out`, `ports.out`, `port.outbound`, or `ports.outbound`.
- `@SecondaryAdapter`: implementation of a secondary port, such as MyBatis/JPA persistence, Redis, HTTP clients, broker senders, file storage, or external SDK adapters.

## Package Defaults

For new Hexagonal projects, copy `assets/templates/structure/hexagonal-package-structure.txt`.

Recommended package shape:

```text
PACKAGE_NAME
  domain
  application
    port.in
    port.out
    service
  adapter.in
  adapter.out
  infrastructure
  boot
```

Keep `domain` and `application` free of MyBatis, Spring MVC, broker clients, and persistence models. Primary adapters call application services or primary ports. Secondary adapters implement secondary ports.

## Annotation Placement

Prefer package-level annotations when an entire package has one role. Use class-level annotations for incremental migration or mixed packages.

For Hexagonal, add `package-info.java` files in role packages where useful:

```java
@org.jfoundry.architecture.hexagonal.Application
package PACKAGE_NAME.application;
```

```java
@org.jfoundry.architecture.hexagonal.PrimaryPort
package PACKAGE_NAME.application.port.in;
```

```java
@org.jfoundry.architecture.hexagonal.SecondaryPort
package PACKAGE_NAME.application.port.out;
```

```java
@org.jfoundry.architecture.hexagonal.PrimaryAdapter
package PACKAGE_NAME.adapter.in;
```

```java
@org.jfoundry.architecture.hexagonal.SecondaryAdapter
package PACKAGE_NAME.adapter.out;
```

## Do Not

- Do not put controllers, schedulers, or message listeners in the application package.
- Do not let controllers call repositories, mappers, secondary ports, or secondary adapters directly.
- Do not put MyBatis `Mapper`, `Wrapper`, `Page`, `IPage`, Spring Data repositories, or JPA specifications in domain/application signatures.
- Do not create one interface for every class. Create ports for real boundaries and outbound needs.
- Do not enable CQRS only for symmetry. Use it when command and query models actually diverge.

