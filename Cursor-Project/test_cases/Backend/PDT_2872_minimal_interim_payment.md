# Minimal amount for interim payment (PDT-2872)

**Jira:** PDT-2872  
**Type:** Task  
**Summary:** Create `INTERIM_AND_ADVANCE_PAYMENT` invoice only when **total amount including VAT** in **main currency** is **≥ 5.00 EUR** after rounding; otherwise **do not** create invoice or liabilities.  
**Scope:** **Dev** — Backend/API only. Standard interim (`BillingRunInterimProcessingService`) + manual interim (`BillingRunManualInterimAdvancePaymentProcess`, `PATCH /billing-run/start-billing`).

## Rounding and gate (mandatory for all TCs)

**Gate (PDT-2872):** Create `INTERIM_AND_ADVANCE_PAYMENT` invoice only when **`totalAmountIncludingVat`** in **main currency** is **≥ 5.00** after scaling; otherwise no invoice and no interim-linked liability.

**Phoenix scaling (code):** `EPBDecimalUtils.convertToCurrencyScale` / `setScale(2, RoundingMode.HALF_UP)` on invoice totals; interim EXACT_AMOUNT VAT uses `multiply` → `setScale(12, HALF_UP)` → `divide(100, 12, HALF_UP)`, then totals summarized and scaled to 2 decimals (`BillingRunInterimProcessingService`, `BillingRunManualInterimAdvancePaymentProcess`).

**Test amount model (IAP `EXACT_AMOUNT`, cent precision):**

1. `vatLine = round2(amountExcludingVat × vat% / 100)`
2. `totalInclVat = round2(amountExcludingVat + vatLine)` — **this** value is compared to the **5.00** gate.

**Important:** Some “nice” incl. targets are **not reachable** at cent-level `amountExcludingVat` (e.g. at **20% VAT**, **4.18** → **5.02**, not **5.01**). TCs must assert **scaled** totals from the table below, not a literal incl. that math cannot produce.

**Before preconditions:** `GET vat-rates?statuses=ACTIVE` — record `valueInPercent`; recompute appendix table (do not assume 20%).

### Appendix — example @ 20% VAT (recalculate per environment)

| Role | `amountExcludingVat` | Scaled `totalInclVat` | Gate |
|------|----------------------|----------------------|------|
| Boundary (≥ 5.00) | 4.17 | **5.00** | Create |
| First step above 5.00 | 4.18 | **5.02** (not 5.01) | Create |
| Below threshold | 4.16 | **4.99** | Do not create |
| Dev IAP minimum | 0.01 | **0.01** | Do not create (API rejects 0.00; value must be > 0.01) |

**Playwright / automation:** Resolve amounts dynamically (`resolvePdt2872Amounts`); assert `totalAmountIncludingVat` against computed `inclFor*` for the chosen excl, and use **≥ 5.00** / **≥ 5.01** only where the TC describes the gate, not an unreachable exact incl. After each test, attach **billing run portal preview URL(s)** (Playwright `[PDT-2872] Billing run portal URLs` + console): **standard** interim → `http://10.236.20.11:8080/billing-run/preview/basic-parameters?type=STANDARD_BILLING&id={id}`; **manual** interim (TC-BE-7/8) → `.../basic-parameters?type=MANUAL_INTERIM_AND_ADVANCE_PAYMENT&id={id}` (not `/billing-run/preview?id=` alone).

---

## Backend Test Cases

### TC-BE-1 (Positive): Standard interim — total incl. VAT 5.00 EUR creates invoice

