# Missing interim invoice – standard billing run with interim/advance model (PDT-2750)

**Jira:** PDT-2750 (Phoenix Delivery / delivery board per project)  
**Type:** Bug  
**Summary:** Standard billing runs that include interim and advance payment processing must create interim invoices when interim terms use **PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT** (or equivalent paths). The parent reference for calculation must follow **Jira-comment expectation**: prefer the **latest previous REAL standard invoice on the same contract**; if none qualifies, fall back to the **latest previous REAL invoice that includes at least one POD**. Production example: customer UIC **202629378**, contract churn — **ПКСП-2501002782** POD deactivated **28.02.2026**, **EPES2602001270** POD active **01.03.2026**; standard invoice **1100102503** existed but **no interim** for run **BILLING202603200011** (**20.03.2026**).

**Scope:** Backend verification covers stored procedure **`billing_run.run_standard_billing_interim_data_preparation`**, persistence in **`billing_run.run_interim_data`** (`prev_invoice_id`, `is_valid_for_generation`), **`BillingRunStandardInvoiceGenerationProcessor.startInterimProcessing`**, **`BillingRunInterimProcessingService.process`** (including **`PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`** and **`interimCalculatedFromInvoiceId`**), **`BillingRunStartGenerationInvokeService.getValidInvoices`** (PDF generation eligibility vs REAL parent), and post-accounting **deduction** behaviour for standard billing. **Executable target environment:** Test (reproduce using Test APIs and DB read-only checks — **do not** rely on PROD identifiers).

---

## Jira comments interpreted into checks

These assertions are **explicit acceptance checks** derived from Jira discussion (including the clarification comment that the interim **should** relate to invoice **1100102503** / same scenario).

| # | Interpreted rule | Concrete check |
|---|------------------|----------------|
| J1 | **Same-contract parent first** | For each interim row tied to contract **C**, `prev_invoice_id` (or equivalent source for PERCENT path) MUST resolve to the **latest chronologically previous** invoice that is **REAL**, **standard-type billable invoice** for **contract C**, among invoices eligible by business rules (same customer legal entity / billing context as configured). |
| J2 | **Fallback across invoices with POD lines** | If no invoice satisfies J1 for contract **C**, the system MUST fall back to the **latest previous REAL invoice** (any eligible contract for that customer/run context per product rules) that contains **at least one POD-linked standard detail line** (invoice “includes ≥1 POD”). |
| J3 | **REAL status matters for PDF / generation gate** | Any gate that blocks interim document generation when the parent invoice is **not** in the current run’s invoice batch MUST still allow generation **if** the parent invoice exists in the database with **`InvoiceStatus.REAL`** (see code path under **`BillingRunStartGenerationInvokeService.getValidInvoices`**). |
| J4 | **Churn around month boundary** | When an **old** product contract’s POD is **deactivated** on **last days of month N** and a **new** product contract’s POD is **active from first day of month N+1**, a **March** standard billing run must still produce an **interim** row and invoice when interim IAP terms apply — **not** silently skip because POD moved between contracts. |
| J5 | **Audit vs facts** | Backend logs / billing error publications MUST NOT claim success paths that contradict absence of an interim invoice row or **`InvoiceType.INTERIM_AND_ADVANCE_PAYMENT`** invoice for the run (misleading audit called out in ticket). |

**Evidence (code — read-only):** Interim processing sets parent reference from preparation data for PERCENT path:

```281:283:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/service/BillingRunInterimProcessingService.java
        } else if (interimData.getValueType().equals("PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT")) {
            invoice.setInterimCalculatedFromInvoiceId(interimData.getPrevInvoiceId());
```

PDF batch filtering for interim invoices when parent is **outside** the current run’s invoice set:

