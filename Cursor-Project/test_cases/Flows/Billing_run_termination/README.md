# Billing_run_termination – Test cases for billing run termination flow (PDT-2023)

This folder contains test cases for the **billing run termination** flow: disabling the terminate button when appropriate and adding the new status "in progress termination" (IN_PROGRESS_TERMINATION).

## Scope

- **Terminate button:** Disabled when status is IN_PROGRESS_TERMINATION or when termination is not allowed (e.g. CANCELLED, COMPLETED).
- **New status:** IN_PROGRESS_TERMINATION; final status after termination is CANCELLED.
- **Entry points:** PATCH /billing-run/terminate, BillingRunService.cancel(), UI terminate button.
- **Regression:** UI button during IN_PROGRESS_TERMINATION; resume() and schedulers excluding IN_PROGRESS_TERMINATION/CANCELLED; lock cleanup and terminate_billing_run/unlock ordering; list and filters for new status; API/enum; concurrent start and terminate.

## Test case files

| File | Content |
|------|--------|
| **Terminate_button_and_status.md** | UI: terminate button enabled/disabled by status; display of "in progress termination"; list and filter support for new status (TC-1–TC-6). |
| **API_terminate_flow.md** | PATCH /billing-run/terminate and BillingRunService.cancel(): success for allowed statuses; rejection for IN_PROGRESS_TERMINATION, CANCELLED, COMPLETED, invalid ID; status transition; lock cleanup (TC-1–TC-8). |
| **Resume_and_schedulers.md** | resume() rejects IN_PROGRESS_TERMINATION and CANCELLED; resume() allows resumable statuses; schedulers do not pick up IN_PROGRESS_TERMINATION or CANCELLED (TC-1–TC-5). |
| **Concurrent_and_lock_ordering.md** | Concurrent start and terminate; terminate_billing_run/unlock ordering; second terminate rejected (TC-1–TC-3). |
| **List_and_filters.md** | List displays IN_PROGRESS_TERMINATION; filter by new status; API/enum includes new status; no regression on other statuses (TC-1–TC-4). |

## References

- **Jira:** PDT-2023 – Change in billing run termination (disabling terminate button, new status "in progress termination").
- **BillingStatus:** availableStatusesForTermination = INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED; IN_PROGRESS_TERMINATION; CANCELLED.
- **Template:** Cursor-Project/config/Test_case_template.md.
