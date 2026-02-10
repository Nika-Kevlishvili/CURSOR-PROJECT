# Jira Description Writer Agent

## Overview

The JiraDescriptionWriterAgent automatically analyzes EnergoTS test code and generates detailed Jira descriptions based on the test structure. It extracts all required objects, parameters, and test conditions, then writes them to Jira tickets using Jira MCP tools.

## Usage

### Basic Usage

```python
from agents.Main import get_jira_description_writer_agent

# Initialize agent
agent = get_jira_description_writer_agent()

# Write description from test file
result = agent.write_description_from_test_file(
    jira_id="REG-1037",
    test_file_path="Cursor-Project/EnergoTS/tests/billing/forVolumes/forVolumes.spec.ts",
    cloud_id="ad451d5c-7331-46f8-9a47-f51dc8e6bbde"  # Optional, will be fetched if not provided
)

if result['success']:
    print(f"Description written to {result['jira_id']}")
    print(f"Objects extracted: {result['objects_count']}")
else:
    print(f"Error: {result['error']}")
```

### With Cloud ID Fetching

```python
from agents.Main import get_jira_description_writer_agent
from mcp import mcp_Jira_getAccessibleAtlassianResources

# Get cloud ID
resources = mcp_Jira_getAccessibleAtlassianResources()
cloud_id = resources[0]['id'] if resources else None

# Initialize agent
agent = get_jira_description_writer_agent()

# Write description
result = agent.write_description_from_test_file(
    jira_id="REG-1037",
    test_file_path="Cursor-Project/EnergoTS/tests/billing/forVolumes/forVolumes.spec.ts",
    cloud_id=cloud_id
)
```

### Manual Description Generation

```python
from agents.Main import get_jira_description_writer_agent
from mcp import mcp_Jira_editJiraIssue

# Initialize agent
agent = get_jira_description_writer_agent()

# Analyze test code
with open("test_file.spec.ts", "r") as f:
    test_code = f.read()

analysis = agent.analyze_test_code(test_code, "REG-1037")
description = agent.generate_jira_description(analysis)

# Write to Jira manually
result = mcp_Jira_editJiraIssue(
    cloudId="ad451d5c-7331-46f8-9a47-f51dc8e6bbde",
    issueIdOrKey="REG-1037",
    fields={'description': description}
)
```

## Features

### Automatic Object Extraction

The agent automatically extracts:
- Customer objects and types
- Terms objects
- POD (Point of Delivery) objects with grid operators
- Meters with scale codes and tariffs
- Data by Scales with detailed table structures
- Price Components
- Products and Contracts
- Billing Runs and Invoices

### Parameter Detection

The agent detects:
- Grid operators
- Scale codes and tariffs
- Checkbox states (calculationForNumberOfDays)
- Zero amounts and multipliers
- Nomenclature prompts
- Test conditions

### Jira Markup Generation

The agent generates Jira markup formatted descriptions with:
- Headings (h2, h3)
- Bullet lists
- Proper formatting for nested structures
- Test conditions
- Expected behavior

## Method Reference

### `analyze_test_code(test_code: str, jira_id: str) -> Dict[str, Any]`

Analyzes test code and extracts objects, parameters, and test conditions.

**Parameters:**
- `test_code`: Test code as string
- `jira_id`: Jira ticket ID (e.g., REG-1037)

**Returns:**
- Dictionary with extracted information including objects, test conditions, and expected behavior

### `generate_jira_description(analysis: Dict[str, Any]) -> str`

Generates Jira markup formatted description from analysis.

**Parameters:**
- `analysis`: Analysis dictionary from `analyze_test_code()`

**Returns:**
- Jira markup formatted description string

### `write_description_to_jira(jira_id: str, test_code: str, cloud_id: Optional[str] = None) -> Dict[str, Any]`

Analyzes test code and prepares description for Jira (returns description, caller writes to Jira).

**Parameters:**
- `jira_id`: Jira ticket ID (e.g., REG-1037)
- `test_code`: Test code as string
- `cloud_id`: Jira cloud ID (optional)

**Returns:**
- Dictionary with description and metadata (caller should write to Jira using MCP tools)

### `write_description_from_test_file(jira_id: str, test_file_path: str, cloud_id: Optional[str] = None) -> Dict[str, Any]`

Reads test file, analyzes it, and prepares description for Jira.

**Parameters:**
- `jira_id`: Jira ticket ID (e.g., REG-1037)
- `test_file_path`: Path to test file
- `cloud_id`: Jira cloud ID (optional)

**Returns:**
- Dictionary with description and metadata (caller should write to Jira using MCP tools)

## Integration with Rules

### Rule 0.3 - IntegrationService

The agent automatically calls `IntegrationService.update_before_task()` before writing descriptions.

### Rule 0.6 - Report Generation

The agent automatically generates reports after operations using `ReportingService`.

### Rule 34 - Agent Organization

The agent is properly organized in `agents/Main/` directory with adapter in `agents/Adapters/`.

## Example Output

The agent generates descriptions like:

```
h2. Test Case Description

This test verifies splitting logic when scale codes total amount is zero and "Number or days" checkbox is not selected.

h2. Required Objects

h3. 1. Customer
* Type: Legal customer

h3. 2. Terms
* Standard terms object

h3. 3. POD (Point of Delivery)
* Type: Settlement POD
* Grid Operator: 'splitting'

...

h2. Test Conditions
* Scale codes total amount is 0 (multiplier = "0", totalVolumes = "0" in both scale code rows)
* "Number or days" checkbox is NOT selected (calculationForNumberOfDays: false) in scale code and tariff nomenclatures
* Grid operator: 'splitting'

h2. Expected Behavior

The test verifies that splitting logic works correctly when scale codes total amount is zero and "Number or days" checkbox is not selected.
```

## Notes

- The agent expects test code to follow EnergoTS test patterns
- Test steps should use `test.step()` with descriptive names
- The agent extracts information from test step names and code patterns
- Cloud ID can be obtained using `mcp_Jira_getAccessibleAtlassianResources()`
- The agent returns descriptions ready to write to Jira, but actual writing should be done by caller using MCP tools
