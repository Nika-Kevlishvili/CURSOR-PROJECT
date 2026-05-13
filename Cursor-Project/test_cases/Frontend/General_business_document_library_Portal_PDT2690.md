# Resources (Operations management) – business document file library UI (PDT-2690)

**Jira:** PDT-2690 (Phoenix Delivery)  
**Type:** Customer Feedback / feature (frontend: Angular Resources module)  
**Summary:** Frontend test cases for the **Resources** screen: navigation from Operations management, upload form (file picker, display name, save), list with search/sort/pagination, download and delete actions, edit flow, permission-based control disabling, validation messages, and confirmation dialogs. Aligns with `phoenix-ui` Resources components and the Jira user story (EN/BG labels).

**Scope:** Verifies UI behaviour against `ResourcesListComponent`, `ResourcesUploadFormComponent`, `ResourcesListFiltersComponent`, `ResourcesService`, and permission keys `resource_file_view`, `resource_file_create`, `resource_file_edit`, `resource_file_delete`. **Implementation note:** Jira story lists **create / view / delete**; the application also exposes **edit** (rename / replace file) — cases below include edit where present in code.

**Phoenix alignment (this analysis):** Environment **dev** (see Backend file). On **2026-05-06**, `switch-phoenix-branches.ps1 -Environment dev` exited **2** (`mfe-poc-with-nx`, `phoenix-migration` **missing-remote**). Log: `.cursor/logs/switch-phoenix-branches-dev-20260506-124521.log` (workspace root).

**Story vs UI (date column):** Jira asks for date **DD.MM.YYYY**. Current list template uses `optimizedDate:'dd.MM.yyyy HH:mm'` — expect **date + time** unless product changes UX.

---

## Cross-dependency summary (UI)

| Area | Detail |
|------|--------|
| **Entry points** | Route `/resources` (`app-constants` sidebar under Operations management), module `ResourcesModule` |
| **Services** | `ResourcesService` → Core `/resource-file` endpoints |
| **Shared components** | `phx-table`, `BaseListComponent` (sorting, pagination), `DialogService` (delete / save confirmations) |
| **Related Jira** | Parent **PDT-2690**; subtasks **PDT-2805** (Frontend), **PDT-2806** (DB); focused QA items **PDT-2833** (default / API sort behaviour), **PDT-2835** (Choose file UX when create permission absent) |

---

## Test data (preconditions)

1. **Environment:** Phoenix **portal** on **Dev** (per Jira confirmation thread). Use supported browsers per your QA matrix.
2. **Localization:** Run scenarios in **English** and repeat label-sensitive checks in **Bulgarian** if bilingual coverage is required (Jira specifies BG labels: e.g. “Файл за импорт”, “Ресурси”).
3. **User accounts:** Prepare portal users whose roles map to Phoenix permissions (same persona ideas as Backend file: `FULL`, `VIEW_ONLY`, `CREATE_VIEW`, users missing `resource_file_view` for download button disabled state, etc.). **How:** Use your organization’s user–role administration for the Dev environment; log in through the **standard Phoenix login page** (UI navigation, not API-only).
4. **Local test files:** Prepare (a) small `.txt` under 1 MB, (b) optional `.pdf` / `.xlsx` for “any file type” smoke, (c) a file **> 100 MB** (or mock size if tooling allows) for client-side validation, (d) optional file for edit-replace flow.
5. **Code reference:** `Cursor-Project/Phoenix/phoenix-ui/src/app/pages/resources/`, `.../shared/services/resources/resources.service.ts`, `app-constants.ts` sidebar entry `sidebar.resources` → `/resources`.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Navigate to Resources from Operations management

**Description:** Verify the Resources page is reachable from the burger menu **Operations management** section as specified in the story.

**Preconditions:**
1. Log in as a user with at least `resource_file_view` so the menu entry is available (if menu is permission-gated in your build, use **User `FULL`**).

**Test steps:**
1. Open the main navigation / burger menu.
2. Locate **Operations management** (or equivalent parent per translation).
3. Click **Resources** / **Ресурси** (per locale).

**Expected test case results:** Browser URL contains `/resources`. Page heading shows the translated resources title (`resources.title`). Upload form and list regions render.

