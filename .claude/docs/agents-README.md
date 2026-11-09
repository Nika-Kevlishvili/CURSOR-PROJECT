# Claude Code Subagents (Project)

Subagents delegate work to specialized contexts. Each file under **`.claude/agents/`** describes how to run that role **in Claude Code chat** (no `Cursor-Project/agents/` Python package in this workspace).

**Location:** Workspace root `.claude/agents/` (same level as `commands/`, `hooks/`, `skills/`).

**Canonical map (roles, report paths, outputs):** `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`

---

## Mapping: Subagent → Role / Rules

| Subagent file | Role | When to use |
|---------------|------|-------------|
| **phoenix-qa.md** | PhoenixExpert (Rule 0.2) + Senior QA lens | Phoenix Q&A; Confluence REST GET + codebase; **Findings** on mismatch. READ-ONLY. |
| **senior-qa.md** | Senior QA Tester (Rule QA.0) | QA audit, doc gaps, code↔doc mismatches, spec vs implementation. READ-ONLY. |
| **bug-validator.md** | Bug validation (Rule 32) | Confluence → codebase → full analysis in chat; **Chat reports** file only if **`/report`** or explicit save. READ-ONLY. |
| **test-runner.md** | TestAgent | Tests; consult PhoenixExpert first; report results. |
| **report-generator.md** | Reports (Rule 0.6) | **`/report`**, **`/feedback`**, or explicit save request only — per **`Cursor-Project/reports/README.md`**. |
| **database-query.md** | DB workflow (Rule DB.0+) | Official psql via `psql-phoenix.py`; see `integrations/database_workflow.md`. |
| **shell.md** | Shell / CLI | Delegated terminal + safe git/CLI with hooks and project safety rules. |
| **environment-access.md** | Environment access | Dev/Dev2; browser/MCP per subagent doc. |
| **environment-resolver.md** | Environment resolver | Resolves `dev/dev2/test/preprod/prod/experiments` from user/Jira; asks user when ambiguous. |
| **postman-collection.md** | Postman collections | PhoenixExpert first; save under `postman/`. |
| **test-case-generator.md** | Test cases (Rule 35) | **MANDATORY:** read `config/playwright_generation/playwright instructions/` before `.md`; Confluence + codebase + `cross_dependency_data`; save Backend `.md` + manual `.xlsx` always; Frontend `.md` + `.xlsx` + `.playwright.md` only if TC-FRONTEND scope includes UI. |
| **test-case-quality-validator.md** | TC quality gate (Rule 35 Step 2.5) | **10-axis, ≥80/100** STRICT scoring; READ-ONLY; max 3 rewrites. |
| **cross-dependency-finder.md** | Cross-dependencies (Rule 35, 35a) | Jira + code + deep Confluence exploration; **no** local merge/git; hand off to test-case-generator. |
| **energo-ts-test.md** | EnergoTSTestAgent (Rule 0.8) | **MANDATORY:** read `config/playwright_generation/playwright instructions/` before `.spec.ts`. API specs under `src/tests/`. |
| **performance-test.md** | PerformanceTestAgent | Asks which object and environment, then writes and runs listing performance specs the same way as Points of Delivery. Skill: `performance-test`. |
| *(no subagent)* | Frontend Playwright (Rule 45) | User entry is **`/playwright-frontend`** only (`.claude/commands/playwright-frontend.md`). Procedures are `.claude/procedures/playwright-frontend/` and `.claude/procedures/playwright-frontend-validator/`. They are not slash entries. |
| **energo-ts-run.md** | Playwright runner (Rule 36) | `npx playwright test` from EnergoTS `src/tests/`; stay on `cursorMerged` or `staging`. JSON is `src/backend/playwright-report.json`. JSON→MD + Slack uploads for **scoped Slack path 2**; ad-hoc runs: generator only if user explicitly asks (**Rule DPR.0**). |
| **jira-bug.md** | Jira bug (Rule JIRA.0) | Experiments board only. |
| **phoenix-bug-reporter.md** | Phoenix Phase 2 bug reporter (Rule PHOENIX-BUG.0) | Review file → Step 3.5 **bug-validator** (parent prefetch) → APPROVE + `.active-bugreview` sidecar → `acli jira workitem create`; hook guards Internal Bug + Bug; reporter from `.env`; Internal tester from QA/Test subtask. |
| **playwright-test-validator.md** | Playwright QA gate | Spec vs test cases + **`playwright instructions/`** (Rule 35 / energo-ts-test quality gate). API specs. |
| **production-data-reader.md** | Production data (Rule PDR.0) | `psql-phoenix.py --environment prod`, read-only. |

---

## Rules and skills

- **Rules:** `.claude/rules/**/*.md` — index: **`main/phoenix.md`**.
- **Skills:** `.claude/skills/` (phoenix-agent-workflow, phoenix-reporting, phoenix-database, etc.)

**Rule 0.3:** No Python `IntegrationService` in chat; use MCP/Jira when external context is needed. **Rule 0.6:** Markdown on disk only for **`/report`** / explicit user request → **Chat reports**, **`/feedback`** / explicit save-feedback → **Feedback**, per **`Cursor-Project/reports/README.md`**; not `ReportingService`.

---

## How Claude Code uses these

- Delegate when a task fits a subagent.
- Subagents are loaded from `.claude/agents/`.
