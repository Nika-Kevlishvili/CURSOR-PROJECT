# EnergoTS Playwright Test Run Command

Route requests to **run** specific Playwright tests from EnergoTS (local repo synced from GitHub) to the EnergoTS Playwright Test Runner workflow. Tests run **only from the `cursor` branch** (Rule ENERGOTS.0). Use when the user wants to execute tests based on a prompt (e.g. "run newly created test", "run test REG-123", "run this test from GitHub").

## When to Use

Use this command when the user asks to:
- Run a Playwright test from EnergoTS / from GitHub
- Run the newly created test
- Run a specific test (by name, Jira key like REG-123, or file path)
- Execute a single test file or a set of tests (e.g. by domain)

## Workflow

1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Ensure `cursor` branch** – In `Cursor-Project/EnergoTS/`, check `git branch --show-current`. If not `cursor`, run `git checkout cursor`. Tests must run only from the `cursor` branch (Rule ENERGOTS.0).
3. **Resolve test from prompt** – Determine which test(s) to run:
   - "Newly created" → most recent `.spec.ts` in `Cursor-Project/EnergoTS/tests/`
   - Jira key (e.g. REG-123) → grep in tests, then run matching file(s) or `npx playwright test --grep "REG-123"`
   - File path/name → run that path relative to `EnergoTS/`
4. **Execute** – From `Cursor-Project/EnergoTS/`: `npx playwright test <path|grep|dir>`
5. **Report** – Summarize results; save report to `Cursor-Project/reports/YYYY-MM-DD/` (Rule 0.6); end with "Agents involved: EnergoTS Playwright Test Runner".

## Important

- **Cursor branch only** (Rule ENERGOTS.0): Tests run **only** from the `cursor` branch. Before running, ensure EnergoTS is on `cursor` (check and `git checkout cursor` if needed). Never run from main or other branches.
- **From GitHub**: Tests are run from the **local** EnergoTS repo (synced from GitHub), on branch `cursor`. If the user explicitly wants the latest from main, they must request update from main first; do not auto-sync.
- **No code changes**: Only run tests; do not modify source or test code (Rule 0.8).
- **EnergoTS path**: Default `Cursor-Project/EnergoTS/`; use `playwright.config.*` / `package.json` to confirm.

## Examples

- **User**: "გამიშვი ახალი შექმნილი ტესტი" / "Run the newly created test"  
  **Action**: Find latest `.spec.ts` in `EnergoTS/tests/`, run it.

- **User**: "Run test REG-123"  
  **Action**: Grep for REG-123 in tests, run matching test(s).

- **User**: "Run customer.spec.ts"  
  **Action**: Run `npx playwright test tests/customers/customer.spec.ts` (or correct path).

- **User**: "Run Playwright tests from GitHub"  
  **Action**: Run from local clone; if user wants latest, suggest sync then run.
