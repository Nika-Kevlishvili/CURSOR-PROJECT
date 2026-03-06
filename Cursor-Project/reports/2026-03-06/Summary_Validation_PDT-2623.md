## Summary — PDT-2623 Bug Validation (2026-03-06)

### Outcome
- **Bug valid:** **YES**
- **Reason (high signal):** The invoice correction credit note POD scope is controlled by DB procedure output and conditional Java filtering; the current code path can include **non-corrected PODs**, matching the observed issue.

### Key references
- **DB procedure call (invoice correction preparation):** `billing_run.run_standard_billing_main_data_preparation_correction(run_id)`  
  `Cursor-Project/Phoenix/phoenix-core-lib/.../BillingRunStandardPreparationService.java` (lines 43–56)
- **Correction reversal invocation:**  
  `Cursor-Project/Phoenix/phoenix-core-lib/.../BillingRunStandardInvoiceGenerationProcessor.java` (lines 1319–1333)
- **POD filtering logic:**  
  `Cursor-Project/Phoenix/phoenix-core-lib/.../BillingRunCorrectionService.java` (lines 216–228)

### What to investigate next (most likely root cause)
- Stored procedure populates `billing_run.correction_pods` and flags `billing.billings.volume_change/price_change`. If it marks too many PODs as `full_reversal_needed=true`, the credit note will include them.

