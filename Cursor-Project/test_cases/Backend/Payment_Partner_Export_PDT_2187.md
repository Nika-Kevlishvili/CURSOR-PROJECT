# Payment Partner export TXT — one channel, one export file (PDT-2187)

**Jira:** PDT-2187  
**Type:** Customer Feedback  
**Summary:** Export Liabilities JOB writes references in a **single Payment Partner** collection-channel export (423-char rows) per User Story V 0.2 — one shared channel, **one** `POST /collection-channel/test-export-liabilities-job`, **one** output `.txt`; each TC verifies **its row** in that file.

**Scope:** Backend on **Dev**. Frontend not in scope.  
**Primary source:** `Cursor-Project/config/jira/attachments/PDT-2187/User Story V 0.2.extracted.txt`

**Execution model:** Run **Shared test data** once → run **Single export** once → run **TC-BE-1 … TC-BE-10** as row-level checks on the **same** export file (no per-TC channel creation, no per-TC export job).

**Note on TC-STANDALONE-PRE.0:** This file uses an explicit **shared data pack** + per-TC **row locator** (user-requested). Each TC still lists what that scenario needs from the pack so a tester can re-run one AC in isolation by repeating only the relevant shared steps.

**Bank partner (TC-BE-6 / TC-BE-6b):** Requires **`typeOfFile=BANK_PARTNER`** — a **second** channel and **second** output file (CSV). Reuses liabilities from the shared pack where noted; not mixed into the Payment Partner `.txt`.

---

## User story — TO-BE rules (summary)

| Priority | Condition | Prefix (338) | Number (341) | Document date (351) |
|----------|-----------|--------------|--------------|---------------------|
| 1 | Rescheduling | `RES` | id zero-pad 10 (truncate left if >10) | Unchanged |
| 2 | External outgoing document | `000` | digits pad 10 | **Occurrence date** |
| 3 | Invoice / action / deposit / LPF | 3+10 split | per nomenclature | Invoice date; **LPF → Logical date** |

**Cols:** 338 (3), 341 (10), 351, 361, 371, 387, 403 (`12345`). Row length **423**.

---

## Acceptance criteria traceability

| AC | TC | Row in **same** Payment Partner export |
|----|-----|----------------------------------------|
| AC1 | All TC-BE-1…10, 7 | Layout on located row |
| AC2 | TC-BE-1 | MAIN — external `0000222357185` |
| AC3 | TC-BE-3, TC-BE-7 | RES-SHORT `RES0000001065`; RES-LONG last 10 digits |
| AC4 | TC-BE-2 | MAIN — LPF logical date |
| AC5 | TC-BE-4, 8, 10 | MAIN — invoice / action / deposit |
| AC6 | TC-BE-9 | CUST-A present; **CUST-B absent** |
| AS-IS guard | TC-BE-5 | MAIN — manual without external doc |

---

## Shared test data — one Payment Partner collection channel

Execute **once** before any TC. Capture all IDs in the table at the end.

### A. Auth and product stack (shared catalog)

1. Authenticate on **Dev**; store Bearer token.
2. Create terms via `POST /terms` (type **PERIOD**, value **30**); capture `termId`.
3. Create price component via `POST /price-components` (type **ELECTRICITY**, `vatRateId` from env); capture `priceComponentId`.
4. Create product via `POST /products` (link step 2–3, **ACTIVE**, `availableForSale` true); capture `productId`.

### B. Customers and contracts (three in channel; one excluded)

5. Create customer **MAIN** via `POST /customer` (**PRIVATE**, **ACTIVE**); capture `customerIdMain`, `customerNumberMain`.
6. Create POD for **MAIN** via `POST /pod` (**ELECTRICITY**, **ACTIVE**); capture `podIdMain`.
7. Create product contract for **MAIN** via `POST /product-contract` (customer step 5, POD step 6, product step 4); capture `contractIdMain`, `contractBillingGroupIdMain`.
8. Create energy data / billing profile for **MAIN** contract-POD.

9. Create customer **RES-SHORT** + POD + contract + energy data (same pattern as steps 5–8); capture `customerNumberResShort`, `contractIdResShort`.

10. Create customer **RES-LONG** + POD + contract + energy data; capture `customerNumberResLong`, `contractIdResLong`.

11. Create customer **CUST-A** via `POST /customer` (**ACTIVE**); capture `customerNumberA`.
12. Create customer **CUST-B** via `POST /customer` (**ACTIVE**); capture `customerNumberB` (must **not** match channel condition in step 14).

### C. One OFFLINE Payment Partner collection channel

13. Create **one** collection channel via `POST /collection-channel`:
    - `typeOfFile` = **PAYMENT_PARTNER**
    - `channelType` = **OFFLINE**
    - Valid send **RRULE** (export allowed in test window)
    - `folderForFileSending` under Dev `app.cfg.sharedFolderPath` tree
    - **Customer condition** includes **MAIN**, **RES-SHORT**, **RES-LONG**, **CUST-A** — **excludes CUST-B** (e.g. segment/condition on test customer numbers A + main cohort only)
    - Capture **`collectionChannelId`** (single channel for entire pack).

