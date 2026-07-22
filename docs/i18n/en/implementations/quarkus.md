# Quarkus Runtime Integration

`jfoundry-quarkus-runtime` is a Quarkus extension that exposes the framework-neutral
`TransactionRunner` as a CDI bean. It keeps Quarkus, CDI, Jakarta Transactions, and GraalVM types
outside the domain, application, and infrastructure modules.

## Dependency Setup

Import the Quarkus BOM for the jfoundry release line, then add the runtime extension. The deployment
artifact is discovered by Quarkus from the runtime extension descriptor; applications must not add it
directly.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-quarkus-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-quarkus-runtime</artifactId>
    </dependency>
</dependencies>
```

The extension brings Quarkus Arc and Narayana JTA as runtime dependencies. It registers one
application-scoped `QuarkusTransactionRunner`, which can be injected through the framework-neutral
`TransactionRunner` contract.

## Transaction Semantics

The adapter maps all six `TransactionPropagation` values to Jakarta Transactions behavior:

| jfoundry propagation | Quarkus/Jakarta behavior |
|----------------------|--------------------------|
| `REQUIRED` | Joins an active transaction or starts one. |
| `REQUIRES_NEW` | Suspends an active transaction, starts a new transaction, then resumes it. |
| `SUPPORTS` | Joins an active transaction or runs without one. |
| `MANDATORY` | Requires an active transaction. |
| `NOT_SUPPORTED` | Suspends an active transaction and runs without one. |
| `NEVER` | Runs only when no transaction is active. |

Callback exceptions roll back an owned transaction. When the adapter joins an existing transaction,
callback exceptions mark that transaction rollback-only and preserve the original exception.

`TransactionOptions.timeout` maps to the Jakarta transaction timeout for the transaction started by
the adapter and restores the default afterwards. Jakarta Transactions has no portable transaction
name or read-only transaction setting, so this adapter rejects `TransactionOptions.name` and
`TransactionOptions.readOnly` rather than silently ignoring them.

## Domain Event Dispatch

The base runtime extension also provides the application-service event boundary. For every CDI bean
annotated with framework-neutral `@ApplicationService`, Quarkus adds a runtime-only interceptor
binding during augmentation. On the outermost successful invocation, the interceptor drains events
from aggregates registered through `DomainEventContext` and sends them to every CDI
`DomainEventDispatcher`. Nested application-service invocations share the same
scope, so dispatch occurs once at the outermost boundary. An exception escaping that boundary
discards its pending events.

```java
@ApplicationScoped
@ApplicationService
class ConfirmOrder {

    private final DomainEventContext domainEventContext;

    ConfirmOrder(DomainEventContext domainEventContext) {
        this.domainEventContext = domainEventContext;
    }

    void handle(Order order) {
        order.confirm();
        domainEventContext.register(order);
    }
}
```

The extension supplies the `DomainEventContext` used by this boundary. This assembly supports
synchronous application-service methods only; `CompletionStage` and Mutiny return types are rejected.
It provides in-process domain-event orchestration only and does not add an Outbox store, serializer,
broker client, or automatic event externalization.

## JPA Aggregate Persistence

To use `JpaAggregateRepository`, add `jfoundry-persistence-jpa` and the Quarkus Hibernate ORM and
datasource extensions selected by the application. A repository subclass must be a CDI bean and
receive `EntityManager` through its constructor. The jfoundry extension discovers CDI beans that
implement `AggregatePersistenceContextAware` and supplies a JTA transaction-bound persistence
context automatically. An application may replace that default by declaring its own CDI
`AggregatePersistenceContext` bean.

Keep `findById(...)`, domain behavior, and `modify(...)` in the same `TransactionRunner` callback.
Quarkus binds the injected `EntityManager` and aggregate persistence state to that transaction, so
the repository applies changes to the entity graph loaded in that same persistence context.

```java
transactionRunner.run(() -> {
    Order order = repository.findById(orderId);
    order.confirm();
    repository.modify(order);
});
```

This assembly covers business aggregate persistence only. Add the explicit JPA Outbox capability
described below when an application needs a JPA-backed Outbox store.

## JPA Outbox Storage

Add `jfoundry-outbox-jpa-quarkus-runtime` alongside the base runtime extension and Quarkus Hibernate
ORM:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-outbox-jpa-quarkus-runtime</artifactId>
</dependency>
```

