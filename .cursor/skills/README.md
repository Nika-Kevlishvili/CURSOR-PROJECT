# Project Skills

Skills guide the Cursor agent for this repo’s workflows. **Location:** workspace root `.cursor/skills/`.

## Skills

| Skill | Purpose |
|-------|---------|
| **phoenix-agent-workflow** | Rules + subagents + PhoenixExpert; Rule 0.3 (no `agents.*`); reports |
| **phoenix-bug-validation** | Rule 32 in chat (Confluence → codebase → analysis in reply; file only if `/report` or explicit save) |
| **phoenix-file-organization** | Where to put files (`Cursor-Project/`, `.cursor/`, `User story/`) |
| **phoenix-reporting** | On-demand / workflow-mandated markdown per **`Cursor-Project/reports/README.md`** (Rule 0.6) |
| **phoenix-commands** | Which slash command / workflow to use |
| **phoenix-database** | PostgreSQL MCP; environments; connect first |
| **production-data-reader** | Rule PDR.0; PostgreSQLProd MCP readonly |
| **environment-resolver** | Resolve dev/dev2/test/preprod/prod/experiments; ask when ambiguous |
| **cross-dependency-finder** | Rule 35 / 35a; output for test-case-generator |
| **test-case-generator** | Rule 35; save to `test_cases/Backend/` (+ `Frontend/` when scope includes UI) |
| **test-case-quality-validator** | 10-axis rubric; ≥80/100; HandsOff **Step 3.5** / Rule 35 Step 2.5 |
| **energo-ts-test** | Rule 0.8.1; Playwright authoring under `EnergoTS/tests/` only |
| **playwright-test-validator** | HandsOff Step 4.5; spec vs TC quality gate |
| **phoenix-safety-readonly** | GitLab/Confluence read-only; path tiers |
| **jira-evidence** | Jira ticket completeness, custom fields, attachments, linked Confluence (Rule 42/44) |
| **jira-bug-template** | Experiments board bugs only (Rule JIRA.0) |
| **energo-ts-run** | Rule 36; Playwright from EnergoTS `cursor` branch |

## Source

- **Subagent specs:** `.cursor/agents/*.md`
- **Rules:** `.cursor/rules/**/*.mdc` (index: `main/phoenix.mdc`)
- **Commands:** `.cursor/commands/*.md`
- **Operating model:** `Cursor-Project/docs/CURSOR_OPERATING_MODEL.md`

Rule 0.0: load rules before acting. Skills summarize; rules are authoritative.
