# Assessment: Deleting `Cursor-Project/agents/`

**Date:** 2026-03-28  
**Request:** Remove `Cursor-Project/agents/` only if nothing depends on it.  
**Decision:** **Do not delete.** Multiple dependencies exist.

## Dependencies (non-exhaustive)

### Workspace rules (`.cursor/rules/`)
- `core_rules.mdc` — `load_rules_at_start`, `log_ai_response`, `get_reporting_service`; Tier C lists `Cursor-Project/agents/**`.
- `workflow_rules.mdc` — `get_bug_finder_agent` example imports.
- `agent_rules.mdc` — `environment_access_agent`, `integration_service`, `agent_router`, import patterns.
- `production_data_reader.mdc` — `get_production_data_reader_agent`, `get_agent_router`.

### Commands and agents (`.cursor/commands/`, `.cursor/agents/`)
- `hands-off.md` — references `Cursor-Project/agents/Services/reporting_service.py`.
- `energo-ts-test.md`, `report.md` — example `from agents.Main` / `reporting_service` imports.
- Several `.cursor/agents/*.md` files map explicitly to files under `Cursor-Project/agents/` (e.g. test-case-generator, postman-collection, environment-access).

### Skills (`.cursor/skills/`)
- `phoenix-agent-workflow`, `phoenix-bug-validation`, `phoenix-reporting`, `production-data-reader`, `cross-dependency-finder` context, `README.md` — reference Python `agents.*` or the folder path.

### Project docs and examples (`Cursor-Project/docs/`, `Cursor-Project/examples/`)
- `AGENTS_README.md`, `EnergoTSTestAgent_README.md`, `SETUP_CURSOR_NEW_PROJECT_GITLAB.md`, and other docs.
- Examples: `generate_test_cases.py`, `jira_description_writer_example.py`, `update_project_from_gitlab.py`, and several legacy scripts importing `agents.*`.

## Impact if deleted anyway
- Running any of the above Python examples or tooling that imports `agents` → **ImportError**.
- Rules/skills remain valid as *guidance* for the AI in chat, but **code samples and file paths** would point to missing files.

## Recommendation
Keep `Cursor-Project/agents/` unless you plan a dedicated cleanup: update or remove all references, then remove the package in one change set.
