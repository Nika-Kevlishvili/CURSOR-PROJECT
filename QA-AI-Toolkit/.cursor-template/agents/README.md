# Agents

Subagent specs for QA AI Toolkit. Each file is a Cursor chat role.

| Agent | Role |
|----|---|
| product-qa | ProductExpert — product behavior Q&A |
| senior-qa | Senior QA — Findings / gaps |
| bug-validator | Bug validation (Rule 32) |
| cross-dependency-finder | Dependencies before TCs |
| test-case-generator | Test case markdown |
| test-case-quality-validator | TC quality gate |
| environment-resolver | Env from project-config |
| database-query | PostgreSQL MCP after env ask |
| jira-bug | Bug draft from template |
| report-generator | `/report` / `/feedback` only |
| shell | Delegated CLI |

Project config: `.cursor/project-config.json`  
Canon: `docs/RULES_CANONICAL_INDEX.md`
