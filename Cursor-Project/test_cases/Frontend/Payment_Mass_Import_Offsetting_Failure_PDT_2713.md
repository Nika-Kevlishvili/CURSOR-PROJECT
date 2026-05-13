# Payment mass import — UI process visibility, failures, and `:20:` parity (PDT-2713)

**Jira:** PDT-2713  
**Type:** Bug  
**Summary:** Verify Operations/Receivables Portal UI for **payment mass import** processes: file staging, **process start**, **processed_record_info** grids, error text for failed offsetting, mixed vs all-failed outcomes, and parity with evidence from process **1781** (PreProd).

**Scope:** **Integration points** from cross-dependency: FTP path is server-side; Portal must present **ProcessCreated** lifecycle, process detail, **processed record** list with **record_identifier** including Cyrillic outgoing document **РИ 1100103788**, **success** flag, **`record_identifier_version`** when present, downloadable source file. Playwright should map each step to **`await test.step('...')`** mirroring UI hops (login → navigate → assert).

---

## Test data (preconditions)

- **Environment:** PreProd (**read-only** parity checks) **or** Test with fixtures.

1. **Authenticate in the Phoenix Portal**: open base URL per `.env`, submit **username/password** (`PORTAL_USER` / `PASSWORD` pattern), verify landing dashboard loads (JWT/session established — assert via network log or authenticated shell element).
2. **Role / permissions**: use an account possessing **PROCESS_VIEW** (**or** SU variants) AND **PROCESS_START** (**or** START_SU) plus **mass import execution** privileges per product IAM matrix — resolve exact role names from Confluence onboarding **or** admin assignment UI.
3. **Entity chain** (mirror Backend file when UI drives creation): alternatively **pre-create** via API helpers per mandatory creation-step rule and only **verify UI** reflective state:
   - Create customer via **`POST /customer`** (**ACTIVE**) — capture **`customerNumber`** used in TXT.
   - Create POD via **`POST /pod`**, product/terms/price via **`POST`** chain, contract via **`POST /product-contract`**, billing artifacts through **`POST /billing-run`** completion so an **open invoice** expects payment (**same chain as Backend steps 2–9**).
4. Create or open an existing **Payment mass import process** (`processId` captured from process list filters **or** from incident ticket **1781** sandbox clone).
5. **Fixture file**: sanitized copy analogous to **`7fa8668c-f53f-4d61-9e35-6fe354a52207_BG42_26.03.2026_test.txt`** (bank format with `:20:`) stored locally for upload dialogs.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Process list surfaces payment mass import **ProcessCreated** entries with correct domain/type badges

**Description:** Validates **processed_record visibility** precondition at list level — user can locate new process rows after **`ProcessCreated` event** ingestion (timing may require refresh/poll).

**Preconditions:**
1. Complete Shared step **2** — operator can view Processes.
2. A payment mass import process exists (**step 4**).

**Test steps:**
1. **Navigate**: menu → **Operations / Processes** (**exact menu label per locale** — use English UI if selectable).
2. **Filter**: apply filters for domain/type including **payment** / mass import equivalents; apply **created date** narrow window around test run **or** search by **`processId`** if search box exposed.
3. **Assert**: row renders **identifier**, **status** (Created/Running/Done variants), timestamps.
4. **Optional `test.step`**: intercept UI network (**Playwright**) for process list **`GET`** — assert **HTTP 200** and non-empty **`content`**/`items` slice contains target **`processId`**.

**Expected test case results:** Process appears without console errors; user can drill into **`/process/:id`** detail route (**exact path per build**).

**References:** **integration_points**: `processed_record_info` visibility extends from list/detail navigation.

---

### TC-FE-2 (Positive): Upload payment mass import file through Portal **→** bind to **`processId`**

**Description:** Mirrors **`POST /mass-import/payment/files/upload`** UX — multipart picker, optional **date**, optional **collection channel** controls.

**Preconditions:**
1. Shared steps **2–5**; **`processId`** known.
2. **UNLOCKED** payment package/channel selection available in wizard if UI mandates.

**Test steps:**
1. Open process detail (`processId`).
2. **`test.step`: Select file** dialog — attach **fixture TXT** (**absolute path from repo / CI artifact**).
3. If UI exposes **processing date** and **collection channel** dropdown populated from **`filterForPaymentMassImport`** — select values aligning with precondition **paymentDate** (**step** per field).
4. Confirm **Upload**/ **Save**.
5. **Assert** success toast/snackbar (**message text** substring **accepted**/**queued**/**uploaded**) **or** inline green status per design system.

**Expected test case results:** No validation blocking; **`file_url`/attachment** indicators appear (filename, size, checksum link if exposed); UI ready for **Start**.

**References:** **FTP/file path** eventual server path may not be shown verbatim — optional column **Imported file**.

---

### TC-FE-3 (Positive): **Start process** execution from UI **`PUT /process/{id}/start` equivalent**

**Preconditions:**
1. File uploaded (**TC-FE-2**) and process status **Start-able**.

**Test steps:**
1. Click **Start**/ **Run import** (**button label** localized).
2. Observe spinner / **running** pill; optionally listen to **`GET /process/{id}` polling** network every N seconds (**Playwright**: `waitForResponse` predicate).
3. Wait until terminal **Succeeded**/**Completed**/**Finished** or actionable **Paused/Error**.

