# Payment mass import — automatic payment offsetting failure and process outcomes (PDT-2713)

**Jira:** PDT-2713 (Phoenix Delivery — use your board label if different)  
**Type:** Bug  
**Summary:** Verify payment mass import rows create payments when automatic offsetting succeeds, and document safe failure when stored procedure `receivable.automatic_payment_offsetting_out` errors (error signature `Automatic payment offsetting out failed;`), including parity with manual payment creation and bank-file `:20:` date handling.

**Scope:** Payment mass import invokes `PaymentMassImportProcessService.processRecord`, then `PaymentService.create`, which calls `AutomaticOffsettingService.offsetOfPayments` ( JDBC call to `receivable.automatic_payment_offsetting_out`). On SQL/procedure-level exception the service throws `ClientException` with message `Automatic payment offsetting out failed;` — the transactional create must not leave a persisted payment for the failed row. PreProd evidence: process **1781**, `file_url` under `payment_mass_import/2026-04-30/`, failed row `record_identifier='РИ 1100103788'`, `record_identifier_version=null`, `success=false`, **no payment** created for that row. Automated API tests MUST use **`test.step`-sized steps**: one HTTP interaction (or logical DB/assertion bundle) per step; assert HTTP status then parse body (`CheckResponse` pattern in Playwright).

---

## Test data (preconditions)

Shared setup used by several scenarios. Resolve exact JSON field names and enums from Swagger for the target environment (**Rule SWAGGER.0**) before automation.

- **Environment:** PreProd (for parity with incident evidence) **or** Test — state which is used.

1. Authenticate via the Phoenix API (JWT) using the environment auth endpoint from Swagger; store Bearer token for all subsequent requests.
2. Create a customer via `POST /customer` with **type** PRIVATE (or LEGAL per test need), **status** ACTIVE; capture `customerId` and `customerNumber` from the response body.
3. Create a POD (Point of Delivery) via `POST /pod` (**type**: ELECTRICITY or GAS per tariff need, **status** ACTIVE); capture `podId`.
4. Create a product via `POST /product`, terms via `POST /terms`, price component via `POST /price-component` per dependency order required by Swagger; capture `productId`.
5. Create a product contract via `POST /product-contract` linking **customerId** from step 2, **podId** from step 3, **productId** from step 4; set **status** ACTIVE and sensible **entry-into-force** date; capture `contractId` / `contractBillingGroupId` as exposed by Swagger (`CreatePaymentRequest`-related linkage).
6. Create energy data / billing profile entries required for invoicing via the Swagger-documented endpoints (linked to contract and POD).
7. Create and execute a billing run via `POST /billing-run` (period covering the planned **paymentDate**); ensure at least one **invoice** exists with outstanding receivable suitable for incoming payment automatic offsetting (capture `invoiceId`, amounts, accounting period identifiers as required by `CreatePaymentRequest`).
8. Obtain an **UNLOCKED** payment package applicable to the planned **paymentDate** and **collectionChannelId**: create or select via `GET`/`POST` patterns under Swagger for `payment-package` (use `Endpoints.paymentPackage` in EnergoTS); capture `paymentPackageId`.
9. Create or resolve a **collection channel** cleared for payment mass import: use the Swagger endpoint that lists/filters channels for payment mass import (implementation: `CollectionChannelService.filterForPaymentMassImport`; path per refreshed spec); capture `collectionChannelId` and **currencyId** aligning with invoice/POD.
10. Provision a **payment mass import process** tied to domain **PAYMENT** with status allowing file attach and **start**: use the Portal workflow **or** the process-creation endpoints from Swagger (**Process** area). Capture numeric **`processId`**. _(If Swagger does not expose create, document the UI-only creation path and persist `processId` from `GET /process` list after creation.)_
11. Stage the import **file**: either **`POST /mass-import/payment/files/upload`** (`MassImportController`, multipart: `file`, optional `date`, optional `collectionChannelId`) — **domainType** path segment `payment` matches `DomainType.PAYMENT` — **or**, when internal jobs read from FTP, place the generated `.txt` on the FTP path analogous to `/data/ftp_folder/payment_mass_import/<yyyy-MM-dd>/` per integration configuration; associate the staged file with `processId` per product rules.
12. **Incident replay file reference (manual / PreProd investigation):** `file_url`: `/data/ftp_folder/payment_mass_import/2026-04-30/7fa8668c-f53f-4d61-9e35-6fe354a52207_BG42_26.03.2026_test.txt` — use sanitized copies in lower environments without production data.

---

## Backend Test Cases

### TC-BE-1 (Positive): Payment-partner TXT import — successful row persists payment and processed_record_info success

**Description:** Validates the happy path for **non-bank** (payment partner) files: mapper builds `CreatePaymentRequest`, validation passes, `PaymentService.create` completes, automatic offsetting returns **OK**, and the process stores a successful processed record.

**Preconditions:**
1. Complete shared steps **1–10** from Test data using a dataset where liabilities/receivable state allows **`receivable.automatic_payment_offsetting_out`** to return **o_message = OK**.
2. Build a minimal conforming payment-partner `.txt` (no `:20:` in file body so `isBankFile` is **false**) containing one payable row keyed to **customerNumber** / outgoing document identifiers required by `PaymentPartnerMapper`.
3. `processId` exists in **Created**/**Ready-to-start** (or equivalent) state per `GET /process/{id}`.

**Test steps:**
1. **`POST`** multipart `/mass-import` with form field **`file`** = generated `.txt` and **`processId`** = value from precondition 10 (see `CustomerMassImportController`; controller maps `/mass-import` **POST** multipart). Use `Expect` status **204** (`NO_CONTENT`).
2. **`PUT /process/{id}/start`** to run the worker chain for `PAYMENT_MASS_IMPORT_PROCESS` — poll **`GET /process/{id}`** until terminal or success criteria from response DTO indicates completion (timeouts per environment SLA).
3. Query processed records for **`processId`** using the Swagger-documented listing/detail endpoint **or** read `processed_record_info` via controlled read-only DB access (SELECT) — whichever is the acceptance method for QA; locate the row matching the test **record_identifier**.
4. **`GET`** payment list or **`GET /payment/{id}`** (per Swagger) for the created payment by business key (payment number / customer / package / date composite as available).

**Expected test case results:** Processed row has **`success` = true** and **no** error message (or empty). A **payment** row exists linked to correct **customer**, **payment package**, **collection channel**, and **payment date**; `initialAmount/currentAmount` match expectations after procedure `automatic_payment_offsetting_out` (non-negative residual per business rules).

**Actual result (if bug):** Row succeeds in mapper but disappears or **`success`** false with `Automatic payment offsetting out failed;`.

**References:** `PaymentMassImportProcessService.processRecord` (partner branch calls `paymentService.create`).

---

### TC-BE-2 (Positive): Bank-partner TXT with `:20:` header — payment date from header drives mapping and succeeds

**Description:** Validates **bank file** parsing path: detection via **`:20:`** in payload, **`BankPartnerMapper`**, **`extractTag20Date`** supplies **paymentDate**/`tag20Date` aligned with **`mapToPaymentRequest`**, consistent with **`PaymentMassImportProcessService.isBankFile`**.

**Preconditions:**
1. Complete shared steps **1–10** with reconciliation data suitable for automatic offset success.
2. Build a synthetic bank TXT containing **`:20:`** line with **`paymentDate`** equal to **`yyyy-MM-dd`** parseable header metadata per `paymentDate` extraction in `extractTag20Date` / `bankPartnerMapper.parseMetadata`.
3. At least one transactional record stanza parses to field **`86`** sub-tags required for identifier extraction (**outgoing doc** matching step 8 invoice numbering pattern or **customer / billing group** identifiers per mapper rules).

