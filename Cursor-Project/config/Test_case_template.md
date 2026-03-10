# Test Case Document Template – Detailed & Human-Readable

**Scope:** All test case `.md` files under `Cursor-Project/test_cases/Flows/` and `Cursor-Project/test_cases/Objects/` MUST follow this structure. Test cases must be **maximally detailed** and written in **human-readable language** (no unexplained jargon; plain English).

**Positive and negative (mandatory):** Every test case document MUST include **both positive and negative** scenarios. **Positive** = valid input, happy path, expected success (e.g. "User cancels invoice when data is valid → cancellation is created"). **Negative** = invalid input, error conditions, edge cases, expected failure or rejection (e.g. "User tries to cancel already-cancelled invoice → system returns error and does not create duplicate cancellation"). Label each TC as **(Positive)** or **(Negative)** in the title; include at least one of each unless the scope is explicitly only one type.

**Reference:** `.cursor/rules/test_cases_structure.mdc` (folder layout); `.cursor/commands/test-case-generate.md`; HandsOff Step 3.

---

## Template Structure

Use the following structure for every test case document. Replace placeholders with concrete, understandable descriptions. Prefer full sentences over terse bullets where it helps clarity.

```
# {Document title} – {Short scenario or scope} ({JIRA_KEY})

**Jira:** {JIRA_KEY} ({Board or project name})  
**Type:** {Task | Bug | Feature}  
**Summary:** {One or two sentences: what this document tests and why it matters.}

**Scope:** {2–4 sentences in plain language: what area or flow is covered, what the expected behaviour is, and—if a bug—what currently goes wrong. Avoid technical IDs unless necessary; use "the payment", "the invoice", "the customer" etc.}

---

## Test data (preconditions)

- **Environment:** {e.g. Test, Dev, or "as per ticket"}.
- **{Entity 1}:** {State in plain language, e.g. "A customer exists and is active."}
- **{Entity 2}:** {State in plain language, e.g. "An invoice has been generated and is not cancelled."}
- **{Entity 3}:** {State in plain language, e.g. "The payment package for this payment is locked (lock status = LOCKED).}
- {Add as many entities as needed; each line = one clear precondition.}

---

## TC-1 (Positive): {Scenario title – happy path / valid case}

**Objective:** {One or two sentences: what we want to verify and why a human would care. No jargon without a short explanation.}

**Preconditions:**
1. {First precondition in full sentence.}
2. {Second precondition.}
3. {Continue as needed.}

**Steps:**
1. {First step: what to do, in clear language. Include "e.g." or "for example" if it helps.}
2. {Second step.}
3. {Third step.}
4. {Continue until the scenario is fully described.}

**Expected result:** {One or more sentences describing the desired outcome. What should the user see? What should the system do? What must not happen?}

**Actual result (if bug):** {Optional. Only for bug scenarios: what currently happens (error message, wrong state, etc.) in plain language.}

**References:** {Optional. Links to Jira, Confluence, or related flows.}

---

## TC-2 (Negative): {Scenario title – invalid / error / edge case}

**Objective:** {…}

**Preconditions:**
1. {…}

**Steps:**
1. {…}

**Expected result:** {…}

**Actual result (if bug):** {…}

**References:** {…}

---

## References

- **Jira:** {JIRA_KEY} – {short title}.
- **Related:** {Any related tickets, endpoints, or documentation in human terms.}
```

---

## Placeholders and rules

| Section / placeholder | Rule | Example |
|----------------------|------|--------|
| **Document title** | Short, descriptive; include Jira key in parentheses. | `Invoice Cancellation – Paid Invoice with Locked Payment Package (NT-1)` |
| **Summary** | 1–2 sentences; what is tested and why it matters. | "Invoice cancellation must be possible when the invoice is paid and the payment package is locked." |
| **Scope** | 2–4 sentences; area, expected behaviour, and—if bug—current wrong behaviour. Plain language. | "Create invoice cancellation when the invoice is paid and the payment package is LOCKED. Currently the system returns an error; it should allow cancellation." |
| **Test data (preconditions)** | List environment and each entity’s state in clear, full sentences. | "**Payment package:** The payment package for this payment is locked (lock status = LOCKED)." |
| **TC-N: title** | Human-readable scenario name. **MUST** be labelled **(Positive)** or **(Negative)**. Each document MUST have at least one Positive and one Negative TC. | "TC-1 (Positive): Invoice cancellation when paid and payment package is locked (main scenario)" / "TC-2 (Negative): Cancellation rejected when invoice number is invalid" |
| **Objective** | What we verify and why; no unexplained jargon. | "Verify that the user can cancel the invoice even when the payment package is locked; the system must not block with 'Payment package not found with lock status in UNLOCKED'." |
| **Preconditions** | Numbered list; each item = one full sentence. | "1. An invoice has been generated. 2. The invoice has been paid (a payment exists and is linked to it)." |
| **Steps** | Numbered list; each step = one clear action. Use "e.g." where helpful. | "1. Create a customer (e.g. via API POST /customer). 2. Create a payment package. 3. Create a payment linked to the invoice. 4. Call the invoice cancellation endpoint with the invoice number." |
| **Expected result** | Desired outcome in full sentences: what the user sees, what the system does, what must not happen. | "The invoice cancellation is created successfully. The system allows cancellation even when the payment package is locked. No error message containing 'lock status in UNLOCKED' appears." |
| **Actual result (if bug)** | Optional; only for bugs. Current wrong behaviour in plain language. | "The system returns an error: 'Payment package not found with id 1100 and lock status in UNLOCKED'. Cancellation is blocked." |

---

## Human-readable language rules

