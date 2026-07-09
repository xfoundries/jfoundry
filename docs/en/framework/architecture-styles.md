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
| `@SecondaryPort` | `application.port.out` | Capability the application requires from the outside. |
| `@SecondaryAdapter` | `adapter.out.persistence`, `adapter.out.messaging` | Technology implementation of an outgoing port. |

jfoundry intentionally does not wrap generic `@Port` or `@Adapter`; use the primary/secondary
specializations when direction is known.

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

![onion-architecture.png](../../assets/onion-architecture.png)

## Rule Entry Points

```java
@AnalyzeClasses(packages = "com.mycompany.myapp")
class ArchitectureTest {

    @ArchTest
    ArchRule[] rules = JFoundryRules.hexagonalStrict();
}
```

Use `JFoundryRules.hexagonalStrict()` for Hexagonal projects. Use `JFoundryRules.onionSimple()` or
`JFoundryRules.onionClassical()` for Onion projects. These entries include baseline guard rules and
the Hexagonal/Onion mutual-exclusion rule.

## References

- jMolecules Architecture: <https://github.com/xmolecules/jmolecules/tree/main/jmolecules-architecture>
- jMolecules ArchUnit: <https://github.com/xmolecules/jmolecules-integrations/tree/main/jmolecules-archunit>
- Hexagonal Architecture / Ports and Adapters: <https://alistair.cockburn.us/hexagonal-architecture/>
- Onion Architecture: <https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
