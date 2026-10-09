---
name: sdlc-status
description: Show where the project stands in the SDLC — current phase, gate statuses, what blocks the next gate, and the single next step. Use at session start, when the owner asks "სად ვართ / რა ეტაპზე ვართ / what's next", or before starting any phase.
---
# SDLC status

1. Read `docs/sdlc-state.md`, `memory/now.md`, the Current State / In Progress / Next Steps sections of `PROGRESS.md`, and the newest file in `memory/episodic/sessions/` (skip whatever does not exist yet).
2. Run `git status --short`, `git log --oneline -5` and `python3 sdlc/checks/memory_hygiene.py` (exit 3 = memory actions due — mention them in one line).
3. If the previous session has no summary in `memory/episodic/sessions/`, say so and offer to write it first (`/sdlc-wrap`).
4. Reply in Georgian prose, at most ~12 lines, no table unless asked:
   - the current phase and its status;
   - what was last approved, by whom and when;
   - what blocks the next gate (missing artifact, open auditor findings, an owner decision);
   - exactly one next step, with the command to run (e.g. `/sdlc-spec`).
5. If `docs/sdlc-state.md` and the repository disagree (the state says `approved` but the artifact is missing or uncommitted), report the mismatch plainly — never guess which side is right.
