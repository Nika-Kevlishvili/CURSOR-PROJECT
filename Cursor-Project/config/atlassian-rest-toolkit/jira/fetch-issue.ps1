<#
.SYNOPSIS
    Fetches a full Jira issue via Atlassian Cloud REST API v3 (read-only).

.DESCRIPTION
    Downloads the complete issue payload (all fields, expand=names, changelog peek)
    and saves it as JSON. Uses Basic Auth with credentials from the toolkit .env file.

.PARAMETER IssueKey
    Jira issue key (e.g. PDT-2854, PROJ-123).

.PARAMETER OutputDir
    Optional output directory. Defaults to .\output\<IssueKey>\

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File fetch-issue.ps1 -IssueKey 'PDT-2854'
#>
param(
    [Parameter(Mandatory = $true)][string]$IssueKey,
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
    $OutputDir = Join-Path $toolkitRoot "output\$IssueKey"
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

$IssueKey = $IssueKey.Trim()

Write-Host "Fetching issue $IssueKey from $baseUrl ..."

$url = "$baseUrl/rest/api/3/issue/${IssueKey}?expand=names"
$response = $null
try {
    $raw = Invoke-WebRequest -Uri $url -Headers $headers -Method Get -UseBasicParsing
    $response = $raw.Content | ConvertFrom-Json
}
catch {
    Write-Error "Failed to fetch issue ${IssueKey}: $($_.Exception.Message)"
    exit 1
}

try {
    $clRaw = Invoke-WebRequest -Uri "$baseUrl/rest/api/3/issue/${IssueKey}/changelog?maxResults=1" -Headers $headers -Method Get -UseBasicParsing -ErrorAction SilentlyContinue
    if ($clRaw -and $clRaw.StatusCode -eq 200) {
        $cl = $clRaw.Content | ConvertFrom-Json
        Add-Member -InputObject $response -NotePropertyName '_changelogPeek' -NotePropertyValue $cl -Force
    }
}
catch { }

$outPath = Join-Path $OutputDir 'issue-rest.json'
$response | ConvertTo-Json -Depth 30 | Out-File -FilePath $outPath -Encoding utf8
Write-Host "Saved: $outPath"
exit 0
