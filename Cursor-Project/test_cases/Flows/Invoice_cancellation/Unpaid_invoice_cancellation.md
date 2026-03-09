# Invoice Cancellation – Unpaid Invoice (Baseline)

**Jira:** NT-1 (AI Experiments) – related flow  
**Scope:** Invoice cancellation when the invoice has **not** been paid. Simplest baseline; no payment package involved.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **Invoice:** Generated and not yet paid; not yet cancelled.

---

## TC-1: Invoice cancellation when invoice is unpaid

**Objective:** Verify that invoice cancellation succeeds when the invoice has no payment (unpaid). No payment package is involved.

**Preconditions:**
1. Invoice has been generated.
2. Invoice has **not** been paid (no payment recorded for this invoice).

**Steps:**
1. Generate an invoice (or use existing test invoice that is unpaid).
2. Do **not** pay the invoice.
3. Create invoice cancellation (via UI or API).

**Expected result:** Invoice cancellation is created successfully. No error related to payment or payment package.

---

## TC-2: Regression – unpaid cancellation unchanged after NT-1 fix

**Objective:** After any fix for NT-1 (locked payment package), ensure cancellation of unpaid invoices still works.

**Preconditions:** Optionally after NT-1 fix deployed.

**Steps:**
1. Generate an unpaid invoice and create cancellation.
2. Verify success and that cancellation record is created.
3. (Optional) Call invoice cancellation API for an unpaid invoice and assert success.

**Expected result:** No regression; unpaid invoice cancellation continues to work.

---

## References

- **Jira:** NT-1 – baseline flow without payment.
- **Integration:** Invoice cancellation; no payment package dependency for unpaid case.
