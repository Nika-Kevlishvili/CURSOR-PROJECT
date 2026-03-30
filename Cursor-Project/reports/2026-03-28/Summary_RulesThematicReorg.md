# Summary — Thematic `.cursor/rules/` reorganization

**Date:** 2026-03-28  
**Request:** Split rules by theme; main rules separate for universal use; safety separate; etc.

## New layout

- `main/` — `core_rules.mdc`, `phoenix.mdc` (index)
- `safety/` — `safety_rules.mdc`
- `agents/` — `agent_rules.mdc`
- `workflows/` — `workflow_rules.mdc`, `handsoff_playwright_report.mdc`
- `workspace/` — `file_organization_rules.mdc`, `test_cases_structure.mdc`
- `integrations/` — `database_workflow.mdc`, `git_sync_workflow.mdc`, `energots_branch_lock.mdc`, `production_data_reader.mdc`, `jira_bug_agent.mdc`

Also added `.cursor/rules/README.md` (human map).

## Other updates

- **Rule 0.0** (`core_rules.mdc`): explicit subfolder + glob guidance.
- **`phoenix.mdc`**: rewritten as thematic index with full canonical paths.
- Cross-links inside rules + references in `.cursor/commands`, `.cursor/agents`, `.cursor/skills`, `Cursor-Project/docs/`, templates, hooks, examples — paths updated to new locations (bulk replace + manual fixes).

**Verify:** Cursor Settings → Project Rules — all rules still listed; run one Agent chat to confirm `alwaysApply` behavior.

**Agents involved:** PhoenixExpert (orchestration), Reporting (Rule 0.6)
