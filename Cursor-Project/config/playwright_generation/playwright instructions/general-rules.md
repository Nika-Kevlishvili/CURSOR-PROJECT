## Strict Rules

### Read-Only Files and Folders — DO NOT MODIFY

The following files and directories are **off-limits** and must never be created, edited, or deleted under any circumstances:

- `.github/` — entire directory including all subfolders (`workflows/`, `scripts/`, etc.)
- `playwright.config.ts`
- `utils/` — entire directory and everything inside it
- `scripts/` — entire directory and everything inside it
- `tests/setup/global-setup.ts`
- `tests/setup/global-teardown.ts`

### No New Dependencies

**Never install, add, or suggest new npm packages or Node modules.** Do not run `npm install`, `npm add`, `yarn add`, or modify `package.json` dependencies. Package sources cannot be verified and could introduce harmful or vulnerable code. Work exclusively with what is already installed.

### No System-Level Side Effects

Test code must **never** execute commands or operations that affect anything outside this project's own files. No shell commands that alter the host machine, no filesystem operations outside the project directory, no network calls to unknown endpoints, no modifications to system-wide configuration. All work must be strictly scoped to this repository's files and the APIs under test.

### Fixtures — Preserve Existing Architecture

The `fixtures/` directory should be treated with care. Do not restructure, refactor, or significantly alter existing fixture files. If something new needs to be added, follow the **exact existing patterns** (same coding style, same export conventions, same integration approach). No architectural changes — additions only, and only when necessary.