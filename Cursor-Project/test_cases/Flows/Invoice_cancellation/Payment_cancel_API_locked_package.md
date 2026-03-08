# Payment cancel API – locked payment package (NT-1 regression)

**Jira:** NT-1 (regression / integration)  
**Cross-dependency:** cross_dependencies/2026-03-08_NT-1-invoice-cancellation-paid-locked-package.json

**Scope:** Direct payment cancel API (POST payment cancel) and any flow that calls `PaymentService.cancel` when the payment package is LOCKED. Ensures that a fix for NT-1 does not break direct cancel behaviour and that behaviour is consistent between invoice cancellation and direct cancel.

**Code references:**
- `PaymentController` (phoenix-core) – exposes payment cancel API; calls `paymentService.cancel(request)` (e.g. line 167).
- `PaymentService.cancel` (phoenix-core-lib) – requires payment package lock status UNLOCKED (lines 904–908).
- Mass import / other callers may use `PaymentService.cancel`.

---

## Test data (preconditions)

- **Payment** in status that allows cancel (not already reversed), linked to a **payment package**.
- Ability to set the payment package to **LOCKED** (e.g. package blocker job or test data).

---

## TC-1: Direct payment cancel API when package is LOCKED (current behaviour)

**Objective:** Document and verify current behaviour of the direct payment cancel API when the payment's package is LOCKED.

**Preconditions:** One payment linked to a payment package; package lock status = LOCKED.

**Steps:**
1. Ensure the payment package for the target payment is LOCKED (e.g. run package blocker or set lock status in test data).
2. Call POST payment cancel API (e.g. `PaymentController` endpoint) with the payment ID (and any required body/params).
3. Observe response status and body.

**Expected result (current):**  
- API returns error (e.g. 404 or 4xx) with message containing "Payment package not found" and "lock status in UNLOCKED" (or equivalent).  
- Payment is **not** cancelled.

**Expected result (after NT-1 fix, if direct cancel is also relaxed):**  
- If product decision is to allow cancel when package is locked: request succeeds and payment is cancelled.  
- If product decision is to keep strict UNLOCKED check for direct API: same as current (error).  
- Behaviour must be documented and consistent with invoice cancellation.

**References:** PaymentController → paymentService.cancel; PaymentService.cancel (UNLOCKED check).

---

## TC-2: Direct payment cancel API when package is UNLOCKED (regression)

**Objective:** Ensure direct payment cancel API still works when the payment package is UNLOCKED (no regression).

**Preconditions:** One payment linked to a payment package; package lock status = UNLOCKED.

**Steps:**
1. Call POST payment cancel API with the payment ID (and required request body).
2. Verify response is success (e.g. 200) and payment is cancelled (e.g. status REVERSED).
3. Verify liability/receivable and related data are updated as per business rules.

**Expected result:**  
- Cancel succeeds; payment reversed; related offsets/receivables/liabilities updated correctly.  
- No regression after any NT-1 change.

**References:** PaymentController; PaymentService.cancel (happy path).

---

## TC-3: Mass import / PaymentService.cancel with locked package

**Objective:** Cover flows that call `PaymentService.cancel` outside invoice cancellation (e.g. mass import). If NT-1 fix only affects invoice cancellation path, mass import may still fail when package is locked.

**Preconditions:** Mass import or other process that triggers payment cancel; payment(s) with LOCKED package.

**Steps:**
1. Prepare input (e.g. file or API) that triggers payment cancel for a payment whose package is LOCKED.
2. Run the mass import or process (e.g. upload file or invoke batch).
3. Check response/report: success vs error and error message.

**Expected result (current):**  
- Process fails or reports error for the payment(s) with locked package; error message references payment package lock status.

**Expected result (after fix):**  
- Per product decision: either cancel is allowed for locked package in this flow too, or error remains but is consistent and documented.  
- No silent partial success that leaves data inconsistent.

**References:** cross_dependency_data "what could break": Mass import / PaymentService.cancel; AbstractTxtMassImportProcessService or other callers of PaymentService.cancel.

---

## Confluence / code references (summary)

| Topic | Reference |
|-------|-----------|
| Payment cancel API | PaymentController (phoenix-core) – POST payment cancel; paymentService.cancel(request). |
| PaymentService.cancel | phoenix-core-lib PaymentService.cancel – UNLOCKED check; DomainEntityNotFoundException. |
| Integration | Invoice cancellation uses same PaymentService.cancel; direct cancel API and mass import are other callers. |
