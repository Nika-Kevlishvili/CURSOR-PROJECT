# Reports (`Cursor-Project/reports/`)

## Canonical path

All task and agent reports use **today’s date folder**:

**`Cursor-Project/reports/YYYY-MM-DD/`**

Example: `Cursor-Project/reports/2026-03-28/PhoenixExpert_1430.md`

This matches **Rule 0.6** (`.cursor/rules/main/core_rules.mdc`) and **`file_organization_rules.mdc`**.

## Naming

| Pattern | Purpose |
|---------|---------|
| `{AgentName}_{HHMM}.md` | Per-agent / per-role report |
| `Summary_{HHMM}.md` | Session summary |
| `BugValidation_{Name}.md` | Bug validation (Rule 32) |
| `{JIRA_KEY}.md` | HandsOff Playwright results |

## How reports are produced (this workspace)

There is **no** Python `ReportingService` or `agents.reporting_service` in this repo. The Cursor assistant **writes markdown files** with editor/file tools.

- Orchestration: **`.cursor/agents/report-generator.md`**
- Skill: **`phoenix-reporting`** (`.cursor/skills/phoenix-reporting/SKILL.md`)

## Agent / subagent map

See **`Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`** for which **`.cursor/agents/*.md`** file corresponds to each workflow.

## Folder layout example

```
Cursor-Project/reports/
├── 2026-03-28/
│   ├── PhoenixExpert_1430.md
│   ├── Summary_1430.md
│   └── BugValidation_Invoice_cancel.md
└── README.md
```
