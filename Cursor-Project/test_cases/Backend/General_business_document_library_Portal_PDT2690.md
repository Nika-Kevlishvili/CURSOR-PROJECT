# Resources (Operations management) – business document file library API (PDT-2690)

**Jira:** PDT-2690 (Phoenix Delivery)  
**Type:** Customer Feedback / feature (backend slice: `resource-file` API)  
**Summary:** Backend test cases for the Resources feature: listing, upload, download, metadata edit, delete, sorting, search prompt behaviour, permission gates, and validation aligned with `ResourceFileController` / `ResourceFileService` on aligned Phoenix **dev** branches.

**Scope:** Validates Core API under `/resource-file`: `GET /resource-file/filter` returns **206 Partial Content** with a page of items; `POST /resource-file` (multipart) creates a row and stores file via `FileService`; `GET /resource-file/{id}` (download path uses `/download/{id}`); `DELETE /resource-file/{id}` removes DB row; `PUT /resource-file` updates name and optionally replaces file. Permissions: `RESOURCE_FILE_VIEW`, `RESOURCE_FILE_CREATE`, `RESOURCE_FILE_EDIT`, `RESOURCE_FILE_DELETE`. Name validation: `@Size(min=3, max=512)` on create/edit requests. File size limit: **100 MB** in service. List filter uses case-insensitive **partial** `LIKE` on `name` when `prompt` is non-blank (`EPBStringUtils.fromPromptToQueryParameter`). **Note:** There is **no** server-side minimum length on `prompt`; the **3-character minimum** is enforced in the UI filter form only.

**Phoenix alignment (this analysis):** Environment **dev** per Jira comments (Yoana Prodanova: implement in Dev). On **2026-05-06**, `switch-phoenix-branches.ps1 -Environment dev` exited **2** (partial): `mfe-poc-with-nx` and `phoenix-migration` reported **missing-remote** (`origin/dev` not found); `phoenix-core`, `phoenix-core-lib`, `phoenix-ui`, and other core repos were **already-aligned**. Log: `.cursor/logs/switch-phoenix-branches-dev-20260506-124521.log` (workspace root). Conclusions below are from the local tree state at analysis time (**mixed-remote** caveat for the two repos without `origin/dev`).

---

## Cross-dependency summary (for regression)

| Area | Detail |
|------|--------|
| **Entry points** | `ResourceFileController` (`/resource-file`), `ResourcesService` (Angular), `resource.files` DB table + FTP path `resource_files` |
| **Upstream** | `FileService` (FTP upload/download), `ResourceFileRepository.filter` (native query on `resource.files`) |
| **Downstream** | Portal UI Resources page; any consumer calling `/resource-file/*` |
| **what_could_break** | Changes to `EPBStringUtils.fromPromptToQueryParameter`, permission enums, multipart contract, HTTP status on filter (206), max file size constant, sort column mapping (`ResourceFileListColumns.DATE` → `dateOfCreation`, `NAME` → `name`), FTP base path |
| **Related Jira (parent & splits)** | **PDT-2690** (parent). Subtasks/siblings: **PDT-2805** (Frontend), **PDT-2806** (DB). Targeted regressions: **PDT-2833** ([Backend] listing sort by creation date), **PDT-2835** ([Frontend] choose-file control when user has view-only/create mismatch) |

---

## Test data (preconditions)

Shared setup for backend tests in an integration/API environment (e.g. **Dev**).

