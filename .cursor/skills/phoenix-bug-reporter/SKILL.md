---
name: phoenix-bug-reporter
description: Full workflow for drafting and submitting Phoenix Phase 2 Internal Bug and External (standalone Bug) tickets. User selects the Jira project at Step 0 from live visible projects (denylist in phoenix_bug_reporter.mdc). Enforces approval gate via bug review file. Rule PHOENIX-BUG.0.
---

# Phoenix Bug Reporter — SKILL

Creates **Internal Bug** sub-tasks on Phoenix Phase 2 Jira boards and **External** standalone **Bug** tickets on any visible Jira project except the denylist in **`.cursor/rules/integrations/phoenix_bug_reporter.mdc`**. Always produces a review file for user approval before touching Jira.

**Agent file:** `.cursor/agents/phoenix-bug-reporter.md`
**Rule:** `.cursor/rules/integrations/phoenix_bug_reporter.mdc` (PHOENIX-BUG.0)

---

## Jira transport (official ACLI — no MCP, no hand-written Jira REST)

Jira MCP is forbidden (Rule ATLASSIAN.0). Do **not** call `createJiraIssue`, `editJiraIssue`, `getJiraIssue`, `getVisibleJiraProjects`, `getJiraProjectIssueTypesMetadata`, or `getJiraIssueTypeMetaWithFields`.

Do **not** call Jira REST yourself for this workflow. That includes `GET /rest/api/3/user/search` and `GET /rest/api/3/project/{key}` (versions, createmeta, issue types). The `.env` API token is a different login from `acli`. On this site that token returned SSL errors and **401** against `api.atlassian.com`, an empty user search on the site, and **“No project could be found”** for PHN. `acli` (OAuth) could see the same project, versions, and user.

| Need | Official command |
|---|---|
| Logged-in email | `acli jira auth status` |
| Current user `accountId` | `acli jira workitem search --jql "reporter = currentUser() ORDER BY created DESC" --limit 1 --json --fields "reporter"` |
| Read a work item | `acli jira workitem view <KEY> --json --fields "<list in atlassian_zero_cost.mdc>"` |
| List projects | `acli jira project list --limit 50 --json` |
| Issue types and fix versions | `acli jira project view --key <KEY> --json` (`issueTypes`, `versions`) |
| Create | `acli jira workitem create --from-json <payload.json> --json` |
| Edit | `acli jira workitem edit --key <KEY> --from-json <payload.json> --yes --json` |

Create and edit run only after `# Bug Review — APPROVED` and `.active-bugreview`. The shell hook blocks `acli jira workitem create` for type **Bug** and **Internal Bug** until that gate passes.

`acli` has **no createmeta** and does **not** return `renderedFields`. Step **0c** reads issue types and versions from `acli jira project view`. Step **5d** checks the ADF JSON from `acli jira workitem view`. If create rejects `customfield_10103`, use the retry table later in this skill.

The create file must match `acli jira workitem create --generate-json`. Do not send MCP keys (`cloudId`, `issueTypeName`, `contentFormat`, `additional_fields`, `assignee_account_id`, `parent`). Do not pass `--editor` or `--web`.

**Create JSON (fill this shape):**

```json
{
  "projectKey": "PHN",
  "type": "Bug",
  "summary": "<summary>",
  "labels": ["Backend"],
  "reporter": "<accountId or email, omit when unknown>",
  "assignee": "<accountId or email, omit when Leave unset>",
  "parentIssueId": "<parent key for Internal Bug only, otherwise omit>",
  "additionalAttributes": {
    "priority": { "name": "Highest" },
    "fixVersions": [{ "name": "<version name from Step 0c>" }],
    "customfield_10095": { "accountId": "<testerAccountId, omit the key when Leave unset>" },
    "customfield_10103": { "type": "doc", "version": 1, "content": [] }
  }
}
```

