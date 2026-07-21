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
- Summary label prefix: `[Backend]` for backend/API bugs, `[Frontend]` for UI-only bugs

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
| **`customfield_10103`** | **Full** Key details ADF at **Step 5a** create — all review-file content mapped per **Key details ADF template** (steps, actual/expected, env line, API block, optional Example `codeBlock`, optional screenshot after 5b via conditional **5c**). **`strong` + `textColor`** on section/API labels (exact hex below). |
| **`description`** | **Not** part of authoring: omit or empty at **5a**. If create validation requires standard Description, retry **once** with **minimal stub** only (see Step 5a). **MUST NOT** write the full bug body to standard `description` on split-ADF boards. |

Inline marks (`strong`, `textColor`, `code`) on the **standard `description`** field corrupt rendering (literal `{color:…}` / `*bold*`). Colored marks belong **only** on **`customfield_10103`**.

**Rules (split ADF boards):**
- Step **5a:** `createJiraIssue` with **full colored Key details ADF** in **`customfield_10103`** (`additional_fields` + ADF). Standard **`description`** omitted or empty; minimal stub retry only on create validation failure. (External projects may require Epic, Fix version, Environment ADF, Tester — see **External Bug (standalone)** and createmeta.)
- Step **5b:** Screenshot upload when file exists (after **5a**).
- Step **5d:** **Mandatory** after **5b** — verify **`renderedFields.customfield_10103`** only. **Pass → skip 5c** and finish.
- Step **5c:** **Conditional** — run **only when 5d fails**; `editJiraIssue` with **`customfield_10103` only** (no `description` key). At most **two** 5c attempts per ticket. **Forbidden:** Jira REST `PUT`/`POST` from Shell for formatting — MCP only. Temp ADF payload files must **not** include `description`.

**Legacy Ph2 boards without `customfield_10103`:** Unchanged — **5a** full markdown `description`; **5c** colored ADF on **`description` only**. Do not set `customfield_10103`.

### Key details — label colors (exact hex, always bold + color)

Every label uses **both** `{ "type": "strong" }` and `{ "type": "textColor", "attrs": { "color": "<hex>" } }` on the **label text node only**. Body paragraphs under a header use **no marks**.

| Key details label (exact text) | Hex |
|-------------------------------|-----|
| **Reproduce Steps:** | `#00B8D9` |
| **Actual Result:** | `#FF5630` |
| **Expected Result:** | `#36B37E` |
| **Endpoint** (label word only in `Endpoint: …` line) | `#5E6C84` |
| **Method** | `#5E6C84` |
| **Status** | `#5E6C84` |
| **Payload** | `#5E6C84` |
| **Response** | `#5E6C84` |

Optional **context paragraph** from the review file **Description:** section: one unmarked paragraph **after Reproduce Steps header and before the `orderedList`**, or folded into **Actual Result** body — do not duplicate full markdown in standard `description`.

### External Bug (standalone — project-specific)

When **bug class = External** and Step 0 stored **`externalProjectKey`** (e.g. **`GB`**):

**Review file:** Record **Bug class** = External; **Issue Type** = Bug; **Board** = `externalProjectKey`; **Parent** = —; include **Epic Link**, **Fix version** when known (ask user or resolve via createmeta before Step 5a).

**Step 0:** Tester/reporter from `.env` (same as Internal). No chapter-subtask assignee for External. A parent key in the user message for **context only** does **not** set Jira `parent` on External creates.

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
    "customfield_10095": { "accountId": "<tester accountId from Step 0>" },
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
- **`customfield_10103`:** full Key details ADF at create — same structure as **Key details ADF template** (screenshot nodes usually added in conditional **5c** after **5b**).
- Resolve required fields via `getJiraIssueTypeMetaWithFields` for **`externalProjectKey`** when create fails; never guess Epic or Fix version — ask the user. Field ids may differ by project (GB uses `customfield_10008`, `customfield_10095`, etc.).
- **`reporter`:** same optional `additional_fields.reporter` rule as PHN; omit if Jira rejects.

**Steps 5b–5d:** Same as Internal split ADF — **5b** screenshot, **5d** verify `customfield_10103`, **5c** patch **`customfield_10103` only** when **5d** fails.

