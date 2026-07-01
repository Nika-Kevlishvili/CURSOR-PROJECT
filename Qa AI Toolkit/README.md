# Qa AI Toolkit

Universal **Cursor QA agent framework** — copy into any project. Same structure as a full `.cursor` workspace (agents, rules, skills, commands, hooks) without being locked to Phoenix, EnergoTS, or HandsOff.

## Contents

```
Qa AI Toolkit/
  setup.ps1              <- run this to deploy + configure
  README.md              <- this file
  template/
    .cursor/             <- full Cursor orchestration template
    project/             <- config, scripts, test_cases, reports skeletons
```

## Quick start

1. Copy the **`Qa AI Toolkit`** folder anywhere (USB, monorepo, shared drive).
2. From PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1"
```

3. Enter:
   - **Target workspace root** — your new or existing project folder
   - **Project folder name** — where config/scripts live (default: `Project`)
4. Complete the credential wizard (Jira, Confluence, Swagger, optional DB/Slack).
5. Open the **target** folder in Cursor.
6. Paste `Project/Cursor Setup/mcp.generated.json` into **Cursor Settings → MCP**.

## What you get after setup

| Path (under target) | Purpose |
|---------------------|---------|
| `.cursor/` | Rules, agents, skills, hooks |
| `Project/` (or your name) | Swagger config, scripts, test case & report templates |
| `.env` | Secrets (gitignored) |
| `.qa-toolkit.json` | Install manifest (paths, date) |

## Workflows included

- Bug validation (Rule 32)
- Test case generation (Rule 35)
- Cross-dependency analysis
- Regression validator
- Senior QA / product expert Q&A
- Postman collection helper
- Production data reader (read-only, gated)

**Not included:** Playwright/EnergoTS, HandsOff orchestration, cached Jira/Confluence data.

## Re-run / update

```powershell
powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1" -TargetPath C:\path\to\workspace -ProjectFolder Project
```

Use `-SkipWizard` to redeploy template only without changing `.env`.

## Help

```powershell
powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1" -Help
```

## Customization

- **Protected app code path:** set `QA_APP_CODE_GLOB` in `.env` (Tier A — AI read-only).
- **Atlassian hosts:** `JIRA_BASE_URL`, `CONFLUENCE_WIKI_BASE` in `.env`.
- **Ticket prefix:** `QA_TICKET_PREFIX` (e.g. `PROJ`, `ABC`).

Edit files under `template/` before distributing if your organization needs different defaults.