**Expected test case results:** Process transitions without client-side exceptions; **`PUT /process/{id}/start`** emitted with **HTTP 200** (**CheckResponse**/status assertion).

---

### TC-FE-4 (Positive): Processed records grid — successful row displays **green**/`success=true` affordance & links to payment

**Preconditions:**
1. Companion automated/API run produced **mixed** outcome with **≥1 success** (**TC-BE-4** prerequisite data).

**Test steps:**
1. Open **Processed records**/ **Journal** tab on process detail.
2. Locate row matching known **business identifier**.
3. **Assert** **`success`** column/icon (**checkmark** vs **failure** glyph).
4. If UI hyperlink **Open payment**, click-through → payment detail asserts **matching amount/date**.

**Expected test case results:** Clear visual differentiation; deeplink (**URL** contains `/payment/` or drawer id slug) resolves.

---

### TC-FE-5 (Positive): Download original import file (**`download-mass-import-file`**) via UI affordance

**Preconditions:**
1. Upload completed.

**Test steps:**
1. Click **Download source file**/ **Imported file**.
2. **Assert** filesystem save dialog or binary response (`application/force-download`); checksum vs local fixture (**optional hashed compare**).

**Expected test case results:** File bytes match (**TC-BE-5** parity).

---

### TC-FE-6 (Positive): **`GET /process/{id}/report/download`** — export **XLSX** from UI Reports button

**Preconditions:**
1. Process finalized with ≥1 processed row.

**Test steps:**
1. Click **Download report** (**multi-sheet excel** picker if modal asks type).
2. Open workbook (automation optional: parse first sheet RowCount failures/success counts).

**Expected test case results:** Workbook aligns with **`processed_record_info`** tallies (**mixed** TC expectation).

---

### TC-FE-7 (Negative): Failed automatic offset row shows **`Automatic payment offsetting out failed;`** verbatim (localized wrapper ok) and **`record_identifier` = `РИ 1100103788`**

**Description:** Validates **failure transparency** aligning with incident row evidence (`success=false`, **`record_identifier_version=null`** visually blank **or dash**).

**Preconditions:**
1. Environment reproducing **TC-BE-7** Backend failure (**DB/procedure**) **or** PreProd **`processId=1781`** **read-only** verification with approval.
2. Operator has **PROCESS_VIEW**.

**Test steps:**
1. Open process detail for failing run (**1781** **or** lab clone).
2. Filter processed grid by **`record_identifier`** (**cyrillic**/Unicode normalization — assert cell raw text).
3. Read **Error**/ **Reason** column; expand tooltip if truncated.

**Expected test case results:** Error text **contains substring** **`Automatic payment offsetting out failed;`** (**full string** tolerance if translation wrapper prefixes common prefix). **`record_identifier_version`** column displays **blank**/`—`/`null` per UI null rendering rules when source null. **No** payment hyperlink for that failure row.

**Actual result:** Matches PreProd artifact path & row failure evidence.

---

### TC-FE-8 (Negative): **Mixed batch** UX — aggregated summary shows partial success while failed rows remain expanded

**Preconditions:**
1. **TC-BE-4** data.

**Test steps:**
1. Open summary banner / statistics strip on process (**e.g.** **Succeeded: 1 / Failed: 1**).
2. Toggle **Failures only** filter if exposed.

**Expected test case results:** Counts reconcile; pagination unaffected; clearing filter restores all rows.

---

### TC-FE-9 (Negative): **All failures** scenario — banners, destructive coloring, actionable next steps (**no phantom success toast**)

**Preconditions:**
1. **TC-BE-12** dataset.

**Test steps:**
1. Run import with only invalid rows.
2. Inspect top-of-page **`Alert`** severity **error/danger**.
3. Ensure **notifications** (**Process_payment_mass_import_completed/error** equivalents) semantics: user **not misled** by success toast (**assert absence** `Success` toast when **`success`** count zero).

**Expected test case results:** Clear **failure-first** UX; downloadable error report reachable.

---

### TC-FE-10 (Negative): Operator **without PROCESS_VIEW / START** cannot open others’ payment mass imports (**403**/redirect)

**Preconditions:**
1. Login as **narrow** role stripped of **PROCESS_VIEW_SU**.

**Test steps:**
1. Deep-link **`/process/{foreignId}`** with known valid id (**guarded trial**).

**Expected test case results:** Access denied route **or** **403** skeleton; message references permissions; nothing leaks row PII (**record_identifier**) **or** sanitized message only.

---

## References

- **Jira:** PDT-2713  
- **Backend twin:** `test_cases/Backend/Payment_Mass_Import_Offsetting_Failure_PDT_2713.md`  
- **Incident evidence:** **`process_id` 1781**, **`file_url`** path `.../payment_mass_import/2026-04-30/7fa8668c-f53f-4d61-9e35-6fe354a52207_BG42_26.03.2026_test.txt`, failed row **`Automatic payment offsetting out failed;`**, **`record_identifier='РИ 1100103788'`**, **`record_identifier_version=null`**, DB **no payment** for failing row.

