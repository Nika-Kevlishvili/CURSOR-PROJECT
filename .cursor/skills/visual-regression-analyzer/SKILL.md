# Visual Regression Screenshot Analyzer

## Purpose

Analyze visual regression screenshots from EnergoTS Playwright tests against Confluence documentation and checklist requirements. Provide Pass/Fail verdicts for each screenshot.

## When to Use

- User asks to "analyze regression screenshots"
- User provides a screenshots folder path
- User asks "გააანალიზე screenshots" or similar

## Input

- Path to screenshots folder: `EnergoTS/tests/regression/screenshots/{run-id}/{section}/{sub-item}/`
- Structure mirrors Phoenix sidebar menu (`menu-structure.json`):
  - `listing/` — list view screenshots (filters, search, table)
  - `object/` — preview/detail/create form screenshots
- Contains: PNG files in subfolders + `manifest.json` at sub-item root

## Workflow

### Step 1: Read Manifest

```
Read: {screenshots-folder}/manifest.json
```

Extract:
- `runId`, `environment`, `baseUrl`, `section`, `subItem`
- `listing[]` and `object[]` (or flat `screenshots[]`) with `filename`, `relativePath`, `kind`, `stepName`, `description`, `checkItems`, `confluenceRef`

### Step 2: Read Checklist (if exists)

Check for checklist file in the test folder:
```
EnergoTS/tests/regression/{module}/{submodule}/checklist.md
```

### Step 3: For Each Screenshot

1. **Read the image file** using Read tool (supports images)
2. **Compare against checkItems** from manifest
3. **Check Confluence** if `confluenceRef` is provided (use Confluence MCP)
4. **Determine verdict**: PASS, FAIL, or WARNING

### Step 4: Generate Report

Format:

```markdown
## Visual Regression Analysis Report

### Run Info
- **Run ID:** {runId}
- **Environment:** {environment}
- **Test:** {testName}
- **Date:** {timestamp}

---

### Screenshot Analysis

#### {filename}
**Step:** {stepName}
**Description:** {description}

| Check Item | Result | Notes |
|------------|--------|-------|
| {checkItem1} | ✅ OK | {observation} |
| {checkItem2} | ❌ FAIL | {issue found} |
| {checkItem3} | ⚠️ N/A | {reason} |

**Verdict: ✅ PASS** (or ❌ FAIL)

---

### Summary
- **Total Screenshots:** {count}
- **Passed:** {passCount}
- **Failed:** {failCount}
- **Warnings:** {warnCount}

### Failed Items (Action Required)
1. `{filename}` - {issue description}
2. ...

### Confluence Mismatches (if any)
- Page {pageId}: Expected {X}, Found {Y}
```

## Analysis Guidelines

### What to Check in Screenshots

1. **Data Loading**
   - Tables have data (not empty)
   - Loading spinners are gone
   - Content is rendered

2. **UI Elements**
   - Buttons visible/hidden as expected
   - Required field indicators (*)
   - Status badges with correct colors
   - Tabs and navigation elements

3. **Layout**
   - Elements properly aligned
   - No overlapping content
   - Responsive to viewport

4. **Text Content**
   - Labels correct
   - Column headers match specification
   - Error/success messages appropriate

### Verdict Rules

- **✅ PASS**: All checkItems verified successfully
- **❌ FAIL**: One or more checkItems failed
- **⚠️ WARNING**: Unable to verify (element not visible, N/A state)

### Confluence Cross-Check

When `confluenceRef` is provided:
1. Fetch Confluence page content
2. Compare UI state against documented behavior
3. Report mismatches as Findings

## Example Usage

User: `გააანალიზე რეგრესიის screenshots: EnergoTS/tests/regression/screenshots/2024-07-03T14-30-22/product-contracts/`

Agent:
1. Reads `manifest.json`
2. Reads each PNG file
3. Reads `checklist.md` if exists
4. Optionally checks Confluence
5. Generates analysis report with verdicts

## Output Location

Analysis is provided in chat response. If user requests, save to:
`Cursor-Project/reports/Chat reports/{YYYY}/{month}/{DD}/VisualRegression_{HHMM}.md`

## Related Files

- Screenshots: `EnergoTS/tests/regression/screenshots/`
- Checklists: `EnergoTS/tests/regression/{module}/{submodule}/checklist.md`
- Playwright config: `EnergoTS/tests/regression/playwright.regression.config.ts`
