# TestAgent - Automated Testing Agent

## Overview

TestAgent არის ავტომატიზირებული ტესტირების აგენტი, რომელიც იღებს დავალებებს და ასრულებს ტესტებს. ის მხარს უჭერს სხვადასხვა ტიპის ტესტებს: API, UI, Integration და E2E.

TestAgent is an automated testing agent that receives tasks and executes tests. It supports multiple test types: API, UI, Integration, and E2E.

## Features

- ✅ **Multiple Test Types**: API, UI, Integration, E2E, Custom
- ✅ **Auto-detection**: Automatically detects test type from task description
- ✅ **Postman Support**: Execute Postman collections
- ✅ **Playwright Support**: Execute Playwright UI tests
- ✅ **REST API Testing**: Direct API endpoint testing
- ✅ **Test Reports**: Detailed JSON reports with execution history
- ✅ **Autonomous Execution**: Executes tests without user confirmation
- ✅ **Agent Consultation**: Automatically consults with other agents (PhoenixExpert) when needed
- ✅ **Intelligent Information Gathering**: Uses agent consultation to enhance test execution

## Installation

### Prerequisites

```bash
# Install Python dependencies
pip install requests

# For Postman collections (optional)
npm install -g newman

# For Playwright tests (optional)
npm install -g @playwright/test
```

## Usage

### Basic Usage

```python
from agents import get_test_agent

# Initialize agent
agent = get_test_agent(base_url="http://localhost:8080")

# Execute a task
result = agent.execute_task("Test GET /api/customers endpoint")

print(f"Status: {result['status']}")
print(f"Summary: {result['summary']}")
```

### Examples

#### API Testing

```python
agent = get_test_agent(base_url="http://localhost:8080")

# Test GET endpoint
result = agent.execute_task("Test GET /api/customers")

# Test POST endpoint
result = agent.execute_task("Test POST /api/customers with customer data")

# Test with specific method
result = agent.execute_task("Test PUT /api/customers/123")
```

#### UI Testing

```python
agent = get_test_agent()

# Execute Playwright test
result = agent.execute_task("Test login page UI with Playwright")

# Test navigation
result = agent.execute_task("Test navigation flow in browser")
```

#### Postman Collections

```python
agent = get_test_agent()

# Execute Postman collection
result = agent.execute_task("Run Postman collection for billing API")
```

#### Explicit Test Type

```python
from agents import get_test_agent, TestType

agent = get_test_agent()

# Explicitly specify test type
result = agent.execute_task(
    "Test customer API",
    test_type=TestType.API
)
```

### Task Descriptions

TestAgent automatically detects test type from task description:

- **API Tests**: Keywords like "api", "endpoint", "rest", "http", "request", "postman"
- **UI Tests**: Keywords like "ui", "browser", "playwright", "selenium", "page", "click", "navigate"
- **Integration Tests**: Keywords like "integration", "service", "component"
- **E2E Tests**: Keywords like "e2e", "end-to-end", "full flow", "complete flow"

### Execution History

```python
agent = get_test_agent()

# Execute some tasks
agent.execute_task("Test GET /api/customers")
agent.execute_task("Test POST /api/customers")

# Get execution history
history = agent.get_execution_history()
print(f"Total executions: {len(history)}")

# Get last execution
last = agent.get_last_execution()
if last:
    print(f"Last execution ID: {last['execution_id']}")
    print(f"Status: {last['status']}")
```

## Test Results

Test results are automatically saved to `test_results/` directory:

```
test_results/
├── TEST_20240101_120000.json
├── TEST_20240101_120100.json
└── ...
```

Each report contains:
- Execution ID
- Task description
- Test type
- Status (passed/failed/skipped/error)
- Start/end time
- Detailed results
- Summary (total, passed, failed, skipped, errors)

## Test Types

### TestType Enum

