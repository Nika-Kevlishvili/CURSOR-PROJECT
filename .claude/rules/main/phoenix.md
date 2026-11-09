---
description: "Navigation index for .claude/rules — thematic layout and quick reference. Load when editing rules or onboarding."
paths:
  - ".claude/**"
  - "Cursor-Project/docs/RULES_CANONICAL_INDEX.md"
  - "Cursor-Project/docs/CURSOR_OPERATING_MODEL.md"
  - "Cursor-Project/docs/WORKSPACE_PATTERNS.md"
---

# PHOENIX PROJECT RULES - MAIN INDEX

This file is the **main index** for workspace rules under **`.claude/rules/`**. Files live in **thematic subfolders** (not only the rules root).

## Thematic layout

| Folder | Path | Contents |
|--------|------|----------|
| **main** | `.claude/rules/main/` | `core_rules.md` (Rule 0.x), `senior_qa_product_quality.md` (Rule QA.0), `clarification_and_confidence.md` (Rule CONF.0), `phoenix.md` (this index) |
| **safety** | `.claude/rules/safety/` | `safety_rules.md` |
| **agents** | `.claude/rules/agents/` | `agent_rules.md` |
| **workflows** | `.claude/rules/workflows/` | `workflow_rules.md`, `playwright_test_constraints.md`, `playwright_detailed_reporting.md` |
| **workspace** | `.claude/rules/workspace/` | `file_organization_rules.md`, `test_cases_structure.md` |
| **integrations** | `.claude/rules/integrations/` | `database_workflow.md`, `elasticsearch_workflow.md`, `energots_branch_lock.md`, `production_data_reader.md`, `jira_bug_agent.md`, `jira_rest_fallback.md`, `confluence_rest_fallback.md`, `phoenix_branch_switching.md` |
| **scoring** | `.claude/rules/scoring/` | `confidence_scoring_matrix.md` (CONF.1 Three-Zone evidence factors, Prod safety gate, calibration) |

**Additional (optional):** `Cursor-Project/config/cursorrules/autonomous_rules.md` — autonomous-operation notes if present.

## Rules loading

Claude Code loads **all** `.md` files under `.claude/rules/` **recursively** (files with `paths:` frontmatter load only when a matching file is read; files without it are always loaded). **Rule 0.0** (tiered): always honor the always-loaded rules; before **substantive** work, read the **canonical** SKILL/agent for that workflow (`Cursor-Project/docs/RULES_CANONICAL_INDEX.md`); full recursive `.md` audit only for explicit rules audits, **`validate-claude-rules.ps1`**, or broad rule edits — see **`core_rules.md`** Rule 0.0.

## Quick reference (canonical paths)

- **Rule 0.x:** `.claude/rules/main/core_rules.md`
- **Clarification / confidence gate:** `.claude/rules/main/clarification_and_confidence.md` (Rule CONF.0)
- **Senior QA mission:** `.claude/rules/main/senior_qa_product_quality.md` (Rule QA.0–QA.4; default persona)
- **Agents:** `.claude/rules/agents/agent_rules.md`
- **Safety:** `.claude/rules/safety/safety_rules.md`
- **Workflows:** `.claude/rules/workflows/workflow_rules.md`, `.claude/rules/workflows/playwright_test_constraints.md`, `.claude/rules/workflows/playwright_detailed_reporting.md`
- **Files / test cases layout:** `.claude/rules/workspace/file_organization_rules.md`, `.claude/rules/workspace/test_cases_structure.md`
- **DB / Elasticsearch / Git / EnergoTS / Prod / Jira (official ACLI + bug agent) / Confluence REST GET / Phoenix branch switching:** `.claude/rules/integrations/*.md` — **ATLASSIAN.0:** no Jira or Confluence MCP (`atlassian_zero_cost.md`). **DB.0 / DB.0a:** PostgreSQL via official `psql` (`psql-phoenix.py`), not PostgreSQL MCP; ask env before the wrapper (`phoenix-database` SKILL Step 0 + CONF.0). **ES.0 / ES.0a:** Elasticsearch via the official Elastic CLI (`elastic-phoenix.py`), not Elasticsearch MCP. Dev/Dev2 share a cluster by `app_name`; Test 2 ES/SLR share the Test cluster URL with `app_name`+`environment.keyword`; portal URL markers `phoenix-test2/` / `phoenix2-slr-test/` override the bug-validator log target
- **Slack reporting (two paths):** `Cursor-Project/config/template/Slack_reporting_paths.md` — short **Slack text** + **attach** detailed `.md` (`Cursor-Project/config/slack/upload-file-to-slack.ps1`)
- **Workspace patterns (architecture):** `Cursor-Project/docs/WORKSPACE_PATTERNS.md` · **Rule → canon map:** `Cursor-Project/docs/RULES_CANONICAL_INDEX.md`

