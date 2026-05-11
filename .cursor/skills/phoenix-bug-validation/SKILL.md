---
name: phoenix-bug-validation
description: Validates bug reports using Rule 32 workflow: Confluence first, then codebase, then combined analysis and report. Use when the user asks to validate a bug, verify a bug report, or run bug validation (Rule 32). READ-ONLY.
---

# Phoenix Bug Validation

Ensures **Rule 32** bug validation (mandated by `.cursor/rules/workflows/workflow_rules.mdc`; **this SKILL is the canonical procedure**): Confluence → codebase → analysis → **full reply in chat**. **READ-ONLY** — no code changes during validation. Persisted `BugValidation_*.md` only if the user runs **`/report`** or explicitly asks to save.

## When to Apply

- User asks to validate a bug, verify a bug report, or check if a bug is valid.
- User mentions "bug validation", "bug report", or "Rule 32".

## Mandatory: Rule 32 in chat

There is **no** `from agents.Main import get_bug_finder_agent` in this workspace. Run the steps below directly.

## Workflow (Rule 32) - 5-Verdict System + mandatory reproducibility

### Step 0: Resolve environment + align Phoenix branches (Rule PHOENIX-SWITCH.0) [MANDATORY]

- Pick the environment from the bug ticket: Environment field, ticket text, attached logs, screenshots showing URL hostnames. Map to one of `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`. If genuinely ambiguous, ASK the user (Rule CONF.0).
- Run `.cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` so every `Cursor-Project/Phoenix/*` repo aligns to `origin/<branch>` (latest tip). For `prod` you MUST first explain to the user that local Phoenix edits will be discarded, wait for explicit ack, and only then call the script with `-ConfirmProd`.
- Inspect the exit code: `0` proceed; `2` proceed but flag mixed-state in chat; `3` stop and ask the user to fix connectivity / VPN / credentials before continuing. Local Phoenix edits are discarded by the script; Phoenix files remain READ-ONLY (Rule 0.8 Tier A).
- If a previous step in this chat session already aligned Phoenix to the same environment (e.g. parent ran `/phoenix` first), do NOT re-run alignment — reuse it (Rule PHOENIX-SWITCH.0 §7a).

### Step 1: Extract Expected Behavior

- Extract the bug's expected result from the ticket description.
- Identify the specific behavior that should occur according to the bug reporter.
- Document the claimed expected behavior clearly.

### Step 1b: Process diagrams (local library + linked assets)

Use diagrams **when they sharpen expected flow or reproducibility**, not as a substitute for code or Confluence.

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

### Step 2: Confluence validation (evidence strength)

- Use MCP Confluence tools: search, getSpaces, getPages, getConfluencePage.
- Check for EXACT match: Does Confluence explicitly support the bug's expected behavior?
- Check for CONTEXTUAL match: Does Confluence provide similar/related rules that suggest the expected behavior?
- Check for CONTRADICTION: Does Confluence explicitly state different behavior than what the bug expects?
- Report: "Confluence validation: [exact match / contextual match / no match / contradicts / search failed] - [explanation]".
- Document Confluence sources (page IDs, titles, URLs).
- If Confluence MCP fails:
  - retry up to 3 times with short backoff,
  - if still failing, return `PROCESS BLOCKED` (operational status),
  - report exact error and attempted fixes,
  - ask the user explicitly what should be done next.
- Never continue to final bug verdict when Confluence validation is unavailable.

### Step 3: Code validation (behavior analysis)

- Search codebase (semantic search, grep, read_file) for relevant code.
- Analyze actual code implementation behavior.
- Check if code behavior matches the faulty behavior described in the bug report.
- Report: "Code validation: [matches reported behavior / does not match reported behavior / could not verify] - [explanation]".
- Include file paths, line numbers, and code snippets; identify exact implementation.

### Step 4: Reproducibility verification via test pipeline (MANDATORY)

- In Rule 32 bug validation, this pipeline is always required before final verdict.
- Run in this order:
  1. `cross-dependency-finder`
  2. `test-case-generator`
  3. `energo-ts-test`
  4. `playwright-test-validator`
  5. `energo-ts-run`
