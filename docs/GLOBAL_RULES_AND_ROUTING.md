# Global Rules and Automatic Agent Routing

## Overview

This document describes the global rules system and automatic agent routing that all agents must follow.

## Global Rules System

### Purpose

The Global Rules system (`agents/global_rules.py`) enforces rules that ALL agents must follow, ensuring consistent behavior and security across the entire agent ecosystem.

### Rule 1: GitHub Operations Require Explicit Permission

**CRITICAL**: No agent can make ANY changes to GitHub without explicit user permission.

#### What is Blocked:
- Git push operations
- Git commit operations  
- Git merge operations
- Creating/deleting branches
- Creating pull requests
- Any GitHub repository modifications
- Git remote operations that modify GitHub

#### How It Works:

1. **Automatic Detection**: The system automatically detects GitHub-related operations in queries using keyword matching and pattern recognition.

2. **Permission Check**: Before any GitHub operation, agents MUST check with the GlobalRules system:
   ```python
   from agents.global_rules import get_global_rules
   
   global_rules = get_global_rules()
   permission = global_rules.check_github_permission(
       operation="push to GitHub",
       details={'branch': 'main'}
   )
   
   if not permission['permitted']:
       # Operation is BLOCKED
       return {'error': permission['message']}
   ```

3. **Blocking**: If permission is not granted, the operation is automatically blocked and the user is notified.

4. **Permission Grant**: User must explicitly grant permission:
   ```python
   global_rules.grant_github_permission()
   ```

#### User Commands:

- **Grant Permission**: 
  - "Grant GitHub permission"
  - "Allow GitHub operations"
  - "Enable GitHub access"

- **Revoke Permission**:
  - "Revoke GitHub permission"
  - "Block GitHub operations"
  - "Disable GitHub access"

#### Example Usage in Agents:

```python
from agents.global_rules import get_global_rules

class MyAgent:
    def execute_task(self, task: str):
        global_rules = get_global_rules()
        
        # Check if this is a GitHub operation
        if global_rules.is_github_operation(task):
            permission = global_rules.check_github_permission(
                operation=task,
                details={'agent': self.get_name()}
            )
            
            if not permission['permitted']:
                return {
                    'success': False,
                    'error': permission['message'],
                    'requires_permission': True
                }
        
        # Proceed with task execution
        # ...
```

### Rule 2: Automatic Agent Routing

**CRITICAL**: All user queries are automatically routed to the most competent agent(s).

## Automatic Agent Routing System

### Purpose

The Agent Router (`agents/agent_router.py`) automatically:
1. Analyzes user queries to determine intent
2. Identifies which agent(s) are most competent
3. Routes queries to appropriate agent(s)
4. Combines responses from multiple agents when needed

### How It Works

#### 1. Query Analysis

The router analyzes the user's query to determine:
- Primary intent (test, question, documentation, etc.)
- Confidence score
- Whether multiple agents are needed

#### 2. Agent Selection

The router evaluates all registered agents based on:
- Agent capabilities matching query keywords
- Agent name matching detected intent
- Agent's `can_help_with()` method result
- Competence scores calculated from multiple factors

#### 3. Routing

- **Single Agent**: Routes directly to the best matching agent
- **Multiple Agents**: Orchestrates multiple agents and combines their responses

#### 4. Response Combination

When multiple agents are used:
- Responses are intelligently combined
- Highest-scoring agent's response is primary
- Supplementary responses from other agents are included
- Unified result is returned

### Usage

#### Basic Usage:

```python
from agents.agent_router import get_agent_router

router = get_agent_router()
result = router.route_query("How do I test the customer API endpoint?")
```

#### With Context:

```python
context = {
    'test_type': 'api',
    'base_url': 'http://localhost:8080',
    'environment': 'development'
}

result = router.route_query("Test customer create endpoint", context)
```

### Response Format

#### Single Agent Response:

```python
{
    'success': True,
    'routing_type': 'single',
    'agents_used': ['TestAgent'],
    'primary_agent': 'TestAgent',
    'response': {
        # Agent's response data
    },
    'query': 'original query'
}
```

#### Multi-Agent Response:

```python
{
    'success': True,
    'routing_type': 'orchestrated',
    'agents_used': ['TestAgent', 'PhoenixExpert'],
    'primary_agent': 'TestAgent',
    'agent_responses': [
        {
            'agent': 'TestAgent',
            'score': 0.9,
            'response': {...},
            'success': True
        },
        {
            'agent': 'PhoenixExpert',
            'score': 0.7,
            'response': {...},
            'success': True
        }
    ],
    'combined_response': {
        'combined': True,
        'primary_response': {...},
        'primary_agent': 'TestAgent',
        'supplementary_responses': [...],
        'summary': 'Combined responses from 2 agents'
    },
    'query': 'original query'
}
```

### Integration with Cursor

The Agent Router is designed to be used automatically by Cursor when processing user queries. Cursor should:

1. **Intercept User Queries**: When a user makes a request, Cursor should use the router
2. **Automatic Routing**: Let the router determine which agent(s) to use
3. **Return Combined Results**: Present the unified result to the user

#### Example Cursor Integration:

```python
# In Cursor's query handler
from agents.agent_router import get_agent_router

def handle_user_query(user_query: str):
    router = get_agent_router()
    result = router.route_query(user_query)
    
    if not result.get('success'):
        return f"Error: {result.get('error')}"
    
    # Format and return result to user
    if result['routing_type'] == 'single':
        return format_single_agent_response(result)
    else:
        return format_multi_agent_response(result)
```

## Agent Competence Evaluation

The router calculates competence scores based on:

1. **Keyword Matching**: How well agent keywords match the query
2. **Intent Matching**: How well agent name matches detected intent
3. **Capability Matching**: How well agent capabilities match query requirements
4. **Historical Performance**: (Future enhancement)

### Agent Keywords

Agents are associated with keywords that help the router identify them:

- **Test Agent**: test, testing, api test, ui test, automation, test case, etc.
- **Phoenix Expert**: phoenix, question, how, what, why, explain, documentation, etc.
- **Postman**: postman, collection, export, import, generate collection, etc.

## Best Practices

### For Agent Developers:

1. **Always Check Global Rules**: Before performing any restricted operation, check with GlobalRules
2. **Implement can_help_with()**: Ensure your agent properly implements this method for routing
3. **Provide Clear Capabilities**: List clear capabilities in `get_capabilities()` method
4. **Handle Routing**: Be prepared to work with the router's orchestration

### For System Integrators:

1. **Use Router for All Queries**: Always route user queries through the AgentRouter
2. **Respect Permission System**: Never bypass the GlobalRules permission checks
3. **Monitor Routing History**: Use `router.get_routing_history()` to track routing decisions
4. **Handle Multi-Agent Results**: Properly format and present combined responses

## Examples

### Example 1: GitHub Operation Blocked

```python
# User query: "Push changes to GitHub"
router = get_agent_router()
result = router.route_query("Push changes to GitHub")

# Result:
{
    'success': False,
    'error': 'GitHub operations require explicit permission from user',
    'requires_permission': True,
    'operation_type': 'github',
    'query': 'Push changes to GitHub'
}
```

### Example 2: Automatic Test Agent Routing

```python
# User query: "Test the customer create API endpoint"
router = get_agent_router()
result = router.route_query("Test the customer create API endpoint")

# Router automatically:
# 1. Detects intent: "test"
# 2. Finds TestAgent as most competent
# 3. Routes query to TestAgent
# 4. Returns TestAgent's response
```

### Example 3: Multi-Agent Orchestration

```python
# User query: "How do I test the customer endpoint and what are the validation rules?"
router = get_agent_router()
result = router.route_query("How do I test the customer endpoint and what are the validation rules?")

# Router automatically:
# 1. Detects intent: both "test" and "question"
# 2. Finds TestAgent and PhoenixExpert as competent
# 3. Orchestrates both agents
# 4. Combines responses:
#    - TestAgent provides test execution details
#    - PhoenixExpert provides validation rules
# 5. Returns unified result
```

## Troubleshooting

### Router Not Finding Agents

- Ensure agents are registered with AgentRegistry
- Check that agents implement `can_help_with()` correctly
- Verify agent capabilities are properly defined

### Permission Issues

- Check if GitHub permission is granted: `global_rules.github_permission_granted`
- Review violation history: `global_rules.get_rule_violations()`
- Grant permission if needed: `global_rules.grant_github_permission()`

### Routing History

- View routing decisions: `router.get_routing_history()`
- Analyze which agents were selected and why
- Use for debugging and optimization

## Future Enhancements

- Machine learning-based agent selection
- Historical performance tracking
- User preference learning
- Dynamic agent capability discovery
- Advanced response synthesis algorithms