```215:232:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/billing/billingRun/actions/startGeneration/BillingRunStartGenerationInvokeService.java
    private List<Invoice> getValidInvoices(BillingRun billingRun, List<Invoice> invoicesForDocumentGenerationContext) {
        List<Invoice> validInvoices = new ArrayList<>();
        Set<Long> invoiceIdSet = invoicesForDocumentGenerationContext.stream().map(Invoice::getId).collect(Collectors.toSet());
        List<InvoiceErrorShortObject> errors = new ArrayList<>();
        for (Invoice invoice : invoicesForDocumentGenerationContext) {
            if (invoice.getInvoiceType().equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT) && invoice.getInterimCalculatedFromInvoiceId() != null && !invoiceIdSet.contains(invoice.getInterimCalculatedFromInvoiceId())) {
                if (invoiceRepository.existsInvoiceByIdAndInvoiceStatus(invoice.getInterimCalculatedFromInvoiceId(), InvoiceStatus.REAL)){
                    validInvoices.add(invoice);
                }{
                    Optional<String> interimErrorObject = invoiceRepository.getInterimErrorObject(invoice.getId());
                    errors.add(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), "PDF is not generated for interim (%s) for this contract %s".formatted(invoice.getInvoiceNumber(), interimErrorObject.orElse(""))));
                }
            } else {
                validInvoices.add(invoice);
            }
        }
```

> **Note:** The excerpt above matches the workspace file; the `}{` fragment appears to be a **syntax defect** in the repository copy — resolution logic should be validated during bug fix. Tests still assert **observable behaviour**: interim PDF generation when parent is REAL.

Interim preparation invocation:

```62:75:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/service/BillingRunStandardPreparationService.java
    public void startInterimAdvancePaymentPreparation(BillingRun billingRun) {
        ...
                CallableStatement statement = work.prepareCall("CALL billing_run.run_standard_billing_interim_data_preparation(?)");
                ...
                statement.setLong(1, runId);

                statement.execute();
```

Entity shape for **`prev_invoice_id`** / **`is_valid_for_generation`**:

```21:84:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/model/entity/BillingRunInterimData.java
    @Column(name = "is_valid_for_generation")
    private Boolean isValidForGeneration;
    ...
    @Column(name = "prev_invoice_id")
    private Long prevInvoiceId;
```

---

## Test data (preconditions)

Shared chain for **Test** environment. Replace `{…}` placeholders with values from **`Cursor-Project/config/swagger/test/swagger-spec.json`** after refresh (Rule SWAGGER.0). All POST bodies MUST match Swagger **`required`** fields and **`enum`** literals exactly.

- **Environment:** Test (`BASE_URL` pointing at Test Phoenix API). Database checks (optional): PostgreSQL Test — schema **`billing_run.run_interim_data`**, **`billing.invoice`** (read-only SELECT).

1. Create a **customer** via `POST /customer` (or equivalent customer-create endpoint per Swagger) with **type** and **status** valid for billing (e.g. PRIVATE / ACTIVE), **customerIdentifier** unique — note **`customerId`** / **`customerDetailId`** from response.
2. Create **POD (Point of Delivery)** **P1** via `POST /pod` — **type** ELECTRICITY (or gas per product), **status** ACTIVE, **activationDate** ≤ **2026-01-01**, identifier unique — note **`podId`**.
3. Create **product** via `POST /product` with **Interim and Advance Payment (IAP)** terms configured (attach interim advance payment terms via product/service setup endpoints per Swagger — **percent-from-previous-invoice** style value type **`PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`** where applicable) — note **`productId`**.
4. Create **terms** via `POST /terms` linked to product from step 3 (per Swagger).
5. Create **price component(s)** via `POST /price-component` linked to product from step 3 (energy/grid/tax as required for standard invoice lines).
6. Create **product contract A** (“old” contract) via `POST /product-contract` — link **customer** from step 1, **POD** from step 2, **product** from step 3; **status** ACTIVE; **entryIntoForceDate** **2026-01-01**; **terminationDate** **null** initially; ensure contract-POD row supports billing — note **`contractAId`**, **`contractADetailId`**.
7. Attach **contract POD** row for contract A with **deactivationDate** **2026-02-28** (simulate prod **28.02.2026**) via contract POD update endpoint per Swagger — POD must be **inactive** after that date for billing selection tests.
8. Create **product contract B** (“new” contract) via `POST /product-contract` — same **customer**, same physical **POD** from step 2, same or successor **product** per migration scenario; **entryIntoForceDate** **2026-03-01**; **status** ACTIVE — note **`contractBId`**, **`contractBDetailId`**.
9. Create **energy data / billing profile** for **contract A × POD** for periods covering **Jan–Feb 2026** via energy-data endpoints (scales or profile per product).
10. Create **energy data / billing profile** for **contract B × POD** for periods covering **Mar 2026** onward.
11. Create **accounting period** / ensure **billing group** membership per Phoenix rules so both contracts participate in the **same standard billing run** for **March 2026** (use nomenclature endpoints as needed).
12. Execute **prior billing runs** as needed so **invoice I_old** exists: a **REAL**, **STANDARD** invoice for **contract A** with **invoiceDate** / tax event in **February 2026** and **≥1** POD-linked standard detail — amounts non-zero for percent calculation. Transition invoice to **REAL** via normal billing accounting flow (`InvoiceStatus.REAL`).
13. Ensure **no** newer REAL standard invoice exists on **contract B** before March run that would supersede fallback rules **unless** testing J1 specifically (adjust dates accordingly per TC).

