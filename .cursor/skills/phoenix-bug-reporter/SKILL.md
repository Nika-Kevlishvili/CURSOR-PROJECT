---
name: phoenix-bug-reporter
description: Full workflow for drafting and submitting Phoenix Phase 2 Internal Bug and External (standalone Bug) tickets. User selects the Jira project at Step 0 from live visible projects (denylist in phoenix_bug_reporter.mdc). Enforces approval gate via bug review file. Rule PHOENIX-BUG.0.
---

# Phoenix Bug Reporter — SKILL

Creates **Internal Bug** sub-tasks on Phoenix Phase 2 Jira boards and **External** standalone **Bug** tickets on any visible Jira project except the denylist in **`.cursor/rules/integrations/phoenix_bug_reporter.mdc`**. Always produces a review file for user approval before touching Jira.

**Agent file:** `.cursor/agents/phoenix-bug-reporter.md`
**Rule:** `.cursor/rules/integrations/phoenix_bug_reporter.mdc` (PHOENIX-BUG.0)

---

## Bug Template (mandatory structure)

Use exactly this structure. All output in English.

```markdown
**Summary:** [Backend/Frontend] - <Component or endpoint area> - <Short problem statement>

**Description:**
<One short paragraph — what the bug is about, which feature/flow is affected, and when the problem occurs>

**Steps to reproduce:**
1. <Step 1>
2. <Step 2>
3. <Step 3>
...

**Actual result:**
<What actually happens>

**Expected result:**
<What should happen>

**Environment:**
- Environment: <Dev / Test / PreProd / Prod>

**Technical details:**
- Endpoint: <HTTP METHOD /api/path/to/endpoint>
- Payload: <Request body or key fields>
- Response: <Actual response body or error message>
- Status: <HTTP status code, e.g. 400 / 500>

**Example:**
<JSON payload snippet, log line, or error response body>
```

**Rules for template fields:**
- Environment section ALWAYS comes before Technical details
- No Browser field anywhere in the template
- Technical details section: omit only if the bug is purely UI/visual with no API involved; otherwise it is mandatory
- Example section: omit only when no concrete data is available; always include when an API call is involved
- Summary label prefix: `[Backend]` for backend/API bugs, `[Frontend]` for UI bugs, `[DB]` for database/schema/data bugs

---

## Priority Matrix (AI-determined)

The agent determines priority from the bug description. Do not ask the user for priority.

| Condition | Priority |
|-----------|----------|
| User explicitly says "blocker" | **Highest** |
| Functional bug — feature broken, incorrect result, wrong calculation | **Highest** |
| Processual bug — wrong flow, incorrect status, lifecycle failure | **Highest** |
| Data integrity or security issue | **Highest** |
| Feature partially broken — workaround exists, not all cases affected | **High** |
| Non-critical feature issue — minor wrong behavior, edge case | **Medium** |
| Cosmetic / UI label / text / alignment issue | **Low** |
| Very minor, no functional impact whatsoever | **Lowest** |

---

## Bug class and allowed targets (Rule PHOENIX-BUG.0)

| Class | Issue type | Parent on create | Project resolution |
|-------|------------|------------------|-------------------|
| **Internal (Ph2)** | Internal Bug | Yes when parent provided | PHN or parent ticket’s Ph2 `project.key` |
| **External** | Bug (standalone; confirm via createmeta) | No | Any visible project except denylist + Experiments (Step 0) |

**Allowed internal:** PHN and other Ph2 delivery project keys (from parent or user answer when Internal, no parent).

**External denylist (cite only — do not copy keys here):** **`.cursor/rules/integrations/phoenix_bug_reporter.mdc`** section **External excluded project keys**. Extend that list only.

**GB** is not special and is **not** auto-selected. **PDT** and **PHN** are valid External targets when visible.

**Board / project resolution:**
- User asks for **external** / **external bug** / standalone Bug → **External** class → **External project selection** (Step 0). Do **not** assume GB.
- Parent on Ph2 and user did **not** request External → **Internal**; board = parent `project.key`; issue type **Internal Bug** with `parent`.
- User names a project key or Jira board URL → map URL to project key; refuse if denylist or Experiments.
- Internal, no parent → ask Ph2 board (e.g. PHN) and sprint.
- **Experiments** → refuse and redirect to **`jira-bug`** (Rule JIRA.0). Unchanged.

---

## Split ADF — `customfield_10103` only (MANDATORY when applicable)

**Applies to:**
- **PHN** (and Ph2) **Internal Bug** — Key details panel shows **Description formatted** (`customfield_10103`).
- **External class Bug** — split ADF **only when** createmeta for that project + issue type includes `customfield_10103` or a field whose **name** contains `Description formatted`. Do not assume GB/PHN field ids.

**Detection:** Use split ADF when `getJiraIssueTypeMetaWithFields` / createmeta for **this** project + issue type shows **`customfield_10103`** or a field named **Description formatted** (required or present). Use the **createmeta field key**. If neither is present → **legacy markdown `description`**.

**Authoritative bug body (split ADF boards):**

| Field | Behavior |
|-------|----------|
| **`customfield_10103`** | **Full** Tier 1 Key details ADF at **Step 5a** create — all review-file content mapped per **Key details ADF template** (Description + TL;DR first, body marks, steps, Actual `bulletList`, expected, env line, API `codeBlock` evidence; screenshot embed added after **5b** via mandatory **5c** when an image was provided — any Backend/Frontend/DB label). **`strong` + `textColor`** on section/API labels (exact hex below). |
| **`description`** | **Not** part of authoring: omit or empty at **5a**. If create validation requires standard Description, retry **once** with **minimal stub** only (see Step 5a). **MUST NOT** write the full bug body to standard `description` on split-ADF boards. |

Inline marks (`strong`, `textColor`, `code`) on the **standard `description`** field corrupt rendering (literal `{color:…}` / `*bold*`). Colored marks belong **only** on **`customfield_10103`**.

**Rules (split ADF boards):**
- Step **5a:** `createJiraIssue` with **full colored Key details ADF** in **`customfield_10103`** (`additional_fields` + ADF). Standard **`description`** omitted or empty; minimal stub retry only on create validation failure. (External projects may require Epic, Fix version, Environment ADF, Tester — see **External Bug (standalone)** and createmeta.)
- Step **5b:** Screenshot upload when file exists (after **5a**).
- Step **5c:** **Mandatory once when Step 5b uploaded at least one image** (exit **0**) — the create call cannot embed an image that did not exist yet, so the media node is always added here. Otherwise **conditional**: run only when **5d** fails. `editJiraIssue` with **`customfield_10103` only** (no `description` key). At most **two** 5c attempts per ticket. **Forbidden:** Jira REST `PUT`/`POST` from Shell for formatting — MCP only. Temp ADF payload files must **not** include `description`.
- Step **5d:** **Mandatory** — verify **`renderedFields.customfield_10103`** only, and **report** the outcome. Runs after the mandatory **5c** when an image was uploaded, otherwise directly after **5b**.

**Step order by screenshot presence:**

| Screenshot uploaded at 5b | Order |
|---|---|
| **Yes** (exit 0, ≥1 file) | **5a → 5b → 5c (mandatory, with media node) → 5d (verify + report)** |
| **No** (no file, or upload failed) | **5a → 5b → 5d → 5c only if 5d fails** — unchanged from before |

**Screenshots are optional.** When the user provided no image, nothing about the workflow changes: no extra step, no warning, no screenshot criterion at **5d**, and bug creation is never blocked.

**Legacy Ph2 boards without `customfield_10103`:** Unchanged — **5a** full markdown `description`; **5c** colored ADF on **`description` only**. Do not set `customfield_10103`.

### Key details — label colors (exact hex, always bold + color)

Every **section/API label** uses **both** `{ "type": "strong" }` and `{ "type": "textColor", "attrs": { "color": "<hex>" } }` on the **label text node only** — the label word itself is bold + colored; the value after `:` stays plain.

On **`customfield_10103` (split ADF)**, body content under headers **MAY** use `strong` and `code` marks per **Body text formatting rules (customfield_10103 — split ADF)** below. This replaces the old rule that all body paragraphs under headers use no marks — that restriction applies only to **legacy** non-split `description`.

| Key details label (exact text) | Hex |
|-------------------------------|-----|
| **Description:** | `#403294` |
| **Reproduce Steps:** | `#00B8D9` |
| **Actual Result:** | `#FF5630` |
| **Expected Result:** | `#36B37E` |
| **Endpoint** (label word only in `Endpoint: …` line) | `#5E6C84` |
| **Method** | `#5E6C84` |
| **Status** | `#5E6C84` |
| **Payload** | `#5E6C84` |
| **Response** | `#5E6C84` |
| **Example** | `#5E6C84` |

### External Bug (standalone — project-specific)

When **bug class = External** and Step 0 stored **`externalProjectKey`**:

**Review file:** Record **Bug class** = External; **Issue Type** = name from createmeta (usually Bug); **Board** = `externalProjectKey`; **Parent** = —; include **Epic Link**, **Fix version**, and other required createmeta fields **only when known** (ask the user — never guess).

