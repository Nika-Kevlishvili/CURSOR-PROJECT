# Cursor Subagents (Project)

Subagents delegate work to specialized contexts. Each subagent here maps to this project's **existing agents** and **rules** so the main Cursor agent can delegate Phoenix-related tasks consistently.

**Location:** Workspace root `.cursor/agents/` (same level as `commands/`, `hooks/`, `skills/`).

---

## Mapping: Subagent → Project Agent / Rules

| Subagent file       | Maps to                    | When to use |
|---------------------|----------------------------|-------------|
| **phoenix-qa.md**    | PhoenixExpert (Rule 0.2)   | Phoenix Q&A: backend, endpoints, business logic, Confluence + codebase. READ-ONLY. |
| **bug-validator.md**| BugFinderAgent (Rule 32)   | Validate bug: Confluence first, then codebase; report to `reports/YYYY-MM-DD/BugValidation_*.md`. READ-ONLY. |
| **test-runner.md**  | TestAgent (Rule 8, 17)     | Run tests; consult PhoenixExpert first; report results. |
| **report-generator.md** | ReportingService (Rule 0.6) | After task: save agent report + summary to `Cursor-Project/reports/YYYY-MM-DD/`. |
| **database-query.md**  | database_workflow.mdc (Rule DB.0–DB.5) | PostgreSQL MCP: correct env (Dev/Test/Prod), connect first, contract/POD patterns; no credentials in output. |
| **git-sync.md**        | GitLabUpdateAgent / git_sync_workflow.mdc | Sync/update/checkout Phoenix repos from GitLab; !sync, !update &lt;branch&gt;, !checkout &lt;branch&gt;; READ-ONLY (no push). |
| **environment-access.md** | EnvironmentAccessAgent (Rule 10)     | Access Dev or Dev2: navigation, login, environment selection. |
| **postman-collection.md** | PostmanCollectionGenerator (Rule 8, 17) | Generate Postman collections; consult PhoenixExpert first; save to postman/. |
| **test-case-generator.md** | TestCaseGeneratorAgent | Generate test cases from bug/task; Confluence (MCP) + codebase; save to test_cases/. |

---

## Project rules and agents

- **Rules:** `.cursor/rules/*.mdc` (agent_rules, core_rules, workflow_rules, database_workflow, safety_rules, etc.)
- **Python agents:** `Cursor-Project/agents/` (Main: PhoenixExpert, TestAgent, BugFinderAgent; Support: GitLabUpdateAgent, EnvironmentAccessAgent; Core: AgentRouter, IntegrationService; Services: ReportingService)
- **Skills:** `.cursor/skills/` (phoenix-agent-workflow, phoenix-bug-validation, phoenix-reporting, phoenix-database, etc.)

Subagent prompts reference these so delegated work follows the same IntegrationService, PhoenixExpert consultation, and reporting requirements.

---

## How Cursor uses these

- The main agent can **delegate** to these subagents when a task fits (e.g. "validate this bug" → bug-validator).
- Each subagent runs in its **own context**; the parent gets back a summary/result.
- Subagents are **discovered** from `.cursor/agents/`; no extra config needed.
- For more: [Cursor Docs – Subagents](https://cursor.com/docs/context/subagents).
