# TestCaseGeneratorAgent - Documentation

## Overview

TestCaseGeneratorAgent is an agent that generates test cases based on prompts. The agent considers:
- The prompt (bug or task description)
- Confluence documentation
- Codebase analysis

## Usage

### Basic Usage

```python
from agents.Main import get_test_case_generator_agent

# Initialize agent
agent = get_test_case_generator_agent()

# Generate test cases (basic - without Confluence/codebase)
result = agent.generate_test_cases(
    prompt="User cannot save customer data when identifier is too long",
    prompt_type='bug'
)
```

### Confluence and Codebase Integration

**Important:** Cursor AI must call MCP Confluence tools and codebase_search tool to retrieve data.

**Recommended Process:**

1. **Confluence Search (MCP tools):**
   ```python
   # Cursor AI should call:
   # 1. mcp_Confluence_getAccessibleAtlassianResources() - get cloudId
   # 2. mcp_Confluence_search(query=prompt) - search Confluence
   # 3. mcp_Confluence_getConfluencePage(cloudId, pageId) - get page content
   # 4. Collect results into list of dicts with 'title', 'content', 'pageId', 'spaceId'
   ```

2. **Codebase Search:**
   ```python
   # Cursor AI should call:
   # codebase_search(query="customer validation", target_directories=[])
   # codebase_search(query="identifier length", target_directories=[])
   # Collect results into findings list
   ```

3. **Test Case Generation:**
   ```python
   # Pass Confluence data and codebase findings
   confluence_data = [...]  # From MCP Confluence tools
   codebase_findings = {
       'findings': [...],  # From codebase_search
       'search_terms': ['customer', 'validation', ...]
   }
   
   result = agent.generate_test_cases(
       prompt=prompt,
       prompt_type='bug',
       confluence_data=confluence_data,
       context={'codebase_findings': codebase_findings}
   )
   ```

## Features

### 1. Auto-Detection
- Automatically detects prompt type (bug vs task)
- Analyzes key terms present in the prompt

### 2. Confluence Integration
- Searches for relevant documentation in Confluence
- Analyzes based on relevance
- Considers Confluence information when generating test cases

### 3. Codebase Analysis
- Searches for relevant code in the codebase
- Extracts validation rules, error handling, boundary conditions
- Generates positive and negative test cases from code

### 4. Test Case Types
- **Positive Test Cases:** Validation rules, boundary conditions, happy path
- **Negative Test Cases:** Error handling, null checks, invalid inputs

## Output Format

The agent generates test cases in text format, which includes:

1. **Confluence Documentation References** - Relevant Confluence pages
2. **Codebase Analysis** - References found from the codebase
3. **Test Cases Extracted from Code** - Test scenarios extracted from code
4. **Standard Test Cases** - Standard test cases

## Example Output Structure

```
# Test Cases for Bug Verification

## Bug Description
[bug description]

## Confluence Documentation References
[relevant Confluence pages]

## Codebase Analysis
[code references]

## Test Cases Extracted from Code
### Positive Test Cases
[validation, boundary conditions]
### Negative Test Cases
[error handling, null checks]

## Test Cases
### Test Case 1: Verify Bug Reproduction
...
```

## Integration with Other Agents

TestCaseGeneratorAgent is integrated into AgentRegistry and can be used by other agents for consultation.

## Configuration

The agent uses:
- `test_cases/` directory to store test cases
- ReportingService for activity logging
- IntegrationService for GitLab/Jira integration

## Notes

- Confluence and codebase search must be performed by Cursor AI using MCP tools
- The agent works with data structures that Cursor AI must populate
- Test cases are stored in the `test_cases/` directory with timestamps

