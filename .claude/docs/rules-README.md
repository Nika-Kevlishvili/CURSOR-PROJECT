# Project rules layout (thematic folders)

Claude Code loads all `.md` files under `.claude/rules/` **recursively**. A file with no `paths:` frontmatter is loaded in every session (Cursor `alwaysApply: true`). A file with `paths:` globs loads when Claude reads a matching file (Cursor `globs`). `CLAUDE.md` at the workspace root lists the scoped rules so they can also be read on demand.

| Folder | Contents |
|--------|----------|
| **`main/`** | `core_rules.md` (Rule 0.x), `phoenix.md` (index) |
| **`safety/`** | `safety_rules.md` |
| **`agents/`** | `agent_rules.md` |
| **`workflows/`** | `workflow_rules.md`, `playwright_test_constraints.md`, `playwright_detailed_reporting.md` |
| **`workspace/`** | `file_organization_rules.md`, `test_cases_structure.md` |
| **`integrations/`** | `database_workflow.md`, `energots_branch_lock.md`, `production_data_reader.md`, `jira_bug_agent.md` |

**Start here:** `.claude/rules/main/phoenix.md`

When adding a rule, use the matching folder and update `main/phoenix.md` if you add a new canonical document.

**Note:** `Cursor-Project/Phoenix/*/.cursor/rules/` are separate repo rules owned by those repositories.
