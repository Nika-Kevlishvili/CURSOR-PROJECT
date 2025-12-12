# Environment Variables Setup Guide

## პრობლემა / Problem

როდესაც პროექტს გადაიტანთ სხვა კომპიუტერზე, environment variables არ არის დაყენებული, რის გამოც agents არ მუშაობს სწორად.

When you move the project to another computer, environment variables are not set, causing agents to not work correctly.

## გამოსავალი / Solution

### 1. Environment Variables Template

პროექტში არის `.env.example` ფაილი, რომელიც შეიცავს template-ებს.

The project has a `.env.example` file that contains templates.

### 2. Manual Setup

ან ხელით დააყენეთ environment variables:

Or manually set environment variables:

```powershell
# GitLab
$env:GITLAB_URL="https://gitlab.com"
$env:GITLAB_TOKEN="your-token"
$env:GITLAB_PROJECT_ID="12345678"

# Jira
$env:JIRA_URL="https://your-company.atlassian.net"
$env:JIRA_EMAIL="your-email@example.com"
$env:JIRA_API_TOKEN="your-token"
$env:JIRA_PROJECT_KEY="PROJ"

# Postman
$env:POSTMAN_API_KEY="your-api-key"
$env:POSTMAN_WORKSPACE_ID="your-workspace-id"

# Confluence
$env:CONFLUENCE_URL="https://your-company.atlassian.net/wiki/home"
```

## Required Environment Variables

### GitLab (for IntegrationService)
- `GITLAB_URL` - GitLab instance URL
- `GITLAB_TOKEN` - Personal Access Token
- `GITLAB_PROJECT_ID` - Project ID (numeric)
- `GITLAB_PIPELINE_ID` - Optional, pipeline ID

### Jira (for IntegrationService)
- `JIRA_URL` - Jira instance URL
- `JIRA_EMAIL` - Your Jira account email
- `JIRA_API_TOKEN` - API token from Jira
- `JIRA_PROJECT_KEY` - Project key (e.g., "PROJ")

### Postman (for PostmanCollectionGenerator)
- `POSTMAN_API_KEY` - API key from Postman
- `POSTMAN_WORKSPACE_ID` - Workspace ID

### Confluence (for PhoenixExpert)
- `CONFLUENCE_URL` - Confluence base URL (optional, uses cache if not set)

## Permanent Setup

მუდმივი დასაყენებლად (User-level):

For permanent setup (User-level):

```powershell
[Environment]::SetEnvironmentVariable('GITLAB_URL', 'https://gitlab.com', 'User')
[Environment]::SetEnvironmentVariable('GITLAB_TOKEN', 'your-token', 'User')
# ... და ა.შ.
```

ან System Properties → Environment Variables → User variables

Or System Properties → Environment Variables → User variables

## Verification

შემოწმება:

Verification:

```powershell
# Check if variables are set
$env:GITLAB_URL
$env:JIRA_URL
$env:POSTMAN_API_KEY
```

## Troubleshooting

### Agents არ მუშაობს / Agents not working

1. შეამოწმეთ environment variables დაყენებულია თუ არა
   Check if environment variables are set

2. გადატვირთეთ PowerShell session
   Restart PowerShell session

3. შეამოწმეთ .env ფაილი და დააყენეთ environment variables ხელით
   Check .env file and set environment variables manually

### Integration Service warnings

თუ ხედავთ warnings:
If you see warnings:

```
IntegrationService: ⚠ GitLab URL not configured
IntegrationService: ⚠ Jira email not configured
```

ეს ნიშნავს, რომ environment variables არ არის დაყენებული.
This means environment variables are not set.

---

**ბოლო განახლება / Last Updated**: 2025-01-14

