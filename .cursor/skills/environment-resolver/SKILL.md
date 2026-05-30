---
name: environment-resolver
description: Resolves target environment (dev, dev2, test, preprod, prod, experiments) from user message or Jira before Phoenix branch alignment, bug validation, test cases, or HandsOff. Asks user when ambiguous (Rule CONF.0).
disable-model-invocation: true
---

# Environment Resolver Skill

Routes to **environment-resolver** subagent (`.cursor/agents/environment-resolver.md`).

## When to apply

- **HandsOff Step 1** — before `switch-phoenix-branches.ps1`.
- **Rule 35 Step 0a (TC-ENV-ASK.0)** — before Frontend question and Phoenix alignment.
- **Rule 32 / DB.0a** — when environment is missing for Phoenix or PostgreSQL work.

## Output

Exactly one of: `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`.

If Jira Environment is empty and user did not name env in chat → **AskQuestion** (six options); no silent Test default.

## Subagent reference

`.cursor/agents/environment-resolver.md`
