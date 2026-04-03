# Project Skills

Skills teach the Cursor agent how to follow this project's agents, rules, and commands. They are loaded dynamically when the agent determines they are relevant (based on the description field).

**Location:** `.cursor/skills/` (same level as `commands/` and `hooks/`)

## Skills

| Skill | Purpose |
|-------|---------|
| **phoenix-core** | Core agent workflow: routing, PhoenixExpert consultation, IntegrationService, read-only safety, report generation, agent directory structure |
| **phoenix-workflows** | Maps user intent to commands: bug validation, database queries, file organization, Jira bugs, sync, cross-deps, test cases, Playwright, HandsOff |
| **cross-dependency-finder** | CrossDependencyFinderAgent: find cross-dependencies and what could break; merge lookup for Jira keys (Rule 35/35a) |
| **test-case-generator** | TestCaseGeneratorAgent: generate test cases (Rule 35 = cross-dependency-finder first) |
| **production-data-reader** | ProductionDataReaderAgent: read production data, analyze offsets, explain entity creation (Rule PDR.0) |
| **energo-ts-run** | Run Playwright tests from EnergoTS by prompt (newly created, Jira key, file path) |
| **jira-bug-template** | Jira bug text for Experiments board only (Rule JIRA.0); NOT for Phoenix delivery |

## Source

Skills are derived from:
- **Agents:** `Cursor-Project/agents/` (Main, Support, Core, Adapters, Services, Utils)
- **Rules:** `.cursor/rules/*.mdc`
- **Commands:** `.cursor/commands/*.md`
