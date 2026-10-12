---
name: pom
description: Builds and wires a Playwright Page Object Model from a live create page, adds one typed happy-path creation workflow, generates one frontend creation spec, runs it, and verifies the object was created. Use only when explicitly invoked with /pom. Drives the live page with playwright-cli, not Playwright MCP.
disable-model-invocation: true
---

# Playwright POM Builder

Build or update one Page Object Model from a live page, then prove it with one successful frontend creation test. The page the user names is the only UI evidence source. Do not ask for an HTML snapshot or a recorded `.spec.ts`.

A successful `/pom` invocation has two mandatory deliverables:

1. A wired POM with a typed public orchestration method that creates the page's object.
2. One happy-path spec under `src/frontend/` that calls that method, runs successfully, and verifies creation.

All EnergoTS paths below are under `Cursor-Project/EnergoTS/`.

## Invocation

The user must name the page. A full URL is enough:

```text
/pom https://devapps.energo-pro.bg/app/phoenix1-dev/customers
/pom https://.../customers/create create
/pom /customers/123/edit edit
```

- The page target is the first URL or in-app path in the command.
- `create` and `edit` are optional. Infer the mode from the path, the visible screen, and existing POM names. Ask only when the mode would change which class or folder to write.
- If the target is a path rather than an absolute URL, resolve the frontend origin from `Cursor-Project/EnergoTS/src/frontend/pom/MainPage.ts` using the environment the user named. If no environment was named, ask which one (dev, dev2, test, preprod, prod, experiments) and stop.
- If no page target is present, ask for the URL or path and stop. Do not explore the app to guess the page.

## Playwright CLI

Drive the live page with `playwright-cli` only. Do not call Playwright MCP (`@playwright/mcp`, `browser_navigate`, `browser_snapshot`, or any other `browser_*` MCP tool). Do not fall back to HTML files, a recorded spec, or another browser.

Before the first browser command:

1. Confirm `playwright-cli` is on `PATH` (`playwright-cli --version`). If it is missing, stop and tell the user to install `@playwright/cli`. When a global install into `/usr/local` is not writable, the working install on this machine is `npm install -g @playwright/cli@latest --prefix "$HOME/.local"` (`~/.local/bin` is already on `PATH`).
2. Read `.agents/skills/playwright-cli/SKILL.md`. That skill is the command reference. Cursor also loads it through `.cursor/skills/playwright-cli`.
3. Run every `playwright-cli` command from the workspace root, so snapshots stay under the gitignored `.playwright-cli/` directory.

Use one named session for the crawl: `playwright-cli -s=pom ...`. Open it headed, with the Chrome channel the installer already found. Sign in by opening the frontend root with `attachFrontendJwt`, then go to the named page. Do not open the portal login page.

```bash
playwright-cli -s=pom open "<frontend-root-with-jwt>" --browser=chrome --headed
```

Headed mode keeps the session alive for a long crawl. A headless session shuts down after an hour without a command.

Refs such as `e12` come from the latest snapshot and die when the page changes. Re-snapshot before the next ref. Never store a ref in the POM.

After each command, read the snapshot file the CLI prints (`.playwright-cli/page-*.yml` or the `--filename` you passed). For every settled inventory state, also save a named copy and read that file:

```bash
playwright-cli -s=pom snapshot --filename=.playwright-cli/pom/<state>.yml
```

A full snapshot file is required for the inventory. `find` only locates the next control. Do not call `webmcp-call`. Page-provided tools skip the locator inventory.

`generate-locator` is how a ref becomes POM source. It does not activate the control:

```bash
playwright-cli -s=pom generate-locator e5 --raw
```

Keep the result only when it matches the locator order below. Prove it matches one element:

```bash
playwright-cli -s=pom run-code "async (page) => page.getByRole('button', { name: 'Save' }).count()" --raw
```

When the snapshot has no test id, read it without clicking:

```bash
playwright-cli -s=pom eval "el => el.getAttribute('data-testid')" e5 --raw
```

Close the crawl session before the spec run: `playwright-cli -s=pom close`.

## Required project context

Before writing the POM:

1. Read `Cursor-Project/EnergoTS/src/frontend/.instructions.md` when it exists, and follow its POM conventions.
2. Read `Cursor-Project/EnergoTS/src/frontend/pom/MainPage.ts` and the closest existing POM for the same domain or mode.
3. When these files exist, read them too:
   - `Cursor-Project/EnergoTS/src/frontend/pom/PageObjects.ts`
   - `Cursor-Project/EnergoTS/src/frontend/fixtures/FrontEndFixture.ts`
   - `Cursor-Project/EnergoTS/src/frontend/utils/dropdownSelector.ts`
4. Follow current repository code when it conflicts with an outdated example in the instructions.
5. Absence of the optional files above is not a blocker. Do not invent a registry, fixture, or helper the repository does not already have.
6. Preserve unrelated user changes.
7. Before writing a `.spec.ts`, follow the EnergoTS Playwright authoring gate: read the available Playwright instructions pack, run the mandatory Swagger refresh, and record any refresh failure. A UI-only test does not need to invent or call an API endpoint.

## Reach the page

Do not use the Energo-Pro portal login form, do not fill `PORTAL_USER` or `PASSWORD`, and do not click an application tile such as Phoenix 2.

The Phoenix shell consumes `?jwt=` on the frontend root, stores it, and replaces that URL. A deeper page opened with `?jwt=` still attached stays blank, so do not put the token on the named page URL.

1. Read the JWT from `Cursor-Project/EnergoTS/src/backend/fixtures/token.json`, field `token`. `src/tests/setup/global-setup.ts` writes that file. If the file is missing or `token` is empty, stop and tell the user to run the Playwright setup project. Do not fall back to the portal login page.
2. Build the login URL with `attachFrontendJwt` from `Cursor-Project/EnergoTS/src/frontend/pom/MainPage.ts`, passing the frontend root from `getFrontendUrl()` (or the origin of the user-supplied URL). Shape: `https://devapps.energo-pro.bg/app/phoenix2-dev/?jwt=<token>`. Never print, log, or write the token or the full JWT URL into the POM, a locator inventory, or the chat summary.
3. Open that root URL with `playwright-cli -s=pom open "<frontend-root-with-jwt>" --browser=chrome --headed`. If that session is already open, use `goto`. Pass the URL without echoing it.
4. Wait until the shell is visible (`page.locator('.burger')`) and the `jwt` query parameter is gone. Use `run-code` with `waitFor` or `waitForURL`. Do not use a fixed sleep.
5. `goto` the user-supplied page without a `jwt` parameter. Do not use `history.pushState`.
6. If the portal login page is showing (Welcome, Username, Password, Log in), the token was rejected. Stop and report that. Do not fill credentials.
7. Confirm the landed URL and the visible heading match the page the user named. An error page or empty shell is not the target. If they do not match, stop and report what opened.

## Crawl the page

Stay on this page. The goal is a complete locator inventory of the page and of UI that this page reveals, not a tour of the application.

### Inventory

After each settled screen state, run `playwright-cli -s=pom snapshot --filename=.playwright-cli/pom/<state>.yml` and read that file. Record every meaningful control:

- buttons, links, icon buttons, tabs, menus
- text inputs, textareas, checkboxes, radios, date fields, file inputs
- dropdowns, comboboxes, selects, autocomplete
- accordions, expandable panels, drawers, dialogs, and their triggers
- tables and row actions that belong to this page
- disabled and read-only controls

Ignore scripts, styles, layout wrappers, and duplicated hidden templates.

### Open every revealer

Walk each control that discloses more UI. Do them one at a time, and return to a known state before the next one.

Open, snapshot, record, then dismiss:

- tabs and sub-tabs
- accordions and expandable panels
- dropdowns, comboboxes, and menus (record the visible options, then close without applying a change)
- toolbars and "more" menus
- dialogs, drawers, and overlays opened by a button on this page

Dismiss with `press Escape`, the same toggle, or an explicit Close or Cancel control. Use `go-back` only when a click changed the route, then continue from the original page. Use `dialog-dismiss` for a native browser dialog.

