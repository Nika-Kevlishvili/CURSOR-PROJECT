# Slack reporting — three paths (workspace)

There are **three** distinct Slack delivery paths. **Do not merge or confuse them.**

| # | Path | When | Channel / recipients | Template / format |
|---|------|------|----------------------|-------------------|
| **1** | **Bug validation (BugFinder)** | After completed bug validation (Rule 32) | **`bug-validation`** (`C0AUEEDVCEL`) — chat analysis only; optional disk file only on `/report` | Structured verdict report per Rule 32 — **not** `Slack_report_template.md` |
| **2** | **HandsOff** | **`/HandsOff`** / **`!HandsOff`** | Tester DM + **`#ai-report`** (`C0AK96S1D7X`) | **Text:** 3 blocks (`Slack_report_summary_short_template.md`). **MANDATORY:** attach **`{JIRA_KEY}.md`** + **`playwright-report-detailed.md`** (when JSON exists) via **`upload-file-to-slack.ps1`**. **Disk:** **`HandsOff reports/…/{JIRA_KEY}.md`** + **`EnergoTS/playwright-report-detailed.md`**. Optional long chat: **`Slack_report_template.md`**. |
| **3** | **Scoped Playwright Slack** | User asks Slack for **specific** tests | Same recipients when Jira Tester exists | Same **text + mandatory uploads**: **ScopedPlaywright_*.md** + **`playwright-report-detailed.md`** (when JSON exists). **`send-playwright-results-slack.md`**. |

## Rules

- **Path 1** is unchanged by Playwright / HandsOff work — do not replace it with the Playwright template. **Do not modify** `.cursor/agents/bug-validator.md`, Rule 32 delivery, or bug-validation skill Slack behavior when documenting or implementing paths 2–3.
- **Paths 2 and 3** use **short Slack text (three blocks)** + **upload the smart `.md` and the machine `playwright-report-detailed.md`** (when generated) + on-disk reports (see table). Same dual recipients as HandsOff Step 7 when Jira Tester is set.
- **Path 3** does not run the full HandsOff pipeline (no cross-deps / test-case generation unless the user asks separately).

## Related

- HandsOff: `.cursor/commands/hands-off.md`, Rule 37
- Scoped Slack: `.cursor/commands/send-playwright-results-slack.md`
- Short Slack: `Slack_report_summary_short_template.md` — detailed (human) file structure: `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md` — upload: `Cursor-Project/config/slack/upload-file-to-slack.ps1` — long Slack (optional): `Slack_report_template.md`
- Bug validation: `.cursor/rules/workflows/workflow_rules.mdc` Rule 32, `.cursor/skills/phoenix-bug-validation/SKILL.md`
- Machine detailed Markdown (`playwright-report-detailed.md` under **`Cursor-Project/EnergoTS/`**, next to JSON): Rule DPR.0 — **HandsOff / path 3:** generate + upload with smart report to **#ai-report** + Tester; **ad-hoc:** only on explicit user request
