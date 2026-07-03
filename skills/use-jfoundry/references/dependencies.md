# Dependency Guidance

## BOM Choice

Choose the BOM by runtime, not by architecture style:

- Use `jfoundry-dependencies` for framework-neutral projects that need DDD, architecture annotations, application contracts, SPI, and non-Spring adapters.
- Use `jfoundry-spring-dependencies` only when the project uses Spring Framework adapters, Spring Boot auto-configuration, or Spring Boot starters. It imports `jfoundry-dependencies` and adds Spring-related dependency management.

Copy `assets/templates/maven/dependency-management-core.xml` or `assets/templates/maven/dependency-management-spring.xml` and replace `JFOUNDRY_VERSION`.

## Starter Selection

Choose starters by Maven module or layer. In a serious DDD project, prefer multi-module Maven because dependency boundaries become physical build boundaries. A small project may start as a single application module, but it must still preserve package boundaries and ArchUnit tests.

| Module / layer | Add | Never add |
|---|---|---|
| `domain` | `jfoundry-domain-starter` | Spring, MyBatis-Plus, JPA, MQ clients, HTTP clients, Spring Boot starters |
| `application` | `jfoundry-application-starter` | Spring Boot starters, MyBatis mappers/services, broker adapters |
| `infrastructure` | `jfoundry-infrastructure-mybatis-plus-starter` only for framework-neutral MyBatis-Plus adapters | controllers, application entry points, Spring Boot auto-configuration starters |
| `boot` / runtime assembly | `jfoundry-spring-boot-starter` and selected runtime feature starters | domain model or business rule implementation |
| architecture tests | `jfoundry-architecture-test` with `test` scope | production scope |

Use only the capabilities the module actually needs:

- Domain module: `jfoundry-domain-starter`
- Application module: `jfoundry-application-starter`
- Infrastructure module with MyBatis-Plus repositories: `jfoundry-infrastructure-mybatis-plus-starter`
- Spring Boot boot/runtime module: `jfoundry-spring-boot-starter`
- Spring Boot boot/runtime module with MyBatis-Plus business persistence: `jfoundry-mybatis-plus-spring-boot-starter`
- Local Spring domain event dispatch: `jfoundry-event-spring-boot-starter`
- Messaging transport contracts and default logging sender: `jfoundry-messaging-spring-boot-starter`
- Kafka sender adapter: `jfoundry-messaging-kafka-spring-boot-starter`
- RabbitMQ sender adapter: `jfoundry-messaging-rabbitmq-spring-boot-starter`
- RocketMQ sender adapter: `jfoundry-messaging-rocketmq-spring-boot-starter`
- Outbox core + Spring transaction/scheduling integration: `jfoundry-outbox-spring-boot-starter`
- Outbox MyBatis-Plus store: `jfoundry-outbox-mybatis-plus-spring-boot-starter`
- Outbox JobRunr dispatcher: `jfoundry-outbox-jobrunr-spring-boot-starter`
- Inbox core + `InboxTemplate`: `jfoundry-inbox-spring-boot-starter`
- Inbox MyBatis-Plus store: `jfoundry-inbox-mybatis-plus-spring-boot-starter`
- Architecture tests: `jfoundry-architecture-test` with `test` scope

## Template Mapping

- Use `dependency-management-core.xml` unless the project selects Spring Framework or Spring Boot.
- Use `dependency-management-spring.xml` when any selected starter is Spring-specific.
- Use `domain-module-dependencies.xml` for a dedicated domain module.
- Use `application-module-dependencies.xml` for a dedicated application module.
- Use `infrastructure-mybatis-plus-dependencies.xml` for a dedicated infrastructure module using MyBatis-Plus.
- Use `architecture-test-dependencies.xml` in the module that runs ArchUnit tests, usually the boot module test source set or a dedicated architecture-test module.
- Use `spring-boot-app-dependencies.xml` for a Spring Boot app or boot/runtime assembly module.
- Use `spring-boot-mybatis-plus-dependencies.xml` in the boot/runtime assembly module only when the application uses MyBatis-Plus for business persistence.
- Use `outbox-dependencies.xml` only when reliable external publication is required.
- Use `outbox-mybatis-plus-dependencies.xml` only when Outbox uses the MyBatis-Plus store.
- Use `inbox-dependencies.xml` only when consumer idempotency is required.
- Use `inbox-mybatis-plus-dependencies.xml` only when Inbox uses the MyBatis-Plus store.
- Use `broker-dependencies.xml` only when selecting a real broker adapter. Pick one broker starter unless the application truly publishes to multiple brokers.

## Avoid

- Do not add Outbox/Inbox starters by default.
- Do not assume MyBatis-Plus is present just because the project uses Spring Boot.
- Do not depend directly on low-level adapter modules from business code unless the project is doing an advanced custom assembly.
- Do not put Spring Boot starters into pure domain or application modules.
