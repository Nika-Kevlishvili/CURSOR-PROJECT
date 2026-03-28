# Python agents package removed

**Date:** 2026-03-28  
**Action:** Deleted `Cursor-Project/agents/` (entire tree) per user request (chat-only workflow).

## Done

- Removed directory `Cursor-Project/agents/` (all Python agent modules, services, adapters).
- Updated **always-applied** rules to stop requiring Python `agents.*`: `core_rules.mdc`, `workflow_rules.mdc` (Rule 32), `agent_rules.mdc`, `production_data_reader.mdc`, `file_organization_rules.mdc`, `safety_rules.mdc` (Rules 19–20), `phoenix.mdc` (quick reference).
- Updated `.cursor/commands/hands-off.md` reporting reference (no `reporting_service.py` path).

## Still referencing Python `agents` (non-blocking for Cursor chat)

- `.cursor/skills/*.md`, some `.cursor/agents/*.md`, and `Cursor-Project/docs/*.md` / `Cursor-Project/examples/*.py` may still show **historical** `from agents...` snippets.
- **Running** `Cursor-Project/examples/*.py` that import `agents` will **fail** until those scripts are updated or the package is restored.

## Follow-up (optional)

- Trim or rewrite `Cursor-Project/docs/AGENTS_README.md` and broken examples if you still run Python from this repo.
