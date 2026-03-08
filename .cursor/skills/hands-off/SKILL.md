---
name: hands-off
description: Runs the full HandsOff flow when user provides a Jira ticket (link, key, or name) and /HandsOff or !HandsOff. Orchestrates Jira fetch → cross-dependencies → test cases → Playwright creation → run tests → report (save as Jira key + send to Slack to tester). Use when the user gives a Jira ticket and invokes HandsOff.
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
   - Bridge: test case .md → test_specification → EnergoTSTestAgent create_new_test.
   - Run Playwright tests (energo-ts-run, cursor branch).
   - Save report as `reports/YYYY-MM-DD/{JIRA_KEY}.md` (pass/fail + reasons).
   - Send report to Slack to the tester on the ticket (user-slack MCP).

## References

- **Command (full steps):** `.cursor/commands/hands-off.md`
- **Agent (orchestrator):** `.cursor/agents/hands-off.md`
- **Phoenix commands:** `.cursor/skills/phoenix-commands/SKILL.md` (Hands-off row)

## Constraints

- Do not skip steps; run the full flow.
- EnergoTS must use **cursor** branch only (Rule ENERGOTS.0).
- Report must be named after the Jira key and sent to Slack as part of Step 9.
- **Lessons learned (do not repeat):** See `.cursor/rules/handsoff_playwright_report.mdc` – test cases in required folder and verified on disk; Playwright spec in `tests/cursor/` and verified; report ONLY Playwright test results (per test: what is verified, result, failure reason); send **full** report to Slack, not a short summary.
