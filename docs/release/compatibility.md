# Compatibility Matrix

## Stable 1.x Line

| Area | Supported Baseline |
|------|--------------------|
| Java compile target | 21 |
| Runtime Java | 21, 25 after CI verification |
| Spring Boot | 3.5.x |
| Spring Framework | 6.2.x |
| Maven release tool | 3.9.x |
| Maven 4 | Compatibility check only until Maven 4 GA |

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

No version substitutions were required for this baseline.

## Future 2.x Line

| Area | Target Baseline |
|------|-----------------|
| Java compile target | 25 |
| Spring Boot | 4.x |
| Spring Framework | 7.x |
| Jakarta EE | 11 via Spring Boot 4 dependencies |

## Release Gates

- `mvn test`
- `mvn -DskipTests package`
- `mvn -pl jfoundry-integration-tests -am -Pit verify`
- Java 25 runtime matrix for the 1.x line
- Maven 4 compatibility matrix
