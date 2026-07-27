# MCP authentication

The installer writes Atlassian MCP server entries (`Confluence`, `Jira`) into `%USERPROFILE%\.cursor\mcp.json`.

Writing JSON is not enough — you must authenticate in Cursor:

1. Restart Cursor (or reload MCP servers).
2. Open MCP / Atlassian settings and complete OAuth / login for Jira and Confluence.
3. Confirm tools appear (e.g. get issue, get Confluence page).

PostgreSQL servers (`PostgreSQL{Env}`) use `npx mcp-postgres-server` and the connection fields you entered. They do not use Atlassian OAuth.

If MCP fails after retries, use REST helpers under `config/scripts/` with credentials from `.env`.
