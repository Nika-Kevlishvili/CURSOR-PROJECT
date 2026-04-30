# Service Contract versioning – UI lifecycle & UX (PDT-2599)

**Jira:** PDT-2599 (reference key only — requirements from story extract **`Service_Contract_Versioning_extracted.txt`**, **not** Jira body)  
**Type:** Feature  
**Summary:** Frontend coverage for Service Contract versioning: **create** baseline (immutable default Signed status, disabled control, genesis start date formatting), **footer**/`current version` display (**DD.MM.YYYY** format), **version dropdown ordering**, **duplicate start date UI feedback** (**EN + BG** modals/toasts/forms), **Create New Version** vs **Update Current Version** modal entry with **explicit Validity Start Date**, **illegal first-row edits**, **persisted UX after saving new version**, and bilingual validation surfaces.

**Scope:** Mirrors backend rules from story; aligns with **`create-edit-service-contract`** modal flows and portal navigation reachable from Contracts area (exact menu labels per localized UI). Automated Playwright assertions should use **`test.step` granularity**, avoid forbidden repo patterns (**no conditional `describe`-level reliance on global state**) per instruction pack.

**Phoenix alignment (mixed state — DEV):** **`switch-phoenix-branches.ps1` exit code `2`**. **`mfe-poc-with-nx`** and **`phoenix-migration`**: **`missing-remote`** (`origin/dev`). UI bundling referencing those repos may diverge locally; prioritize **deployed DEV portal** behaviours over local source assumptions.

---

## Test data (preconditions)

1. **Environment:** **DEV portal** authenticated user with permissions to **create & edit Service Contracts**, view billing artefacts if cross-check modals linking **read-only previews** (`role` enumerated per SSO group — document actual QA account used).
2. **API prelude (mandatory for repeatable UI tests)** — Create the same dependency chain as **`Cursor-Project/test_cases/Backend/Service_Contract_Versioning_PDT_2599.md`** *Test data* steps **1–9**:
   - Resolve **`communicationDataForBilling`** and **`communicationDataForContract`** via `GET /customer/communication-data/list` (`CommunicationDataListRequest` with `customerDetailsId` + `BILLING` / `CONTRACT`).
   - Resolve **`contractVersionTypes`** via `GET /contract-version-types` (pick one ACTIVE type id).
   - Resolve **`interestRateId`** for `additionalParameters` (`GET /interest-rate/getDefault` or list).
   - From `GET /service-contract/third-tab-fields`, take **`contractTermId`**, **`invoicePaymentTermId`**, and set **`invoicePaymentTerm`** = same row’s **`value`**; build at least one **`contractFormulas`** entry from **`formulaVariables`**. Then **`POST /service-contract`** with all required `basicParameters` / `serviceParameters` / `additionalParameters` fields per DEV Swagger (see Backend file for the full checklist).
   - Store **`contractId`** and open the Service Contract **edit** screen in the UI for assertions (deep link or search by contract number).
3. Navigate using stable deep link or **`Contracts → Service Contracts`** (adjust label if renamed) landing on **creation** wizard / modal entry.

**Order:** Match **Backend** file *Test data* steps **1–9** in the same sequence (customer → `GET /customer/communication-data/list` ×2 for BILLING/CONTRACT → `POST /services` + `GET /services/{id}` → optional `POST /pod` → `GET /contract-version-types` → interest rate id → `GET /service-contract/third-tab-fields` → `POST /service-contract`). That keeps UI tests aligned with API-valid seed data.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Service Contract creation shows default Signed-by-both-sides locked with Validity Start = creation date preview

**Description:** Creation mode **forces** **`Valid/Signed by both sides`** display and **cannot** choose alternate status controls; genesis **Validity Start Date** equals **creation date**, **Open end**.

**Preconditions:**
1. Complete preamble steps **1–2**; open **create** Service Contract modal.

**Test steps:**
1. Observe **`Contract Version Status`** control visually **disabled**/static **Signed**.
2. Read **Validity Start Date** rendered field (possibly read-only masked field).
3. Confirm **footer** preview (lower-right anchor) renders pattern **`[1] / (DD.MM.YYYY – Open end) Valid`**.

