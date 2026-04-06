# Bug Validator — GitHub Actions Pipeline

Automated bug validation triggered by Jira automation. Validates bug reports against Confluence documentation and Phoenix codebase (via GitLab API, read-only), then sends results to Slack.

## Architecture

```
Jira Bug (status change) → Jira Automation (webhook) → GitHub Actions → Bug Validator → Slack Report
```

### Pipeline Steps

1. **Jira** — Fetches bug details (summary, description, priority)
2. **Confluence** — Searches documentation for related pages, validates bug description
3. **GitLab** — Searches Phoenix codebase via REST API (read-only token)
4. **Claude AI** — Analyzes bug + documentation + code, determines validity
5. **Slack** — Sends structured report with verdict

## Setup

### 1. GitHub Secrets

Add these secrets in your GitHub repository (Settings → Secrets → Actions):

| Secret | Description | Example |
|---|---|---|
| `JIRA_BASE_URL` | Jira Cloud base URL | `https://yourorg.atlassian.net` |
| `JIRA_EMAIL` | Jira account email | `user@example.com` |
| `JIRA_API_TOKEN` | Jira API token | [Create here](https://id.atlassian.com/manage-profile/security/api-tokens) |
| `GITLAB_URL` | GitLab instance URL | `https://gitlab.example.com` |
| `GITLAB_TOKEN` | GitLab read-only access token | Project Access Token (Reporter role, `read_api` + `read_repository`) |
| `GITLAB_PROJECT_IDS` | Comma-separated project IDs | `42,43` |
| `ANTHROPIC_API_KEY` | Claude API key | [Get from Anthropic Console](https://console.anthropic.com/) |
| `SLACK_WEBHOOK_URL` | Slack incoming webhook | [Create here](https://api.slack.com/messaging/webhooks) |
| `CONFLUENCE_BASE_URL` | *(optional)* Confluence base URL | `https://yourorg.atlassian.net` |
| `CONFLUENCE_EMAIL` | *(optional)* Confluence email | `user@example.com` |
| `CONFLUENCE_API_TOKEN` | *(optional)* Confluence API token | Same as Jira token if Atlassian Cloud |
| `CONFLUENCE_SPACE_KEYS` | *(optional)* Comma-separated space keys | `PHOENIX,DEV` |

### 2. GitLab Access Token

Create a **Project Access Token** in GitLab:

1. Go to your Phoenix project → Settings → Access Tokens
2. Name: `bug-validator-readonly`
3. Role: **Reporter**
4. Scopes: `read_api`, `read_repository`
5. Copy the token → add as `GITLAB_TOKEN` in GitHub Secrets

Repeat for each project or use a Group Access Token if projects are in the same group.

### 3. Slack Webhook

1. Go to [Slack API](https://api.slack.com/apps) → Create App → Incoming Webhooks
2. Enable webhooks → Add to channel (e.g. `#bug-validation`)
3. Copy the webhook URL → add as `SLACK_WEBHOOK_URL` in GitHub Secrets

### 4. Jira Automation (your part)

Create a Jira Automation rule:

- **Trigger:** Issue created / Status changed to "To Validate"
- **Condition:** Issue type = Bug
- **Action:** Send web request:
  - URL: `https://api.github.com/repos/{OWNER}/{REPO}/dispatches`
  - Method: POST
  - Headers: `Authorization: token {GITHUB_PAT}`, `Accept: application/vnd.github.v3+json`
  - Body:
    ```json
    {
      "event_type": "bug-validation",
      "client_payload": {
        "jira_key": "{{issue.key}}"
      }
    }
    ```

You need a GitHub Personal Access Token with `repo` scope for this webhook.

## Manual Trigger

You can also run the workflow manually from GitHub Actions:

1. Go to Actions → Bug Validator → Run workflow
2. Enter the Jira key (e.g. `REG-456`)
3. Click "Run workflow"

## Output

- **Slack:** Formatted message with verdict (VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / NOT VALID / INSUFFICIENT EVIDENCE)
- **GitHub Artifact:** JSON + Markdown report downloadable from the Actions run

## File Structure

```
Cursor-Project/scripts/bug-validator/
├── main.py               # Entry point — orchestrates the pipeline
├── jira_client.py         # Jira REST API — fetch bug details
├── gitlab_client.py       # GitLab REST API — read-only code search
├── confluence_client.py   # Confluence REST API — documentation search
├── analyzer.py            # Claude AI — bug analysis and validation
├── slack_reporter.py      # Slack webhook — send report
├── requirements.txt       # Python dependencies
└── README.md              # This file
```
