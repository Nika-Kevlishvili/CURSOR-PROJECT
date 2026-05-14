# Test Case Quality Rubric

Scoring system for evaluating generated test cases. Each TC is scored on **6 axes** (0–2 points each). **Maximum: 12 points. Pass threshold: 8/12.**

---

## Axis 1: Intent Uniqueness (0–2)

Does this TC test something **distinct** from every other TC in the same document?

| Score | Criteria |
|-------|----------|
| **0** | Duplicate — tests the same scenario as another TC with no meaningful difference |
| **1** | Overlapping — tests a similar scenario but with a minor variation (e.g. different field, same validation) |
| **2** | Unique — tests a clearly distinct scenario, condition, or behavior path |

**Anti-patterns:**
- Two TCs that both test "missing required field" with different field names but identical logic → score 1
- Two TCs with identical steps but different test data values that trigger the same code path → score 0

---

## Axis 2: Observable Expected Result (0–2)

Is the **expected outcome** specific, verifiable, and unambiguous?

| Score | Criteria |
|-------|----------|
| **0** | Vague — "should work correctly", "returns error", "data is saved" |
| **1** | Partially specific — mentions status code OR error message but not both; missing field-level assertions |
| **2** | Fully specific — exact status code + response body assertions (error code/message, created entity fields, state changes) |

**Examples:**
- Bad (0): "The system should return an error."
- Medium (1): "Returns 400 Bad Request."
- Good (2): "Returns 400 with error code `INVALID_STATUS` and message containing 'Contract must be in ACTIVE status'. No entity is created in the database."

---

## Axis 3: Endpoint / Action Specificity (0–2)

Are **API endpoints** (or UI actions) clearly and correctly specified?

| Score | Criteria |
|-------|----------|
| **0** | No endpoint or action specified — "call the API", "create an entity" |
| **1** | Endpoint specified but missing HTTP method, or uses generic path without parameters |
| **2** | Full specification — HTTP method + path + key parameters/body fields named |

**Examples:**
- Bad (0): "Create a customer in the system."
- Medium (1): "POST /customer"
- Good (2): "POST /customer with body: { type: 'BUSINESS', status: 'ACTIVE', name: 'Test Corp', taxId: '12345' }"

---

## Axis 4: Delta Clarity (0–2)

Are **TC-specific precondition deltas** explicit and distinct from the shared Test Data section?

| Score | Criteria |
|-------|----------|
| **0** | No delta stated — TC repeats shared preconditions verbatim or says nothing about setup differences |
| **1** | Delta mentioned but vague — "with different status" without specifying which status |
| **2** | Delta is explicit — states exact changes from shared setup (e.g. "Delta: contract status = TERMINATED instead of ACTIVE from step 3") |

**Anti-patterns:**
- Two TCs with identical `Preconditions:` text → both score 0
- Negative TC with no delta explaining why the scenario should fail → score 0

**Special case:** If a TC genuinely uses the shared setup exactly as-is, it MUST state: "Delta: none (uses shared setup exactly as-is)." — this scores 2.

---

## Axis 5: Risk Coverage (0–2)

Does this TC cover a **risk identified by cross-dependency analysis**?

| Score | Criteria |
|-------|----------|
| **0** | No connection to any identified risk, integration point, or what_could_break item |
| **1** | Loosely related to a risk area but doesn't directly test the failure mode |
| **2** | Directly tests a specific risk: an integration point, a downstream consumer, a contract boundary, or a regression scenario from cross_dependency_data |

**Note:** Not every TC must score 2 here. Happy-path TCs that verify core functionality may score 0–1 on this axis and still pass overall if other axes are strong. However, a test suite with **zero** TCs scoring 2 on this axis indicates missing regression coverage.

---

## Axis 6: Readability (0–2)

Is the TC **clear, well-structured, and human-readable** without requiring prior context?

| Score | Criteria |
|-------|----------|
| **0** | Confusing — unclear steps, unexplained jargon, missing context, inconsistent formatting |
| **1** | Readable with effort — steps are present but could be clearer; some jargon without explanation |
| **2** | Immediately clear — full sentences, logical step order, no unexplained abbreviations, a tester can execute without asking questions |

---

## Scoring Summary

| Total Score | Verdict |
|-------------|---------|
| **10–12** | Excellent — production-ready |
| **8–9** | Good — passes threshold, minor improvements possible |
| **6–7** | Below threshold — needs rewrite on failing axes |
| **0–5** | Poor — significant rewrite needed |

## Process

1. Score each TC independently on all 6 axes.
2. Sum the scores (max 12).
3. TCs scoring **< 8** must be rewritten (max 2 rounds).
4. After rewrites, any TC still < 8 is flagged to the user with the failing axis and reason.

## Output format

```
TC-BE-1 (Positive): <title>
  Axis 1 Intent uniqueness:     X/2
  Axis 2 Observable expected:   X/2
  Axis 3 Endpoint specificity:  X/2
  Axis 4 Delta clarity:         X/2
  Axis 5 Risk coverage:         X/2
  Axis 6 Readability:           X/2
  Total: X/12  PASS | FAIL
  Failing axes: <names + brief reason>
  Suggested fixes: <concise rewrite suggestions>
```
