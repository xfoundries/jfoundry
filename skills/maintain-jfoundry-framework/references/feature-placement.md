# Feature Placement

Use this file before adding modules, classes, annotations, rules, adapters, starters, or docs.

## Placement Map

| Change | Location |
|---|---|
| Domain building block, entity/value object/event abstraction | `jfoundry-domain` |
| Architecture facade annotation | `jfoundry-architecture/jfoundry-hexagonal` or `jfoundry-architecture/jfoundry-onion` |
| CQRS annotation or dispatcher contract | `jfoundry-architecture/jfoundry-cqrs` |
| Reusable ArchUnit rule or test helper | `jfoundry-architecture/jfoundry-architecture-test` |
| Application service marker or application-layer contract | `jfoundry-application/jfoundry-application-core` |
| Domain event dispatch contract | `jfoundry-application/jfoundry-event-core` |
| Event externalization metadata or routing rules | `jfoundry-application/jfoundry-event-externalization-core` |
| Message sending or payload serialization SPI | `jfoundry-application/jfoundry-messaging-core` |
| Outbox state, store contract, dispatcher service, retry/backoff core | `jfoundry-application/jfoundry-outbox-core` |
| Inbox state, store contract, `InboxTemplate` | `jfoundry-application/jfoundry-inbox-core` |
| MyBatis-Plus business persistence adapter | `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus` |
| MyBatis-Plus Outbox/Inbox store adapter | `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus` or `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus` |
| Broker `MessageSender` adapter | `jfoundry-infrastructure/jfoundry-messaging-<broker>` |
| Payload serializer adapter | `jfoundry-infrastructure/jfoundry-messaging-jackson` |
| Pure JobRunr dispatcher adapter | `jfoundry-infrastructure/jfoundry-outbox-jobrunr` |
| Spring Framework local event adapter | `jfoundry-spring/jfoundry-spring-runtime/jfoundry-event-spring` |
| Spring default messaging adapter | `jfoundry-spring/jfoundry-spring-runtime/jfoundry-messaging-spring` |
| Spring Outbox transaction/scheduling adapter | `jfoundry-spring/jfoundry-spring-runtime/jfoundry-outbox-spring` |
| Spring Boot conditions/properties/wiring | `jfoundry-spring/jfoundry-spring-boot-autoconfigure` |
| User dependency entry point | `jfoundry-starters` or `jfoundry-spring/jfoundry-spring-boot-starters` |
| Middleware integration verification | `jfoundry-verification/jfoundry-middleware-integration-tests` |

## Decision Rules

- If the code defines an abstraction used by multiple runtimes, keep it framework-neutral.
- If the code uses Spring transaction synchronization, `ApplicationEventPublisher`, scheduling, bean lifecycle, or `@ConfigurationProperties`, put it under `jfoundry-spring`.
- If the code registers Spring Boot beans conditionally, put it in `jfoundry-spring-boot-autoconfigure`.
- If the code only selects dependencies for users, put it in a starter POM.
- If the code talks to a concrete database, ORM, broker, serializer, or scheduler but does not require Spring Boot wiring, put it in `jfoundry-infrastructure`.
- If a change helps only jfoundry's own middleware verification, put it in `jfoundry-verification`, not in public modules.

## Public API Discipline

Public API Javadoc must be English. Keep Javadoc concise and focused on intent.

When changing public types:

- preserve binary/source compatibility when practical;
- document behavioral changes;
- add tests for new contracts;
- consider whether starter dependencies or docs need updates;
- call out migration impact in the final response.

## Documentation Placement

- Business-facing feature docs go under `docs/`.
- Release and compatibility docs go under `docs/release/`.
- Framework maintainer rules may be summarized in this skill and should reference the docs rather than duplicate long explanations.
- Do not create scattered ad hoc notes when an existing doc has the same topic.

