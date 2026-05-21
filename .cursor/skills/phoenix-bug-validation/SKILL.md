---
name: phoenix-bug-validation
description: Validates bug reports using Rule 32 workflow — Confluence, refreshed Swagger/OpenAPI, Phoenix codebase, diagrams, recovery patterns — then 5-verdict analysis. No automatic test-case or Playwright pipeline. READ-ONLY.
---

# Phoenix Bug Validation

Ensures **Rule 32** bug validation (mandated by `.cursor/rules/workflows/workflow_rules.mdc`; **this SKILL is the canonical procedure**): **primary evidence** from **Confluence**, **OpenAPI/Swagger** (after mandatory refresh), and **Phoenix code** (aligned to the target environment), plus ticket/diagram recovery patterns → **full reply in chat**. **READ-ONLY** — no code changes during validation. Persisted `BugValidation_*.md` only if the user runs **`/report`** or explicitly asks to save.

**Out of scope for Rule 32:** automatic **test case** generation, **Playwright** spec authoring, **playwright-test-validator**, and **energo-ts-run**. Those belong to **Rule 35** (test cases), **Rule 36/37** (runs / HandsOff), or explicit user requests — not the bug-validator workflow.

**Confluence (exclusive to Rule 32):** **Broad, proactive** Confluence information gathering (Step 2 below) applies **only** when running **this** skill / **`bug-validator`** agent. **Do not** copy Step 2 into cross-dependency-finder, test-case-generator, HandsOff, or general Jira/Phoenix Q&A — those workflows keep **Rule 39** (linked-only for non-bugs), **Rule 35a** shallow Confluence for cross-dep, or user-requested search only.

## When to Apply

- User asks to validate a bug, verify a bug report, or check if a bug is valid.
- User mentions "bug validation", "bug report", or "Rule 32".

## Mandatory: Rule 32 in chat

There is **no** `from agents.Main import get_bug_finder_agent` in this workspace. Run the steps below directly.

## Workflow (Rule 32) — 5-Verdict System (Confluence + Swagger + Code)

### Step 0: Resolve environment + align Phoenix branches (Rule PHOENIX-SWITCH.0) [MANDATORY]

- Pick the environment from the bug ticket: Environment field, ticket text, attached logs, screenshots showing URL hostnames. Map to one of `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`. If genuinely ambiguous, ASK the user (Rule CONF.0).
- Run `.cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` so every `Cursor-Project/Phoenix/*` repo aligns to `origin/<branch>` (latest tip). For `prod` you MUST first explain to the user that local Phoenix edits will be discarded, wait for explicit ack, and only then call the script with `-ConfirmProd`.
- Inspect the exit code: `0` proceed; `2` proceed but flag mixed-state in chat; `3` stop and ask the user to fix connectivity / VPN / credentials before continuing. Local Phoenix edits are discarded by the script; Phoenix files remain READ-ONLY (Rule 0.8 Tier A).
- If a previous step in this chat session already aligned Phoenix to the same environment (e.g. parent ran `/phoenix` first), do NOT re-run alignment — reuse it (Rule PHOENIX-SWITCH.0 §7a).

### Step 1: Extract Expected Behavior + Reproduce Steps

- Extract the bug's expected result from the ticket description.
- Identify the specific behavior that should occur according to the bug reporter.
- Document the claimed expected behavior clearly.
- **Reproduce steps (mandatory, best effort):** Build a **numbered** list of steps to reproduce. Prefer text from the ticket (`Steps to reproduce`, description, comments). If missing or incomplete, extend using **Step 0b** recovery, code-informed order, and Confluence-linked flows. Label the source of each step or subsection: **`from_ticket`** | **`inferred_hypothesis`** | **`from_pattern_library`**. If absolutely no steps exist, provide a **minimal single step** describing what to open/verify and mark it **`inferred_hypothesis`** — do not omit the section.

### Step 1b: Process diagrams (local library + linked assets)

Use diagrams **when they sharpen expected flow or scope**, not as a substitute for code or Confluence.

1. **Local diagram library (no attachment on ticket)**  
   - Search **`Cursor-Project/config/Diagrams/`** — **`Bundle 4`**, **`Bundle 5`**, **`Bundle 6`** — when the bug has **no** diagram attachment and no diagram URL tied to this scope in Jira/Confluence evidence yet.  
   - Match by process/domain keywords from the ticket, filenames, and (if ambiguous) open candidate **`.svg`** files and verify labels/branches fit **this** case.  
   - If a clear match exists: cite **full workspace path** in the analysis (e.g. expected sequence vs reporter steps).