**Description:** Inclusive boundary: **5.00** incl. VAT must produce one interim invoice.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record main `currencyId` (EUR) and global `vatRateId` with `valueInPercent` (example **20**).
3. Create customer via `POST /customer` (`status: ACTIVE`, required fields per Swagger).
4. Create POD via `POST /pod` (`type: ELECTRICITY`, `status: ACTIVE`, `activationDate` ≤ billing date).
5. Create terms via `POST /terms`; create product via `POST /product` (default contract types per Swagger).
6. Add IAP on product: `valueType: EXACT_AMOUNT`, `calculationValue` = **exclFor500** from appendix (e.g. **4.17** @ 20% VAT → scaled incl. **5.00**), `currencyId` = main currency, linked VAT rate from step 2.
7. Create product contract via `POST /product-contract` (customer, POD, product; `status: ACTIVE`; `entryIntoForceDate` ≤ invoice date).
8. Save contract IAP so interim preparation is valid.
9. Open accounting period for `invoiceDate`.
10. Create billing run via `POST /billing-run`: `billingType: STANDARD_BILLING`, `applicationModelType` includes `INTERIM_AND_ADVANCE_PAYMENT`, set `invoiceDate`, `taxEventDate`, link contract/PODs.
11. `POST /invoice/listing` (`searchBy: BILLING_RUN`, `billingRun` = run number) — baseline count of `INTERIM_AND_ADVANCE_PAYMENT` = **0** for this run.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}` — expect **202 Accepted**.
2. Poll `GET /billing-run/{id}` until interim generation finished.
3. `POST /invoice/listing` for this billing run.
4. `GET /invoice?id={invoiceId}`.

**Expected test case results:**
1. **202** on start-billing; billing run not **FAILED** due to threshold alone.
2. Exactly **one** new invoice: `invoiceType` = **INTERIM_AND_ADVANCE_PAYMENT**, `billingId` = run id.
3. `totalAmountIncludingVat` = scaled **inclFor500** from appendix (e.g. **5.00** @ 20% VAT).
4. `invoiceStatus` = **DRAFT** or **REAL** (record actual).

---

### TC-BE-2 (Positive): Standard interim — scaled incl. VAT above 5.00 EUR creates invoice

**Description:** First cent step **above** the 5.00 boundary after HALF_UP scaling creates invoice. At 20% VAT this is **4.18** → **5.02** (exact **5.01** incl. is not reachable with cent-level excl.).

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record main `currencyId` and `vatRateId` (`valueInPercent` documented).
3. Create customer via `POST /customer` (`status: ACTIVE`).
4. Create POD via `POST /pod` (`type: ELECTRICITY`, `status: ACTIVE`).
5. Create terms + product via `POST /terms`, `POST /product`.
6. Add IAP: `EXACT_AMOUNT`, `calculationValue` = **exclFor501** from appendix (e.g. **4.18** @ 20% VAT).
7. Create product contract via `POST /product-contract` (`status: ACTIVE`).
8. Save contract IAP; open accounting period.
9. Create `STANDARD_BILLING` run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim invoice count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`; `GET /invoice?id={invoiceId}`.

**Expected test case results:**
1. One interim invoice; `totalAmountIncludingVat` = scaled **inclFor501** (e.g. **5.02** @ 20% VAT); must be **≥ 5.01** and **≥ 5.00** gate.
2. `totalAmountExcludingVat` > 0; `totalAmountOfVat` > 0.

---

### TC-BE-3 (Negative): Standard interim — total incl. VAT 4.99 EUR — no invoice