1. **Environment:** Use the target Phoenix Core API base URL from your test configuration (e.g. `BASE_URL` / Core URL used by EnergoTS). Confirm Swagger for that environment lists `/resource-file` operations (refresh specs before automation per Rule SWAGGER.0). **Swagger vs runtime:** `GetResourceFileListRequest` exposes `sortBy` enum `DATE`|`NAME` and **`direction`** `ASC`|`DESC` (not `sortDirection`). OpenAPI may show **200** for `GET /resource-file/filter`; the controller returns **206 Partial Content** — assert **runtime status** when automating.
2. **Authentication — obtain JWT:** Authenticate via the **standard Phoenix portal login flow** used by your automation (e.g. EnergoTS `tokenAuth()` / credentials from `.env`). Record the bearer token for API calls.
3. **Permission personas (no single Phoenix “assign permission” endpoint assumed here):** For each test, use a user account whose **effective** permission set already includes only the permissions under test. Prepare accounts in advance per your **role / IAM administration process** (or lab personas), for example:
   - **User `FULL`:** `RESOURCE_FILE_VIEW`, `RESOURCE_FILE_CREATE`, `RESOURCE_FILE_EDIT`, `RESOURCE_FILE_DELETE`.
   - **User `VIEW_ONLY`:** `RESOURCE_FILE_VIEW` only.
   - **User `CREATE_VIEW`:** `RESOURCE_FILE_VIEW` + `RESOURCE_FILE_CREATE` (no edit/delete).
   - **User `NO_RESOURCE`:** none of the `RESOURCE_FILE_*` permissions (any other portal permissions as needed to obtain a token).
4. **Multipart client:** Use a client that sends **`multipart/form-data`** with a **`file`** part for `POST` and `PUT` (e.g. Playwright `FileUploadRequest` / `FormData` per `Cursor-Project/config/playwright_generation/playwright instructions/project-description.md`).
5. **Reference implementation (code):** `Cursor-Project/Phoenix/phoenix-core/.../ResourceFileController.java`, `Cursor-Project/Phoenix/phoenix-core-lib/.../ResourceFileService.java`, `Cursor-Project/Phoenix/phoenix-core-lib/.../ResourceFileRepository.java`, `CreateResourceFileRequest.java`, `GetResourceFileListRequest.java`, `EditResourceFileRequest.java`.

---

## Backend Test Cases

### TC-BE-1 (Positive): List resource files with blank search — HTTP 206 and paged body

**Description:** Verify `GET /resource-file/filter` returns **206** and a page of items when `prompt` is empty or omitted (converted to no `LIKE` filter in SQL).

**Preconditions:**
1. Complete steps 1–4 from Test data using **User `FULL`** (or any user with `RESOURCE_FILE_VIEW`).
2. Ensure at least zero or more existing `ACTIVE` files may exist; test is valid for empty or non-empty catalog.

**Test steps:**
1. Call `GET /resource-file/filter?page=0&size=25&prompt=` (and/or omit `prompt` per Swagger binding) with `Authorization: Bearer <token>`.

**Expected test case results:** Response status **206 Partial Content**. Body is a **page** object with `content` array; each element exposes listing fields consistent with `ResourceFileListingResponse` (e.g. `id`, `name`, `dateOfCreation`, `fileUrl` as implemented). No error payload.

**References:** `ResourceFileController.filter` → `HttpStatus.PARTIAL_CONTENT`; `ResourceFileRepository.filter` (`prompt` null branch).

---

### TC-BE-2 (Positive): Create resource file — HTTP 201 and persisted record

**Description:** Verify multipart upload creates a DB entity and returns **201** with `ResourceFileResponse`.

**Preconditions:**
1. Authenticate as **User `FULL`** (includes `RESOURCE_FILE_CREATE` and `RESOURCE_FILE_VIEW`).
2. Prepare a small binary **`file`** (any extension permitted by platform) under **100 MB**.

**Test steps:**
1. `POST /resource-file` with `multipart/form-data`: part `file` = prepared file; provide **`name`** query parameter or request part exactly as required by Swagger for `CreateResourceFileRequest` (typical UI sends `name` URL-encoded; **verify field binding in Swagger** before automating).
2. Use `name` = a unique string between **3** and **512** characters (e.g. `doc_` + timestamp).

**Expected test case results:** Status **201 Created**. Response body contains new `id` and `name` equal to trimmed request name. Row is visible in subsequent `GET /resource-file/filter` for **User `FULL`**.

**References:** `ResourceFileController.create`; `ResourceFileService.create`.

---

### TC-BE-3 (Positive): Filter by partial name — case-insensitive substring match

**Description:** Verify `prompt` filters by **partial** match on stored **display name** (not original upload filename only).

