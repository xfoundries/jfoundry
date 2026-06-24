# Framework Boundaries Implementation Plan

## Goal

Make jfoundry core modules independent from application frameworks while preserving the current Spring Boot starter behavior.

## Steps

1. Split messaging serialization and routing.
   - Add `jfoundry-messaging-jackson` for `JacksonPayloadSerializer`.
   - Remove Jackson and Spring dependencies from `jfoundry-messaging-core`.
   - Replace Spring SpEL key evaluation with framework-neutral property-path evaluation.

2. Extract Outbox dispatch runtime.
   - Add framework-neutral dispatch service to `jfoundry-outbox-core`.
   - Make Spring scheduled and JobRunr dispatchers delegate to that service.
   - Keep scheduling annotations in Spring/JobRunr adapters only.

3. Move JobRunr Spring Boot auto-configuration.
   - Keep `jfoundry-outbox-jobrunr` as pure JobRunr dispatcher code.
   - Move `JobRunrDispatcherAutoConfiguration` and Spring Boot import metadata into `jfoundry-autoconfigure`.

4. Verify boundaries.
   - Check core modules have no Spring, Spring Boot, Helidon, Quarkus, CDI, or Jakarta runtime dependencies.
   - Run targeted module tests after each step and full `mvn test` with Java 21 at the end.
