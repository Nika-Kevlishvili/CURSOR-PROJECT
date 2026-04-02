# TestCaseGeneratorAgent Report — PDT-2474 Regeneration

**Date:** 2026-04-02  
**Task:** Regenerate test cases for PDT-2474 using updated rules (two-folder layout + mandatory creation-step preconditions)

---

## Workflow Executed

1. **Rule 35 Step 1 — Cross-Dependency Finder:** Reused existing cross-dependency report from `Cursor-Project/cross_dependencies/2026-04-02_PDT-2474.json` (generated earlier in this session).
2. **Rule 35 Step 2 — Playwright Instructions:** Read all 4 instruction files from `Cursor-Project/config/playwright_generation/playwright instructions/` in correct order: `project-description.md`, `general-rules.md`, `test-writing-rules.instructions.md`, `SKILL.md`.
3. **Rule 35 Step 3 — Test Case Generation:** Generated 69 test cases (60 Backend + 9 Frontend) following the updated rules.

## Files Generated

| File | TCs | Lines |
|------|-----|-------|
| `test_cases/Backend/Zero_amount_liability_receivable.md` | TC-BE-1 through TC-BE-60 | ~1038 |
| `test_cases/Frontend/Zero_amount_liability_receivable.md` | TC-FE-1 through TC-FE-9 | ~141 |

## READMEs Updated

- `test_cases/README.md` — root index updated to two-folder layout
- `test_cases/Backend/README.md` — lists the Backend file
- `test_cases/Frontend/README.md` — lists the Frontend file

---

## Comparison: Old vs New

### Old file (before regeneration)

- **Location:** `test_cases/Zero_amount_liability_receivable.md` (single flat file)
- **Structure:** One file with both Backend (TC-BE-1..60) and Frontend (TC-FE-1..9) sections
- **Total TCs:** 69 (60 BE + 9 FE)
- **Lines:** ~1225

### New files (after regeneration)

- **Location:** `test_cases/Backend/Zero_amount_liability_receivable.md` + `test_cases/Frontend/Zero_amount_liability_receivable.md`
- **Structure:** Two separate files — Backend-only and Frontend-only
- **Total TCs:** 69 (60 BE + 9 FE) — identical coverage
- **Lines:** ~1038 (Backend) + ~141 (Frontend) = ~1179 total

### Key differences

| Aspect | OLD (single file) | NEW (two files) |
|--------|-------------------|-----------------|
| **File structure** | Single `test_cases/Zero_amount_liability_receivable.md` with Backend + Frontend sections | Two files: `Backend/` and `Frontend/` sub-folders |
| **Preconditions format** | Generic: "An active customer exists", "A product contract is linked" | Creation-step: "Create customer via `POST /customer` (type: PRIVATE, status: ACTIVE)" |
| **Entity chain** | Vague, often missing intermediate steps | Full numbered chain with endpoint, parameters, and cross-references |
| **Billing chain** | "An active customer with a linked active product contract exists" (1 line) | 9 numbered steps: customer → POD → product → terms → price component → contract → energy data → billing run → execute |
| **Deposit** | "An active customer exists. A deposit is being created with amount > 0" | "Create customer via POST /customer (...). Create deposit via POST /deposit (amount: 200.00, linked to customer from step 1)" |
| **Invoice cancellation** | "An active customer has an existing invoice with total amount > 0" | Full 12-step chain from customer creation through billing to invoice to cancellation |
| **Payment reversal** | "An active customer has a recorded payment with amount > 0" | Full 12-step chain from customer to billing to payment to reversal |
| **Frontend preconditions** | "User is logged into the portal with permissions" | "Open portal login page at {BASE_URL}/login, enter credentials, click Login. Create customer via POST /customer (...). Navigate to customer detail → Receivables tab" |
| **Playwright alignment** | Not explicitly aligned | Steps reference HTTP methods, endpoints, status codes; granularity maps to test.step(); CheckResponse-style assertions |

### Same (no change)

- **Test case count:** 60 Backend + 9 Frontend = 69 total (identical)
- **Test case IDs:** TC-BE-1..60 and TC-FE-1..9 (same numbering)
- **Coverage scope:** All flows — manual, billing run, deposit, LPF, rescheduling, payment, action, orders, cancellation, reversal, MLO, compensation, VAT base, disconnection, listener, negative validation, correction
- **Positive/Negative split:** Same distribution of positive and negative test cases

---

## Conclusion

The regenerated test cases fix both critical issues:
1. **Structure:** Properly split into `Backend/` and `Frontend/` sub-folders as required
2. **Preconditions:** Every entity creation step now includes the API endpoint, parameters, and cross-references — no more vague "entity exists" shortcuts

Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent, PhoenixExpert
