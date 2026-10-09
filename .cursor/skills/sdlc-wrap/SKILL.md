---
name: sdlc-wrap
description: End-of-session wrap-up — merge this session's handoffs into one session summary that follows the owner's principles, record lessons and changed decisions, refresh memory/now.md, and save a checkpoint. Use when the owner says goodbye, "დავასრულოთ", "wrap up", or before stopping a long piece of work.
---
# Session wrap

1. Gather the material: this session's handoff in `memory/episodic/handoffs/`; `git log` since the session started; `docs/sdlc-state.md`.
2. Write `memory/episodic/sessions/<YYYY-MM-DD>-<slug>.md` from `sdlc/templates/session-summary.md`, following the owner's principles in `memory/README.md`. Merge, do not concatenate: one coherent story, superseded content dropped.
3. Changed minds: compare today's decisions and instructions with the accepted ADRs and `memory/now.md`. List every reversal under "Changed minds" with the old and the new position, and propose a superseding ADR — do not write it without the owner's yes.
4. Lessons: corrections the owner made today go into `tasks/lessons.md` (date · what went wrong · rule · kind · check · in AGENTS.md). Classify each one: `mechanical` (a program could detect it) → propose a check in `sdlc/checks/` over a prose rule; `judgement` → a rule. Then run `python3 sdlc/checks/lessons_graduate.py` (exit 3 = proposals) and put every proposal to the owner — promote a lesson repeated on two dates into AGENTS.md, build the missing check. Nothing is promoted without the owner's yes.
5. Refresh `memory/now.md` (at most 120 lines): current phase, active decisions, open loops, next step. Delete what is no longer true.
6. If a phase, gate or milestone finished this session, append a dated entry to `PROGRESS.md` and refresh its Current State, In Progress and Next Steps. Never rewrite old entries.
7. Move merged handoffs to `memory/archive/handoffs/` — never delete them. Then run `python3 sdlc/checks/memory_hygiene.py` (exit 3 = actions due) and do what it lists: archive stale handoffs; when 10+ session summaries are live, write the monthly digest `memory/semantic/digests/YYYY-MM.md` and move those summaries to `memory/archive/sessions/`; report a reached QMD trigger or a due ADR review to the owner instead of acting on it.
8. Checkpoint commit: `chore(memory): wrap session <date>`.
9. Tell the owner in 3–4 Georgian sentences what we did, what comes next, and how to resume (`/sdlc-status`).
