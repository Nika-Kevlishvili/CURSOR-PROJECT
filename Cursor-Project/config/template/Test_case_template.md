# Test case document template

**Scope:** All `.md` files under `Cursor-Project/test_cases/` MUST follow this structure. Use **plain English**, full sentences where helpful, no unexplained jargon.

**Backend / Frontend split (mandatory):** Each document MUST contain two sections: **Backend Test Cases** and **Frontend Test Cases**. Backend TCs use prefix `TC-BE-N`, frontend TCs use `TC-FE-N`. If a section is not applicable, keep the heading with a note: *"No backend/frontend test cases applicable for this scope."*

**Positive and negative (mandatory):** Each section (Backend and Frontend) MUST include at least one **(Positive)** and one **(Negative)** scenario (when the section is applicable). Label every TC in its heading: `TC-BE-1 (Positive): …` or `TC-FE-2 (Negative): …`.

**Per test case — use exactly these blocks (in this order):**

| Block | Meaning |
|-------|---------|
| **Test title** | Issue-style summary: what this case is about (also the text after `TC-BE-N (Positive|Negative):` in the heading). |
| **Description** | What needs to be checked; the verification goal. |
| **Preconditions** | **Complete data chain** that must exist before you run this case (numbered list). List every entity, relationship, and attribute that the test depends on — from top-level (customer) down to the entity under test. See **Data completeness rule** below. |
| **Test steps** | Actions to perform during the test (numbered list). |
| **Expected test case results** | Correct system/user-visible outcome; what "pass" looks like. |

Optional for bugs: **Actual result** (current wrong behaviour). Optional: **References** (Jira, Confluence, API name).

### Data completeness rule (MANDATORY)

Preconditions MUST describe the **full data chain** required by the test — not just the entity under test, but **every upstream entity** that must exist for the scenario to be valid. Apply the **specificity principle**:

- **Generic:** If the test works with any instance of an entity (e.g. any customer), write a short precondition: *"A customer exists."*
- **Specific:** If the test depends on a particular type, state, attribute, or relationship, spell it out: *"A private customer exists with customer manager Nika Kevlishvili and status ACTIVE."*

**What to include (when relevant to the test):**

| Data layer | Examples of what to specify |
|---|---|
| **Customer** | Type (legal / private), status, customer manager, segment, specific attributes. |
| **POD (Point of Delivery)** | Identifier, type (electricity / gas), activation date, deactivation date, status, coordinates. |
| **Product / Tariff** | Product name or code, term (fixed / indefinite), price components (energy, grid, tax), data delivery type (by scale / by profile), specific amounts or rates if the test is sensitive to them. |
| **Product contract** | Contract number, status, entry-into-force date, termination date, version, linked POD(s), linked product. |
| **Service contract** | Same as product contract where applicable. |
| **Billing run** | Type (standard / interim / closing), period (from–to dates), status, linked contracts. |
| **Invoice** | Invoice number, status (generated / paid / cancelled), amount, currency, linked billing run. |
| **Payment** | Amount, status, linked invoice, linked payment package. |
| **Payment package** | Lock status (LOCKED / UNLOCKED), linked payments. |
| **Dates** | Activation / deactivation dates, contract entry-into-force / termination dates, billing period boundaries — **whenever the test outcome depends on timing or date ranges**. |
| **Amounts** | Specific monetary values, quantities, scale boundaries — **whenever the test validates calculation, thresholds, or rounding**. |

**Rule of thumb:** If removing a detail from the precondition would make the test ambiguous or impossible to set up without guessing, that detail MUST be present.

**Reference:** `.cursor/rules/workspace/test_cases_structure.mdc`

---

## Copy-paste blank

````markdown
# {Document title} – {Short scope} ({JIRA_KEY})

**Jira:** {JIRA_KEY} ({Board})  
**Type:** {Task | Bug | Feature}  
**Summary:** {What this file tests and why it matters — 1–2 sentences.}

**Scope:** {Area/flow in plain language; expected behaviour; if bug — what fails today.}

---

## Test data (preconditions)

Shared setup for this file (environment + entities). List the **full data chain** from top-level entities down to the entity under test. Be specific where the test demands it; be generic where any instance works.

- **Environment:** {e.g. Test}
- **Customer:** {type (legal/private), status, relevant attributes — or "any active customer" if generic}
- **POD:** {identifier, type, activation/deactivation dates if relevant}
- **Product:** {name/code, term, price components, data delivery type (scale/profile) if relevant}
- **Product contract:** {status, entry-into-force date, linked POD, linked product — if relevant}
- **{Other entities as needed}:** {billing run type/period, invoice status/amount, payment, etc.}

---

## Backend Test Cases

### TC-BE-1 (Positive): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}
2. {…}

**Test steps:**
1. {…}
2. {…}

**Expected test case results:** {Correct response: what the API returns / system does; status code, body, side effects.}

**Actual result (if bug):** {Omit if not a bug.}

**References:** {Optional.}

---

### TC-BE-2 (Negative): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}

**Test steps:**
1. {…}

**Expected test case results:** {Rejection, error, or safe failure — no bad data created; expected status code and error message.}

**Actual result (if bug):** {Optional.}

**References:** {Optional.}

---

## Frontend Test Cases

### TC-FE-1 (Positive): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}
2. {…}

**Test steps:**
1. {…}
2. {…}

**Expected test case results:** {Correct UI behaviour: what the user sees, form states, navigation, success messages.}

**Actual result (if bug):** {Omit if not a bug.}

**References:** {Optional.}

---

### TC-FE-2 (Negative): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}

**Test steps:**
1. {…}

**Expected test case results:** {UI validation error, disabled button, error toast — no bad data submitted.}

