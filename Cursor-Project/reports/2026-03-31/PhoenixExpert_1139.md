# PhoenixExpert — Local branch state (Cursor-Project/Phoenix)

**Date:** 2026-03-31

## Finding

- The file-explorer screenshot does not show a Git branch; branch is usually shown in the editor status bar (bottom-left).
- Each component under `Cursor-Project/Phoenix/` is its own repository.
- **None** of these repos are currently checked out on a **named local branch**; all report `HEAD (no branch)` (detached HEAD at a specific commit).

## phoenix-core (example)

- **HEAD:** `5fd685d140` — `fix(NT-10): bump devVersion to align with core-lib fix`
- That commit is contained by remote branches including `origin/experiment` and `origin/fix/NT-11` (not necessarily exclusive to those).

## Snapshot of other Phoenix subrepos (commit + last message)

| Folder | Short SHA | Last commit summary |
|--------|-----------|---------------------|
| mfe-poc-with-nx | 04c9475 | Initial commit |
| phoenix-api-gateway | a2e47f9 | Initial commit |
| phoenix-billing-run | f17018a | "experiment" |
| phoenix-core-lib | 87b79d6a4 | fix(NT-10): strip full JWT tokens from error/warn log messages |
| phoenix-mass-import | a3fd809 | Initial commit |
| phoenix-migration | 1e1c230 | Remove composite key classes... |
| phoenix-payment-api | dbebee0 | 1.33.45 |
| phoenix-scheduler | 56ef53a | Initial commit |
| phoenix-ui | 864dff6f9 | Merge branch 'feature/NT-7' into 'experiment' |

## Conclusion for the user

**There is no single branch name** for “what you see in the tree”: each submodule is detached at its own commit. Content is consistent with **experiment / NT-related** fixes on several repos, not with “main only.”

To see the branch in the UI: open Source Control or check the status bar; to attach a branch run `git checkout <branch>` in each repo (or use the project’s sync workflow).
