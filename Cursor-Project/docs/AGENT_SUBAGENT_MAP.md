# Agent roles — canonical paths (whole project)

Use this file as the **single map** from “agent role” to **where it lives today**. The old Python tree **`Cursor-Project/agents/`** is **not** in this workspace.

## 1. Subagent specs (Cursor)

**Path pattern:** `<workspace-root>/.cursor/agents/<name>.md`  
(The folder that contains `Cursor-Project` also contains `.cursor`.)

| Subagent file | Role (concept) | Primary trigger |
|---------------|----------------|-----------------|
| **phoenix-qa.md** | Phoenix Q&A (PhoenixExpert) | `/phoenix`, Phoenix questions (Rule 0.2) |
| **bug-validator.md** | Bug validation | `/bug-validate`, Rule 32 |
| **test-runner.md** | Test execution | Test-related runs; consult PhoenixExpert |
| **report-generator.md** | Rule 0.6 reports | After tasks; `/report` |
| **database-query.md** | PostgreSQL (Dev/Test/Prod) | DB questions; `database_workflow.mdc` |
| **production-data-reader.md** | Production DB (read-only) | Rule PDR.0; PostgreSQLProd MCP |
| **git-sync.md** | Phoenix GitLab sync | `/sync`, `!sync`, `git_sync_workflow.mdc` |
| **shell.md** | CLI / terminal delegation | Parent Task `shell`; hooks; optional `git_sync` for multi-repo |
| **environment-access.md** | Dev / Dev2 access | Rule 10; browser/MCP |
| **postman-collection.md** | Postman collections | Postman generation flows |
| **test-case-generator.md** | Test cases (Rule 35) | `/test-case-generate`; after cross-dependency-finder |
| **cross-dependency-finder.md** | Cross-dependencies (35, 35a) | Before test cases; Jira + code + shallow Confluence; **no** local merge/git; **`docs/CROSS_DEPENDENCY_WORK_PATTERN.md`** |
| **energo-ts-test.md** | Playwright test authoring | Rule 0.8.1; `EnergoTS/tests/` only |
| **energo-ts-run.md** | Playwright test run | Rule 36; `cursor` branch only |
| **jira-bug.md** | Jira bug text (Experiments) | Rule JIRA.0; `/jira-bug` |
| **hands-off.md** | HandsOff orchestrator | Rule 37; `/HandsOff`, `!HandsOff` |
| **playwright-test-validator.md** | Spec vs test cases | HandsOff step 4.5 |

**Registry:** `.cursor/agents/README.md` · **Commands:** `.cursor/commands/*.md` · **Rules index:** `.cursor/rules/main/phoenix.mdc`

## 2. Rules and skills

| Kind | Path |
|------|------|
| Rules | `.cursor/rules/**/*.mdc` |
| Skills | `.cursor/skills/**/SKILL.md` |
| Slash commands | `.cursor/commands/*.md` |

## 3. Outputs (always under `Cursor-Project/`)

| Artifact | Path |
|----------|------|
| Reports (Rule 0.6) | **`Cursor-Project/reports/README.md`** — each area uses **`YYYY/<english-month>/<DD>/`** before filenames |
| Test cases | **`Cursor-Project/test_cases/Objects/`**, **`Cursor-Project/test_cases/Flows/`** |
| Playwright (automation branch) | **`Cursor-Project/EnergoTS/tests/cursor/`** |
| Templates | **`Cursor-Project/config/template/`** |
| Postman exports | **`Cursor-Project/postman/`** |

Do **not** use bare `reports/...` at repo root without `Cursor-Project/` unless the doc explicitly means a different repo layout.

## 4. Removed Python package

**`Cursor-Project/agents/`** (Main, Support, Core, Adapters, Services, Utils) — removed.  
Do **not** run `from agents import ...` in this tree for current workflows.

- Historical documentation that still shows Python imports: see **`HISTORICAL_PYTHON_AGENTS_PACKAGE.md`**.
- Legacy example scripts: **`Cursor-Project/examples/README.md`**.

## 5. Related docs

- **`CURSOR_SUBAGENTS.md`** — Subagents overview  
- **`COMMANDS_REFERENCE.md`** — Commands and behaviors  
- **`AGENTS_COMPARISON_AND_ALIGNMENT.md`** — Cursor-first model  
