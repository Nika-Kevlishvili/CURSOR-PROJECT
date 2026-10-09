# Visual Regression Testing Suite

Phoenix frontend visual regression — modular by menu section, runnable individually or all together.

## Structure

```
regression/
├── config/menu-structure.json     # Phoenix sidebar menu (all sections + submenus)
├── recorded/
│   ├── reference-customers-flow.spec.ts   # Recording reference pattern
│   └── auth-state.json
├── modules/                       # One spec per menu section
│   ├── 01-shortcuts.spec.ts
│   ├── 02-customers.spec.ts
│   ├── ...
│   └── regression-all.spec.ts     # Full suite in one run
├── helpers/                       # Shared navigation, screenshots, flows
└── screenshots/                   # Generated output (gitignored)
    └── {run-id}/
        ├── 00-session/            # regression-all sidebar capture
        │   └── 01-sidebar/
        │       ├── listing/
        │       └── manifest.json
        └── 02-customers/          # section index + slug (menu-structure.json)
            ├── 01-customers/      # sub-item index + slug
            │   ├── listing/     # list view, filters, search, sort
            │   ├── object/      # preview / detail / create form
            │   └── manifest.json
            ├── 02-unwanted-customers/
            └── 03-groups-of-connected-customers/
```

## Run commands

### One section only
```bash
cd EnergoTS
npx playwright test tests/regression/modules/02-customers.spec.ts --config=tests/regression/playwright.regression.config.ts
```

### By tag (grep)
```bash
npx playwright test tests/regression/modules --config=tests/regression/playwright.regression.config.ts --grep @section-customers
```

### Full regression (all sections, menu order)
```bash
npx playwright test tests/regression/modules/regression-all.spec.ts --config=tests/regression/playwright.regression.config.ts
```

### Headed mode (watch browser)
```bash
npx playwright test tests/regression/modules/02-customers.spec.ts --config=tests/regression/playwright.regression.config.ts --headed
```

## Flow per submenu (from recording reference)

1. Open sidebar (hamburger if closed)
2. Click section (e.g. Customers)
3. Click submenu in flyout (e.g. Customers)
4. Screenshot list view → `listing/` (+ horizontal scroll)
5. **Filters** (open each dropdown) + **search** + **column sort** → more `listing/` shots
6. Open first row preview (dblclick)
7. Screenshot detail → `object/` (vertical sections)
7. Save `manifest.json` at sub-item root for AI analysis

## AI analysis after run

```
გააანალიზე რეგრესიის screenshots: EnergoTS/tests/regression/screenshots/<run-id>/02-customers/01-customers/
```

## Environment

- API: `BASE_URL` in `.env` (default `http://10.236.20.11:8091`)
- Frontend auto-mapped to `http://10.236.20.11:8080/`
- Auth via `fixtures/token.json` from global setup
