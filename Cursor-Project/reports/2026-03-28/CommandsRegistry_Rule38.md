# Command registry + Rule 38 (2026-03-28)

**Problem:** `commands_rules.mdc` only carried numbered Rules **36–37**, while `.cursor/commands/` has many more command docs.

**Change:** Added **Rule 38** (mandatory: follow each command’s `.md` + linked rules) and a **COMMAND REGISTRY** table mapping all **20** `.cursor/commands/*.md` files to typical agents and governing `.cursor/rules/**` files. Updated **`main/phoenix.mdc`**, **`README.md`** (rules), and **`workflows/workflow_rules.mdc`** cross-reference (36–38).

**Maintain:** When adding a new `.cursor/commands/<name>.md`, add a registry row and cite Rule 38 in reviews.

Agents involved: None (direct edit)
