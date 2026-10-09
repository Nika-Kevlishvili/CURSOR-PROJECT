<#
.SYNOPSIS
  Post a Rule 32 bug-validation report to Slack #bug-validation (Path 1).

.DESCRIPTION
  Primary delivery in Cursor: plugin-slack-slack MCP slack_send_message -> C0AUEEDVCEL.
  This script is the REST fallback using SLACK_BOT_TOKEN / SLACK_API_TOKEN from EnergoTS\.env.

  #bug-validation is a PRIVATE channel. The bot must be a member (/invite @report) or
  chat.postMessage returns channel_not_found. On failure, posts to #ai-report (C0AK96S1D7X).

.PARAMETER ReportMarkdownPath
  Full path to a .md file (e.g. chat export or BugValidation_*.md).

.PARAMETER Message
  Raw message body (use instead of -ReportMarkdownPath).

.PARAMETER IssueKey
  Optional label prefix in fallback note (e.g. PDT-2915).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File Cursor-Project/config/slack/send-bug-validation-to-slack.ps1 `
    -ReportMarkdownPath "Cursor-Project/reports/Chat reports/2026/june/01/BugValidation_PDT-2915.md"
#>
param(
    [string]$ReportMarkdownPath,
    [string]$Message,
    [string]$IssueKey = ''
)

$ErrorActionPreference = 'Stop'

if (-not $Message -and -not $ReportMarkdownPath) {
    Write-Error 'Provide -ReportMarkdownPath or -Message.'
    exit 1
}

if ($ReportMarkdownPath) {
    if (-not (Test-Path -LiteralPath $ReportMarkdownPath)) {
        Write-Error "Report file not found: $ReportMarkdownPath"
        exit 1
    }
    $Message = Get-Content -LiteralPath $ReportMarkdownPath -Raw -Encoding UTF8
}

$token = & (Join-Path $PSScriptRoot '_slack-token-helper.ps1') -ReturnToken

$targets = @(
    @{ Id = 'C0AUEEDVCEL'; Label = 'bug-validation' },
    @{ Id = 'C0AK96S1D7X'; Label = 'ai-report (fallback)' }
)

$posted = $false
$lastError = $null

foreach ($target in $targets) {
    $header = ''
    if ($target.Label -like '*fallback*') {
        $keyPart = if ($IssueKey) { " $IssueKey" } else { '' }
        $header = "[Note:$keyPart #bug-validation (C0AUEEDVCEL) not accessible to bot - posted to #ai-report instead]`n`n"
    }

    $payload = @{
        channel = $target.Id
        text    = ($header + $Message)
        mrkdwn  = $false
    } | ConvertTo-Json -Depth 5 -Compress

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
    $response = Invoke-RestMethod `
        -Uri 'https://slack.com/api/chat.postMessage' `
        -Headers @{ Authorization = "Bearer $token" } `
        -Method Post `
        -Body $bytes `
        -ContentType 'application/json; charset=utf-8'

    if ($response.ok) {
        Write-Host "OK: Posted bug validation to $($target.Label) (channel=$($response.channel) ts=$($response.ts))"
        $posted = $true
        break
    }
    $lastError = $response.error
    Write-Host "WARN: $($target.Label) failed: $lastError"
}

if (-not $posted) {
    Write-Error "Slack delivery failed. Last error: $lastError. Invite bot to #bug-validation or use Slack MCP in Cursor."
    exit 1
}
