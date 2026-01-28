# Phoenix Expert Query

Route ALL Phoenix-related questions to PhoenixExpert agent (Rule 0.2 - ABSOLUTE PRIORITY).

## Mandatory Workflow:

1. **IntegrationService** - Call `IntegrationService.update_before_task()` FIRST
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

## Generate Reports (Rule 0.6):
- Save to `reports/YYYY-MM-DD/PhoenixExpert_{HHMM}.md`
- Save summary to `reports/YYYY-MM-DD/Summary_{HHMM}.md`
