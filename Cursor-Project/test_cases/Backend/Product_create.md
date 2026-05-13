# Product create – API coverage (Confluence page 9994309)

**Jira:** N/A (Confluence-only scope — Phoenix **Product Create**, page ID **9994309**, space Phoenix)  
**Type:** Feature / regression (product master data)  
**Summary:** Backend test cases for creating energy **products** via **`POST /products`**, covering permissions, validation (`ProductCreateRequest` / `BaseProductRequest`), term vs term group rules, individual vs standard products, linked entities, and regression surfaces called out in cross-dependency analysis.

**Scope:** Validates `ProductsController.create` → `ProductService.create` behaviour: HTTP **201 Created** with `Long` product id (see `ProductsController.java` — Spring `ResponseEntity` with `HttpStatus.CREATED`). The dev OpenAPI file lists response **200** for the same operation; **code overrides spec** for status code. Preconditions follow Playwright dependency order: **Terms → Price components → Product**, with JWT auth. Field names and enums for requests must be taken from **`Cursor-Project/config/swagger/{env}/swagger-spec.json`** (`ProductCreateRequest` schema) after refresh — not from this document alone.

---

## Test data (preconditions)

Shared setup for backend API tests against the target API environment (set **`BASE_URL`** / auth to that environment).

1. **Authenticate** using the EnergoTS pattern: obtain JWT via main API login (same as `fixtures/login.ts` / `tokenAuth()`) so **`Authorization: Bearer \<token\>`** is sent on all requests.
2. **Resolve nomenclature IDs** either from **`envVariables.json`** (after global setup) **or** by creating entities, depending on framework policy — if tests must be self-contained per **`precondition-data-creation.instructions.md`**, create:
   - **Term:** `POST /terms` with payload from **`GeneratePayload.productAndServices.term()`** (store returned `id` in **`Responses.terms`**).
   - **Electricity price component:** `POST /price-component` (or the path in **`Endpoints.priceComponent`**) with **`GeneratePayload.productAndServices.electricity()`**; persist id in **`Responses.priceComponent`**.
3. **Pick valid foreign keys** for the product payload: **`productTypeId`**, **`electricityPriceTypeId`**, **`productGroupId`** (if used), **`vatRateId`** when **`globalVatRate`** is false — use ids from active nomenclature consistent with `ProductService` validation (ACTIVE entities only). Document the chosen source (created vs envVariables) per test run.
4. For tests that attach **price component IDs** to the product: ensure each referenced price component has **income account** and **cost center** populated **or** set **`incomeAccountNumber`** / **`costCenterControllingOrder`** at product level (`validatePriceComponentFields` in `ProductService`).
5. For **individual** products (**`isIndividual: true`**): use a user/token with **`INDIVIDUAL_PRODUCT_CREATE`**; include **`customerIdentifier`** and satisfy **`IndividualProductValidator`** rules (pattern / globals / balancing names vs POD types per request).
6. For **term group** scenarios: obtain a valid **`termGroupId`** (ACTIVE term group) instead of **`termId`** — mutually exclusive (**`ProductTermsValidator`** — exactly one of `termId` / `termGroupId`).
7. **Optional linked data:** penalties, terminations, interim advance payments, related entities, **`productFileIds`** (files created via product file upload flow), **`templateIds`**, **`collectionChannelIds`** — create or resolve valid ACTIVE ids before invoking **`POST /products`**.

---

## Backend Test Cases

### TC-BE-1 (Positive): Standard product – minimal valid create with `termId`

**Description:** Verify a non-individual product can be created when mandatory Swagger fields and business rules are satisfied and a valid **`termId`** is supplied (no **`termGroupId`**).

**Preconditions:**
1. Complete **Test data** steps 1–3 for a user with **`PRODUCT_CREATE`** (not only individual).
2. Ensure **`termId`** references an ACTIVE term allowed for products (aligned with `TermsRepository.findAllAvailableTermIdsForProduct` semantics).

**Test steps:**
1. Build **`ProductCreateRequest`** JSON with **`isIndividual`:** false or omitted, **`productTerms`** (at least one element per schema), **`contractTypes`**, **`paymentGuarantees`**, POD/voltage/metering enums, **`printingName`**, **`printingNameTransliterated`**, **`productStatus`**, **`termId`** set, **`termGroupId`** omitted, valid **`productTypeId`**, **`electricityPriceTypeId`**, VAT/global flags consistent with **`vatRateId`**, and accounting fields satisfying PC vs product rules.
2. Send **`POST /products`** with the JSON body.
3. Optionally **`POST /products/list`** with a filter that returns the new id and assert the product appears.

**Expected test case results:** Response status **201 Created**; response body is numeric **product id** (`Long`). No server error payload. Downstream list can find the product when permissions allow.

**References:** `ProductsController.java` (`POST` mapping); `ProductService.create`; `Cursor-Project/config/swagger/dev/swagger-spec.json` → `ProductCreateRequest`.

---

### TC-BE-2 (Positive): Standard product – create with `termGroupId` (no `termId`)

