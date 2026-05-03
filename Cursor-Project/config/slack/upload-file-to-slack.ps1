<#
.SYNOPSIS
Uploads a local file to Slack (channel or DM) using Web API external upload (files.getUploadURLExternal + completeUploadExternal).

.DESCRIPTION
Requires **SLACK_API_TOKEN** (same as EnergoTS CI / SlackReporter) or **SLACK_BOT_TOKEN** — **Bot User OAuth Token** (`xoxb-...`) with **files:write**. Do **not** use App-level tokens (`xapp-...`); they return `not_allowed_token_type` for file uploads.
Upload the same file twice for HandsOff: once to Tester user ID (DM), once to #ai-report channel ID.

.PARAMETER FilePath
Absolute path to the file (e.g. detailed HandsOff report .md).

.PARAMETER ChannelOrUserId
Slack channel ID (e.g. C0AK96S1D7X) or user ID for DM (e.g. Uxxxx).

.PARAMETER InitialComment
Optional short comment shown with the file in Slack.

.EXAMPLE
$env:SLACK_API_TOKEN = 'xoxb-...'
powershell -ExecutionPolicy Bypass -File upload-file-to-slack.ps1 -FilePath 'D:\...\PHN-2823.md' -ChannelOrUserId 'C0AK96S1D7X' -InitialComment 'Detailed Playwright report'
#>
param(
    [Parameter(Mandatory = $true)][string]$FilePath,
    [Parameter(Mandatory = $true)][string]$ChannelOrUserId,
    [string]$InitialComment = ''
)

# Prefer Bot User OAuth Token (xoxb-) from either env var — file upload API does not accept App-level (xapp-) tokens.
$api = $env:SLACK_API_TOKEN
$bot = $env:SLACK_BOT_TOKEN
$slackToken = $null
foreach ($t in @($api, $bot)) {
    if ($t -and $t.StartsWith('xoxb-', [System.StringComparison]::OrdinalIgnoreCase)) {
        $slackToken = $t
        break
    }
}
if (-not $slackToken) {
    if ($api) { $slackToken = $api }
    elseif ($bot) { $slackToken = $bot }
}
if (-not $slackToken) {
    Write-Error 'Set SLACK_API_TOKEN (EnergoTS) or SLACK_BOT_TOKEN before running this script.'
    exit 1
}
if ($slackToken.StartsWith('xapp-', [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Error 'Slack App-level token (xapp-...) cannot be used for files.getUploadURLExternal. Add Bot User OAuth Token (xoxb-...) in SLACK_API_TOKEN or SLACK_BOT_TOKEN (OAuth & Permissions → Bot User OAuth Token; scope files:write; reinstall app).'
    exit 1
}

$item = Get-Item -LiteralPath $FilePath -ErrorAction Stop
$bytes = [System.IO.File]::ReadAllBytes($item.FullName)
$length = $bytes.Length
$fileName = $item.Name
$encodedFileName = [uri]::EscapeDataString($fileName)

$headers = @{
    Authorization = "Bearer $slackToken"
}

$getUrl = "https://slack.com/api/files.getUploadURLExternal?filename=$encodedFileName&length=$length&pretty=1"
$step1 = Invoke-RestMethod -Uri $getUrl -Headers $headers -Method Get

if (-not $step1.ok) {
    Write-Error ("files.getUploadURLExternal failed: " + ($step1 | ConvertTo-Json -Compress))
    exit 1
}

$uploadUrl = $step1.upload_url
$fileId = $step1.file_id

try {
    Invoke-WebRequest -Uri $uploadUrl -Method Post -ContentType 'application/octet-stream' -Body $bytes -UseBasicParsing | Out-Null
}
catch {
    Write-Error "Binary upload failed: $_"
    exit 1
}

$safeTitle = $fileName.Replace('\', '\\').Replace('"', '\"')
$filesPayload = "[{`"id`":`"$fileId`",`"title`":`"$safeTitle`"}]"
$filesEscaped = [uri]::EscapeDataString($filesPayload)
$channelEscaped = [uri]::EscapeDataString($ChannelOrUserId.Trim())
$formParts = @(
    "files=$filesEscaped",
    "channel_id=$channelEscaped"
)
if ($InitialComment) {
    $formParts += "initial_comment=$([uri]::EscapeDataString($InitialComment))"
}
$formBody = $formParts -join '&'

# POST form body (Slack Web API); avoids long query strings and matches Slack examples.
$step2 = Invoke-RestMethod -Uri 'https://slack.com/api/files.completeUploadExternal' -Headers $headers -Method Post -Body $formBody -ContentType 'application/x-www-form-urlencoded'

if (-not $step2.ok) {
    Write-Error ("files.completeUploadExternal failed: " + ($step2 | ConvertTo-Json -Compress))
    exit 1
}

Write-Host "OK: Uploaded '$fileName' to $ChannelOrUserId"
