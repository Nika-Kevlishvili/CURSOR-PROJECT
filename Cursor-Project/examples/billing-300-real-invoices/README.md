# Bulk billing — 300+ REAL invoices (Dev)

Playwright API test that creates fresh Dev data and runs a **STANDARD_BILLING** / **FOR_VOLUMES** billing run until invoices are **REAL** (post-accounting), not DRAFT only.

## Prerequisites

- VPN / network access to Dev API (`http://10.236.20.11:8091`)
- EnergoTS dependencies installed (`npm install` in `Cursor-Project/EnergoTS`)
- Global setup completed (auth token + `fixtures/envVariables.json` with open `accounting_period`)

## Run

```powershell
cd Cursor-Project/EnergoTS
npx playwright test tests/cursor/billing-300-real-invoices.spec.ts --project=main --workers=1
# optional: $env:TARGET_INVOICES=300
# requires global setup / token (playwright runs setup project first or run global-setup)
```

### Smoke run (recommended first)

```powershell
$env:TARGET_INVOICES=10
npx playwright test tests/cursor/billing-300-real-invoices.spec.ts --project=main --workers=1
```

## What the test does

1. One legal customer, shared term / settlement price component / product
2. `TARGET_INVOICES` PODs (default 300), created in batches of 15
3. One product contract linking all PODs
4. Billing group updated with `separateInvoiceForEachPod: true`
5. POD activation + `billing-by-profile` (monthly, `timeZone: CET`) per POD
6. Billing run → `start-billing` → `start-generating` → `start-accounting` with paginated draft-invoice polling

## Estimated runtime

| Phase | Duration |
|-------|----------|
| Catalog + contract | ~2–5 min |
| 300 PODs + activations + profiles | ~30–60 min |
| Billing run (300 invoices) | ~15–45 min |
| **Total** | **~1–2 hours** (test timeout: 2 h) |

See `DESIGN.md` in this folder for architecture notes.
