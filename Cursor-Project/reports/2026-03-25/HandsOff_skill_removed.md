# HandsOff skill removed (command-only trigger)

**Date:** 2026-03-25

## Changes

1. **Deleted** `.cursor/skills/hands-off/SKILL.md` and removed empty `.cursor/skills/hands-off/` directory.
2. **Updated** `.cursor/rules/workflow_rules.mdc` (Rule 37 reference line): removed link to the skill; noted HandsOff is triggered via **/HandsOff**, **!HandsOff**, or the **hands-off** slash command only.
3. **Updated** `.cursor/skills/phoenix-commands/SKILL.md`: frontmatter and Hands-off section clarify there is no separate `hands-off` skill; use `.cursor/commands/hands-off.md` and Rule 37 triggers.

## Unchanged

- `.cursor/commands/hands-off.md` — primary procedural spec.
- `.cursor/agents/hands-off.md` — orchestrator agent doc.
- Rule 37 behavior (mandatory flow when user uses /HandsOff or !HandsOff).
