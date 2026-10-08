---
title: <title>
status: draft            # draft | approved
intent: docs/intent.md
prd: docs/prd.md
trd: docs/trd.md
decisions: []            # ADR ids this spec relies on
created: YYYY-MM-DD
approved: —
---
# Spec: <title>

> The agreed, testable contract built from docs/prd.md, docs/trd.md and the accepted ADRs. Reference them by ID (P-n, J-n, TR-n) instead of copying their text. Mark every unknown inline as `[NEEDS CLARIFICATION: <question>]`; an approved spec contains none.

## Summary (მოკლედ)

## Users and scenarios (მომხმარებლები და სცენარები)
Numbered scenarios: who, wants what, in which situation.

## Functional requirements (ფუნქციური მოთხოვნები)
- FR-1 — When <trigger>, the system <behaviour>. Source: P-n / J-n
  - Acceptance: <observable check>

## Non-functional requirements (არაფუნქციური მოთხოვნები)
- NFR-1 — performance / reliability / cost / accessibility / privacy / language. Source: TR-n / metric
  - Acceptance: <observable check>

## Data and integrations (მონაცემები და ინტეგრაციები)

## UX notes (ინტერფეისი)
Only if there is a UI.

## AI behaviour (AI-ის ქცევა)
Only if the product contains an agent: tools it may use, what it must never do, when it hands over to a human.

## Acceptance and evaluation map (მიღების რუკა)
| Success metric (PRD) | Requirement(s) | Planned check (test / eval / gate) |
|---|---|---|

## Flagged concerns (მონიშნული საკითხები)
| Concern | Why it matters | Outcome (resolved / parked) |
|---|---|---|

## Out of scope (რას არ ვაკეთებთ)

## Traceability (კავშირები)
PRD feature (P-n) and journey (J-n) → FR IDs; TRD requirement (TR-n) → NFR IDs. Every must-priority feature reaches at least one FR.

## Reader test (მკითხველის ტესტი)
| Question a builder would ask | Cold reader's answer | Correct? | Fix |
|---|---|---|---|
