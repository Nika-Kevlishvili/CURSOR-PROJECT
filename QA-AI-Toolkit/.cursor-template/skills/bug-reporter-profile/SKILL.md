---
name: bug-reporter-profile
description: Uses the Jira identity and board saved by the toolkit installer on every bug. The reporting procedure stays in the AI project that owns the reporter.
---

# Bug reporter profile

The installer writes these keys into the target `.env` and does not continue until they are set:

- `JIRA_EMAIL`
- `JIRA_API_TOKEN`
- `JIRA_BASE_URL`
- `JIRA_REPORTER_EMAIL`
- `JIRA_PROJECT_KEY`

When writing a bug:

1. The reporter is always `JIRA_REPORTER_EMAIL`.
2. The board is `JIRA_PROJECT_KEY`, unless the user names a different project in the current message.
3. Follow the bug reporter procedure from the AI project that owns the reporter: review file, approval, then create. Do not invent a second workflow in this toolkit.
4. Field ids come from that board's createmeta.
