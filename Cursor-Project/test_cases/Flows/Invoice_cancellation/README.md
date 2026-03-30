# Invoice_cancellation – Flow test cases

Invoice cancellation flow: cancel invoice in different states (unpaid, paid with unlocked payment package, paid with locked payment package).  
**Jira:** NT-1 (AI Experiments) – Invoice cancellation when paid and payment package is locked.

| File | Content |
|------|--------|
| **Unpaid_invoice_cancellation.md** | Cancel invoice when it is not paid (baseline; no payment package). |
| **Paid_invoice_unlocked_package.md** | Cancel invoice when paid and payment package is unlocked (happy path). |
| **Locked_payment_package.md** | Cancel invoice when paid and payment package is **locked** (NT-1 main scenario; current bug: error "Payment package not found with id X and lock status in UNLOCKED"; expected: cancellation allowed). Entry points: POST /invoice-cancellation, POST /payment/cancel; InvoiceCancellationService.processDebitNoteInvoice; PaymentService.cancel. |
| **Payment_cancel_API_locked_package.md** | Regression: POST /payment/cancel when payment package is locked; invoice cancellation vs payment cancel API (what_could_break). |
| **Validation_and_permissions.md** | Negative-heavy validation and access control coverage for invoice cancellation requests (missing/wrong identifiers, forbidden user, duplicate submissions). |
| **Accounting_period_and_state.md** | Accounting period closed/open constraints, and invoice/payment state boundaries (already cancelled, partial payment) to prevent invalid accounting behaviour. |
| **Async_processing_and_documents.md** | Resilience and integration-point coverage: process/notification, retries, idempotency/concurrency, and document generation failure handling. |
