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

## Workflow (Rule 32)

### Step 1: Confluence validation (first)

- Use MCP Confluence tools: search, getSpaces, getPages, getConfluencePage.
- Check if bug description matches Confluence documentation.
- Report: "Confluence validation: [correct / incorrect / partially correct] - [explanation]".
- Document Confluence sources (page IDs, titles, URLs).

### Step 2: Code validation (second)

- Search codebase (semantic search, grep, read_file) for relevant code.
- Analyze implementation vs expected behavior in the bug report.
- Report: "Code validation: [satisfies / does not satisfy] the bug report - [explanation]".
- Include file paths, line numbers, and code snippets; identify bug location.

### Step 3: Combined analysis

- Confluence findings + code analysis.
- Clear answers: (1) Is bug report correct per Confluence? (2) Does code satisfy the case? (3) Bug VALID or NOT VALID?
- Structure: "1. Confluence Validation", "2. Code Analysis", "3. Conclusion".

### Step 4: Report file

- Save markdown to: `Cursor-Project/reports/YYYY-MM-DD/BugValidation_[DescriptiveName].md`.
- Include Confluence validation, code analysis, conclusion, code references (paths + lines).
- May include suggested fix as text only; do not implement code changes.

## READ-ONLY

- No code modifications during validation.
- No fixing bugs unless user explicitly asks after validation is complete.

## Integration

- **Rule 0.3:** follow MCP/Jira when needed — no Python IntegrationService here.
- Consult PhoenixExpert for context when needed.
- Markdown reports per Rule 0.6.
- End with: "Agents involved: BugFinderAgent (workflow), PhoenixExpert" (or as applicable).

## Command reference

- `.cursor/commands/bug-validate.md`
- `.cursor/agents/bug-validator.md`
