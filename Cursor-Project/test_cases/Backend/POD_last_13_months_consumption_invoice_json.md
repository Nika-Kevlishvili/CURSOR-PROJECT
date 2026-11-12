# POD last 13 months consumption on the invoice JSON

**Jira:** none (Confluence story only)  
**Type:** Story  
**Summary:** Billing must put each POD's consumption for the last 13 complete months on the invoice JSON as `podLast13MonthsConsumption`. A nightly job fills a POD period table from invoiced scales. A second nightly job stores the customer/POD total. The billing run copies that stored total. It does not calculate it while generating the invoice.

**Scope:** Environment **dev2**. Backend only. These cases follow the story [In the invoices to show consumption history for 13 month](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1224802306/In+the+invoices+to+show+consumption+history+for+13+month) (page ID 1224802306, version 12, updated 2026-10-28) and were compared with the diagram `last 13 month consumption .drawio.svg`. On dev2 today the document model has `ProvidedPower` and does not have `podLast13MonthsConsumption`, and phoenix-scheduler has no job for this table. Executing these cases before that work is deployed will fail. The expected results below are the story, not today's runtime. Where the diagram and the story disagree, the case is not changed. Those points are open questions.

**Coverage:** TC-BE-1 through TC-BE-46. Happy path (the 13-month figure the invoice copies): TC-BE-10, TC-BE-15, TC-BE-16, TC-BE-17. Other positive cases check a rule that keeps or replaces a quantity. Negative cases check a rule that leaves a quantity out. Combinations the story does not define are listed under References and are not given a pass result.

**Deployment period start used in this file:** `2026-06-01`. Historical rows are months through May 2026. The new-table job collects invoiced scales from 1 June 2026 onward. The story does not fix the deployment date; these cases use 1 June 2026 so May is the last historical month and June is the first counted month. No new-table period and no variable-job invoice date is later than 31 October 2026. The usual invoice date is 17 October 2026, so the window is 1 September 2025 through 30 September 2026. October 2026 is the invoice month and is excluded. TC-BE-10 stays on 17 July 2026 and TC-BE-15 stays on 1 August 2026. A missing deactivation date is still compared with `2090-01-01` (TC-BE-13); that sentinel is not a row in the new table. The story's own invoice example is quoted only in References.

**Jobs (no HTTP endpoint on dev2):**

- **Scale rebuild job:** midnight job that rebuilds the new POD consumption table from active, already invoiced scales, starting at the deployment period. It must not change historical rows.
- **Variable job:** nightly job that writes each customer/POD 13-month total into the prepared customer table. Billing reads that table.

**Invoiced flag:** `POST /billing-by-scales` stores `invoiced` as NULL. `GET /billing-by-scales/list` shows that as `NO`. The new-table job reads scales that are active and already invoiced (`YES`). Story version 12 says the historical cutoff uses that same AC-4 rule. The diagram's historical cutoff box says active, and does not repeat "already invoiced". Cases still require `YES` until that difference is confirmed.

---

## Test data (preconditions)

Reference only. Every test case repeats its own setup.

- **Environment:** dev2
- **New POD row:** one row per POD and header period. `type` / kind is scale, stored as `1`. `period_from` and `period_to` are the scale header dates. `total_volume` is the sum of kept lines. `hours` lists each clock hour inside each kept line's own dates, once. The quantity is not split across hours.
- **Line kept:** the line's scale nomenclature has `scaleForActiveElectricity` true. Scale type, scale name, and tariff name are not filters. If `volumes` is set, use `volumes`. If `volumes` is empty, use `totalVolumes`. Do not use difference, correction kWh, deducted kWh, meter readings, or price.
- **Historical row:** inserted manually. Columns are POD identifier, customer number, quantity kWh, and period (month and year). There is no create API for this table.
- **Invoice JSON:** field name `podLast13MonthsConsumption`, numeric kWh, on the POD block of the billing document model (the same block as `ProvidedPower` / `PODID`). `GET /invoice/detailed-data` is the billed period, not this field.

---

## Backend Test Cases

### TC-BE-1 (Positive): Invoiced active-electricity scale becomes one POD row and keeps the quantity whole

**Description:** One invoiced scale for one day after the deployment date, with `volumes` set, produces one scale row. Hours are stored once per clock hour. The quantity stays on the row and is not divided by 24.

**Preconditions:**
1. On dev2, use a token that can create grid operators, customers, PODs, meters, scales, products, contracts, and billing-by-scales. Deployment period start for this case is `2026-06-01`.
2. Create a grid operator via `POST /grid-operators` with `name` `GO13M01`, `fullName` `GO13M01`, `status` `ACTIVE`, `defaultSelection` false, `ownedByEnergoPro` false. Expect HTTP 201. Keep `id` as gridOperatorId.
3. Create a customer via `POST /customer` with `customerType` `PRIVATE_CUSTOMER` and status `ACTIVE`. Expect HTTP 201. Keep `id` as customerId. Call `GET /customer/{customerId}` and keep `customerNumber`.
4. Create a POD via `POST /pod` with `identifier` `32Z13M000000001`, `gridOperatorId` from step 2, `type` `CONSUMER`, `consumptionPurpose` `HOUSEHOLD`, `voltageLevel` `LOW`, status `ACTIVE`. Expect HTTP 201. Keep `id` as podId and the current `podDetailId`.
5. Create a meter via `POST /meters` with `number` `M13M01`, `gridOperatorId` from step 2, `podId` from step 4, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create a scale nomenclature via `POST /scales` with `name` `SC13M01A`, `gridOperatorId` from step 2, `scaleType` `ACTIVE`, `scaleCode` `A13M01`, `status` `ACTIVE`, `defaultSelection` false, `scaleForActiveElectricity` true. Expect HTTP 201. Keep `id` as activeScaleId.
7. Create terms via `POST /terms` with status `ACTIVE`. Expect HTTP 201. Keep termId.
8. Create a price component via `POST /price-components` with status `ACTIVE`, application type `BY_SCALES`, and a positive unit price. Expect HTTP 201. Keep priceComponentId.
9. Create a product via `POST /products` with status `ACTIVE`, `contractTypes` `[SUPPLY_ONLY]`, `termId` from step 7, `priceComponentIds` `[priceComponentId]`, `availableForSale` true. Expect HTTP 201. Keep productId.
10. Create a product contract via `POST /product-contract` for customerId, productId, and this POD, `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Keep contractDetailId.
11. Activate the POD via `POST /contract-pods/manual` with `podDetailId` from step 4, `contractDetailId` from step 10, and `activationDate` `2026-06-01`. Do not send `deactivationDate`. Expect HTTP 200.
12. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000001`, `dateFrom` `2026-06-01`, `dateTo` `2026-06-01`, `billingPowerInKw` 1, `invoiceNumber` `INV13M01`, `invoiceDate` `2026-06-01T00:00:00`, `correction` false, `override` false, `saveRecordForIntermediatePeriod` false, `saveRecordForMeterReadings` false, and one `billingByScalesTableCreateRequests` row: `periodFrom` `2026-06-01`, `periodTo` `2026-06-01`, `meterNumber` `M13M01`, `scaleNumber` `1`, `scaleCode` `A13M01`, `scaleType` `IGNORED-TYPE`, `tariffScale` `IGNORED-TARIFF`, `oldMeterReading` 0, `newMeterReading` 999, `difference` 999, `multiplier` 1, `correction` 50, `deducted` 20, `volumes` 120, `totalVolumes` 1, `unitPrice` 9, `totalValue` 999, `index` 1. Expect HTTP 201. Keep the returned id as scaleId.
13. Call `GET /accounting-period/available-accounting-periods`. Expect HTTP 200. Keep an `accountingPeriodId` that covers `2026-06-01`.
14. Create a standard billing run via `POST /billing-run` with `billingType` `STANDARD_BILLING`, `commonParameters.periodicity` `STANDARD`, `commonParameters.accountingPeriodId` from step 13, `commonParameters.invoiceDate` `2026-06-30`, `commonParameters.taxEventDate` `2026-06-30`, `basicParameters.applicationModelType` `[FOR_VOLUMES]`, `basicParameters.maxEndDate` `2026-06-30`, `basicParameters.billingCriteria` `ALL`, `basicParameters.billingApplicationLevel` `POD`. Expect HTTP 200. Keep the billing-run id.
15. Call `PATCH /billing-run/start-billing?billingRunId={id}` (HTTP 202), wait until `GET /billing-run/{id}` shows `commonParameters.status` `DRAFT`, call `PATCH /billing-run/start-generating?billingRunId={id}` (HTTP 202), wait until status is `GENERATED`, then call `PATCH /billing-run/start-accounting?billingRunId={id}` (HTTP 202) and wait until accounting has finished for this run.
16. Call `GET /billing-by-scales/list` with `page` 0, `size` 20, `prompt` `32Z13M000000001`, `searchBy` `POD_IDENTIFIER`, `exactMatch` true, `dateFrom` `2026-06-01`, `dateTo` `2026-06-01`. Expect HTTP 206.

**Test steps:**
1. Open the scale list for this POD. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once. It rebuilds the new POD table from 1 Jun 2026.
3. Open the new table and find the row for this POD and 1 Jun 2026.
**Expected test case results:**
1. Step 16, before the job, shows `invoiced` `YES` for scaleId. If it is still `NO`, stop: the scale is not an input to this job.
2. Exactly one new row exists for this POD and these header dates. Kind / `type` is `1` (scale). `period_from` is `2026-06-01` and `period_to` is `2026-06-01` (the header dates, not a tariff-line date). `total_volume` is `120` (from `volumes`, not from `totalVolumes` 1, `difference` 999, `correction` 50, `deducted` 20, the meter readings, or `unitPrice`).
3. `hours` contains 24 timestamps, `2026-06-01 00:00` through `2026-06-01 23:00`, each once. `total_volume` is still `120`, not `5` (120/24).
4. The scale type `IGNORED-TYPE` and tariff `IGNORED-TARIFF` did not drop the line.

