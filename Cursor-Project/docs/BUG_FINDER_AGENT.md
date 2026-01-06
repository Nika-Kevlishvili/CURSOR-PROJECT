# BugFinderAgent - Bug Validation Agent

## Overview

BugFinderAgent is a specialized agent for validating bug reports by comparing them against Confluence documentation and codebase implementation. It follows Rule 32 workflow to ensure comprehensive bug validation.

## Workflow

The agent follows a strict 4-step workflow (Rule 32):

1. **Confluence Validation (FIRST)**
   - Search Confluence using MCP tools
   - Validate bug description against documentation
   - Check if bug report information is correct
   - Document all sources found

2. **Code Validation (SECOND)**
   - Search codebase using semantic and text search
   - Analyze code implementation
   - Check if code satisfies bug report requirements
   - Identify exact bug location (file, lines, code snippets)

3. **Comprehensive Analysis**
   - Combine Confluence and code findings
   - Determine if bug is valid (code differs from description)
   - Provide detailed analysis with conclusions

4. **Report Generation**
   - Create markdown report file in `reports/YYYY-MM-DD/BugValidation_[BugName].md`
   - Include all findings, code references, and conclusions
   - **CRITICAL: READ-ONLY mode - NO code modifications during validation**

## Usage

### Basic Usage

```python
from agents.Main import get_bug_finder_agent

# Get agent instance
bug_finder = get_bug_finder_agent()

# Validate a bug
bug_description = "User cannot save changes to customer profile"
result = bug_finder.validate_bug(bug_description)

# Format report
report = bug_finder.format_validation_report(result)
print(report)
```

### Usage with Cursor AI

When using BugFinderAgent with Cursor AI, the agent structure is provided, but actual MCP tool calls are made by Cursor AI. Here's the recommended workflow:

1. **Call BugFinderAgent.validate_bug()** - This initializes the workflow and logs activities
2. **Use MCP Confluence tools** (called by Cursor AI):
   - `mcp_Confluence_search()` - General search
   - `mcp_Confluence_searchConfluenceUsingCql()` - CQL search
   - `mcp_Confluence_getConfluencePage()` - Get specific page
   - `mcp_Confluence_getConfluenceSpaces()` - Get spaces
3. **Use codebase search tools** (called by Cursor AI):
   - `codebase_search()` - Semantic search
   - `grep()` - Text search
4. **Populate validation result** with findings
5. **Call format_validation_report()** to generate markdown report

### Example: Complete Validation Flow

```python
from agents.Main import get_bug_finder_agent

bug_finder = get_bug_finder_agent()
bug_description = "Payment API returns 500 error when processing refunds"

# Step 1: Initialize validation (logs activity, calls IntegrationService)
result = bug_finder.validate_bug(bug_description)

# Step 2: Search Confluence (using MCP tools - called by Cursor AI)
# mcp_Confluence_search(query=bug_description)
# mcp_Confluence_searchConfluenceUsingCql(cql="text ~ 'payment' AND text ~ 'refund'")

# Step 3: Search codebase (using codebase tools - called by Cursor AI)
# codebase_search(query="How does payment refund processing work?")
# grep(pattern="refund", path="Phoenix/phoenix-payment-api")

# Step 4: Populate result with findings
result['confluence_validation'] = {
    'status': 'correct',
    'explanation': 'Confluence documentation matches bug description',
    'sources': [
        {'title': 'Payment API Documentation', 'url': 'https://...'}
    ]
}

result['code_validation'] = {
    'status': 'does_not_satisfy',
    'explanation': 'Code has error handling issue in refund processing',
    'code_references': [
        {'file': 'PaymentService.java', 'line': 145, 'description': 'Missing null check'}
    ]
}

result['conclusion'] = {
    'bug_valid': True,
    'summary': 'Bug is valid - code differs from documentation',
    'details': 'Documentation states refunds should handle errors gracefully, but code lacks proper error handling'
}

# Step 5: Generate report
report = bug_finder.format_validation_report(result)
```

## Integration with Rules

BugFinderAgent follows all critical rules:

- **Rule 0.3**: Calls `IntegrationService.update_before_task()` before validation
- **Rule 0.4**: Consults PhoenixExpert before validation
- **Rule 0.6**: Generates reports via ReportingService
- **Rule 32**: Follows mandatory bug validation workflow
- **Rule 0.1**: Ends with "Agents involved" disclosure

## CRITICAL: READ-ONLY Mode

**When validating bugs, BugFinderAgent MUST work in READ-ONLY mode:**

- ✅ **DO**: Analyze code, search Confluence, search codebase
- ✅ **DO**: Document findings, create reports, identify bug locations
- ✅ **DO**: Provide analysis and conclusions
- ❌ **DO NOT**: Modify code during validation
- ❌ **DO NOT**: Attempt to fix bugs during validation
- ❌ **DO NOT**: Make any code changes unless user explicitly requests bug fixing AFTER validation is complete

**Code modifications are ONLY allowed if user explicitly requests bug fixing AFTER validation is complete.**

## Output Format

The agent generates structured markdown reports with:

1. **Bug Description** - The bug being validated
2. **Confluence Validation** - Status, explanation, and sources
3. **Code Analysis** - Status, explanation, and code references
4. **Conclusion** - Bug validity determination with summary and details

## Agent Organization

BugFinderAgent is located in `agents/Main/` as it's a primary agent providing core functionality (bug validation).

## Reporting

All bug validations are logged via ReportingService and reports are saved to `reports/YYYY-MM-DD/BugFinderAgent_HHMM.md`.