### Do not mutate data during discovery

Record the locator, and do not activate the action, for controls that persist, destroy, or send:

- Save, Submit, Create, Delete, Remove, Confirm, Approve, Reject, Pay, Generate, Upload, Logout
- a dropdown option or menu item that commits a business change
- anything that starts a download, file picker, or external site

If such a control opens a dialog before any commit, snapshot the dialog, record its locators, and dismiss it. Never confirm it.

This restriction applies to the CLI crawl. The generated happy-path test must submit the form later in the explicit execution phase.

### Bounds

- One page per invocation. A control that navigates to another route is recorded as navigation. Do not crawl the destination.
- Depth is the page, then one disclosed layer (panel, menu, dropdown, or dialog), then the controls inside that layer. Do not recurse through nested flows.
- Track triggers already opened (role + accessible name + section). Do not click the same trigger again.
- If the same pattern repeats in a table, record the row pattern once. Do not click every row.
- If the snapshot stops changing, or a control cannot be opened safely, record it and move on. Report skipped controls and why.

## Locator rules

Prefer the first locator that is unique and stable:

1. `getByRole` with the accessible name from the snapshot
2. `getByLabel`
3. `getByPlaceholder`
4. `getByTestId` when a `data-testid` (or the project's existing test id) is present
5. A scoped CSS or id locator only when the options above are missing or not unique

Use `generate-locator <ref> --raw` as the candidate and keep it only if it matches this order. Verify with `run-code` that the chosen locator matches exactly one element in the correct scope. Use `eval` when a `data-testid` or other attribute is missing from the snapshot. Read `.agents/skills/playwright-cli/references/element-attributes.md` before that `eval`.

Scope dialog, drawer, and menu controls to that overlay root when the same role and name also exist on the page underneath.

Never invent a locator, option, required flag, or modal step that the crawl did not show. If a dropdown list is truncated, type the parameter as `string` and do not claim the seen options are the full set.

## Build the POM

1. Determine the domain, page boundary, and create/edit mode.
2. Update an existing matching POM instead of creating a duplicate.
3. Keep one class for this page. Do not add classes for other routes the crawl refused to enter.
4. Declare locators for every recorded control, including disabled and read-only ones.
5. Initialize locators in the style required by `Cursor-Project/EnergoTS/src/frontend/.instructions.md` when that file exists; otherwise match the closest existing POM.
6. Disabled and read-only fields get locators and no mutation methods.
7. Put each required happy-path interaction in a private atomic method.
8. Do not put assertions of business outcome inside the POM.
9. Replace fixed sleeps with Playwright state waits that the crawl showed were needed (overlay visible, panel expanded, list rendered).
10. If `Cursor-Project/EnergoTS/src/frontend/pom/PageObjects.ts` exists, register a new facade there. Do not register internal helpers separately. If the registry does not exist, export the class from its file and do not create a registry.
11. Define and export a typed `<Object>CreateArguments` type. Required properties are fields needed by every successful minimal creation; optional properties represent optional fields or optional branches.
12. End the class with one public creation method named for the object, such as `createLegalCustomer(args)` or `createProduct(args)`. This is the POM's primary API.
13. The public creation method must call the private atomic methods in UI order, apply optional arguments conditionally, click the final Create/Save action, and wait for the observed submit transition. Do not generate test data inside the POM; the spec supplies all arguments.
14. Keep assertions in the spec. The POM may return observable creation data such as the submitted identifier or resulting URL when that helps verification.

The removed customer POMs before commit `b8021091c59581c6ae7e5a295b800e7ea741dffc` are the structural precedent: typed arguments, private field operations, and one public `create…()` method. Reuse the pattern, not their outdated locators.

## Build the happy-path spec

After the POM is wired:

1. Create or update exactly one focused spec in `Cursor-Project/EnergoTS/src/frontend/`.
2. Ensure `playwright.config.ts` has a reusable frontend project that discovers `src/frontend/**/*.spec.ts`; do not hardcode the project to one filename.
3. Use `FrontEndFixture.ts` and the registered `POM` facade. Repair missing fixture wiring when necessary.
4. Generate unique valid arguments in the spec. Never put credentials, tokens, or environment-specific session values in source.
5. Open the creation page with `openAuthenticatedFrontendPage` from `MainPage.ts`. That helper reads `src/backend/fixtures/token.json` through `attachFrontendJwt`, waits until the frontend root has consumed `?jwt=`, then opens the creation path with `page.goto`. Do not sign in through the portal, do not click an application tile, do not append `jwt` to the creation path, and do not use `history.pushState`. Then call only the POM's public creation method for the business flow. The spec may contain setup and assertions, but must not duplicate field-by-field actions.
6. Keep one test: the minimal successful creation path. Additional field-presence, dialog, and negative tests are out of scope unless the user asks.
7. Prove creation with at least two independent signals when the page exposes them:
   - the create/save network response succeeds;
   - the route changes to the created object's detail/edit page;
   - the created identifier/name/customer number is visible;
   - a success notification is visible.
8. Merely seeing the Create/Save button, clicking it, or observing no exception is not proof of creation.
9. Do not delete the created object. The user's goal is to leave a successfully created test object.

## Execute and repair

1. Close the `pom` CLI session, then run the narrow spec with the frontend Playwright project from `Cursor-Project/EnergoTS`. Set `PLAYWRIGHT_HTML_OPEN=never`.
2. If it fails, read `.agents/skills/playwright-cli/references/playwright-tests.md` and the Heal section of `references/test-generation.md`. Re-run that spec in the background with `--debug=cli`, attach with `playwright-cli attach <session>`, and inspect the paused page with `snapshot`, `console`, `requests`, and `generate-locator`. Use the trace, screenshot, console, network response, and visible validation messages as well.
3. Re-run after each repair, up to three focused iterations. Stop the background debug run before the next attempt. Do not weaken assertions to make a failure pass.
4. Finish only when the test passes and creation evidence is captured.
5. If authentication, environment availability, permissions, or product validation blocks creation, stop with the exact blocker and do not claim the workflow succeeded.
6. Creation tests may mutate Dev, Dev2, Test, or Experiments. PreProd or Prod requires explicit user approval before execution.

EnergoTS may be on `cursor` or `staging`. Do not refuse a write because the branch is `staging` or because the file is outside `tests/`.

## Output locations

- Create pages: `Cursor-Project/EnergoTS/src/frontend/pom/createPOM/`
- Edit pages: `Cursor-Project/EnergoTS/src/frontend/pom/editPOM/`
- Multi-tab pages: a domain subdirectory in the appropriate mode directory, with one facade class
- Happy-path spec: `Cursor-Project/EnergoTS/src/frontend/<ObjectCreate>.spec.ts`
- Also edit fixtures, helpers, registry files, and any other file under `Cursor-Project/EnergoTS/` required to compile or generate the frontend page object
- Do not edit `Cursor-Project/Phoenix/**`

Use names consistent with nearby code and the conventions file.

## Quality gate

Before finishing:

1. Every recorded control is a locator on the POM, or an intentional exclusion (mutating action that was recorded as locator-only, repeated table row, non-interactive markup).
2. Every panel, tab, dropdown, and dialog that was opened has its inner controls on the POM.
3. No CLI snapshot ref, token, session value, username, or password appears in the POM.
4. Imports, constructor dependencies, and registry wiring agree when a registry exists.
5. Format changed TypeScript consistently with nearby files.
6. Run the narrowest available TypeScript or lint check, then a broader typecheck when practical.
7. Confirm the public creation method is the last workflow-level method in the class and uses private atomic helpers.
8. Confirm the spec supplies the method arguments and does not repeat raw form interactions.
9. Confirm the spec is discovered by the frontend Playwright project.
10. Run the spec and confirm both Playwright success and object-creation evidence.
11. Run the Playwright test validator after authoring. Fix introduced errors; report unrelated pre-existing failures separately.

Finish with the POM, registry/fixture/config changes, spec path, public creation method signature, generated object identifier, test command and pass result, creation evidence, recorded controls/revealers, skipped controls, and any blocker.
