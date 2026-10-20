# attach-report-to-jira.ps1
# Attaches one verification Word report (.docx) to an existing Jira issue and adds a comment.
# The comment text comes first. The report file is embedded in that same comment.
#
# Usage (from workspace root):
#   .cursor\commands\attach-report-to-jira.ps1 -IssueKey PDT-3672
#   .cursor\commands\attach-report-to-jira.ps1 -IssueKey PDT-3672 -ReportPath "C:\...\PDT-3672_Report.docx"
#   .cursor\commands\attach-report-to-jira.ps1 -IssueKey PDT-3672 -DryRun
#
# Exit codes:
#   0 = comment posted (or dry run resolved a file)
#   1 = credential / config error
#   2 = Jira upload or comment failed
#   3 = no Word report found for the issue
#   4 = issue key missing

param(
    [string]$IssueKey,

    [string]$ReportPath,

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$envFile = Join-Path $workspaceRoot "Cursor-Project\.env"
$reportsRoot = Join-Path $workspaceRoot "Cursor-Project\reports"

function Stop-With {
    param(
        [int]$Code,
        [string]$Message
    )
    Write-Host $Message -ForegroundColor Red
    exit $Code
}

function ConvertTo-JsonLiteral {
    param([string]$Value)
    if ($null -eq $Value) { $Value = "" }
    $escaped = $Value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '').Replace("`n", '\n').Replace("`t", '\t')
    return '"' + $escaped + '"'
}

function Get-IssueKeyFromName {
    param([string]$Name)
    $match = [regex]::Match($Name, '(?<![A-Z0-9])([A-Z][A-Z0-9]+-\d+)(?!\d)')
    if ($match.Success) { return $match.Groups[1].Value }
    return $null
}

function Find-ReportFile {
    param(
        [string]$Key,
        [string]$ReportsRootPath
    )
    if (-not (Test-Path -LiteralPath $ReportsRootPath)) {
        return $null
    }

    $keyPattern = '(?<![A-Z0-9])' + [regex]::Escape($Key) + '(?!\d)'
    $files = @(Get-ChildItem -LiteralPath $ReportsRootPath -Filter *.docx -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -notlike '~$*' -and $_.Name -match $keyPattern
        })

    if ($files.Count -eq 0) { return $null }

    return $files | Sort-Object `
        @{ Expression = { if ($_.FullName -match 'Chat reports') { 0 } else { 1 } } }, `
        @{ Expression = { if ($_.Name -match '_Report\.docx$') { 0 } else { 1 } } }, `
        @{ Expression = { $_.LastWriteTime }; Descending = $true } |
        Select-Object -First 1
}

function Read-DotEnv {
    param([string]$Path)
    $vars = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line -match "^([^=]+)=(.*)$") {
            $vars[$Matches[1].Trim()] = $Matches[2].Trim().Trim('"').Trim("'")
        }
    }
    return $vars
}

function Read-HttpError {
    param($ErrorRecord)
    try {
        $response = $ErrorRecord.Exception.Response
        if (-not $response) { return $ErrorRecord.Exception.Message }
        $stream = $response.GetResponseStream()
        if (-not $stream) { return $ErrorRecord.Exception.Message }
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        $reader.Dispose()
        if ($body) { return $body }
    }
    catch { }
    return $ErrorRecord.Exception.Message
}

function Get-VerificationSentence {
    param([string]$IssueTypeName)
    $kind = "bug or task"
    if ($IssueTypeName -match '(?i)bug') {
        $kind = "bug"
    }
    elseif ($IssueTypeName -match '(?i)task') {
        $kind = "task"
    }
    return "These are the main cases that were run and verified for this $kind."
}

function New-CommentBodyJson {
    param(
        [string]$Text,
        [string]$MediaUuid,
        [string]$FileName,
        [string]$LinkHref
    )

    $textJson = ConvertTo-JsonLiteral $Text
    $nodes = New-Object System.Collections.Generic.List[string]
    $nodes.Add(@"
      {
        "type": "paragraph",
        "content": [
          { "type": "text", "text": $textJson }
        ]
      }
"@)

    if ($MediaUuid) {
        $idJson = ConvertTo-JsonLiteral $MediaUuid
        $altJson = ConvertTo-JsonLiteral $FileName
        $nodes.Add(@"
      {
        "type": "mediaGroup",
        "content": [
          {
            "type": "media",
            "attrs": {
              "type": "file",
              "id": $idJson,
              "alt": $altJson,
              "collection": ""
            }
          }
        ]
      }
"@)
    }
    elseif ($LinkHref) {
        $hrefJson = ConvertTo-JsonLiteral $LinkHref
        $nameJson = ConvertTo-JsonLiteral $FileName
        $nodes.Add(@"
      {
        "type": "paragraph",
        "content": [
          { "type": "text", "text": "Report file: " },
          {
            "type": "text",
            "text": $nameJson,
            "marks": [ { "type": "link", "attrs": { "href": $hrefJson } } ]
          }
        ]
      }
"@)
    }

    $joined = $nodes -join ",`r`n"
    return @"
{
  "body": {
    "type": "doc",
    "version": 1,
    "content": [
$joined
    ]
  }
}
"@
}

function Resolve-MediaUuid {
    param(
        [string]$ApiBase,
        [string]$AttachmentId,
        $AuthHeaders
    )

    $location = $null
    try {
        $redirect = Invoke-WebRequest -Uri "$ApiBase/rest/api/3/attachment/content/$AttachmentId" `
            -Headers $AuthHeaders -MaximumRedirection 0 -UseBasicParsing -ErrorAction Stop
        $location = $redirect.Headers['Location']
    }
    catch {
        $redirectResponse = $_.Exception.Response
        if ($redirectResponse) {
            try { $location = $redirectResponse.Headers['Location'] } catch { }
            if (-not $location) {
                try { $location = $redirectResponse.Headers.Location.ToString() } catch { }
            }
        }
    }

    if (-not $location) { return $null }
    $uuidMatch = [regex]::Match([string]$location, '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')
    if ($uuidMatch.Success) { return $uuidMatch.Value }
    return $null
}

