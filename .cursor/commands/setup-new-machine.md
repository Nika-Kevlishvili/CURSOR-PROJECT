# setup-new-machine

Onboard a new Windows machine for this workspace after the root repo is cloned from GitHub.

## Manual steps (before the script)

1. Clone this workspace from GitHub and open it in Cursor.
2. Install **Node.js 22.12+ and npm** (official Elastic CLI) and the **PostgreSQL client** (`psql`, via `brew install libpq` on macOS or the PostgreSQL installer on Windows).
3. Install extensions manually:
   - **PowerShell** — `ms-vscode.powershell`
   - **Playwright Test for VSCode (Modified)** — `custom.playwright-custom` (custom VSIX, not Marketplace)
4. Ensure GitLab VPN/credentials work for `git.domain.internal` (Phoenix submodules).

Lists live in [`Cursor-Project/Cursor Setup/extensions.json`](../../Cursor-Project/Cursor%20Setup/extensions.json).

## Run

```powershell
.\.cursor\commands\setup-new-machine.ps1
```

Optional:

```powershell
.\.cursor\commands\setup-new-machine.ps1 -VerifyOnly
.\.cursor\commands\setup-new-machine.ps1 -ForceEnv
.\.cursor\commands\setup-new-machine.ps1 -ForceMcp
.\.cursor\commands\setup-new-machine.ps1 -SkipClone
.\.cursor\commands\setup-new-machine.ps1 -SkipExtensions
.\.cursor\commands\setup-new-machine.ps1 -GitLabToken <token>
```

`-GitLabToken` (or env `GITLAB_TOKEN`) is used for HTTPS clone of Phoenix repos when credentials are not already stored.

## What the script does

| Phase | Action |
|-------|--------|
| 0 | Preconditions (workspace, `.gitmodules`, templates, npm + Python check) |
| 1 | Clone/init all submodules from `.gitmodules` (skip if already valid) |
| 2 | Copy `Cursor Setup/env.example` → `Cursor-Project/.env` and `EnergoTS/.env` |
| 3a | `npm install` the official Elastic CLI in `Cursor-Project/config/elasticsearch` |
| 3 | Merge `Cursor Setup/mcp_content.txt` into **workspace** `.cursor/mcp.json` (backup first); strip Elasticsearch and PostgreSQL MCP servers; clear user-level `%USERPROFILE%\.cursor\mcp.json` if it contains duplicate MCP servers |
| 4 | Install `scriptInstall` extensions via `cursor --install-extension` |
| 5 | Verify repos, remotes, EnergoTS `cursor` branch, env, MCP (no Elasticsearch or PostgreSQL MCP), Elastic CLI binary, `psql-phoenix.py`, extensions, npm |

## Exit codes

| Code | Meaning |
|------|---------|
| `0` | All checks passed |
| `1` | Hard failure (missing templates, clone hard-fail, Cursor CLI missing for install, etc.) |
| `2` | Setup finished with partial verify failures |

## After the script

1. Restart Cursor (or reload MCP servers). Elasticsearch uses the Elastic CLI. PostgreSQL uses `psql` through `Cursor-Project/scripts/psql-phoenix.py`, not MCP.
2. Confirm workspace MCP in `.cursor/mcp.json` has no `Elasticsearch*` or `PostgreSQL*` server, and that user-level `mcp.json` does not duplicate servers.
3. For Dev and Prod logs, set `ELASTIC_DEV_API_KEY` and `ELASTIC_PROD_API_KEY`, or copy `Cursor-Project/config/elasticsearch/secrets.env.example` to `secrets.env`. Do not put those keys in `mcp.json`.
4. For database passwords, copy `Cursor-Project/config/postgres/secrets.env.example` to `secrets.env` and fill `PGPASSWORD_*`. Do not put those passwords in `mcp.json`.
5. Review `.env` files and adjust credentials if needed.
6. Re-run `.\.cursor\commands\setup-new-machine.ps1 -VerifyOnly` anytime.