**Actual result (if bug):** {Optional.}

**References:** {Optional.}

---

## References

- **Jira:** {JIRA_KEY} – {short title}.
- **Related:** {…}
````

---

## Placeholder guide

| Part | Rule |
|------|------|
| Document `#` title | Short; end with `({JIRA_KEY})`. |
| **Test title** (in `TC-BE-N` / `TC-FE-N` line) | One line; same idea as an issue summary. |
| **Description** | Verification intent — not a repeat of the title only; say *what* is validated. |
| **Preconditions** | Numbered; **full data chain** — every entity, type, state, date, and amount the test depends on. Reference **Test data** for shared setup; add TC-specific details here. Apply the specificity principle: generic when any instance works, specific when the test is sensitive to type/state/value. |
| **Test steps** | One action per step; use "e.g." if several ways to execute. |
| **Expected test case results** | Observable outcome; add HTTP code in parentheses only after behaviour is described. |

---

## Language checklist

1. Explain special terms once (e.g. payment package = …).  
2. Say *user* / *system* when describing behaviour.  
3. No unexplained abbreviations — spell out POD (Point of Delivery) on first use.  
4. All file content in **English** (Rule 0.7).

---

## Example (filled)

````markdown
# Invoice cancellation – paid invoice, locked payment package (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Bug  
**Summary:** Cancellation must work when the invoice is paid and the payment package is locked.

**Scope:** User cancels a paid invoice whose payment is in a LOCKED package. Expected: cancellation allowed. Actual bug: API errors and asks for UNLOCKED package.

---

## Test data (preconditions)

- **Environment:** Test
- **Customer:** Any active customer (legal or private; type does not affect this flow).
- **Product:** Any product with at least one price component; term and data delivery type are not relevant to cancellation.
- **Product contract:** Status ACTIVE, entry-into-force date in the past, linked to the customer and a valid POD.
- **Billing run:** A standard billing run has been executed for the contract; period covers at least one billing cycle.
- **Invoice:** Generated by the billing run above; status is PAID (not already cancelled); amount > 0.
- **Payment:** A payment exists that is linked to the invoice; amount matches the invoice amount.
- **Payment package:** The payment above belongs to a payment package whose lock status is **LOCKED** (e.g. already reconciled).

---

## Backend Test Cases

### TC-BE-1 (Positive): Cancel paid invoice while package stays locked

**Description:** Check that the cancellation API succeeds and the service does not require an UNLOCKED payment package.

**Preconditions:**
1. Customer, product contract, billing run, invoice, payment, and payment package exist as described in Test data above.
2. Invoice status is PAID.
3. Payment package lock status is **LOCKED**.
4. No prior cancellation exists for this invoice.

**Test steps:**
1. Submit invoice cancellation via `POST /invoice-cancellation` with the invoice identifier.
2. Read response status and body; check cancellation record / invoice state.

**Expected test case results:** The invoice cancellation is created successfully (HTTP 200/201). The system does not require the payment package to be UNLOCKED for this flow. No error message referencing lock status.

**Actual result (if bug):** Error: "Payment package not found with id … and lock status in UNLOCKED"; cancellation blocked.

**References:** NT-1.

---

### TC-BE-2 (Negative): Reject cancel when invoice id is invalid

**Description:** Check that invalid or missing invoice reference is rejected clearly and no cancellation is stored.

**Preconditions:**
1. A user with invoice-cancellation permissions exists and can call the cancel endpoint.
2. No invoice exists with the identifier that will be used in this test (e.g. use a non-existent or malformed invoice number).

**Test steps:**
1. Call `POST /invoice-cancellation` with an empty, malformed, or non-existent invoice identifier.
2. Inspect response status and body; verify no cancellation record was created for any valid invoice.

**Expected test case results:** Validation or not-found error (HTTP 400 or 404); message explains the problem; no orphan or incorrect cancellation row in the database.

**References:** Invoice cancellation API input validation.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Cancel paid invoice from invoice detail page

**Description:** Check that the UI allows cancellation of a paid invoice with a locked payment package and shows a success confirmation.

**Preconditions:**
1. Same data as Test data above (paid invoice, locked payment package).
2. User is logged into the portal with invoice-cancellation permissions.

**Test steps:**
1. Navigate to the invoice detail page for the paid invoice.
2. Click the "Cancel invoice" button.
3. Confirm the cancellation in the confirmation dialog.

**Expected test case results:** The cancellation is submitted successfully. The UI shows a success message (e.g. "Invoice cancelled"). The invoice status updates to CANCELLED on the detail page.

**Actual result (if bug):** Error toast: "Payment package not found with id … and lock status in UNLOCKED"; invoice remains PAID.

**References:** NT-1.

---

### TC-FE-2 (Negative): Cancel button disabled for already-cancelled invoice

**Description:** Check that the UI prevents double cancellation of an already-cancelled invoice.

**Preconditions:**
1. An invoice exists with status CANCELLED (already cancelled previously).
2. User is logged into the portal with invoice-cancellation permissions.

**Test steps:**
1. Navigate to the invoice detail page for the already-cancelled invoice.
2. Observe the state of the "Cancel invoice" button.

**Expected test case results:** The "Cancel invoice" button is disabled or not visible. No cancellation can be submitted for an already-cancelled invoice.

**References:** Invoice cancellation UI validation.

---

## References

- **Jira:** NT-1 – Locked package blocks cancellation.
- **Related:** Invoice cancellation service; payment package lock; PaymentService.cancel.
````

---

## File layout

Files live directly under `Cursor-Project/test_cases/<Topic_name>.md`. Update `test_cases/README.md` when adding new files.
