# Autonomous Rules

## GLOBAL RULES - ALL AGENTS MUST FOLLOW

### Rule 1: GitHub Operations Require Explicit Permission

**ABSOLUTE REQUIREMENT**: NO agent can make ANY changes to GitHub (push, commit, merge, create branch, etc.) until explicit permission is granted by the user.

#### What Requires Permission:
- Git push operations
- Git commit operations
- Git merge operations
- Creating/deleting branches
- Creating pull requests
- Any GitHub repository modifications
- Git remote operations that modify GitHub

#### How Permission Works:
1. **Automatic Detection**: The system automatically detects GitHub-related operations in queries
2. **Permission Check**: Before any GitHub operation, agents MUST check with `GlobalRules.check_github_permission()`
3. **Blocking**: If permission is not granted, the operation is BLOCKED and the user is notified
4. **Permission Grant**: User must explicitly grant permission using `GlobalRules.grant_github_permission()`

#### Implementation:
```python
from agents.global_rules import get_global_rules

global_rules = get_global_rules()
permission = global_rules.check_github_permission(operation="push to GitHub")
if not permission['permitted']:
    # BLOCK operation and notify user
    return {'error': permission['message']}
```

#### User Commands:
- To grant permission: "Grant GitHub permission" or "Allow GitHub operations"
- To revoke permission: "Revoke GitHub permission" or "Block GitHub operations"

### Rule 2: Automatic Agent Routing

**ABSOLUTE REQUIREMENT**: When a user makes any request or question, Cursor MUST automatically:
1. Analyze the query to determine intent
2. Identify which agent(s) are most competent for the task
3. Route the query to the appropriate agent(s)
4. Combine responses from multiple agents when needed

#### How Automatic Routing Works:

1. **Query Analysis**: 
   - AgentRouter analyzes the user's query
   - Determines primary intent (test, question, documentation, etc.)
   - Calculates confidence scores

2. **Agent Selection**:
   - Evaluates all registered agents
   - Calculates competence scores for each agent
   - Selects the most competent agent(s)

3. **Routing**:
   - Single agent: Routes directly to the best agent
   - Multiple agents: Orchestrates multiple agents and combines their responses

4. **Response Combination**:
   - When multiple agents are used, intelligently combines their responses
   - Prioritizes responses by agent competence scores
   - Provides unified result

#### Implementation:
```python
from agents.agent_router import get_agent_router

router = get_agent_router()
result = router.route_query(user_query, context)
# Automatically routes to best agent(s) and returns combined result
```

#### Agent Competence Evaluation:
- Agent capabilities matching query keywords
- Agent name matching detected intent
- Agent's `can_help_with()` method result
- Historical performance (if available)

#### Multi-Agent Orchestration:
- When a query requires multiple agents, they are consulted in parallel
- Responses are combined with the highest-scoring agent as primary
- Supplementary responses from other agents are included

## Global Behavior

- Code is authoritative, Confluence is read-only.

- PhoenixExpert may read code and Confluence, but never modify either.

- All knowledge must come from code first, Confluence is supplementary.

- PhoenixExpert operates fully autonomously without asking for confirmation.

- Prompts from the user are the only inputs; PhoenixExpert answers based on indexed knowledge.

- All operations are READ-ONLY - no modifications, commits, pushes, merges, deletes, or executions.

## PhoenixExpert Agent

- **Role**: Specialized Q&A agent for the Phoenix project.

- **Access**: READ-ONLY access everywhere.

- **Priority Rule**: 
  - Source of truth hierarchy:
    1. Phoenix code (primary)
    2. Confluence (secondary)
  - If code and Confluence contradict, always use code.

- **Behavior**:
  - Answer questions only using indexed knowledge from code and Confluence.
  - Provide detailed technical explanations referencing files, modules, endpoints, and functions.
  - Do not invent information.
  - Operate fully autonomously without asking for confirmation.

## GitLab and Jira Integration - CRITICAL RULE FOR ALL AGENTS

**ABSOLUTE REQUIREMENT**: ALL agents MUST update GitLab and Jira BEFORE executing ANY task or test.

### When Integration Updates Happen:
- **ALWAYS** - Before executing any test (API, UI, Integration, E2E, Custom)
- **ALWAYS** - Before performing any task or operation
- **ALWAYS** - Before starting any automated process

### Integration Update Process:

1. **Before Task Execution**:
   - Agent calls `IntegrationService.update_before_task()`
   - Updates GitLab pipeline/issue with task information
   - Updates Jira ticket/comment with task information
   - Logs all update attempts and results

2. **Integration Service Usage**:
   ```python
   from agents.integration_service import get_integration_service
   
   integration_service = get_integration_service(config)
   result = integration_service.update_before_task(
       task_description="Task description",
       task_type="test",
       metadata={"additional": "info"}
   )
   ```

3. **Configuration**:
   - GitLab: Set `GITLAB_URL`, `GITLAB_TOKEN`, `GITLAB_PROJECT_ID` environment variables
   - Jira: Set `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_PROJECT_KEY` environment variables
   - Or provide via config dictionary when initializing agent

