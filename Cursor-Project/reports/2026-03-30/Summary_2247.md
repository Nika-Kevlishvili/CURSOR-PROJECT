# Summary — revert Rule 35b from workflow rules (2026-03-30 22:47)

## User request

Keep the setup **as originally done**: do **not** centralize cross-dependency git/Confluence policy in `workflow_rules.mdc` as a new numbered rule.

## Changes

- **Removed** Rule **35b** block from `.cursor/rules/workflows/workflow_rules.mdc`.
- **Restored** Rule **35** Step 1 to reference only **Merge-first and conditional sync** (Rule 35a section below).
- **Restored** Rule **37** HandsOff step 2 to **Rule 35a** only (no 35b).
- **Restored** `.cursor/rules/main/phoenix.mdc` quick reference item 10 to **Rule 35/35a** only.

## Unchanged

- Full instructions remain in `.cursor/skills/cross-dependency-finder/SKILL.md`, `.cursor/agents/cross-dependency-finder.md`, `.cursor/commands/cross-dependency-finder.md`, and `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md` (git snapshot fast path + shallow Confluence as before).
- `CrossDependency_GitSnapshot.ps1` and `cross_dependencies/cache/` unchanged.

Agents involved: PhoenixExpert
