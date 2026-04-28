# Sales Portal — Upsert Additional Contact Person (PHN-2214) — Frontend (SPRequest-driven)

**Jira:** PHN-2214 (Phoenix)
**Type:** Task
**Summary:** PHN-2214 is a backend-only change (no dedicated UI screen). "Frontend" test cases here validate the Sales Portal **API client contract** as consumed by the Sales Portal frontend — driven via the `SPRequest` fixture with a real OAuth2 client-credentials JWT. They cover the happy flow, input validation surfaced to UI callers, and authentication scenarios that the frontend depends on.

**Scope:** Sales Portal **API client** validation (not a UI screen) — request shape, OAuth token handling, validation errors surfaced to the UI, and response contract (`communicationDataId`, `contactPersonId`) needed by the Sales Portal frontend to update the customer view after the call.

---

## Test data (preconditions)

Shared setup for every Frontend test case in this file.

- **Environment:** Dev2
- **Client:** Sales Portal frontend using `SPRequest` (separate base URL, OAuth2 bearer token).

1. Obtain a Sales Portal OAuth2 client-credentials token via `salesPortalTokenAuth()` — required for every positive and error-validation call (the UI uses the same token flow).
2. Create an active nomenclature title via `POST /nomenclature/titles` (name: `"Mr."`, status: `ACTIVE`); save `titleId`.
3. Create a private customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, auto-generated 10-digit identifier, linked `titleId` from step 2); save `customerUic` (= customerIdentifier) and `versionId`.
4. For EDIT-flow frontend tests: seed an additional contact person by calling the endpoint under test in CREATE mode first (`source: "Sales_Portal"`, mandatory fields) and store the returned `communicationDataId` and `contactPersonId`.

---

## Frontend Test Cases

### TC-FE-1 (Positive): SPRequest — CREATE additional contact person, happy path

**Description:** Verify that a Sales Portal client with a valid OAuth2 token can call the endpoint and receive the expected response contract that the frontend uses to render the new additional contact.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call `POST /sales-portal/additional-contacts/{customerUic}/{versionId}` with `{ source: "Sales_Portal", title: <titleId>, name: "John", middleName: "P", surname: "Doe", phones: ["+359888000111"], emails: ["john.doe@example.com"], relationship: "Spouse", contactIsOver18: true }`.
2. Attach the response to the test report.

**Expected test case results:** HTTP `200`. Response body contains `{ communicationDataId: <Long>, contactPersonId: <Long> }`. Both IDs are numeric and > 0 — the frontend can use them to refresh its contact-person list.

---

### TC-FE-2 (Positive): SPRequest — Minimal CREATE (mandatory fields only)

**Description:** The Sales Portal UI must handle the minimum required payload (when the user fills only the required form fields). Validate that the API accepts it and returns both IDs.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call the endpoint with `{ source: "Sales_Portal", title: <titleId>, name: "Anna", surname: "Ivanova" }`.
2. Read response body.

**Expected test case results:** HTTP `200`. Body returns both `communicationDataId` and `contactPersonId`. No additional fields in the response.

---

### TC-FE-3 (Positive): SPRequest — EDIT existing additional contact person

**Description:** The frontend supports editing an already-created additional contact. Verify the EDIT flow when both IDs are passed in.

**Preconditions:**
1. Complete shared steps 1–4.

**Test steps:**
1. Using `SPRequest`, call the endpoint with `{ source: "Sales_Portal", communicationDataId: <from step 4>, contactPersonId: <from step 4>, title: <titleId>, name: "John", middleName: "P2", surname: "Doe" }`.
2. Inspect response.

**Expected test case results:** HTTP `200`. Response returns the **same** `communicationDataId` and `contactPersonId` as passed in — the UI can keep using the same row id for re-render.

---

### TC-FE-4 (Positive): SPRequest — Source = "Self_Service_Portal"

**Description:** Some Sales Portal flows run with `source = "Self_Service_Portal"`. The API must accept it for the frontend to reuse the same UI logic.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call the endpoint with `source = "Self_Service_Portal"` + mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `200`. Response returns both IDs.

---

### TC-FE-5 (Positive): SPRequest — multiple phones and emails accepted