**Test steps:**
1. Upload file via **`POST /mass-import/payment/files/upload`** (`MassImportController`) with **`date`** query param deliberately **different** from `:20:` **only if product allows checking precedence** — otherwise omit `date` and assert `:20:` drives **`tag20Date`** (assert via resulting payment.paymentDate vs process date expectation).
2. Trigger process run ( **`PUT /process/{processId}/start`** after binding file to same `processId` per product wiring).
3. Assert processed record **success=true** for the interpreted identifier.
4. **`GET`** created payment — **`paymentDate`** equals **parsed `:20:`** date, **not** an arbitrary runner default.

**Expected test case results:** No validation error **`Error: Header date (:20:) is missing.`** or **`Invalid header date (:20:)`**; mapper uses header date where designed; payment persisted with correct **paymentDate**.

**References:** `PaymentMassImportProcessService.isBankFile`, `extractTag20Date`; lines **145–187** logic for missing/invalid `:20:`.

---

### TC-BE-3 (Positive): Manual `POST /payment` parity — identical financial keys succeed when mass import succeeds

**Description:** Regression for **coverage_focus** manual vs mass-import parity — same **`CreatePaymentRequest`**-equivalent field set created via Swagger **`POST /payment`** should succeed whenever mass-import row succeeds (permission and package rules aligned).

**Preconditions:**
1. Complete entity chain steps **2–9** identical to TC-BE-1 **without** invoking mass import yet.
2. Use a user/context with **`RECEIVABLE_PAYMENT`** create permissions and **not** missing offsetting-related validation.

**Test steps:**
1. Build **`CreatePaymentRequest`** payload from Swagger (all **required** fields): **initialAmount**, **paymentDate**, **currencyId**, **collectionChannelId**, **paymentPackageId**, **accountPeriodId**, **customerId**, **contractBillingGroupId**, **outgoingDocumentType** / **invoiceId** as rule requires.
2. **`POST /payment`** with JSON body; assert **2xx** and capture **`paymentId`** **after** body assertion order per Playwright rules (`CheckResponse` then `json()`).
3. Optionally repeat with mass-import row payload representing the same business keys and compare **paymentId** absence/presence rules (second payment may be blocked — document expected uniqueness).

**Expected test case results:** Manual create returns success; automatic offsetting path runs (`executePaymentOffsetting` from `PaymentService.create`); no spurious `Automatic payment offsetting out failed;` when DB healthy.

**References:** `PaymentService.create` **lines 212–287** (save, `executePaymentOffsetting`, receivable follow-up).

---

### TC-BE-4 (Positive): Mixed batch — one row succeeds, second row fails independently

**Description:** Process lifecycle with **mixed** outcomes: individual row transactions must not roll back unrelated committed rows (isolation per record / task as implemented).

**Preconditions:**
1. Shared steps **1–10** with two independent customer/invoice chains (repeat legal entities) so two rows can be prepared in one file.
2. First row valid; second row triggers **known** validation error (e.g. unknown customer number) **or** offsetting failure if environment can simulate — pick one failure mode and keep the other success.

**Test steps:**
1. Upload multi-row file; **start** process.
2. Retrieve all **`processed_record_info`** rows for `processId`.
3. Map each **record_identifier** to success/failure.

**Expected test case results:** At least one **`success=true`** and at least one **`success=false`** with distinct **errorMessage**; **no** cross-row identifier collision; successful row has payment if offsetting OK.

**References:** Process row-level handling in `AbstractTxtMassImportProcessService` (subclass `PaymentMassImportProcessService`).

---

### TC-BE-5 (Positive): Download mass import source file — `GET /process/download-mass-import-file/{id}`

**Description:** FTP / storage integration surface — ensure API returns the bytes stored for the process (integrity after upload).

**Preconditions:**
1. Completed successful **upload** for `processId` (step 11).
2. Caller has **PROCESS_VIEW** / **PROCESS_VIEW_SU** equivalent permissions.

**Test steps:**
1. **`GET /process/download-mass-import-file/{processId}`**.
2. Compare SHA-256 checksum of response body vs original local fixture file.

**Expected test case results:** HTTP **200**; content matches uploaded file (`ProcessController.downloadMassImportFile`).

---

### TC-BE-6 (Positive): Offsetting consumes amount — residual creates receivable when rule applies

