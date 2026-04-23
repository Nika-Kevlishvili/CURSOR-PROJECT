---
name: phoenix-database
description: Runs PostgreSQL queries via MCP using the correct environment (Dev, Dev2, Test, PreProd, Prod) with connect-first workflow and standard contract/POD query patterns. Use when the user asks about database, queries, contracts, POD identifier, or a specific Phoenix DB environment (Dev, Dev2, Test, PreProd, Prod).
---

# Phoenix Database Queries

Canonical guidance for querying Phoenix PostgreSQL via MCP. Connection credentials live **only** in MCP server config - never in this skill, in rules, or in code. Loaded on demand when the user mentions database / queries / contracts / POD / environment.

## When to apply

- User asks to query the database, check contracts, find data by POD, or inspect status/dates.
- User mentions Dev, Dev2, Test, PreProd, or Prod environment.
- Task requires Phoenix PostgreSQL data (contracts, POD, status, dates, payments, etc.).

## Rule DB.0 - Environment selection (CRITICAL)

Use the **exact** environment the user requests. Do NOT switch to a different one.

| User says | MCP server | Notes |
|-----------|------------|-------|
| Dev       | `PostgreSQLDev`     | |
| Dev2      | `PostgreSQLDev2`    | |
| Test      | `PostgreSQLTest`    | |
| PreProd   | `PostgreSQLPreProd` | |
| Prod      | `PostgreSQLProd`    | Read-only user only - see `production-data-reader` skill (Rule PDR.0) |

## Rule DB.1 - Credentials (CRITICAL - SECURITY)

- All host / port / user / password values for each environment live in the **MCP server configuration** (Cursor MCP settings).
- NEVER paste credentials into a skill, rule, command, agent, code file, report, or chat.
- NEVER log passwords. NEVER commit them to git.
- If credentials are missing or wrong, ask the user to update the MCP server config; do not hard-code them anywhere.

## Connect-first workflow (Rule DB.6)

1. **Connect** with the matching MCP server, e.g. `mcp_PostgreSQLDev_connect_db(...)`. The connection persists for the session; reconnect if it drops.
2. **Run queries:** `mcp_PostgreSQL{Env}_query(sql="SELECT ...")` for SELECT, `mcp_PostgreSQL{Env}_execute(...)` for INSERT/UPDATE/DELETE (only when allowed).
3. **Format results clearly** in chat (table or list); include the relevant columns (status, dates, IDs).
4. **Save to a markdown report only** if the user runs `/report` or explicitly asks (Rule 0.6); otherwise reply in chat only.

## Rule DB.2 - Standard query patterns

For "find contracts by POD identifier", use the templates in `references/contract_pod_queries.sql` (loaded on demand). Two queries: one for `product_contract.*` (POD via `pod_details`) and one for `service_contract.*` (POD via direct `pod_id`).

Schema relationships:

- **Product contracts:** `contracts -> contract_details -> contract_pods -> pod_details -> pod`
- **Service contracts:** `contracts -> contract_details -> contract_pods -> pod` (direct `pod_id`)
- **POD identifier:** `pod.pod.identifier` (`varchar(33)`)

## Rule DB.3 - Query best practices

1. Always use `DISTINCT` when joining multiple tables to avoid duplicate rows.
2. Include a `contract_type` column when querying both Product and Service contracts so results are distinguishable.
3. Order by `contract_number, contract_id` (or equivalent stable keys) for consistent output.
4. Include relevant dates: `create_date`, `entry_into_force_date`, `termination_date`, `activation_date`, `deactivation_date`.
5. Include status fields: contract status, contract_pod status.

## Rule DB.4 - Error handling

- If connection fails: verify the MCP server is configured and reachable; do not fall back to hard-coded credentials.
- If query fails: check SQL syntax, schema, and table names against the schema chain above.
- Handle empty result sets gracefully (return "no rows" instead of erroring).
- Log errors with full context (no credentials).

## Rule DB.5 - Security

- READ-ONLY by default, especially on Prod (`PostgreSQLProd` uses a read-only user; route Prod questions through the `production-data-reader` skill / Rule PDR.0).
- Never commit credentials. Never log passwords. Never include them in reports.

## Related

- **Rule 33** (workflow_rules.mdc): Test environment queries use `PostgreSQLTest` MCP.
- **`production-data-reader` skill** (Rule PDR.0): Prod data analysis (liability, receivable, payment, deposit, invoice, contract, etc.).
- **References:** `references/contract_pod_queries.sql` for full SQL templates.
