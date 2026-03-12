# Manual Liability and Receivable Creation – Zero Amount Rejected (PDT-2474)

**Jira:** PDT-2474 (Phoenix)  
**Type:** Task / Customer Feedback  
**Summary:** Every flow that generates liabilities and receivables must not create records with amount zero. This document covers manual creation via API: the system must reject requests with initial amount zero and accept valid amounts.

**Scope:** Manual liability creation and manual receivable creation are entry points that persist CustomerLiability and CustomerReceivable. The API must validate initialAmount and reject zero. When a valid non-zero amount is sent, the record must be created successfully. Integration point: Manual liability/receivable REST API; validation (e.g. request DTO or JPA @PrePersist) must prevent zero from being persisted.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Customer:** A customer exists and is active (required to link the liability or receivable).
- **API access:** The user (or system) has access to the manual liability creation endpoint and the manual receivable creation endpoint (e.g. POST with payload containing initialAmount, customer reference, currency, etc.).
- **Permissions:** The caller has sufficient permissions to create manual liabilities and manual receivables.

---

## TC-1 (Negative): Manual liability creation rejected when initial amount is zero

**Objective:** Verify that when the user sends a request to create a manual liability with initialAmount equal to zero, the system rejects the request and does not persist any CustomerLiability with amount zero. The response must indicate a validation error (e.g. 400 Bad Request) with a clear message.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual liability creation API (e.g. POST endpoint for manual liability).
3. The request payload supports a field for initial amount (e.g. initialAmount or equivalent).

**Steps:**
1. Prepare a request payload for manual liability creation with all required fields (customer identifier, currency, etc.) and set the initial amount to zero (0 or 0.00).
2. Send the request to the manual liability creation endpoint (e.g. POST).
3. Observe the HTTP status code and response body.

**Expected result:** The system returns a validation error (e.g. 400 Bad Request). The response body contains a clear message that the initial amount must not be zero (or equivalent). No CustomerLiability record with initialAmount zero is created in the database.

**References:** PDT-2474; Manual liability API; ZeroAmountValidationListener or equivalent validation; integration point: Manual liability REST API.

---

## TC-2 (Negative): Manual receivable creation rejected when initial amount is zero

**Objective:** Verify that when the user sends a request to create a manual receivable with initialAmount equal to zero, the system rejects the request and does not persist any CustomerReceivable with amount zero. The response must indicate a validation error (e.g. 400 Bad Request) with a clear message.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual receivable creation API (e.g. POST endpoint for manual receivable).
3. The request payload supports a field for initial amount (e.g. initialAmount or equivalent).

**Steps:**
1. Prepare a request payload for manual receivable creation with all required fields (customer identifier, currency, etc.) and set the initial amount to zero (0 or 0.00).
2. Send the request to the manual receivable creation endpoint (e.g. POST).
3. Observe the HTTP status code and response body.

**Expected result:** The system returns a validation error (e.g. 400 Bad Request). The response body contains a clear message that the initial amount must not be zero (or equivalent). No CustomerReceivable record with initialAmount zero is created in the database.

**References:** PDT-2474; Manual receivable API; validation rules; integration point: Manual receivable REST API.

---

## TC-3 (Positive): Manual liability creation succeeds when initial amount is non-zero

**Objective:** Verify that when the user sends a valid request to create a manual liability with a non-zero initial amount, the system accepts the request and persists the CustomerLiability with the given amount. This confirms that the flow is not incorrectly blocking all creations, only zero amounts.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual liability creation API.
3. A valid non-zero amount is chosen (e.g. 10.00 in the contract currency).

**Steps:**
1. Prepare a request payload for manual liability creation with all required fields and set the initial amount to a valid non-zero value (e.g. 10.00).
2. Send the request to the manual liability creation endpoint.
3. Verify the response indicates success (e.g. 201 Created or 200 OK) and that a liability record exists with the same amount.

**Expected result:** The system creates the manual liability successfully. The response indicates success and the created CustomerLiability has initialAmount equal to the sent value (e.g. 10.00). No validation error is returned.