**References:** `app-constants.ts` `link: '/resources'`, `subItemLabel: 'sidebar.resources'`.

---

### TC-FE-2 (Positive): Upload a new file — happy path

**Description:** User with **create** permission selects a file, enters display name, saves; row appears in the table.

**Preconditions:**
1. Log in as user with `resource_file_create` and `resource_file_view` (**User `FULL`** or `CREATE_VIEW`).

**Test steps:**
1. Click **Choose file** / translated equivalent.
2. Select a small test file from TC-FE preconditions.
3. Confirm **File for import** (read-only field) shows the **selected file name**.
4. Enter **File name** with 3–512 characters (e.g. `QA Resource 2026-05`).
5. Click **Save**.

**Expected test case results:** Success notification (e.g. `resources.success.uploaded`). New row appears in listing with **Name** = entered display name. **Date of creation** populated. No lingering unsaved state on form after success.

**References:** `ResourcesUploadFormComponent._doCreate`; `ResourcesListComponent.onFormSaved` refresh.

---

### TC-FE-3 (Positive): Search — partial match with minimum 3 characters

**Description:** Search field filters by display **File name** substring; at least **3** characters required before search is enabled.

**Preconditions:**
1. **User `FULL`**. Ensure at least two rows exist with distinct names (create via TC-FE-2 or API).

**Test steps:**
1. Type **2** characters in search — observe Search button state.
2. Type a **3+** character substring that matches only one existing **name**; click **Search**.

**Expected test case results:** With 1–2 characters, **Search** remains **disabled** (`searchForm.invalid` from `Validators.minLength(3)`). With 3+ valid chars, search runs and table shows only matching rows (partial / case-insensitive per backend).

**References:** `ResourcesListFiltersComponent` `Validators.minLength(3)`; `ResourcesListComponent` passes `prompt: filters?.search`.

---

### TC-FE-4 (Positive): Clear search restores full list

**Preconditions:**
1. Same as TC-FE-3 after a successful search narrowed the list.

**Test steps:**
1. Click the **clear** (circle delete) control on the filter row.

**Expected test case results:** Filters reset; listing reloads showing unfiltered page (or default sort/pagination).

**References:** `ResourcesListFiltersComponent.onClearSearchValue`.

---

### TC-FE-5 (Positive): Default sort — newest creation date first (PDT-2833)

**Description:** Story default order by **Date of creation**, newest on top (**PDT-2833** validates listing sort).

**Preconditions:**
1. **User `FULL`**, at least two files created at different times.

**Test steps:**
1. Open `/resources` (fresh load after creating rows in sequence).

**Expected test case results:** Default column sort is **date** **descending** (newest first) — `ResourcesListComponent` sets `sortListColumn = 'DATE'` and `sortDirection = DESC` on init; verify table indicator matches.

**References:** `ResourcesListComponent.ngOnInit`.

---

### TC-FE-6 (Positive): Change sort — date ascending / descending and name A–Z / Z–A

**Preconditions:**
1. **User `FULL`**, several rows with different names and dates.

**Test steps:**
1. Use table header sort on **Date of creation** to toggle ascending/descending.
2. Sort by **Name** ascending and descending.

**Expected test case results:** Order updates correctly after each click; network calls include `sortBy` (`DATE` \| `NAME`) and **`direction`** (`ASC` \| `DESC`) per `GetResourceFileListRequest` / `ResourcesService.getList` — not `sortDirection` (verify in DevTools).

**References:** `ResourcesListComponent` `getList` request object; `displayedColumns` sort fields `NAME`, `DATE`.

---

### TC-FE-7 (Positive): Download file from row actions

**Preconditions:**
1. **User `FULL`** with `resource_file_view`. At least one file exists.

**Test steps:**
1. Click **download** (circle download) on a row.

**Expected test case results:** Browser receives file download; downloaded filename combines row **name** + extension derived from `fileUrl` (see `ResourcesListComponent.onDownload`). No error toast.

**References:** `ResourcesService.download`; list template download button.

---

### TC-FE-8 (Positive): Delete — confirm **Yes** removes row

**Preconditions:**
1. **User `FULL`** with `resource_file_delete`. One expendable test file.