2. **Diagrams from Jira / Confluence links**  
   - Extract URLs from ticket rich-text and linked pages per **`evidence_only_project_answers.mdc`** (Figma, diagrams.net, embedded media).  
   - When `getConfluencePage` or linked URLs expose **direct downloadable assets** (e.g. PNG/SVG/PDF): save **read-only** copies under **`Cursor-Project/config/confluence/diagrams/<pageId-or-issueKey>/`** (create folders as needed) and cite that path.  
   - Macro-only embeds without a direct file URL: state limitation in chat; do not invent flows.

3. **Authority order**  
   - **Phoenix code > Confluence > diagram.** If a diagram contradicts code or Confluence, record the conflict explicitly — diagrams alone never overturn verified code/spec.

4. **Tracking for the final report**  
   - Maintain a list of **every diagram** that informed this validation (whether **local** `Cursor-Project/config/Diagrams/...`, **downloaded** under `Cursor-Project/config/confluence/diagrams/...`, or **Jira attachment** path after download). Each entry must be repeatable: **full workspace path** and, when applicable, **source wiki URL** or **Jira attachment filename + issue key**.

### Step 2: Confluence validation (evidence strength)

**Rule 32 only (vs Rule 39 / Rule 35a):** **Only bug-validator** performs proactive Confluence discovery here. **Do not** limit to URLs in the ticket. Other agents handling the **same bug ticket** (cross-dep, test cases, HandsOff) **must not** treat “it’s a Bug” as permission for broad wiki search — they follow **their** Confluence limits unless the user explicitly asks for wider search.

**2a — Topic scope (MANDATORY)**

- List **business domain + concepts** to research (objects, processes, fields, UI) from the ticket — not only text copied from a reporter’s SQL `LIKE` filter.
- Document scope in the report (short bullets).

**2b — Proactive information gathering (MANDATORY)**

**Goal:** Gather **any** Confluence information relevant to validating the bug (requirements, flows, rules, diagrams). This is **broad wiki research**, not mandatory `text ~` / SQL-style keyword matching.

**2c — Phase 2 / experimental wiki exclusion (MANDATORY for Prod and PreProd validation)**

When the resolved environment is **Prod** or **PreProd** (and by default for **Test** unless the ticket explicitly scopes Phase 2 / not-yet-released behavior):

1. **Decision basis (verdict, expected behavior, Confluence classification):** Use only pages that are **in scope for released production behavior**:
   - Under **`Phoenix documentation- Phase 1`** (page **164356**) or descendants whose title **does not** start with `Phase 2 -`.
   - Standalone Phoenix pages **without** a `Phase 2 -` title prefix (e.g. `Bundle 6 - Receivables`, `Invoice details`, `Manual credit or debit note change`).
2. **PROHIBITED as decision basis:** Any page whose **title** starts with **`Phase 2 -`**, or lives under wiki trees **`Phase 2 - Phoenix documentation`**, **`Phase 2 - Only Changes`**, **`Phase 2 - postponed`**, **`Phase 2 - Experimental Documentation`**, or **`Experimental Documentation`** (non–Phase-1 experimental specs).
3. **Allowed but non-decisive:** Phase 2 pages may be **read** for context or to note “planned / delta only”; they **must not** drive **VALID**, **NOT VALID**, or **exact match** alone. If Phase 1 has no rule, classify Confluence as **contextual match** or **no match** and lean on **code + DB** (code still wins over any Confluence).
4. **Report:** In **`### Confluence evidence (decision basis)`**, add **`Phase 2 excluded: yes`** and list any Phase 2 pages **read but excluded** (title + ID + URL). CQL/Rovo queries should prefer `ancestor = 164356` and/or `title !~ "Phase 2"` where supported.
5. **User override:** If the user explicitly asks to include Phase 2 for this ticket, document **`Phase 2 excluded: no (user override)`**.

1. **MCP:** Use Confluence read/search tools (`search`, `searchConfluenceUsingCql`, `getPagesInConfluenceSpace`, `getConfluencePage`, etc.). Apply **≥2 distinct** discovery approaches (e.g. space browse + search, title-oriented CQL + full-text). Cap result size; no full-wiki crawl.
2. **REST fallback (Rule 43):** MCP failure after retries → **`search-confluence-rest.ps1`** and/or **`get-confluence-page-rest.ps1`**.
3. **Read** bodies of relevant hits. Jira-linked pages are **additive**, never a substitute for proactive discovery.
4. Optional: grep cached **`config/confluence/pages/*.json`** after live attempts.

**Classify:** EXACT match / contextual match / no match / contradicts / **search failed**.

- **no match** = discovery **succeeded** (multiple attempts) but no applicable rule in pages read.
- **search failed** = Confluence unavailable after retries (list attempts).
- **PROHIBITED:** Skip Confluence when ticket has no link; wiki research limited to reporter `LIKE`/CQL `text ~` only; stop after one failed query.