### TC-BE-2 (Positive): Empty volumes falls back to totalVolumes

**Description:** When the kept line has no `volumes` number, the row uses `totalVolumes`. Difference, correction, deducted, readings, and price stay unused.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`. Use a token that can create the same chain as a supply contract with an activated POD.
2. Create a grid operator via `POST /grid-operators` with `name` `GO13M02`, `fullName` `GO13M02`, `status` `ACTIVE`, `defaultSelection` false. Expect HTTP 201. Keep gridOperatorId.
3. Create a customer via `POST /customer` with `customerType` `PRIVATE_CUSTOMER` and status `ACTIVE`. Expect HTTP 201. Keep customerId. Call `GET /customer/{customerId}` and keep `customerNumber`.
4. Create a POD via `POST /pod` with `identifier` `32Z13M000000002`, `gridOperatorId` from step 2, `type` `CONSUMER`, `consumptionPurpose` `HOUSEHOLD`, `voltageLevel` `LOW`, status `ACTIVE`. Expect HTTP 201. Keep podId and podDetailId.
5. Create a meter via `POST /meters` with `number` `M13M02`, `gridOperatorId` from step 2, `podId` from step 4, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create a scale via `POST /scales` with `name` `SC13M02A`, `gridOperatorId` from step 2, `scaleType` `ACTIVE`, `scaleCode` `A13M02`, `status` `ACTIVE`, `defaultSelection` false, `scaleForActiveElectricity` true. Expect HTTP 201.
7. Create terms via `POST /terms` status `ACTIVE` (HTTP 201), a price component via `POST /price-components` status `ACTIVE` and application type `BY_SCALES` (HTTP 201), and a product via `POST /products` status `ACTIVE`, `contractTypes` `[SUPPLY_ONLY]`, that term, and that price component (HTTP 201).
8. Create a product contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`, customerId, productId, and this POD. Expect HTTP 201. Keep contractDetailId.
9. Activate the POD via `POST /contract-pods/manual` with podDetailId, contractDetailId, and `activationDate` `2026-06-01`. No deactivation date. Expect HTTP 200.
10. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000002`, `dateFrom` `2026-06-02`, `dateTo` `2026-06-02`, `billingPowerInKw` 1, `invoiceNumber` `INV13M02`, `invoiceDate` `2026-06-02T00:00:00`, `correction` false, `override` false, `saveRecordForIntermediatePeriod` false, `saveRecordForMeterReadings` false, and one table row: `periodFrom` `2026-06-02`, `periodTo` `2026-06-02`, `meterNumber` `M13M02`, `scaleCode` `A13M02`, `scaleNumber` `1`, `volumes` omitted (null), `totalVolumes` 80, `difference` 400, `correction` 15, `deducted` 7, `oldMeterReading` 10, `newMeterReading` 410, `unitPrice` 3, `index` 1. Expect HTTP 201.
11. Bill June 2026 for this POD with `POST /billing-run` (`billingType` `STANDARD_BILLING`, `invoiceDate` `2026-06-30`, `maxEndDate` `2026-06-30`, `billingCriteria` `ALL`, `billingApplicationLevel` `POD`, accounting period covering June 2026). Run `PATCH /billing-run/start-billing`, `start-generating`, and `start-accounting` (each HTTP 202) until the run has finished accounting.
12. Call `GET /billing-by-scales/list` with `page` 0, `size` 20, `prompt` `32Z13M000000002`, `exactMatch` true. Expect HTTP 206 and `invoiced` `YES`. If it is `NO`, stop.

**Test steps:**
1. Open the scale list for this POD. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 2 Jun 2026.
**Expected test case results:**
1. One row, `type` `1`, `total_volume` `80`.
2. `total_volume` is not `400`, `15`, `7`, `10`, `410`, or `3`.

### TC-BE-3 (Positive): A line whose scale is not active electricity is left out

**Description:** Two lines share one header. Only the line tied to `scaleForActiveElectricity` true is added. The other line's quantity is ignored. Scale name and tariff are not reasons to drop the kept line.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create a grid operator via `POST /grid-operators` with `name` `GO13M03`, `fullName` `GO13M03`, `status` `ACTIVE`, `defaultSelection` false. Expect HTTP 201. Keep gridOperatorId.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and, from `GET /customer/{id}`, `customerNumber`.
4. Create a POD via `POST /pod` with `identifier` `32Z13M000000003`, that grid operator, `type` `CONSUMER`, `consumptionPurpose` `HOUSEHOLD`, `voltageLevel` `LOW`, status `ACTIVE`. Expect HTTP 201. Keep podId and podDetailId.
5. Create a meter via `POST /meters` with `number` `M13M03`, that grid operator and podId, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale A via `POST /scales` with `name` `SC13M03A`, `scaleCode` `A13M03`, `scaleType` `DAY`, `scaleForActiveElectricity` true, `status` `ACTIVE`, `defaultSelection` false, `gridOperatorId` from step 2. Expect HTTP 201.
7. Create scale B via `POST /scales` with `name` `SC13M03B`, `scaleCode` `B13M03`, `scaleType` `NIGHT`, `scaleForActiveElectricity` false, `status` `ACTIVE`, `defaultSelection` false, same grid operator. Expect HTTP 201.
8. Create terms (`POST /terms`, `ACTIVE`, HTTP 201), a `BY_SCALES` price component (`POST /price-components`, HTTP 201), and a product (`POST /products`, `ACTIVE`, `contractTypes` `[SUPPLY_ONLY]`, HTTP 201).
9. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`, this customer, product, and POD. Expect HTTP 201. Keep contractDetailId.
10. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
11. Create one scale header via `POST /billing-by-scales` with `identifier` `32Z13M000000003`, `dateFrom` `2026-06-03`, `dateTo` `2026-06-03`, `billingPowerInKw` 1, `invoiceNumber` `INV13M03`, `invoiceDate` `2026-06-03T00:00:00`, `correction` false, `override` false, `saveRecordForIntermediatePeriod` false, `saveRecordForMeterReadings` false, and two table rows. Row `index` 1: `scaleCode` `A13M03`, `scaleType` `WHATEVER`, `tariffScale` `TARIFF-X`, `volumes` 70, `meterNumber` `M13M03`, `periodFrom` `2026-06-03`, `periodTo` `2026-06-03`. Row `index` 2: `scaleCode` `B13M03`, `volumes` 30, same meter and dates. Expect HTTP 201.
12. Run standard billing for June 2026 through `PATCH /billing-run/start-accounting` (create HTTP 200, each start HTTP 202) until `GET /billing-by-scales/list` (`page` 0, `size` 20, `prompt` `32Z13M000000003`, `exactMatch` true) returns HTTP 206 and `invoiced` `YES`. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list for this POD. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 3 Jun 2026.
**Expected test case results:**
1. One row, `type` `1`, `total_volume` `70`.
2. `total_volume` is not `100` and not `30`.

### TC-BE-4 (Positive): Cutoff keeps scales created at or after the latest qualifying create time

**Description:** The cutoff is the latest create time among active invoiced scales for the same POD and dates whose correction and override are both off, or both on. Lines from scales created before that cutoff are not added. A scale created at the cutoff is added.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M04` via `POST /grid-operators` (`status` `ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber` from `GET /customer/{id}`.
4. Create POD `32Z13M000000004` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`, that grid operator). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M04` via `POST /meters` on that POD, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M04` via `POST /scales` with `scaleForActiveElectricity` true, `status` `ACTIVE`, `defaultSelection` false, `scaleType` `ACTIVE`. Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create scale Early via `POST /billing-by-scales` with `identifier` `32Z13M000000004`, `dateFrom` `2026-06-04`, `dateTo` `2026-06-04`, `billingPowerInKw` 1, `invoiceNumber` `INV13M04A`, `invoiceDate` `2026-06-04T00:00:00`, `correction` false, `override` false, both save flags false, one row `scaleCode` `A13M04`, `volumes` 10, `meterNumber` `M13M04`, `index` 1, line dates `2026-06-04`. Expect HTTP 201. Keep scaleEarlyId. This is the older both-off scale.
10. Wait until Early's `create_date` is strictly earlier than the next create, then create scale Late via `POST /billing-by-scales` with the same identifier and header dates, `invoiceNumber` `INV13M04B`, `correction` true, `override` true, one row `volumes` 40, `scaleCode` `A13M04`, `index` 1. Expect HTTP 201. Keep scaleLateId. This is the newer both-on scale, so it is the cutoff.
11. Run standard billing for June 2026 through `start-accounting` (HTTP 202 on each start) so both scales are consumed. `GET /billing-by-scales/list` (`page` 0, `size` 20, `prompt` `32Z13M000000004`, `exactMatch` true) must show HTTP 206 and `invoiced` `YES` for both ids. If either is `NO`, stop.

**Test steps:**
1. Open the scale list. Both scales must show invoiced YES. If either is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 4 Jun 2026.
**Expected test case results:**
1. One row, `type` `1`, `total_volume` `40`.
2. `total_volume` is not `10` and not `50`. Early is before the cutoff and is left out. Late is on the cutoff and is kept.

### TC-BE-5 (Positive): The nightly rebuild does not change a historical month

