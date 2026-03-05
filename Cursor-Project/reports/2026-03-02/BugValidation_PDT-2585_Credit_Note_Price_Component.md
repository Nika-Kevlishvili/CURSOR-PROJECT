# Bug Validation Analysis – PDT-2585

**Jira:** [PDT-2585](https://oppa-support.atlassian.net/browse/PDT-2585) – Frontend - URGENT: Billing - No Price component in a credit note  
**Validation date:** 2026-03-02  
**Rule:** 32 (BugFinderAgent workflow – READ-ONLY, no code modifications)  
**Validation run:** Bug-validator subagent (Confluence MCP unavailable this session)

---

## 1. Confluence validation

**Status:** **Partially correct** (inferred from project artifacts; Confluence MCP was not available in this session).

**Explanation:**  
Confluence could not be queried directly (MCP server `user-Confluence` is not in the available servers list). Assessment is based on:

- **Cross-dependency artifact:** `Cursor-Project/cross_dependencies/2026-03-02_PDT-2585-credit-note-price-component.json` and its `confluence_sources`.
- **Prior report:** Existing BugValidation report for PDT-2585.

Documentation supports: (1) Invoice details tabs – total volumes "if price component group - always empty"; summary/detailed from billing run; price component names must be shown. (2) Phase 2 Invoice correction – reversal invoice = opposite document; for invoice create credit note. (3) Manual credit or debit note – Basic, Summary data, Detailed data; reversal of invoice type. (4) Invoice preview pages – summary data with separate rows per price component/group; total volumes per price component.

**Conclusion:** Bug description (credit note: same price component names as original invoice; Total volumes empty, not 0) aligns with documented behaviour and project Confluence references.

**Sources (from cross_dependencies / prior report):**

| Title | Page ID |
|-------|----------|
| Invoice details: 1,2,3 tabs | 122978306 |
| Phase 2 - Invoice correction - process | 585730223 |
| Manual credit or debit note - Create | 151945240 |
| Service Order Invoice - preview | 25526337 |
| Product contract Invoice - Preview | 24969696 |

---

## 2. Code analysis

**Status:** **Does not satisfy** the bug report.

**Explanation:**  
For a **credit note** (invoice type REVERSAL) created from a **STANDARD** (non–order) invoice, summary data is loaded via the **same** path as for a standard invoice: no special handling for "credit note → price component names present, totalVolumes empty".

- **Entry point:** `InvoiceService.listSummaryData(Long id, InvoiceDataListingRequest request)`  
  - **File:** `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/billing/invoice/InvoiceService.java`  
  - **Lines:** 359–453 (method), 386–421 (REVERSAL branch).  
  - For `invoice.getInvoiceType() == REVERSAL` and original type `STANDARD`, non–order:  
    `invoiceStandardDetailedDataRepository.findAllSummaryDataByInvoiceId(id, PageRequest.of(...))` (lines 416–418).

- **Summary data query:** `InvoiceStandardDetailedDataRepository.findAllSummaryDataByInvoiceId(@Param("id") Long id, Pageable pageable)`  
  - **File:** `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/repository/billing/invoice/InvoiceStandardDetailedDataRepository.java`  
  - **Lines:** 36–225 (native query), 50 and 145:  
    - **totalVolumes:** `case when d.detail_type <> 'DISCOUNT' then sum(d.total_volumes) end` – no credit-note-specific logic; if data are stored as 0 or sum is 0, API returns 0 (not null/empty).  
    - **Price component name:** from `pc.name` via `join price_component.price_components pc on pc.id = d.pc_id` on `invoice.invoice_standard_detailed_data d`. If credit note rows have null/wrong `pc_id` or no rows, names are missing.

- **Alternative summary path (standard invoice summary):** `InvoiceDocumentDataRepository.getStandardInvoiceSummaryData(Long invoiceId)`  
  - **File:** `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/repository/billing/invoice/InvoiceDocumentDataRepository.java`  
  - **Lines:** 95–193. Same pattern: `price_component` from `pc.name`, `total_volumes` from `case when d.detail_type <> 'DISCOUNT' then sum(d.total_volumes) end`; no document-type distinction for credit note.

**Bug location:**  
(1) Use of the same summary repository/query for both invoice and credit note (REVERSAL + STANDARD) with no rule "credit note → totalVolumes empty".  
(2) Reliance on `invoice_standard_detailed_data` and `pc_id` for credit note; if reversal/creation does not populate or align `pc_id` (and optionally total_volumes) for the credit note document, price component names and "empty" total volumes are not achieved.

**Code references:**

| Location | File | Lines |
|----------|------|--------|
| listSummaryData, REVERSAL + STANDARD branch | phoenix-core-lib/.../invoice/InvoiceService.java | 386–421, 416–418 |
| findAllSummaryDataByInvoiceId (summary query) | phoenix-core-lib/.../invoice/InvoiceStandardDetailedDataRepository.java | 36–225 (50, 145 totalVolumes) |
| getStandardInvoiceSummaryData | phoenix-core-lib/.../invoice/InvoiceDocumentDataRepository.java | 95–193 |

---

## 3. Conclusion

1. **Bug report vs Confluence:** Correct per available project documentation (Confluence not queried this run; sources from cross_dependencies and prior report).
2. **Code vs bug report:** Does not satisfy – same summary path for invoice and credit note; no "totalVolumes empty" for credit note; price component names depend on data (pc_id) with no guaranteed alignment for credit note.
3. **Bug valid:** **YES**

**Suggested fix (do not implement here):**  
(1) Ensure reversal/credit note creation populates `invoice_standard_detailed_data` (or equivalent) for the credit note with correct `pc_id` so the summary query returns price component names (e.g. aligned with source invoice).  
(2) For credit note summary, set or return `totalVolumes` as null/empty where the business rule requires it (not 0) – either in persistence or in the API/DTO layer when document type is credit note.  
(3) Regression: keep same DTO/API where possible; verify manual credit/debit note and invoice correction flows (see `Cursor-Project/test_cases/Flows/Billing/Credit_note_summary_after_reversal.md`).

---

## 4. References

- **Jira:** https://oppa-support.atlassian.net/browse/PDT-2585  
- **Cross-dependency:** `Cursor-Project/cross_dependencies/2026-03-02_PDT-2585-credit-note-price-component.json`  
- **Test cases:** `Cursor-Project/test_cases/Flows/Billing/Credit_note_summary_after_reversal.md`