- Use MCP Confluence tools: search, getSpaces, getPages, getConfluencePage.
- Check for EXACT match: Does Confluence explicitly support the bug's expected behavior?
- Check for CONTEXTUAL match: Does Confluence provide similar/related rules that suggest the expected behavior?
- Check for CONTRADICTION: Does Confluence explicitly state different behavior than what the bug expects?
- Report: "Confluence validation: [exact match / contextual match / no match / contradicts / search failed] - [explanation]".
- **Decision-basis URLs (MANDATORY):** For **every** Confluence page that informed the classification, narrative, or verdict (**including all pages read from search hits** and any Jira-linked URLs), list **page title**, **page ID**, and a **full browser wiki URL** in the final report (chat + Slack + optional disk). Build the URL from MCP/REST payload links (`webui`, `_links.base` + relative path, or v2 `webUrl`) or compose from **`CONFLUENCE_WIKI_BASE`** / **`CONFLUENCE_URL`** / **`JIRA_BASE_URL`+`/wiki`** per **`.cursor/rules/integrations/confluence_rest_fallback.mdc`** — never rely on page ID alone without a resolvable link. If a page was read but no absolute URL can be formed after one repair attempt, state **why** and paste the best available path fragment; do not silently omit pages that drove the decision.
- If Confluence MCP fails:
  - retry up to 3 times with short backoff,
  - if still failing, attempt **Confluence REST fallback** per **`.cursor/rules/integrations/confluence_rest_fallback.mdc`** for **CQL search** (`search-confluence-rest.ps1`) **and** page read (`get-confluence-page-rest.ps1`). Disclose **`Confluence source: REST fallback (MCP unavailable or failed after retries).`** in the validation output.
  - if **both** MCP (after retries) **and** REST fallback fail or credentials/base URL are missing, return **`PROCESS BLOCKED`** (operational status), report exact errors, **discovery methods attempted**, and ask the user what to do next.
- If REST fallback succeeds, complete Confluence validation from the REST payload (same evidence rules as MCP) and keep **`Confluence source: REST fallback …`** in the output.
- Never continue to final bug verdict when Confluence **search/read** remains unavailable after MCP retries **and** REST fallback has failed or could not be attempted (**`PROCESS BLOCKED`** per above).

### Step 3: Swagger / OpenAPI refresh and validation [MANDATORY]

- **Refresh (always run for Rule 32):**  
  `powershell -ExecutionPolicy Bypass -File ".cursor/commands/update-swagger-specs.ps1"`  
  - On **success:** document `swagger_refresh=ok`.  
  - On **failure** (network/VPN): **warn**, continue with **cached** specs under **`Cursor-Project/config/swagger/`**, document `swagger_refresh=failed_using_cache` and the error summary (no secrets).
- **Read OpenAPI evidence:** After refresh (or cache), use the spec for the **same environment** as Step 0. Paths follow **`Cursor-Project/config/swagger/environments.json`** `id` values (e.g. `dev`, `test`, `dev2`, `experiment`, `prod`). Map workspace environment `experiments` → swagger folder **`experiment`** when applicable. Prefer **`Cursor-Project/config/swagger/<id>/swagger-spec.json`** (see **`Cursor-Project/config/swagger/README.md`** if PreProd or extra ids exist).
- Cross-check bug claims that depend on HTTP contracts: paths, methods, request/response schemas, required fields, enums, status codes. Cite **swagger file path + operationId or path + schema name** per **`evidence_only_project_answers.mdc`**.
- Report: "Swagger validation: [supports reporter / contradicts reporter / not applicable / could not verify] - [explanation]".
- Swagger does **not** replace code: if Swagger and implementation disagree, **code wins** (same as Confluence vs code — code primary for runtime behavior; Swagger documents the published contract).

### Step 4: Code validation (behavior analysis)

- Search codebase (semantic search, grep, read_file) for relevant code **after** Step 0 alignment.
- Analyze actual code implementation behavior.
- Check if code behavior matches the faulty behavior described in the bug report.
- Report: "Code validation: [matches reported behavior / does not match reported behavior / could not verify] - [explanation]".
- Include file paths, line numbers, and code snippets; identify exact implementation.

### Step 5: Apply 5-Verdict Decision Matrix

Use **Confluence classification + code analysis**, with **Swagger** as supporting contract evidence when the bug is API-shaped. Allowed **after** Steps 0–4 are executed to the extent possible; if Step 2 ends in **`PROCESS BLOCKED`**, do **not** issue a business verdict.

- **VALID**: Exact Confluence match + code confirms reported faulty behavior (Swagger may strengthen contract-level claims).
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior.
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior.
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence.
- **INSUFFICIENT EVIDENCE**: Confluence and/or code and/or Swagger evidence cannot be reliably established for a business verdict.

**Report section order (chat + Slack + optional disk):**  
`### Reproduce steps` → `### Diagrams used in this validation` → `### Expected behavior` → **`### Confluence evidence (decision basis)`** (must include **full wiki URL per page** used; see Step 2) → Swagger → Code → Verdict → Next steps → Evidence checklist → Confidence.

