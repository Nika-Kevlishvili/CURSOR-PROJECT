---
name: sdlc-harness
description: Phase 3 — define the harness: permissions, boundaries, autonomy tier and security rules for the Cursor agent in this repo and, if the product is an agent, for that agent; plus the memory settings. Produces .cursor/hooks.json, .cursor/hooks/, .cursor/cli.json and an ADR. Use after architecture is approved, or whenever permissions, hooks or memory need to change.
---
# Phase 3 — Harness

The harness is everything around a model that decides what it can see, touch and remember. There can be two: the Cursor agent's own harness in this repo (always) and the product agent's harness (only if the product is or contains an agent).

## Before you start
- Gate: phase 2 `approved`. Read the accepted ADRs — the chosen stack decides which commands are safe to pre-approve.
- Read sdlc/design/memory-and-context.md, ADR-0004 and ADR-0008: the engine-level memory mechanics are decided there; this phase only tunes them.
- Read sdlc/design/ai-security.md, layers 1–3.

## 1. Cursor harness (this repo)
One topic per question round, in Georgian, each with a recommendation:
1. Autonomy tier per environment — read-only / propose / act / deploy. Default: act locally on a branch; never deploy or push without the owner.
2. Permissions — Cursor's IDE has no checked-in deny list, so every hard "never" is a hook (`beforeShellExecution` for commands, `preToolUse` for file tools), and `failClosed` stays on. `allow` the safe inner loop of the chosen stack (test, lint, build, git status/diff/log) in `.cursor/cli.json` and, if the owner wants fewer prompts in the IDE, in a `.cursor/permissions.json` terminal allowlist; "ask first" commands are the guard's ask findings; `deny` reading secrets (.env*, secrets/**), destructive commands and pushes to main.
3. Protected paths — what a hook must block (generated code, migrations, CI config, test files during a fix task).
4. Deterministic guards (hooks) — fast, scoped to the changed file; every block explains its reason and the route to approval.
5. Memory — the session-start block and the post-compaction checkpoint (ADR-0004, ADR-0008); Cursor's compaction point cannot be set. Optional OS isolation: `.cursor/sandbox.json`.
Write `.cursor/hooks.json`, `.cursor/hooks/*` and `.cursor/cli.json`, then record every choice in an ADR (`scope: engine` for the Cursor harness, `scope: product` for the agent).

## 2. Product agent harness (only if the product is or contains an agent)
Specify, ready to paste into docs/spec.md: tool allowlist; data it may read and write; actions that need human approval; spend and rate limits; memory scope and retention; prompt-injection defences (untrusted input stays data); a kill switch.

## 3. Prove it — an undrilled gate is not evidence
For every blocking hook or deny rule: a clean control that passes and a seeded violation that is blocked (a `deny` answer, or exit code 2). Add both to sdlc/checks/drill_cursor.py and paste the outputs into the ADR's Verification section. Retire a hook by saving hooks.json without it first, and delete its script only after Cursor has reloaded (tasks/lessons.md).

## Close
`auditor` (gate: harness — review loop per AGENTS.md, loop id `gate-harness`) → owner approval → phase 3 `approved` + a gate-log line → commit. Next step: `/sdlc-spec`.

## Done when
Hooks and permissions are committed, every blocking control has a recorded drill, and the ADR explains each choice in plain language.
