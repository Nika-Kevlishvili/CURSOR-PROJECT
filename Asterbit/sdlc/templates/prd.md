---
title: <product name>
status: draft            # draft | approved
intent: docs/intent.md
created: YYYY-MM-DD
approved: —
---
# PRD: <title>

> Product requirements: what we build, for whom and why. Written at the start of Phase 2 from the accepted intent, before the ADRs. No technology here — stack and tools go to the ADRs, technical requirements to docs/trd.md. Mark every unknown inline as `[NEEDS CLARIFICATION: <question>]`; an approved PRD contains none (sdlc/checks/check_structure.py enforces this).

## Problem and goal (პრობლემა და მიზანი)
One paragraph built from docs/intent.md (Problem, Desired outcome). Link the intent instead of copying it.

## Users (მომხმარებლები)
Who uses it, in which situation, with what skill level. One line per user type.

## User journeys (მომხმარებლის გზები)
- J-1 — <who> wants <goal> when <trigger>: <steps> → <outcome>.

## Features and priorities (ფუნქციები და პრიორიტეტები)
| ID | Feature | Journeys | Priority (must / should / could) | Why |
|---|---|---|---|---|
| P-1 | | J-1 | must | |

## Success metrics (წარმატების საზომი)
Every success criterion of the intent becomes a metric with a target number.
| Metric | Target | Intent criterion |
|---|---|---|

## Release scope (გამოშვების ფარგლები)
- In v1:
- Not in v1 (and why):

## Assumptions and risks (დაშვებები და რისკები)

## Open questions (ღია კითხვები)
Unknowns carried to the ADRs and the TRD. Never invent an answer to fill a section.

## Reader test (მკითხველის ტესტი)
Cold-reader result (/sdlc-architecture step 1): the questions asked, which answers were wrong or "not stated", and what was fixed.
| Question a builder would ask | Cold reader's answer | Correct? | Fix |
|---|---|---|---|
