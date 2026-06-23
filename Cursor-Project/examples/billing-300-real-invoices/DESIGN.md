# Bulk billing — 300+ REAL invoices (Dev)

## Approach

| Item | Choice |
|------|--------|
| Environment | Dev (`http://10.236.20.11:8091`) |
| Application model | `FOR_VOLUMES` (settlement / profile) |
| Invoice count | 300 PODs → 300 invoices when `separateInvoiceForEachPod: true` on billing group |
| Contracts | 1 product contract (faster than 300 separate contracts) |
| Invoice status REAL | `start-billing` → DRAFT → `start-generating` → GENERATED → `start-accounting` → COMPLETED |

## Why one contract + separate invoice per POD?

- Creating 300 full contract chains via API is slow (hours).
- `volumesBigData.spec.ts` already uses many PODs on one contract; enabling `separateInvoiceForEachPod` splits one invoice per POD at generation time (see `ContractBillingGroup.separateInvoiceForEachPod` in Phoenix).

## Prerequisites

- VPN / network access to Dev API
- EnergoTS `global-setup` (token + `fixtures/envVariables.json`)
- Open accounting period in Dev (referenced via `envVariables.accounting_period`)

## Run

See `README.md` in this folder after the Playwright spec is added under `EnergoTS/tests/cursor/`.

## Estimated runtime

| Phase | Rough duration |
|-------|----------------|
| Shared catalog (term, PC, product) | ~1 min |
| 300 PODs (batched) | ~5–15 min |
| Contract + billing group | ~1 min |
| 300 activations + profile data | ~30–60 min |
| Billing run (300 invoices) | ~15–45 min |
| **Total** | **~1–2 hours** |

Use `TARGET_INVOICES=10` for a smoke run before full 300.
