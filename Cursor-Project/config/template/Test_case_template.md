# Test case document template

**Scope:** All `.md` files under `Cursor-Project/test_cases/Objects/` and `Cursor-Project/test_cases/Flows/` MUST follow this structure. Use **plain English**, full sentences where helpful, no unexplained jargon.

**Positive and negative (mandatory):** Each document MUST include at least one **(Positive)** and one **(Negative)** scenario. Label every TC in its heading: `TC-1 (Positive): …` or `TC-2 (Negative): …`.

**Per test case — use exactly these blocks (in this order):**

| Block | Meaning |
|-------|---------|
| **Test title** | Issue-style summary: what this case is about (also the text after `TC-N (Positive|Negative):` in the heading). |
| **Description** | What needs to be checked; the verification goal. |
| **Preconditions** | Parameters and state that must be true before you run this case (numbered list). |
| **Test steps** | Actions to perform during the test (numbered list). |
| **Expected test case results** | Correct system/user-visible outcome; what “pass” looks like. |

Optional for bugs: **Actual result** (current wrong behaviour). Optional: **References** (Jira, Confluence, API name).

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

Shared setup for this file (environment + entities):

- **Environment:** {e.g. Test}
- **{Entity}:**
- **{Entity}:**

---

## TC-1 (Positive): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}
2. {…}

**Test steps:**
1. {…}
2. {…}

**Expected test case results:** {Correct response: what the user sees / system does; what must not happen.}

**Actual result (if bug):** {Omit if not a bug.}

**References:** {Optional.}

---

## TC-2 (Negative): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {…}

**Test steps:**
1. {…}

**Expected test case results:** {Rejection, error, or safe failure — no bad data created.}

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
| **Test title** (in `TC-N` line) | One line; same idea as an issue summary. |
| **Description** | Verification intent — not a repeat of the title only; say *what* is validated. |
| **Preconditions** | Numbered; reference **Test data** when setup is already listed there. |
| **Test steps** | One action per step; use “e.g.” if several ways to execute. |
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
- **Customer:** Active.
- **Invoice:** Paid, not already cancelled.
- **Payment package:** LOCKED; contains the payment for that invoice.

---

## TC-1 (Positive): Cancel paid invoice while package stays locked

**Description:** Check that cancellation succeeds and the service does not require an UNLOCKED payment package.

**Preconditions:**
1. Test data above is present.
2. Package lock status is LOCKED.

**Test steps:**
1. Submit invoice cancellation for that invoice (UI or API per your harness).
2. Read response and check cancellation / invoice state.

**Expected test case results:** Cancellation is recorded; no error demanding UNLOCKED lock status.

**Actual result (if bug):** Error mentions package not found / UNLOCKED requirement; cancellation blocked.

**References:** NT-1.

---

## TC-2 (Negative): Reject cancel when invoice id is invalid

**Description:** Check that invalid or missing invoice reference is rejected clearly and no cancellation is stored.

**Preconditions:**
1. Caller can reach the cancel operation (same auth as normal tests).

**Test steps:**
1. Call cancel with empty, malformed, or unknown invoice id.
2. Inspect response and verify no new cancellation for valid invoices.

**Expected test case results:** Validation or not-found error; message explains the problem; no orphan or wrong cancellation row.

**References:** Cancel API validation.

---

## References

- **Jira:** NT-1 – Locked package blocks cancellation.
- **Related:** Invoice cancel service; payment package lock.
````

---

## Folder layout

Files live under `Cursor-Project/test_cases/Objects/<Entity>/` or `Cursor-Project/test_cases/Flows/<Flow>/`. Update folder `README.md` when adding entities or flows.
