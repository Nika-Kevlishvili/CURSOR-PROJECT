# Atlassian REST Toolkit

Self-contained PowerShell scripts for reading data from **Jira** and **Confluence** via Atlassian Cloud REST APIs. No external dependencies — just PowerShell 5.1+ (built into Windows).

## Folder Structure

```
atlassian-rest-toolkit/
├── .env.example                       # Template — copy to .env, fill in credentials
├── .env                               # YOUR credentials (never share or commit!)
├── README.md                          # This file
├── jira/
│   ├── fetch-issue.ps1                # Fetch full Jira issue as JSON
│   └── download-attachments.ps1       # Download issue attachments + extract DOCX text
├── confluence/
│   ├── get-page.ps1                   # Fetch a Confluence page by numeric ID
│   └── cql-search.ps1                 # Search Confluence via CQL query
└── output/                            # Auto-created — downloaded data lands here
    └── <IssueKey>/
        ├── issue-rest.json
        └── attachments/
```

## Quick Start

### 1. Create your `.env` file

```powershell
Copy-Item .env.example .env
```

Then edit `.env` and fill in your real values:

```env
JIRA_EMAIL=your-email@company.com
JIRA_API_TOKEN=your-api-token
JIRA_BASE_URL=https://your-site.atlassian.net
```

### 2. Get an API Token

1. Go to [Atlassian API Tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click **Create API token**
3. Give it a label (e.g. "REST Toolkit")
4. Copy the token into `.env`

### 3. Run scripts

All scripts use `-ExecutionPolicy Bypass` to avoid signature issues:

```powershell
cd atlassian-rest-toolkit
```

---

## Jira Scripts

### Fetch a Jira Issue

Downloads the full issue payload (all fields, custom fields, links, attachments metadata, changelog peek) as JSON.

```powershell
powershell -ExecutionPolicy Bypass -File jira\fetch-issue.ps1 -IssueKey 'PDT-2854'
```

**Output:** `output\PDT-2854\issue-rest.json`

The JSON contains:
- All standard fields (summary, description, status, priority, labels, components...)
- All custom fields (with `expand=names` so field IDs map to human labels)
- Linked issues metadata
- Attachments metadata (filename, mimeType, size, download URL)
- Changelog peek (last change)

### Download Jira Attachments

Downloads all attachments from an issue. Automatically extracts text from `.docx` files.

```powershell
powershell -ExecutionPolicy Bypass -File jira\download-attachments.ps1 -IssueKey 'PDT-2854'
```

**Output:** `output\PDT-2854\attachments\` + `manifest.json`

Download specific attachments only:

```powershell
powershell -ExecutionPolicy Bypass -File jira\download-attachments.ps1 -IssueKey 'PDT-2854' -AttachmentIds '10001,10002'
```

---

## Confluence Scripts

### Fetch a Confluence Page

Downloads a single page by its numeric ID (the number in the URL `.../pages/<id>/...`).

```powershell
# Default: v1 API with full expand (body, space, ancestors, labels, children)
powershell -ExecutionPolicy Bypass -File confluence\get-page.ps1 -PageId '779517953'

# v2 API (alternative)
powershell -ExecutionPolicy Bypass -File confluence\get-page.ps1 -PageId '779517953' -Api v2

# Save to file instead of stdout
powershell -ExecutionPolicy Bypass -File confluence\get-page.ps1 -PageId '779517953' -OutFile page.json
```

### Search Confluence via CQL

Searches Confluence using CQL (Confluence Query Language).

```powershell
# Search by title
powershell -ExecutionPolicy Bypass -File confluence\cql-search.ps1 -Cql 'type=page AND title~"API documentation"'

# Search in a specific space
powershell -ExecutionPolicy Bypass -File confluence\cql-search.ps1 -Cql 'space=DEV AND label="release-notes"' -Limit 25

# Save results to file
powershell -ExecutionPolicy Bypass -File confluence\cql-search.ps1 -Cql 'type=page AND title~"deployment"' -OutFile results.json
```

**Common CQL examples:**

| Query | Description |
|-------|-------------|
| `type=page AND title~"search term"` | Pages with title containing text |
| `space=MYSPACE AND type=page` | All pages in a space |
| `label="important" AND type=page` | Pages with a specific label |
| `creator=currentUser() AND type=page` | Pages you created |
| `lastModified >= "2025-01-01"` | Recently modified pages |

---

## Credential Resolution

All scripts resolve credentials in the same order:

1. **System environment variables** (highest priority)
2. **`.env` file** in the toolkit root directory

### Required Variables

| Variable | Required | Used By | Description |
|----------|----------|---------|-------------|
| `JIRA_EMAIL` | **Yes** | All scripts | Your Atlassian account email |
| `JIRA_API_TOKEN` | **Yes** | All scripts | API token from Atlassian |
| `JIRA_BASE_URL` | **Yes** | Jira scripts | e.g. `https://your-site.atlassian.net` |

### Optional Confluence Overrides

Only needed if your Confluence is on a **different** Atlassian site than Jira:

| Variable | Used By | Description |
|----------|---------|-------------|
| `CONFLUENCE_EMAIL` | Confluence scripts | Overrides JIRA_EMAIL for Confluence |
| `CONFLUENCE_API_TOKEN` | Confluence scripts | Overrides JIRA_API_TOKEN for Confluence |
| `CONFLUENCE_WIKI_BASE` | Confluence scripts | e.g. `https://other-site.atlassian.net/wiki` |
| `CONFLUENCE_URL` | Confluence scripts | Alternative: full URL like `https://other-site.atlassian.net/wiki/home` |

**Wiki base URL resolution order:**
1. `CONFLUENCE_WIKI_BASE` (explicit, highest priority)
2. `CONFLUENCE_URL` (normalized to `.../wiki`)
3. `JIRA_BASE_URL` + `/wiki` (same Atlassian Cloud site fallback)

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `JIRA_EMAIL and JIRA_API_TOKEN must be set` | Create `.env` from `.env.example` and fill credentials |
| `401 Unauthorized` | Check email + API token are correct; token may have expired |
| `403 Forbidden` | Your account lacks permission for this resource |
| `404 Not Found` | Wrong issue key, page ID, or base URL |
| `Missing wiki base URL` | Set `CONFLUENCE_WIKI_BASE` or `CONFLUENCE_URL` in `.env` |
| `Execution policy` error | Run with `powershell -ExecutionPolicy Bypass -File ...` |

---

## Security

- **NEVER commit `.env`** to version control — it contains your API token
- **NEVER share your API token** — each person creates their own
- All operations are **READ-ONLY** — these scripts cannot modify Jira or Confluence
- API tokens can be revoked at any time from [Atlassian settings](https://id.atlassian.com/manage-profile/security/api-tokens)
