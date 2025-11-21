# Postman Collection Generator

## Overview

Postman Collection Generator ავტომატურად ქმნის Postman collection-ებს სხვადასხვა test case-ებით და ატვირთავს მათ Postman workspace-ში.

Postman Collection Generator automatically generates Postman collections with various test cases and uploads them to Postman workspace.

## Features

- ✅ **Automatic Collection Generation**: Creates Postman collections with multiple test cases
- ✅ **POD Create Test Cases**: Generates comprehensive test cases for POD (Point of Delivery) create endpoint
- ✅ **Postman API Integration**: Automatically uploads collections to Postman workspace
- ✅ **Local Storage**: Saves collections locally for backup
- ✅ **Test Scripts**: Includes Postman test scripts for validation

## Configuration

### Environment Variables

```bash
# Postman API Key (required for upload)
export POSTMAN_API_KEY="your-postman-api-key"

# Postman Workspace ID (required for upload)
export POSTMAN_WORKSPACE_ID="your-workspace-id"
```

### Configuration via Code

```python
from agents import get_test_agent

config = {
    'postman_api_key': 'your-api-key',
    'postman_workspace_id': 'your-workspace-id'
}

agent = get_test_agent(base_url="http://localhost:8080", config=config)
```

## Usage

### Generate POD Create Collection

```python
from agents import get_test_agent

# Initialize agent
agent = get_test_agent(base_url="http://localhost:8080")

# Generate and upload POD create collection
result = agent.generate_postman_collection(
    collection_type="pod_create",
    collection_name="POD Create - Automation Tests",
    upload=True  # Upload to Postman workspace
)

if result['success']:
    print(f"Collection created: {result['file_path']}")
    if result.get('upload_result') and result['upload_result'].get('success'):
        print(f"Collection URL: {result['upload_result']['url']}")
```

### Generate Without Upload

```python
# Generate collection but don't upload
result = agent.generate_postman_collection(
    collection_type="pod_create",
    upload=False
)

# Collection saved locally at: result['file_path']
```

### Direct Generator Usage

```python
from agents import get_postman_collection_generator

generator = get_postman_collection_generator()

# Generate collection
collection = generator.generate_pod_create_collection(
    base_url="http://localhost:8080",
    collection_name="My POD Collection"
)

# Save locally
file_path = generator.save_collection_locally(collection)

# Upload to Postman
upload_result = generator.upload_to_postman(collection)
```

## POD Create Test Cases

The generator creates the following test cases:

### Success Cases

1. **Basic Consumer** - Successful POD creation with minimal required fields for CONSUMER type
2. **Generator** - Successful POD creation for GENERATOR type
3. **With SLP** - Successful POD creation with Smart Load Profile enabled
4. **With Settlement Period** - Successful POD creation with settlement period enabled
5. **Non-Household Consumer** - Successful POD creation for NON_HOUSEHOLD consumption purpose

### Validation Error Cases

6. **Missing Required Fields** - Should fail with 400 when required fields are missing
7. **Invalid Identifier Format** - Should fail with 400 when identifier contains invalid characters
8. **Identifier Too Long** - Should fail with 400 when identifier exceeds max length (33)
9. **Invalid Consumption Value** - Should fail with 400 when estimatedMonthlyAvgConsumption is out of range
10. **Duplicate Identifier** - Should fail with 400 when identifier already exists

## Test Case Structure

Each test case includes:

- **Request**: POST request to `/api/pod` endpoint
- **Headers**: Content-Type: application/json
- **Body**: JSON body with POD create request data
- **Test Scripts**: Postman test scripts for validation
  - Status code validation
  - Response structure validation
  - Response time validation
  - Error message validation (for error cases)

## Collection Structure

```json
{
  "info": {
    "name": "POD Create - Test Cases",
    "description": "Automated test cases for POD Create endpoint",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. POD Create - Success - Basic Consumer",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/pod",
        "body": { ... }
      },
      "event": [
        {
          "listen": "test",
          "script": { ... }
        }
      ]
    },
    ...
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    }
  ]
}
```

## Postman API Key Generation

1. Go to [Postman API Keys](https://go.postman.co/settings/me/api-keys)
2. Click "Generate API Key"
3. Copy the API key
4. Set as `POSTMAN_API_KEY` environment variable

## Workspace ID

1. Open Postman
2. Go to your workspace
3. The workspace ID is in the URL: `https://app.getpostman.com/workspace/{workspace-id}/...`
4. Or use Postman API to list workspaces:
   ```bash
   curl -X GET https://api.getpostman.com/workspaces \
     -H "X-Api-Key: your-api-key"
   ```

## File Locations

- **Generated Collections**: `postman/postman_collections/`
- **Collection Format**: `{collection_name}.postman_collection.json`

## Example Output

```
======================================================================
PostmanCollectionGenerator: Generating POD Create Collection
======================================================================
Collection Name: POD Create - Test Cases - 20250101_120000
Base URL: http://localhost:8080
----------------------------------------------------------------------
PostmanCollectionGenerator: ✓ Generated 11 test cases
======================================================================

PostmanCollectionGenerator: ✓ Collection saved locally: postman/postman_collections/POD_Create_-_Test_Cases_-_20250101_120000.postman_collection.json

======================================================================
PostmanCollectionGenerator: Uploading Collection to Postman
======================================================================
Collection: POD Create - Test Cases - 20250101_120000
Workspace ID: your-workspace-id
----------------------------------------------------------------------
PostmanCollectionGenerator: ✓ Collection uploaded successfully
Collection ID: 12345678-1234-1234-1234-123456789abc
Collection URL: https://app.getpostman.com/collection/12345678-1234-1234-1234-123456789abc
======================================================================
```

## Error Handling

- **Missing API Key**: Collection is generated and saved locally, but upload is skipped
- **Invalid Workspace ID**: Upload fails with error message
- **Network Errors**: Upload fails gracefully, collection still saved locally
- **Validation Errors**: All errors are logged with detailed messages

## Integration with TestAgent

The Postman collection generator is automatically available in TestAgent:

```python
from agents import get_test_agent

agent = get_test_agent()

# Generate collection
result = agent.generate_postman_collection("pod_create")
```

## Future Enhancements

- Support for other collection types (Customer, Billing, etc.)
- Custom test case templates
- Integration with PhoenixExpert for endpoint discovery
- Automatic test case generation from Swagger/OpenAPI specs

