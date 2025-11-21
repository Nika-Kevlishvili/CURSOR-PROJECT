# TestAgent Consultation Process Documentation

## Overview

TestAgent **ALWAYS** consults with PhoenixExpert agent before executing ANY test. This ensures that all tests are built with accurate information from the Phoenix codebase and Confluence documentation.

## When Consultation Happens

Consultation occurs **EVERY TIME** a test is executed, regardless of test type:
- ✅ API Tests
- ✅ UI Tests  
- ✅ Integration Tests
- ✅ E2E Tests
- ✅ Custom Tests

## Consultation Process Flow

### Step 1: Test Task Received
```
User: "Test customer create endpoint"
TestAgent: Receives task description
```

### Step 2: Consultation Decision
```
TestAgent._should_consult_agents()
  → ALWAYS returns True
  → Logs: "Consultation rule - ALWAYS consulting PhoenixExpert for all tests"
```

### Step 3: Context Extraction
```
TestAgent._extract_consultation_context()
  → Extracts from task description:
    - endpoint_path: "/api/customer"
    - method: "POST"
    - domain: "customer"
    - controller: "customer-controller"
    - operation: "create"
    - test_type: "api"
    - base_url: "http://localhost:8080"
```

### Step 4: Consultation Request
```
TestAgent._consult_other_agents()
  → Calls: AgentRegistry.consult_best_agent(task_description, context)
  → AgentRegistry finds PhoenixExpert as best match
  → PhoenixExpertAdapter.consult() is called
```

### Step 5: PhoenixExpert Processing
```
PhoenixExpert.answer_question()
  → Searches Phoenix codebase for:
    - Customer controller files
    - CreateCustomerRequest model
    - Validation annotations
    - Permission requirements
  → Searches Confluence for:
    - Customer API documentation
    - Business rules
  → Returns structured information
```

### Step 6: Response Processing
```
TestAgent receives response:
  {
    "success": true,
    "agent": "PhoenixExpert",
    "response": {
      "information": {
        "endpoint": [
          {
            "path": "/api/customer",
            "method": "POST",
            "parameters": [...],
            "requestBody": {...}
          }
        ],
        "domain": {
          "name": "customer",
          "controllers": [...],
          "permissions": ["CUSTOMER_CREATE", ...]
        },
        "controller": {
          "name": "customer-controller",
          "endpoints": [...]
        }
      },
      "phoenix_answer": {
        "answer": "Found relevant files...",
        "sources": {
          "code": ["CustomerService.java", "CreateCustomerRequest.java", ...],
          "confluence": ["Customer API", ...]
        }
      },
      "sources": {
        "code_files": [...],
        "confluence": [...]
      }
    }
  }
```

### Step 7: Test Execution
```
TestAgent uses expert information:
  → Builds test request with required fields
  → Validates against expert's validation rules
  → Checks permissions
  → Executes test
  → Reports results with consultation details
```

## Detailed Logging Output

When consultation happens, you'll see output like this:

```
======================================================================
TestAgent: [CONSULTATION PROCESS STARTING]
======================================================================
TestAgent: Task: Test customer create endpoint
TestAgent: Test Type: api
TestAgent: Consultation Time: 2025-11-21 16:30:45
TestAgent: Consulting with PhoenixExpert agent...
----------------------------------------------------------------------
TestAgent: Extracted Context:
  - test_type: api
  - base_url: http://localhost:8080
  - endpoint_path: /api/customer
  - method: POST
  - domain: customer
  - controller: customer-controller
  - operation: create
----------------------------------------------------------------------
TestAgent: [Step 1/3] Extracting consultation context...
TestAgent: [Step 2/3] Sending consultation request to AgentRegistry...
TestAgent: Query: Test customer create endpoint...
TestAgent: Context keys: ['test_type', 'base_url', 'endpoint_path', 'method', 'domain', 'controller', 'operation']
TestAgent: [Step 3/3] Processing consultation response...
TestAgent: ✓ Consultation successful
TestAgent: Agent used: PhoenixExpert
TestAgent: Consultation Duration: 0.45 seconds
----------------------------------------------------------------------
TestAgent: ✓ Successfully consulted with PhoenixExpert agent
TestAgent: ✓ Received information from agent consultation
TestAgent: Consultation Response Structure:
  - Information keys: ['endpoint', 'domain', 'controller']
  - Endpoints found: 1
    [1] POST /api/customer
  - Domain info: {'name': 'customer', ...}
  - Controller info: {'name': 'customer-controller', ...}
  - Phoenix Answer: Found 5 relevant files in Phoenix codebase...
  - Code files found: 5
  - Confluence pages found: 2
TestAgent: ✓ Consultation information will be used for test execution
======================================================================
TestAgent: [CONSULTATION PROCESS COMPLETED]
======================================================================
```

