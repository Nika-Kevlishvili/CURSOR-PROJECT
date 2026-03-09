# Invoice Cancellation – Paid Invoice with Unlocked Payment Package

**Jira:** NT-1 (AI Experiments) – related flow  
**Scope:** Invoice cancellation when the invoice is paid and the payment package is **unlocked**. Baseline / happy path to ensure cancellation works when lock status is UNLOCKED.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **Invoice:** Generated and not yet cancelled.
- **Payment:** Invoice has been paid.
- **Payment package:** The payment package for this payment is **unlocked** (lock status = UNLOCKED).

---

## TC-1: Invoice cancellation when paid and payment package is unlocked (happy path)

**Objective:** Verify that invoice cancellation succeeds when the invoice is paid and the payment package is unlocked. This is the baseline behaviour that should remain working.

**Preconditions:**
1. Invoice has been generated.
2. Invoice has been paid (payment recorded).
3. Payment package for this payment is **unlocked** (lock status UNLOCKED).

**Steps:**
1. Generate an invoice (or use existing test invoice).
2. Pay the invoice (record payment).
3. Ensure the payment package for this payment remains **unlocked** (do not lock it).
4. Create invoice cancellation (via UI or API).

**Expected result:** Invoice cancellation is created successfully. No error related to payment package or lock status.

---

## TC-2: Regression – cancellation still allowed when UNLOCKED

**Objective:** After any fix for NT-1 (locked package case), ensure cancellation when the payment package is unlocked still works and is not broken.

**Preconditions:** Same as TC-1; optionally after NT-1 fix deployed.

**Steps:**
1. Repeat TC-1 with an unpaid then paid invoice and unlocked payment package.
2. Verify cancellation request returns success and cancellation is persisted.
3. (Optional) Call invoice cancellation API with a paid invoice whose payment package is UNLOCKED and assert success.

**Expected result:** No regression; cancellation with unlocked payment package continues to work.

---

## References

- **Jira:** NT-1 – baseline behaviour when payment package is unlocked.
- **Integration:** Invoice cancellation; payment package lock status UNLOCKED.
