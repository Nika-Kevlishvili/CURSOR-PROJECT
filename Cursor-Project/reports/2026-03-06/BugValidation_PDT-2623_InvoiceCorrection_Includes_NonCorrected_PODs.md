## Bug Validation Report — PDT-2623
Date: 2026-03-06

### 1) Bug report (from Jira screenshot)
**Title:** Invoice correction - problem

**Reproduce steps:**
- Perform invoice correction for invoice **1100040017** (billing run preview link contains `type=INVOICE_CORRECTION&id=2620`).
- Only POD **327140000019850D** has corrections for the period.
- System generates a **credit note** (summary data link contains `id=78743`) where **all PODs** from the invoice are included **except three** with measurement type `SETTLEMENT_PERIOD`:
  - `32Z140000201085L`
  - `32Z710300116688B`
  - `32Z140000176706T`
- Other PODs are with measurement type `SLP`.

**Expected:**
- In the credit note, **only the POD with the correction** should be included (POD `327140000019850D`).

---

### 2) Confluence validation (best-effort)
**Status:** Partially possible

**Limitations:** Confluence MCP calls require passing arguments (cloudId/pageId/CQL). In this workspace session, the MCP invocation helper is not accepting arguments, so I cannot fetch pages live.

**Local references (indirect Confluence anchors):**
- `Cursor-Project/test_cases/Flows/Billing/Credit_note_summary_after_reversal.md` references Confluence pages relevant to invoice correction and credit notes, including:
  - **Phase 2 - Invoice correction** (page id `585730223`)
  - **Invoice details tabs** (page id `122978306`)
  - **Manual credit or debit note - Create** (page id `151945240`)
  - **Product contract Invoice - Preview** (page id `24969696`)

These references support that invoice correction / credit note behavior is a documented business flow, but they do not directly state “only corrected PODs should be included” in the local artifacts.

#### 2.1 Is it written somewhere that it shouldn't be allowed?
**Answer:** In the **local workspace** (User story, FOR_VOLUMES docs, test cases) there is **no** explicit sentence stating that including all PODs in the correction credit note should not be allowed or that the credit note must include only the POD(s) with the correction.

- The **only** explicit formulation of that rule in our context is the **Jira ticket PDT-2623** (Expected: *In the credit note should be included only the POD with the correction.*).
- The place where such a business rule would normally be documented is **Confluence**, in particular the page **Phase 2 - Invoice correction** (page id 585730223). To confirm whether the rule is formally documented, open that Confluence page and search for wording about credit note scope / corrected PODs only.

---

### 3) Code analysis (Phoenix)
**Status:** Completed (codebase = source of truth)

#### 3.1 Where invoice correction data preparation happens (DB stored procedure)
Invoice correction preparation is delegated to a database stored procedure:

```43:60:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/service/BillingRunStandardPreparationService.java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void startDataPreparationCorrection(BillingRun billingRun) {
    ...
    CallableStatement statement = work.prepareCall("CALL billing_run.run_standard_billing_main_data_preparation_correction(?)");
    statement.setLong(1, runId);
    ...
    statement.execute();
    ...
}
```

This aligns with your note that “most of the logic is in DB procedures”: **the correction scope (which PODs are corrected) is expected to be determined in the DB layer**.

#### 3.2 Where the credit note is created for INVOICE_CORRECTION
During invoice generation, the correction reversal path is invoked:

```1319:1334:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/service/BillingRunStandardInvoiceGenerationProcessor.java
if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
    billingRunCorrectionService.reverseInvoices(
            billingRunModel,
            baseCorrectionInvoice,
            invoice,
            invoiceDetails,
            standardVatBaseDetails,
            runContract.getContractType(),
            currencyHashMap,
            mainCurrencyId,
            altCurrencyId,
            mainCurrency,
            correctionRunId
    );
}
```

#### 3.3 How “which PODs are included” is decided
`BillingRunCorrectionService.reverseInvoices()` clones detailed rows from the **base (original) invoice** into a new reversal document. The filtering by “corrected PODs only” is **conditional** and depends on:
- billing run flags: `volumeChange == true` AND `priceChange == false`
- a DB-populated table `billing_run.correction_pods` (mapped as `BillingRunCorrectionPods`)
- and **only** for specific detail types

Key logic:

```212:228:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/billingRun/service/BillingRunCorrectionService.java
for (InvoiceStandardDetailedData baseDetail : baseDetailedData) {
    if (billingRun.getVolumeChange() && !billingRun.getPriceChange()) {
        if (correctionPodsList.stream().noneMatch(f -> f.getFullReversalNeeded()
                && f.getPodId().equals(baseDetail.getPodId())
                && (
                    baseDetail.getDetailType().equals(InvoiceStandardDetailType.SCALE)
                    || baseDetail.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT)
                    || baseDetail.getDetailType().equals(InvoiceStandardDetailType.DISCOUNT)
                )
        )) {
            continue;
        }
    }
    ...
}
```

**Implications for PDT-2623:**
- If `volumeChange=false` **or** `priceChange=true`, then **no POD filtering happens** and **all PODs from the base invoice** will be cloned into the correction credit note.
- If `volumeChange=true` and `priceChange=false`, filtering happens, but:
  - it relies on DB rows in `billing_run.correction_pods` and the boolean `full_reversal_needed`
  - it only filters for `SCALE`, `SETTLEMENT`, `DISCOUNT` detail types

That means the Jira-observed behavior “all PODs are included except three with measurement type SETTLEMENT_PERIOD” is consistent with a scenario where:
- the **DB procedure populates `correction_pods` too broadly** (e.g., marks many/all PODs as `full_reversal_needed = true`), and
- the three excluded PODs correspond to details that do **not** land in `SCALE/SETTLEMENT/DISCOUNT` rows during cloning (or are stored outside `invoice_standard_detailed_data`).

---

### 4) Conclusion
#### 4.1 Confluence validation
**Status:** Incomplete (tooling limitation in this session).

#### 4.2 Code validation vs expected behavior
**Status:** Does **not** satisfy the expected behavior as stated in PDT-2623.

The current implementation does **not guarantee** that an invoice correction credit note includes **only** the POD(s) that have corrections. Inclusion depends on:
- `billing.billings.volume_change` / `billing.billings.price_change` flags, and
- DB procedure output (`billing_run.correction_pods.full_reversal_needed`), and
- detail type filtering.

#### 4.3 Bug validity
**Bug valid:** **YES** (based on the ticket’s expected behavior and the code path that can include non-corrected PODs).

---

### 5) Most likely root cause (where to look next)
- **DB stored procedure**: `billing_run.run_standard_billing_main_data_preparation_correction(run_id)` likely populates:
  - which PODs are “corrected” for that correction run
  - `billing_run.correction_pods` rows and their `full_reversal_needed` flag
  - `billing.billings.volume_change` / `billing.billings.price_change`

If the procedure marks all PODs as needing full reversal when only one POD has corrections, the Java layer will clone all those PODs.

---

### 6) Suggested fix (no code changes made)
- **DB-side**: Ensure `billing_run.correction_pods` contains **only** the POD(s) that truly have corrections for the selected period, and set `full_reversal_needed=true` only for those POD(s).
- **Java-side hardening** (optional, depends on intended business rule):
  - Apply correction-pod filtering for **all** `INVOICE_CORRECTION` runs (not only `volumeChange && !priceChange`), if the business rule is “credit note should include only corrected PODs” regardless of change type.
  - Revisit the `detailType` whitelist `(SCALE|SETTLEMENT|DISCOUNT)` to avoid unintentionally excluding corrected POD lines of other types.

