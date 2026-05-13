# Customer Invoices – Invoice date label translation key (PDT-2811)

**Jira:** PDT-2811 (Phoenix Delivery)
**Type:** Bug
**Summary:** The customer invoices list UI incorrectly reuses the translation key `billing_data_by_scales.date_of_invoice`, which resolves to 'Дата на фактура от мрежови оператор'. This is a pure frontend translation / labelling defect; no backend API behaviour is affected.

**Scope:** Translation key mismatch in the customer invoices list UI. The fix involves introducing a new translation key for the customer invoices context and updating the two affected Angular component files. No backend endpoint, DTO, or service logic is changed.

> **⚠️ Mixed-state note (environment: Test):** Branch alignment to `origin/test` failed in the parent task due to a parser error. Backend test cases below are written from the cross-dependency context provided; local Phoenix source state may be partially stale.

---

## Test data (preconditions)

No backend test cases are applicable for this scope. The bug and its fix are purely in the Angular frontend layer (translation key wording). No backend API behaviour, endpoint, DTO field, or database value is affected.

---

## Backend Test Cases

*No backend test cases applicable for this scope.*

This defect is confined to the Angular UI translation layer:

- **Affected files (frontend only):**
  - `phoenix-ui/src/app/pages/customers/create-or-edit-customer/customer-invoices-list/customer-invoices-list.component.ts` – column `invoiceDate` caption key
  - `phoenix-ui/src/app/pages/customers/create-or-edit-customer/customer-invoices-list/list-filters/list-filters.component.html` – date range picker title key
- **Translation source (frontend only):**
  - `phoenix-ui/src/assets/i18n/bg.json` – `billing_data_by_scales.date_of_invoice = 'Дата на фактура от мрежови оператор'`

Backend test coverage for the data layer supporting customer invoices (billing runs, invoice generation, invoice retrieval API) is out of scope for this specific translation-key bug. If a regression in invoice data retrieval is suspected, that should be tracked under a separate backend test case topic.

---

## References

- **Jira:** PDT-2811 – Wrong invoice date label in customer invoices UI.
- **Related:** Frontend translation key `billing_data_by_scales.date_of_invoice`; `customer-invoices-list.component.ts`; `list-filters.component.html`; `bg.json`.