**Description:** Verify create succeeds when **`termGroupId`** is provided and **`termId`** is absent (term group path).

**Preconditions:**
1. Same as TC-BE-1, plus an ACTIVE **`termGroupId`** valid for the operation.

**Test steps:**
1. Build payload with **`termGroupId`** only.
2. **`POST /products`**.

**Expected test case results:** **201 Created**; new product id returned.

---

### TC-BE-3 (Negative): Validation – both `termId` and `termGroupId` present

**Description:** Request must not include both identifiers (`ProductTermsValidator` / XOR rule).

**Preconditions:**
1. Authentication and nomenclature ids available (steps 1–3 Test data).

**Test steps:**
1. Build an otherwise valid payload with **both** **`termId`** and **`termGroupId`** set.
2. **`POST /products`**.

**Expected test case results:** **4xx** validation error; product must not be created (no persisted id usable for contracts).

---

### TC-BE-4 (Negative): Validation – neither `termId` nor `termGroupId`

**Description:** Exactly one of term vs term group is required.

**Preconditions:**
1. Same as TC-BE-3.

**Test steps:**
1. Omit both **`termId`** and **`termGroupId`** from an otherwise complete payload.
2. **`POST /products`**.

**Expected test case results:** **4xx** client/validation error; no creation.

---

### TC-BE-5 (Positive): Individual product – allowed with `INDIVIDUAL_PRODUCT_CREATE`

**Description:** **`isIndividual: true`** succeeds when the caller has **`INDIVIDUAL_PRODUCT_CREATE`** and service-level `validateCreatePermissions` passes.

**Preconditions:**
1. JWT for a user with **`INDIVIDUAL_PRODUCT_CREATE`** on PRODUCTS context.
2. Valid **`customerIdentifier`**, balancing/POD-type fields per **`IndividualProductValidator`** for the scenario under test.

**Test steps:**
1. Build individual product payload per **`BaseProductRequest`** / **`IndividualProductValidator`** (globals for sales channel/area/segment as required).
2. **`POST /products`**.

**Expected test case results:** **201 Created**; id returned.

---

### TC-BE-6 (Negative): Individual product – token only has `PRODUCT_CREATE`

**Description:** Service rejects individual create when permission is wrong (`validateCreatePermissions`).

**Preconditions:**
1. JWT with **`PRODUCT_CREATE`** but **without** **`INDIVIDUAL_PRODUCT_CREATE`** (if your test harness can isolate permissions).

**Test steps:**
1. Send individual product payload (`**isIndividual: true**`).
2. **`POST /products`**.

**Expected test case results:** **403** / **`ClientException`** with access denied type message per `ProductService.validateCreatePermissions` (exact message from API).

---

### TC-BE-7 (Negative): Standard product – token only has `INDIVIDUAL_PRODUCT_CREATE`

**Description:** Non-individual create requires **`PRODUCT_CREATE`**.

**Preconditions:**
1. JWT with only **`INDIVIDUAL_PRODUCT_CREATE`**.

**Test steps:**
1. Standard product payload (`**isIndividual: false**` or omitted).
2. **`POST /products`**.

**Expected test case results:** Access denied; product not created.

---

### TC-BE-8 (Negative): Duplicate product name (non-individual)

**Description:** `validateProductNameUniqueness` rejects duplicate **`name`** for non-individual products.

**Preconditions:**
1. An existing non-individual product with a known **`name`** (create once via **`POST /products`** successfully).

**Test steps:**
1. Submit a second **`POST /products`** with the **same** **`name`**, all else valid and distinct where needed.

**Expected test case results:** Error response referencing duplicate name (`basicSettings.name-Product with same name ...` pattern from `ProductService`); no second create.

---

### TC-BE-9 (Negative): Price component accounting – missing income/cost when product fields blank

**Description:** When product-level **`incomeAccountNumber`** / **`costCenterControllingOrder`** are blank, every linked price component must carry those fields (`validatePriceComponentFields`).

**Preconditions:**
1. **Terms** and at least one **price component** created whose accounting fields are **incomplete** for this rule.
2. Reference that PC via **`priceComponentIds`**.

**Test steps:**
1. **`POST /products`** with empty product-level income/cost and linked PCs lacking required accounting data.

**Expected test case results:** Validation / chained exception with messages from `validatePriceComponentFields`; **not** 201.

---

### TC-BE-10 (Negative): Missing required OpenAPI fields

**Description:** Jakarta **`@Valid`** + schema **`required`** array reject incomplete bodies.

**Preconditions:**
1. Valid auth.

**Test steps:**
1. Omit one **`required`** field from **`ProductCreateRequest`** in **`swagger-spec.json`** (e.g. **`contractTypes`**).
2. **`POST /products`**.

**Expected test case results:** **4xx** validation error; body documents missing/invalid field.

---

### TC-BE-11 (Negative): Invalid enum value (e.g. `contractTypes`)

**Description:** Enum strings must match spec exactly (e.g. **`SUPPLY_ONLY`**, not UI label).

**Preconditions:**
1. Valid auth and core ids.

**Test steps:**
1. Send syntactically valid JSON with an **invalid** **`contractTypes`** entry (wrong casing or unknown value).
2. **`POST /products`**.

**Expected test case results:** **4xx**; no creation.

---

### TC-BE-12 (Positive): Create with `priceComponentIds` (accounting satisfied)

**Description:** Product references existing price components where accounting constraints are met.

**Preconditions:**
1. Create two price components with complete accounting **or** set product-level income/cost.
2. Valid **`termId`** or **`termGroupId`**.

**Test steps:**
1. **`POST /products`** including **`priceComponentIds`**: [ids].

**Expected test case results:** **201**; product id returned; linked PCs associated (verify via GET/detail if available).

---

### TC-BE-13 (Positive): Response contract – numeric id and 201

**Description:** Align runtime behaviour with `ProductsController` (**CREATED**) vs OpenAPI (may show 200).

**Preconditions:**
1. Successful create path (TC-BE-1 quality payload).

**Test steps:**
1. **`POST /products`** and capture status + body.

**Expected test case results:** Status **201**; body parseable as **integer id**; document discrepancy if OpenAPI still says 200 for monitoring.

---

### TC-BE-14 (Negative): Unauthenticated request

**Description:** Endpoint is secured with bearer token.

**Preconditions:**
1. None, or intentionally omit **`Authorization`**.

**Test steps:**
1. **`POST /products`** without token.

**Expected test case results:** **401/403** (per security config); no product created.

---

### TC-BE-15 (Positive): Attach interim advance payments / penalties / terminations by id

**Description:** `ProductService.create` calls interim/penalty/termination services — valid ids succeed.

**Preconditions:**
1. Create or resolve valid ids for **`interimAdvancePayments`**, **`penaltyIds`**, **`terminationIds`** (ACTIVE, business-valid for product attachment).

**Test steps:**
1. **`POST /products`** including one or more of these id lists.

**Expected test case results:** **201**; if an id is invalid, expect non-201 and messages accumulated in chained validation pattern.

---

### TC-BE-16 (Negative): Invalid `termId` (not in available term set)

**Description:** Terms must be ACTIVE and allowed for product linking when using **`termId`**.

**Preconditions:**
1. A **`termId`** that is inactive or not in the “available for product” set (environment-specific seed or controlled creation).

**Test steps:**
1. **`POST /products`** with that **`termId`**.

**Expected test case results:** Error from validation / `validateTerms`; no product persist.

---

### TC-BE-17 (Positive): `productStatus` ACTIVE vs INACTIVE on create

**Description:** Swagger allows **`ACTIVE`** / **`INACTIVE`** for **`productStatus`** — both should be accepted if all other rules pass.

**Preconditions:**
1. Full valid baseline payloads for each status.

**Test steps:**
1. Create product with **`productStatus`: `ACTIVE`** — expect 201.
2. Repeat with **`INACTIVE`** — expect 201.

**Expected test case results:** Both return **201** and ids; list/detail reflects status.

---

### TC-BE-18 (Negative): `paymentGuarantees` requiring deposit amounts without proper amounts/currencies

**Description:** Business rules link payment guarantee type to deposit fields (validators on `BaseProductRequest` / service).

**Preconditions:**
1. Set **`paymentGuarantees`** to require cash/bank guarantee but omit **`cashDepositAmount`** / **`bankGuaranteeAmount`** or currencies as per validation rules.

**Test steps:**
1. **`POST /products`**.

**Expected test case results:** **4xx** with validation detail; no creation.

---

### TC-BE-19 (Positive): Related products/services (`relatedEntities`)

**Description:** `productRelatedEntitiesService.addRelatedProductsAndServicesToProduct` runs on create.

**Preconditions:**
1. Valid **`relatedEntities`** entries pointing to existing related product/service ids allowed by API.

**Test steps:**
1. **`POST /products`** with populated **`relatedEntities`**.

**Expected test case results:** **201**; relationships stored; invalid id yields error path.

---

### TC-BE-20 (Regression): After create, product contract flow can resolve product detail

**Description:** Downstream **`ProductContractBasicParametersService`** uses product id + detail version — smoke that a **product contract** create (separate **`POST`**, e.g. product-contract endpoint) can reference the new product id.

**Preconditions:**
1. Successful product create; customer + POD prepared for contract create per contract TC patterns.

**Test steps:**
1. Create minimal **product contract** referencing the new product id (follow **`ProductContractController`** / swagger for that environment).

**Expected test case results:** Contract create succeeds or returns documented validation if other contract fields missing — goal is **no blind backend failure** solely due to product row shape.

---

## References

- Confluence: **Product Create** — `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/9994309/Product+Create` (page id **9994309**) — **content not verified in this run** (login wall).
- Code: `Cursor-Project/Phoenix/phoenix-core/src/main/java/bg/energo/phoenix/controller/product/product/ProductsController.java`
- Code: `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/product/product/ProductService.java`
- OpenAPI: `Cursor-Project/config/swagger/dev/swagger-spec.json` — `paths['/products'].post`, `components.schemas.ProductCreateRequest`
