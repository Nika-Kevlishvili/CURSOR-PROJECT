---
id: "0005"
title: Harness v0 — balanced autonomy, repo-local hooks in Python, 30-day transcripts, CI on every pull request
status: proposed
scope: engine
date: 2026-10-07
deciders: owner (chose every option below on 2026-10-07), Claude (drafted)
supersedes: —
superseded_by: —
review_by: Phase 0 gate (engine v1 approval)
tags: [engine, harness, security, memory, ci]
---
# 0005 — Harness v0

## Context (კონტექსტი)
Until now the engine had no project settings: the models of ADR-0003 and the 65% compaction of ADR-0004 were not applied, nothing saved handoffs automatically, and every safety rule lived only in the owner's global `~/.claude` setup — which does not exist on a collaborator's machine (lashavamleti has write access since 2026-10-07). ADR-0004 also left two items open: transcript retention and the owner's handoff principles. The owner asked to finish the engine before telling the product idea.

## Options considered (განხილული ვარიანტები)
### Autonomy (owner's choice, 2026-10-07)
- **A — Careful:** ask before every edit and every command. Safest, many confirmations. Reversibility: one settings change.
- **B — Balanced (chosen):** edit files in the repo, run checks, commit and push working branches without asking; ask before installs, internet fetches, settings/hooks/CI changes, merging into main and commands that throw away uncommitted work; block destructive commands, force pushes, pushes to main and secret files. Reversibility: one settings change.
- **C — High:** also install tools without asking; main still needs the owner. Reversibility: one settings change.
### Where the protection lives
- **A — Owner's global hooks only:** already exist, but other machines get nothing. Reversibility: easy.
- **B — Repo-local hooks (chosen):** `.claude/settings.json` + `.claude/hooks/*.py`, so every clone gets them. Python 3 standard library only, no extra packages. Cost: needs `python3` on the machine; on Windows they run under Git Bash or WSL. Reversibility: easy — remove the hook entries from settings.json.
### Transcript retention (owner's choice)
- 1 year / 90 days / **30 days — Claude Code's default (chosen)**. No `cleanupPeriodDays` setting; our own summaries and handoffs stay in git regardless. Reversibility: easy, but transcripts already deleted do not come back.
### Handoff and summary principles
- **The eight principles Claude proposed (chosen)** / the owner writes their own. Recorded in `memory/README.md`. Reversibility: edit the text between the markers.
### CI
- **GitHub Actions on every pull request (chosen)** / local checks only. Private repos on GitHub Free get 2,000 Actions minutes a month; one run takes about a minute. Reversibility: delete one workflow file.

## Decision (გადაწყვეტილება)
1. `.claude/settings.json`: `model: claude-sonnet-5-5`, `modelSettings.claude-sonnet-5-5.effortLevel: high`, `advisorModel: claude-opus-5-5` (ADR-0003); `autoCompactWindow: 650000` (ADR-0004); `permissions.defaultMode: acceptEdits` with allow / ask / deny lists for balanced autonomy; the official plugin marketplace listed so Superpowers can be installed, but not enabled by the repo (optional — ADR-0002).
2. Hooks in `.claude/hooks/`:
   - `guard.py` (PreToolUse) — blocks `rm -rf`, `git reset --hard`, `git clean -f`, force pushes, any push that lands on or deletes main (also `git push origin HEAD` / `@` while on main), `--no-verify`, history rewriting, credential files sent over the network or encoded, writing secret files; asks first before commands that throw away uncommitted work or delete a branch (`switch -f`, `checkout -- path`, `restore`, `branch -D`, `stash drop`) and — owner, 2026-10-08 — before every file deletion (`rm` run as a command, `git rm`); looks inside `bash -c` / `sh -c` / `eval`; for the `auditor`, `verifier` and `architect` agents it also blocks every file-changing tool and command, except reading the stash (`git stash list` / `show`, owner, 2026-10-08). It fails closed. Owner, 2026-10-08: the push check keeps reading the session folder's branch — a push from another folder that sits on main is not blocked, and the owner chose not to block it.
   - `commit_secrets.py` (PreToolUse) — before `git commit`, scans the staged changes with built-in patterns and with gitleaks when installed, names the scanners that ran, and blocks on a finding, when the scan cannot run, or when the commit would include changes that are not staged (`git add … && git commit`, `-a`, `-i`, `-o`, `-p`, file names). It scans the repository the commit really lands in (`git -C dir`, a preceding `cd dir &&`, `bash -c '…'` strings are followed — 2026-10-08) and blocks when that folder cannot be known (`$VAR`, `--git-dir`, `GIT_DIR=`).
   - `memory_handoff.py` (PostCompact) — saves the compaction summary as the session's rolling handoff, archives the previous version, redacts secret-looking values.
   - `memory_context.py` (SessionStart) — injects `memory/now.md`, PROGRESS.md Current State / Next Steps and a handoff, labelled as data, capped at 12,000 characters.
