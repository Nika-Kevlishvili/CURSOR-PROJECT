# Project templates (`config/template/`)

Canonical markdown templates for Cursor workflows and agents.

| File | Purpose |
|------|---------|
| `Test_case_template.md` | Structure and rules for test case `.md` files under `test_cases/`. |
| `Slack_reporting_paths.md` | Index: three Slack paths (bug validation vs HandsOff vs scoped Playwright). |
| `../playwright/Playwright_run_detailed_report_template.md` | **Disk:** full run report — TC mapping, links/ids, expected vs actual, meets expectation (same folder as `generate-detailed-report.mjs`). |
| `Slack_report_summary_short_template.md` | **Slack (default):** three-block short body + **attach** detailed `.md` (`config/slack/upload-file-to-slack.ps1`). |
| `../slack/README.md` | How to upload the detailed report file to Slack (paths 2 & 3). |
| `Slack_report_template.md` | **Slack (optional):** long per-test body if user explicitly requests full paste in Slack. |

References in rules and commands use path: `Cursor-Project/config/template/<filename>.md` (except Playwright run detailed template: `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`).
