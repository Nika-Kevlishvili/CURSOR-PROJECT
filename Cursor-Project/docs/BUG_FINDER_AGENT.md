# BugFinderAgent - Bug Validation Agent

> **Current locations:** Cursor → **`.cursor/agents/bug-validator.md`**, **`.cursor/skills/phoenix-bug-validation/SKILL.md`**. Reports → **`Cursor-Project/reports/README.md`** (Chat reports + `YYYY/<english-month>/<DD>/`). Map: **[AGENT_SUBAGENT_MAP.md](AGENT_SUBAGENT_MAP.md)**. The sections below describe the **removed** Python agent; behavior is implemented in chat per Rule 32.

## Overview

BugFinderAgent is a specialized agent for validating bug reports using **Confluence**, **refreshed OpenAPI/Swagger** specs, and **aligned Phoenix code**, plus ticket/diagram recovery patterns. It follows **Rule 32**; the canonical step-by-step procedure is **`.cursor/skills/phoenix-bug-validation/SKILL.md`** (not historical Python steps below).

## Workflow

**Authoritative procedure (Cursor):** environment resolution + Phoenix branch alignment → expected behavior + diagrams → Confluence (MCP, REST fallback) → **mandatory `update-swagger-specs.ps1`** + OpenAPI cross-check → code analysis → **5-verdict matrix** → full answer in chat + Slack **`bug-validation`**; optional `BugValidation_*.md` only on **`/report`** or explicit save. **Rule 32 does not** run automatic test-case generation, Playwright authoring, or **`energo-ts-run`** — use Rules **35–37** / **36** for that scope.

**Historical note:** The numbered list below described an older 4-step narrative; prefer the SKILL above.

1. **Confluence validation** — MCP + REST fallback; evidence strength classification.
2. **Swagger/OpenAPI** — refresh then cite `Cursor-Project/config/swagger/<env>/swagger-spec.json` for contract-level claims.
3. **Code validation** — after `switch-phoenix-branches.ps1`; paths, lines, snippets.
4. **Verdict + delivery** — structured chat + Slack; READ-ONLY (no fixes during validation).

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

In this workspace, reports are **markdown files** under **`Cursor-Project/reports/`** per **`Cursor-Project/reports/README.md`** (Rule 0.6), not a Python ReportingService.
