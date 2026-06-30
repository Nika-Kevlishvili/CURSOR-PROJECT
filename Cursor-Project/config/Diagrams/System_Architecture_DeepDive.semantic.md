# System Architecture Deepdive

**Domain:** General
**Source:** System_Architecture_DeepDive.drawio
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 7 |
| User Actions | 2 |
| Process Steps | 169 |
| Save Operations | 2 |
| Error States | 9 |

## User Actions (Entry Points)

- Triggered by: Jira ticket + /HandsOff or !HandsOff — must NOT skip any step
- User request arrives

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. 3 iterations<br>exhausted?

- **Yes** --> BLOCK WORKFLOW<br>Escalate to user with all<br>failing axes and reasons

### 2. Environment<br>needed?

- **Yes** --> environment-resolver<br>(ask user or extract from ticket)
- **No / resolved** --> Proceed to workflow<br>(see dedicated pages: 4–9)

### 3. beforeAll<br>banned?

- **OK** --> Use test.step('Precondition: …')<br>+ helpers (Rule 40)
- **Violation** --> BLOCKED:<br>Fix code

### 4. Verify: cursor branch<br>(Rule ENERGOTS.0)

- **OK** --> Resolve test file<br>(newly created / Jira key /<br>file path / domain)
- **Fail** --> Wrong branch<br>→ BLOCKED

### 5. All TCs<br>≥ 80?

- **< 80** --> Fail: return failing TCs + detailed fixes<br>to test-case-generator for rewrite
- **≥ 80 ✓** --> TCs saved and validated — workflow complete

### 6. What type of<br>request?

- **Phoenix Q&A** --> phoenix-qa<br>(PhoenixExpert)
- **Bug report** --> bug-validator<br>(BugFinderAgent)
- **Test cases** --> cross-dep → tc-gen<br>→ tc-qual-val
- **/HandsOff** --> hands-off<br>(full orchestrator)
- **Run tests** --> energo-ts-run
- **DB query** --> database-query
- **Prod data** --> prod-data-reader
- **Other** --> Other: jira-bug,<br>postman, report,<br>env-access, shell

### 7. 3. CONF.0: scope clear?

- **No** --> Ask clarifying Qs
- **Yes** --> 4. Route to agent/skill

## Process Steps

