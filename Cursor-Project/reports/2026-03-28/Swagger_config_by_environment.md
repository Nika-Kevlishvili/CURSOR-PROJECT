# Swagger / OpenAPI layout under `config/swagger/`

**Date:** 2026-03-28

## What changed

- Added `Cursor-Project/config/swagger/` with per-environment folders: **dev**, **dev2**, **test**, **preprod**, **prod** (aligned with common Phoenix environment names in this workspace).
- Moved existing `config/swagger-spec.json` → **`config/swagger/dev/swagger-spec.json`** (export contained server `http://10.236.20.11:8091`).
- Root `config/swagger/README.md` describes usage; `dev/SOURCE.md` placeholder for the real Swagger URL; other envs have `README.md` until specs are added.

## Follow-up

When Swagger links are available for each environment, record them in each folder’s `SOURCE.md` and drop the exported JSON as `swagger-spec.json`.

Agents involved: None (direct tool usage)