**Description:** Validates `PaymentService.create` post-offset branch: **`paymentCurrentAmount > 0`** triggers **`createReceivableFromPayment`** and optional notification paths.

**Preconditions:**
1. Data where procedure returns **remainder** **`> 0`** on purpose (partial applicability of automatic offset).

**Test steps:**
1. Run payment create (mass import row **or** `POST /payment`).
2. Read payment **currentAmount** vs **initialAmount**.
3. Query receivable created from payment (Swagger list/detail or controlled DB SELECT).

**Expected test case results:** Receivable id returned when **`paymentCurrentAmount.compareTo(ZERO) > 0`** per code path **266–287** behavior.

---

### TC-BE-7 (Negative): Automatic offsetting exception — transactional failure, no persisted payment for row (**PDT-2713 signature**)

**Description:** Primary bug/regression reproduction: JDBC layer throws when calling **`receivable.automatic_payment_offsetting_out`**; **`AutomaticOffsettingService.offsetOfPayments`** catch block throws **`ClientException("Automatic payment offsetting out failed;")`** — **`PaymentService.create`** transaction must roll back persisted payment identity for mass-import row (**observed outcome: no payment** on PreProd failing row).

**Preconditions:**
1. Complete upstream chain **through step 10** plus file row referencing valid customer/invoice identifiers so validation passes until offsetting (**must be reproducible**: broken DB linkage, sabotaged connectivity, mocked failure in isolated env, **or** data pattern known to provoke SQL exception inside procedure — engineer with DBA playbook).
2. Prepare **instrumentation**: ability to observe DB **payment** table / sequence for phantom rows.

**Test steps:**
1. Upload offending row matching evidence pattern (identifier analogous to **`РИ 1100103788`** in field **86** / outgoing doc extraction when bank file format).
2. **Start** process; wait completion.
3. Read **`processed_record_info`**: **`success=false`**, **`errorMessage`** substring **`Automatic payment offsetting out failed;`**.
4. **`GET`** payment list filtering by inferred payment number/customer/date — verify **zero** persisted payment tied to attempted import row (and **sequence**/`payment_number` not consumed for orphaned ACTIVE row if transactional rollback enforced).

**Expected test case results:** Processed row failed with exact error substring; **no** orphan payment ACTIVE row attributable to failure; aligns with **`AutomaticOffsettingService.offsetOfPayments`** throwing **ClientException** on exception path **lines 101–133** while **`PaymentService.create`** had called **`saveAndFlush`** before offsetting (**lines 253–260**) — failure must rollback entire transaction boundary for **`create`** (assert via absence of orphan row).

**Actual result:** PreProd Process **1781**: `success=false`, `record_identifier_version=null`, observed **no payment** for failed row — document match.

**References:** `AutomaticOffsettingService.offsetOfPayments` **101–133**; `PaymentService.create` **253–287**.

---

### TC-BE-8 (Negative): Bank file missing valid `:20:` date — rejects before **`paymentService.create`**

**Description:** Validates **`extractTag20Date`** accumulation of **`errorMessages`** when **`paymentDate`** absent or blank.

**Preconditions:**
1. Synthetic bank TXT with `:20:` token present but **`paymentDate` metadata missing** OR header stripped per mapper rules (controlled negative fixture).

**Test steps:**
1. Upload and run process **or** call mapper-validation layer if isolated unit path exists API-side.
2. Inspect processed record / error list entry.

**Expected test case results:** Error message contains **`Header date (:20:) is missing.`** (**code** references **PaymentMassImportProcessService** extractTag20Date lines **174–178**).

---

### TC-BE-9 (Negative): Invalid parseable `:20:` value — `Invalid header date (:20:) value.`

**Preconditions:**
1. Bank TXT with unparsable `paymentDateStr` injected into mapper metadata extraction.

**Test steps:**
1. Upload/start.
2. Read failure reason on processed row.

**Expected test case results:** Error includes **`Invalid header date (:20:)`** per **184–185**; **no payment** persisted.

