---
name: phoenix-database
description: Runs PostgreSQL queries with the official psql client (psql-phoenix.py) on Dev, Dev2, Test, PreProd, Prod, Experiments, ES, or SLR. Ask environment before any call (DB.0a). Do not use PostgreSQL MCP.
---

# Phoenix Database Queries (official psql)

PostgreSQL MCP servers are removed. They loaded a tool schema into every chat. Queries go through the official `psql` client, wrapped by `Cursor-Project/scripts/psql-phoenix.py`.

**Chat line when the database was read:** `PostgreSQL source: official psql CLI (psql-phoenix).`

**Primary enforcement:** **rules** (`.cursor/rules/integrations/database_workflow.mdc` Rule DB.0 / DB.0a, `.cursor/rules/main/clarification_and_confidence.mdc` Rule CONF.0) + **this skill** + **database-query** agent (`.cursor/agents/database-query.md`). The agent MUST follow Step 0 in chat **before** any `psql-phoenix.py` call. Project hooks (`.cursor/hooks/*`) are a **secondary safety net only** — never skip Step 0 because hooks exist or might fail.

Passwords come from `Cursor-Project/config/postgres/secrets.env` (gitignored) or the process environment. Never embed or log them. Do not call `psql` yourself.

## When to Apply [MANDATORY]

Apply this skill **before** any live PostgreSQL read when:

- User asks for data from the database (reminder/RFD/POD/contract/liability/etc.).
- Task needs live DB evidence (IDs, links, “which X uses Y”).
- Parent workflow delegates database work (bug validation, cross-dep, production reader, Q&A).

Do **not** call `PostgreSQLDev`, `PostgreSQLDev2`, `PostgreSQLTest`, `PostgreSQLPreProd`, `PostgreSQLProd`, `PostgreSQLexperiments`, `PostgreSQLes`, `PostgreSQLSLR`, or `mcp-postgres-server`. A hook denies them.

## Step 0 — Environment gate (Rule DB.0a) [STOP — no psql until done]

**Order (non-negotiable):**

1. **Read** whether the **user** named an environment in the **current task** (`Dev`, `Dev2`, `Test`, `PreProd`, `Prod`, `Experiments`, or ES/SLR) or said `your choice` / `defaults` / `same as last time`.
2. If **missing** → **STOP**. Ask in chat (prefer **AskQuestion** with all six options: Dev, Dev2, Test, PreProd, Prod, Experiments). **Do not** grep/code-read as a substitute for DB env. **Do not** run `psql-phoenix.py`.
3. **Forbidden inference** (does NOT count as user naming env):
   - PDT / Jira ticket prefix (e.g. PDT-2529, PDT-2861)
   - EnergoTS test defaults (e.g. `PDT-2529` → reminder `2163` on **Dev**)
   - `envVariables`, Playwright branch, prior chat, or habit (“usually Test”)
   - **Rule 32 / bug-validator:** empty Jira `environment` + “code-only validation” — still ask before `psql-phoenix.py`; parent must not pre-pick `test` in Task prompt without user confirmation
4. Only after the user names env (or authorizes your choice) → map to `--environment` per the table below → run the wrapper.
5. State in chat: `DB environment: <Env>` before first query result.

**Violation of Step 0 is a BLOCK** (same as Rule CONF.0).

## Environment Selection (Rule DB.0)

Use the **exact** environment the user asks for. Do not switch.

| User says | `--environment` | Writes |
|-----------|-----------------|--------|
| Dev | `dev` | `execute --allow-write` (approval hook) |
| Dev2 | `dev2` | `execute --allow-write` (approval hook) |
| Test | `test` | `execute --allow-write` (approval hook) |
| PreProd | `preprod` | `execute --allow-write` (approval hook) |
| Prod | `prod` | Read-only |
| Experiments | `experiments` | `execute --allow-write` (approval hook) |
| ES | `es` | Read-only. Only when the user names ES |
| SLR | `slr` | Read-only. Only when the user names SLR |

`experiments` is database `experiments` on `10.236.20.85` (the server Cursor loaded as `PostgreSQLexperiments`). A removed workspace MCP entry named `PostgreSQLExperiment` pointed at database `postgres` with `readonly_user` and was not the live server.

## Command

```bash
python3 Cursor-Project/scripts/psql-phoenix.py <command> --environment <env> [options]
```

```powershell
pwsh -NoProfile -File Cursor-Project/scripts/psql-phoenix.ps1 <command> --environment <env> [options]
```

