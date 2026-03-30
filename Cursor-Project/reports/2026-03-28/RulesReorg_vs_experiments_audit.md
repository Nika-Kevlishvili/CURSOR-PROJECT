# Rules reorganization audit vs `experiments` branch

**Date:** 2026-03-28  
**Scope:** Compare thematic `.cursor/rules/` layout (working tree) to `experiments` branch (flat layout).

## Baseline

- **`experiments`:** 12 files, flat paths under `.cursor/rules/*.mdc`.
- **Working tree:** Thematic subfolders (`main/`, `safety/`, `agents/`, `workflows/`, `commands/`, `workspace/`, `integrations/`), plus `README.md` and **`commands/commands_rules.mdc`** (Rules 36–37).

## Git state (important)

At audit time, the reorganization was **uncommitted**: `git status` shows deleted tracked flat files and untracked new directories. **Last commit** still points at the flat layout. Until you commit (or merge), a fresh clone at `HEAD` will **not** include the new structure.

## Did anything “break”?

### Structure and loading

- **Cursor** loads `.mdc` files under `.cursor/rules/` including subfolders; moving files does not drop rules if all files keep `alwaysApply: true` and remain under that tree.
- **Rules 36–37:** Full text lives in `.cursor/rules/commands/commands_rules.mdc`; `.cursor/rules/workflows/workflow_rules.mdc` replaces the old sections with an explicit cross-reference. Obligations are **preserved**, not removed.

### Reference drift (repo grep)

- No remaining references found to obsolete flat names like `.cursor/rules/core_rules.mdc` without the `main/` prefix in tracked markdown searched during audit; docs/skills/agents use thematic paths.

### Intentional differences from `experiments` (not caused by folder moves)

These appear in `git diff experiments:… -- working tree` and reflect **policy/doc updates** on your current line of work, not the folder split alone:

| Topic | `experiments` | Working tree |
|--------|----------------|--------------|
| Rule 32 | Python `BugFinderAgent` import example | Cursor workflow: Confluence + codebase + report; no `agents` package |
| Rule 0.8 | Broad “no code modification” framing | Tiered: Phoenix forbidden; EnergoTS tests only via EnergoTSTestAgent; other paths editable when user requests |
| Rule 0.0 / 0.3 / 0.6 | Python `rules_loader`, `IntegrationService`, `reporting_service` | Cursor/MCP-oriented wording |
| Rule 35a sync | “fetch, then merge/rebase” | Explicit **merge** `origin/<branch>` (aligned with `!sync`) |
| Rule 37 “Reference” | Included `.cursor/skills/hands-off/SKILL.md` | States HandsOff has **no** project skill; use commands/agents only |

If you need **`experiments` to remain the single source of truth** for those policies, you would need to reconcile those text differences separately from the folder layout.

## Assessment: quality of the reorganization

**Strengths**

- Clear **separation of concerns** (main vs safety vs workflows vs integrations vs commands).
- **`phoenix.mdc`** as an index with canonical paths reduces “where is Rule X?” friction.
- **Commands** (36–37) colocated with `.cursor/commands/` is coherent for operators.
- **Deduplication** (e.g. Rule 3 → pointer to Rule 0.1) reduces conflicting copies.

**Risks / follow-ups**

- **Commit** the new tree so CI, other machines, and `HEAD` match what Cursor sees locally.
- **External** wikis or personal bookmarks that still point at old flat paths will 404 until updated.
- **Rule 37** reference change (no `hands-off` skill path) should stay consistent with what actually exists under `.cursor/skills/`.

## Conclusion

The thematic layout **does not remove** critical workflows; Rules **36** and **37** are **moved**, not dropped. Compared to **`experiments`**, the working tree also contains **substantive rule text changes** (especially 0.8 and 32)—verify those match product intent. The main operational gap is **version control**: uncommitted moves mean the “official” branch snapshot still looks like the old flat rules until you commit.
