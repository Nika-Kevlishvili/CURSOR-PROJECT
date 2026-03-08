# Invoice cancellation flow

Test cases for **invoice cancellation** when the invoice is paid and the payment package is locked or unlocked (Jira NT-1).

| File | Content |
|------|---------|
| **Paid_invoice_locked_package.md** | Invoice cancellation with paid invoice and locked/unlocked payment package; NT-1 reproduction; happy path; multiple payments; liability/receivable state. |
| **Payment_cancel_API_locked_package.md** | Direct payment cancel API when package is LOCKED vs UNLOCKED; mass import / PaymentService.cancel with locked package (regression). |

**Jira:** NT-1 – Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked.

**Entry points:** POST `/invoice-cancellation`, POST `/invoice-cancellation/upload-file`, POST payment cancel (`/payment/cancel`), InvoiceCancellationProcessService, PaymentService.cancel.