- If pipeline evidence is incomplete after retries, return `PROCESS BLOCKED` (no final verdict).
- Never claim "not reproducible" if `energo-ts-run` was not executed.

### Step 5: Apply 5-Verdict Decision Matrix

- **VALID**: Exact Confluence match + code confirms reported faulty behavior
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence
- **INSUFFICIENT EVIDENCE**: Confluence/code evidence cannot be reliably established

- Structure: "1. Expected Behavior", "2. Confluence Validation", "3. Code Analysis", "4. Reproducibility Pipeline", "5. Final Verdict", "6. Next Steps".

### Step 6: Results (chat + Slack; optional file)

- **Required (every run):** Post the full structured analysis in **chat** after each completed validation (expected behavior, Confluence validation, code analysis, verdict, paths/lines, next steps).
- **Required (every run):** Send the same full structured analysis to the Slack channel **`bug-validation`** (channel ID: `C0AUEEDVCEL`) after each completed validation. Use `slack_send_message(channel_id: "C0AUEEDVCEL", message: <full report>)` via plugin-slack-slack MCP.
- Slack delivery is built into the Cursor bug-validator workflow; it is not a manual one-off send from the parent chat.
- If Slack MCP/auth is unavailable, include `Slack delivery: failed` and the failure reason in the validation output.
- Chat posting is mandatory even if Slack delivery succeeds or a markdown file is written.
- Never send only "report sent" or summary-only text without the full chat analysis.
- **Optional:** If the user runs **`/report`** or explicitly asks to save → write `…/YYYY/<english-month>/<DD>/BugValidation_[DescriptiveName].md` under **Chat reports** per **`Cursor-Project/reports/README.md`**.

## Hard Gate: Reproducibility Pipeline Cannot Be Skipped

- Bug validation is considered complete only after reproducibility pipeline evidence is present.
- Required evidence list:
  - Swagger refresh status (`ok` or `failed_using_cache` with warning),
  - cross-dependency output reference,
  - generated test case references for both:
    - `test_cases/Backend/<Topic_name>.md`
    - `test_cases/Frontend/<Topic_name>.md`
  - generated Playwright spec reference,
  - Playwright validator result,
  - Playwright run result from **`energo-ts-run`** (subagent / EnergoTS `npx playwright test`).
- If any evidence item is missing, trigger auto-recovery and retries; if still unresolved after max retries, return `PROCESS BLOCKED` with `Missing pipeline evidence`.
- Do not issue `VALID`, `NEEDS CLARIFICATION`, `NEEDS APPROVAL`, or `NOT VALID` without **`energo-ts-run`** execution evidence.
- Do not label a bug "not reproducible" when **`energo-ts-run`** was not executed.
- Auto-recovery is mandatory when a step fails:
  - capture failure reason,
  - apply targeted fix,
  - rerun failed step and dependent downstream steps.
- Retry policy:
  - up to 3 retries per failed step,
  - up to 3 full pipeline attempts.
- If retries are exhausted, stop with `PROCESS BLOCKED` (no final verdict), report blocker and required user action.
- `PROCESS BLOCKED` output must not contain a final verdict; it must contain blocker details and a direct user question for next action.

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
- Confluence/code evidence is unavailable or too weak for a reliable business verdict
- Action: resolve evidence gaps, then rerun validation

**PROCESS BLOCKED (operational status, not verdict)**
- Used only when mandatory pipeline retries are exhausted
- Indicates execution blocker prevented completion of required evidence
- Action: user resolves blocker; validator resumes from failed step

### Important Notes:
- Never use vague verdicts like "INCONCLUSIVE"  
- Always separate evidence quality from business verdict
- Make recommendations actionable based on verdict type

## Confidence Score (Rule CONF.1) [MANDATORY]

The final output MUST include a **Confidence Score** (0–100%). Format: `**Confidence: XX%** Reason: <explanation>`. Scoring: 90–100% = Confluence exact match + code confirms; 70–89% = contextual match or partial code evidence; 50–69% = significant evidence gaps; <50% = validation incomplete, flag prominently. Be honest — do not inflate.

## Routing reference

- **Subagent (canonical procedure):** `.cursor/agents/bug-validator.md` — invoke **`bug-validator`** / BugFinderAgent workflow when the user asks for bug validation (Rule 32).