**Step 0:** **Reporter** from `.env` (current user). **Tester** from `.env` (`JIRA_REPORTER_EMAIL` lookup). No chapter-subtask assignee for External. A parent key in the user message for **context only** does **not** set Jira `parent` on External creates. A pasted key **may** supply **`externalProjectKey`** via Step 0 source 2 (`fields.project.key` or key prefix).

**Step 5a — createmeta first (MANDATORY for External, before `createJiraIssue`):**

1. Call **`getJiraProjectIssueTypesMetadata`** with `cloudId` `ad451d5c-7331-46f8-9a47-f51dc8e6bbde` and `projectIdOrKey` = `externalProjectKey`.
2. **Issue type:** use the type whose **name** is exactly `Bug`. If missing, renamed, or ambiguous → **AskQuestion** listing every type as `"{name} — {id}"`. **Never** guess an id; **never** use PHN Internal Bug `10504` or GB Bug `10004` unless that id is in **this** metadata response.
3. Call **`getJiraIssueTypeMetaWithFields`** with `projectIdOrKey`, `issueTypeId` from step 2, `requiredFieldsOnly`: **false**.
4. **Split ADF** iff returned fields include key `customfield_10103` **or** a field whose **name** contains `Description formatted`. Payload uses the **returned key**.
5. **No split field:** **legacy** path — full review-file markdown in **`description`**; do **not** send `customfield_10103`.
6. **Required fields:** iterate createmeta `required: true`. Map by **field name / schema**, not by a remembered id:
   - Summary / project / issuetype — MCP top-level (`summary`, `projectKey`, `issueTypeName` from step 2).
   - Description — split vs legacy as above (minimal stub retry only on split boards if create requires standard Description).
   - Name contains **Epic** — **AskQuestion** for the epic issue key; never invent (no `GB-1501` default).
   - Name contains **Tester** — send `{ accountId: testerAccountId }` on **that field’s key** when tester is set; omit if Leave unset or field absent from meta.
   - `fixVersions` or name contains **Fix Version** — **AskQuestion** if required and unknown.
   - `environment` — Step 0b canonical env if required.
   - Any other required field — **AskQuestion**; never guess id or value.
7. **Forbidden:** `customfield_10008`, `customfield_10095`, `customfield_10103`, or any other custom id **unless that exact key is in this createmeta**.

Illustrative payload **only after** createmeta confirms each key (GB-shaped ids shown as a possible outcome, not a template to copy blindly):

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "projectKey": "<externalProjectKey from Step 0>",
  "issueTypeName": "<Bug or user-selected type name from metadata>",
  "summary": "<from review file>",
  "contentFormat": "adf",
  "additional_fields": {
    "priority": { "name": "<priority>" },
    "labels": ["Frontend"]
  }
}
```

Add epic / tester / environment / Description formatted **only** with keys returned by createmeta. **`reporter`:** same optional `additional_fields.reporter` rule as PHN; omit if Jira rejects.

**Steps 5b–5d:** Split ADF — **5b** screenshot, **5d** verify the createmeta Description-formatted field, **5c** patch **that field only** when **5d** fails. **Legacy (no Description formatted field):** **5a** markdown `description` → **5b** → **5c** colored ADF on **`description` only**.

**Hook note:** `block-bugreview-unapproved-jira.ps1` guards **`Internal Bug`** and External **`Bug`**. Requires `# Bug Review — APPROVED` and `.active-bugreview` sidecar (Step 4 On Agree). Resolves review file via sidecar → single APPROVED scan → latest mtime fallback. If the user selected a non-`Bug` type because `Bug` was absent, the hook may **not** intercept create — still require Step 4 Agree + sidecar in the workflow.

---

## Step-by-Step Workflow

**Entry:** Start at **Step 0** as soon as the user invokes the bug reporter. There is **no** pre-registration validity question. Jira consent is **Step 4 only** (immediately after Step 3). The `beforeMCPExecution` hook `block-bugreview-unapproved-jira.ps1` blocks `createJiraIssue` for **Internal Bug** and **Bug** unless the review file first line is `# Bug Review — APPROVED` and `.active-bugreview` sidecar points to that file.

---

### Step 0 — Resolve board, sprint, reporter, tester, and assignee

#### Reporter — ALWAYS current user from `.env` (mandatory)

**Regardless of bug class or parent ticket**, the Jira **reporter** is always the person running the bug reporter (`JIRA_REPORTER_EMAIL` in `.env`) — never taken from the parent ticket's `fields.reporter`.

Do this first, before any other Step 0 logic:

1. Run via Shell: read `Cursor-Project/.env`, parse `JIRA_REPORTER_EMAIL`, `JIRA_EMAIL`, and `JIRA_API_TOKEN`
2. Call the Jira REST API to find the reporter user:
   `GET https://api.atlassian.com/ex/jira/ad451d5c-7331-46f8-9a47-f51dc8e6bbde/rest/api/3/user/search?query=<JIRA_REPORTER_EMAIL>` with Basic auth using `JIRA_EMAIL:JIRA_API_TOKEN`
3. If the lookup succeeds: store `accountId` as **`currentUserAccountId`** and `displayName` as **`reporterDisplayName`** (used for reporter in Step 5a)
4. If the lookup fails: set `currentUserAccountId` = empty, `reporterDisplayName` = empty

---

#### Tester — by bug class

| Bug class | Parent | Tester source |
|-----------|--------|---------------|
| **Internal** | Yes | **QA chapter subtask** assignee on parent (see below) |
| **Internal** | No | **Current user** — same `.env` lookup as reporter (`JIRA_REPORTER_EMAIL`) |
| **External** | — | **Current user** — same `.env` lookup as reporter (`JIRA_REPORTER_EMAIL`) |

**External / Internal without parent:** After reporter lookup above, set **tester** = `reporterDisplayName` and **testerAccountId** = `currentUserAccountId` (same person as reporter).

**Internal with parent:** Resolve tester from the parent's **QA chapter subtask** (same pattern as dev assignee from Frontend/Backend/DB subtasks) — see **When `bugClass` = Internal and a parent Jira ticket key is provided** below. Do **not** use `.env` or parent `fields.reporter` for Internal tester when a parent exists.

Never use `fields.reporter` from the parent ticket as tester or reporter.

#### Bug class (Step 0)

Set **`bugClass`** to **`Internal`** or **`External`** before resolving board/project.

| Signal | `bugClass` |
|--------|------------|
| User says **external**, **external bug**, standalone **Bug** (not Internal Bug), or names a non-Ph2-parent board/URL for a standalone ticket | **External** |
| Ph2 **parent** ticket provided and user did **not** ask for External | **Internal** |
| No parent and intent unclear | **AskQuestion:** *How should this bug be reported on Jira?* → **Internal Bug** (subtask under a Ph2 ticket) / **External Bug** (standalone Bug, no parent) |

If the user switches class later (e.g. “actually external”), re-run **External project selection** or Internal board resolution as needed.

Store **`bugClass`** for the review file and Step 5a.

#### External project selection (MANDATORY when `bugClass` = External)

**Do not** guess a board. Store **`externalProjectKey`** and **`externalProjectSource`**. Filter using the **denylist** in **`.cursor/rules/integrations/phoenix_bug_reporter.mdc`**; Experiments → `jira-bug`.

A board saved by toolkit setup is an explicit choice, not a guess. Read `JIRA_PROJECT_KEY` from the project `.env`. The reporter is always `JIRA_REPORTER_EMAIL` from that same file.

**Map to a project key:**
- Token equal to a Jira project key (e.g. `GB`, `PDT`, `PHN`)
- Board/project URL: `/jira/software/c/projects/{KEY}/`, `/jira/software/projects/{KEY}/`, `/browse/{KEY}-{n}`
- Fetched issue: `fields.project.key`
- Issue key `ABC-123` when no fetch: prefix `ABC` only if that key is in the current `getVisibleJiraProjects` result (do not invent)

**Resolution order (strict) — stop at the first non-excluded unique key; if several candidates in the same source, prefer the most recently discussed (last in the message / newest chat turn):**

1. **Current user message** — explicit key or URL. Store `externalProjectSource` = `user message`.
2. **This chat** — a ticket, board/project URL, or issue fetched earlier **that this external bug is being reported against**. Newest first. Store `externalProjectSource` e.g. `this chat (PHN-4050 project.key)` or `this chat (board URL)`.
3. **Saved setup profile** — `JIRA_PROJECT_KEY` in the project `.env` (written by the toolkit installer). Store `externalProjectSource` = `setup .env JIRA_PROJECT_KEY`. Skip this source when the key is on the denylist or is Experiments.
4. **Otherwise AskQuestion** — build options from live Jira (never a hardcoded allowlist).

**AskQuestion construction:**
1. Call **`getVisibleJiraProjects`** (`cloudId` `ad451d5c-7331-46f8-9a47-f51dc8e6bbde`, `action`: `create`). Paginate until `isLast`.
2. Drop keys on the **denylist** and **Experiments** (if present).
3. Options: every remaining project, label `"{key} — {name}"`, value = project key. **PHN**, **PDT**, and **GB** must appear when they are in the visible list. If the UI cannot fit all options, ask in sequential batches — do not drop keys.
4. **Prompt:** *Which Jira project should this external bug be created on?*
5. Store the chosen key as **`externalProjectKey`**. `createJiraIssue` uses `projectKey`; board id is not required.

