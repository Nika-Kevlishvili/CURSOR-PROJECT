# Payment Cancel API – Locked vs Unlocked Package (NT-1)

**Jira:** NT-1 – Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked  
**Entry points:** POST `/payment/cancel` (payment_id), PaymentService.cancel  
**Integration:** POST `/payment/cancel` with payment_id → UNLOCKED check; payment package lifecycle UNLOCKED → lock (LPF) → cancel requires UNLOCKED.  
**What could break:** POST /payment/cancel when LOCKED; LPF/lock semantics.

---

## Preconditions

- A payment exists and is linked to a payment package.
- The payment package has a lock status (UNLOCKED or LOCKED) that can be set or queried.

---

## TC-1: POST /payment/cancel with UNLOCKED package (success)

**Objective:** Verify that payment cancel succeeds when the payment’s package is UNLOCKED.

**Steps:**

1. Identify a payment whose payment package is **UNLOCKED** (e.g. query by payment_id and package lock status).
2. Call POST `/payment/cancel` with the payment identifier (e.g. `payment_id`).
3. Verify response (e.g. 200 or success payload).
4. Verify payment and related entities (e.g. liability/receivable, package) are updated as per cancel flow (e.g. status CANCELLED or equivalent).

**Expected result:** Cancel succeeds. No "Payment package not found with id … and lock status in UNLOCKED" error. Payment and related data reflect cancellation.

---

## TC-2: POST /payment/cancel with LOCKED package (current error – NT-1)

**Objective:** Reproduce the behaviour when cancel is requested for a payment whose package is LOCKED.

**Steps:**

1. Identify a payment whose payment package is **LOCKED** (e.g. locked via LPF or lock flow).
2. Call POST `/payment/cancel` with that payment identifier (e.g. `payment_id`).
3. Observe response and any error message.

**Expected result (current behaviour):** Request fails with error such as: *"Payment package not found with id &lt;package_id&gt; and lock status in UNLOCKED"* (or equivalent indicating cancel is not allowed for LOCKED package).

**Expected result (after fix / product decision):** Either cancel is allowed for LOCKED package with defined behaviour, or a clear, user-facing message explains that the package must be unlocked first.

**Technical note:** PaymentService.cancel() only allows UNLOCKED; no handling for LOCKED today.

---

## Regression: Mass import / Invoice cancellation upload

**Objective:** Ensure invoice cancellation via mass import (e.g. POST `/invoice-cancellation/upload-file`) is covered when some payments have locked packages.

**Steps:**

1. Prepare a file with invoice cancellation data where at least one invoice is paid and its payment package is LOCKED.
2. Call POST `/invoice-cancellation/upload-file` with the file.
3. Verify behaviour: partial success vs full failure, error messages, and that errors match the locked-package case (e.g. same "UNLOCKED" constraint message).

**Expected result:** Behaviour is consistent with single cancellation: either clear error for locked package or defined product rule (e.g. skip with warning, or fail row with message).
