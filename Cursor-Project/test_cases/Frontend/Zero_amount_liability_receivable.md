# Zero-amount liability and receivable prevention – Frontend validation (PDT-2474)

**Jira:** PDT-2474 (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** Liabilities and receivables must not be created with amount zero. The portal must validate amount > 0 on relevant forms, and backend validation from `ZeroAmountValidationListener` must appear as clear, user-facing messages.

**Scope:** UI forms and flows that create liabilities, receivables, and deposits linked to those entities. Forms must validate amount > 0 before submission where applicable. Backend errors from `ZeroAmountValidationListener` (and related DTO validation on `CustomerLiabilityRequest` / `CustomerReceivableRequest`) must be shown as user-friendly messages—not raw HTTP 500 pages or stack traces.

**Related implementation (reference only):** `ZeroAmountValidationListener` — `@PrePersist` JPA listener; throws if `initialAmount == 0`. `CustomerLiabilityRequest`: `@DecimalMin(value="0", inclusive=false)` on `initialAmount`. `CustomerReceivableRequest`: `@Positive` on `initialAmount`.

---

## Test data (preconditions)

Shared setup for all cases in this file. Replace `{BASE_URL}` with the portal base URL for the target environment (e.g. Test). Use a test user that has the permissions noted in each test case.

1. **Environment:** Use the agreed Phoenix portal environment (e.g. Test). Confirm the portal is reachable at `{BASE_URL}`.

2. **Portal login (generic):** Open `{BASE_URL}/login` in a supported browser. Enter the test user’s username and password. Click **Login** (or equivalent). Confirm the main portal shell loads (home or dashboard).

3. **Customer creation (when a fresh customer is needed):** Create a customer via `POST /customer` with at least: `type`: PRIVATE (or per product default), `status`: ACTIVE, `customerIdentifier`: auto-generated or unique string per run. Store the returned customer identifier or internal id from the response body for navigation and API correlation.

4. **Customer navigation (when using an existing or newly created customer):** In the portal, go to **Customers** (or equivalent top-level menu). Search by the identifier from step 3 (or from an existing test customer). Open the customer’s **detail** view.

5. **Receivables / liabilities area:** From customer detail, open the tab or submenu labeled **Receivables**, **Liabilities**, or **Financial** / **Balances** (use the label present in the build under test). This is the area where **Create Liability**, **Create Receivable**, and related actions are available.

6. **Deposits area (for deposit tests):** From the same customer detail, open the **Deposits** tab (or equivalent). This is where **Create Deposit** is started.

Individual test cases below may reference these steps as “Complete Test data steps 1–N” and add case-specific steps.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Manual liability creation form accepts valid non-zero amount

**Description:** Verify that when the user enters a valid positive initial amount and satisfies all other required fields, the liability is created and visible in the UI with the correct amount.

**Preconditions:**
1. Open the portal login page at `{BASE_URL}/login` and log in with a user that has **liability creation** permissions (enter credentials, click **Login**).
2. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated) **or** navigate to an existing active customer: **Customers** → search → select customer (reference customer from Test data step 4).
3. Navigate to the customer’s receivables / liabilities management area (customer detail → **Receivables** / **Liabilities** tab or equivalent — reference Test data step 5).

**Test steps:**
1. Click **Create Liability** (or equivalent) to open the form.
2. Enter **100.00** in the **Initial Amount** field.
3. Fill all other required fields: liability type, due date, currency **BGN** (or required currency for the environment).
4. Click **Save** / **Create**.

**Expected test case results:** The form submits successfully. A success notification (toast or inline message) is shown. The new liability appears in the customer’s liabilities list with initial amount **100.00** (and matching currency). No validation error is shown on the amount field.

---

### TC-FE-2 (Negative): Manual liability creation form rejects zero amount

**Description:** Verify that entering zero in **Initial Amount** is blocked by client-side (or immediate) validation and no liability is persisted.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **liability creation** permissions (enter credentials, click **Login**).
2. Navigate to an active customer: **Customers** → search → select **or** create one via `POST /customer` (type: PRIVATE, status: ACTIVE) and open that customer’s detail (reference customer id / identifier from the response when searching).
3. Navigate to the customer’s receivables / liabilities management page (customer detail → tab from Test data step 5).

