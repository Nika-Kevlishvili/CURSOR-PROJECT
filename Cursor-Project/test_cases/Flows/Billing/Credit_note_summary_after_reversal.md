# Billing – Credit Note Summary After Reversal (PDT-2585)

**Bug reference:** PDT-2585 – Frontend - URGENT: Billing - No Price component in a credit note  
**Cross-dependency:** cross_dependencies/2026-03-02_PDT-2585-credit-note-price-component.json

**Scope:** Reversal billing run (invoice → credit note); credit note summary data must show price component names (same as original invoice) and Total volumes must be empty (not 0).

---

## Test data (preconditions)

- **Environment:** Test (testapps.energo-pro.bg).
- **Original invoice:** id = 39650 (or equivalent invoice with known price components and volumes).
  - URL (example): `https://testapps.energo-pro.bg/app/phoenix-epres/billing-run/invoices/preview/summary-data?id=39650`
- **Resulting credit note:** Created by reversal billing run; e.g. id = 46045.
  - URL (example): `https://testapps.energo-pro.bg/app/phoenix-epres/billing-run/invoices/preview/summary-data?id=46045`
- **APIs:** PATCH `/billing-run/test-reversal/{id}` (dry run), PATCH `/billing-run/test-reversal-save/{id}` (persist); GET `/invoice/summary-data`, GET `/invoice/detailed-data`, GET `/invoice` (preview).

---

## TC-1: Credit note summary – price component names (main bug)

**Objective:** Verify that after creating a reversal billing run for an invoice, the resulting credit note’s summary data shows the **same price component names** as the original invoice.

**Preconditions:** Invoice with id = 39650 (or test invoice with known price components) exists. User can create reversal billing run and open invoice/credit note preview.

**Steps:**
1. Open the original invoice summary (e.g. invoice id = 39650) and note all **price component names** in the Summary Data tab (tab 2).
2. Create a **reversal billing run** for this invoice (PATCH test-reversal then test-reversal-save, or equivalent UI).
3. Open the resulting **credit note** (e.g. id = 46045) and go to the **Summary Data** tab (tab 2).
4. Compare the price component names shown in the credit note summary with the original invoice.
5. (Optional) Call GET `/invoice/summary-data` with the credit note id and verify that `priceComponentOrPriceComponentGroupOrItem` (or equivalent) is populated for each row.

**Expected result:** The credit note summary shows the **same price component names** as the original invoice. No missing or blank price component names.

**Actual result (bug):** Summary data of the credit note does not show the name of the price components.

---

## TC-2: Credit note summary – Total volumes empty (not 0)

**Objective:** Verify that in the credit note summary, **Total volumes** is **empty** (null/omitted), not displayed as "0".

**Preconditions:** Same as TC-1; credit note created from reversal billing run.

**Steps:**
1. Create a reversal billing run for the test invoice and open the resulting credit note.
2. Open the **Summary Data** tab (tab 2) of the credit note.
3. Check the **Total volumes** value for each summary row (and in the UI header if applicable).
4. (Optional) Call GET `/invoice/summary-data` with the credit note id and verify the `totalVolumes` field: for credit note it should be empty (null or omitted), not 0.

**Expected result:** Total volumes is **empty** (blank), not "0".

**Actual result (bug):** Total volumes is shown as "0".

---

## TC-3: Credit note detailed data and preview (integration)

**Objective:** Ensure credit note **Detailed Data** (tab 3) and **Preview** also show correct price component names and total volumes (empty not 0) where applicable.

**Preconditions:** Credit note created from reversal billing run (e.g. id = 46045).

**Steps:**
1. Open the credit note → **Detailed Data** tab (tab 3). Verify price component names and total volumes (empty vs 0) per business rules.
2. Open the credit note **Preview** (GET `/invoice` with document id or UI preview). Verify summary and detailed information is consistent.
3. (Optional) Call GET `/invoice/detailed-data` with credit note id and assert `priceComponent` and `totalVolumes` in the response.

**Expected result:** Detailed data and preview are consistent with summary: price component names present; total volumes empty where required, not 0.

---

## TC-4: Regression – manual credit/debit note and invoice correction

**Objective:** Ensure that fixing PDT-2585 does not break manual credit/debit note creation or invoice correction flows that use the same summary/detailed APIs and DTOs.

**Preconditions:** Access to manual credit or debit note flow and invoice correction (reversal) flow.

**Steps:**
1. Create a **manual credit or debit note** (Confluence: "Manual credit or debit note - Create"). Open Summary Data (tab 2) and Detailed Data (tab 3). Verify price component names and total volumes display correctly.
2. Perform **invoice correction** (reversal) for another invoice and open the resulting credit note summary. Verify same assertions as TC-1 and TC-2.
3. (Optional) Call GET `/invoice/summary-data` and GET `/invoice/detailed-data` for an invoice (not credit note) and confirm total volumes and price component behaviour unchanged.

**Expected result:** Manual credit/debit note and invoice correction flows still show correct summary/detailed data; no regression.

---

## TC-5: Regression – CSV/PDF export and regenerate-compensations

**Objective:** Ensure export and regenerate-compensations that depend on summary/detailed structure still work after fix.

**Preconditions:** Credit note (e.g. id = 46045) and invoice available.

**Steps:**
1. For the credit note, trigger **CSV export** (GET `/invoice/generate-csv/{id}` or UI). Check that exported data contains price component names and total volumes (empty not 0) as expected.
2. Trigger **PDF download** (GET `/invoice/download-document` or UI) for the credit note. Verify document content matches summary/detailed rules.
3. If applicable, call PATCH `/invoice/regenerate-compensations` for a document that uses summary/detailed data and verify no errors and consistent data.

**Expected result:** Export and regenerate-compensations work; output reflects correct price components and empty total volumes for credit notes.

---

## References

- **Confluence:** Invoice details tabs (122978306), Phase 2 - Invoice correction (585730223), Manual credit or debit note - Create (151945240), Service Order Invoice - preview (25526337), Product contract Invoice - Preview (24969696).
- **API contract:** SummaryDataRowParameters / SummaryDataRowParametersResponse – `priceComponentOrPriceComponentGroupOrItem`, `totalVolumes` (for credit note: empty not 0); DetailedDataRowParameters – `priceComponent`, `totalVolumes`.
- **Cross-dependency:** entry_points (test-reversal, test-reversal-save, summary-data, detailed-data, invoice preview); what_could_break: credit note summary/preview, backend builder, manual credit/debit note, invoice correction, CSV/PDF export, regenerate-compensations.
