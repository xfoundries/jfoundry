# Transactional Outbox Completion Design

## Goal

Complete jfoundry's Transactional Outbox capability from a framework-level polling outbox into a production-usable reliable messaging foundation:

- keep outbox core broker-neutral;
- support business aggregate ordering metadata;
- provide a first real broker adapter for Kafka;
- provide consumer-side idempotency through an Inbox template;
- fix the outbox auto-configuration package boundary and table-name override reliability.

This design intentionally does not implement transaction-log tailing or CDC. CDC can be added later as a Debezium-oriented deployment option or adapter, but it is heavier operationally than the current polling relay.

## Current State

jfoundry already has the core sender-side outbox chain:

```text
DomainEventPublisher
  -> DomainEventExternalizer
  -> OutboxRepository
  -> ddd_outbox_event
  -> ScheduledOutboxDispatcher / JobRunrOutboxDispatcher
  -> MessageSender
```

The current implementation supports transactional event externalization, claim/send/mark dispatch, retry, dead-lettering, stuck-dispatch recovery, cleanup, MyBatis-Plus persistence, and Spring Boot auto-configuration.

The remaining gaps are:

- `JfoundryOutboxProperties` and `OutboxMybatisPlusAutoConfiguration` live under `org.jfoundry.autoconfigure.persistence`, even though their domain is outbox storage.
- `jfoundry.outbox.table-name` works only when jfoundry creates the default `MybatisPlusInterceptor`; a user-provided interceptor can bypass the dynamic table-name handler.
- There is no real broker `MessageSender`; the default sender only logs.
- There is no framework-provided consumer idempotency support.
- There is no aggregate metadata for broker-neutral ordering semantics.

## Design Principles

Outbox remains broker-neutral. No Kafka, partition, producer record, or Kafka header type appears in outbox core.

Messaging adapters map broker-neutral message metadata to broker-specific constructs. Kafka is the first adapter, not a special case in outbox.

Inbox is a separate consumer-side reliability capability. It is related to outbox, but it is not part of outbox core.

Spring Boot auto-configuration belongs in `jfoundry-spring/jfoundry-autoconfigure`; technology adapters must not register their own `AutoConfiguration.imports`.

The first implementation should be practical and minimal. It should avoid Kafka transactions, CDC, listener interception, and annotation-based consumer magic.

## Module Changes

### Existing Modules

`jfoundry-outbox-core`

- Add aggregate metadata to `OutboxEntry`.
- Keep `OutboxRepository`, dispatch service, status machine, and retry behavior broker-neutral.

`jfoundry-outbox-mybatis-plus`

- Add persistence fields for aggregate metadata.
- Add migration scripts for the new columns.
- Keep using `OutboxData` as a MyBatis-Plus persistence view.

`jfoundry-autoconfigure`

- Move outbox-specific auto-configuration classes from `org.jfoundry.autoconfigure.persistence` and `org.jfoundry.autoconfigure.dispatcher` to outbox-scoped packages.
- Keep generic persistence concerns in `org.jfoundry.autoconfigure.persistence`.
- Ensure outbox table-name rewriting is applied even when the user provides a `MybatisPlusInterceptor`.

### New Modules

`jfoundry-messaging-kafka`

- Implements `MessageSender` using Spring Kafka's `KafkaTemplate<String, String>`.
- Contains no outbox repository or dispatcher logic.

`jfoundry-inbox-core`

- Defines consumer idempotency abstractions:
  - `InboxRepository`
  - `InboxTemplate`
  - `InboxMessageStatus`
  - `InboxHandler`

`jfoundry-inbox-mybatis-plus`

- Provides MyBatis-Plus persistence for processed messages.
- Owns the `jfoundry_inbox_message` table schema and mapper.

## Package Layout

Target auto-configuration package layout:

```text
org.jfoundry.autoconfigure.outbox
  JfoundryOutboxProperties

org.jfoundry.autoconfigure.outbox.persistence
  OutboxMybatisPlusAutoConfiguration
  OutboxTableNameCustomizer

org.jfoundry.autoconfigure.outbox.dispatcher
  OutboxDispatcherAutoConfiguration
  JobRunrDispatcherAutoConfiguration
  OutboxRecoveryJob
  OutboxRecoveryProperties
  OutboxCleanupJob
  OutboxCleanupProperties

org.jfoundry.autoconfigure.messaging
  DomainEventPublisherAutoConfiguration
  DomainEventExternalizerAutoConfiguration
  MessageSenderAutoConfiguration

org.jfoundry.autoconfigure.messaging.kafka
  KafkaMessageSenderAutoConfiguration
  KafkaMessageSenderProperties

org.jfoundry.autoconfigure.inbox
  InboxAutoConfiguration
  InboxMybatisPlusAutoConfiguration
```

