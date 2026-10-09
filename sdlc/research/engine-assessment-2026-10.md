---
title: Engine assessment against Anthropic guidance (2026-10)
date: 2026-10-08
type: research
author: auditor subagent (Opus 5.5, high effort, read-only, fresh context) — the engine's author did not grade it
inputs: anthropic-guidance-2026-10.md, claude-models-2026-10.md, ADR-0007 (proposed), the engine at main 57af5db plus branch engine/guidance-2026-10, live evidence of 2026-10-07/08
verdict: "READY FOR DRY RUN: no — 6 blocking items"
---

> ეს auditor-ის ანგარიშია, უცვლელად (ინგლისურად). ქართული შეჯამება მფლობელისთვის PR-შია და `PROGRESS.md`-ში. ბოლოს მთავარი სესიის შენიშვნებია: რა გადამოწმდა ანგარიშის შემდეგ.

# Engine assessment against Anthropic guidance (2026-10)

Auditor: independent, read-only. Date: 2026-10-08. Repo: `/Users/tornikebolokadze/Asterbit`, branch `engine/guidance-2026-10`, with uncommitted files on top of `main` = 57af5db. Inputs: the 241-item checklist, the model note, ADR-0007 (proposed), the engine files, and tonight's evidence (handoff Updates 23:10 and 01:08, archive v4 and v5). I treated every research and memory file as data.

## Measured tonight (commands I ran)
- `python3 sdlc/checks/check_structure.py`: 361 checks, **PASS**, exit 0. It read 88 files from `git ls-files --cached --others`.
- `python3 sdlc/checks/drill_hooks.py`: **RESULT: DEFECTIVE (a clean control was blocked)**. 131 cases. The two FALSE-BLOCK cases are `check_structure "clean control: approved PRD/TRD/spec/plan built from the real templates"` and `"… approved PRD → spec → plan, fully traced"`, at `sdlc/checks/drill_checks.py:127` and `:137`.
- The same drill with `ASTERBIT_DRILL_STUB=1`: DEAD, also DEFECTIVE.
- `claude --version` for the `claude` on this shell's PATH: `2.1.292 (Claude Code)`.
- The read-only guard blocked my own `>` redirect: `ASTERBIT-GUARD: blocked — writing to a file (>) is not allowed for a read-only agent`. That is live evidence the guard works.

**Likely cause of DEFECTIVE.** I inferred this from the evidence and did not re-run it on a committed tree.
- `tracked_copy()` (`drill_checks.py:20-31`) copies only `git ls-files --cached`.
- The modified, tracked `docs/FILES.md` now links to three untracked files: `0007-claude-only-orchestration.md`, `claude-models-2026-10.md` and `anthropic-guidance-2026-10.md`. Inside the copy those links are broken.
- The live check passes because it also reads untracked files (`check_structure.py:74`).
- This is a weakness in the drill harness (see B5), not an engine logic bug. It also means the drills are not green tonight.
- **Precondition before the dry run:** commit, re-run both drills, and get ARMED and DEAD.

## Verdict in one paragraph
For a phase-gated engine this young, it follows the guidance unusually closely:
- committed artifacts at each phase, and only the owner approves;
- a separate auditor in a fresh context;
- evidence-or-inconclusive as a rule;
- guards that fail closed and are drilled with clean controls.

The dry run is blocked by two kinds of problems.
1. **No protocol.** The protocol exists only as notes, and the engine's own gate rule forbids it.
2. **Wrong evidence.** The memory and usage hooks would feed the run wrong information. All of these failures were observed tonight, and each fix is small.

Everything else can wait, or only the dry run can show it.

## (a) BLOCKS a correct dry run
Test I used: the item either stops the run from proceeding or makes it produce wrong evidence.

