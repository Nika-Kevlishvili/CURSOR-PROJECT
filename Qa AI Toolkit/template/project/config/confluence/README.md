# Confluence REST read fallback (Cursor / agents)

When **Confluence MCP** is unavailable, use **read-only** Confluence Cloud REST per **`.cursor/rules/integrations/confluence_rest_fallback.mdc`** (Rule **CONFLUENCE.1** / workflow Rule **43**).

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONFLUENCE_WIKI_BASE` | Recommended | Wiki API origin — **`https://asterbit.atlassian.net/wiki`** for Phoenix Confluence (user links: Rule LINK.1 in `atlassian_link_format.mdc`) |
| `CONFLUENCE_URL` | Optional fallback | e.g. `https://your-site.atlassian.net/wiki/home` — script normalizes to wiki base |
| `JIRA_BASE_URL` | Optional fallback | e.g. `https://your-site.atlassian.net` — script may derive `https://your-site.atlassian.net/wiki` when wiki vars are unset |
| `JIRA_EMAIL` | Yes* | Atlassian account email (shared with Jira on same cloud) |
| `JIRA_API_TOKEN` | Yes* | API token from [Atlassian API tokens](https://id.atlassian.com/manage-profile/security/api-tokens) |
| `CONFLUENCE_EMAIL` | Optional | Overrides `JIRA_EMAIL` for Confluence REST only |
| `CONFLUENCE_API_TOKEN` | Optional | Overrides `JIRA_API_TOKEN` for Confluence REST only |

\*Or the `CONFLUENCE_*` override pair if you use a separate technical user.

## Script

**Fetch one page by numeric ID** (from the page URL):

```powershell
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/confluence/get-confluence-page-rest.ps1" -PageId "779517953"
```

Optional: `-OutFile "page.json"` to save JSON. Optional: `-Api v1` (default) or `-Api v2`.

## Chat disclosure

When the assistant used REST instead of MCP: include **`Confluence source: REST fallback (MCP unavailable or failed after retries).`**
