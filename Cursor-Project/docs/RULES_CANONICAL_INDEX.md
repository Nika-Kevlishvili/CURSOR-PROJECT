# Rules canonical index

Single map from **rule / topic** to the **primary procedural** file(s). When `workflow_rules.md` and a SKILL overlap, follow the SKILL for step-by-step detail unless a **safety** rule forbids it.

| Rule / topic | Obligations (summary) | Canonical procedural detail |
|--------------|-------------------------|----------------------------|
| 0.x core | Disclosure, PhoenixExpert, reports, tiers | `.claude/rules/main/core_rules.md` |
| CONF.0 | Clarify before substantive work | `.claude/rules/main/clarification_and_confidence.md` |
| CONF.1 | Evidence-based confidence scoring (Three-Zone: GO/CAUTION/STOP) | `.claude/rules/main/clarification_and_confidence.md`, **`.claude/rules/scoring/confidence_scoring_matrix.md`** |
| QA.0 Senior QA mission | Default Senior QA persona; code defects, doc gaps, code↔doc mismatches; dual-track answers | **`.claude/rules/main/senior_qa_product_quality.md`**, **`.claude/skills/senior-qa-analysis/SKILL.md`**, **`.claude/agents/senior-qa.md`** |
| 0.10 | Senior QA on all Phoenix scope (points to QA.0) | `.claude/rules/main/core_rules.md` |
| 1 / safety | GitLab & Confluence read-only; tiers | `.claude/rules/safety/safety_rules.md` |
| 8 / agents | Consultation patterns | `.claude/rules/agents/agent_rules.md` |
| 32 Bug validation | BugFinder: env alignment, diagrams, Confluence, mandatory Swagger refresh + OpenAPI, code, 5 verdicts, Slack — no TC/Playwright pipeline | `.claude/skills/phoenix-bug-validation/SKILL.md`, `.claude/agents/bug-validator.md` |
| 33 DB Test | `psql-phoenix.py --environment test` | `.claude/rules/integrations/database_workflow.md` |
| DB.0a DB env gate | Ask env in chat before `psql-phoenix.py` (`phoenix-database` SKILL Step 0, `database-query` agent). No PostgreSQL MCP | `.claude/skills/phoenix-database/SKILL.md`, `.claude/rules/integrations/database_workflow.md`, `.claude/agents/database-query.md` |
| 35 / 35a Test cases | **Gates:** `test_cases_structure.md` (TC-ENV, TC-FRONTEND, STANDALONE). **Procedure:** cross-dep → generator → quality | Gates: **`.claude/rules/workspace/test_cases_structure.md`**. Steps: cross-dep / generator / quality **SKILL.md** files |
| 36 EnergoTS run | `energo-ts-run`; stay on `cursor`, `staging`, or `cursorMerged` | `.claude/skills/energo-ts-run/SKILL.md`, `.claude/agents/energo-ts-run.md` |
| Listing performance | Ask object + environment; Swagger and list screen; one Playwright case per UI filter; run with workers=1; hand off payloads at or above 2 s | `.claude/skills/performance-test/SKILL.md`, `.claude/agents/performance-test.md` |
| 45 Frontend Playwright | `/playwright-frontend` on EnergoTS **`cursorMerged` only** (no checkout); one validator pass; then `--project=frontend` | `.claude/commands/playwright-frontend.md`, `.claude/procedures/playwright-frontend/procedure.md`, `.claude/procedures/playwright-frontend-validator/procedure.md` |
| 39 Non-bug Confluence | Linked pages only | `workflow_rules.md` (short); Confluence REST GET (`get-confluence-page-rest.ps1`) |
| 40 beforeAll ban | Playwright preconditions | **`.claude/rules/workflows/playwright_test_constraints.md`** |
| 41 Swagger refresh | Before `.spec.ts` edits | `.claude/rules/integrations/swagger_refresh_mandatory.md` |
| 42 / JIRA.1 Jira read | Official ACLI only; no Jira MCP; ticket completeness | `.claude/skills/jira-evidence/SKILL.md`, `.claude/rules/integrations/atlassian_zero_cost.md` |
| 43 / CONFLUENCE.1 Confluence read | REST GET scripts only; no Confluence MCP | `.claude/rules/integrations/atlassian_zero_cost.md`, `.claude/rules/integrations/confluence_rest_fallback.md`, `Cursor-Project/config/confluence/README.md` |
| LINK.0–LINK.1 Atlassian user links | Confluence = asterbit wiki slug; Jira = oppa-support browse | `.claude/rules/integrations/atlassian_link_format.md`, `Cursor-Project/reports/README.md` |
| 44 Jira analysis + Confluence | Ticket analysis must include linked Confluence reads | `.claude/skills/jira-evidence/SKILL.md` § linked Confluence; `workflow_rules.md` Rule 44 |
| PHOENIX-SWITCH.0 | Align Phoenix repos | **`.claude/skills/phoenix-branch-switching/SKILL.md`**, `.claude/rules/integrations/phoenix_branch_switching.md` (summary), `.claude/commands/switch-phoenix-branches.ps1` |
| JIRA.0 | Experiments board bugs only | `.claude/rules/integrations/jira_bug_agent.md`, `.claude/skills/jira-bug-template/SKILL.md` |
| PHOENIX-BUG.0 | Phoenix Phase 2 Internal/External Bug reporting; Step 3.5 pre-create validation; hook (Internal Bug + Bug) + `.active-bugreview` sidecar; mandatory approval gate | `.claude/rules/integrations/phoenix_bug_reporter.md`, `.claude/skills/phoenix-bug-reporter/SKILL.md`, `.claude/agents/phoenix-bug-reporter.md` |
| PDR.0 | Production DB read | `.claude/skills/production-data-reader/SKILL.md` |
| Reporting / Slack | Paths and uploads | `Cursor-Project/config/template/Slack_reporting_paths.md`, `.claude/rules/workflows/playwright_detailed_reporting.md` |
| File layout | Stories, reports, test_cases | `.claude/rules/workspace/file_organization_rules.md`, `.claude/rules/workspace/test_cases_structure.md` |

**Rules index (navigation):** `.claude/rules/main/phoenix.md`  
**Agent role → file:** `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`  
**Intended work pattern (architecture):** `Cursor-Project/docs/WORKSPACE_PATTERNS.md`  
**`.cursor` operating model (layers, workflows, cheat sheet):** `Cursor-Project/docs/CURSOR_OPERATING_MODEL.md`
