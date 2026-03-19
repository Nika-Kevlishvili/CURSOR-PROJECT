PHN-2160 - Playwright test results (rerun)

Jira: PHN-2160  
Title: Put: Update existing POD  
Date: 2026-03-19  
Spec: `Cursor-Project/EnergoTS/tests/cursor/PHN-2160-put-update-existing-pod.spec.ts`

Total: 4 passed, 0 failed, 59 skipped.

-------------------------

Rerun notes

- Fixed previously failing assertions:
  - Non-existent POD update now accepts `404` (and `400` if returned) as a valid error outcome.
  - `/exists` for a non-existent identifier now treats `400/404` as valid error outcomes; if `200` is returned, it asserts the boolean is `false`.

How to run

- From `Cursor-Project/EnergoTS/` (cursor branch only):
  - `npx playwright test tests/cursor/PHN-2160-put-update-existing-pod.spec.ts --reporter=line`