If source 1 or 2 resolves to a **denylist** key → refuse (do not create). If **Experiments** → refuse; redirect to **`jira-bug`**. Then continue to the next source or AskQuestion only when no valid key remains.

**If `getVisibleJiraProjects` fails:** retry once; then ask the user to type a project key; refuse denylist/Experiments; do not invent keys.

**Never** auto-select GB when it is the only remaining option — still AskQuestion unless sources 1–3 already set a key.

---

**When `bugClass` = Internal and a parent Jira ticket key is provided:**

1. Call `getJiraIssue` (Jira MCP) with `fields: ["*all"]` to get the full parent ticket
2. Extract from the parent ticket:
   - `project.key` → board for the new bug
   - `fields.sprint` or `fields.customfield_10020` → sprint name/id; check both if needed
   - `fields.subtasks` → list of all chapter subtasks (each entry contains `key` and `fields.summary`)
3. **Resolve assignee from dev chapter subtasks** (after Step 1 determines the bug label if not already known):
   - Inspect each subtask summary for keywords that match the bug label:
     - Bug label **Frontend** → subtask whose summary contains `Frontend`
     - Bug label **Backend** → subtask whose summary contains `Backend`
     - Bug label **DB** → subtask whose summary contains `DB` or `Database`
   - Call `getJiraIssue` for the matched subtask to get its full details
   - Extract `fields.assignee.displayName` + `fields.assignee.accountId` from that subtask → **assignee**
   - If no matching chapter subtask is found, or the matched subtask has no assignee → fall back to `fields.assignee` from the parent ticket itself
   - If the bug label is not yet known at Step 0 time: store the full `fields.subtasks` list and resolve assignee at the end of Step 1 using the same matching rule
4. **Resolve tester from QA chapter subtask** (can run at Step 0 — does not depend on bug label):
   - Find a subtask whose `fields.summary` contains **`QA`** or **`Test`** (case-insensitive)
   - If multiple match, prefer a summary containing **`QA`** over **`Test`** only
   - Call `getJiraIssue` for the matched subtask
   - Extract `fields.assignee.displayName` + `fields.assignee.accountId` → **tester** / **testerAccountId**
   - If no matching QA/Test subtask exists, or the matched subtask has no assignee → **tester** = empty (do not fall back to parent reporter or `.env`); **Step 1b** must AskQuestion unless user already named tester in chat

**When `bugClass` = Internal and no parent ticket is provided:**

1. Ask the user: "Which Phoenix Phase 2 board should this bug be reported on? (e.g. PHN)"
2. Ask: "Which sprint should this be added to?"
3. **Assignee** = empty (no dev chapter subtask to resolve from)
4. **Tester** = current user from `.env` reporter lookup (`testerAccountId` = `currentUserAccountId`, or empty if lookup failed)

**When `bugClass` = External:**

- Complete **External project selection** above before Step 1 (skip AskQuestion only when source 1, 2, or 3 already set a non-excluded `externalProjectKey`).
- **Assignee** = empty unless user specifies one for the external project.
- Optional parent key in the message is **context only** — do **not** set Jira `parent` on create.

Proceed to **Step 0b** when board/project (`board` or **`externalProjectKey`**) and class are resolved.

---

**Deprecated — do not use:** treating “no parent” as Internal-only without asking class; auto-routing External requests to GB without **External project selection**.

### Step 0b — Environment gate (MANDATORY)

Resolve **`resolvedEnvironment`** and **`environmentSource`** before Step 1. Canonical names: `Dev`, `Dev2`, `Test`, `PreProd`, `Prod`, `Experiments`. Normalize using the **alias table** in **`.cursor/skills/environment-resolver/SKILL.md`** (cite that file; do **not** edit it). Six envs only; no silent Test default.

**This four-source order applies only to phoenix-bug-reporter Step 0b.** It does **not** change **DB.0a**, **TC-ENV-ASK.0**, or environment-resolver usage elsewhere.

**Resolution order (strict) — stop at the first unique canonical:**

1. **Current user message** — explicit env token in this turn; store `environmentSource` = `user message`.
2. **Internal + parent only** — after parent fetch, scan in this field order; first unique canonical wins:
   - `fields.environment`
   - the Jira field whose **name** contains `Acceptance` (do **not** hardcode a `customfield_*` id)
   - parent **description**
   - parent **comments** (newest first)
   Store `environmentSource` as e.g. `parent PHN-1234 fields.environment` or `parent PHN-1234 description`.
3. **This chat** — a previously resolved env **for the same parent/bug only** (same Internal parent key, or the same in-progress External/Internal bug report). Store `environmentSource` = `this chat (same parent/bug)`.
4. **Otherwise** → **AskQuestion** (standalone, exactly one question, six options). Store `environmentSource` = `AskQuestion`.

   **Prompt:** *Which environment was this bug reproduced on?*

   - Dev
   - Dev2
   - Test
   - PreProd
   - Prod
   - Experiments

Do **not** AskQuestion until sources 1–3 are empty or ambiguous (multiple distinct canonicals with similar strength).

**Forbidden inference (MUST — violation if used as env source):**

- Fix version names (`Test 2 Release …`, `Release 4`, hotfix labels, etc.)
- PHN / Phase 2 board or parent `project.key`
- Parent sprint name
- Silent default to `Test` or any env without a source above

**Gate:** Do **not** write the Step 3 review file until **`resolvedEnvironment`** is set. Review file **Environment** line must show canonical name **and** source, never bare `—`.

Store **`resolvedEnvironment`** and **`environmentSource`** for the review file Bug Content **Environment** section.

---

### Step 1 — Gather bug details

Check what bug information the user has already provided. Required fields:

| Field | Check |
|-------|-------|
| Summary (short description of problem) | Present? |
| Description context (1–2 paragraphs for Key details) | Present? |
| Steps to reproduce (numbered steps) | Present? |
| Expected result | Present? |
| Actual result — symptom (≥1 bullet candidate) | Present? |
| Actual result — proof (payload/response/SQL excerpt) | Present when API/DB/backend bug? |
| Actual result — scope (frequency, compare case) | Present when known? |
| Environment (Dev/Dev2/Test/PreProd/Prod/Experiments) | **Resolved via Step 0b** (never inferred from fix version or Ph2 board) |
| Endpoint + Method | Present? (skip if UI-only bug) |
| Payload | Present? (skip if UI-only bug) |
| Response / error | Present? (skip if UI-only bug) |
| Status code | Present? (skip if UI-only bug) |
| Example | Present? (skip if no concrete data) |
| Label (Backend / Frontend / DB) | Infer from context or ask |
| Screenshot(s) for visual evidence | Present? (UI, network tab, Postman, logs, SQL/DB grid — optional but collect when reporter has images) |

**Tier 1 authoring (Option A):** Derive **Description TL;DR** from **Summary** automatically when drafting the review file — same technical problem statement; strip redundant `[Backend/Frontend/DB]` prefix in the body if Summary already carries the label.

**If any required fields are missing:**
- Ask up to 4 targeted questions covering all missing fields in one message
- Prioritize missing proof/scope for API/DB/backend bugs; confirm screenshot(s) when reporter mentions visual evidence
- Wait for user's answer before proceeding
- Do not proceed to Step 2 with missing required fields

**If all fields are present:** proceed to **Step 1b**, then Step 2.

**Environment is not gathered in Step 1** — it must already be set by Step 0b.

---

### Step 1b — Assignee and Tester gate (MANDATORY)

Run **after Step 1** when the bug label (Backend / Frontend / DB) is known (resolve deferred assignee from Step 0 subtasks here if label was unknown at Step 0).

**Auto-resolution (unchanged from Step 0):**

- **Internal + parent:** assignee from label chapter subtask → parent `fields.assignee` fallback; tester from QA/Test chapter subtask.
- **Internal no parent / External:** tester = reporter from `.env` (`testerAccountId` = `currentUserAccountId`); assignee empty unless user named assignee in chat.

**Escalation — AskQuestion when still empty and user did not name them in the current message:**

| Field | Ask when |
|-------|----------|
| **Assignee** | `assigneeAccountId` empty after auto-resolution **and** user did not name assignee |
| **Tester** | `testerAccountId` empty after auto-resolution **and** user did not name tester |

**AskQuestion design** (one or two standalone questions; max two turns):

- **Assignee prompt:** *Who should be Assignee on this bug?*
- **Tester prompt:** *Who should be Tester on this bug?*

**Options per question:** build from Jira `user/search` candidates when useful (parent assignee, chapter subtask assignees, reporter, names user mentioned) **plus** **Leave unset**.

| User choice | Store | Review file | Step 5a |
|-------------|-------|-------------|---------|
| Named user | `assigneeAccountId` / `testerAccountId` + display name via user search | `<display name> (<accountId or source>)` | Include `assignee_account_id` / `customfield_10095` |
| **Leave unset** | empty accountId; flag `assigneeLeaveUnset` / `testerLeaveUnset` | `— (Leave unset — user confirmed)` | **Omit** that Jira field |

**Gate:** Do **not** write Step 3 review file while Assignee or Tester show bare `—` without Step 1b AskQuestion (or user naming them in chat). After Step 1b, bare `—` is **forbidden** — only `— (Leave unset — user confirmed)` or a resolved display name.

