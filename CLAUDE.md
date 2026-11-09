# Phoenix QA workspace

QA workspace for the Phoenix platform. `Cursor-Project/Phoenix/*` holds the product repositories (git submodules, read-only), `Cursor-Project/EnergoTS` holds the Playwright test suite, and the rest of `Cursor-Project/` holds config, docs, scripts, reports and test cases. The Claude Code configuration lives in `.claude/` at this workspace root.

## How this workspace is configured

| Area | Location | Notes |
|------|----------|-------|
| Rules | `.claude/rules/**/*.md` | Files without `paths:` frontmatter load in every session. Files with `paths:` load when a matching file is read. Index: `.claude/rules/main/phoenix.md` |
| Subagents | `.claude/agents/*.md` | Delegate with the Agent tool (`subagent_type: <name>`). Map: `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md` |
| Skills | `.claude/skills/<name>/SKILL.md` | Invoke with the Skill tool or `/<name>`. Read the SKILL before gated work (Rule 0.0) |
| Commands | `.claude/commands/*.md` | Slash commands. The `.ps1` scripts beside them run with `pwsh -NoProfile -File` |
| Procedures | `.claude/procedures/` | Role + procedure files for playwright-frontend and its validator |
| Hooks | `.claude/settings.json` + `.claude/hooks/` | `claude-hook.ps1` dispatches to the guard scripts. See `.claude/README.md` |
| Rule → canon map | `Cursor-Project/docs/RULES_CANONICAL_INDEX.md` | Which SKILL or agent file governs each rule number. In older docs, a path written as `.cursor/...` or `.mdc` means `.claude/...` and `.md` |

## Hard limits (enforced by hooks)

- Never edit anything under `Cursor-Project/Phoenix/` (Rule 0.8). Switch Phoenix branches only with `.claude/commands/switch-phoenix-branches.ps1`.
- `Cursor-Project/EnergoTS` stays on `cursor`, `staging` or `cursorMerged` (Rule ENERGOTS.0).
- Jira: official `acli jira workitem view` and `search` only, search with `--jql` and `--limit` of 50 or less. No Jira or Confluence MCP, no Rovo or Teamwork Graph commands (ATLASSIAN.0).
- Confluence: read-only through `Cursor-Project/config/confluence/get-confluence-page-rest.ps1` and `search-confluence-cql-rest.ps1`.
- PostgreSQL: `python3 Cursor-Project/scripts/psql-phoenix.py` only, never the bare client or a PostgreSQL MCP. Ask the environment first (DB.0a). `prod`, `es`, `slr` are read-only.
- Elasticsearch: `python3 Cursor-Project/scripts/elastic-phoenix.py` only, read-only (ES.0).
- Jira bug creation needs an APPROVED BugReview file (PHOENIX-BUG.0). Experiments-board bugs never go to Phoenix delivery (JIRA.0).

## Asking the user

When a rule says to ask (environment, frontend yes/no, approval), use the `AskUserQuestion` tool and wait for the answer (Rule CONF.0).

## Always-loaded rules

- `.claude/rules/agents/agent_rules.md`
- `.claude/rules/integrations/atlassian_zero_cost.md` — Jira via official ACLI only. Confluence via existing REST GET scripts only. No Atlassian MCP. No Rovo credits
- `.claude/rules/main/clarification_and_confidence.md` — Require clarifying questions when task scope, environment, or success criteria are ambiguous—do not guess or improvise defaults
- `.claude/rules/main/core_rules.md`
- `.claude/rules/main/evidence_only_project_answers.md` — Evidence-only answers for project-related questions: require grounding in code, Confluence REST GET (atlassian_zero_cost.md), and Swagger/OpenAPI; ask 1–2 clarifying questions instead of improvising when evidence is missing
- `.claude/rules/main/senior_qa_product_quality.md` — Senior QA Tester mission — product quality first; hunt code defects, doc gaps, and code↔doc mismatches; never silently collapse conflicts
- `.claude/rules/safety/safety_rules.md`
- `.claude/rules/workflows/workflow_rules.md` — Workflow obligations (Rules 32–45): summary + exit criteria

## Scoped rules

These load automatically when a matching file is read. Read the file yourself when its topic applies and it has not loaded yet.