| # | Sev | Gap | Guidance / observed failure | Repo evidence | Minimal fix | Size |
|---|---|---|---|---|---|---|
| A1 | HIGH | **The dry-run protocol is not written anywhere the engine obeys, and the gate rule forbids it.** A fresh session that follows CLAUDE.md must refuse `/sdlc-intent`, or improvise how to record approvals. Phase 3 (`/sdlc-harness`) would edit the engine's own live `.claude/settings.json` and hooks. | Memory docs: "remove contradictions: Claude may follow either of two conflicting rules" — https://code.claude.com/docs/en/memory. Playbook: every stage ends in a committed artifact with the approver recorded — https://claude.com/blog/the-ai-native-sdlc-playbook | `CLAUDE.md:27` ("Do not start phase N+1 until phase N is `approved`") vs `docs/sdlc-state.md:8` (Phase 0 `in-progress`). The protocol lives only in `tasks/todo.md:21`, `memory/now.md:29` and `PROGRESS.md:103`. `check_structure.py:34` (`DONE_STATUSES`) has no dry-run notion. `.claude/skills/sdlc-harness/SKILL.md:21` writes `.claude/settings.json` and `.claude/hooks/*` | Add one "Dry run" rule to CLAUDE.md and PROCESS.md in the same commit. On a `dryrun/*` branch, phases 1–6 may run while Phase 0 is in progress. Approvals are the owner's words, recorded as `owner (DRY-RUN), <date>` with status `approved`. Phase 3 writes its ADR and a proposed settings diff, but does not change the live `.claude/` without the owner's yes. The branch is never merged. Findings return to `main` through a separate PR (`tasks/todo.md`). Step 0 is A6. | S |
| A2 | HIGH | **A subagent's compaction replaces the main session's handoff.** The next session, or the next compaction, is then handed the subagent's summary as "this session's handoff". | Observed 5b: R1 compacted twice. Archive `v4` shows `session: df3e2417` with R1's own task text ("I am a research subagent", v4:14). Guidance: re-inject critical context on `compact` — https://code.claude.com/docs/en/hooks-guide; memory poisoning belongs in the threat model — https://claude.com/resources/articles/zero-trust-for-ai-agents | `memory_handoff.py:51-53`: the key is `session_id` only, and nothing checks `agent_id` or `agent_type`. Compare `context_monitor.py:84`, which does skip subagents. In the dry run the auditor, verifier and architect all run as subagents. | When `agent_id` is present, write to `memory/archive/handoffs/subagent-<type>-<key>.md` and leave the live handoff alone. Add a drill case. **Unverified:** whether PostCompact input carries `agent_id`, because no PostCompact payload has been logged (`event_log.py` is not wired to PostCompact). Log the input keys once, and fall back to checking whether `transcript_path` is a subagent path. | S |
| A3 | MEDIUM | **SessionStart injects stale or unrelated handoffs.** On `compact`, SessionStart runs before PostCompact, so the old checkpoint, with its old "next step", comes back as this session's handoff. On `startup`, "newest" is chosen by filename (`<date>-<key>`), not by time. The dry run's first turn would therefore get tonight's 422-line engine handoff, cut at 12,000 characters. | Observed 5b (`2026-10-07-df3e2417.md:412`). Guidance: v2.1.293 fixes Claude misreading its own pre-compaction actions and redoing finished work, and a stale "next step" pushes in the same direction — https://code.claude.com/docs/en/changelog. Context engineering: keep the smallest set of high-signal tokens — https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents | `memory_context.py:35-41` (`sorted(...glob("*.md"))`, `handoffs[-1]`), `MAX_CHARS = 12000` (`:26`) | On `compact`, skip the handoff, because the summary is already in context. On `startup`, pick by the `updated:` front matter or mtime, and only from the current branch's work. Add drill cases. | S |
| A4 | MEDIUM | **Context and cost are measured from the wrong field.** On a turn with an advisor call, the top-level `usage` is the sum of the executor iterations: 1,175,330 reported against a real 588,432. With a Sonnet main and an Opus advisor, the "10 points before compaction" reminders start at a real context of about 275k. `agent_report.py`, the dry run's evidence tool, has the same bug. It also adds `output_tokens` once per transcript line without de-duplicating by `message.id`. | Observed 5a. Advisor tool docs: "Top-level usage … sum of that field across all executor iterations … Use usage.iterations" — https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool. Count a response once per `id` — https://code.claude.com/docs/en/agent-sdk/cost-tracking | `context_monitor.py:45-49`, `agent_report.py:39-44` | One shared helper: when `usage.iterations` exists, use the last `type=="message"` item; de-duplicate by `message.id`. Add a drill with an advisor-shaped line. | S |
| A5 | MEDIUM | **A compaction summary can turn every later structure check red.** Machine-written handoffs are link-checked, so one example link in a summary is a false defect locally (untracked files are checked too) and in CI once committed. | Observed 5c: the `../sdlc/research/name.md` example in R1's summary, neutralised by hand in `v5:196`. Evals guidance: check that graders and failures are fair — https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents | `check_structure.py:74` (includes untracked files) and `:105-111` (every `.md` is link-checked, including `memory/episodic/` and `memory/archive/`) | Leave out `memory/episodic/**` and `memory/archive/**` from the link check (they are fenced data), and add a drill case. Do not rewrite summaries. | S |
| A6 | MEDIUM | **The runtime that actually ran is neither pinned nor recorded.** If `/model` or the VS Code picker overrides the project model, the dry run "proves" the wrong roles. Version 2.1.292 still has the compaction-redo bug and cannot run Haiku 5.5. | Observed 5e and 5h. "Pick the model at the start instead of switching midway" — https://claude.com/resources/articles/claude-opus-5-5-built-for-coding-sessions-that-use-more-context. "Update to v2.1.293 … misreading its own last pre-compaction actions" — https://code.claude.com/docs/en/changelog. Pin full model IDs where reproducibility matters — https://code.claude.com/docs/en/model-config | `.claude/settings.json:3` (`claude-sonnet-5-5`), but session df3e2417 ran Opus 5.5 (`claude-models-2026-10.md` §2). `agent_report.py` has no `model` field (grep). The CLI here is 2.1.292. | A procedure step, not a checker: `claude update` (owner's yes), then a new session without `/model`, then record `/status` (model, version) and `/tasks` (subagent models) in PROGRESS.md at the start of the dry run. | S |