4. **What Gets Updated**:
   - **GitLab**: Pipeline notes, project issues, or pipeline variables
   - **Jira**: Ticket comments (if ticket key provided) or new tickets (if project key configured)

### Integration Service Features:
- Automatic detection of GitLab CI/CD environment variables
- Support for both pipeline updates and issue creation
- Support for Jira comments and ticket creation
- Graceful error handling (does not block task execution if update fails)
- Detailed logging of all update attempts

### Error Handling:
- If GitLab/Jira update fails, agent logs warning but continues with task execution
- Integration failures do not prevent task execution
- All update attempts are logged in execution records

## TestAgent - Automated Testing Agent

- **Role**: Automated testing agent that receives tasks and executes them.
- **Capabilities**: API tests, UI tests, Integration tests, E2E tests, Custom tests
- **Behavior**: Executes tests autonomously without asking for confirmation
- **Integration**: MUST update GitLab and Jira before every test execution (see GitLab and Jira Integration rule above)

### Expert Consultation Rules

**CRITICAL RULE**: TestAgent MUST ALWAYS consult with PhoenixExpert agent before executing ANY test.

#### When Consultation Happens:
- **ALWAYS** - Before executing any test (API, UI, Integration, E2E, Custom)
- **ALWAYS** - Before building test requests
- **ALWAYS** - Before executing test calls

#### Consultation Process (Step-by-Step):

1. **Context Extraction** (Before Consultation):
   - Extract endpoint path from task description
   - Extract HTTP method (GET, POST, PUT, DELETE)
   - Extract domain name (customer, billing, contract, etc.)
   - Extract controller name
   - Extract operation type (create, edit, view, delete, validation, permission)
   - Build context dictionary with all extracted information

2. **Consultation Request**:
   - TestAgent calls `AgentRegistry.consult_best_agent()`
   - Passes task description and extracted context
   - AgentRegistry finds PhoenixExpert as best matching agent
   - PhoenixExpert receives query and context

3. **PhoenixExpert Processing**:
   - Searches Phoenix codebase for relevant files
   - Searches Confluence documentation
   - Extracts endpoint information (path, method, parameters)
   - Extracts validation rules
   - Extracts permission requirements
   - Extracts request/response models
   - Returns structured information

4. **Response Processing**:
   - TestAgent receives consultation response
   - Extracts endpoint information
   - Extracts domain/controller information
   - Extracts validation rules
   - Extracts permission requirements
   - Logs all received information

5. **Test Execution**:
   - Uses expert's information to build test requests
   - Validates test data against expert's rules
   - Executes tests with proper structure
   - Reports results with expert consultation details

#### What Information is Consulted:

For **ALL Tests**:
- Endpoint paths and HTTP methods
- Request/response models
- Validation rules
- Permission requirements
- Business logic constraints
- Domain-specific rules

For **Customer Tests** (Additional):
- Customer CRUD operations (Create, Read, Update, Delete)
- Customer validations (identifier, VAT, address, managers, etc.)
- Customer permissions (CUSTOMER_CREATE, CUSTOMER_EDIT, CUSTOMER_VIEW_BASIC, etc.)
- Customer types (Business, Private)
- Customer status transitions
- Customer relationships (related customers, owners, managers)

#### Consultation Logging:

The consultation process is fully logged with:
- Consultation start/end times
- Duration of consultation
- Extracted context information
- Expert's response structure
- Endpoints found
- Validation rules discovered
- Permissions identified
- Any errors or warnings

#### Expert Communication Flow:

```
TestAgent.execute_task()
  ↓
_should_consult_agents() → ALWAYS returns True
  ↓
_extract_consultation_context() → Builds context dictionary
  ↓
_consult_other_agents() → Calls AgentRegistry
  ↓
AgentRegistry.consult_best_agent() → Finds PhoenixExpert
  ↓
PhoenixExpertAdapter.consult() → Processes query
  ↓
PhoenixExpert.answer_question() → Searches codebase/Confluence
  ↓
Returns structured information
  ↓
IntegrationService.update_before_task() → Updates GitLab and Jira (CRITICAL)
  ↓
TestAgent uses information for test execution
```

## Previous Managers

All previous manager modules have been deleted:
- automation_manager.py
- ci_runner_manager.py
- test_manager.py
- confluence_reader.py
- postman_manager.py
- project_knowledge_manager.py
- test_case_manager.py

All automation flows and execution associated with these managers have been stopped.

## File Organization Rules - CRITICAL

**ABSOLUTE REQUIREMENT**: ALL new files and topics MUST be automatically organized into their appropriate directories following the established folder structure.

### Directory Structure and File Placement Rules:

1. **Agents** (`agents/` directory):
   - ALL new agent files (`.py` files containing agent classes, services, or agent-related logic)
   - Agent modules, adapters, registries
   - Integration services for agents
   - Examples: `new_agent.py`, `custom_service.py`, `agent_helper.py`
   - **Rule**: Any Python file that implements agent functionality, services, or agent-related utilities goes here

