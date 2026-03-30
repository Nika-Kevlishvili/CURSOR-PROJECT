# Sync (`!sync`) — Phoenix fetch

**Date:** 2026-03-28  
**Scope:** `Cursor-Project/Phoenix/` (read-only `git fetch`)

## Repos processed (7)

| Repo | Result | Stash used |
|------|--------|------------|
| phoenix-api-gateway | success | no |
| phoenix-billing-run | success | no |
| phoenix-core | success | no |
| phoenix-core-lib | success | no |
| phoenix-migration | success | no |
| phoenix-payment-api | success | no |
| phoenix-ui | success | yes (stashed before fetch, popped after) |

## Not processed (no `.git` under Phoenix)

- `mfe-poc-with-nx` — no `.git` in that folder (not processed as a repo)
- `phoenix-mass-import`
- `phoenix-scheduler`

*(Only directories containing `.git` were included.)*

## Notes

- Git Credential Manager emitted TLS verification warnings (environment-specific); fetches still completed.
- **phoenix-ui:** remote branch cleanup/rename activity reported (deleted `origin/BugFix/...`, new `origin/bugfix/...`).

## Doc fix

Replaced invalid **`git fetch origin --all`** with **`git fetch origin --prune`** in `git_sync_workflow.mdc`, `sync.md`, and `git-sync.md` (Git rejects `origin` + `--all` together).

Agents involved: GitLabUpdateAgent (sync workflow; direct git)