On split-ADF boards omit `description`. `reporter` and `assignee` are top-level strings (email or account id), matching `--generate-json`. `fixVersions`, priority, Tester, and Description formatted go in `additionalAttributes`. This shape created PHN-4595 (`type` Bug, `customfield_10103` populated, `description` null, Fix version Release 4).

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
| **External** | Bug (standalone; name must exist on `acli jira project view` `issueTypes`) | No | Any visible project except denylist + Experiments (Step 0) |

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
- **External class Bug on PHN** — same split ADF as Internal Bug. PHN standalone Bug uses **Description formatted** (`customfield_10103`). Send it. Do not wait for createmeta.
- **External class Bug on any other project** — send `customfield_10103` only when `acli jira workitem view` of a recent same-type issue shows that field **non-null**, or the user confirmed the field. Otherwise use legacy `description`.

**Detection:** PHN uses split ADF on **`customfield_10103`**. There is no createmeta call. If `acli jira workitem create` rejects that field, retry **once** as legacy `description` and do not send `customfield_10103`.

**Authoritative bug body (split ADF boards):**

| Field | Behavior |
|-------|----------|
| **`customfield_10103`** | **Full** Tier 1 Key details ADF at **Step 5a** create — all review-file content mapped per **Key details ADF template** (Description + TL;DR first, body marks, steps, Actual `bulletList`, expected, env line, API `codeBlock` evidence; screenshot embed added after **5b** via mandatory **5c** when an image was provided — any Backend/Frontend/DB label). **`strong` + `textColor`** on section/API labels (exact hex below). |
| **`description`** | **Not** part of authoring: omit or empty at **5a**. If create validation requires standard Description, retry **once** with **minimal stub** only (see Step 5a). **MUST NOT** write the full bug body to standard `description` on split-ADF boards. |

Inline marks (`strong`, `textColor`, `code`) on the **standard `description`** field corrupt rendering (literal `{color:…}` / `*bold*`). Colored marks belong **only** on **`customfield_10103`**.

**Rules (split ADF boards):**
- Step **5a:** `acli jira workitem create --from-json` with **full colored Key details ADF** in **`additionalAttributes.customfield_10103`**. Standard **`description`** omitted on split-ADF boards; one minimal stub retry only when create says Description is required. Fix version is resolved in **Step 0c** before the review file, not after a failed create.
- Step **5b:** Screenshot upload when file exists (after **5a**).
- Step **5c:** **Mandatory once when Step 5b uploaded at least one image** (exit **0**) — the create call cannot embed an image that did not exist yet, so the media node is always added here. Otherwise **conditional**: run only when **5d** fails. `acli jira workitem edit --key <KEY> --from-json <payload.json> --yes --json` with **`customfield_10103` only** (no `description` key). At most **two** 5c attempts per ticket. **Forbidden:** Jira MCP and hand-written Jira REST `PUT`/`POST`. Temp ADF payload files must **not** include `description`.
- Step **5d:** **Mandatory** — `acli jira workitem view` and verify the **`customfield_10103` ADF JSON** (node order, colours, lists, code blocks). `acli` does not return rendered HTML. Runs after the mandatory **5c** when an image was uploaded, otherwise directly after **5b**.

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

**Review file:** Record **Bug class** = External; **Issue Type** = `Bug` when Step **0c** shows that name with `subtask` false; **Board** = `externalProjectKey`; **Parent** = —; **Fix version** = the Step **0c** choice when that project and type require one.

**Step 0:** **Reporter** from `acli jira auth status` plus `reporter = currentUser()` (see below). **Tester** is that same person for External. No chapter-subtask assignee for External. A parent key in the user message for **context only** does **not** set `parentIssueId` on External creates. A pasted key **may** supply **`externalProjectKey`** via Step 0 source 2 (`fields.project.key` or key prefix).

**Step 5a — issue type (External, already checked in Step 0c):**

1. Use issue type name **Bug** when the user asked for a regular bug and `issueTypes` contains `Bug` with `subtask` false. If that name is absent, **AskQuestion** with the live `issueTypes` names. Do not guess an id.
2. **PHN** standalone **Bug** and **Internal Bug** use split ADF. Put the ADF in `additionalAttributes.customfield_10103` and omit `description`.
3. If create rejects `customfield_10103`, retry **once** as legacy: full review-file text in top-level `description`, and delete `customfield_10103`.
4. **Fix version** is already in the approved review when Step **0c** marked it required. Send `additionalAttributes.fixVersions: [{ "name": "<that name>" }]`. Do not discover it by failing create.
5. If create still rejects another required field (Epic or similar), show the error, **AskQuestion**, update the review file, and return to Step 4. Never invent Epic, field ids, or `GB-1501`.
6. **Tester** on PHN is `additionalAttributes.customfield_10095.accountId` when `testerAccountId` is set. Omit the key when the user confirmed **Leave unset**. Do not send `customfield_10008`.

