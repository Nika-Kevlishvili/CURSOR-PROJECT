# Bug Validation

Validate bug reports using BugFinderAgent (Rule 32 - MANDATORY workflow).

## ALWAYS Use BugFinderAgent:
ALL bug validation requests MUST be handled by BugFinderAgent - NO EXCEPTIONS.

## Mandatory Workflow:

### Step 0a: Resolve environment + align Phoenix branches (Rule PHOENIX-SWITCH.0)
- **MANDATORY resolver call:** Run `/environment-resolve` (EnvironmentResolverAgent) first and pass bug context (ticket fields/text/log hints). It must return exactly one environment from `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`.
- If ambiguity remains, EnvironmentResolverAgent MUST show a questionnaire with those six options and use the user-selected environment (Rule CONF.0). Do not silently default.
- **Prod safety gate (Rule PHOENIX-SWITCH.0 §1a):** if the resolved env is `prod`, FIRST tell the user that local Phoenix edits will be discarded and force-reset to `origin/prod`, wait for explicit user acknowledgement, then add `-ConfirmProd`. Skip this step for non-prod envs.
- **Subagent reuse (Rule PHOENIX-SWITCH.0 §7a):** if a previous step in this chat session already aligned Phoenix to the same env and exited `0`, do NOT re-run the script — reuse it.
- Otherwise run: `powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` (add ` -ConfirmProd` for `prod` only, after user ack).
- Aligns every `Cursor-Project/Phoenix/*` repo to `origin/<branch>` (latest tip). Local Phoenix edits are DISCARDED; Phoenix files remain READ-ONLY (Rule 0.8 Tier A).
- Inspect exit code: `0` proceed; `2` proceed but flag mixed-state in the chat answer; `3` STOP and ask user to fix VPN / credentials before retrying.
- Report the environment, target branch, exit code, and per-repo alignment outcome in the chat answer before continuing.

### Step 0b: Recovery Intake (when ticket has no direct steps)
- Do not reject validation only because explicit reproduce steps are missing.
- Read `Cursor-Project/config/bug_validation/production_bug_patterns.json` and use it as the baseline case-pattern library.
- Match ticket signals against `domain`, `evidence_keywords`, and `trigger_signature`.
- Prefer `pattern_reliability=high`; treat `medium` and `medium_high` as hypotheses requiring stronger code and Playwright evidence.
- Do not use `Won't Do` patterns as application-defect proof; use them only as data-state or historical-flow hypotheses.
- Extract and structure available evidence from summary/description/comments.
- Parse links and possible logs/errors from ticket text.
- Infer candidate reproduce flow from available actions, entities, and symptoms.
- Continue validation using this recovered context.

### Step 1: Extract Expected Behavior
- Extract bug's expected result from ticket description  
- Identify specific behavior claimed by bug reporter
- Document expected behavior clearly

### Step 2: Confluence Validation (Evidence Strength)
- Search Confluence using MCP tools FIRST
- Assess evidence strength: exact match / contextual match / no match / contradicts / search failed
- Report: "Confluence validation: [evidence strength] - [explanation]"

### Step 3: Code Validation (Behavior Analysis)
- Search codebase using code search tools
- Analyze actual implementation behavior
- Check if code matches faulty behavior described in bug
- Report: "Code validation: [matches reported behavior/does not match/could not verify] - [explanation]"

### Step 4: Apply 5-Verdict Decision Matrix
- Use evidence + behavior to determine verdict
- Apply one of 5 verdicts: VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / NOT VALID / INSUFFICIENT EVIDENCE

### Step 5: Reproducibility Test Pipeline (MANDATORY)

The parent/orchestrator MUST run this delegated pipeline after analytical validation:

0. **Swagger refresh pre-step (MANDATORY):** run `powershell -ExecutionPolicy Bypass -File ".cursor/commands/update-swagger-specs.ps1"` before attempting Playwright generation. If refresh fails (VPN/network), continue with cached specs but explicitly flag `swagger_refresh=failed_using_cache` in the validation output.
1. `/cross-dependency-finder`  
2. `/test-case-generate`  
3. `/energo-ts-test`  
4. route to **playwright-test-validator** subagent (PlaywrightTestValidatorAgent)  
5. `/energo-ts-run`

