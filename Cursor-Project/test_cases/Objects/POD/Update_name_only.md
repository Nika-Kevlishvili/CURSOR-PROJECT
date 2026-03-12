# POD – Update existing POD name only (Put / Update existing POD)

**Jira:** — (Technical User Story, Confluence)  
**Type:** Task / Feature  
**Summary:** The API `PUT /api/pod/{id}` updates only the **name** of an existing Point of Delivery (POD) on the **current version** (no new version is created). This document covers all test scenarios for that behaviour: valid updates, validation errors, and not-found cases.

**Scope:** This document covers the flow where a user (e.g. from the Sales portal) updates the customer-facing name of an existing POD. The system must accept a valid POD id (path), versionId and name (body), update only the `name` field of the existing POD version in the database, and return a success response. Invalid or missing parameters must result in HTTP 400 with per-field error messages; a non-existent POD or version must result in HTTP 404. The system must **not** create a new POD version; only the existing row in `pod.pod_details` identified by (pod_id, version_id) must have its `name` updated.

**Source:** Confluence – [Put Update existing POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740229121/Put+Update+existing+POD). Environments: Dev 1, Dev 2, Test.

---

## Test data (preconditions)

- **Environment:** Dev 1, Dev 2, or Test (as per release plan).
- **POD:** A Point of Delivery (POD) exists in the system with a known `id` (POD DB id) and at least one version (row in `pod.pod_details` with a known `version_id`).
- **Authentication:** Caller has a valid bearer token and permissions to edit the POD (e.g. POD_EDIT as per Phoenix API standards).
- **API:** Phoenix API is available; endpoint `PUT /api/pod/{id}` with body `{ "versionId": <integer>, "name": "<string 1–1024 chars, non-blank>" }`.

---

## TC-1 (Positive): Update POD name with valid data – happy path

**Objective:** Verify that when the user sends a valid POD id (path), an existing versionId, and a valid name (1–1024 characters, non-blank), the system updates the POD name on the current version and returns HTTP 200 with the expected response shape. No new POD version is created.

**Preconditions:**
1. A POD exists with id = P (e.g. 12345) and has at least one version with version_id = V (e.g. 1).
2. The caller has permission to edit the POD and a valid bearer token.
3. The current name of the POD version may be any value; the test will set a new name.

**Steps:**
1. Prepare the request: `PUT /api/pod/{P}` with body `{ "versionId": V, "name": "Updated POD Name" }` (Content-Type: application/json, Authorization: Bearer &lt;token&gt;).
2. Send the request to the Phoenix API.
3. Assert the response status is 200.
4. Assert the response body contains the updated POD id, the pod detail id, and the versionId (e.g. id, podDetailId, versionId as in PodResponse).
5. Optionally, call GET /api/pod/{P}?versionId=V (or equivalent) and verify that the name field is "Updated POD Name" and that the version_id has not changed (no new version was created).

**Expected result:** The system returns HTTP 200. The response includes the POD id, pod detail id, and versionId. The name of the POD version identified by (P, V) is updated to "Updated POD Name". No new row is created in pod_details; the same version is updated. Other POD data (e.g. address, grid operator data) remain unchanged.

**References:** Confluence Put Update existing POD; Phoenix API PointOfDeliveryController, PointOfDeliveryService.

---

## TC-2 (Positive): Update POD name – minimum length (1 character)

**Objective:** Verify that a name of exactly one character is accepted and persisted, as the business rule allows name length 1–1024.

**Preconditions:**
1. A POD exists with a known id and versionId (as in TC-1).
2. Caller has valid token and permissions.

**Steps:**
1. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id>, "name": "A" }`.
2. Check response status and body.

**Expected result:** HTTP 200. The POD version name is updated to "A". No validation error for length.

**References:** Request validation: name @Size(1, 1024).

---

## TC-3 (Positive): Update POD name – maximum length (1024 characters)

**Objective:** Verify that a name of exactly 1024 characters is accepted and persisted.

**Preconditions:**
1. A POD exists with a known id and versionId.
2. Caller has valid token and permissions.

**Steps:**
1. Build a string of exactly 1024 characters (e.g. 1024 'x' or alphanumeric).
2. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id>, "name": "<1024 chars>" }`.
3. Check response status and body.