**Description:** Below threshold must not create interim invoice or liability.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record main `currencyId` and `vatRateId` (20% example).
3. Create customer via `POST /customer` (`status: ACTIVE`).
4. Create POD via `POST /pod` (`type: ELECTRICITY`, `status: ACTIVE`).
5. Create terms + product via `POST /terms`, `POST /product`.
6. Add IAP: `EXACT_AMOUNT`, `calculationValue` = **exclFor499** (e.g. **4.16** → scaled incl. **4.99** @ 20% VAT).
7. Create product contract via `POST /product-contract` (`status: ACTIVE`).
8. Save contract IAP; open accounting period.
9. Create `STANDARD_BILLING` + `INTERIM_AND_ADVANCE_PAYMENT` billing run; record `invoiceDate`.
10. `POST /invoice/listing` — baseline interim count = **0**.
11. `GET /customer-liability/list` for customer — record liability count **before** billing.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing` for billing run.
3. `GET /customer-liability/list` — same customer and date range as step 11.

**Expected test case results:**
1. Interim invoice count still **0** for this `billingId`.
2. `GET /billing-run/{id}` does not return a new interim invoice id in linked invoice lists (if exposed).
3. Liability count unchanged vs step 11 (no new interim-linked liability).

---

### TC-BE-4 (Negative): Standard interim — total incl. VAT 0.00 EUR — no invoice

**Description:** Zero amount must not create interim invoice.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record main `currencyId` and `vatRateId`.
3. Create customer via `POST /customer` (`status: ACTIVE`).
4. Create POD via `POST /pod` (`type: ELECTRICITY`, `status: ACTIVE`).
5. Create terms + product via `POST /terms`, `POST /product`.
6. Add IAP: `EXACT_AMOUNT`, `calculationValue: 0.01` (Dev minimum; API rejects **0.00** / values ≤ 0.01). Scaled incl. VAT remains **&lt; 5.00**.
7. Create product contract via `POST /product-contract` (`status: ACTIVE`).
8. Save contract IAP; open accounting period.
9. Create separate `STANDARD_BILLING` billing run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`.

**Expected test case results:**
1. **No** `INTERIM_AND_ADVANCE_PAYMENT` invoice for this billing run.
2. **No** interim-linked liability created.

---

### TC-BE-5 (Boundary): Rounding — scaled total 5.00 EUR creates invoice

**Description:** Confirms gate uses **scaled** `totalAmountIncludingVat` (scale 2, HALF_UP), not raw `amountExcludingVat × (1 + VAT%)` without intermediate VAT rounding.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record `vatRateId` and percent.
3. Create customer, POD, terms, product (steps as TC-BE-1).
4. Add IAP `EXACT_AMOUNT` with `calculationValue` = **exclRoundingEdge** if different from **exclFor500**, else **exclFor500** (document excl/incl pair from appendix).
5. Create product contract; open accounting period; create standard billing run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `GET /invoice?id={invoiceId}`.

**Expected test case results:**
1. Interim invoice exists; `totalAmountIncludingVat` = scaled **inclRoundingEdge** / **inclFor500** (e.g. **5.00** @ 20% VAT).

---

### TC-BE-6 (Boundary): Rounding — scaled total 4.99 EUR — no invoice

**Description:** Scaled **4.99** must not create invoice.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record `vatRateId` (20% example).
3. Create customer via `POST /customer` (`status: ACTIVE`).
4. Create POD via `POST /pod` (`type: ELECTRICITY`, `status: ACTIVE`).
5. Create terms + product.
6. Add IAP `EXACT_AMOUNT`, `calculationValue` = **exclFor499**.
7. Create product contract; accounting period; `STANDARD_BILLING` run + `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`.

**Expected test case results:**
1. **No** interim invoice for this billing run.

---

### TC-BE-7 (Positive): Manual interim — total incl. VAT ≥ 5.00 EUR creates invoice

