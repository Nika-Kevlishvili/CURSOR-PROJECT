# EnergoTS Test Management Command

Route EnergoTS test-related queries to EnergoTSTestAgent (Rule 0.8.1 exception for test file modifications).

## When to Use

Use this command when the user asks about:
- EnergoTS tests
- Creating new tests in EnergoTS
- Modifying tests in EnergoTS
- Copying/converting tests
- Analyzing test patterns
- Studying test files
- Test management in EnergoTS project

## Mandatory Workflow:

1. **IntegrationService** - Call `IntegrationService.update_before_task()` FIRST (Rule 0.3)
2. **PhoenixExpert Consultation** - Consult PhoenixExpert if needed for API/business logic understanding (Rule 0.4)
3. **EnergoTSTestAgent** - Use EnergoTSTestAgent for all test operations
4. **Report Generation** - Generate reports after task completion (Rule 0.6)

## Available Operations

### Study Test
```python
from agents.Main import get_energo_ts_test_agent
agent = get_energo_ts_test_agent()
analysis = agent.study_test("tests/customers/customer.spec.ts")
```

### Create New Test
```python
result = agent.create_new_test({
    'jira_id': 'REG-123',
    'test_name': 'Create Customer',
    'domain': 'customers',
    'fixtures': ['Request', 'GeneratePayload', 'Endpoints'],
    'endpoint': 'customer',
    'method': 'POST',
    'payload_generator': 'customers.customer_private_business'
})
```

### Copy and Convert Test
```python
result = agent.copy_and_convert_test(
    source_test_path="tests/customers/customer.spec.ts",
    target_test_path="tests/customers/customer_v2.spec.ts",
    conversion_rules={'change_jira_id': 'REG-999'}
)
```

### Analyze Patterns
```python
patterns = agent.analyze_test_patterns()
```

### List Tests by Domain
```python
tests = agent.list_tests_by_domain("billing")
```

## Permissions (Rule 0.8.1)

**EXCEPTION TO RULE 0.8**: EnergoTSTestAgent can modify code files ONLY in `Cursor-Project/EnergoTS/tests/` directory.

- ✅ **ALLOWED**: Create/modify `.spec.ts` files in `EnergoTS/tests/`
- ❌ **FORBIDDEN**: Modify files outside `EnergoTS/tests/`
- ❌ **FORBIDDEN**: Modify Phoenix project files
- ❌ **FORBIDDEN**: Modify non-test files in EnergoTS

## Response Requirements

- State "**Agent:** EnergoTSTestAgent" at beginning
- Provide comprehensive answer with file paths and operations performed
- End with: "Agents involved: EnergoTSTestAgent" (and PhoenixExpert if consulted)

## Generate Reports (Rule 0.6)

- Save to `Cursor-Project/reports/YYYY-MM-DD/EnergoTSTestAgent_{HHMM}.md`
- Save summary to `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`

## Examples

**User**: "Create a new test for customer creation in EnergoTS"
**Action**: Use EnergoTSTestAgent.create_new_test() with customer specification

**User**: "Study the billing test in EnergoTS"
**Action**: Use EnergoTSTestAgent.study_test() with billing test path

**User**: "Copy the customer test and change Jira ID to REG-999"
**Action**: Use EnergoTSTestAgent.copy_and_convert_test() with conversion rules
