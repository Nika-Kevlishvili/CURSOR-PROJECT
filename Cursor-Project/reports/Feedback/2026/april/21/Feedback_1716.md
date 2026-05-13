# Feedback — Session notes

**Date:** 2026-04-21
**User sentiment:** Other

## Optional detail

Structured sentiment picker was skipped; user sentiment was not explicitly selected. Recorded as **Other** so session notes could still be saved per `/feedback`.

## Session summary

- **Prod billing 3060 + interim value types:** User asked (Georgian) to locate billing `3060` on production and list interim **Value Type** values used on products across contracts in that billing, and to explain meanings of “% from previous invoice amount”, “exact amount”, and other types. Outcome: PostgreSQL Prod queries on `billing.billings`, joins to `product_contract` and `product.product_interim_advance_payments` / `interim_advance_payment.interim_advance_payments`; enum `iap_value_type` documented (`PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`, `EXACT_AMOUNT`, `PRICE_COMPONENT`). For this billing’s contract list, active product-linked interims were **only** `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT` with `value` 50; Confluence page “Flows and Logics of interim and advance payments” cited for business definitions.
- **Follow-up — products’ Value Type:** User asked again what Value Type is configured on those products’ interims. Outcome: same answer reiterated—only `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT` for active product interims in scope; no `EXACT_AMOUNT` or `PRICE_COMPONENT` in that selection.

## Confidence

Assistant stated **92%** and **90%** confidence on Prod-backed answers, with short caveats about “previous invoice” resolution depending on `deduction_from` / billing-run rules. Overall assessment: **high (inferred)** for factual DB enum and row counts; **medium–high** for full business edge cases without deeper billing-run trace.

---

Agents involved: None (direct tool usage)
