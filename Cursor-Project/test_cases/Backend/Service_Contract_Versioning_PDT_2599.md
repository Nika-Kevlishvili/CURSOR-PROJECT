# Service Contract versioning – lifecycle & billing resolution (PDT-2599)

**Jira:** PDT-2599 (reference key only — requirements taken from linked story extract, **not** from Jira body)  
**Type:** Feature  
**Summary:** Exhaustive backend (API + billing) verification for Service Contract **version numbering**, **validity start/end chaining** for Signed versions, **Not Valid** version rules, **status transitions**, duplicate-date validation (**EN + BG**), dropdown ordering semantics, edit constraints for the **first version**, timeline **recalculation** after inserts and date changes, and **billing** resolution by effective date against Signed-only windows.

**Scope:** Primary source is `Cursor-Project/docs/Service_Contract_Versioning_extracted.txt`. Endpoints referenced from cross-dependency context: **`POST /service-contract`**, **`PUT /service-contract/{id}?versionId=`**, **`PUT /service-contract/status-update/{id}`**, **`GET /service-contract/{id}`**. Billing assertions use the generic billing pipeline for **one-time**, **periodical**, **per piece**, and **interim** runs; exact paths and payloads must be taken from **refreshed** `Cursor-Project/config/swagger/dev/swagger-spec.json` and EnergoTS `Endpoints` — see Playwright Swagger rules.

**Phoenix alignment (mixed state — DEV):** Local Phoenix clones were switched with **`switch-phoenix-branches.ps1` exit code `2`** (partial success). Repos **`mfe-poc-with-nx`** and **`phoenix-migration`** were reported as **`missing-remote`** for `origin/dev`. Behaviour described below is **requirements from the story**; implementation traces in those repos may not be available locally — reconcile with Swagger and DEV runtime.

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

### TC-BE-1 (Positive): Create Service Contract initializes first Signed version with creation-date start and open end

**Description:** On create, first version uses **‘Valid/Signed by both sides’**, start date aligns with **contract creation date**, end is **open end**, and version number is **1** (or smallest supported by API response schema).

**Preconditions:**
1. Complete prerequisite chain through **step 9** from Test data (steps 1–8 prepare IDs and catalogue rows; step 9 calls `POST /service-contract`) with a minimal valid payload per **DEV** Swagger.
2. Note **HTTP 2xx**, persist `contractId` and returned version collection.

**Test steps:**
1. Call **`GET /service-contract/{contractId}`** (`contractId` from **Test data step 9**).
2. Inspect response: first **Signed/Valid** version; **startDate** equals **creation date** semantics from create response/body fields (field names per Swagger).

**Expected test case results:** Response shows **exactly one** Signed/Valid-compatible version initially; **`endDate`/equivalent absent or sentinel for open-end** per contract model; **`CheckResponse`**-style success (**HTTP 200**).

**References:** Story — Service Contract Creation AC.

---

### TC-BE-2 (Positive): Duplicate start date prevented with EN + BG error payload or messages

**Description:** Attempting another version whose **Validity Start Date** equals an existing version’s date returns validation error texts in **English and Bulgarian**.

**Preconditions:**
1. Complete chain through **step 9**; ensure contract has version **V1** with known `startDate` **D**.
2. Prepare `POST /service-contract`-child version **or** add-version API per Swagger (same `contractId`, new row) with **duplicate** start **D**.

**Test steps:**
1. Submit duplicate start date creation.
2. Parse error structure (nested `messages`, `localizedMessage`, or array per API).

**Expected test case results:** **HTTP 4xx** validation; payload or message bundle includes EN: **`Contract version already has provided start date!`** and BG: **`Версията на договора вече има зададена начална дата!`** (exact matching per backend contract).

**References:** Story — duplicate date strings.

---

### TC-BE-3 (Positive): Signed versions sorted ascending — end dates recalculated (`nextSignedStart − 1 day`)

**Description:** Signed versions ordered by **Validity Start Date** ascending yield **computed** end dates: each non-last Signed gets **last day before next Signed start**; last Signed remains **open end**.

**Preconditions:**
1. Build contract with **three Signed versions** whose starts are strictly increasing (via multiple `POST`/`PUT` additions per Swagger); ensure no gaps in business rules—use dates **01.01.2024**, **01.01.2025**, then insert **01.04.2025** as new last Signed (**story scenario A**) per allowed API sequencing.

**Test steps:**
1. After each mutate, **`GET /service-contract/{id}`**.
2. For each Signed version, assert **effectiveEndDate = nextSigned.startDate − 1 calendar day** except last (**open end**).

**Expected test case results:** Matches story example (**V2** closes **31.03.2025** when **V3** starts **01.04.2025**); **HTTP 200** on GET.

**References:** Story — scenarios A/B.

---

### TC-BE-4 (Positive): Insert Signed between two Signed — predecessor closes day before inserted start

**Description:** Inserting **middle** Signed version recalculates neighbours: predecessor ends **inserted−1**, inserted ends **nextSigned−1**, later chain intact.

**Preconditions:**
1. Start from two Signed rows **V1: 01.01.2024**, **V2: 01.01.2025–open**.
2. Add **Signed V3 start 01.10.2024** (between first and second) using correct create/update API sequence.

**Test steps:**
1. `GET /service-contract/{id}` post-change.
2. Assert **V1 ends 30.09.2024**, **V3 spans 01.10.2024–31.12.2024**, **V2** still **starts 01.01.2025** open-ended.

**Expected test case results:** Timeline matches story **scenario B**.

---

### TC-BE-5 (Positive): Numeric version numbering monotonic regardless of chronological order

**Description:** Each new version increments **Version Number** (next integer starting at **1**) even when new **Validity Start Date** is **between** older rows.

**Preconditions:**
1. Create sequential versions mixing **later** then **between** chronological inserts while observing returned `versionNumber`/`ordinal` fields from API.

**Test steps:**
1. Record `versionNumber` after **each** create.
2. Assert strict **+1** from previous greatest number.

**Expected test case results:** No duplicate ordinal; order in response may differ — numbering still monotonic increasing.

---

### TC-BE-6 (Positive): Non-first **Not Valid** (Draft / Ready / Cancelled) — unique start, not before contract creation date, open end semantics

**Description:** Draft/Ready/Cancelled versions enforce **distinct** validity start versus all versions and **cannot** start before contract **creation**.

**Preconditions:**
1. Service contract exists (**step 9**). Note **creationDate** field from API.
2. Create **Draft** version with **`startDate = creationDate`** (allowed equality per “not earlier than creation” wording — if API forbids equality, adjust assertion to Swagger).

**Test steps:**
1. `POST`/append **Draft** with valid distinct date after creation.
2. `GET —` verify **`end`/open-end** semantics and label payload shows **Not Valid** branch.

**Expected test case results:** Version stored with **Draft** status enum; **`end`** open-ended; **HTTP 200/201**.

**References:** Story — Not Valid subsection.

---

### TC-BE-7 (Negative): Not Valid start date clashes with existing version Start Date

**Description:** Duplicate start across **Draft/Ready/Cancelled** must reuse same duplicate-date rule (**EN + BG**).

**Preconditions:**
1. Contract has any version at date **D**. Prepare **Draft** with same **D**.

**Test steps:**
1. Attempt create.
**Expected test case results:** **4xx**; localization includes **same EN/BG texts** as Valid duplicate rule (unless spec differentiates).

---

### TC-BE-8 (Negative): Valid **Signed** Validity Start violates “not earlier than **previous Signed** Start” rule on edit (non-first)

**Description:** For **non-first** Valid version, **`PUT /service-contract/{id}?versionId=`** shifting start **before** immediate predecessor Signed boundary must fail (**EN + BG**).

**Preconditions:**
1. Two **Signed** versions with starts **t1 ` t2`; target **versionId`** = second Signed.

**Test steps:**
1. `PUT …?versionId={v2}` with **startDate — t1`** (exclusive violation per story wording “must not be earlier than the previous version’s Start Date” interpreted as chronological predecessor in Signed timeline — clarify with Swagger if ambiguous).

**Expected test case results:** **HTTP 400/422**; clear validation; bilingual messages.

---

### TC-BE-9 (Positive): Editing non-last Signed moves end dates — chain coherent

**Description:** **`PUT`** changing **Validity Start Date** on mid Signed recomputes full Signed chain identical to relational rules above.

**Preconditions:**
1. Three-tier Signed baseline as TC-BE-4 result.

**Test steps:**
1. Move middle Signed start later/earlier within legal span.
2. `GET`; assert predecessors/successors’ **effectiveEndDates** coherent.

**Expected test case results:** No overlaps; contiguous windows; bilingual errors if illegal.

---

### TC-BE-10 (Negative): First Signed version date update forbidden via API

**Description:** **`PUT`** must reject changing **Validity Start Date** for **ordinal 1**.

**Preconditions:**
1. Fresh contract (`version 1`) only.

**Test steps:**
1. `PUT …?versionId={first}` with altered `startDate`.

**Expected test case results:** **4xx**, no persisted change; bilingual message.

---

### TC-BE-11 (Negative): First Signed cannot transition to Cancelled (`status-update`)

**Description:** Story requires first version **remain** Signed; cancelling must be blocked server-side even if caller sends **`PUT /service-contract/status-update/{id}`** with Cancelled-like target enum.

**Preconditions:**
1. Single-version contract (**first Signed**).

