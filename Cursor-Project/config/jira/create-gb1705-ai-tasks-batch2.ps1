param(
    [string]$ParentKey = 'GB-1705',
    [string]$FixVersionId = '13966',  # Tech tasks (matches GB-1705 epic scope)
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
    Write-Error 'Missing JIRA_EMAIL or JIRA_API_TOKEN (check .env paths)'
    exit 1
}

Write-Host "Jira base: $base | credentials: ok"

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

# New AI tasks requested under GB-1705 (Gela Barkalaia board)
$summaries = @(
    '[ AI ] Jira Bug creator',
    '[ AI ] Phoenix project setup script',
    '[ AI ] QA AI Toolkit and setup script',
    '[ AI ] Graph RAG / LLM research and integration',
    '[ AI ] Main agents should have a clearly defined role (e.g. Senior QA Agent)',
    '[ AI ] Elastic integration on AI project'
)

$parent = Invoke-RestMethod -Uri "$base/rest/api/3/issue/${ParentKey}?fields=summary,issuetype,project" -Headers $getHeaders
$projectKey = $parent.fields.project.key
$parentType = $parent.fields.issuetype.name

Write-Host "Parent: $ParentKey ($parentType) project=$projectKey"

# GB-1705 is Epic — use Task + Epic Link (Sub-task cannot attach directly to Epic)
$issueTypeId = '10002'   # Task
$epicLinkField = 'customfield_10008'

$created = @()
$failCount = 0
foreach ($summary in $summaries) {
    $body = @{
        fields = @{
            project     = @{ key = $projectKey }
            summary     = $summary
            issuetype   = @{ id = $issueTypeId }
            fixVersions = @(@{ id = $FixVersionId })
            $epicLinkField = $ParentKey
        }
    } | ConvertTo-Json -Depth 6

    if ($WhatIf) {
        Write-Host "WHATIF: would create Task under epic $ParentKey : $summary"
        continue
    }

    try {
        $resp = Invoke-RestMethod -Method Post -Uri "$base/rest/api/3/issue" -Headers $postHeaders -Body $body
        $url = "$base/browse/$($resp.key)"
        Write-Host "Created $($resp.key): $summary"
        Write-Host "  $url"
        $created += $resp.key
        Start-Sleep -Milliseconds 400
    }
    catch {
        $err = $_.ErrorDetails.Message
        if (-not $err) { $err = $_.Exception.Message }
        Write-Host "FAILED: $summary"
        Write-Host $err
        $script:failCount++
    }
}

Write-Host "`nDone. Created $($created.Count) issues; failed $failCount."
if ($created.Count -gt 0) {
    Write-Host ($created -join ', ')
}
if ($failCount -gt 0) { exit 1 }
