# Volume billing — WITH_ELECTRICITY vs POD deactivation (UI parity) (PDT-2376)

**Jira:** PDT-2376  
**Type:** Regression (UI parity)  
**Summary:** Frontend visibility of tariff lines aligns with backend matrix: PODs deactivated before the invoiced calendar month must not expose WITH_ELECTRICITY lines in invoice preview/export for that billing run scope.

**Scope:** Manual/UI checks after API setup from Backend document. Automated API matrix lives in EnergoTS; UI assertions here are exploratory / smoke unless mapped to Portal/Energo QA scripts.

---

## Test data (preconditions)

1. Follow **Backend / Volume_billing_WITH_ELECTRICITY_POD_deactivation_PDT_2376.md** precondition chain through invoice generation so at least eight PODs attach to one customer contract **or** recreate same logical structure via Portal (customer, PODs, catalogue product with volume + WITH_ELECTRICITY components, activation dates per matrix).
2. Finance user authenticated on the environment under test (`INVOICE_VIEW` / equivalent permissions for STANDARD invoices).
3. Note `invoicePeriodTo` / adaptation comment from runtime (attached JSON in automated run or Dev accounting note).

---

## Frontend Test Cases

### TC-FE-1 (Positive): Invoice explorer shows WITH_ELECTRICITY line items for eligible PODs

**Description:** PODs still active across the invoice month (“eligible” tier in matrix: `YearMonth(deactivation)` same as `invoicePeriodTo` calendar month OR `null`) must display electricity tax line items for their identifiers inside invoice detail grids / previews.

**Preconditions:** Data from §Backend steps with mixed eligible + ineligible PODs; drafts converted to invoices available in UI listings.

**Test steps:**

1. Navigate to Billing → Billing run workspace, open completed run referencing the seeded contract (`contractNumber`).
2. Open each generated STANDARD invoice hyperlink / preview pane.
3. Expand detailed line breakdown (matching API `invoice/detailed-data` presentation).
4. For matrix labels `P1`–`P4` plus `P8` (eligible pattern), visually confirm presence of tariff row labelled similarly to automation (`priceComponent` contains “electricity” / organisational naming).

**Expected test case results:** Every eligible POD identifier shows at least one WITH_ELECTRICITY-class row grouped under that POD; no blocking UI errors while paging line items.

---

### TC-FE-2 (Negative): Invoice UI must omit WITH_ELECTRICITY rows for PODs deactivated before invoice calendar month

**Description:** PODs whose deactivation strictly precedes invoiced calendar month (`YearMonth(deactivation)` < `YearMonth(invoicePeriodTo)`), must neither show NOR export WITH_ELECTRICITY rows labelled for those POD identifiers once invoice is FINAL / REAL.

**Preconditions:** Same seeded dataset confirming at least PODs analogous to automation labels `P5`–`P7`.

**Test steps:**

1. Follow TC-FE-1 navigation to invoice previews / exports.
2. Filter or search POD identifiers belonging to deactivated-early PODs (`P5`–`P7` analogues).
3. Attempt downloadable PDF/CSV preview if tenant workflow supports it — scan text for POD identifier + tariff keywords.

**Expected test case results:** No WITH_ELECTRICITY-labelled row appears bound to flagged POD identifiers; any mismatch flagged as parity defect vs Backend TC-BE-2.

---

## References

- Companion API scenarios: `test_cases/Backend/Volume_billing_WITH_ELECTRICITY_POD_deactivation_PDT_2376.md`
- Automated API trace: `Cursor-Project/EnergoTS/tests/cursor/PDT-2376-volume-with-electricity-pod-deactivation.spec.ts`
