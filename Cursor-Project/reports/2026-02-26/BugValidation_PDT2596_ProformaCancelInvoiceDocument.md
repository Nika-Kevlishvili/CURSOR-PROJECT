# Bug Validation Report: PDT-2596

**Title:** Service Order - Cancel Proforma invoice - Cancel Invoice document should not be created  
**Jira:** [PDT-2596](https://oppa-support.atlassian.net/browse/PDT-2596)  
**Date:** 2026-02-26  
**Validation:** Confluence + Codebase (Rule 32, READ-ONLY)

---

## 1. Confluence Validation

**Status:** Partially correct – no explicit Confluence page found for this rule.

**Explanation:**
- Rovo Search (Jira + Confluence) was used with queries: "PDT-2596 Service Order Cancel Proforma invoice", "proforma invoice cancellation cancel invoice document", "invoice_cancellation_numbers proforma", "Phoenix invoice proforma cancel document", "proforma invoice cancel document creation business rule".
- **Found:** The Jira issue PDT-2596 and related Jira issues (PHX-13141, PHX-13120, PHX-13117, PHX-13116, PHX-13112, etc.) describing proforma invoice cancellation and Cancel Invoice document behavior.
- **Not found:** No Confluence documentation page was returned that explicitly states the business rule: "Cancel Invoice document should not be created for cancelled proforma invoices" or "invoice_cancellation_numbers should not be populated for proforma cancellations."
- The expected behavior described in the bug (no Cancel document and no row in invoice_cancellation_numbers for proforma) is a reasonable business rule and is not contradicted by any Confluence content found.

**Sources:** Jira PDT-2596; related Jira issues (PHX-13141, PHX-13120, PHX-13117, PHX-13116, PHX-13112). No Confluence page IDs/URLs to list.

---

## 2. Code Analysis

**Status:** Code does **not** satisfy the bug report's expected behavior.

**Explanation:**
- The cancellation flow is implemented in `InvoiceCancellationProcessor` and `InvoiceCancellationService`.
- In `InvoiceCancellationProcessor.processInvoiceCancellation()` the decision to create the Cancel Invoice document and to insert into the cancellation numbers table is based **only** on `invoice.getInvoiceStatus()`:
  - **case REAL:** the code sets cancellation id, calls `setInvoiceCancellationNumber()`, creates the Cancel Invoice document via `generateCancellationDocument()`, and sends email.
  - There is **no** check for `invoice.getInvoiceDocumentType()` (e.g. PROFORMA_INVOICE vs INVOICE).
- Therefore, when a **proforma** invoice has status **REAL** (issued proforma), it is processed exactly like a final invoice: a new row is saved in the cancellation numbers table and a Cancel Invoice document is generated.
- `setInvoiceCancellationNumber()` (lines 381–413) saves a new `InvoiceCancellationNumber` via `invoiceCancellationNumberRepository.save(...)`, which corresponds to the `invoice.invoice_cancellation_numbers` table mentioned in the bug.
- `generateCancellationDocument()` (lines 426+) creates and stores the Cancel Invoice document.

**Code references:**

| Location | File | Lines | Description |
|----------|------|-------|-------------|
| Cancellation logic by status | `phoenix-core-lib/.../invoice/cancellation/InvoiceCancellationProcessor.java` | 255–290 | `switch (invoice.getInvoiceStatus())` – only REAL creates document and number; no PROFORMA_INVOICE branch. |
| Cancellation number persistence | Same file | 381–413 | `setInvoiceCancellationNumber()` – builds and saves `InvoiceCancellationNumber` (table invoice_cancellation_numbers). |
| Document generation | Same file | 426+ | `generateCancellationDocument()` – creates Cancel Invoice document for any invoice in REAL status. |
| Proforma handling (liabilities only) | `phoenix-core-lib/.../invoice/cancellation/InvoiceCancellationService.java` | 318–350 | `processProformaInvoice()` handles liabilities/receivables; it does not control creation of Cancel document or cancellation number. |

**Conclusion from code:** The bug report's **Actual** behavior (Cancel Invoice document created and row in invoice_cancellation_numbers for cancelled proforma) is exactly what the current code does when the proforma has status REAL. The **Expected** behavior (no document and no row for proforma) is **not** implemented; the code does not distinguish proforma from final invoice in this step.

---

## 3. Conclusion

**Bug valid:** **YES**

**Summary:**
- **Confluence:** No Confluence page was found that explicitly documents this rule; the expected behavior is stated in the bug and is not contradicted.
- **Code:** The implementation does **not** satisfy the expected behavior. Cancel Invoice document and invoice_cancellation_numbers row are created for any invoice with status REAL, including proforma invoices. The bug report correctly describes the current (incorrect) behavior and the desired (correct) behavior.

**Suggested fix (analysis only – no code changes):**  
In `InvoiceCancellationProcessor.processInvoiceCancellation()`, when handling `case REAL`, add a condition on `invoice.getInvoiceDocumentType()`. If the document type is `PROFORMA_INVOICE`, do **not** call `setInvoiceCancellationNumber()` and do **not** call `generateCancellationDocument()` / send cancellation email; only update invoice status and cancellation id (and any other non-document/non-number fields required for proforma cancellation). Ensure `InvoiceCancellationService.processProformaInvoice()` remains responsible for liability/receivable handling. This would align behavior with the expected result described in PDT-2596.

---

*Report generated as part of bug validation workflow (Rule 32). READ-ONLY; no code was modified.*
