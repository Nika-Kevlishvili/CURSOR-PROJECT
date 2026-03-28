# Summary — Rule deduplication via canonical pointers

**Date:** 2026-03-28  
**Request:** Duplicate rules exist; consolidate without breaking behavior.

## Answer

**Yes, there was duplication** (same policy in multiple `alwaysApply` files). **Approach:** Keep **one authoritative copy** and replace repeated prose with **pointers** so numbering and files stay stable.

## Changes

1. **`workflows/workflow_rules.mdc` Rule 3** — No longer repeats Rule 0.1 text; points to `.cursor/rules/main/core_rules.mdc` Rule **0.1** as canonical.
2. **`safety/safety_rules.mdc` Rule 7** — Tier summary removed; points to **Rules 0.8 / 0.8.1** in `core_rules.mdc` for authoritative path tiers.
3. **`safety/safety_rules.mdc` Rule 31** — Removed restatement identical to path tiers; reinforces Rule 7 + `core_rules` 0.8 / 0.8.1.
4. **`main/phoenix.mdc` — Critical reminders** — Replaced long numbered list (overlapping `core_rules` / `workflow_rules`) with a **pointer table** plus a short one-line sanity check.

## What we did *not* merge

- Full merger of files (e.g. one giant `rules.mdc`) — would risk context size, blur themes, and complicate maintenance.
- Removal of `alwaysApply` files — all thematic files remain loaded as before.

**Agents involved:** PhoenixExpert, Reporting (Rule 0.6)
