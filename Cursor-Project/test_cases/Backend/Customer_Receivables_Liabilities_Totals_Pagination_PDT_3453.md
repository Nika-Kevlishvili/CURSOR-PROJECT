# Customer Liabilities and receivables Amount board totals – pagination (PDT-3453)

**Jira:** [PDT-3453](https://oppa-support.atlassian.net/browse/PDT-3453) (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** Amount board **TOTAL Initial Amount** and **TOTAL Current Amount** on Customer → Liabilities and receivables must equal the sum across **all pages** of the filtered dataset. Totals must stay unchanged when only the page changes, and must change when filter/search checkboxes change.

**Scope:** Backend listing API `POST /customer/{customerId}/customer-liability-and-receivable` response fields `totalInitialAmount` and `totalCurrentAmount` (Swagger: `CustomerLiabilityAndReceivableListingResponse`). Target environment: **dev**.

---

## Test data (preconditions)

Shared reference chain for this file. Every TC below repeats the full creation steps required for that scenario (Rule TC-STANDALONE-PRE.0).

- **Environment:** Dev Phoenix API (`BASE_URL` for dev).
- **Auth:** Bearer token for a user with permission **View Customer Liabilities and receivables**.

---

## Backend Test Cases

### TC-BE-1 (Positive): Listing totals stay equal across page 0 and page 1 for the same unfiltered request

**Description:** With more than one page of uncovered liabilities (default page size 25), verify `totalInitialAmount` and `totalCurrentAmount` on page 0 equal the same fields on page 1, and that each equals the arithmetic sum of the corresponding amounts over **all** pages (not only `content[]` on the current page).

**Preconditions:**
1. Resolve an open accounting period id via the environment nomenclature / accounting-period API used by the project (store as `accountingPeriodId`).
2. Resolve the default system currency id from environment nomenclatures (store as `currencyId`).
3. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, unique `customerIdentifier`). Store returned customer id as `customerId`.
4. Create **30** customer liabilities via `POST /customer-liability`, each with: `customerId` from step 3, `accountingPeriodId` from step 1, `currencyId` from step 2, `occurrenceDate`: today (ISO date), `dueDate`: today + 30 days, `initialAmount`: `10.00` (exclusive minimum > 0), and leave block flags unset/false so rows remain visible in the default uncovered listing. Store all created liability ids.
5. Confirm listing has more than one page: call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 0, "size": 25 }` and assert HTTP 200 and `totalElements` ≥ 30 and `totalPages` ≥ 2.

**Test steps:**
1. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 0, "size": 25 }` (no filter flags, no `prompt`).
2. Record HTTP status, `totalInitialAmount` as `T0_init`, `totalCurrentAmount` as `T0_curr`, and `content[]`.
3. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 1, "size": 25 }` (identical filters).
4. Record `totalInitialAmount` as `T1_init` and `totalCurrentAmount` as `T1_curr`.
5. Call the same endpoint repeatedly with `page` = 0 .. `totalPages-1`, `size` = 25, and compute `Sum_init` = sum of all `content[].initialAmount` across pages and `Sum_curr` = sum of all `content[].currentAmount` across pages.

**Expected test case results:**
1. Steps 1 and 3 return **HTTP 200**.
2. `T0_init` == `T1_init` and `T0_curr` == `T1_curr` (totals do not change when only `page` changes).
3. `T0_init` == `Sum_init` and `T0_curr` == `Sum_curr` (totals equal the full multi-page dataset).
4. For page 0, `sum(content[].currentAmount)` ≠ `T0_curr` when `totalElements` > `size` (page-local sum must not be used as the board total).

---

### TC-BE-2 (Positive): Listing totals change when `blockedForOffsetting` filter is applied

**Description:** Verify that setting listing filter `blockedForOffsetting: true` (not changing `page`) updates `totalInitialAmount` / `totalCurrentAmount` to the blocked-for-offsetting subset only, and that those filtered totals stay stable across pages.

**Preconditions:**
1. Resolve an open accounting period id via `GET` accounting-period list/filter used by the project (store as `accountingPeriodId`).
2. Resolve default system currency id from environment nomenclatures (store as `currencyId`).
3. Resolve a valid liabilities-offsetting block reason id from the project nomenclature for blocked-for-offsetting reasons (store as `offsetBlockReasonId`).
4. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, unique identifier). Store `customerId`.
5. Create **10** liabilities via `POST /customer-liability`, each with: `customerId`, `accountingPeriodId`, `currencyId`, `occurrenceDate` today, `dueDate` today+30, `initialAmount`: `20.00`, and all block flags unset/false. Expected contribution to unfiltered totals: initial **200.00**.
6. Create **5** liabilities via `POST /customer-liability`, each with: same required fields as step 5, `initialAmount`: `30.00`, `blockedForLiabilitiesOffsetting`: `true`, `blockedForLiabilitiesOffsettingFromDate`: today, `blockedForLiabilitiesOffsettingToDate`: today+365, `blockedForLiabilitiesOffsettingReasonId`: `offsetBlockReasonId`, `blockedForLiabilitiesOffsettingAdditionalInfo`: `PDT-3453 filter fixture`. Expected filtered-only initial sum: **150.00**.
7. Baseline unfiltered: `POST /customer/{customerId}/customer-liability-and-receivable` with `{ "page": 0, "size": 25 }` → store `B_init`, `B_curr`, `B_count` = `totalElements`. Expect `B_count` = **15** and `B_init` = **350.00** (200 + 150) when all 15 rows are returned in the default uncovered listing.

**Test steps:**
1. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 0, "size": 25, "blockedForOffsetting": true }`.
2. Record `F_init` = `totalInitialAmount`, `F_curr` = `totalCurrentAmount`, `F_count` = `totalElements`.
3. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 0, "size": 2, "blockedForOffsetting": true }`. Record `F0_init` / `F0_curr`.
4. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{ "page": 1, "size": 2, "blockedForOffsetting": true }`. Record `F1_init` / `F1_curr`.
5. Call the filtered endpoint with `size`: 2 for every page until all `F_count` elements are read; compute `SumF_init` / `SumF_curr` from all `content[]` rows.

