# Remove config/cursorrules and reference fixes

**Date:** 2026-03-27

## Actions

- Deleted `Cursor-Project/config/cursorrules/autonomous_rules.md` and removed empty `config/cursorrules/` directory.
- Updated `.cursor/rules/main/phoenix.mdc`: dropped index entry for `autonomous_rules.md`; Quick Reference now points to `agent_rules.mdc`, `workflow_rules.mdc`, and `safety_rules.mdc` for autonomous-operation patterns.
- Updated `Cursor-Project/agents/Main/test_agent.py` docstring for `_should_consult_agents` to reference workspace rules instead of the removed file.

## Note

Historical reports under `Cursor-Project/reports/` may still mention the old path; left unchanged as archive.

Agents involved: None (direct edits; TestAgent docstring only)
