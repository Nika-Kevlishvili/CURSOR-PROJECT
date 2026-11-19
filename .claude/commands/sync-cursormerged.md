---
description: Bring new staging and cursor commits onto the EnergoTS cursorMerged branch by cherry-pick
---

# /sync-cursormerged

Keep the EnergoTS `cursorMerged` branch up to date with `staging` and `cursor`.

**Usage:** `/sync-cursormerged` or `/sync-cursormerged plan`

- `plan` (or "what is pending"): show what would be cherry-picked. Nothing is changed.
- No argument: plan, cherry-pick, adapt, verify, then report. Never pushes (Rule ENERGOTS.1).

## What this command does

Follow `.claude/skills/energo-ts-sync/SKILL.md` (agent: `.claude/agents/energo-ts-sync.md`). In short:

1. Run the plan and show it:

   ```bash
   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1
   ```

2. Stop here when the user asked only for the plan.
3. Resolve anything in the way (wrong branch, local changes) by asking the user.
4. Cherry-pick:

   ```bash
   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Apply
   ```

5. Resolve conflicts by reading both sides.
6. Fit everything to the `cursorMerged` architecture (SKILL step 5). The script does the mechanical part at the end of `-Apply`; these two re-run it and prove it:

   ```bash
   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Adapt
   ```

   ```bash
   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Check
   ```

   Fix every type error in the synced files, read each picked file against the layout rules, confirm `npx playwright test --list` loads the new specs, then commit the adaptation.
7. Report, including how many commits `cursorMerged` is ahead of `origin/cursorMerged`. Never push it (Rule ENERGOTS.1); the user pushes it themselves.

## What the script does

| Mode | Effect |
|------|--------|
| Default | `git fetch origin cursor staging cursorMerged`, then prints, per source, the last commit already on `cursorMerged` and the commits after it |
| `-Apply` | Fast-forwards `cursorMerged` to its remote if behind, then `git cherry-pick -x` for each pending commit, staging first, then cursor. Stops at the first conflict. When all picks are done, runs the adaptation |
| `-Adapt` | For commits not yet pushed: moves files out of pre-merge folders, removes root run logs, rewrites imports that point at the pre-merge layout, lists what needs a reader. Leaves changes uncommitted |
| `-Check` | Runs `npx tsc --noEmit` and lists the errors in files the not-yet-pushed commits touched |

It never switches branches, never stashes or resets, and never pushes.

Other options: `-Source staging|cursor`, `-Exclude <sha>`, `-StagingBase <sha>`, `-CursorBase <sha>`, `-AllowDirty`, `-NoFetch`. Exit codes are listed in the SKILL.

## Notes

- Works in `Cursor-Project/EnergoTS/` and requires that checkout to be on `cursorMerged` (Rule ENERGOTS.0).
- `sync-cursor-with-staging` is a different, older command for the `cursor` branch. It does not update `cursorMerged`.
