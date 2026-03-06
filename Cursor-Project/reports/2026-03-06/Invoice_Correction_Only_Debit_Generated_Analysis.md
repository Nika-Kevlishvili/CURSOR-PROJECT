# Invoice Correction – Only Debit Generated (Billing Run 11179)

**Date:** 2026-03-06  
**Context:** User reported that when running invoice correction (billing run preview: `type=INVOICE_CORRECTION&id=11179`), only **debit** was generated. The original invoice had **two schedules**; the user corrected **one** schedule and left the **other** unchanged. Expected: both credit (reversal) and debit (corrected amounts); actual: only debit.

---

## 1. How invoice correction works in code

### 1.1 Two documents per corrected invoice

For each base invoice in an **INVOICE_CORRECTION** billing run, the system is designed to produce:

1. **Correction invoice (debit side)**  
   New invoice with corrected amounts (what the user sees as “only debit”).

2. **Reversal document (credit side)**  
   Credit note (or debit note) that reverses the original invoice, created in `BillingRunCorrectionService.reverseInvoices()`.

So “only debit” means: the **reversal (credit note) was not created**, while the correction invoice was.

### 1.2 Where the credit note is skipped

In `BillingRunCorrectionService.reverseInvoices()`:

- Reversal details are built by **cloning** the base invoice’s details via `cloneDetails()` and `cloneDetailsVatBase()`.
- If **both** cloned lists are **empty**, the method **returns without creating the reversal document**:

```java
List<InvoiceStandardDetailedData> newReversalDetailedData = cloneDetails(...);
List<InvoiceStandardDetailedDataVatBase> newReversalDetailedDataVatBase = cloneDetailsVatBase(...);
if (newReversalDetailedData.isEmpty() && newReversalDetailedDataVatBase.isEmpty()) {
    return;  // no credit note created
}
```

So when you see “only debit”, it is because the reversal step exited early: nothing was cloned from the base invoice.

---

## 2. Why the cloned list can be empty (one schedule corrected, two in base)

Cloning is **filtered** when **both** are true:

- `billingRun.getVolumeChange() == true`
- `billingRun.getPriceChange() == false`

In that case, a base detail is **only** cloned if it matches a row in `billing_run.correction_pods` with `full_reversal_needed = true` and the same `pod_id`, and the detail type is one of: **SCALE**, **SETTLEMENT**, **DISCOUNT**. Otherwise the detail is skipped (`continue`).

So:

- If **no** base detail matches (wrong PODs, wrong `full_reversal_needed`, or detail types not in SCALE/SETTLEMENT/DISCOUNT), **all** details are skipped → both lists empty → **no credit note**.
- If **either** `volumeChange == false` or `priceChange == true`, **no** filtering by correction PODs is applied; then all base details would be cloned and a credit note would normally be created. So your scenario (only debit) is consistent with **volumeChange = true** and **priceChange = false**, and with the current filter excluding every detail (e.g. only one schedule corrected and that schedule’s PODs/details not matching the filter, or `correction_pods` not populated for that schedule).

---

## 3. Relation to “two schedules” and PDT-2623

- You had **one** original invoice with **two** schedules; you corrected **one** and left the other as is.
- The logic in `cloneDetails` / `cloneDetailsVatBase` does **not** distinguish “schedules” explicitly; it filters by **POD** and **detail type** and by `billing_run.correction_pods` (and only when volumeChange && !priceChange).
- If the corrected schedule’s PODs are not in `correction_pods` with `full_reversal_needed`, or the details are not SCALE/SETTLEMENT/DISCOUNT, they are not cloned → nothing to put on the credit note → only debit is generated.

This is consistent with the behaviour already analysed in **PDT-2623** (credit note including non-corrected PODs / scope of correction): the same flags and the same `correction_pods` + detail-type logic govern **whether** a reversal is created and **what** it contains.

---

## 4. What to check for billing run 11179

1. **Billing run flags** (e.g. in DB or UI):
   - `volume_change`
   - `price_change`  
   If `volume_change = true` and `price_change = false`, the “only corrected PODs” filter is active; then the next step matters.

2. **Table `billing_run.correction_pods`** for this run:
   - Rows for run id = 11179 (and the correction run id used in your scenario).
   - For the **corrected** schedule: is the corresponding POD present and is `full_reversal_needed` set so that the base details (SCALE/SETTLEMENT/DISCOUNT) for that POD are included?
   - If this table is empty or not aligned with the corrected schedule, the Java filter will drop all details → empty clone → no credit note.

