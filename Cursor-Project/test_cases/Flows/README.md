# Flows – Flow-based test cases

In this folder you will find test cases **grouped by business or technical flow**.

Each subfolder is one flow. The folder name tells you what type of tests are inside:

| Subfolder | Content |
|-----------|--------|
| **Contract_termination/** | Contract termination flow: multi-version termination date, POD-based vs term-based termination, scheduler overwrite, Basic Parameter display, audit trail, test endpoints. |
| **Billing/** | Billing flow: reversal billing run, credit note summary/detailed data (price components, total volumes), export, manual credit/debit note, regenerate-compensations. |
| **Invoice_cancellation/** | Invoice cancellation (NT-1): unpaid baseline, paid with unlocked/locked payment package, payment cancel API regression. Files: `Unpaid_invoice_cancellation.md`, `Paid_invoice_unlocked_package.md`, `Locked_payment_package.md`, `Payment_cancel_API_locked_package.md`. |
| **Zero_amount_liability_receivable/** | Zero-amount liability/receivable prevention (PDT-2474): exhaustive coverage so no liability or receivable is generated with amount zero. Liability: Action, Deposit, Late payment fine, Rescheduling, Manual, Payment, Invoice (goods order, service order, billing run, disconnection), Invoice cancellation, Reversals (invoice, payment, manual liability offsetting). Receivable: Manual creation, Deposit, Invoice (credit notes, corrections, VAT base, compensations), Invoice cancellation, Payments, Reversals (Payment, LPF, Manual liability offsetting, Invoice, Rescheduling). Files: `Manual_creation.md`, `Billing_and_invoice_flows.md`, `Liability_Action_Deposit_Late_fine_Rescheduling.md`, `Liability_Payment_Invoice_flows.md`, `Liability_Invoice_cancellation_Reversals.md`, `Receivable_Deposit_Invoice_flows.md`, `Receivable_Payments_Reversals.md`. |

Inside each flow folder: one or more `.md` files per **scenario or variant** (e.g. `Multi_version_termination_date.md`, regression tests).
