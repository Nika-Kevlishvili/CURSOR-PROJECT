# Test Case Quality Rubric (STRICT 0-100 SCORING)

**Purpose:** Every test case generated or reviewed in this project is scored against this rubric before being saved. The rubric is the single source of truth for what makes a test case "good enough to keep." It is used by:
- `test-case-generator` skill and subagent (self-scoring before file write)
- `test-case-quality-validator` subagent (second-pass independent review)
- `/test-case-quality` command (ad-hoc quality check on existing files)

**CRITICAL:** This rubric enforces **strict quality standards**. The validator must be a **harsh critic** — no leniency, no "good enough" passes. If a TC has any weakness, points are deducted.

---

## Scoring model (0-100 scale)

Score each TC on **10 axes**. Maximum total: **100 points**. **Minimum to pass: 80/100**.

Any TC scoring below 80 MUST be rewritten. No exceptions.

| Axis | Max Points | Scoring Criteria |
|------|-----------|------------------|
| **1. Intent uniqueness** | 10 | **10**: TC has a crystal-clear, unique verification goal that no other TC in the file covers; title alone tells what's being tested. **7**: Title unique but description overlaps slightly with another TC. **4**: Title is generic ("Error case", "Invalid input") but content differs. **0**: Duplicate of another TC or so vague it overlaps with ≥2 others. |
| **2. Observable expected result** | 15 | **15**: Expected result includes: exact HTTP status code + specific response body field(s) and value(s) + side effect verification + error message text (for negatives). **10**: Has status code + some response details but missing side effects or error semantics. **5**: Only status code ("HTTP 200") with no other detail. **0**: "System works correctly", "operation succeeds", "no error", or any non-verifiable statement. |
| **3. Endpoint specificity** | 12 | **12**: Every API call has HTTP method + full path + all key payload fields with example values/types. **8**: Method + path present, payload fields listed but no example values. **4**: Endpoint mentioned but parameters described as "{parameters}" or "required fields". **0**: No API endpoint named in steps. |
| **4. Scenario differentiation (STANDALONE)** | 10 | **10**: Negative/variant TC's **Preconditions** list the **full chain** AND state the exact parameter/value/status that differs from the positive case (e.g. step 7 omits payment; step 3 uses `amountExcludingVat: 4.16`). **6**: Difference present but buried. **3**: Inferable only by diffing another TC. **0**: Identical preconditions to positive with no scenario-specific change. **Legacy read-only:** `Apply Test data steps X–Y` allowed only when `## Test data` contains the full chain and slice + scenario delta are unambiguous — score 6–8, not 10. **New files:** `Apply Test data steps` without full chain in same TC → 0. |
| **5. Risk coverage (cross_dep)** | 10 | **10**: TC explicitly names and tests ≥1 item from `what_could_break` or `integration_points`. **6**: TC addresses a cross-dep risk implicitly. **3**: TC is in unrelated domain (stated clearly why cross-dep N/A). **0**: Cross-dep data exists but TC ignores all risks. |
| **6. Precondition completeness** | 15 | **15**: Every entity creation step has: endpoint + HTTP method + key parameters + expected response. Full chain documented. **10**: Most entities have creation details but 1-2 are vague. **5**: Says "entity X exists" without explaining how to create it. **0**: Preconditions missing or say "assume data exists". |
| **7. Step granularity** | 8 | **8**: Each step is ONE atomic action; all steps numbered; no compound steps. **5**: Steps numbered but some combine 2 actions. **2**: Steps unnumbered or mix multiple actions. **0**: Steps are paragraphs, not discrete actions. |
| **8. Assertion specificity** | 10 | **10**: Each assertion checks a specific field/value/state with exact comparison. **6**: Assertions present but some are vague ("response is valid"). **3**: Only status code asserted, no body/state checks. **0**: No assertions or "verify it works". |
| **9. Error semantics (negatives only)** | 5 | **5**: Negative TC specifies exact error code + error message fragment + which field/constraint failed. **3**: Error code specified but message/field missing. **0**: "Any 4xx is acceptable" or no error detail. N/A for positive TCs (award 5). |
| **10. Readability & clarity** | 5 | **5**: No jargon without explanation; a new tester could execute from steps alone. **3**: Some abbreviations unexplained but mostly clear. **0**: Requires domain expertise to understand; unexplained acronyms. |

