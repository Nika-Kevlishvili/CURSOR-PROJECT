# Generate Test Cases

Generate test cases from a bug or task description. **Rule 35:** When the user requests test case creation, run **cross-dependency-finder** FIRST, then **test-case-generator** with the finder's output. Route to **test-case-generator** subagent / TestCaseGeneratorAgent.

## When to Use

Use this command when the user asks to:
- Generate test cases for a bug
- Create test scenarios from a task or feature
- Derive test cases from a description
- Write test cases for a Jira ticket or Confluence story

## Mandatory Workflow (Rule 35)

### Step 1: Cross-Dependency Finder (MANDATORY – do not skip)

1. Run **cross-dependency-finder** for the same scope (bug/task/feature). It MUST follow **Rule 35a**: **Jira MCP + codebase + shallow Confluence** — **no** local merge/git or sync solely for cross-dep; **technical_details** from Jira + codebase (MR/merge only if user explicitly asked GitLab).
2. Cross-dependency-finder may consult **PhoenixExpert** to study the project.
3. Obtain the structured output (including **what_could_break** and **technical_details**).
4. Pass this output to the next step as `context['cross_dependency_data']`.

### Step 2: Test Case Generator

0. **Playwright instructions (MANDATORY):** Before generating test-case content, **read** **`Cursor-Project/config/playwright_generation/playwright instructions/`** in order: `project-description.md` → `general-rules.md` → `test-writing-rules.instructions.md` → `SKILL.md`; then any other `*.md` in that folder alphabetically. **Ignore** `__MACOSX` / `._*`. Generated cases MUST follow that pack so steps/expectations **bridge** to EnergoTS Playwright specs (`test.step`, HTTP/status/body checks per instructions, no contradictions with `general-rules.md`). Still use **`Cursor-Project/config/template/Test_case_template.md`** for document shape.
1. **Rule 0.3** — No Python `IntegrationService` in this workspace; follow MCP/Jira when needed (see `.cursor/rules/main/core_rules.mdc`).
2. **PhoenixExpert** – Consult if needed (Rule 8); reuse context from cross-dependency-finder if already provided.
3. **Confluence (MCP)** – Search and collect relevant docs (title, content, pageId, spaceId).
4. **Codebase** – Search for terms from the prompt; collect codebase_findings.
5. **Generate** – Call TestCaseGeneratorAgent with `prompt`, `prompt_type` ('bug'|'task'), `confluence_data`, and `context={'codebase_findings': ..., 'cross_dependency_data': <from Step 1>}` (generator MUST have loaded the Playwright instruction pack in step 0).
6. **Save** – Write test cases as **two separate files**: `Cursor-Project/test_cases/Backend/<Topic_name>.md` (TC-BE-N only) and `Cursor-Project/test_cases/Frontend/<Topic_name>.md` (TC-FE-N only).
7. **Report (optional)** – Save markdown under `reports/` only if the user asks for a run log; otherwise summarize in chat (Rule 0.6 default).

## Output – Structure and content (MANDATORY)

**Comprehensive coverage (CRITICAL):** Test case generation MUST produce **exhaustive** coverage of the task or bug – **not** a random or minimal set. Generate the **maximum number of test cases** that **fully cover** the scope:
- **All positive scenarios:** happy path(s), valid inputs, expected success, main flows.
- **All negative scenarios:** invalid inputs, missing/invalid IDs, wrong state, expected errors and rejections.
- **Edge cases and boundaries:** empty lists, zero amounts, max values, dates at boundaries, already-processed state, duplicate actions.
- **Regression/impact:** every scenario from `cross_dependency_data` (what_could_break, integration points) that could be affected.
Do **not** limit to 2–3 test cases; aim for **all plausible scenarios** so the task or bug is covered entirely.

**Content template:** Every test case document MUST follow the **Test Case Template**: **`Cursor-Project/config/template/Test_case_template.md`**. Use that template's structure (header, Summary, Scope, Test data, TC-1/TC-2/… with Test title in the heading, Description, Preconditions, Test steps, Expected test case results, Actual result if bug, References). **Include both positive and negative scenarios:** at least one **Positive** (valid input, happy path, expected success) and at least one **Negative** (invalid input, error condition, expected rejection); label each TC as **(Positive)** or **(Negative)**. Write in **maximally detailed**, **human-readable** language (full sentences where helpful, no unexplained jargon, plain English).

**Preconditions (MANDATORY creation-step rule):** Preconditions MUST describe HOW to create every entity — never just "entity X exists." Every precondition step must include the API endpoint (or UI action), key parameters (type, status, amount, dates, linked entities), and references to earlier steps. See `Test_case_template.md` for the full rule and examples.

**Folder:** Save as **two separate files** per topic:

**Root:** `Cursor-Project/test_cases/`

**Structure:**
- **Backend:** `Cursor-Project/test_cases/Backend/<Topic_name>.md` — contains ONLY **Backend Test Cases** (`TC-BE-N`).
- **Frontend:** `Cursor-Project/test_cases/Frontend/<Topic_name>.md` — contains ONLY **Frontend Test Cases** (`TC-FE-N`).
- Both files share the same `<Topic_name>` (e.g. `Invoice_cancellation.md`).
- Use underscores for multi-word topic names.

This applies to both generic flow and HandsOff — all test cases go to `Cursor-Project/test_cases/Backend/<Topic>.md` and `Cursor-Project/test_cases/Frontend/<Topic>.md`. Content spec: `Cursor-Project/config/template/Test_case_template.md`.

## Constraints

- **READ-ONLY** for Phoenix code; do not modify production code.
- All output in **English** (Rule 0.7).
- Never skip the cross-dependency-finder step when the user requested test cases.

## Step Log Mode (optional `--step-log`)

When the user passes **`--step-log`** (or asks for a "step log" / "per-step trace"), the orchestrator MUST save a per-step trace to:

`Cursor-Project/reports/<YYYY-MM-DD>/test-case-gen-<topic>-steplog.md`

Default behavior (no flag) is unchanged: only the final test cases are produced. The step log is **opt-in** so it does not bloat reports for every run.

### Why

Testers requested visibility into what happens at each phase so they can inspect context usage and accuracy of each step (cross-deps quality, Confluence hits, codebase findings, Playwright instruction adherence, TC count). Use it when debugging "why does the generator produce X" or when reviewing for the team.

### CRITICAL: Timing is mandatory

**Every step MUST have real wall-clock timing.** The orchestrator MUST:
1. Note the **start time** (HH:MM:SS) when it begins the overall workflow.
2. Note the **start and end time** of each step (or mark steps that ran in parallel with a shared time range).
3. Compute the **duration** of each step (e.g., `~2m 30s`).
4. In the **Summary**, include a timing table showing all steps with start/end/duration and a **Total elapsed** with the real wall-clock duration (not "~session" or "[completed]").

If exact timestamps are not available from tool output, estimate from file modification times, shell command `elapsed_ms`, or conversation flow. **Never write placeholder text** like `[completed]` or `~session` in place of timing data.

### What is logged per step

Each entry records: **timing (start → end | duration)**, step name, inputs (paraphrased), key actions, outputs (paths / counts / refs), and any warnings.

| # | Step | Logged content |
|---|------|---------------|
| 0 | Playwright instructions | Files read (in order), key rules extracted, any junk skipped (`__MACOSX`, `._*`) |
| 1 | Cross-dependency-finder | Scope, Jira key, upstream/downstream counts, `what_could_break` items, `technical_details` refs |
| 2 | PhoenixExpert consultation | Whether consulted; question asked; key answer points (paraphrased) |
| 3 | Confluence (MCP) | Queries run, page titles + IDs/spaces found, count |
| 4 | Codebase | Search terms, top file paths matched (cap at ~10), count |
| 5 | Test case generation | Backend TC count, Frontend TC count, positive/negative/edge split, regression coverage |
| 6 | File save | Backend path, Frontend path, README updates |

### Output format

```markdown
# Test Case Generation Step Log -- <Topic>
Date: <YYYY-MM-DD HH:MM>
Trigger: <prompt summary>
Jira: <key or N/A>
Mode: --step-log

## Step 0 -- Playwright instructions [HH:MM:SS → HH:MM:SS | ~Xm Ys]
Files read (in order):
1. project-description.md
2. general-rules.md
3. test-writing-rules.instructions.md
4. SKILL.md
Other *.md (alphabetical): ...
Skipped: __MACOSX/, ._*
Key rules applied: <bullet list>

## Step 1 -- Cross-dependency-finder [HH:MM:SS → HH:MM:SS | ~Xm Ys]
Scope: <bug/task summary>
Jira: <key>
Upstream: <count> -- <short list>
Downstream: <count> -- <short list>
What_could_break (regression candidates): <count>
Technical_details refs: <files / Jira links>

## Step 2 -- PhoenixExpert [HH:MM:SS → HH:MM:SS | parallel with Step N | ~Xm Ys]
Consulted: yes/no
Question: ...
Key points: ...

## Step 3 -- Confluence (MCP) [HH:MM:SS → HH:MM:SS | ~Xm Ys]
Queries: ["<q1>", "<q2>"]
Pages found (count): N
- "<title>" (pageId, spaceId)
- ...

## Step 4 -- Codebase [HH:MM:SS → HH:MM:SS | ~Xm Ys]
Terms: ["<t1>", "<t2>"]
Top matches (N total):
- <path>
- ...

## Step 5 -- Test case generation [HH:MM:SS → HH:MM:SS | ~Xm Ys]
Backend TCs: N (Positive: X, Negative: Y, Edge: Z, Regression: R)
Frontend TCs: N (Positive: X, Negative: Y, Edge: Z, Regression: R)
Coverage notes: <brief>

## Step 6 -- File save [HH:MM:SS → HH:MM:SS | ~Xm Ys]
- Cursor-Project/test_cases/Backend/<Topic>.md
- Cursor-Project/test_cases/Frontend/<Topic>.md
README updates: Backend/README.md, Frontend/README.md

## Summary
Total steps: 7
Start: HH:MM:SS | End: HH:MM:SS

| Step | Name | Start | End | Duration | Notes |
|------|------|-------|-----|----------|-------|
| 0 | Playwright instructions | HH:MM:SS | HH:MM:SS | ~Xm Ys | <what happened> |
| ... | ... | ... | ... | ... | ... |

Warnings: <list or "None">
**Total elapsed: ~Xm Ys** (HH:MM:SS → HH:MM:SS; note parallelism if applicable)
```

### Triggers

- `/test-case-generate --step-log <prompt>`
- `/test-case-generate <prompt> --step-log`
- "Generate test cases for X with step log"
- "Generate test cases and save the per-step trace"

### Notes

- The trace is **separate** from the actual test case files; those still go to `test_cases/Backend/` and `test_cases/Frontend/`.
- The orchestrator (parent agent running this command) writes the step log -- subagents do not need to write it themselves; they just return their results so the parent can summarize each step in the log.
- File naming: `<topic>` matches the topic used for the test case files (e.g. `Invoice_cancellation` -> `test-case-gen-Invoice_cancellation-steplog.md`).

## Response Requirements

- State "**Agent:** TestCaseGeneratorAgent" at beginning when applicable.
- Return the generated test cases and the paths where they were saved (e.g. `test_cases/Backend/Invoice_cancellation.md` and `test_cases/Frontend/Invoice_cancellation.md`).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Example Triggers

- "Generate test cases for this bug"
- "Create test scenarios for REG-1234"
- "Write test cases for the billing profile feature"
- "Derive test cases from this task description"
