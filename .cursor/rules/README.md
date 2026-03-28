# Project rules layout (thematic folders)

Cursor loads all `.mdc` files under `.cursor/rules/` **recursively**.

| Folder | Contents |
|--------|----------|
| **`main/`** | `core_rules.mdc` (Rule 0.x), `phoenix.mdc` (index) |
| **`safety/`** | `safety_rules.mdc` |
| **`agents/`** | `agent_rules.mdc` |
| **`workflows/`** | `workflow_rules.mdc`, `handsoff_playwright_report.mdc` |
| **`workspace/`** | `file_organization_rules.mdc`, `test_cases_structure.mdc` |
| **`integrations/`** | `database_workflow.mdc`, `git_sync_workflow.mdc`, `energots_branch_lock.mdc`, `production_data_reader.mdc`, `jira_bug_agent.mdc` |

**Start here:** `.cursor/rules/main/phoenix.mdc`

When adding a rule, use the matching folder and update `main/phoenix.mdc` if you add a new canonical document.

**Note:** `Cursor-Project/Phoenix/*/.cursor/rules/` are separate repo rules and also appear in Cursor Settings.