---

## Backend Test Cases

### TC-BE-1 (Positive): Month-boundary churn — interim created for March standard run

**Description:** Reproduce the **PROD-analogue** churn: contract A POD ends **2026-02-28**, contract B POD starts **2026-03-01**. Verify an interim invoice is **created** for the **March** standard billing run when **`ApplicationModelType`** includes **INTERIM_AND_ADVANCE_PAYMENT**.

**Preconditions:**
1. Complete steps 1–13 from **Test data**, scheduling dates analogously (Feb last day / Mar first day).
2. Configure IAP on the product so interim rows use **`PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`** with a positive **calculation percent**.
3. **Invoice I_old** (contract A) is **REAL** and is the **latest previous REAL standard** invoice for **contract B’s** IAP resolution **or** satisfies **J2** fallback (document actual expected parent ID per comment expectation).

**Test steps:**
1. Create a **standard billing run** via billing-run API (`POST` create per Swagger) for **March 2026** including **customer** from step 1 and billing scope covering **contract B**’s active POD period.
2. Trigger **main data preparation**: procedure **`billing_run.run_standard_billing_main_data_preparation`** is invoked by the service (`BillingRunStandardPreparationService.startDataPreparation`) — drive via API “start preparation” / workflow step exposed for standard billing.
3. Trigger **interim data preparation**: ensures **`billing_run.run_standard_billing_interim_data_preparation`** executes (`startInterimAdvancePaymentPreparation`).
4. Query **`billing_run.run_interim_data`** (read-only) for **`run_id`** = created billing run id — rows with **`status` = 'CREATED'** and **`is_valid_for_generation` = true**.
5. Advance billing run through **interim processing** (`startInterimProcessing` phase) until interim invoices exist in **`billing.invoice`** with **`invoice_type`** = **INTERIM_AND_ADVANCE_PAYMENT** (or enum string per DB).
6. Assert **`interimCalculatedFromInvoiceId`** on the interim invoice equals **`prev_invoice_id`** from **`run_interim_data`** and matches **J1/J2** expectation.

**Expected test case results:** At least **one** interim invoice exists for the run; **`run_interim_data`** shows **`prev_invoice_id`** pointing to **I_old** or another qualifying REAL invoice per **J2**; interim line amounts match percent of **I_old** standard details within rounding rules; no bogus “success” without persisted interim invoice (**J5**).

**Actual result (if bug):** Standard invoice generates but **no** interim invoice row; or **`is_valid_for_generation` = false** without documented business reason; or **`prev_invoice_id`** null while IAP requires previous invoice.

---

### TC-BE-2 (Positive): J1 — Latest REAL standard invoice on **same** contract is selected

**Description:** When multiple REAL standard invoices exist on **contract B**, **`prev_invoice_id`** must reference the **latest** eligible one (by business ordering — typically issue date DESC then id DESC; confirm with DB query or service spec).

**Preconditions:**
1. Same customer/product/POD base as **Test data**.
2. **Contract B** has **two** REAL standard invoices **I1**, **I2** with **I2** strictly later than **I1**.
3. IAP interim row is generated for the **next** billing run.

**Test steps:**
1. Run interim preparation for the billing run.
2. Read **`run_interim_data.prev_invoice_id`** for contract B’s interim row.
3. Compare with **I1.id** and **I2.id**.

