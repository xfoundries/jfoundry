# Compatibility Matrix

## Stable 1.x Line

| Area | Supported Baseline |
|------|--------------------|
| Java compile target | 21 |
| Runtime Java | 21 |
| Java 25 | CI compatibility target |
| Spring Boot | 3.5.x |
| Spring Framework | 6.2.x |
| Maven release tool | 3.9.x |
| Maven 4 | Compatibility check only while Maven 4 is preview/RC |

## Stable 1.x Dependency Baseline

| Dependency | Version |
|------------|---------|
| Spring Boot | 3.5.16 |
| Spring Framework | 6.2.19 |
| MyBatis-Plus | 3.5.16 |
| MyBatis-Plus Spring Boot 3 starter | 3.5.16 |
| Jackson | 2.19.4 |
| Spring Kafka | 3.3.16 |
| Spring AMQP | 3.2.12 |
| JobRunr | 8.7.1 |
| RocketMQ client | 5.5.0 |
| Javassist override | 3.30.2-GA |

`org.javassist:javassist` is managed explicitly because `rocketmq-client:5.5.0` brings
`rocketmq-remoting -> reflections:0.9.11 -> javassist:3.21.0-GA`, whose POM emits a
Maven 4 model warning. The managed version keeps the Maven 4 compatibility gate clean.

## Verification Evidence

Recorded on 2026-06-27 with local Java `21.0.10-tem` and Maven wrapper `3.9.16`.

| Gate | Command | Result |
|------|---------|--------|
| Unit tests | `./mvnw test` | PASS |
| Package artifacts | `./mvnw -DskipTests package` | PASS |
| Integration tests | `./mvnw -pl jfoundry-integration-tests -am -Pit verify` | PASS, 28 integration tests with Docker 29.5.3/Testcontainers |
| Release guard | `mvn -Prelease -DskipTests validate` | Expected fail fast on `Release builds require non-SNAPSHOT project versions.` |
| Maven 4 validate | Maven `4.0.0-rc-5`, `mvn -B -DskipTests validate -e` | PASS, no model warnings |
| Maven 4 package | Maven `4.0.0-rc-5`, `mvn -B -DskipTests package` | PASS |

Local Java 25 is not installed in this worktree. Java 25 is covered by the GitHub Actions
test matrix and must pass there before advertising Java 25 runtime compatibility for 1.x.

## Future 2.x Line

Spring Boot 4.x and Java 25 should be handled as a 2.x compatibility line, not folded into
the first 1.x Central release. As of 2026-06-27, Spring Boot 4.1.0 is available, Spring Boot
4.0.7 remains a stable 4.0 maintenance release, and Spring Boot 4.1 supports Java versions
up to Java 26. JDK 25 reached General Availability on 2025-09-16.

| Area | Target Baseline |
|------|-----------------|
| Java compile target | 25 |
| Spring Boot | 4.x |
| Spring Framework | 7.x |
| Jakarta EE | 11 via Spring Boot 4 dependencies |
| Maven release tool | Maven 3.9.x until Maven 4 GA |
| Maven 4 | Compatibility matrix first, release tool only after GA evidence |

See `docs/superpowers/plans/2026-06-27-spring-boot-4-java-25.md`.

## Release Gates

- `./mvnw test`
- `./mvnw -DskipTests package`
- `./mvnw -pl jfoundry-integration-tests -am -Pit verify`
- Java 25 runtime matrix for the 1.x line in CI
- Maven 4 compatibility matrix in CI
- Maven Central metadata guard in the `release` profile
