# attach-screenshot-to-jira.ps1
# Uploads one or more screenshot files to a Jira issue as attachments.
# Automatically converts non-PNG/JPG formats (WebP, BMP, TIFF, etc.) to PNG.
# Uses credentials from Cursor-Project/.env (JIRA_EMAIL, JIRA_API_TOKEN, JIRA_BASE_URL).
#
# Usage (from workspace root):
#   .cursor\commands\attach-screenshot-to-jira.ps1 -IssueKey PHN-3710 -ScreenshotPath "C:\...\screenshot.png"
#   .cursor\commands\attach-screenshot-to-jira.ps1 -IssueKey PHN-3710 -ScreenshotPath "C:\...\a.png","C:\...\b.png"
#
# On success, writes structured output lines (captured by the caller):
#   ATTACHMENT_ID=<integer>
#   ATTACHMENT_FILENAME=<filename>
#   ATTACHMENT_THUMBNAIL=<url>
#   ATTACHMENT_CONTENT_URL=<url>
#
# Exit codes: 0 = all uploaded, 1 = credential error, 2 = one or more uploads failed

param(
    [Parameter(Mandatory = $true)]
    [string]$IssueKey,

    [Parameter(Mandatory = $true)]
    [string[]]$ScreenshotPath
)

$ErrorActionPreference = "Stop"

# Resolve paths
$scriptDir      = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot  = Split-Path -Parent (Split-Path -Parent $scriptDir)
$envFile        = Join-Path $workspaceRoot "Cursor-Project\.env"

if (-not (Test-Path $envFile)) {
    Write-Error "Cannot find .env at: $envFile"
    exit 1
}

# Load .env
$envVars = @{}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line -match "^([^=]+)=(.*)$") {
        $envVars[$Matches[1].Trim()] = $Matches[2].Trim().Trim('"').Trim("'")
    }
}

# Authentication always uses JIRA_EMAIL + JIRA_API_TOKEN (the shared API credentials).
# JIRA_REPORTER_EMAIL is only used for user-lookup (tester/reporter field) - never for API auth.
$jiraEmail = $envVars["JIRA_EMAIL"]
$jiraToken = $envVars["JIRA_API_TOKEN"]
$jiraBase  = if ($envVars["JIRA_BASE_URL"]) { $envVars["JIRA_BASE_URL"] } else { "https://oppa-support.atlassian.net" }

if (-not $jiraEmail -or -not $jiraToken) {
    Write-Error "JIRA_EMAIL / JIRA_API_TOKEN not found in $envFile. Add JIRA_EMAIL and JIRA_API_TOKEN to Cursor-Project/.env to use this script."
    exit 1
}

$authEncoded = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes("${jiraEmail}:${jiraToken}"))
$authHeaders = @{
    Authorization       = "Basic $authEncoded"
    "X-Atlassian-Token" = "no-check"
}

# Cloud ID is embedded in the API base path for attachment content URLs
# e.g. https://api.atlassian.com/ex/jira/ad451d5c-7331-46f8-9a47-f51dc8e6bbde
$cloudId   = "ad451d5c-7331-46f8-9a47-f51dc8e6bbde"
$apiBase   = "https://api.atlassian.com/ex/jira/$cloudId"
$uploadUrl = "$apiBase/rest/api/3/issue/$IssueKey/attachments"

# Format conversion: converts non-PNG/JPG formats (WebP, BMP, TIFF, etc.) to PNG using System.Drawing.
# Windows 10+ includes a built-in WebP codec via WIC that System.Drawing can use.
function Convert-ToCompatibleFormat {
    param([string]$InputPath)

    $ext = [System.IO.Path]::GetExtension($InputPath).ToLower()

    if ($ext -in @('.png', '.jpg', '.jpeg', '.gif')) {
        return $InputPath
    }

    $outputPath = [System.IO.Path]::ChangeExtension($InputPath, '.png')
    try {
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bitmap = [System.Drawing.Bitmap]::new($InputPath)
        $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
        Write-Host ("  [INFO] Converted '{0}' to PNG: {1}" -f $ext, (Split-Path -Leaf $outputPath)) -ForegroundColor Yellow
        return $outputPath
    }
    catch {
        Write-Warning ("  Format conversion from '{0}' failed: {1} - uploading original." -f $ext, $_.Exception.Message)
        return $InputPath
    }
}

# Upload each file
$anyFailed = $false

foreach ($rawPath in $ScreenshotPath) {
    if (-not (Test-Path $rawPath)) {
        Write-Warning "File not found, skipping: $rawPath"
        $anyFailed = $true
        continue
    }

    $filePath = Convert-ToCompatibleFormat -InputPath $rawPath
    $fileName = Split-Path -Leaf $filePath

    Write-Host "Uploading '$fileName' to $IssueKey ..." -ForegroundColor Cyan

    try {
        $boundary = [System.Guid]::NewGuid().ToString("N")

        $mime = switch ([System.IO.Path]::GetExtension($fileName).ToLower()) {
            ".png"  { "image/png" }
            ".jpg"  { "image/jpeg" }
            ".jpeg" { "image/jpeg" }
            ".gif"  { "image/gif" }
            ".webp" { "image/webp" }
            default { "application/octet-stream" }
        }

        $enc       = [System.Text.Encoding]::UTF8
        $fileBytes = [System.IO.File]::ReadAllBytes($filePath)

        $crlf = "`r`n"
        $headerText = "--$boundary$crlf" + "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$crlf" + "Content-Type: $mime$crlf$crlf"
        $footerText = "$crlf--$boundary--$crlf"

        $partHeader = $enc.GetBytes($headerText)
        $partFooter = $enc.GetBytes($footerText)

        $bodyList = [System.Collections.Generic.List[byte]]::new()
        $bodyList.AddRange($partHeader)
        $bodyList.AddRange($fileBytes)
        $bodyList.AddRange($partFooter)
        $bodyBytes = $bodyList.ToArray()

        $response = Invoke-WebRequest -Uri $uploadUrl -Method Post -Headers $authHeaders -Body $bodyBytes -ContentType "multipart/form-data; boundary=$boundary" -UseBasicParsing

        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
            Write-Host ("  [OK] Uploaded successfully (HTTP {0})." -f $response.StatusCode) -ForegroundColor Green

            try {
                $attachments = $response.Content | ConvertFrom-Json
                $att = if ($attachments -is [array]) { $attachments[0] } else { $attachments }

                $attId          = $att.id
                $attFilename    = $att.filename
                $attContentUrl  = $att.content
                $attThumbnail   = $att.thumbnail

                Write-Output "ATTACHMENT_ID=$attId"
                Write-Output "ATTACHMENT_FILENAME=$attFilename"
                Write-Output "ATTACHMENT_CONTENT_URL=$attContentUrl"
                if ($attThumbnail) { Write-Output "ATTACHMENT_THUMBNAIL=$attThumbnail" }

                Write-Host ("  [INFO] Attachment ID: {0} | File: {1}" -f $attId, $attFilename) -ForegroundColor DarkCyan
            }
            catch {
                Write-Warning ("  Could not parse upload response: {0}" -f $_.Exception.Message)
            }

        } else {
            Write-Warning ("  Upload failed (HTTP {0}): {1}" -f $response.StatusCode, $response.Content)
            $anyFailed = $true
        }
    }
    catch {
        Write-Warning ("  Exception uploading '{0}': {1}" -f $fileName, $_.Exception.Message)
        $anyFailed = $true
    }
}

if ($anyFailed) { exit 2 } else { exit 0 }
