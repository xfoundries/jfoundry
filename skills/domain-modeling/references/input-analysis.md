# Input Analysis

Use this when the input is a requirements document, user story, support ticket, database schema, API contract, existing code, or a verbal workflow description.

## Extract From Text

Identify:

- Actors: people, systems, organizations, roles.
- Commands: user or system intents, usually verbs.
- Domain events: facts that already happened, usually past tense.
- Policies: rules that react to events or decide whether commands are allowed.
- State: statuses, lifecycle stages, counters, balances, reservations, allocations.
- Invariants: rules that must always hold.
- External systems: payment providers, identity services, brokers, search, ERP, files.
- Read needs: screens, reports, dashboards, exports, notifications.
- Exceptions: cancellation, timeout, retry, compensation, partial failure, manual override.

Prefer exact business words when they appear. If two words look similar, keep both until the domain confirms whether they are synonyms.

## Extract From Tables Or APIs

Tables and APIs are evidence, not the model.

Look for:

- IDs that indicate entity or aggregate identity.
- Status fields that imply lifecycle commands.
- Audit fields that imply events or policies.
- Foreign keys that may be references across aggregates or contexts.
- Wide tables that may hide value objects.
- Join tables that may represent membership, assignment, allocation, or permission concepts.
- Endpoints named `create`, `update`, or `save` that may hide business commands.

Do not mirror every table as an aggregate. Ask what decision the model must protect.

## Questions To Ask

- What action is the user or system trying to complete?
- What business rule can reject this action?
- What must be consistent immediately after the action?
- What can be eventually consistent?
- Who owns this concept and its vocabulary?
- What downstream process reacts after this fact happens?
- What historical edge cases caused bugs or manual corrections?

## Output

Produce a compact extraction list:

- Candidate terms:
- Candidate commands:
- Candidate events:
- Candidate rules/invariants:
- Candidate states:
- External actors/systems:
- Read/reporting needs:
- Open questions:
