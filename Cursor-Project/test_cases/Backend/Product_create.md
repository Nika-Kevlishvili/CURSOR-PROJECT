# Energy product create – API coverage (Confluence Phoenix / Product Create)

**Jira:** N/A (specification source: Confluence)  
**Confluence:** Phoenix — [Product Create](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/9994309/Product+Create) (page id 9994309)  
**Type:** Feature / specification test derivation  
**Summary:** Backend test cases for creating an energy **Product** via the core API (`POST /products`), including validation, permissions, and regression risks for callers that consume product definitions.

**Scope:** Validates `ProductsController` create endpoint and `ProductService.create` behaviour: successful creation returns **HTTP 201** with a **numeric product id** (`Long`); failures return expected **HTTP 4xx** with field-scoped or business messages documented in code. Confluence text was not readable from automation (auth); expectations are anchored on Phoenix code.

---

## Cross-dependency context (Rule 35 handoff)

**Entry points:** `POST /products`, `ProductCreateRequest` / `BaseProductRequest` bean + class-level validators, `ProductService#create`.  
**Downstream / consumers:** Product contract flows (product + version selection), product list/filter, description endpoints, mass-import mappers referencing product ids.  
**What could break:** (1) Duplicate **non-individual** product **name** uniqueness; (2) invalid or inactive **foreign keys** (product type, group, term, term group, VAT, grid operator, sales channels, price components, files); (3) **term vs term-group** XOR rule; (4) **standard vs individual** product field rules (`IndividualProductValidator`); (5) **price component**–driven obligation for **income account** / **cost center** on the product payload; (6) **permission** split `PRODUCT_CREATE` vs `INDIVIDUAL_PRODUCT_CREATE`.

---

## Test data (preconditions)

Shared chain for API tests (reuse nomenclatures from environment fixtures where available — e.g. EnergoTS `global-setup` / `envVariables`; otherwise resolve ids via authenticated read APIs).

- **Environment:** Test (or named env under test); **Base URL:** core API (`BASE_URL`).
1. Obtain a bearer token for the main API (`tokenAuth` / project login helper) suitable for **`PRODUCT_CREATE`** on context **PRODUCTS** (record token for `Authorization: Bearer <jwt>`).
2. Resolve **`productGroupId`**: id of an **ACTIVE** product group (repository status ACTIVE — same rule as `ProductService.validateProductGroup`).
3. Resolve **`productTypeId`**: id of an **ACTIVE** product type.
4. Resolve **`electricityPriceTypeId`**: id of an **ACTIVE** electricity price type.
5. Resolve **`currencyId`** (optional on request but required when price amounts are populated): id of an **ACTIVE** currency.
6. Resolve **`termId`**: call `GET` `/products/available/terms` with valid query/page parameters; pick one **term id** that appears in results and remains allowed by backend (`findAllAvailableTermIdsForProduct` — invalid ids yield `basicSettings.termId-Term ... is not available for this product;`).
   - Alternative slice: resolve **`termGroupId`** via `GET` `/products/available/terms-groups` (ACTIVE term group).
