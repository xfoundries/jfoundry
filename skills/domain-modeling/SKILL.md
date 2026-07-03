---
name: domain-modeling
description: Use when modeling business domains from requirements, user stories, workflows, event storming notes, existing systems, database tables, or APIs; identifying bounded contexts, ubiquitous language, commands, aggregates, entities, value objects, invariants, domain events, domain services, repositories, read models, or reviewing whether a proposed model is behavior-rich rather than table-driven.
---

# Domain Modeling

## Purpose

Use this skill to turn business requirements into an explicit domain model before implementation. It is framework-neutral and should not assume jfoundry, Spring, .NET, Go, Python, or any specific architecture style.

Do not treat domain modeling notes as ceremony for simple CRUD changes. Use them when a change introduces business rules, lifecycle state, invariants, domain events, cross-aggregate coordination, or ambiguous domain language.

## Core Workflow

1. Start from business workflows, not tables or controllers.
2. Extract language, commands, events, rules, states, exceptions, and external actors.
3. Identify bounded-context candidates when terms or rules change meaning across workflows.
4. Design aggregates around invariants and consistency boundaries.
5. Produce the modeling output protocol before coding.
6. Review for table-driven modeling, anemic behavior, oversized aggregates, and misplaced orchestration.

## Reference Routing

- Read `references/input-analysis.md` when starting from requirements, user stories, tickets, existing tables, APIs, or code.
- Read `references/event-storming.md` when workflows are complex, event-heavy, or involve policies and external systems.
- Read `references/bounded-contexts.md` when terms, ownership, data, or rules may differ across teams or subdomains.
- Read `references/aggregate-design.md` when choosing aggregate roots, entities, value objects, invariants, repositories, or domain services.
- Read `references/modeling-output.md` before producing the modeling note or asking for domain review.
- Read `references/review-and-antipatterns.md` when reviewing a proposed model or checking for table-driven/anemic designs.

## Ground Rules

- Prefer domain terms from the business language. Avoid technical names such as manager, handler, data, record, wrapper, or config unless they are real domain terms.
- Keep open domain questions visible. Do not silently hard-code guesses as business rules.
- Do not force DDD patterns into low-complexity CRUD areas.
- Treat architecture and framework mapping as a later step. First make the domain assumptions explicit.
