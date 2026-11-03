# Receivables Regression Coverage — Index (Process-only)

**Single source of truth:** [`Receivables_Regression_Cases.xlsx`](Receivables_Regression_Cases.xlsx)

## Scope
Same rule as the billing catalog. A row stays only when it asserts a receivable-process outcome: offset, payment posted or reversed, deposit liability or refund, late-payment fine, rescheduling, disconnection or reconnection, the reminder document, or a block that stops one of those.

Removed: object create/edit forms, listing, permissions, translations, interim-and-advance setup, and invoice correction/cancellation leaves (those stay in the billing catalog).

## Evidence
| EvidenceType | How we know |
|---|---|
| **Jira** | REG Test Case summary. Epic REG-56 is the process list. |
| **Confluence** | Outcome sentence from a Phase 2 Bundle 6 process page |
| **Diagram** | Decision or outcome step from Bundle 6 `.semantic.md` |
| **Playwright** | Asserting `test(...)` under `tests/receivableManagement` or a matching cursor leaf |

Jira source: REST search. Confluence source: REST fallback (`asterbit.atlassian.net/wiki`).

## Totals
- Rows: 213
- Jira leaves kept: 50 (from 218 keyword candidates)
- Automation Yes / Partial / No: 66 / 20 / 127
- Priority Highest/High/Medium/Low: 182 / 20 / 11 / 0