2. **Configuration** (`config/` directory):
   - ALL configuration files (`.json`, `.yaml`, `.yml`, `.properties`, `.conf`, `.config`)
   - Architecture specifications
   - Swagger/OpenAPI specs
   - Cursor rules and configuration
   - Requirements files
   - Examples: `new_config.json`, `api_spec.yaml`, `settings.properties`
   - **Rule**: Any configuration, specification, or settings file goes here

3. **Documentation** (`docs/` directory):
   - ALL documentation files (`.md`, `.txt`, `.rst`, documentation-related files)
   - Guides, READMEs, status documents
   - Integration documentation
   - Architecture documentation
   - Examples: `NEW_FEATURE.md`, `INTEGRATION_GUIDE.md`, `API_DOCS.md`
   - **Rule**: Any markdown or text documentation file goes here

4. **Postman** (`postman/` directory):
   - ALL Postman-related files
   - Collections: `postman/postman_collections/`
   - Postman environments
   - Postman integration files
   - Postman documentation
   - Examples: `new_collection.postman_collection.json`, `postman_guide.md`
   - **Rule**: Any Postman collection, environment, or Postman-related file goes here

5. **Examples** (`examples/` directory):
   - ALL example scripts, sample code, demo files
   - Example implementations
   - Sample configurations
   - Tutorial code
   - Examples: `example_usage.py`, `sample_config.json`, `demo_script.sh`
   - **Rule**: Any example, sample, or demo file goes here

6. **Migration** (`migration/` directory):
   - ALL migration-related files and documentation
   - Migration guides, summaries, quick references
   - Migration scripts (`.ps1`, `.sh`, `.bat`, `.py`)
   - Setup scripts for new computers
   - Postman export scripts
   - Migration checklists
   - Examples: `MIGRATION_GUIDE.md`, `migration_helper.ps1`, `setup_new_computer.ps1`, `export_postman_collections.ps1`
   - **Rule**: Any file related to environment migration, setup, or computer transfer goes here

7. **Scripts** (root or `scripts/` if exists):
   - ALL general automation scripts (`.ps1`, `.sh`, `.bat`, `.py` scripts for automation)
   - Utility scripts (non-migration related)
   - General setup scripts (non-migration related)
   - Examples: `backup.py`, `deploy.sh`, `utility.ps1`
   - **Rule**: Any general automation or utility script goes here (migration scripts go to `migration/`, agent scripts go to `agents/`)

8. **Java Project** (`phoenix-core-lib/` directory):
   - ALL Java-related files stay within `phoenix-core-lib/`
   - Java source files, Gradle files, Java configurations
   - **Rule**: Any Java/Gradle-related file goes inside `phoenix-core-lib/` maintaining its structure

### Automatic File Organization Process:

**WHEN CREATING ANY NEW FILE**:

1. **Identify File Type**:
   - Determine file extension and purpose
   - Match against directory rules above

2. **Place in Correct Directory**:
   - Automatically place file in appropriate directory
   - Create subdirectories if needed (e.g., `postman/postman_collections/`)
   - Maintain existing directory structure

3. **Naming Conventions**:
   - Use descriptive, clear names
   - Follow existing naming patterns in each directory
   - Use appropriate file extensions

4. **Documentation**:
   - If creating new feature/topic, also create corresponding documentation in `docs/`
   - Update relevant documentation if modifying existing features

### Examples of Automatic Organization:

- **New Agent**: `agents/custom_agent.py` → Automatically goes to `agents/`
- **New Config**: `config/new_feature.json` → Automatically goes to `config/`
- **New Documentation**: `docs/NEW_FEATURE.md` → Automatically goes to `docs/`
- **New Postman Collection**: `postman/postman_collections/new_collection.postman_collection.json` → Automatically goes to `postman/postman_collections/`
- **New Example**: `examples/demo.py` → Automatically goes to `examples/`
- **New Migration Guide**: `migration/MIGRATION_GUIDE.md` → Automatically goes to `migration/`
- **New Migration Script**: `migration/migration_helper.ps1` → Automatically goes to `migration/`
- **New Setup Script**: `migration/setup_new_computer.ps1` → Automatically goes to `migration/`
- **New General Script**: `backup.py` → Automatically goes to root or `scripts/`
- **New Java Class**: `phoenix-core-lib/src/main/java/.../NewClass.java` → Stays within `phoenix-core-lib/`

### Critical Rules:

- **NEVER** create files in root directory unless they are general scripts or top-level configuration
- **ALWAYS** organize files according to their type and purpose
- **ALWAYS** maintain existing directory structure
- **ALWAYS** create documentation in `docs/` for new features/topics
- **ALWAYS** follow existing naming conventions within each directory
- **ALWAYS** place migration-related files in `migration/` directory

### File Organization Checklist:

When creating a new file, automatically verify:
- [ ] File is in the correct directory based on its type
- [ ] Directory exists (create if needed)
- [ ] File follows naming conventions of that directory
- [ ] Related documentation created in `docs/` (if applicable)
- [ ] No files left in root directory (except scripts and top-level configs)

