# Jira integration (local scripts)

This folder holds **Jira-related scripts and optional local exports**. Do not commit secrets; use environment variables or MCP configuration only.

## `download-jira-attachments.ps1`

Downloads issue attachments for analysis. Requires `JIRA_EMAIL`, `JIRA_API_TOKEN`, and `JIRA_BASE_URL` in `Cursor-Project/.env` or the environment (see `Cursor-Project/Cursor Setup/env.example` header — do not duplicate live tokens in docs).

## `jira_bug_validator.py` (`Cursor-Project/scripts/jira_bug_validator.py`)

Optional batch helper that queries Jira and invokes `cursor agent` with validation-related prompts.

| Variable | Required | Description |
|----------|----------|-------------|
| `JIRA_EMAIL` | Yes | Atlassian account email |
| `JIRA_API_TOKEN` | Yes | API token |
| `JIRA_BASE_URL` | No | Default `https://oppa-support.atlassian.net` (no trailing slash required) |
| `JIRA_PROJECT_KEY` | No | Default `PDT` |
| `CURSOR_WORKSPACE` | No | Directory passed to `cursor agent --workspace`. Default: **repository root** (parent of `Cursor-Project/`) resolved from the script path |
| `CURSOR_WORKSPACE_ROOT` | No | Alias for the same default as `CURSOR_WORKSPACE` when you prefer that name |

Placeholders for documentation only (never paste real tokens here):

```text
JIRA_EMAIL=you@example.com
JIRA_API_TOKEN=your_token_here
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_PROJECT_KEY=YOURKEY
CURSOR_WORKSPACE=D:\path\to\git\workspace\root
```

## Local exports and `.gitignore`

By default **`config/jira/attachments/`** is ignored by Git under `Cursor-Project/.gitignore` to avoid committing PII and large JSON exports.

If you need **redacted fixtures** for tests or demos, use a dedicated path such as `config/jira/samples/` with manually redacted JSON and **do not** put live tokens or personal data in the repo. Adjust `.gitignore` only with team agreement.
