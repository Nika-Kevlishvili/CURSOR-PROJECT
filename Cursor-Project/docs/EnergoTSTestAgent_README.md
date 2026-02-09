# EnergoTS Test Management Agent

## Overview

The **EnergoTSTestAgent** is a specialized agent for managing EnergoTS Playwright test automation framework. It provides comprehensive test management capabilities including studying, analyzing, copying, converting, and creating tests in the EnergoTS project.

## Features

### Core Capabilities

1. **Study Tests** - Analyze existing test files to understand their structure, fixtures, endpoints, and patterns
2. **Copy and Convert Tests** - Copy existing tests and convert them to new test scenarios with modifications
3. **Create New Tests** - Generate new tests following EnergoTS patterns and conventions
4. **Analyze Test Patterns** - Analyze patterns across all tests to understand common practices
5. **List Tests by Domain** - Organize and list tests by business domain

### Integration

- **MCP Servers**: Full access to all MCP servers (PostgreSQL, Confluence, etc.)
- **Integrated Sources**: Access to all integrated sources (codebase, documentation, etc.)
- **General Rules**: Follows all general rules:
  - IntegrationService integration (Rule 0.3)
  - PhoenixExpert consultation (Rule 0.4)
  - Report generation (Rule 0.6)
  - Code modification restrictions (Rule 0.8) - requires explicit user approval

## Usage

### Basic Usage

```python
from agents.Main import get_energo_ts_test_agent

# Get agent instance
agent = get_energo_ts_test_agent()

# Study a test
analysis = agent.study_test("tests/customers/customer.spec.ts")
print(analysis)

# List tests by domain
tests_by_domain = agent.list_tests_by_domain("billing")
print(tests_by_domain)

# Analyze test patterns
patterns = agent.analyze_test_patterns()
print(patterns)
```

### Study a Test

```python
# Study a specific test file
analysis = agent.study_test("tests/customers/customer.spec.ts")

# Returns:
# {
#     'test_path': 'tests/customers/customer.spec.ts',
#     'test_name': 'customer',
#     'domain': 'customers',
#     'jira_ids': ['REG-1', 'REG-2', 'REG-3'],
#     'test_structure': {...},
#     'fixtures_used': ['Request', 'GeneratePayload', 'Endpoints', 'Responses'],
#     'endpoints_used': ['customer'],
#     'payload_generators_used': ['customers.customer_private_business'],
#     'test_steps': [...],
#     'assertions': [...],
#     'dependencies': [...],
#     'analysis': '...'
# }
```

### Copy and Convert a Test

```python
# Copy and convert a test
result = agent.copy_and_convert_test(
    source_test_path="tests/customers/customer.spec.ts",
    target_test_path="tests/customers/customer_v2.spec.ts",
    conversion_rules={
        'change_jira_id': 'REG-999',
        'change_domain': 'customers',
        'modify_endpoints': {'customer': 'customer'},
        'modify_payloads': {'customer_private_business': 'customers.customer_legal'}
    }
)

# Check if file was written
if result['file_written']:
    print(f"File written to: {result['target_path']}")
else:
    print("File content generated but not written (check warnings)")
    print(result['new_test_content'])
```

### Create a New Test

```python
# Create a new test
result = agent.create_new_test(
    test_specification={
        'jira_id': 'REG-123',
        'test_name': 'Create Customer Test',
        'domain': 'customers',
        'description': 'Test customer creation',
        'fixtures': ['Request', 'GeneratePayload', 'Endpoints', 'Responses'],
        'endpoint': 'customer',
        'method': 'POST',
        'payload_generator': 'customers.customer_private_business',
        'tags': ['@customer']
    }
)

# Check if file was written
if result['file_written']:
    print(f"Test file created at: {result['test_path']}")
else:
    print("Test content generated but not written (check warnings)")
    print(result['test_content'])
```

### Analyze Test Patterns

```python
# Analyze patterns across all tests
patterns = agent.analyze_test_patterns()

# Returns:
# {
#     'common_fixtures': {'Request': 45, 'GeneratePayload': 45, ...},
#     'common_endpoints': {'customer': 10, 'billing': 15, ...},
#     'common_payloads': {'customers.customer_private_business': 5, ...},
#     'test_structure_patterns': [...],
#     'domain_statistics': {
#         'customers': {'test_count': 10, 'jira_ids': [...], ...},
#         'billing': {'test_count': 15, ...}
#     }
# }
```

## Agent Registration

The agent is automatically registered in the AgentRegistry when using `initialize_all_agents()`:

```python
from agents.Utils import initialize_all_agents

# Initialize all agents (includes EnergoTSTestAgent)
initialize_all_agents()
```

Or register manually:

```python
from agents.Core import get_agent_registry
from agents.Adapters import EnergoTSTestAgentAdapter

registry = get_agent_registry()
adapter = EnergoTSTestAgentAdapter()
registry.register_agent(adapter)
```

## Agent Router Integration

The agent can be automatically selected by AgentRouter for EnergoTS-related queries:

```python
from agents.Core import get_agent_router

router = get_agent_router()
result = router.route_query("Study the customer test in EnergoTS", {})
```

## Important Notes

### Code Modification (Rule 0.8.1 Exception)

**EXCEPTION TO RULE 0.8**: EnergoTSTestAgent has special permission to modify code files ONLY in `EnergoTS/tests/` directory.