**Description:** A quantity inserted for a month before the deployment date stays as inserted. The rebuild must not delete it when it finds no historical scales.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M05` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber` from `GET /customer/{id}`.
4. Create POD `32Z13M000000005` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M05` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create an active-electricity scale `A13M05` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Insert one historical row for POD identifier `32Z13M000000005`, this `customerNumber`, quantity `100`, period April 2026. There is no API for this insert. The story's source columns are `ZNUM`, `CUST_IDENT`, `QTY`, `period` from `PHOENIX.T_MONTHLY_QTY`. Do not insert a new-table row for April.
10. Do not create a billing-by-scales object for April 2026.

**Test steps:**
1. Read the historical April quantity. It is 100.
2. Run the midnight job once from 1 Jun 2026.
3. Read April again, and look for any new-table row that ends before 1 Jun 2026.
**Expected test case results:**
1. The historical quantity is still `100`. The job did not update or delete it.
2. The new POD table has no row for this POD with `period_to` before `2026-06-01`.

### TC-BE-6 (Positive): A new both-off scale for a historical month replaces the historical quantity

**Description:** After deployment, a data-by-scales object whose period ends inside a historical month, with correction and override both off, replaces that month's historical quantity. If several such objects exist, the latest create time wins.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M06` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000006` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M06` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M06` via `POST /scales` with `scaleForActiveElectricity` true, `status` `ACTIVE`. Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `SIGNED`, sub-status that the API accepts for `SIGNED` (use `SIGNED_BY_BOTH_SIDES` when a sub-status is required), entry into force `2026-01-01`. Expect HTTP 201. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Insert a historical row: POD `32Z13M000000006`, this `customerNumber`, quantity `100`, period April 2026.
10. Create the replacement scale via `POST /billing-by-scales` with `identifier` `32Z13M000000006`, `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `billingPowerInKw` 1, `invoiceNumber` `INV13M06`, `invoiceDate` `2026-06-15T00:00:00`, `correction` false, `override` false, both save flags false, one row `scaleCode` `A13M06`, `volumes` 40, `meterNumber` `M13M06`, `periodFrom` `2026-04-01`, `periodTo` `2026-04-30`, `index` 1. The row's create time must be after `2026-06-01`. Expect HTTP 201.
11. Mark that scale invoiced (`YES` on `GET /billing-by-scales/list`, `page` 0, `size` 20, `prompt` `32Z13M000000006`, `exactMatch` true, HTTP 206) by running the billing path that consumes it through `start-accounting`. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. The April scale must show invoiced YES. If it is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026. The window is 1 Sep 2025 through 30 Sep 2026.
3. Open the prepared total for this customer and POD and look at April.
**Expected test case results:**
1. April 2026 contributes `40`, not `100`. The historical `100` is replaced by the new object's `volumes`.
2. The prepared total for this POD includes `40` for April and does not include both `40` and `100`.

### TC-BE-7 (Positive): Correction only adds to an existing historical month

**Description:** When only correction is checked, override is off, and a historical row already exists for that month, the new volume is added to the historical volume.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M07` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000007` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M07` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M07` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `POST /contract-pods/manual`, `activationDate` `2026-01-01`, no deactivation date. Expect HTTP 200.
9. Insert a historical row: POD `32Z13M000000007`, this `customerNumber`, quantity `100`, period April 2026.
10. Create a correction scale via `POST /billing-by-scales` with the same POD, `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `billingPowerInKw` 1, `invoiceNumber` `INV13M07`, `invoiceDate` `2026-06-20T00:00:00`, `correction` true, `override` false, both save flags false, one row `volumes` 25, `scaleCode` `A13M07`, `meterNumber` `M13M07`, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
11. `GET /billing-by-scales/list` must show this scale `invoiced` `YES` (HTTP 206) before the variable job. If it is `NO`, stop.

**Test steps:**
1. Open the scale list. The April scale must show invoiced YES. If it is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total and look at April 2026.
**Expected test case results:**
1. April contributes `125` (`100` historical plus `25` correction).
2. April is not `25` alone and not `100` alone.

### TC-BE-8 (Negative): Correction only with no historical row is skipped

**Description:** If only correction is checked and the historical table has no row for that month, the object is skipped. It must not create a new historical quantity and must not enter the 13-month total.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M08` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000008` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M08` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M08` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Do not insert a historical row for April 2026 for this customer and POD.
10. Create a correction scale via `POST /billing-by-scales` with `identifier` `32Z13M000000008`, `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `correction` true, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M08`, `invoiceDate` `2026-06-20T00:00:00`, both save flags false, one row `volumes` 25, `scaleCode` `A13M08`, `index` 1. Expect HTTP 201.
11. List the scale with `GET /billing-by-scales/list` (`page` 0, `size` 20, `prompt` `32Z13M000000008`, `exactMatch` true). HTTP 206. Set `invoiced` to `YES` through the billing path before the job. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. The scale must show invoiced YES. If it is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Look at the historical table for April, and at the prepared total.
**Expected test case results:**
1. No historical row is created for April 2026. Quantity is not `25`.
2. The prepared 13-month total does not include `25` from this object. HTTP status of the job is success (the skip is business behaviour, not an error response). The customer still has no 13-month quantity from this scale.

### TC-BE-9 (Positive): A partial historical period is attributed to the month of period end

**Description:** A scale from 15 April through 15 May does not cover one calendar month. Its quantity belongs to May, the month of `period_to`, not split across April and May.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M09` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000009` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M09` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M09` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Insert historical rows: April 2026 quantity `10`, and May 2026 quantity `0`, for this POD and `customerNumber`. The May row must exist so a both-off object can replace it.
10. Create the partial scale via `POST /billing-by-scales` with `dateFrom` `2026-04-15`, `dateTo` `2026-05-15`, `identifier` `32Z13M000000009`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M09`, `invoiceDate` `2026-06-21T00:00:00`, both save flags false, one row `volumes` 80, `scaleCode` `A13M09`, `periodFrom` `2026-04-15`, `periodTo` `2026-05-15`, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
11. `GET /billing-by-scales/list` shows this scale `invoiced` `YES` (HTTP 206) before the variable job.

**Test steps:**
1. Open the scale list. The scale must show invoiced YES. If it is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total and compare April and May.
**Expected test case results:**
1. May 2026 contributes `80` (the partial object replaces the May historical `0`, because `period_to` is in May).
2. April 2026 stays `10`. It is not `50`, not `90`, and not `0`.

### TC-BE-10 (Positive): Invoice on 17 July 2026 carries July 2025 through June 2026 and copies the prepared value

**Description:** An invoice generated on 17 July 2026 includes 1 July 2025 through 30 June 2026 and excludes June 2025 and July 2026. Months through May 2026 stay in the historical table. June 2026 is the first counted month. Billing copies `podLast13MonthsConsumption` from the prepared table. It does not recalculate it from scales created after the variable job.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M10` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000010` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M10` via `POST /meters`, `installmentDate` `2025-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M10` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2025-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2025-06-01` and no deactivation date. Expect HTTP 200.
9. Insert historical rows for this POD and `customerNumber`: June 2025 quantity `777`, January 2026 quantity `100`.
10. Create a June 2026 scale via `POST /billing-by-scales` with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000010`, `billingPowerInKw` 1, `invoiceNumber` `INV13M10A`, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, one row `volumes` 50, `scaleCode` `A13M10`, `index` 1. Expect HTTP 201. Make it `invoiced` `YES` (list HTTP 206) before the rebuild job.
11. Create a July 2026 scale the same way with `dateFrom` `2026-07-01`, `dateTo` `2026-07-31`, `invoiceNumber` `INV13M10B`, `volumes` 999. Make it `invoiced` `YES` as well.
12. Run the scale rebuild job for deployment period start `2026-06-01`, then run the variable job as of `2026-07-17`. Read the prepared value and keep it as preparedKwh. It must already equal `150` (`100` January 2026 plus `50` June 2026) before the next scale is created.
13. After that variable job, create one more May 2026 scale with `volumes` 500, `invoiceNumber` `INV13M10C`, and do not run the variable job again.
14. Call `GET /accounting-period/available-accounting-periods` and keep an id covering July 2026.

**Test steps:**
1. Check the prepared total before billing. It must already be 150. Do not rerun the nightly job after the extra scale of 500.
2. Start a standard billing run with invoice date 17 Jul 2026 and generate the invoice for this customer.
3. Open the invoice document for this POD and read podLast13MonthsConsumption.
4. Ignore the detailed-data list. That list is the billed period, not this field.
**Expected test case results:**
1. `podLast13MonthsConsumption` is `150`.
2. It is not `777` (June 2025 is before the window), not `999` (July 2026 is the invoice month and is excluded), and not `650` (the May 2026 scale of `500` was created after the variable job and must not be recalculated during billing).
3. `GET /invoice/detailed-data` returns HTTP 206 and is the billed-period lines. It is not the pass check for `podLast13MonthsConsumption`. Failing this case means the template field is missing or not `150`.

### TC-BE-11 (Negative): Not-invoiced, deleted, and out-of-window scales stay out of the total

**Description:** A scale that is not invoiced, a scale with status `DELETED`, a month before the 13-month window, and the invoice month itself do not add quantity.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M11` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000011` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M11` via `POST /meters`, `installmentDate` `2025-08-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M11` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2025-08-01`. Expect HTTP 201. Activate the POD with `activationDate` `2025-08-01` and no deactivation date. Expect HTTP 200.
9. Insert a historical row for August 2025, quantity `777`, this POD and `customerNumber`. Do not insert September 2025 through May 2026.
10. Create an uninvoiced June 2026 scale via `POST /billing-by-scales` with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `invoiceNumber` `INV13M11A`, `volumes` 50, `scaleCode` `A13M11`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. Do not run billing. `GET /billing-by-scales/list` (`page` 0, `size` 20, `prompt` `32Z13M000000011`, `exactMatch` true) returns HTTP 206 and `invoiced` `NO`.
11. Create a second scale for July 2026 with `volumes` 60 and `invoiceNumber` `INV13M11B`, then delete it via `DELETE /billing-by-scales/{id}`. Expect HTTP 200. Listing with deleted rows visible shows status `DELETED`.

**Test steps:**
1. Confirm June still shows invoiced NO, and July shows Deleted.
2. Run the midnight job from 1 Jun 2026.
3. Run the nightly total job for invoice date 17 Oct 2026.
4. Open the new table and the prepared total for this POD.
**Expected test case results:**
1. The new table has no row whose `total_volume` is `50` or `60` for this POD.
2. The prepared total is `0` or absent. It is not `50`, not `60`, not `110`, and not `777`. August 2025 is before the window, which starts on 1 September 2025.
3. The jobs complete without an HTTP error. The exclusion is the business result. `GET /billing-by-scales/list` for the June scale is still `invoiced` `NO`.

### TC-BE-12 (Negative): Active in perpetuity is outside Signed, Active in Term, and Terminated

**Description:** The story counts only `SIGNED`, `ACTIVE_IN_TERM`, and `TERMINATED`. `ACTIVE_IN_PERPETUITY` is a real contract status and is not in that list, so its quantity is not in the customer's 13-month total.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M12` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create customer C-skip via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerSkipId and `customerNumber` from `GET /customer/{id}`.
4. Create POD `32Z13M000000012` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`, that grid operator). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M12` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M12` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_PERPETUITY`, entry into force `2026-06-01`, customerSkipId, the product, and this POD. Expect HTTP 201. Keep the contract id as contractId and the detail id as contractDetailId.
9. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200. The status under test is `ACTIVE_IN_PERPETUITY`, not the activation call.
10. Create a June 2026 scale via `POST /billing-by-scales` with `volumes` 80, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000012`, `scaleCode` `A13M12`, `invoiceNumber` `INV13M12`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. `GET /billing-by-scales/list` with `page` 0, `size` 20, `prompt` `32Z13M000000012`, `searchBy` `POD_IDENTIFIER`, `exactMatch` true returns HTTP 206 and `invoiced` `YES` before the jobs. If it is `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract. Status must be Active in Perpetuity.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total for this customer and POD.
**Expected test case results:**
1. The prepared total does not include `80`.
2. `GET /product-contract/{contractId}?versionId={contractDetailId}` returns HTTP 200 and `basicParameters.status` is `ACTIVE_IN_PERPETUITY`. That status is outside Signed, Active in Term, and Terminated, so the quantity is left out. The jobs do not return an error body for this skip.

