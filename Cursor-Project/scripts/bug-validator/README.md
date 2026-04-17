# Bug Validator — GitHub Actions Pipeline

Automated bug validation triggered by Jira automation. Validates bug reports against Confluence documentation and local Phoenix codebase, then sends results to Slack.

## Architecture

```
Jira Bug (status change) → Jira Automation (webhook) → GitHub Actions → Bug Validator → Slack Report
```

### Pipeline Steps

1. **Jira** — Fetches bug details (summary, description, priority)
2. **Confluence** — Searches documentation for related pages, validates bug description
3. **Local Phoenix Code** — Searches local Cursor-Project/Phoenix directory for relevant code
4. **Gemini AI** — Analyzes bug + documentation + code, determines validity
5. **Slack** — Sends structured report with verdict

## Setup

### 1. GitHub Secrets

Add these secrets in your GitHub repository (Settings → Secrets → Actions):

| Secret | Description | Example |
|---|---|---|
| `JIRA_BASE_URL` | Jira Cloud base URL | `https://yourorg.atlassian.net` |
| `JIRA_EMAIL` | Jira account email | `user@example.com` |
| `JIRA_API_TOKEN` | Jira API token | [Create here](https://id.atlassian.com/manage-profile/security/api-tokens) |
| `GEMINI_API_KEY` | Google Gemini API key (free tier) | [Get from Google AI Studio](https://makersuite.google.com/) |
| `SLACK_WEBHOOK_URL` | Slack incoming webhook | [Create here](https://api.slack.com/messaging/webhooks) |
| `SLACK_CHANNEL_ID` | *(optional)* Slack channel ID override (recommended for reliability) | `C0AUEEDVCEL` |
| `SLACK_CHANNEL` | *(optional)* Slack channel name override (fallback) | `bug-validation` |
| `CONFLUENCE_BASE_URL` | *(optional)* Confluence base URL | `https://yourorg.atlassian.net` |
| `CONFLUENCE_EMAIL` | *(optional)* Confluence email | `user@example.com` |
| `CONFLUENCE_API_TOKEN` | *(optional)* Confluence API token | Same as Jira token if Atlassian Cloud |
| `CONFLUENCE_SPACE_KEYS` | *(optional)* Comma-separated space keys | `PHOENIX,DEV` |
| `PHOENIX_LOCAL_ROOT` | *(optional)* Custom Phoenix directory path | `/path/to/your/phoenix/code` |

### 2. Self-hosted GitHub Runner Setup

The bug validator now uses a self-hosted runner to access local Phoenix code and VPN resources.

**Prerequisites:**
- Windows machine with VPN access to internal GitLab
- Python 3.11+ installed
- Local `Cursor-Project/Phoenix` directory with Phoenix projects
- Git and PowerShell available

**Setup Steps:**

1. **GitHub Repository Settings:**
   - Go to `Settings` → `Actions` → `Runners`
   - Click `New self-hosted runner`
   - Select `Windows`
   - Follow the download and configuration instructions

2. **Runner Installation:**
   ```powershell
   # Download runner (GitHub provides exact commands)
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/... 
   
   # Extract and configure
   Expand-Archive -Path runner.zip -DestinationPath runner
   cd runner
   ./config.cmd --url https://github.com/YOUR_USERNAME/YOUR_REPO --token YOUR_TOKEN
   
   # Run as a service (recommended)
   ./svc.sh install
   ./svc.sh start
   ```

3. **Phoenix Directory:**
   - Ensure `Cursor-Project/Phoenix` exists on the runner machine
   - Contains your local Phoenix projects (phoenix-core, phoenix-payment-api, etc.)
   - **Custom Path (Optional):** Set `PHOENIX_LOCAL_ROOT` environment variable

**Benefits:**
- ✅ Full VPN access to GitLab and internal resources
- ✅ Local Phoenix code scanning without uploading to GitHub
- ✅ Complete automation: Jira → GitHub Actions → Local Analysis → Slack
- ✅ No cloud storage or security concerns

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
├── main.py                 # Entry point — orchestrates the pipeline
├── extract_jira_key.py     # CI helper — writes GITHUB_OUTPUT key (no bash on Windows)
├── jira_client.py          # Jira REST API — fetch bug details
├── local_phoenix_client.py # Local Phoenix filesystem scanner
├── confluence_client.py    # Confluence REST API — documentation search
├── analyzer.py             # Gemini AI — bug analysis and validation
├── slack_report_template.py # Slack message layout, labels, Confluence basis section
├── slack_reporter.py       # Slack webhook — send report (uses template)
├── requirements.txt        # Python dependencies
└── README.md               # This file
```

To change how Slack reports look (section order, human-readable labels, Confluence excerpt limits), edit **`slack_report_template.py`**.
