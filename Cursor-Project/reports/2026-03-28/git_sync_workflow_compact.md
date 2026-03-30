# git_sync_workflow.mdc compaction

**Date:** 2026-03-28  
**Change:** `.cursor/rules/integrations/git_sync_workflow.mdc` reduced from ~1226 lines to a compact spec (~200 lines).

**Preserved:** `alwaysApply: true`; triggers (`!sync`, `!update`, `!checkout`); `Cursor-Project/Phoenix/` paths; readonly token env pattern; default repo clone list; stash/unstash; divergence abort; read-only (no push); per-step git commands; credential helper hint; safety compatibility note; optional short PowerShell outline for fetch.

**Removed:** Duplicate full PowerShell and Bash implementations repeated for each step; long troubleshooting/testing prose (condensed to a small error table).

**Related update:** `.cursor/commands/sync.md` — wording adjusted so it no longer claims the rule file contains a “complete” embedded script implementation.

Agents involved: None (direct edit)
