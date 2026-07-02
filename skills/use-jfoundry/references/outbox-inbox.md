# Outbox And Inbox Guidance

## When To Use Outbox

Use Outbox only when domain events must be reliably published outside the process:

- Kafka, RabbitMQ, RocketMQ, or another broker.
- Cross-service notification.
- Eventually consistent integration.
- Failure retry and dead-letter handling.

If events only need in-process Spring listeners, use local event dispatch and do not configure Outbox.

## Event Flow

The intended flow is:

```text
@ApplicationService
  -> DomainEventContext
  -> DomainEventDispatcher
  -> DomainEventOutboxRecorder
  -> OutboxMessageStore
  -> jfoundry_outbox_event
  -> OutboxDispatcher
  -> MessageSender
  -> broker / external system
```

Mark only externally published events with `@Externalized`. Use `@MessageRouting` when topic or routing key must be explicit. `@MessageRouting` alone does not make an event externalized.

## Starter Selection

For Outbox with MyBatis-Plus storage:

- Add `jfoundry-outbox-spring-boot-starter`.
- Add `jfoundry-outbox-mybatis-plus-spring-boot-starter`.
- Add one real broker starter, such as `jfoundry-messaging-kafka-spring-boot-starter`, when production dispatch is enabled.

For JobRunr dispatching, add `jfoundry-outbox-jobrunr-spring-boot-starter` and set dispatcher mode accordingly.

## MessageSender Rule

Outbox does not imply a broker. The default logging sender is not a production publisher. If production Outbox dispatch is enabled, provide a real `MessageSender` through one broker starter or a custom adapter.

## Inbox

Use Inbox when a consumer must be idempotent under duplicate delivery or retry:

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

For MyBatis-Plus storage, add `jfoundry-inbox-spring-boot-starter` and `jfoundry-inbox-mybatis-plus-spring-boot-starter`.

## Operational Notes

- Outbox messages move through `PENDING`, `DISPATCHING`, `PUBLISHED`, `FAILED`, and `DEAD_LETTERED`.
- Dispatchers use atomic claim to avoid duplicate dispatch across instances.
- Recovery moves stuck `DISPATCHING` messages back to retryable state.
- Cleanup should only remove terminal records after the configured retention period.
- Consumers still need idempotency because brokers and dispatchers can retry.

