# Test 2 ES — Sales Portal OpenAPI source

**Local file:** `swagger-spec.json` in this folder.

**Swagger UI:** http://10.236.20.81:7095/swagger-ui/index.html

**OpenAPI JSON:** http://10.236.20.81:7095/v3/api-docs/sales-portal

**Also reachable as:** http://10.236.20.81:7095/v3/api-docs (same Sales Portal API document on this host)

**Observed swagger-config urls:** `[{"url":"/v3/api-docs/sales-portal","name":"sales-portal"}]`

**Payloads:** `payloads/` — one JSON file per POST/PUT body (plus query/path sidecars). Catalog: `payloads/INDEX.json`.

Payload JSON files under `payloads/` use live Test 2 ES object and nomenclature IDs (SELECT-only, 2026-08-21). Catalog and source objects: `payloads/INDEX.json`. OAuth client secrets are still placeholders. No POST/PUT requests were sent.
