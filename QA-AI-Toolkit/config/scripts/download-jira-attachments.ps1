param(
    [Parameter(Mandatory = $true)][string]$IssueKey,
    [string]$OutDir,
    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'

function Get-EnvVar([string]$name) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ($v) { return $v }
    $paths = @()
    if ($EnvFile) { $paths += $EnvFile }
    $paths += @((Join-Path (Get-Location) '.env'))
    foreach ($envPath in $paths) {
        if (-not (Test-Path -LiteralPath $envPath)) { continue }
        foreach ($line in Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            if ($trimmed -match "^\s*$name\s*=\s*['""]?(.+?)['""]?\s*$") { return $Matches[1] }
        }
    }
    return $null
}

$base = (Get-EnvVar 'JIRA_BASE_URL').Trim().TrimEnd('/')
$email = Get-EnvVar 'JIRA_EMAIL'
$token = Get-EnvVar 'JIRA_API_TOKEN'
if (-not $base -or -not $email -or -not $token) { Write-Error 'Missing Jira credentials in .env'; exit 1 }

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }
$issue = Invoke-RestMethod -Uri "$base/rest/api/3/issue/${IssueKey}?fields=attachment,summary" -Headers $headers
if (-not $OutDir) { $OutDir = Join-Path (Get-Location) "jira-attachments-$IssueKey" }
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$atts = @($issue.fields.attachment)
if ($atts.Count -eq 0) { Write-Host 'No attachments'; exit 0 }
foreach ($a in $atts) {
    $dest = Join-Path $OutDir $a.filename
    Invoke-WebRequest -Uri $a.content -Headers $headers -OutFile $dest
    Write-Host "Downloaded: $dest"
}
