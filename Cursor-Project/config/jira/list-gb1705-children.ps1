param(
    [string]$ParentKey = 'GB-1705'
)

$ErrorActionPreference = 'Stop'

function Get-EnvVar($name) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ($v) { return $v }
    $envPaths = @(
        (Join-Path $PSScriptRoot '..\..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\EnergoTS\.env')
    )
    foreach ($envPath in $envPaths) {
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

$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'
$base = 'https://oppa-support.atlassian.net'

if (-not $email -or -not $token) {
    Write-Error 'Missing JIRA_EMAIL or JIRA_API_TOKEN'
    exit 1
}

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{
    Authorization = "Basic $pair"
    Accept        = 'application/json'
}

$jql = "`"Epic Link`" = $ParentKey ORDER BY key ASC"
$encoded = [uri]::EscapeDataString($jql)
$url = "$base/rest/api/3/search/jql?jql=$encoded&maxResults=100&fields=summary,status,issuetype"

try {
    $r = Invoke-RestMethod -Uri $url -Headers $headers
} catch {
    # Fallback to legacy search endpoint
    $url = "$base/rest/api/3/search?jql=$encoded&maxResults=100&fields=summary,status,issuetype"
    $r = Invoke-RestMethod -Uri $url -Headers $headers
}

Write-Host "Total: $($r.total)"
foreach ($i in $r.issues) {
    Write-Host ("{0} | {1} | {2} | {3}" -f $i.key, $i.fields.issuetype.name, $i.fields.status.name, $i.fields.summary)
}
