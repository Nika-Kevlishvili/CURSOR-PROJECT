# Cursor Subagents (Project)

Subagents delegate work to specialized contexts. Each file under **`.cursor/agents/`** describes how to run that role **in Cursor chat** (no `Cursor-Project/agents/` Python package in this workspace).

**Location:** Workspace root `.cursor/agents/` (same level as `commands/`, `hooks/`, `skills/`).

**Canonical map (roles, report paths, outputs):** `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`

---

## Mapping: Subagent → Role / Rules

| Subagent file | Role | When to use |
|---------------|------|-------------|
| **phoenix-qa.md** | PhoenixExpert (Rule 0.2) | Phoenix Q&A; Confluence MCP + codebase. READ-ONLY. |
| **bug-validator.md** | Bug validation (Rule 32) | Confluence → codebase → **Chat reports** per **`Cursor-Project/reports/README.md`**. READ-ONLY. |
| **test-runner.md** | TestAgent | Tests; consult PhoenixExpert first; report results. |
| **report-generator.md** | Reports (Rule 0.6) | On user/parent request or workflow-mandated file: markdown per **`Cursor-Project/reports/README.md`**. |
| **database-query.md** | DB workflow (Rule DB.0+) | PostgreSQL MCP; connect first; see `integrations/database_workflow.mdc`. |
| **git-sync.md** | Git sync | `integrations/git_sync_workflow.mdc`; read-only GitLab. |
| **shell.md** | Shell / CLI | Delegated terminal + safe git/CLI; hooks + `git_sync_workflow` for multi-repo sync. |
| **environment-access.md** | Environment access | Dev/Dev2; browser/MCP per subagent doc. |
| **postman-collection.md** | Postman collections | PhoenixExpert first; save under `postman/`. |
| **test-case-generator.md** | Test cases (Rule 35) | **MANDATORY:** read `config/playwright_generation/playwright instructions/` before `.md`; Confluence + codebase + `cross_dependency_data`; save as two files: `test_cases/Backend/<Topic>.md` + `test_cases/Frontend/<Topic>.md`. |
| **cross-dependency-finder.md** | Cross-dependencies (Rule 35, 35a) | Jira + code + shallow Confluence; **no** local merge/git; hand off to test-case-generator. |
| **energo-ts-test.md** | EnergoTSTestAgent (Rule 0.8.1) | **MANDATORY:** read `config/playwright_generation/playwright instructions/` before `.spec.ts`; Playwright under `EnergoTS/tests/` only. |
| **energo-ts-run.md** | Playwright runner (Rule 36) | `npx playwright test` from EnergoTS; `cursor` branch only. |
| **jira-bug.md** | Jira bug (Rule JIRA.0) | Experiments board only. |
| **hands-off.md** | HandsOff orchestrator (Rule 37) | Jira + `/HandsOff` / `!HandsOff` full flow. |
| **playwright-test-validator.md** | Playwright QA gate | Spec vs test cases + **`playwright instructions/`** (HandsOff Step 4.5). |
| **production-data-reader.md** | Production data (Rule PDR.0) | PostgreSQLProd MCP readonly. |

---

## Rules and skills

- **Rules:** `.cursor/rules/**/*.mdc` — index: **`main/phoenix.mdc`**.
- **Skills:** `.cursor/skills/` (phoenix-agent-workflow, phoenix-reporting, phoenix-database, etc.)

**Rule 0.3:** No Python `IntegrationService` in chat; use MCP/Jira when external context is needed. **Rule 0.6:** Markdown on disk only when requested or when a workflow mandates it; paths per **`Cursor-Project/reports/README.md`**; not `ReportingService`.

---

## How Cursor uses these

- Delegate when a task fits a subagent.
- Subagents are loaded from `.cursor/agents/`.