**Hook note:** `block-bugreview-unapproved-jira.ps1` guards **`Internal Bug`** only. External **Bug** still requires Step 4 Agree and `# Bug Review — APPROVED` before create; approval is workflow **MUST**, not hook-enforced for External.

---

## Step-by-Step Workflow

**Entry:** Start at **Step 0** as soon as the user invokes the bug reporter. There is **no** pre-registration validity question. Jira consent is **Step 4 only** (after the review file is on disk). The `beforeMCPExecution` hook `block-bugreview-unapproved-jira.ps1` blocks `createJiraIssue` for Internal Bug unless the review file first line is `# Bug Review — APPROVED`.

---

### Step 0 — Resolve board, sprint, tester, and assignee

#### Tester — ALWAYS from `.env` (Option B, mandatory)

**Regardless of whether a parent ticket is provided or not**, the tester is always the current user (the person running the bug reporter), never taken from the parent ticket's reporter field.

Do this first, before any other Step 0 logic:

1. Run via Shell: read `Cursor-Project/.env`, parse `JIRA_REPORTER_EMAIL` (the reporter/tester's email), `JIRA_EMAIL` (the API token owner's email), and `JIRA_API_TOKEN`
2. Call the Jira REST API to find the reporter user:
   `GET https://api.atlassian.com/ex/jira/ad451d5c-7331-46f8-9a47-f51dc8e6bbde/rest/api/3/user/search?query=<JIRA_REPORTER_EMAIL>` with Basic auth using `JIRA_EMAIL:JIRA_API_TOKEN` (the token owner's credentials are used for auth; we are only searching for the reporter user by their email)
3. If the lookup succeeds: store `accountId` and `displayName` as **tester** — also store as **currentUserAccountId** (used for reporter in Step 5a)
4. If the lookup fails: set tester = empty, currentUserAccountId = empty

---

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
3. **Resolve assignee from chapter subtasks** (do this after Step 1 determines the bug label if not already known from context):
   - Inspect each subtask summary for keywords that match the bug label:
     - Bug label **Frontend** → subtask whose summary contains `Frontend`
     - Bug label **Backend** → subtask whose summary contains `Backend`
     - Bug label **DB** → subtask whose summary contains `DB` or `Database`
   - Call `getJiraIssue` for the matched subtask to get its full details
   - Extract `fields.assignee.displayName` + `fields.assignee.accountId` from that subtask → **assignee**
   - If no matching chapter subtask is found, or the matched subtask has no assignee → fall back to `fields.assignee` from the parent ticket itself
   - If the bug label is not yet known at Step 0 time (because details come in Step 1): store the full `fields.subtasks` list and resolve the assignee at the end of Step 1 using the same matching rule above

**When `bugClass` = Internal and no parent ticket is provided:**

1. Ask the user: "Which Phoenix Phase 2 board should this bug be reported on? (e.g. PHN)"
2. Ask: "Which sprint should this be added to?"
3. **Assignee** = empty (no chapter subtask to resolve from)

**When `bugClass` = External:**

- Complete **External project selection** above before Step 1 (unless skipped per explicit key/URL).
- **Assignee** = empty unless user specifies one for the external project.
- Optional parent key in the message is **context only** — do **not** set Jira `parent` on create.

Proceed to Step 1 when board/project (`board` or **`externalProjectKey`**) and class are resolved.

---

**Deprecated — do not use:** treating “no parent” as Internal-only without asking class; auto-routing External requests to GB without **External project selection**.

### Step 1 — Gather bug details

Check what bug information the user has already provided. Required fields:

| Field | Check |
|-------|-------|
| Summary (short description of problem) | Present? |
| Description (short context paragraph) | Present? |
| Steps to reproduce (numbered steps) | Present? |
| Expected result | Present? |
| Actual result | Present? |
| Environment (Dev/Test/PreProd/Prod) | Present? |
| Endpoint + Method | Present? (skip if UI-only bug) |
| Payload | Present? (skip if UI-only bug) |
| Response / error | Present? (skip if UI-only bug) |
| Status code | Present? (skip if UI-only bug) |
| Example | Present? (skip if no concrete data) |
| Label (Backend / Frontend) | Infer from context or ask |

**If any required fields are missing:**
- Ask up to 4 targeted questions covering all missing fields in one message
- Wait for user's answer before proceeding
- Do not proceed to Step 2 with missing required fields

**If all fields are present:** proceed directly to Step 2.

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
| **Assignee**  | <display name, or — if not set> |
| **Tester**    | <display name (from parent reporter), or — if not set> |
| **Label**     | <Backend / Frontend> |

> Priority rationale: <one sentence explaining priority choice>

---

## Bug Content

**Summary:** [Backend/Frontend] - <Component> - <Short problem>

**Description:**
<paragraph>

**Steps to reproduce:**
1. ...
2. ...

**Actual result:**
<actual>

**Expected result:**
<expected>

**Environment:**
- Environment: <env>

**Technical details:**
- Endpoint: <METHOD /api/path>
- Payload: <payload>
- Response: <response>
- Status: <status code>

**Example:**
<example>

---

*Awaiting your response: **Agree** to submit to Jira, **Disagree** to request changes.*
```

**Screenshot handling — copy to review folder (MANDATORY if user provided an image):**

If the user attached an image in the current chat session, its path is shown in the `image_files` context block. Copy it to the same folder as the review file immediately after writing the review file:

1. Identify the full source path from `image_files` (e.g. `C:\Users\...\assets\..._image.png`)
2. Define the destination: `<review-file-dir>\<review-basename>_screenshot.png`
   - Example: review file = `BugReview_sp-contract_1430.md` → destination = `BugReview_sp-contract_1430_screenshot.png`
3. Copy via Shell:

```powershell
Copy-Item "<source path>" -Destination "<review-dir>\<review-basename>_screenshot.png" -ErrorAction SilentlyContinue
```

4. Note the destination path — Step 5b looks for it automatically by this naming convention.

If no image was provided in chat: skip this sub-step.

Write this file to disk using the file write tool. After writing:
1. Display the review file as a **clickable markdown link** using the full absolute path so the user can open it directly in the IDE:

```
Review file: [BugReview_<slug>_<HHMM>.md](c:\Users\g.gamjashvili\new_cursor\CURSOR-PROJECT\Cursor-Project\reports\Bug Reports\YYYY\<month>\<DD>\BugReview_<slug>_<HHMM>.md)
```

2. Immediately ask the Agree/Disagree question (Step 4) — do NOT display the full file content in chat; the clickable link is sufficient for the user to review it.

---

### Step 4 — Agree / Disagree gate

> **CRITICAL — APPROVAL AskQuestion (Step 4 only):**
> This question may ONLY be asked after the review file has been written to disk (Step 3 complete) and the clickable link has been displayed. Use a standalone **AskQuestion** call for Agree/Disagree — do not batch it with unrelated questions in the same call.

After the review file link is shown, ask using **AskQuestion** with exactly **one question** and exactly **two options**:

```
Is this bug ready to be reported on Jira?
  ● Agree
  ● Disagree
```

**Wait for user response.**

**On Agree:**
- **FIRST** update the review file's first line from `# Bug Review — PENDING APPROVAL` to `# Bug Review — APPROVED` (edit in place, same file, same path)
- This write is required before `createJiraIssue` — the `beforeMCPExecution` hook reads this line to verify consent and will block ticket creation if the file still says `PENDING APPROVAL`
- Then proceed to Step 5

**On Disagree:**
- Ask: "What would you like to update in the bug report? Please describe what's incorrect or missing."
- Wait for the user's answer
- Edit the existing review file at the same path with the corrections — do NOT create a new file, do NOT delete the old one
- Show the updated clickable link again:

```
Updated review file: [BugReview_<slug>_<HHMM>.md](<full path>)
```

- Immediately ask the same Agree/Disagree question again (loop back to the top of Step 4)
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

Call `createJiraIssue` with **full colored Key details ADF** in **`additional_fields.customfield_10103`** (see **Key details ADF template**). Build ADF from **every** review-file section (steps, actual, expected, environment, technical details, example when present). Do NOT condense or summarize.

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
  "assignee_account_id": "<assignee accountId from Step 0, or omit if empty>",
  "additional_fields": {
    "priority": { "name": "<priority name>" },
    "labels": ["<Backend or Frontend>"],
    "reporter": { "accountId": "<currentUserAccountId from Step 0, or omit if empty>" },
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* Key details colored ADF — full template */ ] }
  }
}
```

If create fails because **`customfield_10103`** is empty/rejected: retry once with a minimal 10103 stub (single paragraph: *Details pending — see review file*), then rely on conditional **5c** for the full ADF after **5b**/**5d**.

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
    "reporter": { "accountId": "<currentUserAccountId from Step 0, or omit if empty>" }
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

Check if a screenshot file exists at:

```
<same directory as the review file>\<review-file-basename>_screenshot.png
```

Also check common alternative extensions: `_screenshot.jpg`, `_screenshot.jpeg`, `_screenshot.webp`.

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

#### Key details ADF template (`customfield_10103`)

Used for **Internal Bug** and **External Bug** when split ADF applies. Build at **Step 5a** create and reuse for conditional **Step 5c** patches. Source: approved bug review file.

**ADF structure rules (MUST — prevents GB-1772-style corruption):**

- Each section is a **top-level** node in `content[]`: colored **header paragraph** first, then body (`orderedList` or plain `paragraph` nodes). **Never** start `content` with `orderedList` without the **Reproduce Steps:** header paragraph above it.
- **Close the reproduce `orderedList` before** the **Actual Result:** header. Actual, Expected, screenshot, separator, and API lines are **siblings** after the list — **never** inside the last `listItem`.
- API block lines are separate **paragraph** nodes after the `_________________` paragraph.
- On Step **5d** failure, conditional **5c** patches **`customfield_10103` only** (do not send `description`).

Section order:

1. **Reproduce Steps:** — header (`#00B8D9`, strong + textColor) → optional unmarked context paragraph from review **Description:** → `orderedList` of all steps (unmarked step text).
2. **Actual Result:** — header (`#FF5630`) → one or more unmarked paragraphs with full actual result text.
3. **Screenshot (when Step 5b exit 0):** Prefer `mediaSingle` → `media` with `attrs.type: "file"`, `id: "<media-uuid-if-known>"`, `alt: "<ATTACHMENT_FILENAME>"`. If UUID unknown, unmarked paragraph: `Screenshot: <ATTACHMENT_FILENAME> (attached)`.
4. **Expected Result:** — header (`#36B37E`) → unmarked paragraph(s).
5. **Environment:** — unmarked paragraph(s) from review **Environment:** (e.g. `Environment: Dev2`). No separate colored header required unless team adds one later.
6. Plain paragraph: `_________________`
7. API lines — each a single paragraph; label word with `#5E6C84` strong + textColor, value plain:
   - `Endpoint: <METHOD /path>` — derive METHOD from Technical details (e.g. GET).
   - `Method: <METHOD>`
   - `Status: <code and text>`
   - `Payload: <payload>`
   - `Response: <response>`