**Description:** Manual path applies same threshold in `persistInvoices`.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Record `currencyId`, `vatRateId`, `customerDetailId`, `contractId`, `billingGroupIds`, `bankId`, `iban` (valid per Swagger).
3. Create customer + POD + product contract (as TC-BE-1 steps 3–7).
4. Open accounting period for `invoiceDate`.
5. `POST /billing-run` — capture `{id}` (`MANUAL_INTERIM_AND_ADVANCE_PAYMENT` minimal create).
6. `PUT /billing-run/{id}` with `BillingRunEditRequest`: `billingType: MANUAL_INTERIM_AND_ADVANCE_PAYMENT`, `commonParameters` (`accountingPeriodId`, `invoiceDate`, `taxEventDate`, `runStages`, `executionType: MANUAL`), `interimAndAdvancePaymentParameters.amountExcludingVat` = **exclFor500** (e.g. **4.17**), `currencyId`, `vatRateId`, `contractType: PRODUCT_CONTRACT`, `contractId`, `customerDetailId`, `billingGroupIds`, required enums (`issuedSeparateInvoices`, `deductionFrom`, `issuingForTheMonthToCurrent`, `basisForIssuing`).
7. Baseline: `POST /invoice/listing` — **0** interim invoices for this run.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}` — **202**.
2. `GET /billing-run/{id}`.
3. `POST /invoice/listing`; `GET /invoice?id={invoiceId}`.

**Expected test case results:**
1. **200** PUT; **202** start-billing.
2. ≥1 invoice: `invoiceType` **INTERIM_AND_ADVANCE_PAYMENT**, `totalAmountIncludingVat` ≥ **5.00** (scaled; e.g. **inclFor500**), `invoiceNumber` not empty.

---

### TC-BE-8 (Negative): Manual interim — total incl. VAT 4.99 EUR — no invoice

**Description:** Manual interim below threshold must not persist invoice.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Prepare customer, contract, billing group, bank, VAT (as TC-BE-7 steps 2–3).
3. `POST /billing-run` → `{id}`.
4. `PUT /billing-run/{id}`: same as TC-BE-7 but `amountExcludingVat` = **exclFor499** (scaled incl. **&lt; 5.00**).
5. Baseline interim invoice count = **0** on listing.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`.
3. `GET /billing-run/{id}`.

**Expected test case results:**
1. **No** `INTERIM_AND_ADVANCE_PAYMENT` invoice for this billing run.
2. Billing run may be **DRAFT** with zero new interim invoices in listing.

---

### TC-BE-9 (Positive): `PRICE_COMPONENT` IAP — total incl. VAT ≥ 5.00 creates invoice

**Description:** Gate applies to `PRICE_COMPONENT` value type.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Create customer, POD, product with price component (formula **without** `$PRICE_PROFILE$`).
3. Add IAP `valueType: PRICE_COMPONENT`; set formula variables so computed total incl. VAT ≥ **5.00** (document formula and X values in Preconditions note).
4. Create product contract; open accounting period.
5. Create `STANDARD_BILLING` run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `GET /invoice?id={invoiceId}`; `GET /invoice/detailed-data?id={invoiceId}&page=0&size=10`.

**Expected test case results:**
1. Interim invoice with `totalAmountIncludingVat` ≥ **5.00**.
2. Detail `detailType` = **INTERIM_PRICE_COMPONENT**.

---

### TC-BE-10 (Negative): `PRICE_COMPONENT` IAP — total incl. VAT 4.99 — no invoice

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Create customer, POD, product, price component (no `$PRICE_PROFILE$`).
3. IAP `PRICE_COMPONENT` configured for total incl. VAT **4.99** (document variables).
4. Product contract; accounting period; standard billing run; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`.

**Expected test case results:**
1. **No** interim invoice for this billing run.

---

### TC-BE-11 (Negative): `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT` — total incl. VAT &lt; 5.00 — no invoice

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Create customer, POD, product contract with IAP `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`.
3. Create and finalize a **REAL** parent `STANDARD` invoice on the contract (`totalAmountIncludingVat: 100.00` or documented base).
4. Set IAP percent / preparation so derived interim total incl. VAT &lt; **5.00** (document `calculationValue` after preparation check).
5. Create new `STANDARD_BILLING` run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing`.

**Expected test case results:**
1. **No** new interim invoice.
2. Parent invoice unchanged (`id`, `totalAmountIncludingVat`).

---

### TC-BE-15 (Positive): `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT` — derived total incl. VAT ≥ 5.00 — creates invoice

