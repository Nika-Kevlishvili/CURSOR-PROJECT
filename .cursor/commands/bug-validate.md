# Bug Validation

Validate bug reports using BugFinderAgent (Rule 32 - MANDATORY workflow).

## ALWAYS Use BugFinderAgent:
ALL bug validation requests MUST be handled by BugFinderAgent - NO EXCEPTIONS.

## Mandatory 3-Step Workflow:

### Step 1: Confluence Validation
- Search Confluence using MCP tools FIRST
- Check if bug description matches documentation
- Verify bug report accuracy against Confluence sources
- Report: "Confluence validation: [correct/incorrect/partially correct] - [explanation]"

### Step 2: Code Validation
- Search codebase using code search tools
- Analyze code implementation
- Check if code behavior matches expected behavior
- Report: "Code validation: [satisfies/does not satisfy] - [explanation]"

### Step 3: Comprehensive Analysis
- Combine Confluence and code findings
- Provide clear conclusion

## Response Structure:

```markdown
## Bug Validation Analysis

### 1. Confluence Validation
[Findings from Confluence search]

### 2. Code Analysis
[Findings from codebase analysis]

### 3. Conclusion
[Summary: Is bug report accurate? Does code satisfy requirements?]
```

## Workflow Requirements:
- Call `IntegrationService.update_before_task()` FIRST
- Consult PhoenixExpert for context
- Use READ-ONLY mode (no code modifications)
- Generate reports (Rule 0.6)

## Response Must End With:
"Agents involved: BugFinderAgent, PhoenixExpert"