The capability registers `JpaOutboxMessageEntity` with the default persistence unit and provides a
default CDI `OutboxMessageStore` backed by `JpaOutboxMessageStore`. An application can replace that
store by declaring its own CDI `OutboxMessageStore` bean. As with every jfoundry SQL template, the
application remains responsible for managing the `jfoundry_outbox_event` table through its migration
process.

This capability assembles persistence only. Add the explicit Outbox runtime assembly described
below for dispatching, payload serialization, or automatic domain-event externalization.

## Outbox Dispatching And Maintenance

Add `jfoundry-outbox-quarkus-runtime` when an application needs the shared Outbox claim, send, and
state-transition runtime:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-outbox-quarkus-runtime</artifactId>
</dependency>
```

The extension provides a default CDI `OutboxDispatcher` and uses the Quarkus Scheduler. It remains
inactive unless `jfoundry.outbox.dispatcher.enabled=true`. The application must provide both an
`OutboxMessageStore` (for example through `jfoundry-outbox-jpa-quarkus-runtime`) and a real
`MessageSender`; the dispatcher does not add a broker client or a logging sender. Configure
`jfoundry.outbox.dispatcher.interval` (default `5s`), `batch-size` (default `50`), `max-retries`
(default `5`), `backoff-base` (default `1s`), and `backoff-max` (default `5m`) as needed. An
application-provided CDI `OutboxDispatcher` takes precedence.

Message delivery remains outside database transactions. Each claim and state transition runs in an
independent transaction through `TransactionRunner`, consistent with the framework-neutral Outbox
contract.

The same extension also provides scheduled Outbox maintenance without requiring a `MessageSender`.
Recovery is disabled by default; enable it with `jfoundry.outbox.recovery.enabled=true` to reset
stale `DISPATCHING` records at `jfoundry.outbox.recovery.interval` (default `60s`) after
`jfoundry.outbox.recovery.stuck-timeout` (default `5m`). Cleanup is independently disabled by
default; enable it with `jfoundry.outbox.cleanup.enabled=true` to remove expired terminal records
at `jfoundry.outbox.cleanup.interval` (default `24h`). Its defaults retain `PUBLISHED` records for
seven days, `DEAD_LETTERED` records for 30 days, and delete at most 1000 records per status per
run. Configure `published-retention-days`, `dead-lettered-retention-days`, and `batch-size` under
`jfoundry.outbox.cleanup` when different operational limits are required.

Recovery and each terminal-status cleanup run use independent `REQUIRES_NEW` transaction boundaries.
Broker adapters and starters remain explicit capabilities.

## Kafka Message Delivery

Add `jfoundry-messaging-kafka-quarkus-runtime` to provide the default Quarkus Kafka implementation
of `MessageSender`:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-messaging-kafka-quarkus-runtime</artifactId>
</dependency>
```

The extension brings `quarkus-messaging-kafka` and sends through the fixed outgoing channel
`jfoundry-kafka`. Configure that channel with the SmallRye Kafka connector:

```properties
kafka.bootstrap.servers=localhost:9092
mp.messaging.outgoing.jfoundry-kafka.connector=smallrye-kafka
mp.messaging.outgoing.jfoundry-kafka.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.jfoundry-kafka.value.serializer=org.apache.kafka.common.serialization.StringSerializer
```

`MessageSender.send(topic, payloadKey, payload)` dynamically sets the Kafka topic and key for each
record, so `@Externalized` and `@AggregateRouting` continue to determine Outbox routing. The channel
name is infrastructure configuration, not a business destination. The adapter waits for broker
acknowledgement and maps failures to `SendResult`; configure
`jfoundry.messaging.kafka.send-timeout` to change its `10s` default. It is a Quarkus CDI default
bean, so an application can replace it with its own `MessageSender`.

## Automatic Domain-Event Externalization

