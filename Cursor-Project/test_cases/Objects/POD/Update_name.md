# POD Update Name – Put / Update existing POD (name only) (Technical story)

**Jira:** — (Technical User Story; Confluence: Put Update existing POD)  
**Type:** Task / Feature  
**Summary:** The API `PUT /api/pod/{id}` updates only the **name** of an existing Point of Delivery (POD) on the current version. It does not create a new version. This document covers all positive, negative, and regression scenarios for that flow.

**Scope:** This document covers the flow where a user (e.g. Sales agent) updates the customer-facing POD name from the Sales portal. The system accepts path parameter `id` (POD DB id) and body `versionId` (Integer) and `name` (String, 1–1024 chars, non-blank). Only the `name` attribute of the current POD version is updated in `pod.pod_details`; no new version is created. Valid requests return 200 with `{ id, podDetailId, versionId }`. Validation failures return 400 with per-field messages; missing POD or version returns 404. Downstream consumers (GET /pod/{id}, GET /pod/list, contract screens) must see the updated name.

---

## Test data (preconditions)

- **Environment:** Dev 1, Dev 2, or Test (as per release plan).
- **POD:** A POD exists in `pod.pod` with at least one row in `pod.pod_details` (current version identified by `version_id`).
- **Auth:** Caller has permission to update POD (e.g. Sales portal / Sales agent); request includes valid bearer token and headers per Phoenix API standards (Content-Type: application/json).
- **Data:** For negative tests, use non-existent `id` or `versionId`, or invalid body (blank name, name length &gt; 1024, missing versionId, versionId &lt; 1) as specified per TC.

---

## TC-1 (Positive): Valid POD id, versionId, and name – success and name updated in DB

**Objective:** Verify that when the user sends a valid PUT request with existing POD id, existing versionId for that POD, and a valid name (1–1024 characters, non-blank), the system returns 200 OK, the response contains id, podDetailId, and versionId, and the POD detail name is updated in the database.

**Preconditions:**
1. A POD exists with a known id and at least one pod_details row with a known versionId.
2. The caller is authenticated and has permission to update the POD.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with path `id` equal to the existing POD id.
2. Send JSON body with `versionId` equal to an existing pod_details.version_id for that POD and `name` equal to a non-blank string of length between 1 and 1024 (e.g. "Updated POD Name").
3. Observe the HTTP status and response body.
4. Query the database (or call GET /pod/{id}) and confirm that `pod.pod_details.name` for the row (pod_id = id, version_id = versionId) equals the submitted name and that no new pod_details row was created.

**Expected result:** HTTP 200 OK. Response body is a single object with fields `id` (POD id), `podDetailId` (pod_details.id), and `versionId`. The name in `pod.pod_details` for that version is updated to the request value. No new POD version is created.

**References:** Confluence: Put Update existing POD; validation: name @NotBlank, @Size(1,1024).

---

## TC-2 (Negative): Name blank – 400 with validation message

**Objective:** Verify that when the user sends a valid POD id and versionId but the name is blank (empty string or only whitespace), the system returns 400 and a validation error message for the name field, and the POD name is not updated.

**Preconditions:**
1. A POD exists with a known id and versionId.
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid `id` and body containing valid `versionId` and `name` set to "" (or a string of only spaces).
2. Observe the HTTP status and response body (validation error message).

**Expected result:** HTTP 400 Bad Request. Response includes a validation message for the name field, e.g. "name-Name can not be blank;". No update is applied to pod_details.name.

**References:** Technical details: name @NotBlank; validation message "name-Name can not be blank;".

---

## TC-3 (Negative): Name length greater than 1024 – 400 with validation message

**Objective:** Verify that when the user sends a name longer than 1024 characters, the system returns 400 with a validation message and does not update the POD name.

**Preconditions:**
1. A POD exists with a known id and versionId.
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid `id` and body containing valid `versionId` and `name` set to a string of length 1025 (or more).
2. Observe the HTTP status and response body.

**Expected result:** HTTP 400 Bad Request. Response includes a validation message for the name field, e.g. "name-Name size should be between 1 and 1024;". No update is applied.

**References:** name @Size(1,1024); validation message "name-Name size should be between 1 and 1024;".

---

## TC-4 (Negative): versionId missing in body – 400 with validation message

**Objective:** Verify that when the request body does not include versionId, the system returns 400 with a validation message indicating that version ID is mandatory, and no update is performed.

**Preconditions:**
1. A POD exists with a known id.
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid `id` and body containing only `name` (valid non-blank string), omitting `versionId`.
2. Observe the HTTP status and response body.

**Expected result:** HTTP 400 Bad Request. Response includes a validation message for versionId, e.g. "versionId-Version ID is mandatory;". No update is applied.

**References:** versionId mandatory; validation message "versionId-Version ID is mandatory;".

---

## TC-5 (Negative): versionId invalid (e.g. zero or negative) – 400 with validation message

**Objective:** Verify that when versionId is less than 1 (e.g. 0 or negative), the system returns 400 with a validation message and does not update the POD.

