# Invoice cancellation – paid invoice and locked payment package (NT-1)

**Jira:** NT-1  
**Summary:** Invoice cancellation – it is not possible to cancel an invoice if it is paid and the payment package is locked.  
**Cross-dependency:** cross_dependencies/2026-03-08_NT-1-invoice-cancellation-paid-locked-package.json

**Scope:** Invoice cancellation flow when the invoice has been paid (APO) and the payment's package has been locked. Current behaviour: cancellation fails with "Payment package not found with id X and lock status in UNLOCKED" because `PaymentService.cancel()` requires the package to be UNLOCKED.

**Code references:**
- `InvoiceCancellationService.processDebitNoteInvoice` (phoenix-core-lib) – iterates liabilities, calls `paymentService.cancel` for APO payments (lines 376–385).
- `PaymentService.cancel` (phoenix-core-lib) – `findPaymentPackageByIdAndLockStatusIn(..., UNLOCKED)` and throws `DomainEntityNotFoundException` if package is not UNLOCKED (lines 904–908).
- `PaymentPackageService.onlinePackageBlocker` – sets packages to LOCKED.

---

## Test data (preconditions)

- **Customer** with valid contract(s) and billing setup.
- **Invoice** that can be generated and then paid (e.g. via APO).
- **Payment** linked to a **payment package** that can be locked (e.g. by the package blocker scheduler or equivalent).
- Ability to **lock** the payment package (e.g. run package blocker so package lock status = LOCKED).

---

## TC-1: Invoice cancellation when invoice is paid and payment package is locked (NT-1 reproduction)

**Objective:** Reproduce NT-1 and verify expected behaviour after fix: invoice cancellation must be possible even when the payment package is locked.

**Preconditions:** Environment where invoices can be generated, paid, and payment packages can be locked (e.g. Test/Dev). No prior cancellation for the target invoice.

**Steps:**
1. Generate an invoice for the customer (via billing run or API as per project norms).
2. Pay the invoice (ensure payment is APO and linked to a payment package).
3. Lock the payment package for this payment (e.g. run package blocker job so the package's lock status is LOCKED, or use the mechanism that sets `PaymentPackageLockStatus.LOCKED`).
4. Create invoice cancellation (e.g. POST `/invoice-cancellation` with the invoice/cancellation request, or via Invoice cancellation UI).

**Expected result (current bug):**  
- API/UI returns error: `"Payment package not found with id <packageId> and lock status in UNLOCKED;"` (or equivalent).  
- Cancellation does not complete.

**Expected result (after fix):**  
- Invoice cancellation completes successfully (payment cancel or equivalent handling works for locked package, or package is temporarily unlocked / special path allows cancellation).  
- No error about payment package lock status.  
- Liability/receivable and payment state are consistent after cancellation.

**References:** InvoiceCancellationService.processDebitNoteInvoice → paymentService.cancel; PaymentService.cancel (UNLOCKED check).

---

## TC-2: Invoice cancellation when invoice is paid and payment package is unlocked (happy path)

**Objective:** Ensure standard invoice cancellation still works when the payment package is UNLOCKED (no regression).

**Preconditions:** Same as TC-1, but do **not** lock the payment package (package remains UNLOCKED).

**Steps:**
1. Generate an invoice for the customer.
2. Pay the invoice (APO, linked to payment package; package remains UNLOCKED).
3. Create invoice cancellation (POST `/invoice-cancellation` or UI).

**Expected result:**  
- Invoice cancellation completes successfully.  
- Payment cancel (or equivalent) is performed; liability/receivable and payment state are updated correctly.  
- No error related to payment package lock status.

**References:** processDebitNoteInvoice → paymentService.cancel (package UNLOCKED).

---

## TC-3: Invoice cancellation – multiple payments (one or more locked)

**Objective:** Cover case where the invoice is offset by more than one payment and at least one payment's package is locked.

**Preconditions:** Invoice paid with multiple payments (e.g. two APO payments); at least one payment's package is LOCKED, the other(s) may be UNLOCKED.

**Steps:**
1. Generate an invoice and pay it (e.g. two partial payments, each with its own package).
2. Lock the payment package for one of the payments.
3. Create invoice cancellation (POST `/invoice-cancellation` or UI).

**Expected result (current bug):**  
- Cancellation fails with payment package lock error when processing the locked package's payment.

**Expected result (after fix):**  
- Cancellation either succeeds for all linked payments (including when package is locked) or returns a clear, consistent behaviour (e.g. all-or-nothing with explicit error).  
- No silent partial cancellation inconsistent with business rules.

**References:** processDebitNoteInvoice loops over liabilities and payments; PaymentService.cancel per payment.

---

## TC-4: Liability and receivable state after cancellation (integration)

**Objective:** After a successful invoice cancellation (post-fix), verify liability, receivable, and payment state and that reporting/billing assumptions still hold.

**Preconditions:** Invoice cancelled successfully (e.g. TC-1 after fix or TC-2).

**Steps:**
1. Create invoice cancellation as in TC-1 or TC-2 and ensure it succeeds.
2. Verify in DB or via API: liability status/amounts, receivable transactions, and payment status (e.g. reversed) for the cancelled invoice and its payments.
3. (Optional) Run any billing or reporting jobs that depend on invoice/liability/receivable data and confirm no inconsistent or duplicate figures.

**Expected result:**  
- Liabilities and receivables reflect the cancellation; payments linked to the invoice are reversed or handled as per business rules.  
- No orphaned or inconsistent data; billing/reporting assumptions (from cross_dependency_data "what could break") still hold.

**References:** Integration points: liability-to-payment resolution; billing/reporting assumptions.

---

## Confluence / code references (summary)

| Topic | Reference |
|-------|-----------|
| Invoice cancellation flow | InvoiceCancellationService.processDebitNoteInvoice (phoenix-core-lib); calls paymentService.cancel for APO. |
| Payment cancel lock check | PaymentService.cancel (phoenix-core-lib): findPaymentPackageByIdAndLockStatusIn(UNLOCKED); DomainEntityNotFoundException message. |
| Package lock | PaymentPackageService.onlinePackageBlocker; PaymentPackageLockStatus.LOCKED/UNLOCKED. |
| Entry points | POST /invoice-cancellation; POST /invoice-cancellation/upload-file; InvoiceCancellationProcessService. |
