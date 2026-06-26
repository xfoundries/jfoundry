# Integration Verification Matrix

This project keeps the default test loop free of real middleware:

```bash
mvn test
```

The middleware-backed matrix lives in `jfoundry-integration-tests` and runs through Maven Failsafe.

## MySQL And Kafka

Run the MySQL/Kafka matrix with Docker available:

```bash
mvn -pl jfoundry-integration-tests -am -Pit verify
```

The `it` profile runs:

- MySQL Outbox store verification.
- MySQL Inbox store verification.
- Kafka Outbox dispatch verification.

The tests use Testcontainers and require a working Docker environment. If Docker is not available, Testcontainers fails before running assertions with:

```text
Could not find a valid Docker environment
```

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
- `KafkaOutboxDispatchIT`
- `DamengStoreIT`

## CI Jobs

Recommended CI split:

- `unit`: `mvn test`
- `integration-mysql-kafka`: `mvn -pl jfoundry-integration-tests -am -Pit verify`
- `integration-dm`: external DM8 plus `mvn -pl jfoundry-integration-tests -am -Pit,dm-it verify`

The unit job is the fast compatibility gate. The middleware jobs are the production-readiness gate for Inbox idempotency, Outbox claim-token safety, and Kafka dispatch behavior.