**Test steps:**
1. Click **delete** on the row.
2. In the modal, confirm **Yes** / **Да**.

**Expected test case results:** Modal closes; row disappears after refresh; backend row deleted (API 200). Message keys should align with global delete dialog pattern (`delete_resource_file_v2`).

**References:** `ResourcesListComponent.onDelete` → `onDeleteGroup`.

---

### TC-FE-9 (Positive): Delete — **No** cancels without delete

**Preconditions:**
1. Same as TC-FE-8.

**Test steps:**
1. Click **delete**; in modal choose **No** / **Не**.

**Expected test case results:** Modal closes; row **still present**; no delete API success.

---

### TC-FE-10 (Positive): Edit existing resource — rename and optional new file

**Description:** Row **Edit** loads data into upload form; save shows confirmation then updates.

**Preconditions:**
1. **User `FULL`** including `resource_file_edit`.

**Test steps:**
1. Click **edit** on a row.
2. Observe **File for import** shows existing `fileUrl` string; **File name** matches current name.
3. Change **File name** only; click **Save**; confirm system dialog (`save_resource_file_changes_v2`).
4. Repeat edit flow choosing a **new file** in file picker; save with confirmation.

**Expected test case results:** On confirmed save, success toast (`resources.success.changes_saved`). List refreshes with updated name; when new binary chosen, download reflects new content.

**References:** `ResourcesUploadFormComponent.startEdit`, `_confirmAndSave`, `_doUpdate`; `ResourcesService.update`.

---

### TC-FE-11 (Negative): View-only user — upload controls disabled / non-actionable (PDT-2835)

**Preconditions:**
1. Log in as **User `VIEW_ONLY`** (`resource_file_view` only).

**Test steps:**
1. Observe **Choose file** button and **File name** input; attempt **Save**.

**Expected test case results:** **Choose file** is **disabled** and uses **muted/grey styling** (`buttonClass` applies `!bg-grey-800` when `resource_file_create` is false for create-mode, versus `!bg-required` when true — see `resources-upload-form.component.html`). **`File for import`** remains a **disabled** read-only control (always `[inputDisabled]="true"`). **File name** input **disabled** (`!hasWritePerm()` for create flow). **Save** disabled (`canSave()` is false).

**References:** `resources-upload-form.component.html` `*ngIf` permission context and `[disabled]="!ctx.hasPerm"` on **Choose file**; `ResourcesUploadFormComponent.hasWritePerm` / `canSave`.

---

### TC-FE-12 (Negative): View-only — delete and edit actions disabled

**Preconditions:**
1. **User `VIEW_ONLY`**.

**Test steps:**
1. Inspect row action buttons.

**Expected test case results:** **Edit** and **Delete** buttons **disabled** (no `resource_file_edit` / `resource_file_delete`). **Download** enabled if `resource_file_view` present (`!disabled` on download when view).

**References:** `resources-list.component.html` `[disabled]` on action buttons.

---

### TC-FE-13 (Negative): File name validation — required, min length 3, max 512, no only-spaces

**Preconditions:**
1. **User `FULL`**.

**Test steps:**
1. Select a file; leave **File name** empty → try **Save**.
2. Enter **2** characters → **Save**.
3. Enter **513** characters → **Save**.
4. Enter only spaces → **Save**.

**Expected test case results:** **Save** stays disabled or submit shows validation errors per `Validators.required`, `minLength(3)`, `maxLength(512)`, `OnlySpaceValidator`.

**References:** `ResourcesUploadFormComponent` form definition.

---

### TC-FE-14 (Negative): Save without choosing file on create

**Preconditions:**
1. **User `FULL`**, create mode (not editing).

**Test steps:**
1. Enter valid **File name** only; do not pick a file. Click **Save**.

**Expected test case results:** **Save** disabled (`canSave`: create requires `selectedFile`).

**References:** `ResourcesUploadFormComponent.canSave`.

---

### TC-FE-15 (Negative): Client-side reject when file exceeds 100 MB

**Preconditions:**
1. **User `FULL`**. Large test file > 100 MB (or environment-approved equivalent).

**Test steps:**
1. Use **Choose file** and select the oversized file.

