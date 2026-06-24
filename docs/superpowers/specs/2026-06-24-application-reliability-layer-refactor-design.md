# Application Reliability Layer Refactor Design

## Context

The current project keeps several framework-neutral reliability contracts under
`jfoundry-infrastructure`:

- `jfoundry-messaging-core`
- `jfoundry-outbox-core`
- `jfoundry-inbox-core`

That placement is now misleading. These modules do not implement a database, broker,
Spring runtime, scheduler, or other external technology. They define application-facing
ports, records, state machines, and templates for reliable event publication and
consumer idempotency.

Moving only `outbox-core` and `inbox-core` is not enough because `outbox-core` depends
on `MessageSender` from `messaging-core`. If `messaging-core` stayed under
infrastructure, the new application layer would depend on infrastructure. Therefore the
refactor must move all three framework-neutral reliability modules together.

## Goals

- Introduce a first-class `jfoundry-application` layer.
- Move application-facing reliability contracts out of infrastructure.
- Rename repository-like ports so they are not confused with DDD aggregate repositories.
- Make Outbox and Inbox message naming symmetric.
- Keep technology adapters in infrastructure.
- Update auto-configuration, starter dependencies, docs, and tests.

## Non-Goals

- Do not change database table names.
- Do not change Outbox dispatch semantics, retry semantics, cleanup behavior, or Inbox
  idempotency behavior.
- Do not introduce backward-compatible deprecated aliases in this snapshot-stage refactor.
  The goal is a clean public API before release.
- Do not move `jfoundry-persistence-core`; it is still persistence infrastructure, not an
  application reliability contract.
- Do not move Spring Boot auto-configuration out of `jfoundry-spring`.
- Do not make Outbox/Inbox persistence models inherit aggregate persistence base classes.
  They are reliability message records, not aggregate data objects.

## Target Module Layout

Add a new Maven aggregator:

```text
jfoundry-application
├── jfoundry-messaging-core
├── jfoundry-outbox-core
└── jfoundry-inbox-core
```

Move these modules from `jfoundry-infrastructure` to `jfoundry-application`:

```text
jfoundry-infrastructure/jfoundry-messaging-core
  -> jfoundry-application/jfoundry-messaging-core

jfoundry-infrastructure/jfoundry-outbox-core
  -> jfoundry-application/jfoundry-outbox-core

jfoundry-infrastructure/jfoundry-inbox-core
  -> jfoundry-application/jfoundry-inbox-core
```

Keep these modules under `jfoundry-infrastructure`:

```text
jfoundry-messaging-jackson
jfoundry-messaging-kafka
jfoundry-messaging-spring
jfoundry-outbox-mybatis-plus
jfoundry-outbox-spring
jfoundry-outbox-jobrunr
jfoundry-inbox-mybatis-plus
jfoundry-persistence-core
jfoundry-persistence-mybatis-plus
```

## Package Renames

Move framework-neutral application contracts to `org.jfoundry.application.*`:

```text
org.jfoundry.infrastructure.messaging
  -> org.jfoundry.application.messaging

org.jfoundry.infrastructure.messaging.externalization
  -> org.jfoundry.application.messaging.externalization

org.jfoundry.infrastructure.outbox.core
  -> org.jfoundry.application.outbox

org.jfoundry.infrastructure.inbox
  -> org.jfoundry.application.inbox
```

Infrastructure adapters keep their `org.jfoundry.infrastructure.*` packages and import the
new application contracts.

## Public API Renames

Rename repository-like ports to store-like names:

```text
OutboxRepository -> OutboxMessageStore
InboxRepository  -> InboxMessageStore
```

Rename MyBatis-Plus implementations accordingly:

```text
MybatisPlusOutboxRepository -> MybatisPlusOutboxMessageStore
MybatisPlusInboxRepository  -> MybatisPlusInboxMessageStore
```

Make Outbox message naming symmetric with Inbox:

```text
OutboxEntry  -> OutboxMessage
OutboxStatus -> OutboxMessageStatus
```

Keep these names:

```text
InboxMessage
InboxMessageStatus
InboxTemplate
InboxHandler
MessageSender
SendResult
PayloadSerializer
DomainEventSink
MessageRouting
AggregateRouting
AggregateRoutingMetadata
ExternalizationRule
ExternalizationRuleResolver
AggregateRoutingResolver
BackoffStrategy
OutboxDispatcher
DefaultOutboxDispatchService
OutboxRuntimeIds
```

Rationale:

- `OutboxMessage` and `InboxMessage` are symmetric reliable-message records.
- `OutboxMessageStore` and `InboxMessageStore` are secondary ports, not DDD aggregate
  repositories.
- `OutboxDispatcher` remains appropriate because it names the dispatch behavior, not a
  persistence abstraction.
- `DefaultOutboxDispatchService` remains acceptable because it is a framework-neutral
  application service that coordinates store + sender + state transitions.

## Dependency Direction

After the refactor:

```text
jfoundry-domain
  <- jfoundry-application/*
     <- jfoundry-infrastructure/*
        <- jfoundry-spring/jfoundry-autoconfigure
        <- jfoundry-spring/jfoundry-spring-boot-starter
```

Expected core dependencies:

- `jfoundry-application/jfoundry-messaging-core` depends on `jfoundry-domain`,
  `jmolecules-events`, and `slf4j-api` provided.
- `jfoundry-application/jfoundry-outbox-core` depends on
  `jfoundry-messaging-core` and `slf4j-api` provided.
- `jfoundry-application/jfoundry-inbox-core` has no Spring or persistence dependency.

Infrastructure adapters depend on the application contracts they implement or consume.
Application modules must not depend on infrastructure modules.

## Adapter Updates

Update all adapter modules to use the new application packages and names:

