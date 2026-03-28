# Summary — `Cursor-Project/docs` legacy cleanup (2026-03-28)

## User request

Continue alignment: remove / reframe **Python `Cursor-Project/agents/`** and **IntegrationService / ReportingService** as the primary story in project documentation.

## Files updated

| File | Change |
|------|--------|
| `docs/CURSOR_SUBAGENTS.md` | Report-generator → Rule 0.6 + markdown; test_cases paths; workspace note (no Python package, Rule 0.3); rules path `**/*.mdc` |
| `docs/COMMANDS_REFERENCE.md` | Sections 9–17: Rule 0.3 instead of IntegrationService; production reader = MCP SQL; test_cases Objects/Flows; HandsOff + Slack/test paths; EnergoTS subagent wording |
| `docs/RULES_LOADING_SYSTEM.md` | Full rewrite: Cursor Rule 0.0, no `rules_loader` Python |
| `docs/AGENTS_COMPARISON_AND_ALIGNMENT.md` | Full rewrite: Cursor-first model + pointer to historical docs |
| `docs/HISTORICAL_PYTHON_AGENTS_PACKAGE.md` | **New** — index of docs that still mention old Python API |
| `docs/CROSS_DEPENDENCY_FINDER_AGENT.md` | Rule 0.3; test-case handoff wording; Rule 0.6 markdown |
| `docs/SETUP_CURSOR_NEW_PROJECT_GITLAB.md` | §2.6 + checklist: automation/MCP, not copying `agents/` |
| `docs/QUICK_START.md` | Verification + troubleshooting: `.cursor/rules`, no `get_integration_service` |
| `docs/AGENTS_README.md` | Historical banner at top |
| `README.md` (Cursor-Project) | Structure, Python section, doc links |

## Not rewritten line-by-line

Per **`HISTORICAL_PYTHON_AGENTS_PACKAGE.md`**, many agent-specific markdown files still contain old imports for archive/reference.

## Agents involved

Documentation alignment (direct); PhoenixExpert not required for doc-only edits.
