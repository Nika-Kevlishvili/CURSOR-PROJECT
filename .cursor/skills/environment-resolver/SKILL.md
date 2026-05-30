---
name: environment-resolver
description: Resolves target environment (dev, dev2, test, preprod, prod, experiments) from user message or Jira before Phoenix branch alignment, bug validation, test cases, or HandsOff. Asks user when ambiguous (Rule CONF.0). No silent Test default.
---

# Environment Resolver Skill

Canonical procedure for **TC-ENV-ASK.0**, **DB.0a**, and HandsOff Step 1 environment gate.

**Subagent (I/O contract):** `.cursor/agents/environment-resolver.md`

## When to apply

- **HandsOff Step 1** — before `switch-phoenix-branches.ps1`.
- **Rule 35 Step 0a** — before TC-FRONTEND-ASK.0 and Phoenix alignment.
- **Rule 32 / DB.0a** — when environment is missing for Phoenix or PostgreSQL work.

## Output

Exactly one of: `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`.

If Jira Environment is empty and user did not name env in chat → **AskQuestion** (six options); **no silent Test default**.

## Mandatory behavior

0. **Rule 35:** Test-case generation → this is **Step 0a** before Frontend question and before `switch-phoenix-branches`. Empty Jira `environment` → always questionnaire.

1. **Source order (strict):**
   - Explicit user message in current chat (highest)
   - Jira fields/text (Environment, summary, description, comments)
   - Parent session context (same chat, prior confirmed env)

2. **No silent defaults** — never emit `Resolved environment: test` without user confirmation.

3. **AskQuestion on ambiguity** — six options only. Jira `environment` null/empty → ambiguous unless user named env in chat.

4. **Forbidden inference:** fix version / hotfix label; Approved for Prod; issue key prefix (PDT-*); import report filename; EnergoTS Playwright defaults; "usually Test"; parent habit.

5. **Prod:** note that `-ConfirmProd` + user ack required for branch alignment.

## Resolution algorithm

1. Normalize to lowercase, trim.
2. Match tokens / aliases:
   - `dev`: dev, development
   - `dev2`: dev2, dev-2, development2
   - `test`: test, testing, qa
   - `preprod`: preprod, pre-prod, staging, stage
   - `prod`: prod, production
   - `experiments`: experiments, experiment
3. Multiple environments with similar strength → ambiguous.
4. Confidence &lt; 90% or conflicting evidence → AskQuestion.

## Required output format

**Resolved:**

```markdown
**Resolved environment:** <env>
**Evidence:** <short list in priority order>
**Confidence: XX%**
Reason: <1-2 sentences>
```

**Ambiguous:** Do not emit `Resolved environment` until user picks. After selection:

```markdown
**Resolved environment:** <selected-env>
**Evidence:** user-selected via environment questionnaire
**Confidence: 100%**
Reason: Environment was explicitly selected by the user.
```

End with: `Agents involved: EnvironmentResolverAgent`

## Parent integration

Call before: `switch-phoenix-branches.ps1`, Rule 32, Rule 35 cross-dep/TC gen, env-specific Phoenix Q&A.