### D. Liabilities on **MAIN** (multiple rows, same `customerNumberMain`)

14. Manual / KVASY — `POST /customer-liability` for `customerIdMain`: `outgoingDocumentFromExternalSystem` = **`222357185`**, `occurrenceDate` = **`2025-06-15`**, amounts > 0 → **`liabilityIdExternal`** (TC-BE-1).

15. Manual without external doc — `POST /customer-liability` for `customerIdMain`: no outgoing external doc, `occurrenceDate` = **`2025-08-01`**, no invoice/LPF/rescheduling link → **`liabilityIdNoExternal`** (TC-BE-5).

16. Invoice — `POST /billing-run` for **MAIN** period **`2025-05-01`**–**`2025-05-31`**, execute; invoice `invoiceNumber` = **`25-0000123456`**, `invoiceDate` = **`2025-05-20`** → **`liabilityIdInvoice`** (TC-BE-4).

17. LPF — billing run **MAIN** period **`2025-01-01`**–**`2025-01-31`**, execute; past due; `POST /latePaymentFine/calculate/{id}` with date **`2025-07-01`** and invoice `liabilityId`; persist LPF with `logicalDate` = **`2025-07-01`**, `latePaymentNumber` e.g. **`LPF-00001234`** → **`liabilityIdLpf`** (TC-BE-2).

18. Action — `POST /actions` for **MAIN** (`contractIdMain`) so `actionNumber` = **`ACT-0000999888`** → resolve **`liabilityIdAction`** (TC-BE-8).

19. Deposit — `POST /deposit` for `customerIdMain`; `depositNumber` = **`DEP-0000555666`**; post to receivable → **`liabilityIdDeposit`**, document date basis **`2025-10-01`** (TC-BE-10).

### E. Rescheduling liabilities (separate customers, same channel)

20. **RES-SHORT** — billing run Feb **`2025-02-01`**–**`2025-02-28`**, execute; `POST /rescheduling/calculate-rescheduling` + `POST /rescheduling` → **`Rescheduling-1065`** → **`liabilityIdResShort`** (TC-BE-3).

21. **RES-LONG** — billing run Mar **`2025-03-01`**–**`2025-03-31`**, execute; rescheduling with sanitized digits **`1234567890123`** → **`liabilityIdResLong`** (TC-BE-7).

### F. AC6 — CUST-A in channel, CUST-B out

22. `POST /customer-liability` for **CUST-A**: `outgoingDocumentFromExternalSystem` = **`222357185`**, `occurrenceDate` = **`2025-09-01`** → row for **TC-BE-9** (expect in file).

23. `POST /customer-liability` for **CUST-B**: `outgoingDocumentFromExternalSystem` = **`111222333`**, `occurrenceDate` = **`2025-09-02`** → must **not** appear in file (TC-BE-9).

### G. Capture sheet (fill after setup)

| Key | Value |
|-----|--------|
| `collectionChannelId` | (single PAYMENT_PARTNER channel) |
| `exportFolderPath` | channel `folderForFileSending` on Dev share |
| `customerNumberMain` | |
| `customerNumberResShort` | |
| `customerNumberResLong` | |
| `customerNumberA` / `customerNumberB` | |

---

## Single export run (once)

**Preconditions:** Shared test data steps **1–23** completed.

**Test steps:**
1. Note baseline file list in `exportFolderPath` (or empty).
2. **`POST /collection-channel/test-export-liabilities-job`** — assert HTTP **200**.
3. Identify **newest** `.txt` in `exportFolderPath` → save as **`exportFileName`** (same file for all TC-BE-1…10 below).
4. Confirm file has **multiple** lines (one per exported liability); each data line length **423** for Payment Partner rows.

**Expected:** Job **200**; at least one row each for MAIN (external, invoice, LPF, action, deposit where eligible), RES-SHORT, RES-LONG, CUST-A; **no** line with `customerNumberB`.

---

## Backend test cases — verify rows in `exportFileName`

**Preconditions (every TC):** Shared test data **1–23** + **Single export run** completed; `exportFileName` known.

**Test steps (every TC):** Open **`exportFileName`** → locate row per **Row locator** → read cols **338–350**, **351–360**, **361–370**, **371–387**, **403** (AC1).

---

### TC-BE-1 (Positive): External outgoing document — MAIN (User Story AC2)

**Description:** AC2 — `222357185` → `000` + `0222357185`, date from occurrence.

**Row locator:** `customerNumberMain` + combined reference contains **`0000222357185`** (or unique `outgoingDocumentFromExternalSystem` marker from step 14).

**Expected test case results:** Cols **338–340** = **`000`**; **341–350** = **`0222357185`**; date **351** = **`15.06.2025`**; not `liabilityIdExternal` as number; **AC1** layout; row **423**.

**References:** User Story AC2.

---

### TC-BE-2 (Positive): LPF logical date — MAIN (User Story AC4)

**Description:** AC4 — document date from logical date, not blank.

