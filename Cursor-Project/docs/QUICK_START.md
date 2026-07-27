# Quick Start — New Machine Setup

Onboard this workspace on a new Windows computer.

## Prerequisites (manual)

1. **Clone** this repo from GitHub and open the folder in Cursor.
2. Install **Node.js LTS + npm**.
3. Install Cursor extensions manually:
   - PowerShell (`ms-vscode.powershell`)
   - Playwright Test for VSCode (Modified) (`custom.playwright-custom` VSIX)
4. GitLab access / VPN for Phoenix repos (`git.domain.internal`).

See [`Cursor Setup/extensions.json`](../Cursor%20Setup/extensions.json) for the full manual vs script-install lists.

## Run the setup script

From the workspace root (folder that contains `Cursor-Project/` and `.gitmodules`):

```powershell
.\.cursor\commands\setup-new-machine.ps1
```

This will:

- Clone / init all submodules (EnergoTS + Phoenix repos)
- Create `Cursor-Project/.env` and `Cursor-Project/EnergoTS/.env` from `Cursor Setup/env.example`
- Write MCP servers into `%USERPROFILE%\.cursor\mcp.json` from `Cursor Setup/mcp_content.txt`
- Install marketplace extensions listed under `scriptInstall`
- Verify layout, remotes, env, MCP, and extensions

Full options: [`.cursor/commands/setup-new-machine.md`](../../.cursor/commands/setup-new-machine.md)

```powershell
.\.cursor\commands\setup-new-machine.ps1 -VerifyOnly
.\.cursor\commands\setup-new-machine.ps1 -GitLabToken <token>
```

## After setup

1. Restart Cursor and confirm MCP (Confluence, Jira, PostgreSQL*).
2. Review both `.env` files (portal, Jira, Slack, client credentials).
3. Optional: `git config core.hooksPath Cursor-Project/scripts/git-hooks`

## Checklist

- [ ] Repo cloned and opened in Cursor
- [ ] Node.js / npm installed
- [ ] PowerShell + Playwright Modified extensions installed
- [ ] `setup-new-machine.ps1` exit code `0` (or `2` reviewed)
- [ ] MCP servers visible after Cursor restart
- [ ] `.env` files reviewed
- [ ] EnergoTS on `cursor` branch
- [ ] Phoenix folders under `Cursor-Project/Phoenix/*`

## Related docs

- [README.md](../README.md)
- [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md)
- [Cursor Setup/](../Cursor%20Setup/) — `env.example`, `mcp_content.txt`, `extensions.json`

**Last Updated:** 2026-07-27
