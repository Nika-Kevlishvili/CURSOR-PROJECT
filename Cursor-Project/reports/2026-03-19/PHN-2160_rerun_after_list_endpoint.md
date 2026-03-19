PHN-2160 - Playwright rerun after POD list endpoint fix

Jira: PHN-2160
Title: Put: Update existing POD
Date: 2026-03-19
Spec: `Cursor-Project/EnergoTS/tests/cursor/PHN-2160-put-update-existing-pod.spec.ts`

Result summary

- Passed: 14
- Failed: 15
- Skipped: 34
- Total: 63

Key outcome

- The skip issue related to POD id discovery was improved by using:
  - `http://10.236.20.11:8091/pod/list?page=0&size=25&sortBy=ID&sortDirection=DESC`
- Compared to previous rerun, many tests moved from skipped to executed.

Next step

- Align update/get/exists endpoint assertions and expected status contracts to reduce failures.

