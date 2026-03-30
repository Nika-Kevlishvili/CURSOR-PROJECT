# Swagger bulk-update command

**Date:** 2026-03-28

## Added

- **`Cursor-Project/config/swagger/environments.json`** — canonical **Swagger UI** + **`openapi_json`** (`/v3/api-docs`) URLs for dev, test, dev2, prod.
- **`.cursor/commands/update-swagger-specs.ps1`** — reads `environments.json`, runs `curl.exe` per env, writes `config/swagger/<id>/swagger-spec.json`.
- **`.cursor/commands/update-swagger-specs.md`** — usage, table of links, how to add PreProd.
- **`config/swagger/README.md`** — section **Bulk refresh** pointing to the script and manifest.

## Usage

```powershell
.cursor\commands\update-swagger-specs.ps1
```

Agents involved: None (direct tool usage)
