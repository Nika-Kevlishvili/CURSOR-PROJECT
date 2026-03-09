# Flows – Flow-based test cases

In this folder you will find test cases **grouped by business or technical flow**.

Each subfolder is one flow. The folder name tells you what type of tests are inside:

| Subfolder | Content |
|-----------|--------|
| **Contract_termination/** | Contract termination flow: multi-version termination date, POD-based vs term-based termination, scheduler overwrite, Basic Parameter display, audit trail, test endpoints. |
| **Billing/** | Billing flow: reversal billing run, credit note summary/detailed data (price components, total volumes), export, manual credit/debit note, regenerate-compensations. |
| **Invoice_cancellation/** | Invoice cancellation when invoice is paid and payment package is locked/unlocked (NT-1): reproduction, happy path, payment cancel API, mass import. Scenario files: `Paid_invoice_locked_package.md`, `Payment_cancel_API_locked_package.md`. |

Inside each flow folder: one or more `.md` files per **scenario or variant** (e.g. `Multi_version_termination_date.md`, regression tests).
