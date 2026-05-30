---
name: energo-ts-test
description: Manages EnergoTS Playwright test automation under Cursor-Project/EnergoTS/tests/ only (*.spec.ts, *.fixtures.ts). Rule 0.8.1 sole writer. Mandatory Swagger refresh + playwright instructions pack before edits. HandsOff Step 4.
---

# EnergoTS Test Skill

**Subagent (I/O):** `.cursor/agents/energo-ts-test.md`  
**Validator after authoring:** `playwright-test-validator` (HandsOff Step 4.5)

## When to apply

- HandsOff Step 4 — map test cases → `EnergoTS/tests/cursor/{KEY}-*.spec.ts`
- User asks to create, modify, or analyze EnergoTS Playwright tests
- Any write under `Cursor-Project/EnergoTS/tests/` (**`.spec.ts`**, **`.fixtures.ts`** only — hooks enforce)

## Mandatory before `.spec.ts` / `.fixtures.ts` edits

1. **Playwright instructions pack** — read **`Cursor-Project/config/playwright_generation/playwright instructions/`** in order: `project-description.md` → `general-rules.md` → `test-writing-rules.instructions.md` → `SKILL.md` → other `*.md` alphabetically. Ignore `__MACOSX` / `._*`.
2. **Swagger refresh (Rule 41):**
   ```powershell
   powershell -ExecutionPolicy Bypass -File ".cursor/commands/update-swagger-specs.ps1"
   ```
   Grep `Cursor-Project/config/swagger/{env}/swagger-spec.json` for each endpoint; validate field names, enums, types from spec — **not** from TC .md alone.
3. **Reference specs (multi-entity chains):** Grep `EnergoTS/tests/` (prefer `tests/cursor/`, then domain specs). Read one reference end-to-end. Entity order → **`precondition-data-creation.instructions.md`** unless citing a deliberate reference. Summary must list **Reference spec(s):** paths.

## Test creation workflow

1. **Jira** — read title + description (MCP or REST).
2. **Requirements** — endpoints, methods, payloads, edge cases; scope negatives to business behavior unless auth/routing explicitly in scope.
3. **Swagger** — per-endpoint schema validation (Step 2 above).
4. **Clarify** — ask if requirements ambiguous.
5. **Author** — only after steps 1–4 complete.

## Test naming [CRITICAL]

Format: `test('[JIRA-KEY]: {Exact Jira Task Title}', async ({...}) => {`

- **Exact** Jira title — no abbreviations, no added "| Happy path" unless in Jira title.

## HandsOff bridge (test cases → spec)

**Inputs:** Backend TC path (required); Frontend path when exists; Jira key + title.

1. Read all provided `.md` files; extract TC-BE-N / TC-FE-N, steps, expected results, endpoints.
2. One `test()` per main TC scenario; titles include Jira key.
3. **Output:** `EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`
4. EnergoTS fixtures only — no ad-hoc `getToken()` / `apiRequest()`.

## Precondition rules [CRITICAL]

- **No** `test.beforeAll` / `beforeAll` for data setup (Rule 40).
- Helpers at file top + `test.step('Precondition: …')` per test.
- Per-test **delta** preconditions when TCs differ — no identical setup claiming different scenarios.
- Follow **`precondition-data-creation.instructions.md`** entity order.

## Negative assertions

- Assert intended business rejection: exact status, error code/message, field.
- Do not use wrong URLs to force 404 unless routing is in scope.
- 401/403 only for permission tests.

## Framework quick reference

- Playwright API, TypeScript, `baseFixture` (Request, Endpoints, GeneratePayload, Responses)
- Assertions: prefer `await expect(response).CheckResponse()` for POST/create chains
- Branch: **`cursor`** only (Rule ENERGOTS.0)

## Permissions

- ✅ `EnergoTS/tests/**/*.spec.ts`, `*.fixtures.ts`
- ❌ EnergoTS outside `tests/`; ❌ Phoenix (Tier A)

## Completion

- Summary + **Confidence** (CONF.1) + `Agents involved: EnergoTSTestAgent`
