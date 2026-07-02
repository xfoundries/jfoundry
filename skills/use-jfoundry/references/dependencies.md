# Dependency Guidance

## BOM Choice

For Spring Boot business applications, prefer `jfoundry-spring-dependencies`; it imports the framework-neutral jfoundry BOM and Spring-related dependency BOMs.

For non-Spring projects, use `jfoundry-dependencies`.

Copy `assets/templates/maven/dependency-management.xml` and replace `JFOUNDRY_VERSION`.

## Default Starter Selection

Use only the capabilities the project needs:

- Domain module: `jfoundry-domain-starter`
- Application module: `jfoundry-application-starter`
- Infrastructure module with MyBatis-Plus repositories: `jfoundry-infrastructure-mybatis-plus-starter`
- Minimal Spring Boot runtime: `jfoundry-spring-boot-starter`
- Spring Boot app with MyBatis-Plus business persistence: `jfoundry-mybatis-plus-spring-boot-starter`
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

- Use `domain-module-dependencies.xml` for a dedicated domain module.
- Use `application-module-dependencies.xml` for a dedicated application module.
- Use `infrastructure-mybatis-plus-dependencies.xml` for a dedicated infrastructure module using MyBatis-Plus.
- Use `spring-boot-app-dependencies.xml` for a single Spring Boot app or boot assembly module.
- Use `spring-boot-mybatis-plus-dependencies.xml` in addition to the Spring Boot app template only when the app uses MyBatis-Plus for business persistence.
- Use `outbox-inbox-dependencies.xml` only when reliable external publication or consumer idempotency is required.
- Use `broker-dependencies.xml` only when selecting a real broker adapter. Pick one broker starter unless the application truly publishes to multiple brokers.

## Avoid

- Do not add Outbox/Inbox starters by default.
- Do not assume MyBatis-Plus is present just because the project uses Spring Boot.
- Do not depend directly on low-level adapter modules from business code unless the project is doing an advanced custom assembly.
- Do not put Spring Boot starters into pure domain or application modules.
