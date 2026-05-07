# Test Case Quality Rubric

**Purpose:** Every test case generated or reviewed in this project is scored against this rubric before being saved. The rubric is the single source of truth for what makes a test case "good enough to keep." It is used by:
- `test-case-generator` skill and subagent (self-scoring before file write)
- `test-case-quality-validator` subagent (second-pass independent review)
- `/test-case-quality` command (ad-hoc quality check on existing files)

---

## Scoring model

Score each TC on **6 axes**, each from **0 to 2**. Maximum total: **12**. Minimum to pass: **8**.

| Axis | 0 — Fail | 1 — Partial | 2 — Pass |
|------|----------|-------------|----------|
| **1. Intent uniqueness** | TC title or description is a duplicate of another TC in the same file (same scenario, different name), or is so vague it overlaps with ≥2 others. | Title is unique but description uses generic language that makes the distinguishing intent hard to find. | TC has a clear, unique verification goal that no other TC in the same file covers. |
| **2. Observable expected result** | Expected result says "system works correctly", "operation succeeds", "no error occurs", or any other non-verifiable statement. For negative cases, "any 400 is acceptable" is also fail. | Expected result names the outcome (e.g. "HTTP 200" or "HTTP 400") but omits key observable details (response fields, body content, side effects, error semantics). | Expected result describes a **specific, observable, testable outcome**: HTTP status code AND the relevant response field/value AND any relevant side effect (no record created, status changed, error message text). For negative cases, includes expected error semantics (code/message fragment + failing field/constraint). `400` is valid when intended by business validation; `403` is valid only for permission scenarios. |
| **3. Endpoint specificity** | No API endpoint or UI action is named in test steps or preconditions. | Endpoint is mentioned but key parameters (required fields, status, type) are absent or described only as "{parameters}". | Every API call names the **HTTP method + path** and lists the **key payload fields** or query parameters relevant to this TC. |
| **4. Delta clarity** | TC is a positive case that could be negative (or vice versa) and the Preconditions do not show what makes it distinct. Or: negative case does not specify what is different from the positive case. | The difference from the base case exists but is buried in prose and not easy to identify. | The **delta from the base / Test data setup** is explicitly called out at the start of Preconditions (e.g. "Delta: invoice status = GENERATED instead of PAID"). |
| **5. Risk coverage from cross_dep** | Cross-dependency data (what_could_break, integration points) was provided but this TC does not address any of those risks. | TC partially addresses a cross-dep risk but the connection is implicit. | TC explicitly covers ≥1 item from the `what_could_break` or `integration_points` output of the cross-dependency step, OR the TC is in a domain where cross-dep data is not applicable (state this clearly). |
| **6. Readability** | Steps are not numbered, or contain unexplained abbreviations, or mix multiple distinct actions in a single step, or require domain knowledge to interpret without context. | Steps are numbered but some use terse shorthand that a new tester could misinterpret. | Steps are numbered, each step is one action, all abbreviations are spelled out on first use, and a developer unfamiliar with the domain could execute the test from the steps alone. |

---

## How to score

1. Read the TC's Description, Preconditions, Test steps, and Expected test case results.
2. Assign 0/1/2 to each of the 6 axes using the table above.
3. Sum the scores. If sum ≥ 8, the TC passes. If sum < 8, the TC must be rewritten.
4. On rewrite, rescore. Max 2 rewrite passes. After that, surface the TC to the user with the exact failing axes and the reason.

**Output format for the validator:**

```
TC-BE-3 (Negative): Create billing run with missing contract
  Axis 1 Intent uniqueness:     2
  Axis 2 Observable expected:   1  ← "request fails" — missing status code and error text
  Axis 3 Endpoint specificity:  2
  Axis 4 Delta clarity:         0  ← no delta stated; reads identical to TC-BE-1 setup
  Axis 5 Risk coverage:         2
  Axis 6 Readability:           2
  Total: 9/12  PASS (weaknesses noted for improvement)

TC-BE-7 (Negative): Invalid input
  Axis 1 Intent uniqueness:     0  ← title "Invalid input" is not unique; 3 other TCs have same description
  Axis 2 Observable expected:   0  ← "system rejects the request" — no code, no message
  Axis 3 Endpoint specificity:  1  ← endpoint named, no payload
  Axis 4 Delta clarity:         0  ← no delta
  Axis 5 Risk coverage:         1
  Axis 6 Readability:           1
  Total: 3/12  FAIL — rewrite required
  Suggested fixes:
    - Axis 1: Rename to describe the specific invalid input (e.g. "Billing run rejected when contractId is null").
    - Axis 2: Add "HTTP 400 Bad Request; response body contains validation error for field contractId".
    - Axis 4: Add "Delta: payload omits required contractId field (null)".
```

---

## Anti-pattern catalog

These are the most common quality failures observed in practice. Each maps to one or more rubric axes.

| Anti-pattern | Axis | Why it fails |
|---|---|---|
| Title = "Invalid input" or "Error case" | 1 | Not unique; cannot tell from the title what invalid input or what error. |
| Expected = "The system works correctly." | 2 | Not verifiable. |
| Expected = "The operation succeeds." | 2 | Not verifiable. |
| Expected = "No error occurs." | 2 | Not verifiable. |
| Expected = "HTTP 200" with nothing else | 2 | Status code alone is not enough — what does the response contain? |
| Expected (negative) = "any 400 means pass" | 2 | Does not validate intended business rejection semantics. |
| Negative TC passes on `401/403` auth failure | 2, 5 | Auth failure is not the business rejection being tested (unless auth is the scope). |
| Negative TC uses wrong URL and accepts `404` | 2, 3 | Endpoint/path is incorrect; this tests routing mistake, not feature behavior. |
| TC ignores a scenario-specific error defined in code/contract | 2, 5 | Expected result does not match actual contract behavior for that case. |
| Steps name an endpoint but no method/fields | 3 | "Call the billing run endpoint" — which method? which fields? |
| Preconditions say "An active customer exists." | 3, 4 | Doesn't say how to create; doesn't say what "active" means in parameters. |
| Negative TC has identical preconditions to positive TC | 4 | Cannot tell what makes this scenario fail. |
| No delta line at start of Preconditions | 4 | Reader must diff against Test data manually. |
| cross_dep listed 3 integration points, TC ignores all | 5 | Regression gaps — bugs in integration points won't be caught. |
| Step 3 = "Verify the result is correct." | 6 | Not one action; "correct" is undefined. |
| Two actions combined in one step | 6 | Hard to pinpoint which action caused a failure. |
| Unexplained acronym (LPF, MLO, JPA) | 6 | New testers cannot interpret without background knowledge. |
| TC title, description, and first step are identical in meaning | 1, 6 | Redundant; remove or differentiate. |

---

## Relationship to other documents

- **Template:** `Cursor-Project/config/template/Test_case_template.md` — governs structure and precondition DRY rules.
- **TC structure rule:** `.cursor/rules/workspace/test_cases_structure.mdc` — governs folder layout, numbering, coverage.
- **Validator subagent:** `.cursor/agents/test-case-quality-validator.md` — applies this rubric independently after generation.
- **Generator skill:** `.cursor/skills/test-case-generator/SKILL.md` — applies this rubric during self-check before file write.
