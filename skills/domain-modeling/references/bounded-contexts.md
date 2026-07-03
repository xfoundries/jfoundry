# Bounded Contexts

Use this when a domain has multiple teams, subdomains, integrations, conflicting terms, duplicated data, or workflows with different rule owners.

## Context Signals

Consider a separate bounded context when:

- The same term has different meanings in different workflows.
- Different teams own rules or releases.
- Data is copied because another area needs a different shape or lifecycle.
- One workflow treats a concept as a core decision while another treats it as reference data.
- Consistency requirements differ sharply.
- Integrations require translation or anti-corruption logic.

Do not split contexts only because tables or packages are numerous. Split around language, ownership, and rule boundaries.

## Context Relationship Types

Use simple relationship language:

- Upstream/downstream: one context depends on another's published facts or API.
- Customer/supplier: downstream requirements influence upstream contracts.
- Conformist: downstream accepts upstream model as-is.
- Anti-corruption layer: downstream translates upstream concepts to protect its own model.
- Shared kernel: contexts intentionally share a small model; keep it rare and explicit.
- Published language: contexts integrate through a stable contract or event language.

## Checks

- Does each context have its own ubiquitous language?
- Who owns the rules and vocabulary?
- Which concepts cross the boundary by ID, event, API, or imported snapshot?
- Where is translation needed?
- What must not leak across the boundary?

## Output

- Bounded context candidates:
- Key terms per context:
- Context owners:
- Upstream/downstream relationships:
- Integration contracts/events:
- Anti-corruption needs:
- Open questions:
