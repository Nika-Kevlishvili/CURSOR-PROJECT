---
name: phoenix-database
description: Runs PostgreSQL queries via MCP using the correct environment (Dev, Dev2, Test, PreProd, Prod), connect-first workflow, and standard contract/POD query patterns. Use when the user asks about database, queries, contracts, POD identifier, or a specific environment (Dev, Test, Prod).
---

# Phoenix Database Queries

Ensures database access uses the right PostgreSQL MCP server, connect-before-query workflow, and standard patterns. Credentials and full connection details are in `Cursor-Project/.cursor/rules/database_workflow.mdc`; never embed or log passwords.

## When to Apply

- User asks to query the database, check contracts, or find data by POD.
- User mentions Dev, Dev2, Test, PreProd, or Prod environment.
- Task requires Phoenix PostgreSQL data (contracts, POD, status, dates).

## Environment Selection (Rule DB.0)

Use the **exact** environment the user asks for. Do not switch.

| User says | MCP server |
|-----------|------------|
| Dev | PostgreSQLDev |
| Dev2 | PostgreSQLDev2 |
| Test | PostgreSQLTest |
| PreProd | PostgreSQLPreProd |
| Prod | PostgreSQLProd (read-only user) |

## Workflow

1. **Connect first** with the chosen MCP: `mcp_PostgreSQL{Env}_connect_db(...)` using credentials from `database_workflow.mdc`.
2. **Then run queries:** `mcp_PostgreSQL{Env}_query(sql="SELECT ...")` or `mcp_PostgreSQL{Env}_execute(...)` for writes if allowed.
3. Reconnect if the session drops.

## Standard Patterns (Rule DB.2)

**Contracts by POD identifier:** Use DISTINCT, include `contract_type` (PRODUCT_CONTRACT / SERVICE_CONTRACT), contract and POD IDs, status, dates. Product contracts: `product_contract.contracts` → `contract_details` → `contract_pods` → `pod_details` → `pod.pod`. Service contracts: `service_contract.contracts` → `contract_details` → `contract_pods` → `pod.pod`. POD identifier is in `pod.pod.identifier` (varchar 33).

**Best practices:** DISTINCT on joins, order by contract_number and id, include status and date fields. See `database_workflow.mdc` for full SQL templates.

## Security (Rule DB.5)

- Never commit or log database credentials or passwords.
- Credentials live in MCP config / rules only; reference the rule file when connecting.

## Rules Source

Full connection details, credentials, and SQL patterns: `Cursor-Project/.cursor/rules/database_workflow.mdc`. Rule 33: Test environment queries use PostgreSQLTest MCP.
