# Test case document template

**Scope:** All `.md` files under `test_cases/Backend/` and `test_cases/Frontend/` MUST follow this structure. Use **plain English**, full sentences where helpful, no unexplained jargon.

**Two-folder layout (mandatory):** Each topic produces **two separate files** — one in `Backend/` (TC-BE-N only) and one in `Frontend/` (TC-FE-N only). A Backend file MUST NOT contain Frontend test cases, and vice versa. See `rules/workspace/test_cases_structure.mdc`.

**Positive and negative (mandatory):** Each file MUST include at least one **(Positive)** and one **(Negative)** scenario (when the layer is applicable). Label every TC in its heading: `TC-BE-1 (Positive): …` or `TC-FE-2 (Negative): …`.

**Per test case — use exactly these blocks (in this order):**

| Block | Meaning |
|-------|---------|
| **Test title** | Issue-style summary: what this case is about (also the text after `TC-BE-N (Positive|Negative):` in the heading). |
| **Description** | What needs to be checked; the verification goal. |
| **Preconditions** | **Complete creation-step chain** that must be executed before this test (numbered list). Describe HOW to create every entity — not just that it "exists." See **Mandatory creation-step precondition rule** below. |
| **Test steps** | Actions to perform during the test (numbered list). |
| **Expected test case results** | Correct system/user-visible outcome; what "pass" looks like. |

Optional for bugs: **Actual result** (current wrong behaviour). Optional: **References** (Jira, Confluence, API name).

**Negative-case expectation rule (CRITICAL):**
- Do not write generic expectations like "request fails" or "any 400 is acceptable."
- Specify the intended rejection: exact status, expected error code/message fragment, and what must NOT be created/changed.
- `400 Bad Request` is valid when business validation failure is the expected behavior for the case.
- Use `403 Forbidden` as expected only when permission/authorization is the thing being tested.
- If code/contract defines a scenario-specific error behavior, write that exact expected error in the test case.

---

### Mandatory creation-step precondition rule (CRITICAL)

Preconditions MUST describe the **full data creation chain** required by the test — **step-by-step instructions on HOW to create every upstream entity**, not just that entities "exist."

**NEVER write "an entity exists" — ALWAYS write "create an entity via [endpoint/action] with [parameters]."**

| BAD (FORBIDDEN) | GOOD (REQUIRED) |
|---|---|
| "An active user exists." | "Create a user via `POST /users` (role: STANDARD, status: ACTIVE, email: test@example.com)." |
| "An order is linked to the user." | "Create an order via `POST /orders` linking user ID from step 1, product ID from step 2 (status: PENDING, total: 99.99 USD)." |
| "A payment has been processed." | "Create a payment via `POST /payments` (amount matching order total from step 3, method: CARD, linked to order from step 3). Payment transitions to status COMPLETED." |

**Every entity in the chain MUST have its own numbered precondition step with:**
1. The **API endpoint** (for backend) or **UI action** (for frontend) used to create or set up the entity.
2. The **key parameters/attributes** required (type, status, amount, dates, linked entities).
3. **References to earlier precondition steps** when linking entities (e.g. "user ID from step 1", "order from step 3").

**Data layers to always include when relevant to the test:**

| Data layer | What to specify in the creation step |
|---|---|
| **User / Account** | `POST /users` — role, status, permissions, identifier. |
| **Organization / Group** | `POST /organizations` — type, status, parent org if applicable. |
| **Product / Item** | `POST /products` — category, price, status, availability. |
| **Order / Transaction** | `POST /orders` — linked user, items, status, amounts, dates. |
| **Payment** | `POST /payments` — amount, method, linked order/invoice. |
| **Invoice** | `POST /invoices` — linked order, amounts, due date, status. |
| **Configuration / Settings** | Describe the config change — endpoint, key, value. |
| **Dates** | Effective dates, expiration dates, period boundaries. |
| **Amounts** | Specific monetary values, rates, quantities — whenever the test validates calculation or thresholds. |

> **Customize these data layers** for your project's domain entities.

**Rule of thumb:** Every entity that must exist for the test to run MUST have its own creation step with endpoint and key parameters. If a tester cannot set up the test without guessing how to create an entity, the precondition is incomplete.

---

### Reuse model — DRY preconditions (MANDATORY)

One topic file typically has many TCs that share the same long entity-creation chain. **Write that chain once** in `## Test data (preconditions)`, then have each TC's `Preconditions:` block reference the shared steps and only add **deltas** (the things that differ for that TC).

**Pattern:**
- `## Test data` — full numbered creation chain (every entity, endpoint, parameters).
- `TC-BE-N Preconditions:` — reference the slice (`Apply Test data steps 1–N.`) + list only TC-specific overrides/additions.

