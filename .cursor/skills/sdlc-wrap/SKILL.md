---
name: sdlc-wrap
description: End-of-session wrap-up only when the person explicitly says goodbye, "დავასრულოთ", "wrap up", or /sdlc-wrap. Do not run it because a chat is long or because Cursor compacted. Do not write a handoff for the next chat. now.md and handoffs are not loaded at session start.
---
# Session wrap

Run these steps only after the person asked for a wrap. Do not create a file under `memory/episodic/handoffs/`. Do not refresh `memory/now.md` for the next chat.

1. Gather the material from this conversation and `docs/sdlc-state.md`. Do not open `memory/now.md` or old handoffs to decide the next step.
2. Do not write `memory/episodic/sessions/`. A chat summary is not context for the next chat.
3. Changed minds: compare today's decisions and instructions with the accepted ADRs. List every reversal with the old and the new position, and propose a superseding ADR — do not write it without the person's yes.
4. Lessons: corrections the owner made today go into `tasks/lessons.md` (date · what went wrong · rule · kind · check · in AGENTS.md). Classify each one: `mechanical` (a program could detect it) → propose a check in `sdlc/checks/` over a prose rule; `judgement` → a rule. Then run `python3 sdlc/checks/lessons_graduate.py` (exit 3 = proposals) and put every proposal to the owner — promote a lesson repeated on two dates into AGENTS.md, build the missing check. Nothing is promoted without the owner's yes.
5. Do not refresh `memory/now.md`. It is not loaded into the next chat.
6. If a phase, gate or milestone finished this session, append a dated entry to `PROGRESS.md` and refresh its Current State, In Progress and Next Steps. Never rewrite old entries.
7. Do not move or write handoffs. Report a reached QMD trigger or a due ADR review only if the person asked for that check.
8. Do not commit from this skill unless the person asked for a commit.
9. Tell the owner in 3–4 Georgian sentences what we did, what comes next, and how to resume (`/sdlc-status`).
