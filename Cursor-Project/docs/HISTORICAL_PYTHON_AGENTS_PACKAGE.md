# Historical: `Cursor-Project/agents/` Python package

**Where to work today:** **[AGENT_SUBAGENT_MAP.md](AGENT_SUBAGENT_MAP.md)** — all subagent paths (`.cursor/agents/*.md`), report paths (`Cursor-Project/reports/...`), test case paths.

The **`Cursor-Project/agents/`** directory (Main, Support, Core, Adapters, Services, Utils) was **removed** from this workspace. Cursor-facing behavior is defined by:

- **`.cursor/rules/**/*.mdc`**
- **`.cursor/agents/*.md`**
- **`.cursor/skills/`** and **`.cursor/commands/`**
- **MCP** servers configured for this project

## Archived docs (Python agents era)

The following files were moved to **`Cursor-Project/docs/_archive/`**. They reference the old Python package (`from agents...`, `AgentRouter`, `IntegrationService`, `ReportingService`, `get_*_agent()`):

- `AGENTS_README.md`, `GLOBAL_RULES_AND_ROUTING.md`
- `BUG_FINDER_AGENT.md`, `TEST_CASE_GENERATOR_AGENT.md`, `JIRA_DESCRIPTION_WRITER_AGENT.md`, `EnergoTSTestAgent_README.md`
- `GITLAB_UPDATE_AGENT.md`, `INTEGRATION_SERVICE_CONFIG.md`
- `POSTMAN_COLLECTION_GENERATOR.md`, `README_TEST_AGENT.md`, `Q&A_MODE_ASSESSMENT.md`
- `DOWNLOAD_PHOENIX_PROJECTS.md`, `PHOENIX_EXPORT_SINGLE_FILE.md`, `PHOENIX_GITLAB_CLONE.md`, `confluence_integration_status.md`

For current procedures, see **`CURSOR_SUBAGENTS.md`**, **`COMMANDS_REFERENCE.md`**, **`RULES_LOADING_SYSTEM.md`**, and **`AGENTS_COMPARISON_AND_ALIGNMENT.md`**.