### TC-BE-13 (Positive): Signed and terminated versions count, and a missing deactivation date means 1 January 2090

**Description:** A `SIGNED` version and a `TERMINATED` version are both in scope when they overlap the 13-month window and the POD is activated. With no deactivation date, period end is still inside the POD's active range because the end date used is `2090-01-01`.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M13` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000013` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M13` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M13` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create version 1 via `POST /product-contract` with `versionStatus` `SIGNED`, entry into force `2026-01-01`, this customer, product, and POD. Expect HTTP 201. Keep contractId and contractDetailIdV1.
9. Activate the POD on version 1 via `POST /contract-pods/manual` with `activationDate` `2026-01-01` and no `deactivationDate`. Expect HTTP 200.
10. Call `PUT /product-contract/status-update/{contractId}?versionId={contractDetailIdV1}` with `contractStatus` `TERMINATED`, `contractSubStatus` `EXPIRED`, `contractVersionStatus` `SIGNED`. Expect HTTP 201. `GET /product-contract/{contractId}?versionId={contractDetailIdV1}` returns HTTP 200 and `basicParameters.status` is `TERMINATED`. The POD activation date stays `2026-01-01` and `deactivationDate` is still empty.
11. Create a June 2026 scale via `POST /billing-by-scales` with `volumes` 55, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000013`, `scaleCode` `A13M13`, `invoiceNumber` `INV13M13`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs.
12. Run the scale rebuild so the new table has `total_volume` `55` for June 2026. `period_to` `2026-06-30` is before `2090-01-01`.

**Test steps:**
1. Open the contract. Status must be Terminated, and the POD still has no deactivation date.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total for this customer and POD.
**Expected test case results:**
1. The prepared total includes `55`.
2. The version status `TERMINATED` did not drop the POD. The missing deactivation date did not drop the June row. The comparison end date used for the POD is `2090-01-01`.

### TC-BE-14 (Positive): Two overlapping versions each add the same period row

**Description:** The story says to sum the new-table rows for each matching contract version. Two versions of the same customer both cover June 2026 and both have the POD activated across that `period_to`. The same `55` row is added twice.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M14` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000014` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId, podDetailId for the version that will be activated twice (use the detail id valid for each version).
5. Create meter `M13M14` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M14` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create version 1 via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`, start of version `2026-01-01`, end of version `2026-10-31`. Expect HTTP 201. Keep contractId and contractDetailIdV1. Activate the POD with `POST /contract-pods/manual`, `activationDate` `2026-01-01`, `deactivationDate` `2026-10-31`. Expect HTTP 200.
9. Create version 2 via `PUT /product-contract/{contractId}?versionId={contractDetailIdV1}&changeFutureVersionsPods=false` with `savingAsNewVersion` true, `startDate` `2026-06-01`, `versionStatus` `ACTIVE_IN_TERM`, the same customer, product, and POD. Expect HTTP 200. Keep the new contract detail id as contractDetailIdV2. Activate the POD on version 2 via `POST /contract-pods/manual` with `activationDate` `2026-06-01` and `deactivationDate` `2026-10-31`. Expect HTTP 200. Both versions overlap June 2026, and `2026-06-30` falls inside both activation ranges.
10. Create one June scale via `POST /billing-by-scales` with `volumes` 55, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000014`, `scaleCode` `A13M14`, `invoiceNumber` `INV13M14`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. List shows `invoiced` `YES` (HTTP 206).
11. Run the scale rebuild. The new table has one row, `total_volume` `55`, for this POD and June 2026.

**Test steps:**
1. Open both contract versions and confirm both activations cover 30 Jun 2026.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total for this customer and POD.
**Expected test case results:**
1. The prepared total includes `110` (`55` added for version 1 and `55` added for version 2).
2. It is not `55`. This is the sum the story writes in the calculation steps. See the Finding in References: overlapping versions can count one period twice, and that needs a product decision. This case checks the written rule.

---

### TC-BE-15 (Positive): An invoice on 1 August 2026 uses August 2025 through July 2026

**Description:** The July example moves forward one month. An invoice on 1 August 2026 excludes August 2026 and also excludes July 2025. The window is 1 August 2025 through 31 July 2026. August 2025 is still in the historical table. July 2026 is counted, because collection starts on 1 June 2026.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M15` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000015` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M15` via `POST /meters`, `installmentDate` `2025-07-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M15` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2025-07-01`. Expect HTTP 201. Activate the POD with `activationDate` `2025-07-01` and no deactivation date. Expect HTTP 200.
9. Insert historical rows for this POD and `customerNumber`: July 2025 quantity `9`, August 2025 quantity `11`.
10. Create a July 2026 scale via `POST /billing-by-scales` with `dateFrom` `2026-07-01`, `dateTo` `2026-07-31`, `correction` false, `override` false, `identifier` `32Z13M000000015`, `volumes` 13, `scaleCode` `A13M15`, `invoiceNumber` `INV13M15`, `billingPowerInKw` 1, `invoiceDate` `2026-07-31T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs.
11. Run the scale rebuild for deployment period start `2026-06-01`.

**Test steps:**
1. Open the scale list. The July 2026 scale must show invoiced YES. If it is NO, stop.
2. Run the nightly total job for invoice date 1 Aug 2026.
3. Open the prepared total for this customer and POD.
**Expected test case results:**
1. The prepared total is `24` (`11` for August 2025 plus `13` for July 2026).
2. It is not `9` (July 2025 is before this window), not `11` alone, not `13` alone, and not `33`.

### TC-BE-16 (Positive): Two PODs of one customer keep their own totals

**Description:** The variable is per POD for the customer. One POD's quantity is not added onto the other POD.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M16` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create one customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD A `32Z13M000000016` and POD B `32Z13M000000116` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201 for each. Keep both pod ids and detail ids.
5. Create meters `M13M16A` and `M13M16B` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true, one meter on each POD. Expect HTTP 201 each.
6. Create scale `A13M16` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create one contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`, this customer, and both PODs. Expect HTTP 201. Activate each POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200 each.
9. Create a June 2026 scale for POD A via `POST /billing-by-scales`, `identifier` `32Z13M000000016`, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `volumes` 50, `scaleCode` `A13M16`, `invoiceNumber` `INV13M16A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. Create a June 2026 scale for POD B the same way with `identifier` `32Z13M000000116`, `volumes` 70, `invoiceNumber` `INV13M16B`. Expect HTTP 201.
11. `GET /billing-by-scales/list` shows `invoiced` `YES` for both (HTTP 206). Run the scale rebuild for `2026-06-01`.

**Test steps:**
1. Open the scale list. Both scales must show invoiced YES.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total for POD A, then for POD B.
**Expected test case results:**
1. POD A is `50`. POD B is `70`.
2. Neither value is `120`.

### TC-BE-17 (Positive): Historical months alone fill the 13-month total

**Description:** When the new table has no rows, the variable is the sum of this customer's historical months inside the window. A month before the window stays out.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M17` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000017` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M17` via `POST /meters`, `installmentDate` `2025-08-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M17` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2025-08-01`. Expect HTTP 201. Activate the POD with `activationDate` `2025-08-01` and no deactivation date. Expect HTTP 200.
9. Insert historical rows for this POD and `customerNumber`: August 2025 quantity `5`, January 2026 quantity `10`, March 2026 quantity `20`, May 2026 quantity `30`.
10. Do not create a billing-by-scales object.

