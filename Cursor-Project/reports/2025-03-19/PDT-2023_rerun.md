# PDT-2023 – Playwright rerun results (EnergoTS)

**Run command:** `npx playwright test tests/cursor/PDT-2023-billing-run-termination.spec.ts`  
**Branch:** `cursor`  
**Total tests:** 55  
**Passed:** 4  
**Failed:** 41  
**Skipped:** 10  

## Failure summary (key error snippet)

Most failures are caused by an unexpected response shape for billing-run list endpoints (the tests expect an array at `body.content` or `body`, but receive a non-array), resulting in errors like:

```
TypeError: (intermediate value)(intermediate value)(intermediate value).find is not a function
TypeError: (intermediate value)(intermediate value)(intermediate value).filter is not a function
TypeError: runs is not iterable
```

## Failed tests (41)

For each failed test below, the failure reason snippet is the TypeError shown above (array operations on a non-array response), with stack traces pointing into `tests/cursor/PDT-2023-billing-run-termination.spec.ts`.

1. `[PDT-2023] TC-1: Terminate button enabled when billing run is in a status that allows termination` – `TypeError: runs.find is not a function`
2. `[PDT-2023] TC-2: Terminate button disabled when billing run status is "in progress termination"` – `TypeError: ...filter is not a function`
3. `[PDT-2023] TC-3: Terminate button disabled when billing run status is CANCELLED` – `TypeError: ...find is not a function`
4. `[PDT-2023] TC-4: Terminate button disabled when billing run status is COMPLETED or DELETED` – `TypeError: ...find is not a function`
5. `[PDT-2023] TC-5: UI displays the new status "in progress termination" correctly` – `TypeError: runs.find is not a function`
6. `[PDT-2023] TC-7: PATCH /billing-run/terminate succeeds when billing run is in an allowed status` – `TypeError: ...find is not a function`
7. `[PDT-2023] TC-8: Status transition to IN_PROGRESS_TERMINATION then CANCELLED (or direct to CANCELLED)` – `TypeError: ...find is not a function`
8. `[PDT-2023] TC-9: PATCH /billing-run/terminate returns error when status is already IN_PROGRESS_TERMINATION` – `TypeError: ...find is not a function`
9. `[PDT-2023] TC-10: PATCH /billing-run/terminate returns error when status is already CANCELLED` – `TypeError: ...find is not a function`
10. `[PDT-2023] TC-12: PATCH /billing-run/terminate returns error when status is COMPLETED` – `TypeError: ...find is not a function`
11. `[PDT-2023] TC-13: BillingRunService.cancel() performs full termination and sets final status CANCELLED` – `TypeError: ...find is not a function`
12. `[PDT-2023] TC-15: resume() rejects billing run in status IN_PROGRESS_TERMINATION` – `TypeError: ...find is not a function`
13. `[PDT-2023] TC-16: resume() rejects billing run in status CANCELLED` – `TypeError: ...find is not a function`
14. `[PDT-2023] TC-17: resume() allows billing run in a resumable status (e.g. PAUSED)` – `TypeError: ...find is not a function`
15. `[PDT-2023] TC-20: Concurrent start and terminate – consistent final state` – `TypeError: ...find is not a function`
16. `[PDT-2023] TC-22: Second terminate request while first is in progress or after CANCELLED is rejected` – `TypeError: ...find is not a function`
17. `[PDT-2023] TC-23: Billing run list displays runs in status IN_PROGRESS_TERMINATION with correct label` – `TypeError: runs.find is not a function`
18. `[PDT-2023] TC-24: Filter by status IN_PROGRESS_TERMINATION returns only runs in that status` – `TypeError: runs is not iterable`
19. `[PDT-2023] TC-25: API and enum include IN_PROGRESS_TERMINATION – no fixed enum assumption` – `TypeError: runs is not iterable`
20. `[PDT-2023] Terminate_allowed_statuses TC-1: Terminate billing run in INITIAL status` – `TypeError: ...find is not a function`
21. `[PDT-2023] Terminate_allowed_statuses TC-2: Terminate billing run in IN_PROGRESS_DRAFT status` – `TypeError: ...find is not a function`
22. `[PDT-2023] Terminate_allowed_statuses TC-3: Terminate billing run in DRAFT status` – `TypeError: ...find is not a function`
23. `[PDT-2023] Terminate_allowed_statuses TC-4: Terminate billing run in IN_PROGRESS_GENERATION status` – `TypeError: ...find is not a function`
24. `[PDT-2023] Terminate_allowed_statuses TC-5: Terminate billing run in GENERATED status` – `TypeError: ...find is not a function`
25. `[PDT-2023] Terminate_allowed_statuses TC-6: Terminate billing run in PAUSED status` – `TypeError: ...find is not a function`
26. `[PDT-2023] Terminate_allowed_statuses TC-7: Terminate rejected when billing run is already CANCELLED` – `TypeError: ...find is not a function`
27. `[PDT-2023] Terminate_allowed_statuses TC-8: Terminate rejected when billing run is in "in progress termination"` – `TypeError: ...find is not a function`
28. `[PDT-2023] Terminate_allowed_statuses TC-9: Terminate rejected when billing run is IN_PROGRESS_ACCOUNTING` – `TypeError: ...find is not a function`
29. `[PDT-2023] Terminate_button_and_in_progress_status TC-1: Terminate button visible and enabled when run is in allowed status` – `TypeError: ...find is not a function`
30. `[PDT-2023] Terminate_button_and_in_progress_status TC-3: API returns or reflects "in progress termination" when termination in progress` – `TypeError: ...find is not a function`
31. `[PDT-2023] Terminate_button_and_in_progress_status TC-4: Terminate button disabled or hidden when run is CANCELLED` – `TypeError: ...find is not a function`
32. `[PDT-2023] Terminate_button_and_in_progress_status TC-5: Terminate button disabled or hidden when run is "in progress termination"` – `TypeError: ...find is not a function`
33. `[PDT-2023] Lock_cleanup_and_resume_after_cancel TC-3: Resume or re-run after cancel – CANCELLED run does not block new or resumed run` – `TypeError: ...find is not a function`
34. `[PDT-2023] Concurrent_operations TC-1: Single terminate request wins when start and terminate are concurrent` – `TypeError: ...find is not a function`
35. `[PDT-2023] Concurrent_operations TC-2: Second terminate request rejected when first is already in progress` – `TypeError: ...find is not a function`
36. `[PDT-2023] Concurrent_operations TC-3: Start or resume rejected when run is already "in progress termination"` – `TypeError: ...find is not a function`
37. `[PDT-2023] Billing_run_list_and_filters TC-1: Billing run list includes runs in CANCELLED status` – `TypeError: runs is not iterable`
38. `[PDT-2023] Billing_run_list_and_filters TC-2: Billing run list includes runs in "in progress termination" status` – `TypeError: runs.find is not a function`
39. `[PDT-2023] Billing_run_list_and_filters TC-3: Filter by status CANCELLED returns only CANCELLED runs` – `TypeError: runs is not iterable`
40. `[PDT-2023] Billing_run_list_and_filters TC-4: Filter by "in progress termination" returns only runs in that status` – `TypeError: runs is not iterable`
41. `[PDT-2023] Billing_run_list_and_filters TC-5: List does not show runs in undefined or wrong status after termination` – `TypeError: runs is not iterable`

