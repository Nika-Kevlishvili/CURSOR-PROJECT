# GitLab sync — experiment branch

**Operation:** `!update experiment` per `.cursor/rules/integrations/git_sync_workflow.mdc` (stash → fetch → checkout `experiment` → merge `origin/experiment` if behind; abort if diverged).

**Root:** `Cursor-Project/Phoenix/`

## Per-repo results

| Repo | Status | Detail |
|------|--------|--------|
| phoenix-api-gateway | skipped | No `origin/experiment` |
| phoenix-billing-run | up-to-date | On `experiment`, synced with origin |
| phoenix-core | **diverged** | ahead=28, behind=136 — merge not applied (workflow stop) |
| phoenix-core-lib | **diverged** | ahead=23, behind=179 — merge not applied (workflow stop) |
| phoenix-migration | skipped | No `origin/experiment` |
| phoenix-payment-api | up-to-date | On `experiment`, synced with origin |
| phoenix-ui | merged | Merged 2 commit(s) from `origin/experiment` |

## Not processed as git repos

Directories without `.git`: `mfe-poc-with-nx`, `phoenix-mass-import`, `phoenix-scheduler`.

## Next steps (user)

- **phoenix-core** / **phoenix-core-lib:** Choose how to reconcile (merge, rebase, reset to `origin/experiment`, or keep local commits). Review: `git log HEAD...origin/experiment --oneline`.
- Repos without remote `experiment`: use another branch or confirm whether GitLab should publish `experiment` for those projects.

**No push performed** (read-only sync).