## What Information is Extracted

### From Task Description:
- **Endpoint Path**: Extracted from description or inferred (e.g., "/api/customer")
- **HTTP Method**: Detected from keywords (create→POST, edit→PUT, view→GET, delete→DELETE)
- **Domain**: Identified from context (customer, billing, contract, etc.)
- **Controller**: Mapped from domain (customer → customer-controller)
- **Operation**: Extracted from keywords (create, edit, view, delete, validation, permission)

### From PhoenixExpert:
- **Endpoint Details**: Exact path, method, parameters, request body structure
- **Validation Rules**: Required fields, field constraints, business rules
- **Permissions**: Required permissions for the operation
- **Request Models**: Structure of CreateCustomerRequest, EditCustomerRequest, etc.
- **Response Models**: Structure of CustomerResponse, etc.
- **Code References**: Relevant Java files, service methods, repositories
- **Documentation**: Confluence pages with API documentation

## Example: Customer Create Test

### Input:
```
Task: "Test customer create endpoint"
```

### Consultation Process:
1. **Context Extracted**:
   ```python
   {
     "endpoint_path": "/api/customer",
     "method": "POST",
     "domain": "customer",
     "controller": "customer-controller",
     "operation": "create"
   }
   ```

2. **PhoenixExpert Searches**:
   - `CreateCustomerRequest.java` - Finds required fields
   - `CustomerService.java` - Finds business logic
   - `CustomerController.java` - Finds endpoint definition
   - Confluence: "Customer API" page

3. **Information Returned**:
   - Required fields: customerType, customerIdentifier, foreign, marketingConsent, address
   - Validation: customerIdentifier length 1-17, customerDetailStatus cannot be POTENTIAL
   - Permissions: CUSTOMER_CREATE required
   - Endpoint: POST /api/customer

4. **Test Built**:
   ```python
   test_data = {
     "customerType": "PRIVATE",
     "customerIdentifier": "AUTOMATION20251121163045",
     "foreign": False,
     "marketingConsent": False,
     "customerDetailStatus": "ACTIVE",
     "address": {
       "countryId": 1,
       "regionId": 1,
       "municipalityId": 1,
       "settlementId": 1,
       "street": "Test Street",
       "streetNumber": "1"
     }
   }
   ```

5. **Test Executed**:
   - POST /api/customer with test_data
   - Validates response
   - Reports results with consultation details

## Benefits of Always Consulting

1. **Accuracy**: Tests use real endpoint information from codebase
2. **Validation**: Tests follow actual validation rules
3. **Permissions**: Tests check correct permission requirements
4. **Maintainability**: When code changes, tests automatically use updated information
5. **Documentation**: Tests serve as documentation of actual API behavior

## Configuration

Consultation can be disabled in TestAgent configuration:
```python
agent = TestAgent(
    base_url="http://localhost:8080",
    config={'enable_agent_consultation': False}  # Disables consultation
)
```

**Note**: Disabling consultation is NOT recommended as it reduces test accuracy.

## Troubleshooting

### Consultation Returns None
- Check if AgentRegistry is initialized
- Check if PhoenixExpert is registered
- Check if consultation is enabled in config

### Consultation Takes Too Long
- PhoenixExpert searches entire codebase
- Consider caching consultation results for repeated tests
- Large codebases may take 1-2 seconds

### No Information Received
- Check if PhoenixExpert found relevant files
- Verify endpoint path is correct
- Check if domain/controller names match codebase

## Summary

**Every test execution follows this process:**
1. ✅ Extract context from task
2. ✅ Consult PhoenixExpert with context
3. ✅ Receive structured information
4. ✅ Build test using expert information
5. ✅ Execute test
6. ✅ Report results with consultation details

This ensures all tests are accurate, maintainable, and reflect the actual Phoenix codebase behavior.

