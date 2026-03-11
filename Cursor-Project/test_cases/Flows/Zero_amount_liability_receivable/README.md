# Zero Amount Liability and Receivable (PDT-2474)

Test cases for the flow: **Liabilities and receivables must not be generated with amount zero.**

| File | Content |
|------|--------|
| **Manual_creation.md** | Manual liability and manual receivable creation via API: validation rejects zero amount (negative); valid non-zero amount succeeds (positive). |
| **Billing_and_invoice_flows.md** | Billing run, invoice cancellation, invoice reversal, credit notes/corrections: no zero-amount liability or receivable persisted; persistence layer rejects zero (defensive). |
