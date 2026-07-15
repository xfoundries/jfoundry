# Architecture Styles

jfoundry expresses two primary architecture styles through jMolecules: Hexagonal Architecture
(Ports and Adapters) and Onion Architecture. Both are pure Java annotation modules and do not
depend on Spring, Helidon, Quarkus, Micronaut, or other runtime frameworks.

jfoundry internals use Onion Simple to keep module roles consistent. Business applications can
choose either Hexagonal or Onion based on their architecture language.

## Module Choice

| Module | Use when |
|--------|----------|
| `jfoundry-hexagonal` | The project wants explicit primary/secondary ports and adapters. |
| `jfoundry-onion` | The project wants dependency direction to converge toward the domain model. |

`jfoundry-architecture` is an aggregate POM for architecture style modules. Business code should
normally depend on the concrete style module or a starter that brings it in.

## Hexagonal

Hexagonal Architecture is useful when external inputs and outputs are first-class design concepts.

| Role | Typical location | Meaning |
|------|------------------|---------|
| `@Application` | `domain`, `application` | Application core protected from external technologies. |
| `@PrimaryPort` | `application.port.in` | Use case entrypoint exposed to driving adapters. |
| `@PrimaryAdapter` | `adapter.in.web`, `adapter.in.messaging` | Technology that drives the application. |
| `@SecondaryPort` | `application.port.out`; `domain.repository` for aggregate repositories | Capability the application requires from the outside. |
| `@SecondaryAdapter` | `adapter.out.persistence`, `adapter.out.messaging` | Technology implementation of an outgoing port. |

jfoundry intentionally does not wrap generic `@Port` or `@Adapter`; use the primary/secondary
specializations when direction is known.

An aggregate repository remains a DDD repository contract. In a Hexagonal project it may also be
annotated as a `@SecondaryPort`, while remaining under `domain.repository`; do not duplicate it as
an application `port.out` interface. A `@SecondaryAdapter` may implement either a regular secondary
port or such a DDD repository.

Hexagonal Architecture does not mandate a package tree. Small projects may use global
`application.port.in` and `application.port.out` packages. For non-trivial business applications,
jfoundry recommends organizing by business capability first and locating direction inside it, for
example `application.claim.query.port.in`, `application.claim.query.port.out`, and
`application.claim.query.view`. The strict convention rules recognize direction packages at any
depth.

Primary and Secondary Ports may share application-owned query or view models, but neither direction
should own models exposed by the other. Put shared models in a neutral application capability
package: Primary Ports must not depend on `port.out` packages, and Secondary Ports must not depend
on `port.in` packages. Keep HTTP DTOs in the primary adapter and persistence/remote data models in
the secondary adapter.

![hexagonal-architecture.png](../../assets/hexagonal-architecture.png)

## Onion

Onion Architecture is useful when the main goal is protecting the domain core and making dependency
direction explicit.

| Style | Role | Typical location |
|-------|------|------------------|
| Onion Simple | `@DomainRing` | `domain` |
| Onion Simple | `@ApplicationRing` | `application` |
| Onion Simple | `@InfrastructureRing` | `infrastructure`, `adapter` |
| Onion Classical | `@DomainModelRing` | `domain.model` |
| Onion Classical | `@DomainServiceRing` | `domain.service` |
| Onion Classical | `@ApplicationServiceRing` | `application` |
| Onion Classical | `@InfrastructureRing` | `infrastructure` |

For new projects that choose Onion, prefer Onion Simple unless the team already has a clear reason
to separate domain model, domain service, and application service rings.

In Onion, the same aggregate repository is an inner-ring DDD contract implemented by an
`@InfrastructureRing` type. This is the Onion expression of dependency inversion; do not add
Hexagonal annotations to an Onion analysis scope.

Onion Architecture does not define Primary/Secondary Port or Adapter roles and does not prescribe
`*Port`, `*Adapter`, or `*UseCase` suffixes. Name domain types from the ubiquitous language and name
application contracts by their actual responsibility. Names such as `Reader`, `Store`, `Finder`, or
`Provider` are clear Java project conventions when they fit the responsibility; they are not DDD or
Onion patterns and jfoundry does not require them. Infrastructure implementations may add technology
names such as `Mybatis` or `Kafka` where that identifies the implementation.

Within an Onion ring, non-trivial code may still be organized by business capability first. Models
shared by application contracts belong to a neutral package inside that application capability,
not to the domain or infrastructure merely for reuse. This ownership rule does not introduce
`port.in` / `port.out` semantics into Onion.

![onion-architecture.png](../../assets/onion-architecture.png)

## Rule Entry Points

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchTests rules = JFoundryRules.hexagonalStrict();
}
```

Use `JFoundryRules.hexagonalStrict()` for Hexagonal projects. Use `JFoundryRules.onionSimple()` or
`JFoundryRules.onionClassical()` for Onion projects. These entries include baseline guard rules and
the Hexagonal/Onion mutual-exclusion rule. Each primary-style entrypoint also fails when the
analyzed scope contains no matching architecture annotation at all. This prevents an annotation-
driven rule set from passing as an empty no-op. It does not require every possible role or ring to
appear in a deliberately partial analysis scope.

## References

- jMolecules Architecture: <https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit: <https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters: <https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture: <https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