**Expected test case results:**
1. Step 1 returns **HTTP 200**.
2. `F_count` = **5**, `F_init` = **150.00**, and `F_curr` equals the sum of current amounts of those five blocked liabilities (for unpaid manual liabilities typically **150.00**).
3. `F_init` ≠ `B_init` and `F_curr` ≠ `B_curr` (filter changes aggregates vs unfiltered baseline **350.00** / matching current).
4. `F0_init` == `F1_init` and `F0_curr` == `F1_curr` (pagination under the same filter does not change totals).
5. `F0_init` == `SumF_init` and `F0_curr` == `SumF_curr`.
6. `F0_init` == `F_init` and `F0_curr` == `F_curr` (totals identical for `size` 25 and `size` 2 with the same filter).

---

### TC-BE-3 (Negative): Listing rejected when required `page` and `size` are omitted

**Description:** Verify the listing contract rejects a body missing required pagination fields so clients cannot obtain ambiguous totals without an explicit page request.

**Preconditions:**
1. Resolve open `accountingPeriodId` via accounting-period API and default `currencyId` from nomenclatures.
2. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId`.
3. Create **1** liability via `POST /customer-liability` with `customerId`, `accountingPeriodId`, `currencyId`, `occurrenceDate` today, `dueDate` today+30, `initialAmount`: `15.00`.

**Test steps:**
1. Call `POST /customer/{customerId}/customer-liability-and-receivable` with body `{}` (omit required `page` and `size`).
2. Record HTTP status and response error payload (field names / messages).

**Expected test case results:**
1. HTTP **400 Bad Request**.
2. Response validation errors reference required properties `page` and/or `size` (field name or message fragment contains `page` or `size`).
3. Response body is not a successful `CustomerLiabilityAndReceivableListingResponse` with populated `totalCurrentAmount`.

---

### TC-BE-4 (Negative): Listing for a non-existent customerId returns 404 and does not leak another customer’s totals

**Description:** Verify that requesting liabilities/receivables for an unknown `customerId` returns not-found and does not return totals belonging to a different customer. Cross-dep risk: UI Amount board binding must not display another customer’s aggregates if the API is called with a wrong id.

**Preconditions:**
1. Resolve open `accountingPeriodId` via accounting-period API and default `currencyId` from nomenclatures.
2. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store real `customerId`.
3. Create **1** liability via `POST /customer-liability` with: `customerId` from step 2, `accountingPeriodId`, `currencyId`, `occurrenceDate` today, `dueDate` today+30, `initialAmount`: `40.00`.
4. Set `missingCustomerId` = `999999999`. Call `GET /customer/{missingCustomerId}` and confirm HTTP **404** (customer does not exist).

**Test steps:**
1. Call `POST /customer/{missingCustomerId}/customer-liability-and-receivable` with `{ "page": 0, "size": 25 }`.
2. Record HTTP status and error body.
3. Call `POST /customer/{customerId}/customer-liability-and-receivable` with `{ "page": 0, "size": 25 }` and record `Real_curr` = `totalCurrentAmount`.

**Expected test case results:**
1. Step 1 returns HTTP **404**.
2. Error message/body fragment indicates customer not found (text contains `customer` and `not found`, or the project’s standard customer-not-found error code).
3. Step 1 body must not include `totalCurrentAmount` equal to `Real_curr`.
4. Step 3 returns HTTP **200** with `totalCurrentAmount` reflecting the **40.00** liability from precondition step 3 (for unpaid liability, typically `40.00`).

---

## References

- **Jira:** [PDT-3453](https://oppa-support.atlassian.net/browse/PDT-3453)
- **Confluence:** [Customer Receivable/Liability](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/209977345/Customer+Receivable+Liability) (page ID `209977345`) — Amount board section
- **Swagger (dev):** `Cursor-Project/config/swagger/dev/swagger-spec.json` — `POST /customer/{customerId}/customer-liability-and-receivable` → `CustomerLiabilityAndReceivableListingResponse.totalInitialAmount` / `totalCurrentAmount`
- **Cross-dependency:** `Cursor-Project/cross_dependencies/2026-09-17_PDT-3453_customer_receivable_liability_totals_pagination.json`
- **Confluence source:** REST fallback (MCP Confluence for asterbit not granted; `get-confluence-page-rest.ps1`)

### Finding: Amount board multi-page totals missing from Confluence
- **Type:** Doc gap | Code↔Doc mismatch risk
- **User impact:** High (balances appear to change while paging; trust in customer balance)
- **Spec / doc says:** Confluence Amount board — totals are dynamic from filtering/search and equal the sum of items “shown further down” (ambiguous whether that means current page or full filtered set). Page ID `209977345`.
- **Code / runtime does:** Swagger exposes aggregate `totalInitialAmount` / `totalCurrentAmount` beside paginated `content[]`. PDT-3453 expected behavior (ticket + customer feedback): aggregates = all pages; stable on page change; change on checkbox/filter only. Phoenix Java/UI sources unavailable in this cloud checkout (empty submodules).
- **Gap:** Wiki does not state the all-pages aggregation rule that PDT-3453 requires.
- **Recommendation:** Update Confluence Amount board section; keep TCs aligned to ticket/customer-feedback expected behavior until the wiki is updated.
