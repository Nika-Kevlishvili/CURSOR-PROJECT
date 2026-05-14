<#
.SYNOPSIS
    Upload a file to a Slack channel via Bot token.

.PARAMETER ChannelId
    Slack channel ID.

.PARAMETER FilePath
    Path to the file to upload.

.PARAMETER Title
    Optional title for the file in Slack.

.PARAMETER Comment
    Optional initial comment alongside the file.

.PARAMETER BotToken
    Optional. Overrides SLACK_BOT_TOKEN / SLACK_API_TOKEN from env/.env.

.EXAMPLE
    .\upload-file.ps1 -ChannelId 'C0123456789' -FilePath 'reports\BugValidation.md' -Title 'Bug Validation Report'
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$ChannelId,

    [Parameter(Mandatory=$true)]
    [string]$FilePath,

    [string]$Title,

    [string]$Comment,

    [string]$BotToken
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolkitRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

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

if (-not (Test-Path $FilePath)) {
    Write-Error "File not found: $FilePath"
    exit 1
}

$fileName = Split-Path -Leaf $FilePath
if (-not $Title) { $Title = $fileName }

$headers = @{ "Authorization" = "Bearer $BotToken" }

$fileBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $FilePath))
$boundary = [System.Guid]::NewGuid().ToString()

$bodyLines = @()
$bodyLines += "--$boundary"
$bodyLines += "Content-Disposition: form-data; name=`"channels`""
$bodyLines += ""
$bodyLines += $ChannelId
$bodyLines += "--$boundary"
$bodyLines += "Content-Disposition: form-data; name=`"title`""
$bodyLines += ""
$bodyLines += $Title
if ($Comment) {
    $bodyLines += "--$boundary"
    $bodyLines += "Content-Disposition: form-data; name=`"initial_comment`""
    $bodyLines += ""
    $bodyLines += $Comment
}
$bodyLines += "--$boundary"
$bodyLines += "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`""
$bodyLines += "Content-Type: application/octet-stream"
$bodyLines += ""

$headerBytes = [System.Text.Encoding]::UTF8.GetBytes(($bodyLines -join "`r`n") + "`r`n")
$footerBytes = [System.Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--`r`n")

$bodyStream = New-Object System.IO.MemoryStream
$bodyStream.Write($headerBytes, 0, $headerBytes.Length)
$bodyStream.Write($fileBytes, 0, $fileBytes.Length)
$bodyStream.Write($footerBytes, 0, $footerBytes.Length)
$fullBody = $bodyStream.ToArray()
$bodyStream.Dispose()

try {
    $resp = Invoke-RestMethod -Uri "https://slack.com/api/files.upload" `
        -Method Post `
        -Headers $headers `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $fullBody

    if ($resp.ok) {
        Write-Host "[OK] File uploaded: $fileName → $ChannelId" -ForegroundColor Green
    } else {
        Write-Error "Slack API error: $($resp.error)"
        exit 1
    }
} catch {
    Write-Error "Failed to upload file: $_"
    exit 1
}