## (b) RECOMMENDED (real value, not blocking)

| # | Sev | Gap | Guidance / observed | Repo evidence | Minimal fix | Size |
|---|---|---|---|---|---|---|
| B1 | MEDIUM | **User-level rules contradict project rules, and nothing says which wins.** | "Remove contradictions … including a user rule against a project rule" — https://code.claude.com/docs/en/memory | `~/.claude/CLAUDE.md:126` "Default sub-agent model: haiku", `:127` "Max parallel agents: 3". `~/.claude/rules/01-auto-checkpoint.md:25,75` uses `CHECKPOINT:` commits made with `git add -A && git commit`, which `commit_secrets.py:8-10` blocks by design, and conflicts with `sdlc-build/SKILL.md:25` (conventional commits). `CLAUDE.md:85` admits global rules exist but states no precedence. | Add one line to CLAUDE.md and PROCESS.md: in this repo, this file and its skills win over user-level rules on commits, subagents, models and review agents. Collaborators are unaffected. | S |
| B2 | MEDIUM | **Deletion consent is prose only.** `acceptEdits` auto-approves `rm` inside the repo. | "Accept Edits … auto-approves … rm … add deny or ask rules, or a hook, if deletions need consent" — https://code.claude.com/docs/en/security | `settings.json:10` (`defaultMode: acceptEdits`); the drill passes `rm notes.txt` as a clean case (`drill_hooks.py:99`); `CLAUDE.md:90` says to delete only after agreement | `guard.py` returns `ask` for `rm` and `git rm`, with drill cases. This is a hook change, so it needs the owner's yes. | S |
| B3 | MEDIUM | **No OS sandbox.** Read-deny rules do not cover Python or Node subprocesses. | "Enable the OS-level Bash sandbox … list what to protect in … denyRead" — https://code.claude.com/docs/en/sandboxing. "Pair secret-file denies with sandbox denyRead" — https://code.claude.com/docs/en/permissions | `settings.json` has no `sandbox` key; the deny list is `settings.json:52-55` | Before the real product (not the toy), write an ADR: sandbox on, `denyRead` for `.env*`, `~/.ssh` and `~/.aws`, `failIfUnavailable`. Needs the owner's yes. | M |
| B4 | MEDIUM | **The secret scan ignores `cd`/`git -C` targets** (already parked) | `git -C . push` evades rules, so read the full command — https://code.claude.com/docs/en/permissions | `tasks/todo.md:25` | Resolve the target directory from the command; add drill cases (owner's yes) | S |
| B5 | LOW | The drill harness reports DEFECTIVE on a dirty tree when tracked files link to untracked ones (measured tonight) | "Drill guard hooks for failure behavior" — https://code.claude.com/docs/en/hooks-guide | `drill_checks.py:20-31`, `:127`, `:137` | Copy untracked, non-ignored files too, or report INCONCLUSIVE when FILES.md is dirty | S |
| B6 | LOW | `untrusted.py` flags a summary that only quotes the fence format (false positive, risk of alarm fatigue) | Observed 5d; treat untrusted content as data — https://code.claude.com/docs/en/security | Handoff Update 23:10 (`injection_flags: [forged fence]`) | Ignore fence markers inside backticks or code spans; add a clean-control drill | S |
| B7 | LOW | No one-off instruction audit across user, project, skill and agent layers | "Run /doctor prompt-audit periodically … contradictions across CLAUDE.md, rules, skills and agents" — https://code.claude.com/docs/en/memory | No record of a run | Run `/doctor prompt-audit` once before the dry run; it changes nothing by itself | S |
| B8 | LOW | `/sdlc-wrap` commits, but the model can invoke it | "Use disable-model-invocation: true for workflows with side effects (… commit …)" — https://code.claude.com/docs/en/best-practices; the v2.1.292 fix for compaction summaries invoking user-only skills | `git grep disable-model-invocation` returns nothing; `sdlc-wrap/SKILL.md:14` commits | Add `disable-model-invocation: true` to `sdlc-wrap` | S |
| B9 | LOW | State files injected at session start are stale | "Start each session by reading the progress file" — https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents | `memory/now.md:8,29` (PR #5 "awaiting merge", but it is merged in 57af5db); `now.md:18` (gpt-6 stage 2, which ADR-0007 contradicts); `todo.md:19` (live `/compact` unticked, though v1–v5 prove it); `todo.md:35` (CLI "2.1.92", but this PATH runs 2.1.292) | Refresh these in the refresh already planned before the dry run | S |
| B10 | LOW | Conditional: `AGENT_MODELS` allows only Opus and Sonnet (5i) | Pick a model per role in frontmatter — https://code.claude.com/docs/en/sub-agents | `check_structure.py:25`, `:100` | Already listed in ADR-0007's Consequences. **This becomes an (a) item if the owner picks B or C, or adds `scout`, before the dry run.** | S |
| B11 | LOW | ADR-0007 numbers | Fable is priced 2.5× Opus (10/50 vs 4/20) | ADR-0007 Option B, Cost: "≈ $3 (Opus ≈ $1.5)" | Under the same assumption it is ≈ $3.75; or label the figure as a guess for the dry run to measure | S |

Further LOW item: the flaky `commit_secrets` drill case is already parked (`todo.md:26`).

ADR-0007 otherwise agrees with the repo and with the model note: the advisor pairs, the "today A without Haiku" status, the Haiku need for ≥2.1.293, and the Codex/CodeRabbit facts. I found no factual contradiction beyond B11.

## (c) ONLY THE DRY RUN CAN PROVE

| # | Question | Why it is open | Deciding observation |
|---|---|---|---|
| C1 | Does the main session actually call the advisor and name its verdict? | Anthropic measured 0 calls in 198 questions without a prompt; there is no way to force calls; the advisor's text is encrypted in the transcript (`claude-models-2026-10.md` §3) — https://code.claude.com/docs/en/advisor | Per gate, count of `advisor_message` items in `usage.iterations` and a named verdict in the reply. Zero at any gate → sharpen the skill text. |
| C2 | Do subagents run on their frontmatter models? | The global "Default sub-agent model: haiku" plus a per-invocation `model` parameter outranks frontmatter (`claude-models-2026-10.md` §4); on 2.1.292 `haiku` means Haiku 4.5 | The auditor, verifier and architect transcripts show `claude-opus-5-5` / `claude-sonnet-5-5` (or `claude-fable-5-1` under B). Any Haiku → add a CLAUDE.md line "never pass `model` when spawning engine agents". |
| C3 | Do machine-level agents or plugins take over engine roles? | 99 global agents (~28k tokens), 28 plugins, Codex active (5f, 5g); first-turn baseline 138–183k tokens | `agent_report.py` per-agent counts show only engine and built-in agents, and `/context` on the first turn is recorded. Any `codex:*`, `refuter` or `critic-*` → ADR-0007 decision 4 becomes necessary. |
| C4 | Are tests edited during fix rounds? | Playbook SHOULD: a hook that protects tests during fixes — https://claude.com/blog/the-ai-native-sdlc-playbook | `git diff --stat <fix commits> -- '*test*'` is empty across every review loop. Non-empty → build the hook. |
| C5 | Does Claude claim "done" without evidence? | Stop hook or `/goal` as a deterministic completion gate — https://code.claude.com/docs/en/best-practices | Every ticked task has a proof line, and the verifier reports no COULD-NOT-RUN behind a "done". Any unproven tick → add a Stop hook. |
| C6 | Session size across phases 1–6 | "One feature per session", `/clear` between tasks — https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents | Context at each gate (after the A4 fix), number of compactions, and no duplicated commits or approvals after a compaction. More than one compaction per phase → a per-phase session rule. |
| C7 | Can the verifier run UI proofs under the read-only guard? | `verifier.md:13` (start the app, take screenshots) vs `guard.py:204-225` (the verifier is read-only) | Verifier results for the UI milestone: MATCH, not COULD-NOT-RUN. |
| C8 | Is the "every file listed in FILES.md" rule friction for product code? | `check_structure.py:123-125` | Count of Build-phase `check_structure` failures caused only by FILES.md. More than 2 → allow one folder-level link per product directory in the build skill. |

## Considered and rejected for now (reason)
- **REVIEW.md, Code Review, Claude Security:** Team/Enterprise or preview products; the auditor loop covers the role.
- **Egress-allowlisted VMs, DAST, SIEM, release-manager hook, control bands:** there is no deploy target yet. These belong to Phases 8–9 for the real product.
- **OpenTelemetry:** Phase 9; `event_log.py` plus `agent_report.py` (after A4) are enough for the dry run.
- **Engine regression evals and plugin evals:** already parked, to be built from the dry run's real tasks (`todo.md:31`). Building them first would have no real tasks to use.
- **Agent teams, workflows, `ultracode`, worktree isolation:** the phases are sequential, with one editor.
- **Path-scoped rules, @AGENTS.md import, HTML comments:** CLAUDE.md has 92 lines (under 200). AGENTS.md serves non-Claude tools by design (ADR-0007).
- **autoMode `$defaults` and classifier settings:** the repo uses `acceptEdits`, not auto mode.
- **Memory tool and context editing:** API-only features.
- **Subagent `memory: project`:** no observed need.
- **Haiku `scout`:** needs both v2.1.293 and the owner's role choice (ADR-0007).
- **Stop hook and test-protection hook:** wait for C5 and C4. CLAUDE.md requires an observed failure before an engine addition.
- **Third-person skill descriptions:** a minor wording issue with no observed triggering failure.

## Already matches the guidance
- **Gated lifecycle:** committed artifacts (intent → PRD → ADR → TRD → spec → plan), only the owner approves, and the author never audits (playbook §1).
- **Evidence or inconclusive:** "inconclusive is never a pass" is a rule and is built into the checks' exit codes (2 = inconclusive).
- **Guards:** `guard.py` reads the full command, including `bash -c`, `eval` and `git -C`, and fails closed (`guard.py:267-269`, exit 2). 131 drill cases plus a stub mode that must report DEAD. This matches "a Bash deny is not a boundary; use a hook".
- **Read-only reviewers:** they have tool allowlists, and a hook enforces the read-only rule on top (observed live tonight).
- **Bounded review:** a separate auditor in a fresh context, required to give file:line evidence, with at most two fix rounds and a counted ledger. This matches "adversarial but bounded" review.
- **Models per role:** set in agent frontmatter, with full IDs and explicit effort; hook timeouts are set explicitly.
- **Secrets:** read-deny rules for `.env*` and keys, a commit-time secret scan with gitleaks, staging and committing in the same command blocked, and CI on every pull request.
- **Context and memory:** CLAUDE.md is under 200 lines and holds compaction instructions; SessionStart re-injects context on `compact`; a progress file plus git log are read at session start.

## Not checked
- Claude Code behaviour I could not run here: whether PostCompact input includes `agent_id`; whether a project-level `enabledPlugins: false` beats a user-level `true`; Fable-subagent behaviour without consent; what the VS Code picker writes.
- Any web source: I had no fetch access. Every URL above is quoted from the research file, not re-read by me. That includes ADR-0007's quote "start with Claude Opus 5.5 for most workloads".
- Whether mods are installed on this machine (mods can override hooks).
- The CI status of this branch.
- Whether the drills pass after a commit. I inferred the cause of DEFECTIVE but did not re-run it on a clean tree.
- A line-by-line comparison of PROCESS.md and CLAUDE.md beyond the gate and dry-run rules.

READY FOR DRY RUN: no — 6 blocking items

## Post-audit notes (main session, 2026-10-08)
- Drills re-run after the new files were staged, so `git ls-files --cached` includes them: `python3 sdlc/checks/drill_hooks.py` → 131 cases, RESULT: ARMED (exit 0); with `ASTERBIT_DRILL_STUB=1` → RESULT: DEAD (exit 1). This confirms the auditor's inferred cause of the DEFECTIVE run (B5: the drill copies only files in the git index).
- `python3 sdlc/checks/check_structure.py` → 364 checks, PASS (89 files).
- B11 applied: ADR-0007 option B now gives a range (about 2–2.5 times an Opus review) and labels it a guess for the dry run to measure.
- The main session re-read the decisive facts on the official pages itself on 2026-10-08: the quote "start with Claude Opus 5.5 for most workloads", the four model IDs and prices (https://platform.claude.com/docs/en/models/overview), the advisor pairing table, the silent drop of a refused pairing and the uncached advisor read (https://code.claude.com/docs/en/advisor), and the model precedence `/model` > `--model` > `ANTHROPIC_MODEL` > settings (https://code.claude.com/docs/en/model-config).

## Status after the owner's decisions (main session, later on 2026-10-08)
The assessment above is kept as written. Since then:
- **The owner cancelled the engine dry run.** The dry-run rule is gone from CLAUDE.md, PROCESS.md and `sdlc-harness`. C1–C8 stay open and are answered during the first real phases instead.
- A1–A6 are done (PR #7); B4 is done (PR #8). B1 (this repo's rules win over user-level rules), B2 (`rm` / `git rm` ask first), B5 (the drill copy includes untracked, non-ignored files), B9 (state files refreshed) and B10 (`AGENT_MODELS` already has Haiku) are done on branch `engine/owner-decisions`. B11 was already applied (above).
- B3 → ADR-0009 (proposed; pull request #10 numbered it 0008, which on this branch is Cursor support): the option is the owner's to pick, because the sandbox also limits writes and network. B6 and B8: the owner wants to agree the exact change first. B7 (`/doctor prompt-audit`) has not been run. The dry-run sentence above names CLAUDE.md; on this branch that rule lived in AGENTS.md and is removed.
