# Playwright detailed report template

Canonical output file: `Cursor-Project/EnergoTS/playwright-report-detailed.md`  
Generator: `Cursor-Project/config/playwright/generate-detailed-report.mjs`

Use this exact structure for all detailed Playwright reports.

## Document structure

```md
# {JIRA_KEY or N/A} Detailed Test Report

Run context
- Jira key: {JIRA_KEY or N/A}
- Date: {YYYY-MM-DD}
- Branch: cursor
- Spec path: `{single spec}` OR list multiple spec paths
- Source JSON: `playwright-report.json`
- Command: {exact command if available, otherwise "N/A (read from generated Playwright JSON report)"}

Summary
- Total in scope: {N}
- Passed: {N}
- Failed: {N}
- Skipped: {N}
- Duration: ~{seconds}s
- Test case IDs: {TC-BE-1, TC-BE-2, ...} (optional when mapped)

Test cases

TC-BE-1
- Status: PASSED|FAILED|SKIPPED
- Expected Result: {from test title / mapped scenario}
- Actual Result: {short factual outcome}
- Portal data links:
  - {entity}: {url}
  - not available

TC-BE-2
- Status: ...
- Expected Result: ...
- Actual Result: ...
- Portal data links:
  - ...
```

## Rules

- Keep report text concise and readable.
- Prefer one test case block per executed Playwright test in run order.
- If TC mapping is not present in title, use `Test-{N}` as block title.
- Always include `Portal data links` with either real links or `not available`.
- Do not paste full report into Slack body; send short summary + attach file.
