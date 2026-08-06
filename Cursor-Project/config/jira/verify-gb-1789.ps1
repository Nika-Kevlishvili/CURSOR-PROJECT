$ErrorActionPreference = 'Stop'
function Get-EnvVar($name) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ($v) { return $v }
    foreach ($envPath in @(
        (Join-Path $PSScriptRoot '..\..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\.env')
    )) {
        if (-not (Test-Path -LiteralPath $envPath)) { continue }
        foreach ($line in Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            if ($trimmed -match "^\s*$name\s*=\s*['""]?(.+?)['""]?\s*$") { return $Matches[1] }
        }
    }
    return $null
}
$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'
$base = 'https://oppa-support.atlassian.net'
$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
$fields = 'summary,priority,assignee,status,fixVersions,customfield_10008,customfield_10096'
$i = Invoke-RestMethod -Uri "$base/rest/api/3/issue/GB-1789?fields=$fields" -Headers $headers
Write-Host "Summary: $($i.fields.summary)"
Write-Host "Status: $($i.fields.status.name)"
Write-Host "Priority: $($i.fields.priority.name)"
Write-Host "Assignee: $($i.fields.assignee.displayName)"
Write-Host "Epic Link: $($i.fields.customfield_10008)"
$testers = $i.fields.customfield_10096
if ($testers) {
    Write-Host ("Tester: " + (($testers | ForEach-Object { $_.displayName }) -join ', '))
} else {
    Write-Host 'Tester: (empty)'
}
$fv = $i.fields.fixVersions
if ($fv) { Write-Host ("FixVersion: " + (($fv | ForEach-Object { $_.name }) -join ', ')) }
Write-Host 'URL: https://oppa-support.atlassian.net/browse/GB-1789'
