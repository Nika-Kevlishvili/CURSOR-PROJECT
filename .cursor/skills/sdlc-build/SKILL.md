---
name: sdlc-build
description: Phase 6 — implement tasks/todo.md with the build loop (test first, check, commit). The two-fix Sonnet↔Opus review loop does not apply in this workspace. Use when the plan is approved and it is time to write code, or to resume building. Product code here is Playwright tests only.
---
# Phase 6 — Build

## Roles (ADR-0007, ADR-0006)
- You, the main chat (Sonnet 5.5, effort high): the coder. You implement, run checks, fix.
- `advisor` agent (Opus 5.5): consult it before choosing the approach for a non-trivial task, when the same error appears twice, and before declaring a milestone done; give it a brief — goal, plan or error, evidence. Name its verdict.
- `verifier` subagent: runs the milestone's proof commands in a fresh context and reports only.
- `auditor` subagent (Opus 5.5, high): one read-only review of a milestone diff when you ask for it. There is no two-fix review loop.
- Owner: sees a demo at every milestone, approves it, and decides every finding the loop could not close.

## Before you start
Gate: phase 5 `approved`. Work on a branch `feat/<milestone>`, never on main. Set phase 6 to `in-progress`.

## The task loop — once per task
1. Take the first unchecked task in tasks/todo.md and re-read its proof.
2. RED — write the failing test (or the screenshot check for UI). Run it and confirm it fails for the expected reason. Technique: test-driven-development.
3. GREEN — write the minimal code that passes. Reuse before you write: is it needed, does it already exist here, does the standard library do it? Every changed line must trace to the task.
4. CHECK — run the project's single check command (tests + lint + types). Iterate. After 3 failed fix attempts on the same check: stop, explain in Georgian, ask. Technique for failures: systematic-debugging.
5. IMPROVE — simplify if needed, then re-run CHECK. A deliberate shortcut gets an `asterbit-debt: <what> · limit: <when it breaks> · revisit: <trigger>` comment; never a silent one.
6. RECORD — tick the task with evidence (command + result line). If you deviated from docs/plan.md, add a line to its Deviation log in the same commit.
7. COMMIT — conventional commit on the branch.

## The milestone gate — after the last task of a milestone
1. `verifier` runs the milestone proof in a fresh context.
2. Owner demo — tell the owner where to look, what success looks like and what failure would look like.
3. Owner approval → merge or PR per the harness rules; update the progress note in docs/sdlc-state.md.

## Never
- Edit or delete a test to make it pass during a fix.
- Skip CHECK because "the change is small".
- Grow scope beyond the task — park new ideas in tasks/todo.md under "Parked".
- Run the retired two-fix review loop or `review_rounds.py`.

## Done when (phase)
All tasks are ticked with evidence, every milestone is owner-approved, and the phase-7 gates are green.