8. **Example (when review file has Example):** unmarked paragraph `Example:` then `codeBlock` with `language: "json"` when JSON; plain `codeBlock` otherwise.

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

**Example `mediaSingle` (when media UUID resolved after Step 5b):**

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

- HTML contains color markup for section headers (e.g. `color="#00B8D9"`, `color="#FF5630"`, or `<font color=` equivalents).
- **Reproduce Steps:** header appears **before** the `<ol>` (not missing).
- Reproduce steps present (`<ol>` or numbered content).
- Actual and expected content present **outside** the last list item (no nested actual/expected inside `<li>`).
- API block includes **Endpoint** and **Method** after the separator (not inside the list).
- **Content coverage** vs approved review file (no dropped sections).
- When Step **5b** exit **0**: screenshot **media** embed **or** fallback line naming `ATTACHMENT_FILENAME` appears in rendered Key details.

**Standard `description` (split ADF):** Must be empty or stub-only (e.g. *Full details in Description formatted.*). If full duplicate markdown/HTML appears, treat as workflow violation — do not claim success until corrected manually or via a one-time stub trim (never add full body to `description` in **5c**).

**On pass:** Skip **5c**. Proceed to post-workflow (CREATED header + URL).

**On failure:** Run **Step 5c** once (patch **`customfield_10103` only**), then **5d** again. If still failing after at most **two** **5c** attempts, warn: `⚠️ Key details (Description formatted / customfield_10103) incomplete — verify in Jira.` Do **not** claim full formatting success.

