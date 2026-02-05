---
name: phoenix-bug-validation
description: Validates bug reports using BugFinderAgent: Confluence first, then codebase, then combined analysis and report. Use when the user asks to validate a bug, verify a bug report, or run bug validation workflow (Rule 32).
---

# Phoenix Bug Validation

Ensures bug validation follows the mandatory BugFinderAgent workflow (Rule 32): Confluence → codebase → analysis → report. READ-ONLY; no code changes during validation.

## When to Apply

- User asks to validate a bug, verify a bug report, or check if a bug is valid.
- User mentions "bug validation", "bug report", or "Rule 32".
- Command or request references bug-validate or BugFinderAgent.

## Mandatory: Use BugFinderAgent

All bug validation must go through BugFinderAgent. Do not validate bugs ad-hoc.

```python
from agents.Main import get_bug_finder_agent
bug_finder = get_bug_finder_agent()
result = bug_finder.validate_bug(bug_description)
# Perform validation via MCP + codebase search, then:
report = bug_finder.format_validation_report(result)
```

## Workflow (Rule 32)

### Step 1: Confluence validation (first)

- Use MCP Confluence tools: search, getSpaces, getPages, getConfluencePage.
- Check if bug description matches Confluence documentation.
- Report: "Confluence validation: [correct / incorrect / partially correct] - [explanation]".
- Document Confluence sources (page IDs, titles, URLs).

### Step 2: Code validation (second)

- Search codebase (codebase_search, grep) for relevant code.
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
- No fixing bugs unless user explicitly asks for a fix after validation is complete.

## Integration

- Call `IntegrationService.update_before_task()` first.
- Consult PhoenixExpert for context.
- Generate reports per Rule 0.6.
- End with: "Agents involved: BugFinderAgent, PhoenixExpert".

## Response structure (template)

```markdown
## Bug Validation Analysis

### 1. Confluence Validation
**Status:** [correct/incorrect/partially correct]
**Explanation:** ...
**Sources:** [Confluence pages]

### 2. Code Analysis
**Status:** [satisfies/does not satisfy]
**Explanation:** ...
**Code References:** File, lines, issue description

### 3. Conclusion
**Bug Valid:** [YES/NO]
**Summary:** ...
```

Full workflow details: `.cursor/rules/workflow_rules.mdc` (Rule 32).