**Test steps:**
1. Click **Create Liability** to open the form.
2. Enter **0** or **0.00** in the **Initial Amount** field.
3. Fill all other required fields with valid values.
4. Attempt to click **Save** / **Create** (or trigger submit via keyboard if applicable).

**Expected test case results:** A validation error is shown on or near the **Initial Amount** field (e.g. message equivalent to **Amount must be greater than zero**). The **Save** / **Create** action is disabled or submission is blocked. No new liability row appears in the list; no success notification is shown.

---

### TC-FE-3 (Positive): Manual receivable creation form accepts valid non-zero amount

**Description:** Verify that a valid positive initial amount allows receivable creation and the new receivable is listed correctly.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **receivable creation** permissions.
2. Navigate to an active customer **or** create one via `POST /customer` (type: PRIVATE, status: ACTIVE) and open customer detail (reference identifier from creation response for search).
3. Navigate to the customer’s receivables / liabilities management page (Test data step 5).

**Test steps:**
1. Click **Create Receivable** (or equivalent) to open the form.
2. Enter **50.00** in the **Initial Amount** field.
3. Fill all other required fields per the form.
4. Click **Save** / **Create**.

**Expected test case results:** The form submits successfully. A success notification is displayed. The receivable appears in the customer’s receivables list with amount **50.00** (and correct currency).

---

### TC-FE-4 (Negative): Manual receivable creation form rejects zero amount

**Description:** Verify zero initial amount is rejected for receivables and nothing is created.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **receivable creation** permissions.
2. Navigate to an active customer **or** create one via `POST /customer` (type: PRIVATE, status: ACTIVE) and open customer detail.
3. Navigate to the customer’s receivables management page (Test data step 5).

**Test steps:**
1. Click **Create Receivable** to open the form.
2. Enter **0** or **0.00** in the **Initial Amount** field.
3. Fill all other required fields.
4. Attempt to submit the form.

**Expected test case results:** A validation error is shown (message equivalent to **Amount must be positive** or consistent with `@Positive` messaging in the UI). Submission is blocked. No new receivable appears in the list.

---

### TC-FE-5 (Negative): Manual liability creation form rejects negative amount

**Description:** Verify negative values in **Initial Amount** are rejected for liability creation.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **liability creation** permissions.
2. Navigate to an active customer: **Customers** → search → select (or create via `POST /customer` as in TC-FE-2 precondition 2).
3. Open **Create Liability** from the receivables / liabilities area (Test data step 5).

**Test steps:**
1. Enter **-100** in the **Initial Amount** field.
2. Fill other required fields with valid values.
3. Attempt to submit the form.

**Expected test case results:** A validation error is displayed for the amount (or form-level error). The form does not complete successfully. No liability is created and none appears in the list.

---

### TC-FE-6 (Negative): Manual receivable creation form rejects negative amount

**Description:** Verify negative values in **Initial Amount** are rejected for receivable creation.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **receivable creation** permissions.
2. Navigate to an active customer (search or create via `POST /customer` as above).
3. Open **Create Receivable** from the receivables area (Test data step 5).

**Test steps:**
1. Enter **-50** in the **Initial Amount** field.
2. Fill other required fields with valid values.
3. Attempt to submit the form.

**Expected test case results:** A validation error is displayed. The form does not submit successfully. No receivable is created.

---

### TC-FE-7 (Negative): API error from zero-amount liability is displayed as user-friendly message

