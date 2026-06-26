# Integration Verification Design

## Goal

Add a production-oriented integration verification matrix for the Inbox, Outbox, and Kafka messaging paths without changing the framework's historical compatibility choices.

The matrix must make the two recently hardened areas observable against real middleware:

- Inbox idempotency under concurrent consumers.
- Outbox claim-token safety from claim through completion, retry, recovery, and cleanup.

## Non-Goals

- Do not upgrade Spring Boot or force a new dependency baseline.
- Do not remove preview API compatibility.
- Do not change release governance or publishing workflow.
- Do not make external middleware tests run during the default `mvn test`.
- Do not require a public Dameng container image to exist.

## Architecture

Create a dedicated Maven module named `jfoundry-integration-tests`. It owns middleware-backed tests and depends on the existing production modules rather than embedding test code inside the core framework modules.

The default developer loop remains:

```bash
mvn test
```

The integration verification loop becomes:

```bash
mvn verify -Pit
```

Dameng verification is opt-in because a reliable public DM8 Testcontainers image and Maven Central driver cannot be assumed. It runs only when the `dm-it` profile is selected and the required connection settings are provided.

```bash
mvn verify -Pit,dm-it
```

## Maven Profiles

### Default Build

The default build must compile and run unit tests only. It must not start Docker containers, connect to Kafka, or connect to external databases.

### `it`

The `it` profile enables Maven Failsafe for `*IT` classes in `jfoundry-integration-tests`.

It runs:

- MySQL Testcontainers tests.
- Kafka Testcontainers tests.
- Shared integration test support used by those tests.

### `dm-it`

The `dm-it` profile enables Dameng tests against an externally prepared DM8 instance. It is intended for local release checks or CI environments that can provision Dameng internally.

Required environment variables:

- `DM_JDBC_URL`
- `DM_USERNAME`
- `DM_PASSWORD`

If the Dameng JDBC dependency is not available from the configured Maven repositories, the profile must document the required driver coordinate or local installation command rather than making normal builds fail.

## MySQL Verification

MySQL tests use Testcontainers with a MySQL 8 image. They must create the real framework tables using the module SQL migrations or equivalent schema scripts from the production resources.

The Outbox MySQL test suite verifies:

- Pending messages can be claimed by one worker.
- Concurrent workers cannot complete a message with a stale or missing claim token.
- A claimed message can be marked `PUBLISHED` only by the current claim holder.
- A claimed message can be marked `FAILED` only by the current claim holder.
- Recovery releases abandoned in-progress messages after the timeout window.
- Cleanup removes old terminal messages while retaining recent terminal messages and active messages.
- Large payloads can be persisted, loaded, and dispatched without truncation.

The Inbox MySQL test suite verifies:

- A new `messageId + consumerName` pair can transition from absent to `PROCESSING`.
- Concurrent attempts for the same `messageId + consumerName` allow only one handler to start.
- A `PROCESSING` record can transition to `PROCESSED`.
- A `PROCESSING` record can transition to `FAILED`.
- A previously `PROCESSED` record cannot be reprocessed.
- Different consumers for the same message id do not block each other.

## Kafka Verification

Kafka tests use Testcontainers with a real broker. They verify framework behavior at the Outbox-to-Kafka boundary instead of Kafka client internals.

The Kafka suite verifies:

- A pending Outbox message claimed by the dispatcher is sent to Kafka and completed as `PUBLISHED` with the same claim token.
- The produced Kafka record contains the expected topic, key, payload, and headers.
- A send failure leaves the Outbox message in `FAILED` or retryable state through the existing dispatcher failure path.
- Retry metadata is preserved so a later dispatch cycle can claim the message again when eligible.

## Dameng Verification

Dameng tests validate the same store-level safety properties as the MySQL tests where SQL dialect differences matter most.

The Dameng suite verifies:

- Outbox insert, claim, claim-token completion, stale-token rejection, recovery, and cleanup.
- Inbox first-start, duplicate-start rejection, processed transition, and failed transition.
- SQL migrations are compatible with DM8.

Dameng tests must be skipped with a clear message when `dm-it` is selected but the required environment variables are missing. They must not silently pass when configured credentials are present but the database operation fails.

## CI Behavior

Recommended CI jobs:

- `unit`: runs `mvn test`.
- `integration-mysql-kafka`: runs `mvn verify -Pit` on workers with Docker available.
- `integration-dm`: runs `mvn verify -Pit,dm-it` only in an environment with DM8 and the JDBC driver configured.

The `unit` job remains the fast compatibility gate. The middleware jobs are release-readiness gates and should be required before declaring the framework production-ready for Inbox/Outbox/Kafka usage.

## Acceptance Criteria

- `mvn test` passes without Docker, Kafka, MySQL, Dameng, or extra environment variables.
- `mvn verify -Pit` starts MySQL and Kafka containers and runs the MySQL/Kafka matrix.
- `mvn verify -Pit,dm-it` runs Dameng tests when `DM_JDBC_URL`, `DM_USERNAME`, and `DM_PASSWORD` are present.
- Dameng tests fail on real database errors and skip only for missing explicit configuration.
- Integration tests assert the recently fixed claim-token and Inbox idempotency guarantees against real middleware.
- The new module is documented well enough for maintainers to run the matrix locally and in CI.

## Risks And Mitigations

Risk: Dameng JDBC artifacts may not be available in the configured Maven repositories.

Mitigation: Keep Dameng behind `dm-it`, document the dependency requirement, and avoid breaking default builds.

Risk: Container-based tests add runtime cost.

Mitigation: Keep them out of `mvn test` and run them through Failsafe under `-Pit`.

Risk: Integration tests duplicate unit-test assertions without adding value.

Mitigation: Keep unit tests focused on branch behavior and reserve integration tests for database locks, unique constraints, SQL dialect behavior, and real Kafka send/receive semantics.

Risk: Historical compatibility constraints hide framework readiness gaps.

Mitigation: Treat this matrix as the production-readiness gate for the two prioritized concerns only: Inbox idempotency and Outbox/Kafka reliability.
