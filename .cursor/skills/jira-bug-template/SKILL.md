---
name: jira-bug-template
description: Produces Jira bug text using the Experiments board template (Summary, Description, Steps, Expected, Actual, Environment, Technical details, Example). Use when the user wants to create a Jira bug or rewrite an existing one. Applies ONLY to the Experiments board; must NOT create bugs in Phoenix delivery (Rule JIRA.0).
---

# Jira Bug Template (Experiments Board Only)

Turns a free-form bug description into Jira-ready text. **Only for the Experiments board.** Creating bugs in **Phoenix delivery** is **prohibited** (Rule JIRA.0); redirect the user to Experiments.

## When to use

- User asks to create a Jira bug or rewrite an existing ticket.
- User says "Experiments board", "!jira-bug", or similar.
- **Do not use** when the user explicitly asks for a bug in **Phoenix delivery** – refuse and point to Experiments.

## Template (mandatory structure)

Use exactly this structure. All output in English.

```markdown
Summary:
[One short sentence describing the problem]

Description:
[Short context: which experiment/feature/page, and when the bug appears]

Steps to reproduce:
1. [Step 1]
2. [Step 2]
3. [Step 3]
4. [...]

Expected result:
[What should happen]

Actual result:
[What actually happens]

Environment:
- Board: Experiments
- Environment: [Dev / Test / PreProd / Prod]
- Browser: [e.g. Chrome 122]

Technical details:
- Endpoint: [API endpoint URL/path]
- Payload: [Request payload or key fields]
- Status: [HTTP status or system status, e.g. 500 / FAILED]

Example:
[Sample data: JSON payload, response body, or log snippet]
```

## Rules

1. **Board:** Always set `Board: Experiments` in Environment. Never create content for Phoenix delivery.
2. **Language:** All sections in English.
3. **Missing info:** If steps or expected/actual are unclear, ask 1–4 short questions, then fill the template.
4. **Rewrite:** When improving an existing bug, keep this section order and headings; only clarify and clean text.

## Command and sub-agent

- **Command:** `.cursor/commands/jira-bug.md`
- **Sub-agent:** `.cursor/agents/jira-bug.md`
- **Rule:** `.cursor/rules/jira_bug_agent.mdc` (JIRA.0)

## Phoenix delivery prohibition

If the user says "Phoenix delivery" or "create bug in Phoenix delivery", respond with:

- Creating Jira bugs in Phoenix delivery is not allowed. Use the **Experiments** board. You can use `!jira-bug` for that.

Do not fill the template for Phoenix delivery.