**Description:** Verify that when a liability-create request reaches the backend with **initialAmount = 0** (so `ZeroAmountValidationListener` or API validation rejects it), the portal shows a clear, user-facing error—not a generic HTTP 500 page or raw stack trace.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **liability creation** permissions (or sufficient rights to trigger the same create endpoint the UI uses).
2. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated). Note `customerId` / identifier from the response.
3. In the portal, navigate to **Customers** → search by identifier from step 2 → open **customer detail**.
4. Navigate to the receivables / liabilities area and click **Create Liability** to open the form (Test data step 5).
5. Fill all required fields and enter **100.00** in **Initial Amount** so client-side validation would normally pass. Open **browser DevTools** → **Network**, enable **Preserve log**.
6. Submit the form once successfully **or** trigger the save action so the **create liability** XHR/fetch appears in the Network list. Select that request and use **Copy as fetch** (or copy as cURL) to capture URL, headers, and JSON body. **Alternatively**, if the test environment documents another supported way to replay an authenticated create call with a modified body (proxy, API client with portal cookie), use that method—must use the **same authenticated session** as the logged-in user from step 1.
7. From DevTools **Console**, run the copied `fetch`, but change the JSON payload so **`initialAmount`** is **0** (keep `customerId` and other ids from the copied body aligned with step 2–4). Execute the request **or** resend the request via tooling with `initialAmount: 0`.

**Test steps:**
1. Execute the zero-amount liability create attempt as prepared in precondition step 7 (modified `fetch` / resend with `initialAmount: 0`).
2. Observe the portal UI: global error area, toast, modal, or inline message on the form (depending on how the SPA handles API errors).
3. Confirm the browser does not show an unhandled error page with stack trace as the only feedback.

**Expected test case results:** The user sees a **user-friendly** error message (e.g. wording equivalent to **Cannot create a liability with zero amount** or the product’s standard validation message for this case—not a blank 500 page). The message is readable without developer tools. The user can dismiss or correct the issue and continue using the portal. No new zero-amount liability appears in the list.

---

### TC-FE-8 (Positive): Deposit creation form with valid non-zero amount creates deposit with liability and receivable

**Description:** Verify that creating a deposit with a valid positive amount succeeds and the related liability and receivable (if shown in the UI for that flow) display non-zero amounts consistent with the deposit.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **deposit creation** permissions.
2. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated) **or** navigate to an existing active customer (**Customers** → search → select).
3. Navigate to the customer’s **Deposits** area (customer detail → **Deposits** tab — Test data step 6).

**Test steps:**
1. Click **Create Deposit** (or equivalent).
2. Enter **200.00** in the deposit **amount** field.
3. Fill other required fields (deposit type, currency **BGN** or as required).
4. Click **Save** / **Create**.

**Expected test case results:** The deposit is created successfully. A success notification is shown. Where the UI exposes related **liability** and **receivable** lines for this deposit, both show **non-zero** amounts consistent with **200.00** (same currency). If the UI only shows the deposit row, confirm the deposit amount is **200.00** and, via drill-down or linked views per product design, associated liability/receivable amounts are not zero.

---

### TC-FE-9 (Negative): Deposit creation form rejects zero amount

**Description:** Verify the deposit form blocks zero amount so no deposit—and no downstream zero-amount liability/receivable—is created from this UI path.

**Preconditions:**
1. Log into the portal at `{BASE_URL}/login` with a user that has **deposit creation** permissions.
2. Navigate to an active customer: create via `POST /customer` (type: PRIVATE, status: ACTIVE) and open detail **or** search for an existing customer.
3. Open **Create Deposit** from the **Deposits** tab (Test data step 6).

**Test steps:**
1. Enter **0** or **0.00** in the deposit **amount** field.
2. Fill other required fields.
3. Attempt to submit the form.

**Expected test case results:** Validation error is shown (message equivalent to **Amount must be greater than zero** or product-standard wording). The form does not submit successfully. No new deposit appears; no new liability or receivable appears from this attempt.

---

## References

| Item | Notes |
|------|--------|
| **Jira** | PDT-2474 — Liabilities and receivables shouldn't be generated with amount zero. |
| **Backend validation** | `ZeroAmountValidationListener`; `CustomerLiabilityRequest` (`@DecimalMin` on `initialAmount`, exclusive of 0); `CustomerReceivableRequest` (`@Positive` on `initialAmount`). |
| **Portal** | `{BASE_URL}` — set per environment; align with EnergoTS / Playwright `BASE_URL` for automated runs. |
