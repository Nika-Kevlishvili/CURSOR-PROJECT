---
name: sdlc-plan
description: Phase 5 — turn the approved spec into docs/plan.md and tasks/todo.md, interrogated until someone who never saw the conversation could build from the plan alone. Use after the spec (and the evaluation design) are approved.
---
# Phase 5 — Plan

Technique: writing-plans, but the output goes to docs/plan.md and tasks/todo.md.

## Before you start
Gate: phase 4 `approved`, plus the evaluation design from `/sdlc-evaluate` if it was done. Explore the codebase read-only first (plan mode is a good fit for this part). Set phase 5 to `in-progress`.

## Steps
1. Draft docs/plan.md from sdlc/templates/plan.md: approach, milestones (each ends in something the owner can see or try), files that change, order of work, proof per milestone (command + expected output), gates in force, risks, alternatives not chosen.
2. Interrogate the plan and write the answers into the Interrogation log: What could this break? Which step is riskiest, and why? What did we choose not to do? What would we cut if the time halved? Consult the `advisor` agent on the riskiest step.
3. Break milestones into tasks in tasks/todo.md (template sdlc/templates/todo.md). Each task is small (one concern, roughly an hour of agent work) and carries its proof command. Tasks that touch disjoint files get `[parallel-ok]`. Tasks that touch authentication, payments, data deletion, security controls or an irreversible step get `[high-risk]` — the Sonnet↔Opus review loop then runs on that task alone (ADR-0006). Name each requirement ID (FR-n / NFR-n) in the task or milestone that delivers it: sdlc/checks/check_structure.py fails an approved plan that leaves one out.
4. Run the `auditor` (gate: plan — review loop per AGENTS.md, loop id `gate-plan`): coverage of the spec, a proof for every task, risk coverage.
5. Explain the plan to the owner in plain Georgian — the milestones and what they will see after each one, not the file list.
6. Owner approval → `status: approved`, phase 5 `approved` + a gate-log line, commit `docs(plan): approve`.
7. Next step: `/sdlc-build`.

## Done when
Every spec requirement maps to at least one task, every task has a proof, the plan passes the stranger test (buildable without this conversation), and the owner approved.
