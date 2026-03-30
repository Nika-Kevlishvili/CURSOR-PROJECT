# Invoice Cancellation – Validation and Permissions (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Bug  
**Summary:** Validate request payloads and permission checks for invoice cancellation, including negative cases (missing/wrong identifiers, forbidden user) to ensure the NT-1 change does not weaken input validation or access control.

**Scope:** This document covers input validation and permission rules around creating invoice cancellations. It is intentionally heavy on negative scenarios to ensure the system rejects invalid or unauthorized requests with clear errors and without creating partial data. It also includes a couple of positive “smoke” checks to confirm the endpoint still functions for a permitted user with valid input.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **Users:**
  - **Permitted user:** Has permission to create invoice cancellations.
  - **Forbidden user:** Authenticates successfully but does not have permission to create invoice cancellations.
- **Invoices:**
  - **Invoice A:** Exists, generated, not cancelled (use unpaid or paid as needed by each TC).
  - **Invoice B:** Does not exist (non-existent identifier).

---

## TC-1 (Positive): Permitted user creates an invoice cancellation with valid invoice identifier

**Objective:** Confirm that the “happy path” is still possible for a permitted user and valid input (baseline to anchor validation tests).

**Preconditions:**
1. A permitted user exists.
2. Invoice A exists and is not cancelled.

**Steps:**
1. Authenticate as the permitted user.
2. Create an invoice cancellation for Invoice A (via UI or `POST /invoice-cancellation`).
3. Verify the cancellation is created.

**Expected result:** The request succeeds and an invoice cancellation is created for Invoice A.

---

## TC-2 (Negative): Forbidden user cannot create invoice cancellation (permission denied)

**Objective:** Ensure the system enforces access control and does not allow unauthorized cancellation creation.

**Preconditions:**
1. A forbidden user exists (authenticated but lacks the required permission).
2. Invoice A exists and is not cancelled.

**Steps:**
1. Authenticate as the forbidden user.
2. Attempt to create an invoice cancellation for Invoice A.

**Expected result:** The request is rejected (e.g. 403 Forbidden or equivalent). No cancellation is created and the invoice state remains unchanged.

---

## TC-3 (Negative): Missing invoice identifier is rejected (required field validation)

**Objective:** Ensure the API/UI flow validates required identifiers and does not create cancellations for empty payloads.

**Preconditions:**
1. A permitted user exists.

**Steps:**
1. Authenticate as the permitted user.
2. Call the invoice cancellation flow with a missing invoice identifier (e.g. null/empty invoice number, empty list of invoice numbers).
3. Verify response and persistence.

**Expected result:** The request is rejected with a clear validation error describing the missing field(s). No cancellation is created.

---

## TC-4 (Negative): Non-existent invoice identifier is rejected (not found)

**Objective:** Ensure the system rejects cancellation attempts for invoices that do not exist.

**Preconditions:**
1. A permitted user exists.
2. Invoice B does not exist (use a guaranteed-non-existent identifier).

**Steps:**
1. Authenticate as the permitted user.
2. Attempt to create an invoice cancellation for Invoice B.

**Expected result:** The request is rejected with a clear “invoice not found” error. No cancellation is created.

---

## TC-5 (Negative): Malformed invoice identifier is rejected (format validation)

**Objective:** Ensure invoice identifiers are validated for format/length and do not produce unclear internal errors.

**Preconditions:**
1. A permitted user exists.

**Steps:**
1. Authenticate as the permitted user.
2. Attempt to create invoice cancellation using a malformed invoice identifier (e.g. illegal characters, overly long string, wrong pattern).

**Expected result:** The request is rejected with a clear validation error. No cancellation is created.

---

## TC-6 (Negative): Duplicate cancellation request for the same invoice is rejected or treated idempotently (no double-cancel)

**Objective:** Ensure repeated submissions do not create duplicate cancellations and do not corrupt invoice state.

**Preconditions:**
1. A permitted user exists.
2. Invoice A exists and is not cancelled.

**Steps:**
1. Create an invoice cancellation for Invoice A and verify it is created.
2. Submit the same invoice cancellation request again (same payload).
3. Verify system response and persistence.

**Expected result:** The second request does not create a duplicate cancellation. The system either (a) responds with an idempotent success indicating the existing cancellation, or (b) rejects with a clear “already cancelled/already has cancellation” error.

---

## References

- **Jira:** NT-1 – Invoice cancellation blocked for paid invoices with locked payment package.
- **Entry point:** `POST /invoice-cancellation` (as referenced in cross-dependency data).
