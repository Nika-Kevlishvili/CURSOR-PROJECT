# Phoenix Expert Query

Route ALL Phoenix-related questions to PhoenixExpert agent (Rule 0.2 - ABSOLUTE PRIORITY).

## Mandatory Workflow:

1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Confluence Check** - Search Confluence via MCP tools (FRESH, no cache)
3. **Codebase Check** - Search Phoenix codebase (primary source)
4. **PhoenixExpert Response** - Provide answer with full context

## Source Priority:
1. Codebase (highest - always wins)
2. Confluence (secondary)
3. General knowledge (fallback)

## Response Requirements:
- State "**Expert:** PhoenixExpert" at beginning
- Provide comprehensive answer
- End with: "Agents involved: PhoenixExpert"

## Reports (Rule 0.6 — optional):
- **Default:** answer in chat only; do **not** create files under **`Cursor-Project/reports/`** unless the user asks for a saved report or uses **`/report`** (paths per **`Cursor-Project/reports/README.md`**).
