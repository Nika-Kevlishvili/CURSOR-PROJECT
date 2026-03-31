# Verification – Playwright instruction pack + agent wiring

**Date:** 2026-03-31

## Disk check (executed)

PowerShell `Test-Path` on **`Cursor-Project/config/playwright_generation/playwright instructions/`**:

| File | Status |
|------|--------|
| `project-description.md` | OK |
| `general-rules.md` | OK |
| `test-writing-rules.instructions.md` | OK |
| `SKILL.md` | OK |

## Wiring audit (repository)

References to **`Cursor-Project/config/playwright_generation/playwright instructions/`** now include:

- **Test cases:** `test-case-generator` skill/agent, `test-case-generate` command, Rule 35 (`workflow_rules.mdc`), HandsOff Step 3, `hands-off` agent, `handsoff_playwright_report.mdc` §1, `phoenix-commands` skill, `.cursor/agents/README.md`.
- **Playwright authoring:** `energo-ts-test` agent (Before Any Task step 0, HandsOff bridge step 0), `energo-ts-test` command (Mandatory Workflow step 0, HandsOff bridge), HandsOff Step 4 item 0, `handsoff_playwright_report.mdc` §2.
- **Validation:** `playwright-test-validator` (§5 `canon`, process steps 1/8/9, criterion `canon`), HandsOff Step 4.5 bullet, `handsoff_playwright_report.mdc` §2a.

## Limitations

- **Cursor** does not execute agent docs automatically; compliance depends on the model following these files in chat.
- **E2E HandsOff** was not run in this verification (no Jira/MCP full flow). Disk + grep consistency only.

## Fixes applied this session

Restored/made explicit **`energo-ts-test`** and **`playwright-test-validator`** links to the same instruction folder (they were missing from those agent files before this audit).
