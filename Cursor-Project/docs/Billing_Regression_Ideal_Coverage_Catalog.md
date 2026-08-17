# Billing Regression Coverage — Index (Process-only)

**Single source of truth:** [`Billing_Regression_Cases.xlsx`](Billing_Regression_Cases.xlsx)

## Scope
**Only cases with direct effect on billing/invoices.** Removed:
- Object CRUD/setup (PC, POD, IAP, Discount fields, condition-builder UI)
- Listing/UI chrome, permissions, billing-run numbering
- Master-data checkboxes/tabs that do not assert invoice create/amount/status

Kept: start/generate billing, For volumes / pulling / overtime / per-piece (product) / electricity / interim invoice thresholds, discount-in-invoice calc, slot splitting, correction/reversal/cancellation, compensation link/PDF, RPS due date, connected invoices, Playwright billing leaves.

**Excluded:** Goods Order and Service Order.

**Automation:** EnergoTS **staging** working tree `src/tests/billing/**` (23 specs / 118 asserting tests indexed). Cursor leaves only when staging has no match.

Filtered out at generation: **521** rows (from 938).

## Evidence
| EvidenceType | How we know |
|--------------|-------------|
| **Jira** | REST issue summary for REG Test Case / feature key |
| **Confluence** | Verbatim should/must/if line from page body + page ID/URL |
| **Diagram** | Decision / Save text from Bundle 5 `.semantic.md` (process diagrams) |
| **Playwright** | Cursor/billing suite mapped by ticket id / file |

**AutomationStatus=Yes** only when an **asserting** `test(...)` title/id matches the CaseId.

## Priority (within each theme sheet)
Sorted **Highest → High → Medium → Low**; within same priority, **No** automation first.

| Priority | Meaning |
|----------|---------|
| **Highest** | Direct billing process (run/generate/correct/reverse/cancel/compensate/…) |
| **High** | Critical calculation / financial / due-date (in-process) |
| **Medium** | Process validation / error / edge |
| **Low** | Residual supporting (mostly filtered out) |

## Disclosures
- Jira source: REST fallback (MCP unavailable)
- Confluence source: REST fallback (`asterbit.atlassian.net/wiki`)

## Totals (at generation)
- Rows: 417
- Automation Yes / Partial / No: 212 / 43 / 162
- Priority Highest/High/Medium/Low: 355 / 32 / 9 / 21
- Unmapped gap rows: 162

Evidence working files: `docs/_billing_evidence/`
