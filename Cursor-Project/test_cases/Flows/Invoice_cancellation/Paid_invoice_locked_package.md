# Invoice Cancellation – Paid Invoice and Locked/Unlocked Payment Package (NT-1)

**Jira:** NT-1 – Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked  
**Entry points:** POST `/invoice-cancellation`, POST `/invoice-cancellation/upload-file`, InvoiceCancellationProcessService, InvoiceCancellationService.processDebitNoteInvoice → PaymentService.cancel  
**Integration:** Invoice cancellation create → Process → process debit note invoices → for each liability with APO → PaymentService.cancel; payment package lifecycle UNLOCKED → lock (LPF) → cancel requires UNLOCKED.

---

## Preconditions (shared)

- Customer with invoice(s) and payment(s) linked via liabilities (APO – Advance Payment Offset).
- Payment package exists for the payment(s) and can be locked (e.g. via LPF / lock flow).

---

## TC-1: Invoice cancellation when invoice is paid and payment package is LOCKED (current error – NT-1)

**Objective:** Reproduce the bug: creating an invoice cancellation fails when the invoice is paid and the payment package is locked, with error "Payment package not found with id … and lock status in UNLOCKED".

**Steps:**

1. Generate an invoice for the customer (ensure it is in a state that allows cancellation).
2. Pay the invoice (create payment and link via APO so the invoice is considered paid).
3. Lock the payment package for this payment (e.g. via LPF or lock API so package lock status = LOCKED).
4. Create invoice cancellation (e.g. POST `/invoice-cancellation` with the invoice identifier or via UI).

**Expected result (current bug):** Request fails with error such as: *"Payment package not found with id &lt;package_id&gt; and lock status in UNLOCKED"*. Cancellation does not complete.

**Expected result (after fix):** It must be possible to cancel the invoice (either cancellation succeeds with locked package, or a clear business rule is applied and documented).

**Technical note:** PaymentService.cancel() currently allows only UNLOCKED packages; processDebitNoteInvoice calls paymentService.cancel for each APO payment with no handling for locked package.

---

## TC-2: Invoice cancellation when payment package is UNLOCKED (happy path)

**Objective:** Verify that invoice cancellation succeeds when the invoice is paid and the payment package is UNLOCKED.

**Steps:**

1. Generate an invoice for the customer.
2. Pay the invoice (payment linked via APO).
3. Ensure the payment package for this payment remains **UNLOCKED** (do not lock it).
4. Create invoice cancellation (POST `/invoice-cancellation` or equivalent with the invoice identifier).

**Expected result:** Invoice cancellation completes successfully. Debit note / reversal flow runs; PaymentService.cancel is called and succeeds; liability/receivable and payment states are updated as per business rules.

**API / flow:** Invoice cancellation create → Process → process debit note invoices → for each liability with APO → PaymentService.cancel (package UNLOCKED → cancel allowed).
