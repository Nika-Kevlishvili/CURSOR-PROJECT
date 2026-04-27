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

1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Phoenix branch alignment (Rule PHOENIX-SWITCH.0)** — When the consultation depends on current Phoenix code in a specific environment, **MANDATORY resolver call:** run `/environment-resolve` (EnvironmentResolverAgent) and use its resolved output (`dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`) before running `.cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` (with `-ConfirmProd` ONLY for `prod` after explicit user ack). If ambiguity remains, EnvironmentResolverAgent must ask the user via questionnaire (Rule CONF.0). Pure documentation / Confluence-only consultations may skip this step. If a previous step in this session already aligned to the same env, do not re-run it (subagent reuse, see Rule PHOENIX-SWITCH.0 §7a).
3. **Describe Task** - Clearly state what you want to do
4. **PhoenixExpert Review** - Get validation of approach
5. **Approval** - Task cannot proceed without PhoenixExpert approval

## Response Requirements:
- PhoenixExpert validates logic, approach, and correctness
- If rejected, follow PhoenixExpert's guidance (Rule 27)
- End with: "Agents involved: PhoenixExpert, [other agents if any]"

## Important:
- Consultation is BINDING - agents MUST follow PhoenixExpert decision
- ALL agents (TestExpert, DevOpsExpert, PostmanExpert) require this
- Skipping consultation is a CRITICAL SYSTEM ERROR
