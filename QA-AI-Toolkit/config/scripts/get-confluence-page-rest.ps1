param(
    [Parameter(Mandatory = $true)][string]$PageId,
    [ValidateSet('v1', 'v2')][string]$Api = 'v1',
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

function Get-WikiBase {
    $explicit = Get-EnvValue 'CONFLUENCE_WIKI_BASE'
    if ($explicit) { return $explicit.TrimEnd('/') }
    $url = Get-EnvValue 'CONFLUENCE_URL'
    if ($url) {
        $u = $url.Trim().TrimEnd('/')
        if ($u -match '^(https://[^/]+/wiki)') { return $Matches[1] }
        if ($u -match '^(https://[^/]+)$') { return "$($Matches[1])/wiki" }
    }
    $jiraBase = Get-EnvValue 'JIRA_BASE_URL'
    if ($jiraBase -and $jiraBase -match '^(https://[^/]+\.atlassian\.net)') {
        return "$($Matches[1])/wiki"
    }
    return $null
}

$wiki = Get-WikiBase
$email = (Get-EnvValue 'CONFLUENCE_EMAIL'); if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }
$token = (Get-EnvValue 'CONFLUENCE_API_TOKEN'); if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }
if (-not $wiki -or -not $email -or -not $token) { Write-Error 'Missing Confluence/Jira credentials or wiki base'; exit 1 }

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
if ($Api -eq 'v1') {
    $url = "$wiki/rest/api/content/${PageId}?expand=body.storage,version,space"
} else {
    $url = "$wiki/api/v2/pages/${PageId}?body-format=storage"
}
$page = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
if ($OutFile) {
    $page | ConvertTo-Json -Depth 100 | Out-File -Encoding utf8 $OutFile
    Write-Host "Saved: $OutFile"
} else {
    $page | ConvertTo-Json -Depth 20
}
