# Service Contract versioning – lifecycle & billing resolution (PDT-2599)

**Jira:** [PDT-2599](https://oppa-support.atlassian.net/browse/PDT-2599) (Phoenix Delivery — **PDT**)  
**Type:** Customer Feedback (Jira)  
**Summary:** Exhaustive backend (API + billing) verification for Service Contract **version numbering**, **validity start/end chaining** for Signed versions, **Not Valid** version rules, **status transitions**, duplicate-date validation (**EN + BG**), dropdown ordering semantics, edit constraints for the **first version**, timeline **recalculation** after inserts and date changes, and **billing** resolution so that only **Signed** contract versions drive invoice amounts (including the **PDT-2599** multi-version regression).

**Scope:** **`POST /service-contract`**, **`PUT /service-contract/{id}?versionId=`**, **`PUT /service-contract/status-update/{id}`**, **`GET /service-contract/{id}`**, plus billing run and invoice read paths per **refreshed** `Cursor-Project/config/swagger/dev/swagger-spec.json` and EnergoTS `Endpoints`. **Functional story detail** is in `Cursor-Project/docs/Service_Contract_Versioning_extracted.docx` lineage / `Service_Contract_Versioning_extracted.txt` when present; **Jira ticket** carries reproduction (custom field / GE steps HTML), attachments (**Service Contract Versioning\***.docx, screenshots), and discussion comments. **Standard Jira `environment` field:** **PROD** (where the issue was observed); **automation alignment** for this workspace uses **DEV** API/Swagger unless you explicitly target another environment.

**Jira evidence (not only description):**  
- **Summary (Jira):** *BE - Service Contract - The system doesn't consider the contract version status.*  
- **`description`:** empty in API payload — primary narrative is in **custom fields** (e.g. reproduce / GE blocks in rendered HTML), **attachment** docx files, and **comments** (e.g. Dev billing-run links, expected vs actual quantity screenshots, linked child issues for FE/DB layers).  
- **Linked issues:** multiple linked delivery items (examples from issue payload: FE/DB/BE gaps on contract version status and per-piece billing).  
- **Reproduction pattern (GE / steps field):** five versions — V1–V2 **Signed**, V3 **Cancelled**, V4–V5 **Ready**; second billing used **Ready V4** price/qty instead of **Signed V2** for the invoice date — *only Signed versions must be invoiced.*

**Phoenix alignment (mixed state — DEV):** `switch-phoenix-branches.ps1 -Environment dev` completed with **exit code `2`** (2026-05-06): **`mfe-poc-with-nx`** and **`phoenix-migration`** → **`missing-remote`** (`origin/dev` not found). Other repos **ok** or **already-aligned** (`phoenix-core`, `phoenix-core-lib`, `phoenix-ui`, `phoenix-billing-run`, etc.). Conclusions from **`phoenix-core-lib`** below reflect **DEV-aligned** sources; gaps in missing repos should be cross-checked in CI or remote.

**Cross-dependency (Rule 35 — summary):** Version lifecycle in **`ServiceContractService`** (update, `updateStatus`, `getContractVersions`); ordering via **`ServiceContractDetailsRepository.findServiceContractVersionsOrderedByStatusAndStartDate`**. Billing / contract-detail selection for overtime paths uses native SQL in **`ServiceContractDetailsRepository`** that resolves **`service_contract.contract_details`** by **`max(start_date)`** with **`start_date <= current_date`** without **`contract_version_status = SIGNED`** filtering in the subquery — a plausible root cause for “latest row by date” picking **Ready** over **Signed** (verify at runtime). **What could break:** mass import parity, invoice locking, UI ordering vs API list, PER_PIECE / periodical / one-time / interim billing calculators, DB views used by billing.

---

## Test data (preconditions)

**Environment:** DEV API — `Cursor-Project/config/swagger/dev/swagger-spec.json` (server URL in spec). Authenticate every request with **Bearer** (same pattern as EnergoTS global setup).

**At a glance:** Customer → communication channel IDs (BILLING + CONTRACT) → commercial Service (+ version) → optional POD → contract version type id → interest rate id → `third-tab-fields` (term / invoice term / formula rows) → **`POST /service-contract`** → optional billing setup + **`POST /billing-run`**.

**Naming:** All JSON field names and enums in the create body **must** match the spec (`ServiceContractCreateRequest`, `ServiceContractBasicParametersCreateRequest`, `ServiceContractServiceParametersCreateRequest`, `ServiceContractAdditionalParametersRequest`). Below uses **logical** names; copy exact names from Swagger when building JSON.

**Shared data-creation chain (numbered — drop steps 10–11 when a TC does not need billing):**

1. **Customer** — Call `POST /customer` with a minimal valid request body per schema `CustomerRequest` / create schema in DEV spec (e.g. ACTIVE private or legal customer, valid identifier, required addresses/contacts as the API demands). From the response (and if needed `GET /customer/{customerId}`), store **`customerId`**, **`customerVersionId`**, and **`customerDetailsId`** (the id of the **active customer detail** row used for communications — use the field name present in the API response).

2. **Billing communication channel** — Call `GET /customer/communication-data/list` with query object `CommunicationDataListRequest`: `customerDetailsId` from step 1, `communicationDataType` = **`BILLING`**. From the returned array (`CustomerCommunicationDataResponse`), pick one row and store its **`id`** as **`communicationDataForBilling`** (this is the integer required in `basicParameters.communicationDataForBilling`).

3. **Contract communication channel** — Repeat step 2 with `communicationDataType` = **`CONTRACT`**. Store selected **`id`** as **`communicationDataForContract`**.

4. **Commercial service** — Call `POST /services` with a valid `CreateServiceRequest` (required nested objects per spec: at minimum **`basicSettings`**, **`priceSettings`**, **`additionalSettings`**). The response body is the new **`serviceId`** (`long`). Call `GET /services/{serviceId}` and from the response take **`serviceVersionId`** for the **ACTIVE** service version (use the field name and structure from that service view schema).

5. **POD (only if needed)** — If the scenario requires POD execution (e.g. `serviceParameters.podIds` is non-empty per your service/setup), create a POD with `POST /pod` (ACTIVE, linked to the same customer as step 1 per spec) and store **`podId`** for `podIds` later.

6. **Contract version type(s)** — Call `GET /contract-version-types` with a valid `NomenclatureItemsBaseFilterRequest` query (page/size/status filters per spec). Pick one **ACTIVE** version type from the page and store its **`id`**. Set **`basicParameters.contractVersionTypes`** = `[ thatId ]` (array must not be empty).

7. **Interest rate** — Resolve **`interestRateId`** required by `additionalParameters`: e.g. `GET /interest-rate/getDefault` or `GET /interest-rate/list` and pick one allowed id (per spec / business rules).

8. **Third-tab catalogue for service parameters** — Call `GET /service-contract/third-tab-fields` (`ServiceContractThirdPageFields`). From the response:
   - **`contractTermId`** — `id` of one element of **`serviceContractTerms`**.
   - **`invoicePaymentTermId`** — `id` of one element of **`invoicePaymentTerms`**.
   - **`invoicePaymentTerm`** — required `integer` (`int32`) on create; set to the **`value`** field of the **same** `invoicePaymentTerms` row you chose for `invoicePaymentTermId` (`ServiceContractInvoicePaymentTermsResponse.value`).
   - **`paymentGuarantee`** — enum string; use e.g. **`NO`** if the scenario does not need deposit/bank guarantee (otherwise set amounts/currencies per spec).
   - **`contractFormulas`** — non-empty array of `PriceComponentContractFormula`: at least one object `{ "formulaVariableId": <id from formulaVariables[]>, "value": <number within schema min/max> }`.

9. **Create Service Contract** — Call `POST /service-contract` with body `ServiceContractCreateRequest`:
   - **`basicParameters`** — Set all **required** fields from `ServiceContractBasicParametersCreateRequest`: `serviceId`, `serviceVersionId`, `customerId`, `customerVersionId`, `contractStatus`, `contractStatusModifyDate`, `contractType`, `detailsSubStatus`, `contractVersionStatus`, `contractVersionTypes` (ids from **Test data step 6** — contract version types list), `communicationDataForBilling` / `communicationDataForContract` (from **Test data steps 2–3**), `contractTermUntilAmountIsReachedCheckbox`, plus optional fields your scenario needs (`currencyId`, dates, `signInDate`, etc. — only if required by validation for that path).
   - **`serviceParameters`** — Set **required** fields from step 8: `contractTermId`, `invoicePaymentTermId`, `invoicePaymentTerm`, `paymentGuarantee`, `contractFormulas`. Add `podIds: [ podIdFromStep5 ]` when step 5 applies.
   - **`additionalParameters`** — `{ "interestRateId": <from step 7> }` (required key on `ServiceContractAdditionalParametersRequest`; other keys optional per schema).
   - Store **`contractId`**, first version **`versionId`**, **`startDate`**, **`contractVersionStatus`**, and any **`endDate`** returned.

10. **Billing prerequisites (billing TCs only)** — Create or link billing profile / energy data using the **exact** `POST` paths and bodies from DEV Swagger for this service-contract + POD combination so a billing run is **eligible** (field names from spec, not from this story).

11. **Billing run (billing TCs only)** — Call `POST /billing-run` with the run type and period/event date from Swagger (`one-time`, `periodical`, `per-piece`, `interim` — use operation + enum names from spec). Tie **billing date** to the Signed version window under test. Assert invoice creation or validation error per TC.

**Further versions / edits:** add or change versions with **`PUT /service-contract/{id}?versionId=`** and/or **`PUT /service-contract/status-update/{id}`** as required by each TC, then re-fetch with **`GET /service-contract/{id}`** before assertions.

---

## Backend Test Cases

**Playwright parity:** Each `TC-BE-N` heading matches the `test()` title in `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts` (`[PDT-2599] TC-BE-N: …`). Automation must use `test.step`, `await expect(response).CheckResponse()` where success responses apply, Swagger-validated payloads from `config/swagger/dev/swagger-spec.json`, and **no `test.beforeAll`** for preconditions (helper + inline steps only). Manual execution follows the same API sequence with Bearer auth.

### TC-BE-1 (Positive): BE - Service Contract - The system doesn't consider the contract version status

**Description:** Regression anchor for PDT-2599: after creating a service contract with the standard Signed first version, the contract detail exposes **one** Signed row whose **startDate** matches **creationDate** and whose **endDate** is open (null/empty).

**Preconditions:**
1. Complete **Test data** steps **1–9** (customer, communications, commercial service, version types, interest rate, `GET /service-contract/third-tab-fields` with required **`serviceDetailId`** query from the service, then `POST /service-contract`) so the first version is **Signed** per spec.
2. Persist **`contractId`** from the create response.

**Test steps:**
1. `GET /service-contract/{contractId}` (default version context per spec).
2. Parse JSON: read `basicParameters.creationDate`, `versions[]`; assert exactly one version row; row status is Signed; `startDate` equals creation date string; `endDate` is absent, null, or empty string per model.

**Expected test case results:** **HTTP 200**; body satisfies the assertions above (`CheckResponse`-style success).

**References:** PDT-2599 (customer feedback — Signed-only billing); `PDT-2599-be-service-contract-version.spec.ts`.

---

### TC-BE-2 (Positive): POST /service-contract/list — created contract appears when searching by contract number

**Description:** Listing with **`searchBy: CONTRACT_NUMBER`** and **`prompt`** equal to the contract’s number returns a page that contains the created **`contractId`**.

**Preconditions:**
1. Complete **Test data** steps **1–9**; store **`contractId`**.

**Test steps:**
1. `GET /service-contract/{contractId}` → **`CheckResponse`** → read **`basicParameters.contractNumber`**.
2. `POST /service-contract/list` with body `{ page, size, searchBy: "CONTRACT_NUMBER", prompt: <contractNumber> }` per **`ServiceContractTableSearchRequest`** in DEV Swagger.
3. Assert response **HTTP 200**; `content` array includes an element whose **`id`** equals **`contractId`**.

**Expected test case results:** Created contract discoverable via list search by contract number.

**References:** Spec TC-BE-2.

---

### TC-BE-3 (Negative): GET /service-contract/{id} — non-existent id returns 400 or 404

**Description:** Requesting an astronomically unlikely / non-existent contract id yields a client or not-found class status without leaking success body semantics.

**Preconditions:**
1. Valid bearer token only (no contract creation required).

**Test steps:**
1. `GET /service-contract/99999999999999999` (or equivalent placeholder id from automation).
2. Assert **HTTP 400** or **HTTP 404**.

**Expected test case results:** Status is **400** or **404** only.

**References:** Spec TC-BE-3.

---

### TC-BE-4 (Negative): GET /service-contract/{id} — invalid versionId returns error

**Description:** `GET` with a non-existent **`versionId`** query rejects with **400** or **404**.

**Preconditions:**
1. Complete **Test data** steps **1–9**; store **`contractId`**.

**Test steps:**
1. `GET /service-contract/{contractId}?versionId=9999999999999`.
2. Assert **HTTP 400** or **HTTP 404**.

**Expected test case results:** Error status as above; no successful read of a fake version.

**References:** Spec TC-BE-4.

---

### TC-BE-5 (Positive): GET /service-contract/{id} — first version row has versionId 1

**Description:** The first row in **`versions`** carries **`versionId`** === **1** for a newly created contract.

**Preconditions:**
1. Complete **Test data** steps **1–9**.

**Test steps:**
1. `GET /service-contract/{contractId}` → **`CheckResponse`**.
2. Read **`versions[0].versionId`**; assert equals **1**.

**Expected test case results:** **HTTP 200**; ordinal **1** on first row.

**References:** Spec TC-BE-5.

---

### TC-BE-6 (Negative): GET /service-contract/third-tab-fields — missing serviceDetailId rejected

**Description:** Calling **`GET /service-contract/third-tab-fields`** without the required **`serviceDetailId`** (or with it omitted against spec `required`) yields an error response.

**Preconditions:**
1. Authenticated client only.

**Test steps:**
1. `GET /service-contract/third-tab-fields` with **no** `serviceDetailId` query parameter.
2. Assert **HTTP 400**, **404**, or **500** (environment-dependent classification; matches current automation tolerance).

**Expected test case results:** Non-2xx; request does not return a normal catalogue body.

**References:** Spec TC-BE-6.

---

### TC-BE-7 (Negative): duplicate Valid start date on new version → 4xx (EN message)

**Description:** Saving a **new Signed** version whose **startDate** duplicates an existing version’s start date is rejected with **HTTP ≥ 400**; English error text mentions duplicate start / already exists (pattern tolerant).

**Preconditions:**
1. Complete **Test data** steps **1–9**; read **`creationDate`** from detail (first Signed start).

**Test steps:**
1. Load edit detail (`GET` for edit shape as in automation).
2. `PUT /service-contract/{contractId}?versionId=1` with payload equal to edit detail but **`savingAsNewVersion: true`**, **`startDate`** = existing **`creationDate`**, **`contractVersionStatus: SIGNED`** (field names per Swagger).
3. Assert status **≥ 400**; body/text matches `/contract version already has provided start date|already exists/i`.

**Expected test case results:** Duplicate start rejected with visible validation messaging.

**References:** Spec TC-BE-7; bilingual story strings for broader localization tests elsewhere.

---

### TC-BE-8 (Positive): three Signed versions — end dates = day before next start

**Description:** With three **Signed** rows and strictly increasing starts, inner Signed rows end the calendar day **before** the next Signed **startDate**; last Signed remains open-ended.

**Preconditions:**
1. Complete **Test data** steps **1–9**.
2. Add **second** and **third** Signed versions via successive **`PUT /service-contract/{id}?versionId=`** with **`savingAsNewVersion: true`**, new **`startDate`** values offset from **`creationDate`** (automation uses ~+400 and ~+800 days pattern), **`contractVersionStatus: SIGNED`**, using latest `versionId` as `params.versionId` for each append.

**Test steps:**
1. After each append, **`GET /service-contract/{id}`** ( **`CheckResponse`** on happy paths).
2. Assert three Signed rows; for each non-last Signed, `endDate = next.startDate - 1 day`; last `endDate` null/empty.

**Expected test case results:** Timeline arithmetic holds; **HTTP 200** on valid transitions.

**References:** Spec TC-BE-8; story scenario A.

---

### TC-BE-9 (Positive): insert Signed between two Signed — neighbour end dates recalc

**Description:** Inserting a **middle** Signed version between two existing Signed rows recloses predecessor **endDate** to the day before the inserted start and preserves ordering of outer starts.

**Preconditions:**
1. Establish two Signed versions with increasing starts (e.g. from TC-BE-8 partial chain or dedicated setup).
2. Add **third** Signed whose **startDate** falls **strictly between** first and second Signed starts.

**Test steps:**
1. `GET /service-contract/{id}`; assert middle insertion: predecessor ends **insertedStart − 1 day**; inserted row ends day before former second Signed start; chain contiguous.

**Expected test case results:** Recalculated ends match story **scenario B** intent.

**References:** Spec TC-BE-9.

---

### TC-BE-10 (Positive): versionId ordinals 1..n after out-of-order chronological inserts

**Description:** Logical **`versionId`** assignments remain **1..n** monotonic as new rows are added even when **startDate** ordering is not insertion order.

**Preconditions:**
1. Build a multi-version chain using **`PUT`** new-version saves where **`startDate`** may be chosen “between” prior rows per automation (`pickLatestLogicalVersionId` pattern).

**Test steps:**
1. After each mutation, collect all **`versions[].versionId`**; assert set equals **`{1..n}`** without duplicates for current **n**.

**Expected test case results:** Monotonic integer ids without gaps for current count.

**References:** Spec TC-BE-10.

---

### TC-BE-11 (Positive): add non-first Draft — distinct start, open end

**Description:** A **non-first** **Draft** version can be added with a **startDate** distinct from all existing rows and shows **open end** / Not Valid semantics.

**Preconditions:**
1. Complete **Test data** steps **1–9** (first Signed exists).
2. Choose new **`startDate`** after **`creationDate`** and not equal to any existing version start.

**Test steps:**
1. `PUT` with **`savingAsNewVersion: true`**, **`contractVersionStatus: DRAFT`**, chosen **`startDate`**, targeting latest `versionId` param per automation.
2. `GET`; assert new row **Draft**; **`endDate`** open; **`versionId`** incremented.

**Expected test case results:** **HTTP 200** on create path; Draft row stored as expected.

**References:** Spec TC-BE-11.

---

### TC-BE-12 (Negative): Draft duplicate start date → 4xx

**Description:** Attempting to add **Draft** with **startDate** equal to an existing version’s start fails with **HTTP ≥ 400**.

**Preconditions:**
1. Contract from steps **1–9** with known **`creationDate`** / existing version dates.

**Test steps:**
1. `PUT` new version with **`contractVersionStatus: DRAFT`** and **duplicate** **`startDate`**.
2. Assert **HTTP ≥ 400**.

**Expected test case results:** Rejected; no duplicate Draft row committed.

**References:** Spec TC-BE-12.

---

### TC-BE-13 (Negative): second Signed start earlier than first → 4xx

**Description:** Adding a **second Signed** whose **`startDate`** is **before** the **first Signed** **`startDate`** is rejected.

**Preconditions:**
1. First Signed on **`creationDate`** from steps **1–9**.

**Test steps:**
1. Attempt **`PUT`** new Signed with **`startDate` < `creationDate`** (or strictly before first Signed per automation).
2. Assert **HTTP ≥ 400**.

**Expected test case results:** Server-side guard prevents backwards Signed insert.

**References:** Spec TC-BE-13.

---

### TC-BE-14 (Positive): edit middle Signed start — chain stays coherent

**Description:** Moving a **middle** Signed **`startDate`** within allowed bounds recomputes neighbour **`endDate`** values without gaps or overlaps.

**Preconditions:**
1. At least three **Signed** rows from prior **`PUT`** chain.

**Test steps:**
1. `PUT /service-contract/{id}?versionId=<middleSignedLogicalId>` **in-place** (`savingAsNewVersion: false`) with adjusted **`startDate`** within valid region per automation.
2. `GET`; assert full Signed chain coherence (day-before-next rules).

**Expected test case results:** **HTTP 200**; coherent timeline.

**References:** Spec TC-BE-14.

---

### TC-BE-15 (Negative): first Signed cannot change start date (in-place PUT)

**Description:** **In-place** edit on **`versionId` 1** must **not** change **`startDate`**; API returns **≥ 400** or leaves dates unchanged per automation assertions.

**Preconditions:**
1. Single-version contract from steps **1–9**.

**Test steps:**
1. `PUT …?versionId=1` with **`savingAsNewVersion: false`** and altered **`startDate`** vs stored value.
2. Assert failure **or** verify `GET` shows **unchanged** `startDate` (per spec behaviour — automation expects rejection path).

**Expected test case results:** First Signed start immutability enforced.

**References:** Spec TC-BE-15.

---

### TC-BE-16 (Negative): status-update v1 to CANCELLED rejected

**Description:** `PUT /service-contract/status-update/{id}` cannot move **first** version to **CANCELLED** (or disallowed transition)—**HTTP ≥ 400** or stable read proving no transition.

**Preconditions:**
1. Contract with **first** version **Signed** (`versionId` **1**).

**Test steps:**
1. Call **`status-update`** endpoint with body requesting **CANCELLED** (or equivalent disallowed target) for **`versionId` 1** per Swagger `ServiceContractEditStatusRequest`.
2. Assert rejection per automation (`expect` on status / error).

**Expected test case results:** First Signed remains non-cancelled.

**References:** Spec TC-BE-16; `ServiceContractService.updateStatus` guards in Phoenix.

---

### TC-BE-17 (Positive): non-first Draft → READY via status-update (when chain allows)

**Description:** A **non-first** **Draft** may transition to **READY** via **`status-update`** when chain rules permit; **`GET`** shows updated status.

**Preconditions:**
1. Contract with **Draft** row at **logical version ≥ 2** (add Draft after Signed baseline per automation).

**Test steps:**
1. `PUT /service-contract/status-update/{contractId}` with **`versionId`** of Draft row and target **`contractVersionStatus: READY`** (exact field names per Swagger).
2. If **HTTP 2xx**, `GET` reflects **READY**; if environment returns **4xx**, document as chain constraint (automation tolerates failure with attachment).

**Expected test case results:** Either allowed transition succeeds or documented business rejection — automation records outcome.

**References:** Spec TC-BE-17.

---

### TC-BE-18 (Positive): GET versions — Signed group sorted by start; new version ids monotonic

**Description:** **`versions`** payload lists **Signed** rows ordered by **`startDate`** ascending after mixed operations; newly added rows receive higher **`versionId`** than prior max.

**Preconditions:**
1. Multi-version chain mixing Signed / Not Valid operations as in TC-BE-8–11.

**Test steps:**
1. `GET /service-contract/{id}`; filter Signed rows; assert sorted starts; **`versionId`** sequence strictly increases with add operations historically.

**Expected test case results:** Ordering + monotonic ids visible in read model.

**References:** Spec TC-BE-18.

---

### TC-BE-19 (Negative): validation error surfaces message text (sample from duplicate-date)

**Description:** Duplicate-date **`PUT`** returns a body/text snippet that is non-empty and matcher-friendly (regex on duplicate phrasing).

**Preconditions:**
1. Same setup as **TC-BE-7** duplicate attempt.

**Test steps:**
1. Perform duplicate new-version **`PUT`**; read **`.text()`**; assert pattern match for duplicate-start semantics (case-insensitive).

**Expected test case results:** Error payload is inspectable (no silent empty failures).

**References:** Spec TC-BE-19.

---

### TC-BE-20 (Positive): after Signed date edit, Draft row stays open-ended

**Description:** Editing a **middle Signed** **`startDate`** must **not** incorrectly close a coexisting **Draft** tail row as if it were Signed — Draft retains **open end** / not-valid window semantics.

**Preconditions:**
1. Chain with multiple **Signed** plus trailing **Draft** (`savingAsNewVersion` Draft on latest).

**Test steps:**
1. Edit middle Signed **`startDate`** (in-place) within valid bounds.
2. `GET`; locate **Draft** row; assert **`endDate`** still open / status still Not Valid per assertions in automation.

**Expected test case results:** Draft openness preserved alongside Signed recalculations.

**References:** Spec TC-BE-20.

---

### TC-BE-21 (Negative): PUT with unknown versionId → 4xx, GET unchanged

**Description:** **`PUT /service-contract/{id}?versionId=<unknown>`** with syntactically valid body returns **≥ 400**; a subsequent **`GET`** shows **unchanged** contract snapshot vs pre-call.

**Preconditions:**
1. Contract from steps **1–9**; capture baseline JSON or checksum of **`versions`**/critical fields.

**Test steps:**
1. `PUT …?versionId=9999999999999` with minimal edit payload.
2. Assert **≥ 400**; `GET` baseline equals prior state for version collection.

**Expected test case results:** Safe failure — no partial orphan version.

**References:** Spec TC-BE-21.

---

### TC-BE-22 (Negative): new Signed start strictly before first version start → 4xx

**Description:** Same class as TC-BE-13 — explicit guard when **`savingAsNewVersion: true`** attempts Signed row **before** genesis **`creationDate` / first Signed start**.

**Preconditions:**
1. Baseline Signed first version.

**Test steps:**
1. `PUT` append Signed with `startDate` **strictly before** first Signed start.
2. Assert **≥ 400**.

**Expected test case results:** Rejected.

**References:** Spec TC-BE-22.

---

### TC-BE-23 (Negative): invalid contractVersionStatus in PUT body → client error

**Description:** **`PUT`** with non-enum garbage string in **`basicParameters.contractVersionStatus`** returns **400**, **415**, or **422** (client error class).

**Preconditions:**
1. Contract from steps **1–9**; load edit detail.

**Test steps:**
1. Mutate payload **`basicParameters.contractVersionStatus`** to e.g. **`NOT_A_STATUS_ENUM`** while keeping `savingAsNewVersion: false`.
2. `PUT …?versionId=1`; assert status in **{400,415,422}**.

**Expected test case results:** Invalid enum rejected.

**References:** Spec TC-BE-23.

---

### TC-BE-24 (Positive): PER_PIECE — first Signed window — invoices issued (distinct drivers v1/v2)

**Description:** With **two Signed** slices and **distinct** PER_PIECE drivers (quantities / formula graph per **`applyPdt2599BillingTwoSignedDriverGraph`**), a **PER_PIECE** billing run whose **`invoiceDate` / `taxEventDate`** falls in the **first** Signed window creates **≥1** draft invoice; optional invoice **`GET`** exposes linked **`versionId` 1** when API surfaces linkage; money fingerprint finite.

**Preconditions:**
1. **Test data** steps **1–10** for PER_PIECE-eligible service contract + **`runPdt2599SignedServiceContractBillingChain`** (interim/service/billing profile wiring per fixtures).
2. Apply divergent driver graph for **v1** vs **v2** Signed rows.

**Test steps:**
1. Pick **`w1Pick`** billing date in open interval between first and second Signed starts (helper `pickValidBillingDateFromBillingCommon`).
2. `POST /billing-run` with payload from **`buildPdt2599BillingRunPayload`** including **`['PER_PIECE']`**, **`invoiceDate`**/**`taxEventDate`** = **`w1Pick`**.
3. Poll billing run until **≥1** draft invoice; `GET invoice?id=…` → **`CheckResponse`**; assert money fingerprint numeric; if `extractBillingInvoiceLinkedVersionId` non-null → **1**.

**Expected test case results:** **HTTP 2xx** billing; draft invoices exist; linkage/fingerprint assertions per spec.

**References:** Spec TC-BE-24; PDT-2599 Signed-only economics.

---

### TC-BE-25 (Positive): PER_PIECE — second Signed window — billingDate aligns second driver fingerprints

**Description:** Second-window **`invoiceDate`** inside **second Signed** span yields invoices whose optional linked **`versionId`** is **2** and finite money fingerprint.

**Preconditions:** Same chain as TC-BE-24 including two Signed driver graph.

**Test steps:**
1. Choose **`w2Pick`** via **`pickValidBillingDateFromBillingCommonWithinAccountingPeriod`** on **[secondSignedStart, secondSignedStart+60d]**.
2. `POST /billing-run` with **`alignPerPieceSecondSignedWindowBillingPayload`** alignment when required.
3. Poll drafts; **`GET invoice`**; assert fingerprint; optional linked version **2**.

**Expected test case results:** Second Signed window billing correctness.

**References:** Spec TC-BE-25.

---

### TC-BE-26 (Positive): PER_PIECE — billing after Draft v3 start — links latest Signed v2

**Description:** With **Signed v1, v2** and **Draft v3** starting after v2, a billing date **after v3.start** still bills **latest Signed (v2)** — invoice optional linkage **`versionId` 2**, not Draft v3.

**Preconditions:**
1. PER_PIECE billing chain + two Signed graph.
2. Add **Draft v3** with distinct quantity; align calendar with **accounting period** end if needed (shifts v2 start — see fixture errors).

**Test steps:**
1. Pick billing date **`> v3.start`** inside allowed AP.
2. `POST /billing-run` (aligned payload); poll; `GET invoice`; assert optional **`versionId` 2**; fingerprint finite.

**Expected test case results:** Draft does not hijack Signed resolution for billing date after Draft start.

**References:** Spec TC-BE-26; core PDT-2599 defect class.

---

### TC-BE-27 (Positive): OVER_TIME_ONE_TIME — first Signed window — invoices issued (distinct drivers v1/v2)

**Description:** Same as TC-BE-24 but billing model **`OVER_TIME_ONE_TIME`** and one-time formula driver graph (`PDT2599_OVER_TIME_ONE_TIME_*` drivers).

**Preconditions:** **`runPdt2599SignedServiceContractBillingChain`** with **`'OVER_TIME_ONE_TIME'`**; apply **`applyPdt2599BillingTwoSignedDriverGraph`** with one-time driver ids.

**Test steps:** Mirror TC-BE-24 with `['OVER_TIME_ONE_TIME']` payload branch; optional linked **`versionId` 1**.

**Expected test case results:** Draft invoices; fingerprint ok; linkage optional **1**.

**References:** Spec TC-BE-27.

---

### TC-BE-28 (Positive): OVER_TIME_ONE_TIME — second Signed window — billing aligns second driver

**Description:** Second Signed window billing for **OVER_TIME_ONE_TIME**; optional linkage **2**.

**Preconditions:** TC-BE-27 style chain.

**Test steps:** Mirror TC-BE-25 with OVER_TIME_ONE_TIME payloads.

**Expected test case results:** Second-window correctness.

**References:** Spec TC-BE-28.

---

### TC-BE-29 (Positive): OVER_TIME_ONE_TIME — billing after Draft v3 start — links latest Signed v2

**Description:** Draft v3 after two Signed slices; billing date after Draft start still resolves **Signed v2** for OVER_TIME_ONE_TIME.

**Preconditions:** Same structural preconditions as TC-BE-26 but **`runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_ONE_TIME')`**.

**Test steps:** Parallel to TC-BE-26 with one-time payload builders.

**Expected test case results:** Optional invoice `versionId` **2** when exposed.

**References:** Spec TC-BE-29.

---

### TC-BE-30 (Positive): OVER_TIME_PERIODICAL — first Signed window — invoices issued (distinct drivers v1/v2)

**Description:** **OVER_TIME_PERIODICAL** variant of TC-BE-24.

**Preconditions:** Chain with **`'OVER_TIME_PERIODICAL'`** and periodical driver graph.

**Test steps:** `buildPdt2599BillingRunPayload` with `['OVER_TIME_PERIODICAL']`; first window date pick.

**Expected test case results:** Drafts + fingerprint; optional linkage **1**.

**References:** Spec TC-BE-30.

---

### TC-BE-31 (Positive): OVER_TIME_PERIODICAL — second Signed window — billing aligns second driver

**Description:** Periodical second Signed window (mirror TC-BE-25).

**Preconditions:** TC-BE-30 chain complete to two Signed.

**Test steps:** Second-window billing with periodical payload alignment.

**Expected test case results:** Optional linkage **2**.

**References:** Spec TC-BE-31.

---

### TC-BE-32 (Positive): OVER_TIME_PERIODICAL — billing after Draft v3 start — links latest Signed v2

**Description:** Periodical Draft v3 regression (mirror TC-BE-26/29).

**Preconditions:** **`'OVER_TIME_PERIODICAL'`** chain with Draft v3.

**Test steps:** Billing date after v3 start; assert linkage **2** when field present.

**Expected test case results:** Signed v2 wins over Draft for billing.

**References:** Spec TC-BE-32.

---

### TC-BE-33 (Positive): INTERIM — billing after Draft v3 start — links latest Signed v2 (interim amounts differ per version)

**Description:** INTERIM contract with **Signed v1/v2** interim advance amounts **A ≠ B** and **Draft v3** with distinct interim **`interimAdvancePaymentsRequests`**; billing after v3 start still ties invoice to **Signed v2** when linkage fields exist.

**Preconditions:**
1. **`runPdt2599SignedServiceContractBillingChain(fixtures,'INTERIM')`**; resolve **`Responses.interim[0]`** id.
2. Add second Signed slice; **`pdt2599PutServiceContractVersionInterimAdvanceRequests`** for version **1** and **2** with **PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED** vs **V2_SIGNED**.
3. Add Draft v3 with **PDT2599_INTERIM_ADVANCE_AMOUNT_V3_DRAFT** in **`serviceParameters.interimAdvancePaymentsRequests`** (calendar/AP guards per fixtures).

**Test steps:**
1. `POST /billing-run` **INTERIM** payload with **`invoiceDate`/`taxEventDate` > v3.start**, **`maxEndDate` removed**, **`accountingPeriodId`** resolved for billing date, second-window alignment helper as in spec.
2. Poll drafts; `GET invoice`; fingerprint finite; optional **`versionId` 2**.

**Expected test case results:** Latest Signed governs despite later Draft row (INTERIM amounts differ per version).

**References:** Spec TC-BE-33.

---

### TC-BE-34 (Positive): INTERIM — first Signed window — invoices issued (distinct interim amounts v1/v2)

**Description:** Billing date **strictly before** second Signed start (typically **`v2Start - 1 day`**) with distinct interim amounts on v1/v2 yields draft invoices; optional linkage **`versionId` 1**.

**Preconditions:** INTERIM chain; second Signed added; **`pdt2599PutServiceContractVersionInterimAdvanceRequests`** for v1 and v2 with **55.55** vs **92.25** pattern (exact literals from spec constants).

**Test steps:**
1. `w1Pick = v2Start - 1 day`; guard inside **open accounting period**.
2. Build **`POST /billing-run`** INTERIM payload (**drop `maxEndDate`**); run; poll; `GET invoice`; assert fingerprint; optional linkage **1**.

**Expected test case results:** First Signed interim slice billed.

**References:** Spec TC-BE-34.

---

### TC-BE-35 (Positive): INTERIM — second Signed window — billingDate in v2 slice (distinct interim amounts v1/v2)

**Description:** Pick **`w2Pick`** in **second Signed** window with same A/B interim amounts; optional invoice linkage **`versionId` 2**; **`alignPerPieceSecondSignedWindowBillingPayload`** applied as in automation.

**Preconditions:** Same as TC-BE-34 through interim PUTs.

**Test steps:** Second-window INTERIM billing with AP-aligned payload.

**Expected test case results:** Second Signed interim slice billed; optional linkage **2**.

**References:** Spec TC-BE-35.

---

## References

- **Jira:** [PDT-2599](https://oppa-support.atlassian.net/browse/PDT-2599) — summary, GE/repro custom field, attachments, comments, linked issues.
- **Story / extract:** `Cursor-Project/docs/Service_Contract_Versioning_extracted.txt` (when present) and attachment docx on ticket.
- **Code (DEV-aligned read):** `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/contract/service/ServiceContractService.java` (version rules); `.../repository/contract/service/ServiceContractDetailsRepository.java` (version ordering; contract detail selection for billing — review native queries for **Signed** filter gaps).
- **Playwright bridging (1:1):** Backend file defines **TC-BE-1 … TC-BE-35** with the **same titles** as `test()` in `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts`. Use `test.step`, `CheckResponse`, helper preconditions (**no `test.beforeAll`** for setup) — `Cursor-Project/config/playwright_generation/playwright instructions/`; fixtures: `pdt-2599-service-contract.fixtures.ts`.
