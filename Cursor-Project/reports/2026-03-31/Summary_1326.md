# Summary 2026-03-31 13:26

## Task

Updated `.cursor/rules/integrations/energots_branch_lock.mdc` frontmatter per Cursor project rules format (<https://cursor.com/docs/context/rules>).

## Changes

- Added `description` for intelligent application when EnergoTS branch policy is relevant.
- Added `globs` for `Cursor-Project/EnergoTS/**` and energo-ts-run agent/command/skill paths.
- Kept `alwaysApply: false` (workflow rules already reference Rule ENERGOTS.0; file/context scoping avoids loading the full rule on every chat).

## Rationale

`alwaysApply: true` was not chosen to limit context size; `description` + `globs` matches Cursor guidance for “Apply intelligently” and “Apply to specific files” so the rule attaches when working in EnergoTS or running Playwright via energo-ts-run.