1. **Prefer full sentences** over cryptic bullets when it improves understanding.
2. **Explain terms** the first time: e.g. "payment package (the container that groups payments for reconciliation)".
3. **Use "the user", "the system", "the invoice"** instead of only "API" or "entity" when describing behaviour.
4. **Expected result:** say what the user sees or what the system does, not only "HTTP 200" (add "Request succeeds and the cancellation is recorded").
5. **Steps:** one action per step; include "e.g." or "for example" when there are several valid ways to perform the step.
6. **No unexplained abbreviations** in the main text; spell out once if needed (e.g. "POD (Point of Delivery)").
7. **All content in English** (Rule 0.7).

---

## Example (filled)

```markdown
# Invoice Cancellation – Paid Invoice with Locked Payment Package (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Task  
**Summary:** Invoice cancellation must be possible when the invoice is paid and the payment package for that payment is locked. Currently the system blocks this with an error.

**Scope:** This document covers the flow where a user cancels an invoice that has already been paid. The payment is grouped in a payment package, and that package is in LOCKED status (e.g. already reconciled or closed). The expected behaviour is that cancellation is allowed. The current behaviour is that the system returns an error asking for the payment package to be UNLOCKED, which blocks the user from cancelling the invoice.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Customer:** A customer exists and is active (used to create liability and payment).
- **Invoice:** An invoice has been generated and is in a state that allows cancellation (e.g. not already cancelled).
- **Payment:** The invoice has been paid; a payment record exists and is linked to this invoice.
- **Payment package:** The payment belongs to a payment package, and that payment package is **locked** (lock status = LOCKED), e.g. because it has been reconciled or closed.

---

## TC-1 (Positive): Invoice cancellation when paid and payment package is locked (main scenario)

**Objective:** Verify that a user can create an invoice cancellation when the invoice is paid and the payment package is locked. The system must not block cancellation with a message like "Payment package not found with id X and lock status in UNLOCKED".

**Preconditions:**
1. An invoice has been generated (e.g. via billing or API).
2. The invoice has been paid (a payment record exists and is linked to the invoice).
3. The payment package that contains this payment is locked (lock status = LOCKED).

**Steps:**
1. Ensure the test data above exists (create customer, liability, invoice, payment package, payment, and lock the package if your test setup does not provide it).
2. Call the invoice cancellation endpoint (e.g. POST /invoice-cancellation) with the invoice number(s) or the required payload.
3. Observe the response and the state of the invoice and related records.

**Expected result:** The invoice cancellation is created successfully. The system allows cancellation even when the payment package is locked. The user does not see an error message that requires the payment package to be in UNLOCKED status.

**Actual result (current bug):** The system returns an error such as "Payment package not found with id 1100 and lock status in UNLOCKED". Cancellation is blocked until the package is unlocked.

**References:** NT-1; payment package lock status; invoice cancellation flow.

---

## TC-2 (Positive): Confirmation that UNLOCKED is not required for cancellation

**Objective:** Ensure that the cancellation flow does not require the payment package to be in UNLOCKED status. Cancellation should be permitted when the package is LOCKED.

**Preconditions:**
1. Same as TC-1: paid invoice, payment package in LOCKED status.

**Steps:**
1. Confirm (e.g. via API or database) that the payment package lock status is LOCKED.
2. Perform the invoice cancellation (via API or UI, as per the product).
3. Check the response: it should indicate success and a cancellation record should be created; the response must not contain an error message that mentions "lock status in UNLOCKED".

**Expected result:** Cancellation succeeds. The backend does not restrict cancellation to cases where the payment package is UNLOCKED.

---

## TC-3 (Negative): Cancellation rejected when invoice number is invalid or missing

**Objective:** Verify that the system rejects the request when the user sends an invalid or missing invoice identifier, and returns a clear error without creating a cancellation.

**Preconditions:**
1. User has access to the invoice cancellation endpoint (e.g. API or UI).

**Steps:**
1. Call the invoice cancellation endpoint with an invalid payload (e.g. empty invoice numbers, non-existent invoice number, or wrong format).
2. Observe the response: status code and message.

**Expected result:** The system returns an error (e.g. 400 Bad Request or 404 Not Found). No cancellation record is created. The error message is clear and indicates what is wrong (e.g. "Invoice not found" or "Invoice numbers are required").

**References:** Validation rules; invoice cancellation API.

---

## References

- **Jira:** NT-1 – Invoice cancellation when paid and payment package locked.
- **Current error:** "Payment package not found with id 1100 and lock status in UNLOCKED".
- **Related:** Invoice cancellation endpoint/service; payment package entity and lock status; link between payment and invoice.
```

---

## Positive vs negative – quick guide

- **Positive:** Valid data, happy path, expected success. Example: "Create invoice cancellation with valid invoice number → cancellation is created; user sees success."
- **Negative:** Invalid data, forbidden action, or edge case; system must reject or return a clear error. Example: "Create invoice cancellation with non-existent invoice number → system returns 404 or validation error; no cancellation is created."
- **Each document:** At least one **TC (Positive)** and at least one **TC (Negative)**. Add more of either as needed (e.g. several negative cases: missing field, wrong format, already cancelled, etc.).

---

## Summary

- **Folder:** Test case files go under `Cursor-Project/test_cases/Flows/<Flow_name>/` or `Cursor-Project/test_cases/Objects/<Entity_name>/` (see `.cursor/rules/test_cases_structure.mdc`).
- **Content:** Every test case document MUST follow the structure above and be **maximally detailed** and **human-readable**.
- **Positive and negative:** Every document MUST include **both positive and negative** test cases; label each TC as **(Positive)** or **(Negative)**.
- **Language:** English only; full sentences where they help; no unexplained jargon or abbreviations.