**Steps 5b–5d:** Split ADF — **5b** screenshot, **5d** re-read the work item with `acli jira workitem view` and check `customfield_10103` ADF (official `acli` does not return rendered HTML), **5c** `acli jira workitem edit` that field only when **5d** fails. **Legacy:** **5a** description text → **5b** → **5c** description update.

**Hook note:** `block-acli-rovo.ps1` guards `acli jira workitem create` for **Internal Bug** and **Bug**. Requires `# Bug Review — APPROVED` and `.active-bugreview` sidecar (Step 4 On Agree). If the user selected a non-`Bug` type, the hook may **not** intercept create — still require Step 4 Agree + sidecar in the workflow.

---

## Step-by-Step Workflow

**Entry:** Start at **Step 0** as soon as the user invokes the bug reporter. There is **no** pre-registration validity question. Jira consent is **Step 4 only** (immediately after Step 3). `block-acli-rovo.ps1` blocks `acli jira workitem create` for **Internal Bug** and **Bug** unless the review file first line is `# Bug Review — APPROVED` and `.active-bugreview` sidecar points to that file. Jira MCP `createJiraIssue` is denied outright.

---

### Step 0 — Resolve board, sprint, reporter, tester, and assignee

#### Reporter — ALWAYS the `acli` account (mandatory)

**Regardless of bug class or parent ticket**, the Jira **reporter** is the person authenticated in `acli` — never the parent ticket's `fields.reporter`, and never a user looked up with the `.env` API token.

Do this first, before any other Step 0 logic:

1. Run `acli jira auth status`. It must say **Authenticated** and print an **Email**. Store that email as **`reporterEmail`**. If it is not authenticated, stop and ask the user to run `acli jira auth login`. Do not read `JIRA_API_TOKEN`. Do not call `user/search`.
2. Run `acli jira workitem search --jql "reporter = currentUser() ORDER BY created DESC" --limit 1 --json --fields "reporter"`. Store `fields.reporter.accountId` as **`currentUserAccountId`** and `displayName` as **`reporterDisplayName`**.
3. If that search returns no issue, retry once with `--jql "assignee = currentUser() ORDER BY updated DESC" --fields "assignee"` and read `fields.assignee` the same way.
4. If an `accountId` was found, Step 5a sends top-level `"reporter": "<accountId>"`. If create returns a reporter field error, drop `reporter` and continue — the work item is still opened by the `acli` account.
5. If no `accountId` was found, set `currentUserAccountId` empty and `reporterDisplayName` to the auth-status email. Omit `reporter` from the JSON. Do **not** treat that as a failed REST lookup that forces Assignee and Tester to **Leave unset**. Step **1b** still asks only when the field is empty **and** this `acli` lookup did not return an `accountId`.

---

#### Tester — by bug class

| Bug class | Parent | Tester source |
|-----------|--------|---------------|
| **Internal** | Yes | **QA chapter subtask** assignee on parent (see below) |
| **Internal** | No | **Current `acli` user** — same account as reporter |
| **External** | — | **Current `acli` user** — same account as reporter |

**External / Internal without parent:** After the `acli` lookup above, set **tester** = `reporterDisplayName` and **testerAccountId** = `currentUserAccountId` (same person as reporter). A failed `.env` user search is not a reason to leave Tester empty when this lookup returned an `accountId`.

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

**Do not** default to GB or any project. Store **`externalProjectKey`** and **`externalProjectSource`**. Filter using the **denylist** in **`.cursor/rules/integrations/phoenix_bug_reporter.mdc`**; Experiments → `jira-bug`.