**Per-TC mandatory lines:**
- Line 1: `Apply Test data steps X–Y.`
- Line 2: `Delta: ...` (what is unique for this TC)
- If no difference exists: `Delta: none (shared setup unchanged).`

**Important:** Each TC MUST have an explicit delta declaration. Do not rely on Description/Expected results to imply setup differences.

**BAD (FORBIDDEN) — repeated creation chain across TCs:**

```
### TC-BE-1 Preconditions:
1. Create user via POST /users (STANDARD, ACTIVE).
2. Create product via POST /products (AVAILABLE).
3. Create order via POST /orders …
…
7. Payment status = COMPLETED.

### TC-BE-2 Preconditions:
1. Create user via POST /users (STANDARD, ACTIVE).   ← identical to TC-BE-1
2. Create product via POST /products (AVAILABLE).     ← identical to TC-BE-1
…
```

**GOOD (REQUIRED) — shared chain in Test data, only deltas per TC:**

```
## Test data (preconditions)
1. Create user via POST /users (role: STANDARD, status: ACTIVE).
2. Create product via POST /products (category: ELECTRONICS, status: AVAILABLE, price: 99.99 USD).
…
7. Payment status: COMPLETED.

### TC-BE-1 Preconditions:
1. Apply Test data steps 1–7.
2. Delta: confirm order status = FULFILLED (step 5).
3. Delta: confirm payment status = COMPLETED (step 7).

### TC-BE-2 Preconditions:
1. Apply Test data steps 1–4 only (order not yet created).
2. Delta: do NOT create an order — user has products in cart but no order exists.
```

**Self-check rule:** Before finalising a TC file, scan for duplicated `POST /` or "Create … via" lines across multiple TCs. If the same creation step appears in two or more TCs, move it into `## Test data` and replace each occurrence with an "Apply Test data steps …" reference.

---

## Copy-paste blank — Backend file (`test_cases/Backend/<Topic_name>.md`)

````markdown
# {Document title} – {Short scope} ({JIRA_KEY})

**Jira:** {JIRA_KEY} ({Board})  
**Type:** {Task | Bug | Feature}  
**Summary:** {What this file tests and why it matters — 1–2 sentences.}

**Scope:** {Area/flow in plain language; expected behaviour; if bug — what fails today.}

---

## Test data (preconditions)

Shared setup for this file (environment + entity creation chain). Describe step-by-step HOW to create every entity needed.

- **Environment:** {e.g. Test}
1. Create a user via `POST /users` ({role, status, key attributes}).
2. Create a resource via `POST /resources` ({type, status}).
3. Create an order via `POST /orders` (linking user from step 1, resource from step 2; {status, dates}).
4. {Continue with domain-specific setup as needed.}

---

## Backend Test Cases

### TC-BE-1 (Positive): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. Complete steps 1–N from Test data above.
2. {Any TC-specific additional setup — create via endpoint with parameters.}

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
1. {Step-by-step entity creation chain for this negative scenario.}

**Test steps:**
1. {…}

**Expected test case results:** {Rejection, error, or safe failure — no bad data created; expected status code and error message.}

**Actual result (if bug):** {Optional.}

**References:** {Optional.}

---

## References

- **Jira:** {JIRA_KEY} – {short title}.
- **Related:** {…}
````

## Copy-paste blank — Frontend file (`test_cases/Frontend/<Topic_name>.md`)

````markdown
# {Document title} – {Short scope} ({JIRA_KEY})

**Jira:** {JIRA_KEY} ({Board})  
**Type:** {Task | Bug | Feature}  
**Summary:** {What this file tests and why it matters — 1–2 sentences.}

**Scope:** {Area/flow in plain language; expected behaviour; if bug — what fails today.}

---

## Test data (preconditions)

Shared setup for this file (environment + entity creation chain via UI or pre-existing data).

- **Environment:** {e.g. Test}
1. Log into the application with {role/permissions}.
2. {Create or navigate to the required entities — describe full chain.}

---

## Frontend Test Cases

### TC-FE-1 (Positive): {Test title — issue summary stating the test purpose}

**Description:** {What needs to be checked.}

**Preconditions:**
1. {Step-by-step setup: create entities via UI or ensure they exist from backend setup.}
2. User is logged into the application with {required permissions}.

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
1. {Step-by-step setup for this negative scenario.}

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
| **Preconditions** | Numbered; **step-by-step creation chain** — every entity described with its creation endpoint/action, key parameters, and links to earlier steps. NEVER just "entity X exists." |
| **Test steps** | One action per step; use "e.g." if several ways to execute. |
| **Expected test case results** | Observable outcome; add HTTP code in parentheses only after behaviour is described. |

---

## Language checklist

1. Explain special terms once (e.g. payment package = …).  
2. Say *user* / *system* when describing behaviour.  
3. No unexplained abbreviations — spell out on first use.  
4. All file content in **English**.