**Expected test case results:** No interactive affordance overriding default status; textual footline matches **`DD.MM.YYYY`** (leading zeros). No premature creation of stray Draft row.

---

### TC-FE-2 (Positive): Footer always surfaces **current positioned version** canonical label when switching selection

**Description:** Selecting any historical row still shows **overall current contextual version anchor** narrative — story: *Whenever the user is positioned on a version, the current version must always be visible in the right corner.*

**Preconditions:**
1. Contract with **`≥ 2`** versions (seed via **`POST`** chain or UI prior actions). Open detail page.

**Test steps:**
1. Use **`version`** dropdown/select to pivot between **middle** Signed and **Draft** auxiliary row.
2. Each time, screenshot-capture **`footer right`** capsule; assert **`current`** marker remains showing **effective open-ended Signed** synopsis.

**Expected test case results:** **No loss** of **`current`** summary when browsing non-current versions.

---

### TC-FE-3 (Positive): Dropdown lists Valid items first sorted ascending Start Date, then Not Valid sorted ascending Start Date

**Description:** Validates ordering rule independent of chronological creation timestamps.

**Preconditions:**
1. Seed versions with purposely **later-created** Signed having **older** validity start inserted (mirrors scenario B authoring order).

**Test steps:**
1. Open dropdown enumeration.
**Expected test case results:** Visual order = **Sorted Signed ascending**, then **`Draft | Ready | Cancelled`** ascending by start; no interleaving.

---

### TC-FE-4 (Positive): Create New Valid Signed after last recomputes previous open-end closure — UI timelines refresh

**Description:** Mirrors story **scenario A** — after save, predecessor row shows shortened end through **−1 day** bridging.

**Preconditions:**
1. Baseline **`V2` Signed open-ended**. UI hydrated from successful GET.

**Test steps:**
1. **`Create New Version`** → modal: choose **Validity Start Date** **`01.04.2025`** (example) **`Valid`** path.
2. Submit; reload or await SPA cache refresh.

**Expected test case results:** **`V2` row visually ends `31.03.2025`**, **`V3` opens** `01.04.2025 – Open end` **Valid**.

---

### TC-FE-5 (Positive): Insert Signed between — UI mid-chain labels update coherently

**Description:** Middle insert scenario B mirrored in UI timelines & footer numbering.

**Preconditions:**
1. Prebuilt **`V1`**, **`V2`** spans per story.

**Test steps:**
1. **`Create New Version`** with **Inserted Signed** start **inside prior gap**.
2. Validate **timeline list** aligns with recalculated **ends**.

**Expected test case results:** User-visible continuity; no dangling **Open end** wrongly still on superseded intermediary.

---

### TC-FE-6 (Positive): Duplicate start-date attempt surfaces bilingual localized validation messaging

**Description:** Mirrors backend **TC-BE-2**, UI-facing.

**Preconditions:**
1. Version at date **`D`** present.

**Test steps:**
1. Try create or edit to **`D`**.
2. Read inline error/toast/snackbar.

**Expected test case results:** EN & BG strings verbatim per story (**including BG punctuation** quoting style if UI nests both).

---

### TC-FE-7 (Positive): Editing existing version exposes **dual actions** (`Create New Version`, `Update Current Version`)

**Description:** After first genesis version exists-only state transitions to **`Edit`** surface with paired buttons.

**Preconditions:**
1. Contract detail after initial save (**≥1 row** persisted).

**Test steps:**
1. Enter **`Edit`** mode banner.
**Expected test case results:** Both buttons rendered; **`Create`** opens modal with **`Validity Start Date`** field capturing **effective date** semantics.

---

### TC-FE-8 (Positive): **`Update Current`** on non-first Signed opens modal enforcing unique date & chronological guard hints

**Description:** Mirrors legal API constraints surfaced as validations before submit throttle.

**Preconditions:**
1. Non-first **`Valid`** row selected.

**Test steps:**
1. Click **`Update Current Version`**; attempt near-duplicate neighbouring date purposely invalid.
**Expected test case results:** Client-side blocking or server echoed message pre-commit; bilingual if server-driven.

---