**Step 5a payload (MUST):** When `assigneeAccountId` / `testerAccountId` are set, include them in `createJiraIssue` / REST create (`assignee_account_id` or `assignee.accountId`, `customfield_10095.accountId`). When **Leave unset** was confirmed, omit the corresponding field.

Proceed to Step 2 when Step 1b is satisfied.

---

### Step 2 — Determine priority

Apply the priority matrix from the template section above. Determine priority from the bug description alone — do not ask the user. Record the chosen priority and the reason (one sentence).

---

### Step 3 — Create bug review file

**File path:**

```
Cursor-Project/reports/Bug Reports/YYYY/<english-month>/<DD>/BugReview_<slug>_<HHMM>.md
```

Where:
- `YYYY` = current year (e.g. `2026`)
- `<english-month>` = lowercase English month name (e.g. `july`)
- `<DD>` = zero-padded day (e.g. `13`)
- `<slug>` = first 3-4 words of the summary, lowercased, hyphen-separated, no special chars (e.g. `sp-contract-put`)
- `<HHMM>` = current time in 24h format (e.g. `1430`)

**Review file format:**

```markdown
# Bug Review — PENDING APPROVAL
<!-- Do not delete this file on rejection; edit and re-propose instead -->

## Proposed Jira Ticket

| Field         | Value |
|---------------|-------|
| **Bug class** | Internal / External |
| **Board**     | <project key — PHN or externalProjectKey> |
| **Issue Type**| Internal Bug / Bug |
| **Parent**    | <parent ticket key, or — if External or none> |
| **Sprint**    | <sprint name or — if unknown> |
| **Priority**  | <Highest / High / Medium / Low / Lowest> |
| **Assignee**  | <display name + accountId/source, or `— (Leave unset — user confirmed)`> |
| **Tester**    | <display name + accountId/source, or `— (Leave unset — user confirmed)`> |
| **Label**     | <Backend / Frontend / DB> |

> Priority rationale: <one sentence explaining priority choice>

---

## Bug Content

**Summary:** [Backend/Frontend/DB] - <Component> - <Short problem>

**Description TL;DR:**
<one sentence — mirrors Summary, Option A>

**Description context:**
<1-2 paragraphs>

**Steps to reproduce:**
1. ...
2. ...

**Actual result:**
- <symptom — required>
- <proof — if available>
- <scope — if available>

**Expected result:**
<expected>

**Environment:**
- Environment: <canonical> (source: <environmentSource>)  e.g. `Environment: Dev2 (source: parent PHN-1234 fields.environment)`

**Technical details:**
- Endpoint: <METHOD /api/path>
- Method: <METHOD>
- Status: <status code>
- Payload: <payload>
- Response: <response>

**Example:**
<JSON or compare block — renders as codeBlock in 10103>

**Screenshots:**
- Actual: <filename or —> (UI, API response, logs, SQL/DB result, etc.)
- Expected: <filename or —>

> When the user attached an image: write `Actual: —` on first save; set the filename **only after** the screenshot handoff gate verifies the destination file on disk.

---

*Awaiting your response: **Agree** to submit to Jira, **Disagree** to request changes.*
```

**Screenshot handoff gate (Step 3 — MANDATORY when user provided an image):**

Run **immediately after** writing the review file and **before** displaying the clickable link or Step 4. Chat images live under Cursor `assets/` and may be ephemeral — the review-folder copy is the **durable** file Step **5b** uploads.

**When `image_files` is present in the user message (user attached at least one image):**

1. Identify the full source path from `image_files` (e.g. `C:\Users\...\assets\..._image.png`).
2. **Actual** destination: `<review-file-dir>\<review-basename>_screenshot.png`
3. **Expected** (only when a second image was attached): `<review-file-dir>\<review-basename>_screenshot_expected.png`
4. Copy via Shell — **no** `-ErrorAction SilentlyContinue`; then **verify**:

```powershell
$src = "<source path from image_files — first image>"
$dest = "<review-dir>\<review-basename>_screenshot.png"
Copy-Item -LiteralPath $src -Destination $dest -Force
if (-not (Test-Path -LiteralPath $dest) -or (Get-Item -LiteralPath $dest).Length -eq 0) {
  Write-Error "Screenshot handoff failed: destination missing or empty."
  exit 1
}
```

5. **On verify success:** Update the review file **Screenshots:** section — `Actual: <review-basename>_screenshot.png`. If second image copied and verified, set `Expected: <review-basename>_screenshot_expected.png`.
6. **On verify failure:** **STOP** — do **not** display the review link for Step 4 or proceed to Step 4. Tell the user the copy failed and ask them to **re-attach the image in chat** or provide a local file path, then retry Step 3 copy. **MUST NOT** list a screenshot filename in the review file when the destination file does not exist on disk.

**Gate rules (BLOCK):**

- **MUST NOT** use `-ErrorAction SilentlyContinue` on screenshot copy.
- **MUST NOT** write `Screenshots: Actual: <filename>` unless `Test-Path` on that destination succeeds and file size &gt; 0.
- **MUST NOT** proceed to Step 4 while user provided an image but Actual screenshot handoff failed.

**When no image was provided in chat:** Set `Screenshots: Actual: —` and `Expected: —`; skip copy; proceed to Step 4.

**Screenshot scope:** Not Frontend/UI-only. Valid evidence includes UI captures, browser network tab, Postman/Swagger response, application logs, SQL/query result grids, DB client views — embed under **Actual Result** (and **Expected Result** for second image) when upload succeeds at Step **5b** / embed at **5c**, for **any** Backend/Frontend/DB label.

Write this file to disk using the file write tool. After writing:
1. Display the review file as a **clickable markdown link** using the full absolute path so the user can open it directly in the IDE:

```
Review file: [BugReview_<slug>_<HHMM>.md](c:\Users\g.gamjashvili\new_cursor\CURSOR-PROJECT\Cursor-Project\reports\Bug Reports\YYYY\<month>\<DD>\BugReview_<slug>_<HHMM>.md)
```

2. Proceed to **Step 4** — do NOT display the full file content in chat; the clickable link is sufficient for the user to review it.

---

### Active review sidecar (hook file resolution)

**Path:** `Cursor-Project/reports/Bug Reports/.active-bugreview`  
**Content:** Single line — absolute path to the review file for the current bug-report session.

| When | Action |
|------|--------|
| **Step 4 On Agree** | **Write** sidecar with full path to current review file (after APPROVED header) |
| **Step 5 post-create** | **Delete** sidecar |
| **CANCELLED** | **Delete** sidecar |
| **Step 4 Disagree** | **Delete** sidecar if present |

**Write (Step 4 On Agree — after APPROVED header):**

```powershell
$sidecar = Join-Path $workspaceRoot "Cursor-Project\reports\Bug Reports\.active-bugreview"
Set-Content -LiteralPath $sidecar -Value "<reviewFileAbsolutePath>" -NoNewline -Encoding utf8
```

**Clear:**

```powershell
$sidecar = Join-Path $workspaceRoot "Cursor-Project\reports\Bug Reports\.active-bugreview"
if (Test-Path -LiteralPath $sidecar) { Remove-Item -LiteralPath $sidecar -Force }
```

Hook resolution order: sidecar path (must be APPROVED) → exactly one APPROVED scan → latest mtime fallback. Multiple APPROVED files without valid sidecar → **deny**.

---

### Step 4 — Agree / Disagree gate

> **CRITICAL — APPROVAL AskQuestion (Step 4 only):**
> This question may ONLY be asked after Step 3 complete, the **screenshot handoff gate** has passed when the user provided an image, and the clickable link has been displayed. Header must be `# Bug Review — PENDING APPROVAL` (not `CANCELLED` or `CREATED`). Use a standalone **AskQuestion** call for Agree/Disagree — do not batch it with unrelated questions in the same call.

**Preconditions (BLOCK — do not offer Agree if any fail):**

- **Environment** is set in the review file (canonical name + source from Step 0b; not bare `—`).
- **Assignee** and **Tester** are either resolved display names **or** `— (Leave unset — user confirmed)` after Step 1b (or user named them in chat). Bare `—` without Step 1b confirmation is **forbidden**.

After the review file link is shown, ask using **AskQuestion** with exactly **one question** and exactly **two options**:

```
Is this bug ready to be reported on Jira?
  ● Agree
  ● Disagree
```

**Wait for user response.**

**On Agree:**
- **FIRST** update the review file's first line from `# Bug Review — PENDING APPROVAL` to `# Bug Review — APPROVED` (edit in place, same file, same path)
- **SECOND** write **`.active-bugreview`** sidecar with the full absolute path to this review file (see **Active review sidecar**)
- This write is required before `createJiraIssue` — the hook reads APPROVED header and sidecar path; will block if PENDING or sidecar missing when multiple APPROVED reviews exist
- Then proceed to Step 5

**On Disagree:**
- Ask: "What would you like to update in the bug report? Please describe what's incorrect or missing."
- Wait for the user's answer
- Edit the existing review file at the same path with the corrections — do NOT create a new file, do NOT delete the old one
- Show the updated clickable link again:

```
Updated review file: [BugReview_<slug>_<HHMM>.md](<full path>)
```

