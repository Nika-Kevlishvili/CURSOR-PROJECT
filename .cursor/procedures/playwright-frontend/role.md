# Playwright Frontend role

Role contract for the `/playwright-frontend` command. This file is not a subagent and does not appear in the `/` menu.

**Procedure (HOW):** `.cursor/procedures/playwright-frontend/procedure.md` — read before any `src/frontend` spec or page-object edit.

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| Jira key or topic | Yes | Resolves `test_cases/Frontend/<Topic>.playwright.md` |
| Target env | Yes | dev, dev2, test, preprod, prod, experiments. Ask if missing |
| Playwright instructions | Yes | `Cursor-Project/test_cases/Frontend/<Topic>.playwright.md` from test-case-generator |
| Frontend TC markdown | Yes | `Cursor-Project/test_cases/Frontend/<Topic>.md` (TC-FE-N) |

## Outputs

- Updated page object under `Cursor-Project/EnergoTS/src/frontend/pom/` when the merged UI added controls the POM does not expose
- Specs under `Cursor-Project/EnergoTS/src/frontend/`
- One validator critique applied (no second validator pass)
- Chat execution report: passed, failed, product code defect, automation defect

## Authoring contract

1. EnergoTS HEAD must already be `cursorMerged`. If it is not, stop. Do not checkout or switch.
2. Inspect `Cursor-Project/Phoenix/phoenix-ui` with read-only git (`fetch`, `log`, `show`, `diff`). Do not checkout Phoenix.
3. Update the existing POM before writing specs. Record missing locators with `playwright-cli` (same rules as the `pom` skill). Do not use Playwright MCP browser tools.
4. Specs follow `src/frontend/meterCreate.spec.ts`: API preconditions inside `test.step`, `Responses` for JSON, `FrontendResponses` for the on-screen name, then the POM method.
5. Read `.cursor/procedures/playwright-frontend-validator/procedure.md` once and apply that critique. Do not call it again. Do not look for a slash command named playwright-frontend-validator.
6. Run `--project=frontend` and classify each failure.

## Constraints

- Writes only under `Cursor-Project/EnergoTS/**` on `cursorMerged`. Phoenix stays read-only (Rule 0.8 Tier A).
- API specs under `src/tests/` stay with **energo-ts-test**.
- English artifacts (Rule 0.7). PreProd or Prod execution needs explicit user approval.

## Footer

`Agents involved: PlaywrightFrontendAgent` (+ PlaywrightFrontendValidatorAgent after the one critique; + PhoenixExpert if business rules were unclear)
