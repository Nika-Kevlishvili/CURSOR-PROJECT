param(
    [Parameter(Mandatory = $true)][string]$IssueKey,
    [string]$OutFile,
    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'

function Get-EnvVar([string]$name) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ($v) { return $v }
    $paths = @()
    if ($EnvFile) { $paths += $EnvFile }
    $paths += @(
        (Join-Path (Get-Location) '.env'),
        (Join-Path $PSScriptRoot '..\..\..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\..\.env')
    )
    foreach ($envPath in $paths) {
        if (-not (Test-Path -LiteralPath $envPath)) { continue }
        foreach ($line in Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            if ($trimmed -match "^\s*$name\s*=\s*['""]?(.+?)['""]?\s*$") {
                return $Matches[1]
            }
        }
    }
    return $null
}

$base = Get-EnvVar 'JIRA_BASE_URL'
$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'
if (-not $email -or -not $token) { Write-Error 'Missing JIRA_EMAIL or JIRA_API_TOKEN'; exit 1 }
if (-not $base) { Write-Error 'Missing JIRA_BASE_URL'; exit 1 }
$base = $base.Trim().TrimEnd('/')

$fields = @(
    'summary', 'description', 'status', 'priority', 'issuetype', 'environment', 'labels',
    'components', 'assignee', 'reporter', 'attachment', 'issuelinks', 'comment', 'created', 'updated'
) -join ','

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
$url = "$base/rest/api/3/issue/${IssueKey}?expand=names,changelog&fields=$fields"
$issue = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
if (-not $OutFile) { $OutFile = Join-Path (Get-Location) "$IssueKey-full.json" }
$issue | ConvertTo-Json -Depth 100 | Out-File -Encoding utf8 $OutFile
Write-Host "Saved: $OutFile"
Write-Host "Summary: $($issue.fields.summary)"
