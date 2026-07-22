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

## Native Image Verification

The repository's Quarkus native CI job first installs the extension artifacts and then builds a
separate consumer application. Its `@QuarkusIntegrationTest` invokes `TransactionRunner` through an
HTTP endpoint against the native executable.

Run the same verification on a machine with GraalVM Native Image, or with Docker available for
Quarkus container builds:

```bash
./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-runtime,jfoundry-quarkus/jfoundry-quarkus-deployment \
  -am -DskipTests install

./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-integration-tests \
  -Pnative -Dquarkus.native.container-build=true verify
```

## Current Scope

This first Quarkus integration covers CDI discovery and application transactions only. It does not
yet provide Quarkus assembly for JPA, MyBatis-Plus, Outbox, Inbox, messaging, scheduling, web
adapters, configuration properties, or starters. Those capabilities remain explicit follow-up work.