---

### TC-BE-10 (Negative): Row identifier cannot be extracted from **field `86`** — `INVALID_IDENTIFIER` / failure path (`getIdentifier` bank branch)

**Description:** Validates fallback when **`field86` missing** (**`INVALID_IDENTIFIER`**) causes controlled failure (**lines 191–199** logic).

**Preconditions:**
1. Bank record map missing **`86`** column after parsing.

**Test steps:**
1. Submit malformed segment in otherwise valid bank TXT.
2. Inspect processed_record_info **`record_identifier`** and **error**.

**Expected test case results:** Failure flagged; no silent success; aligns with **`getIdentifier`** bank branch safeguards.

---

### TC-BE-11 (Negative): Missing **`PAYMENT_MI_CREATE`** — `ACCESS_DENIED`-style denial on **`processRecord`**

**Description:** Permission gate **PaymentMassImportProcessService.processRecord** line **104–106**.

**Preconditions:**
1. Authentication context **without** `PermissionEnum.PAYMENT_MI_CREATE` while still able to authenticate.

**Test steps:**
1. Upload file/start under restricted token.

**Expected test case results:** Row or process surfaces **permission** failure (`Not enough permission for creating payment`); HTTP layer maps per global error handler (**assert status** from Swagger — typically **403** family).

---

### TC-BE-12 (Negative): All rows invalid — process completes with full failure tally; downloadable report reflects zero successes

**Preconditions:**
1. File consisting only of invalid rows.

**Test steps:**
1. Run import; **`GET /process/{id}/report/download`** (with required `multiSheetExcelType` param per controller).

**Expected test case results:** Report workbook shows failures only; **`GET /process/{id}`** summary fields aligned with zeros success / N failures (**coverage_focus** all-failed package behavior).

---

### TC-BE-13 (Negative): Payment package not applicable for **payment date** + channel — validation rejects before persistence

**Preconditions:**
1. Deliberately supply **`paymentPackageId`** incompatible with **`collectionChannelId` + paymentDate`** per `PaymentService.validateAndSetPaymentPackage` rules (**lines 348–351** finder).

**Test steps:**
1. Mass import mapper produces request with contradictory package/date (**or** direct `POST /payment` parity check).

**Expected test case results:** Processed **`success=false`** with package applicability error substring (`not applicable for selected payment date and collection channel`); **no** payment.

---

### TC-BE-14 (Positive): **`PUT /process/{id}/resume`** — recovery after paused payment mass import (regression lifecycle)

**Preconditions:**
1. Long-running or chunkable import; ability to **`PUT /pause`** mid-run if supported for domain.

**Test steps:**
1. Start; **`PUT /pause`**, **`PUT /resume`**, finalize.

**Expected test case results:** No duplicate payments for already processed stable keys; resumed rows reconcile with idempotency rules (document findings per actual implementation).

---

## References

- **Jira:** PDT-2713 — Payment mass import offsetting failure (PreProd process **1781**, Cyrillic **`РИ 1100103788`**, **`Automatic payment offsetting out failed;`**).
- **Code:**
  - `Cursor-Project/Phoenix/phoenix-core-lib/.../PaymentMassImportProcessService.java`
  - `Cursor-Project/Phoenix/phoenix-core-lib/.../service/receivable/payment/PaymentService.java` (**`create`**, **`executePaymentOffsetting`**).
  - `Cursor-Project/Phoenix/phoenix-core-lib/.../model/entity/receivable/AutomaticOffsettingService.java` (**`offsetOfPayments`**, JDBC `CALL receivable.automatic_payment_offsetting_out`).
  - `Cursor-Project/Phoenix/phoenix-core/.../controller/customer/CustomerMassImportController.java` (**`POST /mass-import`** multipart).
  - `Cursor-Project/Phoenix/phoenix-core/.../controller/process/MassImportController.java` (**`POST /mass-import/payment/files/upload`**).
  - `Cursor-Project/Phoenix/phoenix-core/.../controller/process/ProcessController.java` (**process lifecycle + downloads**).

