# Cursor Subagents in This Project

This project defines **Cursor Subagents** in `.cursor/agents/` (workspace root). Each subagent maps to an existing project agent or workflow so the main Cursor agent can delegate Phoenix-related tasks correctly.

**Canonical path map (subagent file → role → outputs):** [`AGENT_SUBAGENT_MAP.md`](AGENT_SUBAGENT_MAP.md).

---

## What Are Subagents?

Subagents are specialized AI assistants the main Cursor agent can delegate to. They run in their own context, return a result to the parent, and can be used for parallel or isolated work. See [Cursor Docs – Subagents](https://cursor.com/docs/context/subagents).

---

## Subagents in This Project

| Subagent | File | Maps to | Purpose |
|----------|------|---------|---------|
| **Phoenix Q&A** | `phoenix-qa.md` | PhoenixExpert (Rule 0.2) | Answer Phoenix questions from Confluence (MCP) + codebase. READ-ONLY. |
| **Bug Validator** | `bug-validator.md` | BugFinderAgent (Rule 32) | Validate bug: Confluence first, then codebase; save report to **`Cursor-Project/reports/YYYY-MM-DD/BugValidation_*.md`**. READ-ONLY. |
| **Test Runner** | `test-runner.md` | TestAgent (Rule 8, 17) | Run tests; consult PhoenixExpert first; report results. |
| **Report Generator** | `report-generator.md` | Rule 0.6 | Save agent report + summary to `Cursor-Project/reports/YYYY-MM-DD/` (markdown via file tools; no Python ReportingService). |
| **Database Query** | `database-query.md` | database_workflow.mdc | Run PostgreSQL MCP queries; correct env (Dev/Test/Prod); connect first; contract/POD patterns. |
| **Production Data Reader** | `production-data-reader.md` | ProductionDataReaderAgent (Rule PDR.0) | Read production database data; analyze liability offsets, receivable history; explain step-by-step creation process. READ-ONLY. |
| **Git Sync** | `git-sync.md` | GitLabUpdateAgent / git_sync_workflow.mdc | Sync/update/checkout Phoenix repos from GitLab; READ-ONLY (fetch/checkout/merge only). |
| **Environment Access** | `environment-access.md` | EnvironmentAccessAgent (Rule 10) | Access Dev or Dev2; navigation, login, environment selection. |
| **Postman Collection** | `postman-collection.md` | PostmanCollectionGenerator (Rule 8, 17) | Generate Postman collections; consult PhoenixExpert first. |
| **Test Case Generator** | `test-case-generator.md` | TestCaseGeneratorAgent (role in chat) | Generate test cases from bug/task; Confluence + codebase; save under `Cursor-Project/test_cases/` (Objects/Flows per `test_cases_structure.mdc`). |

---

## Relation to Project Agents

- **PhoenixExpert** → Cursor subagent **phoenix-qa**: same “Phoenix Q&A” role; subagent runs in isolated context.
- **BugFinderAgent** → Cursor subagent **bug-validator**: same Rule 32 workflow (Confluence → code → report).
- **TestAgent** → Cursor subagent **test-runner**: same “run tests + consult PhoenixExpert” pattern.
- **Rule 0.6 reporting** → Cursor subagent **report-generator**: save markdown report + summary under `Cursor-Project/reports/YYYY-MM-DD/`.
- **Database workflow** (Rule DB.0–DB.5) → Cursor subagent **database-query**: same env selection and query patterns.
- **GitLabUpdateAgent** / **git_sync_workflow.mdc** → Cursor subagent **git-sync**: same sync/update/checkout workflow; read-only.
- **EnvironmentAccessAgent** (Rule 10) → Cursor subagent **environment-access**: same Dev/Dev2 access workflow.
- **PostmanCollectionGenerator** (Rule 8, 17) → Cursor subagent **postman-collection**: same “consult PhoenixExpert → generate collection” pattern.
- **TestCaseGeneratorAgent** → Cursor subagent **test-case-generator**: same "Confluence + codebase → generate test cases" workflow; save to test_cases/.

**This workspace:** The **`Cursor-Project/agents/`** Python package is **not** present. Roles are implemented **in Cursor chat** via **`.cursor/agents/*.md`**, **`.cursor/rules/**/*.mdc`**, **skills**, and **MCP** (Jira, Confluence, PostgreSQL, Slack, etc.). **Rule 0.3:** external automation elsewhere may still use `IntegrationService.update_before_task()` where that code exists; in Cursor chat follow MCP/Jira/GitLab steps — no `from agents ...` imports.

---

## Where Subagents Live

- **Path:** Workspace root `.cursor/agents/` (same level as `.cursor/commands/`, `.cursor/hooks/`, `.cursor/skills/`).
- **Format:** One markdown file per subagent with YAML frontmatter (`name`, `description`) and instructions in the body.
- **Discovery:** Cursor discovers them automatically; they appear in the agent’s available tools.

---

## Rules and Skills

- **Rules:** `.cursor/rules/**/*.mdc` – index: `main/phoenix.mdc`; subagents refer to Rule 0.2, 0.3, 0.6, 8, 32, 35, 35a, DB.0–DB.5, etc.
- **Skills:** `.cursor/skills/` – phoenix-agent-workflow, phoenix-bug-validation, phoenix-reporting, phoenix-database, etc. Skills guide when/how to use workflows; subagents are the actual delegation targets for those workflows.

Together, rules + skills + subagents keep Phoenix workflows consistent whether the main agent does the work or delegates to a subagent.
