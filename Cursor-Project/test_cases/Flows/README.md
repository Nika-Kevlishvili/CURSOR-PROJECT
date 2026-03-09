# Flows – Flow-based test cases

In this folder you will find test cases **grouped by business or technical flow**.

Each subfolder is one flow. The folder name tells you what type of tests are inside:

| Subfolder | Content |
|-----------|--------|
| **Contract_termination/** | Contract termination flow: multi-version termination date, POD-based vs term-based termination, scheduler overwrite, Basic Parameter display, audit trail, test endpoints. |
| **Billing/** | Billing flow: reversal billing run, credit note summary/detailed data (price components, total volumes), export, manual credit/debit note, regenerate-compensations. |
| **Invoice_cancellation/** | Invoice cancellation (NT-1): unpaid baseline, paid with unlocked payment package, paid with locked payment package. Scenario files: `Unpaid_invoice_cancellation.md`, `Paid_invoice_unlocked_package.md`, `Locked_payment_package.md`. |

Inside each flow folder: one or more `.md` files per **scenario or variant** (e.g. `Multi_version_termination_date.md`, regression tests).
