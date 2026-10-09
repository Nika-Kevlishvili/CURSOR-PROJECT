# Tasks — Engine v0

> ახლა ეს ფაილი ძრავის აწყობას ემსახურება. Phase 5-ში (Plan) აქ პროდუქტის დავალებები ჩაიწერება, ძრავის დასრულებული დავალებები კი „Done log"-ში დარჩება.
> Rule: a box is ticked only with evidence (command + result, or commit).

## Engine v0
- [x] Study Anthropic's AI-native SDLC playbook (2026-08-21) → sdlc/research/playbook-notes.md
- [x] Evaluate the awesome-ai-pulse-georgia collection → sdlc/research/engine-selection.md
- [x] Verify harness facts in the Claude Code docs (bundled v2.1.289): compaction window, PreCompact/PostCompact, advisor, effort, subagent and skill front matter
- [x] Create the directory: CLAUDE.md, README.md, phase skills, subagents, templates, design docs, ADR-0001…0004
- [x] PROCESS.md (working agreement) + PROGRESS.md (work log) — added 2026-10-06 after the owner noticed they were missing
- [x] Memory & context interview → ADR-0004 accepted (2026-10-06: 65% · Obsidian vault · grep → QMD at trigger · ADR + git + wrap check)
- [x] Handoff / session-summary principles → memory/README.md (owner chose Claude's eight principles, 2026-10-07)
- [x] Collaborator access + standalone repo: lashavamleti invited with write access (GitHub invitation 2026-10-07); CONTRIBUTING.md, docs/FILES.md; CLAUDE.md/PROCESS.md People + Safety
- [x] Scaffold merged into `main` as the shared base — PR #1, merge commit (owner's decision 2026-10-07)
- [x] Harness v0 (ADR-0005): .claude/settings.json + guard / commit-secrets / memory hooks — drill 89 cases ARMED, stub DEAD; live: `.env.probe` write and verifier `touch` blocked (2026-10-07)
- [x] Structure check in the repo (`sdlc/checks/check_structure.py`) — 269 checks PASS; seeded copy: 3/3 defects caught (now a drill case)
- [x] CI: `.github/workflows/checks.yml` (structure, drill, stub drill, gitleaks) — green on PR #2, all 7 steps ran (https://github.com/tornikebolokadze1-cyber/Asterbit/actions/runs/37601321945)
- [x] Live compaction → handoff appears in memory/episodic/handoffs/ — proven 2026-10-07 23:00 by a real auto compaction: the checkpoint went to `memory/archive/handoffs/2026-10-07-df3e2417.v2.md`, the new live handoff has `compaction: 3` and `injection_flags:`, SessionStart re-injected memory inside a nonce fence. Found on the way: subagent compactions overwrite it (A2)
- [x] Research of Anthropic's current guidance + independent engine assessment (2026-10-08): `sdlc/research/anthropic-guidance-2026-10.md`, `claude-models-2026-10.md`, `engine-assessment-2026-10.md` (auditor: "READY FOR DRY RUN: no — 6 blocking items"); ADR-0007 proposed
- [x] Owner chose ADR-0007's roles: **A** (2026-10-08); Codex and CodeRabbit stay installed for now (superseded the same day: Codex is removed, CodeRabbit is reviewed later); `scout` (Haiku 5.5) created after `claude update` (CLI 2.1.293, VS Code extension 2.1.294)
- [ ] Owner reviews ADR-0001…0003, ADR-0005…0009 and PROCESS.md → accepted (Phase 0 gate; the owner cancelled the dry run, 2026-10-08)
- [ ] Start the real product: `/sdlc-intent`

## Assessment blockers A1–A6 (auditor, 2026-10-08 — details in `sdlc/research/engine-assessment-2026-10.md`)
> Hook, settings and CLAUDE.md changes need the owner's yes (CLAUDE.md Safety).
> Fixed 2026-10-08 on `engine/dryrun-blockers` (owner's yes). Every new drill case below was run against the old code from `origin/main` first and was MISSED there (7 of 7), then WORKS on the new code.
- [x] A1 (HIGH) Dry-run rule in the gate rules and PROCESS.md §2 (stage 0), and a line in `sdlc-harness/SKILL.md`: on `dryrun/*` phases 1–6 run while Phase 0 is in progress; approvals `owner (DRY-RUN), <date>`; Phase 3 proposes a harness diff in docs/ and changes nothing live; the branch is never merged — the rule was removed again on 2026-10-08, when the owner cancelled the dry run (PR #10; on this branch the rule lived in AGENTS.md)
- [x] A2 (HIGH) `memory_handoff.py`: a compaction with `agent_id` or a `subagents/` transcript is skipped and logged; every PostCompact writes its input key names to `.claude/logs/compactions.jsonl` (the next real compaction confirms which fields Claude Code sends). Drill: "subagent compaction … left as it is" + clean control "agent_type alone → handoff replaced"
- [x] A3 (MEDIUM) `memory_context.py`: `compact` → no handoff; `resume` → own handoff; `startup` → newest by time. The two old `compact` drill cases now use `resume` (same checks); new cases "compact: … no handoff" and "startup → the newest handoff by time"
- [x] A4 (MEDIUM) New shared helper `.claude/hooks/transcript_usage.py` (last `type == "message"` round) used by `context_monitor.py` and `agent_report.py`; output counted once per `message.id`. Drills: two 300k rounds → silent; a real 56% → "56%", not 112%; agent_report → "context now 300,000; output so far 200"
- [x] A5 (MEDIUM) `check_structure.py`: `memory/episodic/` and `memory/archive/` are stored data, not link-checked. Drill: a broken link in docs is reported, one inside archived memory is not
- [x] A6 (MEDIUM) `claude update` done (CLI 2.1.293, VS Code extension 2.1.294 — active after a VS Code reload). The new-session and model-recording steps were part of the dry-run rule (A1, removed 2026-10-08); engine agents are spawned without a `model` parameter passed at call time

## Cursor (ADR-0008, proposed — local branch `engine/cursor-support`, not pushed)
> Hook, agent, rule and AGENTS.md changes need the owner's yes (AGENTS.md Safety); the branch is a proposal until the owner merges it. Nothing is pushed to GitHub without the owner's (and the requester's) yes.
- [x] Live probe in Cursor 3.23.23 (observation-only hook outside the repo): Claude-registered hooks get tool `Shell`, so guard.py and commit_secrets.py passed everything in Cursor; edits are `Write` / deletions `Delete` with `tool_input.file_path`; a subagent's calls carry `parent_tool_call_id`; `transcript_path` is null and transcripts hold no token counts; both hook sources run on every call
- [x] Both tools (commit `ba06fac`): `.cursor/hooks.json` + `.cursor/hooks/cursor_adapter.py`, `.cursor/agents/` (+ `advisor`), runtime rule, `.cursor/cli.json`; check_structure.py Cursor checks; `sdlc/checks/drill_cursor.py` — structure 470 PASS, drill 218 ARMED, stub DEAD; live: force push, `rm -rf` and a `.env` write blocked
- [x] Cursor only (requested 2026-10-08: "delete the Claude files if their Cursor alternative is ready"): the checks moved to `.cursor/hooks/`, the skills to `.cursor/skills/`, the rules into `AGENTS.md`; the guard now also asks before installs, `curl`/`wget` and `gh pr merge` (the old `.claude/settings.json` ask list); then `CLAUDE.md`, `.claude/settings.json`, `.claude/agents/` and the Claude-only hooks (`memory_handoff.py`, `context_monitor.py`, `transcript_usage.py`, `hook_host.py`) were deleted — recoverable from `ba06fac`
- [ ] Owner: review the branch (it removes Claude Code support for the whole team) and decide whether to push and merge it
- [ ] First real Cursor session with `Asterbit` as the workspace root: (1) the ASTERBIT-RUNTIME / memory block appears in a new chat; (2) a custom agent's `subagent_type` in `.cursor/logs/events-*.jsonl` is its name (`auditor` …) — if not, read-only enforcement rests on Cursor's `readonly` flag alone; (3) `/sdlc-*` skills are listed; (4) `python3 sdlc/checks/agent_report.py` shows each engine agent on its pinned model; (5) after a real compaction the checkpoint request arrives
- [ ] Owner's call: add `.cursorignore` by hand (Cursor does not let its agent write it); `.cursor/sandbox.json` together with assessment B3; an IDE allowlist (`.cursor/permissions.json`) if prompts get tiresome
- [ ] Known gap (Cursor cannot "ask" on a file tool): edits to `.cursor/hooks.json`, `.cursor/hooks/` and `.github/` rest on the AGENTS.md rule and PR review; web fetches on Cursor's own approval setting

## Parked (გადადებული)
- [ ] **Security gap, owner's yes needed (found 2026-10-07):** `.cursor/hooks/commit_secrets.py` (was `.claude/hooks/`) scans the repository of the session's `cwd`, not the one a command switches to — `cd <other repo> && git commit …` or `git -C <dir> commit …` is not scanned (ADR-0005 hook). Seen while committing from a git worktree; the commits were then scanned by hand (gitleaks 3 commits, built-in patterns: no leaks). Fix: resolve the target directory from `cd`/`-C` in the command, plus drill cases
- [x] Flaky drill case (found 2026-10-07): `commit_secrets` "generic key only gitleaks knows" MISSED in 1 of 4 local runs. Fix from PR #10: `generic_key_value()` (digits and letters alternate). The 20-of-20 gitleaks run was on the owner's machine; this machine has no gitleaks, so the case stays SKIPPED here
- [ ] Auditor LOW (ENG-v2-P1P2 review 1): requirements in `docs/changes/<n>/` are traced against the root `tasks/todo.md`, so a change's requirement can look traced by an unrelated product task with the same ID — use a change-local todo or prefixed IDs when the first change folder appears
- [x] Auditor note (ENG-v2-P1P2 review 1): `review_rounds.py` checks SHA format only — **done with PR #10:** every SHA must be a commit in `--repo`, a review's base an ancestor of its head, a fix must build on the reviewed head; a folder that is no git repository → INCONCLUSIVE. The drill uses real commits in a scratch repository
- [x] ~~Stage-2 orchestration (Fable 5.1; OpenAI `gpt-6.1-sol`, `gpt-6-astra` via Codex; an open-weight model)~~ — dropped: the owner decided Claude-only (2026-10-07) and chose ADR-0007 option A (2026-10-08)
- [ ] Code graph: Graphify when product code exists, codebase-memory-mcp at ~50 source files — new ADR at that point (owner's choice 2026-10-07, ADR-0006)
- [ ] Engine regression evals: 20–50 real tasks, run on every change to AGENTS.md / .cursor/** — CI exists (PR #2); built from the first real product tasks — the dry run was cancelled (2026-10-08), so nothing to build them from yet (sdlc/design/evals-and-gates.md §5)
- [x] Auditor LOWs (ENG-v2-P3P6 review 1), from PR #10: `run_gates.py` a bad gate config → INCONCLUSIVE, never FAIL; `agent_report.py` skips a corrupt log line and counts it, and a transcript without usage → INCONCLUSIVE, never "context now 0". (`memory_handoff.py` versioning stayed parked: that file is gone on this branch, ADR-0008)
- [x] ~~Auditor LOW (ENG-v2-P3P6 review 2): `context_monitor.py` skips any event with `agent_type`~~ — obsolete: `context_monitor.py` was Claude-only and is deleted (ADR-0008)
- [x] Observation (ENG-v2-P3P6 review 2): `guard.py` blocked `git stash list` for read-only agents — **owner: „მივცეთ უფლება" (2026-10-08, PR #10):** `git stash list` / `show` are allowed; `git stash` and `git stash pop` stay blocked
- [x] ~~Claude Code is 2.1.292 here … (owner's call)~~ — obsolete: the engine runs in Cursor (ADR-0008)
- [ ] Auditor recommendations B1–B11 (2026-10-08) — `sdlc/research/engine-assessment-2026-10.md`. Done on PR #10 and brought here: B1 (AGENTS.md and the skills win over user-level rules), B2 (every `rm` run as a command and every `git rm` asks; `grep rm` does not), B5 (the drill copy includes untracked, non-ignored files). B4 (`commit_secrets` follows `cd` / `git -C`) is done on `main` (PR #8) and not yet on this branch. Open: B3 → ADR-0009, the owner picks an option; B6 and B8 — the owner wants to agree the exact change first; B7 not run
- [ ] B8 / `/sdlc-wrap` — owner, 2026-10-08: the wrap and its commit may run by themselves, but the wrap's logic must be agreed with the owner first
- [ ] B6 / `untrusted.py` false alarm on a quoted fence marker — owner, 2026-10-08: agree the exact change, then do it
- [x] Global UserPromptSubmit hook calls MemPalace — **owner: „გათიშე" (2026-10-08), done on the owner's machine** (PR #10): that entry removed from `~/.claude/settings.json`, backup `~/.claude/settings.json.backup-20261008-165715`. Not re-checked on this computer. While Third-Party Imports is on, Cursor still reads that file — CONTRIBUTING.md §3
- [x] Codex — **owner: delete it (2026-10-08), done on the owner's machine** (PR #10): plugin and marketplace uninstalled, personal `/codex*` commands moved aside. The Codex CLI program stays until the owner says otherwise. Not re-checked on this computer
- [ ] CodeRabbit — the owner decides later (2026-10-08)
- [x] Handoff cut (owner, 2026-10-08, PR #10): `memory_context.py` gives the handoff its own budget (6 000 of 16 000 characters) and cuts only the middle, so the next step at the end still arrives
- [ ] Context monitor's false alarm on a fresh session (fixed in PR #10's `context_monitor.py`) — not brought over: that file was removed with the Claude Code harness (ADR-0008). Cursor does not measure transcript tokens

## Done log
- 2026-10-06 — repository initialised; branch `engine/v0-scaffold`.
- 2026-10-07 — lashavamleti invited; repo made standalone; PR #1 merged into `main`.
- 2026-10-07 — harness v0 built on `engine/v1` (ADR-0005); lashavamleti accepted the invitation.
