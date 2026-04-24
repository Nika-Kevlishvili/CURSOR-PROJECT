# Sales Portal — Upsert Additional Contact Person (PHN-2214)

**Jira:** PHN-2214 (Phoenix)
**Type:** Task
**Summary:** Comprehensive backend coverage for the Sales Portal endpoint that creates or updates an "Additional Contact Person" for a customer: `POST /sales-portal/additional-contacts/{customerUic}/{versionId}`. The endpoint supports four operation scenarios (CREATE fresh communication data + contact, CREATE contact in existing communication data, EDIT contact person, and EDIT with ID-match validation). All four flows plus mandatory/optional field validation, source values, multiple phones/emails, match-validation errors, and regression coverage for downstream SQL consumers are validated.

**Scope:** API-level validation (happy paths, negatives, edge cases, regression) for the Sales Portal additional-contacts upsert endpoint. Note: the Jira title uses "PUT" but the implementation is `POST`; tests assert on the actual `POST` implementation and also explicitly check that a `PUT` call does **not** reach the handler (method-mismatch regression). The endpoint runs under the Sales Portal OAuth2 client-credentials JWT (`/sales-portal/**`).

---

## Test data (preconditions)

Shared setup for every Backend test case in this file. Every entity MUST be created with the specified endpoint and parameters — never assume prior data exists.

- **Environment:** Dev2
- **Auth:** Sales Portal OAuth2 client-credentials JWT obtained through `salesPortalTokenAuth()`; all requests use `SPRequest` with `Authorization: Bearer <SP token>`.

1. Obtain a Sales Portal OAuth2 token via the token endpoint (`grant_type=client_credentials`, valid `CLIENT_ID` + `CLIENT_SECRET`). This token is required by every request in this file.
2. Create an **active nomenclature title** via `POST /nomenclature/titles` (name: `"Mr."`, status: `ACTIVE`). Save the returned id as `titleId`.
3. Create an additional active title via `POST /nomenclature/titles` (name: `"Mrs."`, status: `ACTIVE`) — used by EDIT scenarios. Save as `titleId2`.
4. Ensure the nomenclature record with exact name `"Additional Contact Person"` and status `ACTIVE` exists in `nomenclature.contact_purposes` via `POST /nomenclature/contact-purposes` (name: `"Additional Contact Person"`, status: `ACTIVE`) if it is not already seeded.
5. Create a private customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, customerIdentifier: auto-generated 10-digit UIC, linked `titleId` from step 2). The response returns `customerId`, `customerIdentifier` (used as `customerUic`), and `versionId`. Save all three.
6. Verify the customer's version can be read via `GET /customer/{customerId}` — the latest `versionId` matches the one from step 5.
7. For EDIT scenarios (TC-BE-11, TC-BE-12, TC-BE-13 and similar): seed an existing "Additional Contact Person" by calling the endpoint under test in CREATE mode first — `POST /sales-portal/additional-contacts/{customerUic}/{versionId}` with `{ source: "Sales_Portal", title: <titleId>, name: "John", surname: "Doe", phones: ["+359888000111"], emails: ["john.doe@example.com"] }`. Save the returned `communicationDataId` and `contactPersonId` for the EDIT tests.
8. For CREATE-in-existing-comm-data scenarios (TC-BE-8): use the `communicationDataId` from step 7 as an existing communication data record belonging to the customer/version.
9. For match-validation scenarios (TC-BE-10): create a second customer via `POST /customer` and seed another communication-data + contact-person pair for that second customer (repeat steps 5 + 7 with the second customer); this gives a `contactPersonId` that does **not** belong to the first customer's `communicationDataId`.
10. For "communication-data belongs to different customer" negative tests: reuse the second customer's `communicationDataId` from step 9 to try on the first customer.

---

## Backend Test Cases

### TC-BE-1 (Positive): CREATE additional contact — all fields provided, no existing IDs

**Description:** When neither `communicationDataId` nor `contactPersonId` is supplied, the service MUST create a new communication-data row (with the hardcoded CSP address and `"Additional Contact Person"` purpose) **and** a new contact person inside it, and return the new IDs.

