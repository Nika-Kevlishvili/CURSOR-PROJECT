# Rules `alwaysApply` review — 2026-03-30

## Current state (workspace)

All listed `.mdc` files under `.cursor/rules/` use `alwaysApply: true` **except** `integrations/jira_bug_agent.mdc`, which has **no frontmatter** (should add `alwaysApply` explicitly).

Files verified:

| File | alwaysApply |
|------|-------------|
| main/core_rules.mdc | true |
| main/phoenix.mdc | true |
| safety/safety_rules.mdc | true |
| agents/agent_rules.mdc | true |
| workflows/workflow_rules.mdc | true |
| workflows/handsoff_playwright_report.mdc | true |
| workspace/file_organization_rules.mdc | true |
| workspace/test_cases_structure.mdc | true |
| integrations/git_sync_workflow.mdc | true |
| integrations/database_workflow.mdc | true |
| integrations/energots_branch_lock.mdc | true |
| integrations/production_data_reader.mdc | true |
| integrations/jira_bug_agent.mdc | **missing YAML** |

## Recommendation summary

- **Keep always on:** `core_rules.mdc`, `safety_rules.mdc`, `agent_rules.mdc`, `phoenix.mdc` (index), and a **short** routing stub if large workflows are split off.
- **Strong candidates to turn off `alwaysApply`:** `git_sync_workflow.mdc` (very large; token exposure), `database_workflow.mdc` (credentials in context), `production_data_reader.mdc`, `energots_branch_lock.mdc` (path-scoped glob).
- **Split or scope:** `workflow_rules.mdc` + `handsoff_playwright_report.mdc` — consider HandsOff-only file with `globs` + Agent description; keep bug validation / Rule 32 in always or in a clearly linked file.
- **Risky to scope only by path:** `file_organization_rules.mdc`, `test_cases_structure.mdc` — chats from repo root may miss them unless a one-line reminder stays in `core_rules.mdc`.

## Security note

Embedding Git PAT and DB passwords in rules that are `alwaysApply: true` increases accidental exposure in model context and logs. Prefer env-based tooling and `alwaysApply: false` + narrow triggers for those files.

Agents involved: PhoenixExpert (rules/workspace advisory)
