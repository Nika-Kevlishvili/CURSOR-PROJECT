# Slack Report Template – Playwright Test Results

**Scope:** This template applies **only** to reports sent to Slack (tester and #ai-report channel) as part of the HandsOff flow. The same structure MUST be used every time a Playwright test results report is sent to Slack.

**Reference:** `.cursor/rules/handsoff_playwright_report.mdc` §4; `.cursor/commands/hands-off.md` Step 6–7.

---

## Template Structure

Use the following structure exactly. Replace placeholders with actual values from the Jira ticket and Playwright run.

```
{JIRA_KEY} – Playwright test results

Jira: {JIRA_KEY}
Title: {ticket_title_from_jira}
Date: {YYYY-MM-DD}
Assignee: "{assignee_display_name}" / Tester: "{tester_display_name}" .

Total: {passed_count} passed, {failed_count} failed, {skipped_count} skipped.

-------------------------

Playwright test results:

Test 1: [{JIRA_KEY}] {short_test_title} – {short_details_about_this_test}

Test description:
{all_details_about_this_test}

Expected result:
{expected_result}

Actual result:
{actual_result}

Test result:
{Passed | Failed | Not run}


Test 2: [{JIRA_KEY}] {short_test_title} – {short_details_about_this_test}

Test description:
{all_details_about_this_test}

Expected result:
{expected_result}

Actual result:
{actual_result}

Test result:
{Passed | Failed | Not run}


… (repeat for each test)
```

---

## Placeholders

| Placeholder | Source | Notes |
|-------------|--------|--------|
| `{JIRA_KEY}` | Jira issue key | e.g. NT-1, REG-123 |
| `{ticket_title_from_jira}` | Jira issue summary | Full title of the ticket |
| `{YYYY-MM-DD}` | Report date | Current date when report is built |
| `{assignee_display_name}` | Jira Assignee | Use "—" or "N/A" if not set |
| `{tester_display_name}` | Jira Tester / Assignee | Use assignee if no tester field; "N/A" if not set |
| `{passed_count}` | Playwright run | Number of tests passed |
| `{failed_count}` | Playwright run | Number of tests failed |
| `{skipped_count}` | Playwright run | Number of tests skipped |
| `{short_test_title}` | Test name/title | Brief title of the test |
| `{short_details_about_this_test}` | Test summary | One line or short phrase: what this test verifies |
| `{all_details_about_this_test}` | Test case / spec | Full description: steps, endpoint, behaviour |
| `{expected_result}` | Test case | What was expected |
| `{actual_result}` | Playwright output | What actually happened (if failed: assertion message, status, error snippet) |
| `{Passed \| Failed \| Not run}` | Playwright result | Final result for this test |

---

## Example (filled)

```
NT-1 – Playwright test results

Jira: NT-1
Title: Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked
Date: 2026-03-09
Assignee: "Developer name" / Tester: "Tester name" .

Total: 2 passed, 0 failed, 0 skipped.

-------------------------

Playwright test results:

Test 1: [NT-1] Invoice cancellation – API returns 400 when cancelling paid invoice with locked payment package

Test description:
Call invoice cancellation endpoint for an invoice that is paid and whose payment package is locked. Verify request is rejected.

Expected result:
HTTP 400 (or appropriate business error); invoice remains unchanged.

Actual result:
HTTP 400; response body indicates payment package locked.

Test result:
Passed


Test 2: [NT-1] Invoice cancellation – Locked package scenario documented

Test description:
Verify that the scenario "locked payment package" is covered and assertion matches expected behaviour.

Expected result:
Test passes when service correctly rejects cancellation.

Actual result:
Assertion passed.

Test result:
Passed
```

---

## Rules

1. **Always use this template** when sending Playwright test result reports to Slack (HandsOff flow).
2. **Same content** is sent to both the tester (assignee) and the #ai-report channel (channel_id: C0AK96S1D7X).
3. **One block per test** with: Test N header, Test description, Expected result, Actual result, Test result.
4. **English only** (Rule 0.7).
5. If Slack message length is limited, keep at least the header, Total line, and full Playwright test results section (each test with description, expected, actual, result).
