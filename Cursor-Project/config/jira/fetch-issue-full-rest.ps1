param(
    [Parameter(Mandatory = $true)][string]$IssueKey,
    [string]$OutputDir = ''
)

if (-not $PSScriptRoot) {
    $PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

$email = $null
$apiToken = $null
$baseUrl = $null
$envFile = Join-Path $PSScriptRoot '..\..\.env'
foreach ($line in Get-Content -LiteralPath $envFile -ErrorAction SilentlyContinue) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
    if ($trimmed -match '^\s*JIRA_EMAIL\s*=\s*(.+)$') { $email = $Matches[1].Trim() }
    elseif ($trimmed -match '^\s*JIRA_API_TOKEN\s*=\s*(.+)$') { $apiToken = $Matches[1].Trim() }
    elseif ($trimmed -match '^\s*JIRA_BASE_URL\s*=\s*(.+)$') { $baseUrl = $Matches[1].Trim() }
}

if (-not $email -or -not $apiToken) {
    Write-Error 'JIRA_EMAIL and JIRA_API_TOKEN must be set.'
    exit 1
}

if (-not $baseUrl) {
    $baseUrl = 'https://oppa-support.atlassian.net'
}

$baseUrl = $baseUrl.TrimEnd('/')

if (-not $OutputDir) {
    $OutputDir = Join-Path $PSScriptRoot "attachments\$IssueKey"
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$pair = $email + ':' + $apiToken
$bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
$base64 = [System.Convert]::ToBase64String($bytes)
$headers = @{
    'Authorization' = "Basic $base64"
    'Accept'        = 'application/json'
}

$IssueKey = $IssueKey.Trim()
# Brace variable name — otherwise `$IssueKey?expand` is misparsed in PowerShell (`?` / `=` becomes `issue/=names`).
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
