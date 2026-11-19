---
description: "EnergoTS (Cursor-Project/EnergoTS) git branch lock (Rule ENERGOTS.0): use the `cursor`, `staging`, or `cursorMerged` branch. Do not checkout main or any other branch. Do not auto-sync from main unless the user explicitly asks. Never push cursorMerged to origin (Rule ENERGOTS.1). File writes under EnergoTS are allowed when the user requests them (Rule 0.8). Playwright authoring uses the post-merge src/tests layout."
paths:
  - "Cursor-Project/EnergoTS/**"
  - ".claude/agents/energo-ts-run.md"
  - ".claude/skills/energo-ts-run/SKILL.md"
  - ".claude/skills/energo-ts-test/SKILL.md"
  - ".claude/agents/energo-ts-sync.md"
  - ".claude/skills/energo-ts-sync/SKILL.md"
  - ".claude/commands/sync-cursormerged.md"
  - ".claude/procedures/playwright-frontend/**"
  - ".claude/procedures/playwright-frontend-validator/**"
  - ".claude/commands/playwright-frontend.md"
---

# ENERGOTS BRANCH LOCK RULE [CRITICAL]

## Rule ENERGOTS.0: EnergoTS Branch Restriction

**Claude Code may use only `cursor`, `staging`, or `cursorMerged`** under `Cursor-Project/EnergoTS/`.

**Claude Code MUST NEVER automatically** fetch/merge/pull/rebase from **`main`** — only when the user **explicitly** asks.

**Do not auto-switch branches.** Stay on the current checkout when it is already one of the three allowed branches. Do not leave `cursorMerged` for `cursor`.

### Layout by branch

| Branch | Playwright layout |
|--------|-------------------|
| `cursorMerged` | Post-merge layout. `playwright.config.ts` `testDir` is `./src/tests`. Support code is under `src/backend/`. This is the branch created from `cursor` with `origin/staging` merged (`19fbe36`). |
| `staging` | Same post-merge layout as `cursorMerged`. |
| `cursor` | Pre-merge layout until that branch receives the merge: `testDir` is `./tests` and fixtures live at the repo root. Do not author new specs against that layout. |

Playwright authoring and runs for the current EnergoTS tree MUST see `testDir: './src/tests'` in `playwright.config.ts`. If the checkout is `cursor` and `testDir` is still `./tests`, stop and ask the user before switching to `cursorMerged` or `staging`.

### Allowed vs prohibited

- **Allowed on EnergoTS:** All git ops while already on `cursor`, `staging`, or `cursorMerged`, except pushing `cursorMerged` (Rule ENERGOTS.1); switching **to** one of those three branches when the user asks; merging `main` into the current allowed branch only after an explicit user request.
- **Prohibited:** `git checkout` / `switch` to any branch other than `cursor`, `staging`, or `cursorMerged`; `checkout -b` new branches; any proactive sync with `main` without an explicit request; switching from `cursorMerged` to `cursor` during Playwright work.

## Rule ENERGOTS.1: Never push cursorMerged [BLOCK]

**Claude Code MUST NEVER push `cursorMerged` to `origin`** (or any other remote). This is a project-owner decision (2026-11-19). It holds even when the user says yes to a push in chat, and for every workflow, including **energo-ts-sync**, **reg-fixer**, **energo-ts-test**, and **playwright-frontend**.

- Forbidden: `git push` while on `cursorMerged`, `git push origin cursorMerged`, any refspec that updates `refs/heads/cursorMerged` on a remote (for example `HEAD:cursorMerged`), `--force` / `--force-with-lease` variants, `--all` / `--mirror`, and pushing through another tool or script.
- Commit on `cursorMerged` locally when the user asks. Then stop and tell the user the branch is ahead of `origin/cursorMerged` by N commits; the user pushes it themselves.
- Do not offer to push `cursorMerged`, and do not put a push button or question in the wrap-up.
- Pushing `cursor` or `staging` is not covered by this rule; it still needs the user's explicit request.

### Before git branch ops under EnergoTS

1. Confirm path is under `Cursor-Project/EnergoTS/`.
2. If the operation changes branch, the target **must** be `cursor`, `staging`, or `cursorMerged`. Otherwise **BLOCK**.
3. For Playwright writes or runs, confirm `playwright.config.ts` contains `testDir: './src/tests'` before creating specs.

### Enforcement hooks

Hooks **`block-energots-branch-switch.ps1`** (`PreToolUse (Bash)`) and **`block-energots-branch-requests.ps1`** (`UserPromptSubmit`) allow `cursor`, `staging`, and `cursorMerged` and block every other branch. **`protect-energots-writes.ps1`** allows file writes under `Cursor-Project/EnergoTS/`.

### Integration

- **Rule 0.8:** On `cursor`, `staging`, or `cursorMerged`, user-requested edits under `Cursor-Project/EnergoTS/**` are allowed, including page objects, fixtures, helpers, and frontend tests.
- **Rule 45:** **playwright-frontend** and **playwright-frontend-validator** may run only when EnergoTS is already on `cursorMerged`. They must not checkout or switch, including to `cursor` or `staging`.
- **energo-ts-sync:** `cursorMerged` receives new `staging` and `cursor` commits by cherry-pick through `.claude/commands/sync-cursormerged.ps1` (`.claude/skills/energo-ts-sync/SKILL.md`). That workflow stays on `cursorMerged`, reads `origin/staging` and `origin/cursor` without checking them out, never merges or rebases them in, and never pushes (Rule ENERGOTS.1); the user pushes `cursorMerged` themselves.
- **reg-fixer:** writes to EnergoTS on `staging` only. When the checkout is on another allowed branch, it switches to `staging` by itself if `git status --porcelain` is empty, and stops and asks when there are uncommitted changes. It never stashes, commits or discards to get there. This is a project-owner decision (2026-11-18) and applies to that workflow only (`.claude/skills/reg-fixer/SKILL.md` Step 8).
- Phoenix/other repos: **not** covered by this rule. Phoenix stays read-only.

**Violation of ENERGOTS.0 or ENERGOTS.1 is a BLOCK.**
