---
name: phoenix-database
description: Runs PostgreSQL queries via MCP using the correct environment (Dev, Dev2, Test, PreProd, Prod). PRIMARY control is rules (DB.0, DB.0a, CONF.0) and this skill — ask environment before any MCP. Use for database, contracts, POD, reminder/RFD lookups, or environment-specific data.
---

# Phoenix Database Queries

**Primary enforcement:** **rules** (`.cursor/rules/integrations/database_workflow.mdc` Rule DB.0 / DB.0a, `.cursor/rules/main/clarification_and_confidence.mdc` Rule CONF.0) + **this skill** + **database-query** agent (`.cursor/agents/database-query.md`). The agent MUST follow Step 0 in chat **before** any PostgreSQL MCP call. Project hooks (`.cursor/hooks/*`) are a **secondary safety net only** — never skip Step 0 because hooks exist or might fail.

Connection parameters come from the MCP tool descriptor / Cursor MCP config — never embed or log passwords.

## When to Apply [MANDATORY]

Apply this skill **before** any PostgreSQL MCP (`connect_db`, `query`, `execute`, …) when:

- User asks for data from the database (reminder/RFD/POD/contract/liability/etc.).
- Task needs live DB evidence (IDs, links, “which X uses Y”).
- Parent workflow delegates database work (bug validation, cross-dep, production reader, Q&A).

## Step 0 — Environment gate (Rule DB.0a) [STOP — no MCP until done]

**Order (non-negotiable):**

1. **Read** whether the **user** named an environment in the **current task** (`Dev`, `Dev2`, `Test`, `PreProd`, `Prod`, `Experiments`) or said `your choice` / `defaults` / `same as last time`.
2. If **missing** → **STOP**. Ask in chat (prefer **AskQuestion** with all six options). **Do not** grep/code-read as a substitute for DB env. **Do not** call PostgreSQL MCP.
3. **Forbidden inference** (does NOT count as user naming env):
   - PDT / Jira ticket prefix (e.g. PDT-2529, PDT-2861)
   - EnergoTS test defaults (e.g. `PDT-2529` → reminder `2163` on **Dev**)
   - `envVariables`, Playwright branch, prior chat, or habit (“usually Test”)
   - **Rule 32 / bug-validator:** empty Jira `environment` + “code-only validation” — still ask before PostgreSQL MCP; parent must not pre-pick `test` in Task prompt without user confirmation
4. Only after the user names env (or authorizes your choice) → map to MCP per table below → connect → query.
5. State in chat: `DB environment: <Env>` before first query result.

**Violation of Step 0 is a CRITICAL SYSTEM ERROR** (same as Rule CONF.0).

## Environment Selection (Rule DB.0)

Use the **exact** environment the user asks for. Do not switch.

| User says | MCP server |
|-----------|------------|
| Dev | PostgreSQLDev |
| Dev2 | PostgreSQLDev2 |
| Test | PostgreSQLTest |
| PreProd | PostgreSQLPreProd |
| Prod | PostgreSQLProd (read-only user) |
| Experiments | PostgreSQLexperiments |

## Workflow

1. **Connect first** with the chosen MCP: `mcp_PostgreSQL{Env}_connect_db(...)` using arguments required by that MCP tool’s schema (see MCP descriptors under `mcps/user-PostgreSQL*/`).
2. **Then run queries:** `mcp_PostgreSQL{Env}_query(sql="SELECT ...")` or `mcp_PostgreSQL{Env}_execute(...)` for writes if allowed.
3. Reconnect if the session drops.

## Standard Patterns (Rule DB.2)

**Contracts by POD identifier:** Use DISTINCT, include `contract_type` (PRODUCT_CONTRACT / SERVICE_CONTRACT), contract and POD IDs, status, dates. Product contracts: `product_contract.contracts` → `contract_details` → `contract_pods` → `pod_details` → `pod.pod`. Service contracts: `service_contract.contracts` → `contract_details` → `contract_pods` → `pod.pod`. POD identifier is in `pod.pod.identifier` (varchar 33).

**Best practices:** DISTINCT on joins, order by contract_number and id, include status and date fields. See `database_workflow.mdc` for full SQL templates.

## Security (Rule DB.5)

- Never commit or log database credentials or passwords.
- Credentials live in **Cursor MCP server configuration** only.

## Rules Source

Environment mapping, connect-first workflow, and SQL templates: `.cursor/rules/integrations/database_workflow.mdc`. Rule 33: Test environment queries use PostgreSQLTest MCP.
