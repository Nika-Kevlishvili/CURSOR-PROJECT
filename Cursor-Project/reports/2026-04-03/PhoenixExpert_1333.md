# PhoenixExpert Report — Fix Stale Test Case References

**Date:** 2026-04-03 13:33
**Agent:** PhoenixExpert
**Task:** Verify and fix remaining stale references to old test case structure (Objects/, Flows/, single-file) across all rules, agents, commands, and skills.

## Summary

The user asked whether the rules and test case generation logic had been properly updated. An audit revealed that while most configuration files had been correctly updated to the `Backend/` + `Frontend/` two-folder layout, **9 stale references** remained across 7 files. All were fixed.

## Files Fixed

| # | File | Issue | Fix |
|---|------|-------|-----|
| 1 | `.cursor/agents/hands-off.md` (Step 3) | Said "single file" | Changed to "two separate files" — Backend/ + Frontend/ |
| 2 | `.cursor/agents/energo-ts-test.md` (line 114) | Used single-file path | Changed to two-path input (Backend + Frontend) |
| 3 | `.cursor/agents/playwright-test-validator.md` (Input) | Used single-file path | Changed to two-path input |
| 4 | `.cursor/agents/playwright-test-validator.md` (Coverage) | Referenced "the .md file" | Changed to "Backend file + Frontend file" |
| 5 | `.cursor/agents/README.md` (line 24) | Said single file with Backend/Frontend split | Changed to two files |
| 6 | `.cursor/commands/hands-off.md` (Step 3.2) | Said "single file" with old path | Changed to two separate files |
| 7 | `.cursor/commands/hands-off.md` (Step 3.3-5) | Referenced single file verify/content | Changed to both files |
| 8 | `.cursor/commands/hands-off.md` (Step 4.5) | Referenced Flows/ path | Changed to Backend/ + Frontend/ paths |
| 9 | `.cursor/commands/hands-off.md` (Step 4.2) | Used old single-file path | Changed to two-path input |
| 10 | `.cursor/commands/energo-ts-test.md` (line 37) | Referenced Flows/ path | Changed to Backend/ + Frontend/ paths |
| 11 | `.cursor/commands/energo-ts-test.md` (line 40) | Referenced "file(s)" generically | Changed to "both files (Backend and Frontend)" |
| 12 | `.cursor/skills/phoenix-commands/SKILL.md` (line 84) | Referenced Objects/ and Flows/ | Changed to Backend/ and Frontend/ |
| 13 | `.cursor/skills/README.md` (line 17) | Referenced Objects/ and Flows/ | Changed to Backend/ and Frontend/ |
| 14 | `.cursor/rules/workflows/handsoff_playwright_report.mdc` (checklist) | Said "in the .md file" | Changed to "from Backend file + Frontend file" |

## Verification

Final grep sweep for `Objects/`, `Flows/`, and `single file` across all `.md` and `.mdc` files under `.cursor/` returned **no matches**. All references are now consistent with the two-folder Backend/Frontend layout.

## Files Already Correct (no changes needed)

- `.cursor/rules/workspace/test_cases_structure.mdc` — already defines two-folder layout
- `.cursor/rules/workspace/file_organization_rules.mdc` — already references two-folder structure
- `.cursor/rules/workflows/workflow_rules.mdc` — already references two separate files
- `.cursor/rules/workflows/handsoff_playwright_report.mdc` (§1, §2) — already references Backend/Frontend
- `.cursor/skills/test-case-generator/SKILL.md` — already references two files
- `.cursor/skills/phoenix-file-organization/SKILL.md` — already references Backend/ + Frontend/
- `.cursor/agents/test-case-generator.md` — already references two-folder layout
- `.cursor/commands/test-case-generate.md` — already references Backend/ + Frontend/
- `Cursor-Project/config/template/Test_case_template.md` — already defines two-folder template
