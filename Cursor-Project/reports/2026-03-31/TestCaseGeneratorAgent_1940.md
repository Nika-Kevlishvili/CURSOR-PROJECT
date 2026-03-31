# TestCaseGeneratorAgent – Playwright instructions integration

**Date:** 2026-03-31  
**Task:** Bind TestCaseGeneratorAgent to the user-provided Playwright instruction folder so test case generation is mandatory and aligned with EnergoTS spec patterns.

## Changes

- **Skill:** `.cursor/skills/test-case-generator/SKILL.md` — new section “Mandatory: Playwright instructions”; read order; extra `*.md` in folder; bridge semantics (`test.step`, HTTP/status/body).
- **Agent:** `.cursor/agents/test-case-generator.md` — section “0. MANDATORY – Playwright instructions”.
- **Command:** `.cursor/commands/test-case-generate.md` — Step 2 item 0 before generate.
- **Rule 35:** `.cursor/rules/workflows/workflow_rules.mdc` — Step 2 bullet + workflow summary step 3 updated.
- **HandsOff:** `.cursor/commands/hands-off.md` Step 3 item 0; `.cursor/agents/hands-off.md` Step 3 preamble.
- **Lessons:** `.cursor/rules/workflows/handsoff_playwright_report.mdc` §1 — new Rule (Playwright instructions).
- **Phoenix commands skill:** `.cursor/skills/phoenix-commands/SKILL.md` — test-case-generate flow lists mandatory read of instruction folder.
- **README:** `.cursor/agents/README.md` — test-case-generator row updated.

## Canonical path

`Cursor-Project/config/playwright_generation/playwright instructions/`  
(read: `project-description.md` → `general-rules.md` → `test-writing-rules.instructions.md` → `SKILL.md` → other `*.md` alphabetically; ignore `__MACOSX` / `._*`).

## Note

Template `Cursor-Project/config/template/Test_case_template.md` remains mandatory for document structure; Playwright pack shapes automation-ready steps and expectations.
