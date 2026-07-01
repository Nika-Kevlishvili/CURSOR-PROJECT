# Postman Write Access Rules

## Rules Enforced

### 1. Collection Creation
- ✅ Can create new Collections
- ✅ Can duplicate existing Collections
- ✅ All modifications go to new/duplicated Collections only
- ✅ Original Collections remain untouched

### 2. Collection Modification
- ❌ Never modifies original Collections
- ❌ Never deletes original Collections (unless explicitly instructed with confirmation)
- ✅ Only modifies new or duplicated Collections
- ✅ Preserves original Collections intact

### 3. Automated Flows
- ✅ All automated flows generated in new Collections
- ✅ Uses code-first logic from Swagger and ProjectKnowledgeManager
- ✅ Endpoints, validations, and sequences defined from architecture

### 4. Collection Mapping
- ✅ Maintains mapping of new Collections to original ones
- ✅ Mapping stored in `postman_collections/collection_mapping.json`
- ✅ Tracks creation timestamp and type (new/duplicate)

### 5. Execution
- ✅ Only runs/executes flows in new Collections
- ✅ Never executes or alters original Collections

### 6. Autonomous Operation
- ✅ No confirmation needed for creating/updating new Collections
- ✅ Acts autonomously
- ✅ Confirmation required only for deleting original Collections

## PhoenixExpert Capabilities (Read-Only)

### Query Postman Information
```python
from agents import get_phoenix_expert

expert = get_phoenix_expert()
response = expert.answer_question("What Postman collections exist?")
```

### Get Architecture Information
```python
expert = get_phoenix_expert()
endpoint_info = expert.get_endpoint_info("/billing-runs", "POST")
domain_info = expert.get_domain_info("billing")
```

**Note**: All operations are READ-ONLY. PostmanManager has been deleted.

## File Structure

```
postman_collections/
├── collection_mapping.json          # Maps new -> original collections
├── {collection_name}.postman_collection.json  # New collections
└── ...
```

## Integration

- **PhoenixExpert**: Provides read-only access to architecture data and answers questions about Postman
- **All Managers Deleted**: PostmanManager, TestManager, AutomationManager, ProjectKnowledgeManager no longer exist

## Status

❌ All managers deleted
✅ PhoenixExpert active (READ-ONLY mode)
✅ Architecture data available for Q&A
❌ No modifications, executions, or flow creation allowed
✅ Code-first approach maintained (read-only)

