---
name: energo-ts-sync
description: Keeps the EnergoTS cursorMerged branch up to date with staging and cursor by cherry-pick. Finds the last staging and cursor commits already on cursorMerged, cherry-picks the commits after them, then fits everything that came from cursor to the cursorMerged architecture (folders, imports, shared helpers, types), verifies, and asks before pushing. Use when the user asks to sync or update cursorMerged, move staging or cursor commits to cursorMerged, or asks what is missing there.
---

# EnergoTS cursorMerged sync

Use when the user wants `cursorMerged` to receive the new commits from `staging` and `cursor`, or wants to know what is pending.

All git commands run in `Cursor-Project/EnergoTS/`. The script runs from the workspace root.

## Why this needs its own procedure

`cursorMerged` was made from `cursor` plus a **squashed** copy of `staging` (`19fbe36`, one parent). Since then commits arrive by cherry-pick. Three things follow, and each one breaks an obvious git command:

| Fact | Consequence |
|------|-------------|
| The staging merge has one parent | `git merge-base cursorMerged origin/staging` is a commit from May. Using it lists about 280 staging commits as missing |
| `cursor` still has the pre-merge layout (`tests/`, `fixtures/`, `jsons/`, `utils/`, `validations/` in the root) | A cursor commit changes path when picked, so `git cherry` never recognises it as already applied |
| Picks made before this skill existed carry no `(cherry picked from commit ...)` line | The only trace is that a cherry-pick keeps the author, the author time and the subject |

So detection is the script's job. Do not replace it with `git cherry`, `git merge-base` or `git log A..B`.

## How the script detects the last cherry-pick

For each source (`origin/staging`, `origin/cursor`) it looks for the newest source commit that already exists on `cursorMerged`, using these signals:

1. `(cherry picked from commit <sha>)` in a `cursorMerged` commit message. Every pick made by this skill has it (`-x`).
2. Same author email, author timestamp and subject. This covers the older picks.
3. Identical patch (`git cherry`).
4. A `Synced-staging:` or `Synced-cursor:` trailer written by an earlier sync (see step 6).

That commit is the **watermark**. Pending commits are the non-merge commits after it, oldest first. Merge commits are not picked; the commits they brought in are picked one by one.

If a source has commits missing from `cursorMerged` and no watermark is found, the script exits with code 3. **Stop and ask the user** which commit was the last one brought over, then pass it with `-StagingBase <sha>` or `-CursorBase <sha>`.

## Workflow

### 1. Plan

```bash
pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1
```

This fetches `cursor`, `staging` and `cursorMerged` from `origin` and prints the plan. It changes nothing else. Show the user the watermark and the pending list for each source.

If the user only asked what is pending, stop here.

### 2. Clear the way

| Script says | Action |
|-------------|--------|
| Not on `cursorMerged` (exit 2) | Ask before switching. Only `cursorMerged` is a valid target. Never check out `cursor` or `staging` |
| An unfinished git operation is in progress (exit 2) | Show `git status`. Ask whether to finish or abort it |
| Local and `origin/cursorMerged` have diverged | Ask. Do not rebase or reset on your own |
| Local changes overlap pending commits | Name the files. Ask the user to commit them or put them aside. Do not stash or reset for them |
| Tracked files modified, no overlap | Ask. With their agreement run `-Apply -AllowDirty` |
| The user wants a commit left out | Add `-Exclude <sha>` |

### 3. Apply

A request to sync, update or cherry-pick is the go-ahead to pick locally. A request that only asked what is pending is not.

```bash
pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Apply
```

The script fast-forwards `cursorMerged` to its remote if it is behind, then picks one commit at a time with `git cherry-pick -x`, staging first, then cursor. Files a cursor commit adds under `tests/` land under `src/tests/` during the pick. It skips a commit that leaves nothing to apply. It stops at the first real conflict and leaves that cherry-pick in progress (exit 10). When every pick is done it runs the automatic adaptation of step 5a.

### 4. Conflicts

Read both sides before resolving. Use `git diff`, `git show <sha> -- <path>` and `git log -3 -- <path>`.

- Keep the changes of **both** branches when they touch different things, which is the usual case for registries such as `src/frontend/pom/PageObjects.ts`.
- When both sides rewrote the same logic and it is not clear which one should win, stop and ask the user. Name the file, the two commits, and what each one does.
- Files that `cursorMerged` removed on purpose stay removed: deleted `.github/` files, and run logs or run output (`*.log`, `*.txt`) in the repository root. The script already handles the plain delete-versus-modify case.
- To take one side of a single file, use `git restore --ours -- <path>` or `git restore --theirs -- <path>`. Do not use `git checkout --ours`; the EnergoTS branch hook reads `--ours` as a branch name and blocks it.
- Never take one whole side for every file.

Then:

```bash
git add <resolved files>
```

```bash
git -c core.editor=true cherry-pick --continue
```

Run the script with `-Apply` again. It continues with what is left.