Use final test run outcomes to determine whether the bug condition is practically reproducible, even when original ticket steps were incomplete.

### Step 6: Deliver final verdict (chat + Slack; file only on request)
- Combine all findings with clear verdict and reasoning in the **chat reply** (full structured markdown).
- **Mandatory behavior:** after **every** completed bug validation run, post the full report in the current chat. Do not skip chat posting even when other channels (for example Slack) are used by separate workflows.
- **Mandatory Slack behavior:** after **every** completed bug validation run, send the same full report to the Slack channel **`bug-validation`** (channel ID: `C0AUEEDVCEL`) using `slack_send_message(channel_id: "C0AUEEDVCEL", message: <full report>)` via plugin-slack-slack MCP. This is part of the Cursor bug-validator workflow and must not depend on the user asking for a one-off Slack send.
- If Slack MCP/auth is unavailable, include `Slack delivery: failed` and the failure reason in the chat output.
- Do not substitute the chat report with a short status update like "report sent".
- Include actionable next steps based on verdict.
- **Final verdict rule:** Concluding validity must be based on full evidence, including Playwright pipeline outcomes.
- **Disk:** Save `BugValidation_[DescriptiveName].md` under **Chat reports** only if the user runs **`/report`** or explicitly asks to save; otherwise no file (Rule 0.6).

## Response Structure:

```markdown
## Bug Validation Analysis

### 1. Expected Behavior
**Bug Claims:** [What the bug report says should happen]
**Context:** [Relevant business context or user scenario]

### 2. Confluence Validation  
**Evidence Strength:** [exact match / contextual match / no match / contradicts / search failed]
**Explanation:** [Detailed explanation of Confluence findings]
**Sources:** [List of Confluence pages found with page IDs, titles, URLs]

### 3. Code Analysis
**Behavior Match:** [matches reported behavior / does not match reported behavior / could not verify]
**Explanation:** [Detailed explanation of actual code behavior]
**Code References:**
- File: [path/to/file.java]
- Lines: [123-145]
- Implementation: [Description of what code actually does]

### 4. Reproducibility Pipeline
**Cross-dependency result:** [summary, file/path if produced]
**Generated test cases:** [Backend/Frontend paths and scenario count]
**Generated Playwright spec:** [spec path]
**Playwright validation:** [passed/failed + key issues]
**Playwright run result:** [passed/failed/not run + key failure reason]
**Practical reproducibility:** [reproducible / not reproduced / inconclusive]

### 5. Final Verdict
**Verdict:** [VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / NOT VALID / INSUFFICIENT EVIDENCE]
**Reasoning:** [Why this verdict was chosen based on evidence matrix]
**Next Steps:** [What should be done next based on verdict]
```

## Workflow Requirements:
- **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed
- Consult PhoenixExpert for context
- Use READ-ONLY mode (no code modifications)
- Do **not** save a BugValidation file unless the user runs **`/report`** or explicitly requests a saved report under **Chat reports** (per **`Cursor-Project/reports/README.md`**). No Summary/agent files unless explicitly requested.

## 5-Verdict Decision Matrix:

- **VALID**: Exact Confluence match + code confirms reported faulty behavior → Fix the bug
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior → Get product clarification  
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior → Get product approval
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence → Close as "working as designed"
- **INSUFFICIENT EVIDENCE**: Cannot access Confluence/code or evidence too weak → Resolve technical issues

## Response Must End With:
"Agents involved: BugFinderAgent, PhoenixExpert, CrossDependencyFinderAgent, TestCaseGeneratorAgent, EnergoTSTestAgent, PlaywrightTestValidatorAgent, EnergoTSRunAgent" (omit only agents that truly did not participate because a prior required step failed)