**Map to a project key:**
- Token equal to a Jira project key (e.g. `GB`, `PDT`, `PHN`)
- Board/project URL: `/jira/software/c/projects/{KEY}/`, `/jira/software/projects/{KEY}/`, `/browse/{KEY}-{n}`
- Fetched issue: `fields.project.key`
- Issue key `ABC-123` when no fetch: prefix `ABC` only if that key is in the current `acli jira project list --json` result (do not invent)

**Resolution order (strict) — stop at the first non-excluded unique key; if several candidates in the same source, prefer the most recently discussed (last in the message / newest chat turn):**

1. **Current user message** — explicit key or URL. Store `externalProjectSource` = `user message`.
2. **This chat** — a ticket, board/project URL, or issue fetched earlier **that this external bug is being reported against**. Newest first. Store `externalProjectSource` e.g. `this chat (PHN-4050 project.key)` or `this chat (board URL)`.
3. **Otherwise AskQuestion** — build options from live Jira (never a hardcoded allowlist).

**AskQuestion construction:**
1. Run **`acli jira project list --limit 50 --json`**. Use `--paginate` only when the chosen project is not on that page.
2. Drop keys on the **denylist** and **Experiments** (if present).
3. Options: every remaining project, label `"{key} — {name}"`, value = project key. **PHN**, **PDT**, and **GB** must appear when they are in the visible list. If the UI cannot fit all options, ask in sequential batches — do not drop keys.
4. **Prompt:** *Which Jira project should this external bug be created on?*
5. Store the chosen key as **`externalProjectKey`**. Step 5a sends that key as `projectKey`. Board id is not required.

If source 1 or 2 resolves to a **denylist** key → refuse (do not create). If **Experiments** → refuse; redirect to **`jira-bug`**. Then continue to the next source or AskQuestion only when no valid key remains.

**If `acli jira project list` fails:** retry once; then ask the user to type a project key; refuse denylist/Experiments; do not invent keys.

**Never** auto-select GB when it is the only remaining option — still AskQuestion unless sources 1–2 already set a key.

---

**When `bugClass` = Internal and a parent Jira ticket key is provided:**

1. Run `acli jira workitem view <PARENT> --json` with the field list in **`atlassian_zero_cost.mdc`** to get the parent ticket
2. Extract from the parent ticket:
   - `project.key` → board for the new bug
   - `fields.sprint` or `fields.customfield_10020` → sprint name/id; check both if needed
   - `fields.subtasks` → list of all chapter subtasks (each entry contains `key` and `fields.summary`)
3. **Resolve assignee from dev chapter subtasks** (after Step 1 determines the bug label if not already known):
   - Inspect each subtask summary for keywords that match the bug label:
     - Bug label **Frontend** → subtask whose summary contains `Frontend`
     - Bug label **Backend** → subtask whose summary contains `Backend`
     - Bug label **DB** → subtask whose summary contains `DB` or `Database`
   - Run `acli jira workitem view <SUBTASK> --json` with the same field list
   - Extract `fields.assignee.displayName` + `fields.assignee.accountId` from that subtask → **assignee**
   - If no matching chapter subtask is found, or the matched subtask has no assignee → fall back to `fields.assignee` from the parent ticket itself
   - If the bug label is not yet known at Step 0 time: store the full `fields.subtasks` list and resolve assignee at the end of Step 1 using the same matching rule
4. **Resolve tester from QA chapter subtask** (can run at Step 0 — does not depend on bug label):
   - Find a subtask whose `fields.summary` contains **`QA`** or **`Test`** (case-insensitive)
   - If multiple match, prefer a summary containing **`QA`** over **`Test`** only
   - Run `acli jira workitem view <SUBTASK> --json` with the same field list
   - Extract `fields.assignee.displayName` + `fields.assignee.accountId` → **tester** / **testerAccountId**
   - If no matching QA/Test subtask exists, or the matched subtask has no assignee → **tester** = empty (do not fall back to parent reporter or `.env`); **Step 1b** must AskQuestion unless user already named tester in chat

**When `bugClass` = Internal and no parent ticket is provided:**

1. Ask the user: "Which Phoenix Phase 2 board should this bug be reported on? (e.g. PHN)"
2. Ask: "Which sprint should this be added to?"
3. **Assignee** = empty (no dev chapter subtask to resolve from)
4. **Tester** = current `acli` user (`testerAccountId` = `currentUserAccountId`, or empty only when Step 0 found no `accountId`)

