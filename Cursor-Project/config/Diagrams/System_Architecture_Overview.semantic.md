# System Architecture Overview

**Domain:** General
**Source:** System_Architecture_Overview.drawio
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 0 |
| Process Steps | 54 |
| Save Operations | 0 |
| Error States | 0 |

## Process Steps

- energo-ts-run
- Prod data read
- production-data-reader
- Env access
- jira-bug<br>(Experiments only)
- Jira<br>MCP primary → REST fallback (Rule 42)<br>Attachments: download-jira-attachments.ps1
- cross-dependency-finder
- environment-access
- "Agents involved" footer (Rule 0.1)<br>Every response MUST end with agent disclosure
- PhoenixExpert consultation (Rule 0.4 / 8)<br>ALL agents MUST consult before Phoenix tasks
- test-case-generator
- /report &middot; /feedback
- Skills<br><font style='font-size:10px'>.cursor/skills/*/SKILL.md</font>
- Generate test cases
- Rules<br><font style='font-size:10px'>.cursor/rules/**/*.mdc</font>
- playwright-test-validator
- Swagger / OpenAPI<br>update-swagger-specs.ps1<br>MANDATORY before any .spec.ts
- database-query
- Confluence<br>MCP primary → REST fallback (Rule 43)<br>Broad search ONLY in bug-validator
- Phoenix Q&A
- Run Playwright tests
- Phoenix (READ-ONLY — never edit)<br>Cursor-Project/Phoenix/**<br>Env-aligned via switch-phoenix-branches.ps1
- <b>Cursor-Project Artifacts</b>
- Postman collection
- <b>System Architecture Overview</b><br>Workspace: Cursor &middot; Phoenix QA Automation Platform
- Test cases<br>test_cases/Backend/&lt;Topic&gt;.md<br>test_cases/Frontend/&lt;Topic&gt;.md
- report-generator
- environment-resolver
- Slack<br>Path 1: #bug-validation (C0AUEEDVCEL)<br>Path 2+3: Tester DM + #ai-report (C0AK96S1D7X)
- <b>Cross-cutting concerns (apply to every interaction)</b>
- Jira bug (Exp)
- Bug validation
- phoenix-qa<br>(PhoenixExpert)
- postman-collection
- energo-ts-test<br>(EnergoTSTestAgent)
- Agents<br><font style='font-size:10px'>.cursor/agents/*.md</font>
- EnergoTS<br>Cursor-Project/EnergoTS/<br><b>tests/cursor/</b> = AI-writable (agent only)
- test-case-quality-validator
- Reports (Rule 0.6)<br>reports/Chat reports/<br>reports/HandsOff reports/<br>reports/Feedback/
- /HandsOff &middot; !HandsOff
- PostgreSQL MCP<br>Gate: env MUST be explicit (DB.0a)<br>Dev | Dev2 | Test | PreProd | Prod | Experiments
- test-runner
- <b>.cursor Orchestration Layer</b>
- Config / Templates<br>config/swagger/* &middot; config/template/*<br>config/Diagrams/* &middot; config/playwright_generation/*
- Hooks<br><font style='font-size:10px'>hooks.json + *.ps1</font>
- Confidence score (Rule CONF.1)<br>0–100% mandatory in every response
- <b>Safety Guards (enforcement)</b>
- <b>User / Chat Intent</b>
- bug-validator<br>(BugFinderAgent)
- hands-off<br>(orchestrator)
- shell
- <b>Integrations (read-only / gated)</b>
- Hooks enforcement (hooks.json + hooks/*.ps1)<br>Block forbidden ops: Phoenix edits, EnergoTS branch switch
- DB query

## Flow Connections

Direct relationships between steps:

- Phoenix Q&A [Rule 0.2] --> phoenix-qa<br>(PhoenixExpert)
- Bug validation [Rule 32] --> bug-validator<br>(BugFinderAgent)
- Generate test cases [Rule 35] --> cross-dependency-finder
- /HandsOff &middot; !HandsOff [Rule 37] --> hands-off<br>(orchestrator)
- Run Playwright tests [Rule 36] --> energo-ts-run
- DB query [DB.0a] --> database-query
- Prod data read [PDR.0] --> production-data-reader
- cross-dependency-finder --> test-case-generator
- test-case-generator --> test-case-quality-validator
- energo-ts-test<br>(EnergoTSTestAgent) --> playwright-test-validator
- test-case-generator [writes TCs] --> Test cases<br>test_cases/Backend/&lt;Topic&gt;.md<br>test_cases/Frontend/&lt;Topic&gt;.md
- energo-ts-test<br>(EnergoTSTestAgent) [writes .spec.ts] --> EnergoTS<br>Cursor-Project/EnergoTS/<br><b>tests/cursor/</b> = AI-writable (agent only)
- phoenix-qa<br>(PhoenixExpert) [reads code] --> Phoenix (READ-ONLY — never edit)<br>Cursor-Project/Phoenix/**<br>Env-aligned via switch-phoenix-branches.ps1
- report-generator [saves report] --> Reports (Rule 0.6)<br>reports/Chat reports/<br>reports/HandsOff reports/<br>reports/Feedback/
- bug-validator<br>(BugFinderAgent) [reads] --> Confluence<br>MCP primary → REST fallback (Rule 43)<br>Broad search ONLY in bug-validator
- database-query [queries] --> PostgreSQL MCP<br>Gate: env MUST be explicit (DB.0a)<br>Dev | Dev2 | Test | PreProd | Prod | Experiments
- production-data-reader [queries (Prod)] --> PostgreSQL MCP<br>Gate: env MUST be explicit (DB.0a)<br>Dev | Dev2 | Test | PreProd | Prod | Experiments
- hands-off<br>(orchestrator) [fetches ticket] --> Jira<br>MCP primary → REST fallback (Rule 42)<br>Attachments: download-jira-attachments.ps1
- hands-off<br>(orchestrator) [uploads reports] --> Slack<br>Path 1: #bug-validation (C0AUEEDVCEL)<br>Path 2+3: Tester DM + #ai-report (C0AK96S1D7X)

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

