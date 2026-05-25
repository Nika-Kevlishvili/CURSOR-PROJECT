param(
    [Parameter(Mandatory = $true)]
    [string]$IssueKey,
    [string]$OutFile
)

function Get-EnvVar($name) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ($v) { return $v }

    $envPaths = @(
        (Join-Path $PSScriptRoot '..\..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\EnergoTS\.env'),
        (Join-Path $PSScriptRoot '..\..\Cursor Setup\env.example')
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

$base = Get-EnvVar 'JIRA_BASE_URL'
$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'

if (-not $base -or -not $email -or -not $token) {
    Write-Error 'Missing JIRA_BASE_URL, JIRA_EMAIL, or JIRA_API_TOKEN'
    exit 1
}

$fields = @(
    'summary', 'description', 'status', 'priority', 'issuetype', 'environment', 'labels',
    'components', 'assignee', 'reporter', 'attachment', 'issuelinks', 'comment', 'created', 'updated',
    'customfield_10103', 'customfield_10283', 'customfield_10048', 'customfield_10745',
    'customfield_10104', 'customfield_10217', 'customfield_10095', 'customfield_10877',
    'customfield_10485', 'customfield_10484', 'customfield_10483'
) -join ','

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{
    Authorization = "Basic $pair"
    Accept        = 'application/json'
}

$url = "$base/rest/api/3/issue/$IssueKey?expand=names,changelog&fields=$fields"
$issue = Invoke-RestMethod -Uri $url -Headers $headers -Method Get

if (-not $OutFile) {
    $OutFile = Join-Path $PSScriptRoot "$IssueKey-full.json"
}
$issue | ConvertTo-Json -Depth 100 | Out-File -Encoding utf8 $OutFile

Write-Host "Saved: $OutFile"
Write-Host "Summary: $($issue.fields.summary)"
Write-Host "Status: $($issue.fields.status.name)"
Write-Host "Comments: $($issue.fields.comment.total)"
Write-Host "Changelog entries: $($issue.changelog.histories.Count)"