`jfoundry-outbox-quarkus-runtime` also supplies an explicit automatic externalization assembly. It
adds Quarkus Jackson and produces replaceable defaults for `PayloadSerializer`,
`ExternalizationRuleResolver`, `AggregateRoutingResolver`, `OutboxTemplate`, and
`DomainEventOutboxRecorder`. It does not add an Outbox store or a broker client; add a store
capability such as `jfoundry-outbox-jpa-quarkus-runtime` separately.

Automatic recording is disabled by default. Enable it only when the domain event itself is a stable
integration contract:

```properties
jfoundry.domain.event.dispatch.outbox.enabled=true
```

Mark each intended integration event with `@Externalized("<topic>")`. Add `@AggregateRouting` when
the aggregate type, id, or version should be retained with the Outbox row; the resolved aggregate id
also becomes the default message key when no routing key is specified. Events without
`@Externalized` are not recorded. Applications can replace the default serializer or recorder with
their own CDI bean.

The enclosing transaction must cover the complete application-service invocation, including domain
event dispatch. For example, apply Jakarta `@Transactional` to the `@ApplicationService` method, or
invoke that method from an outer `TransactionRunner` callback. Starting and completing a
`TransactionRunner` callback only around the aggregate mutation inside the application service is
too narrow: the domain-event boundary records the Outbox entry after that callback has returned.

The extension registers `@Externalized` event classes for Jackson reflection during augmentation, so
the default serializer works in Native Image. It does not prescribe a broker transport; use an
explicit `MessageSender` adapter and enable the dispatcher separately when delivery is required.

## JPA Inbox Storage

Add `jfoundry-inbox-jpa-quarkus-runtime` alongside the base runtime extension and Quarkus Hibernate
ORM:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-inbox-jpa-quarkus-runtime</artifactId>
</dependency>
```

The capability registers `JpaInboxMessageEntity` with the default persistence unit. It provides
default CDI beans for `JpaInboxClaimStrategy`, `InboxMessageStore`, and `InboxTemplate`; the store
uses `JpaInboxMessageStore`, and the template uses the runtime's `TransactionRunner` for its claim,
processing, and failure boundaries. The built-in claim strategy is selected from the datasource
product and supports PostgreSQL and MySQL only. For another database, declare a CDI
`JpaInboxClaimStrategy` bean. An application may also replace `InboxMessageStore` or
`InboxTemplate` with its own CDI bean.

The application remains responsible for copying the Inbox SQL template into its migration process
and maintaining the `jfoundry_inbox_message` table. This capability assembles persistence only. It
does not provide a dispatcher, scheduler, serializer, automatic event externalization, or a starter.

## Native Image Verification

The repository's Quarkus native CI job first installs the extension artifacts and then builds a
separate consumer application. Its `@QuarkusIntegrationTest` invokes `TransactionRunner`, domain-event
dispatch, Outbox dispatch, recovery, and cleanup through HTTP endpoints against the native executable.

Run the same verification on a machine with GraalVM Native Image:

```bash
./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-runtime,jfoundry-quarkus/jfoundry-quarkus-deployment,jfoundry-quarkus/jfoundry-outbox-quarkus-runtime,jfoundry-quarkus/jfoundry-outbox-quarkus-deployment,jfoundry-quarkus/jfoundry-messaging-kafka-quarkus-runtime,jfoundry-quarkus/jfoundry-messaging-kafka-quarkus-deployment,jfoundry-quarkus/jfoundry-outbox-jpa-quarkus-runtime,jfoundry-quarkus/jfoundry-outbox-jpa-quarkus-deployment,jfoundry-quarkus/jfoundry-inbox-jpa-quarkus-runtime,jfoundry-quarkus/jfoundry-inbox-jpa-quarkus-deployment \
  -am -DskipTests install

./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-integration-tests \
  -Pnative verify
```

## Current Scope

This Quarkus integration covers CDI discovery, application transactions, application-service domain-event
dispatch, JPA aggregate persistence context assembly, optional JPA Outbox and Inbox storage, automatic
externalization for explicitly marked events, Kafka message delivery, and optional Outbox dispatch,
recovery, and cleanup. It does not yet provide Quarkus assembly for MyBatis-Plus, RabbitMQ, RocketMQ,
web adapters, or starters. Those capabilities remain explicit follow-up work.
