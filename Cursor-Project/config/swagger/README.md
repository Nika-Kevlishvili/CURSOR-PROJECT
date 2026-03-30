# OpenAPI / Swagger by environment

Each subdirectory holds the **exported OpenAPI document** for that environment (same app, different `servers` / host).

## Bulk refresh (all environments)

**Source of truth for URLs:** `environments.json` in this folder (`swagger_ui` + `openapi_json` per environment).

Run from workspace root:

```powershell
.cursor\commands\update-swagger-specs.ps1
```

See also: **`.cursor/commands/update-swagger-specs.md`** (slash command / instructions).

## Swagger UI (canonical links)

| Folder | Swagger UI | Local spec file |
|--------|------------|-----------------|
| `dev/` | http://10.236.20.11:8091/swagger-ui/index.html# | `swagger-spec.json` |
| `test/` | http://10.236.20.31:8091/swagger-ui/index.html# | `swagger-spec.json` (export when needed) |
| `dev2/` | http://10.236.20.11:8092/swagger-ui/index.html# | `swagger-spec.json` (export when needed) |
| `prod/` | http://10.236.20.66:8090/swagger-ui/index.html#/ | `swagger-spec.json` (export when needed) |
| `preprod/` | _(URL not provided — add `SOURCE.md` when available)_ | — |

Details and typical JSON endpoints (`/v3/api-docs`) are in each folder’s **`SOURCE.md`**.

## How to refresh a spec

**All at once:** run **`update-swagger-specs.ps1`** (reads `environments.json`).

**One environment manually:**

1. Open the Swagger UI URL above, or fetch JSON from `http://<host>:<port>/v3/api-docs` (SpringDoc default).
2. Save the document as **`swagger-spec.json`** in the matching folder (overwrite).
3. If hosts change, update **`environments.json`** first, then **`SOURCE.md`** for that folder.

## Current inventory

- **dev, test, dev2, prod:** each folder has **`swagger-spec.json`**, fetched from **`/v3/api-docs`** on the host/port listed in that folder’s `SOURCE.md` (refresh by re-downloading the same URL).
- **preprod:** no Swagger URL on file yet — add `swagger-spec.json` when PreProd URL is known.

If you add PreProd or another environment: add an object to **`environments.json`**, create `config/swagger/<name>/` with **`SOURCE.md`**, then run **`update-swagger-specs.ps1`**.
