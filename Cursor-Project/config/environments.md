# Environments (HandsOff / EnergoTS)

This file documents **environment base URLs** used when running EnergoTS Playwright tests (including via `/hands-off`).

## experiment

- **Base URL:** `http://10.236.20.81:8094`

## Notes

- EnergoTS tests are executed from `Cursor-Project/EnergoTS/` (EnergoTS must remain on the `cursor` branch).
- The **environment selection affects only the target host** (BASE_URL / auth endpoints) used by Playwright setup and tests.
- If the feature exists only on a Phoenix Git branch (e.g. `experiment`) but is **not deployed** to the environment base URL above, tests will fail (typically 404 for missing endpoints).

