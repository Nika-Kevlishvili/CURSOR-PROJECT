# Link check: `.cursor` rules + templates

**Rules paths:** All explicit `.cursor/rules/<folder>/<file>.mdc` references under `.cursor/` and checked `Cursor-Project/docs` entries match on-disk layout (`main/`, `safety/`, `agents/`, `workflows/`, `workspace/`, `integrations/`).

**Fixed (were wrong):** `Cursor-Project/config/Test_case_template.md` and `Cursor-Project/config/Slack_report_template.md` → **`Cursor-Project/config/template/...`** in:
- `.cursor/rules/workflows/handsoff_playwright_report.mdc`
- `.cursor/rules/workspace/test_cases_structure.mdc`
- `.cursor/commands/test-case-generate.md`, `hands-off.md`
- `.cursor/agents/test-case-generator.md`, `playwright-test-validator.md`

**Fixed:** `hands-off.md` removed broken reference to deleted `Cursor-Project/agents/Services/reporting_service.py`; replaced with Rule 0.6 + `reports/YYYY-MM-DD/`.

**Note:** Other files still mention Python `ReportingService` / `agents/` paths (legacy text); not part of this path existence check.

**Agents involved:** None (verification + targeted edits).
