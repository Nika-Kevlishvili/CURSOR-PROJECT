# Contract version validity — Mass Email, Mass SMS, Penalty UI (PDT-2815)

**Jira:** PDT-2815 (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** Changes in processes impacted by version validity  

**Scope:** Frontend verification that import reports, mass-communication flows, and penalty action UI **surface backend rejection messages** and do not mask description-vs-code gaps found in backend testing. Attachment: **Using validity in 3 processes.docx** — FE should keep current UX; rejections visible in import/report areas.

**Environment:** Dev portal

**Linked:** PDT-2599

---

## Test data (preconditions)

Use the same data chain as `Backend/Version_Validity_Three_Processes_PDT_2815.md` (steps 1–13) prepared via API or back-office helpers before UI steps. Tester must have permissions: Mass Email, Mass SMS, Actions (penalty calculate).

---

## Frontend Test Cases

### TC-FE-1 (Negative): Mass Email — contract import report shows rejection for explicit DRAFT version

**Description:** Story 1 — user sees why row failed.

**Preconditions:**
1. Apply shared Test data with product contract DRAFT v1 and SIGNED v2.
2. Delta: Excel import file with row: contract number + explicit version `1` (DRAFT).

**Test steps:**
1. Log in to Dev portal with Mass Email permissions.
2. Open Mass Email creation flow → contract-based customer import.
3. Upload the file from preconditions.
4. Open import **report / error list** after processing.

**Expected test case results:** Row for contract appears as **failed/rejected**. User-visible text includes `not in status Valid/Signed by both sides` (or localized equivalent matching backend English message). User cannot add that row’s customer to send list without fixing version.

---

### TC-FE-2 (Negative): Mass Email — latest fallback failure when no SIGNED version

**Description:** Clear report when no eligible version exists.

**Preconditions:**
1. Apply shared Test data with product contract **DRAFT-only** versions.
2. Delta: import file with contract number only (no version column).

**Test steps:**
1. Upload import file in Mass Email contract import UI.
2. Review report.

**Expected test case results:** Report shows failure reason containing `has no version in status Valid/Signed by both sides`. No silent omission of row without explanation.

---

### TC-FE-3 (Positive): Mass Email — successful row shows eligible signed version in report

**Description:** Positive visibility for accepted imports.

**Preconditions:**
1. Apply shared Test data with explicit SIGNED version `2`.
2. Delta: import row with version `2`.

**Test steps:**
1. Upload file in Mass Email import.
2. Review successful rows in report.

**Expected test case results:** Row marked successful. Displayed version = `2` (or customer linked to signed detail). No error banner for that row.

---

### TC-FE-4 (Negative): Mass SMS — parse/import report shows same rejection semantics as email

**Description:** Story 2 FE parity.

**Preconditions:**
1. Apply shared Test data — service contract DRAFT v1.
2. Delta: SMS contract import row with explicit version `1`.

**Test steps:**
1. Open Mass SMS flow → contract import / parse step.
2. Upload file.
3. Review parse report.

**Expected test case results:** Rejection visible with same business meaning as Mass Email (non-eligible version). User cannot proceed with that recipient until corrected.

---

### TC-FE-5 (Negative): Penalty action — validation errors visible when version-scoped data missing

**Description:** Story 3 — user sees validation when penalty cannot calculate.

**Preconditions:**
1. Apply shared Test data — contract with no SIGNED version on execution date **or** missing price component for tag (TC-BE-8 / TC-BE-10 setup).
2. Delta: open Actions → Create/Edit action with penalty calculation.

**Test steps:**
1. Set execution date in UI to precondition date.
2. Trigger **Calculate penalty** (or equivalent control calling `GET /actions/calculate-penalty-amount`).
3. Observe inline errors / toast / modal messages.

**Expected test case results:** UI displays backend `infoErrorMessages` or validation list (e.g. version not found, price component messages). Amount field empty or blocked until resolved. No misleading calculated amount from wrong version.

---

### TC-FE-6 (Negative): Penalty action — POD from future version accepted in UI without warning (gap confirmation)

**Description:** Mirrors TC-BE-7 — if backend accepts future POD, UI likely shows no warning; documents end-to-end gap.

**Preconditions:**
1. Apply shared Test data — POD only on future contract version; respective signed version exists for execution date.
2. Delta: formula type 2/4/6/8 in action setup.

**Test steps:**
1. Attach future-only POD to action in UI.
2. Run penalty calculation.
3. Note whether UI shows POD validation error.

**Expected test case results (TO-BE per Jira):** UI should show error that POD does not belong to respective version.

**Actual result (likely AS-IS):** Calculation may succeed — record as **UI gap** aligned with backend `validatePods` future-version allowance; link to PDT-2815 defect.

---

### TC-FE-7 (Positive): All-customers mass communication — recipient list excludes DRAFT-only customers

**Description:** Story 1/2 all-customers mode in UI.

**Preconditions:**
1. Apply shared Test data — Customer A signed active contracts; Customer B DRAFT-only.
2. Delta: select “all customers with active contract” (or equivalent) in Mass Email or Mass SMS.

**Test steps:**
1. Start mass communication wizard.
2. Choose all-customers mode.
3. Preview recipient list before send/save draft.

**Expected test case results:** Customer B absent from preview. Customer A present. Count matches backend-eligible set only.

---

### TC-FE-8 (Positive): Mass SMS — latest fallback UI selects signed version when newer draft exists

**Description:** UI reflects backend latest SIGNED logic (TC-BE-3).

**Preconditions:**
1. Apply shared Test data — v2 SIGNED, v3 DRAFT newer start date.
2. Delta: import without version column.

**Test steps:**
1. Parse contract import in Mass SMS UI.
2. Inspect which version appears on successful row.

**Expected test case results:** UI/report shows **version 2** (signed), not version 3 draft.

---

## References

- Backend test cases: `test_cases/Backend/Version_Validity_Three_Processes_PDT_2815.md`
- Jira attachment: `Cursor-Project/config/jira/attachments/PDT-2815/Using validity in 3 processes.extracted.txt`
- Jira: PDT-2815