**Test steps:**
1. Run the midnight job from 1 Jun 2026.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the new table for this POD, then the prepared total.
**Expected test case results:**
1. The new table has no row for this POD.
2. The prepared total is `60` (`10` + `20` + `30`).
3. It is not `65`. August 2025 is outside the window, which starts on 1 September 2025. January, March, and May 2026 are historical months inside the window. No historical row is on June 2026 or October 2026.

### TC-BE-18 (Positive): Two active-electricity lines on one header are added

**Description:** Both lines are marked for active electricity. Their volumes are added into the one row for that header. Scale name and tariff are not filters.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M18` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000018` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M18` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale A `A13M18` and scale B `B13M18` via `POST /scales`. Both have `scaleForActiveElectricity` true, `status` `ACTIVE`, `defaultSelection` false. Expect HTTP 201 each.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create one header via `POST /billing-by-scales` with `identifier` `32Z13M000000018`, `dateFrom` `2026-06-18`, `dateTo` `2026-06-18`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M18`, `invoiceDate` `2026-06-18T00:00:00`, both save flags false, and two rows. Row 1: `scaleCode` `A13M18`, `volumes` 70, `index` 1, line dates `2026-06-18`. Row 2: `scaleCode` `B13M18`, `volumes` 30, `index` 2, same dates. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 18 Jun 2026.
**Expected test case results:**
1. One row, `type` `1`, `total_volume` `100`.
2. `total_volume` is not `70` and not `30`.

### TC-BE-19 (Positive): A full month stores every clock hour once and does not divide the quantity

**Description:** AC-5's month example. A line from 1 June through 30 June stores one timestamp per clock hour. The quantity stays whole.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M19` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000019` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M19` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M19` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000019`, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M19`, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, one row `scaleCode` `A13M19`, `volumes` 3000, `periodFrom` `2026-06-01`, `periodTo` `2026-06-30`, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 1-30 Jun 2026 and count the hours.
**Expected test case results:**
1. One row, `type` `1`, `period_from` `2026-06-01`, `period_to` `2026-06-30`, `total_volume` `3000`.
2. `hours` has 720 timestamps, `2026-06-01 00:00` through `2026-06-30 23:00`, each once. June 2026 has 30 days.
3. `total_volume` is not `4.166` (`3000 / 720`).

### TC-BE-20 (Positive): The same clock hour from two lines is stored once

**Description:** Two kept lines cover the same day. Each hour of that day is stored once, not twice. The two volumes are still added.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M20` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000020` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M20` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scales `A13M20` and `B13M20` via `POST /scales`. Both have `scaleForActiveElectricity` true and `status` `ACTIVE`. Expect HTTP 201 each.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create one header via `POST /billing-by-scales` with `identifier` `32Z13M000000020`, `dateFrom` `2026-06-20`, `dateTo` `2026-06-20`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M20`, `invoiceDate` `2026-06-20T00:00:00`, both save flags false. Row 1: `scaleCode` `A13M20`, `volumes` 10, dates `2026-06-20`. Row 2: `scaleCode` `B13M20`, `volumes` 20, same dates. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 20 Jun 2026 and count the hours.
**Expected test case results:**
1. One row, `total_volume` `30`.
2. `hours` has 24 timestamps, `2026-06-20 00:00` through `2026-06-20 23:00`. It does not have 48.

### TC-BE-21 (Positive): A volumes value of zero is used

**Description:** Zero is a volumes number, so the job does not fall back to `totalVolumes`.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M21` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000021` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M21` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M21` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000021`, `dateFrom` `2026-06-21`, `dateTo` `2026-06-21`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M21`, `invoiceDate` `2026-06-21T00:00:00`, both save flags false, one row `scaleCode` `A13M21`, `volumes` 0, `totalVolumes` 99, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 21 Jun 2026.
**Expected test case results:**
1. One row, `total_volume` `0`.
2. `total_volume` is not `99`.

### TC-BE-22 (Positive): A period that starts on the deployment date is collected

**Description:** Collection starts at the deployment period. A one-day header on that date is written to the new table.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M22` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000022` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M22` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M22` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000022`, `dateFrom` `2026-06-01`, `dateTo` `2026-06-01`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M22`, `invoiceDate` `2026-06-01T00:00:00`, both save flags false, one row `scaleCode` `A13M22`, `volumes` 15, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 1 Jun 2026.
**Expected test case results:**
1. One row, `type` `1`, `period_from` `2026-06-01`, `period_to` `2026-06-01`, `total_volume` `15`.

### TC-BE-23 (Positive): A second rebuild still leaves one row

**Description:** The midnight job clears the POD table and fills it again. Running it twice does not add the same quantity a second time.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M23` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000023` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M23` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M23` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000023`, `dateFrom` `2026-06-23`, `dateTo` `2026-06-23`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M23`, `invoiceDate` `2026-06-23T00:00:00`, both save flags false, one row `scaleCode` `A13M23`, `volumes` 18, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the first job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once and read the row for 23 Jun 2026.
3. Run the same job a second time and read the rows for that day again.
**Expected test case results:**
1. After the first run, one row has `total_volume` `18`.
2. After the second run, there is still one row and `total_volume` is still `18`, not `36`.

### TC-BE-24 (Positive): A scale created after the cutoff is added to the cutoff scale

**Description:** The cutoff is the latest create time among active invoiced scales with both flags off or both flags on. Step 2 then keeps every active invoiced scale for the same POD and dates created at that time or later, and adds the kept line quantities. A later correction-only scale is after the cutoff, so its volume is added.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M24` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000024` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M24` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M24` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create scale Early via `POST /billing-by-scales` with `identifier` `32Z13M000000024`, `dateFrom` `2026-06-24`, `dateTo` `2026-06-24`, `correction` false, `override` false, `volumes` 10, `scaleCode` `A13M24`, `invoiceNumber` `INV13M24A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-24T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. After Early's create time, create scale Cutoff via `POST /billing-by-scales` with the same identifier and dates, `correction` true, `override` true, `volumes` 40, `invoiceNumber` `INV13M24B`. Expect HTTP 201. This both-on scale is the cutoff. Early is before it.
11. After Cutoff's create time, create scale After via `POST /billing-by-scales` with the same identifier and dates, `correction` true, `override` false, `volumes` 7, `invoiceNumber` `INV13M24C`. Expect HTTP 201.
12. `GET /billing-by-scales/list` shows `invoiced` `YES` for all three (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. All three must show invoiced YES. If any is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new-table row for 24 Jun 2026.
**Expected test case results:**
1. One row, `total_volume` `47` (`40` on the cutoff plus `7` created later).
2. It is not `10` (Early is before the cutoff), not `40` alone, not `57`, and not `17`.

### TC-BE-25 (Positive): Both flags on replace a historical month

**Description:** For a historical month, both flags checked is the same branch as both flags unchecked. The new volume replaces the historical volume. It is not added.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M25` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000025` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M25` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M25` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Insert a historical row: POD `32Z13M000000025`, this `customerNumber`, quantity `100`, period April 2026.
10. Create the scale via `POST /billing-by-scales` with `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `identifier` `32Z13M000000025`, `correction` true, `override` true, `volumes` 40, `scaleCode` `A13M25`, `invoiceNumber` `INV13M25`, `billingPowerInKw` 1, `invoiceDate` `2026-06-20T00:00:00`, both save flags false, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
11. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the variable job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total and look at April.
**Expected test case results:**
1. April contributes `40`.
2. April is not `140` and not `100`.

### TC-BE-26 (Positive): The later both-off object replaces a historical month

**Description:** When several objects for the same historical month have both flags off or both flags on, the latest creation time is the one that replaces the historical quantity. The older object is not added.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M26` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000026` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M26` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M26` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Insert a historical row: POD `32Z13M000000026`, this `customerNumber`, quantity `100`, period April 2026.
10. Create the older scale via `POST /billing-by-scales` with `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `correction` false, `override` false, `volumes` 40, `scaleCode` `A13M26`, `invoiceNumber` `INV13M26A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-18T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
11. After that create time, create the newer scale the same way with `volumes` 70, `invoiceNumber` `INV13M26B`, `correction` false, `override` false. Expect HTTP 201.
12. `GET /billing-by-scales/list` shows `invoiced` `YES` for both (HTTP 206) before the variable job.

**Test steps:**
1. Open the scale list. Both must show invoiced YES.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total and look at April.
**Expected test case results:**
1. April contributes `70`.
2. April is not `40`, not `100`, not `110`, and not `210`.

### TC-BE-27 (Positive): A pre-deployment period is not written to the new table

**Description:** A scale object created after deployment for a month before the deployment date updates the historical path only. The midnight job must not write that period into the new table.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M27` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000027` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M27` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M27` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Create an April 2026 scale via `POST /billing-by-scales` with `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `correction` false, `override` false, `identifier` `32Z13M000000027`, `volumes` 40, `scaleCode` `A13M27`, `invoiceNumber` `INV13M27`, `billingPowerInKw` 1, `invoiceDate` `2026-06-19T00:00:00`, both save flags false, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new table for this POD.
**Expected test case results:**
1. The new table has no row for this POD with `period_to` before `2026-06-01`.
2. It has no row whose `total_volume` is `40`.

### TC-BE-28 (Positive): The new table keeps the invoice month and the variable drops it

**Description:** The midnight job does not limit dates, apart from the deployment start. The variable excludes the month of the invoice. Both statements are checked on the same POD.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M28` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000028` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M28` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M28` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June 2026 scale via `POST /billing-by-scales` with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `volumes` 50, `scaleCode` `A13M28`, `invoiceNumber` `INV13M28A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. Create an October 2026 scale the same way with `dateFrom` `2026-10-01`, `dateTo` `2026-10-31`, `volumes` 999, `invoiceNumber` `INV13M28B`, `invoiceDate` `2026-10-31T00:00:00`. Expect HTTP 201.
11. `GET /billing-by-scales/list` shows `invoiced` `YES` for both (HTTP 206). Do not insert historical rows.