if ($IssueKey) {
    $IssueKey = $IssueKey.Trim().ToUpper()
    if ($IssueKey -notmatch '^[A-Z][A-Z0-9]+-\d+$') {
        Stop-With 4 "ISSUE_KEY_INVALID=$IssueKey"
    }
}

$resolvedReport = $null
if ($ReportPath) {
    if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
        $ReportPath = Join-Path $workspaceRoot $ReportPath
    }
    if (-not (Test-Path -LiteralPath $ReportPath)) {
        Stop-With 3 "REPORT_NOT_FOUND=$ReportPath"
    }
    $resolvedReport = Get-Item -LiteralPath $ReportPath
    if ($resolvedReport.Extension -ne ".docx") {
        Stop-With 3 "REPORT_NOT_DOCX=$($resolvedReport.FullName)"
    }
    if (-not $IssueKey) {
        $IssueKey = Get-IssueKeyFromName $resolvedReport.Name
    }
}
elseif ($IssueKey) {
    $resolvedReport = Find-ReportFile -Key $IssueKey -ReportsRootPath $reportsRoot
    if (-not $resolvedReport) {
        Write-Output "REPORT_NOT_FOUND=1"
        Write-Output "ISSUE_KEY=$IssueKey"
        exit 3
    }
}

if (-not $IssueKey) {
    Stop-With 4 "ISSUE_KEY_REQUIRED=1. Pass -IssueKey or a report file whose name contains the key."
}

$filenameKey = Get-IssueKeyFromName $resolvedReport.Name
if ($filenameKey -and $filenameKey -ne $IssueKey) {
    Write-Host "WARNING: report filename key is $filenameKey; commenting on $IssueKey." -ForegroundColor Yellow
}

Write-Output "ISSUE_KEY=$IssueKey"
Write-Output "REPORT_PATH=$($resolvedReport.FullName)"
Write-Output "REPORT_FILENAME=$($resolvedReport.Name)"

if ($DryRun) {
    Write-Output "DRY_RUN=1"
    Write-Output "COMMENT_TEXT=These are the main cases that were run and verified for this bug or task."
    exit 0
}

if (-not (Test-Path -LiteralPath $envFile)) {
    Stop-With 1 "Cannot find .env at: $envFile"
}

$envVars = Read-DotEnv -Path $envFile
$jiraEmail = $envVars["JIRA_EMAIL"]
$jiraToken = $envVars["JIRA_API_TOKEN"]
$jiraBase = if ($envVars["JIRA_BASE_URL"]) { $envVars["JIRA_BASE_URL"].TrimEnd("/") } else { "https://oppa-support.atlassian.net" }
$cloudId = if ($envVars["JIRA_CLOUD_ID"]) { $envVars["JIRA_CLOUD_ID"] } else { "ad451d5c-7331-46f8-9a47-f51dc8e6bbde" }

if (-not $jiraEmail -or -not $jiraToken) {
    Stop-With 1 "JIRA_EMAIL / JIRA_API_TOKEN not found in Cursor-Project/.env."
}

$authEncoded = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes("${jiraEmail}:${jiraToken}"))
$authHeaders = @{
    Authorization = "Basic $authEncoded"
    Accept        = "application/json"
}
$apiBase = "https://api.atlassian.com/ex/jira/$cloudId"

