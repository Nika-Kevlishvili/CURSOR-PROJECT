# Rule 0.8 update — product code vs agent orchestration (2026-03-25)

**Change (user-requested):** Rule 0.8 no longer forbids all code edits globally. It now:

- **FORBIDDEN:** `Cursor-Project/Phoenix/**`, `Cursor-Project/EnergoTS/**` except `EnergoTS/tests/`, and any other `Cursor-Project/**` path not explicitly allowed (examples, docs, config, postman, etc.).
- **ALLOWED:** `Cursor-Project/agents/**`; `.cursor/agents/**`, `.cursor/rules/**`, `.cursor/skills/**`, `.cursor/commands/**`.

**Files updated:** `.cursor/rules/core_rules.mdc` (Rule 0.8, 0.8.1 wording), `.cursor/rules/safety_rules.mdc` (Rules 7, 31), `.cursor/rules/phoenix.mdc` (quick reference).

**Note:** `Cursor-Project/config/cursorrules/autonomous_rules.md` still says broad READ-ONLY; if that conflicts with daily use, extend ALLOW list or align that file in a follow-up.

Agents involved: None (rules edit only)