- `study_test()` - ✅ Safe (read-only)
- `analyze_test_patterns()` - ✅ Safe (read-only)
- `list_tests_by_domain()` - ✅ Safe (read-only)
- `copy_and_convert_test()` - ✅ **CAN write files** to `EnergoTS/tests/` (Rule 0.8.1 exception)
- `create_new_test()` - ✅ **CAN write files** to `EnergoTS/tests/` (Rule 0.8.1 exception)

**Permission Scope:**
- ✅ **ALLOWED**: Creating/modifying `.spec.ts` files in `Cursor-Project/EnergoTS/tests/` directory
- ❌ **FORBIDDEN**: Modifying any files outside `EnergoTS/tests/` directory
- ❌ **FORBIDDEN**: Modifying any files in Phoenix project (Rule 0.8 still applies)
- ❌ **FORBIDDEN**: Modifying non-test files in EnergoTS project (fixtures, utils, configs, etc.)

**Default Behavior:**
- By default, `copy_and_convert_test()` and `create_new_test()` **automatically write files** to disk
- Set `write_to_disk=False` to only generate content without writing

**Important**: This exception applies ONLY to EnergoTSTestAgent. All other agents remain subject to Rule 0.8 (STRICTLY FORBIDDEN).

### IntegrationService (Rule 0.3)

The agent automatically calls `IntegrationService.update_before_task()` before all operations to update GitLab pipelines and Jira tickets.

### PhoenixExpert Consultation (Rule 0.4)

For complex queries, the agent can consult with PhoenixExpert through the AgentRegistry.

### Report Generation (Rule 0.6)

The agent automatically generates reports after all operations using ReportingService.

## File Structure

```
agents/
├── Main/
│   └── energo_ts_test_agent.py          # Main agent implementation
├── Adapters/
│   └── energo_ts_test_agent_adapter.py  # Agent adapter for registry
└── Utils/
    └── initialize_agents.py              # Auto-registration (updated)
```

## EnergoTS Test Framework

The agent understands the EnergoTS test framework structure:

- **Framework**: Playwright API testing
- **Language**: TypeScript
- **Structure**: Domain-driven test organization
- **Fixtures**: `baseFixture.ts` with Request, GeneratePayload, Responses, Endpoints, Nomenclatures
- **Test Pattern**: `test('[REG-XXX]: Test name', async ({ fixtures }) => { ... })`
- **Assertions**: `await expect(response).CheckResponse()`
- **Payloads**: Domain-segregated payload generators in `jsons/payloadGenerators/domains/`

## Examples

### Example 1: Study All Billing Tests

```python
agent = get_energo_ts_test_agent()

# List all billing tests
billing_tests = agent.list_tests_by_domain("billing")
print(f"Found {len(billing_tests.get('billing', []))} billing tests")

# Study each test
for test_path in billing_tests.get('billing', []):
    analysis = agent.study_test(test_path)
    print(f"\n{test_path}:")
    print(f"  Jira IDs: {analysis['jira_ids']}")
    print(f"  Fixtures: {analysis['fixtures_used']}")
```

### Example 2: Create Test Based on Existing Test

```python
agent = get_energo_ts_test_agent()

# Study existing test
source_analysis = agent.study_test("tests/customers/customer.spec.ts")

# Create new test based on specification
new_test = agent.create_new_test({
    'jira_id': 'REG-456',
    'test_name': 'Create Legal Customer',
    'domain': 'customers',
    'fixtures': source_analysis['fixtures_used'],
    'endpoint': 'customer',
    'method': 'POST',
    'payload_generator': 'customers.customer_legal',
    'tags': ['@customer']
})

# Review content (does NOT write to disk)
print(new_test['test_content'])
```

### Example 3: Analyze Common Patterns

```python
agent = get_energo_ts_test_agent()

# Analyze all test patterns
patterns = agent.analyze_test_patterns()

# Find most common fixtures
common_fixtures = sorted(
    patterns['common_fixtures'].items(),
    key=lambda x: x[1],
    reverse=True
)[:5]
print("Most common fixtures:")
for fixture, count in common_fixtures:
    print(f"  {fixture}: {count}")

# Find most common endpoints
common_endpoints = sorted(
    patterns['common_endpoints'].items(),
    key=lambda x: x[1],
    reverse=True
)[:5]
print("\nMost common endpoints:")
for endpoint, count in common_endpoints:
    print(f"  {endpoint}: {count}")
```

## Troubleshooting

### Import Errors

If you encounter import errors, ensure:
1. You're running from the `Cursor-Project` directory
2. Python path includes the project root
3. All dependencies are installed

### Agent Not Found

If the agent is not found in AgentRegistry:
1. Call `initialize_all_agents()` to register all agents
2. Or manually register: `registry.register_agent(EnergoTSTestAgentAdapter())`

### Test File Not Found

If a test file is not found:
1. Verify the test path is relative to `EnergoTS/tests/`
2. Or use absolute path
3. Check that the EnergoTS project exists at `Cursor-Project/EnergoTS/`

## Support

For issues or questions:
1. Check agent logs for detailed error messages
2. Verify EnergoTS project structure
3. Ensure all required dependencies are installed
4. Check that IntegrationService and ReportingService are properly configured