3. **Stored procedure**  
   `billing_run.run_standard_billing_main_data_preparation_correction(run_id)` fills:
   - which PODs are corrected,
   - `billing_run.correction_pods` (and `full_reversal_needed`),
   - and may influence `volume_change` / `price_change`.  
   So the root cause may be in this procedure (e.g. not marking the corrected schedule’s PODs correctly for reversal).

4. **Detail types**  
   Base invoice details that are not SCALE, SETTLEMENT or DISCOUNT are never cloned when the filter is on; if the corrected schedule only has other types, the reversal will be empty.

---

## 5. Conclusion and next steps

- **Observed:** Only the correction invoice (debit) was generated; the reversal (credit note) was not.
- **Mechanism:** `reverseInvoices()` creates the credit note only from cloned base details; when both cloned lists are empty it returns without creating the reversal. Empty lists occur when `volumeChange && !priceChange` and no base detail passes the `correction_pods` + detail-type filter.
- **Likely cause for “two schedules, one corrected”:** Either the corrected schedule’s PODs/details are not represented in `correction_pods` with `full_reversal_needed`, or their detail types are outside SCALE/SETTLEMENT/DISCOUNT, so everything is filtered out.
- **Next steps:**  
  - Check `volume_change` / `price_change` for run 11179.  
  - Check `billing_run.correction_pods` for run 11179 and the related correction run id.  
  - Verify the DB procedure `run_standard_billing_main_data_preparation_correction` so that the corrected schedule’s PODs get correct `correction_pods` rows and `full_reversal_needed` for the reversal to be created.

This behaviour is consistent with the existing bug validation report **BugValidation_PDT-2623_InvoiceCorrection_Includes_NonCorrected_PODs.md** (same correction and reversal logic).

---

## 6. Concrete scenario (user data)

**Identifiers:**

| Concept | ID |
|--------|-----|
| Main invoice | **86367** |
| Scales on main invoice (billed) | **117950**, **117951** |
| Correction scale (for 117950) | **117952** |
| Correction billing run | **11179** |

**Expected behaviour:**

1. **Credit note:** Reverse **only** the record for scale **117950** from the main invoice (i.e. credit note should contain only the reversal of 117950’s data from invoice 86367).
2. **Debit note:** Generate the new corrected amounts using the **correction scale 117952** (debit = new invoice lines with scale 117952).

So: credit = reverse 117950; debit = new 117952.

**Important: current code does not filter by scale ID.**

In `BillingRunCorrectionService.cloneDetails()` / `cloneDetailsVatBase()` the filter uses only:

- `billing_run.correction_pods.pod_id` (and `full_reversal_needed`)
- `baseDetail.podId`
- `baseDetail.detailType` ∈ { SCALE, SETTLEMENT, DISCOUNT }

There is **no** condition on `scale_id` or `billing_data_scale_id`. So:

- If scales **117950** and **117951** belong to **different PODs**, then for the credit to reverse only 117950, `correction_pods` must contain the **POD linked to scale 117950** with `full_reversal_needed = true`. Then all details (SCALE/SETTLEMENT/DISCOUNT) for that POD are cloned into the credit; if 117950 is the only scale on that POD, the effect is “reverse only 117950”.
- If scales **117950** and **117951** belong to the **same POD**, the current logic cannot “reverse only 117950”. It is all-or-nothing per POD: either all details for that POD are reversed (both 117950 and 117951) or none. To get “credit = only 117950 reversed, debit = only 117952” when both 117950 and 117951 are on the same POD would require **scale-level** filtering (e.g. by `scale_id` or billing data scale IDs) in the reversal logic, which is not implemented today.

**What to verify in DB for billing run 11179 and invoice 86367:**

1. Which **POD(s)** the main invoice 86367 details use for scales 117950 and 117951 (e.g. from `invoice_standard_detailed_data` or equivalent).
2. Whether 117950 and 117951 are on the **same** or **different** PODs.
3. For run 11179 (and the correction run id used for this invoice): contents of `billing_run.correction_pods` — which `pod_id` and `full_reversal_needed` values are set. If the POD for scale 117950 is missing or `full_reversal_needed` is false, the credit note will not be created (only debit).
4. If both scales share one POD and the requirement is strictly “credit = only 117950, debit = only 117952”, then the current design (POD-based filter only) cannot support it; a change to include scale-level filtering would be needed (in DB procedure and/or Java).
