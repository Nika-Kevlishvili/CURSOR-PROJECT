param(
    [Parameter(Mandatory = $true)][string]$Cql,
    [int]$Limit = 25,
    [string]$OutFile = '',
    [string]$EnvFile
)

function Get-EnvValue([string]$VarName) {
    $val = [Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }
    $paths = @()
    if ($EnvFile) { $paths += $EnvFile }
    $paths += @((Join-Path (Get-Location) '.env'))
    foreach ($envPath in $paths) {
        if (-not (Test-Path -LiteralPath $envPath)) { continue }
        foreach ($line in Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            if ($trimmed -match "^\s*$VarName\s*=\s*['""]?(.+?)['""]?\s*$") { return $Matches[1] }
        }
    }
    return $null
}

$wikiBase = Get-EnvValue 'CONFLUENCE_WIKI_BASE'
if (-not $wikiBase) {
    $u = Get-EnvValue 'CONFLUENCE_URL'
    if ($u -match '^(https://[^/]+/wiki)') { $wikiBase = $Matches[1] }
    elseif ($u -match '^(https://[^/]+)$') { $wikiBase = "$($Matches[1])/wiki" }
}
$email = (Get-EnvValue 'CONFLUENCE_EMAIL'); if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }
$token = (Get-EnvValue 'CONFLUENCE_API_TOKEN'); if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }
if (-not $wikiBase -or -not $email -or -not $token) { Write-Error 'Missing Confluence credentials'; exit 1 }

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
$encoded = [uri]::EscapeDataString($Cql)
$url = "$($wikiBase.TrimEnd('/'))/rest/api/content/search?cql=$encoded&limit=$Limit"
$result = Invoke-RestMethod -Uri $url -Headers $headers
if ($OutFile) {
    $result | ConvertTo-Json -Depth 40 | Out-File -Encoding utf8 $OutFile
    Write-Host "Saved: $OutFile"
} else {
    $result | ConvertTo-Json -Depth 10
}