**Preconditions:**
1. Complete shared steps 1–6 from Test data above (OAuth2 token + title + customer).
2. No existing additional contact person for this customer version.

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/{customerUic}/{versionId}` with body:
   ```json
   {
     "source": "Sales_Portal",
     "title": "<titleId from step 2>",
     "name": "John",
     "middleName": "Peter",
     "surname": "Doe",
     "birthdate": "1990-01-15",
     "phones": ["+359888000111", "+359888000112"],
     "emails": ["john.doe@example.com", "jp.doe@example.com"],
     "relationship": "Spouse",
     "contactIsOver18": true
   }
   ```
2. Read response status, body and attached metadata.
3. Optionally call a read endpoint (e.g. `GET /customer/{customerId}/communications`) to confirm a new row was persisted.

**Expected test case results:** HTTP `200 OK` (or `201 Created` per implementation). Response body equals `{ "communicationDataId": <Long>, "contactPersonId": <Long> }` with both IDs > 0. A new communication-data record exists with purpose `"Additional Contact Person"`, all foreign address string fields set to `"CSP"`, and a new contact person with `name = "John"`, `middleName = "Peter"`, `surname = "Doe"`, `nameOfContactType = "John Peter Doe"`, `titleId`, `birthdate = 1990-01-15`, `relationship = "Spouse"`, `contactIsOver18Description = "contact person is over 18"`, two phones and two emails persisted.

---

### TC-BE-2 (Positive): CREATE additional contact — mandatory fields only

**Description:** Only `source`, `title`, `name`, `surname` are mandatory. Providing just the mandatory fields MUST succeed and create the entities.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/{customerUic}/{versionId}` with body `{ "source": "Sales_Portal", "title": <titleId>, "name": "Anna", "surname": "Ivanova" }`.
2. Inspect response.

**Expected test case results:** HTTP `200`. Body returns both new IDs. Persisted contact person has `name = "Anna"`, `surname = "Ivanova"`, `nameOfContactType = "Anna Ivanova"` (single space, no double space because `middleName` is absent), no phones, no emails, no `birthdate`, no `relationship`, and `contactIsOver18Description` is null / cleared.

---

### TC-BE-3 (Positive): CREATE with `contactIsOver18 = true`

**Description:** Confirm that the boolean flag `contactIsOver18 = true` produces the derived text `"contact person is over 18"` (or the equivalent implementation field).

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", title, name: "Ivan", surname: "Petrov", contactIsOver18: true }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Persisted field for the age description equals `"contact person is over 18"`.

---

### TC-BE-4 (Positive): CREATE with `contactIsOver18 = false`

**Description:** `contactIsOver18 = false` MUST produce `"contact person is under 18"`.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", title, name: "Maya", surname: "Nikolova", contactIsOver18: false }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Age description equals `"contact person is under 18"`.

---

### TC-BE-5 (Positive): CREATE without `contactIsOver18`

**Description:** When `contactIsOver18` is absent the persisted age description MUST be cleared (null / empty) — not defaulted.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", title, name: "Alex", surname: "Kirov" }` (no `contactIsOver18`).
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Age description on the stored contact person is null / empty.

---

### TC-BE-6 (Positive): CREATE with middleName — nameOfContactType = "Name MiddleName Surname"

**Description:** When `middleName` is provided, `nameOfContactType` MUST be composed as `name + " " + middleName + " " + surname`.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", title, name: "Georgi", middleName: "Ivanov", surname: "Dimitrov" }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. `nameOfContactType = "Georgi Ivanov Dimitrov"` (exactly one space between each token, no leading/trailing whitespace).

---

### TC-BE-7 (Positive): CREATE without middleName — no double space in nameOfContactType

**Description:** When `middleName` is omitted, `nameOfContactType` MUST be `name + " " + surname`, with **no** double space. This is an explicit edge-case requirement from the ticket.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", title, name: "Petar", surname: "Todorov" }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. `nameOfContactType = "Petar Todorov"` — single space only; assert `nameOfContactType.indexOf("  ") === -1`.

---

### TC-BE-8 (Positive): CREATE contact person in EXISTING communication data

**Description:** When `communicationDataId` is provided but `contactPersonId` is absent, the service MUST validate that the communication data exists (and belongs to the customer), create a **new contact person** inside it, and recalculate `nameOfContactType`. No new communication-data row is created.