To give up on one commit: `git cherry-pick --skip`, then pass `-Exclude <sha>` on the next run and tell the user. Once the closing commit records the `Synced-` marker, an excluded commit is not offered again, so exclude only with the user's agreement and name the commit in the report. To give up on the whole run, ask first; commits already picked stay unless the user wants them removed.

### 5. Fit everything to the cursorMerged architecture [MANDATORY]

A sync is not finished when the picks apply. `cursor` and `cursorMerged` are different codebases now, so every picked commit, and above all every cursor commit, must be fitted to the `cursorMerged` tree before the work is reported or pushed. Staging shares the layout, so it usually needs nothing, but the same checks run for it.

**How the two branches differ**

| Area | `cursor` | `cursorMerged` |
|------|----------|----------------|
| Specs | `tests/<domain>/`, `tests/cursor/` | `src/tests/<domain>/`, `src/tests/cursor/` |
| Fixtures, utils, validations | `fixtures/`, `utils/`, `validations/` in the root | `src/backend/fixtures/`, `src/backend/utils/`, `src/backend/validations/` |
| Payloads | `jsons/payloads/create/<domain>/` | `src/backend/jsons/payloads/<domain>/` (no `create/`) |
| Payload generators | `jsons/payloadGenerators/` | `src/backend/jsons/payloadGenerators/` |
| Mass-import library | `mass-imports/generators/`, `mass-imports/massImportPayloads/` | `src/backend/mass-imports/...` |
| Frontend | none | `src/frontend/` (specs, `pom/`, fixtures) |
| Shared cursor helpers | `tests/cursor/cursor-test.fixtures.ts`, `tests/cursor/shared/*` | Same names under `src/tests/cursor/`, but the content has drifted. Types and function signatures differ |
| `playwright.config.ts` | `testDir: './tests'`, report in the root | `testDir: './src/tests'`, reports under `src/backend/`, extra projects (`frontend`, `sales portal`, `additional billing tests`) |
| `tsconfig.json` | Loose | `module` and `moduleResolution` `node16`, `target` `ES2022`. Code that compiled on `cursor` can fail here |
| Root run logs, most of `.github/` | Present | Removed on purpose |

Import prefix rule, placement of new specs and fixture conventions are in `.claude/skills/energo-ts-test/SKILL.md` § Repository layout. Read that section before step 5c.

#### 5a. Automatic part

A finished `-Apply` runs this by itself. Run it again on its own after you resolve the last conflict by hand:

```bash
pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Adapt
```

It works only on files that the commits on `cursorMerged` but not yet on `origin/cursorMerged` added or changed. The user's own uncommitted files are never read or rewritten. It leaves its changes uncommitted:

- moves files that arrived in a pre-merge folder to their `cursorMerged` folder,
- removes run logs and run output added in the repository root,
- rewrites every relative import that no longer resolves to the file's real place under `src/`,
- lists what it could not decide: imports with no single match, a moved file whose target already exists, new `.github/` files, specs that use `beforeAll`, specs whose key already has a spec on `cursorMerged`, and code that builds a file path with a pre-merge folder name.

Read `git diff` after it. Every item under "by hand" or "Review" is yours to settle in 5c.

#### 5b. Type check

```bash
pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Check
```

`-Apply` records which type errors exist before the first pick. `-Check` runs `npx tsc --noEmit` again and prints only the errors that were not there before, in any file, so an error a pick caused in an untouched file is listed too. Fix every one of them. Older errors are counted, not listed; report the number and leave them.

If no baseline exists (the picks were not made by this script), `-Check` falls back to listing errors in files the unpushed commits touched. In that mode an error may be older than the sync: compare with `git show origin/cursorMerged:<file>` before changing anything, and report an older error instead of fixing it.

How to fix:

- The picked file adapts to `cursorMerged`. Change the picked spec or fixture so it uses the types, signatures and helpers that exist on `cursorMerged`.
- Do not overwrite a shared file on `cursorMerged` (`cursor-test.fixtures.ts`, `shared/*`, anything under `src/backend/`) with the `cursor` version to make an error go away. Other specs on `cursorMerged` depend on the current one.
- When a picked file needs something that exists only on `cursor` (a new helper, a new field on a shared type), add that piece to the `cursorMerged` file and keep everything that file already exports.
- A cast or `any` that hides the mismatch is not a fix.

#### 5c. Read every picked file

For each file a cursor commit added or changed, open it and check:

| Check | Fit |
|-------|-----|
| Folder | API specs and their `.fixtures.ts` under `src/tests/cursor/` or the matching `src/tests/<domain>/`. Support code under `src/backend/`. Frontend specs and page objects under `src/frontend/`. Nothing new in a pre-merge root folder |
| Imports | Follow the import prefix rule. A file under `src/tests/` reaches support code through `backend/` |
| Shared helpers | Uses the `cursorMerged` helpers (`./cursor-test.fixtures`, `./shared/...`, `Responses`, payload generators) as they are on this branch. No private copy of a helper that already exists here |
| Paths in code and comments | `path.join`, `readFileSync`, report or output locations, and run commands in comments name the `src/` layout (`npx playwright test src/tests/cursor/...`) |
| Same key already present | When the script lists a spec whose key already has a spec here, compare them. Different scenarios: keep both. Same scenario written twice: ask the user which one stays |
| Conventions | No `beforeAll` for data setup (Rule 40). List what you find; rewriting setup is test authoring, see below |

Then confirm Playwright can load the new specs. This lists the tests without running them:

```bash
npx playwright test --list <added spec files>
```

Loading needs the generated files `src/backend/fixtures/envVariables.json` and `token.json` (git-ignored, written by `--project=setup`). If they are missing, say so and ask; do not run setup against an environment on your own.

**Swagger refresh (Rule 41).** That rule covers every `.spec.ts` edit, so run `pwsh -NoProfile -File .claude/commands/update-swagger-specs.ps1` once before the first hand edit in 5b or 5c. If it fails, warn the user and continue with the cached specs, as the rule says. A fit does not touch payloads, so the endpoint cross-check of that rule applies only when a type fit changes a payload field name, type or enum value; check that field in the refreshed spec.

#### 5d. Boundary

Fitting changes where code lives and what it imports, calls and is typed against. It does not change what a test does: steps, payload values, expected results. When a picked spec cannot be made to fit without changing that, stop on that file, tell the user, and hand it to **energo-ts-test** (Swagger refresh and validator apply there, Rules 40 and 41).

#### Done when

1. `-Adapt` prints `0 imports still unresolved` and has nothing under "by hand".
2. `-Check` prints `0 errors introduced by this sync` (or `0 errors in files this sync touched` when there is no baseline).
3. `npx playwright test --list` loads every added spec.
4. Every "Review" item is settled or reported to the user.
5. `git status` shows no file in a pre-merge folder and no run log in the root.

Do not commit the adaptation, report success, or offer to push before all five hold. If one cannot be met, say which and why.

### 6. Closing commit

Commit the adaptation as one commit. Stage only the files the adaptation touched; leave the user's own uncommitted work out. Add the trailers the script printed:

```
Adapt cherry-picked commits to cursorMerged layout

- <what was moved, which imports, which type fits, per group of files>

Synced-staging: <sha printed by the script>
Synced-cursor: <sha printed by the script>
```

When there is nothing to adapt, make this commit only if the script printed `Marker needed: YES` (the newest commit of a source was skipped or excluded, so nothing records it). In that case use `git commit --allow-empty` with a subject such as `Sync marker: cursorMerged is up to date with staging and cursor` and the same trailers.

### 7. Report, then ask before pushing

Run the plan once more. It must report both sources as up to date.

Report in chat: commits picked per source, commits skipped and why, each conflict and how it was resolved, what the adaptation changed (files moved, imports rewritten, type fits and where), the `-Check` and `--list` results, and anything left for the user or for **energo-ts-test**.

Then ask the user whether to push. On yes:

```bash
git push origin cursorMerged
```

Never force-push. If the push is rejected because the remote moved, run the workflow again from step 1.

## Script reference

| Option | Meaning |
|--------|---------|
| (none) | Plan only. Fetches, prints, changes nothing else |
| `-Apply` | Cherry-pick the pending commits |
| `-Source staging` or `-Source cursor` | Process one source |
| `-Exclude <sha>,<sha>` | Leave these commits out |
| `-StagingBase <sha>`, `-CursorBase <sha>` | Set the watermark by hand |
| `-AllowDirty` | Apply while unrelated tracked files are modified |
| `-Adapt` | Only fit the not-yet-pushed commits to the `cursorMerged` layout. No fetch, no pick |
| `-Check` | Only type-check and list errors in files the not-yet-pushed commits touched |
| `-NoFetch` | Use the remote-tracking refs as they are |

| Exit code | Meaning |
|-----------|---------|
| 0 | Plan printed, already up to date, or applied |
| 2 | Wrong branch, unfinished git operation, or diverged from the remote |
| 3 | No watermark found for a source |
| 4 | Local changes block the apply |
| 5 | A git command failed |
| 6 | `-Adapt` or `-Check` left something to fix |
| 10 | Stopped on a conflict, cherry-pick in progress |

## Constraints

- EnergoTS only. Never edit `Cursor-Project/Phoenix/**` (Rule 0.8).
- Stay on `cursorMerged`. No checkout of `cursor` or `staging`, no new branches (Rule ENERGOTS.0).
- No merge or rebase of `staging` or `cursor` into `cursorMerged`. Cherry-pick only.
- No stash, no reset, no discard of the user's work.
- No push without the user's yes in this conversation. No force-push.
- Never sync from `main` here.
- Commit messages in English (Rule 0.7).

## Agent

`.claude/agents/energo-ts-sync.md` (I/O contract only).
