## Task

Fix `shell` Task subagent failure (`Required tool READ not found in allTools`).

## Change summary

1. Added **`.cursor/agents/shell.md`** with YAML frontmatter (`name: shell`, `model: fast`, `description`) so Cursor resolves **`subagent_type: shell`** to a project subagent with normal tool context (same pattern as `git-sync.md`, `cross-dependency-finder.md`).
2. Updated **`.cursor/agents/README.md`**, **`Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`**, **`Cursor-Project/docs/CURSOR_SUBAGENTS.md`** to register **shell**.
3. Updated **`.cursor/rules/agents/agent_rules.mdc`** Rule 13 with shell routing and fallback (`generalPurpose` / parent shell) if internal errors persist.

## Verification

- **Task** `subagent_type: shell` smoke test: **PASS** — subagent returned `OK-shell-fixed`.

## Note

Root cause was a missing project subagent spec for `shell`; Cursor fell back to a broken built-in tool set. The spec file aligns delegation with other `.cursor/agents/*.md` subagents.