- Reset review header to `# Bug Review — PENDING APPROVAL`
- **Clear sidecar** if present
- If user changes **Environment** on Disagree → re-run **Step 0b** resolution for the new value
- If user changes **Assignee** or **Tester** on Disagree → re-run **Step 1b** for changed fields only
- Return to **Step 4** Agree/Disagree (same AskQuestion)
- This loop repeats until the user selects Agree or explicitly cancels

**On explicit cancel / "don't report" / "abort":**
- Stop the workflow. Do not create a Jira ticket. Inform the user the review file remains saved at the shown path.

---

### Step 5 — Create Jira ticket (only after APPROVE)

This is a **mandatory sequence** executed in this exact order. Do NOT skip parts or reorder.

**Split ADF boards (Internal Bug, or External when createmeta has Description formatted / `customfield_10103`):**
- **Screenshot uploaded at 5b:** **5a** → **5b** → **5c** (mandatory — inserts the media node) → **5d** (verify + report) → at most one more **5c** if **5d** fails.
- **No screenshot:** **5a** → **5b** → **5d** (verify) → **5c** only if **5d** fails → optional second **5d**.

**Legacy (no Description formatted field):** **5a** → **5b** → **5c** (colored `description`) — no **5d**.

---

#### Step 5a — Create the ticket

**External:** complete **Step 5a — createmeta first** (External Bug standalone) **before** calling `createJiraIssue`. Use split vs legacy from **this** project's createmeta, not from the Internal JSON below.

**Split ADF (Internal Bug, or External when createmeta includes Description formatted / `customfield_10103`):**

Call `createJiraIssue` with **full Tier 1 colored Key details ADF** in **`additional_fields.customfield_10103`** (or the createmeta key if different — see **Key details ADF template**). Build ADF from **every** review-file section (Description TL;DR, context, steps, Actual bullets, expected, environment, technical details, example when present). Do NOT condense or summarize.

- **`description`:** Omit or leave empty when MCP allows.
- **Create validation fails** because standard Description is required: retry **once** with this **minimal stub** only (markdown or plain ADF per MCP):

```
<Summary line from review file>

Full details in Description formatted.
```

Do **not** expand the stub in later steps. **MUST NOT** put the full bug body in standard `description`.

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "projectKey": "<board project key>",
  "issueTypeName": "Internal Bug",
  "summary": "<summary from review file>",
  "contentFormat": "adf",
  "parent": "<parent ticket key, or omit if none>",
  "assignee_account_id": "<assigneeAccountId from Step 0/1b — omit if empty or Leave unset>",
  "additional_fields": {
    "priority": { "name": "<priority name>" },
    "labels": ["<Backend or Frontend>"],
    "reporter": { "accountId": "<currentUserAccountId from Step 0, or omit if empty>" },
    "customfield_10095": { "accountId": "<testerAccountId from Step 0/1b — omit if empty or Leave unset>" },
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* Key details colored ADF — full template */ ] }
  }
}
```

If create fails because **`customfield_10103`** is empty/rejected: retry once with a minimal 10103 stub (single paragraph: *Details pending — see review file*), then rely on **5c** for the full ADF after **5b** (mandatory when an image was uploaded; otherwise triggered by the **5d** failure the stub will produce).

**Assignee / Tester payload (MUST):** When `assigneeAccountId` / `testerAccountId` are set (user picked or auto-resolved), **MUST** include them on create. **Internal:** `assignee_account_id` and `customfield_10095.accountId`. **External:** `assignee_account_id` if assignee is in createmeta; Tester on the **createmeta Tester field key** (not assumed `customfield_10095`). When user confirmed **Leave unset** in Step 1b, **omit** the corresponding field — do not send empty accountId objects.

**Legacy Ph2 (no `customfield_10103`):**

Call `createJiraIssue` with the **complete bug description** — every section from the review file, formatted as a single markdown string.

> **MANDATORY (legacy):** Copy ALL sections from the review file into markdown `description`. Do NOT condense or summarize.

Build the `description` as a markdown string with this structure:

```
<Description paragraph>

**Steps to reproduce:**
1. <step 1>
2. <step 2>
...

**Actual result:**
<actual result>

**Expected result:**
<expected result>

**Environment:**
- Environment: <Dev / Test / PreProd / Prod>

**Technical details:**
- Endpoint: <METHOD /api/path>
- Payload: <payload>
- Response: <response>
- Status: <status code>

**Example:**
<JSON or log snippet>
```

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "projectKey": "<board project key>",
  "issueTypeName": "Internal Bug",
  "summary": "<summary from review file>",
  "description": "<complete markdown bug description — all sections>",
  "contentFormat": "markdown",
  "parent": "<parent ticket key, or omit if none>",
  "assignee_account_id": "<assignee accountId from Step 0, or omit if empty>",
  "additional_fields": {
    "priority": { "name": "<priority name>" },
    "labels": ["<Backend or Frontend>"],
    "reporter": { "accountId": "<currentUserAccountId from Step 0, or omit if empty>" },
    "customfield_10095": { "accountId": "<testerAccountId from Step 0, or omit if empty>" }
  }
}
```

> **Reporter note:** Setting `reporter` via `additional_fields` only succeeds when the API token owner has "Modify Reporter" permission on the project. If Jira rejects it (returns a field-validation error on `reporter`), log the error and continue without the reporter field — the ticket is still created under the token owner's name, which is acceptable.

Note the returned `key` (e.g. `PHN-3718`). Proceed immediately to Step 5b.

---

#### Step 5b — Upload screenshot (after create, before verification)

> **MANDATORY SEQUENCE — DO NOT SKIP OR COMBINE:**
> Execute each part as a separate operation: **5a → 5b → 5c → 5d** when 5b uploaded an image, otherwise **5a → 5b → 5d** (plus conditional **5c** when **5d** fails).
> - Do NOT skip Step 5b even if you believe no screenshot exists — always check the file path first.
> - Do NOT use Shell Jira REST to patch formatting — MCP **`editJiraIssue`** only for **5c**.

> **Why 5b precedes the embed:** Step **5a** cannot include an inline `mediaSingle` because the attachment does not exist yet. Once **5b** has uploaded the file and resolved `ATTACHMENT_MEDIA_UUID`, **5c** inserts the media node, and **5d** confirms it actually rendered.

> **Screenshot scope:** Applies to **Backend**, **Frontend**, and **DB** bugs when the reporter provided image evidence (UI, network tab, Postman, logs, SQL/DB result, etc.). Not required when no image was provided.

Check if a screenshot file exists at:

```
<same directory as the review file>\<review-file-basename>_screenshot.png
<same directory as the review file>\<review-file-basename>_screenshot_expected.png
```

Also check common alternative extensions: `_screenshot.jpg`, `_screenshot.jpeg`, `_screenshot.webp` (and `_screenshot_expected.*` for Expected).

**If a screenshot file EXISTS:**

Run via Shell and capture the full output:

```powershell
$uploadOut = & "c:\Users\g.gamjashvili\new_cursor\CURSOR-PROJECT\.cursor\commands\attach-screenshot-to-jira.ps1" `
    -IssueKey "<key from Step 5a>" `
    -ScreenshotPath "<full absolute path to screenshot file>"
$uploadExit = $LASTEXITCODE
```

Parse `$uploadOut` for these structured lines (the script writes them to stdout):

```
ATTACHMENT_ID=<integer>
ATTACHMENT_FILENAME=<filename>
ATTACHMENT_THUMBNAIL=<url>
ATTACHMENT_CONTENT_URL=<url>
ATTACHMENT_MEDIA_UUID=<uuid>
ATTACHMENT_WIDTH=<integer>
ATTACHMENT_HEIGHT=<integer>
```

Extract from stdout:

- `ATTACHMENT_FILENAME` — `media` `alt`, and the fallback screenshot line when no UUID resolves
- `ATTACHMENT_MEDIA_UUID` — **the value for ADF `media.attrs.id`** in Step **5c**
- `ATTACHMENT_WIDTH` / `ATTACHMENT_HEIGHT` — intrinsic pixel size for `media.attrs.width` / `media.attrs.height`
- `ATTACHMENT_ID` — Jira attachment id (integer); **not** usable in `media.attrs.id`
- `ATTACHMENT_CONTENT_URL` — optional; the script derives the UUID from this URL's 303 redirect

**Do not** attempt to recover a media UUID from `getJiraIssue` `fields: ["attachment"]` — the Jira attachment payload exposes only the numeric id, `content`, and `thumbnail`. The upload script resolves the UUID and prints it; if `ATTACHMENT_MEDIA_UUID` is absent, treat the embed as unavailable and use the fallback screenshot paragraph.

- Exit code **0**: screenshot uploaded — retain all parsed lines for mandatory **5c** and **5d**
- Exit code **1**: **Jira authentication failed** — notify the user at the end of the workflow: `⚠️ Screenshot upload failed: Jira API token authentication error. Check JIRA_EMAIL and JIRA_API_TOKEN in Cursor-Project/.env.` Proceed to **5d** without the screenshot.
- Exit code **2**: upload failed for another reason — warn user, proceed to **5d** without the screenshot
- No matching file: skip silently, proceed to **5d** without screenshot