---

#### Step 5c — Conditional patch (split ADF) or mandatory colored Description (legacy)

**Split ADF (Internal Bug or External Bug with `customfield_10103`):**

Run **only when Step 5d verification fails** (or after second **5d** following first failed **5c**). **Skip entirely** when **5d** passes on first fetch.

> **Forbidden:** formatting via Shell Jira REST `PUT`/`POST` — use **`editJiraIssue`** only. **`fields` must contain only `customfield_10103`** — no `description` key.

Call `editJiraIssue` with full Key details ADF from the review file (include screenshot `mediaSingle` or fallback when **5b** succeeded):

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
- **Split ADF (Internal, External with 10103):** Colored Key details template → **`customfield_10103` only** at **5a** and conditional **5c**. **MUST NOT** write full bug body to standard **`description`**. Do not use the legacy colored template on split-ADF `description`.
- **Legacy Ph2 (no `customfield_10103`):** Colored-marks template applies to **`description` only** at **5c**.
- Every section label paragraph contains ONLY the label text (bold + colored) — content follows in a separate node
- Steps to reproduce → `orderedList`
- Environment and Technical details bullets → `bulletList`
- Example → `codeBlock` with `language: "json"` when the example is JSON; use plain `codeBlock` (no language) otherwise
- Omit Technical details and Example nodes entirely when not applicable (UI-only bugs with no API)

