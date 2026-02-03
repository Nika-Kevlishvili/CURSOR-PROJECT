# Cursor Subagents in This Project

This project defines **Cursor Subagents** in `.cursor/agents/` (workspace root). Each subagent maps to an existing project agent or workflow so the main Cursor agent can delegate Phoenix-related tasks correctly.

---

## What Are Subagents?

Subagents are specialized AI assistants the main Cursor agent can delegate to. They run in their own context, return a result to the parent, and can be used for parallel or isolated work. See [Cursor Docs – Subagents](https://cursor.com/docs/context/subagents).

---

## Subagents in This Project

| Subagent | File | Maps to | Purpose |
|----------|------|---------|---------|
| **Phoenix Q&A** | `phoenix-qa.md` | PhoenixExpert (Rule 0.2) | Answer Phoenix questions from Confluence (MCP) + codebase. READ-ONLY. |
| **Bug Validator** | `bug-validator.md` | BugFinderAgent (Rule 32) | Validate bug: Confluence first, then codebase; save report to `reports/YYYY-MM-DD/BugValidation_*.md`. READ-ONLY. |
| **Test Runner** | `test-runner.md` | TestAgent (Rule 8, 17) | Run tests; consult PhoenixExpert first; report results. |
| **Report Generator** | `report-generator.md` | ReportingService (Rule 0.6) | Save agent report + summary to `Cursor-Project/reports/YYYY-MM-DD/`. |
| **Database Query** | `database-query.md` | database_workflow.mdc | Run PostgreSQL MCP queries; correct env (Dev/Test/Prod); connect first; contract/POD patterns. |
| **Production Data Reader** | `production-data-reader.md` | ProductionDataReaderAgent (Rule PDR.0) | Read production database data; analyze liability offsets, receivable history; explain step-by-step creation process. READ-ONLY. |
| **Git Sync** | `git-sync.md` | GitLabUpdateAgent / git_sync_workflow.mdc | Sync/update/checkout Phoenix repos from GitLab; READ-ONLY (fetch/checkout/merge only). |
| **Environment Access** | `environment-access.md` | EnvironmentAccessAgent (Rule 10) | Access Dev or Dev2; navigation, login, environment selection. |
| **Postman Collection** | `postman-collection.md` | PostmanCollectionGenerator (Rule 8, 17) | Generate Postman collections; consult PhoenixExpert first. |
| **Test Case Generator** | `test-case-generator.md` | TestCaseGeneratorAgent | Generate test cases from bug/task; Confluence + codebase; save to test_cases/. |

---

## Relation to Project Agents

- **PhoenixExpert** → Cursor subagent **phoenix-qa**: same “Phoenix Q&A” role; subagent runs in isolated context.
- **BugFinderAgent** → Cursor subagent **bug-validator**: same Rule 32 workflow (Confluence → code → report).
- **TestAgent** → Cursor subagent **test-runner**: same “run tests + consult PhoenixExpert” pattern.
- **ReportingService** → Cursor subagent **report-generator**: same “save report + summary” (Rule 0.6).
- **Database workflow** (Rule DB.0–DB.5) → Cursor subagent **database-query**: same env selection and query patterns.
- **GitLabUpdateAgent** / **git_sync_workflow.mdc** → Cursor subagent **git-sync**: same sync/update/checkout workflow; read-only.
- **EnvironmentAccessAgent** (Rule 10) → Cursor subagent **environment-access**: same Dev/Dev2 access workflow.
- **PostmanCollectionGenerator** (Rule 8, 17) → Cursor subagent **postman-collection**: same “consult PhoenixExpert → generate collection” pattern.
- **TestCaseGeneratorAgent** → Cursor subagent **test-case-generator**: same "Confluence + codebase → generate test cases" workflow; save to test_cases/.

Python agents live in `Cursor-Project/agents/` and are invoked via code (e.g. `get_bug_finder_agent()`). Cursor subagents are invoked by the main Cursor agent when it delegates a task. Both follow the same rules (IntegrationService, PhoenixExpert consultation, reporting).

---

## Where Subagents Live

- **Path:** Workspace root `.cursor/agents/` (same level as `.cursor/commands/`, `.cursor/hooks/`, `.cursor/skills/`).
- **Format:** One markdown file per subagent with YAML frontmatter (`name`, `description`) and instructions in the body.
- **Discovery:** Cursor discovers them automatically; they appear in the agent’s available tools.

---

## Rules and Skills

- **Rules:** `Cursor-Project/.cursor/rules/*.mdc` – subagent prompts refer to these (e.g. Rule 0.2, 0.6, 8, 11, 32, DB.0–DB.5).
- **Skills:** `.cursor/skills/` – phoenix-agent-workflow, phoenix-bug-validation, phoenix-reporting, phoenix-database, etc. Skills guide when/how to use workflows; subagents are the actual delegation targets for those workflows.

Together, rules + skills + subagents keep Phoenix workflows consistent whether the main agent does the work or delegates to a subagent.
