# Reliable Messaging: Outbox And Inbox

Use Transactional Outbox only when a domain event must reach another process or external system
reliably. In-process event handling does not require it. Inbox provides consumer-side idempotency
for a message and consumer combination.

![transactional-outbox.png](../../assets/outbox/transactional-outbox.png)

## Event Flow

```text
aggregate records domain event
  -> application service boundary drains events
  -> externalization selects topic, key, and payload
  -> Outbox row is written in the same database transaction
  -> dispatcher claims and sends through MessageSender
  -> consumer uses InboxTemplate for idempotency
```

`DefaultDomainEventOutboxRecorder` records only events marked `@Externalized`; `@MessageRouting`
supplies routing information but does not externalize an event by itself. Use automatic
externalization only for a deliberately stable public event contract. Otherwise translate at the
application boundary and append the versioned integration contract explicitly with `OutboxTemplate`.
The template participates in the caller's transaction; it neither starts a transaction nor sends
synchronously.

## Payload Contract

Treat `payloadType` as a stable contract name rather than a Java class name. Consumers should
deserialize the envelope into their own versioned contract. The default Jackson serializer writes
portable JSON with ISO-8601 time values, ordinary JSON numbers, and no default-typing metadata or
Java class names. Provide `PayloadSerializer` when another wire format is needed.

## Outbox State Machine

- `PENDING`: written and waiting for dispatch.
- `DISPATCHING`: claimed by a dispatcher.
- `PUBLISHED`: sent successfully.
- `FAILED`: this attempt failed and the message awaits retry.
- `DEAD_LETTERED`: retry limits were exceeded.

Recovery returns stuck `DISPATCHING` messages to `PENDING`. Cleanup deletes expired terminal
records only. The dispatch trigger is `scheduled`, `jobrunr`, or `none`; `none` registers no
dispatcher, recovery job, or cleanup job.

## SQL Templates

SQL is supplied only as a copyable template and is never run by jfoundry. `jfoundry-outbox-core`
owns the canonical Outbox paths and `jfoundry-inbox-core` owns the canonical Inbox path. Copy the
needed template into the application's migration process:

```text
jfoundry/sql/outbox/mysql/create_outbox_event.sql
jfoundry/sql/outbox/postgresql/create_outbox_event.sql
jfoundry/sql/inbox/common/create_inbox_message.sql
```

## Choose A Store

| Need | Guide |
|------|-------|
| MyBatis-Plus Outbox and Inbox stores | [MyBatis-Plus](../implementations/mybatis-plus.md) |
| JPA Outbox and Inbox stores, including database-specific Inbox claiming | [JPA](../implementations/jpa.md) |
| Spring Boot capability assembly and dispatcher configuration | [Spring Boot](../implementations/spring-boot.md) |

Use [Spring Boot Auto-configuration](../reference/spring-boot-autoconfiguration.md) as the lookup
for starters, properties, and registration conditions.
