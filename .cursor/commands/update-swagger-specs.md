# Update all Swagger / OpenAPI specs

Refreshes **`swagger-spec.json`** for every environment defined in **`Cursor-Project/config/swagger/environments.json`** by downloading **`openapi_json`** (SpringDoc: `/v3/api-docs`) into **`Cursor-Project/config/swagger/<id>/swagger-spec.json`**.

## Canonical URLs (also in `environments.json`)

| Environment | Swagger UI | OpenAPI JSON |
|---------------|------------|--------------|
| **dev** | http://10.236.20.11:8091/swagger-ui/index.html# | http://10.236.20.11:8091/v3/api-docs |
| **test** | http://10.236.20.31:8091/swagger-ui/index.html# | http://10.236.20.31:8091/v3/api-docs |
| **dev2** | http://10.236.20.11:8092/swagger-ui/index.html# | http://10.236.20.11:8092/v3/api-docs |
| **prod** | http://10.236.20.66:8090/swagger-ui/index.html#/ | http://10.236.20.66:8090/v3/api-docs |

**PreProd:** add an entry to `environments.json` when you have the URLs; the script will pick it up automatically.

## Usage

From the workspace root (or any directory):

```powershell
.cursor\commands\update-swagger-specs.ps1
```

Or in PowerShell with full path:

```powershell
& "d:\Cursor\cursor-project\.cursor\commands\update-swagger-specs.ps1"
```

## Requirements

- **curl.exe** (bundled on Windows 10+).
- Network reachability to the internal hosts above (VPN if needed).

## When to use

- After backend deployments when APIs change.
- Before regenerating Postman collections or comparing specs across environments.

## Changing URLs

1. Edit **`Cursor-Project/config/swagger/environments.json`** (`swagger_ui` for humans, `openapi_json` for fetch).
2. Optionally mirror the same links in **`config/swagger/<id>/SOURCE.md`** and **`config/swagger/README.md`** for documentation.
3. Run this script again.

## Related

- **`config/swagger/README.md`** — folder layout and inventory.
- **`config/swagger/*/SOURCE.md`** — per-environment notes.
