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

This capability assembles persistence only. It does not provide Outbox dispatching, scheduling,
payload serialization, automatic domain-event externalization, Inbox assembly, or a starter.

## Outbox Dispatching

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
contract. Recovery, cleanup, payload serialization, automatic event externalization, broker
adapters, and starters remain explicit capabilities.

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
separate consumer application. Its `@QuarkusIntegrationTest` invokes `TransactionRunner` through an
HTTP endpoint against the native executable.

Run the same verification on a machine with GraalVM Native Image, or with Docker available for
Quarkus container builds:

```bash
./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-runtime,jfoundry-quarkus/jfoundry-quarkus-deployment,jfoundry-quarkus/jfoundry-outbox-quarkus-runtime,jfoundry-quarkus/jfoundry-outbox-quarkus-deployment,jfoundry-quarkus/jfoundry-outbox-jpa-quarkus-runtime,jfoundry-quarkus/jfoundry-outbox-jpa-quarkus-deployment,jfoundry-quarkus/jfoundry-inbox-jpa-quarkus-runtime,jfoundry-quarkus/jfoundry-inbox-jpa-quarkus-deployment \
  -am -DskipTests install

./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-integration-tests \
  -Pnative -Dquarkus.native.container-build=true verify
```

## Current Scope

This Quarkus integration covers CDI discovery, application transactions, JPA aggregate persistence
context assembly, and optional JPA Outbox and Inbox storage. It does not yet provide Quarkus
assembly for MyBatis-Plus, recovery and cleanup jobs, messaging, web adapters, configuration
properties beyond Outbox dispatching, or starters. Those capabilities remain explicit follow-up work.