**Test steps:**
1. Submit status transition Draft/Cancelled/Ready per matrix — specifically **toward Cancelled** for first Signed.

**Expected test case results:** **Rejected** (**4xx**); state unchanged after `GET`.

**References:** Story — first version immutable status.

---

### TC-BE-12 (Positive): Allowed status transitions for non-first rows follow documented matrix (`status-update`)

**Description:** For each **`From`** row in story **Status Update** narrative (excluding corrupted OCR table), transitions marked allowed succeed; **disallowed** transitions fail (**EN + BG**).

**Preconditions:**
1. Contract with Draft version **non-first**.

**Test steps:**
1. For **Draft → Ready** (if allowed narrative): **`PUT /service-contract/status-update/{id}`** with body per Swagger referencing `targetVersionId`/status enums.
2. Repeat for each allowed edge; negate one disallowed combo.

**Expected test case results:** Allowed 2xx where story grants `✓`; disallowed yields 4xx and stable read model.

*(If matrix incomplete in artifact, annotate actual vs expected blocker in swagger gap note.)*

---

### TC-BE-13 (Positive): Boundary — one version’s **Start Date** may equal another’s **Computed End Date**

**Description:** Explicit story allowance: neighbouring windows may touch without overlap (inclusive/exclusive end handled by **−1 day** rule plus boundary equality).

**Preconditions:**
1. Two Signed chain where **V1.end + 1 day = V2.start** after recalculation.

**Test steps:**
1. `GET`; assert end/start alignment matches rule (no gap >1 day / no overlap).

**Expected test case results:** Consistent sequential coverage.

---

### TC-BE-14 (Positive): Dropdown payload orders Valid first ascending, then Not Valid ascending

**Description:** **`GET`** (or `/list`/nested collection) exposes versions for UI ordering; verify **sorted** segments (if API exposes explicit arrays, assert order; otherwise assert canonical order field like `sortIndex` ascending within groups).

**Preconditions:**
1. Mix Signed + Draft + Cancelled dates out of chronological creation order.

**Test steps:**
1. `GET /service-contract/{id}` parse `versions`.
2. Assert **all Signed** grouped first by **start ascending**, then remainder **Draft/Ready/Cancelled** by **start ascending**.

**Expected test case results:** Ordering matches narrative.

---

### TC-BE-15 (Positive): Persist new Signed version retains focus-equivalent linkage (same `contractId`; target `versionId` latest)

**Description:** Backend response after create/update should reference **latest created** logical version identifiers so UI/API consumer need not wildcard redirect.

**Preconditions:**
1. Baseline Two Signed.

**Test steps:**
1. **`POST`/append Signed** newest.
2. Read **create response** vs **follow-up GET** — latest version id aligns.

**Expected test case results:** New version id surfaced; **`GET`** default “current Signed” aligns with maximal start among Signed (**open-end** bearer).

*(UI assertion continues in Frontend file.)*

---

### TC-BE-16 (Negative): Generic validation errors include bilingual payloads

**Description:** Negative tests **TC-BE-2, TC-BE-7, TC-BE-8, TC-BE-10, TC-BE-11** collectively prove **Story global note** “Every system validation error must include Bulgarian localization (EN + BG).” Optionally sample malformed JSON body (**400**) for EN+BG scaffolding.

**Preconditions:**
1. Induce deterministic validation error subset.

**Test steps:**
1. Inspect error envelope fields per Swagger (`errors[].locale`, nested maps, etc.).
**Expected test case results:** Both locales surfaced or composite message string pattern defined by platform standard.

---

### TC-BE-17 (Positive): One-time billing resolves effective Signed version strictly by billing event date window

**Description:** Billing run resolves **Applicable Service Contract Signed version** if **billingEventDate ∈ [start,end]** (end omitted = open-end). Returned invoice line contract snapshot fields must originate from matched version.

**Preconditions:**
1. Complete prerequisites including service-contract billing linkage + energy data (**Test data steps 10–11**).
2. Service Contract with **two disjoint Signed spans** bridging event date purposely only second window.

**Test steps:**
1. Create & execute **`POST /billing-run`** (**one-time** flavor) referencing contract with **explicit event date landing only in outer Signed window**.
2. Read generated invoice `/invoice` payloads (Swagger path) asserting term/price linkage mapping to matched version id.

**Expected test case results:** Correct version id reflected; **HTTP 200** billing success path.

---

### TC-BE-18 (Negative): One-time billing no eligible Signed span returns validation error (no invoicing)

**Description:** Story: if **billing date lacks Signed coverage**, invoicing suppressed with **validation error**.

**Preconditions:**
1. Service Contract intentionally only **Draft/Ready/Cancelled** versions covering window around event date (no overlapping Signed ranges).

**Test steps:**
1. Execute billing one-time referencing that contract & date intersecting Draft-only row.