**When `bugClass` = External:**

- Complete **External project selection** above before Step 1 (skip AskQuestion only when source 1 or 2 already set a non-excluded `externalProjectKey`).
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

### Step 0c — Issue type and Fix version (MANDATORY before the review file)

Run after the project key is known (`board` or **`externalProjectKey`**) and after Step **0b**. This replaces createmeta and replaces `GET /rest/api/3/project/{key}/versions`.

1. Run `acli jira project view --key <KEY> --json`.
2. **Issue type.** Read `issueTypes`. Internal requires a type named **Internal Bug** with `subtask` true. A regular / external bug requires **Bug** with `subtask` false. If the needed name is missing, **AskQuestion** using the live `name` values. Do not guess an id. Do not call createmeta.
3. **Is Fix version required?** Run both searches (limit 5, `--json`, fields `key,fixVersions`):
   - `project = <KEY> AND issuetype = "<type name>" AND fixVersion is not EMPTY ORDER BY created DESC`
   - `project = <KEY> AND issuetype = "<type name>" AND fixVersion is EMPTY ORDER BY created DESC`
   Fix version is **required** when the first search returns issues and the second returns none. PHN **Bug** and PHN **Internal Bug** are required even when the empty search cannot be read: create returns `Fix versions: Fix versions is required.`
4. When required, **AskQuestion** before Step 3. Options are version `name` values from `project view` where `archived` is not true and `released` is not true. If fewer than two names match, add the unarchived released names. Prompt: *Which Fix version should this bug use?* Store the chosen **name** (not a guessed id).
5. When not required, leave Fix version unset unless the user already named one. If a later create says a field is required, **AskQuestion**, write the value into the review file, return to Step 4, and create again. Never call the `.env` token against `/rest/api/3/project/{key}`.

**Gate:** Do **not** write the review file while Fix version is required and still empty. The review **Fix version** row is the chosen name, or `—` only when Step 0c showed the field is not required.

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
- **Internal no parent / External:** tester = the `acli` current user (`testerAccountId` = `currentUserAccountId`); assignee empty unless the user named an assignee in chat.

**Escalation — AskQuestion when still empty and user did not name them in the current message:**

| Field | Ask when |
|-------|----------|
| **Assignee** | `assigneeAccountId` empty after auto-resolution **and** user did not name assignee |
| **Tester** | `testerAccountId` empty after auto-resolution **and** user did not name tester |

**AskQuestion design** (one or two standalone questions; max two turns):

- **Assignee prompt:** *Who should be Assignee on this bug?*
- **Tester prompt:** *Who should be Tester on this bug?*

**Options per question:** build from account ids already returned by `acli` (parent assignee, chapter subtask assignees, the `currentUser()` reporter) **plus** **Leave unset**. Do not call `user/search`. If the user types a display name and no `accountId` is known, ask them for the email or account id, or let them pick **Leave unset**. `acli` accepts that email or account id in the top-level `assignee` string.

| User choice | Store | Review file | Step 5a |
|-------------|-------|-------------|---------|
| Named user | `assigneeAccountId` / `testerAccountId` + display name from `acli` | `<display name> (<accountId or source>)` | Top-level `assignee` string and/or `additionalAttributes.customfield_10095` |
| **Leave unset** | empty accountId; flag `assigneeLeaveUnset` / `testerLeaveUnset` | `— (Leave unset — user confirmed)` | **Omit** that Jira field |

**Gate:** Do **not** write Step 3 review file while Assignee or Tester show bare `—` without Step 1b AskQuestion (or user naming them in chat). After Step 1b, bare `—` is **forbidden** — only `— (Leave unset — user confirmed)` or a resolved display name.

