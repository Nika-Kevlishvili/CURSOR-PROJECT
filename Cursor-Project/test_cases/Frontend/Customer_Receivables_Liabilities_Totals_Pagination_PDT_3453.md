# Customer Liabilities and receivables Amount board totals – pagination (PDT-3453)

**Jira:** [PDT-3453](https://oppa-support.atlassian.net/browse/PDT-3453) (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** On Customer → **Liabilities and receivables**, the Amount board **TOTAL Initial Amount** and **TOTAL Current Amount** must show the sum for **all pages**, must not change when only switching pages, and must change when filter/search checkboxes change.

**Scope:** Phoenix portal UI Amount board + pagination on the Liabilities and receivables tab. Target environment: **dev**. Backend contract used for correlation: `POST /customer/{customerId}/customer-liability-and-receivable`.

---

## Test data (preconditions)

Shared reference only. Each TC includes its own full numbered setup (Rule TC-STANDALONE-PRE.0).

- **Environment:** Dev portal `{BASE_URL}`.
- **Permission:** User with **View Customer Liabilities and receivables**.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Amount board totals do not change when navigating to the next page

**Description:** With a customer that has more than 25 uncovered liabilities/receivables, verify the Amount board TOTAL Initial Amount and TOTAL Current Amount remain identical after moving from page 1 to page 2 (UI pagination).

**Preconditions:**
1. Open `{BASE_URL}/login`, log in with a user that has **View Customer Liabilities and receivables**, confirm the portal shell loads.
2. Resolve open `accountingPeriodId` and default `currencyId` from the API/nomenclatures used by the environment.
3. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, unique `customerIdentifier`). Store `customerId` and identifier.
4. Create **30** liabilities via `POST /customer-liability` for `customerId`, each with `accountingPeriodId`, `currencyId`, `occurrenceDate` today, `dueDate` today+30 days, `initialAmount`: `10.00`, block flags unset/false.
5. In the portal: open **Customers**, search by the identifier from step 3, open the customer detail.
6. Open the **Liabilities and receivables** tab (after **Invoice**; use the label present in the build). Confirm the grid shows pagination with more than one page (default page size **25**).

**Test steps:**
1. On page 1 of the Liabilities and receivables grid, read and record Amount board **TOTAL Initial Amount** as `UI_T0_init` and **TOTAL Current Amount** as `UI_T0_curr`.
2. Navigate to page 2 using the pagination control (next page / page dropdown / typed page number + Enter — use the control available in the build).
3. Wait until the grid finishes loading page 2 rows.
4. Read Amount board **TOTAL Initial Amount** as `UI_T1_init` and **TOTAL Current Amount** as `UI_T1_curr`.
5. Optionally correlate: call `POST /customer/{customerId}/customer-liability-and-receivable` with `{ "page": 0, "size": 25 }` and confirm API `totalInitialAmount` / `totalCurrentAmount` equal `UI_T0_init` / `UI_T0_curr`.

**Expected test case results:**
1. `UI_T0_init` == `UI_T1_init` and `UI_T0_curr` == `UI_T1_curr` (board does not change when only the page changes).
2. Grid on page 2 shows a different set of row IDs than page 1 (pagination actually advanced).
3. Board values match API `totalInitialAmount` / `totalCurrentAmount` for the same unfiltered request (if step 5 is executed).
4. No toast/error indicating a failed list load.

---

### TC-FE-2 (Positive): Amount board totals change when checkbox Blocked for offsetting is selected

**Description:** Verify Amount board totals update when the user selects the **Blocked for offsetting** filter checkbox, and that after filtering, changing the page size/page leaves the filtered totals unchanged.

**Preconditions:**
1. Open `{BASE_URL}/login`, log in with a user that has **View Customer Liabilities and receivables**, confirm the portal shell loads.
2. Resolve open `accountingPeriodId` via accounting-period API, default `currencyId` from nomenclatures, and `offsetBlockReasonId` from the liabilities-offsetting block-reason nomenclature.
3. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId` and identifier.
4. Create **8** liabilities via `POST /customer-liability` with `initialAmount`: `25.00`, required dates/currency/accounting period, all block flags unset/false (unfiltered contribution initial **200.00**).
5. Create **4** liabilities via `POST /customer-liability` with `initialAmount`: `50.00`, `blockedForLiabilitiesOffsetting`: `true`, `blockedForLiabilitiesOffsettingFromDate`: today, `blockedForLiabilitiesOffsettingToDate`: today+365, `blockedForLiabilitiesOffsettingReasonId`: `offsetBlockReasonId`, `blockedForLiabilitiesOffsettingAdditionalInfo`: `PDT-3453 UI filter` (filtered contribution initial **200.00**).
6. Open **Customers** → search by identifier → customer detail → **Liabilities and receivables** tab with **no** filter checkboxes selected and empty search. Record baseline Amount board `UI_B_init` / `UI_B_curr`. Expect baseline initial total **400.00** when all 12 rows are in the default list.

**Test steps:**
1. Select the filter checkbox labeled **Blocked for offsetting** (UI control bound to listing request field `blockedForOffsetting`).
2. Wait for the grid to refresh.
3. Record Amount board totals as `UI_F_init` / `UI_F_curr` and count visible filtered rows (or total count label).
4. Change page size to **2** (or navigate to page 2 if the control only changes page) while **Blocked for offsetting** remains selected.
5. Record Amount board totals as `UI_F2_init` / `UI_F2_curr`.
6. Clear the **Blocked for offsetting** checkbox.
7. Wait for the grid to refresh and record Amount board totals as `UI_C_init` / `UI_C_curr`.

**Expected test case results:**
1. After step 3, `UI_F_init` = **200.00** and `UI_F_curr` equals the sum of current amounts of the four blocked liabilities (typically **200.00** if unpaid).
2. `UI_F_init` ≠ `UI_B_init` and `UI_F_curr` ≠ `UI_B_curr`.
3. `UI_F2_init` == `UI_F_init` and `UI_F2_curr` == `UI_F_curr` (pagination/page-size change does not alter filtered board totals).
4. After step 7, `UI_C_init` == `UI_B_init` and `UI_C_curr` == `UI_B_curr` (clearing the checkbox restores unfiltered board totals).
5. While filtered, grid rows are only the four blocked liabilities from precondition step 5.

---

### TC-FE-3 (Negative): Amount board must not display the sum of only the visible page rows

**Description:** Guard against the PDT-3453 defect: with multi-page data and no filters, the Amount board must **not** equal the arithmetic sum of only the rows currently visible on the screen when later pages hold additional amounts.

**Preconditions:**
1. Log into `{BASE_URL}/login` with **View Customer Liabilities and receivables** permission.
2. Resolve `accountingPeriodId` and `currencyId`.
3. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId` and identifier.
4. Create **30** liabilities via `POST /customer-liability`, each with `initialAmount`: `10.00`, `occurrenceDate` today, `dueDate` today+30, `accountingPeriodId`, `currencyId`, block flags unset/false (ensures ≥2 pages at size 25).
5. Open customer detail → **Liabilities and receivables** tab on page 1 (default 25 rows). Ensure no filter checkboxes are selected and search is empty.