**Format conversion is automatic:** The script converts WebP, BMP, TIFF, and other non-PNG/JPG formats to PNG automatically. If conversion fails, the original file is uploaded as-is. The output `ATTACHMENT_FILENAME` is the final name used (converted filename if conversion succeeded).

---

#### Key details ADF template (`customfield_10103`) — Tier 1

Used for **Internal Bug** and **External Bug** when split ADF applies. Build at **Step 5a** create and reuse for conditional **Step 5c** patches. Source: approved bug review file.

**ADF structure rules (MUST — prevents GB-1772-style corruption):**

- Each section is a **top-level** node in `content[]`: colored **header paragraph** first, then body (`paragraph`, `orderedList`, `bulletList`, `codeBlock`, or `mediaSingle` nodes). **Never** start `content` with `orderedList` without the **Description:** and **Reproduce Steps:** header paragraphs above them in order.
- **Description:** block comes **first** — header → bold TL;DR paragraph → context paragraph(s). **Do not** fold Description into Reproduce Steps or Actual Result.
- **Close the reproduce `orderedList` before** the **Actual Result:** header. Actual bullets, screenshot, Expected, Environment, separator, and API lines are **siblings** after the list — **never** inside the last `listItem`.
- **Actual Result** uses ADF `bulletList` (Option B) — not a single paragraph for all actual content.
- **Payload**, **Response**, and **Example** values use **`codeBlock`** nodes (not inline paragraphs with hard breaks).
- API label lines (Endpoint, Method, Status) are separate **paragraph** nodes after the `_________________` paragraph; Payload/Response/Example label paragraphs precede their `codeBlock`.
- Step **5c** patches **`customfield_10103` only** (do not send `description`) — mandatory once when **5b** uploaded an image, otherwise only on **5d** failure.

**Mandatory section order for `customfield_10103`:**

1. **Description:** — header (`#403294`, strong + textColor) → **bold TL;DR** paragraph (entire paragraph `strong`, Option A — mirrors Summary) → context paragraph(s) with `strong`/`code` marks as needed.
2. **Reproduce Steps:** — header (`#00B8D9`) → `orderedList` of all steps (step text may use `code` for technical values). **No** Description fold-in.
3. **Actual Result:** — header (`#FF5630`) → `bulletList` (≥1 symptom bullet; optional proof/scope bullets) → `mediaSingle` **when Step 5b exit 0 and `ATTACHMENT_MEDIA_UUID` resolved** (any label — UI, API, logs, SQL/DB evidence). No image provided → no node, no placeholder.
4. **Expected Result:** — header (`#36B37E`) → paragraph(s) → optional `mediaSingle` when second screenshot uploaded (`*_screenshot_expected.png`).
5. **Environment:** — unmarked paragraph(s) from review **Environment:** (e.g. `Environment: Dev2`).
6. Plain paragraph: `_________________`
7. API label lines — each a single paragraph; label word with `#5E6C84` strong + textColor, value plain:
   - `Endpoint: <METHOD /path>`
   - `Method: <METHOD>`
   - `Status: <code and text>`
8. **Payload** — label paragraph (`Payload` colored) → **`codeBlock`** (`language: "json"` when JSON).
9. **Response** — label paragraph → **`codeBlock`**
10. **Example** (when review file has Example): label paragraph → **`codeBlock`** (support `// FAILS` / `// WORKS` compare blocks).

**Example header node (Description):**

```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Description:",
      "marks": [
        { "type": "strong" },
        { "type": "textColor", "attrs": { "color": "#403294" } }
      ]
    }
  ]
}
```

**Example TL;DR paragraph (Option A — entire sentence bold):**

```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Reminder list returns empty when customer has active objection withdrawal.",
      "marks": [ { "type": "strong" } ]
    }
  ]
}
```

**Example header node (Reproduce Steps):**

```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Reproduce Steps:",
      "marks": [
        { "type": "strong" },
        { "type": "textColor", "attrs": { "color": "#00B8D9" } }
      ]
    }
  ]
}
```

**Example Actual Result bulletList (Option B):**

```json
{
  "type": "bulletList",
  "content": [
    {
      "type": "listItem",
      "content": [
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "API returns " },
            { "type": "text", "text": "200 OK", "marks": [ { "type": "code" } ] },
            { "type": "text", "text": " with empty " },
            { "type": "text", "text": "content[]", "marks": [ { "type": "code" } ] },
            { "type": "text", "text": "." }
          ]
        }
      ]
    },
    {
      "type": "listItem",
      "content": [
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "Proof: response body " },
            { "type": "text", "text": "{\"totalElements\":0}", "marks": [ { "type": "code" } ] },
            { "type": "text", "text": "." }
          ]
        }
      ]
    }
  ]
}
```

**Example Payload label + codeBlock:**

```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Payload",
      "marks": [
        { "type": "strong" },
        { "type": "textColor", "attrs": { "color": "#5E6C84" } }
      ]
    }
  ]
},
{
  "type": "codeBlock",
  "attrs": { "language": "json" },
  "content": [ { "type": "text", "text": "{\n  \"customerId\": 12345\n}" } ]
}
```

**Example API line (Endpoint):**

```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Endpoint",
      "marks": [
        { "type": "strong" },
        { "type": "textColor", "attrs": { "color": "#5E6C84" } }
      ]
    },
    { "type": "text", "text": ": GET /power-supply-disconnection-reminder/list" }
  ]
}
```

**Example `mediaSingle` under Actual Result (when `ATTACHMENT_MEDIA_UUID` resolved at Step 5b — any Backend/Frontend/DB label):**

Shape verified against a correctly rendering embed on **PHN-4249** (`customfield_10103`, attachment `70014`).

```json
{
  "type": "mediaSingle",
  "attrs": { "layout": "center", "width": 647, "widthType": "pixel" },
  "content": [
    {
      "type": "media",
      "attrs": {
        "type": "file",
        "id": "<ATTACHMENT_MEDIA_UUID from Step 5b>",
        "alt": "<ATTACHMENT_FILENAME>",
        "collection": "",
        "width": <ATTACHMENT_WIDTH>,
        "height": <ATTACHMENT_HEIGHT>
      }
    }
  ]
}
```

**Attribute rules (MUST):**

- **`id`** is the **media UUID** (`ATTACHMENT_MEDIA_UUID`), never the numeric `ATTACHMENT_ID`. Jira silently drops the node when the id is wrong — the edit still returns success, so a dropped node is invisible without **5d**.
- **`collection`** is the empty string `""`.
- **`media.attrs.width` / `height`** are the image's intrinsic pixel size (`ATTACHMENT_WIDTH` / `ATTACHMENT_HEIGHT`). Omit both when the script did not emit them.
- **`mediaSingle.attrs.width`** is display width — keep `647` with `widthType: "pixel"` and `layout: "center"`.
- Do **not** add `localId` attrs; those are Jira UI artifacts.

Place **Actual** `mediaSingle` immediately after the Actual `bulletList`. Place **Expected** `mediaSingle` after Expected paragraph(s). If `ATTACHMENT_MEDIA_UUID` is missing after **5b** exit 0, use the unmarked fallback paragraph `Screenshot: <ATTACHMENT_FILENAME> (attached)` instead, and report the screenshot as **attached only — not embedded** at the end of the workflow.

**Body text formatting rules (customfield_10103 — split ADF):**

1. **TL;DR (Option A):** Standalone paragraph immediately after **Description:** header; **entire paragraph** uses `{ "type": "strong" }` on all text nodes; text derived from Summary (strip `[Backend/Frontend/DB]` prefix if redundant in body).
2. **Context:** 1–2 paragraphs after TL;DR; `strong` for feature/screen/component names (max ~3 terms per paragraph), `code` for Jira keys, IDs, formulas, endpoints, errors, field names.
3. **Actual bullets (Option B):** After **Actual Result:** header, use ADF `bulletList`:
   - **Required:** ≥1 bullet (symptom)
   - **Optional:** proof bullet (exact payload/response/SQL with inline `code`)
   - **Optional:** scope bullet (frequency, all cases, compare case)
   - **Forbidden:** empty or invented filler bullets
4. **Charset:** Use `→` in step/bullet text; never `?` as arrow substitute.
5. **Screenshots:** When user provided image(s) and Step **5b** exit **0**, embed `mediaSingle` under **Actual Result** (and under **Expected Result** for second image) via mandatory **5c** — **not** gated on Frontend label only. When no image was provided, omit entirely; screenshots are optional and never block bug creation.

All review-file sections map into **`customfield_10103` only** on split-ADF boards — not into standard `description`.

---

#### Step 5d — Verify Key details rendering (split ADF boards — MANDATORY)

Required when split ADF applies (**Internal Bug**, **External Bug**, or any issue type with formatted Key details on `customfield_10103`).

**When 5d runs:** immediately after the mandatory **5c** when Step **5b** uploaded an image; otherwise immediately after **5b**. (Step **5c** is documented below this section — follow the order stated here, not the document order.)

