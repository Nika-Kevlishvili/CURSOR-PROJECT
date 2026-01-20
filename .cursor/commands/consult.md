# PhoenixExpert Consultation

MANDATORY consultation before any task execution (Rule 8 - CRITICAL).

## When to Use:
- Before ANY task that affects Phoenix system
- Before code modifications
- Before test execution
- Before API collection generation
- Before any agent action

## PhoenixExpert Provides:
- Endpoint information and specifications
- Validation rules and constraints
- Permission requirements
- Business logic context
- Approval or rejection with reasoning

## Workflow:

1. **IntegrationService** - Call `IntegrationService.update_before_task()` FIRST
2. **Describe Task** - Clearly state what you want to do
3. **PhoenixExpert Review** - Get validation of approach
4. **Approval** - Task cannot proceed without PhoenixExpert approval

## Response Requirements:
- PhoenixExpert validates logic, approach, and correctness
- If rejected, follow PhoenixExpert's guidance (Rule 27)
- End with: "Agents involved: PhoenixExpert, [other agents if any]"

## Important:
- Consultation is BINDING - agents MUST follow PhoenixExpert decision
- ALL agents (TestExpert, DevOpsExpert, PostmanExpert) require this
- Skipping consultation is a CRITICAL SYSTEM ERROR
