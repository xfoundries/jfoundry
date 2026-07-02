# Testing And Verification

## Choose The Narrowest Useful Test

Use module-scoped Maven commands first:

```bash
mvn -pl <module> test
```

Use `-am` when reactor dependencies must be built:

```bash
mvn -pl <module> -am test
```

For full confidence before release-sensitive changes:

```bash
mvn test
```

## Common Verification Targets

| Change | Verification |
|---|---|
| Domain model API | `mvn -pl jfoundry-domain test` |
| Architecture annotations | `mvn -pl jfoundry-architecture/jfoundry-hexagonal test` or relevant module |
| ArchUnit rules | `mvn -pl jfoundry-architecture/jfoundry-architecture-test test` |
| Application SPI/core | `mvn -pl jfoundry-application/<module> test` |
| Outbox core | `mvn -pl jfoundry-application/jfoundry-outbox-core test` |
| Inbox core | `mvn -pl jfoundry-application/jfoundry-inbox-core test` |
| MyBatis-Plus adapter | `mvn -pl jfoundry-infrastructure/<module> -am test` |
| Broker adapter | `mvn -pl jfoundry-infrastructure/jfoundry-messaging-<broker> -am test` |
| Spring runtime adapter | `mvn -pl jfoundry-spring/jfoundry-spring-runtime/<module> -am test` |
| Boot auto-configuration | `mvn -pl jfoundry-spring/jfoundry-spring-boot-autoconfigure -am test` |
| Starter POM | `mvn -pl <starter-module> -am test` or `mvn validate` for dependency shape |
| Middleware integration | `mvn -pl jfoundry-verification/jfoundry-middleware-integration-tests -am -Pit verify` |

## Test Expectations

- Add focused unit tests near changed behavior.
- Add auto-configuration condition tests for new Boot wiring.
- Add ArchUnit self-tests for new architecture rules.
- Add persistence tests for store or repository behavior.
- Add concurrency tests for claim, retry, idempotency, or state transition changes.
- Add middleware integration tests only when the behavior requires real database or broker verification.

## Special Notes

Some modules use Surefire `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED` for reflection/specification tests. Preserve this when moving related tests.

When changing public API, starter dependencies, configuration properties, table schemas, or release baselines, include compatibility impact in the final report even if tests pass.

