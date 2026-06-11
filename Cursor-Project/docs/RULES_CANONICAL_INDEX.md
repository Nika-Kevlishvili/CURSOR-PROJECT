# Rules canonical index

Single map from **rule / topic** to the **primary procedural** file(s). When `workflow_rules.mdc` and a SKILL overlap, follow the SKILL for step-by-step detail unless a **safety** rule forbids it.

| Rule / topic | Obligations (summary) | Canonical procedural detail |
|--------------|-------------------------|----------------------------|
| 0.x core | Disclosure, PhoenixExpert, reports, tiers | `.cursor/rules/main/core_rules.mdc` |
| CONF.0 | Clarify before substantive work | `.cursor/rules/main/clarification_and_confidence.mdc` |
| CONF.1 | Evidence-based confidence scoring (Three-Zone: GO/CAUTION/STOP) | `.cursor/rules/main/clarification_and_confidence.mdc`, **`.cursor/rules/scoring/confidence_scoring_matrix.mdc`** |
| QA.0 Senior QA mission | Default Senior QA persona; code defects, doc gaps, code↔doc mismatches; dual-track answers | **`.cursor/rules/main/senior_qa_product_quality.mdc`**, **`.cursor/skills/senior-qa-analysis/SKILL.md`**, **`.cursor/agents/senior-qa.md`** |
| 0.10 | Senior QA on all Phoenix scope (points to QA.0) | `.cursor/rules/main/core_rules.mdc` |
| 1 / safety | GitLab & Confluence read-only; tiers | `.cursor/rules/safety/safety_rules.mdc` |
| 8 / agents | Consultation patterns | `.cursor/rules/agents/agent_rules.mdc` |
| 32 Bug validation | BugFinder: env alignment, diagrams, Confluence, mandatory Swagger refresh + OpenAPI, code, 5 verdicts, Slack — no TC/Playwright pipeline | `.cursor/skills/phoenix-bug-validation/SKILL.md`, `.cursor/agents/bug-validator.md` |
| 33 DB Test | PostgreSQLTest MCP | `.cursor/rules/integrations/database_workflow.mdc` |
| DB.0a DB env gate | Ask env in chat before MCP (`phoenix-database` SKILL Step 0, `database-query` agent) | `.cursor/skills/phoenix-database/SKILL.md`, `.cursor/rules/integrations/database_workflow.mdc`, `.cursor/agents/database-query.md` |
| 35 / 35a Test cases | **Gates:** `test_cases_structure.mdc` (TC-ENV, TC-FRONTEND, STANDALONE). **Procedure:** cross-dep → generator → quality | Gates: **`.cursor/rules/workspace/test_cases_structure.mdc`**. Steps: cross-dep / generator / quality **SKILL.md** files |
| 36 EnergoTS run | `energo-ts-run`, `cursor` branch | `.cursor/skills/energo-ts-run/SKILL.md`, `.cursor/agents/energo-ts-run.md` |
| 37 HandsOff | Full orchestration; reporting detail in SKILL | **`.cursor/commands/hands-off.md`**, **`.cursor/skills/hands-off-playwright-report/SKILL.md`**, **`.cursor/rules/workflows/handsoff_playwright_report.mdc`** (exit criteria only) |
| 39 Non-bug Confluence | Linked pages only | `workflow_rules.mdc` (short); Confluence MCP read tools |
| 40 beforeAll ban | Playwright preconditions | `.cursor/rules/workflows/handsoff_playwright_report.mdc` |
| 41 Swagger refresh | Before `.spec.ts` edits | `.cursor/rules/integrations/swagger_refresh_mandatory.mdc` |
| 42 / JIRA.1 Jira read | MCP first, REST fallback; ticket completeness | `.cursor/skills/jira-evidence/SKILL.md`, `.cursor/rules/integrations/jira_rest_fallback.mdc` |
| 43 / CONFLUENCE.1 Confluence read | MCP first, REST fallback | `.cursor/rules/integrations/confluence_rest_fallback.mdc`, `Cursor-Project/config/confluence/README.md` |
| 44 Jira analysis + Confluence | Ticket analysis must include linked Confluence reads | `.cursor/skills/jira-evidence/SKILL.md` § linked Confluence; `workflow_rules.mdc` Rule 44 |
| PHOENIX-SWITCH.0 | Align Phoenix repos | **`.cursor/skills/phoenix-branch-switching/SKILL.md`**, `.cursor/rules/integrations/phoenix_branch_switching.mdc` (summary), `.cursor/commands/switch-phoenix-branches.ps1` |
| JIRA.0 | Experiments board bugs only | `.cursor/rules/integrations/jira_bug_agent.mdc`, `.cursor/skills/jira-bug-template/SKILL.md` |
| PDR.0 | Production DB read | `.cursor/skills/production-data-reader/SKILL.md` |
| Reporting / Slack | Paths and uploads | `Cursor-Project/config/template/Slack_reporting_paths.md`, `.cursor/rules/workflows/playwright_detailed_reporting.mdc` |
| File layout | Stories, reports, test_cases | `.cursor/rules/workspace/file_organization_rules.mdc`, `.cursor/rules/workspace/test_cases_structure.mdc` |

**Rules index (navigation):** `.cursor/rules/main/phoenix.mdc`  
**Agent role → file:** `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`  
**Intended work pattern (architecture):** `Cursor-Project/docs/WORKSPACE_PATTERNS.md`  
**`.cursor` operating model (layers, workflows, cheat sheet):** `Cursor-Project/docs/CURSOR_OPERATING_MODEL.md`
