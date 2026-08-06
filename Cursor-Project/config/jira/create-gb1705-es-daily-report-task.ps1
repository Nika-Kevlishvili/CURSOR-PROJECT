param(
    [string]$ParentKey = 'GB-1705',
    [string]$FixVersionId = '13966',
    [switch]$WhatIf
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
$getHeaders = @{
    Authorization = "Basic $pair"
    Accept        = 'application/json'
}
$postHeaders = @{
    Authorization  = "Basic $pair"
    Accept         = 'application/json'
    'Content-Type' = 'application/json'
}

$summary = '[ AI ] Extract daily logs from Elasticsearch, organize them by objects and processes, and generate a daily report.'
$assigneeSearch = 'Saba Uertashvili'

Write-Host "Looking up user: $assigneeSearch"
$userUrl = "$base/rest/api/3/user/search?query=$([uri]::EscapeDataString($assigneeSearch))&maxResults=10"
$users = Invoke-RestMethod -Uri $userUrl -Headers $getHeaders
if (-not $users -or $users.Count -eq 0) {
    Write-Error "No Jira user found for '$assigneeSearch'"
    exit 1
}
foreach ($u in $users) {
    Write-Host ("  candidate: {0} | {1} | accountId={2}" -f $u.displayName, $u.emailAddress, $u.accountId)
}
$accountId = $users[0].accountId
$displayName = $users[0].displayName
Write-Host "Using: $displayName ($accountId)"

Write-Host "Fetching priorities..."
$priorities = Invoke-RestMethod -Uri "$base/rest/api/3/priority" -Headers $getHeaders
$highest = $priorities | Where-Object { $_.name -eq 'Highest' } | Select-Object -First 1
if (-not $highest) {
    Write-Host "Available priorities:"
    $priorities | ForEach-Object { Write-Host ("  {0} (id={1})" -f $_.name, $_.id) }
    Write-Error "Priority 'Highest' not found"
    exit 1
}
Write-Host ("Priority Highest id={0}" -f $highest.id)

$parent = Invoke-RestMethod -Uri "$base/rest/api/3/issue/${ParentKey}?fields=summary,issuetype,project" -Headers $getHeaders
$projectKey = $parent.fields.project.key
Write-Host "Parent: $ParentKey project=$projectKey"

# Discover Tester field from create metadata if needed
$metaUrl = "$base/rest/api/3/issue/createmeta?projectKeys=$projectKey&issuetypeNames=Task&expand=projects.issuetypes.fields"
$meta = Invoke-RestMethod -Uri $metaUrl -Headers $getHeaders
$fields = $meta.projects[0].issuetypes[0].fields
$testerFieldId = $null
foreach ($prop in $fields.PSObject.Properties) {
    $fname = $prop.Value.name
    if ($fname -eq 'Tester' -or $fname -eq 'QA Tester' -or $fname -like '*Tester*') {
        $testerFieldId = $prop.Name
        Write-Host "Tester field: $testerFieldId ($fname) schema=$($prop.Value.schema.type)/$($prop.Value.schema.custom)"
        break
    }
}
# Known fallback from workspace mapping
if (-not $testerFieldId) {
    $testerFieldId = 'customfield_10095'
    Write-Host "Tester field fallback: $testerFieldId"
}

$issueTypeId = '10002'   # Task
$epicLinkField = 'customfield_10008'

$fieldsBody = @{
    project     = @{ key = $projectKey }
    summary     = $summary
    issuetype   = @{ id = $issueTypeId }
    priority    = @{ id = $highest.id }
    assignee    = @{ accountId = $accountId }
    fixVersions = @(@{ id = $FixVersionId })
    $epicLinkField = $ParentKey
}

# Tester is typically a user picker
$fieldsBody[$testerFieldId] = @{ accountId = $accountId }

$bodyObj = @{ fields = $fieldsBody }
$body = $bodyObj | ConvertTo-Json -Depth 8

if ($WhatIf) {
    Write-Host "WHATIF body:"
    Write-Host $body
    exit 0
}

try {
    $resp = Invoke-RestMethod -Method Post -Uri "$base/rest/api/3/issue" -Headers $postHeaders -Body $body
    $url = "$base/browse/$($resp.key)"
    Write-Host "Created $($resp.key)"
    Write-Host "  $url"
    Write-Host "  Assignee/Tester: $displayName"
    Write-Host "  Priority: Highest"
} catch {
    $err = $_.ErrorDetails.Message
    if (-not $err) { $err = $_.Exception.Message }
    Write-Host "FAILED create:"
    Write-Host $err
    # If Tester field format wrong, retry without tester then set via edit
    Write-Host "Retrying without Tester field, then update..."
    $fieldsBody.Remove($testerFieldId)
    $body2 = (@{ fields = $fieldsBody } | ConvertTo-Json -Depth 8)
    $resp = Invoke-RestMethod -Method Post -Uri "$base/rest/api/3/issue" -Headers $postHeaders -Body $body2
    Write-Host "Created $($resp.key) without Tester; updating Tester..."
    $editBodies = @(
        (@{ fields = @{ $testerFieldId = @{ accountId = $accountId } } } | ConvertTo-Json -Depth 6),
        (@{ fields = @{ $testerFieldId = @(@{ accountId = $accountId }) } } | ConvertTo-Json -Depth 6)
    )
    $testerOk = $false
    foreach ($eb in $editBodies) {
        try {
            Invoke-RestMethod -Method Put -Uri "$base/rest/api/3/issue/$($resp.key)" -Headers $postHeaders -Body $eb | Out-Null
            $testerOk = $true
            Write-Host "Tester set successfully"
            break
        } catch {
            Write-Host "Tester update attempt failed: $($_.ErrorDetails.Message)"
        }
    }
    if (-not $testerOk) {
        Write-Host "WARNING: issue created but Tester field could not be set"
    }
    Write-Host "Created $($resp.key)"
    Write-Host "  https://oppa-support.atlassian.net/browse/$($resp.key)"
}
