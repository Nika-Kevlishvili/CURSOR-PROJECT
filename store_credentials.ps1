# Secure Credentials Storage Script
# ეს სკრიპტი ინახავს credentials უსაფრთხოდ .env ფაილში
# This script stores credentials securely in .env file

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Secure Credentials Storage" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  SECURITY WARNING:" -ForegroundColor Yellow
Write-Host "   - Credentials will be stored in .env file" -ForegroundColor Yellow
Write-Host "   - .env file is in .gitignore (NOT in Git)" -ForegroundColor Yellow
Write-Host "   - DO NOT commit .env file to Git!" -ForegroundColor Red
Write-Host ""

# Check if .env exists, if not create from template
if (-not (Test-Path ".env")) {
    if (Test-Path "env.example") {
        Copy-Item "env.example" ".env" -Force
        Write-Host "✓ Created .env file from template" -ForegroundColor Green
    } else {
        Write-Host "⚠️  env.example not found. Creating basic .env file..." -ForegroundColor Yellow
        @"
# Environment Variables - DO NOT COMMIT TO GIT!
# This file contains sensitive credentials

# GitLab Configuration
GITLAB_URL=https://gitlab.com
GITLAB_TOKEN=
GITLAB_PROJECT_ID=
GITLAB_PIPELINE_ID=

# Jira Configuration
JIRA_URL=
JIRA_EMAIL=
JIRA_API_TOKEN=
JIRA_PROJECT_KEY=

# Postman Configuration
POSTMAN_API_KEY=
POSTMAN_WORKSPACE_ID=

# Confluence Configuration
CONFLUENCE_URL=

# GitHub Configuration (optional)
GITHUB_TOKEN=
"@ | Out-File -FilePath ".env" -Encoding UTF8
        Write-Host "✓ Created .env file" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Please enter your credentials (press Enter to skip):" -ForegroundColor Cyan
Write-Host ""

# GitLab
Write-Host "GitLab Configuration:" -ForegroundColor Yellow
$gitlabUrl = Read-Host "GitLab URL [https://gitlab.com]"
if ($gitlabUrl) {
    (Get-Content .env) -replace 'GITLAB_URL=https://gitlab.com', "GITLAB_URL=$gitlabUrl" | Set-Content .env
}

$gitlabToken = Read-Host "GitLab Token (Personal Access Token)" -AsSecureString
if ($gitlabToken) {
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($gitlabToken)
    $plainToken = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    (Get-Content .env) -replace 'GITLAB_TOKEN=', "GITLAB_TOKEN=$plainToken" | Set-Content .env
}

$gitlabProjectId = Read-Host "GitLab Project ID"
if ($gitlabProjectId) {
    (Get-Content .env) -replace 'GITLAB_PROJECT_ID=', "GITLAB_PROJECT_ID=$gitlabProjectId" | Set-Content .env
}

Write-Host ""

# Jira
Write-Host "Jira Configuration:" -ForegroundColor Yellow
$jiraUrl = Read-Host "Jira URL"
if ($jiraUrl) {
    (Get-Content .env) -replace 'JIRA_URL=', "JIRA_URL=$jiraUrl" | Set-Content .env
}

$jiraEmail = Read-Host "Jira Email"
if ($jiraEmail) {
    (Get-Content .env) -replace 'JIRA_EMAIL=', "JIRA_EMAIL=$jiraEmail" | Set-Content .env
}

$jiraToken = Read-Host "Jira API Token" -AsSecureString
if ($jiraToken) {
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($jiraToken)
    $plainToken = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    (Get-Content .env) -replace 'JIRA_API_TOKEN=', "JIRA_API_TOKEN=$plainToken" | Set-Content .env
}

$jiraProjectKey = Read-Host "Jira Project Key"
if ($jiraProjectKey) {
    (Get-Content .env) -replace 'JIRA_PROJECT_KEY=', "JIRA_PROJECT_KEY=$jiraProjectKey" | Set-Content .env
}

Write-Host ""

# Postman
Write-Host "Postman Configuration:" -ForegroundColor Yellow
$postmanKey = Read-Host "Postman API Key" -AsSecureString
if ($postmanKey) {
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($postmanKey)
    $plainKey = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    (Get-Content .env) -replace 'POSTMAN_API_KEY=', "POSTMAN_API_KEY=$plainKey" | Set-Content .env
}

$postmanWorkspace = Read-Host "Postman Workspace ID"
if ($postmanWorkspace) {
    (Get-Content .env) -replace 'POSTMAN_WORKSPACE_ID=', "POSTMAN_WORKSPACE_ID=$postmanWorkspace" | Set-Content .env
}

Write-Host ""

# Confluence
Write-Host "Confluence Configuration:" -ForegroundColor Yellow
$confluenceUrl = Read-Host "Confluence URL"
if ($confluenceUrl) {
    (Get-Content .env) -replace 'CONFLUENCE_URL=', "CONFLUENCE_URL=$confluenceUrl" | Set-Content .env
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Credentials stored in .env file" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
Write-Host "   - .env file is in .gitignore" -ForegroundColor White
Write-Host "   - DO NOT commit .env to Git!" -ForegroundColor Red
Write-Host "   - Load credentials: .\load_environment.ps1" -ForegroundColor White
Write-Host ""

