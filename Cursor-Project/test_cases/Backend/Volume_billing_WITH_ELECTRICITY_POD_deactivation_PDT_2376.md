# Volume billing — WITH_ELECTRICITY vs POD deactivation month boundaries (PDT-2376)

**Jira:** PDT-2376  
**Type:** Task / Regression  
**Summary:** Single contract carries multiple PODs; FOR_VOLUMES plus WITH_ELECTRICITY_INVOICE billing must only emit electricity tax lines for PODs whose deactivation calendar month is not strictly before the invoice period end calendar month (`YearMonth`-style compare).

**Scope:** API verification of consolidated billing for one product contract linked to eight electricity PODs. Preconditions match `POST /billing-run` STANDARD with `FOR_VOLUMES` and `WITH_ELECTRICITY_INVOICE`, contract-level billing, and invoice detailed rows from `GET /invoice/detailed-data`. Automation: `tests/cursor/PDT-2376-volume-with-electricity-pod-deactivation.spec.ts`.

---

## Test data (preconditions)

1. Create a legal customer via `POST /customer` using the EnergoTS default legal payload generator.
2. Create a settlement volume price component via `POST /price-component` (`priceSettlement`), profile from nomenclatures (e.g. `Gio` profile pattern as in `withElectricity(Product).spec.ts`).
3. Create an electricity / WITH_ELECTRICITY application price component via `POST /price-component` (`GeneratePayload.productAndServices.electricity()`).
4. Create terms via `POST /terms`.
5. Create **eight** electricity settlement PODs via `POST /pod` (`pod_settlement`).
6. Create a catalogue product via `POST /product` attaching **both** price component ids from steps 2–3 and the term from step 4.
7. Create **one** product contract via `POST /product-contract` including **all eight** POD detail ids (default `product_contract()` when `Responses.pod` holds eight entries).
8. For each POD, activate on the contract via `POST /contract-pods/manual` with pairwise dates from the POD matrix (`activationDate` / `deactivationDate`; `null` where specified; use environment `deactivationPurposeId`).
9. For each POD index `0…7`, post monthly billing-profile energy via `POST /billing-by-profile` (`profile1Month` with `{ startDate, endDate }` equal to `[first day of invoice month, invoicePeriodTo]` adapted to OPEN accounting period if needed — see Playwright attachments).
10. Run `POST /billing-run` (`forVolumes`-shaped STANDARD payload) targeting the contract (`billingApplicationLevel` CONTRACT) with models `FOR_VOLUMES` and `WITH_ELECTRICITY_INVOICE`, `taxEventDate` / `invoiceDate` aligned to **`invoicePeriodTo`**, and `accountingPeriodId` resolving to an OPEN period that covers that anchor date.

---

## Backend Test Cases

### TC-BE-1 (Positive): Consolidated multi-POD billing — eligibility matrix Happy path / mixed PODs

**Description:** One billing run spans eight PODs. After billing completes (`waitForInvoiceGeneration` pattern / billing run reaches COMPLETED), assert per POD identifier that a WITH_ELECTRICITY-labelled invoice detailed row (`GET /invoice/detailed-data?page&size`) exists iff `YearMonth(deactivation) >= YearMonth(invoicePeriodTo)` or deactivation is `null`.

**Preconditions:** Full chain §1–9 above plus accounting period resolver output recorded in attachments.

**Test steps:**

1. Execute §10 billing run pipeline (start billing → drafts → generating → accounting) using `PATCH` billing-run stages as enforced by EnergoTS helpers.
2. Collect invoice id(s): `billing-run/draft-invoices` (or Responses invoice bucket after helpers).
3. For each invoice id, page through `GET /invoice/detailed-data?id={invoice}&page=N&size=100`; collect rows where `priceComponent` text indicates electricity / WITH_ELECTRICITY.
4. For each POD, compare `{ identifier }` vs expected presence of such a row (`expectedEligible` column from matrix).

**Expected test case results:** Matrix matches billing output (`actualFoundEligible === expectedEligible`). Attach machine-readable POD table (`identifier`, `activationDate`, `deactivationDate`, `invoicePeriodToUsed`, `expectedEligible`, `actualFoundEligible`).

---

### TC-BE-2 (Negative): POD deactivated in a strictly earlier calendar month must not emit WITH_ELECTRICITY rows

**Description:** PODs intentionally terminated with `YearMonth(deactivation)` **strictly earlier** than `YearMonth(invoicePeriodTo)` must have **zero** qualifying WITH_ELECTRICITY detailed rows tied to their identifiers.

**Preconditions:** Use the consolidated dataset from §1–9 ensuring at least one POD row is configured **only** with prior-calendar-month deactivation (see matrix labels `P5`–`P7` patterns in automation).

**Test steps:**

1. Re-run steps 2–4 from TC-BE-1 focusing identifiers mapped to PODs flagged `expectedEligible = false`.

**Expected test case results:** For each ineligible POD identifier, filtered detailed rows (`priceComponent` WITH_ELECTRICITY pattern) yield **false** membership; assertions fail loudly if backend erroneously attaches electricity tax rows.

---

## References

- Swagger: `/billing-run` (POST Standard), `/invoice/detailed-data` (GET paging), `/contract-pods/manual`.
- Repo: `Cursor-Project/EnergoTS/tests/cursor/pdt-2376-volume-with-electricity.fixtures.ts`.