**Test steps:**
1. Open the scale list. Both scales must show invoiced YES.
2. Run the midnight job from 1 Jun 2026 and open the new table.
3. Run the nightly total job for invoice date 17 Oct 2026 and open the prepared total.
**Expected test case results:**
1. The new table has a June row with `total_volume` `50` and an October 2026 row with `total_volume` `999`.
2. The prepared total is `50`. It is not `999` and not `1049`.

### TC-BE-29 (Positive): A version that is still Signed is included

**Description:** Signed is in the status list. The version is left Signed. It is not moved to Terminated before the job.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M29` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000029` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M29` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M29` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `SIGNED` and sub-status `SIGNED_BY_BOTH_SIDES`, entry into force `2026-01-01`. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200. Do not call status update.
9. Create a June 2026 scale via `POST /billing-by-scales` with `volumes` 55, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000029`, `scaleCode` `A13M29`, `invoiceNumber` `INV13M29`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206). Run the scale rebuild so the new table has `total_volume` `55`.

**Test steps:**
1. Open the contract and confirm the status is still Signed.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total for this customer and POD.
**Expected test case results:**
1. `basicParameters.status` is `SIGNED`.
2. The prepared total includes `55`.

### TC-BE-30 (Positive): A period end equal to the deactivation date is included

**Description:** The new-table row is kept when its period end falls within activation and deactivation. The end date itself is inside that range.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M30` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000030` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M30` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M30` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD via `POST /contract-pods/manual` with `activationDate` `2026-01-01` and `deactivationDate` `2026-06-30`. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `volumes` 55, `scaleCode` `A13M30`, `identifier` `32Z13M000000030`, `invoiceNumber` `INV13M30`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206). Run the scale rebuild. The new table row has `period_to` `2026-06-30` and `total_volume` `55`.

**Test steps:**
1. Confirm the POD deactivation date is 30 Jun 2026, the same day as the period end.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total.
**Expected test case results:**
1. The prepared total includes `55`.
2. `period_to` `2026-06-30` is the deactivation date, so the row is inside the POD's active range.

### TC-BE-31 (Positive): A partial period after deployment stays one row and is summed whole

**Description:** The month-split note applies to historical objects. A new-table header keeps its own start and end. The variable adds that row's whole quantity when the period end is inside activation and inside the 13-month window.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M31` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000031` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M31` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M31` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000031`, `dateFrom` `2026-06-15`, `dateTo` `2026-07-15`, `correction` false, `override` false, `volumes` 80, `scaleCode` `A13M31`, `invoiceNumber` `INV13M31`, `billingPowerInKw` 1, `invoiceDate` `2026-07-15T00:00:00`, both save flags false, one row with the same dates, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the rebuild.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job and open the new-table row.
3. Run the nightly total job for invoice date 17 Oct 2026 and open the prepared total.
**Expected test case results:**
1. One new-table row, `period_from` `2026-06-15`, `period_to` `2026-07-15`, `total_volume` `80`. There are not two month rows.
2. `hours` has 744 timestamps, `2026-06-15 00:00` through `2026-07-15 23:00`, each once (31 days).
3. The prepared total includes `80` once. It is not `40` and not `160`.

### TC-BE-32 (Positive): The customer whose activation contains the period end receives the quantity

**Description:** Two customers use the same POD in sequence. A historical April object is applied to the customer whose activation contains 30 April. A June new-table row is applied to the customer whose activation contains 30 June.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M32` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create customer A and customer B via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201 each. Keep customerNumberA and customerNumberB.
4. Create POD `32Z13M000000032` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and the detail id used for each contract.
5. Create meter `M13M32` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M32` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create contract A via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`, customer A, and this POD. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and `deactivationDate` `2026-04-30`. Expect HTTP 200.
9. Create contract B via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-05-01`, customer B, and this POD. Expect HTTP 201. Activate the POD with `activationDate` `2026-05-01` and `deactivationDate` `2026-10-31`. Expect HTTP 200.
10. Insert a historical row for April 2026, POD `32Z13M000000032`, customerNumberA, quantity `100`. Do not insert an April row for customer B.
11. Create an April scale via `POST /billing-by-scales` with `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `correction` false, `override` false, `volumes` 40, `scaleCode` `A13M32`, `invoiceNumber` `INV13M32A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-20T00:00:00`, both save flags false, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
12. Create a June scale the same way with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `volumes` 55, `invoiceNumber` `INV13M32B`, `correction` false, `override` false. Expect HTTP 201.
13. `GET /billing-by-scales/list` shows `invoiced` `YES` for both (HTTP 206). Run the scale rebuild. The new table has the June row `total_volume` `55` and has no April row.

**Test steps:**
1. Open the scale list. Both scales must show invoiced YES.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total for customer A, then for customer B, on this POD.
**Expected test case results:**
1. Customer A is `40`. April's period end is inside customer A's activation, so the new object replaces A's historical `100`. Customer A does not include June `55`.
2. Customer B is `55`. June's period end is inside customer B's activation. Customer B does not include `40`.
3. Neither total is `95`.

### TC-BE-33 (Negative): Override only, with no both-off or both-on scale, writes no new-table row

**Description:** The cutoff is taken only from active invoiced scales whose correction and override are both off or both on. A single override-only scale does not create a cutoff, so its volume is not written.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M33` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000033` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M33` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M33` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create one scale via `POST /billing-by-scales` with `identifier` `32Z13M000000033`, `dateFrom` `2026-06-25`, `dateTo` `2026-06-25`, `correction` false, `override` true, `volumes` 33, `scaleCode` `A13M33`, `invoiceNumber` `INV13M33`, `billingPowerInKw` 1, `invoiceDate` `2026-06-25T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. Do not create another scale for this POD and these dates.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Look for a new-table row for 25 Jun 2026.
**Expected test case results:**
1. No new-table row exists for this header.
2. No row has `total_volume` `33`. The job completes without an error response.

### TC-BE-34 (Negative): Correction only on a new period, with no cutoff scale, writes no new-table row

**Description:** This is the new-table cutoff rule, not the historical skip. The only scale has correction on and override off, so step 1 finds no cutoff.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M34` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000034` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M34` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M34` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create one scale via `POST /billing-by-scales` with `identifier` `32Z13M000000034`, `dateFrom` `2026-06-26`, `dateTo` `2026-06-26`, `correction` true, `override` false, `volumes` 25, `scaleCode` `A13M34`, `invoiceNumber` `INV13M34`, `billingPowerInKw` 1, `invoiceDate` `2026-06-26T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Look for a new-table row for 26 Jun 2026.
**Expected test case results:**
1. No new-table row exists for this header.
2. No row has `total_volume` `25`.

### TC-BE-35 (Negative): A POD with no activation date is skipped

**Description:** The variable skips a POD on a contract version when that POD has no activation date. The scale job can still write the new-table row, because it does not read the contract.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M35` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000035` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M35` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M35` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`, this customer, product, and POD. Expect HTTP 201. Do not call `POST /contract-pods/manual`. The POD has no activation date.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 80, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000035`, `scaleCode` `A13M35`, `invoiceNumber` `INV13M35`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Confirm the contract is Active in Term and the POD has no activation date.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the new table, then the prepared total.
**Expected test case results:**
1. The new table has a June row with `total_volume` `80`.
2. The prepared total does not include `80`.
3. The contract version status remains `ACTIVE_IN_TERM`. The skip reason is the missing activation date.

### TC-BE-36 (Negative): A period end after deactivation is not summed

**Description:** The June row exists in the new table. Its period end is after the POD deactivation date, so the variable does not add it.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M36` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000036` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M36` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M36` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and `deactivationDate` `2026-05-31`. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `volumes` 55, `scaleCode` `A13M36`, `identifier` `32Z13M000000036`, `invoiceNumber` `INV13M36`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206). Run the scale rebuild.

**Test steps:**
1. Open the new table and confirm the June row exists, total 55, period end 30 Jun 2026.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total.
**Expected test case results:**
1. The new table row exists with `period_to` `2026-06-30` and `total_volume` `55`.
2. The prepared total does not include `55`, because `2026-06-30` is after deactivation `2026-05-31`.

### TC-BE-37 (Negative): A contract version that ends before the 13-month window is not used

**Description:** Terminated is an allowed status, but the version must overlap the 13-month window by at least one day. This version ends on 31 August 2025. The window for an invoice on 17 October 2026 starts on 1 September 2025.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M37` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000037` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M37` via `POST /meters`, `installmentDate` `2025-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M37` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `SIGNED`, sub-status `SIGNED_BY_BOTH_SIDES`, entry into force `2025-01-01`, version start `2025-01-01`, version end `2025-08-31`, this customer, product, and POD. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2025-01-01` and `deactivationDate` `2025-08-31`. Expect HTTP 200.
9. Call `PUT /product-contract/status-update/{contractId}?versionId={contractDetailId}` with `contractStatus` `TERMINATED`, `contractSubStatus` `EXPIRED`, `contractVersionStatus` `SIGNED`. Expect HTTP 201. `GET /product-contract/{contractId}?versionId={contractDetailId}` shows `basicParameters.status` `TERMINATED`.
10. Create a June 2026 scale via `POST /billing-by-scales` with `volumes` 55, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000037`, `scaleCode` `A13M37`, `invoiceNumber` `INV13M37`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. Do not insert historical rows.
11. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206). If it stays `NO`, stop. Run the scale rebuild.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm it is Terminated and ends on 31 Aug 2025.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. The new table may contain `total_volume` `55` for June 2026. The prepared total does not include `55`.
2. The version status is `TERMINATED`, which is allowed, and the version dates do not overlap 1 September 2025 through 30 September 2026.

