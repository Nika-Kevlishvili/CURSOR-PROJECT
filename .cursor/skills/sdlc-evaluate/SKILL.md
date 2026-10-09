---
name: sdlc-evaluate
description: Phase 7 — design and run the evaluation layer: deterministic gates (tests, lint, types, secret and dependency scans, budgets) and evals (task-level checks of product or agent behaviour). Design mode runs right after the spec is approved, so the options fit the real product; run mode executes the suite during build and before release. Use when choosing how to test or evaluate, or to run the gates.
---
# Phase 7 — Evaluate (evals + gates)

Read `sdlc/design/evals-and-gates.md` first — it holds the option menu and the selection rules.

## Design mode — after phase 4 (Spec) is approved
1. From docs/spec.md list what must be proven: every acceptance criterion and every success criterion from the intent.
2. For each item propose the cheapest reliable check, deterministic before model-judged: unit/integration test → end-to-end or screenshot check → contract/schema check → static gate (lint, types, secrets, dependencies) → model-graded eval (only when behaviour cannot be checked deterministically, e.g. an agent's answers).
3. Present the menu to the owner grouped by layer, with cost and benefit in plain Georgian (the AskQuestion tool, recommendation first). The owner picks.
4. Record the choice as an ADR ("evaluation strategy"), write the deterministic gates to `docs/gates.json` (format in `sdlc/checks/run_gates.py`: id, layer, command as a list, and a `count_pattern` wherever "0 tests ran" could look green), and add the gate tasks to the plan.
5. Drill every new gate: a clean control passes, a seeded defect fails with a non-zero exit. An undrilled gate is not evidence. Also ask of every test or eval: would it pass on clearly wrong output? If yes, it does not discriminate — strengthen it.

## Run mode
1. Run `python3 sdlc/checks/run_gates.py` (exit 0 = every gate PASS, 1 = a gate FAILED, 2 = inconclusive), then the eval suite.
2. Report pass / fail per gate with the literal output lines. "Inconclusive" (could not run) is reported separately and never counts as pass.
3. A failing gate blocks the milestone or release. Fix the code, not the gate.

## Done when
Every acceptance criterion has a named, drilled check; the suite runs with one command; the latest green run is linked in docs/sdlc-state.md.
