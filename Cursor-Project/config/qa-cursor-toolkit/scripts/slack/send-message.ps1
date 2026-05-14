<#
.SYNOPSIS
    Send a text message to a Slack channel via Bot token.

.PARAMETER ChannelId
    Slack channel ID (e.g. C0XXXXXXXXX).

.PARAMETER Message
    Message text (supports Slack mrkdwn formatting).

.PARAMETER BotToken
    Optional. Overrides SLACK_BOT_TOKEN / SLACK_API_TOKEN from env/.env.

.EXAMPLE
    .\send-message.ps1 -ChannelId 'C0123456789' -Message 'Bug PDT-1234 validated: VALID'
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$ChannelId,

    [Parameter(Mandatory=$true)]
    [string]$Message,

    [string]$BotToken
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolkitRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

# --- Resolve .env ---
function Load-EnvFile([string]$path) {
    if (Test-Path $path) {
        Get-Content $path | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith('#') -and $line -match '^([^=]+)=(.*)$') {
                $k = $Matches[1].Trim()
                $v = $Matches[2].Trim().Trim('"').Trim("'")
                [Environment]::SetEnvironmentVariable($k, $v, 'Process')
            }
        }
    }
}

Load-EnvFile (Join-Path $toolkitRoot ".env")

if (-not $BotToken) {
    $BotToken = $env:SLACK_BOT_TOKEN
    if (-not $BotToken) { $BotToken = $env:SLACK_API_TOKEN }
}

if (-not $BotToken) {
    Write-Error "SLACK_BOT_TOKEN or SLACK_API_TOKEN must be set in .env or environment."
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $BotToken"
    "Content-Type"  = "application/json; charset=utf-8"
}

$body = @{
    channel = $ChannelId
    text    = $Message
} | ConvertTo-Json -Depth 5

try {
    $resp = Invoke-RestMethod -Uri "https://slack.com/api/chat.postMessage" `
        -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body))

    if ($resp.ok) {
        Write-Host "[OK] Message sent to $ChannelId (ts: $($resp.ts))" -ForegroundColor Green
    } else {
        Write-Error "Slack API error: $($resp.error)"
        exit 1
    }
} catch {
    Write-Error "Failed to send Slack message: $_"
    exit 1
}
