# setup-new-machine

Onboard a new Windows machine for this workspace after the root repo is cloned from GitHub.

## Manual steps (before the script)

1. Clone this workspace from GitHub and open it in Cursor.
2. Install **Node.js LTS + npm** (required for MCP `npx` PostgreSQL servers).
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
| 0 | Preconditions (workspace, `.gitmodules`, templates, npm warning) |
| 1 | Clone/init all submodules from `.gitmodules` (skip if already valid) |
| 2 | Copy `Cursor Setup/env.example` → `Cursor-Project/.env` and `EnergoTS/.env` |
| 3 | Merge `Cursor Setup/mcp_content.txt` into `%USERPROFILE%\.cursor\mcp.json` (backup first) |
| 4 | Install `scriptInstall` extensions via `cursor --install-extension` |
| 5 | Verify repos, remotes, EnergoTS `cursor` branch, env, MCP, extensions, npm |

## Exit codes

| Code | Meaning |
|------|---------|
| `0` | All checks passed |
| `1` | Hard failure (missing templates, clone hard-fail, Cursor CLI missing for install, etc.) |
| `2` | Setup finished with partial verify failures |

## After the script

1. Restart Cursor (or reload MCP servers).
2. Confirm Confluence / Jira / PostgreSQL* MCP servers.
3. Review `.env` files and adjust credentials if needed.
4. Re-run `.\.cursor\commands\setup-new-machine.ps1 -VerifyOnly` anytime.
