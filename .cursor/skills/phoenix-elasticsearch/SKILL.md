---
name: phoenix-elasticsearch
description: Read Phoenix application logs from Elasticsearch with the official Elastic CLI. Use for bug-validation log analysis, log searches, error summaries, or any Elasticsearch question. Do not use Elasticsearch MCP.
---

# Phoenix Elasticsearch (official Elastic CLI)

Elasticsearch MCP servers are removed. They loaded large tool schemas into every chat. Log search goes through the official `@elastic/cli` binary, wrapped by `Cursor-Project/scripts/elastic-phoenix.py`, which applies the Phoenix environment filters and returns a short JSON payload.

**Chat line when logs were read:** `Elasticsearch source: official Elastic CLI (@elastic/cli via elastic-phoenix).`

## When to use

- Bug validation Step 4c (Rule 32)
- Any Phoenix question that needs application logs from Dev, Dev2, Test, PreProd, Test 2 ES, Test 2 SLR, or Prod

Do **not** call `ElasticsearchDev`, `ElasticsearchTest`, `ElasticsearchTest2ES`, `ElasticsearchTest2SLR`, `ElasticsearchProd`, or the old `es_*` MCP tools. A hook denies them.

## Command

```bash
python3 Cursor-Project/scripts/elastic-phoenix.py <command> --environment <env> [options]
```

```powershell
pwsh -NoProfile -File Cursor-Project/scripts/elastic-phoenix.ps1 <command> --environment <env> [options]
```

`<env>` is one of: `dev`, `dev2`, `test`, `preprod`, `test2es`, `test2slr`, `prod`. `experiments` exits 2 with `elasticsearch_investigation=not_available`.

The wrapper picks the cluster. Dev and Dev2 share `phoenix-dev`. Test, PreProd, Test 2 ES, and Test 2 SLR share `phoenix-test`. Prod uses `phoenix-prod`.

## Commands

| Command | Purpose |
|---|---|
| `error-summary` | Top ERROR groups by `logger_name`. First step in bug validation. |
| `search-logs` | Environment-filtered log lines. Primary evidence command. |
| `health` | Size-0 search proving the cluster is reachable. |
| `count` | Count matches for a Query DSL body. |
| `search` | Query DSL when `search-logs` cannot express the filter. |
| `sql` | Read-only Elasticsearch SQL (`SELECT`, `DESCRIBE`, `SHOW`). |
| `field-values` | Top terms for one field. |
| `indices` | List indices. Skip on Prod (API key returns 403). |
| `mapping` | Flattened fields for one index. Skip on Prod (403). |

### Bug validation sequence

```bash
python3 Cursor-Project/scripts/elastic-phoenix.py error-summary --environment test --search-text "invoice" --days-back 7 --top-n 10
python3 Cursor-Project/scripts/elastic-phoenix.py search-logs --environment test --search-text "NullPointerException" --level ERROR --days-back 7 --size 20
python3 Cursor-Project/scripts/elastic-phoenix.py search-logs --environment test --extra-filters '{"invoiceId":"123"}' --days-back 7
```

`--size` is capped at 50. Messages and stack traces are truncated in the wrapper so the chat stays small.

## Environment filters (applied by the wrapper)

| Environment | Context | Filter |
|---|---|---|
| `dev` | `phoenix-dev` | `app_name.keyword` in `phoenix`, `phoenix-scheduler`, `phoenix-billing-run`, `phoenix-mass-import`, `phoenix-sales-portal`, `phoenix-payment-api` |
| `dev2` | `phoenix-dev` | same names with a `2` suffix |
| `test` | `phoenix-test` | same primary names as dev |
| `preprod` | `phoenix-test` | same names with a `2` suffix |
| `test2es` | `phoenix-test` | `app_name.keyword=phoenix2` and `environment.keyword=test` |
| `test2slr` | `phoenix-test` | `app_name.keyword=phoenix2` and `environment.keyword=testi2` |
| `prod` | `phoenix-prod` | no `app_name` filter |

Rule ES.0a: a ticket URL containing `phoenix-test2/` selects `test2es`. `phoenix2-slr-test/` selects `test2slr`. Both markers → ask. This override is logs only.

## Clusters

| Context | URL | Auth |
|---|---|---|
| `phoenix-dev` | `https://10.236.20.13:9200` | `ELASTIC_DEV_API_KEY` (read-only) |
| `phoenix-test` | `http://10.236.20.27:9200` | none |
| `phoenix-prod` | `https://10.236.20.69:9200` | `ELASTIC_PROD_API_KEY` (read-only) |

Dev and Prod keys cannot call cluster health or list indices (403). `health` uses a size-0 search, which those keys allow. Config file: `Cursor-Project/config/elasticsearch/contexts.yml`. The CLI version pin is `8.19` because these clusters are Elasticsearch 8.19. The Elastic CLI is a technical preview documented for 9.x; search against 8.19 is what this wrapper uses.

## Credentials

Never print, commit, or paste API keys. The wrapper loads keys in this order: process environment, `Cursor-Project/config/elasticsearch/secrets.env` (gitignored; copy from `secrets.env.example`), then the macOS keychain service `elastic-cli`.

HTTPS clusters are called with TLS verification disabled inside the wrapper process only, matching the previous self-signed internal certificates.

## Read-only

The wrapper has no write commands. SQL containing `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `TRUNCATE`, `GRANT`, or `REVOKE` is rejected. A shell hook blocks `elastic` write subcommands and `elasticsearch_mcp_server.py`.

## Failure

Retry once on a connection error. Then return JSON `ok: false` and, in bug validation, set `elasticsearch_investigation=failed` or `not_available`. That does not block a verdict.