`JfoundryPersistenceProperties` and `DbTypeResolver` stay in `org.jfoundry.autoconfigure.persistence` because they are persistence-generic.

## Outbox Table Name Override

The public configuration remains:

```yaml
jfoundry:
  outbox:
    table-name: jfoundry_outbox_event
```

The implementation must support both cases:

- jfoundry creates the default `MybatisPlusInterceptor`;
- the application provides its own `MybatisPlusInterceptor`.

The dynamic table-name interceptor must rewrite only the outbox logical table. It must not affect business tables.

The logical table remains the table name declared on `OutboxData`. If the project decides to change the default physical name from `ddd_outbox_event` to `jfoundry_outbox_event`, that should be treated as a compatibility decision and documented as a migration. The dynamic rewrite mechanism should still work either way.

## Aggregate Ordering Metadata

Add these fields to `OutboxEntry` and persistence schema:

```text
aggregate_type      VARCHAR(255) NULL
aggregate_id        VARCHAR(255) NULL
aggregate_version   BIGINT NULL
```

Semantics:

- `aggregate_type`: stable aggregate type name, for example `Order`.
- `aggregate_id`: aggregate identifier, for example an order id.
- `aggregate_version`: business ordering version within the aggregate.

These fields are optional. Events without aggregate metadata remain valid.

`DomainEventExternalizer` should resolve aggregate metadata through a broker-neutral mechanism. The first implementation should use an annotation rather than Kafka-specific concepts.

Proposed annotation:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AggregateRouting {
    String type() default "";
    String id();
    String version() default "";
}
```

Rules:

- `id` and `version` are property paths evaluated against the event object, using the same simple property-path style as `@MessageRouting.key`.
- `type` defaults to the simple event or aggregate name if empty.
- If `aggregate_id` is present and `payload_key` is empty, the externalizer may use `aggregate_id` as the payload key.
- The dispatcher still orders batches by existing dispatchable ordering. This design does not enforce one-at-a-time DB-level aggregate locks.

Ordering guarantee:

- jfoundry records aggregate ordering metadata.
- Kafka adapter maps `payload_key` or `aggregate_id` to Kafka key.
- Kafka can preserve order for messages with the same key within a partition.
- Strict DB-level per-aggregate serialization is not in scope for this version.

## Kafka Message Sender

`jfoundry-messaging-kafka` provides:

```java
public final class KafkaMessageSender implements MessageSender {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaMessageSenderProperties properties;

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        ...
    }
}
```

Behavior:

- Send to `topic`.
- Use `payloadKey` as Kafka key.
- Wait for send completion by default, so the outbox dispatcher can mark success or failure deterministically.
- Return `SendResult.ok()` on successful send.
- Return `SendResult.fail(message)` on send failure.

Configuration:

```yaml
jfoundry:
  messaging:
    kafka:
      enabled: true
      send-timeout: 10s
```

Auto-configuration:

- active only when Spring Kafka's `KafkaTemplate` is on the classpath;
- active only when a `KafkaTemplate<String, String>` bean exists;
- backs off when the application provides a `MessageSender`;
- does not require outbox to be enabled, because `MessageSender` is a messaging SPI.

Headers:

The current `MessageSender` SPI only accepts topic, key, and payload. For this version, Kafka sender should remain compatible with that SPI and not require headers. A later SPI extension may add `MessageEnvelope` if event id and aggregate metadata need to be sent as broker headers.

## Inbox Idempotency

`jfoundry-inbox-core` provides consumer-side idempotency:

```text
InboxTemplate.executeOnce(messageId, consumerName, handler)
```

Desired semantics:

- If `(consumer_name, message_id)` is already successfully processed, skip the handler.
- If it is not processed, execute the handler.
- If the handler succeeds, record the message as processed.
- If the handler throws, do not mark it as processed; rethrow so the broker can retry.

The first version should not provide annotation-based listener interception. Business consumers call the template explicitly:

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    projectionHandler.handle(event);
});
```

