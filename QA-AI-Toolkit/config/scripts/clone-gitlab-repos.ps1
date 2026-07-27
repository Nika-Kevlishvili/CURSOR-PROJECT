param(
    [Parameter(Mandatory = $true)][string]$Url,
    [Parameter(Mandatory = $true)][string]$Destination,
    [string]$EnvFile,
    [string]$Token
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

if (-not $Token) { $Token = Get-EnvVar 'GITLAB_TOKEN' }
$cloneUrl = $Url
if ($Token -and $Url -match '^https://' -and $Url -notmatch '@') {
    $cloneUrl = $Url -replace '^https://', "https://oauth2:${Token}@"
}

$parent = Split-Path -Parent $Destination
if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
if ((Test-Path $Destination) -and (Test-Path (Join-Path $Destination '.git'))) {
    Write-Host "Already a git repo: $Destination"
    exit 0
}
git clone $cloneUrl $Destination
if ($LASTEXITCODE -ne 0) { Write-Error "Clone failed: $Url"; exit 1 }
Write-Host "Cloned: $Destination"