**Step 5a payload (MUST):** When `assigneeAccountId` is set, set top-level `"assignee"` to that account id (or email). When `testerAccountId` is set on PHN, set `additionalAttributes.customfield_10095` to `{ "accountId": "<testerAccountId>" }`. When **Leave unset** was confirmed, omit that key. Do not send `assignee_account_id` or `additional_fields`.

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
| **Fix version** | <Step 0c name, or — when Step 0c showed it is not required> |
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
Review file: [BugReview_<slug>_<HHMM>.md](<workspace>/Cursor-Project/reports/Bug Reports/YYYY/<month>/<DD>/BugReview_<slug>_<HHMM>.md)
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
- **Fix version** is the Step **0c** name when that step marked it required. Bare `—` on a required Fix version is **forbidden**.
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
- This write is required before `acli jira workitem create` — the hook reads the APPROVED header and the sidecar path; it blocks create while the header is PENDING or the sidecar is missing when several APPROVED reviews exist
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
- If user changes **Environment** on Disagree → re-run **Step 0b**. If they change **Fix version** → re-run **Step 0c** for that field only.
- If user changes **Assignee** or **Tester** on Disagree → re-run **Step 1b** for changed fields only
- Return to **Step 4** Agree/Disagree (same AskQuestion)
- This loop repeats until the user selects Agree or explicitly cancels

**On explicit cancel / "don't report" / "abort":**
- Stop the workflow. Do not create a Jira ticket. Inform the user the review file remains saved at the shown path.

---

### Step 5 — Create Jira ticket (only after APPROVE)

This is a **mandatory sequence** executed in this exact order. Do NOT skip parts or reorder.

**Split ADF (PHN Internal Bug, PHN standalone Bug, or any project where a recent same-type issue has non-null `customfield_10103`):**
- **Screenshot uploaded at 5b:** **5a** → **5b** → **5c** (mandatory — inserts the media node) → **5d** (verify + report) → at most one more **5c** if **5d** fails.
- **No screenshot:** **5a** → **5b** → **5d** (verify) → **5c** only if **5d** fails → optional second **5d**.

**Legacy (no Description formatted field):** **5a** → **5b** → **5c** (colored `description`) — no **5d**.

---

#### Step 5a — Create the ticket

Run `acli jira workitem create --from-json <payload.json> --json`. The file uses the shape at the top of this skill. Issue type and Fix version were already resolved in Step **0c**. Do not call createmeta first.

**Split ADF** — full Tier 1 Key details ADF in **`additionalAttributes.customfield_10103`**. Build it from **every** review-file section. Do not condense. Omit top-level `description`.

If create says standard Description is required, retry **once** with this stub only in `description`, and keep the full ADF in `customfield_10103`:

```
<Summary line from review file>

Full details in Description formatted.
```

Do **not** put the full bug body in `description`.

```json
{
  "projectKey": "<board or externalProjectKey>",
  "type": "Bug",
  "summary": "<summary from review file>",
  "labels": ["<Backend or Frontend or DB>"],
  "reporter": "<currentUserAccountId, omit the key when empty>",
  "assignee": "<assigneeAccountId, omit the key when Leave unset>",
  "parentIssueId": "<parent key for Internal Bug only, otherwise omit>",
  "additionalAttributes": {
    "priority": { "name": "<priority name>" },
    "fixVersions": [{ "name": "<Step 0c version name, omit the key when not required>" }],
    "customfield_10095": { "accountId": "<testerAccountId, omit the key when Leave unset>" },
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* Key details colored ADF */ ] }
  }
}
```

`type` is **Internal Bug** when `bugClass` is Internal, and **Bug** when the user asked for a regular bug and Step 0c confirmed that name. PHN Tester stays `customfield_10095` for both classes. Omit `customfield_10095` and `assignee` when the user confirmed **Leave unset**.

If create returns `Fix versions: Fix versions is required.` the Step 0c gate was skipped. Stop, run Step 0c, update the review file, return to Step 4. Do not guess a version. Do not call REST `/project/{key}/versions`.

If create rejects `reporter`, delete that key and create again once. The work item still belongs to the `acli` account.

If create rejects `customfield_10103`, retry **once** as legacy (below) without that key.

**Legacy (project does not use `customfield_10103`):** put the full review text in top-level `description` as ADF (`type: doc`). Do not send `customfield_10103`. Same top-level `reporter`, `assignee`, `labels`, `parentIssueId`, and `additionalAttributes.priority` / `fixVersions` / `customfield_10095` rules as above.

Note the returned `key`. Proceed immediately to Step 5b.

---

#### Step 5b — Upload screenshot (after create, before verification)

