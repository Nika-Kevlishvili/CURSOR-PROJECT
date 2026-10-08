---
name: sdlc-spec
description: Phase 4 — write docs/spec.md, the agreed and testable contract built from the approved PRD, TRD and ADRs, with every requirement traced to its source and to a future check, and every conflict flagged before approval. Use after the harness is approved, or when requirements change.
---
# Phase 4 — Spec

Claude writes the spec; the owner reviews it and resolves the flagged concerns. The spec is the agreed contract: the PRD says what and why, the TRD the technical frame, the spec the numbered, testable requirements that the plan and the checks will use (ADR-0006).

## Before you start
Gate: phase 3 `approved`. Read docs/intent.md, docs/prd.md, docs/trd.md, the accepted ADRs and any project skills that encode policy. Set phase 4 to `in-progress`.

## Steps
1. Copy sdlc/templates/spec.md → docs/spec.md and write it in one pass. Every requirement gets an ID (FR-n / NFR-n), a `Source:` (P-n, J-n or TR-n) and acceptance criteria that a test or an eval can check. Reference the PRD and TRD by ID instead of copying their text. Unknowns stay inline as `[NEEDS CLARIFICATION: …]`.
2. Flag concerns explicitly: contradictions between PRD, TRD and ADRs, open questions still unanswered, privacy or security issues, anything you could not satisfy. Put the concerns first in your summary to the owner.
3. Walk the owner through the concerns and markers one at a time (the AskQuestion tool, recommendation first). Record each outcome — resolved or parked — in the spec.
4. Traceability: every success metric of the PRD → at least one requirement → a planned check; every must-priority feature (P-n) → at least one FR. Missing links are concerns too.
5. Reader test: a fresh `generalPurpose` subagent on Claude Sonnet 5.5 reads ONLY docs/spec.md, changes no file, and answers 5–10 questions a builder would ask. Fix every wrong or "not stated" answer.
6. Run the `auditor` (gate: spec — review loop per AGENTS.md, loop id `gate-spec`): testability, traceability, unverified assumptions, no marker left.
7. Owner approval → `status: approved`, phase 4 `approved` + a gate-log line, commit `docs(spec): approve`.
8. Next steps: `/sdlc-evaluate` in design mode (choose the gates and evals for this spec), then `/sdlc-plan`.

## Done when
No flagged concern is unresolved and no `[NEEDS CLARIFICATION]` is left, every requirement is testable and traced, the auditor reports no open HIGH finding, and the owner approved.
