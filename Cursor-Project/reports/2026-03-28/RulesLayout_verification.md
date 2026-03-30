# Rules layout verification (2026-03-28)

**Thematic folders:** `main/`, `safety/`, `agents/`, `workflows/`, `commands/`, `workspace/`, `integrations/` — contents match `.cursor/rules/README.md` and `main/phoenix.mdc` canonical list (14 `.mdc` files).

**`alwaysApply: true`:** Present on all rule `.mdc` files after adding YAML frontmatter to `integrations/jira_bug_agent.mdc` (previously missing; same as old flat copy on `experiments`).

**Git:** Thematic tree may still be uncommitted vs `HEAD` (flat paths deleted in index, new dirs untracked) — commit when ready so remote/clone matches local layout.

Agents involved: None (direct verification + edit)