> **MANDATORY SEQUENCE — DO NOT SKIP OR COMBINE:**
> Execute each part as a separate operation: **5a → 5b → 5c → 5d** when 5b uploaded an image, otherwise **5a → 5b → 5d** (plus conditional **5c** when **5d** fails).
> - Do NOT skip Step 5b even if you believe no screenshot exists — always check the file path first.
> - Do NOT use Jira MCP or a hand-written Jira REST PUT. Step **5c** is `acli jira workitem edit --from-json`.

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
$uploadOut = & "<workspace>/.cursor/commands/attach-screenshot-to-jira.ps1" `
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

Run:

```bash
acli jira workitem view <KEY> --json --fields "summary,issuetype,priority,labels,fixVersions,assignee,reporter,description,customfield_10103,customfield_10095"
```

Do not pass `expand=renderedFields`. Do not GET the issue with the `.env` token to render HTML.

**Pass criteria for `fields.customfield_10103` (ADF):**

- The value is `{ "type": "doc", "version": 1, "content": [ ... ] }`.
- The first header text is `Description:` with marks `strong` and `textColor` `#403294`, and it appears before `Reproduce Steps:`.
- The TL;DR paragraph under Description has `strong` on its text.
- `Reproduce Steps:` (`textColor` `#00B8D9`) is followed by an `orderedList`, and that list closes before `Actual Result:`.
- `Actual Result:` (`#FF5630`) is followed by a `bulletList`, not a single paragraph that holds every actual-result sentence.
- Actual and expected nodes are siblings after the reproduce list, not nested in the last `listItem`.
- `Expected Result:` uses `#36B37E`.
- A later paragraph contains the label `Endpoint` with `#5E6C84`, then `Method` and `Status`.
- When the review file had Payload, Response, or Example, each of those has a `codeBlock` node.
- The same proof strings from the review file are present (endpoint, status, log lines, ids).
- **Screenshot criterion — only when Step 5b uploaded an image** (exit **0**). The ADF must contain a `media` node whose `attrs.id` equals `ATTACHMENT_MEDIA_UUID`. A text line that only names the filename is **not** a pass.
- **When no image was provided, the screenshot criterion does not apply.**

**Standard `description` (split ADF):** `null` or stub-only (`Full details in Description formatted.`). A full duplicate body is a failure. Also confirm `issuetype.name`, `fixVersions` name, and that Leave-unset assignee / `customfield_10095` are null.

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

> **Forbidden:** Jira MCP and hand-written Jira REST `PUT`/`POST`. Step **5c** is `acli jira workitem edit --key <KEY> --from-json <file> --yes --json`. The edit template from `acli jira workitem edit --generate-json` has no `additionalAttributes`. For a split-ADF repair, send `issues` plus the same `additionalAttributes.customfield_10103` object used at create. Do not send a `description` key on that repair. If `acli` rejects `additionalAttributes` on edit, stop and report the error. Do not PUT the issue with the `.env` token.

```json
{
  "issues": ["<key from Step 5a>"],
  "additionalAttributes": {
    "customfield_10103": { "type": "doc", "version": 1, "content": [ /* same Tier 1 ADF, plus mediaSingle when 5b uploaded an image */ ] }
  }
}
```

At most **two** **5c** attempts per ticket. After each **5c**, re-run **5d**.

**Legacy (no `customfield_10103`) — MANDATORY after 5b:**

Edit with top-level `description` set to the colored ADF doc. `acli jira workitem edit --generate-json` already uses that key. Never skip the legacy edit.

