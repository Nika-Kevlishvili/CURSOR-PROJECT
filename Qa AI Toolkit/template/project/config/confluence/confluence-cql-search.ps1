param([Parameter(Mandatory = $true)][string]$Cql)
if (-not $PSScriptRoot) { $PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }

$email = $null; $token = $null; $confUrl = $null
$envFile = Join-Path $PSScriptRoot '..\..\.env'
foreach ($line in Get-Content -LiteralPath $envFile -ErrorAction SilentlyContinue) {
    $t = $line.Trim()
    if (-not $t -or $t.StartsWith('#')) { continue }
    if ($t -match '^\s*CONFLUENCE_EMAIL\s*=\s*(.+)$') { $email = $Matches[1].Trim() }
    elseif ($t -match '^\s*CONFLUENCE_API_TOKEN\s*=\s*(.+)$') { $token = $Matches[1].Trim() }
    elseif ($t -match '^\s*JIRA_EMAIL\s*=\s*(.+)$') { if (-not $email) { $email = $Matches[1].Trim() } }
    elseif ($t -match '^\s*JIRA_API_TOKEN\s*=\s*(.+)$') { if (-not $token) { $token = $Matches[1].Trim() } }
    elseif ($t -match '^\s*CONFLUENCE_URL\s*=\s*(.+)$') { $confUrl = $Matches[1].Trim() }
}

if (-not $email -or -not $token) { Write-Error 'No Confluence/Jira auth in .env'; exit 1 }
$wiki = 'https://asterbit.atlassian.net/wiki'
if ($confUrl) {
    $u = $confUrl.Trim().TrimEnd('/')
    if ($u -match '^(https://[^/]+/wiki)') { $wiki = $Matches[1] }
    elseif ($u -match '^(https://[^/]+\.atlassian\.net)$') { $wiki = "$($Matches[1])/wiki" }
}

$pair = "${email}:${token}"
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$h = @{ Authorization = "Basic $b64"; Accept = 'application/json' }
$enc = [Uri]::EscapeDataString($Cql)
$searchUrl = "${wiki}/rest/api/content/search?cql=$enc&limit=15&expand=space,version"
try {
    $r = Invoke-WebRequest -Uri $searchUrl -Headers $h -UseBasicParsing
    $r.Content
}
catch {
    Write-Error $_.Exception.Message
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    exit 1
}
