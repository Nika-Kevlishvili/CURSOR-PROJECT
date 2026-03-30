# Rules and cross-link audit (2026-03-28)

## Scope

- All **14** `.mdc` files under `.cursor/rules/**` (thematic layout).
- Explicit **`.cursor/rules/.../*.mdc`** references in rules, commands, agents, skills (sampled repo-wide).
- **Command registry** rows vs **20** `.cursor/commands/*.md` files.

## Results

| Check | Status |
|--------|--------|
| **`main/phoenix.mdc` canonical list (14 paths)** | All files exist on disk. |
| **Cross-references between rules** (`workflow_rules` → `core_rules`, `database_workflow`, `git_sync`, `commands_rules`; `safety` → `core` / `agents`; `energots` ↔ `git_sync` / `safety`; `commands` → `workflow` / `handsoff`) | Paths valid. |
| **No flat legacy paths** (e.g. `.cursor/rules/core_rules.mdc` without `main/`) | None found outside historical report text. |
| **Registry vs commands** | 20 command `.md` files; 20 table rows. |
| **Registry agent files** | All listed `.cursor/agents/*.md` exist (`bug-validator.md` corrected from generic “bug-validator” label). |

## Fixes applied during audit

1. **`commands_rules.mdc`** — Second column uses full **`.cursor/agents/...`** paths; **bug-validate** row points to **`bug-validator.md`**; **consult** row references **`phoenix-qa.md`**; added note for **agent-only** flows (database-query, environment-access, postman-collection, test-runner) not covered by Rule 38’s command-folder scope.
2. **`cross-dependency-finder/SKILL.md`** — Rule 35a sync reference now uses full **`.cursor/rules/integrations/git_sync_workflow.mdc`**.

## Non-blocking / content drift (not broken links)

- Several **`.cursor/commands/*.md`** and **skills** still mention **`IntegrationService.update_before_task()`** or Python `agents` imports; **`core_rules.mdc` Rule 0.3** already states there is no `Cursor-Project/agents/` in this workspace — executors should prefer MCP/chat wording. Optional cleanup task for those markdown files.
- **Runtime** (MCP, Jira, Slack, DB reachability) was **not** exercised; this audit is **static link and layout** consistency only.

## Conclusion

Rule **files and internal `.mdc` links** are **consistent** with the thematic layout. **Rule 38** registry matches the command set after the clarifications above.

Agents involved: None (direct audit and edits)
