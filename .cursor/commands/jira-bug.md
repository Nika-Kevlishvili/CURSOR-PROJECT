# Jira Bug (Experiments Board)

Create or rewrite a Jira bug using the standard template. **Only for the Experiments board.** Creating bugs in Phoenix delivery is **not allowed** (Rule JIRA.0).

## When to use

- User wants to **create a new Jira bug** on the Experiments board.
- User wants to **rewrite or improve an existing Jira bug** to match the template.
- Triggers: `!jira-bug`, "create Jira bug", "Experiments board bug", "write bug in Jira (Experiments)".

## Do NOT use for

- **Phoenix delivery** – Creating or writing bugs in Phoenix delivery board/project is **prohibited**. The hook blocks such requests; redirect user to Experiments board.

## Rule and template

- **Rule:** `.cursor/rules/jira_bug_agent.mdc` (JIRA.0).
- **Template:** Summary, Description, Steps to reproduce, Expected result, Actual result, Environment (Board: Experiments, Environment, Browser), Technical details (Endpoint, Payload, Status), Example.
- **Skill:** `.cursor/skills/jira-bug-template/SKILL.md`.
- **Sub-agent:** `.cursor/agents/jira-bug.md`.

## Flow

1. Confirm target is **Experiments** board (if user says "Phoenix delivery", refuse and point to Experiments).
2. Ask for or extract: feature/experiment, steps, expected vs actual, environment, endpoint/payload/status if applicable.
3. Fill the template and output ready-to-paste Jira text.
4. For "rewrite existing": take current description and reformat into the same template.

## Example

User: `!jira-bug Chart does not load on experiment details page.`

Agent: Asks 1–2 clarifying questions if needed, then outputs the full template (Summary, Description, Steps, Expected, Actual, Environment, Technical details, Example) for the Experiments board.