**Preconditions:**
1. Complete shared steps 1–7 (step 7 produces an existing `communicationDataId` for the customer).

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", communicationDataId: <existing from step 7>, title, name: "Lidia", surname: "Koleva" }`.
2. Inspect the response and the stored rows.

**Expected test case results:** HTTP `200`. Response returns `communicationDataId` equal to the supplied one (NOT a new id) and a **new** `contactPersonId`. Only one communication-data row exists for the purpose; a second contact person is attached to it with `nameOfContactType = "Lidia Koleva"`.

---

### TC-BE-9 (Positive): EDIT with both IDs provided (matching)

**Description:** When both `communicationDataId` and `contactPersonId` are provided and the contact person belongs to that communication data, the service MUST perform an EDIT: update fields, clear absent optional fields, replace phone/email collections and recalculate `nameOfContactType`.

**Preconditions:**
1. Complete shared steps 1–7 (existing contact person with phone/email).

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", communicationDataId: <step 7>, contactPersonId: <step 7>, title: <titleId2>, name: "John-Updated", middleName: "P", surname: "Doe", phones: ["+359888999000"], emails: ["john.updated@example.com"], relationship: "Partner", contactIsOver18: true }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Response returns the same `communicationDataId` and same `contactPersonId` (no new records). Persisted fields reflect the new values: `name = "John-Updated"`, `middleName = "P"`, `surname = "Doe"`, `nameOfContactType = "John-Updated P Doe"`, `titleId = <titleId2>`, `relationship = "Partner"`, age description `"contact person is over 18"`. The previous phones/emails are replaced (soft-deleted and recreated) — only the new phone `+359888999000` and email `john.updated@example.com` are present.

---

### TC-BE-10 (Negative): EDIT with both IDs provided but contact person does NOT belong to communication data

**Description:** When `communicationDataId` and `contactPersonId` are both provided but the contact person is not associated with that communication data, the service MUST raise a `ClientException` / match-validation error.

**Preconditions:**
1. Complete shared steps 1–9 (two customers, seed contact persons for each).

**Test steps:**
1. Call the endpoint for customer #1 with `communicationDataId` from customer #1 but `contactPersonId` from customer #2. Body otherwise valid.
2. Inspect response.

**Expected test case results:** HTTP `400 Bad Request` (or `409` per implementation) with a structured error body — message clearly states the contact person does not belong to the given communication data. No database mutation occurred.

---

### TC-BE-11 (Positive): EDIT with only `contactPersonId` (no communicationDataId)

**Description:** When only `contactPersonId` is supplied, the service MUST resolve the owning communication data via the contact person and perform an EDIT.

**Preconditions:**
1. Complete shared steps 1–7.