**Body text formatting rules (legacy colored `description` only — apply to all content paragraphs):**

1. **Bold summary sentence** — insert a standalone bold paragraph immediately after the `Description:` header and immediately after the `Actual result:` header. One sentence that captures the core problem. Use `{ "type": "strong" }` mark on the entire sentence text node.

2. **Bold key terms** — within body paragraphs, wrap component names, feature names, UI area names, and key nouns in `strong` mark. Use separate text nodes per segment: `{ "type": "text", "text": "Feature Name", "marks": [ { "type": "strong" } ] }`. Do NOT bold everything — only the most important 1–3 terms per paragraph.

3. **Inline code for technical values** — any value the user would copy-paste gets the `code` mark: endpoint paths, field names, token strings, IDs, status codes, payload keys, error codes. Use `{ "type": "code" }` mark on that text node. Examples: `$POD_TYPE$=$GENERATR$`, `id=3345`, `PUT /api/contracts/{id}`, `400 Bad Request`.

4. **Mixed content paragraphs** — a paragraph can contain multiple text nodes with different marks. Split the sentence into segments: plain text nodes for connective words, `strong` nodes for key terms, `code` nodes for technical values. Do NOT mark entire paragraphs — mark individual words or short phrases only.

**After Steps 5a–5b–5d (and conditional 5c when split ADF; legacy 5c) complete:**
1. Return the Jira URL: `https://oppa-support.atlassian.net/browse/<ISSUE_KEY>`
2. If a screenshot was uploaded in Step 5b: confirm "Screenshot attached: `<ATTACHMENT_FILENAME>`" and whether Key details embed succeeded (`customfield_10103`)
3. Update the review file header from `PENDING APPROVAL` to `CREATED — <ISSUE_KEY>` (edit in place)
4. Display the confidence block and agents footer

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
| `createJiraIssue` fails — standard Description required | Retry **once** with minimal stub: summary line + `Full details in Description formatted.` — do not add full body |
| `createJiraIssue` fails — empty/rejected `customfield_10103` | Retry once with minimal 10103 stub paragraph; then **5b** → **5d** → conditional **5c** with full ADF |
| `createJiraIssue` MCP fails (other) | Show error to user; do not retry silently; ask user how to proceed |
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

Always end with: `Agents involved: phoenix-bug-reporter`