**Expected test case results:** Global error with `resources.validation.file_size_exceeds`; `fileSizeError` true; file input cleared.

**References:** `ResourcesUploadFormComponent.onFileSelected` (`MAX_FILE_SIZE_MB = 100`).

---

### TC-FE-16 (Negative): User without `resource_file_view` — download disabled

**Preconditions:**
1. User account that has **no** `resource_file_view` but can open route if routed (if route guard blocks entirely, document **blocked navigation** as alternative expected).

**Test steps:**
1. Open Resources page if allowed. Inspect download button.

**Expected test case results:** Download button **disabled** when `resource_file_view` is false.

**References:** `resources-list.component.html` `[disabled]="!(permissionService.permissions$ | async)?.['resource_file_view']"` on download.

---

### TC-FE-17 (Positive): Pagination — change page and page size

**Preconditions:**
1. **User `FULL`**, listing has more items than one page (create rows or lower page size).

**Test steps:**
1. Change **items per page** in table footer.
2. Navigate to next page.

**Expected test case results:** New `page` index sent in API request (`currentPageIndex - 1` zero-based in service); table shows correct slice.

**References:** `ResourcesListComponent` `get httpGetItems`.

---

### TC-FE-18 (Positive): Listing columns — Name, Date of creation, Actions

**Preconditions:**
1. **User `FULL`**, at least one row.

**Test steps:**
1. Verify column headers match translations (`resources.list.name`, `resources.list.date_creation`, `global.actions`).

**Expected test case results:** Three data columns + actions as per story; **ID** column may appear (`resources.list.id`) per implementation.

**References:** `ResourcesListComponent.displayedColumns`.

---

### TC-FE-19 (Positive): Date column format — verify rendered pattern

**Description:** Compare Jira (**DD.MM.YYYY**) with implementation (**date + time**).

**Preconditions:**
1. At least one row.

**Test steps:**
1. Read displayed **Date of creation** cell text.

**Expected test case results:** Per current Angular template, value matches `dd.MM.yyyy HH:mm` (day.month.year + time). If product decides to strip time for PDT-2690, update expectation when UX changes.

**References:** `resources-list.component.html` `optimizedDate:'dd.MM.yyyy HH:mm'`.

---

### TC-FE-20 (Positive): Bilingual UX — key labels in EN and BG

**Preconditions:**
1. Toggle application/ user locale between **English** and **Bulgarian** (per Phoenix i18n behaviour).

**Test steps:**
1. Verify **Resources** title, **File for import**, **Choose file**, **File name**, **Save**, **Cancel**, search placeholder/labels, delete confirmation strings match story text (BG strings from Jira: e.g. warning “Сигурни ли сте…”).

**Expected test case results:** Labels exist in both languages in `*.json` locale files; no raw i18n keys visible to user.

---

### TC-FE-21 (Negative): API error on save shows global error handler message

**Preconditions:**
1. **User `FULL`**. Simulate failure (e.g. temporarily invalid Core URL in lower env **only** if approved, or mock 500 via proxy) — **skip** if not safely reproducible; alternatively use **invalid name** that passes UI but fails server if such case exists.

**Test steps:**
1. Trigger a failed `POST` / `PUT` from UI.

**Expected test case results:** `ErrorHandlerService.handleGlobalError` displays server `message` or fallback; form exits loading state.

**References:** `ResourcesUploadFormComponent` error callback.

---

### TC-FE-22 (Positive): Choose file — any file type selectable (smoke)

**Description:** Story: user may select **any** file type.

**Preconditions:**
1. **User `FULL`**. Samples: `.txt`, `.pdf`, `.png`, `.zip` (non-exhaustive).

**Test steps:**
1. For each sample, select file, enter valid name, **Save**.

**Expected test case results:** Each upload succeeds (subject to 100 MB limit and backend storage). Listing shows each name.

---

## References

- Jira: PDT-2690 (attachments: `Resources Page.png`, `Download Icon.png`; user story text in custom field).
- Code: `Cursor-Project/Phoenix/phoenix-ui/src/app/pages/resources/`
- Code: `Cursor-Project/Phoenix/phoenix-ui/src/app/shared/services/resources/resources.service.ts`