### Step 6: Results (chat + Slack; optional file)

- **Required (every run):** Post the full structured analysis in **chat** after each completed validation. The body MUST include, at minimum, the sections in the **Report section order** above. **Slack** payload MUST be the **same** full structured body (not summary-only) so **Confluence decision URLs**, **Reproduce steps**, and **diagrams used** appear in **`bug-validation`**. Rule 32 does **not** require a Jira browse link unless the user asks for it.
- **Required (every run):** Send the same full structured analysis to the Slack channel **`bug-validation`** (channel ID: `C0AUEEDVCEL`) after each completed validation. Use `slack_send_message(channel_id: "C0AUEEDVCEL", message: <full report>)` via plugin-slack-slack MCP.
- Slack delivery is built into the Cursor bug-validator workflow; it is not a manual one-off send from the parent chat.
- If Slack MCP/auth is unavailable, include `Slack delivery: failed` and the failure reason in the validation output.
- Chat posting is mandatory even if Slack delivery succeeds or a markdown file is written.
- Never send only "report sent" or summary-only text without the full chat analysis.
- **Optional:** If the user runs **`/report`** or explicitly asks to save → write `…/YYYY/<english-month>/<DD>/BugValidation_[DescriptiveName].md` under **Chat reports** per **`Cursor-Project/reports/README.md`**.

## Operational gates (no Playwright / test-case pipeline)

- **`PROCESS BLOCKED`** (operational status, **not** a business verdict) when:
  - Phoenix alignment script exits **`3`** (all repos failed) and code-based conclusions would be unreliable, **or**
  - Confluence mandatory read path failed after MCP retries **and** REST fallback failed or could not run (**Step 2**), **or**
  - The user must confirm **prod** alignment and has not acknowledged yet.
- **`PROCESS BLOCKED`** output must not contain `VALID` / `NEEDS CLARIFICATION` / `NEEDS APPROVAL` / `NOT VALID` / `INSUFFICIENT EVIDENCE`; it must contain blocker details and a direct user question for next action.
- **Do not** claim Playwright-based reproducibility as part of Rule 32; if the user wants automated reproduction, route to **HandsOff (Rule 37)** or an explicit **test case / Playwright** request (**Rule 35 / 36**).

## READ-ONLY

- No code modifications during validation.
- No fixing bugs unless user explicitly asks after validation is complete.

## Integration

- **Rule 0.3:** follow MCP/Jira when needed — no Python IntegrationService here.
- Consult PhoenixExpert for context when needed.
- **Rule 0.6:** No automatic `BugValidation_*.md`; file only on **`/report`** or explicit save request.
- End with: "Agents involved: BugFinderAgent (workflow), PhoenixExpert" (or as applicable).

## Decision Matrix Details

### When to use each verdict:

**VALID**
- Confluence explicitly documents the expected behavior from the bug report
- Code implementation contradicts that documented expectation  
- Action: Fix the bug

**NEEDS CLARIFICATION**  
- Confluence has related/contextual documentation but no exact rule
- Code behavior matches what the bug describes as faulty
- Action: Get product clarification on expected behavior, then proceed

**NEEDS APPROVAL**
- No relevant Confluence documentation found for this specific case
- Code behavior matches what the bug describes as faulty  
- Action: Get product owner approval before treating as valid bug

**NOT VALID**
- Confluence explicitly contradicts the bug report's expected behavior
- Code correctly implements what Confluence specifies
- Action: Close bug as "working as designed"

**INSUFFICIENT EVIDENCE**
- Confluence/code/Swagger evidence is unavailable or too weak for a reliable business verdict
- Action: resolve evidence gaps, then rerun validation

**PROCESS BLOCKED (operational status, not verdict)**
- Used when mandatory **environment alignment** or **Confluence read** cannot complete (see Operational gates)
- Action: user resolves blocker; validator resumes from failed step

### Important Notes:
- Never use vague verdicts like "INCONCLUSIVE"  
- Always separate evidence quality from business verdict
- Make recommendations actionable based on verdict type

## Confidence Score (Rule CONF.1) [MANDATORY]

The final output MUST include a **Confidence Score** (0–100%). Format: `**Confidence: XX%** Reason: <explanation>`. Scoring: 90–100% = Confluence exact match + code confirms (+ Swagger aligned when API-scoped); 70–89% = contextual match or partial code/Swagger evidence; 50–69% = significant evidence gaps; <50% = validation incomplete, flag prominently. Be honest — do not inflate.

## Routing reference

- **Subagent (canonical procedure):** `.cursor/agents/bug-validator.md` — invoke **`bug-validator`** / BugFinderAgent workflow when the user asks for bug validation (Rule 32).