**Test steps:**
1. Call the endpoint with `{ source: "Sales_Portal", contactPersonId: <step 7>, title, name: "John", middleName: "P2", surname: "Doe" }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Response returns the resolved `communicationDataId` (equal to the existing one) and the same `contactPersonId`. Fields are updated accordingly; `nameOfContactType = "John P2 Doe"`.

---

### TC-BE-12 (Positive): EDIT clears optional fields when omitted

**Description:** On EDIT, absent optional fields MUST be cleared / removed from the persisted row (not left with the previous value). Tests: omit `birthdate`, `relationship`, `middleName`, `contactIsOver18` in the edit call.

**Preconditions:**
1. Complete shared steps 1–7 (seeded contact person has `birthdate`, `relationship`, `middleName`, `contactIsOver18=true`).

**Test steps:**
1. Call the endpoint with ONLY mandatory fields: `{ source: "Sales_Portal", communicationDataId: <step 7>, contactPersonId: <step 7>, title, name: "John", surname: "Doe" }`.
2. Read the persisted contact person.

**Expected test case results:** HTTP `200`. Persisted contact person has `middleName = null`, `birthdate = null`, `relationship = null`, age description cleared, `nameOfContactType = "John Doe"` (no double space).

---

### TC-BE-13 (Positive): EDIT replaces phones and emails wholesale

**Description:** Phone / email collections MUST be replaced (old ones soft-deleted, new ones inserted).

**Preconditions:**
1. Complete shared steps 1–7 (seed with one phone and one email).

**Test steps:**
1. Call the endpoint in EDIT mode with `phones: ["+359111111111", "+359222222222"]` and `emails: ["a@example.com", "b@example.com"]`.
2. Read phones and emails from the persisted contact.

**Expected test case results:** HTTP `200`. Persisted contact has exactly two phones (`+359111111111`, `+359222222222`) and exactly two emails (`a@example.com`, `b@example.com`). The previous phone/email are not active anymore (soft-deleted / not returned).

---

### TC-BE-14 (Positive): EDIT removes phones/emails when omitted

**Description:** Passing no `phones` and no `emails` on EDIT MUST clear them.

**Preconditions:**
1. Complete shared steps 1–7 (seeded with phones/emails).

**Test steps:**
1. Call the endpoint in EDIT mode without `phones` and without `emails` keys in the body.
2. Read phones/emails.

**Expected test case results:** HTTP `200`. No active phones or emails remain on the contact person.

---

### TC-BE-15 (Positive): Source = "Sales_Portal"

**Description:** Source value `"Sales_Portal"` MUST be accepted and persisted (or used as an audit source) correctly.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with `source = "Sales_Portal"` and valid mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `200`. Record created; any audit field for source reflects `Sales_Portal`.

---

### TC-BE-16 (Positive): Source = "Self_Service_Portal"

**Description:** Source value `"Self_Service_Portal"` MUST be accepted.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `source = "Self_Service_Portal"` + mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `200`; record created; audit source = `Self_Service_Portal`.

---

### TC-BE-17 (Positive): Multiple phones and emails persisted in insertion order

**Description:** Collections of phones and emails MUST be accepted as string arrays; all items are persisted; insertion order is preserved (where the response / read endpoint defines ordering).

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with `phones: ["+359001", "+359002", "+359003"]` and `emails: ["e1@x.com", "e2@x.com", "e3@x.com"]` plus mandatory fields.
2. Read phones and emails.

**Expected test case results:** HTTP `200`. All three phones and all three emails are persisted and active.

---

### TC-BE-18 (Negative): Missing mandatory field — `name`

**Description:** Omitting `name` MUST cause validation error; no records created.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `{ source: "Sales_Portal", title, surname: "Doe" }` (no `name`).
2. Inspect response.

**Expected test case results:** HTTP `400 Bad Request`. Error body references missing/invalid `name`. No new communication-data or contact-person row created.

---

### TC-BE-19 (Negative): Missing mandatory field — `surname`

**Description:** Omitting `surname` MUST be rejected.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `{ source: "Sales_Portal", title, name: "John" }` (no `surname`).
2. Inspect response.

**Expected test case results:** HTTP `400`. Error references missing/invalid `surname`. No records created.

---

### TC-BE-20 (Negative): Missing mandatory field — `title`

**Description:** Omitting `title` MUST be rejected.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `{ source: "Sales_Portal", name: "John", surname: "Doe" }` (no `title`).
2. Inspect response.

**Expected test case results:** HTTP `400`. Error references missing/invalid `title`. No records created.

---

### TC-BE-21 (Negative): Missing mandatory field — `source`

**Description:** Omitting `source` MUST be rejected.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `{ title, name: "John", surname: "Doe" }` (no `source`).
2. Inspect response.

**Expected test case results:** HTTP `400`. Error references missing/invalid `source`. No records created.

---

### TC-BE-22 (Negative): Invalid `source` value

**Description:** Only `Sales_Portal` and `Self_Service_Portal` are allowed values for `source`.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `source = "RANDOM_VALUE"` + mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `400`. Error indicates invalid enum for `source`. No records created.

---

### TC-BE-23 (Negative): Invalid `customerUic` — non-existent

**Description:** Calling with a `customerUic` that does not match any customer MUST return a not-found / validation error from `CustomerDetailsRepository.findByCustomerIdentifierAndVersionId`.

**Preconditions:**
1. Obtain SP token (shared step 1). No customer with UIC `9999999999999`.

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/9999999999999/1` with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `404` (or `400` if the implementation maps it that way). Error references the missing customer UIC / version. No records created.

---

### TC-BE-24 (Negative): Invalid `versionId` — non-existent for the customer

**Description:** A valid UIC with a version that does not exist for that customer MUST be rejected.

**Preconditions:**
1. Complete shared steps 1–6 (known `customerUic`, known `versionId`).

