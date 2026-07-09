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

## Configuration Semantics

Add `jfoundry-outbox-spring-boot-starter` when reliable externalization is needed. Add
`jfoundry-outbox-mybatis-plus-spring-boot-starter` when the application uses the built-in
MyBatis-Plus Outbox store.

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

## SQL Templates

jfoundry ships SQL as copyable templates, not auto-run migrations. Applications should copy the
needed template into their own Flyway/Liquibase migration directory or execute it through their
operational process.

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