**Description:** Mirror of **TC-BE-11** with percent chosen so derived interim `totalAmountIncludingVat` from the parent volume invoice is **≥ 5.00** after scaling; gate must **create** one interim invoice.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Create customer, POD, product contract with IAP `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`.
3. Create and finalize a **REAL** parent `STANDARD` / volume invoice on the contract (price component expression **100**, same chain as TC-BE-11).
4. Set IAP percent so derived interim total incl. VAT **≥ 5.00** (automation: **100%** of completed FOR_VOLUMES parent invoice — same pattern as `interimCases.spec.ts` REG-1046; Phoenix applies percent **per previous-invoice line**, not header total × percent only). TC-BE-11 uses expression **100** and **4%** with parent left in **DRAFT** for the below-gate case.
5. Create new `STANDARD_BILLING` run with `INTERIM_AND_ADVANCE_PAYMENT`; baseline interim count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}` — expect **202**.
2. Poll until **one** `INTERIM_AND_ADVANCE_PAYMENT` invoice exists for this run.
3. `POST /invoice/listing`; `GET /invoice?id={invoiceId}`.

**Expected test case results:**
1. Exactly **one** interim invoice; `totalAmountIncludingVat` **≥ 5.00** (matches scaled percent of parent incl. VAT).
2. Parent volume invoice unchanged (`id`, `totalAmountIncludingVat`).
3. Billing run not **FAILED** due to threshold alone.

---

### TC-BE-12 (Positive): Two interim IAPs on one run — positive amount invoices; below-threshold amount does not

**Description:** One product carries **two** interim-and-advance IAP rows on the **same** standard billing run: **IAP-A (positive / at gate)** uses `EXACT_AMOUNT` **exclFor500** (scaled incl. VAT **≥ 5.00**, e.g. **5.00** @ 20% VAT) and **must** produce an interim invoice; **IAP-B (negative / below gate)** uses `EXACT_AMOUNT` **exclFor499** (scaled incl. VAT **&lt; 5.00**, e.g. **4.99**) and **must not** produce a second interim invoice or liability. Confirms the 5 EUR gate is evaluated **per IAP row**, not only on the first row.

**Preconditions:**
1. Authenticate to **Dev** API; store Bearer token.
2. Create customer, POD, product with **two** IAPs on the same product:
   - **IAP-A (positive):** `valueType: EXACT_AMOUNT`, `calculationValue` = **exclFor500** (gate boundary).
   - **IAP-B (negative):** `valueType: EXACT_AMOUNT`, `calculationValue` = **exclFor499** (below gate).
3. Product contract with both IAPs active; accounting period.
4. One `STANDARD_BILLING` run with `INTERIM_AND_ADVANCE_PAYMENT` covering both rows; baseline interim invoice count = **0**.

**Test steps:**
1. `PATCH /billing-run/start-billing?billingRunId={id}`.
2. `POST /invoice/listing` — count `INTERIM_AND_ADVANCE_PAYMENT` for this billing run.

**Expected test case results:**
1. Exactly **one** interim invoice (from **IAP-A only**); `totalAmountIncludingVat` = scaled **inclFor500** (e.g. **5.00**).
2. **No** second interim invoice for **IAP-B** (below-threshold row).
3. Listing does not show a second `INTERIM_AND_ADVANCE_PAYMENT` row for the sub-threshold IAP.

---

## References

- **Jira:** PDT-2872 — `Cursor-Project/config/jira/attachments/PDT-2872/issue-rest.json`
- **Diagram:** `Cursor-Project/config/Diagrams/Bundle 5/...-Interim.drawio.svg`
- **Code:** `BillingRunInterimProcessingService` (VAT `setScale(12, HALF_UP)`, totals `setScale(2, HALF_UP)`), `BillingRunManualInterimAdvancePaymentProcess`, `EPBDecimalUtils.convertToCurrencyScale`
- **Rules:** `TC-STANDALONE-PRE.0`, `TC-NOSKIP-OBS.0` in `.cursor/rules/workspace/test_cases_structure.mdc`
- **Environment:** Dev
