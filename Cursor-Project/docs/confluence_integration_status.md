# Confluence Integration Status

> **Current:** Confluence via **MCP** in Cursor; Phoenix answers → **`.cursor/agents/phoenix-qa.md`** (Rule 0.2, 0.5). Map: **[AGENT_SUBAGENT_MAP.md](AGENT_SUBAGENT_MAP.md)**.

## Integration Complete

Confluence documentation has been integrated into the **PhoenixExpert** agent system in **read-only mode**.

## Integration Points

### PhoenixExpert (Cursor)

- **Role:** **`.cursor/agents/phoenix-qa.md`** — Phoenix Q&A with code primary and Confluence supplementary (Rule 0.2, 0.5).
- **Capabilities:** Read-only Confluence via MCP; REST read fallback per **`confluence_rest_fallback.mdc`** when MCP fails; Phoenix codebase read under **`Cursor-Project/Phoenix/**`** (AI never edits Phoenix).

### Confluence Access
- **Primary (Cursor):** Confluence via **MCP** read tools; Phoenix Q&A → **`.cursor/agents/phoenix-qa.md`** (Rule 0.2, 0.5).
- **REST read fallback:** When MCP is unavailable after retries, agents use **`.cursor/rules/integrations/confluence_rest_fallback.mdc`** (Rule **43** / **CONFLUENCE.1**) — Basic auth, **read-only**, same disclosure line as that rule. Helper script: **`Cursor-Project/config/confluence/get-confluence-page-rest.ps1`**; env table: **`Cursor-Project/config/confluence/README.md`**.
- **Base URL**: `CONFLUENCE_WIKI_BASE` or `CONFLUENCE_URL` (or derive from `JIRA_BASE_URL` per README); optional `CONFLUENCE_EMAIL` / `CONFLUENCE_API_TOKEN` or reuse `JIRA_EMAIL` / `JIRA_API_TOKEN`.
- **Mode**: Read-only (never creates, edits, or deletes)
- **Historical note:** Older Python `get_phoenix_expert` examples below are **not** in this workspace; Cursor uses agents/skills/MCP only.

## Rules Enforced

1. ✅ **Read-only access** - Never modifies Confluence
2. ✅ **Code is authoritative** - Confluence is supplementary
3. ✅ **Automatic integration** - No approval needed to read
4. ✅ **Persistent caching** - Documentation cached for offline use
5. ✅ **Priority system** - Code takes precedence over Confluence
6. ✅ **No modifications** - Do NOT modify, commit, push, merge, delete, or execute anything

## Usage example (REST fallback helper)

```powershell
$env:JIRA_EMAIL = 'user@company.com'
$env:JIRA_API_TOKEN = 'your-api-token'
$env:JIRA_BASE_URL = 'https://your-company.atlassian.net'
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/confluence/get-confluence-page-rest.ps1" -PageId '779517953'
```

## Status

✅ Confluence integration complete
✅ PhoenixExpert agent created
✅ Read-only mode enforced
✅ Code authority maintained
✅ All previous managers deleted
✅ Ready for questions