**Preconditions:**
1. As **User `FULL`**, create two files via `POST` with distinct `name` values, e.g. `AlphaPolicy_2026_HR` and `Beta_Notice_v2`, so both exist in the listing.

**Test steps:**
1. `GET /resource-file/filter?page=0&size=25&prompt=pol` (example middle).

**Expected test case results:** Status **206**. `content` includes only rows whose `lower(name)` matches `%pol%` (e.g. `AlphaPolicy_2026_HR`), and excludes non-matching names.

**References:** `EPBStringUtils.fromPromptToQueryParameter`; `ResourceFileRepository.filter` `lower(f.name) like :prompt`.

---

### TC-BE-4 (Positive): Get resource file by id — HTTP 200

**Description:** Verify read-by-id returns metadata for an existing file.

**Preconditions:**
1. As **User `FULL`**, create one file (`POST`) and capture returned `id`.

**Test steps:**
1. `GET /resource-file?id=<id>` with **User `VIEW_ONLY`** or **User `FULL`** token.

**Expected test case results:** Status **200**. Body matches `ResourceFileResponse` for that id (includes `name`, `fileUrl`, etc. per DTO).

**References:** `ResourceFileController` `GET` mapping; `ResourceFileService.view`.

---

### TC-BE-5 (Positive): Download resource file — HTTP 200 and attachment headers

**Description:** Verify binary download returns bytes and `Content-Disposition` attachment.

**Preconditions:**
1. As **User `FULL`**, create a file with known content (e.g. small text file) and note `id`.

**Test steps:**
1. `GET /resource-file/download/{id}` with a user having `RESOURCE_FILE_VIEW`.

**Expected test case results:** Status **200**. Headers include `Content-Disposition: attachment; filename=...` (encoding per `UrlEncodingUtil.encodeFileName`). Body bytes are non-empty and match stored file content.

**References:** `ResourceFileController.download`; `ResourceFileService.download` / `buildDownloadName`.

---

### TC-BE-6 (Positive): Delete resource file — HTTP 200

**Description:** Verify delete removes the entity when caller has delete permission.

**Preconditions:**
1. As **User `FULL`**, create a file and capture `id`.

**Test steps:**
1. `DELETE /resource-file/{id}` with **User `FULL`**.
2. Repeat `GET /resource-file?id=<id>` or list filter expecting **404** on get (per `DomainEntityNotFoundException` behaviour).

**Expected test case results:** Delete returns **200** (empty body). Subsequent read returns **404** or entity absent from listing.

**References:** `ResourceFileController.delete`; `ResourceFileService.delete`.

---

### TC-BE-7 (Positive): Edit display name only — HTTP 200

**Description:** Verify `PUT` updates `name` without new `file` part.

**Preconditions:**
1. As **User `FULL`**, create file; capture `id` and original `fileUrl`.

**Test steps:**
1. `PUT /resource-file` multipart with **`file` omitted or empty**, `id` and new `name` (3–512 chars) per `EditResourceFileRequest` and Swagger.

**Expected test case results:** Status **200**. `name` updated; `fileUrl` unchanged when no new file supplied.

**References:** `ResourceFileService.edit` branch `file == null || file.isEmpty()`.

---

### TC-BE-8 (Positive): Edit with replacement file — HTTP 200 and new storage URL

**Description:** Verify uploading a new `file` replaces stored URL.

**Preconditions:**
1. As **User `FULL`**, create file; capture `id`.

**Test steps:**
1. `PUT /resource-file` with new **`file`** part and valid `name`, per Swagger.

**Expected test case results:** Status **200**. `fileUrl` changes (new UUID prefix path). Download returns new file bytes.

**References:** `ResourceFileService.edit` when `file != null`.

---

### TC-BE-9 (Positive): Pagination — page and size honoured

**Description:** Verify Spring `PageRequest` paging with `page` and `size`.

**Preconditions:**
1. As **User `FULL`**, ensure more than `size` rows exist (create N+1 files if needed).

**Test steps:**
1. `GET /resource-file/filter?page=0&size=5&prompt=`  
2. `GET /resource-file/filter?page=1&size=5&prompt=`

