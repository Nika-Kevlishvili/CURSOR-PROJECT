$ErrorActionPreference = 'Stop'
# PSScriptRoot = .../Cursor-Project/config/slack → workspace = three levels up
$ws = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path

$msg = @'
PDT-2880 – Playwright test results

Jira: PDT-2880
Title: Customer - "POD is disconnected" incorrect label
Date: 2026-05-19
Assignee: "—" / Tester: "nika kevlishvili"

Total: 0 passed, 1 failed, 0 skipped.

Notes:
Bug reproduced (expected failure): LOST Customer A has isPodDisconnected=true after DPS for Customer B. Detailed reports attached.
'@

$envPath = Join-Path $ws 'Cursor-Project\EnergoTS\.env'
if (Test-Path -LiteralPath $envPath) {
    Get-Content -LiteralPath $envPath | ForEach-Object {
        if ($_ -match '^\s*SLACK_API_TOKEN\s*=\s*(.+)$') { $env:SLACK_API_TOKEN = $matches[1].Trim().Trim('"') }
        elseif ($_ -match '^\s*SLACK_BOT_TOKEN\s*=\s*(.+)$') { $env:SLACK_BOT_TOKEN = $matches[1].Trim().Trim('"') }
    }
}

$sendScript = Join-Path $ws 'Cursor-Project\config\qa-cursor-toolkit\scripts\slack\send-message.ps1'
$token = if ($env:SLACK_BOT_TOKEN) { $env:SLACK_BOT_TOKEN } else { $env:SLACK_API_TOKEN }
& $sendScript -ChannelId 'C0AK96S1D7X' -Message $msg -BotToken $token

$smartReport = Join-Path $ws 'Cursor-Project\reports\Chat reports\2026\may\19\ScopedPlaywright_PDT-2880.md'
$machineReport = Join-Path $ws 'Cursor-Project\EnergoTS\playwright-report-detailed.md'
$uploadWrapper = Join-Path $PSScriptRoot '_run-upload-with-dotenv.ps1'

& $uploadWrapper -FilePath $smartReport -ChannelOrUserId 'C0AK96S1D7X' -InitialComment 'Detailed Playwright report (PDT-2880 — smart)'
& $uploadWrapper -FilePath $machineReport -ChannelOrUserId 'C0AK96S1D7X' -InitialComment 'Playwright JSON detailed (PDT-2880 — machine)'

Write-Host 'Slack delivery completed.' -ForegroundColor Green