| Command | Purpose |
|---|---|
| `query --sql "..."` | Read-only SQL. `SELECT`, `WITH`, `EXPLAIN`, `SHOW`, `VALUES`, `TABLE`. Outer LIMIT default 200, max 500. |
| `list-schemas` | Non-system schemas. |
| `list-tables --schema <name>` | Tables in one schema, or all non-system tables when `--schema` is omitted. |
| `describe --table schema.name` | Columns from `information_schema.columns`. |
| `health` | `current_database()`, `current_user`, server port. |
| `check` | `psql` path and whether each password is set. Prints no secrets. |
| `execute --allow-write --sql "..."` | One write statement. Refused on `prod`, `es`, and `slr`. The shell hook asks before it runs. |

### Examples

```bash
python3 Cursor-Project/scripts/psql-phoenix.py health --environment test
python3 Cursor-Project/scripts/psql-phoenix.py query --environment dev --sql "SELECT id, contract_number FROM product_contract.contracts ORDER BY id DESC LIMIT 20"
python3 Cursor-Project/scripts/psql-phoenix.py describe --environment dev --table product_contract.contracts
```

The wrapper calls `psql -X -w -q -v ON_ERROR_STOP=1 --csv -c` (see the [psql documentation](https://www.postgresql.org/docs/current/app-psql.html)). `-X` ignores `~/.psqlrc`. `-w` never prompts. `-c` sends one command. Reads also set `PGOPTIONS=-c default_transaction_read_only=on`. Connection uses `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` so the password is not a process argument.

Output is one JSON object: `ok`, `columns`, `rows`, `row_count`. `read_only` is the environment policy (`prod`, `es`, and `slr` are true). `session_read_only` is true on `query` and the catalog commands because those sessions set `default_transaction_read_only=on`. Cell values are strings from CSV. Null is `[null]`.

## Workflow

1. Pass Step 0.
2. Run `query` (or `list-schemas` / `list-tables` / `describe`).
3. There is no session to reconnect. Run the command again if it fails once on a connection error, then report the JSON `error`.

## Standard Patterns (Rule DB.2)

### Product Contracts by POD

```sql
SELECT DISTINCT
  'PRODUCT_CONTRACT' as contract_type, c.id, c.contract_number, c.status, c.contract_status,
  c.create_date, c.entry_into_force_date, c.termination_date,
  p.identifier as pod_identifier, cp.id as contract_pod_id,
  cp.activation_date, cp.deactivation_date, cp.status as contract_pod_status
FROM product_contract.contracts c
JOIN product_contract.contract_details cd ON cd.contract_id = c.id
JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id
JOIN pod.pod_details pd ON cp.pod_detail_id = pd.id
JOIN pod.pod p ON pd.pod_id = p.id
WHERE p.identifier = 'POD_IDENTIFIER_HERE'
ORDER BY c.contract_number, c.id
```

### Service Contracts by POD

```sql
SELECT DISTINCT
  'SERVICE_CONTRACT' as contract_type, c.id, c.contract_number, c.status, c.contract_status,
  c.create_date, c.entry_into_force_date, c.termination_date,
  p.identifier as pod_identifier, cp.id as contract_pod_id, cp.status as contract_pod_status
FROM service_contract.contracts c
JOIN service_contract.contract_details cd ON cd.contract_id = c.id
JOIN service_contract.contract_pods cp ON cp.contract_detail_id = cd.id
JOIN pod.pod p ON cp.pod_id = p.id
WHERE p.identifier = 'POD_IDENTIFIER_HERE'
ORDER BY c.contract_number, c.id
```

### Key schemas

- Product: `contracts` → `contract_details` → `contract_pods` → `pod_details` → `pod.pod`
- Service: `contracts` → `contract_details` → `contract_pods` → `pod.pod` (direct pod_id)
- POD identifier: `pod.pod.identifier` (varchar 33)

**Best practices:** DISTINCT on joins; include contract_type; order by contract_number; include status + date fields; LIMIT result sets; schema prefixes.

## Security (Rule DB.5)

- Never commit or log database credentials or passwords.
- Passwords live in **`Cursor-Project/config/postgres/secrets.env`** (gitignored; copy from `secrets.env.example`) or in the process environment.
- `prod`, `es`, and `slr` reject `execute`. Other environments accept one write statement only with `--allow-write`, and the shell hook asks first.
- Do not put a `postgres://` URL with a password in the shell.

## Rules Source

Environment mapping: `.cursor/rules/integrations/database_workflow.mdc` (summary). SQL templates now live here in this SKILL. Rule 33: Test environment queries use `psql-phoenix.py --environment test`.