- `.claude/rules/integrations/atlassian_link_format.md` — Canonical Atlassian URL format for Confluence and Jira in reports, test cases, and chat deliverables
- `.claude/rules/integrations/confluence_rest_fallback.md` — Confluence reads use Cloud REST GET scripts only. Confluence MCP is forbidden. See atlassian_zero_cost.md
- `.claude/rules/integrations/database_workflow.md` — PostgreSQL via official psql (psql-phoenix.py)
- `.claude/rules/integrations/elasticsearch_workflow.md` — Elasticsearch log search via the official Elastic CLI (elastic-phoenix.py). Read-only. Rule ES.0
- `.claude/rules/integrations/energots_branch_lock.md` — EnergoTS (Cursor-Project/EnergoTS) git branch lock (Rule ENERGOTS.0): use the `cursor`, `staging`, or `cursorMerged` branch
- `.claude/rules/integrations/jira_bug_agent.md` — Jira bug agent — Experiments board only; never Phoenix delivery bugs. Rule JIRA.0
- `.claude/rules/integrations/jira_rest_fallback.md` — Jira reads use official ACLI only. Jira MCP and hand-written issue GET are not the path. See atlassian_zero_cost.md
- `.claude/rules/integrations/phoenix_branch_switching.md` — Phoenix branch switching (Rule PHOENIX-SWITCH.0): align Cursor-Project/Phoenix/* repos to origin/<branch> before env-sensitive code reads
- `.claude/rules/integrations/phoenix_bug_reporter.md` — Phoenix Bug Reporter — Phoenix Phase 2 Internal and External Bug reporting with mandatory approval gate. Rule PHOENIX-BUG.0
- `.claude/rules/integrations/production_data_reader.md` — Prod DB questions → ProductionDataReader role, psql-phoenix.py --environment prod, read-only. Rule PDR.0
- `.claude/rules/integrations/swagger_refresh_mandatory.md` — MANDATORY: Always refresh Swagger/OpenAPI specs BEFORE generating or editing any Playwright .spec.ts file
- `.claude/rules/main/phoenix.md` — Navigation index for .claude/rules — thematic layout and quick reference. Load when editing rules or onboarding
- `.claude/rules/scoring/confidence_scoring_matrix.md` — Evidence-based confidence scoring matrix for all workflows
- `.claude/rules/workflows/playwright_detailed_reporting.md` — playwright-report-detailed.md (JSON→MD) next to EnergoTS JSON: scoped Slack path 2 generate + upload with smart report to Tester + #ai-report
- `.claude/rules/workflows/playwright_test_constraints.md` — Rule 40: Playwright test constraints — no beforeAll for preconditions; use fixtures and test.step
- `.claude/rules/workflows/regression_validator_example_first.md` — Regression validator must start from the ticket example scenario
- `.claude/rules/workspace/file_organization_rules.md` — Mandatory directory layout for new workspace files (reports, test_cases, User story, agents, config)
- `.claude/rules/workspace/test_cases_structure.md` — Mandatory two-folder test_cases layout (Backend/Frontend), TC numbering, README updates

## Registry

- **Subagents:** `bug-validator`, `cross-dependency-finder`, `database-query`, `energo-ts-run`, `energo-ts-test`, `environment-access`, `environment-resolver`, `jira-bug`, `performance-test`, `phoenix-bug-reporter`, `phoenix-qa`, `playwright-test-validator`, `postman-collection`, `production-data-reader`, `regression-validator`, `report-generator`, `senior-qa`, `shell`, `test-case-generator`, `test-case-quality-validator`, `test-runner`
- **Skills:** `cross-dependency-finder`, `energo-ts-run`, `energo-ts-test`, `environment-resolver`, `flaky-test-fixer`, `jira-bug-template`, `jira-evidence`, `performance-test`, `phoenix-agent-workflow`, `phoenix-branch-switching`, `phoenix-bug-reporter`, `phoenix-bug-validation`, `phoenix-commands`, `phoenix-database`, `phoenix-elasticsearch`, `phoenix-file-organization`, `phoenix-reporting`, `phoenix-safety-readonly`, `playwright-cli`, `playwright-test-validator`, `pom`, `production-data-reader`, `regression-validator`, `senior-qa-analysis`, `test-case-generator`, `test-case-quality-validator`, `visual-regression-analyzer`
- **Commands:** `/playwright-frontend`, `/playwright-validate`, `/regression-validate`, `/send-playwright-results-slack`, `/switch-phoenix-branches`, `/sync-cursor-with-staging`, `/sync-workspace-repo`, `/test-case-quality`, `/update-main-from-experiments`, `/update-swagger-specs`
