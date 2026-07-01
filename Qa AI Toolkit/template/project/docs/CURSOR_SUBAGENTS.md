# Cursor Subagents in This Project

This project defines **Cursor Subagents** in `.cursor/agents/` (workspace root). Each subagent maps to an existing project agent or workflow so the main Cursor agent can delegate Phoenix-related tasks correctly.

**Canonical path map (subagent file → role → outputs):** [`AGENT_SUBAGENT_MAP.md`](AGENT_SUBAGENT_MAP.md).

---

## What Are Subagents?

Subagents are specialized AI assistants the main Cursor agent can delegate to. They run in their own context, return a result to the parent, and can be used for parallel or isolated work. See [Cursor Docs – Subagents](https://cursor.com/docs/context/subagents).

---

## Subagents in This Project

| Subagent | File | Skill | Purpose |
|----------|------|-------|---------|
| **Phoenix Q&A** | `phoenix-qa.md` | `phoenix-agent-workflow` | Confluence + codebase; **Senior QA Findings** on mismatch. READ-ONLY. |
| **Senior QA** | `senior-qa.md` | `senior-qa-analysis` | Rule QA.0: defects, doc gaps, code↔doc mismatches. READ-ONLY. |
| **Bug Validator** | `bug-validator.md` | `phoenix-bug-validation` | Rule 32 + **`### Quality Findings (Senior QA)`** section; Slack **`bug-validation`**. |
| **Environment Resolver** | `environment-resolver.md` | `environment-resolver` | Resolve Dev/Dev2/Test/PreProd/Prod/Experiments before env-sensitive work (TC-ENV, DB.0a). |
| **Cross-Dependency Finder** | `cross-dependency-finder.md` | `cross-dependency-finder` | Rule 35a: Jira + codebase + deep Confluence exploration; **no** local merge/git. |
| **Test Case Generator** | `test-case-generator.md` | `test-case-generator` | Rule 35: after cross-dep; **`test_cases/Backend/`** always; **`Frontend/`** if TC-FRONTEND-ASK.0 = Yes. |
| **Test Case Quality Validator** | `test-case-quality-validator.md` | `test-case-quality-validator` | 10-axis rubric, ≥80/100, max 3 rewrites (Rule 35 Step 2.5). |
| **HandsOff** | `hands-off.md` | `commands/hands-off.md` | Rule 37 full pipeline: TC → Playwright → reports → Slack. |
| **EnergoTS Test Author** | `energo-ts-test.md` | `energo-ts-test` | Rule 0.8.1: write only under `EnergoTS/tests/`. |
| **Playwright Test Validator** | `playwright-test-validator.md` | `playwright-test-validator` | HandsOff Step 4.5: spec vs test cases before run. |
| **EnergoTS Run** | `energo-ts-run.md` | `energo-ts-run` | Rule 36: run Playwright on **`cursor`** branch only. |
| **Test Runner** | `test-runner.md` | — | Run tests; consult PhoenixExpert first. |
| **Database Query** | `database-query.md` | `phoenix-database` | PostgreSQL MCP; correct env; connect first. |
| **Production Data Reader** | `production-data-reader.md` | `production-data-reader` | Rule PDR.0: production DB read-only traceability. |
| **Jira Bug** | `jira-bug.md` | `jira-bug-template` | Rule JIRA.0: Experiments board only. |
| **Postman Collection** | `postman-collection.md` | — (stub) | Generate Postman collections; consult PhoenixExpert. |
| **Environment Access** | `environment-access.md` | — (stub) | Dev/Dev2 portal access (Rule 10). |
| **Report Generator** | `report-generator.md` | `phoenix-reporting` | Rule 0.6: HandsOff, **`/report`**, **`/feedback`**. |
| **Shell / CLI** | `shell.md` | — | Bash/PowerShell; respects hooks. |

**Git / Phoenix sync (no subagent):** use **`.cursor/commands/switch-phoenix-branches.ps1`** and workspace sync commands (`sync-workspace-repo`, `sync-cursor-with-staging`). Historical **`git-sync.md`** subagent removed.

---

## Relation to Project Agents

- **PhoenixExpert** → **`phoenix-qa`**
- **BugFinderAgent** → **`bug-validator`** (Rule 32; **`phoenix-bug-validation`** skill)
- **TestCaseGeneratorAgent** → **`test-case-generator`** + **`cross-dependency-finder`** first
- **Database workflow** (Rule DB.0–DB.5) → **`database-query`** + **`phoenix-database`** skill
- **ProductionDataReaderAgent** → **`production-data-reader`**
- **EnvironmentAccessAgent** → **`environment-access`**
- **PostmanCollectionGenerator** → **`postman-collection`**
- **EnergoTSTestAgent** → **`energo-ts-test`** (sole writer for `EnergoTS/tests/`)

**This workspace:** The **`Cursor-Project/agents/`** Python package is **not** present. Roles are implemented **in Cursor chat** via **`.cursor/agents/*.md`**, **`.cursor/rules/**/*.mdc`**, **skills**, and **MCP**. **Rule 0.3:** no `from agents ...` imports in Cursor chat.

---

## Where Subagents Live

- **Path:** Workspace root `.cursor/agents/` (same level as `.cursor/commands/`, `.cursor/hooks/`, `.cursor/skills/`).
- **Format:** One markdown file per subagent with YAML frontmatter (`name`, `description`) and instructions in the body.
- **Discovery:** Cursor discovers them automatically; they appear in the agent’s available tools.

---

## Rules and Skills

- **Rules:** `.cursor/rules/**/*.mdc` — obligations and gates (WHAT).
- **Skills:** `.cursor/skills/**/SKILL.md` — canonical procedure (HOW).
- **Index:** [`RULES_CANONICAL_INDEX.md`](RULES_CANONICAL_INDEX.md) · [`CURSOR_OPERATING_MODEL.md`](CURSOR_OPERATING_MODEL.md)
