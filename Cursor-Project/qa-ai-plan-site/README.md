# Asterbit QA AI one-year plan site

Static site for the Q4 2026 – Q3 2027 QA AI plan. Content comes from `data/plan.json` (same structure as the Cursor canvas). Jira status overlay comes from `data/jira-map.json` + `data/jira-status.json`.

## Local preview

```powershell
cd Cursor-Project\qa-ai-plan-site
.\scripts\sync-jira.ps1
.\scripts\serve.ps1
```

Opens `http://127.0.0.1:4173/`. Do not open `index.html` as a file — the browser blocks `fetch` of JSON.

Requires `JIRA_EMAIL` and `JIRA_API_TOKEN` in the environment or a workspace `.env` (same as the existing Jira REST script).

## Update the plan

1. Edit `data/plan.json`.
2. If a task now has a Jira ticket, add it in `data/jira-map.json`:

```json
{
  "epic": "GB-1705",
  "tasks": {
    "T5": "GB-1778"
  }
}
```

3. Run `.\scripts\sync-jira.ps1` then `.\scripts\publish.ps1`.

## Publish to GitHub Pages (shareable link)

One-time:

```powershell
gh auth login
cd Cursor-Project\qa-ai-plan-site
.\scripts\publish.ps1
```

Default repo: `Nika-Kevlishvili/asterbit-qa-ai-plan` (override with `-Repo owner/name` or `GH_PAGES_REPO`). The script creates a **separate public** repo that contains only this site — not the main Cursor workspace.

After Pages is enabled, the URL is:

`https://nika-kevlishvili.github.io/asterbit-qa-ai-plan/`

On that GitHub repo, add Actions secrets `JIRA_EMAIL` and `JIRA_API_TOKEN` so weekday sync can refresh ticket status without a local run.

## Notes

- The published site is public. Anyone with the link can read names, interview quotes, and Jira statuses.
- Jira tokens are never written into the site files.
- Mapped today (title match with the September GB board): T5→GB-1778, P16→GB-1785, P9→GB-1787, P10→GB-1792.