**Expected result:** HTTP 200. The name is updated to the 1024-character value. No validation error.

**References:** Request validation: name @Size(1, 1024).

---

## TC-4 (Positive): Verify only name is updated – other POD data unchanged

**Objective:** Ensure that the update flow changes only the `name` field of the POD details row; other fields (e.g. address, customer, version_id, activation/deactivation dates) and the POD header (e.g. last_pod_detail_id) are not modified.

**Preconditions:**
1. A POD exists with known id and versionId; record the current name and at least one other field (e.g. address or version_id).
2. Caller has valid token and permissions.

**Steps:**
1. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id>, "name": "New Name Only" }`.
2. On success, retrieve the POD (e.g. GET /api/pod/{id}?versionId=&lt;valid_version_id&gt;) or query the database for the same pod_details row.
3. Verify that the name is "New Name Only" and that version_id, pod_id, and other non-name fields are unchanged. Verify that no new pod_details row was created and that the POD header (e.g. last_pod_detail_id) is unchanged if applicable.

**Expected result:** Only the `name` column of the targeted pod_details row is updated. No new version is created; last_pod_detail_id and other POD header fields are not changed. Other POD detail fields remain as before.

**References:** Logic description: "Do not create a new version; do not change last_pod_detail_id or other POD header fields."

---

## TC-5 (Positive): Response shape contains id, podDetailId, versionId

**Objective:** Verify that the successful response body includes the expected fields (e.g. id, podDetailId, versionId) so that the Sales portal or other consumers can use the response without pagination.

**Preconditions:**
1. Same as TC-1.

**Steps:**
1. Send a valid `PUT /api/pod/{id}` request (valid id, versionId, name).
2. Parse the response body and assert presence of id (POD id), podDetailId (or equivalent), and versionId (or equivalent).

**Expected result:** Response is a single object (no pagination) containing at least id, podDetailId, and versionId with values matching the updated POD and version.

**References:** General description: "Return 200 with response including updated POD id, pod_detail id, versionId (e.g. PodResponse shape)."

---

## TC-6 (Negative): POD id not found – 404

**Objective:** Verify that when the path parameter `id` refers to a non-existent POD, the system returns HTTP 404 and does not perform any update.

**Preconditions:**
1. Use a POD id that does not exist in the system (e.g. a very high number or an id known to be deleted).
2. Body may be valid: `{ "versionId": 1, "name": "Some Name" }`.

**Steps:**
1. Send `PUT /api/pod/{non_existent_id}` with body `{ "versionId": 1, "name": "Some Name" }`.
2. Check response status and body.

**Expected result:** HTTP 404. No POD row is updated. Error message or payload indicates that the POD was not found (or equivalent).

**References:** Edge cases: "POD id not found → 404."

---

## TC-7 (Negative): versionId missing in body – 400

**Objective:** Verify that when versionId is omitted from the request body, the system returns HTTP 400 with a validation error (per-field message for versionId).

**Preconditions:**
1. A valid POD id exists.
2. Caller has valid token and permissions.

**Steps:**
1. Send `PUT /api/pod/{valid_id}` with body `{ "name": "Valid Name" }` (no versionId).
2. Check response status and body.

**Expected result:** HTTP 400. Response includes a validation error message indicating that versionId is required or invalid. No update is performed.

**References:** Request validation: "versionId mandatory."

---

## TC-8 (Negative): versionId does not exist for this POD – 404

**Objective:** Verify that when versionId does not match any pod_details row for the given POD, the system returns HTTP 404.

**Preconditions:**
1. A POD exists with id P and known versions (e.g. version_id 1 exists).
2. Use a versionId that does not exist for this POD (e.g. 99999 or a version that belongs to another POD).

**Steps:**
1. Send `PUT /api/pod/{P}` with body `{ "versionId": 99999, "name": "Valid Name" }`.
2. Check response status and body.

**Expected result:** HTTP 404. No pod_details row is updated. Error indicates that the POD version was not found (or equivalent). No new version is created.

**References:** Edge cases: "Missing/invalid versionId (no matching pod_details) → 404."

---

## TC-9 (Negative): name missing in body – 400

**Objective:** Verify that when the name field is omitted from the request body, the system returns HTTP 400 with a validation error for name.

**Preconditions:**
1. A valid POD id and versionId exist.
2. Caller has valid token and permissions.

**Steps:**
1. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id> }` (no name).
2. Check response status and body.

