---
title: <product name>
status: draft            # draft | approved
prd: docs/prd.md
decisions: []            # ADR ids this TRD relies on
created: YYYY-MM-DD
approved: —
---
# TRD: <title>

> Technical requirements: the technical frame every builder must respect. Written at the end of Phase 2, after the ADRs are accepted, from docs/prd.md and those ADRs. It never re-argues a decision — it links the ADR and states the requirement that follows from it. Mark every unknown inline as `[NEEDS CLARIFICATION: <question>]`; an approved TRD contains none.

## Technical context (ტექნიკური კონტექსტი)
| Field | Value | Source (ADR / PRD) |
|---|---|---|
| Language and version | | |
| Main frameworks and libraries | | |
| Data storage | | |
| Testing tools | | |
| Target platform | | |
| Hosting and deployment | | |
| AI models and agent framework (if any) | | |
| Performance goals | | |
| Constraints (budget, privacy, offline…) | | |
| Expected scale | | |

## Components (კომპონენტები)
The parts of the system and how they talk to each other. A mermaid diagram if it helps the owner see them.

## Data model (მონაცემთა მოდელი)
Main entities, their fields and relations.

## Interfaces and contracts (ინტერფეისები)
Every boundary — API, file, event, external service: input, output, errors.

## Technical requirements (ტექნიკური მოთხოვნები)
- TR-1 — <requirement>. Source: ADR-NNNN / P-n. Check: <how it will be verified>.

## Security and privacy (უსაფრთხოება და კონფიდენციალურობა)
Secrets handling, authentication, personal data, untrusted input.

## Observability (დაკვირვება)
What is logged, what is measured, what raises an alarm.

## Open questions (ღია კითხვები)

## Reader test (მკითხველის ტესტი)
| Question a builder would ask | Cold reader's answer | Correct? | Fix |
|---|---|---|---|
