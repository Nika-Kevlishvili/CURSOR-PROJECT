---
id: "0008"
title: Cursor only — the engine runs from AGENTS.md and .cursor/; the Claude Code files are removed
status: proposed
scope: engine
date: 2026-10-08
deciders: requested in a Cursor session on 2026-10-08 ("adapt the project to Cursor", then "delete the Claude files if their Cursor alternative is ready"); owner to approve — the change removes Claude Code support for the whole team
supersedes: "0005" in part (where hooks and settings live) and, for this engine, ADR-0004 decisions 1–2 and ADR-0006 decision 3 — when this ADR is accepted
superseded_by: —
review_by: after the first real Cursor session on this repo (the open checks under Verification)
tags: [engine, harness, cursor, hooks, agents]
---
# 0008 — Cursor only

## Context (კონტექსტი)
On 2026-10-08 the engine was asked to work in Cursor. Cursor 3.23.23 already loads much of a Claude Code setup on its own: CLAUDE.md (always applied), .claude/skills, .claude/agents and the hooks in .claude/settings.json (Settings → Agents → Third-Party Imports, on by default). A live probe in a Cursor session — an observation-only hook outside the repository that logged every payload — showed that "loads" is not "works":

1. Claude-registered hooks receive Cursor's own payload: a shell command as tool `Shell`, not `Bash`, camelCase event names, a `cursor_version` field. guard.py checked `tool == "Bash"` and commit_secrets.py `tool_name == "Bash"`, so in Cursor both let every command through — `rm -rf`, force push, push to main, an unscanned commit — while looking configured. Edits arrive as `Write` with `tool_input.file_path`; before a Write, Cursor first asks about a `Read` of the same file.
2. Cursor ignores the `tools:` line of .claude/agents/*.md (it has no tool lists, only `readonly`), and its hook payload names no agent type. The auditor, verifier and architect would run in Cursor with write access. A subagent's tool calls do carry `parent_tool_call_id`, equal to the `subagent_id` that `subagentStart` announces.
3. Cursor has no post-compaction hook and never shows a hook the summary; hooks get `transcript_path: null`, and Cursor's transcripts hold no token counts. The PostCompact handoff (ADR-0004) and the context monitor (ADR-0006) cannot run. `preCompact` reports the context percentage just before a compaction; the compaction point is not configurable.
4. Cursor cannot pin the main model from the repository (the model picker decides) and has no advisor tool (ADR-0007). Subagents can be pinned in front matter (`model: claude-opus-5-5[effort=high]`); all four Claude 5.5 models are offered.
5. When both .cursor/hooks.json and .claude/settings.json register hooks, Cursor runs all of them — every tool call reached both.
6. The IDE has no checked-in allow / ask / deny list: .cursor/cli.json applies to the Cursor CLI only. A hook can answer `ask` on `beforeShellExecution`, but on `preToolUse` (file tools) only `allow` or `deny`.

## Options considered (განხილული ვარიანტები)
### A — Cursor only: rules in AGENTS.md, everything else in .cursor/, the Claude Code files removed (chosen)
- What it is (one plain sentence): one harness, in Cursor's formats, with the same checks the Claude Code harness ran.
- Pros: one set of files and one hook source — nothing runs twice, no compatibility layer to keep in step; AGENTS.md is also what Codex and other agents read.
- Cons: the owner and the collaborator, who used Claude Code, must switch to Cursor; ADR-0004/0005/0006/0007 mechanics change for this engine (below).
- Cost (money + learning): medium — the drills had to be ported to Cursor's payload; the team learns Cursor.
- Risk: medium for the team, low for safety (the guards are drilled in Cursor's real payload shape).
- Reversibility (how hard to switch later): medium — the Claude Code harness is in git (commit `ba06fac` has both).
- Source checked: the probe; https://cursor.com/docs/hooks, https://cursor.com/docs/subagents, https://cursor.com/docs/skills
### B — Both tools: Cursor-native wiring around the same checks, the Claude Code files kept
- What it is (one plain sentence): .cursor/ calls the checks in .claude/hooks/ through an adapter; the Claude-registered hooks stay silent on a Cursor event.
- Pros: nobody has to switch tools.
- Cons: two front-matter copies of every agent, a runtime-mapping rule, a no-op guard in every Claude hook; Cursor still loads and runs the Claude-registered hooks.
- Cost (money + learning): low.
- Risk: low.
- Reversibility (how hard to switch later): easy.
- Source checked: the probe. Built and verified first (commit `ba06fac`: structure 470 PASS, drill 218 ARMED, stub DEAD), then replaced by A at the requester's instruction.
### C — Rely on Cursor's Claude compatibility as it is
- What it is (one plain sentence): change nothing.
- Pros: free.
- Cons: point 1 of the context — the guards are silently off in Cursor; the read-only agents can write; the memory hooks do not run.
- Cost (money + learning): none now; a silent safety gap.
- Risk: high.
- Reversibility (how hard to switch later): —
- Source checked: the probe. Rejected on evidence.

## Decision (გადაწყვეტილება)
Option A, proposed to the owner. A Claude Code file was deleted only once its Cursor replacement existed and was drilled:

| Was (Claude Code) | Now (Cursor) |
|---|---|
| CLAUDE.md | AGENTS.md — the same rules, in Cursor's terms (model picker, `advisor` agent, AskQuestion, post-compaction checkpoint) |
| .claude/skills/sdlc-* | .cursor/skills/sdlc-* — moved, wording adapted (`/sdlc-*` works as before) |
| .claude/agents (model, effort, tools) | .cursor/agents — the same prompts, `model: claude-…-5-5[effort=…]`, `readonly: true`, plus `advisor` |
| advisor tool (`advisorModel`) | the `advisor` subagent, Opus 5.5, at the same three moments |
| `model` + effort in settings.json | the model picker; the session-start ASTERBIT-RUNTIME line names the running model and warns if it is not Sonnet 5.5 |
| PreToolUse → guard.py, commit_secrets.py | beforeShellExecution → cursor_adapter.py `shell` (block / ask / allow, failClosed); preToolUse → `tool` (secret files, read-only agents through parent_tool_call_id, failClosed) |
| permissions `deny` | the hooks; .cursor/cli.json `deny` for the CLI; Cursor's default `.env*` ignore |
| permissions `ask` (installs, curl/wget, WebFetch, gh pr merge, edits to settings/hooks/CI) | the guard asks before installs (brew, pip, npm, npx), curl/wget and `gh pr merge`; WebFetch follows Cursor's own approval setting; file edits to .cursor/hooks.json, .cursor/hooks/ and .github/ rest on the AGENTS.md rule and PR review — a hook cannot ask on a file tool |
| permissions `allow` | .cursor/cli.json `allow` for the CLI; the IDE asks per Cursor's run mode (an optional `.cursor/permissions.json` allowlist is the owner's call) |
| SessionStart → memory_context.py | sessionStart → `session-start` (the same build_context, newest handoff by time) |
| PostCompact → memory_handoff.py; context monitor at 55%; `autoCompactWindow` 65% | preCompact logs the compaction and arms a request; the next main tool call asks the agent to write the checkpoint (AGENTS.md "Checkpoints"); Cursor's compaction point is its own |
| PostToolUse(+Failure) → event_log.py, .claude/logs | postToolUse(+Failure) and subagentStart/Stop → event_log.py, .cursor/logs, with host and model |
| transcript_usage.py, `agent_report.py --transcript` | removed — Cursor transcripts hold no token counts; cost comes from Cursor's usage page |

What changes for the other ADRs, here rather than by rewriting them: ADR-0004 decisions 1–2 (65% and the PostCompact handoff) and ADR-0006 decision 3 (the 55% checkpoint) do not hold for this engine; ADR-0005's settings file and hook location become .cursor/; ADR-0007's main-model pin and advisor tool become the model picker with a warning and the `advisor` subagent — its roles and models are unchanged.

## Rationale (რატომ)
The strongest reason is evidence: as it stood, the engine's main safety guard was silently off in Cursor. Keeping both tools (B) works and was verified, but it doubles the agent files, needs a translation rule and leaves two hook sources running; once the requester chose Cursor alone, one harness is simpler and the drills now exercise exactly what runs. Hooks remain the enforcement ("the skill makes violations rare and the hook makes them close to impossible"); Cursor's readonly flag and .cursor/cli.json are a second layer.

## Consequences (შედეგები)
- Positive: the same blocks as before, enforced in Cursor and drilled in its real payload shape; read-only agents enforced twice; the event log records which model each subagent ran on, so which model ran is a log line; installs and internet fetches from the shell are held for a yes again; Cloud agents get the guards.
- Negative: Claude Code is no longer supported — the owner and the collaborator need Cursor. The checkpoint comes after a compaction, from the summary, so detail the summary dropped is gone, and there is no early warning. File edits to the harness and CI cannot be held for approval by a hook. The main model is a convention plus a warning. Hooks still need `python3`.
- What becomes harder: a Cursor update that renames payload fields would weaken the adapter; the drill pins the field names it relies on, but only a live session proves them. Going back to Claude Code means restoring the files from `ba06fac`.
- Open, for the owner: `.cursorignore` — Cursor does not let its agent write that file, so a person adds it if wanted (the hooks already block secret-file reads and writes); a project `.cursor/sandbox.json` (assessment B3) stays a separate decision.

## Verification (როგორ შევამოწმებთ)
- Stage B (both tools, commit `ba06fac`): `check_structure.py` 470 checks PASS (380 before); `drill_hooks.py` 218 cases ARMED (138 before; 80 in drill_cursor.py; 1 skipped — gitleaks is not installed on this machine, CI installs it); with `ASTERBIT_DRILL_STUB=1` DEAD — 73 of the 80 Cursor cases fail.
- Live in Cursor 3.23.23 (stage B), with temporary hooks outside the repository pointing at the adapter: `echo` passed; `git -C … push --force` and `rm -rf …` were blocked with ASTERBIT-GUARD; a Write to `.env` was blocked through the Read pre-check; a generalPurpose subagent's Read and Shell calls were logged under `general-purpose` through parent_tool_call_id, with its model.
- Stage A (Cursor only): `check_structure.py` 398 checks PASS (93 files); `drill_hooks.py` 166 cases ARMED — every Claude-era guard, ask, secret-scan, memory and redaction case ported to Cursor's payload, plus asks for installs, curl/wget and `gh pr merge` (1 skipped: gitleaks); with `ASTERBIT_DRILL_STUB=1` DEAD, and no hook case passes in stub mode.
- Found on the way: deleting a loaded workspace hooks.json does not unload it until the window reloads, and a missing hook script makes `python3` exit 2, which Cursor reads as "block" (tasks/lessons.md).
- Still to verify in the first real Cursor session with this repo as the workspace root: (1) the session-start block appears in a new chat; (2) a custom agent's `subagent_type` is its name (`auditor` …) — the event log shows it; (3) the `/sdlc-*` skills are listed; (4) each agent's `subagent_model` in the log is the pinned model; (5) a checkpoint request arrives after a real compaction.

## Links
- [AGENTS.md](../../AGENTS.md) · [.cursor/hooks/cursor_adapter.py](../../.cursor/hooks/cursor_adapter.py) · [sdlc/checks/drill_cursor.py](../../sdlc/checks/drill_cursor.py)
- [ADR-0004](0004-context-and-memory.md) · [ADR-0005](0005-harness-v0.md) · [ADR-0006](0006-engine-v2.md) · [ADR-0007](0007-claude-only-orchestration.md)
- https://cursor.com/docs/hooks · https://cursor.com/docs/reference/third-party-hooks · https://cursor.com/docs/subagents · https://cursor.com/docs/skills · https://cursor.com/docs/cli/reference/permissions · https://cursor.com/docs/models
