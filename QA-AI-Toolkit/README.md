# QA AI Toolkit

Generic Cursor QA control plane: rules, skills, agents, hooks, templates, and an **interactive step wizard** installer. No product-specific application stack, no UI-test automation framework, no fixed environment names.

## Quick start

```powershell
# From the machine that has this toolkit folder:
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -TargetPath D:\Path\To\NewOrExistingProject
```

Or from Cursor chat: run the **`/setup-qa-ai-toolkit`** command.

## Wizard behavior

1. Each phase runs, then asks **Proceed to next step? [Y/n/q]**.
2. Phases that need answers use prompts (`project name`, env count, GitLab selection, DB fields).
3. **`.env` must be filled before any GitLab API/clone** (hard gate).
4. GitLab repos are **chosen at clone time** — not a permanent inventory. Re-run from step 6 later to add more.
5. Quit anytime with `q`; progress is kept (`wizardLastCompletedStep`). Resume with `-FromStep N`.

## What gets installed into the target

| Path | Purpose |
|---|---|
| `.cursor/` | Rules, skills, agents, hooks |
| `.cursor/project-config.json` | Project name, environments, codeRoot |
| `.env` | Empty credential template |
| `config/scripts/` | Jira/Confluence REST + GitLab helpers |
| `config/templates/` | Test case / bug / Finding templates |
| `docs/` | Operating docs |
| `test_cases/`, `reports/`, `<codeRoot>/` | Skeleton folders |
| `%USERPROFILE%\.cursor\mcp.json` | Merged Confluence, Jira, PostgreSQL* |

## Other scripts

```powershell
.\QA-AI-Toolkit\scripts\verify-qa-ai-toolkit.ps1 -TargetPath D:\MyProject
.\QA-AI-Toolkit\scripts\upgrade-qa-ai-toolkit.ps1 -TargetPath D:\MyProject
.\QA-AI-Toolkit\scripts\assert-no-phoenix-leakage.ps1
```

## After install

1. Restart Cursor and authenticate Atlassian MCP (`docs/MCP_AUTH.md`).
2. Confirm MCP servers.
3. Run `-VerifyOnly` anytime.

## Security

Templates ship with **empty** secrets only. Never commit filled `.env` or user `mcp.json`.

## Maintainer check

```powershell
.\QA-AI-Toolkit\scripts\assert-no-phoenix-leakage.ps1
```

Fails if product-specific strings leak into `.cursor-template` or `config`.
