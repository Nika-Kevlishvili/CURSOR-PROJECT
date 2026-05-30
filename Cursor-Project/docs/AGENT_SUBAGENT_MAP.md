# Agent roles — canonical paths (whole project)

Use this file as the **single map** from “agent role” to **where it lives today**. The old Python tree **`Cursor-Project/agents/`** is **not** in this workspace.

## 1. Subagent specs (Cursor)

**Path pattern:** `<workspace-root>/.cursor/agents/<name>.md`  
(The folder that contains `Cursor-Project` also contains `.cursor`.)

| Subagent file | Role (concept) | Skill / command | Primary trigger |
|---------------|----------------|-----------------|-----------------|
| **phoenix-qa.md** | Phoenix Q&A (PhoenixExpert) | `phoenix-agent-workflow` | `/phoenix`, Phoenix questions (Rule 0.2) |
| **bug-validator.md** | Bug validation | `phoenix-bug-validation` | Rule 32; invoke **`bug-validator`** subagent |
| **environment-resolver.md** | Environment gate (6 envs) | `environment-resolver` | TC-ENV-ASK.0, DB.0a, HandsOff Step 1 |
| **cross-dependency-finder.md** | Cross-dependencies (35, 35a) | `cross-dependency-finder` | Before test cases; Jira + code + shallow Confluence |
| **test-case-generator.md** | Test cases (Rule 35) | `test-case-generator` | After **`cross-dependency-finder`** |
| **test-case-quality-validator.md** | TC quality (10-axis ≥80) | `test-case-quality-validator` | Rule 35 Step 2.5; HandsOff Step 3.5 |
| **hands-off.md** | HandsOff orchestrator | `commands/hands-off.md` | Rule 37; `/HandsOff`, `!HandsOff` |
| **energo-ts-test.md** | Playwright test authoring | `energo-ts-test` | Rule 0.8.1; `EnergoTS/tests/` only |
| **playwright-test-validator.md** | Spec vs test cases | `playwright-test-validator` | HandsOff Step 4.5 |
| **energo-ts-run.md** | Playwright test run | `energo-ts-run` | Rule 36; `cursor` branch only |
| **test-runner.md** | Test execution | — (consult PhoenixExpert) | Test-related runs |
| **database-query.md** | PostgreSQL (Dev/Test/Prod) | `phoenix-database` | DB questions; `database_workflow.mdc` |
| **production-data-reader.md** | Production DB (read-only) | `production-data-reader` | Rule PDR.0; PostgreSQLProd MCP |
| **jira-bug.md** | Jira bug text (Experiments) | `jira-bug-template` | Rule JIRA.0; `/jira-bug` |
| **postman-collection.md** | Postman collections | — (stub; consult PhoenixExpert) | Postman generation flows |
| **environment-access.md** | Dev / Dev2 access | — (stub agent) | Rule 10; browser/MCP |
| **report-generator.md** | Persisted reports (Rule 0.6) | `phoenix-reporting` | HandsOff `{JIRA_KEY}.md`; **`/report`**; **`/feedback`** |
| **shell.md** | CLI / terminal delegation | — | Parent Task `shell`; hooks |

**Git / sync (no dedicated subagent):** Phoenix branch alignment → **`.cursor/commands/switch-phoenix-branches.ps1`** (+ `.md`). Workspace git helpers → **`sync-workspace-repo`**, **`sync-cursor-with-staging`**, **`update-main-from-experiments`** under `.cursor/commands/`. Historical **`git-sync.md`** agent was removed.

**Registry:** `.cursor/agents/README.md` · **Commands:** `.cursor/commands/*.md` · **Rules index:** `.cursor/rules/main/phoenix.mdc` · **Intent router:** `phoenix-commands` skill

## 2. Rules and skills

| Kind | Path |
|------|------|
| Rules | `.cursor/rules/**/*.mdc` |
| Skills | `.cursor/skills/**/SKILL.md` |
| Slash commands | `.cursor/commands/*.md` + `.ps1` |

## 3. Outputs (always under `Cursor-Project/`)

| Artifact | Path |
|----------|------|
| Reports (Rule 0.6) | **`Cursor-Project/reports/README.md`** — each area uses **`YYYY/<english-month>/<DD>/`** before filenames |
| Test cases (canonical) | **`Cursor-Project/test_cases/Backend/<Topic>.md`** (always when TCs generated); **`Cursor-Project/test_cases/Frontend/<Topic>.md`** only if **TC-FRONTEND-ASK.0** = Yes |
| Playwright (automation branch) | **`Cursor-Project/EnergoTS/tests/cursor/`** |
| Templates | **`Cursor-Project/config/template/`** |
| Postman exports | **`Cursor-Project/postman/`** |

Legacy layout **`test_cases/Objects/`**, **`Flows/`** — deprecated; see **`test_cases/README.md`**. Do **not** use bare `reports/...` at repo root without `Cursor-Project/` unless the doc explicitly means a different repo layout.

## 4. Removed Python package

**`Cursor-Project/agents/`** (Main, Support, Core, Adapters, Services, Utils) — removed.  
Do **not** run `from agents import ...` in this tree for current workflows.

- Historical documentation that still shows Python imports: see **`HISTORICAL_PYTHON_AGENTS_PACKAGE.md`** (if present) or **`docs/_archive/`**.
- Legacy example scripts: **`Cursor-Project/examples/README.md`**.

## 5. Related docs

- **`CURSOR_OPERATING_MODEL.md`** — Layer model, cheat sheet, Phase 1–3 status  
- **`CURSOR_SUBAGENTS.md`** — Subagents overview  
- **`COMMANDS_REFERENCE.md`** — Commands and behaviors  
- **`AGENTS_COMPARISON_AND_ALIGNMENT.md`** — Cursor-first model  