**Test steps:**
1. Call with `customerUic = <valid>` and `versionId = <versionId + 999>`.
2. Inspect response.

**Expected test case results:** HTTP `404` or `400`. Error message indicates no customer/version match. No records created.

---

### TC-BE-25 (Negative): `communicationDataId` exists but belongs to a DIFFERENT customer

**Description:** Supplying a `communicationDataId` that exists in the DB but belongs to another customer MUST be rejected.

**Preconditions:**
1. Complete shared steps 1–9 (two customers with their own comm data).

**Test steps:**
1. Call for customer #1 with `communicationDataId` from customer #2's comm data. Body otherwise valid for CREATE-in-existing-comm-data (no `contactPersonId`).
2. Inspect response.

**Expected test case results:** HTTP `400` / `404`. Error makes it clear the communication data does not belong to the target customer/version. No records created / no cross-customer write.

---

### TC-BE-26 (Negative): `contactPersonId` does not exist

**Description:** Supplying a `contactPersonId` that does not exist MUST return a not-found / validation error.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call with `contactPersonId = 99999999` (non-existent), plus mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `404` / `400` with an explicit "contact person not found" message. No records modified or created.

---

### TC-BE-27 (Negative): `versionId <= 0` validation

**Description:** Path parameter `versionId` is documented as a **positive** integer. `0` or negative values MUST be rejected at the validation layer.

**Preconditions:**
1. Complete shared steps 1–6 (we only need a valid UIC from step 5).

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/<valid UIC>/0` with a valid body.
2. Call with `-1` as `versionId`.
3. Inspect both responses.

**Expected test case results:** Both calls return HTTP `400`. Error body references the `versionId` constraint. No records created.

---

### TC-BE-28 (Negative): `customerUic` non-integer (path validation)

**Description:** `customerUic` is typed as Integer. A non-integer path segment MUST be rejected by Spring's path-variable binder.

**Preconditions:**
1. Obtain SP token.

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/abc/1` with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `400` (or `404` depending on the routing mapping). No records created.

---

### TC-BE-29 (Negative): `contactPersonId` absent + `communicationDataId` present + bad comm data for this version

**Description:** When creating a contact person in existing comm data, and that comm data exists but is not for the target version of the customer, service MUST reject.

**Preconditions:**
1. Complete shared steps 1–7. After step 7, simulate an older customer version by creating a new version via `POST /customer/{customerId}/version` — the comm data from step 7 is tied to the older version.

**Test steps:**
1. Call with `customerUic` + **new** `versionId`, `communicationDataId` from step 7 (older-version comm data), no `contactPersonId`, valid body.
2. Inspect response.

**Expected test case results:** HTTP `400` / `404`. Error indicates the comm data does not belong to the supplied customer/version. No records created.

---

### TC-BE-30 (Negative): Inactive `title` id (nomenclature not ACTIVE)

**Description:** The ticket states the `titleId` must correspond to an ACTIVE nomenclature title. An INACTIVE title MUST be rejected.

**Preconditions:**
1. Complete shared step 1.
2. Create an INACTIVE title via `POST /nomenclature/titles` (`status: INACTIVE`). Save id as `inactiveTitleId`.
3. Complete shared steps 5–6.

**Test steps:**
1. Call with `title = <inactiveTitleId>`, valid mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `400`. Error body references the title being inactive / invalid. No records created.

---

### TC-BE-31 (Negative): Missing `"Additional Contact Person"` purpose nomenclature

**Description:** If the `"Additional Contact Person"` nomenclature is missing or INACTIVE, CREATE flow MUST fail with a clear error — regression guard from cross-dependency `what_could_break` item "Nomenclature string".

**Preconditions:**
1. Complete shared step 1, then mark `"Additional Contact Person"` contact-purpose nomenclature as INACTIVE via `PUT /nomenclature/contact-purposes/{id}` (status: INACTIVE). (Restore to ACTIVE after test.)
2. Complete shared steps 5–6.

**Test steps:**
1. Call CREATE with valid mandatory fields (no `communicationDataId`, no `contactPersonId`).
2. Inspect response.
3. Cleanup: reactivate nomenclature.

**Expected test case results:** HTTP `400` / `409`. Error clearly indicates the missing/inactive `"Additional Contact Person"` purpose. No communication-data row created.

