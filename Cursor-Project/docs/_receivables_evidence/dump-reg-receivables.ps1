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
$base = Get-EnvVar 'JIRA_BASE_URL'
if (-not $base) { $base = 'https://oppa-support.atlassian.net' }
$base = $base.Trim().TrimEnd('/')
if (-not $email -or -not $token) { Write-Error 'Missing JIRA_EMAIL or JIRA_API_TOKEN'; exit 1 }

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }

$jql = @'
project = REG AND issuetype = "Test Case" AND summary ~ "liability OR receivable OR deposit OR offset OR payment OR \"collection channel\" OR reminder OR disconnection OR reconnection OR \"interest rate\" OR \"late payment\" OR \"default interest\" OR blocking OR \"customer assessment\" OR rescheduling OR \"coordinator objection\" OR \"objection withdrawal\"" ORDER BY key ASC
'@

$all = @()
$next = $null
$pages = 0
do {
    $body = @{ jql = $jql; maxResults = 100; fields = @('summary','status','issuetype') }
    if ($next) { $body.nextPageToken = $next }
    $jsonBody = $body | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$base/rest/api/3/search/jql" -Headers $headers -ContentType 'application/json' -Body $jsonBody
    foreach ($i in $resp.issues) {
        $all += [pscustomobject]@{
            Key = $i.key
            Summary = $i.fields.summary
            Status = $i.fields.status.name
            IssueType = $i.fields.issuetype.name
        }
    }
    $pages++
    $next = $resp.nextPageToken
    if (-not $resp.issues -or $resp.issues.Count -eq 0) { break }
    if ($resp.isLast -eq $true) { break }
} while ($next -and $pages -lt 20)

$out = Join-Path $PSScriptRoot 'jira_receivables_test_cases.json'
$all | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $out -Encoding utf8
Write-Host ("pages={0} issues={1} file={2}" -f $pages, $all.Count, $out)
