---
description: Set up or check this workspace on a new machine (submodules, env files, dependencies, tools)
---

# setup-new-machine

Set up this workspace for Claude Code after cloning it. Works on macOS, Linux and Windows. Safe to run again at any time.

## Before the script

Install these four yourself. The script checks them and reports any that is missing.

| Tool | Why | Install |
|------|-----|---------|
| Git | clone the repositories | macOS `brew install git` · Windows `winget install Git.Git` |
| PowerShell 7 (`pwsh`) | this script and every Claude Code hook run on it | macOS `brew install powershell` · Windows `winget install Microsoft.PowerShell` |
| Node.js 22.12 or later | Elastic CLI and Playwright | macOS `brew install node` · Windows `winget install OpenJS.NodeJS.LTS` |
| Python 3 | database and Elasticsearch wrappers | macOS `brew install python` · Windows `winget install Python.Python.3.13` |

Also connect to the VPN and make sure your GitLab login works for `git.domain.internal`. The Phoenix repositories are cloned from there. Git asks for the credentials on the first clone.

## Run

From the workspace root (the folder that contains `Cursor-Project/` and `.gitmodules`):

```bash
pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1
```

| Option | Effect |
|--------|--------|
| `-VerifyOnly` | Change nothing, only report |
| `-SkipClone` | Do not clone submodules |
| `-SkipInstall` | Do not run `npm install` |
| `-SkipBrowsers` | Do not download Playwright browsers. Use it when other Playwright projects on the machine share the browser cache: the Playwright installer removes cached browser builds that no installation it knows about uses |
| `-ForceEnv` | Overwrite both `.env` files and both `secrets.env` files from their templates |
| `-InstallGitHooks` | Set `core.hooksPath` to `Cursor-Project/scripts/git-hooks` (Conventional Commits check on commit messages, rules validator before commit) |
| `-EnergoTSBranch <name>` | Branch for a freshly cloned EnergoTS: `cursorMerged` (default), `staging` or `cursor` |

## What it does

| Step | Action |
|------|--------|
| Submodules | Clones every repository in `.gitmodules` that is not there yet. Puts a fresh EnergoTS on `cursorMerged`. An EnergoTS that is already on `cursor`, `staging` or `cursorMerged` is left alone |
| Env files | Creates `Cursor-Project/.env` and `Cursor-Project/EnergoTS/.env` from `Cursor Setup/env.example`, and `secrets.env` from `secrets.env.example` in `config/postgres` and `config/elasticsearch`. Existing files are kept |
| Dependencies | `npm install` in `config/elasticsearch` (Elastic CLI) and in EnergoTS, then downloads the Playwright browsers that are missing |
| Wiring | Creates `.claude/logs/`. On Windows, rebuilds the two symlinked skills (`playwright-cli`, `flaky-test-fixer`) as junctions when Git checked them out as text files |
| Verify | Tools, Claude Code config (runs both `validate-claude-*.ps1` scripts), skills, repositories, env keys (names only, values are never printed), packages, browsers, optional tools |

## Reading the result

- `OK` — nothing to do.
- `FAIL` — must be fixed. The **To fix** list at the end gives the command or the file to edit for each one.
- `WARN` — optional. Each one switches on a single integration: `acli` for Jira, `psql` for database queries, `secrets.env` values for the database and for Dev/Prod logs, `playwright-cli` for `/playwright-frontend`.

Exit codes: `0` ready, `2` finished with items to fix, `1` not the workspace root or Git missing.

## After the script

1. Fill the keys the report lists as empty. Never paste the values into chat.
2. Log in to Jira once: `acli jira auth login`.
3. Open the workspace folder in Claude Code. Rules, agents, skills, commands and hooks load from `.claude/` with no further setup.
4. Check again at any time with `-VerifyOnly`.

## When invoked as `/setup-new-machine`

Run the script with `-VerifyOnly` first and show the user the result. Run it without `-VerifyOnly` only after the user agrees, because it clones repositories and downloads packages. Do not fill, print or ask for secret values in chat. Tell the user which file and which keys to fill.
