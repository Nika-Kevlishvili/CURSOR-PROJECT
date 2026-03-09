# Invoice_cancellation – Flow test cases

Invoice cancellation flow: cancel invoice in different states (unpaid, paid with unlocked payment package, paid with locked payment package).  
**Jira:** NT-1 (AI Experiments) – Invoice cancellation when paid and payment package is locked.

| File | Content |
|------|--------|
| **Unpaid_invoice_cancellation.md** | Cancel invoice when it is not paid (baseline; no payment package). |
| **Paid_invoice_unlocked_package.md** | Cancel invoice when paid and payment package is unlocked (happy path). |
| **Locked_payment_package.md** | Cancel invoice when paid and payment package is **locked** (NT-1 main scenario; current bug: error "Payment package not found with id X and lock status in UNLOCKED"; expected: cancellation allowed). |
