# PDT-2854 Detailed Test Report

Run context
- Jira key: PDT-2854
- Date: 2026-05-20
- Branch: cursor
- Spec path: `tests/cursor/PDT-2854-pod-active-two-contracts.spec.ts`
- Source JSON: `playwright-report.json`
- Command: `npx playwright test tests/cursor/PDT-2854-pod-active-two-contracts.spec.ts --workers=1`

Summary
- Total in scope: 2
- Passed: 1
- Failed: 1
- Skipped: 0
- Duration: ~23.6s
- Test case IDs: TC-BE-1, TC-BE-2

Test cases

TC-BE-1
- Status: FAILED
- Expected Result: After manual activation on contract A and mass activation (Supply), POD must **not** become active on contract B.
- Actual Result: Contract B (78764) already had POD active on Dev (Valeri repro state). Manual/mass steps were skipped; final assertion failed — POD is active on contract B (`activationDate` set, no deactivation).
- Notes: Failure confirms unfixed bug PDT-2854 on Dev using Valeri objects.
- Portal data links:
  - POD identifier: `32Z4TAXTEST02021` (id: 323798)
  - Contract A (73460): http://10.236.20.11:8080/energy-product-contracts/preview/point-of-delivery?id=73460
  - Contract B (78764): http://10.236.20.11:8080/energy-product-contracts/preview/point-of-delivery?id=78764
  - Supply activation (mass UI): http://10.236.20.11:8080/supply-activation

TC-BE-2
- Status: PASSED
- Expected Result: With contract A **TERMINATED** but POD row still active on A, manual activation on contract B must be **rejected** and POD must remain inactive on B.
- Actual Result: Manual activation on B returned **400** — `Pod is already active from this date;`. POD on contract B remained inactive after the attempt.
- Notes: Mass import step removed per scope — manual path only.
- Portal data links:
  - POD identifier: `32XZBVNMIYQPF4038499576` (id: 337596)
  - Contract A (78863): http://10.236.20.11:8080/energy-product-contracts/preview/point-of-delivery?id=78863
  - Contract B (78864): http://10.236.20.11:8080/energy-product-contracts/preview/point-of-delivery?id=78864

Overall notes
- Environment: Dev (`http://10.236.20.11:8091` API / `8080` portal)
- TC-BE-1 validates Valeri repro (manual A + mass) — currently fails while bug exists.
- TC-BE-2 validates terminated-A + manual-on-B rejection — passes (manual validation works).
