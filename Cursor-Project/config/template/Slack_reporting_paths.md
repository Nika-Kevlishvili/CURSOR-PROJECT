# Slack reporting — two paths (workspace)

There are **two** distinct Slack delivery paths. **Do not merge or confuse them.**

| # | Path | When | Channel / recipients | Template / format |
|---|------|------|----------------------|-------------------|
| **1** | **Bug validation (BugFinder)** | **Only when user explicitly asks** (e.g. "send to Slack") — not automatic after validation | **`bug-validation`** (`C0AUEEDVCEL`) — chat analysis always; Slack + optional disk file only on user request | Structured verdict report per Rule 32 (same full body as chat) — **not** `Slack_report_template.md` |
| **2** | **Scoped Playwright Slack** | User asks Slack for **specific** tests | Tester DM + **`#ai-report`** (`C0AK96S1D7X`) when Jira Tester exists | **Text:** 3 blocks (`Slack_report_summary_short_template.md`). **MANDATORY:** attach **ScopedPlaywright_*.md** + **`playwright-report-detailed.md`** (when JSON exists) via **`upload-file-to-slack.ps1`**. **Disk:** **`Chat reports/…/ScopedPlaywright_*.md`** + **`EnergoTS/playwright-report-detailed.md`**. |

## Rules

- **Path 1** (bug validation) stays the Rule 32 structured verdict report in **`bug-validation`** — see **`.cursor/skills/phoenix-bug-validation/SKILL.md`**. When documenting or implementing **path 2**, do **not** replace Path 1 with Playwright templates or conflate channels.
- **Path 2** uses **short Slack text (three blocks)** + **upload the smart `.md` and the machine `playwright-report-detailed.md`** (when generated) + on-disk reports (see table).

## Related

- Scoped Slack: `.cursor/commands/send-playwright-results-slack.md`
- Short Slack: `Slack_report_summary_short_template.md` — detailed (human) file structure: `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md` — upload: `Cursor-Project/config/slack/upload-file-to-slack.ps1` — long Slack (optional): `Slack_report_template.md`
- Bug validation: `.cursor/rules/workflows/workflow_rules.mdc` Rule 32, `.cursor/skills/phoenix-bug-validation/SKILL.md`
- Machine detailed Markdown (`playwright-report-detailed.md` under **`Cursor-Project/EnergoTS/`**, next to JSON): Rule DPR.0 — **path 2:** generate + upload with smart report to **#ai-report** + Tester; **ad-hoc:** only on explicit user request
