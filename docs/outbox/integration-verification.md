# Integration Verification Matrix

This project keeps the default test loop free of real middleware:

```bash
mvn test
```

The middleware-backed matrix lives in `jfoundry-integration-tests` and runs through Maven Failsafe.

## Docker Matrix

Run the Docker-backed matrix with Docker available:

```bash
mvn -pl jfoundry-integration-tests -am -Pit verify
```

The `it` profile runs:

- MySQL Outbox store verification.
- MySQL Inbox store verification.
- PostgreSQL Outbox store verification.
- PostgreSQL Inbox store verification.
- Kafka Outbox dispatch verification.
- RabbitMQ Outbox dispatch verification.
- RocketMQ Outbox dispatch verification.

The tests use Testcontainers and require a working Docker environment. If Docker is not available, Testcontainers fails before running assertions with:

```text
Could not find a valid Docker environment
```

RocketMQ broker discovery returns the broker's advertised host and port to the client. The local Docker verification fixes the broker port at `10911` so the host JVM can connect to the same address the broker registers with the name server. Make sure local port `10911` is free before running the full `it` profile.

## Dameng

Dameng verification uses an externally prepared DM8 database. It is intentionally not containerized.

Required environment variables:

- `DM_JDBC_URL`
- `DM_USERNAME`
- `DM_PASSWORD`

Optional environment variable:

- `DM_DRIVER_CLASS`, defaults to `dm.jdbc.driver.DmDriver`

Run:

```bash
DM_JDBC_URL='jdbc:dm://host:5236/SCHEMA' \
DM_USERNAME='SYSDBA' \
DM_PASSWORD='password' \
DM_DRIVER_CLASS='dm.jdbc.driver.DmDriver' \
mvn -pl jfoundry-integration-tests -am -Pit,dm-it \
  -DskipITs=false \
  -Dtest=none \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=DamengStoreIT \
  verify
```

The DM JDBC driver must be available on the test runtime classpath. If the three required DM environment variables are absent, the DM tests are skipped before Spring starts. If the variables are present but the driver or database is unavailable, the tests fail.

## Focused Runs

Run one integration test class:

```bash
mvn -pl jfoundry-integration-tests -am -Pit \
  -DskipITs=false \
  -Dtest=none \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=MySqlOutboxStoreIT \
  verify
```

Useful test classes:

- `MySqlOutboxStoreIT`
- `MySqlInboxStoreIT`
- `PostgreSqlOutboxStoreIT`
- `PostgreSqlInboxStoreIT`
- `KafkaOutboxDispatchIT`
- `RabbitMqOutboxDispatchIT`
- `RocketMqOutboxDispatchIT`
- `DamengStoreIT`

## CI Jobs

Recommended CI split:

- `unit`: `mvn test`
- `integration-docker`: `mvn -pl jfoundry-integration-tests -am -Pit verify`
- `integration-dm`: external DM8 plus `mvn -pl jfoundry-integration-tests -am -Pit,dm-it verify`

The unit job is the fast compatibility gate. The middleware jobs are the production-readiness gate for Inbox idempotency, Outbox claim-token safety, SQL dialect behavior, and broker dispatch behavior.

## Spring Boot Configuration Reuse

JFoundry should not duplicate infrastructure connection configuration that Spring Boot already owns. The current Outbox integration follows these boundaries:

- Database connection settings use Spring Boot's `spring.datasource.*`. `jfoundry.persistence.db-type` is not a replacement for `DataSourceProperties`; it is only an optional MyBatis-Plus `DbType` override for pagination dialect selection when automatic detection is not suitable.
- Kafka broker/client settings use Spring Boot Kafka auto-configuration. JFoundry's Kafka sender auto-configuration requires an existing `KafkaTemplate` bean and only adds JFoundry behavior such as enablement and send timeout.
- RabbitMQ broker/client settings use Spring Boot AMQP auto-configuration. JFoundry's RabbitMQ sender auto-configuration requires an existing `RabbitTemplate` bean and only bridges it to the framework `MessageSender`.
- RocketMQ does not have equivalent first-party Spring Boot infrastructure auto-configuration in this project, so the JFoundry RocketMQ starter expects an application-provided `DefaultMQProducer` and keeps only sender-level behavior such as enablement and send timeout.

When adding new starters, prefer this rule: reuse Spring Boot's existing infrastructure beans and properties first, and introduce `jfoundry.*` properties only for JFoundry-specific behavior or compatibility overrides.
