# Quick Start — New Machine Setup

Set up this workspace for Claude Code on a new computer (macOS, Linux or Windows).

## 1. Install the basics

| Tool | macOS | Windows |
|------|-------|---------|
| Git | `brew install git` | `winget install Git.Git` |
| PowerShell 7 (`pwsh`) | `brew install powershell` | `winget install Microsoft.PowerShell` |
| Node.js 22.12 or later | `brew install node` | `winget install OpenJS.NodeJS.LTS` |
| Python 3 | `brew install python` | `winget install Python.Python.3.13` |
| Claude Code | desktop app or CLI | desktop app or CLI |

PowerShell 7 is required: the setup script and every Claude Code hook in `.claude/hooks/` run on it.

Connect to the VPN and make sure your GitLab login works for `git.domain.internal` (Phoenix repositories).

## 2. Clone and run the setup script

```bash
git clone -b <branch> <workspace-repo-url>
```

From the workspace root (the folder that contains `Cursor-Project/` and `.gitmodules`):

```bash
pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1
```

This will:

- Clone all submodules (EnergoTS + Phoenix repositories) and put EnergoTS on `cursorMerged`
- Create `Cursor-Project/.env`, `Cursor-Project/EnergoTS/.env`, `config/postgres/secrets.env` and `config/elasticsearch/secrets.env` from their templates (existing files are kept)
- Install the Elastic CLI, the EnergoTS packages and the Playwright browsers
- Check tools, Claude Code config, repositories, env keys and optional integrations
- List the credential files and keys to fill, wait while you fill them, then check again and test the Confluence, Slack and portal logins
- Print a **To fix** list for anything still open

Options and details: [`.claude/commands/setup-new-machine.md`](../../.claude/commands/setup-new-machine.md)

## 3. Finish by hand

1. Fill the keys the script reports as empty (`.env` files and `secrets.env` files).
2. Install the optional tools you need and log in:
   - Jira: official Atlassian CLI, then `acli jira auth login`
   - Database: PostgreSQL client (`psql`). macOS: `brew install libpq && brew link --force libpq`
   - Frontend locator recording: `npm install -g @playwright/cli@latest`
3. Check again:

```bash
pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1 -VerifyOnly
```

4. Open the workspace folder in Claude Code. Rules, agents, skills, commands and hooks load from `.claude/` automatically.

## Checklist

- [ ] Git, PowerShell 7, Node.js 22.12+, Python 3 installed
- [ ] `setup-new-machine.ps1` exit code `0`
- [ ] `.env` and `secrets.env` values filled
- [ ] `acli jira auth status` shows your account
- [ ] EnergoTS on `cursorMerged` (or `staging` / `cursor`)
- [ ] Phoenix folders under `Cursor-Project/Phoenix/*`

## Related docs

- [README.md](../README.md)
- [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md)
- [`.claude/README.md`](../../.claude/README.md) — how the Claude Code configuration is laid out
- [Cursor Setup/](../Cursor%20Setup/) — `env.example` template

**Last Updated:** 2026-11-09
