# Project Skills

Skills guide the Cursor agent for this repo’s workflows. **Location:** workspace root `.cursor/skills/`.

## Skills

| Skill | Purpose |
|-------|---------|
| **phoenix-agent-workflow** | Rules + subagents + PhoenixExpert; Rule 0.3 (no `agents.*`); reports |
| **phoenix-bug-validation** | Rule 32 in chat (Confluence → codebase → report) |
| **phoenix-file-organization** | Where to put files (`Cursor-Project/`, `.cursor/`, `User story/`) |
| **phoenix-reporting** | Rule 0.6 markdown reports under `Cursor-Project/reports/YYYY-MM-DD/` |
| **phoenix-commands** | Which slash command / workflow to use |
| **phoenix-database** | PostgreSQL MCP; environments; connect first |
| **production-data-reader** | Rule PDR.0; PostgreSQLProd MCP readonly |
| **cross-dependency-finder** | Rule 35 / 35a; output for test-case-generator |
| **test-case-generator** | Rule 35; save to `test_cases/Backend/` and `test_cases/Frontend/` |
| **phoenix-safety-readonly** | GitLab/Confluence read-only; path tiers |
| **jira-bug-template** | Experiments board bugs only (Rule JIRA.0) |
| **energo-ts-run** | Rule 36; Playwright from EnergoTS `cursor` branch |

## Source

- **Subagent specs:** `.cursor/agents/*.md`
- **Rules:** `.cursor/rules/**/*.mdc` (index: `main/phoenix.mdc`)
- **Commands:** `.cursor/commands/*.md`

Rule 0.0: load rules before acting. Skills summarize; rules are authoritative.
