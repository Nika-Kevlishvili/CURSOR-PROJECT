# Environment Resolve

Resolve the target environment for Phoenix-related tasks using a deterministic process.

## When to Use

Use this command before environment-dependent workflows:
- Phoenix code questions that require branch alignment
- Bug validation
- Cross-dependency analysis
- Test-case generation
- HandsOff or other Jira-driven flows

## Workflow

1. Route to **environment-resolver** subagent.
2. Resolve environment from:
   - explicit user message (highest priority),
   - Jira ticket fields/text,
   - prior confirmed environment in this chat session.
3. If ambiguous, show a questionnaire with options:
   - `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`
4. Return exactly one resolved environment.
5. If `prod`, require explicit user acknowledgement before any destructive alignment step (`-ConfirmProd`).

## Output

Return:
- `resolved_environment`
- short evidence
- confidence score

Then parent workflow continues with:
`powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <resolved_environment>`
(append ` -ConfirmProd` only for `prod` after explicit user acknowledgement).
