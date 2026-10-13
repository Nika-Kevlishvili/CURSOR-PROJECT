param(
    [switch]$SkipFetch
)

$ErrorActionPreference = 'Stop'
$siteRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $siteRoot 'data'
$mapPath = Join-Path $dataDir 'jira-map.json'
$outPath = Join-Path $dataDir 'jira-status.json'

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

function Write-Utf8Json($path, $obj) {
    $json = $obj | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText($path, $json + "`n", [System.Text.UTF8Encoding]::new($false))
}

if (-not (Test-Path -LiteralPath $mapPath)) {
    Write-Error "Missing $mapPath"
}

$map = Get-Content -LiteralPath $mapPath -Raw -Encoding UTF8 | ConvertFrom-Json
$keys = @()
if ($map.epic) { $keys += [string]$map.epic }
if ($map.tasks) {
    $map.tasks.PSObject.Properties | ForEach-Object { $keys += [string]$_.Value }
}
$keys = $keys | Where-Object { $_ } | Select-Object -Unique

$base = Get-EnvVar 'JIRA_BASE_URL'
if (-not $base) { $base = 'https://oppa-support.atlassian.net' }
$base = $base.Trim().TrimEnd('/')

$result = [ordered]@{
    syncedAt = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    jiraBase = $base + '/browse'
    epic     = $null
    issues   = [ordered]@{}
    errors   = @()
}

if ($SkipFetch -or $keys.Count -eq 0) {
    if ($keys.Count -eq 0) {
        $result.errors += 'jira-map.json has no issue keys yet'
    }
    Write-Utf8Json $outPath $result
    Write-Host "Wrote $outPath (no Jira fetch)"
    exit 0
}

$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'
if (-not $email -or -not $token) {
    Write-Error 'Missing JIRA_EMAIL or JIRA_API_TOKEN'
}

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{
    Authorization = "Basic $pair"
    Accept        = 'application/json'
}

function Add-Issue($issue) {
    $fields = $issue.fields
    $assignee = $null
    if ($fields.assignee -and $fields.assignee.displayName) {
        $assignee = [string]$fields.assignee.displayName
    }
    $statusName = $null
    $statusCat = $null
    if ($fields.status) {
        $statusName = [string]$fields.status.name
        if ($fields.status.statusCategory) {
            $statusCat = [string]$fields.status.statusCategory.key
        }
    }
    $priority = $null
    if ($fields.priority) { $priority = [string]$fields.priority.name }
    $entry = [ordered]@{
        key             = [string]$issue.key
        summary         = [string]$fields.summary
        status          = $statusName
        statusCategory  = $statusCat
        assignee        = $assignee
        priority        = $priority
        updated         = [string]$fields.updated
        url             = "$base/browse/$($issue.key)"
    }
    $result.issues[$issue.key] = $entry
    if ($map.epic -and $issue.key -eq $map.epic) {
        $result.epic = $entry
    }
}

$jql = 'key in (' + (($keys | ForEach-Object { '"' + $_ + '"' }) -join ',') + ')'
$searchBody = @{
    jql        = $jql
    maxResults = 100
    fields     = @('summary', 'status', 'assignee', 'updated', 'priority', 'issuetype')
} | ConvertTo-Json

foreach ($searchUrl in @("$base/rest/api/3/search", "$base/rest/api/3/search/jql")) {
    try {
        $payload = Invoke-RestMethod -Uri $searchUrl -Headers $headers -Method Post -ContentType 'application/json' -Body $searchBody
        $issues = @()
        if ($payload.issues) { $issues = @($payload.issues) }
        elseif ($payload.values) { $issues = @($payload.values) }
        foreach ($issue in $issues) { Add-Issue $issue }
        break
    }
    catch {
        continue
    }
}

$missing = @($keys | Where-Object { -not $result.issues.Contains($_) })
foreach ($key in $missing) {
    $url = "$base/rest/api/3/issue/${key}?fields=summary,status,assignee,updated,priority,issuetype"
    try {
        $issue = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
        Add-Issue $issue
    }
    catch {
        $result.errors += "$key : $($_.Exception.Message)"
    }
}

Write-Utf8Json $outPath $result
Write-Host "Synced $($result.issues.Count) issues -> $outPath"
if ($result.errors.Count -gt 0) {
    Write-Warning ($result.errors -join '; ')
}