**Row locator:** `customerNumberMain` + LPF reference (e.g. prefix **`LPF`**, number from **`LPF-00001234`**).

**Expected test case results:** Col **351** = **`01.07.2025`**; cols **351–360** not blank; **AC1**; row **423**.

**References:** User Story AC4.

---

### TC-BE-3 (Positive): Rescheduling 1065 — RES-SHORT (User Story AC3)

**Description:** AC3 — `RES0000001065`, no column shift.

**Row locator:** `customerNumberResShort`.

**Expected test case results:** **338–340** = **`RES`**; **341–350** = **`0000001065`**; amounts at **371** / **387**; **`12345`** at **403**; row **423**.

**References:** User Story AC3.

---

### TC-BE-4 (Positive): Invoice 3+10 — MAIN (User Story AC5)

**Description:** AC5 invoice — `25-0000123456` → **`250`** + **`0000123456`**.

**Row locator:** `customerNumberMain` + invoice liability (not external/rescheduling row).

**Expected test case results:** **338–340** = **`250`**; **341–350** = **`0000123456`**; **351** = **`20.05.2025`**; **AC1**; row **423**.

**References:** User Story AC5.

---

### TC-BE-5 (Negative): Manual without external doc — MAIN (User Story AS-IS)

**Description:** AS-IS guard — must not export `liabilityIdNoExternal` in cols 338–350.

**Row locator:** `customerNumberMain` + liability from step 15 (if row present).

**Expected test case results:** If row exists: cols **338–350** ≠ padded **`liabilityIdNoExternal`**. If row absent: document exclusion only — still pass. No **`000`**/`RES` from wrong source.

**References:** User Story AS-IS table.

---

### TC-BE-7 (Positive): Long rescheduling id — RES-LONG (User Story AC3)

**Description:** AC3 long id — last 10 digits **`4567890123`**.

**Row locator:** `customerNumberResLong`.

**Expected test case results:** **338–340** = **`RES`**; **341–350** = **`4567890123`**; **403** = **`12345`**; col **371** not shifted; row **423**.

**References:** User Story AC3 (long id).

---

### TC-BE-8 (Positive): Action reference — MAIN (User Story AC5)

**Description:** AC5 action — **`ACT`** + **`0000999888`**.

**Row locator:** `customerNumberMain` + action liability row.

**Expected test case results:** **338–340** = **`ACT`**; **341–350** = **`0000999888`**; not `000`/`RES`; **AC1**; row **423**.

**References:** User Story AC5.

---

### TC-BE-9 (Positive): Channel eligibility — CUST-A yes, CUST-B no (User Story AC6)

**Description:** AC6 — same channel, same export file; selection rules unchanged.

**Row locator:** Search whole **`exportFileName`** for `customerNumberA` vs `customerNumberB`.

**Expected test case results:** ≥1 row for **A** with **`0000222357185`** and date **`01.09.2025`**; **zero** rows for **B**.

**References:** User Story AC6.

---

### TC-BE-10 (Positive): Deposit reference — MAIN (User Story AC5)

**Description:** AC5 deposit — **`DEP`** + **`0000555666`**.

**Row locator:** `customerNumberMain` + deposit liability row.

**Expected test case results:** **338–340** = **`DEP`**; **341–350** = **`0000555666`**; **351** = **`01.10.2025`** (if step 19 used that date); **AC1**; row **423**.

**References:** User Story AC5.

---

## Appendix — Bank partner (second channel, second file)

Same **liability entities** where possible; **not** the Payment Partner `exportFileName`.

### Shared add-on (after step 23)

**B1.** Create **one** `BANK_PARTNER` channel via `POST /collection-channel` (BIC/IBAN/direct debit per Swagger), condition **MAIN** + **RES-SHORT**; capture `bankChannelId`.

**B2.** **`POST /collection-channel/test-export-liabilities-job`** (exports all eligible channels) or trigger scoped to bank folder — capture **`bankExportFileName`** (CSV).

### TC-BE-6 (Positive): BANK — external doc (AC2)

**Preconditions:** Shared **1–23**, liability step **14**, appendix **B1–B2**.

**Row locator:** CSV line for `customerNumberMain`; field **11**.

**Expected:** Field **1** = `P02`; field **2** = `20250615`; field **11** contains **`0000222357185`**.

### TC-BE-6b (Positive): BANK — rescheduling (AC3)

**Preconditions:** Shared **1–23**, step **20**, appendix **B1–B2**.

**Row locator:** CSV line for `customerNumberResShort`; field **11**.

**Expected:** Field **11** contains **`RES0000001065`**.

---

## References

| Source | Detail |
|--------|--------|
| **User Story V 0.2** | `config/jira/attachments/PDT-2187/User Story V 0.2.extracted.txt` |
| Jira | `config/jira/PDT-2187-full.json` |
| Sample export | `config/jira/attachments/PDT-2187/Epay_export_20251212_080000.txt` |
| Code | `AsyncExportProcessor.java` |
| API | `POST /collection-channel`, `POST /collection-channel/test-export-liabilities-job` |
