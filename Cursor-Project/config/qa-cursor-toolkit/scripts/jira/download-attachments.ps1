<#
.SYNOPSIS
    Downloads Jira issue attachments and extracts text from DOCX files.

.DESCRIPTION
    Uses Atlassian Cloud REST API with Basic Auth to download all attachments
    from a Jira issue. Extracts text from .docx files using System.IO.Compression.
    Credentials are read from the toolkit .env file.

.PARAMETER IssueKey
    Jira issue key (e.g. PDT-2676).

.PARAMETER AttachmentIds
    Optional comma-separated attachment IDs to download specific files only.

.PARAMETER OutputDir
    Optional output directory. Defaults to .\output\<IssueKey>\attachments\

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File download-attachments.ps1 -IssueKey 'PDT-2676'

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File download-attachments.ps1 -IssueKey 'PDT-2676' -AttachmentIds '10001,10002'
#>
param(
    [Parameter(Mandatory = $true)][string]$IssueKey,
    [string]$AttachmentIds = '',
    [string]$OutputDir = ''
)

if (-not $PSScriptRoot) {
    $PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

$toolkitRoot = Split-Path -Parent $PSScriptRoot

function Get-EnvValue {
    param([string]$VarName)

    $val = [System.Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }

    $envPaths = @(
        (Join-Path $toolkitRoot '.env')
    )

    foreach ($envPath in $envPaths) {
        if (Test-Path -LiteralPath $envPath) {
            $lines = Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue
            foreach ($line in $lines) {
                $trimmed = $line.Trim()
                if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
                if ($trimmed -match "^\s*$VarName\s*=\s*['""]?(.+?)['""]?\s*$") {
                    return $Matches[1]
                }
            }
        }
    }
    return $null
}

function Get-DocxText {
    param([string]$DocxPath)

    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($DocxPath)
        $docEntry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }

        if (-not $docEntry) {
            $zip.Dispose()
            return $null
        }

        $stream = $docEntry.Open()
        $reader = New-Object System.IO.StreamReader($stream)
        $xmlContent = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()
        $zip.Dispose()

        $text = $xmlContent -replace '<[^>]+>', ' '
        $text = $text -replace '\s+', ' '
        $text = $text.Trim()

        return $text
    }
    catch {
        Write-Warning "Failed to extract text from $DocxPath : $_"
        return $null
    }
}

$email    = Get-EnvValue 'JIRA_EMAIL'
$apiToken = Get-EnvValue 'JIRA_API_TOKEN'
$baseUrl  = Get-EnvValue 'JIRA_BASE_URL'

if (-not $email -or -not $apiToken) {
    Write-Error 'JIRA_EMAIL and JIRA_API_TOKEN must be set in .env or environment variables.'
    Write-Host 'Hint: copy .env.example to .env and fill in your credentials.'
    exit 1
}

if (-not $baseUrl) {
    Write-Error 'JIRA_BASE_URL must be set in .env or environment variables.'
    exit 1
}

$baseUrl = $baseUrl.TrimEnd('/')

if (-not $OutputDir) {
    $OutputDir = Join-Path $toolkitRoot "output\$IssueKey\attachments"
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$pair  = "${email}:${apiToken}"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
$base64 = [System.Convert]::ToBase64String($bytes)
$headers = @{
    'Authorization' = "Basic $base64"
    'Accept'        = 'application/json'
}

Write-Host "Fetching issue $IssueKey from $baseUrl ..."
$issueUrl = "$baseUrl/rest/api/3/issue/${IssueKey}?fields=attachment,summary"

try {
    $response = Invoke-RestMethod -Uri $issueUrl -Headers $headers -Method Get -ErrorAction Stop
}
catch {
    Write-Error "Failed to fetch issue $IssueKey : $_"
    exit 1
}

$attachments = $response.fields.attachment
if (-not $attachments -or $attachments.Count -eq 0) {
    Write-Host "No attachments found on $IssueKey"
    exit 0
}

$filterIds = @()
if ($AttachmentIds) {
    $filterIds = $AttachmentIds -split ',' | ForEach-Object { $_.Trim() }
}

$manifest = @{
    issueKey     = $IssueKey
    summary      = $response.fields.summary
    outputDir    = $OutputDir
    downloadedAt = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    files        = @()
}

Write-Host "Found $($attachments.Count) attachment(s). Downloading..."

foreach ($att in $attachments) {
    if ($filterIds.Count -gt 0 -and $att.id -notin $filterIds) {
        Write-Host "  Skipping $($att.filename) (id=$($att.id)) - not in filter list"
        continue
    }

    $filename = $att.filename
    $safeFilename = $filename -replace '[<>:"/\\|?*]', '_'
    $outPath = Join-Path $OutputDir $safeFilename

    Write-Host "  Downloading: $filename ($($att.mimeType), $([math]::Round($att.size/1024, 1)) KB) ..."

    try {
        Invoke-RestMethod -Uri $att.content -Headers $headers -Method Get -OutFile $outPath -ErrorAction Stop
        Write-Host "    -> Saved to: $outPath"

        $fileEntry = @{
            id            = $att.id
            filename      = $filename
            mimeType      = $att.mimeType
            size          = $att.size
            path          = $outPath
            textExtracted = $false
            textPath      = $null
        }

        $ext = [System.IO.Path]::GetExtension($filename).ToLower()
        if ($ext -eq '.docx') {
            Write-Host "    -> Extracting text from DOCX..."
            $text = Get-DocxText -DocxPath $outPath
            if ($text) {
                $txtPath = $outPath -replace '\.docx$', '.extracted.txt'
                $text | Out-File -FilePath $txtPath -Encoding utf8
                Write-Host "    -> Text saved to: $txtPath"
                $fileEntry.textExtracted = $true
                $fileEntry.textPath = $txtPath
            }
            else {
                Write-Host "    -> No text extracted from DOCX"
            }
        }

        $manifest.files += $fileEntry
    }
    catch {
        Write-Warning "  Failed to download $filename : $_"
    }
}

$manifestPath = Join-Path $OutputDir 'manifest.json'
$manifest | ConvertTo-Json -Depth 5 | Out-File -FilePath $manifestPath -Encoding utf8
Write-Host "`nManifest saved to: $manifestPath"
Write-Host "Download complete: $($manifest.files.Count) file(s) saved to $OutputDir"
