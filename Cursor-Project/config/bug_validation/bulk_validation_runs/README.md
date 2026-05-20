# bulk_validation_runs

This folder stores one JSON file per `/bug-validate-bulk` run.

Files are **gitignored** (local only) and survive context resets.

## File naming
`YYYY-MM-DD_HHMM_<label>.json`

Examples:
- `2026-05-13_1600_REG101-and-2-more.json`
- `2026-05-13_1600_EXP-customer-feedback.json`

## Loading in a new session
Tell the agent: "compare with last run" or "compare with [filename]".

See `.cursor/commands/bug-validate-bulk.md` for schema details.