---

### TC-BE-32 (Positive): CREATE produces CSP-filled foreign address fields

**Description:** The CREATE branch fills all foreign-address string fields with `"CSP"` on the new communication-data row. This must be persisted exactly.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with mandatory fields.
2. Read the new communication-data row (`GET /communication-data/{id}` or via customer read).

**Expected test case results:** HTTP `200` on CREATE. The persisted row has all foreign address string fields (e.g. `foreignStreet`, `foreignCity`, `foreignState`, `foreignCountryName`, `foreignZipCode`, etc.) equal to `"CSP"`. `country_id` may be null (regression guard — see TC-BE-39).

---

### TC-BE-33 (Positive): Response structure validation

**Description:** The response body MUST contain exactly `communicationDataId` (Long) and `contactPersonId` (Long).

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with mandatory fields.
2. Parse response.

**Expected test case results:** Response JSON contains exactly the keys `communicationDataId` and `contactPersonId`; both are integers > 0 (fit in `Long`); no extra fields or nulls.

---

### TC-BE-34 (Negative): Auth — missing SP token

**Description:** The endpoint is protected by Sales Portal OAuth2 JWT on `/sales-portal/**`. A request without a bearer token MUST be rejected with `401`.

**Preconditions:**
1. Shared step 1 NOT performed — no token.
2. Complete shared steps 5–6 via a separately authorized admin to set up the customer.

**Test steps:**
1. Call the endpoint with a valid body but without an `Authorization` header.
2. Inspect response.

**Expected test case results:** HTTP `401 Unauthorized`. No records created.

---

### TC-BE-35 (Negative): Auth — expired / invalid SP token

**Description:** A malformed or expired bearer token MUST be rejected with `401`.

**Preconditions:**
1. Complete shared steps 5–6 for entity setup.

**Test steps:**
1. Call the endpoint with `Authorization: Bearer invalid.token.value`.
2. Inspect response.

**Expected test case results:** HTTP `401`. No records created.

---

### TC-BE-36 (Negative): Wrong HTTP method — PUT returns 405 / routing mismatch

**Description:** The Jira title says "PUT" but the implementation uses `POST`. To prevent contract drift, `PUT /sales-portal/additional-contacts/{customerUic}/{versionId}` MUST NOT reach the handler — either 404 or 405. This locks the contract decision.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call `PUT /sales-portal/additional-contacts/{customerUic}/{versionId}` with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `404` or `405 Method Not Allowed`. No upsert executed.

---

### TC-BE-37 (Positive): EDIT preserves `communicationDataId` and `contactPersonId`

**Description:** EDIT flow MUST NOT change the identifiers of the existing comm data or contact person.

**Preconditions:**
1. Complete shared steps 1–7.

**Test steps:**
1. Call the endpoint in EDIT mode with both IDs and modified body.
2. Compare returned IDs to the precondition IDs.

**Expected test case results:** HTTP `200`. Returned `communicationDataId` and `contactPersonId` equal the existing IDs exactly.

---

### TC-BE-38 (Regression): Downstream SQL joins — new comm row must not break billing/receivable/reminder queries

**Description:** Cross-dependency `what_could_break` lists `Receivable/reminder/billing SQL` joins on `customer_communications` and `CustomerService` native SQL with large queries. Creating a new "Additional Contact Person" communication-data row MUST NOT change how those consumers pick the primary communication row.

**Preconditions:**
1. Complete shared steps 1–6.
2. Create a standard communication row for the customer via `POST /customer-communications` (type: primary, email, phone) to simulate the main contact that downstream SQLs rely on.
3. Create an invoice + reminder chain: `POST /product-contract` → `POST /billing-run` (STANDARD, one period) → execute it to GENERATE an invoice → `POST /reminder` targeting that invoice (all linked back to the customer from step 5 of shared data).

**Test steps:**
1. Call the endpoint under test in CREATE mode — a new comm-data row with purpose "Additional Contact Person" is added for the customer.
2. Re-run the receivable / reminder / billing read queries (e.g. `GET /reminder/{id}`, `GET /invoice/{id}`, `POST /receivable/list`) and verify the primary contact info still comes from the step-2 row, not from the new additional contact row.
3. Trigger another billing run / reminder for the same customer and verify the generated communications still reference the primary row.