- 4b. playwright<br>test-validator (≥80)
- <b>Slack (3 reporting paths)</b>
- database_workflow.mdc<br>Rules DB.0–DB.6
- <b>Mandatory Prerequisites for all HandsOff</b><br><br>• Swagger refresh before .spec.ts (Rule 41)<br>• No beforeAll in preconditions (Rule 40)<br>• EnergoTS cursor branch locked (Rule ENERGOTS.0)<br>• Phoenix alignment checked (PHOENIX-SWITCH.0)<br>• Hooks enforce all gates automatically
- 3b. tc-quality<br>validator (≥80)
- Process box = Cross-cutting concern
- Test
- PROCESS BLOCKED
- PreProd
- prod-data-reader
- BLOCKED:<br>Fix code
- <b>PostgreSQL MCP (Rules DB.0–DB.6)</b>
- Ask clarifying Qs
- tc-quality-validator
- Cloud = External service<br>(Jira, Confluence, Slack)
- DEFAULT: Full answer in chat only<br>No automatic disk files
- bug-validator
- database-query
- Path 2: HandsOff<br>Tester DM + #ai-report (C0AK96S1D7X)
- <b>Confluence (Rules 39, 43, 1a)</b>
- PostgreSQL
- env-resolver
- FORBIDDEN: Phoenix delivery
- hands-off<br>(full orchestrator)
- 6. Execute (hooks enforce guards)
- report-generator
- Experiments
- Wrong branch<br>→ BLOCKED
- Cylinder = Database
- phoenix-bug-validation &middot; phoenix-agent-workflow &middot; phoenix-commands<br>phoenix-database &middot; phoenix-safety-readonly &middot; phoenix-file-organization<br>phoenix-reporting &middot; production-data-reader &middot; test-case-generator<br>cross-dependency-finder &middot; energo-ts-run &middot; jira-bug-template
- Confidence: XX% + Reason<br>(Rule CONF.1 — every response)
- FORBIDDEN: create, update,<br>delete, comment (Rule 1a)
- <b>Color Scheme</b>
- 1. Jira ticket<br>fetch (Rule 42/44)
- 4. Route to agent/skill
- Jira Cloud
- Other: jira-bug,<br>postman, report,<br>env-access, shell
- Full analysis → Chat
- Citations: file path + lines / page title + ID<br>(evidence_only_project_answers.mdc)
- Path 1: Bug validation<br>#bug-validation (C0AUEEDVCEL)
- Step 1b: Check diagrams<br>Ticket attachments → config/Diagrams/<br>(Bundle 4/5/6 .svg fallback)
- hands-off.md &middot; switch-phoenix-branches.md &middot; update-swagger-specs.md<br>git-sync.md &middot; (others as needed)
- CANNOT REPRODUCE
- Attachments<br>download-jira-attachments.ps1
- 2. Swagger refresh<br>update-swagger-specs.ps1<br>(MANDATORY — Rule 41)
- Agents involved: [list]<br>(Rule 0.1 — every response)
- 3. test-case<br>generator
- Intent → Rule → Agent(s) → Key Output<hr>Phoenix Q&A → 0.2, 0.4 → phoenix-qa → Chat answer + citations<br>Bug validation → 32 → bug-validator → 5-verdict analysis + Slack<br>Test cases → 35/35a → cross-dep + tc-gen + tc-qual-val → Backend/ + Frontend/ .md<br>HandsOff → 37 → hands-off orchestrator → {KEY}.md + Playwright + Slack<br>Playwright run → 36 → energo-ts-run → Test results + JSON report<br>DB query → DB.0a → database-query → Query results in chat<br>Prod data → PDR.0 → production-data-reader → Entity analysis in chat<br>Postman → 17.P2 → postman-collection (consults PhoenixExpert) → .json<br>Jira bug → JIRA.0 → jira-bug (Experiments board only) → Jira ticket text<br>Report/Feedback → 0.6 → report-generator → .md under reports/<br>Env access → 10 → environment-access → Browser session
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md
- Prod = read-only (SELECT only)<br>production-data-reader uses PostgreSQLProd
- HandsOff (Rule 37):<br>{JIRA_KEY}.md MANDATORY under HandsOff reports/
- integrations/*.mdc<br>Jira/Conf fallback, branch switch,<br>Swagger, EnergoTS lock, prod reader
- env-access
- update-swagger-specs.ps1<br>MANDATORY before any .spec.ts create/edit
- ALLOWED: Experiments board
- 5. Read canon SKILL before work
- safety_rules.mdc<br>Rules 1, 14, 18–31
- Jira MCP (primary)<br>getJiraIssue + expand=names<br>+ all custom fields
- <b>Safety Guards, Prohibitions & Quality Gates</b>
- Dev2
- Phase 2 exclusion: for Prod/PreProd,<br>exclude Phase 2 / experimental wiki trees
- bug-validator<br>(BugFinderAgent)
- NOT A BUG
- 4. energo-ts-test<br>(Playwright author)
- <b>Swagger / OpenAPI (Rule 41)</b>
- 1. Read playwright instructions<br>project-description → general-rules →<br>test-writing-rules → SKILL.md
- <b>Design Principles</b>
- 2. cross-dep<br>finder (Rule 35a)
- Output:<br>scope, entry_points,<br>upstream, downstream,<br>impact_risk / what_could_break,<br>integration_points, technical_details
- <b>Canonical Sources (single source of truth per workflow)</b>
- REST fallback (Rule 42)<br>Atlassian Cloud REST API v3<br>Same field parity
- test_cases/Backend/&lt;Topic&gt;.md<br>(TC-BE-N only)
- <b>Authoring Path (energo-ts-test / EnergoTSTestAgent)</b>
- Disk: BugValidation_*.md<br>ONLY on /report or explicit save
- English for all disk artifacts (Rule 0.7)<br>Chat may use user's language
- 6. Report<br>{JIRA_KEY}.md
- Gray = Utility agents / supporting workflows
- generate-detailed-report.mjs<br>→ playwright-report-detailed.md
- Slack → #bug-validation<br>(C0AUEEDVCEL)
- Rounded rect = Agent / workflow step
- npx playwright test<br>--reporter=json
- upload-file-to-slack.ps1<br>(file attachments, not pasted text)
- test-case-generator
- INCONCLUSIVE
- <b>Mandatory Response Elements</b>
- Step 3: Swagger refresh (MANDATORY)<br>update-swagger-specs.ps1
- Purple = HandsOff orchestration
- test-runner
- <b>Commands (.cursor/commands/*.md)</b><br>Operational checklists
- switch-phoenix-branches.ps1<br>(align Phoenix repos to env branch)
- Production DB — SELECT-ONLY<br>Never INSERT/UPDATE/DELETE
- energo-ts-test
- phoenix-qa<br>(PhoenixExpert)
- CONFIRMED
- <b>Hooks (.cursor/hooks.json + hooks/*.ps1)</b><br>Automated enforcement on agent events
- 5. energo-ts-run<br>(cursor branch)
- production-data-reader
- <b>Skills (.cursor/skills/*/SKILL.md)</b><br>Step-by-step procedural docs
- Confluence<br>Cloud
- Document = Artifact / file output
- <b>Request Flow Through Layers</b>
- Dev
- environment-resolver<br>(ask user or extract from ticket)
- Hexagon = Orchestration layer<br>(rules, skills, hooks)
- 0. Env resolve<br>+ Phoenix align
- 7. Slack upload<br>Tester + #ai-report
- <b>Rules (.cursor/rules/**/*.mdc)</b><br>Always injected — gates & constraints
- Green = EnergoTS / Playwright
- playwright-report.json
- Rules: .cursor/rules/**/*.mdc (always injected by Cursor)<br>Agents: .cursor/agents/*.md (Task tool subagent specs)<br>Skills: .cursor/skills/*/SKILL.md (step-by-step procedure)<br>Commands: .cursor/commands/*.md (operational checklists)<br>Hooks: .cursor/hooks.json + .cursor/hooks/*.ps1 (enforcement)<br><br>Rule → canon map: Cursor-Project/docs/RULES_CANONICAL_INDEX.md<br>Agent roster: Cursor-Project/docs/AGENT_SUBAGENT_MAP.md<br>Architecture: Cursor-Project/docs/WORKSPACE_PATTERNS.md
- Step 4a: Phoenix code analysis<br>(aligned to env branch — READ-ONLY)<br>Endpoints, services, validators, schedulers
- TCs saved and validated — workflow complete
- Use test.step('Precondition: …')<br>+ helpers (Rule 40)
- 2. alwaysApply rules inject
- <b>Reporting Rules (Rule 0.6)</b>
- playwright-test-validator
- /report → Chat reports/ (on demand)<br>/feedback → Feedback/ (on demand)
- workflow_rules.mdc<br>Rules 32, 35, 36, 37, 39–44
- test_cases/Frontend/&lt;Topic&gt;.md<br>(TC-FE-N only)
- <b>Quality Gates (mandatory checks)</b>
- <b>Shape Legend</b>
- energo-ts-run
- core_rules.mdc<br>Rule 0.x (PhoenixExpert, reports, path tiers)
- hands-off
- GitLab — READ-ONLY<br>No commits, pushes, merges, edits
- postman-collection
- <b>Jira (Rules 42, 44, JIRA.0)</b>
- Blue = Test cases / analysis
- Block Phoenix edits &middot; Enforce EnergoTS cursor branch<br>Prevent forbidden Confluence writes &middot; Swagger refresh check
- <b>Steps 6–7: Report + Slack delivery</b><br><br>1. Smart report: {JIRA_KEY}.md under HandsOff reports/ (YYYY/month/DD/)<br>2. Machine report: playwright-report-detailed.md (if JSON exists)<br>3. Slack short summary (3-block) to #ai-report (C0AK96S1D7X)<br>4. Both .md files uploaded as attachments (not pasted)<br>5. Tester DM (customfield_10095) when available<br>6. upload-file-to-slack.ps1 for file delivery
- shell
- <b>Intent → Rule → Agent Mapping</b>
- config/swagger/<env>/swagger-spec.json
- <b>Agents (.cursor/agents/*.md)</b><br>Task-tool subagent specs — 18 agents total
- phoenix-qa
- Env resolve (PHOENIX-SWITCH.0)<br>switch-phoenix-branches.ps1
- <b>Step 4: Playwright authoring (energo-ts-test)</b><br><br>1. Read playwright instructions pack (project-description, general-rules,<br> test-writing-rules, SKILL.md)<br>2. Swagger refresh (MANDATORY — Rule 41)<br>3. No beforeAll for preconditions (Rule 40)<br>4. Write .spec.ts under EnergoTS/tests/cursor/<br>5. Only EnergoTSTestAgent may write (Rule 0.8.1)
- <b>Jira Bug Scope (Rule JIRA.0)</b>
- 4. playwright-test-validator<br>STRICT 0–100 scoring<br>Pass: ≥80
- Bug validation (Rule 32):<br>Chat only — disk only on /report or explicit save
- Gold / yellow = Phoenix / PhoenixExpert domain
- cross-dep → tc-gen<br>→ tc-qual-val
- Step 5: 5-Verdict Matrix
- <b>Step Details</b>
- Proceed to workflow<br>(see dedicated pages: 4–9)
- <b>TIER C — ALLOWED (default)</b><br>docs/, examples/, config/, postman/, reports/,<br>test_cases/, User story/, .cursor/, workspace root<br>User-requested edits permitted
- 1. User sends message
- Prod
- <b>Execution Path (energo-ts-run — Rule 36)</b>
- Step 2.5: test-case-quality-validator<br>STRICT 0–100 scoring (10 axes)<br>Pass threshold: 80/100
- cross-dependency-finder
- Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence
- <b>Code Modification Tiers (Rule 0.8)</b>
- REST fallback (Rule 43)<br>get-confluence-page-rest.ps1<br>Confluence Cloud REST
- 1. PhoenixExpert is mandatory before any Phoenix task (Rule 0.4/8)<br>2. Environment must be explicit — never inferred (Rule CONF.0 / DB.0a)<br>3. Phoenix code is NEVER edited — read-only via aligned branches (Rule 0.8)<br>4. EnergoTS tests/ writable ONLY by EnergoTSTestAgent (Rule 0.8.1)<br>5. Confluence + GitLab = read-only always (Rule 1)<br>6. Swagger refresh mandatory before .spec.ts files (Rule 41)<br>7. No beforeAll in EnergoTS preconditions (Rule 40)<br>8. Every response: Confidence % + Agents involved (Rules CONF.1 / 0.1)<br>9. Disk reports only on /report, /feedback, HandsOff — NOT by default (Rule 0.6)<br>10. Jira bugs: Experiments board only, never Phoenix delivery (Rule JIRA.0)
- Resolve test file<br>(newly created / Jira key /<br>file path / domain)
- jira-bug
- <b>External System Permissions (Rule 1)</b>
- <b>TIER B — EnergoTS (restricted)</b><br>EnergoTS/** outside tests/ → NO AI writes<br>EnergoTS/tests/** → ONLY EnergoTSTestAgent (Rule 0.8.1)<br>Generic Cursor AI MUST NOT edit even if user asks
- 7. Respond: answer + Confidence + Agents involved
- Confluence MCP (primary)<br>READ-ONLY tools only<br>getConfluencePage, search, CQL
- DRY creation-step rule:<br>## Test data once per topic; each TC<br>references slice + deltas only.
- Slack token missing/expired<br>→ note in chat, skip Slack delivery
- Slack
- Path 3: Scoped Playwright run<br>Tester DM + #ai-report (C0AK96S1D7X)
- <b>Step 5: Test execution (energo-ts-run)</b><br><br>1. EnergoTS must be on cursor branch (Rule ENERGOTS.0)<br>2. npx playwright test --reporter=json<br>3. Produces playwright-report.json<br>4. generate-detailed-report.mjs → playwright-report-detailed.md
- <b>Connector Types</b>
- 3. Write .spec.ts<br>EnergoTS/tests/cursor/<br>(ONLY EnergoTSTestAgent — Rule 0.8.1)
- Red / pink = Bug validation / Safety guards

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- <b>TIER A — ABSOLUTELY FORBIDDEN</b><br>Phoenix (Cursor-Project/Phoenix/**)<br>NEVER create, modify, or delete any file.<br>Hooks enforce. Only read/analyze/search/recommend.
- Confluence — READ-ONLY<br>FORBIDDEN: create, update, delete pages/comments

## Error/Exception States

- Fail: return failing TCs + detailed fixes<br>to test-case-generator for rewrite
- Swagger refresh fails<br>→ continue with cached specs (non-fatal)
- BLOCK WORKFLOW<br>Escalate to user with all<br>failing axes and reasons
- Phoenix alignment fails (exit 2/3)<br>→ PROCESS BLOCKED, ask user
- Step 4b: DB evidence (optional)<br>Same env via PostgreSQL MCP<br>Entity state, audit logs, error logs
- Swagger fail?<br>Continue with cached specs
- Confluence MCP fails → REST fallback (Rule 43)<br>REST fails → note in analysis, continue if partial
- Jira MCP fails → REST fallback (Rule 42)<br>REST fails → PROCESS BLOCKED
- <b>Error / Failure Paths</b>

## Flow Connections

Direct relationships between steps:

- 1. User sends message --> 2. alwaysApply rules inject
- 2. alwaysApply rules inject --> 3. CONF.0: scope clear?
- 3. CONF.0: scope clear? [No] --> Ask clarifying Qs
- 3. CONF.0: scope clear? [Yes] --> 4. Route to agent/skill
- 4. Route to agent/skill --> 5. Read canon SKILL before work
- 5. Read canon SKILL before work --> 6. Execute (hooks enforce guards)
- 6. Execute (hooks enforce guards) --> 7. Respond: answer + Confidence + Agents involved
- User request arrives --> What type of<br>request?
- What type of<br>request? [Phoenix Q&A] --> phoenix-qa<br>(PhoenixExpert)
- What type of<br>request? [Bug report] --> bug-validator<br>(BugFinderAgent)
- What type of<br>request? [Test cases] --> cross-dep → tc-gen<br>→ tc-qual-val
- What type of<br>request? [/HandsOff] --> hands-off<br>(full orchestrator)
- What type of<br>request? [Run tests] --> energo-ts-run
- What type of<br>request? [DB query] --> database-query
- What type of<br>request? [Prod data] --> prod-data-reader
- What type of<br>request? [Other] --> Other: jira-bug,<br>postman, report,<br>env-access, shell
- bug-validator<br>(BugFinderAgent) --> Environment<br>needed?
- cross-dep → tc-gen<br>→ tc-qual-val --> Environment<br>needed?
- hands-off<br>(full orchestrator) --> Environment<br>needed?
- database-query --> Environment<br>needed?
- Environment<br>needed? [Yes] --> environment-resolver<br>(ask user or extract from ticket)
- environment-resolver<br>(ask user or extract from ticket) --> switch-phoenix-branches.ps1<br>(align Phoenix repos to env branch)
- Environment<br>needed? [No / resolved] --> Proceed to workflow<br>(see dedicated pages: 4–9)
- Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence --> Jira Cloud
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md --> Confluence<br>Cloud
- Step 4b: DB evidence (optional)<br>Same env via PostgreSQL MCP<br>Entity state, audit logs, error logs --> PostgreSQL
- Env resolve (PHOENIX-SWITCH.0)<br>switch-phoenix-branches.ps1 --> Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence
- Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence --> Step 1b: Check diagrams<br>Ticket attachments → config/Diagrams/<br>(Bundle 4/5/6 .svg fallback)
- Step 1b: Check diagrams<br>Ticket attachments → config/Diagrams/<br>(Bundle 4/5/6 .svg fallback) --> Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md --> Step 3: Swagger refresh (MANDATORY)<br>update-swagger-specs.ps1
- Step 3: Swagger refresh (MANDATORY)<br>update-swagger-specs.ps1 --> Step 4a: Phoenix code analysis<br>(aligned to env branch — READ-ONLY)<br>Endpoints, services, validators, schedulers
- Step 4a: Phoenix code analysis<br>(aligned to env branch — READ-ONLY)<br>Endpoints, services, validators, schedulers --> Step 4b: DB evidence (optional)<br>Same env via PostgreSQL MCP<br>Entity state, audit logs, error logs
- Step 4b: DB evidence (optional)<br>Same env via PostgreSQL MCP<br>Entity state, audit logs, error logs --> Step 5: 5-Verdict Matrix
- Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence --> Output:<br>scope, entry_points,<br>upstream, downstream,<br>impact_risk / what_could_break,<br>integration_points, technical_details
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md --> test_cases/Backend/&lt;Topic&gt;.md<br>(TC-BE-N only)
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md --> test_cases/Frontend/&lt;Topic&gt;.md<br>(TC-FE-N only)
- Step 2.5: test-case-quality-validator<br>STRICT 0–100 scoring (10 axes)<br>Pass threshold: 80/100 --> All TCs<br>≥ 80?
- All TCs<br>≥ 80? [< 80] --> Fail: return failing TCs + detailed fixes<br>to test-case-generator for rewrite
- Fail: return failing TCs + detailed fixes<br>to test-case-generator for rewrite [max 3 iterations] --> Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md
- Fail: return failing TCs + detailed fixes<br>to test-case-generator for rewrite --> 3 iterations<br>exhausted?
- 3 iterations<br>exhausted? [Yes] --> BLOCK WORKFLOW<br>Escalate to user with all<br>failing axes and reasons
- All TCs<br>≥ 80? [≥ 80 ✓] --> TCs saved and validated — workflow complete
- Step 1: cross-dependency-finder (MANDATORY FIRST)<br>Jira-anchored analysis (Rule 35a)<br>No local merge/git — Jira + codebase + shallow Confluence --> Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md
- Step 2: test-case-generator<br>Reads playwright instructions folder (MANDATORY)<br>Uses prompt + Confluence + codebase + cross_dependency_data<br>Writes Backend/&lt;Topic&gt;.md + Frontend/&lt;Topic&gt;.md --> Step 2.5: test-case-quality-validator<br>STRICT 0–100 scoring (10 axes)<br>Pass threshold: 80/100
- 0. Env resolve<br>+ Phoenix align --> 1. Jira ticket<br>fetch (Rule 42/44)
- 1. Jira ticket<br>fetch (Rule 42/44) --> 2. cross-dep<br>finder (Rule 35a)
- 2. cross-dep<br>finder (Rule 35a) --> 3. test-case<br>generator
- 3. test-case<br>generator --> 3b. tc-quality<br>validator (≥80)
- 3b. tc-quality<br>validator (≥80) --> 4. energo-ts-test<br>(Playwright author)
- 4. energo-ts-test<br>(Playwright author) --> 4b. playwright<br>test-validator (≥80)
- ... and 23 more connections

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

