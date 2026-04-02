# TestCaseGeneratorAgent Report

**Date:** 2026-04-02  
**Time:** 21:34  
**Jira:** PDT-2474 (Phoenix Delivery)  
**Task:** Generate test cases for "Liabilities and receivables shouldn't be generated with amount zero"

---

## Workflow Executed

### Step 1: Cross-Dependency Finder (Rule 35)
- **Agent:** CrossDependencyFinderAgent
- **Scope:** All liability and receivable generation flows in Phoenix
- **Result:** Comprehensive dependency analysis completed
- **Key findings:**
  - `ZeroAmountValidationListener` (`@PrePersist`) exists as a global safety net
  - `CustomerLiabilityRequest` has `@DecimalMin(value="0", inclusive=false)` on `initialAmount`
  - `CustomerReceivableRequest` has `@Positive` on `initialAmount`
  - Multiple flows have existing zero-amount guards (VAT base, invoice reversal, payment offsetting)
  - Risk: flows that still construct zero-amount entities will fail at persistence layer

### Step 2: Playwright Instructions Pack (Mandatory)
- Read in order: `project-description.md`, `general-rules.md`, `test-writing-rules.instructions.md`, `SKILL.md`
- Test steps aligned with `test.step()` granularity, HTTP method/endpoint/status checks

### Step 3: Test Case Template
- Read `Cursor-Project/config/template/Test_case_template.md`
- Applied data completeness rule for preconditions
- Backend/Frontend split with TC-BE-N / TC-FE-N numbering

### Step 4: Test Case Generation
- **Confluence:** Not accessible in this session (no granted cloudId); proceeded with Jira + codebase evidence per Rule 35a
- **Codebase analysis:** PhoenixExpert reviewed `CustomerLiabilityService`, `CustomerReceivableService`, `ZeroAmountValidationListener`, `BillingRunStartAccountingInvokeService`, and all related services

## Output

**File:** `Cursor-Project/test_cases/Zero_amount_liability_receivable.md`

### Test Case Count
| Section | Positive | Negative | Total |
|---------|----------|----------|-------|
| Backend (TC-BE) | 30 | 30 | 60 |
| Frontend (TC-FE) | 4 | 5 | 9 |
| **Total** | **34** | **35** | **69** |

### Coverage by Flow

**Liability generation flows covered:**
1. Manual liability creation (TC-BE-1, TC-BE-2)
2. Billing run invoice (TC-BE-5, TC-BE-6)
3. Deposit (TC-BE-9, TC-BE-10)
4. Late payment fine (TC-BE-13, TC-BE-14)
5. Rescheduling (TC-BE-15, TC-BE-16)
6. Payment (TC-BE-17, TC-BE-18)
7. Action (TC-BE-21, TC-BE-22)
8. Goods order invoice (TC-BE-23, TC-BE-24)
9. Service order invoice (TC-BE-25, TC-BE-26)
10. Invoice cancellation (TC-BE-27, TC-BE-28)
11. Invoice reversal (TC-BE-31, TC-BE-32)
12. Payment reversal (TC-BE-35, TC-BE-36)
13. MLO reversal (TC-BE-39, TC-BE-40)
14. Disconnection of power supply (TC-BE-53, TC-BE-54)
15. VAT base adjustment (TC-BE-51, TC-BE-52)

**Receivable generation flows covered:**
1. Manual receivable creation (TC-BE-3, TC-BE-4)
2. Billing run invoice (TC-BE-7, TC-BE-8)
3. Deposit (TC-BE-11, TC-BE-12)
4. Invoice cancellation (TC-BE-29, TC-BE-30)
5. Payment / overpayment (TC-BE-19, TC-BE-20)
6. Invoice reversal (TC-BE-33, TC-BE-34)
7. Payment reversal (TC-BE-37, TC-BE-38)
8. MLO reversal (TC-BE-41, TC-BE-42)
9. LPF reversal (TC-BE-43, TC-BE-44)
10. Rescheduling reversal (TC-BE-45, TC-BE-46)
11. Credit note (TC-BE-47, TC-BE-48)
12. Compensation (TC-BE-49, TC-BE-50)
13. Invoice correction (TC-BE-59, TC-BE-60)

**Edge cases / global safety net:**
- ZeroAmountValidationListener (TC-BE-55, TC-BE-56)
- Negative amount rejection (TC-BE-57, TC-BE-58)

**Frontend flows covered:**
- Manual liability form (TC-FE-1, TC-FE-2, TC-FE-5)
- Manual receivable form (TC-FE-3, TC-FE-4, TC-FE-6)
- Backend error display (TC-FE-7)
- Deposit creation form (TC-FE-8, TC-FE-9)

## Cross-Dependency Data Used
- **Source:** `Cursor-Project/cross_dependencies/2026-04-02_PDT-2474.json`
- **what_could_break items integrated:** Billing run batch failures, zero-total invoice persistence, deposit test endpoints, compensation paired create methods, inconsistent payment-api guards

## Files Modified
- `Cursor-Project/test_cases/Zero_amount_liability_receivable.md` (created)
- `Cursor-Project/test_cases/README.md` (updated index)

---

Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent, PhoenixExpert
