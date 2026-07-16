# Transactional Outbox and Inbox

Transactional Outbox is used only when domain events must be delivered reliably to another process
or external system. If an event only needs in-process handling, do not enable Outbox.

![transactional-outbox.png](../../assets/outbox/transactional-outbox.png)

## Event Flow

```text
aggregate records domain event
  -> application service boundary drains events
  -> externalization rule selects topic/key/payload
  -> Outbox row is written in the same database transaction
  -> dispatcher claims and sends through MessageSender
  -> consumer side uses InboxTemplate for message/consumer idempotency
```

`DefaultDomainEventOutboxRecorder` records only events marked with `@Externalized`. `@MessageRouting`
can provide a topic and routing key, but it does not make an event externalized by itself.

## Direct And Contract-Isolated Externalization

Use automatic externalization when the domain event is deliberately designed as a stable public
contract:

```text
@Externalized domain event -> DomainEventOutboxRecorder -> Outbox
```

Do not expose an internal domain event only to avoid writing a translation. When consumers need a
separate, versioned integration contract, translate at the application boundary and record the
result explicitly:

```java
ExpenseClaimApprovedV1 integrationEvent = translator.translate(domainEvent);
outboxTemplate.append(new OutboxAppendRequest(
        eventId,
        "expense-approval.events.v1",
        claimId,
        "ExpenseClaimApprovedV1",
        integrationEvent,
        occurredAt,
        "ExpenseClaim",
        claimId,
        aggregateVersion));
```

`OutboxTemplate` serializes the supplied payload and appends a pending message through the existing
`OutboxMessageStore`. It participates in the caller's transaction; it does not translate domain
events, start a transaction, or publish synchronously. The existing automatic externalization path
remains available and unchanged.

The default Jackson serializer emits portable JSON without Jackson default-typing metadata or Java
class names. Time values use ISO-8601 and values such as `BigDecimal` remain ordinary JSON numbers.
Use `payloadType` as a stable contract name, not a Java class name, and let each consumer deserialize
the envelope into its own versioned contract type. A custom `PayloadSerializer` remains available
when a project needs another wire format.

## Configuration Semantics

Add `jfoundry-outbox-spring-boot-starter` when reliable externalization is needed. It auto-configures
`OutboxTemplate` when an `OutboxMessageStore` and `PayloadSerializer` are available. Add
`jfoundry-outbox-mybatis-plus-spring-boot-starter` when the application uses the built-in
MyBatis-Plus Outbox store. The messaging starter transitively supplies Spring Boot's JSON starter,
so batch consumers and other non-web applications receive the default Jackson `ObjectMapper` and
`PayloadSerializer` without adding a WebMVC or WebFlux starter. A user-defined `ObjectMapper` or
`PayloadSerializer` still takes precedence.

```yaml
jfoundry:
  outbox:
    dispatcher:
      mode: scheduled
```

`jfoundry.outbox.dispatcher.mode` controls only the dispatch trigger:

- `scheduled`: uses Spring `@Scheduled` polling dispatch and enables recovery/cleanup by default.
- `jobrunr`: uses JobRunr to trigger dispatch; recovery and cleanup remain lightweight Spring
  `@Scheduled` maintenance jobs.
- `none`: registers no automatic dispatcher, recovery job, or cleanup job.

`jfoundry.outbox.recovery.enabled` and `jfoundry.outbox.cleanup.enabled` can disable the
maintenance jobs only in managed dispatch modes (`scheduled` or `jobrunr`).

When a Spring Boot application adds `jfoundry-messaging-kafka-spring-boot-starter`, jfoundry waits
for Boot's Kafka auto-configuration to create `KafkaOperations`, then registers the Kafka sender
before evaluating the logging fallback. Configure String key/value serializers because the sender
publishes the Outbox key and JSON body as strings. In a context or smoke test, assert that the
selected `MessageSender` is the broker sender rather than relying only on classpath presence.

## SQL Templates

jfoundry ships SQL as copyable templates, not auto-run migrations. Applications should copy the
needed template into their own Flyway/Liquibase migration directory or execute it through their
operational process.

Outbox templates are packaged in `jfoundry-outbox-core`; the common Inbox template is packaged in
`jfoundry-inbox-core`.

```text
jfoundry/sql/outbox/mysql/create_outbox_event.sql
jfoundry/sql/outbox/postgresql/create_outbox_event.sql
jfoundry/sql/inbox/common/create_inbox_message.sql
```

Inbox currently uses a portable template for MySQL and PostgreSQL. Proprietary or domestic database
DDL should be supplied by vendors, third-party integrations, or downstream applications.

## State Semantics

- `PENDING`: written and waiting for dispatch.
- `DISPATCHING`: claimed by a dispatcher instance.
- `PUBLISHED`: sent successfully.
- `FAILED`: failed this attempt and waiting for retry.
- `DEAD_LETTERED`: exceeded retry limits.

Recovery moves stuck `DISPATCHING` messages back to `PENDING`. Cleanup deletes expired terminal
states only.