**Expected test case results:** **4xx/contract-specific error** blocking invoice issuance; bilingual if standard.

---

### TC-BE-19 (Positive): Periodical billing segments per-period resolution obeys Signed windows spanning multiple spans

**Description:** Across multi-month period, resolver picks **distinct** Signed versions segment-wise (when boundary crossed mid-cycle).

**Preconditions:**
1. Signed **V_a** ending mid-period, Signed **V_b** starting next day contiguous.

**Test steps:**
1. Billing run **periodical** covering contiguous months crossing boundary.
2. Inspect multiple invoice splits or proportional components per internal API observability (**Swagger** exposes structure).

**Expected test case results:** Each sub-segment derives terms from respective Signed snapshot.

---

### TC-BE-20 (Positive): Piece-based billing uses version effective on billing date terms

**Description:** Mirrors one-time semantics with **quantity/piece driver** enumeration per billing configuration.

**Preconditions:**
1. Eligible metering + piece tariff configuration per DEV data.

**Test steps:**
1. **`POST /billing-run`** (**per-piece** type) aligning date ∈ correct Signed boundary.

**Expected test case results:** Effective version matches deterministic expectation.

---

### TC-BE-21 (Positive): Interim billing uses interim date/period Signed resolution

**Preconditions:**
1. Interim-capable billing template & contract link.

**Test steps:**
1. Trigger interim billing; assert analogous resolution.

**Expected test case results:** Same exclusion of Not Valid statuses.

---

### TC-BE-22 (Regression – Positive): Mutation triggers **recalculate Signed ends**, **Not Valid stay open-ended**

**Description:** After batch **PUT** altering Signed starts, **`GET`** shows Signed recomputed closure; Draft rows keep **Open end**.

**Preconditions:**
1. Mix Signed & Draft concurrently.

**Test steps:**
1. Legal Signed date mutation (TC-BE-9 style).
**Expected test case results:** Draft versions unchanged **`end`** open semantics.

---

### TC-BE-23 (Regression – Positive): Mass import parity — imported Service Contract conforms to versioning rules after import finalize

**Description:** Exercise **mass import** (FileUpload pathway / importer endpoint per EnergoTS domain) concluding with identical **GET timeline** validations as REST-authored contract.

**Preconditions:**
1. Build vertical/horizontal importer template populated with sequential Signed & Not Valid rows per column spec (reference `massImportGenerator.ts` patterns but **Swagger-first** verification).

**Test steps:**
1. Upload import; finalize job.
**Expected test case results:** Post-commit `GET /service-contract/{id}` aligns with authoring equivalence within rounding.

**References:** Cross-dependency risk **mass import parity**.

---

### TC-BE-24 (Negative): Unknown `versionId` on `PUT /service-contract/{id}?versionId=` returns client error without partial mutation

**Description:** Validates safe failure when querying or targeting a non-existent logical version (**typo UUID / stale concurrency**).

**Preconditions:**
1. Contract exists (**step 9**). Prepare random non-assigned surrogate `versionId`.

**Test steps:**
1. `PUT /service-contract/{contractId}?versionId=<unknown>` minimal valid swagger body unrelated to duplication.

**Expected test case results:** **HTTP 404/400** stable classification; **`GET`** afterwards unchanged relative to precondition snapshot (**no orphaned row**).

---

### TC-BE-25 (Negative): Attempt to create **second** Signed with start **before Contract Creation Date** disallowed where story forbids inserting before genesis

**Description:** Narrative distinguishes first creation date anchors; supplemental rows must obey **minimum start ≥ creation** semantics for Draft; Signed additions must obey earlier vs later relative rules excluding first anchor mutation.

**Preconditions:**
1. Known `creationDate`.

**Test steps:**
1. Submit Signed or Draft with **`startDate < creationDate`**.

**Expected test case results:** **4xx**.

---

### TC-BE-26 (Positive): Swagger contract validation — enumerated status values only

**Description:** Smoke check each status field aligns with **enum strings** extracted from Swagger (prevents Silent enum drift regressions impacting timeline classification).

**Preconditions:**
1. Local copy post-refresh `swagger-spec.json`.

**Test steps:**
1. Grep schemas for **Service Contract version** enums; mutate illegal string via raw JSON.

**Expected test case results:** Rejected (**400**) before persistence.

---

## References

- **Story extract:** `Cursor-Project/docs/Service_Contract_Versioning_extracted.txt`
- **Jira key:** PDT-2599 (**traceability — not requirements source for this document**)
- **Cross-dependency:** Service Contract version lifecycle, billing resolution, duplicate/localization/regression/matrix risks
- **Playwright bridging:** Align automated steps with `test.step`, `CheckResponse`, helper-based preconditions (**no `beforeAll`**) — `Cursor-Project/config/playwright_generation/playwright instructions/`
