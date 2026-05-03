# Playwright two-part reporting (summary + detailed file)

**Workspace rule:** `.cursor/rules/workflows/playwright_detailed_reporting.mdc` (Rule **DPR.0**). **`HandsOff`** and **path 3** (`send-playwright-results-slack`) **MUST** run **`generate-detailed-report.mjs`** after tests when **`playwright-report.json`** exists and **MUST** upload **`playwright-report-detailed.md`** to **#ai-report** (`C0AK96S1D7X`) and Tester (if any) **together with** the smart `.md` report. For **ad-hoc** runs outside those flows, generate/upload only when the user explicitly requests it.

**This folder (`Cursor-Project/config/playwright/`):** `generate-detailed-report.mjs`, **`Playwright_run_detailed_report_template.md`** (HandsOff / scoped Slack disk report structure). **Default machine output** is **not** here — it is written next to the JSON: **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** (gitignored under EnergoTS).

## What you get today

- **Summary-style outputs:** Playwright `html` + `json` reporters (see `EnergoTS/playwright.config.ts`), Slack upload of HTML/ZIP from existing utilities.
- **Second file — detailed (machine):** `EnergoTS/playwright-report-detailed.md`, from `EnergoTS/playwright-report.json` when you run the generator from **`Cursor-Project/EnergoTS/`** (default paths). One section per executed test — Jira + **TC-BE-n** / **TC-FE-n** + scenario parsed from the **`test()`** title when the title matches `[KEY] TC-BE-n: …`; see `generate-detailed-report.mjs`.

## Generate the detailed file

### One-shot (workspace script)

From the **workspace root** (`cursor-project`):

```powershell
powershell -ExecutionPolicy Bypass -File ".cursor/commands/playwright-test-with-detailed-report.ps1"
```

Pass through Playwright CLI args, for example:

```powershell
powershell -ExecutionPolicy Bypass -File ".cursor/commands/playwright-test-with-detailed-report.ps1" "--grep" "PDT-2599"
```

### Manual (only detailed MD, tests already ran)

From **`Cursor-Project/EnergoTS`** after a run (so `playwright-report.json` exists). Default output: **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** (same directory as the JSON).

```bash
node ../config/playwright/generate-detailed-report.mjs
```

Custom paths:

```bash
node ../config/playwright/generate-detailed-report.mjs ./playwright-report.json ./my-detailed.md
```

Include setup/teardown in the detailed file:

```bash
# PowerShell
$env:DETAILED_REPORT_SKIP_SETUP="0"
node ../config/playwright/generate-detailed-report.mjs
```

## Richer objective and created-data annotations

Use `test.info().annotate(...)` in specs; annotations appear in the machine detailed report. **HandsOff / path 3:** orchestrator runs the generator after tests when JSON exists (**Rule DPR.0**). **Ad-hoc:** run only when the user explicitly requests it.

**Title convention for TC rows:** use **`[JIRA-KEY] TC-BE-n: Short scenario`** (or **TC-FE-n**) in the Playwright `test('…')` title so the machine report shows the exact test-case id the automation covers.
