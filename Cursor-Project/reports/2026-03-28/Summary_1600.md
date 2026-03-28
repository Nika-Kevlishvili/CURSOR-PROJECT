# Summary — `.cursor` legacy cleanup follow-up (2026-03-28)

## Scope

Continued alignment after removing references to the old Python `Cursor-Project/agents/` stack in Cursor-facing docs.

## Changes (this session)

1. **`.cursor/agents/production-data-reader.md`** — Replaced outdated “Rule 11: IntegrationService” bullet with **Rule 0.3** + MCP workflow pointer (`integrations/production_data_reader.mdc`).
2. **`.cursor/skills/README.md`** — Rewrote skill table (no AgentRouter/IntegrationService as primary); source = `.cursor/agents`, `.cursor/rules`, `.cursor/commands`; added **energo-ts-run** row.
3. **`.cursor/skills/phoenix-file-organization/SKILL.md`** — Directory map now matches Rule 31: `.cursor/agents`, `Cursor-Project/test_cases`, reports path, optional future `agents/` note; removed `agents/Main/*.py` examples.

## Verification

Grep under `.cursor` for legacy imports: remaining hits are **intentional** (“no Python X in this workspace”) or Rule 0.3/Pattern 7 wording for external automation.

## Agents involved

Direct edits (assistant acting as documentation maintainer; PhoenixExpert not required for `.cursor` text-only alignment).