**Expected test case results:** First response `content.length` ≤ 5; second page returns next slice; `totalElements` stable across pages.

**References:** `ResourceFileService.filter` `PageRequest.of(request.getPage(), request.getSize(), ...)`.

---

### TC-BE-10 (Positive): Sort by name ascending and descending

**Description:** Verify `sortBy` maps to JPA sort field `name`.

**Preconditions:**
1. As **User `FULL`**, create files with names that sort distinctly (e.g. `A_doc`, `Z_doc`).

**Test steps:**
1. `GET /resource-file/filter?page=0&size=25&prompt=&sortBy=NAME&direction=ASC` (flattened query per `GetResourceFileListRequest` in OpenAPI — param name **`direction`**).
2. Repeat with `direction=DESC`.

**Expected test case results:** Ordering of `content` by `name` matches requested direction.

**References:** `ResourceFileListColumns.NAME`; `ResourceFileService.getSortByEnum`.

---

### TC-BE-11 (Positive): Sort by creation date ascending and descending

**Description:** Default story (**PDT-2833**) expects correct ordering by **creation date**; API maps `sortBy=DATE` to repository column `create_date` aliased as `dateOfCreation`. Support **ASC** and **DESC**.

**Preconditions:**
1. As **User `FULL`**, create two files in sequence via `POST /resource-file` (different creation timestamps).

**Test steps:**
1. Call filter with `sortBy=DATE&direction=ASC`, then repeat with `sortBy=DATE&direction=DESC` (same `page`, `size`, empty `prompt`).

**Expected test case results:** Rows ordered by creation time ascending for `ASC`, descending for `DESC` (compare `dateOfCreation` in `content[]`).

**References:** `ResourceFileListColumns.DATE` (`dateOfCreation`); `ResourceFileService.getSortByEnum`; `ResourceFileRepository.filter`.

---

### TC-BE-12 (Negative): Create without RESOURCE_FILE_CREATE — forbidden

**Description:** Verify permission validator rejects create when user lacks `RESOURCE_FILE_CREATE`.

**Preconditions:**
1. Token for **User `VIEW_ONLY`** (has `RESOURCE_FILE_VIEW` only).

**Test steps:**
1. `POST /resource-file` with valid multipart body as in TC-BE-2.