$issueTypeName = ""
try {
    $issueResponse = Invoke-WebRequest -Uri "$apiBase/rest/api/3/issue/${IssueKey}?fields=issuetype,summary" `
        -Headers $authHeaders -Method Get -UseBasicParsing
    $issue = $issueResponse.Content | ConvertFrom-Json
    $issueTypeName = [string]$issue.fields.issuetype.name
    Write-Output "ISSUE_TYPE=$issueTypeName"
    Write-Output "ISSUE_SUMMARY=$($issue.fields.summary)"
}
catch {
    $detail = Read-HttpError $_
    Stop-With 2 "Could not read ${IssueKey} before attach. $detail"
}

$commentText = Get-VerificationSentence -IssueTypeName $issueTypeName
Write-Output "COMMENT_TEXT=$commentText"

$filePath = $resolvedReport.FullName
$fileName = $resolvedReport.Name
Write-Host "Uploading '$fileName' to $IssueKey ..." -ForegroundColor Cyan

$uploadHeaders = @{
    Authorization       = "Basic $authEncoded"
    Accept              = "application/json"
    "X-Atlassian-Token" = "no-check"
}

$attachmentId = $null
$mediaUuid = $null
try {
    $boundary = [System.Guid]::NewGuid().ToString("N")
    $enc = [System.Text.Encoding]::UTF8
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
    $crlf = "`r`n"
    $headerText = "--$boundary$crlf" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$crlf" +
        "Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document$crlf$crlf"
    $footerText = "$crlf--$boundary--$crlf"
    $bodyList = [System.Collections.Generic.List[byte]]::new()
    $bodyList.AddRange($enc.GetBytes($headerText))
    $bodyList.AddRange($fileBytes)
    $bodyList.AddRange($enc.GetBytes($footerText))

    $uploadResponse = Invoke-WebRequest -Uri "$apiBase/rest/api/3/issue/$IssueKey/attachments" `
        -Method Post -Headers $uploadHeaders -Body $bodyList.ToArray() `
        -ContentType "multipart/form-data; boundary=$boundary" -UseBasicParsing

    $attachments = $uploadResponse.Content | ConvertFrom-Json
    $att = if ($attachments -is [System.Array]) { $attachments[0] } else { $attachments }
    $attachmentId = [string]$att.id
    if ($att.filename) { $fileName = [string]$att.filename }
    Write-Output "ATTACHMENT_ID=$attachmentId"
    Write-Output "ATTACHMENT_FILENAME=$fileName"
    if ($att.content) { Write-Output "ATTACHMENT_CONTENT_URL=$($att.content)" }

    $mediaUuid = Resolve-MediaUuid -ApiBase $apiBase -AttachmentId $attachmentId -AuthHeaders $authHeaders
    if ($mediaUuid) {
        Write-Output "ATTACHMENT_MEDIA_UUID=$mediaUuid"
    }
    else {
        Write-Host "Media id was not resolved. The comment will link the uploaded file." -ForegroundColor Yellow
    }
}
catch {
    $detail = Read-HttpError $_
    Stop-With 2 "Upload failed for '$fileName'. $detail"
}

$linkHref = $null
if (-not $mediaUuid) {
    $linkHref = "$jiraBase/secure/attachment/$attachmentId/$([uri]::EscapeDataString($fileName))"
}

function Send-JiraComment {
    param(
        [string]$JsonBody
    )
    $commentHeaders = @{
        Authorization  = "Basic $authEncoded"
        Accept         = "application/json"
        "Content-Type" = "application/json"
    }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($JsonBody)
    return Invoke-WebRequest -Uri "$apiBase/rest/api/3/issue/$IssueKey/comment" `
        -Method Post -Headers $commentHeaders -Body $bytes -UseBasicParsing
}

$embedMode = "link"
try {
    $json = New-CommentBodyJson -Text $commentText -MediaUuid $mediaUuid -FileName $fileName -LinkHref $linkHref
    $commentResponse = Send-JiraComment -JsonBody $json
    if ($mediaUuid) { $embedMode = "media" }
}
catch {
    if (-not $mediaUuid) {
        $detail = Read-HttpError $_
        Write-Output "ATTACHMENT_ID=$attachmentId"
        Stop-With 2 "File uploaded, but the comment was not posted. $detail"
    }
    Write-Host "Inline file embed was rejected. Posting the comment with a link to the uploaded file." -ForegroundColor Yellow
    try {
        $linkHref = "$jiraBase/secure/attachment/$attachmentId/$([uri]::EscapeDataString($fileName))"
        $json = New-CommentBodyJson -Text $commentText -FileName $fileName -LinkHref $linkHref
        $commentResponse = Send-JiraComment -JsonBody $json
        $embedMode = "link"
    }
    catch {
        $detail = Read-HttpError $_
        Write-Output "ATTACHMENT_ID=$attachmentId"
        Stop-With 2 "File uploaded, but the comment was not posted. $detail"
    }
}

$comment = $commentResponse.Content | ConvertFrom-Json
$commentId = [string]$comment.id
Write-Output "COMMENT_ID=$commentId"
Write-Output "COMMENT_EMBED=$embedMode"
Write-Output "COMMENT_URL=$jiraBase/browse/${IssueKey}?focusedCommentId=$commentId"
Write-Host "Comment posted on $IssueKey." -ForegroundColor Green
exit 0