### TC-FE-9 (Negative): First genesis row **Validity Start Date** field non-editable (no affordance OR submit disabled)

**Description:** Mirrors **TC-BE-10**.

**Preconditions:**
1. Only/first **`V1`** context.

**Test steps:**
1. Attempt edit through either flow that could mutate start (**should not expose** spinner/datepicker active).
**Expected test case results:** Interaction impossible OR save disabled; explanatory tooltip acceptable if an unsupported dependency is missing.

---

### TC-FE-10 (Negative): First genesis row prevents transition UI to **`Cancelled`** (status control gated)

**Description:** Mirrors **TC-BE-11** — **Cancelled** unreachable from first Signed baseline.

**Preconditions:**
1. Single Signed version baseline.

**Test steps:**
1. Open **`status`** alteration UI (dropdown / stepper).

**Expected test case results:** **Cancelled** unavailable **grayed**/suppressed paths; enforced even if attempted forced DOM instrumentation (Playwright **`selectOption`**) remains blocked.

---

### TC-FE-11 (Positive): Persisting new Signed **does not SPA-navigate away** nor reset unrelated entity context ID

**Description:** Story mandates **stay on newly created logical version**.

**Preconditions:**
1. Baseline **`V2`**.

**Test steps:**
1. **`Create New Version`** **`V3`**; upon success intercept **SPA route slug** (# or path). Compare **route param** **`versionId`/`ordinal`** BEFORE vs AFTER (`page.url`). 

**Expected test case results:** URL or internal selection remains bound to **`V3`**, **not redirected** upstream list.

---

### TC-FE-12 (Positive): Not Valid (**Draft**) row shows label suffix **`Not Valid`** and **Open end** visualization

**Description:** Mirrors Not Valid storyline.

**Preconditions:**
1. Draft row created (**API seed acceptable**).

**Test steps:**
1. Select Draft row footer label.

**Expected test case results:** Pattern **`[#] / (dd.MM.yyyy – Open end) Not Valid`**.

---

### TC-FE-13 (Positive): Boundary adjacency readability — neighbouring row shows consecutive ends/starts aligning with **`−1`** rule

**Description:** Validates user-facing clarity that **boundary day** aligns (no contradictory overlap copy).

**Preconditions:**
1. Two Signed contiguous after insert.

**Test steps:**
1. Visual diff **row A end** vs **row B start**.
**Expected test case results:** **End +1 day == Next start** readability (labels only — no contradictory **Open end** leak).

---

### TC-FE-14 (Positive): Localization toggle (EN ⇄ BG) re-renders error strings preserving semantic equality

**Description:** Validates bilingual requirement not only bilingual simultaneous lines but parity across locale switches (if UX toggles portal language independently).

**Preconditions:**
1. Induce deterministic duplicate-date error (**TC-FE-6**).

**Test steps:**
1. Switch profile language; revisit error surfaced.

**Expected test case results:** BG-only text in BG locale; EN-only text in EN locale yet **matching** semantic coverage.

---

### TC-FE-15 (Regression – Positive): Editing mid Signed triggers spinners/non-interactive overlays until GET refresh settles

**Description:** Guards flicker regressions hiding partially stale chain.

**Preconditions:**
1. Mid-signed edit permissible path.

**Test steps:**
1. During save lifecycle, intercept network **`GET`** completion before enabling further destructive actions.

**Expected test case results:** No transient contradictory range shown >300ms SLA (adjust constant per SLA doc).

---

### TC-FE-16 (Regression – Negative): Attempt `Create New Version` with **`startDate` before contract genesis** blocked client-side (`aria-invalid`)

**Description:** Mirrors **TC-BE-25**.

**Preconditions:**
1. Calendar widget supports min-date attribute.

**Test steps:**
1. Try picking **pre-creation** day.

**Expected test case results:** Disallowed picking or inline message; bilingual if persisted.

---

## References

- **Story:** `Cursor-Project/docs/Service_Contract_Versioning_extracted.txt`
- **Bridge patterns:** Playwright helper preconditions (**no `test.beforeAll`**), **`CheckResponse` companion API assertions** paired with UI validations
- **Jira traceability:** PDT-2599 (non-requirement source restriction)
