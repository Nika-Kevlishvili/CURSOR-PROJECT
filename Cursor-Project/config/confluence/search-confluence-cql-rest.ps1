param(
    [Parameter(Mandatory = $true)][string]$Cql,
    [int]$Limit = 25,
    [string]$OutFile = ''
)

$ErrorActionPreference = 'Stop'

function Get-EnvValue {
    param([string]$VarName)
    $val = [System.Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }
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
            if ($trimmed -match "^\s*$VarName\s*=\s*['""]?(.+?)['""]?\s*$") {
                return $Matches[1]
            }
        }
    }
    return $null
}

function Get-WikiBase {
    $explicit = Get-EnvValue 'CONFLUENCE_WIKI_BASE'
    if ($explicit) { return $explicit.TrimEnd('/') }
    $url = Get-EnvValue 'CONFLUENCE_URL'
    if ($url) {
        $u = $url.Trim().TrimEnd('/')
        if ($u -match '^(https://[^/]+/wiki)') { return $Matches[1] }
        if ($u -match '^(https://[^/]+)$') { return "$($Matches[1])/wiki" }
    }
    # Phoenix wiki is on asterbit, not oppa-support
    return 'https://asterbit.atlassian.net/wiki'
}

$email = Get-EnvValue 'CONFLUENCE_EMAIL'
if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }
$token = Get-EnvValue 'CONFLUENCE_API_TOKEN'
if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }
$base = Get-WikiBase

if (-not $email -or -not $token) { Write-Error 'Missing Confluence/Jira credentials'; exit 1 }
if (-not $base) { Write-Error 'Missing CONFLUENCE_WIKI_BASE'; exit 1 }

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
$encoded = [Uri]::EscapeDataString($Cql)
$url = "$base/rest/api/content/search?cql=$encoded&limit=$Limit&expand=ancestors"

$result = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
if ($OutFile) {
    $result | ConvertTo-Json -Depth 20 | Out-File -Encoding utf8 $OutFile
    Write-Host "Saved: $OutFile"
}
Write-Host "Total size: $($result.size) / $($result.totalSize)"
foreach ($r in $result.results) {
    $anc = @()
    if ($r.ancestors) { $anc = $r.ancestors | ForEach-Object { $_.title } }
    Write-Host ("ID={0} TITLE={1} ANCESTORS={2}" -f $r.id, $r.title, ($anc -join ' > '))
}
