---
name: hands-off
description: Runs the full HandsOff flow when user provides a Jira ticket (link, key, or name) and /HandsOff or !HandsOff. Orchestrates Jira fetch → cross-dependencies → test cases → Playwright creation → run tests → report (save as Jira key + send to Slack to tester and to AI report channel). Use when the user gives a Jira ticket and invokes HandsOff.
---

# HandsOff Skill

Use this skill when the user provides a **Jira ticket** (link, key like REG-123, or ticket name) and invokes **`/HandsOff`** or **`!HandsOff`**. The system must run the **full automated flow** without further user intervention.

## When to Apply

- User message contains a Jira ticket identifier (link, REG-xxx, BUG-xxx, or ticket title) **and** the trigger **/HandsOff** or **!HandsOff**.
- User says "HandsOff for this ticket", "Run HandsOff for REG-123", or similar with a ticket reference.

## What to Do

1. **Route** to the **hands-off** orchestrator (command: `.cursor/commands/hands-off.md`, agent: `.cursor/agents/hands-off.md`).
2. **Execute** the workflow described in `hands-off.md` in order:
   - Get Jira ticket and description (Jira MCP).
   - Run cross-dependency-finder for the Jira key (Rule 35a).
   - Run test-case-generator with cross_dependency_data.
   - **Bridge (Step 4):** Invoke **energo-ts-test agent (EnergoTSTestAgent)** with the **test case .md paths** and Jira key/title. The agent MUST create the Playwright spec from that content using the EnergoTS framework (fixtures); do NOT write the spec manually or with ad-hoc code.
   - Run Playwright tests (energo-ts-run, cursor branch).
   - Save report as `reports/YYYY-MM-DD/{JIRA_KEY}.md` (pass/fail + reasons).
   - Send report to Slack to the tester on the ticket and duplicate the same report to the AI report channel (user-slack MCP).
   - **After the report:** Collect questions from each participating agent (as needed); tag each question with the agent name (`[AgentName]: <question>`); send the list of questions to the same Slack recipients (tester + AI report channel). See `.cursor/rules/handsoff_playwright_report.mdc` §7.

## References

- **Command (full steps):** `.cursor/commands/hands-off.md`
- **Agent (orchestrator):** `.cursor/agents/hands-off.md`
- **Phoenix commands:** `.cursor/skills/phoenix-commands/SKILL.md` (Hands-off row)

## Constraints

- Do not skip steps; run the full flow.
- EnergoTS must use **cursor** branch only (Rule ENERGOTS.0).
- Report must be named after the Jira key and sent to Slack as part of Step 9.
- **Lessons learned (do not repeat):** See `.cursor/rules/handsoff_playwright_report.mdc` – test cases in required folder and verified on disk; Playwright spec in `tests/cursor/` and verified; report ONLY Playwright test results (per test: what is verified, result, failure reason); send **full** report to Slack, not a short summary; after report, send agent questions with attribution (each question tagged with agent name).
