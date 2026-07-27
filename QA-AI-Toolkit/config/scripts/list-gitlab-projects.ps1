param(
    [string]$EnvFile,
    [string]$GroupPath = '',
    [string]$Search = '',
    [int]$PerPage = 50,
    [int]$MaxPages = 20
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

$base = Get-EnvVar 'GITLAB_BASE_URL'
$token = Get-EnvVar 'GITLAB_TOKEN'
if (-not $base -or -not $token) { Write-Error 'GITLAB_BASE_URL and GITLAB_TOKEN required in .env'; exit 1 }
$base = $base.Trim().TrimEnd('/')
$headers = @{ 'PRIVATE-TOKEN' = $token }

$all = @()
for ($page = 1; $page -le $MaxPages; $page++) {
    $qs = "membership=true&simple=true&per_page=$PerPage&page=$page&order_by=name&sort=asc"
    if ($Search) { $qs += "&search=$([uri]::EscapeDataString($Search))" }
    $url = "$base/api/v4/projects?$qs"
    $batch = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
    if (-not $batch -or @($batch).Count -eq 0) { break }
    $all += @($batch)
    if (@($batch).Count -lt $PerPage) { break }
}

if ($GroupPath) {
    $gp = $GroupPath.Trim().Trim('/')
    $all = @($all | Where-Object { $_.path_with_namespace -like "$gp/*" -or $_.path_with_namespace -eq $gp })
}

$all | ForEach-Object {
    [pscustomobject]@{
        id = $_.id
        name = $_.name
        path_with_namespace = $_.path_with_namespace
        http_url_to_repo = $_.http_url_to_repo
        ssh_url_to_repo = $_.ssh_url_to_repo
    }
} | ConvertTo-Json -Depth 5
