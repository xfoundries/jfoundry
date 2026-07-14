# Getting Started for Business Projects

This guide is for teams adopting jfoundry in business systems. Start from architecture boundaries,
then add runtime integration and technical adapters only when the business use case needs them.

## When to Choose jfoundry

jfoundry fits projects that have explicit domain models, aggregate invariants, domain events,
architecture boundaries, or reliable integration requirements. For short CRUD prototypes with no
domain complexity, a plain runtime framework and ORM may be simpler.

## Recommended Path

Start with the smallest useful skeleton:

- Java 21 and Maven;
- a confirmed architecture style, such as Hexagonal or Onion, selected from domain and project
  constraints rather than a jfoundry scaffold default;
- `jfoundry-dependencies` for runtime-neutral projects, or `jfoundry-spring-dependencies` for
  Spring/Spring Boot projects;
- only the starters needed by each module;
- ArchUnit tests early;
- no Outbox, Inbox, broker adapter, or JobRunr until the workflow needs them.

When the architecture style is still undecided, complete domain modeling and architecture guidance
before generating package structures or architecture tests that depend on that choice.

## Dependency Placement

| Module | Add | Avoid |
|--------|-----|-------|
| `domain` | `jfoundry-domain-starter` | Spring, ORM annotations, HTTP, MQ clients, Boot starters |
| `application` | `jfoundry-application-starter` | mapper/service APIs, broker adapters, Boot starters |
| `infrastructure` | `jfoundry-infrastructure-mybatis-plus-starter` or `jfoundry-infrastructure-jpa-starter` for the selected persistence adapter | application entrypoints and Boot auto-configuration |
| `boot` / runtime assembly | `jfoundry-spring-boot-starter` plus explicit capability starters | domain logic |
| architecture tests | `jfoundry-architecture-test` with `test` scope | production scope |

## Common Spring Boot Additions

- Business MyBatis-Plus runtime assembly: `jfoundry-mybatis-plus-spring-boot-starter`
- Business JPA runtime assembly: `jfoundry-jpa-spring-boot-starter`
- Local Spring domain event publishing: `jfoundry-event-spring-boot-starter`
- Messaging contracts and default logging sender: `jfoundry-messaging-spring-boot-starter`
- Kafka/RabbitMQ/RocketMQ sender adapters: dedicated messaging starters
- Redisson-backed distributed locks: `jfoundry-lock-redisson-spring-boot-starter`
- Outbox core runtime: `jfoundry-outbox-spring-boot-starter`
- Outbox MyBatis-Plus store: `jfoundry-outbox-mybatis-plus-spring-boot-starter`
- JobRunr dispatch trigger: `jfoundry-outbox-jobrunr-spring-boot-starter`
- Inbox core runtime: `jfoundry-inbox-spring-boot-starter`
- Inbox MyBatis-Plus store: `jfoundry-inbox-mybatis-plus-spring-boot-starter`
- Web MVC ProblemDetail responses: `jfoundry-webmvc-spring-boot-starter`

## Package Shape

```text
com.example.order
├── boot
├── domain
├── application
│   └── port
│       ├── in
│       └── out
├── adapter
│   ├── in
│   └── out
└── infrastructure
```

The exact names can vary, but the dependency direction should stay stable: domain is independent,
application orchestrates use cases, adapters translate external technologies, and runtime assembly
wires the application.

## Next Reading

- [Architecture Styles](../framework/architecture-styles.md)
- [ArchUnit Architecture Rules](../framework/archunit-rules.md)
- [Repository and Read-side Contracts](../modeling/repository-vs-read-contracts.md)
- [Application Transactions](application-transactions.md)
- [Distributed Locks](distributed-locks.md)
- [Persistence DataMapper and MapStruct](persistence-data-mappers.md)
- [Transactional Outbox and Inbox](transactional-outbox.md)