**References:** PDT-2474; Manual liability API; happy path for manual liability creation.

---

## TC-4 (Positive): Manual receivable creation succeeds when initial amount is non-zero

**Objective:** Verify that when the user sends a valid request to create a manual receivable with a non-zero initial amount, the system accepts the request and persists the CustomerReceivable with the given amount. This confirms that the flow is not incorrectly blocking all creations, only zero amounts.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual receivable creation API.
3. A valid non-zero amount is chosen (e.g. 25.50 in the contract currency).

**Steps:**
1. Prepare a request payload for manual receivable creation with all required fields and set the initial amount to a valid non-zero value (e.g. 25.50).
2. Send the request to the manual receivable creation endpoint.
3. Verify the response indicates success (e.g. 201 Created or 200 OK) and that a receivable record exists with the same amount.

**Expected result:** The system creates the manual receivable successfully. The response indicates success and the created CustomerReceivable has initialAmount equal to the sent value (e.g. 25.50). No validation error is returned.

**References:** PDT-2474; Manual receivable API; happy path for manual receivable creation.

---

## TC-5 (Negative): Manual liability creation rejected when initial amount is negative

**Objective:** Verify that when the user sends a request to create a manual liability with a negative initial amount, the system rejects the request. CustomerLiabilityRequest uses @DecimalMin(0, inclusive=false), so zero and negative values must be rejected.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual liability creation API.

**Steps:**
1. Prepare a request payload for manual liability creation with initial amount set to a negative value (e.g. -10.00).
2. Send the request to the manual liability creation endpoint.
3. Observe the HTTP status code and response body.

**Expected result:** The system returns a validation error (e.g. 400 Bad Request). No CustomerLiability record is created. The response indicates that the initial amount must be greater than zero (or equivalent).

**References:** PDT-2474; CustomerLiabilityRequest validation; @DecimalMin(0, inclusive=false).

---

## TC-6 (Positive): Manual liability creation succeeds with minimum positive amount (boundary)

**Objective:** Verify that the system accepts the smallest positive amount (e.g. 0.01 or the smallest supported decimal) for manual liability creation. This confirms that the validation rejects only zero (and negative), not all small amounts.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual liability creation API.
3. The system supports a small positive amount (e.g. 0.01 in the contract currency).

**Steps:**
1. Prepare a request payload for manual liability creation with initial amount set to the smallest valid positive value (e.g. 0.01).
2. Send the request to the manual liability creation endpoint.
3. Verify the response indicates success and that a liability record exists with that amount.

**Expected result:** The system creates the manual liability successfully with the small positive amount. No validation error is returned. This confirms boundary behaviour: zero is rejected, small positive is accepted.

**References:** PDT-2474; boundary; manual liability API.

---

## TC-7 (Negative): Manual receivable creation rejected when initial amount is missing or null

**Objective:** Verify that when the initial amount is missing or null in the manual receivable creation request, the system rejects the request with a validation error. This ensures required-field validation is in place in addition to zero-amount validation.

**Preconditions:**
1. A customer exists and is active in the system.
2. The caller has access to the manual receivable creation API.

**Steps:**
1. Prepare a request payload for manual receivable creation with the initial amount omitted or set to null.
2. Send the request to the manual receivable creation endpoint.
3. Observe the HTTP status code and response body.

**Expected result:** The system returns a validation error (e.g. 400 Bad Request). No CustomerReceivable record is created. The response indicates that the initial amount is required (or equivalent).

**References:** PDT-2474; CustomerReceivableRequest @Positive; required field validation.

---

## References

- **Jira:** PDT-2474 – Liabilities and receivables shouldn't be generated with amount zero.
- **Entry points:** Manual liability creation API, Manual receivable creation API.
- **What could break:** Manual API must reject initialAmount=0; valid non-zero requests must still succeed.
- **Related:** ZeroAmountValidationListener (JPA @PrePersist); request DTO validation; CustomerLiability, CustomerReceivable persistence.
