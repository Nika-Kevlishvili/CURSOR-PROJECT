---
name: sdlc-status
description: Show where the project stands when the person asks "სად ვართ", "რა ეტაპზე ვართ", "what's next", or /sdlc-status. Do not run this at session start. Do not read memory/now.md or handoffs.
---
# SDLC status

1. Read `docs/sdlc-state.md` only. Do not read `memory/now.md`, `PROGRESS.md`, or anything under `memory/episodic/`. Those files are old notes and are not the current next step.
2. Run `git status --short` and `git log --oneline -5`.
3. Reply in Georgian prose, at most ~12 lines, no table unless asked:
   - the current phase and its status;
   - what was last approved, by whom and when;
   - what blocks the next gate (missing artifact, open auditor findings, an owner decision);
   - exactly one next step, with the command to run (e.g. `/sdlc-spec`).
4. If `docs/sdlc-state.md` and the repository disagree (the state says `approved` but the artifact is missing or uncommitted), report the mismatch plainly — never guess which side is right.