### TC-BE-38 (Negative): Draft is not one of the three counted statuses

**Description:** The variable counts Signed, Active in Term, and Terminated only. Draft is a real contract status and is not in that list.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M38` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000038` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M38` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M38` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `DRAFT`, sub-status `DRAFT`, entry into force `2026-06-01`, this customer, product, and POD. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 81, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000038`, `scaleCode` `A13M38`, `invoiceNumber` `INV13M38`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` (`page` 0, `size` 20, `prompt` `32Z13M000000038`, `searchBy` `POD_IDENTIFIER`, `exactMatch` true) shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm the status is Draft.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. `basicParameters.status` is `DRAFT`.
2. The prepared total does not include `81`.

### TC-BE-39 (Negative): Ready is not one of the three counted statuses

**Description:** Ready is a real contract status. The variable does not count it.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M39` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000039` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M39` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M39` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `READY`, sub-status `READY`, entry into force `2026-06-01`. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 82, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000039`, `scaleCode` `A13M39`, `invoiceNumber` `INV13M39`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm the status is Ready.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. `basicParameters.status` is `READY`.
2. The prepared total does not include `82`.

### TC-BE-40 (Negative): Entered into force is not one of the three counted statuses

**Description:** Entered into force is a real contract status. The variable does not count it.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M40` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000040` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M40` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M40` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ENTERED_INTO_FORCE`, sub-status `AWAITING_ACTIVATION`, entry into force `2026-06-01`. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 83, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000040`, `scaleCode` `A13M40`, `invoiceNumber` `INV13M40`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm the status is Entered into Force.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. `basicParameters.status` is `ENTERED_INTO_FORCE`.
2. The prepared total does not include `83`.

### TC-BE-41 (Negative): Cancelled is not one of the three counted statuses

**Description:** Cancelled is a real contract status. The variable does not count it.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M41` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000041` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M41` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M41` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `CANCELLED`, sub-status `UNSENT_TO_CUSTOMER`, entry into force `2026-06-01`. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 84, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000041`, `scaleCode` `A13M41`, `invoiceNumber` `INV13M41`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm the status is Cancelled.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. `basicParameters.status` is `CANCELLED`.
2. The prepared total does not include `84`.

### TC-BE-42 (Negative): Changed with agreement is not one of the three counted statuses

**Description:** Changed with agreement is a real contract status. The story's list does not include it, so its quantity is not in the total.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M42` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000042` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M42` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M42` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `CHANGED_WITH_AGREEMENT`, sub-status `UNSENT_TO_CUSTOMER`, entry into force `2026-06-01`. Expect HTTP 201. Keep contractId and contractDetailId. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create a June scale via `POST /billing-by-scales` with `volumes` 85, `dateFrom` `2026-06-01`, `dateTo` `2026-06-30`, `correction` false, `override` false, `identifier` `32Z13M000000042`, `scaleCode` `A13M42`, `invoiceNumber` `INV13M42`, `billingPowerInKw` 1, `invoiceDate` `2026-06-30T00:00:00`, both save flags false, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the jobs. If it stays `NO`, stop.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Open the contract and confirm the status is Changed with Agreement.
3. Run the midnight job, then the nightly total job for 17 Oct 2026.
4. Open the prepared total.
**Expected test case results:**
1. `basicParameters.status` is `CHANGED_WITH_AGREEMENT`.
2. The prepared total does not include `85`.

### TC-BE-43 (Negative): Another customer's historical quantity is not used

**Description:** A historical row is stored per customer number and POD. Customer A's total does not take a row stored for customer B, even when the POD identifier is the same.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M43` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create customer A and customer B via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201 each. Keep customerNumberA and customerNumberB.
4. Create POD `32Z13M000000043` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M43` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M43` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create customer A's contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`, customer A, and this POD. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200. Do not create a contract for customer B.
9. Insert one historical row: POD `32Z13M000000043`, customerNumberB, quantity `100`, period January 2026. Do not insert a row for customerNumberA.
10. Do not create a billing-by-scales object.

**Test steps:**
1. Run the nightly total job for invoice date 17 Oct 2026.
2. Open the prepared total for customer A and this POD.
**Expected test case results:**
1. Customer A's prepared total does not include `100`.
2. It is `0` or absent. The historical row belongs to customer B.

### TC-BE-44 (Negative): A deleted scale is gone after the next rebuild

**Description:** The midnight job clears the table and fills it from scales that are still active and invoiced. A scale that was written once and is then deleted is not written again.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M44` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000044` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M44` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M44` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000044`, `dateFrom` `2026-06-27`, `dateTo` `2026-06-27`, `correction` false, `override` false, `volumes` 120, `scaleCode` `A13M44`, `invoiceNumber` `INV13M44`, `billingPowerInKw` 1, `invoiceDate` `2026-06-27T00:00:00`, both save flags false, `index` 1. Expect HTTP 201. Keep scaleId.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206). Run the scale rebuild once and confirm one row with `total_volume` `120`.
11. Call `DELETE /billing-by-scales/{scaleId}`. Expect HTTP 200. Listing shows status `DELETED`.

**Test steps:**
1. Confirm the row of 120 exists, and the scale is now Deleted.
2. Run the midnight job again from 1 Jun 2026.
3. Look for the row for 27 Jun 2026.
**Expected test case results:**
1. No row remains with `total_volume` `120`.
2. The cleared table was not refilled from the deleted scale.

### TC-BE-45 (Negative): A header whose lines are all outside active electricity stores no such quantity

**Description:** Every line on the header uses a scale with `scaleForActiveElectricity` false. The job does not take that line's volume. Difference, correction, and price on that line are also unused.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M45` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000045` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M45` via `POST /meters`, `installmentDate` `2026-06-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `B13M45` via `POST /scales` with `scaleForActiveElectricity` false, `status` `ACTIVE`, `defaultSelection` false. Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-06-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-06-01` and no deactivation date. Expect HTTP 200.
9. Create the scale via `POST /billing-by-scales` with `identifier` `32Z13M000000045`, `dateFrom` `2026-06-28`, `dateTo` `2026-06-28`, `correction` false, `override` false, `billingPowerInKw` 1, `invoiceNumber` `INV13M45`, `invoiceDate` `2026-06-28T00:00:00`, both save flags false, one row `scaleCode` `B13M45`, `volumes` 30, `difference` 30, `correction` 5, `unitPrice` 2, `index` 1. Expect HTTP 201.
10. `GET /billing-by-scales/list` shows `invoiced` `YES` (HTTP 206) before the job.

**Test steps:**
1. Open the scale list. Invoiced must be YES. If it is NO, stop.
2. Run the midnight job once from 1 Jun 2026.
3. Open the new table for 28 Jun 2026.
**Expected test case results:**
1. The stored quantity for this header is not `30`, not `5`, and not `2`.
2. Pass when the row is absent, or when a row exists with `total_volume` `0`. Fail when `total_volume` is `30`.

### TC-BE-46 (Positive): A correction adds to an existing scale when the historical table has no row

**Description:** Story version 12 and the diagram say a correction is added when a record for that period exists in the historical table or as a scale object in the system. This case has no historical row. An earlier both-off scale is the object in the system. The later correction is added to that object's volume. The correction object itself is not treated as the existing record.

**Preconditions:**
1. On dev2, deployment period start is `2026-06-01`.
2. Create grid operator `GO13M46` via `POST /grid-operators` (`ACTIVE`, `defaultSelection` false). Expect HTTP 201.
3. Create a customer via `POST /customer` (`PRIVATE_CUSTOMER`, `ACTIVE`). Expect HTTP 201. Keep customerId and `customerNumber`.
4. Create POD `32Z13M000000046` via `POST /pod` (`CONSUMER`, `HOUSEHOLD`, `LOW`, `ACTIVE`). Expect HTTP 201. Keep podId and podDetailId.
5. Create meter `M13M46` via `POST /meters`, `installmentDate` `2026-01-01`, `warningAcceptedByUser` true. Expect HTTP 201.
6. Create scale `A13M46` via `POST /scales` (`scaleForActiveElectricity` true, `ACTIVE`). Expect HTTP 201.
7. Create terms, a `BY_SCALES` price component, and an `ACTIVE` `SUPPLY_ONLY` product (HTTP 201 each).
8. Create the contract via `POST /product-contract` with `versionStatus` `ACTIVE_IN_TERM`, entry into force `2026-01-01`. Expect HTTP 201. Activate the POD with `activationDate` `2026-01-01` and no deactivation date. Expect HTTP 200.
9. Do not insert a historical row for April 2026.
10. Create the both-off scale via `POST /billing-by-scales` with `identifier` `32Z13M000000046`, `dateFrom` `2026-04-01`, `dateTo` `2026-04-30`, `correction` false, `override` false, `volumes` 40, `scaleCode` `A13M46`, `invoiceNumber` `INV13M46A`, `billingPowerInKw` 1, `invoiceDate` `2026-06-18T00:00:00`, both save flags false, `index` 1. Create time after `2026-06-01`. Expect HTTP 201.
11. After that create time, create the correction scale the same way with `correction` true, `override` false, `volumes` 25, `invoiceNumber` `INV13M46B`. Expect HTTP 201.
12. `GET /billing-by-scales/list` shows both `invoiced` `YES` (HTTP 206) before the nightly job. If either is `NO`, stop.