**Description:** The UI lets the user input several phone numbers and emails. The API must accept them as arrays.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call CREATE with `phones: ["+359001","+359002","+359003"]` and `emails: ["e1@x.com","e2@x.com"]` plus mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `200`. Both IDs returned. Follow-up read via `GET /customer/{customerId}` shows all three phones and both emails on the contact person (the frontend can render them).

---

### TC-FE-6 (Negative): SPRequest — missing OAuth token returns 401 (UI must redirect to login)

**Description:** When the Sales Portal frontend loses its token, the request MUST return `401` so the frontend can redirect to auth.

**Preconditions:**
1. Complete shared step 3 for entity setup. Do NOT attach `Authorization` header on the call under test.

**Test steps:**
1. Call the endpoint without `Authorization` header but with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `401 Unauthorized`. Response includes no business payload. The UI can detect this and trigger the OAuth login flow.

---

### TC-FE-7 (Negative): SPRequest — invalid / expired bearer token

**Description:** An invalid or expired SP token MUST return `401`.

**Preconditions:**
1. Complete shared step 3.

**Test steps:**
1. Call the endpoint with `Authorization: Bearer invalid.jwt.value`.
2. Inspect response.

**Expected test case results:** HTTP `401`. UI treats the session as expired.

---

### TC-FE-8 (Negative): SPRequest — validation surfaces missing mandatory fields

**Description:** The frontend depends on the API returning a structured validation error when the user skips required fields. Simulate the case where the UI form bypasses client-side validation and the backend catches it.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call the endpoint with body `{}`.
2. Inspect response body.

**Expected test case results:** HTTP `400 Bad Request`. Response body lists all four missing mandatory fields (`source`, `title`, `name`, `surname`). The UI can render these directly next to the corresponding form fields.

---

### TC-FE-9 (Negative): SPRequest — invalid `source` enum

**Description:** When the UI builds the request with a bad `source` value (e.g. due to a config mismatch), the API MUST reject it with a clear error.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call with `source = "RANDOM"` plus mandatory fields.
2. Inspect response.

**Expected test case results:** HTTP `400`. Error identifies `source` as invalid enum. The UI can display a generic "unexpected value" error.

---

### TC-FE-10 (Negative): SPRequest — non-existent `customerUic`

**Description:** When the UI loads a customer that no longer exists (or the route is hit directly with a bad UIC), the API must return a 404-like response so the frontend can show a "customer not found" state.

**Preconditions:**
1. Complete shared step 1 (token).

**Test steps:**
1. Using `SPRequest`, call `POST /sales-portal/additional-contacts/9999999999999/1` with valid body.
2. Inspect response.

**Expected test case results:** HTTP `404` (or `400`). Response body references missing customer — frontend can redirect to the customer list / show error banner.

---

### TC-FE-11 (Negative): SPRequest — PUT method not allowed

**Description:** If a client (e.g. an older frontend build or a typo) sends `PUT` to this path, the service MUST NOT process the upsert. The ticket title says "PUT" but the implementation is POST; the UI must target POST.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call `PUT /sales-portal/additional-contacts/{customerUic}/{versionId}` with a valid body.
2. Inspect response.

**Expected test case results:** HTTP `404` or `405 Method Not Allowed`. No contact person is created; the UI fails loud and can display an unexpected-error toast.

---

### TC-FE-12 (Positive): SPRequest — response contract used by frontend is stable

**Description:** The Sales Portal frontend reads exactly `communicationDataId` and `contactPersonId` to update its local state. The response schema must not change.

**Preconditions:**
1. Complete shared steps 1–3.

**Test steps:**
1. Using `SPRequest`, call CREATE with mandatory fields.
2. Parse response JSON and validate keys.

**Expected test case results:** HTTP `200`. Response JSON has exactly the two keys `communicationDataId` and `contactPersonId` (both numeric). No extra fields that could confuse the UI.

---

## References

- **Jira:** PHN-2214 – Sales Portal — Upsert Additional Contact Person.
- **API consumed by frontend:** `POST /sales-portal/additional-contacts/{customerUic}/{versionId}`.
- **Token flow:** Sales Portal OAuth2 client-credentials (`salesPortalTokenAuth()` + `SPRequest` fixture).
- **Template:** `Cursor-Project/config/template/Test_case_template.md`.
- **Layout rule:** `.cursor/rules/workspace/test_cases_structure.mdc`.