**Test steps:**
1. On page 1, sum the **Current amount** column values of all **visible** grid rows; record as `PageSum_curr`. Sum visible **Initial amount** column values as `PageSum_init`.
2. Read Amount board **TOTAL Current Amount** as `Board_curr` and **TOTAL Initial Amount** as `Board_init`.
3. Navigate to the last page and confirm additional rows exist (page-local sums on page 1 cannot represent the full dataset).
4. Call `POST /customer/{customerId}/customer-liability-and-receivable` with `{ "page": 0, "size": 25 }` and read API `totalCurrentAmount` / `totalInitialAmount`.

**Expected test case results:**
1. `Board_curr` ≠ `PageSum_curr` and `Board_init` ≠ `PageSum_init` (board is not the visible-page-only sum).
2. `Board_curr` equals API `totalCurrentAmount` and `Board_init` equals API `totalInitialAmount` (display uses aggregate fields).
3. After navigating to another page, board values remain equal to step 2 (regression of PDT-3453 actual result is a **fail**).

---

### TC-FE-4 (Negative): User without View Customer Liabilities and receivables permission cannot see the Amount board

**Description:** Verify permission gating for the Liabilities and receivables tab. Cross-dep note: Axis covers access control for the Amount board consumer; if the tab is exposed without permission, totals from `POST /customer/{customerId}/customer-liability-and-receivable` could leak.

**Preconditions:**
1. Using an admin/setup user with user-management rights: open portal **Admin** (or **Users / Roles**) and create or select a portal user `user_no_lr_view` whose role does **not** include permission **View Customer Liabilities and receivables**. Store username/password. (Alternatively use the environment-provisioned account documented as lacking that permission, if one exists in the Dev secrets matrix.)
2. With an authorized API token, create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store identifier and `customerId`.
3. Create **1** liability via `POST /customer-liability` for that customer (`initialAmount`: `12.00`, `accountingPeriodId`, `currencyId`, `occurrenceDate` today, `dueDate` today+30).
4. Log out of the admin session if needed. Open `{BASE_URL}/login` and log in as `user_no_lr_view`.
5. Navigate **Customers** → search by identifier from step 2 → open customer detail.

**Test steps:**
1. Inspect the customer detail tab strip for a tab labeled **Liabilities and receivables**.
2. Confirm the Amount board (**TOTAL Initial Amount** / **TOTAL Current Amount**) is not visible on the customer detail page.
3. If a network panel is available, confirm no successful **200** response from `POST /customer/{customerId}/customer-liability-and-receivable` that returns `totalCurrentAmount` **12.00** for this session.

**Expected test case results:**
1. The **Liabilities and receivables** tab is **hidden** (not present in the tab strip).
2. Amount board totals for the liability from precondition step 3 are **not** displayed anywhere on the customer detail for this user.
3. If the listing API is called anyway, it returns HTTP **403 Forbidden** with an access/permission message fragment (e.g. contains `permission` or `forbidden`); a **200** with `totalCurrentAmount` **12.00** is a failure.

---

## References

- **Jira:** [PDT-3453](https://oppa-support.atlassian.net/browse/PDT-3453)
- **Confluence:** [Customer Receivable/Liability](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/209977345/Customer+Receivable+Liability) (page ID `209977345`)
- **Swagger (dev):** `POST /customer/{customerId}/customer-liability-and-receivable`
- **Cross-dependency:** `Cursor-Project/cross_dependencies/2026-09-17_PDT-3453_customer_receivable_liability_totals_pagination.json`
- **Confluence source:** REST fallback (MCP asterbit Confluence not granted)

### Finding: Amount board multi-page totals missing from Confluence
- **Type:** Doc gap
- **User impact:** High
- **Spec / doc says:** Amount board sums items “shown further down” after filter/search (ambiguous for pagination) — page `209977345`.
- **Code / runtime does / ticket expects:** Totals = all pages; stable on page change; change on checkbox/filter only ([PDT-3453](https://oppa-support.atlassian.net/browse/PDT-3453)).
- **Gap:** Wiki missing explicit all-pages rule.
- **Recommendation:** Update Confluence; TCs follow ticket/customer-feedback expected behavior.
