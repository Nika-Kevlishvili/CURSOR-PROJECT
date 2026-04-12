---
name: phoenix-bug-validation
description: Validates bug reports using Rule 32 workflow: Confluence first, then codebase, then combined analysis and report. Use when the user asks to validate a bug, verify a bug report, or run bug validation (Rule 32). READ-ONLY.
---

# Phoenix Bug Validation

Ensures bug validation follows **Rule 32** in `.cursor/rules/workflows/workflow_rules.mdc`: Confluence → codebase → analysis → report. **READ-ONLY** — no code changes during validation.

## When to Apply

- User asks to validate a bug, verify a bug report, or check if a bug is valid.
- User mentions "bug validation", "bug report", or "Rule 32".

## Mandatory: Rule 32 in chat

There is **no** `from agents.Main import get_bug_finder_agent` in this workspace. Run the steps below directly.

## Workflow (Rule 32) - 5-Verdict System

### Step 1: Extract Expected Behavior

- Extract the bug's expected result from the ticket description.
- Identify the specific behavior that should occur according to the bug reporter.
- Document the claimed expected behavior clearly.

### Step 2: Confluence validation (evidence strength)

- Use MCP Confluence tools: search, getSpaces, getPages, getConfluencePage.
- Check for EXACT match: Does Confluence explicitly support the bug's expected behavior?
- Check for CONTEXTUAL match: Does Confluence provide similar/related rules that suggest the expected behavior?
- Check for CONTRADICTION: Does Confluence explicitly state different behavior than what the bug expects?
- Report: "Confluence validation: [exact match / contextual match / no match / contradicts / search failed] - [explanation]".
- Document Confluence sources (page IDs, titles, URLs).

### Step 3: Code validation (behavior analysis)

- Search codebase (semantic search, grep, read_file) for relevant code.
- Analyze actual code implementation behavior.
- Check if code behavior matches the faulty behavior described in the bug report.
- Report: "Code validation: [matches reported behavior / does not match reported behavior / could not verify] - [explanation]".
- Include file paths, line numbers, and code snippets; identify exact implementation.

### Step 4: Apply 5-Verdict Decision Matrix

- **VALID**: Exact Confluence match + code confirms reported faulty behavior
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior  
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence
- **INSUFFICIENT EVIDENCE**: Cannot access Confluence/code or evidence too weak

- Structure: "1. Expected Behavior", "2. Confluence Validation", "3. Code Analysis", "4. Final Verdict".

### Step 5: Report file

- Save markdown under **Chat reports**: `…/YYYY/<english-month>/<DD>/BugValidation_[DescriptiveName].md` per **`Cursor-Project/reports/README.md`**.
- Include expected behavior, Confluence validation, code analysis, final verdict with reasoning, code references (paths + lines).
- Include next steps based on verdict; do not implement code changes during validation.

## READ-ONLY

- No code modifications during validation.
- No fixing bugs unless user explicitly asks after validation is complete.

## Integration

- **Rule 0.3:** follow MCP/Jira when needed — no Python IntegrationService here.
- Consult PhoenixExpert for context when needed.
- Persisted **`BugValidation_*.md`** per Rule 32 (Rule 0.6 workflow exception).
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
- Technical failure: Confluence inaccessible, code unreachable, search failed
- Evidence too weak or incomplete to make determination
- Action: Resolve technical issues and retry validation

### Important Notes:
- Never use vague verdicts like "INCONCLUSIVE"  
- Always separate evidence quality from business verdict
- Make recommendations actionable based on verdict type

## Command reference

- `.cursor/commands/bug-validate.md`
- `.cursor/agents/bug-validator.md`
