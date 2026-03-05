# Test Case Generation Report – PDT-2585

**Date:** 2026-03-02  
**Bug:** PDT-2585 – Frontend - URGENT: Billing - No Price component in a credit note  
**Workflow:** Rule 35 (cross-dependency-finder → test case generation)

## Delivered artifacts

| Artifact | Path |
|----------|------|
| Test case (main) | `Cursor-Project/test_cases/Flows/Billing/Credit_note_summary_after_reversal.md` |
| Billing flow README | `Cursor-Project/test_cases/Flows/Billing/README.md` |
| Flows README updated | `Cursor-Project/test_cases/Flows/README.md` (Billing row added) |
| Cross-dependency data | `Cursor-Project/cross_dependencies/2026-03-02_PDT-2585-credit-note-price-component.json` |

## Test cases summary

- **TC-1:** Credit note summary – price component names (main bug).
- **TC-2:** Credit note summary – Total volumes empty (not 0).
- **TC-3:** Credit note detailed data and preview (integration).
- **TC-4:** Regression – manual credit/debit note and invoice correction.
- **TC-5:** Regression – CSV/PDF export and regenerate-compensations.

## Test data

- Environment: Test (testapps.energo-pro.bg).
- Invoice id: 39650 (original).
- Credit note id: 46045 (after reversal).

Agents involved: CrossDependencyFinderAgent, TestCaseGeneratorAgent (direct implementation), PhoenixExpert (via cross-dependency).