## ⚠️ CRITICAL REMINDERS

1. **Rule 0.0:** Tiered rules loading — see **`core_rules.md`**; canonical map: **`Cursor-Project/docs/RULES_CANONICAL_INDEX.md`**. **Rule CONF.0:** If scope/environment/target/success criteria are ambiguous → ask before substantive action; do not guess (`clarification_and_confidence.md`).
2. **Rule 0.1:** "Agents involved" footer mandatory
3. **Rule 0.2:** ALL Phoenix questions → PhoenixExpert ONLY (+ Senior QA lens, Rule QA.0)
4. **Rule 0.10 / QA.0:** Default Senior QA — defects, doc gaps, code↔doc mismatches; dual-track answers; **Finding** on conflict
5. **Rule 0.6:** Disk reports **only** when **`/report`**, **`/feedback`**, or explicit user save — default **chat-first**; full text: **`core_rules.md`** Rule 0.6
6. **Rule 0.8:** Phoenix never edited by AI; EnergoTS files are editable on user request (`cursor`, `staging`, or `cursorMerged`); Playwright specs go under `EnergoTS/src/tests/` when `testDir` is `./src/tests` — see `.claude/rules/safety/safety_rules.md` Rule 7
7. **Rule 1:** GitLab and Confluence are READ-ONLY
8. **Rule 0.4 / Rule 8:** PhoenixExpert consultation mandatory before tasks
9. **Rule 11 / 0.3:** External automation may use IntegrationService; in Claude Code chat follow MCP/Jira/GitLab steps — no `agents.*` imports here
10. **Rule 32:** ALL bug validation → ALWAYS use BugFinderAgent
11. **Rule 35/35a:** Test cases → **TC-ENV-ASK.0** (environment) → **TC-FRONTEND-ASK.0** → Phoenix align → cross-dependency-finder → generator; Rule 35a: **Jira + codebase + deep Confluence exploration** — **no** local merge/git (see `test_cases_structure.md`, `workflow_rules.md`)
12. **Rule 42 / JIRA.1:** Jira reads — **official ACLI only** (`acli jira workitem view` / `search`). No Jira MCP — **`.claude/rules/integrations/atlassian_zero_cost.md`**
13. **Rule 43 / CONFLUENCE.1:** Confluence reads — **REST GET scripts only**. No Confluence MCP — **`Cursor-Project/config/confluence/get-confluence-page-rest.ps1`** and **`search-confluence-cql-rest.ps1`**
14. **Rule 44:** Jira ticket **analysis/triage/summary** with linked Confluence URLs → **must** read those pages same pass — **`.claude/rules/main/evidence_only_project_answers.md`** (*Jira ticket analysis — linked Confluence*); **`workflow_rules.md`** Rule 44
15. **Rule 36:** Run API Playwright tests from EnergoTS → energo-ts-run workflow ONLY; stay on `cursorMerged` or `staging` when `testDir` is `./src/tests` (Rule ENERGOTS.0). Do not switch to `cursor` for new-layout runs.
15a. **Rule 45:** Frontend Playwright (`src/frontend`, page objects for a merged UI change) → **playwright-frontend** on EnergoTS **`cursorMerged` only**. Do not checkout. One **playwright-frontend-validator** critique, then execute. Command: `.claude/commands/playwright-frontend.md`.
16. **Rule PHOENIX-SWITCH.0:** Before environment-sensitive Phoenix code reading (Q&A, bug validation, cross-dep, test-case generation), align every `Cursor-Project/Phoenix/*` repo to the matching `origin/<branch>` via `.claude/commands/switch-phoenix-branches.ps1`; local Phoenix edits are discarded; Phoenix files stay READ-ONLY (Rule 0.8 Tier A) — see `.claude/rules/integrations/phoenix_branch_switching.md`
17. **Slack paths:** Bug validation (Rule 32) vs scoped Playwright Slack — **`Cursor-Project/config/template/Slack_reporting_paths.md`** (do not merge; bug-validator workflow untouched)
18. **Rule JIRA.0:** Jira bug agent → Experiments board ONLY; NEVER create bugs in Phoenix delivery
19. **Rule PDR.0:** Production data questions → ALWAYS use ProductionDataReaderAgent
20. **Rule 34:** Claude Code subagent specs live in **`.claude/agents/*.md`** (flat registry). Historical Python **`Cursor-Project/agents/`** package removed — do not recreate without explicit project decision.

---

**Note:** Nested Phoenix repos (e.g. `phoenix-core`, `phoenix-core-lib`) may define **additional** rules files of their own (`.cursor/rules/` or `.claude/rules/`); those apply to their paths — they are not duplicates of this workspace ruleset.