**Expected result:** HTTP 400. Validation error message indicates that name is required or invalid. No update is performed.

**References:** Request validation: "name mandatory."

---

## TC-10 (Negative): name blank or empty – 400

**Objective:** Verify that a blank or empty name (e.g. "" or only whitespace if not trimmed) is rejected with HTTP 400.

**Preconditions:**
1. Same as TC-9.

**Steps:**
1. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id>, "name": "" }`.
2. Optionally, repeat with `"name": "   "` (only spaces) if the API trims and treats as blank.
3. Check response status and body.

**Expected result:** HTTP 400. Per-field validation error for name (e.g. @NotBlank). No POD name is updated to blank.

**References:** Request validation: "name mandatory, String 1–1024 chars, non-blank else 400."

---

## TC-11 (Negative): name longer than 1024 characters – 400

**Objective:** Verify that a name exceeding 1024 characters is rejected with HTTP 400.

**Preconditions:**
1. Same as TC-9.

**Steps:**
1. Build a string of 1025 characters (or more).
2. Send `PUT /api/pod/{id}` with body `{ "versionId": <valid_version_id>, "name": "<1025+ chars>" }`.
3. Check response status and body.

**Expected result:** HTTP 400. Validation error indicates that name length must be between 1 and 1024 (or equivalent). No update is performed.

**References:** Request validation: name @Size(1, 1024).

---

## TC-12 (Negative): Invalid versionId type or format – 400

**Objective:** Verify that when versionId is not a valid integer (e.g. string, null, or wrong type), the system returns HTTP 400 at the validation layer.

**Preconditions:**
1. A valid POD id exists.
2. Caller has valid token and permissions.

**Steps:**
1. Send `PUT /api/pod/{id}` with body `{ "versionId": "not_an_integer", "name": "Valid Name" }` (or "versionId": null, or omit and send wrong type per API contract).
2. Check response status and body.

**Expected result:** HTTP 400. Validation or parsing error; per-field message for versionId where applicable. No update is performed.

**References:** Request validation: "versionId mandatory, Integer."

---

## TC-13 (Negative): No new version created after update

**Objective:** Verify that after a successful name update, the POD still has the same number of versions (pod_details rows) and the same version_id for the updated row; the system did not create a new version.

**Preconditions:**
1. A POD exists with known id and exactly one version (version_id = 1) or a known set of versions.
2. Record the current number of pod_details rows for this POD (e.g. via GET or DB query).

**Steps:**
1. Send a valid `PUT /api/pod/{id}` with body `{ "versionId": 1, "name": "Updated" }`.
2. After 200 response, query the number of pod_details rows for this POD (or list versions via API if available).
3. Compare with the count before the update; verify that the same version_id (1) still exists and that no new version_id was added.

**Expected result:** The number of pod_details rows for this POD is unchanged. The row with version_id = 1 has name = "Updated". No new row was inserted. Business rule: "Update always on existing POD version; system must not create a new version."

**References:** Logic description: "Do not create a new version."

---

## References

- **Confluence:** [Put Update existing POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740229121/Put+Update+existing+POD).
- **Endpoint:** `PUT /api/pod/{id}` (path: id = POD DB id; body: versionId, name).
- **Backend:** PointOfDeliveryController, PointOfDeliveryService, PointOfDeliveryDetailsRepository; validation layer (DTO + Jakarta Bean Validation).
- **DB:** pod.pod, pod.pod_details; only pod_details.name updated for (pod_id, version_id).
- **Source (Slack):** Technical User Story from Ani Giorganashvili, #ai-report, 2026-03-11.
