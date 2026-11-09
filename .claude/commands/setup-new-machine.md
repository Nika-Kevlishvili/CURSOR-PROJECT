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
| `-NoPrompt` | Do not wait for input. List the credentials to fill and exit |
| `-SkipLiveChecks` | Do not log in to Confluence, Slack and the portal to test the credentials |
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
| Live logins | With the filled credentials: one Confluence search, Slack `auth.test`, and a portal login on the Test endpoint. A rejected login is a `FAIL`. An unreachable service (VPN off) is a `WARN` |
| Credentials | Lists every file with empty or rejected keys, waits for you to fill them, then runs Verify and the live logins again. Repeats until the required keys are filled and accepted, or you type `s` |

## Where the credentials go

| File | Keys | Used for |
|------|------|----------|
| `Cursor-Project/.env` | `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_BASE_URL`, `CONFLUENCE_URL` | Confluence reads, Jira attachments |
| `Cursor-Project/EnergoTS/.env` | `PORTAL_USER`, `PASSWORD`, `DEVAUTHAPI`, `TESTAUTHAPI`, `SLACK_BOT_TOKEN` (or `SLACK_API_TOKEN`), `CLIENT_ID`, `CLIENT_SECRET` | Playwright runs, Slack reports, sales portal login |
| `Cursor-Project/config/postgres/secrets.env` | `PGPASSWORD_<ENV>` | Database queries (optional, one per environment you query) |
| `Cursor-Project/config/elasticsearch/secrets.env` | `ELASTIC_DEV_API_KEY`, `ELASTIC_PROD_API_KEY` | Dev and Prod log search (optional) |

All four files are git-ignored. The script prints the full path of each file that still needs values.

## Reading the result

- `OK` — nothing to do.
- `FAIL` — must be fixed. The **To fix** list at the end gives the command or the file to edit for each one.
- `WARN` — optional. Each one switches on a single integration: `acli` for Jira, `psql` for database queries, `secrets.env` values for the database and for Dev/Prod logs, `playwright-cli` for `/playwright-frontend`.

Exit codes: `0` ready, `2` finished with items to fix, `1` not the workspace root or Git missing.

## After the script

1. Log in to Jira once: `acli jira auth login`.
2. Open the workspace folder in Claude Code. Rules, agents, skills, commands and hooks load from `.claude/` with no further setup.
3. Check again at any time with `-VerifyOnly`.

## When invoked as `/setup-new-machine`

1. Run the script with `-VerifyOnly` and show the user the result.
2. If setup work is needed, ask the user, then run the script with `-NoPrompt`. It clones repositories and downloads packages. `-NoPrompt` is required because the script cannot wait for keyboard input when you run it.
3. If the output has a **Credentials to fill** section, show the user each file path with its keys and what they are used for. Do not ask for the values, do not print them, and do not write them into the files yourself. The user types them into the files.
4. Ask with `AskUserQuestion` whether the files are filled ("Filled, check again" / "Skip for now"). Wait for the answer.
5. On "Filled, check again", run the script with `-VerifyOnly`. It repeats every check and the live logins. Repeat steps 3 to 5 while required credentials are still empty or rejected.
6. Finish with the summary: what is OK, what is left to fix, what is optional.
