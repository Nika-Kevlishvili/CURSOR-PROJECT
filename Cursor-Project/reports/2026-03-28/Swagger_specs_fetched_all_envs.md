# Fetched `swagger-spec.json` for all environments with URLs

**Date:** 2026-03-28

## Actions

Downloaded OpenAPI 3 JSON from **`/v3/api-docs`** (SpringDoc) into:

| Folder | Source URL | Result |
|--------|------------|--------|
| `config/swagger/dev/` | http://10.236.20.11:8091/v3/api-docs | OK (~1.38 MB) — **Dev refreshed** |
| `config/swagger/test/` | http://10.236.20.31:8091/v3/api-docs | OK (~1.38 MB) |
| `config/swagger/dev2/` | http://10.236.20.11:8092/v3/api-docs | OK (~1.51 MB) |
| `config/swagger/prod/` | http://10.236.20.66:8090/v3/api-docs | OK (~1.38 MB) |

**PreProd:** not fetched (no Swagger base URL recorded under `config/swagger/preprod/`).

Implementation: Python `urllib` with 120s timeout; JSON re-serialized to `swagger-spec.json`.

Agents involved: None (direct tool usage)
