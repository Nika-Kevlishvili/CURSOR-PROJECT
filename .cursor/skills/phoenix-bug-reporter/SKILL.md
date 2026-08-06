---
name: phoenix-bug-reporter
description: Full workflow for drafting and submitting Phoenix Phase 2 Internal Bug and External (standalone Bug) tickets. User selects allowed external Jira project at Step 0. Enforces approval gate via bug review file. Rule PHOENIX-BUG.0.
---

# Phoenix Bug Reporter — SKILL

Creates **Internal Bug** sub-tasks on Phoenix Phase 2 Jira boards and **External** standalone **Bug** tickets on user-selected allowed projects. Always produces a review file for user approval before touching Jira.

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
| **External (Ph2-related)** | Bug (standalone) | No | User-selected **allowed external** project (Step 0) |

**Allowed internal:** PHN and other Ph2 delivery project keys (from parent or user answer when Internal, no parent).

**Allowed external project keys (allowlist — extend only in `phoenix_bug_reporter.mdc`):** **`GB`** ([board 86](https://oppa-support.atlassian.net/jira/software/c/projects/GB/boards/86)). GB is **not** auto-selected.

**Board / project resolution:**
- User asks for **external** / **external bug** / standalone Bug → **External** class → **External project selection** (Step 0) — **always** ask which project (see below). Do **not** assume GB.
- Parent on Ph2 and user did **not** request External → **Internal**; board = parent `project.key`; issue type **Internal Bug** with `parent`.
- User names a project key or Jira board URL → map URL to project key; validate against allowlist for that class.
- Internal, no parent → ask Ph2 board (e.g. PHN) and sprint.
- **Experiments** or other non-approved projects → refuse and redirect (`jira-bug` for Experiments).

---

## Split ADF — `customfield_10103` only (MANDATORY when applicable)

**Applies to:**
- **PHN** (and Ph2) **Internal Bug** — Key details panel shows **Description formatted** (`customfield_10103`).
- **External class Bug** (e.g. on GB) — often requires **`customfield_10103`** at create; UI matches PHN Key details (GB-1772, GB-1773, 2026-07-21).

**Detection:** Use split ADF for **Internal Bug** or **External Bug** when `getJiraIssueTypeMetaWithFields` / createmeta shows **`customfield_10103`** required or present for that issue type and project.

**Authoritative bug body (split ADF boards):**

| Field | Behavior |
|-------|----------|
| **`customfield_10103`** | **Full** Tier 1 Key details ADF at **Step 5a** create — all review-file content mapped per **Key details ADF template** (Description + TL;DR first, body marks, steps, Actual `bulletList`, expected, env line, API `codeBlock` evidence, optional screenshot embed after **5b** via conditional **5c** when image provided — any Backend/Frontend/DB label). **`strong` + `textColor`** on section/API labels (exact hex below). |
| **`description`** | **Not** part of authoring: omit or empty at **5a**. If create validation requires standard Description, retry **once** with **minimal stub** only (see Step 5a). **MUST NOT** write the full bug body to standard `description` on split-ADF boards. |

Inline marks (`strong`, `textColor`, `code`) on the **standard `description`** field corrupt rendering (literal `{color:…}` / `*bold*`). Colored marks belong **only** on **`customfield_10103`**.

**Rules (split ADF boards):**
- Step **5a:** `createJiraIssue` with **full colored Key details ADF** in **`customfield_10103`** (`additional_fields` + ADF). Standard **`description`** omitted or empty; minimal stub retry only on create validation failure. (External projects may require Epic, Fix version, Environment ADF, Tester — see **External Bug (standalone)** and createmeta.)
- Step **5b:** Screenshot upload when file exists (after **5a**).
- Step **5d:** **Mandatory** after **5b** — verify **`renderedFields.customfield_10103`** only. **Pass → skip 5c** and finish.
- Step **5c:** **Conditional** — run **only when 5d fails**; `editJiraIssue` with **`customfield_10103` only** (no `description` key). At most **two** 5c attempts per ticket. **Forbidden:** Jira REST `PUT`/`POST` from Shell for formatting — MCP only. Temp ADF payload files must **not** include `description`.

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

When **bug class = External** and Step 0 stored **`externalProjectKey`** (e.g. **`GB`**):

**Review file:** Record **Bug class** = External; **Issue Type** = Bug; **Board** = `externalProjectKey`; **Parent** = —; include **Epic Link**, **Fix version** when known (ask user or resolve via createmeta before Step 5a).

**Step 0:** **Reporter** from `.env` (current user). **Tester** from `.env` (`JIRA_REPORTER_EMAIL` lookup). No chapter-subtask assignee for External. A parent key in the user message for **context only** does **not** set Jira `parent` on External creates.

**Step 5a — `createJiraIssue` example (External Bug — GB shown as reference):**

```json
{
  "cloudId": "ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
  "projectKey": "<externalProjectKey from Step 0>",
  "issueTypeName": "Bug",
  "summary": "<from review file>",
  "contentFormat": "adf",
  "additional_fields": {
    "priority": { "name": "<priority>" },
    "labels": ["Frontend"],
    "fixVersions": [{ "name": "<fix version name>" }],
    "customfield_10008": "<Epic issue key, e.g. GB-1501>",
    "customfield_10095": { "accountId": "<testerAccountId from Step 0 — current user for External>" },
    "environment": {
      "type": "doc",
      "version": 1,
      "content": [{ "type": "paragraph", "content": [{ "type": "text", "text": "Dev2" }] }]
    },
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* Key details colored ADF — full template */ ] }
  }
}
```

- Omit top-level **`description`** when MCP allows. On validation error requiring standard Description, retry **once** with minimal stub (see Step 5a).
- **`customfield_10103`:** full Tier 1 Key details ADF at create — same structure as **Key details ADF template** (Description first, TL;DR, body marks, Actual bullets, API `codeBlock`s; screenshot `mediaSingle` nodes usually added in conditional **5c** after **5b** when user provided image(s) — Backend, Frontend, or DB).
- Resolve required fields via `getJiraIssueTypeMetaWithFields` for **`externalProjectKey`** when create fails; never guess Epic or Fix version — ask the user. Field ids may differ by project (GB uses `customfield_10008`, `customfield_10095`, etc.).
- **`reporter`:** same optional `additional_fields.reporter` rule as PHN; omit if Jira rejects.

**Steps 5b–5d:** Same as Internal split ADF — **5b** screenshot, **5d** verify `customfield_10103`, **5c** patch **`customfield_10103` only** when **5d** fails.

**Hook note:** `block-bugreview-unapproved-jira.ps1` guards **`Internal Bug`** and External **`Bug`**. Requires `# Bug Review — APPROVED` and `.active-bugreview` sidecar (Step 4 On Agree). Resolves review file via sidecar → single APPROVED scan → latest mtime fallback.

---

## Step-by-Step Workflow

**Entry:** Start at **Step 0** as soon as the user invokes the bug reporter. There is **no** pre-registration validity question. **Step 3.5** (MUST offer Validate/Skip/Cancel) may run **bug-validator** (Rule 32) against the review file before Jira consent. Jira consent is **Step 4 only** (after Step 3 and Step 3.5 resolved). The `beforeMCPExecution` hook `block-bugreview-unapproved-jira.ps1` blocks `createJiraIssue` for **Internal Bug** and **Bug** unless the review file first line is `# Bug Review — APPROVED` and `.active-bugreview` sidecar points to that file.

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
| User says **external**, **external bug**, standalone **Bug** (not Internal Bug), or names an allowed external project / external board URL | **External** |
| Ph2 **parent** ticket provided and user did **not** ask for External | **Internal** |
| No parent and intent unclear | **AskQuestion:** *How should this bug be reported on Jira?* → **Internal Bug** (subtask under a Ph2 ticket) / **External Bug** (standalone Bug, no parent) |

If the user switches class later (e.g. “actually external”), re-run **External project selection** or Internal board resolution as needed.

Store **`bugClass`** for the review file and Step 5a.

#### External project selection (MANDATORY when `bugClass` = External)

**Do not** default to GB. **`externalProjectKey`** must come from user choice or an explicit allowed key/URL in the user message.

**Skip AskQuestion** only when the user already named an **allowed external project key** (e.g. `GB`) or a Jira Software board URL whose project key is on the allowlist — set **`externalProjectKey`** to that key.

**Otherwise (always ask, including when only one allowed project exists):**

1. Call Jira MCP **`getVisibleJiraProjects`** with `cloudId`: `ad451d5c-7331-46f8-9a47-f51dc8e6bbde`.
2. **Filter** projects to keys in **Allowed external project keys** (see **Bug class and allowed targets** — currently **`GB`** only).
3. **AskQuestion** — one question, options built from filtered projects: label `"{key} — {name}"`, value = project key.

   **Prompt:** *Which Jira project should this external bug be created on?*  
   (Only Phase 2–approved external projects you can access are listed.)

4. Store the chosen key as **`externalProjectKey`**. Ticket creation uses **`projectKey`** (`externalProjectKey`); board id is not required for `createJiraIssue`.

**If `getVisibleJiraProjects` fails:** retry once; then ask the user to type a project key and validate it against the external allowlist.

**If the allowlisted project is missing from visible projects:** warn that Jira access may be insufficient; ask user to confirm the key manually or fix permissions — do **not** create on an unlisted project.

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

- Complete **External project selection** above before Step 1 (unless skipped per explicit key/URL).
- **Assignee** = empty unless user specifies one for the external project.
- Optional parent key in the message is **context only** — do **not** set Jira `parent` on create.

Proceed to **Step 0b** when board/project (`board` or **`externalProjectKey`**) and class are resolved.

---

**Deprecated — do not use:** treating “no parent” as Internal-only without asking class; auto-routing External requests to GB without **External project selection**.

### Step 0b — Environment gate (MANDATORY)

Resolve **`resolvedEnvironment`** before Step 1. Canonical procedure aligns with **`.cursor/skills/environment-resolver/SKILL.md`** (six envs only; no silent Test default).

**Resolution order (strict):**

1. **Explicit env in the current user message** — normalize to one of: `Dev`, `Dev2`, `Test`, `PreProd`, `Prod`, `Experiments` (aliases: dev2/dev-2, preprod/pre-prod, etc.).
2. **Parent Jira `environment` field** — when Internal + parent and `fields.environment` is non-null/non-empty after fetch; normalize to canonical name.
3. **Otherwise** → **AskQuestion** (standalone, exactly one question, six options):

   **Prompt:** *Which environment was this bug reproduced on?*

   - Dev
   - Dev2
   - Test
   - PreProd
   - Prod
   - Experiments

**Forbidden inference (MUST — violation if used as env source):**

- Fix version names (`Test 2 Release …`, `Release 4`, hotfix labels, etc.)
- PHN / Phase 2 board or parent `project.key`
- Parent sprint name
- Prior chat session unless user explicitly says "same as before" / "same env"
- Silent default to `Test` or any env without AskQuestion

**Gate:** Do **not** write the Step 3 review file until **`resolvedEnvironment`** is set. Review file **Environment** line must show the canonical name (e.g. `Dev2`), never bare `—`.

Store **`resolvedEnvironment`** for the review file, Step 3.5 bug-validator delegation, and Bug Content **Environment** section.

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
- Environment: <canonical env from Step 0b — e.g. Dev2>

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

## Pre-create validation

| Field | Value |
|-------|-------|
| **Status** | Not run / Skipped / Completed / Stopped |
| **Verdict** | — (VALID / NOT VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / INSUFFICIENT EVIDENCE / PROCESS BLOCKED) |
| **Confidence** | — |
| **Validated at** | — |

> Populated by Step 3.5 when user chooses **Validate**. Leave **Status** = **Not run** on first save.

### Validation summary

<One short paragraph — verdict + key evidence, or "Skipped by user">

### Quality Findings (from validator)

- <Finding or "None">

---

*Awaiting your response: **Agree** to submit to Jira, **Disagree** to request changes.*
```

**Screenshot handoff gate (Step 3 — MANDATORY when user provided an image):**

Run **immediately after** writing the review file and **before** displaying the clickable link or Step 3.5. Chat images live under Cursor `assets/` and may be ephemeral — the review-folder copy is the **durable** file Step **5b** uploads.

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
6. **On verify failure:** **STOP** — do **not** display the review link for Step 3.5 or proceed to Step 3.5. Tell the user the copy failed and ask them to **re-attach the image in chat** or provide a local file path, then retry Step 3 copy. **MUST NOT** list a screenshot filename in the review file when the destination file does not exist on disk.

**Gate rules (BLOCK):**

- **MUST NOT** use `-ErrorAction SilentlyContinue` on screenshot copy.
- **MUST NOT** write `Screenshots: Actual: <filename>` unless `Test-Path` on that destination succeeds and file size &gt; 0.
- **MUST NOT** proceed to Step 3.5 while user provided an image but Actual screenshot handoff failed.

**When no image was provided in chat:** Set `Screenshots: Actual: —` and `Expected: —`; skip copy; proceed to Step 3.5.

**Screenshot scope:** Not Frontend/UI-only. Valid evidence includes UI captures, browser network tab, Postman/Swagger response, application logs, SQL/query result grids, DB client views — embed under **Actual Result** (and **Expected Result** for second image) when upload succeeds at Step **5b** / embed at **5c**, for **any** Backend/Frontend/DB label.

Write this file to disk using the file write tool. After writing:
1. Display the review file as a **clickable markdown link** using the full absolute path so the user can open it directly in the IDE:

```
Review file: [BugReview_<slug>_<HHMM>.md](c:\Users\g.gamjashvili\new_cursor\CURSOR-PROJECT\Cursor-Project\reports\Bug Reports\YYYY\<month>\<DD>\BugReview_<slug>_<HHMM>.md)
```

2. Proceed to **Step 3.5** — do NOT display the full file content in chat; the clickable link is sufficient for the user to review it.

---

### Step 3.5 — Pre-create validation (bug-validator)

**Purpose:** Before Jira consent, optionally run **Rule 32** validation against the draft review file to confirm the bug is real and the report aligns with Confluence, code, and Swagger. **No changes to bug-validator** — the reporter delegates via **Task** (`subagent_type: bug-validator`) and passes the review file as the bug description.

**When:** Immediately after Step 3 (review file on disk, screenshot handoff passed if applicable, clickable link shown). **MUST** offer the AskQuestion below — user may choose Skip.

**AskQuestion — standalone, exactly one question, three options:**

```
Validate this bug before reporting to Jira?
  ● Validate (recommended)
  ● Skip validation
  ● Cancel
```

**On Cancel (#8, #12):**
- Set review header to `# Bug Review — CANCELLED`
- Replace the Agree/Disagree footer line with: `*Report cancelled — do not submit to Jira.*`
- **Clear sidecar** (see **Active review sidecar** below)
- Stop workflow. Do not call Step 4 or create Jira ticket.

**On Skip validation:**
- Update review file **Pre-create validation** section: **Status** = `Skipped`, **Verdict** = `—`, summary = `Skipped by user at Step 3.5.`
- Proceed to **Step 4**.

**On Validate:**

1. **Resolve environment** from review file **Environment** line (e.g. `Dev2`). If missing or ambiguous → **AskQuestion** with six envs (`dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`) before delegating. Do not silently default to `test`.

2. **Parent prefetch (#9 — Internal + parent key):** Before Task delegation, when `bugClass = Internal` and parent ticket key exists:
   - Call `getJiraIssue` on the **parent** (delivery ticket, e.g. `PHN-3795`) with fields for summary, description, and links
   - Extract **parent summary** and **linked Confluence URLs** from parent description, comments, or remote links when available
   - Pass into the Task prompt as **Parent delivery context** (below)

3. **Delegate to bug-validator** via **Task** tool:

```
subagent_type: bug-validator
description: Pre-create bug validation for BugReview
prompt: |
  Pre-create validation for a Phoenix bug **review file** (no Jira bug ticket exists yet).

  **Review file path:** <full absolute path to BugReview_*.md>
  **Environment:** <env from review file or user answer — user must have confirmed>
  **Bug class:** Internal / External

  **Parent delivery context (when Internal + parent):**
  **Parent delivery ticket:** <parent key>
  **Parent summary:** <from getJiraIssue parent>
  **Linked Confluence from parent:** <urls or none>
  **Phase 2 pre-create context:** Apply bug-validation SKILL Step 2c user override:
    Phase 2 excluded: no (pre-create Ph2 delivery — parent <key>).
  Validate expected behavior against parent + linked Confluence + review file — not Phase 1 wiki alone.

  **Instructions:**
  - There is **no Jira bug key** — treat the **Bug Content** and **Code evidence** sections of the review file as the bug report (Steps to reproduce, Expected, Actual, Technical details, Example).
  - Run full Rule 32 workflow per `.cursor/skills/phoenix-bug-validation/SKILL.md`.
  - If screenshots exist next to the review file (`*_screenshot.png`), note their paths as visual evidence.
  - Return: **verdict** (one of VALID / NOT VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / INSUFFICIENT EVIDENCE / PROCESS BLOCKED), **confidence score + zone**, **Validation summary** (2–4 sentences), **Quality Findings** bullets (Rule QA.2), and for NEEDS CLARIFICATION / NEEDS APPROVAL a **What needs clarification** list.
  - Do NOT create Jira tickets or edit Phoenix code.
```

4. **If bug-validator Task fails or times out (#10):** **AskQuestion:** Retry validation / **Cancel** only — **do not** offer Skip (Skip is only on the initial Step 3.5 ask).

5. **Append results** to the review file **Pre-create validation** section (edit in place, same path):
   - **Status** = `Completed` on VALID; `Stopped` on all stop verdicts
   - **Verdict**, **Confidence**, **Validated at** (timestamp)
   - **Validation summary** and **Quality Findings** from subagent output

6. **Show validation outcome in chat** (short — verdict + confidence + link to updated review file). Do not dump the full validator report unless the user asks.

7. **Gate Step 4** by verdict — **strict; no "proceed anyway":**

| Verdict | Action |
|---------|--------|
| **VALID** | Proceed to **Step 4** Agree/Disagree |
| **NOT VALID** | **Stop**. Set header `# Bug Review — VALIDATION STOPPED`. Replace footer with `*Validation stopped — do not submit to Jira.*`. **Clear sidecar**. Explain **why**. **Do not** offer Agree |
| **NEEDS CLARIFICATION** | **Stop**. Same header/footer/sidecar as NOT VALID. List **what needs clarification**. **Do not** offer Agree |
| **NEEDS APPROVAL** | **Stop** (same as NEEDS CLARIFICATION) |
| **INSUFFICIENT EVIDENCE** | **Stop**. Same header/footer/sidecar. Explain missing evidence. **Do not** offer Agree |
| **PROCESS BLOCKED** | **Stop**. Same header/footer/sidecar. Explain blocker. **Do not** offer Agree |

**Footer patch (#12):** On stop or cancel only — replace the line `*Awaiting your response: **Agree** to submit to Jira, **Disagree** to request changes.*` Do **not** change Step 3 initial template on first save; patch footer only when stopping or cancelling.

**Chat template on stop:**

```
Validation: <VERDICT> (<confidence> <zone>)

Why: <2–4 sentences with Confluence/code evidence — for NOT VALID>

What needs clarification: <bullets — for NEEDS CLARIFICATION / NEEDS APPROVAL only>

Quality Findings:
- <Finding bullets>

Bug report stopped. Review file updated. No Jira ticket will be created.
```

**After Step 4 Disagree loop:** When the user edits the review file, reset header to `# Bug Review — PENDING APPROVAL`, clear stale validation (Status = `Not run`), **clear sidecar**, and return to **Step 3.5** (re-offer Validate / Skip / Cancel) before Step 4.

**Footer when validation ran:** append `bug-validator` to agents involved line.

---

### Active review sidecar (hook file resolution)

**Path:** `Cursor-Project/reports/Bug Reports/.active-bugreview`  
**Content:** Single line — absolute path to the review file for the current bug-report session.

| When | Action |
|------|--------|
| **Step 4 On Agree** | **Write** sidecar with full path to current review file (after APPROVED header) |
| **Step 5 post-create** | **Delete** sidecar |
| **VALIDATION STOPPED / CANCELLED** | **Delete** sidecar |
| **Step 4 Disagree** (before re-validate) | **Delete** sidecar if present |

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
> This question may ONLY be asked after Step 3 complete, Step 3.5 resolved (**VALID** path or **Skip validation**), the **screenshot handoff gate** has passed when the user provided an image, the clickable link has been displayed, and the review file header is **not** `# Bug Review — VALIDATION STOPPED`. Use a standalone **AskQuestion** call for Agree/Disagree — do not batch it with unrelated questions in the same call.

**Preconditions (BLOCK — do not offer Agree if any fail):**

- **Environment** is set in the review file (canonical name from Step 0b; not bare `—`).
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

- Reset review header to `# Bug Review — PENDING APPROVAL` and clear **Pre-create validation** (Status = `Not run`) if previously set
- **Clear sidecar** if present
- If user changes **Environment** on Disagree → re-run **Step 0b** resolution for the new value
- If user changes **Assignee** or **Tester** on Disagree → re-run **Step 1b** for changed fields only
- Return to **Step 3.5** (re-offer Validate / Skip / Cancel) before Step 4 Agree
- This loop repeats until the user selects Agree or explicitly cancels

**On explicit cancel / "don't report" / "abort":**
- Stop the workflow. Do not create a Jira ticket. Inform the user the review file remains saved at the shown path.

---

### Step 5 — Create Jira ticket (only after APPROVE)

This is a **mandatory sequence** executed in this exact order. Do NOT skip parts or reorder.

**Split ADF boards (Internal Bug or External Bug with `customfield_10103`):** **5a** → **5b** → **5d** (verify) → **5c** only if **5d** fails → optional second **5d**.

**Legacy Ph2 (no `customfield_10103`):** **5a** → **5b** → **5c** (colored `description`) — no **5d**.

---

#### Step 5a — Create the ticket

**Split ADF (Internal Bug or External Bug with `customfield_10103`):**

Call `createJiraIssue` with **full Tier 1 colored Key details ADF** in **`additional_fields.customfield_10103`** (see **Key details ADF template**). Build ADF from **every** review-file section (Description TL;DR, context, steps, Actual bullets, expected, environment, technical details, example when present). Do NOT condense or summarize.

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

If create fails because **`customfield_10103`** is empty/rejected: retry once with a minimal 10103 stub (single paragraph: *Details pending — see review file*), then rely on conditional **5c** for the full ADF after **5b**/**5d**.

**Assignee / Tester payload (MUST):** When `assigneeAccountId` / `testerAccountId` are set (user picked or auto-resolved), **MUST** include `assignee_account_id` and `customfield_10095.accountId` in create payload. When user confirmed **Leave unset** in Step 1b, **omit** the corresponding field — do not send empty accountId objects.

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
> Execute **5a → 5b → 5d** as separate operations in order (plus conditional **5c** when **5d** fails).
> - Do NOT skip Step 5b even if you believe no screenshot exists — always check the file path first.
> - Do NOT use Shell Jira REST to patch formatting — MCP **`editJiraIssue`** only for **5c**.

> **Why before 5d / conditional 5c:** First create usually cannot include inline screenshot `mediaSingle` (no attachment yet). After **5b**, **5d** checks embed or fallback line; failure triggers **5c** patch on **`customfield_10103` only**.

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
```

Extract from stdout:

- `ATTACHMENT_FILENAME` — required for fallback screenshot line and `media` `alt`
- `ATTACHMENT_ID` — Jira attachment id (integer)
- `ATTACHMENT_CONTENT_URL` — optional; use when resolving embed

After a successful upload (exit **0**), call `getJiraIssue` on the same key with `fields: ["attachment"]` and match the attachment whose `filename` equals `ATTACHMENT_FILENAME`. If the response exposes a media UUID suitable for ADF `media.attrs.id`, use it in conditional Step 5c `mediaSingle`. If no UUID is available, use the **fallback** screenshot paragraph in Key details (unmarked body text).

- Exit code **0**: screenshot uploaded — retain all parsed lines for **5d** / conditional **5c**
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
- On Step **5d** failure, conditional **5c** patches **`customfield_10103` only** (do not send `description`).

**Mandatory section order for `customfield_10103`:**

1. **Description:** — header (`#403294`, strong + textColor) → **bold TL;DR** paragraph (entire paragraph `strong`, Option A — mirrors Summary) → context paragraph(s) with `strong`/`code` marks as needed.
2. **Reproduce Steps:** — header (`#00B8D9`) → `orderedList` of all steps (step text may use `code` for technical values). **No** Description fold-in.
3. **Actual Result:** — header (`#FF5630`) → `bulletList` (≥1 symptom bullet; optional proof/scope bullets) → optional `mediaSingle` **when Step 5b exit 0** (any label — UI, API, logs, SQL/DB evidence).
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

**Example `mediaSingle` under Actual Result (when media UUID resolved after Step 5b — any Backend/Frontend/DB label):**

```json
{
  "type": "mediaSingle",
  "attrs": { "layout": "align-start" },
  "content": [
    {
      "type": "media",
      "attrs": {
        "type": "file",
        "id": "<media-uuid-from-issue-or-upload>",
        "alt": "<ATTACHMENT_FILENAME>",
        "collection": ""
      }
    }
  ]
}
```

Place **Actual** `mediaSingle` immediately after the Actual `bulletList`. Place **Expected** `mediaSingle` after Expected paragraph(s). If UUID unknown after **5b** exit 0, use unmarked fallback paragraph: `Screenshot: <ATTACHMENT_FILENAME> (attached)` — **5d** should fail embed criterion and trigger **5c** with full ADF including `mediaSingle` when UUID becomes available.

**Body text formatting rules (customfield_10103 — split ADF):**

1. **TL;DR (Option A):** Standalone paragraph immediately after **Description:** header; **entire paragraph** uses `{ "type": "strong" }` on all text nodes; text derived from Summary (strip `[Backend/Frontend/DB]` prefix if redundant in body).
2. **Context:** 1–2 paragraphs after TL;DR; `strong` for feature/screen/component names (max ~3 terms per paragraph), `code` for Jira keys, IDs, formulas, endpoints, errors, field names.
3. **Actual bullets (Option B):** After **Actual Result:** header, use ADF `bulletList`:
   - **Required:** ≥1 bullet (symptom)
   - **Optional:** proof bullet (exact payload/response/SQL with inline `code`)
   - **Optional:** scope bullet (frequency, all cases, compare case)
   - **Forbidden:** empty or invented filler bullets
4. **Charset:** Use `→` in step/bullet text; never `?` as arrow substitute.
5. **Screenshots:** When user provided image(s) and Step **5b** exit **0**, embed `mediaSingle` under **Actual Result** (and under **Expected Result** for second image) — **not** gated on Frontend label only.

All review-file sections map into **`customfield_10103` only** on split-ADF boards — not into standard `description`.

---

#### Step 5d — Verify Key details rendering (split ADF boards — MANDATORY after 5b)

Required when split ADF applies (**Internal Bug**, **External Bug**, or any issue type with formatted Key details on `customfield_10103`).

Immediately after Step **5b** (before any **5c**), call:

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
- When Step **5b** exit **0** (any Backend/Frontend/DB label): screenshot **media** embed **or** fallback line naming `ATTACHMENT_FILENAME` appears under **Actual Result**; when second image uploaded, embed or fallback under **Expected Result**.

**Standard `description` (split ADF):** Must be empty or stub-only (e.g. *Full details in Description formatted.*). If full duplicate markdown/HTML appears, treat as workflow violation — do not claim success until corrected manually or via a one-time stub trim (never add full body to `description` in **5c**).

**On pass:** Skip **5c**. Proceed to post-workflow (CREATED header + URL).

**On failure:** Run **Step 5c** once (patch **`customfield_10103` only**), then **5d** again. If still failing after at most **two** **5c** attempts, warn: `⚠️ Key details (Description formatted / customfield_10103) incomplete — verify in Jira.` Do **not** claim full formatting success.

---

#### Step 5c — Conditional patch (split ADF) or mandatory colored Description (legacy)

**Split ADF (Internal Bug or External Bug with `customfield_10103`):**

Run **only when Step 5d verification fails** (or after second **5d** following first failed **5c**). **Skip entirely** when **5d** passes on first fetch.

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
- **Split ADF (Internal, External with 10103):** Tier 1 colored Key details template → **`customfield_10103` only** at **5a** and conditional **5c** (Description first, Option A TL;DR, body marks, Option B Actual bullets, API `codeBlock`s, screenshot embed when **5b** succeeds). **MUST NOT** write full bug body to standard **`description`**. Do not use the legacy colored template on split-ADF `description`.
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

**After Steps 5a–5b–5d (and conditional 5c when split ADF; legacy 5c) complete:**
1. Return the Jira URL: `https://oppa-support.atlassian.net/browse/<ISSUE_KEY>`
2. If a screenshot was uploaded in Step 5b: confirm "Screenshot attached: `<ATTACHMENT_FILENAME>`" and whether Key details embed succeeded (`customfield_10103`)
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
| Pre-create validation VALID (Step 3.5) | +10 |
| Pre-create validation skipped | -5 |
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
| Environment missing after Step 0 (no user message, no parent field) | **Step 0b AskQuestion** (six envs); **MUST NOT** infer from fix version or Ph2 board; **MUST NOT** write review file until resolved |
| `createJiraIssue` fails — standard Description required | Retry **once** with minimal stub: summary line + `Full details in Description formatted.` — do not add full body |
| `createJiraIssue` fails — empty/rejected `customfield_10103` | Retry once with minimal 10103 stub paragraph; then **5b** → **5d** → conditional **5c** with full ADF |
| `createJiraIssue` MCP fails (other) | Show error to user; do not retry silently; ask user how to proceed |
| Step 3 screenshot handoff fails (copy or verify — destination missing/empty) | **STOP** before Step 3.5 / Step 4; ask user to re-attach image or provide path; **MUST NOT** list screenshot filename in review file or proceed to Jira create until handoff passes |
| Step 3.5 validation NOT VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / INSUFFICIENT EVIDENCE / PROCESS BLOCKED | **Stop**; set `# Bug Review — VALIDATION STOPPED`; patch footer; **clear sidecar**; **MUST NOT** offer Step 4 Agree |
| bug-validator Task fails or times out | **AskQuestion:** Retry validation / **Cancel** only (no Skip) |
| Screenshot upload (Step 5b) script exits with code 1 (auth error) | **Notify user at end of workflow:** `⚠️ Screenshot upload failed: Jira API token authentication error. Check JIRA_EMAIL and JIRA_API_TOKEN in Cursor-Project/.env.` Proceed to **5d** without screenshot |
| Screenshot upload (Step 5b) script exits with code 2 | Warn user to attach manually; include the file path and Jira ticket URL; proceed to **5d** without screenshot |
| `editJiraIssue` (Step 5c legacy or conditional split) fails | Warn user that ADF formatting was not applied; do not skip silently |
| Step 5d verification fails (split ADF) | Run **5c** with **`customfield_10103` only** (max two attempts); re-run **5d**; if still failing, warn — Description formatted panel may be incomplete |
| `customfield_10103` rejected at Step 5c | Log the error; warn: `⚠️ Could not populate the "Description formatted" field (customfield_10103) — please verify the ticket's Key details panel.` |
| `getVisibleJiraProjects` fails (External class) | Retry once; then ask user for allowed external project key and validate against allowlist |
| External class but user skipped project AskQuestion without explicit allowed key/URL | **BLOCK** workflow until **externalProjectKey** is set |
| User selects or names non-allowlisted external project | Refuse; list allowed external keys (currently GB) |
| Parent ticket is not a Phoenix Phase 2 board | Warn user; ask if they want to report on PHN anyway |
| User provides Experiments board | Refuse; redirect to `jira-bug` agent (JIRA.0) |

---

## Agents involved footer

Always end with: `Agents involved: phoenix-bug-reporter` (+ `bug-validator` when Step 3.5 Validate ran)