---

## Scoring rules (STRICT)

1. **Read every field** of the TC: Title, Description, Preconditions, Steps, Expected result.
2. **Be harsh**: If something is "almost good enough", deduct points. Do not round up.
3. **Deduct for every weakness**: Each missing detail costs points. Cumulative deductions apply.
4. **No partial credit for missing information**: If a required element is absent, score 0 for that sub-criterion.
5. **Sum all axes**. If total < 80, the TC **MUST be rewritten**. No exceptions.
6. On rewrite, rescore from scratch. Max **3 rewrite iterations**. After 3 failures, escalate to user with all failing axes.

## Pass/Fail thresholds

| Score | Verdict |
|-------|---------|
| **90-100** | EXCELLENT — High-quality TC, ready for Playwright conversion |
| **80-89** | PASS — Acceptable quality, minor improvements optional |
| **70-79** | FAIL — Must rewrite; has significant gaps |
| **60-69** | FAIL — Major quality issues; substantial rewrite needed |
| **Below 60** | FAIL — Reject entirely; start from scratch |

**Output format for the validator:**

```
TC-BE-3 (Negative): Create billing run with missing contractId field
  Axis 1  Intent uniqueness:          10/10
  Axis 2  Observable expected:         8/15  ← Missing error message text and failing field name
  Axis 3  Endpoint specificity:       12/12
  Axis 4  Scenario differentiation:  10/10  ← step 7 omits payment; contractId null in payload
  Axis 5  Risk coverage:               6/10  ← Implicitly covers validation, not explicitly linked to cross-dep
  Axis 6  Precondition completeness:  15/15
  Axis 7  Step granularity:            8/8
  Axis 8  Assertion specificity:       6/10  ← Asserts 400 but not specific error field
  Axis 9  Error semantics:             3/5   ← Missing error message fragment
  Axis 10 Readability:                 5/5
  ─────────────────────────────────────────
  TOTAL: 83/100  ✓ PASS
  
  Weaknesses (for optional improvement):
    - Axis 2: Add expected error message text (e.g., "contractId must not be null")
    - Axis 8: Assert the specific validation error field in response body

TC-BE-7 (Negative): Invalid input
  Axis 1  Intent uniqueness:           0/10  ← "Invalid input" is generic; 3 other TCs could have same title
  Axis 2  Observable expected:         0/15  ← "system rejects the request" — not verifiable
  Axis 3  Endpoint specificity:        4/12  ← Endpoint named but no payload fields
  Axis 4  Scenario differentiation:   0/10  ← Identical setup to positive; no scenario-specific change
  Axis 5  Risk coverage:               0/10  ← Cross-dep provided risks, TC addresses none
  Axis 6  Precondition completeness:   5/15  ← "An active customer exists" — no creation steps
  Axis 7  Step granularity:            5/8   ← Step 2 combines login + navigation
  Axis 8  Assertion specificity:       0/10  ← No specific assertions
  Axis 9  Error semantics:             0/5   ← No error code, message, or field
  Axis 10 Readability:                 3/5   ← "LPF" acronym unexplained
  ─────────────────────────────────────────
  TOTAL: 17/100  ✗ FAIL — REJECT AND REWRITE
  
  MANDATORY fixes before resubmission:
    - Axis 1: Rename to specific scenario (e.g., "Billing run rejected when contractId is null")
    - Axis 2: Add "HTTP 400; response.errors[0].field='contractId'; message contains 'required'"
    - Axis 4: Document full chain; step 7 must omit payment / use null contractId (not generic "invalid input")
    - Axis 6: Document full entity creation chain with endpoints and parameters
    - Axis 9: Specify exact error semantics expected
```

---

## Anti-pattern catalog (INSTANT POINT DEDUCTIONS)

These are **automatic deductions**. If any of these patterns are detected, the corresponding axis score is IMMEDIATELY reduced.

