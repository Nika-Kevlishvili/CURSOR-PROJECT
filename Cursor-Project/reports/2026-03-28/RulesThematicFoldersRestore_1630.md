# Restore thematic `.cursor/rules/` layout

**Request:** Bring back the folder-based rules layout (workspace root), without changing Phoenix nested repos.

**Done:**
- Moved 13 `.mdc` files into: `main/`, `safety/`, `agents/`, `workflows/`, `workspace/`, `integrations/`.
- Replaced flat paths `.cursor/rules/<file>.mdc` → thematic paths across `.cursor/**` (agents, commands, skills, hooks if any).
- Updated `main/core_rules.mdc` Rule 0.0 to require reading all subfolders.
- Rewrote `main/phoenix.mdc` index (thematic table + note on Phoenix nested rules).
- Patched `safety/safety_rules.mdc` core_rules path references.
- Added `.cursor/rules/README.md`, `.cursor/README.md`.
- Docs: `SETUP_CURSOR_NEW_PROJECT_GITLAB.md`, `CROSS_DEPENDENCY_FINDER_AGENT.md` — `git_sync_workflow` path.
- `Cursor-Project/README.md` — manual setup link → `../.cursor/README.md`.

**Not changed:** `commands_rules.mdc` was not present in flat layout; Rules 36–37 remain in `workflows/workflow_rules.mdc`.

**Agents involved:** None (file moves + scripted path updates).