7. Build **`productTerms`**: non-empty list of valid `BaseProductTermsRequest` objects (at least one row) satisfying `ProductTermsValidator` / `@Size` constraints on `BaseProductRequest`.
8. Resolve **`gridOperatorIds`**: non-empty list of **ACTIVE** grid operator ids — **unless** using **`globalGridOperator: true`** (when false/null, `@AssertTrue` requires non-empty grid operators — see `BaseProductRequest.isValidGridOperators`).
9. Resolve **`salesChannelIds`** OR set **`globalSalesChannel: true`** (and leave channel ids **null**) per **`IndividualProductValidator`** rules for **standard** (`isIndividual: false`) products.
10. Resolve **`salesAreasIds`** OR set **`globalSalesArea: true`** (and leave sales area ids **null**) — same validator rules.
11. Resolve **`segmentIds`** OR set **`globalSegment: true`** (and leave segment ids **null`) — same validator rules.
12. Choose **non-empty** sets for **`contractTypes`**, **`paymentGuarantees`**, **`purposeOfConsumptions`**, **`meteringTypeOfThePointOfDeliveries`**, **`voltageLevels`**, **`typePointsOfDelivery`** (enum values permitted by API).
13. For tests involving **attachments**, optionally execute `POST` `/products/upload-file` (`multipart/form-data`) with **`PRODUCT_CREATE`** permission; capture **`productFileIds`** returned for use in create payload.

---

## Backend Test Cases

### TC-BE-1 (Positive): Standard product created with term id only returns 201 and new product id

**Description:** Verify the happy path for a **standard** (`isIndividual: false`) product using exactly one of **termId** OR **termGroupId** (here **termId**), satisfying `ProductTermsValidator` and `IndividualProductValidator`.

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`isIndividual`:** `false`. **`customerIdentifier`:** omit or empty. **`globalVatRate`:** `false` and set **`vatRateId`** from an **ACTIVE** VAT rate id; or **`globalVatRate`:** `true` and **`vatRateId`:** `null` per validator.
3. Delta: **`termId`:** set from step 6; **`termGroupId`:** `null`.
4. Delta: Populate **`name`**, **`nameTransliterated`**, **`printingName`**, **`printingNameTransliterated`**, **`shortDescription`**, **`availableForSale`**, **`productGroupId`**, and flags/ids for sales channel/area/segment per standard-product rules.

**Test steps:**
1. Send **`POST`** **`/products`** with JSON body = full valid `ProductCreateRequest` built from preconditions (`Content-Type: application/json`, `Authorization` from step 1).
2. Parse response body as **numeric id** (`Long`).

**Expected test case results:** **HTTP 201 Created**. Response body is a single **positive long** equal to the new **product root id**. No error envelope. Subsequent **`GET`** **`/products/{id}`** (optional sanity) returns **`ProductDetailResponse`** containing the persisted header fields aligned with payload (spot-check **`name`**, **`productStatus`**, **`term`** linkage).

---

### TC-BE-2 (Positive): Standard product created with term group id only returns 201

**Description:** Same as TC-BE-1 but uses **`termGroupId`** instead of **`termId`** to satisfy XOR rule.

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`termGroupId`** set from available terms-groups; **`termId`:** `null`.
3. All other standard-product obligations same as TC-BE-1 (group, descriptions, globals for sales/VAT segments).

**Test steps:**
1. **`POST`** **`/products`** with valid body.

**Expected test case results:** **HTTP 201** with numeric **`Long`** id in body.

---

### TC-BE-3 (Positive): Individual product with customer identifier succeeds when caller has INDIVIDUAL_PRODUCT_CREATE

**Description:** Validates **`validateCreatePermissions`** branch for **`isIndividual: true`** and **`IndividualProductValidator`** (customer identifier mandatory; sales/global flags restricted).

**Preconditions:**
1. Apply Test data steps 1–12 using a JWT that includes **`INDIVIDUAL_PRODUCT_CREATE`** (not only **`PRODUCT_CREATE`**).
2. Delta: **`isIndividual`:** `true`. **`customerIdentifier`:** non-blank alphanumeric uppercase (**pattern** `^[A-Z\d]+$`, min length ≥ 1 — align with **`@Pattern`** / **`@Size`** on **`BaseProductRequest`**).
3. Delta: Omit or null **`availableForSale`**, **`availableFrom`**, **`availableTo`**; omit **`salesChannelIds`** / **`globalSalesChannel`** and same for sales areas and segments (**must not be sent as non-null / non-empty** per individual validator).
4. Delta: XOR **`termId`** **or** **`termGroupId`** as in TC-BE-1 / TC-BE-2.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 201**; body **`Long`** id. Product row stores **`customerIdentifier`** on **`Product`** (see **`ProductService.createProductDetailsInstance`**).

---

### TC-BE-4 (Negative): Neither term id nor term group id — Bean validation rejects before service

**Description:** Covers **`ProductTermsValidator`** rejection.

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`termId`:** `null` and **`termGroupId`:** `null`; all else minimally valid **except** XOR fix would fail intentionally.

**Test steps:**
1. **`POST`** **`/products`** with offending payload.

**Expected test case results:** **HTTP 400 Bad Request**. Response aggregates validation failures; body **contains** substrings **`[Term Group ID] or [Term ID] must be defined`** (exact template from **`ProductTermsValidator`**).

---

### TC-BE-5 (Negative): Both term id and term group id supplied

**Description:** Second branch of **`ProductTermsValidator`** (mutually exclusive).

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`termId`** and **`termGroupId`** **both non-null**.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with message **`Either [Term Group IDs] or [Term IDs] must be defined`** (both paths emitted in validator template).

---

### TC-BE-6 (Negative): Standard product violates individual-only field rules — customer identifier present

**Description:** **`IndividualProductValidator`** forbids **`customerIdentifier`** when **`isIndividual`:** `false`.

**Preconditions:**
1. Apply Test data steps 1–12 for standard product slice.
2. Delta: **`isIndividual`:** `false`; set **`customerIdentifier`** to non-blank **while** failing the “must be blank for standard product” rule.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with **`basicSettings.customerIdentifier-Customer identifier must be blank for standard product;`** in validation output.

---

### TC-BE-7 (Negative): Standard product omits mandatory name fields

**Description:** Validates required **`name`** / **`nameTransliterated`** for standard products (`IndividualProductValidator`).

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`name`** or **`nameTransliterated`** blank / omitted while **`isIndividual`:** `false`.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with **`basicSettings.name-Name must not be blank;`** or **`nameTransliterated-Transliterated Name must not be blank;`**.

---

### TC-BE-8 (Negative): Global VAT true but vatRateId still sent

**Description:** Validates VAT pairing rule from **`IndividualProductValidator`** for standard products.

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`globalVatRate`:** `true`; **`vatRateId`:** non-null (invalid pairing).

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with **`basicSettings.vatRateId-Vat Rate ID must be null while [globalVatRate] is true;`**.

---

### TC-BE-9 (Negative): Global sales channel true but concrete channel ids list present

**Preconditions:**
1. Apply Test data steps 1–12 standard slice.
2. Delta: **`globalSalesChannel`:** `true`; **`salesChannelIds`:** non-null / non-empty list.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** containing **`salesChannelIds-Sales Channel IDs must be null while [globalSalesChannel] is true;`**.

---

### TC-BE-10 (Negative): Non-global grid operator requires at least one grid operator id — AssertTrue validator

**Preconditions:**
1. Apply Test data steps 1–12 with **`globalGridOperator`:** `false` **or** `null`; **`gridOperatorIds`:** `null` or empty list.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with **`basicSettings.gridOperatorIds-Grid Operator IDs array must have at least one object when globalGridOperator not chosen;`** ( **`@AssertTrue`** on **`BaseProductRequest`**).

---

### TC-BE-11 (Negative): POD type GENERATOR paired with consumer balancing product id — excluded combination

**Description:** Validates balancing-id vs POD-type consistency from **`IndividualProductValidator`** (subset of single POD type logic).

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`typePointsOfDelivery`:** singleton set **`GENERATOR`**; set **`consumerBalancingProductNameId`** non-null (invalid combination).

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **HTTP 400** with **`consumerBalancingProductNameId-ConsumerBalancingProductNameId must be null, because pod GENERATOR type is selected;`**.

---

### TC-BE-12 (Negative): Duplicate product name for non-individual product — service-layer chained error

**Description:** Matches **`validateProductNameUniqueness`** — **risk** regressions affect contract catalog UX.

**Preconditions:**
1. Apply Test data steps 1–12 and successfully create baseline product **`POST`** **`/products`** (record **`name`** value **N**).
2. Delta: New payload identical enough to reuse **N** while **`isIndividual`:** **`false`** and **`validateProductNameUniqueness`** runs (not suppressed for individuals — code only skips uniqueness when **`isIndividual`:** **`true`** per **`ProductService#create`** guard).

**Test steps:**
1. **`POST`** **`/products`** again with **`name`** = **N** (second create).

**Expected test case results:** **HTTP 4xx** envelope used by **`EPBChainedExceptionTriggerUtil`** (typically **`400`** with structured **`message`**). Body/message **includes** substring **`basicSettings.name-Product with same name`** and echoes **N** (`ProductService.validateProductNameUniqueness`). **No second persisted** product with duplicate name among ACTIVE statuses.

---

### TC-BE-13 (Negative): Invalid product type id — repository miss

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`productTypeId`:** `-1` **or** id known **not** to exist among ACTIVE types.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **`400`**/`422`-class client error carrying **`basicSettings.productTypeId-Product Type with presented ID […] not found;`** (**`validateProductType`**).

---

### TC-BE-14 (Negative): Term id unknown — not found — service validation

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`termId`:** bogus id **`999999999`**, **`termGroupId`:** `null`.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** Chained **`400`** **`message`** includes **`basicSettings.termId-Term with presented ID [999999999] not found;`**.

---

### TC-BE-15 (Negative): Term exists but not in available-for-product whitelist

**Preconditions:**
1. Apply Test data steps 1–12.
2. Delta: **`termId`:** ACTIVE term id **not** in **`termsRepository.findAllAvailableTermIdsForProduct()`** (discover candidate via DB or QA-known fixture).

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **`basicSettings.termId-Term with id [x] is not available for this product;`**.

---

### TC-BE-16 (Negative): Price components lacking income account — product omits income account number

**Description:** Validates **`validatePriceComponentFields`** when aggregated price components (direct ids + those from groups) omit income account numbers.

**Preconditions:**
1. Apply Test data steps 1–13.
2. Delta: **`priceComponentIds`** or **`priceComponentGroupIds`** reference stored price components whose **`incomeAccountNumber`** is blank in DB (`PriceComponent`).
3. Delta: Omit / blank **`incomeAccountNumber`** on **`ProductCreateRequest`**.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **`400`** **`message`** includes **`Income account number is mandatory in product as there are price components without income account number:`** suffix listing offending component **`name (id)`** pairs (`ProductService.validatePriceComponentFields`).

---

### TC-BE-17 (Negative): Price components omit cost centre on product payload — symmetrical rule

**Preconditions:**
1. Align with TC-BE-16 but trigger **`costCenterControllingOrder`** branch (leave blank on product while components missing cost centre in DB).

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **`400`** **`message`** includes **`Cost center and controlling order is mandatory in product as there are price components`** … (suffix lists components even if typo “income” appears in duplicate message template per current code — tester asserts **presence** of rule text and component list).

---

### TC-BE-18 (Negative): Caller lacks PRODUCT_CREATE and INDIVIDUAL_PRODUCT_CREATE — permission denied before body validation

**Preconditions:**
1. Obtain JWT **without** both **`PRODUCT_CREATE`** **and** **`INDIVIDUAL_PRODUCT_CREATE`** on **PRODUCTS** context (principal with neither create permission).
2. Delta: Minimal otherwise-valid body irrelevant — expect authz failure.

**Test steps:**
1. **`POST`** **`/products`** with any JSON body using token from precondition 1.

**Expected test case results:** **`HTTP 403 Forbidden`** (**`PermissionValidator`** — align with **`ClientException`** `ACCESS_DENIED` if reached via **`validateCreatePermissions`** for wrong individual flag subset). Exact body matches global error contract (assert **403** and **ACCESS_DENIED** or permission text **`You do not have permission to create`** from **`validateCreatePermissions`** when token passes gateway but misses fine-grained permission).

---

### TC-BE-19 (Negative): Wrong create permission branch — token has only PRODUCT_CREATE but payload is individual

**Description:** Validates **`PRODUCT_CREATE`** **without** **`INDIVIDUAL_PRODUCT_CREATE`** for **`isIndividual: true`** path.

**Preconditions:**
1. JWT with **`PRODUCT_CREATE`** only (no **`INDIVIDUAL_PRODUCT_CREATE`** — requires tailored test user provisioning).
2. Delta: **`isIndividual`:** `true`; valid individual payload aside from permission.

**Test steps:**
1. **`POST`** **`/products`**.

**Expected test case results:** **`HTTP 403`** with message **`You do not have permission to create an individual product.`** (**`validateCreatePermissions`**, **`PermissionEnum.INDIVIDUAL_PRODUCT_CREATE`** branch).

---

## References

- Confluence: Phoenix — Product Create (`https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/9994309/Product+Create`) — primary business spec (content not fetched; code used as corroborating SoT).
- Code: ```65:73:Cursor-Project/Phoenix/phoenix-core/src/main/java/bg/energo/phoenix/controller/product/product/ProductsController.java``` — `POST /products` → **`HttpStatus.CREATED`**.
- Code: ```168:241:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/product/product/ProductService.java``` — **`create(ProductCreateRequest)`** orchestration + uniqueness + validations.
- Code: **`ProductTermsValidator`**, **`IndividualProductValidator`**, **`BaseProductRequest`** (bean validation).
- Automation bridge: **`Endpoints.product`** → **`products`** (`Cursor-Project/EnergoTS/fixtures/constants/endpoints.ts`); use **`GeneratePayload.productAndServices`** where applicable before **`expect(response).CheckResponse()`**.
