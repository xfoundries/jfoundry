# Event Storming

Use this when a workflow has multiple steps, policies, external systems, asynchronous reactions, retries, compensations, or unclear ownership.

## Modeling Elements

- Command: an intent to do something, usually imperative, e.g. `Submit Order`.
- Domain Event: a business fact that happened, past tense, e.g. `Order Submitted`.
- Policy: a rule or process that reacts to an event and may issue a command.
- Aggregate: the consistency boundary that handles commands and emits events.
- External System: something outside the domain model that sends commands or receives facts.
- Read Model: information shaped for a query, screen, report, notification, or decision support.

## Flow

1. List domain events first in business time order.
2. Add commands that cause each event.
3. Add the actor or external system that issues each command.
4. Add policies that react to events and trigger later commands.
5. Mark rules that can reject commands.
6. Mark read models needed by users or policies.
7. Group related commands/events around aggregate candidates.
8. Split the flow when vocabulary or ownership changes.

## Heuristics

- Commands are requests and can fail; events are facts and should not be rejected after the fact.
- If a rule must reject a command, it belongs near the aggregate or domain service making that decision.
- If a reaction can happen later, it may be a policy/process rather than part of the aggregate transaction.
- If a read model combines multiple aggregates, keep it out of the write aggregate.

## Output Shape

Use a compact list:

```text
Actor/System -> Command -> Aggregate -> Event -> Policy/Reaction -> Read Model
```

Then record:

- Rejection rules:
- Immediate consistency needs:
- Eventually consistent reactions:
- External systems:
- Open questions:
