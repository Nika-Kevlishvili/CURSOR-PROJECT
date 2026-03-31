# PhoenixExpert — Swagger experiment environment

**Date:** 2026-03-31  
**Task:** Register Experiment environment OpenAPI/Swagger alongside dev, test, dev2, prod.

## Actions

- Created `Cursor-Project/config/swagger/experiment/SOURCE.md` with Swagger UI and `/v3/api-docs` URLs for `10.236.20.81:8094`.
- Added `experiment` entry to `config/swagger/environments.json` (`swagger_ui` + `openapi_json`).
- Updated `config/swagger/README.md` table and inventory bullet.
- Downloaded current OpenAPI document to `experiment/swagger-spec.json` from `http://10.236.20.81:8094/v3/api-docs` (fetch succeeded).

## Follow-up

- Re-run `.cursor/commands/update-swagger-specs.ps1` after backend changes to refresh all specs including experiment.