**Expected test case results:** **`prev_invoice_id` = I2.id** (latest previous REAL on **same** contract).

---

### TC-BE-3 (Positive): J2 — Fallback to latest REAL invoice with ≥1 POD when same-contract parent missing

**Description:** If **contract B** has **no** prior REAL standard invoice, interim preparation must set **`prev_invoice_id`** to the **latest REAL** invoice (for allowed customer scope) that has **at least one** POD-associated standard detail line.

**Preconditions:**
1. **Contract B** is new with **no** prior REAL standard invoice.
2. **Contract A** (or another eligible contract) holds **I_fallback**: REAL standard invoice with POD lines.
3. IAP configured to allow fallback per product/billing rules.

**Test steps:**
1. Execute **`run_standard_billing_interim_data_preparation`** for the run.
2. Inspect **`run_interim_data`** for **`prev_invoice_id`**.

**Expected test case results:** **`prev_invoice_id`** = **I_fallback.id**; **`is_valid_for_generation`** true when IAP data complete.

---

### TC-BE-4 (Positive): PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT line math matches parent REAL invoice

**Description:** **`BillingRunInterimProcessingService`** builds **`INTERIM_PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`** details from **`findAllStandardDetailsByInvoiceId(prev_invoice_id)`** — totals must equal configured percent of parent lines (per **`calculation_value`**).

**Preconditions:**
1. **`run_interim_data.value_type`** = **`PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`** with known **calculation_value** (e.g. 10%).
2. Parent REAL invoice has stable **standard detailed lines** with known net/VAT.

**Test steps:**
1. Complete interim processing through invoice persistence.
2. Query **`invoice_standard_detailed_data`** (or API invoice GET) for interim invoice lines **`detail_type`** = **INTERIM_PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT**.
3. Compare to manual calculation from parent lines.

**Expected test case results:** Amounts align within **allowed rounding**; **`interim_calculated_from_invoice_id`** populated.

---

### TC-BE-5 (Positive): PDF generation allowed when parent REAL invoice is **outside** current run batch

**Description:** Validates **J3** against **`getValidInvoices`**: interim invoice references **`interimCalculatedFromInvoiceId`** not in **`invoiceIdSet`** but parent exists with **REAL** status → interim remains in PDF queue.

**Preconditions:**
1. Interim invoice in **DRAFT** with **`interimCalculatedFromInvoiceId`** pointing to **prior-run** REAL invoice **not** listed in this run’s **`findInvoicesForDocumentGeneration`** batch.
2. **`InvoiceRepository.existsInvoiceByIdAndInvoiceStatus(parentId, REAL)`** returns true.

**Test steps:**
1. Invoke **document generation** phase for standard billing with **`INTERIM_AND_ADVANCE_PAYMENT`** application model.
2. Observe whether interim PDF is produced and **`billing_run` errors** list does **not** contain “PDF is not generated for interim…” for this invoice.

**Expected test case results:** PDF generated (or explicit non-bug skip reason **other** than missing REAL parent).

**Actual result (if bug):** Error published while parent is REAL — aligns with **what_could_break** “interim PDF gating vs REAL parent invoice”.

---

### TC-BE-6 (Negative): No qualifying REAL parent — interim row invalid or error surfaced

**Description:** When **`prev_invoice_id`** points to a non-REAL invoice or preparation finds no candidate, system must **not** silently emit a misleading success (**J5**).

**Preconditions:**
1. Force **`prev_invoice_id`** to an invoice still in **DRAFT** / non-REAL **or** configure data so preparation yields **`is_valid_for_generation` = false** (per procedure rules).

**Test steps:**
1. Run interim preparation and processing.
2. Inspect **`run_interim_data`**, billing errors, invoice list.

**Expected test case results:** Either **no** interim invoice **or** row **`FAILED`** / **`is_valid_for_generation` false** with **clear** **`error_message`**; billing errors published consistently.

---

### TC-BE-7 (Negative): Wrong customer isolation — must not pick another customer’s REAL invoice

**Description:** Regression for **`InvoiceRepository`** / procedure joins: **`prev_invoice_id`** must never reference another customer’s invoice.