Call:

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "issueIdOrKey": "<key>",
  "fields": ["customfield_10103", "description"],
  "expand": "renderedFields"
}
```

**Pass criteria for `renderedFields.customfield_10103`:**

- HTML contains color markup for section headers (e.g. `color="#403294"` for Description, `color="#00B8D9"`, `color="#FF5630"`, or `<font color=` equivalents).
- **Description:** header appears **before** **Reproduce Steps:** (Tier 1 order).
- Bold TL;DR visible under Description section (HTML `<strong>` immediately after Description header).
- **Reproduce Steps:** header appears **before** the `<ol>` (not missing).
- Reproduce steps present (`<ol>` or numbered content).
- **Actual Result** uses `<ul>` (`bulletList`) — not only a single `<p>` for all actual content.
- Actual and expected content present **outside** the last reproduce list item (no nested actual/expected inside `<li>`).
- API block includes **Endpoint** and **Method** after the separator (not inside the list).
- When review file had Payload/Response/Example: rendered HTML includes `<pre>` / code-block markup for those sections.
- **Content coverage** vs approved review file (no dropped sections).
- **Screenshot criterion — applies only when Step 5b uploaded an image** (exit **0**, ≥1 file). Then the rendered HTML **MUST** contain a real image embed (`<img>` / media markup) under **Actual Result**, and under **Expected Result** when a second image was uploaded. A text line naming `ATTACHMENT_FILENAME` is **NOT** a pass — it is the fallback used when no UUID resolved, and it must be reported as *attached only — not embedded*.
- **When no image was provided, the screenshot criterion does not apply** and **MUST NOT** cause a failure or a warning.

**Standard `description` (split ADF):** Must be empty or stub-only (e.g. *Full details in Description formatted.*). If full duplicate markdown/HTML appears, treat as workflow violation — do not claim success until corrected manually or via a one-time stub trim (never add full body to `description` in **5c**).

**On pass:** Proceed to post-workflow (CREATED header + URL). When an image was uploaded, report `Screenshot embedded under Actual Result`.

**On failure:** Run **Step 5c** once more (patch **`customfield_10103` only**), then **5d** again. If still failing after at most **two** **5c** attempts, warn: `⚠️ Key details (Description formatted / customfield_10103) incomplete — verify in Jira.` Do **not** claim full formatting success.

**On screenshot-only failure** (all other criteria pass; image uploaded but no embed rendered, or `ATTACHMENT_MEDIA_UUID` never resolved): do not retry indefinitely. State plainly in the final response: `⚠️ Screenshot attached to the ticket but not embedded under Actual Result.` **MUST NOT** report the screenshot as embedded when only a filename line is present.

---

#### Step 5c — Screenshot embed / repair patch (split ADF) or mandatory colored Description (legacy)

**Split ADF (Internal Bug or External Bug with `customfield_10103`):**

**When to run:**

| Condition | 5c |
|---|---|
| Step **5b** uploaded ≥1 image (exit **0**) | **Mandatory, once, before 5d** — this is the only step that can place the `mediaSingle` in Key details |
| No image uploaded | **Conditional** — only when **5d** fails |
| **5d** fails after a mandatory 5c | One more attempt (two per ticket maximum) |

**Content is unchanged either way:** rebuild the **same** Tier 1 Key details ADF from the approved review file — identical section order, colours, marks, lists, separator, API lines, and `codeBlock`s — and add the media node(s). **5c MUST NOT** alter, reword, reorder, or restyle any existing content. The only delta versus what **5a** sent is the `mediaSingle` under **Actual Result** (and under **Expected Result** for a second image).

> **Forbidden:** formatting via Shell Jira REST `PUT`/`POST` — use **`editJiraIssue`** only. **`fields` must contain only `customfield_10103`** — no `description` key.

Call `editJiraIssue` with full Tier 1 Key details ADF from the review file (include screenshot `mediaSingle` or fallback under Actual/Expected when **5b** succeeded — any label):

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "issueIdOrKey": "<key from Step 5a>",
  "contentFormat": "adf",
  "fields": {
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* Key details colored ADF — full template */ ] }
  }
}
```

At most **two** **5c** attempts per ticket. After each **5c**, re-run **5d**.

**Legacy Ph2 (no `customfield_10103`) — MANDATORY after 5b:**

> **CRITICAL (legacy):** `createJiraIssue` with markdown produces no colors. Call `editJiraIssue` immediately after **5b** with colored ADF on **`description` only**. Never skip for legacy boards.

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "issueIdOrKey": "<key from Step 5a>",
  "contentFormat": "adf",
  "fields": {
    "description": { "type": "doc", "version": 1, "content": [ /* legacy colored template below */ ] }
  }
}
```

**Legacy (non-split) boards** — ADF on `description` must contain:
- All colored bold section headers (see color table below)
- In the **"Actual result"** section: screenshot paragraph **if** Step 5b succeeded (see screenshot node in standard template)
- If Step 5b found no screenshot or upload failed: omit screenshot nodes

Proceed to post-workflow steps after **5d** pass (split ADF) or after legacy **5c**.

---

**Description field — Jira ADF format with colored section headers (legacy non-split `description` only):**

The description must be formatted as Atlassian Document Format (ADF). Every section label is a **bold + colored** paragraph node followed by the section content.

**Color scheme for section headers:**

| Section label | Color | Hex |
|---------------|-------|-----|
| Description: | Bold Purple | `#6554C0` |
| Steps to reproduce: | Bold Teal | `#00B8D9` |
| Expected result: | Bold Green | `#36B37E` |
| Actual result: | Bold Red | `#FF5630` |
| Environment: | Bold Grey | `#5E6C84` |
| Technical details: | Bold Grey | `#5E6C84` |
| Example: | Bold Grey | `#5E6C84` |

**ADF structure — each section follows this pattern:**

```json
[
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Description:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#6554C0" } }
        ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "<One sentence summary of what the bug is — bold>",
        "marks": [ { "type": "strong" } ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      { "type": "text", "text": "On " },
      { "type": "text", "text": "<Feature or component name>", "marks": [ { "type": "strong" } ] },
      { "type": "text", "text": ", the " },
      { "type": "text", "text": "<key field or area name>", "marks": [ { "type": "strong" } ] },
      { "type": "text", "text": " displays " },
      { "type": "text", "text": "<raw technical value>", "marks": [ { "type": "code" } ] },
      { "type": "text", "text": " instead of the expected behavior." }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Steps to reproduce:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#00B8D9" } }
        ]
      }
    ]
  },
  {
    "type": "orderedList",
    "content": [
      {
        "type": "listItem",
        "content": [
          {
            "type": "paragraph",
            "content": [
              { "type": "text", "text": "Log in to " },
              { "type": "text", "text": "<environment name>", "marks": [ { "type": "strong" } ] },
              { "type": "text", "text": " portal" }
            ]
          }
        ]
      },
      {
        "type": "listItem",
        "content": [
          {
            "type": "paragraph",
            "content": [
              { "type": "text", "text": "Open " },
              { "type": "text", "text": "<Feature name>", "marks": [ { "type": "strong" } ] },
              { "type": "text", "text": " → navigate to record " },
              { "type": "text", "text": "<id or key value>", "marks": [ { "type": "code" } ] }
            ]
          }
        ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Actual result:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#FF5630" } }
        ]

      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "<One sentence summary of what goes wrong — bold>",
        "marks": [ { "type": "strong" } ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      { "type": "text", "text": "<Feature or component name>", "marks": [ { "type": "strong" } ] },
      { "type": "text", "text": " shows " },
      { "type": "text", "text": "<raw value or wrong output>", "marks": [ { "type": "code" } ] },
      { "type": "text", "text": ". No " },
      { "type": "text", "text": "<expected behavior>", "marks": [ { "type": "strong" } ] },
      { "type": "text", "text": " applied." }
    ]
  },

  /* ── SCREENSHOT NODE (include only when Step 5b uploaded a screenshot) ──────
     Place this paragraph as the LAST node inside the "Actual result" section,
     immediately before the "Expected result:" header paragraph.
     Replace <ATTACHMENT_FILENAME> with the value from the ATTACHMENT_FILENAME
     output line of the upload script. Omit entirely if no screenshot was uploaded.
  */
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Screenshot: ",
        "marks": [ { "type": "strong" } ]
      },
      {
        "type": "text",
        "text": "<ATTACHMENT_FILENAME>",
        "marks": [ { "type": "code" } ]
      },
      {
        "type": "text",
        "text": " (attached)"
      }
    ]
  },
  /* ── END SCREENSHOT NODE ────────────────────────────────────────────────── */

  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Expected result:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#36B37E" } }
        ]

      }
    ]
  },
  {
    "type": "paragraph",
    "content": [ { "type": "text", "text": "<expected result>" } ]
  },

  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Environment:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#5E6C84" } }
        ]
      }
    ]
  },
  {
    "type": "bulletList",
    "content": [
      {
        "type": "listItem",
        "content": [
          { "type": "paragraph", "content": [ { "type": "text", "text": "Environment: <Dev/Test/PreProd/Prod>" } ] }
        ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Technical details:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#5E6C84" } }
        ]
      }
    ]
  },
  {
    "type": "bulletList",
    "content": [
      {
        "type": "listItem",
        "content": [ { "type": "paragraph", "content": [
          { "type": "text", "text": "Endpoint: " },
          { "type": "text", "text": "<METHOD /api/path>", "marks": [ { "type": "code" } ] }
        ] } ]
      },
      {
        "type": "listItem",
        "content": [ { "type": "paragraph", "content": [
          { "type": "text", "text": "Payload: " },
          { "type": "text", "text": "<payload>", "marks": [ { "type": "code" } ] }
        ] } ]
      },
      {
        "type": "listItem",
        "content": [ { "type": "paragraph", "content": [
          { "type": "text", "text": "Response: " },
          { "type": "text", "text": "<response>", "marks": [ { "type": "code" } ] }
        ] } ]
      },
      {
        "type": "listItem",
        "content": [ { "type": "paragraph", "content": [
          { "type": "text", "text": "Status: " },
          { "type": "text", "text": "<status code>", "marks": [ { "type": "code" } ] }
        ] } ]
      }
    ]
  },
  {
    "type": "paragraph",
    "content": [
      {
        "type": "text",
        "text": "Example:",
        "marks": [
          { "type": "strong" },
          { "type": "textColor", "attrs": { "color": "#5E6C84" } }
        ]
      }
    ]
  },
  {
    "type": "codeBlock",
    "attrs": { "language": "json" },
    "content": [ { "type": "text", "text": "<example JSON or log snippet>" } ]
  }
]
```

