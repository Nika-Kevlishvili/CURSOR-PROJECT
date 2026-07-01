param(
    [Parameter(Mandatory = $true)][string]$Cql,
    [int]$Limit = 10,
    [string]$OutFile = ''
)
function Get-EnvValue { param([string]$VarName)
    $val = [Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }
    $envPaths = @(
        (Join-Path $PSScriptRoot '..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\EnergoTS\.env'),
        (Join-Path $PSScriptRoot '..\..\Cursor Setup\env.example')
    )
    foreach ($envPath in $envPaths) {
        if (Test-Path -LiteralPath $envPath) {
            foreach ($line in Get-Content -LiteralPath $envPath) {
                $t = $line.Trim()
                if (-not $t -or $t.StartsWith('#')) { continue }
                if ($t -match "^\s*$VarName\s*=\s*['""]?(.+?)['""]?\s*$") { return $Matches[1] }
            }
        }
    }
    return $null
}
$email = Get-EnvValue 'CONFLUENCE_EMAIL'; if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }
$token = Get-EnvValue 'CONFLUENCE_API_TOKEN'; if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }
$wiki = Get-EnvValue 'CONFLUENCE_WIKI_BASE'
if (-not $wiki) {
    $jira = Get-EnvValue 'JIRA_BASE_URL'
    if (-not $jira) { $jira = 'https://oppa-support.atlassian.net' }
    $wiki = $jira.TrimEnd('/') + '/wiki'
}
$pair = "${email}:${token}"
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$headers = @{ Authorization = "Basic $b64"; Accept = 'application/json' }
$enc = [uri]::EscapeDataString($Cql)
$uri = "$($wiki.TrimEnd('/'))/rest/api/content/search?cql=$enc&limit=$Limit"
$r = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
$json = $r | ConvertTo-Json -Depth 10
if ($OutFile) { $json | Set-Content -LiteralPath $OutFile -Encoding UTF8; Write-Host "Wrote $OutFile" }
else { Write-Output $json }
