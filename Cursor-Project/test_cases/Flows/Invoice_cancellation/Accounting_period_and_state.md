# Invoice Cancellation – Accounting Period and Invoice State Constraints (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Bug  
**Summary:** Cover boundary conditions where invoice cancellation may be restricted by accounting periods or invoice/payment state (closed period, partial payment, already reversed/cancelled), ensuring the NT-1 locked-package fix does not create invalid accounting behaviour.

**Scope:** NT-1 requires invoice cancellation to work for paid invoices even when the payment package is locked. However, accounting period rules and invoice-state rules may still legitimately block cancellation. This document verifies that cancellations are allowed only when the business rules permit them (including correct error messages when blocked), and that closed-period constraints are enforced consistently for both LOCKED and UNLOCKED package scenarios.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to create invoice cancellations.
- **Accounting periods:**
  - **Open period:** An accounting period that is open for posting.
  - **Closed period:** An accounting period that is closed for posting (no financial changes allowed).
- **Invoices and payments:**
  - **Invoice A (paid, open period):** Paid invoice in an open period.
  - **Invoice B (paid, closed period):** Paid invoice in a closed period (or a paid invoice dated in a closed accounting period).
  - **Invoice C (partially paid):** Invoice with partial payment recorded (if supported by the product).
  - **Invoice D (already cancelled/reversed):** Invoice that already has a cancellation or reversal.
- **Payment packages:**
  - For each paid invoice, the payment belongs to a payment package that can be set to **UNLOCKED** or **LOCKED** depending on the TC.

---

## TC-1 (Positive): Cancellation allowed in an OPEN accounting period (paid invoice, payment package LOCKED)

**Objective:** Confirm NT-1 main behaviour succeeds when accounting period rules allow cancellation.

**Preconditions:**
1. Invoice A is paid and belongs to an open accounting period.
2. Invoice A’s payment package lock status is **LOCKED**.

**Steps:**
1. Verify the accounting period for Invoice A is open.
2. Create invoice cancellation for Invoice A.

**Expected result:** Cancellation is created successfully. The payment package being LOCKED does not block cancellation in the open period.

---

## TC-2 (Negative): Cancellation rejected when accounting period is CLOSED (paid invoice, payment package LOCKED)

**Objective:** Ensure closed-period restrictions remain enforced even after allowing cancellation for LOCKED packages.

**Preconditions:**
1. Invoice B is paid and belongs to a closed accounting period.
2. Invoice B’s payment package lock status is **LOCKED**.

**Steps:**
1. Verify the accounting period for Invoice B is closed.
2. Attempt to create invoice cancellation for Invoice B.

**Expected result:** The system rejects the cancellation with a clear error indicating the accounting period is closed (or the invoice cannot be changed due to period closure). No cancellation is created.

---

## TC-3 (Negative): Cancellation rejected when accounting period is CLOSED (paid invoice, payment package UNLOCKED)

**Objective:** Ensure period-closure restriction is consistent and not dependent on lock status.

**Preconditions:**
1. Invoice B is paid and belongs to a closed accounting period.
2. Invoice B’s payment package lock status is **UNLOCKED**.

**Steps:**
1. Verify the accounting period for Invoice B is closed.
2. Attempt to create invoice cancellation for Invoice B.

**Expected result:** The cancellation is rejected for closed-period reasons, regardless of payment package lock status.

---

## TC-4 (Negative): Cancellation rejected for an invoice that is already cancelled (state restriction)

**Objective:** Ensure duplicate cancellations are prevented and the system returns a clear message.

**Preconditions:**
1. Invoice D already has an invoice cancellation (it is already cancelled).

**Steps:**
1. Attempt to create another invoice cancellation for Invoice D.

**Expected result:** The request is rejected or treated idempotently, but no second cancellation is created. The response clearly indicates the invoice is already cancelled.

---

## TC-5 (Negative): Cancellation behaviour for partially paid invoice (if supported) is correct and explicit

**Objective:** Ensure the system handles partial payment cases explicitly and does not create inconsistent financial records.

**Preconditions:**
1. Invoice C exists and is partially paid.
2. The payment package for the recorded payment is **LOCKED** (or UNLOCKED; run both if your rules differ).

**Steps:**
1. Attempt to create an invoice cancellation for Invoice C.

**Expected result:** The system either (a) allows cancellation and correctly handles the partial payment reversal/adjustment according to business rules, or (b) rejects the cancellation with a clear rule-based error explaining why partial payment prevents cancellation. In either case, there must be no “package must be UNLOCKED” style error for the invoice-cancellation flow.

---

## References

- **Jira:** NT-1 – cancellation blocked by lock restriction.
- **Regression risk (given):** accounting periods; partial failure behaviour.
