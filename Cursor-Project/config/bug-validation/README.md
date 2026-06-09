# Bug validation automation helpers (Rule 32)

## Slack Path 1 — `#bug-validation`

| Method | When | Channel |
|--------|------|---------|
| **Slack MCP** (`slack_send_message`) | Cursor agent (preferred) | `C0AUEEDVCEL` (private) |
| **`send-bug-validation-to-slack.ps1`** | CI, script, bot invited to channel | `C0AUEEDVCEL` or fallback `#ai-report` |

```powershell
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/slack/send-bug-validation-to-slack.ps1" `
  -ReportMarkdownPath "<path-to-report.md>" `
  -IssueKey "PDT-2915"
```

**Private channel:** invite bot (`/invite @report`) or REST fallback returns `channel_not_found`.

## DB evidence — PDT-2915 pattern (Prod)

Read-only SQL: `queries/PDT-2915-invoice-correction-deleted-bbp.sql`

Replace `:billing_run_id` with the correction run id (e.g. `3400`), run via PostgreSQLProd MCP after `connect_db`.

**Bug signature (supports VALID):**

- `volume_change = true`, `price_change = false`
- `correction_billing_data_ids` maps `DELETED` `billing_by_profile` as `original = false`
- ACTIVE vs DELETED profile `SUM(value)` identical per POD (`zero_delta = true`)
- Ticket PODs in `correction_pods` with `full_reversal_needed = true` but no real volume change

## Playwright (TO-BE regression)

| File | Purpose |
|------|---------|
| `EnergoTS/tests/cursor/PDT-2915-invoice-correction-deleted-bbp.spec.ts` | TO-BE test (fails on unfixed Dev) |
| `EnergoTS/tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts` | Prechain, DELETE BBP, volume correction, draft assertions |

```bash
cd Cursor-Project/EnergoTS
npx playwright test tests/cursor/PDT-2915-invoice-correction-deleted-bbp.spec.ts --grep "PDT-2915" --project=main
```

Authoring: **energo-ts-test** only (Rule 0.8.1).

## Pattern catalog

`patterns/pdt-2915-invoice-correction-deleted-bbp.json` — for `production_bug_patterns.json` merge or bug-validator heuristics.

## Orchestrator

```powershell
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/bug-validation/run-pdt2915-automation.ps1" `
  -BillingRunId 3400 `
  -ReportMarkdownPath "<optional BugValidation md>"
```

DB only (psql; set `PDT2915_PG_HOST`, `PDT2915_PG_USER`, `PDT2915_PG_PASSWORD`, optional `PDT2915_PG_PORT`):

```powershell
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/bug-validation/run-pdt2915-db-evidence.ps1" -BillingRunId 3400
```