- `TestType.API` - API/REST endpoint tests
- `TestType.UI` - UI/browser tests
- `TestType.INTEGRATION` - Integration tests
- `TestType.E2E` - End-to-end tests
- `TestType.CUSTOM` - Custom test scripts

### TestStatus Enum

- `TestStatus.PASSED` - Test passed
- `TestStatus.FAILED` - Test failed
- `TestStatus.SKIPPED` - Test skipped
- `TestStatus.ERROR` - Test error
- `TestStatus.RUNNING` - Test running

## Configuration

```python
config = {
    'timeout': 30,  # Request timeout in seconds
    'retry_count': 3,  # Number of retries
    # Add more configuration as needed
}

agent = get_test_agent(
    base_url="http://localhost:8080",
    config=config
)
```

## Directory Structure

```
.
├── test_agent.py                      # Main TestAgent class
├── test_agent_example.py              # Usage examples
├── test_agent_consultation_example.py # Agent consultation examples
├── agent_registry.py                  # Agent registry for managing agents
├── phoenix_expert_adapter.py          # PhoenixExpert adapter for Agent interface
├── README_TEST_AGENT.md               # This file
├── test_results/                      # Test execution reports
│   └── TEST_*.json
├── test_cases/                        # Test case definitions
└── tests/                             # Test files
    ├── playwright/                   # Playwright tests
    └── postman/                      # Postman collections
```

## Examples

See `test_agent_example.py` for complete usage examples.

## Integration with Phoenix Project

TestAgent can be integrated with Phoenix project:

```python
from agents import get_test_agent

# Initialize with Phoenix base URL
agent = get_test_agent(base_url="https://phoenix-api.example.com")

# Test Phoenix endpoints
agent.execute_task("Test GET /api/phoenix/customers")
agent.execute_task("Test billing run API")
agent.execute_task("Test customer creation flow")
```

## Agent Consultation

TestAgent can automatically consult with other agents (like PhoenixExpert) when executing tasks:

### How It Works

1. **Automatic Detection**: TestAgent automatically detects when it needs information from other agents
2. **Consultation**: When a task mentions Phoenix-related terms, endpoints, architecture, etc., TestAgent consults with PhoenixExpert
3. **Information Processing**: TestAgent processes the information received from other agents
4. **Enhanced Execution**: Uses the information to enhance test execution (e.g., finding correct endpoints)

### Example

```python
from agents import get_test_agent

agent = get_test_agent()

# This task will automatically consult PhoenixExpert about the endpoint
result = agent.execute_task("Test GET /api/customers endpoint - what information does PhoenixExpert have?")

# Check consultation results
consultation = result.get('agent_consultation')
if consultation and consultation.get('success'):
    print(f"Consulted with: {consultation.get('agent')}")
    print(f"Information: {consultation.get('response')}")
```

### Consultation Triggers

TestAgent consults with other agents when tasks contain:
- Phoenix-related terms (phoenix, endpoint, api, controller, domain, billing, customer)
- Questions about architecture or codebase
- Requests for information (how does, what is, explain, information about)

### Consultation History

```python
# Get consultation history
history = agent.get_consultation_history()
for consultation in history:
    print(f"Consulted {consultation['to_agent']} about: {consultation['query']}")
```

## Autonomous Execution

TestAgent executes tests autonomously without asking for confirmation:

- ✅ Automatically detects test type
- ✅ Executes tests immediately
- ✅ Generates reports automatically
- ✅ Handles errors gracefully
- ✅ No user interaction required
- ✅ Automatically consults with other agents when needed

## Future Enhancements

- [ ] Support for more test frameworks (Selenium, Cypress, etc.)
- [ ] Test data generation
- [ ] Parallel test execution
- [ ] Test scheduling
- [ ] Integration with CI/CD pipelines
- [ ] Visual test reports (HTML)
- [ ] Test coverage analysis

## Status

✅ TestAgent created
✅ API testing support
✅ UI testing support (Playwright)
✅ Postman collection support
✅ Test reporting
✅ Execution history
✅ Ready for use

