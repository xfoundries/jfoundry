---
name: maintain-jfoundry-framework
description: Guide AI agents when modifying the jfoundry framework repository itself. Use for changes to jfoundry modules, module boundaries, public APIs, jMolecules architecture annotations, ArchUnit rules, Maven BOMs, starters, Spring Boot auto-configuration, runtime adapters, persistence adapters, messaging adapters, Outbox/Inbox internals, release compatibility, and framework documentation. Do not use for downstream business applications that merely consume jfoundry.
---

# Maintain JFoundry Framework

## Purpose

Use this skill when changing this repository as a framework. It protects jfoundry's module boundaries, dependency direction, starter semantics, public API compatibility, and verification discipline.

Do not apply this skill to downstream business projects. Use `skills/use-jfoundry` for application projects that consume jfoundry.

## Maintenance Workflow

1. Classify the task: domain API, architecture annotation/rule, application SPI, infrastructure adapter, Spring runtime adapter, Boot auto-configuration, starter, BOM, verification, docs, release compatibility, or integration test.
2. Read the matching reference:
   - `references/module-boundaries.md` for dependency direction and module roles.
   - `references/feature-placement.md` before adding or moving code.
   - `references/starters-and-boms.md` before changing dependency management or starter modules.
   - `references/testing.md` before choosing verification commands.
   - `references/common-change-recipes.md` for recurring framework changes.
3. Inspect existing modules and tests that already implement the same pattern.
4. Make the smallest change that preserves framework-neutral core contracts and explicit runtime integration.
5. Add or update focused tests next to the changed module.
6. Run the narrowest Maven verification first, then broader verification when public APIs, starters, auto-configuration, or cross-module behavior changed.
7. Call out compatibility impact when changing public APIs, starter dependencies, configuration properties, table schemas, event routing, or state transitions.

## Non-Negotiable Boundaries

- Keep `jfoundry-domain`, `jfoundry-architecture`, and application core modules independent of Spring, Spring Boot, web frameworks, broker clients, and persistence framework details.
- Keep Spring Framework runtime adapters under `jfoundry-spring/jfoundry-spring-runtime`.
- Keep Spring Boot auto-configuration only under `jfoundry-spring/jfoundry-spring-boot-autoconfigure`.
- Keep Spring Boot starters as dependency entry points. Do not put Java runtime logic in starter modules.
- Keep framework-neutral technical adapters under `jfoundry-infrastructure`.
- Keep reusable architecture tests under `jfoundry-architecture/jfoundry-architecture-test`.
- Keep middleware integration verification under `jfoundry-verification`.
- Do not make default starters heavy. Outbox, Inbox, broker adapters, JobRunr, and MyBatis-Plus store adapters must remain explicit capability choices.

## Source Documents

Prefer current repository documents and code over memory:

- `docs/framework-boundaries.md`
- `docs/architecture-styles.md`
- `docs/archunit-rules.md`
- `docs/transactional-outbox.md`
- `docs/repository-vs-read-ports.md`
- `docs/release/compatibility.md`
- top-level `pom.xml` and module POMs
- nearby tests in the module being changed

## Output Discipline

When reporting a framework change, include:

- modules touched
- boundary decision made
- tests or verification command run
- compatibility or migration impact