**Preconditions:**
1. A POD exists with a known id.
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid `id` and body containing `versionId` set to 0 (or a negative integer) and a valid `name`.
2. Observe the HTTP status and response body.

**Expected result:** HTTP 400 Bad Request. Response includes a validation message for versionId, e.g. "versionId-Version ID must be at least 1;". No update is applied.

**References:** versionId @Min(1); validation message "versionId-Version ID must be at least 1;".

---

## TC-6 (Negative): POD id does not exist – 404

**Objective:** Verify that when the path parameter id does not correspond to any existing POD in the database, the system returns 404 and does not perform any update.

**Preconditions:**
1. Use a non-existent POD id (e.g. a very high number or an id that was never created).
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with a non-existent `id` and body containing a valid `versionId` (e.g. 1) and valid `name`.
2. Observe the HTTP status and response body.

**Expected result:** HTTP 404 Not Found. No update is applied. Response indicates that the POD was not found (or equivalent).

**References:** Logic: resolve POD by id; not found → 404.

---

## TC-7 (Negative): versionId does not exist for the given POD – 404

**Objective:** Verify that when the POD id exists but the provided versionId does not match any pod_details row for that POD, the system returns 404 and does not update any row.

**Preconditions:**
1. A POD exists with a known id; identify the actual version_id(s) in pod_details for this POD.
2. Use a versionId that does not exist for this POD (e.g. a different POD’s version_id or a non-existent version number).
3. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid existing `id` and body containing `versionId` that does not exist for this POD, and a valid `name`.
2. Observe the HTTP status and response body.

**Expected result:** HTTP 404 Not Found. No update is applied. Response indicates that the POD version was not found (or equivalent).

**References:** Logic: resolve pod_details by (pod_id, version_id); not found → 404.

---

## TC-8 (Positive): Update does not create a new version – only current version name changed

**Objective:** Verify that the API updates only the existing pod_details row identified by (pod_id, version_id) and does not create a new version, change last_pod_detail_id, or add a new row to pod_details.

**Preconditions:**
1. A POD exists with a known id and a known versionId; record the current pod_details row count and last_pod_detail_id (if applicable) for that POD.
2. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with valid `id`, `versionId`, and `name`.
2. After receiving 200, query the database: count of pod_details rows for this pod_id and the pod.last_pod_detail_id (if exposed).
3. Confirm that the name of the targeted pod_details row was updated and that no new pod_details row was created and last_pod_detail_id did not change (if applicable).

**Expected result:** HTTP 200 OK. Only the existing pod_details row’s name is updated. No new version is created; row count for that POD is unchanged; last_pod_detail_id is unchanged.

**References:** Business rule: update always on existing POD version; system must not create a new version.

---

## TC-9 (Positive): GET /pod/{id} returns updated name after successful PUT (regression)

**Objective:** Verify that after a successful name update via PUT /api/pod/{id}, a subsequent GET /pod/{id} (or equivalent) returns the updated name, so that downstream consumers (Sales portal, contract screens, listings) see the new value.

**Preconditions:**
1. A POD exists with a known id and versionId.
2. The caller is authenticated for both PUT and GET.

**Steps:**
1. Optionally call GET /pod/{id} and note the current name for the version.
2. Send PUT request to `/api/pod/{id}` with valid `id`, `versionId`, and a new `name` (e.g. "POD Name After Update").
3. Confirm response is 200 and response body contains id, podDetailId, versionId.
4. Call GET /pod/{id} (and if applicable GET /pod/list or contract/POD APIs that display the name).
5. Confirm that the returned POD data includes the updated name for the updated version.

**Expected result:** After PUT 200, GET /pod/{id} (and any listing or contract screens that use POD name) show the updated name. Downstream integration (Sales portal, contract–POD UIs) sees the new name.

**References:** Downstream: GET /pod/{id}, GET /pod/list; integration points from cross-dependency analysis.

---

## TC-10 (Negative): Invalid path id format (e.g. non-numeric) – 400 or 404

**Objective:** Verify that when the path parameter id is not a valid format (e.g. non-numeric string), the system returns 400 Bad Request or 404 and does not perform any update.

**Preconditions:**
1. The caller is authenticated.

**Steps:**
1. Send PUT request to `/api/pod/{id}` with `id` set to an invalid value (e.g. "abc" or "invalid") and body with valid `versionId` and `name`.
2. Observe the HTTP status.

**Expected result:** HTTP 400 Bad Request or 404 Not Found (implementation-dependent). No update is applied to the database.

**References:** Request validation: id (path) mandatory, valid POD id else 404.

---

## References

- **Confluence:** [Put Update existing POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740229121) (Phoenix space, page id 740229121).
- **Endpoint:** PUT /api/pod/{id}. Body: versionId (Integer), name (String, 1–1024, non-blank). Response 200: { id, podDetailId, versionId }.
- **Related:** PointOfDeliveryController, PointOfDeliveryService, PointOfDeliveryDetailsRepository; Sales portal (re-contracting flow); GET /pod/{id}, GET /pod/list; contract–POD screens and APIs.
- **Cross-dependency:** See Cursor-Project/cross_dependencies/2026-03-11_put-update-pod-name-only.json for what_could_break and integration points.
