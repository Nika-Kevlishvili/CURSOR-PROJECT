# Rules canonical index (QA AI Toolkit)

| Rule / topic | Canonical procedure |
|---|---|
| 0.x core | `.cursor/rules/main/core_rules.mdc` |
| CONF.0 / CONF.1 | `.cursor/rules/main/clarification_and_confidence.mdc`, `.cursor/rules/scoring/confidence_scoring_matrix.mdc` |
| QA.0 Senior QA | `.cursor/rules/main/senior_qa_product_quality.mdc`, `.cursor/skills/senior-qa-analysis/SKILL.md` |
| Evidence-only | `.cursor/rules/main/evidence_only_project_answers.mdc` |
| Safety | `.cursor/rules/safety/safety_rules.mdc` |
| 32 Bug validation | `.cursor/skills/bug-validation/SKILL.md`, `.cursor/agents/bug-validator.md` |
| 35 Test cases | `.cursor/skills/cross-dependency-finder/SKILL.md` → `test-case-generator` → `test-case-quality-validator` |
| DB.0a | `.cursor/skills/database/SKILL.md`, `.cursor/rules/integrations/database_workflow.mdc` |
| Jira REST | `.cursor/rules/integrations/jira_rest_fallback.mdc`, `config/scripts/get-jira-issue-rest.ps1` |
| Confluence REST | `.cursor/rules/integrations/confluence_rest_fallback.mdc`, `config/scripts/get-confluence-page-rest.ps1` |
| Env resolve | `.cursor/skills/environment-resolver/SKILL.md` + `.cursor/project-config.json` |

Project identity: **`.cursor/project-config.json`**.
