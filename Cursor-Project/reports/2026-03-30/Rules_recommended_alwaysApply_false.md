# Rules files — recommended `alwaysApply: false`

## Set to `false` (strong recommendation)

| Path | Notes |
|------|--------|
| `.cursor/rules/integrations/git_sync_workflow.mdc` | Very large; `!sync` / `!update` / fetch triggers. Prefer token outside always-on context. |
| `.cursor/rules/integrations/database_workflow.mdc` | DB credentials; load only when using PostgreSQL MCP / user names environment. |
| `.cursor/rules/integrations/production_data_reader.mdc` | Only for production DB / PDR.0 style questions. |
| `.cursor/rules/integrations/energots_branch_lock.mdc` | Scoped to EnergoTS; use `globs` e.g. `Cursor-Project/EnergoTS/**`. |
| `.cursor/rules/workflows/handsoff_playwright_report.mdc` | HandsOff / Playwright / test-case report quality; not every chat. Optional globs: `Cursor-Project/EnergoTS/**`, `Cursor-Project/test_cases/**`. |

## Set to `false` with explicit frontmatter (was missing YAML)

| Path | Notes |
|------|--------|
| `.cursor/rules/integrations/jira_bug_agent.mdc` | Add frontmatter: `alwaysApply: false` + `description:` Experiments board only; never Phoenix delivery bugs. |

## Optional / only after split or stub in `core_rules.mdc`

| Path | Notes |
|------|--------|
| `.cursor/rules/workflows/workflow_rules.mdc` | Large; contains Rule 32, 35, 36, 37. Safer: **split** bug-validation vs HandsOff-only parts, or keep `true` until a one-line pointer exists in `core_rules.mdc` for bug validation. |
| `.cursor/rules/workspace/file_organization_rules.mdc` | If `false`, add short reminders in `core_rules.mdc` for reports + test case paths so root chats still behave. |
| `.cursor/rules/workspace/test_cases_structure.mdc` | Same as file organization — or use `globs` under `Cursor-Project/test_cases/**`. |

## Keep `alwaysApply: true` (do not move to false without replacement)

- `.cursor/rules/main/core_rules.mdc`
- `.cursor/rules/main/phoenix.mdc`
- `.cursor/rules/safety/safety_rules.mdc`
- `.cursor/rules/agents/agent_rules.mdc`

Agents involved: PhoenixExpert