**Test steps:**
1. Open the scale list for this POD. Both April scales must show invoiced YES. If either is NO, stop.
2. Run the nightly total job for invoice date 17 Oct 2026.
3. Open the prepared total and look at April 2026.
**Expected test case results:**
1. April is `65` (`40` from the both-off scale plus `25` from the correction).
2. April is not `40`, not `25`, and not absent. TC-BE-8 stays the case where no other object exists, so the correction is skipped.

---

## References

- **Confluence:** [In the invoices to show consumption history for 13 month](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1224802306/In+the+invoices+to+show+consumption+history+for+13+month) (page ID 1224802306). Parent [Backlog](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/350191630/Backlog) (page ID 350191630) has no extra rules. No child pages.
- **Invoice JSON shape:** [invoice_template](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/102891624/invoice_template) (page ID 102891624). POD values on the document sit with detailed data. A similar add is [Templater: Invoice JSON - Add variable for the Provided power of the POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1185415207/Templater+Invoice+JSON+-+Add+variable+for+the+Provided+power+of+the+POD) (page ID 1185415207), field `DD.ProvidedPower`.
- **Scales API:** `POST /billing-by-scales` returns HTTP 201 in `BillingByScalesController`. Swagger `dev2_swagger.json` documents the same path as HTTP 200. Required body fields include `correction`, `override`, `dateFrom`, `dateTo`, `identifier`, `invoiceNumber`, `invoiceDate`, `saveRecordForIntermediatePeriod`, `saveRecordForMeterReadings`. `scaleForActiveElectricity` is on `POST /scales` (`ScalesRequest`), not on the scale line.
- **Invoiced flag:** insert writes `invoiced` NULL (`BillingByScaleJdbcRepository`). List query maps true to `YES` and anything else to `NO`. Cancellation sets the flag false (`InvoiceCancellationService`). No dev2 Java statement was found that sets `pod.billing_by_scale.invoiced` to true.
- **Document model:** `BillingRunDocumentDetailedDataModel` has `ProvidedPower` and no `podLast13MonthsConsumption`. `BillingRunDocumentModelImpl` is what template generation receives. `GET /invoice/detailed-data` returns HTTP 206 and is not that model.
- **Existing table:** `reporting.pod_consumption_cach` (`schema.sql`) already has `pod_id`, `type`, `period_from`, `period_to`, `total_volume`, `hours`. Penalty SQL treats `type = '1'` as scale and `type = '2'` as profile (`ProductContractDetailsRepository`). The story asks for a new table and also says the midnight job clears the POD table.

### Finding: The diagram leaves out rules the story still states
- **Type:** Doc gap
- **User impact:** High
- **Spec / doc says:** Story page ID 1224802306, version 12. AC-3: the midnight job clears the POD table and starts at the deployment period. AC-5: each row stores clock hours and does not split the quantity. AC-9: the invoice month is excluded (17 February 2027 includes 1 January 2026 through 31 January 2027). AC-11 points the historical cutoff at AC-4, which requires an already invoiced scale.
- **Code / runtime does:** The diagram `last 13 month consumption .drawio.svg` draws the new-table job without a clear step, without hours, and without a deployment-date gate. Its variable flow does not draw the invoice-month exclusion. Its historical cutoff box says active scales and does not repeat "already invoiced". It also says the job runs every night on a week day. The story does not say weekends are skipped.
- **Gap:** A tester cannot tell which document wins for those points. The existing cases follow the story and were not rewritten to match the diagram.
- **Recommendation:** PO clarification. Questions are listed in the chat and on the Excel sheet `Open questions`.

### Finding: Several scale combinations have no written result
- **Type:** Doc gap
- **User impact:** Medium
- **Spec / doc says:** [In the invoices to show consumption history for 13 month](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1224802306/In+the+invoices+to+show+consumption+history+for+13+month) (page ID 1224802306), AC-4 and AC-11. AC-11 defines both flags together, and correction only. It does not define override only on a historical month, a replacement plus a correction on the same month, or a header that crosses the deployment date.
- **Code / runtime does:** No job on dev2 implements these branches, so runtime does not resolve them either.
- **Gap:** A tester cannot mark those three combinations pass or fail from the story.
- **Recommendation:** PO clarification. They are listed on the Excel sheet `Not specified` and are not test cases.

### Not specified in the story — no pass result

These combinations were considered and are not test cases. The story does not say which result is correct.

1. Override is on and correction is off for a **historical** month. AC-11 defines both flags together, and correction only. It does not define override only on a historical month.
2. A historical month already has a table row, and also has both a replacement object and a correction. Story version 12 says the correction is added to an existing record or object, and the replacement is used instead of the historical row. It does not say the order, so the total could be the replacement plus the correction, or the historical row plus the correction. TC-BE-46 covers only the case with no historical row.
3. One scale header starts before `2026-06-01` and ends on or after `2026-06-01`. The story does not say whether that header is new-table data, historical data, or both.
4. The customer has no quantity in the window. The story does not say whether `podLast13MonthsConsumption` is `0` or absent.
5. AC-11 step 6 does not repeat the 13-month filter. AC-9 does. These cases follow AC-9 for the prepared total. The conflict is the Finding below.

### Finding: Invoice JSON field is not in the dev2 document model
- **Type:** Code↔Doc mismatch
- **User impact:** High
- **Spec / doc says:** [In the invoices to show consumption history for 13 month](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1224802306/In+the+invoices+to+show+consumption+history+for+13+month) (page ID 1224802306) — invoice JSON field `podLast13MonthsConsumption`, filled from a prepared table, not calculated inside the billing run.
- **Code / runtime does:** `BillingRunDocumentDetailedDataModel.java` exposes `ProvidedPower` and does not expose `podLast13MonthsConsumption`. phoenix-scheduler has no nightly job that builds this table or this total.
- **Gap:** A generated invoice on dev2 cannot show the 13-month POD consumption the story describes.
- **Recommendation:** Implement the table, both jobs, and the document field. These test cases check the story and will fail on current dev2 code.

### Finding: Story column list omits hours, and a similar table is already used by penalties
- **Type:** Doc gap
- **User impact:** High
- **Spec / doc says:** AC-1 lists `id`, `pod_id`, `type`, `period_from`, `period_to`, `total_volume`. AC-5 also requires hours. The main flow says the midnight job clears the POD table.
- **Code / runtime does:** `reporting.pod_consumption_cach` already stores those columns plus `hours`. `ProductContractDetailsRepository` sums `type = '1'` and `type = '2'` from that table for penalty actual consumption.
- **Gap:** Clearing or reusing that table would change penalty amounts. AC-1 does not mention the hours column that AC-5 requires.
- **Recommendation:** PO clarification — new table versus `reporting.pod_consumption_cach` — and add `hours` to the column list. Do not truncate the penalty cache.

### Finding: Customer column name and historical table name are not one statement
- **Type:** Doc gap
- **User impact:** Medium
- **Spec / doc says:** AC-7 field "Customer Number (CUST_NUM)". AC-8 copy SQL selects `CUST_IDENT` from `PHOENIX.T_MONTHLY_QTY`. The PostgreSQL table name for the historical store is not given.
- **Code / runtime does:** No entity or API was found for this historical table.
- **Gap:** A tester cannot know the target column or table from the story alone.
- **Recommendation:** Update Confluence with the schema, table, and whether the customer key is `customerNumber` or the personal identifier.

### Finding: 13-month window and per-version sum can disagree
- **Type:** Doc gap
- **User impact:** High
- **Spec / doc says:** AC-9 limits the value to the last 13 complete months, excluding the invoice month (example: invoice 17 February 2027 → 1 January 2026 through 31 January 2027). AC-11 step 6 reads new-table rows whose period end is inside POD activation, and step 7 sums those rows per contract version.
- **Code / runtime does:** No implementation to compare.
- **Gap:** Step 6 does not repeat the 13-month filter, so a long activation can pull months outside the window. Step 7 sums per version, so two overlapping versions count one period twice (TC-BE-14 expects `110` until the story changes).
- **Recommendation:** PO clarification. State that each period row is counted once, and only when its month is inside the 13-month window.

### Finding: JSON path and field casing are not specified
- **Type:** Doc gap
- **User impact:** Medium
- **Spec / doc says:** The field name is `podLast13MonthsConsumption`. It does not say root versus detailed data.
- **Code / runtime does:** Existing POD template fields are PascalCase on detailed data, for example `ProvidedPower` (`BillingRunDocumentDetailedDataModel`).
- **Gap:** Templates and tests can bind the value in the wrong place or the wrong case.
- **Recommendation:** PO clarification. These cases put the field on the POD detailed-data block, next to `ProvidedPower`, using the story's exact name.

### Swagger↔Code note
- **Type:** Swagger↔Code mismatch
- **User impact:** Low
- **Spec / doc says:** Cached `Cursor-Project/EnergoTS/copilot_data/dev2_swagger.json` response for `POST /billing-by-scales` and `PATCH /billing-run/start-billing` is HTTP 200.
- **Code / runtime does:** `BillingByScalesController.create` returns HTTP 201. `BillingRunController` start-billing, start-generating, and start-accounting return HTTP 202.
- **Gap:** A client that accepts only the Swagger status will treat a successful call as a failure.
- **Recommendation:** Fix Swagger. These cases expect the controller status.

Phoenix alignment for this run exited **2**. `phoenix-core`, `phoenix-core-lib`, `phoenix-billing-run`, and `phoenix-scheduler` are on `origin/dev2`. `phoenix-migration`, `phoenix-api-gateway`, and `mfe-poc-with-nx` have no `origin/dev2`. Swagger was the cached dev2 file. It was not refreshed, because this run does not create a Playwright spec.
