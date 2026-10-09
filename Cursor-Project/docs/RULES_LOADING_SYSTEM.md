# Rules loading (Cursor workspace)

## Overview

Project constraints live in **`.cursor/rules/`** (including thematic subfolders: `01-main/`, `02-safety/`, `03-agents/`, etc.). **Rule 0.0** requires the assistant to treat these rules as authoritative before acting.

## How rules apply in Cursor

1. **Cursor loads rules** from `.cursor/rules/**/*.mdc` according to Cursor’s Rules UI and `alwaysApply` (and similar) frontmatter.
2. The **canonical index** is **`.cursor/rules/main/phoenix.mdc`** (or **`01-main/phoenix.mdc`** if your copy uses numbered folders).
3. **No Python loader:** This workspace does **not** ship `agents.rules_loader`, `load_rules_at_start()`, or `get_rules_loader()`. Those belonged to the removed **`Cursor-Project/agents/`** package.

## What the assistant must do (Rule 0.0)

- **Before** responding or using tools: load and apply applicable rules from `.cursor/rules/` (including integrations, workflows, commands registry).
- When Rule 0.6 requires a file: write markdown under **`Cursor-Project/reports/`** per **`Cursor-Project/reports/README.md`** — not a Python `ReportingService`.

## Adding or changing rules

- Add or edit **`.mdc`** files under `.cursor/rules/` (correct thematic folder).
- Update **`.cursor/rules/commands/commands_rules.mdc`** Rule **38** if you add a new **`.cursor/commands/*.md`** entry.
- Keep **English** for on-disk rule text (Rule 0.7).

## Violation consequences

Skipping Rule 0.0 or mandatory workflows (e.g. Confluence read-only, Phoenix path tiers, EnergoTS branch lock) is treated as a **critical workflow error** per **`core_rules.mdc`**.
