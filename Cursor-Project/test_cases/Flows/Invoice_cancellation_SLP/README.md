# Invoice_cancellation_SLP – Test cases (PDT-2655)

This flow contains test cases for **invoice cancellation when invoices were generated from an SLP (measured) profile** with two "измерено" (measured) price components in different slots. The billing run produces two invoices; cancellation must work correctly for both.

| File | Content |
|------|--------|
| **SLP_two_invoices_cancellation.md** | Main flow: cancel both SLP invoices (one request or sequential), only one in request, already cancelled, invalid identifier. Positive and negative. |
| **Request_validation_and_validInvoice.md** | Request validation and ValidInvoice: validInvoiceMap includes both SLP invoices; empty/invalid payload; already-cancelled and invalid state excluded; mixed SLP and non-SLP. |
| **Revert_behaviour_and_second_cancel.md** | Profile revert (delete vs setInvoiced): revert per slot; second cancel after first; double-revert and idempotency. |
| **Edge_cases_and_regression.md** | findInvoiceToCancel returns both; incomplete billing run; interim/debit/credit regression; mixed SLP and STANDARD; cancellation order; double-revert prevention; STANDARD profile regression. |

**Jira:** PDT-2655 – [Backend] Invoice Cancelation does not work correctly, when user cancel invoice which is generated from SLP profile.

**Entry points:** POST /invoice-cancellation; InvoiceCancellationProcessService; InvoiceCancellationService.processInvoice (STANDARD/SCALE); InvoiceCancellationInvoicesRepository.findInvoiceToCancel.