**Expected test case results:** Receivable/reminder/billing queries return the same primary communication row as before the additional contact was added. The new row does not appear in their results. No regression in joins.

---

### TC-BE-39 (Regression): `country_id` may remain unset on new CSP comm-data row

**Description:** Cross-dependency `what_could_break` item: "CSP address vs country_id — foreign string fields set to CSP; country_id may be unset". The test guards that leaving `country_id` null (as per implementation) does not crash any read endpoint over the new row.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with mandatory fields.
2. Read the new comm-data row via `GET /communication-data/{id}` and via any consumer read that returns country info.
3. Read customer overview (`GET /customer/{customerId}`) to confirm no NPE / error across all communication rows.

**Expected test case results:** HTTP `200` on all reads. The additional-contact comm-data row has foreign strings = `"CSP"` and `country_id` may be null; all consumers return successfully without throwing NullPointerException / 500.

---

### TC-BE-40 (Negative): `versionId` is a string / non-numeric

**Description:** Non-numeric `versionId` path segment MUST fail type conversion.

**Preconditions:**
1. Shared step 1.

**Test steps:**
1. Call `POST /sales-portal/additional-contacts/<valid UIC>/abc` with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `400`. No records created.

---

### TC-BE-41 (Negative): Empty JSON body

**Description:** An empty body `{}` MUST fail mandatory-field validation and return all missing-field errors.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with body `{}`.
2. Inspect response.

**Expected test case results:** HTTP `400`. Error lists all four missing mandatory fields (`source`, `title`, `name`, `surname`). No records created.

---

### TC-BE-42 (Negative): Body is not valid JSON

**Description:** Malformed JSON body MUST be rejected before the handler runs.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call the endpoint with raw body `{"source":"Sales_Portal", ... malformed`.
2. Inspect response.

**Expected test case results:** HTTP `400 Bad Request`. No records created. Content-type negotiation error surfaced cleanly.

---

### TC-BE-43 (Negative): `phones` is not an array (wrong type)

**Description:** `phones` and `emails` are string arrays. Passing a plain string MUST fail type validation.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with `phones: "+359888000111"` (string, not array) + mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `400`. Error references `phones` type. No records created.

---

### TC-BE-44 (Positive): EDIT same contact keeps phone/email when re-sent

**Description:** Re-sending the same phone/email on EDIT MUST result in an active phone/email equal to the new payload (implementation soft-deletes the old entries and inserts new ones — the outcome must still match the payload).

**Preconditions:**
1. Complete shared steps 1–7 (contact has phone `+359888000111` and email `john.doe@example.com`).

**Test steps:**
1. Call EDIT with the same `phones: ["+359888000111"]` and `emails: ["john.doe@example.com"]`.
2. Read phones/emails.

**Expected test case results:** HTTP `200`. Exactly one active phone `+359888000111` and exactly one active email `john.doe@example.com` on the contact (no duplicates).

---

### TC-BE-45 (Negative): `birthdate` malformed (not `YYYY-MM-DD`)

**Description:** `birthdate` is a date-formatted string. Invalid format MUST fail validation.

**Preconditions:**
1. Complete shared steps 1–6.

**Test steps:**
1. Call CREATE with `birthdate = "15/01/1990"` (wrong format).
2. Call CREATE with `birthdate = "1990-13-40"` (impossible date).
3. Inspect both responses.

**Expected test case results:** Both return HTTP `400`. Error references `birthdate` format. No records created.

---

## References

- **Jira:** PHN-2214 – Sales Portal — Upsert Additional Contact Person.
- **Code entry point:** `SalesPortalAdditionalContactsService.upsertAdditionalContact`; controller `POST /sales-portal/additional-contacts/{customerUic}/{versionId}`.
- **Nomenclature:** `nomenclature.contact_purposes.name = "Additional Contact Person"` (ACTIVE).
- **Auth:** Sales Portal OAuth2 client-credentials JWT.
- **Downstream regression concerns:** `customer_communications` joins in receivable/reminder/billing SQL, `CustomerService` native SQL.
- **Template:** `Cursor-Project/config/template/Test_case_template.md`.
- **Layout rule:** `.cursor/rules/workspace/test_cases_structure.mdc`.
