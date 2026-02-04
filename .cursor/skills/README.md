# Project Skills

Skills teach the Cursor agent how to follow this project's agents, rules, and commands. They are used automatically when the agent detects relevant triggers (e.g. Phoenix questions, bug validation, reports, file placement).

**Location:** Workspace root `.cursor/skills/` (same level as `commands/` and `hooks/`) so Cursor's "Rules, Skills, Subagents" UI can discover them.

## Skills

| Skill | Purpose |
|-------|---------|
| **phoenix-agent-workflow** | AgentRouter, PhoenixExpert consultation, IntegrationService, report footer, agent directory structure |
| **phoenix-bug-validation** | BugFinderAgent workflow (Rule 32): Confluence → codebase → analysis → report |
| **phoenix-file-organization** | Where to put files: agents, docs, User story, reports/YYYY-MM-DD, config, postman |
| **phoenix-reporting** | Report generation (Rule 0.6): agent reports + summary, path and naming |
| **phoenix-commands** | When to use which command: Phoenix, consult, report, bug-validate, production-data-reader, sync |
| **phoenix-database** | PostgreSQL MCP: environment (Dev/Test/Prod), connect first, contract/POD query patterns, no credentials in logs |
| **production-data-reader** | ProductionDataReaderAgent workflow (Rule PDR.0): read production data → analyze offsets → explain step-by-step creation |
| **phoenix-safety-readonly** | GitLab/Confluence read-only, code modification forbidden, Confluence edit tools forbidden, no credentials in logs |

## Source

Skills are derived from:

- **Agents:** `Cursor-Project/agents/` (Main, Support, Core, Adapters, Services, Utils)
- **Rules:** `Cursor-Project/.cursor/rules/*.mdc`
- **Commands:** `.cursor/commands/*.md`

For full authority, the agent still loads rules from `Cursor-Project/.cursor/rules/` (Rule 0.0). Skills summarize and point to those rules.