**Expected test case results:** Response is **403 Forbidden** (or project's standard permission-denied contract — **assert per your global error schema**).

**References:** `@PermissionValidator` on `ResourceFileController.create`.

---

### TC-BE-13 (Negative): Filter without RESOURCE_FILE_VIEW — forbidden

**Preconditions:**
1. Token for **User `NO_RESOURCE`**.

**Test steps:**
1. `GET /resource-file/filter?page=0&size=25&prompt=`

**Expected test case results:** **403** (permission denied).

**References:** `@PermissionValidator` on `filter`.

---

### TC-BE-14 (Negative): Delete without RESOURCE_FILE_DELETE — forbidden

**Preconditions:**
1. **User `FULL`** creates file; capture `id`.
2. Switch to token **User `CREATE_VIEW`** (no delete).

**Test steps:**
1. `DELETE /resource-file/{id}`

**Expected test case results:** **403**. Row still exists when queried with **User `FULL`**.

**References:** `ResourceFileController.delete` permission mapping.

---

### TC-BE-15 (Negative): Create with name shorter than 3 characters — validation error

**Preconditions:**
1. **User `FULL`**, valid small `file` part.

**Test steps:**
1. `POST /resource-file` with `name` = `AB` (2 chars).

**Expected test case results:** **400** (or 422 per global validation handler) with message containing constraint hint from `CreateResourceFileRequest`: `name-File name length should be from {min} to {max} characters;`

**References:** `CreateResourceFileRequest` `@Size(min = 3, max = 512)`.

---

### TC-BE-16 (Negative): Create with blank name — validation error

**Test steps:**
1. `POST /resource-file` with `name` = `""` or whitespace-only if allowed by transport.

**Expected test case results:** **400** with `name-File name should not be blank;` (trim may apply in service, but `@NotBlank` should fail first).

**References:** `CreateResourceFileRequest` `@NotBlank`.

---

### TC-BE-17 (Negative): Upload file larger than 100 MB — rejected

**Preconditions:**
1. **User `FULL`**. Prepare or generate a **`file`** part with declared size **> 100 * 1024 * 1024** bytes.

**Test steps:**
1. `POST /resource-file` with that file and valid `name`.

**Expected test case results:** **400** with client error indicating file size (implementation: `ClientException` `"fileUrl-File size exceeds 100 MB;"`, `ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED`).

**References:** `ResourceFileService.validateFileSize`.

---

### TC-BE-18 (Negative): Get by id — non-existent id returns 404

**Preconditions:**
1. **User `FULL`**.

**Test steps:**
1. `GET /resource-file?id=999999999` (ID unlikely to exist).

**Expected test case results:** **404** `DomainEntityNotFoundException` message pattern `Resource file with ID ... not found`.

**References:** `ResourceFileService.view`.

---

### TC-BE-19 (Negative): Delete non-existent id — 404

**Preconditions:**
1. **User `FULL`**.

**Test steps:**
1. `DELETE /resource-file/999999999`

**Expected test case results:** **404**.

**References:** `ResourceFileService.delete`.

---

### TC-BE-20 (Positive): Short search prompt (1–2 chars) — API still applies LIKE filter

**Description:** Documents divergence from UI: Angular filter uses `Validators.minLength(3)` on search; **backend** accepts any non-blank `prompt` and builds `%trim(lower(prompt))%`.

**Preconditions:**
1. **User `FULL`**, at least one file whose `name` contains substring `ab`.

**Test steps:**
1. `GET /resource-file/filter?prompt=ab&page=0&size=25`

**Expected test case results:** **206**; filtering applies (rows match `%ab%`). No 400 solely due to prompt length.

**References:** `GetResourceFileListRequest` (no `@Size` on prompt); `EPBStringUtils.fromPromptToQueryParameter`; `ResourcesListFiltersComponent` `Validators.minLength(3)` (UI only).

---

### TC-BE-21 (Negative): Edit without RESOURCE_FILE_EDIT — forbidden

**Preconditions:**
1. **User `FULL`** creates file; capture `id`.
2. Token **User `CREATE_VIEW`** (has create + view, no edit).

**Test steps:**
1. `PUT /resource-file` with valid `EditResourceFileRequest` and optional file.

**Expected test case results:** **403**.

**References:** `ResourceFileController.edit` `@PermissionValidator` `RESOURCE_FILE_EDIT`.

---

### TC-BE-22 (Positive): Default sort column — omit `sortBy`, supply `direction` (`DATE` implied)

**Description:** Validates **`PDT-2833`** regression path: When **`sortBy`** is omitted, `ResourceFileService.getSortByEnum` falls back to **`ResourceFileListColumns.DATE`** (`dateOfCreation`). **Note:** Omitting **`direction`** alone may bind `null` into `Sort.Order` and yield a server error in some setups — automation should send **`direction`** explicitly (e.g. `DESC`).

**Preconditions:**
1. As **User `FULL`**, create at least **two** files in sequence via `POST /resource-file` with distinct `name` values; note chronological order.

**Test steps:**
1. `GET /resource-file/filter?page=0&size=25&prompt=&direction=DESC` **without** a `sortBy` query parameter.
2. Repeat with `direction=ASC`.

**Expected test case results:** **206**; `content[]` sorted by **`dateOfCreation`** in the requested **`direction`** (newest-first for `DESC` when timestamps differ). Confirms **`sortBy` optional** binds to **`DATE`** in `ResourceFileService.getSortByEnum`.

**References:** `ResourceFileService.getSortByEnum`; `ResourceFileListColumns.DATE`.

---

## References

- Jira: PDT-2690 (requirements in custom field / comments; description empty in API payload at fetch time).
- Code: `Cursor-Project/Phoenix/phoenix-core/src/main/java/bg/energo/phoenix/controller/resource/ResourceFileController.java`
- Code: `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/resource/ResourceFileService.java`
- Code: `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/repository/resource/ResourceFileRepository.java`
