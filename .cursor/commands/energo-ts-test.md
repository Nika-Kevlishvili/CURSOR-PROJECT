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
2. **Read Jira Task** - ALWAYS read Jira task title and description BEFORE creating test
3. **Clarify Requirements** - Ask clarifying questions if test requirements are unclear
4. **PhoenixExpert Consultation** - Consult PhoenixExpert if needed for API/business logic understanding (Rule 0.4)
5. **EnergoTSTestAgent** - Use EnergoTSTestAgent for all test operations ONLY after full understanding
6. **Report Generation** - Generate reports after task completion (Rule 0.6)

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

## Test Creation Workflow [CRITICAL - MANDATORY]

**ABSOLUTE REQUIREMENT**: Before creating ANY test, you MUST follow this workflow:

### Step 1: Read Jira Task Information
- **ALWAYS** read Jira task title and description FIRST
- Use MCP Jira tools to fetch task details: `mcp_Jira_getJiraIssue(cloudId, issueIdOrKey)`
- Extract: task title, description, acceptance criteria, business requirements

### Step 2: Understand Requirements
- Analyze task description to understand what needs to be tested
- Identify: endpoints, HTTP methods, payloads, expected behavior, edge cases
- Determine: domain, fixtures needed, test steps required

### Step 3: Ask Clarifying Questions (if needed)
- **IF** you cannot determine exactly what needs to be tested from task description
- **THEN** ask clarifying questions BEFORE starting test creation:
  - What endpoint should be tested?
  - What HTTP method (POST, GET, PUT, DELETE)?
  - What is the expected behavior?
  - Are there specific test scenarios or edge cases?
  - What domain does this belong to?
  - What fixtures are needed?
  - Are there prerequisite steps (create customer, POD, contract, etc.)?

### Step 4: Start Test Creation
- **ONLY** after you have complete understanding of requirements
- **ONLY** after all clarifying questions are answered
- Use exact Jira task title as test name (see Test Naming Rule below)

### Example Workflow:
```
User: "Create test for REG-1027"
→ Step 1: Read Jira task REG-1027 (title + description)
→ Step 2: Analyze requirements
→ Step 3: If unclear → Ask: "What endpoint should be tested? What is the expected behavior?"
→ Step 4: After clarification → Create test with exact task title
```

## Test Naming Rule [CRITICAL]

**ABSOLUTE REQUIREMENT**: Test name MUST ALWAYS be EXACTLY the same as the Jira task title.

- When creating a test from Jira ticket (e.g., REG-1027):
  - **Jira Task Title**: "For Volumes - Contract/Customer/POD Level"
  - **Test Name**: `[REG-1027]: For Volumes - Contract/Customer/POD Level`
  - **NO modifications** to the task title
  - **NO additions** like "| Happy path" unless explicitly in the task title
  - **NO abbreviations** or simplifications

- Format: `test('[REG-XXX]: {Exact Jira Task Title}', async ({...}) => {`

- Example:
  ```typescript
  // ✅ CORRECT - Exact match with Jira task title
  test('[REG-1027]: For Volumes - Contract/Customer/POD Level', async ({...}) => {
  
  // ❌ WRONG - Modified title
  test('[REG-1027]: For Volumes | Happy path', async ({...}) => {
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
