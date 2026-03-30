# Phoenix repos aligned with GitLab (`origin`)

**Date:** 2026-03-28  
**Action:** Per repo under `Cursor-Project/Phoenix/`: stash if dirty → `git fetch origin --prune` → `git merge --no-edit origin/<current-branch>` → `git stash pop` when stash existed.

## Result

All seven Git repos: **working tree clean**, **0 behind / 0 ahead** vs `origin/<branch>`.

| Repo | Branch |
|------|--------|
| phoenix-api-gateway | main |
| phoenix-billing-run | experiment |
| phoenix-core | experiment |
| phoenix-core-lib | experiment |
| phoenix-migration | master |
| phoenix-payment-api | experiment |
| phoenix-ui | experiment |

## phoenix-ui follow-up

- `stash pop` after merge reported issues; branch was already **up to date with `origin/experiment`**.
- Two files showed as **deleted** locally (Windows **path too long** blocked `git restore` until `git config core.longpaths true` in that repo).
- After `core.longpaths true`, `git restore` succeeded; tree is clean.

## Note

This used **local merge** (no push). GitLab was not modified. To keep matching `origin`, use `!sync` + `!update <branch>` when needed, or repeat merge after fetch.

Agents involved: GitLabUpdateAgent
