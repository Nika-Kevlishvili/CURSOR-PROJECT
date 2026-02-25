# Push EnergoTS Command

## Description

**Push EnergoTS** updates **GitHub's EnergoTS cursor branch** with your **local version**: it commits any uncommitted changes on the local `cursor` branch and pushes to `origin/cursor`. Use this when you want to publish your local EnergoTS work to the remote cursor branch.

- **Pull EnergoTS** = download and sync locally with remote (never pushes).
- **Push EnergoTS** = update remote (GitHub) with your local cursor branch.

## Usage

```powershell
# Commit all local changes and push to origin/cursor (default message)
.cursor/commands/push-energots.ps1

# Custom commit message
.cursor/commands/push-energots.ps1 -Message "Add billing test for 5 pods"
```

## What It Does

1. **Ensures EnergoTS path** — Uses `Cursor-Project\EnergoTS` (must be a git repo).
2. **Ensures cursor branch** — Switches to `cursor` if needed (EnergoTS branch lock).
3. **Commits local changes** — If there are uncommitted changes: `git add -A`, `git commit -m <message>`.
4. **Pushes to remote** — `git push origin cursor` to update GitHub's EnergoTS cursor branch.

## Parameters

- **`-Message`** (optional) — Commit message when committing local changes. Default: `"Update EnergoTS cursor branch"`.

## When to Use

- You have local changes in EnergoTS and want to update the cursor branch on GitHub.
- You have already committed locally and only need to push to origin/cursor.

## Safety

- Only operates in `Cursor-Project\EnergoTS` and only on the `cursor` branch.
- Pushes to `origin` — ensure you have write access to the EnergoTS remote (GitHub).

## Example Output

```
=== Push EnergoTS (update GitHub cursor branch with local) ===

Current branch: cursor
Uncommitted changes:
  M tests/billing/billingDataByProfile5Pods.spec.ts

Staging all changes...
Committing...
Committed successfully.

Pushing to origin/cursor...
Pushed successfully. GitHub EnergoTS cursor branch updated.

=== Done ===
```

## Related Commands

- **Pull EnergoTS** (`pull-energots.ps1`) — Only downloads and syncs locally with remote; never pushes.
