---
name: jira-evidence
description: Jira ticket read completeness — custom fields, linked issues, attachments, linked Confluence. Load before substantive Jira fetch, triage, analysis, HandsOff Step 1, cross-dep, or bug validation when Jira is the source. Rule 42 REST fallback when MCP fails.
---

# Jira Evidence Skill

**When to load (Rule 0.0):** Any substantive work that **reads or analyzes a Jira issue** — triage, summary, HandsOff, cross-dependency, test cases, bug validation ticket fetch, attachment analysis.

**AlwaysApply pointer:** `.cursor/rules/main/evidence_only_project_answers.mdc` (core evidence gate only). **This SKILL** holds Jira-specific procedure.

---

## Jira ticket evidence completeness (MANDATORY)

When the user asks to retrieve information from a Jira ticket, do **not** rely only on the `description` field.

**Jira MCP unavailable:** Use **REST read fallback** in **`.cursor/rules/integrations/jira_rest_fallback.mdc`**. Disclose **`Jira source: REST fallback …`** in the reply.

Always:

1. Fetch the **full issue payload**; verify whether `description` is present, empty, or `null`.
2. Check **linked issues** and **attachments metadata** regardless of description.
3. If `description` is empty or insufficient, inspect: custom fields (rich-text ADF), comments/history, environment, labels, components.
4. Return consolidated answer; note where details were found (custom field, comments, linked issue, attachment).

Do **not** conclude "no information" only because `description` is empty.

---

## Jira ticket analysis — linked Confluence (Rule 44)

When the user requests **analysis / triage / summary / retrieval** of a Jira issue, and the payload contains **Confluence URLs** (`*.atlassian.net/wiki/...`):

1. **Read every linked in-scope Confluence page** (Rule 39 narrow scope — linked URLs only; no broad CQL except Rule 32 `bug-validator`).
2. Confluence MCP first; after 2–3 retries use **Rule 43** REST (**`get-confluence-page-rest.ps1`**). Set **`CONFLUENCE_WIKI_BASE`** when wiki host differs from default.
3. Merge findings into the **same** response; cite page title + page ID.
4. Jira-only analysis when readable linked Confluence was skipped → **BLOCK violation** (evidence gate).

---

## Jira custom field mapping (MANDATORY)

When fetching via MCP `getJiraIssue`:

1. **`expand: "names"`** — fieldId → human label map.
2. Request content-bearing custom fields (or omit `fields` for breadth):

| Custom Field ID | Human Label | Purpose |
|---|---|---|
| `customfield_10103` | Description formatted | Primary rich-text when `description` empty |
| `customfield_10283` | Clarification Reason | Open questions |
| `customfield_10048` | Acceptance Criteria | Acceptance criteria |
| `customfield_10745` | Test Case | Test case details |
| `customfield_10104` | Design | Design notes |
| `customfield_10217` | Bug Description | Bug A.R. / E.R. |
| `customfield_10095` | Tester | Assigned tester |
| `customfield_10877` | Approved for Prod | Prod approval |
| `customfield_10485` | QA points | QA story points |
| `customfield_10484` | Dev Points | Dev story points |
| `customfield_10483` | DB Points | DB story points |

3. When `description` is null/empty → treat **`customfield_10103`** as primary; parse ADF (images, inline links, attachments).
4. Extract and list all URLs from rich-text fields.
5. Never skip non-null custom fields because `description` has content.

---

## Jira attachment content analysis

When attachments exist and content analysis is required:

1. Metadata via `getJiraIssue` → `fields.attachment[]`.
2. Content: run  
   `powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/jira/download-jira-attachments.ps1" -IssueKey "<KEY>"`  
   → `Cursor-Project/config/jira/attachments/<KEY>/`
3. Analyze: images (Read tool), SVG (text), DOCX (`.txt` extract), PDF (Read tool).
4. Cite: "found in attachment: `<filename>`"
5. If `JIRA_API_TOKEN` missing → metadata only; note in reply.

**Env vars:** `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_BASE_URL` (see `Cursor-Project/EnergoTS/.env` or system env).