**Rules:**
- **Split ADF (Internal, External with 10103):** Tier 1 colored Key details template → **`customfield_10103` only** at **5a** and **5c** (Description first, Option A TL;DR, body marks, Option B Actual bullets, API `codeBlock`s, screenshot embed when **5b** succeeds). **MUST NOT** write full bug body to standard **`description`**. Do not use the legacy colored template on split-ADF `description`.
- **Legacy Ph2 (no `customfield_10103`):** Colored-marks template applies to **`description` only** at **5c**.
- Every section label paragraph contains ONLY the label text (bold + colored) — content follows in a separate node
- Steps to reproduce → `orderedList`
- Environment and Technical details bullets → `bulletList`
- Example → `codeBlock` with `language: "json"` when the example is JSON; use plain `codeBlock` (no language) otherwise
- Omit Technical details and Example nodes entirely when not applicable (UI-only bugs with no API)

**Body text formatting rules (legacy colored `description` only — non-split boards; do NOT apply to `customfield_10103`):**

Split-ADF **`customfield_10103`** uses **Body text formatting rules (customfield_10103 — split ADF)** in the Key details ADF template section above. The rules below apply **only** to legacy **`description`** ADF at Step **5c** on boards without `customfield_10103`.

1. **Bold summary sentence** — insert a standalone bold paragraph immediately after the `Description:` header and immediately after the `Actual result:` header. One sentence that captures the core problem. Use `{ "type": "strong" }` mark on the entire sentence text node.

2. **Bold key terms** — within body paragraphs, wrap component names, feature names, UI area names, and key nouns in `strong` mark. Use separate text nodes per segment: `{ "type": "text", "text": "Feature Name", "marks": [ { "type": "strong" } ] }`. Do NOT bold everything — only the most important 1–3 terms per paragraph.

3. **Inline code for technical values** — any value the user would copy-paste gets the `code` mark: endpoint paths, field names, token strings, IDs, status codes, payload keys, error codes. Use `{ "type": "code" }` mark on that text node. Examples: `$POD_TYPE$=$GENERATR$`, `id=3345`, `PUT /api/contracts/{id}`, `400 Bad Request`.

4. **Mixed content paragraphs** — a paragraph can contain multiple text nodes with different marks. Split the sentence into segments: plain text nodes for connective words, `strong` nodes for key terms, `code` nodes for technical values. Do NOT mark entire paragraphs — mark individual words or short phrases only.

**After Steps 5a–5b–5c–5d (split ADF; 5c mandatory when an image was uploaded, conditional otherwise) or legacy 5c complete:**
1. Return the Jira URL: `https://oppa-support.atlassian.net/browse/<ISSUE_KEY>`
2. If a screenshot was uploaded in Step 5b: confirm `Screenshot attached: <ATTACHMENT_FILENAME>` **and** state the embed outcome verified at **5d** — either `embedded under Actual Result` or `attached only — not embedded`. If no screenshot was provided, say nothing about screenshots.
3. Update the review file header from `# Bug Review — APPROVED` to `# Bug Review — CREATED — <ISSUE_KEY>` (edit in place)
4. **Delete** `.active-bugreview` sidecar
5. Display the confidence block and agents footer

---

## Confidence Score (Rule CONF.1)

Include a confidence score in the final response. Base: 40. Evidence factors:

| Factor | Points |
|--------|--------|
| Parent ticket read + tester/assignee extracted | +15 |
| All required bug fields provided by user | +15 |
| Board/sprint confirmed | +10 |
| Priority determined from clear description | +10 |
| User explicitly APPROVED | +10 |
| Missing bug fields (per missing field) | -5 each |
| Board inferred (no parent ticket) | -5 |
| Assumption made (per assumption) | -5 |

Format:

```
**Confidence: XX% (ZONE)**
Evidence: [+factors, -factors]
Reason: <1-2 sentences>
```

---

## Notes (split ADF — `customfield_10103` only)

- JQL, export, and email may surface only standard **`description`** (stub) — full text lives in **Description formatted**.
- The standard Description panel may look empty in Jira UI — intentional; primary panel is **Description formatted**.

---

## Error Handling

| Error | Action |
|-------|--------|
| `getJiraIssue` MCP fails | Retry once; if still fails, ask user to provide board/sprint/assignee/tester manually |
| Internal + parent: no QA/Test subtask or QA subtask unassigned | **Step 1b AskQuestion** for Tester (+ **Leave unset** option); **MUST NOT** proceed to Step 3 with bare `—` without ask |
| Assignee unresolved after chapter subtask + parent fallback | **Step 1b AskQuestion** for Assignee (+ **Leave unset**); **MUST NOT** create Jira without user pick or confirmed Leave unset |
| Environment missing after Step 0 (sources 1–3 empty: no user message, no parent text, no same-bug chat env) | **Step 0b AskQuestion** (six envs) **only after sources 1–3 are empty**; **MUST NOT** infer from fix version or Ph2 board; **MUST NOT** write review file until resolved |
| `createJiraIssue` fails — standard Description required | Retry **once** with minimal stub: summary line + `Full details in Description formatted.` — do not add full body |
| `createJiraIssue` fails — empty/rejected `customfield_10103` | Retry once with minimal 10103 stub paragraph; then **5b** → **5c** with full ADF → **5d** |
| `createJiraIssue` MCP fails (other) | Show error to user; do not retry silently; ask user how to proceed |
| Step 3 screenshot handoff fails (copy or verify — destination missing/empty) | **STOP** before Step 4; ask user to re-attach image or provide path; **MUST NOT** list screenshot filename in review file or proceed to Jira create until handoff passes |
| Screenshot upload (Step 5b) script exits with code 1 (auth error) | **Notify user at end of workflow:** `⚠️ Screenshot upload failed: Jira API token authentication error. Check JIRA_EMAIL and JIRA_API_TOKEN in Cursor-Project/.env.` Proceed to **5d** without screenshot |
| Screenshot upload (Step 5b) script exits with code 2 | Warn user to attach manually; include the file path and Jira ticket URL; proceed to **5d** without screenshot |
| `editJiraIssue` (Step 5c legacy or conditional split) fails | Warn user that ADF formatting was not applied; do not skip silently |
| Step 5d verification fails (split ADF) | Run **5c** with **`customfield_10103` only** (max two attempts); re-run **5d**; if still failing, warn — Description formatted panel may be incomplete |
| Step 5b exit 0 but no `ATTACHMENT_MEDIA_UUID` printed | Build **5c** with the fallback `Screenshot: <ATTACHMENT_FILENAME> (attached)` paragraph instead of `mediaSingle`; report `⚠️ Screenshot attached to the ticket but not embedded under Actual Result.` Do **not** retry the upload and do **not** block the ticket |
| No screenshot provided by the user | Normal path — no **5c** on screenshot grounds, no screenshot criterion at **5d**, no warning, nothing about screenshots in the final response. Screenshots are **optional** and never block bug creation |
| `customfield_10103` rejected at Step 5c | Log the error; warn: `⚠️ Could not populate the "Description formatted" field (customfield_10103) — please verify the ticket's Key details panel.` |
| `getVisibleJiraProjects` fails (External class) | Retry once; then ask user to type a project key; refuse denylist (rule file) and Experiments |
| External class but `externalProjectKey` unset after sources 1–3 | **AskQuestion** from visible projects minus denylist; **BLOCK** create until set |
| User selects or names a denylist project | Refuse; cite **External excluded project keys** in `phoenix_bug_reporter.mdc` |
| `getJiraProjectIssueTypesMetadata` has no exact `Bug` | **AskQuestion** with `"{name} — {id}"` from metadata; never guess |
| Createmeta required field (Epic, Fix version, other) has no value | **AskQuestion**; never guess field id or value |
| Createmeta has no Description formatted / `customfield_10103` | Legacy markdown **`description`** at 5a; **5c** on **`description` only**; do not send `10103` |
| Parent ticket is not a Phoenix Phase 2 board | Warn user; ask if they want to report on PHN anyway |
| User provides Experiments board | Refuse; redirect to `jira-bug` agent (JIRA.0) |

---

## Agents involved footer

Always end with: `Agents involved: phoenix-bug-reporter`
