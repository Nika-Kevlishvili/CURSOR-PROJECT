# Examples (`Cursor-Project/examples`)

## Current workflows (recommended)

Phoenix / QA / reporting / GitLab flows are driven from **Cursor**:

- Subagents: **`<workspace-root>/.cursor/agents/*.md`**
- Rules: **`<workspace-root>/.cursor/rules/**/*.mdc`**
- Reports: **`Cursor-Project/reports/YYYY-MM-DD/`** (Rule 0.6)

**Canonical map:** `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`

## Python scripts in this folder

Many `*.py` files were written for the removed **`Cursor-Project/agents/`** package (`from agents...`, `get_*_agent()`). They will **fail to import** unless you restore that package elsewhere.

Files that import `agents` are **legacy**; each such file starts with a short notice pointing here and to `AGENT_SUBAGENT_MAP.md`.

Scripts that only use `requests`, Git, or local paths may still run independently — check imports at the top of each file.