```json
{
  "issues": ["<key from Step 5a>"],
  "description": { "type": "doc", "version": 1, "content": [ /* legacy colored template below */ ] }
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
| `acli jira workitem view` fails | Retry once; if still fails, ask user to provide board/sprint/assignee/tester manually. Do not call Jira MCP |
| Internal + parent: no QA/Test subtask or QA subtask unassigned | **Step 1b AskQuestion** for Tester (+ **Leave unset** option); **MUST NOT** proceed to Step 3 with bare `—` without ask |
| Assignee unresolved after chapter subtask + parent fallback | **Step 1b AskQuestion** for Assignee (+ **Leave unset**); **MUST NOT** create Jira without user pick or confirmed Leave unset |
| Environment missing after Step 0 (sources 1–3 empty: no user message, no parent text, no same-bug chat env) | **Step 0b AskQuestion** (six envs) **only after sources 1–3 are empty**; **MUST NOT** infer from fix version or Ph2 board; **MUST NOT** write review file until resolved |
| `acli jira auth status` is not authenticated | Stop. Ask the user to run `acli jira auth login`. Do not fall back to `.env` user search |
| `.env` / `user/search` / `GET /project/{key}` | **Forbidden.** Those calls do not see what `acli` sees. Use auth status, `currentUser()` search, and `acli jira project view` |
| `acli jira workitem create` fails — standard Description required | Retry **once** with the one-line stub in `description`. Keep the full body in `customfield_10103` |
| `acli jira workitem create` fails — `Fix versions is required` | Step 0c was skipped. AskQuestion from `project view` versions, update the review, return to Step 4. Do not guess |
| `acli jira workitem create` fails — rejected `customfield_10103` | Retry **once** as legacy `description` without that key |
| `acli jira workitem create` fails — reporter field | Drop `reporter` and create once more. The `acli` account still owns the issue |
| `acli jira workitem create` fails (other) | Show the error; do not retry silently; do not call Jira MCP or Jira REST |
| Step 3 screenshot handoff fails (copy or verify — destination missing/empty) | **STOP** before Step 4; ask user to re-attach image or provide path; **MUST NOT** list screenshot filename in review file or proceed to Jira create until handoff passes |
| Screenshot upload (Step 5b) script exits with code 1 (auth error) | **Notify user at end of workflow:** `⚠️ Screenshot upload failed: Jira API token authentication error. Check JIRA_EMAIL and JIRA_API_TOKEN in Cursor-Project/.env.` Proceed to **5d** without screenshot |
| Screenshot upload (Step 5b) script exits with code 2 | Warn user to attach manually; include the file path and Jira ticket URL; proceed to **5d** without screenshot |
| `acli jira workitem edit` (Step 5c) fails | Warn that formatting was not applied. Do not PUT the issue with the `.env` token |
| Step 5d verification fails (split ADF) | Run **5c** with **`customfield_10103` only** (max two attempts); re-run **5d**; if still failing, warn — Description formatted panel may be incomplete |
| Step 5b exit 0 but no `ATTACHMENT_MEDIA_UUID` printed | Build **5c** with the fallback `Screenshot: <ATTACHMENT_FILENAME> (attached)` paragraph instead of `mediaSingle`; report `⚠️ Screenshot attached to the ticket but not embedded under Actual Result.` Do **not** retry the upload and do **not** block the ticket |
| No screenshot provided by the user | Normal path — no **5c** on screenshot grounds, no screenshot criterion at **5d**, no warning, nothing about screenshots in the final response. Screenshots are **optional** and never block bug creation |
| `customfield_10103` rejected at Step 5c | Log the error; warn: `⚠️ Could not populate the "Description formatted" field (customfield_10103) — please verify the ticket's Key details panel.` |
| `acli jira project list` fails (External class) | Retry once; then ask user to type a project key; refuse denylist (rule file) and Experiments |
| External class but `externalProjectKey` unset after sources 1–2 | **AskQuestion** from visible projects minus denylist; **BLOCK** create until set |
| User selects or names a denylist project | Refuse; cite **External excluded project keys** in `phoenix_bug_reporter.mdc` |
| `acli jira project view` has no exact `Bug` | **AskQuestion** with the live `issueTypes` names; never guess an id |
| Create says another field is required (Epic or similar) and Step 0c did not already collect it | **AskQuestion**; update the review; return to Step 4; never guess the value |
| Project does not use Description formatted | Legacy ADF in top-level `description` at 5a; **5c** edits `description` only; do not send `customfield_10103` |
| Parent ticket is not a Phoenix Phase 2 board | Warn user; ask if they want to report on PHN anyway |
| User provides Experiments board | Refuse; redirect to `jira-bug` agent (JIRA.0) |

---

## Agents involved footer

Always end with: `Agents involved: phoenix-bug-reporter`