3. Checks in `sdlc/checks/`: `check_structure.py` (required files, skills, agents, links, settings, `docs/FILES.md` covers every file) and `drill_hooks.py` (clean controls must pass, seeded defects must be caught; stub mode must report DEAD).
4. `.github/workflows/checks.yml` runs both checks, the stub drill and gitleaks on every pull request and on pushes to main.
5. This closes ADR-0004's open items: retention stays at the 30-day default; the principles are in `memory/README.md`.

## Rationale (რატომ)
A rule that exists only on the owner's machine protects nothing on a collaborator's machine; repo-local hooks travel with every clone. Balanced autonomy removes routine confirmations while keeping every irreversible or shared change behind a human. Each blocking control is drilled, because a guard that cannot fail is not evidence.

## Consequences (შედეგები)
- Positive: the same guard rails on every machine; handoffs are saved without anyone remembering to; every pull request gets a visible green or red check.
- Negative: hooks need `python3`; if it is missing the hook cannot start, Claude Code reports a hook error and the guard is off on that machine. `main` still has no server-side protection (GitHub Pro needed), so CI shows problems but cannot stop a merge.
- Accepted gaps: the read-only rule for agents works from a list of commands — a program that writes a file from inside (`python3 -c "open(…,'w')"`, a test cache) is not seen. At session start the newest handoff may come from another person's session; it is labelled as data, and handoffs reach `main` only through a reviewed pull request.
- What becomes harder: editing settings, hooks or CI always asks for confirmation (by design).

## Verification (როგორ შევამოწმებთ)
- `python3 sdlc/checks/drill_hooks.py` → `RESULT: ARMED` (89 cases on 2026-10-07: 50 seeded defects caught, 25 clean controls passed, 7 ask cases asked, 7 file/context checks worked, including the gitleaks-only case and a 3-defect copy for the structure check); `ASTERBIT_DRILL_STUB=1 …` → `RESULT: DEAD` (exit 1).
- The first audit (2026-10-07) failed the harness on a real hole — `git push origin HEAD` on main was not blocked — plus eight medium findings; all were fixed and each now has a drill case. The second audit found one more way through — a redirect (`git push origin 2>&1`) was read as a branch name — fixed in `without_redirects()` and drilled (3 push cases on main, 2 clean controls); no third audit round was run (two-round limit), so this last fix rests on the drill.
- Live wiring, 2026-10-07, re-run on the final guard: writing a `.env.probe2` file was blocked by the project hook (ASTERBIT-GUARD); the `verifier` agent's `touch` was blocked with the read-only message and the file was not created; Claude's own commit (scanned: built-in patterns + gitleaks) and push of `engine/v1` went through.
- Not yet verified live: a real `/compact` writing `memory/episodic/handoffs/` (the hook logic is drilled with synthetic input only); a live "ask" prompt from the guard (the JSON format is drilled and follows the hooks documentation); the advisor tool working on every collaborator's account.
- CI green on PR #2 (2026-10-07): structure 269 checks PASS, drill ARMED with gitleaks installed (no skipped case), stub DEAD, gitleaks history clean — https://github.com/tornikebolokadze1-cyber/Asterbit/actions/runs/37601321945

## Links
- [ADR-0003](0003-model-orchestration-phase-1.md) · [ADR-0004](0004-context-and-memory.md) · [ADR-0002](0002-engine-base-native-plus-superpowers.md)
- [sdlc/design/ai-security.md](../../sdlc/design/ai-security.md) · [sdlc/design/memory-and-context.md](../../sdlc/design/memory-and-context.md)
