# Flows – Test cases grouped by flow

This folder contains test cases grouped **by business or technical flow**.  
Each subfolder below represents one flow; inside each folder you will find detailed `.md` files following `Cursor-Project/config/Test_case_template.md`.

| Flow folder | What it covers |
|------------|----------------|
| `Billing_run_termination/` | PDT-2023 – Billing run termination flow: terminate button behaviour, new status `IN_PROGRESS_TERMINATION`, schedulers, resume, concurrent operations, list and filters, and lock cleanup/ordering. |
| `POD_update_existing/` | PHN-2160 – Update existing POD flow: PUT update behaviour, UI edit flow, permissions and validation, concurrency and locking, and regression on list/filters, `/pod/{identifier}/exists`, and contract–POD flows. |
| `Invoice_cancellation/` | NT-1 – Invoice cancellation flow: cancelling invoices in unpaid/paid states, behaviour with locked payment packages, and regression around payment cancel API. |
| `Billing/` | Billing-related flows such as credit note behaviour after reversal (e.g. PDT-2585), including summary/detailed data and export/regeneration regressions. |

<<<<<<< Updated upstream
| Subfolder | Content |
|-----------|--------|
| **Contract_termination/** | Contract termination flow: multi-version termination date, POD-based vs term-based termination, scheduler overwrite, Basic Parameter display, audit trail, test endpoints. |
| **Billing/** | Billing flow: reversal billing run, credit note summary/detailed data (price components, total volumes), export, manual credit/debit note, regenerate-compensations. |
=======
For details of each flow, see the `README.md` file inside the corresponding subfolder.
>>>>>>> Stashed changes