| Anti-pattern | Axis | Deduction | Why it fails |
|---|---|---|---|
| Title = "Invalid input" or "Error case" or "Negative test" | 1 | -10 (to 0) | Not unique; cannot tell what's being tested |
| Expected = "The system works correctly." | 2 | -15 (to 0) | Not verifiable |
| Expected = "The operation succeeds." | 2 | -15 (to 0) | Not verifiable |
| Expected = "No error occurs." | 2 | -15 (to 0) | Not verifiable |
| Expected = "HTTP 200" with nothing else | 2 | -10 | Status code alone is insufficient |
| Expected = "Request is successful" | 2 | -15 (to 0) | Not verifiable |
| Expected (negative) = "any 400 means pass" | 2, 9 | -10 each | Does not validate business rejection semantics |
| Negative TC passes on `401/403` auth failure | 2, 5 | -10 each | Auth failure is not business rejection (unless auth is scope) |
| Negative TC uses wrong URL and accepts `404` | 2, 3 | -12 each | Tests routing mistake, not feature behavior |
| Steps say "Call the endpoint" with no method/path | 3 | -12 (to 0) | Which endpoint? Which method? |
| Payload described as "{parameters}" or "required fields" | 3 | -8 | No actual field names |
| Preconditions say "An active customer exists." | 6 | -15 (to 0) | No creation steps — FORBIDDEN |
| Preconditions say "Assume X is set up" | 6 | -15 (to 0) | No creation steps — FORBIDDEN |
| Preconditions say "entity exists" without how | 6 | -10 | Must document creation endpoint + params |
| Negative TC has identical preconditions to positive (no scenario-specific change) | 4 | -10 (to 0) | Cannot tell what triggers the failure |
| New TC uses `Apply Test data steps` without full chain in same Preconditions block | 4, 6 | -10 each | Violates TC-STANDALONE-PRE.0 |
| Legacy TC uses `Apply Test data steps` but `## Test data` lacks full creation chain | 6 | -10 | Reference target missing |
| cross_dep provided risks but TC covers none | 5 | -10 (to 0) | Ignores known regression risks |
| Step = "Verify the result is correct." | 7, 8 | -8, -10 | Not atomic; "correct" undefined |
| Two or more actions in one step | 7 | -6 | Hard to isolate failures |
| Unexplained acronym (LPF, MLO, JPA, etc.) | 10 | -5 (to 0) | Requires tribal knowledge |
| Assertion = "verify response is valid" | 8 | -10 (to 0) | What does "valid" mean? |
| Assertion = "check the result" | 8 | -10 (to 0) | Check what? Against what? |
| Missing error message in negative TC | 9 | -5 (to 0) | Error semantics incomplete |
| TC title, description, step 1 are identical | 1, 10 | -5 each | Redundant content |

---

## Strict enforcement rules

1. **Zero tolerance for vague language**: "works", "succeeds", "correct", "valid", "proper" — all score 0 for that axis.
2. **Every entity must have creation steps**: "Exists" is not a precondition; HOW to create it is.
3. **Every negative TC must show scenario-specific setup** in its full Preconditions chain (what differs from the positive case — amounts, omitted entities, invalid fields).
4. **Every assertion must be specific**: Field name + expected value + comparison type.
5. **Cross-dep risks must be explicitly addressed**: If cross-dep data was provided, at least 1 risk must be tested.

---

## Rewrite iteration limits

| Iteration | Action |
|-----------|--------|
| 1st fail (<80) | Return detailed feedback with all failing axes; demand rewrite |
| 2nd fail (<80) | Return feedback + specific examples of what's missing; demand rewrite |
| 3rd fail (<80) | **ESCALATE TO USER** with full scoring breakdown; block further generation until user intervenes |

**No more than 3 iterations.** If a TC cannot reach 80/100 after 3 attempts, it indicates a fundamental problem with the test case design or the source requirements.

---

## Relationship to other documents

- **Template:** `Cursor-Project/config/template/Test_case_template.md` — structure and **TC-STANDALONE-PRE.0** preconditions.
- **TC structure rule:** `.cursor/rules/workspace/test_cases_structure.mdc` — governs folder layout, numbering, coverage.
- **Validator subagent:** `.cursor/agents/test-case-quality-validator.md` — applies this rubric independently after generation.
- **Generator skill:** `.cursor/skills/test-case-generator/SKILL.md` — applies this rubric during self-check before file write.