**Preconditions:**
1. Second customer **C2** with its own REAL invoice **I_foreign**.
2. **C1** billing run with IAP.

**Test steps:**
1. Run interim preparation for **C1**.
2. Verify **`prev_invoice_id`** ≠ **I_foreign.id** under all rows for **C1**’s run.

**Expected test case results:** Strict isolation; no cross-customer parent pick.

---

### TC-BE-8 (Negative): Application model without INTERIM_AND_ADVANCE_PAYMENT — no interim branch executed

**Description:** If billing run **`applicationModelType`** omits **INTERIM_AND_ADVANCE_PAYMENT**, **`getValidInvoices`** filtering and interim preparation must **not** apply incorrectly.

**Preconditions:**
1. Standard billing run created **without** interim application model flag.

**Test steps:**
1. Execute full standard billing pipeline.
2. Confirm **`startInterimAdvancePaymentPreparation`** not applicable / no **`run_interim_data`** rows **or** processor skips per product rules.

**Expected test case results:** No orphan interim invoices; behaviour matches model flags (**what_could_break**: missing interim branch).

---

### TC-BE-9 (Edge): Parent invoice has multiple POD lines — still satisfies “≥1 POD” fallback **J2**

**Description:** Fallback candidate must accept invoices with **multiple** POD-linked lines.

**Preconditions:**
1. **I_multi**: REAL invoice with **≥2** POD-related standard lines.

**Test steps:**
1. Exercise **J2** path selecting **I_multi**.

**Expected test case results:** **`prev_invoice_id`** resolves correctly; amounts aggregate correctly.

---

### TC-BE-10 (Regression): Standard billing accounting deducts interim against REAL standard invoices

**Description:** After **`BillingRunStartAccountingInvokeService`** completes standard billing, **`deductInterimInvoices`** runs — verify deductions created without duplicate liability corruption (**what_could_break**: deduction linkage queries).

**Preconditions:**
1. REAL standard invoice + REAL interim from same run/customer context.

**Test steps:**
1. Complete accounting phase for billing run.
2. Query deduction / liability tables or invoke receivable/invoice GET endpoints per Swagger.

**Expected test case results:** Deduction records link interim to parent per design; no orphan receivables.

---

### TC-BE-11 (Edge): Concurrent REAL invoices old vs new contract around boundary — ordering

**Description:** With **I_old** on contract A (Feb REAL) and early **I_new** on contract B (Mar REAL if any), resolve **J1 vs J2** per ticket comment (interim for March run should still anchor to **latest eligible REAL** — typically **I_old** or first REAL on B depending on configuration).

**Preconditions:**
1. Controlled timeline matching **TC-BE-1** with optional extra REAL on B.

**Test steps:**
1. Run March billing; capture **`prev_invoice_id`**.
2. Compare against ordered list of REAL invoices.

**Expected test case results:** Selection matches **J1/J2** documented expectation for your configuration copy (test case documents **expected id** before execution).

---

### TC-BE-12 (Negative): Procedure failure surfaces — `run_standard_billing_interim_data_preparation` error handling

**Description:** If DB procedure raises (constraint / data defect), Java layer logs error and **does not** mark interim preparation as success silently.

**Preconditions:**
1. Use invalid **`run_id`** in controlled DB test **or** corrupted FK scenario **only** in disposable Test DB per QA policy.

**Test steps:**
1. Invoke interim preparation.

**Expected test case results:** Exception logged; **`run_interim_data`** not partially marked SUCCESS incorrectly; **`BillingRun`** shows failed/partial interim preparation status per enum.

---

## References

- **Jira:** [PDT-2750](https://oppa-support.atlassian.net/browse/PDT-2750) — Missing interim invoice (PROD repro references UIC **202629378**, run **BILLING202603200011**).
- **Confluence (cross_dependency hint):** “Interim advance payment process change” — fetch via MCP when validating business wording against implementation.
- **Code:** `BillingRunStandardPreparationService`, `BillingRunStandardInvoiceGenerationProcessor`, `BillingRunInterimProcessingService`, `BillingRunStartGenerationInvokeService`, `BillingRunInterimData`, `InvoiceRepository`.
