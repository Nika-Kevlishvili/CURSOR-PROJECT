---
name: test-case-quality-validator
description: STRICT test case .md validator — 10 axes, 0–100 total, pass ≥80/100. Rule 35 Step 2.5 and HandsOff Step 3.5. Max 3 rewrites. READ-ONLY.
---

# Test Case Quality Validator Skill

**Subagent (report template):** `.cursor/agents/test-case-quality-validator.md`  
**Rubric:** `Cursor-Project/docs/test_case_quality_rubric.md`

## When to apply

- After test-case-generator saves Backend (+ Frontend when scope includes it).
- HandsOff Step 3.5 (mandatory before Playwright).
- `/test-case-quality <Topic_name>`

## Inputs

- `topic_name` — e.g. `Invoice_cancellation`
- `backend_path` — default `Cursor-Project/test_cases/Backend/<topic>.md` (**required**)
- `frontend_path` — optional; score only if file exists — **do not fail** Backend-only scope

## Workflow

### Step 1 — Read rubric (mandatory first)

Read **`test_case_quality_rubric.md`** completely. **10-axis, 0–100**, pass **≥80**. Apply anti-pattern catalog.

### Step 2 — Read TC file(s)

Extract every TC-BE-N from Backend; TC-FE-N from Frontend when present.

### Step 3 — Score each TC (strict)

Start at max per axis; deduct for every weakness.

**Axis flunks (examples):**
- Vague expected ("succeeds", "works correctly") → Axis 2 = 0
- "Customer exists" without creation steps → Axis 6 = 0
- Negative TC same setup as positive without delta → Axis 4 = 0
- Legacy `Apply Test data steps` without inlined chain → Axis 6 = 0 (new files must be STANDALONE)

Per-TC block: all 10 axis scores + TOTAL + PASS/FAIL + mandatory fixes if FAIL.

### Step 4 — Anti-pattern scan

Re-scan rubric catalog; rescore if missed deductions.

### Step 5 — Summary verdict

Backend table + optional Frontend table + OVERALL VERDICT + failed TC list + iteration (1–3).

### Step 6 — Return to parent

- `validation_passed`: true only if **all** TCs ≥80
- `failed_tcs`, `fixes_required`, `iteration`
- Parent re-invokes test-case-generator; after 3 failures → **BLOCK** user escalation

## READ-ONLY

Do not modify TC files.

## Footer

**Confidence** (CONF.1 — Three-Zone): Include `**Confidence: XX% (ZONE)**` with evidence factors. Use **GO** (≥ 85%), **CAUTION** (55–84% + assumptions list), or **STOP** (< 55% — escalate to user). See `.cursor/rules/scoring/confidence_scoring_matrix.mdc`.

`Agents involved: test-case-quality-validator`
