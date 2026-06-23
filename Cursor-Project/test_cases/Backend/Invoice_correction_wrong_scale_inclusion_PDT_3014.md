# Invoice Correction – Wrong Scale Inclusion on Volume-Only Correction (PDT-3014)

**Jira:** [PDT-3014](https://oppa-support.atlassian.net/browse/PDT-3014) (PDT)  
**Type:** Bug  
**Summary:** Invoice correction (volume-only, BY_SCALES) incorrectly includes billing data by scales from other PODs and/or zero-volume / wrong-period records, producing a CREDIT note and scale lines with 0 volume delta instead of the expected correction outcome.

**Scope:** Backend (API + read-only DB verification) on **Dev**. Reproduces prod pattern from billing run **3590** (invoice **161427**, POD **32Z103001436262C**): original scale **185324** only on invoice; correction linked decoy scales **296165** (+818 kWh, different POD) and **296173** (0 kWh, wrong period, different POD).

**Automated test:** `Cursor-Project/EnergoTS/tests/cursor/PDT-3014-invoice-correction-wrong-scale-inclusion.spec.ts`

---

## Test data (preconditions)

Reference appendix only — **each TC repeats its own full numbered chain** (Rule TC-STANDALONE-PRE.0).

- **Environment:** Dev
- **Delivery type:** Product contract with **BY_SCALE** (not BY_PROFILE)
- **Correction flags:** `volumeChange=true`, `priceChange=false` (volume-only correction)
- **Prod analogue:**

| Prod ID | Role | POD (prod) | volumes | Period header | Notes |
|---------|------|------------|---------|---------------|-------|
| 185324 | Original (on invoice) | EVN3232574 | 10445 | 2025-11-30 – 2025-12-30 | Used on REAL invoice 161427 |
| 296165 | Decoy (linked to correction) | 32Z490014025017S | 818 | 2025-12-16 – 2025-12-30 | `original=false` on run 3590 |
| 296173 | Decoy (linked to correction) | 32Z450505108116F | **0** | 2025-11-01 – 2025-12-01 | Wrong period vs original; `correction=false` |

- **Documented rules (Confluence):**
  - [Correction flow for product/service contracts](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/256114692/Correction+flow+for+product+service+contracts) — correction includes **only PODs from the old invoice**; volume-only flow uses new billing data for recalculation.
  - [Create Billing data by scales](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/11108559/Create+Billing+data+by+scales) — correction header period must match original period.
- **Runtime (code):** `BillingRunStandardInvoiceGenerationProcessor.java:1231-1244` — net `< 0` → CREDIT_NOTE; `>= 0` → DEBIT_NOTE.

---

## Backend Test Cases

### TC-BE-1 (Negative): Volume-only correction must not include zero-volume scale from different POD / wrong period

**Description:** Reproduce PDT-3014 on Dev: after REAL invoice on POD-A with one original billing-by-scales (period P1), create decoy billing-by-scales on POD-C (zero volumes, **different header period** than P1, `correction=false`). Run volume-only invoice correction for the original invoice. Verify decoy scale **must not** participate in correction recalculation (prod bug: scale 296173 was linked).

**Preconditions:**
1. Create terms via `POST /terms` (`type: PERIOD`, valid Dev `periodType`); store `termId`.
2. Create price component(s) via `POST /price-components` for **BY_SCALE** product (ELECTRICITY + grid/scale price types from Dev nomenclature); store `priceComponentIds[]`.
3. Create product via `POST /products` with `dataDeliveryType: BY_SCALE`, `termId`, `priceComponentIds`, `status: ACTIVE`; store `productId`.
4. Create customer via `POST /customer` (`customerType: PRIVATE`, ACTIVE); store `customerId`.
5. Create **POD-A** via `POST /pod` (electricity, ACTIVE, Dev `gridOperatorId`, activation ≤ today); store `podAId`, `podAIdentifier`.
6. Create **POD-C** via `POST /pod` (different identifier, same grid operator, ACTIVE); store `podCId`, `podCIdentifier`.
7. Install meters on POD-A and POD-C (installment date before billing period); link active scale nomenclature rows (scale codes matching price components).
8. Create product contract via `POST /product-contract` linking customer step 4, **POD-A** step 5, product step 3; contract-POD active; store `contractId`.
9. Create **original** billing-by-scales on **POD-A** via `POST /billing-by-scales`:
   - `identifier`: `podAIdentifier`
   - `dateFrom`: **P1_FROM** (e.g. first day of previous month)
   - `dateTo`: **P1_TO** (e.g. last day of previous month)
   - `correction`: `false`, `override`: `false`
   - Table row: valid scale/tariff data, `totalVolumes`: **386** (or prod-like positive volume)
   - Store returned billing-data id as `originalScaleIdA`.
10. Create standard billing run via `POST /billing-run` (`billingType: STANDARD_BILLING`, linked to `contractId`, period matching P1); execute to completion (`PATCH /billing-run/start-billing` + poll); store `originalInvoiceId`, `originalInvoiceNumber`, `originalBillingRunId`.
11. Confirm via `GET /invoice/{originalInvoiceId}` or `POST /invoice/listing`: invoice status **REAL**, `documentType: INVOICE`, POD-A lines reference `originalScaleIdA` only.
12. Create **decoy** billing-by-scales on **POD-C** via `POST /billing-by-scales`:
    - `identifier`: `podCIdentifier` (**not** POD-A)
    - `dateFrom`: **P2_FROM** (different from P1_FROM — e.g. two months earlier start)
    - `dateTo`: **P2_TO** (different from P1_TO)
    - `correction`: `false`
    - Table row: `totalVolumes`: **0**, `totalValue`: **0**
    - Store `decoyZeroScaleIdC`.
13. (Optional decoy, prod 296165 pattern) Create billing-by-scales on **POD-B** (third POD) with partial overlap period and `totalVolumes: 818`; store `decoyScaleIdB`.

**Test steps:**
1. Create invoice correction billing run via `POST /billing-run`:
   - `billingType: INVOICE_CORRECTION`
   - `volumeChange: true`, `priceChange: false`
   - `listOfInvoices`: `originalInvoiceNumber` from step 10
   - Valid accounting period, invoice date, template from Dev env
   - Store `correctionBillingRunId`.
2. Start correction generation: `PATCH /billing-run/start-billing` (or project-standard start endpoint); poll `GET /billing-run/draft-invoices?type=INVOICE_CORRECTION&id={correctionBillingRunId}` until draft invoices appear.
3. For each draft invoice id, call `GET /invoice/detailed-data/{invoiceId}` (or listing + detail endpoints per Swagger Dev).
4. Read-only DB (Dev PostgreSQLDev MCP), if available:
   ```sql
   SELECT scale_id, original, ignore, pod_id
   FROM billing_run.correction_billing_data_ids
   WHERE run_id = {correctionBillingRunId};
   ```
   ```sql
   SELECT id, detail_type, scale_id, total_volumes, billing_data_scale_ids
   FROM invoice.invoice_standard_detailed_data
   WHERE invoice_id IN ({draftInvoiceIds});
   ```

**Expected test case results (spec / TO-BE):**
- HTTP **200/206** on correction start; at least one draft correction document generated.
- `correction_billing_data_ids` for run **must not** contain `decoyZeroScaleIdC` (POD-C) nor `decoyScaleIdB` when correction is only for POD-A invoice.
- Invoice detailed data for correction drafts **must not** include `billing_data_scale_ids` referencing decoy scales from other PODs.
- No SCALE detail line with `totalVolumes: 0` while other scale lines on same POD show negative delta (e.g. -3 kWh) unless business rules explicitly allow zero delta for that price component.
- If net correction delta is negative → `documentType: CREDIT_NOTE`; if positive → `documentType: DEBIT_NOTE` per code logic.

**Actual result (prod / suspected Dev bug):** Decoy scale 296173 (0 kWh, wrong POD/period) linked in `correction_billing_data_ids`; invoice line for scale 1020 shows **0.0000** volume; CREDIT_NOTE 0.10 issued.

**References:** PDT-3014; Confluence 256114692, 11108559; prod DB run 3590.

---

### TC-BE-2 (Negative): Zero-volume decoy scale must not appear in invoice `billing_data_scale_ids` array on correction draft

**Description:** Narrow assertion on invoice detail payload — all settlement/scale rows on correction draft must reference only original POD-A scale id(s), not decoy zero-volume scale.

**Preconditions:**
1. Repeat preconditions steps 1–13 from TC-BE-1 (full chain through decoy scale on POD-C).
2. Create and start volume-only correction billing run; obtain `correctionBillingRunId` and draft invoice id `correctionInvoiceId`.

**Test steps:**
1. `GET /invoice/detailed-data/{correctionInvoiceId}` (Swagger Dev path).
2. Collect every `billingDataScaleIds` / `billing_data_scale_ids` array from all detail rows for POD-A.
3. Compare against `{originalScaleIdA}` only.

**Expected test case results:**
- Arrays contain **only** `originalScaleIdA` (and correction-type scale ids created **for POD-A with matching header period**, if any).
- **`decoyZeroScaleIdC` ∉ any array.**
- No row with `totalVolumes: 0` on a scale price component that should receive volume delta when volume correction is -3 kWh on sibling lines.

**References:** PDT-3014; prod invoice 256009 detail ids 4499799–4499805.

---

### TC-BE-3 (Positive): Control — volume-only correction with matching correction scale on same POD produces consistent debit/credit note

**Description:** Control case without decoy PODs: only POD-A, original scale + **valid correction** billing-by-scales (same header period as original, `correction=true`, negative volume delta). Expect deterministic correction document type per net sign.

**Preconditions:**
1. Steps 1–11 from TC-BE-1 (contract, POD-A only, original scale, REAL invoice) — **skip steps 12–13** (no decoy PODs).
2. Create **correction** billing-by-scales on **same POD-A** via `POST /billing-by-scales`:
   - `dateFrom` / `dateTo`: **exact match** to original (P1_FROM, P1_TO)
   - `correction`: `true`, `invoiceCorrection`: `originalInvoiceNumber`
   - Table row: `totalVolumes`: **-3** (negative correction delta, allowed when correction checkbox set per Confluence 11108559)
   - Store `correctionScaleIdA`.

**Test steps:**
1. Create volume-only `INVOICE_CORRECTION` billing run for `originalInvoiceNumber`; store `correctionBillingRunId`.
2. Start billing; poll draft invoices.
3. `GET /invoice/{draftInvoiceId}` — read `documentType`, `totalAmountExcludingVat`.
4. DB: `SELECT document_type, total_amount_excluding_vat FROM invoice.invoices WHERE billing_id = {correctionBillingRunId};`

**Expected test case results:**
- Exactly **one** correction draft invoice (or documented pair if reversal applies) linked to POD-A.
- `correction_billing_data_ids` includes `correctionScaleIdA` with `original=false`; **does not** include unrelated POD scale ids.
- Net excluding VAT **< 0** → `documentType: CREDIT_NOTE` with positive displayed amounts (abs applied per code); net **≥ 0** → `DEBIT_NOTE`.
- All non–power-access scale lines show consistent negative delta (e.g. -3 kWh), not mixed 0 and -3 on the same correction unless spec documents power-access exception.

**References:** Confluence 256114692 (only correction type scale on same POD); code `BillingRunStandardInvoiceGenerationProcessor.java:1231-1244`.

---

### TC-BE-4 (Negative): API must reject or ignore billing-by-scales with wrong header period when saved as correction for same POD

**Description:** Guard aligned with PDT-2708 / Confluence 11108559 — correction scale with header period mismatch should not enter correction pipeline (if API allows save, correction run must not pick it up).

**Preconditions:**
1. Steps 1–11 from TC-BE-1 (POD-A original scale + REAL invoice).

**Test steps:**
1. Attempt `POST /billing-by-scales` on POD-A with `correction: true`, `dateFrom`/`dateTo` **different** from original header period, `totalVolumes: -3`.
2. If HTTP 201, attempt volume-only correction and inspect whether mismatched record appears in `correction_billing_data_ids`.

**Expected test case results (spec):**
- **Preferred:** HTTP **400/409** with message containing *Period From date doesn't match the original scales dates* (PDT-2708 / Confluence 11108559).
- **If save allowed (known gap):** correction run **must not** link mismatched-period scale id to `correction_billing_data_ids` for this invoice correction.

**References:** PDT-2708; `test_cases/Backend/Correction_data_by_scales_header_period.md`; Confluence 11108559.

---

## References

- **Jira:** [PDT-3014](https://oppa-support.atlassian.net/browse/PDT-3014)
- **Confluence:** [Invoice correction - process](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/163545113/Invoice+correction+-+process) (163545113); [Correction flow for product/service contracts](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/256114692/Correction+flow+for+product+service+contracts) (256114692); [Create Billing data by scales](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/11108559/Create+Billing+data+by+scales) (11108559)
- **Code:** `BillingRunStandardInvoiceGenerationProcessor.java:1231-1244`; `BillingRunCorrectionService.java:66-68`, `217-227`
- **Playwright (Dev):** `EnergoTS/tests/cursor/PDT-3014-invoice-correction-wrong-scale-inclusion.spec.ts`
- **Prod evidence:** billing run 3590, invoice 256009, scales 296165/296173, original 185324

### Finding: Wrong-POD zero-volume scale linked to correction (PDT-3014)
- **Type:** Code defect (suspected) / data-mapping
- **User impact:** Medium — incorrect CREDIT note and 0-volume scale lines
- **Spec says:** Only PODs from old invoice; correction period must match original (256114692, 11108559)
- **Prod runtime:** 296173 linked with `original=false`, volumes 0, wrong POD/period
- **Gap:** Decoy scale participates in recalculation though it should not
- **Recommendation:** Fix scale selection in correction pipeline; use TC-BE-1 as regression gate