- `jfoundry-messaging-jackson`
- `jfoundry-messaging-kafka`
- `jfoundry-messaging-spring`
- `jfoundry-outbox-mybatis-plus`
- `jfoundry-outbox-spring`
- `jfoundry-outbox-jobrunr`
- `jfoundry-inbox-mybatis-plus`
- `jfoundry-spring/jfoundry-autoconfigure`
- `jfoundry-spring/jfoundry-spring-boot-starter`

Important adapter class renames:

```text
OutboxData.fromEntry(...) -> OutboxData.fromMessage(...)
OutboxData.toEntry(...)   -> OutboxData.toMessage(...)
```

`OutboxData` itself stays as the MyBatis-Plus persistence view. It is not part of the
application API and still belongs to infrastructure.

## Persistence Base Class Boundary

`OutboxData` and `InboxMessageData` must not extend `BaseData`.

`BaseData<ID>` is the base type for business aggregate persistence data:

- it assumes a single `id` field;
- `ID` must be a jMolecules `Identifier`;
- equality and hash code are based on that aggregate identifier;
- it is used by `DataConverter` and `AbstractPersistenceRepository`.

Outbox and Inbox records do not follow that model:

- `OutboxData` is keyed by `event_id`, not a business aggregate identifier;
- `InboxMessageData` uses the composite key `(consumer_name, message_id)`;
- both records represent delivery/idempotency state, not aggregate state.

`MybatisPlusOutboxMessageStore` and `MybatisPlusInboxMessageStore` must not extend
`MybatisPlusRepository`.

`MybatisPlusRepository` is a template for `AggregateRepository<T, ID>` implementations.
It owns aggregate CRUD, `DataConverter`, and domain-event handoff behavior. Outbox/Inbox
stores instead own message-specific operations such as claim, mark published/failed,
recover stuck dispatching, cleanup, and idempotent processed checks.

Therefore these stores should continue to use `BaseMapper` directly.

## Architecture Annotations and Fitness Rules

The framework itself should follow the same architecture discipline it offers to users.
After this refactor, framework modules should use lightweight architecture annotations at
stable boundaries and ArchUnit tests should verify the dependency direction.

Recommended annotation usage:

- `jfoundry-domain` packages stay domain-layer packages.
- `jfoundry-application` packages are `@ApplicationLayer` and may also use
  hexagonal `@Application` where helpful.
- Store and sender ports are `@SecondaryPort`:
  - `OutboxMessageStore`
  - `InboxMessageStore`
  - `MessageSender`
  - `PayloadSerializer`
- Framework-neutral processing components are application services/components:
  - `InboxTemplate`
  - `DefaultOutboxDispatchService`
  - `DomainEventSink`
- Infrastructure adapters are `@InfrastructureLayer` and `@SecondaryAdapter` where the
  annotation improves clarity:
  - `MybatisPlusOutboxMessageStore`
  - `MybatisPlusInboxMessageStore`
  - `KafkaMessageSender`
  - `JacksonPayloadSerializer`
- `jfoundry-spring` remains Spring Boot integration. It should be constrained by ArchUnit
  rules rather than forced into a hexagonal role annotation.

Add framework self-tests that verify:

- `org.jfoundry.domain..` does not depend on `org.jfoundry.application..`,
  `org.jfoundry.infrastructure..`, or `org.jfoundry.autoconfigure..`;
- `org.jfoundry.application..` does not depend on `org.jfoundry.infrastructure..` or
  `org.jfoundry.autoconfigure..`;
- `org.jfoundry.infrastructure..` does not depend on `org.jfoundry.autoconfigure..`;
- `org.jfoundry.autoconfigure..` is only in `jfoundry-spring/jfoundry-autoconfigure`;
- application store/sender ports are annotated as secondary ports;
- MyBatis/Kafka/Jackson adapter classes are annotated as secondary adapters where applicable.

## Documentation Updates

Update:

- `README.md`
- `docs/transactional-outbox.md`
- `docs/framework-boundaries.md`
- module POM descriptions and dependency comments

Documentation should state:

- `jfoundry-application` hosts application-facing reliability contracts.
- `jfoundry-infrastructure` hosts technical adapters.
- Outbox/Inbox stores are not DDD aggregate repositories.
- Outbox/Inbox data objects do not extend `BaseData`, and their MyBatis stores do not
  extend `MybatisPlusRepository`.
- Kafka remains an optional infrastructure adapter, not part of the default starter.

Historical `docs/superpowers/*` files may keep old names when describing earlier plans,
but current user-facing docs should use the new names.

## Testing Strategy

Run targeted tests after each migration slice:

```bash
mvn -pl jfoundry-application/jfoundry-inbox-core -am test
mvn -pl jfoundry-application/jfoundry-outbox-core -am test
mvn -pl jfoundry-application/jfoundry-messaging-core -am test
mvn -pl jfoundry-infrastructure/jfoundry-outbox-mybatis-plus -am test
mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am test
mvn -pl jfoundry-test -am test
```

Final verification:

```bash
mvn test
rg -n "org\\.jfoundry\\.infrastructure\\.(messaging|outbox\\.core|inbox)|OutboxEntry|OutboxRepository|InboxRepository|MybatisPlusOutboxRepository|MybatisPlusInboxRepository" .
```

Expected final search result:

- no production or current user-facing documentation references to old packages/classes;
- old names may remain only in historical superpowers spec/plan documents if they are
  clearly historical.

## Commit Plan

Use meaningful commits:

1. `refactor(application): introduce application reliability modules`
2. `refactor(outbox): rename outbox message store api`
3. `refactor(inbox): rename inbox message store api`
4. `docs(application): document reliability layer boundaries`

If a commit would contain only mechanical path movement with no semantic readability, combine
it with the corresponding rename commit.
