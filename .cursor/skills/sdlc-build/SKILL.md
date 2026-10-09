---
name: sdlc-build
description: Phase 6 — implement tasks/todo.md with the build loop (test first, check, commit) and the Sonnet↔Opus review loop (review → fix → review → fix → final review → owner) at every milestone and on every [high-risk] task, under the stage-1 orchestration (Sonnet 5.5 coder, Opus 5.5 advisor and auditor). Use when the plan is approved and it is time to write code, or to resume building.
---
# Phase 6 — Build

## Roles (ADR-0007, ADR-0006)
- You, the main chat (Sonnet 5.5, effort high): the coder. You implement, run checks, fix.
- `advisor` agent (Opus 5.5): consult it before choosing the approach for a non-trivial task, when the same error appears twice, and before declaring a milestone done; give it a brief — goal, plan or error, evidence. Name its verdict.
- `verifier` subagent: runs the milestone's proof commands in a fresh context and reports only.
- `auditor` subagent (Opus 5.5, high): reviews the diff in the review loop below; never fixes.
- Owner: sees a demo at every milestone, approves it, and decides every finding the loop could not close.

## Before you start
Gate: phase 5 `approved`. Work on a branch `feat/<milestone>`, never on main. Set phase 6 to `in-progress`.

## The task loop — once per task
1. Take the first unchecked task in tasks/todo.md and re-read its proof. For a `[high-risk]` task, write its base into the task line before you change anything (`· base: <git rev-parse --short HEAD>`) — shell variables do not survive between commands or a compaction.
2. RED — write the failing test (or the screenshot check for UI). Run it and confirm it fails for the expected reason. Technique: test-driven-development.
3. GREEN — write the minimal code that passes. Reuse before you write: is it needed, does it already exist here, does the standard library do it? Every changed line must trace to the task.
4. CHECK — run the project's single check command (tests + lint + types). Iterate. After 3 failed fix attempts on the same check: stop, explain in Georgian, ask. Technique for failures: systematic-debugging.
5. IMPROVE — simplify if needed, then re-run CHECK. A deliberate shortcut gets an `asterbit-debt: <what> · limit: <when it breaks> · revisit: <trigger>` comment; never a silent one.
6. If the task is tagged `[high-risk]`: run the review loop on `<base from the task line>..HEAD` with loop id `<milestone>-<task>` before you tick it.
7. RECORD — tick the task with evidence (command + result line). If you deviated from docs/plan.md, add a line to its Deviation log in the same commit.
8. COMMIT — conventional commit on the branch.

## The review loop — Sonnet ↔ Opus, exactly two fix rounds
Runs at the end of every milestone (loop id `<milestone>`, review 1 range `$(git merge-base main HEAD)..HEAD` on the branch `feat/<milestone>`, which can be recomputed at any time) and on every `[high-risk]` task. Reviews 2 and 3 cover exactly the fix: base = the previous review's head, head = the fix commit — the ledger refuses any other range. Rounds are counted by the ledger, never from memory:

```bash
python3 sdlc/checks/review_rounds.py status <loop>          # 3 = continue (prints the next step) · 0 = closed · 1 = to the owner · 2 = ledger unreadable
python3 sdlc/checks/review_rounds.py review <loop> --base <BASE> --head <HEAD> --high N --medium N --low N
python3 sdlc/checks/review_rounds.py fix <loop> --head <HEAD>
```

1. Review 1 — `auditor` reviews `BASE..HEAD`. Record its `COUNTS` line with `review`.
2. No HIGH or MEDIUM → the loop is closed. LOW findings go to tasks/todo.md "Parked"; they never use a round.
3. Fix 1 — fix only what the auditor located with evidence, run CHECK, commit, record `fix`.
4. Review 2 — `auditor` in re-review mode with the previous findings: ADDRESSED / NOT ADDRESSED, plus the fix diff only. Record it.
5. Fix 2, then review 3 (final), recorded the same way.
6. When `status` exits 1 — findings remain after the final review, or HIGH+MEDIUM did not fall between two reviews — stop. Report to the owner in Georgian: what remains, why, and the options. Mark the milestone UNVERIFIED until the owner decides. Never start a third fix round. If the owner asks for more work, it is a new loop with a new id (`<id>-r2`).
7. `status` exit 2 means the ledger is unreadable or malformed: stop and report it — never guess the round.

## The milestone gate — after the last task of a milestone
1. `verifier` runs the milestone proof in a fresh context.
2. The review loop above, until `status` exits 0, or the owner has decided the open findings.
3. Owner demo — tell the owner where to look, what success looks like and what failure would look like.
4. Owner approval → merge or PR per the harness rules; update the progress note in docs/sdlc-state.md.

## Never
- Edit or delete a test to make it pass during a fix.
- Skip CHECK because "the change is small".
- Count review rounds from memory or start a third fix round.
- Grow scope beyond the task — park new ideas in tasks/todo.md under "Parked".

## Done when (phase)
All tasks are ticked with evidence, every milestone's review loop is closed or decided by the owner, every milestone is owner-approved, and the phase-7 gates are green.
