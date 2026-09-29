---
name: pom
description: Builds and wires a Playwright Page Object Model from a live create page, adds one typed happy-path creation workflow, generates one frontend creation spec, runs it, and verifies the object was created. Use only when explicitly invoked with /pom.
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

## Playwright MCP

Use the Playwright MCP server (`@playwright/mcp`). Before any browser call:

1. Discover the server with `GetDynamicTools` (pattern `playwright`).
2. Choose the namespace whose tools include `browser_navigate` and `browser_snapshot`.
3. Read each tool schema with `GetDynamicTools` before calling it. Do not guess argument names.
4. If that server is not connected, stop and tell the user Playwright MCP is required. Do not fall back to HTML files, a recorded spec, or another browser.

MCP element refs from `browser_snapshot` are session-only. Never store a ref such as `e12` in the POM.

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

1. Open the user-supplied URL with `browser_navigate`.
2. Wait until the screen settles (`browser_wait_for` or an equivalent ready check exposed by the server).
3. If the Energo-Pro login page is showing (Welcome, Username, Password, Log in), sign in before doing anything else. The login page is not the POM target.
4. After login, the portal application list opens (Administrative view). Enter dev 2 by clicking the Phoenix 2 tile before crawling. That list is not the POM target.
5. If the user named a page inside the app and the browser is not on it yet, open that URL only after the dev 2 app has loaded.
6. Confirm the landed URL and the visible heading match the page the user named. An error page or empty shell is not the target. If they do not match, stop and report what opened.

### Login

Read credentials only from `Cursor-Project/EnergoTS/.env`:

- Username: `PORTAL_USER`
- Password: `PASSWORD`

Parse the file. Trim whitespace and strip one pair of surrounding single or double quotes. Do not invent credentials, and do not take them from chat, Confluence, or any other file.

If either variable is missing or empty, stop and say which name is missing. Do not ask the user to paste a password.

Then, using Playwright MCP:

1. Snapshot the login page.
2. Fill the Username field with `PORTAL_USER`.
3. Fill the Password field with `PASSWORD`. Leave the Language dropdown as it is unless the user asked for another language.
4. Click **Log in**. This is the one submit that is required. The later "do not mutate" rule does not apply to this button.
5. Wait until the login form is gone and the portal application list is visible.

### Enter dev 2

On the application list, click the tile marked for dev 2:

- Visible label: **EPRS Phoenix 2 End Supplier and SLR**
- Card header tooltip: **Phoenix 2 ES/SLR FE -> phoenix2**

Click the tile body itself (the phoenix logo and that label). Do not click **Other frontends**, **Administration**, search, or any other application card.

If the click opens a new tab, switch to that tab. Wait until the Phoenix dev 2 app has loaded (`phoenix-dev2` or the dev 2 shell), then continue to the page the user named.

Never print, log, or write the password, the username, or any token into the POM, a locator inventory, or the chat summary. If login fails, or the Phoenix 2 tile is not on the list, report only the visible error text and stop.

## Crawl the page

Stay on this page. The goal is a complete locator inventory of the page and of UI that this page reveals, not a tour of the application.

### Inventory

After each settled screen state, call `browser_snapshot`. Record every meaningful control:

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

Dismiss with Escape, the same toggle, or an explicit Close or Cancel control. Use `browser_navigate_back` only when a click changed the route, then continue from the original page.

### Do not mutate data during discovery

Record the locator, and do not activate the action, for controls that persist, destroy, or send:

- Save, Submit, Create, Delete, Remove, Confirm, Approve, Reject, Pay, Generate, Upload, Logout
- a dropdown option or menu item that commits a business change
- anything that starts a download, file picker, or external site

If such a control opens a dialog before any commit, snapshot the dialog, record its locators, and dismiss it. Never confirm it.

This restriction applies to the MCP crawl. The generated happy-path test must submit the form later in the explicit execution phase.

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

When `browser_generate_locator` is available, use it as a candidate and keep it only if it matches this order. When `browser_evaluate` or `browser_run_code` is available, verify the chosen locator matches exactly one element in the correct scope.

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
5. Navigate to the supplied page and call only the POM's public creation method for the business flow. The spec may contain setup and assertions, but must not duplicate field-by-field actions.
6. Keep one test: the minimal successful creation path. Additional field-presence, dialog, and negative tests are out of scope unless the user asks.
7. Prove creation with at least two independent signals when the page exposes them:
   - the create/save network response succeeds;
   - the route changes to the created object's detail/edit page;
   - the created identifier/name/customer number is visible;
   - a success notification is visible.
8. Merely seeing the Create/Save button, clicking it, or observing no exception is not proof of creation.
9. Do not delete the created object. The user's goal is to leave a successfully created test object.

## Execute and repair

1. Run the narrow spec with the frontend Playwright project.
2. If it fails, use the trace, screenshot, console, network response, and visible validation messages to repair the POM or spec.
3. Re-run after each repair, up to three focused iterations. Do not weaken assertions to make a failure pass.
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
3. No MCP ref, token, session value, username, or password appears in the POM.
4. Imports, constructor dependencies, and registry wiring agree when a registry exists.
5. Format changed TypeScript consistently with nearby files.
6. Run the narrowest available TypeScript or lint check, then a broader typecheck when practical.
7. Confirm the public creation method is the last workflow-level method in the class and uses private atomic helpers.
8. Confirm the spec supplies the method arguments and does not repeat raw form interactions.
9. Confirm the spec is discovered by the frontend Playwright project.
10. Run the spec and confirm both Playwright success and object-creation evidence.
11. Run the Playwright test validator after authoring. Fix introduced errors; report unrelated pre-existing failures separately.

Finish with the POM, registry/fixture/config changes, spec path, public creation method signature, generated object identifier, test command and pass result, creation evidence, recorded controls/revealers, skipped controls, and any blocker.