### Inbox Schema

Default table:

```text
jfoundry_inbox_message
```

Columns:

```text
message_id      VARCHAR(128)  NOT NULL
consumer_name   VARCHAR(255)  NOT NULL
status          VARCHAR(32)   NOT NULL
processed_at    TIMESTAMP     NULL
created_at      TIMESTAMP     NOT NULL
updated_at      TIMESTAMP     NOT NULL
error_message   VARCHAR(2000) NULL
PRIMARY KEY (consumer_name, message_id)
```

Statuses:

```text
PROCESSED
```

Only `PROCESSED` is required for the first version. `PROCESSING` is intentionally omitted to avoid stuck-lock recovery complexity. If two consumers race on the same message, the unique key decides; one succeeds, the other treats the duplicate as already processed.

## Data Flow

### Sending

```text
Application service saves aggregate
  -> publishes domain event
  -> DomainEventExternalizer serializes event
  -> resolves topic, key, aggregate metadata
  -> appends OutboxEntry in same transaction
  -> dispatcher claims dispatchable entries
  -> KafkaMessageSender sends to Kafka
  -> repository marks PUBLISHED or FAILED/DEAD_LETTERED
```

### Consuming

```text
Kafka listener receives message
  -> extracts event id as messageId
  -> calls InboxTemplate.executeOnce(messageId, consumerName, handler)
  -> handler updates local read model or invokes application use case
  -> InboxTemplate records PROCESSED after handler success
```

## Error Handling

Outbox send failures:

- Kafka send exceptions become `SendResult.fail(...)`.
- Dispatcher uses existing retry and dead-letter behavior.

Inbox handler failures:

- `InboxTemplate` rethrows handler exceptions.
- No `PROCESSED` record is written.
- Broker retry remains responsible for redelivery.

Duplicate inbox messages:

- If a `PROCESSED` record already exists, the handler is skipped.
- Duplicate insert races are handled by repository-level unique-key behavior.

Missing aggregate metadata:

- The event is still valid.
- Kafka key falls back to existing `payloadKey`.
- If neither `payloadKey` nor `aggregate_id` exists, the Kafka key is null.

## Documentation

Update `docs/transactional-outbox.md`:

- explain that jfoundry implements the polling publisher variant;
- document aggregate metadata and ordering limits;
- document Kafka sender setup;
- document consumer idempotency with `InboxTemplate`;
- explicitly state that CDC / transaction-log tailing is not part of the default runtime.

Update `README.md`:

- add `jfoundry-messaging-kafka`;
- add `jfoundry-inbox-core` and `jfoundry-inbox-mybatis-plus`;
- clarify reliable sender-side and consumer-side idempotency boundaries.

## Testing Strategy

Outbox metadata:

- unit test event metadata extraction;
- persistence test maps aggregate fields between `OutboxEntry` and `OutboxData`;
- migration/schema test inserts and reads aggregate metadata.

Table-name override:

- test custom table works when jfoundry creates `MybatisPlusInterceptor`;
- test custom table works when application provides `MybatisPlusInterceptor`.

Kafka sender:

- unit test `KafkaMessageSender` returns success when `KafkaTemplate.send(...).get(...)` succeeds;
- unit test returns failure when send throws;
- auto-config test backs off when user provides `MessageSender`;
- auto-config test creates sender when `KafkaTemplate` exists.

Inbox:

- unit test `InboxTemplate` skips already processed messages;
- unit test handler success records `PROCESSED`;
- unit test handler failure does not record `PROCESSED`;
- MyBatis-Plus integration test enforces unique `(consumer_name, message_id)`;
- auto-config test creates repository and template when inbox persistence is present.

## Out of Scope

- Debezium / transaction-log tailing implementation.
- Kafka transactions.
- Annotation-driven listener interception.
- Strict DB-level per-aggregate serialization during claim.
- RocketMQ, RabbitMQ, or other broker adapters.
- Inbox stuck `PROCESSING` recovery.

## Open Compatibility Notes

The project currently has an uncommitted local change that renames `OutboxData.@TableName` from `ddd_outbox_event` to `jfoundry_outbox_event`. This design does not assume that change is final. If the default table name changes, the migration path and default `jfoundry.outbox.table-name` value must be explicitly documented and tested.
